//! # Commands in Temper
//!
//! Defining commands is pretty simple: define an enum or struct, derive `Command`, and give it a
//! root command name.
//!
//! ```rust
//! # use temper_command_infra::args::{EntityArg, PositionArg};
//! # use temper_command_infra::{CommandHandler, CommandResult, CommandSource};
//! # use temper_macros::Command;
//!
//! #[derive(Command)]
//! #[command("example")]
//! enum ExampleCommand {
//!     WithEntity { entity: EntityArg },
//!     WithoutEntity,
//!     #[subcommand("sub")]
//!     Subcommand(ExampleSingleSubcommand),
//! }
//!
//! #[derive(Command)]
//! #[command(subcommand)]
//! struct ExampleSingleSubcommand {
//!     location: PositionArg,
//! }
//! # impl CommandHandler for ExampleCommand {
//! #     type SystemParam<'w, 's> = ();
//! #
//! #     fn handle(self, _source: CommandSource, _params: &mut Self::SystemParam<'_, '_>) -> CommandResult {
//! #         Ok(())
//! #     }
//! # }
//! ```
//!
//! Then implement [CommandHandler] for the command:
//!
//! ```rust
//! # use bevy_ecs::prelude::{Query, Res};
//! # use temper_command_infra::{CommandHandler, CommandResult, CommandSource, ParseError};
//! # use temper_command_infra::args::{EntityArg, PositionArg};
//! # use temper_components::player::position::Position;
//! # use temper_components::player::rotation::Rotation;
//! # use temper_macros::Command;
//! # use temper_state::GlobalStateResource;
//! # #[derive(Command)]
//! # #[command(subcommand)]
//! # struct ExampleSingleSubcommand { location: PositionArg }
//! # #[derive(Command)]
//! # #[command("example")]
//! # enum ExampleCommand {
//! #     WithEntity { entity: EntityArg },
//! #     WithoutEntity,
//! #     #[subcommand("sub")]
//! #     Subcommand(ExampleSingleSubcommand),
//! # }
//!
//! impl CommandHandler for ExampleCommand {
//!     // These can be whatever ECS params you need
//!     type SystemParam<'w, 's> = (
//!         Res<'w, GlobalStateResource>,
//!         Query<'w, 's, (&'static Position, &'static Rotation)>,
//!     );
//!
//!     fn handle(self, source: CommandSource, params: &mut Self::SystemParam<'_, '_>) -> CommandResult {
//!         let (_state, _positions) = params;
//!
//!         match self {
//!             ExampleCommand::WithEntity { entity } => {
//!                 // do something with the entity name/uuid/selector the player gave in the first argument
//!             },
//!             ExampleCommand::WithoutEntity => {
//!                 // do something without an entity
//!             },
//!             ExampleCommand::Subcommand(subcommand) => {
//!                 // do something with the subcommand
//!                 let location = subcommand.location;
//!                 // do something with the position the player gave in the first argument of the subcommand
//!             }
//!         }
//!
//!         Ok(())
//!     }
//!
//!     // Optional error handler method
//!     fn handle_parse_error(
//!         source: CommandSource,
//!         error: ParseError,
//!         _params: &mut Self::SystemParam<'_, '_>,
//!     ) {}
//! }
//! ```
//! The entire system revolves around the [CommandHandler] trait, which is implemented on the
//! command enum/struct. Under the hood the derive macro will generate all the code needed to have
//! the command wired into the ECS, provide suggestions and parse the arguments. All you need to do
//! is define what arguments a command needs and what it does with those args. If a handler returns
//! an error, the dispatcher sends that error to the command source automatically.
//!
//! There are several attributes available including literal args:
//!
//! ```rust
//! # use temper_command_infra::{CommandHandler, CommandResult, CommandSource};
//! # use temper_macros::Command;
//! #[derive(Command)]
//! #[command("example")]
//! enum ExampleCommand {
//!     #[literal("literal")]
//!     LiteralCommand,
//! }
//! # impl CommandHandler for ExampleCommand {
//! #     type SystemParam<'w, 's> = ();
//! #
//! #     fn handle(self, _source: CommandSource, _params: &mut Self::SystemParam<'_, '_>) -> CommandResult {
//! #         Ok(())
//! #     }
//! # }
//! ```
//! that skip the hassle of parsing and verifying an argument when you only allow a specific set of
//! options, aliases on both commands and literals:
//!
//! ```rust
//! # use temper_command_infra::{CommandHandler, CommandResult, CommandSource};
//! # use temper_macros::Command;
//! #[derive(Command)]
//! #[command(name = "example", aliases = ["ex", "exmpl"])]
//! enum ExampleCommand {
//!     #[literal("literal", aliases = ["lit", "l"])]
//!     LiteralCommand,
//! }
//! # impl CommandHandler for ExampleCommand {
//! #     type SystemParam<'w, 's> = ();
//! #
//! #     fn handle(self, _source: CommandSource, _params: &mut Self::SystemParam<'_, '_>) -> CommandResult {
//! #         Ok(())
//! #     }
//! # }
//! ```
//! and permissions:
//!
//! ```rust
//! # use temper_command_infra::{CommandHandler, CommandResult, CommandSource};
//! # use temper_command_infra::Permissions;
//! # use temper_macros::Command;
//! #[derive(Command)]
//! #[command(name = "example", permission = Permissions::Op)]
//! enum ExampleCommand {
//!     #[literal("literal")]
//!     #[permission(Permissions::Kill)]
//!     LiteralCommand,
//! }
//! # impl CommandHandler for ExampleCommand {
//! #     type SystemParam<'w, 's> = ();
//! #
//! #     fn handle(self, _source: CommandSource, _params: &mut Self::SystemParam<'_, '_>) -> CommandResult {
//! #         Ok(())
//! #     }
//! # }
//! ```
//! Permissions are used when building the command graph and when parsing/dispatching commands, so
//! players should not be able to use a command path they do not have permission for. Handlers should
//! still validate any game-specific assumptions, such as whether a resolved entity actually exists.
//!
//! This is the general gist of using commands with existing argument types, check out [args]
//! for how to make your own argument types.

pub mod args;
pub mod ecs;
pub mod error;
pub mod graph;
pub mod metadata;
pub mod reader;
pub mod suggestions;

pub use ctor;
pub use ecs::{
    CommandDispatched, CommandError, CommandHandler, CommandRegistry, CommandResult, CommandSource,
    PlayerCommandGraph, RebuildCommandGraph, RegisteredCommand,
};
pub use ecs::{
    add_system, dispatch_command, register_command_systems, register_static_command,
    send_command_error, send_parse_error, static_commands,
};
pub use error::ParseError;
pub use graph::{CommandGraph, CommandNode, CommandNodeKind};
pub use metadata::SubcommandSpec;
pub use metadata::{
    ArgKind, ArgumentSpec, CommandArg, CommandPath, CommandPathSegment, CommandSpec,
    EntityProperties, IntegerProperties, ParserKind, ParserProperties, ResourceProperties,
    StringMode, SuggestionProviderKind,
};
pub use reader::{Checkpoint, CommandReader};
pub use suggestions::{
    SuggestionInput, command_arg_suggestion_id, register_command_arg_suggestions,
    suggest_command_arg,
};
pub use temper_permissions::Permissions;
