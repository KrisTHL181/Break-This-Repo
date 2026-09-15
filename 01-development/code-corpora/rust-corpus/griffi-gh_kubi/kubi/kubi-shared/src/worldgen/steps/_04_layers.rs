use glam::ivec3;
use crate::{block::Block, chunk::CHUNK_SIZE, worldgen::SeedThingy};
use super::{
  _02_water::WATER_LEVEL,
  super::{WorldGenStep, WorldGenerator}
};

pub struct LayersStep;

impl WorldGenStep for LayersStep {
  fn initialize(_: &WorldGenerator, _: &mut SeedThingy) -> Self { Self }

  fn generate(&mut self, gen: &mut WorldGenerator) {
    if gen.data.master_height_map.is_none() { return }

    for x in 0..CHUNK_SIZE as i32 {
      for z in 0..CHUNK_SIZE as i32 {
        let terrain_height = gen.data.master_height_map.as_ref().unwrap()[x as usize][z as usize];

        // Dirt layer height, naturally gets thinner as height gets deeper
        let mut dirt_layer_height = (((terrain_height as f32 + 15.) / 20.).clamp(0., 1.) * 8.).round() as i32;
        dirt_layer_height -= (gen.seeded_hash((x, z, 0x040)) & 1) as i32; //+ (gen.seeded_hash((x, z, 0x041)) & 1) as i32;

        // Place dirt layer
        for y in gen.local_height(terrain_height - dirt_layer_height)..gen.local_height(terrain_height) {
          gen.place(ivec3(x, y, z), Block::Dirt);
        }

        // If above water level, place grass
        if terrain_height >= WATER_LEVEL {
          if let Some(local_y) = gen.local_y_position(terrain_height - 1) {
            gen.place(ivec3(x, local_y, z), Block::Grass);
          }
        }
      }
    }
  }
}
