//! Path-resolution and sensitive-path guards used by every fs / search tool.
//!
//! Lives in its own submodule because it is pure, has no AI / LLM coupling,
//! and is the natural first slice of the long-term `ai_tools/` split (see
//! `kaku-gui/AGENTS.md`). Keeping it isolated also makes the security check
//! easy to audit independently of the dispatcher in `mod.rs`.

use anyhow::{Context, Result};
use std::path::{Path, PathBuf};

/// Resolve the user's home directory from the environment.
///
/// Single source of truth for `$HOME` lookups in this crate so the failure
/// mode (panic / silent-empty / Result) does not drift between call sites.
pub(crate) fn home() -> Result<PathBuf> {
    let h = std::env::var_os("HOME").context("HOME not set")?;
    Ok(PathBuf::from(h))
}

/// Expand a leading `~`, `~/`, `$HOME`, `$HOME/`, `${HOME}`, or `${HOME}/`
/// in `s` to the user's home directory. Returns `None` when `s` carries
/// none of those forms (caller decides the fallback) or when `$HOME` is
/// unset.
pub(crate) fn expand_user_prefix(s: &str) -> Option<PathBuf> {
    let home = home().ok()?;
    if s == "~" || s == "$HOME" || s == "${HOME}" {
        return Some(home);
    }
    for prefix in ["~/", "$HOME/", "${HOME}/"] {
        if let Some(rest) = s.strip_prefix(prefix) {
            return Some(home.join(rest));
        }
    }
    None
}

/// Refuse reads of well-known credential / system-secret locations, even when
/// the caller passes an absolute or `~/`-prefixed path (both of which bypass
/// the cwd sandbox). Best-effort canonicalization: on ENOENT we compare the
/// raw path so a file about to be created in a blocked directory is still
/// caught.
pub(crate) fn reject_if_sensitive(path: &Path) -> Result<()> {
    let canon = std::fs::canonicalize(path).unwrap_or_else(|_| path.to_path_buf());
    for b in blocked_locations() {
        let b_canon = std::fs::canonicalize(&b).unwrap_or_else(|_| b.clone());
        if canon == b_canon || canon.starts_with(&b_canon) {
            anyhow::bail!(
                "refused: '{}' is a protected secret location",
                path.display()
            );
        }
    }

    // Name-pattern guard: credential files live anywhere, not just in the
    // fixed locations above. Check both the requested name and the
    // canonicalized name so a symlink cannot launder a `.env` past the guard.
    for candidate in [path, canon.as_path()] {
        if candidate.components().any(|component| {
            component
                .as_os_str()
                .to_str()
                .is_some_and(is_sensitive_directory_name)
        }) {
            anyhow::bail!(
                "refused: '{}' is inside a credential directory",
                path.display()
            );
        }
        if let Some(name) = candidate.file_name().and_then(|n| n.to_str()) {
            if is_sensitive_filename(name) {
                anyhow::bail!("refused: '{}' looks like a credential file", path.display());
            }
        }
    }
    Ok(())
}

fn blocked_locations() -> Vec<PathBuf> {
    let mut blocked: Vec<PathBuf> = vec![
        PathBuf::from("/etc/shadow"),
        PathBuf::from("/etc/sudoers"),
        PathBuf::from("/etc/sudoers.d"),
        PathBuf::from("/private/etc/shadow"),
        PathBuf::from("/private/etc/sudoers"),
        PathBuf::from("/private/etc/sudoers.d"),
    ];
    if let Ok(home) = home() {
        for rel in [
            ".ssh",
            ".aws/credentials",
            ".gnupg",
            ".config/gh",
            ".config/kaku/assistant.toml",
            ".config/kaku/secrets",
            ".docker/config.json",
            ".git-credentials",
            ".netrc",
            ".npmrc",
            ".pypirc",
        ] {
            blocked.push(home.join(rel));
        }
    }
    if let Some(config_dir) = config::user_config_path().parent() {
        blocked.push(config_dir.join("assistant.toml"));
        blocked.push(config_dir.join("secrets"));
    }
    blocked
}

