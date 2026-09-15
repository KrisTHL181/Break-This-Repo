//! QuantizedDraft: scalar-quantized u8 linear scan with no verification step.
//!
//! Compresses f32 vectors to u8 (4× smaller), then runs a brute-force scan in
//! u8 space.  Distance computations are integer arithmetic (faster SIMD-friendly
//! path on modern CPUs), but quantization error reduces recall relative to
//! LinearFull.
//!
//! This variant establishes the upper bound on speed and lower bound on accuracy
//! for the draft component used by SpeculativeANN.

use crate::{sq_l2_u8, AnnVariant, Hit, ScalarQuantizer};

/// Draft index backed by scalar-quantized u8 vectors.
pub struct QuantizedDraft {
    quantized: Vec<Vec<u8>>,
    sq: ScalarQuantizer,
    dims: usize,
    n: usize,
}

impl QuantizedDraft {
    /// Build from full-precision f32 vectors.
    pub fn build(vectors: &[Vec<f32>]) -> Self {
        let sq = ScalarQuantizer::train(vectors);
        let quantized: Vec<Vec<u8>> = vectors.iter().map(|v| sq.quantize(v)).collect();
        let dims = vectors.first().map(|v| v.len()).unwrap_or(0);
        Self {
            quantized,
            sq,
            dims,
            n: vectors.len(),
        }
    }

    /// Return the top-`k_prime` candidate ids ordered by u8 distance.
    pub fn draft_ids(&self, query: &[f32], k_prime: usize) -> Vec<usize> {
        let q_u8 = self.sq.quantize(query);

        let mut dists: Vec<(u64, usize)> = self
            .quantized
            .iter()
            .enumerate()
            .map(|(id, qv)| (sq_l2_u8(&q_u8, qv), id))
            .collect();

        if k_prime < dists.len() {
            dists.select_nth_unstable_by_key(k_prime, |&(d, _)| d);
            dists.truncate(k_prime);
        }
        dists.sort_unstable_by_key(|(d, _)| *d);
        dists.into_iter().map(|(_, id)| id).collect()
    }
}

impl AnnVariant for QuantizedDraft {
    fn name(&self) -> &str {
        "QuantizedDraft"
    }

    fn memory_bytes(&self) -> usize {
        // u8 compressed vectors + SQ metadata (2 f32 per dim)
        self.n * self.dims * std::mem::size_of::<u8>() + self.dims * 2 * std::mem::size_of::<f32>()
    }

    fn search(&self, query: &[f32], k: usize) -> Vec<Hit> {
        let q_u8 = self.sq.quantize(query);

        let mut dists: Vec<(u64, usize)> = self
            .quantized
            .iter()
            .enumerate()
            .map(|(id, qv)| (sq_l2_u8(&q_u8, qv), id))
            .collect();

        if k < dists.len() {
            dists.select_nth_unstable_by_key(k, |&(d, _)| d);
            dists.truncate(k);
        }
        dists.sort_unstable_by_key(|(d, _)| *d);

        // Return u64 dist cast to f32 for uniform Hit type.
        dists
            .into_iter()
            .map(|(dist, id)| Hit {
                id,
                dist: dist as f32,
            })
            .collect()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::dataset::{Dataset, DatasetConfig};

    #[test]
    fn test_quantized_draft_returns_k() {
        let cfg = DatasetConfig {
            n_vectors: 100,
            dims: 16,
            n_queries: 5,
            seed: 99,
        };
        let ds = Dataset::generate(cfg);
        let idx = QuantizedDraft::build(&ds.vectors);
        for q in &ds.queries {
            let hits = idx.search(q, 10);
            assert_eq!(hits.len(), 10);
        }
    }

    #[test]
    fn test_draft_ids_count() {
        let vecs: Vec<Vec<f32>> = (0..50).map(|i| vec![i as f32, 0.0]).collect();
        let idx = QuantizedDraft::build(&vecs);
        let ids = idx.draft_ids(&[25.0, 0.0], 15);
        assert_eq!(ids.len(), 15);
    }

    #[test]
    fn test_quantized_self_nearest() {
        // At least 80% of vectors should be their own nearest neighbour under SQ.
        let cfg = DatasetConfig {
            n_vectors: 100,
            dims: 32,
            n_queries: 0,
            seed: 42,
        };
        let ds = Dataset::generate(cfg);
        let idx = QuantizedDraft::build(&ds.vectors);
        let mut correct = 0usize;
        for (i, v) in ds.vectors.iter().enumerate() {
            let hits = idx.search(v, 1);
            if hits[0].id == i {
                correct += 1;
            }
        }
        assert!(
            correct >= 75,
            "only {correct}/100 self-nearest under SQ (expected >=75)"
        );
    }
}
