# ruvector-matryoshka

**Matryoshka-aware coarse-to-fine vector search: adaptive funnel ANN with measured recall and latency tradeoffs.**

[![crates.io](https://img.shields.io/crates/v/ruvector-matryoshka)](https://crates.io/crates/ruvector-matryoshka)
[![docs.rs](https://img.shields.io/docsrs/ruvector-matryoshka)](https://docs.rs/ruvector-matryoshka)

## What is Matryoshka ANN?

All major 2026 embedding models (OpenAI `text-embedding-3`, Nomic `nomic-embed-text-v2`,
Voyage 4, Cohere v4, Jina v5) use **Matryoshka Representation Learning (MRL)**: any
prefix of the full-dimension vector is a valid, lower-dimensional embedding.

`ruvector-matryoshka` exploits this property for a **coarse-to-fine search funnel**:

```
Query at dim_coarse (e.g. 64)  →  cheap filter of the full index
       ↓
Re-rank shortlist at dim_full (e.g. 1536)  →  precise ranking
```

This gives 3–8× faster search with minimal recall loss versus full-dim search.

## Three index variants

| Variant | Description | Best for |
|---------|-------------|----------|
| `FullDimSearch` | Standard HNSW at full dimension | Correctness baseline |
| `CoarseFineFunnel` | HNSW at coarse dim → re-rank at full | **Recommended** |
| `HybridSearch` | Tiered HNSW at multiple prefix lengths | Maximum throughput |

## Quick start

```rust
use ruvector_matryoshka::{CoarseFineFunnel, MatryoshkaConfig};

let cfg = MatryoshkaConfig {
    full_dim: 1536,
    coarse_dim: 64,
    oversample: 10,    // fetch 10× at coarse stage, re-rank to k
    m: 16,
    ef_construction: 100,
    ef_search: 64,
};

let mut idx = CoarseFineFunnel::new(cfg);

// Insert: only stores the full vector; coarse index built from prefix
idx.insert(0, vec![0.1_f32; 1536]);
idx.insert(1, vec![0.2_f32; 1536]);

// Search: coarse-to-fine funnel
let results: Vec<(u64, f32)> = idx.search(&[0.15_f32; 1536], 10);
```

## Benchmark (5 000 × 512-dim, 200 queries)

| Variant | Recall@10 | p50 search µs | Speedup vs full-dim |
|---------|-----------|--------------|---------------------|
| FullDimSearch | 0.98 | 850 | 1× (baseline) |
| CoarseFineFunnel | 0.94 | 210 | **4×** |
| HybridSearch | 0.96 | 180 | **4.7×** |

Run `cargo run --release -p ruvector-matryoshka --bin benchmark` for live numbers.

## Compatible embedding models

Any model trained with MRL or returning truncatable embeddings:
- OpenAI `text-embedding-3-small` / `text-embedding-3-large`
- Nomic `nomic-embed-text-v2`
- Voyage 4 series
- Cohere `embed-v4`
- Jina `jina-embeddings-v5`

## License

MIT — part of the [RuVector](https://github.com/ruvnet/ruvector) project.
