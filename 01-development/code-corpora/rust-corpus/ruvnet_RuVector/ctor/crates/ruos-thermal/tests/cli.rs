//! End-to-end CLI integration tests for `ruos-thermal`.
//!
//! Spawns the real built binary, asserts on output shapes. Mirrors
//! the embed/stats/bench CLI test pattern from
//! `ruvector-hailo-cluster/tests/{embed,stats,bench}_cli.rs`.

use std::process::Command;

const RUOS_THERMAL: &str = env!("CARGO_BIN_EXE_ruos-thermal");

#[test]
fn version_flag_prints_pkg_name_and_version() {
    for arg in &["--version", "-V"] {
        let out = Command::new(RUOS_THERMAL).arg(arg).output().unwrap();
        assert!(out.status.success(), "exited {:?}", out.status);
        let line = String::from_utf8_lossy(&out.stdout).trim().to_string();
        assert!(line.starts_with("ruos-thermal "), "got: {:?}", line);
        let parts: Vec<&str> = line.split_whitespace().collect();
        assert_eq!(parts.len(), 2);
    }
}

#[test]
fn show_profiles_lists_all_five_profiles() {
    let out = Command::new(RUOS_THERMAL).arg("--show-profiles").output().unwrap();
    assert!(out.status.success());
    let stdout = String::from_utf8_lossy(&out.stdout);
    let lines: Vec<&str> = stdout.lines().collect();
    // Header + 5 profile rows.
    assert_eq!(lines.len(), 6, "expected 6 lines, got: {}", stdout);
    assert!(lines[0].starts_with("name\ttarget-mhz"));
    for name in &["eco", "default", "safe-overclock", "aggressive", "max"] {
        assert!(stdout.contains(name), "missing profile {}: {}", name, stdout);
    }
}

#[test]
fn set_profile_without_allow_cpufreq_write_refuses() {
    let out = Command::new(RUOS_THERMAL)
        .args(["--set-profile", "eco"])
        .output()
        .unwrap();
    assert!(!out.status.success(), "expected non-zero exit");
    assert_eq!(out.status.code(), Some(1));
    let stderr = String::from_utf8_lossy(&out.stderr);
    assert!(stderr.contains("--allow-cpufreq-write"), "stderr: {}", stderr);
}

#[test]
fn set_profile_unknown_name_errors_cleanly() {
    let out = Command::new(RUOS_THERMAL)
        .args(["--set-profile", "ludicrous-speed"])
        .output()
        .unwrap();
    assert_eq!(out.status.code(), Some(1));
    let stderr = String::from_utf8_lossy(&out.stderr);
    assert!(stderr.contains("unknown profile"), "stderr: {}", stderr);
}

#[test]
fn json_and_prom_are_mutually_exclusive() {
    let out = Command::new(RUOS_THERMAL)
        .args(["--json", "--prom"])
        .output()
        .unwrap();
    assert_eq!(out.status.code(), Some(1));
    let stderr = String::from_utf8_lossy(&out.stderr);
    assert!(stderr.contains("mutually exclusive"), "stderr: {}", stderr);
}

#[test]
fn unknown_arg_exits_one_with_usage_hint() {
    let out = Command::new(RUOS_THERMAL).arg("--bogus").output().unwrap();
    assert_eq!(out.status.code(), Some(1));
    let stderr = String::from_utf8_lossy(&out.stderr);
    assert!(stderr.contains("unknown arg"), "stderr: {}", stderr);
    assert!(stderr.contains("--help"), "stderr should hint at --help: {}", stderr);
}
