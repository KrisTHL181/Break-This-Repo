//! Forecast-band anomaly detection.
//!
//! Forecast the expected window with TimesFM, then flag observed points that
//! fall outside their `[p10, p90]` quantile band. The band width is the model's
//! own calibrated uncertainty, so a point counts as anomalous only when it
//! leaves the range the model itself considered plausible — no hand-tuned
//! Z-score threshold per series.

use serde::{Deserialize, Serialize};

use crate::forecast_types::Forecast;

/// One observed point scored against its forecast band.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct AnomalyPoint {
    /// Step index within the observed window.
    pub index: usize,
    /// The observed value.
    pub observed: f32,
    /// Model's expected value (p50) for this step.
    pub expected: f32,
    /// Lower band (p10).
    pub lower: f32,
    /// Upper band (p90).
    pub upper: f32,
    /// Signed distance *outside* the band: positive above `upper`, negative
    /// below `lower`, `0.0` when inside the band. Normalized by band width.
    pub deviation: f32,
    /// `true` when the observed value fell outside `[lower, upper]`.
    pub is_anomaly: bool,
}

/// Result of scoring an observed window against a forecast.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct AnomalyReport {
    /// Per-step scoring, one entry per observed value.
    pub points: Vec<AnomalyPoint>,
    /// Number of points flagged as anomalous.
    pub n_anomalies: usize,
}

impl AnomalyReport {
    /// The flagged points only.
    #[must_use]
    pub fn anomalies(&self) -> Vec<&AnomalyPoint> {
        self.points.iter().filter(|p| p.is_anomaly).collect()
    }
}

/// Score `observed` against the bands in `forecast`.
///
/// Compares each observed value to the `[p10, p90]` band of the matching
/// forecast step. `deviation` is normalized by band width so it is comparable
/// across series of different scales. The number of points scored is
/// `min(observed.len(), forecast.horizon())`.
#[must_use]
pub fn score_window(forecast: &Forecast, observed: &[f32]) -> AnomalyReport {
    let p10 = forecast.p10();
    let p50 = forecast.p50();
    let p90 = forecast.p90();
    let n = observed.len().min(forecast.horizon());

    let mut points = Vec::with_capacity(n);
    let mut n_anomalies = 0;
    for i in 0..n {
        let (lower, expected, upper, obs) = (p10[i], p50[i], p90[i], observed[i]);
        // Band width; guard against a degenerate zero-width band.
        let width = (upper - lower).abs().max(f32::EPSILON);
        let deviation = if obs > upper {
            (obs - upper) / width
        } else if obs < lower {
            (obs - lower) / width // negative
        } else {
            0.0
        };
        let is_anomaly = deviation != 0.0;
        if is_anomaly {
            n_anomalies += 1;
        }
        points.push(AnomalyPoint {
            index: i,
            observed: obs,
            expected,
            lower,
            upper,
            deviation,
            is_anomaly,
        });
    }

    AnomalyReport {
        points,
        n_anomalies,
    }
}

#[cfg(feature = "candle")]
impl crate::Forecaster {
    /// Forecast `observed.len()` steps from `history`, then score each observed
    /// value against its `[p10, p90]` band. The forecast never sees `observed`,
    /// so this is a genuine out-of-sample anomaly check.
    pub fn detect_anomalies(
        &self,
        history: &[f32],
        observed: &[f32],
        freq_id: u32,
    ) -> crate::Result<AnomalyReport> {
        if observed.is_empty() {
            return Err(crate::Error::Invalid("observed window is empty".into()));
        }
        let forecast = self.forecast_with_freq(history, observed.len(), freq_id)?;
        Ok(score_window(&forecast, observed))
    }
}
