//! Independent typed secondary-value oracle.
//!
//! This module deliberately owns its numeric decomposition and physical byte
//! contracts. It imports observation DTOs only, never production comparison or
//! encoding code.

use db::migration_parity::ParityValue;
use sha2::{Digest, Sha256};

const EQUALITY_DIGEST_LEN: usize = core::mem::size_of::<u64>();
const MAX_EQUALITY_CANONICAL_LEN: usize = 1024 * 1024 - 64;
const MAX_RANGE_ENCODED_LEN: usize = 1024 * 1024 - 32;

const BOOL_TAG: u8 = 0x01;
const NUMBER_TAG: u8 = 0x02;
const DATETIME_TAG: u8 = 0x03;
const STRING_TAG: u8 = 0x04;
const BYTES_TAG: u8 = 0x05;
const I64_ARRAY_TAG: u8 = 0x06;
const F64_ARRAY_TAG: u8 = 0x07;
const F32_ARRAY_TAG: u8 = 0x08;
const STRING_ARRAY_TAG: u8 = 0x09;

const NEGATIVE_INFINITY_TAG: u8 = 0x01;
const NEGATIVE_FINITE_TAG: u8 = 0x02;
const ZERO_TAG: u8 = 0x03;
const POSITIVE_FINITE_TAG: u8 = 0x04;
const POSITIVE_INFINITY_TAG: u8 = 0x05;

const NUMERIC_DOMAIN: u8 = 0x01;
const DATETIME_DOMAIN: u8 = 0x02;
const STRING_DOMAIN: u8 = 0x03;
const EXPONENT_BIAS: i32 = 1 << 15;
const STRING_ESCAPE: u8 = 0xFF;
const STRING_TERMINATOR: u8 = 0x00;

