use super::CPipeline;

pub struct ComputePipeline;

impl CPipeline for ComputePipeline {
    fn get(
        &self,
        context: &crate::render::WgpuContext,
        layout: wgpu::PipelineLayout,
    ) -> wgpu::ComputePipeline {
        let comp_shader = context.get_shader("raycast.comp");

        let pipeline = context
            .device
            .create_compute_pipeline(&wgpu::ComputePipelineDescriptor {
                label: Some("Compute raycast pipeline"),
                layout: Some(&layout),
                module: &comp_shader,
                entry_point: "cp_main",
            });

        pipeline
    }
}
