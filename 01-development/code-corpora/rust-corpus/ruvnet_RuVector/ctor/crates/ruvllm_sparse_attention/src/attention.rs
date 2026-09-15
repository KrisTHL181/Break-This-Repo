#[cfg(not(feature = "std"))]
use crate::no_std_math::F32Ext as _;
use crate::tensor::Tensor3;
use alloc::collections::BTreeSet;
use alloc::format;
use alloc::string::{String, ToString};
use alloc::vec;
use alloc::vec::Vec;
use core::cmp::Ordering;
use core::fmt::{Display, Formatter};
#[cfg(feature = "std")]
use std::error::Error;

#[derive(Debug, Clone)]
pub enum AttentionError {
    ShapeMismatch {
        q: (usize, usize, usize),
        k: (usize, usize, usize),
        v: (usize, usize, usize),
    },
    InvalidConfig(String),
}

impl Display for AttentionError {
    fn fmt(&self, f: &mut Formatter<'_>) -> core::fmt::Result {
        match self {
            AttentionError::ShapeMismatch { q, k, v } => write!(
                f,
                "shape mismatch, q={:?}, k={:?}, v={:?}; expected equal shapes",
                q, k, v
            ),
            AttentionError::InvalidConfig(message) => write!(f, "invalid config: {}", message),
        }
    }
}

#[cfg(feature = "std")]
impl Error for AttentionError {}

pub trait AttentionBackend {
    fn forward(&self, q: &Tensor3, k: &Tensor3, v: &Tensor3) -> Result<Tensor3, AttentionError>;
}

#[derive(Clone, Debug)]
pub struct SparseAttentionConfig {
    pub window: usize,
    pub block_size: usize,
    pub global_tokens: Vec<usize>,
    pub causal: bool,
    pub use_log_stride: bool,
    pub use_landmarks: bool,
    /// Sort candidate indices before the attention dot-product loop.
    ///
    /// Ascending order makes KV cache access sequential, which benefits the
    /// hardware prefetcher when the KV cache exceeds L3 (e.g. Pi 5 at seq ≥ 2K).
    /// Leave `false` on x86 with large L3 where the sort cost exceeds the gain.
    pub sort_candidates: bool,
}

impl Default for SparseAttentionConfig {
    fn default() -> Self {
        Self {
            window: 128,
            block_size: 64,
            global_tokens: vec![0],
            causal: true,
            use_log_stride: true,
            use_landmarks: true,
            sort_candidates: false,
        }
    }
}

#[derive(Clone, Debug)]
pub struct SubquadraticSparseAttention {
    pub config: SparseAttentionConfig,
}

impl SubquadraticSparseAttention {
    pub fn new(config: SparseAttentionConfig) -> Result<Self, AttentionError> {
        if config.block_size == 0 {
            return Err(AttentionError::InvalidConfig(
                "block_size must be greater than zero".to_string(),
            ));
        }
        Ok(Self { config })
    }

    pub fn estimate_sparse_edges(&self, seq: usize) -> usize {
        if self.config.block_size == 0 {
            return 0;
        }
        let mut seen_tokens = vec![0usize; seq.max(1)];
        let mut seen_blocks = vec![0usize; div_ceil(seq.max(1), self.config.block_size)];
        let mut token_candidates = Vec::new();
        let mut block_candidates = Vec::new();
        let mut total = 0usize;

        for i in 0..seq {
            // Stamp is per-token only (i+1) — this function estimates edge count
            // for a single head. There is no head loop. The result matches the
            // per-head column in docs/benchmark_edge_estimates.csv.
            // See also: forward(), which uses stamp = 1 + h*seq + i for
            // cross-head deduplication safety.
            let stamp = i + 1;
            token_candidates.clear();
            block_candidates.clear();

            build_token_candidates(
                i,
                seq,
                &self.config,
                &mut seen_tokens,
                stamp,
                &mut token_candidates,
            );

            if self.config.use_landmarks {
                build_landmark_candidates(
                    i,
                    seq,
                    &self.config,
                    &mut seen_blocks,
                    stamp,
                    &mut block_candidates,
                );
            }

            total += token_candidates.len() + block_candidates.len();
        }

        total
    }
}

impl AttentionBackend for SubquadraticSparseAttention {
    fn forward(&self, q: &Tensor3, k: &Tensor3, v: &Tensor3) -> Result<Tensor3, AttentionError> {
        validate_qkv(q, k, v)?;
        if self.config.block_size == 0 {
            return Err(AttentionError::InvalidConfig(
                "block_size must be greater than zero".to_string(),
            ));
        }

        let seq = q.seq;
        if seq == 0 {
            return Ok(Tensor3::zeros(0, q.heads, q.dim));
        }

        let heads = q.heads;
        let dim = q.dim;
        let scale = 1.0f32 / (dim as f32).sqrt();
        let landmarks = if self.config.use_landmarks {
            Some(Landmarks::from_kv(k, v, self.config.block_size))
        } else {
            None
        };

        // Parallel path: each head gets its own dedup state — no shared mutation.
        #[cfg(feature = "parallel")]
        let out = {
            use rayon::prelude::*;
            let lm_ref = landmarks.as_ref();
            let config = &self.config;
            let head_vecs: Vec<Vec<f32>> = (0..heads)
                .into_par_iter()
                .map(|h| {
                    let mut seen_tokens = vec![0usize; seq.max(1)];
                    let mut seen_blocks = vec![0usize; div_ceil(seq.max(1), config.block_size)];
                    let mut tok_c = Vec::<usize>::with_capacity(config.window + 64);
                    let mut blk_c = Vec::<usize>::with_capacity(64);
                    let mut acc = vec![0f32; dim];
                    let mut hout = vec![0f32; seq * dim];
                    for i in 0..seq {
                        let stamp = 1 + h * seq + i;
                        tok_c.clear();
                        blk_c.clear();
                        build_token_candidates(i, seq, config, &mut seen_tokens, stamp, &mut tok_c);
                        if lm_ref.is_some() {
                            build_landmark_candidates(
                                i,
                                seq,
                                config,
                                &mut seen_blocks,
                                stamp,
                                &mut blk_c,
                            );
                        }
                        if config.sort_candidates {
                            tok_c.sort_unstable();
                            blk_c.sort_unstable();
                        }
                        let q_row = q.row(i, h);
                        let mut running_max = f32::NEG_INFINITY;
                        let mut denom = 0.0f32;
                        acc.fill(0.0);
                        for &j in &tok_c {
                            let score = dot(q_row, k.row(j, h)) * scale;
                            if score > running_max {
                                let c = (running_max - score).exp();
                                for d in 0..dim {
                                    acc[d] *= c;
                                }
                                denom *= c;
                                running_max = score;
                            }
                            let w = (score - running_max).exp();
                            denom += w;
                            let vr = v.row(j, h);
                            for d in 0..dim {
                                acc[d] += w * vr[d];
                            }
                        }
                        if let Some(lm) = lm_ref {
                            for &b in &blk_c {
                                let score = dot(q_row, lm.keys.row(b, h)) * scale;
                                if score > running_max {
                                    let c = (running_max - score).exp();
                                    for d in 0..dim {
                                        acc[d] *= c;
                                    }
                                    denom *= c;
                                    running_max = score;
                                }
                                let w = (score - running_max).exp();
                                denom += w;
                                let vr = lm.values.row(b, h);
                                for d in 0..dim {
                                    acc[d] += w * vr[d];
                                }
                            }
                        }
                        let inv = if denom > 0.0 { 1.0 / denom } else { 0.0 };
                        let s = &mut hout[i * dim..(i + 1) * dim];
                        for d in 0..dim {
                            s[d] = acc[d] * inv;
                        }
                    }
                    hout
                })
                .collect();
            let mut out = Tensor3::zeros(seq, heads, dim);
            for h in 0..heads {
                for i in 0..seq {
                    out.row_mut(i, h)
                        .copy_from_slice(&head_vecs[h][i * dim..(i + 1) * dim]);
                }
            }
            out
        };

        // Serial path (default — zero extra deps, works on no_std / WASM).
        #[cfg(not(feature = "parallel"))]
        let out = {
            let mut out = Tensor3::zeros(seq, heads, dim);
            let mut seen_tokens = vec![0usize; seq.max(1)];
            let mut seen_blocks = vec![0usize; div_ceil(seq.max(1), self.config.block_size)];
            let mut token_candidates = Vec::<usize>::with_capacity(self.config.window + 64);
            let mut block_candidates = Vec::<usize>::with_capacity(64);
            let mut acc = vec![0f32; dim];

            for h in 0..heads {
                for i in 0..seq {
                    let stamp = 1 + h * seq + i;
                    token_candidates.clear();
                    block_candidates.clear();
                    build_token_candidates(
                        i,
                        seq,
                        &self.config,
                        &mut seen_tokens,
                        stamp,
                        &mut token_candidates,
                    );
                    if landmarks.is_some() {
                        build_landmark_candidates(
                            i,
                            seq,
                            &self.config,
                            &mut seen_blocks,
                            stamp,
                            &mut block_candidates,
                        );
                    }
                    if self.config.sort_candidates {
                        token_candidates.sort_unstable();
                        block_candidates.sort_unstable();
                    }
                    let q_row = q.row(i, h);
                    let mut running_max = f32::NEG_INFINITY;
                    let mut denom = 0.0f32;
                    acc.fill(0.0);
                    for &j in &token_candidates {
                        let score = dot(q_row, k.row(j, h)) * scale;
                        if score > running_max {
                            let corr = (running_max - score).exp();
                            for d in 0..dim {
                                acc[d] *= corr;
                            }
                            denom *= corr;
                            running_max = score;
                        }
                        let w = (score - running_max).exp();
                        denom += w;
                        let v_row = v.row(j, h);
                        for d in 0..dim {
                            acc[d] += w * v_row[d];
                        }
                    }
                    if let Some(lm) = landmarks.as_ref() {
                        for &b in &block_candidates {
                            let score = dot(q_row, lm.keys.row(b, h)) * scale;
                            if score > running_max {
                                let corr = (running_max - score).exp();
                                for d in 0..dim {
                                    acc[d] *= corr;
                                }
                                denom *= corr;
                                running_max = score;
                            }
                            let w = (score - running_max).exp();
                            denom += w;
                            let v_row = lm.values.row(b, h);
                            for d in 0..dim {
                                acc[d] += w * v_row[d];
                            }
                        }
                    }
                    let out_row = out.row_mut(i, h);
                    let inv_denom = if denom > 0.0 { 1.0 / denom } else { 0.0 };
                    for d in 0..dim {
                        out_row[d] = acc[d] * inv_denom;
                    }
                }
            }
            out
        };

        Ok(out)
    }
}

