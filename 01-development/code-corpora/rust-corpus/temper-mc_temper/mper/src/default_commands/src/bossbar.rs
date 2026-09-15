use std::str::FromStr;

use bevy_ecs::entity::Entity;
use bevy_ecs::prelude::{Query, ResMut};
use bevy_ecs::world::World;
use rand::seq::IteratorRandom;
use temper_command_infra::args::GreedyStringArg;
use temper_command_infra::{
    ArgKind, ArgumentSpec, CommandArg, CommandHandler, CommandReader, CommandResult, CommandSource,
    ParseError, ParserKind, ParserProperties, StringMode, SuggestionInput, SuggestionProviderKind,
};
use temper_components::entity_identity::Identity;
use temper_components::player::bossbar_sender::BossbarSender;
use temper_components::player::player_marker::PlayerMarker;
use temper_macros::Command;
use temper_resources::bossbar::{BossBarData, BossBarResource, BossbarColor, BossbarDividers};
use temper_text::ClickEvent::CopyToClipboard;
use temper_text::HoverEvent::ShowText;
use temper_text::{TextComponent, TextComponentBuilder};
use uuid::Uuid;

#[derive(Command)]
#[command("bossbar")]
enum BossbarCommand {
    #[literal("add")]
    Add { name: GreedyStringArg },
    #[literal("get")]
    Get { id: BossbarIdArg },
    #[literal("list")]
    List,
    #[literal("remove")]
    Remove { id: BossbarIdArg },
    #[literal("set")]
    Set {
        id: BossbarIdArg,
        option: BossbarSetOptionArg,
    },
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
struct BossbarIdArg(Uuid);

impl CommandArg for BossbarIdArg {
    type Raw<'a> = Uuid;

    const SUGGESTIONS: SuggestionProviderKind = SuggestionProviderKind::Server;

    fn recognize<'a>(reader: &mut CommandReader<'a>) -> Result<Self::Raw<'a>, ParseError> {
        let cursor = reader.cursor();
        let raw = reader.read_word_span()?;

        Uuid::parse_str(raw).map_err(|_| ParseError::new(cursor, "bossbar id", "invalid UUID"))
    }

    fn parse(raw: Self::Raw<'_>) -> Result<Self, ParseError> {
        Ok(Self(raw))
    }

    fn argument_spec() -> ArgumentSpec {
        ArgumentSpec::with_properties(
            ParserKind::String,
            ParserProperties::String(StringMode::Word),
        )
    }

    fn suggest(_input: SuggestionInput<'_>, world: &mut World) -> Vec<String> {
        let Some(bossbars) = world.get_resource::<BossBarResource>() else {
            return Vec::new();
        };

        bossbars.boss_bars.keys().map(Uuid::to_string).collect()
    }
}

#[derive(Clone)]
enum BossbarSetOptionArg {
    Color(BossbarColor),
    Name(String),
    Players(String),
    Style(BossbarColor, BossbarDividers),
    Value(f32),
    Max(f32),
}

impl CommandArg for BossbarSetOptionArg {
    type Raw<'a> = &'a str;

    const KIND: ArgKind = ArgKind::GreedyTail;
    const SUGGESTIONS: SuggestionProviderKind = SuggestionProviderKind::Server;

    fn recognize<'a>(reader: &mut CommandReader<'a>) -> Result<Self::Raw<'a>, ParseError> {
        reader.read_remaining_span()
    }

    fn parse(raw: Self::Raw<'_>) -> Result<Self, ParseError> {
        parse_set_option(raw)
    }

    fn argument_spec() -> ArgumentSpec {
        ArgumentSpec::with_properties(
            ParserKind::String,
            ParserProperties::String(StringMode::Greedy),
        )
    }

    fn suggest(input: SuggestionInput<'_>, world: &mut World) -> Vec<String> {
        set_option_suggestions(input.full_input, world)
    }
}

impl CommandHandler for BossbarCommand {
    type SystemParam<'w, 's> = (
        ResMut<'w, BossBarResource>,
        Query<
            'w,
            's,
            (
                Entity,
                &'static Identity,
                &'static mut BossbarSender,
                Option<&'static PlayerMarker>,
            ),
        >,
    );

