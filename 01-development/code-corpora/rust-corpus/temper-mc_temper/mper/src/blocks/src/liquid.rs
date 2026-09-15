use crate::BlockBehavior;
use temper_block_data::PlacementContext;
use temper_blocks_generated::LiquidBlock;

impl BlockBehavior for LiquidBlock {
    fn can_be_replaced(&self, _context: PlacementContext) -> bool {
        true
    }

    fn is_solid(&self) -> bool {
        false
    }
}
