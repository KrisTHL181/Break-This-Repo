//! Web search and code search tools.

use anyhow::{Context, Result};
use ignore::overrides::OverrideBuilder;
use ignore::WalkBuilder;
use regex::RegexBuilder;
use std::collections::HashSet;
use std::io::{BufRead, Read};
use std::os::unix::process::CommandExt;
use std::path::Path;
use std::sync::atomic::{AtomicBool, AtomicUsize, Ordering};
use std::sync::{Arc, Mutex, OnceLock};
use std::time::{Duration, Instant};

use super::shell::kill_process_group;
use super::web::{read_error_body, web_client};

/// Wall-clock ceiling for symbol_search / grep_search.
const SEARCH_TIMEOUT_SECS: u64 = 30;

/// The in-process fallback is only used when ripgrep is unavailable. Keep a
/// per-file cap so a single generated artifact cannot exhaust the GUI process.
const FALLBACK_MAX_FILE_BYTES: u64 = 8 * 1024 * 1024;

// ─── Web search providers ─────────────────────────────────────────────────────

fn search_brave(
    query: &str,
    api_key: &str,
    kind: Option<&str>,
    freshness: Option<&str>,
) -> Result<String> {
    let endpoint = if kind == Some("news") {
        "https://api.search.brave.com/res/v1/news/search"
    } else {
        "https://api.search.brave.com/res/v1/web/search"
    };
    let mut req = web_client()
        .get(endpoint)
        .query(&[("q", query), ("count", "10"), ("extra_snippets", "true")])
        .header("X-Subscription-Token", api_key)
        .header("Accept", "application/json");
    if let Some(f) = freshness {
        req = req.query(&[("freshness", f)]);
    }
    let resp = req.send().context("brave search request failed")?;
    if !resp.status().is_success() {
        let status = resp.status();
        let body = read_error_body(resp);
        anyhow::bail!(
            "brave search returned {}: {}",
            status,
            body.chars().take(300).collect::<String>()
        );
    }
    let json: serde_json::Value = resp.json().context("parse brave response")?;
    let results = if kind == Some("news") {
        json["results"]
            .as_array()
            .map(|a| a.as_slice())
            .unwrap_or(&[])
    } else {
        json["web"]["results"]
            .as_array()
            .map(|a| a.as_slice())
            .unwrap_or(&[])
    };
    if results.is_empty() {
        return Ok("No results found.".into());
    }
    let mut out = String::new();
    for r in results.iter().take(10) {
        let title = r["title"].as_str().unwrap_or("(no title)");
        let url = r["url"].as_str().unwrap_or("");
        let desc = r["description"].as_str().unwrap_or("");
        out.push_str(&format!("- **{}** <{}>\n  {}\n", title, url, desc));
        if let Some(extras) = r["extra_snippets"].as_array() {
            for snippet in extras.iter().take(3) {
                if let Some(s) = snippet.as_str() {
                    out.push_str(&format!("  > {}\n", s));
                }
            }
        }
    }
    Ok(out)
}

fn search_pipellm(query: &str, api_key: &str, kind: Option<&str>) -> Result<String> {
    let path = match kind {
        Some("news") => "v1/websearch/search-news",
        Some("deep") => "v1/websearch/search",
        _ => "v1/websearch/simple-search",
    };
    let domains = ["https://api.pipellm.ai", "https://api.pipellm.com"];
    let mut last_err = String::new();
    for base in &domains {
        let url = format!("{}/{}", base, path);
        let resp = match web_client()
            .get(&url)
            .query(&[("q", query)])
            .bearer_auth(api_key)
            .send()
        {
            Ok(r) => r,
            Err(e) => {
                last_err = e.to_string();
                continue;
            }
        };
        if !resp.status().is_success() {
            let status = resp.status();
            let body = read_error_body(resp);
            last_err = format!(
                "{} from {}: {}",
                status,
                base,
                body.chars().take(300).collect::<String>()
            );
            continue;
        }
        let json: serde_json::Value = resp.json().context("parse pipellm response")?;
        let results = json["organic"]
            .as_array()
            .or_else(|| json["data"]["organic"].as_array())
            .map(|a| a.as_slice())
            .unwrap_or(&[]);
        if results.is_empty() {
            return Ok("No results found.".into());
        }
        let mut out = String::new();
        for r in results.iter().take(10) {
            let title = r["title"].as_str().unwrap_or("(no title)");
            let url = r["link"]
                .as_str()
                .or_else(|| r["url"].as_str())
                .unwrap_or("");
            let snippet = r["snippet"]
                .as_str()
                .or_else(|| r["content"].as_str())
                .unwrap_or("");
            out.push_str(&format!("- **{}** <{}>\n  {}\n", title, url, snippet));
        }
        return Ok(out);
    }
    anyhow::bail!("pipellm search failed: {}", last_err)
}

