//! Full-waveform inversion (FWI) — the state-of-the-art step beyond straight-ray
//! time-of-flight reconstruction (ADR-0004 roadmap).
//!
//! We model the 2-D scalar acoustic wave equation `∂ₜ²p = κ ∇²p + f` (with
//! `κ = c²`) by explicit finite differences, then recover `κ` from recorded
//! pressure traces by minimising the L2 data misfit using the **adjoint-state
//! gradient**: forward-propagate the source, back-propagate the receiver
//! residual, and correlate the two wavefields.
//!
//! This is a transparent, dependency-free reference implementation: small grids,
//! single frequency band, L2 misfit. Frequency continuation, source encoding,
//! and 3-D are the documented next steps. Its correctness is proven by an
//! adjoint-vs-finite-difference gradient check (see the tests).

// Time-stepping loops use the step index for the wavelet + stored history, so
// `needless_range_loop` is a false positive throughout this module.
#![allow(clippy::needless_range_loop)]

use crate::geometry::Ring;
use crate::grid::Grid;
use crate::types::Point;

/// FWI / forward-modelling configuration.
#[derive(Debug, Clone, Copy)]
pub struct FwiConfig {
    /// Grid resolution (cells per side).
    pub n: usize,
    /// Physical field of view (m).
    pub extent: f32,
    /// Number of time steps.
    pub nt: usize,
    /// Ricker wavelet centre frequency (Hz).
    pub freq: f32,
    /// Time step (s); if `None`, a CFL-stable value is chosen.
    pub dt: Option<f32>,
}

impl Default for FwiConfig {
    fn default() -> Self {
        FwiConfig { n: 40, extent: 0.12, nt: 360, freq: 80_000.0, dt: None }
    }
}

/// Source/receiver geometry as flat grid-cell indices.
#[derive(Debug, Clone)]
pub struct Geometry {
    /// Source cell indices.
    pub sources: Vec<usize>,
    /// Receiver cell indices.
    pub receivers: Vec<usize>,
}

impl Geometry {
    /// Place `n_src` sources and `n_rec` receivers on a ring of the given grid.
    pub fn ring(cfg: &FwiConfig, n_src: usize, n_rec: usize) -> Geometry {
        let grid = Grid::square(cfg.n, cfg.extent, 0.0);
        let half = cfg.extent / 2.0;
        let cell = |ring: &Ring, i: usize| -> usize {
            let p = ring.positions[i];
            // Clamp onto the grid interior.
            let (x, y) = grid.point_to_cell(Point::new(p.x.clamp(-half * 0.95, half * 0.95), p.y.clamp(-half * 0.95, half * 0.95))).unwrap_or((cfg.n / 2, cfg.n / 2));
            grid.idx(x, y)
        };
        let rs = Ring::new(n_src, half * 0.9);
        let rr = Ring::new(n_rec, half * 0.9);
        Geometry {
            sources: (0..n_src).map(|i| cell(&rs, i)).collect(),
            receivers: (0..n_rec).map(|i| cell(&rr, i)).collect(),
        }
    }
}

/// A Ricker wavelet sample at time `t` for centre frequency `f`.
#[inline]
pub fn ricker(t: f32, f: f32) -> f32 {
    let t0 = 1.0 / f; // delay so the wavelet is causal
    let a = std::f32::consts::PI * f * (t - t0);
    let a2 = a * a;
    (1.0 - 2.0 * a2) * (-a2).exp()
}

/// CFL-stable time step for a given max speed.
fn stable_dt(dx: f32, c_max: f32) -> f32 {
    0.45 * dx / (c_max * std::f32::consts::SQRT_2)
}

