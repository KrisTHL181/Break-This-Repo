use crate::commands::CommandResult;
use crate::tui::app::{App, AppAction};

pub(super) fn tools(app: &mut App, arg: Option<&str>) -> CommandResult {
    let format = arg.unwrap_or("text").trim();
    let Some(snapshot) = app.session.last_tool_request_snapshot.as_ref() else {
        return CommandResult::message(
            "Tool request snapshot unavailable — no model request has been captured for the latest turn.",
        );
    };

    match format {
        "" | "text" => CommandResult::action(AppAction::OpenTextPager {
            title: "Prepared Tool Request".to_string(),
            content: snapshot.render_text(),
        }),
        "json" => match snapshot.render_json() {
            Ok(output) => CommandResult::action(AppAction::OpenTextPager {
                title: "Prepared Tool Request (JSON)".to_string(),
                content: output,
            }),
            Err(error) => CommandResult::error(format!(
                "tool request snapshot could not be serialized: {error}"
            )),
        },
        _ => CommandResult::error("usage: /tools [text|json]"),
    }
}
