[![Documentation](https://docs.rs/home/badge.svg)](https://docs.rs/home)
[![crates.io](https://img.shields.io/crates/v/home.svg)](https://crates.io/crates/home)

Canonical definitions of `home_dir`, `cargo_home`, and `rustup_home`.

This provides the definition of `home_dir` used by Cargo and rustup,
as well functions to find the correct value of `CARGO_HOME` and
`RUSTUP_HOME`.

This crate previously used its own definition of `home_dir` on Windows to avoid
[rust-lang/rust#43321], but this was fixed in [1.85.0] so this crate now
simply forwards to [`home_dir`].

This crate further provides two functions, `cargo_home` and
`rustup_home`, which are the canonical way to determine the location
that Cargo and rustup store their data.

> This crate is maintained by the Cargo team, primarily for use by Cargo and Rustup
> and not intended for external use. This
> crate may make major changes to its APIs or be deprecated without warning.

[rust-lang/rust#43321]: https://github.com/rust-lang/rust/issues/43321
[`home_dir`]: https://doc.rust-lang.org/nightly/std/env/fn.home_dir.html
[1.85.0]: https://blog.rust-lang.org/2025/02/20/Rust-1.85.0/#updates-to-std-env-home-dir

## License

MIT OR Apache-2.0
