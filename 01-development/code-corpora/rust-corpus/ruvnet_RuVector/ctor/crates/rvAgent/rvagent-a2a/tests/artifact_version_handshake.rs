//! ADR-159 M1 r3 — `artifact_version_handshake.rs`.
//!
//! `ArtifactKind` is `#[non_exhaustive]` and wire form includes
//! `metadata.ruvector.artifact_kind_version`. This test exercises:
//!   - `negotiate_version(&["1"], &["1", "2"])` → `Some("1")` (intersection).
//!   - `negotiate_version(&["2"], &["1"])` → `None` (no overlap).
//!   - Decoding a `v2` artifact with a `v1`-only peer → `ArtifactVersionUnsupported`.

use rvagent_a2a::artifact_types::{from_a2a_artifact, negotiate_version, ArtifactKind};
use rvagent_a2a::error::A2aError;
use rvagent_a2a::types::{Artifact, Part};

#[test]
fn negotiate_version_picks_intersection() {
    let got = negotiate_version(&["1".into()], &["1".into(), "2".into()]);
    assert_eq!(got.as_deref(), Some("1"));
}

#[test]
fn negotiate_version_none_on_disjoint() {
    let got = negotiate_version(&["2".into()], &["1".into()]);
    assert!(got.is_none(), "expected None, got {:?}", got);
}

#[test]
fn decoding_future_version_errors_with_expected_variant() {
    // Construct a v2 artifact. The typed layer only supports v1 today
    // (per ADR-159 r3) so `from_a2a_artifact` must reject.
    let art = Artifact {
        name: Some("v2-artifact".into()),
        description: None,
        parts: vec![Part::Text {
            text: "from the future".into(),
        }],
        index: 0,
        append: false,
        last_chunk: true,
        metadata: serde_json::json!({
            "ruvector": { "artifact_kind_version": "2" }
        }),
    };

    match from_a2a_artifact(&art) {
        Err(A2aError::ArtifactVersionUnsupported { got, supported }) => {
            assert_eq!(got, "2");
            assert!(
                supported.iter().any(|s| s == "1"),
                "supported: {:?}",
                supported
            );
        }
        other => panic!("expected ArtifactVersionUnsupported, got {:?}", other),
    }

    // Sanity: round-tripping a v1 artifact does succeed.
    let ok = ArtifactKind::Text("ok".into());
    let wire = rvagent_a2a::artifact_types::to_a2a_artifact(&ok);
    assert!(from_a2a_artifact(&wire).is_ok());
}
