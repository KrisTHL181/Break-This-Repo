use bevy_ecs::world::World;

use crate::SuggestionInput;
use crate::{CommandReader, ParseError};
use temper_permissions::Permissions;

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum ArgKind {
    /// A normal argument that consumes one logical argument slot.
    Normal,

    /// An argument that consumes the rest of the input.
    ///
    /// Greedy tail args must be the final field in a command variant.
    GreedyTail,
}

/// Extra mode information for string parsers.
#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum StringMode {
    /// A single unquoted word.
    Word,

    /// A single word or quoted string.
    Quotable,

    /// The rest of the command input.
    Greedy,
}

/// Min/max bounds for integer arguments.
#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct IntegerProperties {
    /// Minimum accepted value, if one should be sent to the client.
    pub min: Option<i32>,

    /// Maximum accepted value, if one should be sent to the client.
    pub max: Option<i32>,
}

/// Selector flags for entity arguments.
#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct EntityProperties {
    /// Whether the argument should accept only a single selected entity.
    pub single: bool,

    /// Whether the argument should accept only players.
    pub players_only: bool,
}

/// Registry metadata for resource arguments.
#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub struct ResourceProperties {
    /// The vanilla registry id, such as `minecraft:entity_type`.
    pub registry: &'static str,
}

/// Extra parser metadata sent to the client command graph.
///
/// Use this when the parser needs flags beyond the basic [ParserKind]. For example, string
/// arguments need to say whether they are word, quotable, or greedy strings, and resource arguments
/// need to say which registry they refer to.
#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum ParserProperties {
    /// Additional mode data for [ParserKind::String].
    String(StringMode),

    /// Bounds for [ParserKind::Integer].
    Integer(IntegerProperties),

    /// Selector flags for [ParserKind::Entity].
    Entity(EntityProperties),

    /// Registry id for [ParserKind::Resource].
    Resource(ResourceProperties),
}

/// The client-side parser type for an argument.
///
/// This does not parse commands on the server. It describes the argument to the client command graph
/// so the client can highlight input correctly and provide built-in completions where vanilla
/// supports them.
#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum ParserKind {
    /// A single word string.
    Word,

    /// A 32-bit integer.
    Integer,

    /// A string parser configured by [StringMode].
    String,

    /// A three-coordinate position parser.
    Position,

    /// An entity selector/name/uuid parser.
    Entity,

    /// A resource from a specific vanilla registry.
    Resource,
}

