use serde_json::Value;

pub(super) fn summarize_upstream_body_bytes(body: &[u8]) -> Option<String> {
    std::str::from_utf8(body)
        .ok()
        .map(summarize_upstream_body)
        .filter(|summary| !summary.is_empty())
}

pub(super) fn summarize_upstream_json_value(body: &Value) -> Option<String> {
    if let Some(message) = extract_json_error_message(body) {
        return Some(summarize_text_for_log(&message, 180));
    }

    serde_json::to_string(body)
        .ok()
        .map(|compact_json| summarize_text_for_log(&compact_json, 180))
        .filter(|summary| !summary.is_empty())
}

fn summarize_upstream_body(body: &str) -> String {
    if let Ok(json_body) = serde_json::from_str::<Value>(body) {
        if let Some(summary) = summarize_upstream_json_value(&json_body) {
            return summary;
        }
    }

    summarize_text_for_log(body, 180)
}

fn extract_json_error_message(body: &Value) -> Option<String> {
    [
        body.pointer("/error/message"),
        body.pointer("/message"),
        body.pointer("/detail"),
        body.pointer("/error"),
    ]
    .into_iter()
    .flatten()
    .find_map(|value| value.as_str().map(ToString::to_string))
}

fn summarize_text_for_log(text: &str, max_chars: usize) -> String {
    let normalized = text.split_whitespace().collect::<Vec<_>>().join(" ");
    let trimmed = normalized.trim();

    if trimmed.chars().count() <= max_chars {
        return trimmed.to_string();
    }

    let truncated: String = trimmed.chars().take(max_chars).collect();
    let truncated = truncated.trim_end();
    format!("{truncated}...")
}
