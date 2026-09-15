//! Speculative ANN Search — benchmark binary.
//!
//! Measures three variants on a synthetic 128-dim Gaussian dataset:
//!   1. LinearFull      – brute-force f32 scan (ground truth)
//!   2. QuantizedDraft  – brute-force u8 SQ scan (fast, lossy)
//!   3. SpeculativeANN  – u8 draft k' candidates + f32 exact re-rank
//!
//! Run:
//!   cargo run --release -p ruvector-speculative-ann --bin benchmark
//!
//! To change dataset size, set env vars:
//!   N_VECS=50000 N_QUERIES=1000 DIMS=256 cargo run --release ...

use ruvector_speculative_ann::{
    dataset::{Dataset, DatasetConfig},
    linear_full::LinearFull,
    quantized_draft::QuantizedDraft,
    recall_at_k,
    speculative::{SpecConfig, SpeculativeANN},
    AnnVariant,
};
use std::time::Instant;

// ─── benchmark parameters ────────────────────────────────────────────────────

fn n_vecs() -> usize {
    std::env::var("N_VECS")
        .ok()
        .and_then(|v| v.parse().ok())
        .unwrap_or(10_000)
}
fn n_queries() -> usize {
    std::env::var("N_QUERIES")
        .ok()
        .and_then(|v| v.parse().ok())
        .unwrap_or(500)
}
fn dims() -> usize {
    std::env::var("DIMS")
        .ok()
        .and_then(|v| v.parse().ok())
        .unwrap_or(128)
}

const K: usize = 10;
const SEED: u64 = 0xC0DE_CAFE_BABE_7777;

// ─── acceptance thresholds ───────────────────────────────────────────────────

const MIN_RECALL_QUANTIZED: f32 = 0.70; // draft only — lossy by design
const MIN_RECALL_SPECULATIVE: f32 = 0.92; // draft + verify — near-perfect
const MIN_QPS_QUANTIZED: f64 = 1_000.0; // faster than LinearFull on this hw
const MIN_QPS_LINEAR: f64 = 100.0; // minimum baseline

// ─── stat helpers ────────────────────────────────────────────────────────────

fn percentile(sorted: &[u128], p: f64) -> u128 {
    if sorted.is_empty() {
        return 0;
    }
    let idx = ((sorted.len() as f64 * p / 100.0) as usize).min(sorted.len() - 1);
    sorted[idx]
}

fn mean_us(times_ns: &[u128]) -> f64 {
    times_ns.iter().sum::<u128>() as f64 / times_ns.len() as f64 / 1_000.0
}

fn qps(times_ns: &[u128]) -> f64 {
    let total_s = times_ns.iter().sum::<u128>() as f64 / 1e9;
    times_ns.len() as f64 / total_s
}

// ─── print helpers ───────────────────────────────────────────────────────────

fn print_header(n: usize, d: usize, q: usize) {
    println!("╔══════════════════════════════════════════════════════════════════╗");
    println!("║       RuVector Speculative ANN Benchmark                        ║");
    println!("╠══════════════════════════════════════════════════════════════════╣");

    let info = [
        ("OS", std::env::consts::OS.to_string()),
        ("Arch", std::env::consts::ARCH.to_string()),
        ("Corpus", format!("{n} vectors × {d} dims")),
        ("Queries", q.to_string()),
        ("k", K.to_string()),
        (
            "f32 memory (full)",
            format!("{:.1} MB", n * d * 4 / 1_048_576),
        ),
        ("u8 memory (draft)", format!("{:.1} MB", n * d / 1_048_576)),
    ];
    for (k, v) in &info {
        println!("║  {k:<26} {v:<38}║");
    }
    println!("╚══════════════════════════════════════════════════════════════════╝");
    println!();
}

// ─── run one variant ─────────────────────────────────────────────────────────

struct RunResult {
    name: String,
    recall: f32,
    mean_us: f64,
    p50_us: u128,
    p95_us: u128,
    qps: f64,
    mem_mb: f64,
    extra: String,
}

