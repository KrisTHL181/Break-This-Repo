use std::fs;
use std::path::{Path, PathBuf};

use helix_ast::query::{QueryRequest, QueryRequestType};

fn fixture_path(name: &str) -> PathBuf {
    Path::new(env!("CARGO_MANIFEST_DIR"))
        .join("../../docker-image/tests/fixtures")
        .join(name)
}

fn read_request(name: &str) -> QueryRequest {
    let path = fixture_path(name);
    let payload = fs::read_to_string(&path)
        .unwrap_or_else(|error| panic!("failed to read {}: {error}", path.display()));
    serde_json::from_str(&payload)
        .unwrap_or_else(|error| panic!("failed to parse {}: {error}", path.display()))
}

#[test]
fn docker_write_fixture_matches_the_current_query_contract() {
    let request = read_request("dynamic-write.json");

    assert_eq!(request.request_type(), QueryRequestType::Write);
}

#[test]
fn docker_read_fixture_matches_the_current_query_contract() {
    let request = read_request("dynamic-read.json");

    assert_eq!(request.request_type(), QueryRequestType::Read);
}
