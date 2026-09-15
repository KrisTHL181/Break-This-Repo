//! Push-notification webhooks with HMAC-SHA256 signing (ADR-159 M4).
//!
//! Flow:
//!
//! 1. A receiver registers a webhook for a task via
//!    `tasks/pushNotification/set`.
//! 2. Whenever the task transitions state (and in particular on the
//!    terminal transition), the server POSTs
//!    `{ "taskId": ..., "status": ..., "finalState": bool }` to that
//!    URL with header `X-A2A-Signature: sha256=<hex>` where the body
//!    is signed with `HMAC-SHA256(token, body)`.
//! 3. Retry: 3 attempts total, exponential backoff (100ms / 400ms /
//!    1600ms) on 5xx + network errors. 4xx is terminal (receiver
//!    explicitly rejected the payload) and surfaces at `tracing::warn!`.
//! 4. Receivers use [`verify_push_signature`] to verify the HMAC.
//!
//! The `ed25519-webhooks` cargo feature wires an additional Ed25519
//! signature alongside the HMAC (header `X-A2A-Signature-Ed25519`,
//! base64-encoded). When the feature is on, the server mints a
//! keypair at startup, advertises the public half in the AgentCard
//! (`metadata.ruvector.webhook_ed25519_pubkey`), and emits both
//! signature headers on every dispatch. Receivers verify via
//! [`verify_push_signature_ed25519`].

use std::collections::{HashMap, HashSet};
use std::sync::Arc;
use std::time::Duration;

use hmac::{Hmac, Mac};
use serde::{Deserialize, Serialize};
use serde_json::Value;
use sha2::Sha256;
use subtle::ConstantTimeEq;
use thiserror::Error;
use tokio::sync::{Mutex, RwLock};

use super::A2aState;
use crate::error::A2aError;
use crate::types::{AuthScheme, Task, TaskStatus};

type HmacSha256 = Hmac<Sha256>;

// ---------------------------------------------------------------------------
// Config + Registry.
// ---------------------------------------------------------------------------

/// One webhook registration per task. Matches the A2A spec shape:
/// receiver publishes a `url` + optional shared `token` + optional
/// further authentication hints (OAuth bearer, API key — downstream
/// middleware honors these on the receiver side).
#[derive(Clone, Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PushNotificationConfig {
    /// Webhook URL. Must be https:// in production; http:// is only
    /// accepted when the host is `localhost` / `127.0.0.1` / `::1`
    /// (tests + dev loopback). Rejection surfaces as
    /// `A2aError::Internal("insecure push URL")`.
    pub url: url::Url,

    /// Operator-generated shared secret. Used as the HMAC-SHA256 key
    /// when signing the outbound body. If absent, delivery proceeds
    /// unsigned and the server emits a one-shot WARN — receivers that
    /// care about authenticity should always set a token.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub token: Option<String>,

    /// Optional receiver-specified authentication envelope
    /// (e.g. bearer schemes). Stored verbatim and returned on `get`.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub authentication: Option<AuthScheme>,

    /// Optional algorithm preference. Current impl signs with
    /// `hmac-sha256` regardless; `ed25519` lands under the
    /// `ed25519-webhooks` cargo feature.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub algorithm: Option<String>,
}

/// Server-side in-memory registry of push configs, keyed by task id.
/// Shared via `Arc` so the JSON-RPC handlers + the state-change
/// dispatcher both read the same map.
#[derive(Default)]
pub struct PushNotificationRegistry {
    inner: RwLock<HashMap<String, PushNotificationConfig>>,
    /// One-shot "token missing" WARN flag, keyed by URL so we don't
    /// spam the operator's logs every single dispatch.
    warned_untokened: Mutex<HashSet<String>>,
}

impl PushNotificationRegistry {
    pub fn new() -> Arc<Self> {
        Arc::new(Self::default())
    }

