use std::io::Write;

use clap_complete::{generate, Shell};

pub(super) const SUPPORTED_SHELLS: [&str; 5] = ["bash", "elvish", "fish", "powershell", "zsh"];

pub(super) fn supported_shells_usage() -> String {
    SUPPORTED_SHELLS.join("|")
}

pub(super) fn run_completion_command(args: &[String]) -> std::io::Result<i32> {
    let Some(shell) = args.first().map(String::as_str) else {
        print_completion_help();
        return Ok(2);
    };
    if matches!(shell, "help" | "--help" | "-h") {
        print_completion_help();
        return Ok(0);
    }
    if args.len() != 1 {
        print_completion_help();
        return Ok(2);
    }

    let Some(shell) = parse_shell(shell) else {
        eprintln!("unknown shell: {shell}");
        print_completion_help();
        return Ok(2);
    };

    let mut command = super::spec::command();
    if matches!(shell, Shell::Zsh) {
        let mut output = Vec::new();
        generate(shell, &mut command, "herdr", &mut output);
        let script = String::from_utf8(output).map_err(|err| {
            std::io::Error::new(std::io::ErrorKind::InvalidData, err.utf8_error())
        })?;
        crate::platform::begin_cli_output();
        std::io::stdout().write_all(space_separated_zsh_long_options(&script).as_bytes())?;
    } else {
        crate::platform::begin_cli_output();
        generate(shell, &mut command, "herdr", &mut std::io::stdout());
    }
    Ok(0)
}

fn space_separated_zsh_long_options(script: &str) -> String {
    let mut output = String::with_capacity(script.len());
    let mut rest = script;

    while let Some(start) = rest.find("--") {
        output.push_str(&rest[..start]);
        let candidate = &rest[start + 2..];
        let name_len = candidate
            .chars()
            .take_while(|ch| ch.is_ascii_alphanumeric() || *ch == '-')
            .map(char::len_utf8)
            .sum::<usize>();
        if name_len > 0 && candidate[name_len..].starts_with("=[") {
            output.push_str("--");
            output.push_str(&candidate[..name_len]);
            output.push('[');
            rest = &candidate[name_len + 2..];
        } else {
            output.push_str("--");
            rest = candidate;
        }
    }

    output.push_str(rest);
    output
}

fn parse_shell(shell: &str) -> Option<Shell> {
    match shell {
        value if value == SUPPORTED_SHELLS[0] => Some(Shell::Bash),
        value if value == SUPPORTED_SHELLS[1] => Some(Shell::Elvish),
        value if value == SUPPORTED_SHELLS[2] => Some(Shell::Fish),
        value if value == SUPPORTED_SHELLS[3] => Some(Shell::PowerShell),
        value if value == SUPPORTED_SHELLS[4] => Some(Shell::Zsh),
        _ => None,
    }
}

fn print_completion_help() {
    eprintln!("usage: herdr completion <{}>", supported_shells_usage());
}

#[cfg(test)]
mod tests {
    #[test]
    fn parses_supported_shells() {
        assert!(matches!(
            super::parse_shell("zsh"),
            Some(clap_complete::Shell::Zsh)
        ));
        assert!(matches!(
            super::parse_shell("powershell"),
            Some(clap_complete::Shell::PowerShell)
        ));
        assert!(super::parse_shell("tcsh").is_none());
    }

    #[test]
    fn zsh_long_options_use_space_separated_arguments() {
        let script =
            "'--cwd=[]:PATH:_files' \\\n'--json[]' \\\n'--split=[]:DIRECTION:(right down)' \\";
        let normalized = super::space_separated_zsh_long_options(script);
        assert!(normalized.contains("'--cwd[]:PATH:_files'"));
        assert!(normalized.contains("'--json[]'"));
        assert!(normalized.contains("'--split[]:DIRECTION:(right down)'"));
        assert!(!normalized.contains("--cwd=[]"));
        assert!(!normalized.contains("--split=[]"));
    }
}
