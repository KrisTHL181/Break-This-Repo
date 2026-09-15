//! Centered splash frame used for transient in-session feedback such as the
//! `Saving...` / `Saved` flash in the config TUI. The on-entry "Loading..."
//! splash was intentionally removed because it produced a visible flash of
//! centered text that the real TUI overwrote within ~100 ms; entry-time
//! waits now happen against the user's existing shell instead of the
//! alternate screen.

use ratatui::layout::{Alignment, Constraint, Layout};
use ratatui::style::Style;
use ratatui::text::Line;
use ratatui::widgets::{Block, Clear, Paragraph};

use crate::tui_core::theme::{bg, muted};

/// Render a centered splash frame with a custom spinner character.
pub fn render_splash_with_spinner(frame: &mut ratatui::Frame, message: &str, spinner: char) {
    let area = frame.area();
    frame.render_widget(Clear, area);
    frame.render_widget(Block::default().style(Style::default().bg(bg())), area);

    let text = format!("{spinner}  {message}");
    let chunks = Layout::vertical([
        Constraint::Fill(1),
        Constraint::Length(1),
        Constraint::Fill(1),
    ])
    .split(area);

    let para = Paragraph::new(Line::from(text).style(Style::default().fg(muted())))
        .alignment(Alignment::Center);
    frame.render_widget(para, chunks[1]);
}
