use bevy_ecs::prelude::{MessageReader, MessageWriter, Query, Res, With};
use temper_components::combat::CombatProperties;
use temper_components::entity_identity::Identity;
use temper_components::last_synced_position::LastSyncedPosition;
use temper_components::metadata::EntityMetadata;
use temper_components::player::grounded::OnGround;
use temper_components::player::player_marker::PlayerMarker;
use temper_components::player::position::Position;
use temper_components::player::rotation::Rotation;
use temper_components::player::velocity::Velocity;
use temper_components::spawn::SpawnProperties;
use temper_core::dimension::Dimension::Overworld;
use temper_entities::mob_definition::StandardMobParts;
use temper_entities::{MobBundle, MobKind};
use temper_messages::chunk_calc::ChunkCalc;
use temper_messages::cross_chunk_boundary_event::ChunkBoundaryCrossed;
use temper_state::GlobalStateResource;
use temper_world::WorldError;
use tracing::error;

type MobQuery<'a> = (
    &'a Identity,
    &'a EntityMetadata,
    &'a CombatProperties,
    &'a SpawnProperties,
    &'a Position,
    &'a Rotation,
    &'a Velocity,
    &'a OnGround,
    &'a LastSyncedPosition,
    &'a MobKind,
);

pub fn cross_chunk_border(
    mut chunk_cross_events: MessageReader<ChunkBoundaryCrossed>,
    player_query: Query<(), With<PlayerMarker>>,
    mob_query: Query<MobQuery>,
    mut chunk_calc_messages: MessageWriter<ChunkCalc>,
    state: Res<GlobalStateResource>,
) {
    'ev_loop: for event in chunk_cross_events.read() {
        // If it's a player, send the chunk calc message
        if player_query.get(event.entity).is_ok() {
            chunk_calc_messages.write(ChunkCalc(event.entity));
            continue;
        }

        if let Ok((
            identity,
            metadata,
            combat,
            spawn,
            position,
            rotation,
            velocity,
            on_ground,
            last_synced_position,
            mob_kind,
        )) = mob_query.get(event.entity)
        {
            // For mobs, we update the chunk they are saved in
            let old_chunk_cords = event.old_chunk;
            let new_chunk_cords = event.new_chunk;
            // Remove the stale chunk entry before writing a fresh bundle below. We have to do it this way cos holding locks on multiple chunks at once can easily deadlock
            {
                let chunk = state.0.world.get_chunk(old_chunk_cords, Overworld);
                match chunk {
                    Ok(chunk) => {
                        chunk.entities.remove(&identity.uuid);
                        chunk.mark_dirty();
                    }
                    Err(WorldError::ChunkNotFound) => {
                        error!(
                            "Invalid old chunk coordinates in ChunkBoundaryCrossed event for entity {:?}: {:?}",
                            event.entity, old_chunk_cords
                        );
                        continue 'ev_loop;
                    }
                    Err(e) => {
                        error!(
                            "Error accessing old chunk in ChunkBoundaryCrossed event for entity {:?}: {:?}",
                            event.entity, e
                        );
                        continue 'ev_loop;
                    }
                }
            };

            let bundle = MobBundle::from_standard_parts(
                mob_kind.0,
                StandardMobParts {
                    identity,
                    metadata,
                    combat,
                    spawn,
                    position,
                    rotation,
                    velocity,
                    on_ground,
                    last_synced_position,
                },
            );

            // If the server crashes here, bye bye entity
            {
                let chunk = state
                    .0
                    .world
                    .get_or_generate_chunk(new_chunk_cords, Overworld)
                    .expect("Failed to get or generate new chunk in ChunkBoundaryCrossed event");
                chunk
                    .entities
                    .insert(identity.uuid, (bundle.kind(), bundle.serialize_for_chunk()));
                chunk.mark_dirty();
            }
        } else {
            error!(
                "Entity {:?} in ChunkBoundaryCrossed event is neither a player nor a mob",
                event.entity
            );
        }
    }
}
