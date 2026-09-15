//! Task policy + cost-control (ADR-159 r2 — Policy and cost control).
//!
//! An A2A endpoint with no policy is an arbitrary compute service. Every
//! task admitted through the server goes through a [`PolicyGuard`] which
//! enforces allowlist + concurrency at admission, budget checks before
//! dispatch, and wall-clock timeouts mid-run.

use std::{
    collections::HashMap,
    sync::{
        atomic::{AtomicU32, Ordering},
        Arc, Mutex,
    },
    time::Instant,
};

use serde::{Deserialize, Serialize};

use crate::identity::AgentID;
use crate::types::TaskSpec;

// ---------------------------------------------------------------------------
// Policy config.
// ---------------------------------------------------------------------------

#[derive(Clone, Debug, Default, Serialize, Deserialize, PartialEq)]
pub struct TaskPolicy {
    #[serde(default)]
    pub max_tokens: Option<u64>,
    #[serde(default)]
    pub max_cost_usd: Option<f64>,
    #[serde(default)]
    pub max_duration_ms: Option<u64>,
    /// Skill allow-list. `None` = all skills allowed.
    #[serde(default)]
    pub allowed_skills: Option<Vec<String>>,
    #[serde(default)]
    pub max_concurrency: Option<u32>,
}

impl TaskPolicy {
    /// Return `Ok(())` if `skill` is permitted by the allow-list.
    pub fn check_skill(&self, skill: &str) -> Result<(), PolicyError> {
        if let Some(allow) = &self.allowed_skills {
            if !allow.iter().any(|s| s == skill) {
                return Err(PolicyError::SkillDenied(skill.to_string()));
            }
        }
        Ok(())
    }

    /// Enforce pre-dispatch budget estimates against the policy. Either
    /// (or both) of `est_cost_usd` and `est_tokens` may be checked; `None`
    /// for a dimension skips that dimension's check.
    pub fn enforce_budget_pre(
        &self,
        est_cost_usd: f64,
        est_tokens: Option<u64>,
    ) -> Result<(), PolicyError> {
        if let Some(limit) = self.max_cost_usd {
            if est_cost_usd > limit {
                return Err(PolicyError::Exceeded {
                    field: "max_cost_usd",
                    est: est_cost_usd,
                    limit,
                });
            }
        }
        if let (Some(tokens), Some(limit)) = (est_tokens, self.max_tokens) {
            if tokens > limit {
                return Err(PolicyError::Exceeded {
                    field: "max_tokens",
                    est: tokens as f64,
                    limit: limit as f64,
                });
            }
        }
        Ok(())
    }
}

// ---------------------------------------------------------------------------
// Errors.
// ---------------------------------------------------------------------------

#[derive(Debug, thiserror::Error)]
pub enum PolicyError {
    #[error("skill {0} not in allow list")]
    SkillDenied(String),
    #[error("concurrency cap reached: {current}/{limit}")]
    ConcurrencyFull { current: u32, limit: u32 },
    #[error("estimate for {field} exceeds cap: {est} > {limit}")]
    Exceeded {
        field: &'static str,
        est: f64,
        limit: f64,
    },
    #[error("wall-clock {elapsed_ms}ms exceeds cap {limit_ms}ms")]
    MaxDurationExceeded { elapsed_ms: u64, limit_ms: u64 },
}

// ---------------------------------------------------------------------------
// PolicyGuard — admission control.
// ---------------------------------------------------------------------------

/// Per-endpoint guard tracking live concurrency against a shared
/// [`TaskPolicy`]. Cheap to clone.
#[derive(Clone, Debug)]
pub struct PolicyGuard {
    policy: TaskPolicy,
    /// Per-(caller, skill) bucket counters. A single shared counter would
    /// penalize a well-behaved caller for the noisy-neighbour's load; the
    /// per-bucket form is what ADR-159 r2 calls for.
    buckets: Arc<Mutex<HashMap<(AgentID, String), u32>>>,
    /// Fall-back global counter — used when a caller enters via the bare
    /// `enter(spec)` path that doesn't carry a caller id.
    active: Arc<AtomicU32>,
}

impl PolicyGuard {
    pub fn new(policy: TaskPolicy) -> Self {
        Self {
            policy,
            buckets: Arc::new(Mutex::new(HashMap::new())),
            active: Arc::new(AtomicU32::new(0)),
        }
    }

    pub fn policy(&self) -> &TaskPolicy {
        &self.policy
    }

    pub fn active_count(&self) -> u32 {
        self.active.load(Ordering::SeqCst)
    }

    /// Acquire a concurrency ticket for `(caller, skill)`. Each acquire
    /// increments the bucket and returns an RAII handle that decrements on
    /// drop. Exceeding `max_concurrency` returns
    /// [`PolicyError::ConcurrencyFull`].
    pub fn acquire(&self, caller: AgentID, skill: &str) -> Result<BucketTicket, PolicyError> {
        self.policy.check_skill(skill)?;
        let key = (caller, skill.to_string());
        let mut map = self.buckets.lock().expect("policy bucket mutex");
        let current = *map.get(&key).unwrap_or(&0);
        if let Some(limit) = self.policy.max_concurrency {
            if current >= limit {
                return Err(PolicyError::ConcurrencyFull { current, limit });
            }
        }
        map.insert(key.clone(), current + 1);
        Ok(BucketTicket {
            buckets: Arc::clone(&self.buckets),
            key,
        })
    }

