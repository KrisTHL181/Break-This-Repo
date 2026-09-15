use gilrs::{Gilrs, GamepadId, Button, Event, Axis};
use glam::{Vec2, DVec2, vec2, dvec2};
use winit::{
  keyboard::{KeyCode, PhysicalKey},
  event::{DeviceEvent, DeviceId, ElementState, TouchPhase}
};
use hashbrown::HashMap;
use tinyset::{SetU32, SetU64};
use nohash_hasher::BuildNoHashHasher;
use shipyard::{AllStoragesView, Unique, View, IntoIter, UniqueViewMut, Workload, IntoWorkload, UniqueView, NonSendSync};
use crate::{
  events::{InputDeviceEvent, TouchEvent},
  rendering::Renderer,
};

#[derive(Unique, Clone, Copy, Default, Debug)]
pub struct Inputs {
  pub movement: Vec2,
  pub look: Vec2,
  pub action_a: bool,
  pub action_b: bool,
  pub jump: bool,
}

#[derive(Unique, Clone, Copy, Default, Debug)]
pub struct PrevInputs(pub Inputs);

#[derive(Unique, Clone, Default, Debug)]
pub struct RawKbmInputState {
  pub keyboard_state: SetU32,
  pub button_state: [bool; 32],
  pub mouse_delta: DVec2
}

#[derive(Clone, Copy, Debug, Default)]
pub enum FingerCheck {
  #[default]
  Start,
  Current,
  StartOrCurrent,
  StartAndCurrent,
  NotMoved,
}

#[derive(Clone, Copy, Debug)]
pub struct Finger {
  pub id: u64,
  pub device_id: DeviceId,
  pub prev_position: DVec2,
  pub start_position: DVec2,
  pub current_position: DVec2,
  pub has_moved: bool,
}
impl Finger {
  pub fn within_area(&self, area_pos: DVec2, area_size: DVec2, check: FingerCheck) -> bool {
    let within_area = |pos: DVec2| -> bool {
      ((pos - area_pos).min_element() >= 0.) &&
      ((pos - (area_pos + area_size)).max_element() <= 0.)
    };
    let start = within_area(self.start_position);
    let current = within_area(self.current_position);
    match check {
      FingerCheck::Start => start,
      FingerCheck::Current => current,
      FingerCheck::StartOrCurrent => start || current,
      FingerCheck::StartAndCurrent => start && current,
      FingerCheck::NotMoved => current && !self.has_moved,
    }
  }
}

#[derive(Unique, Clone, Default, Debug)]
pub struct RawTouchState {
  //TODO: handle multiple touch devices somehow
  pub fingers: HashMap<u64, Finger, BuildNoHashHasher<u64>>
}

impl RawTouchState {
  pub fn query_area(&self, area_pos: DVec2, area_size: DVec2, check: FingerCheck) -> impl Iterator<Item = Finger> + '_ {
    self.fingers.iter().filter_map(move |(_, &finger)| {
      finger.within_area(area_pos, area_size, check).then_some(finger)
    })
  }
}

#[derive(Unique)]
pub struct GilrsWrapper(Option<Gilrs>);

#[derive(Unique, Default, Clone, Copy)]
pub struct ActiveGamepad(Option<GamepadId>);

//maybe we should manage gamepad state ourselves just like keyboard?
//at least for the sake of consitency

fn process_events(
  device_events: View<InputDeviceEvent>,
  mut input_state: UniqueViewMut<RawKbmInputState>,
) {
  input_state.mouse_delta = DVec2::ZERO;
  for event in device_events.iter() {
    match &event.event {
      DeviceEvent::MouseMotion { delta } => {
        input_state.mouse_delta = DVec2::from(*delta);
      },
      DeviceEvent::Key(input) => {
        if let PhysicalKey::Code(code) = input.physical_key {
          match input.state {
            ElementState::Pressed  => input_state.keyboard_state.insert(code as u32),
            ElementState::Released => input_state.keyboard_state.remove(code as u32),
          };
        }
      },
      DeviceEvent::Button { button, state } => {
        if *button < 32 {
          input_state.button_state[*button as usize] = matches!(*state, ElementState::Pressed);
        }
      },
      _ => ()
    }
  }
}

fn process_touch_events(
  touch_events: View<TouchEvent>,
  mut touch_state: UniqueViewMut<RawTouchState>,
) {
  for (_, finger) in &mut touch_state.fingers {
    finger.prev_position = finger.current_position;
  }
  for event in touch_events.iter() {
    let position = dvec2(event.0.location.x, event.0.location.y);
    match event.0.phase {
      TouchPhase::Started => {
        //println!("touch started: finger {}", event.0.id);
        touch_state.fingers.insert(event.0.id, Finger {
          id: event.0.id,
          device_id: event.0.device_id,
          start_position: position,
          current_position: position,
          prev_position: position,
          has_moved: false
        });
      },
      TouchPhase::Moved => {
        if let Some(finger) = touch_state.fingers.get_mut(&event.0.id) {
          finger.has_moved = true;
          finger.current_position = position;
        }
      },
      TouchPhase::Ended | TouchPhase::Cancelled => {
        //println!("touch ended: finger {}", event.0.id);
        touch_state.fingers.remove(&event.0.id);
      },
    }
  }
}

