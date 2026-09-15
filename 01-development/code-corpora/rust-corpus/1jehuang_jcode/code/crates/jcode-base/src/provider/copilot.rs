//! Copilot pure model-catalog data (compatibility shim).
//!
//! The GitHub Copilot provider *runtime* (`CopilotApiProvider`) now lives in
//! the downstream `jcode-provider-copilot-runtime` crate so provider edits do
//! not rebuild the base -> app-core -> tui spine. The binary's composition
//! root registers it via [`crate::provider::external`]. Base keeps only the
//! pure model-catalog data (from `jcode-provider-copilot`) that its routing
//! logic needs, plus a credentials probe that delegates to auth.

pub use jcode_provider_copilot::{DEFAULT_MODEL, FALLBACK_MODELS, is_known_display_model};
pub use jcode_provider_core::PremiumMode;

/// Whether GitHub Copilot credentials are present (GitHub OAuth token).
///
/// Kept here (not only in `auth::copilot`) because provider routing has
/// historically probed credentials through the provider module.
pub fn has_credentials() -> bool {
    crate::auth::copilot::has_copilot_credentials()
}
