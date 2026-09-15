use serde_json::Value;

const APPROX_CHARS_PER_TOKEN: u64 = 4;

pub(crate) fn estimate_tokens_from_value(value: &Value) -> u64 {
    serde_json::to_string(value)
        .ok()
        .map(|json| estimate_tokens_from_text(&json))
        .unwrap_or(0)
}

pub(crate) fn estimate_tokens_from_text(text: &str) -> u64 {
    estimate_tokens_from_char_count(text.chars().count() as u64)
}

pub(crate) fn estimate_tokens_from_bytes(bytes: &[u8]) -> u64 {
    estimate_tokens_from_text(&String::from_utf8_lossy(bytes))
}

pub(crate) fn estimate_tokens_from_char_count(char_count: u64) -> u64 {
    if char_count == 0 {
        0
    } else {
        char_count.div_ceil(APPROX_CHARS_PER_TOKEN)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    #[test]
    fn estimate_tokens_from_char_count_rounds_up() {
        assert_eq!(estimate_tokens_from_char_count(0), 0);
        assert_eq!(estimate_tokens_from_char_count(1), 1);
        assert_eq!(estimate_tokens_from_char_count(4), 1);
        assert_eq!(estimate_tokens_from_char_count(5), 2);
    }

    #[test]
    fn estimate_tokens_from_value_counts_serialized_payload() {
        let value = json!({"message": "hello world"});

        assert!(estimate_tokens_from_value(&value) > 0);
    }
}
