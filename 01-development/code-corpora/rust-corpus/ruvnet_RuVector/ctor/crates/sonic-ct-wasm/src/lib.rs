//! Raw C-ABI WebAssembly surface for `sonic_ct`.
//!
//! The JS loader drives this in three steps:
//! 1. call [`sct_run`] to compute a scene,
//! 2. read scalar metrics via the `sct_*` getters,
//! 3. read flat data buffers by taking a pointer (`*_ptr`) + length and viewing
//!    the WebAssembly memory directly (no copies, no wasm-bindgen).
//!
//! All state lives in a single module-global; WebAssembly is single-threaded so
//! this is sound. Buffers are owned by the global and stay alive between calls,
//! so the pointers returned to JS remain valid until the next [`sct_run`].

#![allow(clippy::missing_safety_doc)]

use core::ptr::addr_of_mut;

use sonic_ct::memory::check_coherence;
use sonic_ct::organ::detect_organs;
use sonic_ct::pipeline::{run_slice, run_with_model, PipelineConfig};
use sonic_ct::segmentation::SegModel;
use sonic_ct::types::Tissue;
use sonic_ct::volume3d::{VOL_ERROR_SAT, VOL_SPEED_HI, VOL_SPEED_LO};

/// Flattened, JS-readable view of one computed scene.
#[derive(Default)]
struct State {
    n: u32,
    elements: u32,
    measurements: u32,
    mae: f32,
    mean_dice: f32,
    dice: [f32; Tissue::COUNT],
    speed_min: f32,
    speed_max: f32,
    atten_max: f32,
    organ_water: u32,
    bone_water: u32,
    anomaly: u32,

    ring_xy: Vec<f32>,        // [x0,y0, x1,y1, ...] normalised to [-1,1]
    truth_speed: Vec<f32>,    // n*n
    recon_speed: Vec<f32>,    // n*n
    recon_atten: Vec<f32>,    // n*n
    truth_labels: Vec<u8>,    // n*n
    recon_labels: Vec<u8>,    // n*n
    uncertainty: Vec<f32>,    // n*n
}

static mut STATE: Option<State> = None;

#[inline]
fn state() -> &'static mut Option<State> {
    // Safe: WASM is single-threaded, and we only ever hand out one reference at
    // a time across FFI boundaries that do not re-enter.
    unsafe { &mut *addr_of_mut!(STATE) }
}

/// Run the full pipeline. Returns 1 on success, 0 on failure.
///
/// `n` grid resolution, `elements` ring size, `fan` receivers per transmit,
/// `iters` SART sweeps, `seed` phantom seed.
#[no_mangle]
pub extern "C" fn sct_run(n: u32, elements: u32, fan: u32, iters: u32, seed: u32) -> i32 {
    let mut cfg = PipelineConfig::default();
    cfg.phantom.n = n.max(8) as usize;
    cfg.phantom.seed = seed as u64;
    cfg.elements = elements.max(8) as usize;
    cfg.acquisition.fan = fan.max(4) as usize;
    cfg.recon.iters = iters.max(1) as usize;

    let scene = match run_with_model(cfg, &SegModel::tuned()) {
        Ok(s) => s,
        Err(_) => return 0,
    };

    let (s_lo, s_hi) = scene.phantom.speed.min_max();
    let (_, a_hi) = scene.recon_attenuation.min_max();
    let coh = check_coherence(&scene.segmentation.labels);

    // Normalise ring coordinates to the [-1,1] clip square for the UI.
    let half = (cfg.phantom.extent / 2.0).max(1e-6);
    let mut ring_xy = Vec::with_capacity(scene.ring.count() * 2);
    for p in &scene.ring.positions {
        ring_xy.push(p.x / half);
        ring_xy.push(p.y / half);
    }

    let to_u8 = |g: &sonic_ct::Grid| g.data.iter().map(|&v| v as u8).collect::<Vec<u8>>();

    let st = State {
        n: cfg.phantom.n as u32,
        elements: scene.ring.count() as u32,
        measurements: scene.quality.measurements as u32,
        mae: scene.quality.mae_speed,
        mean_dice: scene.quality.mean_dice,
        dice: scene.quality.dice,
        speed_min: s_lo,
        speed_max: s_hi,
        atten_max: a_hi.max(1e-6),
        organ_water: coh.organ_touching_water as u32,
        bone_water: coh.bone_touching_water as u32,
        anomaly: coh.anomaly as u32,
        ring_xy,
        truth_speed: scene.phantom.speed.data.clone(),
        recon_speed: scene.recon_speed.data.clone(),
        recon_atten: scene.recon_attenuation.data.clone(),
        truth_labels: to_u8(&scene.phantom.labels),
        recon_labels: to_u8(&scene.segmentation.labels),
        uncertainty: scene.segmentation.uncertainty.data.clone(),
    };
    *state() = Some(st);
    1
}

