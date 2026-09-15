//! Appliance configuration (ADR-199 §30).
//!
//! Defaults reproduce the reference deployment from the ADR configuration
//! sketch: the Oakville node at 43.4675 N, -79.6877 E, 100 m AMSL, with the
//! ADR §15 anomaly weights and the 0.76 local-alert threshold.

use serde::{Deserialize, Serialize};

/// A fixed physical observer (sensor node) on the ground.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ObserverConfig {
    /// Stable node name (used as `sensor_id` / Observer node id).
    pub name: String,
    /// Geodetic latitude in degrees (WGS-84).
    pub lat: f64,
    /// Geodetic longitude in degrees (WGS-84).
    pub lon: f64,
    /// Altitude above the WGS-84 ellipsoid, metres.
    pub alt_m: f64,
}

impl Default for ObserverConfig {
    fn default() -> Self {
        Self {
            name: "oakville_node".to_string(),
            lat: 43.4675,
            lon: -79.6877,
            alt_m: 100.0,
        }
    }
}

/// Anomaly-scoring configuration (ADR-199 §15 and §30).
///
/// The six weights mirror the composite formula exactly:
///
/// ```text
/// anomaly_score = 0.30 * route_deviation
///               + 0.20 * altitude_deviation
///               + 0.15 * time_of_day_rarity
///               + 0.15 * signal_unusualness
///               + 0.10 * cross_sensor_confirmation
///               + 0.10 * novelty_score
/// ```
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AnomalyConfig {
    /// Weight of the route (heading corridor) deviation component.
    pub w_route_deviation: f64,
    /// Weight of the altitude deviation component.
    pub w_altitude_deviation: f64,
    /// Weight of the time-of-day rarity component.
    pub w_time_of_day_rarity: f64,
    /// Weight of the signal-strength unusualness component.
    pub w_signal_unusualness: f64,
    /// Weight of the cross-sensor confirmation component (Phase 5 placeholder).
    pub w_cross_sensor_confirmation: f64,
    /// Weight of the RuVector novelty component.
    pub w_novelty_score: f64,
    /// Scores at or above this raise a local alert (ADR band "Strong anomaly").
    pub alert_threshold: f64,
    /// Minimum number of prior tracks required before a track is scored.
    /// (The ADR mandates a baseline period before alerting, §26.)
    pub min_history: usize,
}

impl Default for AnomalyConfig {
    fn default() -> Self {
        Self {
            w_route_deviation: 0.30,
            w_altitude_deviation: 0.20,
            w_time_of_day_rarity: 0.15,
            w_signal_unusualness: 0.15,
            w_cross_sensor_confirmation: 0.10,
            w_novelty_score: 0.10,
            alert_threshold: 0.76,
            min_history: 5,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn default_observer_is_oakville_node() {
        let cfg = ObserverConfig::default();
        assert_eq!(cfg.name, "oakville_node");
        assert!((cfg.lat - 43.4675).abs() < 1e-9);
        assert!((cfg.lon + 79.6877).abs() < 1e-9);
        assert!((cfg.alt_m - 100.0).abs() < 1e-9);
    }

    #[test]
    fn anomaly_weights_sum_to_one() {
        let c = AnomalyConfig::default();
        let sum = c.w_route_deviation
            + c.w_altitude_deviation
            + c.w_time_of_day_rarity
            + c.w_signal_unusualness
            + c.w_cross_sensor_confirmation
            + c.w_novelty_score;
        assert!((sum - 1.0).abs() < 1e-9);
        assert!((c.alert_threshold - 0.76).abs() < 1e-9);
    }
}
