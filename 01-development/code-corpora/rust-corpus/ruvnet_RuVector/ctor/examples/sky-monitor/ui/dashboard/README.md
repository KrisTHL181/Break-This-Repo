# SkyGraph all-sky dashboard (ADR-199 presentation plane)

Vanilla JS + Canvas, no build tooling. **Realtime**: polls live ADS-B traffic
(airplanes.live primary, adsb.lol fallback — key-free, CORS-friendly) every
5 s and Open-Meteo weather every 10 min around the fixed observer, and renders
it on a polar all-sky plot: zenith at the centre, horizon at the edge, azimuth
0° = North = up. Aircraft are dots with callsign + altitude labels and fading
trails; between polls the dots glide on smoothed dead reckoning. There is no
embedded scenario — when no ADS-B source is reachable, the dome stays up and
the status line reports offline/retrying.

**Satellite layer** (`sat-feed.js`): CelesTrak TLEs (ACAO `*`, localStorage
6 h cache per group) propagated per frame with SGP4 in `sky-monitor-wasm`.
The ⚙ drawer selects the group — `visual` (~150 brightest, default),
`stations`, or `starlink` (offered only while the WebGPU layer is on).
**Sun & moon** (`astro.js`) render on the dome; sunlit satellites under a
dark sky (sun < −6°) are flagged **✦ visible now**.

**Real §15 anomaly scoring with vector novelty** (`score-live.js` +
`novelty.js`): every live track is embedded through the wasm §13 32-dim
`embed_track` (canonical `embedding.rs` normalization; per-point motion
derived in Rust) and scored against a rolling IndexedDB store of past track
embeddings (cap ~5 000, oldest pruned) with the indexer-calibrated `novelty`
(mean top-3 distance / 1.2). The §15 composite then runs in the wasm
`AnomalyScorer` with that real novelty term — dots/trails/rows take the band
color and the details panel shows score, reasons, and the novelty line.

**Behavior badges** (`behavior.js`, pure functions): holding patterns
(net ≪ path), survey grids (parallel legs + 180° reversals), go-arounds
(descent < 600 m then sustained climb), and formation pairs (< 1 km, matched
vector) — `[HOLD]`/`[GRID]`/`[GO-AROUND]`/`[FORM]` in the table + details.

**Conflict prediction** (`conflict.js`): pairwise CPA in observer ENU — alert
when predicted separation < 1 km horizontal AND < 300 m vertical within 90 s.
Conflicting pairs get a dashed red line + ⚠ status; the selected aircraft
shows a turn-aware predicted-path cone (heading rate from recent fixes).

**Satellite pass timeline** (`passes.js`): wasm `predict_passes` steps SGP4
24 h ahead (30 s grid, sun model in Rust) and the side panel lists the next
naked-eye passes (rise–set local times, max el, direction); a drawer button
arms a Notification ~5 min before each visible pass (permission-gated).

**Route enrichment** (`route-info.js`): on selection only, the callsign is
looked up at adsbdb (probed 2026-06-10: ACAO `*`; 404 = unknown callsign) —
airline + origin → destination in the details panel, 24 h localStorage cache.

**Space weather** (`space-wx.js`): NOAA SWPC planetary Kp (probed 2026-06-10:
ACAO `*`), polled 15 min, shown in the weather card with an aurora hint for
43°N at Kp ≥ 7.

**WebGPU sats (experimental)** (`gpu-sats.js`): a drawer toggle moves the
satellite layer to instanced point sprites on a transparent overlay canvas —
the path that scales to starlink (~7 000 dots). Automatic fallback to
Canvas2D when `navigator.gpu` is missing or init fails; Canvas2D stays the
default.

**Recorded replay** (`record.js`): live projected points stream into an
IndexedDB ring buffer (~1 h). The footer ⏪ button + scrubber re-render the
dome at any past wall-clock t from that buffer (same drawTrack path); LIVE
returns to now. This replays **real recorded traffic** — the synthetic-day
replay was deliberately removed.

The **⚙ drawer** (top right) toggles layers (aircraft / satellites /
sun & moon / trails / labels / conflict alerts) and settings (trail length,
TLE group, WebGPU sats, pass alerts); choices persist in localStorage.

## Serve

```bash
# from this directory (ES modules need http://, not file://)
python3 -m http.server 8000
# open http://localhost:8000/
```

## Optional: wasm projection engine

`sky.js` does the WGS-84 → az/el/range projection in plain JS (mirroring
`src/coords.rs`). If the wasm-pack output exists at `./pkg/`, it is detected
and preferred automatically:

```bash
# from the repository root
wasm-pack build examples/sky-monitor/wasm --target web --out-dir ../ui/dashboard/pkg
```

The header shows which engine is active. Without `./pkg` the dashboard still
renders live traffic on the JS fallback; the satellite layer, §15 scoring,
vector novelty, and pass prediction require the wasm pkg.

## Tests

Pure detector logic (behavior, CPA) runs under node:

```bash
node --test test/behavior.test.mjs test/conflict.test.mjs
```

## Module map

`sky.js` (conductor) · `draw.js` (dome/track/cone primitives) ·
`settings.js` (⚙ drawer) · `panels.js` (details + sat table) ·
`live-feed.js` / `sat-feed.js` / `space-wx.js` / `route-info.js` (data) ·
`novelty.js` / `behavior.js` / `conflict.js` / `passes.js` (intelligence) ·
`record.js` (replay) · `gpu-sats.js` (WebGPU) · `project.js` / `astro.js`
(math) · `score-live.js` (§15 bridge). Every file stays under 500 lines.
