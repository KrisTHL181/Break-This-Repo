//! Hardware acquisition boundary — a mock Butterfly Embedded adapter.
//!
//! There is **no public raw-hardware SDK** for the Butterfly Ultrasound-on-Chip
//! modules, so this module defines a *boundary*, not an integration: a trait the
//! reconstruction core can consume, plus a simulator that satisfies it. A future
//! licensed backend can implement [`AcquisitionBackend`] without touching the
//! physics core (ADR-0002).

use crate::acquisition::{simulate, Acquisition, AcquisitionConfig};
use crate::geometry::Ring;
use crate::phantom::Phantom;

/// A raw radio-frequency frame as it would arrive from hardware.
///
/// The simulator does not synthesise full RF waveforms; this type exists to fix
/// the shape of the data contract (channels × samples) so downstream code and
/// storage formats are designed for raw capture from day one (ADR-0003).
#[derive(Debug, Clone)]
pub struct RawRfFrame {
    /// Transmitting element index.
    pub source: usize,
    /// Number of receive channels in this frame.
    pub channels: usize,
    /// Samples per channel.
    pub samples: usize,
    /// Sample rate (Hz).
    pub sample_rate: f32,
}

/// Static description of a Butterfly Embedded configuration.
#[derive(Debug, Clone, Copy)]
pub struct ButterflyEmbeddedConfig {
    /// Number of Ultrasound-on-Chip modules in the ring.
    pub modules: usize,
    /// Channels per module.
    pub channels_per_module: usize,
    /// Centre frequency (MHz); Butterfly handhelds span ~1–12 MHz.
    pub center_freq_mhz: f32,
}

impl Default for ButterflyEmbeddedConfig {
    fn default() -> Self {
        // Public Midjourney prototype figure: ~40 modules per system.
        ButterflyEmbeddedConfig {
            modules: 40,
            channels_per_module: 64,
            center_freq_mhz: 3.0,
        }
    }
}

impl ButterflyEmbeddedConfig {
    /// Total transducer element count implied by the configuration.
    pub fn total_elements(&self) -> usize {
        self.modules * self.channels_per_module
    }
}

/// The contract any acquisition source (simulated or hardware) must satisfy.
pub trait AcquisitionBackend {
    /// Human-readable backend name (for provenance logging).
    fn name(&self) -> &str;
    /// Produce a set of transmission measurements for the given phantom.
    fn acquire(&self, phantom: &Phantom, ring: &Ring) -> Acquisition;
}

/// A simulator standing in for licensed Butterfly Embedded hardware.
#[derive(Debug, Clone, Default)]
pub struct MockButterflyEmbeddedBackend {
    /// Static hardware description.
    pub config: ButterflyEmbeddedConfig,
    /// Acquisition sweep parameters.
    pub acq: AcquisitionConfig,
}

impl AcquisitionBackend for MockButterflyEmbeddedBackend {
    fn name(&self) -> &str {
        "mock-butterfly-embedded"
    }

    fn acquire(&self, phantom: &Phantom, ring: &Ring) -> Acquisition {
        simulate(phantom, ring, self.acq)
    }
}
