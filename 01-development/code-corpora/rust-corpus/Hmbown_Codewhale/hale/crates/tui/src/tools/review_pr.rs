//! One immutable PR input for CLI, interactive and model-tool reviews.
//! Replaces their separate `gh pr diff` readers; large PRs use local pinned
//! Git objects without fetching, checking out or executing pull-request code.

use std::borrow::Cow;
use std::io::Read;
use std::path::Path;
use std::process::Stdio;
use std::time::Duration;

use anyhow::{Context, Result, bail};
use serde::Deserialize;
use wait_timeout::ChildExt;

use crate::dependencies::{ExternalTool, Gh, Git};

const MAX_OUTPUT_BYTES: usize = 8 * 1024 * 1024;
const VIEW_FIELDS: &str =
    "title,body,baseRefName,headRefName,url,headRefOid,baseRefOid,changedFiles,additions,deletions";

#[derive(Debug, Clone, Default, Deserialize)]
pub(crate) struct GhPullRequest {
    pub title: String,
    pub body: String,
    #[serde(rename = "baseRefName")]
    pub base: String,
    #[serde(rename = "headRefName")]
    pub head: String,
    pub url: String,
    #[serde(rename = "headRefOid")]
    pub head_sha: String,
    #[serde(rename = "baseRefOid")]
    pub base_sha: String,
    #[serde(rename = "changedFiles")]
    pub changed_files: usize,
    pub additions: usize,
    pub deletions: usize,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
enum Program {
    Gh,
    Git,
}

fn pr_args(action: &str, number: u32, repo: Option<&str>) -> Vec<String> {
    let mut args = vec!["pr".into(), action.into(), number.to_string()];
    if let Some(repo) = repo {
        args.extend(["--repo".into(), repo.into()]);
    }
    args
}

fn commit_id(value: &str) -> bool {
    matches!(value.len(), 40 | 64) && value.bytes().all(|byte| byte.is_ascii_hexdigit())
}

fn full_index_objects(line: &str) -> bool {
    let Some(index) = line.strip_prefix("index ") else {
        return false;
    };
    let fields = index.split_ascii_whitespace().collect::<Vec<_>>();
    let valid_fields = match fields.as_slice() {
        [_] => true,
        [_, mode] => mode.len() == 6 && mode.bytes().all(|byte| matches!(byte, b'0'..=b'7')),
        _ => false,
    };
    if !valid_fields {
        return false;
    }
    let Some((old, new)) = fields[0].split_once("..") else {
        return false;
    };
    commit_id(old) && commit_id(new) && old.len() == new.len()
}

fn view_with(
    number: u32,
    repo: Option<&str>,
    run: &mut impl FnMut(Program, &[String]) -> Result<String>,
) -> Result<GhPullRequest> {
    if number == 0 {
        bail!("A positive pull request number is required");
    }
    let mut args = pr_args("view", number, repo);
    args.extend(["--json".into(), VIEW_FIELDS.into()]);
    let view: GhPullRequest = serde_json::from_str(&run(Program::Gh, &args)?)
        .context("gh pr view returned incomplete PR metadata")?;
    if !commit_id(&view.base_sha) || !commit_id(&view.head_sha) {
        bail!("gh pr view did not return exact base and head commit IDs");
    }
    Ok(view)
}

pub(crate) fn fetch_view(
    number: u32,
    repo: Option<&str>,
    workspace: &Path,
) -> Result<GhPullRequest> {
    view_with(number, repo, &mut |program, args| {
        run_command(workspace, program, args)
    })
}

fn same_revision(expected: &GhPullRequest, current: &GhPullRequest) -> Result<()> {
    if expected.head_sha != current.head_sha
        || expected.base_sha != current.base_sha
        || expected.changed_files != current.changed_files
        || expected.additions != current.additions
        || expected.deletions != current.deletions
        || expected.url != current.url
    {
        bail!(
            "Pull request changed during review; no current-PR review or receipt can be accepted. Run the review again."
        );
    }
    Ok(())
}

pub(crate) fn ensure_current(
    number: u32,
    repo: Option<&str>,
    workspace: &Path,
    expected: &GhPullRequest,
) -> Result<()> {
    same_revision(expected, &fetch_view(number, repo, workspace)?)
}

/// Model-only representation of an already verified complete diff. Keep the
/// original for revision checks, fingerprints and comment anchors. Embedded
/// binary payloads are not meaningful text input; their headers retain paths,
/// modes, rename status and exact object IDs. Every text patch remains
/// byte-exact. Local large-PR collection already asks Git for that metadata
/// without embedding the payload.
pub(crate) fn model_diff(diff: &str) -> Cow<'_, str> {
    if !diff.contains("\nGIT binary patch\n") && !diff.contains("\nGIT binary patch\r\n") {
        return Cow::Borrowed(diff);
    }
    let mut output = String::with_capacity(diff.len());
    let mut binary = false;
    let mut block = 0;
    for line in diff.split_inclusive('\n') {
        if line.starts_with("diff --git ") {
            binary = false;
            block = 0;
        }
        let content = line.trim_end_matches(['\r', '\n']);
        if content == "GIT binary patch" {
            binary = true;
            output.push_str("[Binary content not semantically inspected; its encoded payload is omitted from model input.]\n");
        } else if !binary {
            output.push_str(line);
        } else if let Some((encoding, size)) = content.split_once(' ')
            && matches!(encoding, "literal" | "delta")
            && size.parse::<u64>().is_ok()
        {
            let side = if block == 0 { "new" } else { "old" };
            if encoding == "literal" {
                output.push_str(&format!("[Binary {side} object: {size} bytes.]\n"));
            } else {
                output.push_str(&format!("[Binary {side} object: delta instruction stream {size} bytes; object size not established.]\n"));
            }
            block += 1;
        }
    }
    Cow::Owned(output)
}

