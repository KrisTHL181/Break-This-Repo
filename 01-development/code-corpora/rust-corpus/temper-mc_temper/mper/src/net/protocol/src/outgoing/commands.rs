use std::{fmt, io::Write};

use enum_ordinalize::Ordinalize;
use temper_codec::encode::{NetEncode as NetEncodeTrait, NetEncodeOpts, errors::NetEncodeError};
use temper_codec::net_types::{length_prefixed_vec::LengthPrefixedVec, var_int::VarInt};
use temper_command_infra::{
    ArgumentSpec, CommandGraph as InfraCommandGraph, CommandNode as InfraCommandNode,
    CommandNodeKind as InfraCommandNodeKind, EntityProperties, IntegerProperties, ParserKind,
    ParserProperties, ResourceProperties, StringMode,
};
use temper_macros::{NetEncode, packet};

#[derive(Clone, Debug, PartialEq, NetEncode)]
pub enum PrimitiveArgumentFlags {
    Int(IntArgumentFlags),
    String(StringArgumentType),
    Entity(EntityArgumentFlags),
    Resource(String),
}

#[derive(Clone, Debug, PartialEq, Default)]
pub struct IntArgumentFlags {
    pub min: Option<i32>,
    pub max: Option<i32>,
}

impl NetEncodeTrait for IntArgumentFlags {
    fn encode<W: Write>(&self, writer: &mut W, opts: &NetEncodeOpts) -> Result<(), NetEncodeError> {
        let mut flags = 0u8;
        if self.min.is_some() {
            flags |= 0x01;
        }
        if self.max.is_some() {
            flags |= 0x02;
        }
        flags.encode(writer, opts)?;
        self.min.encode(writer, opts)?;
        self.max.encode(writer, opts)
    }
}

#[derive(Clone, Copy, Debug, Default, PartialEq)]
pub struct EntityArgumentFlags {
    pub single: bool,
    pub players_only: bool,
}

impl NetEncodeTrait for EntityArgumentFlags {
    fn encode<W: Write>(&self, writer: &mut W, opts: &NetEncodeOpts) -> Result<(), NetEncodeError> {
        let mut flags = 0u8;
        if self.single {
            flags |= 0x01;
        }
        if self.players_only {
            flags |= 0x02;
        }
        flags.encode(writer, opts)
    }
}

#[derive(Clone, Debug, PartialEq, Ordinalize, Default)]
pub enum StringArgumentType {
    #[default]
    Word,
    Quotable,
    Greedy,
}

impl NetEncodeTrait for StringArgumentType {
    fn encode<W: Write>(&self, writer: &mut W, opts: &NetEncodeOpts) -> Result<(), NetEncodeError> {
        VarInt::new(i32::from(self.ordinal())).encode(writer, opts)
    }
}

#[derive(Clone, Debug, PartialEq, Ordinalize)]
pub enum PrimitiveArgumentType {
    Bool,
    Float,
    Double,
    Int,
    Long,
    String,
    Entity,
    GameProfile,
    BlockPos,
    ColumnPos,
    Vec3,
    Vec2,
    BlockState,
    BlockPredicate,
    ItemStack,
    ItemPredicate,
    Color,
    Component,
    Style,
    Message,
    Nbt,
    NbtTag,
    NbtPath,
    Objective,
    ObjectiveCriteria,
    Operator,
    Particle,
    Angle,
    Rotation,
    ScoreboardDisplaySlot,
    ScoreHolder,
    UpTo3Axes,
    Team,
    ItemSlot,
    ResourceLocation,
    Function,
    EntityAnchor,
    IntRange,
    FloatRange,
    Dimension,
    GameMode,
    Time,
    ResourceOrTag,
    ResourceOrTagKey,
    Resource,
    ResourceKey,
    TemplateMirror,
    TemplateRotation,
    Heightmap,
    UUID,
    Position,
}

impl NetEncodeTrait for PrimitiveArgumentType {
    fn encode<W: Write>(&self, writer: &mut W, opts: &NetEncodeOpts) -> Result<(), NetEncodeError> {
        VarInt::new(i32::from(self.ordinal())).encode(writer, opts)
    }
}

#[derive(Clone, NetEncode)]
pub struct CommandNode {
    pub flags: u8,
    pub children: LengthPrefixedVec<VarInt>,
    pub redirect_node: Option<VarInt>,
    pub name: Option<String>,
    pub parser_id: Option<PrimitiveArgumentType>,
    pub properties: Option<PrimitiveArgumentFlags>,
    pub suggestions_type: Option<String>,
}

impl fmt::Debug for CommandNode {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("CommandNode")
            .field("node_type", &command_node_kind(self.flags))
            .field("executable", &(self.flags & 0x04 != 0))
            .field("has_redirect", &(self.flags & 0x08 != 0))
            .field("has_suggestions_type", &(self.flags & 0x10 != 0))
            .field("flags", &self.flags)
            .field("children", &self.children)
            .field("redirect_node", &self.redirect_node)
            .field("name", &self.name)
            .field("parser_id", &self.parser_id)
            .field("properties", &self.properties)
            .field("suggestions_type", &self.suggestions_type)
            .finish()
    }
}

fn command_node_kind(flags: u8) -> &'static str {
    match flags & 0x03 {
        0 => "Root",
        1 => "Literal",
        2 => "Argument",
        _ => "Invalid",
    }
}

#[derive(NetEncode, Debug)]
#[packet(packet_id = "commands", state = "play")]
pub struct CommandsPacket {
    pub graph: LengthPrefixedVec<CommandNode>,
    pub root_idx: VarInt,
}