macro_rules! getter {
    ($name:ident, $ty:ty, $field:ident, $default:expr) => {
        /// Scalar getter (see field name).
        #[no_mangle]
        pub extern "C" fn $name() -> $ty {
            state().as_ref().map(|s| s.$field).unwrap_or($default)
        }
    };
}

getter!(sct_grid_n, u32, n, 0);
getter!(sct_element_count, u32, elements, 0);
getter!(sct_measurements, u32, measurements, 0);
getter!(sct_mae, f32, mae, 0.0);
getter!(sct_mean_dice, f32, mean_dice, 0.0);
getter!(sct_speed_min, f32, speed_min, 0.0);
getter!(sct_speed_max, f32, speed_max, 0.0);
getter!(sct_atten_max, f32, atten_max, 0.0);
getter!(sct_organ_water, u32, organ_water, 0);
getter!(sct_bone_water, u32, bone_water, 0);
getter!(sct_anomaly, u32, anomaly, 0);

/// Per-class Dice score (`class` in `0..=4`).
#[no_mangle]
pub extern "C" fn sct_dice(class: u32) -> f32 {
    state()
        .as_ref()
        .and_then(|s| s.dice.get(class as usize).copied())
        .unwrap_or(0.0)
}

macro_rules! ptr_getter {
    ($name:ident, $ty:ty, $field:ident) => {
        /// Pointer to a flat data buffer in linear memory.
        #[no_mangle]
        pub extern "C" fn $name() -> *const $ty {
            state()
                .as_ref()
                .map(|s| s.$field.as_ptr())
                .unwrap_or(core::ptr::null())
        }
    };
}

ptr_getter!(sct_ring_xy_ptr, f32, ring_xy);
ptr_getter!(sct_truth_speed_ptr, f32, truth_speed);
ptr_getter!(sct_recon_speed_ptr, f32, recon_speed);
ptr_getter!(sct_recon_atten_ptr, f32, recon_atten);
ptr_getter!(sct_truth_labels_ptr, u8, truth_labels);
ptr_getter!(sct_recon_labels_ptr, u8, recon_labels);
ptr_getter!(sct_uncertainty_ptr, f32, uncertainty);

// ---------------------------------------------------------------------------
// 3-D volume API (progressive: one slice per `sct_vol_step` call)
// ---------------------------------------------------------------------------

#[derive(Default)]
struct VolState {
    n: u32,
    nz: u32,
    cells: usize,
    elements: u32,
    fan: u32,
    iters: u32,
    seed: u32,
    cursor: u32,
    measurements: u32,
    ring_xy: Vec<f32>,
    truth_labels: Vec<u8>,
    recon_labels: Vec<u8>,
    recon_speed_u8: Vec<u8>,
    error_u8: Vec<u8>,
    confidence_u8: Vec<u8>,
    slice_dice: Vec<f32>,
    slice_mae: Vec<f32>,
    class_counts: [u64; Tissue::COUNT],
    body_voxels: u64,
    worst_slice: u32,
    worst_mae: f32,
    organs: Vec<(u8, f32, u32)>, // (organ id, confidence, evidence)
    quality_flags: [u32; 4],     // bone-shadow, sparse-coverage, boundary-uncertainty, gas
}

static mut VOL: Option<VolState> = None;

#[inline]
fn vol() -> &'static mut Option<VolState> {
    unsafe { &mut *addr_of_mut!(VOL) }
}

#[inline]
fn norm_u8(v: f32, lo: f32, hi: f32) -> u8 {
    let t = ((v - lo) / (hi - lo).max(1e-6)).clamp(0.0, 1.0);
    (t * 255.0) as u8
}

/// Begin a progressive volume sweep. Returns 1 on success.
#[no_mangle]
pub extern "C" fn sct_vol_begin(nz: u32, n: u32, elements: u32, fan: u32, iters: u32, seed: u32) -> i32 {
    let n = n.max(8);
    let nz = nz.max(1);
    let cells = (n * n) as usize;
    let total = cells * nz as usize;
    *vol() = Some(VolState {
        n,
        nz,
        cells,
        elements: elements.max(8),
        fan: fan.max(4),
        iters: iters.max(1),
        seed,
        cursor: 0,
        measurements: 0,
        ring_xy: Vec::new(),
        truth_labels: vec![0; total],
        recon_labels: vec![0; total],
        recon_speed_u8: vec![0; total],
        error_u8: vec![0; total],
        confidence_u8: vec![0; total],
        slice_dice: vec![0.0; nz as usize],
        slice_mae: vec![0.0; nz as usize],
        class_counts: [0; Tissue::COUNT],
        body_voxels: 0,
        worst_slice: 0,
        worst_mae: f32::NEG_INFINITY,
        ..Default::default()
    });
    1
}

