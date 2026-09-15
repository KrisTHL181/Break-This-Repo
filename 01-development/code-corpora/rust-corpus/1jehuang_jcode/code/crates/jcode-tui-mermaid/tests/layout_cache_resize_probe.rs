//! Resize-speedup probe for the mermaid layout-tier cache (commit 72bd457b).
//!
//! Verifies, through the public sized-render API only, that:
//! 1. Rendering ONE diagram at terminal widths 120 -> 200 -> 260 cells runs
//!    parse+compute_layout EXACTLY ONCE and rasterizes THREE times. The three
//!    widths map to PNG target widths in distinct width buckets, each below
//!    the 85% cached-width reuse threshold of the next
//!    (CACHE_WIDTH_MATCH_PERCENT), so every step is a genuine rasterize.
//! 2. A warm resize re-render (layout-cache hit, rasterize only) is at least
//!    3x faster than a cold render (parse + layout + rasterize) at the SAME
//!    target width.
//!
//! Because the test-only hooks (`LAYOUT_COMPUTATIONS`,
//! `evict_render_cache_for_test`) are `#[cfg(test)]`-internal and invisible to
//! integration tests, this probe uses the public `debug_stats()` counters
//! (`layout_cache_hits`/`layout_cache_misses` are exact layout-computation
//! proxies: a miss increments iff parse+compute_layout ran) and evicts the PNG
//! memory+disk cache by deleting the `{hash:016x}_w*.png` cache files; the
//! in-memory RENDER_CACHE invalidates entries whose file no longer exists
//! (`path.exists()` check in `MermaidCache::get*`), which is the same
//! mechanism `evict_render_cache_for_test` exercises.
//!
//! Run with:
//!   cargo test --release -p jcode-tui-mermaid --test layout_cache_resize_probe -- --ignored --nocapture
//!
//! NOTE: uses the real jcode mermaid cache dir (like the crate's unit tests);
//! diagrams are content-hash-unique per run, so no stale-cache interference
//! and no interference with other probe files in this checkout.

#![cfg(feature = "renderer")]

use std::path::{Path, PathBuf};
use std::time::Instant;

use jcode_tui_mermaid::{RenderResult, debug_stats, render_mermaid_untracked};

/// Deterministic ~40-node / 45-edge flowchart. There is no `infra` fixture in
/// this crate (checked `crates/jcode-tui-mermaid/` for fixtures/), so this is
/// the labeled substitute: a representative pipeline-shaped flowchart well
/// above the 20-node bar and safely under the MAX_NODES=100 / MAX_EDGES=200
/// complexity gate (both by the bracket-counting estimate and by real parse).
fn probe_flowchart(salt: &str) -> String {
    let unique = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap()
        .as_nanos();
    let mut s = String::from("flowchart TD\n");
    for i in 0..40 {
        s.push_str(&format!(
            "    N{salt}{unique}_{i}[\"Stage {i} of {salt} pipeline\"]\n"
        ));
    }
    for i in 0..39 {
        s.push_str(&format!(
            "    N{salt}{unique}_{i} --> N{salt}{unique}_{}\n",
            i + 1
        ));
    }
    for i in 0..6usize {
        s.push_str(&format!(
            "    N{salt}{unique}_{} --> N{salt}{unique}_{}\n",
            i * 3,
            i * 5 + 7
        ));
    }
    s
}

struct Rendered {
    hash: u64,
    path: PathBuf,
    png_width: u32,
    wall_ms: f64,
}

fn render_at(content: &str, cells: u16) -> Rendered {
    let start = Instant::now();
    match render_mermaid_untracked(content, Some(cells)) {
        RenderResult::Image {
            hash, path, width, ..
        } => Rendered {
            hash,
            path,
            png_width: width,
            wall_ms: start.elapsed().as_secs_f64() * 1000.0,
        },
        RenderResult::Error(error) => panic!("render at {cells} cells failed: {error}"),
    }
}

