//! Bridges shell `#` generation and automatic quick-fix requests to `AiClient`.
//!
//! Bundled Lua owns the existing per-pane spinner, polling, parsing, and command
//! safety UX. It writes a bounded request file and emits an internal event;
//! this module runs the request through the same provider-aware Rust transport
//! as Cmd+L, then writes the response shape the Lua poller already understands.

use crate::ai_client::{AiClient, ApiMessage, AssistantConfig};
use anyhow::{Context, Result};
use std::collections::HashSet;
use std::io::Read;
use std::path::{Path, PathBuf};
use std::sync::{Mutex, OnceLock};

pub(crate) const EVENT_PREFIX: &str = "kaku-ai-inline-request:";
const NOT_CONFIGURED_STATUS: &str = "78";
const FAILURE_STATUS: &str = "1";
const SUCCESS_STATUS: &str = "0";
const MAX_REQUEST_BYTES: u64 = 128 * 1024;

#[derive(Debug)]
struct JobPaths {
    request: PathBuf,
    response: PathBuf,
    stderr: PathBuf,
    status: PathBuf,
}

enum CompletionError {
    NotConfigured,
    Failed(&'static str),
}

enum CompletionOutcome {
    Success(String),
    NotConfigured,
    Failed(String),
}

fn kaku_state_dir() -> PathBuf {
    config::HOME_DIR.join(".config").join("kaku")
}

fn classify_config_load_error(config_file_exists: bool) -> CompletionError {
    if config_file_exists {
        CompletionError::Failed("assistant configuration could not be loaded")
    } else {
        CompletionError::NotConfigured
    }
}

struct InFlightJob {
    job_id: String,
}

fn in_flight_jobs() -> &'static Mutex<HashSet<String>> {
    static JOBS: OnceLock<Mutex<HashSet<String>>> = OnceLock::new();
    JOBS.get_or_init(|| Mutex::new(HashSet::new()))
}

impl InFlightJob {
    fn claim(job_id: &str) -> Result<Self> {
        let mut jobs = in_flight_jobs()
            .lock()
            .map_err(|_| anyhow::anyhow!("inline AI job tracker is unavailable"))?;
        if !jobs.insert(job_id.to_string()) {
            anyhow::bail!("inline AI job is already running");
        }
        Ok(Self {
            job_id: job_id.to_string(),
        })
    }
}

impl Drop for InFlightJob {
    fn drop(&mut self) {
        if let Ok(mut jobs) = in_flight_jobs().lock() {
            jobs.remove(&self.job_id);
        }
    }
}

fn claim_job(jobs_dir: &Path, job_id: &str) -> Result<InFlightJob> {
    let paths = job_paths(jobs_dir, job_id)?;
    let metadata = std::fs::symlink_metadata(&paths.request)
        .with_context(|| format!("stat {}", paths.request.display()))?;
    if !metadata.file_type().is_file() {
        anyhow::bail!("inline AI request is not a regular file");
    }
    if paths.status.exists() {
        anyhow::bail!("inline AI job is already complete");
    }
    InFlightJob::claim(job_id)
}

fn job_paths(jobs_dir: &Path, job_id: &str) -> Result<JobPaths> {
    let parts = job_id.split('-').collect::<Vec<_>>();
    let valid = parts.len() == 2
        && parts
            .iter()
            .all(|part| !part.is_empty() && part.bytes().all(|byte| byte.is_ascii_digit()));
    if !valid || job_id.len() > 48 {
        anyhow::bail!("invalid inline AI job id");
    }

    let base = jobs_dir.join(format!("ai_fix_{job_id}"));
    Ok(JobPaths {
        request: base.with_extension("request.json"),
        response: base.with_extension("response.json"),
        stderr: base.with_extension("stderr.log"),
        status: base.with_extension("status"),
    })
}

