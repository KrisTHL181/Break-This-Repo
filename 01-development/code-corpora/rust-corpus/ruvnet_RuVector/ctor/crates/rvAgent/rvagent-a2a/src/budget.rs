//! r3 — Global budget control.
//!
//! Enforces a system-wide spend ceiling that per-task [`TaskPolicy`] cannot
//! enforce: a rolling 60-second window over combined USD cost, LLM tokens,
//! and task dispatch count. Runs at the dispatch queue, *before* peer
//! selection, so the router only sees tasks that already fit the budget.
//!
//! See ADR-159 "r3 — Global budget control".
//!
//! [`TaskPolicy`]: crate::policy::TaskPolicy

use std::collections::VecDeque;
use std::time::{Duration, Instant};

use parking_lot::Mutex;
use serde::{Deserialize, Serialize};
use thiserror::Error;

/// Width of the rolling rate-limit window.
const WINDOW: Duration = Duration::from_secs(60);

/// Minimum gap between full eviction sweeps. Keeps the hot path O(1) under
/// bursty load — the ledger under-counts by up to this many ms of stale
/// spend, which is fail-closed (conservative) for budget enforcement.
const EVICT_THROTTLE: Duration = Duration::from_millis(100);

/// Global per-minute caps on aggregate task spend. Every field is optional;
/// a field set to `None` disables the corresponding dimension.
#[derive(Clone, Debug, Default, Serialize, Deserialize)]
pub struct GlobalBudget {
    /// Hard cap on combined task cost per 60-second window.
    pub max_usd_per_minute: Option<f64>,

    /// Hard cap on LLM tokens consumed per 60-second window.
    pub max_tokens_per_minute: Option<u64>,

    /// Hard cap on task dispatches per 60-second window.
    pub max_tasks_per_minute: Option<u32>,

    /// Overflow behaviour when the window saturates.
    #[serde(default)]
    pub overflow: OverflowPolicy,
}

/// What to do when the rolling window is full and a new task arrives.
///
/// Serialized on the wire in one of two forms, both accepted at load-time:
///   - bare string: `"shed"` or `"queue"` (the latter defaults the queue
///     depth to 1000);
///   - map: `{ kind = "queue", max_queue_depth = N }`.
#[derive(Clone, Debug, Default, Serialize, PartialEq)]
#[serde(rename_all = "snake_case", tag = "kind")]
pub enum OverflowPolicy {
    /// Reject the task immediately with [`BudgetError::Exceeded`].
    #[default]
    Shed,
    /// Queue up to `max_queue_depth` tasks, failing with
    /// [`BudgetError::QueueFull`] only when the queue saturates.
    Queue { max_queue_depth: u32 },
}

/// Custom deserializer: accept either a bare-string form (`"shed"` /
/// `"queue"`) or the tagged-map form (`{ kind = "queue", max_queue_depth =
/// N }`) so the ADR-159 r3 example TOML loads without surprises.
impl<'de> Deserialize<'de> for OverflowPolicy {
    fn deserialize<D: serde::de::Deserializer<'de>>(d: D) -> Result<Self, D::Error> {
        #[derive(Deserialize)]
        #[serde(untagged)]
        enum Repr {
            Str(String),
            Map {
                kind: String,
                #[serde(default)]
                max_queue_depth: Option<u32>,
            },
        }
        match Repr::deserialize(d)? {
            Repr::Str(s) => match s.as_str() {
                "shed" => Ok(OverflowPolicy::Shed),
                "queue" => Ok(OverflowPolicy::Queue {
                    max_queue_depth: 1000,
                }),
                other => Err(serde::de::Error::custom(format!(
                    "unknown OverflowPolicy kind {other:?}"
                ))),
            },
            Repr::Map {
                kind,
                max_queue_depth,
            } => match kind.as_str() {
                "shed" => Ok(OverflowPolicy::Shed),
                "queue" => Ok(OverflowPolicy::Queue {
                    max_queue_depth: max_queue_depth.unwrap_or(1000),
                }),
                other => Err(serde::de::Error::custom(format!(
                    "unknown OverflowPolicy kind {other:?}"
                ))),
            },
        }
    }
}

