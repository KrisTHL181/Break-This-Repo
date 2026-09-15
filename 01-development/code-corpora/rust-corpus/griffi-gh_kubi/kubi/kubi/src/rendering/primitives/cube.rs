use shipyard::{AllStoragesView, Unique, UniqueView};
use wgpu::util::DeviceExt;
use crate::rendering::{BufferPair, Renderer};
use super::PrimitiveVertex;

#[derive(Unique)]
pub struct CubePrimitive(pub BufferPair);

/// Vertices for a centered cube with a side length of 1
const CUBE_VERTICES: &[PrimitiveVertex] = &[
  // front
  PrimitiveVertex { position: [-0.5, -0.5, 0.5] },
  PrimitiveVertex { position: [ 0.5, -0.5, 0.5] },
  PrimitiveVertex { position: [ 0.5,  0.5, 0.5] },
  PrimitiveVertex { position: [-0.5,  0.5, 0.5] },
  // back
  PrimitiveVertex { position: [-0.5, -0.5, -0.5] },
  PrimitiveVertex { position: [ 0.5, -0.5, -0.5] },
  PrimitiveVertex { position: [ 0.5,  0.5, -0.5] },
  PrimitiveVertex { position: [-0.5,  0.5, -0.5] },
];

/// Indices for a cube primitive
const CUBE_INDICES: &[u16] = &[
  0, 1, 2, 2, 3, 0, // front
  1, 5, 6, 6, 2, 1, // right
  7, 6, 5, 5, 4, 7, // back
  4, 0, 3, 3, 7, 4, // left
  4, 5, 1, 1, 0, 4, // bottom
  3, 2, 6, 6, 7, 3, // top
];

pub fn init_cube_primitive(storages: AllStoragesView) {
  log::info!("init_cube_primitive");
  let renderer = storages.borrow::<UniqueView<Renderer>>().unwrap();
  storages.add_unique(CubePrimitive(BufferPair {
    index: renderer.device().create_buffer_init(&wgpu::util::BufferInitDescriptor {
      label: Some("cube_index_buffer"),
      contents: bytemuck::cast_slice(CUBE_INDICES),
      usage: wgpu::BufferUsages::COPY_DST | wgpu::BufferUsages::INDEX,
    }),
    index_len: CUBE_INDICES.len() as u32,
    vertex: renderer.device().create_buffer_init(&wgpu::util::BufferInitDescriptor {
      label: Some("cube_vertex_buffer"),
      contents: bytemuck::cast_slice(CUBE_VERTICES),
      usage: wgpu::BufferUsages::COPY_DST | wgpu::BufferUsages::VERTEX,
    }),
    vertex_len: CUBE_VERTICES.len() as u32,
  }));
}
