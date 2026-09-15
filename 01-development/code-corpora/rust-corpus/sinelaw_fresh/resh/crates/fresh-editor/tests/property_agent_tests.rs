//! Property-based tests for the remote agent
//!
//! These tests spawn the Python agent locally and use the Rust client (AgentChannel)
//! to stress test agent properties with randomly generated inputs.

use fresh::services::remote::{
    decode_base64, encode_base64, ls_params, read_params, stat_params, write_params, AgentRequest,
    AgentResponse, AGENT_SOURCE,
};
use proptest::prelude::*;
use std::collections::HashSet;
use std::io::{BufRead, BufReader, Write};
use std::process::{Child, Command, Stdio};
use std::sync::atomic::{AtomicU64, Ordering};

/// Test harness for agent communication
struct AgentHarness {
    child: Child,
    stdin: std::process::ChildStdin,
    stdout: BufReader<std::process::ChildStdout>,
    next_id: AtomicU64,
    temp_dir: tempfile::TempDir,
}

impl AgentHarness {
    fn new() -> Option<Self> {
        let temp_dir = tempfile::tempdir().ok()?;

        let mut child = Command::new("python3")
            .arg("-u")
            .arg("-c")
            .arg(AGENT_SOURCE)
            .stdin(Stdio::piped())
            .stdout(Stdio::piped())
            .stderr(Stdio::piped())
            .spawn()
            .ok()?;

        let stdin = child.stdin.take()?;
        let stdout = BufReader::new(child.stdout.take()?);

        let mut harness = Self {
            child,
            stdin,
            stdout,
            next_id: AtomicU64::new(1),
            temp_dir,
        };

        // Read ready message
        let ready = harness.read_response()?;
        if !ready.is_ready() {
            return None;
        }

        Some(harness)
    }

    fn temp_path(&self, name: &str) -> String {
        self.temp_dir
            .path()
            .join(name)
            .to_string_lossy()
            .to_string()
    }

    fn send_request(&mut self, method: &str, params: serde_json::Value) -> Option<AgentResponse> {
        let id = self.next_id.fetch_add(1, Ordering::SeqCst);
        let req = AgentRequest::new(id, method, params);
        self.stdin.write_all(req.to_json_line().as_bytes()).ok()?;
        self.stdin.flush().ok()?;

        // Read responses until we get a final one (result or error)
        loop {
            let resp = self.read_response()?;
            if resp.is_final() {
                return Some(resp);
            }
            // Otherwise it's a streaming data message, skip it
        }
    }

    /// Send a request and collect all streaming data chunks
    fn send_request_with_data(
        &mut self,
        method: &str,
        params: serde_json::Value,
    ) -> Option<(Vec<serde_json::Value>, AgentResponse)> {
        let id = self.next_id.fetch_add(1, Ordering::SeqCst);
        let req = AgentRequest::new(id, method, params);
        self.stdin.write_all(req.to_json_line().as_bytes()).ok()?;
        self.stdin.flush().ok()?;

        let mut data_chunks = Vec::new();

        // Read responses until we get a final one
        loop {
            let resp = self.read_response()?;
            if resp.is_final() {
                return Some((data_chunks, resp));
            }
            // Collect streaming data
            if let Some(data) = resp.data {
                data_chunks.push(data);
            }
        }
    }

    fn read_response(&mut self) -> Option<AgentResponse> {
        let mut line = String::new();
        self.stdout.read_line(&mut line).ok()?;
        serde_json::from_str(&line).ok()
    }

    fn write_file(&mut self, path: &str, data: &[u8]) -> Option<AgentResponse> {
        self.send_request("write", write_params(path, data))
    }

