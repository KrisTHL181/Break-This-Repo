use super::glyphcache::GlyphCache;
use super::quad::*;
use super::utilsprites::{RenderMetrics, UtilSprites};
use crate::termwindow::webgpu::{adapter_info_to_gpu_info, WebGpuState, WebGpuTexture};
use ::window::bitmaps::atlas::OutOfTextureSpace;
use ::window::bitmaps::Texture2d;
use ::window::glium::backend::Context as GliumContext;
use ::window::glium::{
    CapabilitiesSource, IndexBuffer as GliumIndexBuffer, VertexBuffer as GliumVertexBuffer,
};
use ::window::*;
use anyhow::Context;
use std::cell::{Ref, RefCell, RefMut};
use std::convert::TryInto;
use std::rc::Rc;
use wezterm_font::FontConfiguration;
use wgpu::util::DeviceExt;

const INDICES_PER_CELL: usize = 6;

#[derive(Clone)]
pub enum RenderContext {
    Glium(Rc<GliumContext>),
    WebGpu(Rc<WebGpuState>),
}

pub enum RenderFrame<'a> {
    Glium(&'a mut glium::Frame),
    WebGpu,
}

impl RenderContext {
    pub fn allocate_index_buffer(&self, indices: &[u32]) -> anyhow::Result<IndexBuffer> {
        match self {
            Self::Glium(context) => Ok(IndexBuffer::Glium(GliumIndexBuffer::new(
                context,
                glium::index::PrimitiveType::TrianglesList,
                indices,
            )?)),
            Self::WebGpu(state) => Ok(IndexBuffer::WebGpu(WebGpuIndexBuffer::new(indices, state))),
        }
    }

    pub fn allocate_vertex_buffer_initializer(&self, num_quads: usize) -> Vec<Vertex> {
        match self {
            Self::Glium(_) => {
                vec![Vertex::default(); num_quads * VERTICES_PER_CELL]
            }
            Self::WebGpu(_) => vec![],
        }
    }

    pub fn allocate_vertex_buffer(
        &self,
        num_quads: usize,
        initializer: &[Vertex],
    ) -> anyhow::Result<VertexBuffer> {
        match self {
            Self::Glium(context) => Ok(VertexBuffer::Glium(GliumVertexBuffer::dynamic(
                context,
                initializer,
            )?)),
            Self::WebGpu(state) => Ok(VertexBuffer::WebGpu(WebGpuVertexBuffer::new(
                num_quads * VERTICES_PER_CELL,
                state,
            ))),
        }
    }

    pub fn allocate_texture_atlas(&self, size: usize) -> anyhow::Result<Rc<dyn Texture2d>> {
        match self {
            Self::Glium(context) => {
                let caps = context.get_capabilities();
                // You'd hope that allocating a texture would automatically
                // include this check, but it doesn't, and instead, the texture
                // silently fails to bind when attempting to render into it later.
                // So! We check and raise here for ourselves!
                let max_texture_size: usize = caps
                    .max_texture_size
                    .try_into()
                    .context("represent Capabilities.max_texture_size as usize")?;
                if size > max_texture_size {
                    anyhow::bail!(
                        "Cannot use a texture of size {} as it is larger \
                         than the max {} supported by your GPU",
                        size,
                        caps.max_texture_size
                    );
                }
                use crate::glium::texture::SrgbTexture2d;
                let surface: Rc<dyn Texture2d> = Rc::new(SrgbTexture2d::empty_with_format(
                    context,
                    glium::texture::SrgbFormat::U8U8U8U8,
                    glium::texture::MipmapsOption::NoMipmap,
                    size as u32,
                    size as u32,
                )?);
                Ok(surface)
            }
            Self::WebGpu(state) => {
                let texture: Rc<dyn Texture2d> =
                    Rc::new(WebGpuTexture::new(size as u32, size as u32, state)?);
                Ok(texture)
            }
        }
    }

    pub fn renderer_info(&self) -> String {
        match self {
            Self::Glium(ctx) => format!(
                "OpenGL: {} {}",
                ctx.get_opengl_renderer_string(),
                ctx.get_opengl_version_string()
            ),
            Self::WebGpu(state) => {
                let info = adapter_info_to_gpu_info(state.adapter_info.clone());
                format!("WebGPU: {}", info.to_string())
            }
        }
    }
}

pub enum IndexBuffer {
    Glium(GliumIndexBuffer<u32>),
    WebGpu(WebGpuIndexBuffer),
}