/// Forward-propagate one source through `kappa = c²`, returning the recorded
/// receiver traces `[nrec][nt]` and, if `store`, the full pressure history
/// `[nt][ncells]` (needed for the adjoint gradient).
fn forward(
    kappa: &[f32],
    cfg: &FwiConfig,
    dx: f32,
    dt: f32,
    geom: &Geometry,
    src: usize,
    store: bool,
) -> (Vec<Vec<f32>>, Option<Vec<Vec<f32>>>) {
    let n = cfg.n;
    let nc = n * n;
    let mut p_prev = vec![0.0f32; nc];
    let mut p = vec![0.0f32; nc];
    let mut p_next = vec![0.0f32; nc];
    let inv_dx2 = 1.0 / (dx * dx);
    let dt2 = dt * dt;

    let mut rec = vec![vec![0.0f32; cfg.nt]; geom.receivers.len()];
    let mut hist: Option<Vec<Vec<f32>>> = if store { Some(Vec::with_capacity(cfg.nt)) } else { None };

    for it in 0..cfg.nt {
        // Interior Laplacian + leapfrog update.
        for y in 1..n - 1 {
            for x in 1..n - 1 {
                let i = y * n + x;
                let lap = (p[i + 1] + p[i - 1] + p[i + n] + p[i - n] - 4.0 * p[i]) * inv_dx2;
                p_next[i] = 2.0 * p[i] - p_prev[i] + dt2 * kappa[i] * lap;
            }
        }
        // Inject the source.
        let s = ricker(it as f32 * dt, cfg.freq);
        p_next[geom.sources[src]] += dt2 * kappa[geom.sources[src]] * s;

        // Simple damping sponge to limit edge reflections.
        sponge(&mut p_next, n);

        // Record + store.
        for (r, &rc) in geom.receivers.iter().enumerate() {
            rec[r][it] = p_next[rc];
        }
        if let Some(h) = hist.as_mut() {
            h.push(p.clone()); // store p at step it (aligned with Laplacian use)
        }

        std::mem::swap(&mut p_prev, &mut p);
        std::mem::swap(&mut p, &mut p_next);
    }
    (rec, hist)
}

/// Exponential damping in a boundary layer (cheap absorbing boundary).
fn sponge(p: &mut [f32], n: usize) {
    let w = (n / 8).max(2);
    for y in 0..n {
        for x in 0..n {
            let d = x.min(y).min(n - 1 - x).min(n - 1 - y);
            if d < w {
                let f = 0.92 + 0.08 * (d as f32 / w as f32);
                p[y * n + x] *= f;
            }
        }
    }
}

/// Data misfit `½ Σ ‖p_rec − observed‖²` over all sources.
pub fn misfit(kappa: &[f32], cfg: &FwiConfig, dx: f32, dt: f32, geom: &Geometry, observed: &[Vec<Vec<f32>>]) -> f32 {
    let mut chi = 0.0f64;
    for (s, _) in geom.sources.iter().enumerate() {
        let (rec, _) = forward(kappa, cfg, dx, dt, geom, s, false);
        for r in 0..geom.receivers.len() {
            for it in 0..cfg.nt {
                let d = (rec[r][it] - observed[s][r][it]) as f64;
                chi += 0.5 * d * d;
            }
        }
    }
    chi as f32
}

