//! Integration tests for ANN search across COW branches (dual-graph merge).
//!
//! ADR-200 follow-up to PR #617 — stacks on `feat/queryable-cow-branches`.
//!
//! Design: COW dual-graph ANN merge
//! 1. Query child's own HNSW (over-fetch k' = k × 4).
//! 2. Query parent's HNSW (lazily opened, cached; no rebuild per branch).
//! 3. Merge: tombstoned IDs excluded (via membership_filter), child overrides
//!    parent for same ID, re-rank by distance, return top-k.
//!
//! Approximation note: dual-graph merge is slightly approximate.  This file
//! measures and asserts real recall@10 vs the exact ground-truth scan.

use rvf_runtime::options::{DistanceMetric, QueryOptions, RvfOptions};
use rvf_runtime::RvfStore;
use tempfile::TempDir;

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

fn make_opts(dim: u16) -> RvfOptions {
    RvfOptions {
        dimension: dim,
        metric: DistanceMetric::L2,
        ..Default::default()
    }
}

/// Deterministic LCG vector generator (no external rand dependency).
fn lcg_vector(dim: usize, seed: u64) -> Vec<f32> {
    let mut v = Vec::with_capacity(dim);
    let mut x = seed;
    for _ in 0..dim {
        x = x
            .wrapping_mul(6364136223846793005)
            .wrapping_add(1442695040888963407);
        v.push(((x >> 33) as f32) / (u32::MAX as f32) - 0.5);
    }
    v
}

/// Squared L2 distance between two vectors.
fn l2_sq(a: &[f32], b: &[f32]) -> f32 {
    a.iter().zip(b.iter()).map(|(x, y)| (x - y) * (x - y)).sum()
}

/// Exact brute-force k-NN over a slice of (id, vector) pairs.
fn exact_knn(query: &[f32], corpus: &[(u64, Vec<f32>)], k: usize) -> Vec<u64> {
    let mut dists: Vec<(u64, f32)> = corpus
        .iter()
        .map(|(id, v)| (*id, l2_sq(query, v)))
        .collect();
    dists.sort_by(|a, b| a.1.total_cmp(&b.1).then_with(|| a.0.cmp(&b.0)));
    dists.iter().take(k).map(|(id, _)| *id).collect()
}

/// recall@k = |ANN ∩ exact| / k
fn recall_at_k(ann: &[u64], exact: &[u64]) -> f64 {
    let k = exact.len();
    if k == 0 {
        return 1.0;
    }
    let exact_set: std::collections::HashSet<u64> = exact.iter().copied().collect();
    let hits = ann.iter().filter(|id| exact_set.contains(id)).count();
    hits as f64 / k as f64
}

// ===========================================================================
// TEST 1: cow_ann_recall_vs_exact
// ===========================================================================

