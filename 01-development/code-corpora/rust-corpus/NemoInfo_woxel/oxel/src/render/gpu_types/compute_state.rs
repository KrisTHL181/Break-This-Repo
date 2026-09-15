use crate::render::{egui_dev::RenderMode, gpu_types::GpuUniform, Camera};
use bytemuck_derive::{Pod, Zeroable};
use cgmath::SquareMatrix;
use wgpu::{util::DeviceExt, BindGroup, BindGroupLayout, Buffer, Device};

#[repr(C)]
// This is so we can store this in a buffer
#[derive(Debug, Copy, Clone, Pod, Zeroable)]
pub struct ComputeState {
    // We can't use cgmath with bytemuck directly so we'll have
    // to convert the Matrix4 into a 4x4 f32 array
    view_projection: [[f32; 4]; 4],
    camera_to_world: [[f32; 4]; 4],
    eye: [f32; 4],
    // First row of camera to world matrix (u)
    u: [f32; 4],
    // Negative second row of camera to world matrix (-v)
    mv: [f32; 4],
    // w' = (-width / 2) u + (height / 2) v - ((height / 2) / tan(fov * 0.5)) w
    wp: [f32; 4],
    // Type of rendering we are doing
    render_mode: [u32; 4],
    // Flags that enable/disable highlighting boundry voxels
    show_345: [u32; 4],
    // Direction of sun vector
    sun_dir: [f32; 4],
    // Color of sun vector, alpha channel is for intenisty
    sun_color: [f32; 4],
}

impl GpuUniform for ComputeState {
    fn bind(&self, device: &Device) -> (Buffer, Vec<u8>, wgpu::BindGroup, BindGroupLayout) {
        let buffer_contents = self.get_buffer_contents();
        let buffer = self.create_buffer(device, &buffer_contents);
        let layout = self.create_bind_group_layout(device);
        let bind_group = self.create_bind_group(&buffer, &layout, device);

        (buffer, buffer_contents, bind_group, layout)
    }
}

impl ComputeState {
    fn get_buffer_contents(&self) -> Vec<u8> {
        bytemuck::cast_slice(&[*self]).to_vec()
    }

    fn create_buffer(&self, device: &Device, buffer_contents: &[u8]) -> Buffer {
        device.create_buffer_init(&wgpu::util::BufferInitDescriptor {
            label: Some("Camera Buffer"),
            contents: buffer_contents,
            usage: wgpu::BufferUsages::UNIFORM | wgpu::BufferUsages::COPY_DST,
        })
    }

    fn create_bind_group_layout(&self, device: &Device) -> BindGroupLayout {
        device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
            entries: &[wgpu::BindGroupLayoutEntry {
                binding: 0,
                visibility: wgpu::ShaderStages::COMPUTE,
                ty: wgpu::BindingType::Buffer {
                    ty: wgpu::BufferBindingType::Uniform,
                    has_dynamic_offset: false,
                    min_binding_size: None,
                },
                count: None,
            }],
            label: Some("Camera Bind Group Layout"),
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
            label: Some("Camera Bind Group"),
        })
    }

    pub fn build(
        c: &Camera,
        resolution_width: f32,
        render_mode: RenderMode,
        show_grid: [bool; 3],
        sun_dir3: [f32; 3],
        sun_color3: [f32; 3],
        sun_intensity: f32,
    ) -> Self {
        let view_proj = c.build_view_projection_matrix();
        let camera_to_world = match view_proj.invert() {
            Some(c) => c,
            None => panic!("Could not invert camera matrix"),
        };
        let eye = [c.eye.x, c.eye.y, c.eye.z, 0.0];
        let height = resolution_width / c.aspect;
        let u = camera_to_world.x;
        let v = camera_to_world.y;
        let w = camera_to_world.z;
        let wp = (-resolution_width / 2.0) * u + (height / 2.0) * v
            - w * (height / 2.0) / (c.fovy.to_radians() * 0.5).tan();
        let mv = -v;
        let render_mode = [render_mode as u32, 0, 0, 0];
        let show_grid = show_grid.map(|x| x as u32);
        let show_345 = [show_grid[0], show_grid[1], show_grid[2], 0];

        let mut sun_dir = [0.0; 4];
        sun_dir[..3].copy_from_slice(&sun_dir3);

        let mut sun_color = [sun_intensity; 4];
        sun_color[..3].copy_from_slice(&sun_color3);

        Self {
            view_projection: view_proj.into(),
            camera_to_world: camera_to_world.into(),
            eye,
            u: u.into(),
            mv: mv.into(),
            wp: wp.into(),
            render_mode,
            show_345,
            sun_dir,
            sun_color,
        }
    }
}
