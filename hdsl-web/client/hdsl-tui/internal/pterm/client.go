package pterm

import (
	"context"
	"crypto/tls"
	"errors"
	"fmt"
	"io"
	"net"
	"net/http"
	"sync"
	"time"

	"github.com/gorilla/websocket"
)

const (
	// handshakeTimeout is how long the upgrade itself may take.
	handshakeTimeout = 15 * time.Second
	// readLimit bounds one message. A PTY can emit a lot at once (`cat` of a
	// large file, a build log); the default 32 KiB would kill the connection.
	readLimit = 8 << 20
	// stdinChunk is how much of stdin is forwarded per binary frame.
	stdinChunk = 32 << 10
)

// Heartbeat. A terminal is silent whenever the agent is thinking or waiting for
// input, so silence on its own says nothing about the link; a ping and a pong
// deadline are what tell "quiet" apart from "half dead" (a NAT that forgot the
// flow, a panel that went away without closing the socket). The ping is not
// there to keep anything open — the panel's socket has no idle timeout — it is
// there to make the connection prove it still carries bytes.
type heartbeatConfig struct {
	// ping is how often an otherwise silent link is pinged.
	ping time.Duration
	// pong is how long the link may stay silent after a ping was sent. Every
	// ping re-arms the deadline, so it has to stay well below ping: a deadline
	// wider than the interval would be pushed back by the next ping and never
	// expire at all.
	pong time.Duration
}

// heartbeat is the timing a new session gets. Dial copies it into the
// connection, so a session keeps the timing it was opened with and nothing
// reads this again — which is also what lets a test run the same code with a
// much shorter heartbeat.
var heartbeat = heartbeatConfig{ping: 20 * time.Second, pong: 10 * time.Second}

// Conn is one attached terminal WebSocket.
type Conn struct {
	ws     *websocket.Conn
	writeM sync.Mutex
	// beat is this session's heartbeat, copied from heartbeat at Dial.
	beat heartbeatConfig
}

// Dial attaches to the panel. cookie is the `hdsl_session` cookie value; the
// WebSocket handshake has to carry it, because the ticket alone only proves
// that *someone* opened a terminal.
func Dial(ctx context.Context, url, cookie string, tlsConfig *tls.Config) (*Conn, error) {
	dialer := &websocket.Dialer{
		HandshakeTimeout: handshakeTimeout,
		TLSClientConfig:  tlsConfig,
	}
	header := http.Header{}
	if cookie != "" {
		header.Set("Cookie", cookie)
	}
	conn, response, err := dialer.DialContext(ctx, url, header)
	if err != nil {
		if response != nil {
			return nil, &CloseError{Code: response.StatusCode, Reason: response.Status}
		}
		return nil, fmt.Errorf("无法连接终端 WebSocket: %w", err)
	}
	conn.SetReadLimit(readLimit)
	beat := heartbeat
	// A pong is the answer to one of our pings, so it proves the link is alive
	// and buys the time until the next ping plus that ping's own grace. See
	// ping for the other half of the arithmetic.
	conn.SetPongHandler(func(string) error {
		return conn.SetReadDeadline(aliveFor(beat))
	})
	return &Conn{ws: conn, beat: beat}, nil
}

// ping asks the peer to prove the link is alive, and arms the read deadline the
// pong has to clear. WriteControl is safe to call while another goroutine writes
// a message — gorilla serialises frames itself — so a ping goes out between
// frames and never splits one.
//
// The deadline is only pong wide: that is the whole question a ping asks, and a
// link that answers nothing within it is the half-dead one this exists to catch.
// Anything that proves the link is alive — the pong, or any frame at all — arms
// aliveFor instead, which is wider on purpose: a healthy idle session has
// nothing to read between two pings, and a deadline that only covered pong
// would kill it in the gap.
func (c *Conn) ping() error {
	deadline := time.Now().Add(c.beat.pong)
	if err := c.ws.SetReadDeadline(deadline); err != nil {
		return err
	}
	return c.ws.WriteControl(websocket.PingMessage, nil, deadline)
}

// aliveFor is the read deadline that follows proof of life: the next ping is at
// most one interval away, and a ping is allowed its pong time of its own before
// the silence means anything.
func aliveFor(beat heartbeatConfig) time.Time {
	return time.Now().Add(beat.ping + beat.pong)
}

// CloseError reports a close frame (or a refused upgrade) with its code.
type CloseError struct {
	Code   int
	Reason string
}

func (e *CloseError) Error() string {
	return fmt.Sprintf("连接已关闭 (%d %s)", e.Code, e.Reason)
}

// SendBinary writes raw terminal input.
func (c *Conn) SendBinary(data []byte) error {
	c.writeM.Lock()
	defer c.writeM.Unlock()
	return c.ws.WriteMessage(websocket.BinaryMessage, data)
}

// SendText writes a control frame.
func (c *Conn) SendText(data []byte) error {
	c.writeM.Lock()
	defer c.writeM.Unlock()
	return c.ws.WriteMessage(websocket.TextMessage, data)
}

// SendResize tells the agent how big the local terminal is.
func (c *Conn) SendResize(cols, rows int) error {
	frame, err := EncodeResize(cols, rows)
	if err != nil {
		return err
	}
	return c.SendText(frame)
}

// Close closes the socket. A close only detaches; the agent keeps running.
func (c *Conn) Close() error {
	return c.ws.Close()
}

// Command is what a local key sequence asked for, as opposed to what the agent
// did.
type Command int

