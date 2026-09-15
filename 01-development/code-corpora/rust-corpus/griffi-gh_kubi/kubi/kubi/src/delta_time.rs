use std::time::Duration;

use shipyard::{Unique, AllStoragesView};

#[derive(Unique, Default)]
pub(crate) struct DeltaTime(pub Duration);

pub fn init_delta_time(
  storages: AllStoragesView
) {
  storages.add_unique(DeltaTime::default())
}
