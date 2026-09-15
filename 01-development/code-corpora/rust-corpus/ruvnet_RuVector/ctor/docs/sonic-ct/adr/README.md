# MetaBioHacker / sonic_ct — Architecture Decision Records

| # | Decision | Theme |
|---|----------|-------|
| [0001](ADR-0001-simulation-first.md) | Simulation-first | Engine |
| [0002](ADR-0002-hardware-backend-trait.md) | Hardware backend trait | Engine |
| [0003](ADR-0003-preserve-raw-rf-before-ai.md) | Preserve raw RF before AI | Provenance |
| [0004](ADR-0004-delay-backprojection-baseline.md) | Delay-backprojection / SART baseline | Reconstruction |
| [0005](ADR-0005-medical-claims-boundary.md) | Medical claims boundary | Governance |
| [0006](ADR-0006-dicomweb-fhir-adapters.md) | DICOMweb / FHIR as adapters | Standards |
| [0007](ADR-0007-uncertainty-first-ai.md) | Uncertainty-first AI | Safety |
| [0008](ADR-0008-gpu-later.md) | GPU later (CPU/WASM first) | Performance |
| [0009](ADR-0009-five-acoustic-classes-canonical.md) | Five acoustic classes canonical | Reconstruction |
| [0010](ADR-0010-organ-identity-from-priors.md) | Organ identity from anatomical priors | Inference |
| [0011](ADR-0011-function-requires-dynamic-channels.md) | Function needs dynamic channels | Inference |
| [0012](ADR-0012-explainability-mandatory.md) | Explainability mandatory | Safety |
| [0013](ADR-0013-no-disease-labels-research-mode.md) | No disease labels (research mode) | Governance |
| [0014](ADR-0014-freeze-physics-evolve-harness.md) | Freeze physics, evolve harness | Optimization |
| [0015](ADR-0015-patient-state-graph.md) | Patient state graph of typed observations | Data model |
| [0016](ADR-0016-medical-standards-architecture.md) | Medical standards (DICOM/FHIR/LOINC/SNOMED/OMOP) | Standards |
| [0017](ADR-0017-multimodal-fusion-patterns.md) | Typed multimodal fusion patterns | Data fusion |
| [0018](ADR-0018-governance-samd-boundary.md) | Governance & SaMD boundary | Governance |
| [0019](ADR-0019-medical-signal-operating-system.md) | Medical signal operating system | Architecture |
| [0020](ADR-0020-multimodal-canonical-observation.md) | Canonical observation ingest boundary | Data model |
| [0021](ADR-0021-patient-state-graph-contradictions.md) | Patient state graph + contradiction detection | Audit |
| [0022](ADR-0022-reconstruction-run-ledger.md) | Reconstruction run ledger (reproducibility) | Audit |
| [0023](ADR-0023-ruvn-evidence-layer.md) | ruvn evidence layer (claim gate) | Governance |
| [0024](ADR-0024-real-slice-calibration-honesty-gate.md) | Real-slice calibration + domain-gap honesty gate | Benchmark |
| [0025](ADR-0025-method-comparison-standard-metrics.md) | Method comparison (BP/SART/Landweber) + RMSE/PSNR/SSIM | Benchmark |
| [0026](ADR-0026-full-waveform-inversion.md) | Full-waveform inversion (forward + adjoint gradient) | Reconstruction |

Design principle (ADR-0019): a *medical signal operating system*, not an "AI
doctor" — frozen physics engines + deterministic validators at the core, an
evolving harness around them (ADR-0014), and an uncertainty-aware patient state
graph as output (ADR-0015), with provenance, consent scope, and a human-review
path for any clinical claim (ADR-0018).