fn complete_file_set(diff: &str, view: &GhPullRequest) -> Result<()> {
    let mut files = 0;
    let (mut additions, mut deletions) = (0, 0);
    let mut remaining = (0_u32, 0_u32);
    let mut has_patch = false;
    let mut has_full_index = false;
    for line in diff.lines() {
        if remaining != (0, 0) {
            match line.as_bytes().first() {
                Some(b'+') if remaining.1 > 0 => {
                    remaining.1 -= 1;
                    additions += 1;
                }
                Some(b'-') if remaining.0 > 0 => {
                    remaining.0 -= 1;
                    deletions += 1;
                }
                Some(b' ') if remaining.0 > 0 && remaining.1 > 0 => {
                    remaining.0 -= 1;
                    remaining.1 -= 1;
                }
                Some(b'\\') => {}
                _ => bail!("Incomplete PR diff: a text hunk is truncated or malformed"),
            }
            continue;
        }
        if line.starts_with("diff --git ") {
            if files > 0 && !has_patch {
                bail!("Incomplete PR diff: a file patch is missing");
            }
            files += 1;
            has_patch = false;
            has_full_index = false;
        } else if line.starts_with("index ") {
            has_full_index = full_index_objects(line);
        } else if let Some((_, old, new)) = super::review_hunks::parse_hunk_header(line) {
            remaining = (old, new);
            has_patch = true;
        } else if line.starts_with("@@") {
            bail!("Incomplete PR diff: malformed hunk header");
        } else if [
            "new file mode ",
            "deleted file mode ",
            "old mode ",
            "new mode ",
            "rename from ",
            "rename to ",
            "GIT binary patch",
        ]
        .iter()
        .any(|prefix| line.starts_with(prefix))
        {
            has_patch = true;
        } else if line.starts_with("Binary files ") {
            if !has_full_index {
                bail!(
                    "PR diff contains binary metadata without exact full object IDs; complete local Git objects are required"
                );
            }
            has_patch = true;
        }
    }
    if remaining != (0, 0)
        || !has_patch
        || files == 0
        || files != view.changed_files
        || additions != view.additions
        || deletions != view.deletions
    {
        bail!(
            "Incomplete PR diff: received {files} file patches, {additions} additions and {deletions} deletions; expected {}, {} and {}. No partial review is accepted.",
            view.changed_files,
            view.additions,
            view.deletions
        );
    }
    Ok(())
}

