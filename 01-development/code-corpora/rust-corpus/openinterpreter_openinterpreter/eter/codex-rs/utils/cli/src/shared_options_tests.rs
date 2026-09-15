//! Tests shared command-line option inheritance.

use super::SharedCliOptions;

#[test]
fn inherits_worktree_from_root_without_clearing_subcommand_choice() {
    let mut options = SharedCliOptions::default();
    let root = SharedCliOptions {
        worktree: true,
        ..Default::default()
    };
    options.inherit_exec_root_options(&root);
    assert!(options.worktree);

    options.inherit_exec_root_options(&SharedCliOptions::default());
    assert!(options.worktree);
}

#[test]
fn applies_worktree_subcommand_override_without_clearing_root_choice() {
    let mut options = SharedCliOptions::default();
    options.apply_subcommand_overrides(SharedCliOptions {
        worktree: true,
        ..Default::default()
    });
    assert!(options.worktree);

    options.apply_subcommand_overrides(SharedCliOptions::default());
    assert!(options.worktree);
}

#[test]
fn root_chat_completions_is_inherited_by_exec_options() {
    let root = SharedCliOptions {
        chat_completions: true,
        ..Default::default()
    };
    let mut exec = SharedCliOptions::default();

    exec.inherit_exec_root_options(&root);

    assert!(exec.chat_completions);
}

#[test]
fn subcommand_chat_completions_enables_the_combined_options() {
    let mut combined = SharedCliOptions::default();
    let subcommand = SharedCliOptions {
        chat_completions: true,
        ..Default::default()
    };

    combined.apply_subcommand_overrides(subcommand);

    assert!(combined.chat_completions);
}
