mod downloading;
mod generate_source;
mod item_to_block_mapping;
mod registry_packets;
mod tag_packets;

use semver::Version;
use std::fs;
use std::path::{Path, PathBuf};

const MIN_JAVA_VERSION: Version = Version::new(21, 0, 0);

const SERVER_VERSION: &str = "26.2";

const NO_JAVA_MESSAGE: &str = "No java install detected. If you have installed one, please add to your path. If you haven't,\
the Adoptium JDK is recommended and you can get it here: https://adoptium.net/temurin/releases?version=25. If you go with another\
JDK you'll need to make sure it supports at least version 21, but higher is better.";

const RELEASE_FILE_REGEX: &str = r#"JAVA_VERSION="(\d+\.\d+?\.\d+?)?""#;

fn main() {
    println!("cargo:rerun-if-changed=build");
    println!(
        "cargo:rerun-if-changed={}",
        workspace_root::get_workspace_root_directory()
            .unwrap()
            .join("assets")
            .join("generated")
            .join("generated")
            .as_os_str()
            .to_str()
            .unwrap()
    );
    let java_path = match which::which("java") {
        Err(_) => {
            println!("cargo::error={}", NO_JAVA_MESSAGE);
            return;
        }
        Ok(path) => path,
    };

    if let Some(is_higher) = check_version(java_path) {
        if !is_higher {
            println!(
                "cargo::error=Java version is lower than the minimum required version of {}. Please update your Java installation.",
                MIN_JAVA_VERSION
            );
        }
    } else {
        println!(
            "cargo:warning=Could not determine Java version. Build will proceed, but if it fails you are on your own here"
        );
    }

    downloading::generate();
}

fn check_version(path: PathBuf) -> Option<bool> {
    // Using `java -version` sounds easier on paper, but the issue is the different outputs the
    // different JDKs have makes it way harder. The release file should have the same format across
    // all JDKs.

    // If the path is a symlink, resolve it
    let followed_path = if path.is_symlink() {
        match path.read_link() {
            Ok(p) => p,
            Err(_) => {
                println!(
                    "cargo:warning=Can't follow symlink at {:?}, we'll just assume the java install knows what it's doing",
                    path
                );
                return None;
            }
        }
    } else {
        path
    };
    let install_dir = match followed_path.parent() {
        Some(p) => match p.parent() {
            Some(pp) => pp.to_path_buf(),
            None => {
                println!("cargo:warning=Can't get grandparent of {:?}", followed_path);
                return None;
            }
        },
        None => {
            println!("cargo:warning=Can't get parent of {:?}", followed_path);
            return None;
        }
    };
    // Now we see if there is a release file
    let release_file = install_dir.join("release");
    if !release_file.exists() {
        println!("cargo:warning=No release file found at {:?}", release_file);
        return None;
    };

    // Read the file and regex for the semver
    let contents = match std::fs::read_to_string(&release_file) {
        Ok(c) => c,
        Err(e) => {
            println!(
                "cargo:warning=Can't read release file at {:?}: {}",
                release_file, e
            );
            return None;
        }
    };

    let matches = regex::Regex::new(RELEASE_FILE_REGEX)
        .unwrap()
        .captures(&contents);

    let found = match matches {
        Some(found) => found
            .get(1)
            .map(|m| m.as_str().to_string())
            .expect("Regex should have a capture group"),
        None => {
            println!(
                "cargo:warning=No version found in release file at {:?}",
                release_file
            );
            return None;
        }
    };

    // Parse out the semver and see if it's equal or higher than the min we've set
    let release_semver = match Version::parse(&found) {
        Ok(v) => v,
        Err(e) => {
            println!(
                "cargo:warning=Can't parse version from release file at {:?}: {}",
                release_file, e
            );
            return None;
        }
    };

    Some(release_semver >= MIN_JAVA_VERSION)
}

pub(crate) fn write_if_changed(
    path: impl AsRef<Path>,
    content: impl AsRef<[u8]>,
) -> std::io::Result<()> {
    let path = path.as_ref();
    let content = content.as_ref();

    if fs::read(path).is_ok_and(|existing| existing == content) {
        return Ok(());
    }

    fs::write(path, content)
}
