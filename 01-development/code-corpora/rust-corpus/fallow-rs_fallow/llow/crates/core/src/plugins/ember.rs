//! Ember.js / Glimmer / Embroider plugin.
//!
//! Activates on `ember-source`, `ember-cli`, `@embroider/core`,
//! `@embroider/compat`, or `@glimmer/component` dependencies. Tracks Ember's
//! build, test, and runtime tooling deps (so they are not flagged as unused),
//! whitelists the lifecycle and reflectively-invoked members on Ember's class
//! hierarchy, exposes Ember's filesystem-resolved conventions (the classic
//! `app/` and `tests/` layouts) as entry-point globs since those files are
//! loaded by the Ember resolver rather than by static `import`, and declares
//! Ember's `@ember/*` namespace as a virtual module prefix so
//! Embroider-rewritten specifiers like `@ember/object` and
//! `@ember/routing/router-service` don't surface as `unresolved-import`.
//!
//! Scoped to strict-mode Ember apps and v2 addons. Classic v1 addon layouts
//! (`addon/`, `addon-test-support/`) are intentionally out of scope:
//! they predate strict-mode `.gts` / `.gjs`, so the plugin's value-adds
//! (template scanner, virtual `@ember/*` prefixes, `.gts` parsing) don't
//! apply. v1-addon maintainers can list those paths via `entry` in their
//! fallow config; v2 addons follow the standard `package.json#main` /
//! `exports` entry shape and don't need framework-specific globs.
//!
//! Template-block import tracking (`<template>...</template>`, `.gjs`/`.gts`
//! single-file components, and `.hbs` references) is handled separately by the
//! Glimmer-aware extractor in `crates/extract/src/glimmer.rs`. **Co-located
//! `.hbs` templates remain a known limitation** of that extractor: imports
//! consumed only by a sibling `.hbs` file still surface as `unused-import` on
//! the sibling `.js`/`.ts`; see the module-level note in
//! `crates/extract/src/sfc_template/glimmer.rs`. `ENTRY_PATTERNS` below
//! includes `*.hbs` paths so the templates themselves stay reachable as
//! files; binding-level usage tracking inside them is out of scope until the
//! scanner gains a Handlebars front-end. Migrating a component to `.gts`
//! removes the limitation entirely. Decorator-form
//! component, service, helper, and modifier registration (`@classic`,
//! `@service`, `@tracked`, `@action`) flows through the visitor and is not
//! re-implemented here. This plugin only handles the lifecycle and convention
//! members that the framework calls reflectively at runtime.

use fallow_config::{ScopedUsedClassMemberRule, UsedClassMemberRule};

use super::Plugin;

const ENABLERS: &[&str] = &[
    "ember-source",
    "ember-cli",
    "@embroider/core",
    "@embroider/compat",
    "@glimmer/component",
];

/// Packages required by an Ember project but never statically imported from
/// source. Anything an `import` statement can reach in a modern Ember app
/// (`@glimmer/component`, `@ember/test-helpers`, `@ember-data/*`, ...) is
/// intentionally omitted: the normal import graph already credits those, so
/// listing them here would only mask real removals when a user genuinely
/// drops a dependency.
///
/// Note: a package may legitimately appear in BOTH this list and `ENABLERS`.
/// `ember-source` is both the activation signal (its presence in
/// `package.json` is how we detect an Ember project) AND a runtime-resolved
/// dependency that no source file imports directly. The two roles are
/// independent: enablers gate plugin activation; tooling deps suppress
/// `unused-dependency` for build-/CLI-/runtime-resolved packages. Don't
/// dedupe.
const TOOLING_DEPENDENCIES: &[&str] = &[
    "ember-source",
    "ember-cli",
    "ember-cli-htmlbars",
    "ember-cli-babel",
    "ember-auto-import",
    "@embroider/core",
    "@glint/core",
    "@glint/environment-ember-loose",
    "@glint/environment-ember-template-imports",
    "ember-cli-test-loader",
    "ember-exam",
    "ember-template-lint",
    "ember-template-imports",
    "ember-source-channel-url",
    "@ember/optional-features",
    "ember-cli-dependency-checker",
    "ember-cli-inject-live-reload",
    "ember-cli-sri",
    "ember-cli-terser",
    "loader.js",
    "broccoli-asset-rev",
    "ember-cli-app-version",
    "ember-export-application-global",
    "@tsconfig/ember",
    "@glint/tsserver-plugin",
];

