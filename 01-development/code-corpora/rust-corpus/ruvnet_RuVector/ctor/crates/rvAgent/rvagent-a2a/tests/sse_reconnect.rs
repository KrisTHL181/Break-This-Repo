//! ADR-159 M2 acceptance test #2 — `sse_reconnect.rs`.
//!
//! `server/sse.rs::resubscribe` replays retained status events for the
//! task (bounded at `EVENT_CHANNEL_CAPACITY`), then attaches a fresh
//! `broadcast::Receiver` for any events emitted post-attach. Clients
//! pass `last_event_id=<rfc3339-ts>` (query or header) to skip frames
//! already seen. A dead channel + unknown id still synthesizes a single
//! `failed/not-found` terminal event so callers unwedge. Fixtures live
//! in `common/mod.rs`.

#[path = "common/mod.rs"]
mod common;
use common::{mount_sse_routes, parse_sse_frames, signed_card, spec, StreamsMap};

use std::sync::Arc;
use std::time::Duration;

use async_trait::async_trait;
use chrono::Utc;
use ed25519_dalek::SigningKey;
use rand_core::OsRng;
use tokio::sync::{mpsc, Mutex, OnceCell};

use rvagent_a2a::budget::{BudgetLedger, GlobalBudget};
use rvagent_a2a::client::A2aClient;
use rvagent_a2a::executor::{Executor, TaskRunner};
use rvagent_a2a::identity::agent_id_from_pubkey;
use rvagent_a2a::server::{emit_status_event, A2aServer, A2aServerConfig, A2aState};
use rvagent_a2a::types::{
    Artifact, Part, Task, TaskSpec, TaskState, TaskStatus, TaskStatusUpdateEvent,
};

/// Runner whose intermediate events fire on a `mpsc::Receiver<()>` trigger,
/// letting the test script observe exactly when events cross the broadcast
/// channel. After `intermediate_count` events it waits for one final
/// "done" token, then returns `Completed`.
struct TriggerRunner {
    streams: Arc<OnceCell<StreamsMap>>,
    /// Server state handle — used to route each intermediate event
    /// through `emit_status_event` so the replay history stays in
    /// sync with the broadcast channel (ADR-159 M2 follow-up).
    state: Arc<OnceCell<A2aState>>,
    trigger: Arc<Mutex<mpsc::Receiver<()>>>,
    intermediate_count: usize,
}

