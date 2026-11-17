package panel

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strings"
)

// maxErrorBody caps how much of an error response is read; the panel's error
// bodies are one short sentence.
const maxErrorBody = 64 << 10

// maxSuccessBody caps a successful response. A chat transcript is the biggest
// thing this client reads and it easily passes 64 KiB; the old code applied the
// error cap to everything and turned a long session into "unexpected end of
// JSON input".
const maxSuccessBody = 64 << 20

// Client talks to one panel. The session cookie lives here and nowhere else in
// the HTTP layer: no jar, no argv. The UI decides whether to persist it (see
// internal/store); this type only ever holds it in memory.
type Client struct {
	base   *url.URL
	http   *http.Client
	cookie *http.Cookie
	logf   func(format string, args ...any)
}

// New builds a client for baseURL (scheme + host [+ path prefix]).
func New(baseURL string, httpClient *http.Client, logf func(string, ...any)) (*Client, error) {
	trimmed := strings.TrimRight(strings.TrimSpace(baseURL), "/")
	if trimmed == "" {
		return nil, fmt.Errorf("面板地址为空")
	}
	base, err := url.Parse(trimmed)
	if err != nil {
		return nil, fmt.Errorf("面板地址无效 %q: %w", baseURL, err)
	}
	if base.Scheme != "http" && base.Scheme != "https" {
		return nil, fmt.Errorf("面板地址必须以 http:// 或 https:// 开头，收到 %q", baseURL)
	}
	if base.Host == "" {
		return nil, fmt.Errorf("面板地址缺少主机名: %q", baseURL)
	}
	if httpClient == nil {
		httpClient = http.DefaultClient
	}
	if logf == nil {
		logf = func(string, ...any) {}
	}
	return &Client{base: base, http: httpClient, logf: logf}, nil
}

// BaseURL is the normalised panel address.
func (c *Client) BaseURL() *url.URL { return c.base }

// Cookie is the captured session cookie, or nil before login.
func (c *Client) Cookie() *http.Cookie { return c.cookie }

// Session returns a copy of the session cookie usable in a WebSocket
// handshake header, or "" when there is none.
func (c *Client) Session() string {
	if c.cookie == nil {
		return ""
	}
	cookie := *c.cookie
	cookie.Path = "/"
	return cookie.String()
}

// CookieValue is the bare session token, or "" before login. It is what the
// config file stores when the user lets the client remember the session.
func (c *Client) CookieValue() string {
	if c.cookie == nil {
		return ""
	}
	return c.cookie.Value
}

// SetSession installs a session token captured earlier (or clears it when raw
// is empty). The panel decides whether it is still valid; the first request
// after this call is the probe.
func (c *Client) SetSession(raw string) {
	if raw == "" {
		c.cookie = nil
		return
	}
	c.cookie = &http.Cookie{Name: SessionCookie, Value: raw, Path: "/"}
}

// URL resolves a panel-relative path against the base, keeping any prefix the
// panel is mounted under.
func (c *Client) URL(path string, query url.Values) string {
	target := *c.base
	target.Path = strings.TrimRight(c.base.Path, "/") + path
	if query != nil {
		target.RawQuery = query.Encode()
	}
	return target.String()
}

// WebSocketURL turns the `ws` field of an open-terminal answer into a proper
// ws:// or wss:// URL.
//
// The field is a panel-internal path ("/ws/terminal/{ticket}"). It is relative
// to the panel's mount point, not to the host root: resolving it with
// ResolveReference would replace the whole path and silently drop a base path
// such as /hdsl, which breaks terminal connections in every sub-path
// deployment while health checks and login keep working.
func (c *Client) WebSocketURL(path string) (string, error) {
	reference, err := url.Parse(path)
	if err != nil {
		return "", fmt.Errorf("服务端返回的 ws 地址无效 %q: %w", path, err)
	}
	if reference.IsAbs() {
		absolute := *reference
		switch absolute.Scheme {
		case "ws", "wss":
			return absolute.String(), nil
		case "http":
			absolute.Scheme = "ws"
		case "https":
			absolute.Scheme = "wss"
		default:
			return "", fmt.Errorf("不支持的 ws 协议 %q", reference.Scheme)
		}
		return absolute.String(), nil
	}

	target := *c.base
	prefix := strings.TrimRight(c.base.Path, "/")
	target.Path = joinPanelPath(prefix, reference.Path)
	target.RawPath = ""
	if reference.RawPath != "" {
		target.RawPath = joinPanelPath(prefix, reference.RawPath)
	}
	target.RawQuery = reference.RawQuery
	target.Fragment = ""
	switch target.Scheme {
	case "http":
		target.Scheme = "ws"
	case "https":
		target.Scheme = "wss"
	default:
		return "", fmt.Errorf("不支持的 ws 协议 %q", target.Scheme)
	}
	return target.String(), nil
}