fn diff_with(
    number: u32,
    repo: Option<&str>,
    view: &GhPullRequest,
    run: &mut impl FnMut(Program, &[String]) -> Result<String>,
) -> Result<String> {
    // GitHub's diff representation refuses PRs with more than 300 files.
    // Preserve remote-only small-PR usage, but never rely on that limit for
    // completeness: also check the metadata's changed-file count.
    let remote = if view.changed_files <= 300 {
        run(Program::Gh, &pr_args("diff", number, repo)).and_then(|diff| {
            complete_file_set(&diff, view)?;
            Ok(diff)
        })
    } else {
        Err(anyhow::anyhow!("GitHub diff exceeds its 300-file limit"))
    };
    let diff = match remote {
        Ok(diff) => diff,
        Err(remote_error) => {
            let local: Result<String> = (|| {
                let shallow = run(
                    Program::Git,
                    &["rev-parse".into(), "--is-shallow-repository".into()],
                )?;
                if shallow.trim() != "false" {
                    bail!("A full Git history is required to establish the PR merge base");
                }
                let base = run(
                    Program::Git,
                    &[
                        "merge-base".into(),
                        "--all".into(),
                        view.base_sha.clone(),
                        view.head_sha.clone(),
                    ],
                )?;
                let base = base.trim();
                if !commit_id(base) {
                    bail!("The pinned PR commits do not have one available merge base");
                }
                let diff = run(
                    Program::Git,
                    &[
                        "diff".into(),
                        "--no-ext-diff".into(),
                        "--no-textconv".into(),
                        "--no-color".into(),
                        "--no-relative".into(),
                        "--full-index".into(),
                        "--find-renames=50%".into(),
                        "--src-prefix=a/".into(),
                        "--dst-prefix=b/".into(),
                        "--ignore-submodules=none".into(),
                        "--submodule=short".into(),
                        base.into(),
                        view.head_sha.clone(),
                        "--".into(),
                    ],
                )?;
                complete_file_set(&diff, view)?;
                Ok(diff)
            })();
            local.with_context(|| format!("Cannot obtain the complete PR diff ({remote_error}). Make the exact base {} and head {} commits and their full history available in this repository (CI: fetch-depth: 0 and fetch the PR head ref). No fetch or checkout was performed.", view.base_sha, view.head_sha))?
        }
    };
    same_revision(view, &view_with(number, repo, run)?)?;
    Ok(diff)
}

pub(crate) fn fetch_diff(
    number: u32,
    repo: Option<&str>,
    workspace: &Path,
    view: &GhPullRequest,
) -> Result<String> {
    diff_with(number, repo, view, &mut |program, args| {
        run_command(workspace, program, args)
    })
}

fn read_bounded(reader: impl Read, limit: usize) -> std::io::Result<Vec<u8>> {
    let mut bytes = Vec::new();
    reader.take(limit as u64 + 1).read_to_end(&mut bytes)?;
    Ok(bytes)
}

