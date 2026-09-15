use std::{collections::HashMap, io::BufReader, sync::{Arc, Mutex}};

use log::warn;
use wgpu::{BindGroup, BindGroupLayout, Buffer, BufferAsyncError, ShaderModule, Texture};
use winit::window::Window;

use crate::{render::gpu_types::MaskUniform, scene::Scene, vdb::VdbReader};

use super::{
    recorder::{Frame, FrameRecorder},
    egui_dev::{EguiDev, VdbFile},
    frame_descriptor::FrameDescriptor,
    pipelines::{CPipeline, ComputePipeline, Pipeline, VoxelPipeline},
};

pub struct WgpuContext {
    pub surface: wgpu::Surface,
    pub device: wgpu::Device,
    pub queue: wgpu::Queue,
    pub config: wgpu::SurfaceConfiguration,
    pub size: winit::dpi::PhysicalSize<u32>,
    pub egui_dev: EguiDev,
    pub egui_rpass: egui_wgpu_backend::RenderPass,
    atlas_group: ([Texture; 3], BindGroup, BindGroupLayout),
    masks_group: ([Buffer; 6], [Vec<u8>; 6], BindGroup, BindGroupLayout),
    shaders: HashMap<&'static str, ShaderModule>,
    pub frame_recorder: Option<Arc<Mutex<FrameRecorder>>>,
    rt: tokio::runtime::Runtime,
    _textures: HashMap<&'static str, (Texture, BindGroup, BindGroupLayout)>,
}

