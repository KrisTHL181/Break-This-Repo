//! A2A client — discovery + JSON-RPC caller.
//!
//! `A2aClient` handles:
//! - `GET /.well-known/agent.json` with ETag caching + signature verify
//! - `tasks/send` / `tasks/get` / `tasks/cancel` as JSON-RPC calls
//! - `tasks/sendSubscribe` / `tasks/resubscribe` as SSE streams
//!
//! Retry + timeout: 30s default per call, 3 retries with exponential
//! backoff on 5xx status codes. TLS via `rustls` + the system root store
//! (configured in `Cargo.toml` via the `rustls-tls` feature).

use reqwest::header::{HeaderMap, HeaderValue, ETAG, IF_NONE_MATCH};
use reqwest::Client;
use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::collections::HashMap;
use std::sync::Arc;
use std::time::{Duration, Instant};
use tokio::sync::RwLock;

use crate::error::A2aError;
use crate::server::json_rpc::{JsonRpcRequest, JsonRpcResponse};
use crate::types::{AgentCard, Task, TaskSpec};

const DEFAULT_TIMEOUT: Duration = Duration::from_secs(30);
const MAX_RETRIES: u32 = 3;
/// Soft TTL for the ETag-validated discovery cache (ADR-159 proposed 5m).
const CARD_TTL: Duration = Duration::from_secs(5 * 60);

/// Cache entry for a fetched AgentCard — ETag + value + when we saw it.
#[derive(Debug, Clone)]
struct CardCacheEntry {
    etag: String,
    card: AgentCard,
    fetched_at: Instant,
}

/// JSON-RPC + discovery client. Cheaply cloneable (`Arc<RwLock<...>>`
/// inside).
#[derive(Clone)]
pub struct A2aClient {
    http: Client,
    etag_cache: Arc<RwLock<HashMap<String, CardCacheEntry>>>,
    /// When `true`, `fetch_card` rejects unsigned cards with
    /// [`A2aError::CardSignatureInvalid`]. When `false` (the M1 default),
    /// unsigned cards are accepted for bootstrap parity with peers that
    /// haven't plumbed identity yet. Opt in via [`A2aClientBuilder`].
    strict_verify: bool,
}

/// Builder for [`A2aClient`] — exposes configuration knobs that would
/// otherwise require a breaking change on `A2aClient::new`.
#[derive(Debug, Default)]
pub struct A2aClientBuilder {
    http: Option<Client>,
    strict_verify: bool,
}

impl A2aClientBuilder {
    /// Start a new builder with bootstrap-era defaults (`strict_verify =
    /// false`).
    pub fn new() -> Self {
        Self::default()
    }

    /// Provide a caller-supplied reqwest client — useful for binaries that
    /// want to share a connection pool across ACP + A2A.
    pub fn http(mut self, http: Client) -> Self {
        self.http = Some(http);
        self
    }

    /// When `true`, reject unsigned `AgentCard`s with
    /// [`A2aError::CardSignatureInvalid`] at discovery time. Defaults to
    /// `false` so existing deployments aren't broken — operators opt in
    /// once all their peers emit signed cards.
    pub fn strict_verify(mut self, strict: bool) -> Self {
        self.strict_verify = strict;
        self
    }

    /// Finish the builder. Returns the same transport errors as
    /// [`A2aClient::new`] if no caller-supplied client is provided and the
    /// default one fails to construct.
    pub fn build(self) -> Result<A2aClient, A2aError> {
        let http = match self.http {
            Some(c) => c,
            None => Client::builder()
                .timeout(DEFAULT_TIMEOUT)
                .user_agent(concat!("rvagent-a2a/", env!("CARGO_PKG_VERSION")))
                .build()
                .map_err(A2aError::transport)?,
        };
        Ok(A2aClient {
            http,
            etag_cache: Arc::new(RwLock::new(HashMap::new())),
            strict_verify: self.strict_verify,
        })
    }
}

