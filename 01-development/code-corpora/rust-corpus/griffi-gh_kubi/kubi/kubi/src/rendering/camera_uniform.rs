use bytemuck::{Pod, Zeroable};
use shipyard::{AllStoragesView, IntoIter, Unique, UniqueView, View};
use wgpu::util::DeviceExt;
use crate::camera::Camera;
use super::Renderer;

#[derive(Debug, Clone, Copy, Default, Pod, Zeroable)]
#[repr(C, packed)]
pub struct CameraUniformData {
  pub view_proj: [f32; 4 * 4],
}

//TODO if multiple cameras, buffer per camera
#[derive(Unique)]
pub struct CameraUniformBuffer {
  pub camera_uniform_buffer: wgpu::Buffer,
  pub camera_bind_group_layout: wgpu::BindGroupLayout,
  pub camera_bind_group: wgpu::BindGroup,
}

impl CameraUniformBuffer {
  pub fn init(renderer: &Renderer, data: CameraUniformData) -> Self {
    let camera_uniform_buffer = renderer.device().create_buffer_init(&wgpu::util::BufferInitDescriptor {
      label: Some("camera_uniform_buffer"),
      contents: bytemuck::cast_slice(&[data]),
      usage: wgpu::BufferUsages::UNIFORM | wgpu::BufferUsages::COPY_DST,
    });

    let camera_bind_group_layout = renderer.device().create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
      label: Some("camera_bind_group_layout"),
      entries: &[
        wgpu::BindGroupLayoutEntry {
          binding: 0,
          visibility: wgpu::ShaderStages::VERTEX,
          ty: wgpu::BindingType::Buffer {
            ty: wgpu::BufferBindingType::Uniform,
            has_dynamic_offset: false,
            min_binding_size: None,
          },
          count: None,
        },
      ],
    });

    let camera_bind_group = renderer.device().create_bind_group(&wgpu::BindGroupDescriptor {
      label: Some("camera_bind_group"),
      layout: &camera_bind_group_layout,
      entries: &[
        wgpu::BindGroupEntry {
          binding: 0,
          resource: camera_uniform_buffer.as_entire_binding(),
        },
      ],
    });

    Self { camera_uniform_buffer, camera_bind_group_layout, camera_bind_group }
  }

  pub fn init_default(renderer: &Renderer) -> Self {
    Self::init(renderer, CameraUniformData::default())
  }

  pub fn update(&self, renderer: &Renderer, data: CameraUniformData) {
    renderer.queue().write_buffer(&self.camera_uniform_buffer, 0, bytemuck::cast_slice(&[data]));
  }
}

pub fn init_camera_uniform_buffer(storages: AllStoragesView) {
  let renderer = storages.borrow::<UniqueView<Renderer>>().unwrap();
  storages.add_unique(CameraUniformBuffer::init_default(&renderer));
}

pub fn update_camera_uniform_buffer(
  renderer: UniqueView<Renderer>,
  camera_uniform_buffer: UniqueView<CameraUniformBuffer>,
  camera: View<Camera>,
) {
  let Some(camera) = camera.iter().next() else { return };
  let proj = camera.perspective_matrix * camera.view_matrix;
  camera_uniform_buffer.update(&renderer, CameraUniformData { view_proj: proj.to_cols_array() });
}