impl CommandsPacket {
    pub fn from_command_infra_graph(graph: &InfraCommandGraph) -> Self {
        Self {
            graph: LengthPrefixedVec::new(graph.nodes.iter().map(convert_node).collect()),
            root_idx: VarInt::new(graph.root_idx as i32),
        }
    }
}

fn convert_node(node: &InfraCommandNode) -> CommandNode {
    let mut flags = match node.kind {
        InfraCommandNodeKind::Root => 0x00,
        InfraCommandNodeKind::Literal => 0x01,
        InfraCommandNodeKind::Argument => 0x02,
    };

    if node.executable {
        flags |= 0x04;
    }

    if node
        .argument
        .and_then(|arg| arg.protocol_suggestions)
        .is_some()
    {
        flags |= 0x10;
    }

    CommandNode {
        flags,
        children: LengthPrefixedVec::new(
            node.children
                .iter()
                .map(|child| VarInt::new(*child as i32))
                .collect(),
        ),
        redirect_node: None,
        name: node.name.clone(),
        parser_id: node.argument.map(parser_id),
        properties: node.argument.and_then(parser_properties),
        suggestions_type: node
            .argument
            .and_then(|argument| argument.protocol_suggestions)
            .map(str::to_string),
    }
}

fn parser_id(argument: ArgumentSpec) -> PrimitiveArgumentType {
    match argument.parser {
        ParserKind::Word | ParserKind::String => PrimitiveArgumentType::String,
        ParserKind::Integer => PrimitiveArgumentType::Int,
        ParserKind::Position => PrimitiveArgumentType::Vec3,
        ParserKind::Entity => PrimitiveArgumentType::Entity,
        ParserKind::Resource => PrimitiveArgumentType::Resource,
    }
}

fn parser_properties(argument: ArgumentSpec) -> Option<PrimitiveArgumentFlags> {
    match argument.properties {
        Some(ParserProperties::String(mode)) => {
            Some(PrimitiveArgumentFlags::String(string_mode(mode)))
        }
        Some(ParserProperties::Integer(IntegerProperties { min, max })) => {
            Some(PrimitiveArgumentFlags::Int(IntArgumentFlags { min, max }))
        }
        Some(ParserProperties::Entity(EntityProperties {
            single,
            players_only,
        })) => Some(PrimitiveArgumentFlags::Entity(EntityArgumentFlags {
            single,
            players_only,
        })),
        Some(ParserProperties::Resource(ResourceProperties { registry })) => {
            Some(PrimitiveArgumentFlags::Resource(registry.to_string()))
        }
        None if argument.parser == ParserKind::Word => {
            Some(PrimitiveArgumentFlags::String(StringArgumentType::Word))
        }
        None if argument.parser == ParserKind::Entity => Some(PrimitiveArgumentFlags::Entity(
            EntityArgumentFlags::default(),
        )),
        None => None,
    }
}

fn string_mode(mode: StringMode) -> StringArgumentType {
    match mode {
        StringMode::Word => StringArgumentType::Word,
        StringMode::Quotable => StringArgumentType::Quotable,
        StringMode::Greedy => StringArgumentType::Greedy,
    }
}

#[cfg(test)]
mod tests {
    use temper_command_infra::{ArgumentSpec, CommandGraph, CommandPath, CommandPathSegment};

    use super::{
        CommandsPacket, EntityArgumentFlags, PrimitiveArgumentFlags, PrimitiveArgumentType,
    };

    #[test]
    fn converts_command_infra_graph_to_protocol_nodes() {
        let graph = CommandGraph::from_paths(&[CommandPath::new(
            "tp",
            vec![CommandPathSegment::argument(
                "target",
                ArgumentSpec::entity(false, false)
                    .with_protocol_suggestions("minecraft:ask_server"),
            )],
        )]);

        let packet = CommandsPacket::from_command_infra_graph(&graph);

        assert_eq!(packet.root_idx.0, 0);
        assert_eq!(packet.graph.data.len(), 3);
        assert_eq!(packet.graph.data[1].name.as_deref(), Some("tp"));
        assert_eq!(packet.graph.data[2].name.as_deref(), Some("target"));
        assert!(packet.graph.data[2].flags & 0x04 != 0);
        assert!(matches!(
            packet.graph.data[2].properties,
            Some(PrimitiveArgumentFlags::Entity(_))
        ));
        assert_eq!(
            packet.graph.data[2].suggestions_type.as_deref(),
            Some("minecraft:ask_server")
        );
    }

    #[test]
    fn converts_entity_flags_to_protocol_properties() {
        let graph = CommandGraph::from_paths(&[CommandPath::new(
            "gamemode",
            vec![CommandPathSegment::argument(
                "target",
                ArgumentSpec::entity(true, true),
            )],
        )]);

        let packet = CommandsPacket::from_command_infra_graph(&graph);

        assert!(matches!(
            packet.graph.data[2].properties,
            Some(PrimitiveArgumentFlags::Entity(EntityArgumentFlags {
                single: true,
                players_only: true,
            }))
        ));
    }

    #[test]
    fn converts_resource_args_to_protocol_resource_parser() {
        let graph = CommandGraph::from_paths(&[CommandPath::new(
            "summon",
            vec![CommandPathSegment::argument(
                "entity",
                ArgumentSpec::resource("minecraft:entity_type"),
            )],
        )]);

        let packet = CommandsPacket::from_command_infra_graph(&graph);

        assert_eq!(
            packet.graph.data[2].parser_id,
            Some(PrimitiveArgumentType::Resource)
        );
        assert_eq!(
            packet.graph.data[2].properties,
            Some(PrimitiveArgumentFlags::Resource(
                "minecraft:entity_type".to_string()
            ))
        );
        assert_eq!(packet.graph.data[2].suggestions_type, None);
    }
}