    /// Insert (overwriting) a webhook config for `task_id`.
    pub async fn set(&self, task_id: &str, cfg: PushNotificationConfig) {
        self.inner.write().await.insert(task_id.to_string(), cfg);
    }

    /// Fetch the config for `task_id`, if any.
    pub async fn get(&self, task_id: &str) -> Option<PushNotificationConfig> {
        self.inner.read().await.get(task_id).cloned()
    }

    /// Remove `task_id`'s config. Called after the terminal event is
    /// dispatched to keep the map bounded.
    pub async fn remove(&self, task_id: &str) -> Option<PushNotificationConfig> {
        self.inner.write().await.remove(task_id)
    }

    /// List configs matching `task_id`. In this in-memory impl that's
    /// at most 1, but the signature admits future multi-subscriber
    /// registries (e.g. a fan-out webhook sink).
    pub async fn list_by_task(&self, task_id: &str) -> Vec<PushNotificationConfig> {
        self.inner
            .read()
            .await
            .get(task_id)
            .cloned()
            .into_iter()
            .collect()
    }
}

/// Back-compat alias so M1 callers referring to `PushRegistry` keep
/// compiling. New code should use `PushNotificationRegistry`.
pub type PushRegistry = PushNotificationRegistry;

// ---------------------------------------------------------------------------
// Errors.
// ---------------------------------------------------------------------------

