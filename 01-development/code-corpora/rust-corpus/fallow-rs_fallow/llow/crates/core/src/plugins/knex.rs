//! Knex.js query builder plugin.
//!
//! Detects Knex projects and marks migration and seed files as entry points.

use super::Plugin;

const ENABLERS: &[&str] = &["knex"];

const ENTRY_PATTERNS: &[&str] = &["migrations/**/*.{ts,js}", "seeds/**/*.{ts,js}"];

const ALWAYS_USED: &[&str] = &["knexfile.{ts,js}"];

const TOOLING_DEPENDENCIES: &[&str] = &["knex"];

define_plugin! {
    struct KnexPlugin => "knex",
    enablers: ENABLERS,
    entry_patterns: ENTRY_PATTERNS,
    always_used: ALWAYS_USED,
    tooling_dependencies: TOOLING_DEPENDENCIES,
}
