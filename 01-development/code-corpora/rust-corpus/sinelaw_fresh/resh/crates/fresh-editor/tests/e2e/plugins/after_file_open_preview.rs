//! A *preview* file-open (file-explorer browse) must not fire the
//! `after_file_open` plugin hook. Real LSP-helper plugins (e.g. asm-lsp)
//! raise an action popup from that hook — its `.asm-lsp.toml` config offer
//! — so without this guard, arrowing through the explorer pops a dialog
//! over every assembly file the user merely glances at as previews replace
//! each other.
//!
//! We can't ship the asm-lsp plugin on this branch, so the test installs a
//! tiny probe plugin (`tests/plugins/test_after_open_popup.ts`) that pops
//! an action popup carrying a unique marker on every `after_file_open`.
//! Asserting on that marker's presence/absence on the rendered screen keeps
//! the test an observer (CONTRIBUTING §2): it never inspects model state.
//!
//! Both halves run so the negative assertion can't pass vacuously:
//!   1. A deliberate open (`open_file`) DOES fire the hook → marker shows.
//!   2. A file-explorer preview does NOT → marker stays absent, while the
//!      preview tab itself renders normally.

use crate::common::harness::{copy_plugin_lib, EditorTestHarness};
use crossterm::event::{KeyCode, KeyModifiers};
use std::fs;
use std::path::Path;
use std::time::Duration;

const PROBE_PLUGIN: &str = include_str!(concat!(
    env!("CARGO_MANIFEST_DIR"),
    "/tests/plugins/test_after_open_popup.ts"
));

/// Marker the probe plugin's popup title carries; unlikely to collide with
/// editor chrome, filenames, or status text.
const MARKER: &str = "AFTER_OPEN_PROBE_MARKER";

const TAB_BAR_ROW: u16 = 1;

fn setup_project(temp: &Path) -> std::path::PathBuf {
    let project_root = temp.join("project");
    fs::create_dir(&project_root).unwrap();

    let plugins_dir = project_root.join("plugins");
    fs::create_dir(&plugins_dir).unwrap();
    copy_plugin_lib(&plugins_dir);
    fs::write(plugins_dir.join("test_after_open_popup.ts"), PROBE_PLUGIN).unwrap();

    // One file to browse (preview) and one to open deliberately. Padded so
    // a hypothetical popup anchored at the cursor has somewhere to render.
    fs::write(
        project_root.join("browse_me.txt"),
        "browse_me\nline 2\nline 3\n",
    )
    .unwrap();
    fs::write(
        project_root.join("open_me.txt"),
        "open_me\nline 2\nline 3\n",
    )
    .unwrap();

    project_root
}

fn tab_bar(h: &EditorTestHarness) -> String {
    h.screen_row_text(TAB_BAR_ROW)
}

#[test]
fn preview_open_does_not_fire_after_file_open_hook() {
    let temp = tempfile::TempDir::new().unwrap();
    let project_root = setup_project(temp.path());

    let mut harness = EditorTestHarness::with_config_and_working_dir(
        120,
        40,
        Default::default(),
        project_root.clone(),
    )
    .unwrap();
    harness.render().unwrap();

    // ---- Preview path: arrow the explorer onto browse_me.txt. ----
    harness
        .send_key(KeyCode::Char('e'), KeyModifiers::CONTROL)
        .unwrap();
    harness.wait_for_file_explorer().unwrap();
    harness
        .wait_for_file_explorer_item("browse_me.txt")
        .unwrap();

    // Step one entry at a time until browse_me.txt is the preview. Other
    // entries (the `plugins/` dir, the already-untouched open_me.txt) don't
    // produce a NEW-file preview, so browse_me.txt is the only open that
    // would fire the hook without the fix.
    for _ in 0..8 {
        harness
            .send_key(KeyCode::Down, KeyModifiers::empty())
            .unwrap();
        harness.render().unwrap();
        if tab_bar(&harness).contains("browse_me.txt (preview)") {
            break;
        }
    }
    harness
        .wait_for_screen_contains("browse_me.txt (preview)")
        .unwrap();

    // Give the (suppressed) hook every chance to have raised the popup
    // asynchronously, then assert it never did.
    for _ in 0..10 {
        harness.process_async_and_render().unwrap();
        harness.sleep(Duration::from_millis(50));
    }
    assert!(
        !harness.screen_to_string().contains(MARKER),
        "after_file_open must NOT fire for a preview open; probe popup marker \
         appeared on screen:\n{}",
        harness.screen_to_string()
    );
    // The preview itself must still be on screen — suppression must not
    // disturb the browsing flow.
    assert!(
        tab_bar(&harness).contains("browse_me.txt (preview)"),
        "browse_me.txt should still be the active preview; tab bar:\n{}",
        tab_bar(&harness)
    );

    // ---- Positive control: a deliberate open DOES fire the hook. ----
    // Proves the probe plugin is wired and the negative assertion above is
    // not vacuous.
    harness
        .open_file(&project_root.join("open_me.txt"))
        .unwrap();
    harness.wait_for_screen_contains(MARKER).unwrap();
}

/// Escalating a preview to a permanent buffer fires the `after_file_open`
/// hook that was *deferred* at preview-open time. A preview is "just
/// looking"; committing to it (here, by editing) is the moment plugins
/// should see the file as opened — so the asm-lsp config offer, csharp
/// `dotnet restore`, etc. run on commit rather than never.
#[test]
fn escalating_a_preview_fires_the_deferred_after_file_open_hook() {
    let temp = tempfile::TempDir::new().unwrap();
    let project_root = setup_project(temp.path());

    let mut harness = EditorTestHarness::with_config_and_working_dir(
        120,
        40,
        Default::default(),
        project_root.clone(),
    )
    .unwrap();
    harness.render().unwrap();

    // Preview browse_me.txt via the explorer (deferred hook → no marker yet).
    harness
        .send_key(KeyCode::Char('e'), KeyModifiers::CONTROL)
        .unwrap();
    harness.wait_for_file_explorer().unwrap();
    harness
        .wait_for_file_explorer_item("browse_me.txt")
        .unwrap();
    for _ in 0..8 {
        harness
            .send_key(KeyCode::Down, KeyModifiers::empty())
            .unwrap();
        harness.render().unwrap();
        if tab_bar(&harness).contains("browse_me.txt (preview)") {
            break;
        }
    }
    harness
        .wait_for_screen_contains("browse_me.txt (preview)")
        .unwrap();
    for _ in 0..6 {
        harness.process_async_and_render().unwrap();
        harness.sleep(Duration::from_millis(50));
    }
    assert!(
        !harness.screen_to_string().contains(MARKER),
        "hook must stay deferred while the file is only a preview; screen:\n{}",
        harness.screen_to_string()
    );

    // Escalate: focus the editor and type into the preview. Editing commits
    // a preview to a permanent tab, which is where the deferred hook fires.
    harness
        .send_key(KeyCode::Char('e'), KeyModifiers::CONTROL)
        .unwrap();
    harness
        .send_key(KeyCode::Char('X'), KeyModifiers::empty())
        .unwrap();
    harness.render().unwrap();

    // The deferred hook now fires (probe popup marker appears) and the tab is
    // no longer a preview.
    harness.wait_for_screen_contains(MARKER).unwrap();
    assert!(
        !tab_bar(&harness).contains("(preview)"),
        "editing must promote the preview to a permanent tab; tab bar:\n{}",
        tab_bar(&harness)
    );
}
