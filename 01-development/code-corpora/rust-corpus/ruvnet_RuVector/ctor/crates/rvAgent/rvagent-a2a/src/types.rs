//! A2A spec core types.
//!
//! These are the JSON-on-the-wire shapes the A2A protocol defines. They live
//! at `rvagent-a2a::types` and are re-exported from the crate root. Field
//! names and enum variants match the Google A2A spec byte-for-byte under
//! `#[serde(rename_all = "camelCase")]` / the documented `snake_case`
//! discriminants on `Part` / `Role`.
//!
//! ruvector extensions ride in the free-form `metadata` field on every
//! top-level object, namespaced under `metadata.ruvector.*` — see ADR-159
//! §Core type sketches and §r2 / r3 sections.
//!
//! `TaskSpec` is NOT a spec type — it's the unified dispatch input our
//! `Executor` (local or remote) consumes. It lives here because it composes
//! spec types and must be in scope wherever they are.

use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};

// ---------------------------------------------------------------------------
// AgentCard and friends (served at `/.well-known/agent.json`).
// ---------------------------------------------------------------------------

/// Top-level discovery document. Served unauthenticated at
/// `GET /.well-known/agent.json`.
///
/// The `metadata` field is spec-blessed free-form JSON; ruvector-specific
/// extensions (signed identity, artifact version list, memory advertising)
/// live under `metadata.ruvector.*`. See `identity.rs` for the signature
/// layer — this struct deliberately carries the raw shape and leaves
/// signing to that module.
#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct AgentCard {
    pub name: String,
    pub description: String,
    /// Canonical endpoint URL. Must be HTTPS in production.
    pub url: String,
    pub provider: AgentProvider,
    pub version: String,
    pub capabilities: AgentCapabilities,
    pub skills: Vec<AgentSkill>,
    pub authentication: AuthScheme,
    /// Free-form JSON. Ruvector-specific extensions live under
    /// `metadata.ruvector.*` (identity signatures, artifact versions,
    /// ruLake advertising, etc.) per ADR-159.
    #[serde(default)]
    pub metadata: serde_json::Value,
}

/// Human-oriented provenance — who deployed the agent, how to reach them.
#[derive(Clone, Debug, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub struct AgentProvider {
    pub organization: String,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub url: Option<String>,
}

/// Optional-behavior flags. Defaults are intentionally conservative:
/// absent = not supported.
#[derive(Clone, Debug, Serialize, Deserialize, Default, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub struct AgentCapabilities {
    #[serde(default)]
    pub streaming: bool,
    #[serde(default)]
    pub push_notifications: bool,
}

/// A single named capability the agent exposes. Callers filter on `id` +
/// `tags` during `PeerSelector` evaluation.
#[derive(Clone, Debug, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub struct AgentSkill {
    pub id: String,
    pub name: String,
    pub description: String,
    #[serde(default)]
    pub tags: Vec<String>,
    /// MIME-like strings e.g. "text/plain", "application/json".
    #[serde(default)]
    pub input_modes: Vec<String>,
    #[serde(default)]
    pub output_modes: Vec<String>,
}

/// Auth method advertised by the agent — "bearer", "oauth2", "apikey".
/// Kept deliberately simple; downstream enforcement lives in
/// `rvagent-middleware`.
#[derive(Clone, Debug, Serialize, Deserialize, Default, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub struct AuthScheme {
    #[serde(default)]
    pub schemes: Vec<String>,
}

// ---------------------------------------------------------------------------
// Task lifecycle.
// ---------------------------------------------------------------------------

/// A single logical unit of work.
///
/// The spec describes `messages` (conversation-so-far) as a field; we use
/// `history` as the Rust-side name and alias it at the wire boundary. This
/// matches the more recent spec revisions — see ADR-159 §Core type
/// sketches.
#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct Task {
    pub id: String,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub session_id: Option<String>,
    pub status: TaskStatus,
    #[serde(default)]
    pub history: Vec<Message>,
    #[serde(default)]
    pub artifacts: Vec<Artifact>,
    #[serde(default)]
    pub metadata: serde_json::Value,
}

