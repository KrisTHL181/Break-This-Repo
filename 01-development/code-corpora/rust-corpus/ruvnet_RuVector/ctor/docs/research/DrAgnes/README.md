# DrAgnes: AI-Powered Dermatology Intelligence Platform

**Status**: Research & Planning
**Date**: 2026-03-21
**Project**: RuVector DrAgnes Initiative

## Vision

DrAgnes is an AI-powered dermatology intelligence platform that transforms skin lesion detection, classification, and clinical decision support. Built on the RuVector cognitive substrate, it combines DermLite dermoscopic imaging hardware with MobileNetV3 CNN classification, pi.ruv.io collective brain learning, and the RuVocal chat interface to create a system that learns from every diagnosis, improves with every practice that adopts it, and operates with full HIPAA compliance.

The name "DrAgnes" is a portmanteau of "Dermatology" + "Agnes" (from the Greek "hagne," meaning pure/sacred) -- reflecting the platform's commitment to pure, evidence-based diagnostic intelligence.

## Why This Matters

- **Melanoma kills 8,000 Americans annually**. Early detection reduces mortality by 90%. Yet dermatologist wait times average 35 days in the US, and rural areas have virtually no access to dermatoscopy expertise.
- **Existing AI tools are static**. They train once on a fixed dataset and never improve. DrAgnes learns continuously from every participating practice while preserving patient privacy through differential privacy and PII stripping.
- **Dermoscopy is underutilized**. Only 48% of US dermatologists use dermoscopy regularly. DrAgnes makes dermoscopic analysis accessible to primary care physicians, nurse practitioners, and telemedicine providers via a simple phone camera + DermLite adapter.

## Core Differentiators

1. **Collective Intelligence**: Every diagnosis enriches the pi.ruv.io brain's knowledge graph. De-identified embeddings (never raw images) flow into a shared model that benefits all practices. A rural clinic in Montana learns from a university hospital in Boston without ever seeing a patient record.

2. **Offline-First Architecture**: The WASM-compiled CNN runs entirely in the browser. No internet required for classification. The brain syncs opportunistically when connectivity is available.

3. **Provenance & Trust**: Every diagnostic suggestion carries a SHAKE-256 witness chain proving which model version, which training data epoch, and which knowledge graph state produced it. Full reproducibility for clinical audits.

4. **Practice-Adaptive Learning**: SONA MicroLoRA (rank-2) adapts the base model to each practice's patient population using EWC++ regularization to prevent catastrophic forgetting. A practice in equatorial Africa sees different lesion distributions than one in Scandinavia -- DrAgnes adapts accordingly.

5. **DermLite-Native**: Purpose-built integration with DermLite HUD, DL5, DL4, and DL200 devices. Supports polarized and non-polarized dermoscopy, contact and non-contact imaging, and automated ABCDE scoring.

## Platform Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| Frontend | SvelteKit + TailwindCSS (RuVocal) | PWA with camera API, offline support |
| CNN Engine | ruvector-cnn (MobileNetV3 Small) | 576-dim embeddings, INT8 quantized |
| WASM Runtime | ruvector-cnn-wasm | Browser-native inference, SIMD128 |
| Knowledge Graph | pi.ruv.io brain | 1,500+ memories, 316K edges, PubMed |
| Search | HNSW + PiQ3 quantization | Sub-millisecond nearest neighbor |
| Learning | SONA MicroLoRA + EWC++ | Online adaptation per practice |
| Privacy | PII stripping + differential privacy | epsilon=1.0, HIPAA compliant |
| Provenance | Witness chains (SHAKE-256) | Audit trail for every prediction |
| Storage | Firestore + GCS + RVF containers | De-identified metadata only |
| Deployment | Google Cloud Run + Firebase Hosting | Multi-region, auto-scaling |

## Research Documents

| Document | Description |
|----------|-------------|
| [Architecture](architecture.md) | System architecture, data flow, integration points |
| [HIPAA Compliance](hipaa-compliance.md) | Privacy, security, regulatory compliance strategy |
| [Data Sources](data-sources.md) | Training datasets, medical literature, enrichment sources |
| [DermLite Integration](dermlite-integration.md) | Device capabilities, image capture, dermoscopic analysis |
| [Future Vision](future-vision.md) | 25-year forward roadmap (2026-2051) |
| [Competitive Analysis](competitive-analysis.md) | Market landscape and DrAgnes differentiation |
| [Deployment](deployment.md) | Google Cloud deployment plan, cost model |

## Related ADRs

- **ADR-117**: DrAgnes Dermatology Intelligence Platform (this project)
- **ADR-091**: INT8 CNN Quantization
- **ADR-088**: CNN Contrastive Learning
- **ADR-089**: CNN Browser Demo
- **ADR-116**: Spectral Sparsifier Brain Integration

## Clinical Disclaimer

DrAgnes is designed as a clinical decision support tool, not a diagnostic replacement. All classifications must be reviewed by a qualified healthcare professional. DrAgnes does not provide medical diagnoses and should not be used as the sole basis for clinical decisions. The platform is designed to augment, not replace, dermatological expertise.
