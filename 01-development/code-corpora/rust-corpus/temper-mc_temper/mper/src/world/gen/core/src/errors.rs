use temper_core::pos::ChunkPos;
use thiserror::Error;

use crate::GenStage;

#[derive(Debug, Error)]
pub enum GenerationError {
    #[error("unknown generation stage {0}")]
    UnknownStage(GenStage),
    #[error("missing neighbor chunk {pos:?} at generation stage {required_stage}")]
    MissingNeighbor {
        pos: ChunkPos,
        required_stage: GenStage,
    },
    #[error("generation failed: {0}")]
    Failed(String),
}
