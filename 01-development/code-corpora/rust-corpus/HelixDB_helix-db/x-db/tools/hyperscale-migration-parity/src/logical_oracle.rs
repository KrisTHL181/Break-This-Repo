//! Cross-version logical streams for the migration parity oracle.

use std::collections::{BTreeMap, BTreeSet};
use std::num::NonZeroUsize;
use std::path::{Path, PathBuf};

use anyhow::{bail, Context, Result};
use bytes::Bytes;
use db::migration_parity::{
    self, MigrationParitySecondaryMembership, MigrationParitySecondaryValue,
    MigrationParityV2State, ParityProperty, ParityValue,
};
use db::HelixDB;
use helix::db::index::hnsw::VectorDistanceMetric;
use helix::db::{DynamicIndexDefinition as HDynamicIndexDefinition, VectorIndexDefinition};
use helix::{
    graph, HelixDb as HyperscaleDb, Property as HProperty, PropertyValue as HPropertyValue,
};
use hyperscale_slatedb::config::ScanOptions as SourceScanOptions;
use sha2::{Digest, Sha256};

use crate::external_sort::{
    compare_sorted, compare_sorted_with_policy, deduplicate_sorted,
    merge_current_and_legacy_edges_with_equivalence, Comparison, ComparisonPolicy, ExternalSorter,
    Record, SortConfig, SortStats,
};
use crate::secondary_oracle::{
    project_equality, project_range, EqualityProjection, RangeDirection, RangeProjection,
};
const PREFIX_LEN: usize = core::mem::size_of::<u8>();
const ID_LEN: usize = core::mem::size_of::<u64>();
const ENDPOINT_BYTES: usize = ID_LEN * 2;
const FIRST_DIFFERENCE_LIMIT: usize = 100;
const ORACLE_READ_AHEAD_BYTES: usize = 2 * 1024 * 1024;
const ORACLE_MAXIMUM_FETCH_TASKS: usize = 16;
const VECTOR_SIMHASH_DIRECTORY_MIGRATION_KEY: &[u8] =
    b"\xFFkv_vector_simhash_directory_v1_migration";

#[derive(Debug, Clone)]
pub(crate) struct OraclePaths {
    pub(crate) source_nodes: PathBuf,
    pub(crate) source_current_edges: PathBuf,
    pub(crate) source_current_edge_equivalents: PathBuf,
    pub(crate) source_legacy_edges: PathBuf,
    pub(crate) source_expected_edges: PathBuf,
    pub(crate) source_edges_by_id: PathBuf,
    pub(crate) source_exact: PathBuf,
    pub(crate) target_nodes: PathBuf,
    pub(crate) target_edges: PathBuf,
    pub(crate) target_edges_by_id: PathBuf,
    pub(crate) target_exact: PathBuf,
    pub(crate) target_expected_indexes: PathBuf,
    pub(crate) target_actual_indexes: PathBuf,
    pub(crate) target_expected_graph_state: PathBuf,
    pub(crate) target_actual_graph_state: PathBuf,
}

impl OraclePaths {
    pub(crate) fn new(directory: &Path) -> Self {
        Self {
            source_nodes: directory.join("source-nodes.sorted"),
            source_current_edges: directory.join("source-current-edges.sorted"),
            source_current_edge_equivalents: directory
                .join("source-current-edge-equivalents.sorted"),
            source_legacy_edges: directory.join("source-legacy-edges.sorted"),
            source_expected_edges: directory.join("source-expected-edges.sorted"),
            source_edges_by_id: directory.join("source-edges-by-id.sorted"),
            source_exact: directory.join("source-exact.sorted"),
            target_nodes: directory.join("target-nodes.sorted"),
            target_edges: directory.join("target-edges.sorted"),
            target_edges_by_id: directory.join("target-edges-by-id.sorted"),
            target_exact: directory.join("target-exact.sorted"),
            target_expected_indexes: directory.join("target-expected-indexes.sorted"),
            target_actual_indexes: directory.join("target-actual-indexes.sorted"),
            target_expected_graph_state: directory.join("target-expected-graph-state.sorted"),
            target_actual_graph_state: directory.join("target-actual-graph-state.sorted"),
        }
    }
}

#[derive(Debug, Clone, serde::Serialize)]
pub(crate) struct OracleBuildStats {
    pub(crate) nodes: SortStats,
    pub(crate) current_edges: SortStats,
    pub(crate) legacy_edges: Option<SortStats>,
    pub(crate) expected_edges: Option<SortStats>,
    pub(crate) edges_by_id: SortStats,
    pub(crate) exact_keys: SortStats,
    pub(crate) expected_indexes: Option<SortStats>,
    pub(crate) actual_indexes: Option<SortStats>,
    pub(crate) maximum_node_id: Option<u64>,
    pub(crate) node_allocator_watermark: Option<u64>,
    pub(crate) maximum_edge_id: Option<u64>,
    pub(crate) edge_allocator_watermark: Option<u64>,
    pub(crate) expected_graph_state: Option<SortStats>,
    pub(crate) actual_graph_state: Option<SortStats>,
    pub(crate) raw_hash_key_manifest: Option<BTreeMap<String, RawKeyFamilyEvidence>>,
    pub(crate) source_physical_rows: Option<u64>,
    pub(crate) source_physical_bytes: Option<u64>,
    pub(crate) legacy_vector_rows: Option<u64>,
    pub(crate) materialized_node_vector_properties: Option<u64>,
    pub(crate) materialized_edge_vector_properties: Option<u64>,
    pub(crate) unmatched_legacy_vector_rows: Option<u64>,
    pub(crate) preserved_unmanaged_legacy_vector_rows: Option<u64>,
}

#[derive(Debug, Clone, serde::Serialize)]
pub(crate) struct RawKeyFamilyEvidence {
    pub(crate) records: u64,
    pub(crate) key_bytes: u64,
    pub(crate) value_bytes: u64,
    pub(crate) sha256: String,
    pub(crate) first_key_hex: String,
    pub(crate) last_key_hex: String,
}

#[derive(Default)]
struct RawKeyFamilyAccumulator {
    records: u64,
    key_bytes: u64,
    value_bytes: u64,
    digest: Sha256,
    first_key: Option<Vec<u8>>,
    last_key: Vec<u8>,
}

impl RawKeyFamilyAccumulator {
    fn include(&mut self, key: &[u8], value: &[u8]) -> Result<()> {
        self.records = self.records.saturating_add(1);
        self.key_bytes = self
            .key_bytes
            .checked_add(u64::try_from(key.len())?)
            .context("raw-key manifest key bytes overflowed u64")?;
        self.value_bytes = self
            .value_bytes
            .checked_add(u64::try_from(value.len())?)
            .context("raw-key manifest value bytes overflowed u64")?;
        self.digest.update(u64::try_from(key.len())?.to_be_bytes());
        self.digest
            .update(u64::try_from(value.len())?.to_be_bytes());
        self.digest.update(key);
        self.digest.update(value);
        self.first_key.get_or_insert_with(|| key.to_vec());
        self.last_key = key.to_vec();
        Ok(())
    }

    fn finish(self) -> RawKeyFamilyEvidence {
        RawKeyFamilyEvidence {
            records: self.records,
            key_bytes: self.key_bytes,
            value_bytes: self.value_bytes,
            sha256: hex::encode(self.digest.finalize()),
            first_key_hex: hex::encode(self.first_key.unwrap_or_default()),
            last_key_hex: hex::encode(self.last_key),
        }
    }
}

#[derive(Debug, Clone, serde::Serialize)]
pub(crate) struct OracleComparison {
    pub(crate) nodes: Comparison,
    pub(crate) logical_edges: Comparison,
    pub(crate) preexisting_edges_by_id: Comparison,
    pub(crate) exact_passthrough_keys: Comparison,
    pub(crate) indexes: Comparison,
    pub(crate) graph_state: Comparison,
}

