//! Concrete Kaku Assistant configuration parsing, fields, and persistence.

use super::super::{
    assistant_model_options_for_config, mask_key, parse_custom_headers_toml, parse_exit_codes_toml,
    push_ui_error, read_codex_model_options, render_toml_string, FieldEntry, KakuAssistantConfig,
    FOLLOW_CODEX_MODEL,
};
use crate::assistant_config;
use crate::utils::write_atomic;
use anyhow::Context;
use std::io;
use std::path::Path;

/// Parses a KakuAssistantConfig from TOML content.
///
/// This function gracefully handles malformed TOML by using default values
/// for any missing or invalid fields.
pub(in crate::ai_config::tui) fn parse_kaku_assistant_config(raw: &str) -> KakuAssistantConfig {
    let parsed = raw.parse::<toml::Value>().unwrap_or_else(|e| {
        log::warn!("failed to parse assistant.toml: {}", e);
        push_ui_error("Kaku Assistant config TOML is malformed");
        toml::Value::Table(Default::default())
    });

    let enabled = parsed
        .get("enabled")
        .and_then(|v| v.as_bool())
        .unwrap_or(true);
    let api_key = parsed.get("api_key").and_then(|v| v.as_str()).unwrap_or("");
    let model = parsed.get("model").and_then(|v| v.as_str()).unwrap_or("");
    let base_url = parsed
        .get("base_url")
        .and_then(|v| v.as_str())
        .unwrap_or("");
    let stored_auth_type = parsed
        .get("auth_type")
        .and_then(|v| v.as_str())
        .unwrap_or("api_key");
    let api_mode = parsed
        .get("api_mode")
        .and_then(|v| v.as_str())
        .filter(|value| matches!(*value, "chat_completions" | "responses"))
        .unwrap_or("chat_completions");
    let native_web_search = parsed
        .get("native_web_search")
        .and_then(|v| v.as_bool())
        .unwrap_or(false);
    let custom_headers = parse_custom_headers_toml(parsed.get("custom_headers"));
    let auto_fix_ignored_exit_codes =
        parse_exit_codes_toml(parsed.get("auto_fix_ignored_exit_codes"));

    let web_search_provider = parsed
        .get("web_search_provider")
        .and_then(|v| v.as_str())
        .filter(|s| matches!(*s, "brave" | "pipellm" | "tavily"))
        .unwrap_or("none")
        .to_string();

    let web_search_api_key = parsed
        .get("web_search_api_key")
        .and_then(|v| v.as_str())
        .unwrap_or("")
        .to_string();

    let fast_model = parsed
        .get("fast_model")
        .and_then(|v| v.as_str())
        .unwrap_or("")
        .to_string();

    let chat_model = parsed
        .get("chat_model")
        .and_then(|v| v.as_str())
        .unwrap_or("")
        .to_string();

    let chat_model_choices = parsed
        .get("chat_model_choices")
        .and_then(|v| v.as_array())
        .map(|arr| {
            arr.iter()
                .filter_map(|v| v.as_str().map(|s| s.to_string()))
                .filter(|s| !s.trim().is_empty())
                .collect::<Vec<_>>()
        })
        .unwrap_or_default();

    let simple_model = if fast_model.trim().is_empty() {
        model
    } else {
        fast_model.as_str()
    };
    let deep_model = if chat_model.trim().is_empty()
        && !fast_model.trim().is_empty()
        && !model.trim().is_empty()
        && model.trim() != simple_model.trim()
    {
        model.to_string()
    } else {
        chat_model
    };

    let mut cfg = KakuAssistantConfig::new(enabled, api_key, simple_model, base_url)
        .with_custom_headers(custom_headers)
        .with_auto_fix_ignored_exit_codes(auto_fix_ignored_exit_codes)
        .with_web_search(web_search_provider, web_search_api_key);
    cfg.auth_type = stored_auth_type.to_string();
    cfg.api_mode = api_mode.to_string();
    cfg.native_web_search = native_web_search;
    cfg.chat_model = deep_model;
    cfg.chat_model_choices = chat_model_choices;
    cfg
}

