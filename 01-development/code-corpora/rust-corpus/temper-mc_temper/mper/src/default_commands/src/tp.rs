use bevy_ecs::entity::Entity;
use bevy_ecs::prelude::{MessageWriter, Query};
use temper_command_infra::CommandSource::*;
use temper_command_infra::args::{EntitiesArg, EntityArg, PositionArg};
use temper_command_infra::{CommandHandler, CommandResult, CommandSource};
use temper_components::entity_identity::Identity;
use temper_components::player::player_marker::PlayerMarker;
use temper_components::player::position::Position;
use temper_components::player::rotation::Rotation;
use temper_macros::Command;
use temper_messages::teleport_entity::TeleportEntity;

#[derive(Command)]
#[command("tp")]
enum TpCommand {
    ToPos {
        location: PositionArg,
    },
    ToEntity {
        destination: EntityArg,
    },
    EntityToPos {
        target: EntitiesArg,
        location: PositionArg,
    },
    EntityToEntity {
        target: EntitiesArg,
        destination: EntityArg,
    },
}

impl CommandHandler for TpCommand {
    type SystemParam<'w, 's> = (
        Query<'w, 's, (&'static Rotation, &'static Position)>,
        Query<'w, 's, (Entity, &'static Identity, Option<&'static PlayerMarker>)>,
        MessageWriter<'w, TeleportEntity>,
    );

    fn handle(
        self,
        source: CommandSource,
        params: &mut Self::SystemParam<'_, '_>,
    ) -> CommandResult {
        let (positions, identities, teleports) = params;
        match self {
            TpCommand::ToPos { location } => {
                let Player(player) = source else {
                    return Err("This command can only be used by players.".into());
                };

                let Ok((rotation, base_position)) = positions.get(player) else {
                    return Err("Could not find your player entity.".into());
                };

                let destination = location.resolve(base_position);
                teleport_entity(player, *rotation, destination, teleports);
                source.send_message(format!("Teleported to ({}).", destination).into());
            }
            TpCommand::ToEntity { destination } => {
                let Player(player) = source else {
                    return Err("This command can only be used by players.".into());
                };

                let targets = destination.resolve(identities.iter());
                if targets.len() != 1 {
                    return Err("You must specify exactly one target to teleport to.".into());
                }

                let Ok((rotation, _)) = positions.get(player) else {
                    return Err("Could not find your player entity.".into());
                };

                let Ok((_, target_position)) = positions.get(targets[0]) else {
                    return Err("Could not find target entity position.".into());
                };

                teleport_entity(player, *rotation, *target_position, teleports);
                source.send_message(
                    format!("Teleported to the entity at {}.", target_position).into(),
                );
            }
            TpCommand::EntityToPos { target, location } => {
                let base_position = if let Player(entity) = source
                    && let Ok((_, position)) = positions.get(entity)
                {
                    *position
                } else {
                    Position::new(0.0, 0.0, 0.0)
                };
                let destination = location.resolve(&base_position);
                let targets = target.resolve(identities.iter());

                if targets.is_empty() {
                    return Err("No entities matched the target.".into());
                }

                for entity in targets {
                    let Ok((rotation, _)) = positions.get(entity) else {
                        continue;
                    };
                    teleport_entity(entity, *rotation, destination, teleports);
                }

                source.send_message(format!("Teleported entities to ({}).", destination).into());
            }
            TpCommand::EntityToEntity {
                target,
                destination,
            } => {
                let targets = target.resolve(identities.iter());
                let destinations = destination.resolve(identities.iter());

                if targets.is_empty() {
                    return Err("No entities matched the target.".into());
                }

                if destinations.len() != 1 {
                    return Err("You must specify exactly one destination entity.".into());
                }

                let Ok((_, destination_position)) = positions.get(destinations[0]) else {
                    return Err("Could not find destination entity position.".into());
                };

                for entity in targets {
                    let Ok((rotation, _)) = positions.get(entity) else {
                        continue;
                    };
                    teleport_entity(entity, *rotation, *destination_position, teleports);
                }

                source.send_message(
                    format!("Teleported entities to {}.", destination_position).into(),
                );
            }
        }

        Ok(())
    }
}

fn teleport_entity(
    entity: Entity,
    rotation: Rotation,
    destination: Position,
    teleports: &mut MessageWriter<TeleportEntity>,
) {
    teleports.write(TeleportEntity::new(entity, destination, rotation));
}
