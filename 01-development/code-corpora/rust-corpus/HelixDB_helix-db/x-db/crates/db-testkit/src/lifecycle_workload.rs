//! Seeded Index V2 lifecycle and foreground-traffic model runner.
//!
//! The runner uses the same state-specific wrappers for secondary, vector, and
//! text indexes. It records every request as a replayable trace and checks
//! physical visibility, mutation catch-up, work idempotency, blob reference
//! safety, and eventual retirement cleanup after every action.

use std::collections::{BTreeMap, BTreeSet};
use std::num::NonZeroU32;

use serde::{Deserialize, Serialize};

use crate::action::{
    Action, BackgroundAction, DurableInvariant, ElementKind, FaultAction, PropertyValue,
    ReadAction, RuntimeAction, StableFailpoint, VectorMetric, VectorValue, WriteAction,
};
use crate::ids::{
    EntityId, FiniteF32, GenerationId, IndexName, LabelName, PropertyName, RequestId, RuntimeId,
    Sequence, StableSeed, TenantId,
};
use crate::lifecycle::{
    AbsentIndex, ActiveIndex, BlockedIndex, BuildingIndex, IndexAction, IndexActionKind,
    IndexBlocker, IndexDefinition, IndexFamily, RetiredIndex,
};
use crate::model::{ModelIndexState, OracleState};
use crate::trace::{ObservedValue, ReplayTrace, TraceOutcome, TraceRecorder, TypedError};
use crate::{Result, TestkitError};

/// Positive bounded configuration for one deterministic lifecycle run.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub struct LifecycleRunConfig {
    random_steps: NonZeroU32,
    tenant_count: NonZeroU32,
    indexes_per_family: NonZeroU32,
}

impl LifecycleRunConfig {
    /// Validates one finite lifecycle workload shape.
    pub fn try_new(random_steps: u32, tenant_count: u32, indexes_per_family: u32) -> Result<Self> {
        let Some(random_steps) = NonZeroU32::new(random_steps) else {
            return Err(TestkitError::ModelViolation(
                "lifecycle random step count must be positive".to_string(),
            ));
        };
        let Some(tenant_count) = NonZeroU32::new(tenant_count) else {
            return Err(TestkitError::ModelViolation(
                "lifecycle tenant count must be positive".to_string(),
            ));
        };
        let Some(indexes_per_family) = NonZeroU32::new(indexes_per_family) else {
            return Err(TestkitError::ModelViolation(
                "lifecycle indexes per family must be positive".to_string(),
            ));
        };
        Ok(Self {
            random_steps,
            tenant_count,
            indexes_per_family,
        })
    }

    /// Bounded pull-request profile with four tenants and every index family.
    pub fn pull_request() -> Self {
        Self::try_new(256, 4, 1).expect("frozen pull-request lifecycle profile is positive")
    }

    /// Nightly profile with longer histories and two competing indexes per family.
    pub fn nightly() -> Self {
        Self::try_new(2_048, 4, 2).expect("frozen nightly lifecycle profile is positive")
    }

    /// Pre-launch profile with long histories and four indexes per family.
    pub fn pre_launch() -> Self {
        Self::try_new(8_192, 4, 4).expect("frozen pre-launch lifecycle profile is positive")
    }

    /// Returns the randomized suffix length.
    pub const fn random_steps(self) -> NonZeroU32 {
        self.random_steps
    }

    /// Returns the competing tenant count.
    pub const fn tenant_count(self) -> NonZeroU32 {
        self.tenant_count
    }

    /// Returns the logical index count per family and tenant.
    pub const fn indexes_per_family(self) -> NonZeroU32 {
        self.indexes_per_family
    }
}

/// Stable counters proving that one run exercised its required partitions.
#[derive(Debug, Clone, Copy, Default, PartialEq, Eq, Serialize, Deserialize)]
pub struct LifecycleCoverage {
    /// Successful lifecycle transitions.
    pub lifecycle_commits: u64,
    /// Foreground mutations applied during pending or active generations.
    pub foreground_commits: u64,
    /// Successful indexed or catalog reads.
    pub indexed_reads: u64,
    /// Typed limit blocks.
    pub blocked_builds: u64,
    /// Retried blocked generations.
    pub retries: u64,
    /// Aborted partial generations.
    pub aborts: u64,
    /// Process restart actions.
    pub restarts: u64,
    /// Stable failpoint actions.
    pub failpoints: u64,
    /// Duplicate work wakeups detected and ignored.
    pub duplicate_wakeups: u64,
    /// Reader activation or drain actions.
    pub reader_topology_changes: u64,
    /// Retired physical generations reclaimed.
    pub reclaims: u64,
    /// Corruption failures that preserved state.
    pub fail_closed_errors: u64,
}

