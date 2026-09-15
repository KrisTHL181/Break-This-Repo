use std::collections::BTreeMap;

use anyhow::{bail, Context, Result};
use db::migration_parity::{
    migration_parity_graph_hash_contract, migration_parity_index_name_hash_contract, ParityValue,
};
use helix::db::{RangeIndexDirection, TextElementType, VectorElementType};
use helix::{graph, PropertyValue as SourcePropertyValue};
use serde::{Deserialize, Serialize};
use sha2::{Digest, Sha256};

pub(crate) const HASH_CONTRACT_SCHEMA_VERSION: u32 = 1;
pub(crate) const HASH_CONTRACT_SOURCE_REVISION: &str = "e5bac15b020c9acac1649c44b58a2cf16dd1f874";
const GOLDEN_JSON: &str = include_str!("../hash_contract_v1.json");

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
struct GoldenCorpus {
    schema_version: u32,
    source_revision: String,
    graph: BTreeMap<String, BTreeMap<String, String>>,
    typed: BTreeMap<String, BTreeMap<String, String>>,
}

#[derive(Debug, Clone, Serialize)]
pub(crate) struct HashContractEvidence {
    pub(crate) schema_version: u32,
    pub(crate) source_revision: &'static str,
    pub(crate) graph_cases: usize,
    pub(crate) typed_cases: usize,
    pub(crate) compared_outputs: usize,
    pub(crate) migrated_descending_outputs: usize,
    pub(crate) corpus_sha256: String,
    pub(crate) passed: bool,
}

#[derive(Debug, Clone)]
struct GraphCase {
    id: &'static str,
    property: String,
    value: String,
    label: String,
    source: u64,
    target: u64,
    edge_id: u64,
}

pub(crate) fn emit_golden_json() -> Result<String> {
    serde_json::to_string_pretty(&observed_legacy_corpus()?).map_err(Into::into)
}

pub(crate) fn verify() -> Result<HashContractEvidence> {
    let golden: GoldenCorpus = serde_json::from_str(GOLDEN_JSON)
        .context("failed to decode checked-in hash contract golden corpus")?;
    if golden.schema_version != HASH_CONTRACT_SCHEMA_VERSION {
        bail!(
            "hash contract schema mismatch: expected {}, found {}",
            HASH_CONTRACT_SCHEMA_VERSION,
            golden.schema_version
        );
    }
    if golden.source_revision != HASH_CONTRACT_SOURCE_REVISION {
        bail!(
            "hash contract source mismatch: expected {}, found {}",
            HASH_CONTRACT_SOURCE_REVISION,
            golden.source_revision
        );
    }
    validate_golden_encoding(&golden)?;

    let legacy = observed_legacy_corpus()?;
    let target = observed_target_corpus()?;
    compare_section("legacy graph", &legacy.graph, &golden.graph)?;
    let migrated_descending_outputs = compare_target_graph(&target.graph, &golden.graph)?;
    compare_section("legacy typed", &legacy.typed, &golden.typed)?;
    compare_section("target typed", &target.typed, &golden.typed)?;

    let compared_outputs = golden
        .graph
        .values()
        .chain(golden.typed.values())
        .map(BTreeMap::len)
        .sum::<usize>()
        .saturating_mul(2);
    Ok(HashContractEvidence {
        schema_version: HASH_CONTRACT_SCHEMA_VERSION,
        source_revision: HASH_CONTRACT_SOURCE_REVISION,
        graph_cases: golden.graph.len(),
        typed_cases: golden.typed.len(),
        compared_outputs,
        migrated_descending_outputs,
        corpus_sha256: hex::encode(Sha256::digest(GOLDEN_JSON.as_bytes())),
        passed: true,
    })
}

fn migrated_descending_shape(output: &str) -> Option<(usize, usize)> {
    match output {
        "node_range_desc_key" => Some((6, 8)),
        "node_range_desc_value_prefix" => Some((6, 0)),
        "edge_range_out_desc_key" | "edge_range_in_desc_key" => Some((15, 8)),
        "edge_range_out_desc_value_prefix" | "edge_range_in_desc_value_prefix" => Some((15, 0)),
        _ => None,
    }
}

