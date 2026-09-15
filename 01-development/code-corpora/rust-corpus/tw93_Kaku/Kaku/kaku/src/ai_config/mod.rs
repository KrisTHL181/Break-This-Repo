pub mod tui;

use std::ffi::OsString;

use anyhow::Context;
use clap::Parser;

#[derive(Debug, Parser, Clone, Default)]
pub struct AiConfigCommand {}

impl AiConfigCommand {
    pub fn run(
        &self,
        config_file: Option<OsString>,
        config_override: Vec<(String, String)>,
        skip_config: bool,
    ) -> anyhow::Result<()> {
        tui::run(config_file, config_override, skip_config).context("ai config tui")
    }
}
