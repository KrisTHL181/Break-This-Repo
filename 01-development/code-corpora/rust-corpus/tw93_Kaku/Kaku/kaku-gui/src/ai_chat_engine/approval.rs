/// Returns an approval prompt for tools that require explicit user consent.
#[cfg(test)]
pub(crate) fn approval_summary(name: &str, args: &serde_json::Value) -> Option<String> {
    approval_summary_in_cwd(name, args, ".")
}

pub(crate) fn approval_summary_in_cwd(
    name: &str,
    args: &serde_json::Value,
    cwd: &str,
) -> Option<String> {
    let s = |k: &str| sanitize_for_prompt(args[k].as_str().unwrap_or(""));
    match name {
        "shell_exec" => shell_exec_approval_summary(args["command"].as_str().unwrap_or(""), cwd),
        "shell_bg" => Some(format!("shell_bg: {}", s("command"))),
        "fs_write" => Some(format!("write file: {}", s("path"))),
        "fs_patch" => Some(format!("patch file: {}", s("path"))),
        "fs_mkdir" => Some(format!("mkdir: {}", s("path"))),
        "fs_delete" => Some(format!("delete: {}", s("path"))),
        "http_request" => http_request_approval_summary(args),
        // Reads are normally silent, but credential-named source files
        // (credentials.py, prod_credentials.ts, ...) often hold real secrets:
        // keep them readable only behind an explicit per-file approval.
        "fs_read" => {
            let raw_path = args["path"].as_str().unwrap_or("");
            let path = crate::ai_tools::paths::resolve(raw_path, cwd)
                .unwrap_or_else(|_| std::path::PathBuf::from(raw_path));
            if crate::ai_tools::paths::requires_read_approval(&path) {
                Some(format!("read credential-named file: {}", s("path")))
            } else {
                None
            }
        }
        _ => None,
    }
}

/// Flatten control characters (newlines, tabs, ESC, ...) so a model-supplied
/// value cannot forge extra lines or terminal escapes inside the approval
/// prompt, then cap the length for display.
fn sanitize_for_prompt(value: &str) -> String {
    value
        .chars()
        .map(|c| if c.is_control() { ' ' } else { c })
        .take(60)
        .collect()
}

fn http_request_approval_summary(args: &serde_json::Value) -> Option<String> {
    let method = sanitize_for_prompt(&args["method"].as_str().unwrap_or("GET").to_uppercase());
    let url = sanitize_for_prompt(args["url"].as_str().unwrap_or(""));
    Some(format!("http {}: {}", method, url))
}

fn shell_exec_approval_summary(command: &str, cwd: &str) -> Option<String> {
    if command.trim().is_empty() {
        return Some("shell: ".to_string());
    }
    if shell_command_requires_approval_in_cwd(command, cwd) {
        let preview: String = command
            .chars()
            .map(|c| {
                if c == '\n' || c == '\r' || c == '\t' {
                    ' '
                } else {
                    c
                }
            })
            .take(60)
            .collect();
        Some(format!("shell: {}", preview))
    } else {
        None
    }
}

#[cfg(test)]
fn shell_command_requires_approval(command: &str) -> bool {
    shell_command_requires_approval_in_cwd(command, ".")
}

fn shell_command_requires_approval_in_cwd(command: &str, cwd: &str) -> bool {
    let trimmed = command.trim();
    if trimmed.is_empty() {
        return true;
    }
    let segments = match split_shell_segments(trimmed) {
        Some(segments) => segments,
        None => return true,
    };
    segments.iter().any(|segment| {
        let tokens = match shlex::split(segment) {
            Some(tokens) if !tokens.is_empty() => tokens,
            _ => return true,
        };
        shell_tokens_are_dangerous(&tokens, cwd)
    })
}

