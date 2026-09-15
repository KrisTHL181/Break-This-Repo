//! JSON-RPC 2.0 framing + method dispatch.
//!
//! The A2A spec layers JSON-RPC 2.0 on top of a single HTTP `POST /`. This
//! module owns the envelope types and the `dispatch` handler; per-method
//! logic lives in-line but delegates the heavy lifting to
//! `crate::executor`, `crate::budget`, `crate::policy`,
//! `crate::recursion_guard`.

use std::sync::Arc;

use axum::{extract::State, http::StatusCode, response::IntoResponse, Json};
use chrono::Utc;
use serde::{Deserialize, Serialize};
use serde_json::Value;

use super::{A2aState, TaskEvent};
use crate::error::A2aError;
use crate::types::{Message, Task, TaskSpec, TaskState, TaskStatus, TaskStatusUpdateEvent};

// ---------------------------------------------------------------------------
// JSON-RPC 2.0 envelope.
// ---------------------------------------------------------------------------

/// Standard JSON-RPC 2.0 request. `id` is `null` for notifications.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct JsonRpcRequest {
    pub jsonrpc: String,
    pub method: String,
    #[serde(default)]
    pub params: Value,
    #[serde(default)]
    pub id: Value,
}

/// Standard JSON-RPC 2.0 response. Exactly one of `result` or `error`
/// must be set.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct JsonRpcResponse {
    pub jsonrpc: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub result: Option<Value>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub error: Option<JsonRpcError>,
    pub id: Value,
}

impl JsonRpcResponse {
    pub fn ok(id: Value, result: Value) -> Self {
        Self {
            jsonrpc: "2.0".into(),
            result: Some(result),
            error: None,
            id,
        }
    }

    pub fn err(id: Value, error: JsonRpcError) -> Self {
        Self {
            jsonrpc: "2.0".into(),
            result: None,
            error: Some(error),
            id,
        }
    }
}

/// JSON-RPC error object. Codes follow the spec plus A2A reservations:
///
/// - `-32700` Parse error
/// - `-32600` Invalid Request
/// - `-32601` Method not found
/// - `-32602` Invalid params
/// - `-32603` Internal error
/// - `-32001` Authentication required (A2A-reserved; see ADR-159 M1)
/// - `-32002` Policy violation (ruvector extension)
/// - `-32003` Budget exceeded (ruvector extension)
/// - `-32004` Recursion limit (ruvector extension)
/// - `-32005` Artifact version unsupported (ruvector extension)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct JsonRpcError {
    pub code: i32,
    pub message: String,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub data: Option<Value>,
}

impl JsonRpcError {
    pub fn parse_error(msg: impl Into<String>) -> Self {
        Self {
            code: -32700,
            message: msg.into(),
            data: None,
        }
    }
    pub fn invalid_request(msg: impl Into<String>) -> Self {
        Self {
            code: -32600,
            message: msg.into(),
            data: None,
        }
    }
    pub fn method_not_found(method: &str) -> Self {
        Self {
            code: -32601,
            message: format!("method not found: {}", method),
            data: None,
        }
    }
    pub fn invalid_params(msg: impl Into<String>) -> Self {
        Self {
            code: -32602,
            message: msg.into(),
            data: None,
        }
    }
    pub fn internal(msg: impl Into<String>) -> Self {
        Self {
            code: -32603,
            message: msg.into(),
            data: None,
        }
    }
    pub fn auth_required() -> Self {
        Self {
            code: -32001,
            message: "authentication required".into(),
            data: None,
        }
    }
}

