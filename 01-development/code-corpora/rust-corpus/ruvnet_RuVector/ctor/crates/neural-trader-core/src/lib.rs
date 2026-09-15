//! # neural-trader-core
//!
//! Canonical market event types, graph schema, and ingest traits for
//! the RuVector Neural Trader (ADR-084).

use serde::{Deserialize, Serialize};

// ---------------------------------------------------------------------------
// Event types
// ---------------------------------------------------------------------------

/// Canonical market event envelope.
///
/// Every raw feed message is normalized into this structure before it
/// enters the graph or embedding pipeline.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct MarketEvent {
    pub event_id: [u8; 16],
    pub ts_exchange_ns: u64,
    pub ts_ingest_ns: u64,
    pub venue_id: u16,
    pub symbol_id: u32,
    pub event_type: EventType,
    pub side: Option<Side>,
    /// Fixed-point price (e.g. price × 1e8).
    pub price_fp: i64,
    /// Fixed-point quantity.
    pub qty_fp: i64,
    pub order_id_hash: Option<[u8; 16]>,
    pub participant_id_hash: Option<[u8; 16]>,
    pub flags: u32,
    pub seq: u64,
}

/// Discriminant for market event type.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[repr(u8)]
pub enum EventType {
    NewOrder = 0,
    ModifyOrder = 1,
    CancelOrder = 2,
    Trade = 3,
    BookSnapshot = 4,
    SessionMarker = 5,
    VenueStatus = 6,
}

/// Order side.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[repr(u8)]
pub enum Side {
    Bid = 0,
    Ask = 1,
}

// ---------------------------------------------------------------------------
// Graph node and edge kinds
// ---------------------------------------------------------------------------

/// Typed node kinds in the heterogeneous market graph.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
#[repr(u8)]
pub enum NodeKind {
    Symbol = 0,
    Venue = 1,
    PriceLevel = 2,
    Order = 3,
    Trade = 4,
    Event = 5,
    Participant = 6,
    TimeBucket = 7,
    Regime = 8,
    StrategyState = 9,
}

/// Typed edge kinds in the heterogeneous market graph.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
#[repr(u8)]
pub enum EdgeKind {
    AtLevel = 0,
    NextTick = 1,
    Generated = 2,
    Matched = 3,
    ModifiedFrom = 4,
    CanceledBy = 5,
    BelongsToSymbol = 6,
    OnVenue = 7,
    InWindow = 8,
    CorrelatedWith = 9,
    InRegime = 10,
    AffectsState = 11,
}

// ---------------------------------------------------------------------------
// Graph delta
// ---------------------------------------------------------------------------

/// Property keys for graph node updates.
///
/// Using an enum avoids heap-allocating strings on the hot ingest path.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
#[repr(u8)]
pub enum PropertyKey {
    VisibleDepth = 0,
    EstimatedHiddenDepth = 1,
    QueueLength = 2,
    LocalImbalance = 3,
    RefillRate = 4,
    DepletionRate = 5,
    SpreadDistance = 6,
    LocalRealizedVol = 7,
    CancelHazard = 8,
    FillHazard = 9,
    SlippageToMid = 10,
    PostTradeImpact = 11,
    InfluenceScore = 12,
    CoherenceContribution = 13,
    QueueEstimate = 14,
    Age = 15,
    ModifyCount = 16,
}

/// Describes changes applied to the market graph after processing one event.
#[derive(Debug, Clone, Default)]
pub struct GraphDelta {
    pub nodes_added: Vec<(NodeKind, u64)>,
    pub edges_added: Vec<(EdgeKind, u64, u64)>,
    pub properties_updated: Vec<(u64, PropertyKey, f64)>,
}

// ---------------------------------------------------------------------------
// Traits
// ---------------------------------------------------------------------------

/// Ingests normalized market events.
pub trait EventIngestor {
    fn ingest(&mut self, event: MarketEvent) -> anyhow::Result<()>;
}

/// Projects market events onto the dynamic graph.
pub trait GraphUpdater {
    fn apply_event(&mut self, event: &MarketEvent) -> anyhow::Result<GraphDelta>;
}

/// Produces vector embeddings from a state window.
pub trait Embedder {
    fn embed_state(&self, ctx: &StateWindow) -> anyhow::Result<Vec<f32>>;
}

/// A sliding window of graph state used for embedding.
#[derive(Debug, Clone)]
pub struct StateWindow {
    pub symbol_id: u32,
    pub venue_id: u16,
    pub start_ns: u64,
    pub end_ns: u64,
    pub events: Vec<MarketEvent>,
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn market_event_roundtrip_json() {
        let evt = MarketEvent {
            event_id: [1u8; 16],
            ts_exchange_ns: 1_000_000,
            ts_ingest_ns: 1_000_100,
            venue_id: 1,
            symbol_id: 42,
            event_type: EventType::Trade,
            side: Some(Side::Bid),
            price_fp: 500_000_000,
            qty_fp: 10_000,
            order_id_hash: None,
            participant_id_hash: None,
            flags: 0,
            seq: 1,
        };
        let json = serde_json::to_string(&evt).unwrap();
        let back: MarketEvent = serde_json::from_str(&json).unwrap();
        assert_eq!(back.symbol_id, 42);
        assert_eq!(back.event_type, EventType::Trade);
    }

    #[test]
    fn graph_delta_default_is_empty() {
        let d = GraphDelta::default();
        assert!(d.nodes_added.is_empty());
        assert!(d.edges_added.is_empty());
    }
}
