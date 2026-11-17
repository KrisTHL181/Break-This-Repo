package panel_test

import (
	"context"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/gorilla/websocket"

	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/panel"
)

func TestEventStreamSubscribesAndDeliversChatFrames(t *testing.T) {
	gotCookie := make(chan string, 1)
	subscribe := make(chan string, 1)
	upgrader := websocket.Upgrader{}
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/ws" {
			t.Errorf("path = %s", r.URL.Path)
		}
		gotCookie <- r.Header.Get("Cookie")
		socket, err := upgrader.Upgrade(w, r, nil)
		if err != nil {
			return
		}
		defer socket.Close()
		_, first, err := socket.ReadMessage()
		if err == nil {
			subscribe <- string(first)
		}
		_ = socket.WriteMessage(websocket.TextMessage,
			[]byte(`{"type":"chat-delta","sessionId":"s1","turnId":"t1","text":"你好"}`))
		time.Sleep(200 * time.Millisecond)
	}))
	defer server.Close()

	client, err := panel.New(server.URL, server.Client(), nil)
	if err != nil {
		t.Fatalf("panel.New: %v", err)
	}
	client.SetSession("cookie-value")

	stream, err := client.DialEvents(context.Background(), nil)
	if err != nil {
		t.Fatalf("DialEvents: %v", err)
	}
	defer stream.Close()
	if err := stream.Subscribe("chat", "chat:s1"); err != nil {
		t.Fatalf("Subscribe: %v", err)
	}

	select {
	case cookie := <-gotCookie:
		if !strings.Contains(cookie, "hdsl_session=cookie-value") {
			t.Fatalf("cookie = %q", cookie)
		}
	case <-time.After(2 * time.Second):
		t.Fatal("the handshake never arrived")
	}
	select {
	case frame := <-subscribe:
		if !strings.Contains(frame, `"subscribe"`) || !strings.Contains(frame, "chat:s1") {
			t.Fatalf("subscribe frame = %s", frame)
		}
	case <-time.After(2 * time.Second):
		t.Fatal("no subscribe frame")
	}
	select {
	case event := <-stream.Events():
		if event.Type != "chat-delta" || event.SessionID != "s1" || event.Text != "你好" {
			t.Fatalf("event = %+v", event)
		}
	case <-time.After(2 * time.Second):
		t.Fatal("no chat event")
	}
}