/// Exact logical comparison of the live source view with the same database
/// after it has been closed and reopened from durable object storage.
#[derive(Debug, Clone, serde::Serialize)]
pub(crate) struct SourceDurabilityComparison {
    pub(crate) nodes: Comparison,
    pub(crate) current_edges: Comparison,
    pub(crate) legacy_edges: Comparison,
    pub(crate) expected_edges: Comparison,
    pub(crate) edges_by_id: Comparison,
    pub(crate) exact_passthrough_keys: Comparison,
}

#[derive(Debug, Clone, serde::Serialize)]
pub(crate) struct TargetDurabilityComparison {
    pub(crate) nodes: Comparison,
    pub(crate) current_edges: Comparison,
    pub(crate) edges_by_id: Comparison,
    pub(crate) exact_passthrough_keys: Comparison,
    pub(crate) expected_indexes: Comparison,
    pub(crate) actual_indexes: Comparison,
    pub(crate) expected_graph_state: Comparison,
    pub(crate) actual_graph_state: Comparison,
}

impl SourceDurabilityComparison {
    pub(crate) fn is_equal(&self) -> bool {
        self.nodes.is_equal()
            && self.current_edges.is_equal()
            && self.legacy_edges.is_equal()
            && self.expected_edges.is_equal()
            && self.edges_by_id.is_equal()
            && self.exact_passthrough_keys.is_equal()
    }
}

impl TargetDurabilityComparison {
    pub(crate) fn is_equal(&self) -> bool {
        self.nodes.is_equal()
            && self.current_edges.is_equal()
            && self.edges_by_id.is_equal()
            && self.exact_passthrough_keys.is_equal()
            && self.expected_indexes.is_equal()
            && self.actual_indexes.is_equal()
            && self.expected_graph_state.is_equal()
            && self.actual_graph_state.is_equal()
    }
}

impl OracleComparison {
    pub(crate) fn is_equal(&self) -> bool {
        self.nodes.is_equal()
            && self.logical_edges.is_equal()
            && self.preexisting_edges_by_id.is_equal()
            && self.exact_passthrough_keys.is_equal()
            && self.indexes.is_equal()
            && self.graph_state.is_equal()
    }
}

struct Writers {
    nodes: ExternalSorter,
    current_edges: ExternalSorter,
    current_edge_equivalents: Option<ExternalSorter>,
    legacy_edges: Option<ExternalSorter>,
    edges_by_id: ExternalSorter,
    exact_keys: ExternalSorter,
}

impl Writers {
    fn new(directory: &Path, name: &str, buffer_bytes: NonZeroUsize, source: bool) -> Result<Self> {
        let config = SortConfig { buffer_bytes };
        Ok(Self {
            nodes: ExternalSorter::new(directory, format!("{name}-nodes"), config)?,
            current_edges: ExternalSorter::new(directory, format!("{name}-edges"), config)?,
            current_edge_equivalents: source
                .then(|| ExternalSorter::new(directory, format!("{name}-edge-equivalents"), config))
                .transpose()?,
            legacy_edges: source
                .then(|| ExternalSorter::new(directory, format!("{name}-legacy"), config))
                .transpose()?,
            edges_by_id: ExternalSorter::new(directory, format!("{name}-edge-ids"), config)?,
            exact_keys: ExternalSorter::new(directory, format!("{name}-exact"), config)?,
        })
    }
}

