use crate::NormalGenerator;
use crate::terrain::{NoiseGenerator, bilerp, dither_field, smoothstep};
use gen_core::{GenerationError, StageInput};
use rand::seq::IndexedRandom;
use temper_core::block_state_id::BlockStateId;
use temper_core::pos::{ChunkBlockPos, ChunkPos};
use temper_macros::block;

const FLOWER_CHANCE: f64 = 0.03;
const HEIGHTMAP_STEP_XZ: i32 = 4;
const HEIGHT_SCALE: f64 = 64.0;
const HEIGHT_OFFSET: i32 = 68;

const FLOWERS: [BlockStateId; 12] = [
    block!("allium"),
    block!("azure_bluet"),
    block!("blue_orchid"),
    block!("cornflower"),
    block!("dandelion"),
    block!("lily_of_the_valley"),
    block!("oxeye_daisy"),
    block!("poppy"),
    block!("orange_tulip"),
    block!("pink_tulip"),
    block!("red_tulip"),
    block!("white_tulip"),
];

impl NormalGenerator {
    pub(crate) fn generate_surface(&self, input: StageInput<'_>) -> Result<(), GenerationError> {
        let noise = NoiseGenerator::new(self.seed);
        generate_plains(input, &noise);
        Ok(())
    }
}

fn build_heightmap_interpolated(pos: ChunkPos, noise: &NoiseGenerator) -> [i32; 16 * 16] {
    let gx = (16 / HEIGHTMAP_STEP_XZ + 1) as usize;
    let gz = (16 / HEIGHTMAP_STEP_XZ + 1) as usize;

    let idx = |ix: usize, iz: usize| -> usize { iz * gx + ix };

    let mut grid = vec![0.0f64; gx * gz];

    for ix in 0..gx {
        for iz in 0..gz {
            let lx = (ix as i32) * HEIGHTMAP_STEP_XZ;
            let lz = (iz as i32) * HEIGHTMAP_STEP_XZ;

            let world_x = pos.x() * 16 + lx;
            let world_z = pos.z() * 16 + lz;

            grid[idx(ix, iz)] = noise.get_noise(f64::from(world_x), f64::from(world_z));
        }
    }

    let mut out = [0i32; 16 * 16];

    for x in 0..16i32 {
        for z in 0..16i32 {
            let base_ix = (x / HEIGHTMAP_STEP_XZ) as usize;
            let base_iz = (z / HEIGHTMAP_STEP_XZ) as usize;

            let tx = smoothstep(f64::from(x % HEIGHTMAP_STEP_XZ) / f64::from(HEIGHTMAP_STEP_XZ));
            let tz = smoothstep(f64::from(z % HEIGHTMAP_STEP_XZ) / f64::from(HEIGHTMAP_STEP_XZ));

            let ix0 = base_ix;
            let ix1 = (base_ix + 1).min(gx - 1);
            let iz0 = base_iz;
            let iz1 = (base_iz + 1).min(gz - 1);

            let c00 = grid[idx(ix0, iz0)];
            let c10 = grid[idx(ix1, iz0)];
            let c01 = grid[idx(ix0, iz1)];
            let c11 = grid[idx(ix1, iz1)];

            let height = bilerp(c00, c10, c01, c11, tx, tz);

            out[(z as usize) * 16 + (x as usize)] = (height * HEIGHT_SCALE) as i32 + HEIGHT_OFFSET;
        }
    }

    out
}

fn generate_plains(input: StageInput<'_>, noise: &NoiseGenerator) {
    let stone = block!("stone");

    for section_y in -4..4 {
        input
            .target
            .fill_section(section_y as i8, block!("water", {level: 0}));
    }

    let heights = build_heightmap_interpolated(input.pos, noise);

    let mut y_min = i32::MAX;
    for &height in &heights {
        y_min = y_min.min(height);
    }

    let highest_full_section = y_min.div_euclid(16);
    for section_y in -4..highest_full_section {
        input.target.fill_section(section_y as i8, stone);
    }

    let above_filled_sections = highest_full_section * 16 - 1;

    for chunk_x in 0..16i32 {
        for chunk_z in 0..16i32 {
            let height = heights[(chunk_z as usize) * 16 + (chunk_x as usize)];

            if height <= above_filled_sections {
                continue;
            }

            let fill = height - above_filled_sections;
            let global_x = input.pos.x() * 16 + chunk_x;
            let global_z = input.pos.z() * 16 + chunk_z;

            let d = dither_field(noise.seed, global_x, global_z, 16);
            let wobble = ((d * 2.0) - 1.0) * 2.0;

            for dy in 0..fill {
                let y = above_filled_sections + dy;
                let dithered_y = y + wobble.round() as i32;
                let pos = ChunkBlockPos::new(chunk_x as u8, y as i16, chunk_z as u8);

                if dithered_y <= 64 {
                    input
                        .target
                        .set_block_without_heightmap(pos, block!("sand"));
                } else if dithered_y >= 80 {
                    input.target.set_block_without_heightmap(pos, stone);
                } else if dy == fill - 1 {
                    input
                        .target
                        .set_block_without_heightmap(pos, block!("grass_block", {snowy: false}));

                    if rand::random_bool(FLOWER_CHANCE) {
                        let flower = FLOWERS.choose(&mut rand::rng()).unwrap();
                        input.target.set_block_without_heightmap(
                            ChunkBlockPos::new(chunk_x as u8, (y + 1) as i16, chunk_z as u8),
                            *flower,
                        );
                    }
                } else {
                    input
                        .target
                        .set_block_without_heightmap(pos, block!("dirt"));
                }
            }
        }
    }

    input.target.recalculate_heightmap();
}

#[cfg(test)]
mod tests {
    use gen_core::{GenStage, StageInput, StageNeighborhood};
    use temper_core::pos::ChunkPos;
    use temper_world_format::Chunk;

    use super::*;

    fn generate_surface(seed: u64, pos: ChunkPos) -> Chunk {
        let generator = NormalGenerator::new(seed);
        let mut chunk = Chunk::new_empty();

        generator
            .generate_surface(StageInput::new(
                pos,
                GenStage::SURFACE,
                &mut chunk,
                StageNeighborhood::empty(),
            ))
            .expect("normal surface generation should succeed");

        chunk
    }

    #[test]
    fn generates_origin_chunk() {
        generate_surface(0, ChunkPos::new(0, 0));
    }

    #[test]
    fn generated_chunks_count_water_for_motion_blocking() {
        let chunk = generate_surface(0, ChunkPos::new(0, 0));

        for x in 0..16 {
            for z in 0..16 {
                assert!(chunk.heightmaps.motion_blocking.get_height(x, z) >= 63);
            }
        }
    }

    #[test]
    fn generates_high_coordinates() {
        generate_surface(0, ChunkPos::new((1 << 22) - 1, (1 << 22) - 1));
        generate_surface(0, ChunkPos::new(-((1 << 22) - 1), -((1 << 22) - 1)));
    }
}
