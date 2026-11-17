// Package fakeserver is a scripted stand-in for a running panel. It answers
// the REST surface the client depends on with the shapes documented in
// docs/terminal-protocol.md, and lets a test override any route to exercise an
// error path.
//
// It deliberately does not import internal/panel: the panel package's own
// tests use it, and a package under test may not be imported by its helpers.
package fakeserver

import (
	"encoding/json"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"sync"
)

// Request is one call the fake panel received, kept for assertions.
type Request struct {
	Method string
	Path   string
	Query  string
	Body   string
	Cookie string
}

// Server is the fake panel.
type Server struct {
	*httptest.Server

	// Login credentials the fake accepts.
	Username string
	Password string
	// DeveloperPassword is accepted by /api/developer/elevate.
	DeveloperPassword string
	// SessionToken is handed out as the hdsl_session cookie.
	SessionToken string

	Version      string
	HealthStatus string
	UptimeSec    int64

	// DeveloperStatus is the answer of GET /api/developer/status. The zero
	// value would say "nothing is usable here", so New starts it as an
	// elevated panel on HTTPS with the web shell on.
	DevPasswordSet  bool
	Elevated        bool
	WebShellEnabled bool
	HTTPS           bool
	// GuardScope and Estop round out the status body from v0.5.94; the zero
	// values are "guard off" and "not stopped", which is also what a client
	// talking to an older panel sees.
	GuardScope string
	Estop      bool
	// EstopRemainingSeconds is the cooldown the fake reports while Estop is set.
	EstopRemainingSeconds int64

	// DSHInstances is the answer of GET /api/instances.
	DSHInstances []map[string]any
	// MCInstances is the answer of GET /api/mc/instances.
	MCInstances []map[string]any
	// BrandInstances is keyed by brand id.
	BrandInstances map[string][]map[string]any
	// Terminal is called for POST .../terminal; returning false falls back to
	// the default pty answer.
	Terminal func(w http.ResponseWriter, r *http.Request) bool

	mu     sync.Mutex
	seen   []Request
	exact  map[string]http.HandlerFunc
	prefix []prefixRoute
}

type prefixRoute struct {
	method string
	prefix string
	handle http.HandlerFunc
}

// New starts a fake panel on a loopback listener.
func New() *Server {
	server := &Server{
		Username:          "admin",
		Password:          "secret",
		DeveloperPassword: "dev",
		SessionToken:      "fake-session-token",
		Version:           "v0.5.4",
		HealthStatus:      "ok",
		UptimeSec:         4242,
		DevPasswordSet:    true,
		Elevated:          true,
		WebShellEnabled:   true,
		HTTPS:             true,
		DSHInstances:      []map[string]any{{"id": "dsh-1", "name": "main", "state": "running"}},
		MCInstances: []map[string]any{
			{"id": "mc-1", "name": "vanilla", "version": "1.21.11", "port": 25565,
				"state": "running", "consoleAvailable": true},
		},
		BrandInstances: map[string][]map[string]any{
			"kimi":       {{"id": "kimi-1", "name": "kimi-one", "state": "stopped"}},
			"opencode":   {},
			"claude-web": {{"id": "cw-1", "name": "claude-web", "state": "stopped"}},
			"hermes":     {{"id": "hm-1", "name": "hermes", "state": "stopped"}},
			"pi-web":     {{"id": "pi-1", "name": "pi-web", "state": "stopped"}},
		},
		exact: map[string]http.HandlerFunc{},
	}
	server.Server = httptest.NewServer(server)
	return server
}

// Handle overrides one route. An empty method matches any method.
func (s *Server) Handle(method, path string, handle http.HandlerFunc) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.exact[strings.ToUpper(method)+" "+path] = handle
}

// HandlePrefix overrides every route below path.
func (s *Server) HandlePrefix(method, path string, handle http.HandlerFunc) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.prefix = append(s.prefix, prefixRoute{method: strings.ToUpper(method), prefix: path, handle: handle})
}

// Requests returns everything the fake has been asked so far.
func (s *Server) Requests() []Request {
	s.mu.Lock()
	defer s.mu.Unlock()
	return append([]Request(nil), s.seen...)
}

// LastRequestTo returns the most recent call to path.
func (s *Server) LastRequestTo(path string) (Request, bool) {
	s.mu.Lock()
	defer s.mu.Unlock()
	for index := len(s.seen) - 1; index >= 0; index-- {
		if s.seen[index].Path == path {
			return s.seen[index], true
		}
	}
	return Request{}, false
}

