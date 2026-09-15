//! Three adaptive recall-targeted beam search variants.
//!
//! All variants share the same `beam_search` core that runs greedy best-first
//! graph traversal with a candidate heap of size `ef`.
//!
//! ## Variants
//!
//! * **[`FixedEfSearch`]** — Baseline. Uses a constant `ef` regardless of the
//!   recall target. Shows what happens without adaptation.
//!
//! * **[`BinarySearchCalibrated`]** — For each query, binary-searches over
//!   `ef` values to find the minimum that achieves the recall target.
//!   Requires access to ground truth at query time (not practical in production,
//!   but demonstrates the theoretical minimum ef for each query distribution).
//!
//! * **[`TableCalibratedSearch`]** — Pre-calibrates a monotone ef→recall table
//!   offline, then at query time looks up an empirical ef estimate for the
//!   calibrated workload and result count.

use std::cmp::Reverse;
use std::collections::BinaryHeap;

use crate::calibrate::CalibrationTable;
use crate::graph::{l2_sq, FlatGraph};
use crate::metrics::recall_at_k;
use crate::RecallTargetedSearch;

/// A single nearest-neighbour result.
#[derive(Debug, Clone)]
pub struct SearchResult {
    /// Node id.
    pub id: u32,
    /// Squared L2 distance to the query.
    pub dist_sq: f32,
}

/// Ordered f32 for use in `BinaryHeap`.
#[derive(Clone, Copy, PartialEq)]
struct OrdF32(f32);
impl Eq for OrdF32 {}
impl PartialOrd for OrdF32 {
    fn partial_cmp(&self, other: &Self) -> Option<std::cmp::Ordering> {
        Some(self.cmp(other))
    }
}
impl Ord for OrdF32 {
    fn cmp(&self, other: &Self) -> std::cmp::Ordering {
        self.0.total_cmp(&other.0)
    }
}

/// Core greedy best-first beam search on a flat proximity graph.
///
/// Returns up to `k` results sorted nearest-first.
pub fn beam_search(
    graph: &FlatGraph,
    query: &[f32],
    k: usize,
    ef: usize,
    entry: usize,
) -> Vec<SearchResult> {
    if k == 0 {
        return Vec::new();
    }
    assert!(graph.n > 0, "graph must not be empty");
    assert!(entry < graph.n, "entry point is outside the graph");
    assert_eq!(query.len(), graph.config.dims, "query dimension mismatch");
    let ef = ef.max(k);
    let mut visited = vec![false; graph.n];
    visited[entry] = true;

    // Min-heap: (distance, node_id) — we always pop the closest unvisited.
    let mut candidates: BinaryHeap<Reverse<(OrdF32, u32)>> = BinaryHeap::new();
    // Max-heap of size `ef` tracking the best results found so far.
    let mut results: BinaryHeap<(OrdF32, u32)> = BinaryHeap::new();

    let entry_dist = l2_sq(query, graph.vec(entry));
    candidates.push(Reverse((OrdF32(entry_dist), entry as u32)));
    results.push((OrdF32(entry_dist), entry as u32));

    while let Some(Reverse((OrdF32(cand_dist), cand_id))) = candidates.pop() {
        // Termination: if the closest candidate is farther than our ef-th result, stop.
        if results.len() >= ef {
            if let Some(&(OrdF32(worst_result), _)) = results.peek() {
                if cand_dist > worst_result {
                    break;
                }
            }
        }

        // Expand neighbors.
        for &nb in &graph.neighbors[cand_id as usize] {
            let nb_usize = nb as usize;
            if visited[nb_usize] {
                continue;
            }
            visited[nb_usize] = true;

            let nb_dist = l2_sq(query, graph.vec(nb_usize));
            candidates.push(Reverse((OrdF32(nb_dist), nb)));

            if results.len() < ef {
                results.push((OrdF32(nb_dist), nb));
            } else if let Some(&(OrdF32(worst), _)) = results.peek() {
                if nb_dist < worst {
                    results.pop();
                    results.push((OrdF32(nb_dist), nb));
                }
            }
        }
    }

    // Convert to sorted (nearest-first) output, truncate to k.
    let mut out: Vec<SearchResult> = results
        .into_iter()
        .map(|(OrdF32(d), id)| SearchResult { id, dist_sq: d })
        .collect();
    out.sort_by(|a, b| a.dist_sq.total_cmp(&b.dist_sq));
    out.truncate(k);
    out
}

