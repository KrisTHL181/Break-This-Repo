//! Independent graph, search, MVCC, lifecycle, and resource oracles.

use std::cmp::Ordering;
use std::collections::{BTreeMap, BTreeSet, VecDeque};

use serde::{Deserialize, Serialize};

use crate::action::{
    AggregateKind, ElementKind, EntityRef, PropertyMutation, PropertyValue, ReadAction,
    SecondaryRangeBound, SecondaryRangeDirection, SecondaryRangeValue, TraversalDirection,
    VectorMetric, WriteAction,
};
use crate::ids::{
    EntityId, FiniteF32, GenerationId, IndexName, LabelName, PropertyName, RequestId, Sequence,
};
use crate::lifecycle::{
    IndexAction, IndexActionKind, IndexBlocker, IndexDefinition, IndexFamily, IndexGeneration,
};
use crate::{Result, TestkitError};

/// Independent node record.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct ModelNode {
    label: LabelName,
    properties: BTreeMap<PropertyName, PropertyValue>,
}

impl ModelNode {
    /// Borrows the node label.
    pub fn label(&self) -> &LabelName {
        &self.label
    }

    /// Borrows node properties.
    pub fn properties(&self) -> &BTreeMap<PropertyName, PropertyValue> {
        &self.properties
    }
}

/// Independent edge record.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct ModelEdge {
    label: LabelName,
    from: EntityId,
    to: EntityId,
    properties: BTreeMap<PropertyName, PropertyValue>,
}

impl ModelEdge {
    /// Borrows the edge label.
    pub fn label(&self) -> &LabelName {
        &self.label
    }

    /// Returns the source node.
    pub const fn from(&self) -> EntityId {
        self.from
    }

    /// Returns the destination node.
    pub const fn to(&self) -> EntityId {
        self.to
    }

    /// Borrows edge properties.
    pub fn properties(&self) -> &BTreeMap<PropertyName, PropertyValue> {
        &self.properties
    }
}

/// One projected model row.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct ProjectionRow {
    /// Projected element.
    pub entity: EntityRef,
    /// Present selected properties in request order-independent form.
    pub values: BTreeMap<PropertyName, PropertyValue>,
}

/// One independently scored search result.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct ScoredEntity {
    /// Matching graph element.
    pub entity: EntityRef,
    /// Finite distance where smaller is better.
    pub distance: FiniteF32,
}

/// Publicly visible catalog projection in the model.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct IndexCatalogView {
    /// Active definition.
    pub definition: IndexDefinition,
    /// Active physical generation.
    pub generation: GenerationId,
}

/// Result shapes returned by the independent read oracle.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(tag = "kind", content = "value", rename_all = "snake_case")]
pub enum ModelReadResult {
    /// Ordered graph elements.
    Entities(Vec<EntityRef>),
    /// Ordered projected rows.
    Projection(Vec<ProjectionRow>),
    /// Count aggregate.
    Count(usize),
    /// Boolean aggregate.
    Bool(bool),
    /// Ordered scored search results.
    Scored(Vec<ScoredEntity>),
    /// Active catalog entry, if any.
    Catalog(Option<IndexCatalogView>),
    /// Active generation, if any.
    Generation(Option<GenerationId>),
}

/// Outcome of an independent write.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum ModelWriteResult {
    /// The mutation committed atomically.
    Applied,
    /// Explicit contenders require a serializable winner or typed conflict.
    Conflict,
}

/// Canonical in-memory graph model independent of DB storage and codecs.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub struct GraphModel {
    nodes: BTreeMap<EntityId, ModelNode>,
    edges: BTreeMap<EntityId, ModelEdge>,
}

impl GraphModel {
    /// Borrows all nodes in identity order.
    pub fn nodes(&self) -> &BTreeMap<EntityId, ModelNode> {
        &self.nodes
    }

    /// Borrows all edges in identity order.
    pub fn edges(&self) -> &BTreeMap<EntityId, ModelEdge> {
        &self.edges
    }

    /// Executes one graph-only read against the current model state.
    pub fn read(&self, action: &ReadAction) -> Result<ModelReadResult> {
        let Some(result) = self.read_graph(action)? else {
            return Err(TestkitError::ModelViolation(
                "indexed read requires the composite oracle state".to_string(),
            ));
        };
        Ok(result)
    }

    /// Applies one write atomically.
    pub fn apply(&mut self, action: &WriteAction) -> Result<ModelWriteResult> {
        if matches!(action, WriteAction::Conflicting { .. }) {
            return Ok(ModelWriteResult::Conflict);
        }
        let mut staged = self.clone();
        staged.apply_inner(action)?;
        *self = staged;
        Ok(ModelWriteResult::Applied)
    }

    /// Applies a transaction atomically or leaves the graph unchanged.
    pub fn apply_transaction(&mut self, actions: &[WriteAction]) -> Result<ModelWriteResult> {
        let mut staged = self.clone();
        for action in actions {
            if staged.apply(action)? == ModelWriteResult::Conflict {
                return Ok(ModelWriteResult::Conflict);
            }
        }
        *self = staged;
        Ok(ModelWriteResult::Applied)
    }

    fn apply_inner(&mut self, action: &WriteAction) -> Result<()> {
        match action {
            WriteAction::InsertNode {
                id,
                label,
                properties,
            } => {
                if self.nodes.contains_key(id) {
                    return Err(TestkitError::ModelViolation(format!(
                        "duplicate node {}",
                        id.get()
                    )));
                }
                self.nodes.insert(
                    *id,
                    ModelNode {
                        label: label.clone(),
                        properties: properties.clone(),
                    },
                );
            }
            WriteAction::InsertEdge {
                id,
                label,
                from,
                to,
                properties,
            } => {
                if self.edges.contains_key(id) {
                    return Err(TestkitError::ModelViolation(format!(
                        "duplicate edge {}",
                        id.get()
                    )));
                }
                if !self.nodes.contains_key(from) || !self.nodes.contains_key(to) {
                    return Err(TestkitError::ModelViolation(
                        "edge endpoint does not exist".to_string(),
                    ));
                }
                self.edges.insert(
                    *id,
                    ModelEdge {
                        label: label.clone(),
                        from: *from,
                        to: *to,
                        properties: properties.clone(),
                    },
                );
            }
            WriteAction::Update { target, patch } => {
                let properties = self.properties_mut(*target)?;
                for (name, mutation) in patch.iter() {
                    match mutation {
                        PropertyMutation::Set(value) => {
                            properties.insert(name.clone(), value.clone());
                        }
                        PropertyMutation::Remove => {
                            properties.remove(name);
                        }
                    }
                }
            }
            WriteAction::Delete { target } => match target {
                EntityRef::Node(id) => {
                    if self.nodes.remove(id).is_none() {
                        return Err(TestkitError::ModelViolation(format!(
                            "missing node {}",
                            id.get()
                        )));
                    }
                    self.edges
                        .retain(|_, edge| edge.from != *id && edge.to != *id);
                }
                EntityRef::Edge(id) => {
                    if self.edges.remove(id).is_none() {
                        return Err(TestkitError::ModelViolation(format!(
                            "missing edge {}",
                            id.get()
                        )));
                    }
                }
            },
            WriteAction::Conflicting { .. } => {
                return Err(TestkitError::ModelViolation(
                    "conflicting writes must be resolved by the history checker".to_string(),
                ));
            }
        }
        Ok(())
    }

    fn properties_mut(
        &mut self,
        target: EntityRef,
    ) -> Result<&mut BTreeMap<PropertyName, PropertyValue>> {
        match target {
            EntityRef::Node(id) => self.nodes.get_mut(&id).map(|node| &mut node.properties),
            EntityRef::Edge(id) => self.edges.get_mut(&id).map(|edge| &mut edge.properties),
        }
        .ok_or_else(|| {
            TestkitError::ModelViolation(format!(
                "missing {} {}",
                match target.kind() {
                    ElementKind::Node => "node",
                    ElementKind::Edge => "edge",
                },
                target.id().get()
            ))
        })
    }

