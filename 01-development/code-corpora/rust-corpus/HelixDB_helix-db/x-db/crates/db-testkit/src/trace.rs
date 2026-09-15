//! Stable replay trace and recorder contracts.

use std::collections::BTreeSet;

use serde::{Deserialize, Serialize};

use crate::action::{Action, EntityRef};
use crate::ids::{GenerationId, RequestId, RuntimeId, Sequence, StableSeed, TenantId};
use crate::lifecycle::{IndexActionKind, IndexBlocker};
use crate::model::{
    IndexCatalogView, LifecycleModel, ModelReadResult, ProjectionRow, ScoredEntity,
};
use crate::{Result, TestkitError};

/// Current stable trace schema.
pub const TRACE_SCHEMA_VERSION: u32 = 1;

/// Monotonic logical clock used instead of scheduler or wall-clock assumptions.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash, Serialize, Deserialize)]
pub struct LogicalTime(u64);

impl LogicalTime {
    /// Returns the initial logical time.
    pub const fn initial() -> Self {
        Self(0)
    }

    /// Returns the raw logical tick.
    pub const fn get(self) -> u64 {
        self.0
    }

    fn checked_next(self) -> Result<Self> {
        let Some(next) = self.0.checked_add(1) else {
            return Err(TestkitError::TraceViolation(
                "logical trace clock exhausted".to_string(),
            ));
        };
        Ok(Self(next))
    }
}

/// Typed request or infrastructure failure recorded by adapters.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(tag = "kind", rename_all = "snake_case")]
pub enum TypedError {
    /// Serializable write conflict or retry.
    Conflict {
        /// Stable machine-readable code.
        code: String,
    },
    /// Lifecycle build became blocked.
    IndexBlocked {
        /// Stable model blocker.
        blocker: IndexBlocker,
        /// Stable machine-readable code.
        code: String,
    },
    /// Retryable generation or replica error.
    Retryable {
        /// Stable machine-readable code.
        code: String,
    },
    /// Invalid caller request.
    InvalidRequest {
        /// Stable machine-readable code.
        code: String,
    },
    /// Durable corruption failed closed.
    Corruption {
        /// Stable machine-readable code.
        code: String,
    },
    /// Request was cancelled.
    Cancelled,
    /// Deterministic deadline elapsed.
    Timeout,
    /// Required runtime dependency was unavailable.
    Unavailable {
        /// Stable machine-readable code.
        code: String,
    },
    /// Unexpected internal failure.
    Internal {
        /// Stable machine-readable code.
        code: String,
    },
}

/// Adapter-independent observed success value.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(tag = "kind", content = "value", rename_all = "snake_case")]
pub enum ObservedValue {
    /// Ordered graph elements.
    Entities(Vec<EntityRef>),
    /// Ordered projection rows.
    Projection(Vec<ProjectionRow>),
    /// Count aggregate.
    Count(usize),
    /// Boolean aggregate.
    Bool(bool),
    /// Ordered scored results.
    Scored(Vec<ScoredEntity>),
    /// Public catalog result.
    Catalog(Option<IndexCatalogView>),
    /// Public generation result.
    Generation(Option<GenerationId>),
    /// Mutation or maintenance acknowledgement.
    Acknowledged,
    /// Transport JSON used by parity adapters.
    Json(serde_json::Value),
}

impl From<ModelReadResult> for ObservedValue {
    fn from(value: ModelReadResult) -> Self {
        match value {
            ModelReadResult::Entities(value) => Self::Entities(value),
            ModelReadResult::Projection(value) => Self::Projection(value),
            ModelReadResult::Count(value) => Self::Count(value),
            ModelReadResult::Bool(value) => Self::Bool(value),
            ModelReadResult::Scored(value) => Self::Scored(value),
            ModelReadResult::Catalog(value) => Self::Catalog(value),
            ModelReadResult::Generation(value) => Self::Generation(value),
        }
    }
}

/// Recorded request outcome.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(tag = "status", content = "value", rename_all = "snake_case")]
pub enum TraceOutcome {
    /// Successful request value.
    Success(ObservedValue),
    /// Typed request failure.
    Error(TypedError),
}

/// Fields recorded when a request starts.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct RequestStart {
    /// Stable request identity.
    pub request_id: RequestId,
    /// Logical start time.
    pub started_at: LogicalTime,
    /// Stable snapshot sequence selected for this request.
    pub snapshot_sequence: Sequence,
    /// Runtime serving the request.
    pub runtime: RuntimeId,
    /// Tenant storage scope.
    pub tenant: TenantId,
    /// Selected index generation, if the request resolved one.
    pub selected_generation: Option<GenerationId>,
    /// Typed action.
    pub action: Action,
}

