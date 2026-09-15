//! Embedding service with tokenization and caching
//!
//! Provides text-to-vector conversion with LRU caching for efficiency.
//!
//! Two backends are available:
//! - **Random** (default): a random-init character-level matrix + sinusoidal
//!   positions + mean-pooling. Fast, requires no download, but **not
//!   semantic** — it does not discriminate between related and unrelated
//!   text (see `EmbeddingBackend::Random`'s docs below).
//! - **Lattice** (feature `lattice-embeddings`, opt-in): real pretrained
//!   sentence embeddings via [`ruvector_core::embeddings::LatticeEmbedding`],
//!   a pure-Rust native embedder (no ONNX Runtime, no C++ FFI). Selected at
//!   runtime by requesting a model id (see [`EmbeddingService::new`]); a
//!   pretrained model is a ~90-130MB download on first use, so this backend
//!   is never enabled implicitly.

use crate::config::EmbeddingConfig;
use crate::error::{Error, Result};

use ahash::AHashMap;
use lru::LruCache;
use parking_lot::Mutex;
use std::num::NonZeroUsize;

#[cfg(feature = "lattice-embeddings")]
use ruvector_core::embeddings::{EmbeddingProvider, LatticeEmbedding};

/// Environment variable used to request a pretrained model id for the
/// `lattice-embeddings` backend (e.g. `"minilm"`, `"bge-small-en-v1.5"`).
/// Only consulted when the `lattice-embeddings` feature is compiled in; the
/// default build never reads it.
pub const RUVLLM_EMBED_MODEL_ENV: &str = "RUVLLM_EMBED_MODEL";

/// Result of embedding a text
#[derive(Debug, Clone)]
pub struct Embedding {
    /// The embedding vector
    pub vector: Vec<f32>,
    /// Token count
    pub token_count: usize,
    /// Whether text was truncated
    pub truncated: bool,
    /// Cache hit indicator
    pub from_cache: bool,
}

/// Token from tokenization
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct Token {
    /// Token ID
    pub id: u32,
    /// Token text
    pub text: String,
}

/// Tokenizer for text processing
pub struct Tokenizer {
    /// Vocabulary mapping
    vocab: AHashMap<String, u32>,
    /// Reverse mapping
    id_to_token: Vec<String>,
    /// Special tokens
    special_tokens: SpecialTokens,
}

/// Special token IDs
#[derive(Debug, Clone)]
struct SpecialTokens {
    pad: u32,
    unk: u32,
    bos: u32,
    eos: u32,
}

impl Tokenizer {
    /// Create a new basic tokenizer
    pub fn new(vocab_size: usize) -> Self {
        let mut vocab = AHashMap::new();
        let mut id_to_token = Vec::with_capacity(vocab_size);

        // Add special tokens
        let special = ["<pad>", "<unk>", "<bos>", "<eos>", "<sep>"];
        for (i, tok) in special.iter().enumerate() {
            vocab.insert(tok.to_string(), i as u32);
            id_to_token.push(tok.to_string());
        }

        // Build basic character/word vocabulary
        let chars: Vec<char> =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 .,!?;:'\"-_()[]{}"
                .chars()
                .collect();
        for ch in chars {
            let s = ch.to_string();
            if !vocab.contains_key(&s) && vocab.len() < vocab_size {
                let id = vocab.len() as u32;
                vocab.insert(s.clone(), id);
                id_to_token.push(s);
            }
        }

        Self {
            vocab,
            id_to_token,
            special_tokens: SpecialTokens {
                pad: 0,
                unk: 1,
                bos: 2,
                eos: 3,
            },
        }
    }

    /// Tokenize text into token IDs
    pub fn tokenize(&self, text: &str) -> Vec<u32> {
        let mut tokens = vec![self.special_tokens.bos];

        // Simple character-level tokenization
        for word in text.split_whitespace() {
            for ch in word.chars() {
                let s = ch.to_string();
                let id = self
                    .vocab
                    .get(&s)
                    .copied()
                    .unwrap_or(self.special_tokens.unk);
                tokens.push(id);
            }
            // Add space token
            if let Some(&space_id) = self.vocab.get(" ") {
                tokens.push(space_id);
            }
        }

        tokens.push(self.special_tokens.eos);
        tokens
    }

