package cli

import (
	"bytes"
	"errors"
	"strings"
	"testing"

	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/panel"
)

func groups() []Group {
	return []Group{
		{Title: "DSH 实例", Items: []panel.Instance{
			{ID: "dsh-1", Name: "main", State: "running"},
			{ID: "dsh-2", Name: "second", State: "stopped"},
		}},
		{Title: "kimi 实例", Brand: "kimi", Items: []panel.Instance{
			{ID: "kimi-1", Name: "kimi-one", State: "stopped"},
		}},
		{Title: "opencode 实例", Brand: "opencode"},
		{Title: "claude-web 实例", Brand: "claude-web", Items: []panel.Instance{
			{ID: "cw-1", Name: "claude", State: "stopped"},
		}},
	}
}

func TestPickNumbersAcrossGroups(t *testing.T) {
	var out bytes.Buffer
	choice, err := Pick(strings.NewReader("4\n"), &out, groups())
	if err != nil {
		t.Fatalf("Pick: %v", err)
	}
	if choice.Brand != "claude-web" || choice.ID != "cw-1" {
		t.Errorf("choice = %+v", choice)
	}
	rendered := out.String()
	for _, want := range []string{"1)", "2)", "3)", "4)", "DSH 实例", "kimi 实例", "claude-web 实例"} {
		if !strings.Contains(rendered, want) {
			t.Errorf("menu is missing %q:\n%s", want, rendered)
		}
	}
	if strings.Contains(rendered, "opencode") {
		t.Errorf("an empty group must not be rendered:\n%s", rendered)
	}
}

func TestPickDefaultsToTheDSHSurface(t *testing.T) {
	var out bytes.Buffer
	choice, err := Pick(strings.NewReader("1\n"), &out, groups())
	if err != nil {
		t.Fatalf("Pick: %v", err)
	}
	if choice.Brand != "" || choice.ID != "dsh-1" {
		t.Errorf("choice = %+v, want the dsh instance with an empty brand", choice)
	}
}

func TestPickRetriesInvalidInput(t *testing.T) {
	var out bytes.Buffer
	choice, err := Pick(strings.NewReader("nope\n9\n2\n"), &out, groups())
	if err != nil {
		t.Fatalf("Pick: %v", err)
	}
	if choice.ID != "dsh-2" {
		t.Errorf("choice = %+v", choice)
	}
	if !strings.Contains(out.String(), "输入无效") {
		t.Errorf("the menu did not complain about the bad input:\n%s", out.String())
	}
}

func TestPickQuit(t *testing.T) {
	var out bytes.Buffer
	if _, err := Pick(strings.NewReader("q\n"), &out, groups()); !errors.Is(err, ErrQuit) {
		t.Fatalf("err = %v, want ErrQuit", err)
	}
}

func TestPickWithoutInstances(t *testing.T) {
	var out bytes.Buffer
	_, err := Pick(strings.NewReader("1\n"), &out, []Group{{Title: "DSH 实例"}})
	if err == nil {
		t.Fatal("expected an error when there is nothing to pick")
	}
	if errors.Is(err, ErrQuit) {
		t.Errorf("an empty menu is not the user quitting: %v", err)
	}
}
