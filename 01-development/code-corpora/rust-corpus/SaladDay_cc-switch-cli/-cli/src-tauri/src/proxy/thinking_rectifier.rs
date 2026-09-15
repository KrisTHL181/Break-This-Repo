use serde_json::Value;

use super::types::RectifierConfig;

#[derive(Debug, Clone, Default)]
pub struct RectifyResult {
    pub applied: bool,
    pub removed_thinking_blocks: usize,
    pub removed_redacted_thinking_blocks: usize,
    pub removed_signature_fields: usize,
}

pub fn should_rectify_thinking_signature(
    error_message: Option<&str>,
    config: &RectifierConfig,
) -> bool {
    if !config.enabled || !config.request_thinking_signature {
        return false;
    }

    let Some(message) = error_message else {
        return false;
    };
    let lower = message.to_lowercase();

    if lower.contains("invalid")
        && lower.contains("signature")
        && lower.contains("thinking")
        && lower.contains("block")
    {
        return true;
    }

    if lower.contains("must start with a thinking block") {
        return true;
    }

    if lower.contains("expected")
        && (lower.contains("thinking") || lower.contains("redacted_thinking"))
        && lower.contains("found")
        && lower.contains("tool_use")
    {
        return true;
    }

    if lower.contains("signature") && lower.contains("field required") {
        return true;
    }

    if lower.contains("signature") && lower.contains("extra inputs are not permitted") {
        return true;
    }

    if (lower.contains("thinking") || lower.contains("redacted_thinking"))
        && lower.contains("cannot be modified")
    {
        return true;
    }

    lower.contains("非法请求")
        || lower.contains("illegal request")
        || lower.contains("invalid request")
}

pub fn rectify_anthropic_request(body: &mut Value) -> RectifyResult {
    let mut result = RectifyResult::default();

    let messages = match body.get_mut("messages").and_then(|m| m.as_array_mut()) {
        Some(messages) => messages,
        None => return result,
    };

    for message in messages.iter_mut() {
        let content = match message.get_mut("content").and_then(|c| c.as_array_mut()) {
            Some(content) => content,
            None => continue,
        };

        let mut new_content = Vec::with_capacity(content.len());
        let mut modified = false;

        for block in content.iter() {
            match block.get("type").and_then(|t| t.as_str()) {
                Some("thinking") => {
                    result.removed_thinking_blocks += 1;
                    modified = true;
                    continue;
                }
                Some("redacted_thinking") => {
                    result.removed_redacted_thinking_blocks += 1;
                    modified = true;
                    continue;
                }
                _ => {}
            }

            if block.get("signature").is_some() {
                let mut clone = block.clone();
                if let Some(object) = clone.as_object_mut() {
                    object.remove("signature");
                    result.removed_signature_fields += 1;
                    modified = true;
                }
                new_content.push(clone);
                continue;
            }

            new_content.push(block.clone());
        }

        if modified {
            result.applied = true;
            *content = new_content;
        }
    }

    let messages_snapshot = body
        .get("messages")
        .and_then(|messages| messages.as_array())
        .map(|messages| messages.to_vec())
        .unwrap_or_default();

    if should_remove_top_level_thinking(body, &messages_snapshot) {
        if let Some(object) = body.as_object_mut() {
            object.remove("thinking");
            result.applied = true;
        }
    }

    result
}

fn should_remove_top_level_thinking(body: &Value, messages: &[Value]) -> bool {
    let thinking_enabled = body
        .get("thinking")
        .and_then(|thinking| thinking.get("type"))
        .and_then(|value| value.as_str())
        == Some("enabled");

    if !thinking_enabled {
        return false;
    }

    let last_assistant = messages
        .iter()
        .rev()
        .find(|message| message.get("role").and_then(|role| role.as_str()) == Some("assistant"));

    let Some(last_content) = last_assistant
        .and_then(|message| message.get("content"))
        .and_then(|content| content.as_array())
        .filter(|content| !content.is_empty())
    else {
        return false;
    };

    let first_block_type = last_content
        .first()
        .and_then(|block| block.get("type"))
        .and_then(|value| value.as_str());
    if matches!(
        first_block_type,
        Some("thinking") | Some("redacted_thinking")
    ) {
        return false;
    }

    last_content
        .iter()
        .any(|block| block.get("type").and_then(|value| value.as_str()) == Some("tool_use"))
}

pub fn normalize_thinking_type(body: Value) -> Value {
    body
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    fn enabled_config() -> RectifierConfig {
        RectifierConfig::default()
    }

    #[test]
    fn detects_invalid_signature_errors() {
        assert!(should_rectify_thinking_signature(
            Some("messages.1.content.0: Invalid `signature` in `thinking` block"),
            &enabled_config(),
        ));
    }

    #[test]
    fn removes_legacy_thinking_blocks_and_signatures() {
        let mut body = json!({
            "model": "claude-test",
            "messages": [{
                "role": "assistant",
                "content": [
                    { "type": "thinking", "thinking": "t", "signature": "sig" },
                    { "type": "text", "text": "hello", "signature": "sig_text" },
                    { "type": "redacted_thinking", "data": "r", "signature": "sig_redacted" }
                ]
            }]
        });

        let result = rectify_anthropic_request(&mut body);

        assert!(result.applied);
        assert_eq!(result.removed_thinking_blocks, 1);
        assert_eq!(result.removed_redacted_thinking_blocks, 1);
        assert_eq!(result.removed_signature_fields, 1);
        let content = body["messages"][0]["content"].as_array().unwrap();
        assert_eq!(content.len(), 1);
        assert_eq!(content[0]["type"], "text");
        assert!(content[0].get("signature").is_none());
    }

    #[test]
    fn removes_top_level_thinking_for_tool_use_without_prefix() {
        let mut body = json!({
            "model": "claude-test",
            "thinking": { "type": "enabled", "budget_tokens": 1024 },
            "messages": [{
                "role": "assistant",
                "content": [
                    { "type": "tool_use", "id": "toolu_1", "name": "WebSearch", "input": {} }
                ]
            }]
        });

        let result = rectify_anthropic_request(&mut body);

        assert!(result.applied);
        assert!(body.get("thinking").is_none());
    }
}
