//! Memory, soul, onboarding, and spill-file helpers.

use anyhow::Result;
use std::io::Write;
use std::os::unix::fs::OpenOptionsExt;
use std::path::PathBuf;

/// Path to the per-user memory file shared between the overlay and the CLI curator.
pub(crate) fn memory_file_path() -> PathBuf {
    crate::soul::memory_path()
}

/// Presence of this file indicates the user has already seen the onboarding greeting.
#[allow(dead_code)]
pub(crate) fn onboarding_flag_path() -> PathBuf {
    if let Some(xdg) = std::env::var_os("XDG_CONFIG_HOME") {
        PathBuf::from(xdg).join("kaku").join("ai_chat_onboarded")
    } else {
        let home = std::env::var("HOME").unwrap_or_else(|_| ".".into());
        PathBuf::from(home)
            .join(".config")
            .join("kaku")
            .join("ai_chat_onboarded")
    }
}

fn read_soul_file(path: &std::path::Path, label: &str) -> String {
    match std::fs::read_to_string(path) {
        Ok(content) if !content.trim().is_empty() => {
            format!("## {}\n\n{}", label, content.trim_end())
        }
        _ => String::new(),
    }
}

pub(super) fn exec_soul_read(file: &str) -> Result<String> {
    Ok(match file {
        "soul" => read_soul_file(&crate::soul::soul_path(), "SOUL"),
        "style" => read_soul_file(&crate::soul::style_path(), "STYLE"),
        "skill" => read_soul_file(&crate::soul::skill_path(), "SKILL"),
        "memory" => read_soul_file(&crate::soul::memory_path(), "MEMORY"),
        _ => {
            let soul = read_soul_file(&crate::soul::soul_path(), "SOUL");
            let style = read_soul_file(&crate::soul::style_path(), "STYLE");
            let skill = read_soul_file(&crate::soul::skill_path(), "SKILL");
            let memory = read_soul_file(&crate::soul::memory_path(), "MEMORY");
            vec![soul, style, skill, memory]
                .into_iter()
                .filter(|s| !s.is_empty())
                .collect::<Vec<String>>()
                .join("\n\n---\n\n")
        }
    })
}

// ─── Spill-file registry ──────────────────────────────────────────────────────

static SPILL_FILES: std::sync::Mutex<Vec<PathBuf>> = std::sync::Mutex::new(Vec::new());

/// Remove all temp spill files created during this session.
pub fn cleanup_spill_files() {
    if let Ok(mut files) = SPILL_FILES.lock() {
        for path in files.drain(..) {
            let _ = std::fs::remove_file(&path);
        }
    }
}

/// Truncate `result` to `cap` bytes. When the result exceeds the cap, the full
/// content is spilled to a temp file so the model can `fs_read` it afterward.
pub(super) fn truncate_and_spill(result: String, cap: usize) -> Result<String> {
    if result.len() <= cap {
        return Ok(result);
    }
    let boundary = (0..=cap)
        .rev()
        .find(|&i| result.is_char_boundary(i))
        .unwrap_or(0);
    let truncated = &result[..boundary];
    let tmp_path = std::env::temp_dir().join(format!(
        "kaku_tool_{}_{}.txt",
        std::process::id(),
        std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .map(|d| d.as_nanos())
            .unwrap_or(0)
    ));
    let write_result = std::fs::OpenOptions::new()
        .create_new(true)
        .write(true)
        .mode(0o600)
        .open(&tmp_path)
        .and_then(|mut file| file.write_all(result.as_bytes()));
    let note = if write_result.is_ok() {
        if let Ok(mut registry) = SPILL_FILES.lock() {
            registry.push(tmp_path.clone());
        }
        format!(
            "\n[truncated: {} of {} bytes shown]\
             \n[spill: {}]\
             \n[hint: use fs_read(\"{}\") to read the rest]",
            cap,
            result.len(),
            tmp_path.display(),
            tmp_path.display()
        )
    } else {
        format!(
            "\n[truncated: {} bytes shown of {} total]",
            cap,
            result.len()
        )
    };
    Ok(format!("{}{}", truncated, note))
}
