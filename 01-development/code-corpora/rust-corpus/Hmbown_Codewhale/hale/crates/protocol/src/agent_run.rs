//! Dependency-neutral agent-run read model.
//!
//! [`AgentRunSnapshot`] is a uniform, serializable projection of "one unit of
//! agent work" regardless of which subsystem owns it: a direct sub-agent
//! worker, a workflow run, a fleet run, a core background job, or a managed
//! task. It carries serialized IDs, neutral enums, and scalar summaries only —
//! never handles, callbacks, or owner-internal types — so any surface can
//! consume it without depending on the owning subsystem.
//!
//! Ownership contract:
//! - Each owner keeps its own richer record type and maps it here through a
//!   pure adapter function living in the owner's crate/module. This crate
//!   depends on nothing new; owners depend on it, never the reverse.
//! - Adapters must apply the durable-outranks-live rule: when both a durable
//!   record and a live in-memory handle exist, a terminal durable state always
//!   wins over lagging live enrichment. Live state may only refine a run that
//!   the durable record still considers in-flight.
//! - Adapters never fabricate values: unknown budgets and timestamps stay
//!   `None`, and references are logical identifiers within the owner's
//!   namespace — never filesystem paths and never secrets.
//!
//! This is a staging slice: the model compiles and is tested, but nothing
//! consumes it yet. Consumers arrive with later slices.

use serde::{Deserialize, Serialize};

use super::Status;

/// Which subsystem owns the run behind a snapshot.
#[derive(Debug, Clone, Copy, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "snake_case")]
pub enum RunSource {
    /// A directly spawned sub-agent worker.
    Direct,
    /// A workflow VM run.
    Workflow,
    /// A fleet run reconstructed from the durable ledger.
    Fleet,
    /// A core background job.
    CoreJob,
    /// A managed background task.
    Task,
}

/// Neutral lifecycle state of a run.
///
/// The two wait variants beyond model/tool waits exist because real owner
/// state machines report them: workers can wait on user input, and jobs and
/// fleet runs can be explicitly paused. Collapsing those into `Running` or
/// `Queued` would misreport liveness, so the read model keeps them distinct.
#[derive(Debug, Clone, Copy, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "snake_case")]
pub enum RunState {
    /// Accepted but not started.
    Queued,
    /// Spawn intent recorded; startup in progress.
    Initializing,
    /// Actively executing.
    Running,
    /// Blocked on a model response.
    WaitingModel,
    /// Blocked on a tool execution.
    WaitingTool,
    /// Blocked on user input.
    WaitingInput,
    /// Explicitly paused by the user or system.
    Paused,
    /// Cancellation or shutdown requested, not yet terminal.
    Stopping,
    /// Finished; see [`AgentRunSnapshot::terminal`] for the outcome.
    Terminal,
}

impl Status for RunState {
    fn is_terminal(&self) -> bool {
        matches!(self, Self::Terminal)
    }
    fn is_active(&self) -> bool {
        matches!(
            self,
            Self::Queued
                | Self::Initializing
                | Self::Running
                | Self::WaitingModel
                | Self::WaitingTool
                | Self::WaitingInput
                | Self::Stopping
        )
    }
    fn is_paused(&self) -> bool {
        matches!(self, Self::Paused)
    }
}

/// Scalar budget/spend facts for a run.
///
/// Every field is optional: owners report only what they actually track, and
/// adapters must not invent values for the rest.
#[derive(Debug, Clone, Copy, Default, Serialize, Deserialize, PartialEq, Eq)]
pub struct BudgetSummary {
    /// Token ceiling configured for the run, if any.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub token_budget: Option<u64>,
    /// Tokens consumed so far, when the owner tracks spend.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub tokens_used: Option<u64>,
    /// Steps taken so far, when the owner counts steps.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub steps_taken: Option<u32>,
    /// Step ceiling configured for the run, if any.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub max_steps: Option<u32>,
    /// Wall-clock duration in milliseconds, when known.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub duration_ms: Option<u64>,
}

/// How a terminal run ended.
#[derive(Debug, Clone, Copy, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "snake_case")]
pub enum TerminalOutcome {
    /// Ended successfully.
    Completed,
    /// Ended with an error.
    Failed,
    /// Cancelled by the user or a parent.
    Cancelled,
    /// Interrupted (e.g. process restart) before completion.
    Interrupted,
    /// Stopped because it exhausted its own budget.
    BudgetExhausted,
}

/// Scalar summary of a terminal run.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct TerminalSummary {
    pub outcome: TerminalOutcome,
    /// Epoch milliseconds when the run ended, when the owner recorded it.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub ended_at_ms: Option<i64>,
    /// Short human-readable detail (result summary or error text).
    /// Never raw logs, reasoning text, or secrets.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub detail: Option<String>,
}

/// Category of a durable reference attached to a run.
#[derive(Debug, Clone, Copy, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "snake_case")]
pub enum ReceiptKind {
    /// A produced artifact.
    Artifact,
    /// A gate/verification result.
    Gate,
    /// A durable completion receipt.
    Receipt,
    /// An approval record.
    Approval,
}

/// Reference to a durable artifact, gate, receipt, or approval.
///
/// `reference` is a logical identifier within the owner's namespace — never
/// an absolute filesystem path, never secret-bearing.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct ReceiptRef {
    pub kind: ReceiptKind,
    pub reference: String,
    /// Optional short human-readable label.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub label: Option<String>,
}

