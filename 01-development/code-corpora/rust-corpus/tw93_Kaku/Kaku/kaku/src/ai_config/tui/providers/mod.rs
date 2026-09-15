//! Provider configuration, usage scraping, and quota formatting for `kaku ai`.
//!
//! This submodule isolates the AI-tool usage subsystem (Antigravity sqlite /
//! protobuf probing, Codex/Claude/Kimi/Copilot/Gemini fetchers, OAuth
//! constants, cache I/O, and usage-snapshot formatting) from the App and
//! event-loop logic in the parent `tui` module. It is pure code-motion: no
//! behavior, strings, or control flow were changed during extraction.

pub(super) mod assistant;

use std::collections::{HashMap, HashSet};
use std::convert::TryFrom;
use std::ffi::OsStr;
use std::path::{Path, PathBuf};
use std::time::Duration;

use chrono::{DateTime, Utc};

use crate::assistant_config;
use crate::utils::write_atomic;

use super::{
    assistant_model_options_for_config_remote, codex_home_dir, decode_jwt_payload_with_debug,
    extract_antigravity_fields, kimi_credentials_path, read_codex_model_options,
    read_json_file_with_debug, FieldEntry, Tool, FOLLOW_CODEX_MODEL,
};
use assistant::{extract_kaku_assistant_fields_with_model_options, parse_kaku_assistant_config};

const USAGE_CACHE_TTL: Duration = Duration::from_secs(120);
const CLAUDE_OAUTH_CLIENT_ID: &str = "9d1c250a-e61b-44d9-88ed-5944d1962f5e";
const CLAUDE_OAUTH_TOKEN_URL: &str = "https://platform.claude.com/v1/oauth/token";
const KIMI_OAUTH_CLIENT_ID: &str = "17e5f671-d194-4dfb-9706-5516cb48c098";
const KIMI_OAUTH_TOKEN_URL: &str = "https://auth.kimi.com/api/oauth/token";
const KIMI_DEFAULT_BASE_URL: &str = "https://api.kimi.com/coding/v1";
const ANTIGRAVITY_LSP_PROCESS_NAME: &str = "language_server_macos";
const ANTIGRAVITY_CSRF_HEADER: &str = "x-codeium-csrf-token";
const ANTIGRAVITY_GET_USER_STATUS_PATH: &str =
    "/exa.language_server_pb.LanguageServerService/GetUserStatus";
const ANTIGRAVITY_GET_COMMAND_MODEL_CONFIGS_PATH: &str =
    "/exa.language_server_pb.LanguageServerService/GetCommandModelConfigs";
const ANTIGRAVITY_GET_UNLEASH_DATA_PATH: &str =
    "/exa.language_server_pb.LanguageServerService/GetUnleashData";
const ANTIGRAVITY_CONNECT_PROTOCOL_VERSION: &str = "1";

pub(super) struct CodexUsageSnapshot {
    pub(super) summary: Option<String>,
}

pub(super) struct ClaudeUsageSnapshot {
    pub(super) summary: Option<String>,
}

pub(super) struct CopilotUsageSnapshot {
    pub(super) summary: Option<String>,
}

pub(super) struct KimiUsageSnapshot {
    pub(super) summary: Option<String>,
}

pub(super) struct AntigravityUsageSnapshot {
    pub(super) summary: Option<String>,
    pub(super) selected_model_label: Option<String>,
}

#[derive(Clone, Debug, PartialEq)]
pub(super) struct AntigravityQuotaWindow {
    pub(super) model_id: Option<String>,
    pub(super) label: String,
    pub(super) remaining_fraction: f64,
    pub(super) reset_at: Option<String>,
}

pub(super) struct AntigravityProcessInfo {
    pub(super) pid: u32,
    pub(super) csrf_token: String,
    pub(super) extension_server_port: Option<u16>,
}

#[derive(Clone)]
pub(super) struct UsageSummaryUpdate {
    pub(super) tool: Tool,
    pub(super) summary: Option<String>,
    pub(super) fields: Option<Vec<FieldEntry>>,
}

pub(super) fn codex_usage_cache_path() -> PathBuf {
    config::HOME_DIR
        .join(".cache")
        .join("kaku")
        .join("codex_usage.json")
}

pub(super) fn claude_usage_cache_path() -> PathBuf {
    config::HOME_DIR
        .join(".cache")
        .join("kaku")
        .join("claude_usage.json")
}

pub(super) fn copilot_usage_cache_path() -> PathBuf {
    config::HOME_DIR
        .join(".cache")
        .join("kaku")
        .join("copilot_usage.json")
}

pub(super) fn kimi_usage_cache_path() -> PathBuf {
    config::HOME_DIR
        .join(".cache")
        .join("kaku")
        .join("kimi_usage.json")
}

pub(super) fn antigravity_usage_cache_path() -> PathBuf {
    config::HOME_DIR
        .join(".cache")
        .join("kaku")
        .join("antigravity_usage.json")
}

pub(super) fn assistant_models_cache_path() -> PathBuf {
    config::HOME_DIR
        .join(".cache")
        .join("kaku")
        .join("assistant_models.json")
}

pub(super) fn antigravity_app_bundle_path() -> PathBuf {
    PathBuf::from("/Applications/Antigravity.app")
}

pub(super) fn antigravity_state_db_path() -> PathBuf {
    config::HOME_DIR
        .join("Library")
        .join("Application Support")
        .join("Antigravity")
        .join("User")
        .join("globalStorage")
        .join("state.vscdb")
}

pub(super) fn read_codex_auth_info() -> Option<(String, String)> {
    let auth_path = codex_home_dir().join("auth.json");
    let auth_json = read_json_file_with_debug(&auth_path, "codex auth status")?;

    let access_token = auth_json
        .get("tokens")
        .and_then(|tokens| tokens.get("access_token"))
        .and_then(|value| value.as_str())
        .or_else(|| {
            auth_json
                .get("access_token")
                .and_then(|value| value.as_str())
        })?
        .to_string();

    let account_id = auth_json
        .get("tokens")
        .and_then(|tokens| tokens.get("account_id"))
        .and_then(|value| value.as_str())
        .or_else(|| auth_json.get("account_id").and_then(|value| value.as_str()))
        .map(|value| value.to_string())
        .or_else(|| {
            decode_jwt_payload_with_debug(&access_token, "codex auth status").and_then(|payload| {
                payload
                    .get("chatgpt_account_id")
                    .and_then(|value| value.as_str())
                    .map(|value| value.to_string())
            })
        })?;

    Some((access_token, account_id))
}

pub(super) fn read_sqlite_value_with_debug(
    path: &Path,
    query: &str,
    context: &str,
) -> Option<String> {
    use rusqlite::types::ValueRef;
    use rusqlite::{Connection, OpenFlags};

    if !path.exists() {
        log::debug!("{context}: sqlite db missing at {}", path.display());
        return None;
    }

    let conn = Connection::open_with_flags(path, OpenFlags::SQLITE_OPEN_READ_ONLY)
        .map_err(|err| log::debug!("{context}: sqlite open failed: {}", err))
        .ok()?;
    let mut stmt = conn
        .prepare(query)
        .map_err(|err| log::debug!("{context}: sqlite prepare failed: {}", err))
        .ok()?;
    let mut rows = stmt
        .query([])
        .map_err(|err| log::debug!("{context}: sqlite query failed: {}", err))
        .ok()?;
    let row = rows
        .next()
        .map_err(|err| log::debug!("{context}: sqlite row fetch failed: {}", err))
        .ok()??;
    let value = row
        .get_ref(0)
        .map_err(|err| log::debug!("{context}: sqlite value read failed: {}", err))
        .ok()?;

    let text = match value {
        ValueRef::Null => return None,
        ValueRef::Text(bytes) => std::str::from_utf8(bytes)
            .map_err(|err| log::debug!("{context}: sqlite text value is not utf-8: {}", err))
            .ok()?
            .to_string(),
        ValueRef::Blob(bytes) => String::from_utf8(bytes.to_vec())
            .map_err(|err| log::debug!("{context}: sqlite blob value is not utf-8: {}", err))
            .ok()?,
        ValueRef::Integer(value) => value.to_string(),
        ValueRef::Real(value) => value.to_string(),
    };

    let value = text.trim();
    if value.is_empty() {
        return None;
    }
    Some(value.to_string())
}

pub(super) fn decode_base64_standard_with_debug(raw: &str, context: &str) -> Option<Vec<u8>> {
    use base64::Engine;

    base64::engine::general_purpose::STANDARD
        .decode(raw)
        .map_err(|err| log::debug!("{context}: base64 decode failed: {}", err))
        .ok()
}

pub(super) fn read_protobuf_varint(bytes: &[u8], idx: &mut usize) -> Option<u64> {
    let mut shift = 0;
    let mut value = 0u64;
    while *idx < bytes.len() {
        let byte = bytes[*idx];
        *idx += 1;
        value |= u64::from(byte & 0x7f) << shift;
        if byte & 0x80 == 0 {
            return Some(value);
        }
        shift += 7;
        if shift > 63 {
            return None;
        }
    }
    None
}

