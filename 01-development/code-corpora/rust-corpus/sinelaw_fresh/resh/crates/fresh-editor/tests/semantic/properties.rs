//! Property-based theorems.
//!
//! These tests use proptest to generate `Vec<Action>` and check
//! invariants that should hold for *any* such sequence:
//!   - dispatch is deterministic (same input → same output),
//!   - insert-only sequences are perfectly undoable,
//!   - the action handler never panics on a syntactically valid
//!     action stream.
//!
//! When a generated case fails, proptest shrinks the action list to
//! a minimal counterexample. Because the runners return
//! `Result<(), ScenarioFailure>` and `evaluate_actions` doesn't panic
//! on failure, shrinking works without `catch_unwind`.
//!
//! Property failures are saved to
//! `tests/property_theorem.proptest-regressions` and replayed on
//! subsequent runs (proptest's standard regression-tracking).

use crate::common::scenario::buffer_scenario::{
    check_buffer_scenario, BufferScenario, CursorExpect,
};
use crate::common::scenario::failure::ScenarioFailure;
use crate::common::scenario::property::{
    evaluate_actions, initial_text_strategy, insert_only_action_strategy, safe_action_strategy,
};
use fresh::test_api::Action;
use proptest::prelude::*;

proptest! {
    // Cap at 32 cases: each evaluation spins up a fresh harness with
    // a tempdir and a Buffer, so the per-case cost is real.
    #![proptest_config(ProptestConfig {
        cases: 32,
        max_shrink_iters: 256,
        .. ProptestConfig::default()
    })]

    /// Deterministic dispatch.
    ///
    /// Running the same (initial_text, actions) twice must produce
    /// the same end state. A failure here would mean the editor
    /// carries state across buffer instantiations, which is a
    /// real test-isolation bug.
    ///
    /// Bug fixed: state.rs:462 panicked on the same family of cursor/buffer
    /// desync after MoveLineEnd / SelectLineEnd / DeleteBackward over a
    /// whitespace-only buffer. Regression test at
    /// `regressions::regression_delete_backward_panic_on_whitespace_only_buffer`.
    #[test]
    fn property_dispatch_is_deterministic(
        initial_text in initial_text_strategy(),
        actions in prop::collection::vec(safe_action_strategy(), 0..16),
    ) {
        let a = evaluate_actions(&initial_text, &actions);
        let b = evaluate_actions(&initial_text, &actions);
        prop_assert_eq!(a, b);
    }

    /// Insert-only undo identity.
    ///
    /// For an insert-only action sequence of length N, dispatching
    /// N Undo actions afterward must restore the initial buffer
    /// text exactly. This is the algebraic claim "every typed
    /// character is its own undo unit".
    ///
    /// If this property ever fails, shrinking produces the smallest
    /// insert sequence that doesn't undo cleanly — almost certainly
    /// pointing at a transaction-boundary bug.
    #[test]
    fn property_insert_only_undo_is_identity(
        initial_text in initial_text_strategy(),
        actions in prop::collection::vec(insert_only_action_strategy(), 0..12),
    ) {
        // Borrow the initial_text long enough for the leak workaround
        // not to be needed. We run the check by hand instead of
        // through TraceScenario (which takes &'static str).
        let mut all_actions = actions.clone();
        all_actions.extend((0..actions.len()).map(|_| Action::Undo));

        let final_state = evaluate_actions(&initial_text, &all_actions);
        prop_assert_eq!(
            final_state.buffer_text,
            initial_text,
            "Undo identity violated: {} inserts followed by {} undos did not restore initial text",
            actions.len(),
            actions.len(),
        );
    }

    /// Robustness: arbitrary safe-action sequences don't panic.
    ///
    /// Calls evaluate_actions with up to 24 safe actions. We don't
    /// assert anything about the result — the property is just
    /// "this returns normally". A failure here means the editor
    /// panicked on a path the property generator reached, which is
    /// a real bug regardless of intended behavior.
    ///
    /// Bug fixed: the first run found a real production bug at actions.rs:1613
    /// (smart-dedent bounds check). Regression test at
    /// `regressions::regression_smart_dedent_panic_on_phantom_line`.
    #[test]
    fn property_arbitrary_actions_do_not_panic(
        initial_text in initial_text_strategy(),
        actions in prop::collection::vec(safe_action_strategy(), 0..24),
    ) {
        let _ = evaluate_actions(&initial_text, &actions);
    }
}

// ─────────────────────────────────────────────────────────────────────────
// External-driver demonstration: run check_buffer_scenario in a loop
// over a hand-crafted batch and confirm pass/fail counts. This is what
// a fuzzer or proof-search loop would look like, in miniature.
// ─────────────────────────────────────────────────────────────────────────

#[test]
fn property_check_runner_drives_a_batch_without_panic() {
    let cases: Vec<(BufferScenario, bool /* expect ok */)> = vec![
        (
            BufferScenario {
                description: "case 1: identity".into(),
                initial_text: "hello".into(),
                actions: vec![],
                expected_text: "hello".into(),
                expected_primary: CursorExpect::at(0),
                expected_extra_cursors: vec![],
                expected_selection_text: None,
                ..Default::default()
            },
            true,
        ),
        (
            BufferScenario {
                description: "case 2: typo in expected".into(),
                initial_text: "hello".into(),
                actions: vec![],
                expected_text: "WRONG".into(),
                expected_primary: CursorExpect::at(0),
                expected_extra_cursors: vec![],
                expected_selection_text: None,
                ..Default::default()
            },
            false,
        ),
        (
            BufferScenario {
                description: "case 3: insert + correct end state".into(),
                initial_text: "ab".into(),
                actions: vec![Action::MoveDocumentEnd, Action::InsertChar('c')],
                expected_text: "abc".into(),
                expected_primary: CursorExpect::at(3),
                expected_extra_cursors: vec![],
                expected_selection_text: None,
                ..Default::default()
            },
            true,
        ),
    ];

    let mut report: Vec<(String, Result<(), ScenarioFailure>)> = Vec::new();
    for (scenario, _) in &cases {
        let description = scenario.description.clone();
        report.push((description, check_buffer_scenario(scenario.clone())));
    }

    // Confirm pass/fail counts match our predictions.
    for ((_, expected_ok), (desc, result)) in cases.iter().zip(report.iter()) {
        assert_eq!(
            result.is_ok(),
            *expected_ok,
            "{desc}: expected_ok={expected_ok}, got result={result:?}",
        );
    }
}
