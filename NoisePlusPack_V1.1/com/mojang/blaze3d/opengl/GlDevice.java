package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.GpuOutOfMemoryException;
import com.mojang.blaze3d.GraphicsWorkarounds;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.logging.LogUtils;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class GlDevice implements GpuDevice {
   private static final Logger LOGGER = LogUtils.getLogger();
   protected static boolean USE_GL_ARB_vertex_attrib_binding = true;
   protected static boolean USE_GL_KHR_debug = true;
   protected static boolean USE_GL_EXT_debug_label = true;
   protected static boolean USE_GL_ARB_debug_output = true;
   protected static boolean USE_GL_ARB_direct_state_access = true;
   protected static boolean USE_GL_ARB_buffer_storage = true;
   private final CommandEncoder encoder;
   private final @Nullable GlDebug debugLog;
   private final GlDebugLabel debugLabels;
   private final int maxSupportedTextureSize;
   private final DirectStateAccess directStateAccess;
   private final ShaderSource defaultShaderSource;
   private final Map<RenderPipeline, GlRenderPipeline> pipelineCache = new IdentityHashMap<>();
   private final Map<GlDevice.ShaderCompilationKey, GlShaderModule> shaderCache = new HashMap<>();
   private final VertexArrayCache vertexArrayCache;
   private final BufferStorage bufferStorage;
   private final Set<String> enabledExtensions = new HashSet<>();
   private final int uniformOffsetAlignment;
   private final int maxSupportedAnisotropy;

   public GlDevice(long p_391790_, int p_397807_, boolean p_394690_, ShaderSource p_454755_, boolean p_396865_) {
      GLFW.glfwMakeContextCurrent(p_391790_);
      GLCapabilities glcapabilities = GL.createCapabilities();
      int i = getMaxSupportedTextureSize();
      GLFW.glfwSetWindowSizeLimits(p_391790_, -1, -1, i, i);
      GraphicsWorkarounds graphicsworkarounds = GraphicsWorkarounds.get(this);
      this.debugLog = GlDebug.enableDebugCallback(p_397807_, p_394690_, this.enabledExtensions);
      this.debugLabels = GlDebugLabel.create(glcapabilities, p_396865_, this.enabledExtensions);
      this.vertexArrayCache = VertexArrayCache.create(glcapabilities, this.debugLabels, this.enabledExtensions);
      this.bufferStorage = BufferStorage.create(glcapabilities, this.enabledExtensions);
      this.directStateAccess = DirectStateAccess.create(glcapabilities, this.enabledExtensions, graphicsworkarounds);
      this.maxSupportedTextureSize = i;
      this.defaultShaderSource = p_454755_;
      this.encoder = new GlCommandEncoder(this);
      this.uniformOffsetAlignment = GL11.glGetInteger(35380);
      GL11.glEnable(34895);
      GL11.glEnable(34370);
      if (glcapabilities.GL_EXT_texture_filter_anisotropic) {
         this.maxSupportedAnisotropy = Mth.floor(GL11.glGetFloat(34047));
         this.enabledExtensions.add("GL_EXT_texture_filter_anisotropic");
      } else {
         this.maxSupportedAnisotropy = 1;
      }
   }

   public GlDebugLabel debugLabels() {
      return this.debugLabels;
   }

   @Override
   public CommandEncoder createCommandEncoder() {
      return this.encoder;
   }

   @Override
   public int getMaxSupportedAnisotropy() {
      return this.maxSupportedAnisotropy;
   }

   @Override
   public GpuSampler createSampler(
      AddressMode p_452294_, AddressMode p_456049_, FilterMode p_460715_, FilterMode p_458527_, int p_460326_, OptionalDouble p_456743_
   ) {
      if (p_460326_ >= 1 && p_460326_ <= this.maxSupportedAnisotropy) {
         return new GlSampler(p_452294_, p_456049_, p_460715_, p_458527_, p_460326_, p_456743_);
      } else {
         throw new IllegalArgumentException("maxAnisotropy out of range; must be >= 1 and <= " + this.getMaxSupportedAnisotropy() + ", but was " + p_460326_);
      }
   }

   @Override
   public GpuTexture createTexture(
      @Nullable Supplier<String> p_397830_,
      @GpuTexture.Usage int p_394481_,
      TextureFormat p_394839_,
      int p_391831_,
      int p_395609_,
      int p_407582_,
      int p_408928_
   ) {
      return this.createTexture(
         this.debugLabels.exists() && p_397830_ != null ? p_397830_.get() : null, p_394481_, p_394839_, p_391831_, p_395609_, p_407582_, p_408928_
      );
   }

   @Override
   public GpuTexture createTexture(
      @Nullable String p_394142_, @GpuTexture.Usage int p_395535_, TextureFormat p_394951_, int p_393944_, int p_392329_, int p_408015_, int p_406483_
   ) {
      if (p_406483_ < 1) {
         throw new IllegalArgumentException("mipLevels must be at least 1");
      }

      if (p_408015_ < 1) {
         throw new IllegalArgumentException("depthOrLayers must be at least 1");
      }

      boolean flag = (p_395535_ & 16) != 0;
      if (flag) {
         if (p_393944_ != p_392329_) {
            throw new IllegalArgumentException("Cubemap compatible textures must be square, but size is " + p_393944_ + "x" + p_392329_);
         }

         if (p_408015_ % 6 != 0) {
            throw new IllegalArgumentException("Cubemap compatible textures must have a layer count with a multiple of 6, was " + p_408015_);
         }

         if (p_408015_ > 6) {
            throw new UnsupportedOperationException("Array textures are not yet supported");
         }
      } else if (p_408015_ > 1) {
         throw new UnsupportedOperationException("Array or 3D textures are not yet supported");
      }

      GlStateManager.clearGlErrors();
      int i = GlStateManager._genTexture();
      if (p_394142_ == null) {
         p_394142_ = String.valueOf(i);
      }

      int j;
      if (flag) {
         GL11.glBindTexture(34067, i);
         j = 34067;
      } else {
         GlStateManager._bindTexture(i);
         j = 3553;
      }

      GlStateManager._texParameter(j, 33085, p_406483_ - 1);
      GlStateManager._texParameter(j, 33082, 0);
      GlStateManager._texParameter(j, 33083, p_406483_ - 1);
      if (p_394951_.hasDepthAspect()) {
         GlStateManager._texParameter(j, 34892, 0);
      }

      if (flag) {
         for (int k : GlConst.CUBEMAP_TARGETS) {
            for (int l = 0; l < p_406483_; l++) {
               GlStateManager._texImage2D(
                  k,
                  l,
                  GlConst.toGlInternalId(p_394951_),
                  p_393944_ >> l,
                  p_392329_ >> l,
                  0,
                  GlConst.toGlExternalId(p_394951_),
                  GlConst.toGlType(p_394951_),
                  null
               );
            }
         }
      } else {
         for (int i1 = 0; i1 < p_406483_; i1++) {
            GlStateManager._texImage2D(
               j,
               i1,
               GlConst.toGlInternalId(p_394951_),
               p_393944_ >> i1,
               p_392329_ >> i1,
               0,
               GlConst.toGlExternalId(p_394951_),
               GlConst.toGlType(p_394951_),
               null
            );
         }
      }

      int j1 = GlStateManager._getError();
      if (j1 == 1285) {
         throw new GpuOutOfMemoryException("Could not allocate texture of " + p_393944_ + "x" + p_392329_ + " for " + p_394142_);
      }

      if (j1 != 0) {
         throw new IllegalStateException("OpenGL error " + j1);
      }

      GlTexture gltexture = new GlTexture(p_395535_, p_394142_, p_394951_, p_393944_, p_392329_, p_408015_, p_406483_, i);
      this.debugLabels.applyLabel(gltexture);
      return gltexture;
   }

   @Override
   public GpuTextureView createTextureView(GpuTexture p_408208_) {
      return this.createTextureView(p_408208_, 0, p_408208_.getMipLevels());
   }

   @Override
   public GpuTextureView createTextureView(GpuTexture p_406554_, int p_410314_, int p_406705_) {
      if (p_406554_.isClosed()) {
         throw new IllegalArgumentException("Can't create texture view with closed texture");
      } else if (p_410314_ >= 0 && p_410314_ + p_406705_ <= p_406554_.getMipLevels()) {
         return new GlTextureView((GlTexture)p_406554_, p_410314_, p_406705_);
      } else {
         throw new IllegalArgumentException(
            p_406705_ + " mip levels starting from " + p_410314_ + " would be out of range for texture with only " + p_406554_.getMipLevels() + " mip levels"
         );
      }
   }

   @Override
   public GpuBuffer createBuffer(@Nullable Supplier<String> p_398040_, @GpuBuffer.Usage int p_395846_, long p_458099_) {
      if (p_458099_ <= 0L) {
         throw new IllegalArgumentException("Buffer size must be greater than zero");
      }

      GlStateManager.clearGlErrors();
      GlBuffer glbuffer = this.bufferStorage.createBuffer(this.directStateAccess, p_398040_, p_395846_, p_458099_);
      int i = GlStateManager._getError();
      if (i == 1285) {
         throw new GpuOutOfMemoryException("Could not allocate buffer of " + p_458099_ + " for " + p_398040_);
      }

      if (i != 0) {
         throw new IllegalStateException("OpenGL error " + i);
      }

      this.debugLabels.applyLabel(glbuffer);
      return glbuffer;
   }

   @Override
   public GpuBuffer createBuffer(@Nullable Supplier<String> p_396390_, @GpuBuffer.Usage int p_410182_, ByteBuffer p_397021_) {
      if (!p_397021_.hasRemaining()) {
         throw new IllegalArgumentException("Buffer source must not be empty");
      }

      GlStateManager.clearGlErrors();
      long i = p_397021_.remaining();
      GlBuffer glbuffer = this.bufferStorage.createBuffer(this.directStateAccess, p_396390_, p_410182_, p_397021_);
      int j = GlStateManager._getError();
      if (j == 1285) {
         throw new GpuOutOfMemoryException("Could not allocate buffer of " + i + " for " + p_396390_);
      }

      if (j != 0) {
         throw new IllegalStateException("OpenGL error " + j);
      }

      this.debugLabels.applyLabel(glbuffer);
      return glbuffer;
   }

   @Override
   public String getImplementationInformation() {
      return GLFW.glfwGetCurrentContext() == 0L
         ? "NO CONTEXT"
         : GlStateManager._getString(7937) + " GL version " + GlStateManager._getString(7938) + ", " + GlStateManager._getString(7936);
   }

   @Override
   public List<String> getLastDebugMessages() {
      return this.debugLog == null ? Collections.emptyList() : this.debugLog.getLastOpenGlDebugMessages();
   }

   @Override
   public boolean isDebuggingEnabled() {
      return this.debugLog != null;
   }

   @Override
   public String getRenderer() {
      return GlStateManager._getString(7937);
   }

   @Override
   public String getVendor() {
      return GlStateManager._getString(7936);
   }

   @Override
   public String getBackendName() {
      return "OpenGL";
   }

   @Override
   public String getVersion() {
      return GlStateManager._getString(7938);
   }

   private static int getMaxSupportedTextureSize() {
      int i = GlStateManager._getInteger(3379);

      for (int j = Math.max(32768, i); j >= 1024; j >>= 1) {
         GlStateManager._texImage2D(32868, 0, 6408, j, j, 0, 6408, 5121, null);
         int k = GlStateManager._getTexLevelParameter(32868, 0, 4096);
         if (k != 0) {
            return j;
         }
      }

      int l = Math.max(i, 1024);
      LOGGER.info("Failed to determine maximum texture size by probing, trying GL_MAX_TEXTURE_SIZE = {}", l);
      return l;
   }

   @Override
   public int getMaxTextureSize() {
      return this.maxSupportedTextureSize;
   }

   @Override
   public int getUniformOffsetAlignment() {
      return this.uniformOffsetAlignment;
   }

   @Override
   public void clearPipelineCache() {
      for (GlRenderPipeline glrenderpipeline : this.pipelineCache.values()) {
         if (glrenderpipeline.program() != GlProgram.INVALID_PROGRAM) {
            glrenderpipeline.program().close();
         }
      }

      this.pipelineCache.clear();

      for (GlShaderModule glshadermodule : this.shaderCache.values()) {
         if (glshadermodule != GlShaderModule.INVALID_SHADER) {
            glshadermodule.close();
         }
      }

      this.shaderCache.clear();
      String s = GlStateManager._getString(7937);
      if (s.contains("AMD")) {
         sacrificeShaderToOpenGlAndAmd();
      }
   }

   private static void sacrificeShaderToOpenGlAndAmd() {
      int i = GlStateManager.glCreateShader(35633);
      int j = GlStateManager.glCreateProgram();
      GlStateManager.glAttachShader(j, i);
      GlStateManager.glDeleteShader(i);
      GlStateManager.glDeleteProgram(j);
   }

   @Override
   public List<String> getEnabledExtensions() {
      return new ArrayList<>(this.enabledExtensions);
   }

   @Override
   public void close() {
      this.clearPipelineCache();
   }

   public DirectStateAccess directStateAccess() {
      return this.directStateAccess;
   }

   protected GlRenderPipeline getOrCompilePipeline(RenderPipeline p_395704_) {
      return this.pipelineCache.computeIfAbsent(p_395704_, p_447704_ -> this.compilePipeline(p_447704_, this.defaultShaderSource));
   }

   protected GlShaderModule getOrCompileShader(Identifier p_451701_, ShaderType p_393555_, ShaderDefines p_392809_, ShaderSource p_460383_) {
      GlDevice.ShaderCompilationKey gldevice$shadercompilationkey = new GlDevice.ShaderCompilationKey(p_451701_, p_393555_, p_392809_);
      return this.shaderCache.computeIfAbsent(gldevice$shadercompilationkey, p_447706_ -> this.compileShader(p_447706_, p_460383_));
   }

   public GlRenderPipeline precompilePipeline(RenderPipeline p_395575_, @Nullable ShaderSource p_457717_) {
      ShaderSource shadersource = p_457717_ == null ? this.defaultShaderSource : p_457717_;
      return this.pipelineCache.computeIfAbsent(p_395575_, p_447703_ -> this.compilePipeline(p_447703_, shadersource));
   }

   private GlShaderModule compileShader(GlDevice.ShaderCompilationKey p_397148_, ShaderSource p_454231_) {
      String s = p_454231_.get(p_397148_.id, p_397148_.type);
      if (s == null) {
         LOGGER.error("Couldn't find source for {} shader ({})", p_397148_.type, p_397148_.id);
         return GlShaderModule.INVALID_SHADER;
      } else {
         String s1 = GlslPreprocessor.injectDefines(s, p_397148_.defines);
         int i = GlStateManager.glCreateShader(GlConst.toGl(p_397148_.type));
         GlStateManager.glShaderSource(i, s1);
         GlStateManager.glCompileShader(i);
         if (GlStateManager.glGetShaderi(i, 35713) == 0) {
            String s2 = StringUtils.trim(GlStateManager.glGetShaderInfoLog(i, 32768));
            LOGGER.error("Couldn't compile {} shader ({}): {}", new Object[]{p_397148_.type.getName(), p_397148_.id, s2});
            return GlShaderModule.INVALID_SHADER;
         } else {
            GlShaderModule glshadermodule = new GlShaderModule(i, p_397148_.id, p_397148_.type);
            this.debugLabels.applyLabel(glshadermodule);
            return glshadermodule;
         }
      }
   }

   private GlProgram compileProgram(RenderPipeline p_452790_, ShaderSource p_459964_) {
      GlShaderModule glshadermodule = this.getOrCompileShader(p_452790_.getVertexShader(), ShaderType.VERTEX, p_452790_.getShaderDefines(), p_459964_);
      GlShaderModule glshadermodule1 = this.getOrCompileShader(p_452790_.getFragmentShader(), ShaderType.FRAGMENT, p_452790_.getShaderDefines(), p_459964_);
      if (glshadermodule == GlShaderModule.INVALID_SHADER) {
         LOGGER.error("Couldn't compile pipeline {}: vertex shader {} was invalid", p_452790_.getLocation(), p_452790_.getVertexShader());
         return GlProgram.INVALID_PROGRAM;
      }

      if (glshadermodule1 == GlShaderModule.INVALID_SHADER) {
         LOGGER.error("Couldn't compile pipeline {}: fragment shader {} was invalid", p_452790_.getLocation(), p_452790_.getFragmentShader());
         return GlProgram.INVALID_PROGRAM;
      }

      try {
         GlProgram glprogram = GlProgram.link(glshadermodule, glshadermodule1, p_452790_.getVertexFormat(), p_452790_.getLocation().toString());
         glprogram.setupUniforms(p_452790_.getUniforms(), p_452790_.getSamplers());
         this.debugLabels.applyLabel(glprogram);
         return glprogram;
      } catch (ShaderManager.CompilationException shadermanager$compilationexception) {
         LOGGER.error("Couldn't compile program for pipeline {}: {}", p_452790_.getLocation(), shadermanager$compilationexception);
         return GlProgram.INVALID_PROGRAM;
      }
   }

   private GlRenderPipeline compilePipeline(RenderPipeline p_396022_, ShaderSource p_452962_) {
      return new GlRenderPipeline(p_396022_, this.compileProgram(p_396022_, p_452962_));
   }

   public VertexArrayCache vertexArrayCache() {
      return this.vertexArrayCache;
   }

   public BufferStorage getBufferStorage() {
      return this.bufferStorage;
   }

   @OnlyIn(Dist.CLIENT)
   record ShaderCompilationKey(Identifier id, ShaderType type, ShaderDefines defines) {
      @Override
      public String toString() {
         String s = this.id + " (" + this.type + ")";
         return !this.defines.isEmpty() ? s + " with " + this.defines : s;
      }
   }
}
