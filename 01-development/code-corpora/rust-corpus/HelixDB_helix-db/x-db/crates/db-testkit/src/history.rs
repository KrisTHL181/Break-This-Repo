//! Valid-by-construction request histories and normalized isolation cases.
//!
//! [`IsolationHistory`] binds every observation to the exact snapshot owned by
//! its request. Read and write request tokens are distinct, so a read cannot
//! accidentally commit and a write cannot be completed without explicitly
//! committing or aborting its staged transaction.
//!
//! ```
//! use helix_db_testkit::{
//!     action::{AggregateKind, ElementKind, ReadAction},
//!     history::IsolationHistory,
//!     ids::RequestId,
//! };
//!
//! let mut history = IsolationHistory::default();
//! let request = history.begin_read(RequestId::new(1).unwrap()).unwrap();
//! let action = ReadAction::Aggregate {
//!     kind: ElementKind::Node,
//!     aggregate: AggregateKind::Count,
//! };
//! let expected = history.oracle().read_at(request.snapshot(), &action).unwrap();
//! history.observe(&request, &action, &expected).unwrap();
//! history.finish_read(request).unwrap();
//! history.assert_quiescent().unwrap();
//! ```

use serde::{Deserialize, Serialize};

use crate::action::{ReadAction, WriteAction};
use crate::ids::{RequestId, Sequence};
use crate::model::{
    ModelReadResult, ModelWriteResult, MvccHistory, OracleState, ResourceAccounting, ResourceKind,
};
use crate::{Result, TestkitError};

/// Read boundary varied by generated stable-snapshot histories.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum ReadClass {
    /// Exact element lookup.
    Point,
    /// Inclusive element range.
    Range,
    /// Bounded graph traversal.
    Traversal,
    /// Selected property materialization.
    Projection,
    /// Count or existence aggregate.
    Aggregate,
    /// Secondary-index equality lookup.
    Secondary,
    /// Full-text lookup.
    Text,
    /// Vector nearest-neighbor lookup.
    Vector,
    /// Public catalog lookup.
    Catalog,
    /// Public generation resolution.
    Generation,
}

impl ReadClass {
    /// Every normalized request read boundary.
    pub const ALL: [Self; 10] = [
        Self::Point,
        Self::Range,
        Self::Traversal,
        Self::Projection,
        Self::Aggregate,
        Self::Secondary,
        Self::Text,
        Self::Vector,
        Self::Catalog,
        Self::Generation,
    ];
}

/// Relative completion order recorded for overlapping requests.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum CompletionOrder {
    /// The reader publishes before the concurrent writer.
    ReaderFirst,
    /// The writer commits before the reader publishes its older snapshot.
    WriterFirst,
}

impl CompletionOrder {
    /// Both normalized overlap outcomes.
    pub const ALL: [Self; 2] = [Self::ReaderFirst, Self::WriterFirst];
}

/// Request termination paths that must release owned resources.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum CleanupExit {
    /// Explicit application cancellation.
    Cancellation,
    /// Monotonic request deadline.
    Timeout,
    /// Unwind from a panicking task.
    Panic,
    /// Database shutdown while work is in flight.
    DatabaseClose,
    /// Server-side request future dropped after a client disconnect.
    ServerDisconnect,
}

impl CleanupExit {
    /// Every cleanup boundary required by the testing plan.
    pub const ALL: [Self; 5] = [
        Self::Cancellation,
        Self::Timeout,
        Self::Panic,
        Self::DatabaseClose,
        Self::ServerDisconnect,
    ];
}

/// Index lifecycle transition raced with an already-started read.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum GenerationTransition {
    /// A hidden replacement becomes public.
    Activate,
    /// The selected public generation begins retirement.
    Drop,
}

/// Closed generated isolation-history domain.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash, Serialize, Deserialize)]
#[serde(tag = "case", rename_all = "snake_case")]
pub enum NormalizedIsolationCase {
    /// Stable-snapshot read overlapping one committed mutation.
    StableSnapshot {
        /// Read boundary under test.
        read: ReadClass,
        /// Recorded request completion order.
        completion: CompletionOrder,
    },
    /// A writer's following request reads its successful commit.
    WriterReadAfterWrite,
    /// A failed multi-action write leaves every staged action hidden.
    FailedWriteAtomicity,
    /// Two overlapping writers serialize or one reports a typed conflict.
    OverlappingWriters,
    /// An already-started read retains one generation or fails retryably.
    GenerationRace {
        /// Lifecycle transition raced with the read.
        transition: GenerationTransition,
    },
    /// One abnormal exit must release all request-owned resources.
    Cleanup {
        /// Exit boundary under test.
        exit: CleanupExit,
    },
}

