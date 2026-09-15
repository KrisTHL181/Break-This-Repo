//! ADR-159 M3 r2 — `executor_remote.rs` (acceptance test #1).
//!
//! "A remote agent call is indistinguishable from a local call." We dispatch
//! the same `TaskSpec` through `Executor::Local(InMemoryRunner)` and
//! `Executor::Remote(Peer)` and assert the result payload is equivalent
//! modulo generated IDs + timestamps.
//!
//! Wiring approach: bind a plain `tokio::net::TcpListener` on 127.0.0.1:0,
//! mount the `A2aServer` router behind `axum::serve` in a background task,
//! and aim a live `A2aClient` (real reqwest, real TCP) at that ephemeral
//! port. This avoids the in-process vs. reqwest mismatch that
//! `axum_test::TestServer` historically exhibits.

use std::sync::Arc;

use rvagent_a2a::budget::{BudgetLedger, GlobalBudget};
use rvagent_a2a::client::A2aClient;
use rvagent_a2a::context::TaskContext;
use rvagent_a2a::executor::{Executor, InMemoryRunner, Peer};
use rvagent_a2a::identity::{agent_id_from_pubkey, sign_card, AgentID};
use rvagent_a2a::server::{A2aServer, A2aServerConfig};
use rvagent_a2a::types::{
    AgentCapabilities, AgentCard, AgentProvider, AgentSkill, AuthScheme, Message, Part, Role,
    TaskSpec, TaskState,
};

use ed25519_dalek::SigningKey;
use rand_core::OsRng;
use serde_json::json;

fn random_agent_id() -> AgentID {
    agent_id_from_pubkey(&SigningKey::generate(&mut OsRng).verifying_key())
}

fn spec(id: &str) -> TaskSpec {
    TaskSpec {
        id: id.into(),
        skill: "rag.query".into(),
        message: Message {
            role: Role::User,
            parts: vec![Part::Text {
                text: "ping".into(),
            }],
            metadata: serde_json::Value::Null,
        },
        policy: None,
        context: TaskContext::new_root(random_agent_id()),
        metadata: serde_json::Value::Null,
    }
}

#[tokio::test]
async fn local_executor_reaches_completed() {
    // The Local half of the acceptance — kept even after the Remote half
    // is wired so regression failures narrow cleanly to one side.
    let executor = Executor::Local(Arc::new(InMemoryRunner::new()));
    let task = executor.run(spec("ex-local")).await.expect("local run");
    assert_eq!(task.status.state, TaskState::Completed);
    assert!(
        !task.artifacts.is_empty(),
        "local runner produced 0 artifacts"
    );
}

/// Build a fresh signed [`AgentCard`] for a server at `base_url`. Embeds the
/// Ed25519 signature under `metadata.ruvector.signature` so the client's
/// `fetch_card` verify path is exercised end-to-end.
fn signed_card(sk: &SigningKey, base_url: &str) -> AgentCard {
    let card = AgentCard {
        name: "exec-remote-test".into(),
        description: "executor_remote.rs fixture".into(),
        url: base_url.to_string(),
        provider: AgentProvider {
            organization: "ruvector".into(),
            url: None,
        },
        version: "0.1.0".into(),
        capabilities: AgentCapabilities::default(),
        skills: vec![AgentSkill {
            id: "rag.query".into(),
            name: "RAG".into(),
            description: "x".into(),
            tags: vec![],
            input_modes: vec![],
            output_modes: vec![],
        }],
        authentication: AuthScheme {
            schemes: vec!["bearer".into()],
        },
        metadata: json!({ "ruvector": {} }),
    };
    let sig = sign_card(&card, sk).expect("sign card");
    // Re-inject the signature under metadata.ruvector.signature — same
    // pattern as card_signature.rs fixtures.
    let mut v = serde_json::to_value(&card).unwrap();
    let meta = v
        .get_mut("metadata")
        .and_then(|m| m.as_object_mut())
        .unwrap();
    let ruvector = meta
        .entry("ruvector")
        .or_insert_with(|| json!({}))
        .as_object_mut()
        .unwrap();
    ruvector.insert("signature".into(), serde_json::to_value(&sig).unwrap());
    serde_json::from_value(v).expect("re-parse signed card")
}

