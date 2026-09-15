use termwiz::cell::unicode_column_width;
use unicode_segmentation::UnicodeSegmentation;

/// Convert a character index into a byte offset in `s`.
pub(super) fn char_to_byte_pos(s: &str, char_idx: usize) -> usize {
    s.char_indices()
        .nth(char_idx)
        .map(|(b, _)| b)
        .unwrap_or(s.len())
}

/// Char-index of the previous word boundary (macOS Option+Left semantics):
/// skip trailing whitespace, then skip the run of non-whitespace characters.
pub(super) fn prev_word_pos(input: &str, cursor: usize) -> usize {
    let chars: Vec<char> = input.chars().collect();
    let mut i = cursor.min(chars.len());
    while i > 0 && chars[i - 1].is_whitespace() {
        i -= 1;
    }
    while i > 0 && !chars[i - 1].is_whitespace() {
        i -= 1;
    }
    i
}

/// Char-index of the next word boundary (macOS Option+Right semantics):
/// skip leading whitespace, then skip the run of non-whitespace characters.
pub(super) fn next_word_pos(input: &str, cursor: usize) -> usize {
    let chars: Vec<char> = input.chars().collect();
    let mut i = cursor.min(chars.len());
    while i < chars.len() && chars[i].is_whitespace() {
        i += 1;
    }
    while i < chars.len() && !chars[i].is_whitespace() {
        i += 1;
    }
    i
}

/// Truncate `s` to at most `max_cols` visual terminal columns.
/// Accounts for wide characters (CJK = 2 cols per char).
pub(super) fn truncate(s: &str, max_cols: usize) -> String {
    let mut w = 0usize;
    let mut out = String::with_capacity(s.len());
    for g in s.graphemes(true) {
        let gw = unicode_column_width(g, None);
        if w + gw > max_cols {
            break;
        }
        w += gw;
        out.push_str(g);
    }
    out
}

/// Result of hard-wrapping the prompt+input string by visual column width.
/// `cursor_row` / `cursor_col` are 0-indexed positions into the wrapped
/// row grid (col is in visual columns, not bytes).
pub(super) struct InputLayout {
    pub(super) rows: Vec<String>,
    pub(super) cursor_row: usize,
    pub(super) cursor_col: usize,
}

/// Hard-wrap `prompt + input` to `width` visual columns, splitting at
/// grapheme boundaries (no word awareness - chat input expects exact
/// position). Tracks where `cursor_chars` (a char index into `input`)
/// lands in the wrapped grid. When the cursor sits at the trailing edge
/// of a row that is exactly `width` wide, an empty phantom row is
/// appended so the caret has somewhere to render instead of overflowing
/// onto the right border.
pub(super) fn layout_input(
    prompt: &str,
    input: &str,
    cursor_chars: usize,
    width: usize,
) -> InputLayout {
    if width == 0 {
        return InputLayout {
            rows: vec![format!("{}{}", prompt, input)],
            cursor_row: 0,
            cursor_col: 0,
        };
    }

    let cursor_byte_in_input = char_to_byte_pos(input, cursor_chars);
    let cursor_byte_in_combined = prompt.len() + cursor_byte_in_input;
    let combined: String = format!("{}{}", prompt, input);

    let mut rows: Vec<String> = vec![String::new()];
    let mut row_widths: Vec<usize> = vec![0];
    let mut byte_pos = 0usize;
    let mut cursor_row: usize = 0;
    let mut cursor_col: usize = 0;
    let mut found_cursor = false;

    for g in combined.graphemes(true) {
        let is_hard_break = g == "\n";
        let gw = if is_hard_break {
            0
        } else {
            unicode_column_width(g, None)
        };
        // Hard breaks always start a new row regardless of width; only check
        // soft-wrap when the grapheme actually contributes columns.
        let need_wrap = !is_hard_break
            && *row_widths.last().unwrap() + gw > width
            && !rows.last().unwrap().is_empty();
        // Cursor check before the wrap decision: when the row is exactly
        // full and the next grapheme is about to push a new row, a cursor
        // sitting at this byte position visually belongs to the start of
        // that new row, not the trailing edge of the full one (which would
        // collide with the right border).
        if !found_cursor && byte_pos >= cursor_byte_in_combined {
            if need_wrap {
                cursor_row = rows.len();
                cursor_col = 0;
            } else {
                cursor_row = rows.len() - 1;
                cursor_col = *row_widths.last().unwrap();
            }
            found_cursor = true;
        }
        if need_wrap {
            rows.push(String::new());
            row_widths.push(0);
        }
        if is_hard_break {
            // The newline itself is consumed by the layout; start a fresh
            // empty row so subsequent graphemes render at column 0.
            rows.push(String::new());
            row_widths.push(0);
        } else {
            rows.last_mut().unwrap().push_str(g);
            *row_widths.last_mut().unwrap() += gw;
        }
        byte_pos += g.len();
    }
    if !found_cursor {
        if *row_widths.last().unwrap() >= width {
            rows.push(String::new());
            row_widths.push(0);
        }
        cursor_row = rows.len() - 1;
        cursor_col = *row_widths.last().unwrap();
    }

    InputLayout {
        rows,
        cursor_row,
        cursor_col,
    }
}

/// Find the byte offset in `s` that corresponds to visual column `col`.
/// Accounts for wide characters (CJK = 2 cols). Returns `s.len()` if `col`
/// exceeds the string's visual width.
pub(super) fn byte_pos_at_visual_col(s: &str, col: usize) -> usize {
    let mut current = 0usize;
    for (i, ch) in s.char_indices() {
        if current >= col {
            return i;
        }
        current += unicode_column_width(&ch.to_string(), None);
    }
    s.len()
}

/// Pad `s` on the right with spaces until its visual column width reaches `target_cols`.
/// Unlike `format!("{:<width$}", ...)`, this counts visual columns, not chars.
pub(super) fn pad_to_visual_width(s: &str, target_cols: usize) -> String {
    let cur = unicode_column_width(s, None);
    if cur >= target_cols {
        return s.to_string();
    }
    let mut out = String::with_capacity(s.len() + (target_cols - cur));
    out.push_str(s);
    for _ in 0..(target_cols - cur) {
        out.push(' ');
    }
    out
}
