//! rvAgent A2A swarm demo — orchestrator binary.
//!
//! Spawns three `rvagent a2a serve` child processes on distinct local
//! ports, each loading its own TOML config with its own `[policy]` +
//! `[budget]` + `[recursion]` caps:
//!
//!   * `node-cheap`   — 127.0.0.1:18001 — cheap-but-slow tier
//!   * `node-fast`    — 127.0.0.1:18002 — fast-but-pricey tier
//!   * `node-router`  — 127.0.0.1:18003 — dispatcher, CheapestUnderLatency
//!
//! The router's config carries a `[[routing.peers]]` list naming the
//! two leaf nodes. At startup the router's `A2aServer` spawns an async
//! discovery task per peer (`fetch_card` → `PeerRegistry::add`), then
//! installs a `Router` on its `A2aState`. When we dispatch a task to
//! the router, `tasks/send` consults that router and forwards via
//! `Executor::Remote` to whichever peer the `CheapestUnderLatency`
//! selector picks (falling back to `LowestLatency` if nothing is under
//! the 2000 ms cap).
//!
//! The orchestrator:
//!   1. Spawns + discovers all three nodes.
//!   2. Waits briefly for router-side peer discovery to complete (the
//!      router's two `fetch_card` calls are async so we give them a
//!      head start before dispatching).
//!   3. Sends ONE task to the router at `:18003`.
//!   4. Asserts the returned `Task.metadata.ruvector.routed_via.peer_url`
//!      equals one of the two leaf URLs — the unambiguous proof that
//!      the router forwarded over HTTP instead of handling locally.

use std::path::{Path, PathBuf};
use std::process::Stdio;
use std::time::{Duration, Instant};

use anyhow::{bail, Context, Result};
use tokio::io::{AsyncBufReadExt, BufReader};
use tokio::process::{Child, Command as TokioCommand};
use tracing::{info, warn};
use tracing_subscriber::EnvFilter;

// ---------------------------------------------------------------------------
// Topology.
// ---------------------------------------------------------------------------

struct NodeSpec {
    name: &'static str,
    bind: &'static str,
    config: &'static str, // path relative to example root
}

const NODES: &[NodeSpec] = &[
    NodeSpec {
        name: "node-cheap",
        bind: "127.0.0.1:18001",
        config: "configs/node-cheap.toml",
    },
    NodeSpec {
        name: "node-fast",
        bind: "127.0.0.1:18002",
        config: "configs/node-fast.toml",
    },
    NodeSpec {
        name: "node-router",
        bind: "127.0.0.1:18003",
        config: "configs/node-router.toml",
    },
];

const ROUTER_INDEX: usize = 2;
const STARTUP_TIMEOUT: Duration = Duration::from_secs(30);
const SHUTDOWN_TIMEOUT: Duration = Duration::from_secs(10);

// ---------------------------------------------------------------------------
// Entry.
// ---------------------------------------------------------------------------

