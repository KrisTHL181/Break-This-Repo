use bevy_ecs::message::MessageWriter;
use bevy_ecs::prelude::Query;
use temper_command_infra::args::PositionArg;
use temper_command_infra::{
    ArgumentSpec, CommandArg, CommandHandler, CommandReader, CommandResult, CommandSource,
    ParseError, SuggestionProviderKind,
};
use temper_components::player::position::Position;
use temper_components::player::rotation::Rotation;
use temper_entities::entity_types::EntityTypeEnum;
use temper_macros::Command;
use temper_messages::SpawnMobCommand;

#[derive(Debug, Command)]
#[command(name = "summon", aliases = ["spawn"])]
enum SummonCommand {
    AtSelf {
        mob_type: EntityTypeArg,
    },
    AtPos {
        mob_type: EntityTypeArg,
        pos: PositionArg,
    },
}

#[derive(Clone, Debug, Eq, PartialEq)]
struct EntityTypeArg {
    kind: EntityTypeEnum,
    name: String,
}

impl CommandArg for EntityTypeArg {
    type Raw<'a> = (&'a str, EntityTypeEnum);

    const SUGGESTIONS: SuggestionProviderKind = SuggestionProviderKind::None;

    fn recognize<'a>(reader: &mut CommandReader<'a>) -> Result<Self::Raw<'a>, ParseError> {
        let cursor = reader.cursor();
        let raw = reader.read_word_span()?;
        let Some(kind) = parse_entity_type(raw) else {
            return Err(ParseError::new(
                cursor,
                "entity type",
                format!("unknown entity type: {raw}"),
            ));
        };

        Ok((raw, kind))
    }

    fn parse((raw, kind): Self::Raw<'_>) -> Result<Self, ParseError> {
        Ok(Self {
            kind,
            name: entity_type_path(raw).unwrap_or(raw).to_string(),
        })
    }

    fn argument_spec() -> ArgumentSpec {
        ArgumentSpec::resource("minecraft:entity_type")
    }
}

impl CommandHandler for SummonCommand {
    type SystemParam<'w, 's> = (
        MessageWriter<'w, SpawnMobCommand>,
        Query<'w, 's, (&'static Position, &'static Rotation)>,
    );

    fn handle(
        self,
        source: CommandSource,
        params: &mut Self::SystemParam<'_, '_>,
    ) -> CommandResult {
        let (writer, query) = params;
        let CommandSource::Player(player_entity) = source else {
            return Err("Only players can use this command.".into());
        };

        let (entity_kind, entity_name, spawn_pos) = match self {
            SummonCommand::AtSelf { mob_type } => {
                let (pos, rot) = query
                    .get(player_entity)
                    .map_err(|_| "player entity does not exist")?;
                (mob_type.kind, mob_type.name, pos.offset_forward(rot, 2.0))
            }
            SummonCommand::AtPos { mob_type, pos } => (
                mob_type.kind,
                mob_type.name,
                pos.resolve(
                    query
                        .get(player_entity)
                        .map_err(|_| "player entity does not exist")?
                        .0,
                ),
            ),
        };

        writer.write(SpawnMobCommand {
            entity_type: entity_kind,
            location: spawn_pos,
        });
        source.send_message(format!("{} spawned!", entity_name).into());

        Ok(())
    }
}

fn parse_entity_type(raw: &str) -> Option<EntityTypeEnum> {
    EntityTypeEnum::from_snake_case(entity_type_path(raw)?)
}

fn entity_type_path(raw: &str) -> Option<&str> {
    let Some((namespace, path)) = raw.split_once(':') else {
        return Some(raw);
    };

    (namespace == "minecraft").then_some(path)
}

#[cfg(test)]
mod tests {
    use temper_command_infra::{
        CommandPathSegment, CommandSpec, ParserKind, ParserProperties, ResourceProperties,
    };
    use temper_entities::entity_types::EntityTypeEnum;

    use super::SummonCommand;

    #[test]
    fn summon_parses_entity_type_names() {
        let command = SummonCommand::parse("pig").unwrap();

        match command {
            SummonCommand::AtSelf { mob_type } => {
                assert_eq!(mob_type.kind, EntityTypeEnum::Pig);
                assert_eq!(mob_type.name, "pig");
            }
            SummonCommand::AtPos { .. } => panic!("expected summon at self"),
        }
    }

    #[test]
    fn summon_parses_namespaced_entity_type_names() {
        let command = SummonCommand::parse("minecraft:zombie").unwrap();

        match command {
            SummonCommand::AtSelf { mob_type } => {
                assert_eq!(mob_type.kind, EntityTypeEnum::Zombie);
                assert_eq!(mob_type.name, "zombie");
            }
            SummonCommand::AtPos { .. } => panic!("expected summon at self"),
        }
    }

    #[test]
    fn summon_parses_entity_type_with_position() {
        let command = SummonCommand::parse("minecraft:pig 1 ~ 3").unwrap();

        match command {
            SummonCommand::AtPos { mob_type, pos } => {
                assert_eq!(mob_type.kind, EntityTypeEnum::Pig);
                assert_eq!(mob_type.name, "pig");
                assert_eq!(pos.x, "1");
                assert_eq!(pos.y, "~");
                assert_eq!(pos.z, "3");
            }
            SummonCommand::AtSelf { .. } => panic!("expected summon at position"),
        }
    }

    #[test]
    fn summon_rejects_unknown_namespaces() {
        let err = SummonCommand::parse("temper:pig").unwrap_err();

        assert_eq!(err.expected, "entity type");
    }

    #[test]
    fn summon_uses_spawn_alias() {
        assert_eq!(SummonCommand::aliases(), &["spawn"]);
    }

    #[test]
    fn summon_uses_entity_type_resource_parser_and_position_parser() {
        let paths = SummonCommand::paths();
        let entity_spec = paths
            .iter()
            .filter_map(|path| path.segments.first())
            .find_map(|segment| match segment {
                CommandPathSegment::Argument { spec, .. } => Some(*spec),
                _ => None,
            })
            .unwrap();

        assert_eq!(entity_spec.parser, ParserKind::Resource);
        assert_eq!(
            entity_spec.properties,
            Some(ParserProperties::Resource(ResourceProperties {
                registry: "minecraft:entity_type",
            }))
        );
        assert_eq!(entity_spec.protocol_suggestions, None);
        assert_eq!(entity_spec.server_suggestions, None);

        assert!(paths.iter().any(|path| matches!(
            path.segments.as_slice(),
            [
                CommandPathSegment::Argument {
                    name: "mob_type",
                    spec: entity_spec,
                    ..
                },
                CommandPathSegment::Argument {
                    name: "pos",
                    spec: position_spec,
                    ..
                }
            ] if entity_spec.parser == ParserKind::Resource
                && position_spec.parser == ParserKind::Position
        )));
    }
}