    /// Get vocabulary size
    pub fn vocab_size(&self) -> usize {
        self.vocab.len()
    }

    /// Decode tokens back to text
    pub fn decode(&self, tokens: &[u32]) -> String {
        tokens
            .iter()
            .filter_map(|&id| self.id_to_token.get(id as usize))
            .cloned()
            .collect::<Vec<_>>()
            .join("")
    }
}

/// Random-init character-level embedder: matrix + sinusoidal positions +
/// pooling. This is the original (and default) `EmbeddingService` behavior,
/// moved verbatim into its own type so it can sit behind
/// [`EmbeddingBackend::Random`].
///
/// ⚠️ **Not semantic.** The embedding matrix is initialized from uniform
/// random noise and never trained or loaded from a checkpoint, so cosine
/// similarity between texts does not track meaning — unrelated pairs can
/// score higher than genuine paraphrases. Use the `lattice-embeddings`
/// feature (see [`EmbeddingBackend::Lattice`]) for real semantic embeddings.
struct RandomEmbedder {
    dimension: usize,
    max_tokens: usize,
    tokenizer: Tokenizer,
    embedding_matrix: Vec<Vec<f32>>,
    position_embeddings: Vec<Vec<f32>>,
}

impl RandomEmbedder {
    fn new(config: &EmbeddingConfig) -> Self {
        let tokenizer = Tokenizer::new(10000);
        let vocab_size = tokenizer.vocab_size();

        // Initialize embedding matrix with random values
        let mut rng = rand::thread_rng();
        use rand::Rng;

        let embedding_matrix: Vec<Vec<f32>> = (0..vocab_size)
            .map(|_| {
                let mut vec: Vec<f32> = (0..config.dimension)
                    .map(|_| rng.gen_range(-0.1..0.1))
                    .collect();
                // Normalize
                let norm: f32 = vec.iter().map(|x| x * x).sum::<f32>().sqrt();
                if norm > 0.0 {
                    vec.iter_mut().for_each(|x| *x /= norm);
                }
                vec
            })
            .collect();

        // Position embeddings (sinusoidal)
        let position_embeddings: Vec<Vec<f32>> = (0..config.max_tokens)
            .map(|pos| {
                (0..config.dimension)
                    .map(|i| {
                        let angle = pos as f32
                            / (10000.0_f32).powf(2.0 * (i / 2) as f32 / config.dimension as f32);
                        if i % 2 == 0 {
                            angle.sin()
                        } else {
                            angle.cos()
                        }
                    })
                    .collect()
            })
            .collect();

        Self {
            dimension: config.dimension,
            max_tokens: config.max_tokens,
            tokenizer,
            embedding_matrix,
            position_embeddings,
        }
    }

    /// Tokenize, truncate, mean-pool, and wrap into a fully-formed
    /// `Embedding` (token/truncation bookkeeping matches this backend's own
    /// character-level tokenizer).
    fn embed(&self, text: &str) -> Embedding {
        let tokens = self.tokenizer.tokenize(text);
        let truncated = tokens.len() > self.max_tokens;
        let tokens: Vec<u32> = tokens.into_iter().take(self.max_tokens).collect();

        let vector = self.compute_embedding(&tokens);

        Embedding {
            vector,
            token_count: tokens.len(),
            truncated,
            from_cache: false,
        }
    }

    fn embed_with_pooling(&self, text: &str, pooling: PoolingStrategy) -> Embedding {
        let tokens = self.tokenizer.tokenize(text);
        let tokens: Vec<u32> = tokens.into_iter().take(self.max_tokens).collect();

        let vector = match pooling {
            PoolingStrategy::Mean => self.mean_pooling(&tokens),
            PoolingStrategy::Max => self.max_pooling(&tokens),
            PoolingStrategy::CLS => self.cls_pooling(&tokens),
            PoolingStrategy::LastToken => self.last_token_pooling(&tokens),
        };

        Embedding {
            vector,
            token_count: tokens.len(),
            truncated: tokens.len() >= self.max_tokens,
            from_cache: false,
        }
    }

