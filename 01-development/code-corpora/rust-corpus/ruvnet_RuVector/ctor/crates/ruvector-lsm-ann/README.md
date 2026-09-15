# ruvector-lsm-ann

**Write-optimised streaming vector index with multi-tier LSM compaction for agent memory.**

[![crates.io](https://img.shields.io/crates/v/ruvector-lsm-ann)](https://crates.io/crates/ruvector-lsm-ann)
[![docs.rs](https://img.shields.io/docsrs/ruvector-lsm-ann)](https://docs.rs/ruvector-lsm-ann)

## What is it?

`ruvector-lsm-ann` implements a **Log-Structured Merge ANN (LSM-ANN)** index — the
vector-database equivalent of an LSM-tree — designed for high-velocity write streams
where traditional HNSW would stall waiting for graph rebuilds.

```
L0  MemTable  (mutable, brute-force)   ← all writes land here first (<1 µs)
L1  Small frozen segments (NSW graph)  ← compacted from L0 in background
L2  Large merged segment  (NSW graph)  ← compacted from L1 on threshold
```

Queries fan out across all tiers and merge-rank by exact distance.

## When to use it

| Use case | Index to choose |
|----------|-----------------|
| High write rate (agent memory, streaming logs) | **ruvector-lsm-ann** |
| Read-heavy, infrequent writes | ruvector-diskann / HNSW |
| 1-bit ultra-compressed retrieval | ruvector-rabitq |

## Three variants

| Variant | Description | Recall | Insert speed |
|---------|-------------|--------|--------------|
| `BaselineLsm` | Flat MemTable, exact brute-force — oracle | 1.000 | Highest |
| `TwoTierLsm`  | MemTable + one frozen NSW segment | ≥0.85 | High |
| `FullLsm`     | MemTable + L1 segments + L2 merged | ≥0.85 | High |

## Quick start

```rust
use ruvector_lsm_ann::{FullLsm, LsmConfig, LsmIndex};

let cfg = LsmConfig {
    dims: 128,
    m: 16,
    ef_construction: 200,
    ef_search: 200,
    l0_max: 1_000,
    l1_merge_threshold: 5,
};

let mut idx = FullLsm::new(cfg);

// Insert vectors (each write is sub-microsecond until L0 flush)
idx.insert(42, vec![0.1_f32; 128]);
idx.insert(43, vec![0.2_f32; 128]);

// Trigger compaction (or set l0_max to trigger automatically)
idx.compact();

// Search
let results: Vec<(u64, f32)> = idx.search(&[0.15_f32; 128], 10);
```

## Benchmark (10 000 × 128-dim, ef=200)

| Variant | Insert/s | Recall@10 | p50 µs |
|---------|---------|-----------|--------|
| Baseline | ~2 M/s | 1.0000 | 420 |
| TwoTier  | ~180 K/s | 0.92 | 580 |
| FullLsm  | ~150 K/s | 0.91 | 640 |

Run `cargo run --release -p ruvector-lsm-ann --bin benchmark` for live numbers.

## License

MIT — part of the [RuVector](https://github.com/ruvnet/ruvector) project.
