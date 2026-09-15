use std::sync::{LazyLock, RwLock};
use std::{cell::RefCell, sync::Arc};

use bevy_ecs::prelude::{
    Component, Entity, IntoScheduleConfigs, Message, MessageReader, Query, Resource, Schedule,
};
use bevy_ecs::schedule::ScheduleConfigs;
use bevy_ecs::system::{ParamSet, ScheduleSystem, SystemParam, SystemParamItem};
use temper_core::mq;
use temper_permissions::Permissions;
use temper_permissions::player::PlayerPermission;
use temper_text::{NamedColor, TextComponent, TextComponentBuilder};
use tracing::info;

use crate::{CommandGraph, CommandPath, CommandSpec, ParseError};

static STATIC_COMMANDS: LazyLock<RwLock<Vec<RegisteredCommand>>> =
    LazyLock::new(|| RwLock::new(Vec::new()));

thread_local! {
    static SYSTEMS_TO_BE_REGISTERED: RefCell<Vec<ScheduleConfigs<ScheduleSystem>>> = RefCell::new(Vec::new());
}

#[derive(Clone, Debug)]
pub struct RegisteredCommand {
    pub name: &'static str,
    pub aliases: &'static [&'static str],
    pub permission: Option<Permissions>,
    pub paths: Vec<CommandPath>,
}

impl RegisteredCommand {
    pub fn of<C: CommandSpec>() -> Self {
        let primary_paths = C::paths()
            .into_iter()
            .map(|path| path.with_permission(C::permission()));
        let alias_paths = C::aliases().iter().flat_map(|alias| {
            let alias = *alias;
            C::paths()
                .into_iter()
                .map(move |path| path.with_root(alias).with_permission(C::permission()))
        });

        Self {
            name: C::NAME,
            aliases: C::aliases(),
            permission: C::permission(),
            paths: primary_paths.chain(alias_paths).collect(),
        }
    }

    pub fn matches_root(&self, root: &str) -> bool {
        self.name == root || self.aliases.contains(&root)
    }
}

pub fn register_static_command(command: RegisteredCommand) {
    if let Ok(mut commands) = STATIC_COMMANDS.write() {
        commands.push(command);
    }
}

pub fn static_commands() -> Vec<RegisteredCommand> {
    STATIC_COMMANDS
        .read()
        .map(|commands| commands.clone())
        .unwrap_or_default()
}

pub fn add_system<M>(system: impl IntoScheduleConfigs<ScheduleSystem, M>) {
    SYSTEMS_TO_BE_REGISTERED.with(|systems| {
        systems.borrow_mut().push(system.into_configs());
    });
}

pub fn register_command_systems(schedule: &mut Schedule) {
    SYSTEMS_TO_BE_REGISTERED.with(|systems| {
        let mut systems = systems.borrow_mut();
        while let Some(system) = systems.pop() {
            schedule.add_systems(system);
        }
    });
}

pub trait CommandHandler: CommandSpec + Sized + Send + Sync + 'static {
    type SystemParam<'w, 's>: SystemParam;

    /// Execute a parsed command.
    ///
    /// Returning an error sends that error to the command source automatically.
    fn handle(
        self,
        source: CommandSource,
        params: &mut SystemParamItem<'_, '_, Self::SystemParam<'_, '_>>,
    ) -> CommandResult;

    fn handle_parse_error(
        source: CommandSource,
        error: ParseError,
        _params: &mut SystemParamItem<'_, '_, Self::SystemParam<'_, '_>>,
    ) {
        send_parse_error(source, &error);
    }
}

/// Result returned by command handlers.
///
/// `Ok(())` means the command handled its own success output. `Err(error)` means the dispatcher
/// should send the error message to the command source.
pub type CommandResult = Result<(), CommandError>;

/// User-facing command failure.
#[derive(Clone, Debug)]
pub struct CommandError {
    message: Box<TextComponent>,
}

impl CommandError {
    /// Create a command error from a chat component or plain string.
    pub fn new(message: impl Into<TextComponent>) -> Self {
        Self {
            message: Box::new(message.into()),
        }
    }

    /// Borrow the message that will be sent to the command source.
    pub fn message(&self) -> &TextComponent {
        &self.message
    }

    /// Consume the error and return the message to send.
    pub fn into_message(self) -> TextComponent {
        *self.message
    }
}

impl From<TextComponent> for CommandError {
    fn from(message: TextComponent) -> Self {
        Self::new(message)
    }
}

impl From<String> for CommandError {
    fn from(message: String) -> Self {
        Self::new(message)
    }
}

impl From<&str> for CommandError {
    fn from(message: &str) -> Self {
        Self::new(message)
    }
}

