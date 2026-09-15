use ratatui::layout::{Constraint, Layout, Margin, Rect};
use ratatui::style::{Modifier, Style};
use ratatui::text::{Line, Span};
use ratatui::widgets::{Block, Borders, Clear, List, ListItem, ListState, Paragraph};

use super::{App, Tool};
use crate::tui_core::theme::{accent, bg, muted, panel, primary, red, success, text_fg};

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
enum MainLayoutMode {
    HeaderOnly,
    HeaderAndFooter,
    Expanded,
    Compact,
}

pub(super) fn ui(frame: &mut ratatui::Frame, app: &mut App) {
    let full = frame.area();
    if full.width < 2 || full.height < 2 {
        return;
    }
    // Use the full frame so background and popup layers stay visually consistent
    // with the active theme across the whole terminal width.
    let area = full;

    // Clear frame content first to avoid stale glyph artifacts when redrawing
    // in non-alternate-screen mode.
    frame.render_widget(Clear, area);
    frame.render_widget(Block::default().style(Style::default().bg(bg())), area);

    let rendered_tool_rows = app.rendered_tool_row_count() as u16;
    match resolve_main_layout(area.height, rendered_tool_rows) {
        MainLayoutMode::HeaderOnly => {
            let chunks = Layout::vertical([Constraint::Length(2)]).split(area);
            render_header(frame, chunks[0], None);
        }
        MainLayoutMode::HeaderAndFooter => {
            let chunks =
                Layout::vertical([Constraint::Length(2), Constraint::Length(1)]).split(area);
            render_header(frame, chunks[0], None);
            render_status_bar(frame, chunks[1], app);
        }
        MainLayoutMode::Expanded => {
            let chunks = Layout::vertical([
                Constraint::Length(2),                  // logo header
                Constraint::Length(rendered_tool_rows), // tool list
                Constraint::Fill(1),                    // flexible gap above status
                Constraint::Length(1),                  // spacer above status
                Constraint::Length(1),                  // status bar (stick to bottom)
            ])
            .split(area);

            render_header(frame, chunks[0], None);
            render_tools(frame, chunks[1], app);
            render_status_bar(frame, chunks[4], app);
        }
        MainLayoutMode::Compact => {
            let chunks = Layout::vertical([
                Constraint::Length(2), // logo header
                Constraint::Fill(1),   // tool list
                Constraint::Length(1), // spacer above status
                Constraint::Length(1), // status bar (stick to bottom)
            ])
            .split(area);

            render_header(frame, chunks[0], None);
            render_tools(frame, chunks[1], app);
            render_status_bar(frame, chunks[3], app);
        }
    }

    if app.is_selecting() {
        render_selector(frame, area, app);
    } else if app.is_editing() {
        render_editor(frame, area, app);
    }
}

fn resolve_main_layout(area_height: u16, content_rows: u16) -> MainLayoutMode {
    let remaining_height = area_height.saturating_sub(2);
    if remaining_height == 0 {
        MainLayoutMode::HeaderOnly
    } else if remaining_height == 1 {
        MainLayoutMode::HeaderAndFooter
    } else if content_rows + 2 <= remaining_height {
        MainLayoutMode::Expanded
    } else {
        MainLayoutMode::Compact
    }
}

fn render_header(frame: &mut ratatui::Frame, area: Rect, status: Option<&str>) {
    let mut spans = vec![
        Span::styled(
            "  Kaku",
            Style::default().fg(primary()).add_modifier(Modifier::BOLD),
        ),
        Span::styled(" · ", Style::default().fg(muted())),
        Span::styled("AI", Style::default().fg(text_fg())),
    ];
    if let Some(status) = status {
        spans.push(Span::styled("  ", Style::default()));
        spans.push(Span::styled(status, Style::default().fg(muted())));
    }
    let line = Line::from(spans);
    frame.render_widget(Paragraph::new(vec![line, Line::from("")]), area);
}

