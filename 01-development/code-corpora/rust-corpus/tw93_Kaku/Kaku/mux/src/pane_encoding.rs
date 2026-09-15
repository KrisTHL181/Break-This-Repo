use config::keyassignment::PaneEncoding;
use encoding_rs::Encoding;

const MAX_TRAILING_ENCODED_BYTES: usize = 4;

#[derive(Debug, Copy, Clone, PartialEq, Eq)]
enum EscapeState {
    Ground,
    Esc,
    Csi,
    Osc,
    OscEsc,
    Dcs,
    DcsEsc,
}

impl Default for EscapeState {
    fn default() -> Self {
        Self::Ground
    }
}

fn get_encoding(encoding: PaneEncoding) -> Option<&'static Encoding> {
    match encoding {
        PaneEncoding::Utf8 => None,
        PaneEncoding::Gbk => Some(encoding_rs::GBK),
        PaneEncoding::Gb18030 => Some(encoding_rs::GB18030),
        PaneEncoding::Big5 => Some(encoding_rs::BIG5),
        PaneEncoding::EucKr => Some(encoding_rs::EUC_KR),
        PaneEncoding::ShiftJis => Some(encoding_rs::SHIFT_JIS),
    }
}

fn advance_escape(state: EscapeState, byte: u8) -> EscapeState {
    match state {
        EscapeState::Ground => EscapeState::Ground,
        EscapeState::Esc => match byte {
            b'[' => EscapeState::Csi,
            b']' => EscapeState::Osc,
            b'P' => EscapeState::Dcs,
            0x40..=0x7e => EscapeState::Ground,
            _ => EscapeState::Esc,
        },
        EscapeState::Csi => {
            if matches!(byte, 0x40..=0x7e) {
                EscapeState::Ground
            } else {
                EscapeState::Csi
            }
        }
        EscapeState::Osc => match byte {
            0x07 => EscapeState::Ground,
            0x1b => EscapeState::OscEsc,
            _ => EscapeState::Osc,
        },
        EscapeState::OscEsc => {
            if byte == b'\\' {
                EscapeState::Ground
            } else {
                EscapeState::Osc
            }
        }
        EscapeState::Dcs => {
            if byte == 0x1b {
                EscapeState::DcsEsc
            } else {
                EscapeState::Dcs
            }
        }
        EscapeState::DcsEsc => {
            if byte == b'\\' {
                EscapeState::Ground
            } else {
                EscapeState::Dcs
            }
        }
    }
}

fn begin_escape(state: &mut EscapeState, escape_bytes: &mut Vec<u8>, byte: u8) {
    escape_bytes.clear();
    escape_bytes.push(byte);
    *state = if byte == 0x9b {
        EscapeState::Csi
    } else {
        EscapeState::Esc
    };
}

pub fn decode_bytes_to_string(encoding: PaneEncoding, raw: &[u8]) -> String {
    if let Ok(text) = std::str::from_utf8(raw) {
        return text.to_string();
    }

    match get_encoding(encoding) {
        Some(enc) => {
            let (decoded, _, _) = enc.decode(raw);
            decoded.into_owned()
        }
        None => String::from_utf8_lossy(raw).into_owned(),
    }
}

#[derive(Debug)]
pub struct PaneInputEncoder {
    encoding: PaneEncoding,
    state: EscapeState,
    escape_bytes: Vec<u8>,
    pending_utf8: Vec<u8>,
}

impl Default for PaneInputEncoder {
    fn default() -> Self {
        Self {
            encoding: PaneEncoding::Utf8,
            state: EscapeState::Ground,
            escape_bytes: Vec::new(),
            pending_utf8: Vec::new(),
        }
    }
}

impl PaneInputEncoder {
    pub fn encode(&mut self, encoding: PaneEncoding, data: &[u8]) -> Vec<u8> {
        if self.encoding != encoding {
            self.encoding = encoding;
            self.state = EscapeState::Ground;
            self.escape_bytes.clear();
            self.pending_utf8.clear();
        }

        if encoding == PaneEncoding::Utf8 {
            return data.to_vec();
        }

        let mut output = Vec::with_capacity(data.len());
        let mut text_start = 0usize;

        for (idx, &byte) in data.iter().enumerate() {
            if self.state == EscapeState::Ground && (byte == 0x1b || byte == 0x9b) {
                if idx > text_start {
                    self.encode_text(encoding, &data[text_start..idx], &mut output);
                }
                begin_escape(&mut self.state, &mut self.escape_bytes, byte);
                text_start = idx + 1;
                continue;
            }

            if self.state != EscapeState::Ground {
                self.escape_bytes.push(byte);
                self.state = advance_escape(self.state, byte);

                if self.state == EscapeState::Ground {
                    output.extend_from_slice(&self.escape_bytes);
                    self.escape_bytes.clear();
                    text_start = idx + 1;
                }
            }
        }

        if self.state == EscapeState::Ground && text_start < data.len() {
            self.encode_text(encoding, &data[text_start..], &mut output);
        }

        output
    }

