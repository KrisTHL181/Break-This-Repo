use codex_utils_absolute_path::AbsolutePathBuf;
use dirs::home_dir;
use std::path::PathBuf;

const CODEX_HOME_ENV_VAR: &str = "CODEX_HOME";
const INTERPRETER_HOME_ENV_VAR: &str = "INTERPRETER_HOME";

/// Returns the path to the Codex/Open Interpreter configuration directory.
///
/// For `codex`, this can be specified by `CODEX_HOME` and defaults to
/// `~/.codex`. For `interpreter`, the only honored override is
/// `INTERPRETER_HOME` and the default is `~/.openinterpreter`; `CODEX_HOME`
/// is deliberately ignored so the two products stay isolated.
///
/// - If an env override is set, the value must exist and be a directory. The
///   value will be canonicalized and this function will Err otherwise.
/// - If no env override is set, this function does not verify that the
///   directory exists.
pub fn find_codex_home() -> std::io::Result<AbsolutePathBuf> {
    let codex_home_env = env_override(CODEX_HOME_ENV_VAR);
    let interpreter_home_env = env_override(INTERPRETER_HOME_ENV_VAR);
    find_codex_home_from_env(interpreter_home_env.as_deref(), codex_home_env.as_deref())
}

fn find_codex_home_from_env(
    interpreter_home_env: Option<&str>,
    codex_home_env: Option<&str>,
) -> std::io::Result<AbsolutePathBuf> {
    // Open Interpreter deliberately does not honor CODEX_HOME: sharing the
    // Codex home leaks Codex config, update caches, and credentials into the
    // Interpreter identity. Migration goes through the explicit /import flow.
    let env_home = if is_open_interpreter() {
        interpreter_home_env.map(|value| (INTERPRETER_HOME_ENV_VAR, value))
    } else {
        codex_home_env.map(|value| (CODEX_HOME_ENV_VAR, value))
    };

    match env_home {
        Some((env_var, val)) => {
            let path = PathBuf::from(val);
            let metadata = std::fs::metadata(&path).map_err(|err| match err.kind() {
                std::io::ErrorKind::NotFound => std::io::Error::new(
                    std::io::ErrorKind::NotFound,
                    format!("{env_var} points to {val:?}, but that path does not exist"),
                ),
                _ => std::io::Error::new(
                    err.kind(),
                    format!("failed to read {env_var} {val:?}: {err}"),
                ),
            })?;

            if !metadata.is_dir() {
                Err(std::io::Error::new(
                    std::io::ErrorKind::InvalidInput,
                    format!("{env_var} points to {val:?}, but that path is not a directory"),
                ))
            } else {
                let canonical = path.canonicalize().map_err(|err| {
                    std::io::Error::new(
                        err.kind(),
                        format!("failed to canonicalize {env_var} {val:?}: {err}"),
                    )
                })?;
                AbsolutePathBuf::from_absolute_path(canonical)
            }
        }
        None => {
            let mut p = home_dir().ok_or_else(|| {
                std::io::Error::new(
                    std::io::ErrorKind::NotFound,
                    "Could not find home directory",
                )
            })?;
            p.push(default_home_dir_name());
            AbsolutePathBuf::from_absolute_path(p)
        }
    }
}

fn env_override(name: &str) -> Option<String> {
    std::env::var(name).ok().filter(|val| !val.is_empty())
}

fn default_home_dir_name() -> &'static str {
    if is_open_interpreter() {
        ".openinterpreter"
    } else {
        ".codex"
    }
}

fn is_open_interpreter() -> bool {
    codex_product_info::Product::current() == codex_product_info::Product::OpenInterpreter
}

#[cfg(test)]
mod tests {
    use super::find_codex_home_from_env;
    use codex_utils_absolute_path::AbsolutePathBuf;
    use dirs::home_dir;
    use pretty_assertions::assert_eq;
    use std::fs;
    use std::io::ErrorKind;
    use tempfile::TempDir;

    #[test]
    fn find_codex_home_env_missing_path_is_fatal() {
        let temp_home = TempDir::new().expect("temp home");
        let missing = temp_home.path().join("missing-codex-home");
        let missing_str = missing
            .to_str()
            .expect("missing codex home path should be valid utf-8");

        let err = find_codex_home_from_env(/*interpreter_home_env*/ None, Some(missing_str))
            .expect_err("missing CODEX_HOME");
        assert_eq!(err.kind(), ErrorKind::NotFound);
        assert!(
            err.to_string().contains("CODEX_HOME"),
            "unexpected error: {err}"
        );
    }

    #[test]
    fn find_codex_home_env_file_path_is_fatal() {
        let temp_home = TempDir::new().expect("temp home");
        let file_path = temp_home.path().join("codex-home.txt");
        fs::write(&file_path, "not a directory").expect("write temp file");
        let file_str = file_path
            .to_str()
            .expect("file codex home path should be valid utf-8");

        let err = find_codex_home_from_env(/*interpreter_home_env*/ None, Some(file_str))
            .expect_err("file CODEX_HOME");
        assert_eq!(err.kind(), ErrorKind::InvalidInput);
        assert!(
            err.to_string().contains("not a directory"),
            "unexpected error: {err}"
        );
    }

    #[test]
    fn find_codex_home_env_valid_directory_canonicalizes() {
        let temp_home = TempDir::new().expect("temp home");
        let temp_str = temp_home
            .path()
            .to_str()
            .expect("temp codex home path should be valid utf-8");

        let resolved = find_codex_home_from_env(/*interpreter_home_env*/ None, Some(temp_str))
            .expect("valid CODEX_HOME");
        let expected = temp_home
            .path()
            .canonicalize()
            .expect("canonicalize temp home");
        let expected = AbsolutePathBuf::from_absolute_path(expected).expect("absolute home");
        assert_eq!(resolved, expected);
    }

    #[test]
    fn find_codex_home_without_env_uses_default_home_dir() {
        let resolved = find_codex_home_from_env(
            /*interpreter_home_env*/ None, /*codex_home_env*/ None,
        )
        .expect("default CODEX_HOME");
        let mut expected = home_dir().expect("home dir");
        expected.push(".codex");
        let expected = AbsolutePathBuf::from_absolute_path(expected).expect("absolute home");
        assert_eq!(resolved, expected);
    }
}
