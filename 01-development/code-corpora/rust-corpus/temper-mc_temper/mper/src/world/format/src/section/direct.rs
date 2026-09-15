use crate::section::paletted::PalettedSection;
use crate::section::uniform::UniformSection;
use crate::section::{AIR, CHUNK_SECTION_LENGTH};
use deepsize::DeepSizeOf;
use serde_derive::{Deserialize, Serialize};
use temper_core::block_state_id::BlockStateId;
use temper_macros::match_block;
use type_hash::TypeHash;

// Currently there are less block state ids than u16::MAX, so we can store ids as u16s to cut down on memory usage
type CompactBlockStateId = u16;

const AIR_COMPACT: CompactBlockStateId = AIR.raw() as CompactBlockStateId;

#[derive(Clone, DeepSizeOf, Serialize, Deserialize, TypeHash)]
pub struct DirectSection(pub(crate) Box<[CompactBlockStateId]>, u16);

impl Default for DirectSection {
    fn default() -> Self {
        Self(
            vec![AIR_COMPACT; CHUNK_SECTION_LENGTH].into_boxed_slice(),
            0,
        )
    }
}

impl DirectSection {
    #[inline]
    pub fn set_block(&mut self, idx: usize, block: BlockStateId) {
        if self.0[idx] == AIR_COMPACT && block != AIR {
            self.1 += 1
        } else if self.0[idx] != AIR_COMPACT && block == AIR {
            self.1 -= 1
        }

        self.0[idx] = block.raw() as CompactBlockStateId;
    }

    #[inline]
    pub fn get_block(&self, idx: usize) -> BlockStateId {
        BlockStateId::new(self.0[idx].into())
    }

    pub fn block_count(&self) -> u16 {
        self.1
    }

    pub fn fluid_count(&self) -> u16 {
        self.0
            .iter()
            .filter(|&&id| {
                let block_id = BlockStateId::new(id.into());
                match_block!("water", block_id) || match_block!("lava", block_id)
            })
            .count() as u16
    }
}

impl From<&mut UniformSection> for DirectSection {
    fn from(s: &mut UniformSection) -> Self {
        Self(
            vec![s.get_block().raw() as CompactBlockStateId; CHUNK_SECTION_LENGTH]
                .into_boxed_slice(),
            if s.get_block() == AIR { 0 } else { 4096 },
        )
    }
}

impl From<&mut PalettedSection> for DirectSection {
    fn from(s: &mut PalettedSection) -> Self {
        let mut vec = vec![AIR_COMPACT; CHUNK_SECTION_LENGTH];
        let mut count = 0;

        for (block_idx, val) in vec.iter_mut().enumerate() {
            let block = s.get_block(block_idx);
            *val = s.get_block(block_idx).raw() as CompactBlockStateId;

            if block != AIR {
                count += 1
            }
        }

        Self(vec.into_boxed_slice(), count)
    }
}
