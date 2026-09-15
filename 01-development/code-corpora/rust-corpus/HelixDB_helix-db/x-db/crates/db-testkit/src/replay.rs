//! Adapter-independent asynchronous trace replay.

use async_trait::async_trait;
use serde::{Deserialize, Serialize};

use crate::ids::{RequestId, Sequence};
use crate::trace::{ReplayTrace, RequestStart, TraceOutcome};
use crate::Result;

/// Actual result returned by a workload adapter.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct AdapterObservation {
    /// Commit sequence for an acknowledged mutation.
    pub commit_sequence: Option<Sequence>,
    /// Typed result or error.
    pub outcome: TraceOutcome,
}

/// Runtime boundary implemented by embedded, service, and transport adapters.
#[async_trait]
pub trait WorkloadAdapter: Send {
    /// Executes one request using its recorded runtime, tenant, snapshot, and action.
    async fn execute(&mut self, request: &RequestStart) -> AdapterObservation;
}

/// One exact replay disagreement.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct ReplayMismatch {
    /// Request that disagreed.
    pub request: RequestId,
    /// Expected trace observation.
    pub expected: AdapterObservation,
    /// Actual adapter observation.
    pub actual: AdapterObservation,
}

/// Complete replay comparison report.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct ReplayReport {
    requests_executed: usize,
    mismatches: Vec<ReplayMismatch>,
}

impl ReplayReport {
    /// Returns the number of requests sent to the adapter.
    pub const fn requests_executed(&self) -> usize {
        self.requests_executed
    }

    /// Borrows exact mismatches.
    pub fn mismatches(&self) -> &[ReplayMismatch] {
        &self.mismatches
    }

    /// Returns whether every observation matched.
    pub fn is_match(&self) -> bool {
        self.mismatches.is_empty()
    }
}

/// Replays validated traces in their recorded completion order.
#[derive(Debug, Default)]
pub struct ReplayEngine;

impl ReplayEngine {
    /// Executes every request and compares typed outcomes and commit sequences.
    pub async fn replay<A: WorkloadAdapter>(
        &self,
        trace: &ReplayTrace,
        adapter: &mut A,
    ) -> Result<ReplayReport> {
        trace.validate()?;
        let mut mismatches = Vec::new();
        for request in trace.requests() {
            let actual = adapter.execute(&request.start).await;
            let expected = AdapterObservation {
                commit_sequence: request.end.commit_sequence,
                outcome: request.end.outcome.clone(),
            };
            if actual != expected {
                mismatches.push(ReplayMismatch {
                    request: request.start.request_id,
                    expected,
                    actual,
                });
            }
        }
        Ok(ReplayReport {
            requests_executed: trace.requests().len(),
            mismatches,
        })
    }
}

#[cfg(test)]
mod tests {
    use crate::action::{Action, ElementKind, ReadAction};
    use crate::ids::{EntityId, RuntimeId, StableSeed, TenantId};
    use crate::trace::{ObservedValue, TraceRecorder};

    use super::*;

    struct Adapter {
        observation: AdapterObservation,
    }

    #[async_trait]
    impl WorkloadAdapter for Adapter {
        async fn execute(&mut self, _request: &RequestStart) -> AdapterObservation {
            self.observation.clone()
        }
    }

    fn trace() -> ReplayTrace {
        let mut recorder = TraceRecorder::new(StableSeed::new(1));
        let pending = recorder
            .start_request(
                RequestId::new(1).unwrap(),
                RuntimeId::new(1).unwrap(),
                TenantId::try_new("tenant").unwrap(),
                Sequence::initial(),
                Action::Read(ReadAction::Point {
                    kind: ElementKind::Node,
                    id: EntityId::new(1),
                }),
            )
            .unwrap();
        recorder.finish_request(
            pending,
            None,
            TraceOutcome::Success(ObservedValue::Entities(Vec::new())),
        );
        recorder.finish().unwrap()
    }

    #[tokio::test]
    async fn replay_reports_exact_matches_and_mismatches() {
        let trace = trace();
        let mut matching = Adapter {
            observation: AdapterObservation {
                commit_sequence: None,
                outcome: TraceOutcome::Success(ObservedValue::Entities(Vec::new())),
            },
        };
        let report = ReplayEngine.replay(&trace, &mut matching).await.unwrap();
        assert!(report.is_match());
        assert_eq!(report.requests_executed(), 1);

        let mut mismatching = Adapter {
            observation: AdapterObservation {
                commit_sequence: None,
                outcome: TraceOutcome::Success(ObservedValue::Acknowledged),
            },
        };
        let report = ReplayEngine.replay(&trace, &mut mismatching).await.unwrap();
        assert_eq!(report.mismatches().len(), 1);
    }
}
