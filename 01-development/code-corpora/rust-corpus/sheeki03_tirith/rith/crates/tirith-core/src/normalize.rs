//! Per-URL-component normalization: decode only unreserved characters
//! (RFC 3986 §2.3 — `A-Z`, `a-z`, `0-9`, `-`, `.`, `_`, `~`). Reserved
//! characters stay percent-encoded so that downstream rules see the same
//! shape regardless of encoding variation.

/// Check if a byte value represents an unreserved character.
fn is_unreserved(byte: u8) -> bool {
    byte.is_ascii_alphanumeric() || matches!(byte, b'-' | b'.' | b'_' | b'~')
}

/// Decode a hex character to its value.
fn hex_val(b: u8) -> Option<u8> {
    match b {
        b'0'..=b'9' => Some(b - b'0'),
        b'a'..=b'f' => Some(b - b'a' + 10),
        b'A'..=b'F' => Some(b - b'A' + 10),
        _ => None,
    }
}

/// Decode only unreserved percent-encoded characters in a string.
/// Returns the normalized string and whether any unreserved chars were decoded.
/// Hex digits in percent-triplets are always normalized to uppercase.
fn decode_unreserved_once(input: &str) -> (String, bool) {
    let bytes = input.as_bytes();
    let mut result = Vec::with_capacity(bytes.len());
    let mut decoded_any = false;
    let mut i = 0;

    while i < bytes.len() {
        if bytes[i] == b'%' && i + 2 < bytes.len() {
            if let (Some(hi), Some(lo)) = (hex_val(bytes[i + 1]), hex_val(bytes[i + 2])) {
                let decoded_byte = (hi << 4) | lo;
                if is_unreserved(decoded_byte) {
                    result.push(decoded_byte);
                    decoded_any = true;
                    i += 3;
                    continue;
                } else {
                    // Keep the triplet encoded but normalize hex to uppercase.
                    result.push(b'%');
                    result.push(bytes[i + 1].to_ascii_uppercase());
                    result.push(bytes[i + 2].to_ascii_uppercase());
                    i += 3;
                    continue;
                }
            }
            // Invalid percent-triplet, leave as-is.
            result.push(bytes[i]);
            i += 1;
        } else {
            result.push(bytes[i]);
            i += 1;
        }
    }

    (String::from_utf8_lossy(&result).into_owned(), decoded_any)
}

/// Normalize a URL path component (decode unreserved chars, up to 3 rounds).
/// Returns (normalized, raw, detected_double_encoding).
pub fn normalize_path(raw: &str) -> NormalizedComponent {
    let mut current = raw.to_string();
    let mut rounds = 0;

    // Always run one pass so hex case gets normalized even when no unreserved
    // bytes are decoded; stop early on fixpoint, but cap at 3 rounds so a
    // pathological input can't spin forever.
    loop {
        let (decoded, did_decode) = decode_unreserved_once(&current);
        current = decoded;
        rounds += 1;
        if !did_decode || rounds >= 3 {
            break;
        }
    }

    let double_encoded = detect_double_encoding(&current);

    NormalizedComponent {
        raw: raw.to_string(),
        normalized: current,
        double_encoded,
        rounds,
    }
}

/// Normalize a query/fragment component (same treatment as path).
pub fn normalize_query(raw: &str) -> NormalizedComponent {
    normalize_path(raw)
}

/// Detect genuine double-encoding: %25XX patterns (percent-encoded percent sign).
fn detect_double_encoding(s: &str) -> bool {
    let bytes = s.as_bytes();
    if bytes.len() < 5 {
        return false;
    }
    let mut i = 0;
    while i + 4 < bytes.len() {
        if bytes[i] == b'%'
            && bytes[i + 1] == b'2'
            && bytes[i + 2] == b'5'
            && hex_val(bytes[i + 3]).is_some()
            && hex_val(bytes[i + 4]).is_some()
        {
            return true;
        }
        i += 1;
    }
    false
}

/// Result of normalization.
#[derive(Debug, Clone)]
pub struct NormalizedComponent {
    pub raw: String,
    pub normalized: String,
    pub double_encoded: bool,
    pub rounds: u32,
}

