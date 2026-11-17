// Package app is the interactive client: the Debian-installer style screens
// that sit in front of the agent's terminal.
//
// It owns the flow — choose a panel, log in once, pick an instance, attach,
// come back — and nothing else. Every policy decision (HTTPS, the web shell
// switch, developer elevation) stays on the panel; this package only shows
// what the panel answered and gives the user a way back.
package app

import (
	"context"
	"crypto/tls"
	"errors"
	"io"
	"net/http"
	"os"
	"strings"
	"time"

	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/cli"
	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/panel"
	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/store"
	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/termx"
	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/ui"
)

// Options are what the command line hands to the app. The zero value is a
// client that starts at the server picker.
type Options struct {
	// NewClient builds a panel client for a base URL. The command line closes
	// over the TLS configuration (CA file, --insecure), so the app never
	// touches certificates.
	NewClient func(baseURL string) (*panel.Client, error)
	// Instance, Brand and Resume jump straight into one terminal instead of
	// showing the menus first. An empty Instance keeps the menus.
	Instance string
	Brand    string
	Resume   string
	// Kernel picks one of Claude Web's two CLI kernels (`community` or
	// `official`). Empty means "ask when there is a choice".
	Kernel string
	// User is the panel username to prefill (default admin).
	User string
	// TLSConfig is what the terminal WebSocket handshake uses; the REST
	// client already carries the same settings.
	TLSConfig *tls.Config
	// NewInsecureClient builds a client that does not verify the panel's
	// certificate. It is only called after the user has seen what the
	// certificate actually says and asked to connect anyway; nil means the
	// command line cannot offer that (verification is already off).
	NewInsecureClient func(baseURL string) (*panel.Client, error)
	// InsecureTLSConfig is the WebSocket counterpart of NewInsecureClient:
	// both halves of the connection must end up on the same policy.
	InsecureTLSConfig *tls.Config
	// Insecure is set when --insecure was given without a panel address: the
	// fingerprint ritual cannot run yet, so the app shows a warning page
	// before the server picker instead.
	Insecure bool
	// InitialServer skips the server picker once (an address given on the
	// command line).
	InitialServer string
	// Version is printed in the status line.
	Version string
	// Logf receives --verbose details.
	Logf func(format string, args ...any)
}

// App is one interactive session.
type App struct {
	opts   Options
	cfg    *store.Config
	screen *ui.Screen
	stderr io.Writer
	in     *os.File
	out    *os.File

	client   *panel.Client
	server   string
	username string
	health   panel.Health

	// tlsConfig is what the terminal handshake uses. It starts as the command
	// line gave it and is replaced when the user accepts a certificate that
	// does not prove the panel's identity, so REST and WebSocket never end up
	// on different policies.
	tlsConfig *tls.Config
	// unverified records that this run's connection was accepted without
	// certificate verification. It is memory only: nothing about it is saved.
	unverified bool

	// elevatedUntil mirrors the panel's elevation window, so the menus can
	// say "已提权" instead of offering the password again.
	elevatedUntil time.Time

	// guardScope is the panel's developer_guard.scope (off/proxy/instance/all).
	// At "all" every /api request needs the developer password, so the client
	// must ask before it can even list chat sessions.
	guardScope string

	// quit is set when the context was cancelled while an agent was attached:
	// the terminal is already back, so the app tears down at the next loop.
	quit bool
}

// Run takes over the terminal and returns the process exit code.
func Run(ctx context.Context, opts Options, in, out *os.File, stderr io.Writer, raw termx.Raw) (int, error) {
	screen, err := ui.Open(in, out, raw)
	if err != nil {
		return cli.ExitUsage, err
	}
	app := &App{opts: opts, screen: screen, stderr: stderr, in: in, out: out, tlsConfig: opts.TLSConfig}
	defer screen.Close()

	app.loadConfig()
	if app.cfg != nil {
		defer func() { _ = app.cfg.Save() }()
	}
	app.warnInsecure()
	return app.loop(ctx), nil
}

