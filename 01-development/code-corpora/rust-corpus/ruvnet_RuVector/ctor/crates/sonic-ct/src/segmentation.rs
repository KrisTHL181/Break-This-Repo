//! Speed-of-sound to tissue-label segmentation with per-cell uncertainty.
//!
//! Segmentation is intentionally a transparent, auditable threshold model
//! rather than an opaque network (ADR-0007): every label is explained by a
//! speed band, and every label carries an uncertainty derived from its margin
//! to the nearest decision boundary.

use crate::grid::Grid;
use crate::types::Tissue;

/// An ordered piecewise speed-band classifier.
///
/// `bands` is sorted ascending by `upper`; a speed `c` is assigned the class of
/// the first band whose `upper` bound it falls under. The final band should use
/// `f32::INFINITY` so the mapping is total.
#[derive(Debug, Clone, PartialEq)]
pub struct SegModel {
    /// `(inclusive upper speed bound, class)` breakpoints, ascending.
    pub bands: Vec<(f32, Tissue)>,
    /// Soft-boundary scale (m/s) for uncertainty estimation.
    pub margin_scale: f32,
}

impl Default for SegModel {
    /// Literature-derived default boundaries (mid-points between tissue speeds).
    fn default() -> Self {
        SegModel {
            bands: vec![
                (1465.0, Tissue::Fat),
                (1500.0, Tissue::Water),
                (1575.0, Tissue::Organ),
                (2000.0, Tissue::Muscle),
                (f32::INFINITY, Tissue::Bone),
            ],
            margin_scale: 30.0,
        }
    }
}

impl SegModel {
    /// Pre-fitted boundaries from `sonic_ct_train` on the synthetic corpus.
    ///
    /// These were produced by coordinate-ascent training (see [`crate::model`])
    /// and roughly double mean Dice versus [`SegModel::default`] on the
    /// reconstructed (blurred) speed maps. Used as the default for the live
    /// WASM demo so it reflects the trained model out of the box.
    pub fn tuned() -> Self {
        SegModel {
            bands: vec![
                (1479.0, Tissue::Fat),
                (1545.0, Tissue::Water),
                (1598.0, Tissue::Organ),
                (1742.0, Tissue::Muscle),
                (f32::INFINITY, Tissue::Bone),
            ],
            margin_scale: 30.0,
        }
    }

    /// Classify a single speed value.
    #[inline]
    pub fn classify(&self, speed: f32) -> Tissue {
        for &(upper, t) in &self.bands {
            if speed <= upper {
                return t;
            }
        }
        self.bands.last().map(|&(_, t)| t).unwrap_or(Tissue::Water)
    }

    /// Distance (m/s) from `speed` to the nearest finite band boundary.
    fn boundary_margin(&self, speed: f32) -> f32 {
        let mut best = f32::INFINITY;
        for &(upper, _) in &self.bands {
            if upper.is_finite() {
                best = best.min((speed - upper).abs());
            }
        }
        best
    }
}

/// Result of segmenting a reconstructed speed map.
#[derive(Debug, Clone)]
pub struct Segmentation {
    /// Per-cell tissue labels (stored as `f32` of the `u8` value).
    pub labels: Grid,
    /// Per-cell uncertainty in `[0, 1]` (1 == on a decision boundary).
    pub uncertainty: Grid,
}

/// Segment a reconstructed `speed` grid with `model`.
pub fn segment(speed: &Grid, model: &SegModel) -> Segmentation {
    let mut labels = speed.clone();
    let mut uncertainty = speed.clone();
    for i in 0..speed.data.len() {
        let c = speed.data[i];
        let t = model.classify(c);
        labels.data[i] = t as u8 as f32;
        let margin = model.boundary_margin(c);
        // Closer to a boundary => higher uncertainty.
        uncertainty.data[i] = (-margin / model.margin_scale.max(1e-3)).exp();
    }
    Segmentation { labels, uncertainty }
}
