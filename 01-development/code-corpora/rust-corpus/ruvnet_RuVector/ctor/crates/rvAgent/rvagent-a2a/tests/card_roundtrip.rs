//! ADR-159 M1 — `card_roundtrip.rs`.
//!
//! Build an `AgentCard` with every field populated (including skills,
//! capabilities, provider, and `metadata.ruvector.*`), serialize it to JSON
//! via `serde_json::to_string_pretty`, parse it back, and assert structural
//! equality. The A2A spec's sample fixtures are not vendored here; we
//! construct a card inline whose shape matches the documented spec fields
//! (see `crates/rvAgent/rvagent-a2a/src/types.rs`).

use rvagent_a2a::types::{AgentCapabilities, AgentCard, AgentProvider, AgentSkill, AuthScheme};

fn full_card() -> AgentCard {
    AgentCard {
        name: "ruvector-test-agent".into(),
        description: "A2A acceptance-test agent with every optional field populated.".into(),
        url: "https://agent.example.invalid/a2a".into(),
        provider: AgentProvider {
            organization: "ruvector".into(),
            url: Some("https://ruvector.ai".into()),
        },
        version: "0.1.0".into(),
        capabilities: AgentCapabilities {
            streaming: true,
            push_notifications: true,
        },
        skills: vec![
            AgentSkill {
                id: "rag.query".into(),
                name: "RAG query".into(),
                description: "Search the vector corpus and synthesize.".into(),
                tags: vec!["retrieval".into(), "rag".into()],
                input_modes: vec!["text/plain".into()],
                output_modes: vec!["text/plain".into(), "application/json".into()],
            },
            AgentSkill {
                id: "embed.vectorize".into(),
                name: "Embed".into(),
                description: "Return embeddings for arbitrary text.".into(),
                tags: vec!["embed".into()],
                input_modes: vec!["text/plain".into()],
                output_modes: vec!["application/json".into()],
            },
        ],
        authentication: AuthScheme {
            schemes: vec!["bearer".into(), "apikey".into()],
        },
        // ADR-159 §Core type sketches — ruvector-specific extensions live
        // under `metadata.ruvector.*`.
        metadata: serde_json::json!({
            "ruvector": {
                "artifact_kind_version": "1",
                "artifact_kind_versions_supported": ["1"],
                "memory": {
                    "rulake": { "available": true, "capabilities": ["read"] }
                },
                "identity": {
                    "agent_id": "0000000000000000000000000000000000000000000000000000000000000000"
                }
            }
        }),
    }
}

#[test]
fn agent_card_full_json_roundtrip() {
    let card = full_card();
    let pretty = serde_json::to_string_pretty(&card).expect("serialize");
    let back: AgentCard = serde_json::from_str(&pretty).expect("deserialize");
    assert_eq!(card, back, "AgentCard did not survive JSON round-trip");
}

#[test]
fn agent_card_wire_uses_camel_case() {
    let card = full_card();
    let pretty = serde_json::to_string_pretty(&card).expect("serialize");
    // camelCase on the wire — capabilities.pushNotifications, skills[].inputModes.
    assert!(
        pretty.contains("pushNotifications"),
        "expected camelCase, got:\n{}",
        pretty
    );
    assert!(
        pretty.contains("inputModes"),
        "expected camelCase, got:\n{}",
        pretty
    );
    assert!(
        pretty.contains("outputModes"),
        "expected camelCase, got:\n{}",
        pretty
    );
    // Ruvector extensions ride inside `metadata.ruvector.*`.
    assert!(pretty.contains("\"ruvector\""));
}

#[test]
fn agent_card_skills_preserved_through_roundtrip() {
    let card = full_card();
    let back: AgentCard =
        serde_json::from_str(&serde_json::to_string(&card).unwrap()).expect("deserialize");
    assert_eq!(back.skills.len(), 2);
    assert_eq!(back.skills[0].id, "rag.query");
    assert_eq!(back.skills[1].id, "embed.vectorize");
    assert_eq!(
        back.skills[0].tags,
        vec!["retrieval".to_string(), "rag".to_string()]
    );
}