// warnInsecure is the --insecure confirmation when no address was given on the
// command line: without a host there is no certificate to fingerprint, so the
// user acknowledges the blanket risk instead.
func (a *App) warnInsecure() {
	if !a.opts.Insecure {
		return
	}
	index, ok := a.menu(menuPage{
		Title:    "证书校验已关闭",
		Subtitle: "--insecure 对接下来连接的每台面板都跳过证书校验。",
		Items: []ui.Item{
			{Label: "继续（我知道风险）"},
			{Label: "退出"},
		},
		Hint: "↑/↓ 选择    Enter 确定    Ctrl-C 退出",
	})
	if !ok || index != 0 {
		a.quit = true
	}
}

func (a *App) logf(format string, args ...any) {
	if a.opts.Logf != nil {
		a.opts.Logf(format, args...)
	}
}

// loadConfig reads the saved servers and session. A file that cannot be parsed
// is moved aside instead of silently overwritten: the servers list is small
// but it is not ours to delete.
func (a *App) loadConfig() {
	path, err := store.DefaultPath()
	if err != nil {
		a.cfg = &store.Config{Version: store.Version, RememberSession: true}
		a.message("配置不可用", err.Error())
		return
	}
	cfg, err := store.Load(path)
	if err == nil {
		a.cfg = cfg
		return
	}
	backup := path + ".bak"
	renamed := "原文件内容不动，直接重新开始。"
	if renameErr := os.Rename(path, backup); renameErr == nil {
		renamed = "原文件已改名为 " + backup + "，可以手工修好后换回来。"
	}
	a.message("配置文件读取失败", err.Error()+"\n\n"+renamed)
	fresh, _ := store.Load(path)
	a.cfg = fresh
}

func (a *App) save() {
	if a.cfg == nil {
		return
	}
	if err := a.cfg.Save(); err != nil {
		a.logf("保存配置失败: %v", err)
	}
}

// loop is the top of the hierarchy: server list → session → back to server
// list. Returning from the server list exits the program.
func (a *App) loop(ctx context.Context) int {
	for {
		if a.quit {
			return cli.ExitOK
		}
		server, ok := a.selectServer()
		if !ok {
			return cli.ExitOK
		}
		code, back := a.serveServer(ctx, server)
		if !back {
			return code
		}
	}
}

// ---------------------------------------------------------------- screens --

func (a *App) geometry() (width, height, inner, bodyHeight int) {
	width, height = a.screen.Size()
	inner, bodyHeight = ui.Geometry(width, height)
	return width, height, inner, bodyHeight
}

// flush drops keys typed while a network call was running, so they do not act
// on the screen that comes next.
func (a *App) flush() {
	for {
		select {
		case <-a.screen.Events():
		default:
			return
		}
	}
}

func (a *App) draw(title string, body []string, hint string) {
	width, height, _, _ := a.geometry()
	a.screen.Draw(ui.Render(ui.Page{
		Title:  title,
		Body:   body,
		Hint:   hint,
		Status: a.statusLine(),
	}, width, height))
}

func (a *App) statusLine() string {
	parts := []string{"HDSL-Web 终端客户端 " + a.opts.Version}
	if a.server != "" {
		parts = append(parts, a.server)
	}
	if a.username != "" {
		parts = append(parts, "用户 "+a.username)
	}
	if a.elevated() {
		parts = append(parts, "已提权至 "+a.elevatedUntil.Local().Format("15:04"))
	}
	if a.unverified {
		parts = append(parts, "证书未验证")
	}
	return strings.Join(parts, "  ·  ")
}

func (a *App) elevated() bool {
	return !a.elevatedUntil.IsZero() && time.Now().Before(a.elevatedUntil)
}

