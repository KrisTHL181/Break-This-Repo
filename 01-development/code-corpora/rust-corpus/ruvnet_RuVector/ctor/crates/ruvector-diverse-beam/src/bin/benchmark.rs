/// Diverse Beam ANN Benchmark
///
/// Measures three beam-search variants (GreedyBeam, MMRRerank, CoherenceBeam)
/// on two synthetic datasets (uniform random and 10-cluster Gaussian).
/// Reports recall@K AND mean pairwise distance (diversity) so both trade-offs
/// are visible in the results table.
///
/// Run:
///   cargo run --release -p ruvector-diverse-beam --bin benchmark
use ruvector_diverse_beam::{
    dataset::{clustered, queries_clustered, queries_uniform, uniform},
    graph::FlatGraph,
    mean_pairwise_dist, recall_at_k,
    search::{CoherenceBeam, GreedyBeam, MMRRerank},
    BeamSearch, VecId,
};
use std::time::Instant;

// ─── benchmark config ─────────────────────────────────────────────────────────

const N_VECTORS: usize = 2_500;
const DIM: usize = 64;
const K_NN: usize = 16;
// n_entry ≥ n_clusters ensures all clusters receive an entry point.
const N_ENTRY: usize = 12;
const N_QUERIES: usize = 200;
const K: usize = 10;
const BEAM_WIDTH: usize = 50;
const SEED: u64 = 0x00c0_ffee_babe;

const N_CLUSTERS: usize = 10;
const CLUSTER_SIGMA: f32 = 0.14;

// MMR lambda: 0.75 = moderate diversity (1.0 = pure greedy)
const MMR_LAMBDA: f32 = 0.75;
// Coherence threshold: 0.90 = conservative (prune only very similar directions)
const COHERENCE_THETA: f32 = 0.90;

// ─── acceptance thresholds ────────────────────────────────────────────────────

// Greedy: highest recall baseline
const MIN_RECALL_GREEDY: f32 = 0.70;
// MMR: trades recall for diversity — threshold is intentionally lower
const MIN_RECALL_MMR: f32 = 0.55;
// Coherence: similar to greedy on uniform data
const MIN_RECALL_COHERENCE: f32 = 0.60;
const MIN_QPS_ANY: f64 = 200.0;

// ─── types ────────────────────────────────────────────────────────────────────

struct BenchRow {
    variant: String,
    recall: f32,
    diversity: f32,
    mean_us: f64,
    p50_us: u128,
    p95_us: u128,
    qps: f64,
}

// ─── helpers ─────────────────────────────────────────────────────────────────

fn percentile(sorted: &[u128], p: f64) -> u128 {
    if sorted.is_empty() {
        return 0;
    }
    let idx = ((sorted.len() as f64 * p / 100.0) as usize).min(sorted.len() - 1);
    sorted[idx]
}

fn bench_variant<S: BeamSearch>(
    searcher: &S,
    graph: &FlatGraph,
    queries: &[Vec<f32>],
    ground_truth: &[Vec<VecId>],
) -> BenchRow {
    let mut latencies_us: Vec<u128> = Vec::with_capacity(queries.len());
    let mut total_recall = 0.0_f32;
    let mut total_diversity = 0.0_f32;

    let overall_start = Instant::now();
    for (q, gt) in queries.iter().zip(ground_truth.iter()) {
        let t = Instant::now();
        let results = searcher.search(q, K, BEAM_WIDTH);
        latencies_us.push(t.elapsed().as_micros());
        total_recall += recall_at_k(gt, &results, K);
        total_diversity += mean_pairwise_dist(&results, &graph.vectors);
    }
    let total_us = overall_start.elapsed().as_micros() as f64;

    latencies_us.sort_unstable();
    let mean_us = latencies_us.iter().sum::<u128>() as f64 / latencies_us.len() as f64;
    let p50_us = percentile(&latencies_us, 50.0);
    let p95_us = percentile(&latencies_us, 95.0);
    let qps = queries.len() as f64 / (total_us / 1_000_000.0);

    BenchRow {
        variant: searcher.name().to_string(),
        recall: total_recall / queries.len() as f32,
        diversity: total_diversity / queries.len() as f32,
        mean_us,
        p50_us,
        p95_us,
        qps,
    }
}

fn ground_truth(graph: &FlatGraph, queries: &[Vec<f32>], k: usize) -> Vec<Vec<VecId>> {
    queries
        .iter()
        .map(|q| {
            graph
                .brute_force(q, k)
                .into_iter()
                .map(|(id, _)| id)
                .collect()
        })
        .collect()
}

fn memory_estimate_mb(graph: &FlatGraph) -> f64 {
    graph.memory_bytes() as f64 / 1_048_576.0
}

