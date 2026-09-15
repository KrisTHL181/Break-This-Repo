//! Valid-by-construction Index V2 workload transitions.
//!
//! Callers generate lifecycle actions by consuming state-specific wrappers.
//! For example, only [`ActiveIndex`] exposes [`ActiveIndex::drop_index`], and
//! only [`BlockedIndex`] exposes retry and abort operations.
//!
//! ```
//! use std::num::NonZeroU32;
//! use helix_db_testkit::{
//!     action::{ElementKind, VectorMetric},
//!     ids::{IndexName, PropertyName},
//!     lifecycle::{AbsentIndex, IndexActionKind, IndexDefinition},
//! };
//!
//! let definition = IndexDefinition::Vector {
//!     name: IndexName::try_new("embedding").unwrap(),
//!     element: ElementKind::Node,
//!     property: PropertyName::try_new("embedding").unwrap(),
//!     dimension: NonZeroU32::new(3).unwrap(),
//!     metric: VectorMetric::Cosine,
//! };
//! let created = AbsentIndex::new(definition).create().unwrap();
//! assert_eq!(created.action().kind(), IndexActionKind::Create);
//! let building = created.into_next();
//! let activated = building.activate();
//! assert_eq!(activated.action().kind(), IndexActionKind::Activate);
//! ```

use std::num::NonZeroU32;

use serde::{Deserialize, Serialize};

use crate::action::{ElementKind, VectorMetric};
use crate::ids::{GenerationId, IndexName, PropertyName};
use crate::Result;

/// Physical index family.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum IndexFamily {
    /// Equality secondary index.
    Secondary,
    /// Vector nearest-neighbor index.
    Vector,
    /// Full-text index.
    Text,
}

/// Logical index definition used by independent oracles.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(tag = "family", rename_all = "snake_case")]
pub enum IndexDefinition {
    /// Equality secondary index.
    Secondary {
        /// Logical name.
        name: IndexName,
        /// Indexed element family.
        element: ElementKind,
        /// Indexed property.
        property: PropertyName,
        /// Whether duplicate values are rejected.
        unique: bool,
    },
    /// Vector index.
    Vector {
        /// Logical name.
        name: IndexName,
        /// Indexed element family.
        element: ElementKind,
        /// Indexed vector property.
        property: PropertyName,
        /// Required vector dimension.
        dimension: NonZeroU32,
        /// Stable distance semantics.
        metric: VectorMetric,
    },
    /// Text index.
    Text {
        /// Logical name.
        name: IndexName,
        /// Indexed element family.
        element: ElementKind,
        /// Indexed text property.
        property: PropertyName,
    },
}

impl IndexDefinition {
    /// Borrows the logical name.
    pub fn name(&self) -> &IndexName {
        match self {
            Self::Secondary { name, .. } | Self::Vector { name, .. } | Self::Text { name, .. } => {
                name
            }
        }
    }

    /// Returns the physical family.
    pub const fn family(&self) -> IndexFamily {
        match self {
            Self::Secondary { .. } => IndexFamily::Secondary,
            Self::Vector { .. } => IndexFamily::Vector,
            Self::Text { .. } => IndexFamily::Text,
        }
    }

    /// Returns the indexed element family.
    pub const fn element(&self) -> ElementKind {
        match self {
            Self::Secondary { element, .. }
            | Self::Vector { element, .. }
            | Self::Text { element, .. } => *element,
        }
    }

    /// Borrows the indexed property.
    pub fn property(&self) -> &PropertyName {
        match self {
            Self::Secondary { property, .. }
            | Self::Vector { property, .. }
            | Self::Text { property, .. } => property,
        }
    }
}

/// Exact logical definition and physical generation pair.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct IndexGeneration {
    definition: IndexDefinition,
    generation: GenerationId,
}

impl IndexGeneration {
    fn new(definition: IndexDefinition, generation: GenerationId) -> Self {
        Self {
            definition,
            generation,
        }
    }

    /// Borrows the logical definition.
    pub fn definition(&self) -> &IndexDefinition {
        &self.definition
    }

