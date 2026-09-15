//! Typed artifact layer (ADR-159 r2 — Typed artifact semantics).
//!
//! The A2A spec carries outputs as `Artifact { parts: Vec<Part> }` — flexible,
//! but enforces nothing on receivers. This module wraps that wire format in a
//! ruvector-typed sum (`ArtifactKind`) so peers that opt in get a shape-
//! checked view, while peers that don't simply see the underlying `Part::Data`.
//!
//! ## Wire encoding
//!
//! Non-text variants are serialized as a single `Part::Data` whose JSON object
//! carries a typed payload. The version sidecar lives on the wrapping
//! `Artifact.metadata.ruvector.artifact_kind_version` so receivers can reject
//! unknown versions up-front.
//!
//! ## Versioning (ADR-159 r3 — Versioning)
//!
//! `ArtifactKind` is `#[non_exhaustive]` and every card advertises
//! `metadata.ruvector.artifact_kind_version`. Receivers reject unknown
//! versions with [`A2aError::ArtifactVersionUnsupported`] rather than
//! silently truncating — schema evolution is a negotiated version bump.

use serde::{Deserialize, Serialize};

use crate::error::A2aError;
use crate::types::{Artifact, Part};

/// Current wire version shipped by this crate. Peers negotiate the highest
/// value present in both [`supported_versions`] sets.
pub const ARTIFACT_KIND_VERSION: &str = "1";

/// Well-known artifact name used on the wire so receivers can distinguish a
/// ruvector-typed artifact from an opaque one at a glance.
const ARTIFACT_NAME: &str = "ruvector.artifact_kind";

/// ruvector-typed wrapper around the spec's `Artifact`.
///
/// The variants match ADR-159 §"Typed artifact semantics". Marked
/// `#[non_exhaustive]` so adding a variant is a minor version bump, not a
/// breaking change to downstream matchers.
#[derive(Clone, Debug, PartialEq)]
#[non_exhaustive]
pub enum ArtifactKind {
    /// Plain text. Wire: single `Part::Text`.
    Text(String),
    /// Structured JSON. `schema` is the optional JSON-Schema URL a
    /// receiver can use to shape-check the payload.
    StructuredJson {
        value: serde_json::Value,
        schema: Option<String>,
    },
    /// By-reference vector payload. `witness` is a 64-hex-char
    /// content-addressed handle; the receiver resolves the actual data
    /// from the shared ruLake tier named by `backend`/`collection`.
    VectorRef {
        backend: String,
        collection: String,
        witness: String,
        dim: u32,
        count: u32,
    },
    /// Explicit ruLake bundle pointer; enables zero-copy handoff between
    /// peers on the same tier. `data_ref` is the tier URI (e.g. `gs://`).
    /// `capabilities` are the scope-limited grants the receiver gets.
    RuLakeWitness {
        witness: String,
        data_ref: String,
        capabilities: Vec<String>,
    },
    /// Escape hatch — pass through an opaque list of `Part`s unchanged.
    Raw(Vec<Part>),
}

// ---------------------------------------------------------------------------
// Internal wire representation.
// ---------------------------------------------------------------------------

#[derive(Serialize, Deserialize)]
#[serde(tag = "kind", rename_all = "snake_case")]
enum WireKind {
    StructuredJson {
        value: serde_json::Value,
        #[serde(default, skip_serializing_if = "Option::is_none")]
        schema: Option<String>,
    },
    VectorRef {
        backend: String,
        collection: String,
        witness: String,
        dim: u32,
        count: u32,
    },
    RuLakeWitness {
        witness: String,
        data_ref: String,
        capabilities: Vec<String>,
    },
}

// ---------------------------------------------------------------------------
// Public API
// ---------------------------------------------------------------------------

/// Versions this build of the crate can parse. Peers negotiate the highest
/// value present in both sets — see [`negotiate_version`].
pub fn supported_versions() -> Vec<String> {
    vec![ARTIFACT_KIND_VERSION.to_string()]
}

