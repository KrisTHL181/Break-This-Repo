use fastnoise_lite::{FastNoiseLite, NoiseType};
use glam::ivec3;
use crate::{chunk::CHUNK_SIZE, worldgen::SeedThingy};
use super::_02_water::WATER_LEVEL;
use crate::worldgen::{
  WorldGenStep, WorldGenerator,
  structures::{Structure, TreeStructure},
};


pub struct TreesStep {
  density_noise: FastNoiseLite,
}

impl WorldGenStep for TreesStep {
  fn initialize(_: &WorldGenerator, seeder: &mut SeedThingy) -> Self {
    let mut density_noise = FastNoiseLite::with_seed(seeder.next_seed());
    density_noise.set_noise_type(Some(NoiseType::OpenSimplex2));
    density_noise.set_frequency(Some(0.008));
    Self { density_noise }
  }

  fn generate(&mut self, gen: &mut WorldGenerator) {
    if gen.data.master_height_map.is_none() { return }

    for x in 0..CHUNK_SIZE as i32 {
      for z in 0..CHUNK_SIZE as i32 {
        let terrain_height = gen.data.master_height_map.as_ref().unwrap()[x as usize][z as usize];
        if terrain_height < WATER_LEVEL { continue }

        let global_xz = gen.global_position(ivec3(x, 0, z));
        let mut density = self.density_noise.get_noise_2d(global_xz.x as f64, global_xz.z as f64) * 0.5 + 0.5;
        density = density.powi(3);
        if gen.seeded_hash((global_xz.x, global_xz.z, 0x060)) & 0xff >= (density * 7.).round() as u64 {
          continue
        }

        let tree = TreeStructure::default();
        if let Some(local_y) = gen.local_y_position(terrain_height) {
          tree.place(gen, ivec3(x, local_y, z));
        }
      }
    }
  }
}