    fn compute_embedding(&self, tokens: &[u32]) -> Vec<f32> {
        self.mean_pooling(tokens)
    }

    fn mean_pooling(&self, tokens: &[u32]) -> Vec<f32> {
        let mut result = vec![0.0f32; self.dimension];

        for (pos, &token_id) in tokens.iter().enumerate() {
            let token_emb = self.get_token_embedding(token_id);
            let pos_emb = self.get_position_embedding(pos);

            for i in 0..self.dimension {
                result[i] += token_emb[i] + pos_emb[i];
            }
        }

        // Average
        let n = tokens.len() as f32;
        if n > 0.0 {
            result.iter_mut().for_each(|x| *x /= n);
        }

        // Normalize
        let norm: f32 = result.iter().map(|x| x * x).sum::<f32>().sqrt();
        if norm > 0.0 {
            result.iter_mut().for_each(|x| *x /= norm);
        }

        result
    }

    fn max_pooling(&self, tokens: &[u32]) -> Vec<f32> {
        let mut result = vec![f32::NEG_INFINITY; self.dimension];

        for (pos, &token_id) in tokens.iter().enumerate() {
            let token_emb = self.get_token_embedding(token_id);
            let pos_emb = self.get_position_embedding(pos);

            for i in 0..self.dimension {
                let val = token_emb[i] + pos_emb[i];
                if val > result[i] {
                    result[i] = val;
                }
            }
        }

        // Normalize
        let norm: f32 = result.iter().map(|x| x * x).sum::<f32>().sqrt();
        if norm > 0.0 {
            result.iter_mut().for_each(|x| *x /= norm);
        }

        result
    }

    fn cls_pooling(&self, tokens: &[u32]) -> Vec<f32> {
        if let Some(&first_token) = tokens.first() {
            let token_emb = self.get_token_embedding(first_token);
            let pos_emb = self.get_position_embedding(0);

            let mut result: Vec<f32> = token_emb
                .iter()
                .zip(pos_emb.iter())
                .map(|(t, p)| t + p)
                .collect();

            // Normalize
            let norm: f32 = result.iter().map(|x| x * x).sum::<f32>().sqrt();
            if norm > 0.0 {
                result.iter_mut().for_each(|x| *x /= norm);
            }

            result
        } else {
            vec![0.0; self.dimension]
        }
    }

    fn last_token_pooling(&self, tokens: &[u32]) -> Vec<f32> {
        if let Some(&last_token) = tokens.last() {
            let pos = tokens.len().saturating_sub(1);
            let token_emb = self.get_token_embedding(last_token);
            let pos_emb = self.get_position_embedding(pos);

            let mut result: Vec<f32> = token_emb
                .iter()
                .zip(pos_emb.iter())
                .map(|(t, p)| t + p)
                .collect();

            // Normalize
            let norm: f32 = result.iter().map(|x| x * x).sum::<f32>().sqrt();
            if norm > 0.0 {
                result.iter_mut().for_each(|x| *x /= norm);
            }

            result
        } else {
            vec![0.0; self.dimension]
        }
    }

    fn get_token_embedding(&self, token_id: u32) -> &[f32] {
        let idx = (token_id as usize).min(self.embedding_matrix.len() - 1);
        &self.embedding_matrix[idx]
    }

    fn get_position_embedding(&self, pos: usize) -> &[f32] {
        let idx = pos.min(self.position_embeddings.len() - 1);
        &self.position_embeddings[idx]
    }
}

