//! ADR-159 M1 r3 — `config_load.rs`.
//!
//! The ADR specifies a TOML config that materializes into a
//! `ChainedSelector + GlobalBudget + TaskPolicy default + RecursionPolicy`.
//! The loader lives at `config::RvAgentA2aConfig::from_path`. This test
//! writes the sample TOML from ADR-159 to a tempfile, loads it, and asserts
//! each field parsed correctly.

use std::io::Write;

use rvagent_a2a::config::RvAgentA2aConfig;
use tempfile::NamedTempFile;

const ADR_SAMPLE_TOML: &str = r#"
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
fn adr159_sample_toml_loads_and_parses_every_field() {
    let mut tmp = NamedTempFile::new().expect("mktemp");
    tmp.write_all(ADR_SAMPLE_TOML.as_bytes())
        .expect("write toml");
    tmp.flush().expect("flush");

    let cfg = RvAgentA2aConfig::from_path(tmp.path()).expect("load toml");

    // Routing block.
    assert_eq!(cfg.routing.default_selector, "cheapest_under_latency");
    assert_eq!(cfg.routing.latency_budget_ms, 2000);
    assert_eq!(cfg.routing.fallback.as_deref(), Some("lowest_latency"));

    // Budget block.
    assert_eq!(cfg.budget.global.max_usd_per_minute, Some(10.0));

    // Policy.default block.
    assert_eq!(cfg.policy.default.max_cost_usd, Some(0.05));
    assert_eq!(cfg.policy.default.max_duration_ms, Some(30_000));
    let allowed = cfg
        .policy
        .default
        .allowed_skills
        .as_ref()
        .expect("allowed_skills set");
    assert!(allowed.iter().any(|s| s == "rag.query"));
    assert!(allowed.iter().any(|s| s == "embed.vectorize"));
    assert_eq!(allowed.len(), 2);

    // Recursion block.
    assert_eq!(cfg.recursion.max_call_depth, 8);
    assert!(cfg.recursion.deny_revisit);
}
