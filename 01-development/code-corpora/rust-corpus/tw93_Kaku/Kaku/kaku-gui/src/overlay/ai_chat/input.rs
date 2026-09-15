use std::time::Instant;
use termwiz::input::{KeyCode, KeyEvent, Modifiers, MouseButtons, MouseEvent};

use super::layout::{byte_pos_at_visual_col, char_to_byte_pos, next_word_pos, prev_word_pos};
use super::markdown::segments_to_plain;
use super::render::format_tool_suffix;
use super::state::App;
use super::types::*;

pub(super) fn handle_key(key: &KeyEvent, app: &mut App) -> Action {
    // Picker mode: route to dedicated handler.
    if matches!(app.mode, AppMode::ResumePicker { .. }) {
        return handle_key_picker(key, app);
    }

    // Any key that isn't Cmd+C dismisses the current selection.
    let is_copy = matches!(
        (&key.key, key.modifiers),
        (KeyCode::Char('c') | KeyCode::Char('C'), Modifiers::SUPER)
    );
    if !is_copy && app.selection.is_some() {
        app.selection = None;
        app.selecting = false;
    }

    // Handle approval prompt: Enter = approve, Esc = reject, other keys ignored.
    // Esc is captured here so it rejects the tool call rather than exiting the chat.
    if let Some((summary, reply_tx)) = app.pending_approval.take() {
        let is_approve = matches!((&key.key, key.modifiers), (KeyCode::Enter, Modifiers::NONE));
        let is_reject = matches!((&key.key, key.modifiers), (KeyCode::Escape, _));
        if is_approve {
            let _ = reply_tx.send(true);
            return Action::Continue;
        } else if is_reject {
            let _ = reply_tx.send(false);
            return Action::Continue;
        } else {
            // Other key: restore the approval state and ignore the key.
            app.pending_approval = Some((summary, reply_tx));
            return Action::Continue;
        }
    }

    let slash_options = if app.is_streaming {
        Vec::new()
    } else {
        app.slash_picker_options()
    };
    let picker_options = if app.is_streaming {
        Vec::new()
    } else {
        app.attachment_picker_options()
    };
    let picker_exact_match = app
        .current_attachment_query()
        .is_some_and(|(_, _, token)| picker_options.iter().any(|option| option.label == token));

    match (&key.key, key.modifiers) {
        // Escape / Ctrl+C: if streaming, first press cancels; second press exits.
        (KeyCode::Escape, _) | (KeyCode::Char('C'), Modifiers::CTRL) => {
            if app.is_streaming || !app.grapheme_queue.is_empty() {
                app.cancel_stream();
                Action::Continue
            } else {
                Action::Quit
            }
        }

        // Submit: built-in control commands execute immediately. Waza commands
        // only complete the command and leave the cursor ready for arguments.
        (KeyCode::Enter, Modifiers::NONE) if !slash_options.is_empty() => {
            let submits_immediately = app
                .selected_slash_command()
                .is_some_and(super::state::slash_command_submits_immediately);
            app.accept_slash_picker();
            if submits_immediately {
                app.submit();
            }
            Action::Continue
        }
        (KeyCode::Enter, Modifiers::NONE) if !picker_options.is_empty() && !picker_exact_match => {
            app.accept_attachment_picker();
            Action::Continue
        }
        (KeyCode::Enter, Modifiers::NONE) if !app.is_streaming => {
            app.submit();
            Action::Continue
        }
        // Streaming + non-empty input: interrupt current stream and submit immediately.
        (KeyCode::Enter, Modifiers::NONE) => {
            if !app.input.trim().is_empty() {
                app.cancel_stream();
                if let Some(last) = app
                    .messages
                    .iter_mut()
                    .rev()
                    .find(|m| !m.is_tool() && !m.complete)
                {
                    last.complete = true;
                }
                app.scroll_offset = 0;
                app.submit();
            } else if app.scroll_offset > 0 {
                app.scroll_offset = 0;
            }
            Action::Continue
        }
        // Shift+Enter inserts a literal newline so the user can compose
        // multi-line prompts without submitting.
        (KeyCode::Enter, Modifiers::SHIFT) => {
            app.snapshot_input_for_undo();
            let byte_pos = char_to_byte_pos(&app.input, app.input_cursor);
            app.input.insert(byte_pos, '\n');
            app.input_cursor += 1;
            app.attachment_picker_index = 0;
            Action::Continue
        }
        (KeyCode::Enter, _) => Action::Continue,

        // Cmd+Backspace: clear the entire input line (macOS-native shortcut).
        (KeyCode::Backspace, Modifiers::SUPER) => {
            app.snapshot_input_for_undo();
            app.input.clear();
            app.input_cursor = 0;
            app.attachment_picker_index = 0;
            Action::Continue
        }

        // Option+Backspace: delete the previous word (macOS-native shortcut).
        (KeyCode::Backspace, Modifiers::ALT) => {
            if app.input_cursor > 0 {
                app.snapshot_input_for_undo();
                let target = prev_word_pos(&app.input, app.input_cursor);
                let from_byte = char_to_byte_pos(&app.input, target);
                let to_byte = char_to_byte_pos(&app.input, app.input_cursor);
                app.input.drain(from_byte..to_byte);
                app.input_cursor = target;
                app.attachment_picker_index = 0;
            }
            Action::Continue
        }

        // Backspace
        (KeyCode::Backspace, _) => {
            if app.input_cursor > 0 {
                let byte_pos = char_to_byte_pos(&app.input, app.input_cursor - 1);
                let next_pos = char_to_byte_pos(&app.input, app.input_cursor);
                app.input.drain(byte_pos..next_pos);
                app.input_cursor -= 1;
                app.attachment_picker_index = 0;
            }
            Action::Continue
        }

        // Clear line
        (KeyCode::Char('U'), Modifiers::CTRL) => {
            app.snapshot_input_for_undo();
            app.input.clear();
            app.input_cursor = 0;
            app.attachment_picker_index = 0;
            Action::Continue
        }

        // Toggle thinking/reasoning content visibility (Ctrl+O, matching Claude Code).
        (KeyCode::Char('O'), Modifiers::CTRL) => {
            app.show_thinking = !app.show_thinking;
            app.display_lines_dirty = true;
            Action::Continue
        }

        // Undo the last destructive input edit (macOS-native Cmd+Z).
        (KeyCode::Char('z'), Modifiers::SUPER) | (KeyCode::Char('Z'), Modifiers::SUPER) => {
            app.undo_input();
            Action::Continue
        }

        // Jump to start/end of line (readline standard)
        (KeyCode::Char('A'), Modifiers::CTRL) => {
            app.input_cursor = 0;
            app.attachment_picker_index = 0;
            Action::Continue
        }
        (KeyCode::Char('E'), Modifiers::CTRL) => {
            app.input_cursor = app.input.chars().count();
            app.attachment_picker_index = 0;
            Action::Continue
        }

        // Cmd+W: close the AI chat overlay (restores normal window close behavior).
        (KeyCode::Char('w'), Modifiers::SUPER) | (KeyCode::Char('W'), Modifiers::SUPER) => {
            Action::Quit
        }

        // Copy selection to clipboard (Cmd+C on macOS)
        (KeyCode::Char('c'), Modifiers::SUPER) | (KeyCode::Char('C'), Modifiers::SUPER) => {
            if let Some(text) = extract_selection_text(app) {
                if !text.is_empty() {
                    copy_to_clipboard(&text);
                    app.model_status_flash = Some(("copied".to_string(), Instant::now()));
                }
            }
            Action::Continue
        }

        // Scroll up/down in message history
        (KeyCode::UpArrow, _) if !slash_options.is_empty() => {
            app.move_slash_picker(-1);
            Action::Continue
        }
        (KeyCode::DownArrow, _) if !slash_options.is_empty() => {
            app.move_slash_picker(1);
            Action::Continue
        }
        (KeyCode::UpArrow, _) if !picker_options.is_empty() => {
            app.move_attachment_picker(-1);
            Action::Continue
        }
        (KeyCode::DownArrow, _) if !picker_options.is_empty() => {
            app.move_attachment_picker(1);
            Action::Continue
        }
        (KeyCode::UpArrow, _) | (KeyCode::PageUp, _) => {
            let total = app.display_lines().len();
            let max_offset = total.saturating_sub(app.msg_area_height());
            app.scroll_offset = app.scroll_offset.saturating_add(3).min(max_offset);
            Action::Continue
        }
        (KeyCode::DownArrow, _) | (KeyCode::PageDown, _) => {
            app.scroll_offset = app.scroll_offset.saturating_sub(3);
            Action::Continue
        }

        // Cmd+Left / Cmd+Right: jump to start / end of input.
        (KeyCode::LeftArrow, Modifiers::SUPER) => {
            app.input_cursor = 0;
            app.attachment_picker_index = 0;
            Action::Continue
        }
        (KeyCode::RightArrow, Modifiers::SUPER) => {
            app.input_cursor = app.input.chars().count();
            app.attachment_picker_index = 0;
            Action::Continue
        }

        // Option+Left / Option+Right: jump by word.
        (KeyCode::LeftArrow, Modifiers::ALT) => {
            app.input_cursor = prev_word_pos(&app.input, app.input_cursor);
            app.attachment_picker_index = 0;
            Action::Continue
        }
        (KeyCode::RightArrow, Modifiers::ALT) => {
            app.input_cursor = next_word_pos(&app.input, app.input_cursor);
            app.attachment_picker_index = 0;
            Action::Continue
        }

        // Left / Right cursor movement
        (KeyCode::LeftArrow, _) => {
            if app.input_cursor > 0 {
                app.input_cursor -= 1;
            }
            app.attachment_picker_index = 0;
            Action::Continue
        }
        (KeyCode::RightArrow, _) => {
            let len = app.input.chars().count();
            if app.input_cursor < len {
                app.input_cursor += 1;
            }
            app.attachment_picker_index = 0;
            Action::Continue
        }

        (KeyCode::Tab, Modifiers::NONE) | (KeyCode::Char('\t'), Modifiers::NONE)
            if !slash_options.is_empty() =>
        {
            app.accept_slash_picker();
            Action::Continue
        }
        (KeyCode::Tab, Modifiers::NONE) | (KeyCode::Char('\t'), Modifiers::NONE)
            if !picker_options.is_empty() =>
        {
            app.accept_attachment_picker();
            Action::Continue
        }

        // Shift+Tab: rotate through available chat models.
        // macOS rewrites Shift+Tab to KeyCode::Tab + Modifiers::SHIFT (window.rs:4168).
        (KeyCode::Tab, Modifiers::SHIFT) | (KeyCode::Char('\t'), Modifiers::SHIFT) => {
            if !app.is_streaming {
                match &app.model_fetch {
                    ModelFetch::Loading => {
                        // Fetch in progress; indicate visually.
                        app.model_status_flash =
                            Some(("loading models…".to_string(), Instant::now()));
                    }
                    ModelFetch::Failed(e) => {
                        let msg = format!("fetch failed: {}", e);
                        app.model_status_flash = Some((msg, Instant::now()));
                    }
                    ModelFetch::Loaded => {
                        let n = app.available_models.len();
                        if n > 1 {
                            // Cycle so the user can keep pressing Shift+Tab past
                            // the last model and wrap back to the first.
                            app.model_index = (app.model_index + 1) % n;
                            // Persist the selection so it survives overlay close/reopen.
                            let model = app.current_model();
                            if let Err(e) = crate::ai_state::save_last_model(&model) {
                                log::warn!("Failed to save model selection: {e}");
                            }
                        }
                    }
                }
            }
            Action::Continue
        }

        // Regular character input (skip control characters like \t handled above).
        // Allowed during streaming so the user can stage the next message.
        (KeyCode::Char(c), Modifiers::NONE) | (KeyCode::Char(c), Modifiers::SHIFT)
            if !c.is_control() =>
        {
            let byte_pos = char_to_byte_pos(&app.input, app.input_cursor);
            app.input.insert(byte_pos, *c);
            app.input_cursor += 1;
            app.attachment_picker_index = 0;
            Action::Continue
        }

        _ => Action::Continue,
    }
}

