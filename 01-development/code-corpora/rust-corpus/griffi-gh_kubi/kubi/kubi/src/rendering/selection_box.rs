use shipyard::{AllStoragesView, IntoIter, Unique, UniqueView, View};
use wgpu::RenderPassDescriptor;
use crate::{player::MainPlayer, world::raycast::LookingAtBlock};
use super::{camera_uniform::CameraUniformBuffer, depth::DepthTexture, primitives::CubePrimitive, RenderCtx};

mod pipeline;
mod uniform;

use uniform::SelectionBoxUniform;

#[derive(Unique)]
pub struct SboxRenderState {
  pipeline: wgpu::RenderPipeline,
  uniform: SelectionBoxUniform,
}

pub fn init_selection_box_render_state(storages: AllStoragesView) {
  let uniform = storages.run(uniform::init_selection_box_uniform);
  let pipeline = storages.run_with_data(pipeline::init_selection_box_pipeline, &uniform);
  storages.add_unique(SboxRenderState { pipeline, uniform });
}

pub use uniform::update_selection_box_uniform
  as update_selection_box_render_state;

pub fn draw_selection_box(
  ctx: &mut RenderCtx,
  state: UniqueView<SboxRenderState>,
  depth: UniqueView<DepthTexture>,
  cube: UniqueView<CubePrimitive>,
  camera_ubo: UniqueView<CameraUniformBuffer>,
  lookat: View<LookingAtBlock>,
  player: View<MainPlayer>,
) {
  let Some((LookingAtBlock(Some(_)), _)) = (&lookat, &player).iter().next() else {
    return
  };
  let mut rpass = ctx.encoder.begin_render_pass(&RenderPassDescriptor {
    label: Some("rpass_selection_box"),
    color_attachments: &[Some(wgpu::RenderPassColorAttachment {
      view: ctx.surface_view,
      resolve_target: None,
      ops: wgpu::Operations {
        load: wgpu::LoadOp::Load,
        store: wgpu::StoreOp::Store,
      },
    })],
    depth_stencil_attachment: Some(wgpu::RenderPassDepthStencilAttachment {
      view: &depth.depth_view,
      depth_ops: Some(wgpu::Operations {
        load: wgpu::LoadOp::Load,
        store: wgpu::StoreOp::Store,
      }),
      stencil_ops: None,
    }),
    ..Default::default()
  });

  rpass.set_pipeline(&state.pipeline);
  rpass.set_bind_group(0, &camera_ubo.camera_bind_group, &[]);
  rpass.set_bind_group(1, &state.uniform.bind_group, &[]);
  rpass.set_index_buffer(cube.0.index.slice(..), wgpu::IndexFormat::Uint16);
  rpass.set_vertex_buffer(0, cube.0.vertex.slice(..));
  rpass.draw_indexed(0..cube.0.index_len, 0, 0..1);
}
