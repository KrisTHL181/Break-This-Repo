//TODO client-side physics
//TODO move this to shared
use glam::{vec3, Mat4, Vec3, Vec3Swizzles};
use shipyard::{track, AllStoragesView, Component, IntoIter, Unique, UniqueView, ViewMut};
use kubi_shared::{block::{Block, CollisionType}, transform::Transform};
use crate::{delta_time::DeltaTime, world::ChunkStorage};

#[derive(Unique)]
pub struct GlobalClPhysicsConfig {
  pub gravity: Vec3,
  ///XXX: currenly unused:
  pub iterations: usize,
}

impl Default for GlobalClPhysicsConfig {
  fn default() -> Self {
    Self {
      gravity: Vec3::new(0., -9.8, 0.),
      iterations: 10,
    }
  }
}

//TODO: actors should be represented by a vertical line, not a point.
//XXX: maybe a capsule? (or configurable hull?)
//TODO: per block friction

#[derive(Component)]
pub struct ClPhysicsActor {
  pub disable: bool,
  pub offset: Vec3,
  pub forces: Vec3,
  pub frame_velocity: Vec3,
  pub velocity: Vec3,
  pub decel: Vec3,
  pub gravity_scale: f32,
  pub max_velocity: (Option<f32>, Option<f32>, Option<f32>),
  pub hack_xz_circular: bool,
  flag_ground: bool,
  flag_collision: bool,
}

impl ClPhysicsActor {
  pub fn apply_force(&mut self, force: Vec3) {
    self.forces += force;
  }

  pub fn add_frame_velocity(&mut self, force: Vec3) {
    self.frame_velocity += force;
  }

  pub fn on_ground(&self) -> bool {
    self.flag_ground
  }
}

impl Default for ClPhysicsActor {
  fn default() -> Self {
    Self {
      //HACK: for player
      disable: false,
      offset: vec3(0., 1.5, 0.),
      forces: Vec3::ZERO,
      frame_velocity: Vec3::ZERO,
      velocity: Vec3::ZERO,
      //constant deceleration, in ratio per second. e.g. value of 1 should stop the actor in 1 second.
      decel: vec3(1., 0., 1.),
      gravity_scale: 1.,
      max_velocity: (Some(20.), None, Some(20.)),
      hack_xz_circular: true,
      flag_ground: false,
      flag_collision: false,
    }
  }
}

trait BlockCollisionExt {
  fn collision_type(&self) -> CollisionType;
  fn is_solid(&self) -> bool {
    self.collision_type() == CollisionType::Solid
  }
}

impl BlockCollisionExt for Option<Block> {
  fn collision_type(&self) -> CollisionType {
    self.unwrap_or(Block::Air).descriptor().collision
  }
}

impl BlockCollisionExt for Block {
  fn collision_type(&self) -> CollisionType {
    self.descriptor().collision
  }
}

pub fn init_client_physics(
  storages: AllStoragesView,
) {
  storages.add_unique(GlobalClPhysicsConfig::default());
}

pub fn update_client_physics_late(
  mut actors: ViewMut<ClPhysicsActor>,
  mut transforms: ViewMut<Transform, track::All>,
  conf: UniqueView<GlobalClPhysicsConfig>,
  world: UniqueView<ChunkStorage>,
  dt: UniqueView<DeltaTime>,
) {
  for (actor, mut transform) in (&mut actors, &mut transforms).iter() {
    if actor.disable {
      actor.forces = Vec3::ZERO;
      continue;
    }

    //apply forces
    let actor_forces = actor.forces;
    actor.velocity += (actor_forces + conf.gravity) * dt.0.as_secs_f32();
    actor.forces = Vec3::ZERO;

    //get position
    let (scale, rotation, mut actor_position) = transform.0.to_scale_rotation_translation();
    actor_position -= actor.offset;

    //get grid-aligned pos and blocks
    let actor_block_pos = actor_position.floor().as_ivec3();
    let actor_block = world.get_block(actor_block_pos);
    let actor_block_pos_slightly_below = (actor_position + Vec3::NEG_Y * 0.01).floor().as_ivec3();
    let actor_block_below = world.get_block(actor_block_pos_slightly_below);

    //update flags
    actor.flag_collision = actor_block.is_solid();
    actor.flag_ground = actor.flag_collision || actor_block_below.is_solid();

    //push actor back out of the block
    if actor.flag_collision {
      //first, compute restitution, based on position inside the block
      // let block_center = actor_block_pos.as_f32() + Vec3::ONE * 0.5;
      // let to_block_center = actor_position - block_center;

      //then, based on normal:
      //push the actor back
      //actor_position += normal * 0.5;
      //cancel out velocity in the direction of the normal
      // let dot = actor.velocity.dot(normal);
      // if dot > 0. {
      //   //actor.velocity -= normal * dot;
      //   actor.velocity = Vec3::ZERO;
      // }

      //HACK: for now, just stop the vertical velocity if on ground altogether,
      //as we don't have proper collision velocity resolution yet (we need to compute dot product or sth)
      if actor.flag_ground {
        actor.velocity.y = actor.velocity.y.max(0.);
      }
    }

    //clamp velocity
    let max_velocity = actor.max_velocity;
    if actor.hack_xz_circular && actor.max_velocity.0.is_some() && (actor.max_velocity.0 == actor.max_velocity.2) {
      actor.velocity.y = actor.velocity.y.clamp(-max_velocity.1.unwrap_or(f32::MAX), max_velocity.1.unwrap_or(f32::MAX));
      let clamped = actor.velocity.xz().clamp_length_max(actor.max_velocity.0.unwrap_or(f32::MAX));
      actor.velocity.x = clamped.x;
      actor.velocity.z = clamped.y;
    } else {
      actor.velocity = vec3(
        actor.velocity.x.clamp(-max_velocity.0.unwrap_or(f32::MAX), max_velocity.0.unwrap_or(f32::MAX)),
        actor.velocity.y.clamp(-max_velocity.1.unwrap_or(f32::MAX), max_velocity.1.unwrap_or(f32::MAX)),
        actor.velocity.z.clamp(-max_velocity.2.unwrap_or(f32::MAX), max_velocity.2.unwrap_or(f32::MAX)),
      );
    }

    //Apply velocity
    actor_position += (actor.velocity + actor.frame_velocity) * dt.0.as_secs_f32();
    actor.frame_velocity = Vec3::ZERO;
    actor_position += actor.offset;
    transform.0 = Mat4::from_scale_rotation_translation(scale, rotation.normalize(), actor_position);

    //Apply "friction"
    let actor_velocity = actor.velocity;
    let actor_decel = actor.decel;
    actor.velocity -= actor_velocity * actor_decel * dt.0.as_secs_f32();
  }
  // for (_, mut transform) in (&controllers, &mut transforms).iter() {
  //   let (scale, rotation, mut translation) = transform.0.to_scale_rotation_translation();
  //   translation.y -= dt.0.as_secs_f32() * 100.;
  //   transform.0 = Mat4::from_scale_rotation_translation(scale, rotation, translation);
  // }
}
