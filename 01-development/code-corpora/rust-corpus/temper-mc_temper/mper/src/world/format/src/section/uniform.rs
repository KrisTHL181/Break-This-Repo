use crate::BlockStateId;
use crate::section::AIR;
use deepsize::DeepSizeOf;
use serde_derive::{Deserialize, Serialize};
use type_hash::TypeHash;

#[derive(Clone, DeepSizeOf, Serialize, Deserialize, TypeHash)]
pub struct UniformSection(BlockStateId);

impl UniformSection {
    pub fn air() -> Self {
        Self(AIR)
    }

    pub fn new_with(id: BlockStateId) -> Self {
        Self(id)
    }

    #[inline]
    pub fn get_block(&self) -> BlockStateId {
        self.0
    }

    pub fn fill(&mut self, id: BlockStateId) {
        self.0 = id;
    }
}
