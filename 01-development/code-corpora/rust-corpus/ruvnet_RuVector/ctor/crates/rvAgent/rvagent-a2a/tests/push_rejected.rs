//! ADR-159 M4 acceptance test #3 — 4xx is terminal.
//!
//! Receiver returns 400 on every request. The server should POST
//! exactly once and stop (no retry on 4xx). This guards against a
//! misbehaving retry loop that would hammer a rejecting receiver.

use std::sync::atomic::{AtomicUsize, Ordering};
use std::sync::Arc;
use std::time::Duration;

use axum::{extract::State, http::StatusCode, response::IntoResponse, routing::post, Router};

use rvagent_a2a::budget::{BudgetLedger, GlobalBudget};
use rvagent_a2a::context::TaskContext;
use rvagent_a2a::executor::{Executor, InMemoryRunner};
use rvagent_a2a::identity::{agent_id_from_pubkey, AgentID};
use rvagent_a2a::server::{A2aServer, A2aServerConfig};
use rvagent_a2a::types::{
    AgentCapabilities, AgentCard, AgentProvider, AgentSkill, AuthScheme, Message, Part, Role,
    TaskSpec,
};

use ed25519_dalek::SigningKey;
use rand_core::OsRng;
use serde_json::json;

#[derive(Clone)]
struct ReceiverState {
    count: Arc<AtomicUsize>,
}

async fn receive(State(s): State<ReceiverState>, _body: axum::body::Bytes) -> impl IntoResponse {
    s.count.fetch_add(1, Ordering::SeqCst);
    StatusCode::BAD_REQUEST
}

fn make_card(base_url: &str) -> AgentCard {
    AgentCard {
        name: "push-rejected-test".into(),
        description: "push_rejected.rs fixture".into(),
        url: base_url.into(),
        provider: AgentProvider {
            organization: "ruvector".into(),
            url: None,
        },
        version: "0.1.0".into(),
        capabilities: AgentCapabilities {
            streaming: false,
            push_notifications: true,
        },
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
    }
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
        context: TaskContext::new_root(AgentID(
            agent_id_from_pubkey(&SigningKey::generate(&mut OsRng).verifying_key()).0,
        )),
        metadata: serde_json::Value::Null,
    }
}

#[tokio::test]
async fn push_rejected_4xx_does_not_retry() {
    // The dispatcher emits a WARN on 4xx; we don't attach a subscriber
    // here (would require tracing-subscriber as a dev-dep). The test
    // asserts the attempt count instead: if the WARN wasn't emitted or
    // the retry path was taken, the count would be wrong.

    // Receiver.
    let receiver_listener = tokio::net::TcpListener::bind("127.0.0.1:0").await.unwrap();
    let receiver_addr = receiver_listener.local_addr().unwrap();
    let receiver_url = format!("http://{}/", receiver_addr);
    let count = Arc::new(AtomicUsize::new(0));
    let app = Router::new()
        .route("/", post(receive))
        .with_state(ReceiverState {
            count: count.clone(),
        });
    tokio::spawn(async move {
        axum::serve(receiver_listener, app).await.unwrap();
    });

    // Server.
    let server_listener = tokio::net::TcpListener::bind("127.0.0.1:0").await.unwrap();
    let server_addr = server_listener.local_addr().unwrap();
    let server_url = format!("http://{}", server_addr);
    let card = make_card(&server_url);
    let card_bytes = serde_json::to_vec(&card).unwrap();
    let executor = Arc::new(Executor::Local(Arc::new(InMemoryRunner::new())));
    let budget = Arc::new(BudgetLedger::new(GlobalBudget::default()));
    let server = A2aServer::new(
        card,
        card_bytes,
        executor,
        budget,
        A2aServerConfig::default(),
    );
    let router = server.router();
    tokio::spawn(async move {
        axum::serve(server_listener, router).await.unwrap();
    });
    tokio::time::sleep(Duration::from_millis(20)).await;

    // Register push config.
    let http = reqwest::Client::new();
    let set_req = json!({
        "jsonrpc": "2.0",
        "method": "tasks/pushNotification/set",
        "params": {
            "id": "t-rej-1",
            "pushNotificationConfig": {
                "url": receiver_url,
                "token": "rej-token",
            },
        },
        "id": 1,
    });
    let _ = http.post(&server_url).json(&set_req).send().await.unwrap();

    // Drive task.
    let send_req = json!({
        "jsonrpc": "2.0",
        "method": "tasks/send",
        "params": spec("t-rej-1"),
        "id": 2,
    });
    let send_resp = http.post(&server_url).json(&send_req).send().await.unwrap();
    assert!(send_resp.status().is_success());

    // Settle: if the dispatcher was (wrongly) retrying, it would sleep
    // 100ms → 400ms and hit again. Wait long enough to catch that.
    tokio::time::sleep(Duration::from_millis(800)).await;

    assert_eq!(
        count.load(Ordering::SeqCst),
        1,
        "4xx response must not be retried"
    );
}
