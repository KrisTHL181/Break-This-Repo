use std::io::IsTerminal;

use crate::app_config::AppType;
use crate::error::AppError;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum InteractivePath {
    Ratatui,
}

fn decide_interactive_path(
    legacy_tui_requested: bool,
    stdin_is_tty: bool,
    stdout_is_tty: bool,
) -> Result<InteractivePath, AppError> {
    if legacy_tui_requested {
        return Err(AppError::Message(
            crate::cli::i18n::texts::interactive_legacy_tui_removed().to_string(),
        ));
    }

    if !stdin_is_tty || !stdout_is_tty {
        return Err(AppError::Message(
            crate::cli::i18n::texts::interactive_requires_tty().to_string(),
        ));
    }

    Ok(InteractivePath::Ratatui)
}

pub fn run(app: Option<AppType>) -> Result<(), AppError> {
    let path = decide_interactive_path(
        std::env::var("CC_SWITCH_LEGACY_TUI").ok().as_deref() == Some("1"),
        std::io::stdin().is_terminal(),
        std::io::stdout().is_terminal(),
    )?;

    match path {
        InteractivePath::Ratatui => crate::cli::tui::run(app),
    }
}

#[cfg(test)]
mod tests {
    use super::{decide_interactive_path, InteractivePath};
    use crate::cli::i18n::texts;
    use crate::error::AppError;

    #[test]
    fn non_tty_returns_direct_tty_error() {
        let err = decide_interactive_path(false, true, false)
            .expect_err("non-tty interactive mode should fail fast");

        match err {
            AppError::Message(message) => {
                assert_eq!(message, texts::interactive_requires_tty());
            }
            other => panic!("unexpected error variant: {other:?}"),
        }
    }

    #[test]
    fn legacy_env_flag_returns_removed_error() {
        let err = decide_interactive_path(true, true, true)
            .expect_err("legacy env flag should no longer enable removed tui");

        match err {
            AppError::Message(message) => {
                assert_eq!(message, texts::interactive_legacy_tui_removed());
            }
            other => panic!("unexpected error variant: {other:?}"),
        }
    }

    #[test]
    fn tty_without_legacy_flag_uses_new_tui() {
        let path = decide_interactive_path(false, true, true)
            .expect("tty interactive mode should enter ratatui");

        assert_eq!(path, InteractivePath::Ratatui);
    }
}
