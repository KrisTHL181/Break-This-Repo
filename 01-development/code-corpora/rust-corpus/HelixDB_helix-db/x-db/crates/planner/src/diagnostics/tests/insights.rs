use std::num::NonZeroUsize;

use super::support::{
    deep_traversals, diagnostics_for_ops, expand_op, missing_indexes, plan, plan_batch, subplan,
    unbounded_scans,
};
use crate::{catalog, context, diagnostics, exec, ir, properties};
use helix_ast::batch::{read_batch, ReadBatch};
use helix_ast::expr::{Expr, Predicate, StreamBound};
use helix_ast::graph::{EdgeRef, NodeRef};
use helix_ast::traversal::{g, sub, RepeatConfig};

#[test]
fn unbounded_scan_insights_cover_node_and_edge_all_and_label_scans() {
    let cases = [
        (
            plan(g().n(NodeRef::all()), &context::PlannerContext::default()),
            catalog::ElementKind::Node,
            None,
        ),
        (
            plan(
                g().n_with_label("User"),
                &context::PlannerContext::default(),
            ),
            catalog::ElementKind::Node,
            Some("User"),
        ),
        (
            plan(g().e(EdgeRef::all()), &context::PlannerContext::default()),
            catalog::ElementKind::Edge,
            None,
        ),
        (
            plan(
                g().e_with_label("FOLLOWS"),
                &context::PlannerContext::default(),
            ),
            catalog::ElementKind::Edge,
            Some("FOLLOWS"),
        ),
    ];

    for (output, element, label) in cases {
        let scans = unbounded_scans(output.diagnostics());
        assert_eq!(scans.len(), 1);
        assert_eq!(scans[0].element, element);
        assert_eq!(scans[0].label.as_ref().map(AsRef::as_ref), label);
        assert_eq!(scans[0].occurrences, 1);
    }
}

#[test]
fn static_access_bounds_suppress_scans_but_dynamic_bounds_do_not() {
    let bounded_node = plan(
        g().n(NodeRef::all()).limit(3usize),
        &context::PlannerContext::default(),
    );
    let bounded_edge = plan(
        g().e_with_label("FOLLOWS").limit(2usize),
        &context::PlannerContext::default(),
    );
    for output in [&bounded_node, &bounded_edge] {
        assert!(unbounded_scans(output.diagnostics()).is_empty());
    }
    assert_eq!(
        bounded_node
            .diagnostics()
            .statistics
            .node_accesses
            .bounded_accesses,
        1
    );
    assert_eq!(
        bounded_edge
            .diagnostics()
            .statistics
            .edge_accesses
            .bounded_accesses,
        1
    );

    let dynamic = plan(
        g().n(NodeRef::all())
            .limit(StreamBound::expr(Expr::param("runtime_limit"))),
        &context::PlannerContext::default(),
    );
    assert_eq!(unbounded_scans(dynamic.diagnostics()).len(), 1);
    assert_eq!(dynamic.diagnostics().statistics.limits, 1);
    assert_eq!(
        dynamic
            .diagnostics()
            .statistics
            .node_accesses
            .bounded_accesses,
        0
    );
}

#[test]
fn partial_and_prefix_kv_scans_are_not_full_graph_scan_insights() {
    let partial = diagnostics_for_ops([exec::ExecOp::KvRead(exec::KvReadPlan::RangeScan {
        keyspace: exec::ElementKeyspace::NodeProperty,
        start: exec::KvKeyBound::included_id(10),
        end: exec::KvKeyBound::Unbounded,
        limit: None,
    })]);
    let prefix = diagnostics_for_ops([exec::ExecOp::KvRead(exec::KvReadPlan::PrefixScan {
        keyspace: exec::ElementKeyspace::EdgeEndpoints,
        prefix: ir::AtLeast::<_, 1>::from_one(7),
        limit: None,
    })]);

    for diagnostics in [&partial, &prefix] {
        assert!(unbounded_scans(diagnostics).is_empty());
        assert_eq!(diagnostics.statistics.node_accesses.all_scans, 0);
        assert_eq!(diagnostics.statistics.edge_accesses.all_scans, 0);
    }
}

