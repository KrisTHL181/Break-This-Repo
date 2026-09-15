//! Typed workload actions shared by every execution adapter.

use std::collections::BTreeMap;
use std::num::{NonZeroU16, NonZeroU32, NonZeroU64};

use serde::{Deserialize, Serialize};

use crate::ids::{
    EntityId, FiniteF32, IndexName, LabelName, PropertyName, RequestId, RuntimeId, Sequence,
};
use crate::lifecycle::{IndexAction, IndexGeneration};
use crate::{Result, TestkitError};

/// Graph element family.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum ElementKind {
    /// Node element.
    Node,
    /// Edge element.
    Edge,
}

/// Stable typed reference to one graph element.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash, Serialize, Deserialize)]
#[serde(tag = "kind", content = "id", rename_all = "snake_case")]
pub enum EntityRef {
    /// Node identity.
    Node(EntityId),
    /// Edge identity.
    Edge(EntityId),
}

impl EntityRef {
    /// Returns the element kind.
    pub const fn kind(self) -> ElementKind {
        match self {
            Self::Node(_) => ElementKind::Node,
            Self::Edge(_) => ElementKind::Edge,
        }
    }

    /// Returns the graph identity.
    pub const fn id(self) -> EntityId {
        match self {
            Self::Node(id) | Self::Edge(id) => id,
        }
    }
}

/// Closed inclusive graph-ID range.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(try_from = "(EntityId, EntityId)", into = "(EntityId, EntityId)")]
pub struct EntityRange {
    start: EntityId,
    end: EntityId,
}

impl EntityRange {
    /// Constructs a range whose start does not exceed its end.
    pub fn try_new(start: EntityId, end: EntityId) -> Result<Self> {
        if start > end {
            return Err(TestkitError::InvalidRange {
                start: start.get(),
                end: end.get(),
            });
        }
        Ok(Self { start, end })
    }

    /// Returns the inclusive start.
    pub const fn start(self) -> EntityId {
        self.start
    }

    /// Returns the inclusive end.
    pub const fn end(self) -> EntityId {
        self.end
    }
}

impl TryFrom<(EntityId, EntityId)> for EntityRange {
    type Error = TestkitError;

    fn try_from((start, end): (EntityId, EntityId)) -> Result<Self> {
        Self::try_new(start, end)
    }
}

impl From<EntityRange> for (EntityId, EntityId) {
    fn from(value: EntityRange) -> Self {
        (value.start, value.end)
    }
}

/// Exact finite vector with at least one component.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(try_from = "Vec<FiniteF32>", into = "Vec<FiniteF32>")]
pub struct VectorValue(Vec<FiniteF32>);

impl VectorValue {
    /// Constructs a non-empty vector.
    pub fn try_new(values: Vec<FiniteF32>) -> Result<Self> {
        if values.is_empty() {
            return Err(TestkitError::EmptyCollection {
                kind: "vector components",
            });
        }
        Ok(Self(values))
    }

    /// Borrows vector components.
    pub fn as_slice(&self) -> &[FiniteF32] {
        &self.0
    }

    /// Returns the vector dimension.
    pub fn dimension(&self) -> usize {
        self.0.len()
    }
}

impl TryFrom<Vec<FiniteF32>> for VectorValue {
    type Error = TestkitError;

    fn try_from(values: Vec<FiniteF32>) -> Result<Self> {
        Self::try_new(values)
    }
}

impl From<VectorValue> for Vec<FiniteF32> {
    fn from(value: VectorValue) -> Self {
        value.0
    }
}

/// Raw IEEE-754 binary32 value kept independent from the DB implementation.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(transparent)]
pub struct OracleF32(u32);

impl OracleF32 {
    /// Captures one float without normalizing its representation.
    pub const fn new(value: f32) -> Self {
        Self(value.to_bits())
    }

    /// Returns the original IEEE-754 value.
    pub const fn get(self) -> f32 {
        f32::from_bits(self.0)
    }
}