pub(super) fn read_protobuf_bytes<'a>(bytes: &'a [u8], idx: &mut usize) -> Option<&'a [u8]> {
    let len = usize::try_from(read_protobuf_varint(bytes, idx)?).ok()?;
    let start = *idx;
    let end = start.checked_add(len)?;
    let slice = bytes.get(start..end)?;
    *idx = end;
    Some(slice)
}

pub(super) fn skip_protobuf_field(bytes: &[u8], idx: &mut usize, wire_type: u64) -> Option<()> {
    match wire_type {
        0 => {
            let _ = read_protobuf_varint(bytes, idx)?;
        }
        1 => {
            *idx = idx.checked_add(8)?;
        }
        2 => {
            let _ = read_protobuf_bytes(bytes, idx)?;
        }
        5 => {
            *idx = idx.checked_add(4)?;
        }
        _ => return None,
    }
    Some(())
}

pub(super) fn parse_antigravity_state_value_container(bytes: &[u8]) -> Option<String> {
    let mut idx = 0;
    while idx < bytes.len() {
        let tag = read_protobuf_varint(bytes, &mut idx)?;
        let field_number = tag >> 3;
        let wire_type = tag & 0x7;
        if field_number == 1 && wire_type == 2 {
            let value = read_protobuf_bytes(bytes, &mut idx)?;
            return String::from_utf8(value.to_vec()).ok();
        }
        skip_protobuf_field(bytes, &mut idx, wire_type)?;
    }
    None
}

pub(super) fn parse_antigravity_state_entry(bytes: &[u8]) -> Option<(String, String)> {
    let mut idx = 0;
    let mut key = None;
    let mut value = None;
    while idx < bytes.len() {
        let tag = read_protobuf_varint(bytes, &mut idx)?;
        let field_number = tag >> 3;
        let wire_type = tag & 0x7;
        match (field_number, wire_type) {
            (1, 2) => {
                let raw_key = read_protobuf_bytes(bytes, &mut idx)?;
                key = String::from_utf8(raw_key.to_vec()).ok();
            }
            (2, 2) => {
                let nested = read_protobuf_bytes(bytes, &mut idx)?;
                value = parse_antigravity_state_value_container(nested);
            }
            _ => skip_protobuf_field(bytes, &mut idx, wire_type)?,
        }
    }
    Some((key?, value?))
}

pub(super) fn parse_antigravity_unified_state(raw: &str) -> Option<Vec<(String, String)>> {
    let decoded = decode_base64_standard_with_debug(raw, "antigravity unified state")?;
    let mut idx = 0;
    let mut entries = Vec::new();
    while idx < decoded.len() {
        let tag = read_protobuf_varint(&decoded, &mut idx)?;
        let field_number = tag >> 3;
        let wire_type = tag & 0x7;
        match (field_number, wire_type) {
            (1, 2) => {
                let entry = read_protobuf_bytes(&decoded, &mut idx)?;
                if let Some(parsed) = parse_antigravity_state_entry(entry) {
                    entries.push(parsed);
                }
            }
            _ => skip_protobuf_field(&decoded, &mut idx, wire_type)?,
        }
    }
    Some(entries)
}

pub(super) fn decode_antigravity_int32_value(raw: &str) -> Option<i32> {
    let decoded = decode_base64_standard_with_debug(raw, "antigravity int32 state")?;
    let mut idx = 0;
    while idx < decoded.len() {
        let tag = read_protobuf_varint(&decoded, &mut idx)?;
        let field_number = tag >> 3;
        let wire_type = tag & 0x7;
        match (field_number, wire_type) {
            (2, 0) => {
                let value = read_protobuf_varint(&decoded, &mut idx)?;
                return i32::try_from(value).ok();
            }
            _ => skip_protobuf_field(&decoded, &mut idx, wire_type)?,
        }
    }
    None
}

pub(super) fn read_antigravity_storage_value(key: &str, context: &str) -> Option<String> {
    let escaped_key = key.replace('\'', "''");
    let query = format!("select value from ItemTable where key='{escaped_key}';");
    read_sqlite_value_with_debug(&antigravity_state_db_path(), &query, context)
}

pub(super) fn read_antigravity_auth_status() -> Option<serde_json::Value> {
    let raw = read_antigravity_storage_value("antigravityAuthStatus", "antigravity auth status")?;
    serde_json::from_str(&raw)
        .map_err(|err| log::debug!("antigravity auth status JSON parse failed: {}", err))
        .ok()
}

#[cfg(test)]
pub(super) fn extract_antigravity_printable_strings(bytes: &[u8]) -> Vec<String> {
    let mut current = Vec::new();
    let mut strings = Vec::new();
    for byte in bytes {
        if byte.is_ascii_graphic() || *byte == b' ' {
            current.push(*byte);
        } else if current.len() >= 6 {
            strings.push(String::from_utf8_lossy(&current).trim().to_string());
            current.clear();
        } else {
            current.clear();
        }
    }
    if current.len() >= 6 {
        strings.push(String::from_utf8_lossy(&current).trim().to_string());
    }
    strings
}

#[cfg(test)]
pub(super) fn extract_antigravity_plan_name(raw: &str) -> Option<String> {
    let decoded = decode_base64_standard_with_debug(raw, "antigravity user status")?;
    extract_antigravity_printable_strings(&decoded)
        .into_iter()
        .find(|candidate| candidate.starts_with("Google AI "))
}

