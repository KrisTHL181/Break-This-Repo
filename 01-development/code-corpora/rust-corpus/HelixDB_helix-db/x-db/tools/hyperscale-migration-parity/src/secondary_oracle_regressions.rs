use db::migration_parity::ParityValue;
use sha2::{Digest, Sha256};

use crate::secondary_oracle::{
    project_equality, project_range, EqualityProjection, RangeDirection, RangeProjection,
};

fn indexed_equality(value: &ParityValue) -> ([u8; 8], Vec<u8>) {
    let EqualityProjection::Indexed { digest, canonical } = project_equality(value) else {
        panic!("fixture must produce indexed equality bytes");
    };
    (digest, canonical)
}

fn indexed_range(value: &ParityValue, direction: RangeDirection) -> Vec<u8> {
    let RangeProjection::Indexed(encoded) = project_range(value, direction) else {
        panic!("fixture must produce indexed range bytes");
    };
    encoded
}

#[test]
fn equality_oracle_is_typed_exact_and_hashes_complete_canonical_bytes() {
    let boolean = indexed_equality(&ParityValue::Bool(true));
    let string = indexed_equality(&ParityValue::String("true".to_string()));
    let integer = indexed_equality(&ParityValue::I64(42));
    let formatted_integer = indexed_equality(&ParityValue::String("42".to_string()));
    let bytes = indexed_equality(&ParityValue::Bytes(vec![1, 2]));
    let debug_string = indexed_equality(&ParityValue::String("[1, 2]".to_string()));
    let first_array = indexed_equality(&ParityValue::I64Array(vec![1, 2]));
    let second_array = indexed_equality(&ParityValue::I64Array(vec![3, 4]));

    assert_eq!(boolean.1, vec![0x01, 0x01]);
    assert_eq!(
        string.1,
        [vec![0x04, 0x00, 0x00, 0x00, 0x04], b"true".to_vec()].concat()
    );
    assert_ne!(boolean, string);
    assert_ne!(integer, formatted_integer);
    assert_ne!(bytes, debug_string);
    assert_ne!(first_array, second_array);
    assert_eq!(
        integer.1,
        vec![0x02, 0x04, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x15,]
    );
    assert_eq!(
        formatted_integer.1,
        vec![0x04, 0x00, 0x00, 0x00, 0x02, b'4', b'2']
    );
    assert_eq!(
        first_array.1,
        [
            vec![0x06, 0x00, 0x00, 0x00, 0x02],
            1_i64.to_be_bytes().to_vec(),
            2_i64.to_be_bytes().to_vec(),
        ]
        .concat()
    );
    for (digest, canonical) in [
        boolean,
        string,
        integer,
        formatted_integer,
        bytes,
        debug_string,
        first_array,
        second_array,
    ] {
        assert_eq!(digest.as_slice(), &Sha256::digest(canonical)[..8]);
    }
}

#[test]
fn equality_oracle_uses_exact_cross_numeric_and_nonreflexive_semantics() {
    let two_to_53 = indexed_equality(&ParityValue::I64(9_007_199_254_740_992));
    let two_to_53_float =
        indexed_equality(&ParityValue::F64Bits(9_007_199_254_740_992.0_f64.to_bits()));
    let two_to_53_plus_one = indexed_equality(&ParityValue::I64(9_007_199_254_740_993));
    let positive_zero = indexed_equality(&ParityValue::F64Bits(0.0_f64.to_bits()));
    let negative_zero = indexed_equality(&ParityValue::F64Bits((-0.0_f64).to_bits()));
    let positive_infinity = indexed_equality(&ParityValue::F64Bits(f64::INFINITY.to_bits()));

    assert_eq!(two_to_53, two_to_53_float);
    assert_ne!(two_to_53, two_to_53_plus_one);
    assert_eq!(positive_zero, negative_zero);
    assert_eq!(positive_zero.1, vec![0x02, 0x03]);
    assert_eq!(
        indexed_equality(&ParityValue::F64Bits(f64::NEG_INFINITY.to_bits())).1,
        vec![0x02, 0x01]
    );
    assert_eq!(positive_infinity.1, vec![0x02, 0x05]);
    assert_eq!(
        two_to_53.1,
        vec![0x02, 0x04, 0x00, 0x35, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01,]
    );
    assert_eq!(
        two_to_53_plus_one.1,
        vec![0x02, 0x04, 0x00, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01,]
    );
    assert_eq!(
        project_equality(&ParityValue::Null),
        EqualityProjection::Absent
    );
    assert_eq!(
        project_equality(&ParityValue::F64Bits(f64::NAN.to_bits())),
        EqualityProjection::Absent
    );
    assert!(matches!(
        project_equality(&ParityValue::Array(Vec::new())),
        EqualityProjection::Unsupported("Array")
    ));
}