    fn read_file(&mut self, path: &str) -> Option<Vec<u8>> {
        let (data_chunks, _resp) =
            self.send_request_with_data("read", read_params(path, None, None))?;

        // Collect and decode all data chunks
        let mut result = Vec::new();
        for chunk in data_chunks {
            if let Some(b64) = chunk.get("data").and_then(|v| v.as_str()) {
                if let Ok(decoded) = decode_base64(b64) {
                    result.extend(decoded);
                }
            }
        }
        Some(result)
    }

    fn stat(&mut self, path: &str) -> Option<serde_json::Value> {
        let resp = self.send_request("stat", stat_params(path, true))?;
        resp.result
    }

    fn exists(&mut self, path: &str) -> Option<bool> {
        let resp = self.send_request("exists", serde_json::json!({"path": path}))?;
        resp.result?.get("exists")?.as_bool()
    }

    #[allow(dead_code)]
    fn mkdir(&mut self, path: &str) -> Option<AgentResponse> {
        self.send_request("mkdir", serde_json::json!({"path": path}))
    }

    fn rm(&mut self, path: &str) -> Option<AgentResponse> {
        self.send_request("rm", serde_json::json!({"path": path}))
    }

    fn ls(&mut self, path: &str) -> Option<Vec<String>> {
        let resp = self.send_request("ls", ls_params(path))?;
        let result = resp.result?;
        let entries = result.get("entries")?.as_array()?;
        Some(
            entries
                .iter()
                .filter_map(|e: &serde_json::Value| {
                    e.get("name")?.as_str().map(|s: &str| s.to_string())
                })
                .collect(),
        )
    }

    fn realpath(&mut self, path: &str) -> Option<String> {
        let resp = self.send_request("realpath", serde_json::json!({"path": path}))?;
        resp.result?
            .get("path")?
            .as_str()
            .map(|s: &str| s.to_string())
    }
}

impl Drop for AgentHarness {
    fn drop(&mut self) {
        let _ = self.child.kill();
    }
}

// ============================================================================
// Property Tests
// ============================================================================

