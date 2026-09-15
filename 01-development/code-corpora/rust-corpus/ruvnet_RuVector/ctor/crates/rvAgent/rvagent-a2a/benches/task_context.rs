//! Criterion microbenchmarks for [`TaskContext`] child allocation.
//!
//! Two shapes cover the hot-path cases per ADR-159:
//!
//!  - `task_context_child_depth_1`: `TaskContext::new_root(...)` then one
//!    `.child(...)` call. Baseline per-hop cost.
//!  - `task_context_child_depth_8`: 8 chained `.child()` calls from a root
//!    context. This is the worst-case recursion depth allowed by
//!    `RecursionPolicy::max_call_depth = 8` (ADR-159). Each `.child()`
//!    clones `visited_agents`, so cost grows with depth.
//!
//! Run with `cargo bench -p rvagent-a2a --bench task_context`.
//!
//! ## Results
//! Measured 2026-04-24 on AMD Ryzen 9 9950X 16-Core Processor:
//!   - `task_context_child_depth_1`  median ≈ 186.29 ns / iter
//!     (one `new_root` + one `.child()`), ~5.37 M ops/s.
//!   - `task_context_child_depth_8`  median ≈ 1.018 µs / iter
//!     (one `new_root` + 8 chained `.child()`), ~0.98 M full-chains/s
//!     ≈ 7.85 M per-`.child()` calls/s.
//!
//! First run, so these establish baseline (no regression flag expected).

use criterion::{black_box, criterion_group, criterion_main, Criterion};
use rvagent_a2a::context::TaskContext;
use rvagent_a2a::identity::AgentID;

fn bench_child_depth_1(c: &mut Criterion) {
    c.bench_function("task_context_child_depth_1", |b| {
        let root_id = AgentID("agent-root".to_string());
        let next_id = AgentID("agent-next".to_string());
        b.iter(|| {
            let root = TaskContext::new_root(black_box(root_id.clone()));
            let child = root.child(black_box(next_id.clone()));
            black_box(child);
        });
    });
}

fn bench_child_depth_8(c: &mut Criterion) {
    c.bench_function("task_context_child_depth_8", |b| {
        // Pre-create the 8 successor agent ids so the clones measured
        // inside `.child()` dominate — not the AgentID string alloc.
        let root_id = AgentID("agent-root".to_string());
        let chain: Vec<AgentID> = (0..8).map(|i| AgentID(format!("agent-{i}"))).collect();

        b.iter(|| {
            let mut ctx = TaskContext::new_root(black_box(root_id.clone()));
            for next in &chain {
                ctx = ctx.child(black_box(next.clone()));
            }
            black_box(ctx);
        });
    });
}

criterion_group!(benches, bench_child_depth_1, bench_child_depth_8);
criterion_main!(benches);
