use std::sync::Arc;
use atomic::Atomic;
use bytemuck::{CheckedBitPattern, NoUninit};
use glam::IVec3;
use static_assertions::const_assert;
use crate::{
  block::Block,
  chunk::{BlockData, CHUNK_SIZE},
  queue::QueuedBlock,
};

pub mod steps;
pub mod structures;

#[repr(u8)]
#[derive(Clone, Copy, Debug, Default, NoUninit, CheckedBitPattern)]
pub enum AbortState {
  #[default]
  Continue,
  Abort,
  Aborted,
}
const_assert!(Atomic::<AbortState>::is_lock_free());

pub struct SeedThingy {
  pseed: u64,
  iseed: i32,
  iter: u8,
}

impl SeedThingy {
  pub fn new(seed: u64) -> Self {
    Self {
      pseed: seed,
      iseed: (seed & 0x7fffffffu64) as i32,
      iter: 0,
    }
  }

  pub fn next_seed(&mut self) -> i32 {
    self.iter += 1;
    self.iseed = (
      self.pseed
        .rotate_left((3 * self.iter) as _)
      & 0x7fffffff
    ) as i32;
    self.iseed
  }
}
trait WorldGenStep {
  fn initialize(generator: &WorldGenerator, seeder: &mut SeedThingy) -> Self;
  fn generate(&mut self, generator: &mut WorldGenerator);
}

macro_rules! run_steps {
  ($gen: expr, $abort: expr, [$($step:ty),* $(,)?]) => {
    (||{
      let _abort: ::std::sync::Arc<::atomic::Atomic<$crate::worldgen::AbortState>> =
        $abort.unwrap_or_else(|| ::std::sync::Arc::new(::atomic::Atomic::new($crate::worldgen::AbortState::Continue)));

      let _chkabt = || _abort.compare_exchange(
          $crate::worldgen::AbortState::Abort,
          $crate::worldgen::AbortState::Aborted,
          ::atomic::Ordering::Relaxed,
          ::atomic::Ordering::Relaxed
        ).is_ok();

      if _chkabt() { return false }

      let mut _seeder = $crate::worldgen::SeedThingy::new($gen.seed);
      $({
        let _ensure_ref: &mut $crate::worldgen::WorldGenerator = $gen;
        struct _Ensure0<T: $crate::worldgen::WorldGenStep>(T);
        type _Ensure1 = _Ensure0<$step>;
        let mut step: _Ensure1 = _Ensure0(<$step>::initialize(&*_ensure_ref, &mut _seeder));
        if _chkabt() { return false }
        step.0.generate(_ensure_ref);
        if _chkabt() { return false }
      })*

      true
    })()
  };
}

#[derive(Default)]
pub struct WorldGeneratorData {
  pub master_height_map: Option<Vec<Vec<i32>>>,
}

pub struct WorldGenerator {
  seed: u64,
  chunk_position: IVec3,
  blocks: BlockData,
  queue: Vec<QueuedBlock>,
  pub data: WorldGeneratorData,
}

impl WorldGenerator {
  fn offset(&self) -> IVec3 {
    self.chunk_position * CHUNK_SIZE as i32
  }

  fn query(&self, position: IVec3) -> Block {
    // let offset = self.offset();
    // let event_pos = offset + position;
    // if let Some(block) = self.queue.iter().find(|block| block.position == event_pos) {
    //   block.block_type
    // } else {
    //   self.blocks[position.x as usize][position.y as usize][position.z as usize]
    // }
    self.blocks[position.x as usize][position.y as usize][position.z as usize]
  }

  fn place(&mut self, position: IVec3, block: Block) {
    // let offset = self.offset();
    // let event_pos = offset + position;
    // self.queue.retain(|block: &QueuedBlock| {
    //   block.position != event_pos
    // });
    self.blocks[position.x as usize][position.y as usize][position.z as usize] = block;
  }

  fn place_if_empty(&mut self, position: IVec3, block: Block) {
    if self.query(position) == Block::Air {
      self.place(position, block);
    }
  }

  fn place_or_queue(&mut self, position: IVec3, block: Block) {
    let offset = self.offset();
    if position.to_array().iter().any(|&x| !(0..CHUNK_SIZE).contains(&(x as usize))) {
      let event_pos = offset + position;
      self.queue.retain(|block: &QueuedBlock| {
        block.position != event_pos
      });
      self.queue.push(QueuedBlock {
        position: event_pos,
        block_type: block,
        soft: true
      });
    } else {
      self.blocks[position.x as usize][position.y as usize][position.z as usize] = block;
    }
  }

  fn global_position(&self, position: IVec3) -> IVec3 {
    self.offset() + position
  }

  fn local_height(&self, height: i32) -> i32 {
    let offset = self.chunk_position * CHUNK_SIZE as i32;
    (height - offset.y).clamp(0, CHUNK_SIZE as i32)
  }

  fn local_y_position(&self, y: i32) -> Option<i32> {
    let offset = self.chunk_position * CHUNK_SIZE as i32;
    let position = y - offset.y;
    (0..CHUNK_SIZE as i32).contains(&position).then_some(position)
  }

  /// crude hash of self.seed and x
  fn seeded_hash(&self, x: impl std::hash::Hash) -> u64 {
    //use std::hash to hash the seed and x
    use std::hash::{Hash, Hasher};
    let mut hasher = std::collections::hash_map::DefaultHasher::new();
    x.hash(&mut hasher);
    self.seed.hash(&mut hasher);
    hasher.finish()
  }

  pub fn new(chunk_position: IVec3, seed: u64) -> Self {
    Self {
      seed,
      chunk_position,
      blocks: Box::new([[[Block::Air; CHUNK_SIZE]; CHUNK_SIZE]; CHUNK_SIZE]),
      queue: Vec::with_capacity(0),
      data: Default::default(),
    }
  }

  /// Generate the chunk.
  ///
  /// Will return `None` only if the generation was aborted.
  pub fn generate(mut self, abort: Option<Arc<Atomic<AbortState>>>) -> Option<(BlockData, Vec<QueuedBlock>)> {
    run_steps!(&mut self, abort, [
      steps::_01_terrain::TerrainStep,
      steps::_02_water::WaterStep,
      steps::_03_caves::CaveStep,
      steps::_04_layers::LayersStep,
      steps::_05_decorate::DecorateStep,
      steps::_06_trees::TreesStep,
    ]).then_some((self.blocks, self.queue))
  }
}

pub fn generate_world(chunk_position: IVec3, seed: u64, abort: Option<Arc<Atomic<AbortState>>>) -> Option<(BlockData, Vec<QueuedBlock>)> {
  //TODO: pass through None for abort
  WorldGenerator::new(chunk_position, seed).generate(abort)
}
