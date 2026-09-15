//! r3 — Operator-facing TOML config.
//!
//! Loads [`RvAgentA2aConfig`] from a TOML file at startup and materializes
//! it into the runtime's [`GlobalBudget`] / [`TaskPolicy`] /
//! [`RecursionPolicy`] defaults plus an opaque [`RoutingConfig`] consumed
//! by `crate::routing`.
//!
//! See ADR-159 "r3 — Operator-facing config" and the M1 r3 foundations
//! scope bullet.
//!
//! [`TaskPolicy`]: crate::policy::TaskPolicy

use std::path::Path;

use serde::{Deserialize, Serialize};
use thiserror::Error;

use crate::budget::GlobalBudget;
use crate::policy::TaskPolicy;
use crate::recursion_guard::RecursionPolicy;

/// Top-level TOML schema for `rvagent-a2a`. Every section is defaulted so
/// an operator can enable only the pieces they need while consumers can
/// rely on the full shape being present.
#[derive(Clone, Debug, Default, Serialize, Deserialize)]
pub struct RvAgentA2aConfig {
    #[serde(default)]
    pub routing: RoutingConfig,

    #[serde(default)]
    pub budget: BudgetSection,

    #[serde(default)]
    pub policy: PolicySection,

    #[serde(default)]
    pub recursion: RecursionPolicy,
}

/// Routing section — the routing module is the authoritative consumer of
/// [`RoutingConfig::selectors`]; this crate only parses and forwards.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct RoutingConfig {
    #[serde(default = "default_selector")]
    pub default_selector: String,

    #[serde(default = "default_latency_budget_ms")]
    pub latency_budget_ms: u64,

    #[serde(default)]
    pub fallback: Option<String>,

    /// Opaque per-selector config blobs. Each entry is a freeform TOML
    /// table the routing module down-casts based on `kind`.
    #[serde(default)]
    pub selectors: Vec<SelectorConfig>,

    /// Seed list of peers consumed at server startup (ADR-159 M3).
    /// Each entry is discovered via `A2aClient::fetch_card` and inserted
    /// into the [`crate::routing::PeerRegistry`]. Empty = no routing;
    /// the server falls through to its local executor unchanged.
    #[serde(default)]
    pub peers: Vec<PeerEntryConfig>,
}

/// A single peer entry in `[routing.peers]` — the base URL and an
/// optional `verify_card` strict-mode flag. `verify_card = false` (the
/// bootstrap default) accepts unsigned cards; `true` rejects them.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct PeerEntryConfig {
    /// Base URL of the peer — e.g. `http://127.0.0.1:18001`. The
    /// `/.well-known/agent.json` path is appended at discovery time.
    pub url: String,

    /// When `Some(true)`, strict signature verification is required for
    /// this peer's AgentCard. Defaults to `None` (lax parity with the
    /// `A2aClient` bootstrap default).
    #[serde(default)]
    pub verify_card: Option<bool>,
}

impl Default for RoutingConfig {
    fn default() -> Self {
        Self {
            default_selector: default_selector(),
            latency_budget_ms: default_latency_budget_ms(),
            fallback: None,
            selectors: Vec::new(),
            peers: Vec::new(),
        }
    }
}

fn default_selector() -> String {
    "cheapest_under_latency".into()
}
fn default_latency_budget_ms() -> u64 {
    2000
}

/// Wrapper for `[budget.global]` to match ADR-159 r3 example TOML.
#[derive(Clone, Debug, Default, Serialize, Deserialize)]
pub struct BudgetSection {
    #[serde(default)]
    pub global: GlobalBudget,
}

/// Wrapper for `[policy.default]` to match ADR-159 r3 example TOML.
#[derive(Clone, Debug, Default, Serialize, Deserialize)]
pub struct PolicySection {
    #[serde(default)]
    pub default: TaskPolicy,
}

/// Opaque selector config — freeform `serde_json::Value` map forwarded to
/// `crate::routing` for interpretation.
#[derive(Clone, Debug, Serialize, Deserialize)]
#[serde(transparent)]
pub struct SelectorConfig(pub serde_json::Value);