pub(in crate::ai_config::tui) fn get_kaku_assistant_api_key() -> Option<String> {
    let path = assistant_config::ensure_assistant_toml_exists()
        .map_err(|e| log::debug!("assistant config not available: {}", e))
        .ok()?;
    let raw = std::fs::read_to_string(&path)
        .map_err(|e| log::debug!("failed to read assistant config: {}", e))
        .ok()?;
    let cfg = parse_kaku_assistant_config(&raw);
    if cfg.api_key().trim().is_empty() {
        log::debug!("assistant config has no api_key set");
        None
    } else {
        Some(cfg.api_key().to_string())
    }
}

pub(in crate::ai_config::tui) fn extract_kaku_assistant_fields_with_model_options(
    raw: &str,
    model_options: Vec<String>,
) -> Vec<FieldEntry> {
    let cfg = parse_kaku_assistant_config(raw);
    let auth_type = cfg.auth_type();
    let is_codex = auth_type == "codex";

    // Auth Type sits right after Enabled because it decides whether Base URL and
    // API Key are even relevant below.
    let mut fields = vec![
        FieldEntry {
            key: "Enabled".into(),
            value: if cfg.is_enabled() { "On" } else { "Off" }.into(),
            options: vec!["On".into(), "Off".into()],
            editable: true,
        },
        FieldEntry {
            key: "Auth Type".into(),
            value: auth_type.to_string(),
            // api_key = configure Kaku directly; codex = follow the user Codex connection.
            options: vec!["api_key".into(), "codex".into()],
            editable: true,
        },
    ];

    if !is_codex {
        fields.push(FieldEntry {
            key: "API Mode".into(),
            value: cfg.api_mode().to_string(),
            options: vec!["chat_completions".into(), "responses".into()],
            editable: true,
        });
        if cfg.api_mode() == "responses" {
            fields.push(FieldEntry {
                key: "Native Web Search".into(),
                value: if cfg.native_web_search() { "On" } else { "Off" }.into(),
                options: vec!["On".into(), "Off".into()],
                editable: true,
            });
        }
    }

    fields.extend([
        FieldEntry {
            key: "Simple Model".into(),
            value: cfg.model().to_string(),
            options: model_options.clone(),
            editable: true,
        },
        FieldEntry {
            key: "Deep Model".into(),
            value: cfg.deep_model().to_string(),
            options: model_options.clone(),
            editable: true,
        },
    ]);

    // Codex follows the user Codex provider, so assistant.toml's Base URL and
    // API Key do not apply there.
    if !is_codex {
        fields.push(FieldEntry {
            key: "Base URL".into(),
            value: cfg.base_url().to_string(),
            options: vec![],
            editable: true,
        });
        fields.push(FieldEntry {
            key: "API Key".into(),
            value: mask_key(cfg.api_key()),
            options: vec![],
            editable: true,
        });
    }

    fields.push(FieldEntry {
        key: "Web Search".into(),
        value: cfg.web_search_provider().to_string(),
        options: vec![
            "none".into(),
            "brave".into(),
            "pipellm".into(),
            "tavily".into(),
        ],
        editable: true,
    });

    // Show Search Key entry only when a provider is selected.
    if cfg.web_search_provider() != "none" {
        fields.push(FieldEntry {
            key: "Search Key".into(),
            value: mask_key(cfg.web_search_api_key()),
            options: vec![],
            editable: true,
        });
    }

    fields
}

pub(in crate::ai_config::tui) fn extract_kaku_assistant_fields(raw: &str) -> Vec<FieldEntry> {
    let cfg = parse_kaku_assistant_config(raw);
    // codex's own model catalog (from the CLI cache), not the OpenAI-API list.
    let model_options = if cfg.auth_type() == "codex" {
        let mut options = read_codex_model_options();
        if !options.iter().any(|option| option == FOLLOW_CODEX_MODEL) {
            options.insert(0, FOLLOW_CODEX_MODEL.to_string());
        }
        options
    } else {
        assistant_model_options_for_config(&cfg)
    };
    extract_kaku_assistant_fields_with_model_options(raw, model_options)
}

