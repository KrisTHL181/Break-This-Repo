//! ADR-159 M2 acceptance test #1 — `sse_stream.rs`.
//!
//! Spin up an `A2aServer` behind a real TCP listener, subscribe to
//! `tasks/sendSubscribe` via a raw `reqwest` GET (the `A2aClient::stream_task`
//! helper returns a `reqwest::Response` anyway — going direct keeps the byte
//! parser inline so we can inspect every `event:` frame), then POST a
//! `tasks/send` that dispatches through a `SlowRunner` which emits 3
//! intermediate `Working` status events on the task's broadcast channel
//! before returning `Completed`.
//!
//! Observed SSE event shape for a completed task under the current
//! `server/sse.rs` implementation:
//!
//!   1. `submitted` (final=false) — emitted by `dispatch_to_executor`
//!      BEFORE the runner is entered.
//!   2. `working`   (final=false) — emitted by the runner, ×3 with ~30 ms
//!      spacing.
//!   3. `completed` (final=true)  — emitted by `dispatch_to_executor` AFTER
//!      the runner returns.
//!
//! So this test expects **5** events, with `final_: true` on exactly the
//! last one. Fixtures + SSE byte parser live in `common/mod.rs`.

#[path = "common/mod.rs"]
mod common;
use common::{mount_sse_routes, parse_sse_frames, signed_card, spec, StreamsMap};

use std::sync::Arc;
use std::time::Duration;

use async_trait::async_trait;
use chrono::Utc;
use ed25519_dalek::SigningKey;
use rand_core::OsRng;
use tokio::sync::{OnceCell, RwLock};

use rvagent_a2a::budget::{BudgetLedger, GlobalBudget};
use rvagent_a2a::client::A2aClient;
use rvagent_a2a::executor::{Executor, TaskRunner};
use rvagent_a2a::identity::agent_id_from_pubkey;
use rvagent_a2a::server::{A2aServer, A2aServerConfig, TaskEvent};
use rvagent_a2a::types::{
    Artifact, Part, Task, TaskSpec, TaskState, TaskStatus, TaskStatusUpdateEvent,
};

/// Runner that emits 3 `working` status events on the task's broadcast
/// channel (each `final=false`, spaced by `step`) before returning a
/// `Completed` task.
struct SlowRunner {
    /// Injected after `A2aServer::new` so the runner can reach the same
    /// `streams` map the SSE handler attaches subscribers to.
    streams: Arc<OnceCell<StreamsMap>>,
    step: Duration,
}

#[async_trait]
impl TaskRunner for SlowRunner {
    async fn run(&self, spec: TaskSpec) -> Result<Task, rvagent_a2a::error::A2aError> {
        let streams = self.streams.get().cloned().expect("streams filled");
        for _ in 0..3 {
            tokio::time::sleep(self.step).await;
            let tx = streams.read().await.get(&spec.id).cloned();
            if let Some(tx) = tx {
                let _ = tx.send(TaskEvent::Status(TaskStatusUpdateEvent {
                    id: spec.id.clone(),
                    status: TaskStatus {
                        state: TaskState::Working,
                        timestamp: Utc::now(),
                        message: None,
                    },
                    final_: false,
                    metadata: serde_json::Value::Null,
                }));
            }
        }
        Ok(Task {
            id: spec.id,
            session_id: None,
            status: TaskStatus {
                state: TaskState::Completed,
                timestamp: Utc::now(),
                message: None,
            },
            history: vec![spec.message],
            artifacts: vec![Artifact {
                name: Some("slow".into()),
                description: None,
                parts: vec![Part::Text {
                    text: "done".into(),
                }],
                index: 0,
                append: false,
                last_chunk: true,
                metadata: serde_json::Value::Null,
            }],
            metadata: spec.metadata,
        })
    }
    async fn cancel(&self, _id: &str) -> Result<(), rvagent_a2a::error::A2aError> {
        Ok(())
    }
}

