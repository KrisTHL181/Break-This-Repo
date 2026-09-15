//! Track B migration: `tests/e2e/toggle_comment.rs` rewritten as
//! declarative theorems.
//!
//! The original tests configure a real file with a language-specific
//! extension (`.rs`, `.py`, `.sh`, `.yaml`, `.yml`, `.c`), drop the
//! cursor, and dispatch "Toggle Comment" via the command palette
//! (Ctrl+P → text → Enter). The semantic versions set
//! `language: Some("x.<ext>".into())` on the scenario value (so
//! language detection picks the right comment prefix) and dispatch
//! `Action::ToggleComment` directly.
//!
//! Issue #774 (YAML/YML toggle-comment) is covered by the YAML/YML
//! scenarios below. The infinite-loop regression at issue
//! "no-trailing-newline" is preserved by
//! `theorem_toggle_comment_single_line_no_newline` below.

use crate::common::scenario::buffer_scenario::{
    assert_buffer_scenario, BufferScenario, CursorExpect,
};
use fresh::test_api::Action;

// ─────────────────────────────────────────────────────────────────────────
// Per-language comment prefix selection
// ─────────────────────────────────────────────────────────────────────────

#[test]
fn theorem_toggle_comment_rust_uses_double_slash_prefix() {
    // Replaces test_toggle_comment_rust_prefix.
    // Cursor at byte 0 — Toggle Comment comments the first line.
    assert_buffer_scenario(BufferScenario {
        language: Some("x.rs".into()),
        description: "ToggleComment on a .rs file uses '// ' as the prefix".into(),
        initial_text: "fn main() {\n    println!(\"hello\");\n}".into(),
        actions: vec![Action::ToggleComment],
        expected_text: "// fn main() {\n    println!(\"hello\");\n}".into(),
        expected_primary: CursorExpect::at(3),
        expected_extra_cursors: vec![],
        expected_selection_text: None,
        ..Default::default()
    });
}

#[test]
fn theorem_toggle_comment_python_uses_hash_prefix() {
    // Replaces test_toggle_comment_python_prefix.
    assert_buffer_scenario(BufferScenario {
        language: Some("x.py".into()),
        description: "ToggleComment on a .py file uses '# ' as the prefix".into(),
        initial_text: "def main():\n    print(\"hello\")\n".into(),
        actions: vec![Action::ToggleComment],
        expected_text: "# def main():\n    print(\"hello\")\n".into(),
        expected_primary: CursorExpect::at(2),
        expected_extra_cursors: vec![],
        expected_selection_text: None,
        ..Default::default()
    });
}

#[test]
fn theorem_toggle_comment_shell_uses_hash_prefix() {
    // Replaces test_toggle_comment_shell_prefix.
    assert_buffer_scenario(BufferScenario {
        language: Some("x.sh".into()),
        description: "ToggleComment on a .sh file uses '# ' as the prefix".into(),
        initial_text: "echo hello\n".into(),
        actions: vec![Action::ToggleComment],
        expected_text: "# echo hello\n".into(),
        expected_primary: CursorExpect::at(2),
        expected_extra_cursors: vec![],
        expected_selection_text: None,
        ..Default::default()
    });
}

#[test]
fn theorem_toggle_comment_yaml_uses_hash_prefix() {
    // Replaces test_toggle_comment_yaml_prefix (issue #774).
    assert_buffer_scenario(BufferScenario {
        language: Some("x.yaml".into()),
        description: "ToggleComment on a .yaml file uses '# ' (issue #774)".into(),
        initial_text: "key: value\nnested:\n  child: 123".into(),
        actions: vec![Action::ToggleComment],
        expected_text: "# key: value\nnested:\n  child: 123".into(),
        expected_primary: CursorExpect::at(2),
        expected_extra_cursors: vec![],
        expected_selection_text: None,
        ..Default::default()
    });
}

#[test]
fn theorem_toggle_comment_yml_uses_hash_prefix() {
    // Replaces test_toggle_comment_yml_prefix (issue #774).
    assert_buffer_scenario(BufferScenario {
        language: Some("x.yml".into()),
        description: "ToggleComment on a .yml file uses '# ' (issue #774)".into(),
        initial_text: "server:\n  port: 8080".into(),
        actions: vec![Action::ToggleComment],
        expected_text: "# server:\n  port: 8080".into(),
        expected_primary: CursorExpect::at(2),
        expected_extra_cursors: vec![],
        expected_selection_text: None,
        ..Default::default()
    });
}

// ─────────────────────────────────────────────────────────────────────────
// Selection preservation
// ─────────────────────────────────────────────────────────────────────────