fn print_row(result: &RunResult) {
    println!(
        "  {:<18} recall={:.3}  mean={:>7.1}µs  p50={:>6}µs  \
         p95={:>7}µs  {:>8.0} q/s  mem={:>5.1}MB  {}",
        result.name,
        result.recall,
        result.mean_us,
        result.p50_us,
        result.p95_us,
        result.qps,
        result.mem_mb,
        result.extra,
    );
}

fn run_linear_full(ds: &Dataset, ground_truth: &[Vec<ruvector_speculative_ann::Hit>]) -> RunResult {
    let idx = LinearFull::build(ds.vectors.clone());
    let mem_mb = idx.memory_bytes() as f64 / 1_048_576.0;

    let mut times = Vec::with_capacity(ds.queries.len());
    for q in &ds.queries {
        let t0 = Instant::now();
        let _ = idx.search(q, K);
        times.push(t0.elapsed().as_nanos());
    }
    let mut sorted = times.clone();
    sorted.sort_unstable();

    // Recall is 1.0 by definition (ground truth = LinearFull).
    RunResult {
        name: "LinearFull".into(),
        recall: 1.000,
        mean_us: mean_us(&times),
        p50_us: percentile(&sorted, 50.0) / 1_000,
        p95_us: percentile(&sorted, 95.0) / 1_000,
        qps: qps(&times),
        mem_mb,
        extra: "(ground truth)".into(),
    }
}

fn run_quantized_draft(
    ds: &Dataset,
    ground_truth: &[Vec<ruvector_speculative_ann::Hit>],
) -> RunResult {
    let idx = QuantizedDraft::build(&ds.vectors);
    let mem_mb = idx.memory_bytes() as f64 / 1_048_576.0;

    let mut times = Vec::with_capacity(ds.queries.len());
    let mut total_recall = 0.0f32;
    for (i, q) in ds.queries.iter().enumerate() {
        let t0 = Instant::now();
        let res = idx.search(q, K);
        times.push(t0.elapsed().as_nanos());
        total_recall += recall_at_k(&res, &ground_truth[i], K);
    }
    let mut sorted = times.clone();
    sorted.sort_unstable();
    let mean_recall = total_recall / ds.queries.len() as f32;

    RunResult {
        name: "QuantizedDraft".into(),
        recall: mean_recall,
        mean_us: mean_us(&times),
        p50_us: percentile(&sorted, 50.0) / 1_000,
        p95_us: percentile(&sorted, 95.0) / 1_000,
        qps: qps(&times),
        mem_mb,
        extra: format!("SQ u8, k'=k={K}"),
    }
}

struct MultResult {
    mult: usize,
    recall: f32,
    mean_us: f64,
    p50_us: u128,
    p95_us: u128,
    qps: f64,
    acceptance_rate: f32,
}

fn run_speculative_fixed_mults(
    ds: &Dataset,
    ground_truth: &[Vec<ruvector_speculative_ann::Hit>],
) -> Vec<MultResult> {
    let mults = [1usize, 2, 3, 5, 8, 12];
    let spec = SpeculativeANN::build(
        ds.vectors.clone(),
        SpecConfig {
            initial_mult: 3,
            ..Default::default()
        },
    );

    let mut results = Vec::new();
    for &mult in &mults {
        let k_prime = K * mult;
        let mut times = Vec::with_capacity(ds.queries.len());
        let mut total_recall = 0.0f32;
        let mut accepted = 0usize;

        for (i, q) in ds.queries.iter().enumerate() {
            let t0 = Instant::now();
            let res = spec.search_fixed(q, K, mult);
            times.push(t0.elapsed().as_nanos());
            let r = recall_at_k(&res, &ground_truth[i], K);
            total_recall += r;
            if r >= 0.90 {
                accepted += 1;
            }
        }

        let mut sorted = times.clone();
        sorted.sort_unstable();
        let mean_recall = total_recall / ds.queries.len() as f32;
        let acceptance_rate = accepted as f32 / ds.queries.len() as f32;

        results.push(MultResult {
            mult,
            recall: mean_recall,
            mean_us: mean_us(&times),
            p50_us: percentile(&sorted, 50.0) / 1_000,
            p95_us: percentile(&sorted, 95.0) / 1_000,
            qps: qps(&times),
            acceptance_rate,
        });
    }
    results
}

