//! A2A (Agent-to-Agent) protocol subcommand for rvAgent CLI.
//!
//! Wires the `rvagent-a2a` library into the CLI per ADR-159 M1. Three
//! subcommands:
//!
//! - `serve`      — bind an `A2aServer` and serve until SIGINT/SIGTERM.
//! - `discover`   — GET `/.well-known/agent.json` of a remote agent.
//! - `send-task`  — dispatch a `TaskSpec` via `A2aClient::send_task`.
//!
//! Config resolution for `serve --config`:
//!   1. explicit `--config <path>`
//!   2. `$RVAGENT_A2A_CONFIG`
//!   3. `./a2a.toml`
//!   4. `RvAgentA2aConfig::default()` (zero-cap warning at INFO).

use std::net::SocketAddr;
use std::path::{Path, PathBuf};
use std::sync::Arc;
use std::time::Duration;

use anyhow::{Context, Result};
use clap::{Args, Subcommand};
use ed25519_dalek::SigningKey;
use tracing::{info, warn};

use rvagent_a2a::budget::BudgetLedger;
use rvagent_a2a::client::A2aClient;
use rvagent_a2a::config::RvAgentA2aConfig;
use rvagent_a2a::executor::{Executor, InMemoryRunner};
use rvagent_a2a::identity::{agent_id_from_pubkey, sign_card};
use rvagent_a2a::server::{A2aServer, A2aServerConfig};
use rvagent_a2a::types::{
    AgentCapabilities, AgentCard, AgentProvider, AgentSkill, AuthScheme, Message, Part, Role,
    TaskSpec,
};

// ---------------------------------------------------------------------------
// Subcommand definitions.
// ---------------------------------------------------------------------------

/// Top-level `a2a` subcommand.
#[derive(Args, Debug)]
pub struct A2aCommand {
    #[command(subcommand)]
    pub action: A2aAction,
}

#[derive(Subcommand, Debug)]
pub enum A2aAction {
    /// Start an A2A server bound to a TCP socket.
    Serve(ServeArgs),
    /// Fetch and print the AgentCard served at `<URL>/.well-known/agent.json`.
    Discover(DiscoverArgs),
    /// Send a task to a remote A2A endpoint and print the resulting Task.
    SendTask(SendTaskArgs),
}

#[derive(Args, Debug)]
pub struct ServeArgs {
    /// Bind address `host:port`. Default `127.0.0.1:8080`.
    #[arg(long, default_value = "127.0.0.1:8080")]
    pub bind: String,

    /// Path to a `RvAgentA2aConfig` TOML file (optional).
    #[arg(long)]
    pub config: Option<PathBuf>,

    /// Suppress the "no signing key provided" warning — ACK that a fresh
    /// Ed25519 key will be minted for this run.
    #[arg(long, default_value_t = false)]
    pub generate_key: bool,

    /// Enable Ed25519 webhook signatures (informational; the runtime
    /// feature is compile-time gated on `rvagent-a2a` feature
    /// `ed25519-webhooks`).
    #[arg(long)]
    pub features: Option<String>,
}

#[derive(Args, Debug)]
pub struct DiscoverArgs {
    /// Base URL of the target A2A agent.
    pub url: String,
}

#[derive(Args, Debug)]
pub struct SendTaskArgs {
    /// Base URL of the target A2A agent.
    pub url: String,

    /// Skill id (matches `AgentSkill::id`).
    #[arg(long)]
    pub skill: String,

    /// Inline text message. Mutually exclusive with `--input-file`.
    #[arg(long, conflicts_with = "input_file")]
    pub input: Option<String>,

    /// Read the message body from a file.
    #[arg(long = "input-file")]
    pub input_file: Option<PathBuf>,

    /// Write the resulting Task JSON to this file instead of stdout.
    #[arg(long)]
    pub output: Option<PathBuf>,
}

// ---------------------------------------------------------------------------
// Dispatch.
// ---------------------------------------------------------------------------

/// Entry point — called from `main.rs`.
pub async fn run(cmd: A2aCommand) -> Result<()> {
    match cmd.action {
        A2aAction::Serve(args) => run_serve(args).await,
        A2aAction::Discover(args) => run_discover(args).await,
        A2aAction::SendTask(args) => run_send_task(args).await,
    }
}

// ---------------------------------------------------------------------------
// `serve` implementation.
// ---------------------------------------------------------------------------

