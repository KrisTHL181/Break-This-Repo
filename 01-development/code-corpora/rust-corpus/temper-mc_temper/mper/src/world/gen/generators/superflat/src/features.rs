use std::collections::HashMap;

use bevy_math::{I8Vec3, IVec3};
use gen_core::StageInput;
use gen_structures::flower_patch::generate_flower_patch;
use gen_structures::grass_patch::generate_grass_patch;
use gen_structures::lake::generate_lake;
use gen_structures::tree::generate_tree;
use temper_core::block_state_id::BlockStateId;
use temper_core::pos::{BlockPos, ChunkPos};
use temper_macros::block;

const FLOWER_PATCH_CHANCE: u64 = 8;
const FLOWER_PATCH_NEIGHBOR_RADIUS: i32 = 1;
const FLOWER_PATCH_ORIGIN_ATTEMPTS: i32 = 2;
const GRASS_PATCH_CHANCE: u64 = 2;
const GRASS_PATCH_NEIGHBOR_RADIUS: i32 = 1;
const GRASS_PATCH_ORIGIN_ATTEMPTS: i32 = 4;
const LAKE_CHANCE: u64 = 48;
const LAKE_NEIGHBOR_RADIUS: i32 = 1;
const LAKE_ORIGIN_ATTEMPTS: i32 = 1;
const TREE_ORIGIN_ATTEMPTS: i32 = 4;
const TREE_NEIGHBOR_RADIUS: i32 = 1;
const TREE_CHANCE: u64 = 3;

pub(crate) fn generate_features(input: &mut StageInput<'_>, seed: u64) {
    place_lakes(input, seed);
    place_trees(input, seed);
    place_flower_patches(input, seed);
    place_grass_patches(input, seed);
}

fn place_lakes(input: &mut StageInput<'_>, seed: u64) {
    for origin_chunk in nearby_chunks(input.pos, LAKE_NEIGHBOR_RADIUS) {
        for origin in lake_origins_for_chunk(seed, origin_chunk) {
            place_blocks(
                input,
                origin,
                &generate_lake(origin, seed),
                PlacementRule::Replace,
            );
        }
    }
}

fn place_trees(input: &mut StageInput<'_>, seed: u64) {
    for origin_chunk in nearby_chunks(input.pos, TREE_NEIGHBOR_RADIUS) {
        for origin in tree_origins_for_chunk(seed, origin_chunk) {
            if lake_covers(seed, origin + IVec3::NEG_Y) {
                continue;
            }

            place_blocks(
                input,
                origin,
                &generate_tree(origin, seed),
                PlacementRule::Replace,
            );
        }
    }
}

fn place_flower_patches(input: &mut StageInput<'_>, seed: u64) {
    for origin_chunk in nearby_chunks(input.pos, FLOWER_PATCH_NEIGHBOR_RADIUS) {
        for origin in flower_patch_origins_for_chunk(seed, origin_chunk) {
            place_blocks(
                input,
                origin,
                &generate_flower_patch(origin, seed),
                PlacementRule::OnlyAirOnGrass,
            );
        }
    }
}

fn place_grass_patches(input: &mut StageInput<'_>, seed: u64) {
    for origin_chunk in nearby_chunks(input.pos, GRASS_PATCH_NEIGHBOR_RADIUS) {
        for origin in grass_patch_origins_for_chunk(seed, origin_chunk) {
            place_blocks(
                input,
                origin,
                &generate_grass_patch(origin, seed),
                PlacementRule::OnlyAirOnGrass,
            );
        }
    }
}

fn place_blocks(
    input: &mut StageInput<'_>,
    origin: BlockPos,
    blocks: &HashMap<I8Vec3, BlockStateId>,
    rule: PlacementRule,
) {
    for (offset, block) in blocks {
        let block_pos = origin
            + IVec3::new(
                i32::from(offset.x),
                i32::from(offset.y),
                i32::from(offset.z),
            );

        if block_pos.chunk() != input.pos {
            continue;
        }

        let chunk_pos = block_pos.chunk_block_pos();

        match rule {
            PlacementRule::Replace => {}
            PlacementRule::OnlyAirOnGrass => {
                if input.target.get_block(chunk_pos) != block!("air")
                    || input
                        .target
                        .get_block((block_pos + IVec3::NEG_Y).chunk_block_pos())
                        != block!("grass_block", {snowy: false})
                {
                    continue;
                }
            }
        }

        input.target.set_block(chunk_pos, *block);
    }
}

