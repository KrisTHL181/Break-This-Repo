use bevy_ecs::change_detection::Res;
use bevy_ecs::message::MessageWriter;
use std::sync::Arc;
use temper_command_infra::{CommandDispatched, CommandSource};
use temper_resources::server_command_rx::ServerCommandReceiver;

pub fn handle(
    receiver: Res<ServerCommandReceiver>,
    mut dispatch_msgs: MessageWriter<CommandDispatched>,
) {
    for command in receiver.0.try_iter() {
        dispatch_msgs.write(CommandDispatched {
            input: Arc::from(command),
            source: CommandSource::Server,
        });
    }
}

#[cfg(test)]
mod tests {
    use bevy_ecs::message::MessageRegistry;
    use bevy_ecs::prelude::{Messages, Schedule, World};
    use std::sync::Arc;
    use temper_command_infra::{CommandDispatched, CommandSource};
    use temper_resources::server_command_rx::ServerCommandReceiver;

    use super::handle;

    #[test]
    fn tui_server_commands_emit_dispatch_messages() {
        let (sender, receiver) = crossbeam_channel::unbounded();
        sender.send("stop".to_string()).unwrap();

        let mut world = World::new();
        MessageRegistry::register_message::<CommandDispatched>(&mut world);
        world.insert_resource(ServerCommandReceiver(receiver));

        let mut schedule = Schedule::default();
        schedule.add_systems(handle);
        schedule.run(&mut world);

        let message_resource = world.resource::<Messages<CommandDispatched>>();
        let mut cursor = message_resource.get_cursor();
        let messages = cursor.read(message_resource).cloned().collect::<Vec<_>>();

        assert_eq!(messages.len(), 1);
        assert_eq!(messages[0].input, Arc::from("stop"));
        assert_eq!(messages[0].source, CommandSource::Server);
    }
}
