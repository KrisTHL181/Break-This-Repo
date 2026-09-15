use base64::Engine;
use hmac::{Hmac, Mac};
use sha2::Sha256;

type HmacSha256 = Hmac<Sha256>;

/// Standard Webhooks requires symmetric signing keys to contain between 24
/// and 64 bytes after decoding the `whsec_` payload.
pub const MIN_WEBHOOK_SECRET_KEY_BYTES: usize = 24;
pub const MAX_WEBHOOK_SECRET_KEY_BYTES: usize = 64;

/// Decode and validate a Polar/Standard-Webhooks signing secret.
///
/// Kept public within the binary crate so configuration loading and request
/// verification enforce the same format and bounded key-length invariant.
pub fn decode_webhook_secret(secret: &str) -> Result<Vec<u8>, WebhookError> {
    let key_b64 = secret
        .strip_prefix("whsec_")
        .ok_or(WebhookError::InvalidSecret)?;
    let decoded = base64::engine::general_purpose::STANDARD
        .decode(key_b64)
        .map_err(|_| WebhookError::InvalidSecret)?;
    if !(MIN_WEBHOOK_SECRET_KEY_BYTES..=MAX_WEBHOOK_SECRET_KEY_BYTES).contains(&decoded.len()) {
        return Err(WebhookError::InvalidSecret);
    }
    Ok(decoded)
}

/// Verify a Standard Webhooks signature (used by Polar.sh).
///
/// - `secret`: `"whsec_<base64_key>"` — prefix stripped, remainder base64-decoded to get HMAC key.
/// - `msg_id`: `webhook-id` header value.
/// - `timestamp`: `webhook-timestamp` header value (Unix epoch seconds as string).
/// - `raw_body`: raw request body bytes.
/// - `sig_header`: `webhook-signature` header — space-separated `"v1,<base64_sig>"` entries.
/// - `max_age_secs`: maximum allowed age of the timestamp (Standard Webhooks default: 300s).
pub fn verify_webhook(
    secret: &str,
    msg_id: &str,
    timestamp: &str,
    raw_body: &[u8],
    sig_header: &str,
    max_age_secs: i64,
) -> Result<(), WebhookError> {
    // Defense in depth: configuration validates this at startup, but keep the
    // request boundary independently safe for direct callers and future wiring.
    let key_bytes = decode_webhook_secret(secret)?;

    // Replay protection: reject messages whose timestamp is out of window.
    let ts: i64 = timestamp
        .parse()
        .map_err(|_| WebhookError::InvalidTimestamp)?;
    let now = chrono::Utc::now().timestamp();
    // checked_sub guards against overflow on extreme timestamps.
    let expired = now
        .checked_sub(ts)
        .map(|d| d.unsigned_abs() > max_age_secs as u64)
        .unwrap_or(true);
    if expired {
        return Err(WebhookError::TimestampExpired);
    }

    // Standard Webhooks HMAC input: "{msg_id}.{timestamp}.{body}".
    let mut mac =
        HmacSha256::new_from_slice(&key_bytes).map_err(|_| WebhookError::InvalidSecret)?;
    mac.update(msg_id.as_bytes());
    mac.update(b".");
    mac.update(timestamp.as_bytes());
    mac.update(b".");
    mac.update(raw_body);

    let expected = mac.finalize().into_bytes();

    // Accept any matching "v1,<base64>" entry — supports key rotation
    // where multiple signatures are sent in the same header.
    let mut found_v1 = false;
    for entry in sig_header.split(' ') {
        let entry = entry.trim();
        if let Some(b64_sig) = entry.strip_prefix("v1,") {
            found_v1 = true;
            if let Ok(provided) = base64::engine::general_purpose::STANDARD.decode(b64_sig) {
                if provided.len() == expected.len()
                    && constant_time_eq(expected.as_slice(), &provided)
                {
                    return Ok(());
                }
            }
        }
    }

    if !found_v1 {
        return Err(WebhookError::InvalidSignatureFormat);
    }

    Err(WebhookError::SignatureMismatch)
}

/// Constant-time byte comparison to prevent timing attacks.
fn constant_time_eq(a: &[u8], b: &[u8]) -> bool {
    if a.len() != b.len() {
        return false;
    }
    let mut diff = 0u8;
    for (x, y) in a.iter().zip(b.iter()) {
        diff |= x ^ y;
    }
    diff == 0
}

#[derive(Debug)]
pub enum WebhookError {
    InvalidTimestamp,
    TimestampExpired,
    InvalidSecret,
    InvalidSignatureFormat,
    SignatureMismatch,
}

