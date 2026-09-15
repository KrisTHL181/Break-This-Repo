#![no_main]
use libfuzzer_sys::fuzz_target;
use tirith_core::extract::ScanContext;

fuzz_target!(|data: &str| {
    let _ = tirith_core::extract::tier1_scan(data, ScanContext::Exec);
    let _ = tirith_core::extract::tier1_scan(data, ScanContext::Paste);
});
