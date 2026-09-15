use crate::common::harness::EditorTestHarness;
use crossterm::event::{KeyCode, KeyModifiers};

/// Test that AltGr+Shift character input works on Windows.
///
/// On Windows, crossterm reports AltGr as Ctrl+Alt. Some keyboard layouts
/// (e.g. Italian) require AltGr+Shift to type certain characters like
/// curly braces: AltGr+Shift+è = '{', AltGr+Shift+* = '}'.
///
/// Crossterm reports these as Ctrl+Alt+Shift + Char('{') / Char('}').
/// The editor must recognize this modifier combination as text input.
///
/// Reproduces: https://github.com/sinelaw/fresh/issues/993
#[test]
#[cfg_attr(
    not(windows),
    ignore = "AltGr+Shift is a Windows-specific modifier mapping"
)]
fn test_altgr_shift_curly_braces() {
    let mut harness = EditorTestHarness::new(80, 24).unwrap();

    // Simulate AltGr+Shift+è producing '{' on an Italian keyboard.
    // Crossterm reports AltGr as Ctrl+Alt, so AltGr+Shift = Ctrl+Alt+Shift.
    let altgr_shift = KeyModifiers::CONTROL | KeyModifiers::ALT | KeyModifiers::SHIFT;

    harness.send_key(KeyCode::Char('{'), altgr_shift).unwrap();

    harness.assert_buffer_content("{");

    harness.send_key(KeyCode::Char('}'), altgr_shift).unwrap();

    harness.assert_buffer_content("{}");
}
