package app

import (
	"context"
	"encoding/json"
	"fmt"
	"strings"
	"time"

	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/md"
	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/panel"
	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/ui"
)

// Chat colours: the transcript is a light page (like the dialog body), the
// header an inverse bar. Every row is painted with its own background so a
// styled Markdown fragment cannot leak the terminal's default colours.
const (
	chatBody   = "\x1b[37;44m"
	chatHeader = "\x1b[30;46m"
	chatDim    = "\x1b[36m"
	chatUser   = "\x1b[1;36m"
	chatHelper = "\x1b[1;32m"
	chatFg     = "\x1b[37m"
)

// chatState is everything one chat screen owns. It lives on the stack of
// chatLoop, so leaving chat is enough to release it.
type chatState struct {
	session    panel.ChatSession
	assistants []panel.ChatAssistant
	settings   *panel.ChatSettings
	search     bool

	area   *ui.TextArea
	scroll int
	expand bool

	live        bool
	liveText    string
	liveThink   string
	turnID      string
	turnStarted time.Time
	quitArmed   bool
	quitAt      time.Time
	notice      string
	lastError   string
	completion  []string
	compIndex   int

	renderCache map[string][]string
	renderWidth int
}

// chatLoop is the interactive chat screen. It returns when the user backs out
// (Esc) or the context ends.
func (a *App) chatLoop(ctx context.Context, client *panel.Client, sessionID string) {
	st := &chatState{
		area:        ui.NewTextArea(40, 3),
		renderCache: map[string][]string{},
		scroll:      0,
		expand:      false,
	}
	st.area.History = nil

	assistants, err := client.ChatAssistants(ctx)
	if err == nil {
		st.assistants = assistants
	}
	a.chatLoadSettings(ctx, client, st)

	session, err := client.ChatSession(ctx, sessionID)
	if err != nil {
		a.message("打不开会话", explain(err))
		return
	}
	st.session = session
	st.search = false
	st.notice = "Enter 发送 · Ctrl-J 换行 · Ctrl-O 展开 · Ctrl-B 会话 · Ctrl-E 提权 · Esc 返回"

	stream := a.chatStream(ctx, client, sessionID, st)
	defer func() {
		if stream != nil {
			stream.Close()
		}
	}()
	ticker := time.NewTicker(100 * time.Millisecond)
	defer ticker.Stop()

	a.flush()
	dirty := true
	for {
		if dirty {
			a.drawChat(ctx, client, st)
			dirty = false
		}
		var streamEvents <-chan panel.Event
		if stream != nil {
			streamEvents = stream.Events()
		}
		select {
		case <-ctx.Done():
			return
		case event, ok := <-a.screen.Events():
			if !ok {
				return
			}
			if event.Key == ui.KeyResize {
				st.renderCache = map[string][]string{}
				dirty = true
				continue
			}
			if event.Key == ui.KeyEsc {
				return
			}
			action, redraw := a.chatKey(ctx, client, st, event)
			dirty = dirty || redraw
			switch action {
			case chatQuit:
				return
			case chatSwitchSession:
				// chatKey already updated st.session; reconnect the stream.
				if stream != nil {
					stream.Close()
				}
				stream = a.chatStream(ctx, client, st.session.ID, st)
				st.renderCache = map[string][]string{}
				dirty = true
			}
		case event, ok := <-streamEvents:
			if !ok {
				st.notice = "事件通道断开，正在重连…"
				if stream != nil {
					stream.Close()
				}
				stream = a.chatReconnect(ctx, client, st.session.ID, st)
				dirty = true
				continue
			}
			if event.SessionID != "" && event.SessionID != st.session.ID {
				continue
			}
			if a.chatEvent(ctx, client, st, event) {
				dirty = true
			}
		case <-ticker.C:
			if st.live {
				dirty = true
			}
		}
	}
}

type chatAction int

const (
	chatStay chatAction = iota
	chatQuit
	chatSwitchSession
)

func (a *App) chatStream(ctx context.Context, client *panel.Client, sessionID string, st *chatState) *panel.EventStream {
	stream, err := client.DialEvents(ctx, a.tlsConfig)
	if err != nil {
		st.notice = "实时更新不可用：" + explain(err)
		return nil
	}
	topics := []string{"chat", "chat:" + sessionID}
	if err := stream.Subscribe(topics...); err != nil {
		stream.Close()
		st.notice = "订阅聊天事件失败：" + explain(err)
		return nil
	}
	return stream
}

func (a *App) chatReconnect(ctx context.Context, client *panel.Client, sessionID string, st *chatState) *panel.EventStream {
	select {
	case <-ctx.Done():
		return nil
	case <-time.After(time.Second):
	}
	if session, err := client.ChatSession(ctx, sessionID); err == nil {
		st.session = session
		st.live = false
		a.flush()
	}
	return a.chatStream(ctx, client, sessionID, st)
}

func (a *App) chatLoadSettings(ctx context.Context, client *panel.Client, st *chatState) {
	settings, err := client.ChatSettings(ctx)
	if err != nil {
		st.settings = nil
		return
	}
	st.settings = &settings
}

