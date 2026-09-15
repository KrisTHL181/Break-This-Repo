//! # Command args
//!
//! For simpler commands you can just reuse existing argument types defined in this module, but
//! there's a solid chance you'll need to make your own.
//!
//! Similarly to commands, args are defined with a struct and a trait (but no macro this time).
//! The general gist is that you define a struct that stores the value your command handler wants,
//! a method to recognize the next bit of command input, a method to turn that recognized input into
//! the final value, and some graph/suggestion metadata.
//!
//! ```rust
//! # use bevy_ecs::world::World;
//! # use temper_command_infra::{
//! #     ArgumentSpec, CommandArg, CommandReader, ParseError, ParserKind, ParserProperties,
//! #     StringMode, SuggestionInput, SuggestionProviderKind,
//! # };
//!
//! struct ExampleArg(String);
//!
//! impl CommandArg for ExampleArg {
//!     // The borrowed or cheap intermediate type returned by recognize().
//!     type Raw<'a> = &'a str;
//!
//!     // Whether the command graph should use no suggestions, a vanilla protocol provider,
//!     // or server/ECS-backed suggestions.
//!     const SUGGESTIONS: SuggestionProviderKind = SuggestionProviderKind::None;
//!
//!     // Consume exactly the input that belongs to this argument and return the raw value.
//!     // Keep this cheap, because the parser may try several command variants and rewind.
//!     // It is fine to do cheap syntax checks here when they decide whether this arg shape fits.
//!     fn recognize<'a>(reader: &mut CommandReader<'a>) -> Result<Self::Raw<'a>, ParseError> {
//!         reader.read_word_span()
//!     }
//!
//!     // Build the final arg value from the raw value returned by recognize().
//!     // This is the right place for conversions, allocation, and semantic validation.
//!     fn parse(raw: Self::Raw<'_>) -> Result<Self, ParseError> {
//!         Ok(Self(raw.to_string()))
//!     }
//!
//!     // Describe the parser/properties that should be sent to the client command graph.
//!     // Check out the ArgumentSpec docs to see what you should use.
//!     fn argument_spec() -> ArgumentSpec {
//!         ArgumentSpec::with_properties(
//!             ParserKind::String,
//!             ParserProperties::String(StringMode::Word),
//!         )
//!     }
//!
//!     // When SuggestionProviderKind::Server is used, this method is called to generate suggestions
//!     // for the client. It has ECS access so can be useful for stuff like searching for entities.
//!     // This method is optional so you don't need to implement it if you don't need it.
//!     fn suggest(_input: SuggestionInput<'_>, _world: &mut World) -> Vec<String> {
//!         vec!["example".to_string()]
//!     }
//! }
//! ```
//!
//! See [SuggestionProviderKind](crate::SuggestionProviderKind) for which suggestion mode to use.
//! Most args should use [SuggestionProviderKind::None](crate::SuggestionProviderKind::None).

mod entity;
mod integer;
mod position;
mod string;

pub use entity::{EntitiesArg, EntityArg, PlayerArg, PlayersArg};
pub use integer::IntegerArg;
pub use position::PositionArg;
pub use string::{GreedyStringArg, QuotableStringArg, SingleWordArg};
