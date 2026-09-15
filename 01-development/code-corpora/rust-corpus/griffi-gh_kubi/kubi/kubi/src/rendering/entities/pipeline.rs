use shipyard::UniqueView;
use wgpu::include_wgsl;
use crate::{prefabs::{GpuPrefabs, ModelVertex}, rendering::{camera_uniform::CameraUniformBuffer, Renderer}};

use super::instance::InstanceData;

pub fn init_entities_pipeline(
  renderer: UniqueView<Renderer>,
  prefabs: UniqueView<GpuPrefabs>,
  camera_ubo: UniqueView<CameraUniformBuffer>,
) -> wgpu::RenderPipeline {
  log::info!("init_entities_pipeline");

  let module = renderer.device().create_shader_module(include_wgsl!("../../../shaders/entities.wgsl"));

  let pipeline_layout = renderer.device().create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
    label: Some("entities_pipeline_layout"),
    bind_group_layouts: &[
      &prefabs.player_model_diffuse_bind_group_layout,
      &camera_ubo.camera_bind_group_layout,
    ],
    push_constant_ranges: &[],
  });

  renderer.device().create_render_pipeline(&wgpu::RenderPipelineDescriptor {
    label: Some("entities_pipeline"),
    layout: Some(&pipeline_layout),
    vertex: wgpu::VertexState {
      module: &module,
      compilation_options: wgpu::PipelineCompilationOptions::default(),
      entry_point: Some("vs_main"),
      buffers: &[
        ModelVertex::LAYOUT,
        InstanceData::LAYOUT,
      ],
    },
    fragment: Some(wgpu::FragmentState {
      module: &module,
      compilation_options: wgpu::PipelineCompilationOptions::default(),
      entry_point: Some("fs_main"),
      targets: &[Some(wgpu::ColorTargetState {
        format: renderer.surface_config().format,
        blend: Some(wgpu::BlendState::REPLACE),
        write_mask: wgpu::ColorWrites::COLOR,
      })],
    }),
    primitive: wgpu::PrimitiveState {
      topology: wgpu::PrimitiveTopology::TriangleList,
      strip_index_format: None,
      front_face: wgpu::FrontFace::Ccw,
      cull_mode: None, // Some(wgpu::Face::Back), //XXX: this culls their majestic ears! :(
      polygon_mode: wgpu::PolygonMode::Fill,
      conservative: false,
      unclipped_depth: false,
    },
    depth_stencil: Some(wgpu::DepthStencilState {
      format: wgpu::TextureFormat::Depth32Float,
      depth_write_enabled: true,
      depth_compare: wgpu::CompareFunction::Less,
      bias: wgpu::DepthBiasState::default(),
      stencil: wgpu::StencilState::default(),
    }),
    multisample: wgpu::MultisampleState::default(),
    multiview: None,
    cache: None,
  })
}
