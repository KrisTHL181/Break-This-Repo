package app

import (
	"context"
	"fmt"
	"io"

	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/panel"
	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/pterm"
	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/rtty"
	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/ui"
)

// attach opens the terminal for one instance and pumps it until the agent
// exits or the user detaches. It returns to the actions menu either way: the
// login session and the panel connection stay alive.
func (a *App) attach(ctx context.Context, brand string, instance panel.Instance) {
	// The order is the web UI's: password → kernel → terminal. Asking for the
	// kernel first would put a question in front of a user who cannot open a
	// terminal at all yet.
	if !a.ensureElevated(ctx) {
		return
	}
	kernel, proceed := a.pickKernel(ctx, brand, instance.ID)
	if !proceed {
		return
	}
	a.busy("正在打开 " + instanceLabel(instance) + " 的终端…")

	terminal, ok := a.openWithElevation(ctx, brand, instance.ID, kernel)
	if !ok {
		return
	}
	a.runTerminal(ctx, terminal, terminalSession{
		detachNote: "agent 仍在后台运行。重新进来一次就能接回同一个会话。",
		onKill: func(ctx context.Context) {
			a.busy("正在停止终端…")
			killed, err := a.client.CloseTerminal(ctx, brand, instance.ID)
			if err != nil {
				a.message("停止终端失败", explain(err))
				return
			}
			if killed {
				a.message("终端已停止", "agent 进程已结束，实例本身没有受影响。")
			} else {
				a.message("没有终端在跑", "这个实例当前没有正在运行的 Web Shell 终端。")
			}
		},
	})
}

// terminalSession is what the caller knows about the terminal it just opened:
// what leaving means, and whether Ctrl-] k has an endpoint to call. A Minecraft
// console has none — the server must keep running when the operator leaves.
type terminalSession struct {
	detachNote string
	onKill     func(context.Context)
}

// runTerminal pumps one open terminal until it ends. It is shared by the agent
// terminal and the Minecraft console: the wire protocol is the same, only the
// words around "leave" differ.
func (a *App) runTerminal(ctx context.Context, terminal panel.Terminal, session terminalSession) {
	if terminal.Desktop() {
		a.message("这个面板没有 PTY", terminal.Hint)
		return
	}
	wsURL, err := a.client.WebSocketURL(terminal.WS)
	if err != nil {
		a.message("终端地址无效", explain(err))
		return
	}
	a.logf("终端已开: instance=%s brand=%s protocol=%d", terminal.Instance, terminal.Brand, terminal.Protocol)
	if terminal.Protocol != 0 && terminal.Protocol != pterm.Protocol {
		a.logf("面板的终端协议是 %d，本客户端实现的是 %d，仍会尝试连接", terminal.Protocol, pterm.Protocol)
	}

	conn, err := pterm.Dial(ctx, wsURL, a.client.Session(), a.tlsConfig)
	if err != nil {
		a.message("无法连接终端", explain(err))
		return
	}
	defer conn.Close()

	// Hand the real screen back to the terminal, then take it again afterwards.
	a.screen.Suspend()
	resumed := false
	defer func() {
		if !resumed {
			_ = a.screen.Resume()
		}
	}()
	input, feeder := io.Pipe()
	a.screen.AttachInput(feeder)
	defer func() {
		a.screen.DetachInput()
		_ = feeder.Close()
	}()
	result, runErr := rtty.Run(ctx, a.in, a.out, input, conn, a.logf)
	a.screen.DetachInput()
	_ = feeder.Close()
	if err := a.screen.Resume(); err == nil {
		resumed = true
	}

	if ctx.Err() != nil {
		a.quit = true
		return
	}
	if runErr != nil {
		a.message("终端连接中断", explain(runErr))
		return
	}
	switch result.Command {
	case pterm.CommandDetach:
		note := session.detachNote
		if note == "" {
			note = "仍在后台运行。重新进来一次就能接回同一个会话。"
		}
		a.message("已离开终端", note)
	case pterm.CommandKill:
		if session.onKill == nil {
			// No stop endpoint (a console): treat "kill" as leaving, because
			// killing the console is not a thing the server knows how to do.
			a.message("已离开控制台", "服务器继续运行。")
			return
		}
		session.onKill(ctx)
	default:
		if result.ExitKnown {
			a.message("终端已结束", fmt.Sprintf("对方自己退出了（退出码 %d）。面板会话仍然登录着。", result.ExitCode))
			return
		}
		if result.CloseCode != 0 {
			a.message("终端连接已关闭", fmt.Sprintf("面板关闭了连接（%d %s）。", result.CloseCode, result.CloseReason))
			return
		}
		a.message("终端连接已结束", "连接在没有 exit 帧的情况下结束了，通常表示面板重启或实例被删除。")
	}
}

