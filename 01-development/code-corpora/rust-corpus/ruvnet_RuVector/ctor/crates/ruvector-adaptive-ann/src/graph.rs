//! Flat navigable small-world proximity graph.
//!
//! Single-layer k-NN graph with optional long-jump edges — equivalent to
//! HNSW layer-0 plus the upper-layer shortcuts collapsed into direct edges.
//!
//! ## Why not multi-layer HNSW?
//!
//! The adaptive calibration mechanism is graph-agnostic — it measures recall
//! vs ef_search on whatever graph structure is provided. The flat graph keeps
//! the PoC self-contained while still exhibiting the recall–ef trade-off that
//! calibration must navigate.

use rand::rngs::StdRng;
use rand::{Rng, SeedableRng};
use rayon::prelude::*;

/// Configuration for the proximity graph.
#[derive(Clone, Debug)]
pub struct GraphConfig {
    /// Local neighbors per node (exact k-NN within the dataset).
    pub m: usize,
    /// Random long-jump neighbors per node (navigable small world shortcuts).
    pub m_longjump: usize,
    /// Dimensionality of stored vectors.
    pub dims: usize,
}

impl Default for GraphConfig {
    fn default() -> Self {
        GraphConfig {
            m: 16,
            m_longjump: 4,
            dims: 64,
        }
    }
}

/// Flat navigable small-world graph.
pub struct FlatGraph {
    /// Row-major vector store: node `i` lives at `[i*dims .. (i+1)*dims]`.
    pub vectors: Vec<f32>,
    /// Adjacency lists: `neighbors[i]` holds all neighbor ids for node `i`.
    pub neighbors: Vec<Vec<u32>>,
    pub config: GraphConfig,
    pub n: usize,
}

/// Squared L2 distance between two equal-length slices.
#[inline]
pub fn l2_sq(a: &[f32], b: &[f32]) -> f32 {
    assert_eq!(a.len(), b.len(), "vector dimension mismatch");
    a.iter().zip(b.iter()).map(|(x, y)| (x - y) * (x - y)).sum()
}

impl FlatGraph {
    /// Build the graph from a flat row-major vector slice.
    ///
    /// Construction is brute-force O(N² · D) — correct for PoC sizes (≤ 10 K).
    pub fn build(vectors: Vec<f32>, config: GraphConfig) -> Self {
        assert!(
            config.dims > 0,
            "graph dimensions must be greater than zero"
        );
        let n = vectors.len() / config.dims;
        assert_eq!(vectors.len(), n * config.dims, "vector length mismatch");
        assert!(n > 0, "graph must contain at least one vector");

        let m_local = config.m.min(n.saturating_sub(1));
        let dims = config.dims;

        // Build exact local k-NN for each node in parallel.
        let neighbors: Vec<Vec<u32>> = (0..n)
            .into_par_iter()
            .map(|i| {
                let vi = &vectors[i * dims..(i + 1) * dims];
                // Compute distance to all other nodes.
                let mut dists: Vec<(u32, f32)> = (0..n)
                    .filter(|&j| j != i)
                    .map(|j| {
                        let vj = &vectors[j * dims..(j + 1) * dims];
                        (j as u32, l2_sq(vi, vj))
                    })
                    .collect();
                dists.sort_by(|a, b| a.1.total_cmp(&b.1));
                dists.truncate(m_local);
                dists.iter().map(|&(id, _)| id).collect()
            })
            .collect();

        // Add long-jump edges (random connections for small-world property).
        let mut neighbors = neighbors;
        if config.m_longjump > 0 {
            let mut rng = StdRng::seed_from_u64(0xFEED_DEAD);
            for (i, node_neighbors) in neighbors.iter_mut().enumerate() {
                let current_len = node_neighbors.len();
                let mut added = 0usize;
                let mut attempts = 0usize;
                while added < config.m_longjump && attempts < n * 2 {
                    let j = rng.gen_range(0..n);
                    attempts += 1;
                    if j == i {
                        continue;
                    }
                    let jid = j as u32;
                    if !node_neighbors[..current_len + added].contains(&jid) {
                        node_neighbors.push(jid);
                        added += 1;
                    }
                }
            }
        }

        FlatGraph {
            vectors,
            neighbors,
            config,
            n,
        }
    }

    /// Slice of the vector for node `id`.
    #[inline]
    pub fn vec(&self, id: usize) -> &[f32] {
        let d = self.config.dims;
        &self.vectors[id * d..(id + 1) * d]
    }
}
