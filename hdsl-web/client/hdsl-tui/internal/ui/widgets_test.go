package ui

import (
	"strings"
	"testing"
)

func TestListMoveSkipsDisabledRowsAndStopsAtTheEnds(t *testing.T) {
	list := List{Items: []Item{
		{Label: "一"},
		{Label: "二", Disabled: true},
		{Label: "三"},
	}}
	list.Move(1)
	if list.Index != 2 {
		t.Errorf("Index = %d, want 2 (skip the disabled row)", list.Index)
	}
	list.Move(1)
	if list.Index != 2 {
		t.Errorf("Index = %d, want to stay at the last row", list.Index)
	}
	list.Move(-1)
	if list.Index != 0 {
		t.Errorf("Index = %d, want 0", list.Index)
	}
	list.Move(-1)
	if list.Index != 0 {
		t.Errorf("Index = %d, want to stay at the first row", list.Index)
	}
}

func TestListJumpsToTheEndsSkippingDisabled(t *testing.T) {
	list := List{Items: []Item{
		{Label: "一", Disabled: true},
		{Label: "二"},
		{Label: "三"},
		{Label: "四", Disabled: true},
	}}
	list.ToStart()
	if list.Index != 1 {
		t.Errorf("ToStart Index = %d, want 1", list.Index)
	}
	list.ToEnd()
	if list.Index != 2 {
		t.Errorf("ToEnd Index = %d, want 2", list.Index)
	}
}

func TestListLinesAreExactWidthAndScroll(t *testing.T) {
	items := make([]Item, 0, 20)
	for index := 0; index < 20; index++ {
		items = append(items, Item{Label: "项目", Note: "备注"})
	}
	list := List{Items: items, Index: 15}
	lines := list.Lines(30, 5)
	if len(lines) != 5 {
		t.Fatalf("lines = %d, want 5", len(lines))
	}
	for _, line := range lines {
		if VisibleWidth(line) != 30 {
			t.Errorf("line width = %d, want 30 (%q)", VisibleWidth(line), line)
		}
	}
	if list.Top != 11 {
		t.Errorf("Top = %d, want 11 so the cursor at 15 stays visible", list.Top)
	}
}

func TestListEmptyRendersOneHintLine(t *testing.T) {
	list := List{}
	lines := list.Lines(20, 3)
	if len(lines) != 1 {
		t.Fatalf("lines = %v", lines)
	}
	if !strings.Contains(lines[0], "空") {
		t.Errorf("line = %q, want the empty hint", lines[0])
	}
}

func TestInputEditing(t *testing.T) {
	input := Input{}
	input.Handle(Event{Key: KeyRune, Rune: 'a'})
	input.Handle(Event{Key: KeyRune, Rune: 'b'})
	input.Handle(Event{Key: KeyRune, Rune: 'c'})
	if input.String() != "abc" || input.Cursor != 3 {
		t.Fatalf("value = %q cursor = %d", input.String(), input.Cursor)
	}
	input.Handle(Event{Key: KeyLeft})
	input.Handle(Event{Key: KeyRune, Rune: 'X'})
	if input.String() != "abXc" {
		t.Errorf("value = %q, want abXc", input.String())
	}
	input.Handle(Event{Key: KeyBackspace})
	if input.String() != "abc" || input.Cursor != 2 {
		t.Errorf("value = %q cursor = %d", input.String(), input.Cursor)
	}
	input.Handle(Event{Key: KeyHome})
	input.Handle(Event{Key: KeyDelete})
	if input.String() != "bc" {
		t.Errorf("value = %q, want bc", input.String())
	}
	input.Handle(Event{Key: KeyEnd})
	input.Handle(Event{Key: KeyRune, Rune: '中'})
	if input.String() != "bc中" {
		t.Errorf("value = %q", input.String())
	}
}

func TestInputMasksAndKeepsTheCursorInView(t *testing.T) {
	input := Input{Masked: true}
	for _, r := range "hunter2hunter2hunter2" {
		input.Handle(Event{Key: KeyRune, Rune: r})
	}
	lines := input.Lines(20, true)
	if len(lines) != 3 {
		t.Fatalf("lines = %d", len(lines))
	}
	for _, line := range lines {
		if strings.ContainsRune(line, 'h') {
			t.Errorf("masked field leaks the value: %q", line)
		}
		if VisibleWidth(line) != 20 {
			t.Errorf("line width = %d (%q)", VisibleWidth(line), line)
		}
	}
}

func TestCursorCellFollowsTheCursor(t *testing.T) {
	cases := []struct {
		text   string
		offset int
		want   string
	}{
		{"", 0, "\x1b[7m \x1b[27m"},
		{"abc", 0, "\x1b[7ma\x1b[27mbc"},
		{"abc", 1, "a\x1b[7mb\x1b[27mc"},
		{"abc", 3, "abc\x1b[7m \x1b[27m"},
		{"中文", 2, "中\x1b[7m文\x1b[27m"},
		{"中文", 4, "中文\x1b[7m \x1b[27m"},
	}
	for _, test := range cases {
		if got := cursorCell(test.text, test.offset); got != test.want {
			t.Errorf("cursorCell(%q, %d) = %q, want %q", test.text, test.offset, got, test.want)
		}
	}
}

func TestInputLinesPutTheCursorAtTheEnd(t *testing.T) {
	input := Input{Value: []rune("abc"), Cursor: 3}
	lines := input.Lines(20, true)
	if !strings.Contains(lines[1], "abc\x1b[7m \x1b[27m") {
		t.Errorf("line = %q, want the inverted space after abc", lines[1])
	}
	if strings.Contains(lines[1], "\x1b[7m \x1b[27mabc") {
		t.Errorf("line = %q, cursor is at the start of the field", lines[1])
	}
	for _, line := range lines {
		if VisibleWidth(line) != 20 {
			t.Errorf("line width = %d (%q)", VisibleWidth(line), line)
		}
	}
}

func TestRenderProducesAFullScreen(t *testing.T) {
	page := Page{
		Title:  "选择服务器",
		Body:   []string{Fill("  https://panel.example.com:8443", 40, Body)},
		Hint:   "↑/↓ 选择    Enter 确定",
		Status: "面板 · 用户 admin",
	}
	lines := Render(page, 50, 16)
	if len(lines) != 16 {
		t.Fatalf("lines = %d, want 16", len(lines))
	}
	for index, line := range lines {
		if VisibleWidth(line) != 50 {
			t.Errorf("line %d is %d cells, want 50", index, VisibleWidth(line))
		}
	}
	joined := strings.Join(lines, "\n")
	if !strings.Contains(joined, "选择服务器") {
		t.Errorf("title missing:\n%s", joined)
	}
	if !strings.Contains(joined, "https://panel.example.com:8443") {
		t.Errorf("body missing:\n%s", joined)
	}
}

func TestRenderHandlesASmallTerminal(t *testing.T) {
	for _, size := range [][2]int{{12, 6}, {20, 8}, {30, 10}} {
		lines := Render(Page{Title: "x"}, size[0], size[1])
		if len(lines) != size[1] {
			t.Errorf("%dx%d: %d lines", size[0], size[1], len(lines))
		}
		for index, line := range lines {
			if VisibleWidth(line) != size[0] {
				t.Errorf("%dx%d line %d is %d cells", size[0], size[1], index, VisibleWidth(line))
			}
		}
	}
}
