//! The documented DSH plugin path: a Codewhale bundle package installed into
//! a dedicated `codewhale` DSH profile with `dsh plugin --profile codewhale
//! add <absolute path>` (pnpm required).
//!
//! The bundle is an npm-shaped package under
//! `$CODEWHALE_HOME/integrations/dsh/bundle/` whose `cordis.patch.yml`
//! carries the same identity rows as the `--patch` overlay. Because
//! `dsh plugin add <path>` records a `link:` dependency, `update` only has to
//! rewrite the patch file. The dedicated profile also gets DSH's own shipped
//! app bundle (`@deepseek-ai/dsh-web-app` or `dsh-headless`) linked from the
//! installed launcher, so `dsh --profile codewhale` boots without `--patch`.
//! The user's `web`/`headless` profiles are never touched. The profile
//! directory itself is DSH-owned and is left in place on removal.

use std::path::{Path, PathBuf};

use anyhow::{Context, Result};
use serde::{Deserialize, Serialize};

use super::detect::{DshDetection, DshRunner};
use super::identity::sha256_hex;
use super::receipt::write_atomic;
use super::skin::{self, SKIN_SOURCE};
use super::{brand, scene};

pub(crate) const BUNDLE_DIR: &str = "bundle";
pub(crate) const BUNDLE_PACKAGE_NAME: &str = "codewhale-dsh-bundle";
pub(crate) const BUNDLE_PATCH_FILE: &str = "cordis.patch.yml";
pub(crate) const BUNDLE_PROFILE: &str = "codewhale";
pub(crate) const BUNDLE_CLIENT_FILE: &str = "lib/client.js";
pub(crate) const BUNDLE_INDEX_FILE: &str = "lib/index.js";
/// Last row of `cordis.patch.yml` when the skin is enabled. Appended after the
/// identity overlay so the `--patch` overlay file stays byte-identical.
pub(crate) const SKIN_INSERT_YAML: &str =
    "- insert: [{ id: codewhale-skin, name: codewhale-dsh-bundle }]\n";

/// Which shipped DSH app bundle the dedicated profile boots.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "kebab-case")]
pub(crate) enum DshAppBundle {
    Web,
    Headless,
}

impl DshAppBundle {
    pub(crate) fn parse(value: &str) -> Option<Self> {
        match value {
            "web" => Some(Self::Web),
            "headless" => Some(Self::Headless),
            _ => None,
        }
    }

    pub(crate) fn package_name(self) -> &'static str {
        match self {
            Self::Web => "@deepseek-ai/dsh-web-app",
            Self::Headless => "@deepseek-ai/dsh-headless",
        }
    }
}

/// Durable facts about an installed bundle.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub(crate) struct DshBundleRecord {
    pub(crate) installed_at: String,
    pub(crate) updated_at: String,
    pub(crate) profile: String,
    /// `$DSH_HOME/profiles/codewhale` (DSH-owned).
    pub(crate) profile_dir: PathBuf,
    /// `$CODEWHALE_HOME/integrations/dsh/bundle` (Codewhale-owned).
    pub(crate) bundle_dir: PathBuf,
    pub(crate) package_name: String,
    pub(crate) package_version: String,
    /// SHA-256 of `cordis.patch.yml` (identical to the overlay text).
    pub(crate) patch_sha256: String,
    pub(crate) app_bundle: DshAppBundle,
    /// Where the app bundle was linked from (the installed launcher).
    pub(crate) app_bundle_source: PathBuf,
    pub(crate) pnpm_version: String,
    /// SHA-256 of the combined `dsh plugin` output (never the text itself).
    pub(crate) pnpm_output_sha256: String,
}

/// Availability of the plugin path on this machine.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(tag = "kind", rename_all = "snake_case")]
pub(crate) enum BundleAvailability {
    Available { pnpm_version: String },
    NotAvailable { reason: String },
}

impl BundleAvailability {
    pub(crate) fn label(&self) -> String {
        match self {
            Self::Available { pnpm_version } => format!("available (pnpm {pnpm_version})"),
            Self::NotAvailable { reason } => format!("not available: {reason}"),
        }
    }
}