    fn properties(&self, target: EntityRef) -> Option<&BTreeMap<PropertyName, PropertyValue>> {
        match target {
            EntityRef::Node(id) => self.nodes.get(&id).map(ModelNode::properties),
            EntityRef::Edge(id) => self.edges.get(&id).map(ModelEdge::properties),
        }
    }

    fn contains(&self, target: EntityRef) -> bool {
        match target {
            EntityRef::Node(id) => self.nodes.contains_key(&id),
            EntityRef::Edge(id) => self.edges.contains_key(&id),
        }
    }

    fn entities(&self, kind: ElementKind) -> Vec<EntityRef> {
        match kind {
            ElementKind::Node => self.nodes.keys().copied().map(EntityRef::Node).collect(),
            ElementKind::Edge => self.edges.keys().copied().map(EntityRef::Edge).collect(),
        }
    }

    fn read_graph(&self, action: &ReadAction) -> Result<Option<ModelReadResult>> {
        let result = match action {
            ReadAction::Point { kind, id } => {
                let entity = match kind {
                    ElementKind::Node => EntityRef::Node(*id),
                    ElementKind::Edge => EntityRef::Edge(*id),
                };
                ModelReadResult::Entities(
                    self.contains(entity)
                        .then_some(entity)
                        .into_iter()
                        .collect(),
                )
            }
            ReadAction::Range { kind, range } => ModelReadResult::Entities(
                self.entities(*kind)
                    .into_iter()
                    .filter(|entity| entity.id() >= range.start() && entity.id() <= range.end())
                    .collect(),
            ),
            ReadAction::Traversal {
                start,
                direction,
                max_depth,
            } => ModelReadResult::Entities(self.traverse(*start, *direction, max_depth.get())?),
            ReadAction::Projection { targets, fields } => {
                let rows = targets
                    .as_slice()
                    .iter()
                    .filter_map(|target| {
                        let properties = self.properties(*target)?;
                        let values = fields
                            .as_slice()
                            .iter()
                            .filter_map(|field| {
                                properties
                                    .get(field)
                                    .cloned()
                                    .map(|value| (field.clone(), value))
                            })
                            .collect();
                        Some(ProjectionRow {
                            entity: *target,
                            values,
                        })
                    })
                    .collect();
                ModelReadResult::Projection(rows)
            }
            ReadAction::Aggregate { kind, aggregate } => {
                let count = self.entities(*kind).len();
                match aggregate {
                    AggregateKind::Count => ModelReadResult::Count(count),
                    AggregateKind::Exists => ModelReadResult::Bool(count != 0),
                }
            }
            ReadAction::Secondary { .. }
            | ReadAction::SecondaryRange { .. }
            | ReadAction::Text { .. }
            | ReadAction::Vector { .. }
            | ReadAction::Catalog { .. }
            | ReadAction::Generation { .. } => return Ok(None),
        };
        Ok(Some(result))
    }

    fn traverse(
        &self,
        start: EntityId,
        direction: TraversalDirection,
        max_depth: u16,
    ) -> Result<Vec<EntityRef>> {
        if !self.nodes.contains_key(&start) {
            return Err(TestkitError::ModelViolation(format!(
                "missing traversal start node {}",
                start.get()
            )));
        }
        let mut visited = BTreeSet::from([start]);
        let mut output = Vec::new();
        let mut queue = VecDeque::from([(start, 0_u16)]);
        while let Some((node, depth)) = queue.pop_front() {
            if depth == max_depth {
                continue;
            }
            for edge in self.edges.values() {
                let neighbor = match direction {
                    TraversalDirection::Outgoing if edge.from == node => Some(edge.to),
                    TraversalDirection::Incoming if edge.to == node => Some(edge.from),
                    TraversalDirection::Both if edge.from == node => Some(edge.to),
                    TraversalDirection::Both if edge.to == node => Some(edge.from),
                    TraversalDirection::Outgoing
                    | TraversalDirection::Incoming
                    | TraversalDirection::Both => None,
                };
                let Some(neighbor) = neighbor else {
                    continue;
                };
                if visited.insert(neighbor) {
                    output.push(EntityRef::Node(neighbor));
                    queue.push_back((neighbor, depth + 1));
                }
            }
        }
        Ok(output)
    }
}

/// Lifecycle state visible to the independent oracle.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(tag = "state", content = "value", rename_all = "snake_case")]
pub enum ModelIndexState {
    /// Hidden generation under construction.
    Building(IndexGeneration),
    /// Public generation.
    Active(IndexGeneration),
    /// Hidden generation requiring control action.
    Blocked {
        /// Blocked generation.
        generation: IndexGeneration,
        /// Stable blocker.
        blocker: IndexBlocker,
    },
    /// Generation is no longer public and awaits or has completed cleanup.
    Retired(IndexGeneration),
}

/// Typed Index V2 visibility and generation oracle.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub struct LifecycleModel {
    states: BTreeMap<IndexName, ModelIndexState>,
}

impl LifecycleModel {
    /// Applies one state-machine action and rejects an illegal sequence.
    pub fn apply(&mut self, action: &IndexAction) -> Result<()> {
        let name = action.generation().definition().name().clone();
        let current = self.states.get(&name).cloned();
        let expected_generation = action.generation();
        let next = match (action.kind(), current) {
            (IndexActionKind::Create, None) => {
                ModelIndexState::Building(expected_generation.clone())
            }
            (IndexActionKind::Build, Some(ModelIndexState::Building(generation)))
                if generation == *expected_generation =>
            {
                ModelIndexState::Building(generation)
            }
            (IndexActionKind::Activate, Some(ModelIndexState::Building(generation)))
                if generation == *expected_generation =>
            {
                ModelIndexState::Active(generation)
            }
            (IndexActionKind::Drop, Some(ModelIndexState::Active(generation)))
                if generation == *expected_generation =>
            {
                ModelIndexState::Retired(generation)
            }
            (IndexActionKind::Recreate, Some(ModelIndexState::Retired(previous)))
                if previous.definition() == expected_generation.definition()
                    && previous.generation().checked_next()?
                        == expected_generation.generation() =>
            {
                ModelIndexState::Building(expected_generation.clone())
            }
            (IndexActionKind::Retry, Some(ModelIndexState::Blocked { generation, .. }))
                if generation == *expected_generation =>
            {
                ModelIndexState::Building(generation)
            }
            (IndexActionKind::Abort, Some(ModelIndexState::Building(generation)))
                if generation == *expected_generation =>
            {
                ModelIndexState::Retired(generation)
            }
            (IndexActionKind::Abort, Some(ModelIndexState::Blocked { generation, .. }))
                if generation == *expected_generation =>
            {
                ModelIndexState::Retired(generation)
            }
            (
                IndexActionKind::Create
                | IndexActionKind::Build
                | IndexActionKind::Activate
                | IndexActionKind::Drop
                | IndexActionKind::Recreate
                | IndexActionKind::Retry
                | IndexActionKind::Abort,
                _,
            ) => {
                return Err(TestkitError::ModelViolation(format!(
                    "illegal {:?} transition for index {} generation {}",
                    action.kind(),
                    name.as_str(),
                    expected_generation.generation().get()
                )));
            }
        };
        self.states.insert(name, next);
        Ok(())
    }

    /// Moves one building generation into a typed blocked state.
    pub fn mark_blocked(
        &mut self,
        generation: &IndexGeneration,
        blocker: IndexBlocker,
    ) -> Result<()> {
        let name = generation.definition().name();
        let Some(ModelIndexState::Building(current)) = self.states.get(name) else {
            return Err(TestkitError::ModelViolation(
                "only a building generation may become blocked".to_string(),
            ));
        };
        if current != generation {
            return Err(TestkitError::ModelViolation(
                "blocked generation does not match current build".to_string(),
            ));
        }
        self.states.insert(
            name.clone(),
            ModelIndexState::Blocked {
                generation: generation.clone(),
                blocker,
            },
        );
        Ok(())
    }

