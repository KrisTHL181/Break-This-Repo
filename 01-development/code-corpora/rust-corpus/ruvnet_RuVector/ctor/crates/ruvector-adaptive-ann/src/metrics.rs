//! Recall and latency metrics for benchmark reporting.

use std::collections::HashSet;

/// Recall@k: fraction of true top-k neighbours returned by the search.
///
/// `retrieved` and `ground_truth` are sets of node ids.
/// Returns a value in `[0.0, 1.0]`.
pub fn recall_at_k(retrieved: &[u32], ground_truth: &[u32]) -> f32 {
    if ground_truth.is_empty() {
        return 1.0;
    }
    let gt: HashSet<u32> = ground_truth.iter().copied().collect();
    let hits = retrieved.iter().filter(|id| gt.contains(id)).count();
    hits as f32 / ground_truth.len() as f32
}

/// Collected latency samples (nanoseconds).
#[derive(Default)]
pub struct LatencyStats {
    samples: Vec<u64>,
}

impl LatencyStats {
    pub fn push(&mut self, nanos: u64) {
        self.samples.push(nanos);
    }

    pub fn mean_us(&self) -> f64 {
        if self.samples.is_empty() {
            return 0.0;
        }
        self.samples.iter().sum::<u64>() as f64 / self.samples.len() as f64 / 1_000.0
    }

    pub fn p50_us(&self) -> f64 {
        self.percentile_us(50)
    }

    pub fn p95_us(&self) -> f64 {
        self.percentile_us(95)
    }

    pub fn qps(&self) -> f64 {
        if self.samples.is_empty() {
            return 0.0;
        }
        let mean_ns = self.samples.iter().sum::<u64>() as f64 / self.samples.len() as f64;
        1_000_000_000.0 / mean_ns
    }

    fn percentile_us(&self, pct: usize) -> f64 {
        if self.samples.is_empty() {
            return 0.0;
        }
        let mut s = self.samples.clone();
        s.sort_unstable();
        let idx = ((pct * s.len()) / 100).min(s.len() - 1);
        s[idx] as f64 / 1_000.0
    }
}

/// Estimate memory usage of a flat proximity graph.
pub fn memory_estimate_bytes(n: usize, dims: usize, avg_neighbors: usize) -> usize {
    let vector_bytes = n * dims * 4; // f32
    let adj_bytes = n * avg_neighbors * 4; // u32
    vector_bytes + adj_bytes
}