/// Glimmer / classic Ember component lifecycle members called by the framework
/// at runtime. Covers both `@glimmer/component` and the legacy
/// `@ember/component` class hierarchy.
const COMPONENT_MEMBERS: &[&str] = &[
    "willDestroy",
    "didInsertElement",
    "didRender",
    "didUpdate",
    "didReceiveAttrs",
    "willRender",
    "willUpdate",
    "willClearRender",
    "willDestroyElement",
    "didDestroyElement",
];

/// Route hooks called by the Ember router during transitions, plus the
/// convention properties (`actions`, `queryParams`, `templateName`,
/// `controllerName`) that the resolver reads reflectively.
const ROUTE_MEMBERS: &[&str] = &[
    "model",
    "beforeModel",
    "afterModel",
    "setupController",
    "resetController",
    "redirect",
    "serialize",
    "deserialize",
    "activate",
    "deactivate",
    "buildRouteInfoMetadata",
    "actions",
    "queryParams",
    "templateName",
    "controllerName",
    "init",
    "willDestroy",
    "destroy",
];

/// Controller convention properties the Ember resolver reads reflectively
/// plus the `EmberObject` teardown lifecycle. `actions` and `queryParams`
/// are the common reflective cases; `templateName` and `controllerName`
/// are documented Ember APIs that the route's resolver honors when looking
/// up the paired template / controller for a route.
const CONTROLLER_MEMBERS: &[&str] = &[
    "actions",
    "queryParams",
    "templateName",
    "controllerName",
    "init",
    "willDestroy",
    "destroy",
];

const SERVICE_MEMBERS: &[&str] = &["init", "willDestroy", "destroy"];

const HELPER_MEMBERS: &[&str] = &["compute", "recompute", "init", "willDestroy", "destroy"];

const MODIFIER_MEMBERS: &[&str] = &[
    "modify",
    "willDestroy",
    "didReceiveArguments",
    "didInstall",
    "didUpdateArguments",
    "willRemove",
];

/// Application convention properties Ember reads at boot, plus the
/// `EmberObject` teardown lifecycle. `ready` is the framework-invoked
/// post-boot hook. `customEvents` / `eventDispatcher` / `resolver` /
/// `rootElement` are class fields that the framework consults during
/// initialization (e.g. `rootElement = '#my-app'`); without the
/// allowlist they would surface as unused class members on a user's
/// `Application` subclass.
const APPLICATION_MEMBERS: &[&str] = &[
    "ready",
    "customEvents",
    "eventDispatcher",
    "resolver",
    "rootElement",
    "init",
    "willDestroy",
    "destroy",
];

const ROUTER_MEMBERS: &[&str] = &[
    "map",
    "location",
    "rootURL",
    "willTransition",
    "didTransition",
];

