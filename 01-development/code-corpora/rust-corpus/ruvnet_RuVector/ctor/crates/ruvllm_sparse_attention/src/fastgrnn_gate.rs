//! FastGRNN salience gate for near-linear attention.
//!
//! A FastGRNN cell is run as a recurrent O(N · D_h²) pass over the token
//! sequence, producing a per-position salience score. The score is
//! intended to feed `SubquadraticSparseAttention::forward_gated`, which
//! drops below-threshold tokens from the attention candidate set.
//!
//! Combined cost:
//!   FastGRNN forward         : O(N · (D_in · D_h + D_h²))
//!   Sparse attention (gated) : O(N · (W + K_keep + B))
//!   Total                    : O(N) for fixed D_h, W, K_keep, B.
//!
//! Cell math (matches cognitum-agent's `sparse_fastgrnn.rs`):
//!   g  = σ(W_g · x + U_g · h + b_g)
//!   c  = tanh(W_u · x + U_u · h + b_u)
//!   gf = clamp(ζ·g + ν, 0, 1)
//!   h' = gf ⊙ h + (1 − gf) ⊙ c
//!
//! Salience at position i is L2(h_i). EMA baseline tracking is left
//! to the caller (this crate is stateless w.r.t. session-level
//! anomaly baselines — it just produces salience).

#[cfg(not(feature = "std"))]
use crate::no_std_math::F32Ext as _;
use crate::tensor::Tensor3;
use alloc::format;
use alloc::string::String;
use alloc::vec;
use alloc::vec::Vec;
use core::cmp::Ordering;

/// Default gate hidden width — small enough that FastGRNN forward is
/// negligible compared to attention, large enough for useful temporal
/// integration. Yields ~4 KB of weights at `input_dim = 32`.
pub const DEFAULT_HIDDEN_DIM: usize = 32;

/// FastGRNN cell used as a per-token salience gate.
///
/// Stateless across sequences: every `score_sequence` call starts from
/// a zero hidden state. For streaming use cases that span sequences,
/// hold a `score_streaming` iterator across calls instead.
#[derive(Clone, Debug)]
pub struct FastGrnnGate {
    pub input_dim: usize,
    pub hidden_dim: usize,
    w_gate: Vec<f32>,
    u_gate: Vec<f32>,
    w_update: Vec<f32>,
    u_update: Vec<f32>,
    bias_gate: Vec<f32>,
    bias_update: Vec<f32>,
    zeta: f32,
    nu: f32,
}

impl FastGrnnGate {
    /// Initialize with deterministic Xavier-scaled weights (no rand dep).
    /// Same seeding pattern as `cognitum-agent::sparse_fastgrnn` so weights
    /// computed there can be loaded here via `from_weights`.
    pub fn new(input_dim: usize, hidden_dim: usize) -> Self {
        assert!(input_dim > 0 && hidden_dim > 0, "dims must be > 0");
        let scale = (2.0 / (input_dim + hidden_dim) as f32).sqrt();
        let seed_w = |i: usize| -> f32 {
            let r = ((i as u32).wrapping_mul(2654435761).wrapping_add(0x9e3779b9)) as f32;
            (r / u32::MAX as f32 - 0.5) * 2.0 * scale * 0.1
        };
        let n_in_h = input_dim * hidden_dim;
        let n_h_h = hidden_dim * hidden_dim;
        Self {
            input_dim,
            hidden_dim,
            w_gate: (0..n_in_h).map(|i| seed_w(i)).collect(),
            u_gate: (0..n_h_h).map(|i| seed_w(i + 1000)).collect(),
            w_update: (0..n_in_h).map(|i| seed_w(i + 2000)).collect(),
            u_update: (0..n_h_h).map(|i| seed_w(i + 3000)).collect(),
            bias_gate: vec![0.0; hidden_dim],
            bias_update: vec![0.0; hidden_dim],
            zeta: 1.0,
            nu: 0.0,
        }
    }

