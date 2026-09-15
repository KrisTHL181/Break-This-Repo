//! Consent-gated external MCP imports.
//!
//! Discovery can scan `~/.claude.json`, project `.mcp.json`, and marketplace
//! manifests, but **nothing is connected until the user approves**. Provenance
//! (source path + content hash) is shown before import. `enabled=false` and
//! `disabled=true` on a source entry are hard blocks — those candidates never
//! become managed connectors even after a blanket approval.
//!
//! Design (Kimi session_a75a393a-a984-4f35-98d0-b78cfbdcf23f): keep discovery
//! pure and independent of the TUI; merge approved servers through the same
//! config write path as `/mcp add`.

use std::collections::HashMap;
use std::fs;
use std::path::{Path, PathBuf};

use serde::{Deserialize, Serialize};
use serde_json::Value;
use sha2::{Digest, Sha256};

use super::{McpConfig, McpServerConfig};

/// Where an import candidate came from.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum ExternalMcpSourceKind {
    ClaudeJson,
    ProjectMcpJson,
    Marketplace,
}

impl ExternalMcpSourceKind {
    #[must_use]
    pub fn as_str(&self) -> &'static str {
        match self {
            Self::ClaudeJson => "claude.json",
            Self::ProjectMcpJson => ".mcp.json",
            Self::Marketplace => "marketplace",
        }
    }
}

/// One discovered server before consent.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ImportCandidate {
    pub name: String,
    pub source_kind: ExternalMcpSourceKind,
    pub source_path: PathBuf,
    /// Hex sha256 of the raw source file (or marketplace entry blob).
    pub content_hash: String,
    pub summary: String,
    /// When true the entry is present but must never connect.
    pub hard_blocked: bool,
    pub block_reason: Option<String>,
    pub server: McpServerConfig,
}

/// User decision for one candidate.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum ImportDecision {
    Approve,
    Decline,
    Skip,
}

/// Durable consent / decline record keyed by source path + hash.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct ImportConsentStore {
    #[serde(default)]
    pub entries: HashMap<String, ConsentEntry>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ConsentEntry {
    pub source_path: String,
    pub content_hash: String,
    pub decision: ImportDecision,
    pub decided_at_unix: u64,
    pub servers: Vec<String>,
}

fn consent_key(path: &Path, hash: &str) -> String {
    format!("{}::{hash}", path.display())
}

/// Discover candidates from well-known external locations. Never connects.
pub fn discover_external_sources(
    home: &Path,
    workspace: &Path,
    marketplace_paths: &[PathBuf],
) -> Vec<ImportCandidate> {
    let mut out = Vec::new();
    let claude = home.join(".claude.json");
    if claude.is_file() {
        out.extend(discover_from_json_file(
            &claude,
            ExternalMcpSourceKind::ClaudeJson,
        ));
    }
    let project_mcp = workspace.join(".mcp.json");
    if project_mcp.is_file() {
        out.extend(discover_from_json_file(
            &project_mcp,
            ExternalMcpSourceKind::ProjectMcpJson,
        ));
    }
    for path in marketplace_paths {
        if path.is_file() {
            out.extend(discover_from_json_file(
                path,
                ExternalMcpSourceKind::Marketplace,
            ));
        }
    }
    out
}

fn discover_from_json_file(path: &Path, kind: ExternalMcpSourceKind) -> Vec<ImportCandidate> {
    let Ok(raw) = fs::read(path) else {
        return Vec::new();
    };
    let hash = hex_sha256(&raw);
    let Ok(value) = serde_json::from_slice::<Value>(&raw) else {
        return Vec::new();
    };
    let servers = extract_servers_map(&value);
    let mut out = Vec::with_capacity(servers.len());
    for (name, cfg_value) in servers {
        let Ok(server) = serde_json::from_value::<McpServerConfig>(cfg_value.clone()) else {
            continue;
        };
        let hard_blocked = !server.is_enabled();
        let summary = server_summary(&name, &server);
        out.push(ImportCandidate {
            name,
            source_kind: kind.clone(),
            source_path: path.to_path_buf(),
            content_hash: hash.clone(),
            summary,
            hard_blocked,
            block_reason: hard_blocked.then(|| {
                "enabled=false (or disabled=true) is a hard block — will not import".to_string()
            }),
            server,
        });
    }
    out
}

