//! ADR-159 M3 r3 — `dispatch_order.rs`.
//!
//! Invariant (ADR-159 r3, "Dispatch ordering is fixed"):
//! > `GlobalBudget → RecursionPolicy → PeerSelector → TaskPolicy → Remote/Local runner`
//!
//! A task dispatched when the budget is already saturated must fail with
//! `BudgetError::Exceeded` *before* `PeerSelector::pick` is invoked — the
//! router must never see shed-able tasks. We prove this with a mock
//! selector whose `pick` panics; the budget check short-circuits and the
//! panic never fires.

use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::Arc;

use rvagent_a2a::budget::{BudgetError, BudgetLedger, GlobalBudget, OverflowPolicy};
use rvagent_a2a::identity::{agent_id_from_pubkey, AgentID};
use rvagent_a2a::recursion_guard::{check as recursion_check, RecursionError, RecursionPolicy};
use rvagent_a2a::routing::{PeerSelector, PeerSnapshot};

use ed25519_dalek::SigningKey;
use rand_core::OsRng;

fn fresh_id() -> AgentID {
    agent_id_from_pubkey(&SigningKey::generate(&mut OsRng).verifying_key())
}

/// A `PeerSelector` that records every call it receives. If `panic_on_call`
/// is true it panics — used to prove earlier gates short-circuited.
struct InstrumentedSelector {
    called: Arc<AtomicBool>,
    panic_on_call: bool,
}

impl PeerSelector for InstrumentedSelector {
    fn pick<'a>(
        &self,
        _peers: &'a [PeerSnapshot],
        _skill: &str,
        _latency_budget_ms: Option<u64>,
    ) -> Option<&'a PeerSnapshot> {
        self.called.store(true, Ordering::SeqCst);
        if self.panic_on_call {
            panic!("PeerSelector::pick invoked — dispatch-order invariant violated");
        }
        None
    }
    fn name(&self) -> &'static str {
        "instrumented"
    }
}

/// Test-side dispatcher mimicking the production ordering documented in
/// ADR-159 §r2+r3 Routing layer. The real dispatcher lives in
/// `src/server.rs`; this helper exists only so the invariant can be
/// asserted independently of the server's lifecycle.
#[derive(Debug)]
#[allow(dead_code)] // `Recursion(_)` is carried for forensics in the test-side dispatcher.
enum DispatchError {
    Budget(BudgetError),
    Recursion(RecursionError),
    NoPeer,
}

struct DispatchArgs<'a> {
    budget: &'a BudgetLedger,
    recursion: &'a RecursionPolicy,
    selector: &'a dyn PeerSelector,
    pool: &'a [PeerSnapshot],
    ctx: &'a rvagent_a2a::context::TaskContext,
    target: AgentID,
    skill: &'a str,
    est_cost_usd: f64,
    est_tokens: u64,
    latency_budget_ms: Option<u64>,
}

fn dispatch(args: DispatchArgs<'_>) -> Result<(), DispatchError> {
    // 1. GlobalBudget.
    args.budget
        .try_consume(args.est_cost_usd, args.est_tokens)
        .map_err(DispatchError::Budget)?;

    // 2. RecursionPolicy.
    recursion_check(args.recursion, args.ctx, args.target).map_err(DispatchError::Recursion)?;

    // 3. PeerSelector.
    if args
        .selector
        .pick(args.pool, args.skill, args.latency_budget_ms)
        .is_none()
    {
        return Err(DispatchError::NoPeer);
    }
    Ok(())
}

#[test]
fn budget_exhaustion_short_circuits_before_selector_runs() {
    // Budget allows exactly $1.00 per minute, Shed policy.
    let budget = BudgetLedger::new(GlobalBudget {
        max_usd_per_minute: Some(1.0),
        overflow: OverflowPolicy::Shed,
        ..Default::default()
    });
    // Saturate the ledger at the cap.
    budget.try_consume(1.0, 0).expect("fills cap exactly");

    // Recursion policy is permissive — the failure must originate at the
    // budget stage, not here.
    let recursion = RecursionPolicy {
        max_call_depth: 1024,
        deny_revisit: false,
        revisit_allowlist: vec![],
    };

    // A selector that panics if ever called.
    let called = Arc::new(AtomicBool::new(false));
    let selector = InstrumentedSelector {
        called: called.clone(),
        panic_on_call: true,
    };

    let root = fresh_id();
    let target = fresh_id();
    let ctx = rvagent_a2a::context::TaskContext::new_root(root.clone());
    let pool: Vec<PeerSnapshot> = vec![]; // content irrelevant — selector must not run

    let err = dispatch(DispatchArgs {
        budget: &budget,
        recursion: &recursion,
        selector: &selector,
        pool: &pool,
        ctx: &ctx,
        target,
        skill: "rag.query",
        est_cost_usd: 0.50,
        est_tokens: 0,
        latency_budget_ms: Some(500),
    })
    .expect_err("dispatch must fail");

    match err {
        DispatchError::Budget(BudgetError::Exceeded { .. }) => {}
        other => panic!("expected Budget(Exceeded), got {:?}", other),
    }
    assert!(
        !called.load(Ordering::SeqCst),
        "PeerSelector::pick ran before BudgetError — dispatch-order invariant violated"
    );
}
