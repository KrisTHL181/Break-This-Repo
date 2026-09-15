use super::common::{create_config, fixture_path};
use super::framework_convention_coverage_common::{
    collect_unused_exports, collect_unused_files, has_unused_export,
};

#[test]
fn vitepress_docs_scaffold_is_discovered_and_strict() {
    let root = fixture_path("vitepress-docs-project");
    let config = create_config(root.clone());
    let results = fallow_core::analyze(&config).expect("analysis should succeed");

    let unused_files = collect_unused_files(&root, &results);
    for expected_used_file in [
        "docs/.vitepress/config.ts",
        "docs/.vitepress/theme/index.ts",
    ] {
        assert!(
            !unused_files.iter().any(|path| path == expected_used_file),
            "{expected_used_file} should be discovered via the hidden-dir allowlist, unused files: {unused_files:?}"
        );
    }

    let unused_exports = collect_unused_exports(&root, &results);
    assert!(
        !has_unused_export(&unused_exports, "docs/.vitepress/theme/index.ts", "default"),
        "theme entry default export should be treated as framework-used, found: {unused_exports:?}"
    );
    assert!(
        has_unused_export(
            &unused_exports,
            "docs/.vitepress/theme/index.ts",
            "unusedThemeHelper"
        ),
        "unused VitePress theme helper should still be reported, found: {unused_exports:?}"
    );
}
