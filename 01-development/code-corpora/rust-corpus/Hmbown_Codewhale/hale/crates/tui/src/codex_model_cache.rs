//! Secret-free OpenAI Codex / ChatGPT OAuth model roster discovery.
//!
//! The Codex CLI keeps its account-scoped roster in `models_cache.json`.
//! CodeWhale reads model metadata only. An explicit update can ask the Codex
//! CLI's documented stdio API for its ChatGPT account roster; credentials stay
//! with Codex. The query time is not proof of an upstream catalog refresh.

use std::collections::HashSet;
use std::io::Read;
use std::path::{Path, PathBuf};
use std::process::Stdio;
use std::sync::Mutex;
use std::time::SystemTime;

#[cfg(unix)]
use std::os::unix::fs::OpenOptionsExt;

use chrono::{DateTime, Duration, Utc};
use serde::{Deserialize, Serialize};
use serde_json::{Value, json};
use sha2::{Digest, Sha256};
use tokio::io::{AsyncBufReadExt, AsyncReadExt, AsyncWriteExt, BufReader};

use crate::config::DEFAULT_OPENAI_CODEX_MODEL;

const MODEL_CACHE_FILE: &str = "models_cache.json";
const MAX_MODEL_CACHE_BYTES: u64 = 4 * 1024 * 1024;
/// Codex refreshes its own cache much more frequently. CodeWhale is an offline
/// consumer, so it accepts a last-known account roster for one day before
/// falling back to the single conservative compatibility model.
const MODEL_CACHE_MAX_AGE: Duration = Duration::hours(24);
const MAX_FUTURE_CLOCK_SKEW: Duration = Duration::minutes(5);

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum CodexModelCacheFreshness {
    Fresh,
    Missing,
    Stale,
    Invalid,
}