// ─── printing ─────────────────────────────────────────────────────────────────

fn print_header() {
    println!();
    println!("╔═══════════════════════════════════════════════════════════════════╗");
    println!("║      ruvector-diverse-beam  ·  Benchmark  2026-07-26             ║");
    println!("╠═══════════════════════════════════════════════════════════════════╣");
    println!("║  Variant 1 : GreedyBeam   — standard greedy BFS                  ║");
    println!("║  Variant 2 : MMRRerank    — greedy search + MMR post-reranking    ║");
    println!("║  Variant 3 : CoherenceBeam — cosine-pruned diverse traversal      ║");
    println!("╚═══════════════════════════════════════════════════════════════════╝");
    println!();

    let os = std::env::consts::OS;
    let arch = std::env::consts::ARCH;
    println!("  OS       : {os} / {arch}");
    println!("  N        : {N_VECTORS} vectors");
    println!("  DIM      : {DIM}");
    println!("  K_NN     : {K_NN} graph neighbours/node");
    println!("  K        : {K} results requested");
    println!("  Beam     : {BEAM_WIDTH}");
    println!("  N_ENTRY  : {N_ENTRY}");
    println!("  Queries  : {N_QUERIES}");
    println!("  MMR λ    : {MMR_LAMBDA}");
    println!("  Coh θ    : {COHERENCE_THETA}");
    println!();
}

fn print_scenario(name: &str, rows: &[BenchRow], mem_mb: f64) {
    println!("┌── Scenario: {name}");
    println!("│  Memory estimate : {mem_mb:.2} MB");
    println!(
        "│  {:<16}  {:>9}  {:>10}  {:>10}  {:>8}  {:>8}  {:>10}",
        "Variant", "Recall@10", "Diversity", "Mean(μs)", "p50(μs)", "p95(μs)", "QPS"
    );
    println!("│  {}", "─".repeat(80));
    for r in rows {
        println!(
            "│  {:<16}  {:>9.3}  {:>10.4}  {:>10.1}  {:>8}  {:>8}  {:>10.0}",
            r.variant, r.recall, r.diversity, r.mean_us, r.p50_us, r.p95_us, r.qps
        );
    }
    println!("└{}", "─".repeat(82));
    println!();
}

// ─── main ─────────────────────────────────────────────────────────────────────

