//! Bounded review→repair automation (#3832).
//!
//! ## What this is not
//!
//! It is **not a fourth Mode.** Codewhale has exactly three Modes — Plan, Act,
//! Operate — and exactly three permission postures — Ask, Auto-Review, Full
//! Access. A review→repair loop is a *Workflow shape*: an ordering of existing
//! roles over existing gates. It therefore lives here, beside [`crate::gates`],
//! and carries no mode, no posture, and no permission of its own. Whatever Mode
//! and posture the session already has continue to govern every step; this type
//! only decides *when to stop*.
//!
//! ## The four things that make it safe
//!
//! 1. **Explicit ceilings.** [`ReviewRepairBounds`] caps iterations, wall-clock
//!    seconds, and tool calls. Every ceiling is checked *before* work starts, so
//!    the loop cannot overrun by one extra iteration; a zero ceiling means the
//!    loop never runs rather than running forever.
//! 2. **Exact routes on the record.** Each iteration must carry a
//!    [`RouteReceipt`] for the reviewer and, where the policy requires one, the
//!    verifier: role, exact provider/model, requested→effective reasoning, and
//!    who routed it. An iteration with no reviewer route is refused.
//! 3. **Human ratification where policy says.** When
//!    [`ReviewRepairPolicy::require_human_ratification`] is set, a clean verify
//!    does not finish the loop: it parks at
//!    [`StopReason::AwaitingRatification`] until [`ReviewRepairLoop::ratify`]
//!    records an explicit human decision.
//! 4. **Fail closed on stale input.** The loop pins the digest of the artifact
//!    under review. If an iteration reports a different digest — the branch
//!    moved, the diff was rebuilt, the findings came from an older tree — the
//!    loop halts at [`StopReason::StaleInput`] instead of repairing against
//!    something the reviewer never saw.

use serde::{Deserialize, Serialize};

/// Explicit ceilings on a review→repair loop. There is no unbounded variant.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub struct ReviewRepairBounds {
    /// Maximum review→repair iterations. `0` means the loop never starts.
    pub max_iterations: u32,
    /// Maximum cumulative wall-clock seconds across all iterations.
    pub max_wall_clock_secs: u64,
    /// Maximum cumulative tool calls across all iterations.
    pub max_tool_calls: u32,
}

impl ReviewRepairBounds {
    /// Conservative defaults for an unattended loop: short, cheap, and easy to
    /// re-run by hand if it stops early.
    #[must_use]
    pub fn conservative() -> Self {
        Self {
            max_iterations: 3,
            max_wall_clock_secs: 900,
            max_tool_calls: 120,
        }
    }
}

impl Default for ReviewRepairBounds {
    fn default() -> Self {
        Self::conservative()
    }
}

/// Who chose the route for a step. Distinguishing these is the difference
/// between "the user picked this model" and "a Router picked it".
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum RoutedBy {
    /// The exact Fleet member route, frozen at Fleet capture.
    Fleet,
    /// A Router chose reasoning for the frozen member route. The Router's own
    /// exact identity is recorded so its spend is attributable.
    Router { provider: String, model: String },
    /// The session's current model selection.
    SessionDefault,
}

impl RoutedBy {
    /// Human-readable source label for receipts and Workflow rows.
    #[must_use]
    pub fn label(&self) -> String {
        match self {
            Self::Fleet => "fleet".to_string(),
            Self::Router { provider, model } => format!("router {provider}/{model}"),
            Self::SessionDefault => "session default".to_string(),
        }
    }
}

/// The exact route a reviewer or verifier ran on.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct RouteReceipt {
    /// Fleet role (`reviewer`, `verifier`, `implementer`, …).
    pub role: String,
    pub provider: String,
    pub model: String,
    /// What the plan asked for.
    pub requested_reasoning: String,
    /// What the provider actually applied. Divergence is shown, not smoothed.
    pub effective_reasoning: String,
    pub routed_by: RoutedBy,
}

impl RouteReceipt {
    /// True when every identity field is present. An incomplete route is not a
    /// receipt; it is a claim.
    #[must_use]
    pub fn is_complete(&self) -> bool {
        !self.role.trim().is_empty()
            && !self.provider.trim().is_empty()
            && !self.model.trim().is_empty()
            && !self.requested_reasoning.trim().is_empty()
            && !self.effective_reasoning.trim().is_empty()
    }

