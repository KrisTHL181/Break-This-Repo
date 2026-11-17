// Package panel is the HTTP half of the terminal client: login, developer
// elevation, instance lists and opening/closing a terminal. It speaks the
// REST contract documented in docs/terminal-protocol.md and nothing else.
package panel

import "time"

// Health is the body of GET /api/health.
type Health struct {
	Status    string `json:"status"`
	Version   string `json:"version"`
	UptimeSec int64  `json:"uptimeSec"`
}

// Instance is one entry of GET /api/instances or
// GET /api/brands/{brand}/instances. Both endpoints use the same shape.
//
// The first three fields are the contract the client was written against; the
// rest arrived with the panel's instance and Node.js work. An older panel omits
// them, and the zero value is the honest answer — "this panel does not report
// it" — rather than an error.
type Instance struct {
	ID    string `json:"id"`
	Name  string `json:"name"`
	State string `json:"state"`

	// Version is the harness/product version the instance runs.
	Version string `json:"version"`
	// Port is the DSH instance's web port; LastPort is what the brand
	// surfaces report instead.
	Port     int `json:"port"`
	LastPort int `json:"lastPort"`
	// URL is the panel-relative mount a browser opens; empty while stopped.
	URL string `json:"url"`
	// Error is why a failed instance failed, in the same answer that says
	// "failed" (v0.6.x; earlier panels leave it empty).
	Error string `json:"error"`
	// Account is the DSH instance's provider account key, when it has one.
	Account string `json:"account"`
	// Brand is the brand id, present on brand instance rows.
	Brand string `json:"brand"`
	// TerminalOpen says a Web Shell terminal is up for this instance.
	TerminalOpen bool `json:"terminalOpen"`
	// Disabled is the brand policy's "do not launch this" flag.
	Disabled bool `json:"disabled"`
}

// PortOf is the port the instance listens on: DSH reports `port`, the brand
// surfaces report `lastPort`.
func (i Instance) PortOf() int {
	if i.Port != 0 {
		return i.Port
	}
	return i.LastPort
}

type instanceList struct {
	Instances []Instance `json:"instances"`
}

// BrandCapability is one entry of GET /api/brands/capabilities. Kind decides
// whether the surface has a web mode ("web"), a terminal only ("tui"), or both
// ("hybrid"). An older panel omits Kind and the client then treats the brand as
// web — the only shape those panels shipped.
type BrandCapability struct {
	ID          string `json:"id"`
	Kind        string `json:"kind"`
	Web         bool   `json:"web"`
	Terminal    bool   `json:"terminal"`
	Installable bool   `json:"installable"`
	Reason      string `json:"reason"`
}

type brandCapabilities struct {
	Platform string            `json:"platform"`
	Brands   []BrandCapability `json:"brands"`
}

// Terminal is the body of POST .../terminal. Mode is "pty" (Ticket and WS are
// set) or "desktop" (an older panel ran the console on the panel machine's own
// desktop and had no PTY to attach to; Hint explains that to the user). Panels
// on Windows answer "pty" from Windows 10 1809 on.
type Terminal struct {
	Mode     string `json:"mode"`
	Protocol int    `json:"protocol"`
	Instance string `json:"instance"`
	Ticket   string `json:"ticket"`
	WS       string `json:"ws"`
	Brand    string `json:"brand"`
	Hint     string `json:"hint"`
}

// Kernel is one terminal kernel a surface offers. Only Claude Web has more than
// one: the community engine its browser-side provider configuration is written
// for, and the official Claude Code binary.
type Kernel struct {
	ID        string `json:"id"`
	Label     string `json:"label"`
	Available bool   `json:"available"`
	Detail    string `json:"detail"`
}

type kernelList struct {
	Kernels []Kernel `json:"kernels"`
	Default *string  `json:"default"`
}

// Desktop reports whether the panel answered with a desktop console instead of
// a PTY.
func (t Terminal) Desktop() bool { return t.Mode == "desktop" }

// DeveloperStatus is the body of GET /api/developer/status: what the panel holds
// right now, read before a terminal is opened so the developer password is asked
// for in the order the web UI uses.
type DeveloperStatus struct {
	DevPasswordSet  bool   `json:"devPasswordSet"`
	Elevated        bool   `json:"elevated"`
	ExpiresAt       string `json:"expiresAt"`
	WebShellEnabled bool   `json:"webShellEnabled"`
	HTTPS           bool   `json:"https"`
	// GuardScope is the developer guard's reach (off/proxy/instance/all). A
	// panel older than v0.5.94 omits it, which reads as off.
	GuardScope string `json:"guardScope"`
	// Estop is the emergency stop's state. Absent on older panels, and the zero
	// value means "not engaged".
	Estop EmergencyStopState `json:"estop"`
}

// EmergencyStopState mirrors the panel's E-Stop block. Only the fields the
// client can act on are decoded.
type EmergencyStopState struct {
	Engaged          bool   `json:"engaged"`
	ReleasesAt       string `json:"releasesAt"`
	RemainingSeconds int64  `json:"remainingSeconds"`
}

// Expiry parses ExpiresAt; a panel that omits it yields the zero time.
func (s DeveloperStatus) Expiry() time.Time {
	return Elevation{ExpiresAt: s.ExpiresAt}.Expiry()
}

// Elevation is the body of POST /api/developer/elevate.
type Elevation struct {
	Elevated  bool   `json:"elevated"`
	ExpiresAt string `json:"expiresAt"`
	// Token is the bearer credential minted by the same response, for callers
	// that cannot keep the session cookie. The client itself uses cookies, so
	// it keeps this only to pass on to a child process; empty on old panels.
	Token string `json:"token"`
}

// Expiry parses ExpiresAt; a panel that omits it yields the zero time.
func (e Elevation) Expiry() time.Time {
	for _, layout := range []string{time.RFC3339Nano, time.RFC3339, "2006-01-02T15:04:05.999999999"} {
		if parsed, err := time.Parse(layout, e.ExpiresAt); err == nil {
			return parsed
		}
	}
	return time.Time{}
}

// AgentToken is the body of POST /api/developer/agent-token: the bearer grant
// handed to an agent. The token itself appears exactly once, in this answer.
type AgentToken struct {
	Token      string   `json:"token"`
	ID         string   `json:"id"`
	Label      string   `json:"label"`
	Scopes     []string `json:"scopes"`
	InstanceID string   `json:"instanceId"`
	ExpiresAt  string   `json:"expiresAt"`
}

// Expiry parses ExpiresAt the same way the elevation does.
func (t AgentToken) Expiry() time.Time {
	return Elevation{ExpiresAt: t.ExpiresAt}.Expiry()
}

// MCInstance is one Minecraft server as GET /api/mc/instances reports it.
type MCInstance struct {
	ID      string `json:"id"`
	Name    string `json:"name"`
	Version string `json:"version"`
	Port    int    `json:"port"`
	State   string `json:"state"`
	// ConsoleAvailable is false when no console names this instance — the
	// panel's own "can I attach at all" answer.
	ConsoleAvailable bool   `json:"consoleAvailable"`
	Error            string `json:"error"`
}

type loginBody struct {
	OK       bool   `json:"ok"`
	Username string `json:"username"`
}

type closeBody struct {
	OK     bool `json:"ok"`
	Killed bool `json:"killed"`
}
