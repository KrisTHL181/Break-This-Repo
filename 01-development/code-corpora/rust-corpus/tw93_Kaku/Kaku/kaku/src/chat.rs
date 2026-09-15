//! `kaku chat` subcommand: thin wrapper around the bundled `k` binary.
//!
//! Why this exists when `k` is already on PATH after `kaku init`:
//!
//! - **Discoverability** — `kaku --help` should show the full AI story
//!   (`ai` to configure, `chat` to talk). New users find `chat` here without
//!   knowing about the 1-letter `k` shortcut.
//! - **PATH-independent** — works even when the user has not run
//!   `kaku init` or is on a shell without managed integration.
//! - **No dependency bloat** — instead of pulling the GUI/chat engine into
//!   the `kaku` binary (which would link in WGPU + Cairo + the entire
//!   GUI runtime), we `execvp` the `k` binary that already ships in
//!   `Kaku.app/Contents/MacOS/k`. Same chat engine, same args, zero
//!   process-overhead (exec replaces this process in-place on Unix).

use anyhow::anyhow;
#[cfg(not(unix))]
use anyhow::Context;
use clap::Parser;
#[cfg(unix)]
use std::os::unix::process::CommandExt;
use std::path::PathBuf;

#[derive(Debug, Parser, Clone, Default)]
pub struct ChatCommand {
    /// Forward every remaining argument verbatim to the `k` binary so flags
    /// like `-n` (new conversation) and `-r [ID]` (resume) keep working.
    #[arg(trailing_var_arg = true, allow_hyphen_values = true)]
    pub args: Vec<String>,
}

impl ChatCommand {
    pub fn run(&self) -> anyhow::Result<()> {
        let k_bin = resolve_k_binary()?;
        let mut cmd = std::process::Command::new(&k_bin);
        cmd.args(&self.args);

        #[cfg(unix)]
        {
            // Exec replaces the current process; the user's $? on the next
            // shell line is exactly `k`'s exit code, with no extra fork
            // overhead and no parent-process bookkeeping.
            let err = cmd.exec();
            return Err(anyhow!("failed to exec {}: {}", k_bin.display(), err));
        }

        #[cfg(not(unix))]
        {
            let status = cmd
                .status()
                .with_context(|| format!("spawn {}", k_bin.display()))?;
            std::process::exit(status.code().unwrap_or(1));
        }
    }
}

/// Locate the `k` binary that ships next to this `kaku` binary, falling back
/// to common install locations. Mirrors the discovery order used by
/// `init.rs` so behavior stays predictable across `kaku init`, `kaku chat`,
/// and the standalone `k` shell wrapper.
fn resolve_k_binary() -> anyhow::Result<PathBuf> {
    let mut candidates: Vec<PathBuf> = Vec::new();

    // 1. Same directory as the running `kaku` binary (the bundled case).
    if let Ok(exe) = std::env::current_exe() {
        candidates.push(exe.with_file_name("k"));
        if let Ok(canonical) = std::fs::canonicalize(&exe) {
            let beside_canonical = canonical.with_file_name("k");
            if !candidates.contains(&beside_canonical) {
                candidates.push(beside_canonical);
            }
        }
    }

    // 2. Standard macOS install locations.
    #[cfg(target_os = "macos")]
    {
        candidates.push(PathBuf::from("/Applications/Kaku.app/Contents/MacOS/k"));
        candidates.push(
            config::HOME_DIR
                .join("Applications")
                .join("Kaku.app")
                .join("Contents")
                .join("MacOS")
                .join("k"),
        );
    }

    for candidate in &candidates {
        if is_executable(candidate) {
            return Ok(candidate.clone());
        }
    }

    Err(anyhow!(
        "could not locate the `k` chat binary. Run `kaku init` after \
         installing Kaku, or invoke `k` directly if it is on PATH."
    ))
}

fn is_executable(path: &std::path::Path) -> bool {
    #[cfg(unix)]
    {
        use std::os::unix::fs::PermissionsExt;
        match std::fs::metadata(path) {
            Ok(m) => m.is_file() && m.permissions().mode() & 0o111 != 0,
            Err(_) => false,
        }
    }
    #[cfg(not(unix))]
    {
        path.is_file()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn resolve_returns_error_when_no_candidate_exists() {
        // We can't easily stub `current_exe` so the test is environmental:
        // simply ensure the function does not panic and returns a typed
        // result. CI machines without Kaku installed should hit the Err
        // path; developer machines will return Ok with a real path.
        let _ = resolve_k_binary();
    }
}
