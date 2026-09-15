use glam::Vec3;
use shipyard::{IntoIter, UniqueView, View};
use bytemuck::{Pod, Zeroable};
use crate::{
  player::MainPlayer,
  rendering::Renderer,
  world::raycast::LookingAtBlock,
};
use super::SboxRenderState;

#[derive(Clone, Copy, Pod, Zeroable)]
#[repr(C, packed)]
pub struct SelectionBoxUniformData {
  pub position: [f32; 3],
  pub _padding: u32,
}

pub struct SelectionBoxUniform {
  pub buffer: wgpu::Buffer,
  pub bind_group_layout: wgpu::BindGroupLayout,
  pub bind_group: wgpu::BindGroup,
}

pub fn init_selection_box_uniform(
  renderer: UniqueView<Renderer>
) -> SelectionBoxUniform {
  log::info!("init_selection_box_uniform");

  let buffer = renderer.device().create_buffer(&wgpu::BufferDescriptor {
    label: Some("selection_box_uniform"),
    size: std::mem::size_of::<SelectionBoxUniformData>() as u64,
    usage: wgpu::BufferUsages::UNIFORM | wgpu::BufferUsages::COPY_DST,
    mapped_at_creation: false,
  });

  let bind_group_layout = renderer.device().create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
    label: Some("selection_box_bind_group_layout"),
    entries: &[wgpu::BindGroupLayoutEntry {
      binding: 0,
      visibility: wgpu::ShaderStages::VERTEX,
      ty: wgpu::BindingType::Buffer {
        ty: wgpu::BufferBindingType::Uniform,
        has_dynamic_offset: false,
        min_binding_size: None,
      },
      count: None,
    }],
  });

  let bind_group = renderer.device().create_bind_group(&wgpu::BindGroupDescriptor {
    label: Some("selection_box_bind_group"),
    layout: &bind_group_layout,
    entries: &[wgpu::BindGroupEntry {
      binding: 0,
      resource: buffer.as_entire_binding(),
    }],
  });

  SelectionBoxUniform {
    buffer,
    bind_group_layout,
    bind_group,
  }
}

pub fn update_selection_box_uniform(
  renderer: UniqueView<Renderer>,
  state: UniqueView<SboxRenderState>,
  lookat: View<LookingAtBlock>,
  player: View<MainPlayer>,
) {
  //TODO: only update if changed
  if let Some((LookingAtBlock(Some(lookat)), _)) = (&lookat, &player).iter().next() {
    renderer.queue().write_buffer(
      &state.uniform.buffer,
      0,
      bytemuck::cast_slice(&[SelectionBoxUniformData {
        position: (lookat.position.floor() + Vec3::splat(0.5)).to_array(),
        _padding: 0,
      }]),
    );
  };
}
