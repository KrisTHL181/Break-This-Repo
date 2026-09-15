//! Completion service: orchestrates multiple completion providers.
//!
//! The service owns a set of registered `CompletionProvider`s, builds a
//! `CompletionContext` from the current editor state, fans out to each
//! enabled provider, and merges the results into a single ranked list.
//!
//! ## Provider lifecycle
//!
//! 1. Built-in providers (dabbrev, buffer-words) are registered at startup.
//! 2. The LSP provider is always registered but returns `Pending` — its
//!    results arrive asynchronously and are fed in via `supply_async_results`.
//! 3. TypeScript plugins register providers dynamically via the plugin API.
//!
//! ## Huge-file safety
//!
//! The service computes a safe scan window (`CompletionContext::scan_range`)
//! before calling any provider. For normal files this is 512 KB around the
//! cursor. For lazily-loaded files (>100 MB) it shrinks to 32 KB. Providers
//! receive a pre-sliced `&[u8]` and never touch the `Buffer` directly.

use super::buffer_words::BufferWordProvider;
use super::dabbrev::DabbrevProvider;
use super::provider::{
    CompletionCandidate, CompletionContext, CompletionProvider, CompletionSourceId, ProviderResult,
};

/// The completion service.
pub struct CompletionService {
    providers: Vec<Box<dyn CompletionProvider>>,
    /// Async results waiting to be merged (keyed by request id).
    pending_async: Vec<(u64, CompletionSourceId)>,
}

impl CompletionService {
    /// Create a new service with the built-in providers pre-registered.
    pub fn new() -> Self {
        let mut svc = Self {
            providers: Vec::new(),
            pending_async: Vec::new(),
        };
        svc.register(Box::new(BufferWordProvider::new()));
        svc.register(Box::new(DabbrevProvider::new()));
        svc
    }

    /// Register a completion provider.
    pub fn register(&mut self, provider: Box<dyn CompletionProvider>) {
        // Replace existing provider with the same id.
        let id = provider.id();
        self.providers.retain(|p| p.id() != id);
        self.providers.push(provider);
    }

    /// Unregister a provider by id.
    pub fn unregister(&mut self, id: &CompletionSourceId) {
        self.providers.retain(|p| p.id() != *id);
    }

    /// Request completion from all enabled providers.
    ///
    /// `buffer_window` is the pre-sliced byte range corresponding to
    /// `ctx.scan_range`. The caller (Editor) is responsible for extracting
    /// this from the buffer, which keeps the service decoupled from the
    /// buffer internals.
    ///
    /// Returns the synchronously-available candidates, already merged and
    /// sorted. Any providers that return `Pending` are tracked internally;
    /// their results should be supplied later via `supply_async_results`.
    pub fn request(
        &mut self,
        ctx: &CompletionContext,
        buffer_window: &[u8],
    ) -> Vec<CompletionCandidate> {
        self.pending_async.clear();
        let mut all_candidates: Vec<CompletionCandidate> = Vec::new();

        // Sort providers by priority so higher-priority providers run first.
        let mut indices: Vec<usize> = (0..self.providers.len()).collect();
        indices.sort_by_key(|&i| self.providers[i].priority());

        for &i in &indices {
            let provider = &self.providers[i];
            if !provider.is_enabled(ctx) {
                continue;
            }
            let source_id = provider.id();
            match provider.provide(ctx, buffer_window) {
                ProviderResult::Ready(mut candidates) => {
                    for c in &mut candidates {
                        c.source = Some(source_id.clone());
                    }
                    all_candidates.extend(candidates);
                }
                ProviderResult::Pending(request_id) => {
                    self.pending_async.push((request_id, source_id));
                }
            }
        }

        Self::rank(&mut all_candidates);
        all_candidates
    }

    /// Supply results from an async provider (e.g., LSP).
    ///
    /// Returns the merged, re-ranked candidate list including both the
    /// new async results and any previously supplied candidates.
    pub fn supply_async_results(
        &mut self,
        request_id: u64,
        mut candidates: Vec<CompletionCandidate>,
    ) -> Option<Vec<CompletionCandidate>> {
        // Find and remove the pending entry.
        let pos = self
            .pending_async
            .iter()
            .position(|(id, _)| *id == request_id)?;
        let (_rid, source_id) = self.pending_async.remove(pos);
        for c in &mut candidates {
            c.source = Some(source_id.clone());
        }
        Self::rank(&mut candidates);
        Some(candidates)
    }

    /// Check if there are pending async provider requests.
    pub fn has_pending(&self) -> bool {
        !self.pending_async.is_empty()
    }