async fn run_serve(args: ServeArgs) -> Result<()> {
    // 1) Resolve the config path (explicit / env / default-file / zero-cap).
    let cfg = load_config(args.config.as_deref())?;

    // 2) Load or mint an Ed25519 signing key.
    let signing_key = load_or_generate_key(args.generate_key)?;
    let agent_id = agent_id_from_pubkey(&signing_key.verifying_key());

    // 3) Build and sign the AgentCard.
    let (card, card_bytes) = build_signed_card(&args.bind, &signing_key, &agent_id.to_string())?;

    // 4) Runtime components: executor (local InMemoryRunner) + budget ledger.
    let executor = Arc::new(Executor::Local(Arc::new(InMemoryRunner::new())));
    let budget = Arc::new(BudgetLedger::new(cfg.budget.global.clone()));

    // 5) Construct the A2aServer. `A2aServerConfig` now carries the
    //    loaded routing section so `A2aServer::new` can seed a
    //    `PeerRegistry` from `[routing.peers]` and attach a `Router` to
    //    the server state. When `[routing]` is absent or has no peers,
    //    routing is disabled and dispatch stays local — the M1 default.
    let server_config = A2aServerConfig {
        routing: Some(cfg.routing.clone()),
        ..A2aServerConfig::default()
    };
    let server = A2aServer::new(card, card_bytes, executor, budget, server_config);
    let router = server.router();

    // 6) Bind.
    let addr: SocketAddr = args
        .bind
        .parse()
        .with_context(|| format!("invalid --bind address: {}", args.bind))?;
    let listener = tokio::net::TcpListener::bind(addr)
        .await
        .with_context(|| format!("failed to bind {}", addr))?;

    // Resolve the actual bound address (for --bind 127.0.0.1:0 ephemeral
    // ports). Print on stdout so supervisors / tests can parse it.
    let local = listener
        .local_addr()
        .context("failed to query local_addr after bind")?;
    println!("listening on {}", local);
    info!(%local, agent_id = %agent_id, "a2a server listening");

    // 7) Run the axum server under a graceful-shutdown signal.
    let shutdown = shutdown_signal();
    axum::serve(listener, router.into_make_service())
        .with_graceful_shutdown(shutdown)
        .await
        .context("a2a server terminated with an error")?;

    Ok(())
}

/// Resolve the config path per the precedence documented at module top.
fn load_config(explicit: Option<&Path>) -> Result<RvAgentA2aConfig> {
    if let Some(p) = explicit {
        info!(path = %p.display(), "loading a2a config");
        return RvAgentA2aConfig::from_path(p)
            .with_context(|| format!("failed to load --config {}", p.display()));
    }

    if let Ok(p) = std::env::var("RVAGENT_A2A_CONFIG") {
        let pp = PathBuf::from(&p);
        info!(path = %pp.display(), "loading a2a config from RVAGENT_A2A_CONFIG");
        return RvAgentA2aConfig::from_path(&pp)
            .with_context(|| format!("failed to load RVAGENT_A2A_CONFIG={}", p));
    }

    let local = PathBuf::from("a2a.toml");
    if local.exists() {
        info!(path = %local.display(), "loading a2a config from ./a2a.toml");
        return RvAgentA2aConfig::from_path(&local)
            .with_context(|| format!("failed to load {}", local.display()));
    }

    info!("starting with zero-cap config; set --config for production");
    Ok(RvAgentA2aConfig::default())
}

/// Load a signing key from `RVAGENT_A2A_SIGNING_KEY` (hex, 32 bytes) or
/// mint a fresh one. Warns when neither an env key nor `--generate-key`
/// is provided.
fn load_or_generate_key(generate_key_ack: bool) -> Result<SigningKey> {
    if let Ok(hexstr) = std::env::var("RVAGENT_A2A_SIGNING_KEY") {
        let bytes =
            hex::decode(hexstr.trim()).context("RVAGENT_A2A_SIGNING_KEY must be hex-encoded")?;
        if bytes.len() != 32 {
            anyhow::bail!(
                "RVAGENT_A2A_SIGNING_KEY must be exactly 32 bytes (64 hex chars), got {}",
                bytes.len()
            );
        }
        let mut arr = [0u8; 32];
        arr.copy_from_slice(&bytes);
        return Ok(SigningKey::from_bytes(&arr));
    }

    if !generate_key_ack {
        warn!(
            "no RVAGENT_A2A_SIGNING_KEY set and --generate-key not passed; \
             minting an ephemeral Ed25519 keypair for this process — the \
             AgentID will change on restart"
        );
    }

    use rand_core::OsRng;
    Ok(SigningKey::generate(&mut OsRng))
}