/// Successful seeded run and its replay artifact.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct LifecycleRunReport {
    trace: ReplayTrace,
    coverage: LifecycleCoverage,
    max_live_generations: usize,
}

impl LifecycleRunReport {
    /// Borrows the validated replay trace.
    pub const fn trace(&self) -> &ReplayTrace {
        &self.trace
    }

    /// Returns stable exercised-path counters.
    pub const fn coverage(&self) -> LifecycleCoverage {
        self.coverage
    }

    /// Returns the maximum physical-generation resource set size.
    pub const fn max_live_generations(&self) -> usize {
        self.max_live_generations
    }
}

/// Seeded lifecycle simulator shared by all three index families.
#[derive(Debug, Default)]
pub struct LifecycleWorkload;

impl LifecycleWorkload {
    /// Runs one seeded workload, validates after every step, then drains all resources.
    pub fn run(&self, seed: StableSeed, config: LifecycleRunConfig) -> Result<LifecycleRunReport> {
        Runner::new(seed, config)?.run()
    }
}

#[derive(Debug, Clone)]
enum SlotState {
    Absent(AbsentIndex),
    Building(BuildingIndex),
    Blocked(BlockedIndex),
    Active(ActiveIndex),
    Retired(RetiredIndex),
}

#[derive(Debug, Clone)]
struct IndexSlot {
    tenant: TenantId,
    runtime: RuntimeId,
    state: SlotState,
}

#[derive(Debug, Clone, PartialEq, Eq, PartialOrd, Ord)]
struct GenerationKey {
    name: IndexName,
    generation: GenerationId,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum PhysicalVisibility {
    Pending,
    Active,
    Retired,
}

#[derive(Debug, Clone)]
struct PhysicalGenerationModel {
    visibility: PhysicalVisibility,
    created_at: Sequence,
    retired_at: Option<Sequence>,
    foreground_commits: BTreeSet<Sequence>,
    text_blob: Option<&'static str>,
}

struct Runner {
    rng: StableRng,
    config: LifecycleRunConfig,
    recorder: TraceRecorder,
    oracle: OracleState,
    slots: Vec<IndexSlot>,
    resources: BTreeMap<GenerationKey, PhysicalGenerationModel>,
    text_blob_references: BTreeMap<&'static str, usize>,
    foreground_commits: Vec<Sequence>,
    completed_work: BTreeSet<(GenerationKey, u8)>,
    next_request: u64,
    next_entity: u64,
    coverage: LifecycleCoverage,
    max_live_generations: usize,
}

impl Runner {
    fn new(seed: StableSeed, config: LifecycleRunConfig) -> Result<Self> {
        let mut slots = Vec::new();
        let families = [
            IndexFamily::Secondary,
            IndexFamily::Vector,
            IndexFamily::Text,
        ];
        for tenant_index in 0..config.tenant_count.get() {
            let tenant = TenantId::try_new(format!("tenant-{tenant_index}"))?;
            let runtime = RuntimeId::new(tenant_index + 2)?;
            for family in families {
                for index in 0..config.indexes_per_family.get() {
                    let name = IndexName::try_new(format!(
                        "tenant-{tenant_index}-{}-{index}",
                        match family {
                            IndexFamily::Secondary => "secondary",
                            IndexFamily::Vector => "vector",
                            IndexFamily::Text => "text",
                        }
                    ))?;
                    let definition = match family {
                        IndexFamily::Secondary => IndexDefinition::Secondary {
                            name,
                            element: ElementKind::Node,
                            property: PropertyName::try_new("rank")?,
                            unique: false,
                        },
                        IndexFamily::Vector => IndexDefinition::Vector {
                            name,
                            element: ElementKind::Node,
                            property: PropertyName::try_new("vector")?,
                            dimension: NonZeroU32::new(2).expect("vector dimension is positive"),
                            metric: VectorMetric::Euclidean,
                        },
                        IndexFamily::Text => IndexDefinition::Text {
                            name,
                            element: ElementKind::Node,
                            property: PropertyName::try_new("text")?,
                        },
                    };
                    slots.push(IndexSlot {
                        tenant: tenant.clone(),
                        runtime,
                        state: SlotState::Absent(AbsentIndex::new(definition)),
                    });
                }
            }
        }
        Ok(Self {
            rng: StableRng::new(seed),
            config,
            recorder: TraceRecorder::new(seed),
            oracle: OracleState::default(),
            slots,
            resources: BTreeMap::new(),
            text_blob_references: BTreeMap::new(),
            foreground_commits: Vec::new(),
            completed_work: BTreeSet::new(),
            next_request: 1,
            next_entity: 1,
            coverage: LifecycleCoverage::default(),
            max_live_generations: 0,
        })
    }