/// Splits a shell command on sequencing operators so each segment can be
/// classified independently. Returns None for redirections, subshells, or
/// backgrounding that prevent safe static analysis.
fn split_shell_segments(command: &str) -> Option<Vec<String>> {
    let mut segments = Vec::new();
    let mut current = String::new();
    let mut chars = command.chars().peekable();
    let mut in_single = false;
    let mut in_double = false;

    let flush = |current: &mut String, segments: &mut Vec<String>| -> Option<()> {
        let seg = current.trim();
        if seg.is_empty() {
            return None;
        }
        segments.push(seg.to_string());
        current.clear();
        Some(())
    };

    while let Some(ch) = chars.next() {
        if matches!(ch, '\n' | '\r' | '`') {
            return None;
        }
        if ch == '$' && matches!(chars.peek(), Some('(')) {
            return None;
        }

        if ch == '\\' && !in_single {
            let escaped = chars.next()?;
            if matches!(escaped, '\n' | '\r') {
                return None;
            }
            current.push(ch);
            current.push(escaped);
            continue;
        }

        match ch {
            '\'' if !in_double => {
                in_single = !in_single;
                current.push(ch);
            }
            '"' if !in_single => {
                in_double = !in_double;
                current.push(ch);
            }
            '<' if !in_single && !in_double => return None,
            '>' if !in_single && !in_double => {
                if !skip_safe_output_redirection(&mut chars, &mut current) {
                    return None;
                }
            }
            ';' if !in_single && !in_double => {
                flush(&mut current, &mut segments)?;
            }
            '&' if !in_single && !in_double => {
                if matches!(chars.peek(), Some('&')) {
                    chars.next();
                    flush(&mut current, &mut segments)?;
                } else {
                    return None;
                }
            }
            '|' if !in_single && !in_double => {
                if matches!(chars.peek(), Some('|')) {
                    chars.next();
                }
                flush(&mut current, &mut segments)?;
            }
            _ => current.push(ch),
        }
    }

    if in_single || in_double {
        return None;
    }

    flush(&mut current, &mut segments)?;
    Some(segments)
}

fn skip_safe_output_redirection(
    chars: &mut std::iter::Peekable<std::str::Chars<'_>>,
    current: &mut String,
) -> bool {
    if chars.peek() == Some(&'(') {
        return false;
    }
    if chars.peek() == Some(&'>') {
        chars.next();
    }
    strip_trailing_fd_digits(current);
    consume_whitespace(chars);
    if chars.peek() == Some(&'&') {
        chars.next();
        return match chars.peek() {
            Some(c) if c.is_ascii_digit() => {
                while chars.peek().is_some_and(|c| c.is_ascii_digit()) {
                    chars.next();
                }
                true
            }
            Some('-') => {
                chars.next();
                true
            }
            _ => false,
        };
    }
    let target = take_word(chars);
    target == "/dev/null"
}

fn consume_whitespace(chars: &mut std::iter::Peekable<std::str::Chars<'_>>) {
    while let Some(&c) = chars.peek() {
        if c == ' ' || c == '\t' {
            chars.next();
        } else {
            break;
        }
    }
}

fn take_word(chars: &mut std::iter::Peekable<std::str::Chars<'_>>) -> String {
    let mut w = String::new();
    while let Some(&c) = chars.peek() {
        if matches!(
            c,
            ' ' | '\t' | '\n' | '\r' | '|' | '&' | ';' | '<' | '>' | '(' | ')'
        ) {
            break;
        }
        w.push(c);
        chars.next();
    }
    w
}

fn strip_trailing_fd_digits(current: &mut String) {
    let n_digits = current
        .chars()
        .rev()
        .take_while(|c| c.is_ascii_digit())
        .count();
    if n_digits == 0 {
        return;
    }
    let before_idx = current.len() - n_digits;
    let char_before = current[..before_idx].chars().next_back();
    if char_before.is_none_or(|c| c == ' ' || c == '\t') {
        current.truncate(before_idx);
        while current.ends_with(' ') || current.ends_with('\t') {
            current.pop();
        }
    }
}

