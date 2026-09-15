use std::path::Path;

use glam::vec4;
use hui::{
  element::{
    container::Container,
    interactable::ElementInteractableExt,
    text::Text,
    UiElementExt
  },
  layout::Alignment,
  rect::Corners,
  signal::Signal,
  rect_frame,
  size,
};
use kubi_shared::data::{io_thread::IOThreadManager, open_local_save_file};
use shipyard::{AllStoragesView, AllStoragesViewMut, IntoWorkload, NonSendSync, SystemModificator, Unique, UniqueView, UniqueViewMut, Workload, WorkloadModificator};
use crate::{
  control_flow::RequestExit, hui_integration::UiState, main_menu::MainMenuPage, networking::GameType, rendering::Renderer, state::{GameState, NextState}};

use super::{super::settings_ui::render_settings_ui2, MainMenuSignal, MainMenuState};

pub fn settings_ui_shown(mms: Option<UniqueView<MainMenuState>>) -> bool {
  let Some(mms) = mms else { return false };
  matches!(mms.page, MainMenuPage::Settings)
}

#[allow(clippy::just_underscores_and_digits)]
pub fn not_settings_ui_shown(_0: Option<UniqueView<MainMenuState>>) -> bool {
  !settings_ui_shown(_0)
}

// HACK: shows the back button over the settings UI
fn show_settings_back_button(
  mut hui: NonSendSync<UniqueViewMut<UiState>>,
  ren: UniqueView<Renderer>,
) {
  Container::default()
    .with_size(size!(100%, 100%))
    .with_align((Alignment::Center, Alignment::End))
    .with_padding(30.)
    .with_children(|ui| {
      Container::default()
        .with_background(rect_frame! {
          color: (0.1, 0.1, 0.1),
          corner_radius: 10.,
        })
        .with_padding((30., 5.))
        .with_size(size!(auto, 50))
        .with_align(Alignment::Center)
        .with_children(|ui| {
          Text::new("Back to the Main Menu")
            .with_text_size(24)
            .add_child(ui);
        })
        .on_click(|| MainMenuSignal::GotoPage(MainMenuPage::TopMenu))
        .add_child(ui);
    })
    .add_root(&mut hui.hui, ren.size_vec2());
}

pub fn settings_overlay_logic() -> Workload {
  (
    render_settings_ui2,
    show_settings_back_button,
  ).into_sequential_workload().run_if(settings_ui_shown)
}