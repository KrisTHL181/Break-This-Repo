//! Text lifecycle metadata keys. Tantivy blob formats remain unchanged.

use crate::index_lifecycle::{IndexGenerationId, IndexId};

use super::super::lifecycle::IndexEntity;
use super::super::HASH_LEN;

/// Full SHA-256 partition identity.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub(crate) struct PartitionFingerprint([u8; HASH_LEN]);

impl PartitionFingerprint {
    pub(crate) const fn new(bytes: [u8; HASH_LEN]) -> Self {
        Self(bytes)
    }

    pub(crate) const fn as_bytes(&self) -> &[u8; HASH_LEN] {
        &self.0
    }
}

/// Full content-addressed blob identity.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub(crate) struct BlobHash([u8; HASH_LEN]);

impl BlobHash {
    pub(crate) const fn new(bytes: [u8; HASH_LEN]) -> Self {
        Self(bytes)
    }

    pub(crate) const fn as_bytes(&self) -> &[u8; HASH_LEN] {
        &self.0
    }
}

/// Full SHA-256 identity of one analyzed text term.
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub(crate) struct TextTermFingerprint([u8; HASH_LEN]);

impl TextTermFingerprint {
    pub(crate) const fn new(bytes: [u8; HASH_LEN]) -> Self {
        Self(bytes)
    }

    pub(crate) const fn as_bytes(&self) -> &[u8; HASH_LEN] {
        &self.0
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub(crate) struct TextManifestRootKey {
    pub(crate) index_id: IndexId,
    pub(crate) generation: IndexGenerationId,
    pub(crate) partition: PartitionFingerprint,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub(crate) struct TextManifestPageKey {
    pub(crate) root: TextManifestRootKey,
    pub(crate) page: u32,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub(crate) struct TextBuildArtifactKey {
    pub(crate) root: TextManifestRootKey,
    pub(crate) ordinal: u32,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub(crate) struct TextEntityStateKey {
    pub(crate) root: TextManifestRootKey,
    pub(crate) entity: IndexEntity,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub(crate) struct TextCorpusStatisticsKey {
    pub(crate) index_id: IndexId,
    pub(crate) generation: IndexGenerationId,
    pub(crate) partition: PartitionFingerprint,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub(crate) struct TextTermStatisticsKey {
    pub(crate) corpus: TextCorpusStatisticsKey,
    pub(crate) term: TextTermFingerprint,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub(crate) struct TextStatisticsEntityKey {
    pub(crate) index_id: IndexId,
    pub(crate) generation: IndexGenerationId,
    pub(crate) entity: IndexEntity,
}