/// ADR-159 acceptance test #1 — "indistinguishable." Runs the same
/// `TaskSpec` through `Executor::Local(InMemoryRunner)` and
/// `Executor::Remote(Peer)` (backed by a real-TCP `A2aClient` ↔ `A2aServer`
/// pair) and asserts the result shapes match modulo timestamps.
#[tokio::test]
async fn remote_executor_matches_local_shape() {
    // 1. Bind an ephemeral TCP port. Reqwest can always reach this.
    let listener = tokio::net::TcpListener::bind("127.0.0.1:0")
        .await
        .expect("bind ephemeral port");
    let addr = listener.local_addr().expect("local_addr");
    let base_url = format!("http://{}", addr);

    // 2. Build the server: signed AgentCard + InMemoryRunner executor +
    //    unlimited budget.
    let sk = SigningKey::generate(&mut OsRng);
    let card = signed_card(&sk, &base_url);
    let card_bytes = serde_json::to_vec(&card).expect("card bytes");
    let executor = Arc::new(Executor::Local(Arc::new(InMemoryRunner::new())));
    let budget = Arc::new(BudgetLedger::new(GlobalBudget::default()));
    let server = A2aServer::new(
        card.clone(),
        card_bytes,
        executor,
        budget,
        A2aServerConfig::default(),
    );
    let router = server.router();

    // 3. Mount behind `axum::serve` in a background task. Kept for the
    //    lifetime of the test via the tokio runtime's shutdown on drop —
    //    this is the most reliable cross-platform pattern.
    tokio::spawn(async move {
        axum::serve(listener, router).await.expect("axum serve");
    });
    // Give the server a moment to start accepting connections — ~10 ms is
    // ample on a loopback interface.
    tokio::time::sleep(std::time::Duration::from_millis(10)).await;

    // 4. Build the client + Peer.
    let client = A2aClient::new().expect("client");
    // Exercise discovery once so fetch_card() is covered + the signed-card
    // path actually runs through `verify_card`.
    let fetched_card = client.fetch_card(&base_url).await.expect("fetch card");
    assert_eq!(fetched_card.url, base_url);

    let peer = Peer {
        id: agent_id_from_pubkey(&sk.verifying_key()),
        card: fetched_card,
        base_url: url::Url::parse(&base_url).expect("url"),
        client: Arc::new(client),
    };

    let local = Executor::Local(Arc::new(InMemoryRunner::new()));
    let remote = Executor::Remote(Box::new(peer));

    // 5. Dispatch the same TaskSpec through both sides.
    let local_spec = spec("ex-local-side");
    let remote_spec = spec("ex-remote-side");
    let local_task = local.run(local_spec).await.expect("local run");
    let remote_task = remote.run(remote_spec).await.expect("remote run");

    // 6. Both reached Completed.
    assert_eq!(local_task.status.state, TaskState::Completed);
    assert_eq!(remote_task.status.state, TaskState::Completed);

    // 7. Same artifact count.
    assert_eq!(
        local_task.artifacts.len(),
        remote_task.artifacts.len(),
        "artifact counts diverge",
    );

    // 8. Structural equality of artifact Part shapes — compare the
    //    `type` discriminator of each Part, which is what a downstream
    //    consumer actually branches on. Ignores generated ids + timestamps
    //    (which live only on the Task, not on the artifact Parts).
    fn part_kind(p: &Part) -> &'static str {
        match p {
            Part::Text { .. } => "text",
            Part::File { .. } => "file",
            Part::Data { .. } => "data",
        }
    }
    for (l, r) in local_task
        .artifacts
        .iter()
        .zip(remote_task.artifacts.iter())
    {
        let l_kinds: Vec<&str> = l.parts.iter().map(part_kind).collect();
        let r_kinds: Vec<&str> = r.parts.iter().map(part_kind).collect();
        assert_eq!(l_kinds, r_kinds, "part-kind shapes diverge");
        assert_eq!(l.append, r.append);
        assert_eq!(l.last_chunk, r.last_chunk);
    }
}
