//! Shared numeric types, tissue constants, and small utilities.
//!
//! All physical quantities use SI units unless noted:
//! - distances in metres (m)
//! - speed of sound in metres per second (m/s)
//! - time in seconds (s)
//! - acoustic attenuation in nepers per metre (Np/m)

/// Tissue class labels used by the synthetic phantom and the segmenter.
///
/// The ordering is stable and used as the on-the-wire `u8` value exported to
/// the WASM/JS layer, so do not reorder without updating the UI colour map.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
#[repr(u8)]
pub enum Tissue {
    /// Coupling water bath (background).
    Water = 0,
    /// Subcutaneous fat envelope.
    Fat = 1,
    /// Skeletal / smooth muscle.
    Muscle = 2,
    /// Soft organ parenchyma (e.g. liver, kidney).
    Organ = 3,
    /// Cortical bone (e.g. vertebra).
    Bone = 4,
}

impl Tissue {
    /// Total number of distinct tissue classes.
    pub const COUNT: usize = 5;

    /// All classes in label order.
    pub const ALL: [Tissue; Self::COUNT] =
        [Tissue::Water, Tissue::Fat, Tissue::Muscle, Tissue::Organ, Tissue::Bone];

    /// Human-readable class name.
    pub fn name(self) -> &'static str {
        match self {
            Tissue::Water => "water",
            Tissue::Fat => "fat",
            Tissue::Muscle => "muscle",
            Tissue::Organ => "organ",
            Tissue::Bone => "bone",
        }
    }

    /// Construct from the wire `u8` value, clamping unknown values to `Water`.
    pub fn from_u8(v: u8) -> Tissue {
        Self::ALL.get(v as usize).copied().unwrap_or(Tissue::Water)
    }

    /// Nominal speed of sound for this tissue (m/s). Literature mid-points.
    pub fn nominal_speed(self) -> f32 {
        match self {
            Tissue::Water => 1480.0,
            Tissue::Fat => 1450.0,
            Tissue::Muscle => 1580.0,
            Tissue::Organ => 1570.0,
            Tissue::Bone => 3000.0,
        }
    }

    /// Nominal acoustic attenuation (Np/m) at the simulated centre frequency.
    ///
    /// Derived from typical dB/(cm·MHz) figures collapsed to a single band; the
    /// absolute scale is illustrative, the *contrast* between tissues is what
    /// the attenuation reconstruction recovers.
    pub fn nominal_attenuation(self) -> f32 {
        match self {
            Tissue::Water => 0.5,
            Tissue::Fat => 9.0,
            Tissue::Muscle => 16.0,
            Tissue::Organ => 13.0,
            Tissue::Bone => 120.0,
        }
    }
}

/// Speed of sound in the coupling water bath (m/s).
pub const WATER_SPEED: f32 = 1480.0;

/// Plausible reconstruction bounds for speed of sound (m/s).
///
/// Reconstruction is clamped to this range to reject non-physical estimates
/// produced by ray-coverage gaps or noise.
pub const SPEED_MIN: f32 = 1300.0;
/// Upper plausible speed-of-sound bound (m/s).
pub const SPEED_MAX: f32 = 3400.0;

/// Clamp `x` to the inclusive range `[lo, hi]`.
#[inline]
pub fn clamp(x: f32, lo: f32, hi: f32) -> f32 {
    if x < lo {
        lo
    } else if x > hi {
        hi
    } else {
        x
    }
}

/// A 2-D point in physical (metre) coordinates.
#[derive(Debug, Clone, Copy, PartialEq)]
pub struct Point {
    /// X coordinate (m).
    pub x: f32,
    /// Y coordinate (m).
    pub y: f32,
}

impl Point {
    /// Construct a new point.
    pub const fn new(x: f32, y: f32) -> Self {
        Point { x, y }
    }

    /// Euclidean distance to another point.
    #[inline]
    pub fn dist(self, o: Point) -> f32 {
        let dx = self.x - o.x;
        let dy = self.y - o.y;
        (dx * dx + dy * dy).sqrt()
    }
}

/// Errors that can arise while configuring or running the pipeline.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum SonicError {
    /// A configuration value was outside its valid range.
    InvalidConfig(&'static str),
    /// The acquisition produced no usable measurements.
    NoMeasurements,
    /// A dimension mismatch between two grids/vectors.
    DimensionMismatch,
}

impl core::fmt::Display for SonicError {
    fn fmt(&self, f: &mut core::fmt::Formatter<'_>) -> core::fmt::Result {
        match self {
            SonicError::InvalidConfig(m) => write!(f, "invalid configuration: {m}"),
            SonicError::NoMeasurements => write!(f, "acquisition produced no measurements"),
            SonicError::DimensionMismatch => write!(f, "dimension mismatch"),
        }
    }
}

impl std::error::Error for SonicError {}

/// Convenience result alias.
pub type Result<T> = core::result::Result<T, SonicError>;
