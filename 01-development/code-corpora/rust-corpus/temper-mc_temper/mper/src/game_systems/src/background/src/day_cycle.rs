use bevy_ecs::prelude::{Commands, Entity, Query, ResMut};
use temper_codec::net_types::length_prefixed_vec::LengthPrefixedVec;
use temper_components::player::time::LastSentTimeUpdate;
use temper_net_runtime::connection::StreamWriter;
use temper_protocol::outgoing::update_time::{Clock, UpdateTimePacket};
use temper_resources::time::WorldTime;
use tracing::warn;

pub fn tick_daylight_cycle(
    mut world_time: ResMut<WorldTime>,
    players: Query<(Entity, &StreamWriter)>,
    mut last_sent_time: Query<&mut LastSentTimeUpdate>,
    mut commands: Commands,
) {
    world_time.advance_tick();

    let packet = UpdateTimePacket {
        world_age: 0,
        clocks: LengthPrefixedVec::new(vec![Clock {
            // Hardcoded overworld ID. Not ideal but fine for now.
            clock_id: 0.into(),
            time: i32::from(world_time.current_time()).into(),
            fractional_time: 0.0,
            rate: 1.0,
        }]),
    };

    for (eid, writer) in players.iter() {
        if let Ok(mut last_sent_time_update) = last_sent_time.get_mut(eid) {
            if last_sent_time_update.should_resend() {
                last_sent_time_update.reset();

                writer.send_packet_ref(&packet).unwrap_or_else(|_| {
                    warn!("Failed to send UpdateTimePacket to player {}", eid);
                });
            }
        } else {
            commands.entity(eid).insert(LastSentTimeUpdate::default());

            writer.send_packet_ref(&packet).unwrap_or_else(|_| {
                warn!("Failed to send UpdateTimePacket to player {}", eid);
            });
        }
    }
}
