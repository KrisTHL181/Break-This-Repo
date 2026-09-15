use super::support::assert_same_plan_shape;
use crate::{context, planning};
use helix_ast::batch::{read_batch, write_batch, BatchQuery, ReadBatch, WriteBatch};
use helix_ast::graph::NodeRef;
use helix_ast::traversal::g;

#[test]
fn all_diagnostic_entrypoints_preserve_successful_executable_plans() {
    let ctx = context::PlannerContext::default();
    let read = read_batch()
        .var_as("result", g().n(NodeRef::all()))
        .returning(["result"]);
    let ordinary_read = planning::plan_read_batch(&read, &ctx).unwrap();
    let diagnostic_read = planning::plan_read_batch_with_diagnostics(&read, &ctx).unwrap();
    assert_same_plan_shape(&ordinary_read, diagnostic_read.plan());

    let generic = BatchQuery::Read(read);
    let ordinary_generic = planning::plan(&generic, &ctx).unwrap();
    let diagnostic_generic = planning::plan_with_diagnostics(&generic, &ctx).unwrap();
    assert_same_plan_shape(&ordinary_generic, diagnostic_generic.plan());

    let write = write_batch()
        .var_as("created", g().add_n("User", vec![("name", "alice")]))
        .returning(["created"]);
    let ordinary_write = planning::plan_write_batch(&write, &ctx).unwrap();
    let diagnostic_write = planning::plan_write_batch_with_diagnostics(&write, &ctx).unwrap();
    assert_same_plan_shape(&ordinary_write, diagnostic_write.plan());
}

#[test]
fn planning_output_accessors_and_owned_parts_preserve_both_contracts() {
    let ctx = context::PlannerContext::default();
    let batch = read_batch()
        .var_as("result", g().n(NodeRef::all()))
        .returning(["result"]);
    let output = planning::plan_read_batch_with_diagnostics(&batch, &ctx).unwrap();
    let expected_plan = output.plan().clone();
    let expected_diagnostics = output.diagnostics().clone();

    assert_eq!(output.clone().into_plan(), expected_plan);
    assert_eq!(output.into_parts(), (expected_plan, expected_diagnostics));
}

#[test]
fn diagnostic_entrypoints_preserve_read_write_and_generic_errors() {
    let ctx = context::PlannerContext::default();
    let read = ReadBatch::new();
    assert_eq!(
        planning::plan_read_batch(&read, &ctx).unwrap_err(),
        planning::plan_read_batch_with_diagnostics(&read, &ctx).unwrap_err()
    );
    let generic_read = BatchQuery::Read(read);
    assert_eq!(
        planning::plan(&generic_read, &ctx).unwrap_err(),
        planning::plan_with_diagnostics(&generic_read, &ctx).unwrap_err()
    );

    let write = WriteBatch::new();
    assert_eq!(
        planning::plan_write_batch(&write, &ctx).unwrap_err(),
        planning::plan_write_batch_with_diagnostics(&write, &ctx).unwrap_err()
    );
    let generic_write = BatchQuery::Write(write);
    assert_eq!(
        planning::plan(&generic_write, &ctx).unwrap_err(),
        planning::plan_with_diagnostics(&generic_write, &ctx).unwrap_err()
    );
}