/// Build a minimal signed `AgentCard`. Skills advertised: `echo` (the
/// InMemoryRunner echoes any input). Production deployments will replace
/// this with a real card.
fn build_signed_card(
    bind: &str,
    signing_key: &SigningKey,
    agent_id: &str,
) -> Result<(AgentCard, Vec<u8>)> {
    let url = if bind.starts_with("http") {
        bind.to_string()
    } else {
        format!("http://{}", bind)
    };

    let mut card = AgentCard {
        name: "rvagent-a2a".into(),
        description: "rvAgent A2A endpoint (InMemoryRunner)".into(),
        url,
        provider: AgentProvider {
            organization: "ruvector".into(),
            url: None,
        },
        version: env!("CARGO_PKG_VERSION").into(),
        capabilities: AgentCapabilities {
            streaming: true,
            push_notifications: false,
        },
        skills: vec![AgentSkill {
            id: "echo".into(),
            name: "echo".into(),
            description: "InMemoryRunner echo — returns the input as an artifact".into(),
            tags: vec!["test".into()],
            input_modes: vec!["text/plain".into()],
            output_modes: vec!["text/plain".into()],
        }],
        authentication: AuthScheme { schemes: vec![] },
        metadata: serde_json::json!({
            "ruvector": {
                "agent_id": agent_id,
            }
        }),
    };

    // Sign card, patch the signature into metadata.ruvector.signature,
    // then re-serialize canonical bytes so server discovery + verify
    // round-trip.
    let sig = sign_card(&card, signing_key).context("failed to sign AgentCard")?;

    if let Some(obj) = card
        .metadata
        .as_object_mut()
        .and_then(|m| m.get_mut("ruvector").and_then(|r| r.as_object_mut()))
    {
        obj.insert(
            "signature".into(),
            serde_json::to_value(&sig).context("serialize CardSignature")?,
        );
    }

    let card_bytes = serde_json::to_vec(&card).context("serialize signed AgentCard")?;
    Ok((card, card_bytes))
}

/// Wait for SIGINT or SIGTERM with a bounded drain window. On Unix both
/// signals are handled; on other platforms we fall back to ctrl_c.
async fn shutdown_signal() {
    #[cfg(unix)]
    {
        use tokio::signal::unix::{signal, SignalKind};
        let mut sigterm = match signal(SignalKind::terminate()) {
            Ok(s) => s,
            Err(e) => {
                warn!(%e, "failed to install SIGTERM handler; only SIGINT will shut down");
                let _ = tokio::signal::ctrl_c().await;
                info!("shutdown signal received, draining");
                tokio::time::sleep(Duration::from_secs(5)).await;
                return;
            }
        };
        tokio::select! {
            _ = tokio::signal::ctrl_c() => {}
            _ = sigterm.recv() => {}
        }
        info!("shutdown signal received, draining");
        tokio::time::sleep(Duration::from_secs(5)).await;
    }

    #[cfg(not(unix))]
    {
        let _ = tokio::signal::ctrl_c().await;
        info!("shutdown signal received, draining");
        tokio::time::sleep(Duration::from_secs(5)).await;
    }
}

// ---------------------------------------------------------------------------
// `discover` implementation.
// ---------------------------------------------------------------------------

async fn run_discover(args: DiscoverArgs) -> Result<()> {
    // We talk directly to the raw endpoint so we can (a) verify the
    // signature ourselves and emit a human-readable status to stderr, and
    // (b) print the AgentCard exactly as the peer served it.
    let url = format!("{}/.well-known/agent.json", args.url.trim_end_matches('/'));

    let http = reqwest::Client::builder()
        .timeout(Duration::from_secs(30))
        .build()
        .context("failed to build reqwest client")?;

    let resp = http
        .get(&url)
        .send()
        .await
        .with_context(|| format!("GET {}", url))?;
    if !resp.status().is_success() {
        anyhow::bail!("GET {}: HTTP {}", url, resp.status());
    }

    let bytes = resp.bytes().await.context("read agent card body")?;
    let card: AgentCard = serde_json::from_slice(&bytes).context("parse AgentCard")?;

    // Signature check — best-effort, to stderr so stdout stays valid JSON.
    match rvagent_a2a::identity::verify_card(&card) {
        Ok(id) => eprintln!("VERIFIED({})", id),
        Err(rvagent_a2a::identity::IdentityError::SignatureMissing) => {
            eprintln!("UNSIGNED");
        }
        Err(e) => {
            eprintln!("SIGNATURE_INVALID: {}", e);
        }
    }

    // Emit the card as pretty JSON with a stable `agentCard` envelope so
    // smoke tests can grep for the key deterministically.
    let envelope = serde_json::json!({
        "agentCard": card,
    });
    let out = serde_json::to_string_pretty(&envelope).context("serialize output")?;
    println!("{}", out);
    Ok(())
}

