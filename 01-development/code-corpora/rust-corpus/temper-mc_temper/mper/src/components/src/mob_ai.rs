use bevy_ecs::prelude::Component;

/// Per-pig AI state.
#[derive(Component, Default)]
pub struct PigAI {
    pub repath_cooldown: u32,
}