    /// One-line display: role, exact route, requested→effective, routed by.
    #[must_use]
    pub fn display_line(&self) -> String {
        let reasoning = if self.requested_reasoning == self.effective_reasoning {
            self.effective_reasoning.clone()
        } else {
            format!("{}→{}", self.requested_reasoning, self.effective_reasoning)
        };
        format!(
            "{}: {}/{} reasoning {reasoning} (routed by {})",
            self.role,
            self.provider,
            self.model,
            self.routed_by.label()
        )
    }
}

/// The verdict a reviewer/verifier pair produced for one iteration.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum IterationVerdict {
    /// Review and verification both passed. The loop may finish.
    Clean,
    /// Findings remain; another repair iteration is warranted.
    RepairsNeeded,
    /// The reviewer refused to judge (missing input, tool failure, …). The loop
    /// stops rather than treating an absent judgment as a pass.
    Inconclusive,
}

/// What one iteration actually did.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct IterationReceipt {
    /// 1-based iteration number, assigned by the loop.
    pub iteration: u32,
    /// Digest of the artifact this iteration reviewed.
    pub input_digest: String,
    pub reviewer: RouteReceipt,
    /// Present when the policy requires a separate verify step.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub verifier: Option<RouteReceipt>,
    pub verdict: IterationVerdict,
    /// Bounded finding summaries. Not the findings themselves.
    #[serde(default)]
    pub finding_summaries: Vec<String>,
    pub tool_calls_used: u32,
    pub elapsed_secs: u64,
}

/// Why a review→repair loop stopped. Every variant is terminal.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum StopReason {
    /// Clean verdict, and either no ratification was required or it was given.
    Verified,
    /// A human explicitly rejected the result.
    RatificationDeclined {
        note: String,
    },
    /// Clean verdict, waiting on the human the policy requires.
    AwaitingRatification,
    IterationCeiling {
        max_iterations: u32,
    },
    TimeCeiling {
        max_wall_clock_secs: u64,
        elapsed_secs: u64,
    },
    ToolCeiling {
        max_tool_calls: u32,
        used: u32,
    },
    /// The artifact under review changed underneath the loop.
    StaleInput {
        pinned: String,
        reported: String,
    },
    /// The reviewer could not judge, or its receipt was incomplete.
    Inconclusive {
        reason: String,
    },
}

impl StopReason {
    /// True only for the one outcome that means "this loop succeeded".
    #[must_use]
    pub fn is_success(&self) -> bool {
        matches!(self, Self::Verified)
    }

    /// True when the loop stopped because it hit an explicit ceiling. These are
    /// honest incompletions, not failures.
    #[must_use]
    pub fn is_ceiling(&self) -> bool {
        matches!(
            self,
            Self::IterationCeiling { .. } | Self::TimeCeiling { .. } | Self::ToolCeiling { .. }
        )
    }

    /// Stable receipt line. UI surfaces localize around it; the numbers here are
    /// the ones the user must be able to check.
    #[must_use]
    pub fn receipt(&self) -> String {
        match self {
            Self::Verified => "verified".to_string(),
            Self::RatificationDeclined { note } => format!("ratification declined: {note}"),
            Self::AwaitingRatification => {
                "clean; awaiting human ratification before this counts as done".to_string()
            }
            Self::IterationCeiling { max_iterations } => {
                format!("stopped at the iteration ceiling ({max_iterations})")
            }
            Self::TimeCeiling {
                max_wall_clock_secs,
                elapsed_secs,
            } => format!("stopped at the time ceiling ({elapsed_secs}s of {max_wall_clock_secs}s)"),
            Self::ToolCeiling {
                max_tool_calls,
                used,
            } => format!("stopped at the tool ceiling ({used} of {max_tool_calls} calls)"),
            Self::StaleInput { pinned, reported } => format!(
                "stopped: input changed under review (pinned {pinned}, reported {reported})"
            ),
            Self::Inconclusive { reason } => format!("stopped: no usable verdict ({reason})"),
        }
    }
}