fn parse_request(raw: &str) -> Result<(Vec<ApiMessage>, std::time::Duration)> {
    let payload: serde_json::Value =
        serde_json::from_str(raw).context("parse inline AI request")?;
    let messages = payload
        .get("messages")
        .and_then(|value| value.as_array())
        .filter(|messages| !messages.is_empty())
        .ok_or_else(|| anyhow::anyhow!("inline AI request is missing messages"))?
        .iter()
        .map(|message| {
            if !message.is_object() || message.get("role").and_then(|role| role.as_str()).is_none()
            {
                anyhow::bail!("inline AI request contains an invalid message");
            }
            Ok(ApiMessage(message.clone()))
        })
        .collect::<Result<Vec<_>>>()?;
    let timeout_secs = payload
        .get("timeout_secs")
        .and_then(|value| value.as_u64())
        .unwrap_or(30)
        .clamp(1, 600);

    Ok((messages, std::time::Duration::from_secs(timeout_secs)))
}

fn auth_is_configured(auth_type: &str, api_key: &str) -> bool {
    matches!(auth_type, "codex" | "copilot") || !api_key.trim().is_empty()
}

fn read_request(paths: &JobPaths) -> Result<String> {
    let file = std::fs::File::open(&paths.request)
        .with_context(|| format!("open {}", paths.request.display()))?;
    let mut bytes = Vec::new();
    file.take(MAX_REQUEST_BYTES + 1)
        .read_to_end(&mut bytes)
        .with_context(|| format!("read {}", paths.request.display()))?;
    if bytes.len() as u64 > MAX_REQUEST_BYTES {
        anyhow::bail!("inline AI request exceeds {MAX_REQUEST_BYTES} bytes");
    }
    String::from_utf8(bytes).context("inline AI request is not UTF-8")
}

fn persist_outcome(paths: &JobPaths, outcome: CompletionOutcome) -> Result<()> {
    match outcome {
        CompletionOutcome::Success(content) => {
            let response = serde_json::json!({
                "choices": [{
                    "message": {
                        "role": "assistant",
                        "content": content,
                    }
                }]
            });
            std::fs::write(&paths.response, serde_json::to_vec(&response)?)
                .with_context(|| format!("write {}", paths.response.display()))?;
            std::fs::write(&paths.status, SUCCESS_STATUS)
                .with_context(|| format!("write {}", paths.status.display()))?;
        }
        CompletionOutcome::NotConfigured => {
            std::fs::write(&paths.status, NOT_CONFIGURED_STATUS)
                .with_context(|| format!("write {}", paths.status.display()))?;
        }
        CompletionOutcome::Failed(message) => {
            std::fs::write(&paths.stderr, message)
                .with_context(|| format!("write {}", paths.stderr.display()))?;
            std::fs::write(&paths.status, FAILURE_STATUS)
                .with_context(|| format!("write {}", paths.status.display()))?;
        }
    }
    Ok(())
}

fn process_job_with<F>(jobs_dir: &Path, job_id: &str, complete: F) -> Result<()>
where
    F: FnOnce(&[ApiMessage], std::time::Duration) -> std::result::Result<String, CompletionError>,
{
    let paths = job_paths(jobs_dir, job_id)?;
    let outcome = match read_request(&paths).and_then(|raw| parse_request(&raw)) {
        Ok((messages, timeout)) => match complete(&messages, timeout) {
            Ok(content) => CompletionOutcome::Success(content),
            Err(CompletionError::NotConfigured) => CompletionOutcome::NotConfigured,
            Err(CompletionError::Failed(message)) => CompletionOutcome::Failed(message.to_string()),
        },
        Err(_) => CompletionOutcome::Failed("invalid inline AI request".to_string()),
    };

    persist_outcome(&paths, outcome)
}

