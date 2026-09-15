//! Daily sky brief (ADR-199 §21.3).
//!
//! Renders the Oakville-style text block:
//!
//! > **Sky brief — Oakville, 2026-06-09.** 812 aircraft observed; 37 overhead
//! > candidates; 4 unusual tracks. Light rain 14:10–15:30. Most unusual
//! > event: low-altitude eastbound pass at 21:14 (confidence 0.78).

use crate::anomaly::{AnomalyReport, Interpretation};
use crate::config::ObserverConfig;
use crate::track::Track;
use crate::weather::{WeatherCondition, WeatherWindow};
use chrono::NaiveDate;
use serde::{Deserialize, Serialize};
use std::fmt;

/// Description of the day's most unusual event.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MostUnusual {
    pub description: String,
    /// The anomaly score doubles as the confidence that this is genuinely
    /// unusual (it is a calibrated 0–1 composite).
    pub confidence: f64,
}

/// One day's summary of the sky above the observer.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DailySkyBrief {
    pub observer: String,
    pub date: NaiveDate,
    pub aircraft_observed: usize,
    pub overhead_candidates: usize,
    /// Tracks scoring above "mildly unusual" (> 0.55).
    pub unusual_tracks: usize,
    /// Human-readable weather events (non-clear windows / alerts).
    pub weather_events: Vec<String>,
    pub most_unusual: Option<MostUnusual>,
}

impl DailySkyBrief {
    /// Build the brief from the day's tracks, anomaly reports, and weather.
    pub fn build(
        observer: &ObserverConfig,
        tracks: &[Track],
        reports: &[AnomalyReport],
        weather: &[WeatherWindow],
    ) -> Self {
        let date = tracks
            .first()
            .map(|t| t.started.date_naive())
            .unwrap_or_else(|| NaiveDate::from_ymd_opt(1970, 1, 1).unwrap());
        let aircraft: std::collections::BTreeSet<&str> =
            tracks.iter().map(|t| t.icao24.as_str()).collect();
        let overhead = tracks.iter().filter(|t| t.is_overhead_candidate).count();
        let unusual = reports
            .iter()
            .filter(|r| r.band > Interpretation::MildlyUnusual)
            .count();

        let mut weather_events = Vec::new();
        let mut rain_run: Option<(usize, usize)> = None;
        for (i, w) in weather.iter().enumerate() {
            if w.condition != WeatherCondition::Clear && w.condition != WeatherCondition::Cloudy {
                rain_run = Some(rain_run.map_or((i, i), |(s, _)| (s, i)));
            } else if let Some((s, e)) = rain_run.take() {
                weather_events.push(format!(
                    "{} {}–{} UTC",
                    weather[s].condition.as_str(),
                    weather[s].start.format("%H:%M"),
                    weather[e].end.format("%H:%M"),
                ));
            }
        }
        if let Some((s, e)) = rain_run {
            weather_events.push(format!(
                "{} {}–{} UTC",
                weather[s].condition.as_str(),
                weather[s].start.format("%H:%M"),
                weather[e].end.format("%H:%M"),
            ));
        }

        let most_unusual = reports
            .iter()
            .max_by(|a, b| a.score.total_cmp(&b.score))
            .filter(|r| r.score > 0.30)
            .map(|r| {
                let track = tracks.iter().find(|t| t.track_id == r.track_id);
                let when = track
                    .map(|t| t.closest_approach.format("%H:%M UTC").to_string())
                    .unwrap_or_default();
                let alt = track.map(|t| t.mean_altitude_m()).unwrap_or(0.0);
                let heading = track.map(|t| t.dominant_heading_deg()).unwrap_or(0.0);
                let who = if r.callsign.is_empty() {
                    format!("icao24 {}", r.icao24)
                } else {
                    r.callsign.clone()
                };
                MostUnusual {
                    description: format!(
                        "{}-altitude pass by {who} heading {heading:.0}° at {when} ({:.0} m): {}",
                        if alt < 1_500.0 { "low" } else { "high" },
                        alt,
                        r.reasons.first().cloned().unwrap_or_default()
                    ),
                    confidence: r.score,
                }
            });

        Self {
            observer: observer.name.clone(),
            date,
            aircraft_observed: aircraft.len(),
            overhead_candidates: overhead,
            unusual_tracks: unusual,
            weather_events,
            most_unusual,
        }
    }
}

impl fmt::Display for DailySkyBrief {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(
            f,
            "Sky brief — {}, {}. {} aircraft observed; {} overhead candidates; {} unusual track{}.",
            self.observer,
            self.date,
            self.aircraft_observed,
            self.overhead_candidates,
            self.unusual_tracks,
            if self.unusual_tracks == 1 { "" } else { "s" },
        )?;
        if self.weather_events.is_empty() {
            write!(f, " Weather: clear throughout.")?;
        } else {
            write!(f, " Weather: {}.", self.weather_events.join("; "))?;
        }
        if let Some(mu) = &self.most_unusual {
            write!(
                f,
                " Most unusual event: {} (confidence {:.2}).",
                mu.description, mu.confidence
            )?;
        }
        Ok(())
    }
}
