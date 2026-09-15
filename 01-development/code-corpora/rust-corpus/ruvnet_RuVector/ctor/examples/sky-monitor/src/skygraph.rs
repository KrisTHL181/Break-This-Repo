//! SkyGraph (ADR-199 §12), built on `ruvector_graph::GraphDB`.
//!
//! Node labels: `Observer`, `Aircraft`, `Track`, `Observation` (the three
//! evidence samples per track: first / closest approach / last),
//! `WeatherCell`, `TimeWindow`, `Anomaly`.
//!
//! Edge types (ADR §12 vocabulary):
//! * `part_of_track` — Observation → Track,
//! * `observed_by`   — Track → Observer,
//! * `during`        — Track/WeatherCell/Anomaly → TimeWindow (hourly),
//! * `near`          — Track → Observer when closest approach < 10 km,
//! * `correlated_with` — Track → WeatherCell overlapping in time, and
//!   Track → Anomaly linking a flagged track to its score,
//! * `similar_to`    — Track → Track vector-similarity link,
//! * `anomalous_relative_to` — Anomaly → the baseline Track it deviates from.

use crate::anomaly::AnomalyReport;
use crate::config::ObserverConfig;
use crate::track::Track;
use crate::weather::WeatherWindow;
use chrono::{DateTime, Duration, Timelike, Utc};
use ruvector_graph::{EdgeBuilder, GraphDB, Node, NodeBuilder, PropertyValue};

/// Structured, citeable explanation of a track (ADR §27 governance rule 1:
/// every insight links back to observations).
#[derive(Debug, Clone)]
pub struct TrackExplanation {
    pub track_id: String,
    pub aircraft_id: String,
    pub callsign: String,
    /// Evidence lines, each citing graph nodes / observation ids.
    pub evidence: Vec<String>,
}

/// The sky-as-a-graph store.
pub struct SkyGraph {
    graph: GraphDB,
    observer_node_id: String,
}

fn prop_str(node: &Node, key: &str) -> String {
    match node.get_property(key) {
        Some(PropertyValue::String(s)) => s.clone(),
        _ => String::new(),
    }
}

fn prop_i64(node: &Node, key: &str) -> i64 {
    match node.get_property(key) {
        Some(PropertyValue::Integer(i)) => *i,
        _ => 0,
    }
}

fn prop_f64(node: &Node, key: &str) -> f64 {
    match node.get_property(key) {
        Some(PropertyValue::Float(f)) => *f,
        Some(PropertyValue::Integer(i)) => *i as f64,
        _ => 0.0,
    }
}

impl SkyGraph {
    /// Create the graph with its Observer node.
    pub fn new(observer: &ObserverConfig) -> crate::Result<Self> {
        let graph = GraphDB::new();
        let observer_node_id = format!("observer:{}", observer.name);
        graph.create_node(
            NodeBuilder::new()
                .id(observer_node_id.clone())
                .label("Observer")
                .property("name", observer.name.as_str())
                .property("lat", observer.lat)
                .property("lon", observer.lon)
                .property("alt_m", observer.alt_m)
                .build(),
        )?;
        Ok(Self {
            graph,
            observer_node_id,
        })
    }

    fn time_window_id(ts: DateTime<Utc>) -> String {
        format!("window:{}", ts.format("%Y-%m-%dT%H"))
    }

    /// Get-or-create the hourly TimeWindow node containing `ts`.
    fn ensure_time_window(&self, ts: DateTime<Utc>) -> crate::Result<String> {
        let id = Self::time_window_id(ts);
        if self.graph.get_node(&id).is_none() {
            let start = ts
                .date_naive()
                .and_hms_opt(ts.hour(), 0, 0)
                .unwrap()
                .and_utc();
            self.graph.create_node(
                NodeBuilder::new()
                    .id(id.clone())
                    .label("TimeWindow")
                    .property("start_epoch", start.timestamp())
                    .property("end_epoch", (start + Duration::hours(1)).timestamp())
                    .property("start_iso", start.to_rfc3339())
                    .build(),
            )?;
        }
        Ok(id)
    }

    /// Hourly windows overlapped by `[start, end]`.
    fn hours_covering(start: DateTime<Utc>, end: DateTime<Utc>) -> Vec<DateTime<Utc>> {
        let mut t = start
            .date_naive()
            .and_hms_opt(start.hour(), 0, 0)
            .unwrap()
            .and_utc();
        let mut out = Vec::new();
        while t <= end {
            out.push(t);
            t += Duration::hours(1);
        }
        out
    }

