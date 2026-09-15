use std::hash::{Hash, Hasher};
use temper_core::dimension::Dimension;
use temper_core::pos::ChunkPos;
use temper_storage::lmdb::StorageBackend;
use temper_world_format::Chunk;
use temper_world_format::errors::WorldError;
use temper_world_format::errors::WorldError::CorruptedChunkData;
use tracing::warn;
use yazi::CompressionLevel;

pub fn save_chunk_internal(
    storage: &StorageBackend,
    pos: ChunkPos,
    dimension: Dimension,
    chunk: &Chunk,
) -> Result<(), WorldError> {
    let chunk = chunk.clone_without_transient_noise();
    let serialized = bitcode::serialize(&chunk).expect("Unable to serialize chunk");
    save_serialized_chunk_internal(storage, pos, dimension, &serialized)
}

pub fn save_serialized_chunk_internal(
    storage: &StorageBackend,
    pos: ChunkPos,
    dimension: Dimension,
    serialized_chunk: &[u8],
) -> Result<(), WorldError> {
    if !storage.table_exists("chunks".to_string())? {
        storage.create_table("chunks".to_string())?;
    }
    let as_bytes = yazi::compress(
        serialized_chunk,
        yazi::Format::Zlib,
        CompressionLevel::BestSpeed,
    )?;
    let digest = create_key(dimension, pos);
    storage.upsert("chunks".to_string(), digest, as_bytes)?;
    Ok(())
}

pub fn load_chunk_internal(
    storage: &StorageBackend,
    pos: ChunkPos,
    dimension: Dimension,
    verify: bool,
) -> Result<Chunk, WorldError> {
    if !storage.table_exists("chunks".to_string())? {
        return Err(WorldError::ChunkNotFound);
    }

    let digest = create_key(dimension, pos);
    match storage.get("chunks".to_string(), digest)? {
        Some(compressed) => {
            let (data, checksum) = yazi::decompress(compressed.as_slice(), yazi::Format::Zlib)?;
            if verify {
                if let Some(expected_checksum) = checksum {
                    let real_checksum = yazi::Adler32::from_buf(data.as_slice()).finish();
                    if real_checksum != expected_checksum {
                        return Err(CorruptedChunkData(real_checksum, expected_checksum));
                    }
                } else {
                    warn!("Chunk data does not have a checksum, skipping verification.");
                }
            }
            let chunk: Chunk = bitcode::deserialize(&data)
                .map_err(|e| WorldError::BitcodeDeserializeError(e.to_string()))?;
            Ok(chunk)
        }
        None => Err(WorldError::ChunkNotFound),
    }
}

pub fn chunk_exists_internal(
    storage: &StorageBackend,
    pos: ChunkPos,
    dimension: Dimension,
) -> Result<bool, WorldError> {
    if !storage.table_exists("chunks".to_string())? {
        return Ok(false);
    }
    let digest = create_key(dimension, pos);
    Ok(storage.exists("chunks".to_string(), digest)?)
}

pub fn delete_chunk_internal(
    storage: &StorageBackend,
    pos: ChunkPos,
    dimension: Dimension,
) -> Result<(), WorldError> {
    let digest = create_key(dimension, pos);
    storage.delete("chunks".to_string(), digest)?;
    Ok(())
}

pub fn sync_internal(storage: &StorageBackend) -> Result<(), WorldError> {
    storage.flush()?;
    Ok(())
}

fn create_key(dimension: Dimension, pos: ChunkPos) -> u128 {
    let mut hasher = wyhash::WyHash::with_seed(0);
    dimension.hash(&mut hasher);
    let dim_hash = hasher.finish();
    u128::from(dim_hash) << 96 | u128::from(pos.pack())
}

#[cfg(test)]
mod tests {
    use temper_core::dimension::Dimension;
    use tempfile::tempdir;

    use super::*;

    fn test_storage() -> StorageBackend {
        StorageBackend::initialize(Some(tempdir().unwrap().keep()), 1024 * 1024 * 1024).unwrap()
    }

    #[test]
    fn missing_chunks_table_loads_as_missing_chunk() {
        let storage = test_storage();

        assert!(matches!(
            load_chunk_internal(&storage, ChunkPos::new(0, 0), Dimension::Overworld, false),
            Err(WorldError::ChunkNotFound)
        ));
    }

    #[test]
    fn missing_chunks_table_reports_chunk_does_not_exist() {
        let storage = test_storage();

        assert!(
            !chunk_exists_internal(&storage, ChunkPos::new(0, 0), Dimension::Overworld).unwrap()
        );
    }
}
