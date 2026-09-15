use crate::decode::errors::NetDecodeError;
use crate::decode::{NetDecode, NetDecodeOpts};
use crate::encode::errors::NetEncodeError;
use crate::encode::{NetEncode, NetEncodeOpts};
use crate::net_types::var_int::VarInt;
use crate::net_types::NetTypesError;
use bevy_math::DVec3;
use std::io::{Read, Write};

#[derive(Clone, Copy, Debug, PartialEq)]
pub struct Lpvec3(DVec3);

const MAX_QUANTIZED_VALUE: f64 = 32766.0;
const CONTINUATION_FLAG: u64 = 0x04;
const SCALE_BITS: u64 = 0x03;
const PACKED_COORDINATE_MASK: u64 = 32767;

impl From<DVec3> for Lpvec3 {
    fn from(value: DVec3) -> Self {
        Self(value)
    }
}

impl From<Lpvec3> for DVec3 {
    fn from(value: Lpvec3) -> Self {
        value.0
    }
}

impl Lpvec3 {
    fn pack(value: f64) -> u64 {
        ((value * 0.5 + 0.5) * MAX_QUANTIZED_VALUE).round() as u64
    }

    fn unpack(value: u64) -> f64 {
        ((value & PACKED_COORDINATE_MASK) as f64).min(MAX_QUANTIZED_VALUE) * 2.0
            / MAX_QUANTIZED_VALUE
            - 1.0
    }
}

impl NetDecode for Lpvec3 {
    fn decode<R: Read>(reader: &mut R, _: &NetDecodeOpts) -> Result<Self, NetDecodeError> {
        let mut byte = [0u8; 1];
        reader.read_exact(&mut byte)?;

        let byte1 = byte[0];
        if byte1 == 0 {
            return Ok(Self(DVec3::ZERO));
        }

        reader.read_exact(&mut byte)?;
        let byte2 = byte[0];

        let mut bytes3_to_6 = [0u8; 4];
        reader.read_exact(&mut bytes3_to_6)?;

        let packed = (u64::from(u32::from_be_bytes(bytes3_to_6)) << 16)
            | (u64::from(byte2) << 8)
            | u64::from(byte1);

        let mut scale_factor = u64::from(byte1) & SCALE_BITS;
        if (u64::from(byte1) & CONTINUATION_FLAG) != 0 {
            scale_factor |= u64::from(VarInt::decode(reader, &NetDecodeOpts::None)?.0 as u32) << 2;
        }

        let scale_factor = scale_factor as f64;
        Ok(Self(DVec3::new(
            Self::unpack(packed >> 3) * scale_factor,
            Self::unpack(packed >> 18) * scale_factor,
            Self::unpack(packed >> 33) * scale_factor,
        )))
    }
}

impl NetEncode for Lpvec3 {
    fn encode<W: Write>(&self, writer: &mut W, _: &NetEncodeOpts) -> Result<(), NetEncodeError> {
        let max_coordinate = self.0.x.abs().max(self.0.y.abs()).max(self.0.z.abs());
        if !max_coordinate.is_finite() || max_coordinate < 1.0 / MAX_QUANTIZED_VALUE {
            writer.write_all(&[0])?;
            return Ok(());
        }

        let scale_factor = max_coordinate.ceil() as u64;
        let continuation = scale_factor >> 2;
        if continuation > u64::from(u32::MAX) {
            return Err(NetEncodeError::ExternalError(
                NetTypesError::InvalidInputI32.into(),
            ));
        }

        let need_continuation = (scale_factor & SCALE_BITS) != scale_factor;
        let packed_scale = if need_continuation {
            (scale_factor & SCALE_BITS) | CONTINUATION_FLAG
        } else {
            scale_factor
        };

        let scale_factor = scale_factor as f64;
        let packed = (Self::pack(self.0.x / scale_factor) << 3)
            | (Self::pack(self.0.y / scale_factor) << 18)
            | (Self::pack(self.0.z / scale_factor) << 33)
            | packed_scale;

        writer.write_all(&[packed as u8, (packed >> 8) as u8])?;
        writer.write_all(&((packed >> 16) as u32).to_be_bytes())?;

        if need_continuation {
            VarInt::new(continuation as u32 as i32).encode(writer, &NetEncodeOpts::None)?;
        }

        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::io::Cursor;

    fn round_trip(value: DVec3) -> DVec3 {
        let mut encoded = Vec::new();
        Lpvec3::from(value)
            .encode(&mut encoded, &NetEncodeOpts::None)
            .unwrap();

        let mut cursor = Cursor::new(encoded);
        Lpvec3::decode(&mut cursor, &NetDecodeOpts::None)
            .map(DVec3::from)
            .unwrap()
    }

    fn assert_close(actual: DVec3, expected: DVec3) {
        let error = (actual - expected).abs();
        assert!(error.x < 0.001, "x: {} != {}", actual.x, expected.x);
        assert!(error.y < 0.001, "y: {} != {}", actual.y, expected.y);
        assert!(error.z < 0.001, "z: {} != {}", actual.z, expected.z);
    }

    #[test]
    fn encodes_zero_as_single_byte() {
        let mut encoded = Vec::new();
        Lpvec3::from(DVec3::ZERO)
            .encode(&mut encoded, &NetEncodeOpts::None)
            .unwrap();

        assert_eq!(encoded, [0]);
        assert_eq!(round_trip(DVec3::ZERO), DVec3::ZERO);
    }

    #[test]
    fn round_trips_without_scale_continuation() {
        assert_close(
            round_trip(DVec3::new(0.25, -0.5, 1.0)),
            DVec3::new(0.25, -0.5, 1.0),
        );
    }

    #[test]
    fn round_trips_with_scale_continuation() {
        let value = DVec3::new(10.5, -4.25, 2.75);
        let mut encoded = Vec::new();
        Lpvec3::from(value)
            .encode(&mut encoded, &NetEncodeOpts::None)
            .unwrap();

        assert!(encoded[0] & CONTINUATION_FLAG as u8 != 0);
        assert_close(round_trip(value), value);
    }
}
