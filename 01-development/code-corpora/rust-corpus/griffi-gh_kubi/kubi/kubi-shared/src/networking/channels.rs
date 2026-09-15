#[repr(u8)]
pub enum Channel {
  #[deprecated]
  Generic = 0,
  /// Used during the initial handshake process
  Auth = 1,
  /// Used for sending chunk data from server to client
  WorldData = 2,
  /// Used for sending/receiving block place events
  Block = 3,
  /// Used for sending/receiving player movements
  Move = 4,
  /// Used for system events, like players joining or leaving
  SysEvt = 5,
  /// Used for subscribing and unsubscribing from chunks
  SubReq = 6,
}
