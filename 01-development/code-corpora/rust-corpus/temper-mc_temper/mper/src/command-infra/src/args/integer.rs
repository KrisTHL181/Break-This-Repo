use std::ops::Deref;

use crate::{
    ArgumentSpec, CommandArg, CommandReader, IntegerProperties, ParseError, ParserKind,
    ParserProperties, SuggestionProviderKind,
};

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct IntegerArg<const MIN: i32 = { i32::MIN }, const MAX: i32 = { i32::MAX }>(i32);

impl<const MIN: i32, const MAX: i32> Deref for IntegerArg<MIN, MAX> {
    type Target = i32;

    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

impl<const MIN: i32, const MAX: i32> CommandArg for IntegerArg<MIN, MAX> {
    type Raw<'a> = i32;

    const SUGGESTIONS: SuggestionProviderKind = SuggestionProviderKind::None;

    fn recognize<'a>(reader: &mut CommandReader<'a>) -> Result<Self::Raw<'a>, ParseError> {
        let cursor = reader.cursor();
        let raw = reader.read_word_span()?;
        let value = raw
            .parse::<i32>()
            .map_err(|_| ParseError::new(cursor, "integer", "invalid integer"))?;

        if value < MIN {
            return Err(ParseError::new(
                cursor,
                "integer",
                format!("integer too small: {value}, expected at least {MIN}"),
            ));
        }

        if value > MAX {
            return Err(ParseError::new(
                cursor,
                "integer",
                format!("integer too large: {value}, expected at most {MAX}"),
            ));
        }

        Ok(value)
    }

    fn parse(raw: Self::Raw<'_>) -> Result<Self, ParseError> {
        Ok(Self(raw))
    }

    fn argument_spec() -> ArgumentSpec {
        ArgumentSpec::with_properties(
            ParserKind::Integer,
            ParserProperties::Integer(IntegerProperties {
                min: Some(MIN),
                max: Some(MAX),
            }),
        )
    }
}
