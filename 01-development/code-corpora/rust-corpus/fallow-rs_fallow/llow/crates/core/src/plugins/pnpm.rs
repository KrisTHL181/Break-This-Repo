//! pnpm package manager plugin.
//!
//! Marks pnpm workspace and configuration files as always used so they are
//! not flagged as unused. Activates when `pnpm-workspace.yaml` or
//! `pnpm-lock.yaml` exists, since pnpm is rarely a package.json dependency.

use std::path::Path;

use fallow_config::PackageJson;

use super::Plugin;

const ALWAYS_USED: &[&str] = &[
    "pnpm-workspace.yaml",
    "pnpm-lock.yaml",
    ".pnpmfile.cjs",
    ".pnpmfile.mjs",
    ".npmrc",
];

const TOOLING_DEPENDENCIES: &[&str] = &["pnpm"];

pub struct PnpmPlugin;

impl Plugin for PnpmPlugin {
    fn name(&self) -> &'static str {
        "pnpm"
    }

    fn enablers(&self) -> &'static [&'static str] {
        &["pnpm"]
    }

    fn is_enabled(&self, pkg: &PackageJson, root: &Path) -> bool {
        root.join("pnpm-workspace.yaml").exists() || root.join("pnpm-lock.yaml").exists() || {
            let deps = pkg.all_dependency_names();
            self.is_enabled_with_deps(&deps, root)
        }
    }

    fn is_enabled_with_deps(&self, deps: &[String], _root: &Path) -> bool {
        deps.iter().any(|d| d == "pnpm")
    }

    fn always_used(&self) -> &'static [&'static str] {
        ALWAYS_USED
    }

    fn tooling_dependencies(&self) -> &'static [&'static str] {
        TOOLING_DEPENDENCIES
    }
}

#[cfg(test)]
mod tests {
    use std::fs;

    use super::*;

    #[test]
    fn is_enabled_with_pnpm_dep() {
        let plugin = PnpmPlugin;
        let deps = vec!["pnpm".to_string()];
        assert!(plugin.is_enabled_with_deps(&deps, Path::new("/project")));
    }

    #[test]
    fn is_not_enabled_without_pnpm_dep() {
        let plugin = PnpmPlugin;
        let deps = vec!["npm".to_string()];
        assert!(!plugin.is_enabled_with_deps(&deps, Path::new("/project")));
    }

    #[test]
    fn is_not_enabled_with_empty_deps() {
        let plugin = PnpmPlugin;
        let deps: Vec<String> = vec![];
        assert!(!plugin.is_enabled_with_deps(&deps, Path::new("/project")));
    }

    #[test]
    fn is_enabled_when_pnpm_lock_yaml_exists() {
        let dir = tempfile::tempdir().unwrap();
        fs::write(dir.path().join("pnpm-lock.yaml"), "lockfileVersion: 9\n").unwrap();

        let pkg = PackageJson::default();
        let plugin = PnpmPlugin;
        assert!(plugin.is_enabled(&pkg, dir.path()));
    }

    #[test]
    fn is_enabled_when_pnpm_workspace_yaml_exists() {
        let dir = tempfile::tempdir().unwrap();
        fs::write(
            dir.path().join("pnpm-workspace.yaml"),
            "packages:\n  - packages/*\n",
        )
        .unwrap();

        let pkg = PackageJson::default();
        let plugin = PnpmPlugin;
        assert!(plugin.is_enabled(&pkg, dir.path()));
    }

    #[test]
    fn is_not_enabled_in_empty_directory() {
        let dir = tempfile::tempdir().unwrap();

        let pkg = PackageJson::default();
        let plugin = PnpmPlugin;
        assert!(!plugin.is_enabled(&pkg, dir.path()));
    }

    #[test]
    fn is_not_enabled_for_nonexistent_root() {
        let pkg = PackageJson::default();
        let plugin = PnpmPlugin;
        assert!(!plugin.is_enabled(&pkg, Path::new("/nonexistent/path/that/does/not/exist")));
    }

    #[test]
    fn always_used_includes_lockfile() {
        let plugin = PnpmPlugin;
        assert!(plugin.always_used().contains(&"pnpm-lock.yaml"));
    }

    #[test]
    fn always_used_includes_workspace_yaml() {
        let plugin = PnpmPlugin;
        assert!(plugin.always_used().contains(&"pnpm-workspace.yaml"));
    }

    #[test]
    fn always_used_includes_pnpmfile_variants() {
        let plugin = PnpmPlugin;
        assert!(plugin.always_used().contains(&".pnpmfile.cjs"));
        assert!(plugin.always_used().contains(&".pnpmfile.mjs"));
    }

    #[test]
    fn always_used_includes_npmrc() {
        let plugin = PnpmPlugin;
        assert!(plugin.always_used().contains(&".npmrc"));
    }

    #[test]
    fn tooling_dependencies_include_pnpm() {
        let plugin = PnpmPlugin;
        assert!(plugin.tooling_dependencies().contains(&"pnpm"));
    }
}
