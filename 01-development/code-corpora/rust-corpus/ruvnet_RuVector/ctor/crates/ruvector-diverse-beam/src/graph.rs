use crate::{l2_sq, VecId};

/// Flat kNN graph: each node connects to `k_nn` nearest neighbours.
/// Serves as the navigable graph for all three beam-search variants.
///
/// Build is O(n² · dim) — exact kNN, suitable for n ≤ 4 000.
pub struct FlatGraph {
    pub vectors: Vec<Vec<f32>>,
    pub neighbors: Vec<Vec<VecId>>,
    pub dim: usize,
    pub k_nn: usize,
}

impl FlatGraph {
    pub fn build(vectors: Vec<Vec<f32>>, k_nn: usize) -> Self {
        let n = vectors.len();
        assert!(n > k_nn, "n={n} must be > k_nn={k_nn}");
        let dim = vectors[0].len();
        assert!(dim > 0, "vectors must have at least one dimension");
        assert!(
            vectors.iter().all(|vector| vector.len() == dim),
            "all vectors must have the same dimension"
        );
        let mut neighbors = vec![Vec::with_capacity(k_nn); n];

        for i in 0..n {
            let mut dists: Vec<(VecId, f32)> = (0..n)
                .filter(|&j| j != i)
                .map(|j| (j, l2_sq(&vectors[i], &vectors[j])))
                .collect();
            dists.sort_by(|a, b| a.1.total_cmp(&b.1));
            neighbors[i] = dists[..k_nn].iter().map(|&(j, _)| j).collect();
        }

        FlatGraph {
            vectors,
            neighbors,
            dim,
            k_nn,
        }
    }

    #[inline]
    pub fn vec(&self, id: VecId) -> &[f32] {
        &self.vectors[id]
    }

    #[inline]
    pub fn len(&self) -> usize {
        self.vectors.len()
    }

    #[inline]
    pub fn is_empty(&self) -> bool {
        self.vectors.is_empty()
    }

    /// Brute-force exact top-k for ground-truth computation.
    pub fn brute_force(&self, query: &[f32], k: usize) -> Vec<(VecId, f32)> {
        assert_eq!(query.len(), self.dim, "query dimension mismatch");
        let mut dists: Vec<(VecId, f32)> = (0..self.len())
            .map(|i| (i, l2_sq(query, self.vec(i))))
            .collect();
        dists.sort_by(|a, b| a.1.total_cmp(&b.1));
        dists.truncate(k);
        dists
    }

    /// Estimated heap memory: vectors + neighbour lists.
    pub fn memory_bytes(&self) -> usize {
        let n = self.len();
        let vec_bytes = n * self.dim * 4;
        let nb_bytes = n * self.k_nn * 8;
        vec_bytes + nb_bytes
    }
}
