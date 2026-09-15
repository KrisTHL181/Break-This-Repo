//! # ruvector-adaptive-ann
//!
//! Adaptive recall-targeted ANN search: instead of manually tuning the beam
//! width (`ef`) parameter, the caller specifies a **recall target** (e.g. 0.95)
//! and the search engine automatically selects the minimum beam width that
//! achieves it.
//!
//! ## Problem
//!
//! HNSW and proximity-graph ANN require a search parameter `ef` (beam width /
//! candidate list size) that directly controls the recall–latency trade-off.
//! Too small → fast but low recall. Too large → high recall but slow. Finding
//! the right value requires tedious offline profiling and breaks when the data
//! distribution shifts.
//!
//! ## Solution
//!
//! This crate provides three strategies:
//!
//! | Variant | Recall evidence | Overhead | Best for |
//! |---------|-----------------|----------|----------|
//! | [`FixedEfSearch`] | None | None | Baseline |
//! | [`BinarySearchCalibrated`] | Per-query oracle comparison | ~7 searches | Evaluation |
//! | [`TableCalibratedSearch`] | Pre-calibrated mean estimate | Table lookup | Research deployment |
//!
//! ## Trait
//!
//! All variants implement [`RecallTargetedSearch`], which exposes a
//! `search_with_target` method taking a recall target in `[0.0, 1.0]`.
//!
//! ## Agent memory context
//!
//! Agent memory stores grow continuously and query distributions shift as
//! agent goals change. A recall target of 0.95 means the agent will retrieve
//! at least 9.5 of its 10 most relevant memories on average, without the
//! operator needing to re-tune `ef` after every data drift event.

pub mod calibrate;
pub mod dataset;
pub mod graph;
pub mod metrics;
pub mod search;

pub use calibrate::{CalibrationTable, Calibrator};
pub use graph::{FlatGraph, GraphConfig};
pub use metrics::{recall_at_k, LatencyStats};
pub use search::{BinarySearchCalibrated, FixedEfSearch, SearchResult, TableCalibratedSearch};

/// Core trait for all adaptive recall-targeted search strategies.
pub trait RecallTargetedSearch {
    /// Return the top-`k` approximate nearest neighbours to `query`.
    ///
    /// `recall_target` is in `[0.0, 1.0]`. The implementation chooses the
    /// smallest `ef` (beam width) it believes will satisfy the target.
    /// Actual recall depends on graph quality and calibration accuracy.
    fn search_with_target(&self, query: &[f32], k: usize, recall_target: f32) -> Vec<SearchResult>;

    /// Effective beam width used for the given recall target.
    /// Returns `None` when the value is fixed, ignored, or query-dependent and
    /// unavailable without executing the actual query.
    fn effective_ef_for_target(&self, recall_target: f32, k: usize) -> Option<usize>;
}