fn search_tavily(
    query: &str,
    api_key: &str,
    kind: Option<&str>,
    freshness: Option<&str>,
    search_depth: Option<&str>,
) -> Result<String> {
    let mut body = serde_json::json!({
        "query": query,
        "max_results": 10,
        "include_answer": true
    });
    if let Some(k) = kind {
        if k == "news" || k == "finance" {
            body["topic"] = serde_json::json!(k);
        }
    }
    if let Some(d) = search_depth {
        body["search_depth"] = serde_json::json!(d);
    }
    if let Some(f) = freshness {
        let days: u32 = match f {
            "pd" => 1,
            "pw" => 7,
            "pm" => 31,
            "py" => 365,
            other => other.parse().unwrap_or(7),
        };
        body["days"] = serde_json::json!(days);
    }
    let resp = web_client()
        .post("https://api.tavily.com/search")
        .bearer_auth(api_key)
        .json(&body)
        .send()
        .context("tavily search request failed")?;
    if !resp.status().is_success() {
        let status = resp.status();
        let body = read_error_body(resp);
        anyhow::bail!(
            "tavily search returned {}: {}",
            status,
            body.chars().take(300).collect::<String>()
        );
    }
    let json: serde_json::Value = resp.json().context("parse tavily response")?;
    let results = json["results"]
        .as_array()
        .map(|a| a.as_slice())
        .unwrap_or(&[]);
    let mut out = String::new();
    if let Some(answer) = json["answer"].as_str() {
        if !answer.is_empty() {
            out.push_str(&format!("**Answer:** {}\n\n", answer));
        }
    }
    if results.is_empty() && out.is_empty() {
        return Ok("No results found.".into());
    }
    for r in results.iter().take(10) {
        let title = r["title"].as_str().unwrap_or("(no title)");
        let url = r["url"].as_str().unwrap_or("");
        let content = r["content"].as_str().unwrap_or("");
        out.push_str(&format!("- **{}** <{}>\n  {}\n", title, url, content));
    }
    Ok(out)
}

pub(super) fn exec_web_search(
    args: &serde_json::Value,
    config: &crate::ai_client::AssistantConfig,
) -> Result<String> {
    let query = args["query"].as_str().context("missing query")?;
    let provider = config
        .web_search_provider
        .as_deref()
        .context("web_search provider not configured")?;
    let api_key = config
        .web_search_api_key
        .as_deref()
        .context("web_search api key missing")?;
    let kind = args["kind"].as_str();
    let freshness = args["freshness"].as_str();
    let search_depth = args["search_depth"].as_str();
    match provider {
        "brave" => search_brave(query, api_key, kind, freshness),
        "pipellm" => search_pipellm(query, api_key, kind),
        "tavily" => search_tavily(query, api_key, kind, freshness, search_depth),
        _ => anyhow::bail!("unknown web_search provider: {}", provider),
    }
}

// ─── Code search ──────────────────────────────────────────────────────────────

fn escape_regex(s: &str) -> String {
    let mut out = String::with_capacity(s.len() + 8);
    for c in s.chars() {
        if matches!(
            c,
            '\\' | '.' | '+' | '*' | '?' | '(' | ')' | '|' | '[' | ']' | '{' | '}' | '^' | '$'
        ) {
            out.push('\\');
        }
        out.push(c);
    }
    out
}

