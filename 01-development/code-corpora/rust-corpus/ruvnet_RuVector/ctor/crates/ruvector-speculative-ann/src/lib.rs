//! Speculative ANN Search for RuVector
//!
//! Inspired by speculative decoding in LLMs: a fast "draft" index (scalar-quantized
//! u8 vectors) proposes k' candidates; exact f32 distances verify the top-k among
//! them. An adaptive controller tunes k' to maintain a target recall against ground
//! truth, trading off latency vs. accuracy automatically.
//!
//! Three measurable variants:
//!  - `LinearFull`      – brute-force f32 scan (ground truth)
//!  - `QuantizedDraft`  – brute-force u8 scalar-quantized scan (no verify)
//!  - `SpeculativeANN`  – u8 draft for k' candidates + f32 exact re-rank

pub mod dataset;
pub mod linear_full;
pub mod quantized_draft;
pub mod speculative;

use std::collections::HashSet;

// ─── core types ──────────────────────────────────────────────────────────────

/// A single nearest-neighbour hit.
#[derive(Debug, Clone, PartialEq)]
pub struct Hit {
    pub id: usize,
    pub dist: f32,
}

impl Eq for Hit {}

impl PartialOrd for Hit {
    fn partial_cmp(&self, other: &Self) -> Option<std::cmp::Ordering> {
        Some(self.cmp(other))
    }
}

impl Ord for Hit {
    fn cmp(&self, other: &Self) -> std::cmp::Ordering {
        self.dist
            .partial_cmp(&other.dist)
            .unwrap_or(std::cmp::Ordering::Equal)
    }
}

// ─── trait ───────────────────────────────────────────────────────────────────

/// Common search interface for all three variants.
pub trait AnnVariant: Send + Sync {
    /// Return the `k` approximate nearest neighbours to `query`.
    fn search(&self, query: &[f32], k: usize) -> Vec<Hit>;

    /// Human-readable name for reporting.
    fn name(&self) -> &str;

    /// Estimated heap memory used by this index, in bytes.
    fn memory_bytes(&self) -> usize;
}

// ─── recall metric ───────────────────────────────────────────────────────────

/// Recall@k: fraction of ground-truth top-k ids present in `results`.
pub fn recall_at_k(results: &[Hit], ground_truth: &[Hit], k: usize) -> f32 {
    let res_ids: HashSet<usize> = results.iter().take(k).map(|h| h.id).collect();
    let gt_ids: HashSet<usize> = ground_truth.iter().take(k).map(|h| h.id).collect();
    if gt_ids.is_empty() {
        return 1.0;
    }
    let intersection = res_ids.intersection(&gt_ids).count();
    intersection as f32 / k.min(gt_ids.len()) as f32
}

// ─── distance helpers ─────────────────────────────────────────────────────────

/// Squared L2 distance between two f32 slices (no sqrt; monotone for ranking).
#[inline(always)]
pub fn sq_l2_f32(a: &[f32], b: &[f32]) -> f32 {
    a.iter()
        .zip(b.iter())
        .map(|(x, y)| {
            let d = x - y;
            d * d
        })
        .sum()
}

/// Squared L2 distance in u8 space (no overflow up to d=16384 with u64).
#[inline(always)]
pub fn sq_l2_u8(a: &[u8], b: &[u8]) -> u64 {
    a.iter()
        .zip(b.iter())
        .map(|(x, y)| {
            let d = *x as i32 - *y as i32;
            (d * d) as u64
        })
        .sum()
}

// ─── scalar quantizer ────────────────────────────────────────────────────────

/// Per-dimension min/max scalar quantizer: f32 → u8.
///
/// Quantizes each dimension independently using the range observed in the
/// training corpus. Out-of-range query values are clamped to [0, 255].
pub struct ScalarQuantizer {
    pub min_vals: Vec<f32>,
    pub scale: Vec<f32>, // 255.0 / (max - min), 0 when range==0
}