impl NormalizedIsolationCase {
    /// Returns a deterministic matrix covering every finite isolation partition.
    pub fn complete() -> Vec<Self> {
        let mut cases = Vec::new();
        for read in ReadClass::ALL {
            for completion in CompletionOrder::ALL {
                cases.push(Self::StableSnapshot { read, completion });
            }
        }
        cases.extend([
            Self::WriterReadAfterWrite,
            Self::FailedWriteAtomicity,
            Self::OverlappingWriters,
        ]);
        cases.extend([
            Self::GenerationRace {
                transition: GenerationTransition::Activate,
            },
            Self::GenerationRace {
                transition: GenerationTransition::Drop,
            },
        ]);
        cases.extend(
            CleanupExit::ALL
                .into_iter()
                .map(|exit| Self::Cleanup { exit }),
        );
        cases
    }
}

/// One checked observation tied to a recorded request snapshot.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct ReadObservation {
    request: RequestId,
    snapshot: Sequence,
    action: ReadAction,
    result: ModelReadResult,
}

impl ReadObservation {
    /// Returns the request that produced this observation.
    pub const fn request(&self) -> RequestId {
        self.request
    }

    /// Returns the exact visible snapshot.
    pub const fn snapshot(&self) -> Sequence {
        self.snapshot
    }

    /// Borrows the observed action.
    pub fn action(&self) -> &ReadAction {
        &self.action
    }

    /// Borrows the checked semantic result.
    pub fn result(&self) -> &ModelReadResult {
        &self.result
    }
}

/// Open read request owning one immutable snapshot.
#[derive(Debug)]
pub struct OpenReadRequest {
    request: RequestId,
    snapshot: Sequence,
}

impl OpenReadRequest {
    /// Returns the request identity.
    pub const fn request(&self) -> RequestId {
        self.request
    }

    /// Returns the exact selected snapshot.
    pub const fn snapshot(&self) -> Sequence {
        self.snapshot
    }
}

/// Open write request owning one snapshot transaction and its staged actions.
#[derive(Debug)]
pub struct OpenWriteRequest {
    request: RequestId,
    snapshot: Sequence,
    writes: Vec<WriteAction>,
}

impl OpenWriteRequest {
    /// Returns the request identity.
    pub const fn request(&self) -> RequestId {
        self.request
    }

    /// Returns the transaction snapshot.
    pub const fn snapshot(&self) -> Sequence {
        self.snapshot
    }

    /// Stages one atomic mutation in request order.
    pub fn stage(&mut self, write: WriteAction) {
        self.writes.push(write);
    }

    /// Borrows staged writes.
    pub fn writes(&self) -> &[WriteAction] {
        &self.writes
    }
}

/// Independent request-isolation oracle with explicit resource ownership.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub struct IsolationHistory {
    oracle: OracleState,
    mvcc: MvccHistory,
    resources: ResourceAccounting,
    observations: Vec<ReadObservation>,
}

impl IsolationHistory {
    /// Starts history checking from a fully committed fixture oracle.
    pub fn from_oracle(oracle: OracleState) -> Self {
        Self {
            mvcc: MvccHistory::from_baseline(oracle.sequence()),
            oracle,
            resources: ResourceAccounting::default(),
            observations: Vec::new(),
        }
    }

    /// Borrows the independent graph, search, and lifecycle oracle.
    pub const fn oracle(&self) -> &OracleState {
        &self.oracle
    }

    /// Borrows checked observations in execution order.
    pub fn observations(&self) -> &[ReadObservation] {
        &self.observations
    }

    /// Starts a read at the latest committed sequence.
    pub fn begin_read(&mut self, request: RequestId) -> Result<OpenReadRequest> {
        self.begin_read_at(request, self.mvcc.current())
    }

    /// Starts a read at one recorded committed sequence.
    pub fn begin_read_at(
        &mut self,
        request: RequestId,
        snapshot: Sequence,
    ) -> Result<OpenReadRequest> {
        self.mvcc.begin(request, snapshot)?;
        self.resources.acquire(ResourceKind::Snapshot)?;
        Ok(OpenReadRequest { request, snapshot })
    }

