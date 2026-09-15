use serde_json::Value;

use super::types::RectifierConfig;

const MAX_THINKING_BUDGET: u64 = 32000;
const MAX_TOKENS_VALUE: u64 = 64000;
const MIN_MAX_TOKENS_FOR_BUDGET: u64 = MAX_THINKING_BUDGET + 1;

#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct BudgetRectifySnapshot {
    pub max_tokens: Option<u64>,
    pub thinking_type: Option<String>,
    pub thinking_budget_tokens: Option<u64>,
}

#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct BudgetRectifyResult {
    pub applied: bool,
    pub before: BudgetRectifySnapshot,
    pub after: BudgetRectifySnapshot,
}

pub fn should_rectify_thinking_budget(
    error_message: Option<&str>,
    config: &RectifierConfig,
) -> bool {
    if !config.enabled || !config.request_thinking_budget {
        return false;
    }

    let Some(message) = error_message else {
        return false;
    };
    let lower = message.to_lowercase();

    let has_budget_reference = lower.contains("budget_tokens") || lower.contains("budget tokens");
    let has_thinking_reference = lower.contains("thinking");
    let has_1024_constraint = lower.contains("greater than or equal to 1024")
        || lower.contains(">= 1024")
        || (lower.contains("1024") && lower.contains("input should be"));

    has_budget_reference && has_thinking_reference && has_1024_constraint
}

pub fn rectify_thinking_budget(body: &mut Value) -> BudgetRectifyResult {
    let before = snapshot_budget(body);

    if before.thinking_type.as_deref() == Some("adaptive") {
        return BudgetRectifyResult {
            applied: false,
            before: before.clone(),
            after: before,
        };
    }

    if !body.get("thinking").is_some_and(Value::is_object) {
        body["thinking"] = Value::Object(serde_json::Map::new());
    }

    let Some(thinking) = body
        .get_mut("thinking")
        .and_then(|thinking| thinking.as_object_mut())
    else {
        return BudgetRectifyResult {
            applied: false,
            before: before.clone(),
            after: before,
        };
    };

    thinking.insert("type".to_string(), Value::String("enabled".to_string()));
    thinking.insert(
        "budget_tokens".to_string(),
        Value::Number(MAX_THINKING_BUDGET.into()),
    );

    if before.max_tokens.is_none() || before.max_tokens < Some(MIN_MAX_TOKENS_FOR_BUDGET) {
        body["max_tokens"] = Value::Number(MAX_TOKENS_VALUE.into());
    }

    let after = snapshot_budget(body);
    BudgetRectifyResult {
        applied: before != after,
        before,
        after,
    }
}

fn snapshot_budget(body: &Value) -> BudgetRectifySnapshot {
    let max_tokens = body.get("max_tokens").and_then(|value| value.as_u64());
    let thinking = body
        .get("thinking")
        .and_then(|thinking| thinking.as_object());
    let thinking_type = thinking
        .and_then(|thinking| thinking.get("type"))
        .and_then(|value| value.as_str())
        .map(ToString::to_string);
    let thinking_budget_tokens = thinking
        .and_then(|thinking| thinking.get("budget_tokens"))
        .and_then(|value| value.as_u64());

    BudgetRectifySnapshot {
        max_tokens,
        thinking_type,
        thinking_budget_tokens,
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    fn enabled_config() -> RectifierConfig {
        RectifierConfig::default()
    }

    #[test]
    fn detects_budget_constraint_errors() {
        assert!(should_rectify_thinking_budget(
            Some("thinking.budget_tokens: Input should be greater than or equal to 1024"),
            &enabled_config(),
        ));
    }

    #[test]
    fn rectifies_budget_and_max_tokens() {
        let mut body = json!({
            "model": "claude-test",
            "thinking": { "type": "enabled", "budget_tokens": 512 },
            "max_tokens": 1024
        });

        let result = rectify_thinking_budget(&mut body);

        assert!(result.applied);
        assert_eq!(result.before.thinking_budget_tokens, Some(512));
        assert_eq!(
            result.after.thinking_budget_tokens,
            Some(MAX_THINKING_BUDGET)
        );
        assert_eq!(result.after.max_tokens, Some(MAX_TOKENS_VALUE));
        assert_eq!(body["thinking"]["type"], "enabled");
        assert_eq!(body["thinking"]["budget_tokens"], MAX_THINKING_BUDGET);
        assert_eq!(body["max_tokens"], MAX_TOKENS_VALUE);
    }

    #[test]
    fn adaptive_budget_requests_are_left_unchanged() {
        let mut body = json!({
            "model": "claude-test",
            "thinking": { "type": "adaptive", "budget_tokens": 512 },
            "max_tokens": 1024
        });

        let result = rectify_thinking_budget(&mut body);

        assert!(!result.applied);
        assert_eq!(result.before, result.after);
        assert_eq!(body["thinking"]["type"], "adaptive");
        assert_eq!(body["thinking"]["budget_tokens"], 512);
        assert_eq!(body["max_tokens"], 1024);
    }
}
