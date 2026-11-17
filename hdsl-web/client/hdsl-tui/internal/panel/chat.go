package panel

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
)

// ChatAssistant is one role a session can belong to.
type ChatAssistant struct {
	ID             string `json:"id"`
	Name           string `json:"name"`
	Avatar         string `json:"avatar"`
	Description    string `json:"description"`
	SystemPrompt   string `json:"systemPrompt"`
	OpeningMessage string `json:"openingMessage"`
	Account        string `json:"account"`
	Model          string `json:"model"`
	Preset         string `json:"preset"`
	Protocol       string `json:"protocol"`
}

// ChatSessionSummary is one row of the session list.
type ChatSessionSummary struct {
	ID           string `json:"id"`
	AssistantID  string `json:"assistantId"`
	Title        string `json:"title"`
	Model        string `json:"model"`
	Account      string `json:"account"`
	CreatedAt    int64  `json:"createdAt"`
	UpdatedAt    int64  `json:"updatedAt"`
	MessageCount int    `json:"messageCount"`
	LastMessage  string `json:"lastMessage"`
}

// ChatSource is one citation or search hit.
type ChatSource struct {
	Title string `json:"title"`
	URL   string `json:"url"`
	Site  string `json:"site"`
}

// ChatToolCall is one call inside a `tool_calls` part.
type ChatToolCall struct {
	ID        string `json:"id"`
	Name      string `json:"name"`
	Arguments string `json:"arguments"`
}

// ChatPart is one entry of a message's timeline: reasoning, a tool call, or a
// tool result. The server stores these for the web UI's collapsible timeline.
type ChatPart struct {
	Type       string         `json:"type"`
	Text       string         `json:"text"`
	DurationMs int64          `json:"durationMs"`
	Calls      []ChatToolCall `json:"calls"`
	ID         string         `json:"id"`
	Name       string         `json:"name"`
	Query      string         `json:"query"`
	Content    string         `json:"content"`
	Hits       int            `json:"hits"`
	Sources    []ChatSource   `json:"sources"`
}

// ChatAttachment is the metadata of one uploaded file. The TUI does not fetch
// attachments; it never shows them beyond their names.
type ChatAttachment struct {
	ID   string `json:"id"`
	Name string `json:"name"`
	Mime string `json:"mime"`
	Kind string `json:"kind"`
	Size int64  `json:"size"`
}

// ChatMessage is one stored variant.
type ChatMessage struct {
	ID          string           `json:"id"`
	NodeID      string           `json:"nodeId"`
	Variant     int              `json:"variantIndex"`
	Content     string           `json:"content"`
	Reasoning   string           `json:"reasoning"`
	Status      string           `json:"status"`
	Model       string           `json:"model"`
	CreatedAt   int64            `json:"createdAt"`
	Parts       []ChatPart       `json:"parts"`
	Blocks      []ChatBlock      `json:"blocks"`
	Sources     []ChatSource     `json:"sources"`
	Attachments []ChatAttachment `json:"attachments"`
}

// ChatBlock is one DeepSeek-protocol content block: thinking, server tool use,
// tool result or text. The plain timeline renderer reads these when a message
// has no `parts` (the DeepSeek preset stores blocks instead).
type ChatBlock struct {
	Type      string          `json:"type"`
	Thinking  string          `json:"thinking"`
	Text      string          `json:"text"`
	Signature string          `json:"signature"`
	ID        string          `json:"id"`
	Name      string          `json:"name"`
	Input     json.RawMessage `json:"input"`
	ToolUseID string          `json:"tool_use_id"`
	Content   json.RawMessage `json:"content"`
}

// ChatNode is one turn: a user question or an assistant answer, with variants.
type ChatNode struct {
	ID              string        `json:"id"`
	Ordinal         int           `json:"ordinal"`
	Role            string        `json:"role"`
	ActiveVariantID string        `json:"activeVariantId"`
	CreatedAt       int64         `json:"createdAt"`
	Variants        []ChatMessage `json:"variants"`
}

// Active returns the message the web UI shows for this node.
func (n ChatNode) Active() (ChatMessage, bool) {
	for _, message := range n.Variants {
		if message.ID == n.ActiveVariantID {
			return message, true
		}
	}
	if len(n.Variants) == 0 {
		return ChatMessage{}, false
	}
	return n.Variants[0], true
}

// ChatSession is a full transcript.
type ChatSession struct {
	ID             string     `json:"id"`
	AssistantID    string     `json:"assistantId"`
	Title          string     `json:"title"`
	Model          string     `json:"model"`
	Account        string     `json:"account"`
	ReasoningLevel string     `json:"reasoningLevel"`
	CreatedAt      int64      `json:"createdAt"`
	UpdatedAt      int64      `json:"updatedAt"`
	Streaming      bool       `json:"streaming"`
	Nodes          []ChatNode `json:"nodes"`
}

