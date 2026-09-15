//! # ruvector-turbovec
//!
//! Multi-bit **TurboQuant** FastScan-style approximate-nearest-neighbor index
//! (ADR-254). Fills the 2–4-bit scalar-quantized search regime that ruvector
//! lacked: `ruvector-rabitq` is 1-bit, `ruvllm`'s TurboQuant is a KV-cache value
//! codec — neither is a multi-bit *search index*.
//!
//! ## What it reuses
//!
//! - [`ruvector_rabitq::RandomRotation`] — the randomized Hadamard rotation
//!   (`O(D log D)`, no matrix stored) that makes per-coordinate scalar
//!   quantization codebook-free.
//! - [`ruvector_rabitq::AnnIndex`] — the shared index trait, so this drops into
//!   the same dispatcher/consumers as RaBitQ with no new plumbing.
//!
//! ## What it adds (ADR-254 §T2–T4, T6)
//!
//! - **Lloyd–Max 2/3/4-bit scalar quantization** ([`quantize`]) — data-independent
//!   centroids, online ingest, no training.
//! - **TQ+ per-coordinate calibration** ([`calibrate`]) — frozen after warm-up.
//! - **Length-renormalized scoring** — a per-vector scalar empirically reduces
//!   inner-product bias at zero query-time cost. It is not the paper's
//!   provably-unbiased QJL-residual estimator.
//! - **`IdMapIndex`** ([`idmap`]) — external `u64` ids, `O(1)` deletion,
//!   allowlist-filtered search.
//!
//! The FastScan nibble-LUT SIMD kernel (§T5) is a separate future milestone; the
//! scalar scorer here is its determinism oracle.
//!
//! Non-power-of-two input dimensions are zero-padded to the next power of two
//! and quantized in that full rotated space. This preserves L2 geometry (unlike
//! truncating a padded Hadamard transform) at the cost of additional code bytes.
//!
//! ## Example
//!
//! ```
//! use ruvector_turbovec::{TurboVecIndex, BitWidth};
//! use ruvector_rabitq::AnnIndex;
//!
//! let mut ix = TurboVecIndex::new(64, BitWidth::Four).unwrap();
//! for i in 0..1000 {
//!     let v: Vec<f32> = (0..64).map(|j| ((i * 7 + j) % 13) as f32).collect();
//!     ix.add(i, v).unwrap();
//! }
//! ix.finalize();
//! let q: Vec<f32> = (0..64).map(|j| (j % 13) as f32).collect();
//! let hits = ix.search(&q, 5).unwrap();
//! assert_eq!(hits.len(), 5);
//! ```

pub mod calibrate;
pub mod error;
pub mod idmap;
pub mod index;
pub mod quantize;

pub use calibrate::Calibration;
pub use error::{Result, TurboVecError};
pub use idmap::IdMapIndex;
pub use index::{TurboVecIndex, DEFAULT_WARMUP};
pub use quantize::BitWidth;

// Re-export the shared trait so downstream users get one import.
pub use ruvector_rabitq::{AnnIndex, SearchResult};