// menuPage is one selection screen.
type menuPage struct {
	Title    string
	Subtitle string
	// Detail is the text a user has to read before choosing, wrapped to the
	// dialog width and shown above the list. It is for warnings whose wording
	// is the point (the certificate dialog), not for decoration.
	Detail string
	Items  []ui.Item
	Hint   string
}

// menu runs a selection screen until the user confirms a row or backs out.
// The boolean is false for Esc / Ctrl-C.
func (a *App) menu(page menuPage) (int, bool) {
	a.flush()
	_, _, inner, bodyHeight := a.geometry()
	list := ui.List{Items: page.Items}
	if index := firstEnabled(page.Items); index >= 0 {
		list.Index = index
	}
	hint := page.Hint
	if hint == "" {
		hint = "↑/↓ 选择    Enter 确定    Esc 返回    Ctrl-C 取消"
	}

	for {
		header := 0
		body := make([]string, 0, bodyHeight)
		if page.Subtitle != "" {
			body = append(body, ui.Note(page.Subtitle, inner))
			header++
		}
		if page.Detail != "" {
			for _, line := range detailLines(page.Detail, inner-6, bodyHeight-5) {
				body = append(body, ui.Fill("  "+line, inner, ui.Body))
				header++
			}
		}
		body = append(body, ui.Blank(inner))
		header++
		rows := bodyHeight - header
		if rows < 1 {
			rows = 1
		}
		body = append(body, list.Lines(inner, rows)...)
		for len(body) < bodyHeight {
			body = append(body, ui.Blank(inner))
		}
		a.draw(page.Title, body, hint)

		event := <-a.screen.Events()
		switch event.Key {
		case ui.KeyUp:
			list.Move(-1)
		case ui.KeyDown:
			list.Move(1)
		case ui.KeyRune:
			// The help promises "↑/↓ 或 j/k", so the vim keys move the cursor
			// too. Only a menu moves on a letter: the prompt screens below feed
			// the same events to the input field, where j and k are text.
			switch event.Rune {
			case 'j':
				list.Move(1)
			case 'k':
				list.Move(-1)
			}
		case ui.KeyPgUp:
			list.Move(-rows)
		case ui.KeyPgDn:
			list.Move(rows)
		case ui.KeyHome:
			list.ToStart()
		case ui.KeyEnd:
			list.ToEnd()
		case ui.KeyEnter:
			item, ok := list.Selected()
			if ok && !item.Disabled {
				return list.Index, true
			}
		case ui.KeyEsc, ui.KeyInterrupt:
			return -1, false
		}
	}
}

func firstEnabled(items []ui.Item) int {
	for index, item := range items {
		if !item.Disabled {
			return index
		}
	}
	return -1
}

// promptPage is one input screen.
type promptPage struct {
	Title    string
	Subtitle string
	Label    string
	Initial  string
	Masked   bool
	// Replace is true when the initial value should be selected or, for a
	// password field, not shown at all.
	Clear bool
}

// prompt runs an input screen. Esc and Ctrl-C cancel, which is what makes
// "后悔登录了" recoverable: the password prompt never traps the user.
func (a *App) prompt(page promptPage) (string, bool) {
	a.flush()
	initial := []rune(page.Initial)
	if page.Clear {
		initial = nil
	}
	input := ui.Input{Value: initial, Cursor: len(initial), Masked: page.Masked}
	_, _, inner, bodyHeight := a.geometry()
	fieldWidth := inner - 6
	if fieldWidth > 72 {
		fieldWidth = 72
	}
	if fieldWidth < 12 {
		fieldWidth = 12
	}
	hint := "Enter 确定    Esc 返回    Ctrl-C 取消"

	for {
		body := make([]string, 0, bodyHeight)
		if page.Subtitle != "" {
			body = append(body, ui.Note(page.Subtitle, inner))
		} else {
			body = append(body, ui.Blank(inner))
		}
		body = append(body, ui.Blank(inner))
		body = append(body, ui.Heading("  "+page.Label, inner))
		body = append(body, ui.Blank(inner))
		for _, line := range input.Lines(fieldWidth, true) {
			body = append(body, ui.Row("  "+line, inner))
		}
		for len(body) < bodyHeight {
			body = append(body, ui.Blank(inner))
		}
		a.draw(page.Title, body, hint)

		event := <-a.screen.Events()
		switch event.Key {
		case ui.KeyEnter:
			return strings.TrimSpace(input.String()), true
		case ui.KeyEsc, ui.KeyInterrupt:
			return "", false
		}
		input.Handle(event)
	}
}