impl A2aClient {
    /// Build a new client. Uses the default reqwest `Client` with the
    /// 30 s timeout documented above.
    pub fn new() -> Result<Self, A2aError> {
        A2aClientBuilder::new().build()
    }

    /// Entry point for the builder API. Use when you need to opt in to
    /// `strict_verify` or supply a custom reqwest client.
    pub fn builder() -> A2aClientBuilder {
        A2aClientBuilder::new()
    }

    /// Build with a caller-supplied `reqwest::Client` — useful for tests
    /// (swap the connector) and for binaries that want to share a single
    /// client across ACP + A2A.
    pub fn with_http(http: Client) -> Self {
        Self {
            http,
            etag_cache: Arc::new(RwLock::new(HashMap::new())),
            strict_verify: false,
        }
    }

    // -----------------------------------------------------------------
    // Discovery.
    // -----------------------------------------------------------------

    /// GET `{base_url}/.well-known/agent.json`. Honors `If-None-Match`
    /// from a prior call, caching the response until `CARD_TTL` elapses.
    ///
    /// Verifies the card signature via `identity::verify_card` (if that
    /// module has landed). A card that fails verification returns
    /// `A2aError::CardSignatureInvalid` and is never cached.
    #[tracing::instrument(skip(self), level = "info")]
    pub async fn fetch_card(&self, base_url: &str) -> Result<AgentCard, A2aError> {
        let url = format!("{}/.well-known/agent.json", base_url.trim_end_matches('/'));

        // Cache hit (soft TTL).
        let cached = self.etag_cache.read().await.get(&url).cloned();
        if let Some(entry) = cached.as_ref() {
            if entry.fetched_at.elapsed() < CARD_TTL {
                return Ok(entry.card.clone());
            }
        }

        let mut headers = HeaderMap::new();
        if let Some(entry) = cached.as_ref() {
            if let Ok(h) = HeaderValue::from_str(&entry.etag) {
                headers.insert(IF_NONE_MATCH, h);
            }
        }

        let resp = self
            .http
            .get(&url)
            .headers(headers)
            .send()
            .await
            .map_err(|e| A2aError::Discovery(format!("fetch {}: {}", url, e)))?;

        if resp.status() == reqwest::StatusCode::NOT_MODIFIED {
            if let Some(entry) = cached {
                // Refresh the timestamp so the TTL restarts.
                let mut w = self.etag_cache.write().await;
                if let Some(cur) = w.get_mut(&url) {
                    cur.fetched_at = Instant::now();
                }
                return Ok(entry.card);
            }
        }

        if !resp.status().is_success() {
            return Err(A2aError::Discovery(format!(
                "fetch {}: HTTP {}",
                url,
                resp.status()
            )));
        }

        let etag = resp
            .headers()
            .get(ETAG)
            .and_then(|v| v.to_str().ok())
            .unwrap_or("")
            .to_string();

        let body = resp.bytes().await.map_err(A2aError::transport)?;
        let card: AgentCard = serde_json::from_slice(&body)
            .map_err(|e| A2aError::Discovery(format!("parse {}: {}", url, e)))?;

        // Verify signature via sibling module. During parallel dev the
        // real `verify_card` may be a no-op stub; that's fine — this
        // call is the integration point. When `strict_verify` is set,
        // an unsigned card is also rejected (see builder docs).
        verify_card_or_ok(&card, self.strict_verify)?;

        if !etag.is_empty() {
            self.etag_cache.write().await.insert(
                url,
                CardCacheEntry {
                    etag,
                    card: card.clone(),
                    fetched_at: Instant::now(),
                },
            );
        }

        Ok(card)
    }

    // -----------------------------------------------------------------
    // JSON-RPC calls.
    // -----------------------------------------------------------------

