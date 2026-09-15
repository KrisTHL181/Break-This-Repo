use super::common::{create_config, fixture_path};
use super::framework_convention_coverage_common::collect_unused_files;

#[test]
fn electron_vite_rollup_input_entries_keep_renderer_and_preload_trees_alive() {
    let root = fixture_path("issue-600-electron-vite-rollup-input");
    let config = create_config(root.clone());
    let results = fallow_core::analyze(&config).expect("analysis should succeed");

    let unused_files = collect_unused_files(&root, &results);

    for credited in [
        "src/renderer/main-window.ts",
        "src/renderer/shared.ts",
        "src/renderer/settings/settings.ts",
    ] {
        assert!(
            !unused_files.iter().any(|path| path == credited),
            "{credited} should be reachable via a declared renderer HTML entry, unused files: {unused_files:?}"
        );
    }

    for credited in ["electron/preload-bridge.ts", "electron/bridge-helper.ts"] {
        assert!(
            !unused_files.iter().any(|path| path == credited),
            "{credited} should be reachable via a declared preload rollup input, unused files: {unused_files:?}"
        );
    }

    assert!(
        unused_files
            .iter()
            .any(|path| path == "src/renderer/orphan.ts"),
        "orphan renderer file must remain reportable, unused files: {unused_files:?}"
    );
}
