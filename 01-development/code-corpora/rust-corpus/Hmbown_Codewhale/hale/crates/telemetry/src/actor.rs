//! The background writer.
//!
//! One dedicated OS thread behind an unbounded `mpsc`. `record()` is a
//! non-blocking `send` and a hard no-op when the process is not armed;
//! everything the thread does is wrapped in `catch_unwind`, so a panic inside
//! telemetry costs telemetry and nothing else.
//!
//! A plain thread rather than a `tokio` task, deliberately: `init` is called
//! from six subcommand dispatch points, several of which have no runtime yet,
//! and a telemetry subsystem that only works when someone remembered to be
//! inside an executor is a subsystem that silently collects nothing on half its
//! surfaces.

use std::panic::{AssertUnwindSafe, catch_unwind};
use std::path::PathBuf;
use std::sync::mpsc::{Receiver, RecvTimeoutError, Sender, SyncSender, channel, sync_channel};
use std::time::Duration;

use crate::buffer;
use crate::client::{self, SendOutcome};
use crate::decision::{self, TelemetryDecision};
use crate::envelope;
use crate::event::{Batch, Event, SCHEMA_VERSION, Surface};

/// Events per batch.
pub const BATCH_MAX_EVENTS: usize = 200;
/// Byte ceiling per batch body.
pub const BATCH_MAX_BYTES: usize = 64 * 1024;

pub(crate) enum Message {
    Event(Box<Event>),
    PersistLocal(SyncSender<FlushOutcome>),
    Shutdown(SyncSender<FlushOutcome>),
}

/// What a flush attempt did.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum FlushOutcome {
    /// Nothing was buffered.
    Empty,
    /// Events remain in the local buffer for a later network flush.
    Buffered,
    /// A batch was written to the dry-run sink.
    DryRun,
    /// A batch was accepted by the endpoint.
    Sent,
    /// A batch was assembled and dropped — offline, refused, or contended.
    Dropped,
    /// Telemetry was off by the time the flush ran; nothing was sent.
    Suppressed,
    /// The actor did not answer inside the caller's deadline.
    TimedOut,
}

/// Facts the writer thread needs, fixed at arming time.
#[derive(Debug, Clone)]
pub(crate) struct Context {
    pub root: PathBuf,
    pub endpoint: Option<String>,
    pub surface: Surface,
    pub config_path: Option<PathBuf>,
    pub app_version: String,
    pub git_sha: Option<String>,
    pub tty: bool,
}

/// A handle to the writer thread.
#[derive(Debug)]
pub(crate) struct Handle {
    tx: Sender<Message>,
}

impl Handle {
    /// Start the writer thread.
    pub(crate) fn spawn(context: Context) -> Self {
        let (tx, rx) = channel::<Message>();
        // A detached thread: nothing joins it, and the process exiting while it
        // is mid-write is exactly the case the torn-line tolerance covers.
        let _ = std::thread::Builder::new()
            .name("codewhale-telemetry".to_string())
            .spawn(move || run(&context, &rx));
        Self { tx }
    }

    /// Queue an event. Never blocks, never errors upward.
    pub(crate) fn record(&self, event: Event) {
        let _ = self.tx.send(Message::Event(Box::new(event)));
    }

    /// Ask for a final flush and stop the thread.
    pub(crate) fn shutdown(&self, deadline: Duration) -> FlushOutcome {
        self.round_trip(deadline, Message::Shutdown)
    }

    /// Persist queued events locally and stop without making a network request.
    pub(crate) fn persist_local(&self, deadline: Duration) -> FlushOutcome {
        self.round_trip(deadline, Message::PersistLocal)
    }

    fn round_trip(
        &self,
        deadline: Duration,
        build: impl FnOnce(SyncSender<FlushOutcome>) -> Message,
    ) -> FlushOutcome {
        let (ack_tx, ack_rx) = sync_channel::<FlushOutcome>(1);
        if self.tx.send(build(ack_tx)).is_err() {
            return FlushOutcome::TimedOut;
        }
        match ack_rx.recv_timeout(deadline) {
            Ok(outcome) => outcome,
            Err(RecvTimeoutError::Timeout | RecvTimeoutError::Disconnected) => {
                FlushOutcome::TimedOut
            }
        }
    }
}

fn run(context: &Context, rx: &Receiver<Message>) {
    while let Ok(message) = rx.recv() {
        // Telemetry never takes the process with it. The hook has already been
        // installed by the time this thread exists, so a panic here is caught,
        // dropped, and the loop continues.
        let result = catch_unwind(AssertUnwindSafe(|| match message {
            Message::Event(event) => {
                append(context, &event);
                None
            }
            Message::PersistLocal(ack) => {
                let _ = ack.send(persist_local(context));
                Some(())
            }
            Message::Shutdown(ack) => {
                let _ = ack.send(flush(context));
                Some(())
            }
        }));
        match result {
            Ok(Some(())) => return,
            Ok(None) => {}
            Err(_) => {
                tracing::debug!("telemetry writer recovered from a panic");
            }
        }
    }
}

fn append(context: &Context, event: &Event) {
    let Ok(line) = serde_json::to_string(event) else {
        return;
    };
    let path = buffer::buffer_path(&context.root);
    let _ = buffer::append(&context.root, &path, &line);
}

