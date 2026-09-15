//! Terminal-mode ledger built from the raw PTY output stream.
//!
//! The rendered frame cannot answer "did the TUI put the terminal back the way
//! it found it?" — alternate screen, bracketed paste, mouse capture, focus
//! reporting and the kitty keyboard protocol are all *modes*, and a mode that
//! was enabled and then disabled leaves the screen looking identical either
//! way. The only truthful evidence is the control stream itself.
//!
//! This ledger replays every `CSI ? <params> h|l` (DEC private mode set/reset)
//! in the transcript and records the **last** value seen for each mode number,
//! plus how many times the kitty keyboard stack was pushed (`CSI > <flags> u`)
//! and popped (`CSI < <n> u`).
//!
//! Deliberately vendor-agnostic: it never asserts on the order or grouping
//! crossterm happens to emit today, only on the final state of a mode number,
//! which is what an exiting terminal actually inherits.

use std::collections::BTreeMap;

/// DEC private mode numbers this suite reasons about by name.
pub mod mode {
    /// Cursor visibility (DECTCEM).
    pub const CURSOR_VISIBLE: u16 = 25;
    /// X10-compatible mouse button tracking.
    pub const MOUSE_BUTTON: u16 = 1000;
    /// Button-event (drag) mouse tracking.
    pub const MOUSE_DRAG: u16 = 1002;
    /// Any-event mouse tracking.
    pub const MOUSE_ANY: u16 = 1003;
    /// Focus in / focus out reporting.
    pub const FOCUS: u16 = 1004;
    /// Alternate scroll: wheel events become arrow keys on the alt screen.
    pub const ALTERNATE_SCROLL: u16 = 1007;
    /// urxvt extended mouse coordinates.
    pub const MOUSE_URXVT: u16 = 1015;
    /// SGR extended mouse coordinates.
    pub const MOUSE_SGR: u16 = 1006;
    /// Alternate screen buffer with save/restore cursor.
    pub const ALT_SCREEN: u16 = 1049;
    /// Bracketed paste.
    pub const BRACKETED_PASTE: u16 = 2004;
}

/// Every mode that must be off again once the process has exited, whatever
/// path it exited through. Cursor visibility is asserted separately because
/// its restored value is *on*, not off.
pub const MODES_THAT_MUST_NOT_LEAK: &[(u16, &str)] = &[
    (mode::ALT_SCREEN, "alternate screen"),
    (mode::BRACKETED_PASTE, "bracketed paste"),
    (mode::FOCUS, "focus reporting"),
    (mode::ALTERNATE_SCROLL, "alternate scroll"),
    (mode::MOUSE_BUTTON, "mouse button tracking"),
    (mode::MOUSE_DRAG, "mouse drag tracking"),
    (mode::MOUSE_ANY, "mouse any-event tracking"),
    (mode::MOUSE_SGR, "SGR mouse encoding"),
    (mode::MOUSE_URXVT, "urxvt mouse encoding"),
];

#[derive(Debug, Default, Clone)]
pub struct TerminalModeLedger {
    final_state: BTreeMap<u16, bool>,
    transitions: Vec<(u16, bool)>,
    keyboard_pushes: usize,
    keyboard_pops: usize,
}

impl TerminalModeLedger {
    pub fn from_transcript(bytes: &[u8]) -> Self {
        let mut ledger = Self::default();
        let mut i = 0usize;
        while i < bytes.len() {
            if bytes[i] != 0x1b {
                i += 1;
                continue;
            }
            // Every sequence this ledger cares about is `ESC [ <intro> … <final>`.
            let Some(&b'[') = bytes.get(i + 1) else {
                i += 1;
                continue;
            };
            let intro = match bytes.get(i + 2) {
                Some(&b'?') => Intro::Private,
                Some(&b'>') => Intro::KeyboardPush,
                Some(&b'<') => Intro::KeyboardPop,
                _ => {
                    i += 1;
                    continue;
                }
            };
            let params_start = i + 3;
            let mut cursor = params_start;
            while matches!(bytes.get(cursor), Some(b) if b.is_ascii_digit() || *b == b';') {
                cursor += 1;
            }
            let Some(&terminator) = bytes.get(cursor) else {
                // Truncated tail: the child was killed mid-write. Stop rather
                // than guessing at a sequence that was never completed.
                break;
            };
            let params = &bytes[params_start..cursor];
            match (intro, terminator) {
                (Intro::Private, b'h') | (Intro::Private, b'l') => {
                    let enabled = terminator == b'h';
                    for part in params.split(|b| *b == b';') {
                        if let Some(number) = parse_u16(part) {
                            ledger.final_state.insert(number, enabled);
                            ledger.transitions.push((number, enabled));
                        }
                    }
                }
                (Intro::KeyboardPush, b'u') => ledger.keyboard_pushes += 1,
                (Intro::KeyboardPop, b'u') => ledger.keyboard_pops += 1,
                _ => {}
            }
            i = cursor + 1;
        }
        ledger
    }