fn shell_tokens_are_dangerous(tokens: &[String], cwd: &str) -> bool {
    let cmd = tokens[0].as_str();

    if shell_tokens_reference_sensitive_path(tokens, cwd) {
        return true;
    }

    if cmd == "dd"
        || cmd.starts_with("mkfs")
        || cmd == "fdisk"
        || cmd == "parted"
        || cmd == "diskutil"
        || cmd == "sudo"
        || cmd == "xargs"
    {
        return true;
    }

    match cmd {
        "pwd" | "ls" | "cat" | "head" | "tail" | "wc" | "rg" | "grep" | "which" | "whereis"
        | "cut" | "uniq" | "nl" | "stat" | "file" | "realpath" | "readlink" | "basename"
        | "dirname" | "echo" | "tr" | "date" | "uname" | "hostname" | "whoami" | "id"
        | "groups" | "uptime" | "w" | "who" | "last" | "df" | "du" | "ps" | "lsof" | "free"
        | "vm_stat" | "printenv" | "jq" | "base64" | "md5" | "md5sum" | "shasum" | "sha1sum"
        | "sha256sum" | "cksum" | "strings" | "od" | "hexdump" | "diff" | "cmp" | "printf"
        | "seq" | "yes" | "column" | "comm" | "join" | "paste" | "expand" | "unexpand" | "fold"
        | "fmt" | "rev" | "tac" | "dig" | "nslookup" | "host" | "ping" | "cd" | "type" | "true"
        | "false" | "sleep" | "tty" | "locale" | "nm" | "otool" | "addr2line" | "c++filt"
        | "objdump" => false,

        "curl" => curl_is_dangerous(tokens),
        "sed" => tokens
            .iter()
            .skip(1)
            .any(|t| t.starts_with("-i") || t == "--in-place"),
        "sort" | "tree" => has_output_flag(tokens, &["-o", "--output"]),
        "find" => find_is_dangerous(tokens),
        "rm" => true,
        "git" => !git_is_read_only(tokens),
        "gh" => !gh_is_read_only(tokens),
        "brew" => !brew_is_read_only(tokens),
        "perl" | "ruby" => !tokens.iter().skip(1).any(|t| t == "-c"),
        "node" => !tokens.iter().skip(1).any(|t| t == "--check"),
        "bash" | "sh" | "zsh" | "fish" | "python" | "python3" | "awk" => true,
        "cargo" => cargo_is_dangerous(tokens),
        "make" => make_is_dangerous(tokens),
        _ => true,
    }
}

fn shell_tokens_reference_sensitive_path(tokens: &[String], cwd: &str) -> bool {
    tokens
        .iter()
        .skip(1)
        .any(|token| shell_token_references_sensitive_path(token, cwd))
}

fn shell_token_references_sensitive_path(token: &str, cwd: &str) -> bool {
    for candidate in shell_path_candidates(token, cwd) {
        if crate::ai_tools::paths::reject_if_sensitive(&candidate).is_err()
            || crate::ai_tools::paths::requires_read_approval(&candidate)
        {
            return true;
        }
    }
    false
}

fn shell_path_candidates(token: &str, cwd: &str) -> Vec<std::path::PathBuf> {
    let mut candidates = Vec::new();
    push_shell_path_candidate(token, cwd, &mut candidates);

    if let Some((_, value)) = token.split_once('=') {
        push_shell_path_candidate(value, cwd, &mut candidates);
    }

    candidates
}

fn push_shell_path_candidate(raw: &str, cwd: &str, candidates: &mut Vec<std::path::PathBuf>) {
    let s = raw.trim();
    if s.is_empty() {
        return;
    }

    if let Some(expanded) = crate::ai_tools::paths::expand_user_prefix(s) {
        candidates.push(expanded);
        return;
    }

    if s.starts_with('/') {
        candidates.push(std::path::PathBuf::from(s));
        return;
    }

    if !s.starts_with('-') && !s.contains("://") {
        candidates.push(std::path::Path::new(cwd).join(s));
    }
}