    /// Returns only a publicly active generation.
    pub fn active(&self, name: &IndexName) -> Option<&IndexGeneration> {
        let Some(ModelIndexState::Active(generation)) = self.states.get(name) else {
            return None;
        };
        Some(generation)
    }

    /// Borrows any retained lifecycle state.
    pub fn state(&self, name: &IndexName) -> Option<&ModelIndexState> {
        self.states.get(name)
    }
}

/// One immutable oracle snapshot.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct OracleSnapshot {
    graph: GraphModel,
    lifecycle: LifecycleModel,
}

/// Combined independent graph, search, lifecycle, and snapshot oracle.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct OracleState {
    sequence: Sequence,
    current: OracleSnapshot,
    snapshots: BTreeMap<Sequence, OracleSnapshot>,
}

impl Default for OracleState {
    fn default() -> Self {
        let current = OracleSnapshot {
            graph: GraphModel::default(),
            lifecycle: LifecycleModel::default(),
        };
        Self {
            sequence: Sequence::initial(),
            snapshots: BTreeMap::from([(Sequence::initial(), current.clone())]),
            current,
        }
    }
}

impl OracleState {
    /// Returns the latest committed sequence.
    pub const fn sequence(&self) -> Sequence {
        self.sequence
    }

    /// Borrows current graph state.
    pub fn graph(&self) -> &GraphModel {
        &self.current.graph
    }

    /// Borrows current lifecycle state.
    pub fn lifecycle(&self) -> &LifecycleModel {
        &self.current.lifecycle
    }

    /// Mutably borrows current lifecycle state for modeled worker outcomes.
    pub fn lifecycle_mut(&mut self) -> &mut LifecycleModel {
        &mut self.current.lifecycle
    }

    /// Applies one atomic graph write and records a committed snapshot.
    pub fn apply_write(&mut self, action: &WriteAction) -> Result<ModelWriteResult> {
        let mut next = self.current.clone();
        let result = next.graph.apply(action)?;
        if result == ModelWriteResult::Applied {
            self.commit_snapshot(next)?;
        }
        Ok(result)
    }

    /// Applies one transaction atomically and records exactly one committed snapshot.
    pub fn apply_transaction(&mut self, actions: &[WriteAction]) -> Result<ModelWriteResult> {
        let mut next = self.current.clone();
        let result = next.graph.apply_transaction(actions)?;
        if result == ModelWriteResult::Applied {
            self.commit_snapshot(next)?;
        }
        Ok(result)
    }

    /// Applies one lifecycle transition and records a committed snapshot.
    pub fn apply_index(&mut self, action: &IndexAction) -> Result<()> {
        let mut next = self.current.clone();
        next.lifecycle.apply(action)?;
        self.commit_snapshot(next)
    }

    fn commit_snapshot(&mut self, next: OracleSnapshot) -> Result<()> {
        self.sequence = self.sequence.checked_next()?;
        self.current = next.clone();
        let previous = self.snapshots.insert(self.sequence, next);
        assert!(previous.is_none(), "committed sequence must be unique");
        Ok(())
    }

    /// Executes a read against one exact committed snapshot.
    pub fn read_at(&self, snapshot: Sequence, action: &ReadAction) -> Result<ModelReadResult> {
        let Some(state) = self.snapshots.get(&snapshot) else {
            return Err(TestkitError::ModelViolation(format!(
                "unknown snapshot sequence {}",
                snapshot.get()
            )));
        };
        if let Some(result) = state.graph.read_graph(action)? {
            return Ok(result);
        }
        Self::read_indexed(state, action)
    }

    fn read_indexed(state: &OracleSnapshot, action: &ReadAction) -> Result<ModelReadResult> {
        let name = match action {
            ReadAction::Secondary { index, .. }
            | ReadAction::SecondaryRange { index, .. }
            | ReadAction::Text { index, .. }
            | ReadAction::Vector { index, .. }
            | ReadAction::Catalog { index }
            | ReadAction::Generation { index } => index,
            ReadAction::Point { .. }
            | ReadAction::Range { .. }
            | ReadAction::Traversal { .. }
            | ReadAction::Projection { .. }
            | ReadAction::Aggregate { .. } => {
                return Err(TestkitError::ModelViolation(
                    "graph read unexpectedly reached indexed oracle".to_string(),
                ));
            }
        };
        if matches!(action, ReadAction::Catalog { .. }) {
            return Ok(ModelReadResult::Catalog(state.lifecycle.active(name).map(
                |generation| IndexCatalogView {
                    definition: generation.definition().clone(),
                    generation: generation.generation(),
                },
            )));
        }
        if matches!(action, ReadAction::Generation { .. }) {
            return Ok(ModelReadResult::Generation(
                state
                    .lifecycle
                    .active(name)
                    .map(IndexGeneration::generation),
            ));
        }
        let Some(generation) = state.lifecycle.active(name) else {
            return Err(TestkitError::ModelViolation(format!(
                "index {} has no public generation",
                name.as_str()
            )));
        };
        match action {
            ReadAction::Secondary { value, .. } => {
                if generation.definition().family() != IndexFamily::Secondary {
                    return Err(TestkitError::ModelViolation(
                        "secondary lookup used a non-secondary index".to_string(),
                    ));
                }
                let entities = state
                    .graph
                    .entities(generation.definition().element())
                    .into_iter()
                    .filter(|entity| {
                        let Some(candidate) =
                            state.graph.properties(*entity).and_then(|properties| {
                                properties.get(generation.definition().property())
                            })
                        else {
                            return false;
                        };
                        secondary_values_equal(candidate, value)
                    })
                    .collect();
                Ok(ModelReadResult::Entities(entities))
            }
            ReadAction::SecondaryRange { range, .. } => {
                if generation.definition().family() != IndexFamily::Secondary {
                    return Err(TestkitError::ModelViolation(
                        "secondary range lookup used a non-secondary index".to_string(),
                    ));
                }
                let mut entities = state
                    .graph
                    .entities(generation.definition().element())
                    .into_iter()
                    .filter_map(|entity| {
                        let value = state
                            .graph
                            .properties(entity)?
                            .get(generation.definition().property())?;
                        let value = secondary_range_value(value)?;
                        secondary_range_contains(range.lower(), range.upper(), &value)
                            .then_some((value, entity))
                    })
                    .collect::<Vec<_>>();
                entities.sort_by(|(left_value, left_entity), (right_value, right_entity)| {
                    let value_order = secondary_range_total_compare(left_value, right_value);
                    let value_order = match range.direction() {
                        SecondaryRangeDirection::Ascending => value_order,
                        SecondaryRangeDirection::Descending => value_order.reverse(),
                    };
                    value_order.then_with(|| left_entity.cmp(right_entity))
                });
                if let Some(limit) = range.limit() {
                    entities.truncate(limit.get() as usize);
                }
                Ok(ModelReadResult::Entities(
                    entities.into_iter().map(|(_, entity)| entity).collect(),
                ))
            }
            ReadAction::Text { query, limit, .. } => {
                if generation.definition().family() != IndexFamily::Text {
                    return Err(TestkitError::ModelViolation(
                        "text lookup used a non-text index".to_string(),
                    ));
                }
                let query_terms = tokenize(query.as_str());
                let entities = state
                    .graph
                    .entities(generation.definition().element())
                    .into_iter()
                    .filter(|entity| {
                        let Some(PropertyValue::String(text)) =
                            state.graph.properties(*entity).and_then(|properties| {
                                properties.get(generation.definition().property())
                            })
                        else {
                            return false;
                        };
                        let terms = tokenize(text);
                        query_terms.iter().all(|term| terms.contains(term))
                    })
                    .take(limit.get() as usize)
                    .collect();
                Ok(ModelReadResult::Entities(entities))
            }
            ReadAction::Vector {
                vector,
                limit,
                metric,
                ..
            } => {
                let IndexDefinition::Vector {
                    dimension,
                    metric: definition_metric,
                    ..
                } = generation.definition()
                else {
                    return Err(TestkitError::ModelViolation(
                        "vector lookup used a non-vector index".to_string(),
                    ));
                };
                if *definition_metric != *metric || dimension.get() as usize != vector.dimension() {
                    return Err(TestkitError::ModelViolation(
                        "vector query does not match active definition".to_string(),
                    ));
                }
                let mut scored = Vec::new();
                for entity in state.graph.entities(generation.definition().element()) {
                    let Some(PropertyValue::Vector(candidate)) = state
                        .graph
                        .properties(entity)
                        .and_then(|properties| properties.get(generation.definition().property()))
                    else {
                        continue;
                    };
                    if candidate.dimension() != vector.dimension() {
                        return Err(TestkitError::ModelViolation(
                            "stored vector dimension does not match active definition".to_string(),
                        ));
                    }
                    let distance = vector_distance(*metric, vector, candidate)?;
                    scored.push(ScoredEntity { entity, distance });
                }
                scored.sort_by(|left, right| {
                    left.distance
                        .get()
                        .total_cmp(&right.distance.get())
                        .then_with(|| left.entity.cmp(&right.entity))
                });
                scored.truncate(limit.get() as usize);
                Ok(ModelReadResult::Scored(scored))
            }
            ReadAction::Point { .. }
            | ReadAction::Range { .. }
            | ReadAction::Traversal { .. }
            | ReadAction::Projection { .. }
            | ReadAction::Aggregate { .. }
            | ReadAction::Catalog { .. }
            | ReadAction::Generation { .. } => Err(TestkitError::ModelViolation(
                "unexpected indexed read dispatch".to_string(),
            )),
        }
    }
}

