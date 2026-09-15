use super::common::{create_config, fixture_path};

/// Issue #606: ng-packagr `lib.entryFile` files stay reachable as public API, including secondary entry points.
#[test]
fn ng_package_entry_file_keeps_public_api_reachable() {
    let root = fixture_path("ng-package-entrypoint");
    let config = create_config(root);
    let results = fallow_core::analyze(&config).expect("analysis should succeed");

    let unused_file_paths: Vec<String> = results
        .unused_files
        .iter()
        .map(|f| f.file.path.to_string_lossy().replace('\\', "/"))
        .collect();
    let is_unused = |suffix: &str| unused_file_paths.iter().any(|p| p.ends_with(suffix));

    for path in [
        "ng-package-entrypoint/src/public-api.ts",
        "ng-package-entrypoint/src/composables.ts",
        "ng-package-entrypoint/src/context.ts",
        "ng-package-entrypoint/client/src/public_api.ts",
        "ng-package-entrypoint/client/src/client.ts",
    ] {
        assert!(
            !is_unused(path),
            "{path} should be reachable as ng-package public API: {unused_file_paths:?}"
        );
    }

    let unused_export_names: Vec<&str> = results
        .unused_exports
        .iter()
        .map(|e| e.export.export_name.as_str())
        .collect();
    for name in [
        "useHead",
        "useHeadSafe",
        "headSymbol",
        "UnheadInjectionToken",
        "useClientHead",
    ] {
        assert!(
            !unused_export_names.contains(&name),
            "{name} is part of the public API surface, must not be unused: {unused_export_names:?}"
        );
    }

    assert!(
        is_unused("ng-package-entrypoint/src/internal-unused.ts"),
        "internal-unused.ts is unreachable and must stay flagged: {unused_file_paths:?}"
    );
}