// chatEvent folds one gateway frame into the live view. It reports whether the
// screen changed.
func (a *App) chatEvent(ctx context.Context, client *panel.Client, st *chatState, event panel.Event) bool {
	switch event.Type {
	case "chat-turn-started":
		if event.TurnID != "" {
			st.turnID = event.TurnID
		}
		if st.turnStarted.IsZero() {
			st.turnStarted = time.Now()
		}
		st.live = true
		st.liveText = ""
		st.liveThink = ""
		return true
	case "chat-delta":
		st.live = true
		st.liveText += event.Text
		return event.Text != ""
	case "chat-reasoning":
		st.live = true
		st.liveThink += event.Text
		return event.Text != ""
	case "chat-block":
		if event.Phase == "start" && event.BlockType == "thinking" && st.liveThink != "" {
			st.liveThink += "\n\n"
			return true
		}
	case "chat-error":
		st.lastError = event.Message
		st.live = false
		st.turnStarted = time.Time{}
		return true
	case "chat-turn-finished":
		st.live = false
		st.turnID = ""
		st.turnStarted = time.Time{}
		if session, err := client.ChatSession(ctx, st.session.ID); err == nil {
			st.session = session
		}
		return true
	case "chat-title":
		if event.Title != "" {
			st.session.Title = event.Title
			return true
		}
	case "chat-sessions-changed":
		return false
	}
	return false
}

// chatKey handles one key press and returns what the loop should do.
func (a *App) chatKey(ctx context.Context, client *panel.Client, st *chatState, event ui.Event) (chatAction, bool) {
	if event.Key != ui.KeyInterrupt {
		st.quitArmed = false
	}
	switch event.Key {
	case ui.KeyResize:
		st.renderCache = map[string][]string{}
		return chatStay, true
	case ui.KeyEsc:
		// Kimi's Esc cancels first: stop the turn, close the completion; it
		// backs out of chat only when there is nothing to cancel.
		if st.live && st.turnID != "" {
			if err := client.ChatCancel(ctx, st.turnID); err != nil {
				a.chatFail(st, "停止失败：", err)
			} else {
				st.notice = "已请求停止"
			}
			return chatStay, true
		}
		if len(st.completion) > 0 {
			st.completion = nil
			return chatStay, true
		}
		return chatQuit, false
	case ui.KeyEOF:
		// Ctrl-D leaves like Kimi: only from an empty, idle composer.
		if st.area.String() == "" && !st.live {
			return chatQuit, false
		}
		return chatStay, true
	case ui.KeyInterrupt:
		if st.live && st.turnID != "" {
			if err := client.ChatCancel(ctx, st.turnID); err != nil {
				a.chatFail(st, "停止失败：", err)
			} else {
				st.notice = "已请求停止"
			}
			return chatStay, true
		}
		if st.area.String() != "" {
			st.area.Clear()
			return chatStay, true
		}
		// Two presses to leave, like Kimi; one press is a hint, not a quit.
		if !st.quitArmed || time.Since(st.quitAt) > 2*time.Second {
			st.quitArmed = true
			st.quitAt = time.Now()
			st.notice = "再按一次 Ctrl-C 退出聊天（Ctrl-D 也可以）"
			return chatStay, true
		}
		return chatQuit, false
	case ui.KeyExpand:
		if !sessionHasTimeline(st.session) {
			st.notice = "这条会话没有可展开的思考/搜索"
			return chatStay, true
		}
		st.expand = !st.expand
		st.renderCache = map[string][]string{}
		return chatStay, true
	case ui.KeySessions:
		sessionID, ok := a.chatPickSession(ctx, client, st.session.ID)
		if ok && sessionID != "" {
			session, err := client.ChatSession(ctx, sessionID)
			if err != nil {
				a.chatFail(st, "打不开会话：", err)
				return chatStay, true
			}
			st.session = session
			st.live = false
			st.scroll = 0
			return chatSwitchSession, true
		}
		return chatStay, true
	case ui.KeyElevate:
		a.elevate(ctx)
		a.chatLoadSettings(ctx, client, st)
		return chatStay, true
	case ui.KeyPgUp:
		st.scroll += 8
		return chatStay, true
	case ui.KeyPgDn:
		if st.scroll > 0 {
			st.scroll -= 8
			if st.scroll < 0 {
				st.scroll = 0
			}
		}
		return chatStay, true
	case ui.KeyEnter:
		text := strings.TrimSpace(st.area.String())
		if text == "" {
			return chatStay, false
		}
		st.area.AddHistory(text)
		st.area.Clear()
		// The completion list sits above the status line; leaving it populated
		// made a command's own answer ("已关闭联网搜索", "还没配置…") invisible.
		st.completion = nil
		st.compIndex = 0
		if strings.HasPrefix(text, "/") {
			return a.chatCommand(ctx, client, st, text)
		}
		return a.chatSend(ctx, client, st, text)
	case ui.KeyTab:
		if len(st.completion) > 0 {
			st.area.SetValue(st.completion[st.compIndex%len(st.completion)])
			st.compIndex++
			st.completion = chatCompletions(st.area.String())
			return chatStay, true
		}
		return chatStay, false
	}
	if st.area.Handle(event) {
		return chatStay, false
	}
	if strings.HasPrefix(st.area.String(), "/") {
		st.completion = chatCompletions(st.area.String())
		st.compIndex = 0
	} else {
		st.completion = nil
		st.compIndex = 0
	}
	return chatStay, true
}