/// Build & reconstruct the next slice. Returns the number of slices completed.
#[no_mangle]
pub extern "C" fn sct_vol_step() -> i32 {
    let s = match vol().as_mut() {
        Some(s) => s,
        None => return -1,
    };
    if s.cursor >= s.nz {
        return s.cursor as i32;
    }
    let zi = s.cursor;
    let z = if s.nz == 1 { 0.5 } else { zi as f32 / (s.nz - 1) as f32 };

    let mut cfg = PipelineConfig::default();
    cfg.phantom.n = s.n as usize;
    cfg.phantom.seed = s.seed as u64;
    cfg.elements = s.elements as usize;
    cfg.acquisition.fan = s.fan as usize;
    cfg.recon.iters = s.iters as usize;

    let scene = match run_slice(cfg, &SegModel::tuned(), z) {
        Ok(sc) => sc,
        Err(_) => {
            s.cursor += 1;
            return s.cursor as i32;
        }
    };

    if zi == 0 {
        let half = (cfg.phantom.extent / 2.0).max(1e-6);
        s.ring_xy = Vec::with_capacity(scene.ring.count() * 2);
        for p in &scene.ring.positions {
            s.ring_xy.push(p.x / half);
            s.ring_xy.push(p.y / half);
        }
    }

    let base = zi as usize * s.cells;
    for i in 0..s.cells {
        let tl = scene.phantom.labels.data[i] as u8;
        let rl = scene.segmentation.labels.data[i] as u8;
        s.truth_labels[base + i] = tl;
        s.recon_labels[base + i] = rl;
        s.recon_speed_u8[base + i] = norm_u8(scene.recon_speed.data[i], VOL_SPEED_LO, VOL_SPEED_HI);
        let err = (scene.recon_speed.data[i] - scene.phantom.speed.data[i]).abs();
        s.error_u8[base + i] = norm_u8(err, 0.0, VOL_ERROR_SAT);
        s.confidence_u8[base + i] = norm_u8(1.0 - scene.segmentation.uncertainty.data[i], 0.0, 1.0);
        if tl != Tissue::Water as u8 {
            s.body_voxels += 1;
            s.class_counts[tl as usize] += 1;
        }
    }

    s.slice_dice[zi as usize] = scene.quality.mean_dice;
    s.slice_mae[zi as usize] = scene.quality.mae_speed;
    s.measurements += scene.quality.measurements as u32;
    if scene.quality.mae_speed > s.worst_mae {
        s.worst_mae = scene.quality.mae_speed;
        s.worst_slice = zi;
    }
    s.cursor += 1;

    // When the sweep finishes, run organ inference + quality flags once.
    if s.cursor >= s.nz {
        let hyps = detect_organs(&s.recon_labels, s.n as usize, s.nz as usize);
        s.organs = hyps
            .iter()
            .map(|h| (h.organ as u8, h.confidence, h.evidence))
            .collect();

        // Quality flags as severities: 0 = low, 1 = moderate, 2 = high.
        let body = s.body_voxels.max(1) as f32;
        let bone_frac = s.class_counts[Tissue::Bone as usize] as f32 / body;
        let sev = |x: f32, m: f32, h: f32| -> u32 {
            if x >= h { 2 } else if x >= m { 1 } else { 0 }
        };
        // Mean confidence over built voxels → boundary uncertainty.
        let built = (s.cursor as usize) * s.cells;
        let conf_sum: u64 = s.confidence_u8[..built.min(s.confidence_u8.len())]
            .iter()
            .map(|&v| v as u64)
            .sum();
        let mean_conf = if built > 0 { (conf_sum as f32 / built as f32) / 255.0 } else { 0.0 };
        let uncertainty = 1.0 - mean_conf;
        // Path coverage from fan/element ratio.
        let coverage = (s.fan as f32) / (s.elements as f32).max(1.0);
        s.quality_flags = [
            sev(bone_frac, 0.06, 0.12),       // bone shadowing
            sev(1.0 - coverage, 0.4, 0.7),    // sparse path coverage
            sev(uncertainty, 0.45, 0.65),     // boundary uncertainty
            0,                                // gas artifact — not modelled
        ];
    }
    s.cursor as i32
}

