//! Issue #462: data-driven tooling catalogue + prefer plugin-config parsing.
//!
//! The framework-plugin packages `vite-plugin-*` / `prettier-plugin-*` were
//! removed from the general tooling catalogue. They are now credited ONLY when
//! they actually appear in the parsed config file, so a declared-but-unused
//! plugin correctly surfaces as unused instead of being hidden by an exact-name
//! shadow match. These tests pin both directions (credited-when-used,
//! reported-when-unused) across config forms.

use super::common::{create_config, fixture_path};

fn unused_dev_deps(fixture: &str) -> Vec<String> {
    let root = fixture_path(fixture);
    let config = create_config(root);
    let results = fallow_core::analyze(&config).expect("analysis should succeed");
    results
        .unused_dev_dependencies
        .iter()
        .map(|dep| dep.dep.package_name.clone())
        .collect()
}

#[test]
fn declared_but_unused_vite_plugins_now_surface() {
    let unused = unused_dev_deps("issue-462-vite-unused-plugin");

    assert!(
        !unused.contains(&"vite-plugin-inspect".to_string()),
        "vite-plugin-inspect is imported in vite.config.ts and must be credited, got {unused:?}"
    );

    assert!(
        unused.contains(&"vite-plugin-svgr".to_string()),
        "vite-plugin-svgr is declared but unused and must surface, got {unused:?}"
    );
    assert!(
        unused.contains(&"vite-plugin-eslint".to_string()),
        "vite-plugin-eslint is declared but unused and must surface, got {unused:?}"
    );

    assert!(
        unused.contains(&"unused-control".to_string()),
        "unused-control should still be reported, got {unused:?}"
    );
}

#[test]
fn vite_cjs_config_credits_required_plugin() {
    let unused = unused_dev_deps("issue-462-vite-cjs-plugin");

    assert!(
        !unused.contains(&"vite-plugin-svgr".to_string()),
        "vite-plugin-svgr is required in vite.config.cjs and must be credited, got {unused:?}"
    );
    assert!(
        unused.contains(&"vite-plugin-eslint".to_string()),
        "vite-plugin-eslint is declared but not required and must surface, got {unused:?}"
    );
}

#[test]
fn prettier_yaml_config_credits_listed_plugin() {
    let unused = unused_dev_deps("issue-462-prettier-yaml");

    assert!(
        !unused.contains(&"prettier-plugin-tailwindcss".to_string()),
        "prettier-plugin-tailwindcss is listed in .prettierrc.yaml and must be credited, got {unused:?}"
    );
    assert!(
        unused.contains(&"prettier-plugin-organize-imports".to_string()),
        "prettier-plugin-organize-imports is declared but not listed and must surface, got {unused:?}"
    );
    assert!(
        unused.contains(&"unused-control".to_string()),
        "unused-control should still be reported, got {unused:?}"
    );
}

#[test]
fn prettier_package_json_config_credits_listed_plugin() {
    let unused = unused_dev_deps("issue-462-prettier-pkg-json");

    assert!(
        !unused.contains(&"prettier-plugin-svelte".to_string()),
        "prettier-plugin-svelte is listed in package.json#prettier and must be credited, got {unused:?}"
    );
    assert!(
        unused.contains(&"unused-control".to_string()),
        "unused-control should still be reported, got {unused:?}"
    );
}
