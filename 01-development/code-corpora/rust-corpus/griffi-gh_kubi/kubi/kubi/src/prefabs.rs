use std::{io::{BufReader, Read}, path::{Path, PathBuf}};
use bytemuck::{Pod, Zeroable};
use hui::text::FontHandle;
use shipyard::{AllStoragesView, NonSendSync, Unique, UniqueView, UniqueViewMut};
use kubi_shared::block::BlockTexture;
use crate::{filesystem::AssetManager, hui_integration::UiState, rendering::{BufferPair, Renderer}};

//TODO move to rendering module

mod loader;
use loader::{load_texture2darray_prefab, load_texture2d_prefab, load_obj_prefab};

#[derive(Clone, Copy, Default, Pod, Zeroable)]
#[repr(C, packed)]
pub struct ModelVertex {
  pub tex_coords: [f32; 2],
  pub position: [f32; 3],
  pub _padding: u32,
  pub normal: [f32; 3],
}

impl ModelVertex {
  pub const LAYOUT: wgpu::VertexBufferLayout<'static> = wgpu::VertexBufferLayout {
    array_stride: std::mem::size_of::<ModelVertex>() as wgpu::BufferAddress,
    step_mode: wgpu::VertexStepMode::Vertex,
    attributes: &wgpu::vertex_attr_array![
      0 => Float32x2,
      1 => Float32x3,
      2 => Float32x3,
    ],
  };
}

pub trait AssetPaths {
  fn file_name(self) -> &'static str;
}

impl AssetPaths for BlockTexture {
  fn file_name(self) -> &'static str {
    match self {
      Self::Stone         => "stone.png",
      Self::Dirt          => "dirt.png",
      Self::GrassTop      => "grass_top.png",
      Self::GrassSide     => "grass_side.png",
      Self::Sand          => "sand.png",
      Self::Bedrock       => "bedrock.png",
      Self::Wood          => "wood.png",
      Self::WoodTop       => "wood_top.png",
      Self::Leaf          => "leaf.png",
      Self::Torch         => "torch.png",
      Self::TallGrass     => "tall_grass.png",
      Self::Snow          => "snow.png",
      Self::GrassSideSnow => "grass_side_snow.png",
      Self::Cobblestone   => "cobblestone.png",
      Self::Planks        => "planks.png",
      Self::WaterSolid    => "solid_water.png",
      Self::Water         => "water.png",
    }
  }
}

#[derive(Unique)]
pub struct GpuPrefabs {
  pub block_diffuse_texture: wgpu::Texture,
  pub block_diffuse_bind_group_layout: wgpu::BindGroupLayout,
  pub block_diffuse_bind_group: wgpu::BindGroup,
  pub player_model_diffuse_texture: wgpu::Texture,
  pub player_model_diffuse_bind_group_layout: wgpu::BindGroupLayout,
  pub player_model_diffuse_bind_group: wgpu::BindGroup,
  pub player_model: BufferPair,
}

#[derive(Unique)]
#[repr(transparent)]
pub struct UiFontPrefab(pub FontHandle);

