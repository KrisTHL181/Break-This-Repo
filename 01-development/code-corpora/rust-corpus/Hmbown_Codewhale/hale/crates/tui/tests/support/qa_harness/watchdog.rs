//! Stall watchdog for the real-PTY test binaries.
//!
//! These tests drive a real child process through a real pseudo-terminal, and
//! every wait inside [`super::harness::Harness`] is already bounded. The failure
//! mode they cannot bound themselves is a wedge *outside* those waits — a
//! descendant that keeps the PTY slave open, a child that never reaps, a lock
//! nobody releases. libtest has no per-test timeout, so such a wedge does not
//! fail the test: it hangs the binary, and the CI step runs until the job's own
//! ceiling. On the exact-head 0.9.9 `ci.yml` that cost the macOS leg over an
//! hour on the Skills Manager PTY acceptance step.
//!
//! This turns that hang into a failure with evidence. The harness reports
//! progress on every PTY interaction; if no interaction happens for
//! `QA_PTY_STALL_TIMEOUT_SECS` the watchdog prints where it stalled and aborts
//! the process, so the step fails in minutes with a diagnosable message instead
//! of burning the job.
//!
//! It is a backstop, not a budget: the limit is far above any legitimate gap
//! between harness calls (workspace setup, binary spawn), so it can only fire on
//! a genuine wedge. Set `QA_PTY_STALL_TIMEOUT_SECS=0` to disable it when
//! attaching a debugger.

use std::sync::OnceLock;
use std::sync::atomic::{AtomicU64, Ordering};
use std::time::{Duration, Instant};

/// Default ceiling on silence between harness interactions.
///
/// The bounded waits inside the harness are 5–20 s (×4 on CI), and the longest
/// non-interacting gap is workspace setup — seconds. Five minutes is therefore
/// unreachable without a wedge, while still capping a wedged CI step at ~1/12 of
/// what the 0.9.9 incident cost.
const DEFAULT_STALL_TIMEOUT: Duration = Duration::from_secs(300);
const POLL_INTERVAL: Duration = Duration::from_secs(5);

fn epoch() -> Instant {
    static EPOCH: OnceLock<Instant> = OnceLock::new();
    *EPOCH.get_or_init(Instant::now)
}

fn last_progress_millis() -> &'static AtomicU64 {
    static LAST: OnceLock<AtomicU64> = OnceLock::new();
    LAST.get_or_init(|| AtomicU64::new(0))
}

fn last_label() -> &'static std::sync::Mutex<String> {
    static LABEL: OnceLock<std::sync::Mutex<String>> = OnceLock::new();
    LABEL.get_or_init(|| std::sync::Mutex::new("startup".to_string()))
}

fn stall_timeout() -> Option<Duration> {
    let configured = std::env::var("QA_PTY_STALL_TIMEOUT_SECS")
        .ok()
        .and_then(|raw| raw.trim().parse::<u64>().ok());
    match configured {
        Some(0) => None,
        Some(seconds) => Some(Duration::from_secs(seconds)),
        None => Some(DEFAULT_STALL_TIMEOUT),
    }
}

/// Record that the harness is still making progress, naming what it just did.
///
/// Cheap enough to call from the pump loop: one relaxed atomic store, and the
/// label is only taken when the lock is free.
pub fn progress(label: &str) {
    let elapsed = epoch().elapsed().as_millis().min(u128::from(u64::MAX)) as u64;
    last_progress_millis().store(elapsed, Ordering::Relaxed);
    if let Ok(mut slot) = last_label().try_lock()
        && slot.as_str() != label
    {
        slot.clear();
        slot.push_str(label);
    }
}

/// Start the watchdog once per process. Safe and cheap to call on every spawn.
pub fn arm() {
    static ARMED: OnceLock<()> = OnceLock::new();
    // Stamp progress before arming so the first interval is measured from now,
    // not from process start (the binary may have spent minutes linking).
    progress("harness spawn");
    ARMED.get_or_init(|| {
        let Some(limit) = stall_timeout() else {
            return;
        };
        let _ = std::thread::Builder::new()
            .name("qa-pty-watchdog".into())
            .spawn(move || {
                loop {
                    std::thread::sleep(POLL_INTERVAL);
                    let last =
                        Duration::from_millis(last_progress_millis().load(Ordering::Relaxed));
                    let now = epoch().elapsed();
                    let silent = now.saturating_sub(last);
                    if silent < limit {
                        continue;
                    }
                    let label = last_label()
                        .try_lock()
                        .map(|slot| slot.clone())
                        .unwrap_or_else(|_| "<label lock held>".to_string());
                    eprintln!(
                        "\nqa-pty watchdog: no PTY harness activity for {silent:?} \
                         (limit {limit:?}). Last harness step: {label}.\n\
                         A real-PTY test is wedged outside its bounded waits — most likely a \
                         descendant holding the PTY slave open, or a child that never reaps. \
                         Aborting so the step fails now instead of running to the job ceiling. \
                         Set QA_PTY_STALL_TIMEOUT_SECS=0 to disable this when debugging."
                    );
                    std::process::abort();
                }
            });
    });
}
