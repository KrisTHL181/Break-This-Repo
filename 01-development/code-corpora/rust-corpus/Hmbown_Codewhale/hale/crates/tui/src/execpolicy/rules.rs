//! Execpolicy rules loaded from TOML configuration.

use std::collections::BTreeMap;
use std::path::{Path, PathBuf};

use anyhow::{Context, Result};
use serde::Deserialize;

use super::matcher::pattern_matches;
use codewhale_execpolicy::command_safety::prefix_allow_matches;

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ExecPolicyDecision {
    Allow,
    Deny(String),
    AskUser(String),
}

#[derive(Debug, Deserialize, Default)]
pub struct ExecPolicyConfig {
    #[serde(default)]
    pub rules: BTreeMap<String, RuleSet>,
}

#[derive(Debug, Deserialize, Default)]
pub struct RuleSet {
    #[serde(default)]
    pub allow: Vec<String>,
    #[serde(default)]
    pub deny: Vec<String>,
}

impl ExecPolicyConfig {
    pub fn from_str(contents: &str) -> Result<Self> {
        toml::from_str(contents).context("failed to parse execpolicy.toml")
    }

    pub fn from_path(path: &Path) -> Result<Self> {
        let contents = std::fs::read_to_string(path)
            .with_context(|| format!("failed to read execpolicy file {}", path.display()))?;
        Self::from_str(&contents)
    }

    pub fn evaluate(&self, command: &str) -> ExecPolicyDecision {
        // #security: a deny pattern has to be matched against the commands the
        // shell would actually run, not against the text as written. Quoting,
        // command substitution (`` `cmd` ``, `$(cmd)`), grouping, chaining and
        // wrapper payloads (`bash -c …`, `eval …`, `sudo …`) all produce an
        // invocation whose text differs from the rule while its effect does
        // not. `expanded_commands` word-splits the way a shell does and returns
        // every command line involved, so one deny pattern covers all of the
        // spellings instead of one string pattern per metacharacter.
        //
        // Only the deny loop is widened. The allow loop below still matches the
        // command as written, so a broader expansion can never turn into a
        // broader auto-approval.
        let deny_targets = codewhale_execpolicy::shell_expand::expanded_commands(command);
        for (group, rules) in &self.rules {
            for pattern in &rules.deny {
                if deny_targets
                    .iter()
                    .any(|target| pattern_matches(pattern, target))
                {
                    return ExecPolicyDecision::Deny(format!(
                        "execpolicy denied by {group}: {pattern}"
                    ));
                }
            }
        }

        for (group, rules) in &self.rules {
            for pattern in &rules.allow {
                // Allow rules use arity-aware prefix matching first so that
                // `allow = ["git status"]` matches `git status -s` but NOT
                // `git push origin main`.  Fall back to regex-style
                // `pattern_matches` for wildcard patterns (e.g. `cargo *`).
                if prefix_allow_matches(pattern, command) || pattern_matches(pattern, command) {
                    let _ = group;
                    return ExecPolicyDecision::Allow;
                }
            }
        }

        ExecPolicyDecision::AskUser("execpolicy: no matching allow rule".to_string())
    }
}

pub fn default_execpolicy_path() -> Option<PathBuf> {
    crate::config::effective_home_dir().map(|home| home.join(".deepseek").join("execpolicy.toml"))
}

