//! Regression test for issue #1288: "Word select includes whitespace"
//!
//! On the macOS keymap, pressing Alt+Right (Option+Right) from inside a word
//! should move the cursor to the END of the current word — not past the
//! trailing whitespace to the start of the next word. Similarly,
//! Alt+Shift+Right should select up to the word end only. This matches
//! the standard macOS convention (TextEdit, VS Code on macOS, etc.).

use crate::common::harness::{EditorTestHarness, HarnessOptions};
use crossterm::event::{KeyCode, KeyModifiers};
use fresh::config::Config;

fn macos_harness(width: u16, height: u16) -> EditorTestHarness {
    let config = Config {
        active_keybinding_map: "macos".into(),
        ..Default::default()
    };
    EditorTestHarness::create(
        width,
        height,
        HarnessOptions::new()
            .with_config(config)
            .with_preserved_keybinding_map(),
    )
    .unwrap()
}

/// Alt+Right from within the first word should stop at the end of that word,
/// not consume trailing whitespace.
#[test]
fn test_alt_right_stops_at_word_end() {
    let mut harness = macos_harness(80, 24);
    harness.type_text("hello world test").unwrap();
    harness.send_key(KeyCode::Home, KeyModifiers::NONE).unwrap();

    // From start of "hello", Alt+Right should land at end of "hello" (pos 5),
    // NOT at the start of "world" (pos 6).
    harness.send_key(KeyCode::Right, KeyModifiers::ALT).unwrap();
    assert_eq!(
        harness.cursor_position(),
        5,
        "Alt+Right from start of 'hello' should stop at end of word (pos 5), \
         got pos {}",
        harness.cursor_position()
    );

    // A second Alt+Right should skip the space and land at end of "world".
    harness.send_key(KeyCode::Right, KeyModifiers::ALT).unwrap();
    assert_eq!(
        harness.cursor_position(),
        11,
        "Second Alt+Right should stop at end of 'world' (pos 11), got pos {}",
        harness.cursor_position()
    );
}

/// Alt+Shift+Right should extend the selection to the word end, without
/// including trailing whitespace.
#[test]
fn test_alt_shift_right_selects_word_only() {
    let mut harness = macos_harness(80, 24);
    harness.type_text("hello world").unwrap();
    harness.send_key(KeyCode::Home, KeyModifiers::NONE).unwrap();

    harness
        .send_key(KeyCode::Right, KeyModifiers::ALT | KeyModifiers::SHIFT)
        .unwrap();

    let cursor = harness.editor().active_cursors().primary();
    let range = cursor
        .selection_range()
        .expect("Alt+Shift+Right should create a selection");
    assert_eq!(
        (range.start, range.end),
        (0, 5),
        "Alt+Shift+Right from start should select exactly 'hello' (0..5), got {range:?}",
    );

    let selected = harness
        .editor_mut()
        .active_state_mut()
        .get_text_range(range.start, range.end);
    assert_eq!(
        selected, "hello",
        "Selected text should be 'hello' without trailing space, got {selected:?}",
    );
}

/// Ctrl+Right on macOS keymap should behave the same as Alt+Right (end of word).
#[test]
fn test_ctrl_right_stops_at_word_end_on_macos() {
    let mut harness = macos_harness(80, 24);
    harness.type_text("hello world").unwrap();
    harness.send_key(KeyCode::Home, KeyModifiers::NONE).unwrap();

    harness
        .send_key(KeyCode::Right, KeyModifiers::CONTROL)
        .unwrap();
    assert_eq!(
        harness.cursor_position(),
        5,
        "Ctrl+Right (macOS keymap) from start of 'hello' should stop at pos 5, got {}",
        harness.cursor_position()
    );
}