/// Compares two secondary-index values using the independent typed oracle.
///
/// Numeric variants use exact mathematical equality, signed zero is
/// normalized, and every NaN remains non-reflexive.
pub fn secondary_values_equal(left: &PropertyValue, right: &PropertyValue) -> bool {
    match (left, right) {
        (PropertyValue::Null, PropertyValue::Null) => true,
        (PropertyValue::Bool(left), PropertyValue::Bool(right)) => left == right,
        (
            PropertyValue::I64(_) | PropertyValue::F64(_) | PropertyValue::F32(_),
            PropertyValue::I64(_) | PropertyValue::F64(_) | PropertyValue::F32(_),
        ) => property_number(left)
            .zip(property_number(right))
            .is_some_and(|(left, right)| left == right),
        (PropertyValue::DateTime(left), PropertyValue::DateTime(right)) => left == right,
        (PropertyValue::String(left), PropertyValue::String(right)) => left == right,
        (PropertyValue::Bytes(left), PropertyValue::Bytes(right)) => left == right,
        (PropertyValue::I64Array(left), PropertyValue::I64Array(right)) => left == right,
        (PropertyValue::F64Array(left), PropertyValue::F64Array(right)) => {
            float64_arrays_equal(left, right)
        }
        (PropertyValue::F32Array(left), PropertyValue::F32Array(right)) => {
            float32_arrays_equal(left, right)
        }
        (PropertyValue::StringArray(left), PropertyValue::StringArray(right)) => left == right,
        (PropertyValue::Vector(left), PropertyValue::Vector(right)) => left == right,
        _ => false,
    }
}

fn secondary_range_value(value: &PropertyValue) -> Option<SecondaryRangeValue> {
    match value {
        PropertyValue::I64(value) => Some(SecondaryRangeValue::I64(*value)),
        PropertyValue::F64(value) if !value.get().is_nan() => {
            Some(SecondaryRangeValue::F64(*value))
        }
        PropertyValue::F32(value) if !value.get().is_nan() => {
            Some(SecondaryRangeValue::F32(*value))
        }
        PropertyValue::DateTime(value) => Some(SecondaryRangeValue::DateTime(*value)),
        PropertyValue::String(value) => Some(SecondaryRangeValue::String(value.clone())),
        PropertyValue::Null
        | PropertyValue::Bool(_)
        | PropertyValue::F64(_)
        | PropertyValue::F32(_)
        | PropertyValue::Bytes(_)
        | PropertyValue::I64Array(_)
        | PropertyValue::F64Array(_)
        | PropertyValue::F32Array(_)
        | PropertyValue::StringArray(_)
        | PropertyValue::Vector(_) => None,
    }
}

