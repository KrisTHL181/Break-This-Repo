//! Validates that every `LazyLock<Regex>` in the crate compiles successfully.
//!
//! Regex patterns are lazily compiled on first use via `LazyLock`. If a pattern
//! is invalid (e.g. uses `(?i)` without the `unicode-case` feature), it panics
//! at runtime only when that specific file type is parsed. Unit tests that don't
//! exercise every file type parser will miss these failures.
//!
//! This module forces every code path to compile its regexes by parsing a minimal
//! file of each type.

use std::path::Path;

use fallow_types::discover::FileId;

use crate::parse::parse_source_to_module;

fn parse(filename: &str, source: &str) {
    parse_source_to_module(FileId(0), Path::new(filename), source, 0, false);
}

#[test]
fn all_html_regexes_compile() {
    parse(
        "index.html",
        r#"<html><head><link rel="stylesheet" href="style.css"></head><body><script src="app.js"></script></body></html>"#,
    );
}

#[test]
fn all_css_regexes_compile() {
    parse("style.module.css", ".foo { color: red; }");
}

#[test]
fn all_scss_regexes_compile() {
    parse(
        "style.module.scss",
        "@use 'vars';\n@import 'base';\n.foo { @apply text-red; }",
    );
}

#[test]
fn all_vue_sfc_regexes_compile() {
    parse(
        "App.vue",
        "<template><div>{{ msg }}</div></template>\n<script setup>\nconst msg = 'hi'\n</script>",
    );
}

#[test]
fn all_svelte_sfc_regexes_compile() {
    parse(
        "App.svelte",
        "<script>\nlet count = 0\n</script>\n<button>{count}</button>\n<style>.btn{}</style>",
    );
}

#[test]
fn all_astro_regexes_compile() {
    parse(
        "Page.astro",
        "---\nconst title = 'hi'\n---\n\
         <!-- comment -->\n\
         <script src=\"./client.ts\"></script>\n\
         <script>import './side-effect';</script>",
    );
}

#[test]
fn all_angular_template_regexes_compile() {
    parse(
        "app.component.html",
        r#"<div *ngFor="let item of items" [class]="cls" (click)="onClick()">{{ item }}</div>"#,
    );
}