fn legacy_descending_value(value: &str) -> Vec<u8> {
    let mut encoded = Vec::with_capacity(value.len().saturating_mul(2).saturating_add(2));
    for byte in value.as_bytes() {
        encoded.push(!byte);
        encoded.push(0xFE);
    }
    encoded.extend_from_slice(&[0xFF, 0xFF]);
    encoded
}

fn proper_descending_value(value: &str) -> Vec<u8> {
    let mut encoded = Vec::with_capacity(value.len().saturating_add(2));
    for byte in value.as_bytes() {
        encoded.push(!byte);
        if *byte == 0 {
            encoded.push(0x00);
        }
    }
    encoded.extend_from_slice(&[0xFF, 0xFE]);
    encoded
}

fn migrated_descending_expected(
    case: &GraphCase,
    output: &str,
    legacy_hex: &str,
) -> Result<Option<String>> {
    let Some((prefix_len, suffix_len)) = migrated_descending_shape(output) else {
        return Ok(None);
    };
    let legacy = hex::decode(legacy_hex)
        .with_context(|| format!("legacy graph case {} output {output}", case.id))?;
    let legacy_value = legacy_descending_value(&case.value);
    let minimum_len = prefix_len
        .checked_add(legacy_value.len())
        .and_then(|len| len.checked_add(suffix_len))
        .context("descending contract length overflowed")?;
    if legacy.len() != minimum_len || legacy[prefix_len..legacy.len() - suffix_len] != legacy_value
    {
        bail!(
            "legacy graph case {} output {output} does not contain the pinned legacy descending encoding",
            case.id
        );
    }
    let mut expected = Vec::with_capacity(
        prefix_len
            .saturating_add(case.value.len())
            .saturating_add(2)
            .saturating_add(suffix_len),
    );
    expected.extend_from_slice(&legacy[..prefix_len]);
    expected.extend_from_slice(&proper_descending_value(&case.value));
    if suffix_len != 0 {
        expected.extend_from_slice(&legacy[legacy.len() - suffix_len..]);
    }
    Ok(Some(hex::encode(expected)))
}

fn compare_target_graph(
    actual: &BTreeMap<String, BTreeMap<String, String>>,
    legacy_golden: &BTreeMap<String, BTreeMap<String, String>>,
) -> Result<usize> {
    let cases = graph_cases()
        .into_iter()
        .map(|case| (case.id, case))
        .collect::<BTreeMap<_, _>>();
    let mut target_golden = legacy_golden.clone();
    let mut migrated = 0;
    for (case_id, outputs) in &mut target_golden {
        let case = cases
            .get(case_id.as_str())
            .with_context(|| format!("target graph golden has unknown case {case_id}"))?;
        for (name, value) in outputs {
            if let Some(expected) = migrated_descending_expected(case, name, value)? {
                if expected == *value {
                    bail!(
                        "graph case {case_id} output {name} did not prove the intended descending-key migration"
                    );
                }
                *value = expected;
                migrated += 1;
            }
        }
    }
    compare_section("target graph", actual, &target_golden)?;
    Ok(migrated)
}

fn validate_hex_output(context: &str, value: &str) -> Result<()> {
    let decoded = hex::decode(value).with_context(|| format!("{context} is not hexadecimal"))?;
    if hex::encode(decoded) != value {
        bail!("{context} is not canonical lowercase hexadecimal");
    }
    Ok(())
}

fn validate_golden_encoding(golden: &GoldenCorpus) -> Result<()> {
    for (case, outputs) in &golden.graph {
        for (name, value) in outputs {
            validate_hex_output(&format!("graph case {case} output {name}"), value)?;
        }
    }
    for (case, outputs) in &golden.typed {
        for (name, value) in outputs {
            if name.contains("component") || name == "tenant_value_hash" {
                validate_hex_output(&format!("typed case {case} output {name}"), value)?;
                continue;
            }
            for (index, component) in value.split(':').skip(2).enumerate() {
                validate_hex_output(
                    &format!("typed case {case} output {name} hash component {index}"),
                    component,
                )?;
            }
        }
    }
    Ok(())
}