func (a *App) chatSend(ctx context.Context, client *panel.Client, st *chatState, text string) (chatAction, bool) {
	st.live = true
	st.liveText = ""
	st.liveThink = ""
	st.lastError = ""
	result, err := client.ChatSend(ctx, st.session.ID, panel.ChatSendBody{
		Text:   text,
		Search: st.search,
	})
	if err != nil {
		st.live = false
		a.chatFail(st, "", err)
		return chatStay, true
	}
	st.turnID = result.TurnID
	st.turnStarted = time.Now()
	st.scroll = 0
	if session, err := client.ChatSession(ctx, st.session.ID); err == nil {
		st.session = session
	}
	return chatStay, true
}

// chatCommand runs the slash commands the TUI understands.
func (a *App) chatCommand(ctx context.Context, client *panel.Client, st *chatState, text string) (chatAction, bool) {
	fields := strings.Fields(text)
	command := fields[0]
	arg := strings.TrimSpace(strings.TrimPrefix(text, command))
	switch command {
	case "/help", "/h", "/?":
		a.chatHelp()
		return chatStay, true
	case "/quit", "/exit":
		return chatQuit, false
	case "/sessions", "/resume":
		sessionID, ok := a.chatPickSession(ctx, client, st.session.ID)
		if ok && sessionID != "" && sessionID != st.session.ID {
			session, err := client.ChatSession(ctx, sessionID)
			if err != nil {
				a.chatFail(st, "打不开会话：", err)
				return chatStay, true
			}
			st.session = session
			st.live = false
			st.scroll = 0
			return chatSwitchSession, true
		}
		return chatStay, true
	case "/new", "/clear":
		session, ok := a.chatNewSession(ctx, client, st)
		if ok {
			st.session = session
			st.live = false
			st.scroll = 0
			st.renderCache = map[string][]string{}
			return chatSwitchSession, true
		}
		return chatStay, true
	case "/assistant":
		assistantID, ok := a.chatPickAssistant(ctx, st)
		if !ok || assistantID == "" {
			return chatStay, true
		}
		session, err := client.ChatCreateSession(ctx, assistantID)
		if err != nil {
			a.chatFail(st, "新建会话失败：", err)
			return chatStay, true
		}
		st.session = session
		st.live = false
		st.scroll = 0
		st.renderCache = map[string][]string{}
		return chatSwitchSession, true
	case "/model":
		if arg == "" {
			picked, ok := a.chatPickModel(ctx, client, st)
			if !ok || picked == "" {
				return chatStay, true
			}
			arg = picked
		}
		model := arg
		session, err := client.ChatPatchSession(ctx, st.session.ID, panel.ChatSessionPatch{Model: &model})
		if err != nil {
			a.chatFail(st, "切换模型失败：", err)
			return chatStay, true
		}
		st.session = session
		st.notice = "模型已切换到 " + model
		return chatStay, true
	case "/title", "/rename":
		if arg == "" {
			st.notice = "当前标题：" + displayOr(st.session.Title, "（无）")
			return chatStay, true
		}
		title := arg
		session, err := client.ChatPatchSession(ctx, st.session.ID, panel.ChatSessionPatch{Title: &title})
		if err != nil {
			a.chatFail(st, "改标题失败：", err)
			return chatStay, true
		}
		st.session = session
		return chatStay, true
	case "/search":
		if assistantPreset(st, st.session.AssistantID) != "" {
			st.notice = "这个助手用 DeepSeek 官方搜索，面板的搜索开关对它不生效"
			return chatStay, true
		}
		status, err := client.ChatSearchStatus(ctx)
		if err != nil {
			a.chatFail(st, "读取搜索状态失败：", err)
			return chatStay, true
		}
		if !status.Configured {
			st.notice = "还没配置搜索服务商：聊天设置 → 联网搜索"
			return chatStay, true
		}
		st.search = !st.search
		if st.search {
			st.notice = "本轮起开启联网搜索"
		} else {
			st.notice = "已关闭联网搜索"
		}
		return chatStay, true
	case "/stop":
		if st.turnID == "" {
			st.notice = "当前没有正在生成的回答"
			return chatStay, true
		}
		if err := client.ChatCancel(ctx, st.turnID); err != nil {
			a.chatFail(st, "停止失败：", err)
		} else {
			st.notice = "已请求停止"
		}
		return chatStay, true
	case "/regen":
		node, ok := lastAssistantNode(st.session)
		if !ok {
			st.notice = "还没有可重新生成的回答"
			return chatStay, true
		}
		st.live = true
		st.liveText = ""
		st.liveThink = ""
		result, err := client.ChatRegenerate(ctx, st.session.ID, node.ID, "in_place", st.search)
		if err != nil {
			st.live = false
			a.chatFail(st, "重新生成失败：", err)
			return chatStay, true
		}
		st.turnID = result.TurnID
		return chatStay, true
	case "/edit":
		node, message, ok := lastUserNode(st.session)
		if !ok {
			st.notice = "还没有可编辑的提问"
			return chatStay, true
		}
		text, ok := a.prompt(promptPage{
			Title:    "编辑提问",
			Subtitle: "保存后会从这条消息重新生成后面的回答",
			Label:    "消息",
			Initial:  message.Content,
		})
		if !ok || strings.TrimSpace(text) == "" {
			return chatStay, true
		}
		st.live = true
		st.liveText = ""
		st.liveThink = ""
		result, err := client.ChatEdit(ctx, st.session.ID, node.ID, text)
		if err != nil {
			st.live = false
			a.chatFail(st, "编辑失败：", err)
			return chatStay, true
		}
		st.turnID = result.TurnID
		return chatStay, true
	case "/copy":
		if text, ok := lastAssistantText(st.session); ok {
			a.screen.CopyToClipboard(text)
			st.notice = "最后一条回答已复制到剪贴板"
		} else {
			st.notice = "还没有可复制的回答"
		}
		return chatStay, true
	case "/settings", "/config":
		if st.settings == nil {
			st.notice = "聊天设置需要开发者提权（Ctrl-E）"
			return chatStay, true
		}
		a.message("聊天设置", fmt.Sprintf(
			"默认模型：%s\n默认账户：%s\n搜索服务商：%s\n每日 token 上限：%d\n长时间输入阈值：%d tokens",
			displayOr(st.settings.DefaultModel, "（未设）"),
			displayOr(st.settings.DefaultAccount, "（未设）"),
			displayOr(st.settings.SearchProvider, "（未设）"),
			st.settings.DailyTokenLimit,
			st.settings.LongTextTokens))
		return chatStay, true
	case "/status":
		a.message("会话状态", fmt.Sprintf(
			"标题：%s\n模型：%s\n账户：%s\n搜索：%s\n正在生成：%v",
			displayOr(st.session.Title, "（无）"),
			displayOr(st.session.Model, "（默认）"),
			displayOr(st.session.Account, "（默认）"),
			onOff(st.search), st.live))
		return chatStay, true
	}
	// Unknown slash command: send it as a message, like Kimi Code does.
	return a.chatSend(ctx, client, st, text)
}