/// Import-specifier prefixes that `ember-source` exposes through the AMD
/// loader (classic) or the Embroider rewriter (Embroider) rather than as
/// separate npm packages. Anything matching one of these prefixes is
/// suppressed from `unresolved-import` and `unlisted-dependency` reporting.
///
/// The list is **enumerated, not a blanket `@ember/`**, because parts of the
/// `@ember/*` namespace ARE real npm packages users install explicitly and
/// must keep listed in `package.json`:
///
/// - `@ember/test-helpers`
/// - `@ember/render-modifiers`
/// - `@ember/test-waiters`
/// - `@ember/string`
/// - `@ember/jquery`
/// - `@ember/legacy-built-in-components`
/// - `@ember/optional-features`
///
/// Silencing those with a blanket prefix would mask real missing-dep bugs.
///
/// Source of truth for the virtual list: `ember-source`'s `package.json`
/// `exports` field. Keep this in sync (it changes slowly; most additions
/// land as new subpaths under existing roots like `@ember/object/...` which
/// the prefix-match already covers).
///
/// Known gaps NOT covered (documented; users can `ignoreDependencies` or
/// add an inline `fallow-ignore-next-line unresolved-import`):
///
/// - Bare `import Ember from 'ember'`: a legacy Embroider-rewritten
///   specifier. A `"ember"` prefix would also catch `ember-cli`,
///   `ember-data`, `ember-source` and silence legitimate missing-dep
///   reports, so we leave it.
/// - v1 Ember addon subpaths (`ember-in-viewport/modifiers/in-viewport`,
///   `ember-power-select/components/...`): the v1 addon `addon/` tree
///   convention is not Node `exports`. A proper fix is addon-shape probing
///   in the resolver; the escape hatch today is `ignoreDependencies` per
///   addon (or migrating to its v2 build).
const VIRTUAL_MODULE_PREFIXES: &[&str] = &[
    "ember/",
    "@ember/application",
    "@ember/array",
    "@ember/canary-features",
    "@ember/component",
    "@ember/controller",
    "@ember/debug",
    "@ember/destroyable",
    "@ember/engine",
    "@ember/enumerable",
    "@ember/error",
    "@ember/helper",
    "@ember/instrumentation",
    "@ember/modifier",
    "@ember/object",
    "@ember/owner",
    "@ember/renderer",
    "@ember/routing",
    "@ember/runloop",
    "@ember/service",
    "@ember/template",
    "@ember/utils",
    "@ember/version",
];

const ENTRY_PATTERNS: &[&str] = &[
    "app/app.{js,ts,gjs,gts}",
    "app/router.{js,ts}",
    "app/index.html",
    "app/components/**/*.{js,ts,gjs,gts,hbs}",
    "app/routes/**/*.{js,ts,gjs,gts}",
    "app/controllers/**/*.{js,ts}",
    "app/templates/**/*.{hbs,gjs,gts}",
    "app/models/**/*.{js,ts}",
    "app/services/**/*.{js,ts}",
    "app/helpers/**/*.{js,ts,gjs,gts}",
    "app/modifiers/**/*.{js,ts}",
    "app/initializers/**/*.{js,ts}",
    "app/instance-initializers/**/*.{js,ts}",
    "app/adapters/**/*.{js,ts}",
    "app/serializers/**/*.{js,ts}",
    "app/transforms/**/*.{js,ts}",
    "tests/test-helper.{js,ts}",
    "tests/index.html",
    "tests/**/*-test.{js,ts,gjs,gts}",
    "config/environment.js",
    "config/targets.js",
    "config/optional-features.json",
    "config/deprecation-workflow.js",
    "ember-cli-build.js",
    "testem.js",
];

fn scoped_rule(extends: &str, members: &[&str]) -> UsedClassMemberRule {
    UsedClassMemberRule::Scoped(ScopedUsedClassMemberRule {
        extends: Some(extends.to_string()),
        implements: None,
        members: members.iter().map(|s| (*s).to_string()).collect(),
    })
}

pub struct EmberPlugin;

