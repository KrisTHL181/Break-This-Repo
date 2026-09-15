use crate::{BlockBehavior, BlockDispatch};
use temper_block_data::{PlacedBlocks, PlacementContext};
use temper_block_properties::SlabType;
use temper_blocks_generated::SlabBlock;
use temper_core::block_face::BlockFace;
use temper_core::block_state_id::BlockStateId;
use temper_macros::match_block;

impl BlockBehavior for SlabBlock {
    fn get_placement_state(&mut self, context: PlacementContext) -> PlacedBlocks {
        let block = context
            .level
            .get_chunk(context.block_pos.chunk(), context.dimension)
            .map(|c| c.get_block(context.block_pos.chunk_block_pos()))
            .unwrap_or(BlockStateId::new(0));

        self.ty = if let Some(block) = block.try_cast::<SlabBlock>() {
            if block.block_type == self.block_type {
                SlabType::Double
            } else {
                // This will cause a bug where if you place a slab onto a slab of a different type it will replace the block with the one you're placing,
                // but this can't really be fixed until BlockBehavior is updated
                return PlacedBlocks::default(); // TODO: When BlockBehavior is updated with a return value for Option<Self>, return None here
            }
        } else {
            match context.face {
                BlockFace::Top => SlabType::Bottom,
                BlockFace::Bottom => SlabType::Top,
                _ => {
                    if context.cursor.y > 0.5 {
                        SlabType::Top
                    } else {
                        SlabType::Bottom
                    }
                }
            }
        };

        self.waterlogged = match_block!("water", block);

        PlacedBlocks::default()
    }

    fn can_be_replaced(&self, context: PlacementContext) -> bool {
        if matches!(self.ty, SlabType::Double) {
            return false;
        }

        if !matches!(context.face, BlockFace::Top | BlockFace::Bottom) {
            return false;
        }

        let expected_face = match self.ty {
            SlabType::Top => BlockFace::Bottom,
            SlabType::Bottom => BlockFace::Top,
            SlabType::Double => unreachable!(),
        };

        if context.face != expected_face {
            return false;
        }

        context
            .default_placement_state
            .try_cast::<SlabBlock>()
            .is_some_and(|slab| slab.block_type == self.block_type)
    }
}
