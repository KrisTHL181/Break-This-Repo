use bevy_ecs::prelude::Query;
use temper_command_infra::CommandSource::*;
use temper_command_infra::args::GreedyStringArg;
use temper_command_infra::{CommandHandler, CommandResult, CommandSource};
use temper_components::entity_identity::Identity;
use temper_macros::Command;
use temper_text::{TextComponent, TextComponentBuilder};

#[derive(Command)]
#[command("echo")]
struct EchoCommand {
    message: GreedyStringArg,
}

impl CommandHandler for EchoCommand {
    type SystemParam<'w, 's> = Query<'w, 's, &'static Identity>;

    fn handle(
        self,
        source: CommandSource,
        identities: &mut Self::SystemParam<'_, '_>,
    ) -> CommandResult {
        let username = match source {
            Server => "Server".to_string(),
            Player(entity) => identities
                .get(entity)
                .map_err(|_| "sender does not exist")?
                .name
                .as_ref()
                .ok_or("sender does not have a player name")?
                .clone(),
        };

        let message = TextComponentBuilder::new(format!("{username} said: "))
            .extra(TextComponent::from(self.message.to_string()))
            .build();

        source.send_message(message);

        Ok(())
    }
}
