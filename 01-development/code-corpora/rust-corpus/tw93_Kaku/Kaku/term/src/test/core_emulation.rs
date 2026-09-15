use super::*;
use k9::assert_equal as assert_eq;

#[test]
fn streamed_wrapped_url_keeps_complete_destination_on_every_row() {
    use wezterm_surface::hyperlink::{Rule, GENERIC_HYPERLINK_PATTERN};

    let url = "https://developer.apple.com/icon-composer/";
    let rules = vec![
        Rule::with_highlight(r"\((\w+://[\x21-\x7e]+)\)", "$1", 1).unwrap(),
        Rule::new(GENERIC_HYPERLINK_PATTERN, "$0").unwrap(),
    ];
    for width in [20, 27, 28, 40] {
        let mut term = TestTerm::new(6, width, 0);
        // Hover while an AI/TUI streams the URL, including before the physical
        // row gains its wrap flag. A cached partial match must be invalidated.
        for byte in format!("({url}) .").bytes() {
            term.print([byte]);
            term.screen_mut()
                .for_each_logical_line_in_stable_range_mut(0..6, |_, lines| {
                    Line::apply_hyperlink_rules(&rules, lines);
                    true
                });
        }
        let visible = term.screen().visible_lines();
        for (offset, _) in url.bytes().enumerate() {
            let absolute = offset + 1; // opening parenthesis
            let cell = visible[absolute / width]
                .get_cell(absolute % width)
                .unwrap();
            assert_eq!(cell.attrs().hyperlink().unwrap().uri(), url);
        }
    }
}

// ─── Cursor movement ─────────────────────────────────────────────────────────

#[test]
fn cursor_moves_right_with_cuf() {
    let mut term = TestTerm::new(5, 10, 0);
    // CUF (CSI C) moves cursor right N columns
    term.print("\x1b[3C");
    term.assert_cursor_pos(3, 0, Some("CUF 3"), None);
}

#[test]
fn cursor_moves_down_with_cud() {
    let mut term = TestTerm::new(5, 10, 0);
    // CUD (CSI B) moves cursor down N rows
    term.print("\x1b[2B");
    term.assert_cursor_pos(0, 2, Some("CUD 2"), None);
}

#[test]
fn cursor_moves_left_with_cub() {
    let mut term = TestTerm::new(5, 10, 0);
    // Position at column 5 first, then move back 3
    term.print("\x1b[6G"); // CHA: move to column 6 (1-based)
    term.print("\x1b[3D"); // CUB: move left 3
    term.assert_cursor_pos(2, 0, Some("CUB 3 from col 5"), None);
}

#[test]
fn cursor_moves_up_with_cuu() {
    let mut term = TestTerm::new(5, 10, 0);
    // Move down 3, then up 1
    term.print("\x1b[3B\x1b[1A");
    term.assert_cursor_pos(0, 2, Some("CUU 1 from row 3"), None);
}

#[test]
fn cup_positions_cursor_at_row_col() {
    let mut term = TestTerm::new(10, 20, 0);
    // CUP (CSI row;col H) – 1-based in escape, 0-based in assertion
    term.cup(4, 2); // col=4, row=2 (0-based)
    term.assert_cursor_pos(4, 2, Some("CUP 4,2"), None);
}

#[test]
fn cursor_cannot_move_past_screen_edge() {
    let mut term = TestTerm::new(5, 10, 0);
    // Large CUF should clamp at last column (9)
    term.print("\x1b[999C");
    term.assert_cursor_pos(9, 0, Some("CUF clamp at right edge"), None);
}

// ─── Screen and line clearing ─────────────────────────────────────────────────

#[test]
fn erase_in_display_clears_entire_screen() {
    use wezterm_escape_parser::csi::EraseInDisplay;
    let mut term = TestTerm::new(3, 5, 0);
    term.print("hello");
    term.print("\r\nworld");
    let seqno = term.current_seqno();
    term.erase_in_display(EraseInDisplay::EraseDisplay);
    // All three rows should be dirty after a full clear
    term.assert_dirty_lines(seqno, &[0, 1, 2], Some("EraseDisplay dirtied all rows"));
}

#[test]
fn erase_in_line_clears_to_end_of_line() {
    use wezterm_escape_parser::csi::EraseInLine;
    let mut term = TestTerm::new(3, 10, 0);
    term.print("hello");
    let seqno = term.current_seqno();
    // Move cursor to col 2, erase to end of line
    term.print("\x1b[3G"); // CHA to col 3 (1-based)
    term.erase_in_line(EraseInLine::EraseToEndOfLine);
    // Row 0 modified; rows 1 and 2 unchanged
    term.assert_dirty_lines(seqno, &[0], Some("EraseToEndOfLine dirtied row 0 only"));
}

