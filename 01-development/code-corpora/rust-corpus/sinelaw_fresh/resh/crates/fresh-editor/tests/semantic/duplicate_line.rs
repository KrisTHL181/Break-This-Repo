//! Track B migration: rewrites of `tests/e2e/duplicate_line.rs` as
//! declarative theorems.
//!
//! The original tests invoke "duplicate line" through the command
//! palette (Ctrl+P → "duplicate line" → Enter). The semantic action
//! `Action::DuplicateLine` exists and bypasses the palette entirely,
//! so the theorem version is dramatically shorter.
//!
//! Issue #591: Duplicate line or selected lines.

use crate::common::scenario::buffer_scenario::{
    assert_buffer_scenario, BufferScenario, CursorExpect,
};
use crate::common::scenario::trace_scenario::{assert_trace_scenario, TraceScenario};
use fresh::test_api::Action;

#[test]
fn theorem_duplicate_line_basic() {
    // Replaces tests/e2e/duplicate_line.rs::test_duplicate_line_basic.
    // Single-line buffer, cursor at end. DuplicateLine adds a newline
    // and copies the line below; cursor lands at the *start* of the
    // duplicated line (column reset to 0), not at its prior column.
    // The original e2e test never asserted the cursor position; this
    // theorem pins it down.
    assert_buffer_scenario(BufferScenario {
        description: "DuplicateLine on a single line produces line + LF + line".into(),
        initial_text: "hello world".into(),
        actions: vec![Action::MoveDocumentEnd, Action::DuplicateLine],
        expected_text: "hello world\nhello world".into(),
        expected_primary: CursorExpect::at(12),
        expected_extra_cursors: vec![],
        expected_selection_text: None,
        ..Default::default()
    });
}

#[test]
fn theorem_duplicate_line_with_following_lines() {
    // Replaces tests/e2e/duplicate_line.rs::test_duplicate_line_with_newline.
    // Cursor on line 1 of a 3-line buffer. Only line 1 is duplicated;
    // following lines slide down.
    assert_buffer_scenario(BufferScenario {
        description: "DuplicateLine on line 1 of 3 leaves following lines untouched".into(),
        initial_text: "first\nsecond\nthird".into(),
        actions: vec![Action::MoveDocumentStart, Action::DuplicateLine],
        expected_text: "first\nfirst\nsecond\nthird".into(),
        expected_primary: CursorExpect::at(6),
        expected_extra_cursors: vec![],
        expected_selection_text: None,
        ..Default::default()
    });
}

#[test]
fn theorem_duplicate_selected_lines_duplicates_each_selected_line() {
    // Replaces tests/e2e/duplicate_line.rs::test_duplicate_selected_lines.
    // With lines 2-3 selected, DuplicateLine duplicates the selected
    // block as a unit. The original e2e test only used `contains` so
    // the precise cursor position was never pinned down. The theorem
    // shows: the cursor lands at the start of the duplicated block
    // (position 29 = byte index of the second occurrence of "line two"),
    // and the selection is cleared.
    assert_buffer_scenario(BufferScenario {
        description: "DuplicateLine over a multi-line selection duplicates the selected block"
            .into(),
        initial_text: "line one\nline two\nline three\nline four".into(),
        // Move to start of line 2, then select two lines (down twice with shift).
        actions: vec![
            Action::MoveDocumentStart,
            Action::MoveDown,
            Action::SelectDown,
            Action::SelectDown,
            Action::DuplicateLine,
        ],
        expected_text: "line one\nline two\nline three\nline two\nline three\nline four".into(),
        expected_primary: CursorExpect::at(29),
        expected_extra_cursors: vec![],
        expected_selection_text: Some("".into()),
        ..Default::default()
    });
}

#[test]
fn theorem_duplicate_line_then_typing_inserts_into_duplicate() {
    // Replaces tests/e2e/duplicate_line.rs::test_duplicate_line_cursor_on_new_line.
    // Asserts the cursor lands on the *new* (lower) duplicate, so a
    // subsequent insertion appears on that line, not the original.
    assert_buffer_scenario(BufferScenario {
        description: "After DuplicateLine, typing inserts on the duplicate line".into(),
        initial_text: "first\nsecond\nthird".into(),
        actions: vec![
            Action::MoveDocumentStart,
            Action::DuplicateLine,
            Action::InsertChar('X'),
        ],
        expected_text: "first\nXfirst\nsecond\nthird".into(),
        expected_primary: CursorExpect::at(7),
        expected_extra_cursors: vec![],
        expected_selection_text: None,
        ..Default::default()
    });
}

#[test]
fn theorem_duplicate_line_undo_restores_original() {
    // Replaces tests/e2e/duplicate_line.rs::test_duplicate_line_undo.
    // DuplicateLine is a single undo unit — one Undo restores the input.
    assert_trace_scenario(TraceScenario {
        description: "DuplicateLine is one undo unit — Undo restores the input".into(),
        initial_text: "hello world".into(),
        actions: vec![Action::MoveDocumentEnd, Action::DuplicateLine],
        expected_text: "hello world\nhello world".into(),
        undo_count: 1,
    });
}