/// Errors specific to push-notification delivery. Promoted to
/// `A2aError::Internal(..)` at the JSON-RPC boundary so operator logs
/// keep the detailed variant while clients see a stable code.
#[derive(Debug, Error)]
pub enum PushError {
    /// Receiver URL failed validation (e.g. plain `http://` to a
    /// non-loopback host).
    #[error("push URL rejected: {url} ({reason})")]
    UrlRejected { url: String, reason: &'static str },

    /// No push-notification config registered for the given task id.
    /// Surfaced via the custom JSON-RPC code `-32002`.
    #[error("no push config registered")]
    ConfigMissing,

    /// HMAC key material is unusable (zero-length token, etc.).
    #[error("hmac misconfigured")]
    HmacMisconfigured,

    /// All retry attempts were exhausted.
    #[error("push delivery failed after {attempts} attempt(s), last status = {status:?}")]
    DeliveryFailed { attempts: u32, status: Option<u16> },

    /// Transport-level failure (DNS, connect, TLS, timeout). String
    /// form because the underlying `reqwest::Error` isn't `Clone`.
    #[error("push transport: {0}")]
    Transport(String),
}

// ---------------------------------------------------------------------------
// JSON-RPC handlers (tasks/pushNotification/set + get).
// ---------------------------------------------------------------------------

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
struct SetParams {
    /// Task id whose status changes should fan out. The A2A spec uses
    /// `id` in some revisions and `taskId` in others — accept both.
    #[serde(alias = "taskId")]
    id: String,
    push_notification_config: PushNotificationConfig,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
struct GetParams {
    #[serde(alias = "taskId")]
    id: String,
}

/// Validate that a webhook URL is safe to use. https is always OK;
/// http is only OK on localhost / 127.0.0.1 / ::1.
fn validate_url(url: &url::Url) -> Result<(), PushError> {
    match url.scheme() {
        "https" => Ok(()),
        "http" => {
            // `url::Url::host_str()` returns the bracketed form for
            // IPv6 hosts (`[::1]`) — match on both shapes.
            let host = url.host_str().unwrap_or("");
            if host == "localhost" || host == "127.0.0.1" || host == "::1" || host == "[::1]" {
                Ok(())
            } else {
                Err(PushError::UrlRejected {
                    url: url.to_string(),
                    reason: "plain http to non-loopback host",
                })
            }
        }
        _ => Err(PushError::UrlRejected {
            url: url.to_string(),
            reason: "unsupported URL scheme",
        }),
    }
}

#[tracing::instrument(skip(state), level = "debug")]
pub async fn handle_set(state: &A2aState, params: Value) -> Result<Value, A2aError> {
    let p: SetParams = serde_json::from_value(params)
        .map_err(|e| A2aError::JsonRpc(format!("pushNotification/set: {}", e)))?;

    validate_url(&p.push_notification_config.url).map_err(|e| match e {
        PushError::UrlRejected { .. } => A2aError::Internal("insecure push URL".into()),
        _ => A2aError::Internal(format!("{}", e)),
    })?;

    state
        .push_registry
        .set(&p.id, p.push_notification_config.clone())
        .await;

    tracing::info!(task = %p.id, url = %p.push_notification_config.url,
        "push notification registered");

    Ok(serde_json::json!({
        "taskId": p.id,
        "pushNotificationConfig": p.push_notification_config,
    }))
}

#[tracing::instrument(skip(state), level = "debug")]
pub async fn handle_get(state: &A2aState, params: Value) -> Result<Value, A2aError> {
    let p: GetParams = serde_json::from_value(params)
        .map_err(|e| A2aError::JsonRpc(format!("pushNotification/get: {}", e)))?;

    let cfg = state
        .push_registry
        .get(&p.id)
        .await
        .ok_or_else(|| A2aError::Internal("no push config registered".into()))?;

    Ok(serde_json::json!({
        "taskId": p.id,
        "pushNotificationConfig": cfg,
    }))
}

// ---------------------------------------------------------------------------
// Dispatch + retry.
// ---------------------------------------------------------------------------

/// Body we POST to the receiver. Kept intentionally minimal — the full
/// task + artifact payload is available via the regular `tasks/get`
/// call using the embedded `task_id`.
#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
struct WebhookPayload<'a> {
    task_id: &'a str,
    status: &'a TaskStatus,
    /// Whether `status.state` is terminal — receivers can close their
    /// stream bookkeeping without consulting the A2A state machine.
    final_state: bool,
}

/// Maximum attempts per dispatch (initial + 2 retries = 3 total).
const MAX_ATTEMPTS: u32 = 3;
/// Base backoff delay; the nth retry sleeps `BASE * 4^(n-1)`
/// (100ms → 400ms → 1600ms between attempts 1→2, 2→3).
const BASE_BACKOFF: Duration = Duration::from_millis(100);

/// Look up the webhook config for `task_id`, build + sign the payload,
/// and deliver it with retry. Always best-effort — errors surface via
/// tracing + the returned `Result`, not as user-facing failures.
///
/// When the `ed25519-webhooks` feature is on and the caller supplies a
/// signing key via [`A2aState::ed25519_signing_key`], every outbound
/// request gets an additional `X-A2A-Signature-Ed25519: <base64(sig)>`
/// header alongside the HMAC. The signature covers the raw body bytes.
#[cfg(not(feature = "ed25519-webhooks"))]
#[tracing::instrument(
    skip(registry, client, _task, status),
    fields(task = %task_id),
    level = "debug"
)]
pub async fn dispatch_webhook(
    registry: &PushNotificationRegistry,
    client: &reqwest::Client,
    task_id: &str,
    _task: &Task,
    status: &TaskStatus,
) -> Result<(), PushError> {
    deliver_webhook_impl(registry, client, task_id, status).await
}

/// Feature-gated twin of `dispatch_webhook` that additionally emits the
/// `X-A2A-Signature-Ed25519` header when a signing key is supplied.
#[cfg(feature = "ed25519-webhooks")]
#[tracing::instrument(
    skip(registry, client, _task, status, ed25519_key),
    fields(task = %task_id),
    level = "debug"
)]
pub async fn dispatch_webhook(
    registry: &PushNotificationRegistry,
    client: &reqwest::Client,
    task_id: &str,
    _task: &Task,
    status: &TaskStatus,
    ed25519_key: Option<&ed25519_dalek::SigningKey>,
) -> Result<(), PushError> {
    deliver_webhook_impl(registry, client, task_id, status, ed25519_key).await
}