impl WgpuContext {
    pub async fn new(window: &Window) -> Self {
        let rt = tokio::runtime::Runtime::new().expect("Failed tp create tokio runtime");

        let size = window.inner_size();

        let instance = wgpu::Instance::new(wgpu::InstanceDescriptor {
            backends: wgpu::Backends::all(),
            dx12_shader_compiler: Default::default(),
            ..Default::default()
        });

        let surface = unsafe { instance.create_surface(&window) }.unwrap();

        let adapter = instance
            .request_adapter(&wgpu::RequestAdapterOptions {
                power_preference: wgpu::PowerPreference::HighPerformance,
                compatible_surface: Some(&surface),
                force_fallback_adapter: false,
            })
            .await
            .unwrap();

        let backend = format!("{:?}", adapter.get_info().backend);
        let name = format!("{:?}", adapter.get_info().name);
        warn!("Backend: {}", backend);
        warn!("Adaptor: {}", name);

        let (device, queue) = adapter
            .request_device(
                &wgpu::DeviceDescriptor {
                    features: wgpu::Features::empty(),
                    // WebGL doesn't support all of wgpu's features, so if
                    // we're building for the web we'll have to disable some.
                    limits: if cfg!(target_arch = "wasm32") {
                        wgpu::Limits::downlevel_webgl2_defaults()
                    } else {
                        wgpu::Limits::default()
                    },
                    label: None,
                },
                None, // Trace path
            )
            .await
            .unwrap();

        warn!("Device: {:?}", &device);

        let surface_caps = surface.get_capabilities(&adapter);
        // we want to work with srgb format
        let surface_format = surface_caps
            .formats
            .iter()
            .copied()
            .filter(|f| f.is_srgb())
            .next()
            .unwrap_or(surface_caps.formats[0]);

        let config = wgpu::SurfaceConfiguration {
            usage: wgpu::TextureUsages::RENDER_ATTACHMENT,
            format: surface_format,
            width: size.width,
            height: size.height,
            present_mode: surface_caps.present_modes[0],
            alpha_mode: surface_caps.alpha_modes[0],
            view_formats: vec![],
        };
        surface.configure(&device, &config);

        let f = std::fs::File::open("assets/utahteapot.vdb").unwrap();
        let mut vdb_reader = VdbReader::new(BufReader::new(f)).unwrap();
        let mut vdb = vdb_reader.read_vdb345_grid::<u32>("ls_utahteapot").unwrap();
        vdb.compute_sdf();
        warn!("Loaded vdb");
        let atlas = vdb.atlas();


        let atlas_size = atlas
            .iter()
            .map(|n| [n.len() as u32, n[0].len() as u32, n[0][0].len() as u32])
            .collect::<Vec<_>>()
            .try_into()
            .unwrap();

        let (atlas_textures, bind_group, bind_group_layout) =
            FrameDescriptor::create_compute_vdb_atlas_texture_binding(&device, atlas_size);

        let flat_atlas: [Vec<u8>; 3] = atlas
            .iter()
            .map(|cube| {
                let mut flat = vec![];
                for x in 0..cube.len() {
                    for y in 0..cube[0].len() {
                        for z in 0..cube[0][0].len() {
                            flat.append(&mut cube[z][y][x].to_ne_bytes().to_vec());
                        }
                    }
                }
                flat
            })
            .collect::<Vec<_>>()
            .try_into()
            .unwrap();

        for i in 0..3 {
            queue.write_texture(
                atlas_textures[i].as_image_copy(),
                &flat_atlas[i],
                wgpu::ImageDataLayout {
                    offset: 0,
                    bytes_per_row: Some(atlas_size[i][0] * 4), // 4 bytes per u32
                    rows_per_image: Some(atlas_size[i][1]),
                },
                wgpu::Extent3d {
                    width: atlas_size[i][0],
                    height: atlas_size[i][1],
                    depth_or_array_layers: atlas_size[i][2],
                },
            );
        }

        // TODO: Maybe export this to a function in FrameDescriptor
        let (mask_buffers, mask_buffers_contents, mask_bind_group, mask_bind_group_layout) =
            MaskUniform::from(&vdb).bind(&device);

        for i in 0..mask_buffers.len() {
            queue.write_buffer(&mask_buffers[i], 0, &mask_buffers_contents[i]);
        }

        let egui_platform =
            egui_winit_platform::Platform::new(egui_winit_platform::PlatformDescriptor {
                physical_width: size.width,
                physical_height: size.height,
                scale_factor: window.scale_factor(),
                font_definitions: egui::FontDefinitions::default(),
                style: Default::default(),
            });

        let mut egui_dev = EguiDev::new(egui_platform);
        // @HACK: This is kind of ugly
        egui_dev.selected_model = egui_dev.models.binary_search_by_key(&"utahteapot".to_string(), |x| x.name.clone()).unwrap();

        let egui_rpass = egui_wgpu_backend::RenderPass::new(&device, surface_format, 1);

        Self {
            surface,
            device,
            queue,
            config,
            size,
            egui_dev,
            egui_rpass,
            masks_group: (
                mask_buffers,
                mask_buffers_contents,
                mask_bind_group,
                mask_bind_group_layout,
            ),
            atlas_group: (atlas_textures, bind_group, bind_group_layout),
            shaders: HashMap::new(),
            frame_recorder: None,
            rt,
            _textures: HashMap::new(),
        }
    }

    pub fn resize(&mut self, new_size: winit::dpi::PhysicalSize<u32>) {
        if new_size.width > 0 && new_size.height > 0 {
            self.size = new_size;
            self.config.width = new_size.width;
            self.config.height = new_size.height;
            self.surface.configure(&self.device, &self.config);
        }
    }

