use bevy_ecs::prelude::*;
use std::sync::Arc;
use temper_command_infra::{CommandDispatched, CommandSource};
use temper_components::entity_identity::Identity;
use temper_protocol::ChatCommandPacketReceiver;
use tracing::info;

pub fn handle(
    receiver: Res<ChatCommandPacketReceiver>,
    mut dispatch_msgs: MessageWriter<CommandDispatched>,
    query: Query<&Identity>,
) {
    for (event, entity) in receiver.0.try_iter() {
        dispatch_msgs.write(CommandDispatched {
            input: Arc::from(event.command.clone()),
            source: CommandSource::Player(entity),
        });

        let Ok(player_id) = query.get(entity) else {
            continue;
        };
        info!(
            "Player {} executed command: /{}",
            player_id.name.as_ref().expect("No Player Name"),
            event.command
        );
    }
}
