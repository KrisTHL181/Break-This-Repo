use bevy_ecs::prelude::Component;
use crossbeam_channel::{unbounded, Receiver, Sender};
use std::collections::{HashMap, HashSet, VecDeque};
use temper_core::pos::ChunkPos;
use uuid::Uuid;

pub struct ReadyChunk {
    pub chunk_pos: ChunkPos,
    pub packet_data: Vec<u8>,
    pub entities: Vec<(Uuid, u16)>,
    pub is_new_load: bool,
}

pub enum PreparedChunk {
    Ready(ReadyChunk),
    Failed { chunk_pos: ChunkPos },
}

impl PreparedChunk {
    /// The chunk this message is about, whether it succeeded or not.
    pub fn chunk_pos(&self) -> ChunkPos {
        match self {
            Self::Ready(ready) => ready.chunk_pos,
            Self::Failed { chunk_pos } => *chunk_pos,
        }
    }
}

#[derive(Component)]
pub struct ChunkReceiver {
    pub loading: VecDeque<ChunkPos>,
    pub dirty: VecDeque<ChunkPos>,
    pub loaded: HashSet<ChunkPos>,
    pub in_flight: HashSet<ChunkPos>, // dispatched, not yet harvested
    pub unloading: VecDeque<ChunkPos>,
    pub chunks_per_tick: f32,
    pub ready_tx: Sender<PreparedChunk>,
    pub ready_rx: Receiver<PreparedChunk>,
    pub retry_counts: HashMap<ChunkPos, u8>,
}

impl ChunkReceiver {
    pub fn new() -> Self {
        let (ready_tx, ready_rx) = unbounded();
        Self {
            loading: VecDeque::new(),
            loaded: HashSet::new(),
            in_flight: HashSet::new(),
            unloading: VecDeque::new(),
            dirty: VecDeque::new(),
            chunks_per_tick: 32.5,
            ready_tx,
            ready_rx,
            retry_counts: HashMap::new(),
        }
    }
}

impl Default for ChunkReceiver {
    fn default() -> Self {
        Self::new()
    }
}