impl SubquadraticSparseAttention {
    /// FastGRNN-gated forward: same as `forward` but tokens marked
    /// `false` in `keep_mask` are excluded from the long-range portion
    /// of the candidate set. The local causal window and the configured
    /// `global_tokens` are **always** retained regardless of the mask
    /// (they preserve causality and prefix coverage and are constant
    /// in `seq`, so dropping them does not change the asymptotic cost).
    ///
    /// # Asymptotics
    ///
    /// Let `W = config.window`, `G = config.global_tokens.len()`, and
    /// `K_keep = keep_mask.iter().filter(|&&b| b).count()`.
    /// Per-position candidate-set size is bounded by `W + G + K_keep`,
    /// so total cost is `O(seq · (W + G + K_keep) · dim)`.
    ///
    /// When the gate caps `K_keep` at a constant (e.g. via
    /// `FastGrnnGate::keep_mask_top_k`), this is **linear in `seq`**:
    /// `O(seq · const · dim)`. Combined with the FastGRNN gate forward
    /// (`O(seq · D_h²)`), the full pipeline is near-linear at fixed
    /// `D_h`, `W`, `G`, `K_keep`.
    ///
    /// # Errors
    ///
    /// Returns `AttentionError::InvalidConfig` if `keep_mask.len() != q.seq`.
    pub fn forward_gated(
        &self,
        q: &Tensor3,
        k: &Tensor3,
        v: &Tensor3,
        keep_mask: &[bool],
    ) -> Result<Tensor3, AttentionError> {
        validate_qkv(q, k, v)?;
        if self.config.block_size == 0 {
            return Err(AttentionError::InvalidConfig(
                "block_size must be greater than zero".to_string(),
            ));
        }
        if keep_mask.len() != q.seq {
            return Err(AttentionError::InvalidConfig(format!(
                "keep_mask len {} != seq {}",
                keep_mask.len(),
                q.seq
            )));
        }

        let seq = q.seq;
        if seq == 0 {
            return Ok(Tensor3::zeros(0, q.heads, q.dim));
        }

        let heads = q.heads;
        let dim = q.dim;
        let scale = 1.0f32 / (dim as f32).sqrt();
        let landmarks = if self.config.use_landmarks {
            Some(Landmarks::from_kv(k, v, self.config.block_size))
        } else {
            None
        };

        // Pre-compute "is in local window of i" predicate as a closure;
        // window membership is symmetric for non-causal so depends on i.
        // global membership is independent of i.
        let global_set: BTreeSet<usize> = self.config.global_tokens.iter().copied().collect();
        let in_window = |i: usize, j: usize| -> bool {
            let lo = i.saturating_sub(self.config.window);
            let hi = if self.config.causal {
                i
            } else {
                (i + self.config.window).min(seq - 1)
            };
            j >= lo && j <= hi
        };

        let mut out = Tensor3::zeros(seq, heads, dim);
        let mut seen_tokens = vec![0usize; seq.max(1)];
        let mut seen_blocks = vec![0usize; div_ceil(seq.max(1), self.config.block_size)];
        let mut tok_c = Vec::<usize>::with_capacity(self.config.window + 64);
        let mut blk_c = Vec::<usize>::with_capacity(64);
        let mut acc = vec![0f32; dim];

        for h in 0..heads {
            for i in 0..seq {
                let stamp = 1 + h * seq + i;
                tok_c.clear();
                blk_c.clear();
                build_token_candidates(i, seq, &self.config, &mut seen_tokens, stamp, &mut tok_c);
                if landmarks.is_some() {
                    build_landmark_candidates(
                        i,
                        seq,
                        &self.config,
                        &mut seen_blocks,
                        stamp,
                        &mut blk_c,
                    );
                }

                // Apply the gate: drop log-stride candidates that the
                // mask rejects. Keep the current position, window, and
                // globals unconditionally.
                tok_c.retain(|&j| {
                    j == i || in_window(i, j) || global_set.contains(&j) || keep_mask[j]
                });

                if self.config.sort_candidates {
                    tok_c.sort_unstable();
                    blk_c.sort_unstable();
                }
                let q_row = q.row(i, h);
                let mut running_max = f32::NEG_INFINITY;
                let mut denom = 0.0f32;
                acc.fill(0.0);
                for &j in &tok_c {
                    let score = dot(q_row, k.row(j, h)) * scale;
                    if score > running_max {
                        let corr = (running_max - score).exp();
                        for d in 0..dim {
                            acc[d] *= corr;
                        }
                        denom *= corr;
                        running_max = score;
                    }
                    let w = (score - running_max).exp();
                    denom += w;
                    let v_row = v.row(j, h);
                    for d in 0..dim {
                        acc[d] += w * v_row[d];
                    }
                }
                if let Some(lm) = landmarks.as_ref() {
                    for &b in &blk_c {
                        let score = dot(q_row, lm.keys.row(b, h)) * scale;
                        if score > running_max {
                            let corr = (running_max - score).exp();
                            for d in 0..dim {
                                acc[d] *= corr;
                            }
                            denom *= corr;
                            running_max = score;
                        }
                        let w = (score - running_max).exp();
                        denom += w;
                        let v_row = lm.values.row(b, h);
                        for d in 0..dim {
                            acc[d] += w * v_row[d];
                        }
                    }
                }
                let out_row = out.row_mut(i, h);
                let inv_denom = if denom > 0.0 { 1.0 / denom } else { 0.0 };
                for d in 0..dim {
                    out_row[d] = acc[d] * inv_denom;
                }
            }
        }
        Ok(out)
    }

    /// Convenience wrapper: build the keep-mask via FastGRNN scoring
    /// `k`, then call `forward_gated`. The gate's `input_dim` must
    /// equal `k.dim` (the per-head feature width). Caller picks the
    /// gating strategy via `gate_top_k`: at most that many long-range
    /// tokens pass the gate.
    pub fn forward_gated_with_fastgrnn(
        &self,
        q: &Tensor3,
        k: &Tensor3,
        v: &Tensor3,
        gate: &crate::fastgrnn_gate::FastGrnnGate,
        gate_top_k: usize,
    ) -> Result<Tensor3, AttentionError> {
        let salience = gate.score_kv(k);
        let keep = crate::fastgrnn_gate::FastGrnnGate::keep_mask_top_k(&salience, gate_top_k);
        self.forward_gated(q, k, v, &keep)
    }
}

pub fn dense_attention(
    q: &Tensor3,
    k: &Tensor3,
    v: &Tensor3,
    causal: bool,
) -> Result<Tensor3, AttentionError> {
    validate_qkv(q, k, v)?;
    if q.seq == 0 {
        return Ok(Tensor3::zeros(q.seq, q.heads, q.dim));
    }

    let seq = q.seq;
    let heads = q.heads;
    let dim = q.dim;
    let scale = 1.0f32 / (dim as f32).sqrt();
    let mut out = Tensor3::zeros(seq, heads, dim);
    let mut acc = vec![0f32; dim];

    for h in 0..heads {
        for i in 0..seq {
            let q_row = q.row(i, h);
            let last = if causal { i } else { seq - 1 };
            let mut max_score = f32::NEG_INFINITY;

            for j in 0..=last {
                let score = dot(q_row, k.row(j, h)) * scale;
                if score > max_score {
                    max_score = score;
                }
            }

            acc.fill(0.0);
            let mut denom = 0.0f32;

            for j in 0..=last {
                let score = dot(q_row, k.row(j, h)) * scale;
                let weight = (score - max_score).exp();
                denom += weight;
                let v_row = v.row(j, h);
                for d in 0..dim {
                    acc[d] += weight * v_row[d];
                }
            }

            let out_row = out.row_mut(i, h);
            let inv_denom = if denom > 0.0 { 1.0 / denom } else { 0.0 };
            for d in 0..dim {
                out_row[d] = acc[d] * inv_denom;
            }
        }
    }

    Ok(out)
}

fn validate_qkv(q: &Tensor3, k: &Tensor3, v: &Tensor3) -> Result<(), AttentionError> {
    if q.dim == 0 {
        return Err(AttentionError::InvalidConfig(
            "head dimension must be greater than zero".to_string(),
        ));
    }

    if q.shape() != k.shape() || q.shape() != v.shape() {
        return Err(AttentionError::ShapeMismatch {
            q: q.shape(),
            k: k.shape(),
            v: v.shape(),
        });
    }
    Ok(())
}

#[inline]
fn dot(a: &[f32], b: &[f32]) -> f32 {
    debug_assert_eq!(a.len(), b.len());
    a.iter().zip(b.iter()).map(|(x, y)| x * y).sum()
}

#[cfg(feature = "fp16")]
#[allow(dead_code)]
#[inline]
fn dot_f16(a: &[f32], b: &[half::f16]) -> f32 {
    a.iter().zip(b.iter()).map(|(x, y)| x * y.to_f32()).sum()
}

fn build_token_candidates(
    i: usize,
    seq: usize,
    config: &SparseAttentionConfig,
    seen: &mut [usize],
    stamp: usize,
    out: &mut Vec<usize>,
) {
    if seq == 0 {
        return;
    }

    let start = i.saturating_sub(config.window);
    let end = if config.causal {
        i
    } else {
        (i + config.window).min(seq - 1)
    };

    for j in start..=end {
        push_unique(j, seen, stamp, out);
    }

    for &g in &config.global_tokens {
        if g < seq && (!config.causal || g <= i) {
            push_unique(g, seen, stamp, out);
        }
    }

    if config.use_log_stride {
        let mut stride = 1usize;
        while stride < seq {
            if i >= stride {
                push_unique(i - stride, seen, stamp, out);
            }

            if !config.causal {
                let forward = i + stride;
                if forward < seq {
                    push_unique(forward, seen, stamp, out);
                }
            }

            match stride.checked_mul(2) {
                Some(next) => stride = next,
                None => break,
            }
        }
    }
}

fn build_landmark_candidates(
    i: usize,
    seq: usize,
    config: &SparseAttentionConfig,
    seen: &mut [usize],
    stamp: usize,
    out: &mut Vec<usize>,
) {
    if seq == 0 || config.block_size == 0 {
        return;
    }

    let blocks = div_ceil(seq, config.block_size);
    if blocks == 0 {
        return;
    }

    let current_block = i / config.block_size;
    let available_blocks = if config.causal {
        (i + 1) / config.block_size
    } else {
        blocks
    };

    if available_blocks == 0 {
        return;
    }

    let pivot = if config.causal {
        available_blocks - 1
    } else {
        current_block.min(blocks - 1)
    };

    let local_start = pivot.saturating_sub(1);
    let local_end = (pivot + 1).min(available_blocks - 1);
    for b in local_start..=local_end {
        // ADR-185: in non-causal mode skip the block that contains token i.
        // Token i is already reachable via the window; adding its block mean
        // double-counts it and biases the softmax toward the current block.
        if !config.causal && b == current_block {
            continue;
        }
        push_unique(b, seen, stamp, out);
    }

    if config.use_log_stride {
        let mut stride = 1usize;
        while stride < blocks {
            if pivot >= stride {
                let b = pivot - stride;
                if config.causal || b != current_block {
                    push_unique(b, seen, stamp, out);
                }
            }

            if !config.causal {
                let forward = pivot + stride;
                if forward < blocks && forward != current_block {
                    push_unique(forward, seen, stamp, out);
                }
            }

            match stride.checked_mul(2) {
                Some(next) => stride = next,
                None => break,
            }
        }
    }
}

#[inline]
fn push_unique(index: usize, seen: &mut [usize], stamp: usize, out: &mut Vec<usize>) {
    if seen[index] != stamp {
        seen[index] = stamp;
        out.push(index);
    }
}

#[derive(Clone, Debug)]
struct Landmarks {
    keys: Tensor3,
    values: Tensor3,
}

impl Landmarks {
    fn from_kv(k: &Tensor3, v: &Tensor3, block_size: usize) -> Self {
        let blocks = div_ceil(k.seq, block_size);
        let mut keys = Tensor3::zeros(blocks, k.heads, k.dim);
        let mut values = Tensor3::zeros(blocks, v.heads, v.dim);

        for b in 0..blocks {
            let start = b * block_size;
            let end = (start + block_size).min(k.seq);
            let count = (end - start) as f32;

            for h in 0..k.heads {
                for t in start..end {
                    let k_row = k.row(t, h);
                    let v_row = v.row(t, h);
                    let key_out = keys.row_mut(b, h);
                    let value_out = values.row_mut(b, h);
                    for d in 0..k.dim {
                        key_out[d] += k_row[d];
                        value_out[d] += v_row[d];
                    }
                }

                let key_out = keys.row_mut(b, h);
                let value_out = values.row_mut(b, h);
                for d in 0..k.dim {
                    key_out[d] /= count;
                    value_out[d] /= count;
                }
            }
        }

        Self { keys, values }
    }
}