fn process_gilrs_events(
  mut gilrs: NonSendSync<UniqueViewMut<GilrsWrapper>>,
  mut active_gamepad: UniqueViewMut<ActiveGamepad>
) {
  if let Some(gilrs) = &mut gilrs.0 {
    while let Some(Event { id, event: _, time: _, .. }) = gilrs.next_event() {
      active_gamepad.0 = Some(id);
    }
  }
}

fn input_start(
  mut inputs: UniqueViewMut<Inputs>,
  mut prev_inputs: UniqueViewMut<PrevInputs>,
) {
  prev_inputs.0 = *inputs;
  *inputs = Inputs::default();
}

fn update_input_state (
  raw_inputs: UniqueView<RawKbmInputState>,
  mut inputs: UniqueViewMut<Inputs>,
) {
  inputs.movement += Vec2::new(
    raw_inputs.keyboard_state.contains(KeyCode::KeyD as u32) as u32 as f32 -
    raw_inputs.keyboard_state.contains(KeyCode::KeyA as u32) as u32 as f32,
    raw_inputs.keyboard_state.contains(KeyCode::KeyW as u32) as u32 as f32 -
    raw_inputs.keyboard_state.contains(KeyCode::KeyS as u32) as u32 as f32
  );
  inputs.look += raw_inputs.mouse_delta.as_vec2();
  inputs.action_a |= raw_inputs.button_state[0];
  inputs.action_b |= raw_inputs.button_state[1];
  inputs.jump |= raw_inputs.keyboard_state.contains(KeyCode::Space as u32);
}

fn update_input_state_gamepad (
  gilrs: NonSendSync<UniqueView<GilrsWrapper>>,
  active_gamepad: UniqueView<ActiveGamepad>,
  mut inputs: UniqueViewMut<Inputs>,
) {
  if let Some(gilrs) = &gilrs.0 {
    if let Some(gamepad) = active_gamepad.0.map(|id| gilrs.gamepad(id)) {
      let left_stick = vec2(gamepad.value(Axis::LeftStickX), gamepad.value(Axis::LeftStickY));
      let right_stick = vec2(gamepad.value(Axis::RightStickX), -gamepad.value(Axis::RightStickY));
      inputs.movement += left_stick;
      //HACK: for now, we multiply look by 2 to make it feel more responsive
      inputs.look += right_stick * 2.;
      inputs.action_a |= gamepad.is_pressed(Button::West);
      inputs.action_b |= gamepad.is_pressed(Button::East);
      inputs.jump |= gamepad.is_pressed(Button::South);
    }
  }
}

fn update_input_state_touch (
  touch_state: UniqueView<RawTouchState>,
  renderer: UniqueView<Renderer>,
  mut inputs: UniqueViewMut<Inputs>,
) {
  let w = renderer.size_uvec2().as_dvec2();

  //Movement
  if let Some(finger) = touch_state.query_area(
    dvec2(0., 0.),
    dvec2(w.x / 2., w.y),
    FingerCheck::Start
  ).next() {
    inputs.movement += (((finger.current_position - finger.start_position) / (w.x / 4.)) * dvec2(1., -1.)).as_vec2();
  }

  //Action buttons
  let action_button_fingers = {
    let mut action_button_fingers = SetU64::new();

    //Creates iterator of fingers that started within action button area
    let action_finger_iter = || touch_state.query_area(
      dvec2(w.x * 0.75, w.y * 0.666),
      dvec2(w.x * 0.25, w.y * 0.333),
      FingerCheck::Start
    );

    //Action button A
    inputs.action_a |= action_finger_iter().filter(|finger| finger.within_area(
      dvec2(w.x * (0.75 + 0.125), w.y * 0.666),
      dvec2(w.x * 0.125, w.y * 0.333),
      FingerCheck::StartOrCurrent
    )).map(|x| action_button_fingers.insert(x.id)).next().is_some();
    
    //Action button B
    inputs.action_b |= action_finger_iter().filter(|finger| finger.within_area(
      dvec2(w.x * 0.75, w.y * 0.666),
      dvec2(w.x * 0.125, w.y * 0.333),
      FingerCheck::StartOrCurrent
    )).map(|x| action_button_fingers.insert(x.id)).next().is_some();

    action_button_fingers
  };

  //Camera controls
  if let Some(finger) = touch_state.query_area(
    dvec2(w.x / 2., 0.),
    dvec2(w.x / 2., w.y),
    FingerCheck::Start
  ).find(|x| !action_button_fingers.contains(x.id)) {
    inputs.look += (((finger.current_position - finger.prev_position) / (w.x / 4.)) * 300.).as_vec2();
  }
}

fn input_end(
  mut inputs: UniqueViewMut<Inputs>,
) {
  if inputs.movement.length() >= 1. {
    inputs.movement = inputs.movement.normalize();
  }
}

pub fn init_input (
  storages: AllStoragesView
) {
  storages.add_unique_non_send_sync(GilrsWrapper(
    Gilrs::new().map_err(|x| {
      log::error!("Failed to initialize Gilrs");
      x
    }).ok()
  ));
  storages.add_unique(ActiveGamepad::default());
  storages.add_unique(Inputs::default());
  storages.add_unique(PrevInputs::default());
  storages.add_unique(RawKbmInputState::default());
  storages.add_unique(RawTouchState::default());
}

pub fn process_inputs() -> Workload {
  (
    process_events,
    process_touch_events,
    process_gilrs_events,
    input_start,
    update_input_state,
    update_input_state_touch,
    update_input_state_gamepad,
    input_end,
  ).into_sequential_workload()
}
