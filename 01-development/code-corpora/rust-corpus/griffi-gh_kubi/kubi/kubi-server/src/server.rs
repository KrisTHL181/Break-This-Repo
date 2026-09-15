use shipyard::{AllStoragesView, Unique, UniqueView, UniqueViewMut, NonSendSync};
use uflow::{server::{Server, Event as ServerEvent, Config as ServerConfig}, EndpointConfig};
use crate::config::ConfigTable;

#[derive(Unique)]
#[repr(transparent)]
pub struct UdpServer(pub Server);

#[derive(Unique, Default)]
pub struct ServerEvents(pub Vec<ServerEvent>);

pub trait IsMessageOfType {
  ///Checks if postcard-encoded message has a type
  fn is_message_of_type<const T: u8>(&self) -> bool;
}
impl IsMessageOfType for  ServerEvent {
  fn is_message_of_type<const T: u8>(&self) -> bool {
    let ServerEvent::Receive(_, data) = &self else { return false };
    if data.len() == 0 { return false }
    data[0] == T
  }
}

pub fn bind_server(
  storages: AllStoragesView,
) {
  log::info!("Creating server...");
  let config = storages.borrow::<UniqueView<ConfigTable>>().unwrap();
  let server = Server::bind(
    config.server.address, 
    ServerConfig {
      max_total_connections: config.server.max_clients * 2,
      max_active_connections: config.server.max_clients,
      enable_handshake_errors: true,
      endpoint_config: EndpointConfig {
        active_timeout_ms: config.server.timeout_ms,
        keepalive: true,
        keepalive_interval_ms: 5000,
        ..Default::default()
      },
    }
  ).expect("Failed to create the server");
  storages.add_unique_non_send_sync(UdpServer(server));
  storages.add_unique(ServerEvents::default());
}

pub fn update_server(
  mut server: NonSendSync<UniqueViewMut<UdpServer>>,
  mut events: UniqueViewMut<ServerEvents>,
) {
  server.0.flush();
  events.0.clear();
  events.0.extend(server.0.step());
}

pub fn log_server_errors(
  events: UniqueView<ServerEvents>,
) {
  for event in &events.0 {
    if let ServerEvent::Error(addr, error) = event {
      log::error!("Server error addr: {addr} error: {error:?}");
    }
  }
}
