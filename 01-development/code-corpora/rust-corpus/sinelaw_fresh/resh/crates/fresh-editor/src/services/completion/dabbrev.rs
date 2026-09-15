//! Dynamic Abbreviation (dabbrev) completion provider.
//!
//! Scans buffer text near the cursor for words that share the typed prefix,
//! ordered by proximity to the cursor (nearest first). This mirrors the
//! behaviour of Emacs `dabbrev-expand` / `hippie-expand`.
//!
//! # Multi-buffer scanning
//!
//! After exhausting the active buffer, the provider searches other open
//! buffers in MRU (most-recently-used) order — exactly like Emacs.
//!
//! # Smart-case matching
//!
//! If the prefix contains uppercase characters, matching is case-sensitive.
//! Otherwise it is case-insensitive but exact-case matches score higher.
//!
//! # Language-aware word boundaries
//!
//! Word constituents respect `ctx.word_chars_extra`, so languages with
//! non-standard identifier characters (e.g., `-` in Lisp, `$` in PHP)
//! tokenise correctly.
//!
//! # Huge-file safety
//!
//! Only the byte window supplied by the completion service (`buffer_window`)
//! is read. For normal files this is up to 512 KB around the cursor; for
//! lazily-loaded huge files it shrinks to 32 KB.
//!
//! # Unicode
//!
//! Word boundaries are detected using Unicode grapheme clusters via the
//! `unicode-segmentation` crate, so identifiers containing accented
//! characters, CJK, or composed emoji sequences are handled correctly.

use std::collections::HashSet;

use unicode_segmentation::UnicodeSegmentation;

use super::provider::{
    case_mismatch_penalty, is_word_grapheme_for_lang, smart_case_matches, CompletionCandidate,
    CompletionContext, CompletionProvider, CompletionSourceId, ProviderResult,
};

/// Maximum number of candidates the dabbrev provider returns.
const MAX_CANDIDATES: usize = 30;

pub struct DabbrevProvider;

impl DabbrevProvider {
    pub fn new() -> Self {
        Self
    }
}

impl Default for DabbrevProvider {
    fn default() -> Self {
        Self::new()
    }
}

/// Extract all word tokens from `text`, returning `(byte_offset, word)` pairs.
///
/// Word boundaries respect the language-specific `extra` characters.
fn extract_words(text: &str, extra: &str) -> Vec<(usize, String)> {
    let mut words = Vec::new();
    let mut current_word = String::new();
    let mut word_start: usize = 0;
    let mut byte_pos: usize = 0;

    for grapheme in text.graphemes(true) {
        if is_word_grapheme_for_lang(grapheme, extra) {
            if current_word.is_empty() {
                word_start = byte_pos;
            }
            current_word.push_str(grapheme);
        } else if !current_word.is_empty() {
            words.push((word_start, std::mem::take(&mut current_word)));
        }
        byte_pos += grapheme.len();
    }
    // Flush trailing word
    if !current_word.is_empty() {
        words.push((word_start, current_word));
    }
    words
}

/// Scan a text buffer for prefix-matching words, ordered by proximity.
///
/// Returns up to `remaining` candidates. `seen` tracks already-emitted
/// words (lowercased) for deduplication across buffers.
fn scan_for_candidates(
    text: &str,
    cursor_in_window: usize,
    ctx: &CompletionContext,
    seen: &mut HashSet<String>,
    remaining: usize,
    base_score_offset: i64,
) -> Vec<CompletionCandidate> {
    let extra = &ctx.word_chars_extra;
    let words = extract_words(text, extra);

    let mut scored: Vec<(usize, &str)> = words
        .iter()
        .filter(|(_, w)| {
            w.len() > ctx.prefix.len()
                && smart_case_matches(w, &ctx.prefix, ctx.prefix_has_uppercase)
        })
        .map(|(off, w)| {
            let dist = off.abs_diff(cursor_in_window);
            (dist, w.as_str())
        })
        .collect();

    scored.sort_by_key(|(dist, _)| *dist);

    let mut candidates = Vec::new();
    for (dist, word) in scored {
        let lower = word.to_lowercase();
        if !seen.insert(lower) {
            continue;
        }
        // Score: higher for closer words. Apply base offset for cross-buffer ordering.
        let mut score = (1_000_000i64 + base_score_offset).saturating_sub(dist as i64);
        score += case_mismatch_penalty(word, &ctx.prefix, ctx.prefix_has_uppercase);
        candidates.push(CompletionCandidate::word(word.to_string(), score));
        if candidates.len() >= remaining {
            break;
        }
    }

    candidates
}

