//! Astro framework plugin.
//!
//! Detects Astro projects and marks pages, layouts, content, and middleware
//! as entry points. Parses astro.config to extract referenced dependencies.

use super::{Plugin, PluginResult};

const ENABLERS: &[&str] = &["astro"];

const ENTRY_PATTERNS: &[&str] = &[
    "src/pages/**/*.{astro,ts,tsx,js,jsx,mts,mjs,cts,cjs,md,mdx}",
    "src/layouts/**/*.astro",
    "src/content/**/*.{ts,js,mts,mjs,cts,cjs,md,mdx}",
    "src/middleware.{js,ts,mjs,mts,cjs,cts}",
    "src/middleware/index.{js,ts,mjs,mts,cjs,cts}",
    "src/actions/index.{js,ts,mjs,mts,cjs,cts}",
];

const CONFIG_PATTERNS: &[&str] = &["astro.config.{ts,js,mjs}"];

const ALWAYS_USED: &[&str] = &[
    "astro.config.{ts,js,mjs}",
    "src/content/config.{js,ts,mjs,mts,cjs,cts}",
    "src/content.config.{js,ts,mjs,mts,cjs,cts}",
];

const TOOLING_DEPENDENCIES: &[&str] = &["astro", "@astrojs/check", "@astrojs/ts-plugin"];

/// Virtual module prefixes provided by Astro at build time.
/// `astro:` provides built-in modules (content, transitions, env, actions, assets,
/// i18n, middleware, container, schema).
const VIRTUAL_MODULE_PREFIXES: &[&str] = &["astro:"];

const PAGE_EXPORTS: &[&str] = &["getStaticPaths", "prerender", "partial"];
const COMPONENT_PAGE_EXPORTS: &[&str] = &["default", "getStaticPaths", "prerender", "partial"];
const ENDPOINT_EXPORTS: &[&str] = &[
    "GET",
    "POST",
    "PUT",
    "PATCH",
    "DELETE",
    "HEAD",
    "OPTIONS",
    "ALL",
    "getStaticPaths",
    "prerender",
];
const MIDDLEWARE_EXPORTS: &[&str] = &["onRequest"];
const CONTENT_EXPORTS: &[&str] = &["collections"];
const ACTION_EXPORTS: &[&str] = &["server"];

define_plugin! {
    struct AstroPlugin => "astro",
    enablers: ENABLERS,
    entry_patterns: ENTRY_PATTERNS,
    config_patterns: CONFIG_PATTERNS,
    always_used: ALWAYS_USED,
    tooling_dependencies: TOOLING_DEPENDENCIES,
    virtual_module_prefixes: VIRTUAL_MODULE_PREFIXES,
    used_exports: [
        ("src/pages/**/*.{astro,md,mdx}", PAGE_EXPORTS),
        ("src/pages/**/*.{tsx,jsx}", COMPONENT_PAGE_EXPORTS),
        ("src/pages/**/*.{ts,js,mts,mjs,cts,cjs}", ENDPOINT_EXPORTS),
        ("src/middleware.{js,ts,mjs,mts,cjs,cts}", MIDDLEWARE_EXPORTS),
        ("src/middleware/index.{js,ts,mjs,mts,cjs,cts}", MIDDLEWARE_EXPORTS),
        (
            "src/content/config.{js,ts,mjs,mts,cjs,cts}",
            CONTENT_EXPORTS
        ),
        (
            "src/content.config.{js,ts,mjs,mts,cjs,cts}",
            CONTENT_EXPORTS
        ),
        ("src/actions/index.{js,ts,mjs,mts,cjs,cts}", ACTION_EXPORTS),
    ],
    resolve_config: imports_only,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn virtual_module_prefixes_includes_astro_builtins() {
        let plugin = AstroPlugin;
        let prefixes = plugin.virtual_module_prefixes();
        assert!(prefixes.contains(&"astro:"));
    }

    #[test]
    fn used_exports_cover_current_astro_conventions() {
        let plugin = AstroPlugin;
        let exports = plugin.used_exports();

        assert!(exports.iter().any(|(pattern, names)| {
            pattern.contains("src/actions/index") && names.contains(&"server")
        }));
        assert!(exports.iter().any(|(pattern, names)| {
            pattern.contains("src/content/config") && names.contains(&"collections")
        }));
        assert!(exports.iter().any(|(pattern, names)| {
            pattern.contains("src/middleware/index") && names.contains(&"onRequest")
        }));
        assert!(exports.iter().any(|(pattern, names)| {
            pattern.contains("src/pages") && names.contains(&"getStaticPaths")
        }));
    }
}
