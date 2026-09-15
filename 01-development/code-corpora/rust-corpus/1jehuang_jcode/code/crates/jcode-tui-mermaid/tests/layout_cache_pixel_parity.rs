//! Pixel-parity probe for the Layout-tier cache (commit 72bd457b, see
//! `mermaid_cache_render.rs`: `LAYOUT_CACHE`, `layout_cache_get`,
//! `render_mermaid_sized_internal`).
//!
//! Confirms there is NO behavioral difference between a cached-layout render
//! and a fresh-layout render: for three different diagram kinds (flowchart,
//! sequence, pie) and two terminal widths (a typical width and a non-default
//! one), render once fresh (layout computed), evict ONLY the PNG tier, render
//! again (layout-cache hit, asserted via debug stats), and require the two
//! PNGs to be byte-identical. If bytes ever differ, the probe decodes both
//! PNGs and pixel-compares before declaring failure, so PNG-encoder
//! nondeterminism (pixel-equal, byte-different) is reported as its own
//! finding instead of a parity failure.
//!
//! This is an integration test, so it drives the crate through its public
//! API only. `evict_render_cache_for_test` is `#[cfg(test)] pub(super)` and
//! not reachable from here; instead the probe deletes the rendered PNG file
//! on disk. That is an equivalent PNG-tier eviction: the memory-tier entry
//! self-invalidates on the `path.exists()` check inside `MermaidCache::get`,
//! and `discover_on_disk` finds nothing because every fixture embeds a
//! per-run nonce (fresh content hash, no stale files). The layout tier is
//! keyed on content/theme/profile and is untouched by the file deletion.
//!
//! Each (fixture, width) cell uses unique content so the first render of the
//! cell is always a layout-tier MISS even though layouts are terminal-width
//! independent. Global hit/miss counters are safe to delta-assert because
//! this binary contains exactly one test.
//!
//! `#[ignore]`-d: run explicitly with
//! `cargo test -p jcode-tui-mermaid --test layout_cache_pixel_parity -- --ignored --nocapture`

#![cfg(feature = "renderer")]

use jcode_tui_mermaid::{RenderResult, debug_stats, render_mermaid_untracked};

/// Render and return (hash, png path, png bytes), panicking on render errors.
fn render_png(label: &str, content: &str, width: u16) -> (u64, std::path::PathBuf, Vec<u8>) {
    match render_mermaid_untracked(content, Some(width)) {
        RenderResult::Image { hash, path, .. } => {
            let bytes = std::fs::read(&path)
                .unwrap_or_else(|e| panic!("{label}: read {} failed: {e}", path.display()));
            (hash, path, bytes)
        }
        RenderResult::Error(error) => panic!("{label}: render failed: {error}"),
    }
}

/// Decode a PNG to RGBA8 pixels for the byte-mismatch fallback comparison.
fn decode_rgba(label: &str, bytes: &[u8]) -> image::RgbaImage {
    image::load_from_memory(bytes)
        .unwrap_or_else(|e| panic!("{label}: PNG decode failed: {e}"))
        .to_rgba8()
}

