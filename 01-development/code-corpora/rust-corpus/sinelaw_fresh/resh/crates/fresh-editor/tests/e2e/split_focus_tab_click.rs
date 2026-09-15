//! E2E test for bug: clicking a tab in an inactive split pane causes cursor
//! position corruption.
//!
//! Reproduction steps:
//! 1. Type "Hello" in buffer 1
//! 2. Create buffer 2 and type "Hej"
//! 3. Split vertical (both panes show buffer 2)
//! 4. Click in the right pane, type some chars after "Hej"
//! 5. Click the buffer 1 tab in the LEFT pane (switching focus + buffer via tab click)
//! 6. Type a sequence of characters
//!
//! Expected: characters are appended sequentially at the cursor position
//! Actual (bug): cursor column from the old pane is carried over and doesn't
//! advance, so characters are all inserted at the same column (appearing reversed)

use crate::common::harness::EditorTestHarness;
use crossterm::event::{KeyCode, KeyModifiers};

#[test]
fn test_split_focus_via_tab_click_cursor_position() {
    let mut harness = EditorTestHarness::new(120, 30).unwrap();

    // Step 1: Type "Hello" in the initial buffer
    harness.type_text("Hello").unwrap();
    harness.assert_buffer_content("Hello");

    // Step 2: Create a new buffer (Ctrl+N) and type "Hej"
    harness.new_buffer().unwrap();
    harness.type_text("Hej").unwrap();
    harness.assert_buffer_content("Hej");

    // Step 3: Split vertical via command palette
    harness
        .send_key(KeyCode::Char('p'), KeyModifiers::CONTROL)
        .unwrap();
    harness.render().unwrap();
    harness.type_text("split vert").unwrap();
    harness
        .send_key(KeyCode::Enter, KeyModifiers::NONE)
        .unwrap();
    harness.render().unwrap();

    // Verify split happened - should see the vertical separator
    let screen = harness.screen_to_string();
    assert!(
        screen.contains('│'),
        "Expected vertical split separator. Screen:\n{}",
        screen
    );

    // Step 4: Click in the right pane content area and type some chars
    // The right pane starts roughly at half width. Click in its content area.
    let right_pane_col = 80;
    let content_row = 3; // row 0=menu, 1=tabs, 2+=content
    harness.mouse_click(right_pane_col, content_row).unwrap();

    harness.type_text("xyz").unwrap();
    harness.assert_screen_contains("Hejxyz");

    // Step 5: Click the first tab ([No Name]) in the LEFT pane to switch
    // focus from right pane to left pane AND switch to buffer 1 ("Hello").
    // The left pane's tab bar is at row 1. The first tab starts near col 1.
    // We need to find the first tab in the left pane.
    // The left pane occupies roughly cols 0..59. Its tab bar has two tabs.
    // The first tab "[No Name]" starts around col 1-2.
    let left_tab_col = 5;
    let tab_row = 1;
    harness.mouse_click(left_tab_col, tab_row).unwrap();

    // The left pane should now show "Hello"
    harness.assert_screen_contains("Hello");

    // Step 6: Move cursor to end and type a known sequence
    harness.send_key(KeyCode::End, KeyModifiers::NONE).unwrap();
    harness.type_text("ABCDEF").unwrap();

    // Verify: the characters should appear in order after "Hello"
    // If the bug is present, they would appear reversed (e.g. "FEDCBA")
    // because each character is inserted at the same stale column position.
    let screen = harness.screen_to_string();
    eprintln!("Final screen:\n{}", screen);

    assert!(
        screen.contains("HelloABCDEF"),
        "Expected 'HelloABCDEF' (characters in order), but got something else. \
         This indicates the cursor position is corrupted when clicking a tab \
         in an inactive split pane. Screen:\n{}",
        screen
    );
}
