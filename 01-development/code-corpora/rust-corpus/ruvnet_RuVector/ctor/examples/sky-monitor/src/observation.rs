//! Canonical observation schema (ADR-199 §11).
//!
//! Every normalized observation, from any sensor, conforms to one schema so
//! that the SkyGraph, vector index, and assistant all consume a single shape.
//! `raw_ref` links every insight back to evidence; `embedding_ref` links every
//! observation into vector memory.

use crate::config::ObserverConfig;
use crate::coords::{observer_frame, ObserverFrame};
use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

/// The kind of entity an observation describes (ADR-199 §12 node types).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum EntityType {
    Aircraft,
    WeatherCell,
    RfEvent,
    AudioEvent,
    CameraEvent,
    Satellite,
}

impl EntityType {
    /// Stable lowercase name (matches the JSON encoding).
    pub fn as_str(&self) -> &'static str {
        match self {
            EntityType::Aircraft => "aircraft",
            EntityType::WeatherCell => "weather_cell",
            EntityType::RfEvent => "rf_event",
            EntityType::AudioEvent => "audio_event",
            EntityType::CameraEvent => "camera_event",
            EntityType::Satellite => "satellite",
        }
    }
}

/// Geodetic position of the observed entity (WGS-84).
#[derive(Debug, Clone, Copy, PartialEq, Serialize, Deserialize)]
pub struct GeoPosition {
    pub lat: f64,
    pub lon: f64,
    pub alt_m: f64,
}

/// Motion state of the observed entity.
#[derive(Debug, Clone, Copy, PartialEq, Serialize, Deserialize)]
pub struct Motion {
    /// Ground speed, metres per second.
    pub speed_mps: f64,
    /// Ground track, degrees clockwise from true north.
    pub track_deg: f64,
    /// Vertical rate, metres per second (positive = climbing).
    pub vertical_rate_mps: f64,
}

/// One normalized observation (ADR-199 §11).
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Observation {
    /// Unique observation id.
    pub observation_id: Uuid,
    /// UTC timestamp of the observation.
    pub timestamp_utc: DateTime<Utc>,
    /// Producing source, e.g. `adsb_local`, `adsb_synthetic`, `msc_geomet`.
    pub source: String,
    /// Producing sensor node, e.g. `sky_node_001`.
    pub sensor_id: String,
    /// Kind of entity observed.
    pub entity_type: EntityType,
    /// Resolved entity id (icao24 for aircraft, internal id otherwise).
    pub entity_id: String,
    /// Geodetic location of the entity.
    pub location: GeoPosition,
    /// Observer-relative frame (range/azimuth/elevation/bearing), computed
    /// from the observer configuration via the §10 projection pipeline.
    pub observer_frame: ObserverFrame,
    /// Motion state.
    pub motion: Motion,
    /// Free-form sensor attributes (callsign, squawk, signal_dbfs, ...).
    pub attributes: serde_json::Value,
    /// Confidence in `[0, 1]`.
    pub confidence: f64,
    /// Link back to raw evidence in the object store (governance rule 1).
    pub raw_ref: Option<String>,
    /// Link into RuVector memory, set once the observation is embedded.
    pub embedding_ref: Option<String>,
}

impl Observation {
    /// Build a normalized observation, computing `observer_frame` from the
    /// observer configuration and the entity location.
    #[allow(clippy::too_many_arguments)]
    pub fn new(
        observer: &ObserverConfig,
        source: impl Into<String>,
        entity_type: EntityType,
        entity_id: impl Into<String>,
        timestamp_utc: DateTime<Utc>,
        location: GeoPosition,
        motion: Motion,
        attributes: serde_json::Value,
        confidence: f64,
    ) -> Self {
        let frame = observer_frame(
            observer.lat,
            observer.lon,
            observer.alt_m,
            location.lat,
            location.lon,
            location.alt_m,
        );
        Self {
            observation_id: Uuid::new_v4(),
            timestamp_utc,
            source: source.into(),
            sensor_id: observer.name.clone(),
            entity_type,
            entity_id: entity_id.into(),
            location,
            observer_frame: frame,
            motion,
            attributes,
            confidence,
            raw_ref: None,
            embedding_ref: None,
        }
    }

    /// Signal strength attribute (dBFS), if present.
    pub fn signal_dbfs(&self) -> Option<f64> {
        self.attributes.get("signal_dbfs").and_then(|v| v.as_f64())
    }

    /// Callsign attribute, if present.
    pub fn callsign(&self) -> Option<&str> {
        self.attributes.get("callsign").and_then(|v| v.as_str())
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use chrono::TimeZone;

    #[test]
    fn observation_computes_observer_frame_and_roundtrips() {
        let cfg = ObserverConfig::default();
        let obs = Observation::new(
            &cfg,
            "adsb_synthetic",
            EntityType::Aircraft,
            "c01a01",
            Utc.with_ymd_and_hms(2026, 6, 8, 19, 0, 0).unwrap(),
            GeoPosition {
                lat: cfg.lat,
                lon: cfg.lon,
                alt_m: 1_200.0,
            },
            Motion {
                speed_mps: 210.0,
                track_deg: 247.0,
                vertical_rate_mps: -3.1,
            },
            serde_json::json!({ "callsign": "ACA123", "signal_dbfs": -18.4 }),
            0.92,
        );
        // Directly overhead at 1100 m above the observer.
        assert!((obs.observer_frame.elevation_deg - 90.0).abs() < 0.1);
        assert!((obs.observer_frame.range_m - 1_100.0).abs() < 1.0);
        assert_eq!(obs.sensor_id, "oakville_node");
        assert_eq!(obs.callsign(), Some("ACA123"));
        assert_eq!(obs.signal_dbfs(), Some(-18.4));

        let json = serde_json::to_string(&obs).unwrap();
        let back: Observation = serde_json::from_str(&json).unwrap();
        assert_eq!(back.observation_id, obs.observation_id);
        assert_eq!(back.entity_type, EntityType::Aircraft);
        assert!(json.contains("\"entity_type\":\"aircraft\""));
    }
}
