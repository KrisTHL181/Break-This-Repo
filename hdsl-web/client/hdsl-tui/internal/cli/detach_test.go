package cli

import (
	"bytes"
	"testing"

	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/pterm"
)

func TestDetachSequences(t *testing.T) {
	cases := []struct {
		name     string
		chunks   [][]byte
		forward  string
		command  pterm.Command
		armedEnd bool
	}{
		{name: "plain bytes pass through", chunks: [][]byte{[]byte("ls\n")}, forward: "ls\n"},
		{name: "ctrl-c passes through", chunks: [][]byte{{0x03}}, forward: "\x03"},
		{name: "arrow keys pass through", chunks: [][]byte{{0x1b, '[', 'A'}}, forward: "\x1b[A"},
		{name: "ctrl-] then q detaches", chunks: [][]byte{{0x1d, 'q'}}, command: pterm.CommandDetach},
		{name: "ctrl-] then k kills", chunks: [][]byte{{0x1d, 'k'}}, command: pterm.CommandKill},
		{name: "uppercase works too", chunks: [][]byte{{0x1d, 'Q'}}, command: pterm.CommandDetach},
		{name: "ctrl-] alone arms the machine", chunks: [][]byte{{0x1d}}, armedEnd: true},
		{name: "arms across chunks", chunks: [][]byte{{0x1d}, []byte("q")}, command: pterm.CommandDetach},
		{name: "ctrl-] ctrl-] forwards one prefix", chunks: [][]byte{{0x1d, 0x1d}}, forward: "\x1d"},
		{name: "other key after ctrl-] is forwarded", chunks: [][]byte{{0x1d, 'a'}}, forward: "a"},
		{name: "other ctrl after ctrl-] is forwarded", chunks: [][]byte{{0x1d, 0x03}}, forward: "\x03"},
		{name: "bytes before the prefix are kept", chunks: [][]byte{[]byte("ab\x1d"), []byte("q")}, forward: "ab", command: pterm.CommandDetach},
		{name: "bytes after the command are dropped", chunks: [][]byte{{'x', 0x1d, 'q', 'y'}}, forward: "x", command: pterm.CommandDetach},
	}

	for _, testCase := range cases {
		t.Run(testCase.name, func(t *testing.T) {
			detach := &Detach{}
			var forwarded bytes.Buffer
			command := pterm.CommandNone
			for _, chunk := range testCase.chunks {
				out, cmd := detach.Feed(chunk)
				forwarded.Write(out)
				if cmd != pterm.CommandNone {
					command = cmd
				}
			}
			if forwarded.String() != testCase.forward {
				t.Errorf("forwarded = %q, want %q", forwarded.String(), testCase.forward)
			}
			if command != testCase.command {
				t.Errorf("command = %v, want %v", command, testCase.command)
			}
			if detach.Armed() != testCase.armedEnd {
				t.Errorf("armed = %v, want %v", detach.Armed(), testCase.armedEnd)
			}
		})
	}
}

func TestDetachDoesNotMutateTheInputBuffer(t *testing.T) {
	chunk := []byte{0x1d, 0x1d}
	detach := &Detach{}
	forward, _ := detach.Feed(chunk)
	if len(forward) != 1 || forward[0] != 0x1d {
		t.Fatalf("forward = %v", forward)
	}
	if chunk[0] != 0x1d || chunk[1] != 0x1d {
		t.Errorf("the caller's buffer was modified: %v", chunk)
	}
}
