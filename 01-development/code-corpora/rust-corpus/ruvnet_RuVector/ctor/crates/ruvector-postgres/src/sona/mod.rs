//! Sona self-learning module — Micro-LoRA trajectories and EWC++ for PostgreSQL.

pub mod operators;

use dashmap::{mapref::entry::Entry, DashMap};
use ruvector_sona::{SonaConfig, SonaEngine};
use std::sync::Arc;

struct SonaEngineEntry {
    dimension: u32,
    engine: Arc<SonaEngine>,
}

/// Global Sona engine state. A logical table has exactly one embedding
/// dimension: creating hidden per-dimension engines makes successful writes
/// disappear from the table-level stats and learning state.
static SONA_ENGINES: once_cell::sync::Lazy<DashMap<String, SonaEngineEntry>> =
    once_cell::sync::Lazy::new(DashMap::new);

/// Default dimension when none is specified (e.g., for stats queries).
pub(super) const DEFAULT_DIM: u32 = 256;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct SonaDimensionMismatch {
    pub expected: u32,
    pub actual: u32,
}

/// Return an initialized engine without creating state as a side effect.
pub fn get_engine(table_name: &str) -> Option<Arc<SonaEngine>> {
    SONA_ENGINES
        .get(table_name)
        .map(|entry| Arc::clone(&entry.engine))
}

/// Get or create a SonaEngine for a given table with default dimension.
pub fn get_or_create_engine(table_name: &str) -> Arc<SonaEngine> {
    if let Some(engine) = get_engine(table_name) {
        return engine;
    }
    get_or_create_engine_with_dim(table_name, DEFAULT_DIM)
        .expect("a newly initialized SONA engine must accept its configured dimension")
}

/// Get or create a SonaEngine for a table and enforce its first dimension.
pub fn get_or_create_engine_with_dim(
    table_name: &str,
    dim: u32,
) -> Result<Arc<SonaEngine>, SonaDimensionMismatch> {
    match SONA_ENGINES.entry(table_name.to_string()) {
        Entry::Occupied(entry) => {
            if entry.get().dimension != dim {
                return Err(SonaDimensionMismatch {
                    expected: entry.get().dimension,
                    actual: dim,
                });
            }
            Ok(Arc::clone(&entry.get().engine))
        }
        Entry::Vacant(entry) => {
            let engine = Arc::new(SonaEngine::with_config(SonaConfig {
                hidden_dim: dim as usize,
                embedding_dim: dim as usize,
                ..Default::default()
            }));
            entry.insert(SonaEngineEntry {
                dimension: dim,
                engine: Arc::clone(&engine),
            });
            Ok(engine)
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn table_dimension_is_fixed_by_the_first_engine() {
        let table = format!("sona-dimension-test-{}", std::process::id());
        let first = get_or_create_engine_with_dim(&table, 384).unwrap();
        assert_eq!(first.config().embedding_dim, 384);
        assert!(Arc::ptr_eq(&first, &get_engine(&table).unwrap()));

        let mismatch = match get_or_create_engine_with_dim(&table, 256) {
            Ok(_) => panic!("a second dimension must be rejected"),
            Err(mismatch) => mismatch,
        };
        assert_eq!(
            mismatch,
            SonaDimensionMismatch {
                expected: 384,
                actual: 256,
            }
        );
        assert_eq!(get_engine(&table).unwrap().config().embedding_dim, 384);
    }
}
