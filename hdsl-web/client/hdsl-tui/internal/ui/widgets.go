package ui

// Item is one row of a menu.
type Item struct {
	// Label is the text the cursor sits on.
	Label string
	// Note is right-aligned secondary text: a state, a count, "上次使用".
	Note string
	// Disabled rows are shown but cannot be chosen.
	Disabled bool
}

// List is a vertical menu with a moving cursor. Index is the highlighted row;
// Top is the first visible row, kept in sync by Lines.
type List struct {
	Items []Item
	Index int
	Top   int
}

// Selected returns the highlighted item, if any.
func (l *List) Selected() (Item, bool) {
	if l.Index < 0 || l.Index >= len(l.Items) {
		return Item{}, false
	}
	return l.Items[l.Index], true
}

// Move shifts the cursor by delta, skipping disabled rows and stopping at the
// ends (menus do not wrap: the top and bottom are a visible boundary).
func (l *List) Move(delta int) {
	if len(l.Items) == 0 {
		return
	}
	index := l.Index
	for {
		index += delta
		if index < 0 || index >= len(l.Items) {
			return
		}
		if !l.Items[index].Disabled {
			l.Index = index
			return
		}
	}
}

// ToStart and ToEnd jump to the first/last enabled row.
func (l *List) ToStart() {
	if len(l.Items) == 0 {
		return
	}
	l.Index = 0
	if l.Items[0].Disabled {
		l.Move(1)
	}
}

// ToEnd jumps to the last enabled row.
func (l *List) ToEnd() {
	l.Index = len(l.Items) - 1
	if l.Index >= 0 && l.Items[l.Index].Disabled {
		l.Move(-1)
	}
}

// Lines renders the visible rows, one per line, each exactly width cells.
func (l *List) Lines(width, height int) []string {
	if len(l.Items) == 0 {
		return []string{Fill("  （空）", width, BodyDim)}
	}
	if height < 1 {
		height = 1
	}
	if l.Index < 0 {
		l.Index = 0
	}
	if l.Index >= len(l.Items) {
		l.Index = len(l.Items) - 1
	}
	if l.Top > l.Index {
		l.Top = l.Index
	}
	if l.Index >= l.Top+height {
		l.Top = l.Index - height + 1
	}
	if l.Top < 0 {
		l.Top = 0
	}

	lines := make([]string, 0, height)
	for index := l.Top; index < len(l.Items) && len(lines) < height; index++ {
		lines = append(lines, l.row(index, width))
	}
	for len(lines) < height {
		lines = append(lines, Fill("", width, Body))
	}
	return lines
}

func (l *List) row(index, width int) string {
	item := l.Items[index]
	style := Body
	switch {
	case index == l.Index && !item.Disabled:
		style = Selected
	case item.Disabled:
		style = BodyDim
	}
	label := "  " + item.Label
	note := item.Note
	if note != "" {
		room := width - VisibleWidth(note) - 2
		if room < VisibleWidth(label)+2 {
			note = ""
		}
	}
	row := label
	if note != "" {
		gap := width - VisibleWidth(label) - VisibleWidth(note) - 2
		if gap < 1 {
			gap = 1
		}
		row = label + Repeat(" ", gap) + note
	}
	return Fill(row, width, style)
}

// Input is a single-line text field. Value is []rune so a Chinese address or
// an emoji does not land in the middle of a UTF-8 sequence.
type Input struct {
	Value  []rune
	Cursor int
	Masked bool
}

// String is the value as text.
func (i *Input) String() string { return string(i.Value) }

// Handle applies one event. It reports whether the value changed.
func (i *Input) Handle(event Event) bool {
	switch event.Key {
	case KeyRune:
		if event.Rune < 0x20 {
			return false
		}
		i.Value = append(i.Value, 0)
		copy(i.Value[i.Cursor+1:], i.Value[i.Cursor:])
		i.Value[i.Cursor] = event.Rune
		i.Cursor++
		return true
	case KeyBackspace:
		if i.Cursor == 0 {
			return false
		}
		i.Value = append(i.Value[:i.Cursor-1], i.Value[i.Cursor:]...)
		i.Cursor--
		return true
	case KeyDelete:
		if i.Cursor >= len(i.Value) {
			return false
		}
		i.Value = append(i.Value[:i.Cursor], i.Value[i.Cursor+1:]...)
		return true
	case KeyLeft:
		if i.Cursor > 0 {
			i.Cursor--
		}
	case KeyRight:
		if i.Cursor < len(i.Value) {
			i.Cursor++
		}
	case KeyHome:
		i.Cursor = 0
	case KeyEnd:
		i.Cursor = len(i.Value)
	case KeyClear:
		if len(i.Value) == 0 {
			return false
		}
		i.Value = nil
		i.Cursor = 0
		return true
	}
	return false
}

// Lines renders the field as a three-line box, with the cursor cell inverted.
func (i *Input) Lines(width int, focused bool) []string {
	if width < 6 {
		width = 6
	}
	border := Border
	if focused {
		border = "\x1b[1;36;44m"
	}
	inner := width - 2

	visible := make([]rune, len(i.Value))
	for index, r := range i.Value {
		if i.Masked {
			visible[index] = '•'
		} else {
			visible[index] = r
		}
	}

	// Keep the cursor in view: scroll the field so the tail is visible.
	start := 0
	for start < i.Cursor && Width(string(visible[start:i.Cursor])) >= inner-1 {
		start++
	}
	window := visible[start:]
	text := string(window)
	if focused {
		offset := Width(string(visible[start:i.Cursor]))
		text = cursorCell(string(window), offset)
	}

	return []string{
		ScreenBG + border + "┌" + Repeat("─", inner) + "┐" + Reset,
		ScreenBG + border + "│" + Reset + Fill(" "+text, inner, Body) + ScreenBG + border + "│" + Reset,
		ScreenBG + border + "└" + Repeat("─", inner) + "┘" + Reset,
	}
}

// cursorCell inverts the character at offset (a space at the end of the line),
// which is the client's visible cursor.
//
// The cursor sits *before* the rune at offset: offset 0 inverts the first rune,
// offset len(value) has no rune after it and appends an inverted space. The
// previous version fell through to prepending that space at the end, so the
// cursor appeared at the start of the field even when typing at the end.
func cursorCell(text string, offset int) string {
	runes := []rune(text)
	position := 0
	for index, r := range runes {
		if offset <= position {
			return string(runes[:index]) + "\x1b[7m" + string(r) + "\x1b[27m" + string(runes[index+1:])
		}
		position += runeWidth(r)
	}
	return text + "\x1b[7m \x1b[27m"
}

// Width is the printed width of plain text.
func Width(text string) int {
	width := 0
	for _, r := range text {
		width += runeWidth(r)
	}
	return width
}

// Heading is a line inside a dialog: the question or the section name.
func Heading(text string, width int) string {
	return Fill(text, width, "\x1b[1;30;47m")
}

// Note line inside a dialog: an explanation in a dimmer colour.
func Note(text string, width int) string {
	return Fill(text, width, BodyDim)
}

// Error line inside a dialog.
func ErrorLine(text string, width int) string {
	return Fill(text, width, Danger)
}

// Blank is an empty body line.
func Blank(width int) string { return Fill("", width, Body) }

// Pair renders "label: value" with the value in the accent colour.
func Pair(label, value string, width int) string {
	return Fill("  "+label+"："+value, width, Body)
}

// JoinLines concatenates body blocks.
func JoinLines(blocks ...[]string) []string {
	var joined []string
	for _, block := range blocks {
		joined = append(joined, block...)
	}
	return joined
}