    /// Final state of one DEC private mode, or `None` if the transcript never
    /// mentioned it. `None` is not a failure — a terminal that never had a
    /// mode enabled has nothing to restore.
    pub fn state(&self, number: u16) -> Option<bool> {
        self.final_state.get(&number).copied()
    }

    /// Whether the mode was ever enabled at any point in the transcript.
    pub fn was_ever_enabled(&self, number: u16) -> bool {
        self.transitions
            .iter()
            .any(|(mode, enabled)| *mode == number && *enabled)
    }

    pub fn keyboard_pushes(&self) -> usize {
        self.keyboard_pushes
    }

    pub fn keyboard_pops(&self) -> usize {
        self.keyboard_pops
    }

    /// Modes that were switched on and never switched back off. This is the
    /// exact failure the `^[[>5u` shell-pollution reports (#1583) describe.
    pub fn leaked_modes(&self) -> Vec<(u16, &'static str)> {
        MODES_THAT_MUST_NOT_LEAK
            .iter()
            .filter(|(number, _)| self.state(*number) == Some(true))
            .copied()
            .collect()
    }

    /// Human-readable ledger for failure output. Printed next to the frame
    /// dump so a timeout or a leak names the mode instead of the byte offset.
    pub fn debug_dump(&self) -> String {
        let mut out = String::from("== terminal modes ==\n");
        for (number, enabled) in &self.final_state {
            let name = MODES_THAT_MUST_NOT_LEAK
                .iter()
                .find(|(mode, _)| mode == number)
                .map(|(_, name)| *name)
                .unwrap_or(match *number {
                    mode::CURSOR_VISIBLE => "cursor visible",
                    _ => "unclassified",
                });
            out.push_str(&format!(
                "  ?{number:<5} {:<3} ({name})\n",
                if *enabled { "on" } else { "off" }
            ));
        }
        out.push_str(&format!(
            "  keyboard enhancement: {} push / {} pop\n",
            self.keyboard_pushes, self.keyboard_pops
        ));
        out
    }
}

#[derive(Clone, Copy, PartialEq, Eq)]
enum Intro {
    Private,
    KeyboardPush,
    KeyboardPop,
}

fn parse_u16(bytes: &[u8]) -> Option<u16> {
    if bytes.is_empty() {
        return None;
    }
    std::str::from_utf8(bytes).ok()?.parse().ok()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn last_write_wins_per_mode_number() {
        let ledger =
            TerminalModeLedger::from_transcript(b"\x1b[?1049h\x1b[?2004h\x1b[?2004l\x1b[?1049l");

        assert_eq!(ledger.state(mode::ALT_SCREEN), Some(false));
        assert_eq!(ledger.state(mode::BRACKETED_PASTE), Some(false));
        assert!(ledger.was_ever_enabled(mode::BRACKETED_PASTE));
        assert!(ledger.leaked_modes().is_empty());
    }

    #[test]
    fn semicolon_grouped_parameters_each_get_a_state() {
        let ledger = TerminalModeLedger::from_transcript(b"\x1b[?1000;1002;1006h");

        for number in [mode::MOUSE_BUTTON, mode::MOUSE_DRAG, mode::MOUSE_SGR] {
            assert_eq!(ledger.state(number), Some(true));
        }
        assert_eq!(ledger.leaked_modes().len(), 3);
    }

    #[test]
    fn keyboard_enhancement_push_and_pop_are_counted_separately() {
        let ledger = TerminalModeLedger::from_transcript(b"\x1b[>1u\x1b[>1u\x1b[<1u");

        assert_eq!(ledger.keyboard_pushes(), 2);
        assert_eq!(ledger.keyboard_pops(), 1);
    }

    #[test]
    fn unrelated_sgr_and_cursor_sequences_are_ignored() {
        let ledger =
            TerminalModeLedger::from_transcript(b"\x1b[38;2;10;20;30mhello\x1b[2J\x1b[?25l");

        assert_eq!(ledger.state(mode::CURSOR_VISIBLE), Some(false));
        assert_eq!(ledger.state(mode::ALT_SCREEN), None);
    }

    #[test]
    fn a_transcript_truncated_mid_sequence_does_not_panic() {
        let ledger = TerminalModeLedger::from_transcript(b"\x1b[?1049h\x1b[?200");

        assert_eq!(ledger.state(mode::ALT_SCREEN), Some(true));
        assert_eq!(
            ledger.leaked_modes(),
            vec![(mode::ALT_SCREEN, "alternate screen")]
        );
    }
}