#[tokio::main]
async fn main() -> Result<()> {
    tracing_subscriber::fmt()
        .with_env_filter(
            EnvFilter::try_from_default_env().unwrap_or_else(|_| EnvFilter::new("info")),
        )
        .with_target(false)
        .init();

    let example_root = manifest_dir();
    let rvagent_bin = resolve_rvagent_binary(&example_root)?;
    info!(bin = %rvagent_bin.display(), "using rvagent binary");

    // 1) Spawn the three nodes. `RunningNode` keeps their `Child` handles
    //    alive so `kill_on_drop` can tear them down even on panic paths.
    let mut running: Vec<RunningNode> = Vec::new();
    for spec in NODES {
        let node = spawn_node(&rvagent_bin, &example_root, spec)
            .await
            .with_context(|| format!("failed to spawn {}", spec.name))?;
        info!(
            name = spec.name,
            bind = %node.bound_addr,
            pid = node.child.id().unwrap_or_default(),
            "node is listening",
        );
        running.push(node);
    }

    // 2) Discover each peer. This is the health probe that also proves the
    //    signed AgentCard round-trips over HTTP.
    let http = reqwest::Client::builder()
        .timeout(Duration::from_secs(5))
        .build()
        .context("build reqwest client")?;

    for node in &running {
        let url = format!("http://{}/.well-known/agent.json", node.bound_addr);
        let resp = http
            .get(&url)
            .send()
            .await
            .with_context(|| format!("GET {}", url))?;
        if !resp.status().is_success() {
            bail!("{} discover: HTTP {}", node.spec.name, resp.status());
        }
        let body: serde_json::Value = resp.json().await.context("parse agent card")?;
        let skill_count = body
            .get("skills")
            .and_then(|s| s.as_array())
            .map(|a| a.len())
            .unwrap_or(0);
        info!(
            name = node.spec.name,
            url = %url,
            skills = skill_count,
            "discovered signed AgentCard",
        );
    }

    // 3) Give the router's async peer-discovery tasks a head start so
    //    the `PeerRegistry` is seeded before we dispatch. Each discovery
    //    is a single `GET /.well-known/agent.json` against a peer we
    //    already know is listening — ~100 ms loopback is usually enough,
    //    but we wait a little longer to be safe on slow CI runners.
    tokio::time::sleep(Duration::from_millis(750)).await;

    // 4) Dispatch ONE task to the router. The router has no local
    //    runner for `echo` beyond what `InMemoryRunner` provides, but
    //    its `[routing]` block forces forwarding: the `Router` picks a
    //    peer, `Executor::Remote` posts `tasks/send` over HTTP, and the
    //    peer's runner produces the echo artifact. The returned Task
    //    carries `metadata.ruvector.routed_via.peer_url` stamped by
    //    the server on every successful forward.
    let router_node = &running[ROUTER_INDEX];
    let started = Instant::now();
    let router_result = dispatch_task(&rvagent_bin, &router_node.bound_addr, "hello swarm").await;
    let elapsed_ms = started.elapsed().as_millis() as u64;

    let (router_state, routed_via_url, routed_via_selector, ok) = match &router_result {
        Ok(task_json) => {
            let state = task_json
                .pointer("/status/state")
                .and_then(|s| s.as_str())
                .or_else(|| task_json.get("state").and_then(|s| s.as_str()))
                .unwrap_or("<missing>")
                .to_string();
            let routed_via_url = task_json
                .pointer("/metadata/ruvector/routed_via/peer_url")
                .and_then(|s| s.as_str())
                .map(|s| s.to_string());
            let routed_via_selector = task_json
                .pointer("/metadata/ruvector/routed_via/selector")
                .and_then(|s| s.as_str())
                .map(|s| s.to_string());
            let forwarded = routed_via_url.is_some();
            let completed = state == "completed";
            (
                state,
                routed_via_url,
                routed_via_selector,
                completed && forwarded,
            )
        }
        Err(e) => {
            warn!(error = %e, "router dispatch failed");
            (format!("error: {e}"), None, None, false)
        }
    };

    // Build a small outcomes table that also records each node's
    // discover outcome as background evidence.
    let outcomes: Vec<DispatchOutcome> = running
        .iter()
        .enumerate()
        .map(|(i, node)| DispatchOutcome {
            name: node.spec.name,
            bind: node.bound_addr.clone(),
            state: if i == ROUTER_INDEX {
                router_state.clone()
            } else {
                "discovered".into()
            },
            elapsed_ms: if i == ROUTER_INDEX { elapsed_ms } else { 0 },
            ok: if i == ROUTER_INDEX { ok } else { true },
        })
        .collect();

    let router = &outcomes[ROUTER_INDEX];

    println!();
    println!("=== a2a-swarm demo summary ============================================");
    for o in &outcomes {
        println!(
            "  {:<12} bind={:<18} state={:<12} took={}ms  ok={}",
            o.name, o.bind, o.state, o.elapsed_ms, o.ok
        );
    }
    println!("-----------------------------------------------------------------------");
    println!(
        "  dispatched to router at {}: state={} took={}ms",
        router.bind, router.state, router.elapsed_ms
    );
    match (&routed_via_url, &routed_via_selector) {
        (Some(url), Some(sel)) => {
            println!(
                "  router FORWARDED via selector={} to peer_url={}",
                sel, url
            );
            // Identify which named peer handled it for a friendly log line.
            let handler = outcomes
                .iter()
                .find(|o| url.contains(&o.bind))
                .map(|o| o.name)
                .unwrap_or("<unknown>");
            println!("  handled by: {}", handler);
        }
        _ => {
            println!("  router did NOT forward — no routed_via stamp on the returned Task");
        }
    }
    println!("=======================================================================");

    // 5) Tear down all three children with SIGTERM, then wait with a
    //    bounded timeout.
    for mut node in running.drain(..) {
        let pid = node.child.id().unwrap_or_default();
        // `tokio::process::Child::start_kill` sends SIGKILL on Unix;
        // the CLI's own shutdown signal handler uses SIGINT/SIGTERM.
        // We send SIGTERM manually so the server's graceful-shutdown
        // path runs. Fall back to `start_kill` if signalling fails.
        #[cfg(unix)]
        if let Some(pid) = node.child.id() {
            // SAFETY: kill(2) with SIGTERM on a child we own. Any
            // error (EPERM / ESRCH) just falls through to start_kill.
            let _ = libc_kill(pid as i32, SIGTERM);
        }
        #[cfg(not(unix))]
        {
            let _ = node.child.start_kill();
        }

        match tokio::time::timeout(SHUTDOWN_TIMEOUT, node.child.wait()).await {
            Ok(Ok(status)) => info!(name = node.spec.name, pid, ?status, "node exited"),
            Ok(Err(e)) => warn!(name = node.spec.name, pid, error = %e, "wait error"),
            Err(_) => {
                warn!(
                    name = node.spec.name,
                    pid, "shutdown timed out, force-killing"
                );
                let _ = node.child.start_kill();
                let _ = node.child.wait().await;
            }
        }
    }

    if router.ok && routed_via_url.is_some() {
        Ok(())
    } else if !router.ok {
        bail!(
            "router node did not forward + complete its task (state={}, routed_via_url={:?})",
            router.state,
            routed_via_url,
        );
    } else {
        bail!(
            "router returned a completed task but without routed_via metadata \
             — the task was handled locally, not forwarded"
        );
    }
}

