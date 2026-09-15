//! Qwik framework plugin.
//!
//! Detects Qwik projects and marks route files, entry points, and components
//! as entry points. Recognizes conventional route module exports.

use super::Plugin;

const ENABLERS: &[&str] = &[
    "@builder.io/qwik",
    "@builder.io/qwik-city",
    "@qwik.dev/core",
    "@qwik.dev/router",
];

const ENTRY_PATTERNS: &[&str] = &[
    "src/routes/**/index.{ts,tsx,js,jsx,mdx}",
    "src/routes/**/layout.{ts,tsx,js,jsx}",
    "src/routes/**/layout!.{ts,tsx,js,jsx}",
    "src/routes/**/plugin.{ts,js}",
    "src/routes/**/plugin@*.{ts,js}",
    "src/routes/**/service-worker.{ts,js}",
    "src/root.{ts,tsx,js,jsx}",
    "src/entry.*.{ts,tsx,js,jsx}",
    "src/global.{css,scss,less}",
    "adapters/**/vite.config.{ts,js}",
];

const ALWAYS_USED: &[&str] = &["src/entry.*.{ts,tsx,js,jsx}", "src/root.{ts,tsx,js,jsx}"];

const TOOLING_DEPENDENCIES: &[&str] = &[
    "@builder.io/qwik",
    "@builder.io/qwik-city",
    "@qwik.dev/core",
    "@qwik.dev/router",
    "@builder.io/qwik-react",
    "@qwik.dev/react",
];

const ROUTE_EXPORTS: &[&str] = &[
    "default",
    "head",
    "onGet",
    "onPost",
    "onPut",
    "onDelete",
    "onPatch",
    "onHead",
    "onRequest",
];

const LAYOUT_EXPORTS: &[&str] = &["default", "head"];

define_plugin! {
    struct QwikPlugin => "qwik",
    enablers: ENABLERS,
    entry_patterns: ENTRY_PATTERNS,
    always_used: ALWAYS_USED,
    tooling_dependencies: TOOLING_DEPENDENCIES,
    used_exports: [
        ("src/routes/**/index.{ts,tsx,js,jsx,mdx}", ROUTE_EXPORTS),
        ("src/routes/**/layout.{ts,tsx,js,jsx}", LAYOUT_EXPORTS),
        ("src/routes/**/layout!.{ts,tsx,js,jsx}", LAYOUT_EXPORTS),
    ],
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn used_exports_cover_route_handlers() {
        let plugin = QwikPlugin;
        let exports = plugin.used_exports();

        assert!(exports.iter().any(|(pattern, names)| {
            pattern == &"src/routes/**/index.{ts,tsx,js,jsx,mdx}"
                && names.contains(&"default")
                && names.contains(&"onGet")
                && names.contains(&"onPost")
                && names.contains(&"head")
        }));
    }

    #[test]
    fn used_exports_cover_layouts() {
        let plugin = QwikPlugin;
        let exports = plugin.used_exports();

        assert!(exports.iter().any(|(pattern, names)| {
            pattern == &"src/routes/**/layout.{ts,tsx,js,jsx}"
                && names.contains(&"default")
                && names.contains(&"head")
        }));
    }

    #[test]
    fn used_exports_cover_reset_layouts() {
        let plugin = QwikPlugin;
        let exports = plugin.used_exports();

        assert!(exports.iter().any(|(pattern, names)| {
            pattern == &"src/routes/**/layout!.{ts,tsx,js,jsx}"
                && names.contains(&"default")
                && names.contains(&"head")
        }));
    }

    #[test]
    fn entry_patterns_cover_all_route_conventions() {
        let plugin = QwikPlugin;
        let patterns = plugin.entry_patterns();

        assert!(patterns.contains(&"src/routes/**/index.{ts,tsx,js,jsx,mdx}"));
        assert!(patterns.contains(&"src/routes/**/layout.{ts,tsx,js,jsx}"));
        assert!(patterns.contains(&"src/routes/**/layout!.{ts,tsx,js,jsx}"));
        assert!(patterns.contains(&"src/routes/**/plugin.{ts,js}"));
        assert!(patterns.contains(&"src/routes/**/plugin@*.{ts,js}"));
        assert!(patterns.contains(&"src/routes/**/service-worker.{ts,js}"));
        assert!(patterns.contains(&"src/root.{ts,tsx,js,jsx}"));
        assert!(patterns.contains(&"src/entry.*.{ts,tsx,js,jsx}"));
    }

    #[test]
    fn enablers_cover_both_package_scopes() {
        let plugin = QwikPlugin;
        let deps_v1 = vec!["@builder.io/qwik".to_string()];
        let deps_v2 = vec!["@qwik.dev/core".to_string()];
        let deps_city = vec!["@builder.io/qwik-city".to_string()];
        let deps_router = vec!["@qwik.dev/router".to_string()];
        let deps_none = vec!["react".to_string()];

        assert!(plugin.is_enabled_with_deps(&deps_v1, std::path::Path::new("/p")));
        assert!(plugin.is_enabled_with_deps(&deps_v2, std::path::Path::new("/p")));
        assert!(plugin.is_enabled_with_deps(&deps_city, std::path::Path::new("/p")));
        assert!(plugin.is_enabled_with_deps(&deps_router, std::path::Path::new("/p")));
        assert!(!plugin.is_enabled_with_deps(&deps_none, std::path::Path::new("/p")));
    }
}
