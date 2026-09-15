//! Changesets versioning plugin.
//!
//! Detects Changesets projects and marks config files as always used.

use super::Plugin;

const ENABLERS: &[&str] = &["@changesets/cli"];

const ALWAYS_USED: &[&str] = &[".changeset/config.json"];

const TOOLING_DEPENDENCIES: &[&str] = &[
    "@changesets/cli",
    "@changesets/changelog-github",
    "@changesets/changelog-git",
];

define_plugin! {
    struct ChangesetsPlugin => "changesets",
    enablers: ENABLERS,
    always_used: ALWAYS_USED,
    tooling_dependencies: TOOLING_DEPENDENCIES,
}
