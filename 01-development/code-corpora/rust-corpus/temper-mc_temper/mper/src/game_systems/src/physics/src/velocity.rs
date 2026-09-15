use bevy_ecs::prelude::Query;
use bevy_math::Vec3A;
use temper_components::player::position::Position;
use temper_components::player::velocity::Velocity;

pub fn handle(mut query: Query<(&Velocity, &mut Position)>) {
    for (vel, mut pos) in query.iter_mut() {
        if **vel == Vec3A::ZERO {
            continue;
        }
        pos.coords += vel.as_dvec3();
    }
}
