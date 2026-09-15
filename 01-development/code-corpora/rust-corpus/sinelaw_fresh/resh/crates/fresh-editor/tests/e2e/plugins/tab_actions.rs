//! E2E tests for tab actions plugin

use crate::common::harness::layout;
use crate::common::harness::{copy_plugin, copy_plugin_lib, EditorTestHarness};
use crossterm::event::{KeyCode, KeyModifiers};

fn tab_actions_harness() -> (EditorTestHarness, tempfile::TempDir) {
    let temp_dir = tempfile::TempDir::new().unwrap();
    let project_root = temp_dir.path().join("project_root");
    std::fs::create_dir_all(&project_root).unwrap();

    let plugins_dir = project_root.join("plugins");
    std::fs::create_dir_all(&plugins_dir).unwrap();
    copy_plugin(&plugins_dir, "tab_actions");
    copy_plugin_lib(&plugins_dir);

    let mut harness =
        EditorTestHarness::with_config_and_working_dir(80, 24, Default::default(), project_root)
            .unwrap();

    harness.render().unwrap();

    (harness, temp_dir)
}

fn tab_bar(harness: &EditorTestHarness) -> String {
    harness.screen_row_text(layout::TAB_BAR_ROW as u16)
}

/// Open the command palette, type a command, submit it, and wait for the
/// palette to close. Uses semantic waiting throughout — no single-render races.
fn run_palette_command(harness: &mut EditorTestHarness, command: &str) {
    harness
        .send_key(KeyCode::Char('p'), KeyModifiers::CONTROL)
        .unwrap();
    harness.wait_for_prompt().unwrap();
    harness.type_text(command).unwrap();
    harness.wait_for_screen_contains(command).unwrap();
    harness
        .send_key(KeyCode::Enter, KeyModifiers::NONE)
        .unwrap();
    harness.wait_for_prompt_closed().unwrap();
}

/// Switch the active buffer using Ctrl+P / quick-open. `name` is matched against
/// the buffer list.
fn switch_to_buffer(harness: &mut EditorTestHarness, name: &str) {
    harness
        .send_key(KeyCode::Char('p'), KeyModifiers::CONTROL)
        .unwrap();
    harness.wait_for_prompt().unwrap();
    harness
        .send_key(KeyCode::Backspace, KeyModifiers::NONE)
        .unwrap();
    harness.type_text(name).unwrap();
    harness
        .send_key(KeyCode::Enter, KeyModifiers::NONE)
        .unwrap();
    harness.wait_for_prompt_closed().unwrap();
}

fn open_three_files(
    harness: &mut EditorTestHarness,
    project_root: &std::path::Path,
) -> (std::path::PathBuf, std::path::PathBuf, std::path::PathBuf) {
    let file1 = project_root.join("file1.txt");
    let file2 = project_root.join("file2.txt");
    let file3 = project_root.join("file3.txt");
    std::fs::write(&file1, "Content 1").unwrap();
    std::fs::write(&file2, "Content 2").unwrap();
    std::fs::write(&file3, "Content 3").unwrap();

    harness.open_file(&file1).unwrap();
    harness
        .wait_until(|h| tab_bar(h).contains("file1.txt"))
        .unwrap();
    harness.open_file(&file2).unwrap();
    harness
        .wait_until(|h| tab_bar(h).contains("file2.txt"))
        .unwrap();
    harness.open_file(&file3).unwrap();
    harness
        .wait_until(|h| {
            let bar = tab_bar(h);
            bar.contains("file1.txt") && bar.contains("file2.txt") && bar.contains("file3.txt")
        })
        .unwrap();

    (file1, file2, file3)
}

#[test]
fn test_close_other_buffers() {
    let (mut harness, temp_dir) = tab_actions_harness();
    let project_root = temp_dir.path().join("project_root");

    open_three_files(&mut harness, &project_root);

    switch_to_buffer(&mut harness, "file2");
    harness.wait_for_buffer_content("Content 2").unwrap();

    run_palette_command(&mut harness, "Close Other Tabs");

    harness
        .wait_until(|h| {
            let bar = tab_bar(h);
            !bar.contains("file1.txt") && bar.contains("file2.txt") && !bar.contains("file3.txt")
        })
        .unwrap();
}

#[test]
fn test_close_all_buffers() {
    let (mut harness, temp_dir) = tab_actions_harness();
    let project_root = temp_dir.path().join("project_root");

    open_three_files(&mut harness, &project_root);

    switch_to_buffer(&mut harness, "file2");
    harness.wait_for_buffer_content("Content 2").unwrap();

    run_palette_command(&mut harness, "Close All Tabs");

    harness
        .wait_until(|h| {
            let bar = tab_bar(h);
            !bar.contains("file1.txt") && !bar.contains("file2.txt") && !bar.contains("file3.txt")
        })
        .unwrap();
}

