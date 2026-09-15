use std::collections::HashSet;

use bevy_ecs::prelude::*;
use temper_command_infra::{
    CommandGraph, CommandRegistry, PlayerCommandGraph, RebuildCommandGraph,
};
use temper_net_runtime::connection::StreamWriter;
use temper_permissions::player::PlayerPermission;
use temper_protocol::outgoing::commands::CommandsPacket;
use tracing::error;

pub fn rebuild_and_send_command_graphs(
    mut commands: Commands,
    mut rebuilds: MessageReader<RebuildCommandGraph>,
    registry: Res<CommandRegistry>,
    query: Query<(
        &StreamWriter,
        Option<&PlayerCommandGraph>,
        Option<&PlayerPermission>,
    )>,
) {
    let players = rebuilds
        .read()
        .map(|rebuild| rebuild.player)
        .collect::<HashSet<_>>();

    for player in players {
        let Ok((writer, previous_graph, permissions)) = query.get(player) else {
            continue;
        };

        let graph =
            CommandGraph::from_paths(&registry.paths_for_player_permissions(player, permissions));
        let packet = CommandsPacket::from_command_infra_graph(&graph);

        if let Err(err) = writer.send_packet(packet) {
            error!("failed sending rebuilt command graph to player {player:?}: {err}");
            continue;
        }

        commands
            .entity(player)
            .insert(PlayerCommandGraph::next(graph, previous_graph));
    }
}
