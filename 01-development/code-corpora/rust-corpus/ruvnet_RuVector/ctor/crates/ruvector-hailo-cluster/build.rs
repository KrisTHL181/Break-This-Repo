//! Build script: run tonic codegen on `proto/embedding.proto`.
//!
//! Uses `protoc-bin-vendored` so we don't depend on a system `protoc`
//! install (ruvultra doesn't have one as of 2026-05). The bundled binary
//! is per-target in the crate; we point `PROTOC` at it before invoking
//! tonic-build.

use std::env;

fn main() {
    let protoc = protoc_bin_vendored::protoc_bin_path()
        .expect("protoc-bin-vendored should ship a protoc for this host");
    // SAFETY: build.rs runs single-threaded (no other code reading env).
    unsafe {
        env::set_var("PROTOC", protoc);
    }

    println!("cargo:rerun-if-changed=proto/embedding.proto");
    println!("cargo:rerun-if-changed=build.rs");

    tonic_build::configure()
        .build_server(true)
        .build_client(true)
        .compile_protos(&["proto/embedding.proto"], &["proto"])
        .expect("tonic-build failed to compile embedding.proto");
}