/// Return the highest version present in both `local` and `remote`. Plain
/// lexicographic ordering is sufficient while versions are single integers.
pub fn negotiate_version(local: &[String], remote: &[String]) -> Option<String> {
    local
        .iter()
        .filter(|v| remote.iter().any(|r| r == *v))
        .max()
        .cloned()
}

fn kind_tag(kind: &ArtifactKind) -> &'static str {
    match kind {
        ArtifactKind::Text(_) => "text",
        ArtifactKind::StructuredJson { .. } => "structured_json",
        ArtifactKind::VectorRef { .. } => "vector_ref",
        ArtifactKind::RuLakeWitness { .. } => "rulake_witness",
        ArtifactKind::Raw(_) => "raw",
    }
}

/// Lift a typed `ArtifactKind` onto the spec's `Artifact`. Text + Raw
/// degrade to natural Part shapes; structured variants become a single
/// `Part::Data` carrying the typed payload. The sidecar version tag is
/// attached to `Artifact.metadata`.
pub fn to_a2a_artifact(kind: &ArtifactKind) -> Artifact {
    let tag = kind_tag(kind);

    let parts = match kind {
        ArtifactKind::Text(text) => vec![Part::Text { text: text.clone() }],
        ArtifactKind::Raw(parts) => parts.clone(),
        ArtifactKind::StructuredJson { value, schema } => {
            let wire = WireKind::StructuredJson {
                value: value.clone(),
                schema: schema.clone(),
            };
            let data = serde_json::to_value(&wire).unwrap_or(serde_json::Value::Null);
            vec![Part::Data { data }]
        }
        ArtifactKind::VectorRef {
            backend,
            collection,
            witness,
            dim,
            count,
        } => {
            let wire = WireKind::VectorRef {
                backend: backend.clone(),
                collection: collection.clone(),
                witness: witness.clone(),
                dim: *dim,
                count: *count,
            };
            let data = serde_json::to_value(&wire).unwrap_or(serde_json::Value::Null);
            vec![Part::Data { data }]
        }
        ArtifactKind::RuLakeWitness {
            witness,
            data_ref,
            capabilities,
        } => {
            let wire = WireKind::RuLakeWitness {
                witness: witness.clone(),
                data_ref: data_ref.clone(),
                capabilities: capabilities.clone(),
            };
            let data = serde_json::to_value(&wire).unwrap_or(serde_json::Value::Null);
            vec![Part::Data { data }]
        }
    };

    Artifact {
        name: Some(ARTIFACT_NAME.to_string()),
        description: None,
        parts,
        index: 0,
        append: false,
        last_chunk: true,
        metadata: serde_json::json!({
            "ruvector": {
                "artifact_kind": tag,
                "artifact_kind_version": ARTIFACT_KIND_VERSION,
            }
        }),
    }
}

