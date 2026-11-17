package cli

import (
	"context"
	"errors"
	"net/http"
	"strings"
	"testing"

	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/panel"
	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/pterm"
)

func apiError(status int, message string) error {
	code := ""
	if prefix, _, found := strings.Cut(message, ":"); found && !strings.Contains(prefix, " ") {
		code = prefix
	}
	return &panel.APIError{Status: status, Message: message, Code: code}
}

func TestExplainMapsExitCodes(t *testing.T) {
	cases := []struct {
		name string
		err  error
		code int
		want string
	}{
		{"no error", nil, ExitOK, ""},
		{
			"insecure transport is a policy refusal",
			apiError(http.StatusForbidden, "insecure_transport: the web shell requires HTTPS"),
			ExitPolicy, "HTTPS",
		},
		{
			"web shell off is a policy refusal",
			apiError(http.StatusForbidden, "web_shell_disabled: the web shell is disabled in developer settings"),
			ExitPolicy, "设置",
		},
		{
			"tls_required is a policy refusal",
			apiError(http.StatusConflict, "tls_required: the developer password is only accepted over HTTPS"),
			ExitPolicy, "HTTPS",
		},
		{
			"emergency stop is a policy refusal",
			apiError(http.StatusServiceUnavailable, "emergency_stop: the panel is in emergency stop; terminals are frozen"),
			ExitPolicy, "紧急停机",
		},
		{
			"an estop cooldown is a policy refusal",
			apiError(http.StatusConflict, "estop_cooldown: the emergency stop cannot be released yet"),
			ExitPolicy, "冷却",
		},
		{
			"developer password not set is an auth failure",
			apiError(http.StatusConflict, "developer_password_not_set: set it before opening a terminal"),
			ExitAuth, "开发者密码",
		},
		{
			"elevation required is an auth failure",
			apiError(http.StatusForbidden, "developer_elevation_required: elevate with the developer password first"),
			ExitAuth, "提权",
		},
		{
			"bad login is an auth failure",
			apiError(http.StatusUnauthorized, "invalid username or password"),
			ExitAuth, "用户名或密码",
		},
		{
			"lockout is an auth failure",
			apiError(429, "too many failed attempts; this session is locked for 5 minutes"),
			ExitAuth, "锁定",
		},
		{
			"windows console confirmation is a policy refusal",
			apiError(http.StatusBadRequest, `confirmation_required: send {"confirm":"open-desktop-terminal"}`),
			ExitPolicy, "Windows",
		},
		{
			"terminal failure is a policy refusal",
			apiError(http.StatusConflict, "terminal_failed: no pty available"),
			ExitPolicy, "no pty available",
		},
		{
			"a dsh instance without a terminal plugin is a policy refusal",
			apiError(http.StatusConflict, "未安装终端界面插件，请先到实例的「安装」页安装一个 TUI 插件（推荐 @deepseek-harness-tui/dsh-tui）。"),
			ExitPolicy, "「安装」页",
		},
		{
			"unknown instance points at the picker",
			apiError(http.StatusNotFound, "instance not found"),
			ExitTransport, "--instance",
		},
		{
			"expired ticket needs a rerun",
			&pterm.CloseError{Code: 1008, Reason: "unknown or expired ticket"},
			ExitAuth, "1008",
		},
		{
			"no running terminal needs a rerun",
			&pterm.CloseError{Code: 1011, Reason: "no running terminal for this instance"},
			ExitTransport, "1011",
		},
		{
			"refused upgrade with an HTTP status",
			&pterm.CloseError{Code: http.StatusForbidden, Reason: "Forbidden"},
			ExitPolicy, "HTTPS",
		},
		{
			"an unreachable panel is a transport failure",
			&panel.TransportError{Op: "GET /api/health", Err: errors.New("connection refused")},
			ExitTransport, "无法连接面板",
		},
		{
			"a cancelled context is a transport failure",
			context.Canceled,
			ExitTransport, "中断",
		},
	}

	for _, testCase := range cases {
		t.Run(testCase.name, func(t *testing.T) {
			code, text := Explain(testCase.err)
			if code != testCase.code {
				t.Errorf("exit code = %d, want %d (text %q)", code, testCase.code, text)
			}
			if testCase.want == "" {
				if text != "" {
					t.Errorf("text = %q, want empty", text)
				}
				return
			}
			if !strings.Contains(text, testCase.want) {
				t.Errorf("text = %q, want it to mention %q", text, testCase.want)
			}
		})
	}
}

func TestExplainKeepsTheActionableWording(t *testing.T) {
	_, text := Explain(apiError(http.StatusForbidden, "insecure_transport: the web shell requires HTTPS"))
	for _, want := range []string{"HTTPS 证书", "https.trust_forwarded_proto"} {
		if !strings.Contains(text, want) {
			t.Errorf("text = %q, want it to mention %q", text, want)
		}
	}
}

func TestExplainNeverDumpsTheScreen(t *testing.T) {
	// A panel that echoes something huge must not end up in the terminal the
	// user is looking at.
	huge := "terminal_failed: " + strings.Repeat("x", 5000)
	_, text := Explain(apiError(http.StatusConflict, huge))
	if len(text) > 400 {
		t.Errorf("text is %d bytes long; error text must stay short", len(text))
	}
}

func TestExitCodeFor(t *testing.T) {
	cases := map[panel.Kind]int{
		panel.KindAuth:      ExitAuth,
		panel.KindTransport: ExitTransport,
		panel.KindPolicy:    ExitPolicy,
		panel.KindOther:     ExitTransport,
	}
	for kind, want := range cases {
		if got := ExitCodeFor(kind); got != want {
			t.Errorf("ExitCodeFor(%v) = %d, want %d", kind, got, want)
		}
	}
}

// A panel that refuses to start the terminal is a policy refusal (exit 4): the
// reason is a PTY it does not have or a plugin it cannot load, and no rerun
// fixes either. panel.Classify and cli.Explain disagreed about that — Classify
// called it "other", which reads as the transport failure (exit 3) — so the
// same failure had two exit codes depending on which table answered. Both are
// asserted here so the two cannot drift apart again.
func TestTerminalFailureIsPolicyOnBothPaths(t *testing.T) {
	err := apiError(http.StatusConflict, "terminal_failed: no pty available")
	if code, text := Explain(err); code != ExitPolicy {
		t.Errorf("Explain = %d (%q), want %d", code, text, ExitPolicy)
	}
	if code := ExitCodeFor(panel.Classify(err)); code != ExitPolicy {
		t.Errorf("ExitCodeFor(Classify) = %d, want %d", code, ExitPolicy)
	}
}
