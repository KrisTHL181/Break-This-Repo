//! Persistent state for the AI chat overlay.
//!
//! Stores UI state like the last selected model in `~/.config/kaku/ai_chat_state.json`.
//! Load once at overlay start; save when the user switches models.
//!
//! # Why two model-list caches?
//!
//! Kaku has two on-disk caches of the assistant's `/models` response that
//! intentionally do *not* share a file:
//!
//! | Surface | Path | TTL | Purpose |
//! |---|---|---|---|
//! | `kaku ai` TUI | `~/.cache/kaku/assistant_models.json` | 30 min, base_url-keyed | Driving the dropdown picker; refreshed every TUI session so a freshly-added model shows up. |
//! | Cmd+L overlay (here) | `~/.config/kaku/ai_chat_state.json` `cached_models` | persistent, endpoint-keyed | Show the last list while the same endpoint refreshes. Cached entries are never trusted for the active selection until that refresh succeeds. |
//!
//! Merging them would force one TTL policy on both surfaces and add a
//! cross-binary file lock. The caches are tiny (a list of model IDs) and the
//! divergence is intentional. If you change one, document why the other is
//! still the way it is.

use anyhow::{Context, Result};
use std::path::PathBuf;

#[derive(serde::Serialize, serde::Deserialize, Default)]
struct StateFile {
    version: u32,
    /// Last model selected by the user via Shift+Tab.
    last_model: Option<String>,
    /// Cached model list from the last successful /models fetch.
    #[serde(default)]
    cached_models: Vec<String>,
    /// Identifies the auth transport and endpoint that produced `cached_models`.
    /// Legacy state files do not have this field; their unscoped cache is ignored.
    #[serde(default)]
    cached_models_key: Option<String>,
}

/// Load the last selected model from disk. Returns None on any error (non-fatal).
pub fn load_last_model() -> Option<String> {
    try_load().ok().flatten().and_then(|f| f.last_model)
}

/// Load the cached model list only when it belongs to the current endpoint.
pub fn load_cached_models(cache_key: &str) -> Vec<String> {
    try_load()
        .ok()
        .flatten()
        .map(|file| cached_models_for_key(file, cache_key))
        .unwrap_or_default()
}

fn cached_models_for_key(file: StateFile, cache_key: &str) -> Vec<String> {
    if file.cached_models_key.as_deref() == Some(cache_key) {
        file.cached_models
    } else {
        vec![]
    }
}

fn try_load() -> Result<Option<StateFile>> {
    let path = state_path()?;
    if !path.exists() {
        return Ok(None);
    }
    let raw = std::fs::read_to_string(&path).with_context(|| format!("read {}", path.display()))?;
    let file: StateFile =
        serde_json::from_str(&raw).with_context(|| format!("parse {}", path.display()))?;
    Ok(Some(file))
}

fn load_or_default() -> StateFile {
    try_load()
        .unwrap_or_else(|e| {
            log::warn!("Could not load AI chat state: {e}");
            None
        })
        .unwrap_or_default()
}

/// Save the last selected model to disk atomically.
pub fn save_last_model(model: &str) -> Result<()> {
    let path = state_path()?;
    let mut file = load_or_default();
    file.version = 2;
    file.last_model = Some(model.to_string());
    write_state(&path, &file)
}

/// Persist a fetched model list together with its endpoint identity.
pub fn save_cached_models(cache_key: &str, models: &[String]) -> Result<()> {
    let path = state_path()?;
    let mut file = load_or_default();
    file.version = 2;
    file.cached_models = models.to_vec();
    file.cached_models_key = Some(cache_key.to_string());
    write_state(&path, &file)
}

fn write_state(path: &std::path::PathBuf, file: &StateFile) -> Result<()> {
    let json = serde_json::to_string_pretty(file).context("serialize state")?;
    let tmp = path.with_extension("tmp");
    std::fs::write(&tmp, &json).with_context(|| format!("write {}", tmp.display()))?;
    std::fs::rename(&tmp, path)
        .with_context(|| format!("rename {} -> {}", tmp.display(), path.display()))?;
    Ok(())
}

fn state_path() -> Result<PathBuf> {
    let user_config_path = config::user_config_path();
    let config_dir = user_config_path
        .parent()
        .ok_or_else(|| anyhow::anyhow!("invalid user config path"))?;
    Ok(config_dir.join("ai_chat_state.json"))
}

#[cfg(test)]
mod tests {
    use super::{cached_models_for_key, StateFile};

    #[test]
    fn legacy_unscoped_model_cache_is_ignored() {
        let file = StateFile {
            cached_models: vec!["old-provider-model".to_string()],
            ..Default::default()
        };

        assert!(cached_models_for_key(file, "api_key\nhttps://new.example").is_empty());
    }

    #[test]
    fn model_cache_is_restored_only_for_matching_endpoint() {
        let make_file = || StateFile {
            cached_models: vec!["current-model".to_string()],
            cached_models_key: Some("api_key\nhttps://current.example".to_string()),
            ..Default::default()
        };

        assert_eq!(
            cached_models_for_key(make_file(), "api_key\nhttps://current.example"),
            ["current-model"]
        );
        assert!(cached_models_for_key(make_file(), "codex\nhttps://current.example").is_empty());
    }
}
