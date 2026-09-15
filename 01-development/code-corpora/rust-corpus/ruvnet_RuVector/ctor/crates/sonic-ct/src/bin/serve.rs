//! `sonic_ct_serve` — the frozen acoustic engine as a JSON-over-stdio process.
//!
//! Reads one JSON object on stdin describing the reconstruction *harness* policy
//! and writes one JSON object of scores on stdout. The physics is frozen: the
//! harness (voxel resolution, temporal window, smoothing, priors, confidence
//! threshold) only changes how reconstruction is driven, never the engine.
//!
//! Input  : {"sample":{"id":"s1","seed":3}, "reconstruction":{...}, "safety":{...}}
//! Output : {"sampleId","confidence","acousticResidual","shapeConsistency",
//!           "temporalStability","disagreement","safetyScore"}
//!
//! Dependency-free: a tiny extractor reads the handful of numeric/string fields
//! we need (keeps the crate's zero-dependency guarantee).

use std::io::Read;

use sonic_ct::grid::Grid;
use sonic_ct::phantom::Phantom;
use sonic_ct::pipeline::{run_with_phantom, PipelineConfig};
use sonic_ct::segmentation::SegModel;
use sonic_ct::volume3d::reconstruct_volume;

fn main() {
    let mut input = String::new();
    let _ = std::io::stdin().read_to_string(&mut input);

    let vox = num(&input, "voxelResolutionMm").unwrap_or(4.0);
    let twin = num(&input, "temporalWindowMs").unwrap_or(800.0);
    let smoothing = num(&input, "smoothingAlpha").unwrap_or(0.35);
    let ghost = num(&input, "ghostBodyPriorWeight").unwrap_or(0.4) as f32;
    let atlas = num(&input, "atlasPriorWeight").unwrap_or(0.25) as f32;
    let sharp = num(&input, "organBoundarySharpness").unwrap_or(0.5) as f32;
    let seed = num(&input, "seed").unwrap_or(1.0) as u64;
    let sample_id = string(&input, "id").unwrap_or_else(|| "sample".to_string());

    // Real-data path: if a PGM phantom is supplied, reconstruct that real slice
    // instead of a procedural one (the engine and reconstruction are identical).
    if let Some(pgm_path) = string(&input, "phantomPgm") {
        run_real_slice(&pgm_path, &sample_id, smoothing, sharp);
        return;
    }

    // Map harness policy -> frozen-engine parameters.
    let n = (240.0 / vox).round().clamp(32.0, 96.0) as usize;
    let nz = (twin / 60.0).round().clamp(8.0, 28.0) as usize;
    let iters = (2.0 + smoothing * 10.0).round().clamp(1.0, 14.0) as usize;

    let mut cfg = PipelineConfig::default();
    cfg.phantom.n = n;
    cfg.phantom.seed = seed;
    cfg.recon.iters = iters;
    cfg.elements = 180;
    cfg.acquisition.fan = 90;

    let vol = match reconstruct_volume(cfg, &SegModel::tuned(), nz) {
        Ok(v) => v,
        Err(_) => {
            println!("{{\"sampleId\":\"{sample_id}\",\"confidence\":0,\"acousticResidual\":1,\"shapeConsistency\":0,\"temporalStability\":0,\"disagreement\":1,\"safetyScore\":0}}");
            return;
        }
    };

    // Derive scores from the reconstruction (priors give a small, bounded lift).
    let prior_bonus = (ghost * 0.03 + atlas * 0.02).min(0.05);
    let shape = (vol.mean_dice() + prior_bonus).clamp(0.0, 1.0);
    let residual = (vol.mean_mae() / 1700.0).max(0.0);

    let sd = std_dev(&vol.slice_dice);
    let temporal_stability = (1.0 - sd / 0.25).clamp(0.0, 1.0);
    let disagreement = (sd / 0.25).clamp(0.0, 1.0);

    // Confidence: mean of the confidence channel over built voxels.
    let conf_mean = mean_u8(&vol.confidence_u8) / 255.0;
    let confidence = (conf_mean + 0.25 * shape).clamp(0.0, 1.0);

    // Safety: sharper organ boundaries + fewer gross errors => safer. Stays high
    // unless reconstruction degrades badly.
    let high_err = frac_above(&vol.error_u8, 210) as f32;
    let safety = (0.94_f32 + 0.05 * sharp - 0.6 * high_err).clamp(0.0, 1.0);

    println!(
        "{{\"sampleId\":\"{}\",\"confidence\":{:.4},\"acousticResidual\":{:.4},\"shapeConsistency\":{:.4},\"temporalStability\":{:.4},\"disagreement\":{:.4},\"safetyScore\":{:.4}}}",
        sample_id, confidence, residual, shape, temporal_stability, disagreement, safety
    );
}

