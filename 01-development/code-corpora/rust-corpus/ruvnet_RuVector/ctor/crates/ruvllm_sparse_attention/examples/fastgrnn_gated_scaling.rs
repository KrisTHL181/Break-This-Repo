//! Demonstrates FastGRNN-gated sparse attention at near-linear scale.
//!
//! Runs `forward` and `forward_gated_with_fastgrnn` across a sweep of
//! sequence lengths and prints wall-clock timings. The gated path's
//! candidate set is bounded by `window + globals + gate_top_k`, which
//! is constant in `seq`, so its runtime grows ~linearly. The plain
//! `forward` adds log-stride candidates, so it grows as `O(N · log N)`.
//!
//! Run with:
//!   cargo run -p ruvllm_sparse_attention --example fastgrnn_gated_scaling --release

use ruvllm_sparse_attention::{
    AttentionBackend, FastGrnnGate, SparseAttentionConfig, SubquadraticSparseAttention, Tensor3,
};
use std::time::Instant;

fn fill_random(t: &mut Tensor3, mut seed: u32) {
    let mut next = || {
        seed ^= seed << 13;
        seed ^= seed >> 17;
        seed ^= seed << 5;
        (seed as f32 / u32::MAX as f32 - 0.5) * 0.5
    };
    for i in 0..t.seq {
        for h in 0..t.heads {
            for d in 0..t.dim {
                t.row_mut(i, h)[d] = next();
            }
        }
    }
}

fn main() {
    let cfg = SparseAttentionConfig {
        window: 32,
        block_size: 16,
        global_tokens: vec![0, 1],
        causal: true,
        use_log_stride: true,
        use_landmarks: true,
        sort_candidates: false,
    };
    let attn = SubquadraticSparseAttention::new(cfg.clone()).unwrap();
    let heads = 4;
    let dim = 32;
    let gate = FastGrnnGate::new(dim, 16);
    // Tight gating budget — at seq ≥ 1024, log_stride ≈ 10+ candidates
    // per position, so gate_top_k=8 demonstrates the linear regime.
    let gate_top_k = 8;

    println!(
        "{:>6} | {:>8} | {:>8} | {:>10} | {:>10} | {:>6}",
        "seq", "ungated_ms", "gated_ms", "ungated/N", "gated/N", "speedup"
    );
    println!("{}", "-".repeat(64));

    for &seq in &[128, 256, 512, 1024, 2048] {
        let mut q = Tensor3::zeros(seq, heads, dim);
        let mut k = Tensor3::zeros(seq, heads, dim);
        let mut v = Tensor3::zeros(seq, heads, dim);
        fill_random(&mut q, 0x1234);
        fill_random(&mut k, 0x5678);
        fill_random(&mut v, 0x9abc);

        // Warm-up to amortize page faults / cache fill.
        let _ = attn.forward(&q, &k, &v).unwrap();
        let _ = attn
            .forward_gated_with_fastgrnn(&q, &k, &v, &gate, gate_top_k)
            .unwrap();

        let t0 = Instant::now();
        let _ = attn.forward(&q, &k, &v).unwrap();
        let ungated_ms = t0.elapsed().as_secs_f64() * 1000.0;

        let t0 = Instant::now();
        let _ = attn
            .forward_gated_with_fastgrnn(&q, &k, &v, &gate, gate_top_k)
            .unwrap();
        let gated_ms = t0.elapsed().as_secs_f64() * 1000.0;

        let per_n_un = ungated_ms / seq as f64;
        let per_n_g = gated_ms / seq as f64;

        println!(
            "{:>6} | {:>8.2} | {:>8.2} | {:>10.4} | {:>10.4} | {:>5.2}x",
            seq,
            ungated_ms,
            gated_ms,
            per_n_un,
            per_n_g,
            ungated_ms / gated_ms
        );
    }

    println!();
    println!("Near-linear: gated/N stays roughly flat across sequence lengths");
    println!(
        "(gate_top_k = {} is constant in seq), while ungated/N grows",
        gate_top_k
    );
    println!("logarithmically with the log-stride candidate scheme.");
}
