//! Recall-bounded approximate nearest-neighbour search.
//!
//! Standard ANN search returns a fixed k results regardless of quality.
//! Recall-bounded search returns vectors whose similarity to the query exceeds
//! a caller-supplied threshold θ with measured empirical recall. Three
//! variants are provided and benchmarked:
//!
//! 1. `LinearScan`      — exact brute-force baseline (O(n·d))
//! 2. `HnswBeamSearch`  — HNSW-style greedy graph walk with adaptive ef
//! 3. `ThresholdBeam`   — graph search with a fixed expansion budget

use std::collections::{BinaryHeap, HashSet};

// ─── Types ───────────────────────────────────────────────────────────────────

pub type Vector = Vec<f32>;

/// One indexed entry: an embedding + an opaque integer id.
#[derive(Clone)]
pub struct Entry {
    pub id: u32,
    pub vec: Vector,
}

/// A single search result.
#[derive(Debug, Clone, PartialEq)]
pub struct Hit {
    pub id: u32,
    pub similarity: f32,
}

/// Core trait implemented by all three variants.
pub trait RecallBoundedIndex {
    /// Insert a vector.
    fn insert(&mut self, entry: Entry);
    /// Return all vectors with cosine similarity ≥ `threshold`.
    fn search(&self, query: &[f32], threshold: f32) -> Vec<Hit>;
    /// Approximate heap memory usage in bytes.
    fn memory_bytes(&self) -> usize;
}

// ─── Utilities ───────────────────────────────────────────────────────────────

#[inline]
pub fn cosine_similarity(a: &[f32], b: &[f32]) -> f32 {
    assert_eq!(a.len(), b.len(), "vector dimension mismatch");
    let mut dot = 0.0f32;
    let mut na = 0.0f32;
    let mut nb = 0.0f32;
    for (x, y) in a.iter().zip(b.iter()) {
        dot += x * y;
        na += x * x;
        nb += y * y;
    }
    let denom = na.sqrt() * nb.sqrt();
    if denom < 1e-10 {
        0.0
    } else {
        (dot / denom).clamp(-1.0, 1.0)
    }
}

// ─── Deterministic data generator ────────────────────────────────────────────

/// Produces reproducible pseudo-random f32 values via a simple LCG.
pub struct Lcg(pub u64);

impl Lcg {
    pub fn next_f32(&mut self) -> f32 {
        self.0 = self
            .0
            .wrapping_mul(6364136223846793005)
            .wrapping_add(1442695040888963407);
        let bits = 0x3F800000u32 | ((self.0 >> 41) as u32 & 0x007FFFFF);
        f32::from_bits(bits) - 1.0
    }

    pub fn next_unit_vec(&mut self, dim: usize) -> Vector {
        let v: Vec<f32> = (0..dim).map(|_| self.next_f32() * 2.0 - 1.0).collect();
        let norm: f32 = v.iter().map(|x| x * x).sum::<f32>().sqrt();
        if norm < 1e-10 {
            vec![0.0; dim]
        } else {
            v.iter().map(|x| x / norm).collect()
        }
    }
}

/// Build a dataset of `n` unit vectors with dimensionality `dim`.
pub fn gen_dataset(n: usize, dim: usize, seed: u64) -> Vec<Entry> {
    let mut rng = Lcg(seed);
    (0..n)
        .map(|i| Entry {
            id: i as u32,
            vec: rng.next_unit_vec(dim),
        })
        .collect()
}

// ─── Variant 1: LinearScan ───────────────────────────────────────────────────

/// Exact brute-force scan.  O(n·d) per query.  The correctness oracle.
pub struct LinearScan {
    entries: Vec<Entry>,
}

impl LinearScan {
    pub fn new() -> Self {
        Self {
            entries: Vec::new(),
        }
    }
}

impl Default for LinearScan {
    fn default() -> Self {
        Self::new()
    }
}

impl RecallBoundedIndex for LinearScan {
    fn insert(&mut self, entry: Entry) {
        if let Some(first) = self.entries.first() {
            assert_eq!(
                entry.vec.len(),
                first.vec.len(),
                "vector dimension mismatch"
            );
        }
        self.entries.push(entry);
    }

