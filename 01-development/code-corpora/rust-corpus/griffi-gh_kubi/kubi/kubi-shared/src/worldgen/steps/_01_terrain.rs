use fastnoise_lite::{FastNoiseLite, FractalType};
use glam::ivec3;
use crate::{block::Block, chunk::CHUNK_SIZE};
use super::super::{SeedThingy, WorldGenStep, WorldGenerator};

pub const MAX_TERAIN_HEIGHT: i32 = 32;
pub const MIN_TERRAIN_HEIGHT: i32 = -MAX_TERAIN_HEIGHT;

pub struct TerrainStep {
  noise: FastNoiseLite,
}

impl WorldGenStep for TerrainStep {
  fn initialize(_: &WorldGenerator, seeder: &mut SeedThingy) -> Self {
    let mut noise = FastNoiseLite::with_seed(seeder.next_seed());
    noise.set_fractal_type(Some(FractalType::FBm));
    noise.set_fractal_octaves(Some(4));
    noise.set_frequency(Some(0.003));
    Self { noise }
  }

  fn generate(&mut self, gen: &mut WorldGenerator) {
    let is_oob_upper = gen.offset().y > MAX_TERAIN_HEIGHT;
    if is_oob_upper { return }

    let is_oob_lower = (gen.offset().y + CHUNK_SIZE as i32) < MIN_TERRAIN_HEIGHT;
    if is_oob_lower {
      for x in 0..CHUNK_SIZE as i32 {
        for y in 0..CHUNK_SIZE as i32 {
          for z in 0..CHUNK_SIZE as i32 {
            gen.place(ivec3(x, y, z), Block::Stone);
          }
        }
      }
      return
    }

    let mut height_map = vec![vec![0; CHUNK_SIZE]; CHUNK_SIZE];
    for x in 0..CHUNK_SIZE as i32 {
      for z in 0..CHUNK_SIZE as i32 {
        let global_xz = gen.global_position(ivec3(x, 0, z));
        let height = (self.noise.get_noise_2d(global_xz.x as f64, global_xz.z as f64) * MAX_TERAIN_HEIGHT as f32) as i32;
        height_map[x as usize][z as usize] = height;
        for y in 0..gen.local_height(height) {
          gen.place(ivec3(x, y, z), Block::Stone);
        }
      }
    }
    gen.data.master_height_map = Some(height_map);
  }
}
