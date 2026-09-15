//! HEF on-disk verification — magic-byte check (iter 173) + optional
//! sha256 pin (iter 174).
//!
//! Lives in its own module rather than inline in `hef_pipeline.rs` so
//! the verification code can be unit-tested without the `hailo`
//! feature flag (which requires HailoRT FFI on Pi 5 + AI HAT+, absent
//! on dev hosts). Behavior is unchanged — `HefPipeline::open` calls
//! through here at boot.
//!
//! Iter 198 — extracted + unit-tested.

use crate::error::HailoError;
use std::path::Path;

/// Verify the HEF file at `hef_path` is well-formed.
///
/// 1. Read the first 4 bytes; refuse if they don't match `\x01HEF`.
///    Catches obviously-tampered or wrong-format files before the
///    HailoRT loader gets a chance to crash on them.
/// 2. If `pinned_sha256` is `Some`, stream-hash the entire file with
///    sha2 and compare against the trimmed/lowercased expected digest.
///    Costs ~16 ms on Pi 5 NEON for the 15.7 MB iter-156b HEF (~1 GB/s
///    sustained), well inside the boot budget.
///
/// Both checks return `HailoError::BadModelDir` on mismatch with a
/// distinctive `what:` string so operators can grep the failure.
pub fn verify_hef_header_and_pin(
    hef_path: &Path,
    pinned_sha256: Option<&str>,
) -> Result<(), HailoError> {
    use std::io::Read as _;
    const HEF_MAGIC: [u8; 4] = [0x01, b'H', b'E', b'F'];

    let mut header = [0u8; 4];
    let mut f = std::fs::File::open(hef_path)
        .map_err(|e| HailoError::Tokenizer(format!("open HEF: {}", e)))?;
    f.read_exact(&mut header)
        .map_err(|e| HailoError::Tokenizer(format!("read HEF header: {}", e)))?;
    if header != HEF_MAGIC {
        return Err(HailoError::BadModelDir {
            path: hef_path.display().to_string(),
            what: "model.hef magic mismatch — not a Hailo HEF",
        });
    }
    drop(f);

    if let Some(want) = pinned_sha256 {
        use sha2::{Digest, Sha256};
        let mut f2 = std::fs::File::open(hef_path)
            .map_err(|e| HailoError::Tokenizer(format!("open HEF for sha256: {}", e)))?;
        let mut h = Sha256::new();
        let mut buf = [0u8; 64 * 1024];
        loop {
            let n = f2
                .read(&mut buf)
                .map_err(|e| HailoError::Tokenizer(format!("read HEF for sha256: {}", e)))?;
            if n == 0 {
                break;
            }
            h.update(&buf[..n]);
        }
        let got = format!("{:x}", h.finalize());
        let want_norm = want.trim().to_lowercase();
        if got != want_norm {
            return Err(HailoError::BadModelDir {
                path: hef_path.display().to_string(),
                what: "model.hef sha256 mismatch — RUVECTOR_HEF_SHA256 pin failed",
            });
        }
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    /// Write a 4-byte HEF magic followed by `payload` to a fresh
    /// tempfile under /tmp and return the path. Caller is responsible
    /// for cleanup; tests use unique names.
    fn write_hef_fixture(name: &str, payload: &[u8]) -> std::path::PathBuf {
        use std::io::Write as _;
        let path = std::env::temp_dir().join(format!("iter198-{}.hef", name));
        let mut f = std::fs::File::create(&path).expect("create fixture");
        f.write_all(&[0x01, b'H', b'E', b'F']).expect("write magic");
        f.write_all(payload).expect("write payload");
        f.sync_all().expect("sync");
        path
    }

    fn sha256_of(bytes: &[u8]) -> String {
        use sha2::{Digest, Sha256};
        let mut h = Sha256::new();
        h.update(bytes);
        format!("{:x}", h.finalize())
    }

    #[test]
    fn rejects_non_hef_magic() {
        let path = std::env::temp_dir().join("iter198-bad-magic.hef");
        std::fs::write(&path, b"NOTAHEFFILE").unwrap();
        let err = verify_hef_header_and_pin(&path, None).unwrap_err();
        match err {
            HailoError::BadModelDir { what, .. } => {
                assert!(what.contains("magic mismatch"), "got: {:?}", what);
            }
            other => panic!("expected BadModelDir, got {:?}", other),
        }
        let _ = std::fs::remove_file(&path);
    }

    #[test]
    fn accepts_correct_magic_with_no_pin() {
        let path = write_hef_fixture("ok-no-pin", b"deadbeef");
        verify_hef_header_and_pin(&path, None).expect("no pin = magic-only check");
        let _ = std::fs::remove_file(&path);
    }

    #[test]
    fn rejects_sha256_mismatch() {
        let payload = b"iter198-payload-A";
        let path = write_hef_fixture("sha-mismatch", payload);
        let wrong = "0".repeat(64);
        let err = verify_hef_header_and_pin(&path, Some(&wrong)).unwrap_err();
        match err {
            HailoError::BadModelDir { what, .. } => {
                assert!(
                    what.contains("sha256 mismatch") && what.contains("RUVECTOR_HEF_SHA256"),
                    "got: {:?}",
                    what
                );
            }
            other => panic!("expected BadModelDir, got {:?}", other),
        }
        let _ = std::fs::remove_file(&path);
    }

    #[test]
    fn accepts_matching_sha256() {
        let payload = b"iter198-payload-B-distinct";
        let path = write_hef_fixture("sha-ok", payload);
        let mut full = vec![0x01, b'H', b'E', b'F'];
        full.extend_from_slice(payload);
        let want = sha256_of(&full);
        verify_hef_header_and_pin(&path, Some(&want)).expect("matching sha must succeed");
        let _ = std::fs::remove_file(&path);
    }

    #[test]
    fn normalizes_pin_whitespace_and_case() {
        // Operators sometimes paste a sha256 with stray newlines or in
        // upper-case. iter-174 trim()+to_lowercase() normalization
        // should accept either; lock it in.
        let payload = b"iter198-payload-C";
        let path = write_hef_fixture("sha-norm", payload);
        let mut full = vec![0x01, b'H', b'E', b'F'];
        full.extend_from_slice(payload);
        let want_lc = sha256_of(&full);
        let messy = format!("  \n  {}  \n", want_lc.to_uppercase());
        verify_hef_header_and_pin(&path, Some(&messy))
            .expect("trim + lowercase normalization should accept upper-case + ws");
        let _ = std::fs::remove_file(&path);
    }
}
