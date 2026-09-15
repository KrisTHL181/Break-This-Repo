//! Home-directory compatibility adapter, kept as an includable leaf.
//!
//! This is one function that could live in `paths.rs` — and did, until #4757
//! made it the crate-wide replacement for `dirs::home_dir()`. Two of the new
//! call sites (`network_policy.rs`, `skills/install.rs`) are pulled into the
//! `skill_cli` integration test via `#[path]` includes, where `crate::` means
//! the test binary's root rather than the lib. Anything they reach for has to
//! be includable there too.
//!
//! `paths.rs` is not: its `env_config_path` calls `crate::test_support` under
//! `#[cfg(test)]`, and integration test binaries compile *with* `cfg(test)`
//! set, so including it drags in `test_support` — and then
//! `config_persistence` behind that. Splitting this function out keeps the
//! includable surface to `std` + `codewhale-paths` with no `crate::` references
//! at all, so the test binary picks up production behavior verbatim instead of
//! a divergent shim.
//!
//! `paths.rs` re-exports this so `config::effective_home_dir` and every
//! existing `use paths::{...}` caller resolve unchanged. The implementation
//! delegates to the workspace's leaf path-authority crate, which is also safe
//! to reference from the integration-test binary that includes this file.

use std::path::PathBuf;

/// Resolve the user's home directory, preferring the environment over the
/// platform lookup so tests can fake it consistently across OSes (#4757).
///
/// `HOME` and `USERPROFILE` are checked first (empty values are treated as
/// unset, since an empty home is never a usable path), then the Windows
/// `HOMEDRIVE`/`HOMEPATH` pair, and only then `dirs::home_dir()`. Keeping the
/// OS lookup last is what makes a faked environment win in tests while
/// production still falls back to the real thing.
pub(crate) fn effective_home_dir() -> Option<PathBuf> {
    codewhale_paths::user_home()
}