    fn run(mut self) -> Result<LifecycleRunReport> {
        self.record_runtime(
            RuntimeId::new(1)?,
            TenantId::try_new("control")?,
            RuntimeAction::OpenWriter,
        )?;
        for tenant_index in 0..self.config.tenant_count.get() {
            self.record_runtime(
                RuntimeId::new(tenant_index + 2)?,
                TenantId::try_new(format!("tenant-{tenant_index}"))?,
                RuntimeAction::AddReader {
                    runtime: RuntimeId::new(tenant_index + 2)?,
                },
            )?;
            self.coverage.reader_topology_changes += 1;
        }
        self.record_foreground_write(0)?;

        for slot in 0..self.slots.len() {
            self.prime_slot(slot)?;
        }
        for (index, failpoint) in StableFailpoint::ALL.into_iter().enumerate() {
            let slot = index % self.slots.len();
            self.record_fault(
                slot,
                FaultAction::Failpoint { failpoint },
                TypedError::Retryable {
                    code: "injected_stable_failpoint".to_string(),
                },
            )?;
            self.coverage.failpoints += 1;
            self.restart(slot)?;
        }
        for _ in 0..self.config.random_steps.get() {
            let slot = self.rng.index(self.slots.len());
            self.random_step(slot)?;
        }
        for slot in 0..self.slots.len() {
            self.drain_slot(slot)?;
        }
        for tenant_index in 0..self.config.tenant_count.get() {
            self.record_runtime(
                RuntimeId::new(tenant_index + 2)?,
                TenantId::try_new(format!("tenant-{tenant_index}"))?,
                RuntimeAction::RemoveReader {
                    runtime: RuntimeId::new(tenant_index + 2)?,
                },
            )?;
            self.coverage.reader_topology_changes += 1;
        }
        self.record_runtime(
            RuntimeId::new(1)?,
            TenantId::try_new("control")?,
            RuntimeAction::CloseWriter,
        )?;
        self.validate_invariants()?;
        if !self.resources.is_empty() || !self.text_blob_references.is_empty() {
            return Err(TestkitError::ModelViolation(
                "lifecycle drain retained physical generations or blobs".to_string(),
            ));
        }
        let trace = self.recorder.finish()?;
        Ok(LifecycleRunReport {
            trace,
            coverage: self.coverage,
            max_live_generations: self.max_live_generations,
        })
    }

    fn prime_slot(&mut self, slot: usize) -> Result<()> {
        self.create(slot)?;
        self.record_foreground_write(slot as i64 + 1)?;
        self.build(slot, 0)?;
        self.build(slot, 0)?;
        if slot.is_multiple_of(2) {
            self.block(slot, IndexBlocker::ResourceLimit)?;
            self.retry(slot)?;
        }
        self.activate(slot)?;
        self.read_active(slot)?;
        let SlotState::Active(active) = &self.slots[slot].state else {
            unreachable!("activation publishes an active state")
        };
        if active.generation().definition().family() == IndexFamily::Text {
            self.fail_closed(slot)?;
        }
        self.drop_active(slot)?;
        self.reclaim(slot)?;
        self.recreate(slot)?;
        if slot.is_multiple_of(3) {
            self.abort_build(slot)?;
        } else {
            self.build(slot, 1)?;
            self.activate(slot)?;
        }
        self.validate_invariants()
    }

    fn random_step(&mut self, slot: usize) -> Result<()> {
        match self.slots[slot].state.clone() {
            SlotState::Absent(_) => self.create(slot)?,
            SlotState::Building(_) => match self.rng.next_u64() % 7 {
                0 => {
                    let chunk = (self.rng.next_u64() % 4) as u8;
                    self.build(slot, chunk)?;
                }
                1 => self.activate(slot)?,
                2 => self.block(slot, IndexBlocker::ResourceLimit)?,
                3 => self.abort_build(slot)?,
                4 => self.record_foreground_write(self.next_entity as i64)?,
                5 => self.failpoint(slot)?,
                _ => self.restart(slot)?,
            },
            SlotState::Blocked(_) => match self.rng.next_u64() % 3 {
                0 => self.retry(slot)?,
                1 => self.abort_blocked(slot)?,
                _ => self.restart(slot)?,
            },
            SlotState::Active(active) => match self.rng.next_u64() % 7 {
                0 | 1 => self.read_active(slot)?,
                2 => self.record_foreground_write(self.next_entity as i64)?,
                3 => self.drop_active(slot)?,
                4 if active.generation().definition().family() == IndexFamily::Text => {
                    self.fail_closed(slot)?
                }
                4 => self.restart(slot)?,
                5 => self.failpoint(slot)?,
                _ => self.restart(slot)?,
            },
            SlotState::Retired(_) => match self.rng.next_u64() % 4 {
                0 => self.recreate(slot)?,
                1 => self.reclaim(slot)?,
                2 => self.restart(slot)?,
                _ => self.record_background(slot, BackgroundAction::Reconcile)?,
            },
        }
        self.validate_invariants()
    }

