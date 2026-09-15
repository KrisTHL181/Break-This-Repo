//! Identity and trust for A2A agents (ADR-159 r2 — Identity and trust model).
//!
//! Every production AgentCard is signed with Ed25519. The `AgentID` is the
//! SHAKE-256(32) digest of the ed25519 public key, rendered as a 64-char
//! lowercase hex string — stable, content-addressed, usable as the key in
//! trust graphs, allowlists, and rate-limit buckets.
//!
//! Canonicalization uses a minimal JCS-like form (RFC 8785): keys sorted
//! lexicographically, no insignificant whitespace, integer floats rendered
//! without a decimal point. Good enough for our own cards; full JCS string
//! escaping rules are deferred to the `serde_json` default (which is RFC
//! 8259-compliant) — sufficient for round-trip between our own peers.

use std::{fmt, hash::Hash, str::FromStr};

use ed25519_dalek::{Signature, Signer, SigningKey, Verifier, VerifyingKey};
use serde::{Deserialize, Serialize};
use sha3::{
    digest::{ExtendableOutput, Update, XofReader},
    Shake256,
};

use crate::types::AgentCard;

// ---------------------------------------------------------------------------
// AgentID — hex-encoded SHAKE-256(32) over ed25519 pubkey. Wire form is a
// bare JSON string (via `#[serde(transparent)]`).
// ---------------------------------------------------------------------------

/// Content-addressed agent identifier: SHAKE-256 of the Ed25519 public key,
/// truncated to 32 bytes and hex-encoded. Two peers that mint their pubkey
/// the same way produce the same `AgentID`.
#[derive(Clone, Debug, PartialEq, Eq, Hash, Serialize, Deserialize)]
#[serde(transparent)]
pub struct AgentID(pub String);

impl fmt::Display for AgentID {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(&self.0)
    }
}

impl FromStr for AgentID {
    type Err = IdentityError;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        // Light validation: must be 64 hex chars (32 bytes).
        if s.len() != 64 || !s.chars().all(|c| c.is_ascii_hexdigit()) {
            return Err(IdentityError::PublicKeyMalformed(format!(
                "AgentID must be 64 hex chars, got {:?}",
                s
            )));
        }
        Ok(AgentID(s.to_ascii_lowercase()))
    }
}

/// Derive an [`AgentID`] from an Ed25519 verifying key using SHAKE-256(32).
pub fn agent_id_from_pubkey(pk: &VerifyingKey) -> AgentID {
    let mut hasher = Shake256::default();
    hasher.update(pk.as_bytes());
    let mut reader = hasher.finalize_xof();
    let mut out = [0u8; 32];
    reader.read(&mut out);
    AgentID(hex::encode(out))
}

// ---------------------------------------------------------------------------
// CardSignature — goes into metadata.ruvector.signature.
// ---------------------------------------------------------------------------

/// Ed25519 signature envelope attached to an [`AgentCard`].
///
/// NOTE: ADR-159 specifies base64 for `signature`/`public_key`. This crate
/// has no base64 dependency available, so we serialize as hex and tag the
/// `alg` field accordingly (`"ed25519-hex"`). A migration to base64 is a
/// one-liner once the Cargo.toml gains the dep; the signed bytes themselves
/// are unchanged.
#[derive(Clone, Debug, Serialize, Deserialize, PartialEq, Eq)]
pub struct CardSignature {
    pub alg: String,
    /// Raw Ed25519 signature (64 bytes), hex-encoded.
    pub signature: String,
    /// Raw Ed25519 public key (32 bytes), hex-encoded.
    pub public_key: String,
}

impl Default for CardSignature {
    fn default() -> Self {
        Self {
            alg: "ed25519-hex".to_string(),
            signature: String::new(),
            public_key: String::new(),
        }
    }
}

// ---------------------------------------------------------------------------
// Sign / verify.
// ---------------------------------------------------------------------------

/// Sign an [`AgentCard`]. Canonicalizes the card with any existing
/// `metadata.ruvector.signature` REMOVED, signs the canonical bytes, and
/// returns a [`CardSignature`] ready to be inserted back into the card.
#[tracing::instrument(level = "debug", skip_all)]
pub fn sign_card(card: &AgentCard, keypair: &SigningKey) -> Result<CardSignature, IdentityError> {
    let bytes = canonical_card_bytes(card)?;
    let sig: Signature = keypair.sign(&bytes);
    Ok(CardSignature {
        alg: "ed25519-hex".to_string(),
        signature: hex::encode(sig.to_bytes()),
        public_key: hex::encode(keypair.verifying_key().as_bytes()),
    })
}