    fn handle(
        self,
        source: CommandSource,
        params: &mut Self::SystemParam<'_, '_>,
    ) -> CommandResult {
        let (bossbars, players) = params;

        match self {
            Self::Add { name } => add_bossbar(source, bossbars, &name),
            Self::Get { id } => get_bossbar(source, bossbars, id.0),
            Self::List => list_bossbars(source, bossbars),
            Self::Remove { id } => remove_bossbar(source, bossbars, id.0),
            Self::Set { id, option } => set_bossbar(source, bossbars, players, id.0, option),
        }
    }
}

fn add_bossbar(source: CommandSource, bossbars: &mut BossBarResource, name: &str) -> CommandResult {
    let uuid = bossbars.add_bar(BossBarData::new(
        TextComponent::from(name),
        0.0,
        100.0,
        BossbarColor::Pink,
    ));

    source.send_message(
        TextComponent::from(format!("Created bossbar with uuid: {uuid}"))
            .click_event(CopyToClipboard(uuid.to_string()))
            .hover_event(ShowText(TextComponent::from(uuid.to_string()).into())),
    );

    Ok(())
}

fn get_bossbar(source: CommandSource, bossbars: &BossBarResource, uuid: Uuid) -> CommandResult {
    if let Some(bossbar) = bossbars.boss_bars.get(&uuid) {
        source.send_message(
            TextComponentBuilder::new("Bossbar: ")
                .extra(TextComponent::from(format!("{bossbar}")))
                .build(),
        );
    } else {
        return Err(missing_bossbar_message(uuid).into());
    }

    Ok(())
}

fn list_bossbars(source: CommandSource, bossbars: &BossBarResource) -> CommandResult {
    if bossbars.boss_bars.is_empty() {
        source.send_message(TextComponentBuilder::new("No bossbars exist.").build());
        return Ok(());
    }

    for uuid in bossbars.boss_bars.keys() {
        source.send_message(
            TextComponentBuilder::new("Bossbar: ")
                .extra(
                    TextComponent::from(uuid.to_string())
                        .click_event(CopyToClipboard(uuid.to_string()))
                        .hover_event(ShowText(TextComponent::from(uuid.to_string()).into())),
                )
                .build(),
        );
    }

    Ok(())
}

fn remove_bossbar(
    source: CommandSource,
    bossbars: &mut BossBarResource,
    uuid: Uuid,
) -> CommandResult {
    if bossbars.boss_bars.contains_key(&uuid) {
        bossbars.remove_bar(uuid);
        source.send_message(TextComponentBuilder::new("removed bossbar").build());
    } else {
        return Err(missing_bossbar_message(uuid).into());
    }

    Ok(())
}

fn set_bossbar(
    source: CommandSource,
    bossbars: &mut BossBarResource,
    players: &mut Query<(Entity, &Identity, &mut BossbarSender, Option<&PlayerMarker>)>,
    uuid: Uuid,
    option: BossbarSetOptionArg,
) -> CommandResult {
    let Some(bossbar) = bossbars.boss_bars.get(&uuid) else {
        return Err(missing_bossbar_message(uuid).into());
    };

    match option {
        BossbarSetOptionArg::Color(color) => {
            let dividers = bossbar.dividers;
            for (_, _, mut sender, _) in players.iter_mut() {
                sender.update(uuid);
            }
            bossbars.update_style(uuid, color, dividers);
            source.send_message(TextComponentBuilder::new("Updated bossbar color").build());
        }
        BossbarSetOptionArg::Name(title) => {
            for (_, _, mut sender, _) in players.iter_mut() {
                sender.update(uuid);
            }
            bossbars.update_title(uuid, TextComponent::from(title));
            source.send_message(TextComponentBuilder::new("Updated bossbar name").build());
        }
        BossbarSetOptionArg::Players(target) => {
            set_bossbar_players(bossbars, players, uuid, &target)
        }
        BossbarSetOptionArg::Style(color, dividers) => {
            for (_, _, mut sender, _) in players.iter_mut() {
                sender.update(uuid);
            }
            bossbars.update_style(uuid, color, dividers);
            source.send_message(TextComponentBuilder::new("Updated bossbar style").build());
        }
        BossbarSetOptionArg::Value(value) => {
            let max = bossbar.max;
            for (_, _, mut sender, _) in players.iter_mut() {
                sender.update(uuid);
            }
            bossbars.update_health(uuid, value, max);
            source.send_message(TextComponentBuilder::new("Updated bossbar value").build());
        }
        BossbarSetOptionArg::Max(max) => {
            let health = bossbar.health;
            for (_, _, mut sender, _) in players.iter_mut() {
                sender.update(uuid);
            }
            bossbars.update_health(uuid, health, max);
            source.send_message(TextComponentBuilder::new("Updated bossbar max").build());
        }
    }

    Ok(())
}

