package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.GpuOutOfMemoryException;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.GpuDebugOptions;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.CommandEncoderBackend;
import com.mojang.blaze3d.systems.DeviceInfo;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import com.mojang.blaze3d.systems.GpuQueryPool;
import com.mojang.blaze3d.systems.GpuSurfaceBackend;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.logging.LogUtils;
import java.nio.ByteBuffer;
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
import org.lwjgl.opengl.ARBClipControl;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GLCapabilities;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
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
    private final ShaderSource defaultShaderSource;
    private final Map<RenderPipeline, GlRenderPipeline> pipelineCache = new IdentityHashMap<>();
    private final Map<GlDevice.ShaderCompilationKey, GlShaderModule> shaderCache = new HashMap<>();
    private final FrameBufferCache frameBufferCache = new FrameBufferCache();
    private final VertexArrayCache vertexArrayCache;
    private final BufferStorage bufferStorage;
    private final DeviceInfo deviceInfo;

    public GlDevice(final long windowHandle, final ShaderSource defaultShaderSource, final GpuDebugOptions debugOptions) {
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
        this.defaultShaderSource = defaultShaderSource;
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

    @Override
    public void clearPipelineCache() {
        for (GlRenderPipeline pipeline : this.pipelineCache.values()) {
            if (pipeline.program() != GlProgram.INVALID_PROGRAM) {
                pipeline.program().close();
            }
        }

        this.pipelineCache.clear();

        for (GlShaderModule shader : this.shaderCache.values()) {
            if (shader != GlShaderModule.INVALID_SHADER) {
                shader.close();
            }
        }

        this.shaderCache.clear();
        String glRenderer = GlStateManager._getString(7937);
        if (glRenderer.contains("AMD")) {
            sacrificeShaderToOpenGlAndAmd();
        }
    }

    private static void sacrificeShaderToOpenGlAndAmd() {
        int shader = GlStateManager.glCreateShader(35633);
        int program = GlStateManager.glCreateProgram();
        GlStateManager.glAttachShader(program, shader);
        GlStateManager.glDeleteShader(shader);
        GlStateManager.glDeleteProgram(program);
    }

    @Override
    public void close() {
        this.clearPipelineCache();
        this.encoder.close();
    }

    public DirectStateAccess directStateAccess() {
        return this.directStateAccess;
    }

    protected GlRenderPipeline getOrCompilePipeline(final RenderPipeline pipeline) {
        return this.pipelineCache.computeIfAbsent(pipeline, p -> this.compilePipeline(p, this.defaultShaderSource));
    }

    protected GlShaderModule getOrCompileShader(final Identifier id, final ShaderType type, final ShaderDefines defines, final ShaderSource shaderSource) {
        GlDevice.ShaderCompilationKey key = new GlDevice.ShaderCompilationKey(id, type, defines);
        return this.shaderCache.computeIfAbsent(key, k -> this.compileShader(k, shaderSource));
    }

    public GlRenderPipeline precompilePipeline(final RenderPipeline pipeline, final @Nullable ShaderSource customShaderSource) {
        ShaderSource shaderSource = customShaderSource == null ? this.defaultShaderSource : customShaderSource;
        return this.pipelineCache.computeIfAbsent(pipeline, p -> this.compilePipeline(p, shaderSource));
    }

    private GlShaderModule compileShader(final GlDevice.ShaderCompilationKey key, final ShaderSource shaderSource) {
        String source = shaderSource.get(key.id, key.type);
        if (source == null) {
            LOGGER.error("Couldn't find source for {} shader ({})", key.type, key.id);
            return GlShaderModule.INVALID_SHADER;
        } else {
            String sourceWithDefines = GlslPreprocessor.injectDefines(source, key.defines);
            int shaderId = GlStateManager.glCreateShader(GlConst.toGl(key.type));
            GlStateManager.glShaderSource(shaderId, sourceWithDefines);
            GlStateManager.glCompileShader(shaderId);
            if (GlStateManager.glGetShaderi(shaderId, 35713) == 0) {
                String logInfo = StringUtils.trim(GlStateManager.glGetShaderInfoLog(shaderId, 32768));
                LOGGER.error("Couldn't compile {} shader ({}): {}", key.type.getName(), key.id, logInfo);
                return GlShaderModule.INVALID_SHADER;
            } else {
                GlShaderModule module = new GlShaderModule(shaderId, key.id, key.type);
                this.debugLabels.applyLabel(module);
                return module;
            }
        }
    }

    private GlProgram compileProgram(final RenderPipeline pipeline, final ShaderSource shaderSource) {
        GlShaderModule vertexShader = this.getOrCompileShader(pipeline.getVertexShader(), ShaderType.VERTEX, pipeline.getShaderDefines(), shaderSource);
        GlShaderModule fragmentShader = this.getOrCompileShader(pipeline.getFragmentShader(), ShaderType.FRAGMENT, pipeline.getShaderDefines(), shaderSource);
        if (vertexShader == GlShaderModule.INVALID_SHADER) {
            LOGGER.error("Couldn't compile pipeline {}: vertex shader {} was invalid", pipeline.getLocation(), pipeline.getVertexShader());
            return GlProgram.INVALID_PROGRAM;
        }

        if (fragmentShader == GlShaderModule.INVALID_SHADER) {
            LOGGER.error("Couldn't compile pipeline {}: fragment shader {} was invalid", pipeline.getLocation(), pipeline.getFragmentShader());
            return GlProgram.INVALID_PROGRAM;
        }

        try {
            GlProgram compiled = GlProgram.link(vertexShader, fragmentShader, pipeline.getVertexFormatBindings(), pipeline.getLocation().toString());
            compiled.setupBindGroupLayouts(pipeline.getBindGroupLayouts());
            this.debugLabels.applyLabel(compiled);
            return compiled;
        } catch (IllegalArgumentException e) {
            LOGGER.error("Couldn't compile program for pipeline {}: {}", pipeline.getLocation(), e.getMessage());
            return GlProgram.INVALID_PROGRAM;
        } catch (ShaderManager.CompilationException e) {
            LOGGER.error("Couldn't compile program for pipeline {}: {}", pipeline.getLocation(), e);
            return GlProgram.INVALID_PROGRAM;
        }
    }

    private GlRenderPipeline compilePipeline(final RenderPipeline pipeline, final ShaderSource shaderSource) {
        return new GlRenderPipeline(pipeline, this.compileProgram(pipeline, shaderSource));
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

    @OnlyIn(Dist.CLIENT)
    private record ShaderCompilationKey(Identifier id, ShaderType type, ShaderDefines defines) {
        @Override
        public String toString() {
            String string = this.id + " (" + this.type + ")";
            return !this.defines.isEmpty() ? string + " with " + this.defines : string;
        }
    }
}