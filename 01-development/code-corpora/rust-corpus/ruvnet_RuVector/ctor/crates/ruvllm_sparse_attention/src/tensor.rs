use alloc::format;
use alloc::string::{String, ToString};
use alloc::vec;
use alloc::vec::Vec;

#[derive(Clone, Debug, PartialEq)]
pub struct Tensor3 {
    pub data: Vec<f32>,
    pub seq: usize,
    pub heads: usize,
    pub dim: usize,
}

impl Tensor3 {
    pub fn zeros(seq: usize, heads: usize, dim: usize) -> Self {
        let len = seq
            .checked_mul(heads)
            .and_then(|v| v.checked_mul(dim))
            .unwrap_or_else(|| {
                panic!(
                    "Tensor3::zeros: shape overflow seq={} heads={} dim={}",
                    seq, heads, dim
                )
            });
        Self {
            data: vec![0.0; len],
            seq,
            heads,
            dim,
        }
    }

    pub fn from_vec(data: Vec<f32>, seq: usize, heads: usize, dim: usize) -> Result<Self, String> {
        let expected = seq
            .checked_mul(heads)
            .and_then(|v| v.checked_mul(dim))
            .ok_or_else(|| "tensor shape overflow".to_string())?;

        if data.len() != expected {
            return Err(format!(
                "data length mismatch, got {}, expected {}",
                data.len(),
                expected
            ));
        }

        Ok(Self {
            data,
            seq,
            heads,
            dim,
        })
    }

    #[inline]
    pub fn idx(&self, token: usize, head: usize, dim: usize) -> usize {
        ((token * self.heads + head) * self.dim) + dim
    }

    #[inline]
    pub fn row_offset(&self, token: usize, head: usize) -> usize {
        (token * self.heads + head) * self.dim
    }

    #[inline]
    pub fn row(&self, token: usize, head: usize) -> &[f32] {
        let start = self.row_offset(token, head);
        &self.data[start..start + self.dim]
    }

    #[inline]
    pub fn row_mut(&mut self, token: usize, head: usize) -> &mut [f32] {
        let start = self.row_offset(token, head);
        &mut self.data[start..start + self.dim]
    }

    #[inline]
    pub fn get(&self, token: usize, head: usize, dim: usize) -> f32 {
        self.data[self.idx(token, head, dim)]
    }

    #[inline]
    pub fn set(&mut self, token: usize, head: usize, dim: usize, value: f32) {
        let idx = self.idx(token, head, dim);
        self.data[idx] = value;
    }

    pub fn shape(&self) -> (usize, usize, usize) {
        (self.seq, self.heads, self.dim)
    }
}