impl OverflowPolicy {
    /// Convenience: parse from a loosely-typed string like `"shed"`.
    pub fn from_str_loose(s: &str) -> Option<Self> {
        match s {
            "shed" => Some(OverflowPolicy::Shed),
            "queue" => Some(OverflowPolicy::Queue {
                max_queue_depth: 1000,
            }),
            _ => None,
        }
    }
}

#[derive(Debug, Error)]
pub enum BudgetError {
    #[error("budget exceeded on dim {dim}: current {current} > limit {limit}")]
    Exceeded {
        dim: &'static str,
        limit: f64,
        current: f64,
    },
    #[error("queue full at depth {depth}")]
    QueueFull { depth: u32 },
}

/// Snapshot of per-dimension current-vs-limit observations.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct BudgetSnapshot {
    pub usd_current: f64,
    pub usd_limit: Option<f64>,
    pub tokens_current: u64,
    pub tokens_limit: Option<u64>,
    pub tasks_current: u32,
    pub tasks_limit: Option<u32>,
    pub queue_depth: u32,
}

#[derive(Debug)]
struct Entry {
    at: Instant,
    cost_usd: f64,
    tokens: u64,
}

#[derive(Debug)]
struct LedgerState {
    window: VecDeque<Entry>,
    queue_depth: u32,
    last_evict: Option<Instant>,
}

/// Rolling 60-second ledger. Thread-safe via internal mutex — the rate of
/// `try_consume` calls is bounded by dispatch rate, so contention is low.
pub struct BudgetLedger {
    budget: GlobalBudget,
    state: Mutex<LedgerState>,
}

impl BudgetLedger {
    pub fn new(budget: GlobalBudget) -> Self {
        Self {
            budget,
            state: Mutex::new(LedgerState {
                window: VecDeque::with_capacity(256),
                queue_depth: 0,
                last_evict: None,
            }),
        }
    }

    /// Attempt to consume `cost_usd` + `tokens` (and one task slot). Returns
    /// `Ok(())` if admitted, or a [`BudgetError`] if the window is full and
    /// the overflow policy rejects the task.
    ///
    /// Hot-path optimization: we evict at most once per `EVICT_THROTTLE`
    /// (100 ms). Under steady load the amortized cost of `try_consume` is
    /// O(1); at rest the next call pays for a full sweep. The worst-case
    /// staleness (≤100 ms of "extra" unevicted spend) is fail-closed, so
    /// the error is always conservative — we never admit a task the
    /// policy would have rejected had we evicted aggressively.
    #[tracing::instrument(skip(self), level = "debug")]
    pub fn try_consume(&self, cost_usd: f64, tokens: u64) -> Result<(), BudgetError> {
        // Fast path: no caps configured → admit without touching the window.
        // Otherwise every call would push an Entry into an ever-growing
        // VecDeque with no eviction trigger, turning a 75 ns hot path into
        // a 95 µs linear-scan one (see benches/budget_ledger.rs).
        if self.budget.max_usd_per_minute.is_none()
            && self.budget.max_tokens_per_minute.is_none()
            && self.budget.max_tasks_per_minute.is_none()
        {
            return Ok(());
        }

        let now = Instant::now();
        let mut st = self.state.lock();

        let due_for_evict = st
            .last_evict
            .map(|t| now.duration_since(t) >= EVICT_THROTTLE)
            .unwrap_or(true);
        if due_for_evict {
            while let Some(front) = st.window.front() {
                if now.duration_since(front.at) >= WINDOW {
                    st.window.pop_front();
                } else {
                    break;
                }
            }
            st.last_evict = Some(now);
        }

        let (sum_usd, sum_tokens, count) = st
            .window
            .iter()
            .fold((0.0_f64, 0_u64, 0_u32), |(u, t, c), e| {
                (u + e.cost_usd, t + e.tokens, c + 1)
            });

        // Check each dim with the hypothetical new entry added.
        let new_usd = sum_usd + cost_usd;
        let new_tokens = sum_tokens + tokens;
        let new_count = count + 1;

        if let Some(cap) = self.budget.max_usd_per_minute {
            // Absolute-tolerance compare — raw f64 accumulation in the
            // window hot-path otherwise rejects exact-fill sequences like
            // `0.01 * 100 == 1.00000000000007 > 1.0` that should admit.
            const USD_EPS: f64 = 1e-9;
            if new_usd > cap + USD_EPS {
                return reject(
                    &mut st,
                    &self.budget.overflow,
                    BudgetError::Exceeded {
                        dim: "usd",
                        limit: cap,
                        current: new_usd,
                    },
                );
            }
        }
        if let Some(cap) = self.budget.max_tokens_per_minute {
            if new_tokens > cap {
                return reject(
                    &mut st,
                    &self.budget.overflow,
                    BudgetError::Exceeded {
                        dim: "tokens",
                        limit: cap as f64,
                        current: new_tokens as f64,
                    },
                );
            }
        }
        if let Some(cap) = self.budget.max_tasks_per_minute {
            if new_count > cap {
                return reject(
                    &mut st,
                    &self.budget.overflow,
                    BudgetError::Exceeded {
                        dim: "tasks",
                        limit: cap as f64,
                        current: new_count as f64,
                    },
                );
            }
        }

        st.window.push_back(Entry {
            at: now,
            cost_usd,
            tokens,
        });
        Ok(())
    }

