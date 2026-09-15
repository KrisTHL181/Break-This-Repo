//! ADR-159 M4 acceptance test #2 — retry on 5xx.
//!
//! Receiver returns 503 for the first 2 attempts, 200 for the 3rd.
//! We expect exactly 3 recorded attempts with delivered-once semantics
//! (one successful POST at the end).

use std::sync::atomic::{AtomicUsize, Ordering};
use std::sync::Arc;
use std::time::Duration;

use axum::{extract::State, http::StatusCode, response::IntoResponse, routing::post, Router};
use tokio::sync::mpsc;

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
    success_count: Arc<AtomicUsize>,
    tx: mpsc::UnboundedSender<u16>,
}

async fn receive(State(s): State<ReceiverState>, _body: axum::body::Bytes) -> impl IntoResponse {
    let n = s.count.fetch_add(1, Ordering::SeqCst) + 1;
    // First two attempts → 503; third (and any further) → 200.
    let code = if n <= 2 {
        StatusCode::SERVICE_UNAVAILABLE
    } else {
        s.success_count.fetch_add(1, Ordering::SeqCst);
        StatusCode::OK
    };
    let _ = s.tx.send(code.as_u16());
    code
}

fn build_receiver() -> (
    Router,
    Arc<AtomicUsize>,
    Arc<AtomicUsize>,
    mpsc::UnboundedReceiver<u16>,
) {
    let count = Arc::new(AtomicUsize::new(0));
    let success = Arc::new(AtomicUsize::new(0));
    let (tx, rx) = mpsc::unbounded_channel();
    let app = Router::new()
        .route("/", post(receive))
        .with_state(ReceiverState {
            count: count.clone(),
            success_count: success.clone(),
            tx,
        });
    (app, count, success, rx)
}

fn make_card(base_url: &str) -> AgentCard {
    AgentCard {
        name: "push-retry-test".into(),
        description: "push_retry.rs fixture".into(),
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
async fn push_retry_delivers_once_on_5xx_then_200() {
    // Receiver: 503, 503, 200.
    let receiver_listener = tokio::net::TcpListener::bind("127.0.0.1:0").await.unwrap();
    let receiver_addr = receiver_listener.local_addr().unwrap();
    let receiver_url = format!("http://{}/", receiver_addr);
    let (app, count, success, mut rx) = build_receiver();
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
            "id": "t-retry-1",
            "pushNotificationConfig": {
                "url": receiver_url,
                "token": "retry-token",
            },
        },
        "id": 1,
    });
    let _ = http.post(&server_url).json(&set_req).send().await.unwrap();

    // Drive task; the dispatcher will block until all 3 attempts finish
    // (retries sleep 100ms + 400ms = ~500ms worst case).
    let send_req = json!({
        "jsonrpc": "2.0",
        "method": "tasks/send",
        "params": spec("t-retry-1"),
        "id": 2,
    });
    let send_resp = http.post(&server_url).json(&send_req).send().await.unwrap();
    assert!(send_resp.status().is_success());

    // Collect receiver observations with a generous deadline covering
    // the exponential-backoff budget.
    let mut observed = Vec::new();
    for _ in 0..10 {
        match tokio::time::timeout(Duration::from_millis(500), rx.recv()).await {
            Ok(Some(code)) => observed.push(code),
            _ => break,
        }
        if observed.len() >= 3 {
            break;
        }
    }

    assert_eq!(
        count.load(Ordering::SeqCst),
        3,
        "expected 3 total attempts, observed codes = {:?}",
        observed
    );
    assert_eq!(
        success.load(Ordering::SeqCst),
        1,
        "expected exactly 1 successful delivery"
    );
    assert_eq!(observed, vec![503, 503, 200]);
}
