//! Organ-identity inference from the reconstructed acoustic volume.
//!
//! The five acoustic classes (water/fat/muscle/organ/bone) cannot, by speed of
//! sound alone, tell a liver from a spleen (ADR-0009). Organ *identity* is
//! therefore inferred as a separate layer using **anatomical priors** — zone
//! (cranio-caudal position), side (left/right), size, posterior adjacency, and
//! consistency across neighbouring slices (ADR-0010). Every hypothesis carries
//! an explicit evidence bitmask and a confidence; nothing is asserted from
//! speed alone.
//!
//! Input is the reconstructed soft-tissue ("organ" class) distribution, never
//! the ground-truth phantom — this is genuine inference over the reconstruction.

use crate::types::Tissue;

/// Organs the detector hypothesises.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
#[repr(u8)]
pub enum Organ {
    /// Liver (right upper abdomen).
    Liver = 0,
    /// Spleen (left upper abdomen).
    Spleen = 1,
    /// Left kidney (posterior, mid abdomen).
    KidneyLeft = 2,
    /// Right kidney (posterior, mid abdomen).
    KidneyRight = 3,
    /// Aorta (central, anterior to spine).
    Aorta = 4,
    /// Heart (central thorax).
    Heart = 5,
    /// Left lung (lateral thorax).
    LungLeft = 6,
    /// Right lung (lateral thorax).
    LungRight = 7,
}

impl Organ {
    /// All organs in id order.
    pub const ALL: [Organ; 8] = [
        Organ::Liver,
        Organ::Spleen,
        Organ::KidneyLeft,
        Organ::KidneyRight,
        Organ::Aorta,
        Organ::Heart,
        Organ::LungLeft,
        Organ::LungRight,
    ];

    /// Human-readable name.
    pub fn name(self) -> &'static str {
        match self {
            Organ::Liver => "liver",
            Organ::Spleen => "spleen",
            Organ::KidneyLeft => "left kidney",
            Organ::KidneyRight => "right kidney",
            Organ::Aorta => "aorta",
            Organ::Heart => "heart",
            Organ::LungLeft => "left lung",
            Organ::LungRight => "right lung",
        }
    }
}

/// Evidence bit: candidate lies in the expected cranio-caudal zone.
pub const EV_ZONE: u32 = 1;
/// Evidence bit: candidate is on the expected side (left/right/central).
pub const EV_SIDE: u32 = 2;
/// Evidence bit: candidate has a plausible size.
pub const EV_SIZE: u32 = 4;
/// Evidence bit: candidate is posterior (kidneys/aorta) as expected.
pub const EV_ADJACENCY: u32 = 8;
/// Evidence bit: candidate is consistent across neighbouring slices.
pub const EV_CONSISTENCY: u32 = 16;

/// One organ hypothesis.
#[derive(Debug, Clone, Copy)]
pub struct OrganHypothesis {
    /// Which organ.
    pub organ: Organ,
    /// Confidence in `[0, 1]`.
    pub confidence: f32,
    /// Evidence bitmask (`EV_*`).
    pub evidence: u32,
    /// Fraction of body voxels assigned to this candidate region.
    pub volume_frac: f32,
}

/// Spatial prior for one organ.
struct Spec {
    organ: Organ,
    z: (f32, f32),       // cranio-caudal range [0,1]
    side: f32,           // -1 left, +1 right, 0 central
    posterior: bool,     // expected toward the back (y<0)
    expected_frac: f32,  // expected fraction of region cells that are organ
}

const SPECS: [Spec; 8] = [
    Spec { organ: Organ::Liver, z: (0.58, 0.86), side: 1.0, posterior: false, expected_frac: 0.18 },
    Spec { organ: Organ::Spleen, z: (0.58, 0.82), side: -1.0, posterior: false, expected_frac: 0.06 },
    Spec { organ: Organ::KidneyLeft, z: (0.33, 0.66), side: -1.0, posterior: true, expected_frac: 0.05 },
    Spec { organ: Organ::KidneyRight, z: (0.33, 0.66), side: 1.0, posterior: true, expected_frac: 0.05 },
    Spec { organ: Organ::Aorta, z: (0.2, 0.9), side: 0.0, posterior: true, expected_frac: 0.02 },
    Spec { organ: Organ::Heart, z: (0.8, 1.0), side: -0.3, posterior: false, expected_frac: 0.08 },
    Spec { organ: Organ::LungLeft, z: (0.78, 1.0), side: -1.0, posterior: false, expected_frac: 0.1 },
    Spec { organ: Organ::LungRight, z: (0.78, 1.0), side: 1.0, posterior: false, expected_frac: 0.1 },
];

