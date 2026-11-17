// Package pterm is the WebSocket half: the byte channel to the agent plus the
// JSON control frames around it. See docs/terminal-protocol.md.
package pterm

import (
	"encoding/json"
	"fmt"
)

// Protocol is the control-frame revision this client understands. The server
// announces it in the `ready` frame; the byte channel itself is unchanged
// between revisions, so a mismatch is a warning, not a refusal.
const Protocol = 1

// Control frame types.
const (
	TypeReady  = "ready"
	TypeExit   = "exit"
	TypeResize = "resize"
)

// Control is a decoded text frame. Unknown fields are ignored and unknown
// types are carried through with only Type set, so a newer server does not
// break an older client.
type Control struct {
	Type     string
	Protocol int
	Instance string
	Brand    string
	Cols     int
	Rows     int
	Code     int
	HasCode  bool
}

// DecodeControl parses a server text frame. A frame that is not JSON at all is
// reported as an error; a frame that is JSON but unrecognised is returned with
// an empty Type.
func DecodeControl(data []byte) (Control, error) {
	var fields map[string]json.RawMessage
	if err := json.Unmarshal(data, &fields); err != nil {
		return Control{}, fmt.Errorf("控制帧不是 JSON: %w", err)
	}
	var control Control
	decode(fields, "type", &control.Type)
	decode(fields, "protocol", &control.Protocol)
	decode(fields, "instance", &control.Instance)
	decode(fields, "brand", &control.Brand)
	decode(fields, "cols", &control.Cols)
	decode(fields, "rows", &control.Rows)
	if raw, ok := fields["code"]; ok {
		var code int
		if json.Unmarshal(raw, &code) == nil {
			control.Code = code
			control.HasCode = true
		}
	}
	return control, nil
}

func decode(fields map[string]json.RawMessage, name string, target any) {
	raw, ok := fields[name]
	if !ok {
		return
	}
	_ = json.Unmarshal(raw, target)
}

// EncodeResize builds the only control frame a client sends: the PTY window
// size, in characters.
func EncodeResize(cols, rows int) ([]byte, error) {
	if cols < 1 || rows < 1 {
		return nil, fmt.Errorf("无效的终端尺寸 %dx%d", cols, rows)
	}
	return json.Marshal(struct {
		Type string `json:"type"`
		Cols int    `json:"cols"`
		Rows int    `json:"rows"`
	}{Type: TypeResize, Cols: cols, Rows: rows})
}
