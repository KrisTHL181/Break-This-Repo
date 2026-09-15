use glam::{IVec3, ivec3};
use kubi_shared::{block::Block, chunk::CHUNK_SIZE, queue::QueuedBlock};
use shipyard::{UniqueViewMut, Unique};
use super::ChunkStorage;

#[derive(Unique, Default, Clone)]
#[repr(transparent)]
pub struct BlockUpdateQueue(pub Vec<QueuedBlock>);
impl BlockUpdateQueue {
  pub fn new() -> Self {
    Self::default()
  }
}

pub fn apply_queued_blocks(
  mut queue: UniqueViewMut<BlockUpdateQueue>,
  mut world: UniqueViewMut<ChunkStorage>
) {
  //maybe i need to check for desired/current state here before marking as  dirty?
  queue.0.retain(|&event| {
    if let Some(block) = world.get_block_mut(event.position) {
      if event.soft && *block != Block::Air {
        return false
      }
      if event.block_type == *block {
        return false
      }
      *block = event.block_type;
      //mark chunk as dirty
      let (chunk_pos, block_pos) = ChunkStorage::to_chunk_coords(event.position);
      let chunk = world.chunks.get_mut(&chunk_pos).expect("This error should never happen, if it does then something is super fucked up and the whole project needs to be burnt down.");
      chunk.mesh_dirty = true;
      chunk.data_modified = true;
      //If block pos is close to the border, some neighbors may be dirty!
      const DIRECTIONS: [IVec3; 6] = [
        ivec3(1,  0,  0),
        ivec3(-1, 0,  0),
        ivec3(0,  1,  0),
        ivec3(0, -1,  0),
        ivec3(0,  0,  1),
        ivec3(0,  0, -1),
      ];
      for direction in DIRECTIONS {
        let outside_chunk = |x| !(0..CHUNK_SIZE as i32).contains(x);
        let chunk_dirty = (block_pos + direction).to_array().iter().any(outside_chunk);
        if chunk_dirty {
          let dir_chunk_pos = chunk_pos + direction;
          if let Some(dir_chunk) = world.chunks.get_mut(&dir_chunk_pos) {
            dir_chunk.mesh_dirty = true;
          }
        }
      }
      return false
    }
    true
  });
}