#[test]
fn scan_occurrences_are_aggregated_by_target_and_predicate_properties() {
    let batch = read_batch()
        .var_as(
            "filtered",
            g().n_with_label_where("User", Predicate::eq("username", "alice")),
        )
        .var_as("unfiltered_one", g().n_with_label("User"))
        .var_as("unfiltered_two", g().n_with_label("User"));
    let output = plan_batch(&batch, &context::PlannerContext::default());

    assert_eq!(missing_indexes(&output)[0].occurrences, 1);
    let scans = unbounded_scans(output.diagnostics());
    assert_eq!(scans.len(), 2);
    assert_eq!(scans[0].label.as_ref().unwrap().as_ref(), "User");
    assert!(scans[0].predicate_properties.is_empty());
    assert_eq!(scans[0].occurrences, 2);
    assert_eq!(scans[1].label.as_ref().unwrap().as_ref(), "User");
    assert_eq!(
        scans[1]
            .predicate_properties
            .iter()
            .map(AsRef::as_ref)
            .collect::<Vec<_>>(),
        ["username"]
    );
    assert_eq!(scans[1].occurrences, 1);
}

#[test]
fn deep_traversal_threshold_is_inclusive_and_linear_depth_is_exact() {
    let shallow = plan(
        g().n_with_label("User")
            .out(Some("FOLLOWS"))
            .out(Some("FOLLOWS")),
        &context::PlannerContext::default(),
    );
    let deep = plan(
        g().n_with_label("User")
            .out(Some("FOLLOWS"))
            .out(Some("FOLLOWS"))
            .out(Some("FOLLOWS")),
        &context::PlannerContext::default(),
    );

    assert!(deep_traversals(shallow.diagnostics()).is_empty());
    assert_eq!(
        deep_traversals(deep.diagnostics()),
        vec![&diagnostics::DeepTraversalInsight {
            expansion_count: 3,
            repeat_count: 0,
            maximum_depth: 3,
        }]
    );
}

#[test]
fn branch_depth_uses_the_longest_branch_instead_of_summing_branches() {
    let branches = ir::AtLeast::<_, 2>::from_pair(
        subplan([expand_op("A"), expand_op("B"), expand_op("C")]),
        subplan([expand_op("D")]),
    );
    let diagnostics = diagnostics_for_ops([
        exec::ExecOp::Noop,
        exec::ExecOp::Branch {
            plan: exec::ExecBranchPlan::Union(branches),
        },
    ]);

    assert_eq!(diagnostics.statistics.branches, 1);
    assert_eq!(diagnostics.statistics.expansions, 4);
    assert_eq!(
        deep_traversals(&diagnostics),
        vec![&diagnostics::DeepTraversalInsight {
            expansion_count: 4,
            repeat_count: 0,
            maximum_depth: 3,
        }]
    );
}

#[test]
fn independent_batch_roots_reset_traversal_depth() {
    let batch = read_batch()
        .var_as(
            "one",
            g().n_with_label("User")
                .out(Some("FOLLOWS"))
                .out(Some("FOLLOWS")),
        )
        .var_as(
            "two",
            g().n_with_label("User")
                .out(Some("FOLLOWS"))
                .out(Some("FOLLOWS")),
        );
    let output = plan_batch(&batch, &context::PlannerContext::default());

    assert_eq!(output.diagnostics().statistics.expansions, 4);
    assert_eq!(deep_traversals(output.diagnostics())[0].maximum_depth, 2);
}

#[test]
fn every_repeat_stop_form_uses_its_effective_iteration_bound() {
    let cases = [
        (
            RepeatConfig::new(sub().out(Some("FOLLOWS"))).max_depth(2),
            2,
        ),
        (
            RepeatConfig::new(sub().out(Some("FOLLOWS")))
                .times(3)
                .max_depth(10),
            3,
        ),
        (
            RepeatConfig::new(sub().out(Some("FOLLOWS")))
                .times(10)
                .max_depth(3),
            3,
        ),
        (
            RepeatConfig::new(sub().out(Some("FOLLOWS")))
                .until(Predicate::eq("done", true))
                .max_depth(4),
            4,
        ),
        (
            RepeatConfig::new(sub().out(Some("FOLLOWS")))
                .times(5)
                .until(Predicate::eq("done", true))
                .max_depth(3),
            3,
        ),
    ];

    for (config, expected_depth) in cases {
        let output = plan(
            g().n_with_label("User").repeat(config),
            &context::PlannerContext::default(),
        );
        let insight = deep_traversals(output.diagnostics())[0];
        assert_eq!(insight.maximum_depth, expected_depth);
        assert_eq!(insight.expansion_count, 1);
        assert_eq!(insight.repeat_count, 1);
    }
}

