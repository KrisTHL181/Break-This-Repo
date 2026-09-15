//! Track B migration: rewrites of pure-state tests from
//! `tests/e2e/emacs_actions.rs` as declarative theorems.
//!
//! The original tests configure an Emacs keybinding map and drive the
//! editor through `Ctrl-T` / `Ctrl-O` / `Ctrl-Space`. The semantic
//! version dispatches `Action::TransposeChars` / `Action::OpenLine` /
//! `Action::SetMark` directly, so it tests the *action* (the Emacs
//! semantics) without depending on the keybinding-map plumbing.
//!
//! Skipped:
//!   * `test_recenter_basic` — viewport-dependent, would need a
//!     LayoutScenario with explicit dimensions and scrolling.
//!   * `test_escape_cancels_mark_mode` / `test_ctrl_g_cancels_mark_mode`
//!     — these test the *keybinding* (Esc / Ctrl-G → cancel-mark-mode)
//!     plus the internal `deselect_on_move` flag, which isn't part of
//!     the public `Caret` projection.

use crate::common::scenario::buffer_scenario::{
    assert_buffer_scenario, BufferScenario, CursorExpect,
};
use fresh::test_api::Action;

// ─────────────────────────────────────────────────────────────────────────
// TransposeChars (Emacs C-t)
// ─────────────────────────────────────────────────────────────────────────

#[test]
fn theorem_transpose_chars_swaps_two_characters() {
    // Replaces tests/e2e/emacs_actions.rs::test_transpose_chars_basic.
    // With cursor between 'b' and 'c' in "abc", TransposeChars swaps
    // the chars on either side of the cursor.
    assert_buffer_scenario(BufferScenario {
        description: "TransposeChars swaps the chars on either side of the cursor".into(),
        initial_text: "abc".into(),
        actions: vec![
            Action::MoveDocumentEnd,
            Action::MoveLeft,
            Action::TransposeChars,
        ],
        expected_text: "acb".into(),
        expected_primary: CursorExpect::at(3),
        expected_extra_cursors: vec![],
        expected_selection_text: None,
        ..Default::default()
    });
}

#[test]
fn theorem_transpose_chars_at_beginning_is_noop() {
    // Replaces test_transpose_chars_at_beginning.
    // At position 0 there is no char to the left, so TransposeChars
    // is a no-op (text unchanged, cursor unchanged).
    assert_buffer_scenario(BufferScenario {
        description: "TransposeChars at beginning of buffer is a no-op".into(),
        initial_text: "abc".into(),
        actions: vec![Action::MoveDocumentStart, Action::TransposeChars],
        expected_text: "abc".into(),
        expected_primary: CursorExpect::at(0),
        expected_extra_cursors: vec![],
        expected_selection_text: None,
        ..Default::default()
    });
}

#[test]
fn theorem_transpose_chars_at_end_is_noop() {
    // Replaces test_transpose_chars_at_end.
    // With the cursor at end-of-buffer, there is no char *at* the
    // cursor to swap with the previous one; TransposeChars is a no-op.
    assert_buffer_scenario(BufferScenario {
        description: "TransposeChars at end of buffer is a no-op".into(),
        initial_text: "ab".into(),
        actions: vec![Action::MoveDocumentEnd, Action::TransposeChars],
        expected_text: "ab".into(),
        expected_primary: CursorExpect::at(2),
        expected_extra_cursors: vec![],
        expected_selection_text: None,
        ..Default::default()
    });
}

// ─────────────────────────────────────────────────────────────────────────
// OpenLine (Emacs C-o)
// ─────────────────────────────────────────────────────────────────────────

#[test]
fn theorem_open_line_inserts_newline_without_advancing_cursor() {
    // Emacs C-o semantics: insert a newline AT the cursor and leave
    // the cursor where it was. A subsequent type appears on the
    // original (upper) line, not the new (lower) one.
    assert_buffer_scenario(BufferScenario {
        description: "OpenLine inserts a newline; cursor stays at insertion point".into(),
        initial_text: "hello".into(),
        // Move to position 3 ("hel|lo") then OpenLine.
        actions: vec![
            Action::MoveDocumentEnd,
            Action::MoveLeft,
            Action::MoveLeft,
            Action::OpenLine,
        ],
        expected_text: "hel\nlo".into(),
        expected_primary: CursorExpect::at(3),
        expected_extra_cursors: vec![],
        expected_selection_text: None,
        ..Default::default()
    });
}

