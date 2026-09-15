use bevy_ecs::prelude::*;
use temper_components::entity_identity::Identity;

/// Fired by the `new_connection` system when a player joins
///
/// Fired by: `new_connection`.
/// Listened for by: `system_messages` to broadcast join and leave messages,
/// and `player_spawn` to broadcast spawn packets
#[derive(Message, Clone)]
pub struct PlayerJoined {
    pub identity: Identity,
    pub entity: Entity,
}