impl IndexBuffer {
    pub fn glium(&self) -> &GliumIndexBuffer<u32> {
        match self {
            Self::Glium(g) => g,
            _ => unreachable!(),
        }
    }
    pub fn webgpu(&self) -> &WebGpuIndexBuffer {
        match self {
            Self::WebGpu(g) => g,
            _ => unreachable!(),
        }
    }
}

pub enum VertexBuffer {
    Glium(GliumVertexBuffer<Vertex>),
    WebGpu(WebGpuVertexBuffer),
}

impl VertexBuffer {
    pub fn glium(&self) -> &GliumVertexBuffer<Vertex> {
        match self {
            Self::Glium(g) => g,
            _ => unreachable!(),
        }
    }
    pub fn webgpu(&self) -> &WebGpuVertexBuffer {
        match self {
            Self::WebGpu(g) => g,
            _ => unreachable!(),
        }
    }
    pub fn webgpu_mut(&mut self) -> &mut WebGpuVertexBuffer {
        match self {
            Self::WebGpu(g) => g,
            _ => unreachable!(),
        }
    }
}

#[derive(Default)]
struct StagingLayer {
    vertices: Vec<Vertex>,
    next_quad: usize,
    capacity: usize,
}

impl StagingLayer {
    fn from_vertex_buffer(vb: &TripleVertexBuffer) -> Self {
        let capacity = vb.capacity;
        let next_quad = *vb.next_quad.borrow();
        let copy_len = next_quad.min(capacity) * VERTICES_PER_CELL;
        let mut vertices = vec![Vertex::default(); copy_len];

        if copy_len > 0 {
            let mut current = vb.current_vb_mut();
            match &mut *current {
                VertexBuffer::Glium(buffer) => {
                    if let Some(buf_slice) = buffer.slice_mut(0..copy_len) {
                        let mapping = buf_slice.map();
                        vertices[..copy_len].copy_from_slice(&mapping[..copy_len]);
                    }
                }
                VertexBuffer::WebGpu(buffer) => {
                    buffer.read_vertices(&mut vertices[..copy_len]);
                }
            }
        }

        Self {
            vertices,
            next_quad,
            capacity,
        }
    }

    fn copy_len(&self) -> usize {
        self.vertices.len().min(self.capacity * VERTICES_PER_CELL)
    }
}

impl QuadAllocator for StagingLayer {
    fn allocate<'a>(&'a mut self) -> anyhow::Result<QuadImpl<'a>> {
        let idx = self.next_quad;
        self.next_quad += 1;
        let idx = if idx >= self.capacity {
            // Keep rendering while tracking overflow. A later pass will
            // detect the shortfall and grow the GPU buffers.
            0
        } else {
            idx
        };

        let start = idx * VERTICES_PER_CELL;
        let end = start + VERTICES_PER_CELL;
        if self.vertices.len() < end {
            self.vertices.resize(end, Vertex::default());
        }

        let mut quad = Quad {
            vert: &mut self.vertices[start..end],
        };
        quad.set_has_color(false);
        Ok(QuadImpl::Vert(quad))
    }

    fn extend_with(&mut self, vertices: &[Vertex]) {
        let idx = self.next_quad;
        let len = vertices.len();

        self.next_quad += len / VERTICES_PER_CELL;
        let start = idx * VERTICES_PER_CELL;
        let capacity = self.capacity * VERTICES_PER_CELL;

        if start + len <= capacity {
            let end = start + len;
            if self.vertices.len() < end {
                self.vertices.resize(end, Vertex::default());
            }
            self.vertices[start..end].copy_from_slice(vertices);
        }
    }
}

pub struct WebGpuVertexBuffer {
    buf: wgpu::Buffer,
    num_vertices: usize,
    state: Rc<WebGpuState>,
}

impl std::ops::Deref for WebGpuVertexBuffer {
    type Target = wgpu::Buffer;
    fn deref(&self) -> &Self::Target {
        &self.buf
    }
}

impl WebGpuVertexBuffer {
    pub fn new(num_vertices: usize, state: &Rc<WebGpuState>) -> Self {
        Self {
            buf: state.device.create_buffer(&wgpu::BufferDescriptor {
                label: Some("Vertex Buffer"),
                size: (num_vertices * std::mem::size_of::<Vertex>()) as wgpu::BufferAddress,
                usage: wgpu::BufferUsages::VERTEX,
                mapped_at_creation: true,
            }),
            num_vertices,
            state: Rc::clone(state),
        }
    }

