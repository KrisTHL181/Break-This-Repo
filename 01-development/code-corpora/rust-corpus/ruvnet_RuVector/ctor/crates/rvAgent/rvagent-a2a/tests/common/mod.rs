//! Shared helpers for the M2 SSE acceptance tests.
//!
//! Each integration test file gets its own crate, so this module lives
//! behind `tests/common/mod.rs` — the one pattern cargo treats as a shared
//! helper rather than a separate test binary. Files that need it do
//! `#[path = "common/mod.rs"] mod common;`.
//!
//! What's here:
//!
//! - `signed_card` — produces a fresh signed `AgentCard` so the server's
//!   discovery endpoint is exercised end-to-end.
//! - `spec` — minimal `TaskSpec` constructor with a root `TaskContext`.
//! - `parse_sse_frames` — splits a raw `text/event-stream` byte buffer
//!   into `(event, data)` pairs.
//! - `mount_sse_routes` — merges the `/tasks/sendSubscribe` and
//!   `/tasks/resubscribe` GET handlers onto the server's router. As of
//!   this writing `server::A2aServer::router()` doesn't mount them; the
//!   M4-branch peer is doing that. Until then, tests mount locally.
//! - `StreamsMap` — the concrete type of `A2aState::streams`, exposed so
//!   custom `TaskRunner`s can emit intermediate events on the same
//!   broadcast channel the SSE handler subscribes to.

#![allow(dead_code)]

use std::collections::HashMap;
use std::sync::Arc;

use axum::Router;
use ed25519_dalek::SigningKey;
use serde_json::json;
use tokio::sync::{broadcast, RwLock};

use rvagent_a2a::context::TaskContext;
use rvagent_a2a::identity::{sign_card, AgentID};
use rvagent_a2a::server::{A2aState, TaskEvent};
use rvagent_a2a::types::{
    AgentCapabilities, AgentCard, AgentProvider, AgentSkill, AuthScheme, Message, Part, Role,
    TaskSpec,
};

/// Type alias matching `A2aState::streams`. Tests use this to plumb the
/// server's broadcast-sender map into a custom `TaskRunner`.
pub type StreamsMap = Arc<RwLock<HashMap<String, broadcast::Sender<TaskEvent>>>>;

/// Build a freshly signed `AgentCard` for a server rooted at `base_url`.
/// The signature lives at `metadata.ruvector.signature`, matching the
/// fixture pattern in `executor_remote.rs`.
pub fn signed_card(sk: &SigningKey, base_url: &str, fixture_name: &str) -> AgentCard {
    let card = AgentCard {
        name: fixture_name.into(),
        description: "sse M2 acceptance fixture".into(),
        url: base_url.to_string(),
        provider: AgentProvider {
            organization: "ruvector".into(),
            url: None,
        },
        version: "0.1.0".into(),
        capabilities: AgentCapabilities {
            streaming: true,
            push_notifications: false,
        },
        skills: vec![AgentSkill {
            id: "echo".into(),
            name: "echo".into(),
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
    let mut v = serde_json::to_value(&card).unwrap();
    let ruvector = v
        .get_mut("metadata")
        .and_then(|m| m.as_object_mut())
        .unwrap()
        .entry("ruvector")
        .or_insert_with(|| json!({}))
        .as_object_mut()
        .unwrap();
    ruvector.insert("signature".into(), serde_json::to_value(&sig).unwrap());
    serde_json::from_value(v).expect("re-parse signed card")
}

/// A small `TaskSpec` rooted at `root` — "ping" user-text, `echo` skill.
pub fn spec(id: &str, root: AgentID) -> TaskSpec {
    TaskSpec {
        id: id.into(),
        skill: "echo".into(),
        message: Message {
            role: Role::User,
            parts: vec![Part::Text {
                text: "ping".into(),
            }],
            metadata: serde_json::Value::Null,
        },
        policy: None,
        context: TaskContext::new_root(root),
        metadata: serde_json::Value::Null,
    }
}

/// Split a raw `text/event-stream` byte buffer into `(event, data)` pairs.
/// Frames are delimited by a blank line — matches the shape `axum::sse::Event`
/// emits.
pub fn parse_sse_frames(raw: &[u8]) -> Vec<(String, String)> {
    let text = String::from_utf8_lossy(raw).to_string();
    let mut out = Vec::new();
    for frame in text.split("\n\n") {
        let mut name = String::new();
        let mut data = String::new();
        for line in frame.split('\n') {
            if let Some(v) = line.strip_prefix("event:") {
                name = v.trim().to_string();
            } else if let Some(v) = line.strip_prefix("data:") {
                if !data.is_empty() {
                    data.push('\n');
                }
                data.push_str(v.trim_start());
            }
        }
        if !name.is_empty() && !data.is_empty() {
            out.push((name, data));
        }
    }
    out
}

/// Historically merged the SSE GET routes onto the test router while
/// `A2aServer::router()` didn't mount them. As of ADR-159 M2 final the
/// production router (`server/mod.rs`) mounts `/tasks/sendSubscribe` and
/// `/tasks/resubscribe` itself, so this helper is now a no-op kept only
/// for import compatibility with the test files that still call it.
/// axum's `Router::merge` panics on duplicate routes, so we cannot
/// re-mount here.
pub fn mount_sse_routes(base: Router, _state: A2aState) -> Router {
    // Routes already mounted by `A2aServer::router()`.
    base
}
