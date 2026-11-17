//go:build windows

package termx

import (
	"context"
	"os"
	"time"

	"golang.org/x/sys/windows"
	"golang.org/x/term"
)

// resizePollInterval is the Windows substitute for SIGWINCH: the console has
// no signal for window changes, so the size is sampled.
const resizePollInterval = 500 * time.Millisecond

// utf8CodePage is 65001; the agent emits UTF-8 and the console defaults to the
// OEM code page.
const utf8CodePage = 65001

// Open returns the terminal attached to in/out.
func Open(in, out *os.File) (Raw, error) {
	return &windowsRaw{in: in, out: out}, nil
}

type windowsRaw struct {
	in  *os.File
	out *os.File
}

// Enter enables the console features the agent's output assumes (VT escape
// sequences and an UTF-8 code page), then puts stdin into raw mode. Everything
// is remembered so the console is handed back exactly as it was found —
// including for a short-lived cmd.exe that shares the window.
func (t *windowsRaw) Enter() (func() error, error) {
	var savedIn, savedOut uint32
	inHandle := windows.Handle(t.in.Fd())
	outHandle := windows.Handle(t.out.Fd())
	haveIn := windows.GetConsoleMode(inHandle, &savedIn) == nil
	haveOut := windows.GetConsoleMode(outHandle, &savedOut) == nil
	if haveOut {
		_ = windows.SetConsoleMode(outHandle, savedOut|windows.ENABLE_VIRTUAL_TERMINAL_PROCESSING)
	}
	if haveIn {
		_ = windows.SetConsoleMode(inHandle, savedIn|windows.ENABLE_VIRTUAL_TERMINAL_INPUT)
	}
	savedInCP, _ := windows.GetConsoleCP()
	savedOutCP, _ := windows.GetConsoleOutputCP()
	_ = windows.SetConsoleCP(utf8CodePage)
	_ = windows.SetConsoleOutputCP(utf8CodePage)

	state, err := term.MakeRaw(int(t.in.Fd()))
	if err != nil {
		if haveOut {
			_ = windows.SetConsoleMode(outHandle, savedOut)
		}
		if haveIn {
			_ = windows.SetConsoleMode(inHandle, savedIn)
		}
		return nil, err
	}
	return func() error {
		restoreErr := term.Restore(int(t.in.Fd()), state)
		if haveIn {
			_ = windows.SetConsoleMode(inHandle, savedIn)
		}
		if haveOut {
			_ = windows.SetConsoleMode(outHandle, savedOut)
		}
		_ = windows.SetConsoleCP(savedInCP)
		_ = windows.SetConsoleOutputCP(savedOutCP)
		return restoreErr
	}, nil
}

func (t *windowsRaw) Size() (Size, error) {
	cols, rows, err := term.GetSize(int(t.out.Fd()))
	if err != nil {
		return Size{}, err
	}
	return Size{Cols: cols, Rows: rows}, nil
}

func (t *windowsRaw) Poll(ctx context.Context, fn func(Size)) error {
	ticker := time.NewTicker(resizePollInterval)
	defer ticker.Stop()
	last, _ := t.Size()
	for {
		select {
		case <-ctx.Done():
			return nil
		case <-ticker.C:
			current, err := t.Size()
			if err != nil || current == last {
				continue
			}
			last = current
			fn(current)
		}
	}
}
