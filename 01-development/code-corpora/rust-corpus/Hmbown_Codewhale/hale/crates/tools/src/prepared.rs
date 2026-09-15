use serde_json::Value;

use crate::{ApprovalRequirement, ResourceClaim};

/// A side-effect-free, input-specific tool decision prepared for authority
/// checks and scheduling.
#[derive(Debug, Clone, PartialEq)]
pub struct PreparedToolCall {
    pub name: String,
    pub input: Value,
    pub description: String,
    pub read_only: bool,
    pub supports_parallel: bool,
    pub starts_detached: bool,
    pub approval: ApprovalRequirement,
    pub resources: Vec<ResourceClaim>,
}