fn render_tools(frame: &mut ratatui::Frame, area: Rect, app: &App) {
    let mut items: Vec<ListItem> = Vec::new();
    let mut selected_flat: Option<usize> = None;
    let mut flat = 0usize;

    for (ti, tool) in app.tools.iter().enumerate() {
        let is_current_tool = ti == app.tool_index;
        let is_collapsed = app.tool_is_collapsed(ti);
        let header_selected =
            is_current_tool && tool.tool == Tool::KakuAssistant && app.field_index == 0;

        let tool_style = if header_selected || (is_current_tool && tool.tool != Tool::KakuAssistant)
        {
            Style::default().fg(primary()).add_modifier(Modifier::BOLD)
        } else if tool.installed {
            Style::default().fg(success()).add_modifier(Modifier::BOLD)
        } else {
            Style::default().fg(muted())
        };

        let mut header = Line::from(vec![
            Span::styled(
                if header_selected || (is_current_tool && tool.tool != Tool::KakuAssistant) {
                    "➤ "
                } else {
                    "  "
                },
                Style::default().fg(primary()).add_modifier(Modifier::BOLD),
            ),
            Span::styled(tool.tool.label(), tool_style),
        ]);
        if tool.tool == Tool::KakuAssistant {
            header.spans.push(Span::styled(
                if is_collapsed { " ▸" } else { " ▾" },
                Style::default().fg(if is_current_tool { primary() } else { muted() }),
            ));
        }
        if let Some(summary) = &tool.summary {
            header.spans.push(Span::styled("  ", Style::default()));
            header
                .spans
                .push(Span::styled(summary, Style::default().fg(text_fg())));
        } else if !tool.installed {
            header.spans.push(Span::styled(
                "  not installed",
                Style::default().fg(muted()),
            ));
        }
        items.push(ListItem::new(header));
        if header_selected {
            selected_flat = Some(flat);
        }
        flat += 1;

        if is_collapsed {
            items.push(ListItem::new(Line::raw("")));
            flat += 1;
            continue;
        }

        for (fi, field) in tool.fields.iter().enumerate() {
            let display_index = if tool.tool == Tool::KakuAssistant {
                fi + 1
            } else {
                fi
            };
            let is_selected = is_current_tool && display_index == app.field_index;
            if is_selected {
                selected_flat = Some(flat);
            }

            let last = fi == tool.fields.len() - 1;
            let connector = if last { "└" } else { "├" };
            let rule = "─";

            let val_color = if field.value.starts_with('✓')
                || (field.key.contains("API Key") && field.value != "-")
            {
                success()
            } else if field.value.starts_with('✗') {
                red()
            } else if field.value == "-" {
                muted()
            } else {
                accent()
            };

            let (display_key, extra_indent) = if let Some(pos) = field.key.find(" ▸ ") {
                (format!("↳ {}", &field.key[pos + " ▸ ".len()..]), true)
            } else {
                (field.key.clone(), false)
            };

            let indent_str = if extra_indent { "    │  " } else { "    " };
            let key_width = if extra_indent { 21 } else { 24 };
            let tree_color = if is_selected { primary() } else { muted() };
            let row_color = if is_selected { primary() } else { text_fg() };
            let key_marker = if is_selected { "› " } else { "  " };

            let val_prefix = if field.value.starts_with('✓') || field.value.starts_with('✗') {
                ""
            } else if field.editable {
                "→ "
            } else {
                "· "
            };

            let key_style = Style::default().fg(row_color).add_modifier(if is_selected {
                Modifier::BOLD
            } else {
                Modifier::empty()
            });
            let value_style = Style::default()
                .fg(if is_selected { primary() } else { val_color })
                .add_modifier(if is_selected {
                    Modifier::BOLD
                } else {
                    Modifier::empty()
                });

            let line = Line::from(vec![
                Span::styled(indent_str, Style::default().fg(tree_color)),
                Span::styled(
                    connector,
                    Style::default()
                        .fg(tree_color)
                        .add_modifier(if is_selected {
                            Modifier::BOLD
                        } else {
                            Modifier::empty()
                        }),
                ),
                Span::styled(format!("{rule} "), Style::default().fg(tree_color)),
                Span::styled(
                    key_marker,
                    Style::default()
                        .fg(if is_selected { primary() } else { muted() })
                        .add_modifier(if is_selected {
                            Modifier::BOLD
                        } else {
                            Modifier::empty()
                        }),
                ),
                Span::styled(
                    format!("{:<width$}", display_key, width = key_width),
                    key_style,
                ),
                Span::styled(val_prefix, value_style),
                Span::styled(&field.value, value_style),
            ]);

            items.push(ListItem::new(line));
            flat += 1;
        }

        items.push(ListItem::new(Line::raw("")));
        flat += 1;
    }

    let mut state = ListState::default();
    state.select(selected_flat);

    let list = List::new(items).highlight_style(Style::default());

    frame.render_stateful_widget(list, area, &mut state);
}

fn render_status_bar(frame: &mut ratatui::Frame, area: Rect, app: &App) {
    let status = if let Some(msg) = &app.last_error {
        Line::from(vec![
            Span::styled(" ✖ ", Style::default().fg(red())),
            Span::styled(msg.as_str(), Style::default().fg(red())),
        ])
    } else if let Some(msg) = &app.status_msg {
        Line::from(vec![
            Span::styled(" ℹ ", Style::default().fg(success())),
            Span::styled(msg.as_str(), Style::default().fg(text_fg())),
        ])
    } else {
        render_status_hint(area.width)
    };

    frame.render_widget(Paragraph::new(status), area);
}

