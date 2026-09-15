//! Node-tap test runner plugin.
//!
//! Detects tap projects and marks tap test files as entry points.

use super::Plugin;

const ENABLERS: &[&str] = &["tap"];

const ENTRY_PATTERNS: &[&str] = &[
    "test/**/*.{js,cjs,mjs,jsx,ts,cts,mts,tsx}",
    "tests/**/*.{js,cjs,mjs,jsx,ts,cts,mts,tsx}",
    "__tests__/**/*.{js,cjs,mjs,jsx,ts,cts,mts,tsx}",
    "**/*.test.{js,cjs,mjs,jsx,ts,cts,mts,tsx}",
    "**/*.spec.{js,cjs,mjs,jsx,ts,cts,mts,tsx}",
    "test.{js,cjs,mjs,jsx,ts,cts,mts,tsx}",
    "tests.{js,cjs,mjs,jsx,ts,cts,mts,tsx}",
];

const CONFIG_PATTERNS: &[&str] = &[".taprc", ".taprc.{json,yml,yaml}", "config/taprc"];

const ALWAYS_USED: &[&str] = &[".taprc", ".taprc.{json,yml,yaml}", "config/taprc"];

const TOOLING_DEPENDENCIES: &[&str] = &["tap"];

define_plugin! {
    struct TapPlugin => "tap",
    enablers: ENABLERS,
    entry_patterns: ENTRY_PATTERNS,
    config_patterns: CONFIG_PATTERNS,
    always_used: ALWAYS_USED,
    tooling_dependencies: TOOLING_DEPENDENCIES,
}

#[cfg(test)]
mod tests {
    use std::path::Path;

    use super::*;

    #[test]
    fn is_enabled_with_deps() {
        let plugin = TapPlugin;
        assert!(plugin.is_enabled_with_deps(&["tap".to_string()], Path::new("/project")));
        assert!(!plugin.is_enabled_with_deps(&["mocha".to_string()], Path::new("/project")));
    }

    #[test]
    fn entry_patterns_include_tap_defaults() {
        let plugin = TapPlugin;
        let patterns = plugin.entry_patterns();

        assert!(patterns.contains(&"test/**/*.{js,cjs,mjs,jsx,ts,cts,mts,tsx}"));
        assert!(patterns.contains(&"tests/**/*.{js,cjs,mjs,jsx,ts,cts,mts,tsx}"));
        assert!(patterns.contains(&"**/*.test.{js,cjs,mjs,jsx,ts,cts,mts,tsx}"));
    }

    #[test]
    fn tooling_dependencies_include_tap() {
        let plugin = TapPlugin;
        assert!(plugin.tooling_dependencies().contains(&"tap"));
    }
}