fn curl_is_dangerous(tokens: &[String]) -> bool {
    for (i, t) in tokens.iter().enumerate().skip(1) {
        if t == "-X" || t == "--request" {
            match tokens.get(i + 1).map(String::as_str) {
                Some(m) if m.eq_ignore_ascii_case("GET") || m.eq_ignore_ascii_case("HEAD") => {}
                _ => return true,
            }
            continue;
        }
        if let Some(m) = t.strip_prefix("--request=") {
            if !m.eq_ignore_ascii_case("GET") && !m.eq_ignore_ascii_case("HEAD") {
                return true;
            }
            continue;
        }

        if matches!(
            t.as_str(),
            "-o" | "--output"
                | "-O"
                | "--remote-name"
                | "--remote-name-all"
                | "-T"
                | "--upload-file"
                | "-d"
                | "--data"
                | "--data-raw"
                | "--data-binary"
                | "--data-urlencode"
                | "--data-ascii"
                | "-F"
                | "--form"
                | "--form-string"
        ) {
            return true;
        }
        if t.starts_with("--output=")
            || t.starts_with("--upload-file=")
            || t.starts_with("--data")
            || t.starts_with("--form")
        {
            return true;
        }

        if t.starts_with('-') && !t.starts_with("--") && t.len() > 2 {
            let flags = &t[1..];
            if flags
                .chars()
                .any(|c| matches!(c, 'o' | 'O' | 'T' | 'd' | 'F'))
            {
                return true;
            }
            if flags.contains('X') {
                match tokens.get(i + 1).map(String::as_str) {
                    Some(m) if m.eq_ignore_ascii_case("GET") || m.eq_ignore_ascii_case("HEAD") => {}
                    _ => return true,
                }
            }
        }
    }
    false
}

fn cargo_is_dangerous(tokens: &[String]) -> bool {
    let sub = tokens
        .iter()
        .skip(1)
        .find(|t| !t.starts_with('-') && !t.starts_with('+'))
        .map(String::as_str);
    if sub == Some("clippy") && tokens.iter().skip(1).any(|t| t == "--fix") {
        return true;
    }
    matches!(
        sub,
        Some(
            "install"
                | "uninstall"
                | "publish"
                | "fmt"
                | "fix"
                | "init"
                | "new"
                | "add"
                | "remove"
                | "yank"
                | "login"
                | "logout"
                | "owner"
        )
    )
}

fn make_is_dangerous(tokens: &[String]) -> bool {
    tokens.iter().skip(1).any(|t| {
        let target = t.as_str();
        if target.starts_with('-') || target.contains('=') {
            return false;
        }
        matches!(
            target,
            "clean" | "distclean" | "fmt" | "install" | "uninstall" | "purge"
        )
    })
}

fn find_is_dangerous(tokens: &[String]) -> bool {
    tokens.iter().skip(1).any(|t| {
        matches!(
            t.as_str(),
            "-delete"
                | "-exec"
                | "-execdir"
                | "-ok"
                | "-okdir"
                | "-fprint"
                | "-fprint0"
                | "-fprintf"
                | "-fls"
        )
    })
}

fn git_is_read_only(tokens: &[String]) -> bool {
    if has_output_flag(tokens, &["-o", "--output"]) {
        return false;
    }
    match tokens.get(1).map(String::as_str) {
        Some(
            "status" | "diff" | "show" | "log" | "grep" | "ls-files" | "rev-parse" | "blame"
            | "reflog" | "shortlog" | "describe" | "merge-base" | "ls-tree" | "cat-file"
            | "rev-list" | "name-rev" | "check-ignore" | "check-attr" | "for-each-ref"
            | "whatchanged" | "count-objects" | "var",
        ) => true,
        Some("worktree") => tokens.get(2).map(String::as_str) == Some("list"),
        Some("config") => tokens.iter().skip(2).any(|t| {
            matches!(
                t.as_str(),
                "--get"
                    | "--get-all"
                    | "--get-regexp"
                    | "--get-urlmatch"
                    | "--list"
                    | "-l"
                    | "--show-origin"
                    | "--show-scope"
            )
        }),
        Some("branch") => {
            let has_write_flag = tokens.iter().skip(2).any(|t| {
                t == "-d" || t == "-D" || t == "-m" || t == "-M" || t == "--delete" || t == "--move"
            });
            if has_write_flag {
                return false;
            }
            let has_list_flag = tokens.iter().skip(2).any(|t| t == "-l" || t == "--list");
            if has_list_flag {
                return true;
            }
            !tokens.iter().skip(2).any(|t| !t.starts_with('-'))
        }
        Some("tag") => {
            let has_write_flag = tokens
                .iter()
                .skip(2)
                .any(|t| t == "-d" || t == "-D" || t == "--delete");
            if has_write_flag {
                return false;
            }
            let has_list_flag = tokens.iter().skip(2).any(|t| t == "-l" || t == "--list");
            if has_list_flag {
                return true;
            }
            !tokens.iter().skip(2).any(|t| !t.starts_with('-'))
        }
        Some("remote") => {
            !tokens
                .iter()
                .skip(2)
                .any(|t| t == "add" || t == "remove" || t == "rename" || t == "set-url")
                && !tokens.iter().skip(2).any(|t| !t.starts_with('-'))
        }
        Some("stash") => {
            matches!(
                tokens.get(2).map(String::as_str),
                Some("list") | Some("show")
            )
        }
        _ => false,
    }
}