fn handle_key_picker(key: &KeyEvent, app: &mut App) -> Action {
    let (items, cursor) = match &app.mode {
        AppMode::ResumePicker { items, cursor } => (items.clone(), *cursor),
        _ => return Action::Continue,
    };

    match (&key.key, key.modifiers) {
        (KeyCode::Escape, _) => {
            app.mode = AppMode::Chat;
            Action::Continue
        }
        (KeyCode::UpArrow, _) => {
            if cursor > 0 {
                app.mode = AppMode::ResumePicker {
                    items,
                    cursor: cursor - 1,
                };
            }
            Action::Continue
        }
        (KeyCode::DownArrow, _) => {
            if cursor + 1 < items.len() {
                app.mode = AppMode::ResumePicker {
                    items,
                    cursor: cursor + 1,
                };
            }
            Action::Continue
        }
        (KeyCode::Enter, _) => {
            app.load_conversation_from_picker(cursor);
            Action::Continue
        }
        _ => Action::Continue,
    }
}

pub(super) fn handle_mouse(event: &MouseEvent, app: &mut App) {
    // Scroll wheel support
    if event.mouse_buttons.contains(MouseButtons::VERT_WHEEL) {
        if event.mouse_buttons.contains(MouseButtons::WHEEL_POSITIVE) {
            let total = app.display_lines().len();
            let max_offset = total.saturating_sub(app.msg_area_height());
            app.scroll_offset = app.scroll_offset.saturating_add(2).min(max_offset);
        } else {
            app.scroll_offset = app.scroll_offset.saturating_sub(2);
        }
        return;
    }

    // Mouse selection: row 0 is the top border, rows 1..msg_row_end are the
    // message area. The input box can span multiple rows now, so msg_row_end
    // tracks the separator row (which floats up as input grows).
    let input_visible_h = app.input_visible_rows();
    let msg_row_start = 1usize; // first message row (0 is top border)
    let msg_row_end = app.rows.saturating_sub(2 + input_visible_h); // exclusive

    let mx = event.x as usize;
    let my = event.y as usize;
    let in_msg_area = my >= msg_row_start && my < msg_row_end;

    // Convert absolute mouse row to display-line index accounting for scroll.
    // Pre-compute the values the closure needs so we avoid a long-lived borrow.
    let all_lines = app.display_lines().len();
    let msg_area_h = app.msg_area_height();
    let scroll_offset = app.scroll_offset;
    let to_line_idx = |row: usize| -> usize {
        let visible_start = if all_lines <= msg_area_h {
            0
        } else {
            (all_lines - msg_area_h).saturating_sub(scroll_offset)
        };
        visible_start + row.saturating_sub(msg_row_start)
    };

    // termwiz maps both Button1Press and Button1Drag to MouseButtons::LEFT, so
    // we cannot distinguish press from drag by checking the current event alone.
    // Track the previous frame's state and act on the edge transition instead.
    let is_pressed = event.mouse_buttons.contains(MouseButtons::LEFT);
    let was_pressed = app.left_was_pressed;
    app.left_was_pressed = is_pressed;

    // Input box spans rows [input_top .. bot_row); used to detect clicks
    // anywhere in the (possibly multi-row) input area.
    let input_top = app.rows.saturating_sub(1 + input_visible_h);
    let bot_row = app.rows.saturating_sub(1);

    match (was_pressed, is_pressed) {
        (false, true) => {
            // Press edge: start a new potential selection, clear the old one.
            app.selection = None;
            app.selecting = false;
            app.drag_origin = if in_msg_area {
                Some((to_line_idx(my), mx))
            } else {
                None
            };
            // A click anywhere on the input box during streaming reveals
            // the cursor so the user can stage the next message.
            if my >= input_top && my < bot_row && app.is_streaming {
                app.input_clicked_this_stream = true;
            }
        }
        (true, true) => {
            // Drag: extend the selection if the cursor has actually moved from the anchor.
            if let Some((orig_row, orig_col)) = app.drag_origin {
                if in_msg_area {
                    let line_idx = to_line_idx(my);
                    if app.selecting {
                        if let Some(ref mut sel) = app.selection {
                            sel.2 = line_idx;
                            sel.3 = mx;
                        }
                    } else if line_idx != orig_row || mx != orig_col {
                        app.selection = Some((orig_row, orig_col, line_idx, mx));
                        app.selecting = true;
                    }
                }
            }
        }
        (true, false) => {
            // Release edge: finalize the selection and auto-copy to clipboard.
            // Require at least 5 chars OR multi-word to avoid clobbering clipboard
            // on accidental single-character selections.
            app.selecting = false;
            if app.selection.is_some() {
                if let Some(text) = extract_selection_text(app) {
                    let chars = text.chars().count();
                    let multi_word = text.split_whitespace().count() >= 2;
                    if chars >= 5 || multi_word {
                        copy_to_clipboard(&text);
                        app.model_status_flash = Some(("copied".to_string(), Instant::now()));
                    }
                }
            }
        }
        (false, false) => {}
    }
}

