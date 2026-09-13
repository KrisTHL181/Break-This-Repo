package com.mojang.renderpearl.backend.opengl;

import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.logging.LogUtils;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.GpuQueryPool;
import com.mojang.renderpearl.api.device.DeviceInfo;
import com.mojang.renderpearl.api.device.GpuDebugOptions;
import com.mojang.renderpearl.api.device.GpuOutOfMemoryException;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.backend.api.CommandEncoderBackend;
import com.mojang.renderpearl.backend.api.GpuDeviceBackend;
import com.mojang.renderpearl.backend.api.GpuSurfaceBackend;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.ARBClipControl;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GLCapabilities;
import org.slf4j.Logger;

class GlDevice implements GpuDeviceBackend {
   private static final Logger LOGGER = LogUtils.getLogger();
   protected static boolean USE_GL_ARB_vertex_attrib_binding = true;
   protected static boolean USE_GL_KHR_debug = true;
   protected static boolean USE_GL_EXT_debug_label = true;
   protected static boolean USE_GL_ARB_debug_output = true;
   protected static boolean USE_GL_ARB_direct_state_access = true;
   protected static boolean USE_GL_ARB_buffer_storage = true;
   protected static boolean USE_GL_ARB_base_instance = true;
   protected static boolean USE_GL_ARB_draw_indirect = true;
   protected static boolean USE_GL_ARB_multi_draw_indirect = true;
   protected static boolean USE_GL_ARB_shader_draw_parameters = true;
   private final GlCommandEncoder encoder;
   private final @Nullable GlDebug debugLog;
   private final GlDebugLabel debugLabels;
   private final DirectStateAccess directStateAccess;
   private final FrameBufferCache frameBufferCache = new FrameBufferCache();
   private final VertexArrayCache vertexArrayCache;
   private final BufferStorage bufferStorage;
   private final DeviceInfo deviceInfo;
   private final ShaderDefines globalDefines;
   private boolean shaderCompilerRequiresSacrifice = true;

   public GlDevice(final long windowHandle, final GpuDebugOptions debugOptions) {
      GLFW.glfwMakeContextCurrent(windowHandle);
      GLCapabilities capabilities = GL.createCapabilities();
      Set<String> enabledExtensions = new HashSet<>();
      int maxSupportedAnisotropy;
      if (capabilities.GL_EXT_texture_filter_anisotropic) {
         maxSupportedAnisotropy = Mth.floor(GL33C.glGetFloat(34047));
         enabledExtensions.add("GL_EXT_texture_filter_anisotropic");
      } else {
         maxSupportedAnisotropy = 1;
      }

      GlHeuristics heuristics = new GlHeuristics(GlStateManager._getString(7937));
      this.debugLog = GlDebug.enableDebugCallback(debugOptions.logLevel(), debugOptions.synchronousLogs(), enabledExtensions);
      this.debugLabels = GlDebugLabel.create(capabilities, debugOptions.useLabels(), enabledExtensions);
      this.vertexArrayCache = VertexArrayCache.create(capabilities, this.debugLabels, enabledExtensions);
      this.bufferStorage = BufferStorage.create(capabilities, enabledExtensions);
      this.directStateAccess = DirectStateAccess.create(capabilities, enabledExtensions, heuristics);
      GL33C.glEnable(34895);
      GL33C.glEnable(34370);
      if (capabilities.GL_ARB_clip_control) {
         ARBClipControl.glClipControl(36001, 37727);
         enabledExtensions.add("GL_ARB_clip_control");
      }

      if (capabilities.GL_ARB_shader_draw_parameters && USE_GL_ARB_shader_draw_parameters) {
         enabledExtensions.add("GL_ARB_shader_draw_parameters");
      }

      if (capabilities.GL_ARB_draw_indirect && USE_GL_ARB_draw_indirect) {
         enabledExtensions.add("GL_ARB_draw_indirect");
         if (capabilities.GL_ARB_multi_draw_indirect && USE_GL_ARB_multi_draw_indirect) {
            enabledExtensions.add("GL_ARB_multi_draw_indirect");
         }
      }

      if (capabilities.GL_ARB_base_instance && USE_GL_ARB_base_instance) {
         enabledExtensions.add("GL_ARB_base_instance");
      }

      this.deviceInfo = heuristics.createDeviceInfo(capabilities, maxSupportedAnisotropy, enabledExtensions);
      this.encoder = new GlCommandEncoder(this);
      ShaderDefines.Builder globalDefinesBuilder = ShaderDefines.builder();
      if (this.deviceInfo.isZZeroToOne()) {
         globalDefinesBuilder.define("B3D_DEPTH_IS_ZERO_TO_ONE");
      }

      this.globalDefines = globalDefinesBuilder.build();
   }

   public GlDebugLabel debugLabels() {
      return this.debugLabels;
   }

   @Override
   public GpuSurfaceBackend createSurface(final long windowHandle) {
      return new GlSurface(windowHandle);
   }