fn compare_section(
    section: &str,
    actual: &BTreeMap<String, BTreeMap<String, String>>,
    expected: &BTreeMap<String, BTreeMap<String, String>>,
) -> Result<()> {
    if actual == expected {
        return Ok(());
    }
    for (case, expected_outputs) in expected {
        let Some(actual_outputs) = actual.get(case) else {
            bail!("{section} is missing case {case}");
        };
        for (name, expected_hex) in expected_outputs {
            match actual_outputs.get(name) {
                Some(actual_hex) if actual_hex == expected_hex => {}
                Some(actual_hex) => bail!(
                    "{section} case {case} output {name} changed: expected {expected_hex}, found {actual_hex}"
                ),
                None => bail!("{section} case {case} is missing output {name}"),
            }
        }
        if actual_outputs.len() != expected_outputs.len() {
            bail!(
                "{section} case {case} output count changed: expected {}, found {}",
                expected_outputs.len(),
                actual_outputs.len()
            );
        }
    }
    bail!(
        "{section} case count changed: expected {}, found {}",
        expected.len(),
        actual.len()
    )
}

fn observed_legacy_corpus() -> Result<GoldenCorpus> {
    Ok(GoldenCorpus {
        schema_version: HASH_CONTRACT_SCHEMA_VERSION,
        source_revision: HASH_CONTRACT_SOURCE_REVISION.to_string(),
        graph: graph_cases()
            .iter()
            .map(|case| (case.id.to_string(), legacy_graph_outputs(case)))
            .collect(),
        typed: typed_cases()
            .into_iter()
            .map(|(id, _, source)| (id.to_string(), legacy_typed_outputs(&source)))
            .collect(),
    })
}

fn observed_target_corpus() -> Result<GoldenCorpus> {
    Ok(GoldenCorpus {
        schema_version: HASH_CONTRACT_SCHEMA_VERSION,
        source_revision: HASH_CONTRACT_SOURCE_REVISION.to_string(),
        graph: graph_cases()
            .iter()
            .map(|case| {
                (
                    case.id.to_string(),
                    migration_parity_graph_hash_contract(
                        &case.property,
                        &case.value,
                        &case.label,
                        case.source,
                        case.target,
                        case.edge_id,
                    )
                    .into_iter()
                    .map(|(name, bytes)| (name, hex::encode(bytes)))
                    .collect(),
                )
            })
            .collect(),
        typed: typed_cases()
            .into_iter()
            .map(|(id, target, _)| {
                (
                    id.to_string(),
                    migration_parity_index_name_hash_contract(
                        "nul\0é🦀𝄞",
                        "property/name",
                        "tenant.key",
                        &target,
                    ),
                )
            })
            .collect(),
    })
}

fn graph_cases() -> Vec<GraphCase> {
    vec![
        GraphCase {
            id: "empty",
            property: String::new(),
            value: String::new(),
            label: String::new(),
            source: 0,
            target: u64::MAX,
            edge_id: 0x0102_0304_0506_0708,
        },
        GraphCase {
            id: "label",
            property: "$label".to_string(),
            value: "User".to_string(),
            label: "FOLLOWS".to_string(),
            source: 1,
            target: 2,
            edge_id: 3,
        },
        GraphCase {
            id: "unicode_and_nul",
            property: "nul\0é🦀𝄞".to_string(),
            value: "value\0東京".to_string(),
            label: "关系🦀".to_string(),
            source: 0x1020_3040_5060_7080,
            target: 0x8877_6655_4433_2211,
            edge_id: u64::MAX,
        },
        GraphCase {
            id: "long",
            property: "p".repeat(1_024),
            value: "v".repeat(4_096),
            label: "L".repeat(257),
            source: 42,
            target: 43,
            edge_id: 44,
        },
    ]
}

