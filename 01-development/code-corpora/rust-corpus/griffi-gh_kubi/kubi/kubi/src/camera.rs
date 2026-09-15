use glam::{Mat4, Vec3};
use shipyard::{Component, Workload, IntoWorkload};
use std::f32::consts::PI;

mod matrices;
mod frustum;

use matrices::update_matrices;
use frustum::{Frustum, update_frustum};

#[derive(Component)]
pub struct Camera {
  pub view_matrix: Mat4,
  pub perspective_matrix: Mat4,
  pub frustum: Frustum,
  pub up: Vec3,
  pub fov: f32,
  pub z_near: f32,
  pub z_far: f32,
}
impl Camera {
  pub fn new(fov: f32, z_near: f32, z_far: f32, up: Vec3) -> Self {
    Self {
      fov, z_near, z_far, up,
      //TODO maybe separate this?
      perspective_matrix: Mat4::default(),
      view_matrix: Mat4::default(),
      frustum: Frustum::default(),
    }
  }
}
impl Default for Camera {
  fn default() -> Self {
    Self::new(PI / 3., 0.1, 1024., Vec3::Y)
  }
}

pub fn compute_cameras() -> Workload {
  (
    update_matrices,
    update_frustum,
  ).into_sequential_workload()
}