// joinPanelPath puts a panel-internal path under the mount point. A path that
// already carries the prefix is left alone (never "/hdsl/hdsl/…"), and the
// join never produces a double slash.
func joinPanelPath(prefix, path string) string {
	if path == "" {
		return prefix
	}
	clean := "/" + strings.TrimLeft(path, "/")
	if prefix == "" {
		return clean
	}
	if clean == prefix || strings.HasPrefix(clean, prefix+"/") {
		return clean
	}
	return prefix + clean
}

func (c *Client) do(ctx context.Context, method, path string, query url.Values, body any) (*http.Response, []byte, error) {
	var payload io.Reader
	if body != nil {
		encoded, err := json.Marshal(body)
		if err != nil {
			return nil, nil, err
		}
		payload = bytes.NewReader(encoded)
	}
	target := c.URL(path, query)
	request, err := http.NewRequestWithContext(ctx, method, target, payload)
	if err != nil {
		return nil, nil, err
	}
	request.Header.Set("Accept", "application/json")
	if body != nil {
		request.Header.Set("Content-Type", "application/json")
	}
	if c.cookie != nil {
		request.AddCookie(c.cookie)
	}
	c.logf("> %s %s", method, target)
	response, err := c.http.Do(request)
	if err != nil {
		return nil, nil, &TransportError{Op: method + " " + path, Err: err}
	}
	defer response.Body.Close()
	if method == "POST" {
		c.captureCookie(response)
	}
	limit := int64(maxSuccessBody)
	if response.StatusCode < 200 || response.StatusCode >= 300 {
		limit = maxErrorBody
	}
	data, readErr := io.ReadAll(io.LimitReader(response.Body, limit))
	if readErr != nil {
		return response, nil, &TransportError{Op: method + " " + path, Err: readErr}
	}
	if response.StatusCode >= 200 && response.StatusCode < 300 && int64(len(data)) >= limit {
		return response, nil, &TransportError{Op: method + " " + path,
			Err: fmt.Errorf("响应超过 %d 字节", limit)}
	}
	logged := data
	if len(logged) > 512 {
		logged = logged[:512]
	}
	c.logf("< %d %s", response.StatusCode, strings.TrimSpace(string(logged)))
	if response.StatusCode < 200 || response.StatusCode >= 300 {
		return response, data, apiErrorFrom(response.StatusCode, data)
	}
	return response, data, nil
}

func (c *Client) captureCookie(response *http.Response) {
	for _, cookie := range response.Cookies() {
		if cookie.Name == SessionCookie {
			c.cookie = cookie
			return
		}
	}
}

// apiErrorFrom decodes `{"error":"..."}`; a body that is not JSON (a proxy's
// HTML error page, say) still becomes an APIError with the status text.
func apiErrorFrom(status int, data []byte) error {
	var body struct {
		Error string `json:"error"`
	}
	if err := json.Unmarshal(data, &body); err == nil && body.Error != "" {
		return &APIError{Status: status, Message: body.Error, Code: errorCode(body.Error)}
	}
	text := strings.TrimSpace(string(data))
	if len(text) > 200 {
		text = text[:200]
	}
	return &APIError{Status: status, Message: fmt.Sprintf("HTTP %d %s", status, text)}
}

