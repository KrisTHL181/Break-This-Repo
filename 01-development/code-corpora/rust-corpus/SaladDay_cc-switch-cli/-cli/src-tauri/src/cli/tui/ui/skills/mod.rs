use super::*;

mod detail;
mod discover;
mod helpers;
mod installed;
mod repos;

pub(super) fn skills_installed_filtered<'a>(
    app: &App,
    data: &'a UiData,
) -> Vec<&'a crate::services::skill::InstalledSkill> {
    helpers::skills_installed_filtered(app, data)
}

pub(super) fn skill_display_name<'a>(name: &'a str, directory: &'a str) -> &'a str {
    helpers::skill_display_name(name, directory)
}

pub(super) fn enabled_skill_apps_text(apps: &crate::app_config::SkillApps) -> String {
    helpers::enabled_skill_apps_text(apps)
}

pub(super) fn render_skills_installed(
    frame: &mut Frame<'_>,
    app: &App,
    data: &UiData,
    area: Rect,
    theme: &super::theme::Theme,
) {
    installed::render_skills_installed(frame, app, data, area, theme)
}

pub(super) fn render_skills_discover(
    frame: &mut Frame<'_>,
    app: &App,
    data: &UiData,
    area: Rect,
    theme: &super::theme::Theme,
) {
    discover::render_skills_discover(frame, app, data, area, theme)
}

pub(super) fn render_skills_repos(
    frame: &mut Frame<'_>,
    app: &App,
    data: &UiData,
    area: Rect,
    theme: &super::theme::Theme,
) {
    repos::render_skills_repos(frame, app, data, area, theme)
}

pub(super) fn render_skill_detail(
    frame: &mut Frame<'_>,
    app: &App,
    data: &UiData,
    area: Rect,
    theme: &super::theme::Theme,
    directory: &str,
) {
    detail::render_skill_detail(frame, app, data, area, theme, directory)
}
