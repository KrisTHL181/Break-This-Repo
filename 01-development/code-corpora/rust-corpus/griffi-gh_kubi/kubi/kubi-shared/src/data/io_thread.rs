use glam::IVec3;
use flume::{Receiver, Sender, TryIter};
use shipyard::Unique;
use crate::chunk::BlockData;
use super::{SharedHeader, WorldSaveFile};

// Maximum amount of chunks to save in a single batch before checking if there are any pending read requests
// may be broken, so currently disabled
const MAX_SAVE_BATCH_SIZE: usize = usize::MAX;

#[derive(Clone, Copy, PartialEq, Eq, Debug, PartialOrd, Ord)]
pub enum TerminationStage {
  Starting,
  SaveQueue {
    progress: usize,
    total: usize,
  },
  ProcessRx,
  Terminated,
}

pub enum IOCommand {
  SaveChunk {
    position: IVec3,
    data: BlockData,
  },

  /// Load a chunk from the disk and send it to the main thread
  LoadChunk {
    position: IVec3,
  },

  /// Process all pending write commands and make the thread end itself
  /// LoadChunk commands will be ignored after this command is received
  Kys,
}

#[derive(Debug)]
pub enum IOResponse {
  /// A chunk has been loaded from the disk
  /// Or not, in which case the data will be None
  /// and chunk should be generated
  ChunkLoaded {
    position: IVec3,
    data: Option<BlockData>,
  },

  /// In-progress shutdown info
  KysProgressInformational(TerminationStage),

  /// The IO thread has been terminated
  Terminated,
}

struct IOThreadContext {
  tx: Sender<IOResponse>,
  rx: Receiver<IOCommand>,
  save: WorldSaveFile,
  save_queue: Vec<(IVec3, BlockData)>,
}

//TODO: Implement proper error handling (I/O errors are rlly common)

impl IOThreadContext {
  /// Should be called ON the IO thread
  ///
  /// Initializes the IO thread context, opening the file at the given path
  /// If there's an error opening the file, the thread will panic (TODO: handle this more gracefully)
  pub fn initialize(
    tx: Sender<IOResponse>,
    rx: Receiver<IOCommand>,
    save: WorldSaveFile,
  ) -> Self {
    // save.load_data().unwrap();
    let save_queue = Vec::new();
    Self { tx, rx, save, save_queue }
  }

  pub fn run(mut self) {
    loop {
      // because were waiting for the next command, we can't process the save_queue
      // which breaks batching, so we need to check if there are any pending save requests
      // and if there are, use non-blocking recv to give them a chance to be processed
      'rx: while let Some(command) = {
        if !self.save_queue.is_empty() {
          self.rx.try_recv().ok()
        } else {
          self.rx.recv().ok()
        }
      } {
        match command {
          IOCommand::SaveChunk { position, data } => {
            // if chunk already has a save request, overwrite it
            for (pos, old_data) in self.save_queue.iter_mut() {
              if *pos == position {
                *old_data = data;
                continue 'rx;
              }
            }
            // if not, save to the queue
            self.save_queue.push((position, data));
            //log::trace!("amt of unsaved chunks: {}", self.save_queue.len());
          }
          IOCommand::LoadChunk { position } => {
            // HOLD ON
            // first check if the chunk is already in the save queue
            // if it is, send it and continue
            // (NOT doing this WILL result in data loss if the user returns to the chunk too quickly)
            for (pos, data) in self.save_queue.iter() {
              if *pos == position {
                self.tx.send(IOResponse::ChunkLoaded { position, data: Some(data.clone()) }).unwrap();
                continue 'rx;
              }
            }
            let data = self.save.load_chunk(position).unwrap();
            self.tx.send(IOResponse::ChunkLoaded { position, data }).unwrap();
          }
          IOCommand::Kys => {
            self.tx.send(IOResponse::KysProgressInformational(
              TerminationStage::Starting,
            )).unwrap();

            // Process all pending write commands
            let save_queue_len = self.save_queue.len();

            log::info!("info: queue has {} chunks", save_queue_len);
            let mut saved_amount = 0;
            for (pos, data) in self.save_queue.drain(..) {
              self.save.save_chunk(pos, &data).unwrap();
              saved_amount += 1;

              // Send kys preflight info
              self.tx.send(IOResponse::KysProgressInformational(
                TerminationStage::SaveQueue {
                  progress: saved_amount,
                  total: save_queue_len,
                }
              )).unwrap();
            }

            log::debug!("now, moving on to the rx queue, ...");

            self.tx.send(IOResponse::KysProgressInformational(
              TerminationStage::ProcessRx
            )).unwrap();

            for cmd in self.rx.try_iter() {
              let IOCommand::SaveChunk { position, data } = cmd else {
                continue;
              };
              self.save.save_chunk(position, &data).unwrap();
              saved_amount += 1;
            }
            log::info!("saved {} chunks on exit", saved_amount);

            self.tx.send(IOResponse::KysProgressInformational(
              TerminationStage::Terminated
            )).unwrap();
            self.tx.send(IOResponse::Terminated).unwrap();

            return;
          }
        }
      }
      // between every betch of requests, check if there are any pending save requests
      if !self.save_queue.is_empty() {
        let will_drain = MAX_SAVE_BATCH_SIZE.min(self.save_queue.len());
        log::info!("saving {}/{} chunks with batch size {}...", will_drain, self.save_queue.len(), MAX_SAVE_BATCH_SIZE);
        for (pos, data) in self.save_queue.drain(..will_drain) {
          self.save.save_chunk(pos, &data).unwrap();
        }
      }
    }
  }
}

