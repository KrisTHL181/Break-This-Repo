//! Typed performance, lag, correctness, and resource acceptance gate.
//!
//! A comparison contains exactly ten baseline observations and ten candidate
//! observations. This makes the launch plan's stable-run requirement part of
//! the serialized type instead of a convention in CI.

use std::array;
use std::num::NonZeroU64;

use serde::{Deserialize, Serialize};

use crate::model::ResourceAccounting;
use crate::sustained::ReplicaLagPolicy;

/// Exact number of fixed-runner observations required for a launch decision.
pub const REQUIRED_STABLE_RUNS: usize = 10;

/// Metrics and invariant outcomes from one complete workload-matrix run.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct LaunchObservation {
    performance: LaunchPerformance,
    replica_lag: ReplicaLagObservation,
    correctness_violations: u64,
    resources_after_shutdown: ResourceAccounting,
}

impl LaunchObservation {
    /// Constructs one complete observation from typed metric groups.
    pub const fn new(
        performance: LaunchPerformance,
        replica_lag: ReplicaLagObservation,
        correctness_violations: u64,
        resources_after_shutdown: ResourceAccounting,
    ) -> Self {
        Self {
            performance,
            replica_lag,
            correctness_violations,
            resources_after_shutdown,
        }
    }
}

/// Throughput, latency, memory, and cache measurements from one stable run.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub struct LaunchPerformance {
    throughput_per_second: NonZeroU64,
    p95_latency_micros: NonZeroU64,
    p99_latency_micros: NonZeroU64,
    memory_high_water_bytes: u64,
    cache_high_water_bytes: u64,
}

impl LaunchPerformance {
    /// Constructs one positive-throughput, positive-latency measurement.
    pub const fn new(
        throughput_per_second: NonZeroU64,
        p95_latency_micros: NonZeroU64,
        p99_latency_micros: NonZeroU64,
        memory_high_water_bytes: u64,
        cache_high_water_bytes: u64,
    ) -> Self {
        Self {
            throughput_per_second,
            p95_latency_micros,
            p99_latency_micros,
            memory_high_water_bytes,
            cache_high_water_bytes,
        }
    }
}

/// Exact reader convergence observation from one stable run.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub struct ReplicaLagObservation {
    replica_lag_millis: u64,
    replica_lag_commits: u64,
}

impl ReplicaLagObservation {
    /// Constructs one duration- and sequence-grounded lag observation.
    pub const fn new(replica_lag_millis: u64, replica_lag_commits: u64) -> Self {
        Self {
            replica_lag_millis,
            replica_lag_commits,
        }
    }
}

/// Exactly ten fixed-runner observations.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(transparent)]
pub struct StableLaunchRuns([LaunchObservation; REQUIRED_STABLE_RUNS]);

impl StableLaunchRuns {
    /// Wraps the exact compile-time run count.
    pub const fn new(observations: [LaunchObservation; REQUIRED_STABLE_RUNS]) -> Self {
        Self(observations)
    }

    /// Borrows every observation in stable execution order.
    pub const fn observations(&self) -> &[LaunchObservation; REQUIRED_STABLE_RUNS] {
        &self.0
    }
}

/// Same-host baseline and candidate observations for one launch decision.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct LaunchComparison {
    baseline: StableLaunchRuns,
    candidate: StableLaunchRuns,
}

impl LaunchComparison {
    /// Constructs a comparison whose two sides have the required run count.
    pub const fn new(baseline: StableLaunchRuns, candidate: StableLaunchRuns) -> Self {
        Self {
            baseline,
            candidate,
        }
    }