pub fn send_parse_error(source: CommandSource, error: &ParseError) {
    let message = TextComponentBuilder::new(format!("failed parsing command: {}", error.message))
        .color(NamedColor::Red)
        .build();

    source.send_message(message);
}

pub fn send_command_error(source: CommandSource, error: CommandError) {
    source.send_message(error.into_message());
}

pub fn dispatch_command<C: CommandHandler>(
    mut commands: MessageReader<CommandDispatched>,
    mut params: ParamSet<(Query<&PlayerPermission>, C::SystemParam<'_, '_>)>,
) {
    for event in commands.read() {
        let input = &event.input;
        let Some(root) = input.split_whitespace().next() else {
            continue;
        };

        if root != C::NAME && !C::aliases().contains(&root) {
            continue;
        }

        let parse_result = {
            let permissions = params.p0();
            let can_use = |permission| {
                let source = event.source;
                match source {
                    CommandSource::Server => true,
                    CommandSource::Player(entity) => permissions
                        .get(entity)
                        .is_ok_and(|player_permissions| player_permissions.can(permission)),
                }
            };
            if let Some(permission) = C::permission()
                && !can_use(permission)
            {
                let source = event.source;
                let message =
                    TextComponentBuilder::new("You don't have permission to use this command.")
                        .color(NamedColor::Red)
                        .build();
                source.send_message(message);
                continue;
            }

            let input1 = &event.input;
            let input = input1.strip_prefix(root).unwrap_or(input1).trim_start();
            let mut reader = crate::CommandReader::new(input);
            C::parse_reader_with_permissions(&mut reader, &can_use)
        };

        match parse_result {
            Ok(command) => {
                let mut command_params = params.p1();
                if let Err(error) = command.handle(event.source, &mut command_params) {
                    send_command_error(event.source, error);
                }
            }
            Err(error) => {
                let mut command_params = params.p1();
                C::handle_parse_error(event.source, error, &mut command_params);
            }
        }
    }
}

#[derive(Default, Resource)]
pub struct CommandRegistry {
    commands: Vec<RegisteredCommand>,
}

impl CommandRegistry {
    pub fn from_static_commands() -> Self {
        Self {
            commands: static_commands(),
        }
    }

    pub fn register<C: CommandSpec>(&mut self) {
        self.commands.push(RegisteredCommand::of::<C>());
    }

    pub fn register_command(&mut self, command: RegisteredCommand) {
        self.commands.push(command);
    }

    pub fn commands(&self) -> &[RegisteredCommand] {
        &self.commands
    }

    pub fn owns_input(&self, input: &str) -> bool {
        input.split_whitespace().next().is_some_and(|input_root| {
            self.commands
                .iter()
                .any(|command| command.matches_root(input_root))
        })
    }

    pub fn paths_for_player(&self, _player: Entity) -> Vec<CommandPath> {
        self.paths_for_permissions(|_| true)
    }

    pub fn paths_for_player_permissions(
        &self,
        _player: Entity,
        permissions: Option<&PlayerPermission>,
    ) -> Vec<CommandPath> {
        self.paths_for_permissions(|permission| permissions.is_some_and(|p| p.can(permission)))
    }

    pub fn paths_for_permissions(&self, can_use: impl Fn(Permissions) -> bool) -> Vec<CommandPath> {
        self.commands
            .iter()
            .flat_map(|command| {
                command
                    .paths
                    .iter()
                    .filter(|path| path.is_allowed_by(&can_use))
                    .cloned()
            })
            .collect()
    }

    pub fn build_graph_for_player(&self, player: Entity) -> CommandGraph {
        CommandGraph::from_paths(&self.paths_for_player(player))
    }
}

#[derive(Clone, Copy, Debug, Eq, PartialEq)]
pub enum CommandSource {
    Player(Entity),
    Server,
}

impl CommandSource {
    pub fn send_message(self, message: TextComponent) {
        match self {
            CommandSource::Player(entity) => mq::queue(message, false, entity),
            CommandSource::Server => {
                info!("{}", message.to_plain_text())
            }
        }
    }
}

#[derive(Message, Clone, Debug)]
pub struct CommandDispatched {
    pub input: Arc<str>,
    pub source: CommandSource,
}

#[derive(Component, Clone, Debug, Eq, PartialEq)]
pub struct PlayerCommandGraph {
    pub graph: CommandGraph,
    pub version: u64,
}

impl PlayerCommandGraph {
    pub fn new(graph: CommandGraph) -> Self {
        Self { graph, version: 0 }
    }

    pub fn next(graph: CommandGraph, previous: Option<&PlayerCommandGraph>) -> Self {
        Self {
            graph,
            version: previous.map(|graph| graph.version + 1).unwrap_or(0),
        }
    }
}

#[derive(Message, Clone, Copy, Debug, Eq, PartialEq)]
pub struct RebuildCommandGraph {
    pub player: Entity,
}