fn legacy_graph_outputs(case: &GraphCase) -> BTreeMap<String, String> {
    let mut rows = BTreeMap::new();
    rows.insert(
        "property_name_hash".to_string(),
        hex::encode(graph::hash_property_name(&case.property)),
    );
    rows.insert(
        "property_value_hash".to_string(),
        hex::encode(&graph::make_equality_index_key(&case.property, &case.value)[6..]),
    );
    rows.insert(
        "node_equality_key".to_string(),
        hex::encode(graph::make_equality_index_key(&case.property, &case.value)),
    );
    rows.insert(
        "node_equality_property_prefix".to_string(),
        hex::encode(graph::make_equality_index_property_prefix(&case.property)),
    );
    for (name, direction) in [
        ("asc", RangeIndexDirection::Asc),
        ("desc", RangeIndexDirection::Desc),
    ] {
        rows.insert(
            format!("node_range_{name}_key"),
            hex::encode(graph::make_range_index_key_with_direction(
                &case.property,
                &case.value,
                case.source,
                direction,
            )),
        );
        rows.insert(
            format!("node_range_{name}_property_prefix"),
            hex::encode(graph::make_range_index_prefix_with_direction(
                &case.property,
                direction,
            )),
        );
        rows.insert(
            format!("node_range_{name}_value_prefix"),
            hex::encode(graph::make_range_index_value_prefix_with_direction(
                &case.property,
                &case.value,
                direction,
            )),
        );
        rows.insert(
            format!("edge_range_out_{name}_key"),
            hex::encode(graph::make_edge_range_index_out_key_with_direction(
                case.source,
                &case.property,
                &case.value,
                case.edge_id,
                direction,
            )),
        );
        rows.insert(
            format!("edge_range_out_{name}_property_prefix"),
            hex::encode(graph::make_edge_range_index_out_prefix_with_direction(
                case.source,
                &case.property,
                direction,
            )),
        );
        rows.insert(
            format!("edge_range_out_{name}_value_prefix"),
            hex::encode(
                graph::make_edge_range_index_out_value_prefix_with_direction(
                    case.source,
                    &case.property,
                    &case.value,
                    direction,
                ),
            ),
        );
        rows.insert(
            format!("edge_range_in_{name}_key"),
            hex::encode(graph::make_edge_range_index_in_key_with_direction(
                case.target,
                &case.property,
                &case.value,
                case.edge_id,
                direction,
            )),
        );
        rows.insert(
            format!("edge_range_in_{name}_property_prefix"),
            hex::encode(graph::make_edge_range_index_in_prefix_with_direction(
                case.target,
                &case.property,
                direction,
            )),
        );
        rows.insert(
            format!("edge_range_in_{name}_value_prefix"),
            hex::encode(graph::make_edge_range_index_in_value_prefix_with_direction(
                case.target,
                &case.property,
                &case.value,
                direction,
            )),
        );
    }
    rows.insert(
        "edge_equality_out_key".to_string(),
        hex::encode(graph::make_edge_equality_index_out_key(
            case.source,
            &case.property,
            &case.value,
        )),
    );
    rows.insert(
        "edge_equality_in_key".to_string(),
        hex::encode(graph::make_edge_equality_index_in_key(
            case.target,
            &case.property,
            &case.value,
        )),
    );
    rows.insert(
        "global_edge_label_key".to_string(),
        hex::encode(graph::make_global_edge_label_index_key(&case.label)),
    );
    rows.insert(
        "edge_label_out_key".to_string(),
        hex::encode(graph::make_edge_label_out_key(case.source, &case.label)),
    );
    rows.insert(
        "edge_label_in_key".to_string(),
        hex::encode(graph::make_edge_label_in_key(case.target, &case.label)),
    );
    rows
}

