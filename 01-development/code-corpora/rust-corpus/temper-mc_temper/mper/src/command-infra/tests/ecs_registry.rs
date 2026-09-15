use bevy_ecs::entity::Entity;
use bevy_ecs::message::MessageRegistry;
use bevy_ecs::prelude::{IntoScheduleConfigs, MessageWriter, ResMut, Resource, Schedule, World};
use std::sync::Arc;
use temper_command_infra::args::{EntityArg, PositionArg, SingleWordArg};
use temper_command_infra::{
    CommandDispatched, CommandHandler, CommandRegistry, CommandResult, CommandSource, CommandSpec,
    ParseError, dispatch_command, static_commands,
};
use temper_macros::Command;

#[derive(Debug, PartialEq, Command)]
#[command("tp")]
enum TpCommand {
    TpToPos { location: PositionArg },
    TpToEntity { destination: EntityArg },
}

impl CommandHandler for TpCommand {
    type SystemParam<'w, 's> = ();

    fn handle<'w, 's>(
        self,
        _source: CommandSource,
        _params: &mut Self::SystemParam<'w, 's>,
    ) -> CommandResult {
        Ok(())
    }
}

#[derive(Debug, PartialEq, Command)]
#[command("demo")]
enum DemoCommand {
    Word { value: SingleWordArg },
}

#[derive(Default, Resource)]
struct HandledCommands {
    handled: usize,
    parse_errors: usize,
    last_source: Option<CommandSource>,
    last_value: Option<String>,
}

impl CommandHandler for DemoCommand {
    type SystemParam<'w, 's> = ResMut<'w, HandledCommands>;

    fn handle<'w, 's>(
        self,
        source: CommandSource,
        params: &mut Self::SystemParam<'w, 's>,
    ) -> CommandResult {
        let DemoCommand::Word { value } = self;

        params.handled += 1;
        params.last_source = Some(source);
        params.last_value = Some(value.to_string());

        Ok(())
    }

    fn handle_parse_error<'w, 's>(
        _source: CommandSource,
        _error: ParseError,
        params: &mut Self::SystemParam<'w, 's>,
    ) {
        params.parse_errors += 1;
    }
}

fn emit_demo_commands(mut writer: MessageWriter<CommandDispatched>) {
    writer.write(CommandDispatched {
        input: Arc::from("demo hello"),
        source: CommandSource::Player(Entity::PLACEHOLDER),
    });
    writer.write(CommandDispatched {
        input: Arc::from("demo"),
        source: CommandSource::Player(Entity::PLACEHOLDER),
    });
    writer.write(CommandDispatched {
        input: Arc::from("other hello"),
        source: CommandSource::Player(Entity::PLACEHOLDER),
    });
}

#[test]
fn registry_builds_graph_from_registered_commands() {
    let mut registry = CommandRegistry::default();
    registry.register::<TpCommand>();

    let graph = registry.build_graph_for_player(Entity::PLACEHOLDER);
    let root = &graph.nodes[graph.root_idx];
    let tp_idx = root.children[0];
    let tp = &graph.nodes[tp_idx];

    assert_eq!(registry.commands().len(), 1);
    assert_eq!(tp.name.as_deref(), Some("tp"));
    assert_eq!(tp.children.len(), 2);
    assert!(
        tp.children
            .iter()
            .all(|child| graph.nodes[*child].executable)
    );
}

#[test]
fn dispatch_command_calls_handler_trait_methods() {
    let mut world = World::new();
    MessageRegistry::register_message::<CommandDispatched>(&mut world);
    world.init_resource::<HandledCommands>();

    let mut schedule = Schedule::default();
    schedule.add_systems((emit_demo_commands, dispatch_command::<DemoCommand>).chain());
    schedule.run(&mut world);

    let handled = world.resource::<HandledCommands>();

    assert_eq!(handled.handled, 1);
    assert_eq!(handled.parse_errors, 1);
    assert_eq!(
        handled.last_source,
        Some(CommandSource::Player(Entity::PLACEHOLDER))
    );
    assert_eq!(handled.last_value.as_deref(), Some("hello"));
}

#[test]
fn derived_commands_register_static_metadata() {
    let commands = static_commands();

    assert!(
        commands
            .iter()
            .any(|command| command.name == TpCommand::NAME),
        "derived command was not registered in static command metadata"
    );
}
