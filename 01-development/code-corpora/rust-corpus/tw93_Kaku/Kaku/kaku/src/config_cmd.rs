use std::ffi::OsString;
use std::path::PathBuf;

use anyhow::Context;
use clap::Parser;

use crate::config_tui;

#[derive(Debug, Parser, Clone, Default)]
pub struct ConfigCommand {
    /// Ensure an editable Kaku config file exists, but do not open it.
    #[arg(long, hide = true)]
    ensure_only: bool,
}

impl ConfigCommand {
    pub fn run(
        &self,
        config_path: Option<PathBuf>,
        config_file: Option<OsString>,
        config_override: Vec<(String, String)>,
        skip_config: bool,
    ) -> anyhow::Result<()> {
        let config_path = config_tui::ensure_editable_config_exists(config_path.as_deref())?;
        if self.ensure_only {
            println!("Ensured config: {}", config_path.display());
            return Ok(());
        }

        config_tui::run(config_path, config_file, config_override, skip_config)
            .context("config tui")
    }
}
