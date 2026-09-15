use crate::block::Block;

pub const CHUNK_SIZE: usize = 32;
pub type BlockData = Box<[[[Block; CHUNK_SIZE]; CHUNK_SIZE]; CHUNK_SIZE]>;
pub type BlockDataRef = [[[Block; CHUNK_SIZE]; CHUNK_SIZE]; CHUNK_SIZE];
