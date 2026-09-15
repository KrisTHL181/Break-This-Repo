//! Measure recall and delete latency after deleting 20% of a DiskANN index.

use rand::prelude::*;
use ruvector_diskann::{DiskAnnConfig, DiskAnnIndex};
use std::collections::HashSet;
use std::fs;
use std::path::PathBuf;
use std::time::{Duration, Instant};

const DIM: usize = 32;
const K: usize = 10;
const QUERY_COUNT: usize = 50;

fn l2_squared(a: &[f32], b: &[f32]) -> f32 {
    a.iter()
        .zip(b)
        .map(|(left, right)| {
            let delta = left - right;
            delta * delta
        })
        .sum()
}

fn median_micros(samples: &mut [Duration]) -> f64 {
    samples.sort_unstable();
    samples[samples.len() / 2].as_secs_f64() * 1_000_000.0
}

fn recall_at_10(
    index: &DiskAnnIndex,
    data: &[(String, Vec<f32>)],
    queries: &[usize],
    truth: &[HashSet<String>],
) -> f64 {
    let matches: usize = queries
        .iter()
        .zip(truth)
        .map(|(&query_idx, expected)| {
            index
                .search(&data[query_idx].1, K)
                .expect("search should succeed")
                .iter()
                .filter(|result| expected.contains(&result.id))
                .count()
        })
        .sum();
    matches as f64 / (queries.len() * K) as f64
}

fn run_case(n: usize) {
    let mut rng = StdRng::seed_from_u64(0x679D_15CA ^ n as u64);
    let data: Vec<(String, Vec<f32>)> = (0..n)
        .map(|idx| {
            (
                format!("v{idx}"),
                (0..DIM).map(|_| rng.gen::<f32>()).collect(),
            )
        })
        .collect();
    let config = DiskAnnConfig {
        dim: DIM,
        max_degree: 32,
        build_beam: 64,
        search_beam: 64,
        alpha: 1.2,
        ..Default::default()
    };

    let mut order: Vec<usize> = (0..n).collect();
    order.shuffle(&mut rng);
    let delete_count = n / 5;
    let deleted: HashSet<usize> = order[..delete_count].iter().copied().collect();
    let survivor_indices: Vec<usize> = (0..n).filter(|idx| !deleted.contains(idx)).collect();
    let queries: Vec<usize> = order[delete_count..delete_count + QUERY_COUNT].to_vec();

    let truth: Vec<HashSet<String>> = queries
        .iter()
        .map(|&query_idx| {
            let mut exact: Vec<(usize, f32)> = survivor_indices
                .iter()
                .map(|&idx| (idx, l2_squared(&data[idx].1, &data[query_idx].1)))
                .collect();
            exact.sort_unstable_by(|a, b| a.1.total_cmp(&b.1));
            exact
                .iter()
                .take(K)
                .map(|(idx, _)| data[*idx].0.clone())
                .collect()
        })
        .collect();

    let dir: PathBuf =
        std::env::temp_dir().join(format!("ruvector-delete-recall-{}-{n}", std::process::id()));
    let _ = fs::remove_dir_all(&dir);

    let mut base = DiskAnnIndex::new(config.clone());
    base.insert_batch(data.clone()).expect("insert base data");
    base.build().expect("build base index");
    base.save(&dir).expect("save base index");

    let mut deferred = DiskAnnIndex::load(&dir).expect("load deferred index");
    let mut repaired = DiskAnnIndex::load(&dir).expect("load repaired index");
    let mut deferred_times = Vec::with_capacity(delete_count);
    let mut repair_times = Vec::with_capacity(delete_count);

    for &idx in &order[..delete_count] {
        let started = Instant::now();
        deferred
            .delete_deferred(&data[idx].0)
            .expect("deferred delete");
        deferred_times.push(started.elapsed());

        let started = Instant::now();
        repaired.delete(&data[idx].0).expect("repairing delete");
        repair_times.push(started.elapsed());
    }

    let survivor_data: Vec<(String, Vec<f32>)> = survivor_indices
        .iter()
        .map(|&idx| data[idx].clone())
        .collect();
    let mut fresh = DiskAnnIndex::new(config);
    fresh
        .insert_batch(survivor_data)
        .expect("insert survivor data");
    fresh.build().expect("build fresh survivor index");

    println!("RESULT n={n} mode=main recall_at_10=UNMEASURABLE note=deleted_ids_returned");
    println!(
        "RESULT n={n} mode=tombstone_only recall_at_10={:.4}",
        recall_at_10(&deferred, &data, &queries, &truth)
    );
    println!(
        "RESULT n={n} mode=tombstone_repair recall_at_10={:.4}",
        recall_at_10(&repaired, &data, &queries, &truth)
    );
    println!(
        "RESULT n={n} mode=fresh_rebuild recall_at_10={:.4}",
        recall_at_10(&fresh, &data, &queries, &truth)
    );
    println!(
        "LATENCY n={n} mode=tombstone_only median_us={:.3}",
        median_micros(&mut deferred_times)
    );
    println!(
        "LATENCY n={n} mode=tombstone_repair median_us={:.3}",
        median_micros(&mut repair_times)
    );

    drop((base, deferred, repaired));
    let _ = fs::remove_dir_all(dir);
}

fn main() {
    let threads = std::thread::available_parallelism()
        .map(|count| count.get())
        .unwrap_or(1);
    println!("BENCHMARK threads={threads} profile=release dim={DIM} k={K}");
    for n in [20_000, 100_000] {
        run_case(n);
    }
}
