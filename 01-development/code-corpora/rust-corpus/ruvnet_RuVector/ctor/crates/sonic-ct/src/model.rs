//! Training the segmentation threshold model from labelled reconstructions.
//!
//! The "model" is the set of speed-band boundaries in [`SegModel`]. We fit them
//! by coordinate ascent to maximise mean Dice across a training set of
//! reconstructed/ground-truth pairs. This is a small, fully reproducible
//! optimisation — no external ML framework required — that nonetheless
//! measurably beats the literature-default boundaries on the synthetic data.

use crate::grid::Grid;
use crate::metrics::mean_dice;
use crate::segmentation::{segment, SegModel};

/// A labelled training example.
pub struct TrainExample {
    /// Reconstructed speed map (model input).
    pub recon_speed: Grid,
    /// Ground-truth tissue labels (target).
    pub true_labels: Grid,
}

/// Mean Dice of `model` across all `examples`.
pub fn evaluate(model: &SegModel, examples: &[TrainExample]) -> f32 {
    if examples.is_empty() {
        return 0.0;
    }
    let mut acc = 0.0f32;
    for ex in examples {
        let seg = segment(&ex.recon_speed, model);
        acc += mean_dice(&seg.labels, &ex.true_labels);
    }
    acc / examples.len() as f32
}

/// Fit boundary thresholds by coordinate ascent.
///
/// Starting from `base`, each finite band boundary is perturbed up/down over a
/// shrinking step schedule, keeping any change that improves training-set mean
/// Dice. Boundaries are kept sorted so the band mapping stays valid.
pub fn train(base: &SegModel, examples: &[TrainExample]) -> (SegModel, f32) {
    let mut model = base.clone();
    let mut best = evaluate(&model, examples);

    // Step schedule in m/s, refined over passes.
    let steps = [40.0f32, 20.0, 10.0, 5.0, 2.0];
    let n_finite = model.bands.iter().filter(|(u, _)| u.is_finite()).count();

    for &step in &steps {
        let mut improved = true;
        while improved {
            improved = false;
            for bi in 0..n_finite {
                for &dir in &[-1.0f32, 1.0] {
                    let mut trial = model.clone();
                    trial.bands[bi].0 += dir * step;
                    if !boundaries_sorted(&trial) {
                        continue;
                    }
                    let score = evaluate(&trial, examples);
                    if score > best + 1e-6 {
                        best = score;
                        model = trial;
                        improved = true;
                    }
                }
            }
        }
    }
    (model, best)
}

/// Whether the finite band boundaries are strictly ascending.
fn boundaries_sorted(model: &SegModel) -> bool {
    let mut prev = f32::NEG_INFINITY;
    for &(u, _) in &model.bands {
        if u <= prev {
            return false;
        }
        prev = u;
    }
    true
}
