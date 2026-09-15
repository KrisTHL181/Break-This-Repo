//! Snapshot loader — reads pinned JSON deck definitions from
//! `crates/phase-ai/duel_decks/<format>/<file>` and expands them into the
//! `Vec<String>` form consumed by `engine::game::deck_loading`.

use std::fs::File;
use std::io::BufReader;
use std::path::{Path, PathBuf};

use serde::Deserialize;

use super::DeckRef;

/// The root directory for pinned deck snapshots, resolved relative to the
/// `phase-ai` crate. `CARGO_MANIFEST_DIR` is set by Cargo at build time and
/// points at `crates/phase-ai/`.
fn snapshot_root() -> PathBuf {
    PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("duel_decks")
}

#[derive(Debug, Deserialize)]
struct RawEntry {
    name: String,
    count: u32,
}

#[derive(Debug, Deserialize)]
struct RawSnapshot {
    #[allow(dead_code)]
    name: String,
    #[allow(dead_code)]
    format: String,
    #[allow(dead_code)]
    frozen_date: String,
    main: Vec<RawEntry>,
}

/// A parsed snapshot, ready for game setup.
#[derive(Debug, Clone)]
pub struct Snapshot {
    pub cards: Vec<String>,
}

#[derive(Debug)]
pub enum SnapshotError {
    Io(std::io::Error),
    Parse(serde_json::Error),
    Empty(PathBuf),
}

impl std::fmt::Display for SnapshotError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            SnapshotError::Io(e) => write!(f, "snapshot I/O error: {e}"),
            SnapshotError::Parse(e) => write!(f, "snapshot parse error: {e}"),
            SnapshotError::Empty(p) => write!(f, "snapshot {} has no cards", p.display()),
        }
    }
}

impl std::error::Error for SnapshotError {}

impl From<std::io::Error> for SnapshotError {
    fn from(e: std::io::Error) -> Self {
        SnapshotError::Io(e)
    }
}

impl From<serde_json::Error> for SnapshotError {
    fn from(e: serde_json::Error) -> Self {
        SnapshotError::Parse(e)
    }
}

/// Load a snapshot at `<crate>/duel_decks/<format>/<file>` and expand into
/// a flat `Vec<String>` of card names (length = sum of counts).
pub fn load_snapshot(format: &str, file: &str) -> Result<Snapshot, SnapshotError> {
    let path = snapshot_root().join(format).join(file);
    load_snapshot_at(&path)
}

/// Load a snapshot from an absolute path. Used by tests that iterate the
/// registry with a pre-computed path.
pub fn load_snapshot_at(path: &Path) -> Result<Snapshot, SnapshotError> {
    let file = File::open(path)?;
    let raw: RawSnapshot = serde_json::from_reader(BufReader::new(file))?;
    if raw.main.is_empty() {
        return Err(SnapshotError::Empty(path.to_path_buf()));
    }
    let mut cards = Vec::with_capacity(raw.main.iter().map(|e| e.count as usize).sum());
    for entry in raw.main {
        for _ in 0..entry.count {
            cards.push(entry.name.clone());
        }
    }
    Ok(Snapshot { cards })
}

/// Resolve a `DeckRef` into a flat `Vec<String>` of card names.
pub fn resolve_deck_ref(deck: &DeckRef) -> Result<Vec<String>, SnapshotError> {
    match deck {
        DeckRef::Snapshot { format, file } => Ok(load_snapshot(format, file)?.cards),
        DeckRef::Inline { build, .. } => Ok(build()),
    }
}

/// Return the absolute filesystem path for a snapshot `DeckRef`. Returns
/// `None` for inline decks.
pub fn snapshot_path(deck: &DeckRef) -> Option<PathBuf> {
    match deck {
        DeckRef::Snapshot { format, file } => Some(snapshot_root().join(format).join(file)),
        DeckRef::Inline { .. } => None,
    }
}
