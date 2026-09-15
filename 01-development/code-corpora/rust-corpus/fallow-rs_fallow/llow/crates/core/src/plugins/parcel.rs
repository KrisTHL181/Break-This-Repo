//! Parcel plugin.
//!
//! Detects Parcel bundler projects and marks config files as always used.

use super::Plugin;

const ENABLERS: &[&str] = &["parcel", "@parcel/"];

const ENTRY_PATTERNS: &[&str] = &["index.html"];

const ALWAYS_USED: &[&str] = &[".parcelrc"];

const TOOLING_DEPENDENCIES: &[&str] = &["parcel"];

define_plugin! {
    struct ParcelPlugin => "parcel",
    enablers: ENABLERS,
    entry_patterns: ENTRY_PATTERNS,
    always_used: ALWAYS_USED,
    tooling_dependencies: TOOLING_DEPENDENCIES,
}
