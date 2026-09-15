use bytemuck::{Pod, Zeroable};
use kubi_shared::{entity::Entity, transform::Transform};
use renderer::Renderer;
use shipyard::{EntityId, IntoIter, IntoWithId, UniqueView, UniqueViewMut, View};

use crate::{camera::Camera, rendering::renderer};

use super::EntitiesRenderState;

#[derive(Clone, Copy, Pod, Zeroable)]
#[repr(C, packed)]
pub struct InstanceData {
  pub mat: [f32; 4 * 4],
}

impl InstanceData {
  pub const LAYOUT: wgpu::VertexBufferLayout<'static> = wgpu::VertexBufferLayout {
    array_stride: std::mem::size_of::<InstanceData>() as wgpu::BufferAddress,
    step_mode: wgpu::VertexStepMode::Instance,
    attributes: &wgpu::vertex_attr_array![
      3 => Float32x4,
      4 => Float32x4,
      5 => Float32x4,
      6 => Float32x4,
    ],
  };
}

pub struct InstanceBuffer {
  pub count: u32,
  pub buffer: wgpu::Buffer,
}

pub fn create_instance_buffer(
  renderer: UniqueView<Renderer>,
) -> InstanceBuffer {
  log::info!("entities: create_instance_buffer");
  let buffer = renderer.device().create_buffer(&wgpu::BufferDescriptor {
    label: Some("instance_buffer"),
    size: 255 * std::mem::size_of::<InstanceData>() as u64,
    usage: wgpu::BufferUsages::VERTEX | wgpu::BufferUsages::COPY_DST,
    mapped_at_creation: false,
  });
  InstanceBuffer { count: 0, buffer }
}

pub fn update_instance_buffer(
  renderer: UniqueView<Renderer>,
  mut state: UniqueViewMut<EntitiesRenderState>,
  entities: View<Entity>,
  transforms: View<Transform>,
  camera: View<Camera>,
) {
  //Get id of the camera entity (this assumes a single camera entity)
  let cam_id = (&camera)
    .iter().with_id().next()
    .map(|(x, _)| x)
    .unwrap_or(EntityId::dead());

  // Create a list of instance data for all entities except ones that have camera attached
  let mut instances = Vec::with_capacity(entities.len() - 1);
  for (id, (_, trans)) in (&entities, &transforms).iter().with_id() {
    if id == cam_id { continue }
    instances.push(InstanceData {
      mat: trans.0.to_cols_array(),
    });
  }

  state.instance_buffer.count = instances.len() as u32;

  if !instances.is_empty() {
    renderer.queue().write_buffer(
      &state.instance_buffer.buffer,
      0,
      bytemuck::cast_slice(&instances)
    );
  }
}