    /// Applies the reviewed cloud-launch thresholds.
    pub fn evaluate(
        &self,
        lag_policy: ReplicaLagPolicy,
    ) -> std::result::Result<(), LaunchGateFailures> {
        let mut violations = Vec::new();
        let baseline = self.baseline.observations();
        let candidate = self.candidate.observations();

        let baseline_throughput = upper_median(array::from_fn(|index| {
            baseline[index].performance.throughput_per_second.get()
        }));
        let candidate_throughput = upper_median(array::from_fn(|index| {
            candidate[index].performance.throughput_per_second.get()
        }));
        if ratio_below(candidate_throughput, baseline_throughput, 85) {
            violations.push(LaunchGateViolation::ThroughputRegression {
                baseline_per_second: baseline_throughput,
                candidate_per_second: candidate_throughput,
                maximum_percent: 15,
            });
        }

        for (percentile, baseline_value, candidate_values) in [
            (
                LatencyPercentile::P95,
                upper_median(array::from_fn(|index| {
                    baseline[index].performance.p95_latency_micros.get()
                })),
                array::from_fn(|index| candidate[index].performance.p95_latency_micros.get()),
            ),
            (
                LatencyPercentile::P99,
                upper_median(array::from_fn(|index| {
                    baseline[index].performance.p99_latency_micros.get()
                })),
                array::from_fn(|index| candidate[index].performance.p99_latency_micros.get()),
            ),
        ] {
            let candidate_value = upper_median(candidate_values);
            let confirmed_runs = candidate_values
                .into_iter()
                .filter(|value| ratio_above(*value, baseline_value, 120))
                .count();
            if ratio_above(candidate_value, baseline_value, 120) && confirmed_runs >= 2 {
                violations.push(LaunchGateViolation::LatencyRegression {
                    percentile,
                    baseline_micros: baseline_value,
                    candidate_micros: candidate_value,
                    confirmed_runs,
                    maximum_percent: 20,
                });
            }
        }

        for (resource, baseline_value, candidate_value) in [
            (
                CapacityMetric::Memory,
                maximum(array::from_fn(|index| {
                    baseline[index].performance.memory_high_water_bytes
                })),
                maximum(array::from_fn(|index| {
                    candidate[index].performance.memory_high_water_bytes
                })),
            ),
            (
                CapacityMetric::Cache,
                maximum(array::from_fn(|index| {
                    baseline[index].performance.cache_high_water_bytes
                })),
                maximum(array::from_fn(|index| {
                    candidate[index].performance.cache_high_water_bytes
                })),
            ),
        ] {
            if ratio_above(candidate_value, baseline_value, 120) {
                violations.push(LaunchGateViolation::CapacityRegression {
                    resource,
                    baseline_bytes: baseline_value,
                    candidate_bytes: candidate_value,
                    maximum_percent: 20,
                });
            }
        }

        for (run, observation) in candidate.iter().enumerate() {
            if observation.correctness_violations != 0 {
                violations.push(LaunchGateViolation::CorrectnessViolation {
                    run,
                    count: observation.correctness_violations,
                });
            }
            if u128::from(observation.replica_lag.replica_lag_millis)
                > lag_policy.maximum_duration().as_millis()
                || observation.replica_lag.replica_lag_commits > lag_policy.maximum_commits()
            {
                violations.push(LaunchGateViolation::ReplicaLagExceeded {
                    run,
                    observed_millis: observation.replica_lag.replica_lag_millis,
                    maximum_millis: lag_policy.maximum_duration().as_millis(),
                    observed_commits: observation.replica_lag.replica_lag_commits,
                    maximum_commits: lag_policy.maximum_commits(),
                });
            }
            if observation
                .resources_after_shutdown
                .assert_quiescent()
                .is_err()
            {
                violations.push(LaunchGateViolation::ResourceLeak { run });
            }
        }

        let mut violations = violations.into_iter();
        let Some(first) = violations.next() else {
            return Ok(());
        };
        Err(LaunchGateFailures {
            first,
            remaining: violations.collect(),
        })
    }
}

/// Latency percentile governed by the launch threshold.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum LatencyPercentile {
    /// 95th-percentile request latency.
    P95,
    /// 99th-percentile request latency.
    P99,
}

/// Capacity high-water metric governed by the launch threshold.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum CapacityMetric {
    /// Process memory high-water bytes.
    Memory,
    /// Accounted cache-resident bytes.
    Cache,
}

/// One exact reason a candidate cannot pass the launch gate.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(tag = "kind", rename_all = "snake_case")]
pub enum LaunchGateViolation {
    /// Median throughput fell by more than 15%.
    ThroughputRegression {
        /// Baseline upper-median operations per second.
        baseline_per_second: u64,
        /// Candidate upper-median operations per second.
        candidate_per_second: u64,
        /// Reviewed maximum regression percentage.
        maximum_percent: u8,
    },
    /// Median latency exceeded 20% and at least one rerun confirmed it.
    LatencyRegression {
        /// Regressed percentile.
        percentile: LatencyPercentile,
        /// Baseline upper-median latency.
        baseline_micros: u64,
        /// Candidate upper-median latency.
        candidate_micros: u64,
        /// Candidate runs independently exceeding the threshold.
        confirmed_runs: usize,
        /// Reviewed maximum regression percentage.
        maximum_percent: u8,
    },
    /// Memory or cache high-water usage grew by more than 20%.
    CapacityRegression {
        /// Regressed capacity metric.
        resource: CapacityMetric,
        /// Baseline maximum bytes.
        baseline_bytes: u64,
        /// Candidate maximum bytes.
        candidate_bytes: u64,
        /// Reviewed maximum growth percentage.
        maximum_percent: u8,
    },
    /// An independent model or lifecycle invariant failed.
    CorrectnessViolation {
        /// Zero-based stable-run index.
        run: usize,
        /// Number of violations reported by that run.
        count: u64,
    },
    /// Reader convergence exceeded its duration or sequence policy.
    ReplicaLagExceeded {
        /// Zero-based stable-run index.
        run: usize,
        /// Measured convergence duration.
        observed_millis: u64,
        /// Allowed convergence duration.
        maximum_millis: u128,
        /// Measured committed-sequence lag.
        observed_commits: u64,
        /// Allowed committed-sequence lag.
        maximum_commits: u64,
    },
    /// Shutdown retained at least one accounted resource.
    ResourceLeak {
        /// Zero-based stable-run index.
        run: usize,
    },
}

/// Non-empty collection of launch-gate failures.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct LaunchGateFailures {
    first: LaunchGateViolation,
    remaining: Vec<LaunchGateViolation>,
}

