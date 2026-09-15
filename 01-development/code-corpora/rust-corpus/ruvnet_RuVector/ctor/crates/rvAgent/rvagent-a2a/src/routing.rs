//! Peer routing + circuit breaker (ADR-159 r2 — Routing strategy, M3).
//! Pluggable [`PeerSelector`] over a [`PeerRegistry`] whose breaker removes
//! flapping peers from the pool. Lifecycle: `Healthy → Open (N fails) →
//! HalfOpen (after cooldown) → Healthy (on success probe)`. `HalfOpen`
//! stays selectable so exactly one probe gets through.

use crate::identity::AgentID;
use crate::types::AgentCard;
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::{Arc, RwLock};
use std::time::{Duration, Instant};

/// Live view over a candidate peer — signed card + rolling stats fed by the
/// middleware rate-limit / metrics layer.
#[derive(Clone, Debug)]
pub struct PeerSnapshot {
    pub id: AgentID,
    pub card: AgentCard,
    pub ewma_latency_ms: f64,
    pub ewma_cost_usd: f64,
    pub open_tasks: u32,
    pub failure_rate: f64,
}

impl PeerSnapshot {
    fn supports_skill(&self, skill: &str) -> bool {
        self.card.skills.iter().any(|s| s.id == skill)
    }
    /// Capabilities from `metadata.ruvector.capabilities`. Absent ⇒ empty.
    fn ruvector_capabilities(&self) -> Vec<String> {
        self.card
            .metadata
            .pointer("/ruvector/capabilities")
            .and_then(|v| v.as_array())
            .map(|a| {
                a.iter()
                    .filter_map(|v| v.as_str().map(String::from))
                    .collect()
            })
            .unwrap_or_default()
    }
}

/// Pick a peer from a pool. `None` → "no eligible peer"; [`ChainedSelector`]
/// then tries the next link.
pub trait PeerSelector: Send + Sync {
    fn pick<'a>(
        &self,
        pool: &'a [PeerSnapshot],
        skill: &str,
        latency_budget_ms: Option<u64>,
    ) -> Option<&'a PeerSnapshot>;
    fn name(&self) -> &'static str;
}

fn f64_cmp(a: f64, b: f64) -> std::cmp::Ordering {
    a.partial_cmp(&b).unwrap_or(std::cmp::Ordering::Equal)
}

/// Lowest `ewma_cost_usd` under the latency cap. Ties break on `open_tasks`.
///
/// `budget_ms` is a selector-level latency ceiling; the `latency_budget_ms`
/// argument to `pick` overrides on a per-call basis if `Some`.
#[derive(Default, Debug, Clone, Copy)]
pub struct CheapestUnderLatency {
    pub budget_ms: u64,
}

impl PeerSelector for CheapestUnderLatency {
    fn pick<'a>(
        &self,
        pool: &'a [PeerSnapshot],
        skill: &str,
        latency_budget_ms: Option<u64>,
    ) -> Option<&'a PeerSnapshot> {
        let cap_ms = latency_budget_ms.unwrap_or(self.budget_ms).max(1);
        let cap = cap_ms as f64;
        pool.iter()
            .filter(|p| p.supports_skill(skill) && p.ewma_latency_ms <= cap)
            .min_by(|a, b| {
                f64_cmp(a.ewma_cost_usd, b.ewma_cost_usd)
                    .then_with(|| a.open_tasks.cmp(&b.open_tasks))
            })
    }
    fn name(&self) -> &'static str {
        "cheapest_under_latency"
    }
}

/// Minimum `ewma_latency_ms`, cost be damned.
#[derive(Default)]
pub struct LowestLatency;

impl PeerSelector for LowestLatency {
    fn pick<'a>(
        &self,
        pool: &'a [PeerSnapshot],
        skill: &str,
        _latency_budget_ms: Option<u64>,
    ) -> Option<&'a PeerSnapshot> {
        pool.iter()
            .filter(|p| p.supports_skill(skill))
            .min_by(|a, b| f64_cmp(a.ewma_latency_ms, b.ewma_latency_ms))
    }
    fn name(&self) -> &'static str {
        "lowest_latency"
    }
}

/// Deterministic rotation over eligible peers.
#[derive(Default)]
pub struct RoundRobin {
    counter: AtomicU64,
}

