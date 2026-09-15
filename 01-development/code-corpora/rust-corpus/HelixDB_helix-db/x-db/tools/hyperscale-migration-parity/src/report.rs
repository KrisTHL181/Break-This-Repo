use std::path::Path;
use std::process::Command;
use std::sync::atomic::{AtomicU64, Ordering};

use anyhow::{Context, Result};
use serde::{Deserialize, Serialize};

use crate::logical_oracle::{OracleBuildStats, OracleComparison, SourceDurabilityComparison};
use crate::object_store_metrics::ObjectStoreMetrics;

const AMPLIFICATION_GATE_MIN_BASELINE_ROWS: u64 = 500_000;
const EXPONENT_GATE_MIN_ROWS: u64 = 100_000;
static PEAK_SCRATCH_BYTES: AtomicU64 = AtomicU64::new(0);

#[derive(Debug, Clone, Serialize)]
pub(crate) struct RevisionEvidence {
    pub(crate) target_helix: String,
    pub(crate) target_helix_dirty: bool,
    pub(crate) source_hyperscale: String,
    pub(crate) source_hyperscale_dirty: bool,
    pub(crate) source_slatedb: String,
    pub(crate) target_slatedb: String,
}

#[derive(Debug, Clone, Serialize)]
pub(crate) struct HarnessConfig {
    pub(crate) profile: String,
    pub(crate) fixture_version: u32,
    pub(crate) database: String,
    pub(crate) batch_rows: u64,
    pub(crate) batch_source_bytes: u64,
    pub(crate) oracle_buffer_bytes_per_stream: u64,
    pub(crate) object_storage: String,
    pub(crate) added_latency_millis: u64,
    pub(crate) target_fault: Option<String>,
    pub(crate) migration_failpoint: Option<String>,
    pub(crate) maximum_open_attempts: u64,
    pub(crate) scale_nodes: u64,
    pub(crate) scale_edges: u64,
    pub(crate) seed_batch_rows: u64,
    pub(crate) distribution: String,
    pub(crate) resume_source_seed: bool,
    pub(crate) maximum_scenario_seconds: u64,
    pub(crate) maximum_suite_seconds: u64,
    pub(crate) compaction_drain_seconds: u64,
    pub(crate) maximum_steady_l0_ssts: u64,
    pub(crate) definition_migration_batch_rows: u64,
}

#[derive(Debug, Clone, Serialize)]
pub(crate) struct HostEvidence {
    pub(crate) logical_cpus: usize,
    pub(crate) total_memory_bytes: Option<u64>,
    pub(crate) scratch_available_bytes: Option<u64>,
    pub(crate) required_logical_cpus: usize,
    pub(crate) required_memory_bytes: u64,
    pub(crate) profile_passed: bool,
    pub(crate) operating_system: String,
    pub(crate) architecture: String,
    pub(crate) rustc: String,
}

#[derive(Serialize)]
struct FailureVerificationReport<'a> {
    schema_version: u32,
    status: &'static str,
    revisions: &'a RevisionEvidence,
    config: HarnessConfig,
    peak_rss_bytes: Option<u64>,
    peak_scratch_bytes: Option<u64>,
    error: &'a str,
    completed_scenarios: &'a [ScenarioEvidence],
}

#[derive(Debug, Clone, Serialize)]
pub(crate) struct TimingsMillis {
    pub(crate) seed: u64,
    pub(crate) source_oracle: u64,
    pub(crate) source_durable_reopen: u64,
    pub(crate) source_garbage_collection: u64,
    pub(crate) immutable_copy: u64,
    pub(crate) blocking_rewrite_open: u64,
    pub(crate) cleanup: u64,
    pub(crate) definition_migration: u64,
    pub(crate) reopen: u64,
    pub(crate) target_oracle: u64,
    pub(crate) post_migration_crud: u64,
    pub(crate) post_crud_reopen: u64,
    pub(crate) garbage_collection: u64,
    pub(crate) snapshot_restore: u64,
    pub(crate) compaction_drain: u64,
    pub(crate) total: u64,
}

#[derive(Debug, Clone, Serialize)]
pub(crate) struct LogicalCounts {
    pub(crate) source_nodes: u64,
    pub(crate) source_current_edges: u64,
    pub(crate) source_legacy_edges: u64,
    pub(crate) expected_target_edges: u64,
    pub(crate) target_nodes: u64,
    pub(crate) target_edges: u64,
}