// Health reads GET /api/health. It is the only endpoint that works before
// login and is used as a reachability and version check.
func (c *Client) Health(ctx context.Context) (Health, error) {
	_, data, err := c.do(ctx, http.MethodGet, "/api/health", nil, nil)
	if err != nil {
		return Health{}, err
	}
	var health Health
	if err := json.Unmarshal(data, &health); err != nil {
		return Health{}, &TransportError{Op: "GET /api/health", Err: err}
	}
	return health, nil
}

// Login posts the panel credentials and captures the session cookie. The
// cookie is never written anywhere.
func (c *Client) Login(ctx context.Context, username, password string) (string, error) {
	_, data, err := c.do(ctx, http.MethodPost, "/api/auth/login", nil, map[string]string{
		"username": username,
		"password": password,
	})
	if err != nil {
		return "", err
	}
	if c.cookie == nil {
		return "", &TransportError{Op: "POST /api/auth/login", Err: fmt.Errorf("登录成功但没有收到 %s Cookie", SessionCookie)}
	}
	var body loginBody
	if err := json.Unmarshal(data, &body); err != nil {
		return body.Username, nil
	}
	if body.Username == "" {
		body.Username = username
	}
	return body.Username, nil
}

// Elevate proves the developer password. The elevation lives on the panel,
// keyed by session token and client address; it expires after 30 minutes of
// inactivity.
func (c *Client) Elevate(ctx context.Context, password string) (Elevation, error) {
	_, data, err := c.do(ctx, http.MethodPost, "/api/developer/elevate", nil, map[string]string{
		"password": password,
	})
	if err != nil {
		return Elevation{}, err
	}
	var elevation Elevation
	if err := json.Unmarshal(data, &elevation); err != nil {
		return Elevation{Elevated: true}, nil
	}
	return elevation, nil
}

// ExtendElevation stretches an existing elevation to three hours from now
// (POST /api/developer/extend). The panel refuses with
// developer_elevation_required when this session is not elevated yet, so the
// caller runs it after a successful Elevate.
func (c *Client) ExtendElevation(ctx context.Context) (Elevation, error) {
	return c.elevationAction(ctx, "/api/developer/extend")
}

// RevokeElevation drops this session's elevation immediately (POST
// /api/developer/revoke), ahead of its expiry. Idempotent: revoking without an
// elevation is a no-op the panel answers with elevated:false.
func (c *Client) RevokeElevation(ctx context.Context) (Elevation, error) {
	return c.elevationAction(ctx, "/api/developer/revoke")
}

func (c *Client) elevationAction(ctx context.Context, path string) (Elevation, error) {
	_, data, err := c.do(ctx, http.MethodPost, path, nil, nil)
	if err != nil {
		return Elevation{}, err
	}
	var elevation Elevation
	if err := json.Unmarshal(data, &elevation); err != nil {
		return Elevation{}, &TransportError{Op: "POST " + path, Err: err}
	}
	return elevation, nil
}

// MintAgentToken mints a bearer grant for an agent (POST
// /api/developer/agent-token). It requires a developer elevation; without one
// the panel answers 403, which the caller reports as "elevate first" rather
// than retrying.
func (c *Client) MintAgentToken(ctx context.Context, label string, scopes []string) (AgentToken, error) {
	body := map[string]any{"scopes": scopes}
	if label != "" {
		body["label"] = label
	}
	_, data, err := c.do(ctx, http.MethodPost, "/api/developer/agent-token", nil, body)
	if err != nil {
		return AgentToken{}, err
	}
	var token AgentToken
	if err := json.Unmarshal(data, &token); err != nil {
		return AgentToken{}, &TransportError{Op: "POST /api/developer/agent-token", Err: err}
	}
	return token, nil
}

// DeveloperStatus reads GET /api/developer/status: whether a developer password
// exists, whether this session is elevated, whether the web shell switch is on
// and whether the panel is speaking TLS. A panel older than this endpoint
// answers 404, which the caller must treat as "ask the panel by opening".
func (c *Client) DeveloperStatus(ctx context.Context) (DeveloperStatus, error) {
	_, data, err := c.do(ctx, http.MethodGet, "/api/developer/status", nil, nil)
	if err != nil {
		return DeveloperStatus{}, err
	}
	var status DeveloperStatus
	if err := json.Unmarshal(data, &status); err != nil {
		return DeveloperStatus{}, &TransportError{Op: "GET /api/developer/status", Err: err}
	}
	return status, nil
}

