package ui

import "testing"

func keyOf(event Event) Key { return event.Key }

func TestDecodeArrowsAndEnter(t *testing.T) {
	cases := []struct {
		name  string
		input string
		want  Key
	}{
		{"up", "\x1b[A", KeyUp},
		{"down", "\x1b[B", KeyDown},
		{"right", "\x1b[C", KeyRight},
		{"left", "\x1b[D", KeyLeft},
		{"home", "\x1b[H", KeyHome},
		{"end", "\x1b[F", KeyEnd},
		{"pgup", "\x1b[5~", KeyPgUp},
		{"pgdn", "\x1b[6~", KeyPgDn},
		{"delete", "\x1b[3~", KeyDelete},
		{"application cursor up", "\x1bOA", KeyUp},
		{"enter cr", "\r", KeyEnter},
		{"ctrl-j newline", "\n", KeyNewline},
		{"tab", "\t", KeyTab},
		{"backspace del", "\x7f", KeyBackspace},
		{"backspace bs", "\x08", KeyBackspace},
		{"ctrl-p history", "\x10", KeyHistoryPrev},
		{"ctrl-n history", "\x0e", KeyHistoryNext},
		{"ctrl-o expand", "\x0f", KeyExpand},
		{"ctrl-b sessions", "\x02", KeySessions},
		{"ctrl-e elevate", "\x05", KeyElevate},
	}
	for _, testCase := range cases {
		t.Run(testCase.name, func(t *testing.T) {
			events, rest := Decode([]byte(testCase.input))
			if len(rest) != 0 {
				t.Fatalf("rest = %q, want none", rest)
			}
			if len(events) != 1 {
				t.Fatalf("events = %v, want exactly one", events)
			}
			if keyOf(events[0]) != testCase.want {
				t.Errorf("key = %v, want %v", keyOf(events[0]), testCase.want)
			}
		})
	}
}

func TestDecodeCtrlCIsAnEventNotASignal(t *testing.T) {
	events, _ := Decode([]byte{0x03})
	if len(events) != 1 || events[0].Key != KeyInterrupt {
		t.Fatalf("events = %v, want KeyInterrupt", events)
	}
}

func TestDecodeRunesIncludingChinese(t *testing.T) {
	events, rest := Decode([]byte("a中"))
	if len(rest) != 0 {
		t.Fatalf("rest = %q", rest)
	}
	if len(events) != 2 {
		t.Fatalf("events = %v, want two", events)
	}
	if events[0].Rune != 'a' || events[1].Rune != '中' {
		t.Errorf("runes = %q %q", events[0].Rune, events[1].Rune)
	}
}

func TestDecodeHoldsAnIncompleteEscapeSequence(t *testing.T) {
	events, rest := Decode([]byte("\x1b"))
	if len(events) != 0 {
		t.Fatalf("events = %v, a lone ESC must wait", events)
	}
	if string(rest) != "\x1b" {
		t.Fatalf("rest = %q", rest)
	}

	events, rest = Decode([]byte("\x1b["))
	if len(events) != 0 || string(rest) != "\x1b[" {
		t.Fatalf("events = %v rest = %q", events, rest)
	}

	// A chunk split in the middle of a sequence still decodes once joined.
	first, rest := Decode([]byte("\x1b["))
	second, rest2 := Decode(append([]byte(nil), append(rest, []byte("A")...)...))
	if len(first) != 0 || len(rest2) != 0 || len(second) != 1 || second[0].Key != KeyUp {
		t.Fatalf("second = %v rest2 = %q", second, rest2)
	}
}

func TestDecodeHoldsPartialUTF8(t *testing.T) {
	full := []byte("中")
	events, rest := Decode(full[:1])
	if len(events) != 0 || len(rest) != 1 {
		t.Fatalf("events = %v rest = %q, want the byte held back", events, rest)
	}
}

func TestDecodeForceTurnsALoneEscapeIntoBack(t *testing.T) {
	events := DecodeForce([]byte("\x1b"))
	if len(events) != 1 || events[0].Key != KeyEsc {
		t.Fatalf("events = %v, want KeyEsc", events)
	}
}

func TestDecodeIgnoresUnknownSequences(t *testing.T) {
	events, _ := Decode([]byte("\x1b[1;5C")) // Ctrl-Right
	if len(events) != 0 {
		t.Fatalf("events = %v, want nothing", events)
	}
}

func TestDecodeMultipleKeysInOneChunk(t *testing.T) {
	events, rest := Decode([]byte("\x1b[Bq\r"))
	if len(rest) != 0 {
		t.Fatalf("rest = %q", rest)
	}
	if len(events) != 3 || events[0].Key != KeyDown || events[1].Rune != 'q' || events[2].Key != KeyEnter {
		t.Fatalf("events = %v", events)
	}
}
