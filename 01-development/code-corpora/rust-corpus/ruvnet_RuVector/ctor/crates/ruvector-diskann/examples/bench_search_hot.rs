use rand::rngs::StdRng;
use rand::{Rng, SeedableRng};
use ruvector_diskann::{DiskAnnConfig, DiskAnnIndex};
use std::hint::black_box;
use std::time::Instant;

const DIM: usize = 128;
const QUERY_COUNT: usize = 2_000;
const WARMUP_COUNT: usize = 100;
const K: usize = 10;

fn random_unit_vector(rng: &mut StdRng) -> Vec<f32> {
    let mut vector: Vec<f32> = (0..DIM).map(|_| rng.gen_range(-1.0..1.0)).collect();
    let norm = vector.iter().map(|value| value * value).sum::<f32>().sqrt();
    for value in &mut vector {
        *value /= norm;
    }
    vector
}

fn parse_n() -> usize {
    let mut args = std::env::args().skip(1);
    match args.next() {
        Some(flag) if flag == "--n" => args
            .next()
            .expect("--n requires a value")
            .parse()
            .expect("n must be a positive integer"),
        Some(value) => value.parse().expect("n must be a positive integer"),
        None => 100_000,
    }
}

fn main() {
    let n = parse_n();
    assert!(n > 1, "n must be greater than one");

    let mut data_rng = StdRng::seed_from_u64(0x677D_15CA_77);
    let mut index = DiskAnnIndex::new(DiskAnnConfig {
        dim: DIM,
        max_degree: 4,
        build_beam: 8,
        search_beam: 64,
        alpha: 1.0,
        ..Default::default()
    });

    let build_started = Instant::now();
    for id in 0..n {
        index
            .insert(id.to_string(), random_unit_vector(&mut data_rng))
            .expect("benchmark insert failed");
    }
    index.build().expect("benchmark build failed");
    let build_seconds = build_started.elapsed().as_secs_f64();

    let mut query_rng = StdRng::seed_from_u64(0x6770_0A11_CE);
    let queries: Vec<Vec<f32>> = (0..QUERY_COUNT)
        .map(|_| random_unit_vector(&mut query_rng))
        .collect();

    for query in queries.iter().take(WARMUP_COUNT) {
        black_box(index.search(black_box(query), K).expect("warmup failed"));
    }

    let mut latencies_ns = Vec::with_capacity(QUERY_COUNT);
    let run_started = Instant::now();
    for query in &queries {
        let query_started = Instant::now();
        black_box(index.search(black_box(query), K).expect("search failed"));
        latencies_ns.push(query_started.elapsed().as_nanos() as u64);
    }
    let elapsed = run_started.elapsed();

    latencies_ns.sort_unstable();
    let p50_us = latencies_ns[QUERY_COUNT / 2] as f64 / 1_000.0;
    let p99_us = latencies_ns[(QUERY_COUNT * 99 / 100).min(QUERY_COUNT - 1)] as f64 / 1_000.0;
    let qps = QUERY_COUNT as f64 / elapsed.as_secs_f64();

    println!(
        "RESULT n={n} d={DIM} k={K} queries={QUERY_COUNT} build_s={build_seconds:.3} p50_us={p50_us:.3} p99_us={p99_us:.3} qps={qps:.3}"
    );
}