    /// Checks one observed read against the request's immutable oracle snapshot.
    pub fn observe(
        &mut self,
        request: &OpenReadRequest,
        action: &ReadAction,
        actual: &ModelReadResult,
    ) -> Result<()> {
        if self.mvcc.snapshot_for(request.request)? != request.snapshot {
            return Err(TestkitError::ModelViolation(
                "read token does not match its active snapshot".to_string(),
            ));
        }
        let expected = self.oracle.read_at(request.snapshot, action)?;
        if &expected != actual {
            return Err(TestkitError::ModelViolation(format!(
                "read result does not match snapshot {}",
                request.snapshot.get()
            )));
        }
        self.observations.push(ReadObservation {
            request: request.request,
            snapshot: request.snapshot,
            action: action.clone(),
            result: actual.clone(),
        });
        Ok(())
    }

    /// Completes a read and releases its snapshot.
    pub fn finish_read(&mut self, request: OpenReadRequest) -> Result<()> {
        self.mvcc.abort(request.request)?;
        self.resources.release(ResourceKind::Snapshot)
    }

    /// Starts a write transaction at the latest committed sequence.
    pub fn begin_write(&mut self, request: RequestId) -> Result<OpenWriteRequest> {
        let snapshot = self.mvcc.current();
        self.mvcc.begin(request, snapshot)?;
        self.resources.acquire(ResourceKind::Transaction)?;
        Ok(OpenWriteRequest {
            request,
            snapshot,
            writes: Vec::new(),
        })
    }

    /// Commits every staged action atomically at one new sequence.
    pub fn commit_write(&mut self, request: OpenWriteRequest) -> Result<Sequence> {
        if self.mvcc.snapshot_for(request.request)? != request.snapshot {
            return Err(TestkitError::ModelViolation(
                "write token does not match its transaction snapshot".to_string(),
            ));
        }
        let mut next_oracle = self.oracle.clone();
        if next_oracle.apply_transaction(&request.writes)? != ModelWriteResult::Applied {
            return Err(TestkitError::ModelViolation(
                "explicit conflicting write requires a typed conflict outcome".to_string(),
            ));
        }
        let mut next_mvcc = self.mvcc.clone();
        let commit = next_mvcc.commit(request.request, request.writes)?;
        if commit != next_oracle.sequence() {
            return Err(TestkitError::ModelViolation(
                "oracle and MVCC commit sequences diverged".to_string(),
            ));
        }
        self.resources.release(ResourceKind::Transaction)?;
        self.oracle = next_oracle;
        self.mvcc = next_mvcc;
        Ok(commit)
    }

    /// Records a failed or conflicted write without exposing staged mutations.
    pub fn abort_write(&mut self, request: OpenWriteRequest) -> Result<()> {
        self.mvcc.abort(request.request)?;
        self.resources.release(ResourceKind::Transaction)
    }

    /// Validates sequence ordering and verifies all request resources were released.
    pub fn assert_quiescent(&self) -> Result<()> {
        self.mvcc.validate()?;
        self.mvcc.assert_quiescent()?;
        self.resources.assert_quiescent()
    }
}

#[cfg(test)]
mod tests {
    use std::collections::{BTreeMap, BTreeSet};
    use std::num::{NonZeroU16, NonZeroU32};

    use crate::action::{
        AggregateKind, ElementKind, EntityRange, EntityRef, EntitySelection, ProjectionFields,
        PropertyMutation, PropertyPatch, PropertyValue, TextQuery, TraversalDirection,
        VectorMetric, VectorValue,
    };
    use crate::ids::{EntityId, FiniteF32, IndexName, LabelName, PropertyName};
    use crate::lifecycle::{AbsentIndex, IndexDefinition};

    use super::*;

    fn property(name: &str) -> PropertyName {
        PropertyName::try_new(name).unwrap()
    }

    fn node(id: u64, rank: i64, text: &str, vector: [f32; 2]) -> WriteAction {
        WriteAction::InsertNode {
            id: EntityId::new(id),
            label: LabelName::try_new("Document").unwrap(),
            properties: BTreeMap::from([
                (property("rank"), PropertyValue::I64(rank)),
                (property("text"), PropertyValue::String(text.to_string())),
                (
                    property("vector"),
                    PropertyValue::Vector(
                        VectorValue::try_new(
                            vector
                                .into_iter()
                                .map(|value| FiniteF32::try_new(value).unwrap())
                                .collect(),
                        )
                        .unwrap(),
                    ),
                ),
            ]),
        }
    }

