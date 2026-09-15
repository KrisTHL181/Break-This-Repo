use kubi_shared::networking::client::ClientId;
use shipyard::{AllStoragesView, Unique};

pub enum ChatMessage {
  PlayerMessage {
    id: ClientId,
    username: String,
    message: String,
  },
  PlayerJoin {
    id: ClientId,
    username: String,
  },
  PlayerLeave {
    id: ClientId,
    username: String,
  },
  System(String),
}

// impl ChatMessage {
//   pub fn render() -> String {
//     todo!() //TODO
//   }
// }

#[derive(Unique, Default)]
pub struct ChatHistory {
  pub messages: Vec<ChatMessage>,
}

impl ChatHistory {
  pub fn add_message(&mut self, message: ChatMessage) {
    self.messages.push(message);
  }

  pub fn add_chat_message(&mut self, id: ClientId, username: String, message: String,) {
    self.messages.push(ChatMessage::PlayerMessage { id, username, message });
  }

  pub fn add_player_join(&mut self, id: ClientId, username: String) {
    self.messages.push(ChatMessage::PlayerJoin { id, username });
  }

  pub fn add_player_leave(&mut self, id: ClientId, username: String) {
    self.messages.push(ChatMessage::PlayerLeave { id, username });
  }

  pub fn add_system_message(&mut self, message: String) {
    self.messages.push(ChatMessage::System(message));
  }

  pub fn get_messages(&self) -> &[ChatMessage] {
    &self.messages[..]
  }
}

pub fn init_chat_manager(
  storages: AllStoragesView,
) {
  let mut chat_manager = ChatHistory::default();
  chat_manager.add_system_message("Welcome to Kubi! Chat messages will appear here".to_string());
  chat_manager.add_system_message("F1 (Hold): Settings; F3: Release cursor; F4/F5: Gamemode".to_string());
  storages.add_unique(chat_manager);
}
