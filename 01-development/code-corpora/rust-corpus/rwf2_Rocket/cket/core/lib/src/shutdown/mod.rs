//! Shutdown configuration and notification handle.

mod tripwire;
mod handle;
mod sig;
mod config;

pub(crate) use tripwire::TripWire;
pub(crate) use handle::Stages;

pub use config::ShutdownConfig;
pub use handle::Shutdown;
pub use sig::Sig;
