//! E2E test for active tab color bleeding through dropdown menus.
//!
//! When a dropdown menu overlaps the tab bar, the active tab's distinct
//! styling (fg, bg, modifiers) should not seep through to the menu border
//! cells. The dropdown border should have uniform styling regardless of
//! what was rendered underneath.

use crate::common::harness::EditorTestHarness;
use crossterm::event::{KeyCode, KeyModifiers};

/// Test that the active tab's styling does not bleed through the dropdown
/// menu's top border where it overlaps the tab bar row.
///
/// The File menu dropdown starts at row 1 (the tab bar row). The active
/// tab has BOLD modifier. The dropdown border rendering applies fg via
/// set_style, but doesn't clear modifiers from the underlying cells.
/// This causes the BOLD modifier from the active tab to leak into the
/// dropdown border, which on many terminals makes the border color
/// appear as a different (brighter) shade where it overlaps the tab.
#[test]
fn test_active_tab_color_does_not_bleed_through_menu() {
    let mut harness = EditorTestHarness::new(80, 30).unwrap();
    harness.render().unwrap();

    // Verify the active tab is visible on the tab bar (row 1)
    let tab_row = 1u16;
    let tab_row_text = harness.get_row_text(tab_row);
    assert!(
        tab_row_text.contains("No Name"),
        "Expected tab on row {}. Got: '{}'",
        tab_row,
        tab_row_text
    );

    // Open the File menu (Alt+F) — its dropdown starts at row 1 (overlapping the tab bar)
    harness
        .send_key(KeyCode::Char('f'), KeyModifiers::ALT)
        .unwrap();
    harness.render().unwrap();

    // Verify menu is open
    harness.assert_screen_contains("New File");

    let screen = harness.screen_to_string();

    // Find the dropdown's column range on row 1 by locating box-drawing chars
    let row_chars: Vec<char> = harness.get_row_text(tab_row).chars().collect();
    let menu_left = row_chars
        .iter()
        .position(|&c| c == '┌')
        .expect("no ┌ on tab row") as u16;
    let menu_right = row_chars
        .iter()
        .rposition(|&c| c == '┐')
        .expect("no ┐ on tab row") as u16;

    // Collect styles of ALL border cells on the top border (row 1)
    let border_styles: Vec<_> = (menu_left..=menu_right)
        .filter_map(|col| harness.get_cell_style(col, tab_row).map(|s| (col, s)))
        .collect();

    // All top border cells should have UNIFORM styling. If some cells have
    // different fg, bg, or modifiers than others, it means styling from the
    // underlying tab bar leaked through the dropdown border.
    //
    // Use the LAST border cell (┐, far right) as reference — it's least
    // likely to overlap with the active tab.
    let (ref_col, ref_style) = *border_styles.last().unwrap();

    let mut inconsistent = Vec::new();
    for &(col, style) in &border_styles {
        if style.fg != ref_style.fg
            || style.bg != ref_style.bg
            || style.add_modifier != ref_style.add_modifier
        {
            inconsistent.push((col, style));
        }
    }

    if !inconsistent.is_empty() {
        let mut detail = String::new();
        for (col, style) in &inconsistent {
            detail.push_str(&format!(
                "  col {}: fg={:?}, bg={:?}, modifier={:?}\n",
                col, style.fg, style.bg, style.add_modifier
            ));
        }
        panic!(
            "Tab styling bleeds through dropdown border on row {}.\n\
             Expected uniform style (from col {}): fg={:?}, bg={:?}, modifier={:?}\n\
             But these border cells differ:\n{}\
             The active tab's BOLD modifier leaks through because the dropdown \
             rendering only patches fg/bg without clearing prior modifiers.\n\
             Screen:\n{}",
            tab_row, ref_col, ref_style.fg, ref_style.bg, ref_style.add_modifier, detail, screen
        );
    }
}
