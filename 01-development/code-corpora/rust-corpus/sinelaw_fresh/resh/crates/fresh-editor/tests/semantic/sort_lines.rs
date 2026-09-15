//! Track B migration: rewrites of `tests/e2e/sort_lines.rs` as
//! declarative theorems.
//!
//! Notable: the original tests invoke "sort lines" through the
//! command palette (Ctrl+P → type → Enter). The semantic action
//! `Action::SortLines` exists and bypasses the palette entirely, so
//! the theorem version is dramatically shorter — it tests the
//! transformation, not the palette UX.

use crate::common::scenario::buffer_scenario::{
    assert_buffer_scenario, BufferScenario, CursorExpect,
};
use crate::common::scenario::trace_scenario::{assert_trace_scenario, TraceScenario};
use fresh::test_api::Action;

#[test]
fn theorem_sort_lines_basic_alphabetical() {
    // Replaces tests/e2e/sort_lines.rs::test_sort_lines_basic.
    //
    // FINDING: when SortLines actually mutates text, the selection
    // is cleared (position 19, anchor None). The original imperative
    // test was silent about this. The companion theorem
    // `theorem_sort_lines_already_sorted_is_idempotent` shows that
    // when SortLines is a no-op, the selection is preserved — an
    // asymmetry pinned down by the declarative form.
    assert_buffer_scenario(BufferScenario {
        description: "SelectAll + SortLines orders three lines alphabetically".into(),
        initial_text: "cherry\napple\nbanana".into(),
        actions: vec![Action::SelectAll, Action::SortLines],
        expected_text: "apple\nbanana\ncherry".into(),
        expected_primary: CursorExpect::at(19),
        expected_extra_cursors: vec![],
        expected_selection_text: Some("".into()),
        ..Default::default()
    });
}

#[test]
fn theorem_sort_lines_already_sorted_is_idempotent() {
    // Replaces tests/e2e/sort_lines.rs::test_sort_lines_already_sorted.
    // See finding in `theorem_sort_lines_basic_alphabetical`: the
    // selection is preserved here because SortLines is a no-op.
    assert_buffer_scenario(BufferScenario {
        description: "SortLines on sorted input is idempotent and preserves the selection".into(),
        initial_text: "apple\nbanana\ncherry".into(),
        actions: vec![Action::SelectAll, Action::SortLines],
        expected_text: "apple\nbanana\ncherry".into(),
        expected_primary: CursorExpect::range(0, 19),
        expected_extra_cursors: vec![],
        expected_selection_text: Some("apple\nbanana\ncherry".into()),
        ..Default::default()
    });
}

#[test]
fn theorem_sort_lines_undo_restores_original_order() {
    // Replaces tests/e2e/sort_lines.rs::test_sort_lines_undo.
    // Forward: select all, sort. Reverse: one undo restores order.
    // SortLines is one transactional unit, so undo_count = 1
    // (not "one undo per line").
    assert_trace_scenario(TraceScenario {
        description: "SortLines is a single undo unit — one Undo restores the input".into(),
        initial_text: "cherry\napple\nbanana".into(),
        actions: vec![Action::SelectAll, Action::SortLines],
        expected_text: "apple\nbanana\ncherry".into(),
        undo_count: 1,
    });
}
