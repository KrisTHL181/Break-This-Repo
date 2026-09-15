pub mod stages;
mod terrain;

use gen_core::{
    ChunkGenerator, GenStage, GenerationError, GeneratorId, StageDependencies, StageInput,
    StageSpec,
};

pub struct NormalGenerator {
    seed: u64,
}

impl NormalGenerator {
    pub const fn new(seed: u64) -> Self {
        Self { seed }
    }
}

impl ChunkGenerator for NormalGenerator {
    fn id(&self) -> GeneratorId {
        GeneratorId::new("normal")
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
                StageDependencies::with_neighbors(GenStage::CARVERS, GenStage::CARVERS, 1),
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
            GenStage::EMPTY => generate_empty(input),
            GenStage::STRUCTURE_STARTS => generate_structure_starts(input, self.seed),
            GenStage::STRUCTURE_REFERENCES => generate_structure_references(input, self.seed),
            GenStage::BIOMES => generate_biomes(input, self.seed),
            GenStage::NOISE => self.generate_noises(input),
            GenStage::SURFACE => self.generate_surface(input),
            GenStage::CARVERS => self.generate_carvers(input),
            GenStage::FEATURES => self.generate_features(input),
            GenStage::FULL => finish_chunk(input),
            _ => Ok(()),
        }
    }
}

fn generate_empty(_input: StageInput<'_>) -> Result<(), GenerationError> {
    Ok(())
}

fn generate_structure_starts(_input: StageInput<'_>, _seed: u64) -> Result<(), GenerationError> {
    Ok(())
}

fn generate_structure_references(
    _input: StageInput<'_>,
    _seed: u64,
) -> Result<(), GenerationError> {
    Ok(())
}

fn generate_biomes(_input: StageInput<'_>, _seed: u64) -> Result<(), GenerationError> {
    Ok(())
}

fn finish_chunk(input: StageInput<'_>) -> Result<(), GenerationError> {
    // Clearing so we don't try to compress like 1.6mb of data we don't need on save
    input.target.noise.clear_transient_3d();
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    fn dependencies(stage: GenStage) -> StageDependencies {
        NormalGenerator::new(0)
            .stage_spec(stage)
            .expect("normal generator should define this stage")
            .dependencies
    }

    #[test]
    fn current_stages_have_expected_dependencies() {
        assert_eq!(
            dependencies(GenStage::STRUCTURE_STARTS),
            StageDependencies::only_own(GenStage::EMPTY),
        );
        assert_eq!(
            dependencies(GenStage::STRUCTURE_REFERENCES),
            StageDependencies::only_own(GenStage::STRUCTURE_STARTS),
        );
        assert_eq!(
            dependencies(GenStage::BIOMES),
            StageDependencies::only_own(GenStage::STRUCTURE_REFERENCES),
        );
        assert_eq!(
            dependencies(GenStage::NOISE),
            StageDependencies::only_own(GenStage::BIOMES),
        );
        assert_eq!(
            dependencies(GenStage::SURFACE),
            StageDependencies::only_own(GenStage::NOISE),
        );
        assert_eq!(
            dependencies(GenStage::CARVERS),
            StageDependencies::only_own(GenStage::SURFACE),
        );
        assert_eq!(
            dependencies(GenStage::FEATURES),
            StageDependencies::with_neighbors(GenStage::CARVERS, GenStage::CARVERS, 1),
        );
    }
}
