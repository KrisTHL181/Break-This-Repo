use std::cmp::Ordering;
use std::collections::{BinaryHeap, HashSet};

use crate::graph::FlatGraph;
use crate::{cosine_sim, l2_sq, BeamSearch, SearchResult, VecId};

// ─── ordered wrappers ─────────────────────────────────────────────────────────

#[derive(Clone, PartialEq)]
struct MinDist {
    id: VecId,
    dist: f32,
}
impl Eq for MinDist {}
impl PartialOrd for MinDist {
    fn partial_cmp(&self, o: &Self) -> Option<Ordering> {
        Some(self.cmp(o))
    }
}
impl Ord for MinDist {
    fn cmp(&self, o: &Self) -> Ordering {
        o.dist.total_cmp(&self.dist)
    }
}

#[derive(Clone, PartialEq)]
struct MaxDist {
    id: VecId,
    dist: f32,
}
impl Eq for MaxDist {}
impl PartialOrd for MaxDist {
    fn partial_cmp(&self, o: &Self) -> Option<Ordering> {
        Some(self.cmp(o))
    }
}
impl Ord for MaxDist {
    fn cmp(&self, o: &Self) -> Ordering {
        self.dist.total_cmp(&o.dist)
    }
}

// ─── shared helper ────────────────────────────────────────────────────────────

fn gcd(mut a: usize, mut b: usize) -> usize {
    while b != 0 {
        (a, b) = (b, a % b);
    }
    a
}

pub(crate) fn entry_points(graph: &FlatGraph, query: &[f32], n_entry: usize) -> Vec<(VecId, f32)> {
    let n = graph.len();
    assert!(n > 0, "graph must contain at least one vector");
    assert!(n_entry > 0, "n_entry must be greater than zero");
    assert_eq!(query.len(), graph.dim, "query dimension mismatch");

    let count = n_entry.min(n);
    // Choose a stride near n/count that is coprime with n. This guarantees
    // distinct deterministic entry points for every count <= n.
    let mut step = (n / count).max(1);
    while gcd(step, n) != 1 {
        step += 1;
    }
    (0..count)
        .map(|i| {
            let id = (i * step) % n;
            (id, l2_sq(query, graph.vec(id)))
        })
        .collect()
}

fn push_result(results: &mut BinaryHeap<MaxDist>, id: VecId, dist: f32, beam: usize) {
    results.push(MaxDist { id, dist });
    if results.len() > beam {
        results.pop();
    }
}

fn to_sorted_results(heap: BinaryHeap<MaxDist>, k: usize) -> Vec<SearchResult> {
    let mut v: Vec<SearchResult> = heap.into_iter().map(|r| (r.id, r.dist)).collect();
    v.sort_by(|a, b| a.1.total_cmp(&b.1));
    v.truncate(k);
    v
}

// ─── Variant 1: GreedyBeam ────────────────────────────────────────────────────

/// Standard greedy beam search.
/// Always expands the candidate nearest the query vector.
/// Baseline: maximum recall, minimum result diversity.
pub struct GreedyBeam<'a> {
    pub graph: &'a FlatGraph,
    pub n_entry: usize,
}

impl<'a> BeamSearch for GreedyBeam<'a> {
    fn name(&self) -> &str {
        "GreedyBeam"
    }

    fn search(&self, query: &[f32], k: usize, beam_width: usize) -> Vec<SearchResult> {
        if k == 0 {
            return Vec::new();
        }
        let beam = beam_width.max(k);
        let mut candidates: BinaryHeap<MinDist> = BinaryHeap::new();
        let mut results: BinaryHeap<MaxDist> = BinaryHeap::new();
        let mut visited: HashSet<VecId> = HashSet::new();

        for (id, dist) in entry_points(self.graph, query, self.n_entry) {
            candidates.push(MinDist { id, dist });
            push_result(&mut results, id, dist, beam);
            visited.insert(id);
        }

        while let Some(MinDist {
            id: cur,
            dist: cur_dist,
        }) = candidates.pop()
        {
            let worst = results.peek().map(|r| r.dist).unwrap_or(f32::MAX);
            if cur_dist > worst && results.len() >= beam {
                break;
            }
            for &nb in &self.graph.neighbors[cur] {
                if visited.contains(&nb) {
                    continue;
                }
                visited.insert(nb);
                let d = l2_sq(query, self.graph.vec(nb));
                let worst = results.peek().map(|r| r.dist).unwrap_or(f32::MAX);
                if results.len() < beam || d < worst {
                    candidates.push(MinDist { id: nb, dist: d });
                    push_result(&mut results, nb, d, beam);
                }
            }
        }

        to_sorted_results(results, k)
    }
}

// ─── Variant 2: MMRRerank ─────────────────────────────────────────────────────

/// Greedy-traversal + Maximum Marginal Relevance post-reranking.
///
/// Step 1: standard greedy beam search collects `beam_width` candidates.
/// Step 2: iterative MMR selection picks k items that balance:
///   - relevance  (proximity to query)
///   - diversity  (distance from already-selected items)
///
/// MMR score:
///   S(c) = λ · (−dist_q) + (1−λ) · min_{s∈selected} dist(c, s)
///
/// λ → 1.0 : pure relevance (degenerates to top-k by dist)
/// λ → 0.0 : pure diversity
///
/// Trade-off: recall@k decreases by sacrificing the closest duplicates;
/// mean pairwise distance of results increases.
pub struct MMRRerank<'a> {
    pub graph: &'a FlatGraph,
    pub n_entry: usize,
    pub lambda: f32,
}

