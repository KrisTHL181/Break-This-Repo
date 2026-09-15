//! Shell execution tools: shell_exec, shell_bg, shell_poll.

use anyhow::{Context, Result};
use std::collections::HashMap;
use std::io::Read;
use std::os::unix::fs::OpenOptionsExt;
use std::os::unix::process::CommandExt;
use std::path::{Path, PathBuf};
use std::sync::atomic::{AtomicBool, AtomicUsize, Ordering};
use std::sync::{Arc, Mutex, OnceLock};
use std::time::{Duration, Instant};

use super::paths::resolve;

/// Wall-clock ceiling for a single `shell_exec` invocation.
pub(super) const SHELL_EXEC_TIMEOUT_SECS: u64 = 60;

/// SIGKILL the entire process group. Required because `Child::kill()` only
/// signals the direct child (the login shell), leaving grandchildren running.
pub(super) fn kill_process_group(child: &std::process::Child) {
    unsafe {
        libc::killpg(child.id() as libc::pid_t, libc::SIGKILL);
    }
}

fn shell_exec_wrapper(command: &str, cwd_tmp_path: &Path, shell: &str) -> String {
    let is_fish = Path::new(shell)
        .file_name()
        .and_then(|name| name.to_str())
        .is_some_and(|name| name == "fish");

    if is_fish {
        format!(
            "{}; set __kaku_rc $status; printf '%s' (pwd) > {}; exit $__kaku_rc",
            command,
            cwd_tmp_path.display()
        )
    } else {
        format!(
            "{}; __kaku_rc=$?; printf '%s' \"$(pwd)\" > {}; exit $__kaku_rc",
            command,
            cwd_tmp_path.display()
        )
    }
}

// ─── Background process registry ─────────────────────────────────────────────

struct BgProcess {
    child: std::process::Child,
    output: Arc<Mutex<String>>,
}

impl Drop for BgProcess {
    fn drop(&mut self) {
        kill_process_group(&self.child);
        let _ = self.child.wait();
    }
}

static BG_PROCS: OnceLock<Mutex<HashMap<u32, BgProcess>>> = OnceLock::new();

fn bg_registry() -> &'static Mutex<HashMap<u32, BgProcess>> {
    BG_PROCS.get_or_init(|| Mutex::new(HashMap::new()))
}

/// Spawn a reader thread that drains `reader` into `buf`, up to `cap` bytes
/// total (shared across sibling threads via `bytes_total`).
pub(super) fn pump_reader_capped<R: Read + Send + 'static>(
    reader: R,
    buf: Arc<Mutex<String>>,
    bytes_total: Arc<AtomicUsize>,
    cap: usize,
) -> std::thread::JoinHandle<()> {
    crate::thread_util::spawn_with_pool(move || {
        let mut r = reader;
        let mut chunk = [0u8; 8192];
        loop {
            match r.read(&mut chunk) {
                Ok(0) | Err(_) => break,
                Ok(n) => {
                    let prev = bytes_total.fetch_add(n, Ordering::Relaxed);
                    if prev < cap {
                        if let Ok(mut g) = buf.lock() {
                            let room = cap.saturating_sub(g.len());
                            if room > 0 {
                                let to_write = n.min(room);
                                g.push_str(&String::from_utf8_lossy(&chunk[..to_write]));
                            }
                        }
                    }
                }
            }
        }
    })
}

