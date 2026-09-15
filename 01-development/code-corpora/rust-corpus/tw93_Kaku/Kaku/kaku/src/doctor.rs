//! Doctor command for diagnosing shell integration, environment, and runtime issues.

use crate::shell::{resolve_shell_kind, ManagedShell, ShellKind};
use clap::Parser;
use std::fs;
use std::io::{self, ErrorKind, Write};
use std::path::{Path, PathBuf};
use std::process::Command;
use std::time::{Duration, Instant};

#[derive(Debug, Parser, Clone, Default)]
pub struct DoctorCommand {
    /// Apply safe automatic fixes, then rerun diagnostics
    #[arg(long)]
    pub fix: bool,

    /// Prompt before offering safe automatic fixes
    #[arg(long)]
    pub prompt_fix: bool,

    /// Shell integration to diagnose and repair
    #[arg(long, value_enum)]
    pub shell: Option<ManagedShell>,
}

impl DoctorCommand {
    pub fn run(&self) -> anyhow::Result<()> {
        let report = build_report(self.shell);
        print!("{}", render_text_report(&report));

        if self.fix {
            run_auto_fix_and_rerun_report(self.shell);
            return Ok(());
        }

        if self.prompt_fix && should_offer_auto_fix(&report) {
            match prompt_yes_no("Run safe auto-fix now with `kaku init --update-only`? [Y/n] ") {
                Ok(true) => run_auto_fix_and_rerun_report(self.shell),
                Ok(false) => {}
                Err(err) => eprintln!("Auto-fix prompt skipped: {}", err),
            }
        }

        Ok(())
    }
}

fn should_offer_auto_fix(report: &DoctorReport) -> bool {
    report.overall_status.severity_rank() >= DoctorStatus::Warn.severity_rank()
}

fn prompt_yes_no(question: &str) -> anyhow::Result<bool> {
    print!("{}", question);
    io::stdout().flush()?;

    let mut input = String::new();
    let bytes = io::stdin().read_line(&mut input)?;
    if bytes == 0 {
        return Ok(false);
    }
    let answer = input.trim().to_ascii_lowercase();
    Ok(answer.is_empty() || answer == "y" || answer == "yes")
}

fn run_auto_fix_and_rerun_report(shell: Option<ManagedShell>) {
    println!("Auto-fix: running `kaku init --update-only`");
    let init_cmd = crate::init::InitCommand {
        update_only: true,
        shell,
    };
    match init_cmd.run() {
        Ok(()) => println!("Auto-fix: completed"),
        Err(err) => println!("Auto-fix: failed: {:#}", err),
    }

    let after = build_report(shell);
    println!();
    println!("After Auto-fix");
    print!("{}", render_text_report(&after));
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum DoctorStatus {
    Ok,
    Warn,
    Fail,
    Info,
}

impl DoctorStatus {
    fn severity_rank(self) -> u8 {
        match self {
            Self::Fail => 3,
            Self::Warn => 2,
            Self::Ok => 1,
            Self::Info => 0,
        }
    }

    fn label(self) -> &'static str {
        match self {
            Self::Ok => "OK",
            Self::Warn => "WARN",
            Self::Fail => "FAIL",
            Self::Info => "INFO",
        }
    }

    fn icon(self) -> &'static str {
        match self {
            Self::Ok => "✓",
            Self::Warn => "!",
            Self::Fail => "x",
            Self::Info => "i",
        }
    }
}

#[derive(Debug)]
struct DoctorReport {
    overall_status: DoctorStatus,
    summary: DoctorSummary,
    groups: Vec<DoctorGroup>,
}

#[derive(Debug)]
struct DoctorSummary {
    ok: usize,
    warn: usize,
    fail: usize,
    info: usize,
}

#[derive(Debug)]
struct DoctorGroup {
    title: &'static str,
    status: DoctorStatus,
    checks: Vec<DoctorCheck>,
}

#[derive(Debug)]
struct DoctorCheck {
    title: &'static str,
    status: DoctorStatus,
    summary: String,
    details: Vec<String>,
    fix: Option<String>,
}

fn build_report(shell: Option<ManagedShell>) -> DoctorReport {
    let shell_kind = resolve_shell_kind(shell);
    let env_group = build_environment_group(&shell_kind, shell);
    let shell_group = build_shell_integration_group(&shell_kind, shell);
    let runtime_group = build_runtime_group(&shell_kind);

    let mut all_checks = Vec::new();
    all_checks.extend(env_group.checks.iter());
    all_checks.extend(shell_group.checks.iter());
    all_checks.extend(runtime_group.checks.iter());

    let summary = DoctorSummary {
        ok: all_checks
            .iter()
            .filter(|c| c.status == DoctorStatus::Ok)
            .count(),
        warn: all_checks
            .iter()
            .filter(|c| c.status == DoctorStatus::Warn)
            .count(),
        fail: all_checks
            .iter()
            .filter(|c| c.status == DoctorStatus::Fail)
            .count(),
        info: all_checks
            .iter()
            .filter(|c| c.status == DoctorStatus::Info)
            .count(),
    };

    let overall_status = if summary.fail > 0 {
        DoctorStatus::Fail
    } else if summary.warn > 0 {
        DoctorStatus::Warn
    } else {
        DoctorStatus::Ok
    };

    let health_group = build_health_group(overall_status, &summary);

    DoctorReport {
        overall_status,
        summary,
        groups: vec![health_group, env_group, shell_group, runtime_group],
    }
}

fn build_health_group(overall_status: DoctorStatus, summary: &DoctorSummary) -> DoctorGroup {
    let mut details = vec![
        format!(
            "Summary: {} ok, {} warn, {} fail, {} info",
            summary.ok, summary.warn, summary.fail, summary.info
        ),
        format!("Kaku version: {}", doctor_version_string()),
    ];

    if summary.fail > 0 || summary.warn > 0 {
        details.push("Run `kaku init --update-only` after fixing shell or PATH issues".to_string());
    }

    let checks = vec![DoctorCheck {
        title: "Overall Health",
        status: overall_status,
        summary: match overall_status {
            DoctorStatus::Ok => "No blocking issues detected".to_string(),
            DoctorStatus::Warn => "Kaku works but setup is incomplete".to_string(),
            DoctorStatus::Fail => "Kaku command entry is broken or missing".to_string(),
            DoctorStatus::Info => "Informational only".to_string(),
        },
        details,
        fix: if overall_status.severity_rank() >= DoctorStatus::Warn.severity_rank() {
            Some("kaku init --update-only".to_string())
        } else {
            None
        },
    }];

    DoctorGroup {
        title: "Health",
        status: group_status(&checks),
        checks,
    }
}

