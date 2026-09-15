//! Wires remote AI chat requests (from the iOS app via kaku-remote) to the
//! local `AiClient`. Runs each chat on a blocking std::thread because
//! `AiClient` uses `reqwest::blocking`.
//!
//! MVP scope: plain streaming chat with optional pane-buffer context.
//! Tool calling / agentic loops are intentionally left out here so we do not
//! need to expose the overlay approval UI to remote clients. Conversation
//! history is also not preserved server-side; the iOS client is responsible
//! for sending the full turn each request. If a `conversation_id` appears
//! twice, the second call starts fresh.

use crate::ai_client::{AiClient, ApiMessage, AssistantConfig};
use kaku_remote::{set_ai_handler, AiEvent, AiHandler, AiRequest};
use mux::pane::CachePolicy;
use mux::Mux;
use std::sync::atomic::{AtomicUsize, Ordering};
use std::sync::Arc;

/// Registers the remote AI handler if `assistant.toml` is configured.
/// Safe to call at any point after kaku-remote is initialised. Returns early
/// and logs when no API key is set.
pub fn register_if_configured() {
    let config = match AssistantConfig::load() {
        Ok(c) => c,
        Err(err) => {
            log::info!(
                "kaku-remote AI: not registering remote AI handler ({}). \
                 Configure ~/.config/kaku/assistant.toml and restart.",
                err
            );
            return;
        }
    };
    let client = AiClient::new(config);
    let handler: AiHandler = Arc::new(move |req: AiRequest| {
        let client = client.clone();
        let in_flight = req.in_flight.clone();
        let tx = req.tx.clone();
        let conversation_id = req.conversation_id.clone();
        if let Err(err) = std::thread::Builder::new()
            .name("kaku-remote-ai".to_string())
            .spawn(move || run_chat(client, req))
        {
            // The dispatcher already incremented in_flight; release it here so a
            // spawn failure does not permanently burn a session slot.
            log::warn!("kaku-remote AI: failed to spawn handler thread: {err}");
            in_flight.fetch_sub(1, Ordering::AcqRel);
            let _ = tx.send(AiEvent::AiError {
                conversation_id,
                message: "internal_error".to_string(),
            });
        }
    });
    set_ai_handler(handler);
    log::info!("kaku-remote AI: handler registered");
}

/// RAII guard that decrements the session in-flight counter exactly once,
/// even on panic. The dispatcher incremented before handing us `req`.
struct InFlightGuard(Arc<AtomicUsize>);

impl Drop for InFlightGuard {
    fn drop(&mut self) {
        self.0.fetch_sub(1, Ordering::AcqRel);
    }
}

fn run_chat(client: AiClient, req: AiRequest) {
    let AiRequest {
        conversation_id,
        content,
        attach_pane,
        tx,
        cancel,
        in_flight,
    } = req;
    let _guard = InFlightGuard(in_flight);

    let mut messages: Vec<ApiMessage> = Vec::new();
    messages.push(ApiMessage::system(SYSTEM_PROMPT));
    if let Some(context) = attach_pane.and_then(capture_pane_context) {
        messages.push(ApiMessage::system(format!(
            "Terminal context:\n{}",
            context
        )));
    }
    messages.push(ApiMessage::user(&content));

    let model = client.config().chat_model.clone();
    let tx_tokens = tx.clone();
    let conv_tokens = conversation_id.clone();

    let result = client.chat_step(
        &model,
        &messages,
        &[],
        false,
        &cancel,
        &mut move |tok: &str| {
            let _ = tx_tokens.send(AiEvent::AiToken {
                conversation_id: conv_tokens.clone(),
                delta: tok.to_string(),
            });
        },
        &mut |_| {},
    );

    match result {
        Ok(_) => {
            let _ = tx.send(AiEvent::AiDone { conversation_id });
        }
        Err(err) => {
            // Full error to local log; short classified code to remote client.
            log::warn!("kaku-remote AI: chat failed: {err:#}");
            let _ = tx.send(AiEvent::AiError {
                conversation_id,
                message: classify_ai_error(&err).to_string(),
            });
        }
    }
}

/// Reduce an upstream error to a small set of stable codes so we do not leak
/// local paths, API response bodies, or key fragments to remote clients.
fn classify_ai_error(err: &anyhow::Error) -> &'static str {
    let msg = format!("{err:#}").to_lowercase();
    if msg.contains("401")
        || msg.contains("403")
        || msg.contains("unauthorized")
        || msg.contains("api key")
        || msg.contains("api_key")
    {
        "auth_failed"
    } else if msg.contains("429") || msg.contains("rate limit") || msg.contains("rate-limit") {
        "rate_limited"
    } else if msg.contains("timeout")
        || msg.contains("timed out")
        || msg.contains("connection")
        || msg.contains("network")
        || msg.contains("dns")
    {
        "network_error"
    } else if msg.contains("500")
        || msg.contains("502")
        || msg.contains("503")
        || msg.contains("504")
        || msg.contains("server error")
    {
        "model_unavailable"
    } else {
        "internal_error"
    }
}

/// Capture the last ~50 lines plus cwd from the specified pane.
fn capture_pane_context(pane_id: usize) -> Option<String> {
    let mux = Mux::try_get()?;
    let pane = mux.get_pane(mux::pane::PaneId::from(pane_id))?;
    let dims = pane.get_dimensions();
    let start = dims
        .physical_top
        .saturating_sub(50 - dims.viewport_rows as isize);
    let end = dims.physical_top + dims.viewport_rows as isize;
    let (_first, raw_lines) = pane.get_lines(start..end);
    let buffer: String = raw_lines
        .iter()
        .map(|line| line.as_str().to_string())
        .collect::<Vec<_>>()
        .join("\n");
    let cwd = pane
        .get_current_working_dir(CachePolicy::FetchImmediate)
        .map(|u| u.to_string())
        .unwrap_or_default();
    Some(format!("cwd: {}\n\nrecent buffer:\n{}", cwd, buffer))
}

const SYSTEM_PROMPT: &str = "You are Kaku, an assistant embedded inside a terminal emulator. \
When a user attaches terminal context, use it to ground your answer. \
Reply concisely; prefer code fences for commands the user might want to run.";