pub(crate) async fn build_source(
    db: &HyperscaleDb,
    directory: &Path,
    paths: &OraclePaths,
    buffer_bytes: NonZeroUsize,
) -> Result<OracleBuildStats> {
    let raw = db.inner_db();
    let mut writers = Writers::new(directory, "source", buffer_bytes, true)?;
    let options = SourceScanOptions::default()
        .with_read_ahead_bytes(ORACLE_READ_AHEAD_BYTES)
        .with_cache_blocks(false)
        .with_max_fetch_tasks(ORACLE_MAXIMUM_FETCH_TASKS);

    let vector_definitions = crate::migration_index_definitions()
        .into_iter()
        .filter_map(|definition| match definition {
            HDynamicIndexDefinition::Vector(definition) => Some(definition),
            HDynamicIndexDefinition::Secondary(_) | HDynamicIndexDefinition::Text(_) => None,
        })
        .collect::<Vec<_>>();
    let mut legacy_vector_rows = BTreeMap::new();
    let mut vectors = raw
        .scan_prefix_with_options(Bytes::from_static(&[0xF1]), &options)
        .await?;
    while let Some(kv) = vectors.next().await? {
        let Some(identity) = legacy_vector_row_identity(&kv.key) else {
            continue;
        };
        if legacy_vector_rows
            .insert(identity, kv.value.to_vec())
            .is_some()
        {
            bail!(
                "legacy HNSW contains multiple canonical vector rows for index {:#018x}, entity {}",
                identity.0,
                identity.1
            );
        }
    }
    let legacy_vector_row_count = u64::try_from(legacy_vector_rows.len())?;
    let mut managed_vector_index_ids = BTreeSet::new();
    let mut materialized_node_vector_properties = 0_u64;
    let mut materialized_edge_vector_properties = 0_u64;

    let mut nodes = raw
        .scan_prefix_with_options(
            graph::key_space_prefix(graph::KeySpace::NodeProperty),
            &options,
        )
        .await?;
    let mut maximum_node_id = None;
    while let Some(kv) = nodes.next().await? {
        let node_id = one_id_key(&kv.key, "source node property")?;
        maximum_node_id =
            Some(maximum_node_id.map_or(node_id, |current: u64| current.max(node_id)));
        let properties = graph::decode_properties(&kv.value)?;
        let (properties, materialized) = source_properties_after_vector_materialization(
            &properties,
            helix::db::VectorElementType::Node,
            node_id,
            &vector_definitions,
            &mut legacy_vector_rows,
            &mut managed_vector_index_ids,
        )?;
        materialized_node_vector_properties = materialized_node_vector_properties
            .checked_add(materialized)
            .context("materialized node-vector property count overflowed u64")?;
        writers.nodes.push(Record::new(
            node_id.to_be_bytes(),
            serde_json::to_vec(&properties)?,
        ))?;
    }

    let mut maximum_edge_id = None;
    let mut endpoints = raw
        .scan_prefix_with_options(
            graph::key_space_prefix(graph::KeySpace::EdgeEndpoints),
            &options,
        )
        .await?;
    let mut next_endpoint = endpoints.next().await?;
    let mut edges = raw
        .scan_prefix_with_options(
            graph::key_space_prefix(graph::KeySpace::EdgeProperty),
            &options,
        )
        .await?;
    while let Some(kv) = edges.next().await? {
        match kv.key.len() {
            len if len == PREFIX_LEN + ID_LEN => {
                let edge_id = one_id_key(&kv.key, "source edge property")?;
                maximum_edge_id =
                    Some(maximum_edge_id.map_or(edge_id, |current: u64| current.max(edge_id)));
                let Some(endpoint) = &next_endpoint else {
                    bail!("source edge {edge_id} has properties but no endpoint row");
                };
                let endpoint_id = one_id_key(&endpoint.key, "source edge endpoints")?;
                let endpoint_bytes = match endpoint_id.cmp(&edge_id) {
                    std::cmp::Ordering::Less => {
                        bail!("source endpoint row {endpoint_id} has no matching edge property");
                    }
                    std::cmp::Ordering::Equal => endpoint.value.clone(),
                    std::cmp::Ordering::Greater => {
                        bail!("source edge {edge_id} has properties but no endpoint row");
                    }
                };
                next_endpoint = endpoints.next().await?;
                let (from, to) = decode_endpoints(&endpoint_bytes, "source edge endpoints")?;
                let properties = graph::decode_properties(&kv.value)?;
                writers
                    .current_edge_equivalents
                    .as_mut()
                    .context("source oracle is missing its current-edge equivalence sorter")?
                    .push(logical_edge_record(
                        from,
                        to,
                        serde_json::to_vec(&source_parity_properties(&properties))?,
                    ))?;
                let (properties, materialized) = source_properties_after_vector_materialization(
                    &properties,
                    helix::db::VectorElementType::Edge,
                    edge_id,
                    &vector_definitions,
                    &mut legacy_vector_rows,
                    &mut managed_vector_index_ids,
                )?;
                materialized_edge_vector_properties = materialized_edge_vector_properties
                    .checked_add(materialized)
                    .context("materialized edge-vector property count overflowed u64")?;
                let encoded_properties = serde_json::to_vec(&properties)?;
                writers.current_edges.push(logical_edge_record(
                    from,
                    to,
                    encoded_properties.clone(),
                ))?;
                writers.edges_by_id.push(edge_by_id_record(
                    edge_id,
                    from,
                    to,
                    encoded_properties,
                ))?;
            }
            len if len == PREFIX_LEN + ID_LEN * 2 => {
                let (from, to) = two_id_key(&kv.key, "source legacy edge property")?;
                let properties = graph::decode_properties(&kv.value)?;
                writers
                    .legacy_edges
                    .as_mut()
                    .context("source oracle is missing its legacy sorter")?
                    .push(logical_edge_record(
                        from,
                        to,
                        serde_json::to_vec(&source_parity_properties(&properties))?,
                    ))?;
            }
            actual => bail!(
                "source edge property key has invalid length {actual}; expected {} or {}",
                PREFIX_LEN + ID_LEN,
                PREFIX_LEN + ID_LEN * 2
            ),
        }
    }
    if let Some(endpoint) = next_endpoint {
        let endpoint_id = one_id_key(&endpoint.key, "source edge endpoints")?;
        bail!("source endpoint row {endpoint_id} has no matching edge property");
    }

    let mut raw_hash_key_manifest = BTreeMap::<String, RawKeyFamilyAccumulator>::new();
    let mut property_indexes = raw
        .scan_prefix_with_options(
            graph::key_space_prefix(graph::KeySpace::PropertyIndex),
            &options,
        )
        .await?;
    while let Some(kv) = property_indexes.next().await? {
        let family = raw_hash_key_family(&kv.key);
        raw_hash_key_manifest
            .entry(family)
            .or_default()
            .include(&kv.key, &kv.value)?;
    }

    let mut source_physical_rows = 0_u64;
    let mut source_physical_bytes = 0_u64;
    let mut all = raw.scan_with_options::<Bytes, _>(.., &options).await?;
    while let Some(kv) = all.next().await? {
        source_physical_rows = source_physical_rows.saturating_add(1);
        source_physical_bytes = source_physical_bytes
            .checked_add(u64::try_from(kv.key.len().saturating_add(kv.value.len()))?)
            .context("source physical byte count overflowed u64")?;
        if exact_passthrough_key(&kv.key) {
            writers.exact_keys.push(Record::new(
                kv.key.clone(),
                source_exact_value(&kv.key, &kv.value)?,
            ))?;
        }
    }
    let node_stats = writers.nodes.finish(&paths.source_nodes)?;
    let current_edge_stats = writers.current_edges.finish(&paths.source_current_edges)?;
    writers
        .current_edge_equivalents
        .context("source oracle is missing its current-edge equivalence sorter")?
        .finish(&paths.source_current_edge_equivalents)?;
    let legacy_edge_stats = writers
        .legacy_edges
        .context("source oracle is missing its legacy sorter")?
        .finish(&paths.source_legacy_edges)?;
    let expected_edges = merge_current_and_legacy_edges_with_equivalence(
        &paths.source_current_edges,
        &paths.source_current_edge_equivalents,
        &paths.source_legacy_edges,
        &paths.source_expected_edges,
    )?;
    let edges_by_id = writers.edges_by_id.finish(&paths.source_edges_by_id)?;
    let exact_keys = writers.exact_keys.finish(&paths.source_exact)?;
    let node_allocator_watermark = raw
        .get(b"\xffnext_node_id")
        .await?
        .map(|bytes| decode_watermark(&bytes, "source node allocator"))
        .transpose()?;
    let edge_allocator_watermark = raw
        .get(b"\xffnext_edge_id")
        .await?
        .map(|bytes| decode_watermark(&bytes, "source edge allocator"))
        .transpose()?;
    let unmatched_legacy_vector_rows = u64::try_from(
        legacy_vector_rows
            .keys()
            .filter(|(index_id, _)| managed_vector_index_ids.contains(index_id))
            .count(),
    )?;
    let preserved_unmanaged_legacy_vector_rows = u64::try_from(
        legacy_vector_rows
            .keys()
            .filter(|(index_id, _)| !managed_vector_index_ids.contains(index_id))
            .count(),
    )?;
    Ok(OracleBuildStats {
        nodes: node_stats,
        current_edges: current_edge_stats,
        legacy_edges: Some(legacy_edge_stats),
        expected_edges: Some(expected_edges),
        edges_by_id,
        exact_keys,
        expected_indexes: None,
        actual_indexes: None,
        maximum_node_id,
        node_allocator_watermark,
        maximum_edge_id,
        edge_allocator_watermark,
        expected_graph_state: None,
        actual_graph_state: None,
        raw_hash_key_manifest: Some(
            raw_hash_key_manifest
                .into_iter()
                .map(|(family, evidence)| (family, evidence.finish()))
                .collect(),
        ),
        source_physical_rows: Some(source_physical_rows),
        source_physical_bytes: Some(source_physical_bytes),
        legacy_vector_rows: Some(legacy_vector_row_count),
        materialized_node_vector_properties: Some(materialized_node_vector_properties),
        materialized_edge_vector_properties: Some(materialized_edge_vector_properties),
        unmatched_legacy_vector_rows: Some(unmatched_legacy_vector_rows),
        preserved_unmanaged_legacy_vector_rows: Some(preserved_unmanaged_legacy_vector_rows),
    })
}

