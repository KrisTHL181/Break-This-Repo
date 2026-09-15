//! Typed tuning for durable storage migrations.

use std::num::{NonZeroU64, NonZeroUsize};

/// Background worker execution mode for durable migrations.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum MigrationWorkerMode {
    /// Run background cleanup while the writer is open.
    Background,
    /// Leave background work for deterministic manual stepping.
    Disabled,
}

/// Positive source-row limit for one migration transaction.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct MigrationBatchRows(NonZeroUsize);

impl MigrationBatchRows {
    /// Creates a positive source-row limit.
    pub fn new(rows: usize) -> Option<Self> {
        NonZeroUsize::new(rows).map(Self)
    }

    /// Returns the positive source-row limit.
    pub const fn get(self) -> usize {
        self.0.get()
    }
}

/// Positive source-byte limit for one migration transaction.
///
/// ```
/// use db::config::MigrationBatchBytes;
///
/// let limit = MigrationBatchBytes::new(64 * 1024 * 1024).expect("positive byte limit");
/// assert_eq!(limit.get(), 64 * 1024 * 1024);
/// assert!(MigrationBatchBytes::new(0).is_none());
/// ```
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct MigrationBatchBytes(NonZeroUsize);

impl MigrationBatchBytes {
    /// Creates a positive source-byte limit.
    pub fn new(bytes: usize) -> Option<Self> {
        NonZeroUsize::new(bytes).map(Self)
    }

    /// Returns the positive source-byte limit.
    pub const fn get(self) -> usize {
        self.0.get()
    }
}

/// Positive delay after a worker commits useful work.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct MigrationActiveIntervalMillis(NonZeroU64);

impl MigrationActiveIntervalMillis {
    /// Creates a positive active-loop interval.
    pub fn new(millis: u64) -> Option<Self> {
        NonZeroU64::new(millis).map(Self)
    }

    /// Returns the interval in milliseconds.
    pub const fn get(self) -> u64 {
        self.0.get()
    }
}

/// Positive delay after a worker finds no runnable work.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct MigrationIdleIntervalMillis(NonZeroU64);

impl MigrationIdleIntervalMillis {
    /// Creates a positive idle-loop interval.
    pub fn new(millis: u64) -> Option<Self> {
        NonZeroU64::new(millis).map(Self)
    }

    /// Returns the interval in milliseconds.
    pub const fn get(self) -> u64 {
        self.0.get()
    }
}

/// Bounded migration worker policy.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct MigrationTuning {
    worker_mode: MigrationWorkerMode,
    batch_rows: MigrationBatchRows,
    batch_bytes: MigrationBatchBytes,
    active_interval_millis: MigrationActiveIntervalMillis,
    idle_interval_millis: MigrationIdleIntervalMillis,
}

impl MigrationTuning {
    /// Default source rows per transaction.
    pub const DEFAULT_BATCH_ROWS: usize = 1_024;
    /// Default source bytes per transaction (64 MiB).
    pub const DEFAULT_BATCH_BYTES: usize = 64 * 1_024 * 1_024;
    /// Default active-loop delay.
    pub const DEFAULT_ACTIVE_INTERVAL_MILLIS: u64 = 10;
    /// Default idle-loop delay.
    pub const DEFAULT_IDLE_INTERVAL_MILLIS: u64 = 1_000;

    /// Returns the worker mode.
    pub const fn worker_mode(self) -> MigrationWorkerMode {
        self.worker_mode
    }

    /// Returns the source-row limit.
    pub const fn batch_rows(self) -> MigrationBatchRows {
        self.batch_rows
    }

    /// Returns the source-byte limit.
    pub const fn batch_bytes(self) -> MigrationBatchBytes {
        self.batch_bytes
    }

    /// Returns the active-loop interval.
    pub const fn active_interval_millis(self) -> MigrationActiveIntervalMillis {
        self.active_interval_millis
    }

    /// Returns the idle-loop interval.
    pub const fn idle_interval_millis(self) -> MigrationIdleIntervalMillis {
        self.idle_interval_millis
    }

    /// Replaces the worker mode.
    pub const fn with_worker_mode(mut self, mode: MigrationWorkerMode) -> Self {
        self.worker_mode = mode;
        self
    }

    /// Replaces the source-row limit.
    pub const fn with_batch_rows(mut self, rows: MigrationBatchRows) -> Self {
        self.batch_rows = rows;
        self
    }

    /// Replaces the source-byte limit.
    pub const fn with_batch_bytes(mut self, bytes: MigrationBatchBytes) -> Self {
        self.batch_bytes = bytes;
        self
    }

    /// Replaces the active-loop interval.
    pub const fn with_active_interval(mut self, interval: MigrationActiveIntervalMillis) -> Self {
        self.active_interval_millis = interval;
        self
    }

    /// Replaces the active-loop interval.
    pub const fn with_active_interval_millis(
        self,
        interval: MigrationActiveIntervalMillis,
    ) -> Self {
        self.with_active_interval(interval)
    }

    /// Replaces the idle-loop interval.
    pub const fn with_idle_interval(mut self, interval: MigrationIdleIntervalMillis) -> Self {
        self.idle_interval_millis = interval;
        self
    }

    /// Replaces the idle-loop interval.
    pub const fn with_idle_interval_millis(self, interval: MigrationIdleIntervalMillis) -> Self {
        self.with_idle_interval(interval)
    }
}

impl Default for MigrationTuning {
    fn default() -> Self {
        Self {
            worker_mode: MigrationWorkerMode::Background,
            batch_rows: MigrationBatchRows::new(Self::DEFAULT_BATCH_ROWS)
                .expect("default migration row limit is nonzero"),
            batch_bytes: MigrationBatchBytes::new(Self::DEFAULT_BATCH_BYTES)
                .expect("default migration byte limit is nonzero"),
            active_interval_millis: MigrationActiveIntervalMillis::new(
                Self::DEFAULT_ACTIVE_INTERVAL_MILLIS,
            )
            .expect("default migration active interval is nonzero"),
            idle_interval_millis: MigrationIdleIntervalMillis::new(
                Self::DEFAULT_IDLE_INTERVAL_MILLIS,
            )
            .expect("default migration idle interval is nonzero"),
        }
    }
}
