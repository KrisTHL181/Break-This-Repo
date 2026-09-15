//! Dump the scenario corpus to JSON for external drivers.
//!
//! Run with `--include-ignored` (or `cargo test corpus_dump --
//! --ignored`) to write `target/scenario-corpus.json`. The job is
//! `#[ignore]`d so that normal `cargo test` runs are not slowed by
//! filesystem writes; CI runs it explicitly to publish the corpus
//! as a build artifact.

use crate::semantic::corpus;
use std::path::PathBuf;

#[test]
#[ignore = "writes a file; run explicitly with --include-ignored"]
fn dump_scenario_corpus_json() {
    let scenarios = corpus::buffer_scenarios();
    let payload = serde_json::to_string_pretty(&scenarios).expect("serialise scenario corpus");
    let path = target_dir().join("scenario-corpus.json");
    std::fs::create_dir_all(path.parent().unwrap()).expect("create target dir");
    std::fs::write(&path, payload).expect("write corpus JSON");
    eprintln!("wrote {} scenarios to {}", scenarios.len(), path.display());
}

/// Round-trip every corpus scenario through serde_json so a schema
/// change that breaks deserialisation is caught even when the
/// `dump_*` test is not run. This is the gating test referenced by
/// the data-model lockdown phase.
#[test]
fn corpus_round_trips_through_json() {
    let scenarios = corpus::buffer_scenarios();
    for s in &scenarios {
        let json = serde_json::to_string(s).expect("serialise");
        let back: crate::common::scenario::buffer_scenario::BufferScenario =
            serde_json::from_str(&json).expect("deserialise");
        assert_eq!(s.description, back.description);
        assert_eq!(s.initial_text, back.initial_text);
        assert_eq!(s.expected_text, back.expected_text);
    }
}

fn target_dir() -> PathBuf {
    // CARGO_TARGET_TMPDIR is per-test-binary scratch under target/,
    // which is the right place to drop large generated artifacts.
    std::env::var_os("CARGO_TARGET_TMPDIR")
        .map(PathBuf::from)
        .unwrap_or_else(|| PathBuf::from("target"))
}
