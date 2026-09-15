//! SIFT-1M benchmark binary that loads from fvecs/ivecs files directly.
//!
//! Does NOT require HDF5 headers — reads the raw TEXMEX fvecs format that is
//! already present in bench_data/sift/.
//!
//! Produces a recall@10 vs QPS sweep at multiple ef_search values so the
//! before/after PR #619 comparison can be run on both branches.
//!
//! Usage:
//!   cargo run --release -p ruvector-sota-bench --bin sift1m-bench -- \
//!       --data /path/to/bench_data/sift \
//!       [--corpus-limit N]  # default: 1_000_000
//!       [--query-limit  N]  # default: 10_000
//!       [--m M]             # default: 16
//!       [--ef-construction EC]  # default: 200
//!       [--k K]             # default: 10

use std::fs::File;
use std::io::{BufReader, Read};
use std::path::{Path, PathBuf};
use std::time::Instant;

use ruvector_core::{
    index::{hnsw::HnswIndex, VectorIndex},
    types::HnswConfig,
    DistanceMetric,
};

// ---------------------------------------------------------------------------
// fvecs / ivecs reader
// ---------------------------------------------------------------------------

/// Read an fvecs file: [d:u32, f[0]:f32, ..., f[d-1]:f32] × N
fn read_fvecs(path: &Path, max_vecs: usize) -> anyhow::Result<(Vec<Vec<f32>>, usize)> {
    let f = File::open(path).map_err(|e| anyhow::anyhow!("Cannot open {}: {e}", path.display()))?;
    let mut r = BufReader::new(f);

    let mut vecs: Vec<Vec<f32>> = Vec::new();
    let mut buf4 = [0u8; 4];
    let mut dims: usize = 0;

    loop {
        match r.read_exact(&mut buf4) {
            Ok(_) => {}
            Err(e) if e.kind() == std::io::ErrorKind::UnexpectedEof => break,
            Err(e) => return Err(e.into()),
        }
        let d = u32::from_le_bytes(buf4) as usize;
        if dims == 0 {
            dims = d;
        } else if d != dims {
            return Err(anyhow::anyhow!(
                "Inconsistent dimension in fvecs: expected {dims}, got {d}"
            ));
        }
        let mut v = vec![0f32; d];
        let byte_slice =
            unsafe { std::slice::from_raw_parts_mut(v.as_mut_ptr() as *mut u8, d * 4) };
        r.read_exact(byte_slice)?;
        vecs.push(v);
        if vecs.len() >= max_vecs {
            break;
        }
    }
    Ok((vecs, dims))
}

/// Read an ivecs file: [d:u32, i[0]:i32, ..., i[d-1]:i32] × N
fn read_ivecs(path: &Path, max_vecs: usize) -> anyhow::Result<Vec<Vec<u64>>> {
    let f = File::open(path).map_err(|e| anyhow::anyhow!("Cannot open {}: {e}", path.display()))?;
    let mut r = BufReader::new(f);

    let mut vecs: Vec<Vec<u64>> = Vec::new();
    let mut buf4 = [0u8; 4];
    let mut dims: usize = 0;

    loop {
        match r.read_exact(&mut buf4) {
            Ok(_) => {}
            Err(e) if e.kind() == std::io::ErrorKind::UnexpectedEof => break,
            Err(e) => return Err(e.into()),
        }
        let d = u32::from_le_bytes(buf4) as usize;
        if dims == 0 {
            dims = d;
        } else if d != dims {
            return Err(anyhow::anyhow!(
                "Inconsistent dimension in ivecs: expected {dims}, got {d}"
            ));
        }
        let mut v = vec![0u64; d];
        for val in v.iter_mut() {
            r.read_exact(&mut buf4)?;
            *val = i32::from_le_bytes(buf4) as u64;
        }
        vecs.push(v);
        if vecs.len() >= max_vecs {
            break;
        }
    }
    Ok(vecs)
}

// ---------------------------------------------------------------------------
// recall@k
// ---------------------------------------------------------------------------

fn recall_at_k(result_ids: &[u64], ground_truth: &[u64], k: usize) -> f64 {
    use std::collections::HashSet;
    let gt: HashSet<u64> = ground_truth.iter().take(k).cloned().collect();
    let res: HashSet<u64> = result_ids.iter().take(k).cloned().collect();
    let hits = gt.intersection(&res).count();
    hits as f64 / k.min(gt.len()) as f64
}

// ---------------------------------------------------------------------------
// CLI args
// ---------------------------------------------------------------------------

struct Args {
    data_dir: PathBuf,
    corpus_limit: usize,
    query_limit: usize,
    m: usize,
    ef_construction: usize,
    k: usize,
    ef_values: Vec<usize>,
}