pub(super) fn exec_shell_exec(
    args: &serde_json::Value,
    cwd: &mut String,
    cap: usize,
    cancel: &Arc<AtomicBool>,
) -> Result<String> {
    let command = args["command"].as_str().context("missing command")?;
    let exec_cwd = args["cwd"]
        .as_str()
        .map(|p| resolve(p, cwd))
        .transpose()?
        .unwrap_or_else(|| PathBuf::from(cwd.as_str()));

    let ts = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .map(|d| d.subsec_nanos())
        .unwrap_or(0);
    let cwd_tmp_path =
        std::env::temp_dir().join(format!("kaku_cwd_{}_{}.txt", std::process::id(), ts));
    // create_new + mode 0o600 ensures we own the file exclusively before the shell writes to it.
    // Propagate on failure: an EEXIST or symlink collision here would allow the shell redirection
    // to follow a pre-placed symlink, turning this into a write-anywhere primitive.
    std::fs::OpenOptions::new()
        .create_new(true)
        .write(true)
        .mode(0o600)
        .open(&cwd_tmp_path)
        .with_context(|| format!("could not create temp cwd file {}", cwd_tmp_path.display()))?;
    let shell = std::env::var("SHELL").unwrap_or_else(|_| "/bin/bash".into());
    let wrapped = shell_exec_wrapper(command, &cwd_tmp_path, &shell);
    let streaming_cap = cap.saturating_sub(512);

    let mut child = std::process::Command::new(&shell)
        .arg("-l")
        .arg("-c")
        .arg(&wrapped)
        .current_dir(&exec_cwd)
        .stdout(std::process::Stdio::piped())
        .stderr(std::process::Stdio::piped())
        .process_group(0)
        .spawn()
        .with_context(|| format!("shell exec failed ({})", shell))?;

    let bytes_total = Arc::new(AtomicUsize::new(0));
    let stdout_buf = Arc::new(Mutex::new(String::new()));
    let stderr_buf = Arc::new(Mutex::new(String::new()));

    let h1 = child
        .stdout
        .take()
        .map(|s| pump_reader_capped(s, stdout_buf.clone(), bytes_total.clone(), streaming_cap));
    let h2 = child
        .stderr
        .take()
        .map(|s| pump_reader_capped(s, stderr_buf.clone(), bytes_total.clone(), streaming_cap));

    let start = Instant::now();
    let timeout = Duration::from_secs(SHELL_EXEC_TIMEOUT_SECS);
    let mut canceled = false;
    let mut timed_out = false;
    let mut overflowed = false;
    loop {
        if cancel.load(Ordering::Relaxed) {
            kill_process_group(&child);
            canceled = true;
            break;
        }
        if start.elapsed() >= timeout {
            kill_process_group(&child);
            timed_out = true;
            break;
        }
        if !overflowed && bytes_total.load(Ordering::Relaxed) >= streaming_cap {
            overflowed = true;
        }
        if let Ok(Some(_)) = child.try_wait() {
            break;
        }
        std::thread::sleep(Duration::from_millis(50));
    }
    let status = child.wait().ok();
    if let Some(h) = h1 {
        let _ = h.join();
    }
    if let Some(h) = h2 {
        let _ = h.join();
    }

    let stdout_raw = stdout_buf.lock().map(|g| g.clone()).unwrap_or_default();
    if let Ok(new_dir) = std::fs::read_to_string(&cwd_tmp_path) {
        let new_dir = new_dir.trim().to_string();
        if !new_dir.is_empty() {
            *cwd = new_dir;
        }
    }
    let _ = std::fs::remove_file(&cwd_tmp_path);

    let mut stdout_lines: Vec<&str> = stdout_raw.lines().collect();
    stdout_lines.retain(|l| !l.starts_with("__KAKU_CWD__:"));
    let mut out = stdout_lines.join("\n");
    if stdout_raw.ends_with('\n') && !out.ends_with('\n') {
        out.push('\n');
    }
    let stderr_str = stderr_buf.lock().map(|g| g.clone()).unwrap_or_default();
    if !stderr_str.is_empty() {
        if !out.is_empty() {
            out.push('\n');
        }
        out.push_str("[stderr] ");
        out.push_str(&stderr_str);
    }
    if overflowed {
        let total = bytes_total.load(Ordering::Relaxed);
        out.push_str(&format!(
            "\n[truncated: first ~{} bytes shown ({} total). \
             For large output, use shell_bg + shell_poll to avoid waiting.]",
            streaming_cap, total
        ));
    }
    if canceled {
        out.push_str("\n[canceled by user before completion]");
    }
    if timed_out {
        out.push_str(&format!(
            "\n[killed: exceeded {}s timeout. For long-running commands \
             use shell_bg + shell_poll; for searching code use grep_search.]",
            SHELL_EXEC_TIMEOUT_SECS
        ));
    }
    if let Some(s) = status {
        if !s.success() && !canceled && !timed_out {
            let code = s.code().unwrap_or(-1);
            out.push_str(&format!("\n[exit {}]", code));
        }
    }
    if out.trim().is_empty() {
        Ok("(no output)".into())
    } else {
        Ok(out)
    }
}

