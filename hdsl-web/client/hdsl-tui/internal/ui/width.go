package ui

import (
	"strings"
	"unicode/utf8"
)

// decodeRune reads one UTF-8 rune, returning size 0 while the bytes for it are
// still incomplete (so the caller can wait for the rest of the chunk).
func decodeRune(data []byte) (rune, int) {
	if len(data) == 0 {
		return 0, 0
	}
	if !utf8.FullRune(data) {
		return 0, 0
	}
	return utf8.DecodeRune(data)
}

// runeWidth is the number of terminal cells a rune occupies: 2 for the East
// Asian wide ranges (CJK text, full-width punctuation), 0 for combining marks,
// 1 otherwise. A zero-width joiner or emoji sequence is beyond this client's
// needs; menus are plain text.
func runeWidth(r rune) int {
	switch {
	case r == 0:
		return 0
	case r < 0x20 || (r >= 0x7f && r < 0xa0):
		return 0
	case r < 0x300:
		return 1
	case r >= 0x300 && r <= 0x36f:
		return 0
	case r == 0x2329 || r == 0x232a:
		return 2
	case r >= 0x1100 && r <= 0x115f:
		return 2
	case r >= 0x2e80 && r <= 0xa4cf:
		return 2
	case r >= 0xac00 && r <= 0xd7a3:
		return 2
	case r >= 0xf900 && r <= 0xfaff:
		return 2
	case r >= 0xfe30 && r <= 0xfe6f:
		return 2
	case r >= 0xff00 && r <= 0xff60:
		return 2
	case r >= 0xffe0 && r <= 0xffe6:
		return 2
	case r >= 0x1f300 && r <= 0x1f64f:
		return 2
	case r >= 0x20000 && r <= 0x3fffd:
		return 2
	}
	return 1
}

// VisibleWidth is the printed width of s, ignoring SGR escape sequences.
func VisibleWidth(s string) int {
	width := 0
	for index := 0; index < len(s); {
		if s[index] == 0x1b && index+1 < len(s) && s[index+1] == '[' {
			index = skipSGR(s, index)
			continue
		}
		r, size := utf8.DecodeRuneInString(s[index:])
		if size == 0 {
			break
		}
		width += runeWidth(r)
		index += size
	}
	return width
}

// skipSGR returns the index just past the escape sequence starting at index.
func skipSGR(s string, index int) int {
	for cursor := index + 2; cursor < len(s); cursor++ {
		if s[cursor] >= 0x40 && s[cursor] <= 0x7e {
			return cursor + 1
		}
	}
	return len(s)
}

// Truncate cuts s to width cells, appending an ellipsis when it had to cut.
func Truncate(s string, width int) string {
	if width <= 0 {
		return ""
	}
	if VisibleWidth(s) <= width {
		return s
	}
	var builder strings.Builder
	used := 0
	for index := 0; index < len(s); {
		if s[index] == 0x1b && index+1 < len(s) && s[index+1] == '[' {
			next := skipSGR(s, index)
			builder.WriteString(s[index:next])
			index = next
			continue
		}
		r, size := utf8.DecodeRuneInString(s[index:])
		if size == 0 {
			break
		}
		cell := runeWidth(r)
		if used+cell > width-1 {
			break
		}
		builder.WriteRune(r)
		used += cell
		index += size
	}
	builder.WriteString("…")
	return builder.String()
}

// Pad appends spaces until s occupies width cells. A string that is already
// wider is returned unchanged.
func Pad(s string, width int) string {
	missing := width - VisibleWidth(s)
	if missing <= 0 {
		return s
	}
	return s + strings.Repeat(" ", missing)
}

// Fill renders s padded to width cells inside style, closed with a reset.
func Fill(s string, width int, style string) string {
	return style + Pad(Truncate(s, width), width) + Reset
}

// Row places an already styled line on the dialog background and pads it to
// width cells, for content that brings its own colours.
func Row(content string, width int) string {
	missing := width - VisibleWidth(content)
	if missing < 0 {
		missing = 0
	}
	return content + ScreenBG + Repeat(" ", missing)
}

// Repeat returns s repeated count times.
func Repeat(s string, count int) string {
	if count <= 0 {
		return ""
	}
	return strings.Repeat(s, count)
}