pub(super) fn antigravity_arg_value<'a>(args: &'a [&'a str], key: &str) -> Option<&'a str> {
    args.iter()
        .position(|arg| *arg == key)
        .and_then(|idx| args.get(idx + 1).copied())
}

pub(super) fn curl_config_quote(value: &str) -> String {
    let mut quoted = String::with_capacity(value.len());
    for ch in value.chars() {
        match ch {
            '\\' => quoted.push_str("\\\\"),
            '"' => quoted.push_str("\\\""),
            '\n' => quoted.push_str("\\n"),
            '\r' => quoted.push_str("\\r"),
            '\t' => quoted.push_str("\\t"),
            _ => quoted.push(ch),
        }
    }
    quoted
}

pub(super) fn curl_config_header(name: &str, value: &str) -> String {
    format!("header = \"{}: {}\"\n", name, curl_config_quote(value))
}

pub(super) fn curl_config_data_urlencode(name: &str, value: &str) -> String {
    format!(
        "data-urlencode = \"{}={}\"\n",
        name,
        curl_config_quote(value)
    )
}

pub(super) fn parse_antigravity_process_info_line(line: &str) -> Option<AntigravityProcessInfo> {
    let mut parts = line.split_whitespace();
    let pid = parts.next()?.parse::<u32>().ok()?;
    let args = parts.collect::<Vec<_>>();

    if !args
        .iter()
        .any(|arg| arg.contains(ANTIGRAVITY_LSP_PROCESS_NAME))
    {
        return None;
    }

    // Restrict to the desktop app process to avoid collisions with unrelated servers.
    let is_antigravity_app = args
        .windows(2)
        .any(|pair| pair[0] == "--app_data_dir" && pair[1] == "antigravity");
    if !is_antigravity_app {
        return None;
    }

    let csrf_token = antigravity_arg_value(&args, "--csrf_token")?.to_string();
    let extension_server_port = antigravity_arg_value(&args, "--extension_server_port")
        .and_then(|value| value.parse::<u16>().ok());

    Some(AntigravityProcessInfo {
        pid,
        csrf_token,
        extension_server_port,
    })
}

pub(super) fn find_antigravity_process_info() -> Option<AntigravityProcessInfo> {
    let output = std::process::Command::new("/bin/ps")
        .args(["-ax", "-o", "pid=,command="])
        .output()
        .ok()?;
    if !output.status.success() {
        let stderr = String::from_utf8_lossy(&output.stderr);
        log::debug!(
            "antigravity process probe failed with status {}: {}",
            output.status,
            stderr.trim()
        );
        return None;
    }

    let stdout = String::from_utf8(output.stdout)
        .map_err(|err| log::debug!("antigravity process probe returned non-utf8: {}", err))
        .ok()?;
    stdout
        .lines()
        .filter_map(parse_antigravity_process_info_line)
        .max_by_key(|info| info.pid)
}

pub(super) fn parse_antigravity_listen_port(line: &str) -> Option<u16> {
    // Example line:
    // language_ 34643 tang ... TCP 127.0.0.1:56503 (LISTEN)
    let token = line.split_whitespace().find(|token| token.contains(':'))?;
    let (_, port) = token.rsplit_once(':')?;
    port.parse::<u16>().ok()
}

pub(super) fn read_antigravity_listen_ports(pid: u32) -> Vec<u16> {
    let output = match std::process::Command::new("/usr/sbin/lsof")
        .args(["-nP", "-iTCP", "-sTCP:LISTEN", "-a", "-p", &pid.to_string()])
        .output()
    {
        Ok(output) => output,
        Err(err) => {
            log::debug!("antigravity lsof probe failed to launch: {}", err);
            return Vec::new();
        }
    };
    if !output.status.success() {
        let stderr = String::from_utf8_lossy(&output.stderr);
        log::debug!(
            "antigravity lsof probe failed with status {}: {}",
            output.status,
            stderr.trim()
        );
        return Vec::new();
    }

    let stdout = match String::from_utf8(output.stdout) {
        Ok(stdout) => stdout,
        Err(err) => {
            log::debug!("antigravity lsof probe returned non-utf8: {}", err);
            return Vec::new();
        }
    };

    stdout
        .lines()
        .skip(1)
        .filter_map(parse_antigravity_listen_port)
        .collect()
}

pub(super) fn antigravity_lsp_curl_args(url: &str, payload: &str) -> Vec<String> {
    vec![
        "-k".into(),
        "-sS".into(),
        "--max-time".into(),
        "3".into(),
        "-X".into(),
        "POST".into(),
        url.into(),
        "-H".into(),
        "Content-Type: application/json".into(),
        "-H".into(),
        format!(
            "Connect-Protocol-Version: {}",
            ANTIGRAVITY_CONNECT_PROTOCOL_VERSION
        ),
        "--data".into(),
        payload.into(),
    ]
}

pub(super) fn post_antigravity_lsp_json(
    https_port: u16,
    csrf_token: &str,
    path: &str,
    payload: &serde_json::Value,
) -> Option<serde_json::Value> {
    let payload = serde_json::to_string(payload)
        .map_err(|err| log::debug!("antigravity payload serialize failed: {}", err))
        .ok()?;
    let url = format!("https://127.0.0.1:{https_port}{path}");
    let args = antigravity_lsp_curl_args(&url, &payload);
    let curl_config = curl_config_header(ANTIGRAVITY_CSRF_HEADER, csrf_token);

    let mut child = match std::process::Command::new("/usr/bin/curl")
        .args(&args)
        .args(["--config", "-"])
        .stdin(std::process::Stdio::piped())
        .stdout(std::process::Stdio::piped())
        .stderr(std::process::Stdio::piped())
        .spawn()
    {
        Ok(child) => child,
        Err(err) => {
            log::debug!("antigravity lsp curl spawn failed for {}: {}", path, err);
            return None;
        }
    };

    if let Some(mut stdin) = child.stdin.take() {
        use std::io::Write;
        let _ = stdin.write_all(curl_config.as_bytes());
    }

    let output = child
        .wait_with_output()
        .map_err(|err| log::debug!("antigravity lsp curl wait failed for {}: {}", path, err))
        .ok()?;
    if !output.status.success() {
        let stderr = String::from_utf8_lossy(&output.stderr);
        log::debug!(
            "antigravity lsp request failed for {} with status {}: {}",
            path,
            output.status,
            stderr.trim()
        );
        return None;
    }

    let raw = String::from_utf8(output.stdout)
        .map_err(|err| log::debug!("antigravity lsp response non-utf8 for {}: {}", path, err))
        .ok()?;
    let parsed = serde_json::from_str::<serde_json::Value>(&raw)
        .map_err(|err| {
            log::debug!(
                "antigravity lsp response JSON parse failed for {}: {}",
                path,
                err
            )
        })
        .ok()?;

    // Responses shaped like {"code":"unauthenticated",...} indicate auth mismatch.
    if parsed.get("code").is_some() && parsed.get("message").is_some() {
        let code = parsed
            .get("code")
            .and_then(|value| value.as_str())
            .unwrap_or_default();
        if matches!(code, "unauthenticated" | "permission_denied") {
            return None;
        }
    }

    Some(parsed)
}

pub(super) fn antigravity_unleash_probe(https_port: u16, csrf_token: &str) -> bool {
    post_antigravity_lsp_json(
        https_port,
        csrf_token,
        ANTIGRAVITY_GET_UNLEASH_DATA_PATH,
        &serde_json::json!({}),
    )
    .is_some()
}

pub(super) fn discover_antigravity_https_port(process: &AntigravityProcessInfo) -> Option<u16> {
    let mut candidates = Vec::new();
    if let Some(extension_port) = process.extension_server_port {
        if extension_port < u16::MAX {
            candidates.push(extension_port + 1);
        }
        candidates.push(extension_port);
    }
    candidates.extend(read_antigravity_listen_ports(process.pid));

    let mut seen = HashSet::new();
    candidates.retain(|port| seen.insert(*port));
    candidates
        .into_iter()
        .find(|port| antigravity_unleash_probe(*port, &process.csrf_token))
}

pub(super) fn fetch_antigravity_usage_json() -> Option<serde_json::Value> {
    let process = find_antigravity_process_info()?;
    let https_port = discover_antigravity_https_port(&process)?;

    let user_status = post_antigravity_lsp_json(
        https_port,
        &process.csrf_token,
        ANTIGRAVITY_GET_USER_STATUS_PATH,
        &serde_json::json!({}),
    )?;
    let command_model_configs = post_antigravity_lsp_json(
        https_port,
        &process.csrf_token,
        ANTIGRAVITY_GET_COMMAND_MODEL_CONFIGS_PATH,
        &serde_json::json!({}),
    )
    .unwrap_or(serde_json::Value::Null);

    Some(serde_json::json!({
        "fetched_at": Utc::now().to_rfc3339(),
        "user_status": user_status,
        "command_model_configs": command_model_configs,
    }))
}

pub(super) fn antigravity_value_as_f64(value: &serde_json::Value) -> Option<f64> {
    value
        .as_f64()
        .or_else(|| value.as_i64().map(|value| value as f64))
        .or_else(|| value.as_u64().map(|value| value as f64))
        .or_else(|| value.as_str()?.parse::<f64>().ok())
}

pub(super) fn collect_antigravity_model_name_map(
    value: &serde_json::Value,
    model_name_map: &mut HashMap<String, String>,
) {
    match value {
        serde_json::Value::Object(map) => {
            let model_id = map
                .get("modelId")
                .or_else(|| map.get("id"))
                .and_then(|value| value.as_str())
                .filter(|value| !value.is_empty());
            let display_name = map
                .get("modelDisplayName")
                .or_else(|| map.get("displayName"))
                .or_else(|| map.get("name"))
                .and_then(|value| value.as_str())
                .filter(|value| !value.is_empty());
            if let (Some(model_id), Some(display_name)) = (model_id, display_name) {
                model_name_map
                    .entry(model_id.to_string())
                    .or_insert_with(|| display_name.to_string());
            }
            for child in map.values() {
                collect_antigravity_model_name_map(child, model_name_map);
            }
        }
        serde_json::Value::Array(items) => {
            for item in items {
                collect_antigravity_model_name_map(item, model_name_map);
            }
        }
        _ => {}
    }
}

pub(super) fn antigravity_model_name_map(
    command_model_configs: &serde_json::Value,
) -> HashMap<String, String> {
    let mut model_name_map = HashMap::new();
    collect_antigravity_model_name_map(command_model_configs, &mut model_name_map);
    model_name_map
}

pub(super) fn antigravity_strip_parenthetical_suffix(label: &str) -> String {
    let trimmed = label.trim();
    if !trimmed.ends_with(')') {
        return trimmed.to_string();
    }
    let Some(idx) = trimmed.rfind(" (") else {
        return trimmed.to_string();
    };
    let candidate = trimmed[..idx].trim();
    if candidate.is_empty() {
        trimmed.to_string()
    } else {
        candidate.to_string()
    }
}

pub(super) fn antigravity_window_label(
    object: &serde_json::Map<String, serde_json::Value>,
    model_name_map: &HashMap<String, String>,
) -> Option<(Option<String>, String)> {
    let model_id = object
        .get("modelId")
        .or_else(|| object.get("id"))
        .or_else(|| {
            object
                .get("modelOrAlias")
                .and_then(|value| value.get("model"))
        })
        .and_then(|value| value.as_str())
        .filter(|value| !value.is_empty())
        .map(|value| value.to_string());

    let raw_label = object
        .get("modelDisplayName")
        .or_else(|| object.get("displayName"))
        .or_else(|| object.get("label"))
        .or_else(|| object.get("modelName"))
        .or_else(|| object.get("name"))
        .and_then(|value| value.as_str())
        .filter(|value| !value.is_empty())
        .map(|value| value.to_string())
        .or_else(|| model_id.clone())?;

    let resolved_from_id = model_id
        .as_ref()
        .and_then(|model_id| model_name_map.get(model_id))
        .cloned();
    let label = resolved_from_id
        .or_else(|| model_name_map.get(&raw_label).cloned())
        .unwrap_or_else(|| raw_label.clone());
    let label = antigravity_strip_parenthetical_suffix(&label);
    Some((model_id, label))
}

pub(super) fn antigravity_quota_window_from_object(
    object: &serde_json::Map<String, serde_json::Value>,
    model_name_map: &HashMap<String, String>,
) -> Option<AntigravityQuotaWindow> {
    let quota_info = object.get("quotaInfo").and_then(|value| value.as_object());

    let mut remaining_fraction = object
        .get("remainingFraction")
        .or_else(|| object.get("remaining_fraction"))
        .or_else(|| object.get("remaining"))
        .or_else(|| quota_info.and_then(|quota| quota.get("remainingFraction")))
        .or_else(|| quota_info.and_then(|quota| quota.get("remaining_fraction")))
        .or_else(|| quota_info.and_then(|quota| quota.get("remaining")))
        .and_then(antigravity_value_as_f64)?;
    if remaining_fraction > 1.0 && remaining_fraction <= 100.0 {
        remaining_fraction /= 100.0;
    }
    if !(0.0..=1.0).contains(&remaining_fraction) {
        remaining_fraction = remaining_fraction.clamp(0.0, 1.0);
    }

    let reset_at = object
        .get("resetAt")
        .or_else(|| object.get("resetsAt"))
        .or_else(|| object.get("resetTime"))
        .or_else(|| object.get("reset_at"))
        .or_else(|| quota_info.and_then(|quota| quota.get("resetAt")))
        .or_else(|| quota_info.and_then(|quota| quota.get("resetsAt")))
        .or_else(|| quota_info.and_then(|quota| quota.get("resetTime")))
        .or_else(|| quota_info.and_then(|quota| quota.get("reset_at")))
        .and_then(|value| {
            value
                .as_str()
                .map(|value| value.to_string())
                .or_else(|| value.as_i64().map(|value| value.to_string()))
                .or_else(|| value.as_u64().map(|value| value.to_string()))
        });

    let (model_id, label) = antigravity_window_label(object, model_name_map)?;
    Some(AntigravityQuotaWindow {
        model_id,
        label,
        remaining_fraction,
        reset_at,
    })
}

pub(super) fn collect_antigravity_quota_windows_from_value(
    value: &serde_json::Value,
    model_name_map: &HashMap<String, String>,
    windows: &mut Vec<AntigravityQuotaWindow>,
) {
    match value {
        serde_json::Value::Array(items) => {
            for item in items {
                if let Some(map) = item.as_object() {
                    if let Some(window) = antigravity_quota_window_from_object(map, model_name_map)
                    {
                        windows.push(window);
                    }
                }
            }
        }
        serde_json::Value::Object(map) => {
            if let Some(window) = antigravity_quota_window_from_object(map, model_name_map) {
                windows.push(window);
            }
        }
        _ => {}
    }
}

pub(super) fn collect_antigravity_quota_windows_from_paths(
    root: &serde_json::Value,
    paths: &[&str],
    model_name_map: &HashMap<String, String>,
    windows: &mut Vec<AntigravityQuotaWindow>,
) {
    for path in paths {
        if let Some(value) = root.pointer(path) {
            collect_antigravity_quota_windows_from_value(value, model_name_map, windows);
        }
    }
}

pub(super) fn antigravity_command_model_ids(
    command_model_configs: &serde_json::Value,
) -> Vec<String> {
    const ARRAY_PATHS: [&str; 2] = ["/clientModelConfigs", "/configs"];
    let mut model_ids = Vec::new();

    for path in ARRAY_PATHS {
        let Some(items) = command_model_configs
            .pointer(path)
            .and_then(|value| value.as_array())
        else {
            continue;
        };
        for item in items {
            let Some(map) = item.as_object() else {
                continue;
            };
            let model_id = map
                .get("modelId")
                .or_else(|| map.get("id"))
                .or_else(|| map.get("modelOrAlias").and_then(|value| value.get("model")))
                .and_then(|value| value.as_str())
                .filter(|value| !value.is_empty());
            if let Some(model_id) = model_id {
                model_ids.push(model_id.to_string());
            }
        }
    }

    model_ids.sort();
    model_ids.dedup();
    model_ids
}

pub(super) fn antigravity_format_reset_time(raw: &str) -> Option<String> {
    raw.parse::<i64>()
        .ok()
        .and_then(format_reset_time_from_epoch)
        .or_else(|| format_reset_time_from_iso(raw))
}

pub(super) fn antigravity_format_quota_value(window: &AntigravityQuotaWindow) -> String {
    let mut value = format!(
        "remain {}",
        format_percent_value((window.remaining_fraction * 100.0).clamp(0.0, 100.0))
    );
    if let Some(reset_in) = window
        .reset_at
        .as_deref()
        .and_then(antigravity_format_reset_time)
    {
        value.push_str(" · reset ");
        value.push_str(&reset_in);
    }
    value
}

pub(super) fn antigravity_selected_model_sentinel() -> Option<i32> {
    #[cfg(test)]
    {
        return None;
    }

    #[cfg(not(test))]
    let raw = read_antigravity_storage_value(
        "antigravityUnifiedStateSync.modelPreferences",
        "antigravity model preferences",
    )?;
    #[cfg(not(test))]
    let entries = parse_antigravity_unified_state(&raw)?;
    #[cfg(not(test))]
    entries
        .into_iter()
        .find(|(key, _)| key == "last_selected_agent_model_sentinel_key")
        .and_then(|(_, value)| decode_antigravity_int32_value(&value))
}

pub(super) fn antigravity_model_id_from_sentinel(
    sentinel: i32,
    model_ids: &[String],
) -> Option<String> {
    if sentinel <= 0 {
        return None;
    }

    let mut candidates = Vec::new();
    candidates.push(sentinel);
    if sentinel >= 1000 {
        candidates.push(sentinel - 1000);
    }
    if sentinel >= 100 {
        candidates.push(sentinel % 1000);
    }
    candidates.retain(|candidate| *candidate > 0);
    candidates.sort_unstable();
    candidates.dedup();

    for candidate in candidates {
        let exact = format!("MODEL_PLACEHOLDER_M{candidate}");
        if model_ids.iter().any(|model_id| model_id == &exact) {
            return Some(exact);
        }

        let suffix = format!("_M{candidate}");
        if let Some(found) = model_ids
            .iter()
            .find(|model_id| model_id.ends_with(&suffix))
            .cloned()
        {
            return Some(found);
        }
    }

    None
}

pub(super) fn antigravity_model_label_from_sentinel_value(value: i32) -> Option<&'static str> {
    // Last-resort fallback when live LSP fetch, cached usage data, and
    // model-id resolution all fail. These sentinel values come from
    // Antigravity's internal model enum and may need refreshing as the app
    // updates its bundled model list.
    match value {
        18 => Some("Gemini 3 Flash"),
        26 => Some("Claude Opus 4.6"),
        35 => Some("Claude Sonnet 4.6"),
        36 | 37 => Some("Gemini 3.1 Pro"),
        _ => None,
    }
}

