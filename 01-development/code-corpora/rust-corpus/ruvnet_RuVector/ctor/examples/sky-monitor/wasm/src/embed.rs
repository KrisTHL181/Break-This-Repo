//! §13 track embedding + §15 vector novelty for the browser (ADR-199).
//!
//! The live dashboard feed carries `[t, lat, lon, alt_m, azimuth_deg,
//! elevation_deg, range_m]` per point plus one receiver `rssi` per track;
//! per-point speed / heading / vertical rate are derived here by finite
//! differences before calling the canonical
//! [`track_embedding_from_samples`](sky_monitor::embedding) so browser
//! embeddings share the exact native §13 normalization.
//!
//! [`novelty`] mirrors the native indexer calibration (`src/indexer.rs`):
//! `min(1, mean(top-3 prior distances) / 1.2)`, neutral `0.5` with no priors.
//! Brute-force distance is intentional — the browser store caps at ~5 000
//! embeddings, far below where an index would pay off.

use sky_monitor::embedding::{track_embedding_from_samples, EmbeddingSample, TRACK_EMBEDDING_DIM};
use wasm_bindgen::prelude::*;

/// Distance scale at which novelty saturates (`indexer::NOVELTY_CALIBRATION`).
const NOVELTY_CALIBRATION: f32 = 1.2;
/// Nearest prior neighbours averaged (`indexer::NOVELTY_K`).
const NOVELTY_K: usize = 3;
/// Below this many stored embeddings novelty is the neutral 0.5
/// (`indexer::MIN_NEIGHBOURS` — the §26 baseline period).
const MIN_NEIGHBOURS: usize = 1;

/// Fields per live point: `[t, lat, lon, alt_m, az_deg, el_deg, range_m]`.
const FIELDS: usize = 7;

/// Expand flat live points into [`EmbeddingSample`]s, deriving motion by
/// finite differences (each point gets the velocity of the step arriving at
/// it; the first point copies the second's — same convention as the live
/// feed's dead-reckoning, which only knows instantaneous velocity).
fn samples_from_flat(points: &[f64], rssi_dbfs: f64) -> Vec<EmbeddingSample> {
    let n = points.len() / FIELDS;
    let mut out = Vec::with_capacity(n);
    for i in 0..n {
        let p = &points[i * FIELDS..(i + 1) * FIELDS];
        out.push(EmbeddingSample {
            t_unix: p[0],
            lat: p[1],
            lon: p[2],
            alt_m: p[3],
            speed_mps: 0.0,
            track_deg: 0.0,
            vertical_rate_mps: 0.0,
            signal_dbfs: rssi_dbfs,
            azimuth_deg: p[4],
            elevation_deg: p[5],
            range_m: p[6],
        });
    }
    for i in 1..out.len() {
        let (a, b) = (out[i - 1], out[i]);
        let dt = b.t_unix - a.t_unix;
        if dt <= 0.0 {
            out[i].speed_mps = a.speed_mps;
            out[i].track_deg = a.track_deg;
            out[i].vertical_rate_mps = a.vertical_rate_mps;
            continue;
        }
        // Equirectangular step, identical to Track::path_length_m.
        let dy = (b.lat - a.lat) * 111_132.0;
        let dx = (b.lon - a.lon) * 111_320.0 * ((a.lat + b.lat) / 2.0).to_radians().cos();
        out[i].speed_mps = dx.hypot(dy) / dt;
        out[i].track_deg = if dx == 0.0 && dy == 0.0 {
            a.track_deg
        } else {
            dx.atan2(dy).to_degrees().rem_euclid(360.0)
        };
        out[i].vertical_rate_mps = (b.alt_m - a.alt_m) / dt;
    }
    if out.len() > 1 {
        out[0].speed_mps = out[1].speed_mps;
        out[0].track_deg = out[1].track_deg;
        out[0].vertical_rate_mps = out[1].vertical_rate_mps;
    }
    out
}

