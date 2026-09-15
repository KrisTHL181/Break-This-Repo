use shipyard::UniqueView;
use crate::{
  prefabs::GpuPrefabs,
  rendering::{camera_uniform::CameraUniformBuffer, depth::DepthTexture, world::ChunkVertex, Renderer}
};

pub fn init_world_pipeline(
  ren: UniqueView<Renderer>,
  depth: UniqueView<DepthTexture>,
  textures: UniqueView<GpuPrefabs>,
  camera_ubo: UniqueView<CameraUniformBuffer>,
) -> (wgpu::RenderPipeline, wgpu::RenderPipeline) {
  log::info!("init_world_pipeline: creating shader module");

  let shader = ren.device().create_shader_module(
    wgpu::include_wgsl!("../../../shaders/world.wgsl")
  );

  log::info!("init_world_pipeline: creating pipeline layout");

  let world_pipeline_layout = ren.device().create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
    label: Some("world_pipeline_layout"),
    bind_group_layouts: &[
      &textures.block_diffuse_bind_group_layout,
      &camera_ubo.camera_bind_group_layout,
    ],
    push_constant_ranges: &[],
  });

  log::info!("init_world_pipeline: create main pipeline");

  let pipeline_main = ren.device().create_render_pipeline(&wgpu::RenderPipelineDescriptor {
    label: Some("world_pipeline"),
    layout: Some(&world_pipeline_layout),
    fragment: Some(wgpu::FragmentState {
      module: &shader,
      entry_point: Some("fs_main"),
      compilation_options: wgpu::PipelineCompilationOptions::default(),
      targets: &[Some(wgpu::ColorTargetState {
        format: ren.surface_config().format,
        blend: Some(wgpu::BlendState::REPLACE),
        write_mask: wgpu::ColorWrites::COLOR,
      })],
    }),
    vertex: wgpu::VertexState {
      module: &shader,
      entry_point: Some("vs_main"),
      compilation_options: wgpu::PipelineCompilationOptions::default(),
      buffers: &[
        ChunkVertex::LAYOUT,
      ],
    },
    primitive: wgpu::PrimitiveState {
      topology: wgpu::PrimitiveTopology::TriangleList,
      strip_index_format: None,
      cull_mode: Some(wgpu::Face::Back),
      front_face: wgpu::FrontFace::Ccw,
      unclipped_depth: false,
      polygon_mode: wgpu::PolygonMode::Fill,
      conservative: false,
    },
    depth_stencil: Some(wgpu::DepthStencilState {
      format: depth.depth_texture.format(),
      depth_write_enabled: true,
      depth_compare: wgpu::CompareFunction::Less,
      stencil: wgpu::StencilState::default(),
      bias: wgpu::DepthBiasState::default(),
    }),
    multisample: wgpu::MultisampleState::default(),
    multiview: None,
    cache: None,
  });

  log::info!("init_world_pipeline: create trans pipeline");

  let pipeline_trans = ren.device().create_render_pipeline(&wgpu::RenderPipelineDescriptor {
    label: Some("world_pipeline_trans"),
    layout: Some(&world_pipeline_layout),
    fragment: Some(wgpu::FragmentState {
      module: &shader,
      entry_point: Some("fs_main_trans"),
      compilation_options: wgpu::PipelineCompilationOptions::default(),
      targets: &[Some(wgpu::ColorTargetState {
        format: ren.surface_config().format,
        blend: Some(wgpu::BlendState::ALPHA_BLENDING),
        write_mask: wgpu::ColorWrites::COLOR,
      })],
    }),
    vertex: wgpu::VertexState {
      module: &shader,
      entry_point: Some("vs_main"),
      compilation_options: wgpu::PipelineCompilationOptions::default(),
      buffers: &[
        ChunkVertex::LAYOUT,
      ],
    },
    primitive: wgpu::PrimitiveState {
      topology: wgpu::PrimitiveTopology::TriangleList,
      strip_index_format: None,
      cull_mode: None,
      front_face: wgpu::FrontFace::Ccw,
      unclipped_depth: false,
      polygon_mode: wgpu::PolygonMode::Fill,
      conservative: false,
    },
    depth_stencil: Some(wgpu::DepthStencilState {
      format: depth.depth_texture.format(),
      depth_write_enabled: false,
      depth_compare: wgpu::CompareFunction::Less,
      stencil: wgpu::StencilState::default(),
      bias: wgpu::DepthBiasState::default(),
    }),
    multisample: wgpu::MultisampleState::default(),
    multiview: None,
    cache: None,
  });

  (pipeline_main, pipeline_trans)
}
