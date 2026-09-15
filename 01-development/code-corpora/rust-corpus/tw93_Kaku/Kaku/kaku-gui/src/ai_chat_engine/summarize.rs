//! Conversation history summarization for the agent loop.
//!
//! When in-flight message bytes exceed a threshold, fold older turns into a
//! single summary block to free room for the active task. Modeled after the
//! Claude Code conversation-summarization prompt: keep recent messages
//! verbatim, replace older ones with a structured summary that preserves
//! constraints the user stated.
//!
//! Entry point: `summarize_in_place`. It calls the model synchronously via
//! `AiClient::complete_once`, and on success rewrites `messages` in place.
//! On any error it leaves `messages` untouched so the caller can continue.

use super::strip_prompt_metadata;
use crate::ai_client::{AiClient, ApiMessage};

/// Bytes of conversation history above which we trigger summarization.
/// Picked below `MAX_HISTORY_BYTES` (120k) so we have headroom before the
/// hard "wrap up" nag in the agent loop kicks in.
pub(crate) const SUMMARIZE_THRESHOLD_BYTES: usize = 72_000;

/// System and environment messages are always preserved. The environment turn
/// carries cwd/date/platform context and must not be folded into a summary.
const PREFIX_KEEP: usize = 2;

/// How many recent messages to keep verbatim. Anything older folds into the
/// summary, unless doing so would cut through the active tool-call sequence.
const KEEP_TAIL: usize = 6;

const SUMMARIZE_PROMPT: &str = include_str!("../../../assets/prompts/summarize.txt");

/// Leading marker for the folded-summary user message. Kept as a constant so
/// the generator (`summarize_in_place`) and the detector (`is_summary_message`)
/// can't drift apart.
const SUMMARY_PREFIX: &str = "Previous conversation summary";

fn role_of(msg: &ApiMessage) -> &str {
    if let Some(item) = msg.0.get("kaku_responses_output_item") {
        return if item["type"] == "function_call_output" {
            "tool"
        } else {
            "assistant"
        };
    }
    msg.0.get("role").and_then(|v| v.as_str()).unwrap_or("user")
}

fn content_of(msg: &ApiMessage) -> String {
    if let Some(item) = msg.0.get("kaku_responses_output_item") {
        return match item["type"].as_str() {
            Some("message") => item["content"]
                .as_array()
                .into_iter()
                .flatten()
                .filter_map(|part| part["text"].as_str().or_else(|| part["refusal"].as_str()))
                .collect::<Vec<_>>()
                .join(""),
            Some("function_call") => format!(
                "[tool_call: {}]",
                item["name"].as_str().unwrap_or("unknown")
            ),
            Some("function_call_output") => item["output"].as_str().unwrap_or("").to_string(),
            _ => String::new(),
        };
    }
    if let Some(s) = msg.0.get("content").and_then(|v| v.as_str()) {
        return s.to_string();
    }
    if let Some(arr) = msg.0.get("tool_calls").and_then(|v| v.as_array()) {
        let names: Vec<String> = arr
            .iter()
            .filter_map(|c| {
                c.get("function")
                    .and_then(|f| f.get("name"))
                    .and_then(|n| n.as_str())
                    .map(String::from)
            })
            .collect();
        return format!("[tool_calls: {}]", names.join(", "));
    }
    String::new()
}

fn serialize_transcript(msgs: &[ApiMessage]) -> String {
    let mut out = String::new();
    for m in msgs {
        let role = role_of(m);
        let content = content_of(m);
        if content.is_empty() {
            continue;
        }
        out.push_str(&format!("<{}>\n{}\n</{}>\n\n", role, content, role));
    }
    out
}

fn has_tool_calls(msg: &ApiMessage) -> bool {
    msg.0.get("tool_calls").and_then(|v| v.as_array()).is_some()
}

fn is_responses_output_item(msg: &ApiMessage) -> bool {
    msg.0.get("kaku_responses_output_item").is_some()
}

fn is_tool_result(msg: &ApiMessage) -> bool {
    role_of(msg) == "tool"
}

fn is_summary_message(msg: &ApiMessage) -> bool {
    role_of(msg) == "user" && content_of(msg).starts_with(SUMMARY_PREFIX)
}

/// Index of the user message that initiated the tool sequence ending at
/// `boundary`. Falls back to `boundary` when no user turn precedes it.
fn active_tool_sequence_start(messages: &[ApiMessage], boundary: usize) -> usize {
    messages[PREFIX_KEEP..boundary]
        .iter()
        .rposition(|msg| role_of(msg) == "user")
        .map(|idx| PREFIX_KEEP + idx)
        .unwrap_or(boundary)
}

