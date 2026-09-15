# ruvector-bounded-rag

Research implementations of context-budgeted retrieval for RuVector.

The crate compares cosine top-k retrieval, priority traversal over a dense
similarity graph, and an Edmonds–Karp min-cut partition followed by relevance
ranking and budget truncation. The graph-based variants rebuild their pairwise
similarity structures per query and are intended as auditable research
baselines rather than production-scale indexes.

```sh
cargo test -p ruvector-bounded-rag
cargo run --release -p ruvector-bounded-rag --bin benchmark
```

See `docs/adr/ADR-272-bounded-rag-mincut.md` and the associated nightly
research report for methodology and limitations.
