//! # sonic_ct
//!
//! A research-grade **Ultrasound Computed Tomography (USCT)** simulator and
//! reconstruction toolkit.
//!
//! `sonic_ct` models a Midjourney-style ring scanner: a subject is coupled in a
//! water bath and surrounded by a dense ring of ultrasound transducers that
//! transmit and receive through tissue from many angles. From the simulated
//! travel-time and attenuation measurements it reconstructs maps of acoustic
//! properties, segments them into tissue classes, and scores the result against
//! ground truth.
//!
//! The crate is deliberately dependency-free so it builds natively, as a CLI,
//! and to `wasm32-unknown-unknown` for in-browser use.
//!
//! ## Pipeline
//!
//! ```text
//! phantom ─▶ ring ─▶ acquisition ─▶ SART reconstruction ─▶ segmentation ─▶ metrics
//!                                            │                                  │
//!                                            └──────── acoustic memory ◀────────┘
//! ```
//!
//! ## Quick start
//!
//! ```
//! use sonic_ct::pipeline::{run, PipelineConfig};
//! let scene = run(PipelineConfig::default()).expect("pipeline runs");
//! assert!(scene.quality.measurements > 0);
//! assert!(scene.quality.mae_speed.is_finite());
//! ```
//!
//! ## Scope & safety
//!
//! This is research/simulation code. It makes **no diagnostic claim** and the
//! Butterfly Embedded boundary is a mock adapter, not a hardware SDK.

#![forbid(unsafe_code)]
#![warn(missing_docs)]

pub mod acquisition;
pub mod butterfly;
pub mod fwi;
pub mod geometry;
pub mod grid;
pub mod memory;
pub mod metrics;
pub mod model;
pub mod organ;
pub mod phantom;
pub mod pipeline;
pub mod ray;
pub mod reconstruction;
pub mod segmentation;
pub mod shepp_logan;
pub mod types;
pub mod volume3d;

pub use grid::Grid;
pub use pipeline::{run, run_with_model, PipelineConfig, Scene};
pub use types::{Point, Result, SonicError, Tissue};

#[cfg(test)]
mod tests {
    use super::*;
    use crate::phantom::{Phantom, PhantomConfig};

    fn small_cfg() -> PipelineConfig {
        let mut cfg = PipelineConfig::default();
        cfg.phantom.n = 48;
        cfg.elements = 96;
        cfg.acquisition.fan = 48;
        cfg.recon.iters = 4;
        cfg
    }

    #[test]
    fn pipeline_produces_valid_measurements() {
        let scene = run(small_cfg()).unwrap();
        assert!(scene.quality.measurements > 0, "should have measurements");
        assert_eq!(scene.recon_speed.nx, scene.phantom.speed.nx);
    }

    #[test]
    fn reconstruction_beats_water_prior() {
        // The reconstruction must be closer to truth than a flat water guess.
        let scene = run(small_cfg()).unwrap();
        let mut flat = scene.phantom.speed.clone();
        flat.data.iter_mut().for_each(|v| *v = types::WATER_SPEED);
        let flat_mae = metrics::mae_speed(&flat, &scene.phantom.speed);
        assert!(
            scene.quality.mae_speed < flat_mae,
            "recon MAE {} should beat flat-water MAE {}",
            scene.quality.mae_speed,
            flat_mae
        );
    }

    #[test]
    fn more_iters_do_not_worsen_mae() {
        let mut a = small_cfg();
        a.recon.iters = 1;
        let mut b = small_cfg();
        b.recon.iters = 8;
        let mae1 = run(a).unwrap().quality.mae_speed;
        let mae8 = run(b).unwrap().quality.mae_speed;
        assert!(mae8 <= mae1 * 1.05, "more iters should not regress much");
    }

    #[test]
    fn dice_scores_in_unit_range() {
        let scene = run(small_cfg()).unwrap();
        for &d in &scene.quality.dice {
            assert!((0.0..=1.0).contains(&d), "dice out of range: {d}");
        }
    }

    #[test]
    fn invalid_config_is_rejected() {
        let mut cfg = small_cfg();
        cfg.elements = 2;
        assert!(run(cfg).is_err());
    }

    #[test]
    fn phantom_is_deterministic() {
        let c = PhantomConfig::default();
        let a = Phantom::build(c);
        let b = Phantom::build(c);
        assert_eq!(a.speed.data, b.speed.data);
    }
}