/// Raw IEEE-754 binary64 value kept independent from the DB implementation.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(transparent)]
pub struct OracleF64(u64);

impl OracleF64 {
    /// Captures one float without normalizing its representation.
    pub const fn new(value: f64) -> Self {
        Self(value.to_bits())
    }

    /// Returns the original IEEE-754 value.
    pub const fn get(self) -> f64 {
        f64::from_bits(self.0)
    }
}

/// Model property value kept independent from the DB encoding implementation.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(tag = "kind", content = "value", rename_all = "snake_case")]
pub enum PropertyValue {
    /// Explicit null.
    Null,
    /// Boolean scalar.
    Bool(bool),
    /// Signed integer scalar.
    I64(i64),
    /// UTC datetime in epoch milliseconds.
    DateTime(i64),
    /// IEEE-754 binary64 scalar.
    F64(OracleF64),
    /// IEEE-754 binary32 scalar.
    F32(OracleF32),
    /// UTF-8 string scalar.
    String(String),
    /// Raw byte string.
    Bytes(Vec<u8>),
    /// Homogeneous signed-integer array.
    I64Array(Vec<i64>),
    /// Homogeneous binary64 array.
    F64Array(Vec<OracleF64>),
    /// Homogeneous binary32 array.
    F32Array(Vec<OracleF32>),
    /// Homogeneous UTF-8 array.
    StringArray(Vec<String>),
    /// Finite non-empty vector.
    Vector(VectorValue),
}

/// Typed scalar accepted by the independent secondary range oracle.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(tag = "kind", content = "value", rename_all = "snake_case")]
pub enum SecondaryRangeValue {
    /// Signed integer scalar.
    I64(i64),
    /// IEEE-754 binary64 scalar.
    F64(OracleF64),
    /// IEEE-754 binary32 scalar.
    F32(OracleF32),
    /// UTC datetime in epoch milliseconds.
    DateTime(i64),
    /// UTF-8 string scalar.
    String(String),
}

/// Inclusive or exclusive typed secondary range bound.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(tag = "kind", content = "value", rename_all = "snake_case")]
pub enum SecondaryRangeBound {
    /// Includes an exactly equal value.
    Inclusive(SecondaryRangeValue),
    /// Excludes an exactly equal value.
    Exclusive(SecondaryRangeValue),
}

impl SecondaryRangeBound {
    /// Borrows the typed bound value.
    pub const fn value(&self) -> &SecondaryRangeValue {
        match self {
            Self::Inclusive(value) | Self::Exclusive(value) => value,
        }
    }

    /// Reports whether exact equality satisfies this bound.
    pub const fn is_inclusive(&self) -> bool {
        matches!(self, Self::Inclusive(_))
    }
}

/// Requested physical ordering for a secondary range read.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum SecondaryRangeDirection {
    /// Smallest semantic value first.
    Ascending,
    /// Largest semantic value first.
    Descending,
}

/// Independently validated bounded secondary range.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct SecondaryRange {
    lower: Option<SecondaryRangeBound>,
    upper: Option<SecondaryRangeBound>,
    direction: SecondaryRangeDirection,
    limit: Option<NonZeroU32>,
}

impl SecondaryRange {
    /// Constructs a range, rejecting mixed value types and reversed bounds.
    pub fn try_new(
        lower: Option<SecondaryRangeBound>,
        upper: Option<SecondaryRangeBound>,
        direction: SecondaryRangeDirection,
        limit: Option<NonZeroU32>,
    ) -> Result<Self> {
        if let (Some(lower), Some(upper)) = (&lower, &upper) {
            let Some(ordering) =
                crate::model::secondary_range_compare(lower.value(), upper.value())
            else {
                return Err(TestkitError::ModelViolation(
                    "secondary range bounds must be comparable non-NaN values".to_string(),
                ));
            };
            if ordering.is_gt() {
                return Err(TestkitError::ModelViolation(
                    "secondary range bounds must have ascending semantic values".to_string(),
                ));
            }
        }
        Ok(Self {
            lower,
            upper,
            direction,
            limit,
        })
    }

