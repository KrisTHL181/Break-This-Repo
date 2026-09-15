use bytemuck_derive::{Pod, Zeroable};
use wgpu::{util::DeviceExt, BindGroup, BindGroupLayout, Buffer, Device};

use crate::scene::State;

use super::GpuUniform;

#[repr(C)]
#[derive(Clone, Copy, Debug, Pod, Zeroable)]
pub struct StateUniform {
    pub screen_size: [f32; 4],
}

impl GpuUniform for StateUniform {
    fn bind(&self, device: &Device) -> (Buffer, Vec<u8>, BindGroup, BindGroupLayout) {
        let buffer_contents = self.get_buffer_contents();
        let buffer = self.create_buffer(device, &buffer_contents);
        let layout = self.create_bind_group_layout(device);
        let bind_group = self.create_bind_group(&buffer, &layout, device);

        (buffer, buffer_contents, bind_group, layout)
    }
}

impl StateUniform {
    fn get_buffer_contents(&self) -> Vec<u8> {
        bytemuck::cast_slice(&[*self]).to_vec()
    }

    fn create_buffer(&self, device: &Device, buffer_contents: &[u8]) -> Buffer {
        device.create_buffer_init(&wgpu::util::BufferInitDescriptor {
            label: Some("Screen State Buffer"),
            contents: buffer_contents,
            usage: wgpu::BufferUsages::UNIFORM | wgpu::BufferUsages::COPY_DST,
        })
    }

    fn create_bind_group_layout(&self, device: &Device) -> BindGroupLayout {
        device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
            entries: &[wgpu::BindGroupLayoutEntry {
                binding: 0,
                visibility: wgpu::ShaderStages::FRAGMENT,
                ty: wgpu::BindingType::Buffer {
                    ty: wgpu::BufferBindingType::Uniform,
                    has_dynamic_offset: false,
                    min_binding_size: None,
                },
                count: None,
            }],
            label: Some("Screen State Bind Group Layout"),
        })
    }

    fn create_bind_group(
        &self,
        buffer: &Buffer,
        layout: &BindGroupLayout,
        device: &Device,
    ) -> BindGroup {
        device.create_bind_group(&wgpu::BindGroupDescriptor {
            layout,
            entries: &[wgpu::BindGroupEntry {
                binding: 0,
                resource: buffer.as_entire_binding(),
            }],
            label: Some("Screen State Bind Group"),
        })
    }
}

impl<'a> From<&'a State> for StateUniform {
    fn from(state: &'a State) -> Self {
        let [width, height]: [f32; 2] = state.resolution;
        Self {
            screen_size: [width, height, 0.0, 0.0],
        }
    }
}
