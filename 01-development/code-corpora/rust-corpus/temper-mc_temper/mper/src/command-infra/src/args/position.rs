use temper_components::player::position::Position;

use crate::{
    ArgumentSpec, CommandArg, CommandReader, ParseError, ParserKind, SuggestionProviderKind,
};

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct PositionArg {
    pub x: String,
    pub y: String,
    pub z: String,
}

impl PositionArg {
    pub fn resolve(&self, base: &Position) -> Position {
        Position::new(
            resolve_coord(&self.x, base.x),
            resolve_coord(&self.y, base.y),
            resolve_coord(&self.z, base.z),
        )
    }
}

impl CommandArg for PositionArg {
    type Raw<'a> = (&'a str, &'a str, &'a str);

    const SUGGESTIONS: SuggestionProviderKind = SuggestionProviderKind::None;

    fn recognize<'a>(reader: &mut CommandReader<'a>) -> Result<Self::Raw<'a>, ParseError> {
        let x = read_coord(reader, "x coordinate")?;
        let y = read_coord(reader, "y coordinate")?;
        let z = read_coord(reader, "z coordinate")?;

        Ok((x, y, z))
    }

    fn parse(raw: Self::Raw<'_>) -> Result<Self, ParseError> {
        Ok(Self {
            x: raw.0.to_string(),
            y: raw.1.to_string(),
            z: raw.2.to_string(),
        })
    }

    fn argument_spec() -> ArgumentSpec {
        ArgumentSpec::new(ParserKind::Position)
    }
}

fn read_coord<'a>(
    reader: &mut CommandReader<'a>,
    expected: &'static str,
) -> Result<&'a str, ParseError> {
    let cursor = reader.cursor();
    let span = reader.read_word_span()?;

    if let Some(relative) = span.strip_prefix('~')
        && (relative.is_empty() || relative.parse::<f64>().is_ok())
    {
        Ok(span)
    } else if span.parse::<f64>().is_ok() {
        Ok(span)
    } else {
        Err(ParseError::new(
            cursor,
            expected,
            format!("invalid {expected}: {span}"),
        ))
    }
}

fn resolve_coord(coord: &str, base: f64) -> f64 {
    if let Some(relative) = coord.strip_prefix('~') {
        if relative.is_empty() {
            base
        } else {
            base + relative.parse::<f64>().unwrap_or(0.0)
        }
    } else {
        coord.parse::<f64>().unwrap_or(base)
    }
}
