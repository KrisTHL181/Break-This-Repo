//! ADR-159 M3 r2 — `circuit_breaker.rs`.
//!
//! A peer that fails N consecutive times is removed from the `healthy_pool`
//! by the circuit breaker. After a cooldown it enters half-open and can be
//! probed; a successful probe re-enables it.
//!
//! Because this test cannot advance real wall-clock time, the registry is
//! constructed with a 10 ms cooldown (via the test-side `with_cooldown`
//! constructor documented in ADR-159 §Dispatch order — we exercise the
//! invariant, not the specific clock wiring).

use std::time::Duration;

use rvagent_a2a::identity::{agent_id_from_pubkey, AgentID};
use rvagent_a2a::routing::{PeerRegistry, PeerSnapshot};
use rvagent_a2a::types::{AgentCapabilities, AgentCard, AgentProvider, AgentSkill, AuthScheme};

use ed25519_dalek::SigningKey;
use rand_core::OsRng;

fn stable_id() -> AgentID {
    agent_id_from_pubkey(&SigningKey::generate(&mut OsRng).verifying_key())
}

fn demo_card() -> AgentCard {
    AgentCard {
        name: "probe".into(),
        description: "circuit test".into(),
        url: "https://probe.invalid".into(),
        provider: AgentProvider {
            organization: "ruvector".into(),
            url: None,
        },
        version: "0.1.0".into(),
        capabilities: AgentCapabilities::default(),
        skills: vec![AgentSkill {
            id: "rag.query".into(),
            name: "RAG".into(),
            description: "x".into(),
            tags: vec![],
            input_modes: vec![],
            output_modes: vec![],
        }],
        authentication: AuthScheme {
            schemes: vec!["bearer".into()],
        },
        metadata: serde_json::json!({ "ruvector": {} }),
    }
}

fn demo_snapshot(id: AgentID) -> PeerSnapshot {
    PeerSnapshot {
        id,
        card: demo_card(),
        ewma_latency_ms: 100.0,
        ewma_cost_usd: 0.01,
        open_tasks: 0,
        failure_rate: 0.0,
    }
}

#[tokio::test]
async fn three_failures_drop_peer_from_healthy_pool_and_success_restores_it() {
    // ADR-159 M3 r2: "Peer removed from pool after N failures; half-open
    // probe after cooldown; full re-enable on success."
    let id = stable_id();
    // `with_cooldown` is the test-only constructor declared alongside
    // `PeerRegistry::new` — ADR-159 calls out that the cooldown has to be
    // tunable to keep the test deterministic.
    let registry = PeerRegistry::with_cooldown(Duration::from_millis(10));
    registry.upsert(demo_snapshot(id.clone()));

    // Healthy to start.
    assert_eq!(
        registry.healthy_pool().len(),
        1,
        "peer should start healthy"
    );

    for _ in 0..3 {
        registry.record_failure(&id);
    }
    assert!(
        registry.healthy_pool().is_empty(),
        "peer should be dropped after 3 consecutive failures"
    );

    // Wait past cooldown → half-open → peer appears in pool again for probe.
    tokio::time::sleep(Duration::from_millis(25)).await;
    assert_eq!(
        registry.healthy_pool().len(),
        1,
        "peer should be eligible for probe after cooldown (half-open)"
    );

    registry.record_success(&id);
    assert_eq!(
        registry.healthy_pool().len(),
        1,
        "successful probe should fully re-enable peer"
    );
}
