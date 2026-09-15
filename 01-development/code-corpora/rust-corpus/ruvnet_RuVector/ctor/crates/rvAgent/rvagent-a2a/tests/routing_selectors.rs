//! ADR-159 M3 r2 — `routing_selectors.rs`.
//!
//! Four stock `PeerSelector` implementations, exercised against a fixed
//! three-peer pool:
//!   - peer[0]: cost=0.10, lat=300ms
//!   - peer[1]: cost=0.05, lat=800ms
//!   - peer[2]: cost=0.08, lat=150ms
//!
//! `CheapestUnderLatency { budget_ms: 500 }` filters to peers with
//! `lat ≤ 500` (peer[0], peer[2]), then picks cheapest → peer[2] (0.08).
//! `LowestLatency` picks min-lat → peer[2] (150ms).
//! `RoundRobin` rotates 0→1→2→0.
//! `ChainedSelector([CapabilityMatch(unavailable), CheapestUnderLatency])`
//! falls through to `CheapestUnderLatency`.

use rvagent_a2a::identity::{agent_id_from_pubkey, AgentID};
use rvagent_a2a::routing::{
    CapabilityMatch, ChainedSelector, CheapestUnderLatency, LowestLatency, PeerSelector,
    PeerSnapshot, RoundRobin,
};
use rvagent_a2a::types::{AgentCapabilities, AgentCard, AgentProvider, AgentSkill, AuthScheme};

use ed25519_dalek::SigningKey;
use rand_core::OsRng;

fn stable_id() -> AgentID {
    agent_id_from_pubkey(&SigningKey::generate(&mut OsRng).verifying_key())
}

fn card(name: &str) -> AgentCard {
    AgentCard {
        name: name.into(),
        description: "routing test".into(),
        url: format!("https://{}.invalid", name),
        provider: AgentProvider {
            organization: "ruvector".into(),
            url: None,
        },
        version: "0.1.0".into(),
        capabilities: AgentCapabilities {
            streaming: false,
            push_notifications: false,
        },
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

fn snapshot(name: &str, cost: f64, lat: f64, tasks: u32) -> PeerSnapshot {
    PeerSnapshot {
        id: stable_id(),
        card: card(name),
        ewma_latency_ms: lat,
        ewma_cost_usd: cost,
        open_tasks: tasks,
        failure_rate: 0.0,
    }
}

fn pool() -> Vec<PeerSnapshot> {
    vec![
        snapshot("alpha", 0.10, 300.0, 0),
        snapshot("bravo", 0.05, 800.0, 0),
        snapshot("charlie", 0.08, 150.0, 0),
    ]
}

#[test]
fn cheapest_under_latency_prefers_peer2_over_latency_violator() {
    // bravo (0.05) is cheapest but exceeds 500ms. charlie (0.08) wins.
    let pool = pool();
    let sel = CheapestUnderLatency { budget_ms: 500 };
    let pick = sel
        .pick(&pool, "rag.query", Some(500))
        .expect("a pick under 500ms exists");
    assert_eq!(
        pick.card.name, "charlie",
        "expected charlie, got {}",
        pick.card.name
    );
}

#[test]
fn lowest_latency_picks_fastest_peer() {
    let pool = pool();
    let sel = LowestLatency;
    let pick = sel
        .pick(&pool, "rag.query", None)
        .expect("lowest-latency pick exists");
    assert_eq!(pick.card.name, "charlie");
}

#[test]
fn round_robin_rotates_deterministically() {
    let pool = pool();
    let sel = RoundRobin::new();
    // ADR-159 r2: `RoundRobin { seed }` — "deterministic rotation for test
    // reproducibility." We assert the three-cycle visits each peer once and
    // wraps back to the first.
    let a = sel.pick(&pool, "rag.query", None).expect("1st");
    let b = sel.pick(&pool, "rag.query", None).expect("2nd");
    let c = sel.pick(&pool, "rag.query", None).expect("3rd");
    let d = sel.pick(&pool, "rag.query", None).expect("4th (wraps)");

    // All three distinct in the first cycle; the fourth wraps to the first.
    let names = [
        a.card.name.as_str(),
        b.card.name.as_str(),
        c.card.name.as_str(),
    ];
    let mut sorted = names;
    sorted.sort();
    assert_eq!(
        sorted,
        ["alpha", "bravo", "charlie"],
        "rotation missed a peer: {:?}",
        names
    );
    assert_eq!(d.card.name, a.card.name, "4th should wrap to 1st");
}

#[test]
fn chained_selector_falls_through_on_miss() {
    // CapabilityMatch requires a capability none of our peers advertises.
    // Chained to CheapestUnderLatency it must fall through and still pick.
    let pool = pool();
    let chained = ChainedSelector::new(vec![
        Box::new(CapabilityMatch {
            required: vec!["premium.tier".into()],
        }),
        Box::new(CheapestUnderLatency { budget_ms: 500 }),
    ]);
    let pick = chained
        .pick(&pool, "rag.query", Some(500))
        .expect("fall-through pick exists");
    assert_eq!(pick.card.name, "charlie");
}