    /// Borrows the optional lower bound.
    pub const fn lower(&self) -> Option<&SecondaryRangeBound> {
        self.lower.as_ref()
    }

    /// Borrows the optional upper bound.
    pub const fn upper(&self) -> Option<&SecondaryRangeBound> {
        self.upper.as_ref()
    }

    /// Returns the requested semantic ordering.
    pub const fn direction(&self) -> SecondaryRangeDirection {
        self.direction
    }

    /// Returns the optional positive result limit.
    pub const fn limit(&self) -> Option<NonZeroU32> {
        self.limit
    }
}

/// Non-empty property update set.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(
    try_from = "BTreeMap<PropertyName, PropertyMutation>",
    into = "BTreeMap<PropertyName, PropertyMutation>"
)]
pub struct PropertyPatch(BTreeMap<PropertyName, PropertyMutation>);

impl PropertyPatch {
    /// Constructs a patch containing at least one mutation.
    pub fn try_new(values: BTreeMap<PropertyName, PropertyMutation>) -> Result<Self> {
        if values.is_empty() {
            return Err(TestkitError::EmptyCollection {
                kind: "property patch",
            });
        }
        Ok(Self(values))
    }

    /// Iterates over ordered property mutations.
    pub fn iter(&self) -> impl Iterator<Item = (&PropertyName, &PropertyMutation)> {
        self.0.iter()
    }
}

impl TryFrom<BTreeMap<PropertyName, PropertyMutation>> for PropertyPatch {
    type Error = TestkitError;

    fn try_from(values: BTreeMap<PropertyName, PropertyMutation>) -> Result<Self> {
        Self::try_new(values)
    }
}

impl From<PropertyPatch> for BTreeMap<PropertyName, PropertyMutation> {
    fn from(value: PropertyPatch) -> Self {
        value.0
    }
}

/// One property mutation.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(tag = "kind", content = "value", rename_all = "snake_case")]
pub enum PropertyMutation {
    /// Set or replace a value.
    Set(PropertyValue),
    /// Remove the property.
    Remove,
}

/// Non-empty stable query text.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(try_from = "String", into = "String")]
pub struct TextQuery(String);

impl TextQuery {
    /// Constructs a query containing at least one non-whitespace character.
    pub fn try_new(value: impl Into<String>) -> Result<Self> {
        let value = value.into();
        if value.trim().is_empty() {
            return Err(TestkitError::EmptyIdentifier { kind: "text query" });
        }
        Ok(Self(value))
    }

    /// Borrows the original query.
    pub fn as_str(&self) -> &str {
        &self.0
    }
}

impl TryFrom<String> for TextQuery {
    type Error = TestkitError;

    fn try_from(value: String) -> Result<Self> {
        Self::try_new(value)
    }
}

impl From<TextQuery> for String {
    fn from(value: TextQuery) -> Self {
        value.0
    }
}

/// Vector distance semantics used by the independent brute-force oracle.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum VectorMetric {
    /// Euclidean L2 distance.
    Euclidean,
    /// Cosine distance `1 - cosine_similarity`.
    Cosine,
    /// Negative dot product, where smaller remains better.
    Dot,
}

/// Traversal direction.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum TraversalDirection {
    /// Follow outgoing edges.
    Outgoing,
    /// Follow incoming edges.
    Incoming,
    /// Follow either direction.
    Both,
}

/// Aggregate operation.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum AggregateKind {
    /// Count matching elements.
    Count,
    /// Report whether any matching element exists.
    Exists,
}

