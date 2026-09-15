//! Unicode confusable character lookup using data embedded by `build.rs`.

use once_cell::sync::Lazy;
use std::collections::HashMap;

include!(concat!(env!("OUT_DIR"), "/confusables_gen.rs"));

/// Map from confusable char to the ASCII char it resembles.
static CONFUSABLES_MAP: Lazy<HashMap<char, char>> = Lazy::new(|| {
    let mut m = HashMap::with_capacity(CONFUSABLE_COUNT);
    for &(src, tgt) in CONFUSABLE_TABLE {
        if let (Some(s), Some(t)) = (char::from_u32(src), char::from_u32(tgt)) {
            m.insert(s, t);
        }
    }
    m
});

/// Check if a character has a confusable mapping.
pub fn is_confusable(ch: char) -> Option<char> {
    CONFUSABLES_MAP.get(&ch).copied()
}

/// Convert a string to its "skeleton" form by replacing confusables with ASCII equivalents.
pub fn skeleton(s: &str) -> String {
    s.chars()
        .map(|c| CONFUSABLES_MAP.get(&c).copied().unwrap_or(c))
        .collect()
}

/// Check if two strings are confusable (their skeletons match).
pub fn are_confusable(a: &str, b: &str) -> bool {
    skeleton(&a.to_lowercase()) == skeleton(&b.to_lowercase())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_cyrillic_a() {
        assert_eq!(is_confusable('\u{0430}'), Some('a'));
    }

    #[test]
    fn test_ascii_not_confusable() {
        assert_eq!(is_confusable('a'), None);
    }

    #[test]
    fn test_skeleton() {
        // Cyrillic "а" looks like Latin "a"
        assert_eq!(skeleton("\u{0430}"), "a");
    }

    #[test]
    fn test_confusable_domain() {
        // "gіthub.com" with Cyrillic і
        assert!(are_confusable("g\u{0456}thub.com", "github.com"));
    }
}
