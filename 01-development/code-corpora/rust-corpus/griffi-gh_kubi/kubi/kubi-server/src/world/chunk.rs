use hashbrown::HashSet;
use nohash_hasher::BuildNoHashHasher;
use kubi_shared::{
  chunk::BlockData, 
  networking::client::ClientId
};

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum ChunkState {
  Nothing,
  Loading,
  Loaded,
}

pub struct Chunk {
  pub state: ChunkState,
  pub blocks: Option<BlockData>,
  pub subscriptions: HashSet<ClientId, BuildNoHashHasher<ClientId>>,
  pub data_modified: bool,
}

impl Chunk {
  pub fn new() -> Self {
    Self {
      state: ChunkState::Nothing,
      blocks: None,
      subscriptions: HashSet::with_capacity_and_hasher(4, BuildNoHashHasher::default()),
      data_modified: false,
    }
  }
}