fn run_speculative_adaptive(
    ds: &Dataset,
    ground_truth: &[Vec<ruvector_speculative_ann::Hit>],
) -> RunResult {
    let mut spec = SpeculativeANN::build(
        ds.vectors.clone(),
        SpecConfig {
            initial_mult: 2,
            target_recall: 0.95,
            ..Default::default()
        },
    );
    let mem_mb = spec.memory_bytes() as f64 / 1_048_576.0;

    let mut times = Vec::with_capacity(ds.queries.len());
    let mut total_recall = 0.0f32;

    for (i, q) in ds.queries.iter().enumerate() {
        let t0 = Instant::now();
        let res = spec.search_adaptive(q, K, Some(&ground_truth[i]));
        times.push(t0.elapsed().as_nanos());
        total_recall += recall_at_k(&res, &ground_truth[i], K);
    }

    let mut sorted = times.clone();
    sorted.sort_unstable();
    let mean_recall = total_recall / ds.queries.len() as f32;
    let final_mult = spec.current_mult();
    let acc_rate = spec.acceptance_rate();

    RunResult {
        name: "SpeculativeANN".into(),
        recall: mean_recall,
        mean_us: mean_us(&times),
        p50_us: percentile(&sorted, 50.0) / 1_000,
        p95_us: percentile(&sorted, 95.0) / 1_000,
        qps: qps(&times),
        mem_mb,
        extra: format!("adaptive mult={final_mult} acc={acc_rate:.2}"),
    }
}

// ─── acceptance tests ────────────────────────────────────────────────────────

fn check(label: &str, ok: bool) -> bool {
    let marker = if ok { "PASS ✓" } else { "FAIL ✗" };
    println!("  [{marker}] {label}");
    ok
}

// ─── main ────────────────────────────────────────────────────────────────────