    pub fn write_vertices(&mut self, vertices: &[Vertex]) {
        if vertices.is_empty() {
            return;
        }

        let mut mapping = self.buf.slice(..).get_mapped_range_mut();
        let mapped: &mut [Vertex] = bytemuck::cast_slice_mut(&mut mapping);
        let copy_len = vertices.len().min(mapped.len());
        if copy_len > 0 {
            mapped[..copy_len].copy_from_slice(&vertices[..copy_len]);
        }
    }

    pub fn read_vertices(&self, out: &mut [Vertex]) {
        if out.is_empty() {
            return;
        }

        let mapping = self.buf.slice(..).get_mapped_range();
        let mapped: &[Vertex] = bytemuck::cast_slice(&mapping);
        let copy_len = out.len().min(mapped.len());
        if copy_len > 0 {
            out[..copy_len].copy_from_slice(&mapped[..copy_len]);
        }
    }

    pub fn recreate(&mut self) -> wgpu::Buffer {
        let mut new_buf = self.state.device.create_buffer(&wgpu::BufferDescriptor {
            label: Some("Vertex Buffer"),
            size: (self.num_vertices * std::mem::size_of::<Vertex>()) as wgpu::BufferAddress,
            usage: wgpu::BufferUsages::VERTEX,
            mapped_at_creation: true,
        });
        std::mem::swap(&mut new_buf, &mut self.buf);
        new_buf
    }
}

pub struct WebGpuIndexBuffer {
    buf: wgpu::Buffer,
}

impl std::ops::Deref for WebGpuIndexBuffer {
    type Target = wgpu::Buffer;
    fn deref(&self) -> &Self::Target {
        &self.buf
    }
}

impl WebGpuIndexBuffer {
    pub fn new(indices: &[u32], state: &WebGpuState) -> Self {
        Self {
            buf: state
                .device
                .create_buffer_init(&wgpu::util::BufferInitDescriptor {
                    label: Some("Index Buffer"),
                    usage: wgpu::BufferUsages::INDEX,
                    contents: bytemuck::cast_slice(indices),
                }),
        }
    }
}

pub struct TripleVertexBuffer {
    pub index: RefCell<usize>,
    pub bufs: RefCell<[VertexBuffer; 3]>,
    pub indices: IndexBuffer,
    pub capacity: usize,
    pub next_quad: RefCell<usize>,
}

impl TripleVertexBuffer {
    pub fn clear_quad_allocation(&self) {
        *self.next_quad.borrow_mut() = 0;
    }

    pub fn need_more_quads(&self) -> Option<usize> {
        let next = *self.next_quad.borrow();
        if next > self.capacity {
            Some(next)
        } else {
            None
        }
    }

    /// Number of vertices/indices to draw for this buffer.
    ///
    /// `next_quad` deliberately keeps counting past `capacity` so that
    /// `need_more_quads()` can report the shortfall and grow the GPU
    /// buffers. The staging layer only ever writes `capacity` quads worth
    /// of vertices, and the index buffer only contains `capacity` quads
    /// worth of indices, so the draw call must be clamped to the capacity.
    /// Returning the unclamped count makes glium's `slice()` yield `None`
    /// and wgpu raise `Index N extends beyond limit M`, aborting the
    /// process on any frame that fails to converge.
    pub fn vertex_index_count(&self) -> (usize, usize) {
        let num_quads = (*self.next_quad.borrow()).min(self.capacity);
        (num_quads * VERTICES_PER_CELL, num_quads * INDICES_PER_CELL)
    }

    fn apply_staging(&self, layer: &StagingLayer) {
        *self.next_quad.borrow_mut() = layer.next_quad;

        let copy_len = layer.copy_len();
        if copy_len == 0 {
            return;
        }

        let mut vb = self.current_vb_mut();
        match &mut *vb {
            VertexBuffer::Glium(buffer) => {
                if let Some(buf_slice) = buffer.slice_mut(0..copy_len) {
                    let mut mapping = buf_slice.map();
                    mapping.copy_from_slice(&layer.vertices[..copy_len]);
                }
            }
            VertexBuffer::WebGpu(buffer) => {
                buffer.write_vertices(&layer.vertices[..copy_len]);
            }
        }
    }