    /// Construct from caller-provided weights (e.g. trained externally
    /// or imported from cognitum-agent's `to_json`). Weights are owned
    /// (moved) — no copy.
    #[allow(clippy::too_many_arguments)]
    pub fn from_weights(
        input_dim: usize,
        hidden_dim: usize,
        w_gate: Vec<f32>,
        u_gate: Vec<f32>,
        w_update: Vec<f32>,
        u_update: Vec<f32>,
        bias_gate: Vec<f32>,
        bias_update: Vec<f32>,
        zeta: f32,
        nu: f32,
    ) -> Result<Self, String> {
        let n_in_h = input_dim * hidden_dim;
        let n_h_h = hidden_dim * hidden_dim;
        if w_gate.len() != n_in_h {
            return Err(format!("w_gate len {} != {}", w_gate.len(), n_in_h));
        }
        if u_gate.len() != n_h_h {
            return Err(format!("u_gate len {} != {}", u_gate.len(), n_h_h));
        }
        if w_update.len() != n_in_h {
            return Err(format!("w_update len {} != {}", w_update.len(), n_in_h));
        }
        if u_update.len() != n_h_h {
            return Err(format!("u_update len {} != {}", u_update.len(), n_h_h));
        }
        if bias_gate.len() != hidden_dim {
            return Err("bias_gate len != hidden_dim".into());
        }
        if bias_update.len() != hidden_dim {
            return Err("bias_update len != hidden_dim".into());
        }
        Ok(Self {
            input_dim,
            hidden_dim,
            w_gate,
            u_gate,
            w_update,
            u_update,
            bias_gate,
            bias_update,
            zeta,
            nu,
        })
    }

    /// Score a sequence of `seq` token feature vectors, each of length
    /// `input_dim`. Returns a `seq`-length salience vector (L2 norm of
    /// hidden state at each step). O(seq · D_h²).
    pub fn score_sequence(&self, tokens: &[Vec<f32>]) -> Vec<f32> {
        let seq = tokens.len();
        let mut salience = Vec::with_capacity(seq);
        let mut h = vec![0.0f32; self.hidden_dim];
        for x in tokens {
            debug_assert_eq!(x.len(), self.input_dim, "token feature dim mismatch");
            let h_new = self.step(x, &h);
            salience.push(l2_norm(&h_new));
            h = h_new;
        }
        salience
    }

    /// Convenience: derive a per-position feature vector by mean-pooling
    /// `k` across heads, then score. `input_dim` must equal `k.dim`.
    /// Cost: O(seq · (heads · dim + D_h²)).
    pub fn score_kv(&self, k: &Tensor3) -> Vec<f32> {
        assert_eq!(self.input_dim, k.dim, "gate input_dim must equal k.dim");
        let seq = k.seq;
        let mut tokens: Vec<Vec<f32>> = Vec::with_capacity(seq);
        let inv_h = 1.0 / k.heads as f32;
        for i in 0..seq {
            let mut pooled = vec![0.0f32; k.dim];
            for h in 0..k.heads {
                let row = k.row(i, h);
                for d in 0..k.dim {
                    pooled[d] += row[d] * inv_h;
                }
            }
            tokens.push(pooled);
        }
        self.score_sequence(&tokens)
    }

    /// Streaming variant: caller maintains the hidden state externally
    /// (so successive sequences can share state for online inference).
    /// Returns `(new_hidden, salience)`. Cost: O(D_h²).
    pub fn step_with_hidden(&self, x: &[f32], h: &[f32]) -> (Vec<f32>, f32) {
        debug_assert_eq!(x.len(), self.input_dim);
        debug_assert_eq!(h.len(), self.hidden_dim);
        let h_new = self.step(x, h);
        let s = l2_norm(&h_new);
        (h_new, s)
    }

