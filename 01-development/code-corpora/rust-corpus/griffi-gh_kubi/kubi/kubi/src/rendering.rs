use shipyard::{AllStoragesViewMut, IntoIter, IntoWorkload, Unique, UniqueView, UniqueViewMut, View, Workload, WorkloadModificator};
use winit::dpi::PhysicalSize;
use glam::Vec3;
use crate::{events::WindowResizedEvent, hui_integration::kubi_ui_draw, state::is_ingame};

mod renderer;
mod primitives;
mod selection_box;
mod entities;
pub use renderer::Renderer;

pub mod background;
pub mod world;
pub mod camera_uniform;
pub mod depth;
pub mod smoverlay;

pub struct BufferPair {
  pub index: wgpu::Buffer,
  pub index_len: u32,
  pub vertex: wgpu::Buffer,
  pub vertex_len: u32,
}

#[derive(Unique)]
pub struct BackgroundColor(pub Vec3);

pub struct RenderCtx<'a> {
  //pub renderer: &'a Renderer,
  pub encoder: &'a mut wgpu::CommandEncoder,
  pub surface_view: &'a wgpu::TextureView,
}

//TODO run init_world_render_state, init_selection_box_state, etc. only once ingame?

pub fn init_rendering() -> Workload {
  (
    depth::init_depth_texture,
    camera_uniform::init_camera_uniform_buffer,
    primitives::init_primitives,
    world::init_world_render_state, //req: depth, camera
    entities::init_entities_render_state, //req: depth, camera
    selection_box::init_selection_box_render_state, //req: depth, camera, primitives
    smoverlay::init_smoverlay_render_state, //req: primitives
  ).into_sequential_workload()
}

pub fn update_rendering_early() -> Workload {
  (
    resize_renderer,
    depth::resize_depth_texture,
  ).into_sequential_workload()
}

pub fn update_rendering_late() -> Workload {
  (
    camera_uniform::update_camera_uniform_buffer,
    (
      selection_box::update_selection_box_render_state,
      entities::update_entities_render_state,
      smoverlay::update_smoverlay_render_state,
    ).into_workload().run_if(is_ingame),
  ).into_workload()
}

pub fn render_master(storages: AllStoragesViewMut) {
  let renderer = storages.borrow::<UniqueView<Renderer>>().unwrap();

  let mut encoder = renderer.device().create_command_encoder(&wgpu::CommandEncoderDescriptor {
    label: Some("main_encoder"),
  });
  let surface_texture = renderer.surface().get_current_texture().unwrap();
  let surface_view = surface_texture.texture.create_view(&wgpu::TextureViewDescriptor::default());

  let mut data = RenderCtx {
    encoder: &mut encoder,
    surface_view: &surface_view,
  };

  storages.run_with_data(background::clear_bg, &mut data);
  if storages.run(is_ingame) {
    storages.run_with_data(world::draw_world, &mut data);
    storages.run_with_data(selection_box::draw_selection_box, &mut data);
    storages.run_with_data(entities::render_entities, &mut data);
    storages.run_with_data(world::rpass_submit_trans_bundle, &mut data);
    storages.run_with_data(smoverlay::render_submerged_view, &mut data);
  }
  storages.run_with_data(kubi_ui_draw, &mut data);

  renderer.queue().submit([encoder.finish()]);
  surface_texture.present();
}

/// Resize the renderer when the window is resized
pub fn resize_renderer(
  mut renderer: UniqueViewMut<Renderer>,
  resize: View<WindowResizedEvent>,
) {
  if let Some(size) = resize.iter().last() {
    renderer.resize(PhysicalSize::new(size.0.x, size.0.y));
  }
}

// pub fn if_resized (resize: View<WindowResizedEvent>,) -> bool {
//   resize.len() > 0
// }