    fn drain_slot(&mut self, slot: usize) -> Result<()> {
        match self.slots[slot].state.clone() {
            SlotState::Absent(_) => return Ok(()),
            SlotState::Building(_) => self.abort_build(slot)?,
            SlotState::Blocked(_) => self.abort_blocked(slot)?,
            SlotState::Active(_) => self.drop_active(slot)?,
            SlotState::Retired(_) => {}
        }
        self.reclaim(slot)
    }

    fn create(&mut self, slot: usize) -> Result<()> {
        let SlotState::Absent(absent) = self.slots[slot].state.clone() else {
            return Err(TestkitError::ModelViolation(
                "create requires an absent slot".to_string(),
            ));
        };
        let transition = absent.create()?;
        let (action, next) = transition.into_parts();
        self.record_index(
            slot,
            action,
            TraceOutcome::Success(ObservedValue::Acknowledged),
        )?;
        self.slots[slot].state = SlotState::Building(next);
        Ok(())
    }

    fn recreate(&mut self, slot: usize) -> Result<()> {
        let SlotState::Retired(retired) = self.slots[slot].state.clone() else {
            return Err(TestkitError::ModelViolation(
                "recreate requires a retired slot".to_string(),
            ));
        };
        self.reclaim(slot)?;
        let transition = retired.recreate()?;
        let (action, next) = transition.into_parts();
        self.record_index(
            slot,
            action,
            TraceOutcome::Success(ObservedValue::Acknowledged),
        )?;
        self.slots[slot].state = SlotState::Building(next);
        Ok(())
    }

    fn build(&mut self, slot: usize, chunk: u8) -> Result<()> {
        let SlotState::Building(building) = self.slots[slot].state.clone() else {
            return Err(TestkitError::ModelViolation(
                "build requires a building slot".to_string(),
            ));
        };
        let action = building.build();
        let key = generation_key(&action);
        if !self.completed_work.insert((key, chunk)) {
            self.coverage.duplicate_wakeups += 1;
        }
        self.record_index(
            slot,
            action,
            TraceOutcome::Success(ObservedValue::Acknowledged),
        )
    }

    fn block(&mut self, slot: usize, blocker: IndexBlocker) -> Result<()> {
        let SlotState::Building(building) = self.slots[slot].state.clone() else {
            return Err(TestkitError::ModelViolation(
                "block requires a building slot".to_string(),
            ));
        };
        let action = building.build();
        self.record_index(
            slot,
            action,
            TraceOutcome::Error(TypedError::IndexBlocked {
                blocker,
                code: "index_build_blocked".to_string(),
            }),
        )?;
        self.slots[slot].state = SlotState::Blocked(building.blocked(blocker));
        self.coverage.blocked_builds += 1;
        Ok(())
    }

    fn retry(&mut self, slot: usize) -> Result<()> {
        let SlotState::Blocked(blocked) = self.slots[slot].state.clone() else {
            return Err(TestkitError::ModelViolation(
                "retry requires a blocked slot".to_string(),
            ));
        };
        let (action, next) = blocked.retry().into_parts();
        self.record_index(
            slot,
            action,
            TraceOutcome::Success(ObservedValue::Acknowledged),
        )?;
        self.slots[slot].state = SlotState::Building(next);
        self.coverage.retries += 1;
        Ok(())
    }

    fn activate(&mut self, slot: usize) -> Result<()> {
        let SlotState::Building(building) = self.slots[slot].state.clone() else {
            return Err(TestkitError::ModelViolation(
                "activate requires a building slot".to_string(),
            ));
        };
        let (action, next) = building.activate().into_parts();
        self.record_index(
            slot,
            action,
            TraceOutcome::Success(ObservedValue::Acknowledged),
        )?;
        self.slots[slot].state = SlotState::Active(next);
        Ok(())
    }

    fn abort_build(&mut self, slot: usize) -> Result<()> {
        let SlotState::Building(building) = self.slots[slot].state.clone() else {
            return Err(TestkitError::ModelViolation(
                "abort requires a building slot".to_string(),
            ));
        };
        let (action, next) = building.abort().into_parts();
        self.record_index(
            slot,
            action,
            TraceOutcome::Success(ObservedValue::Acknowledged),
        )?;
        self.slots[slot].state = SlotState::Retired(next);
        self.coverage.aborts += 1;
        Ok(())
    }

