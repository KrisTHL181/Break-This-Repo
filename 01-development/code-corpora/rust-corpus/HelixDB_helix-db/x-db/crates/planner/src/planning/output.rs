use crate::{context, diagnostics, exec};

/// Successful planner output with an interpreter-ready plan and stable,
/// telemetry-safe diagnostics.
#[derive(Debug, Clone, PartialEq)]
pub struct PlanningOutput {
    plan: exec::ExecutablePlan,
    diagnostics: diagnostics::PlannerDiagnostics,
}

impl PlanningOutput {
    pub(super) fn new(plan: exec::ExecutablePlan, ctx: &context::PlannerContext) -> Self {
        let diagnostics = diagnostics::analyze(&plan, ctx);
        Self { plan, diagnostics }
    }

    /// Borrow the executable plan.
    pub const fn plan(&self) -> &exec::ExecutablePlan {
        &self.plan
    }

    /// Borrow the stable planner diagnostics.
    pub const fn diagnostics(&self) -> &diagnostics::PlannerDiagnostics {
        &self.diagnostics
    }

    /// Consume the output and return the executable plan.
    pub fn into_plan(self) -> exec::ExecutablePlan {
        self.plan
    }

    /// Consume the output and return both owned parts.
    pub fn into_parts(self) -> (exec::ExecutablePlan, diagnostics::PlannerDiagnostics) {
        (self.plan, self.diagnostics)
    }
}
