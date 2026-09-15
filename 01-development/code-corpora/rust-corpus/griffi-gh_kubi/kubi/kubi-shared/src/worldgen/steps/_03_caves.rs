use fastnoise_lite::{FastNoiseLite, FractalType};
use glam::ivec3;
use crate::{block::Block, chunk::CHUNK_SIZE};
use super::{
  super::{SeedThingy, WorldGenStep, WorldGenerator},
  _01_terrain::MAX_TERAIN_HEIGHT,
};

pub struct CaveStep {
  a: FastNoiseLite,
  b: FastNoiseLite,
}

impl WorldGenStep for CaveStep {
  fn initialize(_: &WorldGenerator, seeder: &mut SeedThingy) -> Self {
    let mut a = FastNoiseLite::with_seed(seeder.next_seed());
    a.set_fractal_type(Some(FractalType::FBm));
    a.set_fractal_octaves(Some(2));

    let mut b = FastNoiseLite::with_seed(seeder.next_seed());
    b.set_fractal_type(Some(FractalType::FBm));
    b.set_fractal_octaves(Some(2));

    Self { a, b }
  }

  fn generate(&mut self, gen: &mut WorldGenerator) {
    // If chunk's lower bound is above max terrain height,
    // ...we can skip this step as caves cannot exist here
    if gen.offset().y > MAX_TERAIN_HEIGHT { return }

    for x in 0..CHUNK_SIZE as i32 {
      for y in 0..CHUNK_SIZE as i32 {
        for z in 0..CHUNK_SIZE as i32 {
          let cave_size = ((gen.offset().y + y - 50) as f64 / -200.).clamp(0., 1.) as f32;
          let inv_cave_size = 1. - cave_size;
          if cave_size < 0.1 { continue }

          let pos = ivec3(x, y, z);
          if gen.query(pos) != Block::Stone { continue }

          let pos_global = gen.global_position(pos);
          let noise_a = self.a.get_noise_3d(pos_global.x as f64, pos_global.y as f64, pos_global.z as f64) * 0.5 + 0.5;
          let noise_b = self.b.get_noise_3d(pos_global.x as f64, pos_global.y as f64, pos_global.z as f64) * 0.5 + 0.5;

          if noise_a.min(noise_b) > (0.6 + 0.4 * inv_cave_size) {
            gen.place(pos, Block::Air);
          }
          //TODO
        }
      }
    }
  }
}