    fn abort_blocked(&mut self, slot: usize) -> Result<()> {
        let SlotState::Blocked(blocked) = self.slots[slot].state.clone() else {
            return Err(TestkitError::ModelViolation(
                "abort requires a blocked slot".to_string(),
            ));
        };
        let (action, next) = blocked.abort().into_parts();
        self.record_index(
            slot,
            action,
            TraceOutcome::Success(ObservedValue::Acknowledged),
        )?;
        self.slots[slot].state = SlotState::Retired(next);
        self.coverage.aborts += 1;
        Ok(())
    }

    fn drop_active(&mut self, slot: usize) -> Result<()> {
        let SlotState::Active(active) = self.slots[slot].state.clone() else {
            return Err(TestkitError::ModelViolation(
                "drop requires an active slot".to_string(),
            ));
        };
        let (action, next) = active.drop_index().into_parts();
        self.record_index(
            slot,
            action,
            TraceOutcome::Success(ObservedValue::Acknowledged),
        )?;
        self.slots[slot].state = SlotState::Retired(next);
        Ok(())
    }

    fn reclaim(&mut self, slot: usize) -> Result<()> {
        let SlotState::Retired(retired) = self.slots[slot].state.clone() else {
            return Ok(());
        };
        let key = GenerationKey {
            name: retired.generation().definition().name().clone(),
            generation: retired.generation().generation(),
        };
        self.record_background(
            slot,
            BackgroundAction::Reclaim {
                generation: retired.generation().clone(),
            },
        )?;
        let Some(resource) = self.resources.remove(&key) else {
            self.coverage.duplicate_wakeups += 1;
            return Ok(());
        };
        if resource.visibility != PhysicalVisibility::Retired {
            return Err(TestkitError::ModelViolation(
                "only retired physical generations may be reclaimed".to_string(),
            ));
        }
        if let Some(blob) = resource.text_blob {
            let Some(references) = self.text_blob_references.get_mut(blob) else {
                return Err(TestkitError::ModelViolation(
                    "text generation lost its shared blob reference".to_string(),
                ));
            };
            *references -= 1;
            if *references == 0 {
                self.text_blob_references.remove(blob);
            }
        }
        self.coverage.reclaims += 1;
        Ok(())
    }

    fn read_active(&mut self, slot: usize) -> Result<()> {
        let SlotState::Active(active) = self.slots[slot].state.clone() else {
            return Err(TestkitError::ModelViolation(
                "indexed read requires an active slot".to_string(),
            ));
        };
        let generation = active.generation();
        let action = match generation.definition() {
            IndexDefinition::Secondary { name, .. } => ReadAction::Secondary {
                index: name.clone(),
                value: PropertyValue::I64(0),
            },
            IndexDefinition::Text { name, .. } => ReadAction::Text {
                index: name.clone(),
                query: crate::action::TextQuery::try_new("shared")?,
                limit: NonZeroU32::new(8).expect("text limit is positive"),
            },
            IndexDefinition::Vector { name, .. } => ReadAction::Vector {
                index: name.clone(),
                vector: VectorValue::try_new(vec![
                    FiniteF32::try_new(1.0)?,
                    FiniteF32::try_new(0.0)?,
                ])?,
                limit: NonZeroU32::new(8).expect("vector limit is positive"),
                metric: VectorMetric::Euclidean,
            },
        };
        let request = self.request_id()?;
        let pending = self
            .recorder
            .start_request(
                request,
                self.slots[slot].runtime,
                self.slots[slot].tenant.clone(),
                self.oracle.sequence(),
                Action::Read(action.clone()),
            )?
            .with_generation(generation.generation());
        let result = self.oracle.read_at(self.oracle.sequence(), &action)?;
        self.recorder
            .finish_request(pending, None, TraceOutcome::Success(result.into()));
        self.coverage.indexed_reads += 1;
        Ok(())
    }

