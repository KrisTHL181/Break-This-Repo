//! r3 — Recursion guard.
//!
//! Rejects unbounded chains (`A → B → C → … → Z`) via `max_call_depth` and
//! short cycles (`A → B → A`) via the `visited_agents` check. Enforced at
//! `tasks/send` on the receiving side, immediately after policy + budget
//! checks, before runner dispatch.
//!
//! See ADR-159 "r3 — Recursion guard".

use serde::{Deserialize, Serialize};
use thiserror::Error;

use crate::context::TaskContext;
use crate::identity::AgentID;

/// Bounds on call-graph shape enforced before dispatch.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct RecursionPolicy {
    /// Maximum chain depth from the root. Default: 8.
    pub max_call_depth: u32,

    /// If true, reject any task whose target [`AgentID`] is already in
    /// [`TaskContext::visited_agents`]. Default: true.
    pub deny_revisit: bool,

    /// Optional allowlist — agents explicitly named here are exempt from
    /// `deny_revisit`. Use for legitimate bounce-through patterns.
    #[serde(default)]
    pub revisit_allowlist: Vec<AgentID>,
}

impl Default for RecursionPolicy {
    fn default() -> Self {
        Self {
            max_call_depth: 8,
            deny_revisit: true,
            revisit_allowlist: Vec::new(),
        }
    }
}

#[derive(Debug, Error)]
pub enum RecursionError {
    #[error("recursion depth {depth} exceeds limit {limit}")]
    MaxDepthExceeded { depth: u32, limit: u32 },

    #[error("agent {agent:?} already visited in path {path:?}")]
    Revisit { agent: AgentID, path: Vec<AgentID> },
}

/// Check whether dispatching to `target` from the given context respects
/// the recursion policy. `ctx.depth` is the depth of the *incoming* task;
/// dispatching to `target` would land us at `ctx.depth + 1`, but the ADR
/// uses the pre-dispatch depth (>= limit) as the trigger.
#[tracing::instrument(skip(policy, ctx), level = "debug")]
pub fn check(
    policy: &RecursionPolicy,
    ctx: &TaskContext,
    target: AgentID,
) -> Result<(), RecursionError> {
    if ctx.depth > policy.max_call_depth {
        tracing::warn!(
            depth = ctx.depth,
            limit = policy.max_call_depth,
            "recursion depth exceeded"
        );
        return Err(RecursionError::MaxDepthExceeded {
            depth: ctx.depth,
            limit: policy.max_call_depth,
        });
    }

    if policy.deny_revisit
        && ctx.visited_agents.contains(&target)
        && !policy.revisit_allowlist.contains(&target)
    {
        tracing::warn!(
            agent = ?target,
            path = ?ctx.visited_agents,
            "revisit denied"
        );
        return Err(RecursionError::Revisit {
            agent: target,
            path: ctx.visited_agents.clone(),
        });
    }

    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    fn aid(s: &str) -> AgentID {
        AgentID(s.to_string())
    }

    fn mkctx(depth: u32, visited: &[&str]) -> TaskContext {
        let root = aid(visited.first().copied().unwrap_or("root"));
        TaskContext {
            trace_id: "0".repeat(32),
            parent_task_id: None,
            depth,
            root_agent_id: root.clone(),
            current_agent: root,
            visited_agents: visited.iter().map(|s| aid(s)).collect(),
        }
    }

    #[test]
    fn default_policy_is_depth_8_deny_revisit() {
        let p = RecursionPolicy::default();
        assert_eq!(p.max_call_depth, 8);
        assert!(p.deny_revisit);
        assert!(p.revisit_allowlist.is_empty());
    }

    #[test]
    fn depth_above_limit_rejects() {
        let p = RecursionPolicy::default();
        let ctx = mkctx(9, &["a", "b", "c"]);
        let err = check(&p, &ctx, aid("d")).unwrap_err();
        assert!(matches!(
            err,
            RecursionError::MaxDepthExceeded { depth: 9, limit: 8 }
        ));
    }

    #[test]
    fn depth_below_limit_ok() {
        let p = RecursionPolicy::default();
        let ctx = mkctx(7, &["a", "b", "c"]);
        assert!(check(&p, &ctx, aid("d")).is_ok());
    }

    #[test]
    fn revisit_rejected_by_default() {
        let p = RecursionPolicy::default();
        let ctx = mkctx(2, &["a", "b", "c"]);
        let err = check(&p, &ctx, aid("a")).unwrap_err();
        assert!(matches!(err, RecursionError::Revisit { .. }));
    }

    #[test]
    fn revisit_allowlist_bypasses_cycle_check() {
        let p = RecursionPolicy {
            max_call_depth: 8,
            deny_revisit: true,
            revisit_allowlist: vec![aid("a")],
        };
        let ctx = mkctx(2, &["a", "b", "c"]);
        assert!(check(&p, &ctx, aid("a")).is_ok());
    }

    #[test]
    fn deny_revisit_disabled_allows_cycle() {
        let p = RecursionPolicy {
            max_call_depth: 8,
            deny_revisit: false,
            revisit_allowlist: vec![],
        };
        let ctx = mkctx(2, &["a", "b", "c"]);
        assert!(check(&p, &ctx, aid("a")).is_ok());
    }

    #[test]
    fn unvisited_target_ok() {
        let p = RecursionPolicy::default();
        let ctx = mkctx(3, &["a", "b", "c"]);
        assert!(check(&p, &ctx, aid("z")).is_ok());
    }

    #[test]
    fn depth_check_runs_before_revisit_check() {
        // depth>limit AND target is an ancestor — depth error wins.
        let p = RecursionPolicy::default();
        let ctx = mkctx(9, &["a", "b", "c"]);
        let err = check(&p, &ctx, aid("a")).unwrap_err();
        assert!(matches!(err, RecursionError::MaxDepthExceeded { .. }));
    }
}