pub fn load_default_policy() -> Result<Option<ExecPolicyConfig>> {
    let Some(path) = default_execpolicy_path() else {
        return Ok(None);
    };
    if !path.exists() {
        return Ok(None);
    }
    ExecPolicyConfig::from_path(&path).map(Some)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_execpolicy_evaluate() {
        let config = ExecPolicyConfig {
            rules: BTreeMap::from([
                (
                    "git".to_string(),
                    RuleSet {
                        allow: vec!["git status".to_string(), "git log *".to_string()],
                        deny: vec!["git push --force".to_string()],
                    },
                ),
                (
                    "danger".to_string(),
                    RuleSet {
                        allow: vec![],
                        deny: vec!["rm -rf /".to_string()],
                    },
                ),
            ]),
        };

        assert!(matches!(
            config.evaluate("git status"),
            ExecPolicyDecision::Allow
        ));
        assert!(matches!(
            config.evaluate("git log --oneline"),
            ExecPolicyDecision::Allow
        ));
        assert!(matches!(
            config.evaluate("git push --force"),
            ExecPolicyDecision::Deny(_)
        ));
        assert!(matches!(
            config.evaluate("unknown command"),
            ExecPolicyDecision::AskUser(_)
        ));
    }

    #[test]
    fn test_prefix_rule_allows_git_status_with_flags() {
        // Arity-aware: `allow = ["git status"]` must match `git status -s`.
        let config = ExecPolicyConfig {
            rules: BTreeMap::from([(
                "git".to_string(),
                RuleSet {
                    allow: vec!["git status".to_string()],
                    deny: vec![],
                },
            )]),
        };

        assert!(matches!(
            config.evaluate("git status -s"),
            ExecPolicyDecision::Allow
        ));
        assert!(matches!(
            config.evaluate("git status --porcelain"),
            ExecPolicyDecision::Allow
        ));
        // Push must NOT match the "git status" allow rule.
        assert!(matches!(
            config.evaluate("git push origin main"),
            ExecPolicyDecision::AskUser(_)
        ));
    }

    fn danger_policy() -> ExecPolicyConfig {
        ExecPolicyConfig {
            rules: BTreeMap::from([(
                "danger".to_string(),
                RuleSet {
                    allow: vec!["echo *".to_string()],
                    deny: vec!["rm -rf /".to_string()],
                },
            )]),
        }
    }

    /// #security: the deny pattern must survive every way a shell can spell the
    /// command it names. A whole-string match saw only the text as typed.
    #[test]
    fn deny_pattern_covers_every_shell_spelling() {
        let config = danger_policy();
        let mut evaded = Vec::new();
        for command in [
            "rm -rf /",
            "ls && rm -rf /",
            "ls & rm -rf /",
            "true; rm -rf /",
            "ls | rm -rf /",
            "ls\nrm -rf /",
            "(rm -rf /)",
            "{ rm -rf /; }",
            "`rm -rf /`",
            "echo `rm -rf /`",
            "echo \"`rm -rf /`\"",
            "$(rm -rf /)",
            "echo $(rm -rf /)",
            "x=$(rm -rf /)",
            "diff <(rm -rf /) b",
            "rm -rf \"/\"",
            "rm -rf '/'",
            "eval 'rm -rf /'",
            "bash -c 'rm -rf /'",
            "sh -lc \"rm -rf /\"",
            "sudo rm -rf /",
            "env rm -rf /",
            "timeout 5 rm -rf /",
            "xargs rm -rf /",
        ] {
            if !matches!(config.evaluate(command), ExecPolicyDecision::Deny(_)) {
                evaded.push(command);
            }
        }
        assert!(evaded.is_empty(), "deny pattern bypassed by: {evaded:#?}");
    }

    /// The fix must not deny a command merely for containing a metacharacter.
    #[test]
    fn deny_pattern_leaves_harmless_metacharacter_uses_alone() {
        let config = danger_policy();
        for command in [
            // Substitution of something the rule does not name.
            "echo \"built at $(date)\"",
            "echo `date`",
            // Single quotes are literal: this prints the text, runs nothing.
            "echo '`rm -rf /`'",
            "echo 'rm -rf /'",
        ] {
            assert!(
                !matches!(config.evaluate(command), ExecPolicyDecision::Deny(_)),
                "harmless command wrongly denied: {command:?}"
            );
        }
    }

    #[test]
    fn test_prefix_rule_allows_cargo_check_variants() {
        let config = ExecPolicyConfig {
            rules: BTreeMap::from([(
                "cargo".to_string(),
                RuleSet {
                    allow: vec!["cargo check".to_string()],
                    deny: vec![],
                },
            )]),
        };

        assert!(matches!(
            config.evaluate("cargo check"),
            ExecPolicyDecision::Allow
        ));
        assert!(matches!(
            config.evaluate("cargo check --workspace"),
            ExecPolicyDecision::Allow
        ));
        assert!(matches!(
            config.evaluate("cargo build --release"),
            ExecPolicyDecision::AskUser(_)
        ));
    }
}
