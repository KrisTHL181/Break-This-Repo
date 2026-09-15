use bytemuck::{Pod, Zeroable};
use kubi_shared::transform::Transform;
use shipyard::{IntoIter, UniqueView, UniqueViewMut, View};
use crate::{player::MainPlayer, rendering::Renderer, world::ChunkStorage};
use super::SmOverlayRenderState;

#[derive(Clone, Copy, Debug, PartialEq, Pod, Zeroable)]
#[repr(C, packed)]
pub struct SmUniformData {
  pub color: [f32; 4],
}

pub struct SmUniform {
  pub internal_do_render_flag: bool,
  stored_data: Option<SmUniformData>,
  pub buffer: wgpu::Buffer,
  pub bind_group_layout: wgpu::BindGroupLayout,
  pub bind_group: wgpu::BindGroup,
}

pub fn init_sm_uniform(
  renderer: UniqueView<Renderer>
) -> SmUniform {
  log::info!("init_sm_uniform");

  let buffer = renderer.device().create_buffer(&wgpu::BufferDescriptor {
    label: Some("smoverlay_uniform"),
    size: std::mem::size_of::<SmUniformData>() as u64,
    usage: wgpu::BufferUsages::UNIFORM | wgpu::BufferUsages::COPY_DST,
    mapped_at_creation: false,
  });

  let bind_group_layout = renderer.device().create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
    label: Some("smoverlay_bind_group_layout"),
    entries: &[wgpu::BindGroupLayoutEntry {
      binding: 0,
      visibility: wgpu::ShaderStages::FRAGMENT,
      ty: wgpu::BindingType::Buffer {
        ty: wgpu::BufferBindingType::Uniform,
        has_dynamic_offset: false,
        min_binding_size: None,
      },
      count: None,
    }],
  });

  let bind_group = renderer.device().create_bind_group(&wgpu::BindGroupDescriptor {
    label: Some("smoverlay_bind_group"),
    layout: &bind_group_layout,
    entries: &[wgpu::BindGroupEntry {
      binding: 0,
      resource: buffer.as_entire_binding(),
    }],
  });

  SmUniform {
    internal_do_render_flag: false,
    stored_data: None,
    buffer,
    bind_group_layout,
    bind_group,
  }
}

pub fn update_sm_uniform(
  mut state: UniqueViewMut<SmOverlayRenderState>,
  renderer: UniqueView<Renderer>,
  plr: View<MainPlayer>,
  trans: View<Transform>,
  world: UniqueView<ChunkStorage>,
) {
  state.uniform.internal_do_render_flag = false;

  let (_, plr_trans) = (&plr, &trans).iter().next().expect("Main player MIA");
  let plr_pos = plr_trans.0.to_scale_rotation_translation().2;
  let block_at_pos = world.get_block(plr_pos.floor().as_ivec3());
  let Some(block_at_pos) = block_at_pos  else { return };
  let Some(color) = block_at_pos.descriptor().submerge else { return };

  let new_data = SmUniformData {
    color: color.to_array()
  };

  state.uniform.internal_do_render_flag = true;
  if state.uniform.stored_data == Some(new_data) {
    return
  }
  state.uniform.stored_data = Some(new_data);

  log::debug!("update_sm_uniform: {:?}", new_data);

  renderer.queue().write_buffer(
    &state.uniform.buffer,
    0,
    bytemuck::cast_slice(&[new_data]),
  );
}
