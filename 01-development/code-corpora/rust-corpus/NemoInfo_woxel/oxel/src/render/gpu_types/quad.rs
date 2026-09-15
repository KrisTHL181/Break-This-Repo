use bytemuck_derive::{Pod, Zeroable};

use crate::render::gpu_types::{GpuPrimitive, GpuVertex};

#[repr(C)]
#[derive(Clone, Copy, Debug, Pod, Zeroable)]
pub struct GpuQuad;

pub const GPU_QUAD: GpuQuad = GpuQuad;

impl GpuPrimitive for GpuQuad {
    fn data(&self) -> Vec<u8> {
        let vertices: &[GpuVertex] = &[
            GpuVertex {
                position: [1.0, 1.0, 0.0],
            },
            GpuVertex {
                position: [-1.0, 1.0, 0.0],
            },
            GpuVertex {
                position: [-1.0, -1.0, 0.0],
            },
            GpuVertex {
                position: [1.0, -1.0, 0.0],
            },
        ];
        bytemuck::cast_slice(vertices).to_vec()
    }

    fn desc<'a>() -> wgpu::VertexBufferLayout<'a> {
        GpuVertex::desc()
    }
}

impl GpuQuad {
    pub const INDEXES: [u16; 6] = [0, 1, 2, 0, 2, 3];
}
