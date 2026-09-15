fn main() {
    // build.rs runs on the host, so `cfg(target_os)` would always be the host's
    // OS — not the cargo --target. Read CARGO_CFG_TARGET_OS instead.
    // ADR-165 §7 step 1: lets `--features host-test` and other non-espidf
    // targets compile without the espidf-only embuild path, while still
    // re-emitting esp-idf-sys's link args (incl. --ldproxy-linker) when
    // cross-compiling to *-espidf.
    if std::env::var("CARGO_CFG_TARGET_OS").as_deref() == Ok("espidf") {
        embuild::espidf::sysenv::output();
    }
}
