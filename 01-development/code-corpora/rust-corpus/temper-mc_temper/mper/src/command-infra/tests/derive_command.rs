use bevy_ecs::prelude::{Entity, Resource, World};
use temper_command_infra::args::{
    EntitiesArg, EntityArg, GreedyStringArg, IntegerArg, PlayerArg, PlayersArg, PositionArg,
    SingleWordArg,
};
use temper_command_infra::{
    ArgumentSpec, CommandArg, CommandGraph, CommandHandler, CommandNodeKind, CommandReader,
    CommandResult, CommandSource, CommandSpec, ParseError, ParserKind, ParserProperties,
    Permissions, SuggestionInput, SuggestionProviderKind,
};
use temper_macros::Command;

#[derive(Debug, PartialEq, Command)]
#[command("tp")]
enum TpCommand {
    ToPos {
        location: PositionArg,
    },
    ToEntity {
        destination: EntityArg,
    },
    EntityToPos {
        target: EntitiesArg,
        location: PositionArg,
    },
    EntityToEntity {
        target: EntitiesArg,
        destination: EntityArg,
    },
}

#[derive(Debug, PartialEq, Command)]
#[command("entity-flags")]
enum EntityFlagsCommand {
    Entity { target: EntityArg },
    Entities { targets: EntitiesArg },
    Player { player: PlayerArg },
    Players { players: PlayersArg },
}

#[derive(Debug, PartialEq, Command)]
#[command("overlap")]
enum OverlapCommand {
    Word { value: SingleWordArg },
    Entity { target: EntityArg },
}

#[derive(Debug, PartialEq, Command)]
#[command("say")]
enum SayCommand {
    Say { message: GreedyStringArg },
}

#[derive(Debug, PartialEq, Command)]
#[command("number")]
enum NumberCommand {
    Number { value: IntegerArg<0, 10> },
}

#[derive(Debug, PartialEq, Command)]
#[command("rename")]
enum RenameCommand {
    Rename {
        #[arg("display_name")]
        name: SingleWordArg,
    },
}

#[derive(Debug, PartialEq, Command)]
#[command("stop")]
struct StopCommand;

#[derive(Debug, PartialEq, Command)]
#[command("me")]
struct MeCommand {
    action: GreedyStringArg,
}

#[derive(Debug, PartialEq, Command)]
#[command(name = "time")]
enum TimeCommand {
    #[subcommand("set", aliases = ["s"])]
    #[permission(Permissions::Op)]
    Set(SetTimeCommand),
    #[literal("add")]
    Add {
        #[permission(Permissions::Kill)]
        amount: IntegerArg<0, 24000>,
    },
}

#[derive(Debug, PartialEq, Command)]
#[command(subcommand)]
enum SetTimeCommand {
    #[literal("day", aliases = ["d"])]
    #[permission(Permissions::DeOp)]
    Day,
    #[literal("night")]
    Night,
    Ticks {
        value: IntegerArg<0, 24000>,
    },
}

#[derive(Debug, PartialEq, Command)]
#[command(
    name = "alias-demo",
    aliases = ["ad", "demoalias"],
    permission = Permissions::Teleport
)]
struct AliasCommand;

#[derive(Debug, PartialEq, Command)]
#[command("suggested")]
struct SuggestedCommand {
    value: SuggestedWordArg,
}

#[derive(Debug, PartialEq, Command)]
#[command("client-suggested")]
struct ClientSuggestedCommand {
    value: ClientSuggestedArg,
}

#[derive(Debug, PartialEq, Command)]
#[command("summon")]
struct SummonCommand {
    entity: ResourceArg,
}

#[derive(Clone, Debug, Eq, PartialEq)]
struct SuggestedWordArg(String);

#[derive(Clone, Debug, Eq, PartialEq)]
struct ClientSuggestedArg(String);

#[derive(Clone, Debug, Eq, PartialEq)]
struct ResourceArg(String);

#[derive(Resource)]
struct SuggestedWords(Vec<String>);

impl CommandArg for SuggestedWordArg {
    type Raw<'a> = &'a str;

    const SUGGESTIONS: SuggestionProviderKind = SuggestionProviderKind::Server;