/// Main recall test.
///
/// Build a base with 1 200 L2 vectors (large enough for parent HNSW).
/// Branch it, then in the child:
/// - add 60 new vectors (IDs 5000..5059)
/// - override 20 parent vectors (IDs 0..19 with different values)
/// - tombstone 10 parent vectors (IDs 100..109 deleted from child view)
///
/// Run a COW ANN query on the child and compare to the exact ground truth.
/// Assert recall@10 >= 0.95, override correctness, and tombstone absence.
#[test]
fn cow_ann_recall_vs_exact() {
    let dir = TempDir::new().unwrap();
    let base_path = dir.path().join("base.rvf");
    let child_path = dir.path().join("child.rvf");
    const DIM: u16 = 32;
    const BASE_N: usize = 1_200;
    const K: usize = 10;

    // ── Build base store ─────────────────────────────────────────────────
    let mut base = RvfStore::create(&base_path, make_opts(DIM)).unwrap();
    let base_vecs: Vec<Vec<f32>> = (0..BASE_N)
        .map(|i| lcg_vector(DIM as usize, i as u64))
        .collect();
    let base_refs: Vec<&[f32]> = base_vecs.iter().map(|v| v.as_slice()).collect();
    let base_ids: Vec<u64> = (0..BASE_N as u64).collect();
    base.ingest_batch(&base_refs, &base_ids, None).unwrap();
    base.close().unwrap();

    // ── Branch ───────────────────────────────────────────────────────────
    let mut base = RvfStore::open(&base_path).unwrap();
    let mut child = base.branch(&child_path).unwrap();
    base.close().unwrap();

    // ── Child edits ───────────────────────────────────────────────────────

    // (a) Add 60 new vectors not in parent.
    const NEW_START: u64 = 5_000;
    const NEW_COUNT: usize = 60;
    let new_vecs: Vec<Vec<f32>> = (0..NEW_COUNT)
        .map(|i| lcg_vector(DIM as usize, 9_000 + i as u64))
        .collect();
    let new_refs: Vec<&[f32]> = new_vecs.iter().map(|v| v.as_slice()).collect();
    let new_ids: Vec<u64> = (NEW_START..NEW_START + NEW_COUNT as u64).collect();
    child.ingest_batch(&new_refs, &new_ids, None).unwrap();

    // (b) Override 20 parent vectors with different data (same IDs 0..19).
    const OVERRIDE_COUNT: usize = 20;
    let override_vecs: Vec<Vec<f32>> = (0..OVERRIDE_COUNT)
        .map(|i| lcg_vector(DIM as usize, 99_000 + i as u64))
        .collect();
    let override_refs: Vec<&[f32]> = override_vecs.iter().map(|v| v.as_slice()).collect();
    let override_ids: Vec<u64> = (0..OVERRIDE_COUNT as u64).collect();
    child
        .ingest_batch(&override_refs, &override_ids, None)
        .unwrap();

    // (c) Tombstone 10 parent vectors (IDs 100..109) from the child view.
    const TOMBSTONE_START: u64 = 100;
    const TOMBSTONE_COUNT: usize = 10;
    let tombstone_ids: Vec<u64> =
        (TOMBSTONE_START..TOMBSTONE_START + TOMBSTONE_COUNT as u64).collect();
    child.delete(&tombstone_ids).unwrap();

    // ── Build ground-truth corpus visible from child ──────────────────────
    // Ground truth = parent vectors (excluding overrides + tombstones)
    //              ∪ child override vectors
    //              ∪ child new vectors
    let mut ground_truth_corpus: Vec<(u64, Vec<f32>)> = Vec::new();

    // Parent vectors: visible unless overridden or tombstoned.
    let override_set: std::collections::HashSet<u64> = override_ids.iter().copied().collect();
    let tombstone_set: std::collections::HashSet<u64> = tombstone_ids.iter().copied().collect();
    for (i, v) in base_vecs.iter().enumerate() {
        let id = i as u64;
        if override_set.contains(&id) || tombstone_set.contains(&id) {
            continue;
        }
        ground_truth_corpus.push((id, v.clone()));
    }
    // Child overrides (use child's version).
    for (i, v) in override_vecs.iter().enumerate() {
        ground_truth_corpus.push((override_ids[i], v.clone()));
    }
    // Child new vectors.
    for (i, v) in new_vecs.iter().enumerate() {
        ground_truth_corpus.push((new_ids[i], v.clone()));
    }

    // ── Query: use a vector near an existing cluster ──────────────────────
    // Query near parent vector 500 (not overridden, not tombstoned).
    let query = lcg_vector(DIM as usize, 500);

    // Exact ground truth (brute force).
    let exact_top_k = exact_knn(&query, &ground_truth_corpus, K);
    assert_eq!(
        exact_top_k.len(),
        K,
        "ground truth must return K={} results",
        K
    );

    // ANN result via dual-graph merge (default QueryOptions → uses HNSW paths).
    let ann_opts = QueryOptions {
        ef_search: 300, // generous ef so recall is high
        ..Default::default()
    };
    let ann_results = child.query(&query, K, &ann_opts).unwrap();
    assert_eq!(
        ann_results.len(),
        K,
        "ANN query must return K={} results",
        K
    );
    let ann_ids: Vec<u64> = ann_results.iter().map(|r| r.id).collect();

    // Recall@K measurement.
    let recall = recall_at_k(&ann_ids, &exact_top_k);
    println!(
        "cow_ann_recall_vs_exact: recall@{K} = {:.4} (ANN top-{K}: {:?})",
        recall, ann_ids
    );
    assert!(
        recall >= 0.95,
        "recall@{K} {:.4} is below the 0.95 contract (ANN={:?}, exact={:?})",
        recall,
        ann_ids,
        exact_top_k
    );

    child.close().unwrap();

    println!("PASS: cow_ann_recall_vs_exact (recall@{K} = {recall:.4})");
}

