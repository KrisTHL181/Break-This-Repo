use glam::Mat4;
use shipyard::{UniqueViewMut, View, IntoIter, AllStoragesView, AllStoragesViewMut, UniqueView, ViewMut, Get};
use uflow::{SendMode, client::Event as ClientEvent};
use kubi_shared::{
  transform::Transform,
  networking::{
    channels::Channel,
    client::{ClientIdMap, Username},
    messages::{ClientToServerMessage, ServerToClientMessage, ServerToClientMessageType},
  },
};
use crate::{
  chat::ChatHistory,
  events::player_actions::PlayerActionEvent,
  player::spawn_remote_player_multiplayer,
};
use super::{UdpClient, NetworkEvent};

pub fn init_client_map(
  storages: AllStoragesView,
) {
  storages.add_unique(ClientIdMap::new());
}

pub fn send_player_movement_events(
  actions: View<PlayerActionEvent>,
  mut client: UniqueViewMut<UdpClient>,
) {
  for event in actions.iter() {
    let PlayerActionEvent::PositionChanged { position, velocity, direction } = event else {
      continue
    };
    client.0.send(
      postcard::to_allocvec(&ClientToServerMessage::PositionChanged {
        position: *position,
        velocity: *velocity,
        direction: *direction
      }).unwrap().into_boxed_slice(), 
      Channel::Move as usize,
      SendMode::TimeSensitive
    );
  }
}

pub fn receive_player_movement_events(
  mut transforms: ViewMut<Transform>,
  network_events: View<NetworkEvent>,
  id_map: UniqueView<ClientIdMap>
) {
  for event in network_events.iter() {
    let ClientEvent::Receive(data) = &event.0 else {
      continue
    };

    if !event.is_message_of_type::<{ServerToClientMessageType::PlayerPositionChanged as u8}>() {
      continue
    }

    let Ok(parsed_message) = postcard::from_bytes(data) else {
      log::error!("Malformed message");
      continue
    };

    let ServerToClientMessage::PlayerPositionChanged {
      client_id, position, direction
    } = parsed_message else { unreachable!() };

    let Some(&ent_id) = id_map.0.get(&client_id) else {
      log::error!("Not in client-id map");
      continue
    };

    let mut transform = (&mut transforms).get(ent_id)
      .expect("invalid player entity id");

    transform.0 = Mat4::from_rotation_translation(direction, position);
  }
}

pub fn receive_player_connect_events(
  mut storages: AllStoragesViewMut,
) {
  let messages: Vec<ServerToClientMessage> = storages.borrow::<View<NetworkEvent>>().unwrap().iter().filter_map(|event| {
    let ClientEvent::Receive(data) = &event.0 else {
      return None
    };
    if !event.is_message_of_type::<{ServerToClientMessageType::PlayerConnected as u8}>() {
      return None
    };
    let Ok(parsed_message) = postcard::from_bytes(data) else {
      log::error!("Malformed message");
      return None
    };
    Some(parsed_message)
  }).collect();

  for message in messages {
    let ServerToClientMessage::PlayerConnected { init } = message else { unreachable!() };
    log::info!("player connected: {} (id {})", init.username, init.client_id);
    let mut chat = storages.borrow::<UniqueViewMut<ChatHistory>>().unwrap();
    chat.add_player_join(init.client_id, init.username.clone());
    drop(chat);
    spawn_remote_player_multiplayer(&mut storages, init);
  }
}

pub fn receive_player_disconnect_events(
  mut storages: AllStoragesViewMut,
) {
  let messages: Vec<ServerToClientMessage> = storages.borrow::<View<NetworkEvent>>().unwrap().iter().filter_map(|event| {
    let ClientEvent::Receive(data) = &event.0 else {
      return None
    };
    if !event.is_message_of_type::<{ServerToClientMessageType::PlayerDisconnected as u8}>() {
      return None
    };
    let Ok(parsed_message) = postcard::from_bytes(data) else {
      log::error!("Malformed message");
      return None
    };
    Some(parsed_message)
  }).collect();

  for message in messages {
    let ServerToClientMessage::PlayerDisconnected { id } = message else { unreachable!() };
    log::info!("player disconnected: {}", id);

    let mut id_map = storages.borrow::<UniqueViewMut<ClientIdMap>>().unwrap();
    let Some(ent_id) = id_map.0.remove(&id) else {
      log::warn!("Disconnected player entity not found in client-id map");
      continue
    };

    let username = storages.get::<&Username>(ent_id).unwrap();
    let mut chat = storages.borrow::<UniqueViewMut<ChatHistory>>().unwrap();
    chat.add_player_leave(id, username.0.to_string());

    drop(chat);
    drop(id_map);
    drop(username);

    if !storages.delete_entity(ent_id) {
      log::warn!("Disconnected player entity not found in storage");
    }
  }
}
