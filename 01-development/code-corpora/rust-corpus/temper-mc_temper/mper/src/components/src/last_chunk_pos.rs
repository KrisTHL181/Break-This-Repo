use bevy_ecs::prelude::Component;
use temper_core::pos::ChunkPos;

#[derive(Component, Clone, Copy, Debug, Eq, PartialEq)]
pub struct LastChunkPos(pub ChunkPos);

impl LastChunkPos {
    pub fn new(chunk: ChunkPos) -> Self {
        Self(chunk)
    }
}