#[test]
fn theorem_toggle_comment_preserves_selection() {
    // Replaces test_toggle_comment_preserves_selection.
    // Select "line1\nline2\n" (positions 0..12), comment, expect a
    // selection that has *grown* by 6 (= 2 lines × 3 chars "// ").
    // FINDING (theorem-only): the original e2e test only asserted
    // that *some* selection survives; the theorem pins both endpoints.
    assert_buffer_scenario(BufferScenario {
        language: Some("x.rs".into()),
        description: "Comment-toggling preserves and grows the selection by 2*'// '".into(),
        initial_text: "line1\nline2\nline3\nline4".into(),
        actions: vec![
            Action::SelectDown,
            Action::SelectDown,
            Action::ToggleComment,
        ],
        expected_text: "// line1\n// line2\nline3\nline4".into(),
        expected_primary: CursorExpect::range(0, 18),
        expected_extra_cursors: vec![],
        expected_selection_text: Some("// line1\n// line2\n".into()),
        ..Default::default()
    });
}

#[test]
fn theorem_toggle_uncomment_preserves_selection() {
    // Replaces test_toggle_uncomment_preserves_selection.
    // Initial buffer has "// " on the first three lines. Selecting
    // lines 1 and 2 (positions 0..18 = "// line1\n// line2\n") and
    // toggling uncomments them; the selection shrinks by 2*'// ' = 6.
    assert_buffer_scenario(BufferScenario {
        language: Some("x.rs".into()),
        description: "Comment-toggling on commented lines uncomments and preserves the selection"
            .into(),
        initial_text: "// line1\n// line2\n// line3\nline4".into(),
        actions: vec![
            Action::SelectDown,
            Action::SelectDown,
            Action::ToggleComment,
        ],
        expected_text: "line1\nline2\n// line3\nline4".into(),
        expected_primary: CursorExpect::range(0, 12),
        expected_extra_cursors: vec![],
        expected_selection_text: Some("line1\nline2\n".into()),
        ..Default::default()
    });
}

#[test]
fn theorem_toggle_comment_roundtrip_with_selection_is_identity() {
    // Replaces test_toggle_comment_roundtrip_with_selection.
    // SelectAll + ToggleComment + SelectAll + ToggleComment == identity.
    assert_buffer_scenario(BufferScenario {
        language: Some("x.rs".into()),
        description: "SelectAll + Toggle + SelectAll + Toggle is the identity on text".into(),
        initial_text: "line1\nline2\nline3".into(),
        actions: vec![
            Action::SelectAll,
            Action::ToggleComment,
            Action::SelectAll,
            Action::ToggleComment,
        ],
        expected_text: "line1\nline2\nline3".into(),
        expected_primary: CursorExpect::range(0, 17),
        expected_extra_cursors: vec![],
        expected_selection_text: Some("line1\nline2\nline3".into()),
        ..Default::default()
    });
}

// ─────────────────────────────────────────────────────────────────────────
// Edge cases — buffer end / no trailing newline
// ─────────────────────────────────────────────────────────────────────────

#[test]
fn theorem_toggle_comment_single_line_no_newline() {
    // Replaces test_toggle_comment_single_line_no_newline.
    // Regression: a previous version of the toggle-comment loop went
    // infinite when `selection.end == buffer.len()` and the buffer
    // had no trailing newline. The theorem terminates → guaranteed
    // no-infinite-loop. C uses `// `.
    assert_buffer_scenario(BufferScenario {
        language: Some("x.c".into()),
        description: "ToggleComment on a single-line .c buffer with no trailing newline".into(),
        initial_text: "int main() {}".into(),
        actions: vec![Action::SelectAll, Action::ToggleComment],
        expected_text: "// int main() {}".into(),
        expected_primary: CursorExpect::range(0, 16),
        expected_extra_cursors: vec![],
        expected_selection_text: Some("// int main() {}".into()),
        ..Default::default()
    });
}

#[test]
fn theorem_toggle_comment_selection_at_buffer_end() {
    // Replaces test_toggle_comment_selection_at_buffer_end.
    // Multi-line .rs buffer with no trailing newline, SelectAll then
    // toggle. Both lines should get commented.
    assert_buffer_scenario(BufferScenario {
        language: Some("x.rs".into()),
        description: "ToggleComment over a SelectAll that ends exactly at buffer length".into(),
        initial_text: "fn foo() {}\nfn bar() {}".into(),
        actions: vec![Action::SelectAll, Action::ToggleComment],
        expected_text: "// fn foo() {}\n// fn bar() {}".into(),
        expected_primary: CursorExpect::range(0, 29),
        expected_extra_cursors: vec![],
        expected_selection_text: Some("// fn foo() {}\n// fn bar() {}".into()),
        ..Default::default()
    });
}
