use shipyard::UniqueView;
use crate::rendering::{
  camera_uniform::CameraUniformBuffer,
  depth::DepthTexture,
  primitives::PrimitiveVertex,
  Renderer
};

pub fn init_selection_box_pipeline(
  uniform: &super::uniform::SelectionBoxUniform,
  ren: UniqueView<Renderer>,
  depth: UniqueView<DepthTexture>,
  camera_ubo: UniqueView<CameraUniformBuffer>,
) -> wgpu::RenderPipeline {
  log::info!("init_selection_box_pipeline");

  let shader = ren.device().create_shader_module(
    wgpu::include_wgsl!("../../../shaders/selection_box.wgsl")
  );

  let selection_box_pipeline_layout = ren.device().create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
    label: Some("selection_box_pipeline_layout"),
    bind_group_layouts: &[
      &camera_ubo.camera_bind_group_layout,
      &uniform.bind_group_layout,
    ],
    push_constant_ranges: &[],
  });

  ren.device().create_render_pipeline(&wgpu::RenderPipelineDescriptor {
    label: Some("selection_box_pipeline"),
    layout: Some(&selection_box_pipeline_layout),
    fragment: Some(wgpu::FragmentState {
      module: &shader,
      entry_point: Some("fs_main"),
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
        PrimitiveVertex::LAYOUT,
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
      depth_write_enabled: false,
      depth_compare: wgpu::CompareFunction::LessEqual,
      stencil: wgpu::StencilState::default(),
      bias: wgpu::DepthBiasState::default(),
    }),
    multisample: wgpu::MultisampleState::default(),
    multiview: None,
    cache: None,
  })
}
