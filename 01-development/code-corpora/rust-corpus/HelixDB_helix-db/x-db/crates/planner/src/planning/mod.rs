use std::time::Instant;

use crate::{context, error, exec};
use helix_ast::batch::{BatchQuery, ReadBatch, WriteBatch};

pub mod control_flow;
mod envelope;
mod executable;
mod index_ddl;
pub mod mutation;
mod output;
pub mod search;
mod selected;

pub use output::PlanningOutput;

/// Plan a read or write batch into the executable DAG contract.
pub fn plan(
    query: &BatchQuery,
    ctx: &context::PlannerContext,
) -> Result<exec::ExecutablePlan, error::PlannerError> {
    plan_with_diagnostics(query, ctx).map(PlanningOutput::into_plan)
}

/// Plan a read or write batch and return stable planner diagnostics alongside
/// the executable DAG.
pub fn plan_with_diagnostics(
    query: &BatchQuery,
    ctx: &context::PlannerContext,
) -> Result<PlanningOutput, error::PlannerError> {
    executable::executable_from_query(query, ctx, Instant::now())
        .map(|plan| PlanningOutput::new(plan, ctx))
}

/// Plan a read batch into the executable DAG contract.
pub fn plan_read_batch(
    batch: &ReadBatch,
    ctx: &context::PlannerContext,
) -> Result<exec::ExecutablePlan, error::PlannerError> {
    plan_read_batch_with_diagnostics(batch, ctx).map(PlanningOutput::into_plan)
}

/// Plan a read batch and return stable planner diagnostics alongside the
/// executable DAG.
pub fn plan_read_batch_with_diagnostics(
    batch: &ReadBatch,
    ctx: &context::PlannerContext,
) -> Result<PlanningOutput, error::PlannerError> {
    executable::executable_from_read_batch(batch, ctx, Instant::now())
        .map(|plan| PlanningOutput::new(plan, ctx))
}

/// Plan a write batch into the executable DAG contract.
pub fn plan_write_batch(
    batch: &WriteBatch,
    ctx: &context::PlannerContext,
) -> Result<exec::ExecutablePlan, error::PlannerError> {
    plan_write_batch_with_diagnostics(batch, ctx).map(PlanningOutput::into_plan)
}

/// Plan a write batch and return stable planner diagnostics alongside the
/// executable DAG.
pub fn plan_write_batch_with_diagnostics(
    batch: &WriteBatch,
    ctx: &context::PlannerContext,
) -> Result<PlanningOutput, error::PlannerError> {
    executable::executable_from_write_batch(batch, ctx, Instant::now())
        .map(|plan| PlanningOutput::new(plan, ctx))
}

#[cfg(test)]
mod tests;
