//! Error type for the TurboVec index.

use thiserror::Error;

/// Errors produced by [`crate::TurboVecIndex`] / [`crate::IdMapIndex`].
#[derive(Debug, Error)]
pub enum TurboVecError {
    /// A vector's length did not match the index dimension.
    #[error("dimension mismatch: expected {expected}, got {got}")]
    DimMismatch { expected: usize, got: usize },

    /// `dim` was zero at construction.
    #[error("dimension must be > 0")]
    ZeroDim,

    /// An inserted vector contained NaN or infinity.
    #[error("vector contains a non-finite value")]
    NonFiniteVector,

    /// A query contained NaN or infinity.
    #[error("query contains a non-finite value")]
    NonFiniteQuery,

    /// A scoring helper was called before calibration was frozen.
    #[error("index has not been finalized")]
    NotFinalized,

    /// A scoring helper was called with an invalid row position.
    #[error("position {pos} is out of bounds for {len} encoded vectors")]
    PositionOutOfBounds { pos: usize, len: usize },

    /// Search was requested on an empty index.
    #[error("index is empty")]
    EmptyIndex,

    /// An external id was reused in `add_with_ids`.
    #[error("duplicate external id: {0}")]
    DuplicateId(u64),

    /// `add_with_ids` was called with `vectors.len() != ids.len()`.
    #[error("batch length mismatch: {vectors} vectors but {ids} ids")]
    BatchLenMismatch { vectors: usize, ids: usize },
}

/// Crate result alias.
pub type Result<T> = std::result::Result<T, TurboVecError>;
