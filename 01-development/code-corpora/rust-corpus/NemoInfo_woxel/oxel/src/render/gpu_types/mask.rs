use crate::vdb::{VdbValueType, VDB345};
use bytemuck_derive::{Pod, Zeroable};
use wgpu::{util::DeviceExt, BindGroup, BindGroupLayout, Buffer, Device};

#[derive(Debug, Clone)]
pub struct MaskUniform {
    kids5: Vec<Node5Mask>,
    vals5: Vec<Node5Mask>,
    kids4: Vec<Node4Mask>,
    vals4: Vec<Node4Mask>,
    vals3: Vec<Node3Mask>,
    origins: Vec<[i32; 4]>,
}

impl MaskUniform {
    pub fn bind(&self, device: &Device) -> ([Buffer; 6], [Vec<u8>; 6], BindGroup, BindGroupLayout) {
        let buffer_contents = self.get_contents();
        let buffers = self.create_buffers(device, &buffer_contents);
        let layout = self.create_bind_group_layout(device);
        let bind_group = self.create_bind_group(&buffers, &layout, device);

        (buffers, buffer_contents, bind_group, layout)
    }

    fn get_contents(&self) -> [Vec<u8>; 6] {
        [
            bytemuck::cast_slice(&self.kids5).to_vec(),
            bytemuck::cast_slice(&self.vals5).to_vec(),
            bytemuck::cast_slice(&self.kids4).to_vec(),
            bytemuck::cast_slice(&self.vals4).to_vec(),
            bytemuck::cast_slice(&self.vals3).to_vec(),
            bytemuck::cast_slice(&self.origins).to_vec(),
        ]
    }

    fn create_buffers(&self, device: &Device, buffer_contents: &[Vec<u8>; 6]) -> [Buffer; 6] {
        buffer_contents
            .iter()
            .enumerate()
            .map(|(idx, contents)| {
                device.create_buffer_init(&wgpu::util::BufferInitDescriptor {
                    label: Some(&format!("Mask Buffer {idx}")),
                    contents,
                    usage: wgpu::BufferUsages::STORAGE | wgpu::BufferUsages::COPY_DST,
                })
            })
            .collect::<Vec<_>>()
            .try_into()
            .unwrap()
    }

    fn create_bind_group_layout(&self, device: &Device) -> BindGroupLayout {
        let entries = &(0..=5)
            .map(|binding| wgpu::BindGroupLayoutEntry {
                binding,
                visibility: wgpu::ShaderStages::COMPUTE,
                ty: wgpu::BindingType::Buffer {
                    ty: wgpu::BufferBindingType::Storage { read_only: true },
                    has_dynamic_offset: false,
                    min_binding_size: None,
                },
                count: None,
            })
            .collect::<Vec<_>>()[..];

        device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
            entries,
            label: Some("Mask Bind Group Layout"),
        })
    }

    fn create_bind_group(
        &self,
        buffers: &[Buffer; 6],
        layout: &BindGroupLayout,
        device: &Device,
    ) -> BindGroup {
        let entries = &buffers
            .iter()
            .enumerate()
            .map(|(binding, buffer)| wgpu::BindGroupEntry {
                binding: binding as u32,
                resource: buffer.as_entire_binding(),
            })
            .collect::<Vec<_>>()[..];

        device.create_bind_group(&wgpu::BindGroupDescriptor {
            layout,
            entries,
            label: Some("Mask Bind Group"),
        })
    }
}

impl<'a, ValueType: VdbValueType> From<&'a VDB345<ValueType>> for MaskUniform {
    fn from(vdb: &'a VDB345<ValueType>) -> Self {
        let masks = vdb.masks();

        let kids5: Vec<Node5Mask> = masks.0.iter().map(|x| Node5Mask(*x)).collect();
        let vals5: Vec<Node5Mask> = masks.1.iter().map(|x| Node5Mask(*x)).collect();
        let kids4: Vec<Node4Mask> = masks.2.iter().map(|x| Node4Mask(*x)).collect();
        let vals4: Vec<Node4Mask> = masks.3.iter().map(|x| Node4Mask(*x)).collect();
        let vals3: Vec<Node3Mask> = masks.4.iter().map(|x| Node3Mask(*x)).collect();
        let origins = vdb
            .origins()
            .iter()
            .map(|&[x, y, z]| [x, y, z, 0])
            .collect::<Vec<_>>();

        Self {
            kids5,
            vals5,
            kids4,
            vals4,
            vals3,
            origins,
        }
    }
}

#[repr(C)]
#[derive(Debug, Copy, Clone, Pod, Zeroable)]
struct Node5Mask([u32; 32 * 32 * 32 / 32]);

#[repr(C)]
#[derive(Debug, Copy, Clone, Pod, Zeroable)]
struct Node4Mask([u32; 16 * 16 * 16 / 32]);

#[repr(C)]
#[derive(Debug, Copy, Clone, Pod, Zeroable)]
struct Node3Mask([u32; 8 * 8 * 8 / 32]);