proptest! {
    #![proptest_config(ProptestConfig {
        cases: 50,
        max_shrink_iters: 100,
        ..ProptestConfig::default()
    })]

    /// Property: Write then read returns identical data (roundtrip)
    #[test]
    fn prop_write_read_roundtrip(data in prop::collection::vec(any::<u8>(), 0..10000)) {
        let Some(mut harness) = AgentHarness::new() else {
            eprintln!("Skipping test: Python3 not available");
            return Ok(());
        };

        let path = harness.temp_path("roundtrip.bin");

        // Write data
        let write_resp = harness.write_file(&path, &data);
        prop_assert!(write_resp.is_some(), "write failed");
        prop_assert!(write_resp.unwrap().result.is_some(), "write returned error");

        // Read back
        let read_data = harness.read_file(&path);
        prop_assert!(read_data.is_some(), "read failed");
        prop_assert_eq!(data, read_data.unwrap(), "data mismatch after roundtrip");
    }

    /// Property: Stat returns consistent results for the same path
    #[test]
    fn prop_stat_consistent(path in "(/)|((/[a-z]+)+)") {
        let Some(mut harness) = AgentHarness::new() else {
            eprintln!("Skipping test: Python3 not available");
            return Ok(());
        };

        // Stat the same path twice
        let stat1 = harness.stat(&path);
        let stat2 = harness.stat(&path);

        // Both should succeed or both should fail
        prop_assert_eq!(stat1.is_some(), stat2.is_some(), "stat consistency failed");

        // If they succeeded, sizes should match
        if let (Some(s1), Some(s2)) = (stat1, stat2) {
            let size1 = s1.get("size").and_then(|v| v.as_u64());
            let size2 = s2.get("size").and_then(|v| v.as_u64());
            prop_assert_eq!(size1, size2, "stat size mismatch");
        }
    }

    /// Property: exists() agrees with stat() success
    #[test]
    fn prop_exists_agrees_with_stat(name in "[a-z]{1,10}".prop_filter("no Windows reserved names",
        |n| !matches!(n.as_str(), "con" | "prn" | "aux" | "nul"))) {
        let Some(mut harness) = AgentHarness::new() else {
            eprintln!("Skipping test: Python3 not available");
            return Ok(());
        };

        let path = harness.temp_path(&name);

        // Initially shouldn't exist
        let exists_before = harness.exists(&path);
        let stat_before = harness.stat(&path);
        prop_assert_eq!(exists_before, Some(false), "file shouldn't exist initially");
        prop_assert!(stat_before.is_none(), "stat should fail for non-existent file");

        // Create file
        harness.write_file(&path, b"test");

        // Now should exist
        let exists_after = harness.exists(&path);
        let stat_after = harness.stat(&path);
        prop_assert_eq!(exists_after, Some(true), "file should exist after write");
        prop_assert!(stat_after.is_some(), "stat should succeed for existing file");
    }

    /// Property: File size matches written data length
    #[test]
    fn prop_size_matches_written_length(data in prop::collection::vec(any::<u8>(), 0..5000)) {
        let Some(mut harness) = AgentHarness::new() else {
            eprintln!("Skipping test: Python3 not available");
            return Ok(());
        };

        let path = harness.temp_path("sized.bin");
        harness.write_file(&path, &data);

        let stat = harness.stat(&path);
        prop_assert!(stat.is_some(), "stat failed");

        let size = stat.unwrap().get("size").and_then(|v| v.as_u64());
        prop_assert_eq!(size, Some(data.len() as u64), "size mismatch");
    }

    /// Property: ls() returns created files
    #[test]
    fn prop_ls_contains_created_files(names in prop::collection::hash_set("[a-z]{1,8}", 1..10)
        .prop_filter("no Windows reserved names",
            |names| !names.iter().any(|n| matches!(n.as_str(), "con" | "prn" | "aux" | "nul")))) {
        let Some(mut harness) = AgentHarness::new() else {
            eprintln!("Skipping test: Python3 not available");
            return Ok(());
        };

        // Create files
        for name in &names {
            let path = harness.temp_path(name);
            harness.write_file(&path, b"content");
        }

        // List directory
        let dir_path = harness.temp_dir.path().to_str().unwrap().to_string();
        let listed = harness.ls(&dir_path);
        prop_assert!(listed.is_some(), "ls failed");

        let listed_set: HashSet<String> = listed.unwrap().into_iter().collect();

        // All created files should be listed
        for name in &names {
            prop_assert!(listed_set.contains(name.as_str()), "missing file: {}", name);
        }
    }

    /// Property: realpath returns canonical absolute path
    #[test]
    fn prop_realpath_is_canonical(_dummy in 0..1i32) {
        let Some(mut harness) = AgentHarness::new() else {
            eprintln!("Skipping test: Python3 not available");
            return Ok(());
        };

        // Create a file
        let path = harness.temp_path("canonical.txt");
        harness.write_file(&path, b"test");

        // Get realpath
        let real = harness.realpath(&path);
        prop_assert!(real.is_some(), "realpath failed");

        let real = real.unwrap();
        // Should be absolute (cross-platform check)
        prop_assert!(
            std::path::Path::new(&real).is_absolute(),
            "realpath should be absolute, got: {}",
            real
        );
        // Should not contain . or .. components
        prop_assert!(
            !real.contains("/./") && !real.contains("\\.\\"),
            "realpath should not contain ./: {}",
            real
        );
        prop_assert!(
            !real.contains("/../") && !real.contains("\\..\\"),
            "realpath should not contain ../: {}",
            real
        );

        // Calling realpath on realpath should return same result
        let real2 = harness.realpath(&real);
        prop_assert_eq!(Some(real.clone()), real2, "realpath should be idempotent");
    }

    /// Property: Delete removes file
    #[test]
    fn prop_delete_removes_file(name in "[a-z]{1,8}".prop_filter("no Windows reserved names",
        |n| !matches!(n.as_str(), "con" | "prn" | "aux" | "nul"))) {
        let Some(mut harness) = AgentHarness::new() else {
            eprintln!("Skipping test: Python3 not available");
            return Ok(());
        };

        let path = harness.temp_path(&name);

        // Create file
        harness.write_file(&path, b"to delete");
        prop_assert_eq!(harness.exists(&path), Some(true), "file should exist after write");

        // Delete
        let rm_resp = harness.rm(&path);
        prop_assert!(rm_resp.is_some(), "rm failed");

        // Should not exist
        prop_assert_eq!(harness.exists(&path), Some(false), "file should not exist after delete");
    }

    /// Property: Base64 encoding roundtrip preserves all byte values
    #[test]
    fn prop_base64_roundtrip(data in prop::collection::vec(any::<u8>(), 0..1000)) {
        let encoded = encode_base64(&data);
        let decoded = decode_base64(&encoded);
        prop_assert!(decoded.is_ok(), "decode failed");
        prop_assert_eq!(data, decoded.unwrap(), "base64 roundtrip mismatch");
    }

    /// Property: Overwriting a file replaces content completely
    #[test]
    fn prop_overwrite_replaces_content(
        data1 in prop::collection::vec(any::<u8>(), 100..500),
        data2 in prop::collection::vec(any::<u8>(), 50..200)
    ) {
        let Some(mut harness) = AgentHarness::new() else {
            eprintln!("Skipping test: Python3 not available");
            return Ok(());
        };

        let path = harness.temp_path("overwrite.bin");

        // Write first content
        harness.write_file(&path, &data1);

        // Overwrite with second content
        harness.write_file(&path, &data2);

        // Read should return second content
        let read = harness.read_file(&path);
        prop_assert!(read.is_some(), "read failed");
        prop_assert_eq!(data2, read.unwrap(), "overwrite didn't replace content");
    }
}