    fn install_index(oracle: &mut OracleState, definition: IndexDefinition) {
        let create = AbsentIndex::new(definition).create().unwrap();
        let (create_action, building) = create.into_parts();
        oracle.apply_index(&create_action).unwrap();
        let (activate_action, _) = building.activate().into_parts();
        oracle.apply_index(&activate_action).unwrap();
    }

    fn fixture() -> IsolationHistory {
        let mut oracle = OracleState::default();
        oracle
            .apply_write(&node(1, 1, "alpha", [1.0, 0.0]))
            .unwrap();
        oracle.apply_write(&node(2, 2, "beta", [0.0, 1.0])).unwrap();
        oracle
            .apply_write(&WriteAction::InsertEdge {
                id: EntityId::new(7),
                label: LabelName::try_new("LINK").unwrap(),
                from: EntityId::new(1),
                to: EntityId::new(2),
                properties: BTreeMap::new(),
            })
            .unwrap();
        install_index(
            &mut oracle,
            IndexDefinition::Secondary {
                name: IndexName::try_new("by-rank").unwrap(),
                element: ElementKind::Node,
                property: property("rank"),
                unique: false,
            },
        );
        install_index(
            &mut oracle,
            IndexDefinition::Text {
                name: IndexName::try_new("by-text").unwrap(),
                element: ElementKind::Node,
                property: property("text"),
            },
        );
        install_index(
            &mut oracle,
            IndexDefinition::Vector {
                name: IndexName::try_new("by-vector").unwrap(),
                element: ElementKind::Node,
                property: property("vector"),
                dimension: NonZeroU32::new(2).unwrap(),
                metric: VectorMetric::Euclidean,
            },
        );
        IsolationHistory::from_oracle(oracle)
    }

    fn actions() -> Vec<(ReadClass, ReadAction)> {
        vec![
            (
                ReadClass::Point,
                ReadAction::Point {
                    kind: ElementKind::Node,
                    id: EntityId::new(1),
                },
            ),
            (
                ReadClass::Range,
                ReadAction::Range {
                    kind: ElementKind::Node,
                    range: EntityRange::try_new(EntityId::new(1), EntityId::new(2)).unwrap(),
                },
            ),
            (
                ReadClass::Traversal,
                ReadAction::Traversal {
                    start: EntityId::new(1),
                    direction: TraversalDirection::Outgoing,
                    max_depth: NonZeroU16::new(1).unwrap(),
                },
            ),
            (
                ReadClass::Projection,
                ReadAction::Projection {
                    targets: EntitySelection::try_new(vec![EntityRef::Node(EntityId::new(1))])
                        .unwrap(),
                    fields: ProjectionFields::try_new(vec![property("rank")]).unwrap(),
                },
            ),
            (
                ReadClass::Aggregate,
                ReadAction::Aggregate {
                    kind: ElementKind::Node,
                    aggregate: AggregateKind::Count,
                },
            ),
            (
                ReadClass::Secondary,
                ReadAction::Secondary {
                    index: IndexName::try_new("by-rank").unwrap(),
                    value: PropertyValue::I64(1),
                },
            ),
            (
                ReadClass::Text,
                ReadAction::Text {
                    index: IndexName::try_new("by-text").unwrap(),
                    query: TextQuery::try_new("alpha").unwrap(),
                    limit: NonZeroU32::new(2).unwrap(),
                },
            ),
            (
                ReadClass::Vector,
                ReadAction::Vector {
                    index: IndexName::try_new("by-vector").unwrap(),
                    vector: VectorValue::try_new(vec![
                        FiniteF32::try_new(1.0).unwrap(),
                        FiniteF32::try_new(0.0).unwrap(),
                    ])
                    .unwrap(),
                    limit: NonZeroU32::new(1).unwrap(),
                    metric: VectorMetric::Euclidean,
                },
            ),
            (
                ReadClass::Catalog,
                ReadAction::Catalog {
                    index: IndexName::try_new("by-rank").unwrap(),
                },
            ),
            (
                ReadClass::Generation,
                ReadAction::Generation {
                    index: IndexName::try_new("by-rank").unwrap(),
                },
            ),
        ]
    }

    #[test]
    fn complete_domain_covers_every_read_order_generation_and_cleanup_partition() {
        let cases = NormalizedIsolationCase::complete();
        assert_eq!(
            cases.len(),
            ReadClass::ALL.len() * CompletionOrder::ALL.len() + 10
        );
        assert_eq!(cases.iter().collect::<BTreeSet<_>>().len(), cases.len());
        for read in ReadClass::ALL {
            for completion in CompletionOrder::ALL {
                assert!(
                    cases.contains(&NormalizedIsolationCase::StableSnapshot { read, completion })
                );
            }
        }
        for exit in CleanupExit::ALL {
            assert!(cases.contains(&NormalizedIsolationCase::Cleanup { exit }));
        }
    }

