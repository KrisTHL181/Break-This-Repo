//! Provider-neutral experimental search authoring for Workflow.
//!
//! This module is an authoring and freeze boundary, not a new runtime or
//! scheduler. A validated search still has to be lowered by the Workflow host
//! into Fleet workers plus a runtime-owned evaluator. In particular, worker
//! self-reports are never promoted to hard-gate evidence here.

use serde::{Deserialize, Serialize};
use sha2::{Digest, Sha256};
use std::path::{Component, Path};
use thiserror::Error;

use crate::{DEFAULT_FLEET_WORKFLOW_MAX_AGENTS, experimental_search::SearchSpecError::*};

pub const WORKFLOW_SEARCH_SCHEMA_VERSION: u32 = 1;
/// Fallback live-worker ceiling for one search admission batch.
///
/// This is the answer when the host resolves no Fleet concurrency limit at
/// all — not a second knob. 16 matches the Workflow host's live-child ceiling
/// today (`codewhale_workflow_js::WORKFLOW_MAX_CONCURRENT`, from which the tui
/// driver sizes its per-run admission semaphore). This crate cannot import
/// that constant directly because `codewhale-workflow-js` depends on
/// `codewhale-workflow`.
///
/// Prefer passing the live limit: every validation entry point has a
/// `*_with_limit` twin that takes the resolved Fleet ceiling, and the frozen
/// receipt records which of the two actually bounded the search.
pub const WORKFLOW_SEARCH_DEFAULT_MAX_CONCURRENT: u16 = 16;

/// Where a search's live-worker ceiling came from.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize, Default)]
#[serde(rename_all = "snake_case")]
pub enum SearchConcurrencySource {
    /// Resolved from the host's Fleet concurrency configuration.
    FleetLimit,
    /// No Fleet limit resolved, so [`WORKFLOW_SEARCH_DEFAULT_MAX_CONCURRENT`]
    /// applied.
    #[default]
    Default,
}

/// The live-worker ceiling that bounded a search, plus where it came from.
///
/// Carried on the frozen receipt so an operator reading a run can tell a
/// deliberately small Fleet pool from this crate's fallback.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub struct ResolvedSearchConcurrency {
    pub limit: u16,
    pub source: SearchConcurrencySource,
}

impl Default for ResolvedSearchConcurrency {
    fn default() -> Self {
        Self::resolve(None)
    }
}

impl ResolvedSearchConcurrency {
    /// Resolve the ceiling from an already-resolved Fleet limit.
    ///
    /// `None` — and a nonsensical `Some(0)`, which would admit nothing — fall
    /// back to [`WORKFLOW_SEARCH_DEFAULT_MAX_CONCURRENT`].
    #[must_use]
    pub fn resolve(fleet_limit: Option<u16>) -> Self {
        match fleet_limit.filter(|limit| *limit > 0) {
            Some(limit) => Self {
                limit,
                source: SearchConcurrencySource::FleetLimit,
            },
            None => Self {
                limit: WORKFLOW_SEARCH_DEFAULT_MAX_CONCURRENT,
                source: SearchConcurrencySource::Default,
            },
        }
    }

    /// Resolve straight from the two config seams the host already owns:
    /// `[workflow] max_concurrent` (the per-run live-agent ceiling, see
    /// `codewhale_config::WorkflowConfigToml::max_concurrent`) and a Fleet
    /// profile's `delegation.max_concurrency` hint
    /// (`codewhale_config::FleetDelegationHints::max_concurrency`).
    ///
    /// The lower of the present values wins: a profile that asks for fewer
    /// workers than the run allows is a real bound, and a run ceiling below a
    /// profile hint is the admission the host will actually grant.
    #[must_use]
    pub fn from_fleet_config(
        workflow_max_concurrent: Option<u32>,
        profile_max_concurrency: Option<usize>,
    ) -> Self {
        let workflow =
            workflow_max_concurrent.map(|value| u16::try_from(value).unwrap_or(u16::MAX));
        let profile = profile_max_concurrency.map(|value| u16::try_from(value).unwrap_or(u16::MAX));
        let resolved = match (workflow.filter(|v| *v > 0), profile.filter(|v| *v > 0)) {
            (Some(left), Some(right)) => Some(left.min(right)),
            (Some(value), None) | (None, Some(value)) => Some(value),
            (None, None) => None,
        };
        Self::resolve(resolved)
    }

