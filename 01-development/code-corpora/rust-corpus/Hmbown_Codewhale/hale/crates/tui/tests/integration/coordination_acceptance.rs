//! Process-boundary acceptance for delegated coordination (#4647).
//!
//! This target retains the real-Git fan-in proof: terminal reconciliation must
//! keep both candidates available instead of reducing them to source strings.
//! Visible focus and composer behavior is accepted in the actual terminal.

use std::path::Path;
use std::process::Command;

use tempfile::tempdir;

#[test]
fn terminal_retry_fixture_preserves_both_real_git_candidates() {
    let repo = tempdir().expect("temp repo");
    git(repo.path(), &["init"]);
    git(repo.path(), &["config", "core.autocrlf", "false"]);
    git(repo.path(), &["config", "user.name", "codewhale Tests"]);
    git(repo.path(), &["config", "user.email", "tests@example.com"]);
    git(repo.path(), &["config", "commit.gpgsign", "false"]);
    git(repo.path(), &["commit", "--allow-empty", "-m", "base"]);
    let base = git_stdout(repo.path(), &["branch", "--show-current"]);

    git(repo.path(), &["switch", "-c", "candidate-a"]);
    std::fs::create_dir_all(repo.path().join("src")).expect("src");
    std::fs::write(repo.path().join("src/a.rs"), "pub const A: u8 = 1;\n").expect("candidate A");
    git(repo.path(), &["add", "src/a.rs"]);
    git(repo.path(), &["commit", "-m", "candidate A"]);
    let candidate_a = git_stdout(repo.path(), &["rev-parse", "HEAD"]);

    git(repo.path(), &["switch", &base]);
    git(repo.path(), &["switch", "-c", "candidate-b"]);
    std::fs::create_dir_all(repo.path().join("src")).expect("src");
    std::fs::write(repo.path().join("src/b.rs"), "pub const B: u8 = 2;\n").expect("candidate B");
    git(repo.path(), &["add", "src/b.rs"]);
    git(repo.path(), &["commit", "-m", "candidate B"]);
    let candidate_b = git_stdout(repo.path(), &["rev-parse", "HEAD"]);

    assert_ne!(candidate_a, candidate_b);
    assert_eq!(
        git_stdout(repo.path(), &["show", "candidate-a:src/a.rs"]),
        "pub const A: u8 = 1;"
    );
    assert_eq!(
        git_stdout(repo.path(), &["show", "candidate-b:src/b.rs"]),
        "pub const B: u8 = 2;"
    );
}

fn git(repo: &Path, args: &[&str]) {
    let output = Command::new("git")
        .args(args)
        .current_dir(repo)
        .output()
        .expect("git command");
    assert!(
        output.status.success(),
        "git {args:?} failed: {}",
        String::from_utf8_lossy(&output.stderr)
    );
}

fn git_stdout(repo: &Path, args: &[&str]) -> String {
    let output = Command::new("git")
        .args(args)
        .current_dir(repo)
        .output()
        .expect("git command");
    assert!(
        output.status.success(),
        "git {args:?} failed: {}",
        String::from_utf8_lossy(&output.stderr)
    );
    String::from_utf8_lossy(&output.stdout).trim().to_string()
}
