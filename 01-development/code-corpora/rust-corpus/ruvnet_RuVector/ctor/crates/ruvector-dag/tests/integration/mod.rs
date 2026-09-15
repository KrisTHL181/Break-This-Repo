//! Integration tests for Neural DAG Learning

// Integration test modules; relax pedantic lints that don't affect correctness.
#![allow(
    unused_imports,
    clippy::manual_range_contains,
    clippy::len_zero,
    clippy::comparison_chain,
    clippy::absurd_extreme_comparisons,
    unused_comparisons
)]

mod attention_tests;
mod dag_tests;
mod healing_tests;
mod mincut_tests;
mod sona_tests;
