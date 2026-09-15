use crate::BlockBehavior;
use std::collections::HashMap;
use temper_block_data::{PlacedBlocks, PlacementContext};
use temper_block_properties::Axis;
use temper_blocks_generated::PillarBlock;
use temper_core::block_face::BlockFace;

impl BlockBehavior for PillarBlock {
    fn get_placement_state(&mut self, context: PlacementContext) -> PlacedBlocks {
        self.axis = match context.face {
            BlockFace::Top | BlockFace::Bottom => Axis::Y,
            BlockFace::North | BlockFace::South => Axis::Z,
            BlockFace::East | BlockFace::West => Axis::X,
        };

        PlacedBlocks {
            take_item: true,
            blocks: HashMap::with_capacity(0),
            place_original: true,
        }
    }
}
