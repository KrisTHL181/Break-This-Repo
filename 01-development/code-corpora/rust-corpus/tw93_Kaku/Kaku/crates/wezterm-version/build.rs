fn main() {
    println!("cargo:rerun-if-changed=build.rs");

    let manifest_dir = std::env::var("CARGO_MANIFEST_DIR").unwrap();
    let kaku_toml_path = std::path::Path::new(&manifest_dir)
        .join("..")
        .join("..")
        .join("kaku")
        .join("Cargo.toml");

    let mut ci_tag = String::from("0.1.0-unknown");

    if kaku_toml_path.exists() {
        println!("cargo:rerun-if-changed={}", kaku_toml_path.display());
        if let Ok(contents) = std::fs::read_to_string(&kaku_toml_path) {
            if let Some(line) = contents
                .lines()
                .find(|line| line.trim().starts_with("version ="))
            {
                if let Some(v) = line.split('"').nth(1) {
                    ci_tag = v.to_string();
                }
            }
        }
    }

    let target = std::env::var("TARGET").unwrap_or_else(|_| "unknown".to_string());

    println!("cargo:rustc-env=WEZTERM_TARGET_TRIPLE={}", target);
    println!("cargo:rustc-env=WEZTERM_CI_TAG={}", ci_tag);
}