fn find_on_path(path: Option<&std::ffi::OsString>, names: &[&str]) -> Option<PathBuf> {
    let path = path?;
    for dir in std::env::split_paths(path) {
        if dir.as_os_str().is_empty() {
            continue;
        }
        for name in names {
            let candidate = dir.join(name);
            if candidate.is_file() {
                return Some(candidate);
            }
        }
    }
    None
}

/// Probe pnpm on `PATH` (dsh shells out to it by name).
pub(crate) fn bundle_availability(
    path: Option<&std::ffi::OsString>,
    runner: &dyn DshRunner,
) -> BundleAvailability {
    let Some(pnpm) = find_on_path(path, &["pnpm", "pnpm.cmd", "pnpm.exe"]) else {
        return BundleAvailability::NotAvailable {
            reason: "pnpm missing from PATH (dsh plugin shells out to pnpm)".to_string(),
        };
    };
    match runner.run(&pnpm, &["--version"]) {
        Ok((true, text)) => {
            let version = text
                .lines()
                .map(str::trim)
                .rfind(|l| !l.is_empty() && l.chars().next().is_some_and(|c| c.is_ascii_digit()))
                .unwrap_or("")
                .to_string();
            if version.is_empty() {
                BundleAvailability::NotAvailable {
                    reason: "pnpm --version printed no version".to_string(),
                }
            } else {
                BundleAvailability::Available {
                    pnpm_version: version,
                }
            }
        }
        Ok((false, _)) => BundleAvailability::NotAvailable {
            reason: "pnpm --version exited non-zero".to_string(),
        },
        Err(error) => BundleAvailability::NotAvailable {
            reason: format!("pnpm could not be run: {error}"),
        },
    }
}

/// Resolve the installed launcher's package root (`…/@deepseek-ai/dsh`)
/// from the `dsh` binary path (an npm bin shim symlink to `lib/bin.js`).
pub(crate) fn launcher_package_root(binary: &Path) -> Option<PathBuf> {
    let resolved = std::fs::canonicalize(binary).ok()?;
    // …/@deepseek-ai/dsh/lib/bin.js → …/@deepseek-ai/dsh
    let mut dir = resolved.parent()?.to_path_buf();
    for _ in 0..4 {
        if is_dsh_package_root(&dir) {
            return Some(dir);
        }
        let Some(parent) = dir.parent() else { break };
        dir = parent.to_path_buf();
    }
    // npm on Windows (and some Unix wrappers) install a copied `.cmd`/`.ps1`
    // shim next to the prefix's `node_modules` instead of a symlink into the
    // package, so the shim's directory owns the launcher package directly.
    let sibling = resolved
        .parent()?
        .join("node_modules")
        .join("@deepseek-ai")
        .join("dsh");
    is_dsh_package_root(&sibling).then_some(sibling)
}

fn is_dsh_package_root(dir: &Path) -> bool {
    dir.join("package.json").is_file()
        && std::fs::read_to_string(dir.join("package.json"))
            .ok()
            .is_some_and(|text| text.contains("\"@deepseek-ai/dsh\""))
}

/// The shipped app bundle directory inside the installed launcher, if it
/// declares `dsh.bundle.patch`.
pub(crate) fn app_bundle_source(binary: &Path, app: DshAppBundle) -> Result<PathBuf> {
    let root = launcher_package_root(binary).ok_or_else(|| {
        anyhow::anyhow!(
            "cannot locate the installed dsh package root from {}",
            binary.display()
        )
    })?;
    let dir = root.join("node_modules").join(app.package_name());
    let manifest = std::fs::read_to_string(dir.join("package.json"))
        .with_context(|| format!("read {}", dir.join("package.json").display()))?;
    if !manifest.contains("\"bundle\"") || !manifest.contains("\"patch\"") {
        anyhow::bail!(
            "{} does not declare dsh.bundle.patch; cannot link it as a profile bundle",
            dir.display()
        );
    }
    Ok(dir)
}

pub(crate) fn bundle_version(codewhale_version: &str, patch_sha256: &str) -> String {
    format!(
        "{codewhale_version}+dsh.{}",
        &patch_sha256[..12.min(patch_sha256.len())]
    )
}

