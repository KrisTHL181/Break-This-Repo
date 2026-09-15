use shipyard::{Component, View, ViewMut, EntitiesViewMut, IntoIter, track};
use glam::{IVec3, Quat, Vec3};
use kubi_shared::block::Block;
use crate::{
  client_physics::ClPhysicsActor, player::MainPlayer, transform::Transform
};
use super::EventComponent;

#[derive(Component, Clone, Copy, Debug)]
pub enum PlayerActionEvent {
  PositionChanged {
    position: Vec3,
    //XXX: should this even be here?
    velocity: Vec3,
    direction: Quat
  },
  UpdatedBlock {
    position: IVec3,
    block: Block,
  },
}

pub fn generate_move_events(
  transforms: View<Transform, track::All>,
  player: View<MainPlayer>,
  actors: View<ClPhysicsActor>,
  mut entities: EntitiesViewMut,
  mut events: ViewMut<EventComponent>,
  mut actions: ViewMut<PlayerActionEvent>,
) {
  let Some((_, transform, actor)) = (&player, transforms.inserted_or_modified(), &actors).iter().next() else { return };
  let (_, direction, position) = transform.0.to_scale_rotation_translation();
  //HACK: if the actor is disabled, the velocity is irrelevant, so we just set it to zero.
  let velocity = if actor.disable { Vec3::ZERO } else { actor.velocity };
  entities.add_entity(
    (&mut events, &mut actions),
    (EventComponent, PlayerActionEvent::PositionChanged { position, velocity, direction })
  );
}