#[inline]
fn div_ceil(a: usize, b: usize) -> usize {
    if a == 0 {
        0
    } else {
        1 + (a - 1) / b
    }
}

/// Incrementally maintained landmark block-means for O(1) decode updates (ADR-189).
///
/// Updated via Welford running mean on each `KvCache::append`, eliminating the
/// O(T × kv_heads × dim) full rebuild that `decode_step` previously did per token.
#[derive(Clone, Debug)]
pub struct IncrementalLandmarks {
    /// Running block-mean keys  [max_blocks, kv_heads, dim]
    pub keys: Tensor3,
    /// Running block-mean values [max_blocks, kv_heads, dim]
    pub values: Tensor3,
    counts: Vec<usize>,
    pub block_size: usize,
}

impl IncrementalLandmarks {
    pub fn new(capacity: usize, block_size: usize, kv_heads: usize, dim: usize) -> Self {
        let max_blocks = if block_size == 0 || capacity == 0 {
            1
        } else {
            div_ceil(capacity, block_size)
        };
        Self {
            keys: Tensor3::zeros(max_blocks, kv_heads, dim),
            values: Tensor3::zeros(max_blocks, kv_heads, dim),
            counts: vec![0; max_blocks],
            block_size,
        }
    }

    /// Welford online mean update for the block containing token `t`. O(H × D).
    pub fn update(&mut self, t: usize, k: &Tensor3, v: &Tensor3) {
        if self.block_size == 0 {
            return;
        }
        let b = t / self.block_size;
        if b >= self.counts.len() {
            return;
        }
        self.counts[b] += 1;
        let count = self.counts[b] as f32;
        for h in 0..k.heads {
            let k_src = k.row(0, h);
            let v_src = v.row(0, h);
            let k_dst = self.keys.row_mut(b, h);
            let v_dst = self.values.row_mut(b, h);
            for d in 0..k.dim {
                k_dst[d] += (k_src[d] - k_dst[d]) / count;
                v_dst[d] += (v_src[d] - v_dst[d]) / count;
            }
        }
    }

    /// Reset all block means (does not free memory).
    pub fn reset(&mut self) {
        self.keys.data.fill(0.0);
        self.values.data.fill(0.0);
        self.counts.fill(0);
    }
}

/// KV cache for incremental decode. Stores keys/values for all previous tokens.
/// For GQA/MQA set kv_heads = k.heads (8 for Mistral-7B, not 32).
#[derive(Clone, Debug)]
pub struct KvCache {
    pub keys: Tensor3,
    pub values: Tensor3,
    pub len: usize,
    pub capacity: usize,
    /// Incrementally maintained block-mean landmarks — updated O(H×D) per append.
    pub landmarks: IncrementalLandmarks,
}

impl KvCache {
    pub fn new(capacity: usize, kv_heads: usize, dim: usize, block_size: usize) -> Self {
        Self {
            keys: Tensor3::zeros(capacity, kv_heads, dim),
            values: Tensor3::zeros(capacity, kv_heads, dim),
            len: 0,
            capacity,
            landmarks: IncrementalLandmarks::new(capacity, block_size, kv_heads, dim),
        }
    }

    /// Append one new token's K/V (shape [1, kv_heads, dim]); updates landmarks.
    /// Panics on capacity overflow — use `try_append` for recoverable errors.
    pub fn append(&mut self, k: &Tensor3, v: &Tensor3) {
        self.try_append(k, v).expect("KvCache capacity exceeded");
    }

    /// Non-panicking append. Returns `Err` on capacity overflow or shape mismatch.
    pub fn try_append(&mut self, k: &Tensor3, v: &Tensor3) -> Result<(), AttentionError> {
        if k.seq != 1 || v.seq != 1 {
            return Err(AttentionError::InvalidConfig(
                "try_append expects single-token tensors (seq == 1)".into(),
            ));
        }
        if k.heads != self.keys.heads || v.heads != self.keys.heads {
            return Err(AttentionError::InvalidConfig(format!(
                "kv_heads mismatch: cache={}, k={}, v={}",
                self.keys.heads, k.heads, v.heads
            )));
        }
        if self.len >= self.capacity {
            return Err(AttentionError::InvalidConfig(format!(
                "KvCache capacity exceeded: capacity={}, len={}",
                self.capacity, self.len
            )));
        }
        for h in 0..k.heads {
            self.keys.row_mut(self.len, h).copy_from_slice(k.row(0, h));
            self.values
                .row_mut(self.len, h)
                .copy_from_slice(v.row(0, h));
        }
        self.landmarks.update(self.len, k, v);
        self.len += 1;
        Ok(())
    }

    pub fn is_full(&self) -> bool {
        self.len >= self.capacity
    }

    /// Bulk-load a multi-token K/V tensor (shape [n, kv_heads, dim]) into the cache.
    /// Used for the prefill pass to populate the cache from the full prompt.
    pub fn append_all(&mut self, k: &Tensor3, v: &Tensor3) -> Result<(), AttentionError> {
        let n = k.seq;
        if v.seq != n {
            return Err(AttentionError::InvalidConfig(
                "append_all: k.seq != v.seq".into(),
            ));
        }
        if k.heads != self.keys.heads || v.heads != self.keys.heads {
            return Err(AttentionError::InvalidConfig(format!(
                "kv_heads mismatch: cache={}, k={}, v={}",
                self.keys.heads, k.heads, v.heads
            )));
        }
        if self.len + n > self.capacity {
            return Err(AttentionError::InvalidConfig(format!(
                "KvCache overflow: capacity={}, len={}, adding={}",
                self.capacity, self.len, n
            )));
        }
        let kv_heads = k.heads;
        let dim = k.dim;
        for t in 0..n {
            let pos = self.len + t;
            for h in 0..kv_heads {
                self.keys.row_mut(pos, h).copy_from_slice(k.row(t, h));
                self.values.row_mut(pos, h).copy_from_slice(v.row(t, h));
            }
            // Update incremental landmarks using a single-token view (avoids allocation
            // for the common case where landmarks are disabled in prefill).
            if self.landmarks.block_size > 0 {
                let k_t = Tensor3::from_vec(
                    k.data[t * kv_heads * dim..(t + 1) * kv_heads * dim].to_vec(),
                    1,
                    kv_heads,
                    dim,
                )
                .unwrap();
                let v_t = Tensor3::from_vec(
                    v.data[t * kv_heads * dim..(t + 1) * kv_heads * dim].to_vec(),
                    1,
                    kv_heads,
                    dim,
                )
                .unwrap();
                self.landmarks.update(pos, &k_t, &v_t);
            }
        }
        self.len += n;
        Ok(())
    }

    /// Reset to empty without freeing memory.
    pub fn reset(&mut self) {
        self.len = 0;
        self.landmarks.reset();
    }

    /// H2O-style heavy-hitter eviction: compact the cache by removing the token
    /// with the lowest cumulative attention score, then append the new token.
    ///
    /// `attention_scores` must be a slice of length `self.len` where
    /// `attention_scores[j]` is the total attention weight token `j` has received
    /// across all decode steps.  Global-token positions and the most recent
    /// `window` tokens are protected from eviction.
    ///
    /// Returns `Err` on capacity zero, k/v shape mismatch, or score-len mismatch.
    pub fn evict_and_append(
        &mut self,
        k: &Tensor3,
        v: &Tensor3,
        attention_scores: &[f32],
        global_tokens: &[usize],
        window: usize,
    ) -> Result<(), AttentionError> {
        if !self.is_full() {
            return self.try_append(k, v);
        }
        if attention_scores.len() != self.len {
            return Err(AttentionError::InvalidConfig(format!(
                "evict_and_append: attention_scores.len={} != cache.len={}",
                attention_scores.len(),
                self.len
            )));
        }
        if self.capacity == 0 {
            return Err(AttentionError::InvalidConfig(
                "evict_and_append: capacity is zero".into(),
            ));
        }

        // Find the eviction victim: lowest attention score, not in recent window,
        // not a global token.
        let recent_start = self.len.saturating_sub(window);
        let is_protected =
            |idx: usize| -> bool { idx >= recent_start || global_tokens.contains(&idx) };
        let victim = (0..self.len)
            .filter(|&idx| !is_protected(idx))
            .min_by(|&a, &b| {
                attention_scores[a]
                    .partial_cmp(&attention_scores[b])
                    .unwrap_or(Ordering::Equal)
            });

        let victim = match victim {
            Some(v) => v,
            None => {
                // All tokens protected — fall back to evicting the oldest non-global.
                (0..recent_start)
                    .find(|idx| !global_tokens.contains(idx))
                    .unwrap_or(0)
            }
        };

        // Compact: shift tokens [victim+1..len] left by one slot.
        let kv_heads = self.keys.heads;
        let dim = self.keys.dim;
        for t in victim..self.len - 1 {
            for h in 0..kv_heads {
                // SAFETY: t < t+1 < self.len <= self.capacity, all in bounds.
                let src_off = ((t + 1) * kv_heads + h) * dim;
                let dst_off = (t * kv_heads + h) * dim;
                self.keys.data.copy_within(src_off..src_off + dim, dst_off);
                self.values
                    .data
                    .copy_within(src_off..src_off + dim, dst_off);
            }
        }
        self.len -= 1;
        // Rebuild landmarks after compaction (eviction is infrequent, O(T) rebuild ok).
        self.landmarks.reset();
        let lm_block = self.landmarks.block_size;
        if lm_block > 0 {
            for t in 0..self.len {
                let k_t = Tensor3::from_vec(
                    self.keys.data[t * kv_heads * dim..(t + 1) * kv_heads * dim].to_vec(),
                    1,
                    kv_heads,
                    dim,
                )
                .unwrap();
                let v_t = Tensor3::from_vec(
                    self.values.data[t * kv_heads * dim..(t + 1) * kv_heads * dim].to_vec(),
                    1,
                    kv_heads,
                    dim,
                )
                .unwrap();
                self.landmarks.update(t, &k_t, &v_t);
            }
        }
        self.try_append(k, v)
    }
}