fn gh_is_read_only(tokens: &[String]) -> bool {
    let sub = match tokens.get(1).map(String::as_str) {
        Some(s) => s,
        None => return true,
    };
    if matches!(sub, "search" | "status" | "version" | "help") {
        return true;
    }
    let verb = tokens.get(2).map(String::as_str);
    match (sub, verb) {
        (
            "issue" | "pr" | "repo" | "release" | "label" | "workflow" | "run" | "gist" | "project"
            | "ruleset" | "secret" | "variable" | "cache" | "extension",
            Some("list" | "view" | "diff" | "checks" | "status" | "show"),
        ) => true,
        ("auth", Some("status")) => true,
        ("alias", Some("list")) => true,
        ("api", _) => gh_api_is_get(tokens),
        _ => false,
    }
}

fn brew_is_read_only(tokens: &[String]) -> bool {
    let sub = match tokens.get(1).map(String::as_str) {
        Some(s) => s,
        None => return true,
    };
    matches!(
        sub,
        "list"
            | "ls"
            | "info"
            | "abv"
            | "search"
            | "outdated"
            | "home"
            | "homepage"
            | "doctor"
            | "dr"
            | "deps"
            | "uses"
            | "leaves"
            | "desc"
            | "options"
            | "config"
            | "--cache"
            | "--prefix"
            | "--cellar"
            | "--repository"
            | "--repo"
            | "--caskroom"
            | "--version"
            | "-v"
            | "help"
    )
}

fn gh_api_is_get(tokens: &[String]) -> bool {
    let mut iter = tokens.iter().skip(2).peekable();
    while let Some(t) = iter.next() {
        if t == "-X" || t == "--method" {
            return iter.next().is_some_and(|m| m.eq_ignore_ascii_case("GET"));
        }
        if let Some(rest) = t.strip_prefix("--method=") {
            return rest.eq_ignore_ascii_case("GET");
        }
        if matches!(
            t.as_str(),
            "-F" | "-f" | "--field" | "--raw-field" | "--input"
        ) {
            return false;
        }
    }
    true
}

fn has_output_flag(tokens: &[String], flags: &[&str]) -> bool {
    tokens.iter().skip(1).any(|token| {
        flags.contains(&token.as_str())
            || flags.iter().any(|flag| {
                if let Some(long_flag) = flag.strip_prefix("--") {
                    token.starts_with(&format!("--{}=", long_flag))
                } else {
                    token.starts_with(flag) && token.len() > flag.len()
                }
            })
    })
}

#[cfg(test)]
mod tests {
    use super::{approval_summary, shell_command_requires_approval};

    #[test]
    fn fs_read_prompts_only_for_credential_named_source_files() {
        let gated = serde_json::json!({ "path": "/proj/src/credentials.py" });
        let summary = approval_summary("fs_read", &gated).expect("credential-named source prompts");
        assert!(summary.contains("credentials.py"), "{}", summary);

        let plain = serde_json::json!({ "path": "/proj/src/main.py" });
        assert!(approval_summary("fs_read", &plain).is_none());
    }

    #[test]
    fn relative_credential_source_shell_reads_require_approval() {
        assert!(shell_command_requires_approval(
            "cat src/prod_credentials.py"
        ));
        assert!(shell_command_requires_approval(
            "rg token src/prod_credentials.ts"
        ));
    }

