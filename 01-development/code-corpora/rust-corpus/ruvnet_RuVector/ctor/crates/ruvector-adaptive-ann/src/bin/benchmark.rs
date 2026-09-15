//! Adaptive Recall-Targeted ANN — benchmark binary.
//!
//! Compares three search strategies on a flat navigable small-world graph.
//! All strategies search the same graph; the only difference is how they
//! select `ef` (beam width) given a recall target.
//!
//! ## Variants
//!
//! 1. **FixedEf(64)**            — Baseline. Always uses ef=64.
//! 2. **BinarySearchCalibrated** — Binary-searches ef per-query with ground truth.
//! 3. **TableCalibrated**        — One-shot offline calibration; O(1) ef lookup.
//!
//! ## Usage
//!
//!   cargo run --release -p ruvector-adaptive-ann --bin benchmark

use std::time::Instant;

use ruvector_adaptive_ann::{
    calibrate::Calibrator,
    dataset::{clustered_unit_vectors, ground_truth, random_queries},
    graph::{FlatGraph, GraphConfig},
    metrics::{memory_estimate_bytes, recall_at_k, LatencyStats},
    search::{beam_search, BinarySearchCalibrated, FixedEfSearch, TableCalibratedSearch},
    RecallTargetedSearch,
};

// ─── Dataset parameters ───────────────────────────────────────────────────────
const N_CLUSTERS: usize = 10;
const N_PER_CLUSTER: usize = 300; // 10 × 300 = 3_000 total
const N: usize = N_CLUSTERS * N_PER_CLUSTER;
const DIMS: usize = 64;
const CLUSTER_STD: f32 = 0.20;

// Graph parameters.
const M: usize = 16;
const M_LONGJUMP: usize = 6;

// Search parameters.
const K: usize = 10;
const N_QUERIES: usize = 300;
const RECALL_TARGET: f32 = 0.90;
const EF_FIXED: usize = 64;
const EF_MIN: usize = 8;
const EF_MAX: usize = 256;

// Calibration parameters.
const N_CALIB_SAMPLE: usize = 100;
const EF_CANDIDATES: &[usize] = &[8, 16, 24, 32, 48, 64, 96, 128, 192, 256];

// Fixed entry point (simulates HNSW layer-0 start after upper-layer descent).
const ENTRY: usize = 0;

// ─── Acceptance thresholds ────────────────────────────────────────────────────
const MIN_BASELINE_RECALL: f32 = 0.70;
// TableCalibrated must achieve the recall target (within 2% tolerance).
const MIN_TABLE_CALIB_RECALL: f32 = RECALL_TARGET - 0.02;
// TableCalibrated must beat the FixedEf baseline (calibration must add value).
const TABLE_MUST_BEAT_BASELINE: bool = true;