    /// Returns the physical generation identity.
    pub const fn generation(&self) -> GenerationId {
        self.generation
    }
}

/// Stable lifecycle command kind.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum IndexActionKind {
    /// Create the first hidden generation.
    Create,
    /// Execute bounded build work.
    Build,
    /// Atomically publish a completed generation.
    Activate,
    /// Drop an active generation.
    Drop,
    /// Recreate after a retired generation.
    Recreate,
    /// Retry blocked build work.
    Retry,
    /// Abort and clean a partial build.
    Abort,
}

/// Stable reason a build requires explicit retry or abort.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum IndexBlocker {
    /// Source data violates the index definition.
    InvalidSourceData,
    /// Unique values conflict.
    UniquenessViolation,
    /// A typed resource limit is too low.
    ResourceLimit,
    /// Durable state is corrupt.
    DurableCorruption,
}

/// One generated lifecycle action.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct IndexAction {
    kind: IndexActionKind,
    generation: IndexGeneration,
}

impl IndexAction {
    fn new(kind: IndexActionKind, generation: IndexGeneration) -> Self {
        Self { kind, generation }
    }

    /// Returns the command kind.
    pub const fn kind(&self) -> IndexActionKind {
        self.kind
    }

    /// Borrows the exact generation.
    pub fn generation(&self) -> &IndexGeneration {
        &self.generation
    }
}

/// A generated action paired with its only legal successor state.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Transition<S> {
    action: IndexAction,
    next: S,
}

impl<S> Transition<S> {
    fn new(action: IndexAction, next: S) -> Self {
        Self { action, next }
    }

    /// Borrows the generated action.
    pub fn action(&self) -> &IndexAction {
        &self.action
    }

    /// Consumes the transition and returns the action and successor state.
    pub fn into_parts(self) -> (IndexAction, S) {
        (self.action, self.next)
    }

    /// Consumes the transition and returns the successor state.
    pub fn into_next(self) -> S {
        self.next
    }
}

/// State wrapper for a logical index with no live generation.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct AbsentIndex {
    definition: IndexDefinition,
    last_generation: Option<GenerationId>,
}

impl AbsentIndex {
    /// Constructs a never-created logical index.
    pub fn new(definition: IndexDefinition) -> Self {
        Self {
            definition,
            last_generation: None,
        }
    }

    /// Creates the only legal next hidden generation.
    pub fn create(self) -> Result<Transition<BuildingIndex>> {
        let generation = match self.last_generation {
            Some(last_generation) => last_generation.checked_next()?,
            None => GenerationId::new(1)?,
        };
        let generation = IndexGeneration::new(self.definition, generation);
        Ok(Transition::new(
            IndexAction::new(IndexActionKind::Create, generation.clone()),
            BuildingIndex { generation },
        ))
    }
}

/// State wrapper for a hidden generation under construction.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct BuildingIndex {
    generation: IndexGeneration,
}

impl BuildingIndex {
    /// Emits one bounded build action without changing lifecycle state.
    pub fn build(&self) -> IndexAction {
        IndexAction::new(IndexActionKind::Build, self.generation.clone())
    }

    /// Atomically activates the hidden generation.
    pub fn activate(self) -> Transition<ActiveIndex> {
        Transition::new(
            IndexAction::new(IndexActionKind::Activate, self.generation.clone()),
            ActiveIndex {
                generation: self.generation,
            },
        )
    }

    /// Records an externally observed blocker while retaining the generation.
    pub fn blocked(self, blocker: IndexBlocker) -> BlockedIndex {
        BlockedIndex {
            generation: self.generation,
            blocker,
        }
    }

    /// Aborts and retires partial build state.
    pub fn abort(self) -> Transition<RetiredIndex> {
        Transition::new(
            IndexAction::new(IndexActionKind::Abort, self.generation.clone()),
            RetiredIndex {
                generation: self.generation,
            },
        )
    }

    /// Borrows the hidden generation.
    pub fn generation(&self) -> &IndexGeneration {
        &self.generation
    }
}

