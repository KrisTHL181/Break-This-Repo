// Package termx owns the local terminal: raw mode, window size, resize
// notification and (on Windows) the console modes the agent's output needs.
package termx

import (
	"context"
	"fmt"
	"io"
	"os"

	"golang.org/x/term"
)

// Size is a terminal window size in characters.
type Size struct {
	Cols int
	Rows int
}

// Raw is a terminal in raw mode. It is an interface so tests — which never
// have a tty — can substitute Nop.
type Raw interface {
	// Enter switches to raw mode and returns the restore function. The restore
	// function must run on every exit path, signals included.
	Enter() (func() error, error)
	// Size reports the current window size.
	Size() (Size, error)
	// Poll calls fn for every size change until ctx is done. Unix listens for
	// SIGWINCH; Windows has no such signal and polls instead.
	Poll(ctx context.Context, fn func(Size)) error
}

// Nop is the no-terminal stand-in: raw mode is a no-op, the size is the
// classic 80x24, and Poll only waits for cancellation.
type Nop struct{}

func (Nop) Enter() (func() error, error) { return func() error { return nil }, nil }

func (Nop) Size() (Size, error) { return Size{Cols: 80, Rows: 24}, nil }

func (Nop) Poll(ctx context.Context, fn func(Size)) error {
	<-ctx.Done()
	return nil
}

// IsTerminal reports whether f is a character device.
func IsTerminal(f *os.File) bool { return term.IsTerminal(int(f.Fd())) }

// ReadPassword prompts on w and reads a line with echo disabled. The bytes
// never leave this function: nothing is stored, logged or put in argv.
func ReadPassword(w io.Writer, prompt string) (string, error) {
	fmt.Fprint(w, prompt)
	secret, err := term.ReadPassword(int(os.Stdin.Fd()))
	fmt.Fprintln(w)
	if err != nil {
		return "", fmt.Errorf("无法关闭回显读取密码（需要一个真正的终端）: %w", err)
	}
	return string(secret), nil
}

// ReadLine prompts on w and reads one line, echo left alone.
func ReadLine(r io.Reader, w io.Writer, prompt string) (string, error) {
	fmt.Fprint(w, prompt)
	return ReadLineRaw(r)
}

// ReadLineRaw reads one line without printing a prompt.
func ReadLineRaw(r io.Reader) (string, error) {
	var line []byte
	single := make([]byte, 1)
	for {
		n, err := r.Read(single)
		if n > 0 {
			switch single[0] {
			case '\n':
				return string(line), nil
			case '\r':
			default:
				line = append(line, single[0])
			}
		}
		if err != nil {
			if len(line) > 0 && err == io.EOF {
				return string(line), nil
			}
			return "", err
		}
	}
}