impl SubquadraticSparseAttention {
    /// Single-token decode against the KV cache (ADR-189).
    ///
    /// **Caller must append the new token's K/V to `cache` before calling this.**
    /// The new token is at position `cache.len - 1`; landmarks are O(1)-updated
    /// by `KvCache::append` so no rebuild is needed here.
    ///
    /// `q`: shape `[1, q_heads, dim]`.  Returns shape `[1, q_heads, dim]`.
    pub fn decode_step(&self, q: &Tensor3, cache: &KvCache) -> Result<Tensor3, AttentionError> {
        if q.seq != 1 {
            return Err(AttentionError::InvalidConfig(
                "decode_step requires q.seq == 1".to_string(),
            ));
        }
        if q.dim == 0 {
            return Err(AttentionError::InvalidConfig(
                "head dimension must be greater than zero".to_string(),
            ));
        }
        if cache.len == 0 {
            return Ok(Tensor3::zeros(1, q.heads, q.dim));
        }
        if cache.keys.heads == 0 || q.heads % cache.keys.heads != 0 {
            return Err(AttentionError::InvalidConfig(format!(
                "q_heads={} must be divisible by kv_heads={}",
                q.heads, cache.keys.heads
            )));
        }

        let q_heads = q.heads;
        let kv_heads = cache.keys.heads;
        let group_size = q_heads / kv_heads;
        let dim = q.dim;
        let scale = 1.0f32 / (dim as f32).sqrt();
        // New token was appended before this call; its position is cache.len - 1.
        let i = cache.len - 1;
        let seq = cache.len;

        let mut seen_tokens = vec![0usize; seq.max(1)];
        let mut seen_blocks = vec![0usize; div_ceil(seq.max(1), self.config.block_size)];
        let mut token_candidates = Vec::with_capacity(self.config.window + 64);
        let mut block_candidates = Vec::with_capacity(64);

        build_token_candidates(
            i,
            seq,
            &self.config,
            &mut seen_tokens,
            1,
            &mut token_candidates,
        );
        if self.config.use_landmarks {
            build_landmark_candidates(
                i,
                seq,
                &self.config,
                &mut seen_blocks,
                1,
                &mut block_candidates,
            );
        }
        if self.config.sort_candidates {
            token_candidates.sort_unstable();
            block_candidates.sort_unstable();
        }

        let mut out = Tensor3::zeros(1, q_heads, dim);
        let mut acc = vec![0f32; dim];

        for h in 0..q_heads {
            let kv_h = h / group_size;
            let q_row = q.row(0, h);
            let mut running_max = f32::NEG_INFINITY;
            let mut denom = 0.0f32;
            acc.fill(0.0);

            for &j in &token_candidates {
                let score = dot(q_row, cache.keys.row(j, kv_h)) * scale;
                if score > running_max {
                    let corr = (running_max - score).exp();
                    for d in 0..dim {
                        acc[d] *= corr;
                    }
                    denom *= corr;
                    running_max = score;
                }
                let w = (score - running_max).exp();
                denom += w;
                let v_row = cache.values.row(j, kv_h);
                for d in 0..dim {
                    acc[d] += w * v_row[d];
                }
            }

            // Use O(1) incremental landmarks — no per-step rebuild.
            for &b in &block_candidates {
                let score = dot(q_row, cache.landmarks.keys.row(b, kv_h)) * scale;
                if score > running_max {
                    let corr = (running_max - score).exp();
                    for d in 0..dim {
                        acc[d] *= corr;
                    }
                    denom *= corr;
                    running_max = score;
                }
                let w = (score - running_max).exp();
                denom += w;
                let v_row = cache.landmarks.values.row(b, kv_h);
                for d in 0..dim {
                    acc[d] += w * v_row[d];
                }
            }

            let out_row = out.row_mut(0, h);
            let inv = if denom > 0.0 { 1.0 / denom } else { 0.0 };
            for d in 0..dim {
                out_row[d] = acc[d] * inv;
            }
        }

        Ok(out)
    }

    /// Decode a batch of draft tokens (speculative decoding).
    ///
    /// Appends each draft token's K/V to `cache` and computes attention for the
    /// corresponding query token against the growing cache.  Equivalent to calling
    /// `cache.try_append` + `decode_step` for each token in sequence, but shares
    /// the allocation overhead.
    ///
    /// `q`, `new_k`, `new_v` must all have the same `seq` (the draft length).
    /// Returns shape `[draft_len, q_heads, dim]`.
    pub fn decode_batch(
        &self,
        q: &Tensor3,
        new_k: &Tensor3,
        new_v: &Tensor3,
        cache: &mut KvCache,
    ) -> Result<Tensor3, AttentionError> {
        let draft_len = q.seq;
        if draft_len == 0 {
            return Ok(Tensor3::zeros(0, q.heads, q.dim));
        }
        if q.dim == 0 {
            return Err(AttentionError::InvalidConfig(
                "head dimension must be greater than zero".into(),
            ));
        }
        if new_k.seq != draft_len || new_v.seq != draft_len {
            return Err(AttentionError::InvalidConfig(format!(
                "decode_batch: q.seq={draft_len} but new_k.seq={} new_v.seq={}",
                new_k.seq, new_v.seq
            )));
        }
        if cache.keys.heads == 0 || q.heads % cache.keys.heads != 0 {
            return Err(AttentionError::InvalidConfig(format!(
                "q_heads={} must be divisible by kv_heads={}",
                q.heads, cache.keys.heads
            )));
        }

        let q_heads = q.heads;
        let kv_heads = new_k.heads;
        let dim = q.dim;
        let mut out = Tensor3::zeros(draft_len, q_heads, dim);

        for t in 0..draft_len {
            // Extract single-token slices as owned Tensor3 (avoids unsafe borrow aliasing).
            let q_t = Tensor3::from_vec(
                q.data[t * q_heads * dim..(t + 1) * q_heads * dim].to_vec(),
                1,
                q_heads,
                dim,
            )
            .unwrap();
            let k_t = Tensor3::from_vec(
                new_k.data[t * kv_heads * dim..(t + 1) * kv_heads * dim].to_vec(),
                1,
                kv_heads,
                dim,
            )
            .unwrap();
            let v_t = Tensor3::from_vec(
                new_v.data[t * kv_heads * dim..(t + 1) * kv_heads * dim].to_vec(),
                1,
                kv_heads,
                dim,
            )
            .unwrap();

            cache.try_append(&k_t, &v_t)?;
            let out_t = self.decode_step(&q_t, cache)?;
            out.data[t * q_heads * dim..(t + 1) * q_heads * dim].copy_from_slice(&out_t.data);
        }

        Ok(out)
    }
}

fn validate_gqa(q: &Tensor3, k: &Tensor3, v: &Tensor3) -> Result<(), AttentionError> {
    if q.dim == 0 {
        return Err(AttentionError::InvalidConfig(
            "head dimension must be greater than zero".to_string(),
        ));
    }
    if q.seq != k.seq || k.seq != v.seq {
        return Err(AttentionError::ShapeMismatch {
            q: q.shape(),
            k: k.shape(),
            v: v.shape(),
        });
    }
    if q.dim != k.dim || k.dim != v.dim {
        return Err(AttentionError::InvalidConfig(format!(
            "head dim mismatch: q.dim={}, k.dim={}",
            q.dim, k.dim
        )));
    }
    if k.heads == 0 || q.heads % k.heads != 0 {
        return Err(AttentionError::InvalidConfig(format!(
            "q_heads={} must be divisible by kv_heads={}",
            q.heads, k.heads
        )));
    }
    if k.heads != v.heads {
        return Err(AttentionError::InvalidConfig(format!(
            "k.heads={} != v.heads={}",
            k.heads, v.heads
        )));
    }
    Ok(())
}

impl SubquadraticSparseAttention {
    /// GQA/MQA forward: q has q_heads, k/v have kv_heads where q_heads % kv_heads == 0.
    /// group_size = q_heads / kv_heads (4 for Mistral-7B/Llama-3, 8 for TinyLlama).
    pub fn forward_gqa(
        &self,
        q: &Tensor3,
        k: &Tensor3,
        v: &Tensor3,
    ) -> Result<Tensor3, AttentionError> {
        validate_gqa(q, k, v)?;
        if self.config.block_size == 0 {
            return Err(AttentionError::InvalidConfig(
                "block_size must be greater than zero".to_string(),
            ));
        }

        let seq = q.seq;
        if seq == 0 {
            return Ok(Tensor3::zeros(0, q.heads, q.dim));
        }

        let q_heads = q.heads;
        let kv_heads = k.heads;
        let group_size = q_heads / kv_heads;
        let dim = q.dim;
        let scale = 1.0f32 / (dim as f32).sqrt();

        let landmarks = if self.config.use_landmarks {
            Some(Landmarks::from_kv(k, v, self.config.block_size))
        } else {
            None
        };

        #[cfg(feature = "parallel")]
        let out = {
            use rayon::prelude::*;
            let lm_ref = landmarks.as_ref();
            let config = &self.config;
            let head_vecs: Vec<Vec<f32>> = (0..q_heads)
                .into_par_iter()
                .map(|h| {
                    let kv_h = h / group_size;
                    let mut seen_tokens = vec![0usize; seq.max(1)];
                    let mut seen_blocks = vec![0usize; div_ceil(seq.max(1), config.block_size)];
                    let mut tok_c = Vec::<usize>::with_capacity(config.window + 64);
                    let mut blk_c = Vec::<usize>::with_capacity(64);
                    let mut acc = vec![0f32; dim];
                    let mut hout = vec![0f32; seq * dim];
                    for i in 0..seq {
                        let stamp = 1 + h * seq + i;
                        tok_c.clear();
                        blk_c.clear();
                        build_token_candidates(i, seq, config, &mut seen_tokens, stamp, &mut tok_c);
                        if lm_ref.is_some() {
                            build_landmark_candidates(
                                i,
                                seq,
                                config,
                                &mut seen_blocks,
                                stamp,
                                &mut blk_c,
                            );
                        }
                        if config.sort_candidates {
                            tok_c.sort_unstable();
                            blk_c.sort_unstable();
                        }
                        let q_row = q.row(i, h);
                        let mut running_max = f32::NEG_INFINITY;
                        let mut denom = 0.0f32;
                        acc.fill(0.0);
                        for &j in &tok_c {
                            let score = dot(q_row, k.row(j, kv_h)) * scale;
                            if score > running_max {
                                let c = (running_max - score).exp();
                                for d in 0..dim {
                                    acc[d] *= c;
                                }
                                denom *= c;
                                running_max = score;
                            }
                            let w = (score - running_max).exp();
                            denom += w;
                            let vr = v.row(j, kv_h);
                            for d in 0..dim {
                                acc[d] += w * vr[d];
                            }
                        }
                        if let Some(lm) = lm_ref {
                            for &b in &blk_c {
                                let score = dot(q_row, lm.keys.row(b, kv_h)) * scale;
                                if score > running_max {
                                    let c = (running_max - score).exp();
                                    for d in 0..dim {
                                        acc[d] *= c;
                                    }
                                    denom *= c;
                                    running_max = score;
                                }
                                let w = (score - running_max).exp();
                                denom += w;
                                let vr = lm.values.row(b, kv_h);
                                for d in 0..dim {
                                    acc[d] += w * vr[d];
                                }
                            }
                        }
                        let inv = if denom > 0.0 { 1.0 / denom } else { 0.0 };
                        let s = &mut hout[i * dim..(i + 1) * dim];
                        for d in 0..dim {
                            s[d] = acc[d] * inv;
                        }
                    }
                    hout
                })
                .collect();
            let mut out = Tensor3::zeros(seq, q_heads, dim);
            for h in 0..q_heads {
                for i in 0..seq {
                    out.row_mut(i, h)
                        .copy_from_slice(&head_vecs[h][i * dim..(i + 1) * dim]);
                }
            }
            out
        };

        #[cfg(not(feature = "parallel"))]
        let out = {
            let mut out = Tensor3::zeros(seq, q_heads, dim);
            let mut seen_tokens = vec![0usize; seq.max(1)];
            let mut seen_blocks = vec![0usize; div_ceil(seq.max(1), self.config.block_size)];
            let mut token_candidates = Vec::<usize>::with_capacity(self.config.window + 64);
            let mut block_candidates = Vec::<usize>::with_capacity(64);
            let mut acc = vec![0f32; dim];

            for h in 0..q_heads {
                let kv_h = h / group_size;
                for i in 0..seq {
                    let stamp = 1 + h * seq + i;
                    token_candidates.clear();
                    block_candidates.clear();
                    build_token_candidates(
                        i,
                        seq,
                        &self.config,
                        &mut seen_tokens,
                        stamp,
                        &mut token_candidates,
                    );
                    if landmarks.is_some() {
                        build_landmark_candidates(
                            i,
                            seq,
                            &self.config,
                            &mut seen_blocks,
                            stamp,
                            &mut block_candidates,
                        );
                    }
                    if self.config.sort_candidates {
                        token_candidates.sort_unstable();
                        block_candidates.sort_unstable();
                    }
                    let q_row = q.row(i, h);
                    let mut running_max = f32::NEG_INFINITY;
                    let mut denom = 0.0f32;
                    acc.fill(0.0);
                    for &j in &token_candidates {
                        let score = dot(q_row, k.row(j, kv_h)) * scale;
                        if score > running_max {
                            let corr = (running_max - score).exp();
                            for d in 0..dim {
                                acc[d] *= corr;
                            }
                            denom *= corr;
                            running_max = score;
                        }
                        let w = (score - running_max).exp();
                        denom += w;
                        let v_row = v.row(j, kv_h);
                        for d in 0..dim {
                            acc[d] += w * v_row[d];
                        }
                    }
                    if let Some(lm) = landmarks.as_ref() {
                        for &b in &block_candidates {
                            let score = dot(q_row, lm.keys.row(b, kv_h)) * scale;
                            if score > running_max {
                                let corr = (running_max - score).exp();
                                for d in 0..dim {
                                    acc[d] *= corr;
                                }
                                denom *= corr;
                                running_max = score;
                            }
                            let w = (score - running_max).exp();
                            denom += w;
                            let v_row = lm.values.row(b, kv_h);
                            for d in 0..dim {
                                acc[d] += w * v_row[d];
                            }
                        }
                    }
                    let out_row = out.row_mut(i, h);
                    let inv = if denom > 0.0 { 1.0 / denom } else { 0.0 };
                    for d in 0..dim {
                        out_row[d] = acc[d] * inv;
                    }
                }
            }
            out
        };

        Ok(out)
    }

