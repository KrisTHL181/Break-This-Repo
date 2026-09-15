//! Commitlint plugin.
//!
//! Detects Commitlint projects and marks config files as always used.
//! Parses config to extract referenced dependencies.

use super::{Plugin, PluginResult};

const ENABLERS: &[&str] = &["@commitlint/cli"];

const CONFIG_PATTERNS: &[&str] = &[
    "commitlint.config.{js,cjs,mjs,ts}",
    ".commitlintrc.{js,cjs}",
];

const ALWAYS_USED: &[&str] = &[
    "commitlint.config.{js,cjs,mjs,ts}",
    ".commitlintrc.{json,yaml,yml,js,cjs}",
];

const TOOLING_DEPENDENCIES: &[&str] = &[
    "@commitlint/cli",
    "@commitlint/config-conventional",
    "@commitlint/config-angular",
];

define_plugin! {
    struct CommitlintPlugin => "commitlint",
    enablers: ENABLERS,
    config_patterns: CONFIG_PATTERNS,
    always_used: ALWAYS_USED,
    tooling_dependencies: TOOLING_DEPENDENCIES,
    resolve_config: imports_only,
}
