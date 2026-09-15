use shipyard::{UniqueViewMut, UniqueView, View, IntoIter, ViewMut, EntitiesViewMut, Workload, IntoWorkload};
use winit::keyboard::KeyCode;
use kubi_shared::{
  block::Block,
  queue::QueuedBlock,
  player::PlayerHolding,
};
use crate::{
  player::MainPlayer,
  world::{
    raycast::{LookingAtBlock, RAYCAST_STEP},
    queue::BlockUpdateQueue
  },
  input::{Inputs, PrevInputs, RawKbmInputState},
  events::{
    EventComponent,
    player_actions::PlayerActionEvent
  },
};

const BLOCK_KEY_MAP: &[(KeyCode, Block)] = &[
  (KeyCode::Digit1, Block::Cobblestone),
  (KeyCode::Digit2, Block::Planks),
  (KeyCode::Digit3, Block::Dirt),
  (KeyCode::Digit4, Block::Grass),
  (KeyCode::Digit5, Block::Sand),
  (KeyCode::Digit6, Block::Stone),
  (KeyCode::Digit7, Block::Torch),
  (KeyCode::Digit8, Block::Leaf),
];

fn pick_block_with_number_keys(
  main_player: View<MainPlayer>,
  mut holding: ViewMut<PlayerHolding>,
  input: UniqueView<RawKbmInputState>,
) {
  let Some((_, holding)) = (&main_player, &mut holding).iter().next() else { return };
  for &(key, block) in BLOCK_KEY_MAP {
    if input.keyboard_state.contains(key as u32) {
      holding.0 = Some(block);
      return
    }
  }
}

fn block_placement_system(
  main_player: View<MainPlayer>,
  holding: View<PlayerHolding>,
  raycast: View<LookingAtBlock>,
  input: UniqueView<Inputs>,
  prev_input: UniqueView<PrevInputs>,
  mut block_event_queue: UniqueViewMut<BlockUpdateQueue>,
  mut entities: EntitiesViewMut,
  mut events: ViewMut<EventComponent>,
  mut player_events: ViewMut<PlayerActionEvent>,
) {
  let action_place = input.action_b && !prev_input.0.action_b;
  let action_break = input.action_a && !prev_input.0.action_a;
  if action_place ^ action_break {
    //get components
    let Some((_, ray, block)) = (&main_player, &raycast, &holding).iter().next() else { return };
    let Some(ray) = ray.0 else { return };
    //get coord and block type
    let (place_position, place_block) = if action_place {
      if block.0.is_none() { return }
      let position = (ray.position - ray.direction * (RAYCAST_STEP + 0.001)).floor().as_ivec3();
      (position, block.0.unwrap())
    } else {
      (ray.block_position, Block::Air)
    };
    //queue place
    block_event_queue.0.push(QueuedBlock {
      position: place_position,
      block_type: place_block,
      soft: place_block != Block::Air,
    });
    //send event
    entities.add_entity(
      (&mut events, &mut player_events), 
      (EventComponent, PlayerActionEvent::UpdatedBlock {
        position: place_position,
        block: place_block,
      })
    );
  }
}

pub fn update_block_placement() -> Workload {
  (
    pick_block_with_number_keys,
    block_placement_system
  ).into_sequential_workload()
}
