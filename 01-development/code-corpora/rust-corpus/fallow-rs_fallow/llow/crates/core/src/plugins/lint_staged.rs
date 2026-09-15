//! lint-staged plugin.
//!
//! Detects lint-staged projects and marks config files as always used.
//! Parses JS/CJS config files to extract referenced dependencies.

use super::{Plugin, PluginResult};

const ENABLERS: &[&str] = &["lint-staged"];

const CONFIG_PATTERNS: &[&str] = &[
    "lint-staged.config.{js,cjs,mjs,ts}",
    ".lintstagedrc.{js,cjs,mjs,ts}",
];

const ALWAYS_USED: &[&str] = &[
    "lint-staged.config.{js,cjs,mjs,ts}",
    ".lintstagedrc",
    ".lintstagedrc.{json,yaml,yml,js,cjs,mjs,ts}",
];

const TOOLING_DEPENDENCIES: &[&str] = &["lint-staged"];

define_plugin! {
    struct LintStagedPlugin => "lint-staged",
    enablers: ENABLERS,
    config_patterns: CONFIG_PATTERNS,
    always_used: ALWAYS_USED,
    tooling_dependencies: TOOLING_DEPENDENCIES,
    resolve_config: imports_only,
}
