//! Pluggable completion service.
//!
//! This module provides a framework for composing multiple completion sources
//! (providers) into a single, ranked completion experience. It ships with
//! two built-in providers and supports both Rust-native and TypeScript plugin
//! providers.
//!
//! # Architecture
//!
//! ```text
//!  ┌──────────────────────────────────────────────────┐
//!  │                CompletionService                  │
//!  │  ┌────────────┐ ┌──────────┐ ┌───────────────┐  │
//!  │  │ LSP        │ │ dabbrev  │ │ buffer_words  │  │
//!  │  │ (async)    │ │ (sync)   │ │ (sync)        │  │
//!  │  └────────────┘ └──────────┘ └───────────────┘  │
//!  │  ┌────────────────────────────────────────────┐  │
//!  │  │  TS plugin providers  (async via QuickJS)  │  │
//!  │  └────────────────────────────────────────────┘  │
//!  │                                                  │
//!  │  merge → rank → deduplicate → popup / ghost text │
//!  └──────────────────────────────────────────────────┘
//! ```
//!
//! # Rust core vs TypeScript plugins
//!
//! | Concern | Where | Why |
//! |---------|-------|-----|
//! | `CompletionProvider` trait, `CompletionService` orchestrator | Rust | Zero-overhead dispatch, direct `&[u8]` buffer access |
//! | dabbrev scan, buffer-word proximity scoring | Rust | Hot-path byte-level scanning must stay < 1 ms |
//! | Fuzzy matching / Smith-Waterman scoring | Rust | O(mn) matrix work needs SIMD-friendly code |
//! | LSP bridge (send request, receive response) | Rust | Already integrated, async I/O via tokio |
//! | Static index (ctags-style) lookup | Rust | Sub-ms binary search on a sorted Vec |
//! | Custom snippet / dictionary providers | TypeScript | Extensibility; content is small, latency tolerant |
//! | Provider registration / lifecycle | TypeScript API | Plugins call `registerCompletionProvider()` |
//! | Ghost-text rendering decision | Rust (view layer) | Must be frame-synchronous |
//!
//! # Huge-file contract
//!
//! The service computes a **scan window** before calling any provider:
//!
//! - Normal files (< 100 MB): 512 KB radius around cursor.
//! - Large files (≥ 100 MB, lazily loaded): 32 KB radius.
//!
//! Providers receive only the pre-sliced `&[u8]` and the `CompletionContext`
//! which documents the valid byte range. This makes it structurally
//! impossible for a provider to trigger an expensive full-buffer scan.

pub mod buffer_words;
pub mod dabbrev;
pub mod provider;
pub mod service;

// Re-export the main types that the Editor needs.
pub use provider::{
    CompletionCandidate, CompletionContext, CompletionProvider, CompletionSourceId,
    OtherBufferSlice, ProviderResult,
};
pub use service::CompletionService;
