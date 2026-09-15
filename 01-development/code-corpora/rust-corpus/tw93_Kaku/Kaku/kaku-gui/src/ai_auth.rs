//! OAuth token management for AI providers that use OAuth instead of API keys.
//!
//! Copilot: exchanges a GitHub OAuth token (set by the TUI device-code flow)
//! for a short-lived Copilot API token, caching it in copilot_auth.json.
//!
//! Codex: reads the access token written by the Codex CLI into ~/.codex/auth.json.

use anyhow::{Context, Result};
use std::path::{Path, PathBuf};
use std::time::{SystemTime, UNIX_EPOCH};

const COPILOT_TOKEN_URL: &str = "https://api.github.com/copilot_internal/v2/token";

// ─── Token file paths ─────────────────────────────────────────────────────────

pub fn copilot_auth_file_path() -> Option<PathBuf> {
    let user_config_path = config::user_config_path();
    let config_dir = user_config_path.parent()?;
    Some(config_dir.join("copilot_auth.json"))
}

pub(crate) fn codex_home_dir() -> PathBuf {
    kaku_ai_utils::codex_home_dir(&config::HOME_DIR)
}

fn codex_auth_file_path() -> PathBuf {
    codex_home_dir().join("auth.json")
}

// ─── Copilot auth ─────────────────────────────────────────────────────────────

#[derive(serde::Serialize, serde::Deserialize, Default, Clone)]
struct CopilotAuthFile {
    pub github_token: String,
    #[serde(default)]
    pub copilot_token: String,
    /// Unix seconds when the cached Copilot token expires.
    #[serde(default)]
    pub copilot_expires_at: u64,
}

fn load_copilot_auth() -> Option<CopilotAuthFile> {
    let path = copilot_auth_file_path()?;
    let raw = std::fs::read_to_string(&path)
        .map_err(|e| log::debug!("copilot auth read failed: {e}"))
        .ok()?;
    serde_json::from_str(&raw)
        .map_err(|e| log::debug!("copilot auth parse failed: {e}"))
        .ok()
}

fn save_copilot_auth(auth: &CopilotAuthFile) -> Result<()> {
    let path = copilot_auth_file_path()
        .ok_or_else(|| anyhow::anyhow!("cannot determine copilot auth path"))?;
    let json = serde_json::to_vec_pretty(auth).context("serialize copilot auth")?;
    write_secret_file(&path, &json).with_context(|| format!("write {}", path.display()))?;
    Ok(())
}

#[cfg(unix)]
fn write_secret_file(path: &Path, bytes: &[u8]) -> Result<()> {
    use std::io::Write;
    use std::os::unix::fs::{OpenOptionsExt, PermissionsExt};

    if let Some(parent) = path.parent() {
        std::fs::create_dir_all(parent).with_context(|| format!("create {}", parent.display()))?;
    }

    let mut file = std::fs::OpenOptions::new()
        .create(true)
        .truncate(true)
        .write(true)
        .mode(0o600)
        .open(path)?;
    file.write_all(bytes)?;
    std::fs::set_permissions(path, std::fs::Permissions::from_mode(0o600))?;
    Ok(())
}

#[cfg(not(unix))]
fn write_secret_file(path: &Path, bytes: &[u8]) -> Result<()> {
    if let Some(parent) = path.parent() {
        std::fs::create_dir_all(parent).with_context(|| format!("create {}", parent.display()))?;
    }
    std::fs::write(path, bytes)?;
    Ok(())
}

fn now_unix_secs() -> u64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_secs()
}

