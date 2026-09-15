//! SpeculativeANN: draft-then-verify with adaptive candidate multiplier.
//!
//! Protocol (mirrors speculative decoding from LLM inference):
//!
//!  1. **Draft**: run `QuantizedDraft` to get `k'` candidate ids cheaply.
//!  2. **Verify**: compute exact f32 distances for those `k'` ids only.
//!  3. **Re-rank**: take top-k from verified set and return.
//!  4. **Calibrate**: track rolling recall against ground truth; if acceptance
//!     rate drops below `target_recall`, increase `k'`; if well above, shrink.
//!
//! Cost model:
//!   draft  = O(n × d / 4)   [u8 integers vs f32]
//!   verify = O(k' × d)      [exact f32 on small candidate set]
//!   total  ≈ (n/4 + k') × d   vs.  n × d  for LinearFull
//!
//! Break-even: any k' << 3n/4, which is always true for practical k.

use crate::quantized_draft::QuantizedDraft;
use crate::{sq_l2_f32, AnnVariant, Hit};

/// Configuration for the adaptive controller.
#[derive(Debug, Clone)]
pub struct SpecConfig {
    /// Initial candidate multiplier (k' = k * initial_mult).
    pub initial_mult: usize,
    /// Minimum multiplier (1 = k' == k, never below ground truth set size).
    pub min_mult: usize,
    /// Maximum multiplier cap.
    pub max_mult: usize,
    /// Rolling window size for acceptance rate estimation.
    pub window: usize,
    /// Target recall; increase k' if rolling recall falls below this.
    pub target_recall: f32,
}

impl Default for SpecConfig {
    fn default() -> Self {
        Self {
            initial_mult: 3,
            min_mult: 1,
            max_mult: 20,
            window: 50,
            target_recall: 0.95,
        }
    }
}

/// Tracks rolling recall over the last `window` queries.
struct RollingRecall {
    buf: Vec<f32>,
    head: usize,
    filled: bool,
    window: usize,
}

impl RollingRecall {
    fn new(window: usize) -> Self {
        assert!(window > 0, "recall window must be greater than zero");
        Self {
            buf: vec![0.0; window],
            head: 0,
            filled: false,
            window,
        }
    }

    fn push(&mut self, recall: f32) {
        self.buf[self.head] = recall;
        self.head = (self.head + 1) % self.window;
        if self.head == 0 {
            self.filled = true;
        }
    }

    fn mean(&self) -> f32 {
        let len = if self.filled {
            self.window
        } else {
            self.head.max(1)
        };
        self.buf.iter().take(len).sum::<f32>() / len as f32
    }
}

/// Speculative ANN index with adaptive candidate multiplier.
///
/// The struct is intentionally `!Sync` because the adaptive state (`mult`,
/// `rolling`) is mutated during search.  For parallel benchmarks, clone one
/// `SpeculativeANN` per thread or wrap in `Mutex`.
pub struct SpeculativeANN {
    /// Full-precision corpus for the verify step.
    full_vecs: Vec<Vec<f32>>,
    /// Scalar-quantized index for the draft step.
    draft: QuantizedDraft,
    dims: usize,
    cfg: SpecConfig,
    /// Current candidate multiplier (mutable during adaptive search).
    mult: usize,
    /// Rolling recall estimator.
    rolling: RollingRecall,
    /// Statistics (queries processed, acceptances).
    pub queries_run: usize,
    pub accepted: usize,
}

impl SpeculativeANN {
    pub fn build(vectors: Vec<Vec<f32>>, cfg: SpecConfig) -> Self {
        let draft = QuantizedDraft::build(&vectors);
        let dims = vectors.first().map(|v| v.len()).unwrap_or(0);
        let mult = cfg.initial_mult;
        let window = cfg.window;
        Self {
            full_vecs: vectors,
            draft,
            dims,
            cfg: cfg.clone(),
            mult,
            rolling: RollingRecall::new(window),
            queries_run: 0,
            accepted: 0,
        }
    }

    /// Current candidate multiplier (k' = k × mult).
    pub fn current_mult(&self) -> usize {
        self.mult
    }

    /// Rolling acceptance rate (last `window` queries).
    pub fn acceptance_rate(&self) -> f32 {
        self.rolling.mean()
    }

    /// Non-adaptive search at a fixed `mult` (used in benchmarks to isolate
    /// the benefit of a specific k' without feedback noise).
    pub fn search_fixed(&self, query: &[f32], k: usize, mult: usize) -> Vec<Hit> {
        if k == 0 {
            return Vec::new();
        }
        assert_eq!(query.len(), self.dims, "query dimension mismatch");
        let k_prime = k.saturating_mul(mult).max(k);
        let candidate_ids = self.draft.draft_ids(query, k_prime);
        self.verify_and_rerank(query, &candidate_ids, k)
    }

    /// Verify candidate ids with exact f32 distances and return top-k.
    fn verify_and_rerank(&self, query: &[f32], ids: &[usize], k: usize) -> Vec<Hit> {
        let mut verified: Vec<Hit> = ids
            .iter()
            .map(|&id| Hit {
                id,
                dist: sq_l2_f32(query, &self.full_vecs[id]),
            })
            .collect();
        verified.sort_unstable_by(|a, b| a.dist.total_cmp(&b.dist));
        verified.truncate(k);
        verified
    }

