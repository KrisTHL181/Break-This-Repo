//! Reconstruction and segmentation quality metrics.

use crate::grid::Grid;
use crate::types::Tissue;

/// Dice similarity coefficient for one class between predicted and truth labels.
///
/// Returns 1.0 for a class that is absent from both maps (vacuously perfect),
/// matching the common convention for empty-class Dice.
pub fn dice(pred_labels: &Grid, true_labels: &Grid, class: Tissue) -> f32 {
    let c = class as u8 as f32;
    let mut inter = 0u64;
    let mut a = 0u64;
    let mut b = 0u64;
    for (p, t) in pred_labels.data.iter().zip(&true_labels.data) {
        let pp = *p == c;
        let tt = *t == c;
        if pp {
            a += 1;
        }
        if tt {
            b += 1;
        }
        if pp && tt {
            inter += 1;
        }
    }
    if a + b == 0 {
        return 1.0;
    }
    (2 * inter) as f32 / (a + b) as f32
}

/// Per-class Dice scores in `Tissue::ALL` order.
pub fn dice_all(pred_labels: &Grid, true_labels: &Grid) -> [f32; Tissue::COUNT] {
    let mut out = [0.0f32; Tissue::COUNT];
    for (i, &t) in Tissue::ALL.iter().enumerate() {
        out[i] = dice(pred_labels, true_labels, t);
    }
    out
}

/// Mean Dice across all classes.
pub fn mean_dice(pred_labels: &Grid, true_labels: &Grid) -> f32 {
    let d = dice_all(pred_labels, true_labels);
    d.iter().sum::<f32>() / d.len() as f32
}

/// Mean absolute speed-of-sound error (m/s) between two grids.
pub fn mae_speed(pred: &Grid, truth: &Grid) -> f32 {
    pred.mean_abs_diff(truth).unwrap_or(f32::NAN)
}

/// Root-mean-square error between two equally shaped grids.
pub fn rmse(pred: &Grid, truth: &Grid) -> f32 {
    let n = pred.data.len().min(truth.data.len());
    if n == 0 {
        return 0.0;
    }
    let mut acc = 0.0f64;
    for i in 0..n {
        let d = (pred.data[i] - truth.data[i]) as f64;
        acc += d * d;
    }
    (acc / n as f64).sqrt() as f32
}

/// Peak signal-to-noise ratio (dB), with the peak taken as the dynamic range of
/// `truth`. Higher is better; returns `+inf` for a perfect match.
pub fn psnr(pred: &Grid, truth: &Grid) -> f32 {
    let (lo, hi) = truth.min_max();
    let peak = (hi - lo).max(1e-6);
    let e = rmse(pred, truth);
    if e <= 0.0 {
        return f32::INFINITY;
    }
    20.0 * (peak / e).log10()
}

/// Global Structural Similarity Index (SSIM) in `[-1, 1]` (1 == identical).
///
/// Single-window SSIM over the whole image with the standard stabilising
/// constants `C1 = (0.01 L)²`, `C2 = (0.03 L)²` where `L` is the dynamic range
/// of `truth`.
pub fn ssim(pred: &Grid, truth: &Grid) -> f32 {
    let n = pred.data.len().min(truth.data.len());
    if n == 0 {
        return 1.0;
    }
    let (lo, hi) = truth.min_max();
    let l = (hi - lo).max(1e-6) as f64;
    let c1 = (0.01 * l).powi(2);
    let c2 = (0.03 * l).powi(2);

    let nf = n as f64;
    let (mut mx, mut my) = (0.0f64, 0.0f64);
    for i in 0..n {
        mx += pred.data[i] as f64;
        my += truth.data[i] as f64;
    }
    mx /= nf;
    my /= nf;

    let (mut vx, mut vy, mut cxy) = (0.0f64, 0.0f64, 0.0f64);
    for i in 0..n {
        let dx = pred.data[i] as f64 - mx;
        let dy = truth.data[i] as f64 - my;
        vx += dx * dx;
        vy += dy * dy;
        cxy += dx * dy;
    }
    vx /= nf - 1.0;
    vy /= nf - 1.0;
    cxy /= nf - 1.0;

    let num = (2.0 * mx * my + c1) * (2.0 * cxy + c2);
    let den = (mx * mx + my * my + c1) * (vx + vy + c2);
    (num / den) as f32
}

/// A compact bundle of quality metrics for one reconstruction.
#[derive(Debug, Clone)]
pub struct QualityReport {
    /// Mean absolute speed error (m/s).
    pub mae_speed: f32,
    /// Per-class Dice in `Tissue::ALL` order.
    pub dice: [f32; Tissue::COUNT],
    /// Mean Dice across classes.
    pub mean_dice: f32,
    /// Number of valid measurements used.
    pub measurements: usize,
}
