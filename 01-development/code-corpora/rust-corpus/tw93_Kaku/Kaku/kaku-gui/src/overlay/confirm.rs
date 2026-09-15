use crate::scripting::guiwin::GuiWin;
use config::keyassignment::{Confirmation, KeyAssignment};
use mux::termwiztermtab::TermWizTerminal;
use mux_lua::MuxPane;
use std::rc::Rc;
use termwiz::cell::{unicode_column_width, AttributeChange, Intensity};
use termwiz::color::ColorAttribute;
use termwiz::input::{InputEvent, KeyCode, KeyEvent, MouseButtons, MouseEvent};
use termwiz::surface::{Change, CursorVisibility, Position};
use termwiz::terminal::Terminal;

pub fn run_confirmation(message: &str, term: &mut TermWizTerminal) -> anyhow::Result<bool> {
    run_confirmation_impl(message, term)
}

#[derive(Copy, Clone, PartialEq, Eq)]
enum ActiveButton {
    None,
    Yes,
    No,
}

struct ButtonLayout {
    x: usize,
    width: usize,
}

fn run_confirmation_impl(message: &str, term: &mut TermWizTerminal) -> anyhow::Result<bool> {
    term.set_raw_mode()?;

    let size = term.get_screen_size()?;
    let yes_label = "[Y] Confirm";
    let no_label = "[N] Cancel";
    let horizontal_padding = 2;
    let button_gap = 3;
    let max_dialog_width = size.cols.saturating_sub(6).max(4);
    let max_content_width = max_dialog_width
        .saturating_sub(horizontal_padding * 2)
        .saturating_sub(2)
        .max(1);
    let wrap_width = max_content_width.min(52);

    let options = textwrap::Options::new(wrap_width).break_words(false);
    let mut wrapped = Vec::new();
    for line in message.lines() {
        if line.is_empty() {
            wrapped.push(String::new());
            continue;
        }
        for row in textwrap::wrap(line, &options) {
            wrapped.push(row.into_owned());
        }
    }
    if wrapped.is_empty() {
        wrapped.push(String::new());
    }

    let message_width = wrapped
        .iter()
        .map(|line| unicode_column_width(line, None))
        .max()
        .unwrap_or(0);
    let yes_w = unicode_column_width(yes_label, None);
    let no_w = unicode_column_width(no_label, None);
    let button_group_width = no_w + button_gap + yes_w;
    let desired_content_width = message_width.max(button_group_width);
    let dialog_width = (desired_content_width + horizontal_padding * 2 + 2).min(size.cols.max(4));
    let inner_width = dialog_width.saturating_sub(2);
    let content_x = horizontal_padding + 1;
    let content_area_width = inner_width.saturating_sub(horizontal_padding * 2);
    let x_pos = (size.cols.saturating_sub(dialog_width)) / 2;

    let dialog_height = wrapped.len() + 5;
    let top_row = (size.rows.saturating_sub(dialog_height)) / 2;
    let button_row = top_row + wrapped.len() + 3;
    let mut active = ActiveButton::None;

    let button_group_x =
        x_pos + content_x + (content_area_width.saturating_sub(button_group_width)) / 2;
    let no_button = ButtonLayout {
        x: button_group_x,
        width: no_w,
    };
    let yes_button = ButtonLayout {
        x: no_button.x + no_w + button_gap,
        width: yes_w,
    };

    let render = |term: &mut TermWizTerminal, active: ActiveButton| -> termwiz::Result<()> {
        let mut changes = vec![
            Change::ClearScreen(ColorAttribute::Default),
            Change::CursorVisibility(CursorVisibility::Hidden),
        ];

        changes.push(Change::CursorPosition {
            x: Position::Absolute(x_pos),
            y: Position::Absolute(top_row),
        });
        changes.push(Change::Text(format!(
            "╭{}╮",
            "─".repeat(dialog_width.saturating_sub(2))
        )));

        for y in 1..dialog_height.saturating_sub(1) {
            changes.push(Change::CursorPosition {
                x: Position::Absolute(x_pos),
                y: Position::Absolute(top_row + y),
            });
            changes.push(Change::Text(format!(
                "│{}│",
                " ".repeat(dialog_width.saturating_sub(2))
            )));
        }

        changes.push(Change::CursorPosition {
            x: Position::Absolute(x_pos),
            y: Position::Absolute(top_row + dialog_height.saturating_sub(1)),
        });
        changes.push(Change::Text(format!(
            "╰{}╯",
            "─".repeat(dialog_width.saturating_sub(2))
        )));

        for (y, row) in wrapped.iter().enumerate() {
            let row_width = unicode_column_width(row, None);
            let row_x = x_pos + content_x + (content_area_width.saturating_sub(row_width)) / 2;
            changes.push(Change::CursorPosition {
                x: Position::Absolute(row_x),
                y: Position::Absolute(top_row + 2 + y),
            });
            changes.push(Change::Text(row.to_string()));
        }

        changes.push(Change::CursorPosition {
            x: Position::Absolute(no_button.x),
            y: Position::Absolute(button_row),
        });
        if active == ActiveButton::No {
            changes.push(AttributeChange::Reverse(true).into());
            changes.push(AttributeChange::Intensity(Intensity::Bold).into());
        }
        changes.push(Change::Text(no_label.to_string()));
        if active == ActiveButton::No {
            changes.push(Change::AllAttributes(Default::default()));
        }

        changes.push(Change::CursorPosition {
            x: Position::Absolute(yes_button.x),
            y: Position::Absolute(button_row),
        });
        if active == ActiveButton::Yes {
            changes.push(AttributeChange::Reverse(true).into());
            changes.push(AttributeChange::Intensity(Intensity::Bold).into());
        }
        changes.push(Change::Text(yes_label.to_string()));
        if active == ActiveButton::Yes {
            changes.push(Change::AllAttributes(Default::default()));
        }

        term.render(&changes)?;
        term.flush()
    };

    render(term, active)?;

    while let Ok(Some(event)) = term.poll_input(None) {
        match event {
            InputEvent::Key(KeyEvent {
                key: KeyCode::Char('y' | 'Y'),
                ..
            }) => {
                return Ok(true);
            }
            InputEvent::Key(KeyEvent {
                key: KeyCode::Enter,
                ..
            }) => {
                return Ok(true);
            }
            InputEvent::Key(KeyEvent {
                key: KeyCode::Char('n' | 'N'),
                ..
            })
            | InputEvent::Key(KeyEvent {
                key: KeyCode::Escape,
                ..
            }) => {
                return Ok(false);
            }
            InputEvent::Mouse(MouseEvent {
                x,
                y,
                mouse_buttons,
                ..
            }) => {
                let x = x as usize;
                let y = y as usize;
                if y == button_row && x >= yes_button.x && x < yes_button.x + yes_button.width {
                    active = ActiveButton::Yes;
                    if mouse_buttons == MouseButtons::LEFT {
                        return Ok(true);
                    }
                } else if y == button_row && x >= no_button.x && x < no_button.x + no_button.width {
                    active = ActiveButton::No;
                    if mouse_buttons == MouseButtons::LEFT {
                        return Ok(false);
                    }
                } else {
                    active = ActiveButton::None;
                }

                if mouse_buttons != MouseButtons::NONE {
                    // Treat any other mouse button as cancel
                    return Ok(false);
                }
            }
            _ => {}
        }

        render(term, active)?;
    }

    Ok(false)
}