// chatFail puts a refusal in the status line, naming elevation when that is
// what the panel actually wants.
func (a *App) chatFail(st *chatState, prefix string, err error) {
	if needsElevation(err) {
		st.notice = "需要开发者提权：按 Ctrl-E"
		return
	}
	st.notice = prefix + explain(err)
}

// chatNeedsGuardElevation reports whether the panel's developer guard gates
// chat itself. Only scope=all does; instance/proxy scopes leave /api/chat open.
func chatNeedsGuardElevation(scope string) bool {
	return scope == "all"
}

func (a *App) chatHelp() {
	a.message("聊天命令与快捷键",
		"Enter 发送      Ctrl-J 换行      Ctrl-C 停止 / 清空\n"+
			"Ctrl-O 展开思考  Ctrl-B 会话列表  Ctrl-E 开发者提权\n"+
			"PgUp/PgDn 滚动    Esc 返回主菜单\n\n"+
			"/new /sessions /assistant /model [id] /title [text]\n"+
			"/search /stop /regen /edit /copy /settings /status /clear /help /quit")
}

func (a *App) chatPickSession(ctx context.Context, client *panel.Client, current string) (string, bool) {
	sessions, err := client.ChatSessions(ctx, "")
	if err != nil {
		a.message("会话列表读不到", explain(err))
		return "", false
	}
	if len(sessions) == 0 {
		a.message("还没有会话", "用 /new 或 /assistant 新建一个。")
		return "", false
	}
	items := make([]ui.Item, 0, len(sessions))
	for _, session := range sessions {
		label := session.Title
		if label == "" {
			label = session.ID
		}
		note := fmt.Sprintf("%s · %d 条", displayOr(session.Model, "默认"), session.MessageCount)
		if session.ID == current {
			note += " · 当前"
		}
		items = append(items, ui.Item{Label: label, Note: note})
	}
	choice, ok := a.menu(menuPage{
		Title:    "会话",
		Subtitle: "Enter 打开    Esc 返回聊天",
		Items:    items,
	})
	if !ok {
		return "", false
	}
	return sessions[choice].ID, true
}

// chatPickModel opens the model picker for the session's account. `/model <id>`
// still works without the menu; the menu is what makes the list discoverable.
func (a *App) chatPickModel(ctx context.Context, client *panel.Client, st *chatState) (string, bool) {
	account := st.session.Account
	if account == "" {
		a.message("当前会话没有账户", "先在网页版给会话或助手指定一个模型账户。")
		return "", false
	}
	models, err := client.ChatModels(ctx, account)
	if err != nil {
		a.message("模型列表读不到", explain(err))
		return "", false
	}
	if len(models) == 0 {
		a.message("账户没有模型列表", "在网页版「聊天设置 → 账户」刷新一次模型。")
		return "", false
	}
	current := displayOr(st.session.Model, settingsModel(st.settings))
	items := make([]ui.Item, 0, len(models))
	for _, model := range models {
		label := model.ID
		if model.ID == current {
			label += "  (当前)"
		}
		items = append(items, ui.Item{Label: label, Note: modelNote(model)})
	}
	choice, ok := a.menu(menuPage{
		Title:    "选择模型",
		Subtitle: "Enter 切换    Esc 返回",
		Items:    items,
	})
	if !ok {
		return "", false
	}
	return models[choice].ID, true
}