impl CompletionProvider for DabbrevProvider {
    fn id(&self) -> CompletionSourceId {
        CompletionSourceId("dabbrev".into())
    }

    fn display_name(&self) -> &str {
        "Dynamic Abbreviation"
    }

    fn is_enabled(&self, ctx: &CompletionContext) -> bool {
        !ctx.prefix.is_empty()
    }

    fn provide(&self, ctx: &CompletionContext, buffer_window: &[u8]) -> ProviderResult {
        let text = String::from_utf8_lossy(buffer_window);
        let cursor_in_window = ctx.cursor_byte.saturating_sub(ctx.scan_range.start);

        let mut seen = HashSet::new();
        // The word currently being typed should not appear as a candidate.
        seen.insert(ctx.prefix.to_lowercase());

        // Phase 1: Scan the active buffer window.
        let mut candidates =
            scan_for_candidates(&text, cursor_in_window, ctx, &mut seen, MAX_CANDIDATES, 0);

        // Phase 2: Scan other open buffers (MRU order) if we have room.
        for (i, other) in ctx.other_buffers.iter().enumerate() {
            if candidates.len() >= MAX_CANDIDATES {
                break;
            }
            let other_text = String::from_utf8_lossy(&other.bytes);
            let remaining = MAX_CANDIDATES - candidates.len();
            // Cross-buffer candidates get a lower base score so active-buffer
            // results always rank higher at the same distance.
            let base_offset = -200_000 * (i as i64 + 1);
            let more = scan_for_candidates(
                &other_text,
                0, // distance measured from start for other buffers
                ctx,
                &mut seen,
                remaining,
                base_offset,
            );
            candidates.extend(more);
        }

        ProviderResult::Ready(candidates)
    }

