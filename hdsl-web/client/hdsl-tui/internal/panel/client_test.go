package panel_test

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"strings"
	"testing"

	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/panel"
	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/panel/fakeserver"
)

func newClient(t *testing.T, server *fakeserver.Server) *panel.Client {
	t.Helper()
	client, err := panel.New(server.URL, server.Client(), nil)
	if err != nil {
		t.Fatalf("panel.New: %v", err)
	}
	return client
}

func login(t *testing.T, client *panel.Client) {
	t.Helper()
	if _, err := client.Login(context.Background(), "admin", "secret"); err != nil {
		t.Fatalf("Login: %v", err)
	}
}

func TestLoginCapturesSessionCookie(t *testing.T) {
	server := fakeserver.New()
	defer server.Close()
	client := newClient(t, server)

	username, err := client.Login(context.Background(), "admin", "secret")
	if err != nil {
		t.Fatalf("Login: %v", err)
	}
	if username != "admin" {
		t.Errorf("username = %q, want admin", username)
	}
	if client.Cookie() == nil {
		t.Fatal("the session cookie was not captured")
	}
	if client.Cookie().Name != panel.SessionCookie {
		t.Errorf("cookie name = %q, want %q", client.Cookie().Name, panel.SessionCookie)
	}
	if client.Cookie().Value != "fake-session-token" {
		t.Errorf("cookie value = %q", client.Cookie().Value)
	}
	if !strings.Contains(client.Session(), panel.SessionCookie+"=fake-session-token") {
		t.Errorf("Session() = %q", client.Session())
	}
}

func TestLoginRejectsBadCredentials(t *testing.T) {
	server := fakeserver.New()
	defer server.Close()
	client := newClient(t, server)

	_, err := client.Login(context.Background(), "admin", "wrong")
	if err == nil {
		t.Fatal("expected an error")
	}
	if panel.Status(err) != http.StatusUnauthorized {
		t.Errorf("status = %d, want 401", panel.Status(err))
	}
	if client.Cookie() != nil {
		t.Error("a failed login must not leave a cookie behind")
	}
	if kind := panel.Classify(err); kind != panel.KindAuth {
		t.Errorf("classify = %v, want KindAuth", kind)
	}
}

func TestLoginWithoutCookieIsATransportFailure(t *testing.T) {
	server := fakeserver.New()
	defer server.Close()
	server.Handle("POST", "/api/auth/login", func(w http.ResponseWriter, r *http.Request) {
		fakeserver.WriteJSON(w, http.StatusOK, map[string]any{"ok": true, "username": "admin"})
	})
	client := newClient(t, server)

	_, err := client.Login(context.Background(), "admin", "secret")
	if err == nil {
		t.Fatal("expected an error when no cookie is handed out")
	}
	var transport *panel.TransportError
	if !errors.As(err, &transport) {
		t.Fatalf("error %v is not a TransportError", err)
	}
}

func TestHealth(t *testing.T) {
	server := fakeserver.New()
	defer server.Close()
	client := newClient(t, server)

	health, err := client.Health(context.Background())
	if err != nil {
		t.Fatalf("Health: %v", err)
	}
	if health.Status != "ok" || health.Version != "v0.5.4" || health.UptimeSec != 4242 {
		t.Errorf("health = %+v", health)
	}
}

func TestHealthOnUnreachablePanel(t *testing.T) {
	server := fakeserver.New()
	url := server.URL
	server.Close()

	client, err := panel.New(url, &http.Client{}, nil)
	if err != nil {
		t.Fatalf("panel.New: %v", err)
	}
	_, err = client.Health(context.Background())
	if err == nil {
		t.Fatal("expected an error")
	}
	if kind := panel.Classify(err); kind != panel.KindTransport {
		t.Errorf("classify = %v, want KindTransport", kind)
	}
}