fn secondary_range_contains(
    lower: Option<&SecondaryRangeBound>,
    upper: Option<&SecondaryRangeBound>,
    value: &SecondaryRangeValue,
) -> bool {
    let above_lower = lower.is_none_or(|bound| match bound {
        SecondaryRangeBound::Inclusive(lower) => {
            secondary_range_compare(value, lower).is_some_and(|ordering| !ordering.is_lt())
        }
        SecondaryRangeBound::Exclusive(lower) => {
            secondary_range_compare(value, lower).is_some_and(Ordering::is_gt)
        }
    });
    let below_upper = upper.is_none_or(|bound| match bound {
        SecondaryRangeBound::Inclusive(upper) => {
            secondary_range_compare(value, upper).is_some_and(|ordering| !ordering.is_gt())
        }
        SecondaryRangeBound::Exclusive(upper) => {
            secondary_range_compare(value, upper).is_some_and(Ordering::is_lt)
        }
    });
    above_lower && below_upper
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum ExactNumber {
    NegativeInfinity,
    NegativeFinite {
        exponent: i16,
        odd_significand: u64,
        floor_log2: i16,
        normalized_significand: u64,
    },
    Zero,
    PositiveFinite {
        exponent: i16,
        odd_significand: u64,
        floor_log2: i16,
        normalized_significand: u64,
    },
    PositiveInfinity,
}

fn normalize_finite_number(negative: bool, significand: u64, exponent: i16) -> ExactNumber {
    assert_ne!(significand, 0, "zero has a dedicated exact-number variant");
    let trailing = significand.trailing_zeros() as i16;
    let odd_significand = significand >> trailing;
    let exponent = exponent + trailing;
    let floor_log2 = exponent + (u64::BITS - 1 - odd_significand.leading_zeros()) as i16;
    let normalized_significand = odd_significand << odd_significand.leading_zeros();
    if negative {
        ExactNumber::NegativeFinite {
            exponent,
            odd_significand,
            floor_log2,
            normalized_significand,
        }
    } else {
        ExactNumber::PositiveFinite {
            exponent,
            odd_significand,
            floor_log2,
            normalized_significand,
        }
    }
}

fn exact_i64(value: i64) -> ExactNumber {
    if value == 0 {
        ExactNumber::Zero
    } else {
        normalize_finite_number(value.is_negative(), value.unsigned_abs(), 0)
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
    Some(normalize_finite_number(negative, significand, exponent))
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
    Some(normalize_finite_number(negative, significand, exponent))
}

fn property_number(value: &PropertyValue) -> Option<ExactNumber> {
    match value {
        PropertyValue::I64(value) => Some(exact_i64(*value)),
        PropertyValue::F64(value) => exact_f64(value.get()),
        PropertyValue::F32(value) => exact_f32(value.get()),
        PropertyValue::Null
        | PropertyValue::Bool(_)
        | PropertyValue::DateTime(_)
        | PropertyValue::String(_)
        | PropertyValue::Bytes(_)
        | PropertyValue::I64Array(_)
        | PropertyValue::F64Array(_)
        | PropertyValue::F32Array(_)
        | PropertyValue::StringArray(_)
        | PropertyValue::Vector(_) => None,
    }
}

fn range_number(value: &SecondaryRangeValue) -> Option<Option<ExactNumber>> {
    match value {
        SecondaryRangeValue::I64(value) => Some(Some(exact_i64(*value))),
        SecondaryRangeValue::F64(value) => Some(exact_f64(value.get())),
        SecondaryRangeValue::F32(value) => Some(exact_f32(value.get())),
        SecondaryRangeValue::DateTime(_) | SecondaryRangeValue::String(_) => None,
    }
}

fn finite_magnitude(number: ExactNumber) -> Option<(i16, u64)> {
    match number {
        ExactNumber::NegativeFinite {
            floor_log2,
            normalized_significand,
            ..
        }
        | ExactNumber::PositiveFinite {
            floor_log2,
            normalized_significand,
            ..
        } => Some((floor_log2, normalized_significand)),
        ExactNumber::NegativeInfinity | ExactNumber::Zero | ExactNumber::PositiveInfinity => None,
    }
}

fn exact_number_compare(left: ExactNumber, right: ExactNumber) -> Ordering {
    use ExactNumber::{NegativeFinite, NegativeInfinity, PositiveFinite, PositiveInfinity, Zero};
    match (left, right) {
        (NegativeInfinity, NegativeInfinity)
        | (Zero, Zero)
        | (PositiveInfinity, PositiveInfinity) => Ordering::Equal,
        (NegativeInfinity, _) | (_, PositiveInfinity) => Ordering::Less,
        (_, NegativeInfinity) | (PositiveInfinity, _) => Ordering::Greater,
        (NegativeFinite { .. }, NegativeFinite { .. }) => finite_magnitude(right)
            .unwrap()
            .cmp(&finite_magnitude(left).unwrap()),
        (PositiveFinite { .. }, PositiveFinite { .. }) => finite_magnitude(left)
            .unwrap()
            .cmp(&finite_magnitude(right).unwrap()),
        (NegativeFinite { .. }, _) | (Zero, PositiveFinite { .. }) => Ordering::Less,
        (_, NegativeFinite { .. }) | (PositiveFinite { .. }, Zero) => Ordering::Greater,
    }
}

/// Partially compares two independently represented range-index values.
///
/// Numeric variants compare across types. Datetimes and strings compare only
/// within their own domains; NaN and mixed domains return `None`.
pub fn secondary_range_compare(
    left: &SecondaryRangeValue,
    right: &SecondaryRangeValue,
) -> Option<Ordering> {
    match (range_number(left), range_number(right)) {
        (Some(Some(left)), Some(Some(right))) => Some(exact_number_compare(left, right)),
        (Some(_), Some(_)) => None,
        (None, None) => match (left, right) {
            (SecondaryRangeValue::DateTime(left), SecondaryRangeValue::DateTime(right)) => {
                Some(left.cmp(right))
            }
            (SecondaryRangeValue::String(left), SecondaryRangeValue::String(right)) => {
                Some(left.cmp(right))
            }
            _ => None,
        },
        (Some(_), None) | (None, Some(_)) => None,
    }
}

/// Totally orders admitted range values as numeric, datetime, then string.
pub fn secondary_range_total_compare(
    left: &SecondaryRangeValue,
    right: &SecondaryRangeValue,
) -> Ordering {
    let domain = |value: &SecondaryRangeValue| match value {
        SecondaryRangeValue::I64(_) | SecondaryRangeValue::F64(_) | SecondaryRangeValue::F32(_) => {
            0_u8
        }
        SecondaryRangeValue::DateTime(_) => 1,
        SecondaryRangeValue::String(_) => 2,
    };
    domain(left).cmp(&domain(right)).then_with(|| {
        secondary_range_compare(left, right)
            .expect("same-domain admitted range values are comparable")
    })
}

/// Returns graph IDs whose independent values equal the requested value.
pub fn secondary_equality_ids(
    rows: &[(EntityId, PropertyValue)],
    query: &PropertyValue,
) -> Vec<EntityId> {
    let mut ids = rows
        .iter()
        .filter_map(|(entity, value)| secondary_values_equal(value, query).then_some(*entity))
        .collect::<Vec<_>>();
    ids.sort_unstable();
    ids
}

/// Returns graph IDs under equality semantics where a missing property is null.
pub fn secondary_optional_equality_ids(
    rows: &[(EntityId, Option<PropertyValue>)],
    query: &PropertyValue,
) -> Vec<EntityId> {
    let mut ids = rows
        .iter()
        .filter_map(|(entity, value)| {
            value
                .as_ref()
                .map_or(matches!(query, PropertyValue::Null), |value| {
                    secondary_values_equal(value, query)
                })
                .then_some(*entity)
        })
        .collect::<Vec<_>>();
    ids.sort_unstable();
    ids
}

/// Applies typed bounds, physical direction, entity-ID ties, and `LIMIT`.
pub fn secondary_range_ids(
    rows: &[(EntityId, SecondaryRangeValue)],
    range: &crate::action::SecondaryRange,
) -> Vec<EntityId> {
    let mut admitted = rows
        .iter()
        .filter(|(_, value)| secondary_range_contains(range.lower(), range.upper(), value))
        .cloned()
        .collect::<Vec<_>>();
    admitted.sort_by(|(left_entity, left_value), (right_entity, right_value)| {
        let ordering = secondary_range_total_compare(left_value, right_value);
        let ordering = match range.direction() {
            SecondaryRangeDirection::Ascending => ordering,
            SecondaryRangeDirection::Descending => ordering.reverse(),
        };
        ordering.then_with(|| left_entity.cmp(right_entity))
    });
    if let Some(limit) = range.limit() {
        admitted.truncate(limit.get() as usize);
    }
    admitted.into_iter().map(|(entity, _)| entity).collect()
}

fn float64_arrays_equal(
    left: &[crate::action::OracleF64],
    right: &[crate::action::OracleF64],
) -> bool {
    left.len() == right.len()
        && left.iter().zip(right).all(|(left, right)| {
            exact_f64(left.get())
                .zip(exact_f64(right.get()))
                .is_some_and(|(left, right)| left == right)
        })
}

fn float32_arrays_equal(
    left: &[crate::action::OracleF32],
    right: &[crate::action::OracleF32],
) -> bool {
    left.len() == right.len()
        && left.iter().zip(right).all(|(left, right)| {
            exact_f32(left.get())
                .zip(exact_f32(right.get()))
                .is_some_and(|(left, right)| left == right)
        })
}

fn tokenize(value: &str) -> BTreeSet<String> {
    value
        .split(|character: char| !character.is_alphanumeric())
        .filter(|term| !term.is_empty())
        .map(str::to_lowercase)
        .collect()
}

fn vector_distance(
    metric: VectorMetric,
    query: &crate::action::VectorValue,
    candidate: &crate::action::VectorValue,
) -> Result<FiniteF32> {
    let pairs = query
        .as_slice()
        .iter()
        .zip(candidate.as_slice())
        .map(|(left, right)| (left.get(), right.get()));
    let distance = match metric {
        VectorMetric::Euclidean => pairs
            .map(|(left, right)| {
                let delta = left - right;
                delta * delta
            })
            .sum::<f32>()
            .sqrt(),
        VectorMetric::Dot => -pairs.map(|(left, right)| left * right).sum::<f32>(),
        VectorMetric::Cosine => {
            let mut dot = 0.0_f32;
            let mut left_norm = 0.0_f32;
            let mut right_norm = 0.0_f32;
            for (left, right) in pairs {
                dot += left * right;
                left_norm += left * left;
                right_norm += right * right;
            }
            if left_norm == 0.0 || right_norm == 0.0 {
                return Err(TestkitError::ModelViolation(
                    "cosine distance is undefined for a zero vector".to_string(),
                ));
            }
            1.0 - dot / (left_norm.sqrt() * right_norm.sqrt())
        }
    };
    FiniteF32::try_new(distance)
}

/// One serializable committed transaction record.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct CommitRecord {
    /// Request that committed.
    pub request: RequestId,
    /// Snapshot used by the transaction.
    pub snapshot: Sequence,
    /// Commit sequence.
    pub commit: Sequence,
    /// Atomic writes.
    pub writes: Vec<WriteAction>,
}

/// Independent MVCC history checker for snapshot and commit ordering.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub struct MvccHistory {
    baseline: Sequence,
    current: Sequence,
    active: BTreeMap<RequestId, Sequence>,
    commits: Vec<CommitRecord>,
}

