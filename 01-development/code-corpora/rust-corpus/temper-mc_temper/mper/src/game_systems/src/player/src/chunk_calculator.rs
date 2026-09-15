use bevy_ecs::prelude::{MessageReader, Query, Res};
use std::collections::HashSet;
use temper_components::player::chunk_receiver::ChunkReceiver;
use temper_components::player::client_information::ClientInformationComponent;
use temper_components::player::position::Position;
use temper_core::pos::ChunkPos;
use temper_messages::chunk_calc::ChunkCalc;
use temper_state::GlobalStateResource;
use tracing::warn;

pub fn handle(
    mut messages: MessageReader<ChunkCalc>,
    mut query: Query<(&Position, &mut ChunkReceiver, &ClientInformationComponent)>,
    state: Res<GlobalStateResource>,
) {
    for message in messages.read() {
        let (position, mut chunk_receiver, client_info) = match query.get_mut(message.0) {
            Ok(data) => data,
            Err(_) => {
                warn!("Player does not exist, skipping chunk calculation");
                continue;
            }
        };

        let server_render_distance = state.0.config.chunk_render_distance as i32;
        let client_view_distance = i32::from(client_info.view_distance);
        let radius = server_render_distance.min(client_view_distance);
        let player_chunk = ChunkPos::from(position.coords);

        // 1. Build the absolute set of chunks the player currently needs
        let mut needed_set = HashSet::new();
        for x in player_chunk.x() - radius..=player_chunk.x() + radius {
            for z in player_chunk.z() - radius..=player_chunk.z() + radius {
                needed_set.insert(ChunkPos::new(x, z));
            }
        }

        // 2. Unload chunks that are currently loaded but fall outside the new radius
        let mut to_unload = Vec::new();
        for &loaded_chunk in &chunk_receiver.loaded {
            if !needed_set.contains(&loaded_chunk) {
                to_unload.push(loaded_chunk);
            }
        }
        for chunk in to_unload {
            chunk_receiver.loaded.remove(&chunk);
            chunk_receiver.unloading.push_back(chunk);
        }

        // 3. Purge the queues of chunks the player ran away from before they could generate
        chunk_receiver.loading.retain(|c| needed_set.contains(c));
        chunk_receiver.dirty.retain(|c| needed_set.contains(c));

        // 4. Queue genuinely new chunks, preventing duplicates
        for &chunk_pos in &needed_set {
            if !chunk_receiver.loaded.contains(&chunk_pos)
                && !chunk_receiver.loading.contains(&chunk_pos)
                && !chunk_receiver.dirty.contains(&chunk_pos)
                && !chunk_receiver.in_flight.contains(&chunk_pos)
            {
                chunk_receiver.loading.push_back(chunk_pos);
            }
        }

        // 5. Re-sort the pending queue so the threads always generate the closest chunks first
        chunk_receiver
            .loading
            .make_contiguous()
            .sort_by_key(|chunk_pos| chunk_pos.pos.chebyshev_distance(player_chunk.pos));
    }
}
