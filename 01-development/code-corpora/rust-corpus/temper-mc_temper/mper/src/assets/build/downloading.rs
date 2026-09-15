use crate::{generate_source::generate_source, write_if_changed};
use build_print::info;
use std::fs::File;
use std::path::{Path, PathBuf};

const SERVER_JAR_URL: &str =
    "https://piston-data.mojang.com/v1/objects/823e2250d24b3ddac457a60c92a6a941943fcd6a/server.jar";

pub fn generate() {
    if let Ok(assets_dir) = setup() {
        // Gotta use file locking cos nexttest running multiple builds at doesn't play nice
        let lock_path = assets_dir
            .parent()
            .expect("Generated assets directory should have a parent")
            .join(".generate.lock");
        let _lock = lock_generation(lock_path);

        let version_changed = if assets_dir.join("version").exists()
            && std::fs::read_to_string(assets_dir.join("version"))
                .expect("Failed to read version file")
                == crate::SERVER_VERSION
        {
            false
        } else {
            info!("Server version changed, regenerating assets");
            std::fs::remove_dir_all(&assets_dir).expect("Failed to remove old generated assets");
            std::fs::create_dir(&assets_dir).expect("Failed to create generated assets directory");
            true
        };

        if !generated_assets_exist(&assets_dir) || version_changed {
            let server_jar_path = assets_dir.join("server.jar");
            if !server_jar_path.exists() {
                download_jar(server_jar_path);
            }

            let java_path = which::which("java").expect("Failed to find java in PATH");

            let output = std::process::Command::new(java_path)
                .arg("-DbundlerMainClass=net.minecraft.data.Main")
                .arg("-jar")
                .arg("server.jar")
                .arg("--all")
                .current_dir(
                    workspace_root::get_workspace_root_directory()
                        .expect("Failed to get workspace root directory")
                        .join("assets/generated"),
                )
                .output()
                .expect("Failed to execute java command");

            if !output.status.success() {
                panic!(
                    "Failed to generate assets\nstatus: {}\nstdout:\n{}\nstderr:\n{}",
                    output.status,
                    String::from_utf8_lossy(&output.stdout),
                    String::from_utf8_lossy(&output.stderr)
                );
            }

            if !generated_assets_exist(&assets_dir) {
                panic!(
                    "Server jar finished without generating the expected reports\nstdout:\n{}\nstderr:\n{}",
                    String::from_utf8_lossy(&output.stdout),
                    String::from_utf8_lossy(&output.stderr)
                );
            }

            info!("Finished generating assets");
        }
        generate_source(assets_dir.clone());
        write_if_changed(
            assets_dir.join("DONT_USE_PLS_READ.txt"),
            include_str!("notice.txt"),
        )
        .expect("Could not write notice file");
        write_if_changed(assets_dir.join("version"), crate::SERVER_VERSION)
            .expect("Could not write version file");
    } else {
        println!("cargo::error=Setup failed");
    }
}

fn lock_generation(path: PathBuf) -> File {
    let file = File::options()
        .read(true)
        .write(true)
        .create(true)
        .truncate(false)
        .open(&path)
        .unwrap_or_else(|error| {
            panic!("Failed to open asset generation lock at {path:?}: {error}")
        });

    file.lock()
        .unwrap_or_else(|error| panic!("Failed to lock asset generation at {path:?}: {error}"));

    file
}

fn download_jar(path: PathBuf) {
    info!(
        "Downloading server jar, this could take a sec. If this takes unusually long and network \
    usage is low, its a bug and should be reported"
    );
    let server_jar_bytes = reqwest::blocking::Client::new()
        .get(SERVER_JAR_URL)
        .send()
        .expect("Failed to send request")
        .error_for_status()
        .expect("Failed to download server jar")
        .bytes()
        .expect("Failed to read response bytes")
        .as_ref()
        .to_owned();
    info!("Downloaded server jar, writing to {:?}", path);
    std::fs::write(path, server_jar_bytes).expect("Failed to write server jar to disk");
    info!("Wrote server jar to disk");
}
fn setup() -> Result<PathBuf, ()> {
    let root = workspace_root::get_workspace_root_directory()
        .expect("Failed to get workspace root directory")
        .canonicalize()
        .expect("Failed to canonicalize root directory");
    let generated_assets_dir = root.join("assets/generated");
    if !generated_assets_dir.exists() {
        if generated_assets_dir
            .parent()
            .expect("Failed to get parent directory")
            .exists()
        {
            std::fs::create_dir_all(&generated_assets_dir)
                .expect("Failed to create generated assets directory");
        } else {
            println!(
                "cargo::error=Parent directory of generated assets does not exist, is {:?} the right directory for the project?",
                root
            );
            return Err(());
        }
    }

    Ok(generated_assets_dir)
}

fn generated_assets_exist(generated_assets_dir: &Path) -> bool {
    let generated_dir = generated_assets_dir.join("generated");
    let reports_dir = generated_dir.join("reports");

    reports_dir.join("blocks.json").exists()
        && reports_dir.join("registries.json").exists()
        && reports_dir.join("datapack.json").exists()
        && generated_dir.join("data").exists()
}
