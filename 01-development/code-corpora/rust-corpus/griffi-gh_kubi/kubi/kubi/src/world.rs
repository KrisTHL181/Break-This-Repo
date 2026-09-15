use nohash_hasher::BuildNoHashHasher;
use shipyard::{Unique, AllStoragesView};
use glam::IVec3;
use hashbrown::HashMap;
use anyhow::{Result, Context};

pub use kubi_shared::{worldgen, block::Block};

pub mod chunk;
pub mod tasks;
pub mod loading;
pub mod mesh;
pub mod neighbors;
pub mod raycast;
pub mod queue;

use chunk::{Chunk, ChunkMesh, CHUNK_SIZE};
use tasks::ChunkTaskManager;
use queue::BlockUpdateQueue;

#[derive(Default, Unique)]
pub struct ChunkStorage {
  pub chunks: HashMap<IVec3, Chunk>
}
impl ChunkStorage {
  pub const fn to_chunk_coords(position: IVec3) -> (IVec3, IVec3) {
    (
      IVec3::new(
        position.x.div_euclid(CHUNK_SIZE as i32),
        position.y.div_euclid(CHUNK_SIZE as i32),
        position.z.div_euclid(CHUNK_SIZE as i32),
      ),
      IVec3::new(
        position.x.rem_euclid(CHUNK_SIZE as i32),
        position.y.rem_euclid(CHUNK_SIZE as i32),
        position.z.rem_euclid(CHUNK_SIZE as i32),
      )
    )
  }
  pub fn get_block(&self, position: IVec3) -> Option<Block> {
    let (chunk, block) = Self::to_chunk_coords(position);
    let block = self.chunks
      .get(&chunk)?
      .block_data.as_ref()?
      .blocks.get(block.x as usize)?
      .get(block.y as usize)?
      .get(block.z as usize)?;
    Some(*block)
  }
  pub fn get_block_mut(&mut self, position: IVec3) -> Option<&mut Block> {
    let (chunk, block) = Self::to_chunk_coords(position);
    let block = self.chunks
      .get_mut(&chunk)?
      .block_data.as_mut()?
      .blocks.get_mut(block.x as usize)?
      .get_mut(block.y as usize)?
      .get_mut(block.z as usize)?;
    Some(block)
  }
  pub fn new() -> Self {
    Self::default()
  }
}

// #[derive(Unique)]
// pub struct WorldInfo {
//   pub seed: u32,
// }

#[derive(Default, Unique)]
pub struct ChunkMeshStorage {
  meshes: HashMap<usize, ChunkMesh, BuildNoHashHasher<usize>>,
  index: usize,
}
impl ChunkMeshStorage {
  pub fn new() -> Self {
    Self {
      meshes: HashMap::with_capacity_and_hasher(250, BuildNoHashHasher::default()),
      index: 0,
    }
  }
  pub fn insert(&mut self, mesh: ChunkMesh) -> usize {
    let index = self.index;
    debug_assert!(self.meshes.get(&index).is_none());
    unsafe {
      self.meshes.insert_unique_unchecked(index, mesh);
    }
    self.index += 1;
    index
  }
  pub fn update(&mut self, key: usize, mesh: ChunkMesh) -> Result<()> {
    *self.meshes.get_mut(&key).context("Chunk doesn't exist")? = mesh;
    Ok(())
  }
  pub fn remove(&mut self, key: usize) -> Result<()> {
    self.meshes.remove(&key).context("Chunk doesn't exist")?;
    Ok(())
  }
  pub fn get(&self, key: usize) -> Option<&ChunkMesh> {
    self.meshes.get(&key)
  }
}

pub fn init_game_world(
  storages: AllStoragesView,
) {
  log::info!("init_game_world called");
  storages.add_unique_non_send_sync(ChunkMeshStorage::new());
  storages.add_unique(ChunkStorage::new());
  storages.add_unique(ChunkTaskManager::new());
  storages.add_unique(BlockUpdateQueue::new());
}