fn run_command(workspace: &Path, program: Program, args: &[String]) -> Result<String> {
    let mut command = match program {
        Program::Gh => Gh::command().context("PR review requires GitHub CLI on PATH")?,
        Program::Git => Git::review_command(workspace)?,
    };
    command
        .args(args)
        .current_dir(workspace)
        .env("GIT_NO_REPLACE_OBJECTS", "1")
        .env("GIT_NO_LAZY_FETCH", "1")
        .env("GIT_TERMINAL_PROMPT", "0")
        .env("GH_PROMPT_DISABLED", "1")
        .stdin(Stdio::null())
        .stdout(Stdio::piped())
        .stderr(Stdio::piped());
    let mut child = command
        .spawn()
        .context("Failed to start PR input command")?;
    let stdout = child
        .stdout
        .take()
        .context("PR command stdout unavailable")?;
    let stderr = child
        .stderr
        .take()
        .context("PR command stderr unavailable")?;
    let stdout = std::thread::spawn(move || read_bounded(stdout, MAX_OUTPUT_BYTES));
    let stderr = std::thread::spawn(move || read_bounded(stderr, 64 * 1024));
    let status = match child.wait_timeout(Duration::from_secs(60))? {
        Some(status) => status,
        None => {
            let _ = child.kill();
            let _ = child.wait();
            bail!("PR input command timed out; no partial output was accepted");
        }
    };
    let stdout = stdout
        .join()
        .map_err(|_| anyhow::anyhow!("PR stdout reader failed"))??;
    let stderr = stderr
        .join()
        .map_err(|_| anyhow::anyhow!("PR stderr reader failed"))??;
    if stdout.len() > MAX_OUTPUT_BYTES || stderr.len() > 64 * 1024 {
        bail!(
            "PR input exceeds the bounded capture limit (8 MiB diff); no partial output was accepted"
        );
    }
    if !status.success() {
        bail!(
            "PR input command failed: {}",
            String::from_utf8_lossy(&stderr).trim()
        );
    }
    String::from_utf8(stdout).context("PR diff is not valid UTF-8; no lossy review is accepted")
}

#[cfg(test)]
mod tests {
    use super::*;

    fn view(files: usize) -> GhPullRequest {
        GhPullRequest {
            title: "Fixture".into(),
            body: String::new(),
            base: "main".into(),
            head: "feature".into(),
            url: "https://github.com/example/repo/pull/6002".into(),
            base_sha: "a".repeat(40),
            head_sha: "b".repeat(40),
            changed_files: files,
            additions: files,
            deletions: 0,
        }
    }

    fn metadata(view: &GhPullRequest) -> String {
        serde_json::json!({
            "title": view.title, "body": view.body, "baseRefName": view.base,
            "headRefName": view.head, "url": view.url, "headRefOid": view.head_sha,
            "baseRefOid": view.base_sha, "changedFiles": view.changed_files,
            "additions": view.additions, "deletions": view.deletions,
        })
        .to_string()
    }

    fn patch(name: &str) -> String {
        format!(
            "diff --git a/{name} b/{name}\nnew file mode 100644\n--- /dev/null\n+++ b/{name}\n@@ -0,0 +1 @@\n+complete\n"
        )
    }

    fn git(workspace: &Path, args: &[&str]) -> String {
        run_command(
            workspace,
            Program::Git,
            &args.iter().map(|arg| (*arg).into()).collect::<Vec<_>>(),
        )
        .expect("local Git fixture")
    }

    fn repository() -> tempfile::TempDir {
        let dir = tempfile::tempdir().unwrap();
        git(dir.path(), &["init", "--template="]);
        git(dir.path(), &["config", "user.name", "Review Fixture"]);
        git(
            dir.path(),
            &["config", "user.email", "review@example.invalid"],
        );
        git(dir.path(), &["config", "commit.gpgsign", "false"]);
        let hooks = dir.path().join("empty-hooks");
        std::fs::create_dir(&hooks).unwrap();
        git(
            dir.path(),
            &["config", "core.hooksPath", hooks.to_str().unwrap()],
        );
        dir
    }

