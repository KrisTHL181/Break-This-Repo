package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.logging.LogUtils;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntConsumer;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.util.ArrayListDeque;
import net.minecraft.util.Mth;
import net.minecraft.util.TimeSource;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallbackI;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RenderSystem {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final double DEFAULT_DEPTH_CLEAR_VALUE = 0.0;
    public static final int MINIMUM_ATLAS_TEXTURE_SIZE = 1024;
    public static final int PROJECTION_MATRIX_UBO_SIZE = new Std140SizeCalculator().putMat4f().get();
    private static @Nullable Thread renderThread;
    private static @Nullable GpuDevice DEVICE;
    private static final RenderSystem.AutoStorageIndexBuffer sharedSequential = new RenderSystem.AutoStorageIndexBuffer(1, 1, IntConsumer::accept);
    private static final RenderSystem.AutoStorageIndexBuffer sharedSequentialQuad = new RenderSystem.AutoStorageIndexBuffer(4, 6, (c, i) -> {
        c.accept(i);
        c.accept(i + 1);
        c.accept(i + 2);
        c.accept(i + 2);
        c.accept(i + 3);
        c.accept(i);
    });
    private static final RenderSystem.AutoStorageIndexBuffer sharedSequentialLines = new RenderSystem.AutoStorageIndexBuffer(4, 6, (c, i) -> {
        c.accept(i);
        c.accept(i + 1);
        c.accept(i + 2);
        c.accept(i + 3);
        c.accept(i + 2);
        c.accept(i + 1);
    });
    private static ProjectionType projectionType = ProjectionType.PERSPECTIVE;
    private static ProjectionType savedProjectionType = ProjectionType.PERSPECTIVE;
    private static final Matrix4fStack modelViewStack = new Matrix4fStack(16);
    private static @Nullable GpuBufferSlice shaderFog = null;
    private static @Nullable GpuBufferSlice shaderLightDirections;
    private static @Nullable GpuBufferSlice projectionMatrixBuffer;
    private static @Nullable GpuBufferSlice savedProjectionMatrixBuffer;
    private static final AtomicLong pollEventsWaitStart = new AtomicLong();
    private static final AtomicBoolean pollingEvents = new AtomicBoolean(false);
    private static final ArrayListDeque<RenderSystem.GpuAsyncTask> PENDING_FENCES = new ArrayListDeque<>();
    public static @Nullable GpuTextureView outputColorTextureOverride;
    public static @Nullable GpuTextureView outputDepthTextureOverride;
    private static @Nullable GpuBuffer globalSettingsUniform;
    private static @Nullable DynamicUniforms dynamicUniforms;
    private static final ScissorState scissorStateForRenderTypeDraws = new ScissorState();
    private static final SamplerCache samplerCache = new SamplerCache();

    public static SamplerCache getSamplerCache() {
        return samplerCache;
    }

    public static void initRenderThread() {
        if (renderThread != null) {
            throw new IllegalStateException("Could not initialize render thread");
        }

        renderThread = Thread.currentThread();
    }

    public static boolean isOnRenderThread() {
        return Thread.currentThread() == renderThread;
    }

    public static void assertOnRenderThread() {
        if (!isOnRenderThread()) {
            throw constructThreadException();
        }
    }

    private static IllegalStateException constructThreadException() {
        return new IllegalStateException("Rendersystem called from wrong thread");
    }

    public static void pollEvents() {
        pollEventsWaitStart.set(Util.getMillis());
        pollingEvents.set(true);
        GLFW.glfwPollEvents();
        pollingEvents.set(false);
    }

    public static boolean isFrozenAtPollEvents() {
        return pollingEvents.get() && Util.getMillis() - pollEventsWaitStart.get() > 200L;
    }

    public static void setShaderFog(final GpuBufferSlice fog) {
        shaderFog = fog;
    }

    public static @Nullable GpuBufferSlice getShaderFog() {
        return shaderFog;
    }

    public static void setShaderLights(final GpuBufferSlice buffer) {
        shaderLightDirections = buffer;
    }

    public static @Nullable GpuBufferSlice getShaderLights() {
        return shaderLightDirections;
    }

    public static void enableScissorForRenderTypeDraws(final int x, final int y, final int width, final int height) {
        scissorStateForRenderTypeDraws.enable(x, y, width, height);
    }

    public static void disableScissorForRenderTypeDraws() {
        scissorStateForRenderTypeDraws.disable();
    }

    public static ScissorState getScissorStateForRenderTypeDraws() {
        return scissorStateForRenderTypeDraws;
    }

    public static String getBackendDescription() {
        return String.format(Locale.ROOT, "LWJGL version %s", GLX._getLWJGLVersion());
    }

    public static TimeSource.NanoTimeSource initBackendSystem() {
        return GLX._initGlfw()::getAsLong;
    }

    public static void initRenderer(final GpuDevice device) {
        if (DEVICE != null) {
            throw new IllegalStateException("RenderSystem.DEVICE already initialized");
        }

        DEVICE = device;
        dynamicUniforms = new DynamicUniforms();
        samplerCache.initialize();
    }

    public static void shutdownRenderer() {
        sharedSequential.close();
        sharedSequentialQuad.close();
        sharedSequentialLines.close();
        samplerCache.close();
        if (dynamicUniforms != null) {
            dynamicUniforms.close();
        }

        if (DEVICE != null) {
            DEVICE.close();
        }
    }

    public static void setErrorCallback(final GLFWErrorCallbackI onFullscreenError) {
        GLX._setGlfwErrorCallback(onFullscreenError);
    }

    public static void setupDefaultState() {
        modelViewStack.clear();
    }

    public static void setProjectionMatrix(final GpuBufferSlice projectionMatrixBuffer, final ProjectionType type) {
        assertOnRenderThread();
        RenderSystem.projectionMatrixBuffer = projectionMatrixBuffer;
        projectionType = type;
    }

    public static void backupProjectionMatrix() {
        assertOnRenderThread();
        savedProjectionMatrixBuffer = projectionMatrixBuffer;
        savedProjectionType = projectionType;
    }

    public static void restoreProjectionMatrix() {
        assertOnRenderThread();
        projectionMatrixBuffer = savedProjectionMatrixBuffer;
        projectionType = savedProjectionType;
    }

    public static @Nullable GpuBufferSlice getProjectionMatrixBuffer() {
        assertOnRenderThread();
        return projectionMatrixBuffer;
    }

    public static Matrix4f getModelViewMatrixCopy() {
        assertOnRenderThread();
        return new Matrix4f(modelViewStack);
    }

    public static Matrix4fStack getModelViewStack() {
        assertOnRenderThread();
        return modelViewStack;
    }

    public static RenderSystem.AutoStorageIndexBuffer getSequentialBuffer(final PrimitiveTopology primitiveTopology) {
        assertOnRenderThread();

        return switch (primitiveTopology) {
            case QUADS -> sharedSequentialQuad;
            case LINES -> sharedSequentialLines;
            default -> sharedSequential;
        };
    }

    public static void setGlobalSettingsUniform(final GpuBuffer buffer) {
        globalSettingsUniform = buffer;
    }

    public static @Nullable GpuBuffer getGlobalSettingsUniform() {
        return globalSettingsUniform;
    }

    public static ProjectionType getProjectionType() {
        assertOnRenderThread();
        return projectionType;
    }

    public static void queueFencedTask(final Runnable task) {
        PENDING_FENCES.addLast(new RenderSystem.GpuAsyncTask(task, getDevice().createCommandEncoder().createFence()));
    }

    public static void executePendingTasks() {
        for (RenderSystem.GpuAsyncTask task = PENDING_FENCES.peekFirst(); task != null; task = PENDING_FENCES.peekFirst()) {
            if (!task.fence.awaitCompletion(0L)) {
                return;
            }

            try {
                task.callback.run();
            } finally {
                task.fence.close();
            }

            PENDING_FENCES.removeFirst();
        }
    }

    public static GpuDevice getDevice() {
        if (DEVICE == null) {
            throw new IllegalStateException("Can't getDevice() before it was initialized");
        } else {
            return DEVICE;
        }
    }

    public static @Nullable GpuDevice tryGetDevice() {
        return DEVICE;
    }

    public static DynamicUniforms getDynamicUniforms() {
        if (dynamicUniforms == null) {
            throw new IllegalStateException("Can't getDynamicUniforms() before device was initialized");
        } else {
            return dynamicUniforms;
        }
    }

    public static void bindDefaultUniforms(final RenderPass renderPass) {
        GpuBufferSlice projectionMatrix = getProjectionMatrixBuffer();
        if (projectionMatrix != null) {
            renderPass.setUniform("Projection", projectionMatrix);
        }

        GpuBufferSlice fog = getShaderFog();
        if (fog != null) {
            renderPass.setUniform("Fog", fog);
        }

        GpuBuffer globalUniform = getGlobalSettingsUniform();
        if (globalUniform != null) {
            renderPass.setUniform("Globals", globalUniform);
        }

        GpuBufferSlice shaderLights = getShaderLights();
        if (shaderLights != null) {
            renderPass.setUniform("Lighting", shaderLights);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static final class AutoStorageIndexBuffer implements AutoCloseable {
        private final int vertexStride;
        private final int indexStride;
        private final RenderSystem.AutoStorageIndexBuffer.IndexGenerator generator;
        private @Nullable GpuBuffer buffer;
        private IndexType type = IndexType.SHORT;
        private int indexCount;

        private AutoStorageIndexBuffer(final int vertexStride, final int indexStride, final RenderSystem.AutoStorageIndexBuffer.IndexGenerator generator) {
            this.vertexStride = vertexStride;
            this.indexStride = indexStride;
            this.generator = generator;
        }

        @Override
        public void close() {
            if (this.buffer != null) {
                this.buffer.close();
            }
        }

        public boolean hasStorage(final int indexCount) {
            return indexCount <= this.indexCount;
        }

        public GpuBuffer getBuffer(final int indexCount) {
            this.ensureStorage(indexCount);
            return this.buffer;
        }

        private void ensureStorage(int indexCount) {
            if (!this.hasStorage(indexCount)) {
                indexCount = Mth.roundToward(indexCount * 2, this.indexStride);
                RenderSystem.LOGGER.debug("Growing IndexBuffer: Old limit {}, new limit {}.", this.indexCount, indexCount);
                int primitiveCount = indexCount / this.indexStride;
                int vertexCount = primitiveCount * this.vertexStride;
                IndexType type = IndexType.least(vertexCount);
                int bufferSize = Mth.roundToward(indexCount * type.bytes, 4);
                ByteBuffer data = MemoryUtil.memAlloc(bufferSize);

                try {
                    this.type = type;
                    it.unimi.dsi.fastutil.ints.IntConsumer intConsumer = this.intConsumer(data);

                    for (int ii = 0; ii < indexCount; ii += this.indexStride) {
                        this.generator.accept(intConsumer, ii * this.vertexStride / this.indexStride);
                    }

                    data.flip();
                    if (this.buffer != null) {
                        this.buffer.close();
                    }

                    this.buffer = RenderSystem.getDevice().createBuffer(() -> "Auto Storage index buffer", 64, data);
                } finally {
                    MemoryUtil.memFree(data);
                }

                this.indexCount = indexCount;
            }
        }

        private it.unimi.dsi.fastutil.ints.IntConsumer intConsumer(final ByteBuffer buffer) {
            switch (this.type) {
                case SHORT:
                    return value -> buffer.putShort((short)value);
                case INT:
                default:
                    return buffer::putInt;
            }
        }

        public IndexType type() {
            return this.type;
        }

        @OnlyIn(Dist.CLIENT)
        private interface IndexGenerator {
            void accept(final it.unimi.dsi.fastutil.ints.IntConsumer consumer, final int start);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private record GpuAsyncTask(Runnable callback, GpuFence fence) {
    }
}