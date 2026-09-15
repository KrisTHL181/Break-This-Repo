use std::sync::Arc;
use atomic::Atomic;
use flume::{Receiver, Sender, TryIter};
use glam::IVec3;
use kubi_shared::{queue::QueuedBlock, worldgen::AbortState};
use shipyard::Unique;
use rayon::{ThreadPool, ThreadPoolBuilder};
use super::{
  chunk::BlockData,
  mesh::{generate_mesh, data::MeshGenData},
  worldgen::generate_world,
};
use crate::rendering::world::ChunkVertex;

pub enum ChunkTask {
  ChunkWorldgen {
    seed: u64,
    position: IVec3,
    abortion: Option<Arc<Atomic<AbortState>>>,
  },
  GenerateMesh {
    position: IVec3,
    data: MeshGenData
  }
}

pub enum ChunkTaskResponse {
  ChunkWorldgenDone {
    position: IVec3,
    chunk_data: BlockData,
    queued: Vec<QueuedBlock>
  },
  GenerateMeshDone {
    position: IVec3,
    vertices: Vec<ChunkVertex>,
    indices: Vec<u32>,
    trans_vertices: Vec<ChunkVertex>,
    trans_indices: Vec<u32>,
  },
}

#[derive(Unique)]
pub struct ChunkTaskManager {
  channel: (Sender<ChunkTaskResponse>, Receiver<ChunkTaskResponse>),
  pool: ThreadPool,
}

impl ChunkTaskManager {
  pub fn new() -> Self {
    Self {
      channel: flume::unbounded::<ChunkTaskResponse>(), //maybe put a bound or even bound(0)?
      pool: ThreadPoolBuilder::new().num_threads(4).build().unwrap()
    }
  }

  //TODO get rid of add_sussy_response

  /// Add a response to the queue, to be picked up by the main thread
  /// Used by the multiplayer netcode, a huge hack
  pub fn add_sussy_response(&self, response: ChunkTaskResponse) {
    // this WILL get stuck if the channel is bounded
    // don't make the channel bounded ever
    self.channel.0.send(response).unwrap()
  }

  pub fn spawn_task(&self, task: ChunkTask) {
    let sender = self.channel.0.clone();
    self.pool.spawn(move || {
      let _ = sender.send(match task {
        ChunkTask::GenerateMesh { position, data } => {
          let (
            (vertices, indices),
            (trans_vertices, trans_indices),
          ) = generate_mesh(position, data);
          ChunkTaskResponse::GenerateMeshDone {
            position,
            vertices, indices,
            trans_vertices, trans_indices,
          }
        },
        ChunkTask::ChunkWorldgen { position, seed, abortion } => {
          let Some((chunk_data, queued)) = generate_world(position, seed, abortion) else {
            log::warn!("aborted operation");
            return
          };
          ChunkTaskResponse::ChunkWorldgenDone { position, chunk_data, queued }
        }
      });
    });
  }

  #[deprecated(note = "use poll instead")]
  pub fn receive(&self) -> Option<ChunkTaskResponse> {
    self.channel.1.try_recv().ok()
  }

  pub fn poll(&self) -> TryIter<ChunkTaskResponse> {
    self.channel.1.try_iter()
  }
}
