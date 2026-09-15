//! Mock Service Worker (MSW) plugin.
//!
//! Detects MSW projects and marks mock handler files as entry points.

use super::Plugin;

const ENABLERS: &[&str] = &["msw"];

const ENTRY_PATTERNS: &[&str] = &[
    "mocks/**/*.{ts,tsx,js,jsx}",
    "src/mocks/**/*.{ts,tsx,js,jsx}",
    "**/mocks/**/*.{ts,tsx,js,jsx}",
];

const ALWAYS_USED: &[&str] = &["public/mockServiceWorker.js"];

const TOOLING_DEPENDENCIES: &[&str] = &["msw", "msw-storybook-addon"];

define_plugin! {
    struct MswPlugin => "msw",
    enablers: ENABLERS,
    entry_patterns: ENTRY_PATTERNS,
    always_used: ALWAYS_USED,
    tooling_dependencies: TOOLING_DEPENDENCIES,
}
