//! Closed color vocabulary for status-bar chrome.
//!
//! Seven semantic families. Widgets pick a [`ChromeInk`] that already maps
//! to a live [`UiTheme`] slot — they do not invent RGB or an eighth
//! meaning. Failure red is reserved for actual failure.
//!
//! Contract: `docs/design/STATUS_BAR_COLOR_GRAMMAR.md`.

use ratatui::style::{Color, Style};

use super::themes::UiTheme;

/// The seven meanings the chrome may speak.
#[cfg_attr(not(test), allow(dead_code))]
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SemanticFamily {
    /// GREEN — outcome / success / model output.
    Outcome,
    /// ORANGE — cognition / consequential action / elevated capability.
    Cognition,
    /// CYAN — currently active / orchestration.
    Active,
    /// PURPLE — user-selected mode / policy.
    Policy,
    /// BLUE — identity / navigation.
    Identity,
    /// GRAY — passive metadata / historical state.
    Metadata,
    /// RED — actual failure / destructive warning only.
    Failure,
}

impl SemanticFamily {
    #[cfg(test)]
    pub const ALL: [Self; 7] = [
        Self::Outcome,
        Self::Cognition,
        Self::Active,
        Self::Policy,
        Self::Identity,
        Self::Metadata,
        Self::Failure,
    ];
}

/// Named status-bar inks. Each variant is an existing `UiTheme` slot, not a
/// new theme. Adding a variant requires assigning one of the seven families.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ChromeInk {
    Outcome,
    PermissionAsk,
    PermissionAutoReview,
    PermissionFullAccess,
    Waiting,
    Attention,
    Active,
    PolicyAct,
    PolicyPlan,
    PolicyOperate,
    Identity,
    Info,
    MetadataValue,
    Metadata,
    MetadataHint,
    MetadataDim,
    Failure,
}

impl ChromeInk {
    #[cfg(test)]
    pub const ALL: [Self; 17] = [
        Self::Outcome,
        Self::PermissionAsk,
        Self::PermissionAutoReview,
        Self::PermissionFullAccess,
        Self::Waiting,
        Self::Attention,
        Self::Active,
        Self::PolicyAct,
        Self::PolicyPlan,
        Self::PolicyOperate,
        Self::Identity,
        Self::Info,
        Self::MetadataValue,
        Self::Metadata,
        Self::MetadataHint,
        Self::MetadataDim,
        Self::Failure,
    ];

    #[must_use]
    #[cfg_attr(not(test), allow(dead_code))]
    pub const fn family(self) -> SemanticFamily {
        match self {
            Self::Outcome => SemanticFamily::Outcome,
            Self::PermissionAsk
            | Self::PermissionAutoReview
            | Self::PermissionFullAccess
            | Self::Waiting
            | Self::Attention => SemanticFamily::Cognition,
            Self::Active => SemanticFamily::Active,
            Self::PolicyAct | Self::PolicyPlan | Self::PolicyOperate => SemanticFamily::Policy,
            Self::Identity | Self::Info => SemanticFamily::Identity,
            Self::MetadataValue | Self::Metadata | Self::MetadataHint | Self::MetadataDim => {
                SemanticFamily::Metadata
            }
            Self::Failure => SemanticFamily::Failure,
        }
    }

    /// Resolve through the live theme. Do not bypass this for status-bar
    /// ink — that is how an eighth semantic sneaks in.
    #[must_use]
    pub fn color(self, theme: &UiTheme) -> Color {
        match self {
            Self::Outcome => theme.success,
            Self::PermissionAsk => theme.permission_ask,
            Self::PermissionAutoReview => theme.permission_auto_review,
            Self::PermissionFullAccess => theme.permission_full_access,
            Self::Waiting => theme.accent_action,
            Self::Attention => theme.warning,
            Self::Active => theme.status_working,
            Self::PolicyAct => theme.mode_agent,
            Self::PolicyPlan => theme.mode_plan,
            Self::PolicyOperate => theme.mode_operate,
            Self::Identity => theme.accent_primary,
            Self::Info => theme.info,
            Self::MetadataValue => theme.text_soft,
            Self::Metadata => theme.text_muted,
            Self::MetadataHint => theme.text_hint,
            Self::MetadataDim => theme.text_dim,
            Self::Failure => theme.error_fg,
        }
    }
}

