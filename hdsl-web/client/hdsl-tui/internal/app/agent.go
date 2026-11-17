package app

import (
	"context"
	"fmt"
	"net/url"
	"strings"

	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/panel"
	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/ui"
)

// agentScope is one capability class the panel lets a ticket carry, in the
// order the page lists them.
type agentScope struct {
	ID    string
	Label string
	Hint  string
}

var agentScopes = []agentScope{
	{"read", "读取面板", "实例、日志、任务"},
	{"mc", "Minecraft", "启停服务器、控制台命令"},
	{"mc-console", "MC 控制台", "附着 PTY 控制台，实时读写"},
	{"system", "系统性能", "CPU、内存、磁盘读数"},
	{"instances", "管理实例", "建删启停其它实例"},
	{"config", "面板配置", "账号、模型、TLS"},
	{"plugins", "插件与整合包", "装/卸插件、整合包"},
}

// mintAgentTicket signs a bearer grant for an agent and offers the prompt that
// carries it. The ticket is minted by the operator's elevated session, not by
// the agent password: this is the "someone opened the panel and copied it"
// path the panel distinguishes from a password mint.
func (a *App) mintAgentTicket(ctx context.Context) {
	if !a.elevated() {
		a.message("需要开发者提权", "签票要用开发者密码提权。请先在主菜单选择「开发者提权」。")
		return
	}
	selected := map[string]bool{"read": true}
	for {
		items := make([]ui.Item, 0, len(agentScopes)+2)
		for _, scope := range agentScopes {
			mark := "[ ]"
			if selected[scope.ID] {
				mark = "[x]"
			}
			items = append(items, ui.Item{Label: mark + " " + scope.Label, Note: scope.Hint})
		}
		chosen := 0
		for _, scope := range agentScopes {
			if selected[scope.ID] {
				chosen++
			}
		}
		items = append(items,
			ui.Item{Label: "生成票", Note: fmt.Sprintf("已勾选 %d 类", chosen)},
			ui.Item{Label: "← 返回"},
		)

		index, ok := a.menu(menuPage{
			Title:    "给 agent 签票",
			Subtitle: "Enter 切换勾选；选「生成票」结束。票与开发者提权同生命周期。",
			Items:    items,
			Hint:     "↑/↓ 选择    Enter 切换/确定    Esc 返回",
		})
		if !ok || index == len(items)-1 {
			return
		}
		if index < len(agentScopes) {
			id := agentScopes[index].ID
			selected[id] = !selected[id]
			continue
		}

		scopes := make([]string, 0, len(agentScopes))
		for _, scope := range agentScopes {
			if selected[scope.ID] {
				scopes = append(scopes, scope.ID)
			}
		}
		if len(scopes) == 0 {
			a.message("范围为空", "至少要勾选一类范围。")
			continue
		}

		a.busy("正在签票…")
		ticket, err := a.client.MintAgentToken(ctx, "hdsl-tui", scopes)
		if err != nil {
			a.message("签票失败", explain(err))
			return
		}
		prompt := agentPrompt(a.server, ticket)
		a.screen.CopyToClipboard(prompt)
		// The token gets its own screen on purpose: a terminal that ignores
		// OSC 52 still leaves the operator a single line to copy by hand or
		// type into the agent's own TUI.
		a.message("票已签发", "Token（单独一行，可手动输入到 agent 的终端）：\n\n"+
			ticket.Token+"\n\n范围："+scopeText(ticket.Scopes)+"\n"+expiryLine(ticket))
		a.message("给 agent 的提示词", prompt+
			"\n\n已请求终端复制；终端不支持 OSC 52 时，用鼠标选中上面的文字复制。")
		return
	}
}

// agentPrompt is the text an operator pastes into the agent's input box. The
// panel address is the loopback one: the prompt ends up in a transcript, and
// the real host name has no business there.
func agentPrompt(server string, ticket panel.AgentToken) string {
	return strings.Join([]string{
		"你现在获得了一张 HDSL-web 面板的授权票。",
		"",
		"面板：" + loopbackPanelURL(server),
		"用法：/api 请求带上 Authorization: Bearer " + ticket.Token,
		"范围：" + scopeText(ticket.Scopes),
		expiryLine(ticket),
	}, "\n")
}

// expiryLine is the one sentence both screens repeat: the ticket inherits the
// developer window, and the window's end is the ticket's latest possible end.
func expiryLine(ticket panel.AgentToken) string {
	if at := ticket.Expiry(); !at.IsZero() {
		return "有效期：与开发者提权同生命周期，最晚到 " + at.Local().Format("2006-01-02 15:04")
	}
	return "有效期：与开发者提权同生命周期"
}

// loopbackPanelURL rewrites the connected address to 127.0.0.1. Port 443 (or
// no port) means the panel is behind a proxy; its own listener is the default
// 3080, which is what an agent on the same machine reaches.
func loopbackPanelURL(server string) string {
	parsed, err := url.Parse(server)
	if err != nil {
		return server
	}
	port := parsed.Port()
	if port == "" || port == "443" {
		port = "3080"
	}
	return "https://127.0.0.1:" + port
}

func scopeText(scopes []string) string {
	names := make([]string, 0, len(scopes))
	for _, id := range scopes {
		name := id
		for _, scope := range agentScopes {
			if scope.ID == id {
				name = scope.Label
				break
			}
		}
		names = append(names, name)
	}
	if len(names) == 0 {
		return "（无）"
	}
	return strings.Join(names, "、")
}
