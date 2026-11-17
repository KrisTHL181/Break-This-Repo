package ui

import (
	"context"
	"encoding/base64"
	"fmt"
	"io"
	"strings"
	"sync"
	"time"

	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/termx"
)

// escapeTimeout is how long Decode waits for the rest of an escape sequence
// before deciding the user merely pressed Esc.
const escapeTimeout = 25 * time.Millisecond

// Screen owns the alternate screen, the raw terminal and the event stream.
//
// There is exactly one reader of the terminal, for the whole lifetime of the
// screen. When an agent is attached the same reader forwards the raw bytes to
// it (AttachInput) instead of decoding them: two readers would race for the
// first keystroke after suspending, and that keystroke is usually the one that
// detaches.
type Screen struct {
	raw termx.Raw
	in  io.Reader
	out io.Writer

	mu     sync.Mutex
	width  int
	height int
	active bool
	closed bool

	// agent is the write side of the pipe to the attached terminal, nil when
	// the UI is reading keys itself.
	agent io.Writer

	restore func() error

	events     chan Event
	readerStop chan struct{}
	resizeStop context.CancelFunc
}

// Open enters the alternate screen and raw mode. The caller must Close it on
// every exit path.
func Open(in io.Reader, out io.Writer, raw termx.Raw) (*Screen, error) {
	screen := &Screen{
		raw:        raw,
		in:         in,
		out:        out,
		events:     make(chan Event, 32),
		readerStop: make(chan struct{}),
	}
	if err := screen.enter(); err != nil {
		return nil, err
	}
	screen.startReader()
	return screen, nil
}

func (s *Screen) enter() error {
	restore, err := s.raw.Enter()
	if err != nil {
		return fmt.Errorf("无法进入终端原始模式: %w", err)
	}
	s.restore = restore
	if _, err := io.WriteString(s.out, "\x1b[?1049h\x1b[?25l\x1b[2J"); err != nil {
		_ = restore()
		return err
	}
	s.measure()
	s.mu.Lock()
	s.active = true
	s.mu.Unlock()
	s.startResize()
	return nil
}

func (s *Screen) measure() {
	if size, err := s.raw.Size(); err == nil && size.Cols > 0 && size.Rows > 0 {
		s.mu.Lock()
		s.width, s.height = size.Cols, size.Rows
		s.mu.Unlock()
		return
	}
	s.mu.Lock()
	if s.width == 0 {
		s.width, s.height = 80, 24
	}
	s.mu.Unlock()
}

// Size is the current terminal size in cells.
func (s *Screen) Size() (width, height int) {
	s.mu.Lock()
	defer s.mu.Unlock()
	return s.width, s.height
}

// Events is the stream of key presses and resize notices.
func (s *Screen) Events() <-chan Event { return s.events }

// Draw repaints the whole screen. lines should be exactly height long; extra
// lines are ignored, missing ones are left as they are.
func (s *Screen) Draw(lines []string) {
	s.mu.Lock()
	if !s.active || s.closed {
		s.mu.Unlock()
		return
	}
	s.mu.Unlock()

	var builder strings.Builder
	builder.WriteString("\x1b[H")
	for index, line := range lines {
		builder.WriteString(line)
		// Erase the remainder of the row on the screen background, so a
		// resize narrower than the old content does not leave debris.
		builder.WriteString(ScreenBG)
		builder.WriteString("\x1b[K")
		builder.WriteString(Reset)
		if index < len(lines)-1 {
			builder.WriteString("\r\n")
		}
	}
	builder.WriteString(Reset)
	_, _ = io.WriteString(s.out, builder.String())
}

// Suspend hands the terminal back: raw mode off, alternate screen left. The
// reader keeps running and drops bytes until AttachInput is set, so no
// keystroke is consumed twice.
func (s *Screen) Suspend() {
	s.stopResize()
	s.mu.Lock()
	if !s.active {
		s.mu.Unlock()
		return
	}
	s.active = false
	restore := s.restore
	s.mu.Unlock()
	_, _ = io.WriteString(s.out, "\x1b[?25h\x1b[0m\x1b[?1049l")
	if restore != nil {
		_ = restore()
	}
}

// Resume re-enters the alternate screen after Suspend.
func (s *Screen) Resume() error { return s.enter() }

