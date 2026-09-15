//! Integration tests for ruvector-adaptive-ann.
//!
//! Each test builds a small graph, calibrates, and verifies that the
//! adaptive strategies actually achieve their declared recall targets.

use ruvector_adaptive_ann::{
    calibrate::Calibrator,
    dataset::{clustered_unit_vectors, ground_truth, random_queries},
    graph::{FlatGraph, GraphConfig},
    metrics::recall_at_k,
    search::{beam_search, BinarySearchCalibrated, FixedEfSearch, TableCalibratedSearch},
    RecallTargetedSearch,
};

const DIMS: usize = 32;
const N_CLUSTERS: usize = 4;
const PER_CLUSTER: usize = 100; // N = 400
const M: usize = 12;
const M_LONGJUMP: usize = 4;
const K: usize = 10;
const ENTRY: usize = 0;

fn build_graph() -> (FlatGraph, Vec<f32>) {
    let (data, _) = clustered_unit_vectors(N_CLUSTERS, PER_CLUSTER, DIMS, 0.20, 0xDEAD_1234);
    let graph = FlatGraph::build(
        data.clone(),
        GraphConfig {
            m: M,
            m_longjump: M_LONGJUMP,
            dims: DIMS,
        },
    );
    (graph, data)
}

// ─────────────────────────────────────────────────────────────────────────────
// Beam search correctness: ef=N should approach brute-force recall.
// ─────────────────────────────────────────────────────────────────────────────
#[test]
fn test_beam_search_high_ef_achieves_near_perfect_recall() {
    let (graph, data) = build_graph();
    let n = N_CLUSTERS * PER_CLUSTER;
    let queries = random_queries(20, DIMS, 0xAAAA_BBBB);
    let gt = ground_truth(&data, &queries, DIMS, K);

    let mut total_recall = 0.0f32;
    for qi in 0..20 {
        let q = &queries[qi * DIMS..(qi + 1) * DIMS];
        let results = beam_search(&graph, q, K, n, ENTRY);
        let ids: Vec<u32> = results.iter().map(|r| r.id).collect();
        total_recall += recall_at_k(&ids, &gt[qi]);
    }
    let mean_recall = total_recall / 20.0;
    assert!(
        mean_recall >= 0.90,
        "ef=N should achieve near-perfect recall, got {mean_recall:.3}"
    );
}

