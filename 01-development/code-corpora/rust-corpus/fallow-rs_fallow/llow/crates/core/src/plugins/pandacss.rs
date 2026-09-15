//! `PandaCSS` plugin.
//!
//! Detects `PandaCSS` projects (via `@pandacss/dev`) and marks the convention-based
//! `panda.config.{ts,js,mjs,cjs}` as always used. The `panda` CLI discovers the
//! config by filesystem convention with no import edge, so static analysis cannot
//! see it as used.
//!
//! Parses the config to collect imports as referenced dependencies so preset and
//! plugin packages (e.g. `@pandacss/preset-panda`, `@park-ui/panda-preset`) are
//! not flagged as unused.

use super::config_parser;
use super::{Plugin, PluginResult};

const ENABLERS: &[&str] = &["@pandacss/dev"];

const CONFIG_PATTERNS: &[&str] = &["panda.config.{ts,js,mjs,cjs}"];

const ALWAYS_USED: &[&str] = &["panda.config.{ts,js,mjs,cjs}"];

const TOOLING_DEPENDENCIES: &[&str] = &[
    "@pandacss/dev",
    "@pandacss/studio",
    "@pandacss/eslint-plugin",
];

define_plugin! {
    struct PandaCssPlugin => "pandacss",
    enablers: ENABLERS,
    config_patterns: CONFIG_PATTERNS,
    always_used: ALWAYS_USED,
    tooling_dependencies: TOOLING_DEPENDENCIES,
    resolve_config(config_path, source, _root) {
        let mut result = PluginResult::default();
        super::add_import_referenced_dependencies(&mut result, source, config_path);

        let preset_strings =
            config_parser::extract_config_shallow_strings(source, config_path, "presets");
        for preset in &preset_strings {
            result
                .referenced_dependencies
                .push(crate::resolve::extract_package_name(preset));
        }

        result
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn enabler_matches_pandacss_dev() {
        let plugin = PandaCssPlugin;
        let deps = vec!["@pandacss/dev".to_string()];
        assert!(plugin.is_enabled_with_deps(&deps, std::path::Path::new("/project")));
    }

    #[test]
    fn enabler_ignores_unrelated_deps() {
        let plugin = PandaCssPlugin;
        let deps = vec!["tailwindcss".to_string()];
        assert!(!plugin.is_enabled_with_deps(&deps, std::path::Path::new("/project")));
    }

    #[test]
    fn resolve_config_extracts_preset_imports() {
        let source = r"
            import { defineConfig } from '@pandacss/dev';
            import pandaPreset from '@pandacss/preset-panda';
            import parkPreset from '@park-ui/panda-preset';
            export default defineConfig({
                presets: [pandaPreset, parkPreset],
            });
        ";
        let plugin = PandaCssPlugin;
        let result = plugin.resolve_config(
            std::path::Path::new("panda.config.ts"),
            source,
            std::path::Path::new("/project"),
        );
        let deps = &result.referenced_dependencies;
        assert!(deps.contains(&"@pandacss/dev".to_string()));
        assert!(deps.contains(&"@pandacss/preset-panda".to_string()));
        assert!(deps.contains(&"@park-ui/panda-preset".to_string()));
    }

    #[test]
    fn resolve_config_extracts_preset_string_entries() {
        let source = r"
            import { defineConfig } from '@pandacss/dev';
            export default defineConfig({
                presets: ['@pandacss/preset-panda', '@park-ui/panda-preset'],
            });
        ";
        let plugin = PandaCssPlugin;
        let result = plugin.resolve_config(
            std::path::Path::new("panda.config.ts"),
            source,
            std::path::Path::new("/project"),
        );
        let deps = &result.referenced_dependencies;
        assert!(deps.contains(&"@pandacss/preset-panda".to_string()));
        assert!(deps.contains(&"@park-ui/panda-preset".to_string()));
    }

    #[test]
    fn resolve_config_empty_captures_define_config_import() {
        let source = r"
            import { defineConfig } from '@pandacss/dev';
            export default defineConfig({});
        ";
        let plugin = PandaCssPlugin;
        let result = plugin.resolve_config(
            std::path::Path::new("panda.config.ts"),
            source,
            std::path::Path::new("/project"),
        );
        assert_eq!(
            result.referenced_dependencies,
            vec!["@pandacss/dev".to_string()]
        );
    }
}
