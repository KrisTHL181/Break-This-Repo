//! End-to-end orchestration (ADR-199 Phases 1–4 in one pass).
//!
//! `scenario → observations → tracks → baseline split → RuVector index →
//! anomaly scores → SkyGraph → daily brief`. The demo binary, the acceptance
//! tests, and the criterion benches all run through [`Pipeline::run`] so they
//! exercise exactly the same path.

use crate::adsb::{default_day_start, generate_scenario};
use crate::anomaly::{score_track, AnomalyReport, BaselineStats, Interpretation};
use crate::brief::DailySkyBrief;
use crate::config::{AnomalyConfig, ObserverConfig};
use crate::embedding::{track_embedding, TRACK_EMBEDDING_DIM};
use crate::indexer::TrackIndexer;
use crate::observation::{EntityType, GeoPosition, Motion, Observation};
use crate::skygraph::SkyGraph;
use crate::track::{stitch_tracks, Track, TRACK_GAP_SECS};
use crate::weather::{generate_weather, WeatherWindow};
use chrono::{DateTime, Utc};

/// Everything a run produced (kept in memory for inspection / rendering).
pub struct PipelineReport {
    pub observations: Vec<Observation>,
    pub tracks: Vec<Track>,
    /// One anomaly report per scored track (tracks after the baseline split).
    pub reports: Vec<AnomalyReport>,
    /// Top cross-track similarity pairs `(track_a, track_b, distance)`.
    pub similar_pairs: Vec<(String, String, f32)>,
    pub skygraph: SkyGraph,
    pub weather: Vec<WeatherWindow>,
    pub brief: DailySkyBrief,
}

/// The end-to-end SkyGraph appliance pipeline over the synthetic scenario.
pub struct Pipeline {
    pub observer: ObserverConfig,
    pub anomaly: AnomalyConfig,
    pub seed: u64,
    pub day_start: DateTime<Utc>,
}

impl Default for Pipeline {
    fn default() -> Self {
        Self {
            observer: ObserverConfig::default(),
            anomaly: AnomalyConfig::default(),
            seed: 42,
            day_start: default_day_start(),
        }
    }
}

impl Pipeline {
    /// Phase 1: synthetic ADS-B samples → canonical observations (§11).
    pub fn observations(&self) -> Vec<Observation> {
        generate_scenario(
            self.observer.lat,
            self.observer.lon,
            self.seed,
            self.day_start,
        )
        .iter()
        .map(|s| {
            Observation::new(
                &self.observer,
                "adsb_synthetic",
                EntityType::Aircraft,
                s.icao24.clone(),
                s.ts,
                GeoPosition {
                    lat: s.lat,
                    lon: s.lon,
                    alt_m: s.alt_m,
                },
                Motion {
                    speed_mps: s.speed_mps,
                    track_deg: s.track_deg,
                    vertical_rate_mps: s.vertical_rate_mps,
                },
                serde_json::json!({ "callsign": s.callsign, "signal_dbfs": s.signal_dbfs }),
                0.95,
            )
        })
        .collect()
    }

    /// Phases 1–3 shortcut used by tests/benches: stitched tracks (time
    /// ordered) plus their embeddings.
    pub fn tracks_and_embeddings(&self) -> crate::Result<(Vec<Track>, Vec<Vec<f32>>)> {
        let observations = self.observations();
        let tracks = stitch_tracks(&self.observer, &observations, TRACK_GAP_SECS);
        let embeddings = tracks.iter().map(track_embedding).collect();
        Ok((tracks, embeddings))
    }