impl LaunchGateFailures {
    /// Iterates every failure, beginning with the required first item.
    pub fn iter(&self) -> impl Iterator<Item = &LaunchGateViolation> {
        std::iter::once(&self.first).chain(&self.remaining)
    }
}

fn upper_median(mut values: [u64; REQUIRED_STABLE_RUNS]) -> u64 {
    values.sort_unstable();
    values[REQUIRED_STABLE_RUNS / 2]
}

fn maximum(values: [u64; REQUIRED_STABLE_RUNS]) -> u64 {
    values
        .into_iter()
        .max()
        .expect("stable run set is non-empty")
}

fn ratio_below(candidate: u64, baseline: u64, retained_percent: u8) -> bool {
    u128::from(candidate) * 100 < u128::from(baseline) * u128::from(retained_percent)
}

fn ratio_above(candidate: u64, baseline: u64, allowed_percent: u8) -> bool {
    u128::from(candidate) * 100 > u128::from(baseline) * u128::from(allowed_percent)
}

#[cfg(test)]
mod tests {
    use crate::model::ResourceKind;

    use super::*;

    fn observation(
        throughput: u64,
        p95: u64,
        p99: u64,
        memory: u64,
        cache: u64,
    ) -> LaunchObservation {
        LaunchObservation::new(
            LaunchPerformance::new(
                NonZeroU64::new(throughput).unwrap(),
                NonZeroU64::new(p95).unwrap(),
                NonZeroU64::new(p99).unwrap(),
                memory,
                cache,
            ),
            ReplicaLagObservation::new(1_000, 2),
            0,
            ResourceAccounting::default(),
        )
    }

    fn runs(observation: LaunchObservation) -> StableLaunchRuns {
        StableLaunchRuns::new(array::from_fn(|_| observation.clone()))
    }

    #[test]
    fn unchanged_ten_run_candidate_passes_and_round_trips() {
        let runs = runs(observation(1_000, 100, 150, 1_000, 500));
        let comparison = LaunchComparison::new(runs.clone(), runs);
        comparison
            .evaluate(ReplicaLagPolicy::launch_default())
            .unwrap();
        let json = serde_json::to_vec(&comparison).unwrap();
        assert_eq!(
            serde_json::from_slice::<LaunchComparison>(&json).unwrap(),
            comparison
        );
    }

    #[test]
    fn threshold_failures_are_non_empty_and_cover_every_launch_dimension() {
        let baseline = runs(observation(1_000, 100, 150, 1_000, 500));
        let mut leaked = ResourceAccounting::default();
        leaked.acquire(ResourceKind::Snapshot).unwrap();
        let candidate = StableLaunchRuns::new(array::from_fn(|_| {
            LaunchObservation::new(
                LaunchPerformance::new(
                    NonZeroU64::new(849).unwrap(),
                    NonZeroU64::new(121).unwrap(),
                    NonZeroU64::new(181).unwrap(),
                    1_201,
                    601,
                ),
                ReplicaLagObservation::new(30_001, 257),
                1,
                leaked.clone(),
            )
        }));
        let failures = LaunchComparison::new(baseline, candidate)
            .evaluate(ReplicaLagPolicy::launch_default())
            .unwrap_err();
        let violations = failures.iter().collect::<Vec<_>>();
        assert!(violations.iter().any(|violation| matches!(
            violation,
            LaunchGateViolation::ThroughputRegression { .. }
        )));
        assert!(violations.iter().any(|violation| matches!(
            violation,
            LaunchGateViolation::LatencyRegression {
                percentile: LatencyPercentile::P95,
                ..
            }
        )));
        assert!(violations.iter().any(|violation| matches!(
            violation,
            LaunchGateViolation::LatencyRegression {
                percentile: LatencyPercentile::P99,
                ..
            }
        )));
        assert!(violations.iter().any(|violation| matches!(
            violation,
            LaunchGateViolation::CapacityRegression {
                resource: CapacityMetric::Memory,
                ..
            }
        )));
        assert!(violations.iter().any(|violation| matches!(
            violation,
            LaunchGateViolation::CapacityRegression {
                resource: CapacityMetric::Cache,
                ..
            }
        )));
        assert!(violations.iter().any(|violation| matches!(
            violation,
            LaunchGateViolation::CorrectnessViolation { .. }
        )));
        assert!(violations
            .iter()
            .any(|violation| matches!(violation, LaunchGateViolation::ReplicaLagExceeded { .. })));
        assert!(violations
            .iter()
            .any(|violation| matches!(violation, LaunchGateViolation::ResourceLeak { .. })));
    }

    #[test]
    fn one_latency_spike_does_not_satisfy_rerun_confirmation() {
        let baseline = runs(observation(1_000, 100, 150, 1_000, 500));
        let candidate = StableLaunchRuns::new(array::from_fn(|index| {
            if index == 0 {
                observation(1_000, 10_000, 15_000, 1_000, 500)
            } else {
                observation(1_000, 100, 150, 1_000, 500)
            }
        }));
        LaunchComparison::new(baseline, candidate)
            .evaluate(ReplicaLagPolicy::launch_default())
            .unwrap();
    }
}