fn legacy_typed_outputs(value: &SourcePropertyValue) -> BTreeMap<String, String> {
    let mut rows = BTreeMap::new();
    for (name, vector_kind, text_kind) in [
        ("node", VectorElementType::Node, TextElementType::Node),
        ("edge", VectorElementType::Edge, TextElementType::Edge),
    ] {
        rows.insert(
            format!("vector_{name}"),
            helix::db::index::vector_index_name(vector_kind, "nul\0é🦀𝄞", "property/name"),
        );
        rows.insert(
            format!("text_{name}"),
            helix::db::index::text_index_name(text_kind, "nul\0é🦀𝄞", "property/name"),
        );
        rows.insert(
            format!("vector_tenant_{name}"),
            helix::db::index::vector_tenant_index_name(
                vector_kind,
                "nul\0é🦀𝄞",
                "property/name",
                "tenant.key",
                value,
            ),
        );
        rows.insert(
            format!("text_tenant_{name}"),
            helix::db::index::text_tenant_index_name(
                text_kind,
                "nul\0é🦀𝄞",
                "property/name",
                "tenant.key",
                value,
            ),
        );
    }
    let tenant_name = rows
        .get("text_tenant_node")
        .expect("typed contract always contains the tenant text name")
        .clone();
    rows.insert(
        "index_component_label".to_string(),
        tenant_name
            .split(':')
            .nth(2)
            .expect("tenant text name contains label hash")
            .to_string(),
    );
    rows.insert(
        "index_component_property".to_string(),
        tenant_name
            .split(':')
            .nth(3)
            .expect("tenant text name contains property hash")
            .to_string(),
    );
    rows.insert(
        "tenant_value_hash".to_string(),
        tenant_name
            .rsplit(':')
            .next()
            .expect("tenant text name contains value hash")
            .to_string(),
    );
    rows
}

