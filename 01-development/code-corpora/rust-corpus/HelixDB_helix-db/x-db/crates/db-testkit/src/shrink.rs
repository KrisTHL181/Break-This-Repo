//! Deterministic request-granularity shrinking and regression corpus storage.

use std::fs;
use std::path::{Path, PathBuf};

use sha2::{Digest, Sha256};

use crate::trace::ReplayTrace;
use crate::Result;

/// Result of deterministic delta debugging.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ShrinkReport {
    original_requests: usize,
    minimized: ReplayTrace,
}

impl ShrinkReport {
    /// Returns the original request count.
    pub const fn original_requests(&self) -> usize {
        self.original_requests
    }

    /// Borrows the minimized failing trace.
    pub fn minimized(&self) -> &ReplayTrace {
        &self.minimized
    }

    /// Consumes the report into the minimized trace.
    pub fn into_minimized(self) -> ReplayTrace {
        self.minimized
    }
}

/// Deterministic delta debugger that preserves trace validity.
#[derive(Debug, Default)]
pub struct TraceShrinker;

impl TraceShrinker {
    /// Produces a one-minimal request subsequence for the supplied failure predicate.
    ///
    /// Invalid lifecycle subsequences are discarded rather than passed to the
    /// predicate, so shrinking cannot manufacture an illegal reproducer.
    pub fn shrink<F>(&self, trace: &ReplayTrace, mut still_fails: F) -> Result<ShrinkReport>
    where
        F: FnMut(&ReplayTrace) -> bool,
    {
        trace.validate()?;
        if !still_fails(trace) {
            return Ok(ShrinkReport {
                original_requests: trace.requests().len(),
                minimized: trace.clone(),
            });
        }
        let original_requests = trace.requests().len();
        let seed = trace.seed();
        let mut current = trace.clone();
        let mut granularity = 2_usize;
        while !current.requests().is_empty() {
            let len = current.requests().len();
            let chunk_size = len.div_ceil(granularity);
            let mut reduced = false;
            let mut start = 0_usize;
            while start < len {
                let end = start.saturating_add(chunk_size).min(len);
                let requests = current
                    .requests()
                    .iter()
                    .enumerate()
                    .filter(|(index, _)| *index < start || *index >= end)
                    .map(|(_, request)| request.clone())
                    .collect();
                let Ok(candidate) = ReplayTrace::try_new(seed, requests) else {
                    start = end;
                    continue;
                };
                if still_fails(&candidate) {
                    current = candidate;
                    granularity = granularity.saturating_sub(1).max(2);
                    reduced = true;
                    break;
                }
                start = end;
            }
            if reduced {
                continue;
            }
            if granularity >= len {
                break;
            }
            granularity = granularity.saturating_mul(2).min(len);
        }
        Ok(ShrinkReport {
            original_requests,
            minimized: current,
        })
    }
}

/// Filesystem-backed minimized regression corpus.
#[derive(Debug, Clone)]
pub struct FileRegressionCorpus {
    root: PathBuf,
}

impl FileRegressionCorpus {
    /// Creates a corpus rooted at `path`.
    pub fn new(path: impl Into<PathBuf>) -> Self {
        Self { root: path.into() }
    }

    /// Borrows the corpus root.
    pub fn root(&self) -> &Path {
        &self.root
    }

    /// Saves a validated trace under a content-addressed stable filename.
    pub fn save(&self, trace: &ReplayTrace) -> Result<PathBuf> {
        let bytes = trace.to_json()?;
        let digest = Sha256::digest(&bytes);
        let digest = digest
            .iter()
            .take(12)
            .map(|byte| format!("{byte:02x}"))
            .collect::<String>();
        let path = self
            .root
            .join(format!("seed-{}-{digest}.json", trace.seed().get()));
        fs::create_dir_all(&self.root)?;
        if path.exists() {
            let existing = fs::read(&path)?;
            if existing == bytes {
                return Ok(path);
            }
        }
        fs::write(&path, bytes)?;
        Ok(path)
    }

    /// Loads and validates one corpus entry.
    pub fn load(&self, path: impl AsRef<Path>) -> Result<ReplayTrace> {
        ReplayTrace::from_json(&fs::read(path)?)
    }
}

#[cfg(test)]
mod tests {
    use crate::action::{Action, ElementKind, ReadAction};
    use crate::ids::{EntityId, RequestId, RuntimeId, Sequence, StableSeed, TenantId};
    use crate::trace::{ObservedValue, TraceOutcome, TraceRecorder};

    use super::*;

    fn trace() -> ReplayTrace {
        let mut recorder = TraceRecorder::new(StableSeed::new(99));
        for id in 1..=3 {
            let pending = recorder
                .start_request(
                    RequestId::new(id).unwrap(),
                    RuntimeId::new(1).unwrap(),
                    TenantId::try_new("tenant").unwrap(),
                    Sequence::initial(),
                    Action::Read(ReadAction::Point {
                        kind: ElementKind::Node,
                        id: EntityId::new(id),
                    }),
                )
                .unwrap();
            recorder.finish_request(
                pending,
                None,
                TraceOutcome::Success(ObservedValue::Entities(Vec::new())),
            );
        }
        recorder.finish().unwrap()
    }

    #[test]
    fn shrinker_finds_a_one_minimal_valid_request_subsequence() {
        let trace = trace();
        let report = TraceShrinker
            .shrink(&trace, |candidate| {
                candidate
                    .requests()
                    .iter()
                    .any(|request| request.start.request_id.get() == 2)
            })
            .unwrap();
        assert_eq!(report.original_requests(), 3);
        assert_eq!(report.minimized().requests().len(), 1);
        assert_eq!(report.minimized().requests()[0].start.request_id.get(), 2);
    }

    #[test]
    fn corpus_names_by_seed_and_content_and_round_trips() {
        let directory = tempfile::tempdir().unwrap();
        let corpus = FileRegressionCorpus::new(directory.path());
        let trace = trace();
        let path = corpus.save(&trace).unwrap();
        assert!(path
            .file_name()
            .unwrap()
            .to_string_lossy()
            .starts_with("seed-99-"));
        assert_eq!(corpus.save(&trace).unwrap(), path);
        assert_eq!(corpus.load(path).unwrap(), trace);
    }
}