#[test]
fn theorem_open_line_at_beginning_inserts_leading_newline() {
    // Replaces test_open_line_at_beginning. Cursor at position 0
    // before AND after — the inserted newline ends up *after* the
    // cursor.
    assert_buffer_scenario(BufferScenario {
        description: "OpenLine at beginning inserts a leading newline; cursor stays at 0".into(),
        initial_text: "hello".into(),
        actions: vec![Action::MoveDocumentStart, Action::OpenLine],
        expected_text: "\nhello".into(),
        expected_primary: CursorExpect::at(0),
        expected_extra_cursors: vec![],
        expected_selection_text: None,
        ..Default::default()
    });
}

#[test]
fn theorem_open_line_then_typing_inserts_on_original_line() {
    // Regression test for the bug fixed in the OpenLine handler:
    // before the fix, the cursor advanced past the newline so this
    // sequence produced "hel\nXlo" instead of "helX\nlo".
    assert_buffer_scenario(BufferScenario {
        description: "After OpenLine, typing inserts on the original (upper) line".into(),
        initial_text: "hello".into(),
        actions: vec![
            Action::MoveDocumentEnd,
            Action::MoveLeft,
            Action::MoveLeft,
            Action::OpenLine,
            Action::InsertChar('X'),
        ],
        expected_text: "helX\nlo".into(),
        expected_primary: CursorExpect::at(4),
        expected_extra_cursors: vec![],
        expected_selection_text: None,
        ..Default::default()
    });
}

// ─────────────────────────────────────────────────────────────────────────
// SetMark (Emacs C-Space) — mark-mode anchor behavior
// ─────────────────────────────────────────────────────────────────────────

#[test]
fn theorem_set_mark_creates_anchor_at_cursor_position() {
    // Replaces test_set_mark_basic.
    // After SetMark at position 0, the cursor has an anchor at 0.
    // No characters are between cursor and anchor, so the selection
    // text is empty.
    assert_buffer_scenario(BufferScenario {
        description: "SetMark sets the anchor at the cursor position".into(),
        initial_text: "hello world".into(),
        actions: vec![Action::MoveDocumentStart, Action::SetMark],
        expected_text: "hello world".into(),
        expected_primary: CursorExpect::range(0, 0),
        expected_extra_cursors: vec![],
        expected_selection_text: Some("".into()),
        ..Default::default()
    });
}

#[test]
fn theorem_set_mark_then_move_extends_selection() {
    // Replaces test_set_mark_then_regular_move_creates_selection.
    // The defining property of Emacs mark-mode: after SetMark, plain
    // (non-shift) movements extend the selection rather than clearing
    // the anchor. Selecting "hello" via MoveRight x5.
    assert_buffer_scenario(BufferScenario {
        description: "SetMark + MoveRight extends the selection (mark mode)".into(),
        initial_text: "hello world".into(),
        actions: vec![
            Action::MoveDocumentStart,
            Action::SetMark,
            Action::MoveRight,
            Action::MoveRight,
            Action::MoveRight,
            Action::MoveRight,
            Action::MoveRight,
        ],
        expected_text: "hello world".into(),
        expected_primary: CursorExpect::range(0, 5),
        expected_extra_cursors: vec![],
        expected_selection_text: Some("hello".into()),
        ..Default::default()
    });
}

#[test]
fn theorem_set_mark_then_shift_move_extends_selection() {
    // Replaces test_set_mark_then_shift_move_creates_selection.
    // Even with shift movements, the anchor set by SetMark is the one
    // that remains; selection still spans 0..5.
    assert_buffer_scenario(BufferScenario {
        description: "SetMark + SelectRight extends the selection".into(),
        initial_text: "hello world".into(),
        actions: vec![
            Action::MoveDocumentStart,
            Action::SetMark,
            Action::SelectRight,
            Action::SelectRight,
            Action::SelectRight,
            Action::SelectRight,
            Action::SelectRight,
        ],
        expected_text: "hello world".into(),
        expected_primary: CursorExpect::range(0, 5),
        expected_extra_cursors: vec![],
        expected_selection_text: Some("hello".into()),
        ..Default::default()
    });
}