/// File-name patterns that commonly hold secrets regardless of directory
/// (`.env`, private keys, PEM material). Obvious example/template files are
/// allowed because they are committed to repos on purpose and carry no
/// secrets.
fn is_sensitive_filename(name: &str) -> bool {
    let lower = name.to_ascii_lowercase();

    // `.env.example`, `config.pem.sample`, `secrets.key.template`, ... are
    // placeholders, not real credentials.
    const ALLOW_SUFFIXES: [&str; 4] = [".example", ".sample", ".template", ".dist"];
    if ALLOW_SUFFIXES.iter().any(|s| lower.ends_with(s)) {
        return false;
    }

    // Dotenv files: `.env`, `.env.local`, `.env.production`, ...
    if lower == ".env" || lower.starts_with(".env.") {
        return true;
    }

    // Private key material by extension.
    if lower.ends_with(".pem") || lower.ends_with(".key") {
        return true;
    }

    // Credential stores: block data/config shapes ("credentials",
    // "aws_credentials.json", ...) but keep source code and docs that merely
    // have "credentials" in the name (credentials.rs, credentials_test.go,
    // docs/credentials.md) readable.
    if lower.contains("credentials") && !has_source_or_doc_extension(&lower) {
        return true;
    }

    if matches!(
        lower.as_str(),
        ".git-credentials" | ".netrc" | ".npmrc" | ".pypirc" | "assistant.toml"
    ) {
        return true;
    }

    // Well-known SSH private key file names. Public `.pub` siblings do not
    // match the exact names, so they stay readable.
    matches!(
        lower.as_str(),
        "id_rsa" | "id_dsa" | "id_ecdsa" | "id_ed25519"
    )
}

/// Extensions that indicate source code or documentation rather than a
/// credential data file. Data/config extensions (json, toml, yml, ini, csv,
/// txt, no extension, ...) deliberately stay outside this list so they remain
/// blocked when the name mentions credentials.
fn has_source_or_doc_extension(lower_name: &str) -> bool {
    let Some((_, ext)) = lower_name.rsplit_once('.') else {
        return false;
    };
    matches!(
        ext,
        "rs" | "go"
            | "py"
            | "ts"
            | "tsx"
            | "js"
            | "jsx"
            | "mjs"
            | "cjs"
            | "java"
            | "kt"
            | "swift"
            | "c"
            | "h"
            | "cpp"
            | "hpp"
            | "cc"
            | "cs"
            | "rb"
            | "php"
            | "ex"
            | "exs"
            | "erl"
            | "hs"
            | "ml"
            | "scala"
            | "lua"
            | "md"
            | "mdx"
            | "rst"
            | "html"
            | "css"
            | "scss"
            | "vue"
            | "svelte"
            | "sql"
            | "proto"
    )
}

/// Credential-named source/doc files (`credentials.py`, `credentials.md`)
/// are readable, but only behind a per-file approval prompt: they often hold
/// real secrets in source form, so a prompt-injected read must not be silent.
pub(crate) fn is_credential_named_source_file(name: &str) -> bool {
    let lower = name.to_ascii_lowercase();
    lower.contains("credentials") && has_source_or_doc_extension(&lower)
}

/// Credential-named source and documentation files are allowed by the hard
/// path guard, but reading them must be visible to the user. Check both the
/// requested path and its canonical target so a harmless-looking symlink
/// cannot turn an approved-path decision into a different read at execution.
pub(crate) fn requires_read_approval(path: &Path) -> bool {
    let canonical = std::fs::canonicalize(path).unwrap_or_else(|_| path.to_path_buf());
    [path, canonical.as_path()].iter().any(|candidate| {
        candidate
            .file_name()
            .and_then(|name| name.to_str())
            .is_some_and(is_credential_named_source_file)
    })
}

fn is_sensitive_directory_name(name: &str) -> bool {
    matches!(
        name.to_ascii_lowercase().as_str(),
        ".ssh" | ".gnupg" | "secrets"
    )
}