struct FallbackSearchOutput {
    lines: Vec<String>,
    truncation: Option<FallbackTruncation>,
    timed_out: bool,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum FallbackTruncation {
    ResultLimit,
    OutputBytes,
}

#[allow(clippy::too_many_arguments)]
fn fallback_regex_search(
    pattern: &str,
    root: &Path,
    glob_filter: Option<&str>,
    context_lines: usize,
    case_insensitive: bool,
    max_results: usize,
    cancel: &AtomicBool,
    provider_label: &str,
) -> Result<FallbackSearchOutput> {
    let regex = RegexBuilder::new(pattern)
        .case_insensitive(case_insensitive)
        .build()
        .with_context(|| format!("invalid {provider_label} regular expression"))?;

    let override_root = if root.is_dir() {
        root
    } else {
        root.parent().unwrap_or_else(|| Path::new("."))
    };
    let mut walker = WalkBuilder::new(root);
    walker.follow_links(false).filter_entry(|entry| {
        // Mirror the rg path's `!**/*credentials*` exclusion: search results
        // cannot be approval-gated per file, so credential-named entries are
        // skipped here even when fs_read would allow them behind a prompt.
        let credential_named = entry
            .file_name()
            .to_str()
            .is_some_and(|name| name.to_ascii_lowercase().contains("credentials"));
        !credential_named && super::paths::reject_if_sensitive(entry.path()).is_ok()
    });
    if let Some(glob) = glob_filter {
        let mut overrides = OverrideBuilder::new(override_root);
        overrides
            .add(glob)
            .with_context(|| format!("invalid search glob '{glob}'"))?;
        walker.overrides(overrides.build().context("build search glob")?);
    }

    let started = Instant::now();
    let timeout = Duration::from_secs(SEARCH_TIMEOUT_SECS);
    let mut output = Vec::new();
    let mut output_bytes = 0usize;
    let mut match_count = 0usize;
    let mut truncation = None;
    let mut timed_out = false;

    'entries: for entry in walker.build() {
        if cancel.load(Ordering::Relaxed) {
            anyhow::bail!("{provider_label} canceled");
        }
        if started.elapsed() >= timeout {
            timed_out = true;
            break;
        }

        let Ok(entry) = entry else { continue };
        if !entry.file_type().is_some_and(|kind| kind.is_file()) {
            continue;
        }
        let Ok(metadata) = entry.metadata() else {
            continue;
        };
        if metadata.len() > FALLBACK_MAX_FILE_BYTES {
            continue;
        }
        let Ok(bytes) = std::fs::read(entry.path()) else {
            continue;
        };
        if bytes.contains(&0) {
            continue;
        }
        let text = String::from_utf8_lossy(&bytes);
        let file_lines: Vec<&str> = text.lines().collect();
        let mut matches = Vec::new();
        let mut result_limit_reached = false;

        for (index, line) in file_lines.iter().enumerate() {
            if index % 256 == 0 {
                if cancel.load(Ordering::Relaxed) {
                    anyhow::bail!("{provider_label} canceled");
                }
                if started.elapsed() >= timeout {
                    timed_out = true;
                    break 'entries;
                }
            }
            if !regex.is_match(line) {
                continue;
            }
            if match_count >= max_results {
                truncation = Some(FallbackTruncation::ResultLimit);
                result_limit_reached = true;
                break;
            }
            match_count += 1;
            matches.push(index);
        }

        if matches.is_empty() {
            if result_limit_reached {
                break;
            }
            continue;
        }

        let matched_lines: HashSet<usize> = matches.iter().copied().collect();
        let mut regions: Vec<(usize, usize)> = Vec::new();
        for index in matches {
            let start = index.saturating_sub(context_lines);
            let end = index
                .saturating_add(context_lines)
                .min(file_lines.len().saturating_sub(1));
            if let Some((_, previous_end)) = regions.last_mut() {
                if start <= previous_end.saturating_add(1) {
                    *previous_end = (*previous_end).max(end);
                    continue;
                }
            }
            regions.push((start, end));
        }

        for (region_index, (start, end)) in regions.into_iter().enumerate() {
            for (offset, line) in file_lines[start..=end].iter().enumerate() {
                let index = start + offset;
                let marker = if matched_lines.contains(&index) {
                    ':'
                } else {
                    '-'
                };
                let rendered = format!(
                    "{}{marker}{}{marker}{}",
                    entry.path().display(),
                    index + 1,
                    line
                );
                let rendered_bytes = rendered.len() + 1;
                let separator_bytes = usize::from(region_index > 0 && offset == 0) * 3;
                if output_bytes
                    .checked_add(separator_bytes)
                    .and_then(|next| next.checked_add(rendered_bytes))
                    .is_none_or(|next| next > FALLBACK_MAX_OUTPUT_BYTES)
                {
                    truncation = Some(FallbackTruncation::OutputBytes);
                    break 'entries;
                }
                if separator_bytes > 0 {
                    output_bytes += separator_bytes;
                    output.push("--".to_string());
                }
                output_bytes += rendered_bytes;
                output.push(rendered);
            }
        }
        if result_limit_reached {
            break;
        }
    }

