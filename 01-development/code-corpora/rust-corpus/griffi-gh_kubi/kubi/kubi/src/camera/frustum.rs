// basically ported from c++
// - used as a reference:
//   [ https://github.com/Beastwick18/gltest/blob/main/src/renderer/Frustum.cpp ]
//   - original code: 
//     [ https://gist.github.com/podgorskiy/e698d18879588ada9014768e3e82a644 ]
//     - which uses cube vs frustum intersection code from:
//       [ http://iquilezles.org/www/articles/frustumcorrect/frustumcorrect.htm ]
// three layers of stolen code, yay!

use glam::{Vec3A, Vec4, Mat3A, vec3a, Vec3, vec4};
use shipyard::{ViewMut, IntoIter, View, track};
use crate::transform::Transform;
use super::Camera;

#[repr(usize)]
enum FrustumPlane {
  Left,
  Right,
  Bottom,
  Top,
  Near,
  Far,
}

const PLANE_COUNT: usize = 6;
const PLANE_COMBINATIONS: usize = PLANE_COUNT * (PLANE_COUNT - 1) / 2;
const POINT_COUNT: usize = 8;

#[derive(Default)]
pub struct Frustum {
  planes: [Vec4; PLANE_COUNT],
  points: [Vec3A; POINT_COUNT]
}
impl Frustum {
  pub fn compute(camera: &Camera) -> Self {
    //compute transposed view-projection matrix
    let mat = (camera.perspective_matrix * camera.view_matrix).transpose();

    // compute planes
    let mut planes = [Vec4::default(); PLANE_COUNT];
    planes[FrustumPlane::Left   as usize] = mat.w_axis + mat.x_axis;
    planes[FrustumPlane::Right  as usize] = mat.w_axis - mat.x_axis;
    planes[FrustumPlane::Bottom as usize] = mat.w_axis + mat.y_axis;
    planes[FrustumPlane::Top    as usize] = mat.w_axis - mat.y_axis;
    planes[FrustumPlane::Near   as usize] = mat.w_axis + mat.z_axis;
    planes[FrustumPlane::Far    as usize] = mat.w_axis - mat.z_axis;

    //compute crosses
    let crosses = [
      Vec3A::from_vec4(planes[FrustumPlane::Left as usize]).cross(Vec3A::from_vec4(planes[FrustumPlane::Right as usize])),
      Vec3A::from_vec4(planes[FrustumPlane::Left as usize]).cross(Vec3A::from_vec4(planes[FrustumPlane::Bottom as usize])),
      Vec3A::from_vec4(planes[FrustumPlane::Left as usize]).cross(Vec3A::from_vec4(planes[FrustumPlane::Top as usize])),
      Vec3A::from_vec4(planes[FrustumPlane::Left as usize]).cross(Vec3A::from_vec4(planes[FrustumPlane::Near as usize])),
      Vec3A::from_vec4(planes[FrustumPlane::Left as usize]).cross(Vec3A::from_vec4(planes[FrustumPlane::Far as usize])),
      Vec3A::from_vec4(planes[FrustumPlane::Right as usize]).cross(Vec3A::from_vec4(planes[FrustumPlane::Bottom as usize])),
      Vec3A::from_vec4(planes[FrustumPlane::Right as usize]).cross(Vec3A::from_vec4(planes[FrustumPlane::Top as usize])),
      Vec3A::from_vec4(planes[FrustumPlane::Right as usize]).cross(Vec3A::from_vec4(planes[FrustumPlane::Near as usize])),
      Vec3A::from_vec4(planes[FrustumPlane::Right as usize]).cross(Vec3A::from_vec4(planes[FrustumPlane::Far as usize])),
      Vec3A::from_vec4(planes[FrustumPlane::Bottom as usize]).cross(Vec3A::from_vec4(planes[FrustumPlane::Top as usize])),
      Vec3A::from_vec4(planes[FrustumPlane::Bottom as usize]).cross(Vec3A::from_vec4(planes[FrustumPlane::Near as usize])),
      Vec3A::from_vec4(planes[FrustumPlane::Bottom as usize]).cross(Vec3A::from_vec4(planes[FrustumPlane::Far as usize])),
      Vec3A::from_vec4(planes[FrustumPlane::Top as usize]).cross(Vec3A::from_vec4(planes[FrustumPlane::Near as usize])),
      Vec3A::from_vec4(planes[FrustumPlane::Top as usize]).cross(Vec3A::from_vec4(planes[FrustumPlane::Far as usize])),
      Vec3A::from_vec4(planes[FrustumPlane::Near as usize]).cross(Vec3A::from_vec4(planes[FrustumPlane::Far as usize])),
    ];

    //compute points
    let points = [
      intersection::<{FrustumPlane::Left as usize},  {FrustumPlane::Bottom as usize}, {FrustumPlane::Near as usize}>(&planes, &crosses),
      intersection::<{FrustumPlane::Left as usize},  {FrustumPlane::Top as usize},    {FrustumPlane::Near as usize}>(&planes, &crosses),
      intersection::<{FrustumPlane::Right as usize}, {FrustumPlane::Bottom as usize}, {FrustumPlane::Near as usize}>(&planes, &crosses),
      intersection::<{FrustumPlane::Right as usize}, {FrustumPlane::Top as usize},    {FrustumPlane::Near as usize}>(&planes, &crosses),
      intersection::<{FrustumPlane::Left as usize},  {FrustumPlane::Bottom as usize}, {FrustumPlane::Far as usize}>(&planes, &crosses),
      intersection::<{FrustumPlane::Left as usize},  {FrustumPlane::Top as usize},    {FrustumPlane::Far as usize}>(&planes, &crosses),
      intersection::<{FrustumPlane::Right as usize}, {FrustumPlane::Bottom as usize}, {FrustumPlane::Far as usize}>(&planes, &crosses),
      intersection::<{FrustumPlane::Right as usize}, {FrustumPlane::Top as usize},    {FrustumPlane::Far as usize}>(&planes, &crosses),
    ];

    Self { planes, points }
  }

