use bevy_ecs::prelude::Message;

#[derive(Message)]
pub struct LoadChunkEntities(pub temper_core::pos::ChunkPos);