impl From<A2aError> for JsonRpcError {
    fn from(e: A2aError) -> Self {
        match e {
            A2aError::PolicyError(inner) => JsonRpcError {
                code: -32002,
                message: format!("policy violation: {}", inner),
                data: None,
            },
            A2aError::BudgetExceeded(inner) => JsonRpcError {
                code: -32003,
                message: format!("budget exceeded: {}", inner),
                data: None,
            },
            A2aError::RecursionLimit { depth, path } => JsonRpcError {
                code: -32004,
                message: format!("recursion limit at depth {}", depth),
                data: Some(serde_json::json!({ "path": path })),
            },
            A2aError::ArtifactVersionUnsupported { got, supported } => JsonRpcError {
                code: -32005,
                message: format!("artifact version {} unsupported", got),
                data: Some(serde_json::json!({ "supported": supported })),
            },
            A2aError::CardSignatureInvalid => JsonRpcError {
                code: -32006,
                message: "card signature invalid".into(),
                data: None,
            },
            A2aError::NotFound(what) => JsonRpcError {
                code: -32010,
                message: format!("not found: {}", what),
                data: None,
            },
            A2aError::Discovery(m) => JsonRpcError {
                code: -32011,
                message: format!("discovery: {}", m),
                data: None,
            },
            A2aError::Transport(m) => JsonRpcError {
                code: -32012,
                message: format!("transport: {}", m),
                data: None,
            },
            A2aError::JsonRpc(m) => JsonRpcError::invalid_request(m),
            A2aError::Internal(m) => JsonRpcError::internal(m),
        }
    }
}

// ---------------------------------------------------------------------------
// Dispatch entry point.
// ---------------------------------------------------------------------------

/// The single `POST /` handler. Parses the envelope, routes on `method`,
/// returns a JSON-RPC response.
#[tracing::instrument(skip(state, body), level = "info")]
pub async fn dispatch(State(state): State<A2aState>, body: axum::body::Bytes) -> impl IntoResponse {
    let req: JsonRpcRequest = match serde_json::from_slice(&body) {
        Ok(r) => r,
        Err(e) => {
            return (
                StatusCode::OK,
                Json(JsonRpcResponse::err(
                    Value::Null,
                    JsonRpcError::parse_error(e.to_string()),
                )),
            );
        }
    };
    if req.jsonrpc != "2.0" {
        return (
            StatusCode::OK,
            Json(JsonRpcResponse::err(
                req.id,
                JsonRpcError::invalid_request("jsonrpc != \"2.0\""),
            )),
        );
    }

    let method = req.method.clone();
    let id = req.id.clone();
    tracing::debug!(method = %method, "a2a rpc");

    let resp = match method.as_str() {
        "tasks/send" => handle_tasks_send(&state, req.params).await,
        "tasks/get" => handle_tasks_get(&state, req.params).await,
        "tasks/cancel" => handle_tasks_cancel(&state, req.params).await,
        // sendSubscribe + resubscribe are SSE and normally do NOT land
        // here (the client picks the GET route). Left in as JSON-RPC
        // for spec conformance; a POST that asks for these simply
        // returns 200 with a pointer-style response.
        "tasks/sendSubscribe" => handle_tasks_send(&state, req.params).await,
        "tasks/resubscribe" => Ok(serde_json::json!({ "pending": true })),
        "tasks/pushNotification/set" => super::push::handle_set(&state, req.params).await,
        "tasks/pushNotification/get" => super::push::handle_get(&state, req.params).await,
        other => Err(A2aError::JsonRpc(format!("method not found: {}", other))),
    };

    match resp {
        Ok(v) => (StatusCode::OK, Json(JsonRpcResponse::ok(id, v))),
        Err(e) => (StatusCode::OK, Json(JsonRpcResponse::err(id, e.into()))),
    }
}

// ---------------------------------------------------------------------------
// tasks/send.
// ---------------------------------------------------------------------------