    #[test]
    fn http_request_summary_flattens_control_characters() {
        let args = serde_json::json!({
            "method": "GET\n[approve: fake? y/N]\u{1b}[31m",
            "url": "https://ok.example\n[approve: fake? y/N]\u{1b}[31m"
        });
        let summary = approval_summary("http_request", &args).expect("GET now prompts");
        assert!(
            !summary.contains('\n'),
            "newline must not survive: {}",
            summary
        );
        assert!(
            !summary.contains('\u{1b}'),
            "ESC must not survive: {}",
            summary
        );
    }

    #[test]
    fn stderr_to_dev_null_no_approval() {
        assert!(!shell_command_requires_approval(
            "ls -la ~/www/kaku 2>/dev/null"
        ));
    }

    #[test]
    fn stderr_with_spaces_no_approval() {
        assert!(!shell_command_requires_approval("ls 2> /dev/null"));
    }

    #[test]
    fn stderr_to_stdout_fd_dup_no_approval() {
        assert!(!shell_command_requires_approval("cat foo 2>&1 | grep bar"));
    }

    #[test]
    fn stdin_input_requires_approval() {
        assert!(shell_command_requires_approval("grep foo < input.txt"));
    }

    #[test]
    fn original_bug_report_no_approval() {
        assert!(!shell_command_requires_approval(
            "ls -la ~/www/kaku 2>/dev/null || echo \"Not found\""
        ));
    }

    #[test]
    fn write_to_real_file_requires_approval() {
        assert!(shell_command_requires_approval("echo hi > /tmp/foo"));
    }

    #[test]
    fn append_to_real_file_requires_approval() {
        assert!(shell_command_requires_approval("echo hi >> /tmp/foo"));
    }

    #[test]
    fn process_substitution_requires_approval() {
        assert!(shell_command_requires_approval(
            "diff <(ls dir1) <(ls dir2)"
        ));
    }

    #[test]
    fn plain_write_redirect_requires_approval() {
        assert!(shell_command_requires_approval("cat foo.txt > bar.txt"));
    }

    #[test]
    fn rm_requires_approval() {
        assert!(shell_command_requires_approval("rm -rf /tmp/old"));
    }

    #[test]
    fn git_reset_hard_requires_approval() {
        assert!(shell_command_requires_approval("git reset --hard HEAD~1"));
    }

    #[test]
    fn mv_requires_approval() {
        assert!(shell_command_requires_approval("mv foo.txt bar.txt"));
    }

    #[test]
    fn grep_read_only_no_approval() {
        assert!(!shell_command_requires_approval("grep -r TODO src/"));
    }

    #[test]
    fn cat_no_approval() {
        assert!(!shell_command_requires_approval("cat Cargo.toml"));
    }

    #[test]
    fn cat_ssh_key_requires_approval() {
        assert!(shell_command_requires_approval("cat ~/.ssh/id_rsa"));
        assert!(shell_command_requires_approval("cat $HOME/.ssh/id_rsa"));
        assert!(shell_command_requires_approval("cat ${HOME}/.ssh/id_rsa"));
    }

    #[test]
    fn grep_aws_credentials_requires_approval() {
        assert!(shell_command_requires_approval(
            "grep aws_secret_access_key ~/.aws/credentials"
        ));
    }

    #[test]
    fn option_secret_path_requires_approval() {
        assert!(shell_command_requires_approval("cat --input=~/.ssh/id_rsa"));
    }

    #[test]
    fn git_log_no_approval() {
        assert!(!shell_command_requires_approval("git log --oneline -10"));
    }

    #[test]
    fn git_status_no_approval() {
        assert!(!shell_command_requires_approval("git status"));
    }

    #[test]
    fn ls_piped_to_grep_no_approval() {
        assert!(!shell_command_requires_approval("ls -la | grep Cargo"));
    }

    #[test]
    fn chained_safe_commands_no_approval() {
        assert!(!shell_command_requires_approval(
            "cd /tmp && ls -la && cat README.md"
        ));
    }

    #[test]
    fn cargo_check_no_approval() {
        assert!(!shell_command_requires_approval("cargo check"));
    }