/// Read operations understood by the shared oracle and every adapter.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(tag = "action", rename_all = "snake_case")]
pub enum ReadAction {
    /// Point lookup.
    Point {
        /// Element family.
        kind: ElementKind,
        /// Element identity.
        id: EntityId,
    },
    /// Inclusive identity range scan.
    Range {
        /// Element family.
        kind: ElementKind,
        /// Validated range.
        range: EntityRange,
    },
    /// Bounded graph traversal from one node.
    Traversal {
        /// Starting node.
        start: EntityId,
        /// Edge direction.
        direction: TraversalDirection,
        /// Positive maximum depth.
        max_depth: NonZeroU16,
    },
    /// Property projection over a non-empty explicit selection.
    Projection {
        /// Elements to project.
        targets: EntitySelection,
        /// Properties to project.
        fields: ProjectionFields,
    },
    /// Aggregate all elements of one family.
    Aggregate {
        /// Element family.
        kind: ElementKind,
        /// Aggregate operation.
        aggregate: AggregateKind,
    },
    /// Secondary-index equality lookup.
    Secondary {
        /// Logical index name.
        index: IndexName,
        /// Exact property value.
        value: PropertyValue,
    },
    /// Typed, bounded secondary-index range lookup.
    SecondaryRange {
        /// Logical index name.
        index: IndexName,
        /// Independently validated bounds, direction, and limit.
        range: SecondaryRange,
    },
    /// Text search.
    Text {
        /// Logical index name.
        index: IndexName,
        /// Non-empty query.
        query: TextQuery,
        /// Positive result limit.
        limit: NonZeroU32,
    },
    /// Brute-force comparable vector search.
    Vector {
        /// Logical index name.
        index: IndexName,
        /// Exact query vector.
        vector: VectorValue,
        /// Positive result limit.
        limit: NonZeroU32,
        /// Distance metric.
        metric: VectorMetric,
    },
    /// Catalog lookup by logical name.
    Catalog {
        /// Logical index name.
        index: IndexName,
    },
    /// Resolve the publicly visible generation.
    Generation {
        /// Logical index name.
        index: IndexName,
    },
}

/// Non-empty ordered projection target selection.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(try_from = "Vec<EntityRef>", into = "Vec<EntityRef>")]
pub struct EntitySelection(Vec<EntityRef>);

impl EntitySelection {
    /// Constructs a non-empty selection.
    pub fn try_new(values: Vec<EntityRef>) -> Result<Self> {
        if values.is_empty() {
            return Err(TestkitError::EmptyCollection {
                kind: "projection targets",
            });
        }
        Ok(Self(values))
    }

    /// Borrows ordered targets.
    pub fn as_slice(&self) -> &[EntityRef] {
        &self.0
    }
}

impl TryFrom<Vec<EntityRef>> for EntitySelection {
    type Error = TestkitError;

    fn try_from(values: Vec<EntityRef>) -> Result<Self> {
        Self::try_new(values)
    }
}

impl From<EntitySelection> for Vec<EntityRef> {
    fn from(value: EntitySelection) -> Self {
        value.0
    }
}

/// Non-empty ordered projection field selection.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(try_from = "Vec<PropertyName>", into = "Vec<PropertyName>")]
pub struct ProjectionFields(Vec<PropertyName>);

impl ProjectionFields {
    /// Constructs a non-empty field list.
    pub fn try_new(values: Vec<PropertyName>) -> Result<Self> {
        if values.is_empty() {
            return Err(TestkitError::EmptyCollection {
                kind: "projection fields",
            });
        }
        Ok(Self(values))
    }

    /// Borrows ordered fields.
    pub fn as_slice(&self) -> &[PropertyName] {
        &self.0
    }
}

impl TryFrom<Vec<PropertyName>> for ProjectionFields {
    type Error = TestkitError;

    fn try_from(values: Vec<PropertyName>) -> Result<Self> {
        Self::try_new(values)
    }
}

impl From<ProjectionFields> for Vec<PropertyName> {
    fn from(value: ProjectionFields) -> Self {
        value.0
    }
}