    fn priority(&self) -> u32 {
        30
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn make_ctx(prefix: &str, cursor: usize, buf_len: usize) -> CompletionContext {
        CompletionContext {
            prefix: prefix.into(),
            cursor_byte: cursor,
            word_start_byte: cursor.saturating_sub(prefix.len()),
            buffer_len: buf_len,
            is_large_file: false,
            scan_range: 0..buf_len,
            viewport_top_byte: 0,
            viewport_bottom_byte: buf_len,
            language_id: None,
            word_chars_extra: String::new(),
            prefix_has_uppercase: prefix.chars().any(|c| c.is_uppercase()),
            other_buffers: Vec::new(),
        }
    }

    #[test]
    fn extract_words_basic() {
        let words = extract_words("hello world_foo bar", "");
        let labels: Vec<&str> = words.iter().map(|(_, w)| w.as_str()).collect();
        assert_eq!(labels, vec!["hello", "world_foo", "bar"]);
    }

    #[test]
    fn extract_words_unicode() {
        let words = extract_words("café naïve über_cool", "");
        let labels: Vec<&str> = words.iter().map(|(_, w)| w.as_str()).collect();
        assert_eq!(labels, vec!["café", "naïve", "über_cool"]);
    }

    #[test]
    fn extract_words_cjk_and_emoji() {
        let words = extract_words("foo 変数 bar", "");
        let labels: Vec<&str> = words.iter().map(|(_, w)| w.as_str()).collect();
        assert_eq!(labels, vec!["foo", "変数", "bar"]);
    }

    #[test]
    fn extract_words_with_extra_chars() {
        // Lisp-style kebab-case with `-` as word char
        let words = extract_words("my-variable other-thing foo", "-");
        let labels: Vec<&str> = words.iter().map(|(_, w)| w.as_str()).collect();
        assert_eq!(labels, vec!["my-variable", "other-thing", "foo"]);
    }

    #[test]
    fn extract_words_with_dollar_sign() {
        // PHP/Bash style with `$` as word char
        let words = extract_words("$user_name = $other_var", "$");
        let labels: Vec<&str> = words.iter().map(|(_, w)| w.as_str()).collect();
        assert_eq!(labels, vec!["$user_name", "$other_var"]);
    }

    #[test]
    fn dabbrev_proximity_ordering() {
        let provider = DabbrevProvider::new();
        let text = b"apple_pie banana apple_sauce cherry apple_tree";
        let ctx = CompletionContext {
            prefix: "apple".into(),
            cursor_byte: 22,
            word_start_byte: 17,
            buffer_len: text.len(),
            is_large_file: false,
            scan_range: 0..text.len(),
            viewport_top_byte: 0,
            viewport_bottom_byte: text.len(),
            language_id: None,
            word_chars_extra: String::new(),
            prefix_has_uppercase: false,
            other_buffers: Vec::new(),
        };
        let result = provider.provide(&ctx, text);
        match result {
            ProviderResult::Ready(candidates) => {
                let labels: Vec<&str> = candidates.iter().map(|c| c.label.as_str()).collect();
                assert_eq!(labels[0], "apple_sauce");
                assert!(labels.contains(&"apple_pie"));
                assert!(labels.contains(&"apple_tree"));
            }
            _ => panic!("expected Ready"),
        }
    }

    #[test]
    fn dabbrev_skips_exact_prefix() {
        let provider = DabbrevProvider::new();
        let text = b"hello hello_world";
        let ctx = make_ctx("hello", 5, text.len());
        let result = provider.provide(&ctx, text);
        match result {
            ProviderResult::Ready(candidates) => {
                assert_eq!(candidates.len(), 1);
                assert_eq!(candidates[0].label, "hello_world");
            }
            _ => panic!("expected Ready"),
        }
    }

    #[test]
    fn smart_case_lowercase_prefix_matches_all() {
        let provider = DabbrevProvider::new();
        let text = b"HttpServer http_request HTTP_CONST";
        let ctx = make_ctx("http", 0, text.len());
        assert!(!ctx.prefix_has_uppercase);
        let result = provider.provide(&ctx, text);
        match result {
            ProviderResult::Ready(candidates) => {
                assert_eq!(candidates.len(), 3);
                let labels: Vec<&str> = candidates.iter().map(|c| c.label.as_str()).collect();
                assert!(labels.contains(&"HttpServer"));
                assert!(labels.contains(&"http_request"));
                assert!(labels.contains(&"HTTP_CONST"));
                // Exact-case match should score highest
                let http_req = candidates
                    .iter()
                    .find(|c| c.label == "http_request")
                    .unwrap();
                let http_srv = candidates.iter().find(|c| c.label == "HttpServer").unwrap();
                assert!(
                    http_req.score > http_srv.score,
                    "exact-case 'http_request' should outscore 'HttpServer'"
                );
            }
            _ => panic!("expected Ready"),
        }
    }

    #[test]
    fn smart_case_uppercase_prefix_filters_strictly() {
        let provider = DabbrevProvider::new();
        let text = b"HttpServer http_request HTTP_CONST";
        let mut ctx = make_ctx("HTTP", 0, text.len());
        ctx.prefix_has_uppercase = true;
        let result = provider.provide(&ctx, text);
        match result {
            ProviderResult::Ready(candidates) => {
                // Only HTTP_CONST starts with "HTTP" (case-sensitive)
                assert_eq!(candidates.len(), 1);
                assert_eq!(candidates[0].label, "HTTP_CONST");
            }
            _ => panic!("expected Ready"),
        }
    }

    #[test]
    fn multi_buffer_scanning() {
        use super::super::provider::OtherBufferSlice;

        let provider = DabbrevProvider::new();
        let active_text = b"foo_bar baz";
        let mut ctx = make_ctx("foo", 0, active_text.len());
        ctx.other_buffers = vec![OtherBufferSlice {
            buffer_id: 2,
            bytes: b"foo_quux foo_zap".to_vec(),
            label: "other.rs".into(),
        }];
        let result = provider.provide(&ctx, active_text);
        match result {
            ProviderResult::Ready(candidates) => {
                let labels: Vec<&str> = candidates.iter().map(|c| c.label.as_str()).collect();
                assert!(labels.contains(&"foo_bar")); // from active buffer
                assert!(labels.contains(&"foo_quux")); // from other buffer
                assert!(labels.contains(&"foo_zap")); // from other buffer
                                                      // Active buffer candidate should score higher
                let bar = candidates.iter().find(|c| c.label == "foo_bar").unwrap();
                let quux = candidates.iter().find(|c| c.label == "foo_quux").unwrap();
                assert!(
                    bar.score > quux.score,
                    "active-buffer 'foo_bar' should outscore cross-buffer 'foo_quux'"
                );
            }
            _ => panic!("expected Ready"),
        }
    }

    #[test]
    fn language_aware_kebab_case() {
        let provider = DabbrevProvider::new();
        let text = b"my-variable my-function other";
        let mut ctx = make_ctx("my", 0, text.len());
        ctx.word_chars_extra = "-".into();
        let result = provider.provide(&ctx, text);
        match result {
            ProviderResult::Ready(candidates) => {
                let labels: Vec<&str> = candidates.iter().map(|c| c.label.as_str()).collect();
                assert!(labels.contains(&"my-variable"));
                assert!(labels.contains(&"my-function"));
            }
            _ => panic!("expected Ready"),
        }
    }
}
