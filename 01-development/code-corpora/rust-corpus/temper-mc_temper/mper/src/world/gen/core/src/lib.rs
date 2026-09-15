#![doc = include_str!("explainer.md")]
mod errors;
mod generator;
mod stage;

pub use errors::GenerationError;
pub use generator::{ChunkGenerator, GeneratorId, StageInput, StageNeighbor, StageNeighborhood};
pub use stage::{GenStage, StageDependencies, StageSpec};
