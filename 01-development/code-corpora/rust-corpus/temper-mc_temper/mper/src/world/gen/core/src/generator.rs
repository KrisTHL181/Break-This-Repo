use std::fmt::{self, Display, Formatter};

use temper_core::pos::ChunkPos;
use temper_world_format::Chunk;

use crate::{GenStage, GenerationError, StageSpec};

#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash)]
pub struct GeneratorId(&'static str);

impl GeneratorId {
    pub const fn new(id: &'static str) -> Self {
        Self(id)
    }

    pub const fn as_str(self) -> &'static str {
        self.0
    }
}

impl Display for GeneratorId {
    fn fmt(&self, f: &mut Formatter<'_>) -> fmt::Result {
        f.write_str(self.0)
    }
}

#[derive(Clone, Copy)]
pub struct StageNeighbor<'a> {
    pub pos: ChunkPos,
    pub stage: GenStage,
    pub chunk: &'a Chunk,
}

impl<'a> StageNeighbor<'a> {
    pub const fn new(pos: ChunkPos, stage: GenStage, chunk: &'a Chunk) -> Self {
        Self { pos, stage, chunk }
    }
}

#[derive(Clone, Copy)]
pub struct StageNeighborhood<'a> {
    chunks: &'a [StageNeighbor<'a>],
}

impl<'a> StageNeighborhood<'a> {
    pub const fn new(chunks: &'a [StageNeighbor<'a>]) -> Self {
        Self { chunks }
    }

    pub const fn empty() -> Self {
        Self { chunks: &[] }
    }

    pub fn iter(&self) -> impl Iterator<Item = &'a StageNeighbor<'a>> {
        self.chunks.iter()
    }

    pub fn get(&self, pos: ChunkPos) -> Option<&'a StageNeighbor<'a>> {
        self.iter().find(|chunk| chunk.pos == pos)
    }

    pub const fn len(&self) -> usize {
        self.chunks.len()
    }

    pub const fn is_empty(&self) -> bool {
        self.chunks.is_empty()
    }
}

pub struct StageInput<'a> {
    pub pos: ChunkPos,
    pub stage: GenStage,
    pub target: &'a mut Chunk,
    pub neighborhood: StageNeighborhood<'a>,
}

impl<'a> StageInput<'a> {
    pub const fn new(
        pos: ChunkPos,
        stage: GenStage,
        target: &'a mut Chunk,
        neighborhood: StageNeighborhood<'a>,
    ) -> Self {
        Self {
            pos,
            stage,
            target,
            neighborhood,
        }
    }
}

pub trait ChunkGenerator: Send + Sync {
    fn id(&self) -> GeneratorId;

    fn final_stage(&self) -> GenStage;

    fn stage_spec(&self, stage: GenStage) -> Option<StageSpec>;

    fn advance_stage(&self, input: StageInput<'_>) -> Result<(), GenerationError>;
}

#[cfg(test)]
mod tests {
    use temper_core::pos::ChunkPos;
    use temper_world_format::Chunk;

    use super::*;

    #[test]
    fn neighborhood_finds_chunks_by_position() {
        let chunk = Chunk::new_empty();
        let pos = ChunkPos::new(2, 4);
        let neighbor = StageNeighbor::new(pos, GenStage::NOISE, &chunk);
        let neighbors = [neighbor];
        let neighborhood = StageNeighborhood::new(&neighbors);

        assert!(neighborhood.get(pos).is_some());
        assert!(neighborhood.get(ChunkPos::new(8, 8)).is_none());
    }
}