/// Shared retry/dispatch core. Split off the public entry points so we
/// can `#[cfg]`-gate a single extra parameter without running into
/// Rust's "no attributes on fn parameters" restriction.
async fn deliver_webhook_impl(
    registry: &PushNotificationRegistry,
    client: &reqwest::Client,
    task_id: &str,
    status: &TaskStatus,
    #[cfg(feature = "ed25519-webhooks")] ed25519_key: Option<&ed25519_dalek::SigningKey>,
) -> Result<(), PushError> {
    let Some(cfg) = registry.get(task_id).await else {
        return Err(PushError::ConfigMissing);
    };

    let payload = WebhookPayload {
        task_id,
        status,
        final_state: status.state.is_terminal(),
    };
    let body = serde_json::to_vec(&payload)
        .map_err(|e| PushError::Transport(format!("serialize payload: {}", e)))?;

    let signature_header = if let Some(token) = cfg.token.as_deref() {
        Some(sign_hmac_header(token, &body)?)
    } else {
        // One-shot per URL: complain about the missing token exactly
        // once so a misconfigured webhook is obvious without drowning
        // out other signal.
        let url_key = cfg.url.to_string();
        let mut warned = registry.warned_untokened.lock().await;
        if warned.insert(url_key.clone()) {
            tracing::warn!(url = %url_key,
                "push config has no token; HMAC signing disabled for this URL");
        }
        None
    };

    // Pre-compute the Ed25519 signature once per dispatch when the
    // feature is on and a key was supplied — the body is constant
    // across retries.
    #[cfg(feature = "ed25519-webhooks")]
    let ed25519_header = ed25519_key.map(|k| ed25519_sign_body(k, &body));

    let mut attempt: u32 = 0;
    let mut last_status: Option<u16> = None;
    while attempt < MAX_ATTEMPTS {
        attempt += 1;
        let mut req = client
            .post(cfg.url.clone())
            .header("content-type", "application/json")
            .body(body.clone());
        if let Some(sig) = signature_header.as_deref() {
            req = req.header("X-A2A-Signature", sig);
        }
        // Optional Ed25519 path. When the feature is on and the caller
        // supplied a key, sign the raw body bytes and emit the header
        // alongside the HMAC one. Both signatures coexist — receivers
        // MAY verify either.
        #[cfg(feature = "ed25519-webhooks")]
        if let Some(sig) = ed25519_header.as_deref() {
            req = req.header("X-A2A-Signature-Ed25519", sig);
        }

        match req.send().await {
            Ok(resp) => {
                let code = resp.status().as_u16();
                last_status = Some(code);
                if resp.status().is_success() {
                    tracing::debug!(task = %task_id, url = %cfg.url, attempt, code,
                        "push webhook delivered");
                    return Ok(());
                } else if (400..500).contains(&code) {
                    // 4xx — receiver-visible rejection. No retry.
                    tracing::warn!(task = %task_id, url = %cfg.url, code,
                        "push webhook rejected with 4xx; not retrying");
                    return Err(PushError::DeliveryFailed {
                        attempts: attempt,
                        status: Some(code),
                    });
                } else {
                    // 5xx or other — retry.
                    tracing::warn!(task = %task_id, url = %cfg.url, attempt, code,
                        "push webhook failed; will retry");
                }
            }
            Err(e) => {
                tracing::warn!(task = %task_id, url = %cfg.url, attempt, err = %e,
                    "push webhook transport error; will retry");
            }
        }

        if attempt < MAX_ATTEMPTS {
            let backoff = BASE_BACKOFF * 4u32.pow(attempt - 1);
            tokio::time::sleep(backoff).await;
        }
    }

    Err(PushError::DeliveryFailed {
        attempts: attempt,
        status: last_status,
    })
}