/// Identity overlay plus the optional skin insert row. The `--patch` overlay
/// file is never this text — only the dedicated profile's `cordis.patch.yml`.
pub(crate) fn render_bundle_patch(overlay_text: &str, skin: bool) -> String {
    if !skin {
        return overlay_text.to_string();
    }
    let mut out = overlay_text.to_string();
    if !out.ends_with('\n') && !out.is_empty() {
        out.push('\n');
    }
    out.push_str(SKIN_INSERT_YAML);
    out
}

/// Files the bundle package consists of (all Codewhale-owned). `ocean` only
/// matters with `skin`: it splices the ambient scene into `lib/client.js`
/// (dsh serves one client file per plugin, so there is no `lib/scene.js`).
pub(crate) fn render_bundle_files(
    codewhale_version: &str,
    overlay_text: &str,
    skin: bool,
    ocean: bool,
) -> Vec<(&'static str, String)> {
    let patch_sha = sha256_hex(overlay_text.as_bytes());
    let version = bundle_version(codewhale_version, &patch_sha);
    let mut dsh = serde_json::json!({
        "bundle": { "patch": format!("./{BUNDLE_PATCH_FILE}") }
    });
    let mut package_json = serde_json::json!({
        "name": BUNDLE_PACKAGE_NAME,
        "version": version,
        "private": true,
        "description": "DeepSeek Harness connected through Codewhale: identity overlay bundle (generated; do not edit)",
        "license": "MIT",
        "dsh": dsh.clone(),
        "codewhale": {
            "generated_by": "codewhale integrations dsh install-bundle",
            "patch_sha256": patch_sha,
            "skin": skin,
            "ocean": skin && ocean,
        }
    });
    if skin {
        dsh["client"] = serde_json::json!({
            "platform": "web",
            "immediately": true,
            "inject": ["@deepseek-ai/dsh-client-ui-theme"],
        });
        package_json["dsh"] = dsh;
        package_json["type"] = serde_json::json!("module");
        package_json["main"] = serde_json::json!("./lib/index.js");
        // Node's exports map is exhaustive: the cordis loader imports the bare
        // package name (needs ".") and dsh-client-modules resolves
        // `<name>/package.json` (needs "./package.json"); without both, the
        // insert row fails with ERR_PACKAGE_PATH_NOT_EXPORTED and the client
        // half is silently never served.
        package_json["exports"] = serde_json::json!({
            ".": { "default": format!("./{BUNDLE_INDEX_FILE}") },
            "./client": { "default": format!("./{BUNDLE_CLIENT_FILE}") },
            "./package.json": "./package.json"
        });
        package_json["codewhale"]["skin_sha256"] = serde_json::json!(skin::skin_tokens_sha256());
        package_json["codewhale"]["skin_source"] = serde_json::json!(SKIN_SOURCE);
        package_json["codewhale"]["brand_sha256"] = serde_json::json!(brand::brand_sha256());
        if ocean {
            package_json["codewhale"]["ocean_scene_sha256"] =
                serde_json::json!(scene::scene_sha256());
        }
    }
    let readme = format!(
        "# {BUNDLE_PACKAGE_NAME}\n\nDeepSeek Harness connected through Codewhale.\n\nGenerated bundle: `{BUNDLE_PATCH_FILE}` carries the exact Codewhale provider/model/endpoint identity (no credentials). Installed into the dedicated DSH profile `{BUNDLE_PROFILE}` with `dsh plugin --profile {BUNDLE_PROFILE} add <this directory>`. Regenerated by `codewhale integrations dsh update`; removed by `codewhale integrations dsh remove-bundle`. Do not edit by hand.\n"
    );
    let notice = "This bundle is generated by Codewhale and configures the official DeepSeek Harness (dsh).\n\nDeepSeek Harness — MIT License, Copyright (c) 2026 DeepSeek. The DeepSeek Harness copyright and permission notice apply to the harness packages this bundle references; this bundle redistributes none of them.\n\nThis generated bundle is provided under the MIT License.\n".to_string();
    let mut files = vec![
        (
            "package.json",
            format!(
                "{}\n",
                serde_json::to_string_pretty(&package_json).expect("json")
            ),
        ),
        (BUNDLE_PATCH_FILE, render_bundle_patch(overlay_text, skin)),
        ("README.md", readme),
        ("NOTICE.md", notice),
    ];
    if skin {
        files.push((BUNDLE_INDEX_FILE, skin::bundle_index_js()));
        files.push((BUNDLE_CLIENT_FILE, skin::bundle_client_js(ocean)));
    }
    files
}

