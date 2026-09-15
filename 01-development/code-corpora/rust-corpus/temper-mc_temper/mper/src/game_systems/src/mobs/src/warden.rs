use bevy_ecs::prelude::*;
use temper_components::bossbar::BossbarOwner;
use temper_components::player::bossbar_sender::BossbarSender;
use temper_components::player::grounded::OnGround;
use temper_components::player::player_marker::PlayerMarker;
use temper_components::player::position::Position;
use temper_components::player::velocity::Velocity;
use temper_entities::markers::entity_types::Warden;
use temper_resources::bossbar::{BossBarData, BossBarResource, BossbarColor};
use temper_text::TextComponent;

const BB_ENTER_RANGE: f64 = 10.0;
const BB_EXIT_RANGE: f64 = 12.0;

type WardenQuery<'a> = (
    &'a Position,
    &'a mut Velocity,
    &'a OnGround,
    &'a BossbarOwner,
);

pub fn init_warden(
    warden: Query<&BossbarOwner, With<Warden>>,
    mut boss_bar_resource: ResMut<BossBarResource>,
) {
    let warden_max_health = 500.0;
    let warden_health = 500.0;

    for owner in warden.iter() {
        if boss_bar_resource.boss_bars.contains_key(&owner.id()) {
            continue;
        }

        let data = BossBarData::new(
            TextComponent::from("Warden"),
            warden_health,
            warden_max_health,
            BossbarColor::Blue,
        );

        boss_bar_resource.register_bar_with_id(owner.id(), data);
    }
}

pub fn tick_warden(
    warden: Query<WardenQuery, (With<Warden>, With<BossbarOwner>)>,
    mut players: Query<(&Position, &mut BossbarSender), With<PlayerMarker>>,
    boss_bar_resource: ResMut<BossBarResource>,
) {
    for (warden_pos, _, _, owned_bossbar) in warden.iter() {
        let uuid = owned_bossbar.id();

        for (player_pos, mut bossbar_sender) in players.iter_mut() {
            let distance = warden_pos.distance(**player_pos);

            let current = bossbar_sender.0.get(&uuid).copied();

            if distance <= BB_ENTER_RANGE {
                if current.is_none() {
                    bossbar_sender.add(uuid);
                    boss_bar_resource.queue_networking(uuid, true);
                }
            } else if distance > BB_EXIT_RANGE
                && let Some(_) = current
            {
                bossbar_sender.remove(uuid);
                boss_bar_resource.queue_networking(uuid, false);
            }
        }
    }
}
