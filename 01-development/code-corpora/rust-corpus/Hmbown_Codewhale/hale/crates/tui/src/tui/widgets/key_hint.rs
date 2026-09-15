//! Platform-aware Alt labels and AltGr disambiguation for terminal shortcuts.
//!
//! Windows keyboard layouts commonly report AltGr as `Ctrl+Alt`. The helpers
//! here keep those glyph-producing events from triggering Ctrl/Alt shortcuts,
//! while preserving the platform-specific label used by hotbar hints.

use crossterm::event::KeyModifiers;

#[cfg(test)]
const ALT_PREFIX: &str = "⌥+";
#[cfg(all(not(test), target_os = "macos"))]
const ALT_PREFIX: &str = "⌥+";
#[cfg(all(not(test), not(target_os = "macos")))]
const ALT_PREFIX: &str = "alt+";

/// Platform-specific prefix for `Alt`-modified chords, matching how the rest
/// of the TUI labels them: `⌥+` on macOS and `alt+` on Linux/Windows.
pub fn alt_prefix() -> &'static str {
    ALT_PREFIX
}

/// `true` if `mods` carries Ctrl or Alt, except for the AltGr Ctrl+Alt
/// combination on Windows.
pub fn has_ctrl_or_alt(mods: KeyModifiers) -> bool {
    (mods.contains(KeyModifiers::CONTROL) || mods.contains(KeyModifiers::ALT)) && !is_altgr(mods)
}

/// On Windows, AltGr is delivered as `Ctrl+Alt`. Crossterm does not expose a
/// portable left-vs-right modifier distinction, so treat that exact pair as
/// AltGr. Other platforms do not need the disambiguation.
#[cfg(windows)]
#[inline]
pub fn is_altgr(mods: KeyModifiers) -> bool {
    mods.contains(KeyModifiers::ALT) && mods.contains(KeyModifiers::CONTROL)
}

#[cfg(not(windows))]
#[inline]
pub fn is_altgr(_mods: KeyModifiers) -> bool {
    false
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn altgr_only_fires_on_windows() {
        let altgr_mods = KeyModifiers::ALT | KeyModifiers::CONTROL;
        if cfg!(windows) {
            assert!(is_altgr(altgr_mods));
            assert!(!has_ctrl_or_alt(altgr_mods));
        } else {
            assert!(!is_altgr(altgr_mods));
            assert!(has_ctrl_or_alt(altgr_mods));
        }
        assert!(!is_altgr(KeyModifiers::ALT));
        assert!(has_ctrl_or_alt(KeyModifiers::ALT));
        assert!(!has_ctrl_or_alt(KeyModifiers::NONE));
    }

    #[test]
    fn alt_prefix_uses_deterministic_test_spelling() {
        assert_eq!(alt_prefix(), "⌥+");
    }
}