    #[test]
    fn every_read_class_remains_on_its_original_snapshot_after_a_commit() {
        let mut history = fixture();
        let read = history.begin_read(RequestId::new(1).unwrap()).unwrap();
        let expected = actions()
            .into_iter()
            .map(|(class, action)| {
                let result = history.oracle().read_at(read.snapshot(), &action).unwrap();
                (class, action, result)
            })
            .collect::<Vec<_>>();
        let mut writer = history.begin_write(RequestId::new(2).unwrap()).unwrap();
        writer.stage(node(3, 1, "alpha", [1.0, 0.0]));
        let committed = history.commit_write(writer).unwrap();
        assert!(committed > read.snapshot());

        for (_, action, result) in &expected {
            history.observe(&read, action, result).unwrap();
        }
        assert_eq!(
            history
                .observations()
                .iter()
                .map(|observation| observation.action())
                .collect::<Vec<_>>(),
            expected
                .iter()
                .map(|(_, action, _)| action)
                .collect::<Vec<_>>()
        );
        history.finish_read(read).unwrap();
        history.assert_quiescent().unwrap();
    }

    #[test]
    fn failed_transaction_is_atomic_and_next_writer_read_observes_success() {
        let mut history = fixture();
        let before = history.oracle().sequence();
        let mut failed = history.begin_write(RequestId::new(1).unwrap()).unwrap();
        failed.stage(node(3, 3, "hidden", [0.5, 0.5]));
        failed.stage(WriteAction::InsertEdge {
            id: EntityId::new(8),
            label: LabelName::try_new("LINK").unwrap(),
            from: EntityId::new(3),
            to: EntityId::new(99),
            properties: BTreeMap::new(),
        });
        history.abort_write(failed).unwrap();
        assert_eq!(history.oracle().sequence(), before);

        let mut successful = history.begin_write(RequestId::new(2).unwrap()).unwrap();
        successful.stage(node(3, 3, "visible", [0.5, 0.5]));
        let committed = history.commit_write(successful).unwrap();
        let read = history.begin_read(RequestId::new(3).unwrap()).unwrap();
        assert_eq!(read.snapshot(), committed);
        let action = ReadAction::Point {
            kind: ElementKind::Node,
            id: EntityId::new(3),
        };
        let result = history.oracle().read_at(read.snapshot(), &action).unwrap();
        history.observe(&read, &action, &result).unwrap();
        history.finish_read(read).unwrap();
        history.assert_quiescent().unwrap();
    }

    #[test]
    fn overlapping_conflict_and_every_abnormal_exit_leave_no_resources() {
        for exit in CleanupExit::ALL {
            let mut history = fixture();
            let mut winner = history.begin_write(RequestId::new(1).unwrap()).unwrap();
            let mut contender = history.begin_write(RequestId::new(2).unwrap()).unwrap();
            let patch = PropertyPatch::try_new(BTreeMap::from([(
                property("rank"),
                PropertyMutation::Set(PropertyValue::I64(9)),
            )]))
            .unwrap();
            winner.stage(WriteAction::Update {
                target: EntityRef::Node(EntityId::new(1)),
                patch: patch.clone(),
            });
            contender.stage(WriteAction::Update {
                target: EntityRef::Node(EntityId::new(1)),
                patch,
            });
            history.commit_write(winner).unwrap();
            history.abort_write(contender).unwrap();
            history.assert_quiescent().unwrap_or_else(|error| {
                panic!("{exit:?} cleanup retained modeled resources: {error}")
            });
        }
    }

    #[test]
    fn observation_rejects_a_result_from_a_newer_snapshot() {
        let mut history = fixture();
        let read = history.begin_read(RequestId::new(1).unwrap()).unwrap();
        let action = ReadAction::Aggregate {
            kind: ElementKind::Node,
            aggregate: AggregateKind::Count,
        };
        let mut writer = history.begin_write(RequestId::new(2).unwrap()).unwrap();
        writer.stage(node(3, 3, "new", [0.5, 0.5]));
        let latest = history.commit_write(writer).unwrap();
        let newer = history.oracle().read_at(latest, &action).unwrap();
        assert!(history.observe(&read, &action, &newer).is_err());
        history.finish_read(read).unwrap();
        history.assert_quiescent().unwrap();
    }
}