/// The backend actually used to compute embedding vectors.
enum EmbeddingBackend {
    /// Random-init character-level matrix (default; not semantic — see
    /// [`RandomEmbedder`]'s docs).
    Random(RandomEmbedder),
    /// Real pretrained sentence embeddings via `lattice-embed`, through
    /// [`ruvector_core::embeddings::LatticeEmbedding`]. Selected at runtime
    /// when a model id is requested (config field or
    /// [`RUVLLM_EMBED_MODEL_ENV`]); never enabled implicitly.
    #[cfg(feature = "lattice-embeddings")]
    Lattice(LatticeEmbedding),
}

impl EmbeddingBackend {
    /// Human-readable backend label for [`EmbeddingServiceStats::backend`]
    /// (e.g. `"random-charlevel"` / `"lattice:bge-small-en-v1.5"`).
    fn label(&self) -> String {
        match self {
            EmbeddingBackend::Random(_) => "random-charlevel".to_string(),
            #[cfg(feature = "lattice-embeddings")]
            EmbeddingBackend::Lattice(l) => format!("lattice:{}", l.name()),
        }
    }

    fn is_pretrained(&self) -> bool {
        match self {
            EmbeddingBackend::Random(_) => false,
            #[cfg(feature = "lattice-embeddings")]
            EmbeddingBackend::Lattice(_) => true,
        }
    }
}

/// Service for text embedding with caching
pub struct EmbeddingService {
    /// Embedding dimension actually produced by the active backend (may
    /// differ from `config.dimension` when a pretrained model is loaded —
    /// e.g. bge-small is 384D regardless of what `config.dimension` requested).
    dimension: usize,
    /// LRU cache for embeddings
    cache: Mutex<LruCache<u64, Embedding>>,
    /// Active embedding backend
    backend: EmbeddingBackend,
    /// Statistics
    stats: EmbeddingStats,
}

/// Embedding service statistics
struct EmbeddingStats {
    cache_hits: std::sync::atomic::AtomicU64,
    cache_misses: std::sync::atomic::AtomicU64,
    total_tokens: std::sync::atomic::AtomicU64,
}

impl EmbeddingService {
    /// Create a new embedding service.
    ///
    /// By default this builds the random-init character-level backend
    /// (`EmbeddingBackend::Random`) — fast, no download, but not semantic.
    ///
    /// A real pretrained backend can be selected at runtime by requesting a
    /// model id via the `RUVLLM_EMBED_MODEL` environment variable (e.g.
    /// `"minilm"`, `"bge-small-en-v1.5"`) **and** compiling with the
    /// `lattice-embeddings` feature. If a model is requested but the feature
    /// is not compiled in, or the feature is compiled in but the requested
    /// model fails to load (bad id, network/download failure), this returns
    /// an error rather than silently falling back to the random backend — an
    /// explicitly-requested pretrained model that can't load is a real
    /// failure.
    pub fn new(config: &EmbeddingConfig) -> Result<Self> {
        let requested_model = std::env::var(RUVLLM_EMBED_MODEL_ENV).ok();

        let backend = match requested_model {
            Some(model_id) if !model_id.trim().is_empty() => {
                Self::load_lattice_backend(&model_id)?
            }
            _ => EmbeddingBackend::Random(RandomEmbedder::new(config)),
        };

        let dimension = match &backend {
            EmbeddingBackend::Random(_) => config.dimension,
            #[cfg(feature = "lattice-embeddings")]
            EmbeddingBackend::Lattice(l) => l.dimensions(),
        };

        let cache_size = NonZeroUsize::new(10000).unwrap();

        Ok(Self {
            dimension,
            cache: Mutex::new(LruCache::new(cache_size)),
            backend,
            stats: EmbeddingStats {
                cache_hits: std::sync::atomic::AtomicU64::new(0),
                cache_misses: std::sync::atomic::AtomicU64::new(0),
                total_tokens: std::sync::atomic::AtomicU64::new(0),
            },
        })
    }

    #[cfg(feature = "lattice-embeddings")]
    fn load_lattice_backend(model_id: &str) -> Result<EmbeddingBackend> {
        let provider = LatticeEmbedding::from_pretrained(model_id).map_err(|e| {
            Error::Embedding(format!(
                "requested pretrained model '{model_id}' via {RUVLLM_EMBED_MODEL_ENV} \
                 failed to load: {e}"
            ))
        })?;
        Ok(EmbeddingBackend::Lattice(provider))
    }