fn render_status_hint(width: u16) -> Line<'static> {
    if width >= 52 {
        Line::from(vec![
            Span::styled(
                " ↑↓ ",
                Style::default().fg(primary()).add_modifier(Modifier::BOLD),
            ),
            Span::styled("Navigate", Style::default().fg(muted())),
            Span::styled(" | ", Style::default().fg(muted())),
            Span::styled(
                " Enter ",
                Style::default().fg(primary()).add_modifier(Modifier::BOLD),
            ),
            Span::styled("Edit", Style::default().fg(muted())),
            Span::styled(" | ", Style::default().fg(muted())),
            Span::styled(
                " Esc ",
                Style::default().fg(primary()).add_modifier(Modifier::BOLD),
            ),
            Span::styled("Back", Style::default().fg(muted())),
            Span::styled(" | ", Style::default().fg(muted())),
            Span::styled(
                " R ",
                Style::default().fg(primary()).add_modifier(Modifier::BOLD),
            ),
            Span::styled("Refresh", Style::default().fg(muted())),
        ])
    } else if width >= 34 {
        Line::from(vec![
            Span::styled(
                " ↑↓ ",
                Style::default().fg(primary()).add_modifier(Modifier::BOLD),
            ),
            Span::styled("Move", Style::default().fg(muted())),
            Span::styled(" · ", Style::default().fg(muted())),
            Span::styled(
                " Enter ",
                Style::default().fg(primary()).add_modifier(Modifier::BOLD),
            ),
            Span::styled("Edit", Style::default().fg(muted())),
            Span::styled(" · ", Style::default().fg(muted())),
            Span::styled(
                " Esc ",
                Style::default().fg(primary()).add_modifier(Modifier::BOLD),
            ),
            Span::styled("Back", Style::default().fg(muted())),
        ])
    } else {
        Line::from(vec![
            Span::styled(
                " ↑↓ ",
                Style::default().fg(primary()).add_modifier(Modifier::BOLD),
            ),
            Span::styled("Move", Style::default().fg(muted())),
            Span::styled(" · ", Style::default().fg(muted())),
            Span::styled(
                " Enter ",
                Style::default().fg(primary()).add_modifier(Modifier::BOLD),
            ),
            Span::styled("Edit", Style::default().fg(muted())),
        ])
    }
}

pub(super) fn render_editor(frame: &mut ratatui::Frame, area: Rect, app: &App) {
    let Some(tool) = app.tools.get(app.tool_index) else {
        return;
    };
    let Some((field_idx, edit_buf, edit_cursor)) = app.editing_view() else {
        return;
    };
    if field_idx >= tool.fields.len() {
        return;
    }
    let field = &tool.fields[field_idx];

    let popup_width = ((area.width as f32 * 0.8) as u16).min(area.width.saturating_sub(4));
    let popup_height = 5u16.min(area.height.saturating_sub(4));
    let popup = Rect::new(
        (area.width.saturating_sub(popup_width)) / 2,
        (area.height.saturating_sub(popup_height)) / 2,
        popup_width,
        popup_height,
    );

    frame.render_widget(Clear, popup);

    let block = Block::default()
        .title(Line::from(vec![
            Span::styled(" Edit: ", Style::default().fg(primary())),
            Span::styled(&field.key, Style::default().fg(text_fg())),
            Span::styled("  ", Style::default()),
            Span::styled("Enter", Style::default().fg(primary())),
            Span::styled(": Save  ", Style::default().fg(muted())),
            Span::styled("Esc", Style::default().fg(primary())),
            Span::styled(": Cancel ", Style::default().fg(muted())),
        ]))
        .borders(Borders::ALL)
        .border_style(Style::default().fg(primary()))
        .style(Style::default().bg(panel()));

    let inner = block.inner(popup);
    frame.render_widget(block, popup);

    let content_area = inner.inner(Margin::new(1, 0));

    let line = if edit_buf.is_empty() {
        Line::from(Span::styled(" ", Style::default().bg(primary())))
    } else {
        let cursor_pos = edit_cursor;
        let before = &edit_buf[..cursor_pos];
        let after = &edit_buf[cursor_pos..];

        if cursor_pos >= edit_buf.len() {
            Line::from(vec![
                Span::styled(before, Style::default().fg(text_fg())),
                Span::styled(" ", Style::default().bg(primary())),
            ])
        } else {
            let mut chars = after.chars();
            let current_char = chars.next().unwrap_or(' ');
            let remaining = chars.as_str();

            Line::from(vec![
                Span::styled(before, Style::default().fg(text_fg())),
                Span::styled(
                    current_char.to_string(),
                    Style::default().bg(primary()).fg(bg()),
                ),
                Span::styled(remaining, Style::default().fg(text_fg())),
            ])
        }
    };

    let input = Paragraph::new(vec![line]).wrap(ratatui::widgets::Wrap { trim: false });
    frame.render_widget(input, content_area);
}