/// Returns a valid Copilot API token, exchanging/refreshing via the GitHub token
/// stored in copilot_auth.json when the cached token is expired or missing.
pub fn get_copilot_token(client: &reqwest::blocking::Client) -> Result<String> {
    let mut auth = load_copilot_auth().ok_or_else(|| {
        anyhow::anyhow!(
            "Copilot: not logged in. Open `kaku ai` and select Copilot, then press Enter on \
             the GitHub Auth field to authenticate."
        )
    })?;

    if auth.github_token.trim().is_empty() {
        anyhow::bail!("Copilot: GitHub token missing. Open `kaku ai` and authenticate via GitHub.");
    }

    // Refresh 60 seconds before expiry so tokens don't expire mid-request.
    let needs_refresh =
        auth.copilot_token.is_empty() || now_unix_secs() + 60 >= auth.copilot_expires_at;

    if needs_refresh {
        let resp = client
            .get(COPILOT_TOKEN_URL)
            .header(
                "Authorization",
                format!("Bearer {}", auth.github_token.trim()),
            )
            .header("Accept", "application/json")
            .header("User-Agent", "kaku/1.0")
            .header("Editor-Version", "vscode/1.110.1")
            .header("Editor-Plugin-Version", "copilot-chat/0.38.2")
            .header("Copilot-Integration-Id", "vscode-chat")
            .send()
            .context("fetch Copilot token from GitHub")?;

        if !resp.status().is_success() {
            let status = resp.status();
            let body = resp.text().unwrap_or_default();
            anyhow::bail!("Copilot token refresh failed ({}): {}", status, body);
        }

        let data: serde_json::Value = resp.json().context("parse Copilot token response")?;
        let token = data["token"]
            .as_str()
            .ok_or_else(|| anyhow::anyhow!("missing `token` field in Copilot token response"))?
            .to_string();

        // expires_at may be ISO 8601 (string) or Unix seconds (number).
        let expires_at = if let Some(secs) = data["expires_at"].as_u64() {
            secs
        } else if let Some(s) = data["expires_at"].as_str() {
            parse_iso8601_to_unix(s).unwrap_or_else(|| now_unix_secs() + 1500)
        } else {
            now_unix_secs() + 1500 // fallback: 25 minutes
        };

        auth.copilot_token = token;
        auth.copilot_expires_at = expires_at;

        if let Err(e) = save_copilot_auth(&auth) {
            log::warn!("Failed to persist refreshed Copilot token: {e}");
        }
    }

    Ok(auth.copilot_token.clone())
}

/// Returns true when copilot_auth.json exists and has a GitHub token.
#[allow(dead_code)]
pub fn copilot_is_authenticated() -> bool {
    load_copilot_auth().is_some_and(|auth| !auth.github_token.trim().is_empty())
}

/// RFC 3339 / ISO 8601 timestamp -> Unix seconds. Used for the GitHub Copilot
/// token expiry. Returns None if the string is malformed or pre-1970.
fn parse_iso8601_to_unix(s: &str) -> Option<u64> {
    let ts = chrono::DateTime::parse_from_rfc3339(s.trim())
        .ok()?
        .timestamp();
    if ts < 0 {
        None
    } else {
        Some(ts as u64)
    }
}

// ─── Codex auth ───────────────────────────────────────────────────────────────

/// Codex CLI's public OAuth client id (same value Codex itself uses).
const CODEX_CLIENT_ID: &str = "app_EMoamEEZ73f0CkXaXp7hrann";
const CODEX_OAUTH_TOKEN_URL: &str = "https://auth.openai.com/oauth/token";

/// Codex OAuth credentials read from ~/.codex/auth.json.
#[derive(Clone)]
pub struct CodexAuth {
    pub access_token: String,
    /// ChatGPT account id, sent as the `chatgpt-account-id` header on the Codex
    /// responses backend. Parsed from the JWT id_token; None when unavailable.
    pub account_id: Option<String>,
}

fn read_codex_auth_json() -> Option<serde_json::Value> {
    let raw = std::fs::read_to_string(codex_auth_file_path())
        .map_err(|e| log::debug!("codex auth read failed: {e}"))
        .ok()?;
    serde_json::from_str(&raw)
        .map_err(|e| log::debug!("codex auth parse failed: {e}"))
        .ok()
}

/// Codex auth.json has two observed shapes: {"tokens":{"access_token":"..."}}
/// and {"access_token":"..."}.
fn codex_access_token_from(v: &serde_json::Value) -> Option<String> {
    v.get("tokens")
        .and_then(|t| t.get("access_token"))
        .or_else(|| v.get("access_token"))
        .and_then(|t| t.as_str())
        .filter(|t| !t.is_empty())
        .map(String::from)
}

/// Extracts the ChatGPT account id. Prefers a directly-stored `tokens.account_id`,
/// otherwise decodes the JWT id_token claims (`chatgpt_account_id`, either
/// top-level or nested under the `https://api.openai.com/auth` namespace).
fn codex_account_id_from(v: &serde_json::Value) -> Option<String> {
    if let Some(id) = v
        .get("tokens")
        .and_then(|t| t.get("account_id"))
        .and_then(|t| t.as_str())
        .filter(|s| !s.is_empty())
    {
        return Some(id.to_string());
    }

    let id_token = v
        .get("tokens")
        .and_then(|t| t.get("id_token"))
        .or_else(|| v.get("id_token"))
        .and_then(|t| t.as_str())?;
    codex_account_id_from_id_token(id_token)
}

