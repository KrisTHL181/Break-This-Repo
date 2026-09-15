use glam::{Vec3, IVec3, Quat};
use serde::{Serialize, Deserialize};
use crate::{chunk::{BlockData, CHUNK_SIZE}, queue::QueuedBlock, entity::Health};
use super::client::ClientId;

pub const PROTOCOL_ID: u16 = 0;

pub trait ToMessageType<T> {
  fn message_type(&self) -> T;
}

#[repr(u8)]
#[non_exhaustive]
pub enum ClientToServerMessageType {
  ClientHello = 0,
  PositionChanged = 1,
  ChunkSubRequest = 2,
  ChunkUnsubscribe = 3,
  QueueBlock = 4,
}

#[derive(Serialize, Deserialize, Clone)]
#[repr(u8)]
#[non_exhaustive]
pub enum ClientToServerMessage {
  ClientHello {
    username: String,
    password: Option<String>,
  } = ClientToServerMessageType::ClientHello as u8,
  PositionChanged {
    position: Vec3,
    velocity: Vec3,
    direction: Quat,
  } = ClientToServerMessageType::PositionChanged as u8,
  ChunkSubRequest {
    chunk: IVec3,
  } = ClientToServerMessageType::ChunkSubRequest as u8,
  ChunkUnsubscribe {
    chunk: IVec3,
  } = ClientToServerMessageType::ChunkUnsubscribe as u8,
  QueueBlock {
    item: QueuedBlock
  } = ClientToServerMessageType::QueueBlock as u8,
}

impl ToMessageType<ClientToServerMessageType> for ClientToServerMessage {
  fn message_type(&self) -> ClientToServerMessageType {
    match self {
      ClientToServerMessage::ClientHello { .. } => ClientToServerMessageType::ClientHello,
      ClientToServerMessage::PositionChanged { .. } => ClientToServerMessageType::PositionChanged,
      ClientToServerMessage::ChunkSubRequest { .. } => ClientToServerMessageType::ChunkSubRequest,
      ClientToServerMessage::ChunkUnsubscribe { .. } => ClientToServerMessageType::ChunkUnsubscribe,
      ClientToServerMessage::QueueBlock { .. } => ClientToServerMessageType::QueueBlock,
    }
  }
}

#[repr(u8)]
#[non_exhaustive]
pub enum ServerToClientMessageType {
  ServerHello = 0,
  ServerFuckOff = 1,
  PlayerPositionChanged = 2,
  ChunkResponse = 3,
  QueueBlock = 4,
  PlayerConnected = 5,
  PlayerDisconnected = 6,
}

#[serde_with::serde_as]
#[derive(Serialize, Deserialize, Clone)]
#[repr(u8)]
#[non_exhaustive]
pub enum ServerToClientMessage {
  ServerHello {
    init: InitData
  } = ServerToClientMessageType::ServerHello as u8,

  ServerFuckOff {
    reason: String,
  } = ServerToClientMessageType::ServerFuckOff as u8,

  PlayerPositionChanged {
    client_id: ClientId,
    position: Vec3,
    direction: Quat,
  } = ServerToClientMessageType::PlayerPositionChanged as u8,

  ///## WARNING: THIS IS COMPRESSED
  ///MESSAGES OF THIS TYPE ARE FULLY
  ///COMPRESSED ***EXCEPT THE FIRST BYTE***
  ///TO REDUCE NETWORK USAGE
  ChunkResponse {
    chunk: IVec3,
    #[serde_as(as = "Box<[[[_; CHUNK_SIZE]; CHUNK_SIZE]; CHUNK_SIZE]>")]
    data: BlockData,
    queued: Vec<QueuedBlock>,
  } = ServerToClientMessageType::ChunkResponse as u8,

  QueueBlock {
    item: QueuedBlock
  } = ServerToClientMessageType::QueueBlock as u8,

  PlayerConnected {
    init: ClientInitData
  } = ServerToClientMessageType::PlayerConnected as u8,

  PlayerDisconnected {
    id: ClientId
  } = ServerToClientMessageType::PlayerDisconnected as u8,
}

impl ToMessageType<ServerToClientMessageType> for ServerToClientMessage {
  fn message_type(&self) -> ServerToClientMessageType {
    match self {
      ServerToClientMessage::ServerHello { .. } => ServerToClientMessageType::ServerHello,
      ServerToClientMessage::ServerFuckOff { .. } => ServerToClientMessageType::ServerFuckOff,
      ServerToClientMessage::PlayerPositionChanged { .. } => ServerToClientMessageType::PlayerPositionChanged,
      ServerToClientMessage::ChunkResponse { .. } => ServerToClientMessageType::ChunkResponse,
      ServerToClientMessage::QueueBlock { .. } => ServerToClientMessageType::QueueBlock,
      ServerToClientMessage::PlayerConnected { .. } => ServerToClientMessageType::PlayerConnected,
      ServerToClientMessage::PlayerDisconnected { .. } => ServerToClientMessageType::PlayerDisconnected,
    }
  }
}

//---

#[derive(Serialize, Deserialize, Clone)]
pub struct ClientInitData {
  pub client_id: ClientId,
  pub username: String,
  pub position: Vec3,
  pub velocity: Vec3,
  pub direction: Quat,
  pub health: Health,
}

#[derive(Serialize, Deserialize, Clone)]
pub struct InitData {
  pub user: ClientInitData,
  pub users: Vec<ClientInitData>
}