// ===========================================================================
// TEST 5: cow_ann_recall_vs_exact_cosine
// ===========================================================================

/// Cosine-metric COW recall regression test.
///
/// This is the primary regression test for the native COW dual-graph cosine
/// bug (fixed in this PR): before the fix the parent store was re-opened via
/// `open_readonly()` which went through `boot()` without restoring the metric,
/// so the parent defaulted to L2.  The parent HNSW was built with L2 distance
/// and returned L2-ordered candidates that were then merged with the child's
/// cosine distances — completely breaking the ordering.
///
/// Before fix: cosine recall@10 ≈ 0.10 (bug reproducible here).
/// After fix : cosine recall@10 ≥ 0.95 (metric persisted in manifest).
///
/// Design mirrors `cow_ann_recall_vs_exact` (L2) with:
/// - `metric: DistanceMetric::Cosine`
/// - ground-truth computed via cosine distance (1 − cos_sim)
/// - same child-edit mix (60 new, 20 override, 10 tombstone)
fn make_cosine_opts(dim: u16) -> RvfOptions {
    RvfOptions {
        dimension: dim,
        metric: DistanceMetric::Cosine,
        ..Default::default()
    }
}

/// Cosine distance: 1 − dot(a,b)/(‖a‖·‖b‖).
fn cosine_dist(a: &[f32], b: &[f32]) -> f32 {
    let mut dot = 0.0f32;
    let mut na = 0.0f32;
    let mut nb = 0.0f32;
    for (x, y) in a.iter().zip(b.iter()) {
        dot += x * y;
        na += x * x;
        nb += y * y;
    }
    let denom = (na * nb).sqrt();
    if denom < f32::EPSILON {
        1.0
    } else {
        1.0 - dot / denom
    }
}

/// Exact brute-force k-NN over a slice of (id, vector) pairs using cosine
/// distance.
fn exact_knn_cosine(query: &[f32], corpus: &[(u64, Vec<f32>)], k: usize) -> Vec<u64> {
    let mut dists: Vec<(u64, f32)> = corpus
        .iter()
        .map(|(id, v)| (*id, cosine_dist(query, v)))
        .collect();
    dists.sort_by(|a, b| a.1.total_cmp(&b.1).then_with(|| a.0.cmp(&b.0)));
    dists.iter().take(k).map(|(id, _)| *id).collect()
}