fn extract_servers_map(value: &Value) -> Vec<(String, Value)> {
    // Claude / team: { "mcpServers": { name: {...} } }
    // Marketplace catalog: { "servers": { name: {...} } } or array of {name, ...}
    if let Some(map) = value
        .get("mcpServers")
        .or_else(|| value.get("servers"))
        .and_then(|v| v.as_object())
    {
        return map.iter().map(|(k, v)| (k.clone(), v.clone())).collect();
    }
    if let Some(arr) = value.as_array() {
        let mut out = Vec::new();
        for item in arr {
            let Some(name) = item.get("name").and_then(|v| v.as_str()) else {
                continue;
            };
            out.push((name.to_string(), item.clone()));
        }
        return out;
    }
    Vec::new()
}

fn server_summary(name: &str, server: &McpServerConfig) -> String {
    if let Some(url) = server.url.as_deref() {
        return format!("{name} — http {url}");
    }
    if let Some(cmd) = server.command.as_deref() {
        let args = server.args.join(" ");
        if args.is_empty() {
            return format!("{name} — stdio {cmd}");
        }
        return format!("{name} — stdio {cmd} {args}");
    }
    format!("{name} — (incomplete server config)")
}

fn hex_sha256(bytes: &[u8]) -> String {
    let digest = Sha256::digest(bytes);
    digest.iter().map(|b| format!("{b:02x}")).collect()
}

/// Load consent store from disk (missing file → empty).
pub fn load_consent_store(path: &Path) -> ImportConsentStore {
    let Ok(raw) = fs::read_to_string(path) else {
        return ImportConsentStore::default();
    };
    serde_json::from_str(&raw).unwrap_or_default()
}

/// Persist consent store atomically-ish (write then rename best-effort).
pub fn save_consent_store(path: &Path, store: &ImportConsentStore) -> std::io::Result<()> {
    if let Some(parent) = path.parent() {
        fs::create_dir_all(parent)?;
    }
    let raw = serde_json::to_string_pretty(store)
        .map_err(|err| std::io::Error::new(std::io::ErrorKind::InvalidData, err))?;
    let tmp = path.with_extension("json.tmp");
    fs::write(&tmp, raw)?;
    fs::rename(tmp, path)?;
    Ok(())
}

/// Filter candidates that still need a user decision for this content hash.
#[allow(dead_code)] // used by future selector UI + unit tests
pub fn candidates_needing_consent(
    candidates: &[ImportCandidate],
    store: &ImportConsentStore,
) -> Vec<ImportCandidate> {
    candidates
        .iter()
        .filter(|c| {
            let key = consent_key(&c.source_path, &c.content_hash);
            match store.entries.get(&key) {
                Some(entry) if entry.decision == ImportDecision::Decline => false,
                Some(entry) if entry.decision == ImportDecision::Approve => {
                    // Re-prompt only when the specific server was not part of
                    // the prior approval list (partial approval).
                    !entry.servers.iter().any(|s| s == &c.name)
                }
                _ => true,
            }
        })
        .cloned()
        .collect()
}

/// Apply approvals: returns servers to merge into user mcp.json.
/// Hard-blocked candidates are never returned even if decision is Approve.
pub fn apply_approved(
    candidates: &[ImportCandidate],
    decisions: &HashMap<String, ImportDecision>,
) -> Vec<(String, McpServerConfig, ImportCandidate)> {
    let mut out = Vec::new();
    for candidate in candidates {
        let decision = decisions
            .get(&candidate.name)
            .copied()
            .unwrap_or(ImportDecision::Skip);
        if decision != ImportDecision::Approve {
            continue;
        }
        if candidate.hard_blocked {
            continue;
        }
        out.push((
            candidate.name.clone(),
            candidate.server.clone(),
            candidate.clone(),
        ));
    }
    out
}

