use wgpu::VertexBufferLayout;

use crate::render::gpu_types::GpuPrimitive;
use bytemuck_derive::{Pod, Zeroable};

#[repr(C)]
#[derive(Clone, Copy, Debug, Pod, Zeroable)]
pub struct GpuVertex {
    pub position: [f32; 3],
}

impl GpuVertex {
    pub const BUFFER_LAYOUT: VertexBufferLayout<'static> = wgpu::VertexBufferLayout {
        array_stride: std::mem::size_of::<GpuVertex>() as wgpu::BufferAddress,
        step_mode: wgpu::VertexStepMode::Vertex,
        attributes: &wgpu::vertex_attr_array![0 => Float32x3],
    };
}

impl GpuPrimitive for GpuVertex {
    fn data(&self) -> Vec<u8> {
        bytemuck::cast_slice(&[*self]).to_vec()
    }

    fn desc<'a>() -> VertexBufferLayout<'a> {
        Self::BUFFER_LAYOUT
    }
}