fn init_update_command(shell: Option<ManagedShell>) -> String {
    match shell {
        Some(shell) => format!("kaku init --update-only --shell {}", shell.name()),
        None => "kaku init --update-only".to_string(),
    }
}

fn build_environment_group(
    shell_kind: &ShellKind,
    selected_shell: Option<ManagedShell>,
) -> DoctorGroup {
    let mut checks = Vec::new();

    let shell = std::env::var("SHELL").ok();
    let shell_supported = shell_kind.is_managed();
    let mut shell_details = vec![
        "Kaku shell integration supports zsh and fish for PATH injection and managed shell config"
            .to_string(),
        "Doctor reports the current process environment. GUI-launched apps can differ from a Terminal login shell."
            .to_string(),
    ];
    if let Some(selected) = selected_shell {
        shell_details.push(format!(
            "The --shell {} override selects which integration Doctor checks and repairs",
            selected.name()
        ));
    }

    checks.push(DoctorCheck {
        title: "Current Shell Environment",
        status: if shell_supported {
            DoctorStatus::Ok
        } else if shell.is_some() {
            DoctorStatus::Warn
        } else {
            DoctorStatus::Info
        },
        summary: match (selected_shell, &shell) {
            (Some(selected), Some(value)) => {
                format!(
                    "Selected {} via --shell (SHELL is {value})",
                    selected.name()
                )
            }
            (Some(selected), None) => format!("Selected {} via --shell", selected.name()),
            (None, Some(value)) if shell_supported => {
                // Diagnosis follows the persisted selection, not $SHELL; say
                // so when they disagree (e.g. chsh to bash after `kaku init`).
                match crate::shell::persisted_managed_shell() {
                    Some(persisted) if !value.ends_with(persisted.name()) => format!(
                        "Checking selected {} integration (SHELL is {value})",
                        persisted.name()
                    ),
                    _ => format!("SHELL is {value}"),
                }
            }
            (None, Some(value)) => format!("SHELL is {value} (zsh and fish are supported)"),
            (None, None) => "SHELL is not set".to_string(),
        },
        details: shell_details,
        fix: if !shell_supported {
            Some("Use zsh or fish, or add the kaku bin dir to your shell PATH manually".to_string())
        } else {
            None
        },
    });

    if shell_kind.is_managed() {
        let managed_bin = managed_bin_dir(shell_kind);
        let path_entries: Vec<PathBuf> =
            std::env::split_paths(&std::env::var_os("PATH").unwrap_or_default()).collect();
        let path_has_managed_bin = path_entries.iter().any(|entry| entry == &managed_bin);
        let bin_dir_display = managed_bin.display().to_string();
        let init_command = init_update_command(selected_shell);
        let restart_hint = if *shell_kind == ShellKind::Fish {
            format!("Run `{init_command}` and restart fish")
        } else {
            format!("Run `{init_command}` and restart zsh with `exec zsh -l`")
        };
        checks.push(DoctorCheck {
            title: "PATH Contains Kaku Managed Bin",
            status: if path_has_managed_bin {
                DoctorStatus::Ok
            } else {
                DoctorStatus::Warn
            },
            summary: if path_has_managed_bin {
                format!("PATH includes {}", bin_dir_display)
            } else {
                format!("PATH is missing {}", bin_dir_display)
            },
            details: vec![
                format!("Kaku command wrapper is expected at {}/kaku", bin_dir_display),
                "This PATH entry is normally added by Kaku shell integration on startup".to_string(),
                "PATH in Doctor reflects the current process environment and can differ between GUI and Terminal launches."
                    .to_string(),
            ],
            fix: if path_has_managed_bin {
                None
            } else {
                Some(restart_hint)
            },
        });

        if *shell_kind == ShellKind::Fish {
            let conf_d = home_dir()
                .join(".config")
                .join("fish")
                .join("conf.d")
                .join("kaku.fish");
            checks.push(DoctorCheck {
                title: "Fish conf.d Entry Point",
                status: DoctorStatus::Info,
                summary: if conf_d.exists() {
                    format!("Found {}", conf_d.display())
                } else {
                    format!("Not present: {}", conf_d.display())
                },
                details: vec![format!(
                    "Fish loads Kaku integration via {}",
                    conf_d.display()
                )],
                fix: None,
            });
        } else {
            let zdotdir = std::env::var_os("ZDOTDIR").map(PathBuf::from);
            checks.push(DoctorCheck {
                title: "Zsh Config Target Path",
                status: DoctorStatus::Info,
                summary: match &zdotdir {
                    Some(dir) => format!("ZDOTDIR is {}", dir.display()),
                    None => "ZDOTDIR is not set and ~/.zshrc is used".to_string(),
                },
                details: vec![format!("Expected zshrc path: {}", zshrc_path().display())],
                fix: None,
            });
        }
    } else {
        checks.push(DoctorCheck {
            title: "PATH Contains Kaku Managed Bin",
            status: DoctorStatus::Info,
            summary: "Current shell is not managed by Kaku; skipping shell-specific PATH check"
                .to_string(),
            details: vec![
                "Kaku shell integration manages PATH for zsh and fish.".to_string(),
                "For other shells, add the kaku bin directory to PATH manually.".to_string(),
            ],
            fix: None,
        });
    }

    DoctorGroup {
        title: "Environment",
        status: group_status(&checks),
        checks,
    }
}

