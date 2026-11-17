package panel

import "testing"

// The surfaces do not share a path prefix: dsh lives at /api/instances, the
// brand categories at /api/brands/{brand}, and ZCode is its own category at
// /api/zcode. A client that guessed one shape would address the wrong surface.
func TestSurfacePaths(t *testing.T) {
	listCases := map[string]string{
		"":           "/api/instances",
		"dsh":        "/api/instances",
		"kimi":       "/api/brands/kimi/instances",
		"opencode":   "/api/brands/opencode/instances",
		"claude-web": "/api/brands/claude-web/instances",
		"pi-web":     "/api/brands/pi-web/instances",
		"zcode":      "/api/zcode/instances",
	}
	for brand, want := range listCases {
		if got := instancesPath(brand); got != want {
			t.Errorf("instancesPath(%q) = %q, want %q", brand, got, want)
		}
	}

	terminalCases := map[string]string{
		"":           "/api/instances/i1/terminal",
		"dsh":        "/api/instances/i1/terminal",
		"kimi":       "/api/brands/kimi/instances/i1/terminal",
		"claude-web": "/api/brands/claude-web/instances/i1/terminal",
		"pi-web":     "/api/brands/pi-web/instances/i1/terminal",
		"zcode":      "/api/zcode/instances/i1/terminal",
	}
	for brand, want := range terminalCases {
		if got := terminalPath(brand, "i1"); got != want {
			t.Errorf("terminalPath(%q) = %q, want %q", brand, got, want)
		}
	}
}

// An id with a slash must not become a second path segment.
func TestTerminalPathEscapesTheInstanceID(t *testing.T) {
	got := terminalPath("zcode", "a/b")
	if got != "/api/zcode/instances/a%2Fb/terminal" {
		t.Errorf("terminalPath = %q", got)
	}
}

// The kernel list is a sibling of the terminal endpoint and only exists for the
// brand categories (Claude Web is the one with two kernels).
func TestKernelsPath(t *testing.T) {
	if got := kernelsPath("claude-web", "i1"); got != "/api/brands/claude-web/instances/i1/terminals" {
		t.Errorf("kernelsPath = %q", got)
	}
	if got := kernelsPath("claude-web", "a/b"); got != "/api/brands/claude-web/instances/a%2Fb/terminals" {
		t.Errorf("kernelsPath escaped = %q", got)
	}
}
