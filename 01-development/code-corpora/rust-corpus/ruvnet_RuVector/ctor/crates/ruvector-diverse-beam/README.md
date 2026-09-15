# ruvector-diverse-beam

Research implementations of diverse approximate-nearest-neighbour search for
RuVector. The crate compares:

- greedy beam traversal;
- greedy traversal followed by Maximum Marginal Relevance reranking; and
- coherence-pruned traversal.

The implementation uses an exact flat k-nearest-neighbour graph so that the
search strategies can be measured in isolation. It is intended for research
and evaluation, not as a replacement for RuVector's production HNSW index.

Run the tests and benchmark with:

```sh
cargo test -p ruvector-diverse-beam
cargo run --release -p ruvector-diverse-beam --bin benchmark
```

See `docs/adr/ADR-272-diverse-beam-ann.md` and the nightly research report for
methodology, limitations, and measured results.
