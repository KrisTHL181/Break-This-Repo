//! Vertical-sweep 3-D volume scaffolding.
//!
//! The Midjourney-style scanner lowers the subject through the ring at a
//! constant rate, producing a stack of 2-D slices. This module models that
//! sweep as an ordered set of slice configurations; full 3-D reconstruction
//! (and inter-slice regularisation) is future work (ADR-0008 roadmap).

use crate::pipeline::{run_slice, PipelineConfig};
use crate::segmentation::SegModel;
use crate::types::{Result, Tissue};

/// A planned vertical sweep through the body.
#[derive(Debug, Clone)]
pub struct SweepPlan {
    /// Base pipeline configuration applied to every slice.
    pub base: PipelineConfig,
    /// Z heights (m) of each slice plane, top to bottom.
    pub z_levels: Vec<f32>,
    /// Platform descent speed (m/s); Midjourney public target ≈ 0.05 m/s.
    pub descent_speed: f32,
}

impl SweepPlan {
    /// Build a uniform sweep of `slices` planes spanning `height` metres.
    pub fn uniform(base: PipelineConfig, slices: usize, height: f32, descent_speed: f32) -> Self {
        let n = slices.max(1);
        let mut z_levels = Vec::with_capacity(n);
        for i in 0..n {
            let t = if n == 1 { 0.0 } else { i as f32 / (n - 1) as f32 };
            z_levels.push(height * (0.5 - t)); // top (+) to bottom (-)
        }
        SweepPlan {
            base,
            z_levels,
            descent_speed: descent_speed.max(1e-3),
        }
    }

    /// Estimated scan duration (s) for the whole sweep.
    pub fn estimated_duration(&self) -> f32 {
        let span = match (self.z_levels.first(), self.z_levels.last()) {
            (Some(a), Some(b)) => (a - b).abs(),
            _ => 0.0,
        };
        span / self.descent_speed
    }

    /// Number of slices in the sweep.
    pub fn slices(&self) -> usize {
        self.z_levels.len()
    }
}

/// Global speed-of-sound window used to normalise volume textures (m/s).
pub const VOL_SPEED_LO: f32 = 1400.0;
/// Upper speed window for volume normalisation (m/s).
pub const VOL_SPEED_HI: f32 = 3100.0;
/// Error-map saturation point (m/s) — errors at/above this map to full red.
pub const VOL_ERROR_SAT: f32 = 900.0;

/// A reconstructed 3-D body volume assembled from a cranio-caudal slice sweep.
///
/// Four co-registered channels are kept separate (ADR-0005): ground truth,
/// reconstruction, error (|recon − truth|), and confidence (1 − uncertainty).
/// All are stored as browser-friendly `u8` with index `x + y*n + z*n*n`.
#[derive(Debug, Clone)]
pub struct Volume {
    /// Grid resolution per slice.
    pub n: usize,
    /// Number of slices (depth).
    pub nz: usize,
    /// Ground-truth tissue labels (`u8` class id).
    pub truth_labels: Vec<u8>,
    /// Reconstructed tissue labels.
    pub recon_labels: Vec<u8>,
    /// Reconstructed speed normalised to `[VOL_SPEED_LO, VOL_SPEED_HI]`.
    pub recon_speed_u8: Vec<u8>,
    /// Per-voxel absolute speed error, normalised by [`VOL_ERROR_SAT`].
    pub error_u8: Vec<u8>,
    /// Per-voxel confidence (255 = certain).
    pub confidence_u8: Vec<u8>,
    /// Mean Dice per slice (length `nz`).
    pub slice_dice: Vec<f32>,
    /// Mean absolute speed error per slice (m/s).
    pub slice_mae: Vec<f32>,
    /// Index of the worst (highest-MAE) slice.
    pub worst_slice: usize,
    /// Total valid measurements across the sweep.
    pub measurements: usize,
    /// Fraction of body voxels per tissue class (body composition).
    pub fractions: [f32; Tissue::COUNT],
}

impl Volume {
    /// Mean Dice across all slices.
    pub fn mean_dice(&self) -> f32 {
        if self.slice_dice.is_empty() {
            0.0
        } else {
            self.slice_dice.iter().sum::<f32>() / self.slice_dice.len() as f32
        }
    }

    /// Mean absolute speed error across all slices (m/s).
    pub fn mean_mae(&self) -> f32 {
        if self.slice_mae.is_empty() {
            0.0
        } else {
            self.slice_mae.iter().sum::<f32>() / self.slice_mae.len() as f32
        }
    }
}

/// Reconstruct a full body volume by sweeping `nz` cranio-caudal slices.
pub fn reconstruct_volume(
    cfg: PipelineConfig,
    model: &SegModel,
    nz: usize,
) -> Result<Volume> {
    let nz = nz.max(1);
    let n = cfg.phantom.n;
    let cells = n * n;
    let mut vol = Volume {
        n,
        nz,
        truth_labels: vec![0; cells * nz],
        recon_labels: vec![0; cells * nz],
        recon_speed_u8: vec![0; cells * nz],
        error_u8: vec![0; cells * nz],
        confidence_u8: vec![0; cells * nz],
        slice_dice: vec![0.0; nz],
        slice_mae: vec![0.0; nz],
        worst_slice: 0,
        measurements: 0,
        fractions: [0.0; Tissue::COUNT],
    };

    let mut class_counts = [0u64; Tissue::COUNT];
    let mut body_voxels = 0u64;
    let mut worst = f32::NEG_INFINITY;

    for zi in 0..nz {
        let z = if nz == 1 { 0.5 } else { zi as f32 / (nz - 1) as f32 };
        let scene = run_slice(cfg, model, z)?;
        let base = zi * cells;

        for i in 0..cells {
            let tl = scene.phantom.labels.data[i] as u8;
            let rl = scene.segmentation.labels.data[i] as u8;
            vol.truth_labels[base + i] = tl;
            vol.recon_labels[base + i] = rl;

            let rs = scene.recon_speed.data[i];
            vol.recon_speed_u8[base + i] = norm_u8(rs, VOL_SPEED_LO, VOL_SPEED_HI);

            let err = (scene.recon_speed.data[i] - scene.phantom.speed.data[i]).abs();
            vol.error_u8[base + i] = norm_u8(err, 0.0, VOL_ERROR_SAT);

            let conf = 1.0 - scene.segmentation.uncertainty.data[i];
            vol.confidence_u8[base + i] = norm_u8(conf, 0.0, 1.0);

            if tl != Tissue::Water as u8 {
                body_voxels += 1;
                class_counts[tl as usize] += 1;
            }
        }

        vol.slice_dice[zi] = scene.quality.mean_dice;
        vol.slice_mae[zi] = scene.quality.mae_speed;
        vol.measurements += scene.quality.measurements;
        if scene.quality.mae_speed > worst {
            worst = scene.quality.mae_speed;
            vol.worst_slice = zi;
        }
    }

    if body_voxels > 0 {
        for (frac, &count) in vol.fractions.iter_mut().zip(class_counts.iter()) {
            *frac = count as f32 / body_voxels as f32;
        }
    }
    Ok(vol)
}

#[inline]
fn norm_u8(v: f32, lo: f32, hi: f32) -> u8 {
    let t = ((v - lo) / (hi - lo).max(1e-6)).clamp(0.0, 1.0);
    (t * 255.0) as u8
}
