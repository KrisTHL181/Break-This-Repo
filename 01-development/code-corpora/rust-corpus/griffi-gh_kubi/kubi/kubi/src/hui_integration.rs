use hui::UiInstance;
use hui_wgpu::WgpuUiRenderer;
//use hui_glium::GliumUiRenderer;
use shipyard::{AllStoragesView, Unique, UniqueView, NonSendSync, UniqueViewMut};
use crate::rendering::{RenderCtx, Renderer};

#[derive(Unique)]
pub struct UiState {
  pub hui: UiInstance,
  pub renderer: WgpuUiRenderer,
}

pub fn kubi_ui_init(
  storages: AllStoragesView
) {
  let renderer = storages.borrow::<UniqueView<Renderer>>().unwrap();
  storages.add_unique_non_send_sync(UiState {
    hui: UiInstance::new(),
    renderer: WgpuUiRenderer::new(renderer.device(), renderer.surface_config().format),
  });
}

pub fn kubi_ui_begin(
  mut ui: NonSendSync<UniqueViewMut<UiState>>
) {
  ui.hui.begin();
}

pub fn kubi_ui_end(
  mut ui: NonSendSync<UniqueViewMut<UiState>>,
  renderer: UniqueView<Renderer>,
) {
  let ui: &mut UiState = &mut ui;
  let UiState { hui, renderer: ui_renderer } = ui;
  hui.end();
  ui_renderer.update(hui, renderer.queue(), renderer.device(), renderer.size_vec2());
}

pub fn kubi_ui_draw(
  ctx: &mut RenderCtx,
  ui: NonSendSync<UniqueView<UiState>>,
) {
  ui.renderer.draw(ctx.encoder, ctx.surface_view);
}

pub fn hui_process_winit_events(
  event: &winit::event::Event<()>,
  mut ui: NonSendSync<UniqueViewMut<UiState>>,
) {
  hui_winit::handle_winit_event(&mut ui.hui, event);
}