/// Delete every cached PNG for `hash` (all width variants) from the on-disk
/// cache. The in-memory RENDER_CACHE entry for the hash is invalidated on the
/// next lookup because its `path.exists()` check fails, so this evicts both
/// tiers of the PNG cache while leaving the layout-tier cache warm.
fn evict_pngs(cache_dir: &Path, hash: u64) -> usize {
    let prefix = format!("{hash:016x}_");
    let mut removed = 0;
    if let Ok(entries) = std::fs::read_dir(cache_dir) {
        for entry in entries.flatten() {
            let path = entry.path();
            let is_probe_png = path.extension().and_then(|e| e.to_str()) == Some("png")
                && path
                    .file_name()
                    .and_then(|n| n.to_str())
                    .is_some_and(|n| n.starts_with(&prefix));
            if is_probe_png && std::fs::remove_file(&path).is_ok() {
                removed += 1;
            }
        }
    }
    removed
}

fn count_pngs(cache_dir: &Path, hash: u64) -> usize {
    let prefix = format!("{hash:016x}_");
    std::fs::read_dir(cache_dir)
        .map(|entries| {
            entries
                .flatten()
                .filter(|entry| {
                    let path = entry.path();
                    path.extension().and_then(|e| e.to_str()) == Some("png")
                        && path
                            .file_name()
                            .and_then(|n| n.to_str())
                            .is_some_and(|n| n.starts_with(&prefix))
                })
                .count()
        })
        .unwrap_or(0)
}