// CopyToClipboard asks the terminal to place text on the system clipboard
// using OSC 52. Terminals that do not implement it ignore the sequence; the
// screen is not redrawn and nothing else changes. Base64 keeps newlines and
// non-ASCII intact.
func (s *Screen) CopyToClipboard(text string) {
	s.mu.Lock()
	defer s.mu.Unlock()
	if s.closed || s.out == nil || text == "" {
		return
	}
	_, _ = fmt.Fprintf(s.out, "\x1b]52;c;%s\a", base64.StdEncoding.EncodeToString([]byte(text)))
}

// AttachInput routes the terminal's bytes to w instead of decoding them. The
// caller closes the other side of the pipe when the terminal ends.
func (s *Screen) AttachInput(w io.Writer) {
	s.mu.Lock()
	s.agent = w
	s.mu.Unlock()
}

// DetachInput stops forwarding bytes to the attached terminal.
func (s *Screen) DetachInput() {
	s.mu.Lock()
	s.agent = nil
	s.mu.Unlock()
}

// Close restores the terminal for good.
func (s *Screen) Close() {
	s.stopResize()
	s.mu.Lock()
	if s.closed {
		s.mu.Unlock()
		return
	}
	s.closed = true
	s.agent = nil
	active := s.active
	s.active = false
	restore := s.restore
	s.mu.Unlock()
	close(s.readerStop)
	if active {
		_, _ = io.WriteString(s.out, "\x1b[?25h\x1b[0m\x1b[?1049l")
	}
	if restore != nil {
		_ = restore()
	}
}

func (s *Screen) startResize() {
	ctx, cancel := context.WithCancel(context.Background())
	s.mu.Lock()
	s.resizeStop = cancel
	s.mu.Unlock()
	go func() {
		_ = s.raw.Poll(ctx, func(size termx.Size) {
			if size.Cols <= 0 || size.Rows <= 0 {
				return
			}
			s.mu.Lock()
			s.width, s.height = size.Cols, size.Rows
			active := s.active
			s.mu.Unlock()
			if !active {
				return
			}
			select {
			case s.events <- Event{Key: KeyResize}:
			default:
			}
		})
	}()
}

func (s *Screen) stopResize() {
	s.mu.Lock()
	cancel := s.resizeStop
	s.resizeStop = nil
	s.mu.Unlock()
	if cancel != nil {
		cancel()
	}
}

// startReader starts the only reader of the terminal and the loop that turns
// its bytes into events. Both live for the whole screen: a reader started and
// stopped per screen would race with the agent pump for the first keystroke.
func (s *Screen) startReader() {
	chunks := make(chan []byte, 8)
	go func() {
		buffer := make([]byte, 512)
		for {
			count, err := s.in.Read(buffer)
			if count > 0 {
				chunk := make([]byte, count)
				copy(chunk, buffer[:count])
				select {
				case chunks <- chunk:
				case <-s.readerStop:
					return
				}
			}
			if err != nil {
				close(chunks)
				return
			}
		}
	}()
	go s.decodeLoop(chunks)
}

// decodeLoop routes one chunk at a time: to the attached agent, or through the
// key decoder. A chunk that ends in the middle of an escape sequence is held
// back for escapeTimeout, which is what makes a lone ESC (back) tell itself
// apart from the start of an arrow key.
func (s *Screen) decodeLoop(chunks <-chan []byte) {
	var pending []byte
	for {
		var timer *time.Timer
		var tick <-chan time.Time
		if len(pending) > 0 {
			timer = time.NewTimer(escapeTimeout)
			tick = timer.C
		}
		select {
		case <-s.readerStop:
			if timer != nil {
				timer.Stop()
			}
			return
		case chunk, ok := <-chunks:
			if !ok {
				return
			}
			if timer != nil {
				timer.Stop()
			}
			s.mu.Lock()
			agent := s.agent
			active := s.active
			s.mu.Unlock()
			if agent != nil {
				pending = nil
				// A closed pipe means the terminal just ended; the loop must
				// stay alive for the menu that comes next.
				_, _ = agent.Write(chunk)
				continue
			}
			if !active {
				pending = nil
				continue
			}
			pending = append(pending, chunk...)
			events, rest := Decode(pending)
			pending = rest
			if !s.emitAll(events) {
				return
			}
		case <-tick:
			events := DecodeForce(pending)
			pending = nil
			if !s.emitAll(events) {
				return
			}
		}
	}
}

func (s *Screen) emitAll(events []Event) bool {
	for _, event := range events {
		select {
		case s.events <- event:
		case <-s.readerStop:
			return false
		}
	}
	return true
}
