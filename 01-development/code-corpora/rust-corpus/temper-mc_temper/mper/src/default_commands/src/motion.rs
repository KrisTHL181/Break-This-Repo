use bevy_ecs::prelude::{Entity, Query};
use bevy_ecs::query::Without;
use temper_command_infra::args::{EntitiesArg, PositionArg};
use temper_command_infra::{CommandHandler, CommandResult, CommandSource};
use temper_components::entity_identity::Identity;
use temper_components::player::player_marker::PlayerMarker;
use temper_components::player::position::Position;
use temper_components::player::velocity::Velocity;
use temper_macros::Command;
use temper_permissions::Permissions;

#[derive(Debug, Command)]
#[command(name = "motion", permission = Permissions::Op)]
struct MotionCommand {
    target: EntitiesArg,
    offset: PositionArg,
}

impl CommandHandler for MotionCommand {
    type SystemParam<'w, 's> = (
        Query<'w, 's, (Entity, &'static Identity, Option<&'static PlayerMarker>)>,
        Query<'w, 's, (&'static Identity, &'static mut Velocity), Without<PlayerMarker>>,
    );

    fn handle(
        self,
        source: CommandSource,
        params: &mut Self::SystemParam<'_, '_>,
    ) -> CommandResult {
        let (entity_query, motion_targets) = params;

        let entities = self.target.resolve(entity_query.iter());

        let offset = self.offset.resolve(&Position::new(0f64, 0f64, 0f64));

        for entity in &entities {
            if let Ok((_, mut velocity)) = motion_targets.get_mut(*entity) {
                velocity.vec += offset.as_vec3a();
            }
        }

        source.send_message(
            format!(
                "Added velocity of ~{} ~{} ~{} for {} entitie(s)",
                offset.x,
                offset.y,
                offset.z,
                entities.len()
            )
            .into(),
        );

        Ok(())
    }
}