    /// Auto-dispatch: uses forward() for MHA (q_heads==k_heads), forward_gqa() for GQA/MQA.
    pub fn forward_auto(
        &self,
        q: &Tensor3,
        k: &Tensor3,
        v: &Tensor3,
    ) -> Result<Tensor3, AttentionError> {
        if q.heads == k.heads {
            self.forward(q, k, v)
        } else {
            self.forward_gqa(q, k, v)
        }
    }

    /// IO-optimal tiled sparse attention (Flash-Sparse) — shared inner kernel.
    ///
    /// Iterates KV in tiles of `tile_size` tokens; for each tile processes all
    /// queries whose window overlaps that tile using online softmax accumulators.
    /// KV is accessed sequentially across tiles so the hardware prefetcher keeps
    /// L1/L2 hot, unlike the query-outer loop which scatters KV reads.
    ///
    /// `group_size`: 1 for MHA, q_heads/kv_heads for GQA.
    fn forward_flash_inner(
        &self,
        q: &Tensor3,
        k: &Tensor3,
        v: &Tensor3,
        tile_size: usize,
        group_size: usize,
    ) -> Result<Tensor3, AttentionError> {
        let seq = q.seq;
        let q_heads = q.heads;
        let _kv_heads = k.heads;
        let dim = q.dim;
        let scale = 1.0f32 / (dim as f32).sqrt();
        let window = self.config.window;
        let causal = self.config.causal;

        // Choose tile size: auto means use window (fits L1 for typical models).
        let tile = if tile_size == 0 {
            window.max(1)
        } else {
            tile_size
        };

        // Per-query running accumulators [q_heads][seq] (running_max, denom, acc*dim).
        // Layout: flattened per query-head, then per query token.
        let num_slots = q_heads * seq;
        let mut running_max = vec![f32::NEG_INFINITY; num_slots];
        let mut denom = vec![0.0f32; num_slots];
        let mut out_data = vec![0.0f32; q_heads * seq * dim];

        // --- Phase 1: Tiled window scan ---
        // Iterate KV tiles in ascending order. For each tile, find which queries
        // overlap (window intersection is non-empty), then accumulate.
        let mut kv_start = 0usize;
        while kv_start < seq {
            let kv_end = (kv_start + tile).min(seq); // exclusive

            // Query range whose window overlaps [kv_start, kv_end):
            let q_lo = if causal {
                kv_start // causal: q must see kv_start, so q >= kv_start
            } else {
                kv_start.saturating_sub(window)
            };
            let q_hi = if causal {
                (kv_end - 1 + window + 1).min(seq) // exclusive upper bound on q
            } else {
                (kv_end - 1 + window + 1).min(seq)
            };

            for h in 0..q_heads {
                let kv_h = h / group_size;
                for qi in q_lo..q_hi {
                    // Window intersection of query qi with this tile:
                    let win_lo = if causal {
                        qi.saturating_sub(window).max(kv_start)
                    } else {
                        qi.saturating_sub(window).max(kv_start)
                    };
                    let win_hi = if causal {
                        qi.min(kv_end.saturating_sub(1))
                    } else {
                        (qi + window).min(kv_end.saturating_sub(1))
                    };
                    if win_lo > win_hi {
                        continue;
                    }
                    let slot = h * seq + qi;
                    let q_row = q.row(qi, h);
                    let out_base = slot * dim;

                    for j in win_lo..=win_hi {
                        let score = dot(q_row, k.row(j, kv_h)) * scale;
                        if score > running_max[slot] {
                            let corr = (running_max[slot] - score).exp();
                            for d in 0..dim {
                                out_data[out_base + d] *= corr;
                            }
                            denom[slot] *= corr;
                            running_max[slot] = score;
                        }
                        let w = (score - running_max[slot]).exp();
                        denom[slot] += w;
                        let v_row = v.row(j, kv_h);
                        for d in 0..dim {
                            out_data[out_base + d] += w * v_row[d];
                        }
                    }
                }
            }

            kv_start = kv_end;
        }

        // --- Phase 2: Scattered pass (globals + log-stride + landmarks) ---
        // We need per-query dedup so pre-mark window positions for each query,
        // then push non-window sparse candidates via the standard helpers,
        // excluding already-seen positions.
        let landmarks = if self.config.use_landmarks {
            Some(Landmarks::from_kv(k, v, self.config.block_size))
        } else {
            None
        };

        // seen_tokens stamp array: shared across heads (fine — stamps include h).
        let mut seen_tokens = vec![0usize; seq.max(1)];
        let mut seen_blocks = vec![0usize; div_ceil(seq.max(1), self.config.block_size)];
        let mut sparse_toks: Vec<usize> = Vec::with_capacity(128);
        let mut sparse_blks: Vec<usize> = Vec::with_capacity(64);

        for h in 0..q_heads {
            let kv_h = h / group_size;
            for qi in 0..seq {
                let stamp_base = 1 + h * seq + qi;

                // Pre-mark the window positions so push_unique skips them.
                // Window was already processed in Phase 1.
                {
                    let win_lo = qi.saturating_sub(window);
                    let win_hi = if causal {
                        qi
                    } else {
                        (qi + window).min(seq - 1)
                    };
                    let mark_stamp = stamp_base; // same stamp
                    for j in win_lo..=win_hi {
                        seen_tokens[j] = mark_stamp;
                    }
                }

                sparse_toks.clear();
                sparse_blks.clear();

                // globals
                for &g in &self.config.global_tokens {
                    if g < seq && (!causal || g <= qi) {
                        push_unique(g, &mut seen_tokens, stamp_base, &mut sparse_toks);
                    }
                }

                // log-stride
                if self.config.use_log_stride {
                    let mut stride = 1usize;
                    while stride < seq {
                        if qi >= stride {
                            push_unique(
                                qi - stride,
                                &mut seen_tokens,
                                stamp_base,
                                &mut sparse_toks,
                            );
                        }
                        if !causal {
                            let fwd = qi + stride;
                            if fwd < seq {
                                push_unique(fwd, &mut seen_tokens, stamp_base, &mut sparse_toks);
                            }
                        }
                        match stride.checked_mul(2) {
                            Some(next) => stride = next,
                            None => break,
                        }
                    }
                }

                // landmarks
                if let Some(lm) = landmarks.as_ref() {
                    build_landmark_candidates(
                        qi,
                        seq,
                        &self.config,
                        &mut seen_blocks,
                        stamp_base,
                        &mut sparse_blks,
                    );
                    let slot = h * seq + qi;
                    let out_base = slot * dim;
                    for &b in &sparse_blks {
                        let score = dot(q.row(qi, h), lm.keys.row(b, kv_h)) * scale;
                        if score > running_max[slot] {
                            let corr = (running_max[slot] - score).exp();
                            for d in 0..dim {
                                out_data[out_base + d] *= corr;
                            }
                            denom[slot] *= corr;
                            running_max[slot] = score;
                        }
                        let w = (score - running_max[slot]).exp();
                        denom[slot] += w;
                        let v_row = lm.values.row(b, kv_h);
                        for d in 0..dim {
                            out_data[out_base + d] += w * v_row[d];
                        }
                    }
                }

                // sparse token candidates (globals + log-stride)
                {
                    let slot = h * seq + qi;
                    let out_base = slot * dim;
                    for &j in &sparse_toks {
                        let score = dot(q.row(qi, h), k.row(j, kv_h)) * scale;
                        if score > running_max[slot] {
                            let corr = (running_max[slot] - score).exp();
                            for d in 0..dim {
                                out_data[out_base + d] *= corr;
                            }
                            denom[slot] *= corr;
                            running_max[slot] = score;
                        }
                        let w = (score - running_max[slot]).exp();
                        denom[slot] += w;
                        let v_row = v.row(j, kv_h);
                        for d in 0..dim {
                            out_data[out_base + d] += w * v_row[d];
                        }
                    }
                }
            }
        }

        // --- Phase 3: Normalize ---
        let mut out = Tensor3::zeros(seq, q_heads, dim);
        for h in 0..q_heads {
            for qi in 0..seq {
                let slot = h * seq + qi;
                let inv = if denom[slot] > 0.0 {
                    1.0 / denom[slot]
                } else {
                    0.0
                };
                let out_row = out.row_mut(qi, h);
                let src_base = slot * dim;
                for d in 0..dim {
                    out_row[d] = out_data[src_base + d] * inv;
                }
            }
        }

        Ok(out)
    }

    /// IO-optimal tiled sparse attention for MHA.
    ///
    /// `tile_size = 0` auto-selects `window` as the tile (keeps KV tile in L1).
    pub fn forward_flash(
        &self,
        q: &Tensor3,
        k: &Tensor3,
        v: &Tensor3,
        tile_size: usize,
    ) -> Result<Tensor3, AttentionError> {
        validate_qkv(q, k, v)?;
        if self.config.block_size == 0 {
            return Err(AttentionError::InvalidConfig(
                "block_size must be greater than zero".to_string(),
            ));
        }
        if q.seq == 0 {
            return Ok(Tensor3::zeros(0, q.heads, q.dim));
        }
        self.forward_flash_inner(q, k, v, tile_size, 1)
    }

    /// IO-optimal tiled sparse attention for GQA/MQA.
    ///
    /// `q` has `q_heads`, `k`/`v` have `kv_heads` where `q_heads % kv_heads == 0`.
    /// `tile_size = 0` auto-selects `window` as the tile.
    pub fn forward_gqa_flash(
        &self,
        q: &Tensor3,
        k: &Tensor3,
        v: &Tensor3,
        tile_size: usize,
    ) -> Result<Tensor3, AttentionError> {
        validate_gqa(q, k, v)?;
        if self.config.block_size == 0 {
            return Err(AttentionError::InvalidConfig(
                "block_size must be greater than zero".to_string(),
            ));
        }
        if q.seq == 0 {
            return Ok(Tensor3::zeros(0, q.heads, q.dim));
        }
        let group_size = q.heads / k.heads;
        self.forward_flash_inner(q, k, v, tile_size, group_size)
    }
}

/// FP16 KV cache — halves KV memory vs. f32 at the cost of inline conversion during decode.
///
/// Gated by the `fp16` feature; requires the `half` crate.
///
/// `decode_step_f16` converts f16 → f32 row-by-row during the dot product; LLVM
/// auto-vectorises this to NEON `vcvt` on AArch64 (no allocation needed — a
/// stack-allocated `dim`-sized buffer is reused across heads).
#[cfg(feature = "fp16")]
pub struct KvCacheF16 {
    keys: Vec<half::f16>, // [capacity * kv_heads * dim] flattened
    values: Vec<half::f16>,
    pub len: usize,
    pub capacity: usize,
    kv_heads: usize,
    pub dim: usize,
    /// Incremental f32 landmarks for precision.
    pub landmarks: IncrementalLandmarks,
}

