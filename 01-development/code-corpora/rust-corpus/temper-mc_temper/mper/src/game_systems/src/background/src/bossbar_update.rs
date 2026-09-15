use bevy_ecs::prelude::{Query, ResMut};
use temper_components::player::bossbar_sender::{BossbarSender, BossbarSenderState};
use temper_net_runtime::connection::StreamWriter;
use temper_protocol::outgoing::boss_event::BossbarPacket;
use temper_resources::bossbar::{BossBarData, BossBarResource, UpdateBBKind};
use tracing::warn;
use uuid::Uuid;

pub fn handle(
    mut player_query: Query<(&StreamWriter, &mut BossbarSender)>,
    mut boss_bar_resource: ResMut<BossBarResource>,
) {
    let mut updated: Vec<(Uuid, UpdateBBKind)> = Vec::new();

    // --- Resource update phase ---
    while let Some((uuid, update_kind)) = boss_bar_resource.update_queue.pop() {
        match &update_kind {
            UpdateBBKind::Add { data } => {
                boss_bar_resource.boss_bars.insert(uuid, data.clone());
            }

            UpdateBBKind::Remove => {
                boss_bar_resource.boss_bars.remove(&uuid);
            }

            UpdateBBKind::UpdateHealth {
                new_health,
                new_max,
            } => {
                if let Some(data) = boss_bar_resource.boss_bars.get_mut(&uuid) {
                    data.health = *new_health;
                    data.max = *new_max;
                }
            }

            UpdateBBKind::UpdateTitle { title } => {
                if let Some(data) = boss_bar_resource.boss_bars.get_mut(&uuid) {
                    data.title = title.clone();
                }
            }

            UpdateBBKind::UpdateStyle { color, dividers } => {
                if let Some(data) = boss_bar_resource.boss_bars.get_mut(&uuid) {
                    data.color = *color;
                    data.dividers = *dividers;
                }
            }

            UpdateBBKind::UpdateFlags { flags } => {
                if let Some(data) = boss_bar_resource.boss_bars.get_mut(&uuid) {
                    data.flags = flags.clone();
                }
            }

            UpdateBBKind::UpdateNetworking { .. } => {}
        }

        updated.push((uuid, update_kind));
    }

    // --- Player sync phase ---
    for (writer, mut bossbar_sender) in player_query.iter_mut() {
        for (uuid, update_kind) in &updated {
            let state = bossbar_sender.get_state(*uuid);

            match update_kind {
                UpdateBBKind::Add { .. } => continue,

                UpdateBBKind::Remove => {
                    remove_bb_player(writer, *uuid, &mut bossbar_sender);
                    continue;
                }

                UpdateBBKind::UpdateNetworking { additive } => {
                    let Some(bar) = boss_bar_resource.boss_bars.get(uuid) else {
                        continue;
                    };

                    if let Some(BossbarSenderState::Additive | BossbarSenderState::Subtractive) =
                        state
                    {
                        if !additive {
                            remove_bb_player(writer, *uuid, &mut bossbar_sender);
                        } else {
                            add_bb_player(writer, *uuid, &mut bossbar_sender, bar);
                        }
                    }

                    continue;
                }

                _ => {}
            }

            // --- update packets ---
            if state != Some(BossbarSenderState::Update) {
                continue;
            }

            let packet = match update_kind {
                UpdateBBKind::UpdateHealth {
                    new_health,
                    new_max,
                } => BossbarPacket::update_health(uuid.as_u128(), *new_health, *new_max),

                UpdateBBKind::UpdateTitle { title } => {
                    BossbarPacket::update_title(uuid.as_u128(), title.clone())
                }

                UpdateBBKind::UpdateStyle { color, dividers } => BossbarPacket::update_style(
                    uuid.as_u128(),
                    color.discriminant().into(),
                    dividers.discriminant().into(),
                ),

                UpdateBBKind::UpdateFlags { flags } => {
                    BossbarPacket::update_flags(uuid.as_u128(), flags.get())
                }

                _ => continue,
            };

            if writer.send_packet_ref(&packet).is_ok() {
                bossbar_sender.informed(*uuid);
            } else {
                warn!("Failed to send Bossbar Packet to player");
            }
        }
    }
}

fn add_bb_player(writer: &StreamWriter, uuid: Uuid, sender: &mut BossbarSender, bar: &BossBarData) {
    let id = uuid.as_u128();

    let packet = BossbarPacket::add_bossbar(
        id,
        bar.title.clone(),
        bar.health / bar.max,
        bar.color.discriminant().into(),
        bar.dividers.discriminant().into(),
        bar.flags.get(),
    );

    if writer.send_packet_ref(&packet).is_ok() {
        sender.informed(uuid);
    } else {
        warn!("Failed to send Bossbar Packet to player");
    }
}

fn remove_bb_player(writer: &StreamWriter, uuid: Uuid, sender: &mut BossbarSender) {
    if sender.0.contains_key(&uuid) {
        let packet = BossbarPacket::remove_bossbar(uuid.as_u128());

        if writer.send_packet_ref(&packet).is_ok() {
            sender.informed(uuid);
        } else {
            warn!("Failed to send Bossbar Packet to player");
        }
    }
}
