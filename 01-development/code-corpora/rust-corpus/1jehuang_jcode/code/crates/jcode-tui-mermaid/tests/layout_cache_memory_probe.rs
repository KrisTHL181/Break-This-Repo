//! Memory-bound probe for the Layout-tier cache (see
//! `mermaid_cache_render.rs`: `LayoutCache`, `LAYOUT_CACHE_MAX`,
//! `approx_layout_bytes`, `layout_cache_usage`).
//!
//! Renders 50 distinct diagrams through the sized-render path and checks that
//! the layout tier stays bounded: entries <= LAYOUT_CACHE_MAX (32), approx
//! resident bytes <= ~2.5 MB (the documented worst-case estimate), LRU
//! eviction actually kicks in (50 misses but only 32 resident), and process
//! RSS growth over the run is sane (< 50 MB; note the raw RSS delta also
//! includes PNG-tier metadata, rasterizer/fontdb allocations, and allocator
//! slack, so the cache's own approx_bytes is reported separately to keep the
//! cache-tier contribution attributable).
//!
//! `#[ignore]`-d: run explicitly with
//! `cargo test -p jcode-tui-mermaid --test layout_cache_memory_probe -- --ignored --nocapture`

#![cfg(feature = "renderer")]

/// VmRSS from /proc/self/status, in bytes (Linux only).
fn vm_rss_bytes() -> Option<u64> {
    let status = std::fs::read_to_string("/proc/self/status").ok()?;
    for line in status.lines() {
        if let Some(rest) = line.strip_prefix("VmRSS:") {
            let kb = rest
                .trim()
                .trim_end_matches("kB")
                .trim()
                .parse::<u64>()
                .ok()?;
            return Some(kb * 1024);
        }
    }
    None
}

/// A distinct small flowchart per index: node count varies (3..=10 chain
/// links) and every label embeds the index plus a per-run nonce so source
/// hashes differ across probe iterations *and* across repeated runs (a stale
/// PNG disk cache from a previous run cannot satisfy the render, but even if
/// the PNG tier hit, the layout tier misses because the hash is new).
fn probe_diagram(idx: usize, nonce: u128) -> String {
    let links = 3 + (idx % 8);
    let mut out = String::from("flowchart TD\n");
    for step in 0..links {
        out.push_str(&format!(
            "    P{idx}S{step}N{nonce}[probe {idx} step {step}] --> P{idx}S{s}N{nonce}[probe {idx} step {s}]\n",
            s = step + 1
        ));
    }
    out
}

#[test]
#[ignore = "memory probe: run explicitly with --ignored --nocapture"]
fn layout_cache_stays_bounded_under_50_distinct_renders() {
    const RENDERS: usize = 50;
    /// Documented worst case: 32 entries x ~75 KB (100-node cap) ~= 2.4 MB.
    const APPROX_BYTES_CAP: u64 = 2_500_000;
    /// Generous cap for whole-process growth over 50 small renders. The raw
    /// delta includes non-cache allocations (rasterizer, fontdb, PNG-tier).
    const RSS_DELTA_CAP: u64 = 50 * 1024 * 1024;

    let nonce = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap()
        .as_nanos();

    jcode_tui_mermaid::reset_debug_stats();
    let stats_before = jcode_tui_mermaid::debug_stats();
    let limit = stats_before.layout_cache_limit;
    assert_eq!(limit, 32, "probe assumes LAYOUT_CACHE_MAX == 32");
    assert!(
        RENDERS > limit,
        "probe must overflow the cache to exercise LRU eviction"
    );

    let rss_before = vm_rss_bytes().expect("VmRSS readable on Linux");

    let mut render_errors = 0usize;
    for idx in 0..RENDERS {
        let content = probe_diagram(idx, nonce);
        match jcode_tui_mermaid::render_mermaid_untracked(&content, Some(90)) {
            jcode_tui_mermaid::RenderResult::Image { .. } => {}
            jcode_tui_mermaid::RenderResult::Error(error) => {
                render_errors += 1;
                eprintln!("render {idx} failed: {error}");
            }
        }
    }
    assert_eq!(render_errors, 0, "all probe renders must succeed");

    let rss_after = vm_rss_bytes().expect("VmRSS readable on Linux");
    let stats = jcode_tui_mermaid::debug_stats();
    let memory = jcode_tui_mermaid::debug_memory_profile();

    let rss_delta = rss_after.saturating_sub(rss_before);
    eprintln!("--- layout cache memory probe ---");
    eprintln!("renders:                  {RENDERS} distinct diagrams");
    eprintln!(
        "layout_cache entries:     {} / {} (limit)",
        stats.layout_cache_entries, stats.layout_cache_limit
    );
    eprintln!(
        "layout_cache approx_bytes:{} ({:.1} KB)",
        stats.layout_cache_approx_bytes,
        stats.layout_cache_approx_bytes as f64 / 1024.0
    );
    eprintln!(
        "layout hits/misses:       {} / {}",
        stats.layout_cache_hits, stats.layout_cache_misses
    );
    eprintln!(
        "RSS before/after/delta:   {:.1} MB / {:.1} MB / {:.1} MB \
         (delta includes PNG-tier + rasterizer allocations, not just the layout cache)",
        rss_before as f64 / 1048576.0,
        rss_after as f64 / 1048576.0,
        rss_delta as f64 / 1048576.0
    );

    // (a) Entry count bounded, consistently reported by stats and memory profile.
    assert!(
        stats.layout_cache_entries <= limit,
        "entries {} exceed LAYOUT_CACHE_MAX {limit}",
        stats.layout_cache_entries
    );
    assert_eq!(
        memory.layout_cache_entries, stats.layout_cache_entries,
        "MermaidMemoryProfile and MermaidDebugStats disagree on entry count"
    );
    assert_eq!(memory.layout_cache_limit, limit);

    // (b) Approximate resident bytes bounded by the documented worst case.
    assert!(
        stats.layout_cache_approx_bytes <= APPROX_BYTES_CAP,
        "layout cache approx bytes {} exceed the ~2.5MB worst-case bound",
        stats.layout_cache_approx_bytes
    );
    assert_eq!(
        memory.layout_cache_approx_bytes,
        stats.layout_cache_approx_bytes
    );
    assert!(
        stats.layout_cache_approx_bytes > 0,
        "cache should hold real layouts after 50 renders"
    );

    // LRU eviction actually kicked in: every distinct source is a layout-tier
    // miss (this integration-test binary runs the probe alone, so the global
    // counters are not shared with other tests), and only `limit` entries
    // stay resident.
    assert_eq!(
        stats.layout_cache_misses, RENDERS as u64,
        "each distinct diagram must miss the layout tier once"
    );
    assert_eq!(
        stats.layout_cache_hits, 0,
        "distinct sources must not hit the layout tier"
    );
    assert_eq!(
        stats.layout_cache_entries, limit,
        "LRU eviction must cap residency at LAYOUT_CACHE_MAX"
    );

    // (c) Whole-process growth stays sane.
    assert!(
        rss_delta < RSS_DELTA_CAP,
        "RSS delta {rss_delta} bytes exceeds {RSS_DELTA_CAP} bytes"
    );
}
