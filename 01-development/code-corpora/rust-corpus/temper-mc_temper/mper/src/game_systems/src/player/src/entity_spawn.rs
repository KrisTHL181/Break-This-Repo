use bevy_ecs::prelude::*;
use temper_entities::MobBundle;
use temper_messages::{SpawnMobBundle, SpawnMobCommand};

/// Processes `/spawn` command messages by turning them into mob bundle spawns.
pub fn spawn_command_processor(
    mut spawn_commands: MessageReader<SpawnMobCommand>,
    mut mob_bundle_events: MessageWriter<SpawnMobBundle>,
) {
    for command in spawn_commands.read() {
        mob_bundle_events.write(SpawnMobBundle {
            bundle: MobBundle::new(command.entity_type, command.location),
            persist: true,
        });
    }
}
