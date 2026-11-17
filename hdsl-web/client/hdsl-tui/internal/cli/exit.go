package cli

import "github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/panel"

// Exit codes. They are part of the client's contract: scripts branch on them.
const (
	// ExitOK — the session ended normally, or the agent exited with 0.
	ExitOK = 0
	// ExitAuth — authentication or developer elevation was refused (2).
	ExitAuth = 2
	// ExitTransport — the panel could not be reached, or the connection ended
	// unexpectedly (3).
	ExitTransport = 3
	// ExitPolicy — the panel refuses on policy or platform grounds and no
	// retry helps: not on HTTPS, web shell off, the terminal cannot start,
	// Windows desktop console (4).
	ExitPolicy = 4
	// ExitUsage — the command line was wrong (5).
	ExitUsage = 5
)

// ExitCodeFor maps a failure bucket onto the exit code table.
func ExitCodeFor(kind panel.Kind) int {
	switch kind {
	case panel.KindAuth:
		return ExitAuth
	case panel.KindPolicy:
		return ExitPolicy
	default:
		return ExitTransport
	}
}
