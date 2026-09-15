use shipyard::{AllStoragesView, Unique, UniqueView};
use wgpu::util::DeviceExt;
use crate::rendering::Renderer;
use super::PrimitiveVertex2;

pub const FSTRI_VERTICES: &[PrimitiveVertex2] = &[
  PrimitiveVertex2 { position: [-1.0, -1.0] },
  PrimitiveVertex2 { position: [ 3.0, -1.0] },
  PrimitiveVertex2 { position: [-1.0,  3.0] },
];

#[derive(Unique)]
pub struct FstriPrimitive(pub wgpu::Buffer);

pub fn init_fstri_primitive(storages: AllStoragesView) {
  log::info!("init_fstri_primitive");
  let renderer = storages.borrow::<UniqueView<Renderer>>().unwrap();
  let buffer = renderer.device().create_buffer_init(&wgpu::util::BufferInitDescriptor {
    label: Some("fstri_vertex_buffer"),
    contents: bytemuck::cast_slice(FSTRI_VERTICES),
    usage: wgpu::BufferUsages::COPY_DST | wgpu::BufferUsages::VERTEX,
  });
  storages.add_unique(FstriPrimitive(buffer));
}