// ─── Variant 1: FixedEfSearch ────────────────────────────────────────────────

/// Baseline: fixed beam width, ignores recall target.
pub struct FixedEfSearch<'g> {
    pub graph: &'g FlatGraph,
    pub ef: usize,
    pub entry: usize,
}

impl<'g> RecallTargetedSearch for FixedEfSearch<'g> {
    fn search_with_target(
        &self,
        query: &[f32],
        k: usize,
        _recall_target: f32,
    ) -> Vec<SearchResult> {
        beam_search(self.graph, query, k, self.ef, self.entry)
    }

    fn effective_ef_for_target(&self, _recall_target: f32, _k: usize) -> Option<usize> {
        None // Fixed; target is ignored.
    }
}

// ─── Variant 2: BinarySearchCalibrated ───────────────────────────────────────

/// Binary-searches ef at query time to find the minimum beam width that hits
/// the recall target (verified against provided ground truth).
///
/// This is primarily a theoretical baseline: it shows the oracle minimum ef
/// per query but requires ground truth — impractical in production.
pub struct BinarySearchCalibrated<'g> {
    pub graph: &'g FlatGraph,
    pub entry: usize,
    /// ef lower bound for binary search.
    pub ef_min: usize,
    /// ef upper bound for binary search.
    pub ef_max: usize,
    /// Ground truth per query (for recall measurement during search).
    pub ground_truth: &'g [Vec<u32>],
    /// Index of the current query (set before calling search_with_target).
    pub query_index: usize,
}

impl<'g> RecallTargetedSearch for BinarySearchCalibrated<'g> {
    fn search_with_target(&self, query: &[f32], k: usize, recall_target: f32) -> Vec<SearchResult> {
        let gt = self
            .ground_truth
            .get(self.query_index)
            .expect("query_index must identify a ground-truth row");
        let recall_target = recall_target.clamp(0.0, 1.0);
        let mut lo = self.ef_min;
        let mut hi = self.ef_max;
        let mut best = beam_search(self.graph, query, k, hi, self.entry);

        // Binary search: find smallest ef achieving recall ≥ target.
        while lo < hi {
            let mid = (lo + hi) / 2;
            let results = beam_search(self.graph, query, k, mid, self.entry);
            let ids: Vec<u32> = results.iter().map(|r| r.id).collect();
            let r = recall_at_k(&ids, gt);
            if r >= recall_target {
                best = results;
                hi = mid;
            } else {
                lo = mid + 1;
            }
        }
        best
    }

    fn effective_ef_for_target(&self, _recall_target: f32, _k: usize) -> Option<usize> {
        // Effective ef is query-dependent and cannot be determined without
        // running the oracle search for the actual query.
        None
    }
}

// ─── Variant 3: TableCalibratedSearch ────────────────────────────────────────

/// Pre-calibrated search: uses an offline-built ef→recall table to select ef
/// using an empirical table calibrated for a specific `k` and workload.
pub struct TableCalibratedSearch<'g> {
    pub graph: &'g FlatGraph,
    pub entry: usize,
    pub table: &'g CalibrationTable,
}

impl<'g> RecallTargetedSearch for TableCalibratedSearch<'g> {
    fn search_with_target(&self, query: &[f32], k: usize, recall_target: f32) -> Vec<SearchResult> {
        let ef = self.table.min_ef_for_target(recall_target, k);
        beam_search(self.graph, query, k, ef, self.entry)
    }

    fn effective_ef_for_target(&self, recall_target: f32, k: usize) -> Option<usize> {
        Some(self.table.min_ef_for_target(recall_target, k))
    }
}
