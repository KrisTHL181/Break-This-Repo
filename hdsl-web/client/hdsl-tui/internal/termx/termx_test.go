package termx

import (
	"context"
	"os"
	"strings"
	"testing"
	"time"
)

func TestNopIsUsableWithoutATerminal(t *testing.T) {
	restore, err := Nop{}.Enter()
	if err != nil {
		t.Fatalf("Enter: %v", err)
	}
	if err := restore(); err != nil {
		t.Fatalf("restore: %v", err)
	}
	size, err := Nop{}.Size()
	if err != nil {
		t.Fatalf("Size: %v", err)
	}
	if size.Cols != 80 || size.Rows != 24 {
		t.Errorf("size = %+v", size)
	}
}

func TestNopPollStopsWithTheContext(t *testing.T) {
	ctx, cancel := context.WithCancel(context.Background())
	called := make(chan struct{}, 1)
	done := make(chan error, 1)
	go func() {
		done <- Nop{}.Poll(ctx, func(Size) { called <- struct{}{} })
	}()
	select {
	case <-called:
		t.Fatal("Nop must not report a size it did not observe")
	case <-time.After(20 * time.Millisecond):
	}
	cancel()
	select {
	case err := <-done:
		if err != nil {
			t.Errorf("Poll: %v", err)
		}
	case <-time.After(time.Second):
		t.Fatal("Poll ignored the cancelled context")
	}
}

func TestReadLineRaw(t *testing.T) {
	cases := []struct {
		input string
		want  string
	}{
		{"admin\n", "admin"},
		{"\n", ""},
		{"crlf\r\n", "crlf"},
		{"no newline", "no newline"},
	}
	for _, testCase := range cases {
		got, err := ReadLineRaw(strings.NewReader(testCase.input))
		if err != nil {
			t.Fatalf("ReadLineRaw(%q): %v", testCase.input, err)
		}
		if got != testCase.want {
			t.Errorf("ReadLineRaw(%q) = %q, want %q", testCase.input, got, testCase.want)
		}
	}
}

func TestReadLinePrompts(t *testing.T) {
	var out strings.Builder
	line, err := ReadLine(strings.NewReader("yes\n"), &out, "确认继续请输入 yes: ")
	if err != nil {
		t.Fatalf("ReadLine: %v", err)
	}
	if line != "yes" {
		t.Errorf("line = %q", line)
	}
	if out.String() != "确认继续请输入 yes: " {
		t.Errorf("prompt = %q", out.String())
	}
}

func TestIsTerminalOnAPipe(t *testing.T) {
	read, write, err := os.Pipe()
	if err != nil {
		t.Fatalf("os.Pipe: %v", err)
	}
	defer read.Close()
	defer write.Close()
	if IsTerminal(read) {
		t.Error("a pipe is not a terminal")
	}
}