// ChatTurnResult is what a send/regenerate/edit answers with.
type ChatTurnResult struct {
	TurnID           string       `json:"turnId"`
	AssistantMessage *ChatMessage `json:"assistantMessage"`
	UserMessage      *ChatMessage `json:"userMessage"`
	AssistantNodeID  string       `json:"assistantNodeId"`
}

// ChatSettings is the part of the chat settings the TUI reads. The endpoint
// itself needs developer elevation; the TUI only uses it for defaults and to
// show the daily budget.
type ChatSettings struct {
	Retention          string `json:"retention"`
	RetentionDays      int    `json:"retentionDays"`
	LongTextTokens     int    `json:"longTextTokens"`
	DailyTokenLimit    int    `json:"dailyTokenLimit"`
	DefaultAccount     string `json:"defaultAccount"`
	DefaultModel       string `json:"defaultModel"`
	SearchProvider     string `json:"searchProvider"`
	SearchMaxResults   int    `json:"searchMaxResults"`
	PersistToolTrail   bool   `json:"persistToolTrail"`
	DeepseekPreset     bool   `json:"deepseekPresetEnabled"`
	AgentChatMaxTokens int    `json:"agentChatMaxInputTokens"`
}

// ChatSearchStatus answers which search provider is configured.
type ChatSearchStatus struct {
	Configured bool   `json:"configured"`
	Provider   string `json:"provider"`
}

// ChatSendBody is the POST body for a message.
type ChatSendBody struct {
	Text         string   `json:"text"`
	Attachments  []string `json:"attachments,omitempty"`
	Model        string   `json:"model,omitempty"`
	Account      string   `json:"account,omitempty"`
	RequestID    string   `json:"requestId,omitempty"`
	Search       bool     `json:"search,omitempty"`
	SearchSource string   `json:"searchProvider,omitempty"`
}

// ChatSessionPatch carries the mutable session fields; nil fields are omitted.
type ChatSessionPatch struct {
	Title          *string `json:"title,omitempty"`
	Model          *string `json:"model,omitempty"`
	Account        *string `json:"account,omitempty"`
	ReasoningLevel *string `json:"reasoningLevel,omitempty"`
}

// ChatAssistants lists the roles.
func (c *Client) ChatAssistants(ctx context.Context) ([]ChatAssistant, error) {
	_, data, err := c.do(ctx, http.MethodGet, "/api/chat/assistants", nil, nil)
	if err != nil {
		return nil, err
	}
	var body struct {
		Assistants []ChatAssistant `json:"assistants"`
	}
	if err := json.Unmarshal(data, &body); err != nil {
		return nil, err
	}
	return body.Assistants, nil
}

// ChatModel is one entry of GET /api/chat/models: the id to send plus the
// capabilities the panel resolved for it.
type ChatModel struct {
	ID   string `json:"id"`
	Caps struct {
		Vision    bool `json:"vision"`
		Files     bool `json:"files"`
		Reasoning bool `json:"reasoning"`
	} `json:"caps"`
}

// ChatModels lists the models an account offers.
func (c *Client) ChatModels(ctx context.Context, account string) ([]ChatModel, error) {
	query := url.Values{"account": {account}}
	_, data, err := c.do(ctx, http.MethodGet, "/api/chat/models?"+query.Encode(), nil, nil)
	if err != nil {
		return nil, err
	}
	var body struct {
		Models []ChatModel `json:"models"`
	}
	if err := json.Unmarshal(data, &body); err != nil {
		return nil, err
	}
	return body.Models, nil
}

// ChatSessions lists summaries, optionally for one assistant.
func (c *Client) ChatSessions(ctx context.Context, assistantID string) ([]ChatSessionSummary, error) {
	query := url.Values{}
	if assistantID != "" {
		query.Set("assistant", assistantID)
	}
	_, data, err := c.do(ctx, http.MethodGet, "/api/chat/sessions", query, nil)
	if err != nil {
		return nil, err
	}
	var body struct {
		Sessions []ChatSessionSummary `json:"sessions"`
	}
	if err := json.Unmarshal(data, &body); err != nil {
		return nil, err
	}
	return body.Sessions, nil
}

// ChatSession reads one transcript.
func (c *Client) ChatSession(ctx context.Context, id string) (ChatSession, error) {
	_, data, err := c.do(ctx, http.MethodGet, "/api/chat/sessions/"+url.PathEscape(id), nil, nil)
	if err != nil {
		return ChatSession{}, err
	}
	var session ChatSession
	if err := json.Unmarshal(data, &session); err != nil {
		return ChatSession{}, err
	}
	return session, nil
}

