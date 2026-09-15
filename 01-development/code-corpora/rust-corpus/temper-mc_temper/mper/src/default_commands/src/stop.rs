use std::sync::atomic::Ordering::Relaxed;

use bevy_ecs::prelude::Res;
use temper_command_infra::CommandSource::*;
use temper_command_infra::{CommandHandler, CommandResult, CommandSource};
use temper_macros::Command;
use temper_state::GlobalStateResource;
use tracing::info;

#[derive(Command)]
#[command(name = "stop", aliases = ["quit"])]
struct StopCommand;

impl CommandHandler for StopCommand {
    type SystemParam<'w, 's> = Res<'w, GlobalStateResource>;

    fn handle(self, source: CommandSource, state: &mut Self::SystemParam<'_, '_>) -> CommandResult {
        if let Player(_) = source {
            return Err("This command can only be used by the server.".into());
        }

        info!("Shutting down server...");
        state.0.shut_down.store(true, Relaxed);
        state
            .0
            .world
            .sync()
            .map_err(|error| format!("Failed to sync world before shutdown: {error}"))?;

        Ok(())
    }
}
