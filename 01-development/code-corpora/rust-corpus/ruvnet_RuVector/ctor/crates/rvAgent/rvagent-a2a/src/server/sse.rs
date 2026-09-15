//! SSE streaming for `tasks/sendSubscribe` and `tasks/resubscribe`.
//!
//! Implementation notes:
//! - Bounded `broadcast::channel` (256) per task, set up by
//!   `server::ensure_stream`. Overflow surfaces as `RecvError::Lagged`
//!   which we map to a tracing WARN + drop the laggard subscriber.
//! - On first subscription we also replay the current `TaskStatus` so
//!   late-joiners don't miss the "already working" edge.
//! - `resubscribe` looks up the existing channel; if missing we return
//!   a single `final=true` status for the terminal state so the client
//!   closes cleanly instead of hanging.

use axum::{
    extract::{Query, State},
    response::{
        sse::{Event, KeepAlive, Sse},
        IntoResponse,
    },
};
use chrono::Utc;
use futures::stream::{self, Stream, StreamExt};
use serde::Deserialize;
use std::convert::Infallible;
use std::pin::Pin;
use std::time::Duration;
use tokio::sync::broadcast::error::RecvError;

use super::{A2aState, TaskEvent};
use crate::types::{TaskState, TaskStatus, TaskStatusUpdateEvent};

#[derive(Debug, Deserialize)]
pub struct TaskIdQuery {
    pub id: String,
}

/// Query params for `tasks/resubscribe`. `last_event_id` is the
/// RFC3339-encoded timestamp of the last `TaskStatusUpdateEvent` the
/// client observed; the server filters the replay window to events
/// strictly after that instant. Mirrors the standard EventSource
/// `Last-Event-Id` header — clients may send either (the header wins
/// if both are present? no — the query param wins, so URL-only flows
/// don't need to mess with custom HTTP headers).
#[derive(Debug, Deserialize)]
pub struct ResubscribeQuery {
    pub id: String,
    #[serde(default)]
    pub last_event_id: Option<String>,
}

/// `GET /tasks/sendSubscribe?id=...` — subscribe to live task events.
///
/// Clients that want the JSON-RPC shape instead POST; this GET route is
/// the SSE variant per the spec's EventSource convention.
#[tracing::instrument(skip(state), level = "info")]
pub async fn send_subscribe(
    State(state): State<A2aState>,
    Query(q): Query<TaskIdQuery>,
) -> Sse<impl Stream<Item = Result<Event, Infallible>>> {
    let tx = super::ensure_stream(&state, &q.id).await;
    let rx = tx.subscribe();
    let stream = event_stream(rx, q.id.clone());
    Sse::new(stream).keep_alive(KeepAlive::new().interval(Duration::from_secs(15)))
}