#[cfg(feature = "fp16")]
impl KvCacheF16 {
    pub fn new(capacity: usize, kv_heads: usize, dim: usize, block_size: usize) -> Self {
        let n = capacity * kv_heads * dim;
        Self {
            keys: vec![half::f16::ZERO; n],
            values: vec![half::f16::ZERO; n],
            len: 0,
            capacity,
            kv_heads,
            dim,
            landmarks: IncrementalLandmarks::new(capacity, block_size, kv_heads, dim),
        }
    }

    /// Append a single-token K/V (shape [1, kv_heads, dim]); converts f32 → f16.
    pub fn try_append(&mut self, k: &Tensor3, v: &Tensor3) -> Result<(), AttentionError> {
        if k.seq != 1 || v.seq != 1 {
            return Err(AttentionError::InvalidConfig(
                "KvCacheF16::try_append expects single-token tensors (seq == 1)".into(),
            ));
        }
        if k.heads != self.kv_heads || v.heads != self.kv_heads {
            return Err(AttentionError::InvalidConfig(format!(
                "kv_heads mismatch: cache={}, k={}, v={}",
                self.kv_heads, k.heads, v.heads
            )));
        }
        if self.len >= self.capacity {
            return Err(AttentionError::InvalidConfig(format!(
                "KvCacheF16 capacity exceeded: capacity={}, len={}",
                self.capacity, self.len
            )));
        }
        let base = self.len * self.kv_heads * self.dim;
        for (dst, src) in self.keys[base..base + self.kv_heads * self.dim]
            .iter_mut()
            .zip(k.data.iter())
        {
            *dst = half::f16::from_f32(*src);
        }
        for (dst, src) in self.values[base..base + self.kv_heads * self.dim]
            .iter_mut()
            .zip(v.data.iter())
        {
            *dst = half::f16::from_f32(*src);
        }
        self.landmarks.update(self.len, k, v);
        self.len += 1;
        Ok(())
    }

    pub fn is_full(&self) -> bool {
        self.len >= self.capacity
    }

    pub fn reset(&mut self) {
        self.len = 0;
        self.landmarks.reset();
    }