fn main() {
    let n = n_vecs();
    let d = dims();
    let q_count = n_queries();

    print_header(n, d, q_count);

    println!("Building dataset (n={n}, d={d}, q={q_count}, seed=0x{SEED:016X})...");
    let ds = Dataset::generate(DatasetConfig {
        n_vectors: n,
        dims: d,
        n_queries: q_count,
        seed: SEED,
    });

    println!("Computing ground truth (LinearFull)...");
    let baseline = LinearFull::build(ds.vectors.clone());
    let ground_truth: Vec<_> = ds.queries.iter().map(|q| baseline.search(q, K)).collect();

    // ── primary runs ─────────────────────────────────────────────────────────
    println!("\nRunning variants...\n");

    let r_full = run_linear_full(&ds, &ground_truth);
    let r_draft = run_quantized_draft(&ds, &ground_truth);
    let r_spec = run_speculative_adaptive(&ds, &ground_truth);

    // ── k' sweep table ────────────────────────────────────────────────────────
    println!("─── SpeculativeANN: candidate multiplier sweep ───────────────────────────────");
    println!(
        "  {:<8} {:<8} {:<12} {:<10} {:<10} {:<10} {:<12}",
        "k'", "mult", "recall@10", "mean(µs)", "p50(µs)", "p95(µs)", "accept-rate"
    );
    let mults_data = run_speculative_fixed_mults(&ds, &ground_truth);
    for m in &mults_data {
        println!(
            "  {:<8} {:<8} {:<12.3} {:<10.1} {:<10} {:<10} {:<12.3}",
            K * m.mult,
            m.mult,
            m.recall,
            m.mean_us,
            m.p50_us,
            m.p95_us,
            m.acceptance_rate
        );
    }
    println!();

    // ── summary table ─────────────────────────────────────────────────────────
    println!("─── Summary ─────────────────────────────────────────────────────────────────");
    for r in [&r_full, &r_draft, &r_spec] {
        print_row(r);
    }
    println!();

    // ── memory math ───────────────────────────────────────────────────────────
    let f32_mb = n * d * 4 / 1_048_576;
    let u8_mb = n * d / 1_048_576;
    let spec_mb = f32_mb + u8_mb; // verify store + draft store
    println!("─── Memory math ─────────────────────────────────────────────────────────────");
    println!(
        "  LinearFull  f32 store: {f32_mb} MB  ({} bytes/vector)",
        d * 4
    );
    println!(
        "  DraftIndex  u8  store: {u8_mb} MB  ({} bytes/vector) — 4× compression",
        d
    );
    println!("  SpecANN both stores:   {spec_mb} MB  (1.25× LinearFull overhead)");
    println!("  u8 scan cost:          ~4× cheaper per distance op (integer arithmetic)");
    println!();

    // ── cost model ────────────────────────────────────────────────────────────
    let best_spec = mults_data
        .iter()
        .find(|m| m.recall >= 0.95)
        .map(|m| m.mult)
        .unwrap_or(12);
    println!("─── Cost model (n={n}, d={d}, k={K}) ─────────────────────────────────────────");
    println!("  LinearFull cost: O(n × d) = {}", n * d);
    println!("  QuantizedDraft:  O(n × d / 4) ≈ {}", n * d / 4);
    println!("  SpeculativeANN:  O(n × d / 4) + O(k' × d)");
    println!(
        "    with mult={best_spec}: O({}) + O({}) = O({})",
        n * d / 4,
        K * best_spec * d,
        n * d / 4 + K * best_spec * d
    );
    println!(
        "  Speedup (draft vs full): ~{:.1}×",
        r_full.mean_us / r_draft.mean_us.max(0.001)
    );
    println!(
        "  Speedup (spec vs full):  ~{:.1}×",
        r_full.mean_us / r_spec.mean_us.max(0.001)
    );
    println!();

    // ── acceptance tests ──────────────────────────────────────────────────────
    println!("─── Acceptance tests ────────────────────────────────────────────────────────");
    let mut all_pass = true;
    all_pass &= check(
        &format!(
            "QuantizedDraft recall@{K} >= {MIN_RECALL_QUANTIZED:.2} (got {:.3})",
            r_draft.recall
        ),
        r_draft.recall >= MIN_RECALL_QUANTIZED,
    );
    all_pass &= check(
        &format!(
            "SpeculativeANN recall@{K} >= {MIN_RECALL_SPECULATIVE:.2} (got {:.3})",
            r_spec.recall
        ),
        r_spec.recall >= MIN_RECALL_SPECULATIVE,
    );
    all_pass &= check(
        &format!(
            "QuantizedDraft QPS >= {MIN_QPS_QUANTIZED:.0} (got {:.0})",
            r_draft.qps
        ),
        r_draft.qps >= MIN_QPS_QUANTIZED,
    );
    all_pass &= check(
        &format!(
            "LinearFull QPS >= {MIN_QPS_LINEAR:.0} (got {:.0})",
            r_full.qps
        ),
        r_full.qps >= MIN_QPS_LINEAR,
    );
    all_pass &= check(
        &format!(
            "SpeculativeANN faster than LinearFull ({:.1}µs vs {:.1}µs mean)",
            r_spec.mean_us, r_full.mean_us
        ),
        r_spec.mean_us < r_full.mean_us,
    );
    all_pass &= check(
        &format!(
            "QuantizedDraft faster than LinearFull ({:.1}µs vs {:.1}µs mean)",
            r_draft.mean_us, r_full.mean_us
        ),
        r_draft.mean_us < r_full.mean_us,
    );

    println!();
    if all_pass {
        println!("ACCEPTANCE RESULT: PASS ✓ — all thresholds met");
    } else {
        println!("ACCEPTANCE RESULT: FAIL ✗ — one or more thresholds not met");
        std::process::exit(1);
    }
}