func (s *Server) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	body := readBody(r)
	r.Body = io.NopCloser(strings.NewReader(body))
	s.mu.Lock()
	s.seen = append(s.seen, Request{
		Method: r.Method,
		Path:   r.URL.Path,
		Query:  r.URL.RawQuery,
		Body:   body,
		Cookie: cookieOf(r, "hdsl_session"),
	})
	exact, hasExact := s.exact[strings.ToUpper(r.Method)+" "+r.URL.Path]
	anyMethod, hasAny := s.exact[" "+r.URL.Path]
	var longest prefixRoute
	for _, route := range s.prefix {
		if route.method != "" && route.method != strings.ToUpper(r.Method) {
			continue
		}
		if !strings.HasPrefix(r.URL.Path, route.prefix) {
			continue
		}
		if len(route.prefix) >= len(longest.prefix) {
			longest = route
		}
	}
	s.mu.Unlock()

	switch {
	case hasExact:
		exact(w, r)
	case hasAny:
		anyMethod(w, r)
	case longest.handle != nil:
		longest.handle(w, r)
	default:
		s.route(w, r)
	}
}

func (s *Server) route(w http.ResponseWriter, r *http.Request) {
	segments := split(r.URL.Path)
	method := strings.ToUpper(r.Method)

	switch {
	case method == "GET" && r.URL.Path == "/api/health":
		writeJSON(w, http.StatusOK, map[string]any{
			"status": s.HealthStatus, "version": s.Version, "uptimeSec": s.UptimeSec,
		})
		return
	case method == "POST" && r.URL.Path == "/api/auth/login":
		var body struct{ Username, Password string }
		_ = json.Unmarshal([]byte(readBody(r)), &body)
		if body.Username != s.Username || body.Password != s.Password {
			writeError(w, http.StatusUnauthorized, "invalid username or password")
			return
		}
		http.SetCookie(w, &http.Cookie{Name: "hdsl_session", Value: s.SessionToken, Path: "/", HttpOnly: true})
		writeJSON(w, http.StatusOK, map[string]any{"ok": true, "username": body.Username})
		return
	case method == "POST" && r.URL.Path == "/api/developer/elevate":
		var body struct{ Password string }
		_ = json.Unmarshal([]byte(readBody(r)), &body)
		if body.Password != s.DeveloperPassword {
			writeError(w, http.StatusUnauthorized, "invalid developer password")
			return
		}
		writeJSON(w, http.StatusOK, map[string]any{"elevated": true, "expiresAt": "2026-10-23T12:34:56Z"})
		return
	case method == "POST" && r.URL.Path == "/api/developer/extend":
		if !s.Elevated {
			writeError(w, http.StatusForbidden, "developer_elevation_required: elevate with the developer password first")
			return
		}
		writeJSON(w, http.StatusOK, map[string]any{"elevated": true, "expiresAt": "2026-10-23T15:34:56Z"})
		return
	case method == "POST" && r.URL.Path == "/api/developer/agent-token":
		var body struct {
			Label  string   `json:"label"`
			Scopes []string `json:"scopes"`
		}
		_ = json.Unmarshal([]byte(readBody(r)), &body)
		if !s.Elevated {
			writeError(w, http.StatusForbidden,
				"developer_elevation_required: elevate with the developer password first")
			return
		}
		if body.Label == "" {
			body.Label = "agent"
		}
		if len(body.Scopes) == 0 {
			body.Scopes = []string{"read"}
		}
		writeJSON(w, http.StatusOK, map[string]any{
			"token":     "agent-token-" + body.Label,
			"id":        "grant-1",
			"label":     body.Label,
			"scopes":    body.Scopes,
			"expiresAt": "2026-10-23T12:34:56Z",
		})
		return
	case method == "POST" && r.URL.Path == "/api/developer/revoke":
		s.Elevated = false
		writeJSON(w, http.StatusOK, map[string]any{"elevated": false})
		return
	case method == "GET" && r.URL.Path == "/api/developer/status":
		estop := map[string]any{"engaged": s.Estop, "remainingSeconds": int64(0)}
		if s.Estop {
			estop["remainingSeconds"] = s.EstopRemainingSeconds
		}
		writeJSON(w, http.StatusOK, map[string]any{
			"devPasswordSet":  s.DevPasswordSet,
			"elevated":        s.Elevated,
			"webShellEnabled": s.WebShellEnabled,
			"https":           s.HTTPS,
			"guardScope":      s.GuardScope,
			"estop":           estop,
		})
		return
	case method == "GET" && r.URL.Path == "/api/mc/instances":
		writeJSON(w, http.StatusOK, map[string]any{"instances": s.MCInstances})
		return
	case method == "GET" && len(segments) == 2 && segments[0] == "api" && segments[1] == "instances":
		writeJSON(w, http.StatusOK, map[string]any{"instances": s.DSHInstances})
		return
	case method == "GET" && len(segments) == 4 && segments[0] == "api" && segments[1] == "brands" && segments[3] == "instances":
		writeJSON(w, http.StatusOK, map[string]any{"instances": s.BrandInstances[segments[2]]})
		return
	}

	if method == "POST" && (strings.HasSuffix(r.URL.Path, "/launch") ||
		strings.HasSuffix(r.URL.Path, "/start") ||
		strings.HasSuffix(r.URL.Path, "/stop")) {
		if strings.HasSuffix(r.URL.Path, "/stop") {
			writeJSON(w, http.StatusOK, map[string]any{"ok": true, "state": "stopping"})
			return
		}
		writeJSON(w, http.StatusAccepted, map[string]any{"ok": true, "state": "starting"})
		return
	}
	if method == "POST" && strings.HasSuffix(r.URL.Path, "/console/ticket") {
		parts := split(r.URL.Path)
		id := "mc-1"
		if len(parts) >= 4 {
			id = parts[3]
		}
		writeJSON(w, http.StatusOK, map[string]any{
			"mode":     "pty",
			"protocol": 1,
			"instance": id,
			"brand":    "mc",
			"ticket":   "mc-ticket-" + id,
			"ws":       "/ws/terminal/mc-ticket-" + id,
		})
		return
	}
	if !strings.HasSuffix(r.URL.Path, "/terminal") {
		writeError(w, http.StatusNotFound, "not found")
		return
	}
	brand, id, ok := terminalTarget(segments)
	if !ok {
		writeError(w, http.StatusNotFound, "not found")
		return
	}

	switch method {
	case "POST":
		if s.Terminal != nil && s.Terminal(w, r) {
			return
		}
		ticket := "ticket-" + brand + "-" + id
		writeJSON(w, http.StatusOK, map[string]any{
			"mode":     "pty",
			"protocol": 1,
			"instance": id,
			"ticket":   ticket,
			"ws":       "/ws/terminal/" + ticket,
			"brand":    brandOrDefault(brand),
		})
	case "DELETE":
		writeJSON(w, http.StatusOK, map[string]any{"ok": true, "killed": true})
	default:
		writeError(w, http.StatusMethodNotAllowed, "method not allowed")
	}
}