    #[test]
    fn large_pr_uses_all_pinned_git_patches_and_exact_binary_ids_not_the_checkout() {
        let dir = repository();
        git(dir.path(), &["commit", "--allow-empty", "-m", "base"]);
        let base = git(dir.path(), &["rev-parse", "HEAD"]).trim().to_string();
        for i in 0..301 {
            std::fs::write(
                dir.path().join(format!("file-{i}.txt")),
                format!("file {i}\n"),
            )
            .unwrap();
        }
        std::fs::write(dir.path().join("binary.dat"), b"\0\x01\x02\xff").unwrap();
        git(dir.path(), &["add", "*.txt", "binary.dat"]);
        git(dir.path(), &["commit", "-m", "PR head"]);
        let head = git(dir.path(), &["rev-parse", "HEAD"]).trim().to_string();
        std::fs::write(
            dir.path().join("file-300.txt"),
            "unreviewed checkout content\n",
        )
        .unwrap();
        git(dir.path(), &["add", "file-300.txt"]);
        git(dir.path(), &["commit", "-m", "unrelated local head"]);
        std::fs::write(dir.path().join("file-0.txt"), "dirty worktree content\n").unwrap();
        let view = GhPullRequest {
            base_sha: base,
            head_sha: head,
            additions: 301,
            ..view(302)
        };
        let diff = diff_with(6002, Some("example/repo"), &view, &mut |program, args| {
            if program == Program::Gh {
                assert_eq!(
                    args[1], "view",
                    "large PR must not call the 300-file endpoint"
                );
                Ok(metadata(&view))
            } else {
                run_command(dir.path(), program, args)
            }
        })
        .unwrap();
        assert_eq!(
            diff.lines()
                .filter(|line| line.starts_with("diff --git "))
                .count(),
            302
        );
        assert!(diff.contains("+file 300\n"));
        assert!(diff.contains("Binary files /dev/null and b/binary.dat differ"));
        assert!(diff.lines().any(full_index_objects));
        assert!(!diff.contains("GIT binary patch"));
        assert!(!diff.contains("unreviewed checkout content"));
        assert!(!diff.contains("dirty worktree content"));
    }

    #[test]
    fn limit_error_missing_files_and_missing_binary_patch_use_the_same_fallback() {
        let view = view(2);
        let complete = patch("a.txt") + &patch("b.txt");
        for remote in [
            None,
            Some(patch("a.txt")),
            Some(
                patch("a.txt")
                    + "diff --git a/b.txt b/b.txt\nBinary files a/b.txt and b/b.txt differ\n",
            ),
        ] {
            let mut used_local = false;
            let result = diff_with(6002, None, &view, &mut |program, args| match (
                program,
                args[0].as_str(),
            ) {
                (Program::Gh, _) if args[1] == "diff" => remote
                    .clone()
                    .ok_or_else(|| anyhow::anyhow!("HTTP 406: diff exceeds 300 files")),
                (Program::Gh, _) => Ok(metadata(&view)),
                (Program::Git, "rev-parse") => Ok("false\n".into()),
                (Program::Git, "merge-base") => {
                    assert_eq!(&args[2..], &[view.base_sha.clone(), view.head_sha.clone()]);
                    Ok(view.base_sha.clone())
                }
                (Program::Git, "diff") => {
                    used_local = true;
                    assert!(args.contains(&"--no-ext-diff".into()));
                    assert!(args.contains(&"--no-textconv".into()));
                    assert!(!args.contains(&"--binary".into()));
                    assert!(args.contains(&"--full-index".into()));
                    assert_eq!(
                        &args[args.len() - 3..],
                        &[view.base_sha.clone(), view.head_sha.clone(), "--".into()]
                    );
                    Ok(complete.clone())
                }
                _ => panic!("unexpected command"),
            })
            .unwrap();
            assert!(used_local);
            assert_eq!(result, complete);
        }
    }

