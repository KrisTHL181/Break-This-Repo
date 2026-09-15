# @metabiohacker/core — Multimodal Ingest + Fusion + Ledger (V0)

The medical signal operating system around the frozen `sonic_ct` acoustic
engine. **No real patient data, no diagnosis, no claims** — this proves the
pipeline can ingest typed artifacts, canonicalise them, build priors, fuse them,
and score whether multimodal context improves reconstruction, with full
provenance and a verifiable audit trail.

## Layers

```
typed artifacts (CSV/DICOM-sidecar/JSON)
  └─ ingest adapters ─▶ canonical MedicalObservation (provenance + uncertainty + consent)
       └─ patient state graph + rule-based contradiction detection
            └─ prior builder ─▶ reconstruction priors (never force a conclusion)
                 └─ Darwin fusion harness (mapLimit + paretoFront) selects a fusion policy
                      └─ evidence layer (ruvn): A/B-or-blocked claim gate, off the hot path
                           └─ reconstruction run ledger (stable hashes) ─▶ safe UI packet
```

The Rust acoustic engine is **frozen**; only the harness around it evolves.
External medical data changes priors, confidence, routing, and uncertainty — it
**cannot** force a diagnosis. Pathology/biopsy/Pap/HPV/cytology always force
human review. Claims ship only on ruvn evidence grade **A or B** with citations.

## Run

```bash
npm install
npm test        # 14 tests (ingest, graph, contradictions, fusion, ledger, evidence gate)
npm run benchmark   # acoustic-only vs evolved multimodal fusion + ledger verification
```

Representative benchmark: +10% reconstruction stability, ~37% uncertainty drop,
acoustic residual unchanged, ledger verified, pathology → clinical-review mode.

## Layout

- `src/ingest/` — canonical `MedicalObservation` + lab / imaging / pathology adapters
- `src/graph/` — patient state graph + contradiction detection
- `src/fusion/` — prior builder, scoring, contradiction penalty, Darwin harness
- `src/evidence/` — ruvn evidence provider interface, grading gate, cached + CLI providers
- `src/ledger/` + `src/output/` — verifiable run ledger + safe UI packet

See `docs/sonic-ct/adr/` ADR-0014..0023 for the decisions behind each layer.
Evidence layer is optional and never a hard dependency on `@ruvnet/ruvn`.