pub(crate) async fn build_target(
    db: &HelixDB,
    directory: &Path,
    paths: &OraclePaths,
    buffer_bytes: NonZeroUsize,
) -> Result<OracleBuildStats> {
    let raw = db.migration_parity_inner_db()?;
    let secondary_definitions =
        active_secondary_definitions(&db.migration_parity_v2_state().await?)?;
    let mut writers = Writers::new(directory, "target", buffer_bytes, false)?;
    let config = SortConfig { buffer_bytes };
    let mut expected_indexes = ExternalSorter::new(directory, "target-expected-indexes", config)?;
    let mut actual_indexes = ExternalSorter::new(directory, "target-actual-indexes", config)?;
    let mut expected_graph_state =
        ExternalSorter::new(directory, "target-expected-graph-state", config)?;
    let mut actual_graph_state =
        ExternalSorter::new(directory, "target-actual-graph-state", config)?;
    let options = migration_parity::migration_parity_scan_options(
        ORACLE_READ_AHEAD_BYTES,
        ORACLE_MAXIMUM_FETCH_TASKS,
    );

    let mut nodes = raw
        .scan_prefix_with_options(Bytes::from_static(&[0x02]), .., &options)
        .await?;
    let mut maximum_node_id = None;
    while let Some(kv) = nodes.next().await? {
        let node_id = one_id_key(&kv.key, "target node property")?;
        maximum_node_id =
            Some(maximum_node_id.map_or(node_id, |current: u64| current.max(node_id)));
        let properties = migration_parity::decode_parity_properties(&kv.value)?;
        writers.nodes.push(Record::new(
            node_id.to_be_bytes(),
            serde_json::to_vec(&properties)?,
        ))?;
        push_expected_node_indexes(
            node_id,
            &properties,
            &secondary_definitions,
            &mut expected_indexes,
        )?;
    }

    let mut maximum_edge_id = None;
    let mut endpoints = raw
        .scan_prefix_with_options(Bytes::from_static(&[0x04]), .., &options)
        .await?;
    let mut next_endpoint = endpoints.next().await?;
    let mut edges = raw
        .scan_prefix_with_options(Bytes::from_static(&[0x01]), .., &options)
        .await?;
    while let Some(kv) = edges.next().await? {
        match kv.key.len() {
            len if len == PREFIX_LEN + ID_LEN => {
                let edge_id = one_id_key(&kv.key, "target edge property")?;
                maximum_edge_id =
                    Some(maximum_edge_id.map_or(edge_id, |current: u64| current.max(edge_id)));
                let Some(endpoint) = &next_endpoint else {
                    bail!("target edge {edge_id} has properties but no endpoint row");
                };
                let endpoint_id = one_id_key(&endpoint.key, "target edge endpoints")?;
                let endpoint_bytes = match endpoint_id.cmp(&edge_id) {
                    std::cmp::Ordering::Less => {
                        bail!("target endpoint row {endpoint_id} has no matching edge property");
                    }
                    std::cmp::Ordering::Equal => endpoint.value.clone(),
                    std::cmp::Ordering::Greater => {
                        bail!("target edge {edge_id} has properties but no endpoint row");
                    }
                };
                next_endpoint = endpoints.next().await?;
                let (from, to) = decode_endpoints(&endpoint_bytes, "target edge endpoints")?;
                let properties = migration_parity::decode_parity_properties(&kv.value)?;
                let encoded_properties = serde_json::to_vec(&properties)?;
                writers.current_edges.push(logical_edge_record(
                    from,
                    to,
                    encoded_properties.clone(),
                ))?;
                writers.edges_by_id.push(edge_by_id_record(
                    edge_id,
                    from,
                    to,
                    encoded_properties,
                ))?;
                push_expected_edge_indexes(
                    edge_id,
                    &properties,
                    &secondary_definitions,
                    &mut expected_indexes,
                )?;
                expected_graph_state.push(adjacency_membership_record(b'O', from, to))?;
                expected_graph_state.push(adjacency_membership_record(b'I', to, from))?;
                expected_graph_state.push(pair_membership_record(from, to, edge_id))?;
            }
            len if len == PREFIX_LEN + ID_LEN * 2 => {
                bail!(
                    "target cleanup left legacy edge property key {}",
                    hex::encode(kv.key)
                );
            }
            actual => bail!(
                "target edge property key has invalid length {actual}; expected {}",
                PREFIX_LEN + ID_LEN
            ),
        }
    }
    if let Some(endpoint) = next_endpoint {
        let endpoint_id = one_id_key(&endpoint.key, "target edge endpoints")?;
        bail!("target endpoint row {endpoint_id} has no matching edge property");
    }

    let mut all = raw.scan_with_options(.., &options).await?;
    while let Some(kv) = all.next().await? {
        let identity = migration_parity::migration_parity_legacy_tenant_key_identity(&kv.key);
        if exact_passthrough_key(&identity) {
            let value = target_exact_value(&identity, &kv.value)?;
            writers
                .exact_keys
                .push(Record::new(identity.into_owned(), value))?;
        }
    }

    let mut indexes = raw
        .scan_prefix_with_options(Bytes::from_static(&[0x06]), .., &options)
        .await?;
    while let Some(kv) = indexes.next().await? {
        for membership in migration_parity::decode_migration_parity_secondary_memberships(
            &kv.key, &kv.value,
        )? {
            actual_indexes.push(secondary_membership_record(&membership)?)?;
        }
    }

    let mut adjacency = raw
        .scan_prefix_with_options(Bytes::from_static(&[0x00]), .., &options)
        .await?;
    while let Some(kv) = adjacency.next().await? {
        let node_id = one_id_key(&kv.key, "target adjacency")?;
        let (outgoing, incoming) = migration_parity::decode_parity_adjacency(&kv.value)?;
        for neighbor in outgoing {
            actual_graph_state.push(adjacency_membership_record(b'O', node_id, neighbor))?;
        }
        for neighbor in incoming {
            actual_graph_state.push(adjacency_membership_record(b'I', node_id, neighbor))?;
        }
    }

    let mut pairs = raw
        .scan_prefix_with_options(Bytes::from_static(&[0x05]), .., &options)
        .await?;
    while let Some(kv) = pairs.next().await? {
        let (from, to) = two_id_key(&kv.key, "target pair index")?;
        for edge_id in migration_parity::decode_parity_bitmap(&kv.value)? {
            actual_graph_state.push(pair_membership_record(from, to, edge_id))?;
        }
    }

    let expected_pre_dedup = directory.join("target-expected-indexes.pre-dedup.sorted");
    expected_indexes.finish(&expected_pre_dedup)?;
    let expected_index_stats =
        deduplicate_sorted(&expected_pre_dedup, &paths.target_expected_indexes)?;
    std::fs::remove_file(&expected_pre_dedup).with_context(|| {
        format!(
            "failed to remove pre-dedup index stream {}",
            expected_pre_dedup.display()
        )
    })?;
    let expected_graph_pre_dedup = directory.join("target-expected-graph-state.pre-dedup.sorted");
    expected_graph_state.finish(&expected_graph_pre_dedup)?;
    let expected_graph_state_stats = deduplicate_sorted(
        &expected_graph_pre_dedup,
        &paths.target_expected_graph_state,
    )?;
    std::fs::remove_file(&expected_graph_pre_dedup).with_context(|| {
        format!(
            "failed to remove pre-dedup graph-state stream {}",
            expected_graph_pre_dedup.display()
        )
    })?;

    let node_allocator_watermark = raw
        .get(b"\xffnext_node_id")
        .await?
        .map(|bytes| decode_watermark(&bytes, "target node allocator"))
        .transpose()?;
    if let Some(maximum) = maximum_node_id
        && node_allocator_watermark.is_none_or(|watermark| watermark <= maximum)
    {
        bail!(
            "target node allocator watermark {node_allocator_watermark:?} is not above maximum node id {maximum}"
        );
    }
    let edge_allocator_watermark = raw
        .get(b"\xffnext_edge_id")
        .await?
        .map(|bytes| decode_watermark(&bytes, "target edge allocator"))
        .transpose()?;
    if let Some(maximum) = maximum_edge_id
        && edge_allocator_watermark.is_none_or(|watermark| watermark <= maximum)
    {
        bail!(
            "target edge allocator watermark {edge_allocator_watermark:?} is not above maximum edge id {maximum}"
        );
    }

    Ok(OracleBuildStats {
        nodes: writers.nodes.finish(&paths.target_nodes)?,
        current_edges: writers.current_edges.finish(&paths.target_edges)?,
        legacy_edges: None,
        expected_edges: None,
        edges_by_id: writers.edges_by_id.finish(&paths.target_edges_by_id)?,
        exact_keys: writers.exact_keys.finish(&paths.target_exact)?,
        expected_indexes: Some(expected_index_stats),
        actual_indexes: Some(actual_indexes.finish(&paths.target_actual_indexes)?),
        maximum_node_id,
        node_allocator_watermark,
        maximum_edge_id,
        edge_allocator_watermark,
        expected_graph_state: Some(expected_graph_state_stats),
        actual_graph_state: Some(actual_graph_state.finish(&paths.target_actual_graph_state)?),
        raw_hash_key_manifest: None,
        source_physical_rows: None,
        source_physical_bytes: None,
        legacy_vector_rows: None,
        materialized_node_vector_properties: None,
        materialized_edge_vector_properties: None,
        unmatched_legacy_vector_rows: None,
        preserved_unmanaged_legacy_vector_rows: None,
    })
}