impl MvccHistory {
    /// Starts history checking after an already-committed fixture baseline.
    pub fn from_baseline(baseline: Sequence) -> Self {
        Self {
            baseline,
            current: baseline,
            active: BTreeMap::new(),
            commits: Vec::new(),
        }
    }

    /// Returns the latest committed sequence.
    pub const fn current(&self) -> Sequence {
        self.current
    }

    /// Begins a request at an existing snapshot.
    pub fn begin(&mut self, request: RequestId, snapshot: Sequence) -> Result<()> {
        if snapshot > self.current {
            return Err(TestkitError::ModelViolation(
                "request snapshot is newer than committed state".to_string(),
            ));
        }
        if self.active.insert(request, snapshot).is_some() {
            return Err(TestkitError::ModelViolation(
                "request already has an active snapshot".to_string(),
            ));
        }
        Ok(())
    }

    /// Commits all writes atomically at the next sequence.
    pub fn commit(&mut self, request: RequestId, writes: Vec<WriteAction>) -> Result<Sequence> {
        let Some(snapshot) = self.active.remove(&request) else {
            return Err(TestkitError::ModelViolation(
                "commit has no active request".to_string(),
            ));
        };
        self.current = self.current.checked_next()?;
        self.commits.push(CommitRecord {
            request,
            snapshot,
            commit: self.current,
            writes,
        });
        Ok(self.current)
    }

    /// Aborts one request without a commit record.
    pub fn abort(&mut self, request: RequestId) -> Result<()> {
        if self.active.remove(&request).is_none() {
            return Err(TestkitError::ModelViolation(
                "abort has no active request".to_string(),
            ));
        }
        Ok(())
    }

    /// Returns the stable snapshot owned by one active request.
    pub fn snapshot_for(&self, request: RequestId) -> Result<Sequence> {
        self.active.get(&request).copied().ok_or_else(|| {
            TestkitError::ModelViolation("request has no active snapshot".to_string())
        })
    }

    /// Returns the number of requests retaining snapshots or transactions.
    pub fn active_count(&self) -> usize {
        self.active.len()
    }

    /// Returns commits visible from one snapshot.
    pub fn visible_commits(&self, snapshot: Sequence) -> impl Iterator<Item = &CommitRecord> {
        self.commits
            .iter()
            .filter(move |record| record.commit <= snapshot)
    }

    /// Verifies strict commit order and that every snapshot predates its commit.
    pub fn validate(&self) -> Result<()> {
        let mut previous = self.baseline;
        for record in &self.commits {
            if record.commit != previous.checked_next()? || record.snapshot >= record.commit {
                return Err(TestkitError::ModelViolation(
                    "MVCC commit history is not serial and snapshot-ordered".to_string(),
                ));
            }
            previous = record.commit;
        }
        Ok(())
    }

    /// Verifies that every request released its snapshot or transaction.
    pub fn assert_quiescent(&self) -> Result<()> {
        if self.active.is_empty() {
            Ok(())
        } else {
            Err(TestkitError::ModelViolation(format!(
                "{} requests retain MVCC resources",
                self.active.len()
            )))
        }
    }

    /// Borrows committed history.
    pub fn commits(&self) -> &[CommitRecord] {
        &self.commits
    }
}

/// Resource category tracked for leak assertions.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum ResourceKind {
    /// Open request snapshot.
    Snapshot,
    /// Open transaction.
    Transaction,
    /// Background worker task.
    WorkerTask,
    /// Open file descriptor.
    FileDescriptor,
    /// Accounted memory allocation unit.
    MemoryUnit,
    /// In-flight object-store operation.
    ObjectStoreOperation,
}

/// Exact resource counts used by sustained-workload leak checks.
#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]
pub struct ResourceAccounting {
    counts: BTreeMap<ResourceKind, u64>,
}

impl ResourceAccounting {
    /// Acquires one resource unit.
    pub fn acquire(&mut self, kind: ResourceKind) -> Result<()> {
        let count = self.counts.entry(kind).or_default();
        let Some(next) = count.checked_add(1) else {
            return Err(TestkitError::ModelViolation(
                "resource accounting overflow".to_string(),
            ));
        };
        *count = next;
        Ok(())
    }

    /// Releases one resource unit and rejects underflow.
    pub fn release(&mut self, kind: ResourceKind) -> Result<()> {
        let Some(count) = self.counts.get_mut(&kind) else {
            return Err(TestkitError::ModelViolation(
                "resource release underflow".to_string(),
            ));
        };
        let Some(next) = count.checked_sub(1) else {
            return Err(TestkitError::ModelViolation(
                "resource release underflow".to_string(),
            ));
        };
        *count = next;
        if next == 0 {
            self.counts.remove(&kind);
        }
        Ok(())
    }

    /// Returns the current count for one resource.
    pub fn count(&self, kind: ResourceKind) -> u64 {
        self.counts.get(&kind).copied().unwrap_or(0)
    }

    /// Verifies all resources returned to zero.
    pub fn assert_quiescent(&self) -> Result<()> {
        if self.counts.is_empty() {
            Ok(())
        } else {
            Err(TestkitError::ModelViolation(format!(
                "resource leak: {:?}",
                self.counts
            )))
        }
    }
}

#[cfg(test)]
mod tests {
    use std::collections::BTreeMap;
    use std::num::{NonZeroU16, NonZeroU32};

    use crate::action::{
        EntityRange, OracleF32, OracleF64, PropertyMutation, PropertyPatch, SecondaryRange,
        TextQuery, VectorValue,
    };
    use crate::lifecycle::AbsentIndex;

    use super::*;

    fn node(id: u64, text: &str, vector: [f32; 2]) -> WriteAction {
        WriteAction::InsertNode {
            id: EntityId::new(id),
            label: LabelName::try_new("Doc").unwrap(),
            properties: BTreeMap::from([
                (
                    PropertyName::try_new("text").unwrap(),
                    PropertyValue::String(text.to_string()),
                ),
                (
                    PropertyName::try_new("embedding").unwrap(),
                    PropertyValue::Vector(
                        VectorValue::try_new(
                            vector
                                .into_iter()
                                .map(|value| FiniteF32::try_new(value).unwrap())
                                .collect(),
                        )
                        .unwrap(),
                    ),
                ),
            ]),
        }
    }