fn set_bossbar_players(
    bossbars: &BossBarResource,
    players: &mut Query<(Entity, &Identity, &mut BossbarSender, Option<&PlayerMarker>)>,
    uuid: Uuid,
    target: &str,
) {
    match target {
        "@e" | "@a" => {
            for (_, _, mut sender, _) in players.iter_mut() {
                sender.add(uuid);
                bossbars.queue_networking(uuid, true);
            }
        }
        "@r" => {
            if let Some((_, _, mut sender, _)) = players.iter_mut().choose(&mut rand::rng()) {
                sender.add(uuid);
                bossbars.queue_networking(uuid, true);
            }
        }
        name => {
            for (_, identity, mut sender, marker) in players.iter_mut() {
                if marker.is_some()
                    && identity
                        .name
                        .as_ref()
                        .is_some_and(|player_name| player_name.eq_ignore_ascii_case(name))
                {
                    if sender.0.contains_key(&uuid) {
                        sender.remove(uuid);
                        bossbars.queue_networking(uuid, false);
                    } else {
                        sender.add(uuid);
                        bossbars.queue_networking(uuid, true);
                    }
                }
            }
        }
    }
}

fn parse_set_option(raw: &str) -> Result<BossbarSetOptionArg, ParseError> {
    let mut parts = raw.split_whitespace();
    let option = parts
        .next()
        .ok_or_else(|| ParseError::expected(0, "bossbar set option"))?;

    match option.to_ascii_lowercase().as_str() {
        "color" => Ok(BossbarSetOptionArg::Color(parse_next(
            parts.next(),
            "color",
        )?)),
        "name" => {
            let title = raw[option.len()..].trim();
            if title.is_empty() {
                Err(ParseError::expected(option.len(), "bossbar name"))
            } else {
                Ok(BossbarSetOptionArg::Name(title.to_string()))
            }
        }
        "players" => Ok(BossbarSetOptionArg::Players(
            parts
                .next()
                .ok_or_else(|| ParseError::expected(option.len(), "player"))?
                .to_string(),
        )),
        "style" => {
            let color = parse_next(parts.next(), "color")?;
            let dividers = parse_next(parts.next(), "style")?;
            Ok(BossbarSetOptionArg::Style(color, dividers))
        }
        "value" => Ok(BossbarSetOptionArg::Value(parse_float(
            parts.next(),
            "value",
        )?)),
        "max" => Ok(BossbarSetOptionArg::Max(parse_float(parts.next(), "max")?)),
        _ => Err(ParseError::new(
            0,
            "bossbar set option",
            format!("invalid bossbar set option: {option}"),
        )),
    }
}

fn parse_next<T: FromStr>(value: Option<&str>, expected: &'static str) -> Result<T, ParseError> {
    let value = value.ok_or_else(|| ParseError::expected(0, expected))?;
    value
        .parse()
        .map_err(|_| ParseError::new(0, expected, format!("invalid {expected}: {value}")))
}

fn parse_float(value: Option<&str>, expected: &'static str) -> Result<f32, ParseError> {
    parse_next(value, expected)
}

