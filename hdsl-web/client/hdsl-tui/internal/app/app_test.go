package app

import (
	"bytes"
	"context"
	"io"
	"net/http"
	"strings"
	"testing"
	"time"

	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/panel"
	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/panel/fakeserver"
	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/termx"
	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/ui"
)

func TestWrapHonoursWidthAndNewlines(t *testing.T) {
	lines := wrap("一二三四五六七八九十", 8)
	for _, line := range lines {
		if ui.VisibleWidth(line) > 8 {
			t.Errorf("line %q is %d cells", line, ui.VisibleWidth(line))
		}
	}
	if len(lines) < 2 {
		t.Errorf("lines = %v, want a wrap", lines)
	}
	if got := wrap("a\n\nb", 10); len(got) != 3 {
		t.Errorf("wrap kept %d lines, want 3", len(got))
	}
}

func TestNormaliseAddress(t *testing.T) {
	cases := []struct {
		input string
		want  string
		bad   bool
	}{
		{"https://panel.example.com:8443/", "https://panel.example.com:8443", false},
		{"panel.example.com:8443", "https://panel.example.com:8443", false},
		{"http://127.0.0.1:8080", "http://127.0.0.1:8080", false},
		{"https://panel.test/base/", "https://panel.test/base", false},
		{"", "", true},
		{"https://", "", true},
		{"ftp://panel.test", "", true},
	}
	for _, testCase := range cases {
		got, err := normaliseAddress(testCase.input)
		if testCase.bad {
			if err == nil {
				t.Errorf("normaliseAddress(%q) = %q, want an error", testCase.input, got)
			}
			continue
		}
		if err != nil {
			t.Errorf("normaliseAddress(%q): %v", testCase.input, err)
			continue
		}
		if got != testCase.want {
			t.Errorf("normaliseAddress(%q) = %q, want %q", testCase.input, got, testCase.want)
		}
	}
}

func TestStateText(t *testing.T) {
	cases := map[string]string{
		"running":  "运行中",
		"stopped":  "已停止",
		"starting": "启动中",
		"failed":   "失败",
		"oddball":  "oddball",
		"":         "状态未知",
	}
	for state, want := range cases {
		if got := stateText(state); got != want {
			t.Errorf("stateText(%q) = %q, want %q", state, got, want)
		}
	}
}

func TestExplainMapsPanelCodesToAdvice(t *testing.T) {
	cases := []struct {
		err  error
		want string
	}{
		{
			&panel.APIError{Status: 403, Message: "developer_elevation_required: nope", Code: "developer_elevation_required"},
			"开发者密码",
		},
		{&panel.APIError{Status: 403, Message: "web_shell_disabled: off", Code: "web_shell_disabled"}, "Web Shell"},
		{&panel.APIError{Status: 403, Message: "insecure_transport: no", Code: "insecure_transport"}, "HTTPS"},
		{&panel.APIError{Status: 400, Message: "invalid username or password", Code: "invalid username or password"}, "invalid username or password"},
		{&panel.APIError{Status: 409, Message: "developer_password_not_set: set it first", Code: "developer_password_not_set"}, "开发者"},
	}
	for _, testCase := range cases {
		if got := explain(testCase.err); !strings.Contains(got, testCase.want) {
			t.Errorf("explain(%v) = %q, want it to mention %q", testCase.err, got, testCase.want)
		}
	}
}

func TestChatNeedsGuardElevation(t *testing.T) {
	if !chatNeedsGuardElevation("all") {
		t.Error("scope=all must require the developer password for chat")
	}
	for _, scope := range []string{"", "off", "proxy", "instance"} {
		if chatNeedsGuardElevation(scope) {
			t.Errorf("scope=%q must not gate /api/chat", scope)
		}
	}
}

func TestNeedsElevation(t *testing.T) {
	required := &panel.APIError{Status: 403, Message: "developer_elevation_required: x", Code: "developer_elevation_required"}
	if !needsElevation(required) {
		t.Error("needsElevation missed the code")
	}
	plain := &panel.APIError{Status: 403, Message: "web_shell_disabled: x", Code: "web_shell_disabled"}
	if needsElevation(plain) {
		t.Error("needsElevation fired on an unrelated refusal")
	}
}

func TestInstanceLabelFallsBackToTheID(t *testing.T) {
	if got := instanceLabel(panel.Instance{ID: "abc"}); got != "abc" {
		t.Errorf("instanceLabel = %q", got)
	}
	if got := instanceLabel(panel.Instance{ID: "abc", Name: "名字"}); got != "名字" {
		t.Errorf("instanceLabel = %q", got)
	}
}