func TestElevateSuccessAndFailures(t *testing.T) {
	server := fakeserver.New()
	defer server.Close()
	client := newClient(t, server)

	elevation, err := client.Elevate(context.Background(), "dev")
	if err != nil {
		t.Fatalf("Elevate: %v", err)
	}
	if !elevation.Elevated {
		t.Error("elevated = false")
	}
	if expiry := elevation.Expiry(); expiry.IsZero() {
		t.Errorf("expiresAt %q did not parse", elevation.ExpiresAt)
	}

	if _, err := client.Elevate(context.Background(), "nope"); err == nil {
		t.Error("expected an error for a wrong developer password")
	} else if panel.Message(err) != "invalid developer password" {
		t.Errorf("message = %q", panel.Message(err))
	} else if kind := panel.Classify(err); kind != panel.KindAuth {
		t.Errorf("classify = %v, want KindAuth", kind)
	}

	server.Handle("POST", "/api/developer/elevate", func(w http.ResponseWriter, r *http.Request) {
		fakeserver.WriteError(w, http.StatusConflict, "tls_required: the developer password is only accepted over HTTPS")
	})
	_, err = client.Elevate(context.Background(), "dev")
	if panel.Code(err) != "tls_required" {
		t.Errorf("code = %q, want tls_required", panel.Code(err))
	}
	if kind := panel.Classify(err); kind != panel.KindPolicy {
		t.Errorf("classify = %v, want KindPolicy", kind)
	}
}

func TestDeveloperStatus(t *testing.T) {
	server := fakeserver.New()
	defer server.Close()
	client := newClient(t, server)
	login(t, client)

	status, err := client.DeveloperStatus(context.Background())
	if err != nil {
		t.Fatalf("DeveloperStatus: %v", err)
	}
	if !status.DevPasswordSet || !status.Elevated || !status.WebShellEnabled || !status.HTTPS {
		t.Errorf("status = %+v, want every flag set", status)
	}

	server.Elevated = false
	server.DevPasswordSet = false
	status, err = client.DeveloperStatus(context.Background())
	if err != nil {
		t.Fatalf("DeveloperStatus: %v", err)
	}
	if status.Elevated || status.DevPasswordSet {
		t.Errorf("status = %+v, want both flags clear", status)
	}
	if !status.WebShellEnabled || !status.HTTPS {
		t.Errorf("status = %+v, want the unrelated flags untouched", status)
	}

	request, ok := server.LastRequestTo("/api/developer/status")
	if !ok || request.Method != http.MethodGet {
		t.Errorf("request = %+v (found %v), want a GET", request, ok)
	}
	if request.Cookie != "fake-session-token" {
		t.Errorf("cookie = %q, want the captured session", request.Cookie)
	}
}

func TestExtendAndRevokeElevation(t *testing.T) {
	server := fakeserver.New()
	defer server.Close()
	client := newClient(t, server)
	login(t, client)

	// Extending an existing elevation answers with a new expiry.
	extended, err := client.ExtendElevation(context.Background())
	if err != nil {
		t.Fatalf("ExtendElevation: %v", err)
	}
	if !extended.Elevated {
		t.Error("extended.elevated = false")
	}
	if expiry := extended.Expiry(); expiry.IsZero() {
		t.Errorf("expiresAt %q did not parse", extended.ExpiresAt)
	}

	// Without an elevation the panel refuses with the same code the terminal
	// path already understands.
	server.Elevated = false
	_, err = client.ExtendElevation(context.Background())
	if panel.Code(err) != "developer_elevation_required" {
		t.Errorf("code = %q, want developer_elevation_required", panel.Code(err))
	}

	// Revoking is idempotent and actually clears the fake panel's elevation.
	revoked, err := client.RevokeElevation(context.Background())
	if err != nil {
		t.Fatalf("RevokeElevation: %v", err)
	}
	if revoked.Elevated {
		t.Error("revoked.elevated = true")
	}
	if server.Elevated {
		t.Error("fake panel still reports elevated")
	}
}