impl std::fmt::Display for WebhookError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::InvalidTimestamp => write!(f, "invalid timestamp"),
            Self::TimestampExpired => write!(f, "timestamp expired"),
            Self::InvalidSecret => write!(f, "invalid webhook secret"),
            Self::InvalidSignatureFormat => write!(f, "no v1 signature found"),
            Self::SignatureMismatch => write!(f, "signature mismatch"),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn make_secret() -> (String, Vec<u8>) {
        let raw_key = b"test-secret-key-32bytes-long!!!!";
        let b64 = base64::engine::general_purpose::STANDARD.encode(raw_key);
        (format!("whsec_{b64}"), raw_key.to_vec())
    }

    fn sign_body(key: &[u8], msg_id: &str, ts: &str, body: &[u8]) -> String {
        let mut mac = HmacSha256::new_from_slice(key).unwrap();
        mac.update(msg_id.as_bytes());
        mac.update(b".");
        mac.update(ts.as_bytes());
        mac.update(b".");
        mac.update(body);
        let sig = mac.finalize().into_bytes();
        format!(
            "v1,{}",
            base64::engine::general_purpose::STANDARD.encode(sig)
        )
    }

    #[test]
    fn test_valid_signature() {
        let (secret, key) = make_secret();
        let body = b"test body content";
        let msg_id = "msg_abc123";
        let ts = chrono::Utc::now().timestamp().to_string();
        let sig_header = sign_body(&key, msg_id, &ts, body);

        assert!(verify_webhook(&secret, msg_id, &ts, body, &sig_header, 300).is_ok());
    }

    #[test]
    fn test_invalid_signature() {
        let (secret, _) = make_secret();
        let body = b"test body";
        let msg_id = "msg_abc123";
        let ts = chrono::Utc::now().timestamp().to_string();

        let wrong_key = b"wrong-secret-key-32bytes-long!!!";
        let sig_header = sign_body(wrong_key, msg_id, &ts, body);

        let result = verify_webhook(&secret, msg_id, &ts, body, &sig_header, 300);
        assert!(matches!(result, Err(WebhookError::SignatureMismatch)));
    }

    #[test]
    fn test_expired_timestamp() {
        let (secret, key) = make_secret();
        let body = b"body";
        let msg_id = "msg_xyz";
        let old_ts = (chrono::Utc::now().timestamp() - 600).to_string();
        let sig_header = sign_body(&key, msg_id, &old_ts, body);

        let result = verify_webhook(&secret, msg_id, &old_ts, body, &sig_header, 300);
        assert!(matches!(result, Err(WebhookError::TimestampExpired)));
    }

    #[test]
    fn test_multi_signature_rotation() {
        let (secret, key) = make_secret();
        let body = b"rotation test";
        let msg_id = "msg_rot";
        let ts = chrono::Utc::now().timestamp().to_string();

        let valid_sig = sign_body(&key, msg_id, &ts, body);
        // Key rotation: a stale signature first, then the current one.
        let sig_header = format!("v1,aW52YWxpZHNpZ25hdHVyZWhlcmUxMjM0NTY3 {valid_sig}");

        assert!(verify_webhook(&secret, msg_id, &ts, body, &sig_header, 300).is_ok());
    }

    #[test]
    fn test_missing_whsec_prefix() {
        let result = verify_webhook("no_prefix_here", "msg_id", "12345", b"body", "v1,abc", 300);
        assert!(matches!(result, Err(WebhookError::InvalidSecret)));
    }

    #[test]
    fn test_no_v1_signatures() {
        let (secret, _) = make_secret();
        let ts = chrono::Utc::now().timestamp().to_string();

        let result = verify_webhook(&secret, "msg_id", &ts, b"body", "v2,abc", 300);
        assert!(matches!(result, Err(WebhookError::InvalidSignatureFormat)));
    }

    #[test]
    fn test_invalid_base64_secret() {
        let result = verify_webhook(
            "whsec_!!!invalid!!!",
            "msg_id",
            "12345",
            b"body",
            "v1,abc",
            300,
        );
        assert!(matches!(result, Err(WebhookError::InvalidSecret)));
    }

    #[test]
    fn test_out_of_range_secret_lengths_are_rejected() {
        for key in [
            Vec::new(),
            vec![0x41],
            vec![0x42; MIN_WEBHOOK_SECRET_KEY_BYTES - 1],
            vec![0x43; MAX_WEBHOOK_SECRET_KEY_BYTES + 1],
        ] {
            let secret = format!(
                "whsec_{}",
                base64::engine::general_purpose::STANDARD.encode(key)
            );
            assert!(
                matches!(
                    decode_webhook_secret(&secret),
                    Err(WebhookError::InvalidSecret)
                ),
                "a Standard Webhooks secret must decode to 24 through 64 bytes"
            );
        }
    }

    #[test]
    fn test_standard_webhooks_key_length_range_is_accepted() {
        for length in [
            MIN_WEBHOOK_SECRET_KEY_BYTES,
            32,
            MAX_WEBHOOK_SECRET_KEY_BYTES,
        ] {
            let key = vec![0x5a; length];
            let secret = format!(
                "whsec_{}",
                base64::engine::general_purpose::STANDARD.encode(&key)
            );
            assert_eq!(decode_webhook_secret(&secret).unwrap(), key);
        }
    }
}