#[test]
fn clack_style_rerender_clears_wrapped_lines() {
    let mut term = TestTerm::new(5, 12, 0);

    // skills/@clack rerenders by moving back to the top of the prompt block,
    // clearing each visual row, then writing the next frame.
    term.print("\x1b[36m│ ❯ ○ alpha-beta-gamma\x1b[0m\r\n");
    term.print("│   ○ Other\r\n");
    term.print("└\r\n");
    assert!(
        term.screen().visible_lines()[0].last_cell_was_wrapped(),
        "long prompt label should wrap before the next clack frame"
    );

    term.print("\x1b[4A");
    for _ in 0..4 {
        term.print("\x1b[2K\x1b[1B");
    }
    for row in 0..4 {
        assert!(
            !term.screen().visible_lines()[row].last_cell_was_wrapped(),
            "clear-line should reset wrapped state on row {}",
            row
        );
    }
    term.print("\x1b[4A");

    term.print("│   ○ alpha\r\n");
    term.print("\x1b[36m│ ❯ ○ Other\x1b[0m\r\n");
    term.print("└\r\n");

    assert_visible_contents(
        &term,
        file!(),
        line!(),
        &[
            "│   ○ alpha ",
            "│ ❯ ○ Other ",
            "└           ",
            "            ",
            "",
        ],
    );
}

#[test]
fn delete_lines_scrolls_content_up() {
    let mut term = TestTerm::new(4, 10, 0);
    term.print("line0\r\nline1\r\nline2\r\nline3");
    let seqno = term.current_seqno();
    // Move cursor to row 1, delete 1 line
    term.cup(0, 1);
    term.delete_lines(1);
    assert_visible_contents(&term, file!(), line!(), &["line0", "line2", "line3", ""]);
    term.assert_dirty_lines(seqno, &[1, 2, 3], Some("delete_lines shifts lower rows"));
}

// ─── Text attributes (SGR) ────────────────────────────────────────────────────

#[test]
fn sgr_bold_sets_intensity() {
    use crate::Intensity;
    let mut term = TestTerm::new(2, 20, 0);
    term.print("\x1b[1mX"); // SGR 1 = bold
    let line = &term.screen().visible_lines()[0];
    let cell = line.get_cell(0).expect("cell at col 0");
    assert_eq!(
        cell.attrs().intensity(),
        Intensity::Bold,
        "SGR 1 should produce Bold intensity"
    );
}

#[test]
fn sgr_reset_clears_bold() {
    use crate::Intensity;
    let mut term = TestTerm::new(2, 20, 0);
    term.print("\x1b[1m\x1b[0mY"); // bold then reset
    let line = &term.screen().visible_lines()[0];
    let cell = line.get_cell(0).expect("cell at col 0");
    assert_eq!(
        cell.attrs().intensity(),
        Intensity::Normal,
        "SGR 0 should reset intensity to Normal"
    );
}

// ─── Scroll region ────────────────────────────────────────────────────────────

#[test]
fn scroll_region_limits_cursor_vertical_movement() {
    let mut term = TestTerm::new(6, 10, 0);
    // Set scroll region rows 2-4 (1-based)
    term.set_scroll_region(1, 3); // 0-based: rows 1 to 3
                                  // Move cursor inside the region to the last row, then print a newline –
                                  // the cursor should stay within the region (scroll, not move beyond).
    term.cup(0, 3);
    term.assert_cursor_pos(0, 3, Some("cursor at bottom of scroll region"), None);
    term.print("\n"); // newline at the bottom of the region triggers scroll
                      // After scrolling, cursor remains at row 3 (the bottom of the region)
    term.assert_cursor_pos(
        0,
        3,
        Some("cursor stays at region bottom after scroll"),
        None,
    );
}

// ─── Tab stops ────────────────────────────────────────────────────────────────

#[test]
fn horizontal_tab_advances_to_next_stop() {
    let mut term = TestTerm::new(2, 40, 0);
    // Default tab stops every 8 columns; starting at col 0, \t → col 8
    term.print("\t");
    term.assert_cursor_pos(8, 0, Some("tab from col 0 stops at col 8"), None);
}

#[test]
fn multiple_tabs_hit_successive_stops() {
    let mut term = TestTerm::new(2, 40, 0);
    term.print("\t\t");
    term.assert_cursor_pos(16, 0, Some("two tabs land at col 16"), None);
}

// ─── Logical line joining ─────────────────────────────────────────────────────

#[test]
fn logical_line_walker_does_not_join_full_width_hard_newline_rows() {
    for indent in ["", "  "] {
        let mut term = TestTerm::new(4, 10, 0);
        // Width alone cannot prove that adjacent rows share logical ownership.
        // Explicit newlines keep them separate even when a row fills the terminal.
        term.print(&format!("ABCDEFGHIJ\r\n{indent}KLMNO\r\nxyz\r\n"));

        let mut groups: Vec<usize> = vec![];
        term.term
            .screen_mut()
            .for_each_logical_line_in_stable_range_mut(0..3, |_range, lines| {
                groups.push(lines.len());
                true
            });

        assert_eq!(
            groups,
            vec![1, 1, 1],
            "hard-newline rows stay independent regardless of width"
        );
    }
}
