use shipyard::{Workload, WorkloadModificator, Unique, AllStoragesView, UniqueViewMut, IntoWorkload};
use hashbrown::HashMap;
use std::time::{Duration, Instant};
use nohash_hasher::BuildNoHashHasher;

#[derive(Unique)]
#[repr(transparent)]
struct FixedTimestampStorage(HashMap<u32, Instant, BuildNoHashHasher<u32>>);
impl FixedTimestampStorage {
  pub fn new() -> Self {
    Self(HashMap::with_capacity_and_hasher(16, BuildNoHashHasher::default()))
  }
}
impl Default for FixedTimestampStorage {
  fn default() -> Self {
    Self::new()
  }
}

pub trait FixedTimestamp {
  fn make_fixed(self, rate_millis: u16, unique_id: u16) -> Self;
}
impl FixedTimestamp for Workload {
  fn make_fixed(self, rate_millis: u16, unique_id: u16) -> Self {
    let key = (rate_millis as u32) | ((unique_id as u32) << 16);
    let duration = Duration::from_millis(rate_millis as u64);
    (self,).into_workload().run_if(move |mut timestamps: UniqueViewMut<FixedTimestampStorage>| {
      let Some(t) = timestamps.0.get_mut(&key) else {
        unsafe {
          timestamps.0.insert_unique_unchecked(key, Instant::now());
        }
        return true
      };
      if t.elapsed() >= duration {
        *t = Instant::now();
        return true
      }
      false
    })
  }
}

pub fn init_fixed_timestamp_storage(
  storages: AllStoragesView
) {
  storages.add_unique(FixedTimestampStorage::new());
}
