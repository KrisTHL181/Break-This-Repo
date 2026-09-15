use anyhow::{anyhow, bail, Context};
use clap::Parser;
use std::fs;
use std::io::{self, IsTerminal, Write};
use std::path::{Path, PathBuf};
use std::process::Command;

#[derive(Debug, Parser, Clone, Default)]
pub struct InitCommand {
    /// Refresh shell integration without interactive prompts
    #[arg(long)]
    pub update_only: bool,

    /// Shell integration to configure
    #[arg(long, value_enum)]
    pub shell: Option<crate::shell::ManagedShell>,
}

impl InitCommand {
    pub fn run(&self) -> anyhow::Result<()> {
        imp::run(self.update_only, self.shell)
    }
}

#[cfg(not(target_os = "macos"))]
mod imp {
    use anyhow::bail;

    pub fn run(
        _update_only: bool,
        _shell: Option<crate::shell::ManagedShell>,
    ) -> anyhow::Result<()> {
        bail!("`kaku init` is currently supported on macOS only")
    }
}

#[cfg(target_os = "macos")]
mod imp {
    use super::*;
    use crate::shell::{
        find_shell_executable, persist_initialized_state, persist_managed_shell,
        preferred_managed_shell, ManagedShell,
    };
    use std::os::unix::fs::PermissionsExt;

    pub fn run(update_only: bool, shell: Option<ManagedShell>) -> anyhow::Result<()> {
        let shell = select_shell(update_only, shell)?;
        if find_shell_executable(shell).is_none() {
            bail!(
                "cannot configure {name}: no `{name}` executable found on PATH or in \
                 standard locations. Install it, or pick the other shell with \
                 `kaku init --shell <shell>`.",
                name = shell.name()
            );
        }
        // Record the selection before running setup so a partially failed run
        // retries against the same shell instead of re-detecting from $SHELL.
        persist_managed_shell(shell).context("remember selected shell")?;
        ensure_user_config().context("ensure user config exists")?;

        install_kaku_wrapper(shell).context("install kaku wrapper")?;
        install_k_wrapper(shell).context("install k wrapper")?;

        let script_name = match shell {
            ManagedShell::Fish => "setup_fish.sh",
            ManagedShell::Zsh => "setup_zsh.sh",
        };
        let script = resolve_setup_script(script_name)
            .ok_or_else(|| anyhow!("failed to locate {} for Kaku initialization", script_name))?;

        let mut cmd = Command::new("/bin/bash");
        cmd.arg(&script)
            .env("KAKU_INIT_INTERNAL", "1")
            .env("KAKU_TARGET_SHELL", shell.name());
        if update_only {
            cmd.arg("--update-only");
        }
        let status = cmd
            .status()
            .with_context(|| format!("run {}", script.display()))?;

        if status.success() {
            let config_version = read_setup_config_version(&script)?;
            persist_initialized_state(shell, config_version)
                .context("record completed shell initialization")?;
            return Ok(());
        }

        bail!("kaku init failed with status {}", status);
    }

    fn select_shell(
        update_only: bool,
        selected: Option<ManagedShell>,
    ) -> anyhow::Result<ManagedShell> {
        if let Some(shell) = selected {
            return Ok(shell);
        }

        let default = preferred_managed_shell();
        if update_only || !io::stdin().is_terminal() || !io::stdout().is_terminal() {
            return Ok(default);
        }

        let Some(zsh_path) = find_shell_executable(ManagedShell::Zsh) else {
            return Ok(default);
        };
        let Some(fish_path) = find_shell_executable(ManagedShell::Fish) else {
            return Ok(default);
        };

        prompt_for_shell(default, &zsh_path, &fish_path)
    }

    fn prompt_for_shell(
        default: ManagedShell,
        zsh_path: &Path,
        fish_path: &Path,
    ) -> anyhow::Result<ManagedShell> {
        println!("Which shell should Kaku configure?");
        println!(
            "  1) zsh  ({}){}",
            zsh_path.display(),
            shell_default_label(default, ManagedShell::Zsh)
        );
        println!(
            "  2) fish ({}){}",
            fish_path.display(),
            shell_default_label(default, ManagedShell::Fish)
        );

        loop {
            print!("Select [{}]: ", shell_number(default));
            io::stdout().flush().context("flush shell prompt")?;

            let mut input = String::new();
            let bytes = io::stdin()
                .read_line(&mut input)
                .context("read shell selection")?;
            if bytes == 0 {
                return Ok(default);
            }
            if let Some(shell) = parse_shell_selection(&input, default) {
                return Ok(shell);
            }
            eprintln!("Choose 1 for zsh or 2 for fish.");
        }
    }

