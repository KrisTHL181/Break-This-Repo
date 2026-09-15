//! Simulated transmission acquisition across the ring.

use crate::geometry::Ring;
use crate::phantom::Phantom;
use crate::ray::Ray;
use crate::types::WATER_SPEED;

/// A single transmit/receive measurement along one acoustic path.
#[derive(Debug, Clone)]
pub struct Measurement {
    /// Transmitting element index.
    pub source: usize,
    /// Receiving element index.
    pub receiver: usize,
    /// Straight-line path length (m).
    pub path_length: f32,
    /// Simulated first-arrival travel time through tissue (s).
    pub travel_time: f32,
    /// Reference travel time if the path were pure water (s).
    pub water_time: f32,
    /// Integrated attenuation along the path (nepers).
    pub attenuation: f32,
    /// Whether the path carries usable interior signal.
    pub valid: bool,
    /// Discretised ray geometry, reused by the reconstructor.
    pub ray: Ray,
}

impl Measurement {
    /// Travel-time delay relative to the water reference (s). Positive means
    /// the path is slower than water (lower average speed of sound).
    #[inline]
    pub fn delay(&self) -> f32 {
        self.travel_time - self.water_time
    }
}

/// Parameters controlling the acquisition sweep.
#[derive(Debug, Clone, Copy)]
pub struct AcquisitionConfig {
    /// Receivers per transmit fan.
    pub fan: usize,
    /// Minimum source/receiver angular separation as a fraction of a half-turn.
    pub min_sep_frac: f32,
    /// Integration samples per grid cell when tracing rays.
    pub samples_per_cell: f32,
    /// Additive Gaussian-like timing noise standard deviation (s).
    pub time_noise: f32,
}

impl Default for AcquisitionConfig {
    fn default() -> Self {
        AcquisitionConfig {
            fan: 96,
            min_sep_frac: 0.25,
            samples_per_cell: 1.5,
            time_noise: 0.0,
        }
    }
}

/// The full set of measurements plus the ring used to produce them.
#[derive(Debug, Clone)]
pub struct Acquisition {
    /// All simulated measurements.
    pub measurements: Vec<Measurement>,
    /// Number of valid measurements.
    pub valid_count: usize,
}

/// Simulate a transmission acquisition of `phantom` using `ring`.
pub fn simulate(phantom: &Phantom, ring: &Ring, cfg: AcquisitionConfig) -> Acquisition {
    // Precompute the slowness field (1/c) so travel time is a linear integral.
    let slowness: Vec<f32> = phantom.speed.data.iter().map(|&c| 1.0 / c).collect();
    let atten = &phantom.attenuation.data;

    let mut measurements = Vec::new();
    let mut valid_count = 0;
    let mut noise = NoiseGen::new(0xC0FF_EE12_3456_789A);

    let n = ring.count();
    for source in 0..n {
        let recv = ring.fan_receivers(source, cfg.fan, cfg.min_sep_frac);
        for r in recv {
            // De-duplicate reciprocal pairs (source<receiver) to halve work.
            if r <= source {
                continue;
            }
            let a = ring.positions[source];
            let b = ring.positions[r];
            let ray = Ray::between(&phantom.speed, a, b, cfg.samples_per_cell);
            let interior = ray.interior_length();

            let mut tt = ray.integrate(&slowness);
            // The part of the path outside the grid travels through water.
            let exterior = (ray.length - interior).max(0.0);
            tt += exterior / WATER_SPEED;
            if cfg.time_noise > 0.0 {
                tt += noise.normal() * cfg.time_noise;
            }
            let water_time = ray.length / WATER_SPEED;
            let attenuation = ray.integrate(atten);

            // A path is informative if it spends meaningful length in tissue.
            let valid = interior > 0.5 * ray.length && ray.length > 0.0;
            if valid {
                valid_count += 1;
            }
            measurements.push(Measurement {
                source,
                receiver: r,
                path_length: ray.length,
                travel_time: tt,
                water_time,
                attenuation,
                valid,
                ray,
            });
        }
    }

    Acquisition {
        measurements,
        valid_count,
    }
}

/// Deterministic approximately-Gaussian noise via summed uniforms.
struct NoiseGen(u64);

impl NoiseGen {
    fn new(seed: u64) -> Self {
        NoiseGen(seed | 1)
    }
    fn next_f32(&mut self) -> f32 {
        // xorshift64*
        let mut x = self.0;
        x ^= x >> 12;
        x ^= x << 25;
        x ^= x >> 27;
        self.0 = x;
        ((x.wrapping_mul(0x2545_F491_4F6C_DD1D) >> 11) as f32) / (1u64 << 53) as f32
    }
    /// Approx N(0,1) by the central-limit sum of 6 uniforms.
    fn normal(&mut self) -> f32 {
        let mut s = 0.0;
        for _ in 0..6 {
            s += self.next_f32();
        }
        (s - 3.0) * std::f32::consts::SQRT_2 // scale to ~unit variance
    }
}
