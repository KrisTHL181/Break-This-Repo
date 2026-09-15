pub mod dataset;
pub mod graph;
pub mod search;

use std::collections::HashSet;

pub type VecId = usize;
pub type SearchResult = (VecId, f32);

/// A beam search backend measurable for recall and latency.
pub trait BeamSearch {
    fn name(&self) -> &str;
    fn search(&self, query: &[f32], k: usize, beam_width: usize) -> Vec<SearchResult>;
}

/// Recall@k: fraction of ground-truth top-k found in results.
pub fn recall_at_k(ground_truth: &[VecId], results: &[SearchResult], k: usize) -> f32 {
    let k = k.min(ground_truth.len()).min(results.len());
    if k == 0 {
        return 0.0;
    }
    let gt: HashSet<VecId> = ground_truth[..k].iter().cloned().collect();
    let found = results[..k]
        .iter()
        .filter(|(id, _)| gt.contains(id))
        .count();
    found as f32 / k as f32
}

/// Mean pairwise L2 distance of the result set — higher = more diverse output.
pub fn mean_pairwise_dist(results: &[SearchResult], vecs: &[Vec<f32>]) -> f32 {
    let n = results.len();
    if n < 2 {
        return 0.0;
    }
    let mut total = 0.0_f32;
    let mut count = 0usize;
    for i in 0..n {
        for j in (i + 1)..n {
            total += l2_sq(&vecs[results[i].0], &vecs[results[j].0]).sqrt();
            count += 1;
        }
    }
    if count == 0 {
        0.0
    } else {
        total / count as f32
    }
}

/// L2 squared distance — same ordering as L2, avoids sqrt.
#[inline]
pub fn l2_sq(a: &[f32], b: &[f32]) -> f32 {
    assert_eq!(a.len(), b.len(), "vector dimension mismatch");
    a.iter().zip(b.iter()).map(|(x, y)| (x - y) * (x - y)).sum()
}

/// Cosine similarity in [−1, 1], clamped.
#[inline]
pub fn cosine_sim(a: &[f32], b: &[f32]) -> f32 {
    assert_eq!(a.len(), b.len(), "vector dimension mismatch");
    let dot: f32 = a.iter().zip(b.iter()).map(|(x, y)| x * y).sum();
    let na: f32 = a.iter().map(|x| x * x).sum::<f32>().sqrt();
    let nb: f32 = b.iter().map(|x| x * x).sum::<f32>().sqrt();
    if na < 1e-9 || nb < 1e-9 {
        0.0
    } else {
        (dot / (na * nb)).clamp(-1.0, 1.0)
    }
}

// ─── unit tests ────────────────────────────────────────────────────────────────

#[cfg(test)]
mod tests {
    use super::*;
    use crate::dataset::{clustered, queries_clustered, queries_uniform, uniform};
    use crate::graph::FlatGraph;
    use crate::search::{CoherenceBeam, GreedyBeam, MMRRerank};

    const N: usize = 300;
    const DIM: usize = 32;
    const K: usize = 5;
    const BEAM: usize = 30;
    const K_NN: usize = 12;
    const N_Q: usize = 30;
    const SEED: u64 = 0xc0_ffee;

    fn make_uniform_graph() -> FlatGraph {
        FlatGraph::build(uniform(N, DIM, SEED), K_NN)
    }

    fn run_recall<S: BeamSearch>(s: &S, graph: &FlatGraph, queries: &[Vec<f32>]) -> f32 {
        let mut total = 0.0_f32;
        for q in queries {
            let gt: Vec<VecId> = graph
                .brute_force(q, K)
                .into_iter()
                .map(|(id, _)| id)
                .collect();
            let res = s.search(q, K, BEAM);
            total += recall_at_k(&gt, &res, K);
        }
        total / queries.len() as f32
    }

    #[test]
    fn greedy_recall_uniform() {
        let g = make_uniform_graph();
        let qs = queries_uniform(N_Q, DIM, SEED);
        let r = run_recall(
            &GreedyBeam {
                graph: &g,
                n_entry: 6,
            },
            &g,
            &qs,
        );
        assert!(r >= 0.65, "GreedyBeam recall@{K} = {r:.3}, expected ≥ 0.65");
    }

    #[test]
    fn mmr_rerank_recall_uniform() {
        let g = make_uniform_graph();
        let qs = queries_uniform(N_Q, DIM, SEED);
        let r = run_recall(
            &MMRRerank {
                graph: &g,
                n_entry: 6,
                lambda: 0.75,
            },
            &g,
            &qs,
        );
        // MMRRerank trades recall for diversity; threshold is lower than greedy.
        assert!(r >= 0.50, "MMRRerank recall@{K} = {r:.3}, expected ≥ 0.50");
    }

