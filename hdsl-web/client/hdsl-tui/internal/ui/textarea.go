package ui

import "strings"

// TextArea is the chat composer: a multi-line buffer with a caret, wrapped to
// the dialog width. It is deliberately small — no selection, no mouse — but it
// carries the keys a chat needs: Enter submits, Ctrl-J inserts a newline, the
// arrows move the caret and Ctrl-P/Ctrl-N walk the history.
type TextArea struct {
	// Value is the buffer, as runes so the caret can sit between any two.
	Value []rune
	// Cursor is the caret index into Value (0..len(Value)).
	Cursor int
	// Width is the number of cells a line may use.
	Width int
	// MaxRows bounds how many wrapped rows are returned.
	MaxRows int
	// History is prior submitted messages, oldest first.
	History []string

	hist     int
	saved    []rune
	savedSet bool
}

// NewTextArea returns an empty composer.
func NewTextArea(width, rows int) *TextArea {
	return &TextArea{Width: width, MaxRows: rows, hist: 0}
}

// SetValue replaces the buffer and puts the caret at the end.
func (t *TextArea) SetValue(text string) {
	t.Value = []rune(text)
	t.Cursor = len(t.Value)
}

// String returns the buffer.
func (t *TextArea) String() string { return string(t.Value) }

// Clear empties the buffer.
func (t *TextArea) Clear() {
	t.Value = nil
	t.Cursor = 0
}

// AddHistory remembers a submitted message.
func (t *TextArea) AddHistory(text string) {
	if strings.TrimSpace(text) == "" {
		return
	}
	if len(t.History) > 0 && t.History[len(t.History)-1] == text {
		return
	}
	t.History = append(t.History, text)
	if len(t.History) > 200 {
		t.History = t.History[len(t.History)-100:]
	}
	t.hist = len(t.History)
	t.saved = nil
	t.savedSet = false
}

// Handle applies one key. submitted is true when the key was Enter.
func (t *TextArea) Handle(event Event) (submitted bool) {
	switch event.Key {
	case KeyEnter:
		return true
	case KeyNewline:
		t.insert([]rune{'\n'})
	case KeyRune:
		if event.Rune >= 0x20 {
			t.insert([]rune{event.Rune})
		}
	case KeyBackspace:
		if t.Cursor > 0 {
			t.Value = append(t.Value[:t.Cursor-1], t.Value[t.Cursor:]...)
			t.Cursor--
		}
	case KeyDelete:
		if t.Cursor < len(t.Value) {
			t.Value = append(t.Value[:t.Cursor], t.Value[t.Cursor+1:]...)
		}
	case KeyLeft:
		if t.Cursor > 0 {
			t.Cursor--
		}
	case KeyRight:
		if t.Cursor < len(t.Value) {
			t.Cursor++
		}
	case KeyUp:
		// Kimi's muscle memory: the arrows walk history at the edge of the
		// buffer, and move the caret inside it.
		if t.atFirstRow() {
			t.walkHistory(-1)
		} else {
			t.moveLine(-1)
		}
	case KeyDown:
		if t.atLastRow() {
			t.walkHistory(1)
		} else {
			t.moveLine(1)
		}
	case KeyHome:
		start, _ := t.lineAt(t.Cursor)
		t.Cursor = start
	case KeyEnd:
		_, end := t.lineAt(t.Cursor)
		t.Cursor = end
	case KeyClear:
		t.Clear()
	case KeyHistoryPrev:
		t.walkHistory(-1)
	case KeyHistoryNext:
		t.walkHistory(1)
	}
	return false
}

func (t *TextArea) insert(runes []rune) {
	if len(runes) == 0 {
		return
	}
	next := make([]rune, 0, len(t.Value)+len(runes))
	next = append(next, t.Value[:t.Cursor]...)
	next = append(next, runes...)
	next = append(next, t.Value[t.Cursor:]...)
	t.Value = next
	t.Cursor += len(runes)
}

