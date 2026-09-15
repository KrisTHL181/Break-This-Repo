//! Cypress test runner plugin.
//!
//! Detects Cypress projects and marks test files and support files as entry points.
//! Parses `cypress.config.{ts,js}` to extract referenced dependencies and to seed
//! `e2e.specPattern`, `component.specPattern`, `e2e.supportFile`, and
//! `component.supportFile` as entry points. Cypress's default component spec
//! pattern is also seeded so `*.cy.*` files outside the default `cypress/**`
//! location are not reported as `unused-files` when the config omits
//! `component.specPattern`.
//!
//! See issue #195 (Case E).

use super::config_parser;
use super::{Plugin, PluginResult};

const ENABLERS: &[&str] = &["cypress"];

const ENTRY_PATTERNS: &[&str] = &[
    "**/*.cy.{ts,tsx,js,jsx}",
    "cypress/**/*.{ts,tsx,js,jsx}",
    "cypress/support/**/*.{ts,js}",
];

const CONFIG_PATTERNS: &[&str] = &["cypress.config.{ts,js,mjs,cjs}"];

const ALWAYS_USED: &[&str] = &["cypress.config.{ts,js,mjs,cjs}"];

const TOOLING_DEPENDENCIES: &[&str] = &["cypress", "@cypress/react", "@cypress/vue"];

define_plugin!(
    struct CypressPlugin => "cypress",
    enablers: ENABLERS,
    entry_patterns: ENTRY_PATTERNS,
    config_patterns: CONFIG_PATTERNS,
    always_used: ALWAYS_USED,
    tooling_dependencies: TOOLING_DEPENDENCIES,
    resolve_config(config_path, source, _root) {
        let mut result = PluginResult::default();

        let imports = config_parser::extract_imports(source, config_path);
        for imp in &imports {
            let dep = crate::resolve::extract_package_name(imp);
            result.referenced_dependencies.push(dep);
        }

        for section in ["e2e", "component"] {
            let spec_patterns = config_parser::extract_config_string_or_array(
                source,
                config_path,
                &[section, "specPattern"],
            );
            result.extend_entry_patterns(spec_patterns);

            let support_file = config_parser::extract_config_string_or_array(
                source,
                config_path,
                &[section, "supportFile"],
            );
            result.extend_entry_patterns(support_file);
        }

        result
    },
);