pub struct IOSingleThread {
  tx: Sender<IOCommand>,
  rx: Receiver<IOResponse>,
  handle: Option<std::thread::JoinHandle<()>>,
  header: SharedHeader,
  exit_requested: bool,
}

impl IOSingleThread {
  pub fn spawn(save: WorldSaveFile) -> Self {
    // Create channels
    let (command_tx, command_rx) = flume::unbounded();
    let (response_tx, response_rx) = flume::unbounded();

    // Grab a handle to the header
    let header = save.get_shared_header();

    // Spawn the thread
    let builder = std::thread::Builder::new()
      .name("io-thread".into());
    let handle = builder.spawn(move || {
      let context = IOThreadContext::initialize(response_tx, command_rx, save);
      context.run();
    }).unwrap();

    IOSingleThread {
      tx: command_tx,
      rx: response_rx,
      handle: Some(handle),
      header,
      exit_requested: false,
    }
  }

  /// Send a command to the IO thread
  pub fn send(&self, cmd: IOCommand) {
    self.tx.send(cmd).unwrap();
  }

  /// Poll the IO thread for a single response (non-blocking)
  pub fn poll_single(&self) -> Option<IOResponse> {
    self.rx.try_recv().ok()
  }

  /// Poll the IO thread for responses (non-blocking)
  pub fn poll(&self) -> TryIter<IOResponse> {
    self.rx.try_iter()
  }

  /// Signal the IO thread to process the remaining requests and wait for it to terminate
  #[deprecated(note = "Use stop_sync instead")]
  pub fn deprecated_stop_sync(&mut self) {
    log::debug!("Stopping IO thread (sync)");

    // Tell the thread to terminate and wait for it to finish
    if !self.exit_requested {
      self.exit_requested = true;
      self.tx.send(IOCommand::Kys).unwrap();
    }
    // while !matches!(self.rx.recv().unwrap(), IOResponse::Terminated) {}

    // // HACK "we have .join at home"
    // while !self.handle.is_finished() {}
    self.stop_async_block_on();

    log::debug!("IO thread stopped"); //almost lol
  }

  /// Same as stop_sync but doesn't wait for the IO thread to terminate
  pub fn stop_async(&mut self) {
    log::debug!("Stopping IO thread (async)");
    self.exit_requested = true;
    self.tx.send(IOCommand::Kys).unwrap();
  }

  pub fn stop_async_block_on(&mut self) {
    self.handle.take().unwrap().join().unwrap();
  }

  pub fn chunk_exists(&self, position: IVec3) -> bool {
    self.header.read().unwrap().chunk_map.contains_key(&position)
  }
}

impl Drop for IOSingleThread {
  fn drop(&mut self) {
    if self.handle.is_some() {
      log::warn!("IOSingleThread dropped without being stopped first. (about to sync unsaved data...)");
      self.deprecated_stop_sync();
    } else {
      log::trace!("IOSingleThread dropped, already stopped");
    }
  }
}

/// This is a stub for future implemntation that may use multiple IO threads
#[derive(Unique)]
pub struct IOThreadManager {
  thread: IOSingleThread,
}

impl IOThreadManager {
  pub fn new(save: WorldSaveFile) -> Self {
    Self {
      thread: IOSingleThread::spawn(save)
    }
  }

  pub fn send(&self, cmd: IOCommand) {
    self.thread.send(cmd);
  }

  pub fn poll_single(&self) -> Option<IOResponse> {
    self.thread.poll_single()
  }

  pub fn poll(&self) -> TryIter<IOResponse> {
    self.thread.poll()
  }

  pub fn chunk_exists(&self, position: IVec3) -> bool {
    self.thread.chunk_exists(position)
  }

  #[allow(deprecated)]
  #[deprecated(note = "Use stop_async and block_on_termination instead")]
  pub fn deprecated_stop_sync(&mut self) {
    self.thread.deprecated_stop_sync();
  }

  pub fn stop_async_block_on(&mut self) {
    self.thread.stop_async_block_on();
  }

  pub fn stop_async(&mut self) {
    self.thread.stop_async();
  }
}

// i think im a girl :3 (noone will ever read this right? :p)

