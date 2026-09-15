use shipyard::{UniqueView, UniqueViewMut, Unique, AllStoragesView};
use winit::keyboard::KeyCode;
use crate::input::RawKbmInputState;

#[derive(Unique)]
pub struct RequestExit(pub bool);

pub fn exit_on_esc(
  raw_inputs: UniqueView<RawKbmInputState>,
  mut exit: UniqueViewMut<RequestExit>
) {
  if raw_inputs.keyboard_state.contains(KeyCode::Escape as u32) {
    exit.0 = true;
  }
}

pub fn insert_control_flow_unique(
  storages: AllStoragesView
) {
  storages.add_unique(RequestExit(false))
}