    Ok(FallbackSearchOutput {
        lines: output,
        truncation,
        timed_out,
    })
}

fn fallback_truncation_notice(
    truncation: Option<FallbackTruncation>,
    max_results: usize,
) -> Option<String> {
    match truncation {
        Some(FallbackTruncation::ResultLimit) => {
            Some(format!("[... truncated at {} results]", max_results))
        }
        Some(FallbackTruncation::OutputBytes) => Some(format!(
            "[... truncated at {} byte output budget]",
            FALLBACK_MAX_OUTPUT_BYTES
        )),
        None => None,
    }
}

fn fallback_empty_truncation_message(
    truncation: Option<FallbackTruncation>,
    result_kind: &str,
) -> Option<String> {
    match truncation {
        Some(FallbackTruncation::ResultLimit) => Some(format!(
            "{result_kind} were found but max_results was zero; increase max_results."
        )),
        Some(FallbackTruncation::OutputBytes) => Some(format!(
            "{result_kind} were found but the first result exceeded the {} byte output budget; narrow the pattern, path, or context_lines.",
            FALLBACK_MAX_OUTPUT_BYTES
        )),
        None => None,
    }
}

fn sort_symbol_lines(lines: &mut [String]) {
    lines.sort_by(|a, b| {
        let has_definition_keyword = |line: &str| {
            line.contains("fn ")
                || line.contains("function ")
                || line.contains("def ")
                || line.contains("struct ")
                || line.contains("class ")
                || line.contains("type ")
                || line.contains("enum ")
                || line.contains("trait ")
                || line.contains("interface ")
        };
        has_definition_keyword(b).cmp(&has_definition_keyword(a))
    });
}

/// Output ceiling for the in-process fallback. Unlike the ripgrep path, the
/// buffer lives in the GUI process, so accumulation must be bounded even when
/// the caller passes huge `context_lines` / `max_results`.
const FALLBACK_MAX_OUTPUT_BYTES: usize = 512 * 1024;

/// Build the ripgrep argument vector for grep_search. Kept pure so tests can
/// pin the `--` flag-parsing barrier: the model-supplied pattern must never
/// be consumed as a ripgrep option (`--pre=<cmd>` would run a program per
/// file, and an injected `--iglob` would override the sensitive-file
/// exclusions because rg's override matcher is last-match-wins).
#[allow(clippy::too_many_arguments)]
fn grep_rg_args(
    pattern: &str,
    abs_path: &str,
    context_lines: usize,
    max_results: usize,
    case_insensitive: bool,
    glob_filter: Option<&str>,
    sensitive_globs: &[String],
) -> Vec<String> {
    let mut args = vec![
        "--line-number".to_string(),
        "--no-heading".to_string(),
        "--color=never".to_string(),
        format!("--context={context_lines}"),
        format!("--max-count={max_results}"),
    ];
    if case_insensitive {
        args.push("--ignore-case".to_string());
    }
    if let Some(glob) = glob_filter {
        args.push("--glob".to_string());
        args.push(glob.to_string());
    }
    for glob in sensitive_globs {
        args.push("--iglob".to_string());
        args.push(glob.clone());
    }
    args.push("--".to_string());
    args.push(pattern.to_string());
    args.push(abs_path.to_string());
    args
}

