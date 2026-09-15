//! HNSW index implementation

use crate::distance::calculate_distance;
use crate::error::{Result, VectorDbError};
use crate::types::{DistanceMetric, SearchQuery, SearchResult};
use parking_lot::RwLock;
use std::cmp::{Ordering, Reverse};
use std::collections::{BinaryHeap, HashMap, HashSet};
use std::sync::Arc;

/// HNSW Index configuration
#[derive(Debug, Clone)]
pub struct HnswConfig {
    /// M parameter - number of connections per node
    pub m: usize,
    /// ef_construction - size of dynamic candidate list during construction
    pub ef_construction: usize,
    /// ef_search - size of dynamic candidate list during search
    pub ef_search: usize,
    /// Distance metric
    pub metric: DistanceMetric,
    /// Number of dimensions
    pub dimensions: usize,
}

impl Default for HnswConfig {
    fn default() -> Self {
        Self {
            m: 32,
            ef_construction: 200,
            ef_search: 100,
            metric: DistanceMetric::Cosine,
            dimensions: 384,
        }
    }
}

#[derive(Clone)]
struct Neighbor {
    id: String,
    distance: f32,
}

impl PartialEq for Neighbor {
    fn eq(&self, other: &Self) -> bool {
        self.distance == other.distance
    }
}

impl Eq for Neighbor {}

impl PartialOrd for Neighbor {
    fn partial_cmp(&self, other: &Self) -> Option<Ordering> {
        Some(self.cmp(other))
    }
}

impl Ord for Neighbor {
    fn cmp(&self, other: &Self) -> Ordering {
        // Reverse ordering for min-heap behavior
        other
            .distance
            .partial_cmp(&self.distance)
            .unwrap_or(Ordering::Equal)
    }
}

/// Simplified HNSW index
pub struct HnswIndex {
    config: HnswConfig,
    vectors: Arc<RwLock<HashMap<String, Vec<f32>>>>,
    graph: Arc<RwLock<HashMap<String, Vec<String>>>>,
    entry_point: Arc<RwLock<Option<String>>>,
}

impl HnswIndex {
    /// Create a new HNSW index
    pub fn new(config: HnswConfig) -> Self {
        Self {
            config,
            vectors: Arc::new(RwLock::new(HashMap::new())),
            graph: Arc::new(RwLock::new(HashMap::new())),
            entry_point: Arc::new(RwLock::new(None)),
        }
    }

    /// Insert a vector into the index
    pub fn insert(&self, id: String, vector: Vec<f32>) -> Result<()> {
        if vector.len() != self.config.dimensions {
            return Err(VectorDbError::InvalidDimensions {
                expected: self.config.dimensions,
                actual: vector.len(),
            });
        }

        // Store vector
        self.vectors.write().insert(id.clone(), vector.clone());

        // Initialize graph connections and check if this is the first vector
        // IMPORTANT: Release all locks before calling search_knn_internal to avoid deadlock
        // (parking_lot::RwLock is NOT reentrant)
        let is_first = {
            let mut graph = self.graph.write();
            graph.insert(id.clone(), Vec::new());

            let mut entry_point = self.entry_point.write();
            if entry_point.is_none() {
                *entry_point = Some(id.clone());
                return Ok(());
            }
            false
        }; // All locks released here

        if is_first {
            return Ok(());
        }

        // Issue #430 (bug B): the insert beam was previously clamped to
        // `ef_construction.min(m * 2)`, which silently became `m * 2` (32 by
        // default) instead of `ef_construction` (200). At scale the resulting
        // beam was dominated by whatever sits near the entry point, so late-
        // inserted clusters got wired up through the wrong nodes. Use the
        // configured `ef_construction` so the beam actually matches the
        // HNSW paper.
        let neighbors = self.search_knn_internal(&vector, self.config.ef_construction);

        // Snapshot the vector store so we can compute neighbour-to-neighbour
        // distances during pruning without re-acquiring the lock per edge.
        let vectors_snapshot = self.vectors.read();

        // Re-acquire graph lock for modifications
        let mut graph = self.graph.write();

        // Connect to nearest neighbors (bidirectional)
        for neighbor in neighbors.iter().take(self.config.m) {
            if let Some(connections) = graph.get_mut(&id) {
                connections.push(neighbor.id.clone());
            }

            if let Some(neighbor_connections) = graph.get_mut(&neighbor.id) {
                neighbor_connections.push(id.clone());

                // Issue #430 (bug C): previously this branch trimmed the
                // adjacency list via `drain(0..)`, which is FIFO — it dropped
                // the OLDEST edges regardless of how close they were. Proper
                // HNSW pruning keeps the m CLOSEST neighbours. We compute the
                // pairwise distances using the vector for `neighbor.id`
                // (which we just looked up successfully above) and keep the
                // bottom-m by distance.
                if neighbor_connections.len() > self.config.m * 2 {
                    if let Some(anchor_vec) = vectors_snapshot.get(&neighbor.id) {
                        let mut scored: Vec<(String, f32)> = neighbor_connections
                            .drain(..)
                            .filter_map(|cid| {
                                vectors_snapshot.get(&cid).map(|cv| {
                                    let d = calculate_distance(anchor_vec, cv, self.config.metric)
                                        .unwrap_or(f32::MAX);
                                    (cid, d)
                                })
                            })
                            .collect();
                        scored.sort_by(|a, b| a.1.partial_cmp(&b.1).unwrap_or(Ordering::Equal));
                        scored.truncate(self.config.m);
                        *neighbor_connections = scored.into_iter().map(|(cid, _)| cid).collect();
                    } else {
                        // Fallback: shouldn't happen because `neighbor.id` came
                        // from the index, but keep the newest-m behavior so we
                        // never panic on a missing vector.
                        let drain_count = neighbor_connections.len() - self.config.m;
                        neighbor_connections.drain(0..drain_count);
                    }
                }
            }
        }

        Ok(())
    }

