//! Fuzzes the public query JSON and AST serde boundary.

#![no_main]

use helix_ast::query::QueryRequest;
use libfuzzer_sys::fuzz_target;

fuzz_target!(|data: &[u8]| {
    let Ok(request) = serde_json::from_slice::<QueryRequest>(data) else {
        return;
    };
    let encoded = serde_json::to_vec(&request).expect("validated query request must serialize");
    let decoded = serde_json::from_slice::<QueryRequest>(&encoded)
        .expect("serialized query request must deserialize");
    assert_eq!(decoded, request);
});