/// Verify the signature embedded at `metadata.ruvector.signature`. Returns
/// the [`AgentID`] derived from the embedded public key on success.
#[tracing::instrument(level = "debug", skip_all)]
pub fn verify_card(card: &AgentCard) -> Result<AgentID, IdentityError> {
    // Serialize, then peel out the signature so we can canonicalize without it.
    let mut value =
        serde_json::to_value(card).map_err(|e| IdentityError::Canonicalization(e.to_string()))?;

    let sig = take_signature(&mut value).ok_or(IdentityError::SignatureMissing)?;

    let pk_bytes = hex::decode(&sig.public_key)
        .map_err(|e| IdentityError::PublicKeyMalformed(e.to_string()))?;
    if pk_bytes.len() != 32 {
        return Err(IdentityError::PublicKeyMalformed(format!(
            "pubkey must be 32 bytes, got {}",
            pk_bytes.len()
        )));
    }
    let mut pk_arr = [0u8; 32];
    pk_arr.copy_from_slice(&pk_bytes);
    let vk = VerifyingKey::from_bytes(&pk_arr)
        .map_err(|e| IdentityError::PublicKeyMalformed(e.to_string()))?;

    let sig_bytes = hex::decode(&sig.signature)
        .map_err(|e| IdentityError::SignatureMalformed(e.to_string()))?;
    if sig_bytes.len() != 64 {
        return Err(IdentityError::SignatureMalformed(format!(
            "signature must be 64 bytes, got {}",
            sig_bytes.len()
        )));
    }
    let mut sig_arr = [0u8; 64];
    sig_arr.copy_from_slice(&sig_bytes);
    let signature = Signature::from_bytes(&sig_arr);

    let canonical = canonicalize_value(&value);
    vk.verify(canonical.as_bytes(), &signature)
        .map_err(|_| IdentityError::SignatureInvalid)?;

    Ok(agent_id_from_pubkey(&vk))
}

/// Return canonical bytes of an AgentCard with any existing signature stripped.
pub fn canonical_card_bytes(card: &AgentCard) -> Result<Vec<u8>, IdentityError> {
    let mut value =
        serde_json::to_value(card).map_err(|e| IdentityError::Canonicalization(e.to_string()))?;
    let _ = take_signature(&mut value);
    Ok(canonicalize_value(&value).into_bytes())
}

// ---------------------------------------------------------------------------
// Minimal JCS-ish canonicalizer: sorted keys, no whitespace, serde_json's
// default string escaping. Sufficient for cards we produce ourselves; a full
// RFC 8785 implementation (including number formatting nuances) can be
// swapped in later without changing the public surface.
// ---------------------------------------------------------------------------

fn canonicalize_value(v: &serde_json::Value) -> String {
    use serde_json::Value;
    match v {
        Value::Null => "null".into(),
        Value::Bool(b) => if *b { "true" } else { "false" }.into(),
        Value::Number(n) => n.to_string(),
        Value::String(s) => serde_json::to_string(s).unwrap_or_else(|_| "\"\"".into()),
        Value::Array(xs) => {
            let mut out = String::from("[");
            for (i, x) in xs.iter().enumerate() {
                if i > 0 {
                    out.push(',');
                }
                out.push_str(&canonicalize_value(x));
            }
            out.push(']');
            out
        }
        Value::Object(map) => {
            let mut keys: Vec<&String> = map.keys().collect();
            keys.sort(); // lexicographic on UTF-8 bytes — matches JCS for ASCII keys.
            let mut out = String::from("{");
            for (i, k) in keys.iter().enumerate() {
                if i > 0 {
                    out.push(',');
                }
                out.push_str(&serde_json::to_string(k).unwrap_or_else(|_| "\"\"".into()));
                out.push(':');
                out.push_str(&canonicalize_value(&map[*k]));
            }
            out.push('}');
            out
        }
    }
}

/// Remove and return `metadata.ruvector.signature` from a serialized card.
fn take_signature(value: &mut serde_json::Value) -> Option<CardSignature> {
    let meta = value.get_mut("metadata")?;
    let ruvector = meta.get_mut("ruvector")?;
    let obj = ruvector.as_object_mut()?;
    let sig_val = obj.remove("signature")?;
    serde_json::from_value::<CardSignature>(sig_val).ok()
}

// ---------------------------------------------------------------------------
// Errors.
// ---------------------------------------------------------------------------

#[derive(Debug, thiserror::Error)]
pub enum IdentityError {
    #[error("canonicalization failed: {0}")]
    Canonicalization(String),
    #[error("AgentCard has no metadata.ruvector.signature")]
    SignatureMissing,
    #[error("AgentCard signature malformed: {0}")]
    SignatureMalformed(String),
    #[error("AgentCard signature does not verify against embedded public key")]
    SignatureInvalid,
    #[error("public key malformed: {0}")]
    PublicKeyMalformed(String),
}

// ---------------------------------------------------------------------------
// Tests.
// ---------------------------------------------------------------------------

#[cfg(test)]
mod tests {
    use super::*;
    use ed25519_dalek::SigningKey;

    /// Deterministic test key — avoids pulling rand_core just for tests.
    fn test_signing_key() -> SigningKey {
        let seed = [7u8; 32];
        SigningKey::from_bytes(&seed)
    }

    #[test]
    fn agent_id_hex_is_64_chars() {
        let sk = test_signing_key();
        let id = agent_id_from_pubkey(&sk.verifying_key());
        assert_eq!(id.0.len(), 64);
        assert!(id.0.chars().all(|c| c.is_ascii_hexdigit()));
    }

    #[test]
    fn agent_id_roundtrip() {
        let sk = test_signing_key();
        let id = agent_id_from_pubkey(&sk.verifying_key());
        let s = id.to_string();
        let parsed: AgentID = s.parse().expect("parse");
        assert_eq!(id, parsed);
    }

    #[test]
    fn agent_id_from_str_rejects_wrong_length() {
        assert!(AgentID::from_str("short").is_err());
    }

    #[test]
    fn agent_id_serializes_as_bare_string() {
        let id = AgentID("deadbeef".repeat(8));
        let j = serde_json::to_string(&id).unwrap();
        assert!(j.starts_with('"'), "wire form is a bare string: {}", j);
        assert!(j.ends_with('"'));
    }
}
