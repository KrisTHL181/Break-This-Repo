//! ADR-159 M1 r3 + M3 r3 — `recursion_guard.rs`.
//!
//! Per ADR-159 "Recursion guard":
//!   - `depth == max_call_depth` admits (boundary inclusive).
//!   - `depth > max_call_depth` → `RecursionError::MaxDepthExceeded`.
//!   - Target ∈ visited + `deny_revisit = true` → `RecursionError::Revisit`.
//!   - Target in `revisit_allowlist` → admits even if visited.

use rvagent_a2a::context::TaskContext;
use rvagent_a2a::identity::{agent_id_from_pubkey, AgentID};
use rvagent_a2a::recursion_guard::{check, RecursionError, RecursionPolicy};

use ed25519_dalek::SigningKey;
use rand_core::OsRng;

fn fresh_id() -> AgentID {
    agent_id_from_pubkey(&SigningKey::generate(&mut OsRng).verifying_key())
}

/// Construct a `TaskContext` at a given depth with a given visited chain.
/// The public API only exposes `new_root` + `.child()`; we synthesize depth
/// by building the chain explicitly. This mirrors how a real dispatch arrives
/// — each hop is appended by its caller.
fn ctx_at_depth(depth: u32, root: AgentID, visited: &[AgentID]) -> TaskContext {
    let mut c = TaskContext::new_root(root);
    // Replace the generated depth/visited via metadata round-trip — these
    // are the same shape a real inbound request would carry.
    let mut meta = c.to_metadata();
    meta["depth"] = serde_json::json!(depth);
    meta["visited_agents"] = serde_json::Value::Array(
        visited
            .iter()
            .map(|id| serde_json::json!(id.to_string()))
            .collect(),
    );
    c = TaskContext::from_metadata(&meta).expect("rebuilt ctx");
    c
}

#[test]
fn admits_at_max_depth_boundary() {
    let a = fresh_id();
    let t = fresh_id();
    let policy = RecursionPolicy {
        max_call_depth: 8,
        deny_revisit: true,
        revisit_allowlist: vec![],
    };
    let ctx = ctx_at_depth(8, a, &[]);
    assert!(
        check(&policy, &ctx, t).is_ok(),
        "depth=8 with max=8 must admit (boundary inclusive per ADR-159)"
    );
}

#[test]
fn rejects_above_max_depth() {
    let a = fresh_id();
    let t = fresh_id();
    let policy = RecursionPolicy {
        max_call_depth: 8,
        deny_revisit: true,
        revisit_allowlist: vec![],
    };
    let ctx = ctx_at_depth(9, a, &[]);
    match check(&policy, &ctx, t) {
        Err(RecursionError::MaxDepthExceeded { .. }) => {}
        other => panic!("expected MaxDepthExceeded, got {:?}", other),
    }
}

#[test]
fn rejects_revisit_when_deny_revisit_is_true() {
    let a = fresh_id();
    let b = fresh_id();
    let c = fresh_id();
    let policy = RecursionPolicy {
        max_call_depth: 8,
        deny_revisit: true,
        revisit_allowlist: vec![],
    };
    // visited = [A, B, C], target = A → cycle.
    let ctx = ctx_at_depth(3, a.clone(), &[a.clone(), b, c]);
    match check(&policy, &ctx, a) {
        Err(RecursionError::Revisit { .. }) => {}
        other => panic!("expected Revisit, got {:?}", other),
    }
}

#[test]
fn allowlist_exempts_agent_from_revisit_check() {
    let a = fresh_id();
    let b = fresh_id();
    let c = fresh_id();
    let policy = RecursionPolicy {
        max_call_depth: 8,
        deny_revisit: true,
        revisit_allowlist: vec![a.clone()],
    };
    let ctx = ctx_at_depth(3, a.clone(), &[a.clone(), b, c]);
    assert!(
        check(&policy, &ctx, a).is_ok(),
        "agent on revisit_allowlist must be admitted even when visited"
    );
}
