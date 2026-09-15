//! Mocha test runner plugin.
//!
//! Detects Mocha projects and marks test files as entry points.

use super::Plugin;

const ENABLERS: &[&str] = &["mocha"];

const ENTRY_PATTERNS: &[&str] = &[
    "test/**/*.{ts,tsx,js,jsx}",
    "tests/**/*.{ts,tsx,js,jsx}",
    "spec/**/*.{ts,tsx,js,jsx}",
    "**/*.test.{ts,tsx,js,jsx}",
    "**/*.spec.{ts,tsx,js,jsx}",
];

const ALWAYS_USED: &[&str] = &[".mocharc.{json,yaml,yml,js,cjs,mjs}"];

const TOOLING_DEPENDENCIES: &[&str] = &["mocha", "@types/mocha", "ts-mocha"];

define_plugin! {
    struct MochaPlugin => "mocha",
    enablers: ENABLERS,
    entry_patterns: ENTRY_PATTERNS,
    always_used: ALWAYS_USED,
    tooling_dependencies: TOOLING_DEPENDENCIES,
}
