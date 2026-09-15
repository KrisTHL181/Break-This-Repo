//! rvAgent A2A — Agent2Agent peer-to-peer protocol.
//!
//! Implements the Google A2A spec (JSON-RPC 2.0 over HTTP, `/.well-known/agent.json`
//! discovery, `text/event-stream` streaming, HMAC-signed push webhooks) with the
//! ruvector extensions described in [ADR-159]:
//!
//! - **r2 identity:** signed `AgentCard`s with content-addressed `AgentID`s.
//! - **r2 policy / routing / typed artifacts:** per-task `TaskPolicy`, pluggable
//!   `PeerSelector`, typed `ArtifactKind` incl. `RuLakeWitness` for zero-copy
//!   vector handoff.
//! - **r3 global budget / trace causality / recursion guard:** `GlobalBudget`
//!   at the dispatch queue, `TaskContext` propagation, and a cycle / depth
//!   guard before runner dispatch.
//!
//! This crate is a library — users mount [`server::A2aServer`] into their own
//! axum binary (typically alongside the `rvagent-acp` router).
//!
//! [ADR-159]: ../../../docs/adr/ADR-159-rvagent-a2a-protocol.md

// Core spec types + error taxonomy — this agent owns these.
pub mod error;
pub mod types;

// Transport — this agent owns these.
pub mod client;
pub mod server;

// ---------------------------------------------------------------------------
// Sibling modules owned by other agents (ADR-159 r2 / r3 implementations).
//
// We declare them here so `rvagent-a2a::*` resolves for downstream crates
// even while parallel work is in flight. If a module file has not been
// written yet at build time the compile error points at the right author.
// ---------------------------------------------------------------------------
pub mod artifact_types;
pub mod budget;
pub mod config;
pub mod context;
pub mod executor;
pub mod identity;
pub mod policy;
pub mod recursion_guard;
pub mod routing;

// ---------------------------------------------------------------------------
// Crate-level re-exports. Keep this list small and stable — anything here
// is effectively our public surface.
// ---------------------------------------------------------------------------
pub use error::A2aError;
pub use types::{
    AgentCapabilities, AgentCard, AgentProvider, AgentSkill, Artifact, AuthScheme, FileContent,
    Message, Part, Role, Task, TaskArtifactUpdateEvent, TaskSpec, TaskState, TaskStatus,
    TaskStatusUpdateEvent,
};

/// Crate version, wired from `CARGO_PKG_VERSION`.
pub const VERSION: &str = env!("CARGO_PKG_VERSION");