#[test]
#[ignore = "wall-clock probe; run explicitly with --ignored --nocapture (release build recommended)"]
fn layout_cache_resize_speedup_probe() {
    // ---- Phase 0: warm process-global one-time costs (font DB scan) with a
    // throwaway diagram so they don't pollute the cold-render timing below.
    let warmup = probe_flowchart("warmup");
    let _ = render_at(&warmup, 120);

    // ---- Phase 1: one diagram, widths 120 -> 200 -> 260 cells.
    // Counts: layout must run exactly once, rasterize exactly three times.
    let content = probe_flowchart("resize");
    let base = debug_stats();

    let first = render_at(&content, 120);
    let cache_dir = first
        .path
        .parent()
        .expect("cached PNG must live in a cache dir")
        .to_path_buf();
    let after_first = debug_stats();
    assert_eq!(
        after_first.layout_cache_misses - base.layout_cache_misses,
        1,
        "cold render must compute the layout (one layout-cache miss)"
    );
    assert_eq!(
        after_first.layout_cache_hits - base.layout_cache_hits,
        0,
        "cold render must not hit the layout cache"
    );
    let cold_stage_parse = after_first.last_parse_ms.unwrap_or(0.0);
    let cold_stage_layout = after_first.last_layout_ms.unwrap_or(0.0);
    let cold_stage_svg = after_first.last_svg_ms.unwrap_or(0.0);
    let cold_stage_png = after_first.last_png_ms.unwrap_or(0.0);

    let second = render_at(&content, 200);
    let after_second = debug_stats();
    assert_eq!(
        after_second.layout_cache_misses - base.layout_cache_misses,
        1,
        "resize to 200 cells must NOT recompute layout"
    );
    assert_eq!(
        after_second.layout_cache_hits - base.layout_cache_hits,
        1,
        "resize to 200 cells must take the layout-cache hit path"
    );
    assert_eq!(
        after_second.last_parse_ms,
        Some(0.0),
        "layout-cache hit renders must skip parse"
    );
    assert_eq!(
        after_second.last_layout_ms,
        Some(0.0),
        "layout-cache hit renders must skip compute_layout"
    );
    assert!(
        after_second.last_png_ms.unwrap_or(0.0) > 0.0,
        "resize to 200 cells must genuinely rasterize (png stage ran)"
    );

    let third = render_at(&content, 260);
    let after_third = debug_stats();
    assert_eq!(
        after_third.layout_cache_misses - base.layout_cache_misses,
        1,
        "resize to 260 cells must NOT recompute layout: layout ran exactly once"
    );
    assert_eq!(
        after_third.layout_cache_hits - base.layout_cache_hits,
        2,
        "both resizes must take the layout-cache hit path"
    );
    assert!(
        after_third.last_png_ms.unwrap_or(0.0) > 0.0,
        "resize to 260 cells must genuinely rasterize (png stage ran)"
    );
    assert_eq!(
        after_third.render_success - base.render_success,
        3,
        "all three widths must be full (non-PNG-cache) renders: rasterize ran three times"
    );
    assert!(
        first.png_width < second.png_width && second.png_width < third.png_width,
        "widths must land in distinct ascending buckets (got {} / {} / {}); \
         RENDER_WIDTH_BUCKET_CELLS quantization or the 85% reuse rule collapsed them",
        first.png_width,
        second.png_width,
        third.png_width
    );
    let distinct_pngs = count_pngs(&cache_dir, first.hash);
    assert_eq!(
        distinct_pngs, 3,
        "three genuine rasterizes must leave three width-distinct PNGs on disk"
    );

    // ---- Phase 2: cold vs warm wall-clock at the SAME 120-cell width.
    // Cold: fresh content hashes (empty layout-cache entry for them).
    // Warm: the phase-1 diagram with its PNGs evicted (memory+disk), so only
    // SVG+rasterize runs against the still-warm layout tier.
    let cold_a = render_at(&probe_flowchart("cold-a"), 120);
    let cold_b = render_at(&probe_flowchart("cold-b"), 120);
    let cold_ms = cold_a.wall_ms.min(cold_b.wall_ms);

    let evicted = evict_pngs(&cache_dir, first.hash);
    assert!(
        evicted >= 3,
        "PNG eviction must remove the phase-1 cache files (removed {evicted})"
    );
    let warm_1 = render_at(&content, 120);
    let evicted_again = evict_pngs(&cache_dir, first.hash);
    assert!(evicted_again >= 1, "re-eviction must remove the warm PNG");
    let warm_2 = render_at(&content, 120);
    let warm_ms = warm_1.wall_ms.min(warm_2.wall_ms);

    let after_warm = debug_stats();
    assert_eq!(
        after_warm.layout_cache_misses - base.layout_cache_misses,
        3, // phase-1 diagram (1) + cold-a (1) + cold-b (1)
        "warm re-renders must not add layout-cache misses"
    );
    assert_eq!(
        after_warm.last_parse_ms,
        Some(0.0),
        "warm re-render must skip parse"
    );
    assert_eq!(
        after_warm.last_layout_ms,
        Some(0.0),
        "warm re-render must skip compute_layout"
    );

    let speedup = cold_ms / warm_ms.max(0.001);
    let stage_cold_total = cold_stage_parse + cold_stage_layout + cold_stage_svg + cold_stage_png;
    println!("== layout_cache_resize_speedup_probe report ==");
    println!(
        "build profile: {}",
        if cfg!(debug_assertions) {
            "debug (timing caveat: release preferred)"
        } else {
            "release"
        }
    );
    println!("layout computations (layout_cache_misses delta, phase 1): 1");
    println!("rasterize count (render_success delta, phase 1):          3");
    println!(
        "phase-1 PNG target widths: {} / {} / {} px",
        first.png_width, second.png_width, third.png_width
    );
    println!(
        "cold render stage breakdown @120 cells: parse {:.1} ms, layout {:.1} ms, svg {:.1} ms, png {:.1} ms (total {:.1} ms)",
        cold_stage_parse, cold_stage_layout, cold_stage_svg, cold_stage_png, stage_cold_total
    );
    println!(
        "cold wall (min of 2, fresh hash @120 cells):   {:.1} ms",
        cold_ms
    );
    println!(
        "warm wall (min of 2, layout hit @120 cells):   {:.1} ms",
        warm_ms
    );
    println!("resize speedup (cold / warm):                  {speedup:.2}x");

    assert!(
        speedup >= 3.0,
        "warm resize re-render must be at least 3x faster than a cold render \
         (cold {cold_ms:.1} ms vs warm {warm_ms:.1} ms = {speedup:.2}x)"
    );
}