    /// One-line receipt copy naming the ceiling and its origin.
    #[must_use]
    pub fn receipt_line(&self) -> String {
        let origin = match self.source {
            SearchConcurrencySource::FleetLimit => "resolved Fleet limit",
            SearchConcurrencySource::Default => "default, no Fleet limit resolved",
        };
        format!("live-worker ceiling {limit} ({origin})", limit = self.limit)
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct WorkflowSearchSpec {
    #[serde(default = "default_schema_version")]
    pub schema_version: u32,
    pub name: String,
    pub objective: String,
    pub population: u16,
    pub rounds: Vec<u16>,
    pub concurrency: u16,
    pub worker: SearchWorkerSpec,
    #[serde(default)]
    pub budget: SearchBudgetSpec,
    pub hard_gates: SearchHardGateSpec,
    pub score: SearchScoreSpec,
    #[serde(default)]
    pub selection: SearchSelectionSpec,
    #[serde(default)]
    pub integration_policy: SearchIntegrationPolicy,
}

impl WorkflowSearchSpec {
    /// Parse and validate against the fallback ceiling. Hosts that know their
    /// Fleet limit should call [`Self::from_toml_with_limit`].
    pub fn from_toml(source: &str) -> Result<Self, SearchSpecError> {
        Self::from_toml_with_limit(source, None)
    }

    /// Parse and validate against the resolved Fleet concurrency limit.
    pub fn from_toml_with_limit(
        source: &str,
        fleet_limit: Option<u16>,
    ) -> Result<Self, SearchSpecError> {
        let spec: Self = toml::from_str(source).map_err(|error| Parse(error.to_string()))?;
        spec.validate_with_limit(fleet_limit)?;
        Ok(spec)
    }

    pub fn validate(&self) -> Result<(), SearchSpecError> {
        self.validate_with_limit(None).map(|_| ())
    }

    /// Validate against the resolved Fleet concurrency limit, returning the
    /// ceiling that applied so the caller can echo it in a receipt.
    pub fn validate_with_limit(
        &self,
        fleet_limit: Option<u16>,
    ) -> Result<ResolvedSearchConcurrency, SearchSpecError> {
        let concurrency_ceiling = ResolvedSearchConcurrency::resolve(fleet_limit);
        if self.schema_version != WORKFLOW_SEARCH_SCHEMA_VERSION {
            return Err(UnsupportedSchemaVersion(self.schema_version));
        }
        validate_name(&self.name)?;
        validate_text("objective", &self.objective, 32_768)?;
        if !(2..=DEFAULT_FLEET_WORKFLOW_MAX_AGENTS as u16).contains(&self.population) {
            return Err(InvalidPopulation(self.population));
        }
        if self.concurrency == 0
            || self.concurrency > concurrency_ceiling.limit
            || self.concurrency > self.population
        {
            return Err(InvalidConcurrency {
                concurrency: self.concurrency,
                population: self.population,
                limit: concurrency_ceiling.limit,
            });
        }
        validate_rounds(self.population, &self.rounds)?;
        validate_text("worker.model", &self.worker.model, 256)?;
        if self.worker.write_roots.is_empty() && self.worker.exact_files.is_empty() {
            return Err(UnboundedWriteScope);
        }
        validate_repo_relative_paths("worker.write_roots", &self.worker.write_roots, 128)?;
        validate_repo_relative_paths("worker.exact_files", &self.worker.exact_files, 256)?;
        if self.budget.max_cost_microusd == Some(0) {
            return Err(ZeroBudget("max_cost_microusd"));
        }
        if self.budget.max_tokens == Some(0) {
            return Err(ZeroBudget("max_tokens"));
        }
        if !self.hard_gates.forbid_test_changes {
            return Err(TestWeakeningAllowed);
        }
        if self.hard_gates.commands.is_empty() {
            return Err(MissingHardGates);
        }
        validate_string_list("hard_gates.commands", &self.hard_gates.commands, 32, 4_096)?;
        validate_string_list(
            "hard_gates.protected_paths",
            &self.hard_gates.protected_paths,
            256,
            1_024,
        )?;
        validate_text("score.command", &self.score.command, 4_096)?;
        validate_text("score.metric", &self.score.metric, 256)?;
        if !(1..=25).contains(&self.score.trials) {
            return Err(InvalidTrials(self.score.trials));
        }
        if self.score.tie_breakers.is_empty() {
            return Err(MissingTieBreakers);
        }
        Ok(concurrency_ceiling)
    }

    /// Freeze the exact public inputs and evaluator identity before admission.
    /// The evaluator bytes are hashed, not exposed to generation workers.
    pub fn freeze(
        &self,
        baseline_commit: &str,
        resolved_model: &str,
        public_evidence: &[u8],
        evaluator: &[u8],
    ) -> Result<FrozenWorkflowSearch, SearchSpecError> {
        self.freeze_with_limit(
            baseline_commit,
            resolved_model,
            public_evidence,
            evaluator,
            None,
        )
    }

    /// Freeze against the resolved Fleet concurrency limit.
    ///
    /// The resolved ceiling is recorded on the receipt but deliberately kept
    /// out of the preregistration hash: the hash freezes the scientific inputs
    /// (spec, model, public evidence, evaluator identity), and how many
    /// workers the operator's pool happened to allow is an operational fact
    /// about the run, not part of what was preregistered.
    pub fn freeze_with_limit(
        &self,
        baseline_commit: &str,
        resolved_model: &str,
        public_evidence: &[u8],
        evaluator: &[u8],
        fleet_limit: Option<u16>,
    ) -> Result<FrozenWorkflowSearch, SearchSpecError> {
        let resolved_concurrency = self.validate_with_limit(fleet_limit)?;
        validate_commit(baseline_commit)?;
        validate_text("resolved_model", resolved_model, 256)?;
        if evaluator.is_empty() {
            return Err(EmptyEvaluator);
        }

        let public_evidence_hash = sha256_label(public_evidence);
        let evaluator_hash = sha256_label(evaluator);
        let freeze_input = FreezeInput {
            spec: self,
            baseline_commit,
            requested_model: &self.worker.model,
            resolved_model,
            public_evidence_hash: &public_evidence_hash,
            evaluator_hash: &evaluator_hash,
        };
        let encoded =
            serde_json::to_vec(&freeze_input).map_err(|error| FreezeEncoding(error.to_string()))?;
        let preregistration_hash = sha256_label(&encoded);
        let search_id = format!("search-{}", &preregistration_hash[7..23]);

        Ok(FrozenWorkflowSearch {
            schema_version: self.schema_version,
            search_id,
            baseline_commit: baseline_commit.to_string(),
            preregistration_hash,
            public_evidence_hash,
            evaluator_hash,
            requested_model: self.worker.model.clone(),
            resolved_model: resolved_model.to_string(),
            candidate_ids: self.candidate_ids(),
            resolved_concurrency,
        })
    }

    #[must_use]
    pub fn candidate_ids(&self) -> Vec<String> {
        let width = self.population.to_string().len().max(3);
        (1..=self.population)
            .map(|index| format!("cand_{index:0width$}"))
            .collect()
    }

    /// Deterministic admission batches. Fleet owns actual scheduling and may
    /// run fewer workers when its configured pool or provider quota is lower.
    pub fn admission_batches(&self) -> Result<Vec<Vec<String>>, SearchSpecError> {
        self.admission_batches_with_limit(None)
    }

    /// Deterministic admission batches, validated against the resolved Fleet
    /// concurrency limit.
    pub fn admission_batches_with_limit(
        &self,
        fleet_limit: Option<u16>,
    ) -> Result<Vec<Vec<String>>, SearchSpecError> {
        self.validate_with_limit(fleet_limit)?;
        Ok(self
            .candidate_ids()
            .chunks(usize::from(self.concurrency))
            .map(<[String]>::to_vec)
            .collect())
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct SearchWorkerSpec {
    #[serde(default)]
    pub provider: Option<String>,
    pub model: String,
    #[serde(default)]
    pub reasoning_effort: SearchReasoningEffort,
    #[serde(default)]
    pub write_authority: SearchWriteAuthority,
    #[serde(default)]
    pub write_roots: Vec<String>,
    #[serde(default)]
    pub exact_files: Vec<String>,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize, Default)]
#[serde(rename_all = "snake_case")]
pub enum SearchReasoningEffort {
    Off,
    Low,
    Medium,
    #[default]
    High,
    Max,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize, Default)]
#[serde(rename_all = "snake_case")]
pub enum SearchWriteAuthority {
    #[default]
    WorktreeWrite,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize, Default)]
pub struct SearchBudgetSpec {
    #[serde(default)]
    pub max_cost_microusd: Option<u64>,
    #[serde(default)]
    pub max_tokens: Option<u64>,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct SearchHardGateSpec {
    pub commands: Vec<String>,
    #[serde(default = "default_true")]
    pub forbid_test_changes: bool,
    #[serde(default)]
    pub protected_paths: Vec<String>,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct SearchScoreSpec {
    pub command: String,
    pub metric: String,
    #[serde(default)]
    pub direction: SearchDirection,
    #[serde(default = "default_trials")]
    pub trials: u16,
    #[serde(default)]
    pub tie_breakers: Vec<SearchTieBreaker>,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize, Default)]
#[serde(rename_all = "snake_case")]
pub enum SearchDirection {
    #[default]
    Minimize,
    Maximize,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum SearchTieBreaker {
    DiffLines,
    CostMicrousd,
    Score,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct SearchSelectionSpec {
    #[serde(default)]
    pub policy: SearchSelectionPolicy,
    #[serde(default = "default_true")]
    pub retain_diversity: bool,
    #[serde(default)]
    pub ordering: Vec<SearchSelectionMetric>,
}

impl Default for SearchSelectionSpec {
    fn default() -> Self {
        Self {
            policy: SearchSelectionPolicy::Pareto,
            retain_diversity: true,
            ordering: Vec::new(),
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize, Default)]
#[serde(rename_all = "snake_case")]
pub enum SearchSelectionPolicy {
    #[default]
    Pareto,
    Ordered,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum SearchSelectionMetric {
    Score,
    Runtime,
    DiffLines,
    CostMicrousd,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize, Default)]
#[serde(rename_all = "snake_case")]
pub enum SearchIntegrationPolicy {
    /// Produce a verified, reviewable winner or NONE. Never apply or merge it.
    #[default]
    ReviewOnly,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct FrozenWorkflowSearch {
    pub schema_version: u32,
    pub search_id: String,
    pub baseline_commit: String,
    pub preregistration_hash: String,
    pub public_evidence_hash: String,
    pub evaluator_hash: String,
    pub requested_model: String,
    pub resolved_model: String,
    pub candidate_ids: Vec<String>,
    /// The live-worker ceiling that bounded this search, and whether it came
    /// from Fleet config or the crate fallback. `#[serde(default)]` so
    /// receipts written before the bound was recorded still load.
    #[serde(default)]
    pub resolved_concurrency: ResolvedSearchConcurrency,
}

impl FrozenWorkflowSearch {
    /// Receipt line naming the ceiling that actually bounded this run.
    #[must_use]
    pub fn concurrency_receipt_line(&self) -> String {
        self.resolved_concurrency.receipt_line()
    }
}

#[derive(Serialize)]
struct FreezeInput<'a> {
    spec: &'a WorkflowSearchSpec,
    baseline_commit: &'a str,
    requested_model: &'a str,
    resolved_model: &'a str,
    public_evidence_hash: &'a str,
    evaluator_hash: &'a str,
}

#[derive(Debug, Clone, PartialEq, Eq, Error)]
pub enum SearchSpecError {
    #[error("failed to parse Workflow search TOML: {0}")]
    Parse(String),
    #[error("unsupported Workflow search schema version {0}")]
    UnsupportedSchemaVersion(u32),
    #[error("search name must be a 1-96 character lowercase token")]
    InvalidName,
    #[error("{field} must be non-empty and no longer than {max} characters")]
    InvalidText { field: &'static str, max: usize },
    #[error("population {0} must be between 2 and 1000")]
    InvalidPopulation(u16),
    #[error(
        "concurrency {concurrency} must be between 1 and {limit} (resolved live-worker ceiling) and no greater than population {population}"
    )]
    InvalidConcurrency {
        concurrency: u16,
        population: u16,
        limit: u16,
    },
    #[error("rounds must start at population, decrease strictly, and end at 1")]
    InvalidRounds,
    #[error("write-capable search workers require write_roots or exact_files")]
    UnboundedWriteScope,
    #[error("{field} contains an empty, oversized, or duplicate entry")]
    InvalidStringList { field: &'static str },
    #[error("{field} entries must be bounded repo-relative paths without parent traversal")]
    InvalidWriteScope { field: &'static str },
    #[error("{0} must be greater than zero when set")]
    ZeroBudget(&'static str),
    #[error("experimental search must forbid test changes")]
    TestWeakeningAllowed,
    #[error("experimental search requires at least one runtime-owned hard-gate command")]
    MissingHardGates,
    #[error("score.trials must be between 1 and 25, got {0}")]
    InvalidTrials(u16),
    #[error("experimental search requires at least one deterministic tie-breaker")]
    MissingTieBreakers,
    #[error("baseline_commit must be a 7-64 character hexadecimal commit id")]
    InvalidBaselineCommit,
    #[error("evaluator bytes must be non-empty")]
    EmptyEvaluator,
    #[error("failed to encode frozen Workflow search: {0}")]
    FreezeEncoding(String),
}

fn default_schema_version() -> u32 {
    WORKFLOW_SEARCH_SCHEMA_VERSION
}

fn default_trials() -> u16 {
    5
}

fn default_true() -> bool {
    true
}

fn validate_name(value: &str) -> Result<(), SearchSpecError> {
    if value.is_empty()
        || value.len() > 96
        || !value
            .bytes()
            .all(|byte| byte.is_ascii_lowercase() || byte.is_ascii_digit() || b"-_".contains(&byte))
    {
        return Err(InvalidName);
    }
    Ok(())
}

fn validate_text(field: &'static str, value: &str, max: usize) -> Result<(), SearchSpecError> {
    if value.trim().is_empty() || value.chars().count() > max {
        return Err(InvalidText { field, max });
    }
    Ok(())
}

fn validate_rounds(population: u16, rounds: &[u16]) -> Result<(), SearchSpecError> {
    if rounds.len() < 2
        || rounds.first() != Some(&population)
        || rounds.last() != Some(&1)
        || rounds.windows(2).any(|pair| pair[0] <= pair[1])
    {
        return Err(InvalidRounds);
    }
    Ok(())
}

fn validate_string_list(
    field: &'static str,
    values: &[String],
    max_items: usize,
    max_chars: usize,
) -> Result<(), SearchSpecError> {
    if values.len() > max_items
        || values
            .iter()
            .any(|value| value.trim().is_empty() || value.chars().count() > max_chars)
        || values
            .iter()
            .enumerate()
            .any(|(index, value)| values[..index].contains(value))
    {
        return Err(InvalidStringList { field });
    }
    Ok(())
}

fn validate_repo_relative_paths(
    field: &'static str,
    values: &[String],
    max_items: usize,
) -> Result<(), SearchSpecError> {
    validate_string_list(field, values, max_items, 1_024)?;
    if values.iter().any(|value| {
        let trimmed = value.trim();
        let normalized = trimmed.replace('\\', "/");
        let windows_drive = normalized.as_bytes().get(1) == Some(&b':')
            && normalized
                .as_bytes()
                .first()
                .is_some_and(u8::is_ascii_alphabetic);
        trimmed != value
            || trimmed.chars().any(|ch| matches!(ch, '\0' | '\r' | '\n'))
            || windows_drive
            || Path::new(&normalized).is_absolute()
            || Path::new(&normalized).components().any(|component| {
                matches!(
                    component,
                    Component::ParentDir | Component::RootDir | Component::Prefix(_)
                )
            })
    }) {
        return Err(InvalidWriteScope { field });
    }
    Ok(())
}

fn validate_commit(value: &str) -> Result<(), SearchSpecError> {
    if !(7..=64).contains(&value.len()) || !value.bytes().all(|byte| byte.is_ascii_hexdigit()) {
        return Err(InvalidBaselineCommit);
    }
    Ok(())
}

fn sha256_label(bytes: &[u8]) -> String {
    let digest = Sha256::digest(bytes);
    let mut output = String::with_capacity(71);
    output.push_str("sha256:");
    for byte in digest {
        use std::fmt::Write as _;
        let _ = write!(output, "{byte:02x}");
    }
    output
}

#[cfg(test)]
mod tests {
    use super::*;

    const SPEC: &str = r#"
name = "speed-up-certificate"
objective = "Reduce runtime without changing exact results"
population = 32
rounds = [32, 8, 3, 1]
concurrency = 16
integration_policy = "review_only"

[worker]
provider = "deepseek"
model = "deepseek-v4-flash"
reasoning_effort = "max"
write_authority = "worktree_write"
write_roots = ["code"]

[budget]
max_cost_microusd = 5000000
max_tokens = 10000000

[hard_gates]
commands = ["cargo test --locked", "git diff --exit-code -- expected.json"]
forbid_test_changes = true
protected_paths = ["tests", "expected.json"]

[score]
command = "./scripts/benchmark_candidate.sh"
direction = "minimize"
metric = "median_runtime_ms"
trials = 5
tie_breakers = ["diff_lines", "cost_microusd"]

[selection]
policy = "pareto"
retain_diversity = true
"#;

    #[test]
    fn parses_valid_search_and_queues_through_live_cap() {
        let spec = WorkflowSearchSpec::from_toml(SPEC).expect("valid search spec");

        assert_eq!(spec.population, 32);
        let batches = spec.admission_batches().expect("validated admission");
        assert_eq!(batches.len(), 2);
        assert_eq!(batches[0].len(), 16);
        assert_eq!(spec.candidate_ids()[0], "cand_001");
        assert_eq!(spec.candidate_ids()[31], "cand_032");
    }

    #[test]
    fn freeze_is_deterministic_and_model_version_sensitive() {
        let spec = WorkflowSearchSpec::from_toml(SPEC).expect("valid search spec");
        let first = spec
            .freeze(
                "33bc6a98",
                "DeepSeek-V4-Flash-0731",
                b"public evidence",
                b"private evaluator",
            )
            .expect("freeze succeeds");
        let replay = spec
            .freeze(
                "33bc6a98",
                "DeepSeek-V4-Flash-0731",
                b"public evidence",
                b"private evaluator",
            )
            .expect("freeze succeeds");
        let drifted = spec
            .freeze(
                "33bc6a98",
                "DeepSeek-V4-Flash-next",
                b"public evidence",
                b"private evaluator",
            )
            .expect("freeze succeeds");

        assert_eq!(first, replay);
        assert_ne!(first.search_id, drifted.search_id);
        assert_ne!(first.preregistration_hash, drifted.preregistration_hash);
        assert_eq!(first.requested_model, "deepseek-v4-flash");
        assert_eq!(first.resolved_model, "DeepSeek-V4-Flash-0731");
    }

    #[test]
    fn rejects_unsafe_or_unbounded_searches() {
        let mut spec = WorkflowSearchSpec::from_toml(SPEC).expect("valid search spec");
        spec.hard_gates.forbid_test_changes = false;
        assert_eq!(spec.validate(), Err(SearchSpecError::TestWeakeningAllowed));

        spec.hard_gates.forbid_test_changes = true;
        spec.worker.write_roots.clear();
        assert_eq!(spec.validate(), Err(SearchSpecError::UnboundedWriteScope));
    }

    #[test]
    fn rejects_invalid_rounds_and_excess_live_concurrency() {
        let mut spec = WorkflowSearchSpec::from_toml(SPEC).expect("valid search spec");
        spec.rounds = vec![32, 8, 8, 1];
        assert_eq!(spec.validate(), Err(SearchSpecError::InvalidRounds));

        spec.rounds = vec![32, 1];
        spec.concurrency = 17;
        assert_eq!(
            spec.validate(),
            Err(SearchSpecError::InvalidConcurrency {
                concurrency: 17,
                population: 32,
                limit: 16,
            })
        );
    }

    #[test]
    fn ceiling_follows_the_resolved_fleet_limit() {
        let mut spec = WorkflowSearchSpec::from_toml(SPEC).expect("valid search spec");

        // A smaller Fleet pool rejects the spec's 16-wide batches...
        assert_eq!(
            spec.validate_with_limit(Some(8)),
            Err(SearchSpecError::InvalidConcurrency {
                concurrency: 16,
                population: 32,
                limit: 8,
            })
        );

        // ...and a larger one admits concurrency the fallback would refuse.
        spec.concurrency = 24;
        let resolved = spec
            .validate_with_limit(Some(32))
            .expect("24 workers fit a 32-wide Fleet limit");
        assert_eq!(
            resolved,
            ResolvedSearchConcurrency {
                limit: 32,
                source: SearchConcurrencySource::FleetLimit,
            }
        );
        let batches = spec
            .admission_batches_with_limit(Some(32))
            .expect("validated admission");
        assert_eq!(batches.len(), 2);
        assert_eq!(batches[0].len(), 24);
    }

    #[test]
    fn fallback_ceiling_stays_sixteen_when_no_limit_resolves() {
        let mut spec = WorkflowSearchSpec::from_toml(SPEC).expect("valid search spec");
        assert_eq!(
            spec.validate_with_limit(None),
            Ok(ResolvedSearchConcurrency {
                limit: WORKFLOW_SEARCH_DEFAULT_MAX_CONCURRENT,
                source: SearchConcurrencySource::Default,
            })
        );
        assert_eq!(WORKFLOW_SEARCH_DEFAULT_MAX_CONCURRENT, 16);

        // A zero limit is not a ceiling of zero — it is an unresolved one.
        assert_eq!(
            spec.validate_with_limit(Some(0)),
            spec.validate_with_limit(None)
        );

        spec.concurrency = 17;
        assert_eq!(
            spec.validate_with_limit(None),
            Err(SearchSpecError::InvalidConcurrency {
                concurrency: 17,
                population: 32,
                limit: 16,
            })
        );
    }

    #[test]
    fn fleet_config_seam_takes_the_lower_present_bound() {
        assert_eq!(
            ResolvedSearchConcurrency::from_fleet_config(Some(16), Some(4)),
            ResolvedSearchConcurrency {
                limit: 4,
                source: SearchConcurrencySource::FleetLimit,
            }
        );
        assert_eq!(
            ResolvedSearchConcurrency::from_fleet_config(Some(6), None),
            ResolvedSearchConcurrency {
                limit: 6,
                source: SearchConcurrencySource::FleetLimit,
            }
        );
        assert_eq!(
            ResolvedSearchConcurrency::from_fleet_config(None, None),
            ResolvedSearchConcurrency::default()
        );
        assert_eq!(
            ResolvedSearchConcurrency::default().source,
            SearchConcurrencySource::Default
        );
    }

    #[test]
    fn freeze_receipt_echoes_the_bound_that_applied() {
        let spec = WorkflowSearchSpec::from_toml(SPEC).expect("valid search spec");

        let fallback = spec
            .freeze("33bc6a98", "DeepSeek-V4-Flash-0731", b"evidence", b"eval")
            .expect("freeze succeeds");
        assert_eq!(
            fallback.resolved_concurrency,
            ResolvedSearchConcurrency {
                limit: 16,
                source: SearchConcurrencySource::Default,
            }
        );
        assert_eq!(
            fallback.concurrency_receipt_line(),
            "live-worker ceiling 16 (default, no Fleet limit resolved)"
        );

        let bounded = spec
            .freeze_with_limit(
                "33bc6a98",
                "DeepSeek-V4-Flash-0731",
                b"evidence",
                b"eval",
                Some(16),
            )
            .expect("freeze succeeds");
        assert_eq!(
            bounded.resolved_concurrency,
            ResolvedSearchConcurrency {
                limit: 16,
                source: SearchConcurrencySource::FleetLimit,
            }
        );
        assert_eq!(
            bounded.concurrency_receipt_line(),
            "live-worker ceiling 16 (resolved Fleet limit)"
        );

        // The preregistration identity is unchanged by the operational bound.
        assert_eq!(
            fallback.preregistration_hash, bounded.preregistration_hash,
            "the resolved ceiling must not perturb the preregistration hash"
        );

        let receipt = serde_json::to_value(&bounded).expect("receipt serializes");
        assert_eq!(receipt["resolved_concurrency"]["limit"], 16);
        assert_eq!(receipt["resolved_concurrency"]["source"], "fleet_limit");
    }

    #[test]
    fn admission_refuses_unvalidated_zero_concurrency_without_panicking() {
        let mut spec = WorkflowSearchSpec::from_toml(SPEC).expect("valid search spec");
        spec.concurrency = 0;

        assert_eq!(
            spec.admission_batches(),
            Err(SearchSpecError::InvalidConcurrency {
                concurrency: 0,
                population: 32,
                limit: 16,
            })
        );
    }

    #[test]
    fn rejects_write_scopes_that_escape_or_obscure_the_repo_boundary() {
        let mut spec = WorkflowSearchSpec::from_toml(SPEC).expect("valid search spec");
        for unsafe_path in ["../outside", "/tmp/outside", r"C:\outside"] {
            spec.worker.write_roots = vec![unsafe_path.to_string()];
            assert_eq!(
                spec.validate(),
                Err(SearchSpecError::InvalidWriteScope {
                    field: "worker.write_roots",
                }),
                "path should be rejected: {unsafe_path}"
            );
        }
    }

    #[test]
    fn deserialization_refuses_auto_merge_policy() {
        let source = SPEC.replace("review_only", "auto_merge");
        let error = WorkflowSearchSpec::from_toml(&source).expect_err("must reject auto merge");

        assert!(matches!(error, SearchSpecError::Parse(_)));
    }
}