/// Write (or rewrite) the bundle package. Returns the identity-overlay SHA-256
/// (not the on-disk patch hash, which may include the skin insert row).
pub(crate) fn write_bundle(
    bundle_dir: &Path,
    codewhale_version: &str,
    overlay_text: &str,
    skin: bool,
    ocean: bool,
) -> Result<String> {
    std::fs::create_dir_all(bundle_dir)
        .with_context(|| format!("create {}", bundle_dir.display()))?;
    if skin {
        std::fs::create_dir_all(bundle_dir.join("lib"))
            .with_context(|| format!("create {}", bundle_dir.join("lib").display()))?;
    }
    for (name, text) in render_bundle_files(codewhale_version, overlay_text, skin, ocean) {
        write_atomic(&bundle_dir.join(name), text.as_bytes())?;
    }
    if !skin {
        remove_client_half(bundle_dir)?;
    }
    Ok(sha256_hex(overlay_text.as_bytes()))
}

fn remove_client_half(bundle_dir: &Path) -> Result<()> {
    for name in [BUNDLE_CLIENT_FILE, BUNDLE_INDEX_FILE] {
        let path = bundle_dir.join(name);
        match std::fs::remove_file(&path) {
            Ok(()) => {}
            Err(error) if error.kind() == std::io::ErrorKind::NotFound => {}
            Err(error) => return Err(error).with_context(|| format!("remove {}", path.display())),
        }
    }
    let _ = std::fs::remove_dir(bundle_dir.join("lib"));
    Ok(())
}

pub(crate) fn remove_bundle_files(bundle_dir: &Path) -> Result<Vec<PathBuf>> {
    let mut removed = Vec::new();
    for name in [
        "package.json",
        BUNDLE_PATCH_FILE,
        "README.md",
        "NOTICE.md",
        BUNDLE_CLIENT_FILE,
        BUNDLE_INDEX_FILE,
    ] {
        let path = bundle_dir.join(name);
        match std::fs::remove_file(&path) {
            Ok(()) => removed.push(path),
            Err(error) if error.kind() == std::io::ErrorKind::NotFound => {}
            Err(error) => return Err(error).with_context(|| format!("remove {}", path.display())),
        }
    }
    let _ = std::fs::remove_dir(bundle_dir.join("lib"));
    // Only remove the directory when Codewhale left nothing else in it.
    let _ = std::fs::remove_dir(bundle_dir);
    Ok(removed)
}

/// On-disk client half vs the receipt's skin/ocean decision. `None` means in
/// sync. The scene lives inside `lib/client.js`, so the byte comparison covers
/// it: an ocean toggle or a drifted scene reads as a modified client half.
pub(crate) fn client_half_stale(bundle_dir: &Path, skin: bool, ocean: bool) -> Option<String> {
    let client = bundle_dir.join(BUNDLE_CLIENT_FILE);
    let present = client.is_file();
    if skin {
        if !present {
            return Some("bundle lib/client.js is missing; run `update`".to_string());
        }
        match std::fs::read_to_string(&client) {
            Ok(text) if text == skin::bundle_client_js(ocean) => None,
            Ok(_) => Some(
                "bundle lib/client.js was modified outside Codewhale; run `update`".to_string(),
            ),
            Err(_) => Some("bundle lib/client.js is unreadable; run `update`".to_string()),
        }
    } else if present {
        Some("bundle lib/client.js is present but skin is disabled; run `update`".to_string())
    } else {
        None
    }
}

/// Outcome of one `dsh plugin …` invocation (output is hashed, not kept).
#[derive(Debug, Clone, PartialEq, Eq)]
pub(crate) struct PluginCommandOutcome {
    pub(crate) args: Vec<String>,
    pub(crate) success: bool,
    pub(crate) output_sha256: String,
    pub(crate) output_excerpt: String,
}

