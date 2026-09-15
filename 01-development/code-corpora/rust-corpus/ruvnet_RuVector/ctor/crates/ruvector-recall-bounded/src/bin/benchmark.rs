use ruvector_recall_bounded::{
    gen_dataset, recall, Entry, HnswBeamSearch, LinearScan, RecallBoundedIndex, ThresholdBeam,
};
use std::time::Instant;

fn percentile(mut v: Vec<u128>, pct: f64) -> u128 {
    v.sort_unstable();
    let idx = ((pct / 100.0) * (v.len() as f64 - 1.0)).round() as usize;
    v[idx.min(v.len() - 1)]
}

struct BenchResult {
    name: &'static str,
    mean_us: f64,
    p50_us: u128,
    p95_us: u128,
    throughput_qps: f64,
    memory_mb: f64,
    mean_hits: f64,
    recall: f64,
    pass: bool,
}

fn bench_variant<I: RecallBoundedIndex>(
    name: &'static str,
    idx: I,
    queries: &[Entry],
    ground_truths: &[Vec<ruvector_recall_bounded::Hit>],
    threshold: f32,
    recall_threshold: f32,
) -> BenchResult {
    let memory_mb = idx.memory_bytes() as f64 / (1024.0 * 1024.0);
    let n_queries = queries.len();
    let mut latencies_us: Vec<u128> = Vec::with_capacity(n_queries);
    let mut total_hits: usize = 0;
    let mut total_recall: f64 = 0.0;

    for (q, gt) in queries.iter().zip(ground_truths.iter()) {
        let t0 = Instant::now();
        let hits = idx.search(&q.vec, threshold);
        let elapsed = t0.elapsed().as_micros();
        latencies_us.push(elapsed);
        total_hits += hits.len();
        total_recall += recall(&hits, gt) as f64;
    }

    let mean_us = latencies_us.iter().sum::<u128>() as f64 / n_queries as f64;
    let p50_us = percentile(latencies_us.clone(), 50.0);
    let p95_us = percentile(latencies_us, 95.0);
    let throughput_qps = 1_000_000.0 / mean_us;
    let mean_recall = total_recall / n_queries as f64;
    let mean_hits = total_hits as f64 / n_queries as f64;

    BenchResult {
        name,
        mean_us,
        p50_us,
        p95_us,
        throughput_qps,
        memory_mb,
        mean_hits,
        recall: mean_recall,
        pass: mean_recall >= recall_threshold as f64,
    }
}