/// `tasks/send` — the synchronous one-shot dispatch path.
///
/// Ordering (ADR-159 M3 §Dispatch ordering):
///   GlobalBudget → RecursionPolicy → PeerSelector → TaskPolicy → Runner
///
/// In M1 the peer selector is a no-op (Local only), but the gate chain
/// is installed in the correct order now so M3 slots in cleanly.
#[tracing::instrument(skip(state), level = "info")]
async fn handle_tasks_send(state: &A2aState, params: Value) -> Result<Value, A2aError> {
    let spec: TaskSpec = serde_json::from_value(params)
        .map_err(|e| A2aError::JsonRpc(format!("tasks/send: invalid params: {}", e)))?;

    // Canonical dispatch ordering per ADR-159 M3 + tests/dispatch_order.rs:
    //   budget → recursion → policy → (runner). PeerSelector slots between
    //   policy and the runner at M3 (not in M1).

    // 1. Global budget (r3). Conservative pre-flight: zero cost + zero
    //    tokens at admission — runners report actuals post-flight.
    state.budget.try_consume(0.0, 0)?;

    // 2. Recursion guard (r3).
    let target = spec.context.root_agent_id.clone();
    crate::recursion_guard::check(
        &crate::recursion_guard::RecursionPolicy::default(),
        &spec.context,
        target,
    )
    .map_err(|e| match e {
        crate::recursion_guard::RecursionError::MaxDepthExceeded { depth, .. } => {
            A2aError::RecursionLimit {
                depth,
                path: spec
                    .context
                    .visited_agents
                    .iter()
                    .map(|a| a.0.clone())
                    .collect(),
            }
        }
        crate::recursion_guard::RecursionError::Revisit { path, .. } => A2aError::RecursionLimit {
            depth: spec.context.depth,
            path: path.into_iter().map(|a| a.0).collect(),
        },
    })?;

    // 3. Policy (r2). No-op when spec.policy is None.
    if let Some(policy) = &spec.policy {
        let guard = crate::policy::PolicyGuard::new(policy.clone());
        let _ticket = guard.enter(&spec)?;
    }

    // 4. Peer selector (r3 M3). Consult the router, if configured, to
    //    find a healthy peer for this skill. On hit, forward via
    //    `Executor::Remote(Peer)`; on success update EWMA stats, on
    //    failure record_failure and fall through to the local executor.
    //    This matches the ADR-159 M3 dispatch ordering: policy →
    //    PeerSelector → runner, with circuit-breaker semantics.
    let tx = super::ensure_stream(state, &spec.id).await;
    let task = match route_and_forward(state, &spec, &tx).await {
        Ok(Some(t)) => t,
        Ok(None) => dispatch_to_executor(state, &spec, &tx).await?,
        Err(e) => {
            tracing::warn!(
                target = "a2a::routing",
                task_id = %spec.id,
                skill = %spec.skill,
                error = %e,
                "remote forward failed; falling through to local executor",
            );
            dispatch_to_executor(state, &spec, &tx).await?
        }
    };

    // Store so `tasks/get` can find it.
    state
        .tasks
        .write()
        .await
        .insert(task.id.clone(), task.clone());

    Ok(serde_json::to_value(&task)?)
}

