use crate::block_data::BlockData;
use ahash::RandomState;
use deepsize::DeepSizeOf;
use once_cell::sync::OnceCell;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::fmt::Display;
use std::str::FromStr;
use temper_codec::net_types::var_int::VarInt;
use tracing::warn;
use type_hash::TypeHash;

const BLOCKSFILE: &str = temper_assets::generated::BLOCKSTATES;

pub static ID2BLOCK: OnceCell<Vec<BlockData>> = OnceCell::new();
pub static BLOCK2ID: OnceCell<HashMap<BlockData, i32, RandomState>> = OnceCell::new();

pub fn create_block_mappings() -> (Vec<BlockData>, HashMap<BlockData, i32, RandomState>) {
    let string_keys: HashMap<String, BlockData, RandomState> =
        serde_json::from_str(BLOCKSFILE).unwrap();

    let block_entries = string_keys
        .keys()
        .map(|key| key.parse::<usize>().unwrap())
        .max()
        .map(|max_id| max_id + 1)
        .unwrap_or_default();

    if string_keys.len() != block_entries {
        warn!(
            "Block mappings file has {} entries, but the highest id needs {} slots",
            string_keys.len(),
            block_entries
        );
    }

    let mut id2block = Vec::with_capacity(block_entries);
    for _ in 0..block_entries {
        id2block.push(BlockData::default());
    }
    string_keys
        .iter()
        .map(|(k, v)| (k.parse::<i32>().unwrap(), v.clone()))
        .for_each(|(k, v)| id2block[k as usize] = v);
    let block2id: HashMap<BlockData, i32, RandomState> = id2block
        .iter()
        .enumerate()
        .map(|(k, v)| (v.clone(), k as i32))
        .collect();
    (id2block, block2id)
}

/// An ID for a block, and it's state in the world. Use this over `BlockData` unless you need to
/// modify or read the block's name/properties directly.
///
/// This should be used over `BlockData` in most cases, as it's much more efficient to store and pass around.
/// You can also generate a block's id at runtime with the [temper_macros::block!] macro.
#[derive(Copy, Clone, Debug, Eq, PartialEq, Hash, Serialize, Deserialize, DeepSizeOf, TypeHash)]
pub struct BlockStateId(u32);

impl BlockStateId {
    /// Do NOT use this by yourself. Instead use the block macro `block!("stone")` the item to block
    /// map
    pub const fn new(id: u32) -> Self {
        Self(id)
    }

    /// Given a BlockData, return a BlockStateId. Does not clone, should be quite fast.
    pub fn from_block_data(block_data: &BlockData) -> Self {
        let id = BLOCK2ID
            .get_or_init(|| create_block_mappings().1)
            .get(block_data)
            .copied()
            .unwrap_or_else(|| {
                warn!("Block data '{block_data}' not found in block mappings file");
                0
            });
        BlockStateId(id as u32)
    }

    /// Given a block state ID, return a BlockData. Will clone, so don't use in hot loops.
    /// If the ID is not found, returns None.
    pub fn to_block_data(&self) -> Option<BlockData> {
        ID2BLOCK
            .get_or_init(|| create_block_mappings().0)
            .get(self.0 as usize)
            .cloned()
    }

    pub fn from_varint(var_int: VarInt) -> Self {
        BlockStateId(var_int.0 as u32)
    }

    pub fn to_varint(&self) -> VarInt {
        VarInt(self.0 as i32)
    }

    /// Do Not use this by yourself. This is only useful for apis that use this as an index or key
    /// to get additionally information about this state.
    pub const fn raw(&self) -> u32 {
        self.0
    }
}

impl Display for BlockStateId {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        if let Some(block_data) = self.to_block_data() {
            write!(f, "BlockStateId({}: {:?})", self.0, block_data)
        } else {
            write!(f, "BlockStateId({}: Unknown)", self.0)
        }
    }
}

impl BlockData {
    /// Converts a BlockData to a BlockStateId. Will panic if the ID is not found.
    pub fn to_block_state_id(&self) -> BlockStateId {
        BlockStateId::from_block_data(self)
    }

    /// Converts a BlockStateId to a BlockData. Will panic if the ID is not found.
    pub fn from_block_state_id(block_state_id: BlockStateId) -> BlockData {
        block_state_id
            .to_block_data()
            .expect("Block state ID not found in block mappings file")
    }

    pub fn try_to_block_state_id(&self) -> Option<BlockStateId> {
        Some(BlockStateId::from_block_data(self))
    }
}
impl From<BlockData> for BlockStateId {
    fn from(block_data: BlockData) -> Self {
        BlockStateId::from_block_data(&block_data)
    }
}
impl From<BlockStateId> for BlockData {
    /// Converts a BlockStateId to a BlockData. Will panic if the ID is not found.
    fn from(block_state_id: BlockStateId) -> Self {
        block_state_id
            .to_block_data()
            .expect("Block state ID not found in block mappings file")
    }
}

impl From<VarInt> for BlockStateId {
    /// Converts a VarInt to a BlockStateId. Probably a no-op, but included for completeness.
    fn from(var_int: VarInt) -> Self {
        Self(var_int.0 as u32)
    }
}

impl From<BlockStateId> for VarInt {
    /// Converts a BlockStateId to a VarInt. Probably a no-op, but included for completeness.
    fn from(block_state_id: BlockStateId) -> Self {
        VarInt(block_state_id.0 as i32)
    }
}

impl Default for BlockStateId {
    /// Returns a BlockStateId with ID 0, which is air.
    fn default() -> Self {
        Self(0)
    }
}

const ITEM_TO_BLOCK_MAPPING_FILE: &str = temper_assets::generated::ITEM_TO_BLOCK_MAPPING;
pub static ITEM_TO_BLOCK_MAPPING: OnceCell<HashMap<i32, BlockStateId>> = OnceCell::new();

pub fn create_item_to_block_mapping() -> HashMap<i32, BlockStateId> {
    let str_form: HashMap<String, String> = serde_json::from_str(ITEM_TO_BLOCK_MAPPING_FILE)
        .expect("Failed to parse item_to_block_mapping.json");
    str_form
        .into_iter()
        .map(|(k, v)| {
            (
                i32::from_str(&k).unwrap(),
                BlockStateId::new(u32::from_str(&v).unwrap()),
            )
        })
        .collect()
}
