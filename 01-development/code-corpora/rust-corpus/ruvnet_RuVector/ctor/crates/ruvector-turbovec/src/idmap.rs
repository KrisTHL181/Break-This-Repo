//! `IdMapIndex` — external `u64` ids, `O(1)` deletion, allowlist-filtered
//! search (ADR-254 §T6).
//!
//! Wraps a [`TurboVecIndex`] whose internal ids are dense insertion positions.
//! A tombstone bitmap + `id → pos` map give `O(1)` `remove`; deleted slots are
//! skipped at query time. Filtering is an allowlist of external ids.
//!
//! For this M1 reference, filtered/deleted search does a full candidate scan and
//! filters in the heap merge. The block-granularity short-circuit (skip whole
//! 32-vector blocks with no allowed slots) is the FastScan-kernel milestone
//! (§T5); the *results* here are the oracle that milestone must match.

use crate::error::{Result, TurboVecError};
use crate::index::TurboVecIndex;
use crate::quantize::BitWidth;
use ruvector_rabitq::AnnIndex;
use std::collections::{HashMap, HashSet};

/// External-id index with deletion and filtered search.
pub struct IdMapIndex {
    inner: TurboVecIndex,
    /// internal pos → external id
    ext_ids: Vec<u64>,
    /// internal pos → alive?
    alive: Vec<bool>,
    /// external id → internal pos
    id_to_pos: HashMap<u64, usize>,
    live_count: usize,
}

impl IdMapIndex {
    /// Empty index over `dim`-d vectors at `bw` bits/coord.
    pub fn new(dim: usize, bw: BitWidth) -> Result<Self> {
        Ok(Self {
            inner: TurboVecIndex::new(dim, bw)?,
            ext_ids: Vec::new(),
            alive: Vec::new(),
            id_to_pos: HashMap::new(),
            live_count: 0,
        })
    }

    /// Reproducible variant.
    pub fn with_seed(dim: usize, bw: BitWidth, seed: u64) -> Result<Self> {
        Ok(Self {
            inner: TurboVecIndex::with_seed(dim, bw, seed)?,
            ext_ids: Vec::new(),
            alive: Vec::new(),
            id_to_pos: HashMap::new(),
            live_count: 0,
        })
    }

    /// Freeze calibration & encode (delegates to the inner index).
    pub fn finalize(&mut self) {
        self.inner.finalize();
    }

    /// Number of live (non-deleted) vectors.
    #[inline]
    pub fn len(&self) -> usize {
        self.live_count
    }

    /// Whether the index holds no live vectors.
    #[inline]
    pub fn is_empty(&self) -> bool {
        self.live_count == 0
    }

    /// Honest memory accounting incl. the id bookkeeping.
    pub fn memory_bytes(&self) -> usize {
        self.inner.memory_bytes()
            + self.ext_ids.len() * 8
            + self.alive.len()
            + self.id_to_pos.len() * (8 + std::mem::size_of::<usize>())
    }

    /// Add one vector under external id `id`. Errors on dim/duplicate id.
    pub fn add_with_id(&mut self, id: u64, vector: Vec<f32>) -> Result<()> {
        if vector.len() != self.inner.dim() {
            return Err(TurboVecError::DimMismatch {
                expected: self.inner.dim(),
                got: vector.len(),
            });
        }
        if !vector.iter().all(|value| value.is_finite()) {
            return Err(TurboVecError::NonFiniteVector);
        }
        if self.id_to_pos.contains_key(&id) {
            return Err(TurboVecError::DuplicateId(id));
        }
        let pos = self.ext_ids.len();
        let got = vector.len();
        // inner uses internal id == pos; dim already validated above so this
        // map_err is effectively unreachable, but stays correct if it isn't.
        self.inner
            .add(pos, vector)
            .map_err(|_| TurboVecError::DimMismatch {
                expected: self.inner.dim(),
                got,
            })?;
        self.ext_ids.push(id);
        self.alive.push(true);
        self.id_to_pos.insert(id, pos);
        self.live_count += 1;
        Ok(())
    }

    /// Bulk add. `ids.len()` must equal `vectors.len()`.
    pub fn add_with_ids(&mut self, vectors: &[Vec<f32>], ids: &[u64]) -> Result<()> {
        if vectors.len() != ids.len() {
            return Err(TurboVecError::BatchLenMismatch {
                vectors: vectors.len(),
                ids: ids.len(),
            });
        }
        let mut batch_ids = HashSet::with_capacity(ids.len());
        for (vector, &id) in vectors.iter().zip(ids) {
            if vector.len() != self.inner.dim() {
                return Err(TurboVecError::DimMismatch {
                    expected: self.inner.dim(),
                    got: vector.len(),
                });
            }
            if !vector.iter().all(|value| value.is_finite()) {
                return Err(TurboVecError::NonFiniteVector);
            }
            if self.id_to_pos.contains_key(&id) || !batch_ids.insert(id) {
                return Err(TurboVecError::DuplicateId(id));
            }
        }
        for (v, &id) in vectors.iter().zip(ids.iter()) {
            self.add_with_id(id, v.clone())?;
        }
        Ok(())
    }

    /// Delete by external id in `O(1)` (tombstone). Returns `true` if removed.
    pub fn remove(&mut self, id: u64) -> bool {
        if let Some(pos) = self.id_to_pos.remove(&id) {
            if self.alive[pos] {
                self.alive[pos] = false;
                self.live_count -= 1;
                return true;
            }
        }
        false
    }

