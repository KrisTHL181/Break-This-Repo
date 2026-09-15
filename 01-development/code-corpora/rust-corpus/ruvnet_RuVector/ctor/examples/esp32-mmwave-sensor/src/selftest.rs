//! On-device self-test for the MR60BHA2 parser.
//!
//! Runs at boot before the UART loop — feeds synthetic frames through
//! the parser on Xtensa and asserts each one decodes to the expected
//! event. This is the on-device twin of the host `cargo test --bin`
//! cases, exercising the *same* state machine on the *same* CPU that
//! will see real radar bytes.
//!
//! Why bother (since host tests already cover the parser): the host
//! is x86_64 little-endian; Xtensa-LE is also LE but the codegen path
//! is different, and the IDF C runtime / FreeRTOS scheduler can subtly
//! perturb stack alignment in ways that surface as silent corruption
//! in static lookup tables. Running the same fixtures on-device proves
//! the parser is correct in the context where it actually runs.

use ruvector_mmwave::{invert_xor_public, Event, Mr60Parser};

/// Build a synthetic MR60BHA2 frame in `out` and return the slice
/// covering the populated bytes. Mirrors the `frame()` helper used by
/// the host `#[cfg(test)]` cases so on-device + host fixtures are
/// byte-identical.
fn make_frame<'a>(out: &'a mut [u8; 32], frame_type: u16, payload: &[u8]) -> &'a [u8] {
    out[0] = 0x01;
    out[1] = 0x00;
    out[2] = 0x00;
    out[3] = (payload.len() >> 8) as u8;
    out[4] = payload.len() as u8;
    out[5] = (frame_type >> 8) as u8;
    out[6] = frame_type as u8;
    out[7] = invert_xor_public(&out[..7]);
    let mut idx = 8;
    for &b in payload {
        out[idx] = b;
        idx += 1;
    }
    out[idx] = invert_xor_public(payload);
    idx += 1;
    &out[..idx]
}

/// Drive a fresh parser through `bytes` and return the last event it
/// produces. Tests below assert on this final event.
fn final_event(bytes: &[u8]) -> Option<Event> {
    let mut p = Mr60Parser::new();
    let mut last = None;
    for &b in bytes {
        if let Some(ev) = p.feed(b) {
            last = Some(ev);
        }
    }
    last
}

/// Run the suite. Returns `Ok(N)` when N fixtures pass, or `Err(msg)`
/// naming the first failing case.
pub fn run() -> Result<usize, &'static str> {
    let mut buf = [0u8; 32];
    let mut passed = 0;

    // 1. breathing 18 bpm
    let f = make_frame(&mut buf, 0x0A14, &[18]);
    if final_event(f) != Some(Event::Breathing { bpm: 18 }) {
        return Err("breathing fixture mismatch");
    }
    passed += 1;

    // 2. heart rate 72 bpm
    let f = make_frame(&mut buf, 0x0A15, &[72]);
    if final_event(f) != Some(Event::HeartRate { bpm: 72 }) {
        return Err("heart-rate fixture mismatch");
    }
    passed += 1;

    // 3. distance 500 cm (big-endian decode)
    let f = make_frame(&mut buf, 0x0A16, &[0x01, 0xF4]);
    if final_event(f) != Some(Event::Distance { cm: 500 }) {
        return Err("distance BE-decode fixture mismatch");
    }
    passed += 1;

    // 4. presence absent
    let f = make_frame(&mut buf, 0x0F09, &[0]);
    if final_event(f) != Some(Event::Presence { present: false }) {
        return Err("presence-absent fixture mismatch");
    }
    passed += 1;

    // 5. presence present
    let f = make_frame(&mut buf, 0x0F09, &[1]);
    if final_event(f) != Some(Event::Presence { present: true }) {
        return Err("presence-present fixture mismatch");
    }
    passed += 1;

    // 6. unknown frame type — the parser must surface it as Unknown
    // rather than dropping or stalling
    let f = make_frame(&mut buf, 0xBABE, &[0xDE, 0xAD]);
    if final_event(f) != Some(Event::Unknown { frame_type: 0xBABE, payload_len: 2 }) {
        return Err("unknown-type fixture mismatch");
    }
    passed += 1;

    // 7. tampered header checksum — must produce ChecksumError, not
    // silently accept or hang the state machine
    let mut tampered = [0u8; 32];
    let n = {
        let f = make_frame(&mut tampered, 0x0A14, &[18]);
        f.len()
    };
    tampered[7] ^= 0xFF;
    let mut p = Mr60Parser::new();
    let mut saw_chksum_err = false;
    for &b in &tampered[..n] {
        if let Some(ev) = p.feed(b) {
            if matches!(ev, Event::ChecksumError) {
                saw_chksum_err = true;
            }
        }
    }
    if !saw_chksum_err {
        return Err("tampered-header fixture did not surface ChecksumError");
    }
    passed += 1;

    // 8. invert_xor reference value — catches a stack-corruption
    // class bug where a static Crockford-style lookup table gets
    // smashed at boot
    if invert_xor_public(&[0x01, 0x00, 0x00, 0x00, 0x01, 0x0A, 0x14]) != 0xE1 {
        return Err("invert_xor reference fixture mismatch");
    }
    passed += 1;

    Ok(passed)
}
