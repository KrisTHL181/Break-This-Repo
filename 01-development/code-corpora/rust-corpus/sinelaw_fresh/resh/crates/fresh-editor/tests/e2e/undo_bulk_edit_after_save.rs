//! Test for undo corruption after save with BulkEdit in history
//!
//! Reproduces bug where:
//! 1. User edits a file (creates StringBuffers in the buffers vec)
//! 2. User does a BulkEdit operation (e.g. toggle-comment) which snapshots the piece tree
//! 3. User saves -> consolidate_after_save() replaces self.buffers with a single buffer
//!    and resets next_buffer_id = 1
//! 4. User types more text -> creates Added(1) buffer with new content
//! 5. User undoes past the BulkEdit -> restore_piece_tree() restores the old tree
//!    but self.buffers still has the post-consolidation buffers
//!    -> old tree references buffer IDs that no longer exist or have wrong data
//!    -> "Buffer range out of bounds" / "Buffer not found" errors
//!
//! User report: "holding ctrl+z (undo) nuked the other lines"
//! Errors from log:
//!   Buffer range out of bounds: requested 0..46, buffer size 1
//!   LineIterator: Failed to load chunk at offset 20
//!   LineIterator::prev(): Failed to load chunk at 613: Buffer not found

use crate::common::harness::{EditorTestHarness, HarnessOptions};
use crossterm::event::{KeyCode, KeyModifiers};
use fresh::config::Config;
use tempfile::TempDir;

/// Helper to run a command from the command palette
fn run_command(harness: &mut EditorTestHarness, command_name: &str) {
    harness
        .send_key(KeyCode::Char('p'), KeyModifiers::CONTROL)
        .unwrap();
    harness.render().unwrap();
    harness.type_text(command_name).unwrap();
    harness.render().unwrap();
    harness
        .send_key(KeyCode::Enter, KeyModifiers::NONE)
        .unwrap();
    harness.render().unwrap();
}

/// Reproduce: BulkEdit (toggle comment) -> save -> type -> undo past BulkEdit -> corruption
///
/// The root cause is that consolidate_after_save() replaces self.buffers and resets
/// next_buffer_id, but BulkEdit undo snapshots still reference the old buffer IDs.
/// restore_piece_tree() restores the tree but NOT the buffers, so the piece tree
/// and buffer list become desynchronized.
#[test]
fn test_undo_past_bulk_edit_after_save_does_not_corrupt_buffer() {
    let temp_dir = TempDir::new().unwrap();
    let file_path = temp_dir.path().join("test.rs");
    std::fs::write(
        &file_path,
        "fn main() {\n    println!(\"hello\");\n    println!(\"world\");\n}\n",
    )
    .unwrap();

    let config = Config::default();
    let mut harness =
        EditorTestHarness::create(80, 24, HarnessOptions::new().with_config(config)).unwrap();
    harness.open_file(&file_path).unwrap();
    harness.render().unwrap();

    let original_content = harness.get_buffer_content().unwrap();

    // Step 1: Type some text to create buffers in the buffers vec
    harness.send_key(KeyCode::End, KeyModifiers::NONE).unwrap();
    harness.type_text(" // edited").unwrap();
    harness.render().unwrap();

    // Step 2: Toggle comment on the current line -> creates a BulkEdit event
    // that snapshots the piece tree (which now references multiple buffers)
    run_command(&mut harness, "Toggle Comment");

    let content_after_comment = harness.get_buffer_content().unwrap();
    assert!(
        content_after_comment.contains("//"),
        "Toggle comment should have added a comment prefix. Got: {:?}",
        content_after_comment
    );

    // Step 3: Save the file -> consolidate_after_save() replaces self.buffers
    // with a single buffer and resets next_buffer_id = 1
    harness
        .send_key(KeyCode::Char('s'), KeyModifiers::CONTROL)
        .unwrap();
    harness.render().unwrap();

    assert!(
        !harness.editor().active_state().buffer.is_modified(),
        "Buffer should not be modified after save"
    );

    // Step 4: Type more text after save. This creates a new Added(1) buffer
    // with just the typed content. After consolidation, the buffers vec is
    // [consolidated_buf_0, new_1_byte_buf], and next_buffer_id = 2.
    harness.send_key(KeyCode::Down, KeyModifiers::NONE).unwrap();
    harness.send_key(KeyCode::End, KeyModifiers::NONE).unwrap();
    harness.type_text("X").unwrap();
    harness.render().unwrap();

    // Step 5: Undo everything back past the BulkEdit.
    // This should NOT corrupt the buffer.
    // The BulkEdit undo calls restore_piece_tree() which restores the old tree,
    // but the old tree may reference buffer IDs that no longer exist after consolidation.
    for _ in 0..20 {
        harness
            .send_key(KeyCode::Char('z'), KeyModifiers::CONTROL)
            .unwrap();
    }
    harness.render().unwrap();

    // The buffer content should be the original file content (all edits undone).
    // If the buffer is corrupted, get_buffer_content() will likely panic or return
    // garbage due to "Buffer range out of bounds" or "Buffer not found" errors.
    let final_content = harness.get_buffer_content().unwrap();
    assert_eq!(
        final_content, original_content,
        "Buffer content should match original after undoing all edits.\n\
         If this fails with 'Buffer range out of bounds' or 'Buffer not found',\n\
         the piece tree and buffer list are desynchronized after consolidation + BulkEdit undo."
    );
}

