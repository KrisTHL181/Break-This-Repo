// Package md turns the chat transcript's Markdown into styled terminal lines.
//
// It is deliberately a subset: headings, bold, inline code, fenced code, lists,
// quotes, rules, links and tables. Everything is line-based, and the styles
// only change foreground/attributes — never the background — because the chat
// screen paints every line on its own background first.
package md

import (
	"strings"

	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/ui"
)

// Style kinds. The renderer emits SGR that keeps the screen background.
const (
	stylePlain = ""
	styleBold  = "\x1b[1m"
	styleDim   = "\x1b[36m"
	styleCode  = "\x1b[7m"
	styleLink  = "\x1b[1;36m"
)

const (
	resetBold = "\x1b[22m"
	resetDim  = "\x1b[37m"
	resetCode = "\x1b[27m"
	resetLink = "\x1b[22m"
)

type span struct {
	text  string
	style string
}

// Render formats Markdown for a chat body `width` cells wide.
//
// Control characters other than tab are dropped: model output is untrusted,
// and an escape sequence that reaches the terminal is a real injection.
func Render(text string, width int) []string {
	if width < 8 {
		width = 8
	}
	text = sanitize(text)
	var lines []string
	inCode := false
	for _, raw := range strings.Split(text, "\n") {
		line := strings.TrimRight(raw, "\r")
		trimmed := strings.TrimSpace(line)

		if strings.HasPrefix(trimmed, "```") {
			inCode = !inCode
			continue
		}
		if inCode {
			for _, wrapped := range wrapPlain("  "+line, width, styleDim) {
				lines = append(lines, wrapped)
			}
			continue
		}
		if trimmed == "" {
			lines = append(lines, "")
			continue
		}
		if isRule(trimmed) {
			lines = append(lines, styleDim+strings.Repeat("─", width)+resetDim)
			continue
		}
		if heading, ok := headingText(trimmed); ok {
			lines = append(lines, wrapSpan([]span{{heading, styleBold}}, width, "")...)
			continue
		}
		if quote, ok := quoteText(trimmed); ok {
			for _, wrapped := range wrapSpan([]span{{quote, styleDim}}, width-2, styleDim) {
				lines = append(lines, styleDim+"│ "+resetDim+wrapped)
			}
			continue
		}
		if item, ok := listText(trimmed); ok {
			wrapped := wrapSpan(parseInline(item), width-2, "")
			for index, entry := range wrapped {
				if index == 0 {
					lines = append(lines, "• "+entry)
				} else {
					lines = append(lines, "  "+entry)
				}
			}
			continue
		}
		lines = append(lines, wrapSpan(parseInline(line), width, "")...)
	}
	return lines
}

// LinkLine returns a plain "title — url" line for a source list.
func LinkLine(sourceTitle, sourceURL string) string {
	if sourceTitle == "" {
		return sourceURL
	}
	if sourceURL == "" {
		return sourceTitle
	}
	return sourceTitle + " — " + sourceURL
}

func isRule(line string) bool {
	if len(line) < 3 {
		return false
	}
	compact := strings.ReplaceAll(line, " ", "")
	if len(compact) < 3 {
		return false
	}
	for _, ch := range compact {
		if ch != '-' && ch != '*' && ch != '_' {
			return false
		}
	}
	return true
}

func headingText(line string) (string, bool) {
	count := 0
	for count < len(line) && line[count] == '#' {
		count++
	}
	if count == 0 || count > 6 || count >= len(line) || line[count] != ' ' {
		return "", false
	}
	return strings.TrimSpace(line[count:]), true
}

func quoteText(line string) (string, bool) {
	if !strings.HasPrefix(line, ">") {
		return "", false
	}
	return strings.TrimSpace(strings.TrimPrefix(line, ">")), true
}

func listText(line string) (string, bool) {
	for _, prefix := range []string{"- ", "* ", "+ "} {
		if strings.HasPrefix(line, prefix) {
			return strings.TrimSpace(line[len(prefix):]), true
		}
	}
	index := 0
	for index < len(line) && line[index] >= '0' && line[index] <= '9' {
		index++
	}
	if index > 0 && index+1 < len(line) && (line[index] == '.' || line[index] == ')') && line[index+1] == ' ' {
		return strings.TrimSpace(line[index+2:]), true
	}
	return "", false
}

// parseInline turns one line into styled spans: bold, inline code and links.
func parseInline(line string) []span {
	var spans []span
	var plain strings.Builder
	flush := func() {
		if plain.Len() > 0 {
			spans = append(spans, span{plain.String(), stylePlain})
			plain.Reset()
		}
	}
	for index := 0; index < len(line); {
		switch {
		case strings.HasPrefix(line[index:], "**"):
			if end := strings.Index(line[index+2:], "**"); end >= 0 {
				flush()
				spans = append(spans, span{line[index+2 : index+2+end], styleBold})
				index += 2 + end + 2
				continue
			}
		case line[index] == '`':
			if end := strings.IndexByte(line[index+1:], '`'); end >= 0 {
				flush()
				spans = append(spans, span{line[index+1 : index+1+end], styleCode})
				index += 1 + end + 1
				continue
			}
		case strings.HasPrefix(line[index:], "!["):
			if text, next, ok := linkAt(line, index+1); ok {
				flush()
				spans = append(spans, span{"[图片: " + text + "]", styleDim})
				index = next
				continue
			}
		case line[index] == '[':
			if text, next, ok := linkAt(line, index); ok {
				flush()
				spans = append(spans, span{text, styleLink})
				index = next
				continue
			}
		}
		plain.WriteByte(line[index])
		index++
	}
	flush()
	return spans
}