#[test]
fn cow_ann_recall_vs_exact_cosine() {
    let dir = TempDir::new().unwrap();
    let base_path = dir.path().join("base_cos.rvf");
    let child_path = dir.path().join("child_cos.rvf");
    // Use the same dim/count as the L2 test so the parent slab is large enough
    // for HNSW to kick in on both arms.
    const DIM: u16 = 32;
    const BASE_N: usize = 1_200;
    const K: usize = 10;

    // ── Build base store (cosine metric) ─────────────────────────────────
    let mut base = RvfStore::create(&base_path, make_cosine_opts(DIM)).unwrap();
    let base_vecs: Vec<Vec<f32>> = (0..BASE_N)
        .map(|i| lcg_vector(DIM as usize, i as u64 + 20_000))
        .collect();
    let base_refs: Vec<&[f32]> = base_vecs.iter().map(|v| v.as_slice()).collect();
    let base_ids: Vec<u64> = (0..BASE_N as u64).collect();
    base.ingest_batch(&base_refs, &base_ids, None).unwrap();
    base.close().unwrap();

    // ── Branch ───────────────────────────────────────────────────────────
    let mut base = RvfStore::open(&base_path).unwrap();
    // Verify the metric was persisted correctly (sanity check).
    assert_eq!(
        base.metric(),
        DistanceMetric::Cosine,
        "base store metric must survive close()+open() round-trip"
    );
    let mut child = base.branch(&child_path).unwrap();
    base.close().unwrap();

    // ── Child edits ───────────────────────────────────────────────────────
    // (a) 60 new vectors (IDs 5000..5059)
    const NEW_START: u64 = 5_000;
    const NEW_COUNT: usize = 60;
    let new_vecs: Vec<Vec<f32>> = (0..NEW_COUNT)
        .map(|i| lcg_vector(DIM as usize, 29_000 + i as u64))
        .collect();
    let new_refs: Vec<&[f32]> = new_vecs.iter().map(|v| v.as_slice()).collect();
    let new_ids: Vec<u64> = (NEW_START..NEW_START + NEW_COUNT as u64).collect();
    child.ingest_batch(&new_refs, &new_ids, None).unwrap();

    // (b) Override 20 parent vectors (IDs 0..19).
    const OVERRIDE_COUNT: usize = 20;
    let override_vecs: Vec<Vec<f32>> = (0..OVERRIDE_COUNT)
        .map(|i| lcg_vector(DIM as usize, 99_000 + i as u64))
        .collect();
    let override_refs: Vec<&[f32]> = override_vecs.iter().map(|v| v.as_slice()).collect();
    let override_ids: Vec<u64> = (0..OVERRIDE_COUNT as u64).collect();
    child
        .ingest_batch(&override_refs, &override_ids, None)
        .unwrap();

    // (c) Tombstone 10 parent vectors (IDs 100..109).
    const TOMBSTONE_START: u64 = 100;
    const TOMBSTONE_COUNT: usize = 10;
    let tombstone_ids: Vec<u64> =
        (TOMBSTONE_START..TOMBSTONE_START + TOMBSTONE_COUNT as u64).collect();
    child.delete(&tombstone_ids).unwrap();

    // ── Build cosine ground-truth corpus visible from child ───────────────
    let mut ground_truth_corpus: Vec<(u64, Vec<f32>)> = Vec::new();

    let override_set: std::collections::HashSet<u64> = override_ids.iter().copied().collect();
    let tombstone_set: std::collections::HashSet<u64> = tombstone_ids.iter().copied().collect();
    for (i, v) in base_vecs.iter().enumerate() {
        let id = i as u64;
        if override_set.contains(&id) || tombstone_set.contains(&id) {
            continue;
        }
        ground_truth_corpus.push((id, v.clone()));
    }
    for (i, v) in override_vecs.iter().enumerate() {
        ground_truth_corpus.push((override_ids[i], v.clone()));
    }
    for (i, v) in new_vecs.iter().enumerate() {
        ground_truth_corpus.push((new_ids[i], v.clone()));
    }

    // ── Query ─────────────────────────────────────────────────────────────
    // Use a query vector near parent vector 500 (not overridden, not tombstoned).
    let query = lcg_vector(DIM as usize, 500 + 20_000);

    // Exact cosine ground truth.
    let exact_top_k = exact_knn_cosine(&query, &ground_truth_corpus, K);
    assert_eq!(
        exact_top_k.len(),
        K,
        "ground truth must return K={K} results"
    );

    // COW ANN via dual-graph merge (the path that was broken before the fix).
    let ann_opts = QueryOptions {
        ef_search: 300,
        ..Default::default()
    };
    let ann_results = child.query(&query, K, &ann_opts).unwrap();
    assert_eq!(ann_results.len(), K, "ANN query must return K={K} results");
    let ann_ids: Vec<u64> = ann_results.iter().map(|r| r.id).collect();

    let recall = recall_at_k(&ann_ids, &exact_top_k);
    println!(
        "cow_ann_recall_vs_exact_cosine: recall@{K} = {:.4} (ANN top-{K}: {:?})",
        recall, ann_ids
    );

    // Before the fix this assertion fired with recall ≈ 0.10.
    // After the fix (metric persisted in manifest → parent re-opened with
    // the correct Cosine metric) recall@10 must be ≥ 0.95.
    assert!(
        recall >= 0.95,
        "recall@{K} {:.4} is below the 0.95 contract — \
         possible metric-persistence regression (ANN={:?}, exact={:?})",
        recall,
        ann_ids,
        exact_top_k
    );

    child.close().unwrap();

    println!("PASS: cow_ann_recall_vs_exact_cosine (recall@{K} = {recall:.4})");
}

