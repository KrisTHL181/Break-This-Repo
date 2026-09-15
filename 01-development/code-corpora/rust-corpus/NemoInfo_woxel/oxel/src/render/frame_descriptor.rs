use wgpu::{util::DeviceExt, BindGroup, BindGroupLayout, Buffer, Color, Device, Texture};
use winit::dpi::PhysicalSize;

use crate::{
    render::{
        gpu_types::{ComputeState, GpuUniform},
        Camera,
    },
    scene::State,
};

use super::{
    egui_dev::EguiDev,
    gpu_types::{
        ComputeOutputTexture, FragmentTexture, GpuPrimitive, GpuQuad, GpuTexture, NodeAtlas,
        StateUniform, GPU_QUAD,
    },
};

pub struct FrameDescriptor {
    pub clear_color: Color,
}

impl FrameDescriptor {
    pub fn build() -> Self {
        let clear_color = wgpu::Color {
            r: 0.0,
            g: 0.0,
            b: 0.0,
            a: 1.0,
        };
        FrameDescriptor { clear_color }
    }

    pub fn create_screen_state_binding(
        device: &Device,
        state: &State,
    ) -> (Buffer, Vec<u8>, BindGroup, BindGroupLayout) {
        StateUniform::from(state).bind(&device)
    }

    pub fn create_compute_state_binding(
        camera: &Camera,
        size: PhysicalSize<u32>,
        device: &Device,
        egui_dev: &EguiDev,
    ) -> (Buffer, Vec<u8>, BindGroup, BindGroupLayout) {
        ComputeState::build(
            camera,
            size.width as f32,
            egui_dev.render_mode,
            egui_dev.show_grid,
            egui_dev.sun_settings.dir3.to_array(),
            egui_dev.sun_settings.color,
            egui_dev.sun_settings.intensity,
        )
        .bind(device)
    }

    pub fn create_vertex_buffer(&self, device: &Device) -> Buffer {
        device.create_buffer_init(&wgpu::util::BufferInitDescriptor {
            label: Some("Vertex Buffer"),
            contents: &GPU_QUAD.data(),
            usage: wgpu::BufferUsages::VERTEX,
        })
    }

    pub fn indicies(&self) -> Vec<u16> {
        GpuQuad::INDEXES.to_vec()
    }

    pub fn create_index_buffer(&self, device: &Device) -> Buffer {
        device.create_buffer_init(&wgpu::util::BufferInitDescriptor {
            label: Some("Index Buffer"),
            contents: bytemuck::cast_slice(&self.indicies()),
            usage: wgpu::BufferUsages::INDEX,
        })
    }

    pub fn create_compute_output_texture_binding(
        device: &Device,
        size: [u32; 2],
    ) -> (Texture, BindGroup, BindGroupLayout) {
        ComputeOutputTexture::new(size).bind(device)
    }

    pub fn create_compute_vdb_atlas_texture_binding(
        device: &Device,
        [size5, size4, size3]: [[u32; 3]; 3],
    ) -> ([Texture; 3], BindGroup, BindGroupLayout) {
        NodeAtlas::new(size5, size4, size3).bind(device)
    }

    pub fn create_fragment_texture_binding(
        device: &Device,
        size: [u32; 2],
    ) -> (Texture, BindGroup, BindGroupLayout) {
        FragmentTexture::new(size).bind(device)
    }

    pub fn create_recording_buffer(device: &Device, [width, height]: [u32; 2]) -> Buffer {
        let bytes_per_pixel = 4;
        let unaligned_row_size = width * bytes_per_pixel;
        let align = wgpu::COPY_BYTES_PER_ROW_ALIGNMENT as u32;
        let aligned_row_size = (unaligned_row_size + align - 1) / align * align;
        let buffer_size = (aligned_row_size * height) as u64;

        let buffer = device.create_buffer(&wgpu::BufferDescriptor {
            label: Some("Recording Buffer"),
            size: buffer_size,
            usage: wgpu::BufferUsages::COPY_DST | wgpu::BufferUsages::COPY_SRC,
            mapped_at_creation: false,
        });

        buffer
    }
}
