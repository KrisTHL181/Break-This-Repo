//! ADR-159 M1 — `task_lifecycle.rs`.
//!
//! An `InMemoryRunner` driven through `Executor::Local` MUST take a task
//! from `Submitted → Working → Completed` with at least one artifact. We
//! construct the `TaskSpec` directly rather than going through the JSON-RPC
//! layer; this isolates the runner + state-machine contract from the
//! server plumbing (which has its own tests).

use std::sync::Arc;

use rvagent_a2a::context::TaskContext;
use rvagent_a2a::executor::{Executor, InMemoryRunner};
use rvagent_a2a::identity::{agent_id_from_pubkey, AgentID};
use rvagent_a2a::types::{Message, Part, Role, TaskSpec, TaskState};

use ed25519_dalek::SigningKey;
use rand_core::OsRng;

fn agent() -> AgentID {
    agent_id_from_pubkey(&SigningKey::generate(&mut OsRng).verifying_key())
}

fn spec(id: &str) -> TaskSpec {
    TaskSpec {
        id: id.into(),
        skill: "rag.query".into(),
        message: Message {
            role: Role::User,
            parts: vec![Part::Text {
                text: "say hi".into(),
            }],
            metadata: serde_json::Value::Null,
        },
        policy: None,
        context: TaskContext::new_root(agent()),
        metadata: serde_json::Value::Null,
    }
}

#[tokio::test]
async fn in_memory_runner_completes_task_with_history() {
    let executor = Executor::Local(Arc::new(InMemoryRunner::new()));
    let task = executor.run(spec("task-1")).await.expect("run ok");

    // Terminal state must be Completed.
    assert_eq!(
        task.status.state,
        TaskState::Completed,
        "runner did not reach Completed"
    );

    // History must include at least submitted→working→completed (two or more
    // messages; ADR-159 leaves the exact shape to the runner, but the state
    // machine is never fewer than two transitions).
    assert!(
        task.history.len() >= 2,
        "expected ≥2 history entries, got {}",
        task.history.len()
    );

    // At least one artifact produced.
    assert!(
        !task.artifacts.is_empty(),
        "expected ≥1 artifact, got {}",
        task.artifacts.len()
    );
}