pub(in crate::ai_config::tui) fn render_kaku_assistant_config(cfg: &KakuAssistantConfig) -> String {
    let mut out = String::new();
    out.push_str("# Kaku Assistant configuration\n");
    out.push_str(
        "# enabled: true enables command analysis suggestions; false disables requests.\n",
    );
    out.push_str("# api_key: provider API key, example: \"sk-xxxx\".\n");
    out.push_str("# model: Simple Model for quick command generation and lightweight chat.\n");
    out.push_str(
        "# chat_model: Deep Model for Cmd+L, k, and tool-using chat. Omit to reuse model.\n",
    );
    out.push_str(
        "# auto_fix_ignored_exit_codes: optional exit codes that should not trigger automatic command-fix suggestions.\n",
    );
    out.push_str("# base_url: OpenAI-compatible API root URL.\n");
    out.push_str("# api_mode: chat_completions (default) or responses for /responses endpoints.\n");
    out.push_str(
        "# native_web_search: add the provider-hosted web_search tool in responses mode.\n",
    );
    out.push_str(
        "# custom_headers: optional extra HTTP headers for enterprise proxies or API gateways.\n",
    );
    out.push_str("#                 format: [\"Header-Name: value\", \"Another-Header: value\"]\n");
    out.push_str("#                 note: Authorization and Content-Type are reserved and cannot be overridden.\n\n");
    out.push_str(if cfg.is_enabled() {
        "enabled = true\n"
    } else {
        "enabled = false\n"
    });
    if cfg.api_key().trim().is_empty() {
        out.push_str("# api_key = \"<your_api_key>\"\n");
    } else {
        out.push_str(&format!(
            "api_key = {}\n",
            render_toml_string(cfg.api_key().trim())
        ));
    }
    out.push_str(&format!(
        "model = {}\n",
        render_toml_string(cfg.model().trim())
    ));
    // Round-trip deep-model choices so the GUI overlay's model picker is preserved.
    if !cfg.chat_model().trim().is_empty() {
        out.push_str(&format!(
            "chat_model = {}\n",
            render_toml_string(cfg.chat_model().trim())
        ));
    }
    if !cfg.chat_model_choices().is_empty() {
        let arr = toml::Value::Array(
            cfg.chat_model_choices()
                .iter()
                .map(|item| toml::Value::String(item.clone()))
                .collect(),
        );
        out.push_str(&format!("chat_model_choices = {}\n", arr));
    }
    if !cfg.auto_fix_ignored_exit_codes().is_empty() {
        let arr = toml::Value::Array(
            cfg.auto_fix_ignored_exit_codes()
                .iter()
                .map(|item| toml::Value::Integer(i64::from(*item)))
                .collect(),
        );
        out.push_str(&format!("auto_fix_ignored_exit_codes = {}\n", arr));
    }
    out.push_str(&format!(
        "base_url = {}\n",
        render_toml_string(cfg.base_url().trim())
    ));
    if cfg.api_mode() == "responses" {
        out.push_str("api_mode = \"responses\"\n");
    } else {
        out.push_str("# api_mode = \"responses\"\n");
    }
    if cfg.native_web_search() {
        out.push_str("native_web_search = true\n");
    } else {
        out.push_str("# native_web_search = true\n");
    }
    // Persist auth_type only when it differs from "api_key" so the Codex provider
    // (same base_url as OpenAI) can be reliably identified on next load.
    if cfg.auth_type() != "api_key" {
        out.push_str(&format!(
            "auth_type = {}\n",
            render_toml_string(cfg.auth_type())
        ));
    }
    if cfg.custom_headers().is_empty() {
        out.push_str("# custom_headers = [\"X-Customer-ID: your-customer-id\"]\n");
    } else {
        let arr = toml::Value::Array(
            cfg.custom_headers()
                .iter()
                .map(|item| toml::Value::String(item.clone()))
                .collect(),
        );
        out.push_str(&format!("custom_headers = {}\n", arr));
    }
    // Web search: comment out when not configured to keep the file clean.
    let provider = cfg.web_search_provider();
    if provider == "none" || provider.is_empty() {
        out.push_str("# web_search_provider = \"brave\"  # or pipellm, tavily\n");
        out.push_str("# web_search_api_key = \"...\"\n");
    } else {
        out.push_str(&format!(
            "web_search_provider = {}\n",
            render_toml_string(provider)
        ));
        if cfg.web_search_api_key().trim().is_empty() {
            out.push_str("# web_search_api_key = \"...\"\n");
        } else {
            out.push_str(&format!(
                "web_search_api_key = {}\n",
                render_toml_string(cfg.web_search_api_key().trim())
            ));
        }
    }
    out
}