impl<'a> BeamSearch for MMRRerank<'a> {
    fn name(&self) -> &str {
        "MMRRerank"
    }

    fn search(&self, query: &[f32], k: usize, beam_width: usize) -> Vec<SearchResult> {
        // Widen the pool to beam_width so MMR has more candidates to choose from.
        let pool_size = beam_width.max(k * 4);
        let greedy = GreedyBeam {
            graph: self.graph,
            n_entry: self.n_entry,
        };
        let mut pool = greedy.search(query, pool_size, pool_size);

        let mut selected: Vec<SearchResult> = Vec::with_capacity(k);

        while selected.len() < k && !pool.is_empty() {
            let best = self.mmr_pick(&pool, &selected);
            let item = pool.remove(best);
            selected.push(item);
        }

        selected
    }
}

impl<'a> MMRRerank<'a> {
    fn mmr_pick(&self, candidates: &[SearchResult], selected: &[SearchResult]) -> usize {
        let lambda = self.lambda;
        let mut best_score = f32::NEG_INFINITY;
        let mut best_idx = 0;

        // Normalise relevance to [0, 1] across the candidate pool.
        let max_dist = candidates
            .iter()
            .map(|&(_, d)| d)
            .fold(0.0_f32, f32::max)
            .max(1e-6);

        for (i, &(id, dist_q)) in candidates.iter().enumerate() {
            let v = self.graph.vec(id);
            // Relevance: 1.0 = closest to query, 0.0 = farthest in pool.
            let relevance = 1.0 - dist_q / max_dist;

            // Angular diversity is independently bounded in [0, 1].
            let diversity = if selected.is_empty() {
                1.0_f32
            } else {
                let min_d = selected
                    .iter()
                    .map(|&(sid, _)| {
                        ((1.0 - cosine_sim(v, self.graph.vec(sid))) * 0.5).clamp(0.0, 1.0)
                    })
                    .fold(f32::INFINITY, f32::min);
                min_d
            };

            let score = lambda * relevance + (1.0 - lambda) * diversity;
            if score > best_score {
                best_score = score;
                best_idx = i;
            }
        }
        best_idx
    }
}

// ─── Variant 3: CoherenceBeam ─────────────────────────────────────────────────

/// Coherence-pruned beam search.
///
/// During traversal: when adding a new neighbour to the candidate heap,
/// discard it if its cosine similarity to any recently-expanded node
/// exceeds `coherence_threshold`.
///
/// Effect: prevents repeatedly exploring the same dense local region,
/// freeing beam budget for geometrically distinct sub-graphs.
/// Best on datasets with overlapping clusters; neutral on uniform data.
pub struct CoherenceBeam<'a> {
    pub graph: &'a FlatGraph,
    pub n_entry: usize,
    /// Cosine similarity threshold in [0, 1]. 0.9+ = conservative pruning.
    pub coherence_threshold: f32,
}

const CBEAM_HISTORY: usize = 8;

impl<'a> BeamSearch for CoherenceBeam<'a> {
    fn name(&self) -> &str {
        "CoherenceBeam"
    }

    fn search(&self, query: &[f32], k: usize, beam_width: usize) -> Vec<SearchResult> {
        if k == 0 {
            return Vec::new();
        }
        let beam = beam_width.max(k);
        let mut candidates: BinaryHeap<MinDist> = BinaryHeap::new();
        let mut results: BinaryHeap<MaxDist> = BinaryHeap::new();
        let mut visited: HashSet<VecId> = HashSet::new();
        let mut expanded_hist: Vec<VecId> = Vec::with_capacity(CBEAM_HISTORY);

        for (id, dist) in entry_points(self.graph, query, self.n_entry) {
            candidates.push(MinDist { id, dist });
            push_result(&mut results, id, dist, beam);
            visited.insert(id);
        }

        while let Some(MinDist {
            id: cur,
            dist: cur_dist,
        }) = candidates.pop()
        {
            let worst = results.peek().map(|r| r.dist).unwrap_or(f32::MAX);
            if cur_dist > worst && results.len() >= beam {
                break;
            }

            expanded_hist.push(cur);
            if expanded_hist.len() > CBEAM_HISTORY {
                expanded_hist.remove(0);
            }

            for &nb in &self.graph.neighbors[cur] {
                if visited.contains(&nb) {
                    continue;
                }
                visited.insert(nb);

                // Coherence gate: skip if direction already well-covered.
                let v_nb = self.graph.vec(nb);
                let coherent = expanded_hist
                    .iter()
                    .any(|&eid| cosine_sim(v_nb, self.graph.vec(eid)) > self.coherence_threshold);
                if coherent {
                    continue;
                }

                let d = l2_sq(query, v_nb);
                let worst = results.peek().map(|r| r.dist).unwrap_or(f32::MAX);
                if results.len() < beam || d < worst {
                    candidates.push(MinDist { id: nb, dist: d });
                    push_result(&mut results, nb, d, beam);
                }
            }
        }

        to_sorted_results(results, k)
    }
}
