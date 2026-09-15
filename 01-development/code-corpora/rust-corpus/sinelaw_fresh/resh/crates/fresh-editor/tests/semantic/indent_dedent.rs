//! Track B migration of `tests/e2e/indent_dedent.rs`.
//!
//! The original tests are heavily keymap-coupled — they assert on
//! the user pressing `Tab` and `Shift+Tab`. The semantic actions
//! `Action::InsertTab` and `Action::DedentSelection` capture the same
//! intent without naming a keystroke; if we ever change the
//! Shift+Tab binding the theorems below stay valid.

use crate::common::scenario::buffer_scenario::{
    assert_buffer_scenario, BufferScenario, CursorExpect,
};
use fresh::test_api::Action;

#[test]
fn theorem_dedent_selection_removes_leading_indent() {
    // Replaces tests/e2e/indent_dedent.rs::test_shift_tab_dedent_single_line_spaces.
    // Initial: "    Hello world" with cursor at byte 0 (after load).
    // DedentSelection on the current line removes one tab-stop (4 spaces).
    assert_buffer_scenario(BufferScenario {
        description: "DedentSelection removes leading 4 spaces from the cursor's line".into(),
        initial_text: "    Hello world".into(),
        actions: vec![Action::DedentSelection],
        expected_text: "Hello world".into(),
        expected_primary: CursorExpect::at(0),
        expected_extra_cursors: vec![],
        expected_selection_text: Some("".into()),
        ..Default::default()
    });
}

#[test]
fn theorem_dedent_selection_partial_indent_removes_all_leading_spaces() {
    // Replaces tests/e2e/indent_dedent.rs::test_shift_tab_dedent_fewer_spaces.
    // Initial: "  Hello world" — only 2 leading spaces (less than
    // tab-stop). Dedent should still remove all of them.
    assert_buffer_scenario(BufferScenario {
        description: "DedentSelection on <tab-stop indent removes all leading spaces".into(),
        initial_text: "  Hello world".into(),
        actions: vec![Action::DedentSelection],
        expected_text: "Hello world".into(),
        expected_primary: CursorExpect::at(0),
        expected_extra_cursors: vec![],
        expected_selection_text: Some("".into()),
        ..Default::default()
    });
}

#[test]
fn theorem_dedent_selection_no_indent_is_idempotent() {
    // Replaces tests/e2e/indent_dedent.rs::test_shift_tab_dedent_no_indentation.
    // No-op: nothing to dedent, so the line stays put.
    assert_buffer_scenario(BufferScenario {
        description: "DedentSelection on un-indented line is a no-op".into(),
        initial_text: "Hello world".into(),
        actions: vec![Action::DedentSelection],
        expected_text: "Hello world".into(),
        expected_primary: CursorExpect::at(0),
        expected_extra_cursors: vec![],
        expected_selection_text: Some("".into()),
        ..Default::default()
    });
}
