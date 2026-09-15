//! Non-blocking anonymous query telemetry transport.

use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::Arc;

use thiserror::Error;

use crate::cli;
use crate::telemetry::{self, Source};

use super::{ClusterId, QueryEvent};

#[derive(Debug, Error)]
pub enum TransportConfigError {
    #[error("query telemetry source must be helix-server or helix-embedded")]
    InvalidSource,
    #[error(transparent)]
    LocalConfiguration(#[from] cli::CliMetricsError),
    #[error(transparent)]
    Telemetry(#[from] telemetry::TelemetryError),
    #[error(transparent)]
    Query(#[from] super::ValidationError),
    #[error("HELIX_CLUSTER_ID must be valid UTF-8")]
    InvalidClusterIdEncoding,
}

#[derive(Clone, Copy, Debug, Default, Eq, PartialEq)]
pub struct MetricsCounterSnapshot {
    pub emitted_events: u64,
    pub dropped_invalid_events: u64,
    pub dropped_ingress_full: u64,
}

#[derive(Default)]
struct MetricsCounters {
    emitted_events: AtomicU64,
    dropped_invalid_events: AtomicU64,
    dropped_ingress_full: AtomicU64,
}

impl MetricsCounters {
    fn snapshot(&self) -> MetricsCounterSnapshot {
        MetricsCounterSnapshot {
            emitted_events: self.emitted_events.load(Ordering::Relaxed),
            dropped_invalid_events: self.dropped_invalid_events.load(Ordering::Relaxed),
            dropped_ingress_full: self.dropped_ingress_full.load(Ordering::Relaxed),
        }
    }
}

#[derive(Clone)]
pub struct OssQueryMetrics {
    recorder: telemetry::Recorder,
    cluster_id: Option<ClusterId>,
    counters: Arc<MetricsCounters>,
}

impl OssQueryMetrics {
    /// Records one event without waiting for delivery.
    #[must_use]
    pub fn record(&self, event: QueryEvent) -> bool {
        let event = match event.into_telemetry_with_cluster(self.cluster_id.as_ref()) {
            Ok(event) => event,
            Err(_) => {
                self.counters
                    .dropped_invalid_events
                    .fetch_add(1, Ordering::Relaxed);
                return false;
            }
        };
        if self.recorder.record(event) {
            self.counters.emitted_events.fetch_add(1, Ordering::Relaxed);
            true
        } else {
            self.counters
                .dropped_ingress_full
                .fetch_add(1, Ordering::Relaxed);
            false
        }
    }

    #[must_use]
    pub fn counters(&self) -> MetricsCounterSnapshot {
        self.counters.snapshot()
    }
}

pub struct StartedQueryMetrics {
    pub recorder: OssQueryMetrics,
    pub runtime: telemetry::Runtime,
}

pub fn start(
    source: Source,
    identity: &super::OssIdentity,
    endpoint: impl Into<String>,
) -> Result<StartedQueryMetrics, TransportConfigError> {
    start_with_cluster_id(source, identity, endpoint, None)
}

fn start_with_cluster_id(
    source: Source,
    identity: &super::OssIdentity,
    endpoint: impl Into<String>,
    cluster_id: Option<ClusterId>,
) -> Result<StartedQueryMetrics, TransportConfigError> {
    if !matches!(source, Source::Server | Source::Embedded) {
        return Err(TransportConfigError::InvalidSource);
    }
    let started = telemetry::start(source, identity.client_info()?, endpoint)?;
    Ok(StartedQueryMetrics {
        recorder: OssQueryMetrics {
            recorder: started.recorder,
            cluster_id,
            counters: Arc::new(MetricsCounters::default()),
        },
        runtime: started.runtime,
    })
}

/// Starts telemetry from persisted privacy settings and environment overrides.
pub fn start_oss_from_env(
    source: Source,
) -> Result<Option<StartedQueryMetrics>, TransportConfigError> {
    let Some(settings) = cli::load_query_metrics_settings()? else {
        return Ok(None);
    };
    let cluster_id = cluster_id_from_env()?;
    start_with_cluster_id(source, &settings.identity, settings.endpoint, cluster_id).map(Some)
}

fn cluster_id_from_env() -> Result<Option<ClusterId>, TransportConfigError> {
    cluster_id_from_env_value(std::env::var("HELIX_CLUSTER_ID"))
}

fn cluster_id_from_env_value(
    value: Result<String, std::env::VarError>,
) -> Result<Option<ClusterId>, TransportConfigError> {
    match value {
        Ok(cluster_id) => ClusterId::new(cluster_id).map(Some).map_err(Into::into),
        Err(std::env::VarError::NotPresent) => Ok(None),
        Err(std::env::VarError::NotUnicode(_)) => {
            Err(TransportConfigError::InvalidClusterIdEncoding)
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::query::{CanonicalQuery, InstallationId, OssIdentity, QueryOutcome, QueryType};

    #[tokio::test]
    async fn recorder_rejects_oversized_properties_without_blocking() {
        let identity = OssIdentity::new(InstallationId::now(), None);
        let started =
            start(Source::Embedded, &identity, "http://127.0.0.1:9").expect("loopback endpoint");
        let event = QueryEvent::now(
            None,
            CanonicalQuery::new(serde_json::json!({"x": "x".repeat(20_000)}).to_string())
                .expect("query"),
            QueryType::Read,
            1,
            None,
            QueryOutcome::Succeeded {
                warnings: Vec::new(),
            },
            None,
        );
        assert!(!started.recorder.record(event));
        assert_eq!(started.recorder.counters().dropped_invalid_events, 1);
        started.runtime.shutdown().await;
    }

    #[tokio::test]
    async fn recorder_adds_the_configured_cluster_id() {
        let identity = OssIdentity::new(InstallationId::now(), None);
        let started = start_with_cluster_id(
            Source::Embedded,
            &identity,
            "http://127.0.0.1:9",
            Some(ClusterId::new("cluster-1").expect("cluster ID")),
        )
        .expect("loopback endpoint");
        let event = QueryEvent::now(
            None,
            CanonicalQuery::new(r#"{"queries":[],"returns":[]}"#).expect("query"),
            QueryType::Read,
            1,
            None,
            QueryOutcome::Succeeded {
                warnings: Vec::new(),
            },
            None,
        );

        assert!(started.recorder.record(event));
        assert_eq!(started.recorder.counters().emitted_events, 1);
        started.runtime.shutdown().await;
    }

    #[test]
    fn cluster_identity_uses_only_the_environment_contract() {
        assert_eq!(
            cluster_id_from_env_value(Ok("cluster-1".to_owned()))
                .expect("valid cluster environment"),
            Some(ClusterId::new("cluster-1").expect("cluster ID"))
        );
        assert_eq!(
            cluster_id_from_env_value(Err(std::env::VarError::NotPresent))
                .expect("missing cluster environment"),
            None
        );
        assert!(matches!(
            cluster_id_from_env_value(Ok(" cluster-1".to_owned())),
            Err(TransportConfigError::Query(
                super::super::ValidationError::InvalidContextId("cluster_id")
            ))
        ));
    }
}