func TestInstanceLists(t *testing.T) {
	server := fakeserver.New()
	defer server.Close()
	client := newClient(t, server)
	login(t, client)

	instances, err := client.Instances(context.Background())
	if err != nil {
		t.Fatalf("Instances: %v", err)
	}
	if len(instances) != 1 || instances[0].ID != "dsh-1" || instances[0].Name != "main" {
		t.Errorf("instances = %+v", instances)
	}

	kimi, err := client.BrandInstances(context.Background(), "kimi")
	if err != nil {
		t.Fatalf("BrandInstances: %v", err)
	}
	if len(kimi) != 1 || kimi[0].ID != "kimi-1" {
		t.Errorf("kimi instances = %+v", kimi)
	}

	empty, err := client.BrandInstances(context.Background(), "opencode")
	if err != nil {
		t.Fatalf("BrandInstances(opencode): %v", err)
	}
	if len(empty) != 0 {
		t.Errorf("opencode instances = %+v, want none", empty)
	}

	if _, ok := server.LastRequestTo("/api/brands/kimi/instances"); !ok {
		t.Error("the brand list endpoint was not called")
	}
}

func TestOpenTerminalPTY(t *testing.T) {
	server := fakeserver.New()
	defer server.Close()
	client := newClient(t, server)
	login(t, client)

	terminal, err := client.OpenTerminal(context.Background(), "", "dsh-1", "session-42", "")
	if err != nil {
		t.Fatalf("OpenTerminal: %v", err)
	}
	if terminal.Mode != "pty" || terminal.Protocol != 1 {
		t.Errorf("terminal = %+v", terminal)
	}
	if terminal.Ticket == "" || terminal.WS != "/ws/terminal/"+terminal.Ticket {
		t.Errorf("ticket/ws = %q / %q", terminal.Ticket, terminal.WS)
	}
	if terminal.Brand != "dsh" {
		t.Errorf("brand = %q", terminal.Brand)
	}
	request, ok := server.LastRequestTo("/api/instances/dsh-1/terminal")
	if !ok {
		t.Fatal("the terminal endpoint was not called")
	}
	if request.Query != "resume=session-42" {
		t.Errorf("query = %q", request.Query)
	}
	if request.Cookie != "fake-session-token" {
		t.Errorf("cookie = %q, want the captured session", request.Cookie)
	}

	ws, err := client.WebSocketURL(terminal.WS)
	if err != nil {
		t.Fatalf("WebSocketURL: %v", err)
	}
	if !strings.HasPrefix(ws, "ws://") || !strings.HasSuffix(ws, terminal.WS) {
		t.Errorf("ws url = %q", ws)
	}
}

func TestOpenTerminalBrandUsesTheBrandSurface(t *testing.T) {
	server := fakeserver.New()
	defer server.Close()
	client := newClient(t, server)
	login(t, client)

	terminal, err := client.OpenTerminal(context.Background(), "kimi", "kimi-1", "", "")
	if err != nil {
		t.Fatalf("OpenTerminal: %v", err)
	}
	if terminal.Brand != "kimi" {
		t.Errorf("brand = %q", terminal.Brand)
	}
	if terminalWS, err := client.WebSocketURL(terminal.WS); err != nil || !strings.HasSuffix(terminalWS, terminal.Ticket) {
		t.Errorf("ws url = %q (%v)", terminalWS, err)
	}
	if _, ok := server.LastRequestTo("/api/brands/kimi/instances/kimi-1/terminal"); !ok {
		t.Error("the brand terminal endpoint was not called")
	}
}

func TestOpenTerminalDesktopMode(t *testing.T) {
	server := fakeserver.New()
	defer server.Close()
	server.Terminal = func(w http.ResponseWriter, r *http.Request) bool {
		fakeserver.WriteJSON(w, http.StatusOK, map[string]any{
			"mode": "desktop", "instance": "dsh-1", "hint": "这台面板运行在 Windows 上",
		})
		return true
	}
	client := newClient(t, server)
	login(t, client)

	terminal, err := client.OpenTerminal(context.Background(), "", "dsh-1", "", "")
	if err != nil {
		t.Fatalf("OpenTerminal: %v", err)
	}
	if !terminal.Desktop() {
		t.Fatalf("mode = %q, want desktop", terminal.Mode)
	}
	if terminal.Hint == "" {
		t.Error("the desktop answer must carry a hint")
	}
}