   @Override
   public CommandEncoderBackend createCommandEncoder() {
      return this.encoder;
   }

   @Override
   public GpuSampler createSampler(
      final AddressMode addressModeU,
      final AddressMode addressModeV,
      final FilterMode minFilter,
      final FilterMode magFilter,
      final int maxAnisotropy,
      final OptionalDouble maxLod
   ) {
      return new GlSampler(addressModeU, addressModeV, minFilter, magFilter, maxAnisotropy, maxLod);
   }

   @Override
   public GpuTexture createTexture(
      final @Nullable Supplier<String> label,
      final @GpuTexture.Usage int usage,
      final GpuFormat format,
      final int width,
      final int height,
      final int depthOrLayers,
      final int mipLevels
   ) {
      return this.createTexture(this.debugLabels.exists() && label != null ? label.get() : null, usage, format, width, height, depthOrLayers, mipLevels);
   }

   @Override
   public GpuTexture createTexture(
      @Nullable String label,
      final @GpuTexture.Usage int usage,
      final GpuFormat format,
      final int width,
      final int height,
      final int depthOrLayers,
      final int mipLevels
   ) {
      GlStateManager.clearGlErrors();
      int id = GlStateManager._genTexture();
      if (label == null) {
         label = String.valueOf(id);
      }

      boolean isCubemap = (usage & 16) != 0;
      int target;
      if (isCubemap) {
         GL33C.glBindTexture(34067, id);
         target = 34067;
      } else {
         GlStateManager._bindTexture(id);
         target = 3553;
      }

      GlStateManager._texParameter(target, 33085, mipLevels - 1);
      GlStateManager._texParameter(target, 33082, 0);
      GlStateManager._texParameter(target, 33083, mipLevels - 1);
      if (format.hasDepthAspect()) {
         GlStateManager._texParameter(target, 34892, 0);
      }

      int glInternalID = GlConst.toGlInternalId(format);
      int glExternalID = GlConst.toGlExternalId(format);
      int glType = GlConst.toGlType(format);
      if (glInternalID != 0 && glExternalID != 0 && glType != 0) {
         if (isCubemap) {
            for (int cubeTarget : GlConst.CUBEMAP_TARGETS) {
               for (int i = 0; i < mipLevels; i++) {
                  GlStateManager._texImage2D(cubeTarget, i, glInternalID, width >> i, height >> i, 0, glExternalID, glType, null);
               }
            }
         } else {
            for (int i = 0; i < mipLevels; i++) {
               GlStateManager._texImage2D(target, i, glInternalID, width >> i, height >> i, 0, glExternalID, glType, null);
            }
         }

         int error = GlStateManager._getError();
         if (error == 1285) {
            throw new GpuOutOfMemoryException("Could not allocate texture of " + width + "x" + height + " for " + label);
         }

         if (error != 0) {
            throw new IllegalStateException("OpenGL error " + error);
         }

         GlTexture texture = new GlTexture(usage, label, format, width, height, depthOrLayers, mipLevels, id, this.frameBufferCache);
         this.debugLabels.applyLabel(texture);
         return texture;
      } else {
         throw new IllegalArgumentException(format + " format cannot be used to create textures");
      }
   }

   @Override
   public GpuTextureView createTextureView(final GpuTexture texture) {
      return this.createTextureView(texture, 0, texture.getMipLevels());
   }

   @Override
   public GpuTextureView createTextureView(final GpuTexture texture, final int baseMipLevel, final int mipLevels) {
      return new GlTextureView((GlTexture)texture, baseMipLevel, mipLevels, this.frameBufferCache);
   }

   @Override
   public GpuBuffer createBuffer(final @Nullable Supplier<String> label, final @GpuBuffer.Usage int usage, final long size) {
      GlStateManager.clearGlErrors();
      GlBuffer buffer = this.bufferStorage.createBuffer(this.directStateAccess, usage, size);
      int error = GlStateManager._getError();
      if (error == 1285) {
         throw new GpuOutOfMemoryException("Could not allocate buffer of " + size + " for " + label);
      }

      if (error != 0) {
         throw new IllegalStateException("OpenGL error " + error);
      }

      this.debugLabels.applyLabel(buffer, label);
      return buffer;
   }

   @Override
   public GpuBuffer createBuffer(final @Nullable Supplier<String> label, final @GpuBuffer.Usage int usage, final ByteBuffer data) {
      GlStateManager.clearGlErrors();
      long size = data.remaining();
      GlBuffer buffer = this.bufferStorage.createBuffer(this.directStateAccess, usage, data);
      int error = GlStateManager._getError();
      if (error == 1285) {
         throw new GpuOutOfMemoryException("Could not allocate buffer of " + size + " for " + label);
      }

      if (error != 0) {
         throw new IllegalStateException("OpenGL error " + error);
      }

      this.debugLabels.applyLabel(buffer, label);
      return buffer;
   }