// Signature matches the rest of the tool dispatch table in mod.rs which all
// take `&mut String` even when individual tools only read; keeping it
// uniform makes the dispatcher symmetric.
#[allow(clippy::ptr_arg)]
pub(super) fn exec_shell_bg(args: &serde_json::Value, cwd: &mut String) -> Result<String> {
    let command = args["command"].as_str().context("missing command")?;
    let exec_cwd = args["cwd"]
        .as_str()
        .map(|p| resolve(p, cwd))
        .transpose()?
        .unwrap_or_else(|| PathBuf::from(cwd.as_str()));
    let shell = std::env::var("SHELL").unwrap_or_else(|_| "/bin/bash".into());
    let mut child = std::process::Command::new(&shell)
        .arg("-l")
        .arg("-c")
        .arg(command)
        .current_dir(&exec_cwd)
        .stdout(std::process::Stdio::piped())
        .stderr(std::process::Stdio::piped())
        .process_group(0)
        .spawn()
        .with_context(|| format!("failed to spawn background command: {}", command))?;
    let pid = child.id();
    let output = Arc::new(Mutex::new(String::new()));
    let bg_cap = super::registry::budget_for("shell_bg", "default");
    let bg_bytes = Arc::new(AtomicUsize::new(0));
    if let Some(stdout) = child.stdout.take() {
        let _ = pump_reader_capped(stdout, output.clone(), bg_bytes.clone(), bg_cap);
    }
    if let Some(stderr) = child.stderr.take() {
        let _ = pump_reader_capped(stderr, output.clone(), bg_bytes.clone(), bg_cap);
    }
    bg_registry()
        .lock()
        .unwrap_or_else(|e| e.into_inner())
        .insert(pid, BgProcess { child, output });
    Ok(format!(
        "Background process started (pid {}). Use shell_poll to check status.",
        pid
    ))
}

pub(super) fn exec_shell_poll(
    args: &serde_json::Value,
    cancel: &Arc<AtomicBool>,
) -> Result<String> {
    let pid = args["pid"].as_u64().context("missing pid")? as u32;
    let timeout_secs = args["timeout_secs"].as_u64().unwrap_or(0);

    let (snapshot, status_opt) = {
        let mut registry = bg_registry().lock().unwrap_or_else(|e| e.into_inner());
        let proc = registry
            .get_mut(&pid)
            .ok_or_else(|| anyhow::anyhow!("no background process with pid {}", pid))?;
        let snap = proc
            .output
            .lock()
            .ok()
            .map(|g| g.clone())
            .unwrap_or_default();
        let status = proc.child.try_wait().ok().flatten();
        (snap, status)
    };

    let final_status = if timeout_secs == 0 || status_opt.is_some() {
        status_opt
    } else {
        let deadline = Instant::now() + Duration::from_secs(timeout_secs);
        loop {
            std::thread::sleep(Duration::from_millis(200));
            if cancel.load(Ordering::Relaxed) || Instant::now() >= deadline {
                break None;
            }
            let mut registry = bg_registry().lock().unwrap_or_else(|e| e.into_inner());
            if let Some(proc) = registry.get_mut(&pid) {
                if let Ok(Some(s)) = proc.child.try_wait() {
                    break Some(s);
                }
            } else {
                break None;
            }
        }
    };

    let final_snapshot = if final_status.is_some() {
        let mut registry = bg_registry().lock().unwrap_or_else(|e| e.into_inner());
        let snap = registry
            .get(&pid)
            .and_then(|p| p.output.lock().ok().map(|g| g.clone()))
            .unwrap_or(snapshot);
        registry.remove(&pid);
        snap
    } else {
        snapshot
    };

    let (done_str, exit_str): (String, String) = match final_status {
        Some(s) => {
            let code = s.code().unwrap_or(-1);
            ("done".into(), format!("[exit {}]", code))
        }
        None => ("running".into(), String::new()),
    };

    if final_snapshot.is_empty() {
        Ok(format!("pid {}: {} {}", pid, done_str, exit_str))
    } else {
        Ok(format!(
            "pid {}: {} {}\n{}",
            pid, done_str, exit_str, final_snapshot
        ))
    }
}

#[cfg(test)]
mod tests {
    use super::shell_exec_wrapper;
    use std::path::Path;

    #[test]
    fn shell_exec_wrapper_uses_fish_syntax() {
        let wrapped =
            shell_exec_wrapper("false", Path::new("/tmp/kaku-cwd"), "/usr/local/bin/fish");

        assert_eq!(
            wrapped,
            "false; set __kaku_rc $status; printf '%s' (pwd) > /tmp/kaku-cwd; exit $__kaku_rc"
        );
    }

    #[test]
    fn shell_exec_wrapper_keeps_posix_syntax_for_zsh_and_bash() {
        for shell in ["/bin/bash", "/bin/zsh"] {
            let wrapped = shell_exec_wrapper("false", Path::new("/tmp/kaku-cwd"), shell);

            assert_eq!(
                wrapped,
                "false; __kaku_rc=$?; printf '%s' \"$(pwd)\" > /tmp/kaku-cwd; exit $__kaku_rc"
            );
        }
    }
}