// message shows text until the user acknowledges it.
func (a *App) message(title, text string) {
	a.flush()
	_, _, inner, bodyHeight := a.geometry()
	body := []string{ui.Blank(inner)}
	for _, line := range wrap(text, inner-6) {
		body = append(body, ui.Fill("  "+line, inner, ui.Body))
	}
	body = append(body, ui.Blank(inner))
	body = append(body, ui.Fill("  < 确定 >", inner, ui.Selected))
	for len(body) < bodyHeight {
		body = append(body, ui.Blank(inner))
	}
	a.draw(title, body, "Enter / Esc 确定")

	for {
		event := <-a.screen.Events()
		switch event.Key {
		case ui.KeyEnter, ui.KeyEsc, ui.KeyInterrupt, ui.KeyEOF:
			return
		}
	}
}

// confirm asks a yes/no question. The safe answer is highlighted first.
func (a *App) confirm(title, question string) bool {
	index, ok := a.menu(menuPage{
		Title:    title,
		Subtitle: question,
		Items: []ui.Item{
			{Label: "取消"},
			{Label: "确定"},
		},
		Hint: "↑/↓ 选择    Enter 确定    Esc 取消",
	})
	return ok && index == 1
}

// busy paints a "please wait" page before a blocking request.
func (a *App) busy(text string) {
	a.flush()
	_, _, inner, bodyHeight := a.geometry()
	body := []string{ui.Blank(inner), ui.Fill("  "+text, inner, ui.Body)}
	for len(body) < bodyHeight {
		body = append(body, ui.Blank(inner))
	}
	a.draw("请稍候", body, "")
}

// detailLines wraps a menu's detail text and keeps it from pushing the choices
// off a short screen: the rows are the point, the explanation may end in "…".
func detailLines(text string, width, limit int) []string {
	lines := wrap(text, width)
	if limit > 0 && len(lines) > limit {
		lines = lines[:limit]
		if len(lines) > 0 {
			lines[len(lines)-1] = ui.Truncate(lines[len(lines)-1]+"…", width)
		}
	}
	return lines
}

// wrap breaks text to width cells, honouring explicit newlines.
func wrap(text string, width int) []string {
	if width < 8 {
		width = 8
	}
	var lines []string
	for _, paragraph := range strings.Split(text, "\n") {
		if paragraph == "" {
			lines = append(lines, "")
			continue
		}
		remaining := []rune(paragraph)
		for len(remaining) > 0 {
			used := 0
			cut := len(remaining)
			for index, r := range remaining {
				cell := ui.Width(string(r))
				if used+cell > width {
					cut = index
					break
				}
				used += cell
			}
			lines = append(lines, string(remaining[:cut]))
			remaining = remaining[cut:]
		}
	}
	return lines
}

// ------------------------------------------------------------- navigation --