fn summary_split_index(messages: &[ApiMessage]) -> Option<usize> {
    if messages.len() <= PREFIX_KEEP + KEEP_TAIL {
        return None;
    }

    let mut split = messages.len() - KEEP_TAIL;

    // Never orphan a tool result from its tool_calls: if the tail would begin
    // on a tool result, walk the boundary back until it doesn't.
    while split > PREFIX_KEEP && is_tool_result(&messages[split]) {
        split -= 1;
    }

    // If the tail now begins on an assistant tool_calls turn, pull the split
    // back to the user message that initiated *that* sequence so the
    // initiating prompt is not folded away from its tool exchange. This
    // anchors on the sequence straddling the boundary, not the first tool use
    // in the whole conversation. Anchoring on the first tool use previously
    // dragged the split to the start and disabled folding entirely whenever
    // tools ran early in the session.
    if split > PREFIX_KEEP
        && (has_tool_calls(&messages[split]) || is_responses_output_item(&messages[split]))
    {
        split = active_tool_sequence_start(messages, split);
    }

    if split <= PREFIX_KEEP {
        return None;
    }

    // Don't fold when the only foldable content is a prior summary block.
    // Recompressing a summary in isolation adds no new context and just
    // compounds information loss every round (and, paired with an oversized
    // tail that keeps total bytes above the threshold, would re-fire each
    // round). Folding a summary together with genuinely new turns is fine.
    if messages[PREFIX_KEEP..split].iter().all(is_summary_message) {
        return None;
    }

    Some(split)
}

/// Replace older ordinary history with a single summary user message. Returns
/// true if substitution happened.
///
/// `model` is the model to use for the summarization call. Caller should pass
/// `fast_model` when available, falling back to `chat_model`, to keep this
/// step cheap and quick.
pub(crate) fn summarize_in_place(
    client: &AiClient,
    model: &str,
    messages: &mut Vec<ApiMessage>,
) -> bool {
    let Some(split) = summary_split_index(messages) else {
        return false;
    };
    let older = &messages[PREFIX_KEEP..split];
    if older.is_empty() {
        return false;
    }

    let transcript = serialize_transcript(older);
    let prompt = strip_prompt_metadata(SUMMARIZE_PROMPT).replace("${TRANSCRIPT}", &transcript);

    let req = vec![ApiMessage::system(prompt)];

    let summary = match client.complete_once(model, &req) {
        Ok(s) if !s.trim().is_empty() => s,
        Ok(_) => {
            log::warn!("summarize_in_place: model returned empty summary");
            return false;
        }
        Err(e) => {
            log::warn!("summarize_in_place: model call failed: {e}");
            return false;
        }
    };

    let summary_msg = ApiMessage::user(format!(
        "{} (covers turns earlier than the {} most recent):\n{}",
        SUMMARY_PREFIX, KEEP_TAIL, summary
    ));

    let tail: Vec<ApiMessage> = messages.drain(split..).collect();
    messages.truncate(PREFIX_KEEP);
    messages.push(summary_msg);
    messages.extend(tail);
    true
}

#[cfg(test)]
mod tests {
    use super::*;

    fn tool_calls() -> ApiMessage {
        ApiMessage::assistant_tool_calls(serde_json::json!([
            {
                "id": "call_1",
                "type": "function",
                "function": { "name": "fs_read", "arguments": "{}" }
            },
            {
                "id": "call_2",
                "type": "function",
                "function": { "name": "grep_search", "arguments": "{}" }
            }
        ]))
    }

    #[test]
    fn summary_split_preserves_system_and_environment_prefix() {
        let mut messages = vec![
            ApiMessage::system("system"),
            ApiMessage::user("environment"),
        ];
        for idx in 0..10 {
            messages.push(ApiMessage::user(format!("user {idx}")));
        }

        assert_eq!(summary_split_index(&messages), Some(6));
    }

    #[test]
    fn summary_split_does_not_cut_into_active_tool_sequence() {
        let messages = vec![
            ApiMessage::system("system"),
            ApiMessage::user("environment"),
            ApiMessage::user("old user"),
            ApiMessage::assistant("old assistant"),
            ApiMessage::user("current user"),
            tool_calls(),
            ApiMessage::tool_result("call_1", "fs_read", "content"),
            ApiMessage::tool_result("call_2", "grep_search", "matches"),
            ApiMessage::assistant("after tools"),
            ApiMessage::user("follow up"),
            ApiMessage::assistant("answer"),
            ApiMessage::user("tail"),
        ];

        assert_eq!(summary_split_index(&messages), Some(4));
    }