fn set_option_suggestions(input: &str, world: &mut World) -> Vec<String> {
    let Some(after_id) = set_option_input(input) else {
        return Vec::new();
    };
    let tokens = after_id.split_whitespace().collect::<Vec<_>>();

    match tokens.as_slice() {
        [] => set_option_names(),
        [option] => set_option_names()
            .into_iter()
            .filter(|suggestion| suggestion.starts_with(&option.to_ascii_lowercase()))
            .collect(),
        ["color", ..] => bossbar_colors(),
        ["style", color] if !after_id.ends_with(char::is_whitespace) => bossbar_colors()
            .into_iter()
            .filter(|suggestion| suggestion.starts_with(&color.to_ascii_lowercase()))
            .collect(),
        ["style", ..] => bossbar_styles(),
        ["players", player] if !after_id.ends_with(char::is_whitespace) => {
            player_suggestions(world)
                .into_iter()
                .filter(|suggestion| suggestion.starts_with(player))
                .collect()
        }
        ["players", ..] => player_suggestions(world),
        _ => Vec::new(),
    }
}

fn set_option_input(input: &str) -> Option<&str> {
    let command = input.strip_prefix('/').unwrap_or(input);
    let rest = command.strip_prefix("bossbar")?.trim_start();
    let rest = rest.strip_prefix("set")?.trim_start();
    let id_end = rest.find(char::is_whitespace)?;

    Some(rest[id_end..].trim_start())
}

fn set_option_names() -> Vec<String> {
    ["color", "name", "players", "style", "value", "max"]
        .into_iter()
        .map(str::to_string)
        .collect()
}

fn bossbar_colors() -> Vec<String> {
    ["blue", "green", "pink", "purple", "red", "white", "yellow"]
        .into_iter()
        .map(str::to_string)
        .collect()
}

fn bossbar_styles() -> Vec<String> {
    [
        "notched_6",
        "notched_10",
        "notched_12",
        "notched_20",
        "progress",
    ]
    .into_iter()
    .map(str::to_string)
    .collect()
}

fn player_suggestions(world: &mut World) -> Vec<String> {
    let mut suggestions = vec!["@e".to_string(), "@a".to_string(), "@r".to_string()];

    for (identity, _, marker) in world
        .query::<(&Identity, &BossbarSender, Option<&PlayerMarker>)>()
        .iter(world)
    {
        if marker.is_some()
            && let Some(name) = &identity.name
        {
            suggestions.push(name.clone());
        }
    }

    suggestions
}

fn missing_bossbar_message(uuid: Uuid) -> TextComponent {
    TextComponentBuilder::new("Bossbar doesn't exist for uuid: ")
        .extra(TextComponent::from(uuid.to_string()))
        .build()
}

#[cfg(test)]
mod tests {
    use temper_command_infra::CommandSpec;

    use super::*;

    #[test]
    fn bossbar_commands_parse() {
        assert!(matches!(
            BossbarCommand::parse("add Hello").unwrap(),
            BossbarCommand::Add { .. }
        ));
        assert!(matches!(
            BossbarCommand::parse("list").unwrap(),
            BossbarCommand::List
        ));
    }

    #[test]
    fn bossbar_set_options_parse() {
        let id = Uuid::new_v4();

        assert!(matches!(
            BossbarCommand::parse(&format!("set {id} color red")).unwrap(),
            BossbarCommand::Set {
                option: BossbarSetOptionArg::Color(BossbarColor::Red),
                ..
            }
        ));
        assert!(matches!(
            BossbarCommand::parse(&format!("set {id} style blue notched_10")).unwrap(),
            BossbarCommand::Set {
                option: BossbarSetOptionArg::Style(BossbarColor::Blue, BossbarDividers::TenNotches),
                ..
            }
        ));
    }

    #[test]
    fn bossbar_paths_preserve_old_syntax() {
        let paths = BossbarCommand::paths();

        assert!(paths.iter().any(|path| path.root == "bossbar"
            && matches!(
                path.segments.as_slice(),
                [
                    temper_command_infra::CommandPathSegment::Literal { name: "set", .. },
                    temper_command_infra::CommandPathSegment::Argument { name: "id", .. },
                    temper_command_infra::CommandPathSegment::Argument { name: "option", .. },
                ]
            )));
    }
}
