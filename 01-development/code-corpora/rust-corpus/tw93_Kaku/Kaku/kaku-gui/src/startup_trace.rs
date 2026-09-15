//! Lightweight startup timing markers gated by `KAKU_STARTUP_TRACE`.

use std::sync::OnceLock;
use std::time::Instant;

static START: OnceLock<Instant> = OnceLock::new();
static ENABLED: OnceLock<bool> = OnceLock::new();

pub fn init() {
    START.get_or_init(Instant::now);
    ENABLED.get_or_init(|| std::env::var_os("KAKU_STARTUP_TRACE").is_some());
}

pub fn mark(label: &str) {
    if *ENABLED.get().unwrap_or(&false) {
        let elapsed = START.get().map(|s| s.elapsed()).unwrap_or_default();
        eprintln!(
            "[startup] {:>8.3}ms  {}",
            elapsed.as_secs_f64() * 1000.0,
            label
        );
    }
}