func TestExplainLeavesNonPanelErrorsAlone(t *testing.T) {
	if got := explain(nil); got != "" {
		t.Errorf("explain(nil) = %q", got)
	}
	err := &panel.TransportError{Op: "GET /api/health", Err: http.ErrServerClosed}
	if got := explain(err); !strings.Contains(got, "网络错误") {
		t.Errorf("explain(transport) = %q", got)
	}
}

// The panel refuses a start with the reason after the code — which profile
// cannot hold the plugin, which kernel is missing — and that reason is the part
// the user can act on. Replacing it with "check the panel log" throws it away.
func TestExplainKeepsThePanelReasonForAFailedTerminal(t *testing.T) {
	err := &panel.APIError{
		Status:  http.StatusConflict,
		Message: "terminal_failed: 这个实例的启动参数把 profile 指回了 web",
		Code:    "terminal_failed",
	}
	got := explain(err)
	if !strings.Contains(got, "profile 指回了 web") {
		t.Errorf("explain = %q, want the panel's own reason", got)
	}
	if strings.Contains(got, "面板日志") {
		t.Errorf("explain = %q, want the reason instead of the generic sentence", got)
	}
	if !strings.HasPrefix(got, "面板启动终端失败：") {
		t.Errorf("explain = %q, want it to keep saying what failed", got)
	}

	// A panel that names no reason keeps the generic sentence.
	for _, message := range []string{"terminal_failed:", "terminal_failed: "} {
		bare := &panel.APIError{Status: http.StatusConflict, Message: message, Code: "terminal_failed"}
		if got := explain(bare); !strings.Contains(got, "面板日志") {
			t.Errorf("explain(%q) = %q, want the fallback", message, got)
		}
	}
}

// ------------------------------------------------------- the attach flow --

// drawGate is the screen's output in tests that need to know when a menu has
// painted: a menu draws, then waits for a key, so the paint is the signal that
// keys typed now are past the flush every screen does on entry.
type drawGate struct{ draws chan struct{} }

func newDrawGate() *drawGate { return &drawGate{draws: make(chan struct{}, 8)} }

func (g *drawGate) Write(data []byte) (int, error) {
	if bytes.HasPrefix(data, []byte("\x1b[H")) {
		select {
		case g.draws <- struct{}{}:
		default:
		}
	}
	return len(data), nil
}

func (g *drawGate) waitForDraw(t *testing.T) {
	t.Helper()
	select {
	case <-g.draws:
	case <-time.After(5 * time.Second):
		t.Fatal("the menu never painted")
	}
}

// The help text promises "↑/↓ 或 j/k" and only the arrows were wired up, so a
// vim user pressing j watched the cursor sit still. Each key is fed after the
// menu has painted, which is what makes the answer exact rather than a race.
func TestMenuMovesWithVimKeys(t *testing.T) {
	cases := []struct {
		name string
		keys []string
		want int
	}{
		{"j moves down", []string{"j", "j", "\r"}, 2},
		{"k moves back up", []string{"j", "j", "k", "\r"}, 1},
		{"the arrows still move", []string{"\x1b[B", "\x1b[B", "\r"}, 2},
		{"other letters do nothing", []string{"x", "j", "\r"}, 1},
	}
	for _, testCase := range cases {
		t.Run(testCase.name, func(t *testing.T) {
			source := &keySource{keys: make(chan []byte, 8)}
			t.Cleanup(func() { close(source.keys) })
			gate := newDrawGate()
			screen, err := ui.Open(source, gate, termx.Nop{})
			if err != nil {
				t.Fatalf("ui.Open: %v", err)
			}
			t.Cleanup(screen.Close)
			app := &App{opts: Options{Version: "test"}, screen: screen}

			selected := make(chan int, 1)
			go func() {
				index, ok := app.menu(menuPage{
					Title: "测试",
					Items: []ui.Item{{Label: "一"}, {Label: "二"}, {Label: "三"}},
				})
				if !ok {
					selected <- -1
					return
				}
				selected <- index
			}()

			for _, chunk := range testCase.keys {
				gate.waitForDraw(t)
				source.keys <- []byte(chunk)
			}
			select {
			case index := <-selected:
				if index != testCase.want {
					t.Errorf("the menu selected %d, want %d", index, testCase.want)
				}
			case <-time.After(5 * time.Second):
				t.Fatal("the menu never returned")
			}
		})
	}
}

// keySource is a keyboard the test feeds: the screen's reader blocks in Read
// until a keystroke is pushed, so no test has to guess when to type.
type keySource struct{ keys chan []byte }

func (k *keySource) Read(p []byte) (int, error) {
	chunk, ok := <-k.keys
	if !ok {
		return 0, io.EOF
	}
	return copy(p, chunk), nil
}

// presser repeats one keystroke until it is stopped. Every screen drops the
// keys that arrived while a request was in flight (flush), so a key has to be
// repeated rather than sent once at a guessed moment.
type presser struct {
	source *keySource
	stop   chan struct{}
	done   chan struct{}
}