/// Current state + timestamp + optional human-readable note.
#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct TaskStatus {
    pub state: TaskState,
    pub timestamp: DateTime<Utc>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub message: Option<Message>,
}

/// A2A task state machine. Wire form is `kebab-case` per spec
/// (e.g. `input-required`).
#[derive(Clone, Copy, Debug, Serialize, Deserialize, PartialEq, Eq, Hash)]
#[serde(rename_all = "kebab-case")]
pub enum TaskState {
    Submitted,
    Working,
    InputRequired,
    Completed,
    Canceled,
    Failed,
}

impl TaskState {
    /// True once no further transitions are possible.
    pub fn is_terminal(&self) -> bool {
        matches!(
            self,
            TaskState::Completed | TaskState::Canceled | TaskState::Failed
        )
    }
}

// ---------------------------------------------------------------------------
// Messages, parts, artifacts.
// ---------------------------------------------------------------------------

/// A single message in the conversation. `role` identifies the sender;
/// `parts` is a heterogeneous bag of content blocks.
#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct Message {
    pub role: Role,
    pub parts: Vec<Part>,
    #[serde(default)]
    pub metadata: serde_json::Value,
}

/// Spec roles. Only `user` and `agent` are defined — tool-use goes through
/// a `Part::Data` convention rather than a new role.
#[derive(Clone, Copy, Debug, Serialize, Deserialize, PartialEq, Eq, Hash)]
#[serde(rename_all = "snake_case")]
pub enum Role {
    User,
    Agent,
}

/// Tagged union for content. Wire form is `{ "type": "text", "text": "…" }`
/// etc. per spec.
#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
#[serde(tag = "type", rename_all = "snake_case")]
pub enum Part {
    Text { text: String },
    File { file: FileContent },
    Data { data: serde_json::Value },
}

/// File attachment. Exactly one of `bytes` or `uri` must be set; the spec
/// is silent on the both-set case, so we accept it and let downstream
/// consumers pick.
#[derive(Clone, Debug, Serialize, Deserialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub struct FileContent {
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub mime_type: Option<String>,
    /// Base64-encoded bytes when the file is inline.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub bytes: Option<String>,
    /// URI (http(s), gs://, s3://, file://...) when by-reference.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub uri: Option<String>,
}

/// Structured output. `index`/`append`/`last_chunk` form the streaming
/// protocol — a receiver with multiple chunks for the same `index` must
/// concatenate in order when `append=true`, and treat `last_chunk=true`
/// as the terminator.
#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct Artifact {
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    pub parts: Vec<Part>,
    #[serde(default)]
    pub index: u32,
    #[serde(default)]
    pub append: bool,
    #[serde(default)]
    pub last_chunk: bool,
    #[serde(default)]
    pub metadata: serde_json::Value,
}

// ---------------------------------------------------------------------------
// SSE events (tasks/sendSubscribe, tasks/resubscribe).
// ---------------------------------------------------------------------------

/// Emitted whenever a task transitions between states.
///
/// `final_` (wire name `final`) is `true` for the last event of a task's
/// lifecycle — receivers use it to close the stream cleanly.
#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct TaskStatusUpdateEvent {
    pub id: String,
    pub status: TaskStatus,
    /// `final` is a reserved word in Rust; wire name kept via serde.
    #[serde(rename = "final")]
    pub final_: bool,
    #[serde(default)]
    pub metadata: serde_json::Value,
}

/// Emitted when the runner produces or extends an artifact.
#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct TaskArtifactUpdateEvent {
    pub id: String,
    pub artifact: Artifact,
    #[serde(default)]
    pub metadata: serde_json::Value,
}

// ---------------------------------------------------------------------------
// Dispatch-input type (NOT spec; unified Local/Remote executor input).
// ---------------------------------------------------------------------------

