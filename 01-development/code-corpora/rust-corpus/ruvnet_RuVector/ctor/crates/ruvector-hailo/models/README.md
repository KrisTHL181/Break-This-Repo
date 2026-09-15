# ruvector-hailo / models — model artifacts and provenance

Two model paths ship today. Pick whichever matches what you have.

## Path A — CPU fallback (production-deployable, iter 134)

The "ship today" path. Real BERT-6 inference via candle on host CPU
(Cortex-A76 NEON on Pi 5, AVX2 on x86 hosts). NPU stays idle but you
get real semantic vectors end-to-end. ~50-150 ms per embed on Pi 5.

```bash
# 1. Fetch the three HF artifacts (~91 MB total, sha256-pinned)
bash crates/ruvector-hailo-cluster/deploy/download-cpu-fallback-model.sh \
    /var/lib/ruvector-hailo/model

# 2. Build the worker with cpu-fallback enabled
cargo build --release --features cpu-fallback \
    --bin ruvector-hailo-worker \
    --manifest-path crates/ruvector-hailo-cluster/Cargo.toml

# 3. Boot
RUVECTOR_MODEL_DIR=/var/lib/ruvector-hailo/model \
RUVECTOR_WORKER_BIND=0.0.0.0:50051 \
    ./target/release/ruvector-hailo-worker
```

The worker reports `ready=true` on the gRPC health probe as soon as
the safetensors load. `--features hailo` is optional — the cpu-fallback
path doesn't need HailoRT installed.

## Path B — HEF (NPU acceleration, iter 135 — blocked at model surgery)

The Hailo Dataflow Compiler tooling is fully installed and the
parser/optimize/compile pipeline runs end-to-end via
`deploy/compile-hef.sh`. But the standard HuggingFace BERT export hits
two ops that aren't representable in Hailo's HN graph:

- `Gather` for token / token-type embedding lookups (table lookups,
  not real ML ops)
- `Where` / `Expand` for broadcasting the attention mask across QK^T

The recommended surgery (~2-3 days):
1. Pre-compute embeddings host-side: tokenize → embedding-table lookup
   → send `embeddings_out` (shape `[1, 128, 384]` float) to the NPU
2. Re-export the encoder block in isolation with
   `start_node_names=[/embeddings/Add_1]` and `end_node_names=[last_hidden_state]`
3. Apply the attention mask host-side after the encoder
4. Modify `HailoEmbedder::embed` to do tokenize → embed-lookup →
   send-to-NPU → mean-pool → L2-normalize

Documented but not scheduled — Path A covers current throughput needs.

## Tooling install (one-time, x86_64 Linux only)

If you do want to push on Path B:

```bash
# Download from https://hailo.ai/developer-zone/sw-downloads/:
#   * hailort_X.Y.Z_amd64.deb
#   * hailo_dataflow_compiler-X.Y.Z-py3-none-linux_x86_64.whl
# (or the AI Software Suite .run installer which bundles both)

bash crates/ruvector-hailo-cluster/deploy/setup-hailo-compiler.sh ~/Downloads/hailo
bash crates/ruvector-hailo-cluster/deploy/compile-hef.sh --out model.hef
```

The current `compile-hef.sh` uses `compile-hef.py` to drive the SDK
directly (avoids the CLI's `-y` auto-recommendation that picks `/Where`
as an end node). `export-minilm-onnx.py` does a clean `torch.onnx.export`
that avoids optimum-cli's TF/keras dependency hell.

## Expected I/O shapes (Path B once surgery is done)

```
input  embeddings_out   [1, 128, 384]  float32   # host pre-computes
input  attention_mask   [1, 128]       int32     # masking applied host-side
output last_hidden_state [1, 128, 384] float32
```

Pooling (mean over sequence dim, masked by attention) is done host-side
after the NPU emits per-token embeddings — same path as cpu-fallback
uses today, just with the encoder forward pass on the NPU instead of
candle.