impl CodexModelCacheFreshness {
    #[must_use]
    pub(crate) const fn picker_label(self) -> &'static str {
        match self {
            Self::Fresh => "ChatGPT OAuth",
            Self::Missing => "OAuth roster missing · fallback",
            Self::Stale => "OAuth roster stale · fallback",
            Self::Invalid => "OAuth roster invalid · fallback",
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub(crate) struct CodexModelRoster {
    pub(crate) models: Vec<CodexModelMetadata>,
    pub(crate) freshness: CodexModelCacheFreshness,
    pub(crate) fetched_at: Option<DateTime<Utc>>,
    pub(crate) observed_at: Option<DateTime<Utc>>,
    pub(crate) source: &'static str,
    /// Applies only to observations from `model/list`, not Codex's own cache.
    pub(crate) observation_persisted: bool,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub(crate) struct CodexModelMetadata {
    pub(crate) id: String,
    pub(crate) context_window: Option<u32>,
    pub(crate) reasoning: Option<bool>,
    /// Effort names the roster advertises for this model, lowercased and in
    /// the order the cache lists them (`low`, `medium`, `high`, `xhigh`,
    /// `max`, `ultra`). Empty when the roster published none, which keeps the
    /// caller on its static ladder instead of inventing tiers.
    pub(crate) efforts: Vec<String>,
}

impl CodexModelRoster {
    fn fallback(freshness: CodexModelCacheFreshness, fetched_at: Option<DateTime<Utc>>) -> Self {
        Self {
            models: vec![CodexModelMetadata {
                id: DEFAULT_OPENAI_CODEX_MODEL.to_string(),
                context_window: None,
                reasoning: None,
                efforts: Vec::new(),
            }],
            freshness,
            fetched_at,
            observed_at: None,
            source: "codex_cli_cache",
            observation_persisted: false,
        }
    }

    #[must_use]
    pub(crate) fn model_ids(&self) -> Vec<String> {
        self.models.iter().map(|model| model.id.clone()).collect()
    }

    #[must_use]
    pub(crate) fn metadata_for(&self, id: &str) -> Option<&CodexModelMetadata> {
        self.models
            .iter()
            .find(|model| model.id.eq_ignore_ascii_case(id.trim()))
    }

    /// The roster's preferred model: the highest-priority entry of a fresh
    /// roster. Missing/stale/invalid rosters yield `None` so callers keep
    /// the static seed default (#5034).
    #[must_use]
    pub(crate) fn preferred_model_id(&self) -> Option<&str> {
        if self.freshness != CodexModelCacheFreshness::Fresh {
            return None;
        }
        self.models.first().map(|model| model.id.as_str())
    }
}

#[derive(Debug, Deserialize)]
struct CacheFile {
    fetched_at: DateTime<Utc>,
    #[serde(default)]
    models: Vec<CacheModel>,
}

#[derive(Debug, Deserialize)]
struct CacheModel {
    slug: String,
    #[serde(default)]
    priority: Option<i64>,
    #[serde(default)]
    context_window: Option<u32>,
    #[serde(default)]
    supported_reasoning_levels: Option<Vec<CacheReasoningLevel>>,
    /// `hide` marks a roster entry the vendor does not offer for selection
    /// (`gpt-reserve`, `codex-auto-review`). Absent means listable.
    #[serde(default)]
    visibility: Option<String>,
}

#[derive(Debug, Deserialize)]
struct CacheReasoningLevel {
    #[serde(default)]
    effort: Option<String>,
}

/// Resolve the Codex home without consulting OAuth-file overrides.
///
/// `OPENAI_CODEX_AUTH_FILE` intentionally does not participate: it may point
/// at a standalone test/credential file while the model roster still belongs
/// to `$CODEX_HOME` (or the default `~/.codex`).
#[must_use]
pub(crate) fn codex_home_path() -> PathBuf {
    std::env::var_os("CODEX_HOME")
        .filter(|value| !value.is_empty())
        .map(PathBuf::from)
        .unwrap_or_else(|| {
            crate::config::effective_home_dir()
                .unwrap_or_else(|| PathBuf::from("."))
                .join(".codex")
        })
}

/// Last parsed roster, keyed by the identity of the file it came from.
///
/// The roster is read from the picker's render path, and the cache file is a
/// couple hundred kilobytes of JSON (it carries full instruction templates),
/// so parsing it per frame is visible as input lag while a picker is open.
/// The key is the resolved path plus the file's mtime and length, so a Codex
/// refresh is picked up on the next call and a test pointing `CODEX_HOME`
/// somewhere else never reads another test's entry.
type RosterCacheKey = Vec<(PathBuf, Option<SystemTime>, u64)>;
static ROSTER_MEMO: Mutex<Option<(RosterCacheKey, CodexModelRoster)>> = Mutex::new(None);

#[must_use]
pub(crate) fn model_roster() -> CodexModelRoster {
    let home = codex_home_path();
    let snapshot_path = cli_snapshot_path(&home);
    let key: RosterCacheKey = std::iter::once(home.join(MODEL_CACHE_FILE))
        .chain(snapshot_path.iter().cloned())
        .map(|path| match std::fs::symlink_metadata(&path) {
            Ok(metadata) => (path, metadata.modified().ok(), metadata.len()),
            Err(_) => (path, None, 0),
        })
        .collect();

    // A `Fresh` roster ages into `Stale` on the clock, not on a file change,
    // so the memo is only trusted while the entry it was built from still
    // reads fresh.
    if let Ok(memo) = ROSTER_MEMO.lock()
        && let Some((cached_key, roster)) = memo.as_ref()
        && *cached_key == key
        && roster.freshness == CodexModelCacheFreshness::Fresh
        && roster
            .fetched_at
            .or(roster.observed_at)
            .is_some_and(|fetched| Utc::now().signed_duration_since(fetched) <= MODEL_CACHE_MAX_AGE)
    {
        return roster.clone();
    }

    let now = Utc::now();
    let mut roster = load_model_roster_from_home_at(&home, now);
    if let Some(snapshot) = snapshot_path.and_then(|path| load_cli_snapshot(&path, now))
        && (roster.freshness != CodexModelCacheFreshness::Fresh
            || snapshot.observed_at > roster.fetched_at)
    {
        roster = snapshot;
    }
    if let Ok(mut memo) = ROSTER_MEMO.lock() {
        *memo = Some((key, roster.clone()));
    }
    roster
}

fn load_model_roster_from_home_at(home: &Path, now: DateTime<Utc>) -> CodexModelRoster {
    let path = home.join(MODEL_CACHE_FILE);
    let bytes = match read_cache_bytes(&path) {
        Ok(bytes) => bytes,
        Err(freshness) => return CodexModelRoster::fallback(freshness, None),
    };
    let cache: CacheFile = match serde_json::from_slice(&bytes) {
        Ok(cache) => cache,
        Err(_) => return CodexModelRoster::fallback(CodexModelCacheFreshness::Invalid, None),
    };

    let age = now.signed_duration_since(cache.fetched_at);
    if age < -MAX_FUTURE_CLOCK_SKEW {
        return CodexModelRoster::fallback(
            CodexModelCacheFreshness::Invalid,
            Some(cache.fetched_at),
        );
    }
    if age > MODEL_CACHE_MAX_AGE {
        return CodexModelRoster::fallback(CodexModelCacheFreshness::Stale, Some(cache.fetched_at));
    }
    // Codex owns this cache, but an observed file-based login replacement
    // after its fetch invalidates the account attribution. No credential
    // content is opened to check that boundary.
    if std::fs::metadata(home.join("auth.json"))
        .and_then(|metadata| metadata.modified())
        .is_ok_and(|modified| DateTime::<Utc>::from(modified) > cache.fetched_at)
    {
        return CodexModelRoster::fallback(CodexModelCacheFreshness::Stale, Some(cache.fetched_at));
    }

    let mut indexed: Vec<_> = cache.models.into_iter().enumerate().collect();
    indexed.sort_by_key(|(index, model)| (model.priority.unwrap_or(i64::MAX), *index));

    let mut seen = HashSet::new();
    let mut models = Vec::new();
    for (_, model) in indexed {
        let slug = model.slug.trim();
        if !valid_model_id(slug) {
            continue;
        }
        if model
            .visibility
            .as_deref()
            .is_some_and(|visibility| visibility.trim().eq_ignore_ascii_case("hide"))
        {
            continue;
        }
        let identity = slug.to_ascii_lowercase();
        if seen.insert(identity) {
            let efforts: Vec<String> = model
                .supported_reasoning_levels
                .as_ref()
                .map(|levels| {
                    levels
                        .iter()
                        .filter_map(|level| level.effort.as_deref())
                        .map(|effort| effort.trim().to_ascii_lowercase())
                        .filter(|effort| !effort.is_empty())
                        .collect()
                })
                .unwrap_or_default();
            models.push(CodexModelMetadata {
                id: slug.to_string(),
                context_window: model
                    .context_window
                    .filter(|window| (1..=16_000_000).contains(window)),
                reasoning: model
                    .supported_reasoning_levels
                    .as_ref()
                    .map(|levels| !levels.is_empty()),
                efforts,
            });
        }
    }
    if models.is_empty() {
        return CodexModelRoster::fallback(
            CodexModelCacheFreshness::Invalid,
            Some(cache.fetched_at),
        );
    }

    CodexModelRoster {
        models,
        freshness: CodexModelCacheFreshness::Fresh,
        fetched_at: Some(cache.fetched_at),
        observed_at: None,
        source: "codex_cli_cache",
        observation_persisted: false,
    }
}

fn read_cache_bytes(path: &Path) -> Result<Vec<u8>, CodexModelCacheFreshness> {
    let path_metadata = match std::fs::symlink_metadata(path) {
        Ok(metadata) => metadata,
        Err(error) if error.kind() == std::io::ErrorKind::NotFound => {
            return Err(CodexModelCacheFreshness::Missing);
        }
        Err(_) => return Err(CodexModelCacheFreshness::Invalid),
    };
    if !path_metadata.file_type().is_file() || path_metadata.len() > MAX_MODEL_CACHE_BYTES {
        return Err(CodexModelCacheFreshness::Invalid);
    }
    let mut file = match open_cache_file(path) {
        Ok(file) => file,
        Err(_) => return Err(CodexModelCacheFreshness::Invalid),
    };
    let metadata = match file.metadata() {
        Ok(metadata) => metadata,
        Err(_) => return Err(CodexModelCacheFreshness::Invalid),
    };
    if !metadata.file_type().is_file() || metadata.len() > MAX_MODEL_CACHE_BYTES {
        return Err(CodexModelCacheFreshness::Invalid);
    }

    let mut bytes = Vec::with_capacity(metadata.len().min(MAX_MODEL_CACHE_BYTES) as usize);
    if file
        .by_ref()
        .take(MAX_MODEL_CACHE_BYTES + 1)
        .read_to_end(&mut bytes)
        .is_err()
        || bytes.len() as u64 > MAX_MODEL_CACHE_BYTES
    {
        return Err(CodexModelCacheFreshness::Invalid);
    }
    Ok(bytes)
}

fn open_cache_file(path: &Path) -> std::io::Result<std::fs::File> {
    let mut options = std::fs::OpenOptions::new();
    options.read(true);
    #[cfg(unix)]
    options.custom_flags(libc::O_NOFOLLOW);
    options.open(path)
}

fn valid_model_id(value: &str) -> bool {
    !value.is_empty()
        && value.len() <= 256
        && value.bytes().any(|byte| byte.is_ascii_alphanumeric())
        && value.bytes().all(|byte| {
            byte.is_ascii_alphanumeric() || matches!(byte, b'.' | b'_' | b':' | b'/' | b'-')
        })
}

#[derive(Serialize, Deserialize)]
struct CliSnapshot {
    observed_at: DateTime<Utc>,
    models: Vec<CodexModelMetadata>,
}

fn cli_snapshot_path(home: &Path) -> Option<PathBuf> {
    let catalog_path = crate::models_dev_live::cache_path()?;
    let home = std::fs::canonicalize(home).ok()?;
    // A URL sanitizer collapses absolute filesystem paths to its single
    // invalid-URL value. Hash exact OS path bytes instead, with a new domain
    // so the old unscoped observations are never imported.
    let mut identity = Sha256::new();
    identity.update(b"codewhale-codex-observation-v2\0");
    identity.update(home.as_os_str().as_encoded_bytes());
    identity.update(b"\0");
    // Bind offline observations to the file-based login's version without
    // reading tokens. Keyring-only accounts cannot prove an offline account
    // binding here; their live command can still load a roster, with skipped
    // persistence disclosed by the receipt.
    let auth = std::fs::metadata(home.join("auth.json")).ok()?;
    if !auth.is_file() {
        return None;
    }
    identity.update(auth.len().to_le_bytes());
    identity.update(
        auth.modified()
            .ok()?
            .duration_since(SystemTime::UNIX_EPOCH)
            .ok()?
            .as_nanos()
            .to_le_bytes(),
    );
    #[cfg(unix)]
    {
        use std::os::unix::fs::MetadataExt;
        identity.update(auth.dev().to_le_bytes());
        identity.update(auth.ino().to_le_bytes());
        identity.update(auth.ctime().to_le_bytes());
        identity.update(auth.ctime_nsec().to_le_bytes());
    }
    let identity: String = identity
        .finalize()
        .iter()
        .map(|byte| format!("{byte:02x}"))
        .collect();
    Some(
        catalog_path
            .parent()?
            .join(format!("codex-{identity}.json")),
    )
}

fn valid_effort(effort: &str) -> bool {
    !effort.is_empty()
        && effort.len() <= 32
        && effort
            .bytes()
            .all(|b| b.is_ascii_alphanumeric() || matches!(b, b'_' | b'-'))
}

fn load_cli_snapshot(path: &Path, now: DateTime<Utc>) -> Option<CodexModelRoster> {
    let snapshot: CliSnapshot = serde_json::from_slice(&read_cache_bytes(path).ok()?).ok()?;
    let age = now.signed_duration_since(snapshot.observed_at);
    if age < -MAX_FUTURE_CLOCK_SKEW
        || age > MODEL_CACHE_MAX_AGE
        || snapshot.models.is_empty()
        || snapshot.models.iter().any(|model| {
            !valid_model_id(&model.id)
                || model.efforts.len() > 16
                || model.efforts.iter().any(|effort| !valid_effort(effort))
                || model
                    .context_window
                    .is_some_and(|window| !(1..=16_000_000).contains(&window))
        })
    {
        return None;
    }
    Some(CodexModelRoster {
        models: snapshot.models,
        freshness: CodexModelCacheFreshness::Fresh,
        fetched_at: None,
        observed_at: Some(snapshot.observed_at),
        source: "codex_app_server",
        observation_persisted: true,
    })
}

/// Ask the credential-owning CLI for its account roster. This does not import
/// tokens, force token refresh, start a thread, or make an inference request.
pub(crate) async fn update_from_codex_cli() -> Result<CodexModelRoster, &'static str> {
    update_from_codex_command(
        tokio::process::Command::new("codex"),
        std::time::Duration::from_secs(20),
    )
    .await
}

async fn update_from_codex_command(
    command: tokio::process::Command,
    timeout: std::time::Duration,
) -> Result<CodexModelRoster, &'static str> {
    let home = codex_home_path();
    let path = cli_snapshot_path(&home);
    let mut roster = query_codex_cli(command, timeout, &model_roster()).await?;
    let Some(path) = path.filter(|before| cli_snapshot_path(&home).as_ref() == Some(before)) else {
        // A login changed while querying, or its version cannot be observed.
        // Preserve the command's live result, never an unbound offline copy.
        return Ok(roster);
    };
    let snapshot = CliSnapshot {
        observed_at: roster.observed_at.ok_or("codex_invalid_response")?,
        models: roster.models.clone(),
    };
    let encoded = serde_json::to_vec(&snapshot).map_err(|_| "cache_write_failed")?;
    if encoded.len() as u64 > MAX_MODEL_CACHE_BYTES {
        return Err("codex_response_too_large");
    }
    codewhale_config::persistence::atomic_write(&path, &encoded)
        .map_err(|_| "cache_write_failed")?;
    if let Ok(mut memo) = ROSTER_MEMO.lock() {
        *memo = None;
    }
    roster.observation_persisted = true;
    Ok(roster)
}

struct CodexProcess(tokio::process::Child);

impl CodexProcess {
    fn terminate(&mut self) {
        // npm-installed CLIs can be wrappers. Kill only the process group
        // created for this invocation, including a wrapped app-server.
        #[cfg(unix)]
        if let Some(pid) = self.0.id().and_then(|id| i32::try_from(id).ok()) {
            // SAFETY: process_group(0) below gives this child its own group.
            unsafe {
                libc::kill(-pid, libc::SIGKILL);
            }
        }
        let _ = self.0.start_kill();
    }
}

impl Drop for CodexProcess {
    fn drop(&mut self) {
        self.terminate();
    }
}

async fn query_codex_cli(
    mut command: tokio::process::Command,
    timeout: std::time::Duration,
    previous: &CodexModelRoster,
) -> Result<CodexModelRoster, &'static str> {
    let directory = tempfile::tempdir().map_err(|_| "codex_temporary_directory_failed")?;
    // Never forward Codewhale/provider keys or source paths to the CLI. Its
    // own HOME/CODEX_HOME still selects its login and configuration.
    command.env_clear();
    for name in [
        "PATH",
        "HOME",
        "USERPROFILE",
        "CODEX_HOME",
        "TMPDIR",
        "TMP",
        "TEMP",
        "LANG",
        "LC_ALL",
        "SYSTEMROOT",
        "WINDIR",
        "PATHEXT",
    ] {
        if let Some(value) = std::env::var_os(name) {
            command.env(name, value);
        }
    }
    // A relative CODEX_HOME belongs to the caller's cwd, not the isolated
    // directory used below to avoid forwarding workspace context to Codex.
    command.env(
        "CODEX_HOME",
        std::path::absolute(codex_home_path()).map_err(|_| "codex_home_unavailable")?,
    );
    command
        .args([
            "app-server",
            "--listen",
            "stdio://",
            "-c",
            "analytics.enabled=false",
        ])
        .current_dir(directory.path())
        .stdin(Stdio::piped())
        .stdout(Stdio::piped())
        .stderr(Stdio::null())
        .kill_on_drop(true);
    #[cfg(unix)]
    command.process_group(0);
    crate::utils::suppress_tokio_console_window(&mut command);
    let mut child = CodexProcess(command.spawn().map_err(|_| "codex_cli_unavailable")?);
    let mut stdin = child.0.stdin.take().ok_or("codex_stdio_unavailable")?;
    let stdout = child.0.stdout.take().ok_or("codex_stdio_unavailable")?;
    let mut stdout = BufReader::new(stdout.take(MAX_MODEL_CACHE_BYTES + 1));
    let mut bytes_read = 0usize;
    let result =
        tokio::time::timeout(timeout, async {
            send_rpc(&mut stdin, &json!({"id": 1, "method": "initialize", "params": {
            "clientInfo": {"name": "codewhale_models", "version": env!("CARGO_PKG_VERSION")},
            "capabilities": {"experimentalApi": false}
        }})).await?;
            read_rpc_result(&mut stdout, 1, &mut bytes_read).await?;
            send_rpc(&mut stdin, &json!({"method": "initialized", "params": {}})).await?;
            send_rpc(
                &mut stdin,
                &json!({"id": 2, "method": "account/read", "params": {"refreshToken": false}}),
            )
            .await?;
            let account = read_rpc_result(&mut stdout, 2, &mut bytes_read).await?;
            if account.pointer("/account/type").and_then(Value::as_str) != Some("chatgpt")
                || account.get("requiresOpenaiAuth").and_then(Value::as_bool) != Some(true)
            {
                return Err("codex_chatgpt_login_required");
            }
            let mut models = Vec::new();
            let mut cursor: Option<String> = None;
            let mut cursors = HashSet::new();
            for page in 0..20u64 {
                let id = page + 3;
                send_rpc(
                    &mut stdin,
                    &json!({"id": id, "method": "model/list", "params": {
                        "cursor": cursor, "limit": 100, "includeHidden": false
                    }}),
                )
                .await?;
                let result = read_rpc_result(&mut stdout, id, &mut bytes_read).await?;
                let page: CliModelPage =
                    serde_json::from_value(result).map_err(|_| "codex_invalid_response")?;
                models.extend(page.data);
                let Some(next) = page.next_cursor else {
                    return roster_from_cli_models(models, previous);
                };
                if next.is_empty() || next.len() > 4096 || !cursors.insert(next.clone()) {
                    return Err("codex_invalid_pagination");
                }
                cursor = Some(next);
            }
            Err("codex_pagination_limit")
        })
        .await
        .unwrap_or(Err("codex_timeout"));
    child.terminate();
    // Bound both the conversation and process reaping. Drop also covers
    // cancellation or errors before this cleanup point.
    let _ = tokio::time::timeout(std::time::Duration::from_secs(2), child.0.wait()).await;
    result
}