    #[test]
    fn secondary_oracle_exact_numeric_and_typed_equality_are_independent() {
        let exactly_representable = PropertyValue::I64(9_007_199_254_740_992);
        let next_integer = PropertyValue::I64(9_007_199_254_740_993);
        let equal_f64 = PropertyValue::F64(OracleF64::new(9_007_199_254_740_992.0));
        assert!(secondary_values_equal(&exactly_representable, &equal_f64));
        assert!(!secondary_values_equal(&next_integer, &equal_f64));
        assert!(secondary_values_equal(
            &PropertyValue::F64(OracleF64::new(-0.0)),
            &PropertyValue::F32(OracleF32::new(0.0)),
        ));
        assert!(!secondary_values_equal(
            &PropertyValue::F64(OracleF64::new(f64::NAN)),
            &PropertyValue::F64(OracleF64::new(f64::NAN)),
        ));
        assert!(!secondary_values_equal(
            &PropertyValue::Bool(true),
            &PropertyValue::String("true".to_string()),
        ));
        assert!(!secondary_values_equal(
            &PropertyValue::Bytes(vec![1, 2]),
            &PropertyValue::String("[1, 2]".to_string()),
        ));
        assert!(!secondary_values_equal(
            &PropertyValue::I64Array(vec![1, 2]),
            &PropertyValue::I64Array(vec![8, 9]),
        ));
        assert!(secondary_values_equal(
            &PropertyValue::F64Array(vec![OracleF64::new(-0.0)]),
            &PropertyValue::F64Array(vec![OracleF64::new(0.0)]),
        ));
        assert!(!secondary_values_equal(
            &PropertyValue::F64Array(vec![OracleF64::new(f64::NAN)]),
            &PropertyValue::F64Array(vec![OracleF64::new(f64::NAN)]),
        ));

        let first = SecondaryRangeValue::I64(9_007_199_254_740_992);
        let second = SecondaryRangeValue::I64(9_007_199_254_740_993);
        let float = SecondaryRangeValue::F64(OracleF64::new(9_007_199_254_740_992.0));
        assert_eq!(
            secondary_range_compare(&first, &float),
            Some(Ordering::Equal)
        );
        assert_eq!(
            secondary_range_compare(&second, &float),
            Some(Ordering::Greater)
        );
        assert_eq!(
            secondary_range_compare(&float, &second),
            Some(Ordering::Less)
        );
        assert_eq!(
            secondary_range_compare(
                &SecondaryRangeValue::F64(OracleF64::new(f64::from_bits(1))),
                &SecondaryRangeValue::F64(OracleF64::new(f64::MIN_POSITIVE)),
            ),
            Some(Ordering::Less)
        );
        assert_eq!(
            secondary_range_compare(
                &SecondaryRangeValue::F64(OracleF64::new(f64::NEG_INFINITY)),
                &SecondaryRangeValue::I64(i64::MIN),
            ),
            Some(Ordering::Less)
        );
        assert_eq!(
            secondary_range_total_compare(
                &SecondaryRangeValue::I64(i64::MAX),
                &SecondaryRangeValue::DateTime(i64::MIN),
            ),
            Ordering::Less
        );
        assert_eq!(
            secondary_range_total_compare(
                &SecondaryRangeValue::DateTime(i64::MAX),
                &SecondaryRangeValue::String(String::new()),
            ),
            Ordering::Less
        );
    }

    #[test]
    fn public_secondary_oracle_applies_missing_bounds_direction_ties_and_limit() {
        let missing_rows = vec![
            (EntityId::new(3), None),
            (EntityId::new(2), Some(PropertyValue::Null)),
            (
                EntityId::new(1),
                Some(PropertyValue::String("null".to_string())),
            ),
        ];
        assert_eq!(
            secondary_optional_equality_ids(&missing_rows, &PropertyValue::Null),
            vec![EntityId::new(2), EntityId::new(3)]
        );

        let range_rows = vec![
            (EntityId::new(3), SecondaryRangeValue::I64(0)),
            (
                EntityId::new(1),
                SecondaryRangeValue::F64(OracleF64::new(-0.0)),
            ),
            (EntityId::new(2), SecondaryRangeValue::I64(1)),
            (EntityId::new(4), SecondaryRangeValue::String(String::new())),
        ];
        let range = SecondaryRange::try_new(
            Some(SecondaryRangeBound::Inclusive(SecondaryRangeValue::I64(0))),
            Some(SecondaryRangeBound::Inclusive(SecondaryRangeValue::I64(1))),
            SecondaryRangeDirection::Descending,
            Some(NonZeroU32::new(3).unwrap()),
        )
        .unwrap();
        assert_eq!(
            secondary_range_ids(&range_rows, &range),
            vec![EntityId::new(2), EntityId::new(1), EntityId::new(3)]
        );
    }

    #[test]
    fn graph_writes_are_atomic_and_traversal_range_projection_cover_shapes() {
        let mut graph = GraphModel::default();
        graph.apply(&node(1, "hello", [1.0, 0.0])).unwrap();
        graph.apply(&node(2, "world", [0.0, 1.0])).unwrap();
        graph
            .apply(&WriteAction::InsertEdge {
                id: EntityId::new(3),
                label: LabelName::try_new("NEXT").unwrap(),
                from: EntityId::new(1),
                to: EntityId::new(2),
                properties: BTreeMap::new(),
            })
            .unwrap();
        assert_eq!(
            graph
                .read_graph(&ReadAction::Traversal {
                    start: EntityId::new(1),
                    direction: TraversalDirection::Outgoing,
                    max_depth: NonZeroU16::new(1).unwrap(),
                })
                .unwrap(),
            Some(ModelReadResult::Entities(vec![EntityRef::Node(
                EntityId::new(2)
            )]))
        );
        assert_eq!(
            graph
                .read_graph(&ReadAction::Range {
                    kind: ElementKind::Node,
                    range: EntityRange::try_new(EntityId::new(2), EntityId::new(9)).unwrap(),
                })
                .unwrap(),
            Some(ModelReadResult::Entities(vec![EntityRef::Node(
                EntityId::new(2)
            )]))
        );
        let before = graph.clone();
        assert!(graph
            .apply(&WriteAction::InsertEdge {
                id: EntityId::new(4),
                label: LabelName::try_new("BAD").unwrap(),
                from: EntityId::new(1),
                to: EntityId::new(99),
                properties: BTreeMap::new(),
            })
            .is_err());
        assert_eq!(graph, before);
    }

    #[test]
    fn lifecycle_visibility_text_and_vector_oracles_are_independent() {
        let mut oracle = OracleState::default();
        oracle
            .apply_write(&node(1, "hello graph", [1.0, 0.0]))
            .unwrap();
        oracle
            .apply_write(&node(2, "hello storage", [0.0, 1.0]))
            .unwrap();

        let text = IndexDefinition::Text {
            name: IndexName::try_new("text").unwrap(),
            element: ElementKind::Node,
            property: PropertyName::try_new("text").unwrap(),
        };
        let created = AbsentIndex::new(text).create().unwrap();
        let (create, building) = created.into_parts();
        oracle.apply_index(&create).unwrap();
        assert!(oracle
            .read_at(
                oracle.sequence(),
                &ReadAction::Text {
                    index: IndexName::try_new("text").unwrap(),
                    query: TextQuery::try_new("hello").unwrap(),
                    limit: NonZeroU32::new(10).unwrap(),
                },
            )
            .is_err());
        let (activate, _) = building.activate().into_parts();
        oracle.apply_index(&activate).unwrap();
        assert_eq!(
            oracle
                .read_at(
                    oracle.sequence(),
                    &ReadAction::Text {
                        index: IndexName::try_new("text").unwrap(),
                        query: TextQuery::try_new("hello graph").unwrap(),
                        limit: NonZeroU32::new(10).unwrap(),
                    },
                )
                .unwrap(),
            ModelReadResult::Entities(vec![EntityRef::Node(EntityId::new(1))])
        );

        let vector = IndexDefinition::Vector {
            name: IndexName::try_new("vector").unwrap(),
            element: ElementKind::Node,
            property: PropertyName::try_new("embedding").unwrap(),
            dimension: NonZeroU32::new(2).unwrap(),
            metric: VectorMetric::Euclidean,
        };
        let created = AbsentIndex::new(vector).create().unwrap();
        let (create, building) = created.into_parts();
        oracle.apply_index(&create).unwrap();
        let (activate, _) = building.activate().into_parts();
        oracle.apply_index(&activate).unwrap();
        let result = oracle
            .read_at(
                oracle.sequence(),
                &ReadAction::Vector {
                    index: IndexName::try_new("vector").unwrap(),
                    vector: VectorValue::try_new(vec![
                        FiniteF32::try_new(1.0).unwrap(),
                        FiniteF32::try_new(0.0).unwrap(),
                    ])
                    .unwrap(),
                    limit: NonZeroU32::new(1).unwrap(),
                    metric: VectorMetric::Euclidean,
                },
            )
            .unwrap();
        let ModelReadResult::Scored(scored) = result else {
            panic!("expected scored vector result");
        };
        assert_eq!(scored[0].entity, EntityRef::Node(EntityId::new(1)));
        assert_eq!(scored[0].distance.get(), 0.0);
    }