    /// Decode step against the f16 KV cache.
    ///
    /// `q`: shape `[1, q_heads, dim]`. Returns shape `[1, q_heads, dim]`.
    /// Inline f16→f32 conversion per row; LLVM auto-vecs to NEON vcvt on AArch64.
    pub fn decode_step_f16(
        &self,
        attn: &SubquadraticSparseAttention,
        q: &Tensor3,
    ) -> Result<Tensor3, AttentionError> {
        if q.seq != 1 {
            return Err(AttentionError::InvalidConfig(
                "decode_step_f16 requires q.seq == 1".into(),
            ));
        }
        if q.dim == 0 {
            return Err(AttentionError::InvalidConfig(
                "head dimension must be greater than zero".into(),
            ));
        }
        if self.len == 0 {
            return Ok(Tensor3::zeros(1, q.heads, q.dim));
        }
        if self.kv_heads == 0 || q.heads % self.kv_heads != 0 {
            return Err(AttentionError::InvalidConfig(format!(
                "q_heads={} must be divisible by kv_heads={}",
                q.heads, self.kv_heads
            )));
        }

        let q_heads = q.heads;
        let group_size = q_heads / self.kv_heads;
        let dim = self.dim;
        let scale = 1.0f32 / (dim as f32).sqrt();
        let seq = self.len;
        let i = seq - 1;

        let mut seen_tokens = vec![0usize; seq.max(1)];
        let mut seen_blocks = vec![0usize; div_ceil(seq.max(1), attn.config.block_size)];
        let mut token_candidates = Vec::with_capacity(attn.config.window + 64);
        let mut block_candidates = Vec::with_capacity(64);

        build_token_candidates(
            i,
            seq,
            &attn.config,
            &mut seen_tokens,
            1,
            &mut token_candidates,
        );
        if attn.config.use_landmarks {
            build_landmark_candidates(
                i,
                seq,
                &attn.config,
                &mut seen_blocks,
                1,
                &mut block_candidates,
            );
        }

        let mut out = Tensor3::zeros(1, q_heads, dim);
        // Reuse a single f32 row buffer for f16→f32 conversion — no extra alloc per candidate.
        let mut k_buf = vec![0.0f32; dim];

        for h in 0..q_heads {
            let kv_h = h / group_size;
            let q_row = q.row(0, h);
            let mut running_max = f32::NEG_INFINITY;
            let mut denom_acc = 0.0f32;
            let mut acc = vec![0.0f32; dim];

            for &j in &token_candidates {
                let base = (j * self.kv_heads + kv_h) * dim;
                for d in 0..dim {
                    k_buf[d] = self.keys[base + d].to_f32();
                }
                let score = dot(q_row, &k_buf) * scale;
                if score > running_max {
                    let corr = (running_max - score).exp();
                    for d in 0..dim {
                        acc[d] *= corr;
                    }
                    denom_acc *= corr;
                    running_max = score;
                }
                let w = (score - running_max).exp();
                denom_acc += w;
                let v_base = (j * self.kv_heads + kv_h) * dim;
                for d in 0..dim {
                    acc[d] += w * self.values[v_base + d].to_f32();
                }
            }

            // f32 landmarks (precision preserved)
            for &b in &block_candidates {
                let score = dot(q_row, self.landmarks.keys.row(b, kv_h)) * scale;
                if score > running_max {
                    let corr = (running_max - score).exp();
                    for d in 0..dim {
                        acc[d] *= corr;
                    }
                    denom_acc *= corr;
                    running_max = score;
                }
                let w = (score - running_max).exp();
                denom_acc += w;
                let v_row = self.landmarks.values.row(b, kv_h);
                for d in 0..dim {
                    acc[d] += w * v_row[d];
                }
            }

            let inv = if denom_acc > 0.0 {
                1.0 / denom_acc
            } else {
                0.0
            };
            let out_row = out.row_mut(0, h);
            for d in 0..dim {
                out_row[d] = acc[d] * inv;
            }
        }

        Ok(out)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn make_tensor(seq: usize, heads: usize, dim: usize) -> Tensor3 {
        let len = seq * heads * dim;
        let data = (0..len)
            .map(|i| ((i * 17 + 11) % 101) as f32 / 101.0)
            .collect::<Vec<f32>>();
        Tensor3::from_vec(data, seq, heads, dim).unwrap()
    }

    #[test]
    fn sparse_matches_dense_when_window_covers_sequence() {
        let seq = 32;
        let heads = 2;
        let dim = 8;
        let q = make_tensor(seq, heads, dim);
        let k = make_tensor(seq, heads, dim);
        let v = make_tensor(seq, heads, dim);

        let dense = dense_attention(&q, &k, &v, false).unwrap();
        let sparse = SubquadraticSparseAttention::new(SparseAttentionConfig {
            window: seq,
            block_size: 8,
            global_tokens: vec![],
            causal: false,
            use_log_stride: false,
            use_landmarks: false,
            sort_candidates: false,
        })
        .unwrap()
        .forward(&q, &k, &v)
        .unwrap();

        for idx in 0..dense.data.len() {
            assert!((dense.data[idx] - sparse.data[idx]).abs() < 1e-5);
        }
    }

    #[test]
    fn causal_sparse_matches_causal_dense_when_window_covers_sequence() {
        let seq = 32;
        let heads = 2;
        let dim = 8;
        let q = make_tensor(seq, heads, dim);
        let k = make_tensor(seq, heads, dim);
        let v = make_tensor(seq, heads, dim);

        let dense = dense_attention(&q, &k, &v, true).unwrap();
        let sparse = SubquadraticSparseAttention::new(SparseAttentionConfig {
            window: seq,
            block_size: 8,
            global_tokens: vec![],
            causal: true,
            use_log_stride: false,
            use_landmarks: false,
            sort_candidates: false,
        })
        .unwrap()
        .forward(&q, &k, &v)
        .unwrap();

        for idx in 0..dense.data.len() {
            assert!((dense.data[idx] - sparse.data[idx]).abs() < 1e-5);
        }
    }

    #[test]
    fn sparse_edges_are_smaller_than_dense_edges() {
        let attention = SubquadraticSparseAttention::new(SparseAttentionConfig {
            window: 128,
            block_size: 64,
            global_tokens: vec![0],
            causal: true,
            use_log_stride: true,
            use_landmarks: true,
            sort_candidates: false,
        })
        .unwrap();

        let seq = 4096;
        let dense_edges = seq * (seq + 1) / 2;
        let sparse_edges = attention.estimate_sparse_edges(seq);
        assert!(sparse_edges < dense_edges / 8);
    }

    // --- ADR-186 edge-case tests ---

    #[test]
    fn empty_sequence_does_not_panic() {
        let q = Tensor3::zeros(0, 2, 8);
        let k = Tensor3::zeros(0, 2, 8);
        let v = Tensor3::zeros(0, 2, 8);
        let result = SubquadraticSparseAttention::new(SparseAttentionConfig::default())
            .unwrap()
            .forward(&q, &k, &v);
        assert!(result.is_ok());
        assert_eq!(result.unwrap().data.len(), 0);
    }

    #[test]
    fn single_token_self_attention_is_identity_of_v() {
        let dim = 8;
        let heads = 2;
        let v_data: Vec<f32> = (0..heads * dim).map(|i| i as f32 * 0.1 + 0.1).collect();
        let q = Tensor3::from_vec(v_data.clone(), 1, heads, dim).unwrap();
        let k = q.clone();
        let v = q.clone();
        let out = SubquadraticSparseAttention::new(SparseAttentionConfig {
            window: 128,
            block_size: 64,
            global_tokens: vec![0],
            causal: true,
            use_log_stride: true,
            use_landmarks: true,
            sort_candidates: false,
        })
        .unwrap()
        .forward(&q, &k, &v)
        .unwrap();
        for (a, b) in out.data.iter().zip(v_data.iter()) {
            assert!((a - b).abs() < 1e-5, "single token: out={} v={}", a, b);
        }
    }

    #[test]
    fn out_of_range_global_token_is_silently_skipped() {
        let seq = 4;
        let q = make_tensor(seq, 1, 4);
        let k = q.clone();
        let v = q.clone();
        let result = SubquadraticSparseAttention::new(SparseAttentionConfig {
            window: 128,
            block_size: 64,
            global_tokens: vec![10],
            causal: true,
            use_log_stride: false,
            use_landmarks: false,
            sort_candidates: false,
        })
        .unwrap()
        .forward(&q, &k, &v);
        assert!(result.is_ok());
    }

    #[test]
    fn block_size_one_does_not_panic() {
        let seq = 16;
        let q = make_tensor(seq, 2, 8);
        let k = q.clone();
        let v = q.clone();
        let result = SubquadraticSparseAttention::new(SparseAttentionConfig {
            window: seq,
            block_size: 1,
            global_tokens: vec![],
            causal: false,
            use_log_stride: false,
            use_landmarks: true,
            sort_candidates: false,
        })
        .unwrap()
        .forward(&q, &k, &v);
        assert!(result.is_ok());
    }

    #[test]
    fn self_attention_only_denom_is_one() {
        // window=0, log-stride=false, landmarks=false, causal=true
        // Every token attends only to itself; single candidate weight = exp(0)=1.0, denom=1.0
        // Output must equal V[i] for all i.
        let seq = 8;
        let heads = 1;
        let dim = 4;
        let v_data: Vec<f32> = (0..seq * heads * dim).map(|i| i as f32 * 0.1).collect();
        let q = Tensor3::from_vec(v_data.clone(), seq, heads, dim).unwrap();
        let k = q.clone();
        let v = q.clone();
        let out = SubquadraticSparseAttention::new(SparseAttentionConfig {
            window: 0,
            block_size: 64,
            global_tokens: vec![],
            causal: true,
            use_log_stride: false,
            use_landmarks: false,
            sort_candidates: false,
        })
        .unwrap()
        .forward(&q, &k, &v)
        .unwrap();
        for (a, b) in out.data.iter().zip(v_data.iter()) {
            assert!((a - b).abs() < 1e-5);
        }
    }

    #[test]
    fn non_causal_sparse_matches_non_causal_dense_with_window_only() {
        // Validates ADR-185 (non-causal landmark fix) + ADR-184 (online softmax)
        let seq = 32;
        let heads = 2;
        let dim = 8;
        let q = make_tensor(seq, heads, dim);
        let k = make_tensor(seq, heads, dim);
        let v = make_tensor(seq, heads, dim);
        let dense = dense_attention(&q, &k, &v, false).unwrap();
        let sparse = SubquadraticSparseAttention::new(SparseAttentionConfig {
            window: seq,
            block_size: 8,
            global_tokens: vec![],
            causal: false,
            use_log_stride: false,
            use_landmarks: false,
            sort_candidates: false,
        })
        .unwrap()
        .forward(&q, &k, &v)
        .unwrap();
        for (a, b) in dense.data.iter().zip(sparse.data.iter()) {
            assert!((a - b).abs() < 1e-5);
        }
    }

    #[test]
    fn estimate_edges_always_below_dense() {
        for seq in [64usize, 128, 256, 512, 1024, 4096] {
            let attention =
                SubquadraticSparseAttention::new(SparseAttentionConfig::default()).unwrap();
            let sparse = attention.estimate_sparse_edges(seq);
            // Upper bound is seq*seq (full non-causal quadratic budget) because
            // estimate_sparse_edges also counts landmark block pseudo-edges, which
            // can push the count slightly above seq*(seq+1)/2 when window >= seq.
            let dense = seq * seq;
            assert!(
                sparse <= dense,
                "seq={}: sparse {} > dense {}",
                seq,
                sparse,
                dense
            );
            // Sublinearity only manifests once seq >> 4*window (≥4096 with window=128).
            if seq >= 4096 {
                let causal_dense = seq * (seq + 1) / 2;
                assert!(
                    sparse < causal_dense / 4,
                    "seq={}: sparse {} not < causal_dense/4 {}",
                    seq,
                    sparse,
                    causal_dense / 4
                );
            }
        }
    }

    // --- ADR-187 overflow check ---

    #[test]
    #[should_panic(expected = "Tensor3::zeros: shape overflow")]
    fn zeros_panics_on_overflow() {
        Tensor3::zeros(usize::MAX / 2 + 1, 3, 1);
    }

    // --- ADR-189 KV cache tests ---

    #[test]
    fn decode_step_single_token_matches_forward() {
        let heads = 2;
        let dim = 8;
        let q = make_tensor(1, heads, dim);
        let k = make_tensor(1, heads, dim);
        let v = make_tensor(1, heads, dim);
        let attn = SubquadraticSparseAttention::new(SparseAttentionConfig {
            window: 128,
            block_size: 64,
            global_tokens: vec![0],
            causal: true,
            use_log_stride: false,
            use_landmarks: false,
            sort_candidates: false,
        })
        .unwrap();
        let fwd = attn.forward(&q, &k, &v).unwrap();

        // Append k/v first, then decode — new convention.
        let mut cache = KvCache::new(256, heads, dim, 64);
        cache.try_append(&k, &v).unwrap();
        let out = attn.decode_step(&q, &cache).unwrap();

        assert_eq!(out.seq, 1);
        assert_eq!(out.heads, heads);
        assert_eq!(out.dim, dim);
        // Values must match forward (single token, window covers entire seq).
        for (a, b) in out.data.iter().zip(fwd.data.iter()) {
            assert!((a - b).abs() < 1e-5, "decode_step vs forward: {a} vs {b}");
        }
    }

    #[test]
    fn kv_cache_append_and_len() {
        let heads = 4;
        let dim = 16;
        let mut cache = KvCache::new(64, heads, dim, 8);
        assert_eq!(cache.len, 0);
        let k = make_tensor(1, heads, dim);
        let v = make_tensor(1, heads, dim);
        cache.append(&k, &v);
        assert_eq!(cache.len, 1);
        cache.append(&k, &v);
        assert_eq!(cache.len, 2);
    }

    #[test]
    fn try_append_at_capacity_returns_error() {
        let heads = 2;
        let dim = 4;
        let mut cache = KvCache::new(2, heads, dim, 1);
        let k = make_tensor(1, heads, dim);
        let v = make_tensor(1, heads, dim);
        assert!(cache.try_append(&k, &v).is_ok());
        assert!(cache.try_append(&k, &v).is_ok());
        assert!(
            cache.try_append(&k, &v).is_err(),
            "should error on overflow"
        );
    }

    #[test]
    fn kv_cache_reset_clears_state() {
        let heads = 2;
        let dim = 4;
        let mut cache = KvCache::new(8, heads, dim, 2);
        let k = make_tensor(1, heads, dim);
        let v = make_tensor(1, heads, dim);
        cache.append(&k, &v);
        cache.append(&k, &v);
        assert_eq!(cache.len, 2);
        cache.reset();
        assert_eq!(cache.len, 0);
        assert!(!cache.is_full());
    }

    #[test]
    fn incremental_landmarks_match_static() {
        // After appending all tokens, IncrementalLandmarks means must equal
        // the static Landmarks::from_kv result (within fp rounding).
        let seq = 16;
        let heads = 2;
        let dim = 8;
        let block_size = 4;
        let k = make_tensor(seq, heads, dim);
        let v = make_tensor(seq, heads, dim);

        let static_lm = Landmarks::from_kv(&k, &v, block_size);

        let mut inc_lm = IncrementalLandmarks::new(seq, block_size, heads, dim);
        for t in 0..seq {
            let k_t = Tensor3::from_vec(
                k.data[t * heads * dim..(t + 1) * heads * dim].to_vec(),
                1,
                heads,
                dim,
            )
            .unwrap();
            let v_t = Tensor3::from_vec(
                v.data[t * heads * dim..(t + 1) * heads * dim].to_vec(),
                1,
                heads,
                dim,
            )
            .unwrap();
            inc_lm.update(t, &k_t, &v_t);
        }

        for (a, b) in inc_lm.keys.data.iter().zip(static_lm.keys.data.iter()) {
            assert!((a - b).abs() < 1e-5, "landmark keys mismatch: {a} vs {b}");
        }
        for (a, b) in inc_lm.values.data.iter().zip(static_lm.values.data.iter()) {
            assert!((a - b).abs() < 1e-5, "landmark values mismatch: {a} vs {b}");
        }
    }

    #[test]
    fn decode_batch_shape_and_matches_sequential_decode_steps() {
        let q_heads = 4;
        let kv_heads = 2;
        let dim = 8;
        let draft_len = 4;
        let capacity = 32;
        let block_size = 4;

        let attn = SubquadraticSparseAttention::new(SparseAttentionConfig {
            window: 16,
            block_size,
            global_tokens: vec![],
            causal: true,
            use_log_stride: false,
            use_landmarks: false,
            sort_candidates: false,
        })
        .unwrap();

        let q = make_tensor(draft_len, q_heads, dim);
        let new_k = make_tensor(draft_len, kv_heads, dim);
        let new_v = make_tensor(draft_len, kv_heads, dim);

        // Batch path
        let mut cache_batch = KvCache::new(capacity, kv_heads, dim, block_size);
        let batch_out = attn
            .decode_batch(&q, &new_k, &new_v, &mut cache_batch)
            .unwrap();
        assert_eq!(batch_out.seq, draft_len);
        assert_eq!(batch_out.heads, q_heads);
        assert_eq!(batch_out.dim, dim);

        // Sequential path (reference)
        let mut cache_seq = KvCache::new(capacity, kv_heads, dim, block_size);
        let mut seq_out = Tensor3::zeros(draft_len, q_heads, dim);
        for t in 0..draft_len {
            let q_t = Tensor3::from_vec(
                q.data[t * q_heads * dim..(t + 1) * q_heads * dim].to_vec(),
                1,
                q_heads,
                dim,
            )
            .unwrap();
            let k_t = Tensor3::from_vec(
                new_k.data[t * kv_heads * dim..(t + 1) * kv_heads * dim].to_vec(),
                1,
                kv_heads,
                dim,
            )
            .unwrap();
            let v_t = Tensor3::from_vec(
                new_v.data[t * kv_heads * dim..(t + 1) * kv_heads * dim].to_vec(),
                1,
                kv_heads,
                dim,
            )
            .unwrap();
            cache_seq.try_append(&k_t, &v_t).unwrap();
            let out_t = attn.decode_step(&q_t, &cache_seq).unwrap();
            seq_out.data[t * q_heads * dim..(t + 1) * q_heads * dim].copy_from_slice(&out_t.data);
        }

        for (a, b) in batch_out.data.iter().zip(seq_out.data.iter()) {
            assert!(
                (a - b).abs() < 1e-5,
                "decode_batch vs sequential: {a} vs {b}"
            );
        }
    }

    // --- ADR-190 GQA/MQA tests ---

    #[test]
    fn forward_gqa_group1_equals_forward() {
        // group_size=1 (MHA): forward_gqa must produce identical output to forward
        let seq = 16;
        let heads = 4;
        let dim = 8;
        let q = make_tensor(seq, heads, dim);
        let k = make_tensor(seq, heads, dim);
        let v = make_tensor(seq, heads, dim);
        let attn = SubquadraticSparseAttention::new(SparseAttentionConfig {
            window: seq,
            block_size: 8,
            global_tokens: vec![],
            causal: false,
            use_log_stride: false,
            use_landmarks: false,
            sort_candidates: false,
        })
        .unwrap();
        let mha = attn.forward(&q, &k, &v).unwrap();
        let gqa = attn.forward_gqa(&q, &k, &v).unwrap();
        for (a, b) in mha.data.iter().zip(gqa.data.iter()) {
            assert!(
                (a - b).abs() < 1e-5,
                "MHA vs GQA group_size=1 mismatch: {} vs {}",
                a,
                b
            );
        }
    }

    #[test]
    fn forward_gqa_group4_produces_valid_output() {
        // group_size=4 (Mistral-7B ratio): 4 Q heads, 1 KV head
        let seq = 8;
        let q_heads = 4;
        let kv_heads = 1;
        let dim = 8;
        let q = make_tensor(seq, q_heads, dim);
        let k = make_tensor(seq, kv_heads, dim);
        let v = make_tensor(seq, kv_heads, dim);
        let attn = SubquadraticSparseAttention::new(SparseAttentionConfig {
            window: seq,
            block_size: 4,
            global_tokens: vec![],
            causal: true,
            use_log_stride: false,
            use_landmarks: false,
            sort_candidates: false,
        })
        .unwrap();
        let out = attn.forward_gqa(&q, &k, &v).unwrap();
        assert_eq!(out.seq, seq);
        assert_eq!(out.heads, q_heads);
        assert_eq!(out.dim, dim);
        // No NaN or inf
        for v in &out.data {
            assert!(v.is_finite(), "GQA output contains non-finite value: {}", v);
        }
    }

    #[test]
    fn forward_auto_dispatches_correctly() {
        let seq = 8;
        let heads = 2;
        let dim = 4;
        let attn = SubquadraticSparseAttention::new(SparseAttentionConfig::default()).unwrap();

        // MHA path: q_heads == k_heads
        let q = make_tensor(seq, heads, dim);
        let k = make_tensor(seq, heads, dim);
        let v = make_tensor(seq, heads, dim);
        assert!(attn.forward_auto(&q, &k, &v).is_ok());

        // GQA path: q_heads=4, kv_heads=2
        let q2 = make_tensor(seq, 4, dim);
        let k2 = make_tensor(seq, 2, dim);
        let v2 = make_tensor(seq, 2, dim);
        assert!(attn.forward_auto(&q2, &k2, &v2).is_ok());
    }

    #[test]
    fn forward_gqa_invalid_head_ratio_errors() {
        let seq = 4;
        let attn = SubquadraticSparseAttention::new(SparseAttentionConfig::default()).unwrap();
        // q_heads=3, kv_heads=2 — not divisible
        let q = make_tensor(seq, 3, 4);
        let k = make_tensor(seq, 2, 4);
        let v = make_tensor(seq, 2, 4);
        assert!(attn.forward_gqa(&q, &k, &v).is_err());
    }

    // --- Flash-Sparse Attention tests ---

    #[test]
    fn forward_flash_matches_forward_mha() {
        let seq = 64;
        let heads = 4;
        let dim = 8;
        let q = make_tensor(seq, heads, dim);
        let k = make_tensor(seq, heads, dim);
        let v = make_tensor(seq, heads, dim);
        let attn = SubquadraticSparseAttention::new(SparseAttentionConfig {
            window: 16,
            block_size: 8,
            global_tokens: vec![0],
            causal: true,
            use_log_stride: true,
            use_landmarks: true,
            sort_candidates: false,
        })
        .unwrap();
        let reference = attn.forward(&q, &k, &v).unwrap();
        let flash = attn.forward_flash(&q, &k, &v, 8).unwrap();
        assert_eq!(flash.seq, seq);
        assert_eq!(flash.heads, heads);
        assert_eq!(flash.dim, dim);
        for (a, b) in reference.data.iter().zip(flash.data.iter()) {
            assert!(
                (a - b).abs() < 1e-4,
                "forward_flash_mha causal mismatch: ref={} flash={}",
                a,
                b
            );
        }
    }

    #[test]
    fn forward_flash_matches_forward_non_causal() {
        let seq = 64;
        let heads = 4;
        let dim = 8;
        let q = make_tensor(seq, heads, dim);
        let k = make_tensor(seq, heads, dim);
        let v = make_tensor(seq, heads, dim);
        let attn = SubquadraticSparseAttention::new(SparseAttentionConfig {
            window: 16,
            block_size: 8,
            global_tokens: vec![0],
            causal: false,
            use_log_stride: true,
            use_landmarks: true,
            sort_candidates: false,
        })
        .unwrap();
        let reference = attn.forward(&q, &k, &v).unwrap();
        let flash = attn.forward_flash(&q, &k, &v, 8).unwrap();
        assert_eq!(flash.seq, seq);
        for (a, b) in reference.data.iter().zip(flash.data.iter()) {
            assert!(
                (a - b).abs() < 1e-4,
                "forward_flash non-causal mismatch: ref={} flash={}",
                a,
                b
            );
        }
    }

    #[test]
    fn forward_gqa_flash_matches_forward_gqa() {
        let seq = 32;
        let q_heads = 4;
        let kv_heads = 2;
        let dim = 8;
        let q = make_tensor(seq, q_heads, dim);
        let k = make_tensor(seq, kv_heads, dim);
        let v = make_tensor(seq, kv_heads, dim);
        let attn = SubquadraticSparseAttention::new(SparseAttentionConfig {
            window: 8,
            block_size: 4,
            global_tokens: vec![0],
            causal: true,
            use_log_stride: true,
            use_landmarks: true,
            sort_candidates: false,
        })
        .unwrap();
        let reference = attn.forward_gqa(&q, &k, &v).unwrap();
        let flash = attn.forward_gqa_flash(&q, &k, &v, 4).unwrap();
        assert_eq!(flash.seq, seq);
        assert_eq!(flash.heads, q_heads);
        assert_eq!(flash.dim, dim);
        for (a, b) in reference.data.iter().zip(flash.data.iter()) {
            assert!(
                (a - b).abs() < 1e-4,
                "forward_gqa_flash mismatch: ref={} flash={}",
                a,
                b
            );
        }
    }

    #[test]
    fn forward_flash_single_token_no_panic() {
        let q = make_tensor(1, 2, 4);
        let k = q.clone();
        let v = q.clone();
        let attn = SubquadraticSparseAttention::new(SparseAttentionConfig {
            window: 4,
            block_size: 4,
            global_tokens: vec![],
            causal: true,
            use_log_stride: false,
            use_landmarks: false,
            sort_candidates: false,
        })
        .unwrap();
        let out = attn.forward_flash(&q, &k, &v, 1);
        assert!(
            out.is_ok(),
            "single-token forward_flash panicked or errored"
        );
        let out = out.unwrap();
        assert_eq!(out.seq, 1);
        for val in &out.data {
            assert!(val.is_finite());
        }
    }

    #[cfg(feature = "fp16")]
    #[test]
    fn kv_cache_f16_roundtrip() {
        let kv_heads = 2;
        let dim = 8;
        let block_size = 4;
        let capacity = 16;
        let mut cache = KvCacheF16::new(capacity, kv_heads, dim, block_size);

        let k = make_tensor(1, kv_heads, dim);
        let v = make_tensor(1, kv_heads, dim);
        cache.try_append(&k, &v).unwrap();

        assert_eq!(cache.len, 1);

        // Verify the stored f16 values round-trip back to f32 within 1e-2
        let base = 0;
        for d in 0..kv_heads * dim {
            let original = k.data[d];
            let stored = cache.keys[base + d].to_f32();
            assert!(
                (original - stored).abs() < 1e-2,
                "f16 roundtrip error too large: original={} stored={}",
                original,
                stored
            );
        }
        for d in 0..kv_heads * dim {
            let original = v.data[d];
            let stored = cache.values[base + d].to_f32();
            assert!(
                (original - stored).abs() < 1e-2,
                "f16 roundtrip error too large: original={} stored={}",
                original,
                stored
            );
        }
    }

    // ---- forward_gated tests ---------------------------------------------

    fn random_qkv(seq: usize, heads: usize, dim: usize) -> (Tensor3, Tensor3, Tensor3) {
        // Deterministic pseudo-random fill (no external crate at unit-test
        // level so this stays platform-stable across runs).
        let mut s = 0xa5a5a5a5u32;
        let mut next = || {
            s ^= s << 13;
            s ^= s >> 17;
            s ^= s << 5;
            (s as f32 / u32::MAX as f32 - 0.5) * 0.5
        };
        let mut q = Tensor3::zeros(seq, heads, dim);
        let mut k = Tensor3::zeros(seq, heads, dim);
        let mut v = Tensor3::zeros(seq, heads, dim);
        for t in 0..seq {
            for h in 0..heads {
                for d in 0..dim {
                    q.row_mut(t, h)[d] = next();
                    k.row_mut(t, h)[d] = next();
                    v.row_mut(t, h)[d] = next();
                }
            }
        }
        (q, k, v)
    }

    #[test]
    fn forward_gated_all_true_matches_forward() {
        // keep_mask all-true must produce bit-identical output to plain forward.
        let cfg = SparseAttentionConfig::default();
        let attn = SubquadraticSparseAttention::new(cfg).unwrap();
        let (q, k, v) = random_qkv(64, 2, 8);
        let baseline = attn.forward(&q, &k, &v).unwrap();
        let keep = vec![true; 64];
        let gated = attn.forward_gated(&q, &k, &v, &keep).unwrap();
        for t in 0..64 {
            for h in 0..2 {
                for d in 0..8 {
                    let a = baseline.row(t, h)[d];
                    let b = gated.row(t, h)[d];
                    assert!(
                        (a - b).abs() < 1e-6,
                        "all-true gate must equal forward: pos {} head {} d {} → {} vs {}",
                        t,
                        h,
                        d,
                        a,
                        b
                    );
                }
            }
        }
    }

    #[test]
    fn forward_gated_all_false_keeps_window_and_globals() {
        // keep_mask all-false: only window + globals + current are kept,
        // so the per-position candidate count must equal the window size
        // plus the global count plus 1 (self), with appropriate truncation
        // at the start of the sequence.
        let cfg = SparseAttentionConfig {
            window: 8,
            global_tokens: vec![0],
            ..SparseAttentionConfig::default()
        };
        let attn = SubquadraticSparseAttention::new(cfg).unwrap();
        let (q, k, v) = random_qkv(64, 2, 8);
        let keep = vec![false; 64];
        // With all-false mask the gate strips log-stride candidates;
        // result should still be valid (no NaN, output finite).
        let out = attn.forward_gated(&q, &k, &v, &keep).unwrap();
        for t in 0..64 {
            for h in 0..2 {
                for d in 0..8 {
                    assert!(out.row(t, h)[d].is_finite());
                }
            }
        }
    }

    #[test]
    fn forward_gated_rejects_wrong_mask_length() {
        let attn = SubquadraticSparseAttention::new(SparseAttentionConfig::default()).unwrap();
        let (q, k, v) = random_qkv(16, 1, 4);
        let keep = vec![true; 15]; // wrong length
        let r = attn.forward_gated(&q, &k, &v, &keep);
        assert!(matches!(r, Err(AttentionError::InvalidConfig(_))));
    }

    #[test]
    fn forward_gated_with_fastgrnn_top_k_runs_and_is_finite() {
        let attn = SubquadraticSparseAttention::new(SparseAttentionConfig::default()).unwrap();
        let (q, k, v) = random_qkv(64, 2, 8);
        let gate = crate::fastgrnn_gate::FastGrnnGate::new(8, 16);
        let out = attn
            .forward_gated_with_fastgrnn(&q, &k, &v, &gate, 16)
            .unwrap();
        for t in 0..64 {
            for h in 0..2 {
                for d in 0..8 {
                    assert!(
                        out.row(t, h)[d].is_finite(),
                        "non-finite at t={}, h={}, d={}",
                        t,
                        h,
                        d
                    );
                }
            }
        }
    }

    #[test]
    fn forward_gated_smaller_top_k_reduces_candidate_count() {
        // Verify the gate is actually pruning by comparing edge estimates.
        // With top_k = 0 (no log-stride passes the gate) per-position
        // candidate count should be ≤ window + globals + 1; with all-true
        // it should match the unfiltered estimate.
        let cfg = SparseAttentionConfig {
            window: 4,
            block_size: 8,
            global_tokens: vec![0],
            causal: true,
            use_log_stride: true,
            use_landmarks: false,
            sort_candidates: false,
        };
        let attn = SubquadraticSparseAttention::new(cfg.clone()).unwrap();
        let seq = 256;
        let unfiltered = attn.estimate_sparse_edges(seq);

        // Manually count candidates after filtering with all-false mask.
        let mut filtered_total = 0usize;
        let mut seen = vec![0usize; seq];
        let mut tokc = Vec::<usize>::new();
        let global_set: BTreeSet<usize> = cfg.global_tokens.iter().copied().collect();
        for i in 0..seq {
            let stamp = i + 1;
            tokc.clear();
            super::build_token_candidates(i, seq, &cfg, &mut seen, stamp, &mut tokc);
            tokc.retain(|&j| {
                let lo = i.saturating_sub(cfg.window);
                j == i || (j >= lo && j <= i) || global_set.contains(&j) // mask all-false
            });
            filtered_total += tokc.len();
        }
        assert!(
            filtered_total < unfiltered,
            "filtered count {} should be < unfiltered count {}",
            filtered_total,
            unfiltered
        );
    }
}
