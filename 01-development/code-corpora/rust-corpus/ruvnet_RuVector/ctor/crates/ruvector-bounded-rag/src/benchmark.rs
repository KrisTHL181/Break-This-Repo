//! Bounded RAG MinCut benchmark
//!
//! Measures three retrieval variants across two dataset sizes.
//! Run: cargo run --release -p ruvector-bounded-rag --bin benchmark
use ruvector_bounded_rag::{
    BoundedRetriever, Corpus, GraphBfsRetriever, MinCutRetriever, Query, RetrieverConfig,
    TopKRetriever,
};

use rand::{rngs::StdRng, SeedableRng};
use rand_distr::{Distribution, Normal};
use std::time::Instant;

struct BenchCase {
    n_chunks: usize,
    n_queries: usize,
    dim: usize,
    n_clusters: usize,
    budget: usize,
    edge_threshold: f32,
    seed_threshold: f32,
}

fn build_corpus(case: &BenchCase, rng: &mut StdRng) -> (Corpus, Vec<Query>) {
    let normal = Normal::new(0.0_f32, 0.08).unwrap();
    let per_cluster = case.n_chunks / case.n_clusters;

    let mut vecs: Vec<Vec<f32>> = Vec::with_capacity(case.n_chunks);
    let mut labels: Vec<u32> = Vec::with_capacity(case.n_chunks);

    for cluster in 0..case.n_clusters {
        for _ in 0..per_cluster {
            let mut v = vec![0.0_f32; case.dim];
            v[cluster % case.dim] = 1.0;
            for x in v.iter_mut() {
                *x += normal.sample(rng);
            }
            vecs.push(v);
            labels.push(cluster as u32);
        }
    }
    let corpus = Corpus::from_vecs(vecs).with_labels(labels);

    let mut queries: Vec<Query> = Vec::with_capacity(case.n_queries);
    for i in 0..case.n_queries {
        let target_cluster = i % case.n_clusters;
        let mut qv = vec![0.0_f32; case.dim];
        qv[target_cluster % case.dim] = 1.0;
        for x in qv.iter_mut() {
            *x += normal.sample(rng) * 0.05;
        }
        queries.push(Query::new(qv).with_relevant([target_cluster as u32]));
    }

    (corpus, queries)
}

struct Stats {
    mean_us: f64,
    p50_us: f64,
    p95_us: f64,
    throughput_qps: f64,
    mean_precision: f64,
    mean_budget_util: f64,
}

fn run_variant(retriever: &dyn BoundedRetriever, corpus: &Corpus, queries: &[Query]) -> Stats {
    let mut latencies_us: Vec<f64> = Vec::with_capacity(queries.len());
    let mut precisions: Vec<f64> = Vec::with_capacity(queries.len());
    let mut budgets: Vec<f64> = Vec::with_capacity(queries.len());

    let total_start = Instant::now();
    for q in queries {
        let t0 = Instant::now();
        let result = retriever.retrieve(corpus, q);
        let elapsed = t0.elapsed().as_micros() as f64;
        latencies_us.push(elapsed);
        precisions.push(result.precision(corpus, q) as f64);
        budgets.push(result.budget_utilisation as f64);
    }
    let total_elapsed = total_start.elapsed().as_secs_f64();

    latencies_us.sort_by(|a, b| a.partial_cmp(b).unwrap());
    let n = latencies_us.len();
    let mean_us = latencies_us.iter().sum::<f64>() / n as f64;
    let p50_us = latencies_us[n / 2];
    let p95_us = latencies_us[(n * 95 / 100).min(n - 1)];
    let throughput_qps = queries.len() as f64 / total_elapsed;
    let mean_precision = precisions.iter().sum::<f64>() / n as f64;
    let mean_budget_util = budgets.iter().sum::<f64>() / n as f64;

    Stats {
        mean_us,
        p50_us,
        p95_us,
        throughput_qps,
        mean_precision,
        mean_budget_util,
    }
}

fn print_header() {
    println!();
    println!("╔══════════════════════════════════════════════════════════════════════════════╗");
    println!("║          ruvector-bounded-rag: MinCut Context Window Benchmark             ║");
    println!("╚══════════════════════════════════════════════════════════════════════════════╝");
    println!();

    // Print system info
    println!("Platform : {}", std::env::consts::OS);
    println!("Arch     : {}", std::env::consts::ARCH);
    println!("Rust     : {}", env!("CARGO_PKG_RUST_VERSION", "unknown"));
    println!();
}