pub(super) fn antigravity_fallback_selected_model_label() -> Option<String> {
    let sentinel = antigravity_selected_model_sentinel()?;
    let mut candidates = Vec::new();
    candidates.push(sentinel);
    if sentinel >= 1000 {
        candidates.push(sentinel - 1000);
    }
    if sentinel >= 100 {
        candidates.push(sentinel % 1000);
    }
    candidates.sort_unstable();
    candidates.dedup();

    candidates
        .into_iter()
        .find_map(|value| antigravity_model_label_from_sentinel_value(value).map(str::to_string))
}

pub(super) fn antigravity_selected_model_id(
    user_status: &serde_json::Value,
    command_model_configs: &serde_json::Value,
    model_ids: &[String],
) -> Option<String> {
    const COMMAND_MODEL_PATHS: [&str; 2] = [
        "/selectedModelConfig/modelOrAlias/model",
        "/commandModelConfig/modelOrAlias/model",
    ];
    if let Some(model_id) = COMMAND_MODEL_PATHS.iter().find_map(|path| {
        command_model_configs
            .pointer(path)
            .and_then(|value| value.as_str())
            .filter(|value| !value.is_empty())
            .map(|value| value.to_string())
    }) {
        return Some(model_id);
    }

    // defaultOverrideModelConfig tracks the current Antigravity model in live status.
    // A single entry in GetCommandModelConfigs can simply mean a command default,
    // so we only use that endpoint as a last-resort fallback.
    const PATHS: [&str; 4] = [
        "/userStatus/cascadeModelConfigData/defaultOverrideModelConfig/modelOrAlias/model",
        "/cascadeModelConfigData/defaultOverrideModelConfig/modelOrAlias/model",
        "/userStatus/defaultOverrideModelConfig/modelOrAlias/model",
        "/defaultOverrideModelConfig/modelOrAlias/model",
    ];
    if let Some(model_id) = PATHS.iter().find_map(|path| {
        user_status
            .pointer(path)
            .and_then(|value| value.as_str())
            .filter(|value| !value.is_empty())
            .map(|value| value.to_string())
    }) {
        return Some(model_id);
    }

    if let Some(sentinel) = antigravity_selected_model_sentinel() {
        if let Some(model_id) = antigravity_model_id_from_sentinel(sentinel, model_ids) {
            return Some(model_id);
        }
    }

    let command_model_ids = antigravity_command_model_ids(command_model_configs);
    if let [model_id] = command_model_ids.as_slice() {
        return Some(model_id.clone());
    }

    None
}

