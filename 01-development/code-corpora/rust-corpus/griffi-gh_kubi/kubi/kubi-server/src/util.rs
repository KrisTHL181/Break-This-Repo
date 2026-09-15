use std::{net::SocketAddr, rc::Rc, cell::RefCell};
use shipyard::{View, Get, EntityId};
use uflow::server::{Event as ServerEvent, RemoteClient};
use kubi_shared::networking::{
  messages::ClientToServerMessage,
  client::{Client, ClientId}
};
use crate::{
  server::{IsMessageOfType, UdpServer}, 
  client::ClientAddressMap
};

#[derive(Clone)]
pub struct CtsMessageMetadata<'a> {
  pub message: ClientToServerMessage,
  pub client_id: ClientId,
  pub entity_id: EntityId,
  pub client_addr: SocketAddr,
  pub client: &'a Rc<RefCell<RemoteClient>>,
}
impl From<CtsMessageMetadata<'_>> for ClientToServerMessage {
  fn from(value: CtsMessageMetadata) -> Self { value.message }
}
impl From<CtsMessageMetadata<'_>> for ClientId {
  fn from(value: CtsMessageMetadata) -> Self { value.client_id }
}
impl From<CtsMessageMetadata<'_>> for EntityId {
  fn from(value: CtsMessageMetadata) -> Self { value.entity_id }
}
impl From<CtsMessageMetadata<'_>> for SocketAddr {
  fn from(value: CtsMessageMetadata) -> Self { value.client_addr }
}
impl<'a> From<CtsMessageMetadata<'a>> for &'a Rc<RefCell<RemoteClient>> {
  fn from(value: CtsMessageMetadata<'a>) -> Self { value.client }
}

pub fn check_message_auth<'a, const C_MSG: u8>(
  server: &'a UdpServer,
  event: &ServerEvent, 
  clients: &View<Client>,
  addr_map: &ClientAddressMap
) -> Option<CtsMessageMetadata<'a>> {
  let ServerEvent::Receive(client_addr, data) = event else{
    return None
  };
  if !event.is_message_of_type::<C_MSG>() {
    return None
  }
  let Some(client) = server.0.client(client_addr) else {
    log::error!("Client doesn't exist");
    return None
  };
  let Some(&entity_id) = addr_map.0.get(client_addr) else {
    log::error!("Client not authenticated");
    return None
  };
  let Ok(&Client(client_id)) = clients.get(entity_id) else {
    log::error!("Entity ID is invalid");
    return None
  };
  let Ok(message) = postcard::from_bytes(data) else {
    log::error!("Malformed message");
    return None
  };
  Some(CtsMessageMetadata {
    message,
    client_id,
    entity_id,
    client_addr: *client_addr,
    client
  })
}
