//! Confirmation popup for resuming a session from the launch card.
//!
//! Resuming replaces the whole session context, and a single click used to do
//! it instantly — founder live-test: "you just click it and boom you're there
//! ... you don't realize it's happening". The first attempt at a fix put an
//! arming line over the composer dock, which read as one more piece of chrome
//! rather than as a question ("that's even more confusing tbh"). This is the
//! question, as a popup, with the session it is about named in it.

use crossterm::event::{KeyCode, KeyEvent, MouseButton, MouseEvent, MouseEventKind};
use ratatui::layout::Rect;
use ratatui::prelude::*;
use ratatui::widgets::{Block, Borders, Padding, Paragraph, Widget, Wrap};

use crate::tui::views::{
    ActionHint, ModalKind, ModalView, ViewAction, ViewEvent, centered_modal_area,
    render_modal_footer, render_modal_surface,
};
use codewhale_localization::{Locale, MessageId, tr};
use codewhale_palette as palette;

pub struct LaunchResumeConfirmView {
    session_id: String,
    title: String,
    detail: String,
    locale: Locale,
    /// The painted popup, so a click outside it can dismiss.
    last_area: std::cell::Cell<Option<Rect>>,
}

impl LaunchResumeConfirmView {
    #[must_use]
    pub fn new(session_id: String, title: String, detail: String, locale: Locale) -> Self {
        Self {
            session_id,
            title,
            detail,
            locale,
            last_area: std::cell::Cell::new(None),
        }
    }

    fn confirm(&self) -> ViewAction {
        ViewAction::EmitAndClose(ViewEvent::LaunchResumeConfirmed {
            session_id: self.session_id.clone(),
        })
    }
}

impl ModalView for LaunchResumeConfirmView {
    fn kind(&self) -> ModalKind {
        ModalKind::LaunchResumeConfirm
    }

    fn handle_key(&mut self, key: KeyEvent) -> ViewAction {
        match key.code {
            KeyCode::Enter => self.confirm(),
            // `y` is the habit every terminal confirmation teaches; Esc and
            // `n` both back out. Nothing else acts, so a stray keystroke
            // cannot resume a session by accident.
            KeyCode::Char('y') | KeyCode::Char('Y') => self.confirm(),
            KeyCode::Esc | KeyCode::Char('n') | KeyCode::Char('N') => ViewAction::Close,
            _ => ViewAction::None,
        }
    }

    fn handle_mouse(&mut self, mouse: MouseEvent) -> ViewAction {
        if !matches!(mouse.kind, MouseEventKind::Down(MouseButton::Left)) {
            return ViewAction::None;
        }
        // A click outside the popup is a dismissal, never a confirmation:
        // the whole point is that resuming needs a deliberate act.
        match self.last_area.get() {
            Some(area) if crate::tui::mouse_ui::mouse_hits_rect(mouse, Some(area)) => {
                ViewAction::None
            }
            Some(_) => ViewAction::Close,
            None => ViewAction::None,
        }
    }

    fn occupied_region(&self, area: Rect) -> Rect {
        centered_modal_area(area, 64, 11, 32, 7)
    }

    fn as_any_mut(&mut self) -> &mut dyn std::any::Any {
        self
    }

    fn render(&self, area: Rect, buf: &mut Buffer) {
        let popup = self.occupied_region(area);
        self.last_area.set(Some(popup));
        render_modal_surface(area, popup, buf);

        let block = Block::default()
            .title(Line::from(Span::styled(
                tr(self.locale, MessageId::LaunchResumeConfirmTitle).to_string(),
                Style::default().fg(palette::WHALE_HUMAN).bold(),
            )))
            .borders(Borders::ALL)
            .border_style(Style::default().fg(palette::BORDER_COLOR))
            .style(Style::default().bg(palette::WHALE_BG))
            .padding(Padding::uniform(1));
        let inner = block.inner(popup);
        block.render(popup, buf);

        let mut lines = vec![
            Line::from(Span::styled(
                self.title.clone(),
                Style::default().fg(palette::TEXT_PRIMARY).bold(),
            )),
            Line::from(Span::styled(
                self.detail.clone(),
                Style::default().fg(palette::TEXT_MUTED),
            )),
            Line::from(""),
            Line::from(Span::styled(
                tr(self.locale, MessageId::LaunchResumeConfirmBody).to_string(),
                Style::default().fg(palette::TEXT_SOFT),
            )),
        ];
        lines.truncate(usize::from(inner.height).saturating_sub(1).max(1));
        Paragraph::new(lines)
            .wrap(Wrap { trim: true })
            .render(inner, buf);

        render_modal_footer(
            inner,
            buf,
            &[
                ActionHint::new(
                    "Enter",
                    tr(self.locale, MessageId::LaunchResumeConfirmResume).to_string(),
                ),
                ActionHint::new(
                    "Esc",
                    tr(self.locale, MessageId::LaunchResumeConfirmCancel).to_string(),
                ),
            ],
        );
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crossterm::event::KeyModifiers;

    fn view() -> LaunchResumeConfirmView {
        LaunchResumeConfirmView::new(
            "sess-1".to_string(),
            "refactor the parser".to_string(),
            "3h ago · 12 msgs".to_string(),
            Locale::En,
        )
    }

    #[test]
    fn enter_confirms_and_esc_walks_away() {
        let mut confirm = view();
        match confirm.handle_key(KeyEvent::new(KeyCode::Enter, KeyModifiers::NONE)) {
            ViewAction::EmitAndClose(ViewEvent::LaunchResumeConfirmed { session_id }) => {
                assert_eq!(session_id, "sess-1");
            }
            other => panic!("Enter must confirm, got {other:?}"),
        }

        let mut confirm = view();
        assert!(matches!(
            confirm.handle_key(KeyEvent::new(KeyCode::Esc, KeyModifiers::NONE)),
            ViewAction::Close
        ));

        // A stray keystroke resumes nothing: the whole point is that this
        // takes a deliberate act.
        let mut confirm = view();
        assert!(matches!(
            confirm.handle_key(KeyEvent::new(KeyCode::Char('k'), KeyModifiers::NONE)),
            ViewAction::None
        ));
    }

    #[test]
    fn the_popup_names_the_session_it_is_asking_about() {
        let confirm = view();
        let area = Rect::new(0, 0, 100, 30);
        let mut buf = Buffer::empty(area);
        confirm.render(area, &mut buf);
        let painted: String = (0..area.height)
            .map(|y| {
                (0..area.width)
                    .map(|x| buf[(x, y)].symbol())
                    .collect::<String>()
            })
            .collect::<Vec<_>>()
            .join("\n");
        assert!(
            painted.contains("refactor the parser"),
            "the session title is in the popup:\n{painted}"
        );
        assert!(
            painted.contains("12 msgs"),
            "so is enough detail to recognise it:\n{painted}"
        );
    }
}