/// Fields recorded when a request ends.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct RequestEnd {
    /// Logical finish time.
    pub finished_at: LogicalTime,
    /// Commit sequence for an acknowledged mutation.
    pub commit_sequence: Option<Sequence>,
    /// Typed result or error.
    pub outcome: TraceOutcome,
}

/// Complete request trace with explicit start and end boundaries.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct RequestTrace {
    /// Request-start fields.
    pub start: RequestStart,
    /// Request-end fields.
    pub end: RequestEnd,
}

/// Replayable workload trace.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct ReplayTrace {
    schema_version: u32,
    seed: StableSeed,
    requests: Vec<RequestTrace>,
}

impl ReplayTrace {
    /// Constructs and validates a trace.
    pub fn try_new(seed: StableSeed, requests: Vec<RequestTrace>) -> Result<Self> {
        let trace = Self {
            schema_version: TRACE_SCHEMA_VERSION,
            seed,
            requests,
        };
        trace.validate()?;
        Ok(trace)
    }

    /// Returns the stable seed.
    pub const fn seed(&self) -> StableSeed {
        self.seed
    }

    /// Borrows requests in recorded completion order.
    pub fn requests(&self) -> &[RequestTrace] {
        &self.requests
    }

    /// Consumes the trace into requests.
    pub fn into_requests(self) -> Vec<RequestTrace> {
        self.requests
    }

    /// Validates clocks, identities, commit order, and lifecycle action order.
    pub fn validate(&self) -> Result<()> {
        if self.schema_version != TRACE_SCHEMA_VERSION {
            return Err(TestkitError::TraceViolation(format!(
                "unsupported trace schema {}",
                self.schema_version
            )));
        }
        let mut request_ids = BTreeSet::new();
        let mut previous_commit = Sequence::initial();
        let mut lifecycle = LifecycleModel::default();
        for request in &self.requests {
            if !request_ids.insert(request.start.request_id) {
                return Err(TestkitError::TraceViolation(format!(
                    "duplicate request ID {}",
                    request.start.request_id.get()
                )));
            }
            if request.start.started_at >= request.end.finished_at {
                return Err(TestkitError::TraceViolation(
                    "request finish must follow its start".to_string(),
                ));
            }
            if let Some(commit) = request.end.commit_sequence {
                if commit <= previous_commit || commit <= request.start.snapshot_sequence {
                    return Err(TestkitError::TraceViolation(
                        "commit sequences must increase and follow request snapshots".to_string(),
                    ));
                }
                if !matches!(request.end.outcome, TraceOutcome::Success(_)) {
                    return Err(TestkitError::TraceViolation(
                        "failed requests cannot record a commit".to_string(),
                    ));
                }
                previous_commit = commit;
            }
            let Action::Index(action) = &request.start.action else {
                continue;
            };
            match &request.end.outcome {
                TraceOutcome::Success(_) => lifecycle.apply(action)?,
                TraceOutcome::Error(TypedError::IndexBlocked { blocker, .. }) => {
                    if action.kind() != IndexActionKind::Build {
                        return Err(TestkitError::TraceViolation(
                            "only build work can enter a blocked lifecycle state".to_string(),
                        ));
                    }
                    lifecycle.mark_blocked(action.generation(), *blocker)?;
                }
                TraceOutcome::Error(
                    TypedError::Conflict { .. }
                    | TypedError::Retryable { .. }
                    | TypedError::InvalidRequest { .. }
                    | TypedError::Corruption { .. }
                    | TypedError::Cancelled
                    | TypedError::Timeout
                    | TypedError::Unavailable { .. }
                    | TypedError::Internal { .. },
                ) => {}
            }
        }
        Ok(())
    }

    /// Serializes validated pretty JSON.
    pub fn to_json(&self) -> Result<Vec<u8>> {
        self.validate()?;
        Ok(serde_json::to_vec_pretty(self)?)
    }

    /// Deserializes and validates untrusted trace JSON.
    pub fn from_json(bytes: &[u8]) -> Result<Self> {
        let trace: Self = serde_json::from_slice(bytes)?;
        trace.validate()?;
        Ok(trace)
    }
}

/// Incomplete request token that must be consumed by [`TraceRecorder::finish_request`].
#[derive(Debug)]
pub struct PendingRequest {
    start: RequestStart,
}

impl PendingRequest {
    /// Records a selected index generation.
    pub fn with_generation(mut self, generation: GenerationId) -> Self {
        self.start.selected_generation = Some(generation);
        self
    }
}

/// Valid-by-construction logical trace recorder.
#[derive(Debug)]
pub struct TraceRecorder {
    seed: StableSeed,
    clock: LogicalTime,
    active: BTreeSet<RequestId>,
    completed: BTreeSet<RequestId>,
    requests: Vec<RequestTrace>,
}

