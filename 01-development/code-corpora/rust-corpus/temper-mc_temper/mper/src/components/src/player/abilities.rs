use super::gamemode::GameMode;
use bevy_ecs::prelude::Component;
use bitcode_derive::{Decode, Encode};
use type_hash::TypeHash;

#[derive(Component, Debug, Clone, Copy, Encode, Decode, TypeHash)]
pub struct PlayerAbilities {
    pub invulnerable: bool,
    pub flying: bool,
    pub may_fly: bool,
    pub creative_mode: bool,
    pub may_build: bool,
    pub flying_speed: f32,
    pub walking_speed: f32,
}

impl Default for PlayerAbilities {
    fn default() -> Self {
        Self::for_game_mode(GameMode::default())
    }
}

impl PlayerAbilities {
    pub fn for_game_mode(game_mode: GameMode) -> Self {
        match game_mode {
            GameMode::Survival => Self {
                invulnerable: false,
                flying: false,
                may_fly: false,
                creative_mode: false,
                may_build: true,
                flying_speed: 0.05,
                walking_speed: 0.1,
            },
            GameMode::Creative => Self {
                invulnerable: true,
                flying: false,
                may_fly: true,
                creative_mode: true,
                may_build: true,
                flying_speed: 0.05,
                walking_speed: 0.1,
            },
            GameMode::Adventure => Self {
                invulnerable: false,
                flying: false,
                may_fly: false,
                creative_mode: false,
                may_build: false,
                flying_speed: 0.05,
                walking_speed: 0.1,
            },
            GameMode::Spectator => Self {
                invulnerable: true,
                flying: true,
                may_fly: true,
                creative_mode: false,
                may_build: false,
                flying_speed: 0.05,
                walking_speed: 0.1,
            },
        }
    }
}