/// Graph writes and explicitly conflicting write pairs.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(tag = "kind", rename_all = "snake_case")]
pub enum WriteAction {
    /// Insert a node with caller-assigned model identity.
    InsertNode {
        /// Node identity.
        id: EntityId,
        /// Node label.
        label: LabelName,
        /// Initial properties.
        properties: BTreeMap<PropertyName, PropertyValue>,
    },
    /// Insert an edge whose endpoints must already exist.
    InsertEdge {
        /// Edge identity.
        id: EntityId,
        /// Edge label.
        label: LabelName,
        /// Source node.
        from: EntityId,
        /// Destination node.
        to: EntityId,
        /// Initial properties.
        properties: BTreeMap<PropertyName, PropertyValue>,
    },
    /// Update one existing element.
    Update {
        /// Element to update.
        target: EntityRef,
        /// Non-empty property mutation set.
        patch: PropertyPatch,
    },
    /// Delete one element. Node deletion removes incident edges in the model.
    Delete {
        /// Element to delete.
        target: EntityRef,
    },
    /// Two concurrent writes deliberately targeting the same element.
    Conflicting {
        /// Shared write target.
        target: EntityRef,
        /// First contender.
        left: PropertyPatch,
        /// Second contender.
        right: PropertyPatch,
    },
}

/// Background maintenance actions.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(tag = "kind", rename_all = "snake_case")]
pub enum BackgroundAction {
    /// Drain the bounded worker queue.
    WorkerDrain,
    /// Reconcile durable lifecycle state.
    Reconcile,
    /// Compact one text generation.
    Compact {
        /// Target text generation.
        generation: IndexGeneration,
    },
    /// Reclaim retired physical resources.
    Reclaim {
        /// Target retired generation.
        generation: IndexGeneration,
    },
    /// Activate a reader runtime for indexed traffic.
    ReaderActivation {
        /// Reader runtime.
        runtime: RuntimeId,
    },
    /// Drain a reader runtime.
    ReaderDrain {
        /// Reader runtime.
        runtime: RuntimeId,
    },
}

/// Stable Index V2 crash boundary mirrored from the production failpoint contract.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum StableFailpoint {
    /// Before operation claim.
    ClaimBefore,
    /// After durable operation claim.
    ClaimAfter,
    /// Before bounded operation read.
    BatchReadBefore,
    /// After bounded operation read.
    BatchReadAfter,
    /// Before physical staging.
    PhysicalStagingBefore,
    /// After physical staging.
    PhysicalStagingAfter,
    /// Before operation checkpoint staging.
    CheckpointStagingBefore,
    /// After operation checkpoint staging.
    CheckpointStagingAfter,
    /// Before operation commit.
    CommitBefore,
    /// After durable operation commit.
    CommitAfter,
    /// Before canonical activation.
    ActivationBefore,
    /// After activation staging.
    ActivationAfter,
    /// Before queue-pointer removal.
    QueueRemovalBefore,
    /// After queue-pointer removal staging.
    QueueRemovalAfter,
}

impl StableFailpoint {
    /// Complete stable failpoint matrix.
    pub const ALL: [Self; 14] = [
        Self::ClaimBefore,
        Self::ClaimAfter,
        Self::BatchReadBefore,
        Self::BatchReadAfter,
        Self::PhysicalStagingBefore,
        Self::PhysicalStagingAfter,
        Self::CheckpointStagingBefore,
        Self::CheckpointStagingAfter,
        Self::CommitBefore,
        Self::CommitAfter,
        Self::ActivationBefore,
        Self::ActivationAfter,
        Self::QueueRemovalBefore,
        Self::QueueRemovalAfter,
    ];
}

/// Durable invariant deliberately violated by a corruption action.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum DurableInvariant {
    /// Canonical index catalog record.
    CatalogRecord,
    /// Durable operation record.
    OperationRecord,
    /// Outbox work record.
    WorkRecord,
    /// Generation-qualified physical row.
    PhysicalRow,
    /// Content-addressed text blob.
    BlobObject,
}

