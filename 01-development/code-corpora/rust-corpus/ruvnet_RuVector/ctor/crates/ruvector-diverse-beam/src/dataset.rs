use rand::rngs::StdRng;
use rand::{Rng, SeedableRng};
use rand_distr::{Distribution, Normal};

/// Uniform random dataset: vectors drawn from U[−1, 1]^dim.
pub fn uniform(n: usize, dim: usize, seed: u64) -> Vec<Vec<f32>> {
    let mut rng = StdRng::seed_from_u64(seed);
    (0..n)
        .map(|_| (0..dim).map(|_| rng.gen_range(-1.0_f32..1.0_f32)).collect())
        .collect()
}

/// Clustered dataset: `n` vectors drawn from `k_clusters` Gaussian blobs.
/// Centers are placed uniformly at random; each cluster has standard deviation `std_dev`.
pub fn clustered(
    n: usize,
    dim: usize,
    k_clusters: usize,
    std_dev: f32,
    seed: u64,
) -> Vec<Vec<f32>> {
    let mut rng = StdRng::seed_from_u64(seed);
    let normal = Normal::new(0.0_f32, std_dev).expect("valid std_dev");

    let centers: Vec<Vec<f32>> = (0..k_clusters)
        .map(|_| (0..dim).map(|_| rng.gen_range(-0.8_f32..0.8_f32)).collect())
        .collect();

    (0..n)
        .map(|i| {
            let c = &centers[i % k_clusters];
            c.iter().map(|&ci| ci + normal.sample(&mut rng)).collect()
        })
        .collect()
}

/// Query vectors for a uniform dataset (different seed offset).
pub fn queries_uniform(n_q: usize, dim: usize, seed: u64) -> Vec<Vec<f32>> {
    uniform(n_q, dim, seed.wrapping_add(0x1a2b_3c4d))
}

/// Query vectors for a clustered dataset (same clustering, different seed offset).
pub fn queries_clustered(
    n_q: usize,
    dim: usize,
    k_clusters: usize,
    std_dev: f32,
    seed: u64,
) -> Vec<Vec<f32>> {
    clustered(
        n_q,
        dim,
        k_clusters,
        std_dev,
        seed.wrapping_add(0x1a2b_3c4d),
    )
}