pub(super) fn parse_antigravity_usage_snapshot(
    data: &serde_json::Value,
) -> Option<AntigravityUsageSnapshot> {
    let user_status = data
        .get("user_status")
        .or_else(|| data.get("userStatus"))
        .unwrap_or(data);
    let command_model_configs = data
        .get("command_model_configs")
        .or_else(|| data.get("commandModelConfigs"))
        .unwrap_or(&serde_json::Value::Null);

    let model_name_map = antigravity_model_name_map(command_model_configs);
    let mut windows = Vec::new();
    collect_antigravity_quota_windows_from_paths(
        user_status,
        &[
            "/userStatus/cascadeModelConfigData/clientModelConfigs",
            "/cascadeModelConfigData/clientModelConfigs",
            "/clientModelConfigs",
        ],
        &model_name_map,
        &mut windows,
    );
    collect_antigravity_quota_windows_from_paths(
        command_model_configs,
        &["/clientModelConfigs", "/configs"],
        &model_name_map,
        &mut windows,
    );

    let mut deduped = HashMap::<String, AntigravityQuotaWindow>::new();
    let mut label_order = Vec::<String>::new();
    for window in windows {
        if !deduped.contains_key(&window.label) {
            label_order.push(window.label.clone());
        }
        deduped
            .entry(window.label.clone())
            .and_modify(|existing| {
                if window.remaining_fraction < existing.remaining_fraction {
                    *existing = window.clone();
                }
            })
            .or_insert(window);
    }

    let windows = label_order
        .into_iter()
        .filter_map(|label| deduped.remove(&label))
        .collect::<Vec<_>>();

    let mut model_ids = windows
        .iter()
        .filter_map(|window| window.model_id.clone())
        .collect::<Vec<_>>();
    model_ids.extend(model_name_map.keys().cloned());
    model_ids.sort();
    model_ids.dedup();

    let mut selected_model_label = None;
    let windows = if let Some(selected_model_id) =
        antigravity_selected_model_id(user_status, command_model_configs, &model_ids)
    {
        let selected_model_label_hint = model_name_map
            .get(&selected_model_id)
            .map(|label| antigravity_strip_parenthetical_suffix(label));
        let selected_windows = windows
            .iter()
            .filter(|window| {
                window.model_id.as_deref() == Some(selected_model_id.as_str())
                    || selected_model_label_hint
                        .as_deref()
                        .is_some_and(|label| window.label == label)
            })
            .cloned()
            .collect::<Vec<_>>();

        if !selected_windows.is_empty() {
            selected_model_label = selected_windows.first().map(|window| window.label.clone());
            selected_windows
        } else {
            windows
        }
    } else {
        windows
    };

    let selected_window = if selected_model_label.is_none() {
        match windows.first().cloned() {
            Some(window) => {
                selected_model_label = Some(window.label.clone());
                Some(window)
            }
            None => None,
        }
    } else {
        windows.first().cloned()
    };

    let summary = selected_window.as_ref().map(antigravity_format_quota_value);

    Some(AntigravityUsageSnapshot {
        summary,
        selected_model_label,
    })
}

pub(super) fn load_antigravity_usage_snapshot() -> Option<AntigravityUsageSnapshot> {
    let cache_path = antigravity_usage_cache_path();
    // Antigravity model selection can change out of band while Kaku is open,
    // so prefer a live local fetch and only fall back to cache when it fails.
    if let Some(live) = fetch_antigravity_usage_json() {
        write_json_cache(&cache_path, &live);
        if let Some(snapshot) = parse_antigravity_usage_snapshot(&live) {
            return Some(snapshot);
        }
    }

    if cache_path.exists() && usage_cache_is_fresh(&cache_path) {
        if let Some(cached) = load_usage_json_from_cache(&cache_path, "antigravity usage cache")
            .and_then(|value| parse_antigravity_usage_snapshot(&value))
        {
            return Some(cached);
        }
    }

    Some(antigravity_fallback_usage_snapshot())
}

pub(super) fn antigravity_fallback_usage_snapshot() -> AntigravityUsageSnapshot {
    AntigravityUsageSnapshot {
        summary: None,
        selected_model_label: antigravity_fallback_selected_model_label(),
    }
}

pub(super) fn load_cached_antigravity_usage_snapshot() -> AntigravityUsageSnapshot {
    let cache_path = antigravity_usage_cache_path();
    if cache_path.exists() && usage_cache_is_fresh(&cache_path) {
        if let Some(cached) = load_usage_json_from_cache(&cache_path, "antigravity usage cache")
            .and_then(|value| parse_antigravity_usage_snapshot(&value))
        {
            return cached;
        }
    }

    antigravity_fallback_usage_snapshot()
}

pub(super) fn format_duration_short(total_seconds: i64) -> Option<String> {
    if total_seconds <= 0 {
        return None;
    }

    let days = total_seconds / 86_400;
    let hours = (total_seconds % 86_400) / 3_600;
    let minutes = (total_seconds % 3_600) / 60;

    if days > 0 {
        Some(format!("{days}d{hours}h"))
    } else if hours > 0 {
        Some(format!("{hours}h{minutes}m"))
    } else {
        Some(format!("{minutes}m"))
    }
}

pub(super) fn format_reset_time_from_epoch(reset_at: i64) -> Option<String> {
    let now = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .ok()?
        .as_secs() as i64;
    format_duration_short(reset_at - now)
}

pub(super) fn format_reset_time_from_iso(reset_at: &str) -> Option<String> {
    let reset_at = DateTime::parse_from_rfc3339(reset_at).ok()?;
    let reset_at = reset_at.with_timezone(&Utc);
    format_duration_short((reset_at - Utc::now()).num_seconds())
}

pub(super) fn supports_remote_usage(tool: Tool) -> bool {
    matches!(
        tool,
        Tool::KakuAssistant
            | Tool::ClaudeCode
            | Tool::Codex
            | Tool::Kimi
            | Tool::Antigravity
            | Tool::Copilot
    )
}

pub(super) fn load_usage_update(tool: Tool) -> UsageSummaryUpdate {
    match tool {
        Tool::KakuAssistant => {
            let path = match assistant_config::ensure_assistant_toml_exists() {
                Ok(path) => path,
                Err(err) => {
                    return UsageSummaryUpdate {
                        tool,
                        summary: Some("Setup failed".into()),
                        fields: Some(vec![FieldEntry {
                            key: "error".into(),
                            value: err.to_string(),
                            options: vec![],
                            editable: false,
                        }]),
                    };
                }
            };
            let raw = std::fs::read_to_string(&path).unwrap_or_default();
            let cfg = parse_kaku_assistant_config(&raw);
            // Codex following mode uses the CLI model catalog rather than
            // assistant.toml's api_key/base_url model endpoint.
            let model_options = if cfg.auth_type() == "codex" {
                let mut options = read_codex_model_options();
                if !options.iter().any(|option| option == FOLLOW_CODEX_MODEL) {
                    options.insert(0, FOLLOW_CODEX_MODEL.to_string());
                }
                options
            } else {
                assistant_model_options_for_config_remote(&cfg)
            };
            UsageSummaryUpdate {
                tool,
                summary: None,
                fields: Some(extract_kaku_assistant_fields_with_model_options(
                    &raw,
                    model_options,
                )),
            }
        }
        Tool::Antigravity => {
            let snapshot = load_antigravity_usage_snapshot();
            UsageSummaryUpdate {
                tool,
                summary: snapshot
                    .as_ref()
                    .and_then(|snapshot| snapshot.summary.clone()),
                fields: Some(extract_antigravity_fields(snapshot.as_ref())),
            }
        }
        Tool::ClaudeCode => UsageSummaryUpdate {
            tool,
            summary: load_claude_usage_snapshot().and_then(|snapshot| snapshot.summary),
            fields: None,
        },
        Tool::Codex => UsageSummaryUpdate {
            tool,
            summary: load_codex_usage_snapshot().and_then(|snapshot| snapshot.summary),
            fields: None,
        },
        Tool::Kimi => UsageSummaryUpdate {
            tool,
            summary: load_kimi_usage_snapshot().and_then(|snapshot| snapshot.summary),
            fields: None,
        },
        Tool::Copilot => UsageSummaryUpdate {
            tool,
            summary: load_copilot_usage_snapshot().and_then(|snapshot| snapshot.summary),
            fields: None,
        },
        _ => UsageSummaryUpdate {
            tool,
            summary: None,
            fields: None,
        },
    }
}

pub(super) fn format_percent_value(percent: f64) -> String {
    if (percent.fract()).abs() < 0.05 {
        format!("{percent:.0}%")
    } else {
        format!("{percent:.1}%")
    }
}

pub(super) fn format_remaining_percent_value(used_percent: f64) -> String {
    format_percent_value((100.0 - used_percent).clamp(0.0, 100.0))
}

pub(super) fn format_remaining_window_value(
    label: &str,
    used_percent: f64,
    reset_in: Option<String>,
) -> String {
    let mut value = format!(
        "{label} remain {}",
        format_remaining_percent_value(used_percent)
    );
    if let Some(reset_in) = reset_in {
        value.push_str(" · reset ");
        value.push_str(&reset_in);
    }
    value
}

pub(super) fn format_remaining_count_value(value: f64) -> String {
    if (value.fract()).abs() < 0.05 {
        format!("{value:.0}")
    } else {
        format!("{value:.1}")
    }
}

