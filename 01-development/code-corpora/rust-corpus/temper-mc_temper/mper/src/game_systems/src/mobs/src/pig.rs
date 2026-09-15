use bevy_ecs::prelude::*;
use bevy_math::{Vec2, Vec3A};
use pathfinding::{Pathfinder, pos_to_block};
use temper_components::mob_ai::PigAI;
use temper_components::player::grounded::OnGround;
use temper_components::player::player_marker::PlayerMarker;
use temper_components::player::position::Position;
use temper_components::player::velocity::Velocity;
use temper_entities::markers::entity_types::Pig;
use temper_messages::particle::SendParticle;
use temper_particles::ParticleType;

/// Pig walk speed in blocks per tick.
const PIG_WALK_SPEED: f32 = 0.1;

/// Jump impulse matching Minecraft's standard jump velocity (blocks/tick).
const JUMP_IMPULSE: f32 = 0.42;

/// How often to update the pathfinding target (ticks).
const REPATH_INTERVAL: u32 = 40;

type PigQuery<'a> = (
    &'a Position,
    &'a mut Velocity,
    &'a OnGround,
    &'a mut PigAI,
    &'a mut Pathfinder,
);

pub fn tick_pig(
    mut pigs: Query<PigQuery, With<Pig>>,
    players: Query<&Position, With<PlayerMarker>>,
) {
    for (pig_pos, mut velocity, grounded, mut ai, mut pathfinder) in pigs.iter_mut() {
        ai.repath_cooldown = ai.repath_cooldown.saturating_sub(1);

        // Repath when the cooldown expires OR when the pig has followed a path
        // to its end (but not when pathfinding simply failed to find a route).
        let path_reached_end =
            !pathfinder.has_path() && !pathfinder.path.is_empty() && !pathfinder.is_searching;

        if ai.repath_cooldown == 0 || path_reached_end {
            pathfinder.target = players
                .iter()
                .min_by(|a, b| {
                    pig_pos
                        .coords
                        .distance_squared(a.coords)
                        .total_cmp(&pig_pos.coords.distance_squared(b.coords))
                })
                .map(pos_to_block);
            pathfinder.request_repath();
            ai.repath_cooldown = REPATH_INTERVAL;
        }

        let current_block = pos_to_block(pig_pos);

        // Advance waypoint when the pig reaches it (same X/Z block).
        if let Some(wp) = pathfinder.current_waypoint()
            && wp.pos.x == current_block.pos.x
            && wp.pos.z == current_block.pos.z
        {
            pathfinder.advance_waypoint();
        }

        let Some(next) = pathfinder.current_waypoint() else {
            stop(&mut velocity);
            continue;
        };

        // Jump if the next waypoint is 1 block above and the pig is on the ground.
        if next.pos.y > current_block.pos.y && grounded.0 {
            velocity.vec.y = JUMP_IMPULSE;
        }

        // Steer horizontally toward the center of the next waypoint block.
        let target = Vec2::new(next.pos.x as f32 + 0.5, next.pos.z as f32 + 0.5);
        let current = Vec2::new(pig_pos.x as f32, pig_pos.z as f32);
        let dir = target - current;

        if dir.length_squared() > 0.01 {
            let normalized = dir.normalize();
            velocity.vec.x = normalized.x * PIG_WALK_SPEED;
            velocity.vec.z = normalized.y * PIG_WALK_SPEED;
        } else {
            velocity.vec.x = 0.0;
            velocity.vec.z = 0.0;
        }
    }
}

pub fn tick_pig_particles(
    pigs: Query<&Pathfinder, With<Pig>>,
    mut msgs: MessageWriter<SendParticle>,
) {
    for path in pigs.iter() {
        for step_pos in &path.path {
            let particle_message = SendParticle {
                particle_type: ParticleType::EndRod,
                position: step_pos.pos.as_vec3a() + Vec3A::new(0.5, 0.5, 0.5),
                offset: Vec3A::new(0.0, 0.0, 0.0),
                speed: 0.0,
                count: 1,
            };
            msgs.write(particle_message);
        }
    }
}

fn stop(velocity: &mut Velocity) {
    velocity.vec.x = 0.0;
    velocity.vec.z = 0.0;
}