    /// Insert a WeatherCell node and its `during` edges.
    pub fn add_weather_window(&self, w: &WeatherWindow) -> crate::Result<()> {
        self.graph.create_node(
            NodeBuilder::new()
                .id(w.window_id.clone())
                .label("WeatherCell")
                .property("condition", w.condition.as_str())
                .property("wind_mps", w.wind_mps)
                .property("precip_mm_hr", w.precip_mm_hr)
                .property("alert", w.alert.clone().unwrap_or_default())
                .property("start_epoch", w.start.timestamp())
                .property("end_epoch", w.end.timestamp())
                .build(),
        )?;
        for h in Self::hours_covering(w.start, w.end - Duration::seconds(1)) {
            let win = self.ensure_time_window(h)?;
            self.graph
                .create_edge(EdgeBuilder::new(w.window_id.clone(), win, "during").build())?;
        }
        Ok(())
    }

    /// Insert Aircraft + Track + evidence Observation nodes and all rule-layer
    /// edges for one stitched track. Weather correlation edges are created
    /// against the already-inserted weather windows.
    pub fn add_track(&self, track: &Track, weather: &[WeatherWindow]) -> crate::Result<String> {
        let aircraft_id = format!("aircraft:{}", track.icao24);
        if self.graph.get_node(&aircraft_id).is_none() {
            self.graph.create_node(
                NodeBuilder::new()
                    .id(aircraft_id.clone())
                    .label("Aircraft")
                    .property("icao24", track.icao24.as_str())
                    .property("callsign", track.callsign.as_str())
                    .build(),
            )?;
        }
        let (first_obs, closest_obs, last_obs) = track.evidence_observation_ids();
        let cf = track.closest_frame();
        self.graph.create_node(
            NodeBuilder::new()
                .id(track.track_id.clone())
                .label("Track")
                .property("icao24", track.icao24.as_str())
                .property("callsign", track.callsign.as_str())
                .property("started_epoch", track.started.timestamp())
                .property("ended_epoch", track.ended.timestamp())
                .property("started_iso", track.started.to_rfc3339())
                .property("min_range_m", track.min_range_m)
                .property("max_elevation_deg", track.max_elevation_deg)
                .property("closest_azimuth_deg", cf.azimuth_deg)
                .property("dominant_heading_deg", track.dominant_heading_deg())
                .property("mean_altitude_m", track.mean_altitude_m())
                .property("overhead", track.is_overhead_candidate)
                .property("n_points", track.points.len() as i64)
                .property("first_observation_id", first_obs.to_string())
                .property("closest_observation_id", closest_obs.to_string())
                .property("last_observation_id", last_obs.to_string())
                .build(),
        )?;
        // Evidence observation nodes (first / closest / last samples).
        for (role, oid) in [
            ("first", first_obs),
            ("closest_approach", closest_obs),
            ("last", last_obs),
        ] {
            let node_id = format!("obs:{oid}");
            self.graph.create_node(
                NodeBuilder::new()
                    .id(node_id.clone())
                    .label("Observation")
                    .property("observation_id", oid.to_string())
                    .property("role", role)
                    .build(),
            )?;
            self.graph.create_edge(
                EdgeBuilder::new(node_id, track.track_id.clone(), "part_of_track")
                    .property("role", role)
                    .build(),
            )?;
        }
        // Track relationships.
        self.graph.create_edge(
            EdgeBuilder::new(track.track_id.clone(), aircraft_id, "part_of_track").build(),
        )?;
        self.graph.create_edge(
            EdgeBuilder::new(
                track.track_id.clone(),
                self.observer_node_id.clone(),
                "observed_by",
            )
            .property("min_range_m", track.min_range_m)
            .build(),
        )?;
        if track.min_range_m < crate::track::OVERHEAD_RANGE_M {
            self.graph.create_edge(
                EdgeBuilder::new(
                    track.track_id.clone(),
                    self.observer_node_id.clone(),
                    "near",
                )
                .property("range_m", track.min_range_m)
                .property("at", track.closest_approach.to_rfc3339())
                .build(),
            )?;
        }
        for h in Self::hours_covering(track.started, track.ended) {
            let win = self.ensure_time_window(h)?;
            self.graph
                .create_edge(EdgeBuilder::new(track.track_id.clone(), win, "during").build())?;
        }
        for w in weather
            .iter()
            .filter(|w| w.overlaps(track.started, track.ended))
        {
            self.graph.create_edge(
                EdgeBuilder::new(
                    track.track_id.clone(),
                    w.window_id.clone(),
                    "correlated_with",
                )
                .property("kind", "weather_context")
                .build(),
            )?;
        }
        Ok(track.track_id.clone())
    }

    /// Vector-similarity link between two tracks.
    pub fn add_similarity(
        &self,
        from_track: &str,
        to_track: &str,
        distance: f32,
    ) -> crate::Result<()> {
        self.graph.create_edge(
            EdgeBuilder::new(from_track.to_string(), to_track.to_string(), "similar_to")
                .property("distance", distance as f64)
                .build(),
        )?;
        Ok(())
    }

