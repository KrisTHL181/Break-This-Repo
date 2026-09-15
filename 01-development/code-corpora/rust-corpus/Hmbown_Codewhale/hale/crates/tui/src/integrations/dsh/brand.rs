//! Whale Brothers / Codewhale identity lockup for the DSH bundle skin.
//!
//! The palette and ocean scene can be mistaken for an ordinary theme. This
//! small, plugin-owned React surface uses DSH's additive `shell.overlay` Slot
//! to make the relationship explicit without replacing or modifying DSH's own
//! product identity.

use super::identity::sha256_hex;

/// Plain client script fragment spliced into the generated bundle client.
pub(crate) fn bundle_brand_js() -> &'static str {
    include_str!("brand.js")
}

pub(crate) fn brand_sha256() -> String {
    sha256_hex(bundle_brand_js().as_bytes())
}