/// A bounded, security-analysis-only view of a path (or query) component with
/// percent-encoded UTF-8 decoded (repo-0304).
///
/// [`normalize_path`] deliberately decodes only RFC 3986 unreserved bytes, so a
/// percent-encoded non-ASCII byte (e.g. one byte of a Cyrillic homoglyph's
/// UTF-8) stays as ASCII `%XX` and would evade the non-ASCII / homoglyph path
/// rules. This view decodes exactly the triplets that can carry non-ASCII text
/// — bytes >= 0x80 — and only as complete, well-formed UTF-8; encoded ASCII
/// bytes (including encoded separators such as `%2F`) stay encoded, so the
/// view can never gain or lose a structural character. Malformed triplets and
/// truncated or ill-formed UTF-8 are replaced by U+FFFD and set `invalid`, and
/// repeatedly-encoded sequences set `repeated_encoded`, so a conservative
/// caller still flags the component instead of silently skipping it.
#[derive(Debug, Clone)]
pub struct PercentDecodedView {
    /// The analysis text. Equals the input when no decodable triplet and no
    /// invalid sequence was present.
    pub decoded: String,
    /// A percent-triplet was malformed, or the decoded bytes were not complete,
    /// well-formed UTF-8 (each bad sequence is U+FFFD in `decoded`).
    pub invalid: bool,
    /// A repeatedly-encoded sequence (`%25XX`) was observed (left encoded in
    /// `decoded`; flagging is the conservative handling).
    pub repeated_encoded: bool,
}

