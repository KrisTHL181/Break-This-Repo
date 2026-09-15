//! Track stitching (ADR-199 §19 `skygraph-builder`, Phase 3).
//!
//! Groups per-aircraft observations into [`Track`] segments, splitting on
//! reception gaps, and computes the summary statistics used by the embeddings
//! (§13), the rule layer (§14), and anomaly scoring (§15).

use crate::config::ObserverConfig;
use crate::coords::ObserverFrame;
use crate::observation::{EntityType, Observation};
use chrono::{DateTime, Timelike, Utc};
use serde::{Deserialize, Serialize};
use std::collections::BTreeMap;
use uuid::Uuid;

/// Gap (seconds) after which a new track segment starts for the same icao24.
pub const TRACK_GAP_SECS: i64 = 60;
/// ADR-199 §14 rule 1: overhead candidate iff range < 10 km AND elevation > 5°.
pub const OVERHEAD_RANGE_M: f64 = 10_000.0;
pub const OVERHEAD_ELEVATION_DEG: f64 = 5.0;

/// One time-ordered point of a stitched track.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TrackPoint {
    pub observation_id: Uuid,
    pub ts: DateTime<Utc>,
    pub lat: f64,
    pub lon: f64,
    pub alt_m: f64,
    pub speed_mps: f64,
    pub track_deg: f64,
    pub vertical_rate_mps: f64,
    pub signal_dbfs: f64,
    pub frame: ObserverFrame,
}

/// A stitched flight path segment through local airspace.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Track {
    pub track_id: String,
    pub icao24: String,
    pub callsign: String,
    pub points: Vec<TrackPoint>,
    pub started: DateTime<Utc>,
    pub ended: DateTime<Utc>,
    /// Minimum slant range to the observer, metres.
    pub min_range_m: f64,
    /// Time of closest approach.
    pub closest_approach: DateTime<Utc>,
    /// Maximum elevation seen, degrees.
    pub max_elevation_deg: f64,
    /// ADR §14 rule 1 result.
    pub is_overhead_candidate: bool,
}

impl Track {
    fn from_points(
        icao24: String,
        callsign: String,
        segment: usize,
        points: Vec<TrackPoint>,
    ) -> Self {
        let started = points.first().map(|p| p.ts).unwrap_or_else(Utc::now);
        let ended = points.last().map(|p| p.ts).unwrap_or(started);
        let closest = points
            .iter()
            .min_by(|a, b| a.frame.range_m.total_cmp(&b.frame.range_m))
            .expect("track has at least one point");
        let min_range_m = closest.frame.range_m;
        let closest_approach = closest.ts;
        let max_elevation_deg = points
            .iter()
            .map(|p| p.frame.elevation_deg)
            .fold(f64::NEG_INFINITY, f64::max);
        let is_overhead_candidate =
            min_range_m < OVERHEAD_RANGE_M && max_elevation_deg > OVERHEAD_ELEVATION_DEG;
        Self {
            track_id: format!("track-{icao24}-{segment}"),
            icao24,
            callsign,
            points,
            started,
            ended,
            min_range_m,
            closest_approach,
            max_elevation_deg,
            is_overhead_candidate,
        }
    }

    pub fn duration_secs(&self) -> f64 {
        (self.ended - self.started).num_milliseconds() as f64 / 1000.0
    }

    pub fn mean_altitude_m(&self) -> f64 {
        mean(self.points.iter().map(|p| p.alt_m))
    }

    pub fn altitude_std_m(&self) -> f64 {
        std_dev(self.points.iter().map(|p| p.alt_m))
    }

    pub fn mean_speed_mps(&self) -> f64 {
        mean(self.points.iter().map(|p| p.speed_mps))
    }

    pub fn speed_std_mps(&self) -> f64 {
        std_dev(self.points.iter().map(|p| p.speed_mps))
    }

    pub fn mean_signal_dbfs(&self) -> f64 {
        mean(self.points.iter().map(|p| p.signal_dbfs))
    }

