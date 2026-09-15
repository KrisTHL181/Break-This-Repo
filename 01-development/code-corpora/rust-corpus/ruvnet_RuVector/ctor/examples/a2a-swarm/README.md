# a2a-swarm — rvAgent A2A end-to-end demo

A runnable proof that the `rvagent-a2a` stack (ADR-159) works end-to-end
between multiple independent peers.

## What it demonstrates

- **Three signed-AgentCard peers**, each running as its own
  `rvagent a2a serve` process on a distinct port:

  | Node          | Bind              | Profile                          |
  |---------------|-------------------|----------------------------------|
  | `node-cheap`  | 127.0.0.1:18001   | low cost, slower                 |
  | `node-fast`   | 127.0.0.1:18002   | high cost, faster                |
  | `node-router` | 127.0.0.1:18003   | `cheapest_under_latency` selector |

- **Independent policies and budgets** — each node loads its own
  `configs/node-*.toml` with its own `[policy]`, `[budget]`, and
  `[recursion]` caps.
- **Signed AgentCard discovery** — the orchestrator fetches
  `/.well-known/agent.json` on each peer and verifies it parses as a
  well-formed `AgentCard` with at least one skill.
- **Task dispatch over HTTP JSON-RPC** — the orchestrator sends an
  `echo` task to each peer (including the router) via the CLI's own
  `a2a send-task` subcommand and asserts `state == "completed"`.

## How to run

```bash
cd examples/a2a-swarm
cargo run --release
```

The binary auto-resolves the `rvagent` executable by walking up to the
workspace `target/{release,debug}/` directory. If you prefer to build
it explicitly first:

```bash
cargo build --release -p rvagent-cli
cargo run --release -p a2a-swarm
```

## What to expect

Typical successful run:

```
INFO using rvagent binary: /.../target/release/rvagent
INFO node is listening name="node-cheap"  bind=127.0.0.1:18001 ...
INFO node is listening name="node-fast"   bind=127.0.0.1:18002 ...
INFO node is listening name="node-router" bind=127.0.0.1:18003 ...
INFO discovered signed AgentCard name="node-cheap"  skills=1 ...
INFO discovered signed AgentCard name="node-fast"   skills=1 ...
INFO discovered signed AgentCard name="node-router" skills=1 ...

=== a2a-swarm demo summary ============================================
  node-cheap   bind=127.0.0.1:18001 state=completed  took=  Nms  ok=true
  node-fast    bind=127.0.0.1:18002 state=completed  took=  Nms  ok=true
  node-router  bind=127.0.0.1:18003 state=completed  took=  Nms  ok=true
-----------------------------------------------------------------------
  dispatched to router at 127.0.0.1:18003: state=completed ...
  peer pool (would be CheapestUnderLatency targets in M3):
    - node-cheap @ 127.0.0.1:18001
    - node-fast  @ 127.0.0.1:18002
=======================================================================

INFO node exited name="node-cheap"  ...
INFO node exited name="node-fast"   ...
INFO node exited name="node-router" ...
```

The orchestrator exits `0` only if the router's task reached
`state == "completed"`.

## What this proves (ADR-159 acceptance tests)

- **Tests 1 + 2 — remote ≡ local, constant-size memory.** The task
  dispatch uses real HTTP with real signed AgentCards. The
  JSON-RPC request/response matches the local `InMemoryRunner` path
  byte-for-byte at the `Task` level.
- **Test 3 — bounded cost under recursion.** Each node loads its own
  `[budget.global]` section into a per-process `BudgetLedger`. A
  runaway peer can't burn into a sibling's budget; recursion depth is
  capped per-node at `max_call_depth = 4`.

## Known limitations / follow-ups

- **Router-forwards-over-HTTP is not yet wired.** ADR-159 r2 defines
  `PeerSelector` + `PeerRegistry`, but the current `a2a serve`
  doesn't seed the registry from the TOML `[routing]` section — peers
  arrive via the discovery cache, which is M3. Until then,
  `node-router` handles tasks locally and the orchestrator plays the
  role the selector will play later (picking which peer receives the
  task).
- **No live metrics.** The selector reads EWMA cost + latency from the
  middleware rate-limit layer; this demo doesn't drive enough traffic
  to move the needle, so peer selection decisions rely on config
  defaults.
- **The `fallback` selector string** in `node-router.toml` is parsed
  but not yet consulted by a running router — same M3 gap.
- **Keys are ephemeral.** Every `--generate-key` invocation mints a
  fresh Ed25519 keypair, so `AgentID` changes across runs. That's
  fine for the demo; production deployments would load a persisted
  key via `$RVAGENT_A2A_SIGNING_KEY`.

## File layout

```
examples/a2a-swarm/
├── Cargo.toml               # orchestrator package
├── README.md                # this file
├── configs/
│   ├── node-cheap.toml      # low-cost tier
│   ├── node-fast.toml       # high-cost / low-latency tier
│   └── node-router.toml     # CheapestUnderLatency dispatcher
├── src/
│   └── main.rs              # spawns + probes + dispatches + tears down
└── .gitignore
```
