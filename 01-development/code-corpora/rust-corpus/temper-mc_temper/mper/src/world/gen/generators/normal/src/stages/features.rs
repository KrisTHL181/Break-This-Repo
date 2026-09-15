use std::collections::HashMap;

use bevy_math::{I8Vec3, IVec3};
use gen_core::{GenerationError, StageInput};
use gen_structures::tree::generate_tree;
use temper_core::block_state_id::BlockStateId;
use temper_core::pos::{BlockPos, ChunkBlockPos, ChunkPos};
use temper_macros::block;
use temper_world_format::Chunk;

use crate::NormalGenerator;

const TREE_CHANCE: u64 = 5;
const TREE_NEIGHBOR_RADIUS: i32 = 1;
const TREE_ORIGIN_ATTEMPTS: i32 = 4;
const MIN_TREE_SURFACE_Y: i16 = -64;

impl NormalGenerator {
    pub(crate) fn generate_features(
        &self,
        mut input: StageInput<'_>,
    ) -> Result<(), GenerationError> {
        place_trees(&mut input, self.seed);
        Ok(())
    }
}

fn place_trees(input: &mut StageInput<'_>, seed: u64) {
    let origins = nearby_chunks(input.pos, TREE_NEIGHBOR_RADIUS)
        .filter_map(|chunk_pos| chunk_for_origin(input, chunk_pos).map(|chunk| (chunk_pos, chunk)))
        .flat_map(|(chunk_pos, chunk)| tree_origins_for_chunk(seed, chunk_pos, chunk))
        .collect::<Vec<_>>();

    for origin in origins {
        place_blocks(input, origin, &generate_tree(origin, seed));
    }
}

fn place_blocks(
    input: &mut StageInput<'_>,
    origin: BlockPos,
    blocks: &HashMap<I8Vec3, BlockStateId>,
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
        let existing = input.target.get_block(chunk_pos);

        if existing != block!("air")
            && existing
                != block!("minecraft:oak_leaves", {distance: 1, persistent: true, waterlogged: false})
        {
            continue;
        }

        input.target.set_block(chunk_pos, *block);
    }
}

fn tree_origins_for_chunk(
    seed: u64,
    chunk_pos: ChunkPos,
    chunk: &Chunk,
) -> impl Iterator<Item = BlockPos> + '_ {
    (0..TREE_ORIGIN_ATTEMPTS).filter_map(move |attempt| {
        let attempt_pos = chunk_pos.block_offset(attempt, 65, attempt);
        let rand = attempt_pos.deterministic_rand(seed);

        if !rand.is_multiple_of(TREE_CHANCE) {
            return None;
        }

        let x = ((rand >> 8) & 0xf) as u8;
        let z = ((rand >> 12) & 0xf) as u8;

        tree_origin_for_column(chunk_pos, chunk, x, z)
    })
}

fn tree_origin_for_column(
    chunk_pos: ChunkPos,
    chunk: &Chunk,
    local_x: u8,
    local_z: u8,
) -> Option<BlockPos> {
    let surface_y = tree_surface_y(chunk, local_x, local_z)?;

    let root_y = surface_y + 1;
    let root_pos = ChunkBlockPos::new(local_x, root_y, local_z);

    if !can_start_tree_in(chunk.get_block(root_pos)) {
        return None;
    }

    Some(chunk_pos.chunk_block(root_pos))
}

fn tree_surface_y(chunk: &Chunk, local_x: u8, local_z: u8) -> Option<i16> {
    let top_y = chunk.heightmaps.world_surface.get_height(local_x, local_z);

    for y in (MIN_TREE_SURFACE_Y..=top_y).rev() {
        let block = chunk.get_block(ChunkBlockPos::new(local_x, y, local_z));

        if block == block!("grass_block", {snowy: false}) {
            return Some(y);
        }

        if block != block!("air") && !is_tree_block(block) {
            return None;
        }
    }

    None
}

fn can_start_tree_in(block: BlockStateId) -> bool {
    block == block!("air") || is_tree_block(block)
}

fn is_tree_block(block: BlockStateId) -> bool {
    block == block!("minecraft:oak_log", {axis: "x"})
        || block == block!("minecraft:oak_log", {axis: "y"})
        || block == block!("minecraft:oak_log", {axis: "z"})
        || block
            == block!("minecraft:oak_leaves", {distance: 1, persistent: true, waterlogged: false})
}

fn chunk_for_origin<'a>(input: &'a StageInput<'_>, pos: ChunkPos) -> Option<&'a Chunk> {
    if pos == input.pos {
        return Some(input.target);
    }

    input.neighborhood.get(pos).map(|neighbor| neighbor.chunk)
}

