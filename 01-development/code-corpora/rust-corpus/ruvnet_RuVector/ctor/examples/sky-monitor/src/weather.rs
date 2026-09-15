//! Synthetic weather context (ADR-199 §9.3 / Phase 2).
//!
//! Stand-in for the MSC GeoMet collector: produces hourly
//! [`WeatherWindow`]s aligned to the synthetic ADS-B timeline, including the
//! light-rain band the ADR's sample brief mentions (14:10–15:30) and calm
//! clear conditions overnight (so the anomalous 03:10 track has no weather
//! corroboration).

use crate::adsb::Lcg;
use chrono::{DateTime, Duration, Utc};
use serde::{Deserialize, Serialize};

/// Coarse sky condition for a window.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum WeatherCondition {
    Clear,
    Cloudy,
    Rain,
    Thunderstorm,
    Snow,
    Fog,
}

impl WeatherCondition {
    pub fn as_str(&self) -> &'static str {
        match self {
            WeatherCondition::Clear => "clear",
            WeatherCondition::Cloudy => "cloudy",
            WeatherCondition::Rain => "rain",
            WeatherCondition::Thunderstorm => "thunderstorm",
            WeatherCondition::Snow => "snow",
            WeatherCondition::Fog => "fog",
        }
    }
}

/// One bounded weather window over the observer (a `WeatherCell` node source).
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct WeatherWindow {
    pub window_id: String,
    pub start: DateTime<Utc>,
    pub end: DateTime<Utc>,
    pub condition: WeatherCondition,
    /// Mean wind speed, m/s.
    pub wind_mps: f64,
    /// Precipitation rate, mm/h.
    pub precip_mm_hr: f64,
    /// Official alert text, if any (drives the §14 weather-suppression rule).
    pub alert: Option<String>,
}

impl WeatherWindow {
    /// True if this window overlaps `[start, end]`.
    pub fn overlaps(&self, start: DateTime<Utc>, end: DateTime<Utc>) -> bool {
        self.start < end && start < self.end
    }
}

/// Generate 28 hourly windows from `day_start` (covers the next-night anomaly
/// at +27 h). Deterministic for a given seed.
pub fn generate_weather(seed: u64, day_start: DateTime<Utc>) -> Vec<WeatherWindow> {
    let mut rng = Lcg::new(seed ^ 0x5eed_2ea7_dead_beef);
    (0..28)
        .map(|h| {
            let start = day_start + Duration::hours(h);
            // Light rain band 14:00-16:00; cloudy shoulder hours; clear otherwise.
            let (condition, precip, alert) = match h {
                13 => (WeatherCondition::Cloudy, 0.0, None),
                14 | 15 => (
                    WeatherCondition::Rain,
                    1.2 + rng.next_f64() * 0.8,
                    Some("light rain advisory".to_string()),
                ),
                16 => (WeatherCondition::Cloudy, 0.1, None),
                _ => (WeatherCondition::Clear, 0.0, None),
            };
            WeatherWindow {
                window_id: format!("weather:{}", start.format("%Y-%m-%dT%H")),
                start,
                end: start + Duration::hours(1),
                condition,
                wind_mps: 2.0 + rng.next_f64() * 4.0,
                precip_mm_hr: precip,
                alert,
            }
        })
        .collect()
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::adsb::default_day_start;

    #[test]
    fn weather_timeline_is_deterministic_with_rain_band() {
        let a = generate_weather(42, default_day_start());
        let b = generate_weather(42, default_day_start());
        assert_eq!(a.len(), 28);
        assert_eq!(a[5].wind_mps.to_bits(), b[5].wind_mps.to_bits());
        assert_eq!(a[14].condition, WeatherCondition::Rain);
        assert!(a[14].alert.is_some());
        assert_eq!(
            a[27].condition,
            WeatherCondition::Clear,
            "anomaly hour is clear"
        );
        // Overlap math.
        assert!(a[14].overlaps(a[14].start, a[14].end));
        assert!(!a[14].overlaps(a[16].start, a[16].end));
    }
}
