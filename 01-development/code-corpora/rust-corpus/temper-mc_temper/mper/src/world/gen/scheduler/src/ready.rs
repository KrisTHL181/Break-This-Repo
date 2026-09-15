use std::cmp::Ordering;

use temper_core::dimension::Dimension;

use crate::JobKey;

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum ReadySource {
    Local,
    Global,
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct ClaimedJob {
    pub key: JobKey,
    pub source: ReadySource,
}

impl ClaimedJob {
    pub const fn new(key: JobKey, source: ReadySource) -> Self {
        Self { key, source }
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct JobPriority {
    pub dependency_depth: u32,
    pub distance_to_target: u32,
    pub sequence: u64,
}

impl JobPriority {
    pub const fn new(dependency_depth: u32, distance_to_target: u32, sequence: u64) -> Self {
        Self {
            dependency_depth,
            distance_to_target,
            sequence,
        }
    }
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct ReadyJob {
    pub key: JobKey,
    pub priority: JobPriority,
}

impl ReadyJob {
    pub const fn new(key: JobKey, priority: JobPriority) -> Self {
        Self { key, priority }
    }
}

impl Ord for ReadyJob {
    fn cmp(&self, other: &Self) -> Ordering {
        self.key
            .stage
            .cmp(&other.key.stage)
            .then_with(|| {
                other
                    .priority
                    .dependency_depth
                    .cmp(&self.priority.dependency_depth)
            })
            .then_with(|| {
                other
                    .priority
                    .distance_to_target
                    .cmp(&self.priority.distance_to_target)
            })
            .then_with(|| other.priority.sequence.cmp(&self.priority.sequence))
            .then_with(|| {
                dimension_rank(self.key.dimension).cmp(&dimension_rank(other.key.dimension))
            })
            .then_with(|| self.key.pos.pos.x.cmp(&other.key.pos.pos.x))
            .then_with(|| self.key.pos.pos.y.cmp(&other.key.pos.pos.y))
    }
}

impl PartialOrd for ReadyJob {
    fn partial_cmp(&self, other: &Self) -> Option<Ordering> {
        Some(self.cmp(other))
    }
}

const fn dimension_rank(dimension: Dimension) -> u32 {
    match dimension {
        Dimension::Overworld => 0,
        Dimension::Nether => 1,
        Dimension::End => 2,
        Dimension::Custom(id) => id as u32 + 3,
    }
}

#[cfg(test)]
mod tests {
    use std::collections::BinaryHeap;

    use gen_core::GenStage;
    use temper_core::dimension::Dimension;
    use temper_core::pos::ChunkPos;

    use super::*;

    #[test]
    fn ready_job_prefers_higher_stage_then_closer_work() {
        let dimension = Dimension::Overworld;
        let pos = ChunkPos::new(0, 0);
        let lower_stage = ReadyJob::new(
            JobKey::new(dimension, pos, GenStage::NOISE),
            JobPriority::new(0, 0, 0),
        );
        let farther = ReadyJob::new(
            JobKey::new(dimension, pos, GenStage::SURFACE),
            JobPriority::new(0, 8, 1),
        );
        let closer = ReadyJob::new(
            JobKey::new(dimension, pos, GenStage::SURFACE),
            JobPriority::new(0, 2, 2),
        );

        let mut heap = BinaryHeap::new();
        heap.push(lower_stage);
        heap.push(farther);
        heap.push(closer);

        assert_eq!(heap.pop(), Some(closer));
        assert_eq!(heap.pop(), Some(farther));
        assert_eq!(heap.pop(), Some(lower_stage));
    }
}
