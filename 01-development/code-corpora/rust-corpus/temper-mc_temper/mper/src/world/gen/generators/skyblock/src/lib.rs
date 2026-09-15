use bevy_math::IVec3;
use gen_core::{
    ChunkGenerator, GenStage, GenerationError, GeneratorId, StageDependencies, StageInput,
    StageSpec,
};
use gen_structures::tree::generate_tree;
use temper_core::block_state_id::BlockStateId;
use temper_core::pos::{BlockPos, ChunkBlockPos, ChunkPos};
use temper_macros::block;

const SKYBLOCK_CHUNK: ChunkPos = ChunkPos::new(0, 0);
const ISLAND_BOTTOM_Y: i16 = 28;
const ISLAND_TOP_Y: i16 = 32;

pub struct SkyblockGenerator {
    seed: u64,
}

impl SkyblockGenerator {
    pub const fn new(seed: u64) -> Self {
        Self { seed }
    }
}

impl ChunkGenerator for SkyblockGenerator {
    fn id(&self) -> GeneratorId {
        GeneratorId::new("skyblock")
    }

    fn final_stage(&self) -> GenStage {
        GenStage::FULL
    }

    fn stage_spec(&self, stage: GenStage) -> Option<StageSpec> {
        match stage {
            GenStage::EMPTY => Some(StageSpec::new(stage, "empty", StageDependencies::NONE)),
            GenStage::STRUCTURE_STARTS => Some(StageSpec::new(
                stage,
                "structure_starts",
                StageDependencies::only_own(GenStage::EMPTY),
            )),
            GenStage::STRUCTURE_REFERENCES => Some(StageSpec::new(
                stage,
                "structure_references",
                StageDependencies::only_own(GenStage::STRUCTURE_STARTS),
            )),
            GenStage::BIOMES => Some(StageSpec::new(
                stage,
                "biomes",
                StageDependencies::only_own(GenStage::STRUCTURE_REFERENCES),
            )),
            GenStage::NOISE => Some(StageSpec::new(
                stage,
                "noise",
                StageDependencies::only_own(GenStage::BIOMES),
            )),
            GenStage::SURFACE => Some(StageSpec::new(
                stage,
                "surface",
                StageDependencies::only_own(GenStage::NOISE),
            )),
            GenStage::CARVERS => Some(StageSpec::new(
                stage,
                "carvers",
                StageDependencies::only_own(GenStage::SURFACE),
            )),
            GenStage::FEATURES => Some(StageSpec::new(
                stage,
                "features",
                StageDependencies::only_own(GenStage::CARVERS),
            )),
            GenStage::FULL => Some(StageSpec::new(
                stage,
                "full",
                StageDependencies::only_own(GenStage::FEATURES),
            )),
            _ => None,
        }
    }

    fn advance_stage(&self, input: StageInput<'_>) -> Result<(), GenerationError> {
        match input.stage {
            GenStage::EMPTY => generate_platform(input),
            GenStage::FEATURES => generate_starting_tree(input, self.seed),
            _ => Ok(()),
        }
    }
}

fn generate_platform(input: StageInput<'_>) -> Result<(), GenerationError> {
    if input.pos != SKYBLOCK_CHUNK {
        return Ok(());
    }

    for x in 5..=10 {
        for z in 5..=10 {
            if !island_column(x, z) {
                continue;
            }

            for y in ISLAND_BOTTOM_Y..ISLAND_TOP_Y {
                input
                    .target
                    .set_block(ChunkBlockPos::new(x, y, z), block!("dirt"));
            }

            input.target.set_block(
                ChunkBlockPos::new(x, ISLAND_TOP_Y, z),
                block!("grass_block", {snowy: false}),
            );
        }
    }

    Ok(())
}

fn generate_starting_tree(input: StageInput<'_>, seed: u64) -> Result<(), GenerationError> {
    if input.pos != SKYBLOCK_CHUNK {
        return Ok(());
    }

    let tree_origin = tree_origin();

    for (offset, block) in generate_tree(tree_origin, seed) {
        let block_pos = tree_origin
            + IVec3::new(
                i32::from(offset.x),
                i32::from(offset.y),
                i32::from(offset.z),
            );

        input.target.set_block(block_pos.chunk_block_pos(), block);
    }

    Ok(())
}

fn tree_origin() -> BlockPos {
    BlockPos::of(5, 33, 10)
}

fn island_column(x: u8, z: u8) -> bool {
    x <= 7 || z <= 7
}

#[cfg(test)]
mod tests {
    use gen_core::ChunkGenerator;
    use temper_world_format::Chunk;

    use super::*;

    #[test]
    fn generates_starting_platform_and_tree_in_origin_chunk() {
        let generator = SkyblockGenerator::new(300);
        let mut chunk = Chunk::new_empty();

        generator
            .advance_stage(StageInput::new(
                SKYBLOCK_CHUNK,
                GenStage::EMPTY,
                &mut chunk,
                gen_core::StageNeighborhood::empty(),
            ))
            .unwrap();
        generator
            .advance_stage(StageInput::new(
                SKYBLOCK_CHUNK,
                GenStage::FEATURES,
                &mut chunk,
                gen_core::StageNeighborhood::empty(),
            ))
            .unwrap();

        assert_eq!(
            chunk.get_block(ChunkBlockPos::new(7, ISLAND_TOP_Y, 7)),
            block!("grass_block", {snowy: false})
        );
        assert_eq!(
            chunk.get_block(ChunkBlockPos::new(5, ISLAND_TOP_Y + 1, 10)),
            block!("minecraft:oak_log", {axis: "y"})
        );
    }

    #[test]
    fn generates_l_shaped_six_by_six_by_four_island() {
        let generator = SkyblockGenerator::new(300);
        let mut chunk = Chunk::new_empty();

        generator
            .advance_stage(StageInput::new(
                SKYBLOCK_CHUNK,
                GenStage::EMPTY,
                &mut chunk,
                gen_core::StageNeighborhood::empty(),
            ))
            .unwrap();

        let mut grass_columns = 0;

        for x in 5..=10 {
            for z in 5..=10 {
                if island_column(x, z) {
                    grass_columns += 1;

                    assert_eq!(
                        chunk.get_block(ChunkBlockPos::new(x, ISLAND_TOP_Y, z)),
                        block!("grass_block", {snowy: false})
                    );

                    for y in ISLAND_BOTTOM_Y..ISLAND_TOP_Y {
                        assert_eq!(chunk.get_block(ChunkBlockPos::new(x, y, z)), block!("dirt"));
                    }
                } else {
                    assert_eq!(
                        chunk.get_block(ChunkBlockPos::new(x, ISLAND_TOP_Y, z)),
                        block!("air")
                    );
                }
            }
        }

        assert_eq!(grass_columns, 27);
    }

    #[test]
    fn leaves_non_origin_chunks_empty() {
        let generator = SkyblockGenerator::new(300);
        let mut chunk = Chunk::new_empty();

        generator
            .advance_stage(StageInput::new(
                ChunkPos::new(1, 0),
                GenStage::EMPTY,
                &mut chunk,
                gen_core::StageNeighborhood::empty(),
            ))
            .unwrap();

        assert_eq!(
            chunk.get_block(ChunkBlockPos::new(8, ISLAND_TOP_Y, 8)),
            block!("air")
        );
    }
}