    fn recognize<'a>(reader: &mut CommandReader<'a>) -> Result<Self::Raw<'a>, ParseError> {
        reader.read_word_span()
    }

    fn parse(raw: Self::Raw<'_>) -> Result<Self, ParseError> {
        Ok(Self(raw.to_string()))
    }

    fn argument_spec() -> ArgumentSpec {
        ArgumentSpec::new(ParserKind::Word)
    }

    fn suggest(_input: SuggestionInput<'_>, world: &mut World) -> Vec<String> {
        world.resource::<SuggestedWords>().0.clone()
    }
}

impl CommandArg for ClientSuggestedArg {
    type Raw<'a> = &'a str;

    const SUGGESTIONS: SuggestionProviderKind =
        SuggestionProviderKind::Protocol("minecraft:available_sounds");

    fn recognize<'a>(reader: &mut CommandReader<'a>) -> Result<Self::Raw<'a>, ParseError> {
        reader.read_word_span()
    }

    fn parse(raw: Self::Raw<'_>) -> Result<Self, ParseError> {
        Ok(Self(raw.to_string()))
    }

    fn argument_spec() -> ArgumentSpec {
        ArgumentSpec::new(ParserKind::Word)
    }
}

impl CommandArg for ResourceArg {
    type Raw<'a> = &'a str;

    const SUGGESTIONS: SuggestionProviderKind = SuggestionProviderKind::None;

    fn recognize<'a>(reader: &mut CommandReader<'a>) -> Result<Self::Raw<'a>, ParseError> {
        reader.read_word_span()
    }

    fn parse(raw: Self::Raw<'_>) -> Result<Self, ParseError> {
        Ok(Self(raw.to_string()))
    }

    fn argument_spec() -> ArgumentSpec {
        ArgumentSpec::resource("minecraft:entity_type")
    }
}

macro_rules! impl_noop_handler {
    ($($command:ty),* $(,)?) => {
        $(
            impl CommandHandler for $command {
                type SystemParam<'w, 's> = ();

                fn handle<'w, 's>(
                    self,
                    _source: CommandSource,
                    _params: &mut Self::SystemParam<'w, 's>,
                ) -> CommandResult {
                    Ok(())
                }
            }
        )*
    };
}

impl_noop_handler!(
    TpCommand,
    OverlapCommand,
    SayCommand,
    NumberCommand,
    RenameCommand,
    StopCommand,
    MeCommand,
    TimeCommand,
    AliasCommand,
    EntityFlagsCommand,
    SuggestedCommand,
    ClientSuggestedCommand,
    SummonCommand,
);

#[test]
fn tp_to_position_parses() {
    let command = TpCommand::parse("~ ~ ~").unwrap();

    assert!(matches!(command, TpCommand::ToPos { .. }));
}

#[test]
fn tp_to_entity_parses() {
    let command = TpCommand::parse("Steve").unwrap();

    match command {
        TpCommand::ToEntity { destination } => assert_eq!(&*destination, "Steve"),
        _ => panic!("expected entity destination"),
    }
}

#[test]
fn tp_entity_to_position_parses() {
    let command = TpCommand::parse("Steve ~ ~ ~").unwrap();

    match command {
        TpCommand::EntityToPos { target, location } => {
            assert_eq!(&*target, "Steve");
            assert_eq!(location.x, "~");
            assert_eq!(location.y, "~");
            assert_eq!(location.z, "~");
        }
        _ => panic!("expected entity to position"),
    }
}

#[test]
fn tp_entity_to_entity_parses() {
    let command = TpCommand::parse("Steve Alex").unwrap();

    match command {
        TpCommand::EntityToEntity {
            target,
            destination,
        } => {
            assert_eq!(&*target, "Steve");
            assert_eq!(&*destination, "Alex");
        }
        _ => panic!("expected entity to entity"),
    }
}

#[test]
fn failed_variants_rewind_cleanly() {
    let command = TpCommand::parse("Steve 1 2 3").unwrap();

    assert!(matches!(command, TpCommand::EntityToPos { .. }));
}

#[test]
fn variant_order_breaks_ties() {
    let command = OverlapCommand::parse("Steve").unwrap();

    assert!(matches!(command, OverlapCommand::Word { .. }));
}

#[test]
fn greedy_tail_variant_parses() {
    let command = SayCommand::parse("hello there").unwrap();

    match command {
        SayCommand::Say { message } => assert_eq!(&*message, "hello there"),
    }
}