// selectServer is the first screen: remembered panels, a new address, exit.
func (a *App) selectServer() (string, bool) {
	if a.opts.InitialServer != "" {
		server := a.opts.InitialServer
		a.opts.InitialServer = ""
		a.cfg.Touch(server, "", time.Now())
		a.save()
		return server, true
	}
	servers := a.cfg.RecentServers()
	items := make([]ui.Item, 0, len(servers)+2)
	for _, entry := range servers {
		items = append(items, ui.Item{Label: entry.URL, Note: serverNote(entry)})
	}
	newIndex := len(servers)
	items = append(items, ui.Item{Label: "＋ 输入新的服务器地址…"})
	items = append(items, ui.Item{Label: "退出程序"})

	index, ok := a.menu(menuPage{
		Title:    "选择服务器",
		Subtitle: "选择要连接的 HDSL-Web 面板。地址会记在本地，下次直接选。",
		Items:    items,
		Hint:     "↑/↓ 选择    Enter 确定    Esc / Ctrl-C 退出",
	})
	if !ok {
		return "", false
	}
	switch index {
	case newIndex:
		return a.newServer()
	case newIndex + 1:
		return "", false
	}
	return servers[index].URL, true
}

func serverNote(entry store.Server) string {
	note := entry.Username
	if !entry.LastUsed.IsZero() {
		if note != "" {
			note += " · "
		}
		note += "上次使用 " + entry.LastUsed.Local().Format("01-02 15:04")
	}
	return note
}

// newServer collects one address, validates its shape and remembers it before
// the network is touched: a typo is a local error, not a connection failure.
func (a *App) newServer() (string, bool) {
	for {
		address, ok := a.prompt(promptPage{
			Title:    "输入服务器地址",
			Subtitle: "例：https://panel.example.com:8443（带路径前缀也可以）",
			Label:    "面板地址",
			Initial:  "https://",
		})
		if !ok {
			return "", false
		}
		normalised, err := normaliseAddress(address)
		if err != nil {
			a.message("地址无效", err.Error())
			continue
		}
		a.cfg.Touch(normalised, "", time.Now())
		a.save()
		return normalised, true
	}
}

func normaliseAddress(address string) (string, error) {
	trimmed := strings.TrimSpace(address)
	if trimmed == "" {
		return "", errors.New("地址是空的")
	}
	if !strings.Contains(trimmed, "://") {
		trimmed = "https://" + trimmed
	}
	client, err := panel.New(trimmed, http.DefaultClient, nil)
	if err != nil {
		return "", err
	}
	return client.BaseURL().String(), nil
}

// serveServer connects, restores or asks for the login, and hands control to
// the main menu. back=true means the user chose another server.
func (a *App) serveServer(ctx context.Context, server string) (code int, back bool) {
	a.server = server
	a.username = ""
	a.elevatedUntil = time.Time{}
	a.unverified = false
	a.tlsConfig = a.opts.TLSConfig

	a.busy("正在连接 " + server + "…")
	client, err := a.opts.NewClient(server)
	if err != nil {
		a.message("地址无效", err.Error())
		return 0, true
	}
	health, err := client.Health(ctx)
	if err != nil {
		certificate, isCertificate := panel.CertificateFailure(err)
		if !isCertificate || a.opts.NewInsecureClient == nil {
			a.message("无法连接面板", explain(err))
			return 0, true
		}
		index, accepted := a.menu(certificateRiskPage(server, certificate))
		if !accepted || index != 1 {
			return 0, true
		}
		a.busy("正在以不验证证书的方式重连 " + server + "…")
		insecureClient, buildErr := a.opts.NewInsecureClient(server)
		if buildErr != nil {
			a.message("仍无法连接面板", buildErr.Error())
			return 0, true
		}
		health, err = insecureClient.Health(ctx)
		if err != nil {
			a.message("仍无法连接面板", "已经按你的选择跳过证书校验，连接还是失败：\n\n"+explain(err))
			return 0, true
		}
		client = insecureClient
		a.tlsConfig = a.opts.InsecureTLSConfig
		a.unverified = true
		a.logf("已按用户选择跳过证书校验（仅本次运行，未写入配置）")
	}
	a.client = client
	a.health = health
	a.logf("面板 %s 版本 %s，已运行 %d 秒", server, health.Version, health.UptimeSec)

	if !a.restoreSession(ctx, server) {
		if !a.login(ctx, server) {
			return 0, true
		}
	}
	// The developer guard's scope decides whether every /api call needs the
	// developer password. Reading it needs no elevation, so it is safe to ask
	// right after the session is established.
	a.refreshDeveloper(ctx)

	if a.opts.Instance != "" {
		a.instanceDirect(ctx)
	}
	return a.mainMenu(ctx)
}

