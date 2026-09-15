# DrAgnes -- Dermatology Intelligence Platform

AI-powered dermoscopy analysis with collective learning, built on the RuVector
embedding engine and pi.ruv.io brain network.

## Quick Start

```bash
cd examples/dragnes
npm install
npm run dev
```

The app starts at `http://localhost:5173`.

## Architecture

DrAgnes is a standalone SvelteKit application that provides:

- **On-device CNN inference** via MobileNetV3-Small (WASM) for dermoscopy image
  classification across 7 HAM10000 lesion classes (akiec, bcc, bkl, df, mel,
  nv, vasc).
- **ABCDE scoring** with asymmetry, border, color, diameter, and evolution
  metrics.
- **Grad-CAM attention overlays** showing which regions influenced the
  classification.
- **HAM10000 demographic adjustment** using Bayesian priors from clinical data
  (age, sex, body location).
- **Privacy-first design** with differential privacy (epsilon=1.0), EXIF
  stripping, and local-only processing by default.
- **Brain sync** via pi.ruv.io for federated learning and collective case
  sharing (opt-in).
- **Offline PWA** with service worker caching for model weights and static
  assets.

## Directory Structure

```
src/
  lib/
    dragnes/       # Core library: classifier, types, preprocessing, privacy
    components/    # Svelte UI components: capture, results, charts, panel
  routes/
    +page.svelte   # Main page (loads DrAgnesPanel)
    api/           # Server endpoints: health, analyze, feedback, similar
static/            # PWA manifest, icons, service worker
tests/             # Unit and benchmark tests
scripts/           # Deployment and analysis scripts
docs/              # Research documentation
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/health` | Health check with model info |
| POST | `/api/analyze` | Classify an image embedding |
| POST | `/api/feedback` | Submit clinician feedback |
| GET | `/api/similar/[id]` | Find similar cases |

## Testing

```bash
npm run test
```

## Deployment

```bash
# Build for production
npm run build

# Deploy to Cloud Run
npm run deploy
```

See `cloud-run.yaml` for the Cloud Run service configuration and `Dockerfile`
for the container build.

## Related

- [ADR-117: DrAgnes Architecture](../../docs/adr/)
- [HAM10000 Analysis](docs/HAM10000_analysis.md)
- [HIPAA Compliance](docs/hipaa-compliance.md)
