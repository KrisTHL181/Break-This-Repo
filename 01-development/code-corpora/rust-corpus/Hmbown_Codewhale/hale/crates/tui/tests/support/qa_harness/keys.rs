//! Byte-sequence builders for keys and paste.
//!
//! These produce the raw bytes a real terminal would deliver to the child's
//! PTY slave. They match crossterm's input-decoding tables: legacy sequences
//! by default, CSI-u for the modified Enter chords the TUI opts into, mouse
//! capture off, and bracketed paste on.

/// Plain key press helpers.
pub mod key {
    pub fn ch(c: char) -> Vec<u8> {
        let mut buf = [0u8; 4];
        c.encode_utf8(&mut buf).as_bytes().to_vec()
    }

    pub fn enter() -> Vec<u8> {
        b"\r".to_vec()
    }

    /// Enhanced-keyboard (CSI-u) encodings used by the TUI for modified Enter.
    pub fn shift_enter() -> Vec<u8> {
        b"\x1b[13;2u".to_vec()
    }

    pub fn alt_enter() -> Vec<u8> {
        b"\x1b[13;3u".to_vec()
    }

    pub fn ctrl_enter() -> Vec<u8> {
        b"\x1b[13;5u".to_vec()
    }

    pub fn ctrl_j() -> Vec<u8> {
        vec![0x0a]
    }

    pub fn ctrl_g() -> Vec<u8> {
        vec![0x07]
    }

    pub fn ctrl_s() -> Vec<u8> {
        vec![0x13]
    }

    pub fn esc() -> Vec<u8> {
        vec![0x1b]
    }

    pub fn down() -> Vec<u8> {
        b"\x1b[B".to_vec()
    }

    /// Shift+Tab as the xterm BackTab sequence accepted by crossterm.
    pub fn backtab() -> Vec<u8> {
        b"\x1b[Z".to_vec()
    }

    pub fn page_up() -> Vec<u8> {
        b"\x1b[5~".to_vec()
    }

    pub fn text(s: &str) -> Vec<u8> {
        s.as_bytes().to_vec()
    }

    pub fn alt(c: char) -> Vec<u8> {
        let mut out = vec![0x1b];
        out.extend(ch(c));
        out
    }

    /// `Ctrl+<letter>` as the ASCII control byte a legacy terminal sends.
    /// Panics on a non-alphabetic argument so a typo cannot silently become
    /// a different key.
    pub fn ctrl(c: char) -> Vec<u8> {
        assert!(c.is_ascii_alphabetic(), "ctrl() takes an ASCII letter");
        vec![(c.to_ascii_lowercase() as u8) - b'a' + 1]
    }

    pub fn ctrl_c() -> Vec<u8> {
        vec![0x03]
    }

    pub fn ctrl_d() -> Vec<u8> {
        vec![0x04]
    }

    pub fn tab() -> Vec<u8> {
        b"\t".to_vec()
    }

    /// DEL (0x7f) — what every xterm-family terminal sends for Backspace.
    pub fn backspace() -> Vec<u8> {
        vec![0x7f]
    }

    pub fn backspaces(count: usize) -> Vec<u8> {
        vec![0x7f; count]
    }

    pub fn up() -> Vec<u8> {
        b"\x1b[A".to_vec()
    }

    pub fn right() -> Vec<u8> {
        b"\x1b[C".to_vec()
    }

    pub fn left() -> Vec<u8> {
        b"\x1b[D".to_vec()
    }

    pub fn page_down() -> Vec<u8> {
        b"\x1b[6~".to_vec()
    }

    /// SS3-encoded function keys, the form xterm-family terminals send.
    pub fn f1() -> Vec<u8> {
        b"\x1bOP".to_vec()
    }

    pub fn f2() -> Vec<u8> {
        b"\x1bOQ".to_vec()
    }

    pub fn f4() -> Vec<u8> {
        b"\x1bOS".to_vec()
    }
}

/// Focus-reporting sequences the terminal sends when the window gains or
/// loses focus (DEC private mode 1004). The TUI re-establishes its terminal
/// modes on `FocusGained`, so these are inputs, not decoration.
pub mod focus {
    pub fn gained() -> Vec<u8> {
        b"\x1b[I".to_vec()
    }

    pub fn lost() -> Vec<u8> {
        b"\x1b[O".to_vec()
    }
}

/// SGR mouse sequences use one-based terminal coordinates.
pub mod mouse {
    pub fn down(row: u16, col: u16) -> Vec<u8> {
        format!("\x1b[<0;{};{}M", col + 1, row + 1).into_bytes()
    }

    pub fn drag(row: u16, col: u16) -> Vec<u8> {
        format!("\x1b[<32;{};{}M", col + 1, row + 1).into_bytes()
    }

    pub fn up(row: u16, col: u16) -> Vec<u8> {
        format!("\x1b[<0;{};{}m", col + 1, row + 1).into_bytes()
    }

    pub fn click(row: u16, col: u16) -> Vec<u8> {
        format!(
            "\x1b[<0;{};{}M\x1b[<0;{};{}m",
            col + 1,
            row + 1,
            col + 1,
            row + 1
        )
        .into_bytes()
    }

    pub fn wheel_down(row: u16, col: u16) -> Vec<u8> {
        format!("\x1b[<65;{};{}M", col + 1, row + 1).into_bytes()
    }

    pub fn wheel_up(row: u16, col: u16) -> Vec<u8> {
        format!("\x1b[<64;{};{}M", col + 1, row + 1).into_bytes()
    }
}

/// Bracketed-paste helpers.
///
/// Wraps the payload in `ESC [ 2 0 0 ~` … `ESC [ 2 0 1 ~` so the receiver sees
/// a `crossterm::Event::Paste(text)` rather than a key-by-key stream.
pub mod paste {
    pub fn bracketed(text: &str) -> Vec<u8> {
        let mut out = b"\x1b[200~".to_vec();
        out.extend_from_slice(text.as_bytes());
        out.extend_from_slice(b"\x1b[201~");
        out
    }

    /// Same as [`bracketed`] but does not wrap — simulates a terminal that
    /// has bracketed paste disabled (e.g. some Windows PowerShell setups).
    /// The child sees the bytes as ordinary keystrokes; an embedded `\n`
    /// becomes an Enter press, which is what reproduces #1073.
    pub fn unbracketed(text: &str) -> Vec<u8> {
        text.replace('\n', "\r").as_bytes().to_vec()
    }
}