// ensureElevated asks for the developer password before anything else in the
// attach flow, which is what makes the order password → kernel → terminal.
//
// The panel's own answer stays the authority: a status that cannot be read
// (an older panel, a transient failure) only means the client skips ahead, and
// openWithElevation still asks for the password when a 403 comes back.
func (a *App) ensureElevated(ctx context.Context) bool {
	status, err := a.client.DeveloperStatus(ctx)
	if err != nil {
		a.logf("读取开发者状态失败，直接尝试打开终端: %v", err)
		return true
	}
	if status.Elevated {
		return true
	}
	if !status.DevPasswordSet {
		// No terminal can open without a developer password — the panel needs
		// one to elevate at all — so a password prompt would be a dead end.
		a.message("无法打开终端", developerPasswordNotSet)
		return false
	}
	return a.elevate(ctx)
}

// openWithElevation asks the panel for a ticket. A missing or expired
// developer elevation is the one refusal that a password fixes, so it is
// handled here instead of at startup.
func (a *App) openWithElevation(ctx context.Context, brand, id, kernel string) (panel.Terminal, bool) {
	for attempt := 0; attempt < 2; attempt++ {
		terminal, err := a.client.OpenTerminal(ctx, brand, id, a.opts.Resume, kernel)
		if err == nil {
			return terminal, true
		}
		if attempt == 0 && needsElevation(err) {
			if !a.elevate(ctx) {
				return panel.Terminal{}, false
			}
			continue
		}
		a.message("无法打开终端", explain(err))
		return panel.Terminal{}, false
	}
	a.message("无法打开终端", "面板连续两次拒绝了打开终端的请求。")
	return panel.Terminal{}, false
}

func needsElevation(err error) bool {
	return panel.IsCode(err, "developer_elevation_required") ||
		panel.Message(err) == "developer_elevation_required"
}

// pickKernel asks which Claude Code kernel to start. Only Claude Web has more
// than one, and an installation with fewer than two usable kernels uses the
// panel's default instead of asking a question with a single answer.
//
// The boolean is false when the user backs out: no terminal should open.
func (a *App) pickKernel(ctx context.Context, brand, id string) (string, bool) {
	if brand != "claude-web" {
		return "", true
	}
	if a.opts.Kernel != "" {
		return a.opts.Kernel, true
	}
	kernels, preferred, err := a.client.TerminalKernels(ctx, brand, id)
	if err != nil {
		a.logf("读取内核列表失败，按面板默认打开: %v", err)
		return preferred, true
	}
	usable := make([]panel.Kernel, 0, len(kernels))
	for _, kernel := range kernels {
		if kernel.Available {
			usable = append(usable, kernel)
		}
	}
	if len(usable) < 2 {
		return preferred, true
	}
	items := make([]ui.Item, 0, len(usable)+1)
	for _, kernel := range usable {
		items = append(items, ui.Item{Label: kernel.Label, Note: kernelNote(kernel.ID)})
	}
	items = append(items, ui.Item{Label: "← 取消"})

	index, ok := a.menu(menuPage{
		Title:    "选择 Claude Code 内核",
		Subtitle: "网页里配置的第三方供应商只对社区引擎生效；官方内核只认 Anthropic 账号。",
		Items:    items,
	})
	if !ok || index >= len(usable) {
		return "", false
	}
	return usable[index].ID, true
}

func kernelNote(id string) string {
	if id == "official" {
		return "官方 Claude Code"
	}
	return "社区引擎"
}