    #[test]
    fn secondary_oracle_uses_typed_equality_bounds_direction_and_limit() {
        let mut oracle = OracleState::default();
        for (id, rank) in [(1, -10), (2, -2), (3, -1), (4, 0), (5, 1)] {
            oracle
                .apply_write(&WriteAction::InsertNode {
                    id: EntityId::new(id),
                    label: LabelName::try_new("Doc").unwrap(),
                    properties: BTreeMap::from([(
                        PropertyName::try_new("rank").unwrap(),
                        PropertyValue::I64(rank),
                    )]),
                })
                .unwrap();
        }
        let definition = IndexDefinition::Secondary {
            name: IndexName::try_new("by-rank").unwrap(),
            element: ElementKind::Node,
            property: PropertyName::try_new("rank").unwrap(),
            unique: false,
        };
        let created = AbsentIndex::new(definition).create().unwrap();
        let (create, building) = created.into_parts();
        oracle.apply_index(&create).unwrap();
        let (activate, _) = building.activate().into_parts();
        oracle.apply_index(&activate).unwrap();

        assert_eq!(
            oracle
                .read_at(
                    oracle.sequence(),
                    &ReadAction::Secondary {
                        index: IndexName::try_new("by-rank").unwrap(),
                        value: PropertyValue::String("-2".to_string()),
                    },
                )
                .unwrap(),
            ModelReadResult::Entities(Vec::new()),
            "typed equality must not coerce a string to an integer"
        );

        let range = crate::action::SecondaryRange::try_new(
            Some(SecondaryRangeBound::Inclusive(SecondaryRangeValue::I64(
                -10,
            ))),
            Some(SecondaryRangeBound::Exclusive(SecondaryRangeValue::I64(0))),
            SecondaryRangeDirection::Descending,
            NonZeroU32::new(2),
        )
        .unwrap();
        assert_eq!(
            oracle
                .read_at(
                    oracle.sequence(),
                    &ReadAction::SecondaryRange {
                        index: IndexName::try_new("by-rank").unwrap(),
                        range,
                    },
                )
                .unwrap(),
            ModelReadResult::Entities(vec![
                EntityRef::Node(EntityId::new(3)),
                EntityRef::Node(EntityId::new(2)),
            ])
        );
    }

    #[test]
    fn deterministic_secondary_workload_spans_lifecycle_mutation_drop_and_reopen() {
        let mut oracle = OracleState::default();
        for (id, rank) in [(1, -10), (2, -2)] {
            oracle
                .apply_write(&WriteAction::InsertNode {
                    id: EntityId::new(id),
                    label: LabelName::try_new("Doc").unwrap(),
                    properties: BTreeMap::from([(
                        PropertyName::try_new("rank").unwrap(),
                        PropertyValue::I64(rank),
                    )]),
                })
                .unwrap();
        }
        let definition = IndexDefinition::Secondary {
            name: IndexName::try_new("by-rank").unwrap(),
            element: ElementKind::Node,
            property: PropertyName::try_new("rank").unwrap(),
            unique: false,
        };
        let created = AbsentIndex::new(definition).create().unwrap();
        let (create, building) = created.into_parts();
        oracle.apply_index(&create).unwrap();
        oracle.apply_index(&building.build()).unwrap();
        oracle
            .apply_write(&WriteAction::Update {
                target: EntityRef::Node(EntityId::new(2)),
                patch: PropertyPatch::try_new(BTreeMap::from([(
                    PropertyName::try_new("rank").unwrap(),
                    PropertyMutation::Set(PropertyValue::I64(-1)),
                )]))
                .unwrap(),
            })
            .unwrap();
        let activated = building.activate();
        let (activate, active) = activated.into_parts();
        oracle.apply_index(&activate).unwrap();
        let range = crate::action::SecondaryRange::try_new(
            None,
            None,
            SecondaryRangeDirection::Ascending,
            None,
        )
        .unwrap();
        assert_eq!(
            oracle
                .read_at(
                    oracle.sequence(),
                    &ReadAction::SecondaryRange {
                        index: IndexName::try_new("by-rank").unwrap(),
                        range: range.clone(),
                    },
                )
                .unwrap(),
            ModelReadResult::Entities(vec![
                EntityRef::Node(EntityId::new(1)),
                EntityRef::Node(EntityId::new(2)),
            ])
        );
        oracle
            .apply_write(&WriteAction::Delete {
                target: EntityRef::Node(EntityId::new(1)),
            })
            .unwrap();
        let dropped = active.drop_index();
        let (drop_action, retired) = dropped.into_parts();
        oracle.apply_index(&drop_action).unwrap();
        assert!(oracle
            .read_at(
                oracle.sequence(),
                &ReadAction::SecondaryRange {
                    index: IndexName::try_new("by-rank").unwrap(),
                    range: range.clone(),
                },
            )
            .is_err());
        let recreated = retired.recreate().unwrap();
        let (recreate, rebuilding) = recreated.into_parts();
        oracle.apply_index(&recreate).unwrap();
        oracle.apply_index(&rebuilding.build()).unwrap();
        let (reactivate, _) = rebuilding.activate().into_parts();
        oracle.apply_index(&reactivate).unwrap();

        let reopened: OracleState =
            serde_json::from_slice(&serde_json::to_vec(&oracle).unwrap()).unwrap();
        assert_eq!(
            reopened
                .read_at(
                    reopened.sequence(),
                    &ReadAction::SecondaryRange {
                        index: IndexName::try_new("by-rank").unwrap(),
                        range,
                    },
                )
                .unwrap(),
            ModelReadResult::Entities(vec![EntityRef::Node(EntityId::new(2))])
        );
    }

    #[test]
    fn lifecycle_rejects_illegal_sequences() {
        let definition = IndexDefinition::Secondary {
            name: IndexName::try_new("secondary").unwrap(),
            element: ElementKind::Node,
            property: PropertyName::try_new("text").unwrap(),
            unique: false,
        };
        let created = AbsentIndex::new(definition).create().unwrap();
        let (create, building) = created.into_parts();
        let activation = building.clone().activate();
        let mut model = LifecycleModel::default();
        assert!(model.apply(activation.action()).is_err());
        model.apply(&create).unwrap();
        let (activate, _) = activation.into_parts();
        model.apply(&activate).unwrap();
        assert!(model
            .active(&IndexName::try_new("secondary").unwrap())
            .is_some());
    }

    #[test]
    fn mvcc_and_resource_models_reject_ordering_and_leak_failures() {
        let request = RequestId::new(1).unwrap();
        let mut history = MvccHistory::default();
        assert!(history.begin(request, Sequence::new(1)).is_err());
        history.begin(request, Sequence::initial()).unwrap();
        let commit = history.commit(request, Vec::new()).unwrap();
        assert_eq!(commit, Sequence::new(1));
        history.validate().unwrap();
        assert_eq!(history.visible_commits(Sequence::initial()).count(), 0);
        assert_eq!(history.visible_commits(commit).count(), 1);

        let mut resources = ResourceAccounting::default();
        assert!(resources.release(ResourceKind::Snapshot).is_err());
        resources.acquire(ResourceKind::Snapshot).unwrap();
        assert!(resources.assert_quiescent().is_err());
        resources.release(ResourceKind::Snapshot).unwrap();
        resources.assert_quiescent().unwrap();
    }
}