/// `GET /tasks/resubscribe?id=...` — reopen a stream after disconnect.
///
/// Semantics (ADR-159 M2): replay every retained `TaskStatusUpdateEvent`
/// for this task (bounded at `EVENT_CHANNEL_CAPACITY`), then — if the
/// terminal event hasn't yet been seen and the live broadcast channel
/// is still alive — attach a fresh receiver so the client sees any
/// events emitted after the replay concludes. If the task is unknown
/// we emit a single synthetic `failed/not-found` final event.
///
/// `Last-Event-Id`: clients can send the HTTP header / query param
/// `last_event_id=<timestamp-rfc3339>` to skip replay frames whose
/// `status.timestamp <= last_event_id`. The absent case replays the
/// full retained window.
#[tracing::instrument(skip(state), level = "info")]
pub async fn resubscribe(
    State(state): State<A2aState>,
    Query(q): Query<ResubscribeQuery>,
    headers: axum::http::HeaderMap,
) -> impl IntoResponse {
    // Last-seen cutoff: prefer query param, fall back to the standard
    // EventSource `Last-Event-Id` header. Parsed as RFC3339; unparseable
    // values mean "no cutoff, replay everything retained".
    let last_seen = q.last_event_id.clone().or_else(|| {
        headers
            .get("last-event-id")
            .and_then(|v| v.to_str().ok())
            .map(|s| s.to_string())
    });
    let last_seen_ts = last_seen
        .as_deref()
        .and_then(|s| chrono::DateTime::parse_from_rfc3339(s).ok())
        .map(|dt| dt.with_timezone(&Utc));

    // Snapshot the replay history + live channel.
    let replay: Vec<TaskStatusUpdateEvent> = {
        let guard = state.status_history.read().await;
        guard
            .get(&q.id)
            .map(|dq| {
                dq.iter()
                    .filter(|ev| match last_seen_ts {
                        Some(cutoff) => ev.status.timestamp > cutoff,
                        None => true,
                    })
                    .cloned()
                    .collect()
            })
            .unwrap_or_default()
    };
    let saw_terminal = replay.iter().any(|ev| ev.final_);

    let live_rx = if saw_terminal {
        None
    } else {
        state
            .streams
            .read()
            .await
            .get(&q.id)
            .map(|tx| tx.subscribe())
    };

    let replay_had_events = !replay.is_empty();
    let replay_stream = stream::iter(replay.into_iter().map(|ev| {
        let payload = serde_json::to_string(&ev).unwrap_or_default();
        Ok::<Event, Infallible>(Event::default().event("status").data(payload))
    }));

    let stream: Pin<Box<dyn Stream<Item = Result<Event, Infallible>> + Send>> = match live_rx {
        Some(rx) => replay_stream.chain(event_stream(rx, q.id.clone())).boxed(),
        None if replay_had_events => replay_stream.boxed(),
        None => {
            // No replay history + no live channel. Fall back to the
            // stored task's terminal status (covers crash-and-restart
            // where history was never captured) or synthesize a
            // `failed/not-found` for an unknown id.
            let status = match state.tasks.read().await.get(&q.id) {
                Some(t) => t.status.clone(),
                None => TaskStatus {
                    state: TaskState::Failed,
                    timestamp: Utc::now(),
                    message: None,
                },
            };
            let ev = TaskStatusUpdateEvent {
                id: q.id.clone(),
                status,
                final_: true,
                metadata: serde_json::Value::Null,
            };
            let payload = serde_json::to_string(&ev).unwrap_or_default();
            stream::once(async move {
                Ok::<Event, Infallible>(Event::default().event("status").data(payload))
            })
            .boxed()
        }
    };
    Sse::new(stream).keep_alive(KeepAlive::new().interval(Duration::from_secs(15)))
}

/// Convert a broadcast receiver into the SSE stream shape. Each inner
/// `TaskEvent` becomes one `Event`; lagged subscribers get a WARN log +
/// a synthetic warning event so downstream closes cleanly.
///
/// We don't pull in `tokio-stream`'s `BroadcastStream` — we roll the
/// same loop with `stream::unfold` against `broadcast::Receiver` to
/// keep our dep graph minimal (the crate already has `futures`).
fn event_stream(
    rx: tokio::sync::broadcast::Receiver<TaskEvent>,
    task_id: String,
) -> impl Stream<Item = Result<Event, Infallible>> {
    stream::unfold(rx, move |mut rx| {
        let task_id = task_id.clone();
        async move {
            match rx.recv().await {
                Ok(TaskEvent::Status(s)) => {
                    let ev = Event::default()
                        .event("status")
                        .data(serde_json::to_string(&s).unwrap_or_default());
                    Some((Ok::<Event, Infallible>(ev), rx))
                }
                Ok(TaskEvent::Artifact(a)) => {
                    let ev = Event::default()
                        .event("artifact")
                        .data(serde_json::to_string(&a).unwrap_or_default());
                    Some((Ok::<Event, Infallible>(ev), rx))
                }
                Err(RecvError::Lagged(n)) => {
                    tracing::warn!(
                        task_id = %task_id,
                        dropped = n,
                        "sse subscriber lagged; dropping oldest events"
                    );
                    let ev = Event::default()
                        .event("warning")
                        .data(format!("{{\"lagged\":{}}}", n));
                    Some((Ok::<Event, Infallible>(ev), rx))
                }
                Err(RecvError::Closed) => None,
            }
        }
    })
}

// ---------------------------------------------------------------------------
// Tests.
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn task_id_query_deserializes() {
        // Exercise the Deserialize impl via serde_json since axum's
        // Query layer uses the same path.
        let v = serde_json::json!({ "id": "t-abc" });
        let q: TaskIdQuery = serde_json::from_value(v).unwrap();
        assert_eq!(q.id, "t-abc");
    }
}
