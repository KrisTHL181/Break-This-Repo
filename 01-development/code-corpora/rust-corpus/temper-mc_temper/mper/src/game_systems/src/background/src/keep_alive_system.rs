use bevy_ecs::prelude::{Entity, Query, Res};
use std::time::Duration;
use temper_components::player::keepalive::KeepAliveTracker;
use temper_net_runtime::connection::StreamWriter;
use temper_state::GlobalStateResource;
use tracing::warn;

pub fn keep_alive_system(
    mut query: Query<(Entity, &mut KeepAliveTracker, &StreamWriter)>,
    state: Res<GlobalStateResource>,
) {
    let now = std::time::Instant::now(); // faster than SystemTime for diffs
    const TIMEOUT: Duration = Duration::from_secs(15);
    const KEEPALIVE_INTERVAL: Duration = Duration::from_secs(10);

    for (entity, mut tracker, stream_writer) in query.iter_mut() {
        // Skip if connection is already closed
        if !stream_writer
            .running
            .load(std::sync::atomic::Ordering::Relaxed)
        {
            if state.0.players.is_connected(entity) {
                state.0.players.disconnect(entity, None);
            }
            continue;
        }

        let time_since_response = now.duration_since(tracker.last_received_keep_alive);
        let time_since_request = now.duration_since(tracker.last_sent_keep_alive);

        // Kill connection if the client has not answered the current request.
        if keep_alive_timed_out(&tracker, now, TIMEOUT) {
            warn!(
                "Killing connection for {}, keepalive response is {:?} overdue",
                entity,
                time_since_request - TIMEOUT,
            );
            state
                .0
                .players
                .disconnect(entity, Some("Connection timed out".to_string()));
            continue;
        }

        // Send keepalive if needed
        if tracker.has_received_keep_alive && time_since_response >= KEEPALIVE_INTERVAL {
            let timestamp = rand::random::<i64>(); // or use a counter
            let packet =
                temper_protocol::outgoing::keep_alive::OutgoingKeepAlivePacket { timestamp };

            if let Err(err) = stream_writer.send_packet_ref(&packet) {
                warn!("Failed to send keep alive packet to {}: {:?}", entity, err);
            }

            tracker.last_sent_keep_alive_id = timestamp;
            tracker.has_received_keep_alive = false;
            tracker.last_sent_keep_alive = now;
        }
    }
}

fn keep_alive_timed_out(
    tracker: &KeepAliveTracker,
    now: std::time::Instant,
    timeout: Duration,
) -> bool {
    !tracker.has_received_keep_alive && now.duration_since(tracker.last_sent_keep_alive) > timeout
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn does_not_timeout_before_sending_a_keepalive_request() {
        let now = std::time::Instant::now();
        let tracker = KeepAliveTracker {
            last_sent_keep_alive_id: 0,
            last_received_keep_alive: now - Duration::from_secs(60),
            last_sent_keep_alive: now - Duration::from_secs(60),
            has_received_keep_alive: true,
        };

        assert!(!keep_alive_timed_out(
            &tracker,
            now,
            Duration::from_secs(15),
        ));
    }

    #[test]
    fn times_out_unanswered_keepalive_requests() {
        let now = std::time::Instant::now();
        let tracker = KeepAliveTracker {
            last_sent_keep_alive_id: 4,
            last_received_keep_alive: now - Duration::from_secs(60),
            last_sent_keep_alive: now - Duration::from_secs(16),
            has_received_keep_alive: false,
        };

        assert!(keep_alive_timed_out(&tracker, now, Duration::from_secs(15),));
    }
}
