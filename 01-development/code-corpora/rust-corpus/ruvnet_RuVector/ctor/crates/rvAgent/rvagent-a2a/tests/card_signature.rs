//! ADR-159 M1 r2 — `card_signature.rs`.
//!
//! Generate an Ed25519 keypair, sign an `AgentCard`, embed the resulting
//! signature into `metadata.ruvector.signature`, verify → assert the derived
//! `AgentID` matches. Mutate `card.name` → assert verification rejects with
//! `IdentityError::SignatureInvalid` (or the `A2aError` wrapper).

use ed25519_dalek::SigningKey;
use rand_core::OsRng;
use rvagent_a2a::identity::{agent_id_from_pubkey, sign_card, verify_card, IdentityError};
use rvagent_a2a::types::{AgentCapabilities, AgentCard, AgentProvider, AgentSkill, AuthScheme};
use serde_json::json;

fn base_card() -> AgentCard {
    AgentCard {
        name: "signer-under-test".into(),
        description: "card_signature.rs fixture".into(),
        url: "https://agent.example.invalid/a2a".into(),
        provider: AgentProvider {
            organization: "ruvector".into(),
            url: None,
        },
        version: "0.1.0".into(),
        capabilities: AgentCapabilities::default(),
        skills: vec![AgentSkill {
            id: "rag.query".into(),
            name: "RAG".into(),
            description: "x".into(),
            tags: vec![],
            input_modes: vec![],
            output_modes: vec![],
        }],
        authentication: AuthScheme {
            schemes: vec!["bearer".into()],
        },
        metadata: json!({ "ruvector": {} }),
    }
}

/// Re-inject a `CardSignature` into `metadata.ruvector.signature` by round-
/// tripping through `serde_json::Value`. Mirrors the helper in
/// `identity.rs`'s own test module.
fn inject_signature(card: &AgentCard, sig: &rvagent_a2a::identity::CardSignature) -> AgentCard {
    let mut v = serde_json::to_value(card).unwrap();
    let meta = v
        .get_mut("metadata")
        .and_then(|m| m.as_object_mut())
        .unwrap();
    let ruvector = meta
        .entry("ruvector")
        .or_insert_with(|| json!({}))
        .as_object_mut()
        .unwrap();
    ruvector.insert("signature".into(), serde_json::to_value(sig).unwrap());
    serde_json::from_value(v).expect("re-parse card with embedded signature")
}

#[test]
fn signed_card_verifies_and_returns_matching_agent_id() {
    let sk = SigningKey::generate(&mut OsRng);
    let card = base_card();
    let sig = sign_card(&card, &sk).expect("sign");
    let signed = inject_signature(&card, &sig);
    let id = verify_card(&signed).expect("verify");
    // ADR-159 r2: AgentID is content-addressed from the pubkey.
    assert_eq!(id, agent_id_from_pubkey(&sk.verifying_key()));
}

#[test]
fn mutated_card_name_fails_verification() {
    let sk = SigningKey::generate(&mut OsRng);
    let card = base_card();
    let sig = sign_card(&card, &sk).expect("sign");
    let mut signed_value = serde_json::to_value(inject_signature(&card, &sig)).unwrap();

    // Tamper with a field the signature covers.
    signed_value["name"] = json!("tampered-name");
    let tampered: AgentCard = serde_json::from_value(signed_value).unwrap();

    match verify_card(&tampered) {
        Err(IdentityError::SignatureInvalid) => {}
        other => panic!("expected SignatureInvalid, got {:?}", other),
    }
}
