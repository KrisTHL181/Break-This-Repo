//! AI conversation overlay for Kaku.
//!
//! Activated via Cmd+L. Renders a full-pane chat TUI using raw termwiz
//! change sequences, communicating with the LLM via a background thread and
//! std::sync::mpsc for streaming tokens.

mod markdown;
mod prompt_context;
mod strings;
mod syntax;
mod waza;

mod input;
mod layout;
mod render;
mod state;
mod types;

// Vocabulary used in this overlay (see also `kaku-gui/AGENTS.md`):
// - **Soul** = persistent user-authored identity files + curator memory
//   (`crate::soul`). Loaded into the system prompt and a per-request env
//   message.
// - **Waza** = ephemeral slash-command "skill" prompts injected for one turn
//   (`overlay/ai_chat/waza.rs`).
// - **Assistant** = TOML role / config record (`assistant.toml`,
//   `kaku::assistant_config::AssistantConfig`).
// - **Agent** = the tool-call loop implementation
//   (`crate::ai_chat_engine::run_agent`). Reached directly from `state.rs`
//   via `crate::ai_chat_engine::run_agent` — there is no shim in this
//   module; the earlier `agent.rs` re-export was deleted because new
//   contributors mistook it for an overlay-local implementation.
//
// `prompt_context` carries the per-request environment + visible-terminal
// snapshot helpers. It is *not* the approval pipeline; that lives in
// `crate::ai_chat_engine::approval`. The earlier name (`approval.rs`) was
// renamed because it was misleading — new contributors looked here for the
// tool-approval logic and found prompt assembly instead.
//
// Sibling modules import what they need directly via `super::markdown::*`,
// `super::prompt_context::*`, etc. This module no longer re-exports those
// names because nothing reaches the overlay through `crate::overlay::ai_chat`
// anymore — the public surface is just `ai_chat_overlay()` plus `TerminalContext`
// (re-exported via `types::*` below for `frontend.rs` callers).
pub(crate) use types::*;

// Private imports needed by ai_chat_overlay.
use self::input::{handle_key, handle_mouse};
use self::layout::char_to_byte_pos;
use self::render::render;
use self::state::App;
use crate::ai_client::{AiClient, AssistantConfig};
use mux::pane::PaneId;
use mux::termwiztermtab::TermWizTerminal;
use std::sync::mpsc::Receiver;
use std::time::Duration;
use termwiz::cell::CellAttributes;
use termwiz::color::ColorAttribute;
use termwiz::input::InputEvent;
use termwiz::surface::{Change, Position};
use termwiz::terminal::Terminal;

// Items from sibling submodules that tests reach via `use super::super::*` or
// explicit path. Child modules can access private items of ancestor modules, so
// these private imports are visible to `tests.rs` and its submodules.
#[cfg(test)]
use self::markdown::{parse_markdown_blocks, segments_to_plain, tokenize_inline, wrap_segments};
#[cfg(test)]
use self::prompt_context::build_visible_snapshot_message;
#[cfg(test)]
use self::state::{
    build_cwd_attachment, format_user_message, push_input_snapshot, push_waza_instruction,
    resolve_input_attachments, slash_command_options_for_token, slash_command_submits_immediately,
};
#[cfg(test)]
use termwiz::cell::unicode_column_width;
#[cfg(test)]
use termwiz::color::SrgbaTuple;

#[cfg(test)]
mod tests;