impl TraceRecorder {
    /// Starts an empty recorder for one stable seed.
    pub fn new(seed: StableSeed) -> Self {
        Self {
            seed,
            clock: LogicalTime::initial(),
            active: BTreeSet::new(),
            completed: BTreeSet::new(),
            requests: Vec::new(),
        }
    }

    /// Starts one request and returns a token owning its unfinished state.
    pub fn start_request(
        &mut self,
        request_id: RequestId,
        runtime: RuntimeId,
        tenant: TenantId,
        snapshot_sequence: Sequence,
        action: Action,
    ) -> Result<PendingRequest> {
        if self.completed.contains(&request_id) || !self.active.insert(request_id) {
            return Err(TestkitError::TraceViolation(format!(
                "duplicate request ID {}",
                request_id.get()
            )));
        }
        self.clock = self.clock.checked_next()?;
        Ok(PendingRequest {
            start: RequestStart {
                request_id,
                started_at: self.clock,
                snapshot_sequence,
                runtime,
                tenant,
                selected_generation: None,
                action,
            },
        })
    }

    /// Finishes one pending request at the next logical tick.
    pub fn finish_request(
        &mut self,
        pending: PendingRequest,
        commit_sequence: Option<Sequence>,
        outcome: TraceOutcome,
    ) {
        let removed = self.active.remove(&pending.start.request_id);
        assert!(removed, "pending request must belong to this recorder");
        let inserted = self.completed.insert(pending.start.request_id);
        assert!(inserted, "pending request must finish exactly once");
        self.clock = self
            .clock
            .checked_next()
            .expect("trace clock cannot exhaust after a successful start");
        self.requests.push(RequestTrace {
            start: pending.start,
            end: RequestEnd {
                finished_at: self.clock,
                commit_sequence,
                outcome,
            },
        });
    }

    /// Finishes and validates the complete trace.
    pub fn finish(self) -> Result<ReplayTrace> {
        if !self.active.is_empty() {
            return Err(TestkitError::TraceViolation(format!(
                "{} requests did not finish",
                self.active.len()
            )));
        }
        ReplayTrace::try_new(self.seed, self.requests)
    }
}

#[cfg(test)]
mod tests {
    use crate::action::{ElementKind, ReadAction};
    use crate::ids::EntityId;

    use super::*;

    fn request(id: u64) -> RequestStart {
        RequestStart {
            request_id: RequestId::new(id).unwrap(),
            started_at: LogicalTime(id * 2 - 1),
            snapshot_sequence: Sequence::initial(),
            runtime: RuntimeId::new(1).unwrap(),
            tenant: TenantId::try_new("tenant").unwrap(),
            selected_generation: None,
            action: Action::Read(ReadAction::Point {
                kind: ElementKind::Node,
                id: EntityId::new(1),
            }),
        }
    }

    #[test]
    fn recorder_and_json_round_trip_preserve_required_fields() {
        let mut recorder = TraceRecorder::new(StableSeed::new(8));
        let pending = recorder
            .start_request(
                RequestId::new(1).unwrap(),
                RuntimeId::new(2).unwrap(),
                TenantId::try_new("tenant").unwrap(),
                Sequence::initial(),
                Action::Read(ReadAction::Point {
                    kind: ElementKind::Node,
                    id: EntityId::new(3),
                }),
            )
            .unwrap()
            .with_generation(GenerationId::new(4).unwrap());
        recorder.finish_request(
            pending,
            None,
            TraceOutcome::Success(ObservedValue::Entities(Vec::new())),
        );
        let trace = recorder.finish().unwrap();
        let decoded = ReplayTrace::from_json(&trace.to_json().unwrap()).unwrap();
        assert_eq!(decoded, trace);
        assert_eq!(decoded.seed(), StableSeed::new(8));
    }

    #[test]
    fn validation_rejects_duplicate_ids_bad_clocks_and_failed_commits() {
        let first = RequestTrace {
            start: request(1),
            end: RequestEnd {
                finished_at: LogicalTime(2),
                commit_sequence: None,
                outcome: TraceOutcome::Success(ObservedValue::Acknowledged),
            },
        };
        assert!(ReplayTrace::try_new(StableSeed::new(1), vec![first.clone(), first]).is_err());

        let mut bad_clock = RequestTrace {
            start: request(1),
            end: RequestEnd {
                finished_at: LogicalTime(1),
                commit_sequence: None,
                outcome: TraceOutcome::Success(ObservedValue::Acknowledged),
            },
        };
        assert!(ReplayTrace::try_new(StableSeed::new(1), vec![bad_clock.clone()]).is_err());
        bad_clock.end.finished_at = LogicalTime(2);
        bad_clock.end.commit_sequence = Some(Sequence::new(1));
        bad_clock.end.outcome = TraceOutcome::Error(TypedError::Timeout);
        assert!(ReplayTrace::try_new(StableSeed::new(1), vec![bad_clock]).is_err());
    }
}