/// Same test but with a larger file and indent operation (another BulkEdit source)
#[test]
fn test_undo_past_indent_after_save_does_not_corrupt_buffer() {
    let temp_dir = TempDir::new().unwrap();
    let file_path = temp_dir.path().join("test.py");
    std::fs::write(
        &file_path,
        "def main():\n    print('hello')\n    print('world')\n    return 0\n",
    )
    .unwrap();

    let config = Config::default();
    let mut harness =
        EditorTestHarness::create(80, 24, HarnessOptions::new().with_config(config)).unwrap();
    harness.open_file(&file_path).unwrap();
    harness.render().unwrap();

    let original_content = harness.get_buffer_content().unwrap();

    // Step 1: Select multiple lines
    harness.send_key(KeyCode::Down, KeyModifiers::NONE).unwrap();
    harness.send_key(KeyCode::Home, KeyModifiers::NONE).unwrap();
    // Select two lines with Shift+Down
    harness
        .send_key(KeyCode::Down, KeyModifiers::SHIFT)
        .unwrap();
    harness
        .send_key(KeyCode::Down, KeyModifiers::SHIFT)
        .unwrap();

    // Step 2: Indent selection (Tab) -> creates a BulkEdit
    harness.send_key(KeyCode::Tab, KeyModifiers::NONE).unwrap();
    harness.render().unwrap();

    // Step 3: Save
    harness
        .send_key(KeyCode::Char('s'), KeyModifiers::CONTROL)
        .unwrap();
    harness.render().unwrap();

    // Step 4: Type after save (creates new buffers after consolidation)
    harness.send_key(KeyCode::End, KeyModifiers::NONE).unwrap();
    harness.type_text("Y").unwrap();
    harness.render().unwrap();

    // Step 5: Undo everything
    for _ in 0..30 {
        harness
            .send_key(KeyCode::Char('z'), KeyModifiers::CONTROL)
            .unwrap();
    }
    harness.render().unwrap();

    let final_content = harness.get_buffer_content().unwrap();
    assert_eq!(
        final_content, original_content,
        "Buffer content should match original after undoing all edits (indent variant).\n\
         If this fails with 'Buffer range out of bounds' or 'Buffer not found',\n\
         the piece tree and buffer list are desynchronized after consolidation + BulkEdit undo."
    );
}