// ===========================================================================
// TEST 2: cow_ann_override_correctness
// ===========================================================================

/// Verify that for an overridden parent vector, the ANN path returns the
/// child's version (with the child's distance), not the parent's stale entry.
#[test]
fn cow_ann_override_correctness() {
    let dir = TempDir::new().unwrap();
    let base_path = dir.path().join("base_ov.rvf");
    let child_path = dir.path().join("child_ov.rvf");
    const DIM: u16 = 16;
    const BASE_N: usize = 1_200;

    // Base: fill with zero vectors so the only interesting vector is id=0.
    let mut base = RvfStore::create(&base_path, make_opts(DIM)).unwrap();
    let base_vecs: Vec<Vec<f32>> = (0..BASE_N).map(|_| vec![0.0f32; DIM as usize]).collect();
    let base_refs: Vec<&[f32]> = base_vecs.iter().map(|v| v.as_slice()).collect();
    let base_ids: Vec<u64> = (0..BASE_N as u64).collect();
    base.ingest_batch(&base_refs, &base_ids, None).unwrap();
    base.close().unwrap();

    let mut base = RvfStore::open(&base_path).unwrap();
    let mut child = base.branch(&child_path).unwrap();
    base.close().unwrap();

    // Override id=0 with a vector far from zero.
    let override_vec: Vec<f32> = vec![100.0f32; DIM as usize];
    child
        .ingest_batch(&[override_vec.as_slice()], &[0u64], None)
        .unwrap();

    // Query very near the zero vector → parent's id=0 would be closest,
    // but child has replaced it with a far-away vector.
    let query = vec![0.01f32; DIM as usize];
    let opts = QueryOptions {
        ef_search: 300,
        ..Default::default()
    };
    let results = child.query(&query, 5, &opts).unwrap();
    let ids: Vec<u64> = results.iter().map(|r| r.id).collect();

    // id=0 may or may not appear, but if it does, its distance must reflect
    // the child's override (very large), not the parent's near-zero distance.
    for r in &results {
        if r.id == 0 {
            let child_dist = l2_sq(&query, &[100.0f32; DIM as usize]);
            assert!(
                (r.distance - child_dist).abs() < 1e-3,
                "id=0 must use child override distance {}, got {}",
                child_dist,
                r.distance
            );
        }
    }

    // The nearest results should NOT be id=0 since it's now far away.
    // Other zero-filled vectors (1..BASE_N) should dominate.
    let nearest = results[0].id;
    assert_ne!(
        nearest, 0,
        "id=0 (overridden to [100;16]) should not be nearest to [0.01;16]: results={:?}",
        ids
    );

    child.close().unwrap();

    println!("PASS: cow_ann_override_correctness");
}

// ===========================================================================
// TEST 3: cow_ann_tombstone_absent
// ===========================================================================