pub fn show_confirmation_overlay(
    mut term: TermWizTerminal,
    args: Confirmation,
    window: GuiWin,
    pane: MuxPane,
) -> anyhow::Result<()> {
    let name = match *args.action {
        KeyAssignment::EmitEvent(id) => id,
        _ => anyhow::bail!("Confirmation requires action to be defined by action_callback"),
    };

    if let Ok(confirm) = run_confirmation_impl(&args.message, &mut term) {
        if confirm {
            promise::spawn::spawn_into_main_thread(async move {
                trampoline(name, window, pane);
                anyhow::Result::<()>::Ok(())
            })
            .detach();
        } else if let Some(key_assignment) = args.cancel {
            if let KeyAssignment::EmitEvent(id) = *key_assignment {
                promise::spawn::spawn_into_main_thread(async move {
                    trampoline(id, window, pane);
                    anyhow::Result::<()>::Ok(())
                })
                .detach();
            }
        }
    }
    Ok(())
}

fn trampoline(name: String, window: GuiWin, pane: MuxPane) {
    promise::spawn::spawn(async move {
        config::with_lua_config_on_main_thread(move |lua| do_event(lua, name, window, pane)).await
    })
    .detach();
}

async fn do_event(
    lua: Option<Rc<mlua::Lua>>,
    name: String,
    window: GuiWin,
    pane: MuxPane,
) -> anyhow::Result<()> {
    if let Some(lua) = lua {
        let args = lua.pack_multi((window, pane))?;

        if let Err(err) = config::lua::emit_event(&lua, (name.clone(), args)).await {
            log::error!("while processing {} event: {:#}", name, err);
        }
    }

    Ok(())
}
