//! TimesFM-driven early stopping for optimization sweeps (ADR-191 §2).
//!
//! Generalizes the `timesfm::prune` example into a reusable, configurable
//! [`EarlyStopper`] for ruflo / Darwin sweeps: feed the champion metric curve
//! (lower = better) and get a [`StopDecision`]. The stopper adds the two gates
//! the raw [`timesfm::prune::decide_prune`] leaves to the caller — a
//! `min_history` warm-up and a `confidence` floor — so a sweep can wire it in
//! with a single `evaluate` call.

use serde::{Deserialize, Serialize};
use timesfm::prune::PruneDecision;

/// Configurable early-stopping policy.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct EarlyStopper {
    /// Viability threshold (same units as the curve; lower = better). A run is a
    /// PRUNE candidate when its forecast plateau is above this.
    pub threshold: f32,
    /// Total iterations the run is budgeted for (the forecast horizon is
    /// `target_iters - len(curve)`).
    pub target_iters: usize,
    /// Don't decide until at least this many points are observed (warm-up).
    pub min_history: usize,
    /// Only `stop` when `decide_prune`'s confidence is at least this. Lets
    /// borderline runs continue.
    pub confidence_gate: f32,
    /// TimesFM frequency bucket (0 = fine/per-iteration, the right one for
    /// optimization curves).
    pub freq_id: u32,
}

impl Default for EarlyStopper {
    fn default() -> Self {
        Self {
            threshold: 0.05,
            target_iters: 1000,
            min_history: 16,
            confidence_gate: 0.6,
            freq_id: 0,
        }
    }
}

impl EarlyStopper {
    /// New stopper with the given viability threshold and iteration budget;
    /// other fields take their [`Default`] values.
    #[must_use]
    pub fn new(threshold: f32, target_iters: usize) -> Self {
        Self {
            threshold,
            target_iters,
            ..Self::default()
        }
    }

    /// Set the warm-up (minimum observed points before a decision is made).
    #[must_use]
    pub fn with_min_history(mut self, min_history: usize) -> Self {
        self.min_history = min_history;
        self
    }

    /// Set the confidence floor for acting on a PRUNE.
    #[must_use]
    pub fn with_confidence_gate(mut self, gate: f32) -> Self {
        self.confidence_gate = gate;
        self
    }
}

/// Outcome of [`EarlyStopper::evaluate`].
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct StopDecision {
    /// `true` ⇒ kill the run now (forecast plateaus above threshold AND the
    /// confidence gate is cleared AND warm-up satisfied).
    pub stop: bool,
    /// Human/audit-readable reason for the decision.
    pub reason: String,
    /// The underlying forecast decision, when one was computed (`None` during
    /// warm-up).
    pub decision: Option<PruneDecision>,
}

#[cfg(feature = "candle")]
impl EarlyStopper {
    /// Evaluate a partial champion curve and decide whether to stop the run.
    ///
    /// Returns `stop = false` (with `decision = None`) while the curve is
    /// shorter than `min_history`. Otherwise forecasts with `forecaster` and
    /// applies the threshold + confidence gate.
    pub fn evaluate(
        &self,
        forecaster: &crate::Forecaster,
        curve: &[f32],
    ) -> crate::Result<StopDecision> {
        if curve.len() < self.min_history {
            return Ok(StopDecision {
                stop: false,
                reason: format!(
                    "warm-up: {}/{} observations before deciding",
                    curve.len(),
                    self.min_history
                ),
                decision: None,
            });
        }

        let decision = forecaster.prune_decision(curve, self.target_iters, self.threshold)?;
        let stop = decision.prune && decision.confidence >= self.confidence_gate;
        let reason =
            if stop {
                format!(
                "PRUNE: forecast plateau {:.4} > threshold {:.4} (confidence {:.3} ≥ gate {:.3})",
                decision.forecast_plateau, self.threshold, decision.confidence, self.confidence_gate
            )
            } else if decision.prune {
                format!(
                    "CONTINUE: plateau {:.4} > threshold but confidence {:.3} < gate {:.3}",
                    decision.forecast_plateau, decision.confidence, self.confidence_gate
                )
            } else {
                format!(
                    "CONTINUE: forecast plateau {:.4} ≤ threshold {:.4} (or already viable)",
                    decision.forecast_plateau, self.threshold
                )
            };
        Ok(StopDecision {
            stop,
            reason,
            decision: Some(decision),
        })
    }
}