/// Recover a typed `ArtifactKind` from the spec `Artifact`.
///
/// Returns [`A2aError::ArtifactVersionUnsupported`] if the sidecar declares
/// a version this build doesn't recognize. A plain-text artifact or a
/// multi-Part blob without the sidecar is accepted as `Text` / `Raw`
/// respectively (v1-implicit).
pub fn from_a2a_artifact(art: &Artifact) -> Result<ArtifactKind, A2aError> {
    // Version gate first — if the sidecar declares a version this build
    // doesn't support, reject up-front regardless of payload shape.
    if let Some(version) = art
        .metadata
        .get("ruvector")
        .and_then(|r| r.get("artifact_kind_version"))
        .and_then(|v| v.as_str())
    {
        let supported = supported_versions();
        if !supported.contains(&version.to_string()) {
            return Err(A2aError::ArtifactVersionUnsupported {
                got: version.to_string(),
                supported,
            });
        }
    }

    // Zero parts → treat as a `Raw` empty artifact rather than an error.
    if art.parts.is_empty() {
        return Ok(ArtifactKind::Raw(vec![]));
    }

    // Single Text part → Text (v1-implicit).
    if art.parts.len() == 1 {
        if let Part::Text { text } = &art.parts[0] {
            return Ok(ArtifactKind::Text(text.clone()));
        }
    }

    // Single Data part with a recognised `kind` → typed decode.
    if art.parts.len() == 1 {
        if let Part::Data { data } = &art.parts[0] {
            if data.get("kind").and_then(|k| k.as_str()).is_some() {
                let wire: WireKind = serde_json::from_value(data.clone()).map_err(|e| {
                    A2aError::Internal(format!("artifact payload decode failed: {e}"))
                })?;
                return Ok(match wire {
                    WireKind::StructuredJson { value, schema } => {
                        ArtifactKind::StructuredJson { value, schema }
                    }
                    WireKind::VectorRef {
                        backend,
                        collection,
                        witness,
                        dim,
                        count,
                    } => ArtifactKind::VectorRef {
                        backend,
                        collection,
                        witness,
                        dim,
                        count,
                    },
                    WireKind::RuLakeWitness {
                        witness,
                        data_ref,
                        capabilities,
                    } => ArtifactKind::RuLakeWitness {
                        witness,
                        data_ref,
                        capabilities,
                    },
                });
            }
        }
    }

    // Fall-through: treat the whole parts list as opaque.
    Ok(ArtifactKind::Raw(art.parts.clone()))
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;

    fn roundtrip(k: ArtifactKind) {
        let art = to_a2a_artifact(&k);
        let back = from_a2a_artifact(&art).expect("roundtrip decode");
        assert_eq!(k, back, "roundtrip mismatch");
    }

    #[test]
    fn roundtrip_text() {
        roundtrip(ArtifactKind::Text("hello world".into()));
    }

    #[test]
    fn roundtrip_structured_json() {
        roundtrip(ArtifactKind::StructuredJson {
            value: serde_json::json!({ "answer": 42, "tags": ["a", "b"] }),
            schema: Some("https://schemas.ruvector.ai/answer.v1".into()),
        });
    }

    #[test]
    fn roundtrip_vector_ref() {
        roundtrip(ArtifactKind::VectorRef {
            backend: "rulake".into(),
            collection: "customers".into(),
            witness: "d".repeat(64),
            dim: 768,
            count: 100_000,
        });
    }

    #[test]
    fn roundtrip_rulake_witness() {
        roundtrip(ArtifactKind::RuLakeWitness {
            witness: "deadbeef".repeat(8),
            data_ref: "gs://ruvector/shard-0".into(),
            capabilities: vec!["read".into(), "search".into()],
        });
    }

    #[test]
    fn roundtrip_raw() {
        roundtrip(ArtifactKind::Raw(vec![
            Part::Text { text: "one".into() },
            Part::Data {
                data: serde_json::json!({"k":"v"}),
            },
        ]));
    }

    #[test]
    fn future_version_is_rejected() {
        // Hand-craft an artifact advertising v2 — the current build supports
        // only v1 and must refuse, not silently downgrade.
        let art = Artifact {
            name: Some(ARTIFACT_NAME.into()),
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
        let err = from_a2a_artifact(&art).unwrap_err();
        assert!(matches!(err, A2aError::ArtifactVersionUnsupported { .. }));
    }

    #[test]
    fn negotiate_picks_intersection() {
        let local = vec!["1".to_string(), "2".to_string()];
        let remote = vec!["1".to_string(), "3".to_string()];
        assert_eq!(negotiate_version(&local, &remote), Some("1".to_string()));
    }

    #[test]
    fn negotiate_returns_none_when_disjoint() {
        let local = vec!["1".to_string()];
        let remote = vec!["9".to_string()];
        assert_eq!(negotiate_version(&local, &remote), None);
    }

    #[test]
    fn supported_versions_contains_current() {
        assert!(supported_versions().contains(&ARTIFACT_KIND_VERSION.to_string()));
    }
}
