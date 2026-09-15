use shipyard::{Unique, UniqueView, UniqueViewMut, AllStoragesView};
use std::mem::take;

#[derive(Unique, PartialEq, Eq, Default, Clone, Copy)]
pub enum GameState {
  #[default]
  Initial,
  MainMenu,
  Connecting,
  LoadingWorld,
  InGame,
  ShuttingDown,
}

#[derive(Unique, PartialEq, Eq, Default, Clone, Copy)]
pub struct NextState(pub Option<GameState>);

pub fn init_state(
  all_storages: AllStoragesView,
) {
  all_storages.add_unique(GameState::default());
  all_storages.add_unique(NextState::default());
}

pub fn update_state(
  mut state: UniqueViewMut<GameState>,
  mut next: UniqueViewMut<NextState>,
) {
  *state = take(&mut next.0).unwrap_or(*state);
}

pub fn is_changing_state(
  state: UniqueView<NextState>
) -> bool {
  state.0.is_some()
}

pub fn is_main_menu(
  state: UniqueView<GameState>
) -> bool {
  *state == GameState::MainMenu
}

pub fn is_connecting(
  state: UniqueView<GameState>
) -> bool {
  *state == GameState::Connecting
}

pub fn is_ingame(
  state: UniqueView<GameState>
) -> bool {
  *state == GameState::InGame
}

pub fn is_loading(
  state: UniqueView<GameState>
) -> bool {
  matches!(*state, GameState::LoadingWorld)
}

pub fn is_shutting_down(
  state: UniqueView<GameState>
) -> bool {
  *state == GameState::ShuttingDown
}

pub fn is_ingame_or_loading(
  state: UniqueView<GameState>
) -> bool {
  matches!(*state, GameState::InGame | GameState::LoadingWorld)
}

pub fn is_ingame_or_loading_or_connecting_or_shutting_down(
  state: UniqueView<GameState>
) -> bool {
  matches!(*state, GameState::InGame | GameState::LoadingWorld | GameState::Connecting)
}

pub fn is_ingame_or_shutting_down(
  state: UniqueView<GameState>
) -> bool {
  matches!(*state, GameState::InGame | GameState::ShuttingDown)
}
