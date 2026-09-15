use gpui::{
    AnyElement, Div, FontWeight, IntoElement, ParentElement, Styled, TextAlign, div, prelude::*,
    px, rgb, rgba,
};
use oxideterm_theme::ThemeTokens;

const STATE_ICON_BG_ALPHA: u32 = 0x0d;
const STATE_LOADING_ICON_BG_ALPHA: u32 = 0x1a;
const STATE_ERROR_ICON_BG_ALPHA: u32 = 0x1a;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum UiStateTone {
    Accent,
    Success,
    Warning,
    Error,
    Muted,
}

pub fn empty_state(
    tokens: &ThemeTokens,
    icon: impl IntoElement,
    title: impl Into<String>,
    description: Option<String>,
    action: Option<AnyElement>,
) -> Div {
    state_shell(tokens)
        .child(state_icon_box(tokens, UiStateTone::Accent, STATE_ICON_BG_ALPHA).child(icon))
        .child(state_title(tokens, title))
        .when_some(description, |state, description| {
            state.child(state_description(tokens, description))
        })
        .when_some(action, |state, action| state.child(action))
}

pub fn loading_state(
    tokens: &ThemeTokens,
    icon: impl IntoElement,
    title: impl Into<String>,
    description: Option<String>,
) -> Div {
    state_shell(tokens)
        .gap(px(tokens.spacing.two))
        .child(state_icon_box(tokens, UiStateTone::Accent, STATE_LOADING_ICON_BG_ALPHA).child(icon))
        .child(state_title(tokens, title))
        .when_some(description, |state, description| {
            state.child(state_description(tokens, description))
        })
}

pub fn error_state(
    tokens: &ThemeTokens,
    icon: impl IntoElement,
    title: impl Into<String>,
    description: Option<String>,
    action: Option<AnyElement>,
) -> Div {
    state_shell(tokens)
        .child(state_icon_box(tokens, UiStateTone::Error, STATE_ERROR_ICON_BG_ALPHA).child(icon))
        .child(state_title(tokens, title).text_color(rgb(tokens.ui.text_heading)))
        .when_some(description, |state, description| {
            state.child(state_description(tokens, description))
        })
        .when_some(action, |state, action| state.child(action))
}

pub fn state_shell(tokens: &ThemeTokens) -> Div {
    div()
        .size_full()
        .flex()
        .flex_col()
        .items_center()
        .justify_center()
        .p(px(tokens.spacing.three * 2.0))
        .text_align(TextAlign::Center)
        .text_color(rgb(tokens.ui.text_muted))
}

pub fn state_icon_box(tokens: &ThemeTokens, tone: UiStateTone, background_alpha: u32) -> Div {
    div()
        .size(px(tokens.metrics.ui_button_lg_height + tokens.spacing.two))
        .flex()
        .items_center()
        .justify_center()
        .rounded(px(tokens.radii.md))
        .bg(rgba(
            (state_tone_color(tokens, tone) << 8) | background_alpha,
        ))
        .text_color(rgb(state_tone_color(tokens, tone)))
}

pub fn state_title(tokens: &ThemeTokens, title: impl Into<String>) -> Div {
    div()
        .mt(px(tokens.spacing.three + tokens.spacing.one))
        .text_size(px(tokens.metrics.ui_text_sm))
        .font_weight(FontWeight::BOLD)
        .text_color(rgb(tokens.ui.text))
        .child(title.into())
}

pub fn state_description(tokens: &ThemeTokens, description: impl Into<String>) -> Div {
    div()
        .mt(px(tokens.spacing.one))
        .max_w(px(tokens.metrics.settings_nav_width))
        .text_size(px(tokens.metrics.ui_text_xs))
        .line_height(px(tokens.metrics.ui_text_xs * 1.5))
        .text_color(rgb(tokens.ui.text_muted))
        .child(description.into())
}

pub fn state_primary_action(tokens: &ThemeTokens, label: impl Into<String>) -> Div {
    crate::button(tokens, label.into(), crate::ButtonTone::Primary)
        .mt(px(tokens.spacing.three + tokens.spacing.one))
}

pub fn inline_empty_state(tokens: &ThemeTokens, label: impl Into<String>) -> Div {
    div()
        .py(px(tokens.spacing.three * 2.0))
        .text_align(TextAlign::Center)
        .text_size(px(tokens.metrics.ui_text_sm))
        .text_color(rgb(tokens.ui.text_muted))
        .child(label.into())
}

pub fn state_notice(
    tokens: &ThemeTokens,
    tone: UiStateTone,
    icon: impl IntoElement,
    title: impl Into<String>,
    description: Option<String>,
) -> Div {
    div()
        .flex()
        .items_start()
        .gap(px(tokens.spacing.three))
        .rounded(px(tokens.radii.md))
        .border_1()
        .border_color(rgba((state_tone_color(tokens, tone) << 8) | 0x33))
        .bg(rgba((state_tone_color(tokens, tone) << 8) | 0x12))
        .p(px(tokens.spacing.three))
        .text_color(rgb(tokens.ui.text))
        .child(
            div()
                .mt(px(1.0))
                .flex_none()
                .text_color(rgb(state_tone_color(tokens, tone)))
                .child(icon),
        )
        .child(
            div()
                .min_w_0()
                .flex()
                .flex_col()
                .gap(px(tokens.spacing.one))
                .child(
                    div()
                        .text_size(px(tokens.metrics.ui_text_sm))
                        .font_weight(FontWeight::SEMIBOLD)
                        .child(title.into()),
                )
                .when_some(description, |body, description| {
                    body.child(
                        div()
                            .text_size(px(tokens.metrics.ui_text_xs))
                            .line_height(px(tokens.metrics.ui_text_xs * 1.5))
                            .text_color(rgb(tokens.ui.text_muted))
                            .child(description),
                    )
                }),
        )
}

fn state_tone_color(tokens: &ThemeTokens, tone: UiStateTone) -> u32 {
    match tone {
        UiStateTone::Accent => tokens.ui.accent,
        UiStateTone::Success => tokens.ui.success,
        UiStateTone::Warning => tokens.ui.warning,
        UiStateTone::Error => tokens.ui.error,
        UiStateTone::Muted => tokens.ui.text_muted,
    }
}
