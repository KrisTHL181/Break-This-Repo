use wgpu::{BindGroup, BindGroupLayout, Device, Sampler, Texture, TextureView};

use super::GpuTexture;

pub struct ComputeOutputTexture {
    size: [u32; 2],
}

impl GpuTexture for ComputeOutputTexture {
    fn bind(&self, device: &Device) -> (Texture, BindGroup, BindGroupLayout) {
        let texture = self.create_texture(device);
        let view = self.create_texture_view(&texture);
        let bind_group_layout = self.create_bind_group_layout(device);
        let bind_group = self.create_bind_group(device, &bind_group_layout, &view);

        (texture, bind_group, bind_group_layout)
    }
}

impl ComputeOutputTexture {
    pub fn new(size: [u32; 2]) -> Self {
        Self { size }
    }

    fn create_texture(&self, device: &Device) -> Texture {
        let texture = device.create_texture(&wgpu::TextureDescriptor {
            size: wgpu::Extent3d {
                width: self.size[0],
                height: self.size[1],
                depth_or_array_layers: 1,
            },
            mip_level_count: 1,
            sample_count: 1,
            dimension: wgpu::TextureDimension::D2,
            format: wgpu::TextureFormat::Rgba8Unorm,
            usage: wgpu::TextureUsages::TEXTURE_BINDING
                | wgpu::TextureUsages::STORAGE_BINDING
                | wgpu::TextureUsages::COPY_DST
                | wgpu::TextureUsages::COPY_SRC,
            label: Some("Compute Texture"),
            view_formats: &[],
        });

        texture
    }

    fn create_texture_view(&self, texture: &Texture) -> TextureView {
        let texture_view = texture.create_view(&wgpu::TextureViewDescriptor {
            label: Some("Compute Texture View"),
            format: Some(wgpu::TextureFormat::Rgba8Unorm),
            base_mip_level: 0,
            mip_level_count: Some(1),
            ..Default::default()
        });

        texture_view
    }

    fn create_bind_group_layout(&self, device: &Device) -> BindGroupLayout {
        let bind_group_layout = device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
            label: Some("Compute Texture Bind Group Layout"),
            entries: &[wgpu::BindGroupLayoutEntry {
                binding: 0,
                visibility: wgpu::ShaderStages::COMPUTE,
                ty: wgpu::BindingType::StorageTexture {
                    access: wgpu::StorageTextureAccess::WriteOnly,
                    format: wgpu::TextureFormat::Rgba8Unorm,
                    view_dimension: wgpu::TextureViewDimension::D2,
                },
                count: None,
            }],
        });

        bind_group_layout
    }

    fn create_bind_group(
        &self,
        device: &Device,
        layout: &BindGroupLayout,
        view: &TextureView,
    ) -> BindGroup {
        let bind_group = device.create_bind_group(&wgpu::BindGroupDescriptor {
            label: Some("Compute Texture Bind Group"),
            layout,
            entries: &[wgpu::BindGroupEntry {
                binding: 0,
                resource: wgpu::BindingResource::TextureView(view),
            }],
        });

        bind_group
    }
}

pub struct FragmentTexture {
    size: [u32; 2],
}

impl GpuTexture for FragmentTexture {
    fn bind(&self, device: &Device) -> (Texture, BindGroup, BindGroupLayout) {
        let texture = self.create_texture(device);
        let view = self.create_texture_view(&texture);
        let sampler = self.create_texture_sampler(device);
        let bind_group_layout = self.create_bind_group_layout(device);
        let bind_group = self.create_bind_group(device, &bind_group_layout, &view, &sampler);

        (texture, bind_group, bind_group_layout)
    }
}

impl FragmentTexture {
    pub fn new(size: [u32; 2]) -> Self {
        Self { size }
    }

    fn create_texture(&self, device: &Device) -> Texture {
        let texture = device.create_texture(&wgpu::TextureDescriptor {
            size: wgpu::Extent3d {
                width: self.size[0],
                height: self.size[1],
                depth_or_array_layers: 1,
            },
            mip_level_count: 1,
            sample_count: 1,
            dimension: wgpu::TextureDimension::D2,
            format: wgpu::TextureFormat::Rgba8Unorm,
            usage: wgpu::TextureUsages::TEXTURE_BINDING | wgpu::TextureUsages::COPY_DST,
            label: Some("Fragment Texture"),
            view_formats: &[],
        });

        texture
    }

    fn create_texture_view(&self, texture: &Texture) -> TextureView {
        let texture_view = texture.create_view(&wgpu::TextureViewDescriptor {
            ..Default::default()
        });

        texture_view
    }

    fn create_texture_sampler(&self, device: &Device) -> Sampler {
        let texture_sampler = device.create_sampler(&wgpu::SamplerDescriptor {
            address_mode_u: wgpu::AddressMode::ClampToEdge,
            address_mode_v: wgpu::AddressMode::ClampToEdge,
            address_mode_w: wgpu::AddressMode::ClampToEdge,
            mag_filter: wgpu::FilterMode::Linear,
            min_filter: wgpu::FilterMode::Nearest,
            mipmap_filter: wgpu::FilterMode::Nearest,
            ..Default::default()
        });

        texture_sampler
    }

    fn create_bind_group_layout(&self, device: &Device) -> BindGroupLayout {
        let bind_group_layout = device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
            label: Some("Fragment Texture Bind Group Layout"),
            entries: &[
                wgpu::BindGroupLayoutEntry {
                    binding: 0,
                    visibility: wgpu::ShaderStages::FRAGMENT,
                    ty: wgpu::BindingType::Texture {
                        sample_type: wgpu::TextureSampleType::Float { filterable: true },
                        view_dimension: wgpu::TextureViewDimension::D2,
                        multisampled: false,
                    },
                    count: None,
                },
                wgpu::BindGroupLayoutEntry {
                    binding: 1,
                    visibility: wgpu::ShaderStages::FRAGMENT,
                    ty: wgpu::BindingType::Sampler(wgpu::SamplerBindingType::Filtering),
                    count: None,
                },
            ],
        });

        bind_group_layout
    }

    fn create_bind_group(
        &self,
        device: &Device,
        layout: &BindGroupLayout,
        view: &TextureView,
        sampler: &Sampler,
    ) -> BindGroup {
        let bind_group = device.create_bind_group(&wgpu::BindGroupDescriptor {
            label: Some("Fragment Texture Bind Group"),
            layout,
            entries: &[
                wgpu::BindGroupEntry {
                    binding: 0,
                    resource: wgpu::BindingResource::TextureView(&view),
                },
                wgpu::BindGroupEntry {
                    binding: 1,
                    resource: wgpu::BindingResource::Sampler(&sampler),
                },
            ],
        });

        bind_group
    }
}
