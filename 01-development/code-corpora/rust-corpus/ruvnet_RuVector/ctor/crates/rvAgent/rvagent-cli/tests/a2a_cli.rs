//! End-to-end smoke test for `rvagent a2a` subcommand.
//!
//! Spawns `rvagent a2a serve --bind 127.0.0.1:0` as a child process,
//! parses the bound port from its stdout, and then exercises
//! `discover` + `send-task` against it.

use std::process::Stdio;
use std::sync::{Arc, Mutex};
use std::time::Duration;

use tokio::io::{AsyncBufReadExt, BufReader};
use tokio::process::Command as TokioCommand;

/// Locate the `rvagent` binary produced by `cargo build -p rvagent-cli`.
/// Works in both `cargo test` (where `CARGO_BIN_EXE_rvagent` is set) and
/// ad-hoc invocations (fallback to `target/debug`).
fn rvagent_bin() -> std::path::PathBuf {
    if let Some(p) = option_env!("CARGO_BIN_EXE_rvagent") {
        return std::path::PathBuf::from(p);
    }
    let mut p = std::path::PathBuf::from(env!("CARGO_MANIFEST_DIR"));
    for _ in 0..3 {
        p = p.parent().unwrap().to_path_buf();
    }
    p.join("target").join("debug").join("rvagent")
}

#[tokio::test]
async fn a2a_serve_discover_and_send_task() {
    let bin = rvagent_bin();
    assert!(bin.exists(), "rvagent binary not found at {:?}", bin);

    // -- 1) Spawn the server on an ephemeral port.
    let mut server = TokioCommand::new(&bin)
        .args(["a2a", "serve", "--bind", "127.0.0.1:0", "--generate-key"])
        .stdout(Stdio::piped())
        .stderr(Stdio::piped())
        .kill_on_drop(true)
        .spawn()
        .expect("spawn rvagent a2a serve");

    let stdout = server.stdout.take().expect("server stdout piped");
    let stderr = server.stderr.take().expect("server stderr piped");
    let mut reader = BufReader::new(stdout).lines();

    // Drain stderr in the background so it doesn't block the child if the
    // pipe fills, AND so we can dump it on diagnostic failure paths.
    let stderr_buf: Arc<Mutex<Vec<u8>>> = Arc::new(Mutex::new(Vec::new()));
    {
        let buf = stderr_buf.clone();
        tokio::spawn(async move {
            use tokio::io::AsyncReadExt;
            let mut reader = stderr;
            let mut chunk = [0u8; 4096];
            while let Ok(n) = reader.read(&mut chunk).await {
                if n == 0 {
                    break;
                }
                buf.lock().unwrap().extend_from_slice(&chunk[..n]);
            }
        });
    }
    let dump_stderr = || -> String {
        let raw = stderr_buf.lock().unwrap().clone();
        String::from_utf8_lossy(&raw).into_owned()
    };

    // -- 2) Parse "listening on 127.0.0.1:<port>" from the first line.
    //
    // Give the server up to 20s to bind + print; CI under load is slower
    // than local. On every failure path we dump stderr so the actual
    // error reason is visible.
    let line = match tokio::time::timeout(Duration::from_secs(20), reader.next_line()).await {
        Ok(Ok(Some(l))) => l,
        Ok(Ok(None)) => panic!(
            "server closed stdout before emitting listening line.\n--- server stderr ---\n{}",
            dump_stderr()
        ),
        Ok(Err(e)) => panic!(
            "server stdout read error: {e}\n--- server stderr ---\n{}",
            dump_stderr()
        ),
        Err(_) => panic!(
            "timed out waiting for server listening line (>20s)\n--- server stderr ---\n{}",
            dump_stderr()
        ),
    };
    let addr = line
        .strip_prefix("listening on ")
        .unwrap_or_else(|| panic!("unexpected first-line stdout from server: {:?}", line))
        .trim()
        .to_string();
    let base_url = format!("http://{}", addr);

    // -- 3) Run `discover` against the live server.
    let out = TokioCommand::new(&bin)
        .args(["a2a", "discover", &base_url])
        .output()
        .await
        .expect("spawn rvagent a2a discover");
    assert!(
        out.status.success(),
        "discover failed: status={:?} stdout={} stderr={}",
        out.status,
        String::from_utf8_lossy(&out.stdout),
        String::from_utf8_lossy(&out.stderr),
    );
    let stdout = String::from_utf8(out.stdout).expect("discover stdout utf8");
    // Must be parseable JSON with an `agentCard` envelope.
    let v: serde_json::Value =
        serde_json::from_str(&stdout).expect("discover stdout is valid JSON");
    assert!(
        v.get("agentCard").is_some(),
        "discover response missing agentCard: {}",
        stdout
    );

    // -- 4) Run `send-task` — skill `echo` is advertised by the built-in
    // InMemoryRunner, which always completes synchronously.
    let out = TokioCommand::new(&bin)
        .args([
            "a2a",
            "send-task",
            &base_url,
            "--skill",
            "echo",
            "--input",
            "hello",
        ])
        .output()
        .await
        .expect("spawn rvagent a2a send-task");
    assert!(
        out.status.success(),
        "send-task failed: status={:?} stdout={} stderr={}",
        out.status,
        String::from_utf8_lossy(&out.stdout),
        String::from_utf8_lossy(&out.stderr),
    );
    let stdout = String::from_utf8(out.stdout).expect("send-task stdout utf8");
    // Task JSON must report completed state. Match both kebab-case
    // (on-wire) and struct-field forms conservatively.
    assert!(
        stdout.contains("\"state\": \"completed\"") || stdout.contains("\"state\":\"completed\""),
        "send-task response did not include completed state:\n{}",
        stdout
    );

    // -- 5) Tear the server down.
    let _ = server.start_kill();
    let _ = server.wait().await;
}