/// Simplest reproduction: bulk edit -> save -> undo (no typing after save)
///
/// 1. Open a file
/// 2. Bulk edit (toggle comment)
/// 3. Save
/// 4. Undo
///
/// Expected: undoes step 2 (content matches original)
/// Actual: corrupts the text
#[test]
fn test_bulk_edit_save_undo_minimal() {
    let temp_dir = TempDir::new().unwrap();
    let file_path = temp_dir.path().join("test.rs");
    std::fs::write(
        &file_path,
        "fn main() {\n    println!(\"hello\");\n    println!(\"world\");\n}\n",
    )
    .unwrap();

    let config = Config::default();
    let mut harness =
        EditorTestHarness::create(80, 24, HarnessOptions::new().with_config(config)).unwrap();
    harness.open_file(&file_path).unwrap();
    harness.render().unwrap();

    let original_content = harness.get_buffer_content().unwrap();

    // Step 1: Toggle comment (BulkEdit)
    run_command(&mut harness, "Toggle Comment");

    let commented = harness.get_buffer_content().unwrap();
    assert_ne!(
        commented, original_content,
        "Toggle comment should have changed the content"
    );

    // Step 2: Save -> consolidate_after_save() replaces buffers
    harness
        .send_key(KeyCode::Char('s'), KeyModifiers::CONTROL)
        .unwrap();
    harness.render().unwrap();

    // Step 3: Undo -> should restore original content
    harness
        .send_key(KeyCode::Char('z'), KeyModifiers::CONTROL)
        .unwrap();
    harness.render().unwrap();

    let final_content = harness.get_buffer_content().unwrap();
    assert_eq!(
        final_content, original_content,
        "Undo after save should restore original content.\n\
         Expected: {:?}\n\
         Got: {:?}\n\
         The BulkEdit undo restored a piece tree referencing buffers that were \
         destroyed by consolidate_after_save().",
        original_content, final_content
    );
}

/// Same minimal reproduction but using move-line-down (Alt+Down) as the bulk edit
#[test]
fn test_move_line_down_save_undo_minimal() {
    let temp_dir = TempDir::new().unwrap();
    let file_path = temp_dir.path().join("test.txt");
    std::fs::write(&file_path, "line A\nline B\nline C\nline D\n").unwrap();

    let config = Config::default();
    let mut harness =
        EditorTestHarness::create(80, 24, HarnessOptions::new().with_config(config)).unwrap();
    harness.open_file(&file_path).unwrap();
    harness.render().unwrap();

    let original_content = harness.get_buffer_content().unwrap();

    // Step 1: Move line down (Alt+Down) - this is a BulkEdit
    harness.send_key(KeyCode::Down, KeyModifiers::ALT).unwrap();
    harness.render().unwrap();

    let moved = harness.get_buffer_content().unwrap();
    assert_ne!(
        moved, original_content,
        "Move line down should have changed the content"
    );

    // Step 2: Save
    harness
        .send_key(KeyCode::Char('s'), KeyModifiers::CONTROL)
        .unwrap();
    harness.render().unwrap();

    // Step 3: Undo -> should restore original content
    harness
        .send_key(KeyCode::Char('z'), KeyModifiers::CONTROL)
        .unwrap();
    harness.render().unwrap();

    let final_content = harness.get_buffer_content().unwrap();
    assert_eq!(
        final_content, original_content,
        "Undo after save should restore original line order.\n\
         Expected: {:?}\n\
         Got: {:?}",
        original_content, final_content
    );
}

/// Test multiple save-edit-undo cycles to catch cumulative corruption
#[test]
fn test_multiple_save_cycles_with_bulk_edit_undo() {
    let temp_dir = TempDir::new().unwrap();
    let file_path = temp_dir.path().join("test.rs");
    std::fs::write(&file_path, "line1\nline2\nline3\nline4\n").unwrap();

    let config = Config::default();
    let mut harness =
        EditorTestHarness::create(80, 24, HarnessOptions::new().with_config(config)).unwrap();
    harness.open_file(&file_path).unwrap();
    harness.render().unwrap();

    let original_content = harness.get_buffer_content().unwrap();

    // Cycle 1: edit -> toggle comment -> save -> type
    harness.type_text("A").unwrap();
    run_command(&mut harness, "Toggle Comment");
    harness
        .send_key(KeyCode::Char('s'), KeyModifiers::CONTROL)
        .unwrap();
    harness.render().unwrap();
    harness.type_text("B").unwrap();

    // Cycle 2: toggle comment again -> save -> type
    run_command(&mut harness, "Toggle Comment");
    harness
        .send_key(KeyCode::Char('s'), KeyModifiers::CONTROL)
        .unwrap();
    harness.render().unwrap();
    harness.type_text("C").unwrap();

    // Now undo everything (past two BulkEdits and two consolidations)
    for _ in 0..50 {
        harness
            .send_key(KeyCode::Char('z'), KeyModifiers::CONTROL)
            .unwrap();
    }
    harness.render().unwrap();

    let final_content = harness.get_buffer_content().unwrap();
    assert_eq!(
        final_content, original_content,
        "Buffer content should match original after undoing through multiple save+BulkEdit cycles."
    );
}