    #[test]
    fn summary_split_does_not_cut_into_responses_tool_sequence() {
        let messages = vec![
            ApiMessage::system("system"),
            ApiMessage::user("environment"),
            ApiMessage::user("old user"),
            ApiMessage::assistant("old assistant"),
            ApiMessage::user("current user"),
            ApiMessage::responses_output_item(serde_json::json!({
                "id": "reasoning_1",
                "type": "reasoning",
                "encrypted_content": "opaque"
            })),
            ApiMessage::responses_output_item(serde_json::json!({
                "id": "fc_1",
                "type": "function_call",
                "call_id": "call_1",
                "name": "fs_read",
                "arguments": "{}"
            })),
            ApiMessage::tool_result("call_1", "fs_read", "content"),
            ApiMessage::assistant("after tools"),
            ApiMessage::user("follow up"),
            ApiMessage::assistant("answer"),
            ApiMessage::user("tail"),
        ];

        assert_eq!(summary_split_index(&messages), Some(4));
    }

    #[test]
    fn summary_split_folds_when_tools_were_used_early() {
        // Regression: tools used right after the prefix must not anchor the
        // split to the start of the conversation. Older history should still
        // fold once the conversation grows past the tail window.
        let mut messages = vec![
            ApiMessage::system("system"),
            ApiMessage::user("environment"),
            ApiMessage::user("first question"),
            tool_calls(),
            ApiMessage::tool_result("call_1", "fs_read", "content"),
            ApiMessage::tool_result("call_2", "grep_search", "matches"),
            ApiMessage::assistant("early answer"),
        ];
        for idx in 0..8 {
            messages.push(ApiMessage::user(format!("later {idx}")));
        }

        let split =
            summary_split_index(&messages).expect("history must fold despite early tool use");
        assert!(split > PREFIX_KEEP);
        assert_eq!(split, messages.len() - KEEP_TAIL);
    }

    #[test]
    fn summary_split_returns_none_when_only_tool_sequence_would_be_summarized() {
        let messages = vec![
            ApiMessage::system("system"),
            ApiMessage::user("environment"),
            tool_calls(),
            ApiMessage::tool_result("call_1", "fs_read", "content"),
            ApiMessage::tool_result("call_2", "grep_search", "matches"),
            ApiMessage::assistant("after tools"),
            ApiMessage::user("follow up"),
            ApiMessage::assistant("answer"),
            ApiMessage::user("tail"),
        ];

        assert_eq!(summary_split_index(&messages), None);
    }

    fn summary_msg() -> ApiMessage {
        ApiMessage::user(format!(
            "{} (covers turns earlier than the {} most recent):\nfolded",
            SUMMARY_PREFIX, KEEP_TAIL
        ))
    }

    #[test]
    fn summary_message_is_detected() {
        assert!(is_summary_message(&summary_msg()));
        assert!(!is_summary_message(&ApiMessage::user("ordinary user turn")));
        assert!(!is_summary_message(&ApiMessage::assistant(SUMMARY_PREFIX)));
    }

    #[test]
    fn summary_split_skips_when_only_prior_summary_is_foldable() {
        // [system, env, summary, +KEEP_TAIL plain] -> foldable region is just
        // the prior summary, so we must not re-fold it.
        let mut messages = vec![
            ApiMessage::system("system"),
            ApiMessage::user("environment"),
            summary_msg(),
        ];
        for idx in 0..KEEP_TAIL {
            messages.push(ApiMessage::user(format!("tail {idx}")));
        }

        assert_eq!(summary_split_index(&messages), None);
    }

    #[test]
    fn summary_split_folds_new_turns_after_prior_summary() {
        // A prior summary followed by genuinely new turns is foldable: rolling
        // the new content into the summary is the intended behavior.
        let mut messages = vec![
            ApiMessage::system("system"),
            ApiMessage::user("environment"),
            summary_msg(),
            ApiMessage::user("new turn 1"),
            ApiMessage::assistant("new turn 2"),
        ];
        for idx in 0..KEEP_TAIL {
            messages.push(ApiMessage::user(format!("tail {idx}")));
        }

        assert_eq!(summary_split_index(&messages), Some(5));
    }
}
