use super::*;

pub(in crate::workspace) const PORTABLE_SETTINGS_DIALOG_WIDTH: f32 = 460.0;
pub(in crate::workspace) const PORTABLE_SETTINGS_PATH_CARD_GAP: f32 = 12.0;
pub(in crate::workspace) const PORTABLE_SETTINGS_BUTTON_GAP: f32 = 12.0;

pub(in crate::workspace) fn portable_activation_label(
    i18n: &oxideterm_i18n::I18n,
    activation: oxideterm_portable_runtime::PortableActivationKind,
) -> String {
    match activation {
        oxideterm_portable_runtime::PortableActivationKind::Marker => {
            i18n.t("settings_view.general.portable_activation_marker")
        }
        oxideterm_portable_runtime::PortableActivationKind::Config => {
            i18n.t("settings_view.general.portable_activation_config")
        }
        oxideterm_portable_runtime::PortableActivationKind::Disabled => {
            i18n.t("settings_view.general.portable_activation_disabled")
        }
    }
}

pub(in crate::workspace) fn portable_status_badge_color(
    status: oxideterm_portable_runtime::PortableBootstrapStatus,
    tokens: &oxideterm_theme::ThemeTokens,
) -> u32 {
    match status {
        oxideterm_portable_runtime::PortableBootstrapStatus::Unlocked => tokens.ui.success,
        oxideterm_portable_runtime::PortableBootstrapStatus::Disabled => tokens.ui.text_muted,
        oxideterm_portable_runtime::PortableBootstrapStatus::NeedsSetup
        | oxideterm_portable_runtime::PortableBootstrapStatus::Locked => tokens.ui.warning,
    }
}

// Keep each portable-settings responsibility in a real module while exposing
// only the workspace-private methods required by the existing settings flow.
#[path = "portable_runtime/actions.rs"]
mod portable_runtime_actions;
#[path = "portable_runtime/dialogs.rs"]
mod portable_runtime_dialogs;
#[path = "portable_runtime/render.rs"]
mod portable_runtime_render;
