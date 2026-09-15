use shipyard::UniqueView;
use crate::rendering::{primitives::PrimitiveVertex2, Renderer};
use super::uniform::SmUniform;

pub fn init_smoverlay_pipeline(
  uniform: &SmUniform,
  renderer: UniqueView<Renderer>
) -> wgpu::RenderPipeline {
  let module = renderer.device().create_shader_module(
    wgpu::include_wgsl!("../../../shaders/c2d.wgsl")
  );

  let rp_layout = renderer.device().create_pipeline_layout(
    &wgpu::PipelineLayoutDescriptor {
      label: Some("smoverlay_pipeline_layout"),
      bind_group_layouts: &[
        &uniform.bind_group_layout,
      ],
      push_constant_ranges: &[],
    }
  );

  renderer.device().create_render_pipeline(&wgpu::RenderPipelineDescriptor {
    label: Some("smoverlay_pipeline"),
    layout: Some(&rp_layout),
    vertex: wgpu::VertexState {
      module: &module,
      compilation_options: wgpu::PipelineCompilationOptions::default(),
      entry_point: Some("vs_main"),
      buffers: &[
        PrimitiveVertex2::LAYOUT,
      ],
    },
    fragment: Some(wgpu::FragmentState {
      module: &module,
      compilation_options: wgpu::PipelineCompilationOptions::default(),
      entry_point: Some("fs_main"),
      targets: &[Some(wgpu::ColorTargetState {
      format: renderer.surface_config().format,
        blend: Some(wgpu::BlendState::ALPHA_BLENDING),
        write_mask: wgpu::ColorWrites::ALL,
      })],
    }),
    primitive: wgpu::PrimitiveState {
      topology: wgpu::PrimitiveTopology::TriangleList,
      strip_index_format: None,
      front_face: wgpu::FrontFace::Ccw,
      cull_mode: Some(wgpu::Face::Back),
      polygon_mode: wgpu::PolygonMode::Fill,
      conservative: false,
      unclipped_depth: false,
    },
    depth_stencil: None,
    multisample: wgpu::MultisampleState::default(),
    multiview: None,
    cache: None,
  })
}
