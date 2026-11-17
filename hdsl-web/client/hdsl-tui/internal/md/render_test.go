package md

import (
	"regexp"
	"strings"
	"testing"

	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/ui"
)

var sgrPattern = regexp.MustCompile("\x1b\\[[0-9;]*m")

func plain(text string) string { return sgrPattern.ReplaceAllString(text, "") }

func TestRenderStylesBoldCodeAndLinks(t *testing.T) {
	lines := Render("**粗体** 和 `代码` 以及 [链接](https://example.com)", 60)
	if len(lines) != 1 {
		t.Fatalf("lines = %q", lines)
	}
	line := lines[0]
	if !strings.Contains(line, "\x1b[1m") || !strings.Contains(line, "\x1b[7m") {
		t.Fatalf("missing bold/code style: %q", line)
	}
	if !strings.Contains(line, "链接 (https://example.com)") {
		t.Fatalf("link = %q", line)
	}
	if ui.VisibleWidth(line) > 60 {
		t.Fatalf("width = %d", ui.VisibleWidth(line))
	}
}

func TestRenderWrapsWideText(t *testing.T) {
	lines := Render(strings.Repeat("中", 30), 10)
	if len(lines) < 3 {
		t.Fatalf("lines = %q", lines)
	}
	for _, line := range lines {
		if ui.VisibleWidth(line) > 10 {
			t.Fatalf("line %q is %d wide", line, ui.VisibleWidth(line))
		}
	}
}

func TestRenderKeepsCodeBlocksLiteralAndDimsThem(t *testing.T) {
	lines := Render("```go\nfmt.Println(\"**x**\")\n```", 40)
	if len(lines) != 1 {
		t.Fatalf("lines = %q", lines)
	}
	if !strings.Contains(lines[0], "fmt.Println") {
		t.Fatalf("code lost: %q", lines[0])
	}
	if strings.Contains(lines[0], "\x1b[1m") {
		t.Fatalf("bold syntax leaked into a code block: %q", lines[0])
	}
}

func TestRenderDropsControlCharacters(t *testing.T) {
	lines := Render("safe \x1b[2J text", 40)
	if len(lines) != 1 || strings.Contains(lines[0], "\x1b[2J") {
		t.Fatalf("control sequence survived: %q", lines)
	}
}

func TestRenderListsAndQuotes(t *testing.T) {
	lines := Render("- 一\n> 引用", 40)
	if len(lines) != 2 {
		t.Fatalf("lines = %q", lines)
	}
	if !strings.Contains(plain(lines[0]), "• 一") {
		t.Fatalf("list = %q", lines[0])
	}
	if !strings.Contains(plain(lines[1]), "│ 引用") {
		t.Fatalf("quote = %q", lines[1])
	}
}
