use clap::ValueEnum;
use std::ffi::OsStr;
use std::path::{Path, PathBuf};

use anyhow::{Context, Result};

#[derive(Debug, Clone, Copy, PartialEq, Eq, ValueEnum)]
pub enum ManagedShell {
    Zsh,
    Fish,
}

impl ManagedShell {
    pub fn name(self) -> &'static str {
        match self {
            Self::Zsh => "zsh",
            Self::Fish => "fish",
        }
    }

    fn from_name(value: &str) -> Option<Self> {
        match value {
            "zsh" => Some(Self::Zsh),
            "fish" => Some(Self::Fish),
            _ => None,
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ShellKind {
    Zsh,
    Fish,
    Unsupported(String),
    Unknown,
}

impl ShellKind {
    pub fn is_managed(&self) -> bool {
        matches!(self, ShellKind::Zsh | ShellKind::Fish)
    }

    pub fn name(&self) -> &str {
        match self {
            ShellKind::Zsh => "zsh",
            ShellKind::Fish => "fish",
            ShellKind::Unsupported(s) => s.as_str(),
            ShellKind::Unknown => "unknown",
        }
    }

    pub fn managed(&self) -> Option<ManagedShell> {
        match self {
            Self::Zsh => Some(ManagedShell::Zsh),
            Self::Fish => Some(ManagedShell::Fish),
            _ => None,
        }
    }
}

pub fn detect_shell_kind() -> ShellKind {
    match std::env::var("SHELL") {
        Err(_) => ShellKind::Unknown,
        Ok(s) => shell_kind_from_path(&s),
    }
}

pub fn resolve_shell_kind(shell: Option<ManagedShell>) -> ShellKind {
    match shell {
        Some(ManagedShell::Zsh) => ShellKind::Zsh,
        Some(ManagedShell::Fish) => ShellKind::Fish,
        None => persisted_managed_shell()
            .map(|shell| match shell {
                ManagedShell::Zsh => ShellKind::Zsh,
                ManagedShell::Fish => ShellKind::Fish,
            })
            .unwrap_or_else(detect_shell_kind),
    }
}

pub fn preferred_managed_shell() -> ManagedShell {
    persisted_managed_shell()
        .or_else(|| detect_shell_kind().managed())
        .unwrap_or(ManagedShell::Zsh)
}

pub fn persisted_managed_shell() -> Option<ManagedShell> {
    read_managed_shell_from_path(&managed_shell_state_path()).or_else(|| {
        default_managed_shell_state_path().and_then(|path| read_managed_shell_from_path(&path))
    })
}

pub fn persist_managed_shell(shell: ManagedShell) -> Result<()> {
    persist_managed_shell_to_path(&managed_shell_state_path(), shell)
}

/// Mark shell initialization complete and mirror that completed state to the
/// default config path used by Finder-launched processes. The mirror happens
/// only after setup succeeds so a partial XDG initialization cannot suppress
/// first-run recovery in a GUI process that does not inherit XDG_CONFIG_HOME.
pub fn persist_initialized_state(shell: ManagedShell, config_version: u64) -> Result<()> {
    persist_initialized_state_to_path(&managed_shell_state_path(), shell, config_version)?;
    if let Some(default_path) = default_managed_shell_state_path() {
        if let Err(error) = persist_initialized_state_to_path(&default_path, shell, config_version)
        {
            log::warn!(
                "could not mirror initialized shell state to {}: {error:#}",
                default_path.display()
            );
        }
    }
    Ok(())
}

fn managed_shell_state_path() -> PathBuf {
    config::user_config_path()
        .parent()
        .map(Path::to_path_buf)
        .unwrap_or_else(|| config::HOME_DIR.join(".config").join("kaku"))
        .join("state.json")
}

/// The non-XDG default state location, or None when it already is the
/// primary path (no XDG override in this environment).
fn default_managed_shell_state_path() -> Option<PathBuf> {
    let default = config::HOME_DIR
        .join(".config")
        .join("kaku")
        .join("state.json");
    (default != managed_shell_state_path()).then_some(default)
}

fn read_managed_shell_from_path(path: &Path) -> Option<ManagedShell> {
    let raw = std::fs::read_to_string(path).ok()?;
    let state: serde_json::Value = serde_json::from_str(&raw).ok()?;
    ManagedShell::from_name(state.get("managed_shell")?.as_str()?)
}

fn persist_managed_shell_to_path(path: &Path, shell: ManagedShell) -> Result<()> {
    update_shell_state_to_path(path, shell, None)
}

fn persist_initialized_state_to_path(
    path: &Path,
    shell: ManagedShell,
    config_version: u64,
) -> Result<()> {
    update_shell_state_to_path(path, shell, Some(config_version))
}

fn update_shell_state_to_path(
    path: &Path,
    shell: ManagedShell,
    config_version: Option<u64>,
) -> Result<()> {
    let parent = path.parent().context("managed shell state has no parent")?;
    std::fs::create_dir_all(parent).with_context(|| format!("create {}", parent.display()))?;
    let mut state = match std::fs::read_to_string(path) {
        Ok(raw) => serde_json::from_str::<serde_json::Value>(&raw)
            .with_context(|| format!("parse {}", path.display()))?,
        Err(error) if error.kind() == std::io::ErrorKind::NotFound => serde_json::json!({}),
        Err(error) => return Err(error).with_context(|| format!("read {}", path.display())),
    };
    let object = state
        .as_object_mut()
        .context("Kaku state must be a JSON object")?;
    object.insert(
        "managed_shell".to_string(),
        serde_json::Value::String(shell.name().to_string()),
    );
    if let Some(config_version) = config_version {
        object.insert(
            "config_version".to_string(),
            serde_json::Value::Number(config_version.into()),
        );
    }
    let bytes = serde_json::to_vec_pretty(&state).context("serialize Kaku state")?;
    crate::utils::write_atomic(path, &bytes)
        .with_context(|| format!("write managed shell to {}", path.display()))
}

pub fn find_shell_executable(shell: ManagedShell) -> Option<PathBuf> {
    if let Some(current) = std::env::var_os("SHELL").map(PathBuf::from) {
        if current.file_name().and_then(OsStr::to_str) == Some(shell.name())
            && config::is_executable_file(&current)
        {
            return Some(current);
        }
    }

    if let Some(path) = std::env::var_os("PATH") {
        for dir in std::env::split_paths(&path) {
            let candidate = dir.join(shell.name());
            if config::is_executable_file(&candidate) {
                return Some(candidate);
            }
        }
    }

    let candidates: &[&str] = match shell {
        ManagedShell::Zsh => &["/bin/zsh", "/usr/bin/zsh"],
        ManagedShell::Fish => &[
            "/opt/homebrew/bin/fish",
            "/usr/local/bin/fish",
            "/usr/bin/fish",
        ],
    };
    candidates
        .iter()
        .map(PathBuf::from)
        .find(|candidate| config::is_executable_file(candidate))
}

fn shell_kind_from_path(shell: &str) -> ShellKind {
    match Path::new(shell)
        .file_name()
        .and_then(OsStr::to_str)
        .unwrap_or("")
    {
        "zsh" => ShellKind::Zsh,
        "fish" => ShellKind::Fish,
        "" => ShellKind::Unknown,
        other => ShellKind::Unsupported(other.to_string()),
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use tempfile::tempdir;

    #[test]
    fn shell_kind_uses_executable_name() {
        assert_eq!(shell_kind_from_path("/bin/zsh"), ShellKind::Zsh);
        assert_eq!(
            shell_kind_from_path("/opt/homebrew/bin/fish"),
            ShellKind::Fish
        );
        assert_eq!(
            shell_kind_from_path("/bin/bash"),
            ShellKind::Unsupported("bash".to_string())
        );
    }

    #[test]
    fn managed_shell_names_match_cli_values() {
        assert_eq!(ManagedShell::Zsh.name(), "zsh");
        assert_eq!(ManagedShell::Fish.name(), "fish");
    }

    #[test]
    fn managed_shell_state_round_trips_without_dropping_other_state() {
        let dir = tempdir().unwrap();
        let path = dir.path().join("state.json");
        std::fs::write(
            &path,
            r#"{"config_version":17,"window_geometry":{"width":120,"height":40}}"#,
        )
        .unwrap();

        persist_managed_shell_to_path(&path, ManagedShell::Fish).unwrap();

        assert_eq!(
            read_managed_shell_from_path(&path),
            Some(ManagedShell::Fish)
        );
        let saved: serde_json::Value =
            serde_json::from_str(&std::fs::read_to_string(&path).unwrap()).unwrap();
        assert_eq!(saved["config_version"], 17);
        assert_eq!(saved["window_geometry"]["width"], 120);
    }

    #[test]
    fn invalid_managed_shell_state_is_ignored() {
        let dir = tempdir().unwrap();
        let path = dir.path().join("state.json");
        std::fs::write(&path, r#"{"managed_shell":"bash"}"#).unwrap();
        assert_eq!(read_managed_shell_from_path(&path), None);
    }

    #[test]
    fn completed_shell_state_sets_version_without_dropping_future_fields() {
        let dir = tempdir().unwrap();
        let path = dir.path().join("state.json");
        std::fs::write(
            &path,
            r#"{"window_position":{"x":10,"y":20},"future":{"enabled":true}}"#,
        )
        .unwrap();

        persist_initialized_state_to_path(&path, ManagedShell::Zsh, 31).unwrap();

        let saved: serde_json::Value =
            serde_json::from_str(&std::fs::read_to_string(&path).unwrap()).unwrap();
        assert_eq!(saved["managed_shell"], "zsh");
        assert_eq!(saved["config_version"], 31);
        assert_eq!(saved["window_position"]["x"], 10);
        assert_eq!(saved["future"]["enabled"], true);
    }
}