   @Override
   public List<String> getLastDebugMessages() {
      return this.debugLog == null ? Collections.emptyList() : this.debugLog.getLastOpenGlDebugMessages();
   }

   @Override
   public boolean isDebuggingEnabled() {
      return this.debugLog != null;
   }

   private void sacrificeShaderToOpenGlAndAmd() {
      if (this.shaderCompilerRequiresSacrifice) {
         this.shaderCompilerRequiresSacrifice = false;
         String glRenderer = GlStateManager._getString(7937);
         if (glRenderer.contains("AMD")) {
            int shader = GlStateManager.glCreateShader(35633);
            int program = GlStateManager.glCreateProgram();
            GlStateManager.glAttachShader(program, shader);
            GlStateManager.glDeleteShader(shader);
            GlStateManager.glDeleteProgram(program);
         }
      }
   }

   void markAmdShaderCompilerAngry() {
      this.shaderCompilerRequiresSacrifice = true;
   }

   @Override
   public void close() {
      this.encoder.close();
   }

   public DirectStateAccess directStateAccess() {
      return this.directStateAccess;
   }

   protected GlShaderModule compileShader(final Identifier id, final ShaderType type, final ShaderDefines defines, final ShaderSource shaderSource) {
      String source = shaderSource.get(id, type);
      if (source == null) {
         LOGGER.error("Couldn't find source for {} shader ({})", type, id);
         return GlShaderModule.INVALID_SHADER;
      } else {
         String sourceWithDefines = GlslPreprocessor.injectDefines(source, defines);
         sourceWithDefines = GlslPreprocessor.injectDefines(sourceWithDefines, this.globalDefines);
         int shaderId = GlStateManager.glCreateShader(GlConst.toGl(type));
         GlStateManager.glShaderSource(shaderId, sourceWithDefines);
         GlStateManager.glCompileShader(shaderId);
         if (GlStateManager.glGetShaderi(shaderId, 35713) == 0) {
            String logInfo = StringUtils.trim(GlStateManager.glGetShaderInfoLog(shaderId, 32768));
            LOGGER.error("Couldn't compile {} shader ({}): {}", new Object[]{type.getName(), id, logInfo});
            return GlShaderModule.INVALID_SHADER;
         } else {
            GlShaderModule module = new GlShaderModule(shaderId, id, type);
            this.debugLabels.applyLabel(module);
            return module;
         }
      }
   }

   private @Nullable GlProgram compileProgram(final RenderPipeline pipeline, final ShaderSource shaderSource) {
      GlShaderModule vertexShader = this.compileShader(pipeline.getVertexShader(), ShaderType.VERTEX, pipeline.getShaderDefines(), shaderSource);
      GlShaderModule fragmentShader = this.compileShader(pipeline.getFragmentShader(), ShaderType.FRAGMENT, pipeline.getShaderDefines(), shaderSource);
      if (vertexShader == GlShaderModule.INVALID_SHADER) {
         LOGGER.error("Couldn't compile pipeline {}: vertex shader {} was invalid", pipeline.getLocation(), pipeline.getVertexShader());
         return null;
      }

      if (fragmentShader == GlShaderModule.INVALID_SHADER) {
         LOGGER.error("Couldn't compile pipeline {}: fragment shader {} was invalid", pipeline.getLocation(), pipeline.getFragmentShader());
         return null;
      }

      try {
         GlProgram compiled = GlProgram.link(vertexShader, fragmentShader, pipeline.getVertexFormatBindings(), pipeline.getLocation().toString());
         compiled.setupBindGroupLayouts(pipeline.getBindGroupLayouts());
         this.debugLabels.applyLabel(compiled);
         return compiled;
      } catch (IllegalArgumentException | ShaderManager.CompilationException e) {
         LOGGER.error("Couldn't compile program for pipeline {}", pipeline.getLocation(), e);
         return null;
      }
   }

   public @Nullable GlRenderPipeline compilePipeline(final RenderPipeline pipeline, final ShaderSource shaderSource) {
      this.sacrificeShaderToOpenGlAndAmd();
      GlProgram glProgram = this.compileProgram(pipeline, shaderSource);
      return glProgram == null ? null : new GlRenderPipeline(this, pipeline, glProgram);
   }

   public VertexArrayCache vertexArrayCache() {
      return this.vertexArrayCache;
   }

   public BufferStorage getBufferStorage() {
      return this.bufferStorage;
   }

   public FrameBufferCache frameBufferCache() {
      return this.frameBufferCache;
   }

   @Override
   public GpuQueryPool createTimestampQueryPool(final int size) {
      return new GlQueryPool(size);
   }

   @Override
   public long getTimestampNow() {
      return GL33C.glGetInteger64(36392);
   }

   @Override
   public DeviceInfo getDeviceInfo() {
      return this.deviceInfo;
   }
}
