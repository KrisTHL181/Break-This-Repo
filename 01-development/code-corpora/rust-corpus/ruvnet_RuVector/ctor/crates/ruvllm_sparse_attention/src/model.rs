use crate::attention::{
    AttentionBackend, AttentionError, SparseAttentionConfig, SubquadraticSparseAttention,
};
use crate::tensor::Tensor3;

#[derive(Clone, Debug)]
pub struct RuvLlmSparseBlockConfig {
    pub attention: SparseAttentionConfig,
}

impl Default for RuvLlmSparseBlockConfig {
    fn default() -> Self {
        Self {
            attention: SparseAttentionConfig::default(),
        }
    }
}

#[derive(Clone, Debug)]
pub struct RuvLlmSparseBlock {
    attention: SubquadraticSparseAttention,
}

impl RuvLlmSparseBlock {
    pub fn new(config: RuvLlmSparseBlockConfig) -> Result<Self, AttentionError> {
        Ok(Self {
            attention: SubquadraticSparseAttention::new(config.attention)?,
        })
    }

    pub fn forward_qkv(
        &self,
        q: &Tensor3,
        k: &Tensor3,
        v: &Tensor3,
    ) -> Result<Tensor3, AttentionError> {
        self.attention.forward(q, k, v)
    }

    pub fn attention_backend(&self) -> &SubquadraticSparseAttention {
        &self.attention
    }
}
