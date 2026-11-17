package app

import (
	"strings"
	"testing"

	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/panel"
)

func TestAgentPromptUsesLoopbackAndCarriesTheTicket(t *testing.T) {
	ticket := panel.AgentToken{
		Token:     "tok-123",
		Scopes:    []string{"read", "mc"},
		ExpiresAt: "2026-11-04T12:34:56Z",
	}
	prompt := agentPrompt("https://nas200.top:3080", ticket)
	for _, want := range []string{
		"https://127.0.0.1:3080",
		"Bearer tok-123",
		"读取面板、Minecraft",
		"与开发者提权同生命周期",
	} {
		if !strings.Contains(prompt, want) {
			t.Errorf("prompt missing %q:\n%s", want, prompt)
		}
	}
	if strings.Contains(prompt, "nas200.top") {
		t.Errorf("prompt leaked the real host:\n%s", prompt)
	}
}

func TestLoopbackPanelURL(t *testing.T) {
	cases := []struct{ in, want string }{
		{"https://nas200.top:3080", "https://127.0.0.1:3080"},
		{"https://nas200.top", "https://127.0.0.1:3080"},
		{"https://nas200.top:443", "https://127.0.0.1:3080"},
		{"http://192.168.1.5:3081", "https://127.0.0.1:3081"},
	}
	for _, c := range cases {
		if got := loopbackPanelURL(c.in); got != c.want {
			t.Errorf("loopbackPanelURL(%q) = %q, want %q", c.in, got, c.want)
		}
	}
}

func TestScopeTextFallsBackToTheID(t *testing.T) {
	if got := scopeText([]string{"read", "future"}); got != "读取面板、future" {
		t.Errorf("scopeText = %q", got)
	}
	if got := scopeText(nil); got != "（无）" {
		t.Errorf("scopeText(nil) = %q", got)
	}
}