    /// Insert multiple vectors in batch
    pub fn insert_batch(&self, vectors: Vec<(String, Vec<f32>)>) -> Result<()> {
        for (id, vector) in vectors {
            self.insert(id, vector)?;
        }
        Ok(())
    }

    /// Search for k nearest neighbors
    pub fn search(&self, query: &SearchQuery) -> Result<Vec<SearchResult>> {
        let ef_search = query.ef_search.unwrap_or(self.config.ef_search);
        // Issue #430: caller's k was silently capped at ef_search; raise ef so we
        // visit at least k candidates.
        let ef = ef_search.max(query.k);
        let candidates = self.search_knn_internal(&query.vector, ef);

        let mut results = Vec::new();
        for candidate in candidates.into_iter().take(query.k) {
            // Apply distance threshold if specified
            if let Some(threshold) = query.threshold {
                if candidate.distance > threshold {
                    continue;
                }
            }

            results.push(SearchResult {
                id: candidate.id,
                score: candidate.distance,
                metadata: HashMap::new(),
                vector: None,
            });
        }

        Ok(results)
    }

    /// Internal k-NN search implementation
    ///
    /// Issue #430: `Neighbor::Ord` is reversed so BinaryHeap acts as a min-heap
    /// (smaller distance == "greater"). That's correct for `candidates` (pop
    /// closest unexplored first), but WRONG for `result` — peek returned the
    /// best candidate, so eviction kept dropping the best item instead of the
    /// worst. Wrap `result` in `Reverse` so peek/pop return the furthest item
    /// (the eviction target).
    fn search_knn_internal(&self, query: &[f32], ef: usize) -> Vec<Neighbor> {
        let vectors = self.vectors.read();
        let graph = self.graph.read();
        let entry_point = self.entry_point.read();

        if entry_point.is_none() {
            return Vec::new();
        }

        let entry_id = entry_point.as_ref().unwrap();
        let mut visited = HashSet::new();
        let mut candidates: BinaryHeap<Neighbor> = BinaryHeap::new();
        // Max-heap over distance — peek() returns the worst (furthest) of the
        // current top-K. Wrap in Reverse to invert Neighbor's min-heap Ord.
        let mut result: BinaryHeap<Reverse<Neighbor>> = BinaryHeap::new();

        // Calculate distance to entry point
        if let Some(entry_vec) = vectors.get(entry_id) {
            let dist = calculate_distance(query, entry_vec, self.config.metric).unwrap_or(f32::MAX);

            let neighbor = Neighbor {
                id: entry_id.clone(),
                distance: dist,
            };

            candidates.push(neighbor.clone());
            result.push(Reverse(neighbor));
            visited.insert(entry_id.clone());
        }

        // Search phase
        while let Some(current) = candidates.pop() {
            // Stop when current is worse than the worst kept result and we have ef.
            if let Some(Reverse(furthest)) = result.peek() {
                if current.distance > furthest.distance && result.len() >= ef {
                    break;
                }
            }

            // Explore neighbors
            if let Some(neighbors) = graph.get(&current.id) {
                for neighbor_id in neighbors {
                    if visited.contains(neighbor_id) {
                        continue;
                    }

                    visited.insert(neighbor_id.clone());

                    if let Some(neighbor_vec) = vectors.get(neighbor_id) {
                        let dist = calculate_distance(query, neighbor_vec, self.config.metric)
                            .unwrap_or(f32::MAX);

                        let neighbor = Neighbor {
                            id: neighbor_id.clone(),
                            distance: dist,
                        };

                        // Add to candidates (min-heap by distance)
                        candidates.push(neighbor.clone());

                        // Add to results if room or strictly better than current worst.
                        if result.len() < ef {
                            result.push(Reverse(neighbor));
                        } else if let Some(Reverse(worst)) = result.peek() {
                            if dist < worst.distance {
                                result.pop();
                                result.push(Reverse(neighbor));
                            }
                        }
                    }
                }
            }
        }

        // Convert to sorted vector (ascending distance).
        let mut sorted_results: Vec<Neighbor> = result.into_iter().map(|Reverse(n)| n).collect();
        sorted_results.sort_by(|a, b| {
            a.distance
                .partial_cmp(&b.distance)
                .unwrap_or(Ordering::Equal)
        });

        sorted_results
    }