/// Fire-and-forget convenience wrapper. The server calls this on every
/// state transition; delivery runs inline (retries are bounded at 3 ×
/// 1.6s worst-case = ~2.1s total) so the ordering with subsequent SSE
/// events stays predictable for tests. Errors are logged, not returned.
pub(crate) async fn notify_state_change(
    state: &A2aState,
    task_id: &str,
    task: &Task,
    status: &TaskStatus,
) {
    let registry = state.push_registry.as_ref();
    let client = &state.push_client;

    #[cfg(feature = "ed25519-webhooks")]
    let result = {
        let key = state.ed25519_signing_key.as_ref().as_ref();
        dispatch_webhook(registry, client, task_id, task, status, key).await
    };
    #[cfg(not(feature = "ed25519-webhooks"))]
    let result = dispatch_webhook(registry, client, task_id, task, status).await;

    match result {
        Ok(_) => {}
        Err(PushError::ConfigMissing) => {} // no hook registered — normal path
        Err(e) => tracing::warn!(task = %task_id, err = %e, "push notify failed"),
    }
    // GC the registry when the task reaches a terminal state so repeated
    // runs of the same task id don't accumulate stale rows.
    if status.state.is_terminal() {
        let _ = registry.remove(task_id).await;
    }
}

// ---------------------------------------------------------------------------
// HMAC signing + verification helpers.
// ---------------------------------------------------------------------------

/// Build the `sha256=<hex>` signature header value for `body`.
fn sign_hmac_header(token: &str, body: &[u8]) -> Result<String, PushError> {
    if token.is_empty() {
        return Err(PushError::HmacMisconfigured);
    }
    let mut mac = <HmacSha256 as Mac>::new_from_slice(token.as_bytes())
        .map_err(|_| PushError::HmacMisconfigured)?;
    mac.update(body);
    let bytes = mac.finalize().into_bytes();
    Ok(format!("sha256={}", hex::encode(bytes)))
}

/// Verify an `X-A2A-Signature` header against `body` under `token`.
/// Accepts the `sha256=<hex>` format (the only format this module
/// emits). Constant-time compare to avoid timing oracles.
///
/// Intended for downstream receivers written in Rust — import this
/// from any service that mounts a webhook endpoint.
pub fn verify_push_signature(
    body: &[u8],
    token: &str,
    signature_header: &str,
) -> Result<(), PushError> {
    if token.is_empty() {
        return Err(PushError::HmacMisconfigured);
    }
    let hex_part = signature_header
        .strip_prefix("sha256=")
        .ok_or(PushError::UrlRejected {
            url: String::new(),
            reason: "signature header missing sha256= prefix",
        })?;
    let provided = hex::decode(hex_part).map_err(|_| PushError::UrlRejected {
        url: String::new(),
        reason: "signature header hex decode failed",
    })?;

    let mut mac = <HmacSha256 as Mac>::new_from_slice(token.as_bytes())
        .map_err(|_| PushError::HmacMisconfigured)?;
    mac.update(body);
    let expected = mac.finalize().into_bytes();

    if provided.len() == expected.len() && provided.as_slice().ct_eq(expected.as_slice()).into() {
        Ok(())
    } else {
        Err(PushError::DeliveryFailed {
            attempts: 0,
            status: None,
        })
    }
}

// ---------------------------------------------------------------------------
// Ed25519 signing + verification (feature = "ed25519-webhooks").
// ---------------------------------------------------------------------------

/// Sign `body` with `key` and return the base64-encoded signature.
#[cfg(feature = "ed25519-webhooks")]
fn ed25519_sign_body(key: &ed25519_dalek::SigningKey, body: &[u8]) -> String {
    use base64::Engine;
    use ed25519_dalek::Signer;
    let sig = key.sign(body);
    base64::engine::general_purpose::STANDARD.encode(sig.to_bytes())
}