func (t *TextArea) walkHistory(delta int) {
	if len(t.History) == 0 {
		return
	}
	if !t.savedSet {
		t.saved = append([]rune(nil), t.Value...)
		t.savedSet = true
		t.hist = len(t.History)
	}
	next := t.hist + delta
	if next < 0 {
		next = 0
	}
	if next > len(t.History) {
		next = len(t.History)
	}
	t.hist = next
	if next == len(t.History) {
		t.Value = append([]rune(nil), t.saved...)
	} else {
		t.Value = []rune(t.History[next])
	}
	t.Cursor = len(t.Value)
}

// Lines returns the wrapped rows, with the caret drawn when focused.
func (t *TextArea) Lines(focused bool) []string {
	layout := t.layout()
	first := 0
	if t.MaxRows > 0 && len(layout.rows) > t.MaxRows {
		caretRow, _ := t.rowOf(layout, t.Cursor)
		first = len(layout.rows) - t.MaxRows
		if caretRow < first {
			first = caretRow
		}
	}
	lines := make([]string, 0, t.MaxRows)
	for index := first; index < len(layout.rows); index++ {
		row := layout.rows[index]
		text := row.text
		if focused && t.Cursor >= row.start && t.Cursor <= row.end {
			before := []rune(text)[:min(t.Cursor-row.start, len([]rune(text)))]
			after := []rune(text)[min(t.Cursor-row.start, len([]rune(text))):]
			if len(after) > 0 {
				text = string(before) + "\x1b[7m" + string(after[0]) + "\x1b[27m" + string(after[1:])
			} else {
				text = string(before) + "\x1b[7m \x1b[27m"
			}
		}
		lines = append(lines, text)
		if t.MaxRows > 0 && len(lines) >= t.MaxRows {
			break
		}
	}
	if len(lines) == 0 {
		lines = append(lines, "")
	}
	return lines
}

type visualRow struct {
	start int
	end   int
	text  string
}

type layoutResult struct {
	rows []visualRow
}

// layout splits the buffer into visual rows at newlines and at Width cells.
func (t *TextArea) layout() layoutResult {
	width := t.Width
	if width < 4 {
		width = 4
	}
	var rows []visualRow
	start := 0
	used := 0
	var builder strings.Builder
	flush := func(end int) {
		rows = append(rows, visualRow{start: start, end: end, text: builder.String()})
		builder.Reset()
		used = 0
		start = end
	}
	for index := 0; index < len(t.Value); index++ {
		r := t.Value[index]
		if r == '\n' {
			flush(index)
			start = index + 1
			continue
		}
		cells := runeWidth(r)
		if cells < 1 {
			cells = 1
		}
		if used+cells > width && used > 0 {
			// Break before this rune; its row starts at index.
			flush(index)
		}
		builder.WriteRune(r)
		used += cells
	}
	flush(len(t.Value))
	return layoutResult{rows: rows}
}

func (t *TextArea) atFirstRow() bool {
	index, _ := t.rowOf(t.layout(), t.Cursor)
	return index == 0
}

func (t *TextArea) atLastRow() bool {
	layout := t.layout()
	index, _ := t.rowOf(layout, t.Cursor)
	return index == len(layout.rows)-1
}

func (t *TextArea) rowOf(layout layoutResult, cursor int) (int, int) {
	for index, row := range layout.rows {
		if cursor >= row.start && cursor <= row.end {
			return index, index
		}
	}
	return len(layout.rows) - 1, max(0, len(layout.rows)-1)
}

func (t *TextArea) lineAt(cursor int) (int, int) {
	layout := t.layout()
	for _, row := range layout.rows {
		if cursor >= row.start && cursor <= row.end {
			return row.start, row.end
		}
	}
	return cursor, cursor
}

func (t *TextArea) moveLine(delta int) {
	layout := t.layout()
	rowIndex, _ := t.rowOf(layout, t.Cursor)
	row := layout.rows[rowIndex]
	column := t.Cursor - row.start
	target := rowIndex + delta
	if target < 0 || target >= len(layout.rows) {
		return
	}
	next := layout.rows[target]
	offset := column
	if offset > next.end-next.start {
		offset = next.end - next.start
	}
	t.Cursor = next.start + offset
}

func min(a, b int) int {
	if a < b {
		return a
	}
	return b
}

func max(a, b int) int {
	if a > b {
		return a
	}
	return b
}
