//! ADR-159 M2 acceptance test #3 — `sse_backpressure.rs`.
//!
//! Producer emits 500 status events on a slow-consumer task as fast as the
//! `broadcast::Sender::send` loop can go. The SSE subscriber reads at
//! ~1 chunk / 50 ms, forcing the per-task broadcast channel (cap 256 per
//! `server::ensure_stream`) to overflow.
//!
//! Observed policy — documented here rather than changed, per the test
//! brief (M4-branch peer owns server-side wiring in parallel):
//!
//!   * The producer does NOT block — `broadcast::Sender::send` is
//!     non-blocking; lagged receivers get `RecvError::Lagged(n)` on the
//!     next `recv()` rather than the sender blocking or erroring.
//!   * On overflow the channel drops the OLDEST slot. The
//!     `event_stream` function in `server/sse.rs` translates `Lagged(n)`
//!     into a synthetic `warning` SSE frame carrying `{"lagged":n}`.
//!
//! Policy in one line: **bounded broadcast, drop-oldest on overflow,
//! surface as a `warning` SSE event so clients see the gap.**

#[path = "common/mod.rs"]
mod common;
use common::{mount_sse_routes, parse_sse_frames, signed_card, spec, StreamsMap};

use std::sync::Arc;
use std::time::{Duration, Instant};

use async_trait::async_trait;
use chrono::Utc;
use ed25519_dalek::SigningKey;
use rand_core::OsRng;
use serde_json::json;
use tokio::sync::OnceCell;

use rvagent_a2a::budget::{BudgetLedger, GlobalBudget};
use rvagent_a2a::client::A2aClient;
use rvagent_a2a::executor::{Executor, TaskRunner};
use rvagent_a2a::identity::agent_id_from_pubkey;
use rvagent_a2a::server::{A2aServer, A2aServerConfig, TaskEvent};
use rvagent_a2a::types::{
    Artifact, Part, Task, TaskSpec, TaskState, TaskStatus, TaskStatusUpdateEvent,
};

/// Emits `count` `working` status events as fast as possible, each carrying
/// a monotonically increasing `seq` in `metadata.seq` so the consumer can
/// detect gaps from drop-oldest. Records its own wall-clock duration so
/// the outer test can assert the producer isn't blocked.
struct BurstRunner {
    streams: Arc<OnceCell<StreamsMap>>,
    count: usize,
    elapsed: Arc<parking_lot::Mutex<Option<Duration>>>,
}

