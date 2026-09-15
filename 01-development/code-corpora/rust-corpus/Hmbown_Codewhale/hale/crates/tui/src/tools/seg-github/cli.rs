//! Command construction: every `gh` and `git` process this tool runs.
//!
//! Nothing above this file assembles an argv or resolves a binary path, so
//! "what did the tool actually shell out to" has exactly one answer.

use std::process::Command;

use serde_json::Value;

use crate::dependencies::ExternalTool;
use crate::tools::spec::{ToolContext, ToolError};

const DEFAULT_GH: &str = "/opt/homebrew/bin/gh";
const FALLBACK_GH_PATHS: &[&str] = &[
    "/usr/bin/gh",                       // Linux system package manager
    "/usr/local/bin/gh",                 // macOS Intel Homebrew / manual install
    "/home/linuxbrew/.linuxbrew/bin/gh", // Linux Homebrew (official prefix)
    "/opt/homebrew/bin/gh",              // macOS Apple Silicon Homebrew
];

fn gh_bin() -> String {
    if let Ok(bin) = std::env::var("CODEWHALE_GH_BIN").or_else(|_| std::env::var("DEEPSEEK_GH_BIN"))
    {
        return bin;
    }
    for path in FALLBACK_GH_PATHS {
        if std::path::Path::new(path).is_file() {
            return path.to_string();
        }
    }
    DEFAULT_GH.to_string()
}

pub(super) fn run_gh_text(context: &ToolContext, args: &[&str]) -> Result<String, ToolError> {
    let out = Command::new(gh_bin())
        .args(args)
        .current_dir(&context.workspace)
        .output()
        .map_err(|e| {
            if e.kind() == std::io::ErrorKind::NotFound {
                ToolError::not_available("gh CLI not found; install it or set DEEPSEEK_GH_BIN")
            } else {
                ToolError::execution_failed(format!("failed to run gh: {e}"))
            }
        })?;
    if !out.status.success() {
        return Err(ToolError::execution_failed(format!(
            "gh {} failed: {}",
            args.join(" "),
            String::from_utf8_lossy(&out.stderr).trim()
        )));
    }
    Ok(String::from_utf8_lossy(&out.stdout).to_string())
}

pub(super) fn run_gh_json(context: &ToolContext, args: &[&str]) -> Result<Value, ToolError> {
    let text = run_gh_text(context, args)?;
    serde_json::from_str(&text).map_err(|e| ToolError::execution_failed(e.to_string()))
}

pub(super) fn ensure_github_repo(context: &ToolContext) -> Result<(), ToolError> {
    let out = crate::dependencies::Git::output(
        &["rev-parse", "--is-inside-work-tree"],
        &context.workspace,
    )
    .map_err(|e| ToolError::execution_failed(format!("failed to run git: {e}")))?;
    if out.status.success() {
        Ok(())
    } else {
        Err(ToolError::not_available(
            "current workspace is not a git repository",
        ))
    }
}

pub(super) fn git_status_porcelain(context: &ToolContext) -> Result<String, ToolError> {
    let out = crate::dependencies::Git::output(&["status", "--porcelain"], &context.workspace)
        .map_err(|e| ToolError::execution_failed(format!("failed to run git status: {e}")))?;
    Ok(String::from_utf8_lossy(&out.stdout).to_string())
}