// restoreSession reuses the saved cookie when the panel still accepts it.
func (a *App) restoreSession(ctx context.Context, server string) bool {
	saved, ok := a.cfg.SavedSession(server)
	if !ok {
		return false
	}
	a.client.SetSession(saved.Cookie)
	if _, err := a.client.Instances(ctx); err == nil {
		a.username = saved.Username
		if a.username == "" {
			a.username = a.cfg.UsernameFor(server)
		}
		a.logf("复用保存的登录（%s）", a.username)
		a.cfg.Touch(server, a.username, time.Now())
		a.save()
		return true
	} else if panel.Status(err) == http.StatusUnauthorized {
		a.client.SetSession("")
		a.cfg.ClearSession()
		a.save()
		a.logf("保存的登录已失效，需要重新登录")
		return false
	} else if panel.Status(err) == http.StatusForbidden {
		// At developer_guard.scope=all every API read is refused until the
		// operator elevates. The cookie is fine: ask the developer status,
		// which is on the guard's allowlist, and keep the session.
		if status, statusErr := a.client.DeveloperStatus(ctx); statusErr == nil {
			a.username = saved.Username
			if a.username == "" {
				a.username = a.cfg.UsernameFor(server)
			}
			a.guardScope = status.GuardScope
			a.applyElevation(status)
			a.cfg.Touch(server, a.username, time.Now())
			a.save()
			a.logf("保存的登录有效（开发者守卫 %s，聊天等 API 需要开发者密码）", status.GuardScope)
			return true
		}
		return false
	}
	// A transport failure here is not proof the cookie is bad: keep it.
	return false
}

// refreshDeveloper learns the guard scope and the current elevation window.
// Both endpoints are on the guard's allowlist, so this works at scope=all.
func (a *App) refreshDeveloper(ctx context.Context) {
	if a.client == nil {
		return
	}
	status, err := a.client.DeveloperStatus(ctx)
	if err != nil {
		return
	}
	a.guardScope = status.GuardScope
	a.applyElevation(status)
}

func (a *App) applyElevation(status panel.DeveloperStatus) {
	if !status.Elevated {
		return
	}
	if status.ExpiresAt == "" {
		a.elevatedUntil = time.Now().Add(30 * time.Minute)
		return
	}
	if expiry, err := time.Parse(time.RFC3339, status.ExpiresAt); err == nil {
		a.elevatedUntil = expiry
	}
}

func (a *App) login(ctx context.Context, server string) bool {
	username := a.cfg.UsernameFor(server)
	if a.opts.User != "" {
		username = a.opts.User
	}
	if username == "" {
		username = "admin"
	}
	// A wrong password must not resend the user through the username field:
	// after the first attempt the loop starts at the password prompt.
	askUsername := true
	for {
		if askUsername {
			answer, ok := a.prompt(promptPage{
				Title:    "登录面板",
				Subtitle: server,
				Label:    "用户名",
				Initial:  username,
			})
			if !ok {
				return false
			}
			if answer != "" {
				username = answer
			}
			askUsername = false
		}
		password, ok := a.prompt(promptPage{
			Title:    "登录面板",
			Subtitle: "用户 " + username + "（Ctrl-C 可以取消，回到服务器列表）",
			Label:    "密码",
			Masked:   true,
		})
		if !ok {
			return false
		}
		a.busy("正在登录 " + server + "…")
		name, err := a.client.Login(ctx, username, password)
		if err != nil {
			a.message("登录失败", explain(err)+"\n\n直接重新输入密码即可；要换用户名，按 Esc 回到服务器列表再进来一次。")
			continue
		}
		if name == "" {
			name = username
		}
		a.username = name
		a.cfg.Touch(server, name, time.Now())
		a.cfg.Remember(server, name, a.client.CookieValue(), time.Now())
		a.save()
		a.logf("已登录，会话已保存（%s）", a.cfg.Path())
		return true
	}
}

