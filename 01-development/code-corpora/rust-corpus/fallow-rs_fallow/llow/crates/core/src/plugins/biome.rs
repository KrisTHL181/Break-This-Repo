//! Biome linter/formatter plugin.
//!
//! Detects Biome projects and marks config files as always used.

use super::Plugin;

const ENABLERS: &[&str] = &["@biomejs/biome"];

const ALWAYS_USED: &[&str] = &["biome.json", "biome.jsonc"];

const TOOLING_DEPENDENCIES: &[&str] = &["@biomejs/biome"];

define_plugin! {
    struct BiomePlugin => "biome",
    enablers: ENABLERS,
    always_used: ALWAYS_USED,
    tooling_dependencies: TOOLING_DEPENDENCIES,
}
