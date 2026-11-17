//go:build unix

package termx

import (
	"context"
	"os"
	"os/signal"
	"syscall"

	"golang.org/x/term"
)

// Open returns the terminal attached to in/out. Nothing is changed until Enter.
func Open(in, out *os.File) (Raw, error) {
	return &unixRaw{in: in, out: out}, nil
}

type unixRaw struct {
	in  *os.File
	out *os.File
}

func (t *unixRaw) Enter() (func() error, error) {
	state, err := term.MakeRaw(int(t.in.Fd()))
	if err != nil {
		return nil, err
	}
	return func() error { return term.Restore(int(t.in.Fd()), state) }, nil
}

func (t *unixRaw) Size() (Size, error) {
	cols, rows, err := term.GetSize(int(t.out.Fd()))
	if err != nil {
		return Size{}, err
	}
	return Size{Cols: cols, Rows: rows}, nil
}

// Poll reports window changes. SIGWINCH is delivered to the process, and the
// size has to be re-read on every delivery — the signal carries no dimensions.
func (t *unixRaw) Poll(ctx context.Context, fn func(Size)) error {
	changes := make(chan os.Signal, 4)
	signal.Notify(changes, syscall.SIGWINCH)
	defer signal.Stop(changes)
	last, _ := t.Size()
	for {
		select {
		case <-ctx.Done():
			return nil
		case <-changes:
			current, err := t.Size()
			if err != nil || current == last {
				continue
			}
			last = current
			fn(current)
		}
	}
}
