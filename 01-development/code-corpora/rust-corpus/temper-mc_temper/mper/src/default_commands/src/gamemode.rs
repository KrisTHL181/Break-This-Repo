use bevy_ecs::message::MessageWriter;
use bevy_ecs::prelude::{Entity, Query, With, World};
use temper_command_infra::args::EntityArg;
use temper_command_infra::{
    ArgKind, ArgumentSpec, CommandArg, CommandHandler, CommandReader, CommandResult, CommandSource,
    ParseError, ParserKind, ParserProperties, StringMode, SuggestionInput, SuggestionProviderKind,
};
use temper_components::entity_identity::Identity;
use temper_components::player::gamemode::GameMode;
use temper_components::player::player_marker::PlayerMarker;
use temper_macros::Command;
use temper_messages::PlayerGameModeChanged;

#[derive(Command)]
#[command("gamemode")]
enum GamemodeCommand {
    SelfTarget(#[arg("gamemode")] GamemodeArg),
    OtherTarget {
        gamemode: GamemodeArg,
        target: EntityArg,
    },
}

enum GamemodeArg {
    Survival,
    Creative,
    Adventure,
    Spectator,
}

impl CommandHandler for GamemodeCommand {
    type SystemParam<'w, 's> = (
        MessageWriter<'w, PlayerGameModeChanged>,
        Query<
            'w,
            's,
            (Entity, &'static Identity, Option<&'static PlayerMarker>),
            With<PlayerMarker>,
        >,
    );

    fn handle(
        self,
        source: CommandSource,
        params: &mut Self::SystemParam<'_, '_>,
    ) -> CommandResult {
        let (writer, query) = params;
        let player_entity = match source {
            CommandSource::Server => return Err("The server can't change gamemode.".into()),
            CommandSource::Player(entity) => entity,
        };

        match self {
            GamemodeCommand::SelfTarget(new_mode) => {
                writer.write(PlayerGameModeChanged {
                    player: player_entity,
                    new_mode: game_mode(new_mode),
                });
            }
            GamemodeCommand::OtherTarget { target, gamemode } => {
                let Some(target) = target.resolve(query.into_iter()).first().copied() else {
                    return Err("No players matched the target.".into());
                };

                writer.write(PlayerGameModeChanged {
                    player: target,
                    new_mode: game_mode(gamemode),
                });
            }
        }

        Ok(())
    }
}

fn game_mode(gamemode: GamemodeArg) -> GameMode {
    match gamemode {
        GamemodeArg::Survival => GameMode::Survival,
        GamemodeArg::Creative => GameMode::Creative,
        GamemodeArg::Adventure => GameMode::Adventure,
        GamemodeArg::Spectator => GameMode::Spectator,
    }
}

impl CommandArg for GamemodeArg {
    type Raw<'a> = &'a str;
    const KIND: ArgKind = ArgKind::Normal;
    const SUGGESTIONS: SuggestionProviderKind = SuggestionProviderKind::Server;

    fn recognize<'a>(reader: &mut CommandReader<'a>) -> Result<Self::Raw<'a>, ParseError> {
        let word = reader.read_word_span()?;
        match word {
            "0" | "survival" | "s" => Ok("survival"),
            "1" | "creative" | "c" => Ok("creative"),
            "2" | "adventure" | "a" => Ok("adventure"),
            // Not actually in vanilla but seems weird not having it
            "3" | "spectator" | "sp" => Ok("spectator"),
            // TODO: Hook this up to the actual config
            "5" | "default" | "d" => Ok("creative"),
            other => Err(ParseError::new(
                reader.cursor(),
                "gamemode",
                format!("invalid gamemode: {}", other),
            )),
        }
    }

    fn parse(raw: Self::Raw<'_>) -> Result<Self, ParseError> {
        let word = raw.split_whitespace().next().unwrap_or("");
        match word.to_ascii_lowercase().as_str() {
            "survival" | "0" | "s" => Ok(GamemodeArg::Survival),
            "creative" | "1" | "c" => Ok(GamemodeArg::Creative),
            "adventure" | "2" | "a" => Ok(GamemodeArg::Adventure),
            "spectator" | "3" | "sp" => Ok(GamemodeArg::Spectator),
            "default" | "5" | "d" => Ok(GamemodeArg::Creative),
            other => Err(ParseError::new(
                0,
                "gamemode",
                format!("invalid gamemode: {}", other),
            )),
        }
    }

    fn argument_spec() -> ArgumentSpec {
        ArgumentSpec::with_properties(
            ParserKind::String,
            ParserProperties::String(StringMode::Word),
        )
    }

    fn suggest(_input: SuggestionInput<'_>, _world: &mut World) -> Vec<String> {
        vec![
            "survival".to_string(),
            "creative".to_string(),
            "adventure".to_string(),
            "spectator".to_string(),
        ]
    }
}