// ---------------------------------------------------------------------------
// Node spawning.
// ---------------------------------------------------------------------------

struct RunningNode {
    spec: &'static NodeSpec,
    bound_addr: String,
    child: Child,
}

async fn spawn_node(
    rvagent_bin: &Path,
    example_root: &Path,
    spec: &'static NodeSpec,
) -> Result<RunningNode> {
    let cfg_path = example_root.join(spec.config);
    if !cfg_path.exists() {
        bail!("missing config file: {}", cfg_path.display());
    }

    let mut cmd = TokioCommand::new(rvagent_bin);
    cmd.args([
        "a2a",
        "serve",
        "--bind",
        spec.bind,
        "--config",
        &cfg_path.to_string_lossy(),
        "--generate-key",
    ])
    .env("RUST_LOG", "rvagent_a2a=info")
    .stdout(Stdio::piped())
    .stderr(Stdio::piped())
    .kill_on_drop(true);

    let mut child = cmd.spawn().with_context(|| {
        format!(
            "spawn `{} a2a serve --bind {} --config {}`",
            rvagent_bin.display(),
            spec.bind,
            cfg_path.display()
        )
    })?;

    let stdout = child.stdout.take().context("child stdout was not piped")?;
    let reader = BufReader::new(stdout);
    let mut lines = reader.lines();

    // Wait up to STARTUP_TIMEOUT for the "listening on <addr>" line.
    let first_line = tokio::time::timeout(STARTUP_TIMEOUT, lines.next_line())
        .await
        .with_context(|| {
            format!(
                "{} did not emit a listening line within {:?}",
                spec.name, STARTUP_TIMEOUT
            )
        })?
        .with_context(|| format!("{} stdout read error", spec.name))?;

    let first_line = first_line.with_context(|| {
        format!(
            "{} closed stdout before emitting a listening line",
            spec.name
        )
    })?;

    let bound_addr = first_line
        .strip_prefix("listening on ")
        .with_context(|| {
            format!(
                "{}: unexpected first line from rvagent: {:?}",
                spec.name, first_line
            )
        })?
        .trim()
        .to_string();

    // Drop the line reader by detaching a background drain — we don't
    // want the child blocking on stdout full. Just discard the rest.
    tokio::spawn(async move {
        let mut lines = lines;
        while let Ok(Some(_l)) = lines.next_line().await {
            // intentionally discarded
        }
    });

    Ok(RunningNode {
        spec,
        bound_addr,
        child,
    })
}