#[derive(Clone, Copy, PartialEq, Eq)]
enum PlacementRule {
    Replace,
    OnlyAirOnGrass,
}

fn nearby_chunks(pos: ChunkPos, radius: i32) -> impl Iterator<Item = ChunkPos> {
    (-radius..=radius).flat_map(move |x| (-radius..=radius).map(move |z| pos + (x, z)))
}

fn tree_origins_for_chunk(seed: u64, chunk: ChunkPos) -> impl Iterator<Item = BlockPos> {
    (0..TREE_ORIGIN_ATTEMPTS).filter_map(move |attempt| {
        let attempt_pos = chunk.block_offset(attempt, 65, attempt);
        let rand = attempt_pos.deterministic_rand(seed);

        if !rand.is_multiple_of(TREE_CHANCE) {
            return None;
        }

        let x = ((rand >> 8) & 0xf) as i32;
        let z = ((rand >> 12) & 0xf) as i32;

        Some(chunk.block_offset(x, 65, z))
    })
}

fn lake_origins_for_chunk(seed: u64, chunk: ChunkPos) -> impl Iterator<Item = BlockPos> {
    (0..LAKE_ORIGIN_ATTEMPTS).filter_map(move |attempt| {
        let attempt_pos = chunk.block_offset(8 + attempt, 64, 8 - attempt);
        let rand = attempt_pos.deterministic_rand(seed ^ 0x3cf5_68e9_8f49_17ba);

        if !rand.is_multiple_of(LAKE_CHANCE) {
            return None;
        }

        let x = ((rand >> 8) & 0xf) as i32;
        let z = ((rand >> 12) & 0xf) as i32;

        Some(chunk.block_offset(x, 64, z))
    })
}

fn lake_covers(seed: u64, pos: BlockPos) -> bool {
    nearby_chunks(pos.chunk(), LAKE_NEIGHBOR_RADIUS).any(|origin_chunk| {
        lake_origins_for_chunk(seed, origin_chunk).any(|origin| {
            generate_lake(origin, seed)
                .keys()
                .any(|offset| origin + offset.as_ivec3() == pos)
        })
    })
}

fn flower_patch_origins_for_chunk(seed: u64, chunk: ChunkPos) -> impl Iterator<Item = BlockPos> {
    (0..FLOWER_PATCH_ORIGIN_ATTEMPTS).filter_map(move |attempt| {
        let attempt_pos = chunk.block_offset(15 - attempt, 65, attempt * 7);
        let rand = attempt_pos.deterministic_rand(seed ^ 0x6c1d_379f_9a12_24b5);

        if !rand.is_multiple_of(FLOWER_PATCH_CHANCE) {
            return None;
        }

        let x = ((rand >> 8) & 0xf) as i32;
        let z = ((rand >> 12) & 0xf) as i32;

        Some(chunk.block_offset(x, 65, z))
    })
}

fn grass_patch_origins_for_chunk(seed: u64, chunk: ChunkPos) -> impl Iterator<Item = BlockPos> {
    (0..GRASS_PATCH_ORIGIN_ATTEMPTS).filter_map(move |attempt| {
        let attempt_pos = chunk.block_offset(attempt * 5, 65, 15 - attempt);
        let rand = attempt_pos.deterministic_rand(seed ^ 0xa4b1_c06d_72ef_1839);

        if !rand.is_multiple_of(GRASS_PATCH_CHANCE) {
            return None;
        }

        let x = ((rand >> 8) & 0xf) as i32;
        let z = ((rand >> 12) & 0xf) as i32;

        Some(chunk.block_offset(x, 65, z))
    })
}