fn nearby_chunks(pos: ChunkPos, radius: i32) -> impl Iterator<Item = ChunkPos> {
    (-radius..=radius).flat_map(move |x| (-radius..=radius).map(move |z| pos + (x, z)))
}

#[cfg(test)]
mod tests {
    use gen_core::{GenStage, StageInput, StageNeighbor, StageNeighborhood};
    use temper_world_format::Chunk;

    use super::*;

    #[test]
    fn tree_origin_uses_grass_world_surface() {
        let mut chunk = Chunk::new_empty();
        chunk.set_block(
            ChunkBlockPos::new(4, 72, 8),
            block!("grass_block", {snowy: false}),
        );

        let root = tree_origin_for_column(ChunkPos::new(2, 3), &chunk, 4, 8)
            .expect("grass surface should allow a tree root");

        assert_eq!(root, ChunkPos::new(2, 3).block_offset(4, 73, 8));
    }

    #[test]
    fn tree_origin_rejects_water_surface() {
        let mut chunk = Chunk::new_empty();
        chunk.set_block(ChunkBlockPos::new(4, 72, 8), block!("water", {level: 0}));

        assert!(tree_origin_for_column(ChunkPos::new(2, 3), &chunk, 4, 8).is_none());
    }

    #[test]
    fn features_can_place_a_tree_in_the_target_chunk() {
        let mut chunk = Chunk::new_empty();
        let origin = ChunkPos::new(0, 0).block_offset(5, 73, 5);

        chunk.set_block(
            ChunkBlockPos::new(5, 72, 5),
            block!("grass_block", {snowy: false}),
        );

        let mut input = StageInput::new(
            ChunkPos::new(0, 0),
            GenStage::FEATURES,
            &mut chunk,
            StageNeighborhood::empty(),
        );

        place_blocks(&mut input, origin, &generate_tree(origin, 0));

        assert_eq!(
            input.target.get_block(ChunkBlockPos::new(5, 73, 5)),
            block!("minecraft:oak_log", {axis: "y"}),
        );
    }

    #[test]
    fn tree_blocks_can_spill_east_into_the_target_chunk() {
        let mut chunk = Chunk::new_empty();
        let origin = ChunkPos::new(0, 0).block_offset(15, 73, 5);
        let mut blocks = HashMap::new();

        blocks.insert(
            I8Vec3::new(1, 0, 0),
            block!("minecraft:oak_log", {axis: "x"}),
        );

        let mut input = StageInput::new(
            ChunkPos::new(1, 0),
            GenStage::FEATURES,
            &mut chunk,
            StageNeighborhood::empty(),
        );

        place_blocks(&mut input, origin, &blocks);

        assert_eq!(
            input.target.get_block(ChunkBlockPos::new(0, 73, 5)),
            block!("minecraft:oak_log", {axis: "x"}),
        );
    }

    #[test]
    fn tree_blocks_can_spill_west_into_the_target_chunk() {
        let mut chunk = Chunk::new_empty();
        let origin = ChunkPos::new(1, 0).block_offset(0, 73, 5);
        let mut blocks = HashMap::new();

        blocks.insert(
            I8Vec3::new(-1, 0, 0),
            block!("minecraft:oak_log", {axis: "x"}),
        );

        let mut input = StageInput::new(
            ChunkPos::new(0, 0),
            GenStage::FEATURES,
            &mut chunk,
            StageNeighborhood::empty(),
        );

        place_blocks(&mut input, origin, &blocks);

        assert_eq!(
            input.target.get_block(ChunkBlockPos::new(15, 73, 5)),
            block!("minecraft:oak_log", {axis: "x"}),
        );
    }

    #[test]
    fn west_origin_tree_can_generate_leaves_in_east_neighbor() {
        let west = grass_chunk(72);
        let mut east = Chunk::new_empty();
        let west_pos = ChunkPos::new(0, 0);
        let east_pos = ChunkPos::new(1, 0);
        let seed = seed_with_tree_origin_near_east_edge(west_pos, &west);
        let neighbors = [StageNeighbor::new(west_pos, GenStage::CARVERS, &west)];
        let neighborhood = StageNeighborhood::new(&neighbors);
        let input = StageInput::new(east_pos, GenStage::FEATURES, &mut east, neighborhood);

        NormalGenerator::new(seed)
            .generate_features(input)
            .expect("features should generate");

        let has_tree_block = (0..16).any(|x| {
            (0..16).any(|z| {
                (73..84).any(|y| {
                    let block = east.get_block(ChunkBlockPos::new(x, y, z));

                    block == block!("minecraft:oak_log", {axis: "y"})
                        || block == block!("minecraft:oak_log", {axis: "x"})
                        || block == block!("minecraft:oak_log", {axis: "z"})
                        || block
                            == block!("minecraft:oak_leaves", {distance: 1, persistent: true, waterlogged: false})
                })
            })
        });

        assert!(has_tree_block);
    }