    #[test]
    fn missing_shallow_or_ambiguous_history_never_returns_a_partial_diff() {
        let view = view(301);
        for failure in ["missing", "shallow", "multiple"] {
            let error = diff_with(6002, None, &view, &mut |program, args| {
                assert_eq!(program, Program::Git);
                match args[0].as_str() {
                    "rev-parse" => Ok(if failure == "shallow" {
                        "true"
                    } else {
                        "false"
                    }
                    .into()),
                    "merge-base" if failure == "multiple" => {
                        Ok(format!("{}\n{}\n", "c".repeat(40), "d".repeat(40)))
                    }
                    "merge-base" => bail!("missing pinned commit object"),
                    _ => panic!("must not generate a diff without exact history"),
                }
            })
            .unwrap_err();
            let error = format!("{error:#}");
            assert!(error.contains("Cannot obtain the complete PR diff"));
            assert!(error.contains("fetch-depth: 0"));
            assert!(error.contains(&view.head_sha));
        }
    }

    #[test]
    fn head_base_and_file_count_changes_after_collection_invalidate_the_review() {
        let view = view(1);
        for field in ["head", "base", "files"] {
            let mut current = view.clone();
            match field {
                "head" => current.head_sha = "c".repeat(40),
                "base" => current.base_sha = "d".repeat(40),
                _ => current.changed_files = 2,
            }
            let error = diff_with(6002, None, &view, &mut |program, args| {
                assert_eq!(program, Program::Gh);
                Ok(if args[1] == "diff" {
                    patch("a.txt")
                } else {
                    metadata(&current)
                })
            })
            .unwrap_err();
            assert!(
                error
                    .to_string()
                    .contains("Pull request changed during review")
            );
            assert!(
                same_revision(&view, &current).is_err(),
                "publication uses the same revision check"
            );
        }
    }

    #[test]
    fn binary_projection_keeps_all_text_headers_and_raw_evidence_unchanged() {
        let before = patch("before.txt");
        let after = "diff --git a/after.txt b/after.txt\r\n@@ -0,0 +1 @@\r\n+GIT binary patch\r\n";
        let headers = "diff --git a/old.png b/new.png\nold mode 100644\nnew mode 100755\nrename from old.png\nrename to new.png\nindex aaa..bbb\n";
        let raw = format!(
            "{before}{headers}GIT binary patch\nliteral 123\nOPAQUE_BASE85\n\ndelta 45\nOLD_BASE85\n\n{after}"
        );
        let original = raw.clone();
        let projected = model_diff(&raw);
        assert!(projected.starts_with(&before));
        assert!(projected.ends_with(after));
        assert!(projected.contains(headers));
        assert!(projected.contains("new object: 123 bytes"));
        assert!(projected.contains(
            "old object: delta instruction stream 45 bytes; object size not established"
        ));
        assert!(projected.contains("not semantically inspected"));
        assert!(!projected.contains("OPAQUE_BASE85"));
        assert!(!projected.contains("OLD_BASE85"));
        assert_eq!(raw, original);
        assert!(matches!(model_diff(&before), Cow::Borrowed(_)));
    }

    #[test]
    fn binary_projection_budget_counts_metadata_and_never_cuts_text() {
        let text = patch("last.txt");
        let raw = format!(
            "diff --git a/image b/image\nnew file mode 100644\nindex 000..abc\nGIT binary patch\nliteral 10000\n{}\n\nliteral 0\n\n{text}",
            "A".repeat(10_000)
        );
        let projected = model_diff(&raw);
        let limit = projected.chars().count();
        assert!(raw.chars().count() > limit);
        assert_eq!(projected.chars().count(), limit);
        assert!(projected.ends_with(&text));
        assert!(projected.contains("old object: 0 bytes"));
    }

    #[test]
    fn malformed_commit_metadata_cannot_become_git_arguments() {
        let mut invalid = view(1);
        invalid.head_sha = "--output=/tmp/unsafe".into();
        assert!(view_with(6002, None, &mut |_, _| Ok(metadata(&invalid))).is_err());
    }

