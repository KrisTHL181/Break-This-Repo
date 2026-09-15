//! Expo framework plugin.
//!
//! Detects Expo projects and marks app entry points and config files.

use super::Plugin;

const ENABLERS: &[&str] = &["expo"];

const ENTRY_PATTERNS: &[&str] = &[
    "App.{ts,tsx,js,jsx}",
    "app/**/*.{ts,tsx,js,jsx}",
    "src/App.{ts,tsx,js,jsx}",
];

const ALWAYS_USED: &[&str] = &[
    "app.json",
    "app.config.{ts,js,mjs,cjs}",
    "metro.config.{ts,js,mjs,cjs}",
    "babel.config.{ts,js,mjs,cjs}",
];

const TOOLING_DEPENDENCIES: &[&str] = &["expo", "expo-cli", "@expo/webpack-config"];

pub struct ExpoPlugin;

impl Plugin for ExpoPlugin {
    fn name(&self) -> &'static str {
        "expo"
    }

    fn enablers(&self) -> &'static [&'static str] {
        ENABLERS
    }

    fn is_enabled_with_deps(&self, deps: &[String], _root: &std::path::Path) -> bool {
        deps.iter().any(|dep| dep == "expo") && !deps.iter().any(|dep| dep == "expo-router")
    }

    fn entry_patterns(&self) -> &'static [&'static str] {
        ENTRY_PATTERNS
    }

    fn always_used(&self) -> &'static [&'static str] {
        ALWAYS_USED
    }

    fn tooling_dependencies(&self) -> &'static [&'static str] {
        TOOLING_DEPENDENCIES
    }
}