    fn record_foreground_write(&mut self, rank: i64) -> Result<()> {
        let request = self.request_id()?;
        let action = WriteAction::InsertNode {
            id: EntityId::new(self.next_entity),
            label: LabelName::try_new("Document")?,
            properties: BTreeMap::from([
                (PropertyName::try_new("rank")?, PropertyValue::I64(rank)),
                (
                    PropertyName::try_new("text")?,
                    PropertyValue::String("shared lifecycle text".to_string()),
                ),
                (
                    PropertyName::try_new("vector")?,
                    PropertyValue::Vector(VectorValue::try_new(vec![
                        FiniteF32::try_new(1.0)?,
                        FiniteF32::try_new(0.0)?,
                    ])?),
                ),
            ]),
        };
        self.next_entity += 1;
        let pending = self.recorder.start_request(
            request,
            RuntimeId::new(1)?,
            TenantId::try_new("control")?,
            self.oracle.sequence(),
            Action::Write(action.clone()),
        )?;
        self.oracle.apply_write(&action)?;
        let commit = self.oracle.sequence();
        self.foreground_commits.push(commit);
        for resource in self.resources.values_mut() {
            if matches!(
                resource.visibility,
                PhysicalVisibility::Pending | PhysicalVisibility::Active
            ) {
                resource.foreground_commits.insert(commit);
            }
        }
        self.recorder.finish_request(
            pending,
            Some(commit),
            TraceOutcome::Success(ObservedValue::Acknowledged),
        );
        self.coverage.foreground_commits += 1;
        Ok(())
    }

    fn record_index(
        &mut self,
        slot: usize,
        action: IndexAction,
        outcome: TraceOutcome,
    ) -> Result<()> {
        let request = self.request_id()?;
        let pending = self.recorder.start_request(
            request,
            self.slots[slot].runtime,
            self.slots[slot].tenant.clone(),
            self.oracle.sequence(),
            Action::Index(action.clone()),
        )?;
        let commit = match &outcome {
            TraceOutcome::Success(_) => {
                self.oracle.apply_index(&action)?;
                self.apply_resource_transition(&action, self.oracle.sequence())?;
                self.coverage.lifecycle_commits += 1;
                Some(self.oracle.sequence())
            }
            TraceOutcome::Error(TypedError::IndexBlocked { blocker, .. }) => {
                self.oracle
                    .lifecycle_mut()
                    .mark_blocked(action.generation(), *blocker)?;
                None
            }
            TraceOutcome::Error(_) => None,
        };
        self.recorder.finish_request(pending, commit, outcome);
        self.max_live_generations = self.max_live_generations.max(self.resources.len());
        Ok(())
    }

    fn apply_resource_transition(&mut self, action: &IndexAction, commit: Sequence) -> Result<()> {
        let key = generation_key(action);
        match action.kind() {
            IndexActionKind::Create | IndexActionKind::Recreate => {
                let text_blob = (action.generation().definition().family() == IndexFamily::Text)
                    .then_some("shared-content-addressed-blob");
                if self
                    .resources
                    .insert(
                        key,
                        PhysicalGenerationModel {
                            visibility: PhysicalVisibility::Pending,
                            created_at: commit,
                            retired_at: None,
                            foreground_commits: BTreeSet::new(),
                            text_blob,
                        },
                    )
                    .is_some()
                {
                    return Err(TestkitError::ModelViolation(
                        "physical generation identity was reused".to_string(),
                    ));
                }
                if let Some(blob) = text_blob {
                    *self.text_blob_references.entry(blob).or_default() += 1;
                }
            }
            IndexActionKind::Build | IndexActionKind::Retry => {}
            IndexActionKind::Activate => {
                let resource = self.resources.get_mut(&key).ok_or_else(|| {
                    TestkitError::ModelViolation(
                        "activation has no pending physical generation".to_string(),
                    )
                })?;
                if resource.visibility != PhysicalVisibility::Pending {
                    return Err(TestkitError::ModelViolation(
                        "activation was not atomic from pending state".to_string(),
                    ));
                }
                resource.visibility = PhysicalVisibility::Active;
            }
            IndexActionKind::Drop | IndexActionKind::Abort => {
                let resource = self.resources.get_mut(&key).ok_or_else(|| {
                    TestkitError::ModelViolation(
                        "retirement has no physical generation".to_string(),
                    )
                })?;
                resource.visibility = PhysicalVisibility::Retired;
                resource.retired_at = Some(commit);
            }
        }
        Ok(())
    }

    fn failpoint(&mut self, slot: usize) -> Result<()> {
        let failpoint = StableFailpoint::ALL[self.rng.index(StableFailpoint::ALL.len())];
        self.record_fault(
            slot,
            FaultAction::Failpoint { failpoint },
            TypedError::Retryable {
                code: "injected_stable_failpoint".to_string(),
            },
        )?;
        self.coverage.failpoints += 1;
        Ok(())
    }

    fn restart(&mut self, slot: usize) -> Result<()> {
        self.record_fault(
            slot,
            FaultAction::ProcessRestart {
                runtime: self.slots[slot].runtime,
            },
            TypedError::Retryable {
                code: "process_restarted".to_string(),
            },
        )?;
        self.coverage.restarts += 1;
        Ok(())
    }

