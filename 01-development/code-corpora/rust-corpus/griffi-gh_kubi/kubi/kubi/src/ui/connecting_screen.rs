use hui::element::{text::Text, UiElementExt};
use kubi_shared::networking::state::ClientJoinState;
use shipyard::{IntoWorkload, NonSendSync, UniqueView, UniqueViewMut, Workload};
use crate::{
  hui_integration::UiState,
  loading_screen::loading_screen_base,
  networking::{ConnectionRejectionReason, ServerAddress},
  rendering::Renderer,
  state::{GameState, NextState}
};

fn render_connecting_ui(
  addr: UniqueView<ServerAddress>,
  rejection: Option<UniqueView<ConnectionRejectionReason>>,
  join_state: UniqueView<ClientJoinState>,
  mut ui: NonSendSync<UniqueViewMut<UiState>>,
  ren: UniqueView<Renderer>,
) {
  let text = match (rejection, *join_state) {
    (Some(err), _) => {
      format!("Connection rejected by {}\n\n{}", addr.0, err.reason)
    },
    (_, ClientJoinState::Disconnected) => {
      format!("Lost connection to {}", addr.0)
    },
    _ => {
      format!("Connecting to {}...", addr.0)
    },
  };

  loading_screen_base(1., |ui| {
    Text::new(text)
      .with_text_size(16)
      .add_child(ui);
  }).add_root(&mut ui.hui, ren.size_vec2())
}

fn switch_to_loading_if_connected(
  mut next_state: UniqueViewMut<NextState>,
  client_state: UniqueView<ClientJoinState>,
) {
  if *client_state == ClientJoinState::Joined {
    next_state.0 = Some(GameState::LoadingWorld);
  }
}

pub fn update_connecting_screen() -> Workload {
  (
    render_connecting_ui,
    switch_to_loading_if_connected,
  ).into_workload()
}
