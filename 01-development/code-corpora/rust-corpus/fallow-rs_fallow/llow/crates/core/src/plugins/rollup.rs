//! Rollup module bundler plugin.
//!
//! Detects Rollup projects and marks config files as always used.
//! Parses rollup config to extract imports and plugin references as dependencies.

use super::config_parser;
use super::{Plugin, PluginResult};

const ENABLERS: &[&str] = &["rollup"];

const CONFIG_PATTERNS: &[&str] = &["rollup.config.{js,ts,mjs,cjs}"];

const ALWAYS_USED: &[&str] = &["rollup.config.{js,ts,mjs,cjs}"];

const TOOLING_DEPENDENCIES: &[&str] = &["rollup"];

define_plugin! {
    struct RollupPlugin => "rollup",
    enablers: ENABLERS,
    config_patterns: CONFIG_PATTERNS,
    always_used: ALWAYS_USED,
    tooling_dependencies: TOOLING_DEPENDENCIES,
    resolve_config(config_path, source, _root) {
        let mut result = PluginResult::default();
        super::add_import_referenced_dependencies(&mut result, source, config_path);

        let inputs = config_parser::extract_config_string_or_array(source, config_path, &["input"]);
        result.extend_entry_patterns(inputs);

        let external =
            config_parser::extract_config_shallow_strings(source, config_path, "external");
        for ext in &external {
            result
                .referenced_dependencies
                .push(crate::resolve::extract_package_name(ext));
        }

        result
    }
}

#[cfg(test)]
mod tests {
    use std::path::Path;

    use super::*;

    #[test]
    fn resolve_config_input_string() {
        let source = r#"export default { input: "./src/index.js" };"#;
        let plugin = RollupPlugin;
        let result =
            plugin.resolve_config(Path::new("rollup.config.js"), source, Path::new("/project"));
        assert_eq!(result.entry_patterns, vec!["src/index.js"]);
    }

    #[test]
    fn resolve_config_input_array() {
        let source = r#"
            export default {
                input: ["./src/index.js", "./src/cli.js"]
            };
        "#;
        let plugin = RollupPlugin;
        let result =
            plugin.resolve_config(Path::new("rollup.config.js"), source, Path::new("/project"));
        assert_eq!(result.entry_patterns, vec!["src/index.js", "src/cli.js"]);
    }

    #[test]
    fn resolve_config_input_object() {
        let source = r#"
            export default {
                input: {
                    main: "./src/main.js",
                    vendor: "./src/vendor.js"
                }
            };
        "#;
        let plugin = RollupPlugin;
        let result =
            plugin.resolve_config(Path::new("rollup.config.js"), source, Path::new("/project"));
        assert_eq!(result.entry_patterns, vec!["src/main.js", "src/vendor.js"]);
    }

    #[test]
    fn resolve_config_external() {
        let source = r#"
            export default {
                input: "./src/index.js",
                external: ["lodash", "react", "@scope/pkg"]
            };
        "#;
        let plugin = RollupPlugin;
        let result =
            plugin.resolve_config(Path::new("rollup.config.js"), source, Path::new("/project"));
        let deps = &result.referenced_dependencies;
        assert!(deps.contains(&"lodash".to_string()));
        assert!(deps.contains(&"react".to_string()));
        assert!(deps.contains(&"@scope/pkg".to_string()));
    }

    #[test]
    fn resolve_config_imports() {
        let source = r#"
            import resolve from '@rollup/plugin-node-resolve';
            import commonjs from '@rollup/plugin-commonjs';
            export default {
                input: "./src/index.js"
            };
        "#;
        let plugin = RollupPlugin;
        let result =
            plugin.resolve_config(Path::new("rollup.config.js"), source, Path::new("/project"));
        let deps = &result.referenced_dependencies;
        assert!(deps.contains(&"@rollup/plugin-node-resolve".to_string()));
        assert!(deps.contains(&"@rollup/plugin-commonjs".to_string()));
    }

    #[test]
    fn resolve_config_empty() {
        let source = r"export default {};";
        let plugin = RollupPlugin;
        let result =
            plugin.resolve_config(Path::new("rollup.config.js"), source, Path::new("/project"));
        assert!(result.entry_patterns.is_empty());
        assert!(result.referenced_dependencies.is_empty());
    }

    #[test]
    fn resolve_config_no_input() {
        let source = r#"
            export default {
                output: { dir: "dist" }
            };
        "#;
        let plugin = RollupPlugin;
        let result =
            plugin.resolve_config(Path::new("rollup.config.js"), source, Path::new("/project"));
        assert!(result.entry_patterns.is_empty());
    }
}