#[test]
fn test_close_buffers_to_left() {
    let (mut harness, temp_dir) = tab_actions_harness();
    let project_root = temp_dir.path().join("project_root");

    open_three_files(&mut harness, &project_root);

    switch_to_buffer(&mut harness, "file2");
    harness.wait_for_buffer_content("Content 2").unwrap();

    run_palette_command(&mut harness, "Close Tabs To Left");

    harness
        .wait_until(|h| {
            let bar = tab_bar(h);
            !bar.contains("file1.txt") && bar.contains("file2.txt") && bar.contains("file3.txt")
        })
        .unwrap();
}

#[test]
fn test_close_buffers_to_right() {
    let (mut harness, temp_dir) = tab_actions_harness();
    let project_root = temp_dir.path().join("project_root");

    open_three_files(&mut harness, &project_root);

    switch_to_buffer(&mut harness, "file2");
    harness.wait_for_buffer_content("Content 2").unwrap();

    run_palette_command(&mut harness, "Close Tabs To Right");

    harness
        .wait_until(|h| {
            let bar = tab_bar(h);
            bar.contains("file1.txt") && bar.contains("file2.txt") && !bar.contains("file3.txt")
        })
        .unwrap();
}

#[test]
fn test_move_tab_left() {
    let (mut harness, temp_dir) = tab_actions_harness();
    let project_root = temp_dir.path().join("project_root");

    open_three_files(&mut harness, &project_root);

    let bar = tab_bar(&harness);
    assert!(
        bar.find("file1.txt") < bar.find("file2.txt")
            && bar.find("file2.txt") < bar.find("file3.txt"),
        "Expected initial order file1, file2, file3: {bar}"
    );

    // file3 is active (last opened). Move it left: file1, file3, file2
    run_palette_command(&mut harness, "Move Tab To Left");
    harness
        .wait_until(|h| {
            let b = tab_bar(h);
            match (
                b.find("file1.txt"),
                b.find("file3.txt"),
                b.find("file2.txt"),
            ) {
                (Some(a), Some(c), Some(d)) => a < c && c < d,
                _ => false,
            }
        })
        .unwrap();

    // Move left again: file3, file1, file2
    run_palette_command(&mut harness, "Move Tab Left");
    harness
        .wait_until(|h| {
            let b = tab_bar(h);
            match (
                b.find("file3.txt"),
                b.find("file1.txt"),
                b.find("file2.txt"),
            ) {
                (Some(a), Some(c), Some(d)) => a < c && c < d,
                _ => false,
            }
        })
        .unwrap();

    // Move left again — file3 is at the first position; order stays file3, file1, file2.
    run_palette_command(&mut harness, "Move Tab Left");
    let b = tab_bar(&harness);
    assert!(
        b.find("file3.txt") < b.find("file1.txt") && b.find("file1.txt") < b.find("file2.txt"),
        "Expected file3, file1, file2 (unchanged at left edge): {b}"
    );
}

#[test]
fn test_move_tab_right() {
    let (mut harness, temp_dir) = tab_actions_harness();
    let project_root = temp_dir.path().join("project_root");

    open_three_files(&mut harness, &project_root);

    let bar = tab_bar(&harness);
    assert!(
        bar.find("file1.txt") < bar.find("file2.txt")
            && bar.find("file2.txt") < bar.find("file3.txt"),
        "Expected initial order file1, file2, file3: {bar}"
    );

    // file3 is active and already at the rightmost — order stays file1, file2, file3.
    run_palette_command(&mut harness, "Move Tab To Right");
    let b = tab_bar(&harness);
    assert!(
        b.find("file1.txt") < b.find("file2.txt") && b.find("file2.txt") < b.find("file3.txt"),
        "Expected file1, file2, file3 (unchanged at right edge): {b}"
    );

    // Switch to file1 (first tab), then move it right: file2, file1, file3
    switch_to_buffer(&mut harness, "file1");
    harness.wait_for_buffer_content("Content 1").unwrap();

    run_palette_command(&mut harness, "Move Tab Right");
    harness
        .wait_until(|h| {
            let b = tab_bar(h);
            match (
                b.find("file2.txt"),
                b.find("file1.txt"),
                b.find("file3.txt"),
            ) {
                (Some(a), Some(c), Some(d)) => a < c && c < d,
                _ => false,
            }
        })
        .unwrap();

    // Move right again: file2, file3, file1
    run_palette_command(&mut harness, "Move Tab Right");
    harness
        .wait_until(|h| {
            let b = tab_bar(h);
            match (
                b.find("file2.txt"),
                b.find("file3.txt"),
                b.find("file1.txt"),
            ) {
                (Some(a), Some(c), Some(d)) => a < c && c < d,
                _ => false,
            }
        })
        .unwrap();
}