/// Build the ripgrep argument vector for symbol_search. Same `--` barrier as
/// [`grep_rg_args`]; the combined pattern is code-built today, but the
/// barrier keeps that assumption out of the safety argument.
fn symbol_rg_args(
    combined_pattern: &str,
    abs_path: &str,
    glob_filter: Option<&str>,
    sensitive_globs: &[String],
) -> Vec<String> {
    let mut args = vec![
        "--line-number".to_string(),
        "--no-heading".to_string(),
        "--color=never".to_string(),
        "--max-count=50".to_string(),
    ];
    if let Some(glob) = glob_filter {
        args.push("--glob".to_string());
        args.push(glob.to_string());
    }
    for glob in sensitive_globs {
        args.push("--iglob".to_string());
        args.push(glob.clone());
    }
    args.push("--".to_string());
    args.push(combined_pattern.to_string());
    args.push(abs_path.to_string());
    args
}

pub(super) fn exec_symbol_search(
    query: &str,
    kind: &str,
    search_path: &str,
    glob_filter: Option<&str>,
    cwd: &str,
    cancel: &Arc<AtomicBool>,
) -> Result<String> {
    let resolved_path = super::paths::resolve_checked_path(search_path, cwd)?;
    let sensitive_globs = super::paths::sensitive_search_globs(&resolved_path);
    let abs_path = resolved_path.to_string_lossy().into_owned();

    let patterns: Vec<String> = match kind {
        "function" => vec![
            format!(r"(fn|function|def|func)\s+{}", escape_regex(query)),
            format!(
                r"(const|let|var)\s+{}\s*=\s*(async\s+)?\(",
                escape_regex(query)
            ),
        ],
        "type" => vec![format!(
            r"(type|struct|enum|interface|typedef)\s+{}",
            escape_regex(query)
        )],
        "class" => vec![format!(r"(class|struct)\s+{}", escape_regex(query))],
        "method" => vec![
            format!(r"(fn|def|func|function)\s+{}", escape_regex(query)),
            format!(r"\.{}\s*=\s*function", escape_regex(query)),
        ],
        _ => vec![
            format!(r"(fn|function|def|func)\s+{}", escape_regex(query)),
            format!(
                r"(const|let|var)\s+{}\s*=\s*(async\s+)?\(",
                escape_regex(query)
            ),
            format!(
                r"(type|struct|enum|interface|class|trait|typedef)\s+{}",
                escape_regex(query)
            ),
            format!(r"(pub\s+)?(mod|module)\s+{}", escape_regex(query)),
        ],
    };
    let combined = patterns.join("|");

    static HAS_RG: OnceLock<bool> = OnceLock::new();
    let rg = *HAS_RG.get_or_init(|| {
        std::process::Command::new("rg")
            .arg("--version")
            .stdout(std::process::Stdio::null())
            .stderr(std::process::Stdio::null())
            .status()
            .is_ok()
    });
    if !rg {
        let mut fallback = fallback_regex_search(
            &combined,
            &resolved_path,
            glob_filter,
            0,
            false,
            100,
            cancel,
            "symbol_search",
        )?;
        if fallback.lines.is_empty() {
            if fallback.timed_out {
                return Ok(format!(
                    "symbol_search timed out after {}s with no results for '{}'.",
                    SEARCH_TIMEOUT_SECS, query
                ));
            }
            if let Some(message) =
                fallback_empty_truncation_message(fallback.truncation, "Symbol definitions")
            {
                return Ok(message);
            }
            return Ok(format!("No symbol definitions found for '{}'.", query));
        }
        sort_symbol_lines(&mut fallback.lines);
        let mut out = fallback.lines.join("\n");
        if let Some(notice) = fallback_truncation_notice(fallback.truncation, 100) {
            out.push('\n');
            out.push_str(&notice);
        }
        if fallback.timed_out {
            out.push_str(&format!(
                "\n[... timed out after {}s, results may be partial]",
                SEARCH_TIMEOUT_SECS
            ));
        }
        return Ok(out);
    }

    let mut cmd = std::process::Command::new("rg");
    cmd.args(symbol_rg_args(
        &combined,
        &abs_path,
        glob_filter,
        &sensitive_globs,
    ));

    cmd.stdout(std::process::Stdio::piped())
        .stderr(std::process::Stdio::null())
        .process_group(0);
    let mut child = cmd.spawn().context("symbol_search exec failed")?;

    let stdout_pipe = child
        .stdout
        .take()
        .ok_or_else(|| anyhow::anyhow!("symbol_search stdout missing"))?;
    let collected = Arc::new(Mutex::new(Vec::<u8>::new()));
    let collected_clone = collected.clone();
    let reader_thread = crate::thread_util::spawn_with_pool(move || {
        let mut r = stdout_pipe;
        let mut buf = [0u8; 8192];
        loop {
            match r.read(&mut buf) {
                Ok(0) | Err(_) => break,
                Ok(n) => {
                    if let Ok(mut g) = collected_clone.lock() {
                        g.extend_from_slice(&buf[..n]);
                    }
                }
            }
        }
    });

    let start = Instant::now();
    let timeout = Duration::from_secs(SEARCH_TIMEOUT_SECS);
    let mut timed_out = false;
    loop {
        if cancel.load(Ordering::Relaxed) {
            kill_process_group(&child);
            child.wait().ok();
            let _ = reader_thread.join();
            anyhow::bail!("symbol_search canceled");
        }
        if start.elapsed() >= timeout {
            kill_process_group(&child);
            timed_out = true;
            break;
        }
        if let Ok(Some(_)) = child.try_wait() {
            break;
        }
        std::thread::sleep(Duration::from_millis(50));
    }
    child.wait().ok();
    let _ = reader_thread.join();

    let raw = collected.lock().map(|g| g.clone()).unwrap_or_default();
    let text = String::from_utf8_lossy(&raw);

    if text.trim().is_empty() {
        if timed_out {
            return Ok(format!(
                "symbol_search timed out after {}s with no results for '{}'.",
                SEARCH_TIMEOUT_SECS, query
            ));
        }
        return Ok(format!("No symbol definitions found for '{}'.", query));
    }

    let mut lines: Vec<String> = text.lines().take(100).map(str::to_owned).collect();
    sort_symbol_lines(&mut lines);

    let mut out = lines.join("\n");
    if timed_out {
        out.push_str(&format!(
            "\n[... timed out after {}s, results may be partial]",
            SEARCH_TIMEOUT_SECS
        ));
    }
    Ok(out)
}