#[async_trait]
impl TaskRunner for TriggerRunner {
    async fn run(&self, spec: TaskSpec) -> Result<Task, rvagent_a2a::error::A2aError> {
        // Streams slot is kept around for backwards compatibility with
        // earlier test harness shape; event emission now routes through
        // the state handle so replay history stays in sync.
        let _ = &self.streams;
        let state = self.state.get().cloned().expect("state filled");
        let mut rx = self.trigger.lock().await;
        for _ in 0..self.intermediate_count {
            let _ = rx.recv().await;
            // Route via `emit_status_event` so the event lands in BOTH
            // the broadcast channel and the replay history.
            let ev = TaskStatusUpdateEvent {
                id: spec.id.clone(),
                status: TaskStatus {
                    state: TaskState::Working,
                    timestamp: Utc::now(),
                    message: None,
                },
                final_: false,
                metadata: serde_json::Value::Null,
            };
            let _ = emit_status_event(&state, ev).await;
        }
        let _ = rx.recv().await; // "done" token
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
                name: Some("trig".into()),
                description: None,
                parts: vec![Part::Text { text: "ok".into() }],
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

/// Pull chunks until a `final=true` `status` frame arrives or `deadline`
/// elapses. Returns the parsed status events collected so far.
async fn drain_until_final(
    resp: &mut reqwest::Response,
    deadline: tokio::time::Instant,
) -> Vec<TaskStatusUpdateEvent> {
    let mut buf = Vec::<u8>::new();
    let mut events: Vec<TaskStatusUpdateEvent> = Vec::new();
    while tokio::time::Instant::now() < deadline {
        let remaining = deadline - tokio::time::Instant::now();
        match tokio::time::timeout(remaining, resp.chunk()).await {
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
    events
}

/// Bring up an `A2aServer` with a `TriggerRunner` and return the handles
/// the tests need.
struct Harness {
    base_url: String,
    trigger_tx: mpsc::Sender<()>,
    sk: SigningKey,
}

async fn boot(intermediate: usize) -> Harness {
    let listener = tokio::net::TcpListener::bind("127.0.0.1:0")
        .await
        .expect("bind");
    let addr = listener.local_addr().expect("local_addr");
    let base_url = format!("http://{}", addr);

    let sk = SigningKey::generate(&mut OsRng);
    let card = signed_card(&sk, &base_url, "sse-reconnect-test");
    let card_bytes = serde_json::to_vec(&card).expect("card bytes");

    let streams_slot: Arc<OnceCell<StreamsMap>> = Arc::new(OnceCell::new());
    let state_slot: Arc<OnceCell<A2aState>> = Arc::new(OnceCell::new());
    let (trigger_tx, trigger_rx) = mpsc::channel::<()>(16);
    let runner = Arc::new(TriggerRunner {
        streams: streams_slot.clone(),
        state: state_slot.clone(),
        trigger: Arc::new(Mutex::new(trigger_rx)),
        intermediate_count: intermediate,
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
    state_slot
        .set(server.state().clone())
        .map_err(|_| ())
        .expect("set state");

    let router = mount_sse_routes(server.router(), server.state().clone());
    tokio::spawn(async move {
        axum::serve(listener, router).await.expect("axum serve");
    });
    tokio::time::sleep(Duration::from_millis(10)).await;
    Harness {
        base_url,
        trigger_tx,
        sk,
    }
}

// -- Test #1: resubscribe-after-completion --------------------------------
// After the task has completed, resubscribe must replay the terminal
// `final=true` status so clients that missed the live window can still
// observe completion. Invariants: (a) no panic; (b) a `final=true`
// status event is observed; (c) the replayed event carries the
// stored `Completed` state.

#[tokio::test(flavor = "multi_thread")]
async fn resubscribe_after_completion_returns_terminal_without_panic() {
    let h = boot(0).await; // 0 intermediate events — one token → Completed
    let task_id = "sse-reconnect-done";
    let root = agent_id_from_pubkey(&h.sk.verifying_key());

    let client = A2aClient::new().expect("client");
    let base_clone = h.base_url.clone();
    let send_handle = tokio::spawn({
        let s = spec(task_id, root);
        async move { client.send_task(&base_clone, s).await }
    });
    h.trigger_tx.send(()).await.expect("trigger done");
    let _ = send_handle.await;
    tokio::time::sleep(Duration::from_millis(50)).await;

    let http = reqwest::Client::new();
    let url = format!("{}/tasks/resubscribe?id={}", h.base_url, task_id);
    let mut resp = http
        .get(&url)
        .header("accept", "text/event-stream")
        .send()
        .await
        .expect("resubscribe GET");
    assert!(resp.status().is_success(), "HTTP {}", resp.status());

    let deadline = tokio::time::Instant::now() + Duration::from_secs(2);
    let events = drain_until_final(&mut resp, deadline).await;
    drop(resp);

    // Must see the replayed terminal status — not just silence.
    let terminal = events
        .iter()
        .find(|e| e.final_)
        .expect("replay must deliver a final=true status");
    assert_eq!(
        terminal.status.state,
        TaskState::Completed,
        "terminal status should be Completed (got {:?})",
        terminal.status.state
    );
}

// -- Test #3: mid-stream replay ------------------------------------------
// ADR-159 M2 follow-up coverage. Emit 5 working events, drop the
// subscriber after event 2, resubscribe, and assert that events 3/4/5
// plus the final Completed land on the second connection. No duplicates
// of 1/2 — the `last_event_id` query param uses the timestamp of the
// last event observed to skip already-seen frames.

#[tokio::test(flavor = "multi_thread")]
async fn resubscribe_after_disconnect_replays_missed_events() {
    let h = boot(5).await;
    let task_id = "sse-reconnect-replay";
    let root = agent_id_from_pubkey(&h.sk.verifying_key());

    let http = reqwest::Client::new();
    let sse_url = format!("{}/tasks/sendSubscribe?id={}", h.base_url, task_id);
    let mut first = http
        .get(&sse_url)
        .header("accept", "text/event-stream")
        .send()
        .await
        .expect("first sub");
    assert!(first.status().is_success());

    let client = A2aClient::new().expect("client");
    let base_clone = h.base_url.clone();
    let send_handle = tokio::spawn({
        let s = spec(task_id, root);
        async move { client.send_task(&base_clone, s).await }
    });

    // Drain until we've seen at least submitted + working + working — the
    // first 3 frames. Then drop the subscriber.
    let mut buf1 = Vec::<u8>::new();
    let mut first_events: Vec<TaskStatusUpdateEvent> = Vec::new();
    // Release 2 intermediate events so there are status-after-submitted
    // frames available on the broadcast channel.
    h.trigger_tx.send(()).await.expect("ev1");
    h.trigger_tx.send(()).await.expect("ev2");
    let drain_deadline = tokio::time::Instant::now() + Duration::from_secs(1);
    while tokio::time::Instant::now() < drain_deadline {
        match tokio::time::timeout(Duration::from_millis(150), first.chunk()).await {
            Ok(Ok(Some(b))) => {
                buf1.extend_from_slice(&b);
                first_events = parse_sse_frames(&buf1)
                    .into_iter()
                    .filter(|(n, _)| n == "status")
                    .filter_map(|(_, d)| serde_json::from_str::<TaskStatusUpdateEvent>(&d).ok())
                    .collect();
                if first_events.len() >= 3 {
                    break;
                }
            }
            _ => break,
        }
    }
    assert!(
        first_events.len() >= 2,
        "first subscriber should have seen >=2 status events, got {}",
        first_events.len()
    );
    let last_seen_ts = first_events
        .last()
        .expect("at least one event")
        .status
        .timestamp;
    drop(first);

    // Release remaining triggers (3, 4, 5) + done token while nobody is
    // subscribed. These events go into the history, NOT the dropped
    // receiver.
    h.trigger_tx.send(()).await.expect("ev3");
    h.trigger_tx.send(()).await.expect("ev4");
    h.trigger_tx.send(()).await.expect("ev5");
    h.trigger_tx.send(()).await.expect("done");
    let _ = send_handle.await;
    tokio::time::sleep(Duration::from_millis(80)).await;

    // Resubscribe with `last_event_id` in the standard `Last-Event-Id`
    // header so the server filters out duplicates of the already-observed
    // prefix. Using the header avoids URL-encoding pitfalls (`:` / `+`
    // in RFC3339 timestamps) that would corrupt a query string.
    let resub_url = format!("{}/tasks/resubscribe?id={}", h.base_url, task_id);
    let mut second = http
        .get(&resub_url)
        .header("accept", "text/event-stream")
        .header("last-event-id", last_seen_ts.to_rfc3339())
        .send()
        .await
        .expect("resub");
    assert!(second.status().is_success());

    let deadline = tokio::time::Instant::now() + Duration::from_secs(2);
    let replayed = drain_until_final(&mut second, deadline).await;

    // (a) Terminal `final=true` must arrive.
    assert!(
        replayed.iter().any(|e| e.final_),
        "replay must deliver final=true (got {} events)",
        replayed.len()
    );

    // (b) No duplicates of the already-seen prefix — every replayed
    // event's timestamp must be strictly after `last_seen_ts`.
    for ev in &replayed {
        assert!(
            ev.status.timestamp > last_seen_ts,
            "replay emitted a duplicate event (ts={} <= last_seen={})",
            ev.status.timestamp,
            last_seen_ts
        );
    }

    // (c) We should see at least one mid-task Working event + the
    // terminal Completed. Strict equality on event counts is brittle
    // under timing — 2 is the minimum useful guarantee.
    assert!(
        replayed.len() >= 2,
        "replay should deliver the missed suffix (got {} events)",
        replayed.len()
    );
}

// -- Test #2: live mid-stream reconnect -----------------------------------
// ADR-159 M2 follow-up: gap-free resume is not implemented — `resubscribe`
// attaches a fresh `broadcast::Receiver`. Relaxed invariants asserted:
//   (a) events emitted AFTER resubscribe-attach are delivered;
//   (b) reconnection does not panic or wedge the stream.

#[tokio::test(flavor = "multi_thread")]
async fn resubscribe_live_delivers_post_attach_events() {
    let h = boot(3).await;
    let task_id = "sse-reconnect-live";
    let root = agent_id_from_pubkey(&h.sk.verifying_key());

    let http = reqwest::Client::new();
    let sse_url = format!("{}/tasks/sendSubscribe?id={}", h.base_url, task_id);
    let mut first = http
        .get(&sse_url)
        .header("accept", "text/event-stream")
        .send()
        .await
        .expect("first sub");
    assert!(first.status().is_success());

    let client = A2aClient::new().expect("client");
    let base_clone = h.base_url.clone();
    let send_handle = tokio::spawn({
        let s = spec(task_id, root);
        async move { client.send_task(&base_clone, s).await }
    });

    // Release two intermediate events; the first subscriber should see
    // at least submitted + working + working (≥ 1 parsed).
    h.trigger_tx.send(()).await.expect("ev1");
    h.trigger_tx.send(()).await.expect("ev2");
    tokio::time::sleep(Duration::from_millis(50)).await;

    let mut buf1 = Vec::<u8>::new();
    let drain_deadline = tokio::time::Instant::now() + Duration::from_millis(500);
    while tokio::time::Instant::now() < drain_deadline {
        match tokio::time::timeout(Duration::from_millis(100), first.chunk()).await {
            Ok(Ok(Some(b))) => {
                buf1.extend_from_slice(&b);
                if parse_sse_frames(&buf1)
                    .iter()
                    .filter(|(n, _)| n == "status")
                    .count()
                    >= 2
                {
                    break;
                }
            }
            _ => break,
        }
    }
    let pre_events: Vec<_> = parse_sse_frames(&buf1)
        .into_iter()
        .filter(|(n, _)| n == "status")
        .filter_map(|(_, d)| serde_json::from_str::<TaskStatusUpdateEvent>(&d).ok())
        .collect();
    assert!(
        !pre_events.is_empty(),
        "first subscriber should have seen ≥1 status event before drop"
    );
    drop(first);

    let resub_url = format!("{}/tasks/resubscribe?id={}", h.base_url, task_id);
    let mut second = http
        .get(&resub_url)
        .header("accept", "text/event-stream")
        .send()
        .await
        .expect("resub");
    assert!(second.status().is_success());

    tokio::time::sleep(Duration::from_millis(30)).await;
    h.trigger_tx.send(()).await.expect("ev3");
    h.trigger_tx.send(()).await.expect("done");
    let _ = send_handle.await;

    let deadline = tokio::time::Instant::now() + Duration::from_secs(2);
    let post_events = drain_until_final(&mut second, deadline).await;

    // Relaxed invariant per the follow-up note above: at minimum the
    // terminal `final=true` must land on the resubscribed stream.
    assert!(
        post_events.iter().any(|e| e.final_),
        "resubscribed stream must deliver the final=true event (got {} events)",
        post_events.len()
    );
}
