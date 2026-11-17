package ui

import "testing"

func TestTextAreaTypingNewlineAndSubmit(t *testing.T) {
	area := NewTextArea(20, 3)
	for _, r := range "你好" {
		area.Handle(Event{Key: KeyRune, Rune: r})
	}
	if submit := area.Handle(Event{Key: KeyNewline}); submit {
		t.Fatal("Ctrl-J must insert a newline, not submit")
	}
	area.Handle(Event{Key: KeyRune, Rune: 'a'})
	if area.String() != "你好\na" {
		t.Fatalf("value = %q", area.String())
	}
	if !area.Handle(Event{Key: KeyEnter}) {
		t.Fatal("Enter must submit")
	}
}

func TestTextAreaCaretAndWrapping(t *testing.T) {
	area := NewTextArea(6, 3)
	area.SetValue("abcdef")
	area.Handle(Event{Key: KeyLeft})
	area.Handle(Event{Key: KeyLeft})
	area.Handle(Event{Key: KeyRune, Rune: 'X'})
	if area.String() != "abcdXef" {
		t.Fatalf("value = %q", area.String())
	}
	lines := area.Lines(false)
	if len(lines) != 2 {
		t.Fatalf("wrapped rows = %d (%q)", len(lines), lines)
	}
}

func TestTextAreaHistory(t *testing.T) {
	area := NewTextArea(40, 3)
	area.AddHistory("first")
	area.AddHistory("second")
	area.Handle(Event{Key: KeyHistoryPrev})
	if area.String() != "second" {
		t.Fatalf("prev = %q", area.String())
	}
	area.Handle(Event{Key: KeyHistoryPrev})
	if area.String() != "first" {
		t.Fatalf("prev2 = %q", area.String())
	}
	area.Handle(Event{Key: KeyHistoryNext})
	if area.String() != "second" {
		t.Fatalf("next = %q", area.String())
	}
}

func TestTextAreaArrowsWalkHistoryAtTheEdges(t *testing.T) {
	area := NewTextArea(40, 4)
	area.AddHistory("older")
	area.SetValue("draft")
	area.Handle(Event{Key: KeyUp})
	if area.String() != "older" {
		t.Fatalf("up at the top row = %q, want history", area.String())
	}
	area.Handle(Event{Key: KeyDown})
	if area.String() != "draft" {
		t.Fatalf("down at the bottom row = %q, want the draft", area.String())
	}
}

func TestTextAreaUpDownKeepsColumn(t *testing.T) {
	area := NewTextArea(40, 3)
	area.SetValue("abc\ndef")
	// Caret at the end of "def"; Up should land at the end of "abc".
	area.Handle(Event{Key: KeyUp})
	if area.Cursor != 3 {
		t.Fatalf("cursor = %d, want 3", area.Cursor)
	}
	area.Handle(Event{Key: KeyDown})
	if area.Cursor != len(area.Value) {
		t.Fatalf("cursor = %d, want %d", area.Cursor, len(area.Value))
	}
}