func modelNote(model panel.ChatModel) string {
	notes := make([]string, 0, 3)
	if model.Caps.Reasoning {
		notes = append(notes, "推理")
	}
	if model.Caps.Vision {
		notes = append(notes, "视觉")
	}
	if model.Caps.Files {
		notes = append(notes, "文件")
	}
	return strings.Join(notes, " · ")
}

func (a *App) chatPickAssistant(ctx context.Context, st *chatState) (string, bool) {
	if len(st.assistants) == 0 {
		assistants, err := a.client.ChatAssistants(ctx)
		if err != nil {
			a.message("助手列表读不到", explain(err))
			return "", false
		}
		st.assistants = assistants
	}
	if len(st.assistants) == 0 {
		a.message("没有可用的助手", "先在网页版创建一个角色。")
		return "", false
	}
	items := make([]ui.Item, 0, len(st.assistants))
	for _, assistant := range st.assistants {
		items = append(items, ui.Item{Label: assistant.Name, Note: assistant.Description})
	}
	choice, ok := a.menu(menuPage{
		Title:    "选择助手",
		Subtitle: "Enter 用这个助手新建会话    Esc 返回",
		Items:    items,
	})
	if !ok {
		return "", false
	}
	return st.assistants[choice].ID, true
}

func (a *App) chatNewSession(ctx context.Context, client *panel.Client, st *chatState) (panel.ChatSession, bool) {
	assistantID, ok := a.chatPickAssistant(ctx, st)
	if !ok || assistantID == "" {
		return panel.ChatSession{}, false
	}
	session, err := client.ChatCreateSession(ctx, assistantID)
	if err != nil {
		a.message("新建会话失败", explain(err))
		return panel.ChatSession{}, false
	}
	return session, true
}

// drawChat paints one frame.
func (a *App) drawChat(ctx context.Context, client *panel.Client, st *chatState) {
	width, height := a.screen.Size()
	if width < 20 {
		width = 20
	}
	if height < 8 {
		height = 8
	}
	inner := width - 4
	st.area.Width = inner
	st.area.MaxRows = 3

	lines := a.chatTranscript(st, inner)
	composerRows := st.area.Lines(true)
	composeHeight := len(composerRows)
	fixed := 1 + 1 + composeHeight + 1 + 1 // header, separator, composer, hint, status
	visible := height - fixed
	if visible < 1 {
		visible = 1
	}

	start := len(lines) - visible - st.scroll
	if start < 0 {
		start = 0
	}
	if start+visible > len(lines) {
		start = maxInt(0, len(lines)-visible)
	}
	var shown []string
	for index := start; index < len(lines) && len(shown) < visible; index++ {
		shown = append(shown, lines[index])
	}
	for len(shown) < visible {
		shown = append(shown, "")
	}

	out := make([]string, 0, height)
	header := " 聊天 · " + displayOr(st.session.Title, "新会话")
	if st.live {
		header += " · 生成中"
		if !st.turnStarted.IsZero() {
			header += fmt.Sprintf(" %ds", int(time.Since(st.turnStarted).Seconds()))
		}
	}
	status := displayOr(st.session.Model, settingsModel(st.settings))
	if st.search {
		status += " · 搜索"
	}
	if a.elevated() {
		status += " · 已提权"
	}
	out = append(out, chatHeaderRow(padBetween(header, status, width), width))

	for _, line := range shown {
		out = append(out, chatRow(line, width))
	}
	separator := "── 输入 "
	separator += strings.Repeat("─", maxInt(0, width-ui.VisibleWidth(separator)-1))
	out = append(out, chatDimRow(separator, width))
	for index, line := range composerRows {
		prefix := "   "
		if index == 0 {
			prefix = " ❯ "
		}
		out = append(out, chatRow(prefix+line, width))
	}
	hint := "Enter 发送 · Ctrl-J 换行 · Ctrl-O 展开 · Ctrl-B 会话 · Ctrl-C 停止 · Esc 返回"
	out = append(out, chatDimRow(hint, width))
	notice := st.notice
	if st.lastError != "" {
		notice = "错误：" + st.lastError
	}
	if len(st.completion) > 0 {
		notice = "命令：" + strings.Join(st.completion, "  ")
	}
	out = append(out, chatDimRow(" "+notice, width))
	for len(out) < height {
		out = append(out, chatRow("", width))
	}
	a.screen.Draw(out[:height])
}