    fn encode_text(&mut self, encoding: PaneEncoding, text: &[u8], output: &mut Vec<u8>) {
        let mut pending = std::mem::take(&mut self.pending_utf8);
        pending.extend_from_slice(text);

        let mut cursor = 0usize;
        while cursor < pending.len() {
            match std::str::from_utf8(&pending[cursor..]) {
                Ok(valid) => {
                    self.push_encoded(encoding, valid, output);
                    return;
                }
                Err(err) => {
                    let valid_len = err.valid_up_to();
                    if valid_len > 0 {
                        let valid_slice = &pending[cursor..cursor + valid_len];
                        if let Ok(valid) = std::str::from_utf8(valid_slice) {
                            self.push_encoded(encoding, valid, output);
                        }
                    }

                    cursor += valid_len;
                    if err.error_len().is_none() {
                        self.pending_utf8.extend_from_slice(&pending[cursor..]);
                        return;
                    }

                    output.push(b'?');
                    cursor += err.error_len().unwrap_or(1);
                }
            }
        }
    }

    fn push_encoded(&self, encoding: PaneEncoding, text: &str, output: &mut Vec<u8>) {
        if let Some(enc) = get_encoding(encoding) {
            let (encoded, _, _) = enc.encode(text);
            output.extend_from_slice(&encoded);
        } else {
            output.extend_from_slice(text.as_bytes());
        }
    }
}

#[derive(Debug)]
pub struct PaneOutputDecoder {
    encoding: PaneEncoding,
    state: EscapeState,
    escape_bytes: Vec<u8>,
    pending_encoded: Vec<u8>,
}

impl Default for PaneOutputDecoder {
    fn default() -> Self {
        Self {
            encoding: PaneEncoding::Utf8,
            state: EscapeState::Ground,
            escape_bytes: Vec::new(),
            pending_encoded: Vec::new(),
        }
    }
}

impl PaneOutputDecoder {
    pub fn decode(&mut self, encoding: PaneEncoding, data: &[u8]) -> Vec<u8> {
        if self.encoding != encoding {
            self.encoding = encoding;
            self.state = EscapeState::Ground;
            self.escape_bytes.clear();
            self.pending_encoded.clear();
        }

        if encoding == PaneEncoding::Utf8 {
            return data.to_vec();
        }

        let mut output = Vec::with_capacity(data.len());
        let mut text_start = 0usize;

        for (idx, &byte) in data.iter().enumerate() {
            if self.state == EscapeState::Ground && (byte == 0x1b || byte == 0x9b) {
                if idx > text_start {
                    self.decode_text(encoding, &data[text_start..idx], &mut output);
                }
                begin_escape(&mut self.state, &mut self.escape_bytes, byte);
                text_start = idx + 1;
                continue;
            }

            if self.state != EscapeState::Ground {
                self.escape_bytes.push(byte);
                self.state = advance_escape(self.state, byte);
                if self.state == EscapeState::Ground {
                    output.extend_from_slice(&self.escape_bytes);
                    self.escape_bytes.clear();
                    text_start = idx + 1;
                }
            }
        }

        if self.state == EscapeState::Ground && text_start < data.len() {
            self.decode_text(encoding, &data[text_start..], &mut output);
        }

        output
    }

