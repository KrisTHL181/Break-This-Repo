// Package ui is the client's terminal user interface: the Debian-installer
// style full screen menu the panel client shows before an agent takes over.
//
// It deliberately depends on nothing but the standard library and the
// vendored terminal package: the client builds offline, and a menu library
// would be a new dependency to vendor for four screens.
package ui

// Key is a logical key press.
type Key int

const (
	// KeyUnknown is a sequence this client does not act on (function keys,
	// mouse reports, …). Screens ignore it.
	KeyUnknown Key = iota
	// KeyRune is a printable character; Event.Rune carries it.
	KeyRune
	KeyUp
	KeyDown
	KeyLeft
	KeyRight
	KeyEnter
	KeyEsc
	KeyTab
	KeyBackspace
	KeyDelete
	KeyHome
	KeyEnd
	KeyPgUp
	KeyPgDn
	// KeyInterrupt is Ctrl-C. In raw mode it arrives as a byte, not as a
	// signal, which is what lets a prompt cancel instead of killing the
	// process mid-login.
	KeyInterrupt
	// KeyEOF is Ctrl-D on an empty line.
	KeyEOF
	// KeyClear is Ctrl-U: clear the input field.
	KeyClear
	// KeyResize is not a key press: the window changed size and the screen
	// should be redrawn.
	KeyResize

	// KeyNewline is Ctrl-J: insert a newline in a multi-line composer.
	KeyNewline
	// KeyHistoryPrev / KeyHistoryNext are Ctrl-P / Ctrl-N: walk the input
	// history (the arrows belong to the composer's caret).
	KeyHistoryPrev
	KeyHistoryNext
	// KeyExpand is Ctrl-O: expand or collapse the reasoning/tool timeline.
	KeyExpand
	// KeySessions is Ctrl-B: open the session list.
	KeySessions
	// KeyElevate is Ctrl-E: request developer elevation.
	KeyElevate
)

// Event is one thing the screen has to react to.
type Event struct {
	Key  Key
	Rune rune
}

// Decode splits a chunk of raw terminal bytes into events. The second return
// value is the tail of an escape sequence that has not arrived completely yet;
// the caller keeps it and prepends it to the next chunk.
func Decode(data []byte) ([]Event, []byte) {
	var events []Event
	for index := 0; index < len(data); {
		value := data[index]
		switch {
		case value == 0x1b:
			consumed, event, complete := decodeEscape(data[index:])
			if !complete {
				return events, data[index:]
			}
			index += consumed
			if event.Key != KeyUnknown {
				events = append(events, event)
			}
		case value == 0x03:
			events = append(events, Event{Key: KeyInterrupt})
			index++
		case value == 0x04:
			events = append(events, Event{Key: KeyEOF})
			index++
		case value == 0x15:
			events = append(events, Event{Key: KeyClear})
			index++
		case value == '\r':
			events = append(events, Event{Key: KeyEnter})
			index++
		case value == '\n':
			// Ctrl-J. A raw-mode Enter is CR; LF is the documented newline key.
			events = append(events, Event{Key: KeyNewline})
			index++
		case value == 0x10:
			events = append(events, Event{Key: KeyHistoryPrev})
			index++
		case value == 0x0e:
			events = append(events, Event{Key: KeyHistoryNext})
			index++
		case value == 0x0f:
			events = append(events, Event{Key: KeyExpand})
			index++
		case value == 0x02:
			events = append(events, Event{Key: KeySessions})
			index++
		case value == 0x05:
			events = append(events, Event{Key: KeyElevate})
			index++
		case value == '\t':
			events = append(events, Event{Key: KeyTab})
			index++
		case value == 0x7f || value == 0x08:
			events = append(events, Event{Key: KeyBackspace})
			index++
		default:
			r, size := decodeRune(data[index:])
			if size == 0 {
				return events, data[index:]
			}
			if r >= 0x20 {
				events = append(events, Event{Key: KeyRune, Rune: r})
			}
			index += size
		}
	}
	return events, nil
}

// DecodeForce is Decode for a buffer that will not grow any more: a lone ESC
// becomes the Esc key and an unfinished sequence is dropped.
func DecodeForce(data []byte) []Event {
	events, rest := Decode(data)
	if len(rest) == 0 {
		return events
	}
	if len(rest) == 1 && rest[0] == 0x1b {
		return append(events, Event{Key: KeyEsc})
	}
	return events
}

func decodeEscape(data []byte) (consumed int, event Event, complete bool) {
	if len(data) == 1 {
		return 0, Event{}, false
	}
	switch data[1] {
	case '[':
		// CSI: parameters/intermediates up to a final byte in 0x40..0x7e.
		for index := 2; index < len(data); index++ {
			if data[index] >= 0x40 && data[index] <= 0x7e {
				return index + 1, csiEvent(data[2:index], data[index]), true
			}
		}
		return 0, Event{}, false
	case 'O':
		// SS3: a single final byte (application cursor keys).
		if len(data) < 3 {
			return 0, Event{}, false
		}
		return 3, finalByteEvent(data[2]), true
	default:
		// Alt-<key> and friends: consume only the ESC so the next byte is
		// decoded normally.
		return 1, Event{Key: KeyEsc}, true
	}
}

func csiEvent(params []byte, final byte) Event {
	// `ESC [ 1 ; 5 C` is Ctrl-Right, not Right: a sequence with parameters is
	// a different key, and mapping it to the plain arrow would make the menu
	// jump when the user meant to do something else.
	if len(params) > 0 {
		if final == '~' {
			switch string(params) {
			case "1", "7":
				return Event{Key: KeyHome}
			case "3":
				return Event{Key: KeyDelete}
			case "4", "8":
				return Event{Key: KeyEnd}
			case "5":
				return Event{Key: KeyPgUp}
			case "6":
				return Event{Key: KeyPgDn}
			}
		}
		return Event{Key: KeyUnknown}
	}
	switch final {
	case 'A':
		return Event{Key: KeyUp}
	case 'B':
		return Event{Key: KeyDown}
	case 'C':
		return Event{Key: KeyRight}
	case 'D':
		return Event{Key: KeyLeft}
	case 'H':
		return Event{Key: KeyHome}
	case 'F':
		return Event{Key: KeyEnd}
	case '~':
		switch string(params) {
		case "1", "7":
			return Event{Key: KeyHome}
		case "3":
			return Event{Key: KeyDelete}
		case "4", "8":
			return Event{Key: KeyEnd}
		case "5":
			return Event{Key: KeyPgUp}
		case "6":
			return Event{Key: KeyPgDn}
		}
	}
	return Event{Key: KeyUnknown}
}

func finalByteEvent(final byte) Event {
	switch final {
	case 'A':
		return Event{Key: KeyUp}
	case 'B':
		return Event{Key: KeyDown}
	case 'C':
		return Event{Key: KeyRight}
	case 'D':
		return Event{Key: KeyLeft}
	case 'H':
		return Event{Key: KeyHome}
	case 'F':
		return Event{Key: KeyEnd}
	}
	return Event{Key: KeyUnknown}
}
