// Package cli holds the parts of the client that are about the user rather
// than about the wire: the instance picker, the detach key state machine and
// the actionable wording of failures.
package cli

import (
	"context"
	"errors"
	"fmt"
	"strings"

	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/panel"
	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/pterm"
)

// explanations turns a panel error code into an actionable sentence. Every one
// of them says what to change, not just what failed.
var explanations = map[string]string{
	"insecure_transport":           "面板不在 HTTPS 下：请在「设置 → HTTPS 证书」里启用，或打开 https.trust_forwarded_proto",
	"tls_required":                 "面板不在 HTTPS 下：开发者密码只在 HTTPS 上接受，请在「设置 → HTTPS 证书」里启用，或打开 https.trust_forwarded_proto",
	"web_shell_disabled":           "Web Shell 已在面板里关闭：请在「设置 → 开发者」里打开后再连",
	"developer_password_not_set":   "还没设置开发者密码：请在面板「设置 → 开发者」里设置后重试",
	"developer_elevation_required": "需要先提权：重新运行本命令并输入开发者密码",
	"invalid developer password":   "开发者密码不正确",
	"invalid username or password": "用户名或密码不正确",
	"too many failed attempts":     "失败次数过多，会话已被临时锁定；请等锁定期结束后再试",
	"terminal_already_open":        "该实例已经有终端在运行；重新运行本命令会附着到它，不必另开",
	"confirmation_required":        "这台面板运行在 Windows 上：终端只能在面板所在机器的桌面上打开，无法远程接入",
	"brand_has_no_tui":             "该品牌没有交互式终端",
	"tui_only":                     "这是 TUI 品牌实例：没有网页可启动，请直接进入终端",
	"emergency_stop":               "面板处于紧急停机（E-Stop）：实例网页、终端与实例启动都被冻结，冷却结束或重启面板后才能恢复",
	"estop_cooldown":               "紧急停机的冷却期还没结束：到点前谁都不能解除（重启面板进程或容器会清空）",
}

// Explain turns a failure into the exit code and the sentence the user gets.
// It never echoes panel internals and never dumps anything from the screen.
func Explain(err error) (int, string) {
	if err == nil {
		return ExitOK, ""
	}

	var closeErr *pterm.CloseError
	if errors.As(err, &closeErr) {
		if closeErr.Code > 0 && closeErr.Code < 1000 {
			// A refused upgrade reports an HTTP status, not a close code.
			return explainHTTPStatus(closeErr.Code, closeErr.Reason)
		}
		switch closeErr.Code {
		case 1008:
			return ExitAuth, "服务端拒绝了这个连接（1008）：ticket 已过期或不属于当前会话；重新运行本命令即可"
		case 1011:
			return ExitTransport, "服务端没有正在运行的终端（1011）；重新运行本命令即可"
		}
		return ExitTransport, fmt.Sprintf("终端连接已断开（%d %s）", closeErr.Code, closeErr.Reason)
	}

	if errors.Is(err, context.Canceled) || errors.Is(err, context.DeadlineExceeded) {
		return ExitTransport, "连接已中断"
	}

	var transport *panel.TransportError
	if errors.As(err, &transport) {
		// A certificate that does not prove the panel's identity is the one
		// transport failure with an action attached; the interactive client
		// offers it as a screen, this is the scripted equivalent.
		if certificate, ok := panel.CertificateFailure(err); ok {
			return ExitTransport, "面板证书未通过校验：" + certificate.Reason +
				"；确认这个地址就是那台面板后，用 --ca <证书文件> 信任它，或用 --insecure 跳过校验（会失去身份保护）"
		}
		return ExitTransport, "无法连接面板：" + transport.Error()
	}

	code := panel.Code(err)
	if text, ok := explanations[code]; ok && code != "" {
		return ExitCodeFor(panel.Classify(err)), text
	}

	message := panel.Message(err)
	if text, ok := explanationForMessage(message); ok {
		return ExitCodeFor(panel.Classify(err)), text
	}
	switch {
	case message == "":
		return ExitTransport, "请求失败：" + shorten(err.Error())
	case panel.IsCode(err, "terminal_failed"):
		// ExitCodeFor(panel.Classify(err)) says the same thing: the panel's
		// terminal_failed is a policy code. The wording lives here because the
		// panel's reason is the part the user can act on.
		return ExitPolicy, "终端启动失败：" + strings.TrimPrefix(shorten(message), "terminal_failed: ")
	case strings.Contains(message, "not installed"):
		return ExitPolicy, "这个版本还没安装：" + shorten(message)
	case strings.Contains(message, "未安装终端界面插件"):
		// The dsh terminal is a plugin, and an instance without one is refused
		// with that sentence as plain text — no `code:` prefix — so nothing
		// else would classify it and a 409 would read like a network fault.
		// Retrying cannot fix it: the plugin is installed from the panel, and
		// the panel's own sentence says which page.
		return ExitPolicy, shorten(message)
	case panel.Status(err) == 404:
		return ExitTransport, "找不到该实例：dsh 实例直接写 id，品牌实例（kimi/opencode/claude-web/hermes/pi-web/qwen/mimo/minimax/zcode）要加 --brand；" +
			"不带 --instance 运行可以列出全部实例"
	}
	return ExitCodeFor(panel.Classify(err)), shorten(message)
}

// explanationForMessage matches whole messages and messages that carry extra
// detail after the known prefix, e.g. "too many failed attempts; this session
// is locked for 5 minutes".
func explanationForMessage(message string) (string, bool) {
	if message == "" {
		return "", false
	}
	for key, text := range explanations {
		if message == key || strings.HasPrefix(message, key+" ") || strings.HasPrefix(message, key+";") {
			return text, true
		}
	}
	return "", false
}

// maxMessageBytes keeps a panel-supplied sentence from flooding the terminal.
const maxMessageBytes = 200

func shorten(text string) string {
	if len(text) <= maxMessageBytes {
		return text
	}
	return text[:maxMessageBytes] + "…"
}

func explainHTTPStatus(status int, reason string) (int, string) {
	switch status {
	case 401:
		return ExitAuth, "认证失败：请检查用户名和密码"
	case 403:
		return ExitPolicy, "服务端拒绝了这次请求：请检查面板的 HTTPS、Web Shell 与开发者设置"
	case 404:
		return ExitTransport, "面板地址不对或接口不存在：请检查 URL"
	}
	return ExitTransport, fmt.Sprintf("服务端返回 HTTP %d %s", status, reason)
}