/// Record decisions in the consent store (including declines).
pub fn record_decisions(
    store: &mut ImportConsentStore,
    candidates: &[ImportCandidate],
    decisions: &HashMap<String, ImportDecision>,
    now_unix: u64,
) {
    // Group by source path + hash so one file approval is one entry.
    let mut by_source: HashMap<(PathBuf, String), Vec<(&ImportCandidate, ImportDecision)>> =
        HashMap::new();
    for candidate in candidates {
        let decision = decisions
            .get(&candidate.name)
            .copied()
            .unwrap_or(ImportDecision::Skip);
        if decision == ImportDecision::Skip {
            continue;
        }
        by_source
            .entry((
                candidate.source_path.clone(),
                candidate.content_hash.clone(),
            ))
            .or_default()
            .push((candidate, decision));
    }
    for ((path, hash), group) in by_source {
        // If any approval exists, record Approve with the approved names;
        // pure decline groups record Decline.
        let any_approve = group.iter().any(|(_, d)| *d == ImportDecision::Approve);
        let decision = if any_approve {
            ImportDecision::Approve
        } else {
            ImportDecision::Decline
        };
        let servers: Vec<String> = group
            .iter()
            .filter(|(_, d)| *d == ImportDecision::Approve)
            .filter(|(c, _)| !c.hard_blocked)
            .map(|(c, _)| c.name.clone())
            .collect();
        let key = consent_key(&path, &hash);
        store.entries.insert(
            key,
            ConsentEntry {
                source_path: path.display().to_string(),
                content_hash: hash,
                decision,
                decided_at_unix: now_unix,
                servers,
            },
        );
    }
}

/// Merge approved servers into an existing McpConfig. Does not touch
/// hard-blocked entries. Returns names that were newly inserted.
pub fn merge_approved_into_config(
    config: &mut McpConfig,
    approved: &[(String, McpServerConfig, ImportCandidate)],
) -> Vec<String> {
    let mut inserted = Vec::new();
    for (name, server, _) in approved {
        if config.servers.contains_key(name) {
            continue;
        }
        // Defense in depth: never insert disabled servers.
        if !server.is_enabled() {
            continue;
        }
        config.servers.insert(name.clone(), server.clone());
        inserted.push(name.clone());
    }
    inserted
}

/// Human-readable provenance block for the selector / status panel.
pub fn format_candidates_for_display(candidates: &[ImportCandidate]) -> String {
    if candidates.is_empty() {
        return "No external MCP sources found (or all already decided for current content)."
            .to_string();
    }
    let mut lines = vec![
        "External MCP import candidates (nothing is installed until you approve):".to_string(),
        String::new(),
    ];
    for (idx, c) in candidates.iter().enumerate() {
        let status = if c.hard_blocked { "BLOCKED" } else { "pending" };
        lines.push(format!(
            "  {}. [{}] {} — provenance: {} ({})",
            idx + 1,
            status,
            c.summary,
            c.source_kind.as_str(),
            c.source_path.display()
        ));
        lines.push(format!(
            "     content_hash: {}",
            &c.content_hash[..12.min(c.content_hash.len())]
        ));
        if let Some(reason) = &c.block_reason {
            lines.push(format!("     {reason}"));
        }
    }
    lines.push(String::new());
    lines.push(
        "Approve one: /mcp import approve <name> · Decline source: /mcp import decline <name> · List: /mcp import"
            .to_string(),
    );
    lines.join("\n")
}

#[cfg(test)]
mod tests {
    use super::*;
    use tempfile::tempdir;

