//! Unified error type for the A2A crate.
//!
//! Maps cleanly onto JSON-RPC 2.0 error codes (see `server::json_rpc`) and
//! lifts the per-module errors (`PolicyError`, `BudgetError`) from the
//! sibling modules without redefining them.
//!
//! Reference: ADR-159 §Decision / §Risk analysis.

use thiserror::Error;

// These are defined by sibling agents. We import them here so every caller
// of this crate can pattern-match on a single top-level `A2aError` instead
// of juggling three error types.
use crate::budget::BudgetError;
use crate::policy::PolicyError;

/// Top-level error taxonomy for the A2A subcrate.
///
/// Every JSON-RPC handler returns `Result<T, A2aError>`. The JSON-RPC layer
/// maps each variant to an integer code + message in
/// [`crate::server::json_rpc::JsonRpcError`].
#[derive(Debug, Error)]
pub enum A2aError {
    /// The AgentCard's Ed25519 signature did not verify against the declared
    /// pubkey, or the canonical-JSON re-serialization diverged. Always
    /// terminal — never retried.
    #[error("agent card signature invalid")]
    CardSignatureInvalid,

    /// Failure to fetch / parse `/.well-known/agent.json`. Wraps transport
    /// and JSON-decode failures so callers can distinguish "peer down" from
    /// "peer is lying."
    #[error("discovery failed: {0}")]
    Discovery(String),

    /// Lower-level HTTP / TLS / connect failure. Corresponds to
    /// `reqwest::Error` in most call sites.
    #[error("transport error: {0}")]
    Transport(String),

    /// JSON-RPC framing violation — malformed request, unknown method,
    /// unexpected response envelope.
    #[error("jsonrpc error: {0}")]
    JsonRpc(String),

    /// Per-task policy rejection (allowed_skills, max_concurrency,
    /// max_cost_usd, max_tokens, max_duration_ms).
    #[error("policy violation: {0}")]
    PolicyError(#[from] PolicyError),

    /// Global per-minute budget rejection (tokens, usd, task count, queue
    /// full). Always surfaces before `PeerSelector` runs so we never pay
    /// for peer discovery on a task that would be shed anyway.
    #[error("budget exceeded: {0}")]
    BudgetExceeded(#[from] BudgetError),

    /// Recursion guard fired. `path` is the `visited_agents` chain up to
    /// (but not including) the rejected hop, useful for operator forensics.
    #[error("recursion limit at depth {depth}: path={path:?}")]
    RecursionLimit { depth: u32, path: Vec<String> },

    /// Artifact-kind version mismatch during send (sender advertises newer
    /// than receiver supports, receiver not opted in to
    /// `accept_lower_version_artifacts`).
    #[error("artifact kind version {got} not supported (this peer supports: {supported:?})")]
    ArtifactVersionUnsupported { got: String, supported: Vec<String> },

    /// Task / session / card lookup miss. Distinguished from `Discovery`
    /// so the client can decide whether to retry with a different target.
    #[error("not found: {0}")]
    NotFound(String),

    /// Any other error — keep the string short and human-readable.
    /// Prefer specific variants over this when possible.
    #[error("internal error: {0}")]
    Internal(String),
}

impl A2aError {
    /// Convenience constructor for reqwest errors.
    pub fn transport<E: std::fmt::Display>(e: E) -> Self {
        Self::Transport(e.to_string())
    }

    /// Convenience constructor for serde_json errors.
    pub fn json_rpc<E: std::fmt::Display>(e: E) -> Self {
        Self::JsonRpc(e.to_string())
    }
}

impl From<serde_json::Error> for A2aError {
    fn from(e: serde_json::Error) -> Self {
        A2aError::JsonRpc(e.to_string())
    }
}

impl From<reqwest::Error> for A2aError {
    fn from(e: reqwest::Error) -> Self {
        A2aError::Transport(e.to_string())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn display_recursion_limit_includes_depth() {
        let e = A2aError::RecursionLimit {
            depth: 9,
            path: vec!["agent-a".into(), "agent-b".into()],
        };
        let s = format!("{}", e);
        assert!(s.contains("depth 9"), "got: {}", s);
        assert!(s.contains("agent-a"), "got: {}", s);
    }

    #[test]
    fn display_artifact_version_mismatch() {
        let e = A2aError::ArtifactVersionUnsupported {
            got: "2".into(),
            supported: vec!["1".into()],
        };
        assert!(format!("{}", e).contains("2"));
    }
}