// Instances lists the DSH instances (GET /api/instances).
func (c *Client) Instances(ctx context.Context) ([]Instance, error) {
	return c.instances(ctx, "/api/instances")
}

// BrandInstances lists one surface's instances. The three brand surfaces answer
// GET /api/brands/{brand}/instances; ZCode is its own experimental category with
// its own prefix (`/api/zcode/instances`), which is why the path is resolved
// here instead of being spelled out by every caller.
func (c *Client) BrandInstances(ctx context.Context, brand string) ([]Instance, error) {
	return c.instances(ctx, instancesPath(brand))
}

// BrandCapabilities lists every brand's shape. It is the one place the client
// learns which surfaces are TUI-only, so the menu can hide launch/stop instead
// of offering a request the panel will refuse.
func (c *Client) BrandCapabilities(ctx context.Context) ([]BrandCapability, error) {
	_, data, err := c.do(ctx, http.MethodGet, "/api/brands/capabilities", nil, nil)
	if err != nil {
		return nil, err
	}
	var body brandCapabilities
	if err := json.Unmarshal(data, &body); err != nil {
		return nil, &TransportError{Op: "GET /api/brands/capabilities", Err: err}
	}
	return body.Brands, nil
}

// instancesPath maps a surface id to its list endpoint.
func instancesPath(brand string) string {
	switch brand {
	case "", "dsh":
		return "/api/instances"
	case "zcode":
		return "/api/zcode/instances"
	}
	return "/api/brands/" + url.PathEscape(brand) + "/instances"
}

func (c *Client) instances(ctx context.Context, path string) ([]Instance, error) {
	_, data, err := c.do(ctx, http.MethodGet, path, nil, nil)
	if err != nil {
		return nil, err
	}
	var list instanceList
	if err := json.Unmarshal(data, &list); err != nil {
		return nil, &TransportError{Op: "GET " + path, Err: err}
	}
	return list.Instances, nil
}

// instancePath is the base of one instance's REST surface: launch, stop, and
// the terminal that hangs off it. An empty brand means the DSH surface; the
// brand categories mirror it under /api/brands, and ZCode has its own prefix.
func instancePath(brand, id string) string {
	id = url.PathEscape(id)
	switch {
	case brand == "" || brand == "dsh":
		return "/api/instances/" + id
	case brand == "zcode":
		return "/api/zcode/instances/" + id
	}
	return "/api/brands/" + url.PathEscape(brand) + "/instances/" + id
}

// terminalPath is the open/close terminal endpoint.
func terminalPath(brand, id string) string {
	return instancePath(brand, id) + "/terminal"
}

// LaunchInstance starts an instance (POST .../launch). The panel answers 202
// with the state it entered — "starting", or "installing" when the version is
// not on disk yet — and the client only needs the request to have been
// accepted.
func (c *Client) LaunchInstance(ctx context.Context, brand, id string) error {
	_, _, err := c.do(ctx, http.MethodPost, instancePath(brand, id)+"/launch", nil, map[string]string{})
	return err
}

// StopInstance stops an instance (POST .../stop). It also ends a running
// terminal, which is the only process some instances have.
func (c *Client) StopInstance(ctx context.Context, brand, id string) error {
	_, _, err := c.do(ctx, http.MethodPost, instancePath(brand, id)+"/stop", nil, map[string]string{})
	return err
}