#[derive(Debug, Clone, PartialEq, Eq)]
pub(crate) enum EqualityProjection {
    Indexed {
        digest: [u8; EQUALITY_DIGEST_LEN],
        canonical: Vec<u8>,
    },
    Absent,
    Unsupported(&'static str),
    Oversized {
        encoded_len: usize,
        maximum: usize,
    },
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum RangeDirection {
    Ascending,
    Descending,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub(crate) enum RangeProjection {
    Indexed(Vec<u8>),
    NaN,
    Unsupported(&'static str),
    Oversized { encoded_len: usize, maximum: usize },
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
struct ExactFinite {
    exponent: i16,
    odd_significand: u64,
    floor_log2: i16,
    normalized_significand: u64,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum ExactNumber {
    NegativeInfinity,
    NegativeFinite(ExactFinite),
    Zero,
    PositiveFinite(ExactFinite),
    PositiveInfinity,
}

pub(crate) fn project_equality(value: &ParityValue) -> EqualityProjection {
    let mut canonical = Vec::new();
    let projection = match value {
        ParityValue::Null => return EqualityProjection::Absent,
        ParityValue::Bool(value) => {
            canonical.push(BOOL_TAG);
            canonical.push(u8::from(*value));
            Some(())
        }
        ParityValue::I64(_) | ParityValue::F64Bits(_) | ParityValue::F32Bits(_) => {
            let Some(number) = exact_number(value) else {
                return EqualityProjection::Absent;
            };
            canonical.push(NUMBER_TAG);
            put_equality_number(&mut canonical, number);
            Some(())
        }
        ParityValue::DateTime(value) => {
            canonical.push(DATETIME_TAG);
            canonical.extend_from_slice(&value.to_be_bytes());
            Some(())
        }
        ParityValue::String(value) => {
            canonical.push(STRING_TAG);
            put_length_delimited(&mut canonical, value.as_bytes())
        }
        ParityValue::Bytes(value) => {
            canonical.push(BYTES_TAG);
            put_length_delimited(&mut canonical, value)
        }
        ParityValue::I64Array(values) => {
            canonical.push(I64_ARRAY_TAG);
            put_count(&mut canonical, values.len()).map(|()| {
                values
                    .iter()
                    .for_each(|value| canonical.extend_from_slice(&value.to_be_bytes()));
            })
        }
        ParityValue::F64ArrayBits(values) => {
            canonical.push(F64_ARRAY_TAG);
            let Some(()) = put_count(&mut canonical, values.len()) else {
                return oversized_equality(values.len());
            };
            for bits in values {
                let Some(number) = exact_f64(f64::from_bits(*bits)) else {
                    return EqualityProjection::Absent;
                };
                put_equality_number(&mut canonical, number);
            }
            Some(())
        }
        ParityValue::F32ArrayBits(values) => {
            canonical.push(F32_ARRAY_TAG);
            let Some(()) = put_count(&mut canonical, values.len()) else {
                return oversized_equality(values.len());
            };
            for bits in values {
                let Some(number) = exact_f32(f32::from_bits(*bits)) else {
                    return EqualityProjection::Absent;
                };
                put_equality_number(&mut canonical, number);
            }
            Some(())
        }
        ParityValue::StringArray(values) => {
            canonical.push(STRING_ARRAY_TAG);
            let Some(()) = put_count(&mut canonical, values.len()) else {
                return oversized_equality(values.len());
            };
            for value in values {
                if put_length_delimited(&mut canonical, value.as_bytes()).is_none() {
                    return oversized_equality(value.len());
                }
            }
            Some(())
        }
        ParityValue::Array(_) => return EqualityProjection::Unsupported("Array"),
        ParityValue::Object(_) => return EqualityProjection::Unsupported("Object"),
    };
    if projection.is_none() || canonical.len() > MAX_EQUALITY_CANONICAL_LEN {
        return oversized_equality(canonical.len());
    }
    let hash = Sha256::digest(&canonical);
    EqualityProjection::Indexed {
        digest: hash[..EQUALITY_DIGEST_LEN]
            .try_into()
            .expect("SHA-256 contains an eight-byte digest prefix"),
        canonical,
    }
}

pub(crate) fn project_range(value: &ParityValue, direction: RangeDirection) -> RangeProjection {
    let mut encoded = Vec::new();
    match value {
        ParityValue::I64(_) | ParityValue::F64Bits(_) | ParityValue::F32Bits(_) => {
            let Some(number) = exact_number(value) else {
                return RangeProjection::NaN;
            };
            encoded.push(NUMERIC_DOMAIN);
            put_ordered_number(&mut encoded, number);
        }
        ParityValue::DateTime(value) => {
            encoded.push(DATETIME_DOMAIN);
            encoded.extend_from_slice(&((*value as u64) ^ (1_u64 << 63)).to_be_bytes());
        }
        ParityValue::String(value) => {
            encoded.push(STRING_DOMAIN);
            for byte in value.as_bytes() {
                if *byte == STRING_TERMINATOR {
                    encoded.push(STRING_TERMINATOR);
                    encoded.push(STRING_ESCAPE);
                } else {
                    encoded.push(*byte);
                }
            }
            encoded.push(STRING_TERMINATOR);
            encoded.push(STRING_TERMINATOR);
        }
        ParityValue::Null => return RangeProjection::Unsupported("Null"),
        ParityValue::Bool(_) => return RangeProjection::Unsupported("Bool"),
        ParityValue::Bytes(_) => return RangeProjection::Unsupported("Bytes"),
        ParityValue::I64Array(_) => return RangeProjection::Unsupported("I64Array"),
        ParityValue::F64ArrayBits(_) => return RangeProjection::Unsupported("F64Array"),
        ParityValue::F32ArrayBits(_) => return RangeProjection::Unsupported("F32Array"),
        ParityValue::StringArray(_) => return RangeProjection::Unsupported("StringArray"),
        ParityValue::Array(_) => return RangeProjection::Unsupported("Array"),
        ParityValue::Object(_) => return RangeProjection::Unsupported("Object"),
    }
    if encoded.len() > MAX_RANGE_ENCODED_LEN {
        return RangeProjection::Oversized {
            encoded_len: encoded.len(),
            maximum: MAX_RANGE_ENCODED_LEN,
        };
    }
    if direction == RangeDirection::Descending {
        encoded.iter_mut().for_each(|byte| *byte = !*byte);
    }
    RangeProjection::Indexed(encoded)
}

fn exact_number(value: &ParityValue) -> Option<ExactNumber> {
    match value {
        ParityValue::I64(value) => Some(exact_i64(*value)),
        ParityValue::F64Bits(bits) => exact_f64(f64::from_bits(*bits)),
        ParityValue::F32Bits(bits) => exact_f32(f64::from_bits(*bits) as f32),
        ParityValue::Null
        | ParityValue::Bool(_)
        | ParityValue::DateTime(_)
        | ParityValue::String(_)
        | ParityValue::Bytes(_)
        | ParityValue::I64Array(_)
        | ParityValue::F64ArrayBits(_)
        | ParityValue::F32ArrayBits(_)
        | ParityValue::StringArray(_)
        | ParityValue::Array(_)
        | ParityValue::Object(_) => None,
    }
}

fn exact_i64(value: i64) -> ExactNumber {
    if value == 0 {
        ExactNumber::Zero
    } else {
        exact_finite(value.is_negative(), value.unsigned_abs(), 0)
    }
}

fn exact_f64(value: f64) -> Option<ExactNumber> {
    let bits = value.to_bits();
    let negative = bits >> 63 != 0;
    let exponent_bits = ((bits >> 52) & 0x7FF) as i16;
    let fraction = bits & ((1_u64 << 52) - 1);
    if exponent_bits == 0x7FF {
        return (fraction == 0).then_some(if negative {
            ExactNumber::NegativeInfinity
        } else {
            ExactNumber::PositiveInfinity
        });
    }
    if exponent_bits == 0 && fraction == 0 {
        return Some(ExactNumber::Zero);
    }
    let (significand, exponent) = if exponent_bits == 0 {
        (fraction, -1074)
    } else {
        ((1_u64 << 52) | fraction, exponent_bits - 1023 - 52)
    };
    Some(exact_finite(negative, significand, exponent))
}

fn exact_f32(value: f32) -> Option<ExactNumber> {
    let bits = value.to_bits();
    let negative = bits >> 31 != 0;
    let exponent_bits = ((bits >> 23) & 0xFF) as i16;
    let fraction = u64::from(bits & ((1_u32 << 23) - 1));
    if exponent_bits == 0xFF {
        return (fraction == 0).then_some(if negative {
            ExactNumber::NegativeInfinity
        } else {
            ExactNumber::PositiveInfinity
        });
    }
    if exponent_bits == 0 && fraction == 0 {
        return Some(ExactNumber::Zero);
    }
    let (significand, exponent) = if exponent_bits == 0 {
        (fraction, -149)
    } else {
        ((1_u64 << 23) | fraction, exponent_bits - 127 - 23)
    };
    Some(exact_finite(negative, significand, exponent))
}

fn exact_finite(negative: bool, significand: u64, exponent: i16) -> ExactNumber {
    assert_ne!(significand, 0, "zero has a dedicated exact-number variant");
    let trailing = significand.trailing_zeros() as i16;
    let odd_significand = significand >> trailing;
    let exponent = exponent + trailing;
    let floor_log2 = exponent + (u64::BITS - 1 - odd_significand.leading_zeros()) as i16;
    let finite = ExactFinite {
        exponent,
        odd_significand,
        floor_log2,
        normalized_significand: odd_significand << odd_significand.leading_zeros(),
    };
    if negative {
        ExactNumber::NegativeFinite(finite)
    } else {
        ExactNumber::PositiveFinite(finite)
    }
}

fn put_equality_number(output: &mut Vec<u8>, number: ExactNumber) {
    match number {
        ExactNumber::NegativeInfinity => output.push(NEGATIVE_INFINITY_TAG),
        ExactNumber::NegativeFinite(value) => {
            output.push(NEGATIVE_FINITE_TAG);
            output.extend_from_slice(&value.exponent.to_be_bytes());
            output.extend_from_slice(&value.odd_significand.to_be_bytes());
        }
        ExactNumber::Zero => output.push(ZERO_TAG),
        ExactNumber::PositiveFinite(value) => {
            output.push(POSITIVE_FINITE_TAG);
            output.extend_from_slice(&value.exponent.to_be_bytes());
            output.extend_from_slice(&value.odd_significand.to_be_bytes());
        }
        ExactNumber::PositiveInfinity => output.push(POSITIVE_INFINITY_TAG),
    }
}

fn put_ordered_number(output: &mut Vec<u8>, number: ExactNumber) {
    match number {
        ExactNumber::NegativeInfinity => output.push(NEGATIVE_INFINITY_TAG),
        ExactNumber::NegativeFinite(value) => {
            output.push(NEGATIVE_FINITE_TAG);
            output.extend_from_slice(&(!biased_exponent(value.floor_log2)).to_be_bytes());
            output.extend_from_slice(&(!value.normalized_significand).to_be_bytes());
        }
        ExactNumber::Zero => output.push(ZERO_TAG),
        ExactNumber::PositiveFinite(value) => {
            output.push(POSITIVE_FINITE_TAG);
            output.extend_from_slice(&biased_exponent(value.floor_log2).to_be_bytes());
            output.extend_from_slice(&value.normalized_significand.to_be_bytes());
        }
        ExactNumber::PositiveInfinity => output.push(POSITIVE_INFINITY_TAG),
    }
}

fn biased_exponent(exponent: i16) -> u16 {
    u16::try_from(i32::from(exponent) + EXPONENT_BIAS)
        .expect("exact binary exponent fits biased u16")
}

fn put_count(output: &mut Vec<u8>, count: usize) -> Option<()> {
    output.extend_from_slice(&u32::try_from(count).ok()?.to_be_bytes());
    Some(())
}

fn put_length_delimited(output: &mut Vec<u8>, value: &[u8]) -> Option<()> {
    output.extend_from_slice(&u32::try_from(value.len()).ok()?.to_be_bytes());
    output.extend_from_slice(value);
    Some(())
}

fn oversized_equality(encoded_len: usize) -> EqualityProjection {
    EqualityProjection::Oversized {
        encoded_len,
        maximum: MAX_EQUALITY_CANONICAL_LEN,
    }
}
