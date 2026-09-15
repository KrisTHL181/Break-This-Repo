use bevy_ecs::prelude::{Entity, Message};
use temper_core::pos::ChunkPos;

// Fired when an entity crosses a chunk boundary. Assumes dimensions are the same
#[derive(Message)]
pub struct ChunkBoundaryCrossed {
    pub entity: Entity,
    pub old_chunk: ChunkPos,
    pub new_chunk: ChunkPos,
}
