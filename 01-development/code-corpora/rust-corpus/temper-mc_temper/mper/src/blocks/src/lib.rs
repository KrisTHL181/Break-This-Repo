mod bed_block;
mod behavior_trait;
mod bubble_column;
mod building;
mod cake;
mod candle_cake;
mod decorative;
mod dried_ghast;
mod facing_block;
mod fire;
mod functional;
mod liquid;
mod nature;
mod redstone;
mod skull;
mod suspicious_block;
mod wall_skull;
mod waterloggable_block;
mod world_extensions;

#[allow(unused_imports)] // Used in the include!
use crate::behavior_trait::BlockBehaviorTable;

pub use crate::behavior_trait::{BlockBehavior, BlockDispatch, StateBehaviorTable};
pub use temper_block_data::*;

pub const BLOCK_MAPPINGS: &[StateBehaviorTable] =
    include!(concat!(env!("OUT_DIR"), "/mappings.rs"));

include!(concat!(env!("OUT_DIR"), "/default_block_states.rs"));
