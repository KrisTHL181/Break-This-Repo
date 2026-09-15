use crate::BlockBehavior;
use crate::behavior_trait::BlockDispatch;
use temper_core::block_state_id::BlockStateId;
use temper_core::dimension::Dimension;
use temper_core::pos::BlockPos;
use temper_world::{World, WorldError};

// TODO: add dimensions to all these functions

/// Trait to add useful functions to the World struct
pub trait WorldBlockUpdates {
    fn update_block_cast<T: BlockBehavior>(
        &self,
        block_pos: BlockPos,
        callback: impl Fn(&mut T),
    ) -> Result<BlockStateId, WorldError>;

    fn block_is<T: BlockBehavior>(&self, block_pos: BlockPos) -> bool;
    fn block_is_and<T: BlockBehavior>(
        &self,
        block_pos: BlockPos,
        callback: impl Fn(T) -> bool,
    ) -> bool;

    fn try_get_block<T: BlockBehavior>(
        &self,
        block_pos: BlockPos,
        dimension: Dimension,
    ) -> Option<T>;
}

impl WorldBlockUpdates for World {
    fn update_block_cast<T: BlockBehavior>(
        &self,
        block_pos: BlockPos,
        callback: impl Fn(&mut T),
    ) -> Result<BlockStateId, WorldError> {
        self.get_chunk(block_pos.chunk(), Dimension::Overworld)
            .and_then(|chunk| {
                let id = chunk.get_block(block_pos.chunk_block_pos());
                let mut id = T::try_from(chunk.get_block(block_pos.chunk_block_pos()).raw())
                    .map_err(|_| WorldError::InvalidBlock(id))?;
                callback(&mut id);

                id.try_into()
                    .map(BlockStateId::new)
                    .map_err(|_| WorldError::InvalidBlock(BlockStateId::default()))
            })
    }

    fn block_is<T: BlockBehavior>(&self, block_pos: BlockPos) -> bool {
        self.get_chunk(block_pos.chunk(), Dimension::Overworld)
            .map(|chunk| T::try_from(chunk.get_block(block_pos.chunk_block_pos()).raw()).is_ok())
            .unwrap_or_default()
    }

    fn block_is_and<T: BlockBehavior>(
        &self,
        block_pos: BlockPos,
        callback: impl Fn(T) -> bool,
    ) -> bool {
        self.get_chunk(block_pos.chunk(), Dimension::Overworld)
            .and_then(|chunk| {
                let block_id = chunk.get_block(block_pos.chunk_block_pos());
                let block =
                    T::try_from(block_id.raw()).map_err(|_| WorldError::InvalidBlock(block_id))?;
                Ok(callback(block))
            })
            .unwrap_or_default()
    }

    fn try_get_block<T: BlockBehavior>(
        &self,
        block_pos: BlockPos,
        dimension: Dimension,
    ) -> Option<T> {
        self.get_chunk(block_pos.chunk(), dimension)
            .map(|c| c.get_block(block_pos.chunk_block_pos()))
            .unwrap_or(BlockStateId::new(0))
            .try_cast::<T>()
    }
}