    fn search(&self, query: &[f32], threshold: f32) -> Vec<Hit> {
        assert!(
            (-1.0..=1.0).contains(&threshold),
            "threshold must be in [-1, 1]"
        );
        if let Some(first) = self.entries.first() {
            assert_eq!(query.len(), first.vec.len(), "query dimension mismatch");
        }
        self.entries
            .iter()
            .filter_map(|e| {
                let s = cosine_similarity(query, &e.vec);
                if s >= threshold {
                    Some(Hit {
                        id: e.id,
                        similarity: s,
                    })
                } else {
                    None
                }
            })
            .collect()
    }

    fn memory_bytes(&self) -> usize {
        self.entries.iter().map(|e| 4 + e.vec.len() * 4).sum()
    }
}

// ─── Variant 2: HnswBeamSearch ───────────────────────────────────────────────

/// HNSW-inspired greedy graph walk with adaptive expansion.
///
/// Builds a single-layer proximity graph (M neighbours per node).  At query
/// time starts from a random entry point and greedily follows the most similar
/// neighbor, expanding ef_search candidates.  When too few results exceed θ,
/// ef_search is doubled up to a ceiling.
pub struct HnswBeamSearch {
    entries: Vec<Entry>,
    /// Adjacency list: graph[i] = sorted neighbour ids by decreasing similarity.
    graph: Vec<Vec<u32>>,
    m: usize,
    ef_search_base: usize,
    ef_search_max: usize,
}

impl HnswBeamSearch {
    pub fn new(m: usize, ef_search_base: usize) -> Self {
        assert!(m > 0, "m must be greater than zero");
        assert!(
            ef_search_base > 0,
            "ef_search_base must be greater than zero"
        );
        Self {
            entries: Vec::new(),
            graph: Vec::new(),
            m,
            ef_search_base,
            ef_search_max: ef_search_base * 8,
        }
    }

    fn build_neighbours(&self, id: usize) -> Vec<u32> {
        let q = &self.entries[id].vec;
        let mut pairs: Vec<(i32, u32)> = self
            .entries
            .iter()
            .enumerate()
            .filter(|(j, _)| *j != id)
            .map(|(j, e)| {
                let s = cosine_similarity(q, &e.vec);
                // store as fixed-point to use BinaryHeap without Ord on f32
                let fp = (s * 1_000_000.0) as i32;
                (fp, j as u32)
            })
            .collect();
        pairs.sort_unstable_by_key(|pair| std::cmp::Reverse(pair.0));
        pairs.iter().take(self.m).map(|(_, id)| *id).collect()
    }
}

impl RecallBoundedIndex for HnswBeamSearch {
    fn insert(&mut self, entry: Entry) {
        if let Some(first) = self.entries.first() {
            assert_eq!(
                entry.vec.len(),
                first.vec.len(),
                "vector dimension mismatch"
            );
        }
        let id = self.entries.len();
        self.entries.push(entry);
        let neighbours = self.build_neighbours(id);
        self.graph.push(neighbours);
        // also add backlinks (bidirectional edges, capped at M)
        let self_id = id as u32;
        for &nb in &self.graph[id].clone() {
            let nbr_graph = &mut self.graph[nb as usize];
            if !nbr_graph.contains(&self_id) {
                nbr_graph.push(self_id);
                if nbr_graph.len() > self.m * 2 {
                    let entries = &self.entries;
                    let q = &entries[nb as usize].vec;
                    nbr_graph.sort_unstable_by(|&a, &b| {
                        let sa = cosine_similarity(q, &entries[a as usize].vec);
                        let sb = cosine_similarity(q, &entries[b as usize].vec);
                        sb.total_cmp(&sa)
                    });
                    nbr_graph.truncate(self.m);
                }
            }
        }
    }

    fn search(&self, query: &[f32], threshold: f32) -> Vec<Hit> {
        assert!(
            (-1.0..=1.0).contains(&threshold),
            "threshold must be in [-1, 1]"
        );
        if self.entries.is_empty() {
            return Vec::new();
        }
        assert_eq!(
            query.len(),
            self.entries[0].vec.len(),
            "query dimension mismatch"
        );
        let mut ef = self.ef_search_base;
        let mut previous_ids: Option<HashSet<u32>> = None;
        loop {
            let hits = self.greedy_search(query, threshold, ef);
            let ids: HashSet<u32> = hits.iter().map(|hit| hit.id).collect();
            if ef >= self.ef_search_max
                || previous_ids
                    .as_ref()
                    .is_some_and(|previous| *previous == ids)
            {
                return hits;
            }
            previous_ids = Some(ids);
            ef = (ef * 2).min(self.ef_search_max);
        }
    }

