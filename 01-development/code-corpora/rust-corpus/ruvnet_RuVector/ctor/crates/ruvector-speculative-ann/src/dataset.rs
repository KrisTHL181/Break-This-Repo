//! Deterministic synthetic dataset generation for speculative ANN benchmarks.
//!
//! Uses a simple LCG PRNG so runs are reproducible without any external crate.

/// Lightweight LCG pseudo-random number generator (Knuth constants).
pub struct Lcg {
    state: u64,
}

impl Lcg {
    pub fn new(seed: u64) -> Self {
        Self {
            state: seed ^ 0xcafe_babe_dead_beef,
        }
    }

    pub fn next_u64(&mut self) -> u64 {
        self.state = self
            .state
            .wrapping_mul(6_364_136_223_846_793_005)
            .wrapping_add(1_442_695_040_888_963_407);
        self.state
    }

    /// Uniform f64 in [0, 1).
    pub fn next_f64(&mut self) -> f64 {
        (self.next_u64() >> 11) as f64 / (1u64 << 53) as f64
    }

    /// Standard-normal sample via Box-Muller (uses two uniform draws).
    pub fn next_normal(&mut self) -> f32 {
        let u1 = (self.next_f64() + 1e-12).ln();
        let u2 = self.next_f64() * std::f64::consts::TAU;
        ((-2.0 * u1).sqrt() * u2.cos()) as f32
    }
}

/// Configuration for a synthetic benchmark dataset.
#[derive(Debug, Clone)]
pub struct DatasetConfig {
    pub n_vectors: usize,
    pub dims: usize,
    pub n_queries: usize,
    pub seed: u64,
}

impl Default for DatasetConfig {
    fn default() -> Self {
        Self {
            n_vectors: 10_000,
            dims: 128,
            n_queries: 500,
            seed: 0x5EED_0000_7777_0000,
        }
    }
}

/// Generated dataset: corpus vectors and query vectors.
pub struct Dataset {
    pub config: DatasetConfig,
    pub vectors: Vec<Vec<f32>>,
    pub queries: Vec<Vec<f32>>,
}

impl Dataset {
    /// Build a dataset with standard-normal f32 values.
    pub fn generate(config: DatasetConfig) -> Self {
        let mut rng = Lcg::new(config.seed);

        let vectors: Vec<Vec<f32>> = (0..config.n_vectors)
            .map(|_| (0..config.dims).map(|_| rng.next_normal()).collect())
            .collect();

        // Use a different seed stream for queries.
        let mut qrng = Lcg::new(config.seed.wrapping_add(0x1111_1111));
        let queries: Vec<Vec<f32>> = (0..config.n_queries)
            .map(|_| (0..config.dims).map(|_| qrng.next_normal()).collect())
            .collect();

        Self {
            config,
            vectors,
            queries,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_dataset_deterministic() {
        let cfg = DatasetConfig {
            n_vectors: 100,
            dims: 8,
            n_queries: 10,
            seed: 42,
        };
        let d1 = Dataset::generate(cfg.clone());
        let d2 = Dataset::generate(cfg);
        assert_eq!(d1.vectors[0], d2.vectors[0]);
        assert_eq!(d1.queries[0], d2.queries[0]);
    }

    #[test]
    fn test_dataset_shape() {
        let cfg = DatasetConfig {
            n_vectors: 50,
            dims: 16,
            n_queries: 5,
            seed: 1,
        };
        let d = Dataset::generate(cfg);
        assert_eq!(d.vectors.len(), 50);
        assert_eq!(d.vectors[0].len(), 16);
        assert_eq!(d.queries.len(), 5);
    }

    #[test]
    fn test_lcg_not_all_same() {
        let mut rng = Lcg::new(0);
        let a = rng.next_normal();
        let b = rng.next_normal();
        // Extremely unlikely to be equal.
        assert_ne!(a.to_bits(), b.to_bits());
    }
}