fn parse_args() -> Args {
    let args: Vec<String> = std::env::args().collect();
    let mut data_dir = PathBuf::from("bench_data/sift");
    let mut corpus_limit: usize = 1_000_000;
    let mut query_limit: usize = 10_000;
    let mut m: usize = 16;
    let mut ef_construction: usize = 200;
    let mut k: usize = 10;
    let mut ef_values: Vec<usize> = vec![20, 40, 80, 100, 200, 400];

    let mut i = 1;
    while i < args.len() {
        match args[i].as_str() {
            "--data" => {
                i += 1;
                data_dir = PathBuf::from(&args[i]);
            }
            "--corpus-limit" => {
                i += 1;
                corpus_limit = args[i].parse().expect("corpus-limit must be a number");
            }
            "--query-limit" => {
                i += 1;
                query_limit = args[i].parse().expect("query-limit must be a number");
            }
            "--m" => {
                i += 1;
                m = args[i].parse().expect("m must be a number");
            }
            "--ef-construction" => {
                i += 1;
                ef_construction = args[i].parse().expect("ef-construction must be a number");
            }
            "--k" => {
                i += 1;
                k = args[i].parse().expect("k must be a number");
            }
            "--ef" => {
                i += 1;
                ef_values = args[i]
                    .split(',')
                    .map(|s| s.trim().parse::<usize>().expect("ef must be a number"))
                    .collect();
            }
            other => {
                eprintln!("Unknown argument: {other}");
            }
        }
        i += 1;
    }

    Args {
        data_dir,
        corpus_limit,
        query_limit,
        m,
        ef_construction,
        k,
        ef_values,
    }
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

fn main() -> anyhow::Result<()> {
    let args = parse_args();

    println!("=== SIFT-1M HNSW Benchmark (ruvector-core) ===");
    println!("Data dir   : {}", args.data_dir.display());
    println!("Corpus cap : {}", args.corpus_limit);
    println!("Query  cap : {}", args.query_limit);
    println!("M          : {}", args.m);
    println!("efConstruct: {}", args.ef_construction);
    println!("k          : {}", args.k);
    println!("ef sweep   : {:?}", args.ef_values);
    println!();

    // ── Load data ──────────────────────────────────────────────────────────
    print!("Loading corpus... ");
    let t0 = Instant::now();
    let (corpus, dims) = read_fvecs(&args.data_dir.join("sift_base.fvecs"), args.corpus_limit)?;
    println!(
        "{} vectors × {}d  ({:.1}s)",
        corpus.len(),
        dims,
        t0.elapsed().as_secs_f64()
    );

    print!("Loading queries... ");
    let t1 = Instant::now();
    let (queries, qdims) = read_fvecs(&args.data_dir.join("sift_query.fvecs"), args.query_limit)?;
    println!(
        "{} vectors × {}d  ({:.1}s)",
        queries.len(),
        qdims,
        t1.elapsed().as_secs_f64()
    );
    assert_eq!(dims, qdims, "Query and corpus dims must match");

    print!("Loading ground truth... ");
    let t2 = Instant::now();
    let ground_truth = read_ivecs(
        &args.data_dir.join("sift_groundtruth.ivecs"),
        args.query_limit,
    )?;
    println!(
        "{} lists  ({:.1}s)",
        ground_truth.len(),
        t2.elapsed().as_secs_f64()
    );
    assert_eq!(
        ground_truth.len(),
        queries.len(),
        "Ground truth count must match query count"
    );

    // ── Build HNSW index ──────────────────────────────────────────────────
    println!();
    println!(
        "Building HNSW index (M={}, efC={})...",
        args.m, args.ef_construction
    );
    let cfg = HnswConfig {
        m: args.m,
        ef_construction: args.ef_construction,
        ef_search: args.ef_values[0],
        ..Default::default()
    };

    let t_build = Instant::now();
    let mut idx = HnswIndex::new(dims, DistanceMetric::Euclidean, cfg)
        .map_err(|e| anyhow::anyhow!("HnswIndex::new: {e}"))?;

    for (i, v) in corpus.iter().enumerate() {
        idx.add(i.to_string(), v.clone())
            .map_err(|e| anyhow::anyhow!("HnswIndex::add {i}: {e}"))?;
        if i > 0 && i % 100_000 == 0 {
            let elapsed = t_build.elapsed().as_secs_f64();
            let rate = i as f64 / elapsed;
            println!("  Inserted {i} ({rate:.0} vec/s, {elapsed:.1}s elapsed)");
        }
    }
    let build_secs = t_build.elapsed().as_secs_f64();
    let build_rate = corpus.len() as f64 / build_secs;
    println!(
        "Build done: {:.1}s  ({build_rate:.0} vec/s, {:.0} MB estimated)",
        build_secs,
        (corpus.len() * dims * 4) as f64 / (1024.0 * 1024.0) * 1.5,
    );

    // ── ef_search sweep ────────────────────────────────────────────────────
    println!();
    println!(
        "{:<8}  {:>10}  {:>10}  {:>12}  {:>10}",
        "ef", "recall@10", "QPS", "p50_us", "p99_us"
    );
    println!("{}", "-".repeat(58));

    for ef in &args.ef_values {
        let ef = *ef;
        // Fetch exactly k results — don't over-fetch or the search_with_ef
        // clamp (effective_ef = ef_search.max(k)) will dominate at small ef.
        let fetch_k = args.k;
        let mut latencies_ns: Vec<u128> = Vec::with_capacity(queries.len());
        let mut recall_sum = 0.0f64;

        for (qi, q) in queries.iter().enumerate() {
            let t = Instant::now();
            let results = idx
                .search_with_ef(q, fetch_k, ef)
                .map_err(|e| anyhow::anyhow!("search_with_ef: {e}"))?;
            latencies_ns.push(t.elapsed().as_nanos());

            let ids: Vec<u64> = results
                .iter()
                .filter_map(|r| r.id.parse::<u64>().ok())
                .collect();
            recall_sum += recall_at_k(&ids, &ground_truth[qi], args.k);
        }

        let n_q = queries.len() as f64;
        let mean_recall = recall_sum / n_q;
        let total_s = latencies_ns.iter().sum::<u128>() as f64 / 1e9;
        let qps = n_q / total_s;

        let mut sorted = latencies_ns.clone();
        sorted.sort_unstable();
        let p50_us = sorted[(sorted.len() as f64 * 0.50) as usize] as f64 / 1000.0;
        let p99_us = sorted[(sorted.len() as f64 * 0.99) as usize] as f64 / 1000.0;

        println!(
            "{:<8}  {:>10.4}  {:>10.0}  {:>12.1}  {:>10.1}",
            ef, mean_recall, qps, p50_us, p99_us,
        );
    }

    println!();
    println!("Done.");
    Ok(())
}
