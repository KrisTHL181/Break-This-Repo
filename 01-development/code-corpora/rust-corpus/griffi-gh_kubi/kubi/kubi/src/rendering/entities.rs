use shipyard::{AllStoragesView, Unique, UniqueView};
use crate::prefabs::GpuPrefabs;

use super::{camera_uniform::CameraUniformBuffer, depth::DepthTexture, RenderCtx};

mod instance;
mod pipeline;

#[derive(Unique)]
pub struct EntitiesRenderState {
  pub pipeline: wgpu::RenderPipeline,
  pub instance_buffer: instance::InstanceBuffer,
}

pub fn init_entities_render_state(storages: AllStoragesView) {
  storages.add_unique(EntitiesRenderState {
    pipeline: storages.run(pipeline::init_entities_pipeline),
    instance_buffer: storages.run(instance::create_instance_buffer),
  });
}

pub use instance::update_instance_buffer as update_entities_render_state;

// TODO: entity models
pub fn render_entities(
  ctx: &mut RenderCtx,
  state: UniqueView<EntitiesRenderState>,
  depth: UniqueView<DepthTexture>,
  prefabs: UniqueView<GpuPrefabs>,
  camera_ubo: UniqueView<CameraUniformBuffer>,
) {
  if state.instance_buffer.count == 0 {
    return
  }

  let mut rpass = ctx.encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
    label: Some("rpass_draw_entities"),
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
  rpass.set_bind_group(0, &prefabs.player_model_diffuse_bind_group, &[]);
  rpass.set_bind_group(1, &camera_ubo.camera_bind_group, &[]);
  rpass.set_vertex_buffer(0, prefabs.player_model.vertex.slice(..));
  rpass.set_vertex_buffer(1, state.instance_buffer.buffer.slice(..));
  rpass.set_index_buffer(prefabs.player_model.index.slice(..), wgpu::IndexFormat::Uint32);
  rpass.draw_indexed(0..prefabs.player_model.index_len, 0, 0..state.instance_buffer.count);
}
