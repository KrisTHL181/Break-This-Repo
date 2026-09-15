// no_std + alloc support: works on ESP32-S3 and other bare-metal targets
// when the default `std` feature is disabled. With `std` (the default),
// the crate behaves exactly as before. The feature is purely additive
// for backwards compatibility — no existing consumer needs to change.
#![cfg_attr(not(feature = "std"), no_std)]

extern crate alloc;

// On no_std targets, f32 method calls like `.exp()` / `.sqrt()` / `.tanh()`
// are unavailable because they need libm. This trait restores them via
// the `libm` crate, so existing math code at the call site stays the same.
// On std builds the trait is not defined and the inherent f32 methods are
// used as before — zero behavioural change for std consumers.
#[cfg(not(feature = "std"))]
pub mod no_std_math {
    pub trait F32Ext {
        fn exp(self) -> Self;
        fn sqrt(self) -> Self;
        fn tanh(self) -> Self;
        fn powi(self, n: i32) -> Self;
    }
    impl F32Ext for f32 {
        #[inline]
        fn exp(self) -> Self {
            libm::expf(self)
        }
        #[inline]
        fn sqrt(self) -> Self {
            libm::sqrtf(self)
        }
        #[inline]
        fn tanh(self) -> Self {
            libm::tanhf(self)
        }
        #[inline]
        fn powi(self, n: i32) -> Self {
            libm::powf(self, n as f32)
        }
    }
}

pub mod attention;
pub mod fastgrnn_gate;
pub mod model;
pub mod tensor;

#[cfg(feature = "fp16")]
pub use attention::KvCacheF16;
pub use attention::{
    dense_attention, AttentionBackend, AttentionError, IncrementalLandmarks, KvCache,
    SparseAttentionConfig, SubquadraticSparseAttention,
};
pub use fastgrnn_gate::{FastGrnnGate, DEFAULT_HIDDEN_DIM as FASTGRNN_DEFAULT_HIDDEN_DIM};
pub use model::{RuvLlmSparseBlock, RuvLlmSparseBlockConfig};
pub use tensor::Tensor3;
