//! # RuView SkyGraph Appliance — core pipeline (ADR-199, Phases 1–4)
//!
//! A local sky-monitoring appliance core that observes, projects, records, and
//! explains activity above a fixed physical location — driven here by a fully
//! deterministic synthetic ADS-B + weather scenario (no network access).
//!
//! The sky is treated as a continuously changing spatial graph, not a
//! dashboard (ADR-199 §1):
//!
//! ```text
//! synthetic ADS-B (adsb) ──► canonical observations (observation, §11)
//!        │                          │ WGS-84 → ECEF → ENU → az/el/range (coords, §10)
//!        ▼                          ▼
//!  track stitching (track) ──► SkyGraph nodes + edges (skygraph, §12)
//!        │                          ▲
//!        ▼                          │
//!  embeddings (embedding, §13) ─► RuVector index (indexer) ─► novelty
//!        │                                                      │
//!        ▼                                                      ▼
//!  anomaly scoring (anomaly, §15) ────────────────► daily sky brief (brief, §21.3)
//! ```
//!
//! [`pipeline::Pipeline`] orchestrates the end-to-end run shared by the demo
//! binary, the acceptance tests, and the criterion benches.

pub mod adsb;
pub mod anomaly;
pub mod brief;
pub mod config;
pub mod coords;
pub mod embedding;
#[cfg(feature = "appliance")]
pub mod indexer;
pub mod observation;
#[cfg(feature = "appliance")]
pub mod pipeline;
#[cfg(feature = "appliance")]
pub mod skygraph;
pub mod track;
pub mod weather;

pub use adsb::{parse_dump1090, AircraftState, ANOMALOUS_ICAO24, GA_OVERHEAD_ICAO24};
pub use anomaly::{
    score_summary, score_track, AnomalyComponents, AnomalyReport, BaselineStats, Interpretation,
    TrackSummary,
};
pub use brief::DailySkyBrief;
pub use config::{AnomalyConfig, ObserverConfig};
pub use coords::{geodetic_to_ecef, observer_frame, Ecef, Enu, ObserverFrame};
pub use embedding::{
    track_embedding, track_embedding_from_samples, weather_window_embedding, EmbeddingSample,
    TRACK_EMBEDDING_DIM,
};
#[cfg(feature = "appliance")]
pub use indexer::TrackIndexer;
pub use observation::{EntityType, GeoPosition, Motion, Observation};
#[cfg(feature = "appliance")]
pub use pipeline::{Pipeline, PipelineReport};
#[cfg(feature = "appliance")]
pub use skygraph::{SkyGraph, TrackExplanation};
pub use track::{stitch_tracks, Track, TrackPoint};
pub use weather::{WeatherCondition, WeatherWindow};

/// Crate-level error type unifying the vector store, graph store, and JSON
/// parsing failure modes encountered by the pipeline.
#[derive(Debug, thiserror::Error)]
pub enum SkyError {
    /// Error from the `ruvector-core` vector database.
    #[cfg(feature = "appliance")]
    #[error("vector store error: {0}")]
    Vector(#[from] ruvector_core::RuvectorError),
    /// Error from the `ruvector-graph` graph database.
    #[cfg(feature = "appliance")]
    #[error("graph store error: {0}")]
    Graph(#[from] ruvector_graph::GraphError),
    /// JSON decode error (e.g. malformed dump1090 payload).
    #[error("json error: {0}")]
    Json(#[from] serde_json::Error),
    /// Pipeline-level invariant violation.
    #[error("pipeline error: {0}")]
    Pipeline(String),
}

/// Crate-wide result alias.
pub type Result<T> = std::result::Result<T, SkyError>;