macro_rules! vgetter {
    ($name:ident, $ty:ty, $field:ident, $default:expr) => {
        /// Volume scalar getter.
        #[no_mangle]
        pub extern "C" fn $name() -> $ty {
            vol().as_ref().map(|s| s.$field).unwrap_or($default)
        }
    };
}

vgetter!(sct_vol_n, u32, n, 0);
vgetter!(sct_vol_slices, u32, nz, 0);
vgetter!(sct_vol_elements, u32, elements, 0);
vgetter!(sct_vol_cursor, u32, cursor, 0);
vgetter!(sct_vol_measurements, u32, measurements, 0);
vgetter!(sct_vol_worst_slice, u32, worst_slice, 0);

/// Mean Dice across the slices built so far.
#[no_mangle]
pub extern "C" fn sct_vol_mean_dice() -> f32 {
    vol()
        .as_ref()
        .map(|s| {
            let k = s.cursor.max(1) as usize;
            s.slice_dice[..k.min(s.slice_dice.len())].iter().sum::<f32>() / k as f32
        })
        .unwrap_or(0.0)
}

/// Mean confidence (mean over built voxels of 1 − uncertainty), in `[0,1]`.
#[no_mangle]
pub extern "C" fn sct_vol_confidence() -> f32 {
    vol()
        .as_ref()
        .map(|s| {
            let built = (s.cursor as usize) * s.cells;
            if built == 0 {
                return 0.0;
            }
            let sum: u64 = s.confidence_u8[..built].iter().map(|&v| v as u64).sum();
            (sum as f32 / built as f32) / 255.0
        })
        .unwrap_or(0.0)
}

/// Body-composition fraction for `class` (0..=4), over body voxels built so far.
#[no_mangle]
pub extern "C" fn sct_vol_fraction(class: u32) -> f32 {
    vol()
        .as_ref()
        .and_then(|s| {
            if s.body_voxels == 0 {
                return None;
            }
            s.class_counts
                .get(class as usize)
                .map(|&c| c as f32 / s.body_voxels as f32)
        })
        .unwrap_or(0.0)
}

macro_rules! vptr {
    ($name:ident, $ty:ty, $field:ident) => {
        /// Volume buffer pointer.
        #[no_mangle]
        pub extern "C" fn $name() -> *const $ty {
            vol().as_ref().map(|s| s.$field.as_ptr()).unwrap_or(core::ptr::null())
        }
    };
}

vptr!(sct_vol_ring_xy_ptr, f32, ring_xy);
vptr!(sct_vol_truth_labels_ptr, u8, truth_labels);
vptr!(sct_vol_recon_labels_ptr, u8, recon_labels);
vptr!(sct_vol_recon_speed_ptr, u8, recon_speed_u8);
vptr!(sct_vol_error_ptr, u8, error_u8);
vptr!(sct_vol_confidence_ptr, u8, confidence_u8);
vptr!(sct_vol_slice_dice_ptr, f32, slice_dice);
vptr!(sct_vol_slice_mae_ptr, f32, slice_mae);

/// Number of organ hypotheses available (0 until the sweep completes).
#[no_mangle]
pub extern "C" fn sct_organ_count() -> u32 {
    vol().as_ref().map(|s| s.organs.len() as u32).unwrap_or(0)
}

/// Organ id for hypothesis `i` (see `sonic_ct::organ::Organ`).
#[no_mangle]
pub extern "C" fn sct_organ_id(i: u32) -> u32 {
    vol().as_ref().and_then(|s| s.organs.get(i as usize)).map(|o| o.0 as u32).unwrap_or(255)
}

/// Confidence for hypothesis `i`.
#[no_mangle]
pub extern "C" fn sct_organ_conf(i: u32) -> f32 {
    vol().as_ref().and_then(|s| s.organs.get(i as usize)).map(|o| o.1).unwrap_or(0.0)
}

/// Evidence bitmask for hypothesis `i`.
#[no_mangle]
pub extern "C" fn sct_organ_evidence(i: u32) -> u32 {
    vol().as_ref().and_then(|s| s.organs.get(i as usize)).map(|o| o.2).unwrap_or(0)
}

/// Quality-flag severity (0 low / 1 moderate / 2 high) for `flag` 0..=3:
/// 0 bone shadowing, 1 sparse path coverage, 2 boundary uncertainty, 3 gas.
#[no_mangle]
pub extern "C" fn sct_quality_flag(flag: u32) -> u32 {
    vol().as_ref().and_then(|s| s.quality_flags.get(flag as usize).copied()).unwrap_or(0)
}
