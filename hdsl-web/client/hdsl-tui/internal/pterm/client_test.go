package pterm_test

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/gorilla/websocket"

	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/pterm"
)

// syncBuffer is an io.Writer the test goroutine and the pump goroutine share.
type syncBuffer struct {
	mu       sync.Mutex
	contents strings.Builder
}

func (b *syncBuffer) Write(data []byte) (int, error) {
	b.mu.Lock()
	defer b.mu.Unlock()
	return b.contents.Write(data)
}

func (b *syncBuffer) String() string {
	b.mu.Lock()
	defer b.mu.Unlock()
	return b.contents.String()
}

func waitFor(t *testing.T, what string, condition func() bool) {
	t.Helper()
	deadline := time.Now().Add(5 * time.Second)
	for time.Now().Before(deadline) {
		if condition() {
			return
		}
		time.Sleep(5 * time.Millisecond)
	}
	t.Fatalf("timed out waiting for %s", what)
}

func wsURL(server *httptest.Server) string {
	return "ws" + strings.TrimPrefix(server.URL, "http")
}

func TestSessionPumpsBytesResizeAndExit(t *testing.T) {
	var mu sync.Mutex
	var cookie string
	sizes := make(chan pterm.Control, 1)
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		mu.Lock()
		cookie = r.Header.Get("Cookie")
		mu.Unlock()
		socket, err := (&websocket.Upgrader{}).Upgrade(w, r, nil)
		if err != nil {
			return
		}
		defer socket.Close()
		writeControl(t, socket, map[string]any{
			"type": "ready", "protocol": pterm.Protocol, "instance": "dsh-1", "brand": "dsh",
		})
		for {
			kind, data, err := socket.ReadMessage()
			if err != nil {
				return
			}
			switch kind {
			case websocket.BinaryMessage:
				_ = socket.WriteMessage(websocket.BinaryMessage, append([]byte("echo:"), data...))
			case websocket.TextMessage:
				control, err := pterm.DecodeControl(data)
				if err != nil || control.Type != pterm.TypeResize {
					continue
				}
				sizes <- control
				_ = socket.WriteMessage(websocket.BinaryMessage,
					[]byte(fmt.Sprintf("size:%dx%d", control.Cols, control.Rows)))
				writeControl(t, socket, map[string]any{"type": "exit", "code": 3})
			}
		}
	}))
	defer server.Close()

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	conn, err := pterm.Dial(ctx, wsURL(server), "hdsl_session=tok", nil)
	if err != nil {
		t.Fatalf("Dial: %v", err)
	}
	defer conn.Close()

	output := &syncBuffer{}
	stdin, stdinWriter := io.Pipe()
	defer stdinWriter.Close()
	ready := make(chan pterm.Control, 1)
	results := make(chan pterm.Result, 1)
	failures := make(chan error, 1)
	go func() {
		result, err := conn.Run(ctx, pterm.Options{
			In: stdin, Out: output,
			OnReady: func(control pterm.Control) { ready <- control },
		})
		if err != nil {
			failures <- err
			return
		}
		results <- result
	}()

	select {
	case control := <-ready:
		if control.Protocol != pterm.Protocol || control.Instance != "dsh-1" || control.Brand != "dsh" {
			t.Errorf("ready = %+v", control)
		}
	case <-time.After(5 * time.Second):
		t.Fatal("the client never reported a ready frame")
	}

	if _, err := stdinWriter.Write([]byte("hello")); err != nil {
		t.Fatalf("write stdin: %v", err)
	}
	waitFor(t, "the echoed bytes", func() bool { return strings.Contains(output.String(), "echo:hello") })

	if err := conn.SendResize(120, 40); err != nil {
		t.Fatalf("SendResize: %v", err)
	}
	waitFor(t, "the size echo", func() bool { return strings.Contains(output.String(), "size:120x40") })

	select {
	case received := <-sizes:
		if received.Cols != 120 || received.Rows != 40 || received.Type != pterm.TypeResize {
			t.Errorf("server saw %+v", received)
		}
	case <-time.After(5 * time.Second):
		t.Fatal("the server never received a resize frame")
	}

	select {
	case result := <-results:
		if !result.ExitKnown || result.ExitCode != 3 {
			t.Errorf("result = %+v, want exit 3", result)
		}
	case err := <-failures:
		t.Fatalf("Run: %v", err)
	case <-time.After(5 * time.Second):
		t.Fatal("the exit frame did not end the session")
	}

	mu.Lock()
	defer mu.Unlock()
	if cookie != "hdsl_session=tok" {
		t.Errorf("handshake cookie = %q, the ticket needs the session cookie", cookie)
	}
}

