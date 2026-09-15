//! ADR-159 M4 acceptance test — Ed25519 webhook signing.
//!
//! Gated on `--features ed25519-webhooks`. Identical shape to
//! `push_signing.rs`, but additionally asserts:
//!   1. The server's AgentCard advertises
//!      `metadata.ruvector.webhook_algos = ["hmac-sha256", "ed25519"]`
//!      and `metadata.ruvector.webhook_ed25519_pubkey`.
//!   2. Every outbound webhook carries both `X-A2A-Signature` (HMAC)
//!      and `X-A2A-Signature-Ed25519` (base64 ed25519).
//!   3. The Ed25519 signature verifies against the advertised pubkey
//!      via `verify_push_signature_ed25519`.

#![cfg(feature = "ed25519-webhooks")]

use std::sync::Arc;
use std::time::Duration;

use axum::{extract::State, http::HeaderMap, response::IntoResponse, routing::post, Router};
use tokio::sync::mpsc;

use rvagent_a2a::budget::{BudgetLedger, GlobalBudget};
use rvagent_a2a::context::TaskContext;
use rvagent_a2a::executor::{Executor, InMemoryRunner};
use rvagent_a2a::identity::{agent_id_from_pubkey, AgentID};
use rvagent_a2a::server::push::{verify_push_signature, verify_push_signature_ed25519};
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

fn build_receiver() -> (Router, mpsc::UnboundedReceiver<Received>) {
    let (tx, rx) = mpsc::unbounded_channel();
    let app = Router::new()
        .route("/", post(receive))
        .with_state(ReceiverState { tx });
    (app, rx)
}

fn make_card(base_url: &str) -> AgentCard {
    AgentCard {
        name: "push-ed25519-test".into(),
        description: "push_ed25519.rs fixture".into(),
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
async fn push_ed25519_emits_both_signatures_and_verifies() {
    // 1. Receiver app.
    let receiver_listener = tokio::net::TcpListener::bind("127.0.0.1:0").await.unwrap();
    let receiver_addr = receiver_listener.local_addr().unwrap();
    let receiver_url = format!("http://{}/", receiver_addr);
    let (receiver_app, mut receiver_rx) = build_receiver();
    tokio::spawn(async move {
        axum::serve(receiver_listener, receiver_app).await.unwrap();
    });

    // 2. A2aServer with the `ed25519-webhooks` feature live. `new()`
    //    mints a keypair and rewrites the AgentCard metadata.
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

    // 3. Fetch the advertised card; assert pubkey + algo list are set.
    let http = reqwest::Client::new();
    let served_card: serde_json::Value = http
        .get(format!("{}/.well-known/agent.json", server_url))
        .send()
        .await
        .unwrap()
        .json()
        .await
        .unwrap();
    let algos = &served_card["metadata"]["ruvector"]["webhook_algos"];
    assert!(
        algos.is_array(),
        "webhook_algos missing: card = {}",
        served_card
    );
    let algo_list: Vec<String> = algos
        .as_array()
        .unwrap()
        .iter()
        .map(|v| v.as_str().unwrap().to_string())
        .collect();
    assert!(algo_list.contains(&"hmac-sha256".to_string()));
    assert!(algo_list.contains(&"ed25519".to_string()));
    let pubkey_b64 = served_card["metadata"]["ruvector"]["webhook_ed25519_pubkey"]
        .as_str()
        .expect("webhook_ed25519_pubkey must be a base64 string")
        .to_string();

    // 4. Register push config.
    let token = "push-token-ed25519";
    let set_req = json!({
        "jsonrpc": "2.0",
        "method": "tasks/pushNotification/set",
        "params": {
            "id": "t-ed-1",
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

    // 5. Drive the task to Completed.
    let send_req = json!({
        "jsonrpc": "2.0",
        "method": "tasks/send",
        "params": spec("t-ed-1"),
        "id": 2,
    });
    let send_resp = http.post(&server_url).json(&send_req).send().await.unwrap();
    assert!(send_resp.status().is_success());

    // 6. Wait for the delivered webhook.
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

    // 7. Both signature headers must be present.
    let hmac_sig = received
        .headers
        .get("x-a2a-signature")
        .expect("X-A2A-Signature (HMAC) missing")
        .to_str()
        .unwrap();
    assert!(hmac_sig.starts_with("sha256="));
    verify_push_signature(&received.body, token, hmac_sig).expect("HMAC verify");

    let ed_sig = received
        .headers
        .get("x-a2a-signature-ed25519")
        .expect("X-A2A-Signature-Ed25519 missing")
        .to_str()
        .unwrap();

    // 8. Ed25519 signature verifies against the advertised pubkey.
    verify_push_signature_ed25519(&received.body, &pubkey_b64, ed_sig)
        .expect("Ed25519 verify against advertised pubkey");

    // 9. Negative control: tampered body must not verify.
    let mut tampered = received.body.clone();
    tampered[0] ^= 0xff;
    assert!(
        verify_push_signature_ed25519(&tampered, &pubkey_b64, ed_sig).is_err(),
        "tampered body must fail Ed25519 verification"
    );
}