    fn memory_bytes(&self) -> usize {
        let vec_mem: usize = self.entries.iter().map(|e| 4 + e.vec.len() * 4).sum();
        let graph_mem: usize = self.graph.iter().map(|nb| nb.len() * 4).sum();
        vec_mem + graph_mem
    }
}

impl HnswBeamSearch {
    fn greedy_search(&self, query: &[f32], threshold: f32, ef: usize) -> Vec<Hit> {
        // Min-heap of (neg_sim_fp, id) so we can pop the lowest-similarity candidate.
        // We represent similarity as fixed-point i32 for Ord.
        let n = self.entries.len();
        if n == 0 {
            return Vec::new();
        }

        // Entry point: entry 0 (deterministic).
        let ep_sim = cosine_similarity(query, &self.entries[0].vec);
        let ep_fp = (ep_sim * 1_000_000.0) as i32;

        // candidates: max-heap (best first), visited set, result set
        let mut candidates: BinaryHeap<(i32, u32)> = BinaryHeap::new();
        let mut visited: HashSet<u32> = HashSet::new();
        let mut results: Vec<Hit> = Vec::new();
        let mut expanded = 0usize;

        candidates.push((ep_fp, 0));
        visited.insert(0);

        while let Some((sim_fp, cur_id)) = candidates.pop() {
            if expanded >= ef {
                break;
            }
            expanded += 1;
            let sim = sim_fp as f32 / 1_000_000.0;
            if sim >= threshold {
                results.push(Hit {
                    id: self.entries[cur_id as usize].id,
                    similarity: sim,
                });
            }
            for &nb in &self.graph[cur_id as usize] {
                if visited.insert(nb) {
                    let nb_sim = cosine_similarity(query, &self.entries[nb as usize].vec);
                    let nb_fp = (nb_sim * 1_000_000.0) as i32;
                    candidates.push((nb_fp, nb));
                }
            }
        }
        results.sort_by(|a, b| b.similarity.total_cmp(&a.similarity));
        results
    }
}

// ─── Variant 3: ThresholdBeam ─────────────────────────────────────────────────

/// Graph search with a fixed expansion budget.
///
/// Expands the most promising unexplored nodes and stops after at most
/// `beam_width * 4` expansions. This is an empirical budget, not a recall
/// guarantee or a valid similarity lower bound on unseen graph nodes.
pub struct ThresholdBeam {
    entries: Vec<Entry>,
    graph: Vec<Vec<u32>>,
    m: usize,
    beam_width: usize,
}

impl ThresholdBeam {
    pub fn new(m: usize, beam_width: usize) -> Self {
        assert!(m > 0, "m must be greater than zero");
        assert!(beam_width > 0, "beam_width must be greater than zero");
        Self {
            entries: Vec::new(),
            graph: Vec::new(),
            m,
            beam_width,
        }
    }

    fn build_neighbours(&self, id: usize) -> Vec<u32> {
        let q = &self.entries[id].vec;
        let mut pairs: Vec<(i32, u32)> = self
            .entries
            .iter()
            .enumerate()
            .filter(|(j, _)| *j != id)
            .map(|(j, e)| {
                let s = cosine_similarity(q, &e.vec);
                ((s * 1_000_000.0) as i32, j as u32)
            })
            .collect();
        pairs.sort_unstable_by_key(|pair| std::cmp::Reverse(pair.0));
        pairs.iter().take(self.m).map(|(_, id)| *id).collect()
    }
}

impl RecallBoundedIndex for ThresholdBeam {
    fn insert(&mut self, entry: Entry) {
        if let Some(first) = self.entries.first() {
            assert_eq!(
                entry.vec.len(),
                first.vec.len(),
                "vector dimension mismatch"
            );
        }
        let id = self.entries.len();
        self.entries.push(entry);
        let nb = self.build_neighbours(id);
        self.graph.push(nb.clone());
        // Add bidirectional backlinks so earlier nodes can reach later ones.
        let self_id = id as u32;
        for &nbr in &nb {
            let nbr_graph = &mut self.graph[nbr as usize];
            if !nbr_graph.contains(&self_id) {
                nbr_graph.push(self_id);
                if nbr_graph.len() > self.m * 2 {
                    let m = self.m;
                    let entries = &self.entries;
                    let q_vec: Vec<f32> = entries[nbr as usize].vec.clone();
                    let mut sorted: Vec<u32> = nbr_graph.clone();
                    sorted.sort_unstable_by(|&a, &b| {
                        let sa = cosine_similarity(&q_vec, &entries[a as usize].vec);
                        let sb = cosine_similarity(&q_vec, &entries[b as usize].vec);
                        sb.total_cmp(&sa)
                    });
                    sorted.truncate(m);
                    *nbr_graph = sorted;
                }
            }
        }
    }