impl ScalarQuantizer {
    /// Train on a slice of flat vectors (each has `dims` elements).
    pub fn train(vectors: &[Vec<f32>]) -> Self {
        assert!(!vectors.is_empty(), "need at least one vector to train SQ");
        let dims = vectors[0].len();
        assert!(dims > 0, "vectors must have at least one dimension");
        assert!(
            vectors.iter().all(|v| v.len() == dims),
            "inconsistent vector dimensions"
        );
        let mut min_vals = vec![f32::MAX; dims];
        let mut max_vals = vec![f32::MIN; dims];

        for v in vectors {
            for (d, &x) in v.iter().enumerate() {
                if x < min_vals[d] {
                    min_vals[d] = x;
                }
                if x > max_vals[d] {
                    max_vals[d] = x;
                }
            }
        }

        let scale: Vec<f32> = min_vals
            .iter()
            .zip(max_vals.iter())
            .map(|(mn, mx)| {
                let range = mx - mn;
                if range > 1e-9 {
                    255.0 / range
                } else {
                    0.0
                }
            })
            .collect();

        Self { min_vals, scale }
    }

    /// Quantize one f32 vector to u8.
    pub fn quantize(&self, v: &[f32]) -> Vec<u8> {
        assert_eq!(v.len(), self.min_vals.len(), "vector dimension mismatch");
        v.iter()
            .enumerate()
            .map(|(d, &x)| {
                if self.scale[d] == 0.0 {
                    return 0u8;
                }
                let q = (x - self.min_vals[d]) * self.scale[d];
                q.clamp(0.0, 255.0).round() as u8
            })
            .collect()
    }
}

// ─── unit tests ──────────────────────────────────────────────────────────────

#[cfg(test)]
mod tests {
    use super::*;

    fn make_vecs() -> Vec<Vec<f32>> {
        vec![
            vec![0.0, 0.0, 0.0, 0.0],
            vec![1.0, 1.0, 1.0, 1.0],
            vec![2.0, 2.0, 2.0, 2.0],
            vec![10.0, 10.0, 10.0, 10.0],
        ]
    }

    #[test]
    fn test_sq_l2_f32_self_zero() {
        let v = vec![1.0f32, 2.0, 3.0];
        assert_eq!(sq_l2_f32(&v, &v), 0.0);
    }

    #[test]
    fn test_sq_l2_u8_self_zero() {
        let v = vec![10u8, 20, 30];
        assert_eq!(sq_l2_u8(&v, &v), 0);
    }

    #[test]
    fn test_scalar_quantizer_range() {
        let vecs = make_vecs();
        let sq = ScalarQuantizer::train(&vecs);
        // min should be all 0.0
        for &m in &sq.min_vals {
            assert_eq!(m, 0.0);
        }
        // scale = 255/10
        for &s in &sq.scale {
            assert!((s - 25.5).abs() < 0.01, "scale {s}");
        }
        // quantize the max vector → all 255
        let q = sq.quantize(&vecs[3]);
        for &b in &q {
            assert_eq!(b, 255);
        }
        // quantize the zero vector → all 0
        let q0 = sq.quantize(&vecs[0]);
        for &b in &q0 {
            assert_eq!(b, 0);
        }
    }

    #[test]
    fn test_recall_at_k_perfect() {
        let gt = vec![
            Hit { id: 0, dist: 0.1 },
            Hit { id: 1, dist: 0.2 },
            Hit { id: 2, dist: 0.3 },
        ];
        let res = gt.clone();
        assert_eq!(recall_at_k(&res, &gt, 3), 1.0);
    }

    #[test]
    fn test_recall_at_k_partial() {
        let gt = vec![
            Hit { id: 0, dist: 0.1 },
            Hit { id: 1, dist: 0.2 },
            Hit { id: 2, dist: 0.3 },
            Hit { id: 3, dist: 0.4 },
        ];
        let res = vec![
            Hit { id: 0, dist: 0.1 },
            Hit { id: 1, dist: 0.2 },
            Hit { id: 99, dist: 99.0 },
            Hit {
                id: 100,
                dist: 99.0,
            },
        ];
        // 2 out of 4 correct
        assert_eq!(recall_at_k(&res, &gt, 4), 0.5);
    }
}