pub(super) fn format_codex_usage_value(label: &str, window: &serde_json::Value) -> Option<String> {
    let used_percent = window.get("used_percent")?.as_f64()?;
    let reset_in = window
        .get("reset_at")
        .and_then(|value| value.as_i64())
        .and_then(format_reset_time_from_epoch);
    Some(format_remaining_window_value(label, used_percent, reset_in))
}

pub(super) fn parse_codex_usage_snapshot(data: &serde_json::Value) -> Option<CodexUsageSnapshot> {
    let rate_limit = data.get("rate_limit")?;
    let current_value = rate_limit
        .get("primary_window")
        .and_then(|window| format_codex_usage_value("5h", window));
    let weekly_value = rate_limit
        .get("secondary_window")
        .and_then(|window| format_codex_usage_value("7d", window));

    let summary = match (current_value, weekly_value) {
        (Some(current), Some(weekly)) => Some(format!("{current}  |  {weekly}")),
        (Some(current), None) => Some(current),
        (None, Some(weekly)) => Some(weekly),
        (None, None) => None,
    };

    summary.as_ref()?;
    Some(CodexUsageSnapshot { summary })
}

pub(super) fn usage_cache_is_fresh(path: &Path) -> bool {
    path.metadata()
        .and_then(|meta| meta.modified())
        .ok()
        .and_then(|modified| modified.elapsed().ok())
        .is_some_and(|elapsed| elapsed < USAGE_CACHE_TTL)
}

pub(super) fn write_json_cache(path: &Path, value: &serde_json::Value) {
    if let Some(parent) = path.parent() {
        if let Err(err) = config::create_user_owned_dirs(parent) {
            log::debug!("failed to create cache dir {}: {}", parent.display(), err);
            return;
        }
    }

    match serde_json::to_vec(value) {
        Ok(bytes) => {
            if let Err(err) = write_atomic(path, &bytes) {
                log::debug!("failed to write {}: {}", path.display(), err);
            }
        }
        Err(err) => log::debug!("failed to serialize {}: {}", path.display(), err),
    }
}

pub(super) fn run_curl_with_config(args: &[&str], curl_config: &str) -> Option<serde_json::Value> {
    let mut command = std::process::Command::new(OsStr::new("/usr/bin/curl"));
    command
        .args(args)
        .args(["--config", "-"])
        .stdin(std::process::Stdio::piped());

    let mut child = command
        .stdout(std::process::Stdio::piped())
        .stderr(std::process::Stdio::piped())
        .spawn()
        .ok()?;
    if let Some(mut stdin) = child.stdin.take() {
        use std::io::Write;
        let _ = stdin.write_all(curl_config.as_bytes());
    }

    let output = child
        .wait_with_output()
        .map_err(|err| log::debug!("curl wait failed: {}", err))
        .ok()?;
    if !output.status.success() {
        let stderr = String::from_utf8_lossy(&output.stderr);
        log::debug!(
            "curl failed with status {}: {}",
            output.status,
            stderr.trim()
        );
        return None;
    }

    let raw = String::from_utf8(output.stdout)
        .map_err(|err| log::debug!("curl returned non-utf8 stdout: {}", err))
        .ok()?;
    serde_json::from_str(&raw)
        .map_err(|err| log::debug!("failed to parse curl json: {}", err))
        .ok()
}

pub(super) fn fetch_codex_usage_json() -> Option<serde_json::Value> {
    let cache_path = codex_usage_cache_path();
    if cache_path.exists() && usage_cache_is_fresh(&cache_path) {
        return load_usage_json_from_cache(&cache_path, "codex usage cache");
    }

    let (access_token, account_id) = read_codex_auth_info()?;
    let authorization = format!("Bearer {access_token}");
    let curl_config = format!(
        "{}{}",
        curl_config_header("Authorization", &authorization),
        curl_config_header("ChatGPT-Account-Id", &account_id)
    );
    let live = run_curl_with_config(
        &[
            "-sS",
            "--max-time",
            "3",
            "-H",
            "Accept: application/json",
            "https://chatgpt.com/backend-api/wham/usage",
        ],
        &curl_config,
    );

    if let Some(value) = live {
        write_json_cache(&cache_path, &value);
        return Some(value);
    }

    load_usage_json_from_cache(&cache_path, "codex usage cache")
}

pub(super) fn load_codex_usage_snapshot() -> Option<CodexUsageSnapshot> {
    let data = fetch_codex_usage_json()?;
    parse_codex_usage_snapshot(&data)
}

pub(super) fn load_usage_json_from_cache(path: &Path, context: &str) -> Option<serde_json::Value> {
    read_json_file_with_debug(path, context)
}

pub(super) fn fetch_usage_json_with_cache<F>(
    path: PathBuf,
    context: &str,
    fetcher: F,
) -> Option<serde_json::Value>
where
    F: FnOnce() -> Option<serde_json::Value>,
{
    if path.exists() && usage_cache_is_fresh(&path) {
        return load_usage_json_from_cache(&path, context);
    }

    if let Some(value) = fetcher() {
        write_json_cache(&path, &value);
        return Some(value);
    }

    load_usage_json_from_cache(&path, context)
}

pub(super) fn read_claude_oauth_credentials() -> Option<serde_json::Value> {
    let output = std::process::Command::new("/usr/bin/security")
        .args([
            "find-generic-password",
            "-s",
            "Claude Code-credentials",
            "-w",
        ])
        .output()
        .ok()?;
    if !output.status.success() {
        let stderr = String::from_utf8_lossy(&output.stderr);
        log::debug!(
            "claude keychain probe failed with status {}: {}",
            output.status,
            stderr.trim()
        );
        return None;
    }

    let raw = String::from_utf8(output.stdout)
        .map_err(|err| log::debug!("claude keychain probe returned non-utf8 stdout: {}", err))
        .ok()?;
    serde_json::from_str::<serde_json::Value>(raw.trim())
        .map_err(|err| log::debug!("failed to parse claude keychain json: {}", err))
        .ok()
}

pub(super) fn read_claude_oauth_access_token() -> Option<String> {
    let parsed = read_claude_oauth_credentials()?;

    parsed
        .get("claudeAiOauth")
        .and_then(|value| value.get("accessToken"))
        .and_then(|value| value.as_str())
        .map(|value| value.to_string())
}

pub(super) fn read_claude_oauth_refresh_token() -> Option<String> {
    let parsed = read_claude_oauth_credentials()?;
    parsed
        .get("claudeAiOauth")
        .and_then(|value| value.get("refreshToken"))
        .and_then(|value| value.as_str())
        .map(|value| value.to_string())
}

pub(super) fn parse_claude_keychain_account(raw: &str) -> Option<String> {
    let marker = "\"acct\"<blob>=\"";
    raw.lines().find_map(|line| {
        let line = line.trim();
        let start = line.find(marker)? + marker.len();
        let end = line[start..].find('"')?;
        Some(line[start..start + end].to_string())
    })
}

pub(super) fn read_claude_keychain_account() -> Option<String> {
    let output = std::process::Command::new("/usr/bin/security")
        .args(["find-generic-password", "-s", "Claude Code-credentials"])
        .output()
        .ok()?;
    if !output.status.success() {
        let stderr = String::from_utf8_lossy(&output.stderr);
        log::debug!(
            "claude keychain account probe failed with status {}: {}",
            output.status,
            stderr.trim()
        );
        return None;
    }

    let raw = String::from_utf8(output.stdout)
        .map_err(|err| log::debug!("claude keychain account probe returned non-utf8: {}", err))
        .ok()?;
    parse_claude_keychain_account(&raw)
}

pub(super) fn write_claude_oauth_credentials(credentials: &serde_json::Value) -> Option<()> {
    let account = read_claude_keychain_account()?;
    let secret = serde_json::to_string(credentials)
        .map_err(|err| log::debug!("failed to serialize claude keychain json: {}", err))
        .ok()?;

    let output = std::process::Command::new("/usr/bin/security")
        .args([
            "add-generic-password",
            "-U",
            "-a",
            &account,
            "-s",
            "Claude Code-credentials",
            "-w",
            &secret,
        ])
        .output()
        .ok()?;
    if !output.status.success() {
        let stderr = String::from_utf8_lossy(&output.stderr);
        log::warn!(
            "failed to update claude keychain credentials: status {}: {}",
            output.status,
            stderr.trim()
        );
        return None;
    }

    Some(())
}