    pub fn current_vb_mut(&self) -> RefMut<'_, VertexBuffer> {
        let index = *self.index.borrow();
        let bufs = self.bufs.borrow_mut();
        RefMut::map(bufs, |bufs| &mut bufs[index])
    }

    pub fn next_index(&self) {
        let mut index = self.index.borrow_mut();
        *index += 1;
        if *index >= 3 {
            *index = 0;
        }
    }
}

pub struct RenderLayer {
    pub vb: RefCell<[TripleVertexBuffer; 3]>,
    context: RenderContext,
    zindex: i8,
}

impl RenderLayer {
    pub fn new(context: &RenderContext, num_quads: usize, zindex: i8) -> anyhow::Result<Self> {
        let vb = [
            Self::compute_vertices(context, 32)?,
            Self::compute_vertices(context, num_quads)?,
            Self::compute_vertices(context, 32)?,
        ];

        Ok(Self {
            context: context.clone(),
            vb: RefCell::new(vb),
            zindex,
        })
    }

    pub fn clear_quad_allocation(&self) {
        for vb in self.vb.borrow().iter() {
            vb.clear_quad_allocation();
        }
    }

    pub fn quad_allocator(&self) -> TripleLayerQuadAllocator<'_> {
        TripleLayerQuadAllocator::Gpu(BorrowedLayers::new(self.vb.borrow()))
    }

    pub fn need_more_quads(&self, vb_idx: usize) -> Option<usize> {
        self.vb.borrow()[vb_idx].need_more_quads()
    }

    pub fn reallocate_quads(&self, idx: usize, num_quads: usize) -> anyhow::Result<()> {
        let vb = Self::compute_vertices(&self.context, num_quads)?;
        self.vb.borrow_mut()[idx] = vb;
        Ok(())
    }

    /// Compute a vertex buffer to hold the quads that comprise the visible
    /// portion of the screen.   We recreate this when the screen is resized.
    /// The idea is that we want to minimize any heavy lifting and computation
    /// and instead just poke some attributes into the offset that corresponds
    /// to a changed cell when we need to repaint the screen, and then just
    /// let the GPU figure out the rest.
    fn compute_vertices(
        context: &RenderContext,
        num_quads: usize,
    ) -> anyhow::Result<TripleVertexBuffer> {
        let verts = context.allocate_vertex_buffer_initializer(num_quads);
        log::trace!(
            "compute_vertices num_quads={}, allocated {} bytes",
            num_quads,
            verts.len() * std::mem::size_of::<Vertex>()
        );
        let mut indices = vec![];
        indices.reserve(num_quads * INDICES_PER_CELL);

        for q in 0..num_quads {
            let idx = (q * VERTICES_PER_CELL) as u32;

            // Emit two triangles to form the glyph quad
            indices.push(idx + V_TOP_LEFT as u32);
            indices.push(idx + V_TOP_RIGHT as u32);
            indices.push(idx + V_BOT_LEFT as u32);

            indices.push(idx + V_TOP_RIGHT as u32);
            indices.push(idx + V_BOT_LEFT as u32);
            indices.push(idx + V_BOT_RIGHT as u32);
        }

        let buffer = TripleVertexBuffer {
            index: RefCell::new(0),
            bufs: RefCell::new([
                context.allocate_vertex_buffer(num_quads, &verts)?,
                context.allocate_vertex_buffer(num_quads, &verts)?,
                context.allocate_vertex_buffer(num_quads, &verts)?,
            ]),
            capacity: num_quads,
            indices: context.allocate_index_buffer(&indices)?,
            next_quad: RefCell::new(0),
        };

        Ok(buffer)
    }
}

pub struct BorrowedLayers<'a> {
    layers: [StagingLayer; 3],
    owner: Ref<'a, [TripleVertexBuffer; 3]>,
}

impl<'a> BorrowedLayers<'a> {
    fn new(owner: Ref<'a, [TripleVertexBuffer; 3]>) -> Self {
        Self {
            layers: [
                StagingLayer::from_vertex_buffer(&owner[0]),
                StagingLayer::from_vertex_buffer(&owner[1]),
                StagingLayer::from_vertex_buffer(&owner[2]),
            ],
            owner,
        }
    }

    fn layer_mut(&mut self, layer_num: usize) -> &mut StagingLayer {
        match layer_num {
            0 => &mut self.layers[0],
            1 => &mut self.layers[1],
            2 => &mut self.layers[2],
            _ => unreachable!("invalid layer index {}", layer_num),
        }
    }
}