func TestFilterCommandEndsTheSession(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		socket, err := (&websocket.Upgrader{}).Upgrade(w, r, nil)
		if err != nil {
			return
		}
		defer socket.Close()
		for {
			if _, _, err := socket.ReadMessage(); err != nil {
				return
			}
		}
	}))
	defer server.Close()

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	conn, err := pterm.Dial(ctx, wsURL(server), "", nil)
	if err != nil {
		t.Fatalf("Dial: %v", err)
	}
	defer conn.Close()

	stdin, stdinWriter := io.Pipe()
	defer stdinWriter.Close()
	filtered := make(chan []byte, 1)
	go func() {
		_, err := conn.Run(ctx, pterm.Options{
			In: stdin, Out: io.Discard,
			Filter: func(chunk []byte) ([]byte, pterm.Command) {
				filtered <- append([]byte(nil), chunk...)
				return nil, pterm.CommandDetach
			},
		})
		if err != nil {
			t.Errorf("Run: %v", err)
		}
	}()
	if _, err := stdinWriter.Write([]byte{0x1d, 'q'}); err != nil {
		t.Fatalf("write stdin: %v", err)
	}
	select {
	case chunk := <-filtered:
		if string(chunk) != "\x1dq" {
			t.Errorf("filter saw %q", chunk)
		}
	case <-time.After(5 * time.Second):
		t.Fatal("the filter was never called")
	}
}

func TestDetachCommandIsReported(t *testing.T) {
	server := echoServer(t)
	defer server.Close()

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	conn, err := pterm.Dial(ctx, wsURL(server), "", nil)
	if err != nil {
		t.Fatalf("Dial: %v", err)
	}
	defer conn.Close()

	result, err := conn.Run(ctx, pterm.Options{
		In:  strings.NewReader("anything"),
		Out: io.Discard,
		Filter: func([]byte) ([]byte, pterm.Command) {
			return nil, pterm.CommandKill
		},
	})
	if err != nil {
		t.Fatalf("Run: %v", err)
	}
	if result.Command != pterm.CommandKill {
		t.Errorf("command = %v, want CommandKill", result.Command)
	}
}

func TestCloseCodeIsSurfacedWithoutError(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		socket, err := (&websocket.Upgrader{}).Upgrade(w, r, nil)
		if err != nil {
			return
		}
		defer socket.Close()
		writeControl(t, socket, map[string]any{"type": "ready", "protocol": 1})
		_ = socket.WriteControl(websocket.CloseMessage,
			websocket.FormatCloseMessage(1008, "unknown or expired ticket"), time.Now().Add(time.Second))
	}))
	defer server.Close()

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	conn, err := pterm.Dial(ctx, wsURL(server), "hdsl_session=tok", nil)
	if err != nil {
		t.Fatalf("Dial: %v", err)
	}
	defer conn.Close()

	result, err := conn.Run(ctx, pterm.Options{In: strings.NewReader(""), Out: io.Discard})
	if err != nil {
		t.Fatalf("a close frame is not a transport error: %v", err)
	}
	if result.CloseCode != 1008 {
		t.Errorf("close code = %d, want 1008", result.CloseCode)
	}
	if result.ExitKnown {
		t.Error("a close frame must not look like an agent exit")
	}
}