/// Ripgrep exclusions for sensitive descendants of an otherwise-readable
/// search root. Root validation alone is insufficient for recursive search.
pub(crate) fn sensitive_search_globs(root: &Path) -> Vec<String> {
    let root = std::fs::canonicalize(root).unwrap_or_else(|_| root.to_path_buf());
    let mut globs = vec![
        "!**/.env".to_string(),
        "!**/.env.*".to_string(),
        "!**/*.pem".to_string(),
        "!**/*.key".to_string(),
        "!**/id_rsa".to_string(),
        "!**/id_dsa".to_string(),
        "!**/id_ecdsa".to_string(),
        "!**/id_ed25519".to_string(),
        "!**/.git-credentials".to_string(),
        "!**/.netrc".to_string(),
        "!**/.npmrc".to_string(),
        "!**/.pypirc".to_string(),
        "!**/assistant.toml".to_string(),
        "!**/*credentials*".to_string(),
        "!**/.ssh/**".to_string(),
        "!**/.gnupg/**".to_string(),
        "!**/secrets/**".to_string(),
    ];
    for blocked in blocked_locations() {
        let blocked = std::fs::canonicalize(&blocked).unwrap_or(blocked);
        let Ok(relative) = blocked.strip_prefix(&root) else {
            continue;
        };
        if relative.as_os_str().is_empty() {
            continue;
        }
        let relative = relative.to_string_lossy().replace('\\', "/");
        globs.push(format!("!{relative}"));
        globs.push(format!("!{relative}/**"));
    }
    globs
}

/// Handles `~/…` expansion and relative paths (resolved against `cwd`).
///
/// Tool paths only accept `~` / `~/`; `$HOME` is not expanded here because
/// AI models do not pass shell-variable references through this path,
/// and silently expanding them would change tool-call semantics.
pub(crate) fn resolve(path: &str, cwd: &str) -> Result<PathBuf> {
    if path == "~" || path.starts_with("~/") {
        let home = home()?;
        return Ok(if path == "~" {
            home
        } else {
            home.join(&path[2..])
        });
    }
    if path.starts_with('/') {
        Ok(PathBuf::from(path))
    } else {
        Ok(PathBuf::from(cwd).join(path))
    }
}

/// Relative tool paths must stay inside the current project. Absolute and
/// `~/` paths remain explicit opt-ins, but `../../…` should not quietly mutate
/// files outside the pane's cwd while the approval prompt shows a relative path.
pub(crate) fn reject_relative_cwd_escape(raw_path: &str, resolved: &Path, cwd: &str) -> Result<()> {
    if raw_path.starts_with('/') || raw_path.starts_with("~/") || raw_path == "~" {
        return Ok(());
    }

    let canon_cwd =
        std::fs::canonicalize(cwd).with_context(|| format!("resolve working directory '{cwd}'"))?;
    if let Ok(canon_path) = std::fs::canonicalize(resolved) {
        if !canon_path.starts_with(&canon_cwd) {
            anyhow::bail!(
                "path '{}' resolves outside the working directory; \
                 use an absolute path to access it",
                raw_path
            );
        }
        return Ok(());
    }

    let mut existing = resolved.to_path_buf();
    while !existing.exists() {
        if !existing.pop() {
            break;
        }
    }
    if existing.exists() {
        let canon_existing = std::fs::canonicalize(&existing)
            .with_context(|| format!("resolve '{}'", existing.display()))?;
        if !canon_existing.starts_with(&canon_cwd) {
            anyhow::bail!(
                "path '{}' resolves outside the working directory; \
                 use an absolute path to access it",
                raw_path
            );
        }
    }

    let mut lexical = canon_cwd.clone();
    for component in Path::new(raw_path).components() {
        match component {
            std::path::Component::CurDir => {}
            std::path::Component::Normal(part) => lexical.push(part),
            std::path::Component::ParentDir => {
                lexical.pop();
                if !lexical.starts_with(&canon_cwd) {
                    anyhow::bail!(
                        "path '{}' resolves outside the working directory; \
                         use an absolute path to access it",
                        raw_path
                    );
                }
            }
            _ => {}
        }
    }

    Ok(())
}

