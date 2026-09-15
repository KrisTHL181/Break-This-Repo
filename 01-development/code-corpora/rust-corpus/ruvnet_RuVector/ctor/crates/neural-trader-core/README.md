# Neural Trader — Coherence-Gated Market Intelligence

A RuVector-native market intelligence stack that treats the limit order book as a **dynamic heterogeneous typed graph** with proof-gated mutation, MinCut coherence gating, and witnessable replay memory.

> **Do not trust prediction alone. Trust prediction only when the surrounding market structure is coherent enough to justify learning, remembering, or acting.**

## Architecture

```
L1 Ingest → L2 Graph → L3 GNN+Attention → L4 Memory → L5 Coherence → L6 Policy
```

| Layer | Crate | Status | Description |
|-------|-------|--------|-------------|
| L1 | [`neural-trader-core`](../neural-trader-core) | Implemented | Canonical event types, graph schema, ingest traits |
| L2 | `neural-trader-graph` | Planned | Dynamic heterogeneous typed graph construction |
| L3 | `neural-trader-gnn` | Planned | GNN embeddings + multi-head temporal attention |
| L4 | [`neural-trader-replay`](../neural-trader-replay) | Implemented | Reservoir store with gated writes + witness receipts |
| L5 | [`neural-trader-coherence`](../neural-trader-coherence) | Implemented | MinCut coherence gate, CUSUM drift detection |
| L6 | `neural-trader-policy` | Planned | Policy actuation with position sizing |
| WASM | [`neural-trader-wasm`](../neural-trader-wasm) | Implemented | Browser bindings via wasm-pack (npm: `@ruvector/neural-trader-wasm`) |

**ADRs:** [ADR-085](../../docs/adr/ADR-085-neural-trader-ruvector.md) (architecture) | [ADR-086](../../docs/adr/ADR-086-neural-trader-wasm.md) (WASM bindings)

## Crates

### neural-trader-core

Canonical market event types, graph schema, and ingest traits.

**Types:**

| Type | Fields | Purpose |
|------|--------|---------|
| `MarketEvent` | event_id, timestamps, venue, symbol, type, side, price, qty, flags, seq | Normalized event envelope for all market data |
| `EventType` | NewOrder, ModifyOrder, CancelOrder, Trade, BookSnapshot, SessionMarker, VenueStatus | 7 event discriminants |
| `Side` | Bid, Ask | Order side |
| `NodeKind` | Symbol, Venue, PriceLevel, Order, Trade, Event, Participant, TimeBucket, Regime, StrategyState | 10 graph node types |
| `EdgeKind` | AtLevel, NextTick, Generated, Matched, ModifiedFrom, CanceledBy, BelongsToSymbol, OnVenue, InWindow, CorrelatedWith, InRegime, AffectsState | 12 edge types |
| `PropertyKey` | VisibleDepth, EstimatedHiddenDepth, QueueLength, LocalImbalance, RefillRate, DepletionRate, SpreadDistance, LocalRealizedVol, CancelHazard, FillHazard, SlippageToMid, PostTradeImpact, InfluenceScore, CoherenceContribution, QueueEstimate, Age, ModifyCount | 17 property keys |
| `GraphDelta` | nodes_added, edges_added, properties_updated | Diff produced by graph projection |
| `StateWindow` | symbol, venue, time range, events | Sliding window for embedding |

**Traits:**

```rust
pub trait EventIngestor {
    fn ingest(&mut self, event: MarketEvent) -> anyhow::Result<()>;
}

pub trait GraphUpdater {
    fn apply_event(&mut self, event: &MarketEvent) -> anyhow::Result<GraphDelta>;
}

pub trait Embedder {
    fn embed_state(&self, ctx: &StateWindow) -> anyhow::Result<Vec<f32>>;
}
```

### neural-trader-coherence

MinCut coherence gate with CUSUM drift detection and proof-gated mutation protocol.

**Coherence Gate** — every memory write, model update, retrieval, and actuation must pass through the gate:

| Permission | Condition | Use Case |
|------------|-----------|----------|
| `allow_retrieve` | mincut above floor | Most permissive — read-only access |
| `allow_write` | mincut + CUSUM + drift + boundary stable | Memory writes |
| `allow_learn` | all of above + drift < 50% of max | Online learning (stricter drift margin) |
| `allow_act` | all base conditions | Live order placement |

**Regime-Adaptive Thresholds:**

| Regime | MinCut Floor (default) | Behavior |
|--------|----------------------|----------|
| Calm | 12 | Highest bar — full confidence required |
| Normal | 9 | Standard operation |
| Volatile | 6 | Relaxed floor — accepts structural uncertainty |

**CUSUM Drift Detection** monitors parameter drift and blocks mutations when score exceeds threshold (default 4.5).

**Types:**

