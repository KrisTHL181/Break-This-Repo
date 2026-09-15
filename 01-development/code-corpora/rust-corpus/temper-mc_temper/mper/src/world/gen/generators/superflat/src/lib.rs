mod features;

use crate::features::generate_features;
use gen_core::{
    ChunkGenerator, GenStage, GenerationError, GeneratorId, StageDependencies, StageInput,
    StageSpec,
};
use temper_core::block_state_id::BlockStateId;
use temper_core::pos::ChunkBlockPos;
use temper_macros::block;

pub struct SuperflatGenerator {
    seed: u64,
}

impl SuperflatGenerator {
    pub const fn new(seed: u64) -> Self {
        Self { seed }
    }
}

impl ChunkGenerator for SuperflatGenerator {
    fn id(&self) -> GeneratorId {
        GeneratorId::new("superflat")
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
                StageDependencies::with_neighbors(GenStage::CARVERS, GenStage::SURFACE, 1),
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
            GenStage::EMPTY => {
                let target = input.target;
                for y in -4..3 {
                    target.fill_section(y, block!("stone"));
                }
                target.fill_section(3, block!("dirt"));
                for x in 0..16 {
                    for z in 0..16 {
                        target.set_block(
                            ChunkBlockPos::new(x, 64, z),
                            block!("grass_block", {snowy: false}),
                        );
                    }
                }
                Ok(())
            }
            GenStage::FEATURES => {
                let mut input = input;
                generate_features(&mut input, self.seed);
                Ok(())
            }
            _ => Ok(()),
        }
    }
}
