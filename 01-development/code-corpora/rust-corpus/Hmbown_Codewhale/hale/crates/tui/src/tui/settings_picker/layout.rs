//! Responsive list/detail geometry for settings pickers.
//!
//! Wide terminals place the option list beside a detail pane. Narrow terminals
//! stack (or, when the focused option prefers it, keep the list alone).

use ratatui::layout::Rect;

use crate::tui::views::ListDetailLayout;

use super::option::SettingOption;

/// Resolved panes for one settings-picker frame.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct SettingsPickerLayout {
    pub list: Rect,
    pub detail: Option<Rect>,
    pub stacked: bool,
    pub narrow: bool,
}

impl SettingsPickerLayout {
    /// Split `area` using the shared list/detail contract, then optionally
    /// collapse the detail pane when the focused option prefers list-only
    /// narrow fallback.
    #[must_use]
    pub fn resolve(area: Rect, min_detail_width: u16, focused: Option<&SettingOption>) -> Self {
        if area.width == 0 || area.height == 0 {
            return Self {
                list: area,
                detail: None,
                stacked: true,
                narrow: true,
            };
        }

        let base = ListDetailLayout::split(area, min_detail_width);
        let narrow = base.stacked || area.width < 96;
        let prefer_list = focused.is_some_and(|option| option.prefer_list_when_narrow);

        if narrow && prefer_list {
            return Self {
                list: area,
                detail: None,
                stacked: true,
                narrow: true,
            };
        }

        Self {
            list: base.list,
            detail: Some(base.detail),
            stacked: base.stacked,
            narrow,
        }
    }
}
