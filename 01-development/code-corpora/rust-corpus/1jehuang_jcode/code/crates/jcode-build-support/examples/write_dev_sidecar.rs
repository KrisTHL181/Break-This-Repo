//! Write the dev-binary source metadata sidecar for the current repo state.
//!
//! Self-dev helper: after a direct `scripts/dev_cargo.sh build` (outside the
//! coordinated build queue), the freshly built `target/selfdev/jcode` has no
//! up-to-date `.source.json` sidecar, so `selfdev reload` refuses to publish
//! it. Run this to stamp the binary with the *current* source state:
//!
//! ```sh
//! cargo run -p jcode-build-support --example write_dev_sidecar
//! ```
fn main() -> anyhow::Result<()> {
    let repo = std::env::current_dir()?;
    let state = jcode_build_support::current_source_state(&repo)?;
    let path = jcode_build_support::write_current_dev_binary_source_metadata(&repo, &state)?;
    println!("wrote {} for {}", path.display(), state.version_label);
    Ok(())
}
