//! Plain forecast data types — available without the `candle` feature so
//! callers can hold/serialize forecasts in code that doesn't pull in the model.

use serde::{Deserialize, Serialize};

/// Number of quantile channels TimesFM emits (p10, p20, …, p90).
pub const NUM_QUANTILES: usize = 9;

/// A horizon forecast: a point (mean) estimate per step plus the nine
/// calibrated quantiles (p10..p90) per step.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct Forecast {
    /// Point (mean-channel) forecast, one value per horizon step.
    pub point: Vec<f32>,
    /// Per-step quantiles, `[p10, p20, p30, p40, p50, p60, p70, p80, p90]`.
    /// `quantiles.len() == point.len()`.
    pub quantiles: Vec<[f32; NUM_QUANTILES]>,
}

impl Forecast {
    /// Horizon length (number of forecast steps).
    #[must_use]
    pub fn horizon(&self) -> usize {
        self.point.len()
    }

    /// Extract quantile channel `q` (`0 => p10 … 8 => p90`) across all steps.
    /// Out-of-range indices are clamped into `0..NUM_QUANTILES`.
    #[must_use]
    pub fn quantile(&self, q: usize) -> Vec<f32> {
        let q = q.min(NUM_QUANTILES - 1);
        self.quantiles.iter().map(|row| row[q]).collect()
    }

    /// The p10 (lower) band, one value per step.
    #[must_use]
    pub fn p10(&self) -> Vec<f32> {
        self.quantile(0)
    }

    /// The p50 (median) band, one value per step.
    #[must_use]
    pub fn p50(&self) -> Vec<f32> {
        self.quantile(4)
    }

    /// The p90 (upper) band, one value per step.
    #[must_use]
    pub fn p90(&self) -> Vec<f32> {
        self.quantile(8)
    }
}