/// Resolve an explicit raw tool path against `cwd` and run both sandbox guards.
///
/// Single source of truth for the path-validation preamble shared by every
/// fs / search / tree tool: resolve `~`/relative paths, then
/// `reject_if_sensitive` before `reject_relative_cwd_escape`. Collapsing
/// identical copies here keeps the guard sequence from drifting between call
/// sites.
pub(crate) fn resolve_checked_path(raw_path: &str, cwd: &str) -> Result<PathBuf> {
    let path = resolve(raw_path, cwd)?;
    reject_if_sensitive(&path)?;
    reject_relative_cwd_escape(raw_path, &path, cwd)?;
    Ok(path)
}

/// Resolve optional `args["path"]`, defaulting to `cwd`, and run both guards.
pub(crate) fn resolve_checked_optional_arg(args: &serde_json::Value, cwd: &str) -> Result<PathBuf> {
    let raw_path = optional_path_arg(args, cwd);
    resolve_checked_path(raw_path, cwd)
}

/// Return optional `args["path"]`, defaulting to `cwd`, without resolving it.
pub(crate) fn optional_path_arg<'a>(args: &'a serde_json::Value, cwd: &'a str) -> &'a str {
    args["path"].as_str().unwrap_or(cwd)
}

/// Resolve required `args["path"]` against `cwd` and run both guards.
pub(crate) fn resolve_checked_arg(args: &serde_json::Value, cwd: &str) -> Result<PathBuf> {
    let raw_path = args["path"].as_str().context("missing path")?;
    resolve_checked_path(raw_path, cwd)
}

