use bevy_ecs::prelude::Message;
use temper_core::pos::ChunkPos;

#[derive(Message, Eq, Hash, PartialEq)]
pub struct SaveChunkEntities(pub ChunkPos);