// OpenTerminal asks for a ticket. resume reattaches to an existing agent
// session of the harness; kernel picks one of Claude Web's two CLI kernels and
// is ignored by surfaces that have only one.
func (c *Client) OpenTerminal(ctx context.Context, brand, id, resume, kernel string) (Terminal, error) {
	query := url.Values{}
	if resume != "" {
		query.Set("resume", resume)
	}
	if kernel != "" {
		query.Set("kernel", kernel)
	}
	var sent url.Values
	if len(query) > 0 {
		sent = query
	}
	_, data, err := c.do(ctx, http.MethodPost, terminalPath(brand, id), sent, map[string]string{})
	if err != nil {
		return Terminal{}, err
	}
	var terminal Terminal
	if err := json.Unmarshal(data, &terminal); err != nil {
		return Terminal{}, &TransportError{Op: "POST terminal", Err: err}
	}
	return terminal, nil
}

// TerminalKernels lists the kernel choices of one brand instance
// (GET /api/brands/{brand}/instances/{id}/terminals). The second result is the
// panel's preferred default, or "" when it names none.
func (c *Client) TerminalKernels(ctx context.Context, brand, id string) ([]Kernel, string, error) {
	path := kernelsPath(brand, id)
	_, data, err := c.do(ctx, http.MethodGet, path, nil, nil)
	if err != nil {
		return nil, "", err
	}
	var list kernelList
	if err := json.Unmarshal(data, &list); err != nil {
		return nil, "", &TransportError{Op: "GET terminals", Err: err}
	}
	preferred := ""
	if list.Default != nil {
		preferred = *list.Default
	}
	return list.Kernels, preferred, nil
}

// kernelsPath is the sibling of terminalPath that lists what a surface can
// start; only Claude Web answers with more than one entry.
func kernelsPath(brand, id string) string {
	return "/api/brands/" + url.PathEscape(brand) + "/instances/" + url.PathEscape(id) + "/terminals"
}

// CloseTerminal stops the running terminal (DELETE .../terminal). It is what
// Ctrl-] k does; the reported `killed` is false when nothing was running.
func (c *Client) CloseTerminal(ctx context.Context, brand, id string) (bool, error) {
	_, data, err := c.do(ctx, http.MethodDelete, terminalPath(brand, id), nil, nil)
	if err != nil {
		return false, err
	}
	var body closeBody
	if err := json.Unmarshal(data, &body); err != nil {
		return false, nil
	}
	return body.Killed, nil
}

// MCInstances lists the Minecraft servers (GET /api/mc/instances). The whole
// surface answers 404 when the feature is off; the caller turns that into
// "not enabled" rather than a transport failure.
func (c *Client) MCInstances(ctx context.Context) ([]MCInstance, error) {
	_, data, err := c.do(ctx, http.MethodGet, "/api/mc/instances", nil, nil)
	if err != nil {
		return nil, err
	}
	var list struct {
		Instances []MCInstance `json:"instances"`
	}
	if err := json.Unmarshal(data, &list); err != nil {
		return nil, &TransportError{Op: "GET /api/mc/instances", Err: err}
	}
	return list.Instances, nil
}

// mcPath is one operation under a Minecraft instance.
func mcPath(id, op string) string {
	return "/api/mc/instances/" + url.PathEscape(id) + "/" + op
}

// MCConsoleTicket asks for the console ticket of one server
// (POST .../console/ticket). A PTY server answers the same shape as the agent
// terminal; a Windows pipe server answers `mode:"pipe"` with no ticket, which
// means the console only exists on the web page.
func (c *Client) MCConsoleTicket(ctx context.Context, id string) (Terminal, error) {
	_, data, err := c.do(ctx, http.MethodPost, mcPath(id, "console/ticket"), nil, map[string]string{})
	if err != nil {
		return Terminal{}, err
	}
	var terminal Terminal
	if err := json.Unmarshal(data, &terminal); err != nil {
		return Terminal{}, &TransportError{Op: "POST /api/mc/instances console/ticket", Err: err}
	}
	return terminal, nil
}

// MCStart starts one server (POST .../start).
func (c *Client) MCStart(ctx context.Context, id string) error {
	_, _, err := c.do(ctx, http.MethodPost, mcPath(id, "start"), nil, map[string]string{})
	return err
}

// MCStop stops one server (POST .../stop).
func (c *Client) MCStop(ctx context.Context, id string) error {
	_, _, err := c.do(ctx, http.MethodPost, mcPath(id, "stop"), nil, map[string]string{})
	return err
}
