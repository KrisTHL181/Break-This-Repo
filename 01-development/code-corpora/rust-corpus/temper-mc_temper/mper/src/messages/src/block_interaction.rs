use bevy_ecs::prelude::{Entity, Message};
use temper_codec::net_types::var_int::VarInt;
use temper_core::pos::BlockPos;

/// Message sent when a player right-clicks an interactive block (door, lever, etc.)
/// and is NOT sneaking.
///
/// Emitted by the PlaceBlock packet handler; consumed by the interaction listener.
#[derive(Message, Clone, Debug)]
pub struct BlockInteractMessage {
    pub player: Entity,
    pub position: BlockPos,
    pub sequence: VarInt,
}
