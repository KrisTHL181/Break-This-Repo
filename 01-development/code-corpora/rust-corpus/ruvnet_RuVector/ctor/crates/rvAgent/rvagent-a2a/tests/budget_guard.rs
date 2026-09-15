//! ADR-159 M1 r3 — `budget_guard.rs`.
//!
//! Drives `BudgetLedger` against both overflow policies documented in
//! ADR-159 "Global budget control":
//!   - `Shed`: first 100 calls admit, 101st rejected with
//!     `BudgetError::Exceeded`.
//!   - `Queue { max_queue_depth: 5 }`: first 100 admit, next 5 enqueue
//!     (still error at the call site — the admission failed), 106th sees
//!     `QueueFull`.

use rvagent_a2a::budget::{BudgetError, BudgetLedger, GlobalBudget, OverflowPolicy};

#[test]
fn shed_admits_up_to_cap_then_rejects() {
    let ledger = BudgetLedger::new(GlobalBudget {
        max_usd_per_minute: Some(1.0),
        max_tokens_per_minute: None,
        max_tasks_per_minute: None,
        overflow: OverflowPolicy::Shed,
    });
    // 0.01 * 100 = 1.00, exactly the cap → all 100 admit.
    for i in 0..100 {
        ledger
            .try_consume(0.01, 100)
            .unwrap_or_else(|e| panic!("consume {} failed: {:?}", i, e));
    }
    // 101st pushes us over.
    match ledger.try_consume(0.01, 100) {
        Err(BudgetError::Exceeded { dim, .. }) => {
            assert_eq!(dim, "usd", "expected usd-dim rejection");
        }
        other => panic!("expected Exceeded(usd), got {:?}", other),
    }
}

#[test]
fn queue_enqueues_up_to_depth_then_queuefull() {
    let ledger = BudgetLedger::new(GlobalBudget {
        max_usd_per_minute: Some(1.0),
        max_tokens_per_minute: None,
        max_tasks_per_minute: None,
        overflow: OverflowPolicy::Queue { max_queue_depth: 5 },
    });
    // 0.01 * 100 = 1.00 → admit all.
    for _ in 0..100 {
        ledger.try_consume(0.01, 100).expect("under cap");
    }
    // Next 5 enqueue. ADR-159 states enqueue-as-rejected-this-window:
    // `reject()` in `budget.rs` returns `Exceeded` while counting up
    // `queue_depth`. Accept either `Exceeded` or `QueueFull` here for
    // the first 5 — both are the enqueue path.
    for i in 0..5 {
        match ledger.try_consume(0.01, 100) {
            Err(BudgetError::Exceeded { .. }) => {}
            Err(BudgetError::QueueFull { .. }) => {
                panic!(
                    "queue declared full at enqueue #{} — depth should accept 5",
                    i + 1
                )
            }
            Ok(()) => panic!(
                "expected admission to fail (over cap), got Ok at enqueue #{}",
                i + 1
            ),
        }
    }
    // 106th: queue is full.
    match ledger.try_consume(0.01, 100) {
        Err(BudgetError::QueueFull { depth }) => assert_eq!(depth, 5),
        other => panic!("expected QueueFull(5), got {:?}", other),
    }
}
