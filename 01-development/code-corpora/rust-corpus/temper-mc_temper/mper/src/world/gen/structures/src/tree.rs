use std::collections::HashMap;

use bevy_math::I8Vec3;
use temper_core::block_state_id::BlockStateId;
use temper_core::pos::BlockPos;
use temper_macros::block;

pub fn generate_tree(root_pos: BlockPos, seed: u64) -> HashMap<I8Vec3, BlockStateId> {
    let mut blocks = HashMap::new();
    let trunk_height = rand_inclusive(root_pos, seed, 0, 4, 7);
    let canopy_radius = rand_inclusive(root_pos, seed, 1, 2, 3);
    let canopy_height = rand_inclusive(root_pos, seed, 2, 3, 4);
    let canopy_bottom = trunk_height - 2;
    let lean = trunk_lean(root_pos, seed);

    for y in 0..trunk_height {
        let (x, z) = trunk_offset(y, trunk_height, lean);
        blocks.insert(
            I8Vec3::new(x, y, z),
            block!("minecraft:oak_log", {axis: "y"}),
        );
    }

    let (top_x, top_z) = trunk_offset(trunk_height - 1, trunk_height, lean);
    add_leaf_blob(
        &mut blocks,
        root_pos,
        seed,
        I8Vec3::new(top_x, canopy_bottom, top_z),
        canopy_radius,
        canopy_height,
    );
    add_branches(&mut blocks, root_pos, seed, trunk_height, lean);

    blocks
}

fn add_branches(
    blocks: &mut HashMap<I8Vec3, BlockStateId>,
    root_pos: BlockPos,
    seed: u64,
    trunk_height: i8,
    lean: (i8, i8),
) {
    const DIRECTIONS: [(i8, i8); 4] = [(1, 0), (-1, 0), (0, 1), (0, -1)];

    let branch_count = rand_inclusive(root_pos, seed, 3, 2, 4);
    let direction_offset = rand_inclusive(root_pos, seed, 4, 0, 3);

    for branch in 0..branch_count {
        let direction = DIRECTIONS[((branch + direction_offset) % DIRECTIONS.len() as i8) as usize];
        let branch_y = trunk_height - 2 - rand_inclusive(root_pos, seed, 10 + branch, 0, 1);
        let branch_length = rand_inclusive(root_pos, seed, 20 + branch, 1, 2);
        let (base_x, base_z) = trunk_offset(branch_y, trunk_height, lean);
        let mut tip = I8Vec3::new(base_x, branch_y, base_z);

        for step in 1..=branch_length {
            tip = I8Vec3::new(
                base_x + direction.0 * step,
                branch_y + step / 2,
                base_z + direction.1 * step,
            );
            blocks.insert(tip, branch_log(direction));
        }

        add_leaf_blob(blocks, root_pos, seed ^ branch as u64, tip, 1, 2);
    }
}

fn add_leaf_blob(
    blocks: &mut HashMap<I8Vec3, BlockStateId>,
    root_pos: BlockPos,
    seed: u64,
    center: I8Vec3,
    radius: i8,
    height: i8,
) {
    for y in 0..height {
        let layer_radius = if y == height - 1 { radius - 1 } else { radius };

        for x in -layer_radius..=layer_radius {
            for z in -layer_radius..=layer_radius {
                let distance = x.abs() + z.abs();
                let corner_cutoff =
                    rand_inclusive(root_pos, seed, 100 + i8::wrapping_add(x, z) + y, 0, 2);

                if distance > layer_radius + corner_cutoff {
                    continue;
                }

                let pos = center + I8Vec3::new(x, y, z);
                blocks.entry(pos).or_insert_with(leaf_block);
            }
        }
    }
}

fn trunk_lean(root_pos: BlockPos, seed: u64) -> (i8, i8) {
    match root_pos.deterministic_rand(seed ^ 0x7d1f_2a35_90bc_e741) % 7 {
        0 => (1, 0),
        1 => (-1, 0),
        2 => (0, 1),
        3 => (0, -1),
        _ => (0, 0),
    }
}

fn trunk_offset(y: i8, trunk_height: i8, lean: (i8, i8)) -> (i8, i8) {
    if y > trunk_height / 2 { lean } else { (0, 0) }
}

fn branch_log(direction: (i8, i8)) -> BlockStateId {
    if direction.0 == 0 {
        block!("minecraft:oak_log", {axis: "z"})
    } else {
        block!("minecraft:oak_log", {axis: "x"})
    }
}

fn leaf_block() -> BlockStateId {
    block!("minecraft:oak_leaves", {distance: 1, persistent: true, waterlogged: false})
}

fn rand_inclusive(root_pos: BlockPos, seed: u64, salt: i8, min: i8, max: i8) -> i8 {
    let range = u64::from((max - min + 1) as u8);
    let value = root_pos.deterministic_rand(seed ^ salt_hash(salt)) % range;

    min + value as i8
}

fn salt_hash(salt: i8) -> u64 {
    0x9e37_79b9_7f4a_7c15u64.wrapping_mul(salt as u8 as u64 + 1)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn tree_generation_is_deterministic_for_the_same_root() {
        let root_pos = BlockPos::of(4, 65, 8);

        assert_eq!(generate_tree(root_pos, 300), generate_tree(root_pos, 300));
    }

    #[test]
    fn tree_generation_varies_between_roots() {
        let first = generate_tree(BlockPos::of(4, 65, 8), 300);
        let second = generate_tree(BlockPos::of(12, 65, 11), 300);

        assert_ne!(first, second);
    }
}