    fn write_claude_json(dir: &Path, body: &str) -> PathBuf {
        let path = dir.join(".claude.json");
        fs::write(&path, body).unwrap();
        path
    }

    #[test]
    fn disabled_imported_server_never_merges() {
        let dir = tempdir().unwrap();
        let body = r#"{
            "mcpServers": {
                "ok": { "command": "npx", "args": ["-y", "good"], "enabled": true },
                "blocked": { "command": "npx", "args": ["-y", "bad"], "enabled": false }
            }
        }"#;
        write_claude_json(dir.path(), body);
        let candidates = discover_external_sources(dir.path(), dir.path(), &[]);
        assert_eq!(candidates.len(), 2);
        let blocked = candidates.iter().find(|c| c.name == "blocked").unwrap();
        assert!(blocked.hard_blocked);

        let mut decisions = HashMap::new();
        decisions.insert("ok".into(), ImportDecision::Approve);
        decisions.insert("blocked".into(), ImportDecision::Approve);
        let approved = apply_approved(&candidates, &decisions);
        assert_eq!(approved.len(), 1);
        assert_eq!(approved[0].0, "ok");

        let mut config = McpConfig::default();
        let inserted = merge_approved_into_config(&mut config, &approved);
        assert_eq!(inserted, vec!["ok".to_string()]);
        assert!(!config.servers.contains_key("blocked"));
        assert!(config.servers["ok"].is_enabled());
    }

    #[test]
    fn declined_consent_skips_reprompt_until_hash_changes() {
        let dir = tempdir().unwrap();
        let path = write_claude_json(
            dir.path(),
            r#"{"mcpServers":{"x":{"command":"echo","enabled":true}}}"#,
        );
        let candidates = discover_from_json_file(&path, ExternalMcpSourceKind::ClaudeJson);
        let mut store = ImportConsentStore::default();
        let mut decisions = HashMap::new();
        decisions.insert("x".into(), ImportDecision::Decline);
        record_decisions(&mut store, &candidates, &decisions, 1);
        let needing = candidates_needing_consent(&candidates, &store);
        assert!(needing.is_empty(), "declined should not re-prompt");

        // Content change → new hash → re-prompt.
        fs::write(
            &path,
            r#"{"mcpServers":{"x":{"command":"echo","args":["changed"],"enabled":true}}}"#,
        )
        .unwrap();
        let refreshed = discover_from_json_file(&path, ExternalMcpSourceKind::ClaudeJson);
        let needing = candidates_needing_consent(&refreshed, &store);
        assert_eq!(needing.len(), 1);
    }

    #[test]
    fn provenance_display_includes_source_and_hash() {
        let dir = tempdir().unwrap();
        write_claude_json(
            dir.path(),
            r#"{"mcpServers":{"hf":{"url":"https://example.com/mcp","enabled":true}}}"#,
        );
        let candidates = discover_external_sources(dir.path(), dir.path(), &[]);
        let text = format_candidates_for_display(&candidates);
        assert!(text.contains("provenance:"));
        assert!(text.contains("claude.json"));
        assert!(text.contains("content_hash:"));
        assert!(text.contains("nothing is installed until you approve"));
    }

    #[test]
    fn project_mcp_json_and_marketplace_are_discovered() {
        let home = tempdir().unwrap();
        let workspace = tempdir().unwrap();
        fs::write(
            workspace.path().join(".mcp.json"),
            r#"{"mcpServers":{"team":{"command":"uvx","args":["team-mcp"]}}}"#,
        )
        .unwrap();
        let market = home.path().join("market.json");
        fs::write(
            &market,
            r#"{"servers":{"shop":{"url":"https://market.example/mcp"}}}"#,
        )
        .unwrap();
        let candidates = discover_external_sources(home.path(), workspace.path(), &[market]);
        let names: Vec<_> = candidates.iter().map(|c| c.name.as_str()).collect();
        assert!(names.contains(&"team"));
        assert!(names.contains(&"shop"));
    }
}
