use bevy_ecs::change_detection::DetectChanges;
use bevy_ecs::prelude::{Entity, MessageWriter, Query, Ref, Without};
use temper_components::last_chunk_pos::LastChunkPos;
use temper_components::player::player_marker::PlayerMarker;
use temper_components::player::position::Position;
use temper_messages::cross_chunk_boundary_event::ChunkBoundaryCrossed;

pub fn handle(
    mut query: Query<(Entity, Ref<Position>, &mut LastChunkPos), Without<PlayerMarker>>,
    mut writer: MessageWriter<ChunkBoundaryCrossed>,
) {
    for (entity, pos, mut last_chunk) in query.iter_mut() {
        if !pos.is_changed() {
            continue;
        }

        let new_chunk = pos.chunk();
        if last_chunk.0 == new_chunk {
            continue;
        }

        writer.write(ChunkBoundaryCrossed {
            entity,
            old_chunk: last_chunk.0,
            new_chunk,
        });
        last_chunk.0 = new_chunk;
    }
}