/// Consult the (optional) router, and if it picks a peer, forward the
/// task to that peer via `Executor::Remote`. Returns `Ok(Some(task))`
/// when a remote forward succeeded, `Ok(None)` when no router is
/// configured OR the selector returned no peer (→ caller falls through
/// to local), and `Err(..)` when the forward failed transport-side (→
/// caller records + falls through).
///
/// Updates `PeerRegistry` stats + breaker state on every attempt.
async fn route_and_forward(
    state: &A2aState,
    spec: &TaskSpec,
    tx: &super::TaskEventTx,
) -> Result<Option<Task>, A2aError> {
    let router = match state.router.as_ref() {
        Some(r) => r.clone(),
        None => return Ok(None),
    };
    let snapshot = match router.route(&spec.skill).await {
        Some(s) => s,
        None => return Ok(None),
    };

    let base_url = match url::Url::parse(&snapshot.card.url) {
        Ok(u) => u,
        Err(e) => {
            tracing::warn!(
                target = "a2a::routing",
                peer_id = %snapshot.id,
                peer_url = %snapshot.card.url,
                error = %e,
                "peer card url is not a valid URL; falling through to local",
            );
            return Ok(None);
        }
    };

    // Build a one-shot A2aClient for the forward. `reqwest::Client` has
    // its own connection pool per instance so this is cheap; a future
    // optimization could share a single client across forwards.
    let client = match crate::client::A2aClient::new() {
        Ok(c) => Arc::new(c),
        Err(e) => {
            tracing::warn!(
                target = "a2a::routing",
                error = %e,
                "A2aClient::new failed; cannot forward, falling through to local",
            );
            return Ok(None);
        }
    };

    let peer = crate::executor::Peer {
        id: snapshot.id.clone(),
        card: snapshot.card.clone(),
        base_url,
        client,
    };
    let remote = crate::executor::Executor::Remote(Box::new(peer));

    // Emit the submitted status before the remote call so SSE
    // subscribers observe the lifecycle transitions same as a local
    // run.
    let submitted = TaskStatus {
        state: TaskState::Submitted,
        timestamp: Utc::now(),
        message: None,
    };
    let submitted_ev = TaskStatusUpdateEvent {
        id: spec.id.clone(),
        status: submitted.clone(),
        final_: false,
        metadata: Value::Null,
    };
    super::record_status_event(state, &submitted_ev).await;
    let _ = tx.send(TaskEvent::Status(submitted_ev));

    let selector_name = router.selector.name();
    tracing::info!(
        target = "a2a::routing",
        selected_peer = %snapshot.id,
        peer_url = %snapshot.card.url,
        selector = %selector_name,
        task_id = %spec.id,
        skill = %spec.skill,
        "task forwarded",
    );

    let started = std::time::Instant::now();
    let result = remote.run(spec.clone()).await;
    let elapsed_ms = started.elapsed().as_millis() as f64;

    match result {
        Ok(mut task) => {
            // Update EWMA + reset breaker on success.
            router.registry.update_stats(&snapshot.id, elapsed_ms, 0.0);
            router.registry.record_success(&snapshot.id);

            // Stamp routing provenance into the task metadata so
            // callers can observe which peer handled it without
            // having to scrape logs.
            if let Some(obj) = task.metadata.as_object_mut() {
                let ruvector = obj
                    .entry("ruvector")
                    .or_insert_with(|| serde_json::json!({}));
                if let Some(rm) = ruvector.as_object_mut() {
                    rm.insert(
                        "routed_via".into(),
                        serde_json::json!({
                            "peer_id": snapshot.id.0,
                            "peer_url": snapshot.card.url,
                            "selector": selector_name,
                        }),
                    );
                }
            } else {
                task.metadata = serde_json::json!({
                    "ruvector": {
                        "routed_via": {
                            "peer_id": snapshot.id.0,
                            "peer_url": snapshot.card.url,
                            "selector": selector_name,
                        }
                    }
                });
            }

            // Broadcast the final status so SSE subscribers see the
            // remote-returned lifecycle state.
            let final_ev = TaskStatusUpdateEvent {
                id: spec.id.clone(),
                status: task.status.clone(),
                final_: true,
                metadata: Value::Null,
            };
            super::record_status_event(state, &final_ev).await;
            let _ = tx.send(TaskEvent::Status(final_ev));

            super::push::notify_state_change(state, &task.id, &task, &task.status).await;
            Ok(Some(task))
        }
        Err(e) => {
            router.registry.record_failure(&snapshot.id);
            Err(e)
        }
    }
}

/// Actually invoke the executor. Broadcasts status + artifact events on
/// the task's channel so any SSE subscriber sees them in real time.
async fn dispatch_to_executor(
    state: &A2aState,
    spec: &TaskSpec,
    tx: &super::TaskEventTx,
) -> Result<Task, A2aError> {
    // Initial `submitted` -> `working` transition.
    let submitted = TaskStatus {
        state: TaskState::Submitted,
        timestamp: Utc::now(),
        message: None,
    };
    let submitted_ev = TaskStatusUpdateEvent {
        id: spec.id.clone(),
        status: submitted.clone(),
        final_: false,
        metadata: Value::Null,
    };
    super::record_status_event(state, &submitted_ev).await;
    let _ = tx.send(TaskEvent::Status(submitted_ev));

    let executor = state.executor.clone();
    let result = executor.run(spec.clone()).await;

    let (final_status, artifacts) = match result {
        Ok(task) => (task.status, task.artifacts),
        Err(e) => (
            TaskStatus {
                state: TaskState::Failed,
                timestamp: Utc::now(),
                message: Some(Message {
                    role: crate::types::Role::Agent,
                    parts: vec![crate::types::Part::Text {
                        text: format!("{}", e),
                    }],
                    metadata: Value::Null,
                }),
            },
            vec![],
        ),
    };

    let final_ev = TaskStatusUpdateEvent {
        id: spec.id.clone(),
        status: final_status.clone(),
        final_: true,
        metadata: Value::Null,
    };
    super::record_status_event(state, &final_ev).await;
    let _ = tx.send(TaskEvent::Status(final_ev));

    let task = Task {
        id: spec.id.clone(),
        session_id: None,
        status: final_status.clone(),
        history: vec![spec.message.clone()],
        artifacts,
        metadata: spec.metadata.clone(),
    };

    // Push-notification webhook (M4). Best-effort; logs + returns on
    // failure without poisoning the JSON-RPC response.
    super::push::notify_state_change(state, &task.id, &task, &final_status).await;

    Ok(task)
}

