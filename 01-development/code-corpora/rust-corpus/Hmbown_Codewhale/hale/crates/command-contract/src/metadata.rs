//! Portable command registration metadata.

use crate::handler::CommandHandler;

/// Static metadata describing a command without importing localization.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct CommandInfo {
    pub name: &'static str,
    pub aliases: &'static [&'static str],
    pub usage: &'static str,
    /// Stable localization key resolved by the current TUI owner.
    pub description_key: &'static str,
}

/// Discovery tier controlling palette and help visibility.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum CommandDiscovery {
    Primary,
    Advanced,
    Compatibility,
}

/// Target registration shape. Existing TUI commands adopt it group by group.
pub trait RegisterCommand<R> {
    fn info() -> &'static CommandInfo;
    fn handler() -> CommandHandler<R>;
}