#[async_trait]
impl TaskRunner for BurstRunner {
    async fn run(&self, spec: TaskSpec) -> Result<Task, rvagent_a2a::error::A2aError> {
        let streams = self.streams.get().cloned().expect("streams filled");
        let tx = streams
            .read()
            .await
            .get(&spec.id)
            .cloned()
            .expect("broadcast sender should exist after ensure_stream");

        let start = Instant::now();
        for seq in 0..self.count {
            let _ = tx.send(TaskEvent::Status(TaskStatusUpdateEvent {
                id: spec.id.clone(),
                status: TaskStatus {
                    state: TaskState::Working,
                    timestamp: Utc::now(),
                    message: None,
                },
                final_: false,
                metadata: json!({ "seq": seq }),
            }));
        }
        *self.elapsed.lock() = Some(start.elapsed());

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
                name: Some("burst".into()),
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
async fn bounded_channel_drops_oldest_without_blocking_producer() {
    let listener = tokio::net::TcpListener::bind("127.0.0.1:0")
        .await
        .expect("bind");
    let addr = listener.local_addr().expect("local_addr");
    let base_url = format!("http://{}", addr);

    let sk = SigningKey::generate(&mut OsRng);
    let card = signed_card(&sk, &base_url, "sse-backpressure-test");
    let card_bytes = serde_json::to_vec(&card).expect("card bytes");

    let streams_slot: Arc<OnceCell<StreamsMap>> = Arc::new(OnceCell::new());
    let elapsed: Arc<parking_lot::Mutex<Option<Duration>>> =
        Arc::new(parking_lot::Mutex::new(None));
    let runner = Arc::new(BurstRunner {
        streams: streams_slot.clone(),
        count: 500,
        elapsed: elapsed.clone(),
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
        .set(server.state().streams.clone())
        .map_err(|_| ())
        .expect("set streams");

    let router = mount_sse_routes(server.router(), server.state().clone());
    tokio::spawn(async move {
        axum::serve(listener, router).await.expect("axum serve");
    });
    tokio::time::sleep(Duration::from_millis(10)).await;

    // Subscribe FIRST so the broadcast channel is live + a subscriber
    // exists to be lagged when the burst fires.
    let task_id = "sse-backpressure-t1";
    let root = agent_id_from_pubkey(&sk.verifying_key());
    let http = reqwest::Client::new();
    let sse_url = format!("{}/tasks/sendSubscribe?id={}", base_url, task_id);
    let mut resp = http
        .get(&sse_url)
        .header("accept", "text/event-stream")
        .send()
        .await
        .expect("sse connect");
    assert!(resp.status().is_success(), "sse HTTP {}", resp.status());

    // Drive `tasks/send` in the background. `dispatch_to_executor` runs
    // the burst synchronously, so the POST won't return until all 500
    // events are broadcast.
    let client = A2aClient::new().expect("client");
    let send_base = base_url.clone();
    let send_spec = spec(task_id, root);
    let wall_start = Instant::now();
    let send_handle = tokio::spawn(async move { client.send_task(&send_base, send_spec).await });

    // Slow consumer: inter-read sleep forces the cap-256 buffer to
    // overflow. We stop as soon as the `warning` frame arrives (Lagged →
    // warning translation kicked in) or we observe the final=true.
    let consume_deadline = Instant::now() + Duration::from_secs(3);
    let mut buf = Vec::<u8>::new();
    let mut frames: Vec<(String, String)> = Vec::new();
    while Instant::now() < consume_deadline {
        match tokio::time::timeout(Duration::from_millis(200), resp.chunk()).await {
            Ok(Ok(Some(b))) => {
                buf.extend_from_slice(&b);
                frames = parse_sse_frames(&buf);
                let saw_warning = frames.iter().any(|(n, _)| n == "warning");
                let saw_final = frames.iter().any(|(n, d)| {
                    n == "status"
                        && serde_json::from_str::<TaskStatusUpdateEvent>(d)
                            .map(|e| e.final_)
                            .unwrap_or(false)
                });
                if saw_warning || saw_final {
                    break;
                }
            }
            Ok(Ok(None)) | Ok(Err(_)) | Err(_) => break,
        }
        tokio::time::sleep(Duration::from_millis(50)).await;
    }
    let wall_elapsed = wall_start.elapsed();
    drop(resp);
    let _ = send_handle.await;

    // Producer must not block. The burst is the ground truth; in practice
    // it comes in well under 100 ms because `broadcast::send` is
    // non-blocking.
    let burst_dur = elapsed
        .lock()
        .expect("runner recorded elapsed — runner did not run to completion");
    assert!(
        burst_dur < Duration::from_secs(5),
        "producer burst took {:?} — expected <5s, producer appears to be blocked",
        burst_dur
    );
    // Loose outer bound to catch a hung consumer loop.
    assert!(
        wall_elapsed < Duration::from_secs(10),
        "end-to-end wall time {:?} suspicious — consumer loop may have hung",
        wall_elapsed
    );

    // Consumer observed EITHER a `warning` frame (Lagged → warning) OR a
    // gap in the delivered `seq` stream — either outcome documents the
    // bounded-channel drop-oldest policy.
    let statuses: Vec<TaskStatusUpdateEvent> = frames
        .iter()
        .filter(|(n, _)| n == "status")
        .filter_map(|(_, d)| serde_json::from_str(d).ok())
        .collect();
    let warnings: Vec<&(String, String)> = frames.iter().filter(|(n, _)| n == "warning").collect();
    let seqs: Vec<u64> = statuses
        .iter()
        .filter_map(|e| e.metadata.get("seq").and_then(|v| v.as_u64()))
        .collect();
    // Lossless delivery would have seqs starting at 0. Under drop-oldest
    // the first delivered seq is much higher.
    let saw_skipped_start = seqs.first().copied().unwrap_or(0) > 0;
    let saw_sequence_gap = seqs
        .last()
        .map(|&last| last + 1 > seqs.len() as u64)
        .unwrap_or(false);

    assert!(
        !warnings.is_empty() || saw_sequence_gap || saw_skipped_start,
        "slow consumer saw no warning events AND no gap in seq numbers \
         (status events: {}, seqs first/last: {:?}/{:?}); \
         that contradicts the bounded-channel drop-oldest policy",
        statuses.len(),
        seqs.first(),
        seqs.last()
    );
}
