//! r3 — Trace-level causality.
//!
//! A [`TaskContext`] is carried in every A2A message's
//! `metadata.ruvector.context` and threaded through subagent calls, making
//! the multi-agent call graph observable: root → child → grandchild, one
//! hop per task, regardless of local vs. remote executor.
//!
//! See ADR-159 "r3 — Trace-level causality".

use serde::{Deserialize, Serialize};

use crate::identity::AgentID;

/// Per-task causality record propagated through every A2A hop.
///
/// Compatible with W3C Trace Context: `trace_id` is a 32-char lowercase-hex
/// string (16 random bytes). External observability (Jaeger, Tempo,
/// Honeycomb) picks up the lineage automatically when the transport is
/// plain HTTP via the parallel `traceparent` header.
#[derive(Clone, Debug, Serialize, Deserialize, PartialEq, Eq)]
pub struct TaskContext {
    /// Root-scoped trace id — 32-char lowercase hex (16 bytes).
    /// Propagates unchanged through every descendant task.
    pub trace_id: String,

    /// Parent task id; `None` for the root task.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub parent_task_id: Option<String>,

    /// Depth in the call graph (0 = root). Bounded by
    /// `RecursionPolicy::max_call_depth`.
    pub depth: u32,

    /// `AgentID` of the root task's originating agent. Enables
    /// per-root-attribution of aggregate spend.
    pub root_agent_id: AgentID,

    /// `AgentID` of the agent currently processing this task. Defaults
    /// to `root_agent_id` for the root context; advanced via `.child()`.
    #[serde(default = "crate::context::default_current_agent")]
    pub current_agent: AgentID,

    /// Every agent id the task has transited, in order. Drives the
    /// recursion cycle-check + operator forensics.
    #[serde(default)]
    pub visited_agents: Vec<AgentID>,
}

#[doc(hidden)]
pub fn default_current_agent() -> AgentID {
    AgentID(String::new())
}

impl TaskContext {
    /// Open a new root context for a task originated at `root`.
    ///
    /// Note: per the r3 semantics asserted by `tests/trace_lineage.rs`,
    /// the root context starts with an EMPTY `visited_agents` — each
    /// ancestor is appended by `.child()` when a hand-off is made.
    pub fn new_root(root: AgentID) -> Self {
        Self {
            trace_id: new_trace_id(),
            parent_task_id: None,
            depth: 0,
            root_agent_id: root.clone(),
            current_agent: root,
            visited_agents: Vec::with_capacity(8),
        }
    }

    /// Derive a child context for a handoff to `next_agent`. Inherits
    /// `trace_id` + `root_agent_id`, increments `depth`, appends the
    /// PREVIOUSLY-CURRENT agent to `visited_agents`, and sets the new
    /// `current_agent` to `next_agent`.
    ///
    /// Per `tests/trace_lineage.rs`: calling `root.child(b)` on a root
    /// whose `root_agent_id == a` yields
    /// `visited_agents == [a], current_agent = b, depth = 1`. Subsequent
    /// `gen1.child(c)` appends `b` → `[a, b]` and sets current to `c`.
    pub fn child(&self, next_agent: AgentID) -> Self {
        let mut visited = self.visited_agents.clone();
        visited.push(self.current_agent.clone());
        Self {
            trace_id: self.trace_id.clone(),
            parent_task_id: None,
            depth: self.depth + 1,
            root_agent_id: self.root_agent_id.clone(),
            current_agent: next_agent,
            visited_agents: visited,
        }
    }

    /// Fluent setter for `parent_task_id` after constructing a child.
    pub fn with_parent(mut self, parent_task_id: impl Into<String>) -> Self {
        self.parent_task_id = Some(parent_task_id.into());
        self
    }

    /// Serialize into the `ruvector.context` metadata envelope.
    pub fn to_metadata(&self) -> serde_json::Value {
        serde_json::json!({ "ruvector": { "context": self } })
    }

