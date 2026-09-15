#[cfg(windows)]
use crate::os::windows::event::EventHandle;
#[cfg(target_os = "macos")]
use core_foundation::runloop::*;
use promise::spawn::{Runnable, SpawnFunc};
use std::collections::VecDeque;
use std::sync::{Arc, Mutex, MutexGuard};
use std::time::Instant;
#[cfg(all(unix, not(target_os = "macos")))]
use {
    filedescriptor::{FileDescriptor, Pipe},
    std::os::unix::io::AsRawFd,
};

lazy_static::lazy_static! {
    pub(crate) static ref SPAWN_QUEUE: Arc<SpawnQueue> = Arc::new(SpawnQueue::new().expect("failed to create SpawnQueue"));
}

pub(crate) fn run_spawn_queue_burst(max_funcs: usize) -> bool {
    SPAWN_QUEUE.run_burst(max_funcs)
}

struct InstrumentedSpawnFunc {
    func: SpawnFunc,
    at: Instant,
}

pub(crate) struct SpawnQueue {
    spawned_funcs: Mutex<VecDeque<InstrumentedSpawnFunc>>,
    spawned_funcs_low_pri: Mutex<VecDeque<InstrumentedSpawnFunc>>,

    #[cfg(windows)]
    pub event_handle: EventHandle,

    #[cfg(all(unix, not(target_os = "macos")))]
    write: Mutex<FileDescriptor>,
    #[cfg(all(unix, not(target_os = "macos")))]
    read: Mutex<FileDescriptor>,
}

fn schedule_with_pri(runnable: Runnable, high_pri: bool) {
    SPAWN_QUEUE.spawn_impl(
        Box::new(move || {
            runnable.run();
        }),
        high_pri,
    );
}

impl SpawnQueue {
    fn lock_recover<'a, T>(mutex: &'a Mutex<T>, name: &str) -> MutexGuard<'a, T> {
        match mutex.lock() {
            Ok(guard) => guard,
            Err(poisoned) => {
                log::error!(
                    "spawn queue: mutex `{}` poisoned; recovering and continuing",
                    name
                );
                poisoned.into_inner()
            }
        }
    }

    pub fn new() -> anyhow::Result<Self> {
        Self::new_impl()
    }

    pub fn register_promise_schedulers(&self) {
        promise::spawn::set_schedulers(
            Box::new(|runnable| {
                schedule_with_pri(runnable, true);
            }),
            Box::new(|runnable| {
                schedule_with_pri(runnable, false);
            }),
        );
    }

    pub fn run(&self) -> bool {
        self.run_impl()
    }

    pub fn run_burst(&self, max_funcs: usize) -> bool {
        self.run_impl_burst(max_funcs)
    }

    // This needs to be a separate function from the loop in `run`
    // in order for the lock to be released before we call the
    // returned function. Used by Linux/Windows run_impl; macOS uses batch drain.
    #[allow(dead_code)]
    fn pop_func(&self) -> Option<SpawnFunc> {
        if let Some(func) = Self::lock_recover(&self.spawned_funcs, "spawned_funcs").pop_front() {
            metrics::histogram!("executor.spawn_delay").record(func.at.elapsed());
            Some(func.func)
        } else if let Some(func) =
            Self::lock_recover(&self.spawned_funcs_low_pri, "spawned_funcs_low_pri").pop_front()
        {
            metrics::histogram!("executor.spawn_delay.low_pri").record(func.at.elapsed());
            Some(func.func)
        } else {
            None
        }
    }

    fn queue_func(&self, f: SpawnFunc, high_pri: bool) {
        let f = InstrumentedSpawnFunc {
            func: f,
            at: Instant::now(),
        };
        let mut queue = if high_pri {
            Self::lock_recover(&self.spawned_funcs, "spawned_funcs")
        } else {
            Self::lock_recover(&self.spawned_funcs_low_pri, "spawned_funcs_low_pri")
        };
        queue.push_back(f);
    }

    fn has_any_queued(&self) -> bool {
        !Self::lock_recover(&self.spawned_funcs, "spawned_funcs").is_empty()
            || !Self::lock_recover(&self.spawned_funcs_low_pri, "spawned_funcs_low_pri").is_empty()
    }
}

#[cfg(windows)]
impl SpawnQueue {
    fn new_impl() -> anyhow::Result<Self> {
        let spawned_funcs = Mutex::new(VecDeque::new());
        let spawned_funcs_low_pri = Mutex::new(VecDeque::new());
        let event_handle = EventHandle::new_manual_reset().expect("EventHandle creation failed");
        Ok(Self {
            spawned_funcs,
            spawned_funcs_low_pri,
            event_handle,
        })
    }

    fn spawn_impl(&self, f: SpawnFunc, high_pri: bool) {
        self.queue_func(f, high_pri);
        self.event_handle.set_event();
    }

    fn run_impl(&self) -> bool {
        self.event_handle.reset_event();
        while let Some(func) = self.pop_func() {
            func();
        }
        self.has_any_queued()
    }

    fn run_impl_burst(&self, max_funcs: usize) -> bool {
        self.event_handle.reset_event();
        for _ in 0..max_funcs {
            let Some(func) = self.pop_func() else {
                break;
            };
            func();
        }
        self.has_any_queued()
    }
}