impl RoundRobin {
    pub fn new() -> Self {
        Self::default()
    }
}

impl PeerSelector for RoundRobin {
    fn pick<'a>(
        &self,
        pool: &'a [PeerSnapshot],
        skill: &str,
        _latency_budget_ms: Option<u64>,
    ) -> Option<&'a PeerSnapshot> {
        let eligible: Vec<&PeerSnapshot> =
            pool.iter().filter(|p| p.supports_skill(skill)).collect();
        if eligible.is_empty() {
            return None;
        }
        let i = self.counter.fetch_add(1, Ordering::Relaxed);
        Some(eligible[(i as usize) % eligible.len()])
    }
    fn name(&self) -> &'static str {
        "round_robin"
    }
}

/// First peer whose card lists every required capability under
/// `metadata.ruvector.capabilities`.
pub struct CapabilityMatch {
    pub required: Vec<String>,
}

impl CapabilityMatch {
    pub fn new(required: Vec<String>) -> Self {
        Self { required }
    }
}

impl PeerSelector for CapabilityMatch {
    fn pick<'a>(
        &self,
        pool: &'a [PeerSnapshot],
        skill: &str,
        _latency_budget_ms: Option<u64>,
    ) -> Option<&'a PeerSnapshot> {
        pool.iter().find(|p| {
            if !p.supports_skill(skill) {
                return false;
            }
            let caps = p.ruvector_capabilities();
            self.required
                .iter()
                .all(|req| caps.iter().any(|c| c == req))
        })
    }
    fn name(&self) -> &'static str {
        "capability_match"
    }
}

/// Try selectors in order; first non-None wins. Deployment-specific policy
/// (SLA tiers, regional preferences, trust gates) plugs in here.
pub struct ChainedSelector {
    pub chain: Vec<Box<dyn PeerSelector>>,
}

impl ChainedSelector {
    pub fn new(chain: Vec<Box<dyn PeerSelector>>) -> Self {
        Self { chain }
    }
}

impl PeerSelector for ChainedSelector {
    fn pick<'a>(
        &self,
        pool: &'a [PeerSnapshot],
        skill: &str,
        latency_budget_ms: Option<u64>,
    ) -> Option<&'a PeerSnapshot> {
        self.chain
            .iter()
            .find_map(|link| link.pick(pool, skill, latency_budget_ms))
    }
    fn name(&self) -> &'static str {
        "chained"
    }
}

/// Per-peer breaker state. `HalfOpen` stays selectable so exactly one probe
/// gets through; success → `Healthy`, failure → re-open.
#[derive(Clone, Debug)]
pub enum PeerHealth {
    Healthy,
    HalfOpen { until: Instant },
    Open { until: Instant, failures: u32 },
}

#[derive(Clone, Debug)]
struct PeerEntry {
    snapshot: PeerSnapshot,
    health: PeerHealth,
    consecutive_failures: u32,
}

const FAILURE_THRESHOLD: u32 = 3;
const COOLDOWN_SECS: u64 = 30;
const EWMA_ALPHA: f64 = 0.3;
const POISON: &str = "peer registry poisoned";

/// Peer pool + breaker bookkeeping. Thread-safe — clone the `Arc` across tasks.
pub struct PeerRegistry {
    inner: RwLock<Vec<PeerEntry>>,
    cooldown: Duration,
    failure_threshold: u32,
}

impl Default for PeerRegistry {
    fn default() -> Self {
        Self::new()
    }
}

impl PeerRegistry {
    pub fn new() -> Self {
        Self::with_breaker_config(FAILURE_THRESHOLD, Duration::from_secs(COOLDOWN_SECS))
    }

    /// Override the default (3 failures / 30s cooldown) — test + operator helper.
    pub fn with_breaker_config(failure_threshold: u32, cooldown: Duration) -> Self {
        Self {
            inner: RwLock::new(Vec::new()),
            cooldown,
            failure_threshold,
        }
    }

    /// Test helper: construct with a custom cooldown, keeping the default
    /// failure threshold. `tests/circuit_breaker.rs` calls this so it can
    /// advance through Open→HalfOpen→Healthy in tens of milliseconds.
    pub fn with_cooldown(cooldown: Duration) -> Self {
        Self::with_breaker_config(FAILURE_THRESHOLD, cooldown)
    }

