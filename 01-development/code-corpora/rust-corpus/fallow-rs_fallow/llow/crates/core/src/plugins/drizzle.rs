//! Drizzle ORM plugin.
//!
//! Detects Drizzle projects and marks migration/schema files as entry points.
//! Parses drizzle.config to extract the `schema` field (making schema file
//! exports framework-consumed entry points) and the `out` field (custom
//! migration output directory). Also extracts referenced dependencies from
//! import statements.

use std::path::Path;

use super::config_parser;
use super::{Plugin, PluginResult};

const ENABLERS: &[&str] = &["drizzle-orm"];

const ENTRY_PATTERNS: &[&str] = &["drizzle/**/*.{ts,js}"];

const CONFIG_PATTERNS: &[&str] = &["drizzle.config.{ts,js,mjs}"];

const ALWAYS_USED: &[&str] = &["drizzle.config.{ts,js,mjs}"];

const TOOLING_DEPENDENCIES: &[&str] = &["drizzle-orm", "drizzle-kit"];

define_plugin! {
    struct DrizzlePlugin => "drizzle",
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

        let schema_paths =
            config_parser::extract_config_string_or_array(source, config_path, &["schema"]);
        for path in &schema_paths {
            result.extend_entry_patterns(schema_path_to_entry_patterns(path));
        }

        if let Some(out_dir) = config_parser::extract_config_string(source, config_path, &["out"]) {
            let out = out_dir.trim_start_matches("./").trim_end_matches('/');
            if out != "drizzle" {
                result.push_entry_pattern(format!("{out}/**/*.{{ts,js}}"));
            }
        }

        result
    }
}

/// Convert a schema path from drizzle.config into entry patterns.
///
/// Returns one or more glob patterns:
/// - Glob patterns (`src/db/*.ts`) → used as-is
/// - Directory paths (`src/db/schema`) → `dir/**/*.{ts,...}`
/// - Index/barrel files (`src/db/schema/index.ts`) → the file itself PLUS
///   `dir/**/*.{ts,...}` for siblings, because Drizzle follows imports from
///   the barrel to discover all schema files
/// - Other files (`src/db/schema.ts`) → just the file
fn schema_path_to_entry_patterns(path: &str) -> Vec<String> {
    let path = path.trim_start_matches("./");

    if path.contains('*') || path.contains('?') || path.contains('{') || path.contains('[') {
        return vec![path.to_string()];
    }

    if let Some(ext) = Path::new(path).extension().and_then(|e| e.to_str())
        && matches!(
            ext,
            "ts" | "tsx" | "js" | "jsx" | "mts" | "mjs" | "cts" | "cjs"
        )
    {
        let mut patterns = vec![path.to_string()];

        if let Some(stem) = Path::new(path).file_stem().and_then(|s| s.to_str())
            && stem == "index"
            && let Some(parent) = Path::new(path).parent()
            && parent != Path::new("")
        {
            let dir = parent.to_string_lossy();
            patterns.push(format!("{dir}/**/*.{{ts,tsx,js,jsx,mts,mjs,cts,cjs}}"));
        }

        return patterns;
    }

    vec![format!("{path}/**/*.{{ts,tsx,js,jsx,mts,mjs,cts,cjs}}")]
}

#[cfg(test)]
mod tests {
    use super::*;

    fn has_entry_pattern(result: &PluginResult, pattern: &str) -> bool {
        result
            .entry_patterns
            .iter()
            .any(|entry_pattern| entry_pattern.pattern == pattern)
    }

    #[test]
    fn schema_path_file() {
        assert_eq!(
            schema_path_to_entry_patterns("./src/db/schema.ts"),
            vec!["src/db/schema.ts"]
        );
    }

    #[test]
    fn schema_path_index_file_adds_directory() {
        assert_eq!(
            schema_path_to_entry_patterns("./src/db/schema/index.ts"),
            vec![
                "src/db/schema/index.ts".to_string(),
                "src/db/schema/**/*.{ts,tsx,js,jsx,mts,mjs,cts,cjs}".to_string(),
            ]
        );
    }

    #[test]
    fn schema_path_directory() {
        assert_eq!(
            schema_path_to_entry_patterns("./src/db/schema"),
            vec!["src/db/schema/**/*.{ts,tsx,js,jsx,mts,mjs,cts,cjs}"]
        );
    }