  pub fn is_box_visible(&self, minp: Vec3, maxp: Vec3) -> bool {
    // check box outside/inside of frustum
    for plane in self.planes {
      if (plane.dot(vec4(minp.x, minp.y, minp.z, 1.)) < 0.) &&
         (plane.dot(vec4(maxp.x, minp.y, minp.z, 1.)) < 0.) &&
         (plane.dot(vec4(minp.x, maxp.y, minp.z, 1.)) < 0.) &&
         (plane.dot(vec4(maxp.x, maxp.y, minp.z, 1.)) < 0.) &&
         (plane.dot(vec4(minp.x, minp.y, maxp.z, 1.)) < 0.) &&
         (plane.dot(vec4(maxp.x, minp.y, maxp.z, 1.)) < 0.) &&
         (plane.dot(vec4(minp.x, maxp.y, maxp.z, 1.)) < 0.) &&
         (plane.dot(vec4(maxp.x, maxp.y, maxp.z, 1.)) < 0.)
      {
        return false
      }
    }

    // check frustum outside/inside box
    if self.points.iter().all(|point| point.x > maxp.x) { return false }
    if self.points.iter().all(|point| point.x < minp.x) { return false }
    if self.points.iter().all(|point| point.y > maxp.y) { return false }
    if self.points.iter().all(|point| point.y < minp.y) { return false }
    if self.points.iter().all(|point| point.z > maxp.z) { return false }
    if self.points.iter().all(|point| point.z < minp.z) { return false }
    
    true
  }
}

const fn ij2k<const I: usize, const J: usize>() -> usize {
  I * (9 - I) / 2 + J - 1 
}
fn intersection<const A: usize, const B: usize, const C: usize>(planes: &[Vec4; PLANE_COUNT], crosses: &[Vec3A; PLANE_COMBINATIONS]) -> Vec3A {
  let d = Vec3A::from_vec4(planes[A]).dot(crosses[ij2k::<B, C>()]);
  let res = Mat3A::from_cols(
    crosses[ij2k::<B, C>()], 
    -crosses[ij2k::<A, C>()], 
    crosses[ij2k::<A, B>()],
  ) * vec3a(planes[A].w, planes[B].w, planes[C].w);
  res * (-1. / d)
}

pub fn update_frustum(
  mut cameras: ViewMut<Camera>,
  transforms: View<Transform, track::All>
) {
  for (camera, _) in (&mut cameras, transforms.inserted_or_modified()).iter() {
    camera.frustum = Frustum::compute(camera);
  }
}