#[test]
fn range_oracle_emits_typed_ascending_and_descending_goldens() {
    for (value, ascending) in [
        ("", vec![0x03, 0x00, 0x00]),
        ("\0", vec![0x03, 0x00, 0xFF, 0x00, 0x00]),
        ("a", vec![0x03, b'a', 0x00, 0x00]),
        ("a\0", vec![0x03, b'a', 0x00, 0xFF, 0x00, 0x00]),
        ("aa", vec![0x03, b'a', b'a', 0x00, 0x00]),
        ("aaa", vec![0x03, b'a', b'a', b'a', 0x00, 0x00]),
    ] {
        let string = ParityValue::String(value.to_string());
        assert_eq!(indexed_range(&string, RangeDirection::Ascending), ascending);
        assert_eq!(
            indexed_range(&string, RangeDirection::Descending),
            ascending.iter().map(|byte| !byte).collect::<Vec<_>>()
        );
    }

    assert_eq!(
        indexed_range(
            &ParityValue::F64Bits(0.0_f64.to_bits()),
            RangeDirection::Ascending
        ),
        vec![0x01, 0x03]
    );
    assert_eq!(
        indexed_range(
            &ParityValue::F64Bits(f64::NEG_INFINITY.to_bits()),
            RangeDirection::Ascending
        ),
        vec![0x01, 0x01]
    );
    assert_eq!(
        indexed_range(
            &ParityValue::F64Bits(f64::INFINITY.to_bits()),
            RangeDirection::Ascending
        ),
        vec![0x01, 0x05]
    );
    assert_eq!(
        indexed_range(&ParityValue::DateTime(-1), RangeDirection::Ascending),
        [vec![0x02], 0x7FFF_FFFF_FFFF_FFFF_u64.to_be_bytes().to_vec()].concat()
    );
}

#[test]
fn range_oracle_preserves_exact_numeric_and_domain_order() {
    let two_to_53 = indexed_range(
        &ParityValue::I64(9_007_199_254_740_992),
        RangeDirection::Ascending,
    );
    let two_to_53_float = indexed_range(
        &ParityValue::F64Bits(9_007_199_254_740_992.0_f64.to_bits()),
        RangeDirection::Ascending,
    );
    let two_to_53_plus_one = indexed_range(
        &ParityValue::I64(9_007_199_254_740_993),
        RangeDirection::Ascending,
    );
    let datetime = indexed_range(&ParityValue::DateTime(i64::MIN), RangeDirection::Ascending);
    let string = indexed_range(
        &ParityValue::String(String::new()),
        RangeDirection::Ascending,
    );

    assert_eq!(two_to_53, two_to_53_float);
    assert!(two_to_53 < two_to_53_plus_one);
    assert!(two_to_53_plus_one < datetime);
    assert!(datetime < string);
    assert_eq!(
        project_range(
            &ParityValue::F64Bits(f64::NAN.to_bits()),
            RangeDirection::Ascending
        ),
        RangeProjection::NaN
    );
    assert!(matches!(
        project_range(&ParityValue::Bool(true), RangeDirection::Ascending),
        RangeProjection::Unsupported("Bool")
    ));
}
