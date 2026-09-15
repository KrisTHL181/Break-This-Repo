# ruvector-pq-search

**Product Quantization with Asymmetric Distance Computation (ADC) for compressed approximate nearest-neighbor search — 64× compression in safe Rust.**

[![crates.io](https://img.shields.io/crates/v/ruvector-pq-search)](https://crates.io/crates/ruvector-pq-search)
[![docs.rs](https://img.shields.io/docsrs/ruvector-pq-search)](https://docs.rs/ruvector-pq-search)

## What is PQ-ADC?

**Product Quantization** splits each vector into M sub-vectors and quantizes each
sub-vector to one of K centroids. A 128-dim f32 vector (512 bytes) can be stored as
16 bytes — **64× compression**.

**Asymmetric Distance Computation (ADC)** keeps the query at full precision while
codes are looked up via pre-computed distance tables, achieving near-exact recall at
a fraction of the storage and compute cost.

## Three index variants

| Variant | Description | Recall | Memory |
|---------|-------------|--------|--------|
| `FlatPq` | Flat scan over PQ codes with ADC | ≥0.85 | **64× less** |
| `IvfPq` | IVF coarse quantizer + PQ codes | ≥0.80 | **64× less** |
| `ResidualPq` | Residual-corrected PQ for higher recall | ≥0.90 | **64× less** |

## Quick start

```rust
use ruvector_pq_search::{FlatPq, PqConfig, PqIndex};

let cfg = PqConfig {
    dims: 128,
    m_subvecs: 8,      // 8 sub-vectors of 16 dims each
    k_centroids: 256,  // 256 centroids per sub-vector (8-bit codes)
    n_train: 50_000,   // training set size for k-means
};

let mut idx = FlatPq::new(cfg);

// Train on representative data
let train_data: Vec<Vec<f32>> = /* ... */;
idx.train(&train_data);

// Insert (stored as 8-byte PQ codes)
idx.insert(0, &[0.1_f32; 128]);

// Search with ADC (query stays at full precision)
let results: Vec<(u64, f32)> = idx.search(&[0.1_f32; 128], 10);
```

## Compression tradeoffs

| Method | Compression | Recall@10 | Build time |
|--------|-------------|-----------|------------|
| Raw f32 | 1× | 1.00 | — |
| RaBitQ (1-bit) | 512× | 0.70–0.90 | Fast |
| **PQ-ADC (8-bit)** | **64×** | **0.85–0.95** | **Medium** |
| Scalar quantized | 4× | 0.97 | Fast |

## Use cases

- **Memory-constrained edge deployment** (IoT, mobile, Pi 5)
- **Large-scale agent memory** where 64× storage reduction enables more history
- **Tiered retrieval**: PQ-ADC coarse filter → exact re-rank on shortlist

## License

MIT — part of the [RuVector](https://github.com/ruvnet/ruvector) project.
