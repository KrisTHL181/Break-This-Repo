//! Forecast-band anomaly detection on a synthetic telemetry series.
//!
//! Models the RuVector/host-telemetry use case (disk-fill, GPU memory, query
//! load): learn the normal daily rhythm, forecast the next window, and flag
//! observed points that leave their p10/p90 band. Run with real weights:
//!
//! ```ignore
//! cargo run -p ruvector-timesfm --features candle --release --example telemetry_anomaly \
//!     -- /tmp/timesfm-parity/timesfm.safetensors
//! ```
//!
//! Skips cleanly (exit 0) when weights are absent — never fabricates a result.

use ruvector_timesfm::Forecaster;

/// A daily-seasonal telemetry signal: sinusoidal load + slow upward drift.
fn synth_series(n: usize) -> Vec<f32> {
    (0..n)
        .map(|t| {
            let day = (t as f32 / 24.0) * std::f32::consts::TAU;
            50.0 + 20.0 * day.sin() + 0.05 * t as f32
        })
        .collect()
}

fn main() -> anyhow::Result<()> {
    let weights = std::env::args()
        .nth(1)
        .or_else(|| std::env::var("TIMESFM_WEIGHTS").ok())
        .unwrap_or_else(|| "/tmp/timesfm-parity/timesfm.safetensors".into());
    if !std::path::Path::new(&weights).exists() {
        eprintln!("SKIP telemetry_anomaly: weights missing ({weights}). Not fabricating a result.");
        return Ok(());
    }

    let device = timesfm::select_device()?;
    let forecaster = Forecaster::load(&weights, device)?;

    // 256 points of history; observe the next 32, with two injected spikes.
    let history = synth_series(256);
    let mut observed = synth_series_window(256, 32);
    observed[8] += 60.0; // sudden spike (e.g. disk write storm)
    observed[20] -= 45.0; // sudden drop (e.g. sensor dropout)

    let report = forecaster.detect_anomalies(&history, &observed, 0)?;
    println!(
        "telemetry_anomaly: scored {} steps, {} anomalies",
        report.points.len(),
        report.n_anomalies
    );
    for a in report.anomalies() {
        println!(
            "  step {:2}: observed={:7.2} band=[{:7.2}, {:7.2}] expected={:7.2} deviation={:+.2}×band",
            a.index, a.observed, a.lower, a.upper, a.expected, a.deviation
        );
    }
    // The two injected spikes should be the dominant anomalies.
    assert!(
        report.n_anomalies >= 2,
        "expected the two injected spikes to be flagged, got {}",
        report.n_anomalies
    );
    Ok(())
}

/// Continuation of `synth_series` starting at `offset`, `len` points long.
fn synth_series_window(offset: usize, len: usize) -> Vec<f32> {
    (offset..offset + len)
        .map(|t| {
            let day = (t as f32 / 24.0) * std::f32::consts::TAU;
            50.0 + 20.0 * day.sin() + 0.05 * t as f32
        })
        .collect()
}
