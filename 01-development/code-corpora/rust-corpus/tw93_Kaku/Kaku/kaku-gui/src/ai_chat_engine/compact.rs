//! Tool-output compaction for the AI chat agent loop.

use crate::ai_client::ApiMessage;
use std::collections::HashMap;
use std::path::Path;

const FS_READ_CAP: usize = 300;
const GREP_CAP: usize = 100;
const GREP_HEAD: usize = 70;
const GREP_TAIL: usize = 20;
const BASH_CAP: usize = 150;
const BASH_HEAD: usize = 100;
const BASH_TAIL: usize = 40;
const TOOL_CONTENT_BYTE_CAP: usize = 16 * 1024;
const TOOL_CONTENT_HEAD_BYTES: usize = 12 * 1024;
const TOOL_CONTENT_TAIL_BYTES: usize = 3 * 1024;

fn boundary_at_or_before(content: &str, index: usize) -> usize {
    let mut boundary = index.min(content.len());
    while boundary > 0 && !content.is_char_boundary(boundary) {
        boundary -= 1;
    }
    boundary
}

fn compact_tool_bytes(content: &str) -> Option<String> {
    if content.len() <= TOOL_CONTENT_BYTE_CAP {
        return None;
    }
    let head_end = boundary_at_or_before(content, TOOL_CONTENT_HEAD_BYTES);
    let tail_start = boundary_at_or_before(
        content,
        content.len().saturating_sub(TOOL_CONTENT_TAIL_BYTES),
    );
    Some(format!(
        "{}\n[{} bytes elided]\n{}",
        &content[..head_end],
        tail_start.saturating_sub(head_end),
        &content[tail_start..]
    ))
}

fn compact_tool_content(tool_name: &str, content: &str) -> Option<String> {
    if let Some(compacted) = compact_tool_bytes(content) {
        return Some(compacted);
    }
    let lines: Vec<&str> = content.lines().collect();
    match tool_name {
        "fs_read" if lines.len() > FS_READ_CAP => {
            let kept = FS_READ_CAP.saturating_sub(1);
            Some(format!(
                "[fs_read: {} lines total, showing first {}. Use start_line/end_line to inspect remaining lines]\n{}",
                lines.len(),
                kept,
                lines[..kept].join("\n")
            ))
        }
        "grep_search" | "symbol_search" | "fs_search" | "fs_list" if lines.len() > GREP_CAP => {
            let total = lines.len();
            Some(format!(
                "{}\n[{} lines elided]\n{}",
                lines[..GREP_HEAD].join("\n"),
                total - GREP_HEAD - GREP_TAIL,
                lines[total - GREP_TAIL..].join("\n")
            ))
        }
        "shell_exec" | "shell_bg" if lines.len() > BASH_CAP => {
            let total = lines.len();
            Some(format!(
                "{}\n[{} lines elided]\n{}",
                lines[..BASH_HEAD].join("\n"),
                total - BASH_HEAD - BASH_TAIL,
                lines[total - BASH_TAIL..].join("\n")
            ))
        }
        _ => None,
    }
}

/// `namespace` separates the two index spaces that share a round: "m" for
/// message-list indexes, "i" for responses-item indexes. Without it the two
/// loops overwrite each other's saved originals within a round.
fn save_original(
    outputs_dir: Option<&Path>,
    namespace: &str,
    round: usize,
    idx: usize,
    content: &str,
) {
    if let Some(dir) = outputs_dir {
        if std::fs::create_dir_all(dir).is_ok() {
            let fname = format!("r{}{}-{}.txt", round, namespace, idx);
            let _ = std::fs::write(dir.join(fname), content.as_bytes());
        }
    }
}

fn compact_response_values(
    values: &mut [serde_json::Value],
    round: usize,
    outputs_dir: Option<&Path>,
) {
    let mut call_names = HashMap::<String, String>::new();
    for (idx, item) in values.iter_mut().enumerate() {
        match item["type"].as_str() {
            Some("function_call") => {
                if let (Some(call_id), Some(name)) =
                    (item["call_id"].as_str(), item["name"].as_str())
                {
                    call_names.insert(call_id.to_string(), name.to_string());
                }
            }
            Some("function_call_output") => {
                let call_id = item["call_id"].as_str().unwrap_or("");
                let tool_name = call_names.get(call_id).map(String::as_str).unwrap_or("");
                let Some(content) = item["output"].as_str().map(str::to_owned) else {
                    continue;
                };
                let Some(compacted) = compact_tool_content(tool_name, &content) else {
                    continue;
                };
                save_original(outputs_dir, "i", round, idx, &content);
                item["output"] = serde_json::Value::String(compacted);
            }
            _ => {}
        }
    }
}