#[test]
fn parse_errors_report_farthest_failure() {
    let err = NumberCommand::parse("20").unwrap_err();

    assert_eq!(err.cursor, 0);
    assert!(err.message.contains("too large"));
}

#[test]
fn graph_generation_merges_shared_prefixes() {
    let graph = CommandGraph::from_paths(&TpCommand::paths());

    let root = &graph.nodes[graph.root_idx];
    assert_eq!(root.children.len(), 1);

    let tp_idx = root.children[0];
    let tp = &graph.nodes[tp_idx];
    assert_eq!(tp.kind, CommandNodeKind::Literal);
    assert_eq!(tp.name.as_deref(), Some("tp"));

    let child_names = tp
        .children
        .iter()
        .map(|idx| graph.nodes[*idx].name.as_deref().unwrap())
        .collect::<Vec<_>>();

    assert_eq!(child_names, vec!["target", "location", "destination"]);

    let destination_idx = tp
        .children
        .iter()
        .copied()
        .find(|idx| graph.nodes[*idx].name.as_deref() == Some("destination"))
        .unwrap();
    let destination = &graph.nodes[destination_idx];

    assert!(destination.children.is_empty());
    assert!(destination.executable);

    let target_idx = tp
        .children
        .iter()
        .copied()
        .find(|idx| graph.nodes[*idx].name.as_deref() == Some("target"))
        .unwrap();
    let target = &graph.nodes[target_idx];

    assert!(!target.executable);
    assert_eq!(target.children.len(), 2);
    assert!(
        target
            .children
            .iter()
            .all(|idx| graph.nodes[*idx].executable)
    );
}

#[test]
fn graph_uses_arg_attribute_names() {
    let graph = CommandGraph::from_paths(&NumberCommand::paths());
    let number_idx = graph.nodes[graph.root_idx].children[0];
    let value_idx = graph.nodes[number_idx].children[0];

    assert_eq!(graph.nodes[number_idx].name.as_deref(), Some("number"));
    assert_eq!(graph.nodes[value_idx].name.as_deref(), Some("value"));
    assert!(graph.nodes[value_idx].executable);
}

#[test]
fn entity_arg_types_generate_entity_flags() {
    let paths = EntityFlagsCommand::paths();

    assert_entity_flags(&paths, "target", true, false);
    assert_entity_flags(&paths, "targets", false, false);
    assert_entity_flags(&paths, "player", true, true);
    assert_entity_flags(&paths, "players", false, true);
}

#[test]
fn arg_suggestions_are_registered_from_field_types() {
    let paths = SuggestedCommand::paths();
    let spec = paths
        .iter()
        .filter_map(|path| path.segments.first())
        .find_map(|segment| match segment {
            temper_command_infra::CommandPathSegment::Argument { spec, .. } => Some(*spec),
            _ => None,
        })
        .unwrap();
    let provider = temper_command_infra::command_arg_suggestion_id::<SuggestedWordArg>();

    assert_eq!(spec.protocol_suggestions, Some("minecraft:ask_server"));
    assert_eq!(spec.server_suggestions, Some(provider));

    let mut world = World::new();
    world.insert_resource(SuggestedWords(vec!["alpha".into(), "beta".into()]));
    temper_command_infra::register_command_arg_suggestions::<SuggestedWordArg>();

    let suggestions = temper_command_infra::suggest_command_arg(
        provider,
        &mut world,
        SuggestionInput {
            full_input: "/suggested a",
            current_token: "a",
            source: Entity::PLACEHOLDER,
        },
    )
    .unwrap();

    assert_eq!(suggestions, vec!["alpha", "beta"]);
}

#[test]
fn position_args_use_client_parser_suggestions() {
    let paths = TpCommand::paths();
    let spec = paths
        .iter()
        .flat_map(|path| path.segments.iter())
        .find_map(|segment| match segment {
            temper_command_infra::CommandPathSegment::Argument {
                name: "location",
                spec,
                ..
            } => Some(*spec),
            _ => None,
        })
        .unwrap();

    assert_eq!(spec.parser, ParserKind::Position);
    assert_eq!(spec.protocol_suggestions, None);
    assert_eq!(spec.server_suggestions, None);
}