fn wrapper_check(
    title: &'static str,
    path: std::path::PathBuf,
    missing_status: DoctorStatus,
    details: Vec<String>,
) -> DoctorCheck {
    let exists = path.is_file();
    let exec = config::is_executable_file(&path);
    DoctorCheck {
        title,
        status: if exists && exec {
            DoctorStatus::Ok
        } else if exists {
            DoctorStatus::Warn
        } else {
            missing_status
        },
        summary: if exists && exec {
            format!("{} is ready at {}", title, path.display())
        } else if exists {
            format!("{} exists but is not executable: {}", title, path.display())
        } else {
            format!("{} is missing: {}", title, path.display())
        },
        details,
        fix: if exists && exec {
            None
        } else if exists {
            Some(format!("Run `chmod +x {}`", path.display()))
        } else {
            Some("Run `kaku init --update-only`".to_string())
        },
    }
}

fn build_shell_integration_group(
    shell_kind: &ShellKind,
    selected_shell: Option<ManagedShell>,
) -> DoctorGroup {
    let mut checks = Vec::new();

    if !shell_kind.is_managed() {
        checks.push(DoctorCheck {
            title: "Shell Integration",
            status: DoctorStatus::Info,
            summary: format!(
                "Current shell ({}) is not managed by Kaku; shell-specific checks skipped",
                shell_kind.name()
            ),
            details: vec!["Kaku shell integration supports zsh and fish.".to_string()],
            fix: None,
        });
        return DoctorGroup {
            title: "Shell Integration",
            status: group_status(&checks),
            checks,
        };
    }

    let is_fish = *shell_kind == ShellKind::Fish;
    let init_command = init_update_command(selected_shell);
    let autosuggest_provider = if is_fish {
        None
    } else {
        detect_external_autosuggest_cli_provider()
    };

    let init_file = managed_init_file(shell_kind);
    let init_exists = init_file.is_file();
    let init_title = if is_fish {
        "Managed Fish Init File"
    } else {
        "Managed Zsh Init File"
    };
    checks.push(DoctorCheck {
        title: init_title,
        status: if init_exists {
            DoctorStatus::Ok
        } else {
            DoctorStatus::Warn
        },
        summary: if init_exists {
            format!("Found {}", init_file.display())
        } else {
            format!("Missing {}", init_file.display())
        },
        details: vec!["Kaku writes PATH and shell integration to this managed file".to_string()],
        fix: if init_exists {
            None
        } else {
            Some(format!("Run `{init_command}`"))
        },
    });

    if let Some(provider) = autosuggest_provider {
        checks.push(build_zsh_external_autosuggest_check(&init_file, provider));
    }

    checks.push(wrapper_check(
        "Kaku Wrapper Script",
        managed_wrapper_path(shell_kind),
        DoctorStatus::Fail,
        vec![
            "The `kaku` shell command is provided by this wrapper".to_string(),
            "Wrapper is generated by `kaku init` before shell setup runs".to_string(),
        ],
    ));

    checks.push(wrapper_check(
        "k Wrapper Script",
        managed_bin_dir(shell_kind).join("k"),
        DoctorStatus::Warn,
        vec![
            "The `k` command launches AI chat from any terminal".to_string(),
            "Created alongside the kaku wrapper by `kaku init`".to_string(),
        ],
    ));

    if is_fish {
        let conf_d = home_dir()
            .join(".config")
            .join("fish")
            .join("conf.d")
            .join("kaku.fish");
        let source_check = check_fish_conf_d_source_line(&conf_d);
        let mut details = vec![
            format!("Checked {}", conf_d.display()),
            "Fish loads Kaku integration via this conf.d file on startup".to_string(),
        ];
        if !source_check.has_valid_source
            && !source_check.missing_file
            && source_check.read_error.is_none()
        {
            details.push(
                "Expected an active line that sources ~/.config/kaku/fish/kaku.fish".to_string(),
            );
        }
        checks.push(DoctorCheck {
            title: "fish conf.d Sources Kaku Init",
            status: if source_check.read_error.is_some() {
                DoctorStatus::Fail
            } else if source_check.has_valid_source {
                DoctorStatus::Ok
            } else {
                DoctorStatus::Warn
            },
            summary: if let Some(err) = &source_check.read_error {
                format!("Could not read {}: {}", conf_d.display(), err)
            } else if source_check.missing_file {
                format!("Missing {}", conf_d.display())
            } else if source_check.has_valid_source {
                format!("Found valid Kaku source entry in {}", conf_d.display())
            } else {
                format!("No valid Kaku source entry in {}", conf_d.display())
            },
            details,
            fix: if source_check.has_valid_source {
                None
            } else {
                Some(format!("Run `{init_command}`"))
            },
        });
    } else {
        let zshrc = zshrc_path();
        let source_check = check_zshrc_source_line(&zshrc);
        checks.push(DoctorCheck {
            title: "zshrc Sources Kaku Init",
            status: if source_check.read_error.is_some() {
                DoctorStatus::Fail
            } else if source_check.has_active_lines() && !source_check.has_legacy_guarded_lines() {
                DoctorStatus::Ok
            } else {
                DoctorStatus::Warn
            },
            summary: if let Some(err) = &source_check.read_error {
                format!("Could not read {}: {}", zshrc.display(), err)
            } else if source_check.missing_file {
                format!("No zshrc file found at {}", zshrc.display())
            } else if source_check.has_legacy_guarded_lines() {
                format!(
                    "Found {} active Kaku source line(s), including {} legacy guarded line(s) in {}",
                    source_check.guarded_active_lines + source_check.unguarded_active_lines,
                    source_check.guarded_active_lines,
                    zshrc.display()
                )
            } else if source_check.has_active_lines() {
                format!(
                    "Found {} active Kaku source line(s) in {}",
                    source_check.guarded_active_lines + source_check.unguarded_active_lines,
                    zshrc.display()
                )
            } else {
                format!("No active Kaku source line in {}", zshrc.display())
            },
            details: source_check.details(&zshrc),
            fix: if source_check.read_error.is_some() {
                Some(format!(
                    "Fix permissions or path access for {} then run `kaku doctor` again",
                    zshrc.display()
                ))
            } else if source_check.has_active_lines() && !source_check.has_legacy_guarded_lines() {
                None
            } else {
                Some(format!("Run `{init_command}`"))
            },
        });
    }

    DoctorGroup {
        title: "Shell Integration",
        status: group_status(&checks),
        checks,
    }
}

