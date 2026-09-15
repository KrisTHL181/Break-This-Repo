//! Stylelint CSS linter plugin.
//!
//! Detects Stylelint projects and marks config files as always used.
//! Parses config to extract referenced dependencies.

use super::{Plugin, PluginResult};

const ENABLERS: &[&str] = &["stylelint"];

const CONFIG_PATTERNS: &[&str] = &["stylelint.config.{js,cjs,mjs}", ".stylelintrc.{js,cjs}"];

const ALWAYS_USED: &[&str] = &[
    "stylelint.config.{js,cjs,mjs}",
    ".stylelintrc.{json,yaml,yml,js,cjs}",
];

const TOOLING_DEPENDENCIES: &[&str] = &[
    "stylelint",
    "stylelint-config-standard",
    "stylelint-config-recommended",
    "stylelint-order",
    "stylelint-scss",
    "postcss-scss",
];

define_plugin! {
    struct StylelintPlugin => "stylelint",
    enablers: ENABLERS,
    config_patterns: CONFIG_PATTERNS,
    always_used: ALWAYS_USED,
    tooling_dependencies: TOOLING_DEPENDENCIES,
    resolve_config: imports_only,
}
