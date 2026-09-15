//! Project-level analysis tools: project_summary and file_tree.

use anyhow::Result;
use std::path::Path;

pub(super) fn exec_project_summary(path: &Path) -> Result<String> {
    let mut out = String::new();
    let dir_name = path
        .file_name()
        .map(|n| n.to_string_lossy().into_owned())
        .unwrap_or_else(|| path.to_string_lossy().into_owned());
    out.push_str(&format!("Project: {}\n", dir_name));

    if path.join(".git").exists() {
        out.push_str("VCS: git\n");
    }

    struct Marker {
        file: &'static str,
        lang: &'static str,
        build: &'static str,
    }
    let markers = [
        Marker {
            file: "Cargo.toml",
            lang: "Rust",
            build: "cargo",
        },
        Marker {
            file: "package.json",
            lang: "JavaScript/TypeScript",
            build: "npm/pnpm",
        },
        Marker {
            file: "go.mod",
            lang: "Go",
            build: "go",
        },
        Marker {
            file: "pyproject.toml",
            lang: "Python",
            build: "pip/poetry",
        },
        Marker {
            file: "setup.py",
            lang: "Python",
            build: "setuptools",
        },
        Marker {
            file: "Gemfile",
            lang: "Ruby",
            build: "bundler",
        },
        Marker {
            file: "pom.xml",
            lang: "Java",
            build: "Maven",
        },
        Marker {
            file: "build.gradle",
            lang: "Java/Kotlin",
            build: "Gradle",
        },
        Marker {
            file: "CMakeLists.txt",
            lang: "C/C++",
            build: "CMake",
        },
        Marker {
            file: "Makefile",
            lang: "",
            build: "make",
        },
        Marker {
            file: "Package.swift",
            lang: "Swift",
            build: "SwiftPM",
        },
    ];

    let mut langs: Vec<&str> = Vec::new();
    let mut builds: Vec<&str> = Vec::new();
    for m in &markers {
        if path.join(m.file).exists() {
            if !m.lang.is_empty() && !langs.contains(&m.lang) {
                langs.push(m.lang);
            }
            if !builds.contains(&m.build) {
                builds.push(m.build);
            }
        }
    }

    if !langs.is_empty() {
        out.push_str(&format!("Languages: {}\n", langs.join(", ")));
    }
    if !builds.is_empty() {
        out.push_str(&format!("Build system: {}\n", builds.join(", ")));
    }

    if let Ok(cargo) = std::fs::read_to_string(path.join("Cargo.toml")) {
        for line in cargo.lines().take(20) {
            let trimmed = line.trim();
            if trimmed.starts_with("name")
                || trimmed.starts_with("version")
                || trimmed.starts_with("description")
            {
                out.push_str(&format!("  {}\n", trimmed));
            }
        }
        if let Some(idx) = cargo.find("[workspace]") {
            for line in cargo[idx..].lines().skip(1).take(20) {
                let t = line.trim();
                if t.starts_with('[') && t != "[workspace]" {
                    break;
                }
                if t.starts_with('"') || t.starts_with("members") {
                    out.push_str(&format!("  {}\n", t));
                }
            }
        }
    }
    if let Ok(pkg) = std::fs::read_to_string(path.join("package.json")) {
        if let Ok(json) = serde_json::from_str::<serde_json::Value>(&pkg) {
            if let Some(name) = json["name"].as_str() {
                out.push_str(&format!("  name: {}\n", name));
            }
            if let Some(ver) = json["version"].as_str() {
                out.push_str(&format!("  version: {}\n", ver));
            }
        }
    }

    let key_dirs: Vec<String> = std::fs::read_dir(path)
        .ok()
        .into_iter()
        .flat_map(|rd| rd.filter_map(|e| e.ok()))
        .filter(|e| e.file_type().map(|t| t.is_dir()).unwrap_or(false))
        .map(|e| e.file_name().to_string_lossy().into_owned())
        .filter(|n| {
            !n.starts_with('.') && n != "node_modules" && n != "target" && n != "__pycache__"
        })
        .collect();
    if !key_dirs.is_empty() {
        let mut sorted = key_dirs;
        sorted.sort();
        out.push_str(&format!("Directories: {}\n", sorted.join(", ")));
    }

    let entry_candidates = [
        "src/main.rs",
        "src/lib.rs",
        "src/index.ts",
        "src/index.js",
        "main.go",
        "main.py",
        "app.py",
        "index.js",
        "index.ts",
    ];
    let mut entries: Vec<&str> = Vec::new();
    for e in &entry_candidates {
        if path.join(e).exists() {
            entries.push(e);
        }
    }
    if !entries.is_empty() {
        out.push_str(&format!("Entry points: {}\n", entries.join(", ")));
    }

    Ok(out)
}

pub(super) fn exec_file_tree(root: &Path, max_depth: usize) -> Result<String> {
    const SKIP_DIRS: &[&str] = &[
        ".git",
        "node_modules",
        "target",
        "__pycache__",
        ".next",
        "dist",
        "build",
        ".build",
        ".cache",
        "vendor",
        ".bundle",
        "venv",
        ".venv",
        "Pods",
        "DerivedData",
    ];
    const MAX_ENTRIES: usize = 500;

    let mut out = String::new();
    let mut count = 0usize;

    fn walk(
        dir: &Path,
        prefix: &str,
        depth: usize,
        max_depth: usize,
        skip: &[&str],
        out: &mut String,
        count: &mut usize,
    ) {
        if depth > max_depth || *count >= MAX_ENTRIES {
            return;
        }
        let mut entries: Vec<(String, bool)> = match std::fs::read_dir(dir) {
            Ok(rd) => rd
                .filter_map(|e| e.ok())
                .map(|e| {
                    let name = e.file_name().to_string_lossy().into_owned();
                    let is_dir = e.file_type().map(|t| t.is_dir()).unwrap_or(false);
                    (name, is_dir)
                })
                .filter(|(name, _)| !name.starts_with('.') || name == ".github")
                .collect(),
            Err(_) => return,
        };
        entries.sort_by(|a, b| match (a.1, b.1) {
            (true, false) => std::cmp::Ordering::Less,
            (false, true) => std::cmp::Ordering::Greater,
            _ => a.0.cmp(&b.0),
        });

        for (name, is_dir) in &entries {
            if *count >= MAX_ENTRIES {
                out.push_str(&format!("{}... (truncated)\n", prefix));
                return;
            }
            *count += 1;
            if *is_dir {
                out.push_str(&format!("{}{}/\n", prefix, name));
                if !skip.contains(&name.as_str()) {
                    walk(
                        &dir.join(name),
                        &format!("{}  ", prefix),
                        depth + 1,
                        max_depth,
                        skip,
                        out,
                        count,
                    );
                }
            } else {
                out.push_str(&format!("{}{}\n", prefix, name));
            }
        }
    }

    let root_name = root
        .file_name()
        .map(|n| n.to_string_lossy().into_owned())
        .unwrap_or_else(|| root.to_string_lossy().into_owned());
    out.push_str(&format!("{}/\n", root_name));
    walk(root, "  ", 1, max_depth, SKIP_DIRS, &mut out, &mut count);
    if count >= MAX_ENTRIES {
        out.push_str(&format!("[truncated at {} entries]\n", MAX_ENTRIES));
    }
    Ok(out)
}