/// Verify that a tombstoned parent vector never appears in ANN or exact results.
#[test]
fn cow_ann_tombstone_absent() {
    let dir = TempDir::new().unwrap();
    let base_path = dir.path().join("base_ts.rvf");
    let child_path = dir.path().join("child_ts.rvf");
    const DIM: u16 = 16;
    const BASE_N: usize = 1_200;

    // Base: id=500 will be very close to the query.
    let mut base = RvfStore::create(&base_path, make_opts(DIM)).unwrap();
    let mut base_vecs: Vec<Vec<f32>> = (0..BASE_N)
        .map(|i| lcg_vector(DIM as usize, i as u64 + 1000))
        .collect();
    // Place id=500 at the query location so it would always be top-1.
    base_vecs[500] = vec![1.0f32; DIM as usize];
    let base_refs: Vec<&[f32]> = base_vecs.iter().map(|v| v.as_slice()).collect();
    let base_ids: Vec<u64> = (0..BASE_N as u64).collect();
    base.ingest_batch(&base_refs, &base_ids, None).unwrap();
    base.close().unwrap();

    let mut base = RvfStore::open(&base_path).unwrap();
    let mut child = base.branch(&child_path).unwrap();
    base.close().unwrap();

    // Tombstone id=500 in the child.
    child.delete(&[500u64]).unwrap();

    // Query near [1.0; DIM] — id=500 would be nearest but is tombstoned.
    let query = vec![1.0f32; DIM as usize];
    let opts = QueryOptions {
        ef_search: 300,
        ..Default::default()
    };
    let ann_results = child.query(&query, 10, &opts).unwrap();
    let ann_ids: Vec<u64> = ann_results.iter().map(|r| r.id).collect();
    assert!(
        !ann_ids.contains(&500),
        "tombstoned id=500 must not appear in ANN results: {:?}",
        ann_ids
    );

    // Also confirm exact scan respects tombstone.
    let exact_opts = QueryOptions {
        force_exact: true,
        ..Default::default()
    };
    let exact_results = child.query(&query, 10, &exact_opts).unwrap();
    let exact_ids: Vec<u64> = exact_results.iter().map(|r| r.id).collect();
    assert!(
        !exact_ids.contains(&500),
        "tombstoned id=500 must not appear in exact results: {:?}",
        exact_ids
    );

    child.close().unwrap();

    println!("PASS: cow_ann_tombstone_absent");
}

// ===========================================================================
// TEST 4: cow_branch_size_independence
// ===========================================================================

/// Verify that the child store file stays small (does not contain a copy of
/// the parent's HNSW or vector slab) after queries populate parent_store.
///
/// This confirms the "no rebuild" contract: the parent HNSW is accessed from
/// the parent file, not copied into the child.
#[test]
fn cow_branch_size_independence() {
    let dir = TempDir::new().unwrap();
    let base_path = dir.path().join("base_sz.rvf");
    let child_path = dir.path().join("child_sz.rvf");
    const DIM: u16 = 32;
    const BASE_N: usize = 1_200;

    let mut base = RvfStore::create(&base_path, make_opts(DIM)).unwrap();
    let base_vecs: Vec<Vec<f32>> = (0..BASE_N)
        .map(|i| lcg_vector(DIM as usize, i as u64 + 7000))
        .collect();
    let base_refs: Vec<&[f32]> = base_vecs.iter().map(|v| v.as_slice()).collect();
    let base_ids: Vec<u64> = (0..BASE_N as u64).collect();
    base.ingest_batch(&base_refs, &base_ids, None).unwrap();
    base.close().unwrap();

    let mut base = RvfStore::open(&base_path).unwrap();
    let child_before_query = base.branch(&child_path).unwrap();
    let child_size_before = std::fs::metadata(&child_path).unwrap().len();
    base.close().unwrap();

    // Issue a COW ANN query (triggers lazy parent open + parent HNSW build).
    let mut child = child_before_query;
    let query = lcg_vector(DIM as usize, 42);
    let opts = QueryOptions {
        ef_search: 200,
        ..Default::default()
    };
    let results = child.query(&query, 10, &opts).unwrap();
    assert_eq!(results.len(), 10, "should return 10 results from parent");

    let child_size_after = std::fs::metadata(&child_path).unwrap().len();
    let base_size = std::fs::metadata(&base_path).unwrap().len();

    // Child must be much smaller than parent (no vector data rebuild).
    assert!(
        child_size_after < base_size / 2,
        "child ({child_size_after} bytes) should be < half of parent ({base_size} bytes) — \
         parent HNSW must not be copied into child file"
    );

    println!(
        "PASS: cow_branch_size_independence — \
         parent={base_size} bytes, child_before={child_size_before}, \
         child_after_query={child_size_after}"
    );

    child.close().unwrap();
}