// mainMenu is the hub the client always returns to: attaching to an agent no
// longer ends the process, it comes back here.
func (a *App) mainMenu(ctx context.Context) (int, bool) {
	for {
		index, ok := a.menu(menuPage{
			Title:    "主菜单",
			Subtitle: "已连接 " + a.server + "（面板 " + a.health.Version + "）",
			Items: []ui.Item{
				{Label: "聊天", Note: "面板聊天的终端界面"},
				{Label: "选择实例并连接", Note: "进入 agent 的终端"},
				{Label: "给 agent 签票", Note: "生成提示词并复制"},
				{Label: "Minecraft 控制台", Note: "连接服务器自己的控制台"},
				{Label: "开发者提权", Note: elevationNote(a.elevatedUntil)},
				{Label: "会话与连接信息"},
				{Label: "清除保存的登录信息"},
				{Label: "切换服务器"},
				{Label: "退出程序"},
			},
			Hint: "↑/↓ 选择    Enter 确定    Esc 返回服务器列表",
		})
		if !ok {
			return 0, true
		}
		switch index {
		case 0:
			a.chatEntry(ctx)
		case 1:
			a.pickInstance(ctx)
		case 2:
			a.mintAgentTicket(ctx)
		case 3:
			a.mcConsole(ctx)
		case 4:
			a.elevate(ctx)
		case 5:
			a.sessionInfo()
		case 6:
			a.clearSavedLogin()
		case 7:
			return 0, true
		case 8:
			return cli.ExitOK, false
		}
	}
}

func elevationNote(until time.Time) string {
	if until.IsZero() || time.Now().After(until) {
		return "需要开发者密码"
	}
	return "有效至 " + until.Local().Format("15:04")
}

func (a *App) sessionInfo() {
	remember := "否"
	if saved, ok := a.cfg.SavedSession(a.server); ok {
		remember = "是（保存于 " + saved.SavedAt.Local().Format("01-02 15:04") + "）"
	}
	lines := []string{
		"面板地址：" + a.server,
		"面板版本：" + a.health.Version,
		"登录用户：" + a.username,
		"提权状态：" + elevationNote(a.elevatedUntil),
		"保存登录：" + remember,
		"配置文件：" + a.cfg.Path(),
	}
	a.message("会话与连接信息", strings.Join(lines, "\n"))
}

func (a *App) clearSavedLogin() {
	if !a.confirm("清除保存的登录信息", "删除本地保存的会话 Cookie？下次启动需要重新输入密码。") {
		return
	}
	a.cfg.ClearSession()
	if a.client != nil {
		a.client.SetSession("")
	}
	a.save()
	a.username = ""
	a.message("已清除", "本地会话已删除。本次仍保持登录，直到你切换服务器或退出。")
}

