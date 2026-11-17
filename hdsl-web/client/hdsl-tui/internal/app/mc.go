package app

import (
	"context"
	"fmt"
	"strings"

	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/panel"
	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/ui"
)

// mcConsole is the Minecraft surface: list, start/stop, and attach to the
// server's own console. Creation and configuration stay on the web page —
// the client's job is the terminal, and a server console is a terminal.
func (a *App) mcConsole(ctx context.Context) {
	for {
		a.busy("正在读取 Minecraft 实例…")
		instances, err := a.client.MCInstances(ctx)
		if err != nil {
			if panel.Status(err) == 404 {
				a.message("Minecraft 功能未开启",
					"面板的 MC 功能没有启用（或已关闭）。请先在网页的「Minecraft」页开启。")
				return
			}
			if needsElevation(err) {
				if !a.elevate(ctx) {
					return
				}
				continue
			}
			a.message("读取失败", explain(err))
			return
		}
		if len(instances) == 0 {
			a.message("没有 MC 实例",
				"面板还没有管理任何 Minecraft 服务器。创建实例在网页的「Minecraft」页。")
			return
		}

		items := make([]ui.Item, 0, len(instances)+1)
		for _, instance := range instances {
			items = append(items, ui.Item{
				Label: mcLabel(instance),
				Note: fmt.Sprintf("%s · %s · 端口 %d",
					instance.Version, mcStateText(instance.State), instance.Port),
			})
		}
		items = append(items, ui.Item{Label: "← 返回主菜单"})

		index, ok := a.menu(menuPage{
			Title:    "Minecraft 控制台",
			Subtitle: "选择服务器。创建与配置在网页的「Minecraft」页。",
			Items:    items,
			Hint:     "↑/↓ 选择    Enter 确定    Esc 返回",
		})
		if !ok || index == len(items)-1 {
			return
		}
		a.mcActions(ctx, instances[index])
	}
}

func (a *App) mcActions(ctx context.Context, instance panel.MCInstance) {
	running := mcRunning(instance.State)
	items := []ui.Item{
		{Label: "连接控制台", Note: "Ctrl-] q 离开", Disabled: !running},
		{Label: "启动服务器", Disabled: running},
		{Label: "停止服务器", Disabled: !running},
		{Label: "查看详情"},
		{Label: "← 返回"},
	}
	index, ok := a.menu(menuPage{
		Title: "MC：" + mcLabel(instance),
		Subtitle: fmt.Sprintf("%s · %s · 端口 %d",
			instance.Version, mcStateText(instance.State), instance.Port),
		Items: items,
		Hint:  "↑/↓ 选择    Enter 确定    Esc 返回",
	})
	if !ok || index == len(items)-1 {
		return
	}
	switch index {
	case 0:
		a.attachMCConsole(ctx, instance)
	case 1:
		a.mcControl(ctx, instance, true)
	case 2:
		a.mcControl(ctx, instance, false)
	case 3:
		a.mcDetail(instance)
	}
}

// mcConsoleNotice is read before the attach on purpose: Ctrl-] q is the exit,
// and `stop` is a server command, not a way out.
const mcConsoleNotice = "接下来是服务器自己的控制台。\n\n" +
	"离开：先按 Ctrl-]，再按 q（服务器继续运行）。\n" +
	"不要输入 stop 来退出——那会真的停服。"

// attachMCConsole joins the server's own console. The words before the attach
// are the point: Ctrl-] q leaves, and `stop` is not an exit key — typing it
// stops the whole server.
func (a *App) attachMCConsole(ctx context.Context, instance panel.MCInstance) {
	if !a.ensureElevated(ctx) {
		return
	}
	a.message("进入 MC 控制台", mcConsoleNotice)
	a.busy("正在打开 " + mcLabel(instance) + " 的控制台…")

	terminal, err := a.mcTicket(ctx, instance.ID)
	if err != nil {
		a.message("无法打开控制台", explain(err))
		return
	}
	if terminal.Mode == "pipe" {
		a.message("这台服没有 PTY",
			"Windows 管道模式的控制台只能在网页的「Minecraft → 控制台」里看（日志 + 输入框），"+
				"本客户端无法附着。")
		return
	}
	a.runTerminal(ctx, terminal, terminalSession{
		detachNote: "服务器继续运行。重新进来一次就能接回控制台。",
		onKill:     nil,
	})
}

// mcTicket asks for the console ticket, retrying once after a password prompt
// when the panel says the elevation is missing.
func (a *App) mcTicket(ctx context.Context, id string) (panel.Terminal, error) {
	terminal, err := a.client.MCConsoleTicket(ctx, id)
	if err != nil && needsElevation(err) && a.elevate(ctx) {
		return a.client.MCConsoleTicket(ctx, id)
	}
	return terminal, err
}

func (a *App) mcControl(ctx context.Context, instance panel.MCInstance, start bool) {
	action := "启动"
	if !start {
		action = "停止"
	}
	if !a.confirm(action+"服务器", action+" "+mcLabel(instance)+"？") {
		return
	}
	if !a.ensureElevated(ctx) {
		return
	}
	a.busy("正在" + action + "…")
	var err error
	if start {
		err = a.client.MCStart(ctx, instance.ID)
	} else {
		err = a.client.MCStop(ctx, instance.ID)
	}
	if err != nil {
		a.message(action+"失败", explain(err))
		return
	}
	a.message("已提交", action+"请求已提交；状态会在列表刷新后更新。")
}

func (a *App) mcDetail(instance panel.MCInstance) {
	lines := []string{
		"名称：" + mcLabel(instance),
		"ID：" + instance.ID,
		"版本：" + instance.Version,
		fmt.Sprintf("端口：%d", instance.Port),
		"状态：" + mcStateText(instance.State),
	}
	if instance.Error != "" {
		lines = append(lines, "失败原因：", instance.Error)
	}
	if !instance.ConsoleAvailable {
		lines = append(lines, "控制台：不可用")
	}
	a.message("MC 详情", strings.Join(lines, "\n"))
}

func mcLabel(instance panel.MCInstance) string {
	if instance.Name != "" {
		return instance.Name
	}
	return instance.ID
}

func mcRunning(state string) bool {
	switch strings.ToLower(state) {
	case "running", "starting", "stopping":
		return true
	}
	return false
}

func mcStateText(state string) string {
	switch strings.ToLower(state) {
	case "running":
		return "运行中"
	case "starting":
		return "启动中"
	case "stopping":
		return "停止中"
	case "stopped":
		return "已停止"
	case "failed":
		return "失败"
	}
	return state
}
