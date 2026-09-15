//! `ruvector-timesfm-forecast` — JSON-in/JSON-out forecasting CLI.
//!
//! This is the shell-out entry point for the RuVector `time_series_forecast`
//! MCP tool: an agent (or the MCP server) writes a JSON request on stdin and
//! reads a JSON forecast on stdout. The device is chosen via `TIMESFM_DEVICE`
//! (`cpu` | `cuda` | `metal`, default cpu).
//!
//! Request:  `{"weights":"/path/timesfm.safetensors","series":[...],"horizon":64,"freq_id":0}`
//! Response: `{"horizon":64,"point":[...],"p10":[...],"p50":[...],"p90":[...]}`
//!
//! Run: `echo '{"weights":"...","series":[...],"horizon":32}' | ruvector-timesfm-forecast`

use std::io::Read;

use ruvector_timesfm::{Error, Forecaster, Result};
use serde::{Deserialize, Serialize};

#[derive(Deserialize)]
struct Request {
    weights: String,
    series: Vec<f32>,
    horizon: usize,
    #[serde(default)]
    freq_id: u32,
}

#[derive(Serialize)]
struct Response {
    horizon: usize,
    device: String,
    point: Vec<f32>,
    p10: Vec<f32>,
    p50: Vec<f32>,
    p90: Vec<f32>,
}

fn run() -> Result<()> {
    let mut buf = String::new();
    std::io::stdin().read_to_string(&mut buf)?;
    if buf.trim().is_empty() {
        return Err(Error::Invalid(
            "no JSON request on stdin (expected {weights, series, horizon})".into(),
        ));
    }
    let req: Request = serde_json::from_str(&buf)?;

    let device = timesfm::select_device()?;
    let device_label = std::env::var("TIMESFM_DEVICE").unwrap_or_else(|_| "cpu".into());

    let forecaster = Forecaster::load(&req.weights, device)?;
    let forecast = forecaster.forecast_with_freq(&req.series, req.horizon, req.freq_id)?;

    let resp = Response {
        horizon: forecast.horizon(),
        device: device_label,
        p10: forecast.p10(),
        p50: forecast.p50(),
        p90: forecast.p90(),
        point: forecast.point,
    };
    println!("{}", serde_json::to_string(&resp)?);
    Ok(())
}

fn main() {
    if let Err(e) = run() {
        eprintln!("ruvector-timesfm-forecast: {e}");
        std::process::exit(1);
    }
}