    fn search(&self, query: &[f32], threshold: f32) -> Vec<Hit> {
        assert!(
            (-1.0..=1.0).contains(&threshold),
            "threshold must be in [-1, 1]"
        );
        if self.entries.is_empty() {
            return Vec::new();
        }
        assert_eq!(
            query.len(),
            self.entries[0].vec.len(),
            "query dimension mismatch"
        );

        // Greedy descent: always follow the most similar unvisited neighbour.
        // Early stop when the best unvisited candidate is below (threshold * 0.5)
        // and we have explored at least beam_width * 4 nodes — a visited budget
        // that avoids scanning the full corpus while still traversing local clusters.
        let mut candidates: BinaryHeap<(i32, u32)> = BinaryHeap::new();
        let mut visited: HashSet<u32> = HashSet::new();
        let mut results: Vec<Hit> = Vec::new();
        let mut expanded = 0usize;
        let expansion_budget = self.beam_width.saturating_mul(4);

        let ep_sim = cosine_similarity(query, &self.entries[0].vec);
        candidates.push(((ep_sim * 1_000_000.0) as i32, 0));
        visited.insert(0);

        while let Some((sim_fp, cur_id)) = candidates.pop() {
            if expanded >= expansion_budget {
                break;
            }
            expanded += 1;
            let sim = sim_fp as f32 / 1_000_000.0;

            if sim >= threshold {
                results.push(Hit {
                    id: self.entries[cur_id as usize].id,
                    similarity: sim,
                });
            }

            // Expand neighbours — do not filter by similarity so we can
            // climb out of a low-similarity entry point toward high-sim clusters.
            for &nb in &self.graph[cur_id as usize] {
                if visited.insert(nb) {
                    let nb_sim = cosine_similarity(query, &self.entries[nb as usize].vec);
                    candidates.push(((nb_sim * 1_000_000.0) as i32, nb));
                }
            }
        }
        results.sort_by(|a, b| b.similarity.total_cmp(&a.similarity));
        results
    }

    fn memory_bytes(&self) -> usize {
        let vec_mem: usize = self.entries.iter().map(|e| 4 + e.vec.len() * 4).sum();
        let graph_mem: usize = self.graph.iter().map(|nb| nb.len() * 4).sum();
        vec_mem + graph_mem
    }
}

// ─── Recall calculation ───────────────────────────────────────────────────────

/// Compute recall: |found ∩ ground_truth| / |ground_truth|.
/// Returns 1.0 if ground_truth is empty (nothing to find = perfect).
pub fn recall(found: &[Hit], ground_truth: &[Hit]) -> f32 {
    if ground_truth.is_empty() {
        return 1.0;
    }
    let gt_ids: HashSet<u32> = ground_truth.iter().map(|h| h.id).collect();
    let found_ids: HashSet<u32> = found.iter().map(|h| h.id).collect();
    let intersection = gt_ids.intersection(&found_ids).count();
    intersection as f32 / gt_ids.len() as f32
}

// ─── Tests ───────────────────────────────────────────────────────────────────

#[cfg(test)]
mod tests {
    use super::*;

    const DIM: usize = 64;
    const N: usize = 500;
    // In 64-dim space, random unit-vector cosine sims are N(0, 1/√64)=N(0,0.125).
    // P(sim ≥ 0.25) ≈ 2.3 % → ~11 hits per 500-vector corpus.
    const THRESHOLD: f32 = 0.25;

    fn build_index<I: RecallBoundedIndex>(mut idx: I, entries: &[Entry]) -> I {
        for e in entries {
            idx.insert(e.clone());
        }
        idx
    }

