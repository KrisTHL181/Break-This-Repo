use shipyard::{AllStoragesView, Unique, UniqueView};
use super::{primitives::FstriPrimitive, RenderCtx};

mod uniform;
mod pipeline;
use uniform::SmUniform;

#[derive(Unique)]
pub struct SmOverlayRenderState {
  pub uniform: SmUniform,
  pub pipeline: wgpu::RenderPipeline,
}

pub fn init_smoverlay_render_state(storages: AllStoragesView) {
  let uniform = storages.run(uniform::init_sm_uniform);
  let pipeline = storages.run_with_data(pipeline::init_smoverlay_pipeline, &uniform);
  storages.add_unique(SmOverlayRenderState { uniform, pipeline });
}

pub use uniform::update_sm_uniform as update_smoverlay_render_state;

pub fn render_submerged_view(
  ctx: &mut RenderCtx,
  state: UniqueView<SmOverlayRenderState>,
  buf: UniqueView<FstriPrimitive>,
) {
  if !state.uniform.internal_do_render_flag { return }

  let mut rpass = ctx.encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
    label: Some("smoverlay_render_pass"),
    color_attachments: &[Some(wgpu::RenderPassColorAttachment {
      view: ctx.surface_view,
      resolve_target: None,
      ops: wgpu::Operations {
        load: wgpu::LoadOp::Load,
        store: wgpu::StoreOp::Store,
      },
    })],
    depth_stencil_attachment: None,
    timestamp_writes: None,
    occlusion_query_set: None,
  });

  rpass.set_pipeline(&state.pipeline);
  rpass.set_bind_group(0, &state.uniform.bind_group, &[]);
  rpass.set_vertex_buffer(0, buf.0.slice(..));
  rpass.draw(0..3, 0..1);
}