func newPresser(t *testing.T, keys string) *presser {
	t.Helper()
	pressed := &presser{
		source: &keySource{keys: make(chan []byte, 64)},
		stop:   make(chan struct{}),
		done:   make(chan struct{}),
	}
	go func() {
		defer close(pressed.done)
		for {
			select {
			case <-pressed.stop:
				return
			case pressed.source.keys <- []byte(keys):
			}
			time.Sleep(5 * time.Millisecond)
		}
	}()
	return pressed
}

func (p *presser) Close() {
	close(p.stop)
	<-p.done
	// The screen's reader blocks in Read; closing the queue lets it see EOF.
	close(p.source.keys)
}

// attachFixture is the fake panel, the app wired to it, and the number of calls
// the fake answered before the attach flow started (the login): indexing from
// there makes an assertion about the flow's own order.
type attachFixture struct {
	app    *App
	server *fakeserver.Server
	base   int
}

func (f *attachFixture) requests() []fakeserver.Request { return f.server.Requests()[f.base:] }

// index is the position of a call among the ones the flow itself made.
func (f *attachFixture) index(method, path string) int {
	for index, request := range f.requests() {
		if request.Method == method && request.Path == path {
			return index
		}
	}
	return -1
}

// newAttachApp is an App wired to a fake panel whose screen reads the test's
// keystrokes. It is enough to run the whole attach flow: the panel answers the
// terminal request, and the WebSocket handshake fails against a fake that has
// no /ws route, which is the first thing after the ordering under test.
func newAttachApp(t *testing.T, keys string, tune func(*fakeserver.Server)) *attachFixture {
	t.Helper()
	server := fakeserver.New()
	t.Cleanup(server.Close)
	if tune != nil {
		tune(server)
	}
	client, err := panel.New(server.URL, server.Client(), nil)
	if err != nil {
		t.Fatalf("panel.New: %v", err)
	}
	pressed := newPresser(t, keys)
	t.Cleanup(pressed.Close)
	screen, err := ui.Open(pressed.source, io.Discard, termx.Nop{})
	if err != nil {
		t.Fatalf("ui.Open: %v", err)
	}
	t.Cleanup(screen.Close)
	if _, err := client.Login(context.Background(), server.Username, server.Password); err != nil {
		t.Fatalf("Login: %v", err)
	}
	return &attachFixture{
		app: &App{
			opts:   Options{Version: "test"},
			screen: screen,
			client: client,
			server: server.URL,
		},
		server: server,
		base:   len(server.Requests()),
	}
}

// attachAndWait runs one attach and fails if a screen it opened is never
// answered: a stuck flow must not hang the whole test binary.
func attachAndWait(t *testing.T, app *App, brand string, instance panel.Instance) {
	t.Helper()
	done := make(chan struct{})
	go func() {
		defer close(done)
		app.attach(context.Background(), brand, instance)
	}()
	select {
	case <-done:
	case <-time.After(15 * time.Second):
		t.Fatal("attach never came back: the flow is stuck on an unanswered screen")
	}
}

// claudeWeb is the instance the ordering tests attach to.
var claudeWeb = panel.Instance{ID: "cw-1", Name: "claude-web"}

// The order the web UI uses is password → kernel → terminal, and the kernel
// question only makes sense once the terminal can actually open.
func TestAttachAsksForThePasswordBeforeTheKernel(t *testing.T) {
	fixture := newAttachApp(t, "dev\r", func(server *fakeserver.Server) {
		server.Elevated = false
	})

	attachAndWait(t, fixture.app, "claude-web", claudeWeb)

	if status := fixture.index(http.MethodGet, "/api/developer/status"); status != 0 {
		t.Fatalf("the developer status was call %d; the calls were %+v", status, fixture.requests())
	}
	elevate := fixture.index(http.MethodPost, "/api/developer/elevate")
	if elevate < 0 {
		t.Fatal("the developer password was never asked for")
	}
	kernels := fixture.index(http.MethodGet, "/api/brands/claude-web/instances/cw-1/terminals")
	if kernels >= 0 && kernels < elevate {
		t.Errorf("the kernel list was asked at %d, before the password at %d", kernels, elevate)
	}
	terminal := fixture.index(http.MethodPost, "/api/brands/claude-web/instances/cw-1/terminal")
	if terminal >= 0 && terminal < elevate {
		t.Errorf("the terminal was opened at %d, before the password at %d", terminal, elevate)
	}
}

// A panel with no developer password can never elevate, so asking for the
// password would be a dead end: the flow stops with where to set one, and the
// kernel question, the terminal request and the elevation POST are all absent.
func TestAttachStopsWhenThePanelHasNoDeveloperPassword(t *testing.T) {
	fixture := newAttachApp(t, "\r", func(server *fakeserver.Server) {
		server.Elevated = false
		server.DevPasswordSet = false
	})

	attachAndWait(t, fixture.app, "claude-web", claudeWeb)

	requests := fixture.requests()
	if len(requests) != 1 || requests[0].Path != "/api/developer/status" {
		t.Errorf("calls = %+v, want only the developer status", requests)
	}
}