/// Dependency-neutral snapshot of a single agent run.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub struct AgentRunSnapshot {
    /// Serialized run identifier in the owner's existing scheme.
    pub run_id: String,
    /// Serialized identifier of the parent run/thread, when the owner
    /// records one.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub parent: Option<String>,
    /// Which subsystem owns this run.
    pub source: RunSource,
    /// Neutral lifecycle state.
    pub state: RunState,
    /// Scalar budget facts; defaults to all-unknown.
    #[serde(default)]
    pub budget: BudgetSummary,
    /// Terminal outcome; present exactly when `state == Terminal`.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub terminal: Option<TerminalSummary>,
    /// Durable references produced by the run.
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub refs: Vec<ReceiptRef>,
}

impl AgentRunSnapshot {
    /// `true` when the terminal summary and lifecycle state agree:
    /// `state == Terminal` iff a terminal summary is present.
    ///
    /// Adapters uphold this by construction; tests assert it.
    #[must_use]
    pub fn is_coherent(&self) -> bool {
        matches!(self.state, RunState::Terminal) == self.terminal.is_some()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn full_snapshot() -> AgentRunSnapshot {
        AgentRunSnapshot {
            run_id: "agent_1234abcd".to_string(),
            parent: Some("agent_00ff00ff".to_string()),
            source: RunSource::Direct,
            state: RunState::Terminal,
            budget: BudgetSummary {
                token_budget: Some(50_000),
                tokens_used: Some(12_345),
                steps_taken: Some(7),
                max_steps: Some(40),
                duration_ms: Some(93_000),
            },
            terminal: Some(TerminalSummary {
                outcome: TerminalOutcome::Completed,
                ended_at_ms: Some(1_800_000_000_000),
                detail: Some("verified build".to_string()),
            }),
            refs: vec![ReceiptRef {
                kind: ReceiptKind::Artifact,
                reference: "runs/agent_1234abcd/result.md".to_string(),
                label: Some("result".to_string()),
            }],
        }
    }

    fn minimal_snapshot() -> AgentRunSnapshot {
        AgentRunSnapshot {
            run_id: "job-42".to_string(),
            parent: None,
            source: RunSource::CoreJob,
            state: RunState::Queued,
            budget: BudgetSummary::default(),
            terminal: None,
            refs: Vec::new(),
        }
    }

    #[test]
    fn full_snapshot_round_trips() {
        let snapshot = full_snapshot();
        let json = serde_json::to_string(&snapshot).expect("serialize");
        let back: AgentRunSnapshot = serde_json::from_str(&json).expect("deserialize");
        assert_eq!(back, snapshot);
        assert!(back.is_coherent());
    }

    #[test]
    fn minimal_snapshot_round_trips_and_skips_empty_fields() {
        let snapshot = minimal_snapshot();
        let json = serde_json::to_string(&snapshot).expect("serialize");
        // Optional/empty fields stay off the wire entirely.
        assert!(!json.contains("parent"));
        assert!(!json.contains("terminal"));
        assert!(!json.contains("refs"));
        assert!(!json.contains("token_budget"));
        let back: AgentRunSnapshot = serde_json::from_str(&json).expect("deserialize");
        assert_eq!(back, snapshot);
        assert!(back.is_coherent());
    }

    #[test]
    fn enum_wire_names_are_snake_case_and_stable() {
        assert_eq!(
            serde_json::to_string(&RunSource::CoreJob).unwrap(),
            "\"core_job\""
        );
        assert_eq!(
            serde_json::to_string(&RunState::WaitingModel).unwrap(),
            "\"waiting_model\""
        );
        assert_eq!(
            serde_json::to_string(&TerminalOutcome::BudgetExhausted).unwrap(),
            "\"budget_exhausted\""
        );
        assert_eq!(
            serde_json::to_string(&ReceiptKind::Gate).unwrap(),
            "\"gate\""
        );
        // Every state deserializes back to itself.
        for state in [
            RunState::Queued,
            RunState::Initializing,
            RunState::Running,
            RunState::WaitingModel,
            RunState::WaitingTool,
            RunState::WaitingInput,
            RunState::Paused,
            RunState::Stopping,
            RunState::Terminal,
        ] {
            let json = serde_json::to_string(&state).unwrap();
            let back: RunState = serde_json::from_str(&json).unwrap();
            assert_eq!(back, state);
        }
    }

    #[test]
    fn run_state_status_trait_partitions_all_states() {
        for state in [
            RunState::Queued,
            RunState::Initializing,
            RunState::Running,
            RunState::WaitingModel,
            RunState::WaitingTool,
            RunState::WaitingInput,
            RunState::Paused,
            RunState::Stopping,
            RunState::Terminal,
        ] {
            let classifications = [state.is_terminal(), state.is_active(), state.is_paused()];
            assert_eq!(
                classifications.iter().filter(|flag| **flag).count(),
                1,
                "state {state:?} must be exactly one of terminal/active/paused"
            );
        }
    }

    #[test]
    fn incoherent_snapshot_is_detectable() {
        let mut snapshot = minimal_snapshot();
        snapshot.terminal = Some(TerminalSummary {
            outcome: TerminalOutcome::Completed,
            ended_at_ms: None,
            detail: None,
        });
        assert!(!snapshot.is_coherent());
    }
}
