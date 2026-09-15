//! A/B measurement harness for #674 (mmap read-through vector storage).
//!
//! Two phases, run as separate process invocations so `/usr/bin/time -l` (or any
//! external RSS sampler) captures one phase's peak RSS cleanly:
//!
//! ```text
//! cargo run --release --example mmap_674_bench -- build   --dir DIR --n 500000 --dim 128
//! cargo run --release --example mmap_674_bench -- measure --dir DIR --mode owned --dim 128
//! cargo run --release --example mmap_674_bench -- measure --dir DIR --mode mmap  --dim 128
//! ```
//!
//! `measure` loads the index (`DiskAnnIndex::load` for `owned`,
//! `DiskAnnIndex::load_mmap` for `mmap`), runs a warmup batch, then times a
//! measured batch and prints a single `RESULT ...` line with p50/p99 latency and
//! QPS. Peak RSS is read externally (this binary does not self-report memory —
//! `/usr/bin/time -l` on macOS or `/usr/bin/time -v` on Linux is the intended
//! wrapper) so the number reflects the whole process (load + queries), not just
//! an in-process allocator counter that would miss mmap'd pages.

use rand::rngs::StdRng;
use rand::{Rng, SeedableRng};
use ruvector_diskann::{DiskAnnConfig, DiskAnnIndex};
use std::hint::black_box;
use std::path::PathBuf;
use std::time::Instant;

fn random_unit_vector(rng: &mut StdRng, dim: usize) -> Vec<f32> {
    let mut v: Vec<f32> = (0..dim).map(|_| rng.gen_range(-1.0..1.0)).collect();
    let norm = v.iter().map(|x| x * x).sum::<f32>().sqrt();
    for x in &mut v {
        *x /= norm;
    }
    v
}

fn get_flag(args: &[String], name: &str, default: &str) -> String {
    for w in args.windows(2) {
        if w[0] == name {
            return w[1].clone();
        }
    }
    default.to_string()
}

fn main() {
    let args: Vec<String> = std::env::args().collect();
    let cmd = args.get(1).map(String::as_str).unwrap_or("");
    match cmd {
        "build" => cmd_build(&args[2..]),
        "measure" => cmd_measure(&args[2..]),
        _ => {
            eprintln!(
                "usage:\n  mmap_674_bench build   --dir DIR --n N --dim D [--max-degree R] [--build-beam L]\n  mmap_674_bench measure --dir DIR --mode owned|mmap --dim D [--queries Q] [--warmup W] [--k K]"
            );
            std::process::exit(2);
        }
    }
}

fn cmd_build(args: &[String]) {
    let dir = PathBuf::from(get_flag(args, "--dir", "/tmp/mmap674"));
    let n: usize = get_flag(args, "--n", "500000").parse().expect("--n");
    let dim: usize = get_flag(args, "--dim", "128").parse().expect("--dim");
    let max_degree: usize = get_flag(args, "--max-degree", "32")
        .parse()
        .expect("--max-degree");
    let build_beam: usize = get_flag(args, "--build-beam", "64")
        .parse()
        .expect("--build-beam");

    // Fixed seed so the fixture is reproducible across build/measure invocations
    // and across owned-vs-mmap comparison runs.
    let mut rng = StdRng::seed_from_u64(0x6D_6D_61_70_36_37_34);
    let mut index = DiskAnnIndex::new(DiskAnnConfig {
        dim,
        max_degree,
        build_beam,
        search_beam: 64,
        alpha: 1.2,
        storage_path: Some(dir.clone()),
        ..Default::default()
    });

    let t0 = Instant::now();
    for id in 0..n {
        index
            .insert(id.to_string(), random_unit_vector(&mut rng, dim))
            .expect("insert failed");
        if id % 50_000 == 0 {
            eprintln!(
                "inserted {id}/{n} ({:.1}s elapsed)",
                t0.elapsed().as_secs_f64()
            );
        }
    }
    eprintln!("insert phase done in {:.1}s", t0.elapsed().as_secs_f64());

    let t1 = Instant::now();
    index.build().expect("build failed"); // build() also calls save() since storage_path is set
    eprintln!(
        "build+save phase done in {:.1}s",
        t1.elapsed().as_secs_f64()
    );

    println!("BUILD_OK n={n} dim={dim} dir={}", dir.display());
}

fn cmd_measure(args: &[String]) {
    let dir = PathBuf::from(get_flag(args, "--dir", "/tmp/mmap674"));
    let mode = get_flag(args, "--mode", "owned");
    let dim: usize = get_flag(args, "--dim", "128").parse().expect("--dim");
    let queries: usize = get_flag(args, "--queries", "1000")
        .parse()
        .expect("--queries");
    let warmup: usize = get_flag(args, "--warmup", "50").parse().expect("--warmup");
    let k: usize = get_flag(args, "--k", "10").parse().expect("--k");

    let load_t0 = Instant::now();
    let index = match mode.as_str() {
        "mmap" => DiskAnnIndex::load_mmap(&dir).expect("load_mmap failed"),
        "owned" => DiskAnnIndex::load(&dir).expect("load failed"),
        other => {
            eprintln!("unknown --mode {other} (expected owned|mmap)");
            std::process::exit(2);
        }
    };
    let load_s = load_t0.elapsed().as_secs_f64();

    // Separate, fixed query seed (distinct from the build/data seed) — same
    // sequence for both owned and mmap runs so the two modes see identical query
    // traffic.
    let mut qrng = StdRng::seed_from_u64(0x71_75_65_72_79_36_37_34);
    let all_queries: Vec<Vec<f32>> = (0..(warmup + queries))
        .map(|_| random_unit_vector(&mut qrng, dim))
        .collect();

    for q in &all_queries[..warmup] {
        black_box(index.search(black_box(q), k).expect("warmup search failed"));
    }

    let mut lat_ns = Vec::with_capacity(queries);
    let run_t0 = Instant::now();
    for q in &all_queries[warmup..] {
        let t = Instant::now();
        black_box(index.search(black_box(q), k).expect("search failed"));
        lat_ns.push(t.elapsed().as_nanos() as u64);
    }
    let run_s = run_t0.elapsed().as_secs_f64();

    lat_ns.sort_unstable();
    let p50_us = lat_ns[queries / 2] as f64 / 1_000.0;
    let p99_us = lat_ns[(queries * 99 / 100).min(queries - 1)] as f64 / 1_000.0;
    let qps = queries as f64 / run_s;

    println!(
        "RESULT mode={mode} n={} dim={dim} k={k} queries={queries} warmup={warmup} load_s={load_s:.3} p50_us={p50_us:.3} p99_us={p99_us:.3} qps={qps:.3}",
        index.count()
    );
}