    /// Run the full pipeline.
    pub fn run(&self) -> crate::Result<PipelineReport> {
        // Phase 1: observe + normalize.
        let observations = self.observations();
        let weather = generate_weather(self.seed, self.day_start);

        // Phase 3: stitch tracks (already time ordered).
        let tracks = stitch_tracks(&self.observer, &observations, TRACK_GAP_SECS);

        // Phase 4: index in RuVector and score anomalies. The first
        // `min_history` tracks form the unscored baseline; every later track
        // is scored against strictly *prior* tracks, then indexed itself.
        let mut indexer = TrackIndexer::new(TRACK_EMBEDDING_DIM)?;
        let embeddings: Vec<Vec<f32>> = tracks.iter().map(track_embedding).collect();
        let mut reports = Vec::new();
        let mut nearest_baseline: Vec<Option<String>> = vec![None; tracks.len()];
        for (i, (track, embedding)) in tracks.iter().zip(&embeddings).enumerate() {
            if i >= self.anomaly.min_history {
                let baseline = BaselineStats::from_tracks(&tracks[..i]);
                let novelty = indexer.novelty_score(embedding)? as f64;
                // Phase 5 placeholder: no second sensor modality in the
                // synthetic scenario, so no cross-sensor confirmation.
                let cross_sensor = 0.0;
                reports.push(score_track(
                    &self.anomaly,
                    track,
                    &baseline,
                    novelty,
                    cross_sensor,
                ));
                nearest_baseline[i] = indexer
                    .similar_tracks(embedding, Some(&track.track_id), 1)?
                    .first()
                    .map(|(id, _)| id.clone());
            }
            indexer.insert_track(track, embedding.clone())?;
        }

        // Cross-track similarity pairs (deduplicated, closest first).
        let mut similar_pairs: Vec<(String, String, f32)> = Vec::new();
        for (track, embedding) in tracks.iter().zip(&embeddings) {
            if let Some((other, d)) = indexer
                .similar_tracks(embedding, Some(&track.track_id), 1)?
                .first()
            {
                let (a, b) = if track.track_id < *other {
                    (track.track_id.clone(), other.clone())
                } else {
                    (other.clone(), track.track_id.clone())
                };
                if !similar_pairs.iter().any(|(x, y, _)| *x == a && *y == b) {
                    similar_pairs.push((a, b, *d));
                }
            }
        }
        similar_pairs.sort_by(|a, b| a.2.total_cmp(&b.2));

        // Phase 3: build the SkyGraph.
        let skygraph = SkyGraph::new(&self.observer)?;
        for w in &weather {
            skygraph.add_weather_window(w)?;
        }
        for track in &tracks {
            skygraph.add_track(track, &weather)?;
        }
        for (a, b, d) in similar_pairs.iter().take(5) {
            skygraph.add_similarity(a, b, *d)?;
        }
        for report in &reports {
            if report.band > Interpretation::MildlyUnusual {
                let i = tracks.iter().position(|t| t.track_id == report.track_id);
                let baseline = i.and_then(|i| nearest_baseline[i].as_deref());
                skygraph.add_anomaly(report, baseline)?;
            }
        }

        // Phase 4: daily brief.
        let brief = DailySkyBrief::build(&self.observer, &tracks, &reports, &weather);

        Ok(PipelineReport {
            observations,
            tracks,
            reports,
            similar_pairs,
            skygraph,
            weather,
            brief,
        })
    }

    /// Serialize a run for the canvas dashboard (`ui/dashboard`): observer
    /// position plus per-track point series and anomaly verdicts.
    ///
    /// Shape: `{ observer: {name, lat, lon, alt_m}, day_start, tracks: [{
    /// icao24, callsign, overhead, points: [{t, lat, lon, alt_m}], anomaly:
    /// {score, band, reasons} | null }] }` — `t` is Unix epoch seconds and
    /// `anomaly` is `null` for the unscored baseline tracks (ADR §26).
    pub fn demo_export_json(&self, report: &PipelineReport) -> serde_json::Value {
        let round = |v: f64, scale: f64| (v * scale).round() / scale;
        let tracks: Vec<serde_json::Value> = report
            .tracks
            .iter()
            .map(|t| {
                let anomaly = report
                    .reports
                    .iter()
                    .find(|r| r.track_id == t.track_id)
                    .map(|r| {
                        serde_json::json!({
                            "score": round(r.score, 1e3),
                            "band": r.band.to_string(),
                            "reasons": r.reasons,
                        })
                    })
                    .unwrap_or(serde_json::Value::Null);
                let points: Vec<serde_json::Value> = t
                    .points
                    .iter()
                    .map(|p| {
                        serde_json::json!({
                            "t": p.ts.timestamp(),
                            "lat": round(p.lat, 1e6),
                            "lon": round(p.lon, 1e6),
                            "alt_m": round(p.alt_m, 10.0),
                        })
                    })
                    .collect();
                serde_json::json!({
                    "icao24": t.icao24,
                    "callsign": t.callsign,
                    "overhead": t.is_overhead_candidate,
                    "points": points,
                    "anomaly": anomaly,
                })
            })
            .collect();
        serde_json::json!({
            "observer": {
                "name": self.observer.name,
                "lat": self.observer.lat,
                "lon": self.observer.lon,
                "alt_m": self.observer.alt_m,
            },
            "day_start": self.day_start.to_rfc3339(),
            "tracks": tracks,
        })
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn pipeline_runs_end_to_end() {
        let report = Pipeline::default().run().unwrap();
        assert_eq!(report.tracks.len(), 10);
        assert_eq!(
            report.reports.len(),
            10 - AnomalyConfig::default().min_history
        );
        assert!(!report.similar_pairs.is_empty());
        let (nodes, edges) = report.skygraph.stats();
        assert!(nodes > 50, "expected a populated graph, got {nodes} nodes");
        assert!(edges > 80, "expected a populated graph, got {edges} edges");
        assert!(report.brief.aircraft_observed == 10);
    }
}