#[test]
fn resource_args_generate_registry_parser_metadata() {
    let paths = SummonCommand::paths();
    let spec = paths
        .iter()
        .filter_map(|path| path.segments.first())
        .find_map(|segment| match segment {
            temper_command_infra::CommandPathSegment::Argument { spec, .. } => Some(*spec),
            _ => None,
        })
        .unwrap();

    assert_eq!(spec.parser, ParserKind::Resource);
    assert_eq!(
        spec.properties,
        Some(ParserProperties::Resource(
            temper_command_infra::ResourceProperties {
                registry: "minecraft:entity_type",
            }
        ))
    );
    assert_eq!(spec.protocol_suggestions, None);
    assert_eq!(spec.server_suggestions, None);
}

#[test]
fn client_suggestions_only_set_protocol_provider() {
    let paths = ClientSuggestedCommand::paths();
    let spec = paths
        .iter()
        .filter_map(|path| path.segments.first())
        .find_map(|segment| match segment {
            temper_command_infra::CommandPathSegment::Argument { spec, .. } => Some(*spec),
            _ => None,
        })
        .unwrap();

    assert_eq!(
        spec.protocol_suggestions,
        Some("minecraft:available_sounds")
    );
    assert_eq!(spec.server_suggestions, None);
}

#[test]
fn arg_attribute_overrides_named_field_name() {
    let graph = CommandGraph::from_paths(&RenameCommand::paths());
    let rename_idx = graph.nodes[graph.root_idx].children[0];
    let name_idx = graph.nodes[rename_idx].children[0];

    assert_eq!(graph.nodes[name_idx].name.as_deref(), Some("display_name"));
}

#[test]
fn unit_struct_command_parses_without_args() {
    let command = StopCommand::parse("").unwrap();

    assert_eq!(command, StopCommand);
    assert!(StopCommand::parse("extra").is_err());
}

#[test]
fn unit_struct_command_graph_executable_at_root_literal() {
    let graph = CommandGraph::from_paths(&StopCommand::paths());
    let stop_idx = graph.nodes[graph.root_idx].children[0];
    let stop = &graph.nodes[stop_idx];

    assert_eq!(stop.name.as_deref(), Some("stop"));
    assert!(stop.children.is_empty());
    assert!(stop.executable);
}

#[test]
fn named_struct_command_parses_single_arg_set() {
    let command = MeCommand::parse("waves hello").unwrap();

    assert_eq!(&*command.action, "waves hello");
}

#[test]
fn named_struct_command_uses_field_names_in_graph() {
    let graph = CommandGraph::from_paths(&MeCommand::paths());
    let me_idx = graph.nodes[graph.root_idx].children[0];
    let action_idx = graph.nodes[me_idx].children[0];
    let action = &graph.nodes[action_idx];

    assert_eq!(graph.nodes[me_idx].name.as_deref(), Some("me"));
    assert_eq!(action.name.as_deref(), Some("action"));
    assert!(action.executable);
}

#[test]
fn nested_subcommand_literal_parses() {
    let command = TimeCommand::parse("set day").unwrap();

    assert!(matches!(command, TimeCommand::Set(SetTimeCommand::Day)));
}

#[test]
fn nested_subcommand_alias_parses() {
    let command = TimeCommand::parse("s night").unwrap();

    assert!(matches!(command, TimeCommand::Set(SetTimeCommand::Night)));
}

#[test]
fn nested_literal_alias_parses() {
    let command = TimeCommand::parse("set d").unwrap();

    assert!(matches!(command, TimeCommand::Set(SetTimeCommand::Day)));
}

#[test]
fn nested_subcommand_arg_parses() {
    let command = TimeCommand::parse("set 1200").unwrap();

    assert!(matches!(
        command,
        TimeCommand::Set(SetTimeCommand::Ticks { .. })
    ));
}

#[test]
fn literal_variant_with_args_parses() {
    let command = TimeCommand::parse("add 20").unwrap();

    assert!(matches!(command, TimeCommand::Add { .. }));
}

