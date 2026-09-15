//! Production contracts for Active text mutation resource admission.
//!
//! The harness is a feature-gated child of the owning module, preserving the
//! private capability boundary while exercising the exact production limits.
//! It proves equality-at-limit admission, stable first-failure ordering, and
//! every typed resource rejection without constructing any persisted row.

use std::num::{NonZeroU64, NonZeroUsize};

use super::*;
use crate::config::{
    SearchIndexBackfillLimits, SearchIndexBatchLimits, TextBackfillCompactionLimits,
    TextBuildArtifactLimits,
};

/// Constructs distinct ceilings so every rejection identifies one resource.
fn limits() -> ActiveTextMutationLimits {
    SearchIndexBackfillLimits::try_new(
        SearchIndexBatchLimits::try_new(
            NonZeroUsize::MIN,
            NonZeroU64::new(10).expect("input limit is non-zero"),
            NonZeroU64::new(20).expect("operation limit is non-zero"),
            NonZeroU64::new(60).expect("output limit is non-zero"),
            NonZeroU64::MIN,
        )
        .expect("batch limits validate"),
        NonZeroUsize::MIN,
        TextBuildArtifactLimits::new(NonZeroUsize::MIN, NonZeroU64::MIN),
        TextBackfillCompactionLimits::new(
            NonZeroUsize::MIN,
            NonZeroU64::new(10).expect("compaction input limit is non-zero"),
            NonZeroU64::new(40).expect("temporary limit is non-zero"),
            NonZeroU64::new(40).expect("split limit is non-zero"),
            NonZeroU64::new(50).expect("manifest limit is non-zero"),
        ),
    )
    .expect("backfill limits validate")
    .active_text_mutation()
}

/// Runs exact admission and every ordered resource rejection.
pub(crate) fn run() {
    let admitted = ActiveTextMutationMeasurements::try_admit(limits(), 10, 20, 60, 40, 50)
        .expect("values equal to every ceiling are admitted");
    assert_eq!(admitted.input_bytes(), 10);
    assert_eq!(admitted.output_operations(), 20);
    assert_eq!(admitted.output_bytes(), 60);
    assert_eq!(admitted.split_bytes(), 40);
    assert_eq!(admitted.manifest_page_bytes(), 50);

    let epoch = ActiveTextMutationMeasurements::try_admit_epoch(
        limits(),
        ActiveTextMutationUsage {
            entities: 1,
            input_bytes: 10,
            output_operations: 20,
            output_bytes: 60,
            split_bytes: 40,
            retained_split_bytes: 10,
            manifest_page_bytes: 50,
        },
    )
    .expect("epoch values equal to every ceiling are admitted");
    assert_eq!(epoch.entities(), 1);
    assert_eq!(epoch.retained_split_bytes(), 10);
    for (entities, retained, expected_resource, expected_limit) in [
        (2, 10, ActiveTextMutationResource::Entities, 1),
        (1, 11, ActiveTextMutationResource::RetainedSplitBytes, 10),
    ] {
        assert!(matches!(
            ActiveTextMutationMeasurements::try_admit_epoch(
                limits(),
                ActiveTextMutationUsage {
                    entities,
                    input_bytes: 10,
                    output_operations: 20,
                    output_bytes: 60,
                    split_bytes: 40,
                    retained_split_bytes: retained,
                    manifest_page_bytes: 50,
                },
            ),
            Err(HelixDbError::ActiveTextMutationLimitExceeded {
                resource,
                observed,
                limit,
            }) if resource == expected_resource
                && observed == expected_limit + 1
                && limit == expected_limit
        ));
    }

    for (values, expected_resource, expected_limit) in [
        (
            [11, 20, 60, 40, 50],
            ActiveTextMutationResource::InputBytes,
            10,
        ),
        (
            [10, 21, 60, 40, 50],
            ActiveTextMutationResource::OutputOperations,
            20,
        ),
        (
            [10, 20, 61, 40, 50],
            ActiveTextMutationResource::OutputBytes,
            60,
        ),
        (
            [10, 20, 60, 41, 50],
            ActiveTextMutationResource::SplitBytes,
            40,
        ),
        (
            [10, 20, 60, 40, 51],
            ActiveTextMutationResource::ManifestPageBytes,
            50,
        ),
    ] {
        let [input, operations, output, split, manifest] = values;
        assert!(matches!(
            ActiveTextMutationMeasurements::try_admit(
                limits(),
                input,
                operations,
                output,
                split,
                manifest,
            ),
            Err(HelixDbError::ActiveTextMutationLimitExceeded {
                resource,
                observed,
                limit,
            }) if resource == expected_resource
                && observed == expected_limit + 1
                && limit == expected_limit
        ));
    }

    assert!(matches!(
        ActiveTextMutationMeasurements::try_admit(
            limits(),
            u64::MAX,
            u64::MAX,
            u64::MAX,
            u64::MAX,
            u64::MAX,
        ),
        Err(HelixDbError::ActiveTextMutationLimitExceeded {
            resource: ActiveTextMutationResource::InputBytes,
            observed: u64::MAX,
            limit: 10,
        })
    ));
}
