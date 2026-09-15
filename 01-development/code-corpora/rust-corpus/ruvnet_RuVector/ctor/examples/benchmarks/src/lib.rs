//! RuVector Benchmarks Library
//!
//! Comprehensive benchmarking suite for:
//! - Temporal reasoning (TimePuzzles-style constraint inference)
//! - Vector index operations (IVF, coherence-gated search)
//! - Swarm controller regret tracking
//! - Intelligence metrics and cognitive capability assessment
//! - Adaptive learning with ReasoningBank trajectory tracking
//!
//! Based on research from:
//! - TimePuzzles benchmark (arXiv:2601.07148)
//! - Sublinear regret in multi-agent control
//! - Tool-augmented iterative temporal reasoning
//! - Cognitive capability assessment frameworks
//! - lean-agentic type theory for verified reasoning

// Benchmark library: relax pedantic style lints that don't affect benchmark fidelity.
#![allow(
    clippy::collapsible_if,
    clippy::collapsible_match,
    clippy::manual_clamp,
    clippy::too_many_arguments,
    clippy::field_reassign_with_default,
    clippy::derivable_impls,
    clippy::needless_range_loop,
    clippy::explicit_counter_loop,
    clippy::redundant_closure,
    clippy::manual_range_contains,
    clippy::manual_is_multiple_of,
    clippy::assign_op_pattern,
    clippy::new_without_default,
    clippy::unnecessary_sort_by,
    clippy::doc_lazy_continuation,
    clippy::empty_line_after_doc_comments,
    clippy::unnecessary_unwrap,
    dead_code,
    unused_imports,
    unused_variables,
    unused_mut,
    unused_assignments
)]

pub mod acceptance_test;
pub mod agi_contract;
pub mod intelligence_metrics;
pub mod logging;
pub mod loop_gating;
pub mod publishable_rvf;
pub mod reasoning_bank;
pub mod rvf_artifact;
pub mod rvf_intelligence_bench;
pub mod superintelligence;
pub mod swarm_regret;
pub mod temporal;
pub mod timepuzzles;
pub mod vector_index;

pub use intelligence_metrics::*;
pub use logging::*;
pub use reasoning_bank::*;
pub use swarm_regret::*;
pub use temporal::*;
pub use timepuzzles::*;
pub use vector_index::*;