fn codex_account_id_from_id_token(id_token: &str) -> Option<String> {
    let claims = decode_jwt_claims(id_token)?;
    claims
        .get("chatgpt_account_id")
        .or_else(|| {
            claims
                .get("https://api.openai.com/auth")
                .and_then(|a| a.get("chatgpt_account_id"))
        })
        .and_then(|x| x.as_str())
        .filter(|s| !s.is_empty())
        .map(String::from)
}

/// Decodes the claims (payload) segment of a JWT. Does not verify the signature
/// (we only read the account id, and the token is trusted from Codex's own file).
fn decode_jwt_claims(jwt: &str) -> Option<serde_json::Value> {
    use base64::Engine;
    let payload = jwt.split('.').nth(1)?;
    let bytes = base64::engine::general_purpose::URL_SAFE_NO_PAD
        .decode(payload)
        .ok()?;
    serde_json::from_slice(&bytes).ok()
}

/// Reads the Codex CLI access token (and ChatGPT account id) from
/// ~/.codex/auth.json. Codex writes these after `codex` login; Kaku reads them
/// to call the Codex backend on the user's behalf.
pub fn read_codex_auth() -> Option<CodexAuth> {
    let v = read_codex_auth_json()?;
    codex_auth_from_value(&v)
}

pub(crate) fn codex_auth_from_value(v: &serde_json::Value) -> Option<CodexAuth> {
    let access_token = codex_access_token_from(v)?;
    let account_id = codex_account_id_from(v);
    Some(CodexAuth {
        access_token,
        account_id,
    })
}

/// Backward-compatible accessor for just the access token.
pub fn read_codex_access_token() -> Option<String> {
    read_codex_auth().map(|a| a.access_token)
}

fn codex_refresh_token_from(v: &serde_json::Value) -> Option<&str> {
    v.get("tokens")
        .and_then(|t| t.get("refresh_token"))
        .or_else(|| v.get("refresh_token"))
        .and_then(|t| t.as_str())
        .filter(|t| !t.is_empty())
}

fn json_string_field(v: &serde_json::Value, key: &str) -> Option<String> {
    v.get(key)
        .and_then(|t| t.as_str())
        .map(str::trim)
        .filter(|t| !t.is_empty())
        .map(String::from)
}

fn set_codex_auth_token_field(
    auth: &mut serde_json::Value,
    key: &str,
    value: String,
) -> Result<()> {
    let root = auth
        .as_object_mut()
        .ok_or_else(|| anyhow::anyhow!("Codex auth.json must be a JSON object"))?;

    if root.contains_key("tokens") {
        if !root.get("tokens").is_some_and(serde_json::Value::is_object) {
            root.insert("tokens".to_string(), serde_json::json!({}));
        }
        let tokens = root
            .get_mut("tokens")
            .and_then(|tokens| tokens.as_object_mut())
            .ok_or_else(|| anyhow::anyhow!("Codex auth.json tokens must be a JSON object"))?;
        tokens.insert(key.to_string(), serde_json::Value::String(value));
    } else {
        root.insert(key.to_string(), serde_json::Value::String(value));
    }

    Ok(())
}

fn merge_codex_refresh_response(
    mut auth_json: serde_json::Value,
    refresh: &serde_json::Value,
) -> Result<(serde_json::Value, CodexAuth)> {
    let access_token = json_string_field(refresh, "access_token")
        .ok_or_else(|| anyhow::anyhow!("Codex refresh response missing access_token"))?;

    set_codex_auth_token_field(&mut auth_json, "access_token", access_token.clone())?;
    for key in ["refresh_token", "id_token"] {
        if let Some(value) = json_string_field(refresh, key) {
            set_codex_auth_token_field(&mut auth_json, key, value)?;
        }
    }

    let refreshed_account_id = json_string_field(refresh, "account_id").or_else(|| {
        json_string_field(refresh, "id_token")
            .and_then(|id_token| codex_account_id_from_id_token(&id_token))
    });
    if let Some(account_id) = refreshed_account_id {
        set_codex_auth_token_field(&mut auth_json, "account_id", account_id)?;
    }

    auth_json
        .as_object_mut()
        .ok_or_else(|| anyhow::anyhow!("Codex auth.json must be a JSON object"))?
        .insert(
            "last_refresh".to_string(),
            serde_json::Value::String(chrono::Utc::now().to_rfc3339()),
        );

    let account_id = codex_account_id_from(&auth_json);
    Ok((
        auth_json,
        CodexAuth {
            access_token,
            account_id,
        },
    ))
}

