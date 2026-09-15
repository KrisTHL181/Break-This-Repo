//! End-to-end simulation + reconstruction pipeline.

use crate::acquisition::{simulate, Acquisition, AcquisitionConfig};
use crate::geometry::Ring;
use crate::grid::Grid;
use crate::metrics::{dice_all, mae_speed, QualityReport};
use crate::phantom::{Phantom, PhantomConfig};
use crate::reconstruction::{reconstruct_attenuation, reconstruct_speed, ReconConfig};
use crate::segmentation::{segment, SegModel, Segmentation};
use crate::types::Result;

/// Full pipeline configuration.
#[derive(Debug, Clone, Copy)]
pub struct PipelineConfig {
    /// Phantom synthesis parameters.
    pub phantom: PhantomConfig,
    /// Number of ring transducer elements.
    pub elements: usize,
    /// Ring radius as a fraction of half the field of view (0,1).
    pub ring_frac: f32,
    /// Acquisition parameters.
    pub acquisition: AcquisitionConfig,
    /// Reconstruction parameters.
    pub recon: ReconConfig,
}

impl Default for PipelineConfig {
    fn default() -> Self {
        PipelineConfig {
            phantom: PhantomConfig::default(),
            elements: 180,
            ring_frac: 0.92,
            acquisition: AcquisitionConfig::default(),
            recon: ReconConfig::default(),
        }
    }
}

impl PipelineConfig {
    /// Validate ranges, returning a descriptive error.
    pub fn validate(&self) -> Result<()> {
        use crate::types::SonicError::InvalidConfig;
        if self.phantom.n < 8 {
            return Err(InvalidConfig("phantom.n must be >= 8"));
        }
        if self.elements < 8 {
            return Err(InvalidConfig("elements must be >= 8"));
        }
        if !(0.1..=0.999).contains(&self.ring_frac) {
            return Err(InvalidConfig("ring_frac must be in (0.1, 0.999)"));
        }
        Ok(())
    }
}

/// All artifacts produced by one pipeline run.
#[derive(Debug, Clone)]
pub struct Scene {
    /// Ground-truth phantom.
    pub phantom: Phantom,
    /// Transducer ring.
    pub ring: Ring,
    /// Simulated measurements.
    pub acquisition: Acquisition,
    /// Reconstructed speed-of-sound map (m/s).
    pub recon_speed: Grid,
    /// Reconstructed attenuation map (Np/m).
    pub recon_attenuation: Grid,
    /// Tissue segmentation of the reconstruction.
    pub segmentation: Segmentation,
    /// Quality metrics versus ground truth.
    pub quality: QualityReport,
}

/// Run the full pipeline with the default segmentation model.
pub fn run(cfg: PipelineConfig) -> Result<Scene> {
    run_with_model(cfg, &SegModel::default())
}

/// Run the full pipeline using a specific segmentation `model`.
pub fn run_with_model(cfg: PipelineConfig, model: &SegModel) -> Result<Scene> {
    run_slice(cfg, model, 0.5)
}

/// Run the full pipeline for a single cranio-caudal slice at height `z ∈ [0,1]`.
///
/// `z = 0.5` is the canonical mid-abdomen slice; sweeping `z` builds a 3-D body
/// (see [`crate::volume3d`]).
pub fn run_slice(cfg: PipelineConfig, model: &SegModel, z: f32) -> Result<Scene> {
    let phantom = Phantom::build_slice(cfg.phantom, z);
    run_with_phantom(cfg, model, phantom)
}

/// Run the pipeline against a *supplied* phantom (e.g. one derived from a real
/// anatomical image), rather than a procedurally generated one. The acoustic
/// engine and reconstruction are identical — only the ground truth differs.
pub fn run_with_phantom(cfg: PipelineConfig, model: &SegModel, phantom: Phantom) -> Result<Scene> {
    cfg.validate()?;

    let half_fov = cfg.phantom.extent / 2.0;
    let ring = Ring::new(cfg.elements, half_fov * cfg.ring_frac);

    let acquisition = simulate(&phantom, &ring, cfg.acquisition);
    if acquisition.valid_count == 0 {
        return Err(crate::types::SonicError::NoMeasurements);
    }

    let recon_speed = reconstruct_speed(&acquisition, &phantom.speed, cfg.recon);
    let recon_attenuation = reconstruct_attenuation(&acquisition, &phantom.attenuation, cfg.recon);
    let segmentation = segment(&recon_speed, model);

    let dice = dice_all(&segmentation.labels, &phantom.labels);
    let mean_dice = dice.iter().sum::<f32>() / dice.len() as f32;
    let quality = QualityReport {
        mae_speed: mae_speed(&recon_speed, &phantom.speed),
        dice,
        mean_dice,
        measurements: acquisition.valid_count,
    };

    Ok(Scene {
        phantom,
        ring,
        acquisition,
        recon_speed,
        recon_attenuation,
        segmentation,
        quality,
    })
}