| Type | Purpose |
|------|---------|
| `GateConfig` | All configurable thresholds (mincut floors, CUSUM, drift, boundary windows) |
| `ThresholdGate` | Default gate implementation using regime-adaptive thresholds |
| `GateContext` | Input: symbol, venue, timestamp, mincut, partition hash, scores, regime |
| `CoherenceDecision` | Output: four booleans + diagnostics (cut value, drift, CUSUM, reasons) |
| `RegimeLabel` | Calm, Normal, Volatile |
| `VerifiedToken` | Proof token minted when coherence + policy both approve a mutation |
| `WitnessReceipt` | Immutable audit record appended after every state mutation |

**Proof-Gated Mutation Protocol:**

```
Compute features → Coherence gate → Policy kernel → Mint token → Apply mutation → Append receipt
```

### neural-trader-replay

Witnessable replay segments, reservoir memory store, and audit receipt logging.

**Replay Segments** — sealed, signed windows containing:

- Compact subgraph events
- Embedding snapshots
- Realized labels (mid-price move, fill outcome)
- Coherence statistics at write time
- Lineage metadata (model ID, policy version)
- Witness hash for tamper detection

**Segment Classification** (7 kinds):

| Kind | Trigger |
|------|---------|
| HighUncertainty | Model confidence below threshold |
| LargeImpact | Significant realized PnL impact |
| RegimeTransition | Regime label changed |
| StructuralAnomaly | Graph structure deviated from norm |
| RareQueuePattern | Unusual queue behavior detected |
| HeadDisagreement | Prediction heads disagree |
| Routine | Standard periodic capture |

**ReservoirStore** — bounded memory with O(1) eviction:

```rust
pub trait MemoryStore {
    fn retrieve(&self, query: &MemoryQuery) -> anyhow::Result<Vec<ReplaySegment>>;
    fn maybe_write(&mut self, seg: ReplaySegment, gate: &CoherenceDecision) -> anyhow::Result<bool>;
}
```

- Writes are gated by `CoherenceDecision.allow_write`
- VecDeque-backed for O(1) front eviction when full
- Filterable by symbol and regime

**InMemoryReceiptLog** — append-only witness logger for testing and research.

### neural-trader-wasm

