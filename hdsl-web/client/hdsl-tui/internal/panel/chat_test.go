package panel_test

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/nas200-timmy/HDSL-WEB/client/hdsl-tui/internal/panel"
)

func TestChatSendRoundTrip(t *testing.T) {
	var got panel.ChatSendBody
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost || r.URL.Path != "/api/chat/sessions/s1/messages" {
			t.Errorf("unexpected %s %s", r.Method, r.URL.Path)
		}
		if err := json.NewDecoder(r.Body).Decode(&got); err != nil {
			t.Errorf("decode body: %v", err)
		}
		io.WriteString(w, `{"turnId":"t1","assistantMessage":{"id":"m1","content":"hi"}}`)
	}))
	defer server.Close()

	client, err := panel.New(server.URL, server.Client(), nil)
	if err != nil {
		t.Fatalf("panel.New: %v", err)
	}
	result, err := client.ChatSend(context.Background(), "s1", panel.ChatSendBody{Text: "hello", Search: true})
	if err != nil {
		t.Fatalf("ChatSend: %v", err)
	}
	if result.TurnID != "t1" || result.AssistantMessage == nil || result.AssistantMessage.Content != "hi" {
		t.Fatalf("result = %+v", result)
	}
	if got.Text != "hello" || !got.Search {
		t.Fatalf("body = %+v", got)
	}
}

func TestChatSessionDecodesTheTimeline(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		io.WriteString(w, `{"id":"s1","title":"demo","nodes":[
			{"id":"n1","role":"assistant","activeVariantId":"m1","variants":[
				{"id":"m1","content":"answer","parts":[
					{"type":"thinking","text":"hmm","durationMs":3200},
					{"type":"tool","name":"search_web","query":"go tui","hits":2,
					 "sources":[{"title":"a","url":"https://a.example"}]}
				]}]}]}`)
	}))
	defer server.Close()

	client, err := panel.New(server.URL, server.Client(), nil)
	if err != nil {
		t.Fatalf("panel.New: %v", err)
	}
	session, err := client.ChatSession(context.Background(), "s1")
	if err != nil {
		t.Fatalf("ChatSession: %v", err)
	}
	if len(session.Nodes) != 1 {
		t.Fatalf("nodes = %+v", session.Nodes)
	}
	message, ok := session.Nodes[0].Active()
	if !ok || message.Content != "answer" {
		t.Fatalf("active = %+v ok=%v", message, ok)
	}
	if len(message.Parts) != 2 || message.Parts[0].Text != "hmm" ||
		message.Parts[1].Query != "go tui" || len(message.Parts[1].Sources) != 1 {
		t.Fatalf("parts = %+v", message.Parts)
	}
}

func TestChatSettingsRefusalKeepsTheStatus(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusForbidden)
		io.WriteString(w, `{"error":"developer password required"}`)
	}))
	defer server.Close()

	client, err := panel.New(server.URL, server.Client(), nil)
	if err != nil {
		t.Fatalf("panel.New: %v", err)
	}
	_, err = client.ChatSettings(context.Background())
	var apiError *panel.APIError
	if !errors.As(err, &apiError) || apiError.Status != http.StatusForbidden {
		t.Fatalf("err = %v, want a 403 APIError", err)
	}
}

func TestChatSessionReadsARealTranscriptSize(t *testing.T) {
	// A long session used to be cut at the error-body cap (64 KiB), which the
	// decoder reported as "unexpected end of JSON input".
	content := strings.Repeat("这是一段很长的回答。", 8000)
	encoded, err := json.Marshal(content)
	if err != nil {
		t.Fatalf("marshal: %v", err)
	}
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		fmt.Fprintf(w, `{"id":"s1","nodes":[{"id":"n1","role":"assistant",
			"activeVariantId":"m1","variants":[{"id":"m1","content":%s}]}]}`, encoded)
	}))
	defer server.Close()

	client, err := panel.New(server.URL, server.Client(), nil)
	if err != nil {
		t.Fatalf("panel.New: %v", err)
	}
	session, err := client.ChatSession(context.Background(), "s1")
	if err != nil {
		t.Fatalf("ChatSession: %v", err)
	}
	message, ok := session.Nodes[0].Active()
	if !ok || message.Content != content {
		t.Fatalf("content length = %d, want %d", len(message.Content), len(content))
	}
}