fn print_case_header(case: &BenchCase) {
    println!("─────────────────────────────────────────────────────────────────────────────");
    println!(
        "Dataset  : {} chunks × {}d, {} clusters, {} queries, budget={}",
        case.n_chunks, case.dim, case.n_clusters, case.n_queries, case.budget
    );
    println!(
        "Config   : edge_threshold={:.2}  seed_threshold={:.2}",
        case.edge_threshold, case.seed_threshold
    );
    println!("─────────────────────────────────────────────────────────────────────────────");
    println!(
        "{:<20} {:>10} {:>10} {:>10} {:>12} {:>10} {:>12}",
        "Variant", "Mean(μs)", "p50(μs)", "p95(μs)", "QPS", "Precision", "BudgetUtil"
    );
    println!("{}", "─".repeat(90));
}

fn print_row(name: &str, s: &Stats) {
    println!(
        "{:<20} {:>10.1} {:>10.1} {:>10.1} {:>12.0} {:>9.3} {:>12.3}",
        name, s.mean_us, s.p50_us, s.p95_us, s.throughput_qps, s.mean_precision, s.mean_budget_util
    );
}

fn run_case(case: &BenchCase) {
    let mut rng = StdRng::seed_from_u64(0xDEAD_BEEF);
    let (corpus, queries) = build_corpus(case, &mut rng);

    let cfg = RetrieverConfig {
        budget: case.budget,
        edge_threshold: case.edge_threshold,
        seed_threshold: case.seed_threshold,
    };

    print_case_header(case);

    let topk = TopKRetriever::new(cfg.clone());
    let bfs = GraphBfsRetriever::new(cfg.clone());
    let mc = MinCutRetriever::new(cfg.clone());

    let s_topk = run_variant(&topk, &corpus, &queries);
    let s_bfs = run_variant(&bfs, &corpus, &queries);
    let s_mc = run_variant(&mc, &corpus, &queries);

    print_row("TopK (baseline)", &s_topk);
    print_row("GraphBFS", &s_bfs);
    print_row("MinCutBounded", &s_mc);

    println!();

    // Acceptance check
    let threshold = 0.70;
    let pass = s_topk.mean_precision >= threshold
        && s_bfs.mean_precision >= threshold
        && s_mc.mean_precision >= threshold;

    if pass {
        println!(
            "✓ PASS — all variants achieved precision >= {threshold:.2} \
             (TopK={:.3}, BFS={:.3}, MinCut={:.3})",
            s_topk.mean_precision, s_bfs.mean_precision, s_mc.mean_precision
        );
    } else {
        println!(
            "✗ FAIL — precision below {threshold:.2} threshold \
             (TopK={:.3}, BFS={:.3}, MinCut={:.3})",
            s_topk.mean_precision, s_bfs.mean_precision, s_mc.mean_precision
        );
    }

    // Memory estimate (rough)
    let chunk_bytes = case.n_chunks * case.dim * 4;
    let graph_edges = case.n_chunks * case.n_chunks / 2; // upper bound
    let graph_bytes = graph_edges * 8; // (usize, f32)
    println!(
        "Memory   : chunks≈{}KB  graph≈{}KB (upper bound)",
        chunk_bytes / 1024,
        graph_bytes / 1024
    );
    println!();
}

fn main() {
    print_header();

    // Small case: 200 chunks, quick sanity
    run_case(&BenchCase {
        n_chunks: 200,
        n_queries: 50,
        dim: 64,
        n_clusters: 4,
        budget: 20,
        edge_threshold: 0.70,
        seed_threshold: 0.45,
    });

    // Medium case: 1000 chunks
    run_case(&BenchCase {
        n_chunks: 1000,
        n_queries: 100,
        dim: 64,
        n_clusters: 5,
        budget: 30,
        edge_threshold: 0.70,
        seed_threshold: 0.45,
    });

    // Larger case: 3000 chunks (MinCut is O(VE) so we limit to 3k)
    run_case(&BenchCase {
        n_chunks: 3000,
        n_queries: 30,
        dim: 32,
        n_clusters: 6,
        budget: 40,
        edge_threshold: 0.72,
        seed_threshold: 0.45,
    });

    println!("═══════════════════════════════════════════════════════════════════════════════");
    println!("  Benchmark complete. All numbers from release build on this hardware.");
    println!("═══════════════════════════════════════════════════════════════════════════════");
}