/// Verify an `X-A2A-Signature-Ed25519` header against `body` under the
/// webhook's advertised public key. `pubkey_b64` is the base64 value
/// from the sender's AgentCard `metadata.ruvector.webhook_ed25519_pubkey`;
/// `signature_header` is the raw header value (base64-encoded 64-byte
/// signature).
///
/// Intended for Rust-side receivers. Ed25519 verification is inherently
/// constant-time so no further side-channel handling is needed.
#[cfg(feature = "ed25519-webhooks")]
pub fn verify_push_signature_ed25519(
    body: &[u8],
    pubkey_b64: &str,
    signature_header: &str,
) -> Result<(), PushError> {
    use base64::Engine;
    use ed25519_dalek::{Signature, Verifier, VerifyingKey};

    let pubkey_bytes = base64::engine::general_purpose::STANDARD
        .decode(pubkey_b64.trim())
        .map_err(|_| PushError::UrlRejected {
            url: String::new(),
            reason: "ed25519 pubkey base64 decode failed",
        })?;
    let pubkey_arr: [u8; 32] =
        pubkey_bytes
            .as_slice()
            .try_into()
            .map_err(|_| PushError::UrlRejected {
                url: String::new(),
                reason: "ed25519 pubkey wrong length",
            })?;
    let vk = VerifyingKey::from_bytes(&pubkey_arr).map_err(|_| PushError::UrlRejected {
        url: String::new(),
        reason: "ed25519 pubkey malformed",
    })?;

    let sig_bytes = base64::engine::general_purpose::STANDARD
        .decode(signature_header.trim())
        .map_err(|_| PushError::UrlRejected {
            url: String::new(),
            reason: "ed25519 signature base64 decode failed",
        })?;
    let sig_arr: [u8; 64] =
        sig_bytes
            .as_slice()
            .try_into()
            .map_err(|_| PushError::UrlRejected {
                url: String::new(),
                reason: "ed25519 signature wrong length",
            })?;
    let signature = Signature::from_bytes(&sig_arr);

    vk.verify(body, &signature)
        .map_err(|_| PushError::DeliveryFailed {
            attempts: 0,
            status: None,
        })
}

// ---------------------------------------------------------------------------
// Tests.
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn validate_url_accepts_https() {
        let u = url::Url::parse("https://hooks.example.com/in").unwrap();
        validate_url(&u).expect("https should pass");
    }

    #[test]
    fn validate_url_accepts_http_localhost() {
        for s in [
            "http://127.0.0.1:8080/hook",
            "http://localhost/hook",
            "http://[::1]/hook",
        ] {
            let u = url::Url::parse(s).unwrap();
            validate_url(&u).expect(s);
        }
    }

    #[test]
    fn validate_url_rejects_plain_http_public() {
        let u = url::Url::parse("http://hooks.example.com/").unwrap();
        assert!(matches!(
            validate_url(&u),
            Err(PushError::UrlRejected { .. })
        ));
    }

    #[test]
    fn hmac_sign_and_verify_roundtrip() {
        let token = "s3cret";
        let body = b"{\"taskId\":\"t1\"}";
        let header = sign_hmac_header(token, body).expect("sign");
        assert!(header.starts_with("sha256="));
        verify_push_signature(body, token, &header).expect("verify");
    }

    #[test]
    fn verify_rejects_tampered_body() {
        let token = "s3cret";
        let header = sign_hmac_header(token, b"hello").expect("sign");
        assert!(verify_push_signature(b"hell0", token, &header).is_err());
    }

    #[test]
    fn verify_rejects_missing_prefix() {
        let token = "s3cret";
        let header = sign_hmac_header(token, b"x").unwrap();
        let stripped = header.trim_start_matches("sha256=");
        assert!(verify_push_signature(b"x", token, stripped).is_err());
    }

    #[tokio::test]
    async fn registry_set_get_remove() {
        let r = PushNotificationRegistry::default();
        let cfg = PushNotificationConfig {
            url: url::Url::parse("https://ex.com/").unwrap(),
            token: Some("t".into()),
            authentication: None,
            algorithm: None,
        };
        r.set("t1", cfg.clone()).await;
        assert!(r.get("t1").await.is_some());
        assert_eq!(r.list_by_task("t1").await.len(), 1);
        r.remove("t1").await;
        assert!(r.get("t1").await.is_none());
    }
}