// linkAt parses `[text](url)` starting at line[index] == '['.
func linkAt(line string, index int) (string, int, bool) {
	if index >= len(line) || line[index] != '[' {
		return "", index, false
	}
	closeBracket := strings.IndexByte(line[index:], ']')
	if closeBracket < 0 {
		return "", index, false
	}
	text := line[index+1 : index+closeBracket]
	after := index + closeBracket + 1
	if after >= len(line) || line[after] != '(' {
		return text, after, true
	}
	closeParen := strings.IndexByte(line[after:], ')')
	if closeParen < 0 {
		return text, after, true
	}
	url := line[after+1 : after+closeParen]
	next := after + closeParen + 1
	if url == "" || url == text {
		return text, next, true
	}
	return text + " (" + url + ")", next, true
}

// wrapSpan wraps styled spans to width, preferring spaces and breaking wide
// (CJK) runes individually. Consecutive cells of one style are emitted as one
// SGR run, so a Chinese link is not repainted character by character.
func wrapSpan(spans []span, width int, prefixStyle string) []string {
	if width < 1 {
		width = 1
	}
	tokens := tokenize(spans)
	var rows [][]token
	current := []token{}
	used := 0
	flush := func() {
		rows = append(rows, trimTokens(current))
		current = nil
		used = 0
	}
	for _, tok := range tokens {
		cells := ui.VisibleWidth(tok.text)
		if used+cells > width && used > 0 {
			flush()
			if strings.TrimSpace(tok.text) == "" {
				continue
			}
		}
		if cells > width {
			for _, chunk := range splitCells(tok.text, width) {
				current = append(current, token{chunk, tok.style, tok.word})
				used += ui.VisibleWidth(chunk)
				flush()
			}
			continue
		}
		current = append(current, tok)
		used += cells
	}
	if len(current) > 0 || len(rows) == 0 {
		flush()
	}
	lines := make([]string, 0, len(rows))
	for _, row := range rows {
		lines = append(lines, prefixStyle+renderTokens(row))
	}
	return lines
}

func trimTokens(tokens []token) []token {
	for len(tokens) > 0 && strings.TrimSpace(tokens[len(tokens)-1].text) == "" {
		tokens = tokens[:len(tokens)-1]
	}
	return tokens
}

func renderTokens(tokens []token) string {
	var builder strings.Builder
	for index := 0; index < len(tokens); {
		style := tokens[index].style
		var run strings.Builder
		for index < len(tokens) && tokens[index].style == style {
			run.WriteString(tokens[index].text)
			index++
		}
		builder.WriteString(open(style))
		builder.WriteString(run.String())
		builder.WriteString(close(style))
	}
	return builder.String()
}

type token struct {
	text  string
	style string
	word  bool
}

func tokenize(spans []span) []token {
	var tokens []token
	for _, item := range spans {
		for _, r := range item.text {
			if r == '\t' {
				r = ' '
			}
			if r == ' ' {
				tokens = append(tokens, token{" ", item.style, false})
				continue
			}
			if isWide(r) {
				tokens = append(tokens, token{string(r), item.style, false})
				continue
			}
			// ASCII word: extend the previous word of the same style.
			if len(tokens) > 0 {
				last := &tokens[len(tokens)-1]
				if last.word && last.style == item.style {
					last.text += string(r)
					continue
				}
			}
			tokens = append(tokens, token{string(r), item.style, true})
		}
	}
	return tokens
}

func isWide(r rune) bool {
	return ui.VisibleWidth(string(r)) > 1
}

func splitCells(text string, width int) []string {
	var chunks []string
	var current strings.Builder
	used := 0
	for _, r := range text {
		cell := ui.VisibleWidth(string(r))
		if cell < 1 {
			cell = 1
		}
		if used+cell > width && used > 0 {
			chunks = append(chunks, current.String())
			current.Reset()
			used = 0
		}
		current.WriteRune(r)
		used += cell
	}
	if current.Len() > 0 {
		chunks = append(chunks, current.String())
	}
	return chunks
}

func wrapPlain(text string, width int, style string) []string {
	return wrapSpan([]span{{text, style}}, width, "")
}

func open(style string) string { return style }
func close(style string) string {
	switch style {
	case styleBold:
		return resetBold
	case styleDim:
		return resetDim
	case styleCode:
		return resetCode
	case styleLink:
		return resetLink
	default:
		return ""
	}
}

func sanitize(text string) string {
	var builder strings.Builder
	for _, r := range text {
		if r == '\n' || r == '\t' || r >= 0x20 {
			builder.WriteRune(r)
		}
	}
	return builder.String()
}
