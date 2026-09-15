// Default allocator: mimalloc. `--no-default-features --features rusty-alloc`
// selects the Rust allocator without building the C allocator (#5872).
// With neither feature the standard library system allocator is used.
#[cfg(all(feature = "mimalloc-allocator", not(feature = "rusty-alloc")))]
#[global_allocator]
static GLOBAL: mimalloc::MiMalloc = mimalloc::MiMalloc;

#[cfg(feature = "rusty-alloc")]
#[global_allocator]
static GLOBAL: rusty_alloc_api::RustyAlloc = rusty_alloc_api::RustyAlloc;

fn main() -> std::process::ExitCode {
    codewhale_tui::run(std::env::args().collect())
}
