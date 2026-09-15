# ruvector-speculative-ann

Experimental draft-then-verify approximate nearest-neighbour search for
RuVector. A scalar-quantized scan proposes candidates and exact `f32`
distances rerank them.

The adaptive controller changes its candidate multiplier only when callers
provide sampled ground-truth recall feedback. Calls without audited feedback
use the current multiplier unchanged.

This crate is research-tier. See
`docs/adr/ADR-272-speculative-ann-search.md` in the RuVector repository for
benchmarks, limitations, and the production roadmap.
