package store

import (
	"os"
	"path/filepath"
	"runtime"
	"testing"
	"time"
)

func tempPath(t *testing.T) string {
	t.Helper()
	return filepath.Join(t.TempDir(), "hdsl-tui", "config.json")
}

func TestLoadMissingFileYieldsDefaults(t *testing.T) {
	config, err := Load(tempPath(t))
	if err != nil {
		t.Fatalf("Load: %v", err)
	}
	if !config.RememberSession {
		t.Error("a fresh config should remember the session")
	}
	if len(config.RecentServers()) != 0 {
		t.Errorf("servers = %v, want none", config.RecentServers())
	}
}

func TestSaveIsPrivateAndRoundTrips(t *testing.T) {
	path := tempPath(t)
	config, err := Load(path)
	if err != nil {
		t.Fatalf("Load: %v", err)
	}
	now := time.Date(2026, 10, 23, 20, 0, 0, 0, time.Local)
	config.Touch("https://panel.example.com:8443", "admin", now)
	config.Remember("https://panel.example.com:8443", "admin", "cookie-value", now)
	if err := config.Save(); err != nil {
		t.Fatalf("Save: %v", err)
	}

	// POSIX permission bits are not enforced on Windows (chmod there only
	// toggles the read-only attribute), so the exact modes are Unix-only.
	if runtime.GOOS != "windows" {
		info, err := os.Stat(path)
		if err != nil {
			t.Fatalf("stat: %v", err)
		}
		if mode := info.Mode().Perm(); mode != 0o600 {
			t.Errorf("file mode = %o, want 600", mode)
		}
		dirInfo, err := os.Stat(filepath.Dir(path))
		if err != nil {
			t.Fatalf("stat dir: %v", err)
		}
		if mode := dirInfo.Mode().Perm(); mode != 0o700 {
			t.Errorf("dir mode = %o, want 700", mode)
		}
	}

	reloaded, err := Load(path)
	if err != nil {
		t.Fatalf("reload: %v", err)
	}
	servers := reloaded.RecentServers()
	if len(servers) != 1 || servers[0].URL != "https://panel.example.com:8443" || servers[0].Username != "admin" {
		t.Fatalf("servers = %+v", servers)
	}
	session, ok := reloaded.SavedSession("https://panel.example.com:8443")
	if !ok || session.Cookie != "cookie-value" || session.Username != "admin" {
		t.Fatalf("session = %+v ok=%v", session, ok)
	}
}

func TestTouchDeduplicatesAndSortsByRecentUse(t *testing.T) {
	config, _ := Load(tempPath(t))
	base := time.Date(2026, 10, 23, 12, 0, 0, 0, time.UTC)
	config.Touch("https://a.test", "", base)
	config.Touch("https://b.test", "", base.Add(time.Minute))
	config.Touch("https://a.test/", "", base.Add(2*time.Minute)) // trailing slash collapses

	servers := config.RecentServers()
	if len(servers) != 2 {
		t.Fatalf("servers = %+v, want two", servers)
	}
	if servers[0].URL != "https://a.test" {
		t.Errorf("first = %q, want the most recently used", servers[0].URL)
	}
}

func TestSavedSessionIsScopedToItsServer(t *testing.T) {
	config, _ := Load(tempPath(t))
	now := time.Now()
	config.Remember("https://a.test", "admin", "cookie", now)
	if _, ok := config.SavedSession("https://b.test"); ok {
		t.Error("a session must not leak to another panel")
	}
	if session, ok := config.SavedSession("https://a.test"); !ok || session.Cookie != "cookie" {
		t.Errorf("session = %+v ok=%v", session, ok)
	}
	config.ClearSession()
	if _, ok := config.SavedSession("https://a.test"); ok {
		t.Error("ClearSession kept the cookie")
	}
}

func TestRememberObeysTheSwitch(t *testing.T) {
	config, _ := Load(tempPath(t))
	config.RememberSession = false
	config.Remember("https://a.test", "admin", "cookie", time.Now())
	if _, ok := config.SavedSession("https://a.test"); ok {
		t.Error("RememberSession=false still stored a cookie")
	}
}

func TestForgetDropsServerAndItsSession(t *testing.T) {
	config, _ := Load(tempPath(t))
	now := time.Now()
	config.Touch("https://a.test", "admin", now)
	config.Touch("https://b.test", "admin", now)
	config.Remember("https://a.test", "admin", "cookie", now)
	config.Forget("https://a.test")
	if len(config.RecentServers()) != 1 || config.RecentServers()[0].URL != "https://b.test" {
		t.Errorf("servers = %+v", config.RecentServers())
	}
	if _, ok := config.SavedSession("https://a.test"); ok {
		t.Error("forgetting a server kept its session")
	}
}

func TestCorruptFileIsReportedNotOverwritten(t *testing.T) {
	path := tempPath(t)
	if err := os.MkdirAll(filepath.Dir(path), 0o700); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(path, []byte("{not json"), 0o600); err != nil {
		t.Fatal(err)
	}
	if _, err := Load(path); err == nil {
		t.Fatal("Load accepted a corrupt file")
	}
	data, err := os.ReadFile(path)
	if err != nil || string(data) != "{not json" {
		t.Errorf("Load touched the file: %q %v", data, err)
	}
}

func TestFutureSchemaIsRefused(t *testing.T) {
	path := tempPath(t)
	if err := os.MkdirAll(filepath.Dir(path), 0o700); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(path, []byte(`{"version": 99}`), 0o600); err != nil {
		t.Fatal(err)
	}
	if _, err := Load(path); err == nil {
		t.Fatal("Load accepted a future schema")
	}
}

func TestUsernameFor(t *testing.T) {
	config, _ := Load(tempPath(t))
	config.Touch("https://a.test", "admin", time.Now())
	if got := config.UsernameFor("https://a.test"); got != "admin" {
		t.Errorf("UsernameFor = %q", got)
	}
	if got := config.UsernameFor("https://b.test"); got != "" {
		t.Errorf("UsernameFor(unknown) = %q", got)
	}
}
