// Package store keeps the client's small amount of local state: the panel
// addresses the user has used, the last username per panel, and — when the
// user allows it — the session cookie so the next start does not ask for the
// password again.
//
// The file is per-user (0700 directory, 0600 file) and holds nothing the panel
// itself would not hand back: no password, no developer password, no terminal
// ticket. Deleting it logs the client out.
package store

import (
	"encoding/json"
	"errors"
	"fmt"
	"io/fs"
	"os"
	"path/filepath"
	"sort"
	"time"
)

// Version is the on-disk schema revision. A file from a future version is
// ignored rather than half-read.
const Version = 1

// EnvPath overrides the config file location; useful for tests and for keeping
// a second profile.
const EnvPath = "HDSL_TUI_CONFIG"

// Server is one remembered panel.
type Server struct {
	URL      string    `json:"url"`
	Username string    `json:"username,omitempty"`
	LastUsed time.Time `json:"lastUsed,omitempty"`
}

// Session is the remembered login. Cookie is the value of the panel's session
// cookie, never its name — the name is fixed by the protocol.
type Session struct {
	Server   string    `json:"server"`
	Username string    `json:"username,omitempty"`
	Cookie   string    `json:"cookie"`
	SavedAt  time.Time `json:"savedAt,omitempty"`
}

// Config is the whole file.
type Config struct {
	Version         int      `json:"version"`
	Servers         []Server `json:"servers,omitempty"`
	Session         *Session `json:"session,omitempty"`
	RememberSession bool     `json:"rememberSession"`
	path            string
}

// DefaultPath is $HDSL_TUI_CONFIG, else <user config dir>/hdsl-tui/config.json.
func DefaultPath() (string, error) {
	if override := os.Getenv(EnvPath); override != "" {
		return override, nil
	}
	dir, err := os.UserConfigDir()
	if err != nil {
		return "", fmt.Errorf("找不到用户配置目录: %w", err)
	}
	return filepath.Join(dir, "hdsl-tui", "config.json"), nil
}

// Load reads path. A missing file is not an error: it yields the defaults,
// which remember the session and have no servers yet. A corrupt file is
// reported, so the caller can decide to start over instead of silently
// overwriting the servers list.
func Load(path string) (*Config, error) {
	config := defaultConfig()
	config.path = path
	data, err := os.ReadFile(path)
	if err != nil {
		if errors.Is(err, fs.ErrNotExist) {
			return config, nil
		}
		return nil, fmt.Errorf("读取 %s 失败: %w", path, err)
	}
	if len(data) == 0 {
		return config, nil
	}
	var parsed Config
	if err := json.Unmarshal(data, &parsed); err != nil {
		return nil, fmt.Errorf("%s 不是有效的配置（%v）；删掉它可重新开始", path, err)
	}
	if parsed.Version > Version {
		return nil, fmt.Errorf("%s 来自更新版本的客户端（schema %d），本程序只认识 %d",
			path, parsed.Version, Version)
	}
	parsed.Version = Version
	parsed.path = path
	parsed.Servers = normaliseServers(parsed.Servers)
	parsed.sortByRecent()
	if parsed.Session != nil && parsed.Session.Cookie == "" {
		parsed.Session = nil
	}
	return &parsed, nil
}

func defaultConfig() *Config {
	return &Config{Version: Version, RememberSession: true}
}

// Path is where this config was loaded from (and will be saved to).
func (c *Config) Path() string { return c.path }

