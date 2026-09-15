use bevy_ecs::prelude::{Query, ResMut};
use temper_command_infra::args::IntegerArg;
use temper_command_infra::{
    ArgumentSpec, CommandArg, CommandHandler, CommandReader, CommandResult, CommandSource,
    ParseError, ParserKind, ParserProperties, StringMode, SuggestionProviderKind,
};
use temper_components::player::time::LastSentTimeUpdate;
use temper_macros::Command;
use temper_resources::time::WorldTime;

#[derive(Command)]
#[command("time")]
enum TimeCommand {
    #[subcommand("set")]
    Set(TimeSetCommand),
    #[literal("add")]
    Add { time: IntegerArg<0, 24000> },
    #[literal("query")]
    Query,
}

#[derive(Command)]
#[command(subcommand)]
enum TimeSetCommand {
    #[literal("day", aliases = ["d"])]
    Day,
    #[literal("noon")]
    Noon,
    #[literal("night", aliases = ["n"])]
    Night,
    #[literal("midnight")]
    Midnight,
    #[literal("dawn")]
    Dawn,
    #[literal("dusk")]
    Dusk,
    Ticks {
        value: TimeSetArg,
    },
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
struct TimeSetArg(u32);

impl CommandArg for TimeSetArg {
    type Raw<'a> = u32;

    const SUGGESTIONS: SuggestionProviderKind = SuggestionProviderKind::None;

    fn recognize<'a>(reader: &mut CommandReader<'a>) -> Result<Self::Raw<'a>, ParseError> {
        let cursor = reader.cursor();
        let raw = reader.read_word_span()?;
        parse_time_ticks(raw).map_err(|message| ParseError::new(cursor, "time", message))
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
}

impl CommandHandler for TimeCommand {
    type SystemParam<'w, 's> = (
        ResMut<'w, WorldTime>,
        Query<'w, 's, &'static mut LastSentTimeUpdate>,
    );

    fn handle(
        self,
        source: CommandSource,
        params: &mut Self::SystemParam<'_, '_>,
    ) -> CommandResult {
        let (world_time, last_sent_time) = params;

        match self {
            Self::Set(command) => {
                let ticks = command.ticks();
                world_time.set_time(ticks);

                source.send_message(
                    format!("Set the world time to {} ticks", world_time.current_time()).into(),
                );
                send_time_next_tick(last_sent_time);
            }
            Self::Add { time } => {
                let ticks = *time as u16;
                let new_time = world_time.current_time() + ticks;

                world_time.set_time(new_time);

                source.send_message(format!("Advanced the world time by {} ticks", *time).into());
                send_time_next_tick(last_sent_time);
            }
            Self::Query => {
                source.send_message(
                    format!("The current world time is: {}", world_time.current_time()).into(),
                );
            }
        }

        Ok(())
    }
}

impl TimeSetCommand {
    fn ticks(&self) -> u16 {
        match self {
            Self::Day => 1000,
            Self::Noon => 6000,
            Self::Night => 13000,
            Self::Midnight => 18000,
            Self::Dawn => 0,
            Self::Dusk => 12000,
            Self::Ticks { value } => (value.0 % u32::from(WorldTime::MAX_TIME)) as u16,
        }
    }
}

fn parse_time_ticks(raw: &str) -> Result<u32, &'static str> {
    if raw.is_empty() {
        return Err("empty time value");
    }

    match raw.chars().last().unwrap() {
        's' => parse_time_number(&raw[..raw.len() - 1])?
            .checked_mul(20)
            .ok_or("time value is too large"),
        't' => parse_time_number(&raw[..raw.len() - 1]),
        _ => parse_time_number(raw),
    }
}

fn parse_time_number(raw: &str) -> Result<u32, &'static str> {
    raw.parse::<u32>().map_err(|_| "invalid time value")
}

fn send_time_next_tick(last_sent_time: &mut Query<&mut LastSentTimeUpdate>) {
    for mut last_sent in last_sent_time.iter_mut() {
        last_sent.send_next_tick();
    }
}

#[cfg(test)]
mod tests {
    use temper_command_infra::CommandSpec;

    use super::*;

    #[test]
    fn time_set_literals_and_aliases_parse() {
        assert!(matches!(
            TimeCommand::parse("set day").unwrap(),
            TimeCommand::Set(TimeSetCommand::Day)
        ));
        assert!(matches!(
            TimeCommand::parse("set d").unwrap(),
            TimeCommand::Set(TimeSetCommand::Day)
        ));
        assert!(matches!(
            TimeCommand::parse("set n").unwrap(),
            TimeCommand::Set(TimeSetCommand::Night)
        ));
    }

    #[test]
    fn time_set_numeric_values_parse() {
        match TimeCommand::parse("set 10s").unwrap() {
            TimeCommand::Set(TimeSetCommand::Ticks { value }) => assert_eq!(value.0, 200),
            _ => panic!("expected numeric time set command"),
        }

        match TimeCommand::parse("set 10t").unwrap() {
            TimeCommand::Set(TimeSetCommand::Ticks { value }) => assert_eq!(value.0, 10),
            _ => panic!("expected numeric time set command"),
        }
    }
}
