use glam::ivec3;
use crate::{block::Block, chunk::CHUNK_SIZE, worldgen::SeedThingy};
use super::{
  _02_water::WATER_LEVEL,
  super::{WorldGenStep, WorldGenerator},
};

pub struct DecorateStep;

impl WorldGenStep for DecorateStep {
  fn initialize(_: &WorldGenerator, _: &mut SeedThingy) -> Self { Self }

  fn generate(&mut self, gen: &mut WorldGenerator) {
    if gen.data.master_height_map.is_none() { return }

    for x in 0..CHUNK_SIZE as i32 {
      for z in 0..CHUNK_SIZE as i32 {
        let global_xz = gen.global_position(ivec3(x, 0, z));

        let terrain_height = gen.data.master_height_map.as_ref().unwrap()[x as usize][z as usize];

        //Place tall grass
        if terrain_height >= WATER_LEVEL {
          if let Some(local_y) = gen.local_y_position(terrain_height) {
            if (gen.seeded_hash((global_xz.x, global_xz.z, 0x050)) & 0xf) == 0xf {
              gen.place_if_empty(ivec3(x, local_y, z), Block::TallGrass);
            }
          }
        }
      }
    }
  }
}