pub fn ai_chat_overlay(
    _pane_id: PaneId,
    mut term: TermWizTerminal,
    context: TerminalContext,
    palette_rx: Receiver<ChatPalette>,
) -> anyhow::Result<()> {
    term.set_raw_mode()?;

    let size = term.get_screen_size()?;
    let cols = size.cols;
    let rows = size.rows;

    let client_cfg = match AssistantConfig::load() {
        Ok(c) => c,
        Err(e) => {
            // Show error briefly and exit
            term.render(&[
                Change::CursorPosition {
                    x: Position::Absolute(0),
                    y: Position::Absolute(0),
                },
                Change::Text(format!("Kaku AI: {}", e)),
            ])?;
            std::thread::sleep(Duration::from_secs(3));
            return Ok(());
        }
    };

    let chat_model = client_cfg.chat_model.clone();
    let chat_model_choices = client_cfg.chat_model_choices.clone();
    let fast_model = client_cfg.fast_model.clone();
    let client = AiClient::new(client_cfg);
    let mut app = App::new(
        context,
        chat_model,
        chat_model_choices,
        fast_model,
        cols,
        rows,
        client,
    );
    let mut needs_redraw = true;

    app.display_lines_dirty = true;

    loop {
        if drain_palette_updates(
            &palette_rx,
            &mut app.context.colors,
            &mut app.display_lines_dirty,
        ) {
            needs_redraw = true;
        }

        // Drain any streaming tokens first.
        if app.drain_tokens() {
            needs_redraw = true;
        }

        // Drain background model fetch result.
        if app.drain_model_fetch() {
            needs_redraw = true;
        }

        // Drain background /suggest result.
        if app.drain_suggest() {
            needs_redraw = true;
        }

        // Expire model status flash after 1.5 s.
        if app
            .model_status_flash
            .as_ref()
            .map_or(false, |(_, t)| t.elapsed() >= Duration::from_millis(1500))
        {
            app.model_status_flash = None;
            needs_redraw = true;
        }

        if needs_redraw {
            app.rebuild_display_cache();
            render(&mut term, &app)?;
            needs_redraw = false;
        }

        // Poll with a short timeout so we can check channels regularly.
        // Use shorter timeout when streaming, fetching models, or flashing status.
        let timeout = if app.is_streaming
            || !app.grapheme_queue.is_empty()
            || app.stream_pending_done
            || app.model_status_flash.is_some()
            || app.suggest_rx.is_some()
            || app.pending_approval.is_some()
            || matches!(app.model_fetch, ModelFetch::Loading)
        {
            Some(Duration::from_millis(30))
        } else {
            Some(Duration::from_millis(500))
        };

        match term.poll_input(timeout)? {
            Some(InputEvent::Key(key)) => {
                match handle_key(&key, &mut app) {
                    Action::Quit => {
                        if app.is_streaming || app.stream_pending_done {
                            if let Some(last) = app
                                .messages
                                .iter_mut()
                                .rev()
                                .find(|m| !m.is_tool() && !m.complete)
                            {
                                last.complete = true;
                            }
                        }
                        app.save_history();
                        break;
                    }
                    Action::Continue => {}
                }
                needs_redraw = true;
            }
            Some(InputEvent::Paste(text)) => {
                // IME composed text (e.g. Chinese, Japanese) arrives here via
                // ForwardWriter in TermWizTerminalPane, which converts bytes
                // written to pane.writer() into InputEvent::Paste events.
                // Allowed during streaming so the user can stage the next message.
                //
                // Shift+Enter / Cmd+Enter may also reach this path: users often
                // bind those to `SendString('\n')` in kaku.lua, which the
                // ForwardWriter delivers as `Paste("\n")`. Treat \r and \n as
                // literal newlines so multi-line composition works regardless
                // of how the byte stream entered the overlay.
                let normalized: String = text
                    .chars()
                    .filter_map(|c| match c {
                        '\r' | '\n' => Some('\n'),
                        c if !c.is_control() => Some(c),
                        _ => None,
                    })
                    .collect();
                if !normalized.is_empty() {
                    app.snapshot_input_for_undo();
                    for c in normalized.chars() {
                        let byte_pos = char_to_byte_pos(&app.input, app.input_cursor);
                        app.input.insert(byte_pos, c);
                        app.input_cursor += 1;
                    }
                    app.attachment_picker_index = 0;
                    app.display_lines_dirty = true;
                    needs_redraw = true;
                }
            }
            Some(InputEvent::Mouse(mouse)) => {
                handle_mouse(&mouse, &mut app);
                needs_redraw = true;
            }
            Some(InputEvent::Resized { cols, rows }) => {
                app.cols = cols;
                app.rows = rows;
                app.display_lines_dirty = true;
                needs_redraw = true;
            }
            Some(_) => {}
            None => {
                // Timeout: advance the spinner whenever a loader is on screen.
                let spinner_changed = (app.is_streaming
                    || app.pending_approval.is_some()
                    || matches!(app.model_fetch, ModelFetch::Loading))
                    && app.try_advance_spinner();
                if app.is_streaming
                    || !app.grapheme_queue.is_empty()
                    || app.stream_pending_done
                    || spinner_changed
                {
                    needs_redraw = true;
                }
            }
        }
    }

    // Clear screen before handing control back to the terminal.
    term.render(&[
        Change::AllAttributes(CellAttributes::default()),
        Change::ClearScreen(ColorAttribute::Default),
    ])?;

    crate::ai_tools::cleanup_spill_files();

    Ok(())
}

fn drain_palette_updates(
    palette_rx: &Receiver<ChatPalette>,
    colors: &mut ChatPalette,
    display_lines_dirty: &mut bool,
) -> bool {
    let mut latest = None;
    while let Ok(palette) = palette_rx.try_recv() {
        latest = Some(palette);
    }
    let Some(palette) = latest else {
        return false;
    };

    *colors = palette;
    *display_lines_dirty = true;
    true
}
