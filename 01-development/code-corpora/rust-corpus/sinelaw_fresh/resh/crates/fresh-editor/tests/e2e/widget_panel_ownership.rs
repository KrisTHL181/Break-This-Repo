//! Panel identity is plugin-scoped: two plugins that allocate the SAME
//! plugin-local panel id must coexist, and each must receive only its
//! own `widget_event`s.
//!
//! This is the regression surface for two related bugs:
//!
//! * the host registry used to key panels by the bare id, so the second
//!   plugin's mount evicted the first's entry (the theme editor's first
//!   panel killed the orchestrator dock's click hit-map);
//! * `widget_event` used to be broadcast to every plugin for client-side
//!   `e.panel_id === panel.id()` filtering — with plugin-local ids the
//!   filter can't tell two plugins' panels apart, so a broadcast would
//!   tick BOTH counters below.
//!
//! `test_panel_owner_alpha.ts` mounts a dock panel with local id 1 and
//! renders `ALPHA=<n>` (its `activate` counter); `test_panel_owner_beta.ts`
//! mounts a centered modal, also with local id 1, rendering `BETA=<n>`.
//! We mount both, click each plugin's button, and assert only the owning
//! plugin's counter moves (CONTRIBUTING §2: drive keys/mouse, assert on
//! rendered output).

use crate::common::harness::{copy_plugin_lib, EditorTestHarness};
use crate::common::tracing::init_tracing_from_env;
use crossterm::event::{KeyCode, KeyModifiers};
use std::fs;

fn install_plugins(project_root: &std::path::Path) {
    let plugins_dir = project_root.join("plugins");
    fs::create_dir_all(&plugins_dir).expect("create plugins dir");
    copy_plugin_lib(&plugins_dir);

    const ALPHA: &str = include_str!(concat!(
        env!("CARGO_MANIFEST_DIR"),
        "/tests/plugins/test_panel_owner_alpha.ts"
    ));
    const BETA: &str = include_str!(concat!(
        env!("CARGO_MANIFEST_DIR"),
        "/tests/plugins/test_panel_owner_beta.ts"
    ));
    fs::write(plugins_dir.join("test_panel_owner_alpha.ts"), ALPHA).unwrap();
    fs::write(plugins_dir.join("test_panel_owner_beta.ts"), BETA).unwrap();
}

fn run_palette_command(h: &mut EditorTestHarness, command: &str) {
    h.send_key(KeyCode::Char('p'), KeyModifiers::CONTROL)
        .unwrap();
    h.wait_for_prompt().unwrap();
    h.type_text(command).unwrap();
    h.wait_until(|h| h.screen_to_string().contains(command))
        .unwrap();
    h.send_key(KeyCode::Enter, KeyModifiers::NONE).unwrap();
}

/// 0-based screen position of `needle`, or panic with the screen.
fn find_on_screen(h: &EditorTestHarness, needle: &str) -> (u16, u16) {
    let screen = h.screen_to_string();
    for (row, line) in screen.lines().enumerate() {
        if let Some(byte) = line.find(needle) {
            let col = line[..byte].chars().count();
            return (col as u16, row as u16);
        }
    }
    panic!("screen missing '{needle}':\n{screen}");
}

#[test]
fn same_local_panel_id_two_plugins_coexist_and_events_route_to_owner() {
    init_tracing_from_env();

    let temp_dir = tempfile::TempDir::new().unwrap();
    let project_root = temp_dir.path().join("project_root");
    fs::create_dir(&project_root).unwrap();
    install_plugins(&project_root);

    let mut h =
        EditorTestHarness::with_config_and_working_dir(120, 36, Default::default(), project_root)
            .unwrap();
    h.render().unwrap();

    // Mount alpha's dock panel (local id 1), then beta's centered modal
    // (also local id 1). Both must render — beta's mount must NOT evict
    // alpha's registry entry.
    run_palette_command(&mut h, "OwnerAlpha: Mount");
    h.wait_until(|h| h.screen_to_string().contains("ALPHA=0"))
        .unwrap();
    run_palette_command(&mut h, "OwnerBeta: Mount");
    h.wait_until(|h| {
        let s = h.screen_to_string();
        s.contains("BETA=0") && s.contains("ALPHA=0")
    })
    .unwrap();

    // Click beta's button (the modal is on top). Only beta's counter
    // may tick: alpha filters on the same `e.panel_id === 1`, so a
    // broadcast would tick ALPHA too.
    let (bcol, brow) = find_on_screen(&h, "BetaGo");
    h.mouse_click(bcol + 1, brow).unwrap();
    h.wait_until(|h| h.screen_to_string().contains("BETA=1"))
        .unwrap();
    h.wait_until_stable(|_| true).unwrap();
    let screen = h.screen_to_string();
    assert!(
        screen.contains("ALPHA=0"),
        "beta's activate must not reach alpha (same local id, different \
         owner).\nScreen:\n{screen}"
    );

    // Close the modal, then click alpha's dock button: alpha ticks,
    // beta (unmounted, but its plugin still listens) stays at 1.
    h.send_key(KeyCode::Esc, KeyModifiers::NONE).unwrap();
    h.wait_until(|h| !h.screen_to_string().contains("BetaGo"))
        .unwrap();
    let (acol, arow) = find_on_screen(&h, "AlphaGo");
    h.mouse_click(acol + 1, arow).unwrap();
    h.wait_until(|h| h.screen_to_string().contains("ALPHA=1"))
        .unwrap();
}