    #[test]
    fn cargo_test_no_approval() {
        assert!(!shell_command_requires_approval("cargo test"));
    }

    #[test]
    fn cargo_build_no_approval() {
        assert!(!shell_command_requires_approval("cargo build --release"));
    }

    #[test]
    fn cargo_fmt_requires_approval() {
        assert!(shell_command_requires_approval("cargo fmt"));
        assert!(shell_command_requires_approval("cargo +nightly fmt"));
        assert!(shell_command_requires_approval("cargo clippy --fix"));
        assert!(shell_command_requires_approval("cargo fix"));
    }

    #[test]
    fn cargo_install_requires_approval() {
        assert!(shell_command_requires_approval("cargo install ripgrep"));
    }

    #[test]
    fn cargo_uninstall_requires_approval() {
        assert!(shell_command_requires_approval("cargo uninstall ripgrep"));
    }

    #[test]
    fn cargo_publish_requires_approval() {
        assert!(shell_command_requires_approval("cargo publish"));
    }

    #[test]
    fn cargo_add_requires_approval() {
        assert!(shell_command_requires_approval("cargo add serde"));
    }

    #[test]
    fn make_no_approval() {
        assert!(!shell_command_requires_approval("make"));
    }

    #[test]
    fn make_app_no_approval() {
        assert!(!shell_command_requires_approval("make app"));
    }

    #[test]
    fn make_flags_no_approval() {
        assert!(!shell_command_requires_approval("make -j8 CC=gcc"));
    }

    #[test]
    fn make_fmt_requires_approval() {
        assert!(shell_command_requires_approval("make fmt"));
    }

    #[test]
    fn make_clean_requires_approval() {
        assert!(shell_command_requires_approval("make clean"));
    }

    #[test]
    fn make_install_requires_approval() {
        assert!(shell_command_requires_approval("make install"));
    }

    #[test]
    fn make_distclean_requires_approval() {
        assert!(shell_command_requires_approval("make distclean"));
    }

    #[test]
    fn read_only_tools_return_none() {
        let empty = serde_json::json!({});
        for tool in &[
            "fs_read",
            "fs_list",
            "fs_search",
            "pwd",
            "shell_poll",
            "memory_read",
        ] {
            assert!(
                approval_summary(tool, &empty).is_none(),
                "{} should not require approval",
                tool
            );
        }
    }

    #[test]
    fn fs_write_requires_approval_and_mentions_path() {
        let args = serde_json::json!({"path": "/tmp/out.txt"});
        let summary = approval_summary("fs_write", &args);
        assert!(summary.is_some());
        assert!(summary.unwrap().contains("out.txt"));
    }

    #[test]
    fn fs_patch_requires_approval() {
        let args = serde_json::json!({"path": "src/main.rs"});
        assert!(approval_summary("fs_patch", &args).is_some());
    }

    #[test]
    fn fs_delete_requires_approval() {
        let args = serde_json::json!({"path": "/tmp/stale.log"});
        assert!(approval_summary("fs_delete", &args).is_some());
    }

    #[test]
    fn fs_mkdir_requires_approval() {
        let args = serde_json::json!({"path": "/tmp/newdir"});
        assert!(approval_summary("fs_mkdir", &args).is_some());
    }

    #[test]
    fn shell_bg_requires_approval() {
        let args = serde_json::json!({"command": "long_running_task"});
        assert!(approval_summary("shell_bg", &args).is_some());
    }

    #[test]
    fn http_get_requires_approval() {
        let args = serde_json::json!({"method": "GET", "url": "https://example.com/api"});
        assert!(approval_summary("http_request", &args).is_some());
    }

    #[test]
    fn http_post_requires_approval() {
        let args = serde_json::json!({"method": "POST", "url": "https://api.example.com/data"});
        let summary = approval_summary("http_request", &args);
        assert!(summary.is_some());
        assert!(summary.unwrap().contains("POST"));
    }

    #[test]
    fn http_delete_requires_approval() {
        let args = serde_json::json!({"method": "DELETE", "url": "https://api.example.com/r/1"});
        assert!(approval_summary("http_request", &args).is_some());
    }
}
