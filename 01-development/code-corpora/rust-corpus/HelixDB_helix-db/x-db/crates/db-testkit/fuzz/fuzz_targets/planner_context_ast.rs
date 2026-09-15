//! Fuzzes arbitrary serialized planner inputs and the normalized finite domain.

#![no_main]

use helix_ast::batch::BatchQuery;
use helix_db_testkit::planner_domain::{
    self, NormalizedPlannerCase, OptimizerLimitClass, OptionalContextClass, PlannerShape,
    ScaleClass, StorageCostClass, UnionLimitClass,
};
use helix_planner::context::PlannerContext;
use libfuzzer_sys::fuzz_target;
use serde::Deserialize;

#[derive(Deserialize)]
struct SerializedPlannerInput {
    query: BatchQuery,
    context: PlannerContext,
}

fuzz_target!(|data: &[u8]| {
    if let Ok(input) = serde_json::from_slice::<SerializedPlannerInput>(data) {
        planner_domain::check_query(&input.query, &input.context)
            .expect("planner properties must hold for every deserialized input");
    }
    if data.len() < 6 {
        return;
    }
    let case = NormalizedPlannerCase {
        shape: PlannerShape::ALL[data[0] as usize % PlannerShape::ALL.len()],
        scale: ScaleClass::ALL[data[1] as usize % ScaleClass::ALL.len()],
        union_limit: UnionLimitClass::ALL[data[2] as usize % UnionLimitClass::ALL.len()],
        optimizer_limits: OptimizerLimitClass::ALL
            [data[3] as usize % OptimizerLimitClass::ALL.len()],
        optional_context: OptionalContextClass::ALL
            [data[4] as usize % OptionalContextClass::ALL.len()],
        storage_cost: StorageCostClass::ALL[data[5] as usize % StorageCostClass::ALL.len()],
    };
    case.check()
        .expect("planner properties must hold for every normalized domain point");
});
