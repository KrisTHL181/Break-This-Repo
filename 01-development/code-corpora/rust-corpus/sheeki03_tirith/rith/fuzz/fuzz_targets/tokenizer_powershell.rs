#![no_main]
use libfuzzer_sys::fuzz_target;
use tirith_core::tokenize::ShellType;

fuzz_target!(|data: &str| {
    let _ = tirith_core::tokenize::tokenize(data, ShellType::PowerShell);
});