// ============================================================================
// Stress Tests (non-property, but intensive)
// ============================================================================

#[test]
fn stress_many_sequential_operations() {
    let Some(mut harness) = AgentHarness::new() else {
        eprintln!("Skipping test: Python3 not available");
        return;
    };

    // Perform many operations in sequence
    for i in 0..100 {
        let path = harness.temp_path(&format!("stress_{}.txt", i));
        let data = format!("content for file {}", i);

        // Write
        let write_resp = harness.write_file(&path, data.as_bytes());
        assert!(write_resp.is_some(), "write {} failed", i);
        assert!(
            write_resp.unwrap().result.is_some(),
            "write {} returned error",
            i
        );

        // Read back
        let read = harness.read_file(&path);
        assert!(read.is_some(), "read {} failed", i);
        assert_eq!(
            data.as_bytes(),
            read.unwrap().as_slice(),
            "mismatch at {}",
            i
        );

        // Stat
        let stat = harness.stat(&path);
        assert!(stat.is_some(), "stat {} failed", i);
    }

    // List all files
    let dir_path = harness.temp_dir.path().to_str().unwrap().to_string();
    let files = harness.ls(&dir_path);
    assert!(files.is_some(), "final ls failed");
    assert_eq!(files.unwrap().len(), 100, "wrong file count");
}

#[test]
fn stress_large_file() {
    let Some(mut harness) = AgentHarness::new() else {
        eprintln!("Skipping test: Python3 not available");
        return;
    };

    // Create a large file (1 MB)
    let size = 1024 * 1024;
    let data: Vec<u8> = (0..size).map(|i| (i % 256) as u8).collect();

    let path = harness.temp_path("large.bin");

    // Write
    let write_resp = harness.write_file(&path, &data);
    assert!(write_resp.is_some(), "write failed");
    assert!(write_resp.unwrap().result.is_some(), "write returned error");

    // Read back
    let read = harness.read_file(&path);
    assert!(read.is_some(), "read failed");
    let read_data = read.unwrap();
    assert_eq!(data.len(), read_data.len(), "size mismatch");
    assert_eq!(data, read_data, "data mismatch");

    // Verify size via stat
    let stat = harness.stat(&path);
    assert!(stat.is_some(), "stat failed");
    let stat_size = stat.unwrap().get("size").and_then(|v| v.as_u64());
    assert_eq!(stat_size, Some(size as u64), "stat size mismatch");
}

