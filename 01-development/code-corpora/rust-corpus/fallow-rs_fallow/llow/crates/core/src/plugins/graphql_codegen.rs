//! GraphQL Codegen plugin.
//!
//! Detects GraphQL Codegen projects and marks config files as always used.
//! Parses codegen config to extract referenced dependencies.

use super::{Plugin, PluginResult};

const ENABLERS: &[&str] = &["@graphql-codegen/cli"];

const CONFIG_PATTERNS: &[&str] = &["codegen.{ts,js}", "graphql.config.{ts,js}"];

const ALWAYS_USED: &[&str] = &[
    "codegen.{ts,js,yml,yaml}",
    "graphql.config.{ts,js,yml,yaml}",
];

const TOOLING_DEPENDENCIES: &[&str] = &[
    "@graphql-codegen/cli",
    "@graphql-codegen/typescript",
    "@graphql-codegen/typescript-operations",
    "@graphql-codegen/typescript-react-query",
];

define_plugin! {
    struct GraphqlCodegenPlugin => "graphql-codegen",
    enablers: ENABLERS,
    config_patterns: CONFIG_PATTERNS,
    always_used: ALWAYS_USED,
    tooling_dependencies: TOOLING_DEPENDENCIES,
    resolve_config: imports_only,
}