fn typed_cases() -> Vec<(&'static str, ParityValue, SourcePropertyValue)> {
    let target_object = BTreeMap::from([
        ("empty".to_string(), ParityValue::String(String::new())),
        (
            "nested".to_string(),
            ParityValue::Array(vec![ParityValue::Bool(false), ParityValue::I64(-1)]),
        ),
    ]);
    let source_object = BTreeMap::from([
        (
            "empty".to_string(),
            SourcePropertyValue::String(String::new()),
        ),
        (
            "nested".to_string(),
            SourcePropertyValue::Array(vec![
                SourcePropertyValue::Bool(false),
                SourcePropertyValue::I64(-1),
            ]),
        ),
    ]);
    vec![
        ("null", ParityValue::Null, SourcePropertyValue::Null),
        (
            "bool_false",
            ParityValue::Bool(false),
            SourcePropertyValue::Bool(false),
        ),
        (
            "bool_true",
            ParityValue::Bool(true),
            SourcePropertyValue::Bool(true),
        ),
        (
            "i64_min",
            ParityValue::I64(i64::MIN),
            SourcePropertyValue::I64(i64::MIN),
        ),
        (
            "i64_max",
            ParityValue::I64(i64::MAX),
            SourcePropertyValue::I64(i64::MAX),
        ),
        (
            "datetime",
            ParityValue::DateTime(-1_234_567_890),
            SourcePropertyValue::DateTime(-1_234_567_890),
        ),
        (
            "f64_negative_zero",
            ParityValue::F64Bits((-0.0_f64).to_bits()),
            SourcePropertyValue::F64(-0.0),
        ),
        (
            "f64_infinity",
            ParityValue::F64Bits(f64::INFINITY.to_bits()),
            SourcePropertyValue::F64(f64::INFINITY),
        ),
        (
            "f64_nan_payload",
            ParityValue::F64Bits(0x7ff8_0000_0000_0042),
            SourcePropertyValue::F64(f64::from_bits(0x7ff8_0000_0000_0042)),
        ),
        (
            "f32_storage_bits",
            ParityValue::F32Bits(0x8000_0000_0000_0000),
            SourcePropertyValue::F32(f64::from_bits(0x8000_0000_0000_0000)),
        ),
        (
            "empty_string",
            ParityValue::String(String::new()),
            SourcePropertyValue::String(String::new()),
        ),
        (
            "unicode_string",
            ParityValue::String("nul\0é🦀𝄞".to_string()),
            SourcePropertyValue::String("nul\0é🦀𝄞".to_string()),
        ),
        (
            "empty_bytes",
            ParityValue::Bytes(Vec::new()),
            SourcePropertyValue::Bytes(Vec::new()),
        ),
        (
            "bytes",
            ParityValue::Bytes(vec![0x00, 0xff, 0x7f, 0x80]),
            SourcePropertyValue::Bytes(vec![0x00, 0xff, 0x7f, 0x80]),
        ),
        (
            "i64_array",
            ParityValue::I64Array(vec![i64::MIN, 0, i64::MAX]),
            SourcePropertyValue::I64Array(vec![i64::MIN, 0, i64::MAX]),
        ),
        (
            "f64_array",
            ParityValue::F64ArrayBits(vec![(-0.0_f64).to_bits(), 0x7ff8_0000_0000_0043]),
            SourcePropertyValue::F64Array(vec![-0.0, f64::from_bits(0x7ff8_0000_0000_0043)]),
        ),
        (
            "f32_array",
            ParityValue::F32ArrayBits(vec![(-0.0_f32).to_bits(), 0x7fc0_0042]),
            SourcePropertyValue::F32Array(vec![-0.0, f32::from_bits(0x7fc0_0042)]),
        ),
        (
            "string_array",
            ParityValue::StringArray(vec![String::new(), "é".to_string(), "é".to_string()]),
            SourcePropertyValue::StringArray(vec![String::new(), "é".to_string(), "é".to_string()]),
        ),
        (
            "heterogeneous_array",
            ParityValue::Array(vec![
                ParityValue::Null,
                ParityValue::Bytes(vec![1, 2, 3]),
                ParityValue::Object(target_object.clone()),
            ]),
            SourcePropertyValue::Array(vec![
                SourcePropertyValue::Null,
                SourcePropertyValue::Bytes(vec![1, 2, 3]),
                SourcePropertyValue::Object(source_object.clone()),
            ]),
        ),
        (
            "object",
            ParityValue::Object(target_object),
            SourcePropertyValue::Object(source_object),
        ),
        (
            "empty_array",
            ParityValue::Array(Vec::new()),
            SourcePropertyValue::Array(Vec::new()),
        ),
        (
            "empty_object",
            ParityValue::Object(BTreeMap::new()),
            SourcePropertyValue::Object(BTreeMap::new()),
        ),
    ]
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn checked_in_corpus_is_complete_and_matches_both_implementations() {
        let evidence = verify().expect("hash contract must remain byte stable");
        assert!(evidence.passed);
        assert_eq!(evidence.graph_cases, graph_cases().len());
        assert_eq!(evidence.typed_cases, typed_cases().len());
        assert_eq!(evidence.migrated_descending_outputs, 24);
    }

    #[test]
    fn corpus_covers_every_property_value_variant() {
        assert_eq!(typed_cases().len(), 22);
    }

    #[test]
    fn golden_decoder_rejects_noncanonical_or_malformed_hashes() {
        assert!(validate_hex_output("test", "not-hex").is_err());
        assert!(validate_hex_output("test", "A0").is_err());
        assert!(validate_hex_output("test", "a0").is_ok());
    }

    #[test]
    fn descending_contract_preserves_legacy_bytes_and_requires_proper_reencoding() {
        let legacy = observed_legacy_corpus().unwrap();
        let target = observed_target_corpus().unwrap();
        for case in graph_cases() {
            let legacy_outputs = legacy.graph.get(case.id).unwrap();
            let target_outputs = target.graph.get(case.id).unwrap();
            for (name, legacy_hex) in legacy_outputs {
                let Some(expected) = migrated_descending_expected(&case, name, legacy_hex).unwrap()
                else {
                    continue;
                };
                assert_ne!(expected, *legacy_hex, "{}/{}", case.id, name);
                assert_eq!(
                    target_outputs.get(name),
                    Some(&expected),
                    "{}/{}",
                    case.id,
                    name
                );
            }
        }
    }
}