    #[cfg(not(feature = "lattice-embeddings"))]
    fn load_lattice_backend(model_id: &str) -> Result<EmbeddingBackend> {
        Err(Error::Embedding(format!(
            "pretrained model '{model_id}' requested via {RUVLLM_EMBED_MODEL_ENV}, but this \
             build was compiled without the `lattice-embeddings` feature. Rebuild with \
             `--features lattice-embeddings` to use a pretrained backend, or unset \
             {RUVLLM_EMBED_MODEL_ENV} to use the default random backend."
        )))
    }

    /// Embed a text string
    pub fn embed(&self, text: &str) -> Result<Embedding> {
        // Check cache
        let hash = self.hash_text(text);
        {
            let mut cache = self.cache.lock();
            if let Some(cached) = cache.get(&hash) {
                self.stats
                    .cache_hits
                    .fetch_add(1, std::sync::atomic::Ordering::Relaxed);
                let mut result = cached.clone();
                result.from_cache = true;
                return Ok(result);
            }
        }
        self.stats
            .cache_misses
            .fetch_add(1, std::sync::atomic::Ordering::Relaxed);

        let embedding = match &self.backend {
            EmbeddingBackend::Random(random) => random.embed(text),
            #[cfg(feature = "lattice-embeddings")]
            EmbeddingBackend::Lattice(lattice) => {
                // Symmetric STS/similarity usage: always the passage side.
                // `EmbeddingProvider::embed` never applies a query
                // instruction (see `LatticeEmbedding`'s docs) — callers that
                // need asymmetric query/passage retrieval should use
                // `LatticeEmbedding::embed_query` directly, which this napi
                // surface does not expose.
                let vector = lattice
                    .embed(text)
                    .map_err(|e| Error::Embedding(format!("lattice-embed inference failed: {e}")))?;
                Embedding {
                    vector,
                    // Informational only: the pretrained backend uses its
                    // own subword tokenizer internally, not the char-level
                    // `Tokenizer` above.
                    token_count: text.split_whitespace().count(),
                    truncated: false,
                    from_cache: false,
                }
            }
        };

        self.stats
            .total_tokens
            .fetch_add(embedding.token_count as u64, std::sync::atomic::Ordering::Relaxed);

        // Cache result
        {
            let mut cache = self.cache.lock();
            cache.put(hash, embedding.clone());
        }

        Ok(embedding)
    }

    /// Embed multiple texts (batched for efficiency)
    pub fn embed_batch(&self, texts: &[&str]) -> Result<Vec<Embedding>> {
        texts.iter().map(|t| self.embed(t)).collect()
    }

    /// Embed with specific pooling strategy.
    ///
    /// Pooling strategies (mean/max/CLS/last-token) are a property of the
    /// `Random` backend's own token+position matrices. The `Lattice` backend
    /// performs its own pooling internally (mean-pooling over the
    /// transformer's hidden states, L2-normalized) and has no separate
    /// per-strategy output, so on that backend this ignores `pooling` and
    /// returns the same vector as [`EmbeddingService::embed`].
    pub fn embed_with_pooling(&self, text: &str, pooling: PoolingStrategy) -> Result<Embedding> {
        match &self.backend {
            EmbeddingBackend::Random(random) => Ok(random.embed_with_pooling(text, pooling)),
            #[cfg(feature = "lattice-embeddings")]
            EmbeddingBackend::Lattice(_) => self.embed(text),
        }
    }

    /// Effective embedding dimension of the active backend. Matches
    /// `config.dimension` on the default random backend; may differ from it
    /// on the `lattice-embeddings` backend, where the loaded pretrained
    /// model's own dimensionality wins (e.g. bge-small is always 384D).
    pub fn dimension(&self) -> usize {
        self.dimension
    }