    #[test]
    fn schema_path_glob() {
        assert_eq!(
            schema_path_to_entry_patterns("./src/db/*.ts"),
            vec!["src/db/*.ts"]
        );
    }

    #[test]
    fn schema_path_no_prefix() {
        assert_eq!(
            schema_path_to_entry_patterns("src/db/schema.ts"),
            vec!["src/db/schema.ts"]
        );
    }

    #[test]
    fn resolve_config_extracts_schema_string() {
        let source = r#"
            import { defineConfig } from "drizzle-kit";
            export default defineConfig({
                schema: "./src/db/schema.ts",
                out: "./drizzle",
                dialect: "postgresql",
            });
        "#;
        let plugin = DrizzlePlugin;
        let result = plugin.resolve_config(
            Path::new("drizzle.config.ts"),
            source,
            Path::new("/project"),
        );
        assert!(has_entry_pattern(&result, "src/db/schema.ts"));
        assert!(
            result
                .referenced_dependencies
                .contains(&"drizzle-kit".to_string())
        );
    }

    #[test]
    fn resolve_config_extracts_schema_array() {
        let source = r#"
            export default {
                schema: ["./src/db/users.ts", "./src/db/posts.ts"],
                out: "./drizzle",
                dialect: "postgresql",
            };
        "#;
        let plugin = DrizzlePlugin;
        let result = plugin.resolve_config(
            Path::new("drizzle.config.ts"),
            source,
            Path::new("/project"),
        );
        assert!(has_entry_pattern(&result, "src/db/users.ts"));
        assert!(has_entry_pattern(&result, "src/db/posts.ts"));
    }

    #[test]
    fn resolve_config_extracts_schema_directory() {
        let source = r#"
            export default {
                schema: "./src/db",
                dialect: "postgresql",
            };
        "#;
        let plugin = DrizzlePlugin;
        let result = plugin.resolve_config(
            Path::new("drizzle.config.ts"),
            source,
            Path::new("/project"),
        );
        assert!(has_entry_pattern(
            &result,
            "src/db/**/*.{ts,tsx,js,jsx,mts,mjs,cts,cjs}"
        ));
    }

    #[test]
    fn resolve_config_extracts_schema_glob() {
        let source = r#"
            export default {
                schema: "./src/db/*.ts",
                dialect: "postgresql",
            };
        "#;
        let plugin = DrizzlePlugin;
        let result = plugin.resolve_config(
            Path::new("drizzle.config.ts"),
            source,
            Path::new("/project"),
        );
        assert!(has_entry_pattern(&result, "src/db/*.ts"));
    }

    #[test]
    fn resolve_config_custom_out_dir() {
        let source = r#"
            export default {
                schema: "./src/db/schema.ts",
                out: "./migrations",
                dialect: "postgresql",
            };
        "#;
        let plugin = DrizzlePlugin;
        let result = plugin.resolve_config(
            Path::new("drizzle.config.ts"),
            source,
            Path::new("/project"),
        );
        assert!(has_entry_pattern(&result, "src/db/schema.ts"));
        assert!(has_entry_pattern(&result, "migrations/**/*.{ts,js}"));
    }

    #[test]
    fn resolve_config_default_out_dir_not_duplicated() {
        let source = r#"
            export default {
                schema: "./src/db/schema.ts",
                out: "./drizzle",
                dialect: "postgresql",
            };
        "#;
        let plugin = DrizzlePlugin;
        let result = plugin.resolve_config(
            Path::new("drizzle.config.ts"),
            source,
            Path::new("/project"),
        );
        assert!(
            !result
                .entry_patterns
                .iter()
                .any(|p| p.starts_with("drizzle/"))
        );
    }

    #[test]
    fn resolve_config_module_exports() {
        let source = r#"
            module.exports = {
                schema: "./src/db/schema",
                out: "./migrations",
                dialect: "mysql",
            };
        "#;
        let plugin = DrizzlePlugin;
        let result = plugin.resolve_config(
            Path::new("drizzle.config.js"),
            source,
            Path::new("/project"),
        );
        assert!(has_entry_pattern(
            &result,
            "src/db/schema/**/*.{ts,tsx,js,jsx,mts,mjs,cts,cjs}"
        ));
        assert!(has_entry_pattern(&result, "migrations/**/*.{ts,js}"));
    }
}
