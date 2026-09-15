# ruvector-recall-bounded

Research implementations of threshold-driven similarity search for RuVector.
The crate compares an exact linear scan with two bounded graph-search
heuristics and reports empirical recall against the exact baseline.

The approximate variants do not provide a formal recall guarantee. Their
expansion parameters must be calibrated and audited on representative data.

```sh
cargo test -p ruvector-recall-bounded
cargo run --release -p ruvector-recall-bounded --bin benchmark
```

See `docs/adr/ADR-272-recall-bounded-ann.md` and the associated nightly report
for methodology and limitations.