    /// Get embedding statistics
    pub fn get_stats(&self) -> EmbeddingServiceStats {
        EmbeddingServiceStats {
            cache_hits: self
                .stats
                .cache_hits
                .load(std::sync::atomic::Ordering::Relaxed),
            cache_misses: self
                .stats
                .cache_misses
                .load(std::sync::atomic::Ordering::Relaxed),
            total_tokens: self
                .stats
                .total_tokens
                .load(std::sync::atomic::Ordering::Relaxed),
            cache_size: self.cache.lock().len(),
            backend: self.backend.label(),
            pretrained: self.backend.is_pretrained(),
        }
    }

    /// Clear the embedding cache
    pub fn clear_cache(&self) {
        self.cache.lock().clear();
    }

    fn hash_text(&self, text: &str) -> u64 {
        use std::collections::hash_map::DefaultHasher;
        use std::hash::{Hash, Hasher};

        let mut hasher = DefaultHasher::new();
        text.hash(&mut hasher);
        hasher.finish()
    }
}

/// Pooling strategy for embeddings
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum PoolingStrategy {
    /// Mean pooling (average all tokens)
    Mean,
    /// Max pooling (element-wise max)
    Max,
    /// CLS token pooling (first token)
    CLS,
    /// Last token pooling
    LastToken,
}

/// Public statistics
#[derive(Debug, Clone)]
pub struct EmbeddingServiceStats {
    /// Cache hits
    pub cache_hits: u64,
    /// Cache misses
    pub cache_misses: u64,
    /// Total tokens processed
    pub total_tokens: u64,
    /// Current cache size
    pub cache_size: usize,
    /// Active backend label, e.g. `"random-charlevel"` or
    /// `"lattice:bge-small-en-v1.5"`. Lets a caller distinguish the
    /// non-semantic default from a loaded pretrained model.
    pub backend: String,
    /// Whether the active backend is a real pretrained model (`true`) or
    /// the random-init default (`false`).
    pub pretrained: bool,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_embedding_dimension() {
        let config = EmbeddingConfig::default();
        let service = EmbeddingService::new(&config).unwrap();
        let embedding = service.embed("Hello world").unwrap();
        assert_eq!(embedding.vector.len(), config.dimension);
    }

    #[test]
    fn test_embedding_normalized() {
        let config = EmbeddingConfig::default();
        let service = EmbeddingService::new(&config).unwrap();
        let embedding = service.embed("Test text").unwrap();

        let norm: f32 = embedding.vector.iter().map(|x| x * x).sum::<f32>().sqrt();
        assert!((norm - 1.0).abs() < 0.01);
    }

    #[test]
    fn test_same_text_same_embedding() {
        let config = EmbeddingConfig::default();
        let service = EmbeddingService::new(&config).unwrap();

        let e1 = service.embed("Same text").unwrap();
        let e2 = service.embed("Same text").unwrap();

        assert_eq!(e1.vector, e2.vector);
        assert!(e2.from_cache);
    }

    #[test]
    fn test_different_texts_different_embeddings() {
        let config = EmbeddingConfig::default();
        let service = EmbeddingService::new(&config).unwrap();

        let e1 = service.embed("Hello world").unwrap();
        let e2 = service.embed("Goodbye moon").unwrap();

        // Character-level tokenizer produces similar embeddings for similar text
        // Just verify they're not identical
        let diff: f32 = e1
            .vector
            .iter()
            .zip(e2.vector.iter())
            .map(|(a, b)| (a - b).abs())
            .sum();
        assert!(
            diff > 0.0,
            "Different texts should produce different embeddings"
        );
    }

    #[test]
    fn test_tokenizer() {
        let tokenizer = Tokenizer::new(1000);

        let tokens = tokenizer.tokenize("Hello world");
        assert!(!tokens.is_empty());
        assert_eq!(tokens[0], 2); // BOS
        assert_eq!(*tokens.last().unwrap(), 3); // EOS
    }