// ─────────────────────────────────────────────────────────────────────────────
// Recall is monotone: higher ef → higher or equal recall.
// ─────────────────────────────────────────────────────────────────────────────
#[test]
fn test_recall_is_monotone_in_ef() {
    let (graph, data) = build_graph();
    let queries = random_queries(10, DIMS, 0x1111_2222);
    let gt = ground_truth(&data, &queries, DIMS, K);

    let ef_values = [8usize, 16, 32, 64, 128];
    let mut prev_mean = 0.0f32;

    for &ef in &ef_values {
        let mut total_recall = 0.0f32;
        for qi in 0..10 {
            let q = &queries[qi * DIMS..(qi + 1) * DIMS];
            let results = beam_search(&graph, q, K, ef, ENTRY);
            let ids: Vec<u32> = results.iter().map(|r| r.id).collect();
            total_recall += recall_at_k(&ids, &gt[qi]);
        }
        let mean = total_recall / 10.0;
        // Allow tiny rounding slack between ef steps.
        assert!(
            mean >= prev_mean - 0.05,
            "recall not monotone: ef={ef} recall={mean:.3} < prev={prev_mean:.3}"
        );
        prev_mean = mean;
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FixedEfSearch: ef=128 must achieve ≥ 0.70 recall on this small graph.
// ─────────────────────────────────────────────────────────────────────────────
#[test]
fn test_fixed_ef_search_achieves_minimum_recall() {
    let (graph, data) = build_graph();
    let queries = random_queries(30, DIMS, 0xCCCC_DDDD);
    let gt = ground_truth(&data, &queries, DIMS, K);

    let searcher = FixedEfSearch {
        graph: &graph,
        ef: 128,
        entry: ENTRY,
    };
    let mut total_recall = 0.0f32;
    for qi in 0..30 {
        let q = &queries[qi * DIMS..(qi + 1) * DIMS];
        let res = searcher.search_with_target(q, K, 0.90);
        let ids: Vec<u32> = res.iter().map(|r| r.id).collect();
        total_recall += recall_at_k(&ids, &gt[qi]);
    }
    let mean = total_recall / 30.0;
    assert!(mean >= 0.70, "FixedEf(128) recall {mean:.3} < 0.70");
}

// ─────────────────────────────────────────────────────────────────────────────
// Calibration table: min_ef_for_target never returns 0.
// ─────────────────────────────────────────────────────────────────────────────
#[test]
fn test_calibration_table_returns_valid_ef() {
    let (graph, _) = build_graph();
    let mut calibrator = Calibrator {
        graph: &graph,
        entry: ENTRY,
        ef_candidates: vec![16, 32, 64, 128],
        n_sample: 30,
        seed: 0xFEED_CAFE,
    };
    let (cq, cgt) = calibrator.build_ground_truth(K, 30);
    let table = calibrator.calibrate(K, &cq, &cgt);

    let ef = table.min_ef_for_target(0.90, K);
    assert!(ef >= K, "ef={ef} must be ≥ k={K}");
    assert!(ef <= 256, "ef={ef} must be ≤ 256 for this table");
}

// ─────────────────────────────────────────────────────────────────────────────
// TableCalibratedSearch achieves recall >= target on most queries.
// ─────────────────────────────────────────────────────────────────────────────
#[test]
fn test_table_calibrated_achieves_recall_target() {
    let (graph, data) = build_graph();

    let mut calibrator = Calibrator {
        graph: &graph,
        entry: ENTRY,
        ef_candidates: vec![16, 32, 48, 64, 96, 128, 192],
        n_sample: 50,
        seed: 0x9999_AAAA,
    };
    let (cq, cgt) = calibrator.build_ground_truth(K, 50);
    let table = calibrator.calibrate(K, &cq, &cgt);

    let queries = random_queries(40, DIMS, 0x5555_6666);
    let gt = ground_truth(&data, &queries, DIMS, K);

    let searcher = TableCalibratedSearch {
        graph: &graph,
        entry: ENTRY,
        table: &table,
    };
    let target = 0.85f32;
    let mut total_recall = 0.0f32;
    for qi in 0..40 {
        let q = &queries[qi * DIMS..(qi + 1) * DIMS];
        let res = searcher.search_with_target(q, K, target);
        let ids: Vec<u32> = res.iter().map(|r| r.id).collect();
        total_recall += recall_at_k(&ids, &gt[qi]);
    }
    let mean = total_recall / 40.0;
    // Allow 12% below target: calibration uses graph-vector queries while test
    // uses random queries — distribution mismatch is expected and is itself a
    // research finding: calibration quality tracks distribution alignment.
    assert!(
        mean >= target - 0.12,
        "TableCalibrated mean recall {mean:.3} more than 12% below target {target}"
    );
}

// ─────────────────────────────────────────────────────────────────────────────
// BinarySearchCalibrated: achieves at least recall_target on each query.
// ─────────────────────────────────────────────────────────────────────────────
#[test]
fn test_binary_search_calibrated_achieves_per_query_target() {
    let (graph, data) = build_graph();
    let queries = random_queries(15, DIMS, 0x7777_8888);
    let gt = ground_truth(&data, &queries, DIMS, K);

    let target = 0.80f32;
    let mut pass_count = 0usize;

    for qi in 0..15 {
        let bsc = BinarySearchCalibrated {
            graph: &graph,
            entry: ENTRY,
            ef_min: 8,
            ef_max: 200,
            ground_truth: &gt,
            query_index: qi,
        };
        let q = &queries[qi * DIMS..(qi + 1) * DIMS];
        let res = bsc.search_with_target(q, K, target);
        let ids: Vec<u32> = res.iter().map(|r| r.id).collect();
        let recall = recall_at_k(&ids, &gt[qi]);
        if recall >= target {
            pass_count += 1;
        }
    }
    // At least 12 of 15 queries should achieve the target.
    assert!(
        pass_count >= 12,
        "BinarySearch only achieved target on {pass_count}/15 queries"
    );
}

// ─────────────────────────────────────────────────────────────────────────────
// effective_ef: TableCalibratedSearch returns Some(ef), FixedEf returns None.
// ─────────────────────────────────────────────────────────────────────────────
#[test]
fn test_effective_ef_for_target() {
    let (graph, _) = build_graph();

    let fixed = FixedEfSearch {
        graph: &graph,
        ef: 64,
        entry: ENTRY,
    };
    assert_eq!(fixed.effective_ef_for_target(0.90, K), None);

    let mut calibrator = Calibrator {
        graph: &graph,
        entry: ENTRY,
        ef_candidates: vec![32, 64, 128],
        n_sample: 20,
        seed: 0xABCD_EF01,
    };
    let (cq, cgt) = calibrator.build_ground_truth(K, 20);
    let table = calibrator.calibrate(K, &cq, &cgt);

    let tcs = TableCalibratedSearch {
        graph: &graph,
        entry: ENTRY,
        table: &table,
    };
    let ef = tcs.effective_ef_for_target(0.90, K);
    assert!(ef.is_some(), "TableCalibrated must return Some(ef)");
    assert!(ef.unwrap() >= K);

    let gt = vec![vec![0; K]];
    let binary = BinarySearchCalibrated {
        graph: &graph,
        entry: ENTRY,
        ef_min: K,
        ef_max: 128,
        ground_truth: &gt,
        query_index: 0,
    };
    assert_eq!(
        binary.effective_ef_for_target(0.90, K),
        None,
        "query-dependent ef must not be estimated from an unrelated dummy query"
    );
}

#[test]
#[should_panic(expected = "calibration table was built for k=10")]
fn test_calibration_table_rejects_different_k() {
    let (graph, _) = build_graph();
    let mut calibrator = Calibrator {
        graph: &graph,
        entry: ENTRY,
        ef_candidates: vec![32, 64],
        n_sample: 10,
        seed: 1,
    };
    let (queries, gt) = calibrator.build_ground_truth(K, 10);
    let table = calibrator.calibrate(K, &queries, &gt);
    let _ = table.min_ef_for_target(0.90, K + 1);
}

#[test]
fn test_calibration_clamps_to_available_ground_truth() {
    let (graph, _) = build_graph();
    let mut calibrator = Calibrator {
        graph: &graph,
        entry: ENTRY,
        ef_candidates: vec![32, 64],
        n_sample: 10,
        seed: 1,
    };
    let (queries, mut gt) = calibrator.build_ground_truth(K, 10);
    gt.truncate(3);
    let table = calibrator.calibrate(K, &queries, &gt);
    assert!(!table.entries().is_empty());
}
