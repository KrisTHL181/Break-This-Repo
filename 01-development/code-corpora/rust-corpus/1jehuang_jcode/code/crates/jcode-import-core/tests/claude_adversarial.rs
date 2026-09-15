//! Adversarial / edge-case tests for the Claude Code JSONL parser and the
//! parent_uuid ordering logic in `jcode-import-core`.
//!
//! These assert the *target* (correct) behavior agreed with the swarm
//! coordinator:
//!   * A malformed shape inside a single block must NEVER drop the whole line.
//!   * `tool_result.content` may be a string, `null`, or an array of blocks.
//!   * Array `image` blocks flatten to a compact `[image]` placeholder, NOT
//!     their base64 payload (no transcript / context bloat).
//!   * `ordered_claude_code_message_entries` must terminate on cycles and never
//!     drop or duplicate messages; sidechain entries are excluded.
//!
//! Pure functions only (no filesystem / env), so safe to run in parallel.
//! Authored by swarm Worker C (dolphin).

use jcode_import_core::{
    ClaudeCodeContent, ClaudeCodeContentBlock, ClaudeCodeEntry, claude_text_from_content,
    ordered_claude_code_message_entries,
};

fn parse(line: &str) -> Result<ClaudeCodeEntry, serde_json::Error> {
    serde_json::from_str::<ClaudeCodeEntry>(line)
}

/// A single JSONL line must always deserialize, regardless of block shape.
fn parse_ok(line: &str) -> ClaudeCodeEntry {
    parse(line).unwrap_or_else(|e| {
        panic!("line should never be dropped, but failed to parse: {e}\nline: {line}")
    })
}

fn first_tool_result_content(entry: ClaudeCodeEntry) -> String {
    let ClaudeCodeContent::Blocks(blocks) = entry.message.expect("message present").content else {
        panic!("expected block content");
    };
    for block in blocks {
        if let ClaudeCodeContentBlock::ToolResult { content, .. } = block {
            return content;
        }
    }
    panic!("no tool_result block found");
}

fn ordered_uuids(entries: &[ClaudeCodeEntry]) -> Vec<Option<&str>> {
    ordered_claude_code_message_entries(entries)
        .iter()
        .map(|e| e.uuid.as_deref())
        .collect()
}

// ---------------------------------------------------------------------------
// Edge case 1: newer tool_result.content schema variants
// ---------------------------------------------------------------------------

#[test]
fn tool_result_array_text_imports_text() {
    // Array form with a single text block -> the text must be preserved.
    let line = r#"{"type":"user","uuid":"u1","message":{"role":"user","content":[{"type":"tool_result","tool_use_id":"t1","content":[{"type":"text","text":"hello world"}]}]}}"#;
    let content = first_tool_result_content(parse_ok(line));
    assert_eq!(content, "hello world");
}

#[test]
fn tool_result_array_mixed_text_and_image_uses_placeholder_not_base64() {
    // Array form with text + image. Target behavior: keep the text, replace the
    // image with a compact "[image]" placeholder, and NEVER embed base64.
    let line = r#"{"type":"user","uuid":"u1","message":{"role":"user","content":[{"type":"tool_result","tool_use_id":"t1","content":[{"type":"text","text":"hello"},{"type":"image","source":{"type":"base64","media_type":"image/png","data":"BASE64DATA"}}]}]}}"#;
    let content = first_tool_result_content(parse_ok(line));
    assert!(
        content.contains("hello"),
        "text must survive, got {content:?}"
    );
    assert!(
        content.contains("[image]"),
        "image must reduce to a placeholder, got {content:?}"
    );
    assert!(
        !content.contains("BASE64DATA"),
        "base64 image payload must NOT be embedded, got {content:?}"
    );
}

#[test]
fn tool_result_array_image_only_uses_placeholder() {
    let line = r#"{"type":"user","uuid":"u1","message":{"role":"user","content":[{"type":"tool_result","tool_use_id":"t1","content":[{"type":"image","source":{"type":"base64","media_type":"image/png","data":"BASE64DATA"}}]}]}}"#;
    let content = first_tool_result_content(parse_ok(line));
    assert_eq!(content, "[image]");
}

#[test]
fn tool_result_content_null_parses_to_empty_not_dropped() {
    let line = r#"{"type":"user","uuid":"u1","message":{"role":"user","content":[{"type":"tool_result","tool_use_id":"t1","content":null}]}}"#;
    assert_eq!(first_tool_result_content(parse_ok(line)), "");
}