#[derive(Debug, Error)]
pub enum ConfigError {
    #[error("io error reading config: {0}")]
    Io(#[from] std::io::Error),

    #[error("toml parse error: {0}")]
    Parse(#[from] toml::de::Error),

    #[error("invalid config: {0}")]
    Invalid(String),
}

impl RvAgentA2aConfig {
    /// Read and parse a TOML config from disk.
    #[tracing::instrument(skip_all, fields(path = %path.as_ref().display()))]
    pub fn from_path(path: impl AsRef<Path>) -> Result<Self, ConfigError> {
        let raw = std::fs::read_to_string(path.as_ref())?;
        Self::from_toml_str(&raw)
    }

    /// Parse TOML directly from a string.
    pub fn from_toml_str(s: &str) -> Result<Self, ConfigError> {
        let cfg: Self = toml::from_str(s)?;
        cfg.validate()?;
        Ok(cfg)
    }

    fn validate(&self) -> Result<(), ConfigError> {
        if self.recursion.max_call_depth == 0 {
            return Err(ConfigError::Invalid(
                "recursion.max_call_depth must be >= 1".into(),
            ));
        }
        if let Some(u) = self.budget.global.max_usd_per_minute {
            if u < 0.0 {
                return Err(ConfigError::Invalid(
                    "budget.global.max_usd_per_minute must be >= 0".into(),
                ));
            }
        }
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const SAMPLE: &str = r#"
        [routing]
        default_selector = "cheapest_under_latency"
        latency_budget_ms = 2000
        fallback = "lowest_latency"

        [budget.global]
        max_usd_per_minute = 10.0
        overflow = "shed"

        [policy.default]
        max_cost_usd = 0.05
        max_duration_ms = 30000
        allowed_skills = ["rag.query", "embed.vectorize"]

        [recursion]
        max_call_depth = 8
        deny_revisit = true
    "#;

    #[test]
    fn parses_sample_config() {
        let cfg = RvAgentA2aConfig::from_toml_str(SAMPLE).expect("parses");
        assert_eq!(cfg.routing.default_selector, "cheapest_under_latency");
        assert_eq!(cfg.routing.latency_budget_ms, 2000);
        assert_eq!(cfg.routing.fallback.as_deref(), Some("lowest_latency"));
        assert_eq!(cfg.budget.global.max_usd_per_minute, Some(10.0));
        assert_eq!(cfg.recursion.max_call_depth, 8);
    }

    #[test]
    fn empty_config_is_ok_with_defaults() {
        let cfg = RvAgentA2aConfig::from_toml_str("").expect("empty parses");
        assert_eq!(cfg.routing.default_selector, "cheapest_under_latency");
    }

    #[test]
    fn invalid_depth_rejected() {
        let bad = r#"
            [recursion]
            max_call_depth = 0
            deny_revisit = true
        "#;
        let err = RvAgentA2aConfig::from_toml_str(bad).unwrap_err();
        assert!(matches!(err, ConfigError::Invalid(_)));
    }

    #[test]
    fn malformed_toml_surfaces_parse_error() {
        let bad = "this = is = not = toml";
        let err = RvAgentA2aConfig::from_toml_str(bad).unwrap_err();
        assert!(matches!(err, ConfigError::Parse(_)));
    }

    #[test]
    fn from_path_reads_and_parses() {
        let dir = tempfile::tempdir().expect("tempdir");
        let p = dir.path().join("a2a.toml");
        std::fs::write(&p, SAMPLE).expect("write");
        let cfg = RvAgentA2aConfig::from_path(&p).expect("from_path");
        assert_eq!(cfg.routing.default_selector, "cheapest_under_latency");
    }

    #[test]
    fn from_path_missing_file_is_io_error() {
        let err = RvAgentA2aConfig::from_path("/no/such/config.toml").unwrap_err();
        assert!(matches!(err, ConfigError::Io(_)));
    }

    #[test]
    fn parses_routing_peers_list() {
        let s = r#"
            [routing]
            default_selector = "cheapest_under_latency"
            latency_budget_ms = 2000

            [[routing.peers]]
            url = "http://127.0.0.1:18001"

            [[routing.peers]]
            url = "http://127.0.0.1:18002"
            verify_card = true
        "#;
        let cfg = RvAgentA2aConfig::from_toml_str(s).expect("parses");
        assert_eq!(cfg.routing.peers.len(), 2);
        assert_eq!(cfg.routing.peers[0].url, "http://127.0.0.1:18001");
        assert_eq!(cfg.routing.peers[0].verify_card, None);
        assert_eq!(cfg.routing.peers[1].url, "http://127.0.0.1:18002");
        assert_eq!(cfg.routing.peers[1].verify_card, Some(true));
    }

    #[test]
    fn empty_routing_has_empty_peers() {
        let cfg = RvAgentA2aConfig::from_toml_str("").expect("empty parses");
        assert!(cfg.routing.peers.is_empty());
    }
}