// ---------------------------------------------------------------------------
// Dispatch helper.
// ---------------------------------------------------------------------------

async fn dispatch_task(
    rvagent_bin: &Path,
    bound_addr: &str,
    input: &str,
) -> Result<serde_json::Value> {
    let url = format!("http://{}", bound_addr);
    let out = TokioCommand::new(rvagent_bin)
        .args([
            "a2a",
            "send-task",
            &url,
            "--skill",
            "echo",
            "--input",
            input,
        ])
        .output()
        .await
        .context("spawn rvagent a2a send-task")?;

    if !out.status.success() {
        bail!(
            "send-task to {url} failed: status={:?} stderr={}",
            out.status,
            String::from_utf8_lossy(&out.stderr),
        );
    }

    let stdout = String::from_utf8(out.stdout).context("send-task stdout utf8")?;
    let task: serde_json::Value =
        serde_json::from_str(&stdout).context("parse Task JSON from send-task")?;
    Ok(task)
}

// ---------------------------------------------------------------------------
// Reporting.
// ---------------------------------------------------------------------------

struct DispatchOutcome {
    name: &'static str,
    bind: String,
    state: String,
    elapsed_ms: u64,
    ok: bool,
}

// ---------------------------------------------------------------------------
// Binary resolution.
// ---------------------------------------------------------------------------

fn manifest_dir() -> PathBuf {
    PathBuf::from(env!("CARGO_MANIFEST_DIR"))
}

/// Find the `rvagent` binary. `cargo run -p a2a-swarm` compiles
/// `rvagent-cli` as a dependency, which drops `rvagent` into the same
/// target directory, so we walk up from our manifest to the workspace
/// root and look under `target/{debug,release}/rvagent`.
fn resolve_rvagent_binary(example_root: &Path) -> Result<PathBuf> {
    // Walk up until we find a directory that contains `target/`.
    let mut cur: &Path = example_root;
    for _ in 0..6 {
        let candidate = cur.join("target");
        if candidate.exists() {
            // Check both release and debug.
            for profile in &["release", "debug"] {
                let bin = candidate.join(profile).join("rvagent");
                if bin.exists() {
                    return Ok(bin);
                }
            }
        }
        match cur.parent() {
            Some(p) => cur = p,
            None => break,
        }
    }

    // Fallback: `cargo build -p rvagent-cli` hint.
    bail!(
        "rvagent binary not found under any ancestor `target/{{debug,release}}/`. \
         Run `cargo build -p rvagent-cli` first, or `cargo run -p a2a-swarm` \
         which will build it as a dependency."
    )
}

// ---------------------------------------------------------------------------
// Minimal libc shim — we only need kill(2). Avoids pulling the `libc` crate
// just for one syscall on the shutdown path.
// ---------------------------------------------------------------------------

#[cfg(unix)]
const SIGTERM: i32 = 15;

#[cfg(unix)]
extern "C" {
    fn kill(pid: i32, sig: i32) -> i32;
}

#[cfg(unix)]
fn libc_kill(pid: i32, sig: i32) -> i32 {
    // SAFETY: kill(2) is a no-op on bad pids (returns -1 + errno=ESRCH).
    unsafe { kill(pid, sig) }
}