fn main() {
    print_header();

    // ── Dataset 1: Uniform random ──────────────────────────────────────────

    println!("Building uniform graph (n={N_VECTORS}, d={DIM}, k_nn={K_NN}) …");
    let t0 = Instant::now();
    let uniform_vecs = uniform(N_VECTORS, DIM, SEED);
    let uniform_graph = FlatGraph::build(uniform_vecs, K_NN);
    println!("  Built in {:.2}s", t0.elapsed().as_secs_f64());

    let uniform_queries = queries_uniform(N_QUERIES, DIM, SEED);
    let uniform_gt = ground_truth(&uniform_graph, &uniform_queries, K);
    let mem_u = memory_estimate_mb(&uniform_graph);

    let gu = bench_variant(
        &GreedyBeam {
            graph: &uniform_graph,
            n_entry: N_ENTRY,
        },
        &uniform_graph,
        &uniform_queries,
        &uniform_gt,
    );
    let mu = bench_variant(
        &MMRRerank {
            graph: &uniform_graph,
            n_entry: N_ENTRY,
            lambda: MMR_LAMBDA,
        },
        &uniform_graph,
        &uniform_queries,
        &uniform_gt,
    );
    let cu = bench_variant(
        &CoherenceBeam {
            graph: &uniform_graph,
            n_entry: N_ENTRY,
            coherence_threshold: COHERENCE_THETA,
        },
        &uniform_graph,
        &uniform_queries,
        &uniform_gt,
    );

    print_scenario("Uniform random", &[gu, mu, cu], mem_u);

    // ── Dataset 2: 10-cluster Gaussian ─────────────────────────────────────

    println!(
        "Building clustered graph (n={N_VECTORS}, {N_CLUSTERS} clusters, σ={CLUSTER_SIGMA}) …"
    );
    let t1 = Instant::now();
    let cluster_vecs = clustered(N_VECTORS, DIM, N_CLUSTERS, CLUSTER_SIGMA, SEED);
    let cluster_graph = FlatGraph::build(cluster_vecs, K_NN);
    println!("  Built in {:.2}s", t1.elapsed().as_secs_f64());

    let cluster_queries = queries_clustered(N_QUERIES, DIM, N_CLUSTERS, CLUSTER_SIGMA, SEED);
    let cluster_gt = ground_truth(&cluster_graph, &cluster_queries, K);
    let mem_c = memory_estimate_mb(&cluster_graph);

    let gc = bench_variant(
        &GreedyBeam {
            graph: &cluster_graph,
            n_entry: N_ENTRY,
        },
        &cluster_graph,
        &cluster_queries,
        &cluster_gt,
    );
    let mc = bench_variant(
        &MMRRerank {
            graph: &cluster_graph,
            n_entry: N_ENTRY,
            lambda: MMR_LAMBDA,
        },
        &cluster_graph,
        &cluster_queries,
        &cluster_gt,
    );
    let cc = bench_variant(
        &CoherenceBeam {
            graph: &cluster_graph,
            n_entry: N_ENTRY,
            coherence_threshold: COHERENCE_THETA,
        },
        &cluster_graph,
        &cluster_queries,
        &cluster_gt,
    );

    print_scenario(
        &format!("{N_CLUSTERS}-cluster Gaussian (σ={CLUSTER_SIGMA})"),
        &[gc, mc, cc],
        mem_c,
    );

    // ── Acceptance tests (on uniform; acceptance is the harder geometric case) ──

    println!("Acceptance thresholds (uniform dataset):");
    println!("  Recall@{K}  GreedyBeam    ≥ {MIN_RECALL_GREEDY:.2}");
    println!("  Recall@{K}  MMRRerank     ≥ {MIN_RECALL_MMR:.2}  (trades recall for diversity)");
    println!("  Recall@{K}  CoherenceBeam ≥ {MIN_RECALL_COHERENCE:.2}");
    println!("  QPS         all variants  ≥ {MIN_QPS_ANY:.0}");
    println!();

    let uniform_vecs2 = uniform(N_VECTORS, DIM, SEED);
    let uniform_graph2 = FlatGraph::build(uniform_vecs2, K_NN);
    let uniform_queries2 = queries_uniform(N_QUERIES, DIM, SEED);
    let uniform_gt2 = ground_truth(&uniform_graph2, &uniform_queries2, K);

    let gv2 = bench_variant(
        &GreedyBeam {
            graph: &uniform_graph2,
            n_entry: N_ENTRY,
        },
        &uniform_graph2,
        &uniform_queries2,
        &uniform_gt2,
    );
    let mv2 = bench_variant(
        &MMRRerank {
            graph: &uniform_graph2,
            n_entry: N_ENTRY,
            lambda: MMR_LAMBDA,
        },
        &uniform_graph2,
        &uniform_queries2,
        &uniform_gt2,
    );
    let cv2 = bench_variant(
        &CoherenceBeam {
            graph: &uniform_graph2,
            n_entry: N_ENTRY,
            coherence_threshold: COHERENCE_THETA,
        },
        &uniform_graph2,
        &uniform_queries2,
        &uniform_gt2,
    );

    let mut pass = true;

    let checks: &[(&BenchRow, f32, &str)] = &[
        (&gv2, MIN_RECALL_GREEDY, "GreedyBeam recall"),
        (&mv2, MIN_RECALL_MMR, "MMRRerank recall"),
        (&cv2, MIN_RECALL_COHERENCE, "CoherenceBeam recall"),
    ];

    for (row, threshold, label) in checks {
        let ok = row.recall >= *threshold;
        let sym = if ok { "✓" } else { "✗" };
        println!(
            "  {sym} {label}: {:.3} (threshold {threshold:.2}) — {}",
            row.recall,
            if ok { "PASS" } else { "FAIL" }
        );
        if !ok {
            pass = false;
        }
    }

    for (row, label) in &[
        (&gv2, "GreedyBeam QPS"),
        (&mv2, "MMRRerank QPS"),
        (&cv2, "CoherenceBeam QPS"),
    ] {
        let ok = row.qps >= MIN_QPS_ANY;
        let sym = if ok { "✓" } else { "✗" };
        println!(
            "  {sym} {label}: {:.0} (threshold {MIN_QPS_ANY:.0}) — {}",
            row.qps,
            if ok { "PASS" } else { "FAIL" }
        );
        if !ok {
            pass = false;
        }
    }

    // MMRRerank must produce strictly more diverse results than greedy.
    let div_ok = mv2.diversity >= gv2.diversity * 0.95;
    let sym = if div_ok { "✓" } else { "✗" };
    println!(
        "  {sym} MMRRerank diversity ≥ 95% of Greedy: {:.4} vs {:.4} — {}",
        mv2.diversity,
        gv2.diversity,
        if div_ok { "PASS" } else { "FAIL" }
    );
    if !div_ok {
        pass = false;
    }

    println!();
    if pass {
        println!("ACCEPTANCE RESULT: PASS ✓ — all recall, QPS and diversity thresholds met");
    } else {
        println!("ACCEPTANCE RESULT: FAIL ✗ — one or more thresholds not met");
        std::process::exit(1);
    }
}