fn main() {
    // ── Config ────────────────────────────────────────────────────────────────
    // Defaults chosen so random unit-vector hits are ~1-3% of corpus:
    //   dim=32  → cosine-sim stddev = 1/√32 ≈ 0.177
    //   θ=0.40  → P(sim ≥ 0.40) ≈ 1.2 %  → ~24 hits/query at n=2000
    // Override with N_VECTORS, DIM, N_QUERIES, THRESHOLD env vars.
    let n_vectors: usize = std::env::var("N_VECTORS")
        .ok()
        .and_then(|v| v.parse().ok())
        .unwrap_or(2_000);
    let dim: usize = std::env::var("DIM")
        .ok()
        .and_then(|v| v.parse().ok())
        .unwrap_or(32);
    let n_queries: usize = std::env::var("N_QUERIES")
        .ok()
        .and_then(|v| v.parse().ok())
        .unwrap_or(100);
    let threshold: f32 = std::env::var("THRESHOLD")
        .ok()
        .and_then(|v| v.parse().ok())
        .unwrap_or(0.40_f32);
    let m: usize = 16;
    let ef_base: usize = 64;
    let beam_width: usize = 32;
    let recall_acceptance: f32 = 0.80;

    // ── System info ───────────────────────────────────────────────────────────
    println!("╔══════════════════════════════════════════════════════════════════╗");
    println!("║        RuVector Recall-Bounded ANN Benchmark                    ║");
    println!("╚══════════════════════════════════════════════════════════════════╝");
    println!();
    println!("Platform : {}", std::env::consts::OS);
    println!("Arch     : {}", std::env::consts::ARCH);
    println!(
        "Rust     : compiled with {}",
        option_env!("RUSTC_BOOTSTRAP").unwrap_or("stable")
    );
    println!("Vectors  : {n_vectors}");
    println!("Dims     : {dim}");
    println!("Queries  : {n_queries}");
    println!("Threshold: {threshold:.3}");
    println!("M (graph): {m}");
    println!("ef_base  : {ef_base}");
    println!("Beam W   : {beam_width}");
    println!("Recall ≥  : {recall_acceptance:.2} (acceptance)");
    println!();

    // ── Build dataset ─────────────────────────────────────────────────────────
    println!("Building dataset ...");
    let t0 = Instant::now();
    let corpus = gen_dataset(n_vectors, dim, 0xDEAD_BEEF);
    println!("  corpus built in {:.1}ms", t0.elapsed().as_millis() as f64);

    let queries = gen_dataset(n_queries, dim, 0xCAFE_BABE);

    // ── Build indexes ─────────────────────────────────────────────────────────
    println!("Building LinearScan ...");
    let t0 = Instant::now();
    let mut ls = LinearScan::new();
    for e in &corpus {
        ls.insert(e.clone());
    }
    println!("  {:.1}ms", t0.elapsed().as_millis() as f64);

    println!("Building HnswBeam (m={m}, ef={ef_base}) ...");
    let t0 = Instant::now();
    let mut hb = HnswBeamSearch::new(m, ef_base);
    for e in &corpus {
        hb.insert(e.clone());
    }
    println!("  {:.1}ms", t0.elapsed().as_millis() as f64);

    println!("Building ThresholdBeam (m={m}, beam={beam_width}) ...");
    let t0 = Instant::now();
    let mut tb = ThresholdBeam::new(m, beam_width);
    for e in &corpus {
        tb.insert(e.clone());
    }
    println!("  {:.1}ms", t0.elapsed().as_millis() as f64);
    println!();

    // ── Compute ground truth ──────────────────────────────────────────────────
    println!("Computing ground truth ...");
    let t0 = Instant::now();
    let ground_truths: Vec<_> = queries
        .iter()
        .map(|q| ls.search(&q.vec, threshold))
        .collect();
    println!(
        "  done in {:.1}ms — mean {:.1} hits/query above threshold {threshold:.3}",
        t0.elapsed().as_millis() as f64,
        ground_truths.iter().map(|g| g.len()).sum::<usize>() as f64 / n_queries as f64
    );
    println!();

    // ── Run benchmarks ────────────────────────────────────────────────────────
    println!("Running benchmarks (each query timed individually) ...\n");

    let r_ls = bench_variant(
        "LinearScan (exact)",
        LinearScan::new().build_from(&corpus),
        &queries,
        &ground_truths,
        threshold,
        1.0, // exact: recall must be 1.0
    );
    let r_hb = bench_variant(
        "HnswBeam (approx)",
        HnswBeamSearch::new(m, ef_base).build_from(&corpus),
        &queries,
        &ground_truths,
        threshold,
        recall_acceptance,
    );
    let r_tb = bench_variant(
        "ThresholdBeam (approx)",
        ThresholdBeam::new(m, beam_width).build_from(&corpus),
        &queries,
        &ground_truths,
        threshold,
        recall_acceptance,
    );

    // ── Print results table ───────────────────────────────────────────────────
    println!("┌─────────────────────────┬──────────┬──────────┬──────────┬──────────┬──────────┬──────────┬──────────┬────────┐");
    println!("│ Variant                 │ Mean(μs) │ p50(μs)  │ p95(μs)  │ QPS      │ Mem(MB)  │ Hits/q   │ Recall   │ PASS   │");
    println!("├─────────────────────────┼──────────┼──────────┼──────────┼──────────┼──────────┼──────────┼──────────┼────────┤");
    for r in &[&r_ls, &r_hb, &r_tb] {
        println!(
            "│ {:<23} │ {:>8.1} │ {:>8} │ {:>8} │ {:>8.0} │ {:>8.2} │ {:>8.1} │ {:>8.3} │ {:<6} │",
            r.name,
            r.mean_us,
            r.p50_us,
            r.p95_us,
            r.throughput_qps,
            r.memory_mb,
            r.mean_hits,
            r.recall,
            if r.pass { "PASS" } else { "FAIL" }
        );
    }
    println!("└─────────────────────────┴──────────┴──────────┴──────────┴──────────┴──────────┴──────────┴──────────┴────────┘");
    println!();

    // ── Memory comparison ─────────────────────────────────────────────────────
    println!("Memory breakdown:");
    let raw_vec_mb = (n_vectors * dim * 4) as f64 / (1024.0 * 1024.0);
    println!("  Raw vectors only  : {raw_vec_mb:.2} MB");
    println!(
        "  LinearScan        : {:.2} MB  ({:.1}× overhead)",
        r_ls.memory_mb,
        r_ls.memory_mb / raw_vec_mb
    );
    println!(
        "  HnswBeam          : {:.2} MB  ({:.1}× overhead)",
        r_hb.memory_mb,
        r_hb.memory_mb / raw_vec_mb
    );
    println!(
        "  ThresholdBeam     : {:.2} MB  ({:.1}× overhead)",
        r_tb.memory_mb,
        r_tb.memory_mb / raw_vec_mb
    );
    println!();

    // ── Speed comparison ──────────────────────────────────────────────────────
    println!("Speed comparison (relative to LinearScan):");
    println!("  HnswBeam   : {:.1}×", r_ls.mean_us / r_hb.mean_us);
    println!("  ThresholdBeam: {:.1}×", r_ls.mean_us / r_tb.mean_us);
    println!();

    // ── Acceptance ────────────────────────────────────────────────────────────
    let all_pass = r_ls.pass && r_hb.pass && r_tb.pass;
    if all_pass {
        println!("✓ ACCEPTANCE: All variants meet recall ≥ {recall_acceptance:.2} at threshold {threshold:.3}");
    } else {
        eprintln!("✗ ACCEPTANCE FAILED:");
        if !r_ls.pass {
            eprintln!("  LinearScan recall={:.3}", r_ls.recall);
        }
        if !r_hb.pass {
            eprintln!("  HnswBeam recall={:.3}", r_hb.recall);
        }
        if !r_tb.pass {
            eprintln!("  ThresholdBeam recall={:.3}", r_tb.recall);
        }
        std::process::exit(1);
    }
}

// ── Helper trait to build an index from a corpus slice ───────────────────────

trait BuildFrom: RecallBoundedIndex + Sized {
    fn build_from(mut self, corpus: &[Entry]) -> Self {
        for e in corpus {
            self.insert(e.clone());
        }
        self
    }
}

impl BuildFrom for LinearScan {}
impl BuildFrom for HnswBeamSearch {}
impl BuildFrom for ThresholdBeam {}