#[inline]
fn gaussian(x: f32, mu: f32, sigma: f32) -> f32 {
    let d = (x - mu) / sigma;
    (-0.5 * d * d).exp()
}

/// Detect organ hypotheses from reconstructed labels (`x + y*n + z*n*n`).
///
/// Returns one hypothesis per organ, ordered by [`Organ::ALL`].
pub fn detect_organs(labels: &[u8], n: usize, nz: usize) -> Vec<OrganHypothesis> {
    let organ_class = Tissue::Organ as u8;
    let cells = n * n;
    let mut out = Vec::with_capacity(SPECS.len());

    // Total organ-class voxels (for normalisation).
    let total_organ = labels.iter().filter(|&&v| v == organ_class).count().max(1);

    for spec in &SPECS {
        let z0 = (spec.z.0 * (nz as f32 - 1.0)).floor() as usize;
        let z1 = (spec.z.1 * (nz as f32 - 1.0)).ceil() as usize;
        let z1 = z1.min(nz.saturating_sub(1));

        let mut region = 0usize; // organ voxels matching zone+side+posterior
        let mut region_cells = 0usize;
        let mut slices_present = 0usize;
        let mut slices_total = 0usize;

        for z in z0..=z1 {
            slices_total += 1;
            let base = z * cells;
            let mut here = 0usize;
            for y in 0..n {
                let yn = (y as f32 + 0.5) / n as f32 * 2.0 - 1.0;
                if spec.posterior && yn > 0.15 {
                    continue; // require posterior placement
                }
                for x in 0..n {
                    let xn = (x as f32 + 0.5) / n as f32 * 2.0 - 1.0;
                    // Side gating.
                    let side_ok = if spec.side > 0.3 {
                        xn > 0.05
                    } else if spec.side < -0.3 {
                        xn < -0.05
                    } else {
                        xn.abs() < 0.18
                    };
                    if !side_ok {
                        continue;
                    }
                    region_cells += 1;
                    if labels[base + y * n + x] == organ_class {
                        region += 1;
                        here += 1;
                    }
                }
            }
            if here > (region_cells / slices_total.max(1)) / 20 + 1 {
                slices_present += 1;
            }
        }

        let coverage = if region_cells > 0 { region as f32 / region_cells as f32 } else { 0.0 };
        let consistency = if slices_total > 0 { slices_present as f32 / slices_total as f32 } else { 0.0 };
        let volume_frac = region as f32 / total_organ as f32;

        // Sub-scores.
        let zone_score = if region > 0 { 1.0 } else { 0.0 };
        let side_score = zone_score; // side already gated; presence implies side
        let size_score = gaussian(coverage, spec.expected_frac, spec.expected_frac.max(0.04));
        let adj_score = if spec.posterior { (region > 0) as i32 as f32 } else { 1.0 };

        // Evidence flags.
        let mut evidence = 0u32;
        if region > 0 {
            evidence |= EV_ZONE | EV_SIDE;
        }
        if size_score > 0.4 {
            evidence |= EV_SIZE;
        }
        // Adjacency holds for anterior organs unconditionally, and for posterior
        // organs only when tissue is actually found in the posterior region.
        if !spec.posterior || region > 0 {
            evidence |= EV_ADJACENCY;
        }
        if consistency > 0.5 {
            evidence |= EV_CONSISTENCY;
        }

        // Weighted confidence, only meaningful if the region has tissue.
        let raw = 0.32 * zone_score
            + 0.22 * side_score
            + 0.2 * size_score
            + 0.12 * adj_score
            + 0.14 * consistency;
        let confidence = if region == 0 { 0.0 } else { (0.45 + 0.5 * raw).clamp(0.0, 0.97) };

        out.push(OrganHypothesis { organ: spec.organ, confidence, evidence, volume_frac });
    }
    out
}