/// Exchanges the refresh_token in ~/.codex/auth.json for fresh Codex OAuth
/// credentials, then preserves Codex's auth file shape while updating the
/// returned token fields. This keeps the official login cache in sync when
/// refresh tokens rotate.
pub fn refresh_codex_auth(client: &reqwest::blocking::Client) -> Result<CodexAuth> {
    let original =
        read_codex_auth_json().ok_or_else(|| anyhow::anyhow!("Codex: auth.json missing"))?;
    let refresh_token = codex_refresh_token_from(&original)
        .ok_or_else(|| anyhow::anyhow!("Codex: no refresh_token; run `codex` to re-login"))?
        .to_string();

    let resp = client
        .post(CODEX_OAUTH_TOKEN_URL)
        .header("Content-Type", "application/x-www-form-urlencoded")
        .header("User-Agent", "codex_cli_rs")
        .form(&[
            ("grant_type", "refresh_token"),
            ("refresh_token", refresh_token.as_str()),
            ("client_id", CODEX_CLIENT_ID),
            ("scope", "openid profile email"),
        ])
        .send()
        .context("refresh Codex token")?;

    if !resp.status().is_success() {
        let status = resp.status();
        let body = resp.text().unwrap_or_default();
        anyhow::bail!("Codex token refresh failed ({status}): {body}");
    }

    let data: serde_json::Value = resp.json().context("parse Codex refresh response")?;
    let current = read_codex_auth_json().unwrap_or(original);
    let (updated, auth) = merge_codex_refresh_response(current, &data)?;
    let path = codex_auth_file_path();
    let json = serde_json::to_vec_pretty(&updated).context("serialize Codex auth")?;
    if let Err(e) =
        write_secret_file(&path, &json).with_context(|| format!("write {}", path.display()))
    {
        log::warn!("Failed to persist refreshed Codex token: {e}");
    }
    Ok(auth)
}

/// Backward-compatible accessor for just the refreshed access token.
#[allow(dead_code)]
pub fn refresh_codex_access_token(client: &reqwest::blocking::Client) -> Result<String> {
    refresh_codex_auth(client).map(|auth| auth.access_token)
}

