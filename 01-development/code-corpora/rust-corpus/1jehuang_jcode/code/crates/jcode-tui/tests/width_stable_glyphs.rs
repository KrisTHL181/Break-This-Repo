//! Guard against width-unstable glyphs in TUI source (issue seen 2026-07-02).
//!
//! Unicode 16 reclassified several symbol ranges from narrow (1 cell) to wide
//! (2 cells): U+2630..U+2637 (trigrams), U+268A..U+268F (monograms/digrams),
//! U+4DC0..U+4DFF (hexagrams), U+1D300..U+1D356 and U+1D360..U+1D376 (Tai Xuan
//! Jing), among others. Terminals with Unicode 16 width tables (kitty >= 0.40)
//! render them 2 cells wide, while `unicode-width` builds pinned to older
//! tables count 1. Any such glyph in a rendered line shears the whole row:
//! everything after it shifts by one column, which pushed the right-docked
//! info-widget borders off screen when swarm "Plan" lines used U+2630.
//!
//! This test scans TUI-rendering crates for those codepoints in non-comment
//! source so the bug class cannot silently return. If you genuinely need one
//! of these glyphs, pad the row defensively and add an exception here with a
//! comment explaining why the shear cannot happen.

use std::path::{Path, PathBuf};

/// Codepoint ranges whose East Asian Width changed narrow -> wide in
/// Unicode 16 (diff of unicode-width 0.2.0 vs 0.2.2 width tables), restricted
/// to symbol blocks plausible as TUI icons. Script/format chars (e.g. Tagalog,
/// Balinese) are omitted: they only appear inside user content, which the
/// renderer already width-measures with the bundled unicode-width.
const UNSTABLE_RANGES: &[(u32, u32, &str)] = &[
    (0x2630, 0x2637, "Yijing trigrams"),
    (0x268A, 0x268F, "Yijing monograms/digrams"),
    (0x31E4, 0x31E5, "CJK strokes added in Unicode 16"),
    (0x4DC0, 0x4DFF, "Yijing hexagrams"),
    (0x1D300, 0x1D356, "Tai Xuan Jing symbols"),
    (0x1D360, 0x1D376, "counting rod numerals"),
];

fn unstable(ch: char) -> Option<&'static str> {
    let cp = ch as u32;
    UNSTABLE_RANGES
        .iter()
        .find(|&&(lo, hi, _)| cp >= lo && cp <= hi)
        .map(|&(_, _, name)| name)
}

fn scan_file(path: &Path, violations: &mut Vec<String>) {
    let Ok(text) = std::fs::read_to_string(path) else {
        return;
    };
    for (lineno, line) in text.lines().enumerate() {
        // Skip pure comment lines so docs may *mention* these glyphs.
        let trimmed = line.trim_start();
        if trimmed.starts_with("//") || trimmed.starts_with("*") {
            continue;
        }
        for ch in line.chars() {
            if let Some(block) = unstable(ch) {
                violations.push(format!(
                    "{}:{}: U+{:04X} '{}' ({}) is width-unstable across Unicode versions",
                    path.display(),
                    lineno + 1,
                    ch as u32,
                    ch,
                    block
                ));
            }
        }
    }
}

fn scan_dir(dir: &Path, violations: &mut Vec<String>) {
    let Ok(entries) = std::fs::read_dir(dir) else {
        return;
    };
    for entry in entries.flatten() {
        let path = entry.path();
        if path.is_dir() {
            scan_dir(&path, violations);
        } else if path.extension().is_some_and(|e| e == "rs") {
            scan_file(&path, violations);
        }
    }
}

#[test]
fn no_width_unstable_glyphs_in_tui_sources() {
    let manifest = PathBuf::from(env!("CARGO_MANIFEST_DIR"));
    let crates_root = manifest.parent().expect("crates dir");
    let mut violations = Vec::new();
    // All crates that produce user-visible terminal lines.
    for krate in [
        "jcode-tui",
        "jcode-tui-messages",
        "jcode-tui-markdown",
        "jcode-tui-render",
        "jcode-tui-tool-display",
        "jcode-render-core",
    ] {
        scan_dir(&crates_root.join(krate).join("src"), &mut violations);
    }
    assert!(
        violations.is_empty(),
        "width-unstable glyphs found (these shear rows on Unicode-16 terminals \
         like kitty >= 0.40; use a width-stable alternative such as ≡ for ☰):\n{}",
        violations.join("\n")
    );
}