#[derive(Debug, Clone, Copy, Serialize)]
pub(crate) struct SourceSeedResumeEvidence {
    pub(crate) enabled: bool,
    pub(crate) recovered_nodes: u64,
    pub(crate) recovered_edges: u64,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub(crate) struct TextHitEvidence {
    pub(crate) entity_id: u64,
    pub(crate) score_bits: u32,
}

#[derive(Debug, Clone, Serialize)]
pub(crate) struct TextQueryCaseEvidence {
    pub(crate) element_kind: &'static str,
    pub(crate) query: String,
    pub(crate) source_hits: Vec<TextHitEvidence>,
    pub(crate) target_manifests: Vec<db::migration_parity::MigrationParityTextSearch>,
}

#[derive(Debug, Clone, Serialize)]
pub(crate) struct TextQueryEvidence {
    pub(crate) cases: Vec<TextQueryCaseEvidence>,
    pub(crate) passed: bool,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub(crate) struct VectorHitEvidence {
    pub(crate) node_id: u64,
    pub(crate) distance_bits: u32,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub(crate) struct SourceVectorMetadataEvidence {
    pub(crate) index_name: String,
    pub(crate) property_name: String,
    pub(crate) dimension: usize,
    pub(crate) m: usize,
    pub(crate) m0: usize,
    pub(crate) ef_construction: usize,
    pub(crate) ml_bits: u32,
    pub(crate) simhash_threshold: usize,
    pub(crate) sampling_ratio_bits: u32,
    pub(crate) adaptive_enabled: bool,
    pub(crate) adaptive_failure_probability_bits: u32,
    pub(crate) entry_point: Option<u64>,
    pub(crate) max_layer: u16,
    pub(crate) count: u64,
}

#[derive(Debug, Clone, Serialize)]
pub(crate) struct VectorQueryCaseEvidence {
    pub(crate) element_kind: &'static str,
    pub(crate) index_name: String,
    pub(crate) source_metadata: SourceVectorMetadataEvidence,
    pub(crate) target_metadata: db::migration_parity::MigrationParityVectorMetadata,
    pub(crate) query_bits: Vec<u32>,
    pub(crate) source_hits: Vec<VectorHitEvidence>,
    pub(crate) target_hits: Vec<VectorHitEvidence>,
}

#[derive(Debug, Clone, Serialize)]
pub(crate) struct VectorQueryEvidence {
    pub(crate) cases: Vec<VectorQueryCaseEvidence>,
    pub(crate) passed: bool,
}

#[derive(Debug, Clone, Serialize)]
pub(crate) struct SourceCompactionStatus {
    pub(crate) id: String,
    pub(crate) status: String,
    pub(crate) spec: String,
    pub(crate) bytes_processed: u64,
}

#[derive(Debug, Clone, Serialize)]
pub(crate) struct SnapshotRestoreEvidence {
    pub(crate) database: String,
    pub(crate) oracle: OracleBuildStats,
    pub(crate) comparison: SourceDurabilityComparison,
    pub(crate) node_text_hits: Vec<TextHitEvidence>,
    pub(crate) edge_text_hits: Vec<TextHitEvidence>,
    pub(crate) node_vector_hits: Vec<VectorHitEvidence>,
    pub(crate) edge_vector_hits: Vec<VectorHitEvidence>,
    pub(crate) storage_bytes: u64,
}

#[derive(Debug, Clone, Serialize)]
pub(crate) struct GarbageCollectionPassEvidence {
    pub(crate) phase: &'static str,
    pub(crate) elapsed_millis: u64,
    pub(crate) checkpoint_clock_advance_millis: u64,
    pub(crate) checkpoints_before: u64,
    pub(crate) expiring_checkpoints_before: u64,
    pub(crate) permanent_checkpoints_before: u64,
    pub(crate) storage_before_bytes: u64,
    pub(crate) storage_after_bytes: u64,
    pub(crate) reclaimed_bytes: u64,
    pub(crate) cold_reopen_passed: bool,
}

#[derive(Debug, Clone, Serialize)]
pub(crate) struct GarbageCollectionEvidence {
    pub(crate) before_parity_oracle: GarbageCollectionPassEvidence,
    pub(crate) after_crud_and_compaction: GarbageCollectionPassEvidence,
}

#[derive(Debug, Clone, Serialize)]
pub(crate) struct ScenarioEvidence {
    pub(crate) name: String,
    pub(crate) edge_policy: u8,
    pub(crate) source_seed_resume: SourceSeedResumeEvidence,
    pub(crate) counts: LogicalCounts,
    pub(crate) cleanup_steps: u64,
    pub(crate) definition_migration_steps: u64,
    pub(crate) definition_migration_active: bool,
    pub(crate) migration_jobs: Vec<db::migrations::MigrationParityJobStatus>,
    pub(crate) adoption_snapshot: db::migration_parity::MigrationParityIndexState,
    pub(crate) migration_snapshot: db::migration_parity::MigrationParityIndexState,
    pub(crate) source_vector_non_metadata_namespace_digests:
        std::collections::BTreeMap<u64, String>,
    pub(crate) text_query: TextQueryEvidence,
    pub(crate) vector_query: VectorQueryEvidence,
    pub(crate) post_migration_crud: db::migration_parity::MigrationParityCrudEvidence,
    pub(crate) post_crud_reopen: db::migration_parity::MigrationParityQueryCorpus,
    pub(crate) source_garbage_collection: GarbageCollectionPassEvidence,
    pub(crate) garbage_collection: GarbageCollectionEvidence,
    pub(crate) snapshot_restore: SnapshotRestoreEvidence,
    pub(crate) source_oracle: OracleBuildStats,
    pub(crate) reopened_source_oracle: OracleBuildStats,
    pub(crate) source_durability: SourceDurabilityComparison,
    pub(crate) post_gc_source_oracle: OracleBuildStats,
    pub(crate) source_gc_durability: SourceDurabilityComparison,
    pub(crate) target_oracle: OracleBuildStats,
    pub(crate) comparison: OracleComparison,
    pub(crate) source_object_store: ObjectStoreMetrics,
    pub(crate) target_object_store: ObjectStoreMetrics,
    pub(crate) target_storage_bytes: u64,
    pub(crate) open_attempts: u64,
    pub(crate) object_store_fault_probe_attempts: u64,
    pub(crate) migration_failpoint_retries: u64,
    pub(crate) object_store_phases: ObjectStorePhaseEvidence,
    pub(crate) slatedb_lsm_phases: SlateDbLsmPhaseEvidence,
    pub(crate) compaction_drain: CompactionDrainEvidence,
    pub(crate) source_compaction_jobs: Vec<SourceCompactionStatus>,
    pub(crate) compaction_jobs: Vec<db::migration_parity::MigrationParityCompactionStatus>,
    pub(crate) failed_compactions: u64,
    pub(crate) compaction_errors: CompactionErrorEvidence,
    pub(crate) timings_millis: TimingsMillis,
}

#[derive(Debug, Clone, Serialize)]
pub(crate) struct ObjectStorePhaseEvidence {
    pub(crate) source_seed: ObjectStoreMetrics,
    pub(crate) source_oracle: ObjectStoreMetrics,
    pub(crate) source_durable_reopen: ObjectStoreMetrics,
    pub(crate) source_garbage_collection: ObjectStoreMetrics,
    pub(crate) source_close_and_copy: ObjectStoreMetrics,
    pub(crate) snapshot_restore: ObjectStoreMetrics,
    pub(crate) target_rewrite: ObjectStoreMetrics,
    pub(crate) cleanup: ObjectStoreMetrics,
    pub(crate) definition_migration: ObjectStoreMetrics,
    pub(crate) reopen: ObjectStoreMetrics,
    pub(crate) target_oracle: ObjectStoreMetrics,
    pub(crate) post_migration_crud: ObjectStoreMetrics,
    pub(crate) post_crud_reopen: ObjectStoreMetrics,
    pub(crate) compaction_drain: ObjectStoreMetrics,
    pub(crate) initial_garbage_collection: ObjectStoreMetrics,
    pub(crate) final_garbage_collection: ObjectStoreMetrics,
    pub(crate) storage_measurement: ObjectStoreMetrics,
    pub(crate) target_close: ObjectStoreMetrics,
    pub(crate) fault_probe: ObjectStoreMetrics,
}

#[derive(Debug, Clone, Serialize)]
pub(crate) struct SlateDbLsmSnapshot {
    pub(crate) manifest_version: u64,
    pub(crate) writer_epoch: u64,
    pub(crate) compactor_epoch: u64,
    pub(crate) l0_ssts: u64,
    pub(crate) l0_bytes: u64,
    pub(crate) compacted_runs: u64,
    pub(crate) compacted_ssts: u64,
    pub(crate) compacted_bytes: u64,
    pub(crate) segments: u64,
    pub(crate) maximum_tree_l0_ssts: u64,
}

#[derive(Debug, Clone, Serialize)]
pub(crate) struct SlateDbLsmPhaseEvidence {
    pub(crate) after_rewrite: SlateDbLsmSnapshot,
    pub(crate) after_cleanup: SlateDbLsmSnapshot,
    pub(crate) after_reopen: SlateDbLsmSnapshot,
    pub(crate) after_oracle: SlateDbLsmSnapshot,
    pub(crate) after_initial_garbage_collection: SlateDbLsmSnapshot,
    pub(crate) after_crud: SlateDbLsmSnapshot,
    pub(crate) after_crud_reopen: SlateDbLsmSnapshot,
    pub(crate) after_final_garbage_collection: SlateDbLsmSnapshot,
}

#[derive(Debug, Clone, Serialize)]
pub(crate) struct CompactionDrainEvidence {
    pub(crate) enabled: bool,
    pub(crate) maximum_wait_millis: u64,
    pub(crate) elapsed_millis: u64,
    pub(crate) maximum_steady_l0_ssts: u64,
    pub(crate) peak_l0_ssts: u64,
    pub(crate) samples: u64,
    pub(crate) passed: Option<bool>,
    pub(crate) initial: SlateDbLsmSnapshot,
    pub(crate) final_snapshot: SlateDbLsmSnapshot,
}

#[derive(Debug, Clone, Serialize)]
pub(crate) struct CompactionErrorEvidence {
    pub(crate) count: u64,
    pub(crate) first_errors: Vec<String>,
}

#[derive(Debug, Clone, Serialize)]
pub(crate) struct ResumeScenarioEvidence {
    pub(crate) name: String,
    pub(crate) migration_jobs: Vec<db::migrations::MigrationParityJobStatus>,
    pub(crate) migration_snapshot: db::migration_parity::MigrationParityIndexState,
    pub(crate) source_vector_non_metadata_namespace_digests:
        std::collections::BTreeMap<u64, String>,
    pub(crate) migration_steps: u64,
    pub(crate) migration_millis: u64,
    pub(crate) text_query: TextQueryEvidence,
    pub(crate) vector_query: VectorQueryEvidence,
    pub(crate) target_oracle: OracleBuildStats,
    pub(crate) comparison: OracleComparison,
    pub(crate) object_store: ObjectStoreMetrics,
    pub(crate) target_storage_bytes: u64,
    pub(crate) slatedb_lsm: SlateDbLsmSnapshot,
    pub(crate) compaction_jobs: Vec<db::migration_parity::MigrationParityCompactionStatus>,
    pub(crate) compaction_errors: CompactionErrorEvidence,
    pub(crate) definition_migration_steps: u64,
    pub(crate) definition_migration_active: bool,
    pub(crate) definition_migration_millis: u64,
    pub(crate) reopen_millis: u64,
    pub(crate) oracle_millis: u64,
    pub(crate) total_millis: u64,
}

#[derive(Serialize)]
struct ResumeVerificationReport<'a> {
    schema_version: u32,
    status: &'static str,
    revisions: &'a RevisionEvidence,
    object_storage: String,
    peak_rss_bytes: Option<u64>,
    peak_scratch_bytes: Option<u64>,
    scenarios: Vec<ResumeScenarioEvidence>,
}

#[derive(Debug, Clone, Serialize)]
pub(crate) struct UnavailableMetric {
    pub(crate) metric: String,
    pub(crate) reason: String,
}

#[derive(Debug, Clone, Serialize)]
pub(crate) struct ScalePoint {
    pub(crate) report: String,
    pub(crate) rows: u64,
    pub(crate) source_physical_rows: u64,
    pub(crate) source_physical_bytes: u64,
    pub(crate) runtime_millis: u64,
    pub(crate) requests: u64,
    pub(crate) transferred_bytes: u64,
    pub(crate) storage_bytes: u64,
    pub(crate) peak_rss_bytes: u64,
    pub(crate) peak_scratch_bytes: u64,
}

#[derive(Debug, Clone, Serialize)]
pub(crate) struct ScaleProjection {
    pub(crate) next_rows: u64,
    pub(crate) exponent_used: f64,
    pub(crate) projected_runtime_millis: u64,
    pub(crate) projected_peak_rss_bytes: u64,
    pub(crate) projected_peak_scratch_bytes: u64,
    pub(crate) safety_factor: f64,
    pub(crate) rss_capacity_limit_bytes: Option<u64>,
    pub(crate) scratch_capacity_limit_bytes: Option<u64>,
    pub(crate) scenario_duration_limit_millis: u64,
    pub(crate) passed: bool,
}

#[derive(Debug, Clone, Serialize)]
pub(crate) struct ProjectionActualComparison {
    pub(crate) projected_rows: u64,
    pub(crate) projected_runtime_millis: u64,
    pub(crate) actual_runtime_millis: u64,
    pub(crate) projected_peak_rss_bytes: u64,
    pub(crate) actual_peak_rss_bytes: u64,
    pub(crate) projected_peak_scratch_bytes: u64,
    pub(crate) actual_peak_scratch_bytes: u64,
}

#[derive(Debug, Clone, Serialize)]
pub(crate) struct ScaleAnalysis {
    pub(crate) measurement_scope: &'static str,
    pub(crate) points: Vec<ScalePoint>,
    pub(crate) runtime_exponent: f64,
    pub(crate) request_exponent: f64,
    pub(crate) transferred_byte_exponent: f64,
    pub(crate) storage_exponent: f64,
    pub(crate) peak_rss_exponent: f64,
    pub(crate) peak_scratch_exponent: f64,
    pub(crate) worst_runtime_exponent: f64,
    pub(crate) worst_request_exponent: f64,
    pub(crate) worst_transferred_byte_exponent: f64,
    pub(crate) worst_storage_exponent: f64,
    pub(crate) worst_peak_rss_exponent: f64,
    pub(crate) worst_peak_scratch_exponent: f64,
    pub(crate) request_amplification_change: f64,
    pub(crate) byte_amplification_change: f64,
    pub(crate) storage_amplification_change: f64,
    pub(crate) amplification_gate_applied: bool,
    pub(crate) exponent_gate_applied: bool,
    pub(crate) exponent_passed: bool,
    pub(crate) projection: Option<ScaleProjection>,
    pub(crate) prior_projection_vs_actual: Option<ProjectionActualComparison>,
    pub(crate) passed: bool,
}

#[derive(Debug, Clone, Serialize)]
pub(crate) struct VerificationReport {
    pub(crate) schema_version: u32,
    pub(crate) status: String,
    pub(crate) revisions: RevisionEvidence,
    pub(crate) hash_contract: crate::hash_contract::HashContractEvidence,
    pub(crate) config: HarnessConfig,
    pub(crate) host: HostEvidence,
    pub(crate) peak_rss_bytes: Option<u64>,
    pub(crate) peak_scratch_bytes: Option<u64>,
    pub(crate) scenarios: Vec<ScenarioEvidence>,
    pub(crate) unavailable_metrics: Vec<UnavailableMetric>,
    pub(crate) release_blockers: Vec<String>,
    pub(crate) scale_analysis: Option<ScaleAnalysis>,
}

impl VerificationReport {
    pub(crate) fn write(&self, path: &Path) -> Result<()> {
        if let Some(parent) = path.parent() {
            std::fs::create_dir_all(parent).with_context(|| {
                format!("failed to create report directory {}", parent.display())
            })?;
        }
        let bytes = serde_json::to_vec_pretty(self)?;
        std::fs::write(path, bytes)
            .with_context(|| format!("failed to write report {}", path.display()))
    }
}

pub(crate) fn write_resume_report(
    path: &Path,
    revisions: &RevisionEvidence,
    object_storage: String,
    scenarios: Vec<ResumeScenarioEvidence>,
) -> Result<()> {
    if let Some(parent) = path.parent() {
        std::fs::create_dir_all(parent)
            .with_context(|| format!("failed to create report directory {}", parent.display()))?;
    }
    let report = ResumeVerificationReport {
        schema_version: 1,
        status: "resumed_verification_passed",
        revisions,
        object_storage,
        peak_rss_bytes: peak_rss_bytes(),
        peak_scratch_bytes: peak_scratch_bytes(),
        scenarios,
    };
    std::fs::write(path, serde_json::to_vec_pretty(&report)?)
        .with_context(|| format!("failed to write resume report {}", path.display()))
}

pub(crate) fn write_failure_report(
    path: &Path,
    revisions: &RevisionEvidence,
    config: HarnessConfig,
    error: &str,
    completed_scenarios: &[ScenarioEvidence],
) -> Result<()> {
    if let Some(parent) = path.parent() {
        std::fs::create_dir_all(parent)
            .with_context(|| format!("failed to create report directory {}", parent.display()))?;
    }
    let report = FailureVerificationReport {
        schema_version: 1,
        status: "failed",
        revisions,
        config,
        peak_rss_bytes: peak_rss_bytes(),
        peak_scratch_bytes: peak_scratch_bytes(),
        error,
        completed_scenarios,
    };
    std::fs::write(path, serde_json::to_vec_pretty(&report)?)
        .with_context(|| format!("failed to write failure report {}", path.display()))
}

pub(crate) fn analyze_scale(
    baseline_paths: &[std::path::PathBuf],
    current_rows: u64,
    scenarios: &[ScenarioEvidence],
    project_next_rows: Option<u64>,
    total_memory_bytes: Option<u64>,
    scratch_available_bytes: Option<u64>,
    maximum_scenario_seconds: u64,
) -> Result<Option<ScaleAnalysis>> {
    if baseline_paths.is_empty() && project_next_rows.is_none() {
        return Ok(None);
    }
    if current_rows == 0 {
        anyhow::bail!("scale baseline requires a nonzero current scale workload");
    }
    let mut points = Vec::with_capacity(baseline_paths.len().saturating_add(1));
    let mut prior_projections = Vec::new();
    for baseline_path in baseline_paths {
        let bytes = std::fs::read(baseline_path).with_context(|| {
            format!(
                "failed to read scale baseline report {}",
                baseline_path.display()
            )
        })?;
        let baseline: serde_json::Value = serde_json::from_slice(&bytes).with_context(|| {
            format!(
                "failed to decode scale baseline report {}",
                baseline_path.display()
            )
        })?;
        let baseline_scenarios = baseline["scenarios"]
            .as_array()
            .context("baseline report is missing scenarios")?;
        if let Some(projection) = baseline["scale_analysis"]["projection"].as_object() {
            prior_projections.push((
                projection
                    .get("next_rows")
                    .and_then(serde_json::Value::as_u64),
                projection
                    .get("projected_runtime_millis")
                    .and_then(serde_json::Value::as_u64),
                projection
                    .get("projected_peak_rss_bytes")
                    .and_then(serde_json::Value::as_u64),
                projection
                    .get("projected_peak_scratch_bytes")
                    .and_then(serde_json::Value::as_u64),
            ));
        }
        let rows = baseline["config"]["scale_nodes"]
            .as_u64()
            .context("baseline report is missing config.scale_nodes")?
            .saturating_add(
                baseline["config"]["scale_edges"]
                    .as_u64()
                    .context("baseline report is missing config.scale_edges")?,
            );
        let storage_bytes = baseline_scenarios
            .iter()
            .map(|scenario| {
                scenario["target_storage_bytes"]
                    .as_u64()
                    .unwrap_or_default()
            })
            .sum::<u64>();
        let (source_physical_rows, source_physical_bytes) = baseline_scenarios
            .iter()
            .map(json_source_physical_size)
            .fold((0_u64, 0_u64), |totals, current| {
                (
                    totals.0.saturating_add(current.0),
                    totals.1.saturating_add(current.1),
                )
            });
        points.push(ScalePoint {
            report: baseline_path.display().to_string(),
            rows,
            source_physical_rows,
            source_physical_bytes,
            runtime_millis: baseline_scenarios.iter().map(json_scale_runtime).sum(),
            requests: baseline_scenarios.iter().map(json_scenario_requests).sum(),
            transferred_bytes: baseline_scenarios.iter().map(json_scenario_bytes).sum(),
            storage_bytes,
            peak_rss_bytes: baseline["peak_rss_bytes"]
                .as_u64()
                .context("baseline report is missing peak_rss_bytes")?,
            peak_scratch_bytes: baseline["peak_scratch_bytes"]
                .as_u64()
                .context("baseline report is missing peak_scratch_bytes")?,
        });
    }
    let current_runtime = scenarios.iter().map(scale_runtime).sum::<u64>();
    let current_requests = scenarios.iter().map(scenario_requests).sum::<u64>();
    let current_bytes = scenarios.iter().map(scenario_bytes).sum::<u64>();
    let current_storage_bytes = scenarios
        .iter()
        .map(|scenario| scenario.target_storage_bytes)
        .sum::<u64>();
    let (current_source_physical_rows, current_source_physical_bytes) = scenarios
        .iter()
        .map(source_physical_size)
        .fold((0_u64, 0_u64), |totals, current| {
            (
                totals.0.saturating_add(current.0),
                totals.1.saturating_add(current.1),
            )
        });
    points.push(ScalePoint {
        report: "<current>".to_string(),
        rows: current_rows,
        source_physical_rows: current_source_physical_rows,
        source_physical_bytes: current_source_physical_bytes,
        runtime_millis: current_runtime,
        requests: current_requests,
        transferred_bytes: current_bytes,
        storage_bytes: current_storage_bytes,
        peak_rss_bytes: peak_rss_bytes().context("peak RSS is unavailable for scale analysis")?,
        peak_scratch_bytes: peak_scratch_bytes()
            .context("peak scratch use is unavailable for scale analysis")?,
    });
    points.sort_by_key(|point| point.rows);
    if points.iter().any(|point| {
        point.rows == 0
            || point.runtime_millis == 0
            || point.requests == 0
            || point.transferred_bytes == 0
            || point.storage_bytes == 0
            || point.peak_rss_bytes == 0
            || point.source_physical_rows == 0
            || point.source_physical_bytes == 0
            || point.peak_scratch_bytes == 0
    }) {
        anyhow::bail!("scale reports must contain nonzero rows and metrics");
    }
    if points
        .windows(2)
        .any(|window| window[0].rows == window[1].rows)
    {
        anyhow::bail!("scale reports must use distinct row counts");
    }
    if points
        .last()
        .is_none_or(|point| point.report != "<current>")
    {
        anyhow::bail!("current scale rows must exceed every baseline row count");
    }
    let has_growth_sample = points.len() >= 2;
    let (
        runtime_exponent,
        request_exponent,
        transferred_byte_exponent,
        storage_exponent,
        peak_rss_exponent,
        peak_scratch_exponent,
        worst_runtime_exponent,
        worst_request_exponent,
        worst_transferred_byte_exponent,
        worst_storage_exponent,
        worst_peak_rss_exponent,
        worst_peak_scratch_exponent,
    ) = if has_growth_sample {
        (
            fitted_exponent(&points, |point| point.runtime_millis)?,
            fitted_exponent(&points, |point| point.requests)?,
            fitted_exponent(&points, |point| point.transferred_bytes)?,
            fitted_exponent(&points, |point| point.storage_bytes)?,
            fitted_exponent(&points, |point| point.peak_rss_bytes)?,
            fitted_exponent(&points, |point| point.peak_scratch_bytes)?,
            worst_adjacent_exponent(&points, |point| point.runtime_millis)?,
            worst_adjacent_exponent(&points, |point| point.requests)?,
            worst_adjacent_exponent(&points, |point| point.transferred_bytes)?,
            worst_adjacent_exponent(&points, |point| point.storage_bytes)?,
            worst_adjacent_exponent(&points, |point| point.peak_rss_bytes)?,
            worst_adjacent_exponent(&points, |point| point.peak_scratch_bytes)?,
        )
    } else {
        // The first rung has no observed growth interval. Its advancement gate
        // is therefore deliberately linear, with the normal 1.5x safety
        // factor applied below. The next rung must supply this report as a
        // baseline before exponent gates can pass.
        (1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
    };
    let baseline = has_growth_sample.then(|| &points[points.len() - 2]);
    let request_amplification_change = baseline.map_or(0.0, |baseline| {
        amplification_change(
            baseline.requests,
            baseline.rows,
            current_requests,
            current_rows,
        )
    });
    let byte_amplification_change = baseline.map_or(0.0, |baseline| {
        amplification_change(
            baseline.transferred_bytes,
            baseline.rows,
            current_bytes,
            current_rows,
        )
    });
    let storage_amplification_change = baseline.map_or(0.0, |baseline| {
        amplification_change(
            baseline.storage_bytes,
            baseline.rows,
            current_storage_bytes,
            current_rows,
        )
    });
    let amplification_gate_applied =
        baseline.is_some_and(|baseline| baseline.rows >= AMPLIFICATION_GATE_MIN_BASELINE_ROWS);
    let amplification_passed = !amplification_gate_applied
        || (request_amplification_change.abs() <= 0.2
            && byte_amplification_change.abs() <= 0.2
            && storage_amplification_change.abs() <= 0.2);
    let exponent_gate_applied = has_growth_sample && current_rows >= EXPONENT_GATE_MIN_ROWS;
    let exponent_passed = (!exponent_gate_applied
        || (runtime_exponent <= 1.1
            && request_exponent <= 1.1
            && transferred_byte_exponent <= 1.1
            && storage_exponent <= 1.1
            && peak_rss_exponent <= 1.0
            && worst_runtime_exponent <= 1.1
            && worst_request_exponent <= 1.1
            && worst_transferred_byte_exponent <= 1.1
            && worst_storage_exponent <= 1.1
            && worst_peak_rss_exponent <= 1.0))
        && scratch_exponent_gate_passed(
            exponent_gate_applied,
            peak_scratch_exponent,
            worst_peak_scratch_exponent,
        );
    let projection = project_next_rows
        .map(|next_rows| {
            if next_rows <= current_rows {
                anyhow::bail!("projected next rows must exceed current rows");
            }
            let last_runtime_exponent = if has_growth_sample {
                Some(adjacent_exponent(
                    &points[points.len() - 2],
                    &points[points.len() - 1],
                    |point| point.runtime_millis,
                )?)
            } else {
                None
            };
            let exponent_used = conservative_projection_exponent(
                has_growth_sample.then_some(runtime_exponent),
                last_runtime_exponent,
            );
            let ratio = next_rows as f64 / current_rows as f64;
            let safety_factor = 1.5;
            let projected_runtime_millis =
                project_with_safety(current_runtime, ratio, exponent_used, safety_factor);
            let rss_exponent = conservative_projection_exponent(
                has_growth_sample.then_some(peak_rss_exponent),
                has_growth_sample.then_some(worst_peak_rss_exponent),
            );
            let projected_peak_rss_bytes = project_with_safety(
                points.last().unwrap().peak_rss_bytes,
                ratio,
                rss_exponent,
                safety_factor,
            );
            let scratch_exponent = conservative_projection_exponent(
                has_growth_sample.then_some(peak_scratch_exponent),
                has_growth_sample.then_some(worst_peak_scratch_exponent),
            );
            let projected_peak_scratch_bytes = project_with_safety(
                points.last().unwrap().peak_scratch_bytes,
                ratio,
                scratch_exponent,
                safety_factor,
            );
            let rss_capacity_limit_bytes =
                total_memory_bytes.map(|bytes| bytes.saturating_mul(70) / 100);
            let scratch_capacity_limit_bytes =
                scratch_available_bytes.map(|bytes| bytes.saturating_mul(70) / 100);
            let scenario_duration_limit_millis = maximum_scenario_seconds.saturating_mul(1_000);
            let passed = rss_capacity_limit_bytes
                .is_none_or(|limit| projected_peak_rss_bytes < limit)
                && scratch_capacity_limit_bytes
                    .is_none_or(|limit| projected_peak_scratch_bytes < limit)
                && projected_runtime_millis < scenario_duration_limit_millis;
            Ok(ScaleProjection {
                next_rows,
                exponent_used,
                projected_runtime_millis,
                projected_peak_rss_bytes,
                projected_peak_scratch_bytes,
                safety_factor,
                rss_capacity_limit_bytes,
                scratch_capacity_limit_bytes,
                scenario_duration_limit_millis,
                passed,
            })
        })
        .transpose()?;
    let prior_projection_vs_actual =
        prior_projections
            .into_iter()
            .rev()
            .find_map(|(rows, runtime, rss, scratch)| {
                if rows != Some(current_rows) {
                    return None;
                }
                Some(ProjectionActualComparison {
                    projected_rows: current_rows,
                    projected_runtime_millis: runtime?,
                    actual_runtime_millis: current_runtime,
                    projected_peak_rss_bytes: rss?,
                    actual_peak_rss_bytes: points.last()?.peak_rss_bytes,
                    projected_peak_scratch_bytes: scratch?,
                    actual_peak_scratch_bytes: points.last()?.peak_scratch_bytes,
                })
            });
    let passed = exponent_passed
        && amplification_passed
        && projection
            .as_ref()
            .is_none_or(|projection| projection.passed);

    Ok(Some(ScaleAnalysis {
        measurement_scope: "total scenario wall time and all object-store phases, including fixture seed, immutable copy, rollback rehearsal, migration, reopen, and verification",
        points,
        runtime_exponent,
        request_exponent,
        transferred_byte_exponent,
        storage_exponent,
        peak_rss_exponent,
        peak_scratch_exponent,
        worst_runtime_exponent,
        worst_request_exponent,
        worst_transferred_byte_exponent,
        worst_storage_exponent,
        worst_peak_rss_exponent,
        worst_peak_scratch_exponent,
        request_amplification_change,
        byte_amplification_change,
        storage_amplification_change,
        amplification_gate_applied,
        exponent_gate_applied,
        exponent_passed,
        projection,
        prior_projection_vs_actual,
        passed,
    }))
}

fn scratch_exponent_gate_passed(
    exponent_gate_applied: bool,
    peak_scratch_exponent: f64,
    worst_peak_scratch_exponent: f64,
) -> bool {
    const STORAGE_LIKE_EXPONENT_LIMIT: f64 = 1.10;
    !exponent_gate_applied
        || (peak_scratch_exponent <= STORAGE_LIKE_EXPONENT_LIMIT
            && worst_peak_scratch_exponent <= STORAGE_LIKE_EXPONENT_LIMIT)
}

fn fitted_exponent(points: &[ScalePoint], metric: impl Fn(&ScalePoint) -> u64) -> Result<f64> {
    let count = points.len() as f64;
    let mean_rows = points
        .iter()
        .map(|point| (point.rows as f64).ln())
        .sum::<f64>()
        / count;
    let mean_metric = points
        .iter()
        .map(|point| (metric(point) as f64).ln())
        .sum::<f64>()
        / count;
    let numerator = points
        .iter()
        .map(|point| {
            ((point.rows as f64).ln() - mean_rows) * ((metric(point) as f64).ln() - mean_metric)
        })
        .sum::<f64>();
    let denominator = points
        .iter()
        .map(|point| ((point.rows as f64).ln() - mean_rows).powi(2))
        .sum::<f64>();
    if denominator == 0.0 {
        anyhow::bail!("scale regression has no row-count variance");
    }
    Ok(numerator / denominator)
}

fn worst_adjacent_exponent(
    points: &[ScalePoint],
    metric: impl Fn(&ScalePoint) -> u64,
) -> Result<f64> {
    points
        .windows(2)
        .map(|window| adjacent_exponent(&window[0], &window[1], &metric))
        .collect::<Result<Vec<_>>>()?
        .into_iter()
        .reduce(f64::max)
        .context("worst adjacent exponent requires two scale points")
}

fn adjacent_exponent(
    left: &ScalePoint,
    right: &ScalePoint,
    metric: impl Fn(&ScalePoint) -> u64,
) -> Result<f64> {
    let row_ratio = right.rows as f64 / left.rows as f64;
    let metric_ratio = metric(right) as f64 / metric(left) as f64;
    if row_ratio <= 1.0 || metric_ratio <= 0.0 {
        anyhow::bail!("adjacent scale points must have positive increasing rows and metrics");
    }
    Ok(metric_ratio.ln() / row_ratio.ln())
}

fn object_store_requests(metrics: &crate::object_store_metrics::ObjectStoreMetrics) -> u64 {
    metrics
        .get_requests
        .saturating_add(metrics.head_requests)
        .saturating_add(metrics.put_requests)
        .saturating_add(metrics.multipart_requests)
        .saturating_add(metrics.list_requests)
        .saturating_add(metrics.delete_requests)
        .saturating_add(metrics.copy_requests)
}

fn scenario_requests(scenario: &ScenarioEvidence) -> u64 {
    let phases = &scenario.object_store_phases;
    [
        &phases.source_seed,
        &phases.source_oracle,
        &phases.source_durable_reopen,
        &phases.source_garbage_collection,
        &phases.source_close_and_copy,
        &phases.snapshot_restore,
        &phases.target_rewrite,
        &phases.cleanup,
        &phases.definition_migration,
        &phases.reopen,
        &phases.target_oracle,
        &phases.post_migration_crud,
        &phases.post_crud_reopen,
        &phases.compaction_drain,
        &phases.initial_garbage_collection,
        &phases.final_garbage_collection,
        &phases.storage_measurement,
        &phases.target_close,
        &phases.fault_probe,
    ]
    .into_iter()
    .map(object_store_requests)
    .sum()
}

fn scenario_bytes(scenario: &ScenarioEvidence) -> u64 {
    let phases = &scenario.object_store_phases;
    [
        &phases.source_seed,
        &phases.source_oracle,
        &phases.source_durable_reopen,
        &phases.source_garbage_collection,
        &phases.source_close_and_copy,
        &phases.snapshot_restore,
        &phases.target_rewrite,
        &phases.cleanup,
        &phases.definition_migration,
        &phases.reopen,
        &phases.target_oracle,
        &phases.post_migration_crud,
        &phases.post_crud_reopen,
        &phases.compaction_drain,
        &phases.initial_garbage_collection,
        &phases.final_garbage_collection,
        &phases.storage_measurement,
        &phases.target_close,
        &phases.fault_probe,
    ]
    .into_iter()
    .map(|metrics| metrics.bytes_read.saturating_add(metrics.bytes_written))
    .sum()
}

fn json_scenario_requests(scenario: &serde_json::Value) -> u64 {
    scale_phase_names()
        .into_iter()
        .flat_map(|phase| {
            [
                "get_requests",
                "head_requests",
                "put_requests",
                "multipart_requests",
                "list_requests",
                "delete_requests",
                "copy_requests",
            ]
            .into_iter()
            .map(move |metric| {
                scenario["object_store_phases"][phase][metric]
                    .as_u64()
                    .unwrap_or_default()
            })
        })
        .sum()
}

fn json_scenario_bytes(scenario: &serde_json::Value) -> u64 {
    scale_phase_names()
        .into_iter()
        .flat_map(|phase| {
            ["bytes_read", "bytes_written"]
                .into_iter()
                .map(move |metric| {
                    scenario["object_store_phases"][phase][metric]
                        .as_u64()
                        .unwrap_or_default()
                })
        })
        .sum()
}

fn scale_runtime(scenario: &ScenarioEvidence) -> u64 {
    scenario.timings_millis.total
}

fn json_scale_runtime(scenario: &serde_json::Value) -> u64 {
    scenario["timings_millis"]["total"]
        .as_u64()
        .unwrap_or_default()
}

const fn scale_phase_names() -> [&'static str; 19] {
    [
        "source_seed",
        "source_oracle",
        "source_durable_reopen",
        "source_garbage_collection",
        "source_close_and_copy",
        "snapshot_restore",
        "target_rewrite",
        "cleanup",
        "definition_migration",
        "reopen",
        "target_oracle",
        "post_migration_crud",
        "post_crud_reopen",
        "compaction_drain",
        "initial_garbage_collection",
        "final_garbage_collection",
        "storage_measurement",
        "target_close",
        "fault_probe",
    ]
}

fn source_physical_size(scenario: &ScenarioEvidence) -> (u64, u64) {
    (
        scenario
            .source_oracle
            .source_physical_rows
            .unwrap_or_default(),
        scenario
            .source_oracle
            .source_physical_bytes
            .unwrap_or_default(),
    )
}

fn json_source_physical_size(scenario: &serde_json::Value) -> (u64, u64) {
    (
        scenario["source_oracle"]["source_physical_rows"]
            .as_u64()
            .unwrap_or_default(),
        scenario["source_oracle"]["source_physical_bytes"]
            .as_u64()
            .unwrap_or_default(),
    )
}

fn amplification_change(
    baseline_total: u64,
    baseline_rows: u64,
    current_total: u64,
    current_rows: u64,
) -> f64 {
    let baseline_per_row = baseline_total as f64 / baseline_rows as f64;
    let current_per_row = current_total as f64 / current_rows as f64;
    if baseline_per_row == 0.0 {
        return if current_per_row == 0.0 {
            0.0
        } else {
            f64::INFINITY
        };
    }
    current_per_row / baseline_per_row - 1.0
}

fn conservative_projection_exponent(fitted: Option<f64>, last_or_worst: Option<f64>) -> f64 {
    fitted
        .unwrap_or(1.0)
        .max(last_or_worst.unwrap_or(1.0))
        .max(1.0)
}

fn project_with_safety(current: u64, ratio: f64, exponent: f64, safety: f64) -> u64 {
    (current as f64 * ratio.powf(exponent) * safety).ceil() as u64
}

pub(crate) fn peak_rss_bytes() -> Option<u64> {
    let mut usage = std::mem::MaybeUninit::<libc::rusage>::uninit();
    // SAFETY: getrusage initializes the provided rusage on a zero return code.
    let result = unsafe { libc::getrusage(libc::RUSAGE_SELF, usage.as_mut_ptr()) };
    if result != 0 {
        return None;
    }
    // SAFETY: the successful getrusage call initialized `usage`.
    let usage = unsafe { usage.assume_init() };
    let maximum = u64::try_from(usage.ru_maxrss).ok()?;
    #[cfg(target_os = "macos")]
    {
        Some(maximum)
    }
    #[cfg(not(target_os = "macos"))]
    {
        maximum.checked_mul(1024)
    }
}

pub(crate) fn record_scratch_bytes(path: &Path) -> Option<u64> {
    fn directory_bytes(path: &Path) -> std::io::Result<u64> {
        let metadata = std::fs::symlink_metadata(path)?;
        if metadata.is_file() {
            return Ok(metadata.len());
        }
        if !metadata.is_dir() {
            return Ok(0);
        }
        let mut total = 0_u64;
        for entry in std::fs::read_dir(path)? {
            total = total.saturating_add(directory_bytes(&entry?.path())?);
        }
        Ok(total)
    }

    let current = directory_bytes(path).ok()?;
    PEAK_SCRATCH_BYTES.fetch_max(current, Ordering::Relaxed);
    Some(current)
}

pub(crate) fn peak_scratch_bytes() -> Option<u64> {
    let bytes = PEAK_SCRATCH_BYTES.load(Ordering::Relaxed);
    (bytes > 0).then_some(bytes)
}

pub(crate) fn host_evidence(scratch: &Path) -> HostEvidence {
    const REQUIRED_LOGICAL_CPUS: usize = 16;
    const REQUIRED_MEMORY_BYTES: u64 = 64 * 1024 * 1024 * 1024;
    let logical_cpus = std::thread::available_parallelism()
        .map(std::num::NonZeroUsize::get)
        .unwrap_or_default();
    let total_memory_bytes = total_memory_bytes();
    let scratch_available_bytes = filesystem_available_bytes(scratch);
    HostEvidence {
        logical_cpus,
        total_memory_bytes,
        scratch_available_bytes,
        required_logical_cpus: REQUIRED_LOGICAL_CPUS,
        required_memory_bytes: REQUIRED_MEMORY_BYTES,
        profile_passed: logical_cpus >= REQUIRED_LOGICAL_CPUS
            && total_memory_bytes.is_some_and(|bytes| bytes >= REQUIRED_MEMORY_BYTES),
        operating_system: std::env::consts::OS.to_string(),
        architecture: std::env::consts::ARCH.to_string(),
        rustc: command_version("rustc", &["--version", "--verbose"]),
    }
}

fn command_version(command: &str, arguments: &[&str]) -> String {
    Command::new(command)
        .args(arguments)
        .output()
        .ok()
        .filter(|output| output.status.success())
        .map(|output| String::from_utf8_lossy(&output.stdout).trim().to_string())
        .unwrap_or_else(|| "unavailable".to_string())
}

fn total_memory_bytes() -> Option<u64> {
    let sysctl = Command::new("sysctl")
        .args(["-n", "hw.memsize"])
        .output()
        .ok()
        .filter(|output| output.status.success())
        .and_then(|output| String::from_utf8(output.stdout).ok())
        .and_then(|value| value.trim().parse().ok());
    if sysctl.is_some() {
        return sysctl;
    }
    let meminfo = std::fs::read_to_string("/proc/meminfo").ok()?;
    let kibibytes = meminfo
        .lines()
        .find_map(|line| line.strip_prefix("MemTotal:"))?
        .split_whitespace()
        .next()?
        .parse::<u64>()
        .ok()?;
    kibibytes.checked_mul(1024)
}

fn filesystem_available_bytes(path: &Path) -> Option<u64> {
    let output = Command::new("df").args(["-kP"]).arg(path).output().ok()?;
    if !output.status.success() {
        return None;
    }
    let text = String::from_utf8(output.stdout).ok()?;
    let available_kibibytes = text
        .lines()
        .last()?
        .split_whitespace()
        .nth(3)?
        .parse::<u64>()
        .ok()?;
    available_kibibytes.checked_mul(1024)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn fitted_exponent_uses_every_scale_point() {
        let points = [1_u64, 10, 100]
            .into_iter()
            .map(|rows| ScalePoint {
                report: rows.to_string(),
                rows,
                source_physical_rows: rows.saturating_mul(2),
                source_physical_bytes: rows.saturating_mul(20),
                runtime_millis: rows.saturating_mul(7),
                requests: rows.saturating_mul(3),
                transferred_bytes: rows.saturating_mul(11),
                storage_bytes: rows.saturating_mul(5),
                peak_rss_bytes: rows.saturating_mul(2),
                peak_scratch_bytes: rows.saturating_mul(3),
            })
            .collect::<Vec<_>>();

        let exponent = fitted_exponent(&points, |point| point.runtime_millis)
            .expect("distinct positive scale points fit");

        assert!((exponent - 1.0).abs() < 1e-12);
    }

    #[test]
    fn amplification_change_reports_both_growth_and_reduction() {
        assert!((amplification_change(100, 10, 120, 10) - 0.2).abs() < 1e-12);
        assert!((amplification_change(100, 10, 80, 10) + 0.2).abs() < 1e-12);
        assert_eq!(amplification_change(0, 10, 0, 100), 0.0);
        assert!(amplification_change(0, 10, 1, 100).is_infinite());
    }

    #[test]
    fn scale_measurement_includes_fixture_seed_and_rollback_rehearsal() {
        let scenario = serde_json::json!({
            "timings_millis": {
                "total": 100,
                "seed": 10,
                "snapshot_restore": 5
            },
            "object_store_phases": {
                "source_seed": {
                    "get_requests": 1_000,
                    "bytes_read": 10_000
                },
                "source_oracle": {
                    "get_requests": 2,
                    "bytes_read": 3
                },
                "source_garbage_collection": {
                    "list_requests": 7,
                    "delete_requests": 11,
                    "bytes_read": 13
                },
                "target_rewrite": {
                    "put_requests": 4,
                    "bytes_written": 5
                },
                "snapshot_restore": {
                    "copy_requests": 2_000,
                    "bytes_written": 20_000
                }
            }
        });

        assert_eq!(json_scale_runtime(&scenario), 100);
        assert_eq!(json_scenario_requests(&scenario), 3_024);
        assert_eq!(json_scenario_bytes(&scenario), 30_021);
    }

    #[test]
    fn worst_adjacent_exponent_detects_a_superlinear_interval() {
        let points = [
            ScalePoint {
                report: "small".to_string(),
                rows: 100,
                source_physical_rows: 200,
                source_physical_bytes: 2_000,
                runtime_millis: 100,
                requests: 100,
                transferred_bytes: 100,
                storage_bytes: 100,
                peak_rss_bytes: 100,
                peak_scratch_bytes: 100,
            },
            ScalePoint {
                report: "medium".to_string(),
                rows: 1_000,
                source_physical_rows: 2_000,
                source_physical_bytes: 20_000,
                runtime_millis: 1_000,
                requests: 1_000,
                transferred_bytes: 1_000,
                storage_bytes: 1_000,
                peak_rss_bytes: 1_000,
                peak_scratch_bytes: 1_000,
            },
            ScalePoint {
                report: "large".to_string(),
                rows: 10_000,
                source_physical_rows: 20_000,
                source_physical_bytes: 200_000,
                runtime_millis: 100_000,
                requests: 10_000,
                transferred_bytes: 10_000,
                storage_bytes: 10_000,
                peak_rss_bytes: 10_000,
                peak_scratch_bytes: 10_000,
            },
        ];
        let exponent = worst_adjacent_exponent(&points, |point| point.runtime_millis)
            .expect("adjacent exponent fits");
        assert!((exponent - 2.0).abs() < 1e-12);
    }

    #[test]
    fn resource_projection_uses_worst_growth_and_safety_margin() {
        assert_eq!(conservative_projection_exponent(None, None), 1.0);
        assert_eq!(conservative_projection_exponent(Some(0.8), Some(0.9)), 1.0);
        assert_eq!(conservative_projection_exponent(Some(1.05), Some(1.2)), 1.2);
        assert_eq!(project_with_safety(1_000, 5.0, 1.0, 1.5), 7_500);
        assert!(project_with_safety(1_000, 5.0, 1.1, 1.5) > 7_500);
        let capacity_gate = 20_000_u64.saturating_mul(70) / 100;
        assert!(project_with_safety(1_000, 5.0, 1.0, 1.5) < capacity_gate);
        assert!(project_with_safety(2_000, 5.0, 1.0, 1.5) >= capacity_gate);
    }

    #[test]
    fn scratch_exponent_gate_rejects_fitted_or_adjacent_superlinear_growth() {
        assert!(!scratch_exponent_gate_passed(true, 1.100_001, 1.0));
        assert!(!scratch_exponent_gate_passed(true, 1.0, 1.100_001));
    }

    #[test]
    fn scratch_exponent_gate_accepts_the_exact_storage_limit() {
        assert!(scratch_exponent_gate_passed(true, 1.10, 1.10));
    }

    #[test]
    fn scratch_exponent_gate_defers_below_the_row_threshold() {
        assert!(scratch_exponent_gate_passed(false, 9.0, 12.0));
    }
}