impl Drop for BorrowedLayers<'_> {
    fn drop(&mut self) {
        for (idx, layer) in self.layers.iter().enumerate() {
            self.owner[idx].apply_staging(layer);
        }
    }
}

impl TripleLayerQuadAllocatorTrait for BorrowedLayers<'_> {
    fn allocate(&mut self, layer_num: usize) -> anyhow::Result<QuadImpl<'_>> {
        self.layer_mut(layer_num).allocate()
    }

    fn extend_with(&mut self, layer_num: usize, vertices: &[Vertex]) {
        self.layer_mut(layer_num).extend_with(vertices)
    }
}

pub struct RenderState {
    pub context: RenderContext,
    pub glyph_cache: RefCell<GlyphCache>,
    pub util_sprites: UtilSprites,
    pub glyph_prog: Option<glium::Program>,
    pub layers: RefCell<Vec<Rc<RenderLayer>>>,
}

impl RenderState {
    pub fn new(
        context: RenderContext,
        fonts: &Rc<FontConfiguration>,
        metrics: &RenderMetrics,
        mut atlas_size: usize,
    ) -> anyhow::Result<Self> {
        loop {
            crate::startup_trace::mark("      GlyphCache::new_gl start");
            let glyph_cache = RefCell::new(GlyphCache::new_gl(&context, fonts, atlas_size)?);
            crate::startup_trace::mark("      GlyphCache::new_gl done");
            let result = UtilSprites::new(&mut *glyph_cache.borrow_mut(), metrics);
            match result {
                Ok(util_sprites) => {
                    let glyph_prog = match &context {
                        RenderContext::Glium(context) => {
                            crate::startup_trace::mark("      compile_prog (shader) start");
                            let prog = Self::compile_prog(&context, Self::glyph_shader)?;
                            crate::startup_trace::mark("      compile_prog (shader) done");
                            Some(prog)
                        }
                        RenderContext::WebGpu(_) => None,
                    };

                    let main_layer = Rc::new(RenderLayer::new(&context, 1024, 0)?);

                    return Ok(Self {
                        context,
                        glyph_cache,
                        util_sprites,
                        glyph_prog,
                        layers: RefCell::new(vec![main_layer]),
                    });
                }
                Err(OutOfTextureSpace {
                    size: Some(size), ..
                }) => {
                    atlas_size = size;
                }
                Err(OutOfTextureSpace { size: None, .. }) => {
                    anyhow::bail!("requested texture size is impossible!?")
                }
            };
        }
    }

    pub fn layer_for_zindex(&self, zindex: i8) -> anyhow::Result<Rc<RenderLayer>> {
        if let Some(layer) = self
            .layers
            .borrow()
            .iter()
            .find(|l| l.zindex == zindex)
            .map(Rc::clone)
        {
            return Ok(layer);
        }

        let layer = Rc::new(RenderLayer::new(&self.context, 128, zindex)?);
        let mut layers = self.layers.borrow_mut();
        layers.push(Rc::clone(&layer));

        // Keep the layers sorted by zindex so that they are rendered in
        // the correct order when the layers array is iterated.
        layers.sort_by_key(|layer| layer.zindex);

        Ok(layer)
    }

    /// Returns true if any of the layers needed more quads to be allocated,
    /// and if we successfully allocated them.
    /// Returns false if the quads were sufficient.
    /// Returns Err if we needed to allocate but failed.
    pub fn allocated_more_quads(&mut self) -> anyhow::Result<bool> {
        let mut allocated = false;

        for layer in self.layers.borrow().iter() {
            for vb_idx in 0..3 {
                if let Some(need_quads) = layer.need_more_quads(vb_idx) {
                    // Round up to next multiple of 128 that is >=
                    // the number of needed quads for this frame
                    let num_quads = (need_quads + 127) & !127;
                    layer.reallocate_quads(vb_idx, num_quads).with_context(|| {
                        format!(
                            "Failed to allocate {} quads (needed {})",
                            num_quads, need_quads,
                        )
                    })?;
                    log::trace!("Allocated {} quads (needed {})", num_quads, need_quads);
                    allocated = true;
                }
            }
        }

        Ok(allocated)
    }

    fn glsl_version_cache_path() -> std::path::PathBuf {
        config::CACHE_DIR.join("glsl_version")
    }