    /// Snapshot of per-dim current-vs-limit observations.
    pub fn snapshot(&self) -> BudgetSnapshot {
        let now = Instant::now();
        let mut st = self.state.lock();
        // Always evict on snapshot — observation cost is not on the hot path.
        while let Some(front) = st.window.front() {
            if now.duration_since(front.at) >= WINDOW {
                st.window.pop_front();
            } else {
                break;
            }
        }
        st.last_evict = Some(now);
        let (u, t, c) = st.window.iter().fold((0.0, 0u64, 0u32), |acc, e| {
            (acc.0 + e.cost_usd, acc.1 + e.tokens, acc.2 + 1)
        });
        BudgetSnapshot {
            usd_current: u,
            usd_limit: self.budget.max_usd_per_minute,
            tokens_current: t,
            tokens_limit: self.budget.max_tokens_per_minute,
            tasks_current: c,
            tasks_limit: self.budget.max_tasks_per_minute,
            queue_depth: st.queue_depth,
        }
    }
}

fn reject(
    st: &mut LedgerState,
    overflow: &OverflowPolicy,
    err: BudgetError,
) -> Result<(), BudgetError> {
    match overflow {
        OverflowPolicy::Shed => {
            tracing::warn!(error = ?err, "budget rejected (shed)");
            Err(err)
        }
        OverflowPolicy::Queue { max_queue_depth } => {
            if st.queue_depth < *max_queue_depth {
                st.queue_depth += 1;
                tracing::warn!(
                    depth = st.queue_depth,
                    "budget saturated, enqueued for next window"
                );
                Err(err)
            } else {
                tracing::warn!(depth = st.queue_depth, "budget queue full");
                Err(BudgetError::QueueFull {
                    depth: st.queue_depth,
                })
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn admits_under_limit() {
        let b = GlobalBudget {
            max_usd_per_minute: Some(10.0),
            max_tokens_per_minute: None,
            max_tasks_per_minute: None,
            overflow: OverflowPolicy::Shed,
        };
        let l = BudgetLedger::new(b);
        for _ in 0..5 {
            assert!(l.try_consume(1.0, 0).is_ok());
        }
    }

    #[test]
    fn rejects_when_over_limit_shed() {
        let b = GlobalBudget {
            max_usd_per_minute: Some(5.0),
            max_tokens_per_minute: None,
            max_tasks_per_minute: None,
            overflow: OverflowPolicy::Shed,
        };
        let l = BudgetLedger::new(b);
        for _ in 0..5 {
            assert!(l.try_consume(1.0, 0).is_ok());
        }
        // 6th pushes us over.
        let err = l.try_consume(1.0, 0).unwrap_err();
        assert!(matches!(err, BudgetError::Exceeded { dim: "usd", .. }));
    }

    #[test]
    fn token_limit_enforced() {
        let b = GlobalBudget {
            max_usd_per_minute: None,
            max_tokens_per_minute: Some(100),
            max_tasks_per_minute: None,
            overflow: OverflowPolicy::Shed,
        };
        let l = BudgetLedger::new(b);
        assert!(l.try_consume(0.0, 100).is_ok());
        let err = l.try_consume(0.0, 1).unwrap_err();
        assert!(matches!(err, BudgetError::Exceeded { dim: "tokens", .. }));
    }

    #[test]
    fn tasks_limit_enforced() {
        let b = GlobalBudget {
            max_usd_per_minute: None,
            max_tokens_per_minute: None,
            max_tasks_per_minute: Some(3),
            overflow: OverflowPolicy::Shed,
        };
        let l = BudgetLedger::new(b);
        assert!(l.try_consume(0.0, 0).is_ok());
        assert!(l.try_consume(0.0, 0).is_ok());
        assert!(l.try_consume(0.0, 0).is_ok());
        let err = l.try_consume(0.0, 0).unwrap_err();
        assert!(matches!(err, BudgetError::Exceeded { dim: "tasks", .. }));
    }

    #[test]
    fn burst_of_100_hits_limit() {
        let b = GlobalBudget {
            max_usd_per_minute: Some(10.0),
            max_tokens_per_minute: None,
            max_tasks_per_minute: None,
            overflow: OverflowPolicy::Shed,
        };
        let l = BudgetLedger::new(b);
        let mut ok = 0;
        let mut rejected = 0;
        for _ in 0..100 {
            if l.try_consume(0.20, 0).is_ok() {
                ok += 1;
            } else {
                rejected += 1;
            }
        }
        assert_eq!(ok, 50);
        assert_eq!(rejected, 50);
    }

    #[test]
    fn window_clears_after_eviction() {
        // We can't wait 60s in a test, so stub the window by inserting
        // pre-aged entries directly — exercises the eviction loop.
        let b = GlobalBudget {
            max_usd_per_minute: Some(5.0),
            ..Default::default()
        };
        let l = BudgetLedger::new(b);
        {
            let mut st = l.state.lock();
            st.last_evict = None;
            let long_ago = Instant::now() - Duration::from_secs(90);
            for _ in 0..10 {
                st.window.push_back(Entry {
                    at: long_ago,
                    cost_usd: 1.0,
                    tokens: 0,
                });
            }
        }
        // Next consume must evict all 10 stale entries first, then admit.
        assert!(l.try_consume(1.0, 0).is_ok());
        let snap = l.snapshot();
        assert_eq!(snap.usd_current as u64, 1);
    }

    #[test]
    fn queue_policy_enqueues_up_to_depth() {
        let b = GlobalBudget {
            max_usd_per_minute: Some(1.0),
            max_tokens_per_minute: None,
            max_tasks_per_minute: None,
            overflow: OverflowPolicy::Queue { max_queue_depth: 3 },
        };
        let l = BudgetLedger::new(b);
        assert!(l.try_consume(1.0, 0).is_ok());
        // Next three attempts enqueue.
        for _ in 0..3 {
            let err = l.try_consume(1.0, 0).unwrap_err();
            assert!(matches!(err, BudgetError::Exceeded { .. }));
        }
        // 5th attempt: queue full.
        let err = l.try_consume(1.0, 0).unwrap_err();
        assert!(matches!(err, BudgetError::QueueFull { depth: 3 }));
    }

    #[test]
    fn snapshot_reports_state() {
        let b = GlobalBudget {
            max_usd_per_minute: Some(5.0),
            max_tokens_per_minute: Some(500),
            max_tasks_per_minute: Some(10),
            overflow: OverflowPolicy::Shed,
        };
        let l = BudgetLedger::new(b);
        let _ = l.try_consume(1.5, 50);
        let _ = l.try_consume(0.5, 10);
        let s = l.snapshot();
        assert_eq!(s.tasks_current, 2);
        assert!((s.usd_current - 2.0).abs() < 1e-9);
        assert_eq!(s.tokens_current, 60);
        assert_eq!(s.usd_limit, Some(5.0));
    }
}
