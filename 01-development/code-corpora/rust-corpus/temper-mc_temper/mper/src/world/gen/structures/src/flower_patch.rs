use std::collections::HashMap;

use bevy_math::I8Vec3;
use temper_core::block_state_id::BlockStateId;
use temper_core::pos::BlockPos;
use temper_macros::block;

pub fn generate_flower_patch(root_pos: BlockPos, seed: u64) -> HashMap<I8Vec3, BlockStateId> {
    let mut blocks = HashMap::new();
    let radius = rand_inclusive(root_pos, seed, 0, 2, 4);
    let density = rand_inclusive(root_pos, seed, 1, 36, 54);
    let dominant_flower = rand(root_pos, seed, 2);
    let accent_flower = rand(root_pos, seed, 3);

    for x in -radius..=radius {
        for z in -radius..=radius {
            let offset = I8Vec3::new(x, 0, z);
            let cell_pos = root_pos + offset.as_ivec3();
            let cell_rand = rand(cell_pos, seed, 10);
            let edge_softness = rand_inclusive(cell_pos, seed, 11, -1, 2);
            let max_distance = i16::from(radius + edge_softness).pow(2);
            let distance = i16::from(x).pow(2) + i16::from(z).pow(2);

            if distance > max_distance || cell_rand % 100 >= density as u64 {
                continue;
            }

            let flower = if cell_rand.is_multiple_of(5) {
                flower_block(accent_flower)
            } else {
                flower_block(dominant_flower)
            };

            blocks.insert(offset, flower);
        }
    }

    blocks
        .entry(I8Vec3::ZERO)
        .or_insert_with(|| flower_block(dominant_flower));

    blocks
}

fn flower_block(rand: u64) -> BlockStateId {
    match rand % 11 {
        0 => block!("minecraft:dandelion"),
        1 => block!("minecraft:poppy"),
        2 => block!("minecraft:blue_orchid"),
        3 => block!("minecraft:allium"),
        4 => block!("minecraft:azure_bluet"),
        5 => block!("minecraft:red_tulip"),
        6 => block!("minecraft:orange_tulip"),
        7 => block!("minecraft:white_tulip"),
        8 => block!("minecraft:pink_tulip"),
        9 => block!("minecraft:oxeye_daisy"),
        _ => block!("minecraft:cornflower"),
    }
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
    fn flower_patch_generation_is_deterministic_for_the_same_root() {
        let root_pos = BlockPos::of(4, 65, 8);

        assert_eq!(
            generate_flower_patch(root_pos, 300),
            generate_flower_patch(root_pos, 300)
        );
    }

    #[test]
    fn flower_patch_generation_varies_between_roots() {
        let first = generate_flower_patch(BlockPos::of(4, 65, 8), 300);
        let second = generate_flower_patch(BlockPos::of(12, 65, 11), 300);

        assert_ne!(first, second);
    }

    #[test]
    fn flower_patch_generation_creates_a_batch() {
        let patch = generate_flower_patch(BlockPos::of(4, 65, 8), 300);

        assert!(patch.len() > 1);
    }
}