/// Input to `Executor::dispatch`. Carries the minimal shape the executor
/// needs to run a task regardless of whether it ends up local or remote.
///
/// The `policy` field is opt-in; if `None`, the server-side defaults from
/// `config.rs` apply. `context` is always populated (freshly generated for
/// root tasks, inherited otherwise).
#[derive(Clone, Debug, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "camelCase")]
pub struct TaskSpec {
    pub id: String,
    /// `AgentSkill::id` of the requested skill. Routing + policy both key
    /// off this.
    pub skill: String,
    /// User-supplied input. Carries `Part`s — text, data, file refs.
    pub message: Message,
    /// Per-task policy (budget / concurrency / allowed_skills). Nullable
    /// so callers without policy plumbing can omit it.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub policy: Option<crate::policy::TaskPolicy>,
    /// Trace context (r3). Always present — generated fresh if no parent.
    pub context: crate::context::TaskContext,
    #[serde(default)]
    pub metadata: serde_json::Value,
}

// ---------------------------------------------------------------------------
// Tests.
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn task_state_serializes_kebab_case() {
        let s = serde_json::to_string(&TaskState::InputRequired).unwrap();
        assert_eq!(s, "\"input-required\"");
    }

    #[test]
    fn task_state_roundtrip() {
        for v in [
            TaskState::Submitted,
            TaskState::Working,
            TaskState::InputRequired,
            TaskState::Completed,
            TaskState::Canceled,
            TaskState::Failed,
        ] {
            let j = serde_json::to_string(&v).unwrap();
            let back: TaskState = serde_json::from_str(&j).unwrap();
            assert_eq!(v, back);
        }
    }

    #[test]
    fn task_state_terminal() {
        assert!(TaskState::Completed.is_terminal());
        assert!(TaskState::Canceled.is_terminal());
        assert!(TaskState::Failed.is_terminal());
        assert!(!TaskState::Working.is_terminal());
        assert!(!TaskState::Submitted.is_terminal());
    }

    #[test]
    fn role_serializes_snake_case() {
        assert_eq!(serde_json::to_string(&Role::User).unwrap(), "\"user\"");
        assert_eq!(serde_json::to_string(&Role::Agent).unwrap(), "\"agent\"");
    }

    #[test]
    fn part_tagged_on_type() {
        let p = Part::Text {
            text: "hello".into(),
        };
        let j = serde_json::to_value(&p).unwrap();
        assert_eq!(j["type"], "text");
        assert_eq!(j["text"], "hello");
    }

    #[test]
    fn part_data_variant() {
        let p = Part::Data {
            data: serde_json::json!({"k": 1}),
        };
        let j = serde_json::to_value(&p).unwrap();
        assert_eq!(j["type"], "data");
        assert_eq!(j["data"]["k"], 1);
    }

    #[test]
    fn agent_card_minimal_roundtrip() {
        let card = AgentCard {
            name: "test".into(),
            description: "test agent".into(),
            url: "https://example.com".into(),
            provider: AgentProvider {
                organization: "ruvector".into(),
                url: None,
            },
            version: "0.1.0".into(),
            capabilities: AgentCapabilities {
                streaming: true,
                push_notifications: false,
            },
            skills: vec![],
            authentication: AuthScheme {
                schemes: vec!["bearer".into()],
            },
            metadata: serde_json::json!({"ruvector": {"artifact_kind_version": "1"}}),
        };
        let j = serde_json::to_string(&card).unwrap();
        let back: AgentCard = serde_json::from_str(&j).unwrap();
        assert_eq!(card, back);
        // camelCase on the wire.
        assert!(j.contains("pushNotifications"));
    }

    #[test]
    fn task_status_update_event_final_field() {
        let ev = TaskStatusUpdateEvent {
            id: "t1".into(),
            status: TaskStatus {
                state: TaskState::Completed,
                timestamp: Utc::now(),
                message: None,
            },
            final_: true,
            metadata: serde_json::Value::Null,
        };
        let j = serde_json::to_value(&ev).unwrap();
        assert_eq!(j["final"], true);
        assert!(j.get("final_").is_none());
    }
}
