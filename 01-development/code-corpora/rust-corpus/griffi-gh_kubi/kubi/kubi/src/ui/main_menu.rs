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
use settings_overlay::{not_settings_ui_shown, settings_overlay_logic};
use shipyard::{AllStoragesView, AllStoragesViewMut, IntoWorkload, NonSendSync, SystemModificator, Unique, UniqueView, UniqueViewMut, Workload, WorkloadModificator};
use crate::{
  control_flow::RequestExit,
  hui_integration::UiState, networking::GameType, rendering::Renderer, state::{GameState, NextState}};


mod settings_overlay;

#[derive(Clone, Copy)]
enum MainMenuPage {
  TopMenu,
  Settings,
  // ListWorlds {
  //   list: Vec<String>,
  // },
}

#[derive(Unique)]
struct MainMenuState {
  page: MainMenuPage,
}

#[derive(Signal, Clone, Copy)]
enum MainMenuSignal {
  GotoPage(MainMenuPage),
  PlayOffline,
  PlayOnline,
  Quit,
  // CreatePlayWorld {
  //   name: String,
  // },
  // PlayWorld {
  //   path: PathBuf,
  // },
}

pub fn main_menu_leave(
  storages: AllStoragesView,
) {
  if storages.remove_unique::<MainMenuState>().is_err() {
    log::warn!("what the fuck? shouldn't matter tho")
  }
}

pub fn render_main_menu_ui(
  mut hui: NonSendSync<UniqueViewMut<UiState>>,
  ren: UniqueView<Renderer>,
) {
  Container::default()
    .with_size(size!(100%, 100%))
    .with_padding(30.)
    .with_gap(20.)
    .with_background(Corners::top_bottom(
      vec4(0.0, 0.0, 0.0, 0.85),
      vec4(0.0, 0.0, 0.0, 0.0))
    )
    .with_children(|ui| {
      Container::default()
        .with_size(size!(100%, auto))
        .with_align(Alignment::Center)
        .with_children(|ui| {
          Text::new("Kubi")
            .with_text_size(120)
            .add_child(ui);
        })
        .add_child(ui);
      Container::default()
        .with_size(size!(100%, 100%=))
        .with_align(Alignment::Center)
        .with_children(|ui| {
          Container::default()
            .with_align((Alignment::Center, Alignment::Begin))
            .with_padding(20.)
            .with_gap(10.)
            .with_background(rect_frame! {
              color: (0.1, 0.1, 0.1),
              corner_radius: 10.,
            })
            .with_children(|ui| {
              for (button_text, button_signal) in [
                ("Singleplayer", MainMenuSignal::PlayOffline),
                ("Multiplayer", MainMenuSignal::PlayOnline),
                ("Settings", MainMenuSignal::GotoPage(MainMenuPage::Settings)),
                ("Quit", MainMenuSignal::Quit),
              ] {
                Container::default()
                  .with_size(size!(300, 50))
                  .with_align(Alignment::Center)
                  .with_background(rect_frame! {
                    color: (0.2, 0.2, 0.2),
                    corner_radius: 3.,
                  })
                  .with_children(|ui| {
                    Text::new(button_text)
                      .with_text_size(24)
                      .add_child(ui);
                  })
                  .on_click(move || button_signal)
                  .add_child(ui)
              }
            })
            .add_child(ui);
        })
        .add_child(ui);
    })
    .add_root(&mut hui.hui, ren.size_vec2());
}

fn main_menu_process_signals(
  storages: AllStoragesViewMut,
) {
  let mut hui = storages.borrow::<NonSendSync<UniqueViewMut<UiState>>>().unwrap();
  let mut quit = storages.borrow::<UniqueViewMut<RequestExit>>().unwrap();
  hui.hui.process_signals(|signal| {
    match signal {
      MainMenuSignal::PlayOffline => {
        log::info!("play button pressed");
        // Open the local save file
        let save_file = open_local_save_file(Path::new("./world.kubi")).expect("failed to open save file");
        storages.add_unique(IOThreadManager::new(save_file));
        // Switch the state and kick off the world loading
        storages.add_unique(GameType::Singleplayer);
        storages.borrow::<UniqueViewMut<NextState>>().unwrap().0 = Some(GameState::LoadingWorld);
      }
      MainMenuSignal::PlayOnline => {

      },
      MainMenuSignal::GotoPage(page) => {
        log::info!("goto page button pressed");
        storages.add_unique(MainMenuState { page });
      }
      MainMenuSignal::Quit => {
        log::info!("quit button pressed");
        quit.0 = true;
      }
      _ => {}
    }
  });
}

pub fn update_main_menu() -> Workload {
  (
    render_main_menu_ui.run_if(not_settings_ui_shown),
    settings_overlay_logic,
    main_menu_process_signals,
  ).into_sequential_workload()
}