fn build_runtime_group(shell_kind: &ShellKind) -> DoctorGroup {
    let mut checks = Vec::new();

    let candidates = kaku_bin_candidates();
    let existing = candidates
        .iter()
        .find(|p| config::is_executable_file(p))
        .cloned();
    checks.push(DoctorCheck {
        title: "Kaku App Binary",
        status: if existing.is_some() {
            DoctorStatus::Ok
        } else {
            DoctorStatus::Fail
        },
        summary: match &existing {
            Some(path) => format!("Found executable {}", path.display()),
            None => "Kaku CLI binary not found in known locations".to_string(),
        },
        details: candidates
            .iter()
            .map(|p| format!("Checked {}", p.display()))
            .collect(),
        fix: if existing.is_some() {
            None
        } else {
            Some("Install Kaku.app to /Applications or ~/Applications".to_string())
        },
    });

    let wrapper = managed_wrapper_path(shell_kind);
    let wrapper_probe = probe_wrapper(&wrapper);
    checks.push(DoctorCheck {
        title: "Wrapper Execution Probe",
        status: wrapper_probe.status,
        summary: wrapper_probe.summary,
        details: wrapper_probe.details,
        fix: wrapper_probe.fix,
    });

    let login_shell_probe = probe_login_shell_integration(shell_kind);
    checks.push(DoctorCheck {
        title: login_shell_probe.title,
        status: login_shell_probe.status,
        summary: login_shell_probe.summary,
        details: login_shell_probe.details,
        fix: login_shell_probe.fix,
    });

    #[cfg(target_os = "macos")]
    checks.push(build_local_network_check());

    DoctorGroup {
        title: "Runtime",
        status: group_status(&checks),
        checks,
    }
}

#[cfg(target_os = "macos")]
fn build_local_network_check() -> DoctorCheck {
    let app_bundle = kaku_bin_candidates()
        .into_iter()
        .find(|path| config::is_executable_file(path))
        .and_then(|path| {
            path.parent()
                .and_then(Path::parent)
                .and_then(Path::parent)
                .map(PathBuf::from)
        });

    let mut details = vec![
        "If LAN access works in Terminal or iTerm2 but fails in Kaku, compare the two launch contexts before changing shell or PATH setup.".to_string(),
        "Run these in both apps: `route -n get <ip>`, `netstat -rn | grep <subnet>`, `ifconfig`, `scutil --nwi`, `ping -v <ip>`, `nc -vz <ip> 22`.".to_string(),
        "Check macOS System Settings > Privacy & Security > Local Network and confirm Kaku is allowed.".to_string(),
        "Compare launching Kaku from Finder/Dock versus Terminal, for example `open -na /Applications/Kaku.app`.".to_string(),
    ];

    if let Some(bundle) = app_bundle {
        details.push(format!(
            "Detected app bundle candidate: {}",
            bundle.display()
        ));
    } else {
        details.push(
            "No installed Kaku.app bundle was detected in the standard locations.".to_string(),
        );
    }

    DoctorCheck {
        title: "Local Network Troubleshooting",
        status: DoctorStatus::Info,
        summary: "Use this when local-network access differs between Kaku and other terminals"
            .to_string(),
        details,
        fix: None,
    }
}

fn detect_external_autosuggest_cli_provider() -> Option<&'static str> {
    if path_has_executable("kiro-cli") {
        Some("kiro-cli")
    } else if path_has_executable("q") && is_amazon_q_cli() {
        Some("q")
    } else {
        None
    }
}

/// Verify the `q` binary is Amazon Q CLI by checking its version output.
/// Guards against false positives from other tools named `q`.
/// Uses spawn()+try_wait() with a 3s timeout to avoid blocking indefinitely.
fn is_amazon_q_cli() -> bool {
    let mut child = match std::process::Command::new("q")
        .arg("--version")
        .stdout(std::process::Stdio::piped())
        .stderr(std::process::Stdio::null())
        .spawn()
    {
        Ok(c) => c,
        Err(_) => return false,
    };
    let deadline = Instant::now() + Duration::from_secs(3);
    loop {
        match child.try_wait() {
            Ok(Some(_)) => {
                return child
                    .wait_with_output()
                    .ok()
                    .map(|o| {
                        String::from_utf8_lossy(&o.stdout)
                            .to_lowercase()
                            .contains("amazon")
                    })
                    .unwrap_or(false);
            }
            Ok(None) if Instant::now() < deadline => {
                std::thread::sleep(Duration::from_millis(50));
            }
            _ => {
                let _ = child.kill();
                let _ = child.wait();
                return false;
            }
        }
    }
}

fn path_has_executable(name: &str) -> bool {
    let Some(path) = std::env::var_os("PATH") else {
        return false;
    };

    std::env::split_paths(&path).any(|dir| config::is_executable_file(&dir.join(name)))
}