pub(super) fn refresh_claude_oauth_access_token() -> Option<String> {
    let current_credentials = read_claude_oauth_credentials()?;
    let refresh_token = read_claude_oauth_refresh_token()?;
    let client_id = format!("client_id={CLAUDE_OAUTH_CLIENT_ID}");
    let curl_config = curl_config_data_urlencode("refresh_token", &refresh_token);
    let refreshed = run_curl_with_config(
        &[
            "-sS",
            "--max-time",
            "5",
            "-X",
            "POST",
            CLAUDE_OAUTH_TOKEN_URL,
            "-H",
            "Content-Type: application/x-www-form-urlencoded",
            "--data-urlencode",
            "grant_type=refresh_token",
            "--data-urlencode",
            &client_id,
        ],
        &curl_config,
    )?;

    let access_token = refreshed
        .get("access_token")
        .and_then(|value| value.as_str())
        .map(|value| value.to_string())?;
    let rotated_refresh_token = refreshed
        .get("refresh_token")
        .and_then(|value| value.as_str())
        .unwrap_or(&refresh_token)
        .to_string();

    let mut updated_credentials = current_credentials;
    let oauth = updated_credentials
        .get_mut("claudeAiOauth")
        .and_then(|value| value.as_object_mut())?;
    oauth.insert(
        "accessToken".into(),
        serde_json::Value::String(access_token.clone()),
    );
    oauth.insert(
        "refreshToken".into(),
        serde_json::Value::String(rotated_refresh_token),
    );

    if let Some(expires_in) = refreshed.get("expires_in").and_then(|value| value.as_i64()) {
        let expires_at_ms = Utc::now().timestamp_millis() + expires_in * 1000;
        oauth.insert(
            "expiresAt".into(),
            serde_json::Value::Number(expires_at_ms.into()),
        );
    }

    if let Some(scope) = refreshed.get("scope").and_then(|value| value.as_str()) {
        let scopes = scope
            .split_whitespace()
            .map(|item| serde_json::Value::String(item.to_string()))
            .collect::<Vec<_>>();
        oauth.insert("scopes".into(), serde_json::Value::Array(scopes));
    }

    let _ = write_claude_oauth_credentials(&updated_credentials);
    Some(access_token)
}

pub(super) fn fetch_claude_usage_with_access_token(
    access_token: &str,
) -> Option<serde_json::Value> {
    let authorization = format!("Bearer {access_token}");
    let curl_config = curl_config_header("Authorization", &authorization);
    run_curl_with_config(
        &[
            "-sS",
            "--max-time",
            "3",
            "-H",
            "anthropic-beta: oauth-2025-04-20",
            "-H",
            "Accept: application/json",
            "-H",
            "Content-Type: application/json",
            "-H",
            "User-Agent: claude-code/2.0.27",
            "https://api.anthropic.com/api/oauth/usage",
        ],
        &curl_config,
    )
}

pub(super) fn fetch_claude_usage_json() -> Option<serde_json::Value> {
    let cache_path = claude_usage_cache_path();
    if cache_path.exists() && usage_cache_is_fresh(&cache_path) {
        if let Some(cached) = load_usage_json_from_cache(&cache_path, "claude usage cache") {
            if parse_claude_usage_error(&cached).is_none() {
                return Some(cached);
            }
        }
    }

    let access_token = read_claude_oauth_access_token()?;
    let live = fetch_claude_usage_with_access_token(&access_token)
        .filter(|value| parse_claude_usage_error(value).is_none())
        .or_else(|| {
            let refreshed = refresh_claude_oauth_access_token()?;
            fetch_claude_usage_with_access_token(&refreshed)
        });

    if let Some(value) = live {
        if parse_claude_usage_error(&value).is_none() {
            write_json_cache(&cache_path, &value);
        }
        return Some(value);
    }

    load_usage_json_from_cache(&cache_path, "claude usage cache")
}

pub(super) fn parse_claude_usage_error(data: &serde_json::Value) -> Option<String> {
    let error = data.get("error")?;
    let error_type = error.get("type").and_then(|value| value.as_str());
    let error_code = error
        .get("details")
        .and_then(|value| value.get("error_code"))
        .and_then(|value| value.as_str());

    if matches!(error_type, Some("authentication_error"))
        || matches!(error_code, Some("token_expired" | "invalid_token"))
    {
        return Some("Re-auth required".into());
    }

    None
}

pub(super) fn parse_claude_usage_snapshot(data: &serde_json::Value) -> Option<ClaudeUsageSnapshot> {
    if let Some(summary) = parse_claude_usage_error(data) {
        return Some(ClaudeUsageSnapshot {
            summary: Some(summary),
        });
    }

    let current_value = data.get("five_hour").and_then(|window| {
        let used_percent = window.get("utilization")?.as_f64()?;
        let reset_in = window
            .get("resets_at")
            .and_then(|value| value.as_str())
            .and_then(format_reset_time_from_iso);
        Some(format_remaining_window_value("5h", used_percent, reset_in))
    });
    let weekly_value = data.get("seven_day").and_then(|window| {
        let used_percent = window.get("utilization")?.as_f64()?;
        let reset_in = window
            .get("resets_at")
            .and_then(|value| value.as_str())
            .and_then(format_reset_time_from_iso);
        Some(format_remaining_window_value("7d", used_percent, reset_in))
    });

    let summary = match (current_value, weekly_value) {
        (Some(current), Some(weekly)) => Some(format!("{current}  |  {weekly}")),
        (Some(current), None) => Some(current),
        (None, Some(weekly)) => Some(weekly),
        (None, None) => None,
    };

    summary.as_ref()?;
    Some(ClaudeUsageSnapshot { summary })
}

pub(super) fn load_claude_usage_snapshot() -> Option<ClaudeUsageSnapshot> {
    let data = fetch_claude_usage_json()?;
    parse_claude_usage_snapshot(&data)
}

pub(super) fn read_kimi_oauth_credentials() -> Option<serde_json::Value> {
    read_json_file_with_debug(&kimi_credentials_path(), "kimi credentials")
}

pub(super) fn write_kimi_oauth_credentials(credentials: &serde_json::Value) -> Option<()> {
    let path = kimi_credentials_path();
    let bytes = serde_json::to_vec(credentials)
        .map_err(|err| log::debug!("failed to serialize kimi credentials: {}", err))
        .ok()?;
    write_atomic(&path, &bytes)
        .map_err(|err| log::debug!("failed to write kimi credentials: {}", err))
        .ok()?;
    Some(())
}

pub(super) fn read_kimi_access_token() -> Option<String> {
    read_kimi_oauth_credentials()?
        .get("access_token")
        .and_then(|value| value.as_str())
        .map(|value| value.to_string())
}

pub(super) fn read_kimi_refresh_token() -> Option<String> {
    read_kimi_oauth_credentials()?
        .get("refresh_token")
        .and_then(|value| value.as_str())
        .map(|value| value.to_string())
}

pub(super) fn kimi_access_token_needs_refresh() -> bool {
    let Some(credentials) = read_kimi_oauth_credentials() else {
        return false;
    };
    let expires_at = credentials
        .get("expires_at")
        .and_then(|value| value.as_f64())
        .unwrap_or(0.0);
    let now = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .ok()
        .map(|duration| duration.as_secs_f64())
        .unwrap_or(0.0);
    expires_at <= now + 300.0
}

pub(super) fn refresh_kimi_access_token() -> Option<String> {
    let current_credentials = read_kimi_oauth_credentials()?;
    let refresh_token = read_kimi_refresh_token()?;
    let client_id = format!("client_id={KIMI_OAUTH_CLIENT_ID}");
    let curl_config = curl_config_data_urlencode("refresh_token", &refresh_token);
    let refreshed = run_curl_with_config(
        &[
            "-sS",
            "--max-time",
            "5",
            "-X",
            "POST",
            KIMI_OAUTH_TOKEN_URL,
            "--data-urlencode",
            &client_id,
            "--data-urlencode",
            "grant_type=refresh_token",
        ],
        &curl_config,
    )?;

    let access_token = refreshed
        .get("access_token")
        .and_then(|value| value.as_str())
        .map(|value| value.to_string())?;
    let rotated_refresh_token = refreshed
        .get("refresh_token")
        .and_then(|value| value.as_str())
        .unwrap_or(&refresh_token)
        .to_string();

    let mut updated_credentials = current_credentials;
    let object = updated_credentials.as_object_mut()?;
    object.insert(
        "access_token".into(),
        serde_json::Value::String(access_token.clone()),
    );
    object.insert(
        "refresh_token".into(),
        serde_json::Value::String(rotated_refresh_token),
    );
    if let Some(expires_in) = refreshed.get("expires_in").and_then(|value| value.as_f64()) {
        let expires_at = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .ok()
            .map(|duration| duration.as_secs_f64() + expires_in)?;
        object.insert("expires_at".into(), serde_json::json!(expires_at));
    }
    if let Some(scope) = refreshed.get("scope").and_then(|value| value.as_str()) {
        object.insert("scope".into(), serde_json::Value::String(scope.to_string()));
    }
    if let Some(token_type) = refreshed.get("token_type").and_then(|value| value.as_str()) {
        object.insert(
            "token_type".into(),
            serde_json::Value::String(token_type.to_string()),
        );
    }

    let _ = write_kimi_oauth_credentials(&updated_credentials);
    Some(access_token)
}

pub(super) fn read_kimi_base_url() -> String {
    let raw = std::fs::read_to_string(Tool::Kimi.config_path()).ok();
    raw.and_then(|raw| raw.parse::<toml::Value>().ok())
        .and_then(|parsed| {
            parsed
                .get("providers")
                .and_then(|value| value.get("managed:kimi-code"))
                .and_then(|value| value.get("base_url"))
                .and_then(|value| value.as_str())
                .map(|value| value.to_string())
        })
        .unwrap_or_else(|| KIMI_DEFAULT_BASE_URL.to_string())
}

