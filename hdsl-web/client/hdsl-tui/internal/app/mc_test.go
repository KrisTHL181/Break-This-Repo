package app

import (
	"strings"
	"testing"
)

func TestMCRunning(t *testing.T) {
	cases := map[string]bool{
		"running":  true,
		"starting": true,
		"stopping": true,
		"stopped":  false,
		"failed":   false,
		"":         false,
	}
	for state, want := range cases {
		if got := mcRunning(state); got != want {
			t.Errorf("mcRunning(%q) = %v, want %v", state, got, want)
		}
	}
}

func TestMCStateText(t *testing.T) {
	cases := map[string]string{
		"running":  "运行中",
		"starting": "启动中",
		"stopping": "停止中",
		"stopped":  "已停止",
		"failed":   "失败",
		"weird":    "weird",
	}
	for state, want := range cases {
		if got := mcStateText(state); got != want {
			t.Errorf("mcStateText(%q) = %q, want %q", state, got, want)
		}
	}
}

// The notice is the difference between leaving the console and stopping the
// server. If a future edit drops either half, this test is the reminder.
func TestMCConsoleNoticeNamesTheExitKeyAndWarnsAboutStop(t *testing.T) {
	for _, want := range []string{"Ctrl-]", "q", "stop", "停服"} {
		if !strings.Contains(mcConsoleNotice, want) {
			t.Errorf("mcConsoleNotice is missing %q:\n%s", want, mcConsoleNotice)
		}
	}
}