    fn cached_glsl_version() -> Option<String> {
        std::fs::read_to_string(Self::glsl_version_cache_path()).ok()
    }

    fn save_glsl_version(version: &str) {
        let path = Self::glsl_version_cache_path();
        if let Some(parent) = path.parent() {
            let _ = std::fs::create_dir_all(parent);
        }
        let _ = std::fs::write(path, version);
    }

    fn compile_prog(
        context: &Rc<GliumContext>,
        fragment_shader: fn(&str) -> (String, String),
    ) -> anyhow::Result<glium::Program> {
        let mut errors = vec![];

        let caps = context.get_capabilities();
        log::trace!("Compiling shader. context.capabilities.srgb={}", caps.srgb);

        let all_versions = ["330 core", "330", "320 es", "300 es"];
        let cached = Self::cached_glsl_version();
        let versions: Vec<&str> = if let Some(ref cached_ver) = cached {
            let trimmed = cached_ver.trim();
            if all_versions.contains(&trimmed) {
                let mut v = vec![trimmed];
                for ver in &all_versions {
                    if *ver != trimmed {
                        v.push(ver);
                    }
                }
                v
            } else {
                all_versions.to_vec()
            }
        } else {
            all_versions.to_vec()
        };

        for version in &versions {
            let (vertex_shader, fragment_shader) = fragment_shader(version);
            let source = glium::program::ProgramCreationInput::SourceCode {
                vertex_shader: &vertex_shader,
                fragment_shader: &fragment_shader,
                outputs_srgb: true,
                tessellation_control_shader: None,
                tessellation_evaluation_shader: None,
                transform_feedback_varyings: None,
                uses_point_size: false,
                geometry_shader: None,
            };
            match glium::Program::new(context, source) {
                Ok(prog) => {
                    Self::save_glsl_version(version);
                    return Ok(prog);
                }
                Err(err) => errors.push(format!("shader version: {}: {:#}", version, err)),
            };
        }

        anyhow::bail!("Failed to compile shaders: {}", errors.join("\n"))
    }

    fn glyph_shader(version: &str) -> (String, String) {
        (
            format!(
                "#version {}\n{}",
                version,
                include_str!("glyph-vertex.glsl")
            ),
            format!("#version {}\n{}", version, include_str!("glyph-frag.glsl")),
        )
    }

    pub fn config_changed(&mut self) {
        self.glyph_cache.borrow_mut().config_changed();
    }

    pub fn recreate_texture_atlas(
        &mut self,
        fonts: &Rc<FontConfiguration>,
        metrics: &RenderMetrics,
        size: Option<usize>,
    ) -> anyhow::Result<()> {
        // We make a a couple of passes at resizing; if the user has selected a large
        // font size (or a large scaling factor) then the `size==None` case will not
        // be able to fit the initial utility glyphs and apply_scale_change won't
        // be able to deal with that error situation.  Rather than make every
        // caller know how to deal with OutOfTextureSpace we try to absorb
        // and accomodate that here.
        let mut size = size;
        let mut attempt = 10;
        loop {
            match self.recreate_texture_atlas_impl(fonts, metrics, size) {
                Ok(_) => return Ok(()),
                Err(err) => {
                    attempt -= 1;
                    if attempt == 0 {
                        return Err(err);
                    }

                    if let Some(&OutOfTextureSpace {
                        size: Some(needed_size),
                        ..
                    }) = err.downcast_ref::<OutOfTextureSpace>()
                    {
                        size.replace(needed_size);
                        continue;
                    }

                    return Err(err);
                }
            }
        }
    }

    fn recreate_texture_atlas_impl(
        &mut self,
        fonts: &Rc<FontConfiguration>,
        metrics: &RenderMetrics,
        size: Option<usize>,
    ) -> anyhow::Result<()> {
        let size = size.unwrap_or_else(|| self.glyph_cache.borrow().atlas.size());
        let mut new_glyph_cache = GlyphCache::new_gl(&self.context, fonts, size)?;
        self.util_sprites = UtilSprites::new(&mut new_glyph_cache, metrics)?;

        let mut glyph_cache = self.glyph_cache.borrow_mut();

        // Steal the decoded image cache; without this, any animating gifs
        // would reset back to frame 0 each time we filled the texture
        std::mem::swap(
            &mut glyph_cache.image_cache,
            &mut new_glyph_cache.image_cache,
        );

        *glyph_cache = new_glyph_cache;
        Ok(())
    }
}
