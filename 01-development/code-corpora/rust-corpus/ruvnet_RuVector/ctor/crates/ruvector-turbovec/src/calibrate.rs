//! TQ+ per-coordinate calibration (ADR-254 §T3).
//!
//! After the randomized Hadamard rotation, coordinate `i` of a *unit* vector is
//! approximately `N(0, 1/d)` — but finite-`d` rotations leave a small residual
//! mean/variance drift per coordinate. We fit two scalars per coordinate from
//! the first ingest batch — `shift[i]` (empirical mean) and `scale[i]`
//! (empirical std) — that map each coordinate onto the canonical `N(0, 1)` the
//! Lloyd–Max table ([`crate::quantize`]) is built for:
//!
//! ```text
//!   z_i = (r_i - shift_i) / scale_i        // calibrate  → ~N(0,1)
//!   r̂_i = centroid[q_i] * scale_i + shift_i // reconstruct
//! ```
//!
//! The calibration is **frozen** after warm-up: every later vector reuses it
//! with no retraining (online ingest). This is the "+" that lifts plain scalar
//! quantization to TQ+ recall.

/// Frozen per-coordinate affine calibration.
#[derive(Clone, Debug, serde::Serialize, serde::Deserialize)]
pub struct Calibration {
    shift: Vec<f32>,
    scale: Vec<f32>,
}

impl Calibration {
    /// Fit from a batch of **rotated unit** vectors (rows of length `dim`).
    ///
    /// `scale` is floored at a small epsilon so dead/near-constant coordinates
    /// never divide by zero. With fewer than two samples we fall back to the
    /// theoretical `N(0, 1/d)` std (`1/√d`) and zero mean, which keeps the
    /// estimator sane on tiny indexes.
    pub fn fit(rotated_unit_rows: &[Vec<f32>], dim: usize) -> Self {
        debug_assert!(
            rotated_unit_rows.iter().all(|r| r.len() == dim),
            "Calibration::fit: every row must have length dim ({dim})"
        );
        let n = rotated_unit_rows.len();
        if n < 2 {
            let s = 1.0f32 / (dim as f32).sqrt();
            return Self {
                shift: vec![0.0; dim],
                scale: vec![s.max(1e-6); dim],
            };
        }
        let mut shift = vec![0.0f32; dim];
        for row in rotated_unit_rows {
            for (acc, &v) in shift.iter_mut().zip(row.iter()) {
                *acc += v;
            }
        }
        let inv_n = 1.0 / n as f32;
        for s in shift.iter_mut() {
            *s *= inv_n;
        }
        let mut var = vec![0.0f32; dim];
        for row in rotated_unit_rows {
            for ((acc, &v), &m) in var.iter_mut().zip(row.iter()).zip(shift.iter()) {
                let d = v - m;
                *acc += d * d;
            }
        }
        let inv_nm1 = 1.0 / (n - 1) as f32;
        let scale: Vec<f32> = var
            .iter()
            .map(|&v| (v * inv_nm1).sqrt().max(1e-6))
            .collect();
        Self { shift, scale }
    }

    /// Map a rotated coordinate onto the canonical `N(0,1)` grid.
    #[inline]
    pub fn calibrate(&self, i: usize, r: f32) -> f32 {
        (r - self.shift[i]) / self.scale[i]
    }

    /// Reconstruct the rotated coordinate from a dequantized centroid value.
    #[inline]
    pub fn reconstruct(&self, i: usize, centroid: f32) -> f32 {
        centroid * self.scale[i] + self.shift[i]
    }

    /// Dimensionality.
    #[inline]
    pub fn dim(&self) -> usize {
        self.shift.len()
    }

    /// Bytes of stored calibration (two `f32` per coordinate).
    #[inline]
    pub fn bytes(&self) -> usize {
        (self.shift.len() + self.scale.len()) * 4
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn fit_recovers_mean_and_std() {
        // Two coordinates: col0 centered at 10 (std ~0), col1 spread.
        let rows = vec![
            vec![10.0, -1.0],
            vec![10.0, 1.0],
            vec![10.0, -2.0],
            vec![10.0, 2.0],
        ];
        let cal = Calibration::fit(&rows, 2);
        assert!((cal.shift[0] - 10.0).abs() < 1e-4);
        assert!(cal.scale[0] >= 1e-6); // floored, never zero
        assert!(cal.shift[1].abs() < 1e-4);
        assert!(cal.scale[1] > 1.0);
        // calibrate∘reconstruct identity on the grid value.
        let z = cal.calibrate(1, 2.0);
        let r = cal.reconstruct(1, z);
        assert!((r - 2.0).abs() < 1e-4);
    }

    #[test]
    fn tiny_index_falls_back_to_theoretical() {
        let cal = Calibration::fit(&[vec![0.0; 16]], 16);
        let expected = 1.0 / (16f32).sqrt();
        assert!((cal.scale[0] - expected).abs() < 1e-6);
    }
}
