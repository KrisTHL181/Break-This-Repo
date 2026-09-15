//! Deterministic dataset generation for benchmarks and tests.

use rand::rngs::StdRng;
use rand::SeedableRng;
use rand_distr::{Distribution, Normal, Uniform};

use crate::graph::l2_sq;

/// Generate `n` random unit-length vectors in `dims` dimensions.
pub fn random_unit_vectors(n: usize, dims: usize, seed: u64) -> Vec<f32> {
    let mut rng = StdRng::seed_from_u64(seed);
    let normal = Normal::new(0.0f32, 1.0).unwrap();
    let mut out = vec![0.0f32; n * dims];
    for i in 0..n {
        let row = &mut out[i * dims..(i + 1) * dims];
        let mut norm_sq = 0.0f32;
        for v in row.iter_mut() {
            *v = normal.sample(&mut rng);
            norm_sq += *v * *v;
        }
        let norm = norm_sq.sqrt().max(1e-9);
        for v in row.iter_mut() {
            *v /= norm;
        }
    }
    out
}

/// Generate `n` clustered vectors: `n_clusters` Gaussian clusters of equal size.
///
/// Returns `(vectors, cluster_assignments)`.
pub fn clustered_unit_vectors(
    n_clusters: usize,
    per_cluster: usize,
    dims: usize,
    std: f32,
    seed: u64,
) -> (Vec<f32>, Vec<usize>) {
    let mut rng = StdRng::seed_from_u64(seed);
    let uni = Uniform::new(-1.0f32, 1.0);
    let normal = Normal::new(0.0f32, std).unwrap();

    // Random cluster centers (unit sphere projected).
    let centers: Vec<Vec<f32>> = (0..n_clusters)
        .map(|_| {
            let mut c: Vec<f32> = (0..dims).map(|_| uni.sample(&mut rng)).collect();
            let norm: f32 = c.iter().map(|x| x * x).sum::<f32>().sqrt().max(1e-9);
            c.iter_mut().for_each(|v| *v /= norm);
            c
        })
        .collect();

    let n = n_clusters * per_cluster;
    let mut vectors = vec![0.0f32; n * dims];
    let mut assignments = vec![0usize; n];

    for (c, center) in centers.iter().enumerate() {
        for p in 0..per_cluster {
            let idx = c * per_cluster + p;
            assignments[idx] = c;
            let row = &mut vectors[idx * dims..(idx + 1) * dims];
            for (d, v) in row.iter_mut().enumerate() {
                *v = center[d] + normal.sample(&mut rng);
            }
            // Normalize.
            let norm: f32 = row.iter().map(|x| x * x).sum::<f32>().sqrt().max(1e-9);
            row.iter_mut().for_each(|v| *v /= norm);
        }
    }

    (vectors, assignments)
}

/// Generate `n_queries` random unit vectors as queries.
pub fn random_queries(n_queries: usize, dims: usize, seed: u64) -> Vec<f32> {
    random_unit_vectors(n_queries, dims, seed)
}

/// Brute-force ground truth: for each query return the indices of the top-`k`
/// nearest neighbours (by squared L2 distance) in `data`.
pub fn ground_truth(data: &[f32], queries: &[f32], dims: usize, k: usize) -> Vec<Vec<u32>> {
    let n_data = data.len() / dims;
    let n_q = queries.len() / dims;

    (0..n_q)
        .map(|qi| {
            let q = &queries[qi * dims..(qi + 1) * dims];
            let mut dists: Vec<(u32, f32)> = (0..n_data)
                .map(|di| {
                    let d = &data[di * dims..(di + 1) * dims];
                    (di as u32, l2_sq(q, d))
                })
                .collect();
            dists.sort_by(|a, b| a.1.total_cmp(&b.1));
            dists.truncate(k);
            dists.iter().map(|&(id, _)| id).collect()
        })
        .collect()
}