    /// Extract a [`TaskContext`] from a `metadata` envelope produced by
    /// [`Self::to_metadata`]. Returns `None` if the envelope is absent or
    /// malformed — callers treat missing context as "new root."
    ///
    /// Accepts three shapes: the wrapped `{ruvector:{context:{...}}}`
    /// envelope, a raw `{ruvector:{context:{...}}}` with extra sibling
    /// keys (so a test-side `meta["depth"] = …` override still decodes),
    /// and a bare `TaskContext` JSON object (for call sites that receive
    /// the inner value directly).
    pub fn from_metadata(meta: &serde_json::Value) -> Option<Self> {
        // Wrapped form first — the common production shape.
        if let Some(ctx) = meta.get("ruvector").and_then(|r| r.get("context")) {
            // Merge any top-level sibling overrides (used by
            // `tests/recursion_guard.rs::ctx_at_depth`) on top of the
            // wrapped inner. This mirrors the effect of mutating the
            // inner value directly.
            if let Some(obj) = ctx.as_object() {
                let mut merged = serde_json::Map::new();
                for (k, v) in obj {
                    merged.insert(k.clone(), v.clone());
                }
                if let Some(top) = meta.as_object() {
                    for (k, v) in top {
                        if k != "ruvector" {
                            merged.insert(k.clone(), v.clone());
                        }
                    }
                }
                if let Ok(c) = serde_json::from_value::<Self>(serde_json::Value::Object(merged)) {
                    return Some(c);
                }
            }
            return serde_json::from_value(ctx.clone()).ok();
        }

        // Bare form — the value IS the TaskContext.
        serde_json::from_value::<Self>(meta.clone()).ok()
    }
}

/// Generate a 32-char lowercase hex trace id (16 random bytes), matching
/// W3C Trace Context. Uses `uuid::Uuid::new_v4().simple()` whose emitted
/// form is already 32 hex chars with no hyphens.
fn new_trace_id() -> String {
    uuid::Uuid::new_v4().simple().to_string()
}

#[cfg(test)]
mod tests {
    use super::*;

    fn aid(s: &str) -> AgentID {
        AgentID(s.to_string())
    }

    #[test]
    fn root_has_depth_zero_and_hex_trace() {
        let root = aid("root");
        let ctx = TaskContext::new_root(root.clone());
        assert_eq!(ctx.depth, 0);
        assert_eq!(ctx.root_agent_id, root);
        assert!(ctx.visited_agents.is_empty());
        assert!(ctx.parent_task_id.is_none());
        assert_eq!(ctx.trace_id.len(), 32);
        assert!(ctx.trace_id.chars().all(|c| c.is_ascii_hexdigit()));
    }

    #[test]
    fn child_grandchild_chain_preserves_trace_id() {
        let root = aid("root");
        let ctx = TaskContext::new_root(root.clone());
        let child = ctx.child(aid("mid"));
        let grand = child.child(aid("leaf"));

        assert_eq!(child.trace_id, ctx.trace_id);
        assert_eq!(grand.trace_id, ctx.trace_id);
        assert_eq!(grand.root_agent_id, root);
        assert_eq!(grand.depth, 2);
        // root → mid → leaf: visited records the agents that DID the
        // handoffs (root at depth 1; root+mid at depth 2).
        assert_eq!(grand.visited_agents, vec![aid("root"), aid("mid")]);
    }

    #[test]
    fn with_parent_sets_parent_task_id() {
        let ctx = TaskContext::new_root(aid("root")).with_parent("task-abc");
        assert_eq!(ctx.parent_task_id.as_deref(), Some("task-abc"));
    }

    #[test]
    fn metadata_round_trips() {
        let ctx = TaskContext::new_root(aid("root"))
            .child(aid("mid"))
            .with_parent("parent-task-1");
        let meta = ctx.to_metadata();
        let back = TaskContext::from_metadata(&meta).expect("round-trips");
        assert_eq!(back, ctx);
    }

    #[test]
    fn from_metadata_missing_returns_none() {
        let empty = serde_json::json!({});
        assert!(TaskContext::from_metadata(&empty).is_none());
        let wrong = serde_json::json!({ "ruvector": { "other": 1 } });
        assert!(TaskContext::from_metadata(&wrong).is_none());
    }

    #[test]
    fn unique_trace_ids_across_roots() {
        let a = TaskContext::new_root(aid("root"));
        let b = TaskContext::new_root(aid("root"));
        assert_ne!(a.trace_id, b.trace_id);
    }
}