#[test]
fn tool_result_content_missing_parses_to_empty_not_dropped() {
    let line = r#"{"type":"user","uuid":"u1","message":{"role":"user","content":[{"type":"tool_result","tool_use_id":"t1"}]}}"#;
    assert_eq!(first_tool_result_content(parse_ok(line)), "");
}

#[test]
fn tool_result_content_plain_string_still_supported() {
    // Regression guard: legacy string form must keep working.
    let line = r#"{"type":"user","uuid":"u1","message":{"role":"user","content":[{"type":"tool_result","tool_use_id":"t1","content":"plain string result"}]}}"#;
    assert_eq!(
        first_tool_result_content(parse_ok(line)),
        "plain string result"
    );
}

#[test]
fn tool_result_array_nested_content_string_blocks_preserved() {
    // Some array blocks carry their text under a "content" string field rather
    // than "text". Target: preserve that text too.
    let line = r#"{"type":"user","uuid":"u1","message":{"role":"user","content":[{"type":"tool_result","tool_use_id":"t1","content":[{"type":"tool_result","content":"nested output"}]}]}}"#;
    let content = first_tool_result_content(parse_ok(line));
    assert!(
        content.contains("nested output"),
        "nested string content must survive, got {content:?}"
    );
}

// ---------------------------------------------------------------------------
// Large base64 image: explicit memory-bloat guard (edge case 9)
// ---------------------------------------------------------------------------

#[test]
fn large_base64_image_does_not_bloat_imported_text() {
    // A 100 KiB base64 image must NOT end up in the imported text. The target
    // is the compact "[image]" placeholder, so content stays tiny.
    let big = "Q".repeat(100_000);
    let line = format!(
        r#"{{"type":"user","uuid":"u1","message":{{"role":"user","content":[{{"type":"tool_result","tool_use_id":"t1","content":[{{"type":"text","text":"screenshot"}},{{"type":"image","source":{{"type":"base64","media_type":"image/png","data":"{big}"}}}}]}}]}}}}"#
    );
    let content = first_tool_result_content(parse_ok(&line));
    assert!(
        !content.contains(&big),
        "base64 payload must not be embedded verbatim"
    );
    assert!(
        content.len() < 1_000,
        "imported tool_result text must stay compact, got {} bytes",
        content.len()
    );
    assert!(content.contains("screenshot"));
    assert!(content.contains("[image]"));
}

// ---------------------------------------------------------------------------
// Edge case 2: thinking / redacted_thinking / unknown blocks
// ---------------------------------------------------------------------------

#[test]
fn thinking_block_without_signature_parses() {
    let line = r#"{"type":"assistant","uuid":"a1","message":{"role":"assistant","content":[{"type":"thinking","thinking":"reasoning here"}]}}"#;
    let entry = parse_ok(line);
    assert_eq!(
        claude_text_from_content(&entry.message.unwrap().content).as_deref(),
        Some("reasoning here")
    );
}

#[test]
fn thinking_block_with_signature_parses() {
    let line = r#"{"type":"assistant","uuid":"a1","message":{"role":"assistant","content":[{"type":"thinking","thinking":"reasoning","signature":"sig-abc"}]}}"#;
    let entry = parse_ok(line);
    assert_eq!(
        claude_text_from_content(&entry.message.unwrap().content).as_deref(),
        Some("reasoning")
    );
}

#[test]
fn redacted_thinking_block_is_skipped_not_dropped() {
    // redacted_thinking is an unknown variant -> serde(other) => Unknown.
    // The line must still parse and the accompanying text block must survive.
    let line = r#"{"type":"assistant","uuid":"a1","message":{"role":"assistant","content":[{"type":"redacted_thinking","data":"REDACTED"},{"type":"text","text":"after redaction"}]}}"#;
    let entry = parse_ok(line);
    let ClaudeCodeContent::Blocks(blocks) = entry.message.unwrap().content else {
        panic!("expected blocks");
    };
    assert!(matches!(blocks[0], ClaudeCodeContentBlock::Unknown));
    assert!(matches!(blocks[1], ClaudeCodeContentBlock::Text { .. }));
}

#[test]
fn unknown_block_type_does_not_drop_sibling_text() {
    // An unknown block type (e.g. server_tool_use / web_search_tool_result)
    // must degrade to Unknown and NOT cause the whole line to fail.
    let line = r#"{"type":"assistant","uuid":"a1","message":{"role":"assistant","content":[{"type":"server_tool_use","id":"s1","name":"web_search","input":{}},{"type":"text","text":"visible answer"}]}}"#;
    let entry = parse_ok(line);
    assert_eq!(
        claude_text_from_content(&entry.message.unwrap().content).as_deref(),
        Some("visible answer")
    );
}