/// Build the [`PercentDecodedView`] of `raw`. Pure and bounded: the output is
/// never larger than the input plus replacement chars for invalid sequences.
pub fn percent_decoded_view(raw: &str) -> PercentDecodedView {
    let bytes = raw.as_bytes();
    let mut out: Vec<u8> = Vec::with_capacity(bytes.len());
    let mut invalid = false;
    let mut repeated_encoded = false;
    let mut i = 0;

    while i < bytes.len() {
        if bytes[i] == b'%' {
            if i + 2 < bytes.len() {
                if let (Some(hi), Some(lo)) = (hex_val(bytes[i + 1]), hex_val(bytes[i + 2])) {
                    let value = (hi << 4) | lo;
                    if value == b'%'
                        && i + 4 < bytes.len()
                        && hex_val(bytes[i + 3]).is_some()
                        && hex_val(bytes[i + 4]).is_some()
                    {
                        // %25XX: a percent-encoded percent sign introducing a
                        // second encoding layer. Flag it and keep the outer
                        // layer encoded rather than recursively decoding.
                        repeated_encoded = true;
                    }
                    if value >= 0x80 {
                        // A candidate UTF-8 lead/continuation byte: decode it.
                        // Whole-buffer validation below rejects incomplete or
                        // ill-formed sequences (and overlong encodings).
                        out.push(value);
                    } else {
                        // An encoded ASCII byte — possibly a structural char
                        // (separator, `@`, `?`): keep it encoded so the view's
                        // structure matches the encoded component exactly.
                        out.extend_from_slice(&bytes[i..i + 3]);
                    }
                    i += 3;
                    continue;
                }
                // A `%` followed by non-hex: malformed triplet.
                invalid = true;
                out.push(bytes[i]);
                i += 1;
                continue;
            }
            // A trailing `%` without two following bytes: malformed.
            invalid = true;
            out.push(bytes[i]);
            i += 1;
            continue;
        }
        out.push(bytes[i]);
        i += 1;
    }

    // `raw` was valid UTF-8, so any ill-formedness in `out` comes from the
    // decoded high bytes (a lone continuation, a truncated sequence, or an
    // overlong form). Replace each bad maximal subsequence with U+FFFD and
    // flag the component invalid rather than dropping it from analysis.
    let (decoded, invalid_utf8) = match String::from_utf8(out) {
        Ok(s) => (s, false),
        Err(e) => (String::from_utf8_lossy(e.as_bytes()).into_owned(), true),
    };

    PercentDecodedView {
        decoded,
        invalid: invalid || invalid_utf8,
        repeated_encoded,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn view_decodes_complete_utf8_triplets() {
        // %D0%B0 is Cyrillic small A (U+0430) — the classic homoglyph byte pair.
        let view = percent_decoded_view("/inst%D0%B0ll");
        assert_eq!(view.decoded, "/inst\u{0430}ll");
        assert!(!view.invalid);
        assert!(!view.repeated_encoded);
    }

    #[test]
    fn view_keeps_encoded_ascii_structural_chars() {
        // Encoded separators must NOT decode: the view's structure matches the
        // encoded component, so %2F can never become a fake path segment split.
        let view = percent_decoded_view("/a%2Fb%41");
        assert_eq!(view.decoded, "/a%2Fb%41");
        assert!(!view.invalid);
    }

    #[test]
    fn view_flags_malformed_triplets() {
        let view = percent_decoded_view("/x%GGy");
        assert!(view.invalid);
        assert_eq!(view.decoded, "/x%GGy");
        let trailing = percent_decoded_view("/x%4");
        assert!(trailing.invalid);
        let lone = percent_decoded_view("/x%");
        assert!(lone.invalid);
    }

    #[test]
    fn view_flags_truncated_or_illformed_utf8() {
        // A lone lead byte without its continuation: conservatively flagged and
        // replaced by U+FFFD (which the non-ASCII check then sees).
        let view = percent_decoded_view("/x%D0y");
        assert!(view.invalid);
        assert!(view.decoded.contains('\u{FFFD}'));
        // Overlong encoding of '/': rejected, not decoded to a separator.
        let overlong = percent_decoded_view("/x%C0%AFy");
        assert!(overlong.invalid);
        // Both overlong bytes become U+FFFD; no '/' materializes from them.
        assert_eq!(overlong.decoded, "/x\u{FFFD}\u{FFFD}y");
    }

    #[test]
    fn view_flags_repeated_encoding_without_decoding_it() {
        let view = percent_decoded_view("/x%252Fy");
        assert!(view.repeated_encoded);
        // The outer layer stays encoded; no recursive decode happens here.
        assert_eq!(view.decoded, "/x%252Fy");
        assert!(!view.invalid);
    }

    #[test]
    fn view_passes_literal_text_through() {
        let view = percent_decoded_view("/caf\u{00E9}/plain");
        assert_eq!(view.decoded, "/caf\u{00E9}/plain");
        assert!(!view.invalid);
        assert!(!view.repeated_encoded);
    }

    #[test]
    fn test_unreserved_decoded() {
        // %41 = 'A' (unreserved) -> should be decoded
        let result = normalize_path("%41");
        assert_eq!(result.normalized, "A");
    }

    #[test]
    fn test_reserved_preserved() {
        // %2F = '/' (reserved) -> should stay encoded
        let result = normalize_path("%2F");
        assert_eq!(result.normalized, "%2F");
    }

    #[test]
    fn test_reserved_at_preserved() {
        // %40 = '@' (reserved) -> should stay encoded
        let result = normalize_path("%40");
        assert_eq!(result.normalized, "%40");
    }

    #[test]
    fn test_reserved_colon_preserved() {
        // %3A = ':' (reserved) -> should stay encoded
        let result = normalize_path("%3A");
        assert_eq!(result.normalized, "%3A");
    }

    #[test]
    fn test_reserved_question_preserved() {
        // %3F = '?' (reserved) -> should stay encoded
        let result = normalize_path("%3F");
        assert_eq!(result.normalized, "%3F");
    }

    #[test]
    fn test_hex_case_normalized() {
        // %2f (lowercase) -> %2F (uppercase, still reserved)
        let result = normalize_path("%2f");
        assert_eq!(result.normalized, "%2F");
    }

    #[test]
    fn test_double_encoding_detected() {
        // %25 is '%' which is NOT unreserved, so %252F stays as-is after
        // decoding — but the lingering %25 prefix is what flags double-encoding.
        let result = normalize_path("%252F");
        assert!(result.double_encoded);
    }

    #[test]
    fn test_single_level_not_double_encoded() {
        let result = normalize_path("%2F");
        assert!(!result.double_encoded);
    }

    #[test]
    fn test_mixed_encoding() {
        // %41%2F -> A%2F (A decoded, / preserved)
        let result = normalize_path("%41%2F");
        assert_eq!(result.normalized, "A%2F");
    }

    #[test]
    fn test_tilde_decoded() {
        // %7E = '~' (unreserved) -> decoded
        let result = normalize_path("%7E");
        assert_eq!(result.normalized, "~");
    }

    #[test]
    fn test_hyphen_decoded() {
        // %2D = '-' (unreserved) -> decoded
        let result = normalize_path("%2D");
        assert_eq!(result.normalized, "-");
    }

    #[test]
    fn test_dot_decoded() {
        // %2E = '.' (unreserved) -> decoded
        let result = normalize_path("%2E");
        assert_eq!(result.normalized, ".");
    }

    #[test]
    fn test_underscore_decoded() {
        // %5F = '_' (unreserved) -> decoded
        let result = normalize_path("%5F");
        assert_eq!(result.normalized, "_");
    }

    #[test]
    fn test_no_encoding() {
        let result = normalize_path("/path/to/file");
        assert_eq!(result.normalized, "/path/to/file");
        // One pass runs even when nothing was encoded (for hex case normalization).
        assert_eq!(result.rounds, 1);
    }

    #[test]
    fn test_invalid_percent_triplet() {
        // %GG is not valid hex -> left as-is
        let result = normalize_path("%GG");
        assert_eq!(result.normalized, "%GG");
    }

    #[test]
    fn test_multiple_rounds() {
        let result = normalize_path("%2541");
        assert!(result.double_encoded);
    }
}
