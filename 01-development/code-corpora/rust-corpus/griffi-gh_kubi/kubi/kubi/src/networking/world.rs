use shipyard::{UniqueView, UniqueViewMut, View, IntoIter};
use uflow::{client::Event as ClientEvent, SendMode};
use lz4_flex::decompress_size_prepended;
use anyhow::{Result, Context};
use kubi_shared::{
  networking::{
    messages::{ClientToServerMessage, ServerToClientMessage, ServerToClientMessageType},
    channels::Channel,
  },
  queue::QueuedBlock
};
use crate::{
  events::player_actions::PlayerActionEvent, 
  world::{
    tasks::{ChunkTaskResponse, ChunkTaskManager},
    queue::BlockUpdateQueue
  },
};
use super::{NetworkEvent, UdpClient};

//TODO multithreaded decompression
fn decompress_chunk_packet(data: &[u8]) -> Result<ServerToClientMessage> {
  let mut decompressed = decompress_size_prepended(&data[1..])?;
  decompressed.insert(0, data[0]);
  postcard::from_bytes(&decompressed).ok().context("Deserialization failed")
}

//TODO get rid of this, this is awfulll
pub fn inject_network_responses_into_manager_queue(
  manager: UniqueView<ChunkTaskManager>,
  events: View<NetworkEvent>
) {
  for event in events.iter() {
    if event.is_message_of_type::<{ServerToClientMessageType::ChunkResponse as u8}>() {
      let NetworkEvent(ClientEvent::Receive(data)) = &event else { unreachable!() };
      let packet = decompress_chunk_packet(data).expect("Chunk decode failed");
      let ServerToClientMessage::ChunkResponse {
        chunk, data, queued
      } = packet else { unreachable!() };
      manager.add_sussy_response(ChunkTaskResponse::ChunkWorldgenDone {
        position: chunk,
        chunk_data: data,
        queued
      });
    }
  }
}

pub fn send_block_place_events(
  action_events: View<PlayerActionEvent>,
  mut client: UniqueViewMut<UdpClient>,
) {
  for event in action_events.iter() {
    let PlayerActionEvent::UpdatedBlock { position, block } = event else {
      continue
    };
    client.0.send(
      postcard::to_allocvec(&ClientToServerMessage::QueueBlock {
        item: QueuedBlock {
          position: *position,
          block_type: *block,
          soft: false
        }
      }).unwrap().into_boxed_slice(),
      Channel::Block as usize,
      SendMode::Reliable,
    );
  }
}

pub fn recv_block_place_events(
  mut queue: UniqueViewMut<BlockUpdateQueue>,
  network_events: View<NetworkEvent>,
) {
  for event in network_events.iter() {
    let ClientEvent::Receive(data) = &event.0 else {
      continue
    };
    if !event.is_message_of_type::<{ServerToClientMessageType::QueueBlock as u8}>() {
      continue
    }
    let Ok(parsed_message) = postcard::from_bytes(data) else {
      log::error!("Malformed message");
      continue
    };
    let ServerToClientMessage::QueueBlock { item } = parsed_message else {
      unreachable!()
    };
    queue.0.push(item);
  }
}