Browser WASM bindings wrapping all 3 crates. Published as [`@ruvector/neural-trader-wasm`](https://www.npmjs.com/package/@ruvector/neural-trader-wasm) on npm.

**Exported Classes:**

| Class | Key Methods |
|-------|-------------|
| `MarketEventWasm` | `new(eventType, symbolId, venueId, priceFp, qtyFp)`, getters/setters, `toJson()`, `fromJson()` |
| `GraphDeltaWasm` | `new()`, `nodesAdded()`, `edgesAdded()`, `propertiesUpdated()` |
| `GateConfigWasm` | `new()` with defaults, all threshold getters/setters |
| `GateContextWasm` | `new(9 params)`, all field getters |
| `ThresholdGateWasm` | `new(config)`, `evaluate(ctx)` |
| `CoherenceDecisionWasm` | `allowRetrieve/Write/Learn/Act`, `partitionHash`, `reasons()`, `toJson()` |
| `ReplaySegmentWasm` | `toJson()`, `fromJson()`, field getters |
| `ReservoirStoreWasm` | `new(maxSize)`, `len()`, `isEmpty()`, `maybeWrite()`, `retrieveBySymbol()` |

**Exported Enums:** `EventTypeWasm` (7), `SideWasm` (2), `RegimeLabelWasm` (3), `SegmentKindWasm` (7), `NodeKindWasm` (10), `EdgeKindWasm` (12), `PropertyKeyWasm` (17)

**Features:**
- BigInt-safe serialization (no u64 precision loss on nanosecond timestamps)
- Non-ASCII-safe hex parsing with optional `0x` prefix support
- Zero-size store guard
- 172 KB WASM binary (uncompressed)

## Quick Start

### Rust

```rust
use neural_trader_coherence::{GateConfig, GateContext, ThresholdGate, CoherenceGate, RegimeLabel};

// Create a coherence gate with default thresholds
let gate = ThresholdGate::new(GateConfig::default());

// Build context from current market state
let ctx = GateContext {
    symbol_id: 42,
    venue_id: 1,
    ts_ns: 1_704_067_200_000_000_000,
    mincut_value: 15,
    partition_hash: [0u8; 16],
    cusum_score: 1.0,
    drift_score: 0.1,
    regime: RegimeLabel::Calm,
    boundary_stable_count: 10,
};

let decision = gate.evaluate(&ctx).unwrap();
assert!(decision.all_allowed());
```

### JavaScript (WASM)

```js
import init, {
  GateConfigWasm, ThresholdGateWasm, GateContextWasm,
  RegimeLabelWasm, MarketEventWasm, EventTypeWasm,
  ReservoirStoreWasm, version, healthCheck,
} from '@ruvector/neural-trader-wasm';

await init();
console.log(version());      // "0.1.1"
console.log(healthCheck());   // true

// Coherence gate
const config = new GateConfigWasm();
const gate = new ThresholdGateWasm(config);
const ctx = new GateContextWasm(
  42, 1, 1000000n, 15n,
  '00000000000000000000000000000000',
  1.0, 0.1, RegimeLabelWasm.Calm, 10
);
const decision = gate.evaluate(ctx);
console.log(decision.allowAct);     // true
console.log(decision.allowLearn);   // true

// Market event
const evt = new MarketEventWasm(EventTypeWasm.Trade, 42, 1, 500000000n, 10000n);
const json = evt.toJson();
const restored = MarketEventWasm.fromJson(json);

// Replay memory
const store = new ReservoirStoreWasm(1000);
store.maybeWrite(segmentJson, decision.toJson());
const results = store.retrieveBySymbol(42, 10);
```

## Graph Schema

The order book is modeled as a **10-node-type, 12-edge-type heterogeneous dynamic graph** with 17 typed property keys.

```
                    ┌──────────┐
                    │  Symbol  │
                    └────┬─────┘
          BELONGS_TO_SYMBOL│      ON_VENUE
              ┌────────────┼────────────┐
              ▼            ▼            ▼
         ┌────────┐  ┌──────────┐  ┌───────┐
         │  Order │  │PriceLevel│  │ Venue │
         └───┬────┘  └────┬─────┘  └───────┘
     AT_LEVEL│    NEXT_TICK│
             ▼            ▼
         ┌────────┐  ┌──────────┐  ┌───────────┐
         │ Trade  │  │  Event   │  │TimeBucket │
         └────────┘  └──────────┘  └─────┬─────┘
                                 IN_REGIME│
                                         ▼
                    ┌───────────┐  ┌──────────────┐
                    │Participant│  │    Regime     │
                    └───────────┘  └──────────────┘
                                         │
                                  AFFECTS_STATE
                                         ▼
                                  ┌──────────────┐
                                  │StrategyState │
                                  └──────────────┘
```

**Node properties** include: visible/hidden depth, queue length, local imbalance, refill/depletion rates, spread distance, realized volatility, cancel/fill hazard, slippage, post-trade impact, influence score, coherence contribution, and more.

## Coherence Gate Flow

```
Market Events ──→ Graph Update ──→ Compute MinCut ──→ Gate Decision
                                        │
                              ┌─────────┼─────────┐
                              ▼         ▼         ▼
                          Retrieve    Write     Learn/Act
                         (permissive) (strict) (strictest)
                              │         │         │
                              ▼         ▼         ▼
                         allow if    allow if   allow if
                         cut ≥ floor  base_ok   base_ok AND
                                               drift < 50% max
```

**Tiered permissions** — retrieval is most permissive, learning is strictest:

| Gate | Requires |
|------|----------|
| Retrieve | MinCut ≥ regime floor |
| Write/Act | MinCut + CUSUM OK + drift OK + boundary stable |
| Learn | All above + drift < half max (extra safety margin) |

## Testing

```bash
# All Rust tests (22 total across 4 crates)
cargo test -p neural-trader-core -p neural-trader-coherence \
           -p neural-trader-replay -p neural-trader-wasm

# Node.js integration tests (43 tests)
cd crates/neural-trader-wasm && node tests/node-smoke.mjs

# Build WASM (requires wasm-pack)
cd crates/neural-trader-wasm
CARGO_PROFILE_RELEASE_CODEGEN_UNITS=256 CARGO_PROFILE_RELEASE_LTO=off \
  wasm-pack build --target web --scope ruvector --release

# Docker test
docker build -f crates/neural-trader-wasm/Dockerfile.test \
  -t neural-trader-wasm-test .
docker run --rm neural-trader-wasm-test
```

## Integration with RuVector Ecosystem

| RuVector Component | Neural Trader Integration |
|-------------------|--------------------------|
| [ruvector-graph](../ruvector-graph) | Host graph for the dynamic market model |
| [ruvector-mincut](../ruvector-mincut) | MinCut computation for coherence gate |
| [ruvector-gnn](../ruvector-gnn) | GNN learning layer over market graph |
| [ruvector-attention](../ruvector-attention) | 46 attention mechanisms for temporal modeling |
| [ruvector-postgres](../ruvector-postgres) | Relational source of record (event log, embeddings, segments) |
| [ruvector-graph-transformer](../ruvector-graph-transformer) | Graph transformer with proof-gated mutation |
| [ruvector-coherence](../ruvector-coherence) | Coherence measurement for signal quality |
| [ruvector-verified](../ruvector-verified) | Formal proof substrate for gated writes |
| [ruvector-mincut-gated-transformer](../ruvector-mincut-gated-transformer) | Early exit + sparse compute during regime instability |

## Roadmap

- [ ] L2: Graph construction from order book snapshots (`neural-trader-graph`)
- [ ] L3: GNN embeddings with temporal attention (`neural-trader-gnn`)
- [ ] L6: Policy kernel with Kelly criterion position sizing (`neural-trader-policy`)
- [ ] Backtest harness with historical LOB data (`neural-trader-backtest`)
- [ ] WebSocket/FIX feed adapters (`neural-trader-feed`)
- [ ] PostgreSQL integration via `ruvector-postgres` SQL functions
- [ ] Streaming sketch memory (Count-Min, Top-K, delta histograms)
- [ ] Cognitum edge deployment (deterministic coherence gate kernel)

## License

MIT OR Apache-2.0