    pub fn mean_elevation_deg(&self) -> f64 {
        mean(self.points.iter().map(|p| p.frame.elevation_deg))
    }

    /// Circular-mean ground track, degrees in `[0, 360)`.
    pub fn dominant_heading_deg(&self) -> f64 {
        let (s, c) = self.points.iter().fold((0.0, 0.0), |(s, c), p| {
            let r = p.track_deg.to_radians();
            (s + r.sin(), c + r.cos())
        });
        crate::coords::normalize_deg(s.atan2(c).to_degrees())
    }

    /// Heading histogram over `bins` equal azimuth buckets (fractions sum 1).
    pub fn heading_histogram(&self, bins: usize) -> Vec<f64> {
        let mut h = vec![0.0; bins];
        for p in &self.points {
            let idx = ((p.track_deg.rem_euclid(360.0) / 360.0) * bins as f64) as usize % bins;
            h[idx] += 1.0;
        }
        let n = self.points.len().max(1) as f64;
        h.iter_mut().for_each(|v| *v /= n);
        h
    }

    /// Total path length, metres (sum of great-circle-approximated steps).
    pub fn path_length_m(&self) -> f64 {
        self.points
            .windows(2)
            .map(|w| flat_distance_m(w[0].lat, w[0].lon, w[1].lat, w[1].lon))
            .sum()
    }

    /// Net displacement / path length in `[0, 1]` (1 = perfectly straight).
    pub fn straightness(&self) -> f64 {
        let path = self.path_length_m();
        if path < 1.0 {
            return 1.0;
        }
        let (a, b) = (self.points.first().unwrap(), self.points.last().unwrap());
        (flat_distance_m(a.lat, a.lon, b.lat, b.lon) / path).clamp(0.0, 1.0)
    }

    /// Fraction of points climbing faster than +2 m/s.
    pub fn climb_ratio(&self) -> f64 {
        ratio(&self.points, |p| p.vertical_rate_mps > 2.0)
    }

    /// Fraction of points descending faster than −2 m/s.
    pub fn descent_ratio(&self) -> f64 {
        ratio(&self.points, |p| p.vertical_rate_mps < -2.0)
    }

    pub fn mean_abs_vertical_rate_mps(&self) -> f64 {
        mean(self.points.iter().map(|p| p.vertical_rate_mps.abs()))
    }

    /// UTC hour of the track start (0–23) — used for time-of-day rarity.
    pub fn start_hour_utc(&self) -> u32 {
        self.started.hour()
    }

    /// Observer frame at the closest point of approach.
    pub fn closest_frame(&self) -> &ObserverFrame {
        &self
            .points
            .iter()
            .min_by(|a, b| a.frame.range_m.total_cmp(&b.frame.range_m))
            .expect("track has at least one point")
            .frame
    }

    /// First / closest-approach / last observation ids (evidence links).
    pub fn evidence_observation_ids(&self) -> (Uuid, Uuid, Uuid) {
        let closest = self
            .points
            .iter()
            .min_by(|a, b| a.frame.range_m.total_cmp(&b.frame.range_m))
            .unwrap();
        (
            self.points.first().unwrap().observation_id,
            closest.observation_id,
            self.points.last().unwrap().observation_id,
        )
    }
}

fn mean(it: impl Iterator<Item = f64>) -> f64 {
    let (sum, n) = it.fold((0.0, 0usize), |(s, n), v| (s + v, n + 1));
    if n == 0 {
        0.0
    } else {
        sum / n as f64
    }
}

fn std_dev(it: impl Iterator<Item = f64> + Clone) -> f64 {
    let m = mean(it.clone());
    let (sq, n) = it.fold((0.0, 0usize), |(s, n), v| (s + (v - m) * (v - m), n + 1));
    if n == 0 {
        0.0
    } else {
        (sq / n as f64).sqrt()
    }
}

fn ratio(points: &[TrackPoint], pred: impl Fn(&TrackPoint) -> bool) -> f64 {
    if points.is_empty() {
        return 0.0;
    }
    points.iter().filter(|p| pred(p)).count() as f64 / points.len() as f64
}