const (
	// CommandNone means the session ended on its own.
	CommandNone Command = iota
	// CommandDetach leaves the agent running and quits the client.
	CommandDetach
	// CommandKill stops the terminal and quits the client.
	CommandKill
)

// Filter inspects stdin on the way to the agent. It returns the bytes to
// forward (nil forwards nothing) and, when the chunk contains a local command,
// that command — the rest of the chunk is then dropped.
//
// The detach state machine behind it lives in internal/cli; keeping it a hook
// here is what lets the pump be exercised without a real terminal.
type Filter func(chunk []byte) (forward []byte, command Command)

// Options configures a session run.
type Options struct {
	In      io.Reader
	Out     io.Writer
	Logf    func(format string, args ...any)
	OnReady func(Control)
	Filter  Filter
}

func (o Options) logf(format string, args ...any) {
	if o.Logf != nil {
		o.Logf(format, args...)
	}
}

// Result is how a session ended.
type Result struct {
	// ExitKnown is true when the agent sent an `exit` frame; ExitCode is then
	// the agent's own exit status.
	ExitKnown bool
	ExitCode  int
	// Command is set when the local user detached or killed the terminal.
	Command Command
	// CloseCode and CloseReason describe the close frame, if any.
	CloseCode   int
	CloseReason string
}

// Run pumps bytes until the agent exits, the socket closes, the user detaches,
// ctx is cancelled, or the heartbeat finds the link dead. The caller owns
// closing the socket and restoring the terminal.
func (c *Conn) Run(ctx context.Context, opts Options) (Result, error) {
	if opts.In == nil {
		opts.In = emptyReader{}
	}
	if opts.Out == nil {
		opts.Out = io.Discard
	}
	done := make(chan struct{})
	defer close(done)
	outcomes := make(chan Result, 1)
	failures := make(chan error, 1)
	publish := func(result Result) {
		select {
		case outcomes <- result:
		case <-done:
		}
	}
	fail := func(err error) {
		select {
		case failures <- err:
		case <-done:
		}
	}

	go func() {
		if err := c.readLoop(opts, publish); err != nil {
			fail(err)
		}
	}()

	// The heartbeat lives exactly as long as the session: done is closed when
	// Run returns, so no ping can outlive the socket it belongs to.
	go func() {
		ticker := time.NewTicker(c.beat.ping)
		defer ticker.Stop()
		for {
			select {
			case <-done:
				return
			case <-ticker.C:
				if err := c.ping(); err != nil {
					// A ping that cannot be written means the transport is gone,
					// and that is the same failure the read loop reports.
					fail(fmt.Errorf("心跳 ping 未能发出: %w", err))
					return
				}
			}
		}
	}()

	// A closed stdin (EOF) stops input but must not end the session: the agent
	// may still have output, and the socket is the only way to see it.
	go func() {
		buffer := make([]byte, stdinChunk)
		for {
			n, err := opts.In.Read(buffer)
			if n > 0 {
				forward, command := buffer[:n], CommandNone
				if opts.Filter != nil {
					forward, command = opts.Filter(buffer[:n])
				}
				if len(forward) > 0 {
					if sendErr := c.SendBinary(forward); sendErr != nil {
						fail(sendErr)
						return
					}
				}
				if command != CommandNone {
					publish(Result{Command: command})
					return
				}
			}
			if err != nil {
				return
			}
		}
	}()

	select {
	case result := <-outcomes:
		return result, nil
	case err := <-failures:
		return Result{}, err
	case <-ctx.Done():
		return Result{}, ctx.Err()
	}
}

// readLoop consumes frames until something ends the session. It returns the
// read error (nil when the session ended through a frame or a close) and
// publishes a terminal result.
func (c *Conn) readLoop(opts Options, publish func(Result)) error {
	for {
		// Whatever arrived last time round proved the link is alive, so the read
		// starts with the wider deadline — and it starts *before* the next
		// frame is written to the local terminal, so minutes spent blocked
		// there cannot trip a pong deadline that has nothing to do with it. The
		// heartbeat narrows the deadline again when it asks its question.
		_ = c.ws.SetReadDeadline(aliveFor(c.beat))
		kind, data, err := c.ws.ReadMessage()
		if err != nil {
			if errors.Is(err, io.EOF) {
				return nil
			}
			var closeErr *websocket.CloseError
			if errors.As(err, &closeErr) {
				publish(Result{CloseCode: closeErr.Code, CloseReason: closeErr.Text})
				return nil
			}
			// The only read deadline this client arms is the heartbeat's, so a
			// timeout means the link stopped answering — worth saying, instead
			// of handing the user a bare "i/o timeout".
			var timeout net.Error
			if errors.As(err, &timeout) && timeout.Timeout() {
				return fmt.Errorf("与面板失去心跳（%s 内没有收到 pong）: %w", c.beat.pong, err)
			}
			return err
		}
		switch kind {
		case websocket.BinaryMessage:
			if _, err := opts.Out.Write(data); err != nil {
				return err
			}
		case websocket.TextMessage:
			control, err := DecodeControl(data)
			if err != nil {
				opts.logf("忽略无法解析的控制帧: %v", err)
				continue
			}
			switch control.Type {
			case TypeReady:
				if opts.OnReady != nil {
					opts.OnReady(control)
				}
			case TypeExit:
				publish(Result{ExitKnown: true, ExitCode: control.Code})
				return nil
			default:
				opts.logf("忽略未知控制帧 %q", control.Type)
			}
		}
	}
}

type emptyReader struct{}

func (emptyReader) Read([]byte) (int, error) { return 0, io.EOF }