fn render_selector(frame: &mut ratatui::Frame, area: Rect, app: &App) {
    let Some(tool) = app.tools.get(app.tool_index) else {
        return;
    };
    let Some((field_idx, select_options, select_index, select_filter)) = app.selecting_view()
    else {
        return;
    };
    if field_idx >= tool.fields.len() {
        return;
    }
    let field = &tool.fields[field_idx];

    let option_count = select_options.len() as u16;
    let max_popup_width = area.width.saturating_sub(4);
    let min_popup_width = 60u16.min(max_popup_width);
    let longest_option_width = select_options
        .iter()
        .map(|opt| opt.chars().count() as u16)
        .max()
        .unwrap_or(0);
    let popup_width = std::cmp::max(
        min_popup_width,
        longest_option_width.saturating_add(6).min(max_popup_width),
    );
    let popup_height = (option_count + 3).min(area.height.saturating_sub(4));
    let popup = Rect::new(
        (area.width.saturating_sub(popup_width)) / 2,
        (area.height.saturating_sub(popup_height)) / 2,
        popup_width,
        popup_height,
    );

    frame.render_widget(Clear, popup);

    let block = Block::default()
        .title(Line::from(vec![
            Span::styled(" Select: ", Style::default().fg(primary())),
            Span::styled(&field.key, Style::default().fg(text_fg())),
            Span::styled("  ", Style::default()),
            Span::styled("Enter/Space", Style::default().fg(primary())),
            Span::styled(": Apply  ", Style::default().fg(muted())),
            Span::styled("Esc", Style::default().fg(primary())),
            Span::styled(": Cancel ", Style::default().fg(muted())),
            Span::styled("Type", Style::default().fg(primary())),
            Span::styled(": Filter ", Style::default().fg(muted())),
        ]))
        .borders(Borders::ALL)
        .border_style(Style::default().fg(primary()))
        .style(Style::default().bg(panel()));

    let inner = block.inner(popup);
    frame.render_widget(block, popup);

    let filter_line = if select_filter.is_empty() {
        "Filter: type to narrow large model lists".to_string()
    } else {
        format!("Filter: {select_filter}")
    };
    let filter_area = Rect::new(inner.x, inner.y, inner.width, 1);
    frame.render_widget(
        Paragraph::new(filter_line).style(Style::default().fg(muted()).bg(panel())),
        filter_area,
    );

    let list_area = Rect::new(
        inner.x,
        inner.y.saturating_add(1),
        inner.width,
        inner.height.saturating_sub(1),
    );

    let items: Vec<ListItem> = select_options
        .iter()
        .enumerate()
        .map(|(i, opt)| {
            let is_sel = i == select_index;
            let marker = if is_sel { "➤ " } else { "  " };
            let style = if is_sel {
                Style::default().fg(primary()).add_modifier(Modifier::BOLD)
            } else {
                Style::default().fg(text_fg())
            };
            ListItem::new(Line::from(vec![
                Span::styled(
                    marker,
                    Style::default().fg(primary()).add_modifier(Modifier::BOLD),
                ),
                Span::styled(opt.as_str(), style),
            ]))
        })
        .collect();

    if items.is_empty() {
        frame.render_widget(
            Paragraph::new("No matches").style(Style::default().fg(muted()).bg(panel())),
            list_area,
        );
        return;
    }

    let mut state = ListState::default();
    state.select(Some(select_index));

    let list = List::new(items).highlight_style(Style::default());
    frame.render_stateful_widget(list, list_area, &mut state);
}

#[cfg(test)]
mod tests {
    use super::{resolve_main_layout, MainLayoutMode};

    #[test]
    fn keeps_spacer_in_compact_layout() {
        assert_eq!(resolve_main_layout(10, 9), MainLayoutMode::Compact);
    }

    #[test]
    fn requires_room_for_status_and_spacer_before_expanding() {
        assert_eq!(resolve_main_layout(10, 6), MainLayoutMode::Expanded);
        assert_eq!(resolve_main_layout(10, 7), MainLayoutMode::Compact);
    }

    #[test]
    fn handles_tiny_terminal_heights() {
        assert_eq!(resolve_main_layout(2, 1), MainLayoutMode::HeaderOnly);
        assert_eq!(resolve_main_layout(3, 1), MainLayoutMode::HeaderAndFooter);
    }
}