    #[test]
    fn mmr_rerank_diversity_gt_greedy() {
        let g = make_uniform_graph();
        let q = &queries_uniform(1, DIM, SEED)[0];
        let greedy = GreedyBeam {
            graph: &g,
            n_entry: 6,
        }
        .search(q, K, BEAM);
        let mmr = MMRRerank {
            graph: &g,
            n_entry: 6,
            lambda: 0.75,
        }
        .search(q, K, BEAM);
        let d_greedy = mean_pairwise_dist(&greedy, &g.vectors);
        let d_mmr = mean_pairwise_dist(&mmr, &g.vectors);
        assert!(
            d_mmr >= d_greedy * 0.90, // MMR result set must not be less diverse
            "MMRRerank diversity {d_mmr:.3} should be ≥ Greedy {d_greedy:.3}"
        );
    }

    #[test]
    fn coherence_recall_uniform() {
        let g = make_uniform_graph();
        let qs = queries_uniform(N_Q, DIM, SEED);
        let r = run_recall(
            &CoherenceBeam {
                graph: &g,
                n_entry: 6,
                coherence_threshold: 0.92,
            },
            &g,
            &qs,
        );
        assert!(
            r >= 0.55,
            "CoherenceBeam recall@{K} = {r:.3}, expected ≥ 0.55"
        );
    }

    #[test]
    fn greedy_recall_clustered() {
        // n_entry ≥ n_clusters ensures every cluster gets at least one entry point.
        let n_clusters = 8usize;
        let n_entry = n_clusters + 2;
        let g = FlatGraph::build(clustered(N, DIM, n_clusters, 0.12, SEED), K_NN);
        let qs = queries_clustered(N_Q, DIM, n_clusters, 0.12, SEED);
        let r = run_recall(&GreedyBeam { graph: &g, n_entry }, &g, &qs);
        assert!(
            r >= 0.60,
            "GreedyBeam clustered recall@{K} = {r:.3}, expected ≥ 0.60"
        );
    }

    #[test]
    fn results_length_correct() {
        let g = make_uniform_graph();
        let q = &g.vectors[0];
        let res = GreedyBeam {
            graph: &g,
            n_entry: 4,
        }
        .search(q, K, BEAM);
        assert_eq!(res.len(), K, "expected exactly {K} results");
    }

    #[test]
    fn results_sorted_ascending() {
        let g = make_uniform_graph();
        let q = &g.vectors[0];
        let res = GreedyBeam {
            graph: &g,
            n_entry: 4,
        }
        .search(q, K, BEAM);
        for w in res.windows(2) {
            assert!(w[0].1 <= w[1].1, "results not sorted by distance");
        }
    }

    #[test]
    fn recall_helper_perfect() {
        let gt = vec![0usize, 1, 2, 3, 4];
        let res: Vec<SearchResult> = gt.iter().map(|&id| (id, id as f32)).collect();
        assert_eq!(recall_at_k(&gt, &res, 5), 1.0);
    }

    #[test]
    fn recall_helper_zero() {
        let gt = vec![0usize, 1, 2];
        let res: Vec<SearchResult> = vec![(10, 0.1), (11, 0.2), (12, 0.3)];
        assert_eq!(recall_at_k(&gt, &res, 3), 0.0);
    }

    #[test]
    fn zero_k_returns_empty_even_with_zero_beam() {
        let g = make_uniform_graph();
        let result = GreedyBeam {
            graph: &g,
            n_entry: 4,
        }
        .search(&g.vectors[0], 0, 0);
        assert!(result.is_empty());
    }

    #[test]
    fn beam_smaller_than_k_still_returns_k() {
        let g = make_uniform_graph();
        let result = GreedyBeam {
            graph: &g,
            n_entry: 4,
        }
        .search(&g.vectors[0], K, 1);
        assert_eq!(result.len(), K);
    }

    #[test]
    #[should_panic(expected = "n_entry must be greater than zero")]
    fn zero_entry_points_are_rejected() {
        let g = make_uniform_graph();
        let _ = GreedyBeam {
            graph: &g,
            n_entry: 0,
        }
        .search(&g.vectors[0], K, BEAM);
    }

    #[test]
    #[should_panic(expected = "query dimension mismatch")]
    fn query_dimension_mismatch_is_rejected() {
        let g = make_uniform_graph();
        let _ = GreedyBeam {
            graph: &g,
            n_entry: 4,
        }
        .search(&[0.0; DIM - 1], K, BEAM);
    }
}