pub(super) fn fetch_kimi_usage_with_access_token(access_token: &str) -> Option<serde_json::Value> {
    let usage_url = format!("{}/usages", read_kimi_base_url().trim_end_matches('/'));
    let authorization = format!("Bearer {access_token}");
    let curl_config = curl_config_header("Authorization", &authorization);
    run_curl_with_config(
        &["-sS", "--max-time", "3", usage_url.as_str()],
        &curl_config,
    )
}

pub(super) fn fetch_kimi_usage_json() -> Option<serde_json::Value> {
    let cache_path = kimi_usage_cache_path();
    if cache_path.exists() && usage_cache_is_fresh(&cache_path) {
        if let Some(cached) = load_usage_json_from_cache(&cache_path, "kimi usage cache") {
            if cached.get("usage").is_some() {
                return Some(cached);
            }
        }
    }

    let mut access_token = read_kimi_access_token()?;
    let mut refreshed = false;
    if kimi_access_token_needs_refresh() {
        access_token = refresh_kimi_access_token()?;
        refreshed = true;
    }

    let live = if let Some(value) = fetch_kimi_usage_with_access_token(&access_token)
        .and_then(|value| value.get("usage").is_some().then_some(value))
    {
        Some(value)
    } else if refreshed {
        None
    } else {
        let retried = {
            let refreshed = refresh_kimi_access_token()?;
            fetch_kimi_usage_with_access_token(&refreshed)
        };
        retried.and_then(|value| value.get("usage").is_some().then_some(value))
    };

    if let Some(value) = live {
        write_json_cache(&cache_path, &value);
        return Some(value);
    }

    load_usage_json_from_cache(&cache_path, "kimi usage cache")
}

pub(super) fn kimi_limit_label(item: &serde_json::Value, idx: usize) -> String {
    let duration = item
        .get("window")
        .and_then(|value| value.get("duration"))
        .and_then(|value| value.as_i64())
        .or_else(|| item.get("duration").and_then(|value| value.as_i64()));
    let time_unit = item
        .get("window")
        .and_then(|value| value.get("timeUnit"))
        .and_then(|value| value.as_str())
        .or_else(|| item.get("timeUnit").and_then(|value| value.as_str()))
        .unwrap_or("");

    match (duration, time_unit) {
        (Some(duration), unit) if unit.contains("MINUTE") => {
            if duration >= 60 && duration % 60 == 0 {
                format!("{}h", duration / 60)
            } else {
                format!("{duration}m")
            }
        }
        (Some(duration), unit) if unit.contains("HOUR") => format!("{duration}h"),
        (Some(duration), unit) if unit.contains("DAY") => format!("{duration}d"),
        _ => format!("Limit #{}", idx + 1),
    }
}

pub(super) fn format_kimi_usage_value(label: &str, detail: &serde_json::Value) -> Option<String> {
    let limit = detail
        .get("limit")
        .and_then(|value| value.as_str().and_then(|value| value.parse::<f64>().ok()))
        .or_else(|| detail.get("limit").and_then(|value| value.as_f64()))?;
    let used = detail
        .get("used")
        .and_then(|value| value.as_str().and_then(|value| value.parse::<f64>().ok()))
        .or_else(|| detail.get("used").and_then(|value| value.as_f64()))
        .or_else(|| {
            let remaining = detail
                .get("remaining")
                .and_then(|value| value.as_str().and_then(|value| value.parse::<f64>().ok()))
                .or_else(|| detail.get("remaining").and_then(|value| value.as_f64()))?;
            Some(limit - remaining)
        })?;
    let reset_in = detail
        .get("resetTime")
        .or_else(|| detail.get("reset_at"))
        .or_else(|| detail.get("resetAt"))
        .and_then(|value| value.as_str())
        .and_then(format_reset_time_from_iso);
    let used_percent = if limit > 0.0 {
        (used / limit) * 100.0
    } else {
        100.0
    };
    Some(format_remaining_window_value(label, used_percent, reset_in))
}

pub(super) fn parse_kimi_usage_snapshot(data: &serde_json::Value) -> Option<KimiUsageSnapshot> {
    let weekly_value = data
        .get("usage")
        .and_then(|usage| format_kimi_usage_value("7d", usage));
    let current_value = data
        .get("limits")
        .and_then(|value| value.as_array())
        .and_then(|limits| {
            limits.iter().enumerate().find_map(|(idx, item)| {
                let label = kimi_limit_label(item, idx);
                let detail = item.get("detail").unwrap_or(item);
                if label == "5h" || label == "300m" {
                    format_kimi_usage_value("5h", detail)
                } else {
                    format_kimi_usage_value(&label, detail)
                }
            })
        });

    let summary = match (current_value, weekly_value) {
        (Some(current), Some(weekly)) => Some(format!("{current}  |  {weekly}")),
        (Some(current), None) => Some(current),
        (None, Some(weekly)) => Some(weekly),
        (None, None) => None,
    };

    summary.as_ref()?;
    Some(KimiUsageSnapshot { summary })
}

pub(super) fn load_kimi_usage_snapshot() -> Option<KimiUsageSnapshot> {
    let data = fetch_kimi_usage_json()?;
    parse_kimi_usage_snapshot(&data)
}

pub(super) fn read_gh_auth_token() -> Option<String> {
    let output = std::process::Command::new("gh")
        .args(["auth", "token"])
        .stdout(std::process::Stdio::piped())
        .stderr(std::process::Stdio::piped())
        .output()
        .ok()?;
    if !output.status.success() {
        let stderr = String::from_utf8_lossy(&output.stderr);
        log::debug!(
            "gh auth token probe failed with status {}: {}",
            output.status,
            stderr.trim()
        );
        return None;
    }

    String::from_utf8(output.stdout)
        .map_err(|err| log::debug!("gh auth token probe returned non-utf8 stdout: {}", err))
        .ok()
        .map(|value| value.trim().to_string())
        .filter(|value| !value.is_empty())
}

pub(super) fn fetch_copilot_usage_json() -> Option<serde_json::Value> {
    let cache_path = copilot_usage_cache_path();
    fetch_usage_json_with_cache(cache_path, "copilot usage cache", || {
        let token = read_gh_auth_token()?;
        let authorization = format!("token {token}");
        let curl_config = curl_config_header("Authorization", &authorization);
        run_curl_with_config(
            &[
                "-sS",
                "--max-time",
                "3",
                "-H",
                "Accept: application/json",
                "-H",
                "Editor-Version: vscode/1.96.2",
                "-H",
                "Editor-Plugin-Version: copilot-chat/0.26.7",
                "-H",
                "User-Agent: GitHubCopilotChat/0.26.7",
                "-H",
                "X-Github-Api-Version: 2025-04-01",
                "https://api.github.com/copilot_internal/user",
            ],
            &curl_config,
        )
    })
}

pub(super) fn parse_copilot_usage_snapshot(
    data: &serde_json::Value,
) -> Option<CopilotUsageSnapshot> {
    let premium = data
        .get("quota_snapshots")
        .and_then(|value| value.get("premium_interactions"))?;
    let remaining = premium
        .get("remaining")
        .and_then(|value| value.as_f64())
        .or_else(|| {
            premium
                .get("remaining")
                .and_then(|value| value.as_str()?.parse::<f64>().ok())
        })
        .or_else(|| {
            premium
                .get("quota_remaining")
                .and_then(|value| value.as_f64())
        })?;
    let reset_in = data
        .get("quota_reset_date_utc")
        .and_then(|value| value.as_str())
        .and_then(format_reset_time_from_iso);

    let mut summary = format!(
        "{} left this month",
        format_remaining_count_value(remaining)
    );
    if let Some(reset_in) = reset_in {
        summary.push_str(" · reset ");
        summary.push_str(&reset_in);
    }

    Some(CopilotUsageSnapshot {
        summary: Some(summary),
    })
}

pub(super) fn load_copilot_usage_snapshot() -> Option<CopilotUsageSnapshot> {
    let data = fetch_copilot_usage_json()?;
    parse_copilot_usage_snapshot(&data)
}

pub(super) fn gemini_quota_summary(data: &serde_json::Value) -> Option<String> {
    let auth_type = data
        .get("security")
        .and_then(|security| security.get("auth"))
        .and_then(|auth| auth.get("selectedType"))
        .and_then(|value| value.as_str())?;

    // Gemini CLI doesn't expose a stable local "remaining quota" endpoint yet,
    // so these are approximate plan limits used for quick at-a-glance guidance.
    match auth_type {
        "oauth-personal" => Some("Quota 1000/day · 60/min".into()),
        "gemini-api-key" | "api-key" => Some("Quota 250/day · 10/min".into()),
        "workspace-standard" => Some("Quota 1500/day · 120/min".into()),
        "workspace-enterprise" => Some("Quota 2000/day · 120/min".into()),
        _ if auth_type.contains("workspace") => Some("Quota via workspace plan".into()),
        _ if auth_type.contains("vertex") => Some("Quota via Vertex AI".into()),
        _ => None,
    }
}