    #[test]
    fn missing_or_truncated_text_patches_cannot_pass_with_a_matching_file_count() {
        let view = view(1);
        for diff in [
            "diff --git a/a b/a\nindex 123..456 100644\n",
            "diff --git a/a b/a\nnew file mode 100644\n",
            "diff --git a/a b/a\n--- a/a\n+++ b/a\n@@ -0,0 +1,2 @@\n+first\n",
            "diff --git a/a b/a\n--- a/a\n+++ b/a\n@@ malformed @@\n+first\n",
        ] {
            assert!(complete_file_set(diff, &view).is_err(), "{diff}");
        }
        complete_file_set(&patch("a"), &view).unwrap();
    }

    #[test]
    fn binary_metadata_requires_exact_full_object_ids() {
        let view = GhPullRequest {
            additions: 0,
            ..view(1)
        };
        let prefix = "diff --git a/image.png b/image.png\n";
        for index in [
            String::new(),
            "index abc..def 100644\n".to_string(),
            format!(
                "index {}..{} extra fields\n",
                "a".repeat(40),
                "b".repeat(40)
            ),
        ] {
            let diff = format!("{prefix}{index}Binary files a/image.png and b/image.png differ\n");
            assert!(complete_file_set(&diff, &view).is_err(), "{diff}");
        }
        let complete = format!(
            "{prefix}index {}..{} 100644\nBinary files a/image.png and b/image.png differ\n",
            "a".repeat(40),
            "b".repeat(40)
        );
        complete_file_set(&complete, &view).unwrap();
    }

    #[test]
    fn oversized_embedded_binary_baseline_fails_but_metadata_fallback_is_complete() {
        let dir = repository();
        git(dir.path(), &["commit", "--allow-empty", "-m", "base"]);
        let base = git(dir.path(), &["rev-parse", "HEAD"]).trim().to_string();
        let mut bytes = vec![0_u8; 7 * 1024 * 1024];
        let mut state = 0x9e37_79b9_u32;
        for byte in &mut bytes {
            state ^= state << 13;
            state ^= state >> 17;
            state ^= state << 5;
            *byte = state as u8;
        }
        bytes[0] = 0;
        std::fs::write(dir.path().join("large.bin"), bytes).unwrap();
        git(dir.path(), &["add", "large.bin"]);
        git(dir.path(), &["commit", "-m", "binary PR head"]);
        let head = git(dir.path(), &["rev-parse", "HEAD"]).trim().to_string();

        let baseline = run_command(
            dir.path(),
            Program::Git,
            &[
                "diff".into(),
                "--no-ext-diff".into(),
                "--no-textconv".into(),
                "--no-color".into(),
                "--no-relative".into(),
                "--binary".into(),
                "--full-index".into(),
                base.clone(),
                head.clone(),
                "--".into(),
            ],
        )
        .unwrap_err();
        assert!(baseline.to_string().contains("bounded capture limit"));

        let view = GhPullRequest {
            base_sha: base,
            head_sha: head,
            additions: 0,
            changed_files: 1,
            ..view(1)
        };
        let diff = diff_with(6002, None, &view, &mut |program, args| {
            if program == Program::Gh {
                if args[1] == "diff" {
                    bail!("remote diff unavailable")
                }
                Ok(metadata(&view))
            } else {
                run_command(dir.path(), program, args)
            }
        })
        .unwrap();
        assert!(diff.contains("Binary files /dev/null and b/large.bin differ"));
        assert!(diff.lines().any(full_index_objects));
        assert!(!diff.contains("GIT binary patch"));
    }

    #[test]
    fn oversized_command_output_is_refused_without_unbounded_capture() {
        let dir = repository();
        std::fs::write(
            dir.path().join("large.txt"),
            vec![b'x'; MAX_OUTPUT_BYTES + 1],
        )
        .unwrap();
        git(dir.path(), &["add", "large.txt"]);
        git(dir.path(), &["commit", "-m", "large fixture"]);
        let error = run_command(
            dir.path(),
            Program::Git,
            &["show".into(), "HEAD:large.txt".into()],
        )
        .unwrap_err();
        assert!(error.to_string().contains("no partial output was accepted"));
    }
}
