use wgpu::{Device, ShaderModule};

pub struct Shader {
    name: &'static str,
    source: String,
}

impl Shader {
    pub fn new(name: &'static str, source: String) -> Self {
        Shader { name, source }
    }

    pub fn bind(&self, device: &Device) -> ShaderModule {
        device.create_shader_module(wgpu::ShaderModuleDescriptor {
            label: Some(self.name),
            source: wgpu::ShaderSource::Wgsl(std::borrow::Cow::Borrowed(&self.source)),
        })
    }
}
