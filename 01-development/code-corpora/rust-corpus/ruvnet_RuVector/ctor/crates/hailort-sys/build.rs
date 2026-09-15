//! Build script for hailort-sys.
//!
//! - Without the `hailo` feature: emit a stub bindings file and tell cargo
//!   we don't actually need the library. Lets non-Pi developer machines run
//!   `cargo check` without HailoRT installed.
//! - With the `hailo` feature: run `bindgen` against `<hailo/hailort.h>` and
//!   link against `libhailort.so`. This is the path used on the Pi 5 with
//!   the AI HAT+.
//!
//! ADR-167 §5 step 3.

use std::env;
use std::path::PathBuf;

fn main() {
    println!("cargo:rerun-if-changed=wrapper.h");
    println!("cargo:rerun-if-changed=build.rs");

    let out_dir = PathBuf::from(env::var("OUT_DIR").expect("OUT_DIR not set by cargo"));
    let bindings_path = out_dir.join("bindings.rs");

    // Stub mode: produce an empty bindings file so `include!` in lib.rs
    // works regardless of feature state.
    if env::var_os("CARGO_FEATURE_HAILO").is_none() {
        std::fs::write(
            &bindings_path,
            "// hailort-sys: hailo feature disabled — empty stub bindings.\n",
        )
        .expect("failed to write stub bindings.rs");
        return;
    }

    // === hailo feature ON: real bindgen + link ===

    // Tell cargo to link the shared HailoRT library at runtime.
    // libhailort.so installs at /usr/lib/aarch64-linux-gnu/libhailort.so on
    // a Pi 5 with the `hailort` apt package. cargo's default search paths
    // already include /usr/lib*; if a custom location is needed, set
    // `HAILORT_LIB_DIR` in the environment.
    if let Ok(dir) = env::var("HAILORT_LIB_DIR") {
        println!("cargo:rustc-link-search=native={}", dir);
    }
    println!("cargo:rustc-link-lib=hailort");

    // Header search path. Default `/usr/include` works on Pi OS; allow
    // override via `HAILORT_INCLUDE_DIR` for custom installs / CI.
    let mut builder = bindgen::Builder::default()
        .header("wrapper.h")
        // HailoRT header includes generic stdint/stdbool/stddef — the host
        // libclang resolves these automatically on the Pi.
        .parse_callbacks(Box::new(bindgen::CargoCallbacks::new()))
        // Block C++ stuff (we use the C header).
        .layout_tests(false)
        // Allowlist: keep the surface tight. We bind the device + vstream +
        // network-group + hef API plus a smoke-test init/version function.
        // bindgen will pull in transitive types/structs as needed.
        .allowlist_function("hailo_.*")
        .allowlist_type("hailo_.*")
        .allowlist_var("HAILO_.*");

    if let Ok(dir) = env::var("HAILORT_INCLUDE_DIR") {
        builder = builder.clang_arg(format!("-I{}", dir));
    }

    let bindings = builder.generate().expect(
        "bindgen failed to produce HailoRT bindings — \
         is /usr/include/hailo/hailort.h present? \
         try `sudo apt install hailort` or set HAILORT_INCLUDE_DIR",
    );

    bindings
        .write_to_file(&bindings_path)
        .expect("failed to write bindings.rs to OUT_DIR");
}