    fn decode_text(&mut self, encoding: PaneEncoding, input: &[u8], output: &mut Vec<u8>) {
        let mut pending = std::mem::take(&mut self.pending_encoded);
        pending.extend_from_slice(input);

        let Some(enc) = get_encoding(encoding) else {
            output.extend_from_slice(&pending);
            return;
        };

        let min_prefix = pending
            .len()
            .saturating_sub(MAX_TRAILING_ENCODED_BYTES)
            .max(1);

        for split in (min_prefix..=pending.len()).rev() {
            if let Some(decoded) =
                enc.decode_without_bom_handling_and_without_replacement(&pending[..split])
            {
                output.extend_from_slice(decoded.as_bytes());
                if split < pending.len() {
                    self.pending_encoded.extend_from_slice(&pending[split..]);
                }
                return;
            }
        }

        if pending.len() <= MAX_TRAILING_ENCODED_BYTES {
            self.pending_encoded.extend_from_slice(&pending);
            return;
        }

        let (decoded, _, _) = enc.decode(&pending);
        output.extend_from_slice(decoded.as_bytes());
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn round_trip_text(encoding: PaneEncoding, text: &str) {
        let mut encoder = PaneInputEncoder::default();
        let mut decoder = PaneOutputDecoder::default();
        let encoded = encoder.encode(encoding, text.as_bytes());
        let decoded = decoder.decode(encoding, &encoded);
        assert_eq!(decoded, text.as_bytes().to_vec());
    }

    #[test]
    fn utf8_passthrough() {
        let mut encoder = PaneInputEncoder::default();
        let mut decoder = PaneOutputDecoder::default();
        let data = "hello world".as_bytes();

        assert_eq!(encoder.encode(PaneEncoding::Utf8, data), data.to_vec());
        assert_eq!(decoder.decode(PaneEncoding::Utf8, data), data.to_vec());
    }

    #[test]
    fn supports_all_encodings_roundtrip() {
        round_trip_text(PaneEncoding::Gbk, "你好");
        round_trip_text(PaneEncoding::Gb18030, "你好世界");
        round_trip_text(PaneEncoding::Big5, "繁體中文");
        round_trip_text(PaneEncoding::EucKr, "안녕하세요");
        round_trip_text(PaneEncoding::ShiftJis, "こんにちは");
    }

    #[test]
    fn preserves_csi_esc_bracket_sequences() {
        let mut decoder = PaneOutputDecoder::default();
        let bytes = b"\x1b[31m";
        assert_eq!(decoder.decode(PaneEncoding::Gbk, bytes), bytes.to_vec());
    }

    #[test]
    fn preserves_csi_single_byte_sequences() {
        let mut decoder = PaneOutputDecoder::default();
        let bytes = [0x9b, b'3', b'1', b'm'];
        assert_eq!(decoder.decode(PaneEncoding::Gbk, &bytes), bytes.to_vec());
    }

    #[test]
    fn preserves_osc_and_dcs_sequences() {
        let mut decoder = PaneOutputDecoder::default();
        let osc = b"\x1b]0;title\x07";
        let dcs = b"\x1bPpayload\x1b\\";

        assert_eq!(decoder.decode(PaneEncoding::Gbk, osc), osc.to_vec());
        assert_eq!(decoder.decode(PaneEncoding::Gbk, dcs), dcs.to_vec());
    }

    #[test]
    fn mixed_text_and_escape_decode() {
        let mut decoder = PaneOutputDecoder::default();

        let mut data = vec![0xc4, 0xe3];
        data.extend_from_slice(b"\x1b[0m");
        data.extend_from_slice(&[0xba, 0xc3]);

        let result = decoder.decode(PaneEncoding::Gbk, &data);
        let mut expected = "你".as_bytes().to_vec();
        expected.extend_from_slice(b"\x1b[0m");
        expected.extend_from_slice("好".as_bytes());
        assert_eq!(result, expected);
    }

    #[test]
    fn split_multibyte_decode_is_buffered() {
        let mut decoder = PaneOutputDecoder::default();

        let part1 = [0xc4];
        let result1 = decoder.decode(PaneEncoding::Gbk, &part1);
        assert!(result1.is_empty());

        let part2 = [0xe3];
        let result2 = decoder.decode(PaneEncoding::Gbk, &part2);
        assert_eq!(result2, "你".as_bytes().to_vec());
    }

    #[test]
    fn split_multibyte_encode_is_buffered() {
        let mut encoder = PaneInputEncoder::default();

        let first = [0xe4];
        let result1 = encoder.encode(PaneEncoding::Gbk, &first);
        assert!(result1.is_empty());

        let second = [0xbd, 0xa0];
        let result2 = encoder.encode(PaneEncoding::Gbk, &second);
        assert_eq!(result2, vec![0xc4, 0xe3]);
    }

    #[test]
    fn decode_bytes_to_string_works_for_utf8_and_non_utf8() {
        let utf8 = decode_bytes_to_string(PaneEncoding::Utf8, "hello世界".as_bytes());
        assert_eq!(utf8, "hello世界".to_string());

        let gbk_bytes = [0xc4, 0xe3, 0xba, 0xc3];
        let text = decode_bytes_to_string(PaneEncoding::Gbk, &gbk_bytes);
        assert_eq!(text, "你好".to_string());
    }

    #[test]
    fn preserves_escape_sequences_all_encodings() {
        let encodings = [
            PaneEncoding::Gbk,
            PaneEncoding::Gb18030,
            PaneEncoding::Big5,
            PaneEncoding::EucKr,
            PaneEncoding::ShiftJis,
        ];
        let csi = b"\x1b[31m";
        let osc = b"\x1b]0;title\x07";
        let dcs = b"\x1bPpayload\x1b\\";
        let csi_9b = [0x9b, b'3', b'1', b'm'];

        for enc in encodings {
            let mut decoder = PaneOutputDecoder::default();
            assert_eq!(decoder.decode(enc, csi), csi.to_vec(), "{enc:?} CSI");
            let mut decoder = PaneOutputDecoder::default();
            assert_eq!(decoder.decode(enc, osc), osc.to_vec(), "{enc:?} OSC");
            let mut decoder = PaneOutputDecoder::default();
            assert_eq!(decoder.decode(enc, dcs), dcs.to_vec(), "{enc:?} DCS");
            let mut decoder = PaneOutputDecoder::default();
            assert_eq!(
                decoder.decode(enc, &csi_9b),
                csi_9b.to_vec(),
                "{enc:?} CSI 0x9b"
            );
        }
    }

    #[test]
    fn mixed_text_and_escape_all_encodings() {
        // GBK: "你" = 0xc4e3, "好" = 0xbac3
        {
            let mut decoder = PaneOutputDecoder::default();
            let mut data = vec![0xc4, 0xe3];
            data.extend_from_slice(b"\x1b[0m");
            data.extend_from_slice(&[0xba, 0xc3]);
            let result = decoder.decode(PaneEncoding::Gbk, &data);
            let mut expected = "你".as_bytes().to_vec();
            expected.extend_from_slice(b"\x1b[0m");
            expected.extend_from_slice("好".as_bytes());
            assert_eq!(result, expected, "GBK mixed");
        }
        // Big5: "你" = 0xa741, "好" = 0xa66e
        {
            let mut decoder = PaneOutputDecoder::default();
            let mut data = vec![0xa7, 0x41];
            data.extend_from_slice(b"\x1b[0m");
            data.extend_from_slice(&[0xa6, 0x6e]);
            let result = decoder.decode(PaneEncoding::Big5, &data);
            let mut expected = "你".as_bytes().to_vec();
            expected.extend_from_slice(b"\x1b[0m");
            expected.extend_from_slice("好".as_bytes());
            assert_eq!(result, expected, "Big5 mixed");
        }
        // EUC-KR: "안" = 0xbec8, "녕" = 0xb3e7
        {
            let mut decoder = PaneOutputDecoder::default();
            let mut data = vec![0xbe, 0xc8];
            data.extend_from_slice(b"\x1b[0m");
            data.extend_from_slice(&[0xb3, 0xe7]);
            let result = decoder.decode(PaneEncoding::EucKr, &data);
            let mut expected = "안".as_bytes().to_vec();
            expected.extend_from_slice(b"\x1b[0m");
            expected.extend_from_slice("녕".as_bytes());
            assert_eq!(result, expected, "EUC-KR mixed");
        }
        // Shift-JIS: "こ" = 0x82b1, "ん" = 0x82f1
        {
            let mut decoder = PaneOutputDecoder::default();
            let mut data = vec![0x82, 0xb1];
            data.extend_from_slice(b"\x1b[0m");
            data.extend_from_slice(&[0x82, 0xf1]);
            let result = decoder.decode(PaneEncoding::ShiftJis, &data);
            let mut expected = "こ".as_bytes().to_vec();
            expected.extend_from_slice(b"\x1b[0m");
            expected.extend_from_slice("ん".as_bytes());
            assert_eq!(result, expected, "Shift-JIS mixed");
        }
        // GB18030: "你" = 0xc4e3, "好" = 0xbac3 (same as GBK for BMP)
        {
            let mut decoder = PaneOutputDecoder::default();
            let mut data = vec![0xc4, 0xe3];
            data.extend_from_slice(b"\x1b[0m");
            data.extend_from_slice(&[0xba, 0xc3]);
            let result = decoder.decode(PaneEncoding::Gb18030, &data);
            let mut expected = "你".as_bytes().to_vec();
            expected.extend_from_slice(b"\x1b[0m");
            expected.extend_from_slice("好".as_bytes());
            assert_eq!(result, expected, "GB18030 mixed");
        }
    }

    #[test]
    fn split_multibyte_decode_all_encodings() {
        // GBK: "你" = 0xc4 e3
        {
            let mut decoder = PaneOutputDecoder::default();
            assert!(decoder.decode(PaneEncoding::Gbk, &[0xc4]).is_empty());
            assert_eq!(
                decoder.decode(PaneEncoding::Gbk, &[0xe3]),
                "你".as_bytes().to_vec()
            );
        }
        // Big5: "你" = 0xa7 41
        {
            let mut decoder = PaneOutputDecoder::default();
            assert!(decoder.decode(PaneEncoding::Big5, &[0xa7]).is_empty());
            assert_eq!(
                decoder.decode(PaneEncoding::Big5, &[0x41]),
                "你".as_bytes().to_vec()
            );
        }
        // EUC-KR: "안" = 0xbe c8
        {
            let mut decoder = PaneOutputDecoder::default();
            assert!(decoder.decode(PaneEncoding::EucKr, &[0xbe]).is_empty());
            assert_eq!(
                decoder.decode(PaneEncoding::EucKr, &[0xc8]),
                "안".as_bytes().to_vec()
            );
        }
        // Shift-JIS: "こ" = 0x82 b1
        {
            let mut decoder = PaneOutputDecoder::default();
            assert!(decoder.decode(PaneEncoding::ShiftJis, &[0x82]).is_empty());
            assert_eq!(
                decoder.decode(PaneEncoding::ShiftJis, &[0xb1]),
                "こ".as_bytes().to_vec()
            );
        }
        // GB18030 2-byte: "你" = 0xc4 e3
        {
            let mut decoder = PaneOutputDecoder::default();
            assert!(decoder.decode(PaneEncoding::Gb18030, &[0xc4]).is_empty());
            assert_eq!(
                decoder.decode(PaneEncoding::Gb18030, &[0xe3]),
                "你".as_bytes().to_vec()
            );
        }
    }

    #[test]
    fn split_multibyte_encode_all_encodings() {
        // "你" in UTF-8 is 0xe4 0xbd 0xa0
        // GBK: "你" = 0xc4 e3
        {
            let mut encoder = PaneInputEncoder::default();
            assert!(encoder.encode(PaneEncoding::Gbk, &[0xe4]).is_empty());
            assert_eq!(
                encoder.encode(PaneEncoding::Gbk, &[0xbd, 0xa0]),
                vec![0xc4, 0xe3]
            );
        }
        // Big5: "你" = 0xa741
        {
            let mut encoder = PaneInputEncoder::default();
            assert!(encoder.encode(PaneEncoding::Big5, &[0xe4]).is_empty());
            assert_eq!(
                encoder.encode(PaneEncoding::Big5, &[0xbd, 0xa0]),
                vec![0xa7, 0x41]
            );
        }
        // "안" in UTF-8 is 0xec 0x95 0x88
        // EUC-KR: "안" = 0xbe c8
        {
            let mut encoder = PaneInputEncoder::default();
            assert!(encoder.encode(PaneEncoding::EucKr, &[0xec]).is_empty());
            assert_eq!(
                encoder.encode(PaneEncoding::EucKr, &[0x95, 0x88]),
                vec![0xbe, 0xc8]
            );
        }
        // "こ" in UTF-8 is 0xe3 0x81 0x93
        // Shift-JIS: "こ" = 0x82 b1
        {
            let mut encoder = PaneInputEncoder::default();
            assert!(encoder.encode(PaneEncoding::ShiftJis, &[0xe3]).is_empty());
            assert_eq!(
                encoder.encode(PaneEncoding::ShiftJis, &[0x81, 0x93]),
                vec![0x82, 0xb1]
            );
        }
        // GB18030 2-byte: "你" = 0xc4 e3
        {
            let mut encoder = PaneInputEncoder::default();
            assert!(encoder.encode(PaneEncoding::Gb18030, &[0xe4]).is_empty());
            assert_eq!(
                encoder.encode(PaneEncoding::Gb18030, &[0xbd, 0xa0]),
                vec![0xc4, 0xe3]
            );
        }
    }

    #[test]
    fn gb18030_four_byte_sequence() {
        // U+20000 (𠀀) in GB18030 = 0x95 0x32 0x82 0x36
        // In UTF-8: 0xf0 0xa0 0x80 0x80
        let utf8_bytes = "\u{20000}".as_bytes();
        let gb18030_bytes = [0x95, 0x32, 0x82, 0x36];

        // Encode: UTF-8 → GB18030
        let mut encoder = PaneInputEncoder::default();
        let encoded = encoder.encode(PaneEncoding::Gb18030, utf8_bytes);
        assert_eq!(encoded, gb18030_bytes.to_vec(), "GB18030 4-byte encode");

        // Decode: GB18030 → UTF-8
        let mut decoder = PaneOutputDecoder::default();
        let decoded = decoder.decode(PaneEncoding::Gb18030, &gb18030_bytes);
        assert_eq!(decoded, utf8_bytes.to_vec(), "GB18030 4-byte decode");

        // Split decode: feed one byte at a time
        let mut decoder = PaneOutputDecoder::default();
        assert!(decoder.decode(PaneEncoding::Gb18030, &[0x95]).is_empty());
        assert!(decoder.decode(PaneEncoding::Gb18030, &[0x32]).is_empty());
        assert!(decoder.decode(PaneEncoding::Gb18030, &[0x82]).is_empty());
        assert_eq!(
            decoder.decode(PaneEncoding::Gb18030, &[0x36]),
            utf8_bytes.to_vec(),
            "GB18030 4-byte split decode"
        );
    }

    #[test]
    fn encoding_switch_resets_decoder_state() {
        let mut decoder = PaneOutputDecoder::default();

        // Start decoding GBK, feed first byte of a 2-byte char
        let result1 = decoder.decode(PaneEncoding::Gbk, &[0xc4]);
        assert!(result1.is_empty(), "GBK first byte buffered");

        // Switch to Shift-JIS - should reset, not carry over partial GBK byte
        let result2 = decoder.decode(PaneEncoding::ShiftJis, &[0x82, 0xb1]);
        assert_eq!(result2, "こ".as_bytes().to_vec(), "Shift-JIS after switch");
    }

    #[test]
    fn encoding_switch_resets_encoder_state() {
        let mut encoder = PaneInputEncoder::default();

        // Start encoding for GBK, feed first byte of "你" in UTF-8
        let result1 = encoder.encode(PaneEncoding::Gbk, &[0xe4]);
        assert!(result1.is_empty(), "GBK encoder first byte buffered");

        // Switch to Shift-JIS - should reset pending UTF-8 bytes
        let result2 = encoder.encode(PaneEncoding::ShiftJis, &[0xe3, 0x81, 0x93]);
        assert_eq!(result2, vec![0x82, 0xb1], "Shift-JIS encode after switch");
    }

    #[test]
    fn decode_bytes_to_string_all_encodings() {
        assert_eq!(
            decode_bytes_to_string(PaneEncoding::Utf8, "hello世界".as_bytes()),
            "hello世界"
        );
        assert_eq!(
            decode_bytes_to_string(PaneEncoding::Gbk, &[0xc4, 0xe3, 0xba, 0xc3]),
            "你好"
        );
        assert_eq!(
            decode_bytes_to_string(PaneEncoding::Gb18030, &[0xc4, 0xe3, 0xba, 0xc3]),
            "你好"
        );
        assert_eq!(
            decode_bytes_to_string(PaneEncoding::Big5, &[0xa7, 0x41, 0xa6, 0x6e]),
            "你好"
        );
        assert_eq!(
            decode_bytes_to_string(PaneEncoding::EucKr, &[0xbe, 0xc8, 0xb3, 0xe7]),
            "안녕"
        );
        assert_eq!(
            decode_bytes_to_string(PaneEncoding::ShiftJis, &[0x82, 0xb1, 0x82, 0xf1]),
            "こん"
        );
    }

    #[test]
    fn ascii_passthrough_all_encodings() {
        let ascii = b"Hello, World! 123";
        let encodings = [
            PaneEncoding::Utf8,
            PaneEncoding::Gbk,
            PaneEncoding::Gb18030,
            PaneEncoding::Big5,
            PaneEncoding::EucKr,
            PaneEncoding::ShiftJis,
        ];
        for enc in encodings {
            let mut encoder = PaneInputEncoder::default();
            let mut decoder = PaneOutputDecoder::default();
            assert_eq!(
                encoder.encode(enc, ascii),
                ascii.to_vec(),
                "{enc:?} encode ASCII"
            );
            assert_eq!(
                decoder.decode(enc, ascii),
                ascii.to_vec(),
                "{enc:?} decode ASCII"
            );
        }
    }
}
