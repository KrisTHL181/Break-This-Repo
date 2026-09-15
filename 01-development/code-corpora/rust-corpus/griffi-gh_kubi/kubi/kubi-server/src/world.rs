use shipyard::{AllStoragesView, Get, IntoIter, IntoWorkload, NonSendSync, SystemModificator, Unique, UniqueView, UniqueViewMut, View, Workload};
use glam::IVec3;
use hashbrown::HashMap;
use kubi_shared::{
  chunk::CHUNK_SIZE,
  queue::QueuedBlock,
  networking::{
    channels::Channel,
    client::{Client, ClientId},
    messages::{ClientToServerMessage, ClientToServerMessageType, ServerToClientMessage}
  },
};
use uflow::{server::RemoteClient, SendMode};
use lz4_flex::compress_prepend_size as lz4_compress;
use anyhow::Result;
use std::{cell::RefCell, rc::Rc};
use kubi_shared::networking::client::ClientIdMap;
use crate::{
  server::{UdpServer, ServerEvents}, 
  config::ConfigTable,
  client::{ClientAddress, ClientAddressMap}, 
  util::check_message_auth, 
};

pub mod chunk;
pub mod tasks;
pub mod save;

use chunk::Chunk;

use self::{
  tasks::{ChunkTaskManager, ChunkTask, ChunkTaskResponse, init_chunk_task_manager},
  chunk::ChunkState,
};

#[derive(Unique, Default)]
pub struct LocalBlockQueue {
  pub queue: Vec<QueuedBlock>,
}

#[derive(Unique, Default)]
pub struct ChunkManager {
  pub chunks: HashMap<IVec3, Chunk>
}
impl ChunkManager {
  pub fn unsubscribe_all(&mut self, client_id: ClientId) {
    for chunk in self.chunks.values_mut() {
      chunk.subscriptions.remove(&client_id);
    }
  }
  pub fn new() -> Self {
    Self::default()
  }
}

///Sends a compressed chunk packet
pub fn send_chunk_compressed(
  client: &Rc<RefCell<RemoteClient>>,
  message: &ServerToClientMessage
) -> Result<()> {
  let mut ser_message = postcard::to_allocvec(&message)?;
  let mut compressed = lz4_compress(&ser_message[1..]);
  ser_message.truncate(1);
  ser_message.append(&mut compressed);
  let ser_message = ser_message.into_boxed_slice();
  client.borrow_mut().send(
    ser_message,
    Channel::WorldData as usize,
    SendMode::Reliable
  );
  Ok(())
}

fn process_chunk_requests(
  server: NonSendSync<UniqueView<UdpServer>>,
  events: UniqueView<ServerEvents>,
  mut chunk_manager: UniqueViewMut<ChunkManager>,
  task_manager: UniqueView<ChunkTaskManager>,
  config: UniqueView<ConfigTable>,
  addr_map: UniqueView<ClientAddressMap>,
  clients: View<Client>
) {
  for event in &events.0 {
    let Some(message) = check_message_auth
      ::<{ClientToServerMessageType::ChunkSubRequest as u8}>
      (&server, event, &clients, &addr_map) else { continue };

    let ClientToServerMessage::ChunkSubRequest { chunk: chunk_position } = message.message else {
      unreachable!()
    };

    if let Some(chunk) = chunk_manager.chunks.get_mut(&chunk_position) {
      chunk.subscriptions.insert(message.client_id);
      //TODO Start task here if status is "Nothing"
      if let Some(blocks) = &chunk.blocks {
        send_chunk_compressed(
          message.client,
          &ServerToClientMessage::ChunkResponse {
            chunk: chunk_position,
            data: blocks.clone(),
            queued: Vec::with_capacity(0)
          }
        ).unwrap();
      }
    } else {
      let mut chunk = Chunk::new();
      chunk.state = ChunkState::Loading;
      chunk.subscriptions.insert(message.client_id);
      chunk_manager.chunks.insert(chunk_position, chunk);
      task_manager.run(ChunkTask::LoadChunk {
        position: chunk_position,
        seed: config.world.seed,
      });
    }
  }
}

fn process_finished_tasks(
  server: NonSendSync<UniqueView<UdpServer>>,
  task_manager: UniqueView<ChunkTaskManager>,
  mut chunk_manager: UniqueViewMut<ChunkManager>,
  id_map: UniqueView<ClientIdMap>,
  client_addr: View<ClientAddress>,
  mut local_queue: UniqueViewMut<LocalBlockQueue>,
) {
  'outer: while let Some(res) = task_manager.receive() {
    let ChunkTaskResponse::ChunkLoaded { chunk_position, blocks, queue } = res;
    let Some(chunk) = chunk_manager.chunks.get_mut(&chunk_position) else {
      log::warn!("Chunk discarded: Doesn't exist");
      continue
    };
    if chunk.state != ChunkState::Loading {
      log::warn!("Chunk discarded: Not Loading");
      continue
    }
    chunk.state = ChunkState::Loaded;
    chunk.blocks = Some(blocks.clone());

    local_queue.queue.extend_from_slice(&queue);

    log::debug!("Chunk {chunk_position} loaded, {} subs", chunk.subscriptions.len());

    let chunk_packet = &ServerToClientMessage::ChunkResponse {
      chunk: chunk_position,
      data: blocks,
      queued: queue //should this be here?
    };

    for &subscriber in &chunk.subscriptions {
      let Some(&entity_id) = id_map.0.get(&subscriber) else {
        log::error!("Invalid subscriber client id");
        continue 'outer;
      };
      let Ok(&ClientAddress(client_addr)) = (&client_addr).get(entity_id) else {
        log::error!("Invalid subscriber entity id");
        continue 'outer;
      };
      let Some(client) = server.0.client(&client_addr) else {
        log::error!("Client not connected");
        continue 'outer;
      };
      send_chunk_compressed(client, chunk_packet).unwrap();
      // client.borrow_mut().send(
      //   chunk_packet.clone(),
      //   CHANNEL_WORLD,
      //   SendMode::Reliable,
      // );
    }
  }
}