// ---------------------------------------------------------------------------
// `send-task` implementation.
// ---------------------------------------------------------------------------

async fn run_send_task(args: SendTaskArgs) -> Result<()> {
    let input_text = match (&args.input, &args.input_file) {
        (Some(t), None) => t.clone(),
        (None, Some(p)) => std::fs::read_to_string(p)
            .with_context(|| format!("read --input-file {}", p.display()))?,
        (None, None) => String::new(),
        (Some(_), Some(_)) => {
            anyhow::bail!("--input and --input-file are mutually exclusive");
        }
    };

    // Build a zero-config root TaskContext. The peer will re-derive its
    // own chain from this on arrival.
    let root = rvagent_a2a::identity::AgentID("0".repeat(64));
    let context = rvagent_a2a::context::TaskContext::new_root(root);

    let spec = TaskSpec {
        id: format!("cli-{}", uuid::Uuid::new_v4()),
        skill: args.skill,
        message: Message {
            role: Role::User,
            parts: vec![Part::Text { text: input_text }],
            metadata: serde_json::Value::Null,
        },
        policy: None,
        context,
        metadata: serde_json::Value::Null,
    };

    let client = A2aClient::new().context("build A2aClient")?;
    let task = client
        .send_task(&args.url, spec)
        .await
        .context("A2aClient::send_task")?;

    let out = serde_json::to_string_pretty(&task).context("serialize Task")?;
    if let Some(path) = args.output {
        std::fs::write(&path, &out)
            .with_context(|| format!("write --output {}", path.display()))?;
    } else {
        println!("{}", out);
    }
    Ok(())
}

// ---------------------------------------------------------------------------
// Tests.
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;
    use clap::{CommandFactory, Parser};

    /// Tiny parent command used only to validate the clap wiring; the real
    /// binary composes `A2aCommand` under `rvagent a2a`.
    #[derive(clap::Parser, Debug)]
    #[command(name = "test-harness")]
    struct Harness {
        #[command(subcommand)]
        cmd: A2aAction,
    }

    #[test]
    fn a2a_subcommand_parses() {
        Harness::command().debug_assert();
    }

    #[test]
    fn serve_default_bind() {
        let h = Harness::try_parse_from(["test-harness", "serve"]).expect("parse");
        match h.cmd {
            A2aAction::Serve(s) => assert_eq!(s.bind, "127.0.0.1:8080"),
            _ => panic!("expected Serve"),
        }
    }

    #[test]
    fn discover_requires_url() {
        let res = Harness::try_parse_from(["test-harness", "discover"]);
        assert!(res.is_err(), "discover without URL must fail");
    }

    #[test]
    fn send_task_parses() {
        let h = Harness::try_parse_from([
            "test-harness",
            "send-task",
            "http://127.0.0.1:8080",
            "--skill",
            "echo",
            "--input",
            "hello",
        ])
        .expect("parse");
        match h.cmd {
            A2aAction::SendTask(s) => {
                assert_eq!(s.skill, "echo");
                assert_eq!(s.input.as_deref(), Some("hello"));
            }
            _ => panic!("expected SendTask"),
        }
    }

    #[test]
    fn send_task_input_conflicts() {
        let res = Harness::try_parse_from([
            "test-harness",
            "send-task",
            "http://x",
            "--skill",
            "s",
            "--input",
            "a",
            "--input-file",
            "/tmp/x",
        ]);
        assert!(res.is_err(), "--input and --input-file must conflict");
    }

    #[test]
    fn build_signed_card_round_trips_signature() {
        let sk = SigningKey::from_bytes(&[9u8; 32]);
        let id = agent_id_from_pubkey(&sk.verifying_key()).to_string();
        let (card, bytes) = build_signed_card("127.0.0.1:8080", &sk, &id).expect("build card");
        assert!(!bytes.is_empty());
        // verify_card should succeed because sign_card stamped a signature.
        let aid = rvagent_a2a::identity::verify_card(&card).expect("verify");
        assert_eq!(aid.to_string(), id);
    }
}
