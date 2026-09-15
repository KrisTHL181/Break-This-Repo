use crate::World;
use temper_core::block_state_id::BlockStateId;
use temper_core::dimension::Dimension;
use temper_core::pos::BlockPos;
use temper_world_format::errors::WorldError;

impl World {
    /// Attempts to get a block in the world. Returns an error if the chunk is not accessible
    pub fn get_block(
        &self,
        pos: BlockPos,
        dimension: Dimension,
    ) -> Result<BlockStateId, WorldError> {
        self.get_chunk(pos.chunk(), dimension)
            .map(|chunk| chunk.get_block(pos.chunk_block_pos()))
    }

    /// Attempts to set a block in the world. Returns an error if the chunk is not mutable
    pub fn set_block(
        &self,
        pos: BlockPos,
        dimension: Dimension,
        block_state_id: BlockStateId,
    ) -> Result<(), WorldError> {
        self.get_chunk_mut(pos.chunk(), dimension)
            .map(|mut chunk| chunk.set_block(pos.chunk_block_pos(), block_state_id))
    }
}