fn run_dsh_plugin(
    runner: &dyn DshRunner,
    binary: &Path,
    profile: &str,
    verb_and_args: &[&str],
) -> Result<PluginCommandOutcome> {
    let mut args = vec!["plugin", "--profile", profile];
    args.extend_from_slice(verb_and_args);
    let (success, output) = runner.run(binary, &args).with_context(|| {
        format!(
            "run dsh plugin --profile {profile} {}",
            verb_and_args.join(" ")
        )
    })?;
    let excerpt: String = output
        .lines()
        .filter(|l| {
            !l.trim().is_empty()
                && !l.contains("pnpm.io")
                && !l.contains('│')
                && !l.contains('╭')
                && !l.contains('╰')
        })
        .rev()
        .take(4)
        .collect::<Vec<_>>()
        .into_iter()
        .rev()
        .collect::<Vec<_>>()
        .join(" | ");
    Ok(PluginCommandOutcome {
        args: args.iter().map(|s| (*s).to_string()).collect(),
        success,
        output_sha256: sha256_hex(output.as_bytes()),
        output_excerpt: excerpt.chars().take(400).collect(),
    })
}

/// Install the app bundle (linked from the installed launcher) and then the
/// Codewhale bundle, in that order so Codewhale's rows patch last.
pub(crate) fn install_into_profile(
    runner: &dyn DshRunner,
    detection: &DshDetection,
    app: DshAppBundle,
    bundle_dir: &Path,
) -> Result<(PathBuf, Vec<PluginCommandOutcome>)> {
    let binary = detection
        .binary
        .as_ref()
        .ok_or_else(|| anyhow::anyhow!("dsh binary is unknown"))?;
    let app_source = app_bundle_source(binary, app)?;
    let mut outcomes = Vec::new();
    let app_source_str = app_source.display().to_string();
    let first = run_dsh_plugin(runner, binary, BUNDLE_PROFILE, &["add", &app_source_str])?;
    let first_ok = first.success;
    let first_excerpt = first.output_excerpt.clone();
    outcomes.push(first);
    if !first_ok {
        anyhow::bail!(
            "dsh plugin add {} failed: {}",
            app.package_name(),
            first_excerpt
        );
    }
    let bundle_str = bundle_dir.display().to_string();
    let second = run_dsh_plugin(runner, binary, BUNDLE_PROFILE, &["add", &bundle_str])?;
    let second_ok = second.success;
    let second_excerpt = second.output_excerpt.clone();
    outcomes.push(second);
    if !second_ok {
        anyhow::bail!("dsh plugin add {BUNDLE_PACKAGE_NAME} failed: {second_excerpt}");
    }
    Ok((app_source, outcomes))
}

pub(crate) fn remove_from_profile(
    runner: &dyn DshRunner,
    detection: &DshDetection,
) -> Result<PluginCommandOutcome> {
    let binary = detection
        .binary
        .as_ref()
        .ok_or_else(|| anyhow::anyhow!("dsh binary is unknown"))?;
    let outcome = run_dsh_plugin(
        runner,
        binary,
        BUNDLE_PROFILE,
        &["remove", BUNDLE_PACKAGE_NAME],
    )?;
    if !outcome.success {
        anyhow::bail!(
            "dsh plugin remove {BUNDLE_PACKAGE_NAME} failed: {}",
            outcome.output_excerpt
        );
    }
    Ok(outcome)
}

/// Read the dedicated profile's `dsh.profile.bundles` (DSH-owned manifest,
/// read-only) so status can prove the bundle is actually composed.
pub(crate) fn profile_bundles(profile_dir: &Path) -> Option<Vec<String>> {
    let text = std::fs::read_to_string(profile_dir.join("package.json")).ok()?;
    let json: serde_json::Value = serde_json::from_str(&text).ok()?;
    Some(
        json.get("dsh")?
            .get("profile")?
            .get("bundles")?
            .as_array()?
            .iter()
            .filter_map(|v| v.as_str().map(str::to_string))
            .collect(),
    )
}