fn main() {
    print_header();

    // ─── Build dataset ────────────────────────────────────────────────────────
    eprintln!(
        "[bench] Generating clustered dataset: {N_CLUSTERS}×{N_PER_CLUSTER}={N} vectors, D={DIMS}…"
    );
    let (data, _) =
        clustered_unit_vectors(N_CLUSTERS, N_PER_CLUSTER, DIMS, CLUSTER_STD, 0xBEEF_DEAD);

    eprintln!("[bench] Generating {N_QUERIES} random queries…");
    let queries = random_queries(N_QUERIES, DIMS, 0xCAFE_1234);

    eprintln!("[bench] Computing brute-force ground truth for {N_QUERIES} queries…");
    let gt = ground_truth(&data, &queries, DIMS, K);

    // ─── Build graph ──────────────────────────────────────────────────────────
    eprintln!("[bench] Building flat k-NN graph (M={M}, M_longjump={M_LONGJUMP})…");
    let t0 = Instant::now();
    let graph = FlatGraph::build(
        data.clone(),
        GraphConfig {
            m: M,
            m_longjump: M_LONGJUMP,
            dims: DIMS,
        },
    );
    let build_ms = t0.elapsed().as_millis();
    eprintln!("[bench] Graph built in {build_ms} ms.");

    let mem_bytes = memory_estimate_bytes(N, DIMS, M + M_LONGJUMP);

    // ─── Calibration ─────────────────────────────────────────────────────────
    // Use held-out random queries (same distribution as test queries) so the
    // calibration table transfers accurately to the test set.
    eprintln!(
        "[bench] Running calibration ({N_CALIB_SAMPLE} held-out random queries, {} ef candidates)…",
        EF_CANDIDATES.len()
    );
    let t_calib = Instant::now();
    let calib_queries = random_queries(N_CALIB_SAMPLE, DIMS, 0xABCD_1234); // different seed from test
    let calib_gt_data = ground_truth(&data, &calib_queries, DIMS, K);
    let mut calibrator = Calibrator {
        graph: &graph,
        entry: ENTRY,
        ef_candidates: EF_CANDIDATES.to_vec(),
        n_sample: N_CALIB_SAMPLE,
        seed: 0xABCD_1234,
    };
    let calib_table = calibrator.calibrate(K, &calib_queries, &calib_gt_data);
    let calib_ms = t_calib.elapsed().as_millis();
    eprintln!("[bench] Calibration done in {calib_ms} ms.");
    eprintln!("[bench] Calibration table (ef → mean_recall@{K}):");
    for &(ef, recall) in calib_table.entries() {
        eprintln!("        ef={:4}  recall={:.3}", ef, recall);
    }
    let table_ef = calib_table.min_ef_for_target(RECALL_TARGET, K);
    eprintln!(
        "[bench] Table selects ef={} for recall_target={RECALL_TARGET}",
        table_ef
    );

    println!();
    println!("Dataset : {N} vectors × {DIMS} dims  ({N_CLUSTERS} clusters, σ={CLUSTER_STD})");
    println!("Graph   : M={M}, M_longjump={M_LONGJUMP}, entry=node {ENTRY}");
    println!("Queries : {N_QUERIES}  k={K}  recall_target={RECALL_TARGET}");
    println!("Memory  : ~{:.1} MB", mem_bytes as f64 / 1_048_576.0);
    println!("Build   : {build_ms} ms");
    println!("Calibration : {calib_ms} ms  table_ef={table_ef}");
    println!();

    // ─── Variant 1: FixedEf baseline ─────────────────────────────────────────
    println!("─── Variant 1: FixedEf(ef={EF_FIXED}) — baseline ───────────────────────");
    let fixed = FixedEfSearch {
        graph: &graph,
        ef: EF_FIXED,
        entry: ENTRY,
    };
    let (fixed_recall, fixed_lat) = run_variant(&fixed, &queries, &gt, RECALL_TARGET);

    // ─── Variant 2: BinarySearchCalibrated ───────────────────────────────────
    println!("─── Variant 2: BinarySearchCalibrated (oracle, per-query ef search) ────");
    let mut bsc = BinarySearchCalibrated {
        graph: &graph,
        entry: ENTRY,
        ef_min: EF_MIN,
        ef_max: EF_MAX,
        ground_truth: &gt,
        query_index: 0,
    };

    let mut bsc_lat = LatencyStats::default();
    let mut bsc_total_recall = 0.0f32;
    let mut bsc_ef_sum = 0usize;

    for qi in 0..N_QUERIES {
        bsc.query_index = qi;
        let q = &queries[qi * DIMS..(qi + 1) * DIMS];
        let t0 = Instant::now();
        let res = bsc.search_with_target(q, K, RECALL_TARGET);
        bsc_lat.push(t0.elapsed().as_nanos() as u64);
        let ids: Vec<u32> = res.iter().map(|r| r.id).collect();
        bsc_total_recall += recall_at_k(&ids, &gt[qi]);

        // Measure effective ef via binary search separately.
        let ef = find_binary_ef(&graph, q, K, &gt[qi], EF_MIN, EF_MAX, RECALL_TARGET, ENTRY);
        bsc_ef_sum += ef;
    }
    let bsc_mean_recall = bsc_total_recall / N_QUERIES as f32;
    let bsc_mean_ef = bsc_ef_sum as f64 / N_QUERIES as f64;

    print_variant_result(
        "BinarySearchCalibrated",
        &bsc_lat,
        bsc_mean_recall,
        Some(bsc_mean_ef),
    );

    // ─── Variant 3: TableCalibratedSearch ────────────────────────────────────
    println!("─── Variant 3: TableCalibratedSearch (offline table, O(1) ef lookup) ──");
    let tcs = TableCalibratedSearch {
        graph: &graph,
        entry: ENTRY,
        table: &calib_table,
    };
    let (tcs_recall, tcs_lat) = run_variant(&tcs, &queries, &gt, RECALL_TARGET);
    let effective_ef = tcs.effective_ef_for_target(RECALL_TARGET, K);

    // ─── Summary table ────────────────────────────────────────────────────────
    println!();
    println!("╔══════════════════════════════════════════════════════════════════════════╗");
    println!("║ Variant              │ Recall@{K:<2} │ Mean µs │ p50 µs │ p95 µs │ QPS    ║");
    println!("╠══════════════════════════════════════════════════════════════════════════╣");
    println!(
        "║ FixedEf({EF_FIXED:3})           │  {:.3}  │ {:7.1} │ {:6.1} │ {:6.1} │ {:6.0} ║",
        fixed_recall,
        fixed_lat.mean_us(),
        fixed_lat.p50_us(),
        fixed_lat.p95_us(),
        fixed_lat.qps()
    );
    println!(
        "║ BinarySearchCalibrated │  {:.3}  │ {:7.1} │ {:6.1} │ {:6.1} │ {:6.0} ║",
        bsc_mean_recall,
        bsc_lat.mean_us(),
        bsc_lat.p50_us(),
        bsc_lat.p95_us(),
        bsc_lat.qps()
    );
    println!(
        "║ TableCalibrated        │  {:.3}  │ {:7.1} │ {:6.1} │ {:6.1} │ {:6.0} ║",
        tcs_recall,
        tcs_lat.mean_us(),
        tcs_lat.p50_us(),
        tcs_lat.p95_us(),
        tcs_lat.qps()
    );
    println!("╚══════════════════════════════════════════════════════════════════════════╝");
    println!();
    println!(
        "Effective ef selected by TableCalibrated for recall_target={RECALL_TARGET}: {:?}",
        effective_ef
    );
    println!(
        "Mean ef selected by BinarySearch per query: {:.1}",
        bsc_mean_ef
    );
    println!();

    // ─── Acceptance test ─────────────────────────────────────────────────────
    let mut pass = true;

    let fixed_pass = fixed_recall >= MIN_BASELINE_RECALL;
    println!(
        "[ACCEPT] FixedEf baseline recall {:.3} >= {MIN_BASELINE_RECALL} → {}",
        fixed_recall,
        if fixed_pass { "PASS" } else { "FAIL" }
    );
    pass &= fixed_pass;

    let tcs_pass = tcs_recall >= MIN_TABLE_CALIB_RECALL;
    println!(
        "[ACCEPT] TableCalibrated recall {:.3} >= {MIN_TABLE_CALIB_RECALL} → {}",
        tcs_recall,
        if tcs_pass { "PASS" } else { "FAIL" }
    );
    pass &= tcs_pass;

    if TABLE_MUST_BEAT_BASELINE {
        let beats_baseline = tcs_recall > fixed_recall;
        println!(
            "[ACCEPT] TableCalibrated recall {:.3} > FixedEf recall {:.3} → {}",
            tcs_recall,
            fixed_recall,
            if beats_baseline { "PASS" } else { "FAIL" }
        );
        pass &= beats_baseline;
    }

    let bsc_pass = bsc_mean_recall >= RECALL_TARGET - 0.05;
    println!(
        "[ACCEPT] BinarySearch mean_recall {:.3} >= {:.2} → {}",
        bsc_mean_recall,
        RECALL_TARGET - 0.05,
        if bsc_pass { "PASS" } else { "FAIL" }
    );
    pass &= bsc_pass;

    println!();
    if pass {
        println!("✓ ALL ACCEPTANCE TESTS PASSED");
    } else {
        println!("✗ ONE OR MORE ACCEPTANCE TESTS FAILED");
        std::process::exit(1);
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

fn run_variant<S: RecallTargetedSearch>(
    searcher: &S,
    queries: &[f32],
    gt: &[Vec<u32>],
    recall_target: f32,
) -> (f32, LatencyStats) {
    let mut lat = LatencyStats::default();
    let mut total_recall = 0.0f32;

    for qi in 0..N_QUERIES {
        let q = &queries[qi * DIMS..(qi + 1) * DIMS];
        let t0 = Instant::now();
        let res = searcher.search_with_target(q, K, recall_target);
        lat.push(t0.elapsed().as_nanos() as u64);
        let ids: Vec<u32> = res.iter().map(|r| r.id).collect();
        total_recall += recall_at_k(&ids, &gt[qi]);
    }
    let mean_recall = total_recall / N_QUERIES as f32;
    print_variant_result("FixedEf/TableCalibrated", &lat, mean_recall, None);
    (mean_recall, lat)
}

fn print_variant_result(_name: &str, lat: &LatencyStats, recall: f32, mean_ef: Option<f64>) {
    println!("  Recall@{K}: {:.3}", recall);
    println!("  Mean latency : {:.1} µs", lat.mean_us());
    println!("  p50 latency  : {:.1} µs", lat.p50_us());
    println!("  p95 latency  : {:.1} µs", lat.p95_us());
    println!("  Throughput   : {:.0} QPS", lat.qps());
    if let Some(ef) = mean_ef {
        println!("  Mean ef used : {:.1}", ef);
    }
    println!();
}

/// Find minimum ef for which recall ≥ target on a specific query, by binary search.
#[allow(clippy::too_many_arguments)]
fn find_binary_ef(
    graph: &FlatGraph,
    query: &[f32],
    k: usize,
    gt: &[u32],
    ef_min: usize,
    ef_max: usize,
    recall_target: f32,
    entry: usize,
) -> usize {
    let mut lo = ef_min;
    let mut hi = ef_max;
    while lo < hi {
        let mid = (lo + hi) / 2;
        let res = beam_search(graph, query, k, mid, entry);
        let ids: Vec<u32> = res.iter().map(|r| r.id).collect();
        if recall_at_k(&ids, gt) >= recall_target {
            hi = mid;
        } else {
            lo = mid + 1;
        }
    }
    lo
}

fn print_header() {
    let os = std::env::consts::OS;
    let arch = std::env::consts::ARCH;
    let _rustc = option_env!("RUSTC_VERSION").unwrap_or("unknown");
    println!("╔═══════════════════════════════════════════════════════════════════════════╗");
    println!("║        ruvector-adaptive-ann  —  Adaptive Recall-Targeted ANN            ║");
    println!("╠═══════════════════════════════════════════════════════════════════════════╣");
    println!("║  OS   : {os:<66}║");
    println!("║  Arch : {arch:<66}║");
    println!("╚═══════════════════════════════════════════════════════════════════════════╝");
    println!();
}