fn decode_watermark(bytes: &[u8], context: &str) -> Result<u64> {
    const WATERMARK_LEN: usize = core::mem::size_of::<u64>();
    if bytes.len() != WATERMARK_LEN {
        bail!(
            "{context} watermark has invalid length {}; expected {WATERMARK_LEN}",
            bytes.len()
        );
    }
    Ok(u64::from_be_bytes(
        bytes[0..WATERMARK_LEN]
            .try_into()
            .expect("validated watermark slice is eight bytes"),
    ))
}

pub(crate) fn compare(paths: &OraclePaths) -> Result<OracleComparison> {
    Ok(OracleComparison {
        nodes: compare_sorted(
            &paths.source_nodes,
            &paths.target_nodes,
            FIRST_DIFFERENCE_LIMIT,
        )?,
        logical_edges: compare_sorted(
            &paths.source_expected_edges,
            &paths.target_edges,
            FIRST_DIFFERENCE_LIMIT,
        )?,
        preexisting_edges_by_id: compare_sorted_with_policy(
            &paths.source_edges_by_id,
            &paths.target_edges_by_id,
            FIRST_DIFFERENCE_LIMIT,
            ComparisonPolicy::SourceSubset,
        )?,
        exact_passthrough_keys: compare_sorted(
            &paths.source_exact,
            &paths.target_exact,
            FIRST_DIFFERENCE_LIMIT,
        )?,
        indexes: compare_sorted(
            &paths.target_expected_indexes,
            &paths.target_actual_indexes,
            FIRST_DIFFERENCE_LIMIT,
        )?,
        graph_state: compare_sorted(
            &paths.target_expected_graph_state,
            &paths.target_actual_graph_state,
            FIRST_DIFFERENCE_LIMIT,
        )?,
    })
}

pub(crate) fn compare_source_durability(
    live: &OraclePaths,
    reopened: &OraclePaths,
) -> Result<SourceDurabilityComparison> {
    Ok(SourceDurabilityComparison {
        nodes: compare_sorted(
            &live.source_nodes,
            &reopened.source_nodes,
            FIRST_DIFFERENCE_LIMIT,
        )?,
        current_edges: compare_sorted(
            &live.source_current_edges,
            &reopened.source_current_edges,
            FIRST_DIFFERENCE_LIMIT,
        )?,
        legacy_edges: compare_sorted(
            &live.source_legacy_edges,
            &reopened.source_legacy_edges,
            FIRST_DIFFERENCE_LIMIT,
        )?,
        expected_edges: compare_sorted(
            &live.source_expected_edges,
            &reopened.source_expected_edges,
            FIRST_DIFFERENCE_LIMIT,
        )?,
        edges_by_id: compare_sorted(
            &live.source_edges_by_id,
            &reopened.source_edges_by_id,
            FIRST_DIFFERENCE_LIMIT,
        )?,
        exact_passthrough_keys: compare_sorted(
            &live.source_exact,
            &reopened.source_exact,
            FIRST_DIFFERENCE_LIMIT,
        )?,
    })
}

pub(crate) fn compare_target_durability(
    before: &OraclePaths,
    after: &OraclePaths,
) -> Result<TargetDurabilityComparison> {
    Ok(TargetDurabilityComparison {
        nodes: compare_sorted(
            &before.target_nodes,
            &after.target_nodes,
            FIRST_DIFFERENCE_LIMIT,
        )?,
        current_edges: compare_sorted(
            &before.target_edges,
            &after.target_edges,
            FIRST_DIFFERENCE_LIMIT,
        )?,
        edges_by_id: compare_sorted(
            &before.target_edges_by_id,
            &after.target_edges_by_id,
            FIRST_DIFFERENCE_LIMIT,
        )?,
        exact_passthrough_keys: compare_sorted(
            &before.target_exact,
            &after.target_exact,
            FIRST_DIFFERENCE_LIMIT,
        )?,
        expected_indexes: compare_sorted(
            &before.target_expected_indexes,
            &after.target_expected_indexes,
            FIRST_DIFFERENCE_LIMIT,
        )?,
        actual_indexes: compare_sorted(
            &before.target_actual_indexes,
            &after.target_actual_indexes,
            FIRST_DIFFERENCE_LIMIT,
        )?,
        expected_graph_state: compare_sorted(
            &before.target_expected_graph_state,
            &after.target_expected_graph_state,
            FIRST_DIFFERENCE_LIMIT,
        )?,
        actual_graph_state: compare_sorted(
            &before.target_actual_graph_state,
            &after.target_actual_graph_state,
            FIRST_DIFFERENCE_LIMIT,
        )?,
    })
}

fn adjacency_membership_record(direction: u8, node: u64, neighbor: u64) -> Record {
    let mut key = Vec::with_capacity(PREFIX_LEN + ID_LEN * 2);
    key.push(direction);
    key.extend_from_slice(&node.to_be_bytes());
    key.extend_from_slice(&neighbor.to_be_bytes());
    Record::new(key, Vec::new())
}

fn pair_membership_record(from: u64, to: u64, edge_id: u64) -> Record {
    let mut key = Vec::with_capacity(PREFIX_LEN + ID_LEN * 3);
    key.push(b'P');
    key.extend_from_slice(&from.to_be_bytes());
    key.extend_from_slice(&to.to_be_bytes());
    key.extend_from_slice(&edge_id.to_be_bytes());
    Record::new(key, Vec::new())
}

fn logical_edge_record(from: u64, to: u64, encoded_properties: Vec<u8>) -> Record {
    let mut key = Vec::with_capacity(ID_LEN * 2);
    key.extend_from_slice(&from.to_be_bytes());
    key.extend_from_slice(&to.to_be_bytes());
    Record::new(key, encoded_properties)
}

fn edge_by_id_record(edge_id: u64, from: u64, to: u64, encoded_properties: Vec<u8>) -> Record {
    let mut value = Vec::with_capacity(ENDPOINT_BYTES + encoded_properties.len());
    value.extend_from_slice(&from.to_be_bytes());
    value.extend_from_slice(&to.to_be_bytes());
    value.extend_from_slice(&encoded_properties);
    Record::new(edge_id.to_be_bytes(), value)
}

fn one_id_key(key: &[u8], description: &str) -> Result<u64> {
    if key.len() != PREFIX_LEN + ID_LEN {
        bail!(
            "{description} key has invalid length {}; expected {}",
            key.len(),
            PREFIX_LEN + ID_LEN
        );
    }
    Ok(u64::from_be_bytes(
        key[PREFIX_LEN..PREFIX_LEN + ID_LEN]
            .try_into()
            .expect("validated one-id key has exactly eight id bytes"),
    ))
}

fn two_id_key(key: &[u8], description: &str) -> Result<(u64, u64)> {
    if key.len() != PREFIX_LEN + ID_LEN * 2 {
        bail!(
            "{description} key has invalid length {}; expected {}",
            key.len(),
            PREFIX_LEN + ID_LEN * 2
        );
    }
    Ok((
        u64::from_be_bytes(
            key[PREFIX_LEN..PREFIX_LEN + ID_LEN]
                .try_into()
                .expect("validated two-id key has a complete first id"),
        ),
        u64::from_be_bytes(
            key[PREFIX_LEN + ID_LEN..PREFIX_LEN + ID_LEN + ID_LEN]
                .try_into()
                .expect("validated two-id key has a complete second id"),
        ),
    ))
}

fn decode_endpoints(bytes: &[u8], description: &str) -> Result<(u64, u64)> {
    if bytes.len() != ENDPOINT_BYTES {
        bail!(
            "{description} value has invalid length {}; expected {ENDPOINT_BYTES}",
            bytes.len()
        );
    }
    Ok((
        u64::from_be_bytes(
            bytes[0..ID_LEN]
                .try_into()
                .expect("validated endpoints have a complete source id"),
        ),
        u64::from_be_bytes(
            bytes[ID_LEN..ID_LEN + ID_LEN]
                .try_into()
                .expect("validated endpoints have a complete target id"),
        ),
    ))
}

