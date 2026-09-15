use crate::NormalGenerator;
use crate::terrain::{NoiseGenerator, smoothstep, trilerp};
use gen_core::{GenerationError, StageInput};
use temper_core::block_state_id::BlockStateId;
use temper_core::pos::ChunkBlockPos;
use temper_macros::{block, match_block};

impl NormalGenerator {
    pub(crate) fn generate_carvers(&self, input: StageInput<'_>) -> Result<(), GenerationError> {
        let noise = NoiseGenerator::new(self.seed);

        generate_caves(input, &noise);

        Ok(())
    }
}

fn generate_caves(input: StageInput<'_>, noise: &NoiseGenerator) {
    const STEP_XZ: i32 = 4;
    const STEP_Y: i32 = 8;
    const Y_MIN: i32 = -60;
    const Y_MAX: i32 = 100;

    let y_len = Y_MAX - Y_MIN;
    let gx = (16 / STEP_XZ + 1) as usize;
    let gz = (16 / STEP_XZ + 1) as usize;
    let gy = (y_len / STEP_Y + 1) as usize;

    let mut grid = vec![0.0f64; gx * gy * gz];
    let idx = |ix: usize, iy: usize, iz: usize| -> usize { (iy * gz + iz) * gx + ix };

    for ix in 0..gx {
        for iz in 0..gz {
            for iy in 0..gy {
                let x = (ix as i32) * STEP_XZ;
                let z = (iz as i32) * STEP_XZ;
                let y = Y_MIN + (iy as i32) * STEP_Y;

                let world_x = input.pos.x() * 16 + x;
                let world_z = input.pos.z() * 16 + z;

                grid[idx(ix, iy, iz)] = noise.get_cave_noise(
                    f64::from(world_x) / 2.0,
                    f64::from(y) / 2.0,
                    f64::from(world_z) / 2.0,
                );
            }
        }
    }

    for x in 0..16i32 {
        for z in 0..16i32 {
            let base_ix = (x / STEP_XZ) as usize;
            let base_iz = (z / STEP_XZ) as usize;

            let tx = smoothstep(f64::from(x % STEP_XZ) / f64::from(STEP_XZ));
            let tz = smoothstep(f64::from(z % STEP_XZ) / f64::from(STEP_XZ));

            for y in Y_MIN..Y_MAX {
                let yy = y - Y_MIN;
                let base_iy = (yy / STEP_Y) as usize;
                let ty = smoothstep(f64::from(yy % STEP_Y) / f64::from(STEP_Y));

                let ix0 = base_ix;
                let ix1 = (base_ix + 1).min(gx - 1);
                let iz0 = base_iz;
                let iz1 = (base_iz + 1).min(gz - 1);
                let iy0 = base_iy;
                let iy1 = (base_iy + 1).min(gy - 1);

                let c000 = grid[idx(ix0, iy0, iz0)];
                let c100 = grid[idx(ix1, iy0, iz0)];
                let c010 = grid[idx(ix0, iy1, iz0)];
                let c110 = grid[idx(ix1, iy1, iz0)];
                let c001 = grid[idx(ix0, iy0, iz1)];
                let c101 = grid[idx(ix1, iy0, iz1)];
                let c011 = grid[idx(ix0, iy1, iz1)];
                let c111 = grid[idx(ix1, iy1, iz1)];

                let cave_noise =
                    trilerp(c000, c100, c010, c110, c001, c101, c011, c111, tx, ty, tz);

                if cave_noise <= 0.6 {
                    continue;
                }

                let pos = ChunkBlockPos::new(x as u8, y as i16, z as u8);
                let current_block = input.target.get_block(pos);
                let above =
                    input
                        .target
                        .get_block(ChunkBlockPos::new(x as u8, (y + 1) as i16, z as u8));

                if match_block!("air", current_block)
                    || match_block!("cave_air", current_block)
                    || match_block!("water", current_block)
                    || match_block!("water", above)
                {
                    continue;
                }

                input.target.set_block(pos, block!("air"));
            }
        }
    }
}
