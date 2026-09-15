//! Keeps track of the number of user-initiated activities
use crate::Mux;
use std::panic::{catch_unwind, AssertUnwindSafe};
use std::sync::atomic::{AtomicUsize, Ordering};

static COUNT: AtomicUsize = AtomicUsize::new(0);

/// Create and hold on to an Activity while you are processing
/// the direct result of a user initiated action, such as preparing
/// to open a window.
/// Once you have opened the window, drop the activity.
/// The activity is used to keep the frontend alive even if there
/// may be no windows present in the mux.
pub struct Activity {}

impl Activity {
    pub fn new() -> Self {
        COUNT.fetch_add(1, Ordering::SeqCst);
        Self {}
    }

    pub fn count() -> usize {
        COUNT.load(Ordering::SeqCst)
    }
}

impl Drop for Activity {
    fn drop(&mut self) {
        let prev = COUNT.fetch_sub(1, Ordering::SeqCst);
        let remaining = prev.saturating_sub(1);
        log::trace!("Activity dropped; remaining={remaining}; scheduling prune_dead_windows");

        promise::spawn::spawn_into_main_thread(async move {
            log::trace!("Activity drop prune_dead_windows running; remaining={remaining}");
            if let Err(err) = catch_unwind(AssertUnwindSafe(|| {
                let mux = Mux::get();
                mux.prune_dead_windows();
            })) {
                log::error!("Activity drop prune_dead_windows panicked: {:?}", err);
            }
        })
        .detach();
    }
}