    fn fail_closed(&mut self, slot: usize) -> Result<()> {
        let before = self.oracle.lifecycle().clone();
        self.record_fault(
            slot,
            FaultAction::CorruptDurableInput {
                invariant: DurableInvariant::WorkRecord,
                bytes: vec![0xff],
            },
            TypedError::Corruption {
                code: "durable_input_corrupt".to_string(),
            },
        )?;
        if self.oracle.lifecycle() != &before {
            return Err(TestkitError::ModelViolation(
                "fail-closed faults changed recoverable lifecycle state".to_string(),
            ));
        }
        self.coverage.fail_closed_errors += 2;
        Ok(())
    }

    fn record_fault(&mut self, slot: usize, action: FaultAction, error: TypedError) -> Result<()> {
        let request = self.request_id()?;
        let pending = self.recorder.start_request(
            request,
            self.slots[slot].runtime,
            self.slots[slot].tenant.clone(),
            self.oracle.sequence(),
            Action::Fault(action),
        )?;
        self.recorder
            .finish_request(pending, None, TraceOutcome::Error(error));
        Ok(())
    }

    fn record_background(&mut self, slot: usize, action: BackgroundAction) -> Result<()> {
        let request = self.request_id()?;
        let pending = self.recorder.start_request(
            request,
            self.slots[slot].runtime,
            self.slots[slot].tenant.clone(),
            self.oracle.sequence(),
            Action::Background(action),
        )?;
        self.recorder.finish_request(
            pending,
            None,
            TraceOutcome::Success(ObservedValue::Acknowledged),
        );
        Ok(())
    }

    fn record_runtime(
        &mut self,
        runtime: RuntimeId,
        tenant: TenantId,
        action: RuntimeAction,
    ) -> Result<()> {
        let request = self.request_id()?;
        let pending = self.recorder.start_request(
            request,
            runtime,
            tenant,
            self.oracle.sequence(),
            Action::Runtime(action),
        )?;
        self.recorder.finish_request(
            pending,
            None,
            TraceOutcome::Success(ObservedValue::Acknowledged),
        );
        Ok(())
    }

    fn validate_invariants(&self) -> Result<()> {
        for slot in &self.slots {
            let (generation, expected_state, expected_visibility) = match &slot.state {
                SlotState::Absent(_) => continue,
                SlotState::Building(building) => (
                    building.generation(),
                    "building",
                    Some(PhysicalVisibility::Pending),
                ),
                SlotState::Blocked(blocked) => {
                    let ModelIndexState::Blocked { generation, .. } = self
                        .oracle
                        .lifecycle()
                        .state(blocked.generation().definition().name())
                        .ok_or_else(|| {
                            TestkitError::ModelViolation(
                                "blocked wrapper has no lifecycle state".to_string(),
                            )
                        })?
                    else {
                        return Err(TestkitError::ModelViolation(
                            "blocked wrapper disagrees with lifecycle state".to_string(),
                        ));
                    };
                    (generation, "blocked", Some(PhysicalVisibility::Pending))
                }
                SlotState::Active(active) => (
                    active.generation(),
                    "active",
                    Some(PhysicalVisibility::Active),
                ),
                SlotState::Retired(retired) => (retired.generation(), "retired", None),
            };
            let name = generation.definition().name();
            let model_state = self.oracle.lifecycle().state(name).ok_or_else(|| {
                TestkitError::ModelViolation(format!(
                    "{expected_state} wrapper has no lifecycle model state"
                ))
            })?;
            let state_matches = matches!(
                (expected_state, model_state),
                ("building", ModelIndexState::Building(_))
                    | ("blocked", ModelIndexState::Blocked { .. })
                    | ("active", ModelIndexState::Active(_))
                    | ("retired", ModelIndexState::Retired(_))
            );
            if !state_matches {
                return Err(TestkitError::ModelViolation(format!(
                    "{expected_state} wrapper disagrees with lifecycle model"
                )));
            }
            if expected_state == "active"
                && self.oracle.lifecycle().active(name) != Some(generation)
            {
                return Err(TestkitError::ModelViolation(
                    "active generation is not atomically public".to_string(),
                ));
            }
            if expected_state != "active" && self.oracle.lifecycle().active(name).is_some() {
                return Err(TestkitError::ModelViolation(
                    "pending or retired generation became publicly searchable".to_string(),
                ));
            }
            let key = GenerationKey {
                name: name.clone(),
                generation: generation.generation(),
            };
            if let Some(expected_visibility) = expected_visibility {
                let resource = self.resources.get(&key).ok_or_else(|| {
                    TestkitError::ModelViolation(
                        "live lifecycle state lost its physical generation".to_string(),
                    )
                })?;
                if resource.visibility != expected_visibility {
                    return Err(TestkitError::ModelViolation(
                        "physical visibility disagrees with lifecycle state".to_string(),
                    ));
                }
            }
        }
        for resource in self.resources.values() {
            let expected = self
                .foreground_commits
                .iter()
                .copied()
                .filter(|commit| {
                    *commit > resource.created_at
                        && resource
                            .retired_at
                            .is_none_or(|retired_at| *commit < retired_at)
                })
                .collect::<BTreeSet<_>>();
            if resource.foreground_commits != expected {
                return Err(TestkitError::ModelViolation(
                    "foreground mutation did not reach every required generation".to_string(),
                ));
            }
        }
        let mut expected_blobs = BTreeMap::new();
        for blob in self
            .resources
            .values()
            .filter_map(|resource| resource.text_blob)
        {
            *expected_blobs.entry(blob).or_default() += 1;
        }
        if expected_blobs != self.text_blob_references {
            return Err(TestkitError::ModelViolation(
                "shared blob references disagree with live generations".to_string(),
            ));
        }
        Ok(())
    }