    /// Insert an Anomaly node for a scored track. `baseline_track_id` is the
    /// most similar prior track (the baseline the anomaly deviates from).
    pub fn add_anomaly(
        &self,
        report: &AnomalyReport,
        baseline_track_id: Option<&str>,
    ) -> crate::Result<String> {
        let anomaly_id = format!("anomaly:{}", report.track_id);
        self.graph.create_node(
            NodeBuilder::new()
                .id(anomaly_id.clone())
                .label("Anomaly")
                .property("track_id", report.track_id.as_str())
                .property("score", report.score)
                .property("band", report.band.to_string())
                .property("reasons", report.reasons.join("; "))
                .build(),
        )?;
        self.graph.create_edge(
            EdgeBuilder::new(
                report.track_id.clone(),
                anomaly_id.clone(),
                "correlated_with",
            )
            .property("kind", "anomaly_score")
            .build(),
        )?;
        if let Some(baseline) = baseline_track_id {
            if self.graph.get_node(baseline).is_some() {
                self.graph.create_edge(
                    EdgeBuilder::new(
                        anomaly_id.clone(),
                        baseline.to_string(),
                        "anomalous_relative_to",
                    )
                    .build(),
                )?;
            }
        }
        Ok(anomaly_id)
    }

    /// Aircraft active in `[start, end]`: `(icao24, track_id)` pairs.
    pub fn aircraft_in_window(
        &self,
        start: DateTime<Utc>,
        end: DateTime<Utc>,
    ) -> Vec<(String, String)> {
        let (s, e) = (start.timestamp(), end.timestamp());
        let mut out: Vec<(String, String)> = self
            .graph
            .get_nodes_by_label("Track")
            .into_iter()
            .filter(|n| prop_i64(n, "started_epoch") <= e && prop_i64(n, "ended_epoch") >= s)
            .map(|n| (prop_str(&n, "icao24"), n.id))
            .collect();
        out.sort();
        out
    }

    /// Track ids satisfying the §14 overhead rule.
    pub fn overhead_candidates(&self) -> Vec<String> {
        let mut ids: Vec<String> = self
            .graph
            .get_nodes_by_property("overhead", &PropertyValue::Boolean(true))
            .into_iter()
            .map(|n| n.id)
            .collect();
        ids.sort();
        ids
    }

    /// `(node_count, edge_count)`.
    pub fn stats(&self) -> (usize, usize) {
        (self.graph.node_count(), self.graph.edge_count())
    }

    /// Structured explanation listing the graph evidence for a track.
    pub fn explain(&self, track_id: &str) -> Option<TrackExplanation> {
        let node = self.graph.get_node(track_id)?;
        let mut evidence = Vec::new();
        evidence.push(format!(
            "track {track_id} stitched from {} observations; evidence observation ids: first {}, closest approach {}, last {}",
            prop_i64(&node, "n_points"),
            prop_str(&node, "first_observation_id"),
            prop_str(&node, "closest_observation_id"),
            prop_str(&node, "last_observation_id"),
        ));
        evidence.push(format!(
            "geometry: closest approach {:.0} m at azimuth {:.0}°, max elevation {:.1}°, dominant heading {:.0}°, mean altitude {:.0} m",
            prop_f64(&node, "min_range_m"),
            prop_f64(&node, "closest_azimuth_deg"),
            prop_f64(&node, "max_elevation_deg"),
            prop_f64(&node, "dominant_heading_deg"),
            prop_f64(&node, "mean_altitude_m"),
        ));
        for edge in self.graph.get_outgoing_edges(&track_id.to_string()) {
            match edge.edge_type.as_str() {
                "observed_by" => evidence.push(format!("observed_by {}", edge.to)),
                "near" => {
                    evidence.push(format!("near {} (closest approach inside 10 km)", edge.to))
                }
                "during" => evidence.push(format!("during {}", edge.to)),
                "part_of_track" => evidence.push(format!("flight of {}", edge.to)),
                "similar_to" => evidence.push(format!("similar_to {}", edge.to)),
                "correlated_with" => {
                    if let Some(w) = self.graph.get_node(&edge.to) {
                        if w.has_label("WeatherCell") {
                            evidence.push(format!(
                                "correlated_with {} ({}, wind {:.1} m/s)",
                                edge.to,
                                prop_str(&w, "condition"),
                                prop_f64(&w, "wind_mps")
                            ));
                        } else if w.has_label("Anomaly") {
                            evidence.push(format!(
                                "anomaly score {:.2} ({}): {}",
                                prop_f64(&w, "score"),
                                prop_str(&w, "band"),
                                prop_str(&w, "reasons")
                            ));
                            for be in self.graph.get_outgoing_edges(&edge.to) {
                                if be.edge_type == "anomalous_relative_to" {
                                    evidence
                                        .push(format!("anomalous_relative_to baseline {}", be.to));
                                }
                            }
                        }
                    }
                }
                _ => {}
            }
        }
        Some(TrackExplanation {
            track_id: track_id.to_string(),
            aircraft_id: prop_str(&node, "icao24"),
            callsign: prop_str(&node, "callsign"),
            evidence,
        })
    }
}