#[cfg(test)]
pub(in crate::ai_config::tui) fn write_kaku_assistant_config(
    path: &Path,
    cfg: &KakuAssistantConfig,
) -> anyhow::Result<()> {
    let out = render_kaku_assistant_config(cfg);
    write_atomic(path, out.as_bytes()).with_context(|| format!("write {}", path.display()))
}

/// Update the fields owned by the TUI while retaining runtime-only and future
/// top-level keys. Reconstructing the whole file used to silently turn
/// `chat_tools_enabled = false` back into the GUI default and dropped custom
/// integrations whenever an unrelated field was edited.
pub(in crate::ai_config::tui) fn write_kaku_assistant_config_preserving(
    path: &Path,
    cfg: &KakuAssistantConfig,
    original_raw: &str,
) -> anyhow::Result<()> {
    const TUI_MANAGED_KEYS: &[&str] = &[
        "enabled",
        "api_key",
        "model",
        "fast_model",
        "chat_model",
        "chat_model_choices",
        "auto_fix_ignored_exit_codes",
        "base_url",
        "api_mode",
        "native_web_search",
        "auth_type",
        "custom_headers",
        "web_search_provider",
        "web_search_api_key",
    ];

    let canonical = render_kaku_assistant_config(cfg);
    // toml_edit keeps the user's comments, ordering, and formatting for every
    // line the TUI does not own; plain `toml` re-serialization stripped all
    // template comments on each save.
    let Ok(mut original) = original_raw.parse::<toml_edit::DocumentMut>() else {
        return write_atomic(path, canonical.as_bytes())
            .with_context(|| format!("write {}", path.display()));
    };
    let canonical_doc = canonical
        .parse::<toml_edit::DocumentMut>()
        .context("parse generated assistant config")?;

    for key in TUI_MANAGED_KEYS {
        match canonical_doc.get(key) {
            Some(item) => {
                original[*key] = item.clone();
            }
            None => {
                original.remove(key);
            }
        }
    }

    write_atomic(path, original.to_string().as_bytes())
        .with_context(|| format!("write {}", path.display()))
}

pub(in crate::ai_config::tui) fn save_kaku_assistant_field(
    field_key: &str,
    new_val: &str,
) -> anyhow::Result<()> {
    let path = assistant_config::ensure_assistant_toml_exists()?;
    save_kaku_assistant_field_to_path(&path, field_key, new_val)
}

