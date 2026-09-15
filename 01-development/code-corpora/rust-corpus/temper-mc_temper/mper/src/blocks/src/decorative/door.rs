use crate::BlockBehavior;
use crate::world_extensions::WorldBlockUpdates;
use std::collections::HashMap;
use temper_block_data::{BlockUpdates, BrokenBlocks, PlacedBlocks, PlacementContext};
use temper_block_properties::{Direction, DoorHingeSide, DoubleBlockHalf};
use temper_blocks_generated::DoorBlock;
use temper_core::block_face::BlockFace;
use temper_core::block_state_id::BlockStateId;
use temper_core::pos::BlockPos;
use temper_world::World;
use tracing::error;

impl BlockBehavior for DoorBlock {
    fn get_placement_state(&mut self, context: PlacementContext) -> PlacedBlocks {
        self.facing = match context.face {
            BlockFace::North => Direction::South,
            BlockFace::South => Direction::North,
            BlockFace::East => Direction::West,
            BlockFace::West => Direction::East,
            BlockFace::Top => Direction::from_yaw(context.player_rotation.yaw),
            _ => {
                error!("Invalid block face clicked");
                return PlacedBlocks::default(); // TODO: should return None or Err in the future
            }
        };

        self.open = false;
        self.powered = false;
        self.hinge = DoorHingeSide::Left;
        self.half = DoubleBlockHalf::Lower;

        let mut top_half = self.clone();
        top_half.half = DoubleBlockHalf::Upper;

        let mut placed_blocks = PlacedBlocks::default();
        placed_blocks.blocks.insert(
            context.block_pos.above(),
            BlockStateId::new(top_half.try_into().unwrap()),
        );

        placed_blocks
    }

    fn interact(&mut self, world: &World, pos: BlockPos) -> BlockUpdates {
        self.open = !self.open;

        let mut blocks = HashMap::new();
        let other_pos = match self.half {
            DoubleBlockHalf::Upper => pos.below(),
            DoubleBlockHalf::Lower => pos.above(),
        };

        if let Ok(other_state) =
            world.update_block_cast::<DoorBlock>(other_pos, |block| block.open = self.open)
        {
            blocks.insert(other_pos, other_state);
        } else {
            error!(
                "Expected door block at {}, but did not find one!",
                other_pos
            );
        }

        BlockUpdates { blocks }
    }

    fn try_break(&self, world: &World, pos: BlockPos) -> BrokenBlocks {
        let pos = match self.half {
            DoubleBlockHalf::Upper => pos.below(),
            DoubleBlockHalf::Lower => pos.above(),
        };

        BrokenBlocks {
            blocks: if world.block_is::<DoorBlock>(pos) {
                vec![pos]
            } else {
                vec![]
            },
        }
    }

    fn is_interactable(&self) -> bool {
        true
    }
}
