package main

import (
	"bytes"
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"net/url"
	"strings"
	"testing"

	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/cli"
	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/pterm"
)

// The report this pins: `--insecure` used to require a typed `yes`, so a
// scripted or ECS run died with "需要交互确认，但当前无法读取输入" and had no way
// to connect at all. With stdin not a terminal the warning still prints, the
// confirmation is skipped, and the explicit flag is what carries the consent.
func TestInsecureWithoutATerminalSkipsThePromptButStillWarns(t *testing.T) {
	server := httptest.NewTLSServer(http.HandlerFunc(func(http.ResponseWriter, *http.Request) {}))
	defer server.Close()
	base, err := url.Parse(server.URL)
	if err != nil {
		t.Fatal(err)
	}

	var notes []string
	config, err := tlsConfigFor("", true, base, strings.NewReader(""), io.Discard,
		func(format string, args ...any) { notes = append(notes, fmt.Sprintf(format, args...)) })
	if err != nil {
		t.Fatalf("tlsConfigFor: %v", err)
	}
	if !config.InsecureSkipVerify {
		t.Error("InsecureSkipVerify = false, want verification skipped")
	}
	mention := false
	for _, note := range notes {
		if strings.Contains(note, "不是终端") {
			mention = true
		}
	}
	if !mention {
		t.Errorf("no non-interactive note was logged: %v", notes)
	}
}

func TestVersionPrintsAndDoesNotTouchTheNetwork(t *testing.T) {
	var stdout, stderr bytes.Buffer
	code := run([]string{"--version"}, strings.NewReader(""), &stdout, &stderr)
	if code != cli.ExitOK {
		t.Fatalf("exit code = %d, want 0", code)
	}
	if !strings.Contains(stdout.String(), version) {
		t.Errorf("stdout = %q, want the version", stdout.String())
	}
	if !strings.Contains(stdout.String(), fmt.Sprintf("terminal protocol %d", pterm.Protocol)) {
		t.Errorf("stdout = %q, want the protocol number", stdout.String())
	}
	if stderr.Len() != 0 {
		t.Errorf("stderr = %q, want nothing", stderr.String())
	}
}

func TestUsageErrors(t *testing.T) {
	cases := []struct {
		name string
		args []string
	}{
		{"no panel address", []string{}},
		{"unknown brand", []string{"--brand", "vscode", "https://panel.test"}},
		{"address without a scheme", []string{"panel.example.com"}},
		{"address without a host", []string{"https://"}},
		{"two addresses", []string{"https://a.test", "https://b.test"}},
		{"unknown flag", []string{"--nope", "https://panel.test"}},
	}
	for _, testCase := range cases {
		t.Run(testCase.name, func(t *testing.T) {
			var stdout, stderr bytes.Buffer
			code := run(testCase.args, strings.NewReader(""), &stdout, &stderr)
			if code != cli.ExitUsage {
				t.Fatalf("exit code = %d, want %d (stderr %q)", code, cli.ExitUsage, stderr.String())
			}
			if !strings.Contains(stderr.String(), "用法:") {
				t.Errorf("stderr does not explain the usage:\n%s", stderr.String())
			}
		})
	}
}

func TestHelpExitsCleanly(t *testing.T) {
	var stdout, stderr bytes.Buffer
	if code := run([]string{"--help"}, strings.NewReader(""), &stdout, &stderr); code != cli.ExitOK {
		t.Fatalf("exit code = %d, want 0", code)
	}
	if !strings.Contains(stdout.String(), "退出码:") {
		t.Errorf("help does not list the exit codes:\n%s", stdout.String())
	}
	if !strings.Contains(stdout.String(), "用法:") {
		t.Errorf("help does not show the usage:\n%s", stdout.String())
	}
}

func TestBrandNormalisation(t *testing.T) {
	if got := normaliseBrand("dsh"); got != "" {
		t.Errorf("normaliseBrand(dsh) = %q, want the DSH instance surface", got)
	}
	if got := normaliseBrand("kimi"); got != "kimi" {
		t.Errorf("normaliseBrand(kimi) = %q", got)
	}
	if got := normaliseBrand("pi-web"); got != "pi-web" {
		t.Errorf("normaliseBrand(pi-web) = %q", got)
	}
	if got := normaliseBrand(""); got != "" {
		t.Errorf("normaliseBrand(\"\") = %q", got)
	}
}

func TestParseArgsAcceptsTheDocumentedForm(t *testing.T) {
	opts, base, err := parseArgs([]string{
		"--instance", "dsh-1", "--brand", "kimi", "--resume", "s-9",
		"--user", "admin", "--ca", "/tmp/ca.pem", "--insecure", "--verbose",
		"https://panel.test:8443/",
	})
	if err != nil {
		t.Fatalf("parseArgs: %v", err)
	}
	if opts.instance != "dsh-1" || opts.brand != "kimi" || opts.resume != "s-9" ||
		opts.user != "admin" || opts.ca != "/tmp/ca.pem" || !opts.insecure || !opts.verbose {
		t.Errorf("options = %+v", opts)
	}
	if base.String() != "https://panel.test:8443" {
		t.Errorf("base = %q, the trailing slash should be trimmed", base.String())
	}
}

// The flag package stops parsing at the first positional argument, so
// `hdsl-tui URL --insecure` used to reach the address check as two positionals
// and was refused with "只需要一个面板地址，收到 2 个" — a command that looks
// right and one that is wrong, indistinguishable. Both orders parse to the same
// options now. (A second address is still an error; TestUsageErrors pins that.)
func TestParseArgsAcceptsOptionsOnEitherSideOfTheAddress(t *testing.T) {
	address := "https://panel.test:8443/"
	flags := []string{"--instance", "dsh-1", "--brand", "kimi", "--resume", "s-9",
		"--user", "admin", "--ca", "/tmp/ca.pem", "--insecure", "--verbose"}
	cases := []struct {
		name string
		args []string
	}{
		{"options before the address", append(append([]string{}, flags...), address)},
		{"options after the address", append([]string{address}, flags...)},
		{"options on both sides", append(
			append([]string{"--brand", "kimi", address}, "--insecure", "--instance", "dsh-1"),
			"--verbose", "--resume", "s-9", "--user", "admin", "--ca", "/tmp/ca.pem")},
	}
	for _, testCase := range cases {
		t.Run(testCase.name, func(t *testing.T) {
			opts, base, err := parseArgs(testCase.args)
			if err != nil {
				t.Fatalf("parseArgs(%v): %v", testCase.args, err)
			}
			if base == nil || base.String() != "https://panel.test:8443" {
				t.Fatalf("base = %v, want the one address", base)
			}
			if opts.instance != "dsh-1" || opts.brand != "kimi" || opts.resume != "s-9" ||
				opts.user != "admin" || opts.ca != "/tmp/ca.pem" || !opts.insecure || !opts.verbose {
				t.Errorf("options = %+v", opts)
			}
		})
	}
}

func TestParseArgsRefusesATypoAfterTheAddress(t *testing.T) {
	if _, _, err := parseArgs([]string{"https://panel.test", "--instnace", "dsh-1"}); err == nil {
		t.Error("an unknown option after the address was accepted")
	}
}