    #[test]
    fn west_origin_tree_still_spills_east_after_origin_chunk_has_features() {
        let west_pos = ChunkPos::new(-16, -12);
        let east_pos = ChunkPos::new(-15, -12);
        let mut west = grass_chunk(72);
        let mut east = Chunk::new_empty();
        let seed = seed_with_tree_origin_near_east_edge(west_pos, &west);
        let origin = tree_origins_for_chunk(seed, west_pos, &west)
            .find(|origin| origin.pos.x >= west_pos.pos.x + 14)
            .expect("test seed should place a tree near the east edge");

        {
            let mut input = StageInput::new(
                west_pos,
                GenStage::FEATURES,
                &mut west,
                StageNeighborhood::empty(),
            );

            place_blocks(&mut input, origin, &generate_tree(origin, seed));
        }

        let neighbors = [StageNeighbor::new(west_pos, GenStage::FULL, &west)];
        let neighborhood = StageNeighborhood::new(&neighbors);
        let input = StageInput::new(east_pos, GenStage::FEATURES, &mut east, neighborhood);

        NormalGenerator::new(seed)
            .generate_features(input)
            .expect("features should generate");

        assert!(chunk_has_tree_blocks(&east));
    }

    #[test]
    fn featured_origin_chunk_can_still_spill_trees_in_each_direction() {
        let origin_pos = ChunkPos::new(-16, -12);

        for offset in [(-1, 0), (1, 0), (0, -1), (0, 1)] {
            let target_pos = origin_pos + offset;
            let mut origin_chunk = grass_chunk(72);
            let mut target_chunk = Chunk::new_empty();
            let (seed, origin) =
                seed_with_tree_spilling_into(origin_pos, target_pos, &origin_chunk);

            {
                let mut input = StageInput::new(
                    origin_pos,
                    GenStage::FEATURES,
                    &mut origin_chunk,
                    StageNeighborhood::empty(),
                );

                place_blocks(&mut input, origin, &generate_tree(origin, seed));
            }

            let neighbors = [StageNeighbor::new(
                origin_pos,
                GenStage::FULL,
                &origin_chunk,
            )];
            let neighborhood = StageNeighborhood::new(&neighbors);
            let input = StageInput::new(
                target_pos,
                GenStage::FEATURES,
                &mut target_chunk,
                neighborhood,
            );

            NormalGenerator::new(seed)
                .generate_features(input)
                .expect("features should generate");

            assert!(
                chunk_has_tree_blocks(&target_chunk),
                "tree did not spill from {origin_pos:?} into {target_pos:?}",
            );
        }
    }

    fn grass_chunk(surface_y: i16) -> Chunk {
        let mut chunk = Chunk::new_empty();

        for x in 0..16 {
            for z in 0..16 {
                chunk.set_block(
                    ChunkBlockPos::new(x, surface_y, z),
                    block!("grass_block", {snowy: false}),
                );
            }
        }

        chunk
    }

    fn chunk_has_tree_blocks(chunk: &Chunk) -> bool {
        (0..16).any(|x| {
            (0..16).any(|z| {
                (73..84).any(|y| {
                    let block = chunk.get_block(ChunkBlockPos::new(x, y, z));
                    is_tree_block(block)
                })
            })
        })
    }

    fn seed_with_tree_origin_near_east_edge(chunk_pos: ChunkPos, chunk: &Chunk) -> u64 {
        (0..10_000)
            .find(|seed| {
                tree_origins_for_chunk(*seed, chunk_pos, chunk)
                    .any(|origin| origin.pos.x >= chunk_pos.pos.x + 14)
            })
            .expect("test should find a seed with an east-edge tree")
    }

    fn seed_with_tree_spilling_into(
        origin_pos: ChunkPos,
        target_pos: ChunkPos,
        chunk: &Chunk,
    ) -> (u64, BlockPos) {
        (0..100_000)
            .find_map(|seed| {
                tree_origins_for_chunk(seed, origin_pos, chunk)
                    .find(|origin| {
                        generate_tree(*origin, seed)
                            .keys()
                            .any(|offset| (*origin + offset.as_ivec3()).chunk() == target_pos)
                    })
                    .map(|origin| (seed, origin))
            })
            .expect("test should find a tree that crosses the target chunk border")
    }
}
