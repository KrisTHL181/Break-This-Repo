use std::fmt::{self, Display, Formatter};
#[derive(Clone, Copy, Debug, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct GenStage(u8);

impl GenStage {
    pub const EMPTY: Self = Self(0);
    pub const STRUCTURE_STARTS: Self = Self(1);
    pub const STRUCTURE_REFERENCES: Self = Self(2);
    pub const BIOMES: Self = Self(3);
    pub const NOISE: Self = Self(4);
    pub const SURFACE: Self = Self(5);
    pub const CARVERS: Self = Self(6);
    pub const FEATURES: Self = Self(7);
    pub const FULL: Self = Self(8);
    pub const fn new(stage: u8) -> Self {
        Self(stage)
    }

    pub const fn raw(self) -> u8 {
        self.0
    }

    pub const fn previous(self) -> Option<Self> {
        match self.0.checked_sub(1) {
            Some(stage) => Some(Self(stage)),
            None => None,
        }
    }

    pub const fn next(self) -> Option<Self> {
        match self.0.checked_add(1) {
            Some(stage) => Some(Self(stage)),
            None => None,
        }
    }
}

impl Display for GenStage {
    fn fmt(&self, f: &mut Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.0)
    }
}

impl From<u8> for GenStage {
    fn from(value: u8) -> Self {
        Self(value)
    }
}

impl From<GenStage> for u8 {
    fn from(value: GenStage) -> Self {
        value.0
    }
}

/// The list of things that must exist before a stage can run.
///
/// `own_stage` is the earlier stage required on the target chunk itself. `neighbor_stage` and
/// `neighbor_radius` describe the surrounding chunks needed by cross-chunk generation. For example,
/// features can ask for all chunks in a radius of 1 to have reached the carver stage before trees
/// try to spill across chunk borders.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct StageDependencies {
    pub own_stage: Option<GenStage>,
    pub neighbor_stage: Option<GenStage>,
    pub neighbor_radius: u8,
}

impl StageDependencies {
    pub const NONE: Self = Self {
        own_stage: None,
        neighbor_stage: None,
        neighbor_radius: 0,
    };

    pub const fn new(
        own_stage: Option<GenStage>,
        neighbor_stage: Option<GenStage>,
        neighbor_radius: u8,
    ) -> Self {
        Self {
            own_stage,
            neighbor_stage,
            neighbor_radius,
        }
    }

    /// Depends only on an earlier stage of the same chunk.
    pub const fn only_own(own_stage: GenStage) -> Self {
        Self {
            own_stage: Some(own_stage),
            neighbor_stage: None,
            neighbor_radius: 0,
        }
    }

    /// Depends on the same chunk and on neighboring chunks being generated far enough.
    pub const fn with_neighbors(
        own_stage: GenStage,
        neighbor_stage: GenStage,
        neighbor_radius: u8,
    ) -> Self {
        Self {
            own_stage: Some(own_stage),
            neighbor_stage: Some(neighbor_stage),
            neighbor_radius,
        }
    }
}

/// A generator's description of one stage.
///
/// The scheduler uses this as the recipe for expanding a requested stage into jobs. The `name` is
/// mostly there to make debugging less miserable; the important bit is the dependency shape.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct StageSpec {
    pub stage: GenStage,
    pub name: &'static str,
    pub dependencies: StageDependencies,
}

impl StageSpec {
    pub const fn new(stage: GenStage, name: &'static str, dependencies: StageDependencies) -> Self {
        Self {
            stage,
            name,
            dependencies,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn stage_arithmetic_stays_inside_u8() {
        assert_eq!(GenStage::EMPTY.previous(), None);
        assert_eq!(
            GenStage::STRUCTURE_REFERENCES.previous(),
            Some(GenStage::STRUCTURE_STARTS)
        );
        assert_eq!(GenStage::NOISE.previous(), Some(GenStage::BIOMES));
        assert_eq!(GenStage::FULL.next(), Some(GenStage::new(9)));
        assert_eq!(GenStage::new(u8::MAX).next(), None);
    }
}