pub(in crate::ai_config::tui) fn save_kaku_assistant_field_to_path(
    path: &Path,
    field_key: &str,
    new_val: &str,
) -> anyhow::Result<()> {
    let raw = match std::fs::read_to_string(path) {
        Ok(raw) => raw,
        Err(e) if e.kind() == io::ErrorKind::NotFound => {
            log::debug!(
                "assistant config missing when saving; recreating {}",
                path.display()
            );
            String::new()
        }
        Err(e)
            if matches!(
                e.kind(),
                io::ErrorKind::PermissionDenied | io::ErrorKind::InvalidData
            ) =>
        {
            log::warn!("failed to read assistant config {}: {}", path.display(), e);
            push_ui_error(format!(
                "cannot read {}. Check file permission or encoding.",
                path.display()
            ));
            String::new()
        }
        Err(e) => {
            log::debug!("failed to read assistant config {}: {}", path.display(), e);
            String::new()
        }
    };
    let cfg = parse_kaku_assistant_config(&raw);

    // Build updated config based on which field changed.
    // Every arm must round-trip ALL fields to avoid losing values not in the changed arm,
    // including chat_model_choices (via with_chat_model_passthrough).
    // auth_type is copied after the match to preserve round-trip for power-user hand-edited toml.
    let mut updated = match field_key {
        "Enabled" => {
            let enabled = matches!(new_val.trim(), "On" | "on" | "true" | "1");
            KakuAssistantConfig::new(enabled, cfg.api_key(), cfg.model(), cfg.base_url())
                .with_custom_headers(cfg.custom_headers().to_vec())
                .with_web_search(cfg.web_search_provider(), cfg.web_search_api_key())
                .with_chat_model_passthrough(&cfg)
        }
        "Simple Model" | "Model" => {
            let model = if new_val.trim().is_empty() || new_val == "-" {
                assistant_config::DEFAULT_MODEL
            } else {
                new_val.trim()
            };
            KakuAssistantConfig::new(cfg.is_enabled(), cfg.api_key(), model, cfg.base_url())
                .with_custom_headers(cfg.custom_headers().to_vec())
                .with_web_search(cfg.web_search_provider(), cfg.web_search_api_key())
                .with_chat_model_passthrough(&cfg)
        }
        "Deep Model" | "Chat Model" => {
            let chat_model = if new_val.trim().is_empty() || new_val == "-" {
                ""
            } else {
                new_val.trim()
            };
            // Use choices-only passthrough: the user's explicit choice (including
            // empty) must not be overwritten by the full passthrough's restore logic.
            KakuAssistantConfig::new(cfg.is_enabled(), cfg.api_key(), cfg.model(), cfg.base_url())
                .with_custom_headers(cfg.custom_headers().to_vec())
                .with_chat_model(chat_model)
                .with_web_search(cfg.web_search_provider(), cfg.web_search_api_key())
                .with_chat_model_choices_passthrough(&cfg)
        }
        "Base URL" => {
            let base_url = if new_val.trim().is_empty() || new_val == "-" {
                assistant_config::DEFAULT_BASE_URL
            } else {
                new_val.trim()
            };
            KakuAssistantConfig::new(cfg.is_enabled(), cfg.api_key(), cfg.model(), base_url)
                .with_custom_headers(cfg.custom_headers().to_vec())
                .with_web_search(cfg.web_search_provider(), cfg.web_search_api_key())
                .with_chat_model_passthrough(&cfg)
        }
        "API Mode" => {
            KakuAssistantConfig::new(cfg.is_enabled(), cfg.api_key(), cfg.model(), cfg.base_url())
                .with_custom_headers(cfg.custom_headers().to_vec())
                .with_web_search(cfg.web_search_provider(), cfg.web_search_api_key())
                .with_chat_model_passthrough(&cfg)
        }
        "Native Web Search" => {
            KakuAssistantConfig::new(cfg.is_enabled(), cfg.api_key(), cfg.model(), cfg.base_url())
                .with_custom_headers(cfg.custom_headers().to_vec())
                .with_web_search(cfg.web_search_provider(), cfg.web_search_api_key())
                .with_chat_model_passthrough(&cfg)
        }
        "API Key" => KakuAssistantConfig::new(
            cfg.is_enabled(),
            new_val.trim(),
            cfg.model(),
            cfg.base_url(),
        )
        .with_custom_headers(cfg.custom_headers().to_vec())
        .with_web_search(cfg.web_search_provider(), cfg.web_search_api_key())
        .with_chat_model_passthrough(&cfg),
        "Web Search" => {
            const VALID: &[&str] = &["none", "brave", "pipellm", "tavily"];
            let provider = if VALID.contains(&new_val.trim()) {
                new_val.trim()
            } else {
                "none"
            };
            // Clearing to "none" also wipes the key to avoid stale credentials.
            let key = if provider == "none" {
                String::new()
            } else {
                cfg.web_search_api_key().to_string()
            };
            KakuAssistantConfig::new(cfg.is_enabled(), cfg.api_key(), cfg.model(), cfg.base_url())
                .with_custom_headers(cfg.custom_headers().to_vec())
                .with_web_search(provider, key)
                .with_chat_model_passthrough(&cfg)
        }
        "Search Key" => {
            KakuAssistantConfig::new(cfg.is_enabled(), cfg.api_key(), cfg.model(), cfg.base_url())
                .with_custom_headers(cfg.custom_headers().to_vec())
                .with_web_search(cfg.web_search_provider(), new_val.trim())
                .with_chat_model_passthrough(&cfg)
        }
        "Auth Type" if new_val.trim() == "codex" => {
            let simple_model = if cfg.model() == assistant_config::DEFAULT_MODEL {
                FOLLOW_CODEX_MODEL
            } else {
                cfg.model()
            };
            let chat_model = if cfg.chat_model() == assistant_config::DEFAULT_CHAT_MODEL {
                FOLLOW_CODEX_MODEL
            } else {
                cfg.chat_model()
            };
            KakuAssistantConfig::new(
                cfg.is_enabled(),
                cfg.api_key(),
                simple_model,
                cfg.base_url(),
            )
            .with_custom_headers(cfg.custom_headers().to_vec())
            .with_chat_model(chat_model)
            .with_web_search(cfg.web_search_provider(), cfg.web_search_api_key())
            .with_chat_model_choices_passthrough(&cfg)
        }
        "Auth Type" => {
            let simple_model = if cfg.model() == FOLLOW_CODEX_MODEL {
                assistant_config::DEFAULT_MODEL
            } else {
                cfg.model()
            };
            let chat_model = if cfg.chat_model() == FOLLOW_CODEX_MODEL {
                assistant_config::DEFAULT_CHAT_MODEL
            } else {
                cfg.chat_model()
            };
            KakuAssistantConfig::new(
                cfg.is_enabled(),
                cfg.api_key(),
                simple_model,
                cfg.base_url(),
            )
            .with_custom_headers(cfg.custom_headers().to_vec())
            .with_web_search(cfg.web_search_provider(), cfg.web_search_api_key())
            .with_chat_model(chat_model)
            .with_chat_model_choices_passthrough(&cfg)
        }
        _ => return Ok(()),
    };
    // auth_type follows the Auth Type field when that is what changed; otherwise
    // it round-trips (migrating legacy "gemini_key" to "api_key" so users stuck
    // on the removed Gemini provider can recover through the TUI).
    updated.auth_type = if field_key == "Auth Type" {
        if new_val.trim() == "codex" {
            "codex".to_string()
        } else {
            "api_key".to_string()
        }
    } else if cfg.auth_type() == "gemini_key" {
        "api_key".to_string()
    } else {
        cfg.auth_type().to_string()
    };
    updated.api_mode = if field_key == "API Mode" {
        if new_val.trim() == "responses" {
            "responses".to_string()
        } else {
            "chat_completions".to_string()
        }
    } else {
        cfg.api_mode().to_string()
    };
    updated.native_web_search = if updated.api_mode != "responses" {
        false
    } else if field_key == "Native Web Search" {
        matches!(new_val.trim(), "On" | "on" | "true" | "1")
    } else {
        cfg.native_web_search()
    };
    updated.auto_fix_ignored_exit_codes = cfg.auto_fix_ignored_exit_codes().to_vec();

    write_kaku_assistant_config_preserving(path, &updated, &raw)
}