/// Controls how an argument participates in tab completion.
///
/// This is only for Brigadier suggestion providers. Parser metadata still belongs in
/// [ArgumentSpec]. For example, registry/resource completion should normally use
/// [ArgumentSpec::resource] instead of a suggestion provider.
#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum SuggestionProviderKind {
    /// Do not attach a suggestion provider to this argument.
    ///
    /// Use this for most arguments, including arguments whose completion is already described by
    /// their parser metadata.
    None,

    /// Attach a raw vanilla suggestion provider id to the protocol command graph.
    ///
    /// The client handles these providers itself. The server will not receive suggestion requests
    /// for them. This is for vanilla providers such as `minecraft:available_sounds`, not for
    /// server/ECS-backed suggestions.
    Protocol(&'static str),

    /// Ask this server for dynamic suggestions.
    ///
    /// The derive macro sends `minecraft:ask_server` in the command graph and registers this arg
    /// type's [CommandArg::suggest] method (that has ECS access) as the handler. Use this if you
    /// want to send specific suggestions from the server, such as a list of online players or
    /// entities.
    Server,
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
/// Metadata generated for one command argument in the client command graph.
///
/// Each [CommandArg](CommandArg) returns an `ArgumentSpec` from
/// [CommandArg::argument_spec](CommandArg::argument_spec). The derive macro then uses that
/// spec when building command paths and protocol graph nodes.
///
/// In most custom args you will use one of:
///
/// ```rust
/// # use temper_command_infra::{ArgumentSpec, ParserKind};
/// ArgumentSpec::new(ParserKind::Word);
/// ArgumentSpec::entity(false, false);
/// ArgumentSpec::resource("minecraft:entity_type");
/// ```
///
/// Suggestions are usually filled in by the derive macro from
/// [CommandArg::SUGGESTIONS](CommandArg::SUGGESTIONS), so custom args should not normally
/// set `protocol_suggestions` or `server_suggestions` by hand.
pub struct ArgumentSpec {
    /// The basic parser type the client should use for this argument.
    pub parser: ParserKind,

    /// Optional parser-specific flags or metadata.
    pub properties: Option<ParserProperties>,

    /// Optional vanilla suggestion provider id sent to the client.
    ///
    /// This is what the protocol graph uses. For server-backed suggestions this will usually be
    /// `minecraft:ask_server`.
    pub protocol_suggestions: Option<&'static str>,

    /// Optional internal provider id used to route server-backed suggestions.
    ///
    /// This is not sent to the client.
    pub server_suggestions: Option<&'static str>,
}

impl ArgumentSpec {
    /// Create an argument spec with only a basic parser type.
    pub const fn new(parser: ParserKind) -> Self {
        Self {
            parser,
            properties: None,
            protocol_suggestions: None,
            server_suggestions: None,
        }
    }

    /// Create an argument spec with parser-specific properties.
    pub const fn with_properties(parser: ParserKind, properties: ParserProperties) -> ArgumentSpec {
        Self {
            parser,
            properties: Some(properties),
            protocol_suggestions: None,
            server_suggestions: None,
        }
    }

    /// Set the vanilla protocol suggestion provider.
    ///
    /// Prefer using [SuggestionProviderKind::Protocol] on the arg type unless manually constructing
    /// command paths.
    pub const fn with_suggestions(mut self, suggestions: &'static str) -> ArgumentSpec {
        self.protocol_suggestions = Some(suggestions);
        self
    }

    /// Set the vanilla protocol suggestion provider.
    ///
    /// Prefer using [SuggestionProviderKind::Protocol] on the arg type unless manually constructing
    /// command paths.
    pub const fn with_protocol_suggestions(mut self, suggestions: &'static str) -> ArgumentSpec {
        self.protocol_suggestions = Some(suggestions);
        self
    }

    /// Set the internal server suggestion provider id.
    ///
    /// Prefer using [SuggestionProviderKind::Server] on the arg type unless manually constructing
    /// command paths.
    pub const fn with_server_suggestions(mut self, suggestions: &'static str) -> ArgumentSpec {
        self.server_suggestions = Some(suggestions);
        self
    }

    /// Create an entity argument spec with selector flags.
    pub const fn entity(single: bool, players_only: bool) -> ArgumentSpec {
        Self::with_properties(
            ParserKind::Entity,
            ParserProperties::Entity(EntityProperties {
                single,
                players_only,
            }),
        )
    }

    /// Create a resource argument spec for a vanilla registry.
    ///
    /// This is the normal way to get registry-backed completion, such as entity type suggestions
    /// for `minecraft:entity_type`.
    pub const fn resource(registry: &'static str) -> ArgumentSpec {
        Self::with_properties(
            ParserKind::Resource,
            ParserProperties::Resource(ResourceProperties { registry }),
        )
    }
}

pub trait CommandArg: Sized {
    type Raw<'a>;

    const KIND: ArgKind = ArgKind::Normal;
    const SUGGESTIONS: SuggestionProviderKind;

    fn recognize<'a>(reader: &mut CommandReader<'a>) -> Result<Self::Raw<'a>, ParseError>;

    fn parse(raw: Self::Raw<'_>) -> Result<Self, ParseError>;

    fn argument_spec() -> ArgumentSpec;

    fn suggest(_input: SuggestionInput<'_>, _world: &mut World) -> Vec<String> {
        Vec::new()
    }
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub enum CommandPathSegment {
    Literal {
        name: &'static str,
        permission: Option<Permissions>,
    },
    Argument {
        name: &'static str,
        spec: ArgumentSpec,
        permission: Option<Permissions>,
    },
}

impl CommandPathSegment {
    pub const fn literal(name: &'static str) -> Self {
        Self::Literal {
            name,
            permission: None,
        }
    }

    pub const fn argument(name: &'static str, spec: ArgumentSpec) -> Self {
        Self::Argument {
            name,
            spec,
            permission: None,
        }
    }

    pub const fn with_permission(mut self, permission: Permissions) -> Self {
        match &mut self {
            Self::Literal {
                permission: segment_permission,
                ..
            }
            | Self::Argument {
                permission: segment_permission,
                ..
            } => *segment_permission = Some(permission),
        }
        self
    }

    pub const fn permission(&self) -> Option<Permissions> {
        match self {
            Self::Literal { permission, .. } | Self::Argument { permission, .. } => *permission,
        }
    }
}

#[derive(Clone, Debug, Eq, PartialEq)]
pub struct CommandPath {
    pub root: &'static str,
    pub permission: Option<Permissions>,
    pub segments: Vec<CommandPathSegment>,
}

impl CommandPath {
    pub fn new(root: &'static str, segments: Vec<CommandPathSegment>) -> Self {
        Self {
            root,
            permission: None,
            segments,
        }
    }

    pub fn with_permission(mut self, permission: Option<Permissions>) -> Self {
        self.permission = permission;
        self
    }

    pub fn with_root(mut self, root: &'static str) -> Self {
        self.root = root;
        self
    }

    pub fn is_allowed_by(&self, can_use: impl Fn(Permissions) -> bool) -> bool {
        self.permission.is_none_or(&can_use)
            && self
                .segments
                .iter()
                .all(|segment| segment.permission().is_none_or(&can_use))
    }
}

pub trait CommandSpec: Sized {
    const NAME: &'static str;

    fn aliases() -> &'static [&'static str] {
        &[]
    }

    fn permission() -> Option<Permissions> {
        None
    }

    fn parse_reader(reader: &mut CommandReader<'_>) -> Result<Self, ParseError>;

    fn parse_reader_with_permissions(
        reader: &mut CommandReader<'_>,
        _can_use: &dyn Fn(Permissions) -> bool,
    ) -> Result<Self, ParseError> {
        Self::parse_reader(reader)
    }

    fn paths() -> Vec<CommandPath>;

    fn parse(input: &str) -> Result<Self, ParseError> {
        let mut reader = CommandReader::new(input);
        Self::parse_reader(&mut reader)
    }
}

pub trait SubcommandSpec: Sized {
    fn parse_reader(reader: &mut CommandReader<'_>) -> Result<Self, ParseError>;

    fn parse_reader_with_permissions(
        reader: &mut CommandReader<'_>,
        _can_use: &dyn Fn(Permissions) -> bool,
    ) -> Result<Self, ParseError> {
        Self::parse_reader(reader)
    }

    fn segments() -> Vec<Vec<CommandPathSegment>>;
}
