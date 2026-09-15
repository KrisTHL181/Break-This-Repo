//! UnoCSS atomic CSS engine plugin.
//!
//! Detects UnoCSS projects and marks config files as always used.
//! Parses uno.config to extract preset and plugin dependencies.

use super::{Plugin, PluginResult};

const ENABLERS: &[&str] = &["unocss", "@unocss/"];

const CONFIG_PATTERNS: &[&str] = &[
    "uno.config.{ts,js,mjs,cjs}",
    "unocss.config.{ts,js,mjs,cjs}",
];

const ALWAYS_USED: &[&str] = &[
    "uno.config.{ts,js,mjs,cjs}",
    "unocss.config.{ts,js,mjs,cjs}",
];

const TOOLING_DEPENDENCIES: &[&str] = &[
    "unocss",
    "@unocss/cli",
    "@unocss/postcss",
    "@unocss/vite",
    "@unocss/webpack",
    "@unocss/astro",
    "@unocss/nuxt",
    "@unocss/svelte-scoped",
    "@unocss/eslint-plugin",
    "@unocss/eslint-config",
    "@unocss/reset",
];

define_plugin! {
    struct UnoCssPlugin => "unocss",
    enablers: ENABLERS,
    config_patterns: CONFIG_PATTERNS,
    always_used: ALWAYS_USED,
    tooling_dependencies: TOOLING_DEPENDENCIES,
    resolve_config: imports_only,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn resolve_config_extracts_preset_imports() {
        let source = r"
            import { defineConfig, presetUno, presetAttributify } from 'unocss';
            import presetIcons from '@unocss/preset-icons';
            export default defineConfig({
                presets: [presetUno(), presetAttributify(), presetIcons()],
            });
        ";
        let plugin = UnoCssPlugin;
        let result = plugin.resolve_config(
            std::path::Path::new("uno.config.ts"),
            source,
            std::path::Path::new("/project"),
        );
        let deps = &result.referenced_dependencies;
        assert!(deps.contains(&"unocss".to_string()));
        assert!(deps.contains(&"@unocss/preset-icons".to_string()));
    }

    #[test]
    fn resolve_config_extracts_transformer_imports() {
        let source = r"
            import { defineConfig } from 'unocss';
            import transformerDirectives from '@unocss/transformer-directives';
            import transformerVariantGroup from '@unocss/transformer-variant-group';
            export default defineConfig({
                transformers: [transformerDirectives(), transformerVariantGroup()],
            });
        ";
        let plugin = UnoCssPlugin;
        let result = plugin.resolve_config(
            std::path::Path::new("uno.config.ts"),
            source,
            std::path::Path::new("/project"),
        );
        let deps = &result.referenced_dependencies;
        assert!(deps.contains(&"@unocss/transformer-directives".to_string()));
        assert!(deps.contains(&"@unocss/transformer-variant-group".to_string()));
    }

    #[test]
    fn resolve_config_empty() {
        let source = r"
            import { defineConfig } from 'unocss';
            export default defineConfig({});
        ";
        let plugin = UnoCssPlugin;
        let result = plugin.resolve_config(
            std::path::Path::new("uno.config.ts"),
            source,
            std::path::Path::new("/project"),
        );
        assert_eq!(result.referenced_dependencies, vec!["unocss".to_string()]);
    }
}