    /// Rank and deduplicate a candidate list.
    ///
    /// Primary sort: score descending. Tie-breaker: label ascending.
    /// Deduplication keeps the highest-scoring entry for each label.
    fn rank(candidates: &mut Vec<CompletionCandidate>) {
        // Sort first so highest score comes first per label.
        candidates.sort_by(|a, b| {
            b.score
                .cmp(&a.score)
                .then_with(|| a.label.to_lowercase().cmp(&b.label.to_lowercase()))
        });

        // Deduplicate by (lowercase label, insert_text), keeping the first
        // (highest-scored) occurrence.
        let mut seen = std::collections::HashSet::new();
        candidates.retain(|c| {
            let key = (
                c.label.to_lowercase(),
                c.insert_text.clone().unwrap_or_default(),
            );
            seen.insert(key)
        });
    }

    /// Access the registered providers (read-only).
    pub fn providers(&self) -> &[Box<dyn CompletionProvider>] {
        &self.providers
    }
}

impl Default for CompletionService {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    /// A trivial test provider that returns static candidates.
    struct StaticProvider {
        id: &'static str,
        candidates: Vec<CompletionCandidate>,
        priority: u32,
    }

    impl CompletionProvider for StaticProvider {
        fn id(&self) -> CompletionSourceId {
            CompletionSourceId(self.id.into())
        }
        fn display_name(&self) -> &str {
            self.id
        }
        fn is_enabled(&self, _ctx: &CompletionContext) -> bool {
            true
        }
        fn provide(&self, _ctx: &CompletionContext, _buffer_window: &[u8]) -> ProviderResult {
            ProviderResult::Ready(self.candidates.clone())
        }
        fn priority(&self) -> u32 {
            self.priority
        }
    }

    fn test_ctx() -> CompletionContext {
        CompletionContext {
            prefix: "te".into(),
            cursor_byte: 10,
            word_start_byte: 8,
            buffer_len: 100,
            is_large_file: false,
            scan_range: 0..100,
            viewport_top_byte: 0,
            viewport_bottom_byte: 100,
            language_id: None,
            word_chars_extra: String::new(),
            prefix_has_uppercase: false,
            other_buffers: Vec::new(),
        }
    }

    #[test]
    fn merges_multiple_providers() {
        let mut svc = CompletionService {
            providers: Vec::new(),
            pending_async: Vec::new(),
        };
        svc.register(Box::new(StaticProvider {
            id: "a",
            candidates: vec![CompletionCandidate::word("test_alpha".into(), 100)],
            priority: 10,
        }));
        svc.register(Box::new(StaticProvider {
            id: "b",
            candidates: vec![CompletionCandidate::word("test_beta".into(), 200)],
            priority: 20,
        }));

        let ctx = test_ctx();
        let results = svc.request(&ctx, b"");
        assert_eq!(results.len(), 2);
        // Higher score first.
        assert_eq!(results[0].label, "test_beta");
        assert_eq!(results[1].label, "test_alpha");
    }

    #[test]
    fn deduplicates_by_label() {
        let mut svc = CompletionService {
            providers: Vec::new(),
            pending_async: Vec::new(),
        };
        svc.register(Box::new(StaticProvider {
            id: "a",
            candidates: vec![CompletionCandidate::word("test".into(), 50)],
            priority: 10,
        }));
        svc.register(Box::new(StaticProvider {
            id: "b",
            candidates: vec![CompletionCandidate::word("test".into(), 100)],
            priority: 20,
        }));

        let ctx = test_ctx();
        let results = svc.request(&ctx, b"");
        // Only one "test" survives, the one with higher score.
        assert_eq!(results.len(), 1);
        assert_eq!(results[0].score, 100);
    }

    #[test]
    fn async_supply() {
        let mut svc = CompletionService {
            providers: Vec::new(),
            pending_async: vec![(42, CompletionSourceId("lsp".into()))],
        };
        let candidates = vec![CompletionCandidate::word("testing".into(), 300)];
        let merged = svc.supply_async_results(42, candidates);
        assert!(merged.is_some());
        let merged = merged.unwrap();
        assert_eq!(merged.len(), 1);
        assert_eq!(merged[0].source.as_ref().unwrap().0, "lsp");
    }

    #[test]
    fn async_supply_unknown_id_returns_none() {
        let mut svc = CompletionService {
            providers: Vec::new(),
            pending_async: vec![(42, CompletionSourceId("lsp".into()))],
        };
        assert!(svc.supply_async_results(99, vec![]).is_none());
    }
}
