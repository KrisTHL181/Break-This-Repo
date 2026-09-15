//! Cross-platform locations for persistent Helix CLI state.
//!
//! `HELIX_HOME` is the exact directory for Helix-owned state such as
//! credentials, configuration, update caches, and metrics. When it is unset or
//! empty, the CLI uses the platform home directory's `.helix` child.
//!
//! `HELIX_CACHE_DIR` independently overrides disposable CLI cache data. Its
//! fallback is the resolved Helix home so existing installations retain their
//! current layout.
//!
//! User-facing paths such as `helix chef`'s default project directory resolve
//! from `HOME` on Unix and `USERPROFILE` on Windows before falling back to the
//! operating system's home-directory API. Honoring the documented environment
//! variable first keeps scripting and isolated execution deterministic.

use eyre::{eyre, Result};
use std::{env, ffi, path};

const HELIX_HOME_ENV: &str = "HELIX_HOME";
const HELIX_CACHE_DIR_ENV: &str = "HELIX_CACHE_DIR";

#[cfg(not(windows))]
const USER_HOME_ENV: &str = "HOME";
#[cfg(windows)]
const USER_HOME_ENV: &str = "USERPROFILE";

/// Resolves the user's home for user-facing default paths and `~` expansion.
pub(crate) fn user_home_dir() -> Result<path::PathBuf> {
    resolve_user_home(env::var_os(USER_HOME_ENV), dirs::home_dir())
}

/// Resolves the directory that owns every persistent Helix CLI state file.
pub(crate) fn helix_home_dir() -> Result<path::PathBuf> {
    resolve_helix_home(env::var_os(HELIX_HOME_ENV), user_home_dir().ok())
}

/// Resolves disposable CLI cache storage without changing persistent state.
pub(crate) fn helix_cache_dir() -> Result<path::PathBuf> {
    resolve_helix_cache(env::var_os(HELIX_CACHE_DIR_ENV), helix_home_dir)
}

fn resolve_helix_home(
    override_dir: Option<ffi::OsString>,
    platform_home: Option<path::PathBuf>,
) -> Result<path::PathBuf> {
    non_empty_path(override_dir)
        .or_else(|| platform_home.map(|home| home.join(".helix")))
        .ok_or_else(|| {
            eyre!("cannot determine Helix home directory; set HELIX_HOME to a writable path")
        })
}

fn resolve_user_home(
    environment_home: Option<ffi::OsString>,
    platform_home: Option<path::PathBuf>,
) -> Result<path::PathBuf> {
    non_empty_path(environment_home)
        .or(platform_home)
        .ok_or_else(|| {
            eyre!(
                "cannot determine user home directory from {USER_HOME_ENV} or the operating system"
            )
        })
}

fn non_empty_path(value: Option<ffi::OsString>) -> Option<path::PathBuf> {
    value
        .filter(|value| !value.is_empty())
        .map(path::PathBuf::from)
}

fn resolve_helix_cache(
    override_dir: Option<ffi::OsString>,
    fallback: impl FnOnce() -> Result<path::PathBuf>,
) -> Result<path::PathBuf> {
    let Some(cache_dir) = non_empty_path(override_dir) else {
        return fallback();
    };
    Ok(cache_dir)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn environment_user_home_wins_over_platform_discovery() {
        let resolved = resolve_user_home(
            Some(ffi::OsString::from("environment-home")),
            Some(path::PathBuf::from("platform-home")),
        );

        assert_eq!(resolved.unwrap(), path::PathBuf::from("environment-home"));
    }

    #[test]
    fn empty_environment_user_home_uses_platform_discovery() {
        let resolved = resolve_user_home(
            Some(ffi::OsString::new()),
            Some(path::PathBuf::from("platform-home")),
        );

        assert_eq!(resolved.unwrap(), path::PathBuf::from("platform-home"));
    }

    #[test]
    fn missing_user_home_is_reported() {
        assert!(resolve_user_home(None, None).is_err());
    }

    #[test]
    fn explicit_helix_home_wins_on_every_platform() {
        let resolved = resolve_helix_home(
            Some(ffi::OsString::from("isolated-helix-home")),
            Some(path::PathBuf::from("platform-home")),
        );

        assert_eq!(
            resolved.unwrap(),
            path::PathBuf::from("isolated-helix-home")
        );
    }

    #[test]
    fn empty_override_uses_platform_default() {
        let resolved = resolve_helix_home(
            Some(ffi::OsString::new()),
            Some(path::PathBuf::from("platform-home")),
        );

        assert_eq!(
            resolved.unwrap(),
            path::PathBuf::from("platform-home").join(".helix")
        );
    }

    #[test]
    fn missing_override_and_platform_home_is_unrepresentable() {
        assert!(resolve_helix_home(None, None).is_err());
    }

    #[test]
    fn explicit_cache_directory_skips_the_fallback() {
        let resolved = resolve_helix_cache(Some(ffi::OsString::from("isolated-cache")), || {
            panic!("an explicit cache directory must not resolve Helix home")
        });

        assert_eq!(resolved.unwrap(), path::PathBuf::from("isolated-cache"));
    }

    #[test]
    fn empty_cache_override_uses_helix_home() {
        let resolved = resolve_helix_cache(Some(ffi::OsString::new()), || {
            Ok(path::PathBuf::from("helix-home"))
        });

        assert_eq!(resolved.unwrap(), path::PathBuf::from("helix-home"));
    }
}
