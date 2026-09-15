//! Architectural lint: any `Some(<float-literal>)` pattern inside a policy's
//! `activation` function body must carry an adjacent `// activation-constant:`
//! marker on the same or prior line. Composed expressions
//! (`Some(features.commitment as f32)`, `Some(arch * turn)`, etc.) are
//! exempt — the marker is required only for hard-coded literals so reviewers
//! can audit the rationale.

use std::fs;
use std::path::Path;

const MARKER: &str = "// activation-constant:";
/// Skip these helper modules — they're not policy implementations.
const SKIP_FILES: &[&str] = &[
    "mod.rs",
    "context.rs",
    "registry.rs",
    "strategy_helpers.rs",
    "effect_classify.rs",
    "activation.rs",
];

#[test]
fn activation_constants_carry_marker() {
    let policies_root = Path::new(concat!(env!("CARGO_MANIFEST_DIR"), "/src/policies"));
    let mut violations: Vec<String> = Vec::new();

    let entries = fs::read_dir(policies_root).expect("policies dir");
    for entry in entries.flatten() {
        let path = entry.path();
        if !path.is_file() || path.extension().and_then(|e| e.to_str()) != Some("rs") {
            continue;
        }
        let file_name = path
            .file_name()
            .and_then(|s| s.to_str())
            .unwrap_or_default();
        if SKIP_FILES.contains(&file_name) {
            continue;
        }
        let Ok(contents) = fs::read_to_string(&path) else {
            continue;
        };
        check_file(&path, &contents, &mut violations);
    }

    assert!(
        violations.is_empty(),
        "activation-constant marker missing for literal Some(...) patterns:\n{}",
        violations.join("\n")
    );
}

fn check_file(path: &Path, contents: &str, violations: &mut Vec<String>) {
    let lines: Vec<&str> = contents.lines().collect();
    let mut i = 0;
    while i < lines.len() {
        if !is_activation_signature(lines[i]) {
            i += 1;
            continue;
        }
        let (start, end) = function_body_range(&lines, i);
        for body_idx in start..end {
            let line = lines[body_idx];
            // Strip line comments before scanning for literal patterns.
            let code = line.split("//").next().unwrap_or("");
            if let Some(span) = find_some_float_literal(code) {
                let same_line_marker = line.contains(MARKER);
                let prior_marker = body_idx
                    .checked_sub(1)
                    .map(|prev| lines[prev].contains(MARKER))
                    .unwrap_or(false);
                if !same_line_marker && !prior_marker {
                    violations.push(format!(
                        "{}:{}: literal `Some(<float>)` (`{}`) without `{MARKER}` marker",
                        path.display(),
                        body_idx + 1,
                        span
                    ));
                }
            }
        }
        i = end;
    }
}

fn is_activation_signature(line: &str) -> bool {
    let trimmed = line.trim_start();
    trimmed.starts_with("fn activation(")
        || trimmed.starts_with("pub fn activation(")
        || trimmed.starts_with("fn activation (")
        || trimmed.starts_with("pub fn activation (")
}

/// Find a `Some(<float-literal>)` substring like `Some(1.0)` or `Some(-0.5_f32)`.
/// Returns the matched substring on first hit.
fn find_some_float_literal(code: &str) -> Option<String> {
    let bytes = code.as_bytes();
    let needle = b"Some(";
    let mut i = 0;
    while i + needle.len() < bytes.len() {
        if &bytes[i..i + needle.len()] == needle {
            // Scan inside the parens up to matching ')'
            let mut depth = 1;
            let mut j = i + needle.len();
            let inner_start = j;
            while j < bytes.len() {
                match bytes[j] {
                    b'(' => depth += 1,
                    b')' => {
                        depth -= 1;
                        if depth == 0 {
                            break;
                        }
                    }
                    _ => {}
                }
                j += 1;
            }
            if j >= bytes.len() {
                return None;
            }
            let inner = code[inner_start..j].trim();
            if is_float_literal(inner) {
                return Some(format!("Some({inner})"));
            }
            i = j + 1;
            continue;
        }
        i += 1;
    }
    None
}

fn is_float_literal(s: &str) -> bool {
    let s = s.trim_end_matches("_f32").trim_end_matches("f32");
    let s = s.trim();
    if s.is_empty() {
        return false;
    }
    let s = s.strip_prefix('-').unwrap_or(s);
    let mut saw_dot = false;
    let mut saw_digit = false;
    for ch in s.chars() {
        if ch.is_ascii_digit() {
            saw_digit = true;
        } else if ch == '.' && !saw_dot {
            saw_dot = true;
        } else if ch == '_' {
            // numeric separator
        } else {
            return false;
        }
    }
    saw_dot && saw_digit
}

/// Return (body_start_line_idx_exclusive_of_signature, body_end_line_idx_exclusive)
fn function_body_range(lines: &[&str], signature_line: usize) -> (usize, usize) {
    let mut depth = 0i32;
    let mut started = false;
    let mut start = signature_line;
    for (idx, line) in lines.iter().enumerate().skip(signature_line) {
        for ch in line.chars() {
            if ch == '{' {
                if !started {
                    started = true;
                    start = idx + 1;
                }
                depth += 1;
            } else if ch == '}' {
                depth -= 1;
                if started && depth == 0 {
                    return (start, idx);
                }
            }
        }
    }
    (start, lines.len())
}

#[cfg(test)]
mod self_check {
    use super::*;

    #[test]
    fn detects_literal() {
        let code = "        Some(1.0)";
        assert!(find_some_float_literal(code).is_some());
    }

    #[test]
    fn rejects_expression() {
        let code = "        Some(arch * turn)";
        assert!(find_some_float_literal(code).is_none());
    }

    #[test]
    fn rejects_cast_expression() {
        let code = "        Some(value as f32)";
        assert!(find_some_float_literal(code).is_none());
    }
}
