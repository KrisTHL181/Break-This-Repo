//! Reader for the TUI's own `codewhale_tui::view_stack` trace records.
//!
//! Modal open/close coverage has an honesty problem: "the frame changed after
//! I pressed F1" is not evidence that a modal opened, and "the frame changed
//! back after Esc" is not evidence that it closed rather than being replaced.
//! `ViewStack::push` and its close paths already emit structured records with
//! the `ModalKind` and the resulting depth, so this reader consumes the
//! product's existing machine-readable signal instead of inventing a parallel
//! one for tests.
//!
//! Enable it by spawning the binary with
//! `RUST_LOG=warn,codewhale_tui::view_stack=debug` and a sealed `HOME`; the
//! subscriber writes to `$HOME/.codewhale/logs/tui-<date>-<pid>.log`.

use std::path::{Path, PathBuf};
use std::time::{Duration, Instant};

use anyhow::{Context, Result, anyhow};

pub const VIEW_STACK_RUST_LOG: &str = "warn,codewhale_tui::view_stack=debug";

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ViewEvent {
    /// `push`, `push_boxed`, `pop`, `close`, or `emit_and_close`, verbatim
    /// from the record.
    pub action: String,
    /// Debug spelling of the `ModalKind`, e.g. `CommandPalette`.
    pub kind: String,
    /// Stack depth *after* the transition, as the product reported it.
    pub depth: usize,
}

impl ViewEvent {
    pub fn is_open(&self) -> bool {
        self.action.starts_with("push")
    }

    pub fn is_close(&self) -> bool {
        matches!(self.action.as_str(), "pop" | "close" | "emit_and_close")
    }
}

/// Locate the log file the sealed-`HOME` child is writing to. Returns the most
/// recently modified `tui-*.log` so a re-spawned process in the same sealed
/// home does not resolve to a stale file.
pub fn log_path(home: &Path) -> Result<PathBuf> {
    let dir = home.join(".codewhale").join("logs");
    let mut newest: Option<(std::time::SystemTime, PathBuf)> = None;
    for entry in
        std::fs::read_dir(&dir).with_context(|| format!("read sealed log dir {}", dir.display()))?
    {
        let entry = entry?;
        let path = entry.path();
        let is_tui_log = path
            .file_name()
            .and_then(|name| name.to_str())
            .is_some_and(|name| name.starts_with("tui-") && name.ends_with(".log"));
        if !is_tui_log {
            continue;
        }
        let modified = entry.metadata()?.modified()?;
        if newest.as_ref().is_none_or(|(seen, _)| modified >= *seen) {
            newest = Some((modified, path));
        }
    }
    newest
        .map(|(_, path)| path)
        .ok_or_else(|| anyhow!("no tui-*.log under {}", dir.display()))
}

/// Parse every view-stack transition currently on disk, in order.
pub fn read_events(home: &Path) -> Result<Vec<ViewEvent>> {
    let path = log_path(home)?;
    let contents =
        std::fs::read_to_string(&path).with_context(|| format!("read {}", path.display()))?;
    Ok(parse_events(&contents))
}

/// Poll the log until at least `count` transitions are visible, or fail with
/// the transitions that *were* observed. The subscriber writes on its own
/// schedule, so a modal that has already repainted may not have been flushed
/// yet; this is a bounded wait on a real signal, never a fixed sleep.
pub fn wait_for_events(home: &Path, count: usize, timeout: Duration) -> Result<Vec<ViewEvent>> {
    let budget = super::harness::ci_scaled(timeout);
    let deadline = Instant::now() + budget;
    let mut last: Vec<ViewEvent> = Vec::new();
    loop {
        if let Ok(events) = read_events(home) {
            last = events;
        }
        if last.len() >= count {
            return Ok(last);
        }
        if Instant::now() >= deadline {
            return Err(anyhow!(
                "view-stack log never reached {count} transitions within {budget:?}; observed {:?}",
                last
            ));
        }
        std::thread::sleep(Duration::from_millis(40));
    }
}

/// Poll until a newly appended transition satisfies `predicate`. Some views
/// are temporarily popped and restored while opening, so counting one record
/// per user gesture is not a stable contract; the semantic transition is.
pub fn wait_for_event_after<F>(
    home: &Path,
    after: usize,
    timeout: Duration,
    mut predicate: F,
) -> Result<(Vec<ViewEvent>, ViewEvent)>
where
    F: FnMut(&ViewEvent) -> bool,
{
    let budget = super::harness::ci_scaled(timeout);
    let deadline = Instant::now() + budget;
    let mut last: Vec<ViewEvent> = Vec::new();
    loop {
        if let Ok(events) = read_events(home) {
            last = events;
        }
        if let Some(event) = last.iter().skip(after).find(|event| predicate(event)) {
            return Ok((last.clone(), event.clone()));
        }
        if Instant::now() >= deadline {
            return Err(anyhow!(
                "view-stack log produced no matching transition after index {after} within \
                 {budget:?}; observed {:?}",
                last
            ));
        }
        std::thread::sleep(Duration::from_millis(40));
    }
}

pub fn parse_events(contents: &str) -> Vec<ViewEvent> {
    contents
        .lines()
        .filter(|line| line.contains("codewhale_tui::view_stack"))
        .filter_map(parse_line)
        .collect()
}

fn parse_line(line: &str) -> Option<ViewEvent> {
    let action = quoted_field(line, "action=")?;
    let kind = bare_field(line, "kind=")?;
    let depth = bare_field(line, "depth=")?.parse().ok()?;
    Some(ViewEvent {
        action,
        kind,
        depth,
    })
}

/// `action="push"` → `push`.
fn quoted_field(line: &str, key: &str) -> Option<String> {
    let rest = line.split_once(key)?.1;
    let rest = rest.strip_prefix('"')?;
    let end = rest.find('"')?;
    Some(rest[..end].to_string())
}

/// `kind=CommandPalette depth=1` → `CommandPalette`.
fn bare_field(line: &str, key: &str) -> Option<String> {
    let rest = line.split_once(key)?.1;
    let end = rest.find(|c: char| c.is_whitespace()).unwrap_or(rest.len());
    let value = rest[..end].trim();
    if value.is_empty() {
        return None;
    }
    Some(value.to_string())
}

#[cfg(test)]
mod tests {
    use super::*;

    const SAMPLE: &str = concat!(
        "2026-07-26T12:00:00.100000Z  WARN codewhale_tui::startup: unrelated line\n",
        "2026-07-26T12:00:01.000000Z DEBUG codewhale_tui::view_stack: view pushed action=\"push\" kind=Help depth=1\n",
        "2026-07-26T12:00:02.000000Z DEBUG codewhale_tui::view_stack: view pushed action=\"push_boxed\" kind=Pager depth=2\n",
        "2026-07-26T12:00:03.000000Z DEBUG codewhale_tui::view_stack: view closed action=\"close\" kind=Pager depth=1\n",
    );

    #[test]
    fn only_view_stack_records_are_parsed_and_order_is_preserved() {
        let events = parse_events(SAMPLE);

        assert_eq!(events.len(), 3);
        assert_eq!(events[0].kind, "Help");
        assert!(events[0].is_open());
        assert_eq!(events[1].action, "push_boxed");
        assert!(events[1].is_open());
        assert!(events[2].is_close());
        assert_eq!(events[2].depth, 1);
    }

    #[test]
    fn a_record_missing_its_fields_is_skipped_rather_than_guessed() {
        let events = parse_events(
            "2026-07-26T12:00:01.000000Z DEBUG codewhale_tui::view_stack: view pushed depth=1\n",
        );

        assert!(events.is_empty());
    }
}
