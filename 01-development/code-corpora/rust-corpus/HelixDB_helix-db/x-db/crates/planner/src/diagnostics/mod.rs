//! Stable, telemetry-safe diagnostics derived from the selected executable
//! plan.
//!
//! These contracts intentionally exclude query values, optimizer traces, and
//! complete executable-plan payloads. Callers can forward them without learning
//! planner-internal memo or rule representations.

mod analyze;
mod insight;
mod statistics;

use serde::{Deserialize, Serialize};

pub use self::insight::{
    DeepTraversalInsight, MissingIndexInsight, PlannerInsight, PredicatePropertySet,
    SecondaryIndexKind, UnboundedScanInsight,
};
pub use self::statistics::{AccessStatistics, PlannerStatistics};

use crate::{context, exec};

/// Maximum number of insights returned for one planned query.
pub const MAX_PLANNER_INSIGHTS: usize = 16;

/// Selected-plan statistics and bounded actionable insights.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub struct PlannerDiagnostics {
    /// Planner work and selected executable-plan shape statistics.
    pub statistics: PlannerStatistics,
    /// Deterministically ordered, deduplicated insights.
    pub insights: Vec<PlannerInsight>,
}

pub(crate) fn analyze(
    plan: &exec::ExecutablePlan,
    ctx: &context::PlannerContext,
) -> PlannerDiagnostics {
    analyze::Analyzer::new(ctx).analyze(plan)
}

#[cfg(test)]
mod tests;