    pub fn render(&mut self, scene: &Scene, window: &Window) -> Result<(), wgpu::SurfaceError> {
        let output = self.surface.get_current_texture()?;
        let view = output
            .texture
            .create_view(&wgpu::TextureViewDescriptor::default());

        let mut encoder = self
            .device
            .create_command_encoder(&wgpu::CommandEncoderDescriptor {
                label: Some("Render Encoder"),
            });

        let frame_descriptor = FrameDescriptor::build();

        let vertex_buffer = frame_descriptor.create_vertex_buffer(&self.device);

        let index_buffer = frame_descriptor.create_index_buffer(&self.device);

        let num_indices = frame_descriptor.indicies().len() as u32;

        let (
            compute_state_buffer,
            compute_state_buffer_contents,
            compute_state_bind_group,
            compute_state_bind_group_layout,
        ) = FrameDescriptor::create_compute_state_binding(&scene.camera, self.size, &self.device, &self.egui_dev);

        let (state_buffer, state_buffer_contents, state_bind_group, state_bind_group_layout) =
            FrameDescriptor::create_screen_state_binding(&self.device, &scene.state);

        let (compute_texture, compute_texture_bind_group, compute_texture_bind_group_layout) =
            FrameDescriptor::create_compute_output_texture_binding(&self.device, self.size.into());

        let (fragment_texture, fragment_texture_bind_group, fragment_texture_bind_group_layout) =
            FrameDescriptor::create_fragment_texture_binding(&self.device, self.size.into());

        let compute_pipline_layout =
            self.device
                .create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
                    label: Some("Compute Pipline Layout"),
                    bind_group_layouts: &[
                        &compute_state_bind_group_layout,
                        &compute_texture_bind_group_layout,
                        &self.atlas_group.2,
                        &self.masks_group.3,
                    ],
                    push_constant_ranges: &[],
                });

        let render_pipeline_layout =
            self.device
                .create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
                    label: Some("Render Pipeline Layout"),
                    bind_group_layouts: &[
                        &fragment_texture_bind_group_layout,
                        &state_bind_group_layout,
                    ],
                    push_constant_ranges: &[],
                });

        let compute_pipeline = ComputePipeline.get(self, compute_pipline_layout);
        let render_pipeline = VoxelPipeline.get(self, render_pipeline_layout);

        {
            let mut compute_pass = encoder.begin_compute_pass(&wgpu::ComputePassDescriptor {
                label: Some("Compute Pass"),
            });

            compute_pass.set_pipeline(&compute_pipeline);
            compute_pass.set_bind_group(0, &compute_state_bind_group, &[]);
            compute_pass.set_bind_group(1, &compute_texture_bind_group, &[]);
            compute_pass.set_bind_group(2, &self.atlas_group.1, &[]);
            compute_pass.set_bind_group(3, &self.masks_group.2, &[]);
            // @TODO: CHOOSE WORKGROUPS BASED ON ADAPTOR (32 for NVDIA, 64 for AMD)
            compute_pass.dispatch_workgroups(self.size.width / 8, self.size.height / 4, 1);
        }

        encoder.copy_texture_to_texture(
            compute_texture.as_image_copy(),
            fragment_texture.as_image_copy(),
            wgpu::Extent3d {
                width: self.size.width,
                height: self.size.height,
                depth_or_array_layers: 1,
            },
        );

        let output_view = output
            .texture
            .create_view(&wgpu::TextureViewDescriptor::default());

        {
            let mut render_pass = encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
                label: Some("Render Pass"),
                color_attachments: &[Some(wgpu::RenderPassColorAttachment {
                    view: &view,
                    resolve_target: None,
                    ops: wgpu::Operations {
                        load: wgpu::LoadOp::Clear(frame_descriptor.clear_color),
                        store: true,
                    },
                })],
                depth_stencil_attachment: None,
                ..Default::default()
            });
            render_pass.set_pipeline(&render_pipeline);
            render_pass.set_bind_group(0, &fragment_texture_bind_group, &[]);
            render_pass.set_bind_group(1, &state_bind_group, &[]);
            render_pass.set_vertex_buffer(0, vertex_buffer.slice(..));
            render_pass.set_index_buffer(index_buffer.slice(..), wgpu::IndexFormat::Uint16);
            render_pass.draw_indexed(0..num_indices, 0, 0..1);
        }

        let (tdelta, paint_jobs, screen_descriptor, model_changed, reload_shaders, recording_changed) = self.egui_dev.get_frame(scene, window);

        // @HACK: refactor egui out and get shader from context
        if reload_shaders {
            match std::fs::read_to_string("src/shaders/raycast.comp.wgsl") {
                Ok(source) => self.reload_shader("raycast.comp", source),
                Err(e) => panic!("Compute shaders missing {e}"),
            }
        }

        if self.egui_dev.show {
            self.egui_rpass
                .add_textures(&self.device, &self.queue, &tdelta)
                .expect("add textures ok");
            self.egui_rpass
                .update_buffers(&self.device, &self.queue, &paint_jobs, &screen_descriptor);
            self.egui_rpass
                .execute(
                    &mut encoder,
                    &output_view,
                    &paint_jobs,
                    &screen_descriptor,
                    None,
                )
                .unwrap();
        }

        self.queue
            .write_buffer(&state_buffer, 0, &state_buffer_contents);
        self.queue
            .write_buffer(&compute_state_buffer, 0, &compute_state_buffer_contents);

        if recording_changed {
            if self.egui_dev.recording {
                let mut fr = FrameRecorder::new(self.egui_dev.recording_file.clone().into());
                fr.start_encoding_thread();
                self.frame_recorder = Some(Arc::new(Mutex::new(fr)));
            } else {
                match self.frame_recorder.take() {
                    Some(fr) => {
                        self.rt.spawn(async move {
                            fr.lock()
                              .expect("Could not aquire frame recorder")
                              .end_encoder();
                        });
                    }
                    _ => {
                        println!("This would be strange");
                   }
                }
            }
        }

        // Capture screen
        let should_capture = match &self.frame_recorder {
            Some(fr) => fr.lock().expect("failed to get recording lock").prev_frame_time.elapsed().as_millis() > 33,
            None => false,
        };

        if should_capture{
            let recording_buffer = FrameDescriptor::create_recording_buffer(&self.device, self.size.into());

            encoder.copy_texture_to_buffer(
                compute_texture.as_image_copy(),
                wgpu::ImageCopyBufferBase {
                    buffer: &recording_buffer,
                    layout: wgpu::ImageDataLayout {
                        offset: 0,
                        bytes_per_row:
                        Some(self.size.width * 4), rows_per_image:
                        Some(self.size.height)
                    }
                },
                wgpu::Extent3d {
                    width: self.size.width,
                    height: self.size.height,
                    depth_or_array_layers: 1 }
            );

            self.queue.submit(std::iter::once(encoder.finish()));

            self.send_recording_buffer(&recording_buffer);
        }
        else {
            self.queue.submit(std::iter::once(encoder.finish()));
        }


        output.present();

        if self.egui_dev.show{
            self.egui_rpass
                .remove_textures(tdelta)
                .expect("remove textures ok");

        }

        if model_changed {
            self.change_vdb_model(self.egui_dev.models[self.egui_dev.selected_model].clone());
        }

        Ok(())
    }

    pub fn send_recording_buffer(&self, buffer: &Buffer) {
        let Some(frame_recorder) = self.frame_recorder.clone() else {
            unreachable!();
        };

        let staging_buffer = self.device.create_buffer(&wgpu::BufferDescriptor {
            label: Some("Staging Buffer"),
            size: buffer.size(),
            usage: wgpu::BufferUsages::MAP_READ | wgpu::BufferUsages::COPY_DST,
            mapped_at_creation: false,
        });

        let mut encoder = self.device.create_command_encoder(&wgpu::CommandEncoderDescriptor {
           label: Some("Buffer to Vec encoder")
        });

        let frame_time = std::time::Instant::now();

        encoder.copy_buffer_to_buffer(buffer, 0, &staging_buffer, 0, buffer.size());
        self.queue.submit(Some(encoder.finish()));

        let (sender, receiver) = futures::channel::oneshot::channel::<Result<(), BufferAsyncError>>();


        staging_buffer.slice(..).map_async(wgpu::MapMode::Read, move |result| {
            if result.is_ok() {
                sender.send(Ok(())).expect("Failed to map");
            }
        });

        let size = self.size.into();
        let completion_fututre = async move {
            match receiver.await {
                Ok(_) => {
                    let data = staging_buffer.slice(..).get_mapped_range();
                    let result = data.to_vec();
                    drop(data);
                    staging_buffer.unmap();

                    let frame = Frame {
                        data: result,
                        size,
                        time: frame_time,
                    };

                    frame_recorder.lock().expect("shit").send_frame(frame);
                },
                Err(_) => {
                    panic!("Oops")
                }
            }
        };

        self.rt.spawn(completion_fututre);
    }

    /// Loads shaders at compile time
    pub fn add_shader(&mut self, name: &'static str, source: &'static str) {
        if self.shaders.contains_key(name) {
            panic!("Shader with name '{name}' already exists");
        }
        let shader = crate::render::Shader::new(name, source.to_string());
        self.shaders.insert(name, shader.bind(&self.device));
    }

    /// Reloads pre existiong shaders at run-time
    pub fn reload_shader(&mut self, name: &'static str, source: String) {
        if !self.shaders.contains_key(name) {
            panic!("Shader with name '{name}' doesn't exist");
        }
        let shader = crate::render::Shader::new(name, source);
        self.shaders.insert(name, shader.bind(&self.device));
    }



    pub fn get_shader(&self, name: &'static str) -> &ShaderModule {
        self.shaders
            .get(name)
            .unwrap_or_else(|| panic!("No shader with name '{name}'"))
    }

    pub fn change_vdb_model(&mut self, model: VdbFile) {
        let f = std::fs::File::open(model.path).unwrap();
        let mut vdb_reader = VdbReader::new(BufReader::new(f)).unwrap();
        // TODO: Display vdb options and read accordingly
        let mut vdb = vdb_reader
            .read_vdb345_grid::<u32>(&model.grid)
            .unwrap();

        vdb.compute_sdf();

        let atlas = vdb.atlas();

        let atlas_size = atlas
            .iter()
            .map(|n| [n.len() as u32, n[0].len() as u32, n[0][0].len() as u32])
            .collect::<Vec<_>>()
            .try_into()
            .unwrap();

        self.atlas_group =
            FrameDescriptor::create_compute_vdb_atlas_texture_binding(&self.device, atlas_size);

        let flat_atlas: [Vec<u8>; 3] = atlas
            .iter()
            .map(|cube| {
                let mut flat = vec![];
                for x in 0..cube.len() {
                    for y in 0..cube[0].len() {
                        for z in 0..cube[0][0].len() {
                            flat.append(&mut cube[z][y][x].to_ne_bytes().to_vec());
                        }
                    }
                }
                flat
            })
            .collect::<Vec<_>>()
            .try_into()
            .unwrap();

        for i in 0..3 {
            self.queue.write_texture(
                self.atlas_group.0[i].as_image_copy(),
                &flat_atlas[i],
                wgpu::ImageDataLayout {
                    offset: 0,
                    bytes_per_row: Some(atlas_size[i][0] * 4), // 4 bytes per u32
                    rows_per_image: Some(atlas_size[i][1]),
                },
                wgpu::Extent3d {
                    width: atlas_size[i][0],
                    height: atlas_size[i][1],
                    depth_or_array_layers: atlas_size[i][2],
                },
            );
        }

        self.masks_group = MaskUniform::from(&vdb).bind(&self.device);

        for i in 0..self.masks_group.0.len() {
            self.queue
                .write_buffer(&self.masks_group.0[i], 0, &self.masks_group.1[i]);
        }

        for i in 0..self.masks_group.0.len() {
            self.queue
                .write_buffer(&self.masks_group.0[i], 0, &self.masks_group.1[i]);
        }
    }
}
