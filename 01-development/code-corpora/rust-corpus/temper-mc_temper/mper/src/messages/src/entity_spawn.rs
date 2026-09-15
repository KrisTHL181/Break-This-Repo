use bevy_ecs::prelude::{Entity, Message};
use temper_components::player::position::Position;
pub(crate) use temper_entities::entity_types::EntityTypeEnum;
use temper_entities::MobBundle;

/// Command to spawn a mob in front of a player.
///
/// This message is written by the /spawn command and processed by
/// the spawn_command_processor system which calculates the spawn position.
#[derive(Message)]
pub struct SpawnMobCommand {
    pub entity_type: EntityTypeEnum,
    pub location: Position,
}

#[derive(Message)]
pub struct SpawnMobBundle {
    pub bundle: MobBundle,
    pub persist: bool,
}

#[derive(Message)]
pub struct DespawnMob {
    pub entity: Entity,
    pub remove_from_chunk: bool,
}
