use hui::{
  element::{
    container::Container,
    progress_bar::ProgressBar,
    text::Text,
    ElementList,
    UiElementExt,
  },
  layout::{Alignment, Direction},
  rect_frame, size,
};
use shipyard::{UniqueView, UniqueViewMut, Workload, NonSendSync, IntoWorkload};
use winit::keyboard::KeyCode;
use crate::{
  hui_integration::UiState,
  input::RawKbmInputState,
  networking::ServerAddress,
  rendering::Renderer,
  state::{GameState, NextState},
  world::ChunkStorage,
};

pub fn loading_screen_base(bg_alpha: f32, xui: impl FnOnce(&mut ElementList)) -> Container {
  Container::default()
    .with_size(size!(100%))
    .with_background((0.1, 0.1, 0.1, bg_alpha))
    .with_align(Alignment::Center)
    .with_children(|ui| {
      Container::default()
        .with_size(size!(400, auto))
        .with_background(rect_frame! {
          color: (0.2, 0.2, 0.2),
          corner_radius: 8.
        })
        .with_gap(5.)
        .with_padding(10.)
        .with_children(xui)
        .add_child(ui);
    })
}

fn render_loading_ui(
  addr: Option<UniqueView<ServerAddress>>,
  world: UniqueView<ChunkStorage>,
  mut ui: NonSendSync<UniqueViewMut<UiState>>,
  ren: UniqueView<Renderer>,
) {
  let loaded = world.chunks.iter().fold(0, |acc, (&_, chunk)| {
    acc + chunk.desired_state.matches_current(chunk.current_state) as usize
  });
  let total = world.chunks.len();
  let value = loaded as f32 / total as f32;
  let percentage = value * 100.;

  loading_screen_base(1. - (value - 0.75).max(0.), |ui| {
    Text::new(match addr {
        Some(addr) => format!("Connected to {}\nDownloading world data...", addr.0),
        _ => "Loading...".into(),
      })
      .with_text_size(16)
      .add_child(ui);

    ProgressBar::default()
      .with_value(value)
      .with_size(size!(100%, 15))
      .with_background(rect_frame! {
        color: (0.1, 0.1, 0.1),
        corner_radius: 2.
      })
      .with_foreground(rect_frame! {
        color: (0.4, 0.4, 1.0),
        corner_radius: 2.
      })
      .add_child(ui);

    Container::default()
      .with_size(size!(100%, auto))
      .with_align((Alignment::End, Alignment::Begin))
      .with_direction(Direction::Horizontal)
      .with_children(|ui| {
        Text::new(format!("{loaded}/{total} ({percentage:.1}%)"))
          .with_text_size(16)
          .add_child(ui)
      })
      .add_child(ui);
  }).add_root(&mut ui.hui, ren.size_vec2());
}

fn switch_to_ingame_if_loaded(
  world: UniqueView<ChunkStorage>,
  mut state: UniqueViewMut<NextState>
) {
  if world.chunks.is_empty() {
    return
  }
  if world.chunks.iter().all(|(_, chunk)| {
    chunk.desired_state.matches_current(chunk.current_state)
  }) {
    log::info!("Finished loading chunks");
    state.0 = Some(GameState::InGame);
  }
}

fn override_loading(
  kbm_state: UniqueView<RawKbmInputState>,
  mut state: UniqueViewMut<NextState>
) {
  if kbm_state.keyboard_state.contains(KeyCode::KeyF as u32) {
    state.0 = Some(GameState::InGame);
  }
}

pub fn update_loading_screen() -> Workload {
  (
    render_loading_ui,
    override_loading,
    switch_to_ingame_if_loaded,
  ).into_sequential_workload()
}