/// Adjoint-state gradient of the misfit w.r.t. `kappa`, plus the misfit value.
///
/// For `∂ₜ²p = κ ∇²p + f`, the operator is self-adjoint; the adjoint field `λ`
/// solves the same equation backward in time with the receiver residual as its
/// source, and `∂χ/∂κ(x) = Σ_t λ(x,t) ∇²p(x,t)`.
pub fn gradient(kappa: &[f32], cfg: &FwiConfig, dx: f32, dt: f32, geom: &Geometry, observed: &[Vec<Vec<f32>>]) -> (Vec<f32>, f32) {
    let n = cfg.n;
    let nc = n * n;
    let inv_dx2 = 1.0 / (dx * dx);
    let dt2 = dt * dt;
    let mut grad = vec![0.0f32; nc];
    let mut chi = 0.0f64;

    for (s, _) in geom.sources.iter().enumerate() {
        let (rec, hist) = forward(kappa, cfg, dx, dt, geom, s, true);
        let hist = hist.unwrap();

        // Residual at receivers.
        let mut resid = vec![vec![0.0f32; cfg.nt]; geom.receivers.len()];
        for r in 0..geom.receivers.len() {
            for it in 0..cfg.nt {
                let d = rec[r][it] - observed[s][r][it];
                resid[r][it] = d;
                chi += 0.5 * (d as f64) * (d as f64);
            }
        }

        // Adjoint propagation backward in time.
        let mut a_prev = vec![0.0f32; nc];
        let mut a = vec![0.0f32; nc];
        let mut a_next = vec![0.0f32; nc];
        for it in (0..cfg.nt).rev() {
            for y in 1..n - 1 {
                for x in 1..n - 1 {
                    let i = y * n + x;
                    let lap = (a[i + 1] + a[i - 1] + a[i + n] + a[i - n] - 4.0 * a[i]) * inv_dx2;
                    a_next[i] = 2.0 * a[i] - a_prev[i] + dt2 * kappa[i] * lap;
                }
            }
            // Inject receiver residual as the adjoint source.
            for (r, &rc) in geom.receivers.iter().enumerate() {
                a_next[rc] += dt2 * kappa[rc] * resid[r][it];
            }
            sponge(&mut a_next, n);

            // Correlate: grad[x] += λ(x,it) * ∇²p(x,it).
            let p = &hist[it];
            for y in 1..n - 1 {
                for x in 1..n - 1 {
                    let i = y * n + x;
                    let lap_p = (p[i + 1] + p[i - 1] + p[i + n] + p[i - n] - 4.0 * p[i]) * inv_dx2;
                    grad[i] += a_next[i] * lap_p;
                }
            }

            std::mem::swap(&mut a_prev, &mut a);
            std::mem::swap(&mut a, &mut a_next);
        }
    }
    (grad, chi as f32)
}

/// Result of an FWI run.
#[derive(Debug, Clone)]
pub struct FwiResult {
    /// Recovered speed-of-sound map (m/s).
    pub speed: Grid,
    /// Data-misfit value per iteration (including the start).
    pub misfit_history: Vec<f32>,
}

/// Invert for speed of sound from `observed` traces, starting from `init_speed`,
/// by gradient descent on `κ = c²` with a normalised, backtracked step.
pub fn invert(init_speed: &Grid, cfg: &FwiConfig, geom: &Geometry, observed: &[Vec<Vec<f32>>], iters: usize) -> FwiResult {
    let dx = cfg.extent / cfg.n as f32;
    let c_max = init_speed.data.iter().cloned().fold(0.0f32, f32::max).max(1.0);
    let dt = cfg.dt.unwrap_or_else(|| stable_dt(dx, c_max * 1.3));

    let mut kappa: Vec<f32> = init_speed.data.iter().map(|&c| c * c).collect();
    let mut history = Vec::with_capacity(iters + 1);

    for _ in 0..iters {
        let (mut grad, chi) = gradient(&kappa, cfg, dx, dt, geom, observed);
        history.push(chi);
        // Standard gradient conditioning: mute the source/receiver footprints
        // (they dominate the raw gradient) and smooth to suppress high-frequency
        // artifacts before stepping.
        mute_around(&mut grad, cfg.n, &geom.sources, 2);
        mute_around(&mut grad, cfg.n, &geom.receivers, 2);
        box_smooth(&mut grad, cfg.n, 2);
        // Normalise the gradient and take a backtracking step.
        let gmax = grad.iter().cloned().fold(0.0f32, |a, b| a.max(b.abs())).max(1e-20);
        let kmean = kappa.iter().sum::<f32>() / kappa.len() as f32;
        let mut step = 0.3 * kmean / gmax;
        let mut accepted = false;
        for _ in 0..6 {
            let trial: Vec<f32> = kappa.iter().zip(&grad).map(|(&k, &g)| (k - step * g).max((1000.0f32).powi(2))).collect();
            let m = misfit(&trial, cfg, dx, dt, geom, observed);
            if m < chi {
                kappa = trial;
                accepted = true;
                break;
            }
            step *= 0.5;
        }
        if !accepted {
            break; // converged / step too small
        }
    }
    history.push(misfit(&kappa, cfg, dx, dt, geom, observed));

    let mut speed = init_speed.clone();
    for (o, &k) in speed.data.iter_mut().zip(&kappa) {
        *o = k.max(0.0).sqrt();
    }
    FwiResult { speed, misfit_history: history }
}

