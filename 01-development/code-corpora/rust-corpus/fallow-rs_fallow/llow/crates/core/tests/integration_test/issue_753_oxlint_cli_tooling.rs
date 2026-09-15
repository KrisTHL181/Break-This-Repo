use super::common::{create_config, fixture_path};

#[test]
fn oxlint_cli_tooling_credited_in_prod_dependencies() {
    let root = fixture_path("issue-753-oxlint-cli-tooling");
    let config = create_config(root);
    let results = fallow_core::analyze(&config).expect("analysis should succeed");

    let unused_dependencies: Vec<&str> = results
        .unused_dependencies
        .iter()
        .map(|dep| dep.dep.package_name.as_str())
        .collect();

    assert!(
        !unused_dependencies.contains(&"oxlint-tsgolint"),
        "oxlint-tsgolint should be credited as an oxlint CLI tooling dependency, got {unused_dependencies:?}"
    );

    assert!(
        unused_dependencies.contains(&"oxlint-other"),
        "an unknown oxlint-prefixed prod dependency should still report, got {unused_dependencies:?}"
    );

    assert!(
        unused_dependencies.contains(&"unused-control-dep"),
        "an unrelated unused prod dependency should still report, got {unused_dependencies:?}"
    );
}
