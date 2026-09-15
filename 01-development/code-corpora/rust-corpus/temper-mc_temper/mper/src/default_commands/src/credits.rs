use bevy_ecs::prelude::Query;
use temper_codec::net_types::adhoc_id::AdHocID;
use temper_command_infra::{CommandHandler, CommandResult, CommandSource};
use temper_macros::Command;
use temper_nbt::NBT;
use temper_net_runtime::connection::StreamWriter;
use temper_protocol::outgoing::show_dialog::{DialogBody, DialogContent, ShowDialog};
use temper_text::TextComponent;

pub(crate) static CREDITS_TEXT: &str = include_str!("../../../assets/data/credits.txt");

#[derive(Command)]
#[command("credits")]
struct CreditsCommand;

impl CommandHandler for CreditsCommand {
    type SystemParam<'w, 's> = Query<'w, 's, &'static StreamWriter>;

    fn handle(
        self,
        source: CommandSource,
        params: &mut Self::SystemParam<'_, '_>,
    ) -> CommandResult {
        let conn = match source {
            CommandSource::Server => return Err("Only players can view credits.".into()),
            CommandSource::Player(entity) => {
                params.get(entity).map_err(|_| "sender does not exist")?
            }
        };
        let lines = CREDITS_TEXT
            .lines()
            .map(|t| DialogBody {
                dialog_body_type: "minecraft:plain_message".to_string(),
                contents: TextComponent::from(t),
                width: Some(1024),
            })
            .collect::<Vec<_>>();
        let packet = ShowDialog {
            content: AdHocID::from(NBT::from(DialogContent {
                dialog_content_type: "minecraft:notice".to_string(),
                title: TextComponent::from("Credits"),
                body: lines,
            })),
        };
        conn.send_packet(packet)
            .map_err(|error| format!("failed to send credits dialog: {error}"))?;

        Ok(())
    }
}
