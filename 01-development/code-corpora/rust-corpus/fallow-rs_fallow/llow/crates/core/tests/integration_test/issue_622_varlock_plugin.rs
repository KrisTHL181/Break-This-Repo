use super::common::{create_config, fixture_path};

fn unused_file_paths(
    root: &std::path::Path,
    results: &fallow_types::results::AnalysisResults,
) -> Vec<String> {
    results
        .unused_files
        .iter()
        .map(|finding| {
            finding
                .file
                .path
                .strip_prefix(root)
                .unwrap_or(&finding.file.path)
                .to_string_lossy()
                .replace('\\', "/")
        })
        .collect()
}

#[test]
fn issue_622_varlock_schema_and_vite_integration_dependencies_are_reachable() {
    let root = fixture_path("issue-622-varlock-plugin");
    let config = create_config(root.clone());
    let results = fallow_core::analyze(&config).expect("analysis should succeed");

    let unused_paths = unused_file_paths(&root, &results);
    assert!(
        !unused_paths.contains(&".env.schema".to_string()),
        "Varlock schema should be reachable, unused files: {unused_paths:?}"
    );
    assert!(
        unused_paths.contains(&"src/orphan.ts".to_string()),
        "ordinary unused files should still report, unused files: {unused_paths:?}"
    );

    let unused_dev_dependencies: Vec<&str> = results
        .unused_dev_dependencies
        .iter()
        .map(|dep| dep.dep.package_name.as_str())
        .collect();
    for dep in [
        "@varlock/google-secret-manager-plugin",
        "@varlock/vite-integration",
        "varlock",
    ] {
        assert!(
            !unused_dev_dependencies.contains(&dep),
            "{dep} should be credited by Varlock plugin support, unused dev deps: {unused_dev_dependencies:?}"
        );
    }
    assert!(
        unused_dev_dependencies.contains(&"unused-control"),
        "unreferenced control dependency should still be reported, unused dev deps: {unused_dev_dependencies:?}"
    );
}

#[test]
fn issue_622_varlock_activates_from_nested_schema_without_dependency() {
    // No `varlock` / `@varlock/*` dependency and no root-level `.env.schema`:
    // activation depends solely on the nested `apps/web/.env.schema` the
    // discovery walk captured. Its `@plugin(varlock-custom-provider)` package
    // must therefore be credited rather than reported as an unused dependency.
    let root = fixture_path("issue-622-varlock-nested-schema");
    let config = create_config(root);
    let results = fallow_core::analyze(&config).expect("analysis should succeed");

    let unused_dev_dependencies: Vec<&str> = results
        .unused_dev_dependencies
        .iter()
        .map(|dep| dep.dep.package_name.as_str())
        .collect();

    assert!(
        !unused_dev_dependencies.contains(&"varlock-custom-provider"),
        "nested .env.schema @plugin package should be credited via varlock activation, unused dev deps: {unused_dev_dependencies:?}"
    );
    assert!(
        unused_dev_dependencies.contains(&"unused-control"),
        "unreferenced control dependency should still be reported, unused dev deps: {unused_dev_dependencies:?}"
    );
}
