use std::path::PathBuf;

// ─── Paths ────────────────────────────────────────────────────────────────────

/// Root of the soul directory: `$XDG_CONFIG_HOME/kaku/soul/` (or
/// `~/.config/kaku/soul/` when XDG is not set).
pub(crate) fn soul_dir() -> PathBuf {
    kaku_config_dir().join("soul")
}

pub(crate) fn soul_path() -> PathBuf {
    soul_dir().join("SOUL.md")
}

pub(crate) fn style_path() -> PathBuf {
    soul_dir().join("STYLE.md")
}

pub(crate) fn skill_path() -> PathBuf {
    soul_dir().join("SKILL.md")
}

pub(crate) fn memory_path() -> PathBuf {
    soul_dir().join("MEMORY.md")
}

#[allow(dead_code)]
pub(crate) fn version_path() -> PathBuf {
    soul_dir().join(".version")
}

#[allow(dead_code)]
pub(crate) fn bootstrapped_path() -> PathBuf {
    soul_dir().join(".bootstrapped")
}

/// XDG-aware config root, matching `config::user_config_path()` parent.
fn kaku_config_dir() -> PathBuf {
    if let Some(xdg) = std::env::var_os("XDG_CONFIG_HOME") {
        PathBuf::from(xdg).join("kaku")
    } else {
        let home = std::env::var("HOME").unwrap_or_else(|_| ".".into());
        PathBuf::from(home).join(".config").join("kaku")
    }
}

// ─── Version sentinel ─────────────────────────────────────────────────────────

#[allow(dead_code)]
const SCHEMA_VERSION: u32 = 1;

#[allow(dead_code)]
fn read_schema_version() -> u32 {
    std::fs::read_to_string(version_path())
        .ok()
        .and_then(|s| s.trim().parse().ok())
        .unwrap_or(0)
}

#[allow(dead_code)]
fn write_schema_version(v: u32) {
    if let Err(e) = std::fs::write(version_path(), format!("{}\n", v)) {
        log::warn!("soul: failed to write .version: {e}");
    }
}

// ─── Migration ────────────────────────────────────────────────────────────────

/// Run on every AI overlay init. Idempotent.
#[allow(dead_code)]
pub(crate) fn migrate_if_needed() {
    let version = read_schema_version();
    if version >= SCHEMA_VERSION {
        return;
    }

    if let Err(e) = std::fs::create_dir_all(soul_dir()) {
        log::warn!("soul: failed to create soul dir: {e}");
        return;
    }

    // v0 -> v1: move legacy ai_chat_memory.md into soul/MEMORY.md
    if version == 0 {
        migrate_v0_to_v1();
    }

    write_schema_version(SCHEMA_VERSION);
}

fn migrate_v0_to_v1() {
    let legacy = kaku_config_dir().join("ai_chat_memory.md");
    let dest = memory_path();

    if legacy.exists() && !dest.exists() {
        match std::fs::rename(&legacy, &dest) {
            Ok(_) => log::info!("soul: migrated ai_chat_memory.md -> soul/MEMORY.md"),
            Err(e) => log::warn!("soul: migration rename failed: {e}"),
        }
    }

    // Write SOUL/STYLE/SKILL stubs so the user can find and edit them.
    write_stub_if_absent(
        &soul_path(),
        "# About Me\n\n\
         <!-- Tell Kaku who you are. This file ships into every system prompt.\n\
              The curator will never overwrite it. Edit freely. -->\n",
    );
    write_stub_if_absent(
        &style_path(),
        "# Voice & Style\n\n\
         <!-- Your preferred reply style, tone, and formatting preferences. -->\n",
    );
    write_stub_if_absent(
        &skill_path(),
        "# Operating Modes\n\n\
         <!-- What you typically work on and how Kaku should help. -->\n",
    );
}

#[allow(dead_code)]
fn write_stub_if_absent(path: &std::path::Path, content: &str) {
    if path.exists() {
        return;
    }
    if let Err(e) = std::fs::write(path, content) {
        log::warn!("soul: failed to write stub {}: {e}", path.display());
    }
}

// ─── Loading for prompts ──────────────────────────────────────────────────────

const SOUL_HARD_CAP: usize = 2_048;
const MEMORY_HARD_CAP: usize = 4_096;

/// Returns SOUL + STYLE + SKILL concatenated for injection into the cached
/// system prompt. Returns an empty string when all three files are absent or
/// contain only stub placeholder text.
pub(crate) fn load_for_prompt() -> String {
    let soul = read_capped(&soul_path(), SOUL_HARD_CAP);
    let style = read_capped(&style_path(), SOUL_HARD_CAP);
    let skill = read_capped(&skill_path(), SOUL_HARD_CAP);

    let parts: Vec<&str> = [soul.as_str(), style.as_str(), skill.as_str()]
        .iter()
        .copied()
        .filter(|s| !s.is_empty())
        .collect();

    parts.join("\n\n")
}

