//! Helpers for invoking `git` from fallow without inheriting ambient repo
//! state from the parent process.
//!
//! When fallow is invoked from a git hook (`pre-commit`, `pre-push`,
//! `commit-msg`, ...) or a tool that wraps git (lint-staged, husky, lefthook,
//! pre-commit framework, IDE git integrations, some CI runners), git exports a
//! handful of environment variables describing the *enclosing* operation:
//! `GIT_INDEX_FILE`, `GIT_DIR`, `GIT_WORK_TREE`, `GIT_OBJECT_DIRECTORY`,
//! `GIT_COMMON_DIR`, `GIT_PREFIX`. Several of these are written as paths
//! relative to the parent's working directory (e.g. `GIT_INDEX_FILE=.git/index`
//! during `git commit`). When fallow then spawns its own `git` subprocess from
//! a different working directory (notably `git worktree add` against a
//! temporary path), the inherited relative paths no longer resolve and the
//! call fails.
//!
//! Fallow always operates against the repository at `--root` (or the cwd) and
//! never wants to share index / object / work-tree state with an enclosing
//! `git` operation, so the safe default is to strip these vars before every
//! `git` invocation.
//!
//! Vars that are *not* stripped: `GIT_AUTHOR_*`, `GIT_COMMITTER_*`, `GIT_EDITOR`,
//! `GIT_EXEC_PATH`. Those are either harmless to fallow's read-only git
//! invocations or required for fallow's tests that depend on the parent shell's
//! git config.

use std::process::Command;

/// Environment variables that describe an enclosing git operation's
/// repository state, in the order they appear in `git`'s own environment
/// documentation. Hook subprocesses inherit some or all of these, often as
/// paths relative to the parent's cwd, and they break fallow's git invocations
/// when fallow runs from a different cwd.
pub const AMBIENT_GIT_ENV_VARS: &[&str] = &[
    "GIT_DIR",
    "GIT_WORK_TREE",
    "GIT_INDEX_FILE",
    "GIT_OBJECT_DIRECTORY",
    "GIT_COMMON_DIR",
    "GIT_PREFIX",
];

/// Strip ambient git repository-state environment variables from a `Command`.
///
/// Apply to every `git` subprocess fallow spawns from production code. The
/// strip is unconditional and idempotent: `Command::env_remove` is a no-op
/// when the variable is not present in the inherited environment.
///
/// Returns the `Command` for fluent chaining alongside `.args()`,
/// `.current_dir()`, and so on.
pub fn clear_ambient_git_env(cmd: &mut Command) -> &mut Command {
    for var in AMBIENT_GIT_ENV_VARS {
        cmd.env_remove(var);
    }
    cmd
}

#[cfg(test)]
mod tests {
    use super::{AMBIENT_GIT_ENV_VARS, clear_ambient_git_env};
    use std::process::Command;

    #[test]
    fn clear_ambient_git_env_removes_every_listed_variable() {
        let mut cmd = Command::new("git");
        clear_ambient_git_env(&mut cmd);
        let envs: Vec<_> = cmd.get_envs().collect();
        for var in AMBIENT_GIT_ENV_VARS {
            assert!(
                envs.iter()
                    .any(|(key, value)| key.to_str() == Some(*var) && value.is_none()),
                "{var} should be cleared from the command env",
            );
        }
    }

    #[test]
    fn clear_ambient_git_env_is_idempotent() {
        let mut cmd = Command::new("git");
        clear_ambient_git_env(&mut cmd);
        clear_ambient_git_env(&mut cmd);
        let cleared = cmd.get_envs().filter(|(_, value)| value.is_none()).count();
        assert_eq!(
            cleared,
            AMBIENT_GIT_ENV_VARS.len(),
            "double-applying the helper should not duplicate env entries",
        );
    }
}