/// Seal every event queued before this message without reaching the network.
///
/// The channel is FIFO, so all prior event appends have settled before this
/// runs. Re-deciding here preserves the same mid-session opt-out boundary as a
/// full flush. An explicitly empty endpoint is the local dry-run sink and can
/// therefore be finalized immediately; a configured endpoint leaves the
/// events in `buffer.jsonl` for the next interactive flush.
fn persist_local(context: &Context) -> FlushOutcome {
    match decision::re_decide(context.config_path.as_deref(), context.surface) {
        TelemetryDecision::Enabled(_) => {}
        TelemetryDecision::OptedOut | TelemetryDecision::ForcedOff => {
            return FlushOutcome::Suppressed;
        }
    }
    if buffer::tombstone_present(&context.root) {
        return FlushOutcome::Suppressed;
    }
    if context.endpoint.is_none() {
        return flush(context);
    }
    if buffer::read_lines(&buffer::buffer_path(&context.root)).is_empty() {
        FlushOutcome::Empty
    } else {
        FlushOutcome::Buffered
    }
}

/// Drain, re-check consent, and deliver.
///
/// The re-check is the point: telemetry is resolved once at init, but the
/// documented mid-session opt-out is an external file write this process would
/// otherwise never observe. If the answer is now `OptedOut`, `decide` has
/// already wiped and left the tombstone, and the drained events go nowhere.
fn flush(context: &Context) -> FlushOutcome {
    match decision::re_decide(context.config_path.as_deref(), context.surface) {
        TelemetryDecision::Enabled(_) => {}
        TelemetryDecision::OptedOut | TelemetryDecision::ForcedOff => {
            return FlushOutcome::Suppressed;
        }
    }
    if buffer::tombstone_present(&context.root) {
        return FlushOutcome::Suppressed;
    }

    let lines = buffer::drain(&context.root);
    if lines.is_empty() {
        return FlushOutcome::Empty;
    }
    let events = parse_events(&lines);
    if events.is_empty() {
        return FlushOutcome::Empty;
    }

    let Ok(install) = envelope::read_or_create_install_id(&context.root) else {
        return FlushOutcome::Dropped;
    };

    let mut state = envelope::read_state(&context.root);
    state.schema_version = SCHEMA_VERSION;
    state.last_flush = Some(envelope::now_rfc3339());
    // Written on attempt, not on success, so a permanently offline machine
    // attempts at most once per interval rather than on every launch.
    let _ = envelope::write_state(&context.root, &state);

    let batch = Batch {
        schema_version: SCHEMA_VERSION,
        notice_version: crate::event::NOTICE_VERSION,
        sent_at: envelope::now_rfc3339(),
        install_id: install.install_id,
        app_version: context.app_version.clone(),
        git_sha: context.git_sha.clone(),
        surface: context.surface,
        os: envelope::current_os(),
        arch: envelope::current_arch(),
        libc: envelope::current_libc(),
        tty: context.tty,
        events,
    };

    match client::send(&context.root, context.endpoint.as_deref(), &batch) {
        SendOutcome::DryRun => FlushOutcome::DryRun,
        SendOutcome::Accepted => FlushOutcome::Sent,
        SendOutcome::Dropped => FlushOutcome::Dropped,
    }
}

/// Parse drained lines into events, capped at [`BATCH_MAX_EVENTS`] and
/// [`BATCH_MAX_BYTES`], skipping anything that does not parse **or does not
/// satisfy its declared string bounds**.
///
/// The bound re-check is the point. Everything upstream of here builds events
/// from closed enums, `u32`s, and two reducers — but this function is a
/// deserializer, and its input is a file on disk that any process running as
/// the user can append to. `Event::is_bounded` is what stops
/// `{"event":"panic","site":"<anything at all>"}` from becoming a first-party
/// POST under the user's install id.
pub(crate) fn parse_events(lines: &[String]) -> Vec<Event> {
    let mut events = Vec::new();
    let mut bytes = 0usize;
    for line in lines {
        if events.len() >= BATCH_MAX_EVENTS || bytes + line.len() > BATCH_MAX_BYTES {
            break;
        }
        if let Ok(event) = serde_json::from_str::<Event>(line) {
            if !event.is_bounded() {
                tracing::debug!("telemetry dropped an out-of-bounds buffered event");
                continue;
            }
            bytes += line.len();
            events.push(event);
        }
    }
    events
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn local_persistence_times_out_behind_a_stalled_writer() {
        // Hold the receiver alive without consuming it: the writer cannot
        // acknowledge the FIFO barrier. No sleep or worker scheduling race.
        let (tx, rx) = channel();
        let handle = Handle { tx };
        handle.record(Event::SessionStart {
            source: crate::SessionSource::Unknown,
        });
        let started = std::time::Instant::now();
        assert_eq!(
            handle.persist_local(crate::CLI_PERSIST_TIMEOUT),
            FlushOutcome::TimedOut
        );
        assert!(started.elapsed() >= crate::CLI_PERSIST_TIMEOUT);
        // A generous liveness watchdog, separate from the unchanged 250ms
        // production budget, avoids timing a busy test scheduler as the writer.
        assert!(started.elapsed() < Duration::from_secs(5));

        assert!(matches!(rx.try_recv(), Ok(Message::Event(_))));
        let Message::PersistLocal(ack) = rx.try_recv().expect("local persistence request") else {
            panic!("short CLI persistence must not request a network shutdown flush");
        };
        assert!(
            ack.send(FlushOutcome::Buffered).is_err(),
            "a late acknowledgement must not keep the exiting caller alive"
        );
        assert!(
            rx.try_recv().is_err(),
            "the timeout must not enqueue a retry"
        );
    }
}
