use super::common::{create_config, fixture_path};
use fallow_config::{FallowConfig, OutputFormat, Severity};

#[test]
fn hidden_dir_allowlist_includes_storybook() {
    let root = fixture_path("hidden-dir-allowlist");
    let config = create_config(root);
    let results = fallow_core::analyze(&config).expect("analysis should succeed");

    let unused_files: Vec<String> = results
        .unused_files
        .iter()
        .map(|f| f.file.path.to_string_lossy().to_string())
        .collect();

    assert!(
        !unused_files.iter().any(|f| f.contains(".storybook")),
        ".storybook/main.ts should not be in unused_files since it's imported. Got: {unused_files:?}"
    );
}

#[test]
fn hidden_dir_non_allowlisted_is_skipped() {
    let root = fixture_path("hidden-dir-allowlist");
    let config = create_config(root);
    let results = fallow_core::analyze(&config).expect("analysis should succeed");

    let all_paths: Vec<String> = results
        .unused_files
        .iter()
        .map(|f| f.file.path.to_string_lossy().to_string())
        .chain(
            results
                .unused_exports
                .iter()
                .map(|e| e.export.path.to_string_lossy().to_string()),
        )
        .collect();

    assert!(
        !all_paths.iter().any(|f| f.contains(".hidden-other")),
        ".hidden-other/ should be completely skipped. Got: {all_paths:?}"
    );
}

#[test]
fn astro_files_parsed_and_analyzed() {
    let root = fixture_path("astro-project");
    let config = create_config(root);
    let results = fallow_core::analyze(&config).expect("analysis should succeed");

    let unused_files: Vec<String> = results
        .unused_files
        .iter()
        .map(|f| f.file.path.to_string_lossy().to_string())
        .collect();

    assert!(
        unused_files.iter().any(|f| f.contains("Orphan")),
        "Orphan.astro should be in unused_files. Got: {unused_files:?}"
    );
}

#[test]
fn mdx_unused_file_detected() {
    let root = fixture_path("mdx-project");
    let config = create_config(root);
    let results = fallow_core::analyze(&config).expect("analysis should succeed");

    let unused_files: Vec<String> = results
        .unused_files
        .iter()
        .map(|f| f.file.path.to_string_lossy().to_string())
        .collect();

    assert!(
        unused_files.iter().any(|f| f.contains("Unused")),
        "Unused.tsx should be in unused_files. Got: {unused_files:?}"
    );
}

#[test]
fn mdx_code_fence_imports_do_not_report_unresolved() {
    let root = fixture_path("mdx-project");
    let config = create_config(root);
    let results = fallow_core::analyze(&config).expect("analysis should succeed");

    let unresolved_specifiers: Vec<&str> = results
        .unresolved_imports
        .iter()
        .map(|u| u.import.specifier.as_str())
        .collect();

    assert!(
        unresolved_specifiers.contains(&"./real-missing"),
        "top-level MDX imports should still be analyzed, found: {unresolved_specifiers:?}"
    );
    for specifier in ["./exampleSlice", "./store/hooks"] {
        assert!(
            !unresolved_specifiers.contains(&specifier),
            "MDX code fence import {specifier} should not be unresolved, found: {unresolved_specifiers:?}"
        );
    }
}

#[test]
fn complexity_project_analyzes_without_errors() {
    let root = fixture_path("complexity-project");
    let config = create_config(root);
    let results = fallow_core::analyze(&config).expect("analysis should succeed");

    assert!(
        results.unresolved_imports.is_empty(),
        "complexity-project should have no unresolved imports"
    );
}

#[test]
fn error_no_package_json_produces_empty_results() {
    let root = fixture_path("error-no-package-json");
    let config = create_config(root);
    let results = fallow_core::analyze(&config).expect("analysis should succeed");

    assert_eq!(
        results.total_issues(),
        0,
        "no package.json should produce empty results"
    );
}

#[test]
fn toml_config_loads_and_applies_rules() {
    let root = fixture_path("config-toml-project");
    let config_path = root.join("fallow.toml");

    let found = FallowConfig::find_config_path(&root);
    assert_eq!(
        found.as_deref(),
        Some(config_path.as_path()),
        "find_config_path should discover fallow.toml"
    );

    let loaded = FallowConfig::load(&config_path).expect("TOML config should load");

    assert_eq!(
        loaded.rules.unused_files,
        Severity::Warn,
        "unused-files should be Warn per fallow.toml"
    );

    assert_eq!(
        loaded.rules.unused_exports,
        Severity::Error,
        "unused-exports should default to Error"
    );

    let resolved = loaded.resolve(root, OutputFormat::Human, 4, true, true, None);
    let results = fallow_core::analyze(&resolved).expect("analysis should succeed");

    let unused_files: Vec<String> = results
        .unused_files
        .iter()
        .map(|f| f.file.path.to_string_lossy().replace('\\', "/"))
        .collect();

    assert!(
        unused_files.iter().any(|f| f.ends_with("src/orphan.ts")),
        "orphan.ts should be in unused_files. Got: {unused_files:?}"
    );

    let unused_export_names: Vec<&str> = results
        .unused_exports
        .iter()
        .map(|e| e.export.export_name.as_str())
        .collect();

    assert!(
        unused_export_names.contains(&"unusedFunction"),
        "unusedFunction should be in unused_exports. Got: {unused_export_names:?}"
    );
}
