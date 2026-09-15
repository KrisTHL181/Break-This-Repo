# sky-monitor — RuView SkyGraph Appliance core (ADR-199, Phases 1–4)

> **See the sky. Remember the sky. Explain the sky.**

A local sky-monitoring appliance core that observes, projects, records, and
explains activity above a fixed observer (the reference *Oakville node*,
43.4675 N / −79.6877 E / 100 m). The sky is treated as a continuously changing
spatial graph, not a dashboard.

Everything runs on a **deterministic synthetic ADS-B + weather scenario** —
no network, no SDR hardware — while a `dump1090` `aircraft.json` parser keeps
the door open for real RTL-SDR data. Vectors live in
[`ruvector-core`](../../crates/ruvector-core) (`VectorDB`), the SkyGraph in
[`ruvector-graph`](../../crates/ruvector-graph) (`GraphDB`).

## Module map → ADR-199 sections

| Module | ADR-199 | What it does |
|--------|---------|--------------|
| `config` | §30 | `ObserverConfig` (Oakville node defaults), `AnomalyConfig` (§15 weights, 0.76 alert threshold, baseline `min_history`) |
| `coords` | §10 | WGS-84 → ECEF → ENU → azimuth/elevation/range/bearing (`ObserverFrame`), pure `f64` |
| `observation` | §11 | Canonical observation schema (uuid, UTC time, entity, location, observer frame, motion, attributes, confidence, `raw_ref`/`embedding_ref`) |
| `adsb` | §9.1, Phase 1 | Seeded synthetic scenario (corridor / arrivals / GA overhead / one anomalous night track) + `parse_dump1090` for real data |
| `track` | §19 skygraph-builder | Gap-based track stitching, summary stats, §14 rule 1 overhead candidates |
| `weather` | §9.3, Phase 2 | Synthetic hourly `WeatherWindow`s aligned to the timeline (incl. the sample-brief rain band) |
| `embedding` | §13 | Deterministic 32-dim track embeddings + 8-dim weather embeddings (separate collections), every dimension documented |
| `indexer` | §19 ruvector-indexer, Phase 4 | `VectorDB` wrapper: similarity search + calibrated novelty score |
| `skygraph` | §12, Phase 3 | `GraphDB` nodes (Observer/Aircraft/Track/Observation/WeatherCell/TimeWindow/Anomaly) + §12 edge vocabulary; time-window / overhead queries; citeable `explain()` |
| `anomaly` | §15 | Exact composite formula, interpretation bands, mandatory reasons (§27 rule 2) |
| `brief` | §21.3 | Daily sky brief with `Display` text block |
| `pipeline` | §22 | One `Pipeline::run()` shared by demo, tests, and benches |

## Run

```bash
# demo (from the repository root)
cargo run -p sky-monitor --release

# demo + JSON export of the synthetic day (writes `const SKY_DATA = {...};` for .js paths)
cargo run -p sky-monitor --release -- --emit-json target/sky-demo-data.js

# acceptance + unit tests (mapped to ADR-199 §31 / §22)
cargo test -p sky-monitor

# criterion benches (projection, embedding, VectorDB, anomaly, end-to-end)
cargo bench -p sky-monitor            # full run
cargo bench -p sky-monitor -- --test  # smoke mode
```

## Feature flags

| Feature | Default | Contents |
|---------|---------|----------|
| `appliance` | **yes** | The heavy native stores — `ruvector-core` (VectorDB) and `ruvector-graph` (GraphDB) — and the modules built on them: `indexer`, `skygraph`, `pipeline`, plus the demo binary, the acceptance tests, and the benches |
| *(none)* | — | `--no-default-features` leaves the pure subset: `config`, `coords`, `observation`, `adsb`, `track`, `weather`, `embedding`, `anomaly`, `brief` — this compiles for `wasm32-unknown-unknown` and is what `sky-monitor-wasm` builds on |

```bash
# verify the wasm-compatible subset
cargo build -p sky-monitor --no-default-features --target wasm32-unknown-unknown
```