fn raw_hash_key_family(key: &[u8]) -> String {
    match key.get(1).copied() {
        Some(0x00) => "node_equality".to_string(),
        Some(0x01) => "node_range_asc".to_string(),
        Some(0x02) => "edge_equality".to_string(),
        Some(0x03) => "edge_range_asc".to_string(),
        Some(0x04) => "edge_global_label".to_string(),
        Some(0x05) => "node_range_desc".to_string(),
        Some(0x06) => "edge_range_desc".to_string(),
        Some(other) => format!("property_index_0x{other:02x}"),
        None => "property_index_truncated".to_string(),
    }
}

fn push_expected_node_indexes(
    node_id: u64,
    properties: &[ParityProperty],
    definitions: &[SecondaryDefinition],
    output: &mut ExternalSorter,
) -> Result<()> {
    push_expected_secondary_memberships(node_id, properties, definitions, true, output)
}

fn push_expected_edge_indexes(
    edge_id: u64,
    properties: &[ParityProperty],
    definitions: &[SecondaryDefinition],
    output: &mut ExternalSorter,
) -> Result<()> {
    push_expected_secondary_memberships(edge_id, properties, definitions, false, output)
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum SecondaryLane {
    NodeEquality,
    NodeUniqueEquality,
    NodeRangeAscending,
    NodeRangeDescending,
    EdgeEquality,
    EdgeRangeAscending,
    EdgeRangeDescending,
}

impl SecondaryLane {
    const fn as_u8(self) -> u8 {
        match self {
            Self::NodeEquality => 1,
            Self::NodeUniqueEquality => 2,
            Self::NodeRangeAscending => 3,
            Self::NodeRangeDescending => 4,
            Self::EdgeEquality => 5,
            Self::EdgeRangeAscending => 6,
            Self::EdgeRangeDescending => 7,
        }
    }

    const fn equality(self) -> bool {
        matches!(
            self,
            Self::NodeEquality | Self::NodeUniqueEquality | Self::EdgeEquality
        )
    }

    const fn range_direction(self) -> Option<RangeDirection> {
        match self {
            Self::NodeRangeAscending | Self::EdgeRangeAscending => Some(RangeDirection::Ascending),
            Self::NodeRangeDescending | Self::EdgeRangeDescending => {
                Some(RangeDirection::Descending)
            }
            Self::NodeEquality | Self::NodeUniqueEquality | Self::EdgeEquality => None,
        }
    }
}

#[derive(Debug, Clone)]
struct SecondaryDefinition {
    index_id: u64,
    generation: u64,
    lane: SecondaryLane,
    node: bool,
    label: String,
    property: String,
}

fn active_secondary_definitions(
    state: &MigrationParityV2State,
) -> Result<Vec<SecondaryDefinition>> {
    state
        .canonical_records
        .iter()
        .filter(|record| {
            record.state == "active"
                && record.definition.get("family").map(String::as_str) == Some("secondary")
        })
        .map(|record| {
            let element_kind = record
                .definition
                .get("element_kind")
                .context("secondary definition is missing element_kind")?;
            let unique = record
                .definition
                .get("unique")
                .context("secondary definition is missing unique")?
                .parse::<bool>()
                .context("secondary definition has invalid unique")?;
            let direction = record
                .definition
                .get("direction")
                .context("secondary definition is missing direction")?;
            let lane = match (element_kind.as_str(), unique, direction.as_str()) {
                ("Node", false, "Asc") if record.identity.starts_with("SecondaryEquality:") => {
                    SecondaryLane::NodeEquality
                }
                ("Node", true, "Asc") if record.identity.starts_with("SecondaryEquality:") => {
                    SecondaryLane::NodeUniqueEquality
                }
                ("Node", false, "Asc") => SecondaryLane::NodeRangeAscending,
                ("Node", false, "Desc") => SecondaryLane::NodeRangeDescending,
                ("Edge", false, "Asc") if record.identity.starts_with("SecondaryEquality:") => {
                    SecondaryLane::EdgeEquality
                }
                ("Edge", false, "Asc") => SecondaryLane::EdgeRangeAscending,
                ("Edge", false, "Desc") => SecondaryLane::EdgeRangeDescending,
                _ => bail!("unsupported canonical secondary definition: {record:?}"),
            };
            Ok(SecondaryDefinition {
                index_id: record.index_id,
                generation: record.generation,
                lane,
                node: element_kind == "Node",
                label: record
                    .definition
                    .get("label")
                    .context("secondary definition is missing label")?
                    .clone(),
                property: record
                    .definition
                    .get("property")
                    .context("secondary definition is missing property")?
                    .clone(),
            })
        })
        .collect()
}

fn push_expected_secondary_memberships(
    entity_id: u64,
    properties: &[ParityProperty],
    definitions: &[SecondaryDefinition],
    node: bool,
    output: &mut ExternalSorter,
) -> Result<()> {
    let Some(label) = parity_label(properties) else {
        return Ok(());
    };
    for definition in definitions
        .iter()
        .filter(|definition| definition.node == node && definition.label == label)
    {
        let Some(property) = properties
            .iter()
            .find(|property| property.name == definition.property)
        else {
            continue;
        };
        let value = if definition.lane.equality() {
            match project_equality(&property.value) {
                EqualityProjection::Indexed { digest, canonical } => {
                    MigrationParitySecondaryValue::Equality { digest, canonical }
                }
                EqualityProjection::Absent => continue,
                EqualityProjection::Unsupported(kind) => bail!(
                    "active secondary definition {} indexes unsupported equality value {kind}",
                    definition.index_id
                ),
                EqualityProjection::Oversized {
                    encoded_len,
                    maximum,
                } => bail!(
                    "active secondary definition {} indexes oversized equality value: {encoded_len} > {maximum}",
                    definition.index_id
                ),
            }
        } else {
            let direction = definition
                .lane
                .range_direction()
                .expect("non-equality secondary lane has a range direction");
            match project_range(&property.value, direction) {
                RangeProjection::Indexed(encoded) => {
                    MigrationParitySecondaryValue::Range(encoded)
                }
                RangeProjection::NaN => bail!(
                    "active secondary definition {} indexes NaN",
                    definition.index_id
                ),
                RangeProjection::Unsupported(kind) => bail!(
                    "active secondary definition {} indexes unsupported range value {kind}",
                    definition.index_id
                ),
                RangeProjection::Oversized {
                    encoded_len,
                    maximum,
                } => bail!(
                    "active secondary definition {} indexes oversized range value: {encoded_len} > {maximum}",
                    definition.index_id
                ),
            }
        };
        let membership = MigrationParitySecondaryMembership {
            index_id: definition.index_id,
            generation: definition.generation,
            lane: definition.lane.as_u8(),
            value,
            entity_id,
        };
        output.push(secondary_membership_record(&membership)?)?;
    }
    Ok(())
}

fn secondary_membership_record(membership: &MigrationParitySecondaryMembership) -> Result<Record> {
    Ok(Record::new(
        serde_json::to_vec(&(
            membership.index_id,
            membership.generation,
            membership.lane,
            &membership.value,
        ))?,
        membership.entity_id.to_be_bytes(),
    ))
}

fn parity_label(properties: &[ParityProperty]) -> Option<&str> {
    properties.iter().find_map(|property| {
        (property.name == "$label")
            .then_some(&property.value)
            .and_then(|value| match value {
                ParityValue::String(label) => Some(label.as_str()),
                _ => None,
            })
    })
}

fn exact_passthrough_key(key: &[u8]) -> bool {
    let Some(prefix) = key.first().copied() else {
        return true;
    };
    match prefix {
        0x00 | 0x01 | 0x03 | 0x04 | 0x05 | 0x06 | 0xF0 | 0xF1 | 0xFE => false,
        0xFF => {
            let name = &key[PREFIX_LEN..];
            name != b"next_node_id"
                && name != b"next_edge_id"
                && !name.starts_with(b"dynamic_index:")
                && !name.starts_with(b"dynamic_index_")
                && !name.starts_with(b"kv_migration_job:")
                && !name.starts_with(b"kv_migration_ready:")
                && !name.starts_with(b"storage_schema_complete:")
                && !name.starts_with(b"text_")
                && !name.starts_with(b"vector_")
                && key != VECTOR_SIMHASH_DIRECTORY_MIGRATION_KEY
        }
        // Legacy indexed vectors are intentionally materialized into node and
        // edge property rows before V2 adopts the indexes. Those rows are
        // compared by the logical node/edge streams, not as byte passthrough.
        0x02 => false,
        _ => true,
    }
}

fn legacy_vector_row_identity(key: &[u8]) -> Option<(u64, u64)> {
    const LEGACY_VECTOR_ROW_LEN: usize = 1 + ID_LEN + 1 + ID_LEN + ID_LEN;
    const LEGACY_VECTOR_KEYSPACE: u8 = 0xF1;
    const LEGACY_VECTOR_KIND: u8 = 0x02;
    if key.len() != LEGACY_VECTOR_ROW_LEN
        || key[0] != LEGACY_VECTOR_KEYSPACE
        || key[1 + ID_LEN] != LEGACY_VECTOR_KIND
    {
        return None;
    }
    let index_id = u64::from_be_bytes(
        key[1..1 + ID_LEN]
            .try_into()
            .expect("validated legacy vector index-id slice is eight bytes"),
    );
    let entity_id = u64::from_be_bytes(
        key[LEGACY_VECTOR_ROW_LEN - ID_LEN..]
            .try_into()
            .expect("validated legacy vector entity-id slice is eight bytes"),
    );
    Some((index_id, entity_id))
}

fn source_properties_after_vector_materialization(
    properties: &[HProperty],
    element_type: helix::db::VectorElementType,
    entity_id: u64,
    definitions: &[VectorIndexDefinition],
    legacy_vector_rows: &mut BTreeMap<(u64, u64), Vec<u8>>,
    managed_vector_index_ids: &mut BTreeSet<u64>,
) -> Result<(Vec<ParityProperty>, u64)> {
    let label = properties
        .iter()
        .find(|property| property.name == "$label")
        .and_then(|property| match &property.value {
            HPropertyValue::String(label) => Some(label.as_str()),
            _ => None,
        });
    let mut materialized = source_parity_properties(properties);
    let mut materialized_count = 0_u64;
    for definition in definitions
        .iter()
        .filter(|definition| definition.element_type == element_type)
        .filter(|definition| label == Some(definition.label.as_str()))
    {
        // This order is part of Proper's migration contract: discard any stale
        // graph copy first, then append the authoritative legacy HNSW value.
        materialized.retain(|property| property.name != definition.property);
        let physical_name = match definition.tenant_property.as_deref() {
            None => helix::db::index::vector_index_name(
                definition.element_type,
                &definition.label,
                &definition.property,
            ),
            Some(tenant_property) => {
                let Some(tenant_value) = properties
                    .iter()
                    .find(|property| property.name == tenant_property)
                    .map(|property| &property.value)
                    .and_then(helix::db::index::text::normalize_tenant_value)
                else {
                    continue;
                };
                helix::db::index::vector_tenant_index_name(
                    definition.element_type,
                    &definition.label,
                    &definition.property,
                    tenant_property,
                    tenant_value,
                )
            }
        };
        let index_id = helix::db::index::hnsw::index_id_from_name(&physical_name);
        managed_vector_index_ids.insert(index_id);
        let Some(encoded) = legacy_vector_rows.remove(&(index_id, entity_id)) else {
            continue;
        };
        let vector = decode_legacy_vector(&encoded, definition.metric).with_context(|| {
            format!(
                "failed to decode legacy HNSW vector for {:?} {}.{} entity {entity_id}",
                definition.element_type, definition.label, definition.property
            )
        })?;
        if vector.len() != definition.dimension {
            bail!(
                "legacy HNSW vector for {:?} {}.{} entity {entity_id} has dimension {}, expected {}",
                definition.element_type,
                definition.label,
                definition.property,
                vector.len(),
                definition.dimension
            );
        }
        materialized.push(ParityProperty {
            name: definition.property.clone(),
            value: ParityValue::F32ArrayBits(vector.into_iter().map(f32::to_bits).collect()),
        });
        materialized_count = materialized_count
            .checked_add(1)
            .context("materialized vector-property count overflowed u64")?;
    }
    Ok((materialized, materialized_count))
}

fn decode_legacy_vector(encoded: &[u8], metric: VectorDistanceMetric) -> Result<Vec<f32>> {
    use helix::db::index::hnsw::distance::{Cosine, Euclidean, Manhattan};

    let vector = match metric {
        VectorDistanceMetric::Cosine => {
            helix::db::index::hnsw::decode_item::<Cosine>(encoded).map(|item| item.vector.to_vec())
        }
        VectorDistanceMetric::Euclidean => {
            helix::db::index::hnsw::decode_item::<Euclidean>(encoded)
                .map(|item| item.vector.to_vec())
        }
        VectorDistanceMetric::Manhattan => {
            helix::db::index::hnsw::decode_item::<Manhattan>(encoded)
                .map(|item| item.vector.to_vec())
        }
    };
    vector.map_err(anyhow::Error::msg)
}

fn source_exact_value(key: &[u8], value: &[u8]) -> Result<Vec<u8>> {
    let Some(index_id) = vector_metadata_index_id(key) else {
        return Ok(value.to_vec());
    };
    if let Ok(metadata) = helix::db::index::hnsw::decode_metadata(value)
        && helix::db::index::hnsw::index_id_from_name(&metadata.config.index_name) == index_id
    {
        let config = metadata.config;
        let current = migration_parity::MigrationParityVectorMetadata {
            index_name: config.index_name,
            property_name: config.property_name,
            dimension: config.dimension,
            m: config.m,
            m0: config.m0,
            ef_construction: config.ef_construction,
            ml: config.ml,
            simhash_threshold: config.simhash_threshold,
            sampling_ratio: config.sampling_ratio,
            adaptive_enabled: config.adaptive_enabled,
            adaptive_failure_prob: config.adaptive_failure_prob,
            entry_point: metadata.entry_point,
            max_layer: metadata.max_layer,
            count: metadata.count,
        };
        return Ok(migration_parity::migration_parity_encode_vector_metadata(
            current,
        ));
    }
    target_exact_value(key, value)
}

fn target_exact_value(key: &[u8], value: &[u8]) -> Result<Vec<u8>> {
    let Some(index_id) = vector_metadata_index_id(key) else {
        return Ok(value.to_vec());
    };
    let (index_name, normalized) =
        migration_parity::migration_parity_normalize_vector_metadata(value)?;
    let actual_index_id = db::search::vector::index_id_from_name(&index_name);
    if actual_index_id != index_id {
        bail!(
            "target vector metadata name '{}' hashes to {actual_index_id:#018x}, expected {index_id:#018x}",
            index_name
        );
    }
    Ok(normalized)
}

fn vector_metadata_index_id(key: &[u8]) -> Option<u64> {
    const KEYSPACE_PREFIX_LEN: usize = core::mem::size_of::<u8>();
    const INDEX_TYPE_LEN: usize = core::mem::size_of::<u8>();
    const INDEX_ID_LEN: usize = core::mem::size_of::<u64>();
    const KEY_KIND_LEN: usize = core::mem::size_of::<u8>();
    const VECTOR_METADATA_KEY_LEN: usize =
        KEYSPACE_PREFIX_LEN + INDEX_TYPE_LEN + INDEX_ID_LEN + KEY_KIND_LEN;
    const VECTOR_INDEX_TYPE: u8 = 0x03;
    const VECTOR_METADATA_KIND: u8 = 0x01;
    if key.len() != VECTOR_METADATA_KEY_LEN
        || key[0] != 0x03
        || key[KEYSPACE_PREFIX_LEN] != VECTOR_INDEX_TYPE
        || key[KEYSPACE_PREFIX_LEN + INDEX_TYPE_LEN + INDEX_ID_LEN] != VECTOR_METADATA_KIND
    {
        return None;
    }
    Some(u64::from_be_bytes(
        key[KEYSPACE_PREFIX_LEN + INDEX_TYPE_LEN
            ..KEYSPACE_PREFIX_LEN + INDEX_TYPE_LEN + INDEX_ID_LEN]
            .try_into()
            .expect("vector metadata index id slice is 8 bytes"),
    ))
}

fn source_parity_properties(properties: &[HProperty]) -> Vec<ParityProperty> {
    properties
        .iter()
        .map(|property| ParityProperty {
            name: property.name.clone(),
            value: source_parity_value(&property.value),
        })
        .collect()
}

fn source_parity_value(value: &HPropertyValue) -> ParityValue {
    match value {
        HPropertyValue::Null => ParityValue::Null,
        HPropertyValue::Bool(value) => ParityValue::Bool(*value),
        HPropertyValue::I64(value) => ParityValue::I64(*value),
        HPropertyValue::DateTime(value) => ParityValue::DateTime(*value),
        HPropertyValue::F64(value) => ParityValue::F64Bits(value.to_bits()),
        HPropertyValue::F32(value) => ParityValue::F32Bits(value.to_bits()),
        HPropertyValue::String(value) => ParityValue::String(value.clone()),
        HPropertyValue::Bytes(value) => ParityValue::Bytes(value.clone()),
        HPropertyValue::I64Array(value) => ParityValue::I64Array(value.clone()),
        HPropertyValue::F64Array(value) => {
            ParityValue::F64ArrayBits(value.iter().map(|value| value.to_bits()).collect())
        }
        HPropertyValue::F32Array(value) => {
            ParityValue::F32ArrayBits(value.iter().map(|value| value.to_bits()).collect())
        }
        HPropertyValue::StringArray(value) => ParityValue::StringArray(value.clone()),
        HPropertyValue::Array(value) => {
            ParityValue::Array(value.iter().map(source_parity_value).collect())
        }
        HPropertyValue::Object(value) => ParityValue::Object(
            value
                .iter()
                .map(|(key, value)| (key.clone(), source_parity_value(value)))
                .collect(),
        ),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn exact_key_classifier_includes_unknown_text_and_vector_but_not_rewrites() {
        assert!(!exact_passthrough_key(b"\x02node"));
        let vector_index_prefix = [&[0x03, 0x03][..], &7_u64.to_be_bytes()].concat();
        assert!(!exact_passthrough_key(&vector_index_prefix));
        assert!(!exact_passthrough_key(b"\x03\x03vector"));
        assert!(!exact_passthrough_key(b"\xF0vector"));
        assert!(!exact_passthrough_key(b"\xFFtext_manifest:test"));
        assert!(exact_passthrough_key(b"\x77unknown"));
        assert!(!exact_passthrough_key(b"\x00adjacency"));
        assert!(!exact_passthrough_key(b"\x03\x00equality"));
        assert!(!exact_passthrough_key(b"\xFFnext_node_id"));
        assert!(!exact_passthrough_key(b"\xFFnext_edge_id"));
        assert!(!exact_passthrough_key(
            b"\xFFkv_migration_job:graph_format_v1_rewrite"
        ));
        assert!(!exact_passthrough_key(b"\xFFstorage_schema_complete:v1"));
        assert!(!exact_passthrough_key(
            VECTOR_SIMHASH_DIRECTORY_MIGRATION_KEY
        ));
        assert!(exact_passthrough_key(
            b"\xFFkv_vector_simhash_directory_v1_unrelated"
        ));
    }

    #[test]
    fn migrated_tenant_passthrough_key_keeps_its_source_identity() {
        let mut source = vec![0x77; core::mem::size_of::<u128>()];
        source.push(0x02);
        source.extend_from_slice(&8_u64.to_be_bytes());
        let mut target = Vec::with_capacity(core::mem::size_of::<u8>() + source.len());
        target.push(0xFD);
        target.extend_from_slice(&source);

        assert!(exact_passthrough_key(&source));
        assert_eq!(
            migration_parity::migration_parity_legacy_tenant_key_identity(&target).as_ref(),
            source.as_slice()
        );
    }

    #[test]
    fn property_order_and_float_bits_are_canonicalized_without_sorting() {
        let properties = vec![
            HProperty::new("duplicate", HPropertyValue::F64(f64::from_bits(1))),
            HProperty::new("duplicate", HPropertyValue::F64(f64::from_bits(2))),
        ];
        let canonical = source_parity_properties(&properties);
        assert_eq!(canonical[0].value, ParityValue::F64Bits(1));
        assert_eq!(canonical[1].value, ParityValue::F64Bits(2));
    }

    #[test]
    fn legacy_vector_row_identity_rejects_noncanonical_hnsw_rows() {
        let index_id = 7_u64;
        let entity_id = 11_u64;
        let mut key = vec![0_u8; 26];
        key[0] = 0xF1;
        key[1..9].copy_from_slice(&index_id.to_be_bytes());
        key[9] = 0x02;
        key[10..18].copy_from_slice(&13_u64.to_be_bytes());
        key[18..26].copy_from_slice(&entity_id.to_be_bytes());
        assert_eq!(
            legacy_vector_row_identity(&key),
            Some((index_id, entity_id))
        );

        key[9] = 0x03;
        assert_eq!(legacy_vector_row_identity(&key), None);
        assert_eq!(legacy_vector_row_identity(&key[..25]), None);
    }

    #[test]
    fn source_oracle_derives_materialized_property_from_legacy_hnsw_bytes() {
        use helix::db::index::hnsw::distance::Euclidean;
        use helix::db::index::hnsw::{encode_item, index_id_from_name, Item};

        let definition = VectorIndexDefinition::new_node(
            "User",
            "embedding",
            3,
            VectorDistanceMetric::Euclidean,
        );
        let physical_name = helix::db::index::vector_index_name(
            definition.element_type,
            &definition.label,
            &definition.property,
        );
        let entity_id = 42_u64;
        let vector = vec![f32::from_bits(0x8000_0000), 1.5, -2.25];
        let encoded = encode_item(&Item::<Euclidean>::new(vector.clone()));
        let mut rows = BTreeMap::from([(
            (index_id_from_name(&physical_name), entity_id),
            encoded.to_vec(),
        )]);
        let mut managed_index_ids = BTreeSet::new();
        let source = vec![
            HProperty::new("$label", HPropertyValue::String("User".to_string())),
            HProperty::new("name", HPropertyValue::String("Ada".to_string())),
            HProperty::new("embedding", HPropertyValue::F32Array(vec![99.0; 3])),
        ];

        let (actual, count) = source_properties_after_vector_materialization(
            &source,
            helix::db::VectorElementType::Node,
            entity_id,
            &[definition],
            &mut rows,
            &mut managed_index_ids,
        )
        .expect("legacy vector materializes");

        assert_eq!(count, 1);
        assert!(rows.is_empty());
        assert_eq!(
            managed_index_ids,
            BTreeSet::from([index_id_from_name(&physical_name)])
        );
        assert_eq!(actual.len(), 3);
        assert_eq!(actual[0].name, "$label");
        assert_eq!(actual[1].name, "name");
        assert_eq!(actual[2].name, "embedding");
        assert_eq!(
            actual[2].value,
            ParityValue::F32ArrayBits(vector.into_iter().map(f32::to_bits).collect())
        );
    }
}
