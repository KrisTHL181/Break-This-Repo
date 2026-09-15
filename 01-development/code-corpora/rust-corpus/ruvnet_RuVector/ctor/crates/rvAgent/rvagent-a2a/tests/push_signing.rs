//! ADR-159 M4 acceptance test #1 — push signing.
//!
//! Flow:
//!   1. Spin a real HTTP receiver on an ephemeral port; every incoming
//!      POST is recorded into a channel.
//!   2. Start an `A2aServer` on another ephemeral port.
//!   3. Register a webhook pointing at the receiver via
//!      `tasks/pushNotification/set`.
//!   4. Drive a task via `tasks/send`.
//!   5. Assert the receiver got at least one POST whose body matches
//!      the `X-A2A-Signature: sha256=<hex>` header under the agreed
//!      token, and whose JSON payload sets `finalState: true`.

use std::sync::Arc;
use std::time::Duration;

use axum::{extract::State, http::HeaderMap, response::IntoResponse, routing::post, Router};
use tokio::sync::mpsc;

use rvagent_a2a::budget::{BudgetLedger, GlobalBudget};
use rvagent_a2a::context::TaskContext;
use rvagent_a2a::executor::{Executor, InMemoryRunner};
use rvagent_a2a::identity::{agent_id_from_pubkey, AgentID};
use rvagent_a2a::server::push::verify_push_signature;
use rvagent_a2a::server::{A2aServer, A2aServerConfig};
use rvagent_a2a::types::{
    AgentCapabilities, AgentCard, AgentProvider, AgentSkill, AuthScheme, Message, Part, Role,
    TaskSpec,
};

use ed25519_dalek::SigningKey;
use rand_core::OsRng;
use serde_json::json;

#[derive(Clone)]
struct Received {
    headers: HeaderMap,
    body: Vec<u8>,
}

#[derive(Clone)]
struct ReceiverState {
    tx: mpsc::UnboundedSender<Received>,
}

async fn receive(
    State(s): State<ReceiverState>,
    headers: HeaderMap,
    body: axum::body::Bytes,
) -> impl IntoResponse {
    let _ = s.tx.send(Received {
        headers: headers.clone(),
        body: body.to_vec(),
    });
    axum::http::StatusCode::OK
}

/// Build a minimal receiver app that forwards every POST into the tx.
fn build_receiver() -> (Router, mpsc::UnboundedReceiver<Received>) {
    let (tx, rx) = mpsc::unbounded_channel();
    let app = Router::new()
        .route("/", post(receive))
        .with_state(ReceiverState { tx });
    (app, rx)
}

fn make_card(base_url: &str) -> AgentCard {
    AgentCard {
        name: "push-signing-test".into(),
        description: "push_signing.rs fixture".into(),
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
async fn push_signing_delivers_signed_webhook() {
    // 1. Receiver.
    let receiver_listener = tokio::net::TcpListener::bind("127.0.0.1:0").await.unwrap();
    let receiver_addr = receiver_listener.local_addr().unwrap();
    let receiver_url = format!("http://{}/", receiver_addr);
    let (receiver_app, mut receiver_rx) = build_receiver();
    tokio::spawn(async move {
        axum::serve(receiver_listener, receiver_app).await.unwrap();
    });

    // 2. Server.
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

    // 3. Register push config.
    let token = "push-token-signing";
    let http = reqwest::Client::new();
    let set_req = json!({
        "jsonrpc": "2.0",
        "method": "tasks/pushNotification/set",
        "params": {
            "id": "t-sign-1",
            "pushNotificationConfig": {
                "url": receiver_url,
                "token": token,
            },
        },
        "id": 1,
    });
    let set_resp: serde_json::Value = http
        .post(&server_url)
        .json(&set_req)
        .send()
        .await
        .unwrap()
        .json()
        .await
        .unwrap();
    assert!(
        set_resp.get("error").map(|e| e.is_null()).unwrap_or(true),
        "set responded with error: {:?}",
        set_resp
    );

    // 4. Drive a task via tasks/send. The InMemoryRunner transitions
    //    straight to Completed, which should trigger one terminal POST.
    let send_req = json!({
        "jsonrpc": "2.0",
        "method": "tasks/send",
        "params": spec("t-sign-1"),
        "id": 2,
    });
    let send_resp = http.post(&server_url).json(&send_req).send().await.unwrap();
    assert!(send_resp.status().is_success());

    // 5. Wait briefly for the webhook to land.
    let mut received: Option<Received> = None;
    for _ in 0..30 {
        match tokio::time::timeout(Duration::from_millis(100), receiver_rx.recv()).await {
            Ok(Some(r)) => {
                received = Some(r);
                break;
            }
            _ => continue,
        }
    }
    let received = received.expect("receiver saw no webhook POST");

    // 6. Body is JSON and carries finalState: true.
    let body_v: serde_json::Value =
        serde_json::from_slice(&received.body).expect("webhook body is JSON");
    assert_eq!(body_v["taskId"], "t-sign-1");
    assert_eq!(body_v["finalState"], true, "payload: {}", body_v);

    // 7. Signature header present + verifies.
    let sig = received
        .headers
        .get("x-a2a-signature")
        .expect("X-A2A-Signature header missing")
        .to_str()
        .unwrap();
    assert!(sig.starts_with("sha256="));
    verify_push_signature(&received.body, token, sig).expect("HMAC verify");
}