#[test]
#[ignore = "pixel-parity probe: run explicitly with --ignored --nocapture"]
fn layout_cache_hit_renders_pixel_identical_png_across_diagram_kinds_and_widths() {
    let nonce = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap()
        .as_nanos();

    // Three genuinely different diagram kinds (different parse + layout code
    // paths in mermaid-rs-renderer). The `{cell}` marker is replaced with a
    // per-(fixture,width) unique token so every cell gets a fresh hash.
    let fixtures: [(&str, String); 3] = [
        (
            "flowchart",
            "flowchart LR\n    A{cell}[Ingest] --> B{cell}{Valid?}\n    B{cell} -->|yes| C{cell}[Store]\n    B{cell} -->|no| D{cell}[Reject]\n    C{cell} --> E{cell}[Index]\n    E{cell} --> F{cell}[Serve]"
                .to_string(),
        ),
        (
            "sequence",
            "sequenceDiagram\n    participant U as User{cell}\n    participant S as Server{cell}\n    participant D as Db{cell}\n    U->>S: request {cell}\n    S->>D: query\n    D-->>S: rows\n    S-->>U: response"
                .to_string(),
        ),
        (
            "pie",
            "pie title Cache mix {cell}\n    \"hits\" : 70\n    \"misses\" : 25\n    \"errors\" : 5"
                .to_string(),
        ),
    ];
    // 90 matches the width used across the crate's unit tests; 147 is a
    // deliberately non-default width (different PNG width bucket / target
    // size math).
    let widths: [u16; 2] = [90, 147];

    let mut report: Vec<String> = Vec::new();
    let mut encoder_nondeterminism: Vec<String> = Vec::new();

    for width in widths {
        for (kind, template) in &fixtures {
            let cell = format!("N{nonce}W{width}");
            let content = template.replace("{cell}", &cell);
            let label = format!("{kind}@w{width}");

            // (1) Fresh render: unique content per cell, so the layout tier
            // must MISS and compute the layout.
            let stats0 = debug_stats();
            let (hash, path_a, bytes_a) = render_png(&label, &content, width);
            let stats1 = debug_stats();
            assert_eq!(
                stats1.layout_cache_misses,
                stats0.layout_cache_misses + 1,
                "{label}: fresh render must be a layout-tier miss"
            );
            assert_eq!(
                stats1.layout_cache_hits, stats0.layout_cache_hits,
                "{label}: fresh render must not hit the layout tier"
            );

            // (2) Evict ONLY the PNG tier: remove the on-disk PNG. The
            // memory-tier entry for this hash is dropped on its next access
            // (path.exists() fails), and no other file can satisfy
            // discover_on_disk because the hash is fresh this run. The layout
            // cache stays warm.
            std::fs::remove_file(&path_a)
                .unwrap_or_else(|e| panic!("{label}: evict {} failed: {e}", path_a.display()));

            let (hash_b, path_b, bytes_b) = render_png(&label, &content, width);
            let stats2 = debug_stats();
            assert_eq!(hash, hash_b, "{label}: content hash must be stable");
            assert_eq!(
                stats2.layout_cache_hits,
                stats1.layout_cache_hits + 1,
                "{label}: second render must be a layout-cache hit (PNG tier evicted)"
            );
            assert_eq!(
                stats2.layout_cache_misses, stats1.layout_cache_misses,
                "{label}: second render must not re-compute the layout"
            );
            assert!(
                path_b.exists(),
                "{label}: second render must re-materialize the PNG on disk"
            );

            // (3) Byte-for-byte parity (byte equality implies pixel equality).
            let byte_equal = bytes_a == bytes_b;
            let mut pixel_equal = byte_equal;
            if !byte_equal {
                // Fallback: decode and pixel-compare before declaring failure.
                // Pixel-equal but byte-different output means the PNG encoder
                // is nondeterministic, which is a finding, not a parity bug.
                let img_a = decode_rgba(&label, &bytes_a);
                let img_b = decode_rgba(&label, &bytes_b);
                pixel_equal =
                    img_a.dimensions() == img_b.dimensions() && img_a.as_raw() == img_b.as_raw();
                assert!(
                    pixel_equal,
                    "{label}: PIXEL PARITY FAILURE: cached-layout render differs from \
                     fresh-layout render ({}x{} vs {}x{}, {} vs {} bytes)",
                    img_a.width(),
                    img_a.height(),
                    img_b.width(),
                    img_b.height(),
                    bytes_a.len(),
                    bytes_b.len()
                );
                encoder_nondeterminism.push(format!(
                    "{label}: pixels identical but bytes differ ({} vs {} bytes)",
                    bytes_a.len(),
                    bytes_b.len()
                ));
            }

            report.push(format!(
                "{label:<16} hash={hash:016x} fresh={} bytes, cached-layout={} bytes, \
                 byte_equal={byte_equal}, pixel_equal={pixel_equal}",
                bytes_a.len(),
                bytes_b.len()
            ));
        }
    }

    eprintln!("--- layout cache pixel-parity probe ---");
    for line in &report {
        eprintln!("{line}");
    }
    if encoder_nondeterminism.is_empty() {
        eprintln!("encoder determinism: all cells byte-identical");
    } else {
        eprintln!("FINDING: PNG encoder nondeterminism (pixel-equal, byte-different):");
        for line in &encoder_nondeterminism {
            eprintln!("  {line}");
        }
    }
    assert_eq!(
        report.len(),
        fixtures.len() * widths.len(),
        "probe must cover every fixture x width cell"
    );
}