/// Embed one live track: `points` is a `Float64Array` of
/// `[t_unix, lat, lon, alt_m, azimuth_deg, elevation_deg, range_m]` per
/// sample (time-ordered), `rssi_dbfs` the track's receiver signal (use −20
/// when the feed carries none). Returns the 32-dim §13 embedding
/// (`Float32Array`), every dimension normalized to `[0, 1]`.
#[wasm_bindgen]
pub fn embed_track(points: &[f64], rssi_dbfs: f64) -> Vec<f32> {
    track_embedding_from_samples(&samples_from_flat(points, rssi_dbfs))
}

/// §15 `novelty_score` of `query` against `past` — a flattened
/// `Float32Array` of concatenated 32-dim prior embeddings. Mirrors
/// `TrackIndexer::novelty_score`: mean euclidean distance to the top-3
/// nearest priors, divided by the 1.2 calibration constant, clamped to 1;
/// neutral 0.5 when no priors exist.
#[wasm_bindgen]
pub fn novelty(query: &[f32], past: &[f32]) -> f32 {
    let n = past.len() / TRACK_EMBEDDING_DIM;
    if n < MIN_NEIGHBOURS || query.len() < TRACK_EMBEDDING_DIM {
        return 0.5;
    }
    let mut best = [f32::INFINITY; NOVELTY_K];
    for i in 0..n {
        let v = &past[i * TRACK_EMBEDDING_DIM..(i + 1) * TRACK_EMBEDDING_DIM];
        let mut d = query
            .iter()
            .zip(v)
            .map(|(a, b)| (a - b) * (a - b))
            .sum::<f32>()
            .sqrt();
        for slot in best.iter_mut() {
            if d < *slot {
                std::mem::swap(slot, &mut d);
            }
        }
    }
    let mut sum = 0.0f32;
    let mut used = 0usize;
    for d in best {
        if d.is_finite() {
            sum += d;
            used += 1;
        }
    }
    if used == 0 {
        return 0.5;
    }
    (sum / used as f32 / NOVELTY_CALIBRATION).min(1.0)
}

#[cfg(test)]
mod tests {
    use super::*;

    // 55 s of straight northbound flight at ~77.8 m/s, level 1 000 m.
    fn flat_points() -> Vec<f64> {
        let mut v = Vec::new();
        for i in 0..12 {
            let t = 1_781_100_000.0 + f64::from(i) * 5.0;
            let lat = 43.0 + f64::from(i) * 0.0035; // ≈ 389 m per 5 s step
            v.extend_from_slice(&[t, lat, -79.0, 1_000.0, 0.0, 45.0, 10_000.0]);
        }
        v
    }

    #[test]
    fn embeds_live_points_with_derived_motion() {
        let e = embed_track(&flat_points(), -20.0);
        assert_eq!(e.len(), TRACK_EMBEDDING_DIM);
        // Northbound: heading ≈ 0° → sin dim ≈ 0.5, cos dim ≈ 1.0.
        assert!((e[4] - 0.5).abs() < 0.05, "heading sin {}", e[4]);
        assert!(e[5] > 0.95, "heading cos {}", e[5]);
        assert!(e[14] > 0.99, "straightness {}", e[14]);
        // Derived speed ≈ 77.8 m/s → /300 ≈ 0.26.
        assert!((e[3] - 0.26).abs() < 0.03, "speed {}", e[3]);
        // Level flight: no climb/descent fractions.
        assert_eq!(e[12], 0.0);
        assert_eq!(e[13], 0.0);
    }

    #[test]
    fn novelty_neutral_empty_and_zero_for_repeats() {
        let e = embed_track(&flat_points(), -20.0);
        assert!((novelty(&e, &[]) - 0.5).abs() < 1e-6);
        let past: Vec<f32> = e.iter().chain(e.iter()).chain(e.iter()).copied().collect();
        assert!(novelty(&e, &past) < 1e-6);
    }

    #[test]
    fn novelty_saturates_for_distant_embeddings() {
        let q = vec![0.0f32; TRACK_EMBEDDING_DIM];
        let past = vec![0.5f32; TRACK_EMBEDDING_DIM]; // distance ≈ 2.83 ≫ 1.2
        assert!((novelty(&q, &past) - 1.0).abs() < 1e-6);
    }
}
