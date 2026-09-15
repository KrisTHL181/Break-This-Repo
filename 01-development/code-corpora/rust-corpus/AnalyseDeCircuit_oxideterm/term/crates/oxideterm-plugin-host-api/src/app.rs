// Copyright (C) 2026 AnalyseDeCircuit
// SPDX-License-Identifier: GPL-3.0-only

//! App-level host API projections shared by native plugin runtimes.

use serde_json::{Value, json};

pub fn native_plugin_i18n_translate(
    i18n: &oxideterm_i18n::I18n,
    plugin_id: &str,
    key: &str,
) -> String {
    let full_key = format!("plugin.{plugin_id}.{key}");
    let translated = i18n.t(&full_key);
    // Tauri pluginI18nManager auto-prefixes plugin keys and falls back to the
    // raw plugin key when no bundle is loaded. Native keeps that contract while
    // plugin locale-bundle loading is completed in the rest of Phase 4.
    if translated == full_key {
        key.to_string()
    } else {
        translated
    }
}

pub fn native_plugin_layout_snapshot(
    sidebar_collapsed: bool,
    active_tab_id: Option<String>,
    tab_count: usize,
) -> Value {
    // Tauri exposes this exact app-store shape and freezes it before returning
    // to plugins. Native mirrors the field names so process runtimes can share
    // the same plugin-facing API contract.
    json!({
        "sidebarCollapsed": sidebar_collapsed,
        "activeTabId": active_tab_id,
        "tabCount": tab_count,
    })
}

pub fn native_plugin_platform_label() -> &'static str {
    if cfg!(target_os = "macos") {
        "macos"
    } else if cfg!(target_os = "windows") {
        "windows"
    } else {
        "linux"
    }
}

pub fn native_plugin_theme_snapshot(theme_name: &str) -> Value {
    json!({
        "name": theme_name,
        "isDark": native_plugin_theme_is_dark(theme_name),
    })
}

fn native_plugin_theme_is_dark(theme_name: &str) -> bool {
    !theme_name.to_ascii_lowercase().contains("light")
}

pub fn native_plugin_settings_section(settings: &Value, category: &str) -> Value {
    settings
        .get(category)
        .filter(|value| value.is_object())
        .cloned()
        .unwrap_or_else(|| json!({}))
}

/// Builds a stable settings projection from an explicit allowlist of non-sensitive fields.
pub fn native_plugin_settings_summary(settings: &Value, locale: &str, theme_name: &str) -> Value {
    // Keep this projection field-by-field. Recursively copying `enabled` values
    // could expose provider, proxy, path, environment, or future secret-bearing settings.
    json!({
        "locale": locale,
        "theme": theme_name,
        "density": settings.pointer("/appearance/uiDensity").and_then(Value::as_str),
        "features": {
            "terminalBackground": settings.pointer("/terminal/backgroundEnabled").and_then(Value::as_bool).unwrap_or(false),
            "terminalCommandBar": settings.pointer("/terminal/commandBar/enabled").and_then(Value::as_bool).unwrap_or(false),
            "terminalCommandMarks": settings.pointer("/terminal/commandMarks/enabled").and_then(Value::as_bool).unwrap_or(false),
            "inBandTransfer": settings.pointer("/terminal/inBandTransfer/enabled").and_then(Value::as_bool).unwrap_or(false),
            "sftpSpeedLimit": settings.pointer("/sftp/speedLimitEnabled").and_then(Value::as_bool).unwrap_or(false),
            "reconnect": settings.pointer("/reconnect/enabled").and_then(Value::as_bool).unwrap_or(false),
            "launcher": settings.pointer("/launcher/enabled").and_then(Value::as_bool).unwrap_or(false),
        },
    })
}

/// Projects connection snapshots into useful metadata without endpoint or error details.
pub fn native_plugin_connection_summaries(
    connections: &[Value],
    connection_states: &std::collections::HashMap<String, Value>,
) -> Value {
    Value::Array(
        connections
            .iter()
            .map(|connection| {
                let connection_id = connection
                    .get("id")
                    .and_then(Value::as_str)
                    .unwrap_or_default();
                let state = connection_states
                    .get(connection_id)
                    .or_else(|| connection.get("state"))
                    .map(native_plugin_safe_state_label)
                    .unwrap_or("unknown");
                json!({
                    "id": connection_id,
                    "state": state,
                    "refCount": connection.get("refCount").and_then(Value::as_u64).unwrap_or_default(),
                    "keepAlive": connection.get("keepAlive").and_then(Value::as_bool).unwrap_or(false),
                    "terminalCount": connection
                        .get("terminalIds")
                        .and_then(Value::as_array)
                        .map(Vec::len)
                        .unwrap_or_default(),
                    "hasParent": connection.get("parentConnectionId").is_some_and(|value| !value.is_null()),
                    "createdAt": connection.get("createdAt").and_then(Value::as_str),
                    "lastActive": connection.get("lastActive").and_then(Value::as_str),
                })
            })
            .collect(),
    )
}