/// State wrapper for a publicly visible generation.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ActiveIndex {
    generation: IndexGeneration,
}

impl ActiveIndex {
    /// Drops and retires the active generation.
    pub fn drop_index(self) -> Transition<RetiredIndex> {
        Transition::new(
            IndexAction::new(IndexActionKind::Drop, self.generation.clone()),
            RetiredIndex {
                generation: self.generation,
            },
        )
    }

    /// Borrows the active generation.
    pub fn generation(&self) -> &IndexGeneration {
        &self.generation
    }
}

/// State wrapper for a blocked hidden generation.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct BlockedIndex {
    generation: IndexGeneration,
    blocker: IndexBlocker,
}

impl BlockedIndex {
    /// Retries the same hidden generation.
    pub fn retry(self) -> Transition<BuildingIndex> {
        Transition::new(
            IndexAction::new(IndexActionKind::Retry, self.generation.clone()),
            BuildingIndex {
                generation: self.generation,
            },
        )
    }

    /// Aborts and retires the blocked generation.
    pub fn abort(self) -> Transition<RetiredIndex> {
        Transition::new(
            IndexAction::new(IndexActionKind::Abort, self.generation.clone()),
            RetiredIndex {
                generation: self.generation,
            },
        )
    }

    /// Returns the blocker.
    pub const fn blocker(&self) -> IndexBlocker {
        self.blocker
    }

    /// Borrows the hidden generation retained across retry or abort.
    pub fn generation(&self) -> &IndexGeneration {
        &self.generation
    }
}

/// State wrapper for a dropped or aborted generation.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct RetiredIndex {
    generation: IndexGeneration,
}

impl RetiredIndex {
    /// Recreates the logical index with the next generation identity.
    pub fn recreate(self) -> Result<Transition<BuildingIndex>> {
        let next = self.generation.generation.checked_next()?;
        let generation = IndexGeneration::new(self.generation.definition, next);
        Ok(Transition::new(
            IndexAction::new(IndexActionKind::Recreate, generation.clone()),
            BuildingIndex { generation },
        ))
    }

    /// Converts retirement into an absent state that retains generation history.
    pub fn into_absent(self) -> AbsentIndex {
        AbsentIndex {
            definition: self.generation.definition,
            last_generation: Some(self.generation.generation),
        }
    }

    /// Borrows the retired generation.
    pub fn generation(&self) -> &IndexGeneration {
        &self.generation
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn definition() -> IndexDefinition {
        IndexDefinition::Secondary {
            name: IndexName::try_new("email").unwrap(),
            element: ElementKind::Node,
            property: PropertyName::try_new("email").unwrap(),
            unique: true,
        }
    }

    #[test]
    fn legal_state_wrappers_generate_the_complete_lifecycle() {
        let created = AbsentIndex::new(definition()).create().unwrap();
        assert_eq!(created.action().kind(), IndexActionKind::Create);
        let building = created.into_next();
        assert_eq!(building.build().kind(), IndexActionKind::Build);
        let active = building.activate().into_next();
        let retired = active.drop_index().into_next();
        let recreated = retired.recreate().unwrap();
        assert_eq!(recreated.action().kind(), IndexActionKind::Recreate);
        assert_eq!(recreated.action().generation().generation().get(), 2);
    }

    #[test]
    fn blocked_state_owns_retry_and_abort() {
        let building = AbsentIndex::new(definition()).create().unwrap().into_next();
        let blocked = building.blocked(IndexBlocker::ResourceLimit);
        assert_eq!(blocked.blocker(), IndexBlocker::ResourceLimit);
        let retried = blocked.retry();
        assert_eq!(retried.action().kind(), IndexActionKind::Retry);
        let aborted = retried.into_next().abort();
        assert_eq!(aborted.action().kind(), IndexActionKind::Abort);
        let absent = aborted.into_next().into_absent();
        assert_eq!(
            absent
                .create()
                .unwrap()
                .action()
                .generation()
                .generation()
                .get(),
            2
        );
    }
}