// Save writes the file atomically with 0600 permissions. The directory is
// created 0700: the cookie in it is a bearer credential for the panel.
func (c *Config) Save() error {
	if c == nil {
		return nil
	}
	if c.path == "" {
		path, err := DefaultPath()
		if err != nil {
			return err
		}
		c.path = path
	}
	c.Version = Version
	data, err := json.MarshalIndent(c, "", "  ")
	if err != nil {
		return err
	}
	data = append(data, '\n')
	dir := filepath.Dir(c.path)
	if err := os.MkdirAll(dir, 0o700); err != nil {
		return fmt.Errorf("创建配置目录 %s 失败: %w", dir, err)
	}
	temp, err := os.CreateTemp(dir, ".config-*.json")
	if err != nil {
		return fmt.Errorf("写入配置失败: %w", err)
	}
	name := temp.Name()
	defer os.Remove(name)
	if err := temp.Chmod(0o600); err != nil {
		temp.Close()
		return err
	}
	if _, err := temp.Write(data); err != nil {
		temp.Close()
		return fmt.Errorf("写入配置失败: %w", err)
	}
	if err := temp.Close(); err != nil {
		return err
	}
	if err := os.Rename(name, c.path); err != nil {
		return fmt.Errorf("保存配置 %s 失败: %w", c.path, err)
	}
	return nil
}

// RecentServers returns the remembered panels, most recently used first.
func (c *Config) RecentServers() []Server {
	if c == nil {
		return nil
	}
	c.sortByRecent()
	return c.Servers
}

// Touch records that url was used (now), keeping at most one entry per URL.
// username may be empty, in which case the remembered one is kept.
func (c *Config) Touch(url, username string, now time.Time) {
	if c == nil {
		return
	}
	url = trim(url)
	if url == "" {
		return
	}
	for index := range c.Servers {
		if c.Servers[index].URL == url {
			c.Servers[index].LastUsed = now
			if username != "" {
				c.Servers[index].Username = username
			}
			c.sortByRecent()
			return
		}
	}
	c.Servers = append(c.Servers, Server{URL: url, Username: username, LastUsed: now})
	c.sortByRecent()
}

// Forget removes one remembered panel (and its session, if it was the one
// being remembered).
func (c *Config) Forget(url string) {
	if c == nil {
		return
	}
	url = trim(url)
	kept := c.Servers[:0]
	for _, server := range c.Servers {
		if server.URL != url {
			kept = append(kept, server)
		}
	}
	c.Servers = kept
	if c.Session != nil && c.Session.Server == url {
		c.Session = nil
	}
}

// Remember stores the session cookie for server.
func (c *Config) Remember(server, username, cookie string, now time.Time) {
	if c == nil || cookie == "" || !c.RememberSession {
		return
	}
	c.Session = &Session{Server: server, Username: username, Cookie: cookie, SavedAt: now}
}

// ClearSession forgets the cookie but keeps the server list.
func (c *Config) ClearSession() {
	if c == nil {
		return
	}
	c.Session = nil
}

// SavedSession returns the cookie remembered for server, or "".
func (c *Config) SavedSession(server string) (Session, bool) {
	if c == nil || c.Session == nil || c.Session.Server != server || c.Session.Cookie == "" {
		return Session{}, false
	}
	return *c.Session, true
}

// UsernameFor returns the last username used with server, or "".
func (c *Config) UsernameFor(server string) string {
	if c == nil {
		return ""
	}
	for _, entry := range c.Servers {
		if entry.URL == server {
			return entry.Username
		}
	}
	return ""
}

func (c *Config) sortByRecent() {
	sort.SliceStable(c.Servers, func(i, j int) bool {
		return c.Servers[i].LastUsed.After(c.Servers[j].LastUsed)
	})
}

// normaliseServers drops empty entries, trims trailing slashes and collapses
// duplicates (first occurrence wins).
func normaliseServers(servers []Server) []Server {
	seen := make(map[string]bool, len(servers))
	kept := make([]Server, 0, len(servers))
	for _, server := range servers {
		url := trim(server.URL)
		if url == "" || seen[url] {
			continue
		}
		seen[url] = true
		server.URL = url
		kept = append(kept, server)
	}
	return kept
}

func trim(url string) string {
	for len(url) > 0 && url[len(url)-1] == '/' {
		url = url[:len(url)-1]
	}
	return url
}