func (a *App) chatTranscript(st *chatState, width int) []string {
	var lines []string
	if st.renderWidth != width {
		st.renderCache = map[string][]string{}
		st.renderWidth = width
	}
	for _, node := range st.session.Nodes {
		message, ok := node.Active()
		if !ok {
			continue
		}
		if node.Role == "user" {
			lines = append(lines, chatUser+"❯ 你 · "+chatTime(message.CreatedAt)+chatFg)
			lines = append(lines, wrapPlain(message.Content, width)...)
		} else {
			label := chatHelper + "● 助手 · " + chatTime(message.CreatedAt)
			if message.Status == "streaming" {
				label += " · 生成中"
			} else if message.Status == "error" {
				label += " · 失败"
			}
			lines = append(lines, label+chatFg)
			lines = append(lines, a.chatTimeline(st, message, width)...)
			lines = append(lines, a.chatMarkdown(st, message.Content, width)...)
			if len(message.Attachments) > 0 {
				var names []string
				for _, attachment := range message.Attachments {
					names = append(names, attachment.Name)
				}
				lines = append(lines, chatDimLine("附件："+strings.Join(names, " · ")))
			}
		}
		lines = append(lines, "")
	}
	if st.live {
		label := "助手 · 生成中"
		if !st.turnStarted.IsZero() {
			label += fmt.Sprintf(" %ds", int(time.Since(st.turnStarted).Seconds()))
		}
		if st.liveThink != "" {
			lines = append(lines, chatDimLine(label+" · 思考中"))
			if st.expand {
				lines = append(lines, wrapPlain(st.liveThink, width)...)
			} else {
				// Kimi Code's trick: two lines of reasoning by default, so a
				// long thinking phase never looks like a frozen screen.
				lines = append(lines, thinkingPreview(st.liveThink, width, 2)...)
			}
		} else {
			lines = append(lines, chatDimLine(label))
		}
		lines = append(lines, a.chatMarkdown(st, st.liveText, width)...)
		lines = append(lines, "")
	}
	if len(lines) == 0 {
		lines = append(lines, chatDimLine("还没有消息。输入内容后回车开始。"))
	}
	return lines
}

// chatTimeline picks the stored timeline shape: OpenAI protocol messages keep
// `parts`, the DeepSeek preset keeps `blocks`. Both render as the same kind of
// one-line summary per step.
func (a *App) chatTimeline(st *chatState, message panel.ChatMessage, width int) []string {
	if len(message.Parts) > 0 {
		return a.chatParts(st, message, width)
	}
	if len(message.Blocks) > 0 {
		return a.chatBlocks(st, message, width)
	}
	return nil
}

// chatParts renders the stored reasoning/tool timeline as one summary line per
// step; Ctrl-O expands the reasoning and source text.
func (a *App) chatParts(st *chatState, message panel.ChatMessage, width int) []string {
	calls := map[string]panel.ChatToolCall{}
	for _, part := range message.Parts {
		if part.Type == "tool_calls" {
			for _, call := range part.Calls {
				calls[call.ID] = call
			}
		}
	}
	var lines []string
	seen := map[string]bool{}
	arrow := "▸ "
	if st.expand {
		arrow = "▾ "
	}
	for _, part := range message.Parts {
		switch part.Type {
		case "thinking":
			duration := ""
			if part.DurationMs > 0 {
				duration = fmt.Sprintf(" %.1fs", float64(part.DurationMs)/1000)
			}
			lines = append(lines, chatDimLine(arrow+"思考"+duration))
			if strings.TrimSpace(part.Text) != "" {
				if st.expand {
					lines = append(lines, wrapPlain(part.Text, width)...)
				} else {
					lines = append(lines, thinkingPreview(part.Text, width, 2)...)
				}
			}
		case "tool":
			call := calls[part.ID]
			name := call.Name
			if name == "" {
				name = part.Name
			}
			query := queryOfArguments(call.Arguments)
			if query == "" {
				query = part.Query
			}
			label := "搜索"
			if strings.Contains(name, "scrape") || strings.Contains(name, "read_url") {
				label = "读取网页"
			}
			summary := label
			if query != "" {
				summary += "：" + query
			}
			if part.Hits > 0 {
				summary += fmt.Sprintf(" · %d 条", part.Hits)
			}
			lines = append(lines, chatDimLine(arrow+summary))
			if st.expand {
				for _, source := range part.Sources {
					if source.URL == "" || seen[source.URL] {
						continue
					}
					seen[source.URL] = true
					lines = append(lines, chatDimLine("  "+md.LinkLine(source.Title, source.URL)))
				}
			}
		}
	}
	return lines
}

// chatBlocks renders the DeepSeek-protocol timeline: thinking blocks and the
// server tools (deepseek_web_search / deepseek_read_url), whose results carry
// the sources.
func (a *App) chatBlocks(st *chatState, message panel.ChatMessage, width int) []string {
	sources := map[string][]panel.ChatSource{}
	for _, block := range message.Blocks {
		if strings.HasSuffix(block.Type, "_tool_result") && block.ToolUseID != "" {
			sources[block.ToolUseID] = blockSources(block.Content)
		}
	}
	var lines []string
	seen := map[string]bool{}
	arrow := "▸ "
	if st.expand {
		arrow = "▾ "
	}
	for _, block := range message.Blocks {
		switch block.Type {
		case "thinking":
			lines = append(lines, chatDimLine(arrow+"思考"))
			if strings.TrimSpace(block.Thinking) != "" {
				if st.expand {
					lines = append(lines, wrapPlain(block.Thinking, width)...)
				} else {
					lines = append(lines, thinkingPreview(block.Thinking, width, 2)...)
				}
			}
		case "server_tool_use":
			label := "搜索"
			if strings.Contains(block.Name, "read_url") {
				label = "读取网页"
			}
			query := blockQuery(block.Input)
			got := sources[block.ID]
			summary := label
			if query != "" {
				summary += "：" + query
			}
			if len(got) > 0 {
				summary += fmt.Sprintf(" · %d 条", len(got))
			}
			lines = append(lines, chatDimLine(arrow+summary))
			if st.expand {
				for _, source := range got {
					if source.URL == "" || seen[source.URL] {
						continue
					}
					seen[source.URL] = true
					lines = append(lines, chatDimLine("  "+md.LinkLine(source.Title, source.URL)))
				}
			}
		}
	}
	return lines
}

