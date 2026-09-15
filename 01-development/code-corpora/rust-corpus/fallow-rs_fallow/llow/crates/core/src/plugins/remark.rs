//! Remark plugin.
//!
//! Detects remark projects and marks config files as always used.
//! Parses JS/CJS config files to extract referenced dependencies.

use super::{Plugin, PluginResult};

const ENABLERS: &[&str] = &["remark", "remark-cli"];

const CONFIG_PATTERNS: &[&str] = &[".remarkrc.{js,cjs,mjs}"];

const ALWAYS_USED: &[&str] = &[".remarkrc", ".remarkrc.{js,cjs,mjs,json,yml,yaml}"];

const TOOLING_DEPENDENCIES: &[&str] = &["remark", "remark-cli"];

define_plugin! {
    struct RemarkPlugin => "remark",
    enablers: ENABLERS,
    config_patterns: CONFIG_PATTERNS,
    always_used: ALWAYS_USED,
    tooling_dependencies: TOOLING_DEPENDENCIES,
    resolve_config: imports_only,
}