fn process_chunk_unsubscribe_events(
  server: NonSendSync<UniqueView<UdpServer>>,
  events: UniqueView<ServerEvents>,
  mut chunk_manager: UniqueViewMut<ChunkManager>,
  addr_map: UniqueView<ClientAddressMap>,
  clients: View<Client>
) {
  for event in &events.0 {
    let Some(message) = check_message_auth
      ::<{ClientToServerMessageType::ChunkUnsubscribe as u8}>
      (&server, event, &clients, &addr_map) else { continue };

    let ClientToServerMessage::ChunkUnsubscribe { chunk: chunk_position } = message.message else {
      unreachable!()
    };

    let Some(chunk) = chunk_manager.chunks.get_mut(&chunk_position) else {
      log::warn!("tried to unsubscribe from non-existent chunk");
      continue
    };

    chunk.subscriptions.remove(&message.client_id);
    //TODO unload chunk if no more subscribers
  }
}

fn process_block_queue_messages(
  server: NonSendSync<UniqueView<UdpServer>>,
  events: UniqueView<ServerEvents>,
  addr_map: UniqueView<ClientAddressMap>,
  clients: View<Client>,
  addrs: View<ClientAddress>,
  mut queue: UniqueViewMut<LocalBlockQueue>,
) {
  for event in &events.0 {
    let Some(message) = check_message_auth
      ::<{ClientToServerMessageType::QueueBlock as u8}>
      (&server, event, &clients, &addr_map) else { continue };

    let ClientToServerMessage::QueueBlock { item } = message.message else { unreachable!() };

    //place in our local world
    queue.queue.push(item);

    log::info!("Placed block {:?} at {}", item.block_type, item.position);
    for (other_client, other_client_address) in (&clients, &addrs).iter() {
      //No need to send the event back
      if message.client_id == other_client.0 {
        continue
      }
      //Get client
      let Some(client) = server.0.client(&other_client_address.0) else {
        log::error!("Client with address not found");
        continue
      };
      //Send the message
      client.borrow_mut().send(
        postcard::to_allocvec(
          &ServerToClientMessage::QueueBlock { item }
        ).unwrap().into_boxed_slice(), 
        Channel::Block as usize,
        SendMode::Reliable,
      );
    }
  }
}

fn process_block_queue(
  mut chunk_manager: UniqueViewMut<ChunkManager>,
  mut queue: UniqueViewMut<LocalBlockQueue>,
) {
  let initial_len = queue.queue.len();
  queue.queue.retain(|item| {
    let chunk_position = item.position.div_euclid(IVec3::splat(CHUNK_SIZE as i32));
    let block_position = item.position.rem_euclid(IVec3::splat(CHUNK_SIZE as i32));
    let Some(chunk) = chunk_manager.chunks.get_mut(&chunk_position) else {
      return true
    };
    let Some(blocks) = &mut chunk.blocks else {
      return true
    };
    let block = &mut blocks[block_position.x as usize][block_position.y as usize][block_position.z as usize];
    if item.block_type != *block {
      *block = item.block_type;
      chunk.data_modified = true;
    }
    false
  });
  if initial_len != queue.queue.len() {
    log::debug!("queue processed {}/{} items", initial_len - queue.queue.len(), initial_len);
  }
}

/// init local block queue and chunk manager
fn init_chunk_manager_and_block_queue(
  storages: AllStoragesView
) {
  storages.add_unique(ChunkManager::new());
  storages.add_unique(LocalBlockQueue::default());
}

pub fn preheat_world(
  mut chunk_manager: UniqueViewMut<ChunkManager>,
  task_manager: UniqueView<ChunkTaskManager>,
  config: UniqueView<ConfigTable>,
) {
  let r = config.world.preheat_radius as i32;
  for x in -r..=r {
    for y in -r..=r {
      for z in -r..=r {
        let chunk_position = IVec3::new(x, y, z);
        let mut chunk = Chunk::new();
        chunk.state = ChunkState::Loading;
        chunk_manager.chunks.insert(chunk_position, chunk);
        task_manager.run(ChunkTask::LoadChunk {
          position: chunk_position,
          seed: config.world.seed,
        });
      }
    }
  }
}

pub fn init_world() -> Workload {
  (
    init_chunk_manager_and_block_queue.before_all(preheat_world),
    init_chunk_task_manager.before_all(preheat_world),
    preheat_world,
  ).into_sequential_workload()
}

pub fn update_world() -> Workload {
  (
    process_finished_tasks,
    process_block_queue_messages,
    process_block_queue,
    process_chunk_unsubscribe_events,
    process_chunk_requests,
  ).into_sequential_workload()
}
