# ruvector-adaptive-ann

Research implementations of empirical `ef` calibration for graph-based
approximate nearest-neighbour search.

Calibration tables are specific to the result count (`k`), graph, and sampled
query distribution. They estimate mean recall observed during calibration and
do not provide a per-query recall guarantee. Recalibrate and audit whenever the
data, graph, or workload shifts.

```sh
cargo test -p ruvector-adaptive-ann
cargo run --release -p ruvector-adaptive-ann --bin benchmark
```

See `docs/adr/ADR-272-adaptive-recall-ann.md` and the associated nightly report
for methodology and limitations.
