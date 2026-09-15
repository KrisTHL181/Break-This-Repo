use crate::world_extensions::WorldBlockUpdates;
use crate::{BlockBehavior, BlockDispatch};
use bevy_math::IVec3;
use std::collections::HashMap;
use temper_block_data::{PlacedBlocks, PlacementContext};
use temper_blocks_generated::{
    FenceAndPaneBlock, FenceAndPaneBlockType, LiquidBlock, LiquidBlockType,
};
use temper_core::dimension::Dimension;
use temper_core::pos::BlockPos;
use temper_world::World;

// North (-Z), East (+X), South (+Z), West (-X)
const SIDE_DIRECTIONS: [(i32, i32); 4] = [(0, -1), (1, 0), (0, 1), (-1, 0)];

impl BlockBehavior for FenceAndPaneBlock {
    fn get_placement_state(&mut self, context: PlacementContext) -> PlacedBlocks {
        self.waterlogged = context
            .level
            .block_is_and::<LiquidBlock>(context.block_pos, |liquid| {
                matches!(liquid.block_type, LiquidBlockType::Water)
            });

        let block_is_pane = is_pane(&self.block_type);

        for ((dx, dz), flag) in SIDE_DIRECTIONS.iter().zip([
            &mut self.north,
            &mut self.east,
            &mut self.south,
            &mut self.west,
        ]) {
            let pos = context.block_pos + IVec3::new(*dx, 0, *dz);
            let block = context
                .level
                .get_block(pos, context.dimension)
                .unwrap_or_default();

            *flag = !context
                .level
                .block_is_and::<FenceAndPaneBlock>(pos, |block| {
                    block_is_pane ^ is_pane(&block.block_type)
                })
                && block.is_solid();
        }

        PlacedBlocks {
            take_item: true,
            blocks: HashMap::with_capacity(0),
            place_original: true,
        }
    }

    fn update(&mut self, world: &World, pos: BlockPos) -> bool {
        let block_is_pane = is_pane(&self.block_type);

        let mut changed = false;

        for ((dx, dz), flag) in SIDE_DIRECTIONS.iter().zip([
            &mut self.north,
            &mut self.east,
            &mut self.south,
            &mut self.west,
        ]) {
            let pos = pos + IVec3::new(*dx, 0, *dz);
            let block = world
                .get_block(pos, Dimension::Overworld)
                .unwrap_or_default();

            let original_flag = *flag;
            *flag = !world.block_is_and::<FenceAndPaneBlock>(pos, |block| {
                block_is_pane ^ is_pane(&block.block_type)
            }) && block.is_solid();

            changed = changed || (original_flag != *flag);
        }

        changed
    }
}

pub fn is_pane(ty: &FenceAndPaneBlockType) -> bool {
    matches!(
        ty,
        FenceAndPaneBlockType::GlassPane
            | FenceAndPaneBlockType::RedStainedGlassPane
            | FenceAndPaneBlockType::OrangeStainedGlassPane
            | FenceAndPaneBlockType::YellowStainedGlassPane
            | FenceAndPaneBlockType::GreenStainedGlassPane
            | FenceAndPaneBlockType::BlueStainedGlassPane
            | FenceAndPaneBlockType::PurpleStainedGlassPane
            | FenceAndPaneBlockType::PinkStainedGlassPane
            | FenceAndPaneBlockType::MagentaStainedGlassPane
            | FenceAndPaneBlockType::LimeStainedGlassPane
            | FenceAndPaneBlockType::LightBlueStainedGlassPane
            | FenceAndPaneBlockType::WhiteStainedGlassPane
            | FenceAndPaneBlockType::LightGrayStainedGlassPane
            | FenceAndPaneBlockType::GrayStainedGlassPane
            | FenceAndPaneBlockType::BlackStainedGlassPane
            | FenceAndPaneBlockType::BrownStainedGlassPane
            | FenceAndPaneBlockType::CyanStainedGlassPane
    )
}