// ---------------------------------------------------------------------------
// Edge case 3: message.content shapes and unusual roles
// ---------------------------------------------------------------------------

#[test]
fn message_content_plain_string_parses() {
    let line = r#"{"type":"user","uuid":"u1","message":{"role":"user","content":"just a string"}}"#;
    let entry = parse_ok(line);
    assert!(matches!(
        entry.message.unwrap().content,
        ClaudeCodeContent::Text(_)
    ));
}

#[test]
fn message_content_missing_defaults_to_empty() {
    let line = r#"{"type":"user","uuid":"u1","message":{"role":"user"}}"#;
    let entry = parse_ok(line);
    assert!(matches!(
        entry.message.unwrap().content,
        ClaudeCodeContent::Empty
    ));
}

#[test]
fn entry_with_system_role_parses_but_is_excluded_from_ordering() {
    // role/type = system should parse fine, but ordered_* only keeps
    // user/assistant entries.
    let entries = vec![
        parse_ok(
            r#"{"type":"system","uuid":"sys","message":{"role":"system","content":"system notice"}}"#,
        ),
        parse_ok(r#"{"type":"user","uuid":"u1","message":{"role":"user","content":"hi"}}"#),
    ];
    assert_eq!(ordered_uuids(&entries), vec![Some("u1")]);
}

#[test]
fn entry_with_tool_role_is_excluded_from_ordering() {
    let entries = vec![
        parse_ok(
            r#"{"type":"tool","uuid":"tl","message":{"role":"tool","content":"tool chatter"}}"#,
        ),
        parse_ok(
            r#"{"type":"assistant","uuid":"a1","message":{"role":"assistant","content":"answer"}}"#,
        ),
    ];
    assert_eq!(ordered_uuids(&entries), vec![Some("a1")]);
}

// ---------------------------------------------------------------------------
// Edge case 4: sidechain entries excluded from ordering
// ---------------------------------------------------------------------------

#[test]
fn sidechain_entries_excluded_from_message_ordering() {
    let entries = vec![
        parse_ok(r#"{"type":"user","uuid":"m1","message":{"role":"user","content":"main"}}"#),
        parse_ok(
            r#"{"type":"assistant","uuid":"s1","parentUuid":"m1","isSidechain":true,"message":{"role":"assistant","content":"sidechain noise"}}"#,
        ),
        parse_ok(
            r#"{"type":"assistant","uuid":"m2","parentUuid":"m1","message":{"role":"assistant","content":"main reply"}}"#,
        ),
    ];
    // Sidechain s1 must not appear; main chain m1 -> m2 must.
    assert_eq!(ordered_uuids(&entries), vec![Some("m1"), Some("m2")]);
}

// ---------------------------------------------------------------------------
// Edge case 5: parent_uuid chains - branching, cycles, multi-root, self-parent
// ---------------------------------------------------------------------------

#[test]
fn parent_chain_cycle_does_not_infinite_loop_and_keeps_all() {
    // a -> b -> a (mutual parent cycle). The visited-set must terminate and
    // every message must appear exactly once.
    let entries = vec![
        parse_ok(
            r#"{"type":"user","uuid":"a","parentUuid":"b","message":{"role":"user","content":"A"}}"#,
        ),
        parse_ok(
            r#"{"type":"assistant","uuid":"b","parentUuid":"a","message":{"role":"assistant","content":"B"}}"#,
        ),
    ];
    let ordered = ordered_uuids(&entries);
    assert_eq!(
        ordered.len(),
        2,
        "cycle must not drop or duplicate: {ordered:?}"
    );
    assert!(ordered.contains(&Some("a")));
    assert!(ordered.contains(&Some("b")));
}

#[test]
fn self_parent_entry_appears_exactly_once() {
    let entries = vec![parse_ok(
        r#"{"type":"assistant","uuid":"x","parentUuid":"x","message":{"role":"assistant","content":"self"}}"#,
    )];
    assert_eq!(ordered_uuids(&entries), vec![Some("x")]);
}

#[test]
fn branching_chain_keeps_all_children() {
    // root r with two children c1 and c2 (a fork). Both children must survive.
    let entries = vec![
        parse_ok(r#"{"type":"user","uuid":"r","message":{"role":"user","content":"R"}}"#),
        parse_ok(
            r#"{"type":"assistant","uuid":"c1","parentUuid":"r","message":{"role":"assistant","content":"C1"}}"#,
        ),
        parse_ok(
            r#"{"type":"assistant","uuid":"c2","parentUuid":"r","message":{"role":"assistant","content":"C2"}}"#,
        ),
    ];
    let ordered = ordered_uuids(&entries);
    assert_eq!(
        ordered.len(),
        3,
        "branch must keep all entries: {ordered:?}"
    );
    assert_eq!(ordered[0], Some("r"), "root must come first");
    assert!(ordered.contains(&Some("c1")));
    assert!(ordered.contains(&Some("c2")));
}

#[test]
fn multiple_roots_all_preserved() {
    let entries = vec![
        parse_ok(r#"{"type":"user","uuid":"r1","message":{"role":"user","content":"R1"}}"#),
        parse_ok(r#"{"type":"user","uuid":"r2","message":{"role":"user","content":"R2"}}"#),
        parse_ok(
            r#"{"type":"assistant","uuid":"k1","parentUuid":"r1","message":{"role":"assistant","content":"K1"}}"#,
        ),
    ];
    let ordered = ordered_uuids(&entries);
    assert_eq!(ordered.len(), 3, "multi-root must keep all: {ordered:?}");
    for id in [Some("r1"), Some("r2"), Some("k1")] {
        assert!(ordered.contains(&id), "missing {id:?} in {ordered:?}");
    }
}

#[test]
fn missing_root_orphan_chain_is_not_dropped() {
    // All entries reference parents that do NOT exist among message entries.
    // They should be treated as roots (orphans) and preserved, not dropped.
    let entries = vec![
        parse_ok(
            r#"{"type":"assistant","uuid":"o1","parentUuid":"ghost","message":{"role":"assistant","content":"O1"}}"#,
        ),
        parse_ok(
            r#"{"type":"assistant","uuid":"o2","parentUuid":"phantom","message":{"role":"assistant","content":"O2"}}"#,
        ),
    ];
    let ordered = ordered_uuids(&entries);
    assert_eq!(ordered.len(), 2, "orphans must be preserved: {ordered:?}");
}

#[test]
fn duplicate_uuids_do_not_loop_forever() {
    // Two entries share uuid "d" with a self/parent reference. Must terminate
    // and not duplicate the visited node unboundedly.
    let entries = vec![
        parse_ok(r#"{"type":"user","uuid":"d","message":{"role":"user","content":"first"}}"#),
        parse_ok(
            r#"{"type":"assistant","uuid":"d","parentUuid":"d","message":{"role":"assistant","content":"dup"}}"#,
        ),
    ];
    let ordered = ordered_uuids(&entries);
    // The visited-set keys on uuid, so the duplicate uuid collapses. Important
    // property: it terminates and does not explode.
    assert!(
        ordered.len() <= 2,
        "must not duplicate unboundedly: {ordered:?}"
    );
    assert!(ordered.contains(&Some("d")));
}

#[test]
fn long_linear_chain_orders_correctly() {
    // 500-deep linear chain must order head->tail and never drop nodes.
    let mut entries = Vec::new();
    entries.push(parse_ok(
        r#"{"type":"user","uuid":"n0","message":{"role":"user","content":"start"}}"#,
    ));
    for i in 1..500 {
        let line = format!(
            r#"{{"type":"assistant","uuid":"n{i}","parentUuid":"n{}","message":{{"role":"assistant","content":"m{i}"}}}}"#,
            i - 1
        );
        entries.push(parse_ok(&line));
    }
    let ordered = ordered_claude_code_message_entries(&entries);
    assert_eq!(ordered.len(), 500);
    assert_eq!(ordered.first().unwrap().uuid.as_deref(), Some("n0"));
    assert_eq!(ordered.last().unwrap().uuid.as_deref(), Some("n499"));
}

// ---------------------------------------------------------------------------
// Edge case 6: malformed JSON lines must be skipped individually
// ---------------------------------------------------------------------------

#[test]
fn malformed_json_line_fails_to_parse_in_isolation() {
    // A genuinely malformed JSON line returns Err (the importer's line loop
    // skips just this one and keeps going). Good lines around it are unaffected
    // (validated at the importer level in the jcode-base integration test).
    assert!(parse("{this is not valid json}").is_err());
    assert!(parse("").is_err());
    // ...but a well-formed line still parses.
    assert!(
        parse(r#"{"type":"user","uuid":"u1","message":{"role":"user","content":"ok"}}"#).is_ok()
    );
}