func TestDialRefusedUpgrade(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		http.Error(w, "no session", http.StatusForbidden)
	}))
	defer server.Close()

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	_, err := pterm.Dial(ctx, wsURL(server), "", nil)
	if err == nil {
		t.Fatal("expected an error")
	}
	var closeErr *pterm.CloseError
	if !asCloseError(err, &closeErr) {
		t.Fatalf("error %v is not a CloseError", err)
	}
	if closeErr.Code != http.StatusForbidden {
		t.Errorf("code = %d, want 403", closeErr.Code)
	}
}

func asCloseError(err error, target **pterm.CloseError) bool {
	closeErr, ok := err.(*pterm.CloseError)
	if ok {
		*target = closeErr
	}
	return ok
}

func TestDecodeControl(t *testing.T) {
	ready, err := pterm.DecodeControl([]byte(`{"type":"ready","protocol":1,"instance":"dsh-1","brand":"dsh","future":true}`))
	if err != nil {
		t.Fatalf("DecodeControl: %v", err)
	}
	if ready.Type != pterm.TypeReady || ready.Protocol != 1 || ready.Instance != "dsh-1" || ready.Brand != "dsh" {
		t.Errorf("ready = %+v", ready)
	}

	exited, err := pterm.DecodeControl([]byte(`{"type":"exit","code":130}`))
	if err != nil {
		t.Fatalf("DecodeControl: %v", err)
	}
	if !exited.HasCode || exited.Code != 130 {
		t.Errorf("exit = %+v", exited)
	}

	withoutCode, err := pterm.DecodeControl([]byte(`{"type":"exit"}`))
	if err != nil {
		t.Fatalf("DecodeControl: %v", err)
	}
	if withoutCode.HasCode || withoutCode.Code != 0 {
		t.Errorf("exit without code = %+v", withoutCode)
	}

	unknown, err := pterm.DecodeControl([]byte(`{"type":"something-new","x":1}`))
	if err != nil {
		t.Fatalf("DecodeControl must tolerate unknown types: %v", err)
	}
	if unknown.Type != "something-new" {
		t.Errorf("unknown frame = %+v", unknown)
	}

	if _, err := pterm.DecodeControl([]byte("not json")); err == nil {
		t.Error("expected an error for a non-JSON frame")
	}
}

func TestEncodeResize(t *testing.T) {
	frame, err := pterm.EncodeResize(120, 40)
	if err != nil {
		t.Fatalf("EncodeResize: %v", err)
	}
	var decoded struct {
		Type string `json:"type"`
		Cols int    `json:"cols"`
		Rows int    `json:"rows"`
	}
	if err := json.Unmarshal(frame, &decoded); err != nil {
		t.Fatalf("EncodeResize produced invalid JSON: %v", err)
	}
	if decoded.Type != "resize" || decoded.Cols != 120 || decoded.Rows != 40 {
		t.Errorf("resize frame = %+v", decoded)
	}
	if _, err := pterm.EncodeResize(0, 40); err == nil {
		t.Error("a zero-column resize must be refused")
	}
	if _, err := pterm.EncodeResize(120, -1); err == nil {
		t.Error("a negative-row resize must be refused")
	}
}

func echoServer(t *testing.T) *httptest.Server {
	t.Helper()
	return httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		socket, err := (&websocket.Upgrader{}).Upgrade(w, r, nil)
		if err != nil {
			return
		}
		defer socket.Close()
		for {
			if _, _, err := socket.ReadMessage(); err != nil {
				return
			}
		}
	}))
}

func writeControl(t *testing.T, socket *websocket.Conn, control map[string]any) {
	t.Helper()
	encoded, err := json.Marshal(control)
	if err != nil {
		t.Errorf("marshal control frame: %v", err)
		return
	}
	if err := socket.WriteMessage(websocket.TextMessage, encoded); err != nil {
		t.Errorf("write control frame: %v", err)
	}
}