#[test]
fn stress_binary_data_all_bytes() {
    let Some(mut harness) = AgentHarness::new() else {
        eprintln!("Skipping test: Python3 not available");
        return;
    };

    // Create data with all 256 byte values
    let data: Vec<u8> = (0..=255u8).collect();

    let path = harness.temp_path("all_bytes.bin");

    // Write
    harness.write_file(&path, &data);

    // Read back
    let read = harness.read_file(&path);
    assert!(read.is_some(), "read failed");
    assert_eq!(data, read.unwrap(), "binary data mismatch");
}

#[test]
fn stress_nested_directories() {
    let Some(mut harness) = AgentHarness::new() else {
        eprintln!("Skipping test: Python3 not available");
        return;
    };

    // Create deeply nested directory structure
    let base = harness.temp_dir.path().to_str().unwrap().to_string();
    let mut current = base.clone();

    for i in 0..10 {
        current = format!("{}/level{}", current, i);
        let resp = harness.send_request(
            "mkdir",
            serde_json::json!({"path": &current, "parents": true}),
        );
        assert!(resp.is_some(), "mkdir {} failed", i);
    }

    // Create a file at the deepest level
    let deep_file = format!("{}/deep.txt", current);
    harness.write_file(&deep_file, b"deep content");

    // Verify we can read it
    let read = harness.read_file(&deep_file);
    assert!(read.is_some(), "read deep file failed");
    assert_eq!(
        b"deep content".to_vec(),
        read.unwrap(),
        "deep file mismatch"
    );

    // Verify realpath works
    let real = harness.realpath(&deep_file);
    assert!(real.is_some(), "realpath failed");
    assert!(
        real.unwrap().contains("level9"),
        "realpath should contain level9"
    );
}

#[test]
fn stress_rapid_create_delete() {
    let Some(mut harness) = AgentHarness::new() else {
        eprintln!("Skipping test: Python3 not available");
        return;
    };

    // Rapidly create and delete files
    for i in 0..50 {
        let path = harness.temp_path(&format!("rapid_{}.txt", i));

        // Create
        harness.write_file(&path, b"temporary");
        assert_eq!(harness.exists(&path), Some(true), "file {} should exist", i);

        // Delete
        harness.rm(&path);
        assert_eq!(
            harness.exists(&path),
            Some(false),
            "file {} should not exist",
            i
        );
    }
}

#[test]
fn stress_special_characters_in_content() {
    let Some(mut harness) = AgentHarness::new() else {
        eprintln!("Skipping test: Python3 not available");
        return;
    };

    // Test various special characters and patterns
    let test_cases: Vec<&[u8]> = vec![
        b"",                          // empty
        b"\x00",                      // null byte
        b"\x00\x00\x00",              // multiple nulls
        b"\n\n\n",                    // newlines
        b"\r\n\r\n",                  // CRLF
        b"{}[]\"'\\",                 // JSON special chars
        b"\xff\xfe\xfd",              // high bytes
        b"unicode: \xc3\xa9\xc3\xa0", // UTF-8
        b"\t\t  \t  ",                // whitespace
        b"line1\nline2\nline3",       // multi-line
    ];

    for (i, data) in test_cases.iter().enumerate() {
        let path = harness.temp_path(&format!("special_{}.bin", i));

        harness.write_file(&path, data);
        let read = harness.read_file(&path);

        assert!(read.is_some(), "read {} failed", i);
        assert_eq!(data.to_vec(), read.unwrap(), "mismatch for test case {}", i);
    }
}