/// Returns MEMORY.md content for injection into the dynamic environment
/// message (kept out of the cached prefix so curator rewrites do not bust
/// the prompt-cache on every turn).
pub(crate) fn load_memory_for_env() -> String {
    read_capped(&memory_path(), MEMORY_HARD_CAP)
}

fn read_capped(path: &std::path::Path, cap: usize) -> String {
    let content = match std::fs::read_to_string(path) {
        Ok(s) => s,
        Err(_) => return String::new(),
    };

    // Skip stubs: files that only contain HTML comment placeholders.
    if is_stub_only(&content) {
        return String::new();
    }

    // Truncate at line boundary nearest to cap (never mid-word).
    if content.len() <= cap {
        return content.trim_end().to_string();
    }

    let mut truncated = String::new();
    for line in content.lines() {
        if truncated.len() + line.len() + 1 > cap {
            log::warn!("soul: {} truncated at {} bytes", path.display(), cap);
            break;
        }
        if !truncated.is_empty() {
            truncated.push('\n');
        }
        truncated.push_str(line);
    }
    truncated
}

fn is_stub_only(content: &str) -> bool {
    content
        .lines()
        .filter(|l| !l.trim().is_empty())
        .all(|l| l.trim_start().starts_with('#') || l.trim_start().starts_with("<!--"))
}

// ─── Bootstrap ────────────────────────────────────────────────────────────────

/// One-shot split of the onboarding answer into SOUL/STYLE/SKILL slots.
/// Sentinel-guarded: runs only once per install. Best-effort; failures are
/// logged and ignored so the user does not see errors.
#[allow(dead_code)]
pub(crate) fn bootstrap_from_onboarding(
    client: &crate::ai_client::AiClient,
    onboarding_reply: &str,
) {
    // Guard: only run once.
    if bootstrapped_path().exists() {
        return;
    }

    // Drop the sentinel first so a crash mid-run does not re-trigger.
    let _ = std::fs::write(bootstrapped_path(), b"");

    let cfg = client.config();
    let model = cfg
        .memory_curator_model
        .clone()
        .unwrap_or_else(|| cfg.chat_model.clone());

    let prompt = format!(
        "The user just answered an onboarding greeting that asked:\n\
         1. What should I call you?\n\
         2. What reply style do you prefer?\n\
         3. What do you typically work on?\n\n\
         Their answer:\n{reply}\n\n\
         Extract the content into three files. Return ONLY a JSON object with \
         keys \"soul\", \"style\", \"skill\". Each value is a Markdown string \
         ready to save directly. Use first-person prose, no bullet lists unless \
         natural. Keep each section under 300 words.\n\n\
         soul: one short paragraph about who they are (name, role, context).\n\
         style: their preferred reply style and tone.\n\
         skill: what they typically work on and how the assistant should help.\n\n\
         If a field cannot be determined from the answer, return an empty string \
         for that key. Do not invent information.",
        reply = onboarding_reply
    );

    let api_msgs = vec![
        crate::ai_client::ApiMessage::system(
            "You split a short onboarding answer into structured identity files.",
        ),
        crate::ai_client::ApiMessage::user(&prompt),
    ];

    let text = match client.complete_once(&model, &api_msgs) {
        Ok(t) => t,
        Err(e) => {
            log::warn!("soul bootstrap failed: {e}");
            return;
        }
    };

    // Parse the JSON response.
    let json: serde_json::Value = match serde_json::from_str(text.trim()) {
        Ok(v) => v,
        Err(_) => {
            // Try to extract JSON from a fenced code block.
            let inner = text
                .lines()
                .skip_while(|l| !l.starts_with('{'))
                .collect::<Vec<_>>()
                .join("\n");
            match serde_json::from_str(inner.trim()) {
                Ok(v) => v,
                Err(e) => {
                    log::warn!("soul bootstrap: could not parse JSON: {e}");
                    return;
                }
            }
        }
    };

    let write_field = |path: &std::path::Path, key: &str| {
        if let Some(val) = json.get(key).and_then(|v| v.as_str()) {
            if !val.trim().is_empty() {
                let _ = std::fs::write(path, format!("{}\n", val.trim()));
            }
        }
    };

    write_field(&soul_path(), "soul");
    write_field(&style_path(), "style");
    write_field(&skill_path(), "skill");

    log::info!("soul: bootstrap complete");
}