#[must_use]
pub fn chrome_style(theme: &UiTheme, ink: ChromeInk) -> Style {
    Style::default().fg(ink.color(theme))
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::themes::UI_THEME;

    #[test]
    fn vocabulary_is_exactly_seven_families() {
        assert_eq!(SemanticFamily::ALL.len(), 7);
        for ink in ChromeInk::ALL {
            assert!(
                SemanticFamily::ALL.contains(&ink.family()),
                "{ink:?} left the closed vocabulary"
            );
        }
    }

    #[test]
    fn only_failure_ink_is_the_failure_family() {
        let failure: Vec<_> = ChromeInk::ALL
            .iter()
            .copied()
            .filter(|ink| ink.family() == SemanticFamily::Failure)
            .collect();
        assert_eq!(failure, [ChromeInk::Failure]);
        assert_eq!(ChromeInk::Failure.color(&UI_THEME), UI_THEME.error_fg);
    }

    /// Second, independent copy of the family map. Regrouping `family()` has
    /// to be done twice to land, and a new `ChromeInk` variant stops this
    /// module compiling — register it in [`ChromeInk::ALL`] at the same time
    /// or the closed-vocabulary tests below will silently skip it.
    fn expected_family(ink: ChromeInk) -> SemanticFamily {
        match ink {
            ChromeInk::Outcome => SemanticFamily::Outcome,
            ChromeInk::PermissionAsk => SemanticFamily::Cognition,
            ChromeInk::PermissionAutoReview => SemanticFamily::Cognition,
            ChromeInk::PermissionFullAccess => SemanticFamily::Cognition,
            ChromeInk::Waiting => SemanticFamily::Cognition,
            ChromeInk::Attention => SemanticFamily::Cognition,
            ChromeInk::Active => SemanticFamily::Active,
            ChromeInk::PolicyAct => SemanticFamily::Policy,
            ChromeInk::PolicyPlan => SemanticFamily::Policy,
            ChromeInk::PolicyOperate => SemanticFamily::Policy,
            ChromeInk::Identity => SemanticFamily::Identity,
            ChromeInk::Info => SemanticFamily::Identity,
            ChromeInk::MetadataValue => SemanticFamily::Metadata,
            ChromeInk::Metadata => SemanticFamily::Metadata,
            ChromeInk::MetadataHint => SemanticFamily::Metadata,
            ChromeInk::MetadataDim => SemanticFamily::Metadata,
            ChromeInk::Failure => SemanticFamily::Failure,
        }
    }

    #[test]
    fn every_registered_ink_keeps_its_declared_family() {
        for ink in ChromeInk::ALL {
            assert_eq!(ink.family(), expected_family(ink), "{ink:?}");
        }
    }

    /// The grammar's central promise is theme-wide, not whale-only: no
    /// Outcome, Active, Policy, Identity, or Metadata chrome may resolve to
    /// the same color as Failure in any selectable preset.
    ///
    /// Cognition is deliberately exempt. A preset may paint Full Access or
    /// waiting with a red-like hue; the role is still Cognition, and
    /// `every_selectable_theme_keeps_permission_lanes_distinct` in
    /// `palette::themes` owns that ramp's separation.
    #[test]
    fn every_selectable_theme_reserves_failure_red() {
        for theme_id in crate::themes::SELECTABLE_THEMES {
            let theme = theme_id.ui_theme();
            let red = ChromeInk::Failure.color(&theme);
            for ink in ChromeInk::ALL {
                let family = ink.family();
                if family == SemanticFamily::Cognition || family == SemanticFamily::Failure {
                    continue;
                }
                assert_ne!(
                    ink.color(&theme),
                    red,
                    "theme '{}' spends Failure red on {ink:?} ({family:?})",
                    theme_id.name()
                );
            }
        }
    }

    #[test]
    fn chrome_inks_keep_the_existing_theme_slots() {
        assert_eq!(ChromeInk::Outcome.color(&UI_THEME), UI_THEME.success);
        assert_eq!(
            ChromeInk::PermissionAsk.color(&UI_THEME),
            UI_THEME.permission_ask
        );
        assert_eq!(
            ChromeInk::PermissionAutoReview.color(&UI_THEME),
            UI_THEME.permission_auto_review
        );
        assert_eq!(
            ChromeInk::PermissionFullAccess.color(&UI_THEME),
            UI_THEME.permission_full_access
        );
        assert_eq!(ChromeInk::Waiting.color(&UI_THEME), UI_THEME.accent_action);
        assert_eq!(ChromeInk::Attention.color(&UI_THEME), UI_THEME.warning);
        assert_eq!(ChromeInk::Active.color(&UI_THEME), UI_THEME.status_working);
        assert_eq!(ChromeInk::PolicyAct.color(&UI_THEME), UI_THEME.mode_agent);
        assert_eq!(ChromeInk::PolicyPlan.color(&UI_THEME), UI_THEME.mode_plan);
        assert_eq!(
            ChromeInk::PolicyOperate.color(&UI_THEME),
            UI_THEME.mode_operate
        );
        assert_eq!(
            ChromeInk::Identity.color(&UI_THEME),
            UI_THEME.accent_primary
        );
        assert_eq!(ChromeInk::Info.color(&UI_THEME), UI_THEME.info);
        assert_eq!(
            ChromeInk::MetadataValue.color(&UI_THEME),
            UI_THEME.text_soft
        );
        assert_eq!(ChromeInk::Metadata.color(&UI_THEME), UI_THEME.text_muted);
        assert_eq!(ChromeInk::MetadataHint.color(&UI_THEME), UI_THEME.text_hint);
        assert_eq!(ChromeInk::MetadataDim.color(&UI_THEME), UI_THEME.text_dim);
        assert_eq!(
            chrome_style(&UI_THEME, ChromeInk::Failure).fg,
            Some(UI_THEME.error_fg)
        );
    }
}