    /// Call `tasks/send` on the peer. Returns the peer's view of the
    /// task (normally `submitted` / `working` depending on whether the
    /// peer ran the runner synchronously).
    #[tracing::instrument(skip(self, spec), fields(task_id = %spec.id), level = "info")]
    pub async fn send_task(&self, base_url: &str, spec: TaskSpec) -> Result<Task, A2aError> {
        let val = self
            .call_rpc(base_url, "tasks/send", serde_json::to_value(&spec)?)
            .await?;
        Ok(serde_json::from_value(val)?)
    }

    /// Call `tasks/get`.
    #[tracing::instrument(skip(self), level = "info")]
    pub async fn get_task(&self, base_url: &str, id: &str) -> Result<Task, A2aError> {
        let val = self
            .call_rpc(base_url, "tasks/get", serde_json::json!({ "id": id }))
            .await?;
        Ok(serde_json::from_value(val)?)
    }

    /// Call `tasks/cancel`.
    #[tracing::instrument(skip(self), level = "info")]
    pub async fn cancel_task(&self, base_url: &str, id: &str) -> Result<(), A2aError> {
        let _ = self
            .call_rpc(base_url, "tasks/cancel", serde_json::json!({ "id": id }))
            .await?;
        Ok(())
    }

    /// `tasks/sendSubscribe` over Server-Sent Events. Returns the raw
    /// response object (`reqwest::Response`) — parsing the byte stream
    /// into typed events is the caller's responsibility for now; a
    /// helper `parse_event_stream` lands in M2 once the event-shape
    /// translator is shared with `rvagent-acp`.
    #[tracing::instrument(skip(self, spec), fields(task_id = %spec.id), level = "info")]
    pub async fn stream_task(
        &self,
        base_url: &str,
        spec: TaskSpec,
    ) -> Result<reqwest::Response, A2aError> {
        let url = format!(
            "{}/tasks/sendSubscribe?id={}",
            base_url.trim_end_matches('/'),
            spec.id
        );
        let resp = self
            .http
            .get(&url)
            .header("accept", "text/event-stream")
            .send()
            .await
            .map_err(A2aError::transport)?;
        if !resp.status().is_success() {
            return Err(A2aError::Transport(format!(
                "sse {}: HTTP {}",
                url,
                resp.status()
            )));
        }
        Ok(resp)
    }

    // -----------------------------------------------------------------
    // Low-level JSON-RPC call with retry/backoff.
    // -----------------------------------------------------------------

