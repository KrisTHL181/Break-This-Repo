# Training Scripts

Scripts for RuvLTRA model training, evaluation, and release gating.

## release_gate.py

Automated ship/no-ship checker implementing the 7 release gates from [ADR-129](../../docs/adr/ADR-129-ruvltra-gcloud-training-turboquant.md) Section 3.2. No external dependencies -- uses Python stdlib only.

### Prerequisites

Generate a `gate_results.json` file by running the evaluation scripts (`eval_humaneval.py`, `eval_routing.py`, `eval_perplexity.py`, `turbo_quant_bench`, `eval_long_context.py`, `e2e_bench`). The file must be placed in a results directory with the following structure:

```json
{
  "model_size": "0.5B",
  "baseline": {
    "humaneval_pass1": 0.40,
    "routing_accuracy": 0.80,
    "wikitext2_ppl": 25.0
  },
  "candidate": {
    "humaneval_pass1": 0.48,
    "routing_accuracy": 0.83,
    "wikitext2_ppl": 24.5,
    "tq_compression": 10.7,
    "tq_ppl_delta": 0.008,
    "long_context_ppl": 18.0,
    "contamination_count": 0,
    "tok_per_sec": 95
  }
}
```

### Usage

```bash
# Basic usage
python scripts/training/release_gate.py --results-dir ./results

# With model path (informational)
python scripts/training/release_gate.py \
  --model-path /models/ruvltra-v2.0-tq \
  --results-dir ./results

# Save JSON report
python scripts/training/release_gate.py \
  --results-dir ./results \
  --output-json ./reports/gate_report.json
```

### Exit codes

| Code | Meaning |
|------|---------|
| `0`  | All 7 gates PASS -- model is approved to ship |
| `1`  | One or more gates FAIL -- do not ship |

### Gates

| Gate | Criterion | 0.5B threshold | 3B threshold |
|------|-----------|---------------|-------------|
| G1 | HumanEval pass@1 | >=45% or >=5pp delta | >=55% or >=5pp delta |
| G2 | Routing accuracy | >=80% | >=80% |
| G3 | Wikitext-2 PPL regression | <5% increase | <5% increase |
| G4 | TurboQuant compression | >=8x, PPL delta <1% | >=8x, PPL delta <1% |
| G5 | Long context PPL at 16K | <20 PPL | <20 PPL |
| G6 | Eval contamination | 0 instances | 0 instances |
| G7 | Inference speed | >=80 tok/s | >=40 tok/s |

### CI integration

```yaml
# In a GitHub Actions workflow or Cloud Build step:
- name: Release gate check
  run: python scripts/training/release_gate.py --results-dir ./results --output-json ./reports/gate_report.json
```

If any gate fails, the script exits with code 1, which fails the CI step and blocks publishing.
