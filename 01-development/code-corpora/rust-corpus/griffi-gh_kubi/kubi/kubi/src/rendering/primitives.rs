use bytemuck::{Pod, Zeroable};
use shipyard::{IntoWorkload, Workload};

mod cube;
mod fstri;
pub use cube::CubePrimitive;
pub use fstri::FstriPrimitive;

pub fn init_primitives() -> Workload {
  (
    cube::init_cube_primitive,
    fstri::init_fstri_primitive,
  ).into_workload()
}

#[derive(Clone, Copy, Default, Pod, Zeroable)]
#[repr(C, packed)]
pub struct PrimitiveVertex {
  pub position: [f32; 3],
}

impl PrimitiveVertex {
  pub const LAYOUT: wgpu::VertexBufferLayout<'static> = wgpu::VertexBufferLayout {
    array_stride: std::mem::size_of::<PrimitiveVertex>() as wgpu::BufferAddress,
    step_mode: wgpu::VertexStepMode::Vertex,
    attributes: &wgpu::vertex_attr_array![0 => Float32x3],
  };
}

#[derive(Clone, Copy, Default, Pod, Zeroable)]
#[repr(C, packed)]
pub struct PrimitiveVertex2 {
  pub position: [f32; 2],
}

impl PrimitiveVertex2 {
  pub const LAYOUT: wgpu::VertexBufferLayout<'static> = wgpu::VertexBufferLayout {
    array_stride: std::mem::size_of::<PrimitiveVertex2>() as wgpu::BufferAddress,
    step_mode: wgpu::VertexStepMode::Vertex,
    attributes: &wgpu::vertex_attr_array![0 => Float32x2],
  };
}