    /// Insert or replace a peer in the registry. Alias of [`Self::add`],
    /// kept separately because callers that think in terms of "upsert"
    /// semantics find it clearer.
    pub fn upsert(&self, peer: PeerSnapshot) {
        self.add(peer);
    }

    pub fn add(&self, peer: PeerSnapshot) {
        let mut guard = self.inner.write().expect(POISON);
        if let Some(slot) = guard.iter_mut().find(|e| e.snapshot.id == peer.id) {
            slot.snapshot = peer;
            return;
        }
        guard.push(PeerEntry {
            snapshot: peer,
            health: PeerHealth::Healthy,
            consecutive_failures: 0,
        });
    }

    pub fn remove(&self, id: &AgentID) {
        self.inner
            .write()
            .expect(POISON)
            .retain(|e| &e.snapshot.id != id);
    }

    /// Return Healthy + HalfOpen peers. `Open` peers whose cooldown has
    /// elapsed are lazily transitioned to `HalfOpen` on this call — no
    /// background sweeper needed.
    pub fn healthy_pool(&self) -> Vec<PeerSnapshot> {
        let now = Instant::now();
        let mut guard = self.inner.write().expect(POISON);
        for entry in guard.iter_mut() {
            if let PeerHealth::Open { until, .. } = &entry.health {
                if now >= *until {
                    entry.health = PeerHealth::HalfOpen {
                        until: now + self.cooldown,
                    };
                }
            }
        }
        guard
            .iter()
            .filter(|e| !matches!(e.health, PeerHealth::Open { .. }))
            .map(|e| e.snapshot.clone())
            .collect()
    }

    #[tracing::instrument(skip(self), fields(peer = %id.0))]
    pub fn record_success(&self, id: &AgentID) {
        let mut guard = self.inner.write().expect(POISON);
        if let Some(e) = guard.iter_mut().find(|e| &e.snapshot.id == id) {
            e.consecutive_failures = 0;
            e.health = PeerHealth::Healthy;
        }
    }

    #[tracing::instrument(skip(self), fields(peer = %id.0))]
    pub fn record_failure(&self, id: &AgentID) {
        let now = Instant::now();
        let mut guard = self.inner.write().expect(POISON);
        if let Some(e) = guard.iter_mut().find(|e| &e.snapshot.id == id) {
            e.consecutive_failures = e.consecutive_failures.saturating_add(1);
            if e.consecutive_failures >= self.failure_threshold {
                e.health = PeerHealth::Open {
                    until: now + self.cooldown,
                    failures: e.consecutive_failures,
                };
            }
        }
    }

    /// EWMA-fold a fresh observation (alpha = 0.3) into rolling stats.
    pub fn update_stats(&self, id: &AgentID, latency_ms: f64, cost_usd: f64) {
        let mut guard = self.inner.write().expect(POISON);
        if let Some(e) = guard.iter_mut().find(|e| &e.snapshot.id == id) {
            let s = &mut e.snapshot;
            s.ewma_latency_ms = EWMA_ALPHA * latency_ms + (1.0 - EWMA_ALPHA) * s.ewma_latency_ms;
            s.ewma_cost_usd = EWMA_ALPHA * cost_usd + (1.0 - EWMA_ALPHA) * s.ewma_cost_usd;
        }
    }

    #[cfg(test)]
    fn health_of(&self, id: &AgentID) -> Option<PeerHealth> {
        self.inner
            .read()
            .expect(POISON)
            .iter()
            .find(|e| &e.snapshot.id == id)
            .map(|e| e.health.clone())
    }
}

/// Hand a skill, get a `PeerSnapshot` (or `None`). Async because future
/// implementations may await the discovery cache / health-check loop.
pub struct Router {
    pub registry: Arc<PeerRegistry>,
    pub selector: Arc<dyn PeerSelector>,
    pub latency_budget_ms: Option<u64>,
}

impl Router {
    pub fn new(
        registry: Arc<PeerRegistry>,
        selector: Arc<dyn PeerSelector>,
        latency_budget_ms: Option<u64>,
    ) -> Self {
        Self {
            registry,
            selector,
            latency_budget_ms,
        }
    }

