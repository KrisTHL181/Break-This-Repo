package cli

import (
	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/pterm"
)

// DetachPrefix is Ctrl-], the escape hatch that keeps an interactive agent
// from swallowing the client's own keys.
const DetachPrefix = 0x1d

// Detach is the local key state machine. Ctrl-] arms it; the next byte decides:
//
//	Ctrl-] q            detach, leaving the agent running
//	Ctrl-] k            stop the terminal, then quit
//	Ctrl-] Ctrl-]       forward one literal Ctrl-]
//	Ctrl-] <anything>   forward that byte
//
// Everything else — Ctrl-C (0x03) included — is forwarded untouched, so the
// agent sees exactly what was typed.
type Detach struct {
	armed bool
}

// Feed consumes one stdin chunk. It returns the bytes to forward (nil when the
// chunk was consumed entirely) and the command, if any.
func (d *Detach) Feed(chunk []byte) ([]byte, pterm.Command) {
	var forward []byte
	for index := 0; index < len(chunk); index++ {
		value := chunk[index]
		if !d.armed {
			if value == DetachPrefix {
				d.armed = true
				continue
			}
			forward = append(forward, value)
			continue
		}
		d.armed = false
		switch value {
		case 'q', 'Q':
			return forward, pterm.CommandDetach
		case 'k', 'K':
			return forward, pterm.CommandKill
		case DetachPrefix:
			forward = append(forward, DetachPrefix)
		default:
			forward = append(forward, value)
		}
	}
	return forward, pterm.CommandNone
}

// Armed reports whether a detach sequence is half-typed. The prompt uses it to
// tell the user what Ctrl-] is waiting for.
func (d *Detach) Armed() bool { return d.armed }

// DetachHint is the sentence printed when the client leaves the agent behind.
const DetachHint = "已离开终端（agent 仍在后台运行）。重新运行本命令可附着回去，Ctrl-] k 可以停掉它。"
