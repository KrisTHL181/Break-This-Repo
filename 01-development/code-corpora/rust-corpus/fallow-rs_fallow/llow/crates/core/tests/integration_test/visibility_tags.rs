use super::common::{create_config, fixture_path};

#[test]
fn public_tag_prevents_unused_export_detection() {
    let root = fixture_path("visibility-tags");
    let config = create_config(root);
    let results = fallow_core::analyze(&config).expect("analysis should succeed");

    let unused_export_names: Vec<&str> = results
        .unused_exports
        .iter()
        .map(|e| e.export.export_name.as_str())
        .collect();

    assert!(!unused_export_names.contains(&"publicExport"));
    assert!(!unused_export_names.contains(&"internalExport"));
    assert!(!unused_export_names.contains(&"betaExport"));
    assert!(!unused_export_names.contains(&"alphaExport"));
}

#[test]
fn untagged_unused_export_still_detected() {
    let root = fixture_path("visibility-tags");
    let config = create_config(root);
    let results = fallow_core::analyze(&config).expect("analysis should succeed");

    let unused_export_names: Vec<&str> = results
        .unused_exports
        .iter()
        .map(|e| e.export.export_name.as_str())
        .collect();

    assert!(unused_export_names.contains(&"trulyUnused"));
    assert!(!unused_export_names.contains(&"usedExport"));
}
