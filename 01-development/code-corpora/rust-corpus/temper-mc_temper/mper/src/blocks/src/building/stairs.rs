use crate::{BlockBehavior, world_extensions::WorldBlockUpdates};
use temper_block_data::{PlacedBlocks, PlacementContext};
use temper_block_properties::{Direction, Half, StairsShape};
use temper_blocks_generated::StairsBlock;
use temper_core::{block_face::BlockFace, block_state_id::BlockStateId, pos::BlockPos};
use temper_macros::match_block;
use temper_world::{Dimension, World};

fn is_different_orientation(
    level: &World,
    block: &StairsBlock,
    block_pos: BlockPos,
    direction: &Direction,
    dimension: Dimension,
) -> bool {
    if let Some(other_block) =
        level.try_get_block::<StairsBlock>(block_pos + direction.get_normal(), dimension)
    {
        other_block.facing != block.facing || other_block.half != block.half
    } else {
        true
    }
}

fn get_shape(
    block: &StairsBlock,
    level: &World,
    block_pos: BlockPos,
    dimension: Dimension,
) -> StairsShape {
    let offset_block_pos = block_pos + block.facing.get_normal();
    let opposite_offset_block_pos = block_pos + block.facing.opposite().get_normal();

    if let Some(offset_block) = level.try_get_block::<StairsBlock>(offset_block_pos, dimension)
        && block.half == offset_block.half
        && offset_block.facing.axis() != block.facing.axis()
        && is_different_orientation(
            level,
            block,
            block_pos,
            &offset_block.facing.opposite(),
            dimension,
        )
    {
        return if offset_block.facing == block.facing.rotate_y_counter_clockwise() {
            StairsShape::OuterLeft
        } else {
            StairsShape::OuterRight
        };
    }

    if let Some(opposite_offset_block) =
        level.try_get_block::<StairsBlock>(opposite_offset_block_pos, dimension)
        && block.half == opposite_offset_block.half
        && opposite_offset_block.facing.axis() != block.facing.axis()
        && is_different_orientation(
            level,
            block,
            block_pos,
            &opposite_offset_block.facing,
            dimension,
        )
    {
        return if opposite_offset_block.facing == block.facing.rotate_y_counter_clockwise() {
            StairsShape::InnerLeft
        } else {
            StairsShape::InnerRight
        };
    }

    StairsShape::Straight
}

impl BlockBehavior for StairsBlock {
    fn get_placement_state(&mut self, context: PlacementContext) -> PlacedBlocks {
        let block = context
            .level
            .get_chunk(context.block_pos.chunk(), context.dimension)
            .map(|c| c.get_block(context.block_pos.chunk_block_pos()))
            .unwrap_or(BlockStateId::new(0));

        self.waterlogged = match_block!("water", block);

        self.half = match context.face {
            BlockFace::Top => Half::Bottom,
            BlockFace::Bottom => Half::Top,
            _ => {
                if context.cursor.y > 0.5 {
                    Half::Top
                } else {
                    Half::Bottom
                }
            }
        };
        self.facing = Direction::from_yaw(context.player_rotation.yaw);
        self.shape = get_shape(self, context.level, context.block_pos, context.dimension);

        PlacedBlocks::default()
    }

    fn update(&mut self, world: &World, pos: BlockPos) -> bool {
        self.shape = get_shape(self, world, pos, Dimension::Overworld);
        false
    }
}
