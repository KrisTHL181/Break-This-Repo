use glam::{Vec3, IVec3};
use shipyard::{View, Component, ViewMut, IntoIter, UniqueView, track};
use kubi_shared::block::Block;
use crate::transform::Transform;
use super::ChunkStorage;

pub const RAYCAST_STEP: f32 = 0.25;

#[derive(Clone, Copy, Debug)]
pub struct RaycastReport {
  pub length: f32,
  pub position: Vec3,
  pub direction: Vec3,
  pub block_position: IVec3,
  pub block: Block,
}

impl ChunkStorage {
  //this is probably pretty slow...
  pub fn raycast(&self, origin: Vec3, direction: Vec3, limit: Option<f32>) -> Option<RaycastReport> {
    debug_assert!(direction.is_normalized(), "Ray direction not normalized");
    let mut position = origin;
    let mut length = 0.;
    loop {
      let block_position = position.floor().as_ivec3();
      if let Some(block) = self.get_block(block_position) {
        if block.descriptor().raycast_collision {
          return Some(RaycastReport { 
            length,
            position,
            direction,
            block_position,
            block
          });
        }
      }
      length += RAYCAST_STEP;
      position += direction * RAYCAST_STEP;
      if let Some(limit) = limit {
        if length > limit {
          return None;
        }
      }
    }
  }
}

#[derive(Component, Clone, Copy, Debug, Default)]
pub struct LookingAtBlock(pub Option<RaycastReport>);

pub fn update_raycasts(
  transform: View<Transform, track::All>,
  mut raycast: ViewMut<LookingAtBlock>,
  world: UniqueView<ChunkStorage>,
) {
  //idk if this check is even needed
  if !(world.is_inserted_or_modified() || (transform.inserted_or_modified(), &raycast).iter().next().is_some()) {
    return
  }
  for (transform, report) in (&transform, &mut raycast).iter() {
    let (_, rotation, position) = transform.0.to_scale_rotation_translation();
    let direction = (rotation.normalize() * Vec3::NEG_Z).normalize();
    *report = LookingAtBlock(world.raycast(position, direction, Some(30.)));
  }
}
