use crate::BlockBehavior;
use temper_block_data::PlacementContext;
use temper_blocks_generated::SimpleBlock;

impl BlockBehavior for SimpleBlock {
    fn is_solid(&self) -> bool {
        !matches!(self, SimpleBlock::Air | SimpleBlock::CaveAir)
    }

    fn can_be_replaced(&self, _context: PlacementContext) -> bool {
        matches!(self, SimpleBlock::Air | SimpleBlock::CaveAir)
    }
}
