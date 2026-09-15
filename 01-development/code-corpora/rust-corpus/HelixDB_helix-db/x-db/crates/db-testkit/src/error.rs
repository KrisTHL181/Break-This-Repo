//! Testkit error boundary.

/// Result returned by testkit contracts.
pub type Result<T> = std::result::Result<T, TestkitError>;

/// Closed failures produced while constructing, validating, or replaying a workload.
#[derive(Debug, thiserror::Error)]
pub enum TestkitError {
    /// A string-backed identifier was empty.
    #[error("{kind} must not be empty")]
    EmptyIdentifier {
        /// Identifier role.
        kind: &'static str,
    },
    /// A numeric identifier that starts at one was zero.
    #[error("{kind} must be non-zero")]
    ZeroIdentifier {
        /// Identifier role.
        kind: &'static str,
    },
    /// A collection required at least one element.
    #[error("{kind} must not be empty")]
    EmptyCollection {
        /// Collection role.
        kind: &'static str,
    },
    /// A finite floating-point boundary rejected NaN or infinity.
    #[error("{kind} must be finite")]
    NonFinite {
        /// Floating-point role.
        kind: &'static str,
    },
    /// A closed interval had its endpoints reversed.
    #[error("range start {start} exceeds end {end}")]
    InvalidRange {
        /// Inclusive start.
        start: u64,
        /// Inclusive end.
        end: u64,
    },
    /// A model invariant was violated.
    #[error("model invariant violated: {0}")]
    ModelViolation(String),
    /// A serialized or recorded trace violated its contract.
    #[error("trace invariant violated: {0}")]
    TraceViolation(String),
    /// A runtime adapter could not execute a request.
    #[error("adapter failed: {0}")]
    Adapter(String),
    /// A regression corpus operation failed.
    #[error("regression corpus I/O failed: {0}")]
    CorpusIo(#[from] std::io::Error),
    /// Trace JSON encoding or decoding failed.
    #[error("trace JSON failed: {0}")]
    TraceJson(#[from] serde_json::Error),
    /// A planner rejected a generated case.
    #[error("planner rejected generated case: {0}")]
    Planner(String),
    /// The database rejected a fixture operation.
    #[error("database fixture failed: {0}")]
    Database(String),
}