/// Summarizes the session tree while excluding labels, endpoints, and failure messages.
pub fn native_plugin_session_summary(session_tree: &[Value]) -> Value {
    let nodes = session_tree
        .iter()
        .map(|node| {
            json!({
                "nodeId": node.get("id").and_then(Value::as_str).unwrap_or_default(),
                "parentNodeId": node.get("parentId").and_then(Value::as_str),
                "state": node
                    .get("connectionState")
                    .map(native_plugin_safe_state_label)
                    .unwrap_or("unknown"),
                "childCount": node
                    .get("childIds")
                    .and_then(Value::as_array)
                    .map(Vec::len)
                    .unwrap_or_default(),
                "terminalCount": node
                    .get("terminalIds")
                    .and_then(Value::as_array)
                    .map(Vec::len)
                    .unwrap_or_default(),
                "hasConnection": node.get("connectionId").is_some_and(|value| !value.is_null()),
                "hasSftpSession": node.get("sftpSessionId").is_some_and(|value| !value.is_null()),
            })
        })
        .collect::<Vec<_>>();
    let active_node_count = nodes
        .iter()
        .filter(|node| {
            matches!(
                node.get("state").and_then(Value::as_str),
                Some("active" | "connected")
            )
        })
        .count();
    json!({
        "nodeCount": nodes.len(),
        "activeNodeCount": active_node_count,
        "nodes": nodes,
    })
}

/// Projects the AI provider and model catalog without conversation content or credentials.
pub fn native_plugin_ai_catalog_summary(snapshot: &Value) -> Value {
    let active_provider_type = snapshot
        .get("activeProvider")
        .and_then(|provider| provider.get("type"))
        .and_then(Value::as_str);
    let available_models = snapshot
        .get("availableModels")
        .and_then(Value::as_array)
        .into_iter()
        .flatten()
        .filter_map(Value::as_str)
        .map(str::to_string)
        .collect::<Vec<_>>();
    json!({
        "configured": active_provider_type.is_some(),
        "activeProviderType": active_provider_type,
        "availableModels": available_models,
    })
}

/// Summarizes IDE state without exposing project names, paths, branches, or file content.
pub fn native_plugin_ide_state_summary(snapshot: &Value) -> Value {
    let open_files = snapshot
        .get("openFiles")
        .and_then(Value::as_array)
        .map(Vec::as_slice)
        .unwrap_or_default();
    json!({
        "isOpen": snapshot.get("isOpen").and_then(Value::as_bool).unwrap_or(false),
        "hasProject": snapshot.get("project").is_some_and(|value| !value.is_null()),
        "isGitRepository": snapshot
            .get("project")
            .and_then(|project| project.get("isGitRepo"))
            .and_then(Value::as_bool)
            .unwrap_or(false),
        "openFileCount": open_files.len(),
        "dirtyFileCount": open_files
            .iter()
            .filter(|file| file.get("isDirty").and_then(Value::as_bool).unwrap_or(false))
            .count(),
        "pinnedFileCount": open_files
            .iter()
            .filter(|file| file.get("isPinned").and_then(Value::as_bool).unwrap_or(false))
            .count(),
        "activeFileLanguage": snapshot
            .get("activeFile")
            .and_then(|file| file.get("language"))
            .and_then(Value::as_str),
    })
}

/// Collapses structured failure state into a stable label at the plugin boundary.
fn native_plugin_safe_state_label(state: &Value) -> &str {
    state.as_str().unwrap_or_else(|| {
        if state.is_object() {
            "error"
        } else {
            "unknown"
        }
    })
}

pub fn native_plugin_custom_event_from_args(
    plugin_id: &str,
    args: Value,
) -> Result<(String, Value), String> {
    let event_name = args
        .get("name")
        .or_else(|| args.get("event"))
        .and_then(Value::as_str)
        .ok_or_else(|| "events.emit requires args.name".to_string())?;
    let owner_plugin_id = args
        .get("pluginId")
        .or_else(|| args.get("ownerPluginId"))
        .and_then(Value::as_str)
        .unwrap_or(plugin_id);
    let event_key = native_plugin_custom_event_key(owner_plugin_id, event_name)?;
    // Custom plugin events are scoped to the emitting plugin by default. The
    // payload names both the owner and public event name so subscribers do not
    // need to parse the internal routing key.
    Ok((
        event_key,
        json!({
            "pluginId": owner_plugin_id,
            "name": event_name,
            "payload": args.get("payload").cloned().unwrap_or(Value::Null),
        }),
    ))
}

pub fn native_plugin_custom_event_key(
    owner_plugin_id: &str,
    event_name: &str,
) -> Result<String, String> {
    native_plugin_validate_plugin_id(owner_plugin_id)?;
    native_plugin_validate_event_name(event_name)?;
    Ok(format!("plugin.{owner_plugin_id}:{event_name}"))
}

pub fn native_plugin_validate_plugin_id(plugin_id: &str) -> Result<(), String> {
    if plugin_id.is_empty() {
        return Err("Plugin ID cannot be empty".to_string());
    }
    if plugin_id.contains("..") {
        return Err("Plugin ID cannot contain path traversal (..)".to_string());
    }
    if plugin_id.contains('/') || plugin_id.contains('\\') {
        return Err("Plugin ID cannot contain path separators".to_string());
    }
    if plugin_id.bytes().any(|byte| byte < 0x20) {
        return Err("Plugin ID contains invalid characters".to_string());
    }
    Ok(())
}

pub fn native_plugin_validate_event_name(event_name: &str) -> Result<(), String> {
    if event_name.trim().is_empty() {
        return Err("Plugin event name cannot be empty".to_string());
    }
    if event_name.len() > 128 {
        return Err("Plugin event name is too long".to_string());
    }
    if event_name.contains("..") || event_name.contains('/') || event_name.contains('\\') {
        return Err("Plugin event name cannot contain path separators or traversal".to_string());
    }
    if event_name
        .bytes()
        .any(|byte| byte < 0x20 || byte == b'*' || byte == b' ')
    {
        return Err("Plugin event name contains invalid characters".to_string());
    }
    Ok(())
}