// thinkingPreview is the collapsed reasoning: the last `limit` wrapped lines of
// the text, indented, so a thinking phase visibly progresses without Ctrl-O.
func thinkingPreview(text string, width, limit int) []string {
	text = strings.TrimSpace(text)
	if text == "" {
		return nil
	}
	runes := []rune(text)
	if len(runes) > 2000 {
		runes = runes[len(runes)-2000:]
	}
	wrapped := wrapPlain(string(runes), maxInt(4, width-2))
	if len(wrapped) > limit {
		wrapped = wrapped[len(wrapped)-limit:]
	}
	lines := make([]string, 0, len(wrapped))
	for _, line := range wrapped {
		lines = append(lines, chatDimLine("  "+line))
	}
	return lines
}

func sessionHasTimeline(session panel.ChatSession) bool {
	for _, node := range session.Nodes {
		if message, ok := node.Active(); ok && (len(message.Parts) > 0 || len(message.Blocks) > 0) {
			return true
		}
	}
	return false
}

func assistantPreset(st *chatState, assistantID string) string {
	for _, assistant := range st.assistants {
		if assistant.ID == assistantID {
			return assistant.Preset
		}
	}
	return ""
}

func queryOfArguments(arguments string) string {
	if strings.TrimSpace(arguments) == "" {
		return ""
	}
	var value struct {
		Query   string   `json:"query"`
		Queries []string `json:"queries"`
		URL     string   `json:"url"`
	}
	if err := json.Unmarshal([]byte(arguments), &value); err != nil {
		return ""
	}
	if value.Query != "" {
		return value.Query
	}
	if len(value.Queries) > 0 {
		return strings.Join(value.Queries, " / ")
	}
	return value.URL
}

func blockQuery(raw json.RawMessage) string {
	if len(raw) == 0 {
		return ""
	}
	var value struct {
		Query   string   `json:"query"`
		Queries []string `json:"queries"`
		URL     string   `json:"url"`
	}
	if err := json.Unmarshal(raw, &value); err != nil {
		return ""
	}
	if value.Query != "" {
		return value.Query
	}
	if len(value.Queries) > 0 {
		return strings.Join(value.Queries, " / ")
	}
	return value.URL
}

func blockSources(raw json.RawMessage) []panel.ChatSource {
	if len(raw) == 0 {
		return nil
	}
	var list []panel.ChatSource
	if err := json.Unmarshal(raw, &list); err == nil && len(list) > 0 {
		return list
	}
	var one panel.ChatSource
	if err := json.Unmarshal(raw, &one); err == nil && one.URL != "" {
		return []panel.ChatSource{one}
	}
	return nil
}

func (a *App) chatMarkdown(st *chatState, text string, width int) []string {
	if text == "" {
		return nil
	}
	key := fmt.Sprintf("%d\x00%s", width, text)
	if cached, ok := st.renderCache[key]; ok {
		return cached
	}
	lines := md.Render(text, width)
	st.renderCache[key] = lines
	if len(st.renderCache) > 200 {
		st.renderCache = map[string][]string{key: lines}
	}
	return lines
}

func chatCompletions(value string) []string {
	commands := []string{
		"/help", "/h", "/?", "/new", "/clear", "/sessions", "/resume",
		"/assistant", "/model", "/title", "/rename", "/search", "/stop",
		"/regen", "/edit", "/copy", "/settings", "/config", "/status", "/quit",
	}
	var matches []string
	for _, command := range commands {
		if strings.HasPrefix(command, value) {
			matches = append(matches, command)
		}
	}
	return matches
}

func lastAssistantNode(session panel.ChatSession) (panel.ChatNode, bool) {
	for index := len(session.Nodes) - 1; index >= 0; index-- {
		node := session.Nodes[index]
		if node.Role != "assistant" {
			continue
		}
		if message, ok := node.Active(); ok && strings.TrimSpace(message.Content) != "" {
			return node, true
		}
	}
	return panel.ChatNode{}, false
}

func lastUserNode(session panel.ChatSession) (panel.ChatNode, panel.ChatMessage, bool) {
	for index := len(session.Nodes) - 1; index >= 0; index-- {
		node := session.Nodes[index]
		if node.Role != "user" {
			continue
		}
		if message, ok := node.Active(); ok {
			return node, message, true
		}
	}
	return panel.ChatNode{}, panel.ChatMessage{}, false
}

