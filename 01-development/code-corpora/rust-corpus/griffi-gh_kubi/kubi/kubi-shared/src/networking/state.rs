use shipyard::{Unique, Component};

// disconnected => connect => join => load => ingame
#[derive(Unique, Component, PartialEq, Eq, Clone, Copy, Debug)]
#[repr(u8)]
pub enum ClientJoinState {
  /// Not connected yet
  Disconnected,
  /// Client has connected to the game, but hasn't authenticated yet
  Connected,
  /// Client has joined the game, but hasn't loaded the world yet
  Joined,
  /// Client is currently ingame 
  InGame,
}