async fn send_rpc(
    stdin: &mut tokio::process::ChildStdin,
    value: &Value,
) -> Result<(), &'static str> {
    let mut bytes = serde_json::to_vec(value).map_err(|_| "codex_invalid_request")?;
    bytes.push(b'\n');
    stdin
        .write_all(&bytes)
        .await
        .map_err(|_| "codex_stdio_failed")?;
    stdin.flush().await.map_err(|_| "codex_stdio_failed")
}

async fn read_rpc_result<R: tokio::io::AsyncBufRead + Unpin>(
    stdout: &mut R,
    id: u64,
    bytes_read: &mut usize,
) -> Result<Value, &'static str> {
    for _ in 0..256 {
        let mut line = Vec::new();
        let count = stdout
            .read_until(b'\n', &mut line)
            .await
            .map_err(|_| "codex_stdio_failed")?;
        *bytes_read += count;
        if *bytes_read as u64 > MAX_MODEL_CACHE_BYTES {
            return Err("codex_response_too_large");
        }
        if count == 0 {
            return Err("codex_stdio_closed");
        }
        let response: Value =
            serde_json::from_slice(&line).map_err(|_| "codex_invalid_response")?;
        if let Some(response_id) = response.get("id") {
            if response_id.as_u64() != Some(id) || response.get("method").is_some() {
                return Err("codex_unexpected_request");
            }
            if response.get("error").is_some() {
                return Err("codex_request_failed");
            }
            return response
                .get("result")
                .cloned()
                .ok_or("codex_invalid_response");
        }
        if response.get("method").and_then(Value::as_str).is_none() {
            return Err("codex_invalid_response");
        }
    }
    Err("codex_notification_limit")
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct CliModelPage {
    data: Vec<CliModel>,
    next_cursor: Option<String>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct CliModel {
    model: String,
    hidden: bool,
    is_default: bool,
    supported_reasoning_efforts: Vec<CliReasoningEffort>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
struct CliReasoningEffort {
    reasoning_effort: String,
}

fn roster_from_cli_models(
    mut entries: Vec<CliModel>,
    previous: &CodexModelRoster,
) -> Result<CodexModelRoster, &'static str> {
    entries.sort_by_key(|model| !model.is_default);
    let mut seen = HashSet::new();
    let mut models = Vec::new();
    for entry in entries.into_iter().filter(|model| !model.hidden) {
        if !valid_model_id(&entry.model) || entry.supported_reasoning_efforts.len() > 16 {
            return Err("codex_invalid_response");
        }
        let mut efforts = Vec::new();
        for effort in entry.supported_reasoning_efforts {
            let effort = effort.reasoning_effort.trim().to_ascii_lowercase();
            if !valid_effort(&effort) {
                return Err("codex_invalid_response");
            }
            if !efforts.contains(&effort) {
                efforts.push(effort);
            }
        }
        if seen.insert(entry.model.to_ascii_lowercase()) {
            models.push(CodexModelMetadata {
                context_window: previous
                    .metadata_for(&entry.model)
                    .and_then(|model| model.context_window),
                id: entry.model,
                reasoning: Some(!efforts.is_empty()),
                efforts,
            });
        }
    }
    if models.is_empty() {
        return Err("codex_empty_catalog");
    }
    Ok(CodexModelRoster {
        models,
        freshness: CodexModelCacheFreshness::Fresh,
        fetched_at: None,
        observed_at: Some(Utc::now()),
        source: "codex_app_server",
        observation_persisted: false,
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    const FIXTURE: &str = include_str!("../tests/fixtures/codex_models_cache.json");
    const FIXTURE_TIME: &str = "2030-01-02T03:04:05Z";

    fn fixture_time() -> DateTime<Utc> {
        FIXTURE_TIME.parse().expect("fixture timestamp")
    }

    fn write_fixture(home: &Path) {
        std::fs::write(home.join(MODEL_CACHE_FILE), FIXTURE).expect("write fixture");
    }

    #[cfg(unix)]
    fn fake_codex(script: &str, trace: &Path) -> tokio::process::Command {
        let mut command = tokio::process::Command::new("/bin/sh");
        command.args(["-c", script, "fake-codex"]).arg(trace);
        command
    }

    #[cfg(unix)]
    const CLI_FIXTURE: &str = r#"
test -z "${OPENAI_API_KEY:-}${OPENAI_CODEX_ACCESS_TOKEN:-}${CODEWHALE_HOME:-}${PRIVATE_TASK_SECRET:-}" || exit 71
pwd > "$1"
while IFS= read -r line; do
  printf '%s\n' "$line" >> "$1"
  case "$line" in
    *'"id":1,'*) printf '%s\n' '{"id":1,"result":{}}' ;;
    *'"id":2,'*) printf '%s\n' '{"id":2,"result":{"account":{"type":"chatgpt","email":"private-account@example.invalid","planType":"pro"},"requiresOpenaiAuth":true}}' ;;
    *'"id":3,'*) printf '%s\n' '{"method":"account/updated","params":{}}' '{"id":3,"result":{"data":[{"model":"gpt-test-secondary","hidden":false,"isDefault":false,"supportedReasoningEfforts":[]},{"model":"hidden-test-model","hidden":true,"isDefault":false,"supportedReasoningEfforts":[]}],"nextCursor":"page-two"}}' ;;
    *'"id":4,'*) printf '%s\n' '{"id":4,"result":{"data":[{"model":"gpt-new-account-model","hidden":false,"isDefault":true,"supportedReasoningEfforts":[{"reasoningEffort":"high"},{"reasoningEffort":"ultra"},{"reasoningEffort":"high"}]}],"nextCursor":null}}' ;;
  esac