// elevate manages the developer elevation. Without one it asks for the
// password — the common path, and the only one a fresh session knows. With one
// it becomes the management door for the panel's two knobs: stretching the
// elevation to three hours and dropping it early.
func (a *App) elevate(ctx context.Context) bool {
	if !a.elevatedUntil.IsZero() && time.Now().Before(a.elevatedUntil) {
		index, ok := a.menu(menuPage{
			Title:    "开发者提权",
			Subtitle: "当前提权有效至 " + a.elevatedUntil.Local().Format("15:04") + "。",
			Items: []ui.Item{
				{Label: "延长到 3 小时", Note: "长时间操作前用，免得到期重输"},
				{Label: "停止提权", Note: "立即失效，开终端会重新要密码"},
				{Label: "重新验证", Note: "有效期重置为 30 分钟"},
				{Label: "← 返回"},
			},
			Hint: "↑/↓ 选择    Enter 确定    Esc 返回",
		})
		if !ok || index == 3 {
			return true
		}
		switch index {
		case 0:
			a.busy("正在延长提权…")
			elevation, err := a.client.ExtendElevation(ctx)
			if err != nil {
				a.message("延长失败", explain(err))
				return false
			}
			a.elevatedUntil = elevation.Expiry()
			if a.elevatedUntil.IsZero() {
				a.elevatedUntil = time.Now().Add(3 * time.Hour)
			}
			a.message("已延长", "提权现在有效至 "+a.elevatedUntil.Local().Format("15:04")+"。")
			return true
		case 1:
			if !a.confirm("停止提权", "立即结束当前提权？之后打开终端需要重新输入开发者密码。") {
				return true
			}
			a.busy("正在停止提权…")
			if _, err := a.client.RevokeElevation(ctx); err != nil {
				a.message("停止提权失败", explain(err))
				return false
			}
			a.elevatedUntil = time.Time{}
			a.message("已停止提权", "开发者提权已立即失效。")
			return true
		}
		// index == 2 falls through to re-verification below.
	}
	password, ok := a.prompt(promptPage{
		Title:    "开发者提权",
		Subtitle: "开发者密码用于打开 Web Shell，面板端 30 分钟内有效。",
		Label:    "开发者密码",
		Masked:   true,
	})
	if !ok {
		return false
	}
	a.busy("正在提权…")
	elevation, err := a.client.Elevate(ctx, password)
	if err != nil {
		a.message("提权失败", explain(err))
		return false
	}
	a.elevatedUntil = elevation.Expiry()
	if a.elevatedUntil.IsZero() {
		a.elevatedUntil = time.Now().Add(30 * time.Minute)
	}
	a.message("已提权", "现在可以打开 Web Shell，有效期至 "+a.elevatedUntil.Local().Format("15:04")+"。")
	return true
}

// developerPasswordNotSet is the one sentence for both ways this refusal
// arrives: the status answer (no developer password has ever been set) and the
// panel's own 409. The client cannot set one for the user.
const developerPasswordNotSet = "还没设置开发者密码：请在面板「设置 → 开发者」里设置后重试。"

// explain turns a panel error into a sentence a user can act on.
func explain(err error) string {
	if err == nil {
		return ""
	}
	var api *panel.APIError
	hasAPIError := errors.As(err, &api)
	switch panel.Code(err) {
	case "developer_elevation_required":
		return "需要开发者密码：在「主菜单 → 开发者提权」里输入一次。"
	case "developer_elevation_extended":
		return "这次提权已经延长过一次：重新验证开发者密码后会发一张新的延长票，剩余时间不会丢。"
	case "developer_password_not_set":
		return developerPasswordNotSet
	case "web_shell_disabled":
		return "面板关闭了 Web Shell（设置 → 开发者 → Web Shell 总开关）。"
	case "insecure_transport", "tls_required":
		return "面板要求 HTTPS 的连接：请用 https:// 地址访问。"
	case "brand_has_no_tui":
		return "这个品牌没有终端可以附着。"
	case "terminal_failed":
		// The panel puts the actionable part after the code — which profile
		// cannot hold the plugin, which kernel is missing — so it is kept
		// instead of being replaced by "check the panel log".
		reason := ""
		if hasAPIError {
			reason = strings.TrimSpace(strings.TrimPrefix(api.Message, "terminal_failed:"))
		}
		if reason == "" {
			return "面板启动终端失败：请查看面板日志。"
		}
		return "面板启动终端失败：" + reason
	}
	if hasAPIError && api.Message != "" {
		return api.Message
	}
	var transport *panel.TransportError
	if errors.As(err, &transport) {
		if certificate, ok := panel.CertificateFailure(err); ok {
			return "面板证书未通过校验：" + certificate.Reason
		}
		return "网络错误：" + transport.Error()
	}
	return err.Error()
}
