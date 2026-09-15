//! Hotbar action registry foundation.
//!
//! Config, sidebar rendering, and key dispatch consume this action surface and
//! the built-in actions defined here.

pub mod actions;
pub mod setup;

pub use actions::HotbarActionRegistry;