/// Policy knobs that are not ceilings.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct ReviewRepairPolicy {
    /// A clean verdict parks at [`StopReason::AwaitingRatification`] until a
    /// human decides.
    #[serde(default)]
    pub require_human_ratification: bool,
    /// Every iteration must carry a verifier receipt in addition to the
    /// reviewer's. A missing verifier is inconclusive, never a pass.
    #[serde(default)]
    pub require_verifier_receipt: bool,
}

impl Default for ReviewRepairPolicy {
    fn default() -> Self {
        // Fail closed by default: a human confirms, and review alone is not
        // verification.
        Self {
            require_human_ratification: true,
            require_verifier_receipt: true,
        }
    }
}

/// A bounded review→repair loop over one lane's artifact.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct ReviewRepairLoop {
    pub lane_id: String,
    pub bounds: ReviewRepairBounds,
    pub policy: ReviewRepairPolicy,
    /// Digest of the artifact this loop was authorized to work on.
    pub pinned_input_digest: String,
    #[serde(default)]
    pub iterations: Vec<IterationReceipt>,
    #[serde(default)]
    pub elapsed_secs: u64,
    #[serde(default)]
    pub tool_calls_used: u32,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub stopped: Option<StopReason>,
}

/// Refusal from [`ReviewRepairLoop::begin_iteration`] or
/// [`ReviewRepairLoop::record_iteration`].
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ReviewRepairError {
    /// The loop already stopped; it does not restart.
    AlreadyStopped(StopReason),
    /// A ceiling or fail-closed condition ended the loop on this call.
    Stopped(StopReason),
    /// The receipt itself was unusable.
    IncompleteReceipt(String),
}

impl ReviewRepairError {
    /// The terminal reason, when this refusal carries one.
    #[must_use]
    pub fn stop_reason(&self) -> Option<&StopReason> {
        match self {
            Self::AlreadyStopped(reason) | Self::Stopped(reason) => Some(reason),
            Self::IncompleteReceipt(_) => None,
        }
    }
}

impl std::fmt::Display for ReviewRepairError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::AlreadyStopped(reason) => {
                write!(f, "review-repair already stopped: {}", reason.receipt())
            }
            Self::Stopped(reason) => write!(f, "{}", reason.receipt()),
            Self::IncompleteReceipt(detail) => {
                write!(f, "review-repair receipt is unusable: {detail}")
            }
        }
    }
}

impl std::error::Error for ReviewRepairError {}

impl ReviewRepairLoop {
    /// Start a loop pinned to the digest of the artifact under review.
    #[must_use]
    pub fn new(
        lane_id: impl Into<String>,
        pinned_input_digest: impl Into<String>,
        bounds: ReviewRepairBounds,
        policy: ReviewRepairPolicy,
    ) -> Self {
        Self {
            lane_id: lane_id.into(),
            bounds,
            policy,
            pinned_input_digest: pinned_input_digest.into(),
            iterations: Vec::new(),
            elapsed_secs: 0,
            tool_calls_used: 0,
            stopped: None,
        }
    }

    /// Whether the loop has stopped, and why.
    #[must_use]
    pub fn stop_reason(&self) -> Option<&StopReason> {
        self.stopped.as_ref()
    }

    /// Claim the next iteration slot, or refuse.
    ///
    /// Ceilings are checked here — *before* any model or tool spend — so a loop
    /// that has exhausted its budget cannot buy one more iteration to find out.
    pub fn begin_iteration(&mut self) -> Result<u32, ReviewRepairError> {
        if let Some(reason) = self.stopped.clone() {
            return Err(ReviewRepairError::AlreadyStopped(reason));
        }
        let next = self.iterations.len() as u32 + 1;
        if next > self.bounds.max_iterations {
            return Err(self.stop(StopReason::IterationCeiling {
                max_iterations: self.bounds.max_iterations,
            }));
        }
        if self.elapsed_secs >= self.bounds.max_wall_clock_secs {
            return Err(self.stop(StopReason::TimeCeiling {
                max_wall_clock_secs: self.bounds.max_wall_clock_secs,
                elapsed_secs: self.elapsed_secs,
            }));
        }
        if self.tool_calls_used >= self.bounds.max_tool_calls {
            return Err(self.stop(StopReason::ToolCeiling {
                max_tool_calls: self.bounds.max_tool_calls,
                used: self.tool_calls_used,
            }));
        }
        Ok(next)
    }