    /// Top-`k` live nearest, returned as `(external_id, score)` ascending.
    pub fn search(&self, query: &[f32], k: usize) -> Result<Vec<(u64, f32)>> {
        self.filtered(query, k, None)
    }

    /// Top-`k` nearest among `allowlist` (external ids) that are still live.
    pub fn search_filtered(
        &self,
        query: &[f32],
        k: usize,
        allowlist: &[u64],
    ) -> Result<Vec<(u64, f32)>> {
        let set: HashSet<u64> = allowlist.iter().copied().collect();
        self.filtered(query, k, Some(&set))
    }

    fn filtered(
        &self,
        query: &[f32],
        k: usize,
        allow: Option<&HashSet<u64>>,
    ) -> Result<Vec<(u64, f32)>> {
        if query.len() != self.inner.dim() {
            return Err(TurboVecError::DimMismatch {
                expected: self.inner.dim(),
                got: query.len(),
            });
        }
        // Full candidate scan (reference path), then filter alive ∧ allowed.
        let n = self.inner.len();
        let all = self
            .inner
            .search(query, n)
            .map_err(|_| TurboVecError::DimMismatch {
                expected: self.inner.dim(),
                got: query.len(),
            })?;
        let mut out = Vec::with_capacity(k);
        for r in all {
            let pos = r.id;
            if pos >= self.alive.len() || !self.alive[pos] {
                continue;
            }
            let ext = self.ext_ids[pos];
            if let Some(a) = allow {
                if !a.contains(&ext) {
                    continue;
                }
            }
            out.push((ext, r.score));
            if out.len() == k {
                break;
            }
        }
        Ok(out)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use rand::{Rng, SeedableRng};

    fn rnd(n: usize, dim: usize, seed: u64) -> Vec<Vec<f32>> {
        let mut rng = rand::rngs::StdRng::seed_from_u64(seed);
        (0..n)
            .map(|_| (0..dim).map(|_| rng.gen::<f32>()).collect())
            .collect()
    }

    #[test]
    fn remove_is_o1_and_hidden_from_search() {
        let dim = 48;
        let data = rnd(500, dim, 4);
        let ids: Vec<u64> = (0..data.len() as u64).map(|i| i * 10 + 1).collect();
        let mut ix = IdMapIndex::with_seed(dim, BitWidth::Four, 1).unwrap();
        ix.add_with_ids(&data, &ids).unwrap();
        ix.finalize();

        let before = ix.search(&data[0], 5).unwrap();
        let victim = before[0].0;
        assert!(ix.remove(victim));
        assert!(!ix.remove(victim)); // second remove is a no-op
        assert_eq!(ix.len(), data.len() - 1);

        let after = ix.search(&data[0], 5).unwrap();
        assert!(
            after.iter().all(|(id, _)| *id != victim),
            "deleted id must not appear"
        );
    }

    #[test]
    fn duplicate_id_rejected() {
        let mut ix = IdMapIndex::new(8, BitWidth::Two).unwrap();
        ix.add_with_id(7, vec![1.0; 8]).unwrap();
        assert!(matches!(
            ix.add_with_id(7, vec![2.0; 8]),
            Err(TurboVecError::DuplicateId(7))
        ));
    }

    #[test]
    fn batch_len_mismatch_rejected() {
        let mut ix = IdMapIndex::new(8, BitWidth::Two).unwrap();
        let vectors = vec![vec![1.0; 8], vec![2.0; 8]];
        let ids = vec![1u64]; // too few
        assert!(matches!(
            ix.add_with_ids(&vectors, &ids),
            Err(TurboVecError::BatchLenMismatch { vectors: 2, ids: 1 })
        ));
    }

    #[test]
    fn batch_validation_is_atomic() {
        let mut ix = IdMapIndex::new(8, BitWidth::Two).unwrap();
        let vectors = vec![vec![1.0; 8], vec![2.0; 8]];
        assert!(matches!(
            ix.add_with_ids(&vectors, &[7, 7]),
            Err(TurboVecError::DuplicateId(7))
        ));
        assert!(ix.is_empty());

        let mut invalid = vec![2.0; 8];
        invalid[0] = f32::NAN;
        assert!(matches!(
            ix.add_with_ids(&[vec![1.0; 8], invalid], &[1, 2]),
            Err(TurboVecError::NonFiniteVector)
        ));
        assert!(ix.is_empty());
    }

    #[test]
    fn wrong_dim_rejected() {
        let mut ix = IdMapIndex::new(8, BitWidth::Two).unwrap();
        assert!(matches!(
            ix.add_with_id(1, vec![0.0; 4]),
            Err(TurboVecError::DimMismatch {
                expected: 8,
                got: 4
            })
        ));
    }

    #[test]
    fn filtered_search_only_returns_allowed() {
        let dim = 32;
        let data = rnd(400, dim, 6);
        let ids: Vec<u64> = (0..data.len() as u64).collect();
        let mut ix = IdMapIndex::with_seed(dim, BitWidth::Four, 9).unwrap();
        ix.add_with_ids(&data, &ids).unwrap();
        ix.finalize();

        let allow: Vec<u64> = (0..400).step_by(5).collect(); // 80 allowed ids
        let allow_set: HashSet<u64> = allow.iter().copied().collect();
        let res = ix.search_filtered(&data[3], 10, &allow).unwrap();
        assert!(!res.is_empty());
        assert!(
            res.iter().all(|(id, _)| allow_set.contains(id)),
            "every hit must be in the allowlist"
        );
    }
}
