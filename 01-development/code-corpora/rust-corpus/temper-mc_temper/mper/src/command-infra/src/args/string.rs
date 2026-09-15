use std::ops::Deref;

use crate::{
    ArgKind, ArgumentSpec, CommandArg, CommandReader, ParseError, ParserKind, ParserProperties,
    StringMode, SuggestionProviderKind, reader::StringSpan,
};

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct SingleWordArg(String);

impl Deref for SingleWordArg {
    type Target = str;

    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

impl CommandArg for SingleWordArg {
    type Raw<'a> = &'a str;

    const SUGGESTIONS: SuggestionProviderKind = SuggestionProviderKind::None;

    fn recognize<'a>(reader: &mut CommandReader<'a>) -> Result<Self::Raw<'a>, ParseError> {
        reader.read_word_span()
    }

    fn parse(raw: Self::Raw<'_>) -> Result<Self, ParseError> {
        Ok(Self(raw.to_string()))
    }

    fn argument_spec() -> ArgumentSpec {
        ArgumentSpec::with_properties(
            ParserKind::String,
            ParserProperties::String(StringMode::Word),
        )
    }
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct QuotableStringArg(String);

impl Deref for QuotableStringArg {
    type Target = str;

    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

impl CommandArg for QuotableStringArg {
    type Raw<'a> = StringSpan<'a>;

    const SUGGESTIONS: SuggestionProviderKind = SuggestionProviderKind::None;

    fn recognize<'a>(reader: &mut CommandReader<'a>) -> Result<Self::Raw<'a>, ParseError> {
        reader.read_string_span()
    }

    fn parse(raw: Self::Raw<'_>) -> Result<Self, ParseError> {
        let parsed = match raw {
            StringSpan::Bare(span) => span.to_string(),
            StringSpan::Quoted(span) => {
                let mut result = String::new();
                let mut escaped = false;
                for c in span.chars() {
                    if escaped {
                        if matches!(c, '"' | '\\') {
                            result.push(c);
                        } else {
                            result.push('\\');
                            result.push(c);
                        }
                        escaped = false;
                        continue;
                    }
                    if c == '\\' {
                        escaped = true;
                    } else {
                        result.push(c);
                    }
                }
                if escaped {
                    result.push('\\');
                }
                result
            }
        };

        Ok(Self(parsed))
    }

    fn argument_spec() -> ArgumentSpec {
        ArgumentSpec::with_properties(
            ParserKind::String,
            ParserProperties::String(StringMode::Quotable),
        )
    }
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct GreedyStringArg(String);

impl Deref for GreedyStringArg {
    type Target = str;

    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

impl CommandArg for GreedyStringArg {
    type Raw<'a> = &'a str;

    const KIND: ArgKind = ArgKind::GreedyTail;
    const SUGGESTIONS: SuggestionProviderKind = SuggestionProviderKind::None;

    fn recognize<'a>(reader: &mut CommandReader<'a>) -> Result<Self::Raw<'a>, ParseError> {
        reader.read_remaining_span()
    }

    fn parse(raw: Self::Raw<'_>) -> Result<Self, ParseError> {
        Ok(Self(raw.to_string()))
    }

    fn argument_spec() -> ArgumentSpec {
        ArgumentSpec::with_properties(
            ParserKind::String,
            ParserProperties::String(StringMode::Greedy),
        )
    }
}