    /// Record a completed iteration and decide whether the loop continues.
    ///
    /// Returns `Ok(None)` when another iteration is warranted, `Ok(Some(reason))`
    /// when the loop is done.
    pub fn record_iteration(
        &mut self,
        receipt: IterationReceipt,
    ) -> Result<Option<StopReason>, ReviewRepairError> {
        if let Some(reason) = self.stopped.clone() {
            return Err(ReviewRepairError::AlreadyStopped(reason));
        }

        // Fail closed on stale input before anything in the receipt is trusted:
        // a verdict about a different tree is not a verdict about this one.
        if receipt.input_digest != self.pinned_input_digest {
            return Err(self.stop(StopReason::StaleInput {
                pinned: self.pinned_input_digest.clone(),
                reported: receipt.input_digest.clone(),
            }));
        }
        if !receipt.reviewer.is_complete() {
            return Err(ReviewRepairError::IncompleteReceipt(format!(
                "reviewer route for iteration {} is missing identity fields",
                receipt.iteration
            )));
        }
        if self.policy.require_verifier_receipt {
            match receipt.verifier.as_ref() {
                None => {
                    return Err(self.stop(StopReason::Inconclusive {
                        reason: "policy requires a verifier receipt and none was produced"
                            .to_string(),
                    }));
                }
                Some(verifier) if !verifier.is_complete() => {
                    return Err(ReviewRepairError::IncompleteReceipt(format!(
                        "verifier route for iteration {} is missing identity fields",
                        receipt.iteration
                    )));
                }
                Some(_) => {}
            }
        }

        self.elapsed_secs = self.elapsed_secs.saturating_add(receipt.elapsed_secs);
        self.tool_calls_used = self.tool_calls_used.saturating_add(receipt.tool_calls_used);
        let verdict = receipt.verdict;
        self.iterations.push(receipt);

        // Overrun is reported honestly: the ceiling is the reason we stop, even
        // though this iteration's spend already happened.
        if self.tool_calls_used > self.bounds.max_tool_calls {
            let reason = StopReason::ToolCeiling {
                max_tool_calls: self.bounds.max_tool_calls,
                used: self.tool_calls_used,
            };
            self.stopped = Some(reason.clone());
            return Ok(Some(reason));
        }
        if self.elapsed_secs > self.bounds.max_wall_clock_secs {
            let reason = StopReason::TimeCeiling {
                max_wall_clock_secs: self.bounds.max_wall_clock_secs,
                elapsed_secs: self.elapsed_secs,
            };
            self.stopped = Some(reason.clone());
            return Ok(Some(reason));
        }

        match verdict {
            IterationVerdict::Inconclusive => {
                let reason = StopReason::Inconclusive {
                    reason: "reviewer returned no usable verdict".to_string(),
                };
                self.stopped = Some(reason.clone());
                Ok(Some(reason))
            }
            IterationVerdict::Clean => {
                let reason = if self.policy.require_human_ratification {
                    StopReason::AwaitingRatification
                } else {
                    StopReason::Verified
                };
                self.stopped = Some(reason.clone());
                Ok(Some(reason))
            }
            IterationVerdict::RepairsNeeded => {
                if self.iterations.len() as u32 >= self.bounds.max_iterations {
                    let reason = StopReason::IterationCeiling {
                        max_iterations: self.bounds.max_iterations,
                    };
                    self.stopped = Some(reason.clone());
                    return Ok(Some(reason));
                }
                Ok(None)
            }
        }
    }

    /// Record the human decision a parked loop is waiting for.
    ///
    /// Only a loop actually parked at [`StopReason::AwaitingRatification`] can be
    /// ratified: a loop that stopped at a ceiling, on stale input, or
    /// inconclusively cannot be waved through, because there is no clean result
    /// to approve.
    pub fn ratify(
        &mut self,
        approved: bool,
        note: impl Into<String>,
    ) -> Result<&StopReason, ReviewRepairError> {
        match self.stopped.clone() {
            Some(StopReason::AwaitingRatification) => {
                self.stopped = Some(if approved {
                    StopReason::Verified
                } else {
                    StopReason::RatificationDeclined { note: note.into() }
                });
                Ok(self.stopped.as_ref().expect("just set"))
            }
            Some(other) => Err(ReviewRepairError::AlreadyStopped(other)),
            None => Err(ReviewRepairError::IncompleteReceipt(
                "the loop has not produced a clean result to ratify".to_string(),
            )),
        }
    }