    #[test]
    fn test_batch_embedding() {
        let config = EmbeddingConfig::default();
        let service = EmbeddingService::new(&config).unwrap();

        let texts = vec!["text one", "text two", "text three"];
        let embeddings = service.embed_batch(&texts).unwrap();

        assert_eq!(embeddings.len(), 3);
        for emb in &embeddings {
            assert_eq!(emb.vector.len(), config.dimension);
        }
    }

    #[test]
    fn test_pooling_strategies() {
        let config = EmbeddingConfig::default();
        let service = EmbeddingService::new(&config).unwrap();
        let text = "Test pooling strategies";

        let mean = service
            .embed_with_pooling(text, PoolingStrategy::Mean)
            .unwrap();
        let max = service
            .embed_with_pooling(text, PoolingStrategy::Max)
            .unwrap();
        let cls = service
            .embed_with_pooling(text, PoolingStrategy::CLS)
            .unwrap();
        let last = service
            .embed_with_pooling(text, PoolingStrategy::LastToken)
            .unwrap();

        assert_eq!(mean.vector.len(), config.dimension);
        assert_eq!(max.vector.len(), config.dimension);
        assert_eq!(cls.vector.len(), config.dimension);
        assert_eq!(last.vector.len(), config.dimension);

        let mean_dot_max: f32 = mean
            .vector
            .iter()
            .zip(max.vector.iter())
            .map(|(a, b)| a * b)
            .sum();
        assert!(mean_dot_max < 0.999);
    }

    #[test]
    fn test_cache_stats() {
        let config = EmbeddingConfig::default();
        let service = EmbeddingService::new(&config).unwrap();

        service.embed("test 1").unwrap();
        service.embed("test 2").unwrap();
        service.embed("test 1").unwrap(); // Cache hit

        let stats = service.get_stats();
        assert_eq!(stats.cache_hits, 1);
        assert_eq!(stats.cache_misses, 2);
        assert_eq!(stats.backend, "random-charlevel");
        assert!(!stats.pretrained);
    }

    #[test]
    fn test_truncation() {
        let mut config = EmbeddingConfig::default();
        config.max_tokens = 10;
        let service = EmbeddingService::new(&config).unwrap();

        let long_text = "This is a very long text that will definitely be truncated because it exceeds the maximum token limit";
        let embedding = service.embed(long_text).unwrap();

        assert!(embedding.truncated);
    }

    #[test]
    fn test_clear_cache() {
        let config = EmbeddingConfig::default();
        let service = EmbeddingService::new(&config).unwrap();

        service.embed("test").unwrap();
        assert_eq!(service.get_stats().cache_size, 1);

        service.clear_cache();
        assert_eq!(service.get_stats().cache_size, 0);
    }

}

/// Discrimination regression test (issue #655, ask #2): asserts that every
/// paraphrase pair scores a higher cosine similarity than every unrelated
/// pair.
///
/// Gated `#[ignore]` unconditionally: it downloads a pretrained model over
/// the network on first run, so it must never fire from a plain `cargo
/// test`. Compiles in both the default and `lattice-embeddings` builds (it
/// only touches the public `EmbeddingService` API), but can only actually
/// pass in the `lattice-embeddings` build — without that feature,
/// `EmbeddingService::new` fails fast (see
/// `EmbeddingService::load_lattice_backend`) because a model was
/// explicitly requested. This is the mutation-sensitivity property required
/// by #655 ask #2: the assertion fails against the random backend (verified
/// manually against `RandomEmbedder` during development — see the PR
/// description for the observed before/after cosine tables) and passes only
/// once real pretrained embeddings are wired in.
///
/// Run with: `RUVLLM_EMBED_MODEL=minilm cargo test --features
/// lattice-embeddings -- --ignored discriminat`
#[cfg(test)]
mod discrimination {
    use super::*;