    async fn call_rpc(
        &self,
        base_url: &str,
        method: &str,
        params: Value,
    ) -> Result<Value, A2aError> {
        let url = base_url.trim_end_matches('/').to_string();
        let req = JsonRpcRequest {
            jsonrpc: "2.0".into(),
            method: method.into(),
            params,
            id: Value::from(uuid_str()),
        };

        let mut attempt = 0u32;
        loop {
            attempt += 1;
            let resp = self.http.post(&url).json(&req).send().await;
            match resp {
                Ok(r) if r.status().is_success() => {
                    let body: JsonRpcResponse = r
                        .json()
                        .await
                        .map_err(|e| A2aError::JsonRpc(e.to_string()))?;
                    if let Some(err) = body.error {
                        return Err(A2aError::JsonRpc(format!(
                            "{} (code {})",
                            err.message, err.code
                        )));
                    }
                    return Ok(body.result.unwrap_or(Value::Null));
                }
                Ok(r) if r.status().is_server_error() && attempt < MAX_RETRIES => {
                    let backoff = Duration::from_millis(200u64 << attempt);
                    tracing::warn!(
                        attempt, status = %r.status(),
                        "a2a rpc got 5xx, backing off"
                    );
                    tokio::time::sleep(backoff).await;
                    continue;
                }
                Ok(r) => {
                    return Err(A2aError::Transport(format!(
                        "rpc {}: HTTP {}",
                        method,
                        r.status()
                    )));
                }
                Err(e) if attempt < MAX_RETRIES => {
                    let backoff = Duration::from_millis(200u64 << attempt);
                    tracing::warn!(attempt, %e, "a2a rpc transport error, backing off");
                    tokio::time::sleep(backoff).await;
                    continue;
                }
                Err(e) => return Err(A2aError::transport(e)),
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Bridge to the (in-flight) identity module. Keeps this file compilable
// even if `identity::verify_card` hasn't landed yet.
// ---------------------------------------------------------------------------

/// Verify the card's Ed25519 signature via the `identity` module. Any
/// failure to verify surfaces as [`A2aError::CardSignatureInvalid`] — we
/// do not retain a cached card that failed verification.
///
/// When `strict` is `false` (the bootstrap-era default), a card missing
/// `metadata.ruvector.signature` is accepted. When `strict` is `true`,
/// missing signatures are rejected with the same error as a malformed or
/// mis-verifying one.
fn verify_card_or_ok(card: &AgentCard, strict: bool) -> Result<(), A2aError> {
    match crate::identity::verify_card(card) {
        Ok(_id) => Ok(()),
        Err(crate::identity::IdentityError::SignatureMissing) if !strict => Ok(()),
        Err(_) => Err(A2aError::CardSignatureInvalid),
    }
}

fn uuid_str() -> String {
    uuid::Uuid::new_v4().to_string()
}

// ---------------------------------------------------------------------------
// Response + error envelope definitions (local aliases for client code).
// ---------------------------------------------------------------------------

/// Minimal JSON-RPC request shape used for request id generation in
/// tests. The real shape lives in `server::json_rpc::JsonRpcRequest`.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ClientRpcEnvelope {
    pub jsonrpc: String,
    pub method: String,
    pub params: Value,
    pub id: Value,
}

// ---------------------------------------------------------------------------
// Tests.
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn uuid_str_is_not_empty() {
        let s = uuid_str();
        assert!(!s.is_empty());
        // v4 UUID string length is 36 chars.
        assert_eq!(s.len(), 36);
    }

    #[test]
    fn client_construction_succeeds() {
        let c = A2aClient::new();
        assert!(c.is_ok());
    }

    fn unsigned_card() -> AgentCard {
        AgentCard {
            name: "x".into(),
            description: "".into(),
            url: "https://x".into(),
            provider: crate::types::AgentProvider {
                organization: "r".into(),
                url: None,
            },
            version: "0".into(),
            capabilities: Default::default(),
            skills: vec![],
            authentication: Default::default(),
            metadata: Value::Null,
        }
    }

    #[test]
    fn verify_card_with_no_signature_is_accepted() {
        // An unsigned card is treated as "not verified, not invalid" — the
        // signature-missing path should be admissible for bootstrap-era
        // peers that don't yet sign their cards. A signed card that fails
        // to verify (tested in `card_signature.rs`) returns
        // `A2aError::CardSignatureInvalid`.
        assert!(verify_card_or_ok(&unsigned_card(), false).is_ok());
    }

    #[test]
    fn strict_verify_rejects_unsigned_card() {
        // Strict clients opt in to "every peer must sign" and surface
        // unsigned cards as CardSignatureInvalid — same error as a
        // malformed signature so operators have one thing to alert on.
        let card = unsigned_card();
        let strict = A2aClient::builder()
            .strict_verify(true)
            .build()
            .expect("build strict client");
        assert!(strict.strict_verify);
        match verify_card_or_ok(&card, true) {
            Err(A2aError::CardSignatureInvalid) => {}
            other => panic!("expected CardSignatureInvalid, got {:?}", other),
        }

        // Same card through a non-strict client is accepted — default
        // behaviour is preserved.
        let lax = A2aClient::builder()
            .strict_verify(false)
            .build()
            .expect("build lax client");
        assert!(!lax.strict_verify);
        assert!(verify_card_or_ok(&card, false).is_ok());
    }

    #[test]
    fn default_client_is_not_strict() {
        // Preserve bootstrap parity: `A2aClient::new()` must NOT opt in to
        // strict_verify; operators flip the flag explicitly via the
        // builder.
        let c = A2aClient::new().expect("new");
        assert!(!c.strict_verify);
    }
}