#[allow(clippy::too_many_arguments)]
pub(super) fn exec_grep_search(
    pattern: &str,
    search_path: &str,
    glob_filter: Option<&str>,
    context_lines: usize,
    case_insensitive: bool,
    max_results: usize,
    cwd: &str,
    cancel: &Arc<AtomicBool>,
) -> Result<String> {
    static HAS_RG: OnceLock<bool> = OnceLock::new();
    let rg = *HAS_RG.get_or_init(|| {
        std::process::Command::new("rg")
            .arg("--version")
            .stdout(std::process::Stdio::null())
            .stderr(std::process::Stdio::null())
            .status()
            .is_ok()
    });
    let resolved_path = super::paths::resolve_checked_path(search_path, cwd)?;
    let sensitive_globs = super::paths::sensitive_search_globs(&resolved_path);
    let abs_path = resolved_path.to_string_lossy().into_owned();

    if !rg {
        let fallback = fallback_regex_search(
            pattern,
            &resolved_path,
            glob_filter,
            context_lines,
            case_insensitive,
            max_results,
            cancel,
            "grep_search",
        )?;
        if fallback.lines.is_empty() {
            if fallback.timed_out {
                return Ok(format!(
                    "grep_search timed out after {}s with no results.",
                    SEARCH_TIMEOUT_SECS
                ));
            }
            if let Some(message) = fallback_empty_truncation_message(fallback.truncation, "Matches")
            {
                return Ok(message);
            }
            return Ok("No matches found.".into());
        }
        let mut out = fallback.lines.join("\n");
        if let Some(notice) = fallback_truncation_notice(fallback.truncation, max_results) {
            out.push('\n');
            out.push_str(&notice);
        }
        if fallback.timed_out {
            out.push_str(&format!(
                "\n[... timed out after {}s, results may be partial]",
                SEARCH_TIMEOUT_SECS
            ));
        }
        return Ok(out);
    }

    let mut cmd = std::process::Command::new("rg");
    cmd.args(grep_rg_args(
        pattern,
        &abs_path,
        context_lines,
        max_results,
        case_insensitive,
        glob_filter,
        &sensitive_globs,
    ));

    cmd.stdout(std::process::Stdio::piped())
        .stderr(std::process::Stdio::piped())
        .process_group(0);
    let mut child = cmd.spawn().context("grep_search exec failed")?;
    let stdout = child
        .stdout
        .take()
        .ok_or_else(|| anyhow::anyhow!("grep stdout missing"))?;
    let stderr = child
        .stderr
        .take()
        .ok_or_else(|| anyhow::anyhow!("grep stderr missing"))?;

    let stderr_buf: Arc<Mutex<Vec<u8>>> = Arc::new(Mutex::new(Vec::with_capacity(512)));
    let stderr_buf_clone = stderr_buf.clone();
    let stderr_handle = crate::thread_util::spawn_with_pool(move || {
        let mut err = stderr;
        let mut chunk = [0u8; 512];
        loop {
            match err.read(&mut chunk) {
                Ok(0) | Err(_) => break,
                Ok(n) => {
                    if let Ok(mut g) = stderr_buf_clone.lock() {
                        let remaining = 512usize.saturating_sub(g.len());
                        if remaining > 0 {
                            g.extend_from_slice(&chunk[..remaining.min(n)]);
                        }
                    }
                }
            }
        }
    });

    let result_lines: Arc<Mutex<Vec<String>>> = Arc::new(Mutex::new(Vec::new()));
    let match_count: Arc<AtomicUsize> = Arc::new(AtomicUsize::new(0));
    let truncated_flag = Arc::new(AtomicBool::new(false));

    let rl = result_lines.clone();
    let mc = match_count.clone();
    let tf = truncated_flag.clone();
    let max = max_results;
    let reader_handle = crate::thread_util::spawn_with_pool(move || {
        let reader = std::io::BufReader::new(stdout);
        for line_result in reader.lines() {
            let line = match line_result {
                Ok(l) => l,
                Err(_) => break,
            };
            if !line.starts_with("--") {
                if mc.load(Ordering::Relaxed) >= max {
                    tf.store(true, Ordering::Relaxed);
                    break;
                }
                mc.fetch_add(1, Ordering::Relaxed);
            }
            if let Ok(mut g) = rl.lock() {
                g.push(line);
            }
        }
    });

    let start = Instant::now();
    let timeout = Duration::from_secs(SEARCH_TIMEOUT_SECS);
    let mut timed_out = false;
    let mut canceled = false;
    loop {
        if cancel.load(Ordering::Relaxed) {
            kill_process_group(&child);
            canceled = true;
            break;
        }
        if start.elapsed() >= timeout {
            kill_process_group(&child);
            timed_out = true;
            break;
        }
        if let Ok(Some(_)) = child.try_wait() {
            break;
        }
        std::thread::sleep(Duration::from_millis(50));
    }
    child.wait().ok();
    let _ = reader_handle.join();
    let _ = stderr_handle.join();

    let truncated = truncated_flag.load(Ordering::Relaxed);
    let lines = result_lines.lock().map(|g| g.clone()).unwrap_or_default();

    if lines.is_empty() {
        if canceled {
            anyhow::bail!("grep_search canceled");
        }
        if timed_out {
            return Ok(format!(
                "grep_search timed out after {}s with no results.",
                SEARCH_TIMEOUT_SECS
            ));
        }
        let hint = stderr_buf
            .lock()
            .ok()
            .map(|g| {
                String::from_utf8_lossy(&g)
                    .trim()
                    .chars()
                    .take(200)
                    .collect::<String>()
            })
            .unwrap_or_default();
        if !hint.is_empty() {
            return Ok(format!("No matches. ({})", hint));
        }
        return Ok("No matches found.".into());
    }

    let mut out = lines.join("\n");
    if truncated {
        out.push_str(&format!("\n[... truncated at {} results]", max_results));
    }
    if timed_out {
        out.push_str(&format!(
            "\n[... timed out after {}s, results may be partial]",
            SEARCH_TIMEOUT_SECS
        ));
    }
    if canceled {
        out.push_str("\n[... canceled by user]");
    }
    Ok(out)
}