#[test]
fn nested_subcommands_generate_literal_graph_paths() {
    let graph = CommandGraph::from_paths(&TimeCommand::paths());
    let time_idx = graph.nodes[graph.root_idx].children[0];
    let time = &graph.nodes[time_idx];

    let time_children = time
        .children
        .iter()
        .map(|idx| graph.nodes[*idx].name.as_deref().unwrap())
        .collect::<Vec<_>>();

    assert_eq!(time_children, vec!["set", "s", "add"]);

    let set_idx = time
        .children
        .iter()
        .copied()
        .find(|idx| graph.nodes[*idx].name.as_deref() == Some("set"))
        .unwrap();
    let set = &graph.nodes[set_idx];
    let set_children = set
        .children
        .iter()
        .map(|idx| graph.nodes[*idx].name.as_deref().unwrap())
        .collect::<Vec<_>>();

    assert_eq!(set_children, vec!["day", "d", "night", "value"]);
    assert!(set.children.iter().all(|idx| graph.nodes[*idx].executable));

    let s_idx = time
        .children
        .iter()
        .copied()
        .find(|idx| graph.nodes[*idx].name.as_deref() == Some("s"))
        .unwrap();
    let s = &graph.nodes[s_idx];
    let s_children = s
        .children
        .iter()
        .map(|idx| graph.nodes[*idx].name.as_deref().unwrap())
        .collect::<Vec<_>>();

    assert_eq!(s_children, vec!["day", "d", "night", "value"]);
    assert!(s.children.iter().all(|idx| graph.nodes[*idx].executable));
}

#[test]
fn command_aliases_generate_extra_roots() {
    let roots = AliasCommand::paths()
        .iter()
        .map(|path| path.root)
        .collect::<Vec<_>>();

    assert_eq!(AliasCommand::aliases(), &["ad", "demoalias"]);
    assert_eq!(AliasCommand::permission(), Some(Permissions::Teleport));
    assert_eq!(roots, vec!["alias-demo"]);

    let registry = temper_command_infra::RegisteredCommand::of::<AliasCommand>();
    let registry_roots = registry
        .paths
        .iter()
        .map(|path| path.root)
        .collect::<Vec<_>>();

    assert_eq!(registry_roots, vec!["alias-demo", "ad", "demoalias"]);
}

#[test]
fn command_permission_blocks_permission_aware_parse() {
    let mut reader = CommandReader::new("");
    let err = AliasCommand::parse_reader_with_permissions(&mut reader, &|permission| {
        permission != Permissions::Teleport
    })
    .unwrap_err();

    assert_eq!(err.expected, "permission");
}

#[test]
fn subcommand_permission_blocks_permission_aware_parse() {
    let mut reader = CommandReader::new("set night");
    let err = TimeCommand::parse_reader_with_permissions(&mut reader, &|permission| {
        permission != Permissions::Op
    })
    .unwrap_err();

    assert_eq!(err.expected, "permission");
}

#[test]
fn literal_permission_blocks_permission_aware_parse() {
    let mut reader = CommandReader::new("set day");
    let err = TimeCommand::parse_reader_with_permissions(&mut reader, &|permission| {
        permission != Permissions::DeOp
    })
    .unwrap_err();

    assert_eq!(err.expected, "permission");
}

#[test]
fn arg_permission_blocks_permission_aware_parse() {
    let mut reader = CommandReader::new("add 20");
    let err = TimeCommand::parse_reader_with_permissions(&mut reader, &|permission| {
        permission != Permissions::Kill
    })
    .unwrap_err();

    assert_eq!(err.expected, "permission");
}

#[test]
fn permission_filter_removes_disallowed_paths() {
    let registry = temper_command_infra::RegisteredCommand::of::<TimeCommand>();
    let allowed_paths = registry
        .paths
        .iter()
        .filter(|path| path.is_allowed_by(|permission| permission != Permissions::Op))
        .map(|path| path.segments.first().unwrap())
        .collect::<Vec<_>>();

    assert_eq!(allowed_paths.len(), 1);
    assert!(matches!(
        allowed_paths[0],
        temper_command_infra::CommandPathSegment::Literal { name: "add", .. }
    ));
}

fn assert_entity_flags(
    paths: &[temper_command_infra::CommandPath],
    arg_name: &str,
    single: bool,
    players_only: bool,
) {
    let spec = paths
        .iter()
        .filter_map(|path| path.segments.first())
        .find_map(|segment| match segment {
            temper_command_infra::CommandPathSegment::Argument { name, spec, .. }
                if *name == arg_name =>
            {
                Some(*spec)
            }
            _ => None,
        })
        .unwrap();

    assert_eq!(
        spec.properties,
        Some(ParserProperties::Entity(
            temper_command_infra::EntityProperties {
                single,
                players_only
            }
        ))
    );
}