/// Fault injection actions.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(tag = "kind", rename_all = "snake_case")]
pub enum FaultAction {
    /// Trigger one stable crash boundary.
    Failpoint {
        /// Exact stable boundary.
        failpoint: StableFailpoint,
    },
    /// Deliberately write malformed durable input while naming the invariant.
    CorruptDurableInput {
        /// Invariant intentionally crossed by the harness.
        invariant: DurableInvariant,
        /// Reproducer bytes.
        bytes: Vec<u8>,
    },
    /// Cancel one in-flight request.
    Cancellation {
        /// Request to cancel.
        request: RequestId,
    },
    /// Reach a deterministic request deadline.
    Timeout {
        /// Request that times out.
        request: RequestId,
        /// Positive logical timeout.
        after_ticks: NonZeroU64,
    },
    /// Simulate a process panic.
    Panic {
        /// Runtime that panics.
        runtime: RuntimeId,
    },
    /// Disconnect the serving transport.
    ServerDisconnect {
        /// Runtime serving the request.
        runtime: RuntimeId,
    },
    /// Restart one process.
    ProcessRestart {
        /// Runtime to restart.
        runtime: RuntimeId,
    },
}

/// Runtime topology and replica-progress actions.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(tag = "kind", rename_all = "snake_case")]
pub enum RuntimeAction {
    /// Open the sole writer.
    OpenWriter,
    /// Close the writer.
    CloseWriter,
    /// Reopen the writer against durable storage.
    ReopenWriter,
    /// Add and open a reader runtime.
    AddReader {
        /// New reader runtime.
        runtime: RuntimeId,
    },
    /// Close and remove a reader runtime.
    RemoveReader {
        /// Removed reader runtime.
        runtime: RuntimeId,
    },
    /// Reopen an existing reader runtime.
    ReopenReader {
        /// Reader runtime.
        runtime: RuntimeId,
    },
    /// Advance replica application through an exact commit sequence.
    AdvanceReplica {
        /// Reader runtime.
        runtime: RuntimeId,
        /// Applied sequence.
        through: Sequence,
    },
    /// Delay replica application by a positive number of commits.
    DelayReplica {
        /// Reader runtime.
        runtime: RuntimeId,
        /// Commit lag.
        by_commits: NonZeroU64,
    },
}

/// Complete closed workload action language.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(tag = "domain", content = "action", rename_all = "snake_case")]
pub enum Action {
    /// Read request.
    Read(ReadAction),
    /// Write request.
    Write(WriteAction),
    /// Index lifecycle request.
    Index(IndexAction),
    /// Background maintenance request.
    Background(BackgroundAction),
    /// Fault injection.
    Fault(FaultAction),
    /// Runtime topology change.
    Runtime(RuntimeAction),
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn range_vector_selection_patch_and_text_boundaries_reject_invalid_states() {
        assert!(EntityRange::try_new(EntityId::new(2), EntityId::new(1)).is_err());
        assert!(VectorValue::try_new(Vec::new()).is_err());
        assert!(EntitySelection::try_new(Vec::new()).is_err());
        assert!(ProjectionFields::try_new(Vec::new()).is_err());
        assert!(PropertyPatch::try_new(BTreeMap::new()).is_err());
        assert!(TextQuery::try_new("  ").is_err());
    }

    #[test]
    fn stable_failpoint_inventory_is_complete_and_round_trips() {
        assert_eq!(StableFailpoint::ALL.len(), 14);
        let json = serde_json::to_string(&StableFailpoint::ActivationAfter).unwrap();
        assert_eq!(json, r#""activation_after""#);
        assert_eq!(
            serde_json::from_str::<StableFailpoint>(&json).unwrap(),
            StableFailpoint::ActivationAfter
        );
    }

    #[test]
    fn entity_references_preserve_kind_and_identity() {
        let entity = EntityRef::Edge(EntityId::new(3));
        assert_eq!(entity.kind(), ElementKind::Edge);
        assert_eq!(entity.id(), EntityId::new(3));
    }
}