#[cfg(test)]
mod fallback_tests {
    use super::*;

    #[test]
    fn grep_rg_args_keeps_flag_barrier_before_pattern() {
        let globs = vec!["!**/.env".to_string()];
        let args = grep_rg_args(
            "--pre=/bin/sh",
            "/tmp/probe",
            2,
            100,
            false,
            Some("*.rs"),
            &globs,
        );
        let barrier = args.iter().position(|a| a == "--").expect("has `--`");
        assert_eq!(args[barrier + 1], "--pre=/bin/sh");
        assert_eq!(args[barrier + 2], "/tmp/probe");
        assert_eq!(barrier + 3, args.len(), "pattern and path are last");
        assert!(!args[..barrier].iter().any(|a| a == "--pre=/bin/sh"));
    }

    #[test]
    fn symbol_rg_args_keeps_flag_barrier_before_pattern() {
        let args = symbol_rg_args("--pre=/bin/sh", "/tmp/probe", None, &[]);
        let barrier = args.iter().position(|a| a == "--").expect("has `--`");
        assert_eq!(args[barrier + 1], "--pre=/bin/sh");
        assert_eq!(args[barrier + 2], "/tmp/probe");
        assert_eq!(barrier + 3, args.len(), "pattern and path are last");
    }

    #[test]
    fn fallback_search_bounds_total_output_bytes() {
        let root = tempfile::tempdir().expect("tempdir");
        let line = format!("needle {}\n", "x".repeat(120));
        std::fs::write(root.path().join("big.txt"), line.repeat(20_000)).unwrap();
        let cancel = AtomicBool::new(false);
        let out = fallback_regex_search(
            "needle",
            root.path(),
            None,
            100,
            false,
            usize::MAX,
            &cancel,
            "grep_search",
        )
        .expect("search ok");
        assert_eq!(out.truncation, Some(FallbackTruncation::OutputBytes));
        let total: usize = out.lines.iter().map(|l| l.len() + 1).sum();
        assert!(
            total <= FALLBACK_MAX_OUTPUT_BYTES,
            "accumulated output {} exceeds budget",
            total
        );
    }