/// Zero the gradient within `radius` cells of any of `cells` (mute footprints).
fn mute_around(grad: &mut [f32], n: usize, cells: &[usize], radius: i64) {
    for &c in cells {
        let (cx, cy) = ((c % n) as i64, (c / n) as i64);
        for dy in -radius..=radius {
            for dx in -radius..=radius {
                let (x, y) = (cx + dx, cy + dy);
                if x >= 0 && y >= 0 && (x as usize) < n && (y as usize) < n {
                    grad[y as usize * n + x as usize] = 0.0;
                }
            }
        }
    }
}

/// In-place 3×3 box smoothing, `passes` times.
fn box_smooth(g: &mut [f32], n: usize, passes: usize) {
    let mut tmp = g.to_vec();
    for _ in 0..passes {
        for y in 1..n - 1 {
            for x in 1..n - 1 {
                let i = y * n + x;
                tmp[i] = (g[i] + g[i - 1] + g[i + 1] + g[i - n] + g[i + n]) / 5.0;
            }
        }
        g.copy_from_slice(&tmp);
    }
}

/// One stage of a multi-scale (frequency-continuation) inversion.
pub struct Stage {
    /// Configuration for this stage (typically a distinct centre frequency).
    pub cfg: FwiConfig,
    /// Observed traces band-matched to this stage's source.
    pub observed: Vec<Vec<Vec<f32>>>,
    /// Gradient-descent iterations for this stage.
    pub iters: usize,
}

/// Multi-scale FWI: invert low frequencies first (smooth, cycle-skip-robust),
/// then refine at higher frequencies, chaining the model and lightly smoothing it
/// between stages (model-space regularisation). This is the standard remedy that
/// turns FWI from anomaly *detection* into quantitative *recovery*.
pub fn invert_multiscale(init_speed: &Grid, geom: &Geometry, stages: &[Stage]) -> FwiResult {
    let mut model = init_speed.clone();
    let mut history = Vec::new();
    for (k, stage) in stages.iter().enumerate() {
        let r = invert(&model, &stage.cfg, geom, &stage.observed, stage.iters);
        model = r.speed;
        // Model-space regularisation between stages (not after the final stage).
        if k + 1 < stages.len() {
            box_smooth(&mut model.data, stage.cfg.n, 1);
        }
        history.extend(r.misfit_history);
    }
    FwiResult { speed: model, misfit_history: history }
}

/// Generate synthetic "observed" traces for `true_speed` (the forward problem).
pub fn observe(true_speed: &Grid, cfg: &FwiConfig, geom: &Geometry) -> Vec<Vec<Vec<f32>>> {
    let dx = cfg.extent / cfg.n as f32;
    let c_max = true_speed.data.iter().cloned().fold(0.0f32, f32::max).max(1.0);
    let dt = cfg.dt.unwrap_or_else(|| stable_dt(dx, c_max * 1.3));
    let kappa: Vec<f32> = true_speed.data.iter().map(|&c| c * c).collect();
    (0..geom.sources.len()).map(|s| forward(&kappa, cfg, dx, dt, geom, s, false).0).collect()
}

/// CFL-stable `dt` used by [`observe`]/[`invert`] for a given config + speed.
pub fn time_step(cfg: &FwiConfig, speed: &Grid) -> f32 {
    let dx = cfg.extent / cfg.n as f32;
    let c_max = speed.data.iter().cloned().fold(0.0f32, f32::max).max(1.0);
    cfg.dt.unwrap_or_else(|| stable_dt(dx, c_max * 1.3))
}
