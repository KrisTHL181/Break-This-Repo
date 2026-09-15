use gen_core::{ChunkGenerator, GenStage, GenerationError, GeneratorId, StageInput, StageSpec};

pub struct UnconfiguredChunkGenerator {
    seed: u64,
}

impl UnconfiguredChunkGenerator {
    pub fn new(seed: u64) -> Self {
        Self { seed }
    }
}

impl ChunkGenerator for UnconfiguredChunkGenerator {
    fn id(&self) -> GeneratorId {
        GeneratorId::new("unconfigured")
    }

    fn final_stage(&self) -> GenStage {
        GenStage::FULL
    }

    fn stage_spec(&self, _stage: GenStage) -> Option<StageSpec> {
        None
    }

    fn advance_stage(&self, _input: StageInput<'_>) -> Result<(), GenerationError> {
        Err(GenerationError::Failed(format!(
            "no chunk generator has been configured for seed {}",
            self.seed
        )))
    }
}