    /// Adaptive search: adjusts `mult` based on rolling recall feedback.
    ///
    /// Note: this mutates `self.mult` and `self.rolling`.  The recall is
    /// measured against a freshly computed brute-force result when
    /// `ground_truth` is supplied. Without audited feedback, the search runs
    /// at the current multiplier but does not adapt: agreement between the
    /// draft and its own reranked candidate pool cannot detect missing true
    /// neighbours and is therefore not a recall estimate.
    pub fn search_adaptive(
        &mut self,
        query: &[f32],
        k: usize,
        ground_truth: Option<&[Hit]>,
    ) -> Vec<Hit> {
        if k == 0 {
            return Vec::new();
        }
        assert_eq!(query.len(), self.dims, "query dimension mismatch");
        let k_prime = k.saturating_mul(self.mult).max(k);
        let candidate_ids = self.draft.draft_ids(query, k_prime);
        let results = self.verify_and_rerank(query, &candidate_ids, k);

        let Some(gt) = ground_truth else {
            return results;
        };
        let recall = crate::recall_at_k(&results, gt, k);

        self.rolling.push(recall);
        self.queries_run += 1;
        if recall >= self.cfg.target_recall {
            self.accepted += 1;
        }

        // Adapt mult based on rolling mean.
        let mean = self.rolling.mean();
        if mean < self.cfg.target_recall {
            self.mult = (self.mult + 2).min(self.cfg.max_mult);
        } else if mean > self.cfg.target_recall + 0.03 {
            self.mult = (self.mult.saturating_sub(1)).max(self.cfg.min_mult);
        }

        results
    }
}

impl AnnVariant for SpeculativeANN {
    fn name(&self) -> &str {
        "SpeculativeANN"
    }

    fn memory_bytes(&self) -> usize {
        // Full vectors for verify + draft index.
        let full = self.full_vecs.len() * self.dims * std::mem::size_of::<f32>();
        let draft = self.draft.memory_bytes();
        full + draft
    }

    fn search(&self, query: &[f32], k: usize) -> Vec<Hit> {
        // Non-adaptive path for trait compatibility (uses current mult).
        self.search_fixed(query, k, self.mult)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::dataset::{Dataset, DatasetConfig};
    use crate::linear_full::LinearFull;
    use crate::recall_at_k;

    fn small_dataset() -> Dataset {
        Dataset::generate(DatasetConfig {
            n_vectors: 500,
            dims: 32,
            n_queries: 50,
            seed: 0xBEEF,
        })
    }

    #[test]
    fn test_speculative_returns_k_results() {
        let ds = small_dataset();
        let spec = SpeculativeANN::build(ds.vectors.clone(), SpecConfig::default());
        for q in &ds.queries {
            let hits = spec.search(q, 10);
            assert_eq!(hits.len(), 10, "expected 10 results");
        }
    }

    #[test]
    fn test_speculative_recall_above_threshold() {
        // With mult=5 and 500 vectors, speculative should achieve >=0.90 mean recall.
        let ds = small_dataset();
        let baseline = LinearFull::build(ds.vectors.clone());
        let spec = SpeculativeANN::build(
            ds.vectors.clone(),
            SpecConfig {
                initial_mult: 5,
                ..Default::default()
            },
        );

        let mut total_recall = 0.0f32;
        for q in &ds.queries {
            let gt = baseline.search(q, 10);
            let res = spec.search(q, 10);
            total_recall += recall_at_k(&res, &gt, 10);
        }
        let mean_recall = total_recall / ds.queries.len() as f32;
        assert!(
            mean_recall >= 0.90,
            "mean recall {mean_recall:.3} below 0.90 with mult=5"
        );
    }

    #[test]
    fn test_adaptive_acceptance_rate() {
        let ds = small_dataset();
        let baseline = LinearFull::build(ds.vectors.clone());
        let mut spec = SpeculativeANN::build(ds.vectors.clone(), SpecConfig::default());

        for q in &ds.queries {
            let gt = baseline.search(q, 10);
            spec.search_adaptive(q, 10, Some(&gt));
        }

        let rate = spec.accepted as f32 / spec.queries_run as f32;
        // With adaptive control, acceptance rate should climb above 0.80.
        assert!(
            rate >= 0.70,
            "acceptance rate {rate:.3} too low; adaptive controller not working"
        );
    }

    #[test]
    fn test_no_feedback_does_not_change_controller() {
        let ds = small_dataset();
        let mut spec = SpeculativeANN::build(ds.vectors, SpecConfig::default());
        let initial = spec.current_mult();
        let _ = spec.search_adaptive(&ds.queries[0], 10, None);
        assert_eq!(spec.current_mult(), initial);
        assert_eq!(spec.queries_run, 0);
    }

    #[test]
    fn test_fixed_mult_higher_mult_better_recall() {
        let ds = small_dataset();
        let baseline = LinearFull::build(ds.vectors.clone());
        let spec = SpeculativeANN::build(ds.vectors.clone(), SpecConfig::default());

        let recall_for_mult = |mult: usize| -> f32 {
            let mut total = 0.0f32;
            for q in &ds.queries {
                let gt = baseline.search(q, 10);
                let res = spec.search_fixed(q, 10, mult);
                total += recall_at_k(&res, &gt, 10);
            }
            total / ds.queries.len() as f32
        };

        let r1 = recall_for_mult(1);
        let r5 = recall_for_mult(5);
        assert!(
            r5 > r1,
            "mult=5 recall {r5:.3} not better than mult=1 {r1:.3}"
        );
    }
}
