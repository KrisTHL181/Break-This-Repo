use bevy_ecs::system::Query;
use bevy_math::Vec3A;
use temper_components::player::velocity::Velocity;

pub fn handle(mut query: Query<&mut Velocity>) {
    for mut vel in query.iter_mut() {
        //TODO: proper dampen
        if vel.length() <= 0.001 {
            **vel = Vec3A::ZERO;
        }
        const DAMPEN_AMOUNT: Vec3A = Vec3A::new(0.9, 0.99, 0.9);
        **vel *= DAMPEN_AMOUNT;
    }
}
