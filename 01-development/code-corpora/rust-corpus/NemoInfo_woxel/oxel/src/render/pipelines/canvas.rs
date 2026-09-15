use wgpu::{PipelineLayout, RenderPipeline};

use crate::render::gpu_types::{GpuPrimitive, GpuQuad};

use super::Pipeline;

pub struct VoxelPipeline;

impl Pipeline for VoxelPipeline {
    fn get(&self, context: &crate::render::WgpuContext, layout: PipelineLayout) -> RenderPipeline {
        let vert_shader = context.get_shader("canvas.vert");
        let frag_shader = context.get_shader("canvas.frag");

        let pipeline = context
            .device
            .create_render_pipeline(&wgpu::RenderPipelineDescriptor {
                label: Some("Voxel Solid Pipline"),
                layout: Some(&layout),
                vertex: wgpu::VertexState {
                    module: vert_shader,
                    entry_point: "vs_main",
                    buffers: &[GpuQuad::desc()],
                },
                primitive: wgpu::PrimitiveState {
                    topology: wgpu::PrimitiveTopology::TriangleList,
                    strip_index_format: None,
                    front_face: wgpu::FrontFace::Ccw,
                    cull_mode: Some(wgpu::Face::Back),
                    unclipped_depth: false,
                    polygon_mode: wgpu::PolygonMode::Fill,
                    conservative: false,
                },
                depth_stencil: None,
                multisample: wgpu::MultisampleState {
                    count: 1,
                    mask: !0,
                    alpha_to_coverage_enabled: false,
                },
                fragment: Some(wgpu::FragmentState {
                    module: frag_shader,
                    entry_point: "fs_main",
                    targets: &[Some(wgpu::ColorTargetState {
                        format: context.config.format,
                        blend: Some(wgpu::BlendState::REPLACE),
                        write_mask: wgpu::ColorWrites::ALL,
                    })],
                }),
                multiview: None,
            });

        pipeline
    }
}