    fn request_id(&mut self) -> Result<RequestId> {
        let request = RequestId::new(self.next_request)?;
        self.next_request = self.next_request.checked_add(1).ok_or_else(|| {
            TestkitError::ModelViolation("request identity exhausted".to_string())
        })?;
        Ok(request)
    }
}

fn generation_key(action: &IndexAction) -> GenerationKey {
    GenerationKey {
        name: action.generation().definition().name().clone(),
        generation: action.generation().generation(),
    }
}

#[derive(Debug, Clone, Copy)]
struct StableRng(u64);

impl StableRng {
    fn new(seed: StableSeed) -> Self {
        Self(seed.get().max(1))
    }

    fn next_u64(&mut self) -> u64 {
        let mut value = self.0;
        value ^= value << 13;
        value ^= value >> 7;
        value ^= value << 17;
        self.0 = value;
        value
    }

    fn index(&mut self, len: usize) -> usize {
        assert!(len > 0, "random choice domain must be non-empty");
        (self.next_u64() % len as u64) as usize
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn config_rejects_every_zero_dimension() {
        assert!(LifecycleRunConfig::try_new(0, 1, 1).is_err());
        assert!(LifecycleRunConfig::try_new(1, 0, 1).is_err());
        assert!(LifecycleRunConfig::try_new(1, 1, 0).is_err());
    }

    #[test]
    fn pull_request_seeds_cover_lifecycle_fault_retry_cleanup_and_replay() {
        for seed in [1_u64, 7, 0x5eed, u32::MAX as u64] {
            let report = LifecycleWorkload
                .run(StableSeed::new(seed), LifecycleRunConfig::pull_request())
                .unwrap_or_else(|error| panic!("lifecycle seed {seed} failed: {error}"));
            let coverage = report.coverage();
            assert!(coverage.lifecycle_commits > 0);
            assert!(coverage.foreground_commits > 0);
            assert!(coverage.indexed_reads > 0);
            assert!(coverage.blocked_builds > 0);
            assert!(coverage.retries > 0);
            assert!(coverage.aborts > 0);
            assert!(coverage.restarts > 0);
            assert!(coverage.failpoints > 0);
            assert!(coverage.reader_topology_changes >= 8);
            assert!(coverage.reclaims > 0);
            assert!(coverage.fail_closed_errors > 0);
            assert!(report.max_live_generations() > 1);
            let json = report.trace().to_json().unwrap();
            assert_eq!(ReplayTrace::from_json(&json).unwrap(), *report.trace());
        }
    }

    #[test]
    fn duplicate_build_wakeups_are_idempotent() {
        let report = LifecycleWorkload
            .run(
                StableSeed::new(3),
                LifecycleRunConfig::try_new(1024, 2, 1).unwrap(),
            )
            .unwrap();
        assert!(report.coverage().duplicate_wakeups > 0);
    }

    #[test]
    #[ignore = "nightly deterministic lifecycle seed matrix"]
    fn nightly_seed_matrix_runs_long_multi_index_histories() {
        for seed in 0_u64..32 {
            LifecycleWorkload
                .run(StableSeed::new(seed), LifecycleRunConfig::nightly())
                .unwrap_or_else(|error| panic!("nightly lifecycle seed {seed} failed: {error}"));
        }
    }

    #[test]
    #[ignore = "pre-launch complete deterministic lifecycle seed matrix"]
    fn pre_launch_seed_matrix_runs_complete_long_histories() {
        for seed in 0_u64..128 {
            LifecycleWorkload
                .run(StableSeed::new(seed), LifecycleRunConfig::pre_launch())
                .unwrap_or_else(|error| panic!("pre-launch lifecycle seed {seed} failed: {error}"));
        }
    }
}