/// Resolve a read path once before approval and replace the model-supplied
/// argument with its canonical target. Approval text and execution then refer
/// to the same file rather than following a symlink twice across the prompt.
pub(crate) fn pin_read_arg(args: &mut serde_json::Value, cwd: &str) -> Result<()> {
    let path = resolve_checked_arg(args, cwd)?;
    let canonical = std::fs::canonicalize(&path)
        .with_context(|| format!("resolve read target '{}'", path.display()))?;
    args["path"] = serde_json::Value::String(canonical.to_string_lossy().into_owned());
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn resolve_expands_tilde() {
        let home = std::env::var("HOME").expect("HOME not set");
        assert_eq!(
            resolve("~/foo", "/tmp").unwrap(),
            PathBuf::from(&home).join("foo")
        );
        assert_eq!(resolve("~", "/tmp").unwrap(), PathBuf::from(&home));
    }

    #[test]
    fn resolve_absolute_unchanged() {
        assert_eq!(
            resolve("/etc/passwd", "/tmp").unwrap(),
            PathBuf::from("/etc/passwd")
        );
    }

    #[test]
    fn resolve_checked_arg_resolves_normal_path() {
        let args = serde_json::json!({ "path": "/tmp" });
        assert_eq!(
            resolve_checked_arg(&args, "/tmp").unwrap(),
            PathBuf::from("/tmp")
        );
    }

    #[test]
    fn resolve_checked_path_resolves_normal_path() {
        assert_eq!(
            resolve_checked_path("/tmp", "/tmp").unwrap(),
            PathBuf::from("/tmp")
        );
    }

    #[test]
    fn resolve_checked_optional_arg_defaults_to_cwd() {
        let args = serde_json::json!({});
        assert_eq!(
            resolve_checked_optional_arg(&args, "/tmp").unwrap(),
            PathBuf::from("/tmp")
        );
    }

    #[test]
    fn optional_path_arg_preserves_raw_relative_path() {
        let args = serde_json::json!({ "path": "src" });
        assert_eq!(optional_path_arg(&args, "/tmp/project"), "src");
    }

    #[test]
    fn resolve_checked_arg_rejects_sensitive_path() {
        let home = std::env::var("HOME").expect("HOME not set");
        let args = serde_json::json!({ "path": format!("{home}/.ssh/id_rsa") });
        let err = resolve_checked_arg(&args, "/tmp").unwrap_err();
        assert!(err.to_string().contains("protected") || err.to_string().contains("credential"));
    }

    #[test]
    fn resolve_checked_arg_requires_path_arg() {
        let args = serde_json::json!({});
        let err = resolve_checked_arg(&args, "/tmp").unwrap_err();
        assert!(err.to_string().contains("missing path"));
    }

    #[cfg(unix)]
    #[test]
    fn pin_read_arg_replaces_symlink_with_approved_target() {
        use std::os::unix::fs::symlink;

        let root = tempfile::tempdir().unwrap();
        let target = root.path().join("prod_credentials.py");
        let link = root.path().join("safe.py");
        std::fs::write(&target, "TOKEN = 'secret'\n").unwrap();
        symlink(&target, &link).unwrap();
        let mut args = serde_json::json!({ "path": link });

        pin_read_arg(&mut args, root.path().to_str().unwrap()).unwrap();

        let canonical_target = std::fs::canonicalize(&target).unwrap();
        assert_eq!(args["path"].as_str(), canonical_target.to_str());
        assert!(requires_read_approval(&target));
    }

    #[test]
    fn resolve_relative_to_cwd() {
        assert_eq!(
            resolve("src/main.rs", "/project").unwrap(),
            PathBuf::from("/project/src/main.rs")
        );
    }

    #[test]
    fn reject_if_sensitive_blocks_ssh() {
        let home = std::env::var("HOME").expect("HOME not set");
        let ssh = PathBuf::from(&home).join(".ssh");
        let err = reject_if_sensitive(&ssh).expect_err("must reject ~/.ssh");
        assert!(err.to_string().contains("protected secret location"));
    }

    #[test]
    fn reject_if_sensitive_blocks_assistant_config() {
        let home = std::env::var("HOME").expect("HOME not set");
        let assistant_config = PathBuf::from(&home).join(".config/kaku/assistant.toml");
        let err = reject_if_sensitive(&assistant_config).expect_err("must reject assistant config");
        assert!(err.to_string().contains("protected secret location"));
    }

    #[test]
    fn reject_if_sensitive_blocks_actual_xdg_assistant_config() {
        let assistant_config = config::user_config_path()
            .parent()
            .unwrap()
            .join("assistant.toml");
        let err = reject_if_sensitive(&assistant_config).expect_err("must reject active config");
        assert!(err.to_string().contains("protected") || err.to_string().contains("credential"));
    }

    #[test]
    fn reject_if_sensitive_blocks_token_dotfiles_in_any_directory() {
        let dir = tempfile::tempdir().unwrap();
        for name in [".npmrc", ".pypirc", ".netrc", ".git-credentials"] {
            let path = dir.path().join(name);
            let err = reject_if_sensitive(&path)
                .err()
                .unwrap_or_else(|| panic!("must reject {}", path.display()));
            assert!(err.to_string().contains("credential"));
        }
    }

    #[test]
    fn reject_if_sensitive_allows_normal_paths() {
        // /tmp is not in the blocked list; resolve_if_sensitive must Ok it.
        assert!(reject_if_sensitive(&PathBuf::from("/tmp")).is_ok());
    }

    #[test]
    fn credentials_named_source_files_stay_readable() {
        let dir = tempfile::tempdir().unwrap();
        for name in ["credentials.rs", "credentials_test.go", "credentials.md"] {
            let path = dir.path().join(name);
            std::fs::write(&path, "code").unwrap();
            assert!(
                reject_if_sensitive(&path).is_ok(),
                "{} is source/doc, must stay readable",
                name
            );
        }
        for name in ["credentials", "credentials.json", "aws_credentials.toml"] {
            let path = dir.path().join(name);
            std::fs::write(&path, "secret").unwrap();
            assert!(
                reject_if_sensitive(&path).is_err(),
                "{} is a credential data file, must stay blocked",
                name
            );
        }
    }

    #[test]
    fn relative_cwd_escape_rejects_parent_traversal_outside_cwd() {
        let dir = tempfile::tempdir().unwrap();
        let cwd = dir.path().join("project");
        std::fs::create_dir(&cwd).unwrap();
        let raw = "../outside.txt";
        let resolved = resolve(raw, cwd.to_str().unwrap()).unwrap();
        let err = reject_relative_cwd_escape(raw, &resolved, cwd.to_str().unwrap())
            .expect_err("must reject cwd escape");
        assert!(err.to_string().contains("outside the working directory"));
    }

    #[test]
    fn relative_cwd_escape_allows_nested_missing_paths() {
        let dir = tempfile::tempdir().unwrap();
        let cwd = dir.path().join("project");
        std::fs::create_dir(&cwd).unwrap();
        let raw = "src/generated/file.txt";
        let resolved = resolve(raw, cwd.to_str().unwrap()).unwrap();
        reject_relative_cwd_escape(raw, &resolved, cwd.to_str().unwrap()).unwrap();
    }

    // ─── Sandbox audit additions ─────────────────────────────────────────
    // The original tests cover the common case (~/.ssh, assistant.toml,
    // single-level cwd escape). The cases below tighten coverage on attacks
    // that look subtle: symlink escape, deep parent traversal, multiple
    // hard-coded sensitive locations not previously exercised.

    #[test]
    fn reject_if_sensitive_blocks_aws_credentials() {
        let home = std::env::var("HOME").expect("HOME not set");
        let aws = PathBuf::from(&home).join(".aws/credentials");
        let err = reject_if_sensitive(&aws).expect_err("must reject ~/.aws/credentials");
        assert!(err.to_string().contains("protected secret location"));
    }

    #[test]
    fn reject_if_sensitive_blocks_gnupg() {
        let home = std::env::var("HOME").expect("HOME not set");
        let gpg = PathBuf::from(&home).join(".gnupg");
        let err = reject_if_sensitive(&gpg).expect_err("must reject ~/.gnupg");
        assert!(err.to_string().contains("protected secret location"));
    }

    #[test]
    fn reject_if_sensitive_blocks_common_cli_token_stores() {
        let home = PathBuf::from(std::env::var("HOME").expect("HOME not set"));
        for relative in [
            ".config/gh/hosts.yml",
            ".docker/config.json",
            ".git-credentials",
            ".netrc",
            ".npmrc",
            ".pypirc",
        ] {
            let path = home.join(relative);
            let error =
                reject_if_sensitive(&path).expect_err("common CLI token store must be rejected");
            assert!(error.to_string().contains("protected secret location"));
        }
    }

    #[test]
    fn reject_if_sensitive_blocks_descendant_of_sensitive_dir() {
        // ~/.ssh/id_rsa, ~/.ssh/config, etc must all be blocked because
        // ~/.ssh as a directory is in the blocklist (starts_with semantic).
        let home = std::env::var("HOME").expect("HOME not set");
        let key = PathBuf::from(&home).join(".ssh/id_rsa");
        let err = reject_if_sensitive(&key).expect_err("must reject ~/.ssh/id_rsa");
        assert!(err.to_string().contains("protected secret location"));
    }

    #[test]
    fn reject_if_sensitive_blocks_etc_shadow_via_private_alias() {
        // macOS exposes /etc/shadow at both /etc/shadow and /private/etc/shadow.
        // The blocklist includes both. Verify the /private variant explicitly.
        let path = PathBuf::from("/private/etc/sudoers");
        let err = reject_if_sensitive(&path).expect_err("must reject /private/etc/sudoers");
        assert!(err.to_string().contains("protected secret location"));
    }

    #[test]
    #[cfg(unix)]
    fn reject_if_sensitive_follows_symlink_to_blocked_dir() {
        // Attack shape: a symlink inside the project points to ~/.ssh. If
        // canonicalize() resolves the symlink before comparing, the blocklist
        // catches it. If it didn't, exfiltration via fs_read would bypass the
        // guard. This is exactly what canonicalize() is for.
        use std::os::unix::fs as unix_fs;
        let dir = tempfile::tempdir().unwrap();
        let home = std::env::var("HOME").expect("HOME not set");
        let target = PathBuf::from(&home).join(".ssh");
        if !target.exists() {
            // ~/.ssh might not exist on this machine; skip the realism check
            // and just assert the lexical match still triggers (covered by
            // reject_if_sensitive_blocks_ssh).
            return;
        }
        let link = dir.path().join("ssh_link");
        unix_fs::symlink(&target, &link).unwrap();
        let err = reject_if_sensitive(&link)
            .expect_err("symlink to ~/.ssh must be rejected via canonicalize");
        assert!(err.to_string().contains("protected secret location"));
    }

    #[test]
    fn relative_cwd_escape_rejects_deep_parent_traversal() {
        // ../../../../etc/passwd is the textbook directory-traversal payload.
        // The lexical fallback must catch it even when none of the intermediate
        // parents exist on disk.
        let dir = tempfile::tempdir().unwrap();
        let cwd = dir.path().join("project");
        std::fs::create_dir(&cwd).unwrap();
        let raw = "../../../../etc/passwd";
        let resolved = resolve(raw, cwd.to_str().unwrap()).unwrap();
        let err = reject_relative_cwd_escape(raw, &resolved, cwd.to_str().unwrap())
            .expect_err("deep ../ chain must be rejected");
        assert!(err.to_string().contains("outside the working directory"));
    }

    #[test]
    fn relative_cwd_escape_rejects_mixed_traversal() {
        // ./foo/../../escape is a sneakier variant: it climbs out of cwd via
        // a relative path that visually looks innocent (./foo/...). The
        // lexical walk must catch the net upward movement.
        let dir = tempfile::tempdir().unwrap();
        let cwd = dir.path().join("project");
        std::fs::create_dir(&cwd).unwrap();
        let raw = "./foo/../../escape.txt";
        let resolved = resolve(raw, cwd.to_str().unwrap()).unwrap();
        let err = reject_relative_cwd_escape(raw, &resolved, cwd.to_str().unwrap())
            .expect_err("mixed ./ + ../ traversal must be rejected");
        assert!(err.to_string().contains("outside the working directory"));
    }

    #[test]
    fn relative_cwd_escape_allows_within_cwd_traversal() {
        // foo/../bar.txt is just bar.txt, no escape. Must not false-positive.
        let dir = tempfile::tempdir().unwrap();
        let cwd = dir.path().join("project");
        std::fs::create_dir(&cwd).unwrap();
        let raw = "foo/../bar.txt";
        let resolved = resolve(raw, cwd.to_str().unwrap()).unwrap();
        reject_relative_cwd_escape(raw, &resolved, cwd.to_str().unwrap())
            .expect("in-cwd traversal must be allowed");
    }

    // ─── Credential file-name guard ───────────────────────────────────
    //
    // `reject_if_sensitive` blocks well-known credential file names
    // (`.env`, `*.pem`, `*.key`, SSH private keys) in any directory, while
    // still allowing committed example/template files.
    #[test]
    fn reject_if_sensitive_blocks_project_env_file() {
        let dir = tempfile::tempdir().unwrap();
        let env_file = dir.path().join(".env");
        std::fs::write(&env_file, "API_KEY=secret").unwrap();
        assert!(
            reject_if_sensitive(&env_file).is_err(),
            "a project-local .env must be refused"
        );
    }

    #[cfg(unix)]
    #[test]
    fn read_approval_follows_symlink_to_credential_source() {
        use std::os::unix::fs::symlink;

        let root = tempfile::tempdir().unwrap();
        let target = root.path().join("prod_credentials.py");
        let link = root.path().join("safe.py");
        std::fs::write(&target, "TOKEN = 'secret'\n").unwrap();
        symlink(&target, &link).unwrap();

        assert!(requires_read_approval(&target));
        assert!(requires_read_approval(&link));
    }

    #[test]
    fn reject_if_sensitive_blocks_dotenv_variants_and_keys() {
        let dir = tempfile::tempdir().unwrap();
        for name in [
            ".env.local",
            ".env.production",
            "server.pem",
            "tls.key",
            "id_rsa",
            "id_ed25519",
        ] {
            let f = dir.path().join(name);
            std::fs::write(&f, "secret").unwrap();
            assert!(
                reject_if_sensitive(&f).is_err(),
                "{} should be refused as a credential file",
                name
            );
        }
    }

    #[test]
    fn reject_if_sensitive_allows_env_examples_and_public_keys() {
        let dir = tempfile::tempdir().unwrap();
        for name in [
            ".env.example",
            ".env.sample",
            "config.pem.template",
            "id_rsa.pub",
            "README.md",
        ] {
            let f = dir.path().join(name);
            std::fs::write(&f, "not a secret").unwrap();
            assert!(
                reject_if_sensitive(&f).is_ok(),
                "{} should remain readable",
                name
            );
        }
    }
}