done
"#;

    #[cfg(unix)]
    #[tokio::test]
    async fn codex_cli_loads_paginated_roster_without_forwarding_credentials_and_persists_offline()
    {
        let _lock = crate::test_support::lock_test_env();
        let home = tempfile::tempdir().unwrap();
        let _codex = crate::test_support::EnvVarGuard::set("CODEX_HOME", home.path().join("codex"));
        let _codewhale =
            crate::test_support::EnvVarGuard::set("CODEWHALE_HOME", home.path().join("codewhale"));
        std::fs::create_dir_all(codex_home_path()).unwrap();
        std::fs::write(codex_home_path().join("auth.json"), "fixture login").unwrap();
        let _key = crate::test_support::EnvVarGuard::set("OPENAI_API_KEY", "private-test-key");
        let _token = crate::test_support::EnvVarGuard::set(
            "OPENAI_CODEX_ACCESS_TOKEN",
            "private-test-token",
        );
        let _secret =
            crate::test_support::EnvVarGuard::set("PRIVATE_TASK_SECRET", "private-task-value");
        let trace = home.path().join("trace");
        let roster = update_from_codex_command(
            fake_codex(CLI_FIXTURE, &trace),
            std::time::Duration::from_secs(3),
        )
        .await
        .unwrap();
        assert_eq!(
            roster.model_ids(),
            ["gpt-new-account-model", "gpt-test-secondary"]
        );
        assert_eq!(
            roster
                .metadata_for("gpt-new-account-model")
                .unwrap()
                .efforts,
            ["high", "ultra"]
        );
        assert_eq!(roster.fetched_at, None);
        assert!(roster.observed_at.is_some());
        assert_eq!(roster.source, "codex_app_server");
        let trace = std::fs::read_to_string(trace).unwrap();
        let mut lines = trace.lines();
        assert_ne!(
            Path::new(lines.next().unwrap()),
            std::env::current_dir().unwrap()
        );
        let requests: Vec<Value> = lines
            .map(|line| serde_json::from_str(line).unwrap())
            .collect();
        assert_eq!(
            requests
                .iter()
                .map(|r| r["method"].as_str().unwrap())
                .collect::<Vec<_>>(),
            [
                "initialize",
                "initialized",
                "account/read",
                "model/list",
                "model/list"
            ]
        );
        assert_eq!(requests[2]["params"]["refreshToken"], false);
        assert_eq!(requests[4]["params"]["cursor"], "page-two");
        assert_eq!(requests[3]["params"]["includeHidden"], false);
        let path = cli_snapshot_path(&codex_home_path()).unwrap();
        let bytes = std::fs::read_to_string(&path).unwrap();
        assert!(!bytes.contains("private-"));
        assert!(!bytes.contains("hidden-test-model"));
        *ROSTER_MEMO.lock().unwrap() = None;
        assert_eq!(model_roster(), roster);
        let before = std::fs::read(&path).unwrap();
        let failure = update_from_codex_command(
            tokio::process::Command::new(home.path().join("missing-codex")),
            std::time::Duration::from_secs(1),
        )
        .await;
        assert_eq!(failure.unwrap_err(), "codex_cli_unavailable");
        assert_eq!(std::fs::read(&path).unwrap(), before);
        assert_eq!(model_roster(), roster);
    }

    #[cfg(unix)]
    #[tokio::test]
    async fn codex_observations_are_isolated_by_absolute_home_and_login_version() {
        let _lock = crate::test_support::lock_test_env();
        let fixture = tempfile::tempdir().unwrap();
        let _codewhale = crate::test_support::EnvVarGuard::set(
            "CODEWHALE_HOME",
            fixture.path().join("codewhale"),
        );
        let first = fixture.path().join("account-a");
        let second = fixture.path().join("account-b");
        for home in [&first, &second] {
            std::fs::create_dir(home).unwrap();
            std::fs::write(home.join("auth.json"), "fixture login").unwrap();
        }
        let first_path = cli_snapshot_path(&first).unwrap();
        let second_path = cli_snapshot_path(&second).unwrap();
        assert_ne!(
            first_path, second_path,
            "absolute homes must not share an observation"
        );
        let trace = fixture.path().join("trace");
        {
            let _codex = crate::test_support::EnvVarGuard::set("CODEX_HOME", &first);
            let roster = update_from_codex_command(
                fake_codex(
                    &CLI_FIXTURE.replace("gpt-new-account-model", "gpt-account-a"),
                    &trace,
                ),
                std::time::Duration::from_secs(3),
            )
            .await
            .unwrap();
            assert!(roster.observation_persisted);
            assert_eq!(model_roster().model_ids()[0], "gpt-account-a");
        }
        {
            let _codex = crate::test_support::EnvVarGuard::set("CODEX_HOME", &second);
            assert_eq!(model_roster().model_ids(), [DEFAULT_OPENAI_CODEX_MODEL]);
            update_from_codex_command(
                fake_codex(
                    &CLI_FIXTURE.replace("gpt-new-account-model", "gpt-account-b"),
                    &trace,
                ),
                std::time::Duration::from_secs(3),
            )
            .await
            .unwrap();
            assert_eq!(model_roster().model_ids()[0], "gpt-account-b");
        }
        let _codex = crate::test_support::EnvVarGuard::set("CODEX_HOME", &first);
        assert_eq!(model_roster().model_ids()[0], "gpt-account-a");
        let previous = std::fs::read(&first_path).unwrap();
        std::fs::write(first.join("auth.json"), "replacement fixture login").unwrap();
        assert_ne!(cli_snapshot_path(&first).unwrap(), first_path);
        assert_eq!(model_roster().model_ids(), [DEFAULT_OPENAI_CODEX_MODEL]);
        assert_eq!(
            std::fs::read(&first_path).unwrap(),
            previous,
            "old cache remains untouched"
        );

        // A login replacement during the CLI query also prevents persistence.
        let script =
            format!("printf '%s' 'rotated fixture' > \"$CODEX_HOME/auth.json\"\n{CLI_FIXTURE}");
        let roster = update_from_codex_command(
            fake_codex(&script, &trace),
            std::time::Duration::from_secs(3),
        )
        .await
        .unwrap();
        assert!(!roster.observation_persisted);
        assert_eq!(roster.model_ids()[0], "gpt-new-account-model");
        assert!(!cli_snapshot_path(&first).unwrap().exists());
    }

    #[cfg(unix)]
    #[tokio::test]
    async fn codex_live_observation_remains_usable_without_a_bound_login_file() {
        let _lock = crate::test_support::lock_test_env();
        let fixture = tempfile::tempdir().unwrap();
        let _codewhale = crate::test_support::EnvVarGuard::set("CODEWHALE_HOME", fixture.path());
        let _codex =
            crate::test_support::EnvVarGuard::set("CODEX_HOME", "codex-relative-test-home");
        let trace = fixture.path().join("trace");
        let script = format!("test \"$CODEX_HOME\" = \"$2\" || exit 72\n{CLI_FIXTURE}");
        let mut command = fake_codex(&script, &trace);
        command.arg(std::path::absolute("codex-relative-test-home").unwrap());
        let roster = update_from_codex_command(command, std::time::Duration::from_secs(3))
            .await
            .unwrap();
        assert_eq!(roster.model_ids()[0], "gpt-new-account-model");
        assert!(!roster.observation_persisted);
        assert!(cli_snapshot_path(&codex_home_path()).is_none());
        assert_eq!(model_roster().model_ids(), [DEFAULT_OPENAI_CODEX_MODEL]);
    }

    #[test]
    fn codex_native_cache_predating_an_observed_login_change_is_stale() {
        let home = tempfile::tempdir().unwrap();
        std::fs::write(home.path().join("auth.json"), "fixture login").unwrap();
        let now = Utc::now();
        let mut cache: Value = serde_json::from_str(FIXTURE).unwrap();
        cache["fetched_at"] = json!(now - Duration::minutes(1));
        std::fs::write(
            home.path().join(MODEL_CACHE_FILE),
            serde_json::to_vec(&cache).unwrap(),
        )
        .unwrap();
        assert_eq!(
            load_model_roster_from_home_at(home.path(), now).freshness,
            CodexModelCacheFreshness::Stale,
        );
        cache["fetched_at"] = json!(now);
        std::fs::write(
            home.path().join(MODEL_CACHE_FILE),
            serde_json::to_vec(&cache).unwrap(),
        )
        .unwrap();
        assert_eq!(
            load_model_roster_from_home_at(home.path(), now).freshness,
            CodexModelCacheFreshness::Fresh,
        );
    }

    #[cfg(unix)]
    #[tokio::test]
    async fn codex_cli_rejects_other_auth_modes_and_invalid_pagination() {
        let home = tempfile::tempdir().unwrap();
        let fallback = CodexModelRoster::fallback(CodexModelCacheFreshness::Missing, None);
        for (script, expected) in [
            (
                CLI_FIXTURE.replace("\"type\":\"chatgpt\"", "\"type\":\"apiKey\""),
                "codex_chatgpt_login_required",
            ),
            (
                CLI_FIXTURE.replace(
                    "\"requiresOpenaiAuth\":true",
                    "\"requiresOpenaiAuth\":false",
                ),
                "codex_chatgpt_login_required",
            ),
            (
                CLI_FIXTURE.replace("\"nextCursor\":null", "\"nextCursor\":\"page-two\""),
                "codex_invalid_pagination",
            ),
            (
                CLI_FIXTURE.replace("gpt-new-account-model", "bad model"),
                "codex_invalid_response",
            ),
            (
                CLI_FIXTURE.replace(
                    "\"reasoningEffort\":\"ultra\"",
                    "\"reasoningEffort\":\"bad effort\"",
                ),
                "codex_invalid_response",
            ),
        ] {
            let trace = home.path().join("trace");
            let error = query_codex_cli(
                fake_codex(&script, &trace),
                std::time::Duration::from_secs(3),
                &fallback,
            )
            .await
            .unwrap_err();
            assert_eq!(error, expected);
            if expected == "codex_chatgpt_login_required" {
                assert!(
                    !std::fs::read_to_string(trace)
                        .unwrap()
                        .contains("model/list")
                );
            }
        }
    }

    #[tokio::test]
    async fn codex_rpc_bounds_output_and_sanitizes_protocol_errors() {
        for (bytes, expected) in [
            (
                b"{\"id\":3,\"error\":{\"message\":\"private-secret\"}}\n".to_vec(),
                "codex_request_failed",
            ),
            (
                b"{\"id\":3,\"method\":\"account/chatgptAuthTokens/refresh\"}\n".to_vec(),
                "codex_unexpected_request",
            ),
            (
                b"{\"id\":9,\"result\":{}}\n".to_vec(),
                "codex_unexpected_request",
            ),
            (
                vec![b'x'; MAX_MODEL_CACHE_BYTES as usize + 1],
                "codex_response_too_large",
            ),
            (
                b"{\"method\":\"noise\"}\n".repeat(256),
                "codex_notification_limit",
            ),
        ] {
            let mut input = bytes.as_slice();
            assert_eq!(
                read_rpc_result(&mut input, 3, &mut 0).await.unwrap_err(),
                expected
            );
        }
    }

    #[cfg(unix)]
    #[tokio::test]
    async fn codex_cli_timeout_and_cancellation_stop_the_wrapper_and_child() {
        let home = tempfile::tempdir().unwrap();
        let fallback = CodexModelRoster::fallback(CodexModelCacheFreshness::Missing, None);
        for cancel in [false, true] {
            let trace = home.path().join(if cancel { "cancel" } else { "timeout" });
            let command = fake_codex(
                "sleep 300 &\nprintf '%s %s' \"$$\" \"$!\" > \"$1\"\nwait",
                &trace,
            );
            let query = query_codex_cli(command, std::time::Duration::from_millis(500), &fallback);
            if cancel {
                let mut query = Box::pin(query);
                tokio::select! {
                    _ = &mut query => panic!("CLI should remain pending"),
                    _ = tokio::time::sleep(std::time::Duration::from_millis(250)) => {}
                }
                drop(query);
            } else {
                assert_eq!(query.await.unwrap_err(), "codex_timeout");
            }
            let pids = std::fs::read_to_string(trace).unwrap();
            for pid in pids.split_whitespace() {
                let mut running = true;
                for _ in 0..40 {
                    let status = tokio::process::Command::new("/bin/ps")
                        .args(["-o", "stat=", "-p", pid])
                        .output()
                        .await
                        .unwrap();
                    let state = String::from_utf8_lossy(&status.stdout);
                    running = !state.trim().is_empty() && !state.trim().starts_with('Z');
                    if !running {
                        break;
                    }
                    tokio::time::sleep(std::time::Duration::from_millis(25)).await;
                }
                assert!(!running, "owned Codex child survived cleanup");
            }
        }
    }

    #[test]
    fn codex_cli_snapshot_age_and_ids_are_validated() {
        let home = tempfile::tempdir().unwrap();
        let path = home.path().join("roster.json");
        let snapshot = CliSnapshot {
            observed_at: fixture_time(),
            models: vec![CodexModelMetadata {
                id: "gpt-account-model".to_string(),
                context_window: None,
                reasoning: Some(true),
                efforts: vec!["ultra".to_string()],
            }],
        };
        codewhale_config::persistence::atomic_write_json(&path, &snapshot).unwrap();
        assert!(load_cli_snapshot(&path, fixture_time()).is_some());
        assert!(load_cli_snapshot(&path, fixture_time() + Duration::hours(25)).is_none());
        assert!(load_cli_snapshot(&path, fixture_time() - Duration::hours(1)).is_none());
        let mut invalid = snapshot;
        invalid.models[0].id = "bad\u{1b}model".to_string();
        codewhale_config::persistence::atomic_write_json(&path, &invalid).unwrap();
        assert!(load_cli_snapshot(&path, fixture_time()).is_none());
    }

    #[test]
    fn valid_cache_uses_priority_order_and_drops_vendor_hidden_models() {
        let home = tempfile::tempdir().expect("temp CODEX_HOME");
        write_fixture(home.path());

        let roster =
            load_model_roster_from_home_at(home.path(), fixture_time() + Duration::minutes(30));

        assert_eq!(roster.freshness, CodexModelCacheFreshness::Fresh);
        assert_eq!(roster.fetched_at, Some(fixture_time()));
        // `codex-test-review` is `visibility: hide` in the fixture, the same
        // marker the live roster puts on `gpt-reserve` and `codex-auto-review`.
        // A model the vendor does not offer must not reach the picker.
        assert_eq!(
            roster.model_ids(),
            ["gpt-test-primary", "gpt-test-secondary"]
        );
        assert!(roster.metadata_for("codex-test-review").is_none());
        let primary = roster
            .metadata_for("gpt-test-primary")
            .expect("primary metadata");
        assert_eq!(primary.context_window, Some(372_000));
        assert_eq!(primary.reasoning, Some(true));
        let secondary = roster
            .metadata_for("gpt-test-secondary")
            .expect("secondary metadata");
        assert_eq!(secondary.context_window, Some(128_000));
        // The effort names survive parsing: the picker builds a per-model
        // thinking ladder from them instead of one static list per provider.
        assert_eq!(primary.efforts, ["high"]);
        assert_eq!(secondary.efforts, ["medium"]);
    }

    #[test]
    fn missing_cache_falls_back_conservatively() {
        let home = tempfile::tempdir().expect("temp CODEX_HOME");
        let roster = load_model_roster_from_home_at(home.path(), fixture_time());

        assert_eq!(roster.freshness, CodexModelCacheFreshness::Missing);
        assert_eq!(roster.model_ids(), [DEFAULT_OPENAI_CODEX_MODEL]);
    }

    #[test]
    fn preferred_model_is_the_fresh_roster_head_only() {
        let home = tempfile::tempdir().expect("temp CODEX_HOME");
        write_fixture(home.path());

        let fresh =
            load_model_roster_from_home_at(home.path(), fixture_time() + Duration::minutes(30));
        assert_eq!(fresh.preferred_model_id(), Some("gpt-test-primary"));

        // Stale and missing rosters must keep the static seed default so a
        // provider switch never trusts outdated route knowledge (#5034).
        let stale =
            load_model_roster_from_home_at(home.path(), fixture_time() + Duration::days(365));
        assert_eq!(stale.preferred_model_id(), None);
        let missing = load_model_roster_from_home_at(
            tempfile::tempdir().expect("empty home").path(),
            fixture_time(),
        );
        assert_eq!(missing.preferred_model_id(), None);
    }

    #[test]
    fn malformed_cache_falls_back_conservatively() {
        let home = tempfile::tempdir().expect("temp CODEX_HOME");
        std::fs::write(home.path().join(MODEL_CACHE_FILE), b"{not-json")
            .expect("write malformed cache");

        let roster = load_model_roster_from_home_at(home.path(), fixture_time());

        assert_eq!(roster.freshness, CodexModelCacheFreshness::Invalid);
        assert_eq!(roster.model_ids(), [DEFAULT_OPENAI_CODEX_MODEL]);
    }

    #[test]
    fn oversized_cache_is_rejected_without_unbounded_read() {
        let home = tempfile::tempdir().expect("temp CODEX_HOME");
        let file = std::fs::File::create(home.path().join(MODEL_CACHE_FILE)).expect("cache file");
        file.set_len(MAX_MODEL_CACHE_BYTES + 1)
            .expect("sparse oversized cache");

        let roster = load_model_roster_from_home_at(home.path(), fixture_time());

        assert_eq!(roster.freshness, CodexModelCacheFreshness::Invalid);
    }

    #[cfg(unix)]
    #[test]
    fn symlink_cache_is_rejected_as_non_regular_input() {
        let home = tempfile::tempdir().expect("temp CODEX_HOME");
        let target = home.path().join("target.json");
        std::fs::write(&target, FIXTURE).expect("target fixture");
        std::os::unix::fs::symlink(&target, home.path().join(MODEL_CACHE_FILE))
            .expect("cache symlink");

        let roster = load_model_roster_from_home_at(home.path(), fixture_time());

        assert_eq!(roster.freshness, CodexModelCacheFreshness::Invalid);
    }

    #[test]
    fn stale_cache_falls_back_conservatively() {
        let home = tempfile::tempdir().expect("temp CODEX_HOME");
        write_fixture(home.path());

        let roster =
            load_model_roster_from_home_at(home.path(), fixture_time() + Duration::hours(25));

        assert_eq!(roster.freshness, CodexModelCacheFreshness::Stale);
        assert_eq!(roster.model_ids(), [DEFAULT_OPENAI_CODEX_MODEL]);
        assert_eq!(roster.fetched_at, Some(fixture_time()));
    }

    #[test]
    fn invalid_and_duplicate_model_ids_are_filtered() {
        let home = tempfile::tempdir().expect("temp CODEX_HOME");
        let cache = format!(
            r#"{{
  "fetched_at": "{FIXTURE_TIME}",
  "models": [
    {{"slug": "gpt-good", "priority": 3}},
    {{"slug": "GPT-GOOD", "priority": 4}},
    {{"slug": "bad model", "priority": 1}},
    {{"slug": "../bad\\path", "priority": 2}}
  ]
}}"#
        );
        std::fs::write(home.path().join(MODEL_CACHE_FILE), cache).expect("write cache");

        let roster = load_model_roster_from_home_at(home.path(), fixture_time());

        assert_eq!(roster.freshness, CodexModelCacheFreshness::Fresh);
        assert_eq!(roster.model_ids(), ["gpt-good"]);
    }

    #[test]
    fn codex_home_respects_environment_override() {
        let lock = crate::test_support::lock_test_env();
        let home = tempfile::tempdir().expect("temp CODEX_HOME");
        let guard = crate::test_support::EnvVarGuard::set("CODEX_HOME", home.path());

        assert_eq!(codex_home_path(), home.path());

        drop(guard);
        drop(lock);
    }
}
