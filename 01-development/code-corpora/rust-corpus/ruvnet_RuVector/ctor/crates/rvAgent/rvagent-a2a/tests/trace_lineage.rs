//! ADR-159 M1 r3 — `trace_lineage.rs`.
//!
//! `TaskContext` is the call-graph primitive the whole r3 observability +
//! recursion story depends on. We check:
//!   1. `new_root` allocates a 32-char hex `trace_id`, depth 0, empty
//!      `visited_agents`.
//!   2. `.child()` preserves `trace_id`, increments `depth`, appends the
//!      parent's `AgentID` to `visited_agents`.
//!   3. `to_metadata` / `from_metadata` round-trips all fields — so the
//!      context survives the A2A wire.

use rvagent_a2a::context::TaskContext;
use rvagent_a2a::identity::{agent_id_from_pubkey, AgentID};

use ed25519_dalek::SigningKey;
use rand_core::OsRng;

fn fresh_id() -> AgentID {
    agent_id_from_pubkey(&SigningKey::generate(&mut OsRng).verifying_key())
}

#[test]
fn new_root_has_depth_zero_and_hex_trace_id() {
    let a = fresh_id();
    let ctx = TaskContext::new_root(a.clone());
    assert_eq!(ctx.depth, 0);
    assert!(ctx.visited_agents.is_empty(), "root has no visited agents");
    assert_eq!(ctx.parent_task_id, None);
    assert_eq!(ctx.root_agent_id, a);
    // W3C-compatible `trace_id` is 16 bytes lowercase hex = 32 chars.
    assert_eq!(ctx.trace_id.len(), 32, "trace_id = {}", ctx.trace_id);
    assert!(ctx
        .trace_id
        .chars()
        .all(|c| c.is_ascii_hexdigit() && !c.is_ascii_uppercase()));
}

#[test]
fn child_increments_depth_and_preserves_trace_id() {
    let a = fresh_id();
    let b = fresh_id();
    let c = fresh_id();
    let root = TaskContext::new_root(a.clone());
    let root_trace = root.trace_id.clone();

    let gen1 = root.child(b.clone());
    assert_eq!(gen1.depth, 1);
    assert_eq!(gen1.trace_id, root_trace);
    assert_eq!(gen1.visited_agents, vec![a.clone()]);

    let gen2 = gen1.child(c);
    assert_eq!(gen2.depth, 2);
    assert_eq!(gen2.trace_id, root_trace);
    assert_eq!(gen2.visited_agents, vec![a, b]);
}

#[test]
fn metadata_roundtrip_preserves_all_fields() {
    // ADR-159 r3 — propagation via `metadata.ruvector.context`.
    let a = fresh_id();
    let b = fresh_id();
    let ctx = TaskContext::new_root(a).child(b);

    let meta = ctx.to_metadata();
    let back = TaskContext::from_metadata(&meta).expect("decode context metadata");

    assert_eq!(back.trace_id, ctx.trace_id);
    assert_eq!(back.depth, ctx.depth);
    assert_eq!(back.root_agent_id, ctx.root_agent_id);
    assert_eq!(back.visited_agents, ctx.visited_agents);
    assert_eq!(back.parent_task_id, ctx.parent_task_id);
}
