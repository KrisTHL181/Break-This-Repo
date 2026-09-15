use std::collections::HashMap;

use bevy_math::I8Vec3;
use temper_core::block_state_id::BlockStateId;
use temper_core::pos::BlockPos;
use temper_macros::block;

pub fn generate_lake(root_pos: BlockPos, seed: u64) -> HashMap<I8Vec3, BlockStateId> {
    let mut blocks = HashMap::new();
    let x_radius = rand_inclusive(root_pos, seed, 0, 6, 10);
    let z_radius = rand_inclusive(root_pos, seed, 1, 5, 9);
    let max_depth = rand_inclusive(root_pos, seed, 2, 3, 4);
    let beach_width = rand_inclusive(root_pos, seed, 3, 2, 3);

    for x in -(x_radius + beach_width)..=(x_radius + beach_width) {
        for z in -(z_radius + beach_width)..=(z_radius + beach_width) {
            let offset = I8Vec3::new(x, 0, z);
            let distance = lake_distance(x, z, x_radius, z_radius);

            if distance <= 1.0 {
                let depth = lake_depth(root_pos, seed, offset, distance, max_depth);
                let bottom_y = -depth;
                blocks.insert(offset + I8Vec3::Y, block!("air"));

                for y in -max_depth..=bottom_y {
                    blocks.insert(I8Vec3::new(x, y, z), block!("sand"));
                }

                for y in (bottom_y + 1)..=0 {
                    blocks.insert(I8Vec3::new(x, y, z), block!("water", {level: 0}));
                }
            } else if distance <= beach_distance(x_radius, z_radius, beach_width) {
                blocks.insert(offset, block!("sand"));
                blocks.insert(offset - I8Vec3::Y, block!("sand"));
            }
        }
    }

    blocks
}

fn lake_depth(root_pos: BlockPos, seed: u64, offset: I8Vec3, distance: f32, max_depth: i8) -> i8 {
    let cell_pos = root_pos + offset.as_ivec3();
    let depth_noise = rand_inclusive(cell_pos, seed, 10, 0, 1);
    let shore_distance = distance.sqrt().clamp(0.0, 1.0);
    let depth = 1 + ((1.0 - shore_distance) * f32::from(max_depth - 1)).round() as i8;

    (depth + depth_noise).clamp(1, max_depth)
}

fn lake_distance(x: i8, z: i8, x_radius: i8, z_radius: i8) -> f32 {
    let x = f32::from(x) / f32::from(x_radius);
    let z = f32::from(z) / f32::from(z_radius);

    x * x + z * z
}

fn beach_distance(x_radius: i8, z_radius: i8, beach_width: i8) -> f32 {
    let x = f32::from(x_radius + beach_width) / f32::from(x_radius);
    let z = f32::from(z_radius + beach_width) / f32::from(z_radius);

    x.min(z).powi(2)
}

fn rand_inclusive(root_pos: BlockPos, seed: u64, salt: u64, min: i8, max: i8) -> i8 {
    let range = u64::from((max - min + 1) as u8);
    let value = rand(root_pos, seed, salt) % range;

    min + value as i8
}

fn rand(root_pos: BlockPos, seed: u64, salt: u64) -> u64 {
    root_pos.deterministic_rand(seed ^ salt_hash(salt))
}

fn salt_hash(salt: u64) -> u64 {
    0x9e37_79b9_7f4a_7c15u64.wrapping_mul(salt + 1)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn lake_generation_is_deterministic_for_the_same_root() {
        let root_pos = BlockPos::of(4, 64, 8);

        assert_eq!(generate_lake(root_pos, 300), generate_lake(root_pos, 300));
    }

    #[test]
    fn lake_generation_varies_between_roots() {
        let first = generate_lake(BlockPos::of(4, 64, 8), 300);
        let second = generate_lake(BlockPos::of(12, 64, 11), 300);

        assert_ne!(first, second);
    }

    #[test]
    fn lake_generation_contains_water_and_clearing_air() {
        let lake = generate_lake(BlockPos::of(4, 64, 8), 300);

        assert!(
            lake.values()
                .any(|block| *block == block!("water", {level: 0}))
        );
        assert!(lake.values().any(|block| *block == block!("air")));
    }

    #[test]
    fn lake_generation_has_a_sand_shore() {
        let lake = generate_lake(BlockPos::of(4, 64, 8), 300);

        assert!(
            lake.iter()
                .any(|(offset, block)| offset.y == 0 && *block == block!("sand"))
        );
    }

    #[test]
    fn lake_water_columns_are_lined_with_sand() {
        let lake = generate_lake(BlockPos::of(4, 64, 8), 300);

        for offset in lake
            .iter()
            .filter_map(|(offset, block)| (*block == block!("water", {level: 0})).then_some(offset))
        {
            assert!(
                (-4..offset.y)
                    .any(|y| lake.get(&I8Vec3::new(offset.x, y, offset.z)) == Some(&block!("sand"))),
                "water column at {:?} should have sand below it",
                offset
            );
        }
    }
}
