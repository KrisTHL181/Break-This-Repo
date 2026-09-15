use glam::ivec3;
use crate::{block::Block, chunk::CHUNK_SIZE, worldgen::SeedThingy};
use super::{
  super::{WorldGenStep, WorldGenerator},
  _01_terrain::MIN_TERRAIN_HEIGHT,
};

pub const WATER_LEVEL: i32 = 0;

pub struct WaterStep;

impl WorldGenStep for WaterStep {
  fn initialize(_: &WorldGenerator, _: &mut SeedThingy) -> Self { Self }
  fn generate(&mut self, gen: &mut WorldGenerator) {
    // If chunk's lower bound is above water level, we can skip this step
    if gen.offset().y > WATER_LEVEL {
      return
    }
    // If upper bound is below terrain, skip this step
    if (gen.offset().y + CHUNK_SIZE as i32) < MIN_TERRAIN_HEIGHT {
      return
    }
    for x in 0..CHUNK_SIZE as i32 {
      for z in 0..CHUNK_SIZE as i32 {
        for y in 0..gen.local_height(WATER_LEVEL) {
          gen.place_if_empty(ivec3(x, y, z), Block::Water);
        }
      }
    }
  }
}