    /// Validate a task at admission. On success returns a [`ConcurrencyTicket`]
    /// that auto-decrements the active count on drop. Used by the JSON-RPC
    /// dispatcher which doesn't know the caller id up-front.
    #[tracing::instrument(level = "debug", skip(self, spec))]
    pub fn enter(&self, spec: &TaskSpec) -> Result<ConcurrencyTicket<'_>, PolicyError> {
        self.policy.check_skill(&spec.skill)?;

        if let Some(limit) = self.policy.max_concurrency {
            let mut current = self.active.load(Ordering::SeqCst);
            loop {
                if current >= limit {
                    return Err(PolicyError::ConcurrencyFull { current, limit });
                }
                match self.active.compare_exchange(
                    current,
                    current + 1,
                    Ordering::SeqCst,
                    Ordering::SeqCst,
                ) {
                    Ok(_) => break,
                    Err(actual) => current = actual,
                }
            }
        } else {
            self.active.fetch_add(1, Ordering::SeqCst);
        }

        Ok(ConcurrencyTicket {
            active: Arc::clone(&self.active),
            _marker: std::marker::PhantomData,
        })
    }
}

/// RAII handle that decrements the guard's active count on drop.
pub struct ConcurrencyTicket<'a> {
    active: Arc<AtomicU32>,
    _marker: std::marker::PhantomData<&'a PolicyGuard>,
}

impl Drop for ConcurrencyTicket<'_> {
    fn drop(&mut self) {
        self.active.fetch_sub(1, Ordering::SeqCst);
    }
}

/// RAII handle for `PolicyGuard::acquire` — decrements the per-bucket
/// counter on drop.
#[derive(Debug)]
pub struct BucketTicket {
    buckets: Arc<Mutex<HashMap<(AgentID, String), u32>>>,
    key: (AgentID, String),
}

impl Drop for BucketTicket {
    fn drop(&mut self) {
        if let Ok(mut map) = self.buckets.lock() {
            if let Some(cnt) = map.get_mut(&self.key) {
                *cnt = cnt.saturating_sub(1);
                if *cnt == 0 {
                    map.remove(&self.key);
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Mid-task duration check.
// ---------------------------------------------------------------------------

/// Mid-task wall-clock check. The runner is expected to poll this
/// periodically (e.g. between token batches) and transition to `Failed`
/// with reason `PolicyTimeout` if it returns an error.
pub fn enforce_duration(policy: &TaskPolicy, started: Instant) -> Result<(), PolicyError> {
    if let Some(limit_ms) = policy.max_duration_ms {
        let elapsed_ms = started.elapsed().as_millis() as u64;
        if elapsed_ms > limit_ms {
            return Err(PolicyError::MaxDurationExceeded {
                elapsed_ms,
                limit_ms,
            });
        }
    }
    Ok(())
}

// ---------------------------------------------------------------------------
// Tests.
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;

    fn test_agent_id() -> AgentID {
        AgentID("a".repeat(64))
    }

    fn spec_for(skill: &str) -> TaskSpec {
        use crate::context::TaskContext;
        use crate::types::{Message, Part, Role};
        TaskSpec {
            id: "t".into(),
            skill: skill.into(),
            message: Message {
                role: Role::User,
                parts: vec![Part::Text { text: "hi".into() }],
                metadata: serde_json::Value::Null,
            },
            policy: None,
            context: TaskContext::new_root(test_agent_id()),
            metadata: serde_json::Value::Null,
        }
    }

    #[test]
    fn skill_allow_list_enforced() {
        let guard = PolicyGuard::new(TaskPolicy {
            allowed_skills: Some(vec!["echo".into()]),
            ..Default::default()
        });
        let _ok = guard.enter(&spec_for("echo")).expect("allowed");
        assert!(matches!(
            guard.enter(&spec_for("summarize")),
            Err(PolicyError::SkillDenied(_))
        ));
    }

    #[test]
    fn empty_allow_list_permits_all() {
        let guard = PolicyGuard::new(TaskPolicy::default());
        assert!(guard.enter(&spec_for("anything")).is_ok());
    }

    #[test]
    fn acquire_enforces_per_bucket_cap() {
        let guard = PolicyGuard::new(TaskPolicy {
            max_concurrency: Some(2),
            ..Default::default()
        });
        let caller = test_agent_id();
        let t1 = guard.acquire(caller.clone(), "x").expect("1st");
        let t2 = guard.acquire(caller.clone(), "x").expect("2nd");
        assert!(matches!(
            guard.acquire(caller.clone(), "x"),
            Err(PolicyError::ConcurrencyFull { .. })
        ));
        drop(t1);
        let _t3 = guard.acquire(caller, "x").expect("slot reopened");
        drop(t2);
    }

    #[test]
    fn budget_pre_check_tokens_and_cost() {
        let policy = TaskPolicy {
            max_tokens: Some(100),
            max_cost_usd: Some(0.50),
            ..Default::default()
        };
        assert!(policy.enforce_budget_pre(0.25, Some(50)).is_ok());
        assert!(matches!(
            policy.enforce_budget_pre(0.25, Some(200)),
            Err(PolicyError::Exceeded {
                field: "max_tokens",
                ..
            })
        ));
        assert!(matches!(
            policy.enforce_budget_pre(1.00, Some(50)),
            Err(PolicyError::Exceeded {
                field: "max_cost_usd",
                ..
            })
        ));
    }
}
