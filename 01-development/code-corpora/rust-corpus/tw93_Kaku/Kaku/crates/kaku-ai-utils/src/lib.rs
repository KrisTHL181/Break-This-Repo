//! Small shared utilities for Kaku's AI-related binaries.

use std::path::{Path, PathBuf};

/// Resolve the user-level Codex configuration directory consistently across
/// Kaku's GUI and configuration CLI.
pub fn codex_home_dir(fallback_home: &Path) -> PathBuf {
    std::env::var_os("CODEX_HOME")
        .filter(|value| !value.is_empty())
        .map(PathBuf::from)
        .unwrap_or_else(|| fallback_home.join(".codex"))
}

/// Returns false for model IDs that are clearly not conversational (embeddings,
/// TTS, image generation, ASR, moderation). Everything else is assumed to be a
/// chat model.
pub fn is_chat_model_id(id: &str) -> bool {
    const BLOCK: &[&str] = &[
        "whisper",
        "tts",
        "dall-e",
        "dalle",
        "embedding",
        "moderation",
        "audio",
        "image",
        "davinci",
        "babbage",
        "ada-",
    ];
    let lower = id.to_ascii_lowercase();
    !BLOCK.iter().any(|p| lower.contains(p))
}