#[test]
fn nested_repeat_depth_multiplies_bounds_without_multiplying_operator_counts() {
    let inner = exec::ExecRepeatPlan {
        body: Box::new(subplan([expand_op("FOLLOWS")])),
        stop: ir::RepeatStopPlan::Times {
            count: NonZeroUsize::new(2).unwrap(),
        },
        emit: ir::RepeatEmitPlan::None,
        max_depth: NonZeroUsize::new(10).unwrap(),
    };
    let outer = exec::ExecRepeatPlan {
        body: Box::new(subplan([exec::ExecOp::Repeat { plan: inner }])),
        stop: ir::RepeatStopPlan::Times {
            count: NonZeroUsize::new(3).unwrap(),
        },
        emit: ir::RepeatEmitPlan::None,
        max_depth: NonZeroUsize::new(10).unwrap(),
    };
    let diagnostics =
        diagnostics_for_ops([exec::ExecOp::Noop, exec::ExecOp::Repeat { plan: outer }]);
    let insight = deep_traversals(&diagnostics)[0];

    assert_eq!(insight.maximum_depth, 6);
    assert_eq!(insight.expansion_count, 1);
    assert_eq!(insight.repeat_count, 2);
}

#[test]
fn insight_cap_prioritizes_sorted_unbounded_scans_before_other_kinds() {
    let batch = (0..diagnostics::MAX_PLANNER_INSIGHTS).fold(ReadBatch::new(), |batch, index| {
        batch.var_as(
            &format!("missing_{index}"),
            g().n_with_label_where(
                "User",
                Predicate::eq(
                    format!("property_{index:02}"),
                    i64::try_from(index).unwrap(),
                ),
            ),
        )
    });
    let batch = batch.var_as("unbounded", g().e(EdgeRef::all())).var_as(
        "deep",
        g().n_with_label("User")
            .out(Some("FOLLOWS"))
            .out(Some("FOLLOWS"))
            .out(Some("FOLLOWS")),
    );
    let output = plan_batch(&batch, &context::PlannerContext::default());

    assert_eq!(
        output.diagnostics().insights.len(),
        diagnostics::MAX_PLANNER_INSIGHTS
    );
    assert!(output
        .diagnostics()
        .insights
        .iter()
        .all(|insight| matches!(insight, diagnostics::PlannerInsight::UnboundedScan(_))));
    let scans = unbounded_scans(output.diagnostics());
    assert!(scans[0].predicate_properties.is_empty());
    assert_eq!(
        scans[1]
            .predicate_properties
            .iter()
            .next()
            .unwrap()
            .as_ref(),
        "property_00"
    );
    assert_eq!(
        scans[diagnostics::MAX_PLANNER_INSIGHTS - 1]
            .predicate_properties
            .iter()
            .next()
            .unwrap()
            .as_ref(),
        "property_14"
    );
}

#[test]
fn directly_limited_native_access_is_counted_without_an_unbounded_insight() {
    let diagnostics = diagnostics_for_ops([exec::ExecOp::Access {
        plan: Box::new(
            exec::ExecAccessPlan::Edge(exec::ExecEdgeAccessPlan::AllScan)
                .limited(properties::PositiveUsize::new(4).unwrap()),
        ),
    }]);

    assert_eq!(diagnostics.statistics.edge_accesses.all_scans, 1);
    assert_eq!(diagnostics.statistics.edge_accesses.bounded_accesses, 1);
    assert!(unbounded_scans(&diagnostics).is_empty());
}