    fn shell_default_label(default: ManagedShell, shell: ManagedShell) -> &'static str {
        if default == shell {
            " (detected login shell)"
        } else {
            ""
        }
    }

    fn shell_number(shell: ManagedShell) -> u8 {
        match shell {
            ManagedShell::Zsh => 1,
            ManagedShell::Fish => 2,
        }
    }

    fn parse_shell_selection(input: &str, default: ManagedShell) -> Option<ManagedShell> {
        match input.trim().to_ascii_lowercase().as_str() {
            "" => Some(default),
            "1" | "zsh" => Some(ManagedShell::Zsh),
            "2" | "fish" => Some(ManagedShell::Fish),
            _ => None,
        }
    }

    fn install_kaku_wrapper(shell: ManagedShell) -> anyhow::Result<()> {
        let wrapper_path = wrapper_path(shell);
        let wrapper_dir = wrapper_path
            .parent()
            .ok_or_else(|| anyhow!("invalid wrapper path"))?;
        config::create_user_owned_dirs(wrapper_dir).context("create wrapper directory")?;

        if fs::symlink_metadata(&wrapper_path)
            .map(|m| m.file_type().is_symlink())
            .unwrap_or(false)
        {
            fs::remove_file(&wrapper_path).with_context(|| {
                format!("remove legacy symlink wrapper {}", wrapper_path.display())
            })?;
        }

        let preferred_bin = resolve_preferred_kaku_bin()
            .unwrap_or_else(|| PathBuf::from("/Applications/Kaku.app/Contents/MacOS/kaku"));
        let preferred_bin = escape_for_double_quotes(&preferred_bin.display().to_string());

        let script = format!(
            r#"#!/bin/bash
set -euo pipefail

if [[ -n "${{KAKU_BIN:-}}" && -x "${{KAKU_BIN}}" ]]; then
	exec "${{KAKU_BIN}}" "$@"
fi

for candidate in \
	"{preferred_bin}" \
	"/Applications/Kaku.app/Contents/MacOS/kaku" \
	"$HOME/Applications/Kaku.app/Contents/MacOS/kaku"; do
	if [[ -n "$candidate" && -x "$candidate" ]]; then
		exec "$candidate" "$@"
	fi
done

echo "kaku: Kaku.app not found. Expected /Applications/Kaku.app." >&2
exit 127
"#
        );

        let mut file = fs::File::create(&wrapper_path)
            .with_context(|| format!("create wrapper {}", wrapper_path.display()))?;
        file.write_all(script.as_bytes())
            .with_context(|| format!("write wrapper {}", wrapper_path.display()))?;
        fs::set_permissions(&wrapper_path, fs::Permissions::from_mode(0o755))
            .with_context(|| format!("chmod wrapper {}", wrapper_path.display()))?;
        Ok(())
    }

    fn install_k_wrapper(shell: ManagedShell) -> anyhow::Result<()> {
        let k_path = k_wrapper_path(shell);
        let k_dir = k_path
            .parent()
            .ok_or_else(|| anyhow!("invalid k wrapper path"))?;
        config::create_user_owned_dirs(k_dir).context("create k wrapper directory")?;

        // If something else already owns this path and it is not our wrapper, skip.
        if k_path.exists() {
            let content = fs::read_to_string(&k_path).unwrap_or_default();
            if !content.contains("Kaku") && !content.contains("kaku") {
                eprintln!(
                    "k: {} already exists and does not appear to be a Kaku wrapper; skipping.",
                    k_path.display()
                );
                return Ok(());
            }
        }
        if fs::symlink_metadata(&k_path)
            .map(|m| m.file_type().is_symlink())
            .unwrap_or(false)
        {
            fs::remove_file(&k_path)
                .with_context(|| format!("remove legacy symlink k wrapper {}", k_path.display()))?;
        }

        let preferred_k_bin = resolve_preferred_k_bin()
            .unwrap_or_else(|| PathBuf::from("/Applications/Kaku.app/Contents/MacOS/k"));
        let preferred_k_bin = escape_for_double_quotes(&preferred_k_bin.display().to_string());

        let script = format!(
            r#"#!/bin/bash
set -euo pipefail

for candidate in \
	"{preferred_k_bin}" \
	"/Applications/Kaku.app/Contents/MacOS/k" \
	"$HOME/Applications/Kaku.app/Contents/MacOS/k"; do
	if [[ -n "$candidate" && -x "$candidate" ]]; then
		exec "$candidate" "$@"
	fi
done

echo "k: Kaku.app not found. Run 'kaku init' after installing Kaku." >&2
exit 127
"#
        );

        let mut file = fs::File::create(&k_path)
            .with_context(|| format!("create k wrapper {}", k_path.display()))?;
        file.write_all(script.as_bytes())
            .with_context(|| format!("write k wrapper {}", k_path.display()))?;
        fs::set_permissions(&k_path, fs::Permissions::from_mode(0o755))
            .with_context(|| format!("chmod k wrapper {}", k_path.display()))?;
        Ok(())
    }

    fn k_wrapper_path(shell: ManagedShell) -> PathBuf {
        let dir = match shell {
            ManagedShell::Fish => "fish",
            ManagedShell::Zsh => "zsh",
        };
        config::HOME_DIR
            .join(".config")
            .join("kaku")
            .join(dir)
            .join("bin")
            .join("k")
    }

    fn resolve_preferred_k_bin() -> Option<PathBuf> {
        if let Ok(exe) = std::env::current_exe() {
            // current_exe is the `kaku` binary; `k` sits alongside it.
            let k_candidate = exe.with_file_name("k");
            if is_executable_file(&k_candidate) {
                return Some(k_candidate);
            }
        }
        for candidate in [
            PathBuf::from("/Applications/Kaku.app/Contents/MacOS/k"),
            config::HOME_DIR
                .join("Applications")
                .join("Kaku.app")
                .join("Contents")
                .join("MacOS")
                .join("k"),
        ] {
            if is_executable_file(&candidate) {
                return Some(candidate);
            }
        }
        None
    }

    fn wrapper_path(shell: ManagedShell) -> PathBuf {
        let dir = match shell {
            ManagedShell::Fish => "fish",
            ManagedShell::Zsh => "zsh",
        };
        config::HOME_DIR
            .join(".config")
            .join("kaku")
            .join(dir)
            .join("bin")
            .join("kaku")
    }

    fn resolve_preferred_kaku_bin() -> Option<PathBuf> {
        if let Some(path) = std::env::var_os("KAKU_BIN") {
            let path = PathBuf::from(path);
            if is_executable_file(&path) {
                return Some(path);
            }
        }

        if let Ok(exe) = std::env::current_exe() {
            if exe
                .file_name()
                .and_then(|n| n.to_str())
                .map(|n| n.eq_ignore_ascii_case("kaku"))
                .unwrap_or(false)
                && is_executable_file(&exe)
            {
                return Some(exe);
            }
        }

        for candidate in [
            PathBuf::from("/Applications/Kaku.app/Contents/MacOS/kaku"),
            config::HOME_DIR
                .join("Applications")
                .join("Kaku.app")
                .join("Contents")
                .join("MacOS")
                .join("kaku"),
        ] {
            if is_executable_file(&candidate) {
                return Some(candidate);
            }
        }

        None
    }

    fn is_executable_file(path: &Path) -> bool {
        fs::metadata(path)
            .map(|meta| meta.is_file() && (meta.permissions().mode() & 0o111 != 0))
            .unwrap_or(false)
    }

    fn escape_for_double_quotes(value: &str) -> String {
        value
            .replace('\\', "\\\\")
            .replace('"', "\\\"")
            .replace('$', "\\$")
            .replace('`', "\\`")
    }

    fn resolve_setup_script(script_name: &str) -> Option<PathBuf> {
        let mut candidates = Vec::new();

        if let Ok(cwd) = std::env::current_dir() {
            candidates.push(
                cwd.join("assets")
                    .join("shell-integration")
                    .join(script_name),
            );
        }

        if let Ok(exe) = std::env::current_exe() {
            if let Some(contents_dir) = exe.parent().and_then(|p| p.parent()) {
                candidates.push(contents_dir.join("Resources").join(script_name));
            }
        }

        candidates.push(PathBuf::from(format!(
            "/Applications/Kaku.app/Contents/Resources/{}",
            script_name
        )));
        candidates.push(
            config::HOME_DIR
                .join("Applications")
                .join("Kaku.app")
                .join("Contents")
                .join("Resources")
                .join(script_name),
        );

        candidates.into_iter().find(|p| p.exists())
    }

    fn read_setup_config_version(setup_script: &Path) -> anyhow::Result<u64> {
        let version_file = setup_script
            .parent()
            .context("setup script has no resource directory")?
            .join("config_version.txt");
        let raw = fs::read_to_string(&version_file)
            .with_context(|| format!("read {}", version_file.display()))?;
        raw.trim()
            .parse::<u64>()
            .with_context(|| format!("parse bundled config version in {}", version_file.display()))
    }

    fn ensure_user_config() -> anyhow::Result<()> {
        config::ensure_user_config_exists().context("ensure user config exists")?;
        Ok(())
    }

    #[cfg(test)]
    mod tests {
        use super::*;

        #[test]
        fn shell_prompt_accepts_numbers_names_and_default() {
            assert_eq!(
                parse_shell_selection("", ManagedShell::Fish),
                Some(ManagedShell::Fish)
            );
            assert_eq!(
                parse_shell_selection("1", ManagedShell::Fish),
                Some(ManagedShell::Zsh)
            );
            assert_eq!(
                parse_shell_selection("fish", ManagedShell::Zsh),
                Some(ManagedShell::Fish)
            );
            assert_eq!(parse_shell_selection("bash", ManagedShell::Zsh), None);
        }

        #[test]
        fn setup_version_comes_from_the_setup_resource_directory() {
            let root = tempfile::tempdir().unwrap();
            let script = root.path().join("setup_zsh.sh");
            fs::write(&script, "#!/bin/bash\n").unwrap();
            fs::write(root.path().join("config_version.txt"), "31\n").unwrap();

            assert_eq!(read_setup_config_version(&script).unwrap(), 31);
        }
    }
}