func lastAssistantText(session panel.ChatSession) (string, bool) {
	for index := len(session.Nodes) - 1; index >= 0; index-- {
		node := session.Nodes[index]
		if node.Role != "assistant" {
			continue
		}
		if message, ok := node.Active(); ok && strings.TrimSpace(message.Content) != "" {
			return message.Content, true
		}
	}
	return "", false
}

func displayOr(value, fallback string) string {
	if strings.TrimSpace(value) == "" {
		return fallback
	}
	return value
}

func settingsModel(settings *panel.ChatSettings) string {
	if settings == nil || strings.TrimSpace(settings.DefaultModel) == "" {
		return "默认"
	}
	return settings.DefaultModel
}

func onOff(value bool) string {
	if value {
		return "开"
	}
	return "关"
}

func chatTime(millis int64) string {
	if millis <= 0 {
		return ""
	}
	return time.UnixMilli(millis).Local().Format("01-02 15:04")
}

func wrapPlain(text string, width int) []string {
	text = stripControl(text)
	if width < 8 {
		width = 8
	}
	var lines []string
	for _, paragraph := range strings.Split(text, "\n") {
		if paragraph == "" {
			lines = append(lines, "")
			continue
		}
		runes := []rune(paragraph)
		used := 0
		var builder strings.Builder
		for _, r := range runes {
			cells := ui.VisibleWidth(string(r))
			if cells < 1 {
				cells = 1
			}
			if used+cells > width && used > 0 {
				lines = append(lines, builder.String())
				builder.Reset()
				used = 0
			}
			builder.WriteRune(r)
			used += cells
		}
		if builder.Len() > 0 {
			lines = append(lines, builder.String())
		}
	}
	return lines
}

// ---------------------------------------------------------------- styling --

func chatRow(content string, width int) string {
	content = ui.Truncate(content, width)
	missing := width - ui.VisibleWidth(content)
	if missing < 0 {
		missing = 0
	}
	return chatBody + content + strings.Repeat(" ", missing) + ui.Reset
}

func chatHeaderRow(content string, width int) string {
	content = ui.Truncate(content, width)
	missing := width - ui.VisibleWidth(content)
	if missing < 0 {
		missing = 0
	}
	return chatHeader + content + strings.Repeat(" ", missing) + ui.Reset
}

func chatDimRow(content string, width int) string {
	return chatRow(chatDim+content, width)
}

func chatDimLine(content string) string {
	return chatDim + content
}

func padBetween(left, right string, width int) string {
	space := width - ui.VisibleWidth(left) - ui.VisibleWidth(right)
	if space < 1 {
		space = 1
	}
	return left + strings.Repeat(" ", space) + right
}

func stripControl(text string) string {
	var builder strings.Builder
	for _, r := range text {
		if r == '\n' || r == '\t' || r >= 0x20 {
			builder.WriteRune(r)
		}
	}
	return builder.String()
}

func maxInt(a, b int) int {
	if a > b {
		return a
	}
	return b
}

// chatEntry opens the chat screen from the main menu.
//
// Reading the panel's chat settings needs developer elevation (the web UI asks
// for the same password there). Chat itself only needs the login, so the
// elevation prompt can be declined and the screen still works with the
// session's own model/search defaults.
func (a *App) chatEntry(ctx context.Context) {
	if a.client == nil {
		return
	}
	if chatNeedsGuardElevation(a.guardScope) {
		// scope=all gates every /api request, so chat cannot even list
		// sessions without the developer password. Ask first; declining
		// means chat is unavailable, and saying so is kinder than a 403.
		if !a.elevated() {
			if !a.confirm("聊天需要开发者密码",
				"面板的开发者守卫是 all 档：列会话、发消息都要开发者提权。现在输入开发者密码？") {
				a.message("聊天需要开发者密码", "开发者守卫是 all 档，未提权时聊天 API 全部拒绝。")
				return
			}
			a.elevate(ctx)
			if !a.elevated() {
				a.message("聊天需要开发者密码", "开发者守卫是 all 档，提权没有成功，聊天无法打开。")
				return
			}
		}
	} else if !a.elevated() {
		if a.confirm("聊天设置需要提权",
			"读取默认模型、搜索和每日限额需要开发者密码。现在提权？（跳过也能聊天，只是用会话自己的设置）") {
			a.elevate(ctx)
		}
	}
	sessions, err := a.client.ChatSessions(ctx, "")
	if err != nil && needsElevation(err) {
		if a.confirm("聊天需要开发者密码", "面板拒绝了请求，需要先开发者提权。现在输入开发者密码？") {
			a.elevate(ctx)
			if a.elevated() {
				sessions, err = a.client.ChatSessions(ctx, "")
			}
		}
	}
	if err != nil {
		a.message("会话列表读不到", explain(err))
		return
	}
	var sessionID string
	if len(sessions) > 0 {
		sessionID = sessions[0].ID
	} else {
		st := &chatState{}
		session, ok := a.chatNewSession(ctx, a.client, st)
		if !ok {
			return
		}
		sessionID = session.ID
	}
	a.chatLoop(ctx, a.client, sessionID)
}