    /// Every route this loop ran, in order, for the Workflow row and receipts.
    #[must_use]
    pub fn route_lines(&self) -> Vec<String> {
        let mut lines = Vec::new();
        for iteration in &self.iterations {
            lines.push(format!(
                "#{} {}",
                iteration.iteration,
                iteration.reviewer.display_line()
            ));
            if let Some(verifier) = iteration.verifier.as_ref() {
                lines.push(format!(
                    "#{} {}",
                    iteration.iteration,
                    verifier.display_line()
                ));
            }
        }
        lines
    }

    /// One-line budget statement: what was used against what was allowed.
    #[must_use]
    pub fn budget_line(&self) -> String {
        format!(
            "iterations {}/{}, tools {}/{}, elapsed {}s/{}s",
            self.iterations.len(),
            self.bounds.max_iterations,
            self.tool_calls_used,
            self.bounds.max_tool_calls,
            self.elapsed_secs,
            self.bounds.max_wall_clock_secs,
        )
    }

    fn stop(&mut self, reason: StopReason) -> ReviewRepairError {
        self.stopped = Some(reason.clone());
        ReviewRepairError::Stopped(reason)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn route(role: &str) -> RouteReceipt {
        RouteReceipt {
            role: role.to_string(),
            provider: "zhipu".to_string(),
            model: "glm-5.2".to_string(),
            requested_reasoning: "high".to_string(),
            effective_reasoning: "high".to_string(),
            routed_by: RoutedBy::Fleet,
        }
    }

    fn receipt(iteration: u32, digest: &str, verdict: IterationVerdict) -> IterationReceipt {
        IterationReceipt {
            iteration,
            input_digest: digest.to_string(),
            reviewer: route("reviewer"),
            verifier: Some(route("verifier")),
            verdict,
            finding_summaries: Vec::new(),
            tool_calls_used: 5,
            elapsed_secs: 10,
        }
    }

    fn loop_with(bounds: ReviewRepairBounds, policy: ReviewRepairPolicy) -> ReviewRepairLoop {
        ReviewRepairLoop::new("lane-1", "digest-a", bounds, policy)
    }

    #[test]
    fn iteration_ceiling_stops_before_spending_another_turn() {
        let bounds = ReviewRepairBounds {
            max_iterations: 2,
            ..ReviewRepairBounds::conservative()
        };
        let mut lane = loop_with(bounds, ReviewRepairPolicy::default());

        for i in 1..=2 {
            let n = lane.begin_iteration().expect("iteration within ceiling");
            assert_eq!(n, i);
            let stop = lane
                .record_iteration(receipt(i, "digest-a", IterationVerdict::RepairsNeeded))
                .expect("recorded");
            if i < 2 {
                assert!(stop.is_none());
            } else {
                assert_eq!(
                    stop,
                    Some(StopReason::IterationCeiling { max_iterations: 2 })
                );
            }
        }

        let err = lane.begin_iteration().expect_err("loop is done");
        assert!(matches!(err, ReviewRepairError::AlreadyStopped(_)));
        assert!(lane.stop_reason().expect("stopped").is_ceiling());
        assert!(!lane.stop_reason().expect("stopped").is_success());
    }

    #[test]
    fn zero_iteration_ceiling_never_starts() {
        let mut lane = loop_with(
            ReviewRepairBounds {
                max_iterations: 0,
                ..ReviewRepairBounds::conservative()
            },
            ReviewRepairPolicy::default(),
        );
        let err = lane
            .begin_iteration()
            .expect_err("a zero ceiling runs nothing");
        assert_eq!(
            err.stop_reason(),
            Some(&StopReason::IterationCeiling { max_iterations: 0 })
        );
    }

    #[test]
    fn tool_and_time_ceilings_stop_the_loop_and_report_the_overrun() {
        let mut lane = loop_with(
            ReviewRepairBounds {
                max_iterations: 5,
                max_wall_clock_secs: 600,
                max_tool_calls: 4,
            },
            ReviewRepairPolicy::default(),
        );
        lane.begin_iteration().expect("first iteration");
        let stop = lane
            .record_iteration(receipt(1, "digest-a", IterationVerdict::RepairsNeeded))
            .expect("recorded");
        assert_eq!(
            stop,
            Some(StopReason::ToolCeiling {
                max_tool_calls: 4,
                used: 5
            })
        );
        assert!(lane.budget_line().contains("tools 5/4"));

        let mut timed = loop_with(
            ReviewRepairBounds {
                max_iterations: 5,
                max_wall_clock_secs: 5,
                max_tool_calls: 100,
            },
            ReviewRepairPolicy::default(),
        );
        timed.begin_iteration().expect("first iteration");
        let stop = timed
            .record_iteration(receipt(1, "digest-a", IterationVerdict::RepairsNeeded))
            .expect("recorded");
        assert_eq!(
            stop,
            Some(StopReason::TimeCeiling {
                max_wall_clock_secs: 5,
                elapsed_secs: 10
            })
        );
    }

    #[test]
    fn stale_input_fails_closed_without_repairing() {
        let mut lane = loop_with(
            ReviewRepairBounds::conservative(),
            ReviewRepairPolicy::default(),
        );
        lane.begin_iteration().expect("first iteration");

        let err = lane
            .record_iteration(receipt(1, "digest-b", IterationVerdict::Clean))
            .expect_err("a verdict about another tree is not a verdict about this one");

        assert_eq!(
            err.stop_reason(),
            Some(&StopReason::StaleInput {
                pinned: "digest-a".to_string(),
                reported: "digest-b".to_string(),
            })
        );
        assert!(lane.iterations.is_empty(), "stale work is not recorded");
        assert!(!lane.stop_reason().expect("stopped").is_success());
    }

    #[test]
    fn clean_verdict_parks_for_human_ratification() {
        let mut lane = loop_with(
            ReviewRepairBounds::conservative(),
            ReviewRepairPolicy::default(),
        );
        lane.begin_iteration().expect("first iteration");
        let stop = lane
            .record_iteration(receipt(1, "digest-a", IterationVerdict::Clean))
            .expect("recorded");

        assert_eq!(stop, Some(StopReason::AwaitingRatification));
        assert!(!lane.stop_reason().expect("stopped").is_success());

        let after = lane.ratify(true, "").expect("ratify a parked loop");
        assert_eq!(after, &StopReason::Verified);
        assert!(lane.stop_reason().expect("stopped").is_success());
    }

    #[test]
    fn declined_ratification_is_not_success() {
        let mut lane = loop_with(
            ReviewRepairBounds::conservative(),
            ReviewRepairPolicy::default(),
        );
        lane.begin_iteration().expect("first iteration");
        lane.record_iteration(receipt(1, "digest-a", IterationVerdict::Clean))
            .expect("recorded");

        lane.ratify(false, "diff touches release scripts")
            .expect("decline is a valid decision");
        assert_eq!(
            lane.stop_reason(),
            Some(&StopReason::RatificationDeclined {
                note: "diff touches release scripts".to_string()
            })
        );
        assert!(!lane.stop_reason().expect("stopped").is_success());
    }

    #[test]
    fn a_ceiling_stop_cannot_be_waved_through_by_ratification() {
        let mut lane = loop_with(
            ReviewRepairBounds {
                max_iterations: 1,
                ..ReviewRepairBounds::conservative()
            },
            ReviewRepairPolicy::default(),
        );
        lane.begin_iteration().expect("first iteration");
        lane.record_iteration(receipt(1, "digest-a", IterationVerdict::RepairsNeeded))
            .expect("recorded");

        let err = lane
            .ratify(true, "looks fine to me")
            .expect_err("there is no clean result to approve");
        assert!(matches!(err, ReviewRepairError::AlreadyStopped(_)));
        assert!(!lane.stop_reason().expect("stopped").is_success());
    }

    #[test]
    fn without_the_ratification_policy_a_clean_verdict_verifies_directly() {
        let mut lane = loop_with(
            ReviewRepairBounds::conservative(),
            ReviewRepairPolicy {
                require_human_ratification: false,
                require_verifier_receipt: true,
            },
        );
        lane.begin_iteration().expect("first iteration");
        let stop = lane
            .record_iteration(receipt(1, "digest-a", IterationVerdict::Clean))
            .expect("recorded");
        assert_eq!(stop, Some(StopReason::Verified));
    }

    #[test]
    fn a_missing_verifier_is_inconclusive_not_a_pass() {
        let mut lane = loop_with(
            ReviewRepairBounds::conservative(),
            ReviewRepairPolicy::default(),
        );
        lane.begin_iteration().expect("first iteration");
        let mut r = receipt(1, "digest-a", IterationVerdict::Clean);
        r.verifier = None;

        let err = lane
            .record_iteration(r)
            .expect_err("review alone is not verification");
        assert!(matches!(
            err.stop_reason(),
            Some(StopReason::Inconclusive { .. })
        ));
    }

    #[test]
    fn an_incomplete_route_is_refused_without_ending_the_loop() {
        let mut lane = loop_with(
            ReviewRepairBounds::conservative(),
            ReviewRepairPolicy::default(),
        );
        lane.begin_iteration().expect("first iteration");
        let mut r = receipt(1, "digest-a", IterationVerdict::Clean);
        r.reviewer.model = "  ".to_string();

        let err = lane
            .record_iteration(r)
            .expect_err("a route without a model is a claim");
        assert!(matches!(err, ReviewRepairError::IncompleteReceipt(_)));
        assert!(
            lane.stop_reason().is_none(),
            "the caller may retry with a real receipt"
        );
    }

    #[test]
    fn inconclusive_review_stops_instead_of_passing() {
        let mut lane = loop_with(
            ReviewRepairBounds::conservative(),
            ReviewRepairPolicy::default(),
        );
        lane.begin_iteration().expect("first iteration");
        let stop = lane
            .record_iteration(receipt(1, "digest-a", IterationVerdict::Inconclusive))
            .expect("recorded");
        assert!(matches!(stop, Some(StopReason::Inconclusive { .. })));
    }

    #[test]
    fn route_lines_show_exact_routes_and_requested_to_effective_reasoning() {
        let mut lane = loop_with(
            ReviewRepairBounds::conservative(),
            ReviewRepairPolicy::default(),
        );
        lane.begin_iteration().expect("first iteration");
        let mut r = receipt(1, "digest-a", IterationVerdict::Clean);
        r.reviewer.requested_reasoning = "max".to_string();
        r.reviewer.effective_reasoning = "high".to_string();
        r.reviewer.routed_by = RoutedBy::Router {
            provider: "moonshot".to_string(),
            model: "kimi-k3".to_string(),
        };
        lane.record_iteration(r).expect("recorded");

        let lines = lane.route_lines();
        assert_eq!(
            lines.len(),
            2,
            "reviewer and verifier both appear: {lines:?}"
        );
        assert!(lines[0].contains("reviewer: zhipu/glm-5.2"));
        assert!(lines[0].contains("max→high"), "{lines:?}");
        assert!(lines[0].contains("routed by router moonshot/kimi-k3"));
        assert!(lines[1].contains("verifier: zhipu/glm-5.2"));
    }

    #[test]
    fn default_policy_fails_closed() {
        let policy = ReviewRepairPolicy::default();
        assert!(policy.require_human_ratification);
        assert!(policy.require_verifier_receipt);
        let bounds = ReviewRepairBounds::default();
        assert!(bounds.max_iterations > 0);
        assert!(bounds.max_wall_clock_secs > 0);
        assert!(bounds.max_tool_calls > 0);
    }

    #[test]
    fn review_repair_defines_no_mode_or_permission_posture() {
        // #3832 must not grow a fourth Mode or a fourth posture. This module's
        // serialized surface is the check: a loop carries ceilings, routes, and
        // verdicts — never a mode, posture, approval policy, or sandbox setting.
        let lane = loop_with(
            ReviewRepairBounds::conservative(),
            ReviewRepairPolicy::default(),
        );
        let json = serde_json::to_string(&lane).expect("serialize");
        for forbidden in [
            "mode",
            "posture",
            "approval_policy",
            "sandbox",
            "permission",
            "auto_review",
            "full_access",
        ] {
            assert!(
                !json.contains(forbidden),
                "review-repair leaked {forbidden} into its own state: {json}"
            );
        }
    }
}