impl Plugin for EmberPlugin {
    fn name(&self) -> &'static str {
        "ember"
    }

    fn enablers(&self) -> &'static [&'static str] {
        ENABLERS
    }

    fn tooling_dependencies(&self) -> &'static [&'static str] {
        TOOLING_DEPENDENCIES
    }

    fn used_class_member_rules(&self) -> Vec<UsedClassMemberRule> {
        vec![
            scoped_rule("Component", COMPONENT_MEMBERS),
            scoped_rule("Route", ROUTE_MEMBERS),
            scoped_rule("Controller", CONTROLLER_MEMBERS),
            scoped_rule("Service", SERVICE_MEMBERS),
            scoped_rule("Helper", HELPER_MEMBERS),
            scoped_rule("Modifier", MODIFIER_MEMBERS),
            scoped_rule("Application", APPLICATION_MEMBERS),
            scoped_rule("Router", ROUTER_MEMBERS),
        ]
    }

    fn virtual_module_prefixes(&self) -> &'static [&'static str] {
        VIRTUAL_MODULE_PREFIXES
    }

    fn entry_patterns(&self) -> &'static [&'static str] {
        ENTRY_PATTERNS
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn enablers_cover_classic_embroider_and_glimmer() {
        let plugin = EmberPlugin;
        assert!(plugin.enablers().contains(&"ember-source"));
        assert!(plugin.enablers().contains(&"@embroider/core"));
        assert!(plugin.enablers().contains(&"@glimmer/component"));
    }

    #[test]
    fn tooling_dependencies_cover_runtime_only_packages() {
        let plugin = EmberPlugin;
        let deps = plugin.tooling_dependencies();
        assert!(deps.contains(&"ember-source"));
        assert!(deps.contains(&"ember-cli-htmlbars"));
        assert!(deps.contains(&"@embroider/core"));
        assert!(deps.contains(&"@glint/core"));
        assert!(deps.contains(&"ember-exam"));
        assert!(deps.contains(&"loader.js"));
        assert!(deps.contains(&"broccoli-asset-rev"));
        assert!(deps.contains(&"ember-cli-app-version"));
        assert!(deps.contains(&"ember-export-application-global"));
        assert!(deps.contains(&"@tsconfig/ember"));
        assert!(deps.contains(&"@glint/tsserver-plugin"));
    }

    #[test]
    fn tooling_dependencies_omits_source_imported_packages() {
        let plugin = EmberPlugin;
        let deps = plugin.tooling_dependencies();
        for name in [
            "@glimmer/component",
            "@glimmer/tracking",
            "@glimmer/env",
            "@glint/template",
            "@ember/test-helpers",
            "ember-qunit",
            "qunit",
            "qunit-dom",
            "ember-data",
            "@ember-data/store",
            "@ember-data/model",
            "@embroider/macros",
            "@embroider/router",
            "@embroider/test-setup",
            "@embroider/webpack",
            "@embroider/vite",
            "@embroider/addon-shim",
            "ember-load-initializers",
            "ember-resolver",
        ] {
            assert!(
                !deps.contains(&name),
                "{name} is imported from source in modern Ember; remove from tooling_dependencies"
            );
        }
    }

    #[test]
    fn lifecycle_rules_scope_component_members_to_glimmer_component() {
        let rules = EmberPlugin.used_class_member_rules();
        let component_rule = rules.iter().find_map(|r| match r {
            UsedClassMemberRule::Scoped(s) if s.extends.as_deref() == Some("Component") => Some(s),
            _ => None,
        });
        let component_rule = component_rule.expect("Component-scoped rule missing");
        assert!(component_rule.members.iter().any(|m| m == "willDestroy"));
        assert!(
            component_rule
                .members
                .iter()
                .any(|m| m == "didInsertElement")
        );
    }

    #[test]
    fn lifecycle_rules_scope_route_members_to_route_class() {
        let rules = EmberPlugin.used_class_member_rules();
        let route_rule = rules.iter().find_map(|r| match r {
            UsedClassMemberRule::Scoped(s) if s.extends.as_deref() == Some("Route") => Some(s),
            _ => None,
        });
        let route_rule = route_rule.expect("Route-scoped rule missing");
        assert!(route_rule.members.iter().any(|m| m == "model"));
        assert!(route_rule.members.iter().any(|m| m == "beforeModel"));
        assert!(route_rule.members.iter().any(|m| m == "setupController"));
    }

    #[test]
    fn unrelated_classes_get_no_lifecycle_rule_match() {
        let rules = EmberPlugin.used_class_member_rules();
        for r in &rules {
            let UsedClassMemberRule::Scoped(s) = r else {
                continue;
            };
            assert!(!s.matches_heritage(Some("UserService"), &[]));
        }
    }

    #[test]
    fn entry_patterns_cover_classic_layout() {
        let plugin = EmberPlugin;
        let patterns = plugin.entry_patterns();
        assert!(patterns.contains(&"app/components/**/*.{js,ts,gjs,gts,hbs}"));
        assert!(patterns.contains(&"tests/**/*-test.{js,ts,gjs,gts}"));
    }

    #[test]
    fn entry_patterns_do_not_include_v1_addon_layout() {
        let plugin = EmberPlugin;
        let patterns = plugin.entry_patterns();
        for v1_only in [
            "addon/**/*.{js,ts,gjs,gts,hbs}",
            "addon-test-support/**/*.{js,ts,gjs,gts}",
        ] {
            assert!(
                !patterns.contains(&v1_only),
                "{v1_only} must not be in entry_patterns (v1 addons are out \
                 of scope); current patterns = {patterns:?}"
            );
        }
    }

    /// Check that an import specifier `spec` would be silenced by the
    /// plugin's virtual-module prefix list. Delegates to the production
    /// `matches_virtual_prefix` matcher so this test cannot drift from the
    /// `unresolved-import` / `unlisted-dependency` suppression sites in
    /// `crates/core/src/analyze/unused_deps.rs`.
    fn is_covered(prefixes: &[&str], spec: &str) -> bool {
        prefixes
            .iter()
            .any(|prefix| crate::analyze::matches_virtual_prefix(prefix, spec))
    }

    #[test]
    fn virtual_module_prefixes_cover_ember_source_runtime() {
        let prefixes = EmberPlugin.virtual_module_prefixes();
        for spec in [
            "@ember/object",
            "@ember/object/computed",
            "@ember/template",
            "@ember/service",
            "@ember/runloop",
            "@ember/utils",
            "@ember/routing/router-service",
            "@ember/helper",
            "@ember/modifier",
            "@ember/application",
            "@ember/component",
            "@ember/component/helper",
            "@ember/controller",
            "@ember/debug",
            "@ember/destroyable",
            "@ember/object/proxy",
            "@ember/routing/route",
            "@ember/template-compilation",
            "@ember/template-factory",
            "@ember/owner",
            "ember",
        ] {
            assert!(
                is_covered(prefixes, spec),
                "expected `{spec}` to be silenced by the virtual-module \
                 prefix list (it is rewritten by Embroider / the AMD loader \
                 and not resolvable through node_modules); prefixes = \
                 {prefixes:?}",
            );
        }
    }

    #[test]
    fn virtual_module_prefixes_do_not_swallow_real_ember_npm_packages() {
        let prefixes = EmberPlugin.virtual_module_prefixes();
        for real in [
            "@ember/test-helpers",
            "@ember/render-modifiers",
            "@ember/test-waiters",
            "@ember/string",
            "@ember/jquery",
            "@ember/legacy-built-in-components",
            "@ember/optional-features",
        ] {
            assert!(
                !is_covered(prefixes, real),
                "`{real}` is a real npm package and must NOT be covered by \
                 the virtual-module prefix list; prefixes = {prefixes:?}",
            );
        }
    }

    #[test]
    fn virtual_module_prefixes_do_not_swallow_ember_dash_packages() {
        let prefixes = EmberPlugin.virtual_module_prefixes();
        for spec in [
            "@ember-data/store",
            "@ember-data/model",
            "@glimmer/component",
            "@glimmer/tracking",
            "ember-source",
            "ember-cli",
            "ember-data",
            "ember-in-viewport",
            "ember-template-lint",
        ] {
            assert!(
                !is_covered(prefixes, spec),
                "`{spec}` must NOT be covered by the virtual-module prefix \
                 list; prefixes = {prefixes:?}",
            );
        }
    }
}