    /// Build a binary keep-mask from a salience vector at the given
    /// quantile (e.g. `quantile = 0.75` keeps the top 25% by salience).
    /// `quantile` clamped to [0, 1]. The current position itself
    /// receives `keep[i] = true` regardless (window must always include
    /// the current token).
    pub fn keep_mask_quantile(salience: &[f32], quantile: f32) -> Vec<bool> {
        let n = salience.len();
        if n == 0 {
            return Vec::new();
        }
        let q = quantile.clamp(0.0, 1.0);
        let mut sorted: Vec<f32> = salience.iter().copied().collect();
        // partial_cmp can return None for NaN — sort treating NaN as smallest.
        sorted.sort_by(|a, b| a.partial_cmp(b).unwrap_or(Ordering::Equal));
        let idx = ((n as f32) * q) as usize;
        let threshold = sorted[idx.min(n - 1)];
        salience.iter().map(|&s| s >= threshold).collect()
    }

    /// Build a keep-mask that retains the top-K salient positions.
    /// Ties broken by lower index (stable). `k = 0` returns all-false;
    /// `k >= n` returns all-true.
    pub fn keep_mask_top_k(salience: &[f32], k: usize) -> Vec<bool> {
        let n = salience.len();
        let mut keep = vec![false; n];
        if k == 0 {
            return keep;
        }
        if k >= n {
            keep.iter_mut().for_each(|b| *b = true);
            return keep;
        }
        let mut idx: Vec<usize> = (0..n).collect();
        idx.sort_by(|&a, &b| {
            salience[b]
                .partial_cmp(&salience[a])
                .unwrap_or(Ordering::Equal)
                .then(a.cmp(&b))
        });
        for &i in idx.iter().take(k) {
            keep[i] = true;
        }
        keep
    }

    // ── Internal cell computation ──────────────────────────────────────

    fn step(&self, x: &[f32], h: &[f32]) -> Vec<f32> {
        let d = self.hidden_dim;
        let mut gate = vec![0.0f32; d];
        let mut update = vec![0.0f32; d];
        matmul_add(&self.w_gate, x, &mut gate, self.input_dim);
        matmul_add(&self.u_gate, h, &mut gate, self.hidden_dim);
        matmul_add(&self.w_update, x, &mut update, self.input_dim);
        matmul_add(&self.u_update, h, &mut update, self.hidden_dim);
        let mut h_new = vec![0.0f32; d];
        for i in 0..d {
            let g = sigmoid(gate[i] + self.bias_gate[i]);
            let c = (update[i] + self.bias_update[i]).tanh();
            let gf = (self.zeta * g + self.nu).clamp(0.0, 1.0);
            h_new[i] = gf * h[i] + (1.0 - gf) * c;
        }
        h_new
    }
}

#[inline]
fn matmul_add(weights: &[f32], input: &[f32], result: &mut [f32], cols: usize) {
    let rows = result.len();
    for i in 0..rows {
        let row_off = i * cols;
        let mut s = result[i];
        for j in 0..cols {
            s += weights[row_off + j] * input[j];
        }
        result[i] = s;
    }
}

#[inline]
fn sigmoid(x: f32) -> f32 {
    1.0 / (1.0 + (-x).exp())
}