// With an elevation already in place the order is status → kernel → terminal,
// and nothing asks for the password again.
func TestAttachAsksTheStatusKernelThenTerminal(t *testing.T) {
	fixture := newAttachApp(t, "\r", nil)

	attachAndWait(t, fixture.app, "claude-web", claudeWeb)

	status := fixture.index(http.MethodGet, "/api/developer/status")
	kernels := fixture.index(http.MethodGet, "/api/brands/claude-web/instances/cw-1/terminals")
	terminal := fixture.index(http.MethodPost, "/api/brands/claude-web/instances/cw-1/terminal")
	if status != 0 || kernels <= status || terminal <= kernels {
		t.Errorf("order = status %d, kernels %d, terminal %d; want status → kernels → terminal",
			status, kernels, terminal)
	}
	if elevate := fixture.index(http.MethodPost, "/api/developer/elevate"); elevate >= 0 {
		t.Errorf("the password was asked for at %d although the panel said it was elevated", elevate)
	}
}

func TestBrandSurfacesComeFromThePanelToo(t *testing.T) {
	got := brandSurfaces([]panel.BrandCapability{
		{ID: "kimi", Kind: "web"},
		{ID: "newtui", Kind: "tui"},
	})
	if len(got) != len(brandOrder)+1 {
		t.Fatalf("surfaces = %v", got)
	}
	if got[len(got)-1] != "newtui" {
		t.Fatalf("unknown brand must follow the known ones: %v", got)
	}
	seen := map[string]int{}
	for _, brand := range got {
		seen[brand]++
	}
	if seen["kimi"] != 1 {
		t.Fatalf("a known brand was listed twice: %v", got)
	}
}

func TestBrandIsTUIFallsBackForOldPanels(t *testing.T) {
	if !brandIsTUI(nil, "qwen") || !brandIsTUI(nil, "mimo") || !brandIsTUI(nil, "minimax") {
		t.Fatal("the pure-TUI brands must stay TUI without capabilities")
	}
	if brandIsTUI(nil, "kimi") {
		t.Fatal("kimi has a web process; it is not a pure TUI brand")
	}
	if !brandIsTUI(map[string]string{"newtui": "tui"}, "newtui") {
		t.Fatal("the panel's capability must win")
	}
	if brandIsTUI(map[string]string{"qwen": "web"}, "qwen") {
		t.Fatal("an explicit web capability must override the fallback")
	}
}

func TestBlockTimelineHelpers(t *testing.T) {
	if got := blockQuery([]byte(`{"query":"go tui"}`)); got != "go tui" {
		t.Fatalf("query = %q", got)
	}
	if got := blockQuery([]byte(`{"queries":["a","b"]}`)); got != "a / b" {
		t.Fatalf("queries = %q", got)
	}
	if got := blockQuery([]byte(`{"url":"https://example.com"}`)); got != "https://example.com" {
		t.Fatalf("url = %q", got)
	}
	sources := blockSources([]byte(`[{"title":"a","url":"https://a.example"}]`))
	if len(sources) != 1 || sources[0].URL != "https://a.example" {
		t.Fatalf("sources = %+v", sources)
	}
	one := blockSources([]byte(`{"title":"b","url":"https://b.example"}`))
	if len(one) != 1 || one[0].URL != "https://b.example" {
		t.Fatalf("single source = %+v", one)
	}
	if got := queryOfArguments(`{"url":"https://c.example"}`); got != "https://c.example" {
		t.Fatalf("tool url = %q", got)
	}
}

func TestThinkingPreviewKeepsTheTail(t *testing.T) {
	lines := thinkingPreview("第一行\n第二行\n第三行\n第四行", 40, 2)
	if len(lines) != 2 {
		t.Fatalf("lines = %q", lines)
	}
	if !strings.Contains(lines[0], "第三行") || !strings.Contains(lines[1], "第四行") {
		t.Fatalf("preview = %q", lines)
	}
	if strings.Contains(strings.Join(lines, ""), "第一行") {
		t.Fatalf("preview kept the oldest line: %q", lines)
	}
}

func TestThinkingPreviewWrapsLongText(t *testing.T) {
	lines := thinkingPreview(strings.Repeat("很长", 50), 10, 2)
	if len(lines) != 2 {
		t.Fatalf("lines = %d", len(lines))
	}
	for _, line := range lines {
		if ui.VisibleWidth(strings.TrimPrefix(line, "\x1b[36m")) > 12 {
			t.Fatalf("line is too wide: %q", line)
		}
	}
}
