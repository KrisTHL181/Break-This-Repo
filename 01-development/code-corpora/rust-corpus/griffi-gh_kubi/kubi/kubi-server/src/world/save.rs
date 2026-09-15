use kubi_shared::data::{io_thread::IOThreadManager, open_local_save_file};
use shipyard::{AllStoragesView, UniqueView, UniqueViewMut};
use crate::config::ConfigTable;
use super::{
  tasks::{ChunkTask, ChunkTaskManager},
  ChunkManager,
};

pub fn init_save_file(storages: &AllStoragesView) -> Option<IOThreadManager> {
  let config = storages.borrow::<UniqueView<ConfigTable>>().unwrap();
  if let Some(file_path) = &config.world.file {
    log::info!("Initializing save file from {:?}", file_path);
    let save = open_local_save_file(file_path).unwrap();
    Some(IOThreadManager::new(save))
  } else {
    log::warn!("No save file specified, world will not be saved");
    None
  }
}

pub fn save_modified(
  mut chunks: UniqueViewMut<ChunkManager>,
  ctm: UniqueView<ChunkTaskManager>,
) {
  log::info!("Saving...");
  let mut amount_saved = 0;
  for (position, chunk) in chunks.chunks.iter_mut() {
    if chunk.data_modified {
      let Some(data) = chunk.blocks.clone() else {
        continue
      };
      ctm.run(ChunkTask::SaveChunk {
        position: *position,
        data,
      });
      chunk.data_modified = false;
      amount_saved += 1;
    }
  }
  if amount_saved > 0 {
    log::info!("Queued {} chunks for saving", amount_saved);
  }
}