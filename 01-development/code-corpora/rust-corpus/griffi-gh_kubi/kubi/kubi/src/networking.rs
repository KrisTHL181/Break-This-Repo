use shipyard::{Unique, AllStoragesView, UniqueView, UniqueViewMut, Workload, IntoWorkload, EntitiesViewMut, Component, ViewMut, SystemModificator, View, IntoIter, WorkloadModificator};
use std::net::SocketAddr;
use uflow::{
  client::{Client, Config as ClientConfig, Event as ClientEvent},
  EndpointConfig
};
use kubi_shared::networking::{
  messages::ServerToClientMessage,
  state::ClientJoinState,
  client::ClientIdMap,
};
use crate::{
  events::EventComponent,
  fixed_timestamp::FixedTimestamp,
  state::{is_ingame_or_loading, is_ingame_or_loading_or_connecting_or_shutting_down},
  world::tasks::ChunkTaskManager,
};

mod handshake;
mod world;
mod player;

pub use handshake::ConnectionRejectionReason;
use handshake::{
  set_client_join_state_to_connected,
  say_hello,
  check_server_hello_response,
  check_server_fuck_off_response,
};
use world::{
  inject_network_responses_into_manager_queue,
  send_block_place_events,
  recv_block_place_events,
};
use player::{
  init_client_map,
  send_player_movement_events,
  receive_player_movement_events, 
  receive_player_connect_events,
  receive_player_disconnect_events,
};

const NET_TICKRATE: u16 = 33;

#[derive(Unique, Clone, Copy, PartialEq, Eq)]
pub enum GameType {
  Singleplayer,
  Muliplayer
}

#[derive(Unique, Clone, Copy, PartialEq, Eq)]
pub struct ServerAddress(pub SocketAddr);

#[derive(Unique)]
pub struct UdpClient(pub Client);

#[derive(Component)]
pub struct NetworkEvent(pub ClientEvent);

impl NetworkEvent {
  ///Checks if postcard-encoded message has a type
  pub fn is_message_of_type<const T: u8>(&self) -> bool {
    let ClientEvent::Receive(data) = &self.0 else { return false };
    if data.len() == 0 { return false }
    data[0] == T
  }
}

#[derive(Component)]
pub struct NetworkMessageEvent(pub ServerToClientMessage);

fn connect_client(
  storages: AllStoragesView
) {
  log::info!("Creating client");
  let address = storages.borrow::<UniqueView<ServerAddress>>().unwrap();
  let client = Client::connect(address.0, ClientConfig {
    endpoint_config: EndpointConfig {
      active_timeout_ms: 10000,
      keepalive: true,
      keepalive_interval_ms: 5000,
      ..Default::default()
    },
  }).expect("Client connection failed");
  storages.add_unique(UdpClient(client));
  storages.add_unique(ClientJoinState::Disconnected);
}

fn poll_client(
  mut client: UniqueViewMut<UdpClient>,
  mut entities: EntitiesViewMut,
  mut events: ViewMut<EventComponent>,
  mut network_events: ViewMut<NetworkEvent>,
) {
  entities.bulk_add_entity((
    &mut events,
    &mut network_events,
  ), client.0.step().map(|event| {
    (EventComponent, NetworkEvent(event))
  }));
}

fn flush_client(
  mut client: UniqueViewMut<UdpClient>,
) {
  client.0.flush();
}

fn handle_disconnect(
  network_events: View<NetworkEvent>,
  mut join_state: UniqueViewMut<ClientJoinState>
) {
  for event in network_events.iter() {
    if matches!(event.0, ClientEvent::Disconnect) {
      log::warn!("Disconnected from server");
      *join_state = ClientJoinState::Disconnected;
      return;
    }
  }
}

pub fn update_networking() -> Workload {
  (
    init_client_map.run_if_missing_unique::<ClientIdMap>(),
    connect_client.run_if_missing_unique::<UdpClient>(),
    poll_client.into_workload().make_fixed(NET_TICKRATE, 0),
    (
      set_client_join_state_to_connected,
      say_hello,
    ).into_sequential_workload().run_if(if_just_connected),
    (
      check_server_hello_response,
      check_server_fuck_off_response,
      handle_disconnect,
    ).into_sequential_workload().run_if(is_join_state::<{ClientJoinState::Connected as u8}>),
    (
      (
        receive_player_connect_events,
        receive_player_disconnect_events,
      ).into_workload(),
      (
        recv_block_place_events,
        receive_player_movement_events,
      ).into_workload()
    ).into_sequential_workload().run_if(is_join_state::<{ClientJoinState::Joined as u8}>).run_if(is_ingame_or_loading),
    inject_network_responses_into_manager_queue.run_if(is_ingame_or_loading).skip_if_missing_unique::<ChunkTaskManager>(),
  ).into_sequential_workload()
}

pub fn update_networking_late() -> Workload {
  (
    (
      send_block_place_events,
      send_player_movement_events,
    ).into_workload().run_if(is_join_state::<{ClientJoinState::Joined as u8}>),
    flush_client.into_workload().make_fixed(NET_TICKRATE, 1)
  ).into_sequential_workload()
}

pub fn disconnect_on_exit(
  mut client: UniqueViewMut<UdpClient>,
) {
  if client.0.is_active() {
    client.0.flush();
    client.0.disconnect();
    while client.0.is_active() { client.0.step().for_each(|_|()); }
    log::info!("Client disconnected");
  } else {
    log::info!("Client inactive")
  }
}

// conditions

fn if_just_connected(
  network_events: View<NetworkEvent>,
) -> bool {
  network_events.iter().any(|event| matches!(&event.0, ClientEvent::Connect))
}

fn is_join_state<const STATE: u8>(
  join_state: UniqueView<ClientJoinState>
) -> bool {
  (*join_state as u8) == STATE
}

pub fn is_multiplayer(
  game_type: Option<UniqueView<GameType>>
) -> bool {
  let Some(game_type) = game_type else { return false };
  *game_type == GameType::Muliplayer
}

pub fn is_singleplayer(
  game_type: Option<UniqueView<GameType>>
) -> bool {
  let Some(game_type) = game_type else { return false };
  *game_type == GameType::Singleplayer
}