/// Equirectangular-approximation ground distance, metres (fine at local scale).
fn flat_distance_m(lat1: f64, lon1: f64, lat2: f64, lon2: f64) -> f64 {
    let dy = (lat2 - lat1) * 111_132.0;
    let dx = (lon2 - lon1) * 111_320.0 * ((lat1 + lat2) / 2.0).to_radians().cos();
    dx.hypot(dy)
}

/// Stitch aircraft observations into tracks: group by `entity_id` (icao24),
/// order by time, and split whenever consecutive samples are more than
/// `gap_secs` apart. Non-aircraft observations are ignored.
pub fn stitch_tracks(
    _observer: &ObserverConfig,
    observations: &[Observation],
    gap_secs: i64,
) -> Vec<Track> {
    let mut by_aircraft: BTreeMap<String, Vec<&Observation>> = BTreeMap::new();
    for o in observations {
        if o.entity_type == EntityType::Aircraft {
            by_aircraft.entry(o.entity_id.clone()).or_default().push(o);
        }
    }
    let mut tracks = Vec::new();
    for (icao24, mut group) in by_aircraft {
        group.sort_by_key(|o| o.timestamp_utc);
        let mut segment: Vec<TrackPoint> = Vec::new();
        let mut seg_no = 0usize;
        let mut callsign = String::new();
        let mut last_ts: Option<DateTime<Utc>> = None;
        for o in group {
            if let (Some(prev), true) = (last_ts, !segment.is_empty()) {
                if (o.timestamp_utc - prev).num_seconds() > gap_secs {
                    tracks.push(Track::from_points(
                        icao24.clone(),
                        callsign.clone(),
                        seg_no,
                        std::mem::take(&mut segment),
                    ));
                    seg_no += 1;
                }
            }
            if callsign.is_empty() {
                callsign = o.callsign().unwrap_or("").to_string();
            }
            segment.push(TrackPoint {
                observation_id: o.observation_id,
                ts: o.timestamp_utc,
                lat: o.location.lat,
                lon: o.location.lon,
                alt_m: o.location.alt_m,
                speed_mps: o.motion.speed_mps,
                track_deg: o.motion.track_deg,
                vertical_rate_mps: o.motion.vertical_rate_mps,
                signal_dbfs: o.signal_dbfs().unwrap_or(-30.0),
                frame: o.observer_frame,
            });
            last_ts = Some(o.timestamp_utc);
        }
        if !segment.is_empty() {
            tracks.push(Track::from_points(icao24, callsign, seg_no, segment));
        }
    }
    tracks.sort_by_key(|t| t.started);
    tracks
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::adsb::{default_day_start, generate_scenario};
    use crate::observation::{GeoPosition, Motion};

    fn observations() -> Vec<Observation> {
        let cfg = ObserverConfig::default();
        generate_scenario(cfg.lat, cfg.lon, 42, default_day_start())
            .iter()
            .map(|s| {
                Observation::new(
                    &cfg,
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

    #[test]
    fn stitches_one_track_per_synthetic_flight() {
        let cfg = ObserverConfig::default();
        let tracks = stitch_tracks(&cfg, &observations(), TRACK_GAP_SECS);
        assert_eq!(tracks.len(), 10, "10 flights → 10 tracks");
        assert!(tracks.windows(2).all(|w| w[0].started <= w[1].started));
        let ga = tracks
            .iter()
            .find(|t| t.icao24 == crate::adsb::GA_OVERHEAD_ICAO24)
            .unwrap();
        assert!(ga.is_overhead_candidate, "GA pass must satisfy ADR rule 1");
        assert!((ga.dominant_heading_deg() - 88.0).abs() < 3.0);
        assert!(ga.straightness() > 0.95);
        let corridor = tracks.iter().find(|t| t.icao24 == "c01a01").unwrap();
        assert!(
            !corridor.is_overhead_candidate,
            "en-route corridor is > 10 km slant range"
        );
        assert!(corridor.mean_altitude_m() > 10_000.0);
    }
}
