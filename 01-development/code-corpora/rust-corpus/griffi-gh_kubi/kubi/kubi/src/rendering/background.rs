use shipyard::UniqueView;
use super::{BackgroundColor, RenderCtx};

pub fn clear_bg(
  ctx: &mut RenderCtx,
  bg: UniqueView<BackgroundColor>,
) {
  let _rpass = ctx.encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
    label: Some("clear_bg"),
    color_attachments: &[Some(wgpu::RenderPassColorAttachment {
      view: ctx.surface_view,
      resolve_target: None,
      ops: wgpu::Operations {
        load: wgpu::LoadOp::Clear(wgpu::Color {
          r: bg.0.x as f64,
          g: bg.0.y as f64,
          b: bg.0.z as f64,
          a: 1.0,
        }),
        store: wgpu::StoreOp::Store,
      },
    })],
    ..Default::default()
  });
}