    fn cosine(a: &[f32], b: &[f32]) -> f32 {
        let dot: f32 = a.iter().zip(b.iter()).map(|(x, y)| x * y).sum();
        let norm_a: f32 = a.iter().map(|x| x * x).sum::<f32>().sqrt();
        let norm_b: f32 = b.iter().map(|x| x * x).sum::<f32>().sqrt();
        if norm_a == 0.0 || norm_b == 0.0 {
            0.0
        } else {
            dot / (norm_a * norm_b)
        }
    }

    #[test]
    #[ignore = "needs the lattice-embeddings feature and downloads a model over the \
                network; run: RUVLLM_EMBED_MODEL=minilm cargo test --features \
                lattice-embeddings -- --ignored discriminat"]
    fn lattice_embeddings_discriminate() {
        let model_id =
            std::env::var(RUVLLM_EMBED_MODEL_ENV).unwrap_or_else(|_| "minilm".to_string());
        // SAFETY: single-threaded test process (this test is `--ignored`,
        // always run in isolation), no concurrent env access.
        unsafe {
            std::env::set_var(RUVLLM_EMBED_MODEL_ENV, &model_id);
        }

        let config = EmbeddingConfig::default();
        let service = EmbeddingService::new(&config)
            .unwrap_or_else(|e| panic!("failed to load pretrained model '{model_id}': {e}"));
        assert!(
            service.get_stats().pretrained,
            "service must report pretrained=true once a real model is loaded"
        );

        // (text, paraphrase, unrelated)
        let corpus: [(&str, &str, &str); 6] = [
            (
                "The cat sat on the mat.",
                "A cat was sitting on the rug.",
                "The stock market crashed yesterday.",
            ),
            (
                "How do I reset my password?",
                "What is the process to change my login password?",
                "The recipe calls for two cups of flour.",
            ),
            (
                "The weather today is sunny and warm.",
                "It's a bright, warm day outside.",
                "The quarterly earnings report was released.",
            ),
            (
                "She enjoys playing the piano in the evening.",
                "In the evenings she likes to play piano.",
                "The bridge was constructed in 1932.",
            ),
            (
                "The car broke down on the highway.",
                "The vehicle stalled on the freeway.",
                "He studied biology at the university.",
            ),
            (
                "Machine learning models require large datasets.",
                "Training ML models needs a lot of data.",
                "The garden was full of blooming roses.",
            ),
        ];

        let mut paraphrase_scores = Vec::with_capacity(corpus.len());
        let mut unrelated_scores = Vec::with_capacity(corpus.len());
        let mut table = String::new();
        table.push_str("text | paraphrase_sim | unrelated_sim\n");

        for (text, paraphrase, unrelated) in corpus {
            let e_text = service.embed(text).unwrap();
            let e_paraphrase = service.embed(paraphrase).unwrap();
            let e_unrelated = service.embed(unrelated).unwrap();

            let sim_paraphrase = cosine(&e_text.vector, &e_paraphrase.vector);
            let sim_unrelated = cosine(&e_text.vector, &e_unrelated.vector);

            table.push_str(&format!(
                "{text:?} | {sim_paraphrase:.4} | {sim_unrelated:.4}\n"
            ));

            paraphrase_scores.push(sim_paraphrase);
            unrelated_scores.push(sim_unrelated);
        }

        println!("model={model_id}\n{table}");

        let min_paraphrase = paraphrase_scores
            .iter()
            .copied()
            .fold(f32::INFINITY, f32::min);
        let max_unrelated = unrelated_scores
            .iter()
            .copied()
            .fold(f32::NEG_INFINITY, f32::max);

        println!(
            "min(paraphrase_sim)={min_paraphrase:.4} max(unrelated_sim)={max_unrelated:.4}"
        );

        for (i, (&p, &u)) in paraphrase_scores.iter().zip(unrelated_scores.iter()).enumerate() {
            assert!(
                p > u,
                "pair {i}: paraphrase similarity ({p:.4}) must exceed unrelated similarity ({u:.4})"
            );
        }

        assert!(
            min_paraphrase > max_unrelated,
            "min(paraphrase_sim)={min_paraphrase:.4} must exceed max(unrelated_sim)={max_unrelated:.4}"
        );
    }
}
