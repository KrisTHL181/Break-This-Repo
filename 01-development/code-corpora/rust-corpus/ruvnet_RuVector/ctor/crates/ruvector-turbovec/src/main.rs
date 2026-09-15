//! `turbovec-demo` — end-to-end **proof** that the TurboVec index (ADR-254)
//! works correctly: compression, recall vs exact brute-force L2, estimator
//! empirical estimator bias, determinism, and IdMap delete + filtered search.
//!
//! Run: `cargo run --release -p ruvector-turbovec`

use ruvector_rabitq::AnnIndex;
use ruvector_turbovec::{BitWidth, IdMapIndex, TurboVecIndex};

use rand::{Rng, SeedableRng};

fn random_vectors(n: usize, dim: usize, seed: u64) -> Vec<Vec<f32>> {
    let mut rng = rand::rngs::StdRng::seed_from_u64(seed);
    (0..n)
        .map(|_| (0..dim).map(|_| rng.gen::<f32>() * 2.0 - 1.0).collect())
        .collect()
}

fn brute_topk(data: &[Vec<f32>], q: &[f32], k: usize) -> Vec<usize> {
    let mut s: Vec<(usize, f32)> = data
        .iter()
        .enumerate()
        .map(|(i, v)| (i, v.iter().zip(q).map(|(a, b)| (a - b) * (a - b)).sum()))
        .collect();
    s.sort_unstable_by(|a, b| a.1.total_cmp(&b.1));
    s.into_iter().take(k).map(|(i, _)| i).collect()
}

fn recall_and_bias(bw: BitWidth, data: &[Vec<f32>], queries: &[Vec<f32>], dim: usize, k: usize) {
    let mut ix = TurboVecIndex::with_seed(dim, bw, 42).unwrap();
    for (i, v) in data.iter().enumerate() {
        ix.add(i, v.clone()).unwrap();
    }
    ix.finalize();

    // Recall@k vs exact brute force.
    let mut hits = 0usize;
    let mut total = 0usize;
    for q in queries {
        let truth = brute_topk(data, q, k);
        let got: Vec<usize> = ix.search(q, k).unwrap().into_iter().map(|r| r.id).collect();
        hits += got.iter().filter(|g| truth.contains(g)).count();
        total += k;
    }
    let recall = hits as f32 / total as f32;

    // Mean signed cosine error (empirical bias measurement).
    let mut sum_err = 0.0f64;
    let mut n = 0u64;
    for q in queries.iter().take(20) {
        let nq: f32 = q.iter().map(|x| x * x).sum::<f32>().sqrt();
        for (pos, x) in data.iter().enumerate().take(300) {
            let nx: f32 = x.iter().map(|t| t * t).sum::<f32>().sqrt();
            let true_cos = q.iter().zip(x).map(|(a, b)| a * b).sum::<f32>() / (nq * nx);
            sum_err += (ix.estimate_cosine(q, pos).unwrap() - true_cos) as f64;
            n += 1;
        }
    }
    let mean_bias = sum_err / n as f64;

    let raw_bytes = dim * 4;
    println!(
        "  {:<6} | recall@{k}: {:.3} | bytes/vec: {:>4} (raw {raw_bytes}) | {:>5.1}x | mean cos-bias: {:+.4}",
        format!("{bw:?}"),
        recall,
        ix.bytes_per_vector(),
        ix.compression_ratio(),
        mean_bias,
    );
}

fn main() {
    let dim = 256;
    let n = 5_000;
    let k = 10;
    let data = random_vectors(n, dim, 10);
    let queries = random_vectors(200, dim, 99);

    println!("\n=== TurboVec (ADR-254) proof — n={n}, dim={dim}, k={k} ===\n");
    println!("[1] Compression + recall vs exact brute-force L2 + estimator bias");
    for bw in [
        BitWidth::One,
        BitWidth::Two,
        BitWidth::Three,
        BitWidth::Four,
    ] {
        recall_and_bias(bw, &data, &queries, dim, k);
    }

    // [2] Determinism.
    println!("\n[2] Determinism (same seed => identical results)");
    let build = |seed: u64| {
        let mut ix = TurboVecIndex::with_seed(dim, BitWidth::Four, seed).unwrap();
        for (i, v) in data.iter().enumerate() {
            ix.add(i, v.clone()).unwrap();
        }
        ix.finalize();
        ix.search(&queries[0], k).unwrap()
    };
    let a = build(7);
    let b = build(7);
    let same = a
        .iter()
        .zip(&b)
        .all(|(x, y)| x.id == y.id && x.score == y.score);
    println!(
        "  seed 7 == seed 7 : {}",
        if same { "PASS" } else { "FAIL" }
    );

    // [3] IdMap: O(1) delete hidden from search + filtered search correctness.
    println!("\n[3] IdMapIndex: delete + allowlist-filtered search");
    let ids: Vec<u64> = (0..n as u64).map(|i| i * 3 + 1).collect();
    let mut idx = IdMapIndex::with_seed(dim, BitWidth::Four, 42).unwrap();
    idx.add_with_ids(&data, &ids).unwrap();
    idx.finalize();
    let before = idx.search(&queries[1], 5).unwrap();
    let victim = before[0].0;
    idx.remove(victim);
    let after = idx.search(&queries[1], 5).unwrap();
    let hidden = after.iter().all(|(id, _)| *id != victim);
    println!(
        "  removed id {victim}: live={}, hidden-from-search: {}",
        idx.len(),
        if hidden { "PASS" } else { "FAIL" }
    );
    let allow: Vec<u64> = ids.iter().copied().step_by(7).collect();
    let allow_set: std::collections::HashSet<u64> = allow.iter().copied().collect();
    let filtered = idx.search_filtered(&queries[1], 10, &allow).unwrap();
    let ok = filtered.iter().all(|(id, _)| allow_set.contains(id)) && !filtered.is_empty();
    println!(
        "  filtered search ({} allowed): {} hits, all in allowlist: {}",
        allow.len(),
        filtered.len(),
        if ok { "PASS" } else { "FAIL" }
    );

    println!("\nAll proof checks complete.\n");
}