/// Returns true when the Codex CLI auth file exists and has a token.
#[allow(dead_code)]
pub fn codex_is_authenticated() -> bool {
    read_codex_access_token().is_some()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parse_iso8601_zulu() {
        // 1970-01-01T00:00:00Z -> 0
        assert_eq!(parse_iso8601_to_unix("1970-01-01T00:00:00Z"), Some(0));
    }

    #[test]
    fn parse_iso8601_offset() {
        // 2024-01-01T00:00:00+00:00 -> some positive value
        let ts = parse_iso8601_to_unix("2024-01-01T00:00:00+00:00");
        assert!(ts.is_some());
        assert!(ts.unwrap() > 1_700_000_000);
    }

    #[test]
    fn parse_iso8601_negative_offset() {
        // 2024-01-01T00:00:00-05:00 == 2024-01-01T05:00:00Z
        let with_offset = parse_iso8601_to_unix("2024-01-01T00:00:00-05:00").unwrap();
        let utc = parse_iso8601_to_unix("2024-01-01T05:00:00Z").unwrap();
        assert_eq!(with_offset, utc);
    }

    #[test]
    fn parse_iso8601_invalid() {
        assert_eq!(parse_iso8601_to_unix("not a date"), None);
        assert_eq!(parse_iso8601_to_unix(""), None);
    }

    fn fake_jwt(payload: serde_json::Value) -> String {
        use base64::Engine;
        let enc = |b: &[u8]| base64::engine::general_purpose::URL_SAFE_NO_PAD.encode(b);
        let header = enc(br#"{"alg":"none"}"#);
        let body = enc(serde_json::to_vec(&payload).unwrap().as_slice());
        format!("{header}.{body}.sig")
    }

    #[test]
    fn codex_access_token_both_shapes() {
        let nested = serde_json::json!({ "tokens": { "access_token": "tok-a" } });
        assert_eq!(codex_access_token_from(&nested).as_deref(), Some("tok-a"));
        let flat = serde_json::json!({ "access_token": "tok-b" });
        assert_eq!(codex_access_token_from(&flat).as_deref(), Some("tok-b"));
        let empty = serde_json::json!({ "tokens": { "access_token": "" } });
        assert_eq!(codex_access_token_from(&empty), None);
    }

    #[test]
    fn codex_account_id_direct_field_wins() {
        let v = serde_json::json!({ "tokens": { "account_id": "acc-direct" } });
        assert_eq!(codex_account_id_from(&v).as_deref(), Some("acc-direct"));
    }

    #[test]
    fn codex_account_id_from_jwt_top_level() {
        let id_token = fake_jwt(serde_json::json!({ "chatgpt_account_id": "acc-jwt" }));
        let v = serde_json::json!({ "tokens": { "id_token": id_token } });
        assert_eq!(codex_account_id_from(&v).as_deref(), Some("acc-jwt"));
    }

    #[test]
    fn codex_account_id_from_jwt_namespaced() {
        let id_token = fake_jwt(serde_json::json!({
            "https://api.openai.com/auth": { "chatgpt_account_id": "acc-ns" }
        }));
        let v = serde_json::json!({ "tokens": { "id_token": id_token } });
        assert_eq!(codex_account_id_from(&v).as_deref(), Some("acc-ns"));
    }

    #[test]
    fn codex_account_id_missing_returns_none() {
        let id_token = fake_jwt(serde_json::json!({ "email": "x@y.z" }));
        let v = serde_json::json!({ "tokens": { "id_token": id_token } });
        assert_eq!(codex_account_id_from(&v), None);
        let no_token = serde_json::json!({ "tokens": { "access_token": "t" } });
        assert_eq!(codex_account_id_from(&no_token), None);
    }

    #[test]
    fn codex_refresh_merge_preserves_nested_auth_and_rotates_refresh() {
        let id_token = fake_jwt(serde_json::json!({
            "https://api.openai.com/auth": { "chatgpt_account_id": "acc-new" }
        }));
        let existing = serde_json::json!({
            "OPENAI_API_KEY": "keep-api-key",
            "auth_mode": "chatgpt",
            "last_refresh": "old",
            "tokens": {
                "access_token": "old-access",
                "refresh_token": "old-refresh",
                "id_token": "old-id-token",
                "account_id": "acc-old",
                "custom_token_field": "keep-token"
            },
            "custom_root_field": true
        });
        let refresh = serde_json::json!({
            "access_token": "new-access",
            "refresh_token": "new-refresh",
            "id_token": id_token
        });

        let (updated, auth) = merge_codex_refresh_response(existing, &refresh).unwrap();

        assert_eq!(auth.access_token, "new-access");
        assert_eq!(auth.account_id.as_deref(), Some("acc-new"));
        assert_eq!(updated["OPENAI_API_KEY"], "keep-api-key");
        assert_eq!(updated["auth_mode"], "chatgpt");
        assert_eq!(updated["custom_root_field"], true);
        assert_eq!(updated["tokens"]["access_token"], "new-access");
        assert_eq!(updated["tokens"]["refresh_token"], "new-refresh");
        assert_eq!(updated["tokens"]["account_id"], "acc-new");
        assert_eq!(updated["tokens"]["custom_token_field"], "keep-token");
        assert_ne!(updated["last_refresh"], "old");
        assert!(updated["last_refresh"].as_str().unwrap().contains('T'));
    }

    #[test]
    fn codex_refresh_merge_preserves_flat_auth_shape() {
        let existing = serde_json::json!({
            "access_token": "old-access",
            "refresh_token": "old-refresh",
            "custom_root_field": "keep"
        });
        let refresh = serde_json::json!({ "access_token": "new-access" });

        let (updated, auth) = merge_codex_refresh_response(existing, &refresh).unwrap();

        assert_eq!(auth.access_token, "new-access");
        assert_eq!(updated["access_token"], "new-access");
        assert_eq!(updated["refresh_token"], "old-refresh");
        assert_eq!(updated["custom_root_field"], "keep");
        assert!(updated.get("tokens").is_none());
        assert!(updated["last_refresh"].as_str().unwrap().contains('T'));
    }

    #[test]
    fn codex_refresh_merge_requires_access_token() {
        let existing = serde_json::json!({ "tokens": { "refresh_token": "old-refresh" } });
        let refresh = serde_json::json!({ "refresh_token": "new-refresh" });

        assert!(merge_codex_refresh_response(existing, &refresh).is_err());
    }
}
