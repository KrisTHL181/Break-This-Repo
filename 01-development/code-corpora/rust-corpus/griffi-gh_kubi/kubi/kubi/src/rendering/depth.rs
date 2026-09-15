use glam::{uvec2, UVec2};
use shipyard::{AllStoragesView, Unique, UniqueView, UniqueViewMut};

use super::Renderer;

#[derive(Unique)]
pub struct DepthTexture {
  pub depth_texture: wgpu::Texture,
  pub depth_view: wgpu::TextureView,
  pub depth_sampler: wgpu::Sampler,
}

impl DepthTexture {
  fn desc(size: UVec2) -> wgpu::TextureDescriptor<'static> {
    wgpu::TextureDescriptor {
      label: Some("depth_texture"),
      size: wgpu::Extent3d {
        width: size.x,
        height: size.y,
        depth_or_array_layers: 1,
      },
      mip_level_count: 1,
      sample_count: 1,
      dimension: wgpu::TextureDimension::D2,
      format: wgpu::TextureFormat::Depth32Float,
      usage: wgpu::TextureUsages::RENDER_ATTACHMENT | wgpu::TextureUsages::TEXTURE_BINDING,
      view_formats: &[wgpu::TextureFormat::Depth32Float],
    }
  }

  pub fn init(renderer: &Renderer) -> Self {
    let size = uvec2(renderer.size().width, renderer.size().height);
    let depth_texture_desc = Self::desc(size);
    let depth_texture = renderer.device().create_texture(&depth_texture_desc);
    let depth_view = depth_texture.create_view(&wgpu::TextureViewDescriptor::default());
    let depth_sampler = renderer.device().create_sampler(&wgpu::SamplerDescriptor {
      label: Some("depth_sampler"),
      address_mode_u: wgpu::AddressMode::ClampToEdge,
      address_mode_v: wgpu::AddressMode::ClampToEdge,
      address_mode_w: wgpu::AddressMode::ClampToEdge,
      mag_filter: wgpu::FilterMode::Nearest,
      min_filter: wgpu::FilterMode::Nearest,
      mipmap_filter: wgpu::FilterMode::Nearest,
      compare: Some(wgpu::CompareFunction::LessEqual),
      ..Default::default()
    });
    Self { depth_texture, depth_view, depth_sampler }
  }

  pub fn resize(&mut self, renderer: &Renderer) {
    let old_size = uvec2(self.depth_texture.size().width, self.depth_texture.size().height);
    let new_size = uvec2(renderer.size().width, renderer.size().height);
    if old_size == new_size { return }
    let depth_texture_desc = Self::desc(new_size);
    self.depth_texture = renderer.device().create_texture(&depth_texture_desc);
    self.depth_view = self.depth_texture.create_view(&wgpu::TextureViewDescriptor::default());
  }
}

pub fn init_depth_texture(
  storages: AllStoragesView,
) {
  let renderer = storages.borrow::<UniqueView<Renderer>>().unwrap();
  storages.add_unique(DepthTexture::init(&renderer));
}

pub fn resize_depth_texture(
  mut depth_texture: UniqueViewMut<DepthTexture>,
  renderer: UniqueView<Renderer>,
) {
  depth_texture.resize(&renderer);
}
