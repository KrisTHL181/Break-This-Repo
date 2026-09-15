//! LinearFull: brute-force f32 linear scan — the ground-truth baseline.
//!
//! O(n × d) distance computations per query, full single-precision accuracy.
//! Used to generate ground-truth results against which the other variants are scored.

use crate::{sq_l2_f32, AnnVariant, Hit};

/// Full-precision linear scan index.
pub struct LinearFull {
    vectors: Vec<Vec<f32>>,
    dims: usize,
}

impl LinearFull {
    pub fn build(vectors: Vec<Vec<f32>>) -> Self {
        let dims = vectors.first().map(|v| v.len()).unwrap_or(0);
        Self { vectors, dims }
    }
}

impl AnnVariant for LinearFull {
    fn name(&self) -> &str {
        "LinearFull"
    }

    fn memory_bytes(&self) -> usize {
        self.vectors.len() * self.dims * std::mem::size_of::<f32>()
    }

    fn search(&self, query: &[f32], k: usize) -> Vec<Hit> {
        assert_eq!(query.len(), self.dims, "query dimension mismatch");
        let mut dists: Vec<(f32, usize)> = self
            .vectors
            .iter()
            .enumerate()
            .map(|(id, v)| (sq_l2_f32(query, v), id))
            .collect();

        // Partial sort: only top k need to be ordered.
        if k < dists.len() {
            dists.select_nth_unstable_by(k, |a, b| a.0.partial_cmp(&b.0).unwrap());
            dists.truncate(k);
        }
        dists.sort_unstable_by(|a, b| a.0.partial_cmp(&b.0).unwrap());

        dists
            .into_iter()
            .map(|(dist, id)| Hit { id, dist })
            .collect()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::dataset::{Dataset, DatasetConfig};

    #[test]
    fn test_linear_full_self_nearest() {
        // Each vector should be its own nearest neighbour.
        let cfg = DatasetConfig {
            n_vectors: 20,
            dims: 4,
            n_queries: 0,
            seed: 7,
        };
        let ds = Dataset::generate(cfg);
        let idx = LinearFull::build(ds.vectors.clone());

        for (i, q) in ds.vectors.iter().enumerate() {
            let hits = idx.search(q, 1);
            assert_eq!(hits[0].id, i, "vector {i} is not its own NN");
            assert!(
                hits[0].dist < 1e-6,
                "self-distance nonzero: {}",
                hits[0].dist
            );
        }
    }

    #[test]
    fn test_linear_full_k_results() {
        let vecs: Vec<Vec<f32>> = (0..100).map(|i| vec![i as f32, 0.0, 0.0, 0.0]).collect();
        let idx = LinearFull::build(vecs);
        let query = vec![50.0f32, 0.0, 0.0, 0.0];
        let hits = idx.search(&query, 5);
        assert_eq!(hits.len(), 5);
        // id=50 should be first (distance 0)
        assert_eq!(hits[0].id, 50);
        assert!(hits[0].dist < 1e-6);
    }

    #[test]
    fn test_linear_full_sorted_results() {
        let vecs: Vec<Vec<f32>> = (0..50).map(|i| vec![i as f32]).collect();
        let idx = LinearFull::build(vecs);
        let hits = idx.search(&[25.0f32], 10);
        for w in hits.windows(2) {
            assert!(w[0].dist <= w[1].dist, "results not sorted");
        }
    }
}
