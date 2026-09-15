use hui::{color, element::{container::Container, text::Text, UiElementExt}, layout::Alignment, size};
use shipyard::{NonSendSync, UniqueView, UniqueViewMut};
use crate::{chat::{ChatHistory, ChatMessage}, hui_integration::UiState, rendering::Renderer};

pub fn render_chat(
  mut hui: NonSendSync<UniqueViewMut<UiState>>,
  ren: UniqueView<Renderer>,
  chat: UniqueView<ChatHistory>,
) {
  let messages = chat.get_messages();
  if messages.is_empty() { return }
  Container::default()
    .with_size(size!(100%, 100%))
    .with_align((Alignment::Begin, Alignment::End))
    .with_children(|ui| {
      for message in messages.iter().rev().take(10).rev() {
        let (text, color) = match message {
          ChatMessage::PlayerMessage { username, id, message } => {
            (format!("{username} ({id}): {message}"), color::CYAN)
          }
          ChatMessage::PlayerJoin { username, id } => {
            (format!("{username} ({id}) joined the game"), color::YELLOW)
          }
          ChatMessage::PlayerLeave { username, id } => {
            (format!("{username} ({id}) left the game"), color::YELLOW)
          }
          ChatMessage::System(message) => {
            (message.clone(), color::WHITE)
          }
        };
        Container::default()
          .with_background((0., 0., 0., 0.5))
          .with_padding((5., 2.))
          .with_children(|ui| {
            Text::new(text)
              .with_color(color)
              .add_child(ui)
          })
          .add_child(ui);
      }
    })
    .add_root(&mut hui.hui, ren.size_vec2());
}