// ChatCreateSession opens a session for an assistant.
func (c *Client) ChatCreateSession(ctx context.Context, assistantID string) (ChatSession, error) {
	body := map[string]any{"assistantId": assistantID}
	_, data, err := c.do(ctx, http.MethodPost, "/api/chat/sessions", nil, body)
	if err != nil {
		return ChatSession{}, err
	}
	var session ChatSession
	if err := json.Unmarshal(data, &session); err != nil {
		return ChatSession{}, err
	}
	return session, nil
}

// ChatPatchSession changes the title/model/account of a session.
func (c *Client) ChatPatchSession(ctx context.Context, id string, patch ChatSessionPatch) (ChatSession, error) {
	_, data, err := c.do(ctx, http.MethodPatch, "/api/chat/sessions/"+url.PathEscape(id), nil, patch)
	if err != nil {
		return ChatSession{}, err
	}
	var session ChatSession
	if err := json.Unmarshal(data, &session); err != nil {
		return ChatSession{}, err
	}
	return session, nil
}

// ChatDeleteSession removes a session.
func (c *Client) ChatDeleteSession(ctx context.Context, id string) error {
	_, _, err := c.do(ctx, http.MethodDelete, "/api/chat/sessions/"+url.PathEscape(id), nil, nil)
	return err
}

// ChatSend posts a message and starts a turn.
func (c *Client) ChatSend(ctx context.Context, sessionID string, body ChatSendBody) (ChatTurnResult, error) {
	_, data, err := c.do(ctx, http.MethodPost,
		"/api/chat/sessions/"+url.PathEscape(sessionID)+"/messages", nil, body)
	if err != nil {
		return ChatTurnResult{}, err
	}
	return decodeOutcome(data)
}

// ChatCancel stops a running turn.
func (c *Client) ChatCancel(ctx context.Context, turnID string) error {
	_, _, err := c.do(ctx, http.MethodPost, "/api/chat/turns/"+url.PathEscape(turnID)+"/cancel", nil, map[string]any{})
	return err
}

// ChatRegenerate asks for another answer; mode is "in_place" or "truncate_after".
func (c *Client) ChatRegenerate(ctx context.Context, sessionID, nodeID, mode string, search bool) (ChatTurnResult, error) {
	body := map[string]any{"nodeId": nodeID, "mode": mode, "search": search}
	_, data, err := c.do(ctx, http.MethodPost,
		"/api/chat/sessions/"+url.PathEscape(sessionID)+"/regenerate", nil, body)
	if err != nil {
		return ChatTurnResult{}, err
	}
	return decodeOutcome(data)
}

// ChatEdit rewrites one user message and regenerates what follows.
func (c *Client) ChatEdit(ctx context.Context, sessionID, nodeID, text string) (ChatTurnResult, error) {
	body := map[string]any{"nodeId": nodeID, "text": text}
	_, data, err := c.do(ctx, http.MethodPost,
		"/api/chat/sessions/"+url.PathEscape(sessionID)+"/edit", nil, body)
	if err != nil {
		return ChatTurnResult{}, err
	}
	return decodeOutcome(data)
}

// ChatSettings reads the panel's chat settings. The endpoint requires developer
// elevation; a caller without one gets a 403 APIError.
func (c *Client) ChatSettings(ctx context.Context) (ChatSettings, error) {
	_, data, err := c.do(ctx, http.MethodGet, "/api/chat/settings", nil, nil)
	if err != nil {
		return ChatSettings{}, err
	}
	var settings ChatSettings
	if err := json.Unmarshal(data, &settings); err != nil {
		return ChatSettings{}, err
	}
	return settings, nil
}

// ChatSearchStatus reports whether a search provider is configured.
func (c *Client) ChatSearchStatus(ctx context.Context) (ChatSearchStatus, error) {
	_, data, err := c.do(ctx, http.MethodGet, "/api/chat/search/status", nil, nil)
	if err != nil {
		return ChatSearchStatus{}, err
	}
	var status ChatSearchStatus
	if err := json.Unmarshal(data, &status); err != nil {
		return ChatSearchStatus{}, err
	}
	return status, nil
}

func decodeOutcome(data []byte) (ChatTurnResult, error) {
	var outcome ChatTurnResult
	if err := json.Unmarshal(data, &outcome); err != nil {
		return ChatTurnResult{}, err
	}
	if outcome.TurnID == "" {
		return ChatTurnResult{}, fmt.Errorf("服务端没有返回 turnId")
	}
	return outcome, nil
}