func TestOpenTerminalErrorMapping(t *testing.T) {
	cases := []struct {
		status int
		body   string
		code   string
		kind   panel.Kind
	}{
		{http.StatusForbidden, "insecure_transport: the web shell requires HTTPS", "insecure_transport", panel.KindPolicy},
		{http.StatusForbidden, "web_shell_disabled: the web shell is disabled in developer settings", "web_shell_disabled", panel.KindPolicy},
		{http.StatusForbidden, "developer_elevation_required: elevate with the developer password first", "developer_elevation_required", panel.KindAuth},
		{http.StatusConflict, "developer_password_not_set: set it before opening a terminal", "developer_password_not_set", panel.KindAuth},
		{http.StatusConflict, "terminal_already_open: another terminal is attached", "terminal_already_open", panel.KindOther},
		// The panel refused to start the terminal: no retry fixes it, so it is
		// a policy refusal (exit 4) and not a transport failure (exit 3).
		{http.StatusConflict, "terminal_failed: no pty available", "terminal_failed", panel.KindPolicy},
		{http.StatusBadRequest, `confirmation_required: send {"confirm":"open-desktop-terminal"}`, "confirmation_required", panel.KindPolicy},
		{http.StatusNotFound, "instance not found", "", panel.KindOther},
		{http.StatusForbidden, "brand_has_no_tui: claude-web has no interactive terminal", "brand_has_no_tui", panel.KindPolicy},
	}
	for index, testCase := range cases {
		t.Run(fmt.Sprintf("case%d_%s", index, testCase.code), func(t *testing.T) {
			server := fakeserver.New()
			defer server.Close()
			message := testCase.body
			status := testCase.status
			server.Terminal = func(w http.ResponseWriter, r *http.Request) bool {
				fakeserver.WriteError(w, status, message)
				return true
			}
			client := newClient(t, server)
			login(t, client)

			_, err := client.OpenTerminal(context.Background(), "", "dsh-1", "", "")
			if err == nil {
				t.Fatal("expected an error")
			}
			if panel.Status(err) != testCase.status {
				t.Errorf("status = %d, want %d", panel.Status(err), testCase.status)
			}
			if panel.Code(err) != testCase.code {
				t.Errorf("code = %q, want %q", panel.Code(err), testCase.code)
			}
			if panel.Message(err) != testCase.body {
				t.Errorf("message = %q", panel.Message(err))
			}
			if kind := panel.Classify(err); kind != testCase.kind {
				t.Errorf("classify = %v, want %v", kind, testCase.kind)
			}
		})
	}
}

func TestNonJSONErrorBodyStillBecomesAnAPIError(t *testing.T) {
	server := fakeserver.New()
	defer server.Close()
	server.HandlePrefix("GET", "/api/health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "text/html")
		w.WriteHeader(http.StatusBadGateway)
		_, _ = w.Write([]byte("<html><body>502 Bad Gateway</body></html>"))
	})
	client := newClient(t, server)

	_, err := client.Health(context.Background())
	if err == nil {
		t.Fatal("expected an error")
	}
	if panel.Status(err) != http.StatusBadGateway {
		t.Errorf("status = %d", panel.Status(err))
	}
	if !strings.Contains(panel.Message(err), "502") {
		t.Errorf("message = %q", panel.Message(err))
	}
}

func TestCloseTerminal(t *testing.T) {
	server := fakeserver.New()
	defer server.Close()
	client := newClient(t, server)
	login(t, client)

	killed, err := client.CloseTerminal(context.Background(), "", "dsh-1")
	if err != nil {
		t.Fatalf("CloseTerminal: %v", err)
	}
	if !killed {
		t.Error("killed = false")
	}
	request, ok := server.LastRequestTo("/api/instances/dsh-1/terminal")
	if !ok || request.Method != http.MethodDelete {
		t.Errorf("request = %+v (found %v)", request, ok)
	}
}

func TestLoginBodyIsJSON(t *testing.T) {
	server := fakeserver.New()
	defer server.Close()
	client := newClient(t, server)
	login(t, client)

	request, ok := server.LastRequestTo("/api/auth/login")
	if !ok {
		t.Fatal("login was not called")
	}
	var body map[string]string
	if err := json.Unmarshal([]byte(request.Body), &body); err != nil {
		t.Fatalf("login body is not JSON: %v", err)
	}
	if body["username"] != "admin" || body["password"] != "secret" {
		t.Errorf("login body = %v", body)
	}
}