    #[test]
    fn fallback_search_keeps_matches_collected_before_result_limit() {
        let root = tempfile::tempdir().expect("tempdir");
        std::fs::write(root.path().join("two.txt"), "needle first\nneedle second\n").unwrap();
        let cancel = AtomicBool::new(false);
        let out = fallback_regex_search(
            "needle",
            root.path(),
            None,
            0,
            false,
            1,
            &cancel,
            "grep_search",
        )
        .expect("search ok");

        assert_eq!(
            out.lines.len(),
            1,
            "the first accepted match must be rendered"
        );
        assert!(out.lines[0].contains("needle first"));
        assert_eq!(out.truncation, Some(FallbackTruncation::ResultLimit));
    }

    #[test]
    fn in_process_search_preserves_recursive_secret_exclusions() {
        let root = tempfile::tempdir().expect("tempdir");
        std::fs::write(root.path().join("visible.txt"), "needle public\n").unwrap();
        std::fs::write(root.path().join("visible.rs"), "fn shared_symbol() {}\n").unwrap();
        std::fs::write(
            root.path().join("Service-Credentials.txt"),
            "needle credential-secret\n",
        )
        .unwrap();
        std::fs::write(
            root.path().join("Assistant.TOML"),
            "needle assistant-secret\n",
        )
        .unwrap();
        let secrets = root.path().join("Secrets");
        std::fs::create_dir(&secrets).unwrap();
        std::fs::write(secrets.join("token.txt"), "needle directory-secret\n").unwrap();
        std::fs::write(secrets.join("secret.rs"), "fn shared_symbol() {}\n").unwrap();

        let cancel = AtomicBool::new(false);
        let grep = fallback_regex_search(
            "needle",
            root.path(),
            None,
            0,
            false,
            20,
            &cancel,
            "grep_search",
        )
        .unwrap()
        .lines
        .join("\n");
        assert!(grep.contains("public"));
        assert!(!grep.contains("credential-secret"));
        assert!(!grep.contains("assistant-secret"));
        assert!(!grep.contains("directory-secret"));

        let symbols = fallback_regex_search(
            r"fn\s+shared_symbol",
            root.path(),
            Some("*.rs"),
            0,
            false,
            20,
            &cancel,
            "symbol_search",
        )
        .unwrap()
        .lines
        .join("\n");
        assert!(symbols.contains("visible.rs"));
        assert!(!symbols.contains("secret.rs"));
    }
}
