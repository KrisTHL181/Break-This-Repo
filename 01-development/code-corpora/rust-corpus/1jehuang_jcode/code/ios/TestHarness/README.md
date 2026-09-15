# iOS E2E Test Harness

A deterministic, no-LLM harness for developing and validating the jcode iOS
client (`JCodeMobile`) end-to-end. It replaces the role of the old Rust
simulator: one source of honest, repeatable server behavior the client can be
built against on this machine, without a device, network, or provider cost.

## Pieces

- **`mock_gateway.py`** - a self-contained (stdlib-only) mock of the jcode
  server gateway. Speaks the exact wire protocol from
  `crates/jcode-base/src/gateway.rs` on one TCP port:
  - `GET /health` -> status/version
  - `POST /pair` -> token exchange (code `123456` by default)
  - `GET /ws` -> WebSocket upgrade carrying the newline-delimited JSON protocol
  A `message` request triggers a scripted assistant turn (reasoning, text
  deltas, a `bash` tool-call lifecycle, tokens, done). `--push-demo` also pushes
  an out-of-band notification + compaction notice after connect.

- **`protocol_smoke_test.py`** - a stdlib WebSocket/HTTP client that drives the
  mock and asserts the full happy-path event sequence (pair, subscribe,
  history, message stream, set_model). Run it against either the mock or a real
  `jcode` gateway.

- **`run_e2e.sh`** - the one-command pipeline: `swift test` -> build app ->
  start mock -> smoke test -> boot simulator -> seed a paired credential ->
  launch -> screenshot.

## Usage

```bash
# Full pipeline, screenshot lands in $TMPDIR/jcode-ios-e2e/chat.png
./TestHarness/run_e2e.sh

# Also exercise the out-of-band notice toasts
./TestHarness/run_e2e.sh --push-demo

# Just the protocol assertions against a running gateway (mock or real)
python3 TestHarness/mock_gateway.py &        # or run a real `jcode` gateway
python3 TestHarness/protocol_smoke_test.py --port 7643
```

## How auto-connect is seeded

The app stores paired servers in the Keychain, falling back to
`Library/Application Support/jcode-servers.json` when the Keychain is
unavailable (unsigned simulator builds). The harness writes that JSON directly
into the app's data container so the app auto-connects on launch, bypassing the
SpringBoard "Open in app?" deep-link confirmation that can't be scripted.

## Why this exists

`JCodeKit` (the platform-free client core) is fully unit-tested with `swift
test`. This harness adds the layer above that: it proves the real SwiftUI app,
running in a simulator, connects over a real WebSocket and renders a real
transcript. Together they make client behavior hill-climbable without a device.

## Measuring + improving the UI (efficiency reward)

"This looks ugly" is turned into a single hill-climbable number.

- **`ui_metrics.py`** - pixel-level scorer for one screenshot (space,
  consistency, legibility, rhythm) with `--annotate` overlays.
- **`ui_lint.py`** - source-level design-token discipline (hardcoded colors /
  fonts / off-grid spacing that bypass `Theme`).
- **`ui_matrix.py`** - renders the app across content scenarios
  (`empty,short,tool,long,code`) x devices x Dynamic Type sizes, scores each
  cell, reports a mean + worst cell. The mean is the hill to climb.
  - **Devices**: defaults to `iPhone 17` (large, 3x) plus
    `iPhone SE (3rd generation)` (small, 2x), so layout robustness is measured
    against real width/height pressure. Override with `--devices`.
  - **Dynamic Type**: the primary device is re-run at `accessibility-large`
    (via `simctl ui <dev> content_size`) so text-scaling breakage shows up in
    the matrix. Tune or disable with `--a11y-size ""`.
  - **Runtime perf**: each cell records best-effort runtime metrics in the
    schema `reward/scorers/perf.py` consumes: `cold_launch_ms` (wall time of
    `simctl launch` on a fresh install, i.e. a true cold launch) and
    `first_frame_ms` (screenshot polling until the app's background dominates
    the screen). Measurements include harness overhead, so treat them as
    consistent relative signals, not absolute truth. If measurement fails the
    cell omits `runtime` and the perf scorer degrades to unavailable
    (weights renormalize; the reward is never tanked by missing data).
    Skip with `--no-perf`. Scroll-jank capture is not implemented yet;
    `scroll_jank_frac` stays absent.
- **`reward/`** - the full UX reward framework. 13 scorers across 5 weighted
  categories (A space .30, B ergonomics .25, C clarity .20, D legibility/a11y
  .15, E responsiveness .10) aggregate into one 0-100 reward with a
  worst-category callout. See `reward/REWARD_SPEC.md`.

Typical loop:

```bash
# 1. capture a screenshot matrix + score it
python3 ui_matrix.py --json > /tmp/before.json
python3 -m reward.aggregate --matrix-json /tmp/before.json --out-json /tmp/before_reward.json

# 2. make a UI change, rebuild, re-measure
python3 ui_matrix.py --json > /tmp/after.json
python3 -m reward.aggregate --matrix-json /tmp/after.json --out-json /tmp/after_reward.json

# 3. gate: only keep the change if reward did not regress
python3 -m reward.aggregate --baseline /tmp/before_reward.json --candidate /tmp/after_reward.json

# scorers must stay pure/deterministic:
python3 -m reward.test_determinism
```

Adding a category is a one-file drop-in under `reward/scorers/` that satisfies
the contract (`NAME`, `CATEGORY`, `WEIGHT`, `score(ctx) -> CategoryScore`); the
aggregator discovers it automatically.
