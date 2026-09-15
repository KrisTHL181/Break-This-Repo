//! Issue #772: a workspace package's `used_class_members` and
//! `scss_include_paths` plugin contributions must survive the
//! `run_plugins` workspace-result merge.
//!
//! The monorepo fixture activates Lit only in `packages/elements` and Angular
//! only in `packages/ng-styles`; the root project depends on neither, so these
//! plugin contributions exist ONLY at the workspace-package level. Before the
//! fix the merge loop cleared both fields, so the package's Lit lifecycle
//! members were wrongly reported as unused and its Angular SCSS includePaths
//! were dropped (surfacing `@import 'variables'` as unresolved).

use crate::common::{create_config, fixture_path};

#[test]
fn workspace_lit_lifecycle_members_survive_merge_but_genuine_unused_still_flagged() {
    let root = fixture_path("issue-772-workspace-plugin-merge");
    let config = create_config(root);
    let results = fallow_core::analyze(&config).expect("analysis should succeed");

    let unused: Vec<String> = results
        .unused_class_members
        .iter()
        .map(|m| format!("{}.{}", m.member.parent_name, m.member.member_name))
        .collect();

    assert!(
        !unused.contains(&"WorkspaceElement.firstUpdated".to_string()),
        "firstUpdated() on a workspace-package LitElement should be plugin-allowlisted after the merge, found: {unused:?}"
    );

    assert!(
        unused.contains(&"WorkspaceElement.unusedHelper".to_string()),
        "genuinely-unused helper on the workspace-package element should still be reported, found: {unused:?}"
    );
}

#[test]
fn workspace_angular_scss_include_paths_survive_merge() {
    let root = fixture_path("issue-772-workspace-plugin-merge");
    let config = create_config(root);
    let results = fallow_core::analyze(&config).expect("analysis should succeed");

    let unresolved_variables: Vec<String> = results
        .unresolved_imports
        .iter()
        .filter(|u| u.import.specifier.contains("variables"))
        .map(|u| u.import.specifier.clone())
        .collect();

    assert!(
        unresolved_variables.is_empty(),
        "workspace-package Angular SCSS includePaths should resolve `@import 'variables'` after the merge, found unresolved: {unresolved_variables:?}"
    );
}