#[inline]
fn l2_norm(v: &[f32]) -> f32 {
    v.iter().map(|x| x * x).sum::<f32>().sqrt()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_gate_score_sequence_shape() {
        let g = FastGrnnGate::new(8, 16);
        let tokens: Vec<Vec<f32>> = (0..32)
            .map(|t| (0..8).map(|d| (t * 8 + d) as f32 * 0.01).collect())
            .collect();
        let s = g.score_sequence(&tokens);
        assert_eq!(s.len(), 32);
        assert!(s.iter().all(|&v| v.is_finite() && v >= 0.0));
    }

    #[test]
    fn test_gate_score_sequence_deterministic() {
        let g = FastGrnnGate::new(4, 8);
        let tokens: Vec<Vec<f32>> = (0..16).map(|t| vec![t as f32 * 0.05; 4]).collect();
        let a = g.score_sequence(&tokens);
        let b = g.score_sequence(&tokens);
        assert_eq!(a, b);
    }

    #[test]
    fn test_gate_zero_input_zero_baseline() {
        // All-zero input must produce a hidden state that grows in a
        // predictable, monotone-ish way driven by the bias-free `c = tanh(0) = 0`
        // term and `gf` being roughly 0.5 — h stays near zero.
        let g = FastGrnnGate::new(4, 8);
        let tokens = vec![vec![0.0f32; 4]; 32];
        let s = g.score_sequence(&tokens);
        assert!(
            s.iter().all(|&v| v < 0.1),
            "zero input should keep salience near zero, got {:?}",
            s
        );
    }

    #[test]
    fn test_keep_mask_quantile_top_quartile() {
        let salience = vec![0.1, 0.5, 0.9, 0.2, 0.8, 0.3, 0.7, 0.4];
        let keep = FastGrnnGate::keep_mask_quantile(&salience, 0.75);
        // Top 25% (≥ index 6 of sorted [0.1,0.2,0.3,0.4,0.5,0.7,0.8,0.9]) — threshold = 0.8
        // Values ≥ 0.8: 0.9 and 0.8 → 2 elements
        let kept: usize = keep.iter().filter(|&&b| b).count();
        assert_eq!(kept, 2, "keep = {:?}", keep);
        assert!(keep[2]); // 0.9
        assert!(keep[4]); // 0.8
    }

    #[test]
    fn test_keep_mask_top_k() {
        let salience = vec![0.1, 0.5, 0.9, 0.2, 0.8, 0.3, 0.7, 0.4];
        let keep = FastGrnnGate::keep_mask_top_k(&salience, 3);
        // Top 3 by salience: 0.9 (idx 2), 0.8 (idx 4), 0.7 (idx 6)
        assert!(keep[2] && keep[4] && keep[6]);
        let kept: usize = keep.iter().filter(|&&b| b).count();
        assert_eq!(kept, 3);
    }

    #[test]
    fn test_keep_mask_top_k_edges() {
        let salience = vec![0.1, 0.2, 0.3];
        assert!(FastGrnnGate::keep_mask_top_k(&salience, 0)
            .iter()
            .all(|&b| !b));
        assert!(FastGrnnGate::keep_mask_top_k(&salience, 99)
            .iter()
            .all(|&b| b));
    }

    #[test]
    fn test_from_weights_validates_shapes() {
        let r = FastGrnnGate::from_weights(
            4,
            8,
            vec![0.0; 32], // 4*8 ok
            vec![0.0; 64], // 8*8 ok
            vec![0.0; 32],
            vec![0.0; 64],
            vec![0.0; 8],
            vec![0.0; 8],
            1.0,
            0.0,
        );
        assert!(r.is_ok());
        let bad = FastGrnnGate::from_weights(
            4,
            8,
            vec![0.0; 7], // wrong
            vec![0.0; 64],
            vec![0.0; 32],
            vec![0.0; 64],
            vec![0.0; 8],
            vec![0.0; 8],
            1.0,
            0.0,
        );
        assert!(bad.is_err());
    }

    #[test]
    fn test_step_with_hidden_advances_state() {
        let g = FastGrnnGate::new(4, 8);
        let h0 = vec![0.0; 8];
        let x = vec![0.5; 4];
        let (h1, _) = g.step_with_hidden(&x, &h0);
        let (h2, _) = g.step_with_hidden(&x, &h1);
        // h1 should differ from h0; h2 from h1.
        assert!(h0.iter().zip(h1.iter()).any(|(a, b)| a != b));
        assert!(h1.iter().zip(h2.iter()).any(|(a, b)| a != b));
    }
}
