use shipyard::Component;
use crate::block::Block;

pub const PLAYER_HEALTH: u8 = 20;

#[derive(Component)]
pub struct Player;

#[derive(Component, Clone, Copy, Debug, Default, PartialEq, Eq)]
#[repr(transparent)]
pub struct PlayerHolding(pub Option<Block>);
