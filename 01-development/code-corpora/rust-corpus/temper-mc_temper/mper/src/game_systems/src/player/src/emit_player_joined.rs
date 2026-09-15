//! Emits `PlayerJoined` events after player entities are fully materialized.
//!
//! This system implements the "announce" phase of two-phase entity creation:
//! 1. `accept_new_connections` spawns entity + adds `PendingPlayerJoin` marker
//! 2. `apply_deferred` flushes commands (entity now exists)
//! 3. This system detects the marker and fires the actual event
//!
//! This ensures `PlayerJoined` events only fire when the entity is queryable.

use bevy_ecs::prelude::{Added, Commands, Entity, MessageWriter, Query};
use temper_command_infra::RebuildCommandGraph;
use temper_components::player::pending_events::PendingPlayerJoin;
use temper_messages::player_join::PlayerJoined;
use tracing::trace;

/// Fires `PlayerJoined` events for newly spawned player entities.
///
/// Must run after `apply_deferred` to ensure entities are fully created.
/// Scheduled via `.chain()` in the game loop.
pub fn emit_player_joined(
    query: Query<(Entity, &PendingPlayerJoin), Added<PendingPlayerJoin>>,
    mut events: MessageWriter<PlayerJoined>,
    mut command_graph_rebuilds: MessageWriter<RebuildCommandGraph>,
    mut commands: Commands,
) {
    for (entity, pending) in query.iter() {
        trace!(
            "Emitting PlayerJoined event for {} ({:?})",
            pending.0.name.clone().unwrap_or("Unknown".to_string()),
            entity
        );

        events.write(PlayerJoined {
            identity: pending.0.clone(),
            entity,
        });
        command_graph_rebuilds.write(RebuildCommandGraph { player: entity });

        // Remove the marker so we don't fire again.
        // This removal is deferred, but that's fine - Added<T> only fires once per addition.
        commands.entity(entity).remove::<PendingPlayerJoin>();
    }
}

#[cfg(test)]
mod tests {
    use bevy_ecs::message::{MessageRegistry, Messages};
    use bevy_ecs::prelude::{Schedule, World};
    use temper_command_infra::RebuildCommandGraph;
    use temper_components::entity_identity::Identity;

    use super::*;

    #[test]
    fn emits_command_graph_rebuild_for_joined_player() {
        let mut world = World::new();
        MessageRegistry::register_message::<PlayerJoined>(&mut world);
        MessageRegistry::register_message::<RebuildCommandGraph>(&mut world);

        let identity = Identity::new(Some("Player".to_string()));
        let player = world.spawn(PendingPlayerJoin(identity.clone())).id();

        let mut schedule = Schedule::default();
        schedule.add_systems(emit_player_joined);
        schedule.run(&mut world);

        let joins = world.resource::<Messages<PlayerJoined>>();
        let rebuilds = world.resource::<Messages<RebuildCommandGraph>>();

        assert_eq!(joins.len(), 1);
        assert_eq!(rebuilds.len(), 1);

        let join = joins.iter_current_update_messages().next().unwrap();
        let rebuild = rebuilds.iter_current_update_messages().next().unwrap();

        assert_eq!(join.entity, player);
        assert_eq!(join.identity.uuid, identity.uuid);
        assert_eq!(rebuild.player, player);
    }
}