    #[tracing::instrument(skip(self), fields(selector = %self.selector.name()))]
    pub async fn route(&self, skill: &str) -> Option<PeerSnapshot> {
        let pool = self.registry.healthy_pool();
        self.selector
            .pick(&pool, skill, self.latency_budget_ms)
            .cloned()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::types::{AgentCapabilities, AgentCard, AgentProvider, AgentSkill, AuthScheme};

    fn skill(id: &str) -> AgentSkill {
        AgentSkill {
            id: id.into(),
            name: id.into(),
            description: String::new(),
            tags: vec![],
            input_modes: vec![],
            output_modes: vec![],
        }
    }

    fn card(name: &str, skills: &[&str], caps: &[&str]) -> AgentCard {
        let metadata = if caps.is_empty() {
            serde_json::Value::Null
        } else {
            serde_json::json!({ "ruvector": { "capabilities": caps } })
        };
        AgentCard {
            name: name.into(),
            description: String::new(),
            url: format!("https://{name}.example"),
            version: "0".into(),
            provider: AgentProvider {
                organization: "ruvector".into(),
                url: None,
            },
            skills: skills.iter().map(|s| skill(s)).collect(),
            authentication: AuthScheme::default(),
            capabilities: AgentCapabilities::default(),
            metadata,
        }
    }

    fn peer(name: &str, skills: &[&str], lat: f64, cost: f64, tasks: u32) -> PeerSnapshot {
        peer_with_caps(name, skills, &[], lat, cost, tasks)
    }
    fn peer_with_caps(
        name: &str,
        skills: &[&str],
        caps: &[&str],
        lat: f64,
        cost: f64,
        tasks: u32,
    ) -> PeerSnapshot {
        PeerSnapshot {
            id: AgentID(format!("id-{name}")),
            card: card(name, skills, caps),
            ewma_latency_ms: lat,
            ewma_cost_usd: cost,
            open_tasks: tasks,
            failure_rate: 0.0,
        }
    }

    #[test]
    fn cheapest_under_latency_respects_cap_and_ties() {
        // `c` is cheapest overall but over the 200ms cap; `b` wins.
        let pool = vec![
            peer("a", &["s"], 100.0, 0.05, 0),
            peer("b", &["s"], 50.0, 0.02, 0),
            peer("c", &["s"], 300.0, 0.01, 0),
        ];
        assert_eq!(
            CheapestUnderLatency::default()
                .pick(&pool, "s", Some(200))
                .unwrap()
                .card
                .name,
            "b"
        );
        // Tie on cost → lowest open_tasks wins.
        let tie = vec![
            peer("busy", &["s"], 10.0, 0.01, 9),
            peer("idle", &["s"], 10.0, 0.01, 0),
        ];
        assert_eq!(
            CheapestUnderLatency::default()
                .pick(&tie, "s", Some(100))
                .unwrap()
                .card
                .name,
            "idle"
        );
        // Skill filter.
        let miss = vec![peer("a", &["other"], 10.0, 0.01, 0)];
        assert!(CheapestUnderLatency::default()
            .pick(&miss, "s", None)
            .is_none());
    }

    #[test]
    fn lowest_latency_picks_fastest() {
        let pool = vec![
            peer("slow", &["x"], 500.0, 0.001, 0),
            peer("fast", &["x"], 10.0, 0.10, 0),
        ];
        assert_eq!(
            LowestLatency.pick(&pool, "x", None).unwrap().card.name,
            "fast"
        );
    }

    #[test]
    fn round_robin_cycles() {
        let pool = vec![
            peer("a", &["x"], 0.0, 0.0, 0),
            peer("b", &["x"], 0.0, 0.0, 0),
            peer("c", &["x"], 0.0, 0.0, 0),
        ];
        let sel = RoundRobin::new();
        let names: Vec<&str> = (0..6)
            .map(|_| sel.pick(&pool, "x", None).unwrap().card.name.as_str())
            .collect();
        assert_eq!(names, vec!["a", "b", "c", "a", "b", "c"]);
        assert!(sel
            .pick(&[peer("z", &["other"], 0.0, 0.0, 0)], "x", None)
            .is_none());
    }

    #[test]
    fn capability_match_requires_all() {
        let pool = vec![
            peer_with_caps("a", &["x"], &["read"], 0.0, 0.0, 0),
            peer_with_caps("b", &["x"], &["read", "write"], 0.0, 0.0, 0),
        ];
        let sel = CapabilityMatch::new(vec!["read".into(), "write".into()]);
        assert_eq!(sel.pick(&pool, "x", None).unwrap().card.name, "b");
    }

    #[test]
    fn chained_selector_falls_through() {
        let pool = vec![peer("only", &["x"], 10.0, 0.0, 0)];
        let never: Box<dyn PeerSelector> =
            Box::new(CapabilityMatch::new(vec!["impossible".into()]));
        let fallback: Box<dyn PeerSelector> = Box::new(LowestLatency);
        let chain = ChainedSelector::new(vec![never, fallback]);
        assert_eq!(chain.pick(&pool, "x", None).unwrap().card.name, "only");
        // All links miss → None.
        let dead = vec![peer("only", &["other"], 0.0, 0.0, 0)];
        let chain2 = ChainedSelector::new(vec![
            Box::new(LowestLatency) as Box<dyn PeerSelector>,
            Box::new(CheapestUnderLatency::default()),
        ]);
        assert!(chain2.pick(&dead, "x", None).is_none());
    }

    #[test]
    fn breaker_full_cycle_open_halfopen_healthy() {
        let reg = PeerRegistry::with_breaker_config(2, Duration::from_millis(20));
        let p = peer("a", &["x"], 10.0, 0.01, 0);
        let id = p.id.clone();
        reg.add(p);
        reg.record_failure(&id);
        reg.record_failure(&id); // → Open
        assert!(matches!(reg.health_of(&id), Some(PeerHealth::Open { .. })));
        assert!(reg.healthy_pool().is_empty());
        std::thread::sleep(Duration::from_millis(30)); // cooldown → HalfOpen
        assert_eq!(reg.healthy_pool().len(), 1);
        assert!(matches!(
            reg.health_of(&id),
            Some(PeerHealth::HalfOpen { .. })
        ));
        reg.record_success(&id); // probe → Healthy
        assert!(matches!(reg.health_of(&id), Some(PeerHealth::Healthy)));
    }

    #[test]
    fn success_resets_consecutive_failures() {
        let reg = PeerRegistry::with_breaker_config(3, Duration::from_secs(60));
        let p = peer("a", &["x"], 10.0, 0.01, 0);
        let id = p.id.clone();
        reg.add(p);
        reg.record_failure(&id);
        reg.record_failure(&id);
        reg.record_success(&id);
        reg.record_failure(&id);
        reg.record_failure(&id);
        assert!(matches!(reg.health_of(&id), Some(PeerHealth::Healthy)));
    }

    #[test]
    fn ewma_and_registry_mutations() {
        let reg = PeerRegistry::new();
        let p = peer("a", &["x"], 100.0, 0.10, 0);
        let id = p.id.clone();
        reg.add(p);
        // EWMA: 0.3*200 + 0.7*100 = 130; 0.3*0.20 + 0.7*0.10 = 0.13.
        reg.update_stats(&id, 200.0, 0.20);
        let fresh = reg.healthy_pool().into_iter().find(|s| s.id == id).unwrap();
        assert!((fresh.ewma_latency_ms - 130.0).abs() < 1e-6);
        assert!((fresh.ewma_cost_usd - 0.13).abs() < 1e-9);
        // add() replaces existing; remove() drops.
        reg.add(peer("a", &["x"], 50.0, 0.05, 1));
        assert_eq!(reg.healthy_pool()[0].ewma_latency_ms, 50.0);
        reg.remove(&id);
        assert!(reg.healthy_pool().is_empty());
    }

    #[tokio::test]
    async fn router_routes_via_selector() {
        let reg = Arc::new(PeerRegistry::new());
        reg.add(peer("slow", &["x"], 500.0, 0.01, 0));
        reg.add(peer("fast", &["x"], 10.0, 0.05, 0));
        let router = Router::new(reg, Arc::new(LowestLatency), None);
        assert_eq!(router.route("x").await.unwrap().card.name, "fast");
        assert!(router.route("missing").await.is_none());
    }
}
