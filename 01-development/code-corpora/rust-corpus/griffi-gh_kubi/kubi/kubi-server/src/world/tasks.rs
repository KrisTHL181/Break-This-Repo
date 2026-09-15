use shipyard::{Unique, AllStoragesView};
use flume::{unbounded, Sender, Receiver};
use glam::IVec3;
use rayon::{ThreadPool, ThreadPoolBuilder};
use anyhow::Result;
use kubi_shared::{
  chunk::BlockData, data::io_thread::{IOCommand, IOResponse, IOThreadManager}, queue::QueuedBlock, worldgen::generate_world
};
use super::save::init_save_file;

pub enum ChunkTask {
  LoadChunk {
    position: IVec3,
    seed: u64,
  },
  SaveChunk {
    position: IVec3,
    data: BlockData,
  },
}

pub enum ChunkTaskResponse {
  ChunkLoaded {
    chunk_position: IVec3,
    blocks: BlockData,
    queue: Vec<QueuedBlock>
  }
}

#[derive(Unique)]
pub struct ChunkTaskManager {
  channel: (Sender<ChunkTaskResponse>, Receiver<ChunkTaskResponse>),
  pool: ThreadPool,
  iota: Option<IOThreadManager>,
}

impl ChunkTaskManager {
  pub fn new(iota: Option<IOThreadManager>) -> Result<Self> {
    Ok(Self {
      channel: unbounded(),
      pool: ThreadPoolBuilder::new().build()?,
      iota,
    })
  }

  pub fn run(&self, task: ChunkTask) {
    match task {
      ChunkTask::LoadChunk { position: chunk_position, seed } => {
        // 1. Check if the chunk exists in the save file
        if let ChunkTask::LoadChunk { position, .. } = &task {
          if let Some(iota) = &self.iota {
            if iota.chunk_exists(*position) {
              iota.send(IOCommand::LoadChunk { position: *position });
              return
            }
          }
        }

        // 2. Generate the chunk if it doesn't exist
        let sender = self.channel.0.clone();
        self.pool.spawn(move || {
          sender.send({
            //unwrap is fine because abort is not possible
            let (blocks, queue) = generate_world(chunk_position, seed, None).unwrap();
            ChunkTaskResponse::ChunkLoaded { chunk_position, blocks, queue }
          }).unwrap()
        });
      },
      ChunkTask::SaveChunk { position, data } => {
        // Save the chunk to the save file
        if let Some(iota) = &self.iota {
          iota.send(IOCommand::SaveChunk { position, data });
        }
      },
    }
  }

  pub fn receive(&self) -> Option<ChunkTaskResponse> {
    // Try to receive IO results first
    // If there are none, try to receive worldgen results
    self.iota.as_ref().and_then(|iota| {
      iota.poll_single().map(|response| match response {
        IOResponse::ChunkLoaded { position, data } => ChunkTaskResponse::ChunkLoaded {
          chunk_position: position,
          blocks: data.expect("chunk data exists in the header, but was not loaded"),
          queue: Vec::with_capacity(0)
        },
        _ => panic!("Unexpected response from IO thread"),
      })
    }).or_else(|| {
      self.channel.1.try_recv().ok()
    })
  }

  pub fn iota(self) -> Option<IOThreadManager> {
    self.iota
  }
}

pub fn init_chunk_task_manager(
  storages: AllStoragesView
) {
  let iota = init_save_file(&storages);
  storages.add_unique(
    ChunkTaskManager::new(iota)
      .expect("ChunkTaskManager Init failed")
  );
}
