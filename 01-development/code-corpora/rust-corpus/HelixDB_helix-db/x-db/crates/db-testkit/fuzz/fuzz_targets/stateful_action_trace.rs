//! Fuzzes replayable stateful action traces.

#![no_main]

use helix_db_testkit::trace::ReplayTrace;
use libfuzzer_sys::fuzz_target;

fuzz_target!(|data: &[u8]| {
    let Ok(trace) = serde_json::from_slice::<ReplayTrace>(data) else {
        return;
    };
    trace
        .validate()
        .expect("deserialized replay traces must satisfy their lifecycle contract");
    let encoded = trace
        .to_json()
        .expect("validated replay trace must serialize");
    let decoded =
        ReplayTrace::from_json(&encoded).expect("serialized replay trace must deserialize");
    assert_eq!(decoded, trace);
});