func TestNewRejectsBadBaseURLs(t *testing.T) {
	for _, raw := range []string{"", "panel.example.com", "ftp://panel.example.com", "https://"} {
		if _, err := panel.New(raw, nil, nil); err == nil {
			t.Errorf("panel.New(%q) accepted a bad address", raw)
		}
	}
	if _, err := panel.New("https://panel.example.com/panel/", nil, nil); err != nil {
		t.Errorf("panel.New with a path prefix failed: %v", err)
	}
}

func TestWebSocketURLKeepsTheMountPoint(t *testing.T) {
	cases := []struct {
		name string
		base string
		ws   string
		want string
		bad  bool
	}{
		{"root mount", "https://panel.example.com", "/ws/terminal/t", "wss://panel.example.com/ws/terminal/t", false},
		{"root mount with slash", "https://panel.example.com/", "/ws/terminal/t", "wss://panel.example.com/ws/terminal/t", false},
		{"one segment", "https://panel.example.com/hdsl", "/ws/terminal/t", "wss://panel.example.com/hdsl/ws/terminal/t", false},
		{"one segment with slash", "https://panel.example.com/hdsl/", "/ws/terminal/t", "wss://panel.example.com/hdsl/ws/terminal/t", false},
		{"other segment", "https://panel.example.com/panel", "/ws/terminal/t", "wss://panel.example.com/panel/ws/terminal/t", false},
		{"two segments", "https://panel.example.com/a/b/", "/ws/terminal/t", "wss://panel.example.com/a/b/ws/terminal/t", false},
		{"plain http", "http://127.0.0.1:8080/hdsl", "/ws/terminal/t", "ws://127.0.0.1:8080/hdsl/ws/terminal/t", false},
		{"no leading slash", "https://panel.example.com/hdsl", "ws/terminal/t", "wss://panel.example.com/hdsl/ws/terminal/t", false},
		{"already prefixed", "https://panel.example.com/hdsl", "/hdsl/ws/terminal/t", "wss://panel.example.com/hdsl/ws/terminal/t", false},
		{"query preserved", "https://panel.example.com/hdsl", "/ws/terminal/t?x=1&y=2", "wss://panel.example.com/hdsl/ws/terminal/t?x=1&y=2", false},
		{"escaped ticket", "https://panel.example.com/hdsl", "/ws/terminal/a%2Fb", "wss://panel.example.com/hdsl/ws/terminal/a%2Fb", false},
		{"absolute wss", "https://panel.example.com/hdsl", "wss://edge.example.com/ws/terminal/t", "wss://edge.example.com/ws/terminal/t", false},
		{"absolute https", "https://panel.example.com/hdsl", "https://edge.example.com/ws/x", "wss://edge.example.com/ws/x", false},
		{"absolute http", "http://panel.example.com", "http://edge.example.com/ws/x", "ws://edge.example.com/ws/x", false},
		{"bad scheme", "https://panel.example.com", "ftp://edge.example.com/x", "", true},
	}
	for _, testCase := range cases {
		t.Run(testCase.name, func(t *testing.T) {
			client, err := panel.New(testCase.base, nil, nil)
			if err != nil {
				t.Fatalf("panel.New(%q): %v", testCase.base, err)
			}
			got, err := client.WebSocketURL(testCase.ws)
			if testCase.bad {
				if err == nil {
					t.Fatalf("WebSocketURL(%q) = %q, want an error", testCase.ws, got)
				}
				return
			}
			if err != nil {
				t.Fatalf("WebSocketURL(%q): %v", testCase.ws, err)
			}
			if got != testCase.want {
				t.Errorf("WebSocketURL(%q) with base %q = %q, want %q", testCase.ws, testCase.base, got, testCase.want)
			}
			if strings.Contains(got, "//ws") || strings.Contains(got, "/hdsl/hdsl") {
				t.Errorf("WebSocketURL produced a doubled path: %q", got)
			}
		})
	}
}