fn process_job(jobs_dir: &Path, job_id: &str) -> Result<()> {
    process_job_with(jobs_dir, job_id, |messages, timeout| {
        let config = AssistantConfig::load()
            .map_err(|_| classify_config_load_error(AssistantConfig::file_exists()))?;
        if !auth_is_configured(&config.auth_type, &config.api_key) {
            return Err(CompletionError::NotConfigured);
        }

        let model = config
            .fast_model
            .clone()
            .unwrap_or_else(|| config.chat_model.clone());
        let client = AiClient::new_with_timeout(config, timeout);
        client
            .complete_once(&model, messages)
            .map_err(|_| CompletionError::Failed("assistant request failed"))
    })
}

pub(crate) fn spawn_job(job_id: &str) -> Result<()> {
    let jobs_dir = kaku_state_dir().join("ai_jobs");
    let claim = claim_job(&jobs_dir, job_id)?;

    let job_id = job_id.to_string();
    crate::thread_util::spawn_with_pool(move || {
        let _claim = claim;
        if let Err(err) = process_job(&jobs_dir, &job_id) {
            log::error!("inline AI job {job_id} failed: {err:#}");
        }
    });
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn auth_accepts_codex_without_api_key() {
        assert!(auth_is_configured("codex", ""));
        assert!(auth_is_configured("copilot", ""));
        assert!(auth_is_configured("api_key", "token"));
        assert!(!auth_is_configured("api_key", ""));
        assert!(!auth_is_configured("custom", ""));
    }

    #[test]
    fn missing_config_file_is_not_reported_as_transport_failure() {
        assert!(matches!(
            classify_config_load_error(false),
            CompletionError::NotConfigured
        ));
        assert!(matches!(
            classify_config_load_error(true),
            CompletionError::Failed(_)
        ));
    }

    #[test]
    fn job_reuses_completion_transport_and_writes_lua_response_shape() {
        let dir = tempfile::tempdir().unwrap();
        let job_id = "1721827200-1";
        let paths = job_paths(dir.path(), job_id).unwrap();
        let request = serde_json::json!({
            "messages": [
                {"role": "system", "content": "Return JSON"},
                {"role": "user", "content": "list files"}
            ],
            "timeout_secs": 9
        });
        std::fs::write(&paths.request, serde_json::to_vec(&request).unwrap()).unwrap();

        process_job_with(dir.path(), job_id, |messages, timeout| {
            assert_eq!(messages.len(), 2);
            assert_eq!(messages[1].0["content"], "list files");
            assert_eq!(timeout, std::time::Duration::from_secs(9));
            Ok(r#"{"summary":"List files","command":"ls"}"#.to_string())
        })
        .unwrap();

        assert_eq!(
            std::fs::read_to_string(&paths.status).unwrap(),
            SUCCESS_STATUS
        );
        let response: serde_json::Value =
            serde_json::from_slice(&std::fs::read(&paths.response).unwrap()).unwrap();
        assert_eq!(
            response["choices"][0]["message"]["content"],
            r#"{"summary":"List files","command":"ls"}"#
        );
    }

    #[test]
    fn job_reports_missing_api_configuration_without_network_request() {
        let dir = tempfile::tempdir().unwrap();
        let job_id = "1721827200-2";
        let paths = job_paths(dir.path(), job_id).unwrap();
        let request = serde_json::json!({
            "messages": [{"role": "user", "content": "list files"}]
        });
        std::fs::write(&paths.request, serde_json::to_vec(&request).unwrap()).unwrap();

        process_job_with(dir.path(), job_id, |_, _| {
            Err(CompletionError::NotConfigured)
        })
        .unwrap();

        assert_eq!(
            std::fs::read_to_string(&paths.status).unwrap(),
            NOT_CONFIGURED_STATUS
        );
        assert!(!paths.response.exists());
    }

    #[test]
    fn oversized_request_is_rejected_before_transport() {
        let dir = tempfile::tempdir().unwrap();
        let job_id = "1721827200-3";
        let paths = job_paths(dir.path(), job_id).unwrap();
        std::fs::write(&paths.request, vec![b'x'; MAX_REQUEST_BYTES as usize + 1]).unwrap();

        process_job_with(dir.path(), job_id, |_, _| {
            panic!("oversized request reached transport")
        })
        .unwrap();

        assert_eq!(
            std::fs::read_to_string(&paths.status).unwrap(),
            FAILURE_STATUS
        );
        assert_eq!(
            std::fs::read_to_string(&paths.stderr).unwrap(),
            "invalid inline AI request"
        );
    }

    #[test]
    fn bridge_contract_stays_in_sync_with_bundled_lua() {
        let lua = include_str!("../../assets/macos/Kaku.app/Contents/Resources/kaku.lua");
        assert!(lua.contains(&format!(
            "wezterm.action.EmitEvent(\"{}\" .. job.id)",
            EVENT_PREFIX
        )));
        assert!(lua.contains("local ai_inline_not_configured_status = 78"));
        assert!(lua.contains("mkdir -p %q && chmod 700 %q"));
        assert!(lua.contains("chmod 600 %q"));
        assert!(lua.contains("local xdg_config_home = os.getenv(\"XDG_CONFIG_HOME\")"));
        assert!(lua.contains("kaku_state_dir .. \"/ai_debug.log\""));
        assert!(lua.contains("wezterm.time.now():format_utc(\"%s%f\")"));
        assert!(lua.contains("cleanup_ai_fix_job_files(job)"));
        assert!(lua.contains("kaku-toast-ai-clear-progress"));
        assert!(lua.contains("trusted_ai_last_command_by_pane"));
        assert!(!lua.contains("vars.kaku_last_cmd"));
        assert!(!lua.contains("/tmp/kaku_ai_debug.log"));
        assert!(!lua.contains("model = model,"));
    }

    #[test]
    fn job_id_cannot_escape_jobs_directory() {
        let dir = tempfile::tempdir().unwrap();
        for invalid in ["", "../1-1", "1", "1-2-3", "abc-1", "1-x"] {
            assert!(job_paths(dir.path(), invalid).is_err());
        }
    }

    #[test]
    fn job_claim_requires_one_unfinished_regular_request() {
        let dir = tempfile::tempdir().unwrap();
        let job_id = "1721827200-99991";
        let paths = job_paths(dir.path(), job_id).unwrap();

        assert!(claim_job(dir.path(), job_id).is_err());
        std::fs::write(&paths.request, b"{}").unwrap();
        let first = claim_job(dir.path(), job_id).unwrap();
        assert!(claim_job(dir.path(), job_id).is_err());
        drop(first);
        std::fs::write(&paths.status, SUCCESS_STATUS).unwrap();
        assert!(claim_job(dir.path(), job_id).is_err());
    }

    #[cfg(unix)]
    #[test]
    fn job_claim_rejects_symlinked_request() {
        use std::os::unix::fs::symlink;

        let dir = tempfile::tempdir().unwrap();
        let job_id = "1721827200-99992";
        let paths = job_paths(dir.path(), job_id).unwrap();
        let target = dir.path().join("target.json");
        std::fs::write(&target, b"{}").unwrap();
        symlink(target, &paths.request).unwrap();

        assert!(claim_job(dir.path(), job_id).is_err());
    }

    #[test]
    fn completion_errors_written_for_lua_are_redacted() {
        let dir = tempfile::tempdir().unwrap();
        let job_id = "1721827200-4";
        let paths = job_paths(dir.path(), job_id).unwrap();
        let request = serde_json::json!({
            "messages": [{"role": "user", "content": "list files"}]
        });
        std::fs::write(&paths.request, serde_json::to_vec(&request).unwrap()).unwrap();

        process_job_with(dir.path(), job_id, |_, _| {
            Err(CompletionError::Failed("assistant request failed"))
        })
        .unwrap();

        assert_eq!(
            std::fs::read_to_string(&paths.stderr).unwrap(),
            "assistant request failed"
        );
    }
}