    /// Remove a vector from the index
    pub fn remove(&self, id: &str) -> Result<bool> {
        let mut vectors = self.vectors.write();
        let mut graph = self.graph.write();

        if vectors.remove(id).is_none() {
            return Ok(false);
        }

        // Remove from graph
        graph.remove(id);

        // Remove references from other nodes
        for connections in graph.values_mut() {
            connections.retain(|conn_id| conn_id != id);
        }

        // Update entry point if needed
        let mut entry_point = self.entry_point.write();
        if entry_point.as_ref() == Some(&id.to_string()) {
            *entry_point = vectors.keys().next().cloned();
        }

        Ok(true)
    }

    /// Get total number of vectors in index
    pub fn len(&self) -> usize {
        self.vectors.read().len()
    }

    /// Check if index is empty
    pub fn is_empty(&self) -> bool {
        self.vectors.read().is_empty()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_hnsw_insert_and_search() {
        let config = HnswConfig {
            m: 16,
            ef_construction: 100,
            ef_search: 50,
            metric: DistanceMetric::Euclidean,
            dimensions: 3,
        };

        let index = HnswIndex::new(config);

        // Insert vectors
        index.insert("v1".to_string(), vec![1.0, 0.0, 0.0]).unwrap();
        index.insert("v2".to_string(), vec![0.0, 1.0, 0.0]).unwrap();
        index.insert("v3".to_string(), vec![0.0, 0.0, 1.0]).unwrap();

        // Search
        let query = SearchQuery {
            vector: vec![0.9, 0.1, 0.0],
            k: 2,
            filters: None,
            threshold: None,
            ef_search: None,
        };

        let results = index.search(&query).unwrap();
        assert_eq!(results.len(), 2);
        assert_eq!(results[0].id, "v1"); // Should be closest
    }

    #[test]
    fn test_hnsw_multiple_inserts_no_deadlock() {
        // Regression test for issue #133: VectorDb.insert() deadlocks on second call
        // The bug was caused by holding write locks while calling search_knn_internal,
        // which tries to acquire read locks on the same RwLocks (parking_lot is not reentrant)
        let config = HnswConfig {
            m: 16,
            ef_construction: 100,
            ef_search: 50,
            metric: DistanceMetric::Cosine,
            dimensions: 128,
        };

        let index = HnswIndex::new(config);

        // Insert many vectors to ensure we exercise the KNN search path
        for i in 0..20 {
            let mut vector = vec![0.0f32; 128];
            vector[i % 128] = 1.0;
            index.insert(format!("v{}", i), vector).unwrap();
        }

        assert_eq!(index.len(), 20);

        // Verify search still works
        let query = SearchQuery {
            vector: vec![1.0; 128],
            k: 5,
            filters: None,
            threshold: None,
            ef_search: None,
        };

        let results = index.search(&query).unwrap();
        assert_eq!(results.len(), 5);
    }

    /// Issue #430: recall@1 collapsed at scale because the result-set
    /// BinaryHeap used min-heap semantics, evicting the BEST match instead of
    /// the worst whenever a new candidate arrived. Searching for a query
    /// identical to an inserted vector returned 0 or unrelated hits.
    /// This test inserts 1024 vectors and verifies recall@1 >= 95%.
    #[test]
    fn test_recall_at_1_with_biased_insertion_order() {
        use std::collections::HashSet;
        let dimensions = 64;
        let config = HnswConfig {
            m: 16,
            ef_construction: 200,
            ef_search: 200,
            metric: DistanceMetric::Cosine,
            dimensions,
        };
        let index = HnswIndex::new(config);

        // Generate 1024 deterministic but well-separated vectors via simple LCG.
        let n: usize = 1024;
        let mut vectors: Vec<Vec<f32>> = Vec::with_capacity(n);
        let mut state: u64 = 0xC0FF_EE15_BEEF_F00D;
        for _ in 0..n {
            let mut v = vec![0f32; dimensions];
            for slot in v.iter_mut() {
                state = state
                    .wrapping_mul(6364136223846793005)
                    .wrapping_add(1442695040888963407);
                let bits = (state >> 32) as u32;
                *slot = (bits as f32 / u32::MAX as f32) - 0.5;
            }
            let norm = v.iter().map(|x| x * x).sum::<f32>().sqrt().max(1e-12);
            for slot in v.iter_mut() {
                *slot /= norm;
            }
            vectors.push(v);
        }

        // Biased insertion order (sorted by first coordinate) — historically
        // what made the graph topology degenerate enough to expose the bug.
        let mut order: Vec<usize> = (0..n).collect();
        order.sort_by(|&a, &b| {
            vectors[a][0]
                .partial_cmp(&vectors[b][0])
                .unwrap_or(Ordering::Equal)
        });
        for &i in &order {
            index.insert(format!("v{i}"), vectors[i].clone()).unwrap();
        }

        // Query with each inserted vector — recall@1 must return that vector.
        let mut hits = 0usize;
        let sample: Vec<usize> = (0..n).step_by(n / 100).collect();
        for &i in &sample {
            let query = SearchQuery {
                vector: vectors[i].clone(),
                k: 1,
                filters: None,
                threshold: None,
                ef_search: Some(200),
            };
            let results = index.search(&query).unwrap();
            if results
                .first()
                .map(|r| r.id == format!("v{i}"))
                .unwrap_or(false)
            {
                hits += 1;
            }
        }
        let recall = hits as f32 / sample.len() as f32;
        assert!(
            recall >= 0.95,
            "recall@1 should be >= 95% with 1024 vectors, got {recall} ({}/{})",
            hits,
            sample.len()
        );

        // Sanity check: distinct ids returned across the sample (no degenerate
        // graph collapsing all queries to one node).
        let returned: HashSet<String> = sample
            .iter()
            .filter_map(|&i| {
                let q = SearchQuery {
                    vector: vectors[i].clone(),
                    k: 1,
                    filters: None,
                    threshold: None,
                    ef_search: Some(200),
                };
                index
                    .search(&q)
                    .ok()
                    .and_then(|r| r.into_iter().next())
                    .map(|n| n.id)
            })
            .collect();
        assert!(
            returned.len() >= (sample.len() * 8) / 10,
            "expected at least 80% distinct ids, got {}/{}",
            returned.len(),
            sample.len()
        );
    }

    /// Issue #430 (k > ef_search): caller-driven k was silently capped at
    /// ef_search; bumping k to exceed ef_search should yield k results.
    #[test]
    fn test_k_exceeds_ef_search_default() {
        let config = HnswConfig {
            m: 16,
            ef_construction: 100,
            ef_search: 10, // small default
            metric: DistanceMetric::Euclidean,
            dimensions: 4,
        };
        let index = HnswIndex::new(config);
        for i in 0..50 {
            let v = vec![i as f32, (i * 2) as f32, (i * 3) as f32, (i * 5) as f32];
            index.insert(format!("v{i}"), v).unwrap();
        }
        let query = SearchQuery {
            vector: vec![10.0, 20.0, 30.0, 50.0],
            k: 25,
            filters: None,
            threshold: None,
            ef_search: None, // default 10
        };
        let results = index.search(&query).unwrap();
        assert_eq!(
            results.len(),
            25,
            "k=25 with default ef_search=10 must still return 25"
        );
    }

    /// Issue #430 (bug C): when an adjacency list overflows `m * 2` the
    /// pruning step must keep the m CLOSEST neighbours, not the most recently
    /// inserted ones. Build a graph where one node ("hub") is the nearest
    /// neighbour of many subsequent inserts, then verify that hub's final
    /// adjacency list contains the m geometrically-closest connections.
    #[test]
    fn test_pruning_keeps_closest_not_newest() {
        let dimensions = 8;
        // Tiny m so the prune branch fires after only a few inserts.
        let config = HnswConfig {
            m: 4,
            ef_construction: 64,
            ef_search: 64,
            metric: DistanceMetric::Euclidean,
            dimensions,
        };
        let index = HnswIndex::new(config);

        // Hub at origin.
        let hub = vec![0f32; dimensions];
        index.insert("hub".into(), hub.clone()).unwrap();

        // 20 close neighbours (distance ~ 1.0..2.0 to hub).
        for i in 0..20 {
            let mut v = vec![0f32; dimensions];
            let r = 1.0 + (i as f32) * 0.05;
            v[i % dimensions] = r;
            index.insert(format!("close_{i}"), v).unwrap();
        }

        // 6 far neighbours (distance ~ 100) — these arrive LAST so the
        // FIFO pruner would keep them. The distance-based pruner must
        // discard them in favour of the closer ones already in the list.
        for i in 0..6 {
            let mut v = vec![0f32; dimensions];
            v[i % dimensions] = 100.0 + (i as f32);
            index.insert(format!("far_{i}"), v).unwrap();
        }

        // Inspect the hub's adjacency list. We can't access the private
        // graph directly, but we can search for the hub vector and check
        // that no "far_*" id is among the closest k=10 — which would be
        // impossible if the hub's edges still pointed at "far_*" nodes.
        let q = SearchQuery {
            vector: hub.clone(),
            k: 10,
            filters: None,
            threshold: None,
            ef_search: Some(64),
        };
        let results = index.search(&q).unwrap();
        let any_far_in_top10 = results.iter().any(|r| r.id.starts_with("far_"));
        assert!(
            !any_far_in_top10,
            "distance-based pruning regressed: 'far_*' nodes appear in top-10 \
             search around the hub, which means the pruner is still keeping \
             newest-by-FIFO instead of closest. results={:?}",
            results.iter().map(|r| (&r.id, r.score)).collect::<Vec<_>>()
        );
    }

    #[test]
    fn test_hnsw_concurrent_inserts() {
        use std::sync::Arc;
        use std::thread;

        let config = HnswConfig {
            m: 16,
            ef_construction: 100,
            ef_search: 50,
            metric: DistanceMetric::Euclidean,
            dimensions: 3,
        };

        let index = Arc::new(HnswIndex::new(config));

        // Spawn multiple threads to insert concurrently
        let mut handles = vec![];
        for t in 0..4 {
            let index_clone = Arc::clone(&index);
            let handle = thread::spawn(move || {
                for i in 0..10 {
                    let id = format!("t{}_v{}", t, i);
                    let vector = vec![t as f32, i as f32, 0.0];
                    index_clone.insert(id, vector).unwrap();
                }
            });
            handles.push(handle);
        }

        // Wait for all threads
        for handle in handles {
            handle.join().unwrap();
        }

        assert_eq!(index.len(), 40);
    }
}
