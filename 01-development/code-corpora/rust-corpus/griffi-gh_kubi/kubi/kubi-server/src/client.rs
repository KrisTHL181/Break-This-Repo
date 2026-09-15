use glam::Mat4;
use shipyard::{AllStoragesView, AllStoragesViewMut, Component, EntityId, Get, IntoIter, NonSendSync, Unique, UniqueView, UniqueViewMut, View, ViewMut};
use hashbrown::HashMap;
use uflow::{server::Event, SendMode};
use std::net::SocketAddr;
use kubi_shared::{
  networking::{
    client::{ClientIdMap, Client},
    messages::{ClientToServerMessage, ServerToClientMessage, ClientToServerMessageType},
    channels::Channel
  },
  transform::Transform
};
use crate::{
  server::{ServerEvents, UdpServer},
  util::check_message_auth, world::ChunkManager
};

#[derive(Component, Clone, Copy)]
pub struct ClientAddress(pub SocketAddr);

#[derive(Unique, Default)]
pub struct ClientAddressMap(pub HashMap<SocketAddr, EntityId>);
impl ClientAddressMap {
  pub fn new() -> Self { Self::default() }
}

pub fn init_client_maps(
  storages: AllStoragesView
) {
  storages.add_unique(ClientIdMap::new());
  storages.add_unique(ClientAddressMap::new());
}

pub fn sync_client_positions(
  server: NonSendSync<UniqueView<UdpServer>>,
  events: UniqueView<ServerEvents>,
  addr_map: UniqueView<ClientAddressMap>,
  clients: View<Client>,
  mut transforms: ViewMut<Transform>,
  addrs: View<ClientAddress>,
) {
  for event in &events.0 {
    let Some(message) = check_message_auth
      ::<{ClientToServerMessageType::PositionChanged as u8}>
      (&server, event, &clients, &addr_map) else { continue };

    let ClientToServerMessage::PositionChanged { position, velocity: _, direction } = message.message else {
      unreachable!()
    };

    //log movement (annoying duh)
    // log::debug!("dbg: player moved id: {} coords: {} quat: {}", message.client_id, position, direction);

    //Apply position to server-side client
    let mut trans = (&mut transforms).get(message.entity_id).unwrap();
    trans.0 = Mat4::from_rotation_translation(direction, position);

    //Transmit the change to other players
    for (other_client, other_client_address) in (&clients, &addrs).iter() {
      if other_client.0 == message.client_id {
        continue
      }
      let Some(client) = server.0.client(&other_client_address.0) else {
        log::error!("Client with address not found");
        continue
      };
      client.borrow_mut().send(
        postcard::to_allocvec(
          &ServerToClientMessage::PlayerPositionChanged {
            client_id: message.client_id,
            position,
            direction
          }
        ).unwrap().into_boxed_slice(),
        Channel::Move as usize,
        SendMode::Reliable
      );
    }
  }
}

pub fn on_client_disconnect(
  mut all_storages: AllStoragesViewMut,
) {
  let mut to_delete = Vec::new();
  {
    let server = all_storages.borrow::<NonSendSync<UniqueView<UdpServer>>>().unwrap();
    let events = all_storages.borrow::<UniqueView<ServerEvents>>().unwrap();
    let mut addr_map = all_storages.borrow::<UniqueViewMut<ClientAddressMap>>().unwrap();
    let mut id_map = all_storages.borrow::<UniqueViewMut<ClientIdMap>>().unwrap();
    let clients = all_storages.borrow::<View<Client>>().unwrap();
    let mut chunk_manager = all_storages.borrow::<UniqueViewMut<ChunkManager>>().unwrap();
    let addrs = all_storages.borrow::<View<ClientAddress>>().unwrap();

    for event in &events.0 {
      if let Event::Disconnect(addr) = event {
        //XXX: do sth with this:
        //let net_client = server.0.client(addr).unwrap();
        let Some(&entity_id) = addr_map.0.get(addr) else {
          log::error!("Disconnected client not authenticated, moving on");
          continue;
        };
        let client_id = clients.get(entity_id).unwrap().0;
        log::info!("Client disconnected: ID {}", client_id);

        addr_map.0.remove(addr);
        id_map.0.remove(&client_id);
        to_delete.push(entity_id);

        //unsubscribe from chunks
        chunk_manager.unsubscribe_all(client_id);

        //send disconnect message to other clients
        for (_, other_client_address) in (&clients, &addrs).iter() {
          let Some(client) = server.0.client(&other_client_address.0) else {
            log::error!("Client with address not found");
            continue
          };
          client.borrow_mut().send(
            postcard::to_allocvec(
              &ServerToClientMessage::PlayerDisconnected { id: client_id }
            ).unwrap().into_boxed_slice(),
            Channel::SysEvt as usize,
            SendMode::Reliable
          );
        }
      }
    }

  }
  for entity_id in to_delete {
    all_storages.delete_entity(entity_id);
  }
}
