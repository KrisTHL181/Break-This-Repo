// Package rtty runs one attached terminal: raw mode on the local terminal,
// window-size forwarding, the detach key filter and the byte pump between the
// terminal and the panel's WebSocket.
//
// It is shared by the interactive client (which suspends its menu around it)
// and by the plain line-based mode, so the two can never drift apart on the
// details that matter: Ctrl-C must reach the agent, Ctrl-] must not.
package rtty

import (
	"context"
	"io"
	"os"

	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/cli"
	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/pterm"
	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/termx"
)

// Run attaches until the agent exits, the socket closes, the user detaches or
// ctx is cancelled. The caller owns the connection and the screen around it.
//
// term is the terminal whose raw mode and size matter; input is where the
// keystrokes arrive. They are the same file in the plain flow, and a pipe in
// the interactive one — there the UI's single reader forwards the bytes,
// because a second reader would swallow the keystroke that detaches.
func Run(ctx context.Context, term, out *os.File, input io.Reader, conn *pterm.Conn, logf func(string, ...any)) (pterm.Result, error) {
	local := localTerminal(term, out, logf)
	restore := rawMode(local, logf)
	defer restore()

	sizeCtx, stopSize := context.WithCancel(ctx)
	defer stopSize()
	go func() {
		if err := local.Poll(sizeCtx, func(size termx.Size) {
			logf("窗口尺寸 %dx%d", size.Cols, size.Rows)
			_ = conn.SendResize(size.Cols, size.Rows)
		}); err != nil {
			logf("尺寸监听结束: %v", err)
		}
	}()
	initial, sizeErr := local.Size()
	if sizeErr == nil {
		// The agent needs a size before its first paint, or it guesses 80x24.
		logf("初始尺寸 %dx%d", initial.Cols, initial.Rows)
		if sendErr := conn.SendResize(initial.Cols, initial.Rows); sendErr != nil {
			logf("初始尺寸未发送: %v", sendErr)
		}
	} else {
		logf("读不到本地窗口尺寸: %v", sizeErr)
	}

	detach := &cli.Detach{}
	result, err := conn.Run(ctx, pterm.Options{
		In:     input,
		Out:    out,
		Logf:   logf,
		Filter: detach.Feed,
		OnReady: func(control pterm.Control) {
			logf("ready: instance=%s brand=%s protocol=%d", control.Instance, control.Brand, control.Protocol)
			if control.Protocol != 0 && control.Protocol != pterm.Protocol {
				logf("警告: 面板的终端协议是 %d，本客户端实现的是 %d", control.Protocol, pterm.Protocol)
			}
			// Send the size once more now that the terminal is really open: a
			// TUI that was already running may have missed the first one (it
			// is sent before the panel finishes the handshake), and a stale
			// size means it paints wider than the window.
			if sizeErr == nil {
				_ = conn.SendResize(initial.Cols, initial.Rows)
			}
		},
	})
	restore()
	return result, err
}

// localTerminal uses the real terminal when there is one and a no-op stand-in
// otherwise, so the client still works with redirected stdin/stdout.
func localTerminal(in, out *os.File, logf func(string, ...any)) termx.Raw {
	if !termx.IsTerminal(in) || !termx.IsTerminal(out) {
		logf("标准输入/输出不是终端，已跳过 raw 模式")
		return termx.Nop{}
	}
	local, err := termx.Open(in, out)
	if err != nil {
		logf("无法初始化终端，已跳过 raw 模式: %v", err)
		return termx.Nop{}
	}
	return local
}

// rawMode enters raw mode and returns a restore function that is safe to call
// more than once.
func rawMode(local termx.Raw, logf func(string, ...any)) func() {
	restore, err := local.Enter()
	if err != nil {
		logf("进入 raw 模式失败: %v", err)
		return func() {}
	}
	done := false
	return func() {
		if done {
			return
		}
		done = true
		if err := restore(); err != nil {
			logf("恢复终端失败: %v", err)
		}
	}
}
