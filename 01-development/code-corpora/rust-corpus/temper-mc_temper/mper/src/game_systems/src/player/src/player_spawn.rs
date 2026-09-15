//! Handles global player list updates when players join the server.
//!
//! When a player joins:
//! 1. Send existing players' tab-list info to the new player
//! 2. Broadcast the new player's tab-list info to existing players
//!
//! Actual in-world entity spawning is handled by `EntityTracker`.

use bevy_ecs::prelude::{Entity, MessageReader, Query, Res};
use temper_components::entity_identity::Identity;
use temper_components::player::player_properties::PlayerProperties;
use temper_messages::player_join::PlayerJoined;
use temper_net_runtime::connection::StreamWriter;
use temper_protocol::outgoing::player_info_update::PlayerInfoUpdatePacket;
use temper_state::GlobalStateResource;
use tracing::{error, trace};

/// Listens for `PlayerJoined` events and syncs tab-list state for all players.
pub fn handle(
    mut events: MessageReader<PlayerJoined>,
    player_query: Query<(Entity, &Identity, &StreamWriter, &PlayerProperties)>,
    state: Res<GlobalStateResource>,
) {
    for event in events.read() {
        let new_player_entity = event.entity;
        let new_player_identity = &event.identity;

        // Get the new player's connection and components
        let Ok((_, _, new_conn, player_properties)) = player_query.get(new_player_entity) else {
            error!(
                "Failed to get new player components for tab sync: {:?}",
                new_player_entity
            );
            continue;
        };

        // Create packets for the new player once (to broadcast to existing players)
        let new_player_info_packet =
            PlayerInfoUpdatePacket::new_player_join_packet(new_player_identity, player_properties);

        let mut listed_for_new_player = 0;
        let mut listed_for_existing = 0;

        for (entity, identity, conn, player_properties) in player_query.iter() {
            // Skip self
            if entity == new_player_entity {
                continue;
            }

            // Skip disconnected players
            if !state.0.players.is_connected(entity) {
                continue;
            }

            // 1. Send existing player's info to the new player
            // PlayerInfoUpdate MUST come before SpawnEntity (protocol requirement)
            let existing_player_info =
                PlayerInfoUpdatePacket::new_player_join_packet(identity, player_properties);
            if let Err(e) = new_conn.send_packet_ref(&existing_player_info) {
                error!("Failed to send existing player info to new player: {:?}", e);
                continue;
            }
            listed_for_new_player += 1;

            // 2. Send new player's info to existing player
            if let Err(e) = conn.send_packet_ref(&new_player_info_packet) {
                error!("Failed to send new player info to existing player: {:?}", e);
                continue;
            }
            listed_for_existing += 1;
        }

        trace!(
            "Player {} joined: synced tab info for {} existing players and broadcast to {} players",
            new_player_identity.name.as_ref().expect("No Player Name"),
            listed_for_new_player,
            listed_for_existing
        );
    }
}
