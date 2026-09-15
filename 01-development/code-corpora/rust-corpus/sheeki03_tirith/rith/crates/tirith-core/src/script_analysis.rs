use once_cell::sync::Lazy;
use regex::Regex;
use serde::Serialize;

/// Result of static script analysis.
#[derive(Debug, Clone, Serialize)]
pub struct ScriptAnalysis {
    pub domains_referenced: Vec<String>,
    pub paths_referenced: Vec<String>,
    /// True when the reference cap cut off further unique domains/paths
    /// (repo-0338): callers must not treat the lists as exhaustive.
    #[serde(default)]
    pub references_capped: bool,
    pub has_sudo: bool,
    pub has_eval: bool,
    pub has_base64: bool,
    pub has_curl_wget: bool,
    pub interpreter: String,
}

/// Hard cap on unique domains/paths collected from one script (repo-0338):
/// bounds both memory and the downstream consumers' work on a hostile
/// multi-MiB script.
const MAX_SCRIPT_REFERENCES: usize = 4096;

static DOMAIN_RE: Lazy<Regex> = Lazy::new(|| {
    Regex::new(r"(?:https?://)?([a-zA-Z0-9][-a-zA-Z0-9]*(?:\.[a-zA-Z0-9][-a-zA-Z0-9]*)+)").unwrap()
});

static PATH_RE: Lazy<Regex> = Lazy::new(|| {
    Regex::new(r"(?:/(?:usr|etc|var|tmp|opt|home|root|bin|sbin|lib|dev)(?:/[\w.-]+)+)").unwrap()
});

/// Perform static analysis on script content.
pub fn analyze(content: &str, interpreter: &str) -> ScriptAnalysis {
    // repo-0338: O(1) membership + a cardinality cap. The old Vec::contains
    // dedup was O(N^2) string comparisons on attacker-controlled scripts of
    // up to 10 MiB — hundreds of thousands of unique short matches could stall
    // `tirith run` before the confirmation prompt.
    let mut capped = false;
    let mut seen_domains = std::collections::HashSet::new();
    let mut domains = Vec::new();
    for cap in DOMAIN_RE.captures_iter(content) {
        if domains.len() >= MAX_SCRIPT_REFERENCES {
            capped = true;
            break;
        }
        if let Some(m) = cap.get(1) {
            let domain = m.as_str().to_string();
            if seen_domains.insert(domain.clone()) {
                domains.push(domain);
            }
        }
    }

    let mut seen_paths = std::collections::HashSet::new();
    let mut paths = Vec::new();
    for mat in PATH_RE.find_iter(content) {
        if paths.len() >= MAX_SCRIPT_REFERENCES {
            capped = true;
            break;
        }
        let path = mat.as_str().to_string();
        if seen_paths.insert(path.clone()) {
            paths.push(path);
        }
    }

    ScriptAnalysis {
        domains_referenced: domains,
        paths_referenced: paths,
        references_capped: capped,
        has_sudo: content.contains("sudo "),
        has_eval: content.contains("eval ") || content.contains("eval("),
        has_base64: content.contains("base64"),
        has_curl_wget: content.contains("curl ")
            || content.contains("wget ")
            || content.contains("http ")
            || content.contains("https ")
            || content.contains("xh "),
        interpreter: interpreter.to_string(),
    }
}

/// Detect interpreter from shebang line.
pub fn detect_interpreter(content: &str) -> &str {
    if let Some(first_line) = content.lines().next() {
        let first_line = first_line.trim();
        if first_line.starts_with("#!") {
            let shebang = first_line.trim_start_matches("#!");
            let parts: Vec<&str> = shebang.split_whitespace().collect();
            if let Some(prog) = parts.first() {
                let base = prog.rsplit('/').next().unwrap_or(prog);
                if base == "env" {
                    // Walk past env flags (-S, -i, …) and VAR=val assignments to the interpreter name.
                    for part in parts.iter().skip(1) {
                        if part.starts_with('-') || part.contains('=') {
                            continue;
                        }
                        return part;
                    }
                } else {
                    return base;
                }
            }
        }
    }
    "sh"
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_detect_interpreter_env_s() {
        let content = "#!/usr/bin/env -S python3 -u\nprint('hello')";
        assert_eq!(detect_interpreter(content), "python3");
    }

    #[test]
    fn test_detect_interpreter_env_s_with_var() {
        let content = "#!/usr/bin/env -S VAR=1 python3\nprint('hello')";
        assert_eq!(detect_interpreter(content), "python3");
    }

    #[test]
    fn test_detect_interpreter_crlf() {
        let content = "#!/bin/bash\r\necho hello";
        assert_eq!(detect_interpreter(content), "bash");
    }

    #[test]
    fn test_detect_interpreter_basic() {
        let content = "#!/usr/bin/env python3\nprint('hello')";
        assert_eq!(detect_interpreter(content), "python3");
    }

    #[test]
    fn test_detect_interpreter_no_shebang() {
        let content = "echo hello";
        assert_eq!(detect_interpreter(content), "sh");
    }
}