#[cfg(all(unix, not(target_os = "macos")))]
impl SpawnQueue {
    fn new_impl() -> anyhow::Result<Self> {
        // On linux we have a slightly sloppy wakeup mechanism;
        // we have a non-blocking pipe that we can use to get
        // woken up after some number of enqueues.  We don't
        // guarantee a 1:1 enqueue to wakeup with this mechanism
        // but in practical terms it does guarantee a wakeup
        // if the main thread is asleep and we enqueue some
        // number of items.
        // We can't affort to use a blocking pipe for the wakeup
        // because the write needs to hold a mutex and that
        // can block reads as well as other writers.
        let mut pipe = Pipe::new()?;
        pipe.write.set_non_blocking(true)?;
        pipe.read.set_non_blocking(true)?;
        Ok(Self {
            spawned_funcs: Mutex::new(VecDeque::new()),
            spawned_funcs_low_pri: Mutex::new(VecDeque::new()),
            write: Mutex::new(pipe.write),
            read: Mutex::new(pipe.read),
        })
    }

    fn spawn_impl(&self, f: SpawnFunc, high_pri: bool) {
        use std::io::Write;

        self.queue_func(f, high_pri);
        while let Err(err) = Self::lock_recover(&self.write, "write").write(b"x") {
            if err.kind() == std::io::ErrorKind::Interrupted {
                continue;
            }
            log::warn!("Failed to signal spawn queue pipe: {:#}", err);
            break;
        }
    }

    fn run_impl(&self) -> bool {
        // On linux we only ever process one at at time, so that
        // we can return to the main loop and process messages
        // from the X server
        if let Some(func) = self.pop_func() {
            func();
        }

        // try to drain the pipe.
        // We do this regardless of whether we popped an item
        // so that we avoid being in a perpetually signalled state.
        // It is ok if we completely drain the pipe because the
        // main loop uses the return value to set the sleep
        // interval and will unconditionally call us on each
        // iteration.
        let mut byte = [0u8; 64];
        use std::io::Read;
        Self::lock_recover(&self.read, "read").read(&mut byte).ok();

        self.has_any_queued()
    }

    fn run_impl_burst(&self, max_funcs: usize) -> bool {
        for _ in 0..max_funcs {
            let Some(func) = self.pop_func() else {
                break;
            };
            func();
        }
        let mut byte = [0u8; 64];
        use std::io::Read;
        Self::lock_recover(&self.read, "read").read(&mut byte).ok();
        self.has_any_queued()
    }

    pub(crate) fn raw_fd(&self) -> std::os::unix::io::RawFd {
        Self::lock_recover(&self.read, "read").as_raw_fd()
    }
}

#[cfg(target_os = "macos")]
impl SpawnQueue {
    fn new_impl() -> anyhow::Result<Self> {
        let spawned_funcs = Mutex::new(VecDeque::new());
        let spawned_funcs_low_pri = Mutex::new(VecDeque::new());

        // Fire on AfterWaiting (runloop just woke up) so spawn callbacks triggered
        // by keyboard/timer events are processed immediately in the same turn,
        // keeping input latency low. BeforeWaiting clears any remaining items
        // before the runloop sleeps again. Using both is cheaper than
        // kCFRunLoopAllActivities while still being responsive.
        let observer = unsafe {
            CFRunLoopObserverCreate(
                std::ptr::null(),
                kCFRunLoopBeforeWaiting | kCFRunLoopAfterWaiting,
                1,
                0,
                SpawnQueue::trigger,
                std::ptr::null_mut(),
            )
        };
        unsafe {
            CFRunLoopAddObserver(CFRunLoopGetMain(), observer, kCFRunLoopCommonModes);
        }

        Ok(Self {
            spawned_funcs,
            spawned_funcs_low_pri,
        })
    }

    extern "C" fn trigger(
        _observer: *mut __CFRunLoopObserver,
        _: CFRunLoopActivity,
        _: *mut std::ffi::c_void,
    ) {
        if SPAWN_QUEUE.run() {
            Self::queue_wakeup();
        }
    }

    fn queue_wakeup() {
        unsafe {
            CFRunLoopWakeUp(CFRunLoopGetMain());
        }
    }

    fn spawn_impl(&self, f: SpawnFunc, high_pri: bool) {
        self.queue_func(f, high_pri);
        Self::queue_wakeup();
    }

    fn run_impl(&self) -> bool {
        // Process a burst each wakeup so chained high-priority GUI work
        // (SpawnWindow -> reconcile -> new_window) doesn't get stretched
        // across multiple runloop turns.
        self.run_impl_burst(64)
    }

    fn run_impl_burst(&self, max_funcs: usize) -> bool {
        // Drain both queues under a single lock each so the burst loop
        // does 2 lock acquisitions instead of up to 2*max_funcs.
        let mut batch: Vec<SpawnFunc> = {
            let mut q = Self::lock_recover(&self.spawned_funcs, "spawned_funcs");
            let n = max_funcs.min(q.len());
            q.drain(..n)
                .map(|f| {
                    metrics::histogram!("executor.spawn_delay").record(f.at.elapsed());
                    f.func
                })
                .collect()
        };
        let remaining = max_funcs.saturating_sub(batch.len());
        if remaining > 0 {
            let mut q = Self::lock_recover(&self.spawned_funcs_low_pri, "spawned_funcs_low_pri");
            let n = remaining.min(q.len());
            batch.extend(q.drain(..n).map(|f| {
                metrics::histogram!("executor.spawn_delay.low_pri").record(f.at.elapsed());
                f.func
            }));
        }
        for func in batch {
            func();
        }
        self.has_any_queued()
    }
}
