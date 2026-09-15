//! ADR-159 M1 r2 — `artifact_kinds.rs`.
//!
//! For each `ArtifactKind` variant (`Text`, `StructuredJson`, `VectorRef`,
//! `RuLakeWitness`, `Raw`) — construct, `to_a2a_artifact` → serialize →
//! deserialize → `from_a2a_artifact` → assert equality. For `RuLakeWitness`,
//! verify that `capabilities: ["read"]` survives the round-trip (ADR-159
//! "scope-limited memory grants").

use rvagent_a2a::artifact_types::{from_a2a_artifact, to_a2a_artifact, ArtifactKind};
use rvagent_a2a::types::{Artifact, Part};

fn wire_roundtrip(kind: &ArtifactKind) -> ArtifactKind {
    let art: Artifact = to_a2a_artifact(kind);
    let json = serde_json::to_string(&art).expect("serialize artifact");
    let back: Artifact = serde_json::from_str(&json).expect("deserialize artifact");
    from_a2a_artifact(&back).expect("typed-decode artifact")
}

#[test]
fn text_artifact_roundtrips() {
    let k = ArtifactKind::Text("hello, peer".into());
    let back = wire_roundtrip(&k);
    match back {
        ArtifactKind::Text(s) => assert_eq!(s, "hello, peer"),
        other => panic!("expected Text, got {:?}", other),
    }
}

#[test]
fn structured_json_artifact_roundtrips_with_schema() {
    let k = ArtifactKind::StructuredJson {
        value: serde_json::json!({ "answer": 42, "refs": ["r1", "r2"] }),
        schema: Some("https://schemas.ruvector.ai/rag-answer.v1".into()),
    };
    let back = wire_roundtrip(&k);
    match back {
        ArtifactKind::StructuredJson { value, schema } => {
            assert_eq!(value["answer"], 42);
            assert_eq!(value["refs"][0], "r1");
            assert_eq!(
                schema.as_deref(),
                Some("https://schemas.ruvector.ai/rag-answer.v1")
            );
        }
        other => panic!("expected StructuredJson, got {:?}", other),
    }
}

#[test]
fn vector_ref_artifact_roundtrips() {
    let k = ArtifactKind::VectorRef {
        backend: "rulake".into(),
        collection: "customers".into(),
        witness: "a".repeat(64),
        dim: 768,
        count: 100_000,
    };
    let back = wire_roundtrip(&k);
    match back {
        ArtifactKind::VectorRef {
            backend,
            collection,
            witness,
            dim,
            count,
        } => {
            assert_eq!(backend, "rulake");
            assert_eq!(collection, "customers");
            assert_eq!(witness.len(), 64);
            assert_eq!(dim, 768);
            assert_eq!(count, 100_000);
        }
        other => panic!("expected VectorRef, got {:?}", other),
    }
}

#[test]
fn rulake_witness_preserves_capabilities() {
    // ADR-159 r2: RuLakeWitness.capabilities is a scope-limited memory
    // grant — it MUST survive the wire round-trip unchanged.
    let k = ArtifactKind::RuLakeWitness {
        witness: "b".repeat(64),
        data_ref: "gs://bucket/shard-0/bundle.bin".into(),
        capabilities: vec!["read".into()],
    };
    let back = wire_roundtrip(&k);
    match back {
        ArtifactKind::RuLakeWitness {
            witness,
            data_ref,
            capabilities,
        } => {
            assert_eq!(witness.len(), 64);
            assert_eq!(data_ref, "gs://bucket/shard-0/bundle.bin");
            assert_eq!(capabilities, vec!["read".to_string()]);
        }
        other => panic!("expected RuLakeWitness, got {:?}", other),
    }
}

#[test]
fn raw_artifact_roundtrips_parts() {
    let parts = vec![
        Part::Text {
            text: "chunk 1".into(),
        },
        Part::Data {
            data: serde_json::json!({ "k": "v" }),
        },
    ];
    let k = ArtifactKind::Raw(parts.clone());
    let back = wire_roundtrip(&k);
    match back {
        ArtifactKind::Raw(got) => assert_eq!(got, parts),
        other => panic!("expected Raw, got {:?}", other),
    }
}