// terminalTarget reads the instance id (and brand, when present) out of
// /api/instances/{id}/terminal or /api/brands/{brand}/instances/{id}/terminal.
func terminalTarget(segments []string) (brand, id string, ok bool) {
	if len(segments) >= 2 && segments[0] == "api" && segments[1] == "brands" {
		if len(segments) >= 5 && segments[3] == "instances" {
			return segments[2], segments[4], true
		}
		return "", "", false
	}
	if len(segments) >= 3 && segments[0] == "api" && segments[1] == "instances" {
		return "", segments[2], true
	}
	return "", "", false
}

func brandOrDefault(brand string) string {
	if brand == "" {
		return "dsh"
	}
	return brand
}

func split(path string) []string {
	trimmed := strings.Trim(path, "/")
	if trimmed == "" {
		return nil
	}
	return strings.Split(trimmed, "/")
}

func writeJSON(w http.ResponseWriter, status int, body any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	encoded, err := json.Marshal(body)
	if err != nil {
		_, _ = w.Write([]byte(`{"error":"marshal failed"}`))
		return
	}
	_, _ = w.Write(encoded)
}

func writeError(w http.ResponseWriter, status int, message string) {
	writeJSON(w, status, map[string]any{"error": message})
}

// WriteError is writeError for tests that script their own failure route.
func WriteError(w http.ResponseWriter, status int, message string) { writeError(w, status, message) }

// WriteJSON is writeJSON for the same purpose.
func WriteJSON(w http.ResponseWriter, status int, body any) { writeJSON(w, status, body) }

func readBody(r *http.Request) string {
	if r.Body == nil {
		return ""
	}
	buffer := make([]byte, 0, 512)
	chunk := make([]byte, 512)
	for {
		n, err := r.Body.Read(chunk)
		buffer = append(buffer, chunk[:n]...)
		if err != nil {
			break
		}
	}
	return string(buffer)
}

func cookieOf(r *http.Request, name string) string {
	cookie, err := r.Cookie(name)
	if err != nil {
		return ""
	}
	return cookie.Value
}