// ---------------------------------------------------------------------------
// tasks/get + tasks/cancel.
// ---------------------------------------------------------------------------

#[derive(Debug, Deserialize)]
struct IdParams {
    id: String,
}

#[tracing::instrument(skip(state), level = "debug")]
async fn handle_tasks_get(state: &A2aState, params: Value) -> Result<Value, A2aError> {
    let p: IdParams = serde_json::from_value(params)
        .map_err(|e| A2aError::JsonRpc(format!("tasks/get: {}", e)))?;
    let tasks = state.tasks.read().await;
    let task = tasks
        .get(&p.id)
        .cloned()
        .ok_or_else(|| A2aError::NotFound(format!("task {}", p.id)))?;
    Ok(serde_json::to_value(task)?)
}

#[tracing::instrument(skip(state), level = "info")]
async fn handle_tasks_cancel(state: &A2aState, params: Value) -> Result<Value, A2aError> {
    let p: IdParams = serde_json::from_value(params)
        .map_err(|e| A2aError::JsonRpc(format!("tasks/cancel: {}", e)))?;

    // Ask the executor to cancel. Best-effort; the runner may already be
    // past the checkpoint that honors the signal.
    state.executor.cancel(&p.id).await?;

    // Update the stored task to `canceled` if we have it, and fan out
    // the push webhook for the terminal transition.
    let canceled_status = TaskStatus {
        state: TaskState::Canceled,
        timestamp: Utc::now(),
        message: None,
    };
    let mut tasks = state.tasks.write().await;
    if let Some(task) = tasks.get_mut(&p.id) {
        task.status = canceled_status.clone();
        let snapshot = task.clone();
        drop(tasks);

        // Record + broadcast the terminal cancel so SSE subscribers
        // (and `resubscribe` replay) observe the transition.
        let cancel_ev = TaskStatusUpdateEvent {
            id: p.id.clone(),
            status: canceled_status.clone(),
            final_: true,
            metadata: Value::Null,
        };
        super::record_status_event(state, &cancel_ev).await;
        if let Some(tx) = state.streams.read().await.get(&p.id).cloned() {
            let _ = tx.send(TaskEvent::Status(cancel_ev));
        }

        super::push::notify_state_change(state, &p.id, &snapshot, &canceled_status).await;
    }

    Ok(serde_json::json!({ "id": p.id, "canceled": true }))
}

// ---------------------------------------------------------------------------
// Tests.
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parse_error_code() {
        let e = JsonRpcError::parse_error("oops");
        assert_eq!(e.code, -32700);
    }

    #[test]
    fn auth_required_code_is_minus_32001() {
        let e = JsonRpcError::auth_required();
        assert_eq!(e.code, -32001);
        assert!(e.message.contains("authentication"));
    }

    #[test]
    fn response_ok_serializes_without_error_field() {
        let r = JsonRpcResponse::ok(Value::from(1), Value::from("yay"));
        let j = serde_json::to_value(&r).unwrap();
        assert_eq!(j["jsonrpc"], "2.0");
        assert_eq!(j["result"], "yay");
        assert!(j.get("error").is_none());
    }

    #[test]
    fn response_err_serializes_without_result_field() {
        let r = JsonRpcResponse::err(Value::from(2), JsonRpcError::internal("oops"));
        let j = serde_json::to_value(&r).unwrap();
        assert_eq!(j["error"]["code"], -32603);
        assert!(j.get("result").is_none());
    }

    #[test]
    fn request_roundtrip() {
        let j = serde_json::json!({
            "jsonrpc": "2.0",
            "method": "tasks/send",
            "params": { "id": "t1", "skill": "echo" },
            "id": 7
        });
        let r: JsonRpcRequest = serde_json::from_value(j).unwrap();
        assert_eq!(r.method, "tasks/send");
        assert_eq!(r.id, Value::from(7));
    }
}
