//! End-to-end smoke test for the Ember.js / Glimmer / Embroider plugin.
//! Covers the plugin suppressions and the HTML placeholder filter at the
//! `AnalysisResults` level.

use super::common::{create_config, fixture_path};

#[test]
fn ember_classic_fixture_recognises_plugin_suppressions() {
    let root = fixture_path("ember-classic");
    let config = create_config(root);
    let results = fallow_core::analyze(&config).expect("analysis should succeed");

    let unused_deps: Vec<&str> = results
        .unused_dependencies
        .iter()
        .map(|finding| finding.dep.package_name.as_str())
        .collect();
    let unlisted_deps: Vec<&str> = results
        .unlisted_dependencies
        .iter()
        .map(|finding| finding.dep.package_name.as_str())
        .collect();
    let unresolved: Vec<&str> = results
        .unresolved_imports
        .iter()
        .map(|finding| finding.import.specifier.as_str())
        .collect();
    let unused_members: Vec<(String, String)> = results
        .unused_class_members
        .iter()
        .map(|finding| {
            (
                finding.member.parent_name.clone(),
                finding.member.member_name.clone(),
            )
        })
        .collect();

    for tool in [
        "ember-source",
        "ember-cli",
        "ember-cli-htmlbars",
        "ember-cli-babel",
        "loader.js",
    ] {
        assert!(
            !unused_deps.contains(&tool),
            "{tool} should not surface as unused-dependency; unused_deps = {unused_deps:?}"
        );
    }

    for virtual_spec in [
        "@ember/object",
        "@ember/service",
        "@ember/routing/router-service",
        "@ember/controller",
    ] {
        assert!(
            !unresolved.contains(&virtual_spec),
            "{virtual_spec} should be silenced by virtual_module_prefixes; \
             unresolved_imports = {unresolved:?}"
        );
        let pkg = virtual_spec
            .split('/')
            .take(2)
            .collect::<Vec<_>>()
            .join("/");
        assert!(
            !unlisted_deps.contains(&pkg.as_str()),
            "{pkg} should be silenced by virtual_module_prefixes; \
             unlisted_dependencies = {unlisted_deps:?}"
        );
    }

    for placeholder_fragment in ["{{rootURL}}", "{{config.assetsPath}}"] {
        assert!(
            !unresolved
                .iter()
                .any(|spec| spec.contains(placeholder_fragment)),
            "{placeholder_fragment} must be filtered out by the HTML asset \
             scanner's template-placeholder check; unresolved_imports = {unresolved:?}"
        );
    }

    let lifecycle_must_survive = [
        ("SessionService", "init"),
        ("SessionService", "willDestroy"),
        ("ApplicationRoute", "model"),
        ("ApplicationRoute", "setupController"),
    ];
    for (parent, member) in lifecycle_must_survive {
        assert!(
            !unused_members
                .iter()
                .any(|(p, m)| p == parent && m == member),
            "{parent}.{member} is framework-invoked and must not surface as \
             unused-class-member; unused_class_members = {unused_members:?}"
        );
    }
}