fn zsh_autosuggest_provider_marker(provider: &str) -> String {
    format!(r#"typeset -g _kaku_autosuggest_cli_provider="{provider}""#)
}

fn zsh_init_has_autosuggest_provider_marker(content: &str, provider: &str) -> bool {
    content.contains(&zsh_autosuggest_provider_marker(provider))
}

fn zsh_init_loads_bundled_autosuggestions(content: &str) -> bool {
    content
        .contains(r#"source "$KAKU_ZSH_DIR/plugins/zsh-autosuggestions/zsh-autosuggestions.zsh""#)
}

fn zsh_init_defers_autosuggestions_to_provider(content: &str, provider: &str) -> bool {
    zsh_init_has_autosuggest_provider_marker(content, provider)
        && !zsh_init_loads_bundled_autosuggestions(content)
}

fn build_zsh_external_autosuggest_check(init_file: &Path, provider: &str) -> DoctorCheck {
    let fix = format!(
        "Run `kaku init --update-only` to regenerate {} with {} autosuggest compatibility, then restart zsh with `exec zsh -l`",
        init_file.display(),
        provider
    );
    let mut details = vec![
        format!("Detected external autosuggest CLI on PATH: {provider}"),
        format!(
            "Checked {} for the provider marker and bundled zsh-autosuggestions source line",
            init_file.display()
        ),
        "When compatibility is active, Kaku keeps Tab bound but leaves it in completion-only mode to avoid widget recursion.".to_string(),
    ];

    let init_content = match fs::read_to_string(init_file) {
        Ok(content) => content,
        Err(err) if err.kind() == ErrorKind::NotFound => {
            return DoctorCheck {
                title: "External Autosuggest Compatibility",
                status: DoctorStatus::Warn,
                summary: format!(
                    "Detected {provider} on PATH, but {} is missing",
                    init_file.display()
                ),
                details,
                fix: Some(fix),
            };
        }
        Err(err) => {
            details.push(format!("Read error: {}", err));
            return DoctorCheck {
                title: "External Autosuggest Compatibility",
                status: DoctorStatus::Warn,
                summary: format!(
                    "Detected {provider} on PATH, but {} could not be read",
                    init_file.display()
                ),
                details,
                fix: Some(fix),
            };
        }
    };

    if zsh_init_defers_autosuggestions_to_provider(&init_content, provider) {
        return DoctorCheck {
            title: "External Autosuggest Compatibility",
            status: DoctorStatus::Info,
            summary: format!(
                "Detected {provider} on PATH; managed zsh init defers autosuggestions to the external provider"
            ),
            details,
            fix: None,
        };
    }

    if !zsh_init_has_autosuggest_provider_marker(&init_content, provider) {
        details.push(format!(
            "Missing provider marker: {}",
            zsh_autosuggest_provider_marker(provider)
        ));
    }
    if zsh_init_loads_bundled_autosuggestions(&init_content) {
        details.push(
            "Managed zsh init still sources bundled zsh-autosuggestions, which can reintroduce widget recursion.".to_string(),
        );
    }

    DoctorCheck {
        title: "External Autosuggest Compatibility",
        status: DoctorStatus::Warn,
        summary: format!(
            "Detected {provider} on PATH, but managed zsh init is not in compatibility mode"
        ),
        details,
        fix: Some(fix),
    }
}

fn group_status(checks: &[DoctorCheck]) -> DoctorStatus {
    checks
        .iter()
        .map(|c| c.status)
        .max_by_key(|s| s.severity_rank())
        .unwrap_or(DoctorStatus::Info)
}

fn render_text_report(report: &DoctorReport) -> String {
    let mut out = String::new();
    out.push_str("Kaku Doctor\n");
    out.push_str(&format!(
        "Status: {} {}\n",
        report.overall_status.icon(),
        report.overall_status.label()
    ));
    out.push_str(&format!(
        "Summary: {} ok  {} warn  {} fail  {} info\n",
        report.summary.ok, report.summary.warn, report.summary.fail, report.summary.info
    ));
    out.push('\n');

    for (group_idx, group) in report.groups.iter().enumerate() {
        out.push_str(&format!(
            "{}. {} [{}]\n",
            group_idx + 1,
            group.title,
            group.status.label()
        ));
        for check in &group.checks {
            out.push_str(&format!(
                "  - {} {}: {}\n",
                check.status.icon(),
                check.title,
                check.summary
            ));
            for detail in &check.details {
                out.push_str(&format!("    - {}\n", detail));
            }
            if let Some(fix) = &check.fix {
                out.push_str(&format!("    Fix: {}\n", fix));
            }
        }
        out.push('\n');
    }

    out
}

#[derive(Default)]
struct ZshrcSourceCheck {
    guarded_active_lines: usize,
    unguarded_active_lines: usize,
    malformed_escaped_path_lines: usize,
    read_error: Option<String>,
    missing_file: bool,
    commented_example: bool,
}

impl ZshrcSourceCheck {
    fn has_active_lines(&self) -> bool {
        self.guarded_active_lines + self.unguarded_active_lines > 0
    }

    fn has_legacy_guarded_lines(&self) -> bool {
        self.guarded_active_lines > 0
    }

    fn details(&self, zshrc: &Path) -> Vec<String> {
        let mut details = Vec::new();
        details.push(format!("Checked {}", zshrc.display()));
        if let Some(err) = &self.read_error {
            details.push(format!("Read error: {}", err));
            return details;
        }
        if self.missing_file {
            details.push("zshrc does not exist yet".to_string());
        }
        if !self.has_active_lines() {
            details.push(
                "Expected an active line that sources ~/.config/kaku/zsh/kaku.zsh".to_string(),
            );
        } else {
            details.push(format!(
                "Active source lines: {} total",
                self.guarded_active_lines + self.unguarded_active_lines
            ));
        }
        if self.has_legacy_guarded_lines() {
            details.push(
                "Found older Kaku-specific guarded source line variants; `kaku init --update-only` will normalize them."
                    .to_string(),
            );
        }
        if self.malformed_escaped_path_lines > 0 {
            details.push(format!(
                "Found {} malformed Kaku source line(s) with escaped absolute paths (for example, \"\\/Users/...\"), which prevents loading kaku.zsh",
                self.malformed_escaped_path_lines
            ));
        }
        if self.commented_example {
            details.push("Found a commented Kaku source line".to_string());
        }
        details
    }
}

fn check_zshrc_source_line(zshrc: &Path) -> ZshrcSourceCheck {
    let mut result = ZshrcSourceCheck::default();
    let content = match fs::read_to_string(zshrc) {
        Ok(content) => content,
        Err(err) => {
            if err.kind() == ErrorKind::NotFound {
                result.missing_file = true;
            } else {
                result.read_error = Some(err.to_string());
            }
            return result;
        }
    };

    // This is intentionally heuristic instead of a strict parser so it can
    // recognize both the current managed source line and older variants that
    // users may have edited manually.
    //
    // Scan all lines instead of stopping at the first match: mixed managed and
    // legacy lines can coexist during migration, and doctor should report
    // the remaining risk until legacy guarded variants are removed.
    for line in content.lines() {
        let trimmed = line.trim_start();
        if trimmed.starts_with('#') && trimmed.contains("kaku/zsh/kaku.zsh") {
            result.commented_example = true;
            continue;
        }
        if is_malformed_escaped_kaku_source_line(trimmed) {
            result.malformed_escaped_path_lines += 1;
            continue;
        }
        if is_active_kaku_source_line(trimmed) {
            if is_legacy_guarded_kaku_source_line(trimmed) {
                result.guarded_active_lines += 1;
            } else {
                result.unguarded_active_lines += 1;
            }
        }
    }
    result
}

#[derive(Default)]
struct FishConfSourceCheck {
    has_valid_source: bool,
    read_error: Option<String>,
    missing_file: bool,
}

fn check_fish_conf_d_source_line(path: &Path) -> FishConfSourceCheck {
    let mut result = FishConfSourceCheck::default();
    let mut has_kaku_init_var = false;
    let mut sources_kaku_init_var = false;
    let content = match fs::read_to_string(path) {
        Ok(c) => c,
        Err(err) => {
            if err.kind() == ErrorKind::NotFound {
                result.missing_file = true;
            } else {
                result.read_error = Some(err.to_string());
            }
            return result;
        }
    };

    for line in content.lines() {
        let trimmed = line.trim_start();
        if trimmed.starts_with('#') {
            continue;
        }

        if trimmed.contains("kaku/fish/kaku.fish") && contains_source_command(trimmed) {
            result.has_valid_source = true;
            break;
        }

        if trimmed.contains("_kaku_fish_init") && trimmed.contains("kaku/fish/kaku.fish") {
            has_kaku_init_var = true;
        }

        if contains_source_command(trimmed) && trimmed.contains("_kaku_fish_init") {
            sources_kaku_init_var = true;
        }
    }

    if !result.has_valid_source && has_kaku_init_var && sources_kaku_init_var {
        result.has_valid_source = true;
    }

    result
}

fn contains_source_command(line: &str) -> bool {
    line.split(|c: char| c.is_whitespace() || matches!(c, ';' | '&' | '|' | '(' | ')'))
        .any(|token| token == "source")
}

fn is_active_kaku_source_line(trimmed_line: &str) -> bool {
    if trimmed_line.starts_with('#') || !trimmed_line.contains("kaku/zsh/kaku.zsh") {
        return false;
    }
    contains_source_command(trimmed_line)
}

fn is_legacy_guarded_kaku_source_line(trimmed_line: &str) -> bool {
    if !is_active_kaku_source_line(trimmed_line) {
        return false;
    }

    let compact = trimmed_line
        .chars()
        .filter(|c| !c.is_whitespace())
        .flat_map(|c| c.to_lowercase())
        .collect::<String>();

    let has_kaku_comparison = compact.contains("==\"kaku\"")
        || compact.contains("=='kaku'")
        || compact.contains("==kaku")
        || compact.contains("=\"kaku\"")
        || compact.contains("='kaku'")
        || compact.contains("=kaku");
    let has_term_guard =
        (compact.contains("${term") || compact.contains("$term")) && has_kaku_comparison;
    let has_term_program_guard = compact.contains("term_program") && has_kaku_comparison;

    compact.contains("wezterm_pane") || has_term_program_guard || has_term_guard
}

fn is_malformed_escaped_kaku_source_line(trimmed_line: &str) -> bool {
    if trimmed_line.starts_with('#') || !trimmed_line.contains("kaku/zsh/kaku.zsh") {
        return false;
    }
    if !contains_source_command(trimmed_line) {
        return false;
    }
    trimmed_line.contains("\"\\/") || trimmed_line.contains("'\\/")
}

fn probe_wrapper(wrapper: &Path) -> DoctorCheck {
    fn wrapper_check(
        status: DoctorStatus,
        summary: String,
        details: Vec<String>,
        fix: Option<String>,
    ) -> DoctorCheck {
        DoctorCheck {
            title: "Wrapper Execution Probe",
            status,
            summary,
            details,
            fix,
        }
    }

    if !wrapper.is_file() {
        return wrapper_check(
            DoctorStatus::Fail,
            format!(
                "Skipped probe because wrapper is missing: {}",
                wrapper.display()
            ),
            vec!["Generate the wrapper first with `kaku init`".to_string()],
            Some("Run `kaku init --update-only`".to_string()),
        );
    }

    // Spawn the child and poll with try_wait so we can kill it cleanly on timeout.
    // Using spawn()+try_wait() avoids the thread-leak that Command::output() in a
    // background thread would cause if the child never exits.
    let mut child = match Command::new(wrapper)
        .arg("--version")
        .stdout(std::process::Stdio::piped())
        .stderr(std::process::Stdio::piped())
        .spawn()
    {
        Ok(c) => c,
        Err(err) => {
            return wrapper_check(
                DoctorStatus::Fail,
                format!("Failed to execute wrapper: {}", err),
                vec![format!("Command: {} --version", wrapper.display())],
                Some("Restore wrapper permissions or rerun `kaku init --update-only`".to_string()),
            );
        }
    };

    let deadline = Instant::now() + Duration::from_secs(5);
    let output = loop {
        match child.try_wait() {
            Ok(Some(_)) => break child.wait_with_output(),
            Ok(None) if Instant::now() < deadline => {
                std::thread::sleep(Duration::from_millis(50));
            }
            Ok(None) => {
                let _ = child.kill();
                let _ = child.wait();
                return wrapper_check(
                    DoctorStatus::Warn,
                    "Wrapper probe timed out after 5 seconds".to_string(),
                    vec![
                        format!("Command: {} --version", wrapper.display()),
                        "The wrapper script did not exit within the time limit.".to_string(),
                    ],
                    Some(
                        "Check that the kaku binary is accessible and not blocked by network or permission issues".to_string(),
                    ),
                );
            }
            Err(err) => break Err(err),
        }
    };

    match output {
        Ok(output) if output.status.success() => {
            let stdout = String::from_utf8_lossy(&output.stdout).trim().to_string();
            wrapper_check(
                DoctorStatus::Ok,
                "Wrapper can launch Kaku binary".to_string(),
                if stdout.is_empty() {
                    vec![format!(
                        "Command succeeded: {} --version",
                        wrapper.display()
                    )]
                } else {
                    vec![
                        format!("Command succeeded: {} --version", wrapper.display()),
                        format!("Output: {}", stdout),
                    ]
                },
                None,
            )
        }
        Ok(output) => {
            let stderr = String::from_utf8_lossy(&output.stderr).trim().to_string();
            let mut details = vec![format!("Command: {} --version", wrapper.display())];
            if !stderr.is_empty() {
                details.push(format!("stderr: {}", stderr));
            }
            wrapper_check(
                DoctorStatus::Fail,
                format!("Wrapper exited with status {}", output.status),
                details,
                Some("Check Kaku.app location then run `kaku init --update-only`".to_string()),
            )
        }
        Err(err) => wrapper_check(
            DoctorStatus::Fail,
            format!("Failed to execute wrapper: {}", err),
            vec![format!("Command: {} --version", wrapper.display())],
            Some("Restore wrapper permissions or rerun `kaku init --update-only`".to_string()),
        ),
    }
}

fn probe_login_shell_integration(shell_kind: &ShellKind) -> DoctorCheck {
    let (title, detail_exec, detail_verify) = match shell_kind {
        ShellKind::Fish => (
            "Login Fish Integration Probe",
            "Doctor does not execute `fish -l` because interactive login startup files can run user-defined commands, plugin managers, and network actions.",
            "Start a new fish session and verify `echo $PATH` includes ~/.config/kaku/fish/bin if you need end-to-end runtime validation.",
        ),
        ShellKind::Zsh => (
            "Login Zsh Integration Probe",
            "Doctor does not execute `/bin/zsh -lic` because interactive login startup files can run user-defined commands, plugin managers, and network actions.",
            "Use `exec zsh -l` manually and verify `echo $KAKU_ZSH_DIR` if you need end-to-end runtime validation.",
        ),
        _ => (
            "Login Shell Integration Probe",
            "Doctor skips login shell probe for shells not managed by Kaku.",
            "Kaku shell integration supports zsh and fish. Other shells are not managed.",
        ),
    };

    DoctorCheck {
        title,
        status: DoctorStatus::Info,
        summary: format!(
            "Skipped interactive login {} probe to avoid shell side effects",
            shell_kind.name()
        ),
        details: vec![detail_exec.to_string(), detail_verify.to_string()],
        fix: None,
    }
}

fn home_dir() -> PathBuf {
    config::HOME_DIR.clone()
}

fn managed_bin_dir(shell_kind: &ShellKind) -> PathBuf {
    let shell_dir = if *shell_kind == ShellKind::Fish {
        "fish"
    } else {
        "zsh"
    };
    home_dir()
        .join(".config")
        .join("kaku")
        .join(shell_dir)
        .join("bin")
}

fn managed_wrapper_path(shell_kind: &ShellKind) -> PathBuf {
    managed_bin_dir(shell_kind).join("kaku")
}

fn managed_init_file(shell_kind: &ShellKind) -> PathBuf {
    if *shell_kind == ShellKind::Fish {
        home_dir()
            .join(".config")
            .join("kaku")
            .join("fish")
            .join("kaku.fish")
    } else {
        home_dir()
            .join(".config")
            .join("kaku")
            .join("zsh")
            .join("kaku.zsh")
    }
}

fn zshrc_path() -> PathBuf {
    if let Some(zdotdir) = std::env::var_os("ZDOTDIR") {
        PathBuf::from(zdotdir).join(".zshrc")
    } else {
        home_dir().join(".zshrc")
    }
}

fn kaku_bin_candidates() -> Vec<PathBuf> {
    let mut candidates = Vec::new();

    if let Some(path) = std::env::var_os("KAKU_BIN") {
        candidates.push(PathBuf::from(path));
    }

    if let Ok(exe) = std::env::current_exe() {
        if exe
            .file_name()
            .and_then(|n| n.to_str())
            .map(|n| n.eq_ignore_ascii_case("kaku"))
            .unwrap_or(false)
        {
            candidates.push(exe);
        }
    }

    candidates.push(PathBuf::from("/Applications/Kaku.app/Contents/MacOS/kaku"));
    candidates.push(
        home_dir()
            .join("Applications")
            .join("Kaku.app")
            .join("Contents")
            .join("MacOS")
            .join("kaku"),
    );

    candidates
}

fn doctor_version_string() -> String {
    let version = config::wezterm_version();
    if version == "someone forgot to call assign_version_info" {
        env!("CARGO_PKG_VERSION").to_string()
    } else {
        version.to_string()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::fs;

    #[test]
    fn missing_zshrc_is_not_read_error() {
        let dir = tempfile::tempdir().expect("create tempdir");
        let path = dir.path().join(".zshrc");

        let check = check_zshrc_source_line(&path);
        assert!(check.missing_file);
        assert!(check.read_error.is_none());
        assert!(!check.has_active_lines());
    }

    #[test]
    fn variable_assignment_is_not_detected_as_source_line() {
        let dir = tempfile::tempdir().expect("create tempdir");
        let path = dir.path().join(".zshrc");
        fs::write(
            &path,
            r#"export KAKU_ZSH_INIT="$HOME/.config/kaku/zsh/kaku.zsh""#,
        )
        .expect("write zshrc");

        let check = check_zshrc_source_line(&path);
        assert_eq!(check.guarded_active_lines, 0);
        assert_eq!(check.unguarded_active_lines, 0);
    }

    #[test]
    fn counts_guarded_and_unguarded_source_lines() {
        let dir = tempfile::tempdir().expect("create tempdir");
        let path = dir.path().join(".zshrc");
        fs::write(
            &path,
            r#"[[ "${TERM:-}" == "kaku" ]] && source "$HOME/.config/kaku/zsh/kaku.zsh"
[[ -f "$HOME/.config/kaku/zsh/kaku.zsh" ]] && source "$HOME/.config/kaku/zsh/kaku.zsh"
[[ -f "$HOME/.config/kaku/zsh/kaku.zsh" ]] && source "$HOME/.config/kaku/zsh/kaku.zsh"
"#,
        )
        .expect("write zshrc");

        let check = check_zshrc_source_line(&path);
        assert!(!check.commented_example);
        assert_eq!(check.unguarded_active_lines, 2);
        assert_eq!(check.guarded_active_lines, 1);
        assert!(check.has_legacy_guarded_lines());
        assert!(check.has_active_lines());
    }

    #[test]
    fn term_program_guard_is_detected_as_legacy_source_line() {
        let dir = tempfile::tempdir().expect("create tempdir");
        let path = dir.path().join(".zshrc");
        fs::write(
            &path,
            r#"[[ "$TERM_PROGRAM" == "Kaku" ]] && [[ -f "$HOME/.config/kaku/zsh/kaku.zsh" ]] && source "$HOME/.config/kaku/zsh/kaku.zsh" # Kaku Shell Integration
"#,
        )
        .expect("write zshrc");

        let check = check_zshrc_source_line(&path);
        assert_eq!(check.guarded_active_lines, 1);
        assert_eq!(check.unguarded_active_lines, 0);
        assert!(check.has_legacy_guarded_lines());
        assert!(check.has_active_lines());
    }

    #[test]
    fn escaped_absolute_path_source_line_is_marked_malformed() {
        let dir = tempfile::tempdir().expect("create tempdir");
        let path = dir.path().join(".zshrc");
        fs::write(
            &path,
            r#"[[ -f "\/Users/lex/.config/kaku/zsh/kaku.zsh" ]] && source "\/Users/lex/.config/kaku/zsh/kaku.zsh""#,
        )
        .expect("write zshrc");

        let check = check_zshrc_source_line(&path);
        assert_eq!(check.guarded_active_lines, 0);
        assert_eq!(check.unguarded_active_lines, 0);
        assert_eq!(check.malformed_escaped_path_lines, 1);
        assert!(!check.has_active_lines());
    }

    #[test]
    fn login_probe_is_passive_and_non_executing() {
        let check = probe_login_shell_integration(&ShellKind::Zsh);
        assert_eq!(check.status, DoctorStatus::Info);
        assert!(check.summary.contains("Skipped interactive login"));
        assert!(check.fix.is_none());
    }

    #[test]
    fn explicit_shell_is_preserved_in_auto_fix_command() {
        assert_eq!(
            init_update_command(Some(ManagedShell::Fish)),
            "kaku init --update-only --shell fish"
        );
        assert_eq!(init_update_command(None), "kaku init --update-only");
    }

    #[test]
    fn fish_conf_d_missing_is_not_valid_source() {
        let dir = tempfile::tempdir().expect("create tempdir");
        let path = dir.path().join("kaku.fish");
        let check = check_fish_conf_d_source_line(&path);
        assert!(check.missing_file);
        assert!(!check.has_valid_source);
        assert!(check.read_error.is_none());
    }

    #[test]
    fn fish_conf_d_empty_file_is_not_valid_source() {
        let dir = tempfile::tempdir().expect("create tempdir");
        let path = dir.path().join("kaku.fish");
        fs::write(&path, "").expect("write");
        let check = check_fish_conf_d_source_line(&path);
        assert!(!check.missing_file);
        assert!(!check.has_valid_source);
    }

    #[test]
    fn fish_conf_d_without_kaku_source_is_not_valid() {
        let dir = tempfile::tempdir().expect("create tempdir");
        let path = dir.path().join("kaku.fish");
        fs::write(&path, "set -x FISH_VAR value\n").expect("write");
        let check = check_fish_conf_d_source_line(&path);
        assert!(!check.has_valid_source);
    }

    #[test]
    fn fish_conf_d_with_valid_source_is_detected() {
        let dir = tempfile::tempdir().expect("create tempdir");
        let path = dir.path().join("kaku.fish");
        fs::write(&path, "source ~/.config/kaku/fish/kaku.fish\n").expect("write");
        let check = check_fish_conf_d_source_line(&path);
        assert!(check.has_valid_source);
        assert!(!check.missing_file);
        assert!(check.read_error.is_none());
    }

    #[test]
    fn fish_conf_d_commented_source_is_not_valid() {
        let dir = tempfile::tempdir().expect("create tempdir");
        let path = dir.path().join("kaku.fish");
        fs::write(&path, "# source ~/.config/kaku/fish/kaku.fish\n").expect("write");
        let check = check_fish_conf_d_source_line(&path);
        assert!(!check.has_valid_source);
    }

    #[test]
    fn fish_conf_d_with_managed_variable_source_is_detected() {
        let dir = tempfile::tempdir().expect("create tempdir");
        let path = dir.path().join("kaku.fish");
        fs::write(
            &path,
            r#"# Kaku shell integration -- managed. Remove with: kaku reset
set -l _kaku_fish_init "$HOME/.config/kaku/fish/kaku.fish"
if test -f $_kaku_fish_init
    source $_kaku_fish_init
end
"#,
        )
        .expect("write");
        let check = check_fish_conf_d_source_line(&path);
        assert!(check.has_valid_source);
        assert!(!check.missing_file);
        assert!(check.read_error.is_none());
    }

    #[test]
    fn shell_kind_unsupported_is_not_managed() {
        let kind = ShellKind::Unsupported("bash".to_string());
        assert!(!kind.is_managed());
    }

    #[test]
    fn shell_kind_zsh_and_fish_are_managed() {
        assert!(ShellKind::Zsh.is_managed());
        assert!(ShellKind::Fish.is_managed());
    }

    #[test]
    fn autosuggest_compatibility_requires_provider_marker() {
        let content = r#"
typeset -g _kaku_autosuggest_cli_provider=""
typeset -g _kaku_external_autosuggest_provider=0
"#;

        assert!(!zsh_init_defers_autosuggestions_to_provider(
            content, "kiro-cli"
        ));
    }

    #[test]
    fn autosuggest_compatibility_rejects_bundled_source_line() {
        let content = r#"
typeset -g _kaku_autosuggest_cli_provider="kiro-cli"
source "$KAKU_ZSH_DIR/plugins/zsh-autosuggestions/zsh-autosuggestions.zsh"
"#;

        assert!(zsh_init_has_autosuggest_provider_marker(
            content, "kiro-cli"
        ));
        assert!(zsh_init_loads_bundled_autosuggestions(content));
        assert!(!zsh_init_defers_autosuggestions_to_provider(
            content, "kiro-cli"
        ));
    }

    #[test]
    fn autosuggest_compatibility_accepts_provider_marker_without_bundled_source() {
        let content = r#"
typeset -g _kaku_autosuggest_cli_provider="kiro-cli"
typeset -g _kaku_external_autosuggest_provider=0
"#;

        assert!(zsh_init_defers_autosuggestions_to_provider(
            content, "kiro-cli"
        ));
    }
}