#[tokio::test(flavor = "multi_thread")]
async fn sse_stream_receives_submitted_working_x3_completed_in_order() {
    // 1. Bind ephemeral TCP port (same pattern as executor_remote.rs).
    let listener = tokio::net::TcpListener::bind("127.0.0.1:0")
        .await
        .expect("bind");
    let addr = listener.local_addr().expect("local_addr");
    let base_url = format!("http://{}", addr);

    // 2. Assemble server with a SlowRunner that'll emit 3 intermediate
    //    `working` events when the dispatcher calls into it.
    let sk = SigningKey::generate(&mut OsRng);
    let card = signed_card(&sk, &base_url, "sse-stream-test");
    let card_bytes = serde_json::to_vec(&card).expect("card bytes");
    let streams_slot: Arc<OnceCell<StreamsMap>> = Arc::new(OnceCell::new());
    let runner = Arc::new(SlowRunner {
        streams: streams_slot.clone(),
        step: Duration::from_millis(30),
    });
    let executor = Arc::new(Executor::Local(runner));
    let budget = Arc::new(BudgetLedger::new(GlobalBudget::default()));
    let server = A2aServer::new(
        card.clone(),
        card_bytes,
        executor,
        budget,
        A2aServerConfig::default(),
    );
    streams_slot
        .set(server.state().streams.clone() as Arc<RwLock<_>>)
        .map_err(|_| ())
        .expect("once-cell set");

    let router = mount_sse_routes(server.router(), server.state().clone());
    tokio::spawn(async move {
        axum::serve(listener, router).await.expect("axum serve");
    });
    tokio::time::sleep(Duration::from_millis(10)).await;

    // 3. Open the SSE subscription FIRST. This pre-creates the broadcast
    //    channel via `ensure_stream` and guarantees we're attached before
    //    `tasks/send` begins emitting.
    let task_id = "sse-stream-t1";
    let root = agent_id_from_pubkey(&sk.verifying_key());
    let http = reqwest::Client::new();
    let sse_url = format!("{}/tasks/sendSubscribe?id={}", base_url, task_id);
    let resp = http
        .get(&sse_url)
        .header("accept", "text/event-stream")
        .send()
        .await
        .expect("sse connect");
    assert!(resp.status().is_success(), "sse HTTP {}", resp.status());
    // reqwest's `stream` feature isn't enabled in this crate; pull chunks
    // with `Response::chunk()` which the default feature set exposes.
    let mut sse_resp = resp;

    // 4. Drive `tasks/send` on a background task — it blocks for ~90 ms
    //    while the SlowRunner emits its intermediate events. Routing
    //    through A2aClient exercises the same code path a real peer takes.
    let send_client = A2aClient::new().expect("client");
    let send_base = base_url.clone();
    let send_spec = spec(task_id, root);
    let send_handle =
        tokio::spawn(async move { send_client.send_task(&send_base, send_spec).await });

    // 5. Collect SSE frames until a `final=true` event arrives or we hit
    //    the safety deadline. Re-parse the full buffer each pass — with 5
    //    small events the cost is trivial.
    let mut buf = Vec::<u8>::new();
    let mut events: Vec<TaskStatusUpdateEvent> = Vec::new();
    let deadline = tokio::time::Instant::now() + Duration::from_secs(3);
    while tokio::time::Instant::now() < deadline {
        let remaining = deadline - tokio::time::Instant::now();
        match tokio::time::timeout(remaining, sse_resp.chunk()).await {
            Ok(Ok(Some(b))) => buf.extend_from_slice(&b),
            Ok(Ok(None)) | Ok(Err(_)) | Err(_) => break,
        }
        events.clear();
        for (name, data) in parse_sse_frames(&buf) {
            if name == "status" {
                if let Ok(ev) = serde_json::from_str::<TaskStatusUpdateEvent>(&data) {
                    events.push(ev);
                }
            }
        }
        if events.last().map(|e| e.final_).unwrap_or(false) {
            break;
        }
    }
    let _ = send_handle.await;

    // 6. Exactly 5 status events, in order, with `final=true` only on the
    //    last one.
    assert_eq!(
        events.len(),
        5,
        "expected 5 SSE status events, got {}: {:?}",
        events.len(),
        events.iter().map(|e| e.status.state).collect::<Vec<_>>()
    );
    assert_eq!(events[0].status.state, TaskState::Submitted);
    assert_eq!(events[1].status.state, TaskState::Working);
    assert_eq!(events[2].status.state, TaskState::Working);
    assert_eq!(events[3].status.state, TaskState::Working);
    assert_eq!(events[4].status.state, TaskState::Completed);
    for (i, ev) in events.iter().enumerate() {
        if i == events.len() - 1 {
            assert!(ev.final_, "last event must be final=true");
        } else {
            assert!(!ev.final_, "intermediate event {} was final=true", i);
        }
    }
}
