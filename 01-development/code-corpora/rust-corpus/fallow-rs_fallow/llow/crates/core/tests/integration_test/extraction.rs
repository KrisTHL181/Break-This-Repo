use std::path::Path;

use fallow_core::discover::FileId;
use fallow_core::extract::parse_from_content;

#[test]
fn dynamic_import_is_parsed() {
    let content = r"const mod = import('./lazy-module');";
    let info = parse_from_content(FileId(0), Path::new("test.ts"), content);

    assert_eq!(info.dynamic_imports.len(), 1);
    assert_eq!(info.dynamic_imports[0].source, "./lazy-module");
}

#[test]
fn cjs_interop_detects_require() {
    let content = r"const fs = require('fs'); const path = require('path');";
    let info = parse_from_content(FileId(0), Path::new("test.js"), content);

    assert_eq!(info.require_calls.len(), 2);
    assert_eq!(info.require_calls[0].source, "fs");
    assert_eq!(info.require_calls[1].source, "path");
}

#[test]
fn type_only_imports_are_marked() {
    let content = r"import type { Foo } from './types'; import { Bar } from './utils';";
    let info = parse_from_content(FileId(0), Path::new("test.ts"), content);

    assert_eq!(info.imports.len(), 2);
    assert!(info.imports[0].is_type_only);
    assert!(!info.imports[1].is_type_only);
}

#[test]
fn enum_members_are_extracted() {
    let content = r"export enum Color { Red = 'red', Green = 'green', Blue = 'blue' }";
    let info = parse_from_content(FileId(0), Path::new("test.ts"), content);

    assert_eq!(info.exports.len(), 1);
    assert_eq!(info.exports[0].members.len(), 3);
    assert_eq!(info.exports[0].members[0].name, "Red");
    assert_eq!(info.exports[0].members[1].name, "Green");
    assert_eq!(info.exports[0].members[2].name, "Blue");
}

#[test]
fn class_members_are_extracted() {
    let content = r"
export class MyService {
    name: string = '';
    async getUser(id: number) { return id; }
    static create() { return new MyService(); }
}
";
    let info = parse_from_content(FileId(0), Path::new("test.ts"), content);

    assert_eq!(info.exports.len(), 1);
    assert!(
        info.exports[0].members.len() >= 3,
        "Should have at least 3 members"
    );
}

#[test]
fn star_re_export_is_parsed() {
    let content = r"export * from './module';";
    let info = parse_from_content(FileId(0), Path::new("test.ts"), content);

    assert_eq!(info.re_exports.len(), 1);
    assert_eq!(info.re_exports[0].imported_name, "*");
    assert_eq!(info.re_exports[0].exported_name, "*");
    assert_eq!(info.re_exports[0].source, "./module");
}

#[test]
fn named_re_export_is_parsed() {
    let content = r"export { foo, bar as baz } from './module';";
    let info = parse_from_content(FileId(0), Path::new("test.ts"), content);

    assert_eq!(info.re_exports.len(), 2);
    assert_eq!(info.re_exports[0].imported_name, "foo");
    assert_eq!(info.re_exports[0].exported_name, "foo");
    assert_eq!(info.re_exports[1].imported_name, "bar");
    assert_eq!(info.re_exports[1].exported_name, "baz");
}

#[test]
fn namespace_import_marks_all_exports_used() {
    let content = r"import * as utils from './utils';";
    let info = parse_from_content(FileId(0), Path::new("test.ts"), content);

    assert_eq!(info.imports.len(), 1);
    assert_eq!(
        info.imports[0].imported_name,
        fallow_core::extract::ImportedName::Namespace
    );
}

#[test]
fn default_export_is_parsed() {
    let content = r"export default class MyComponent {}";
    let info = parse_from_content(FileId(0), Path::new("test.tsx"), content);

    assert_eq!(info.exports.len(), 1);
    assert_eq!(
        info.exports[0].name,
        fallow_core::extract::ExportName::Default
    );
}

#[test]
fn destructured_exports_are_parsed() {
    let content = r"export const { a, b } = { a: 1, b: 2 };";
    let info = parse_from_content(FileId(0), Path::new("test.ts"), content);

    assert_eq!(info.exports.len(), 2);
    assert_eq!(
        info.exports[0].name,
        fallow_core::extract::ExportName::Named("a".to_string())
    );
    assert_eq!(
        info.exports[1].name,
        fallow_core::extract::ExportName::Named("b".to_string())
    );
}

#[test]
fn side_effect_import_is_parsed() {
    let content = r"import './polyfills';";
    let info = parse_from_content(FileId(0), Path::new("test.ts"), content);

    assert_eq!(info.imports.len(), 1);
    assert_eq!(
        info.imports[0].imported_name,
        fallow_core::extract::ImportedName::SideEffect
    );
    assert_eq!(info.imports[0].source, "./polyfills");
}

#[test]
fn named_re_export_with_alias() {
    let content = r"export { default as MyComponent } from './Component';";
    let info = parse_from_content(FileId(0), Path::new("test.ts"), content);

    assert_eq!(info.re_exports.len(), 1);
    assert_eq!(info.re_exports[0].imported_name, "default");
    assert_eq!(info.re_exports[0].exported_name, "MyComponent");
}

#[test]
fn cjs_module_exports_assignment() {
    let content = r"module.exports = { foo: 1, bar: 2 };";
    let info = parse_from_content(FileId(0), Path::new("test.js"), content);

    assert!(info.has_cjs_exports);
}

#[test]
fn cjs_exports_dot_assignment() {
    let content = r"exports.foo = 42; exports.bar = 'hello';";
    let info = parse_from_content(FileId(0), Path::new("test.js"), content);

    assert!(info.has_cjs_exports);
    assert_eq!(info.exports.len(), 2);
}

#[test]
fn multiple_export_types_in_one_file() {
    let content = r"
export const VALUE = 42;
export function helper() {}
export type Config = { key: string };
export interface Logger { log(msg: string): void }
export enum Level { Debug, Info, Warn, Error }
export default class App {}
";
    let info = parse_from_content(FileId(0), Path::new("test.ts"), content);

    assert_eq!(
        info.exports.len(),
        6,
        "Expected 6 exports, got: {:?}",
        info.exports
            .iter()
            .map(|e| e.name.to_string())
            .collect::<Vec<_>>()
    );

    let level_export = info
        .exports
        .iter()
        .find(|e| e.name.to_string() == "Level")
        .unwrap();
    assert_eq!(level_export.members.len(), 4);
}

#[test]
fn extract_package_name_scoped() {
    use fallow_core::resolve::extract_package_name;

    assert_eq!(extract_package_name("react"), "react");
    assert_eq!(extract_package_name("react/jsx-runtime"), "react");
    assert_eq!(extract_package_name("@scope/pkg"), "@scope/pkg");
    assert_eq!(extract_package_name("@scope/pkg/utils"), "@scope/pkg");
    assert_eq!(extract_package_name("@types/node"), "@types/node");
}
