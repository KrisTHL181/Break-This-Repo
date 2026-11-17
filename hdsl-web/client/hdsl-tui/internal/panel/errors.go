package panel

import (
	"errors"
	"fmt"
	"regexp"
	"strings"
)

// SessionCookie is the cookie carrying the panel session. The HTTP client
// holds it in memory; whether it reaches the config file is the user's choice
// (store.RememberSession), and it is the only thing the file stores that
// authenticates anything.
const SessionCookie = "hdsl_session"

// APIError is an error body: every /api failure (except a bare transport
// failure) is `{"error":"..."}`, and the interesting part is usually the
// machine-readable prefix before the colon — `insecure_transport`,
// `web_shell_disabled`, `terminal_failed`, …
type APIError struct {
	Status  int
	Message string
	Code    string
}

func (e *APIError) Error() string {
	if e.Message == "" {
		return fmt.Sprintf("panel returned HTTP %d", e.Status)
	}
	return e.Message
}

// TransportError is a failure to reach the panel at all: DNS, connect, TLS or
// a truncated response. It is deliberately separate from APIError so the exit
// code table can tell "the panel said no" from "there was no panel".
type TransportError struct {
	Op  string
	Err error
}

func (e *TransportError) Error() string {
	if e.Op == "" {
		return e.Err.Error()
	}
	return e.Op + ": " + e.Err.Error()
}

func (e *TransportError) Unwrap() error { return e.Err }

var codePattern = regexp.MustCompile(`^[a-z][a-z0-9_]*$`)

// errorCode extracts the prefix of a server error message. The contract is
// `code: human text`; anything that does not look like a code yields "".
func errorCode(message string) string {
	prefix, _, found := strings.Cut(message, ":")
	if !found {
		return ""
	}
	prefix = strings.TrimSpace(prefix)
	if codePattern.MatchString(prefix) {
		return prefix
	}
	return ""
}

// Message returns the error text a panel supplied, or "" when the failure did
// not come from the panel.
func Message(err error) string {
	var api *APIError
	if errors.As(err, &api) {
		return api.Message
	}
	return ""
}

// Code returns the machine-readable code of an /api error, or "".
func Code(err error) string {
	var api *APIError
	if errors.As(err, &api) {
		return api.Code
	}
	return ""
}

// Status returns the HTTP status of an /api error, or 0.
func Status(err error) int {
	var api *APIError
	if errors.As(err, &api) {
		return api.Status
	}
	return 0
}

// IsCode reports whether err carries the given code.
func IsCode(err error, code string) bool { return Code(err) == code }

// Kind groups failures the way the exit-code table does.
type Kind int

const (
	// KindOther is anything unclassified; it maps to the transport exit code.
	KindOther Kind = iota
	// KindAuth is a refused credential or elevation: retry with the right
	// password (exit 2).
	KindAuth
	// KindTransport is a failure to reach or keep talking to the panel (exit 3).
	KindTransport
	// KindPolicy is a refusal that no retry fixes: the panel is not on HTTPS,
	// the web shell is off, the terminal cannot start at all, or the only
	// console is the panel machine's own desktop (exit 4).
	KindPolicy
)

// authCodes are refusals that a correct password (or a second attempt after
// the lockout) would clear.
var authCodes = map[string]bool{
	"invalid username or password": true,
	"invalid developer password":   true,
	"too many failed attempts":     true,
	"developer_password_not_set":   true,
	"developer_elevation_required": true,
}

// policyCodes are refusals that come from the panel's configuration or from
// where it runs; the client cannot talk its way past them.
var policyCodes = map[string]bool{
	"insecure_transport":    true,
	"tls_required":          true,
	"web_shell_disabled":    true,
	"confirmation_required": true,
	"brand_has_no_tui":      true,
	// The emergency stop and its cooldown are the panel's decision, not
	// something a retry or a password clears: wait, or restart the panel.
	"emergency_stop": true,
	"estop_cooldown": true,
	// The panel refused to start the terminal (no PTY, a profile that cannot
	// hold the plugin, a missing kernel). It is the panel's own reason that
	// reaches the user, and no retry fixes it — the same bucket as a missing
	// plugin, not a transport failure.
	"terminal_failed": true,
}

// Classify buckets a failure for exit-code and wording purposes.
func Classify(err error) Kind {
	if err == nil {
		return KindOther
	}
	var transport *TransportError
	if errors.As(err, &transport) {
		return KindTransport
	}
	message := Message(err)
	if authCodes[Code(err)] || authCodes[message] {
		return KindAuth
	}
	if policyCodes[Code(err)] || policyCodes[message] {
		return KindPolicy
	}
	switch Status(err) {
	case 401, 429:
		return KindAuth
	case 403:
		return KindPolicy
	}
	return KindOther
}
