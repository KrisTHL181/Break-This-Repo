package panel

import (
	"context"
	"crypto/tls"
	"encoding/json"
	"fmt"
	"net/http"
	"sync"
	"time"

	"github.com/gorilla/websocket"
)

// Event is one frame from the panel's `/ws` gateway. The gateway publishes
// flat JSON objects with a `type`; chat events add sessionId/turnId and the
// fields below.
type Event struct {
	Type      string `json:"type"`
	SessionID string `json:"sessionId"`
	TurnID    string `json:"turnId"`
	Seq       int    `json:"seq"`

	// chat-delta / chat-reasoning
	Text string `json:"text"`

	// chat-block
	Phase     string `json:"phase"`
	BlockType string `json:"blockType"`

	// chat-error
	Code      string `json:"code"`
	Message   string `json:"message"`
	Retryable bool   `json:"retryable"`

	// chat-turn-finished
	Status     string `json:"status"`
	MessageID  string `json:"messageId"`
	Finish     string `json:"finishReason"`
	Input      int    `json:"input"`
	Output     int    `json:"output"`
	UsageInput int    `json:"-"`
	Title      string `json:"title"`

	// chat-usage
	// (Input/Output above serve both chat-usage and chat-turn-finished)
	UsageOutput int `json:"-"`
}

// EventStream is one connection to the panel's live event gateway.
//
// The protocol is JSON text frames: a client subscribes with
// `{"type":"subscribe","topics":[...]}` (the set replaces the previous one) and
// the server pushes flat event objects. Chat deltas arrive on
// `chat:<sessionId>`; session-list changes on `chat`.
type EventStream struct {
	ws       *websocket.Conn
	events   chan Event
	done     chan struct{}
	closeOne sync.Once
	writeMu  sync.Mutex
	topics   []string
}

const (
	eventsHandshakeTimeout = 15 * time.Second
	eventsReadLimit        = 8 << 20
	eventsPingInterval     = 20 * time.Second
	eventsPongTimeout      = 10 * time.Second
)

// DialEvents opens `/ws` with the current session cookie.
//
// tlsConfig must be the same policy the REST client uses; nil means the
// process default (system roots), which is what an https panel with a public
// certificate needs.
func (c *Client) DialEvents(ctx context.Context, tlsConfig *tls.Config) (*EventStream, error) {
	target, err := c.WebSocketURL("/ws")
	if err != nil {
		return nil, err
	}
	dialer := &websocket.Dialer{
		HandshakeTimeout: eventsHandshakeTimeout,
		TLSClientConfig:  tlsConfig,
	}
	header := http.Header{}
	if cookie := c.Session(); cookie != "" {
		header.Set("Cookie", cookie)
	}
	conn, response, err := dialer.DialContext(ctx, target, header)
	if err != nil {
		if response != nil {
			return nil, &APIError{Status: response.StatusCode, Message: "无法连接事件通道: " + response.Status}
		}
		return nil, fmt.Errorf("无法连接事件通道: %w", err)
	}
	conn.SetReadLimit(eventsReadLimit)

	stream := &EventStream{
		ws:     conn,
		events: make(chan Event, 512),
		done:   make(chan struct{}),
	}
	conn.SetPongHandler(func(string) error {
		return conn.SetReadDeadline(time.Now().Add(eventsPingInterval + eventsPongTimeout))
	})
	go stream.readLoop()
	go stream.pingLoop()
	return stream, nil
}

// Subscribe replaces the subscription set and sends it to the server.
func (s *EventStream) Subscribe(topics ...string) error {
	s.writeMu.Lock()
	s.topics = append([]string(nil), topics...)
	s.writeMu.Unlock()
	return s.send(map[string]any{"type": "subscribe", "topics": topics})
}

// Events is the decoded frame stream. It is closed when the link dies.
func (s *EventStream) Events() <-chan Event { return s.events }

// Closed reports whether the connection has ended.
func (s *EventStream) Closed() bool {
	select {
	case <-s.done:
		return true
	default:
		return false
	}
}

// Close ends the connection. Safe to call more than once.
func (s *EventStream) Close() {
	s.closeOne.Do(func() {
		close(s.done)
		_ = s.ws.WriteControl(websocket.CloseMessage,
			websocket.FormatCloseMessage(websocket.CloseNormalClosure, ""),
			time.Now().Add(time.Second))
		_ = s.ws.Close()
	})
}

func (s *EventStream) send(payload any) error {
	encoded, err := json.Marshal(payload)
	if err != nil {
		return err
	}
	s.writeMu.Lock()
	defer s.writeMu.Unlock()
	return s.ws.WriteMessage(websocket.TextMessage, encoded)
}

func (s *EventStream) readLoop() {
	defer s.Close()
	for {
		messageType, data, err := s.ws.ReadMessage()
		if err != nil {
			return
		}
		if messageType != websocket.TextMessage {
			continue
		}
		var event Event
		if err := json.Unmarshal(data, &event); err != nil || event.Type == "" {
			continue
		}
		select {
		case s.events <- event:
		case <-s.done:
			return
		}
	}
}

func (s *EventStream) pingLoop() {
	ticker := time.NewTicker(eventsPingInterval)
	defer ticker.Stop()
	for {
		select {
		case <-s.done:
			return
		case <-ticker.C:
			_ = s.ws.WriteControl(websocket.PingMessage, nil,
				time.Now().Add(eventsPongTimeout))
		}
	}
}
