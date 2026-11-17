package app

import (
	"strings"
	"testing"

	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/panel"
	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/ui"
)

// Pressing Enter on an unread screen must return to the server list, never
// disable certificate verification: the menu starts on the first enabled item.
func TestCertificateRiskPageDefaultsToGoingBack(t *testing.T) {
	page := certificateRiskPage("https://192.0.2.10:3080", &panel.CertificateError{
		Host:   "192.0.2.10",
		Reason: "证书里没有 192.0.2.10",
	})
	if len(page.Items) < 2 {
		t.Fatalf("page has %d items, want a back row and a risk row", len(page.Items))
	}
	if page.Items[0].Disabled {
		t.Error("the first row is disabled, so the list would start on the risky one")
	}
	if !strings.Contains(page.Items[0].Label, "返回") {
		t.Errorf("first row = %q, want the way back", page.Items[0].Label)
	}
	if strings.Contains(page.Items[1].Label, "返回") {
		t.Errorf("second row = %q, want the risky choice", page.Items[1].Label)
	}
}

func TestCertificateRiskPageSaysWhatIsWrong(t *testing.T) {
	page := certificateRiskPage("https://192.0.2.10:3080", &panel.CertificateError{
		Reason: "证书里没有 192.0.2.10，它只签给了别的名字",
	})
	if !strings.Contains(page.Detail, "192.0.2.10") {
		t.Errorf("detail = %q, want the certificate reason", page.Detail)
	}
	if !strings.Contains(page.Detail, "本次运行") {
		t.Errorf("detail = %q, want it to say the choice is one-run only", page.Detail)
	}
	if page.Subtitle != "https://192.0.2.10:3080" {
		t.Errorf("subtitle = %q, want the panel address", page.Subtitle)
	}
}

func TestStatusLineMarksAnUnverifiedConnection(t *testing.T) {
	app := &App{opts: Options{Version: "test"}, server: "https://192.0.2.10:3080"}
	if strings.Contains(app.statusLine(), "证书未验证") {
		t.Error("status line claims an unverified connection before one happened")
	}
	app.unverified = true
	if !strings.Contains(app.statusLine(), "证书未验证") {
		t.Errorf("status line = %q, want the unverified marker", app.statusLine())
	}
}

// On a short screen the explanation must give way to the two choices: a page
// whose rows are clipped is a page the user cannot answer.
func TestDetailLinesStopsBeforeTheChoicesDisappear(t *testing.T) {
	long := strings.Repeat("证书里没有这个地址，它只签给了别的名字。", 12)
	lines := detailLines(long, 40, 3)
	if len(lines) != 3 {
		t.Fatalf("detailLines returned %d lines, want the limit of 3", len(lines))
	}
	if !strings.HasSuffix(lines[2], "…") {
		t.Errorf("last line = %q, want it to end in an ellipsis", lines[2])
	}
	for _, line := range lines {
		if ui.VisibleWidth(line) > 40 {
			t.Errorf("line %q is wider than the column", line)
		}
	}
}

func TestDetailLinesLeavesShortTextAlone(t *testing.T) {
	lines := detailLines("证书已过期", 40, 5)
	if len(lines) != 1 || lines[0] != "证书已过期" {
		t.Errorf("detailLines = %q", lines)
	}
}