/// Extract the text covered by the current selection, if any.
fn extract_selection_text(app: &App) -> Option<String> {
    let (mut r0, mut c0, mut r1, mut c1) = app.selection?;

    // Normalize so r0 <= r1
    if r0 > r1 || (r0 == r1 && c0 > c1) {
        std::mem::swap(&mut r0, &mut r1);
        std::mem::swap(&mut c0, &mut c1);
    }

    let lines = app.display_lines();
    if r0 >= lines.len() {
        return None;
    }
    let r1 = r1.min(lines.len().saturating_sub(1));

    let mut result = String::new();
    for (i, line) in lines.iter().enumerate().skip(r0).take(r1 - r0 + 1) {
        // Reconstruct the exact string render() places on this row so that
        // selection column math stays consistent with what the user sees.
        // Returns (rendered_string, render_prefix) so the prefix can be stripped on copy.
        let (rendered, render_prefix): (String, &str) = match line {
            DisplayLine::Header {
                role: Role::User, ..
            } => ("  You".into(), "  "),
            DisplayLine::Header {
                role: Role::Assistant,
                tools,
            } => {
                let mut s = "  AI".to_string();
                s.push_str(&format_tool_suffix(tools, "●"));
                (s, "  ")
            }
            DisplayLine::AttachmentSummary { labels } => {
                (format!("  Attached: {}", labels.join(" ")), "  ")
            }
            DisplayLine::Text {
                segments,
                role,
                block,
            } => {
                let indent = match (role, block) {
                    (Role::Assistant, BlockStyle::Quote) => "  │ ",
                    (Role::Assistant, BlockStyle::ListContinuation) => "    ",
                    _ => "  ",
                };
                (format!("{}{}", indent, segments_to_plain(segments)), indent)
            }
            DisplayLine::LoadingDot => (String::new(), ""),
            DisplayLine::ThinkingHeader { .. } => (String::new(), ""),
            DisplayLine::Blank => (String::new(), ""),
        };

        let total_w = termwiz::cell::unicode_column_width(&rendered, None);
        // Terminal col → content col (col 0 is the left border │, col 1 is first content col).
        let sc = if i == r0 { c0.saturating_sub(1) } else { 0 };
        let ec = if i == r1 {
            c1.saturating_sub(1)
        } else {
            total_w
        };

        let sc_byte = byte_pos_at_visual_col(&rendered, sc);
        let ec_byte = byte_pos_at_visual_col(&rendered, ec).min(rendered.len());
        let slice = &rendered[sc_byte..ec_byte];
        // Strip only the exact render prefix (not all leading spaces) so that
        // code indentation beyond the prefix is preserved on copy.
        result.push_str(slice.strip_prefix(render_prefix).unwrap_or(slice));
        if i < r1 {
            result.push('\n');
        }
    }
    Some(result)
}

/// Copy text to the system clipboard via pbcopy (macOS).
/// Spawns a detached thread so the chat thread is never blocked by IPC.
pub(super) fn copy_to_clipboard(text: &str) {
    use std::io::Write;
    use std::process::{Command, Stdio};

    let text = text.to_string();
    crate::thread_util::spawn_with_pool(move || {
        let mut child = match Command::new("pbcopy").stdin(Stdio::piped()).spawn() {
            Ok(c) => c,
            Err(e) => {
                log::warn!("Failed to spawn pbcopy: {e}");
                return;
            }
        };
        if let Some(mut stdin) = child.stdin.take() {
            let _ = stdin.write_all(text.as_bytes());
        }
        // stdin drop closes the pipe; pbcopy exits naturally.
        let _ = child.wait();
    });
}