/// Apply micro-compaction to all tool-result messages in `messages`.
pub(crate) fn micro_compact(messages: &mut [ApiMessage], round: usize, outputs_dir: Option<&Path>) {
    let mut response_item_indexes = Vec::new();
    for (idx, msg) in messages.iter_mut().enumerate() {
        if msg.0.get("kaku_responses_output_item").is_some() {
            response_item_indexes.push(idx);
            continue;
        }
        let role = msg.0.get("role").and_then(|v| v.as_str()).unwrap_or("");
        if role != "tool" {
            continue;
        }
        let content = match msg.0.get("content").and_then(|v| v.as_str()) {
            Some(c) => c.to_string(),
            None => continue,
        };
        let tool_name = msg.0.get("name").and_then(|v| v.as_str()).unwrap_or("");
        let Some(compacted) = compact_tool_content(tool_name, &content) else {
            continue;
        };

        save_original(outputs_dir, "m", round, idx, &content);

        if let Some(obj) = msg.0.as_object_mut() {
            obj.insert("content".to_string(), serde_json::Value::String(compacted));
        }
    }

    if !response_item_indexes.is_empty() {
        let mut values = response_item_indexes
            .iter()
            .filter_map(|idx| messages[*idx].0.get("kaku_responses_output_item").cloned())
            .collect::<Vec<_>>();
        compact_response_values(&mut values, round, outputs_dir);
        for (idx, value) in response_item_indexes.into_iter().zip(values) {
            messages[idx].0["kaku_responses_output_item"] = value;
        }
    }
}

pub(crate) fn micro_compact_response_items(items: &mut [serde_json::Value], round: usize) {
    compact_response_values(items, round, None);
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn raw_responses_tool_outputs_are_compacted() {
        let large = "x".repeat(TOOL_CONTENT_BYTE_CAP + 100);
        let mut items = vec![
            serde_json::json!({
                "type": "function_call",
                "call_id": "call_1",
                "name": "fs_read",
                "arguments": "{}",
            }),
            serde_json::json!({
                "type": "function_call_output",
                "call_id": "call_1",
                "output": large,
            }),
        ];

        micro_compact_response_items(&mut items, 0);

        let compacted = items[1]["output"].as_str().unwrap();
        assert!(compacted.len() <= TOOL_CONTENT_BYTE_CAP);
        assert!(compacted.contains("bytes elided"));
    }

    #[test]
    fn wrapped_responses_tool_outputs_are_compacted() {
        let large = "x".repeat(TOOL_CONTENT_BYTE_CAP + 100);
        let mut messages = vec![
            ApiMessage::responses_output_item(serde_json::json!({
                "type": "function_call",
                "call_id": "call_1",
                "name": "shell_exec",
                "arguments": "{}",
            })),
            ApiMessage::responses_output_item(serde_json::json!({
                "type": "function_call_output",
                "call_id": "call_1",
                "output": large,
            })),
        ];

        micro_compact(&mut messages, 0, None);

        let compacted = messages[1].0["kaku_responses_output_item"]["output"]
            .as_str()
            .unwrap();
        assert!(compacted.len() <= TOOL_CONTENT_BYTE_CAP);
        assert!(compacted.contains("bytes elided"));
    }

    #[test]
    fn fs_read_line_compaction_is_idempotent() {
        let output = (0..=FS_READ_CAP)
            .map(|line| format!("line {line}"))
            .collect::<Vec<_>>()
            .join("\n");
        let mut items = vec![
            serde_json::json!({
                "type": "function_call",
                "call_id": "call_1",
                "name": "fs_read",
                "arguments": "{}",
            }),
            serde_json::json!({
                "type": "function_call_output",
                "call_id": "call_1",
                "output": output,
            }),
        ];

        micro_compact_response_items(&mut items, 0);
        let once = items.clone();
        micro_compact_response_items(&mut items, 1);

        assert_eq!(items, once);
        assert_eq!(
            items[1]["output"].as_str().unwrap().lines().count(),
            FS_READ_CAP
        );
    }
}