## WASM projection engine (`wasm/` → `sky-monitor-wasm`)

Browser-facing bindings for the ADR-199 presentation plane ("dashboard
first"), wrapping the pure subset with `wasm-bindgen`:

* `SkyProjector` — §10 WGS-84 → az/el/range/bearing projection, single
  (`project`) and batched (`project_batch`, `Float64Array` of `[lat,lon,alt]`
  triplets → `[az,el,range,bearing]` quadruplets for fast trail rendering),
  plus `screen_position` — the polar "fisheye" all-sky mapping (zenith at the
  canvas centre, horizon at the edge, azimuth 0° = North = up).
* `AnomalyScorer` — `baseline_from(tracksJson)` + `score(trackJson, novelty)`
  over track summaries (`{icao24, callsign, mean_alt_m, dominant_heading_deg,
  start_hour, mean_signal_dbfs, min_range_m, max_elevation_deg}`), reusing the
  exact core §15 scorer (`anomaly::BaselineStats::from_summaries` /
  `anomaly::score_summary`) so browser scores match the native pipeline.
* `SatPropagator` — SGP4 satellite propagation from TLEs (`sgp4` crate):
  TEME → GMST-rotated ECEF → geodetic (Bowring) → the same §10 observer
  frame, batched per frame for the dashboard's satellite layer; plus
  `predict_passes(start, hours, step)` — a 24 h pass timeline (rise /
  culmination / set / max elevation per pass, with a low-precision Rust sun
  model flagging naked-eye-visible passes: sunlit satellite, sun < −6°).
* `embed_track` / `novelty` (`embed.rs`) — the §13 32-dim track embedding
  from live points (per-point motion derived by finite differences, then the
  canonical `embedding::track_embedding_from_samples`) and the §15
  vector-novelty score with the native indexer calibration
  (mean top-3 distance / 1.2, neutral 0.5 with no priors).
* `parse_dump1090_json` — the core dump1090 parser for live feeds, and
  `band_for(score)` / `version()` helpers.

```bash
cargo test -p sky-monitor-wasm                                      # native unit tests (screen mapping)
cargo build -p sky-monitor-wasm --target wasm32-unknown-unknown --release
wasm-pack build examples/sky-monitor/wasm --target web --out-dir ../ui/dashboard/pkg  # for the dashboard
```

## Canvas dashboard (`ui/dashboard/`)

Vanilla JS + Canvas, no build tooling (see `ui/dashboard/README.md`): a
**realtime** all-sky polar plot (elevation rings at 0/30/60°, compass labels)
showing live ADS-B aircraft (airplanes.live primary, adsb.lol fallback;
key-free, CORS-friendly) as labelled dots with fading trails and smoothed
dead-reckoned motion between polls, Open-Meteo weather + NOAA SWPC Kp in the
weather card, and a side panel with the live track table, per-track details,
and a 24 h naked-eye satellite **pass timeline** (wasm `predict_passes`,
optional Notification alerts). A satellite layer (CelesTrak `visual` /
`stations` / `starlink` TLEs + wasm SGP4) draws satellites as diamonds —
flagging sunlit-against-dark-sky passes ✦ — with an experimental **WebGPU**
instanced-sprite path (drawer toggle, automatic Canvas2D fallback). Live
tracks are scored through the wasm §15 `AnomalyScorer` with **real §13
vector novelty** (wasm `embed_track` + IndexedDB rolling store of past track
embeddings), annotated with **behavior badges** (holding / survey grid /
go-around / formation) and pairwise **CPA conflict prediction** (< 1 km &
< 300 m within 90 s → dashed alert line + predicted-path cone), and enriched
with readsb metadata plus **adsbdb routes** (airline, origin → destination,
on selection, 24 h cache). A footer scrubber **replays the last hour of
recorded real traffic** from an IndexedDB ring buffer (no synthetic data).
There is no embedded scenario; offline, the dome stays up with a retrying
status line. Projection runs in JS by default and switches to
`sky-monitor-wasm` automatically when the wasm-pack output is present at
`ui/dashboard/pkg/` (satellites, scoring, novelty, and passes require it).

```bash
cd examples/sky-monitor/ui/dashboard && python3 -m http.server 8000
# open http://localhost:8000/
```

## Sample output (trimmed)

```text
RuView SkyGraph Appliance — synthetic demo (ADR-199 Phases 1-4)
Observer: oakville_node (43.4675, -79.6877, 100 m) | seed 42 | 2820 observations

== Tracks (observer-relative at closest approach) ==
track            call     range_km  az_deg  el_deg    alt_m     hdg speed_mps  overhead
track-c01a01-0   ACA101       13.2     162    52.6    10600      72       236
track-c07e07-0   JZA707        7.0     121    30.6     3679      32       145  yes
track-c0a9a9-0   CGSKY         1.1     187    67.8     1100      88        62  yes
track-deadbf-0   -             2.0     254     9.9      450     165        48  yes
...

== SkyGraph ==
nodes: 109   edges: 111
overhead candidates: ["track-c07e07-0", "track-c08f08-0", "track-c0a9a9-0", "track-deadbf-0"]

== Top similar-track pairs (RuVector, euclidean) ==
  track-a03c03-0 <-> track-c01a01-0   distance 0.378
  track-c01a01-0 <-> track-c04d04-0   distance 0.448

== Anomaly scores (ADR-199 §15) ==
track            call     score  band             reasons
track-c04d04-0   WJA404   0.165  normal           within normal envelope: heading 73°, ...
track-c0a9a9-0   CGSKY    0.570  interesting      mean altitude 1100 m deviates 2.9σ ...
track-deadbf-0   -        0.860  strong anomaly   heading 165° is 77° off the nearest known
                                                  corridor | mean altitude 450 m deviates
                                                  2.0σ | start time 03:xx UTC has 0 prior
                                                  tracks within ±2 h | signal -3.0 dBFS is
                                                  3.3σ | vector novelty 1.00 | no callsign

== Explain track-deadbf-0 (strong anomaly, action: local alert) ==
  - track track-deadbf-0 stitched from 420 observations; evidence observation ids:
    first 1964af06-..., closest approach 6a29bba3-..., last 46473de0-...
  - geometry: closest approach 2034 m at azimuth 254°, max elevation 10.2°, ...
  - near observer:oakville_node (closest approach inside 10 km)
  - during window:2026-06-09T03
  - anomalous_relative_to baseline track-c0a9a9-0
  - correlated_with weather:2026-06-09T03 (clear, wind 2.8 m/s)

== Daily sky brief (ADR-199 §21.3) ==
Sky brief — oakville_node, 2026-06-08. 10 aircraft observed; 4 overhead candidates;
2 unusual tracks. Weather: rain 14:00–16:00 UTC. Most unusual event: low-altitude
pass by icao24 deadbf heading 165° at 03:13 UTC (450 m): heading 165° is 77° off
the nearest known corridor (confidence 0.86).
```

## Synthetic scenario

One day over the observer, seed-deterministic (`Pipeline::default()`, seed 42):

* 4 eastbound + 2 westbound **en-route corridor** flights (~072°/252°,
  10.5–11.2 km, ~230 m/s),
* 2 **arrivals** descending through the area (~032°, 4.8 km → 2.6 km),
* 1 low **general-aviation overhead pass** (1.1 km, within 1.1 km slant range),
* 1 **anomalous track**: 450 m, 48 m/s, heading 165° (off-corridor), 03:10 UTC
  the following night, unusually strong signal, no callsign — scores **0.86 →
  strong anomaly → local alert**, while scored corridor flights stay ≤ 0.23.

The first `min_history` (5) tracks form the unscored baseline; later tracks are
scored against strictly prior tracks (ADR §26: baseline before alerting).

## What is deliberately out of scope here

Phase 5 sensors (audio/RF/camera — `cross_sensor_confirmation` is a documented
placeholder at 0), live dump1090/OpenSky ingestion (the parser and the
browser-side `parse_dump1090_json` exist, but nothing polls a receiver),
retention / hash-chained raw archive, and the NL assistant service.
