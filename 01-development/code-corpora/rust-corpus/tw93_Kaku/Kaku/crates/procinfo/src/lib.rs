#![allow(clippy::all)]

use std::collections::{HashMap, HashSet};
use std::path::PathBuf;

#[cfg(feature = "lua")]
use wezterm_dynamic::{FromDynamic, ToDynamic};

mod macos;

#[derive(Debug, Copy, Clone)]
#[cfg_attr(feature = "lua", derive(FromDynamic, ToDynamic))]
pub enum LocalProcessStatus {
    Idle,
    Run,
    Sleep,
    Stop,
    Zombie,
    Tracing,
    Dead,
    Wakekill,
    Waking,
    Parked,
    LockBlocked,
    Unknown,
}

#[derive(Debug, Clone)]
#[cfg_attr(feature = "lua", derive(FromDynamic, ToDynamic))]
pub struct LocalProcessInfo {
    /// The process identifier
    pub pid: u32,
    /// The parent process identifier
    pub ppid: u32,
    /// The process group identifier. Used to distinguish the tty's
    /// foreground job from backgrounded processes in the same tree.
    pub pgid: u32,
    /// The COMM name of the process. May not bear any relation to
    /// the executable image name. May be changed at runtime by
    /// the process.
    /// Many systems truncate this
    /// field to 15-16 characters.
    pub name: String,
    /// Path to the executable image
    pub executable: PathBuf,
    /// The argument vector.
    /// Some systems allow changing the argv block at runtime
    /// eg: setproctitle().
    pub argv: Vec<String>,
    /// The current working directory for the process, or an empty
    /// path if it was not accessible for some reason.
    pub cwd: PathBuf,
    /// The status of the process. Not all possible values are
    /// portably supported on all systems.
    pub status: LocalProcessStatus,
    /// A clock value in unspecified system dependent units that
    /// indicates the relative age of the process.
    pub start_time: u64,
    /// The console handle associated with the process, if any.
    #[cfg(windows)]
    pub console: u64,
    /// Child processes, keyed by pid
    pub children: HashMap<u32, LocalProcessInfo>,
}
#[cfg(feature = "lua")]
luahelper::impl_lua_conversion_dynamic!(LocalProcessInfo);

impl LocalProcessInfo {
    /// Walk this sub-tree of processes and return a unique set
    /// of executable base names. eg: `foo/bar` and `woot/bar`
    /// produce a set containing just `bar`.
    pub fn flatten_to_exe_names(&self) -> HashSet<String> {
        let mut names = HashSet::new();

        fn flatten(item: &LocalProcessInfo, names: &mut HashSet<String>) {
            if let Some(exe) = item.executable.file_name() {
                names.insert(exe.to_string_lossy().into_owned());
            }
            for proc in item.children.values() {
                flatten(proc, names);
            }
        }

        flatten(self, &mut names);
        names
    }

    /// Like `flatten_to_exe_names`, but only includes processes whose process
    /// group id matches `pgid`. Lets callers consider just the tty's foreground
    /// job and ignore backgrounded daemons (e.g. gitstatusd) that live in the
    /// same process tree but a different process group.
    pub fn flatten_to_exe_names_in_group(&self, pgid: u32) -> HashSet<String> {
        let mut names = HashSet::new();

        fn flatten(item: &LocalProcessInfo, pgid: u32, names: &mut HashSet<String>) {
            if item.pgid == pgid {
                if let Some(exe) = item.executable.file_name() {
                    names.insert(exe.to_string_lossy().into_owned());
                }
            }
            for proc in item.children.values() {
                flatten(proc, pgid, names);
            }
        }

        flatten(self, pgid, &mut names);
        names
    }

    #[cfg(not(target_os = "macos"))]
    pub fn with_root_pid(_pid: u32) -> Option<Self> {
        None
    }

    #[cfg(not(target_os = "macos"))]
    pub fn current_working_dir(_pid: u32) -> Option<PathBuf> {
        None
    }

    #[cfg(not(target_os = "macos"))]
    pub fn executable_path(_pid: u32) -> Option<PathBuf> {
        None
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn proc(pid: u32, pgid: u32, exe: &str, children: Vec<LocalProcessInfo>) -> LocalProcessInfo {
        LocalProcessInfo {
            pid,
            ppid: 0,
            pgid,
            name: exe.to_string(),
            executable: PathBuf::from(format!("/usr/bin/{exe}")),
            argv: vec![],
            cwd: PathBuf::new(),
            status: LocalProcessStatus::Run,
            start_time: 0,
            #[cfg(windows)]
            console: 0,
            children: children.into_iter().map(|c| (c.pid, c)).collect(),
        }
    }

    #[test]
    fn group_filter_ignores_background_daemon() {
        // Idle shell in foreground group 100, with a backgrounded gitstatusd
        // daemon in its own group 200 living in the same process tree.
        let tree = proc(100, 100, "zsh", vec![proc(150, 200, "gitstatusd", vec![])]);

        // Whole-tree view sees both processes.
        assert_eq!(
            tree.flatten_to_exe_names(),
            HashSet::from(["zsh".to_string(), "gitstatusd".to_string()])
        );

        // Foreground-group view sees only the shell.
        assert_eq!(
            tree.flatten_to_exe_names_in_group(100),
            HashSet::from(["zsh".to_string()])
        );
    }

    #[test]
    fn group_filter_keeps_foreground_job_children() {
        // A foreground `./build.sh` (bash) that shells out to make stays in the
        // same foreground group and must remain visible.
        let tree = proc(100, 300, "bash", vec![proc(160, 300, "make", vec![])]);

        assert_eq!(
            tree.flatten_to_exe_names_in_group(300),
            HashSet::from(["bash".to_string(), "make".to_string()])
        );
    }
}
