use bevy_ecs::prelude::{Entity, Message};
use temper_components::player::position::Position;
use temper_components::player::rotation::Rotation;
use temper_components::player::velocity::Velocity;

#[derive(Message)]
pub struct TeleportEntity {
    pub entity: Entity,
    pub position: Position,
    pub rotation: Rotation,
    pub velocity: Velocity,
}

impl TeleportEntity {
    pub fn new(entity: Entity, position: Position, rotation: Rotation) -> Self {
        Self {
            entity,
            position,
            rotation,
            velocity: Velocity::zero(),
        }
    }

    pub fn with_velocity(mut self, velocity: Velocity) -> Self {
        self.velocity = velocity;
        self
    }
}