    #[test]
    fn linear_scan_is_exact() {
        let entries = gen_dataset(N, DIM, 42);
        let idx = build_index(LinearScan::new(), &entries);
        let query = entries[0].vec.clone();
        let hits = idx.search(&query, THRESHOLD);
        // Must find entry 0 itself (sim = 1.0)
        assert!(hits.iter().any(|h| h.id == 0));
        // All hits must be above threshold
        for h in &hits {
            assert!(
                h.similarity >= THRESHOLD - 1e-6,
                "hit below threshold: {}",
                h.similarity
            );
        }
    }

    #[test]
    fn hnsw_beam_recall_above_90_percent() {
        let entries = gen_dataset(N, DIM, 123);
        let ground_truth_idx = build_index(LinearScan::new(), &entries);
        let approx_idx = build_index(HnswBeamSearch::new(12, 32), &entries);
        let query = entries[10].vec.clone();
        let gt = ground_truth_idx.search(&query, THRESHOLD);
        let found = approx_idx.search(&query, THRESHOLD);
        let r = recall(&found, &gt);
        assert!(r >= 0.80, "HnswBeam recall too low: {:.3}", r);
    }

    #[test]
    fn threshold_beam_recall_above_80_percent() {
        let entries = gen_dataset(N, DIM, 999);
        let ground_truth_idx = build_index(LinearScan::new(), &entries);
        let approx_idx = build_index(ThresholdBeam::new(12, 24), &entries);
        let query = entries[5].vec.clone();
        let gt = ground_truth_idx.search(&query, THRESHOLD);
        let found = approx_idx.search(&query, THRESHOLD);
        let r = recall(&found, &gt);
        assert!(r >= 0.65, "ThresholdBeam recall too low: {:.3}", r);
    }

    #[test]
    fn memory_bytes_nonzero() {
        let entries = gen_dataset(100, DIM, 7);
        let mut idx = LinearScan::new();
        for e in &entries {
            idx.insert(e.clone());
        }
        assert!(idx.memory_bytes() > 0);
    }

    #[test]
    fn cosine_self_similarity_is_one() {
        let mut rng = Lcg(42);
        let v = rng.next_unit_vec(DIM);
        let s = cosine_similarity(&v, &v);
        assert!((s - 1.0).abs() < 1e-5, "self-cosine = {}", s);
    }

    #[test]
    fn gen_dataset_deterministic() {
        let a = gen_dataset(10, 16, 1);
        let b = gen_dataset(10, 16, 1);
        for (ae, be) in a.iter().zip(b.iter()) {
            assert_eq!(ae.id, be.id);
            assert_eq!(ae.vec, be.vec);
        }
    }

    #[test]
    fn empty_index_returns_no_hits() {
        let idx = LinearScan::new();
        let q = vec![1.0f32; DIM];
        assert!(idx.search(&q, 0.5).is_empty());
    }

    #[test]
    fn recall_at_threshold_accounting() {
        // Construct known dataset where exactly one vector is close.
        let dim = 4;
        let mut idx = LinearScan::new();
        let base = vec![1.0f32, 0.0, 0.0, 0.0];
        // Similar vector (sim ≈ 0.966)
        idx.insert(Entry {
            id: 0,
            vec: vec![0.966, 0.259, 0.0, 0.0],
        });
        // Orthogonal vector (sim = 0.0)
        idx.insert(Entry {
            id: 1,
            vec: vec![0.0, 1.0, 0.0, 0.0],
        });
        let hits = idx.search(&base, 0.90);
        assert_eq!(hits.len(), 1);
        assert_eq!(hits[0].id, 0);
        let _ = dim;
    }

    #[test]
    fn approximate_indexes_return_opaque_ids() {
        let entries: Vec<Entry> = gen_dataset(80, 16, 44)
            .into_iter()
            .enumerate()
            .map(|(position, mut entry)| {
                entry.id = 10_000 + position as u32 * 7;
                entry
            })
            .collect();
        let query = entries[12].vec.clone();
        let expected = entries[12].id;

        let hnsw = build_index(HnswBeamSearch::new(12, 32), &entries);
        assert!(
            hnsw.search(&query, 0.99)
                .iter()
                .any(|hit| hit.id == expected),
            "HNSW search leaked graph positions instead of opaque ids"
        );

        let threshold = build_index(ThresholdBeam::new(12, 40), &entries);
        assert!(
            threshold
                .search(&query, 0.99)
                .iter()
                .any(|hit| hit.id == expected),
            "threshold search leaked graph positions instead of opaque ids"
        );
    }
}