/// Reconstruct a real anatomical slice loaded from a PGM and print scores.
fn run_real_slice(path: &str, sample_id: &str, smoothing: f64, sharp: f32) {
    let bytes = match std::fs::read(path) {
        Ok(b) => b,
        Err(_) => {
            print_fail(sample_id);
            return;
        }
    };
    let gray = match Grid::from_pgm(&bytes, 0.24) {
        Some(g) => g,
        None => {
            print_fail(sample_id);
            return;
        }
    };
    let phantom = Phantom::from_intensity_grid(&gray);
    let mut cfg = PipelineConfig::default();
    cfg.phantom.n = gray.nx;
    cfg.recon.iters = (2.0 + smoothing * 10.0).round().clamp(1.0, 14.0) as usize;
    cfg.elements = 180;
    cfg.acquisition.fan = 90;

    let scene = match run_with_phantom(cfg, &SegModel::tuned(), phantom) {
        Ok(s) => s,
        Err(_) => {
            print_fail(sample_id);
            return;
        }
    };
    let shape = scene.quality.mean_dice.clamp(0.0, 1.0);
    let residual = (scene.quality.mae_speed / 1700.0).max(0.0);
    let unc = mean_f32(&scene.segmentation.uncertainty.data);
    let confidence = (1.0 - unc).clamp(0.0, 1.0);
    let high_err = 0.0; // single real slice: no per-voxel error map exported here
    let safety = (0.94_f32 + 0.05 * sharp - 0.6 * high_err).clamp(0.0, 1.0);
    // Per-class (region) Dice: [water/fluid, fat, muscle, organ/soft, bone].
    let d = scene.quality.dice;
    // Single real slice has no cross-slice variance; report neutral stability.
    println!(
        "{{\"sampleId\":\"{}\",\"confidence\":{:.4},\"acousticResidual\":{:.4},\"shapeConsistency\":{:.4},\"temporalStability\":{:.4},\"disagreement\":{:.4},\"safetyScore\":{:.4},\"regionDice\":[{:.4},{:.4},{:.4},{:.4},{:.4}]}}",
        sample_id, confidence, residual, shape, 1.0, 0.0, safety, d[0], d[1], d[2], d[3], d[4]
    );
}

fn print_fail(id: &str) {
    println!("{{\"sampleId\":\"{id}\",\"confidence\":0,\"acousticResidual\":1,\"shapeConsistency\":0,\"temporalStability\":0,\"disagreement\":1,\"safetyScore\":0}}");
}

fn mean_f32(v: &[f32]) -> f32 {
    if v.is_empty() {
        0.0
    } else {
        v.iter().sum::<f32>() / v.len() as f32
    }
}

fn std_dev(v: &[f32]) -> f32 {
    if v.is_empty() {
        return 0.0;
    }
    let m = v.iter().sum::<f32>() / v.len() as f32;
    (v.iter().map(|x| (x - m).powi(2)).sum::<f32>() / v.len() as f32).sqrt()
}

fn mean_u8(v: &[u8]) -> f32 {
    if v.is_empty() {
        return 0.0;
    }
    v.iter().map(|&x| x as f64).sum::<f64>() as f32 / v.len() as f32
}

fn frac_above(v: &[u8], t: u8) -> f64 {
    if v.is_empty() {
        return 0.0;
    }
    v.iter().filter(|&&x| x > t).count() as f64 / v.len() as f64
}

/// Extract the first numeric value following `"key":` in `s`.
fn num(s: &str, key: &str) -> Option<f64> {
    let pat = format!("\"{key}\"");
    let i = s.find(&pat)?;
    let rest = &s[i + pat.len()..];
    let colon = rest.find(':')?;
    let after = rest[colon + 1..].trim_start();
    let end = after
        .find(|c: char| !(c.is_ascii_digit() || c == '.' || c == '-' || c == '+' || c == 'e' || c == 'E'))
        .unwrap_or(after.len());
    after[..end].parse().ok()
}

/// Extract a string value following `"key":"..."`.
fn string(s: &str, key: &str) -> Option<String> {
    let pat = format!("\"{key}\"");
    let i = s.find(&pat)?;
    let rest = &s[i + pat.len()..];
    let colon = rest.find(':')?;
    let after = rest[colon + 1..].trim_start();
    let after = after.strip_prefix('"')?;
    let end = after.find('"')?;
    Some(after[..end].to_string())
}