pub fn load_prefabs(
  storages: AllStoragesView,
  renderer: UniqueView<Renderer>,
  mut ui: NonSendSync<UniqueViewMut<UiState>>,
  assman: UniqueView<AssetManager>
) {
  log::info!("Loading textures...");
  let block_diffuse_texture = load_texture2darray_prefab::<BlockTexture>(
    &renderer,
    &assman,
    "blocks".into(),
  );

  log::info!("Creating bing groups");
  let block_diffuse_view = block_diffuse_texture.create_view(&wgpu::TextureViewDescriptor {
    label: Some("block_texture_view"),
    ..Default::default()
  });
  let block_diffuse_sampler = renderer.device().create_sampler(&wgpu::SamplerDescriptor {
    label: Some("block_diffuse_sampler"),
    address_mode_u: wgpu::AddressMode::ClampToEdge,
    address_mode_v: wgpu::AddressMode::ClampToEdge,
    address_mode_w: wgpu::AddressMode::ClampToEdge,
    mag_filter: wgpu::FilterMode::Nearest,
    min_filter: wgpu::FilterMode::Linear,
    mipmap_filter: wgpu::FilterMode::Nearest,
    ..Default::default()
  });
  let block_diffuse_bind_group_layout = renderer.device()
    .create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
      label: Some("block_diffuse_bind_group_layout"),
      entries: &[
        wgpu::BindGroupLayoutEntry {
          binding: 0,
          visibility: wgpu::ShaderStages::FRAGMENT,
          ty: wgpu::BindingType::Texture {
            sample_type: wgpu::TextureSampleType::Float { filterable: true },
            view_dimension: wgpu::TextureViewDimension::D2Array,
            multisampled: false,
          },
          count: None,
        },
        wgpu::BindGroupLayoutEntry {
          binding: 1,
          visibility: wgpu::ShaderStages::FRAGMENT,
          ty: wgpu::BindingType::Sampler(wgpu::SamplerBindingType::Filtering),
          count: None,
        }
      ]
    });
  let block_diffuse_bind_group = renderer.device().create_bind_group(&wgpu::BindGroupDescriptor {
    label: Some("block_diffuse_bind_group"),
    layout: &block_diffuse_bind_group_layout,
    entries: &[
      wgpu::BindGroupEntry {
        binding: 0,
        resource: wgpu::BindingResource::TextureView(&block_diffuse_view),
      },
      wgpu::BindGroupEntry {
        binding: 1,
        resource: wgpu::BindingResource::Sampler(&block_diffuse_sampler),
      }
    ]
  });

  let player_model_diffuse_texture = load_texture2d_prefab(&renderer, &assman, &PathBuf::from("playermodel1.png"));
  let player_model_diffuse_view = player_model_diffuse_texture.create_view(&wgpu::TextureViewDescriptor {
    label: Some("player_model_texture_view"),
    ..Default::default()
  });
  let player_model_diffuse_sampler = renderer.device().create_sampler(&wgpu::SamplerDescriptor {
    label: Some("player_model_sampler"),
    address_mode_u: wgpu::AddressMode::ClampToEdge,
    address_mode_v: wgpu::AddressMode::ClampToEdge,
    address_mode_w: wgpu::AddressMode::ClampToEdge,
    mag_filter: wgpu::FilterMode::Linear,
    min_filter: wgpu::FilterMode::Linear,
    mipmap_filter: wgpu::FilterMode::Nearest,
    ..Default::default()
  });
  let player_model_diffuse_bind_group_layout = renderer.device()
    .create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
      label: Some("player_model_bind_group_layout"),
      entries: &[
        wgpu::BindGroupLayoutEntry {
          binding: 0,
          visibility: wgpu::ShaderStages::FRAGMENT,
          ty: wgpu::BindingType::Texture {
            sample_type: wgpu::TextureSampleType::Float { filterable: true },
            view_dimension: wgpu::TextureViewDimension::D2,
            multisampled: false,
          },
          count: None,
        },
        wgpu::BindGroupLayoutEntry {
          binding: 1,
          visibility: wgpu::ShaderStages::FRAGMENT,
          ty: wgpu::BindingType::Sampler(wgpu::SamplerBindingType::Filtering),
          count: None,
        }
      ]
    });
  let player_model_diffuse_bind_group = renderer.device().create_bind_group(&wgpu::BindGroupDescriptor {
    label: Some("player_model_bind_group"),
    layout: &player_model_diffuse_bind_group_layout,
    entries: &[
      wgpu::BindGroupEntry {
        binding: 0,
        resource: wgpu::BindingResource::TextureView(&player_model_diffuse_view),
      },
      wgpu::BindGroupEntry {
        binding: 1,
        resource: wgpu::BindingResource::Sampler(&player_model_diffuse_sampler),
      }
    ]
  });

  let player_model = load_obj_prefab(&renderer, &assman, &PathBuf::from("playermodel1.obj"));

  storages.add_unique_non_send_sync(GpuPrefabs {
    block_diffuse_texture,
    block_diffuse_bind_group_layout,
    block_diffuse_bind_group,
    player_model_diffuse_texture,
    player_model_diffuse_bind_group_layout,
    player_model_diffuse_bind_group,
    player_model,
  });

  log::info!("Loading the UI stuff...");
  {
    let asset_handle = assman.open_asset(Path::new("fonts/Crisp.ttf")).unwrap();
    let mut font_data = vec![];
    BufReader::new(asset_handle).read_to_end(&mut font_data).unwrap();
    let font_handle = ui.hui.add_font(&font_data);
    ui.hui.push_font(font_handle);
    storages.add_unique(UiFontPrefab(font_handle));
  }

  //log::info!("Compiling shaders...");
  // storages.add_unique_non_send_sync(ChunkShaderPrefab(
  //   include_shader_prefab!(
  //     "world",
  //     "../shaders/world.vert",
  //     "../shaders/world.frag",
  //     &renderer.display
  //   )
  // ));
  // storages.add_unique_non_send_sync(ColoredShaderPrefab(
  //   include_shader_prefab!(
  //     "colored",
  //     "../shaders/colored.vert",
  //     "../shaders/colored.frag",
  //     &renderer.display
  //   )
  // ));
  // storages.add_unique_non_send_sync(Colored2ShaderPrefab(
  //   include_shader_prefab!(
  //     "colored",
  //     "../shaders/colored2.vert",
  //     "../shaders/colored2.frag",
  //     &renderer.display
  //   )
  // ));

  //log::info!("releasing shader compiler");

  //renderer.display.release_shader_compiler();
}
